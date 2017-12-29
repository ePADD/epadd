package edu.stanford.muse.AddressBookManager;

import edu.stanford.muse.Config;
import edu.stanford.muse.AuthorityMapper.AuthorityMapper;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static edu.stanford.muse.ie.FASTIndexer.FIELD_NAME_FAST_ID;
import static edu.stanford.muse.ie.variants.EntityBook.canonicalize;

/**
 * Created by chinmay on 18/10/17.
 */
public class CorrespondentAuthorityMapper extends AuthorityMapper implements   java.io.Serializable  {

    public static Log log					= LogFactory.getLog(CorrespondentAuthorityMapper.class);
    private final static long serialVersionUID = 1L;

    /* creates a mapper. first checks if it already exists in the archive dir. otherwise creates a new one, and initializes it for the archive (might take a while to generate candidates) */
    public static CorrespondentAuthorityMapper createCorrespondentAuthorityMapper (Archive archive) throws IOException, ParseException, ClassNotFoundException {

        String filename = archive.baseDir + File.separator + Config.AUTHORITIES_FILENAME;
        File authMapperFile = new File (filename);

        CorrespondentAuthorityMapper cmapper = new CorrespondentAuthorityMapper();
        try {
            if (authMapperFile.exists() && authMapperFile.canRead()) {
                cmapper = (CorrespondentAuthorityMapper) Util.readObjectFromFile(filename);
            }
        } catch (Exception e) {
            Util.print_exception ("Error reading authority mapper file: " + filename, e, log);
        }

        if (cmapper== null)
            cmapper= new CorrespondentAuthorityMapper();

        // get ready for queries
        cmapper.openFastIndex ();

        // compute candidates if we don't have them yet. may need some way to force recomputation in the future, even if already computed.
        if (cmapper.cnameToFastIdCandidates.isEmpty() || cmapper.cnameToCount.isEmpty()) {
            // this can take a while.... might need to make a progress bar available for large archives
            cmapper.setupCandidatesAndCounts(archive);
        }
        return cmapper;
    }

    /** this should be called during creation time, or any time the cnameToFastIdCandidates has to be recomputed */
    public void setupCandidatesAndCounts(Archive archive) throws IOException, ParseException {
        AddressBook ab = archive.getAddressBook();

        List<Contact> contacts = ab.allContacts();
        for (Contact c : contacts) {
            try {
                Set<String> names = c.getNames();
                if (Util.nullOrEmpty(names))
                    continue;

                String contactName = c.pickBestName();
                String cname = canonicalize(contactName);
                List<String> cnameTokens = Util.tokenize(cname);
                if (cnameTokens.size() < 2)
                    continue; // only match when 2 or more words are present in the name

                if (cnameToAuthority.get(cname) == null) {
                    for (String name : names) {
                        List<String> nameTokens = Util.tokenize(name);
                        if (nameTokens.size() < 2)
                            continue; // only match when 2 or more words are present in the name

                        List<Document> hits = lookupNameInFastIndex(name);
                        if (hits.size() > 20)
                            log.warn ("Warning: many (" + hits.size() + ") hits for authority name=" + name + " (associated with contact " + contactName + ")");
                        for (Document d : hits) {
                            Long fastId = Long.parseLong(d.get(FIELD_NAME_FAST_ID));
                            cnameToFastIdCandidates.put(cname, fastId);
                        }
                    }
                }
            } catch (Exception e) {
                Util.print_exception("Error parsing contact for authorities: " + c, e, log);
            }
        }

        List<Pair<Contact, Integer>> pairs = ab.sortedContactsAndCounts((Collection) archive.getAllDocs());
        for (Pair<Contact, Integer> p : pairs) {
            Contact c = p.getFirst();
            String name = c.pickBestName();
            String cname = canonicalize(name);
            if (Util.nullOrEmpty(cname))
                continue;

            cnameToCount.put(cname, p.getSecond());
        }
    }
    /** small class meant to convey temp. results to the frontend. Not serialized. */
    public static class AuthorityInfo {
        public boolean isConfirmed;
        public int nMessages;
        public String name, tooltip, url, errorMessage;
        public AuthorityMapper.AuthorityRecord confirmedAuthority;
        public List<AuthorityMapper.AuthorityRecord> candidates;
    }

    /** returns an authorityInfo object representing info needed by the front-end. Use only for rendering the authorities table. */
    public AuthorityInfo getCorrespondentAuthorityInfo (String archiveID, AddressBook ab, String name) throws IOException, ParseException {
        String cname = canonicalize (name);
        AuthorityInfo result = new AuthorityInfo();
        result.isConfirmed = false;
        result.name = name;

        Integer nMessages = (cnameToCount != null) ? cnameToCount.get(cname) : null;
        result.nMessages = (nMessages == null) ? 0 : nMessages;

        String tooltip = "";
        Collection<Contact> contacts = ab.lookupByName(name);
        if (contacts != null)
            for (Contact contact: contacts) {
                tooltip += contact.toTooltip();
            }
        else {
            result.errorMessage = "Name not in address book: " + name;
            return result;
        }

        result.url = "browse?archiveID=" + archiveID + "&adv-search=on&correspondentTo=on&correspondentFrom=on&correspondentCc=on&correspondentBcc=on&correspondent=" + name;
        result.tooltip = tooltip;
        result.nMessages = nMessages==null?0:nMessages;

        AuthorityMapper.AuthorityRecord authRecord = cnameToAuthority.get(cname);
        if (authRecord != null) {
            result.isConfirmed = true;
            result.confirmedAuthority = authRecord;
        }

        List<AuthorityMapper.AuthorityRecord> candidates = new ArrayList<>();
        Collection<Long> fastIds = cnameToFastIdCandidates.get(cname);
       /* if (fastIds != null)
            for (Long id : fastIds)
                candidates.add (getAuthRecordForFASTId(id));
*/
        result.candidates = candidates;
        return result;
    }


}
