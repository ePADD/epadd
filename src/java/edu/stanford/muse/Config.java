package edu.stanford.muse;

import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.ModeConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/*
VIP class. This class has constants/settings that generally do not change during an ePADD execution, and are set only at startup.
The settings can be public static fields of the Config class and can be read (but should not be written) directly by the rest of the code.
The settings are read by the properties in <user home dir>/epadd.properties (or the file specified by -Depadd.properties=<file>)
Some settings have a default.

Similarly, resource files should be read only through this class. Resource files are not expected to change during one execution of epadd.
 */
public class Config {
    public static final String COLLECTION_METADATA_FILE = "collection-metadata.json"; // all session files end with .session
    public static Log log = LogFactory.getLog(Config.class);
    public static String admin; // this is the admin user for an installation of ePADD

    /* default location for dir under which archives are imported/stored. Should not end in File.separator */
    public final static String	REPO_DIR_APPRAISAL;
    public final static String	REPO_DIR_PROCESSING;
    public final static String	REPO_DIR_DISCOVERY;
    public final static String	REPO_DIR_DELIVERY;

    public static String	SETTINGS_DIR		= System.getProperty("user.home") + File.separator + "epadd-settings" + File.separator;

    public static String 	FAST_INDEX_DIR, AUTHORITIES_FILENAME, AUTHORITIES_CSV_FILENAME, AUTHORITY_ASSIGNER_FILENAME;
    
    //List of resource file that the NER model is trained on
    public static String[] NER_RESOURCE_FILES = new String[0];
    public static String DBPEDIA_INSTANCE_FILE;
    
    public static String	FEATURES_INDEX, TABOO_FILE = "kill.txt";
    
    //this is the folder name that contains the cache for internal authority assignment
    public static int		MAX_ENTITY_FEATURES			= 200;
    public static int		MAX_TRY_TO_RESOLVE_NAMES	= 10;
    public static int		MAX_DOCS_PER_QUERY	= 10000;
    public static int		MAX_TEXT_SIZE_TO_ANNOTATE	= 100000; // messages with bodies longer than this will not be annotated

    public static Boolean 	OPENNLP_NER = false;
    public static String DEFAULT_SETTINGS_DIR = System.getProperty("user.home") + File.separator + "epadd-settings";
    private static String DEFAULT_BASE_DIR = System.getProperty("user.home");
    public static String DEFAULT_LEXICON = "general";
    public static final Map<String, String> attachmentTypeToExtensions = new LinkedHashMap<>(); // must be lower case
    public static final Set<String> allAttachmentExtensions = new LinkedHashSet<>(); // all attachment extensions.

    private static String EPADD_PROPS_FILE = System.getProperty("user.home") + File.separator + "epadd.properties"; // this need not be visible to the rest of ePADD

