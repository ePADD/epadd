package edu.stanford.muse.xcoll;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.Config;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.ModeConfig;
import edu.stanford.muse.webapp.SimpleSessions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/** This is a class that keeps track of all entities in multiple archives and provides an interface to search through them at a token level. */

public class CrossCollectionSearch {
    public static Log log = LogFactory.getLog(CrossCollectionSearch.class);

  /*  public static List<Archive.CollectionMetadata> archiveMetadatas = new ArrayList<>(); // metadata's for the archives. the position number in this list is what is used in the EntityInfo
    public static List<String> archiveDirs = new ArrayList<>(); // metadata's for the archives. the position number in this list is what is used in the EntityInfo
*/
    private static Multimap<String, EntityInfo> cTokenToInfos; // this token -> infos mapping is intended to make the lookup more efficient. Otherwise, we'd have to go through all the infos for looking up a string.
    private static Set<String> allCEntities = new LinkedHashSet<>(); // this token -> infos mapping is intended to make the lookup more efficient. Otherwise, we'd have to go through all the infos for looking up a string.

    // this has to be fleshed out some more -- which version of canonicalize to use?
    // right now, we only lowercase the input string and return it.
    // "Barack Obama" returns "barack obama"
    // ideally, we could do other transformations that make the lookup more robust, e.g.
    // normalize whitespaces "abc  def" => "abc def"
    // or canonicalize variants, like "Bob X" -> "Robert X"
    // or remove accents (o with an umlaut -> o, etc.)
    // note: efficiency is also a concern, since this is called for every entity in all collections.
    private static String canonicalize (String s) {
        if (s == null)
            return null;
        else
            return s.toLowerCase();
    }

    /* should be synchronized so there's no chance of doing it multiple times at the same time. */
    synchronized public static void initialize() {
        if (cTokenToInfos != null)
            return;

        if (ModeConfig.isDiscoveryMode())
            initialize(Config.REPO_DIR_DISCOVERY);
        else if (ModeConfig.isProcessingMode())
            initialize(Config.REPO_DIR_PROCESSING);
    }

    /** initializes lookup structures (entity infos and ctokenToInfos) for cross collection search
     * reads all archives available in the base dir.
     * should be synchronized so there's no chance of doing it multiple times at the same time.
     **/
    synchronized private static void initialize(String baseDir) {

        // this is created only once in one run. if it has already been created, reuse it.
        // in the future, this may be read from a serialized file, etc.
        cTokenToInfos = LinkedHashMultimap.create();

        File[] files = new File(baseDir).listFiles();

        if (files == null) {
            log.warn ("Trying to initialize cross collection search from an invalid directory: " + baseDir);
            return;
        }

        int archiveNum = 0;
        for (File f: files) {
            if (!f.isDirectory())
                continue;

            try {
                String archiveFile = f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.getSessionSuffix();
                if (!new File(archiveFile).exists())
                    continue;

                Archive archive = ArchiveReaderWriter.readArchiveIfPresent(f.getAbsolutePath());
                if (archive == null) {
                    log.warn ("failed to read archive from " + f.getAbsolutePath());
                    continue;
                }

                log.info ("Loaded archive from " + f.getAbsolutePath());
                log.info ("Loaded archive metadata from " + f.getAbsolutePath());

                // process all docs in this archive to set up centityToInfo map
                String archiveID= ArchiveReaderWriter.getArchiveIDForArchive(archive);
                Map<String, EntityInfo> centityToInfo = new LinkedHashMap<>();
                {
                    AddressBook ab = archive.addressBook;
                    for (Document d : archive.getAllDocs()) {
                        EmailDocument ed = (EmailDocument) d;

                        // compute centities, the set of all canonicalized entities in this doc.
                        // see spec in prodpad #140
                        // first come correspondents, then subject entities, then body.
                        // for correspondents we incl. all forms of their email addr or contact name)
                        // it should be a set because we want to count every string only once per message
                        Set<String> entities, correspondentEntities;
                        {
                            entities = new LinkedHashSet<>();
                            Set<Contact> contacts = ed.getParticipatingContacts(ab);
                            for (Contact c : contacts) {
                                if (c.getNames()!= null)
                                    entities.addAll(c.getNames());
                                if (c.getEmails()!= null)
                                    entities.addAll(c.getEmails());
                            }

                            correspondentEntities = new LinkedHashSet<>(entities); // keep track of the correspondent centities also, separately because we need the isCorrespondent flag

                            Set<String> set = archive.getEntitiesInDoc(ed);
                            if (!Util.nullOrEmpty(set))
                                entities.addAll (set);

                            // filter out any null or empty strings (just in case)
                            // don't canonicalize right away because we need to keep the original form of the name
                            entities = entities.stream().filter(s -> !Util.nullOrEmpty(s)).collect(Collectors.toSet());
                        }

                        // convert the correspondent entities to c entities
                        Set<String> correspondentCEntities = correspondentEntities.stream().map(CrossCollectionSearch::canonicalize).collect (Collectors.toSet());

                        for (String entity : entities) {
                           String centity = canonicalize(entity);
                            EntityInfo ei = centityToInfo.get(centity);
                            if (ei == null) {
                                ei = new EntityInfo();
                                ei.archiveID = archiveID;
                                ei.displayName = entity;
                                centityToInfo.put(centity, ei);
                            }

                            // isCorrespondent is set to true if ANY of the messages has centity as a correspondent
                            // it is 1-way, i.e. once it is set, it will not be unset.
                            if (correspondentCEntities.contains (centity)) {
                                ei.isCorrespondent = true;
                            }

                            // update the first/last dates if needed
                            if (ei.firstDate == null || ei.firstDate.after(ed.date)) {
                                ei.firstDate = ed.date;
                            }
                            if (ei.lastDate == null || ei.lastDate.before(ed.date)) {
                                ei.lastDate = ed.date;
                            }
                            ei.count++;
                        }
                    }
                }

                log.info ("Archive # " + archiveNum + " read " + centityToInfo.size() + " entities");

                // now set up this map as a token map
                for (EntityInfo ei: centityToInfo.values()) {
                    String entity = ei.displayName;
                    String centity = canonicalize(entity);
                    allCEntities.add (centity);
                    Set<String> ctokens = new LinkedHashSet<>(Util.tokenize(centity)); // consider a set of tokens because we don't want repeats
                    for (String ctoken: ctokens)
                        cTokenToInfos.put (ctoken, ei);
                }
            } catch (Exception e) {
                Util.print_exception ("Error loading archive in directory " + f.getAbsolutePath(), e, log);
            }
            archiveNum++;
        }
    }

