package edu.stanford.muse.webapp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.stanford.muse.Config;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.CorrespondentAuthorityMapper;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.email.MuseEmailFetcher;
import edu.stanford.muse.ie.variants.EntityBook;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Archive.CollectionMetadata;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.Lexicon;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;
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
	 * Change as on Nov 2017-
	 * Earlier the whole archive was serialized and deserialized as one big entity. Now it is broken into
	 * four main parts, Addressbook, entitybook, correspondentAuthorityMapper and the rest of the object
	 * We save all these four components separately in saveArchive. Therefore while reading, we need to read
	 * all those separately from appropriate files.
	 */
	public static Map<String, Object> loadSessionAsMap(String filename, String baseDir, boolean readOnly) throws IOException
	{
		log.info("Loading session from file " + filename + " size: " + Util.commatize(new File(filename).length() / 1024) + " KB");

		ObjectInputStream ois = null;

		// keep reading till eof exception
		Map<String, Object> result = new LinkedHashMap<>();
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
			/*
				Read other three modules of Archive object which were set as transient and hence did not serialize.
				*/
			//file path names of addressbook, entitybook and correspondentAuthorityMapper data.
			String dir = baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR;

			String addressBookPath = dir + File.separatorChar + Archive.ADDRESSBOOK_SUFFIX;
			String entityBookPath = dir + File.separatorChar + Archive.ENTITYBOOK_SUFFIX;
			String cAuthorityPath =  dir + File.separatorChar + Archive.CAUTHORITYMAPPER_SUFFIX;
			String labMapDirPath= dir + File.separatorChar + Archive.LABELMAPDIR;


			//Error handling: For the case when epadd is running first time on an archive that was not split it is possible that
			//above three files are not present. In that case start afresh with importing the email-archive again in processing mode.
			if(!(new File(addressBookPath).exists()) || !(new File(entityBookPath).exists()) || !(new File(cAuthorityPath).exists())){
				result.put("archive", null);
				return result;
			}


			/////////////////AddressBook////////////////////////////////////////////
			BufferedReader br = new BufferedReader(new FileReader(addressBookPath));
			AddressBook ab = AddressBook.readObjectFromStream(br);
			archive.addressBook = ab;
			br.close();
			////////////////EntityBook/////////////////////////////////////
			br = new BufferedReader(new FileReader(entityBookPath));
			EntityBook eb = EntityBook.readObjectFromStream(br);
			archive.setEntityBook(eb);
			br.close();
			///////////////CorrespondentAuthorityMapper/////////////////////////////
			CorrespondentAuthorityMapper cmapper = null;
			try {
				//Cast is no issue as there is no extra field in the inherited class CorrespondentAuthorityMapper.
				cmapper = (CorrespondentAuthorityMapper)CorrespondentAuthorityMapper.deserializeObjectFromFile(cAuthorityPath);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			archive.correspondentAuthorityMapper = cmapper;
			/////////////////Label Mapper/////////////////////////////////////////////////////
			LabelManager labelManager = null;
			try {
				labelManager = LabelManager.readObjectFromStream(labMapDirPath);
			} catch (Exception e) {
				Util.print_exception ("Exception in reading label manager from archive, assigning a new label manager", e, log);
				labelManager = new LabelManager();
			}

			archive.setLabelManager(labelManager);
			///////////////Processing metadata////////////////////////////////////////////////
			// override the PM inside the archive with the one in the PM file
			//update: since v5 no pm will be inside the archive.
			// this is useful when we import a legacy archive into processing, where we've updated the pm file directly, without updating the archive.
			try {
				archive.collectionMetadata = readCollectionMetadata(baseDir);
			} catch (Exception e) {
				Util.print_exception ("Error trying to read processing metadata file", e, log);
			}
			/////////////////////////////Done reading//////////////////////////////////////////////////////
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
	public static Archive.CollectionMetadata readCollectionMetadata(String baseDir) {
		String processingFilename = baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separatorChar + Config.COLLECTION_METADATA_FILE;
		try (Reader reader = new FileReader(processingFilename)) {
			CollectionMetadata metadata = new Gson().fromJson(reader, Archive.CollectionMetadata.class);
			return metadata;
		} catch (Exception e) {
			Util.print_exception("Unable to read processing metadata from file" + processingFilename, e, log);
		}
		return null;
	}

    /**
     * writes name.processing.metadata to the given basedir. should be used
     * when quick archive metadata is needed without loading the actual archive
     * basedir is up to /.../sessions
	 * v5- Instead of serializing now this data gets stored in json format.
     */
    public static void writeCollectionMetadata(Archive.CollectionMetadata cm, String baseDir)
    {
        String processingFilename = baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separatorChar + Config.COLLECTION_METADATA_FILE;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileWriter fwriter = null;
		try {
			fwriter = new FileWriter(processingFilename);
			gson.toJson(cm,fwriter);
		} catch (IOException e) {
			Util.print_exception("Unable to write processing metadata", e, log);
			e.printStackTrace();
		}finally {
			if(fwriter!=null){
				try {
					fwriter.close();
				} catch (IOException e) {
					Util.print_exception("Unable to write processing metadata", e, log);
					e.printStackTrace();
				}
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
		/*//file path names of addressbook, entitybook and correspondentAuthorityMapper data.
		String addressBookPath = dir + File.separatorChar + Archive.ADDRESSBOOK_SUFFIX;
		String entityBookPath = dir + File.separatorChar + Archive.ENTITYBOOK_SUFFIX;
		String cAuthorityPath =  dir + File.separatorChar + Archive.CAUTHORITYMAPPER_SUFFIX;
		*/
		if (archive.collectionMetadata == null)
			archive.collectionMetadata = new Archive.CollectionMetadata();

		archive.collectionMetadata.timestamp = new Date().getTime();
		archive.collectionMetadata.tz = TimeZone.getDefault().getID();
		archive.collectionMetadata.nDocs = archive.getAllDocs().size();
		archive.collectionMetadata.nUniqueBlobs = archive.blobStore.uniqueBlobs.size();

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

        archive.collectionMetadata.firstDate = firstDate;
        archive.collectionMetadata.lastDate = lastDate;
        archive.collectionMetadata.nIncomingMessages = receivedMessages;
        archive.collectionMetadata.nOutgoingMessages = sentMessages;
		archive.collectionMetadata.nHackyDates = hackyDates;

        archive.collectionMetadata.nBlobs = totalAttachments;
        archive.collectionMetadata.nUniqueBlobs = archive.blobStore.uniqueBlobs.size();
        archive.collectionMetadata.nImageBlobs = images;
        archive.collectionMetadata.nDocBlobs = docs;
        archive.collectionMetadata.nOtherBlobs = others;

		try (ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)))) {
			oos.writeObject("archive");
			oos.writeObject(archive);
		} catch (Exception e1) {
			Util.print_exception("Failed to write archive: ", e1, log);
		}

		//Now write modular transient fields to separate files-
		//By Dec 2017 there are three transient fields which will be saved and loaded separately
		//1. AddressBook -- Stored in a gzip file with name in the same directory as of archive.
		//2. EntityBook
		//3. CorrespondentAuthorityMapper
		/////////////////AddressBook Writing -- In human readable form ///////////////////////////////////
		SimpleSessions.saveAddressBook(archive);
		////////////////EntityBook Writing -- In human readable form/////////////////////////////////////
		SimpleSessions.saveEntityBook(archive);
		///////////////CAuthorityMapper Writing-- Serialized///////////////////////////////
		SimpleSessions.saveCorrespondentAuthorityMapper(archive);
		//////////////LabelManager Writing -- Serialized//////////////////////////////////
		SimpleSessions.saveLabelManager(archive);

        writeCollectionMetadata(archive.collectionMetadata, baseDir);

        /*

        // now write out the metadata
		String processingFilename = dir + File.separatorChar + name + Config.COLLECTION_METADATA_FILE;
		oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(processingFilename)));
		try {
			oos.writeObject(archive.collectionMetadata);
		} catch (Exception e1) {
            Util.print_exception("Failed to write archive's metadata: ", e1, log);
			oos.close();
		} finally {
			oos.close();
		}
		*/
/*

		if (archive.correspondentAuthorityMapper!= null) {
			String authorityMapperFilename = dir + File.separatorChar + name + Config.AUTHORITIES_FILENAME;
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(authorityMapperFilename)));
			try {
				oos.writeObject(archive.correspondentAuthorityMapper);
			} catch (Exception e1) {
				Util.print_exception("Failed to write archive's authority mapper: ", e1, log);
				oos.close();
			} finally {
				oos.close();
			}
		}
*/
		archive.close();

		// re-open for reading
		archive.openForRead();

        // note: no need of saving archive authorities separately -- they are already saved as part of the archive object
		return true;
	}

	public static void saveAddressBook(Archive archive){

		String dir = archive.baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
		new File(dir).mkdirs(); // just to be safe
		//file path name of addressbook data.
		String addressBookPath = dir + File.separatorChar + Archive.ADDRESSBOOK_SUFFIX;
		log.info("Saving addressBook to file " + addressBookPath);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(addressBookPath));
			archive.addressBook.writeObjectToStream(bw,false);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void saveEntityBook(Archive archive){
		String dir = archive.baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
		//file path name of entitybook
		String entityBookPath = dir + File.separatorChar + Archive.ENTITYBOOK_SUFFIX;
		log.info("Saving entity book to file " + entityBookPath);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(entityBookPath));
			archive.getEntityBook().writeObjectToStream(bw);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void saveCorrespondentAuthorityMapper(Archive archive){
		String dir = archive.baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
		//file path name of entitybook
		String cAuthorityPath = dir + File.separatorChar + Archive.CAUTHORITYMAPPER_SUFFIX;
		log.info("Saving entity book to file " + cAuthorityPath);

		try {
			archive.getCorrespondentAuthorityMapper().serializeObjectToFile(cAuthorityPath);
		} catch (ParseException | IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void saveLabelManager(Archive archive){
		String dir = archive.baseDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
		//file path name of labelMap file
		String labMapDir = dir + File.separatorChar + Archive.LABELMAPDIR;
		new File(labMapDir).mkdir();//create dir if not exists.
		log.info("Saving label mapper to directory " + labMapDir);
		archive.getLabelManager().writeObjectToStream(labMapDir);
	}



	// an archive in a given dir should be loaded only once into memory.
	// this map stores the directory -> archive mapping.
	private static LinkedHashMap<String, WeakReference<Archive>> globaldirToArchiveMap = new LinkedHashMap<>();
	private static LinkedHashMap<String,Archive> globalArchiveIDToArchiveMap = new LinkedHashMap<>();
	private static LinkedHashMap<Archive,String> globalArchiveToArchiveIDMap = new LinkedHashMap<>();

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
		String pmFile = baseDir + File.separator + Archive.SESSIONS_SUBDIR + File.separator + Config.COLLECTION_METADATA_FILE;

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
				//check if the loaded archive satisfy the verification condtiions. Call verify method on archive.
				JSPHelper.log.info("After reading the archive checking if it is in good shape");
				a.Verify();

				return a;
			}
		} catch (Exception e) {
			Util.print_exception("Error reading archive from dir: " + archiveFile, e, log);
			throw new RuntimeException(e);
		}
	}

	private static String removeTrailingSlashFromDirName(String dir){
		return new File(dir).getAbsolutePath();
	}

	public static void addToGlobalArchiveMap(String archiveDir, Archive archive){

		String s = removeTrailingSlashFromDirName(archiveDir);
		//add to globalDirmap
		globaldirToArchiveMap.put(s, new WeakReference<>(archive));
		//construct archive ID from the tail of the archiveFile (by sha1)
		String archiveID = Util.hash(s);
		globalArchiveIDToArchiveMap.put(archiveID,archive);
		//for reverse mapping
		globalArchiveToArchiveIDMap.put(archive,archiveID);

	}

	public static void removeFromGlobalArchiveMap(String archiveDir, Archive archive){
		String s = removeTrailingSlashFromDirName(archiveDir);
		globaldirToArchiveMap.remove(s);
		//remove from reverse mapping but first get the archive ID.
		String archiveID = globalArchiveToArchiveIDMap.get(archive);
		globalArchiveToArchiveIDMap.remove(archive);
		//remove from archiveID to archive mapping.
		if(!Util.nullOrEmpty(archiveID))
			globalArchiveIDToArchiveMap.remove(archiveID);
	}


	private static WeakReference<Archive> getArchiveFromGlobalArchiveMap(String archiveFile){
		String s = removeTrailingSlashFromDirName(archiveFile);
		return globaldirToArchiveMap.getOrDefault(s,null);
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
			archive = JSPHelper.preparedArchive(request, archiveDir, new ArrayList<>());
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
			if (c.contains(args[1]))
			{
				System.out.println("\n______________________________" + d + "\n\n" + c + "\n___________________________\n\n\n");
			}
		}

	}

}