    static {
        Properties props = readProperties();

        // set up settings_dir
		SETTINGS_DIR = props.getProperty("epadd.settings.dir", DEFAULT_SETTINGS_DIR);

        // Just for mode, we have another ModeConfig class. This should probable be simplified
        // if null or invalid, we'll leave epadd.mode in APPRAISAL which is the default
        {
            String mode = props.getProperty("epadd.mode", "appraisal");
            if ("appraisal".equalsIgnoreCase(mode))
                ModeConfig.mode = ModeConfig.Mode.APPRAISAL;
            else if ("processing".equalsIgnoreCase(mode))
                ModeConfig.mode = ModeConfig.Mode.PROCESSING;
            else if ("discovery".equalsIgnoreCase(mode))
                ModeConfig.mode = ModeConfig.Mode.DISCOVERY;
            else if ("delivery".equalsIgnoreCase(mode))
                ModeConfig.mode = ModeConfig.Mode.DELIVERY;
            else if (mode != null)
                log.warn("Invalid value for epadd.mode: " + mode);
        }

        // set up base_dir and its subdirs
        String BASE_DIR = props.getProperty("epadd.base.dir", DEFAULT_BASE_DIR);
        REPO_DIR_APPRAISAL = BASE_DIR + java.io.File.separator + "epadd-appraisal"; // this needs to be in sync with system property muse.dirname?
        REPO_DIR_PROCESSING = BASE_DIR + java.io.File.separator + "epadd-processing";
        REPO_DIR_DISCOVERY = BASE_DIR + java.io.File.separator + "epadd-discovery";
        REPO_DIR_DELIVERY = BASE_DIR + java.io.File.separator + "epadd-delivery";

        // site-specific settings
        {
            //admin = props.getProperty("admin", "Peter Chan, pchan3@stanford.edu");
            admin = props.getProperty("admin", "epadd_project@stanford.edu");

        }

        // config for file/dir names, etc.
        {
            FEATURES_INDEX = props.getProperty("FEATURES_INDEX", "features");
            AUTHORITIES_FILENAME = props.getProperty("AUTHORITIES_FILENAME", "authorities.ser");
            AUTHORITIES_CSV_FILENAME = props.getProperty("AUTHORITIES_CSV_FILENAME", "authorities.csv");
            AUTHORITY_ASSIGNER_FILENAME = props.getProperty("AUTHORITY_ASSIGNER_FILENAME", "InternalAuthorityAssigner.ser");
            FAST_INDEX_DIR = props.getProperty("fast.index.dir", SETTINGS_DIR + File.separator + "fast_index");
            String rsrcField = props.getProperty("NER_RESOURCE_FILE", "CONLL/lists/ePADD.ned.list.LOC:::CONLL/lists/ePADD.ned.list.PER:::CONLL/lists/ePADD.ned.list.ORG");
            if (rsrcField != null && rsrcField.length() > 0)
                NER_RESOURCE_FILES = rsrcField.split(":::");

            DBPEDIA_INSTANCE_FILE = props.getProperty("DBPEDIA_INSTANCE_FILE", "instance_types_2014-04.en.txt.bz2");
            // set the int mixtures
        }

        String s = props.getProperty("MAX_ENTITY_FEATURES");
        if (s != null) {
            try {
                MAX_ENTITY_FEATURES = Integer.parseInt(s);
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
        s = props.getProperty("MAX_TRY_TO_RESOLVE_NAMES");
        if (s != null) {
            try {
                MAX_TRY_TO_RESOLVE_NAMES = Integer.parseInt(s);
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
        s = props.getProperty("MAX_DOCS_PER_QUERY");
        if (s != null) {
            try {
                MAX_DOCS_PER_QUERY = Integer.parseInt(s);
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
        s = props.getProperty("OPENNLP_NER");
        if (!Util.nullOrEmpty(s))
            OPENNLP_NER = Boolean.parseBoolean(s);

        s = props.getProperty("epadd.default.lexicon", "general");
        if (s != null) {
            DEFAULT_LEXICON = s;
        }

        {
            // should be all lower case, delimited with Util.OR_DELIMITER
            attachmentTypeToExtensions.put("Graphics", "jpg;jpeg;svg;png;gif;bmp;tif");
            attachmentTypeToExtensions.put("Document", "doc;docx;pages");
            attachmentTypeToExtensions.put("Presentation", "ppt;pptx;key");
            attachmentTypeToExtensions.put("Spreadsheet", "xls;xlsx;numbers");
            attachmentTypeToExtensions.put("PDF", "pdf");
            attachmentTypeToExtensions.put("Internet file", "htm;html;css;js");
            attachmentTypeToExtensions.put("Compressed", "zip;7z;tar;tgz");
            attachmentTypeToExtensions.put("Video", "mp3;ogg");
            attachmentTypeToExtensions.put("Audio", "avi;mp4");
            attachmentTypeToExtensions.put("Database", "fmp;db;mdb;accdb");
            attachmentTypeToExtensions.put("Others", "others");

            attachmentTypeToExtensions.keySet().forEach(k -> {
                Collection<String> exts = Util.splitFieldForOr(attachmentTypeToExtensions.get(k));
                if (!"Others".equals(k))
                    allAttachmentExtensions.addAll(exts);
            });
        }

        {
            log.info("-------------Begin Configuration block -----------------");
            log.info("ePADD base dir = " + BASE_DIR);
            log.info("ePADD settings dir = " + SETTINGS_DIR);
            log.info("ePADD mode = " + ModeConfig.mode);
            log.info("FAST index = " + FAST_INDEX_DIR);
            log.info("Admin = " + admin);
            // add more things here if needed
            log.info("-------------End Configuration block -----------------");
        }
    }

    // return properties set from epadd.properties file and/or system properties
    private static Properties readProperties() {
        Properties props = new Properties();

        // EPADD_PROPS_FILE is where the config is read from.
        // default <HOME>/epadd.properties, but can be overridden by system property epadd.properties
        String propsFile = System.getProperty("epadd.properties");
        if (propsFile != null)
            EPADD_PROPS_FILE = propsFile;

        File f = new File(EPADD_PROPS_FILE);
        if (f.exists() && f.canRead()) {
            log.info("Reading configuration from: " + EPADD_PROPS_FILE);
            try {
                InputStream is = new FileInputStream(EPADD_PROPS_FILE);
                props.load(is);
            } catch (Exception e) {
                Util.print_exception("Error reading epadd properties file " + EPADD_PROPS_FILE, e, log);
            }
        } else {
            log.warn("ePADD properties file " + EPADD_PROPS_FILE + " does not exist or is not readable");
        }

        // each individual property can further be overridden from the command line by a system property
        for (String key: props.stringPropertyNames()) {
            String val = System.getProperty (key);
            if (val != null && val.length() > 0)
                props.setProperty(key, val);
        }
        return props;
    }

    /** reads a resource with the given offset path. Resources should be read ONLY with this method, so there is a uniform way of finding and overriding resources.
     * Path components are always separated by forward slashes, just like resource paths in Java.
	 * First looks in settings folder, then on classpath (e.g. inside war's WEB-INF/classes).
	 **/
	public static InputStream getResourceAsStream(String path) {
		File f = new File(SETTINGS_DIR + File.separator + path.replaceAll("/", "\\" + File.separator));
		if (f.exists()) {
			if (f.canRead()) {
				log.info ("Reading resource " + path + " from " + f.getAbsolutePath());
				try {
					return new FileInputStream(f.getAbsoluteFile());
				} catch (FileNotFoundException fnfe) {
					Util.print_exception(fnfe, log);
				}
			}
			else
				log.warn ("Sorry, resource file exists but cannot read it: " + f.getAbsolutePath());
		}

		InputStream is = Config.class.getClassLoader().getResourceAsStream(path);
		if (is == null)
			log.warn ("UNABLE TO READ RESOURCE FILE: " + path);
		return is;
	}
}