    /** returns EntityInfo's that match entity (word wise) */
    private static Collection<EntityInfo> getInfosFor (String lookupString) {
        // ensure we're initialized
        initialize();

        Set<EntityInfo> result = new LinkedHashSet<>(); // set to ensure that a result appears only once

        // tokenize entity and look up all infos that contain any of its tokens
        // todo: make this handle variants

        // get all the tokens in the lookup
        String cLookupString = canonicalize(lookupString);
        List<String> cLookupTokens = Util.tokenize(cLookupString);

        // check all EntityInfo's that contain any of the tokens
        // if the display name contains the lookupString, the EntityInfo is added to the result
        for (String cLookupToken: cLookupTokens) {
            Collection<EntityInfo> infos = cTokenToInfos.get(cLookupToken);
            if (infos != null)
                for (EntityInfo info: infos) {
                    String cDisplayName = canonicalize(info.displayName);
                    if (cDisplayName != null && cDisplayName.contains (cLookupString)) // only if cLookupString is contained in entirety in cDisplayName do we add this info to result
                        result.add (info);
                }
        }

        return result;
    }

    /* returns archiveNum -> {String -> Info, String -> Info, ...}
    *  get all entity infos that contain entity. result only includes infos that contain entity in its entirety.
     * e.g. entity="Mary" would include both { Mary Lou Retton -> Info, Mary Ann Spinner -> Info, etc.}
     * but entity="Mary Lou" would include only  { Mary Lou Retton -> Info }
     * note that only full words are matched, i.e.
     * entity="Mary" will not return a name like MaryLou in any case.
     **/

    public static Multimap<String, EntityInfo> search (String entity) {

        // first get all infos that match the entity
        Collection<EntityInfo> infos = getInfosFor(entity);

        // break them down by archiveNum
        Multimap<String, EntityInfo> archiveNumToInfos = LinkedHashMultimap.create();
        for (EntityInfo info: infos)
            archiveNumToInfos.put (info.archiveID, info);

        return archiveNumToInfos;
    }

    /** this is more robust (doesn't depend on full word matching. but is highly inefficient right now! need to optimize*/
    public static List<String> searchForAutocomplete (String entity, int max) {
        initialize();
        List<String> result = new ArrayList<>();
        String centity = canonicalize(entity);
        for (String e: allCEntities) {
            if (e.contains (centity)) {
                result.add(e);
            }
            if (result.size() >= max)
                return result;
        }
        return result;
    }
}
