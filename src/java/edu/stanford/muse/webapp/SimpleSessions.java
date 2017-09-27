package edu.stanford.muse.webapp;

import edu.stanford.muse.Config;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.email.MuseEmailFetcher;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Archive.ProcessingMetadata;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.Lexicon;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SimpleSessions {
	private static Log	log	= LogFactory.getLog(SimpleSessions.class);

	private static final String SESSION_SUFFIX = ".archive.v2"; // all session files end with .session
	private  static String CACHE_BASE_DIR = null; // overruled (but not overwritten) by session's attribute "cacheDir"
	private static String CACHE_DIR = null; // overruled (but not overwritten) by session's attribute "cacheDir"
	//private static String SESSIONS_DIR = null;
	private static String MUSE_DIRNAME = ".muse"; // clients might choose to override this
	private static String getVarOrDefault(String prop_name, String default_val)
	{
		String val = System.getProperty(prop_name);
		if (!Util.nullOrEmpty(val))
			return val;
		else
			return default_val;
	}

	static {
		MUSE_DIRNAME = getVarOrDefault("muse.defaultArchivesDir", System.getProperty("user.home") + File.separator + ".muse");
		CACHE_BASE_DIR = getVarOrDefault("muse.dir.cache_base", MUSE_DIRNAME);
		CACHE_DIR      = getVarOrDefault("muse.dir.cache"  , CACHE_BASE_DIR + File.separator + "user"); // warning/todo: this "-D" not universally honored yet, e.g., user_key and fixedCacheDir in MuseEmailFetcher.java
		//SESSIONS_DIR   = getVarOrDefault("muse.dir.sessions", CACHE_DIR + File.separator + Archive.SESSIONS_SUBDIR); // warning/todo: this "-D" not universally honored yet, e.g., it is hard-coded again in saveSession() (maybe saveSession should actually use getSessinoDir() rather than basing it on cacheDir)
	}
	public static String getDefaultCacheDir()
	{
		return CACHE_DIR;
	}

	public static String getDefaultRootDir()
	{
		return CACHE_BASE_DIR;
	}

	public static String getSessionSuffix(){ return SESSION_SUFFIX;}
	/**
	 * loads session from the given filename, and returns the map of loaded
	 * attributes.
	 * if readOnly is false, caller MUST make sure to call packIndex.
	 * baseDir is Indexer's baseDir (path before "indexes/")
	 * 
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 */
	public static Map<String, Object> loadSessionAsMap(String filename, String baseDir, boolean readOnly) throws IOException
	{
		log.info("Loading session from file " + filename + " size: " + Util.commatize(new File(filename).length() / 1024) + " KB");

		ObjectInputStream ois = null;

		// keep reading till eof exception
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		try {
			ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename)));

			while (true)
			{
				String key = (String) ois.readObject();
				log.info("loading key: " + key);
				try {
					Object value = ois.readObject();
					if (value == null)
						break;
					result.put(key, value);
				} catch (InvalidClassException ice)
				{
					log.error("Bad version for value of key " + key + ": " + ice + "\nContinuing but this key is not set...");
				} catch (ClassNotFoundException cnfe)
				{
					log.error("Class not found for value of key " + key + ": " + cnfe + "\nContinuing but this key is not set...");
				}
			}
		} catch (EOFException eof) {
			log.info("end of session file reached");
		} catch (Exception e) {
			log.warn("Warning unable to load session: " + Util.stackTrace(e));
			result.clear();
		}

		if (ois != null)
			try {
				ois.close();
			} catch (Exception e) {
				Util.print_exception(e, log);
			}

		// need to set up sentiments explicitly -- now no need since lexicon is part of the session
		log.info("Memory status: " + Util.getMemoryStats());

		Archive archive = (Archive) result.get("archive");

		// no groups in public mode
		if (archive != null)
		{
            System.err.println("dir: "+archive.indexer);
			// most of this code should probably move inside Archive, maybe a function called "postDeserialized()"
			archive.postDeserialized(baseDir, readOnly);
			result.put("emailDocs", archive.getAllDocs());
		}

		return result;
	}

	/** saves the archive in the current session to the cachedir *//*
	public static boolean saveArchive(String archiveID) throws IOException
	{
		Archive archive =  SimpleSessions.getArchiveForArchiveID(archiveID);
		assert archive!=null : new AssertionError("No archive for archiveID = " + archiveID + ". Is an archive loaded?");
		// String baseDir = (String) session.getAttribute("cacheDir");
		return saveArchive(archive.baseDir, "default", archive);
	}*/

	/** saves the archive in the current session to the cachedir */
	public static boolean saveArchive(Archive archive) throws IOException
	{
		assert archive!=null : new AssertionError("No archive to save.");
		// String baseDir = (String) session.getAttribute("cacheDir");
		return saveArchive(archive.baseDir, "default", archive);
	}

	/**
	 * reads name.processing.metadata from the given basedir. should be used
	 * when quick archive metadata is needed without loading the actual archive
	 */
	public static ProcessingMetadata readProcessingMetadata(String baseDir, String name)
	{
		String processingFilename = baseDir + File.separatorChar + name + Config.PROCESSING_METADATA_SUFFIX;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(processingFilename)));
			ProcessingMetadata metadata = (ProcessingMetadata) ois.readObject();
			return metadata;
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e1) {
				Util.print_exception("Unable to read processing metadata", e1, log);
			}
		}
	}

	/** saves the archive in the current session to the cachedir. note: no blobs saved. */
	public static boolean saveArchive(String baseDir, String name, Archive archive) throws IOException
	{
		log.info("Before saving the archive checking if it is still in good shape");
		archive.Verify();
		String dir = baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
		new File(dir).mkdirs(); // just to be safe
		String filename = dir + File.separatorChar + name + SimpleSessions.SESSION_SUFFIX;
		log.info("Saving archive to (session) file " + filename);

		if (archive.processingMetadata == null)
			archive.processingMetadata = new ProcessingMetadata();

		archive.processingMetadata.timestamp = new Date().getTime();
		archive.processingMetadata.tz = TimeZone.getDefault();
		archive.processingMetadata.nDocs = archive.getAllDocs().size();
		archive.processingMetadata.nUniqueBlobs = archive.blobStore.uniqueBlobs.size();

        int totalAttachments = 0, images = 0, docs = 0, others = 0, sentMessages = 0, receivedMessages = 0, hackyDates = 0;
        Date firstDate = null, lastDate = null;

        for (Document d: archive.getAllDocs()) {
            if (!(d instanceof EmailDocument))
                continue;
            EmailDocument ed = (EmailDocument) d;
            if (ed.date != null) {
				if (ed.hackyDate)
					hackyDates++;
				else {
					if (firstDate == null || ed.date.before(firstDate))
						firstDate = ed.date;
					if (lastDate == null || ed.date.after(lastDate))
						lastDate = ed.date;
				}
            }
            int sentOrReceived = ed.sentOrReceived(archive.addressBook);
            if ((sentOrReceived & EmailDocument.SENT_MASK) != 0)
                sentMessages++;
            if ((sentOrReceived & EmailDocument.RECEIVED_MASK) != 0)
                receivedMessages++;

            if (!Util.nullOrEmpty(ed.attachments))
            {
                totalAttachments += ed.attachments.size();
                for (Blob b: ed.attachments)
                    if (!Util.nullOrEmpty(b.filename))
                    {
                        if (Util.is_image_filename(b.filename))
                            images++;
                        else if (Util.is_doc_filename(b.filename))
                            docs++;
                        else
                            others++;
                    }
            }
        }

        archive.processingMetadata.firstDate = firstDate;
        archive.processingMetadata.lastDate = lastDate;
        archive.processingMetadata.nIncomingMessages = receivedMessages;
        archive.processingMetadata.nOutgoingMessages = sentMessages;
		archive.processingMetadata.nHackyDates = hackyDates;

        archive.processingMetadata.nBlobs = totalAttachments;
        archive.processingMetadata.nUniqueBlobs = archive.blobStore.uniqueBlobs.size();
        archive.processingMetadata.nImageBlobs = images;
        archive.processingMetadata.nDocBlobs = docs;
        archive.processingMetadata.nOtherBlobs = others;

        archive.close();

		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
		try {
			oos.writeObject("archive");
			oos.writeObject(archive);
		} catch (Exception e1) {
			Util.print_exception("Failed to write archive: ", e1, log);
		} finally {
			oos.close();
		}

		// now write out the metadata
		String processingFilename = dir + File.separatorChar + name + Config.PROCESSING_METADATA_SUFFIX;
		oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(processingFilename)));
		try {
			oos.writeObject(archive.processingMetadata);
		} catch (Exception e1) {
            Util.print_exception("Failed to write archive's metadata: ", e1, log);
			oos.close();
		} finally {
			oos.close();
		}

		if (archive.authorityMapper != null) {
			String authorityMapperFilename = dir + File.separatorChar + name + Config.AUTHORITIES_FILENAME;
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(authorityMapperFilename)));
			try {
				oos.writeObject(archive.authorityMapper);
			} catch (Exception e1) {
				Util.print_exception("Failed to write archive's authority mapper: ", e1, log);
				oos.close();
			} finally {
				oos.close();
			}
		}
        // re-open for reading
		archive.openForRead();

        // note: no need of saving archive authorities separately -- they are already saved as part of the archive object
		return true;
	}

	// an archive in a given dir should be loaded only once into memory.
	// this map stores the directory -> archive mapping.
	private static LinkedHashMap<String, WeakReference<Archive>> globaldirToArchiveMap = new LinkedHashMap<String, WeakReference<Archive>>();
	private static LinkedHashMap<String,Archive> globalArchiveIDToArchiveMap = new LinkedHashMap<String, Archive>();
	private static LinkedHashMap<Archive,String> globalArchiveToArchiveIDMap = new LinkedHashMap<Archive, String>();

	/** VIP method. Should be the single place to load an archive from disk.
     * loads an archive from the given directory. always re-uses archive objects loaded from the same directory.
	 * this is fine when:
	 * - running single-user
	 * - running discovery mode epadd, since a single archive should be loaded only once.
	 * - even in a hosted mode with different archives in simultaneous play, where different people have their own userKeys and therefore different dirs.
	 * It may NOT be fine if  multiple people are operating on their different copies of an archive loaded from the same place. Don't see a use-case for this right now.
	 * if you don't like that, tough luck.
	 * return the archive, or null if it doesn't exist. */
	public static Archive readArchiveIfPresent(String baseDir) throws IOException
	{
		String archiveFile = baseDir + File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.SESSION_SUFFIX;
		if (!new File(archiveFile).exists()) {
			return null;
		}

		/*synchronized (globaldirToLoadCountMap) {
			Integer loadCount = globaldirToLoadCountMap.get(baseDir);
			int newCount = (loadCount == null) ? 1 : loadCount + 1;
			globaldirToLoadCountMap.put(baseDir, newCount);
			log.info ("Since server start, the archive: " + archiveFile + " has been (attempted to be) loaded " + Util.pluralize(newCount, "time"));
		}*/

		try {
			// locking the global dir might be inefficient if many people are loading different archives at the same time.
			// not a concern right now. it it does become one, locking a small per-dir object like archiveFile.intern(), along with a ConcurrenctHashMap might handle it.
			synchronized (globaldirToArchiveMap) {
				// the archive is wrapped inside a weak ref to allow the archive object to be collected if there are no references to it (usually the references
				// are in the user sessions).
				WeakReference<Archive> wra = getArchiveFromGlobalArchiveMap(baseDir);
				if (wra != null) {
					Archive a = wra.get();
					if (a != null) {
						log.info("Great, could re-use loaded archive for dir: " + archiveFile + "; archive = " + a);
						return a;
					}
				}

				log.info("Archive not already loaded, reading from dir: " + archiveFile);
				Map<String, Object> map = loadSessionAsMap(archiveFile, baseDir, true);
				// read the session map, but only use archive
				Archive a = (Archive) map.get("archive");
				// could do more health checks on archive here
				if (a == null) {
					log.warn ("Archive key is not present in archive file! The archive must be corrupted! directory:" + archiveFile);
					return null;
				}
				a.setBaseDir(baseDir);
                // no need to read archive authorized authorities, they will be loaded on demand from the legacy authorities.ser file
				addToGlobalArchiveMap(baseDir,a);
				return a;
			}
		} catch (Exception e) {
			Util.print_exception("Error reading archive from dir: " + archiveFile, e, log);
			throw new RuntimeException(e);
		}
	}

	private static void addToGlobalArchiveMap(String archiveDir, Archive archive){

		//add to globalDirmap
		globaldirToArchiveMap.put(archiveDir,new WeakReference<Archive>(archive));
		//construct archive ID from the tail of the archiveFile (by sha1)
		String archiveID = Util.hash(archiveDir);
		globalArchiveIDToArchiveMap.put(archiveID,archive);
		//for reverse mapping
		globalArchiveToArchiveIDMap.put(archive,archiveID);

	}


	private static WeakReference<Archive> getArchiveFromGlobalArchiveMap(String archiveFile){
		return globaldirToArchiveMap.getOrDefault(archiveFile,null);
	}

	//If there is only one archive present in the global map then this funciton returns that
	//else it return null.
	public static Archive getDefaultArchiveFromGlobalArchiveMap(){
		if(globaldirToArchiveMap.size()==1)
			return globaldirToArchiveMap.values().iterator().next().get();
		else
			return null;
	}

	//This function returns the archiveID for the given archive
	public static String getArchiveIDForArchive(Archive archive){

		return globalArchiveToArchiveIDMap.getOrDefault(archive,null);
	}

	//This function returns the archive for the given archiveID
	public static Archive getArchiveForArchiveID(String archiveID){
		return globalArchiveIDToArchiveMap.getOrDefault(archiveID,null);
	}

	/**
	 * reads from default dir (usually ~/.muse/user) and sets up cachedir,
	 * archive vars.
	 */
	public static Archive prepareAndLoadDefaultArchive(HttpServletRequest request) throws IOException
	{
		HttpSession session = request.getSession();

		// allow cacheDir parameter to override default location
		String dir = request.getParameter("cacheDir");
		if (Util.nullOrEmpty(dir))
			dir = SimpleSessions.CACHE_DIR;
		JSPHelper.log.info("Trying to read archive from " + dir);

		Archive archive = SimpleSessions.readArchiveIfPresent(dir);
		if (archive != null)
		{
			JSPHelper.log.info("Good, archive read from " + dir);

		/*	// always set these three together
			session.setAttribute("userKey", "user");
			session.setAttribute("cacheDir", dir);
			session.setAttribute("archive", archive);
*/
			// is this really needed?
			Archive.prepareBaseDir(dir); // prepare default lexicon files etc.
/*
			Lexicon lex = archive.getLexicon("general");
			if (lex != null)
				session.setAttribute("lexicon", lex); // set up default general lexicon, so something is in the session as default lexicon (so facets can show it)
*/
		}
		return archive;
	}

	public static Archive prepareAndLoadArchive(MuseEmailFetcher m, HttpServletRequest request) throws IOException
	{

		// here's where we create a fresh archive
		String userKey = "user";
		/*if (ModeConfig.isServerMode())
		{
			// use existing key, or if not available, ask the fetcher which has the login email addresses for a key
			userKey = (String) session.getAttribute("userKey");
			if (Util.nullOrEmpty(userKey))
				userKey = m.getEffectiveUserKey();
			Util.ASSERT(!Util.nullOrEmpty(userKey)); // disaster if we got here without a valid user key
		}*/

		int i = new Random().nextInt();
		String randomPrefix = String.format("%08x", i);
		String archiveDir = ModeConfig.isProcessingMode()?Config.REPO_DIR_PROCESSING + File.separator + randomPrefix : SimpleSessions.CACHE_BASE_DIR+ File.separator+userKey;
		//String archiveDir = Sessions.CACHE_BASE_DIR + File.separator + userKey;
		Archive archive = SimpleSessions.readArchiveIfPresent(archiveDir);

		if (archive != null) {
			JSPHelper.log.info("Good, existing archive found");
		} else {
			JSPHelper.log.info("Creating a new archive in " + archiveDir);
			archive = JSPHelper.preparedArchive(request, archiveDir, new ArrayList<String>());
		    //by this time the archive is created
			// add this to global maps archiveID->archive, archive->archiveID
			addToGlobalArchiveMap(archiveDir,archive);

		}

		Lexicon lex = archive.getLexicon("general");
	/*	if (lex != null)
			session.setAttribute("lexicon", lex); // set up default general lexicon, so something is in the session as default lexicon (so facets can show it)
	*/
		return archive;
	}

	public static void main(String args[]) throws IOException
	{
		// just use as <basedir> <string to find>
		Archive a = readArchiveIfPresent(args[0]);
		for (Document d : a.getAllDocs())
		{
			String c = a.getContents(d, false);
			if (c.indexOf(args[1]) >= 0)
			{
				System.out.println("\n______________________________" + d + "\n\n" + c + "\n___________________________\n\n\n");
			}
		}

	}

}
