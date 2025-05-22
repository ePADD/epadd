
/*
 * Copyright (C) 2012 The Stanford MobiSocial Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.stanford.muse.webapp;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.epadd.util.OperationInfo;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.LabelManager.Label;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.*;
import edu.stanford.muse.epaddpremis.EpaddEvent;
import edu.stanford.muse.exceptions.CancelledException;
import edu.stanford.muse.exceptions.NoDefaultFolderException;
import edu.stanford.muse.index.*;
import edu.stanford.muse.ner.NER;
import edu.stanford.muse.ner.model.DummyNERModel;
import edu.stanford.muse.ner.model.LoadResultsModel;
import edu.stanford.muse.ner.model.NBModel;
import edu.stanford.muse.ner.model.NERModel;
import edu.stanford.muse.util.DetailedFacetItem;
import edu.stanford.muse.util.Log4JUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTimeComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/* import javax.servlet.jsp.JspWriter; */

/**

 * helper code that would otherwise clutter JSPs. its ok for this file to handle

 * HTML/HTTP.
 * All HTML/HTTP and presentation Java code must be present only in this file,
 * because the rest of the Muse code can be used in other, non-web apps.
 */

public class JSPHelper {
	public static final Logger		log						= LogManager.getLogger(JSPHelper.class);

	public static void doLogging(Object o)
	{
		log.info(o);
		System.out.println("Do logging: " + o);
	}
	public static void doLoggingWarnings(Object o)
	{
		log.warn(o);
		System.out.println("Do warning logging: " + o);
	}
	public static void doLoggingError(Object o)
	{
		log.error(o);
		System.out.println("Do error logging: " + o);
	}
	// jetty processes request params differently from tomcat
	// tomcat assumes incoming params (both get/post) are iso8859.
	// jetty assumes utf-8.
	// so if we are not running jetty, we convert the string from iso8859 to utf8, which is what we eventually want.
	// can set RUNNING_ON_JETTY in either of 2 ways:
	// define -Dmuse.container=jetty
	// check request.getSession().getServletContext().getServerInfo()

	private static boolean	RUNNING_ON_JETTY		= false;

	static {
		// log4j initialize should be called before anything else happens. this is the best place to initialize
		//IMP: When muse.jar (jar carved out from epadd war) is included in other projects and we do not want these log
		//settings to override that project specific settings then we disable the call for this initialization.
		Log4JUtils.initialize();
		// check container
		String container = System.getProperty("muse.container");
		if ("jetty".equals(container))
		{
			RUNNING_ON_JETTY = true;
			log.info("Running on the Jetty web server (due to system property muse.container: " + RUNNING_ON_JETTY + ")");
		}
	}

	public static void checkContainer(HttpServletRequest req)
	{
		String serverInfo = req.getSession().getServletContext().getServerInfo();
		log.info("Running inside container: " + serverInfo);
		if (serverInfo.toLowerCase().contains("jetty"))
			RUNNING_ON_JETTY = true;
		log.info("Running on jetty: " + RUNNING_ON_JETTY);
	}

	private static void checkContainer(HttpSession session)
	{
		String serverInfo = session.getServletContext().getServerInfo();
		log.info("Running inside container: " + serverInfo);
		if (serverInfo.toLowerCase().contains("jetty"))
			RUNNING_ON_JETTY = true;
		log.info("Running on jetty: " + RUNNING_ON_JETTY);
	}
	/**
	 * gets the LOCAL/CLIENT session attribute (to make explicit distinction
	 * from shared/global/server session)
	 */
	private static Object getHttpSessionAttribute(HttpSession session, String attr_name)
	{
		return session.getAttribute(attr_name); // intentional use of session . getSessionAttribute()
	}

	private static String getUserKey(HttpSession session)
	{
		String userKey = (String) getHttpSessionAttribute(session, "userKey");
		// do sanitizeFileName for security reason also
		return Util.sanitizeFileName(userKey);
	}

	/** gets the cache (base) dir for the current session */
	private static String getBaseDir(MuseEmailFetcher m,  HttpSession session)
	{

		if (session == null)
			return SimpleSessions.getDefaultCacheDir();
		else
		{
			String baseDir = (String) getHttpSessionAttribute(session, "cacheDir");
			// if we have an existing cache dir, use it
			if (!Util.nullOrEmpty(baseDir))
				return baseDir;

			String userKey = "user";

			if (ModeConfig.isMultiUser()) // (!runningOnLocalhost(request))
			{
				Collection<EmailStore> emailStores = m.emailStores;
				if (Util.nullOrEmpty(emailStores))
					return "";

				// go through the stores till we have a non-empty key
				for (EmailStore store : emailStores)
				{
					String e = store.emailAddress;
					if (!Util.nullOrEmpty(e))
					{
						userKey = e;
						break;
					}
				}
			}

			session.setAttribute("userKey", userKey); // this is needed for the root dir for piclens
			// now we have the user key
			baseDir = SimpleSessions.getDefaultRootDir() + File.separator + getUserKey(session);
			log.info("base dir set to " + baseDir);
			return baseDir;
		}
	}

	public static Archive getArchive(HttpServletRequest request)
	{
		//get archiveID from request parameter..
		String archiveID = request.getParameter("archiveID");
		if(archiveID==null){
			//if only single archive is present in the map then return that
			//else return null after logging this information.
//			Archive onlyleft = SimpleSessions.getDefaultArchiveFromGlobalArchiveMap();
//			if(onlyleft==null){
				log.debug("No archive ID parameter passed from the client");
			//present for the given archive ID. No default archive could be loaded");
				return null;
//			}else{
//				log.debug("No archiveID parameter passed from the client. Loading the default archive.");
//				return onlyleft;
//			}
		}
		return ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
	}

	public static Archive getArchive(Multimap<String,String> params)
	{
		//get archiveID from request parameter..
		String archiveID = JSPHelper.getParam(params,"archiveID");
		if(archiveID==null){
			//if only single archive is present in the map then return that
			//else return null after logging this information.
//			Archive onlyleft = SimpleSessions.getDefaultArchiveFromGlobalArchiveMap();
//			if(onlyleft==null){
			log.debug("No archive ID parameter passed from the client");
			//present for the given archive ID. No default archive could be loaded");
			return null;
//			}else{
//				log.debug("No archiveID parameter passed from the client. Loading the default archive.");
//				return onlyleft;
//			}
		}
		return ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
	}
	/**
	 * gets the session attribute - look up from the client-side session storage
	 * first then the server-side.
	 * session name (archive name) is determined by attribute ARCHIVE_NAME_STR`
	 * of the client-side session.
	 */
	public static Object getSessionAttribute(HttpSession session, String attr_name)
	{
			Object value = getHttpSessionAttribute(session, attr_name);
		return value;
		//		if (value != null) return value;

		//		now should not need to special case for !ModeConfig.isPublicMode().
		//		if required, should perhaps special case for !"archive".equals(attr_name) instead.
		//		// not loading global sessions unless public mode
		//		if (!ModeConfig.isPublicMode())
		//			return null;

		//		Map<String, Object> s = null;
		//		try {
		//			s = Sessions.getGlobalSession(session);
		//		} catch (Exception e) { Util.print_exception(e, log); }
		//
		//		if (s != null) return s.get(attr_name);
		//
		//		return null;
	}

	/**
	 * converts an array of strings from iso-8859-1 to utf8. useful for
	 * converting i18n chars in http request parameters.
	 * if throwExceptionIfUnsupportedEncoding is true, throws an exception,
	 * otherwise returns
	 */
	private static String[] convertRequestParamsToUTF8(String params[], boolean throwExceptionIfUnsupportedEncoding) throws UnsupportedEncodingException
	{
		if (RUNNING_ON_JETTY)
			return params;
		if (params == null)
			return null;

		// newParams will contain only the strings that successfully can be converted to utf-8
		// others will be reported and ignored
		List<String> newParams = new ArrayList<>();
		for (String param : params) {
			try {
				newParams.add(convertRequestParamToUTF8(param));
			} catch (UnsupportedEncodingException e) {
				log.warn("Unsupported encoding exception for " + param);
				Util.print_exception(e, log);
				if (throwExceptionIfUnsupportedEncoding)
					throw (e);
				// else swallow it
			}
		}
		return newParams.toArray(new String[newParams.size()]);
	}

	public static String[] convertRequestParamsToUTF8(String params[]) throws UnsupportedEncodingException
	{
		return convertRequestParamsToUTF8(params, true);
	}

	// key finding after a lot of experimentation with jetty and tomcat.
	// make all pages UTF-8 encoded.
	// setRequestEncoding("UTF-8") before reading any parameter
	// even with this, with tomcat, GET requests are iso-8859-1.
	// so convert in that case only...
	// converts an array of strings from iso-8859-1 to utf8. useful for converting i18n chars in http request parameters
	public static String convertRequestParamToUTF8(String param) throws UnsupportedEncodingException
	{
		if (RUNNING_ON_JETTY)
		{
			log.info("running on jetty: no conversion for " + param);
			return param;
		}
		if (param == null)
			return null;

		//6.3.2023 There was an issue not being able to import Mbox folders with names
		//containing accented characters.
		//String newParam = new String(param.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		String newParam = new String(param);

		if (!newParam.equals(param))
			log.info("Converted to utf-8: " + param + " -> " + newParam);
		return newParam;
	}

	public static boolean runningOnLocalhost(HttpServletRequest request)
	{
		//return "localhost".equals(request.getServerName());
		return !ModeConfig.isMultiUser();
	}

	//	/* this version of fetchemails must have folders defined in request since there is no primary email address */
	//	public static Triple<Collection<EmailDocument>, AddressBook, BlobStore> fetchEmails(HttpServletRequest request, HttpSession session, boolean download) throws Exception
	//	{
	//		return fetchEmails (request, session, download, /* downloadattachments = */ false, false);
	//	}
	//
	//	/** fetches messages without downloading or attachments.
	//	 * support default folder for primary email address */
	//	public static Triple<Collection<EmailDocument>, AddressBook, BlobStore> fetchEmails(HttpServletRequest request, HttpSession session, String primaryEmailAddress) throws Exception
	//	{
	//		return fetchEmails (request, session, false, false, false);
	//	}
	//
	//	public static boolean fetchEmailsDefaultFolders(HttpServletRequest request, HttpSession session, boolean downloadMessageText, boolean downloadAttachments) throws Exception
	//	{
	//		try {
	//			fetchEmails(request, session, downloadMessageText, downloadAttachments, true);
	//		} catch (Exception e) {
	//			return false;
	//		}
	//		return true;
	//	}
	//
	//	public static Triple<Collection<EmailDocument>, AddressBook, BlobStore> fetchEmails(HttpServletRequest request, HttpSession session, boolean downloadMessageText, boolean downloadAttachments, boolean useDefaultFolders)
	//			throws UnsupportedEncodingException, MessagingException, InterruptedException, IOException, JSONException, NoDefaultFolderException, CancelledException
	//	{
	//		return fetchEmails(request, session, downloadMessageText, downloadAttachments, useDefaultFolders, null);
	//	}

	/**
	 * A VIP method.
	 * reads email accounts and installs addressBook and emailDocs into session
	 * useDefaultFolders: use the default folder for that fetcher if there are
	 * no explicit folders in that fetcher.
	 * throws out of memory error if it runs out of memory.
	 *
	 * @throws JSONException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 * @throws NoDefaultFolderException
	 * @throws Exception
	 */
	public static void fetchAndIndexEmails(Archive archive, MuseEmailFetcher m, Multimap<String, String> paramsMap, HttpSession session, boolean downloadMessageText, boolean downloadAttachments, boolean useDefaultFolders, Consumer<StatusProvider> setStatusProvider)
			throws MessagingException, InterruptedException, IOException, JSONException, NoDefaultFolderException, CancelledException, OutOfMemoryError {
        // first thing, set up a static status so user doesn't see a stale status message
        //session.setAttribute("statusProvider", new StaticStatusProvider("Starting up..."));
		setStatusProvider.accept(new StaticStatusProvider("Starting up..."));
        checkContainer(session);//initially it was checkContainer(request)
        //String encoding = request.getCharacterEncoding();
        //log.info("request parameter encoding is " + encoding);

        if (!downloadMessageText)
            if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"downloadMessageText"))) {
                downloadMessageText = true;
                log.info("Downloading message text because advanced option was set");
            }

        if (!downloadAttachments)
            if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"downloadAttachments"))) {
                downloadAttachments = true;
                downloadMessageText = true; // because text is needed for attachment wall -- otherwise we can't break out from piclens to browsing messages associated with a particular thumbnail
                log.info("Downloading attachments because advanced option was set");
            }

		String[] allFolders = JSPHelper.getParams(paramsMap, "folder").stream().toArray(String[]::new);
        if (allFolders != null) {
            // try to read folder strings, first checking for exceptions
            try {
                allFolders = JSPHelper.convertRequestParamsToUTF8(allFolders, true);
            } catch (UnsupportedEncodingException e) {
                // report exception and try to read whatever folders we can, ignoring the exception this time
                log.warn("Unsupported encoding exception: " + e);
                try {
                    allFolders = JSPHelper.convertRequestParamsToUTF8(allFolders, false);
                } catch (UnsupportedEncodingException e1) {
                    log.warn("Should not reach here!" + e1);
                }
            }
        }

        Filter filter = Filter.parseFilter(paramsMap);
        // if required, forceEncoding can go into fetch config
        //	String s = (String) session.getAttribute("forceEncoding");
        FetchConfig fc = new FetchConfig();
        fc.downloadMessages = downloadMessageText;
        fc.downloadAttachments = downloadAttachments;
        fc.filter = filter;

        archive.setBaseDir(getBaseDir(m, session));
        m.fetchAndIndexEmails(archive, allFolders, useDefaultFolders, fc, session, setStatusProvider);
        //make sure the archive is dumped at this point
        archive.close();
        archive.openForRead();

        //perform entity IE related tasks only if the message text is available
        if (downloadMessageText) {
            String modelFile = NBModel.MODEL_FILENAME;
            NERModel nerModel=null;
            //=(SequenceModel) session.getAttribute("ner");
            //session.setAttribute("statusProvider", new StaticStatusProvider("Loading openNLPNER sequence model from resource: " + modelFile + "..."));
            setStatusProvider.accept(new StaticStatusProvider("Loading Bayesian classifier from resource: " + modelFile + "..."));
            {
                if (System.getProperty("muse.dummy.ner") != null) {
                    log.info("Using dummy openNLPNER model, all CIC patterns will be treated as valid entities");
                    nerModel = new DummyNERModel();
                } else {
                    log.info("Loading entities from external file");
                    nerModel = new LoadResultsModel();
                }
            }
            if (nerModel == null) {
                log.error("Could not load Bayesian classifier model from: " + modelFile);
            } else {
                NER ner = new NER(archive, nerModel);
                //session.setAttribute("statusProvider", ner);
                setStatusProvider.accept(ner);
				ner.recognizeArchive();
                //Here, instead of getting the count of all entities (present in ner.stats object)
				//get the count of only those entities which pass a given threshold.
				//This is to fix a bug where the count of person entities displayed on browse-top.jsp
				//page was different than the count of entities actually displayed following a threshold.
				// @TODO make it more modular
                //archive.collectionMetadata.entityCounts = ner.stats.counts;
				double theta = 0.001;
				archive.collectionMetadata.entityCounts = archive.getEntityBookManager().getEntitiesCountMapModuloThreshold(theta);//getEntitiesCountMapModuloThreshold(archive,theta);
                log.info(ner.stats);
            }
           // archive.collectionMetadata.numPotentiallySensitiveMessages = archive.numMatchesPresetQueries();
            log.info("Number of potentially sensitive messages " + archive.collectionMetadata.numPotentiallySensitiveMessages);

            //Is there a reliable and more proper way of checking the mode it is running in?
            String logF = System.getProperty("muse.log");
            if (logF == null || logF.endsWith("epadd.log")) {
           //     try {
//                InternalAuthorityAssigner assignauthorities = new InternalAuthorityAssigner();
//                session.setAttribute("statusProvider", assignauthorities);
//                assignauthorities.initialize(archive);
//                if (!assignauthorities.isCancelled())
//                    request.getSession().setAttribute("authorities", assignauthorities);
//                else
//                    assignauthorities = null;
//                boolean success = assignauthorities.checkFeaturesIndex(archive, true);
//                if (!success) {
//                    log.warn("Could not build context mixtures for entities");
//                } else
//                    log.info("Successfully built context mixtures for entities");
           //     } catch (Exception e) {
            //        log.warn("Exception while building context mixtures", e);
             //   }
            }
        }
        // add the new stores

		// we add the following code to support file metada requirement in epadd+ project
// 2022-09-05	Added handling for IMAP
//		archive.collectionMetadata.setFileMetadatas(archive, allFolders);
      //  if (archive.isMBOX())
        	archive.collectionMetadata.setFileMetadatas(archive, allFolders);

        // IMAP not currently supported
     //   else archive.collectionMetadata.setFileMetadatasIMAP(archive, allFolders);
        archive.printEpaddPremis();
    }

	/*
	 * creates a new blob store object from the given location (may already
	 * exist) and returns it
	 */
	private static BlobStore preparedBlobStore(String baseDir) throws IOException
	{
		// always set up attachmentsStore even if we are not fetching attachments
		// because the user may already have stuff isn it -- if so, we should make it available.
		String attachmentsStoreDir = baseDir + File.separatorChar +  Archive.BLOBS_SUBDIR + File.separator;
		BlobStore attachmentsStore;
		try {
			File f =  new File(attachmentsStoreDir);
			f.mkdirs(); // the return value is not relevant
			if (!f.exists() || !f.isDirectory() || !f.canWrite())
				throw new IOException("Unable to create directory for writing: " + attachmentsStoreDir);
			attachmentsStore = new BlobStore(attachmentsStoreDir);
		} catch (IOException ioe) {
			log.error("MAJOR ERROR: Disabling attachments because unable to initialize attachments store in directory: " + attachmentsStoreDir + " :" + ioe + " " + Util.stackTrace(ioe));
            attachmentsStore = null;
			throw (ioe);
		}
		return attachmentsStore;
	}

	/** creates a new archive and returns it */
	public static Archive preparedArchive(Multimap<String,String> paramsMap, String baseDir, List<String> extraOptions) throws IOException
	{
		List<String> list = new ArrayList<>();

		if (paramsMap != null)
		{
			if ("yearly".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"period")))
				list.add("-yearly");

			if (JSPHelper.getParam(paramsMap,"noattachments") != null)
				list.add("-noattachments");

			// filter params
			if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"sentOnly")))
				list.add("-sentOnly");

			String str = JSPHelper.getParam(paramsMap,"dateRange");
			if (str != null && str.length() > 0)
			{
				list.add("-date");
				list.add(str);
			}
			String keywords = JSPHelper.getParam(paramsMap,"keywords");
			if (keywords != null && !keywords.equals(""))
			{
				list.add("-keywords");
				list.add(keywords);
			}

			String filter = JSPHelper.getParam(paramsMap,"filter");
			if (filter != null && !filter.equals(""))
			{
				list.add("-filter");
				list.add(filter);
			}
			// end filter params

			// advanced options
			if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"incrementalTFIDF")))
				list.add("-incrementalTFIDF");
			if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"openNLPNER")))
				list.add("-openNLPNER");
			if (!"true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"allText")))
				list.add("-noalltext");
			if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"locationsOnly")))
				list.add("-locationsOnly");
			if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"orgsOnly")))
				list.add("-orgsOnly");
			if ("true".equalsIgnoreCase(JSPHelper.getParam(paramsMap,"includeQuotedMessages")))
				list.add("-includeQuotedMessages");
			String subjWeight = JSPHelper.getParam(paramsMap,"subjectWeight");
			if (subjWeight != null)
			{
				list.add("-subjectWeight");
				list.add(subjWeight);
			}
		}

		if (!Util.nullOrEmpty(extraOptions))
			list.addAll(extraOptions);

		String[] s = new String[list.size()];
		list.toArray(s);

		// careful about the ordering here.. first setup, then read indexer, then run it
		Archive archive = Archive.createArchive();
        BlobStore blobStore = JSPHelper.preparedBlobStore(baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER);
        archive.setup(baseDir, blobStore, s);
		log.info("archive setup in " + baseDir);
		return archive;
	}


	public static void setOperationInfo(HttpSession session, String opID, OperationInfo operationInfo){
		Map<String,OperationInfo> operationInfoMap = (Map<String,OperationInfo>) session.getAttribute("operationInfoMap");
		if(operationInfoMap==null)
			operationInfoMap = new LinkedHashMap<>();
		operationInfoMap.put(opID,operationInfo);
		session.setAttribute("operationInfoMap",operationInfoMap);

	}

	public static OperationInfo getOperationInfo(HttpSession session, String opID){
		Map<String,OperationInfo> operationInfoMap = (Map<String,OperationInfo>) session.getAttribute("operationInfoMap");
		if(operationInfoMap==null)
			return null;
		return operationInfoMap.get(opID);
	}



	public static String getURLWithParametersFromRequestParam(HttpServletRequest request,String exclude){

		StringBuilder sb = new StringBuilder(request.getRequestURL()+"?");

		Map<String, String[]> rpMap = request.getParameterMap();
		for (Object o : rpMap.keySet()) {
			String str1 = (String) o;
			if(str1.trim().toLowerCase().equals(exclude.trim().toLowerCase()))
				continue;
			//sb.append(str1 + "=");
			String[] vals = rpMap.get(str1);
			if (vals.length == 1) {
				sb.append(str1+"=");
				try
				{
					sb.append(Util.ellipsize(URLEncoder.encode(vals[0], StandardCharsets.UTF_8.toString()), 100));
				}
				catch (Exception e)
				{
					log.warn("Exception encoding string in getURLWithParametersFromRequestParam. " + e);
					sb.append(Util.ellipsize(vals[0], 100));
				}
				sb.append("&");
			}
			else {
				for (String x : vals) {
					sb.append(str1+"=");
					sb.append(Util.ellipsize(x, 100));
					sb.append("&");
				}
			}
		}
		return sb.toString();
	}

	/** also sets current thread name to the path of the request */
	private static String getRequestDescription(HttpServletRequest request, boolean includingParams)
	{
		HttpSession session = request.getSession();
		String page = request.getServletPath();
		Thread.currentThread().setName(page);
		String userKey = (String) session.getAttribute("userKey");
		StringBuilder sb = new StringBuilder("Request[" + userKey + "@" + request.getRemoteAddr() + "]: " + request.getRequestURL());
		// return here if params are not to be included
		if (!includingParams)
			return sb.toString();

		String link = request.getRequestURL() + "?";

		Map<String, String[]> rpMap = request.getParameterMap();
		if (rpMap.size() > 0)
			sb.append(" params: ");
		for (Object o : rpMap.keySet())
		{
			String str1 = (String) o;
			sb.append(str1 + " -> ");
			if (str1.startsWith("password"))
			{
				sb.append("*** ");
				continue;
			}

			String[] vals = rpMap.get(str1);
			if (vals.length == 1)
				sb.append(Util.ellipsize(vals[0], 100));
			else
			{
				sb.append("{");
				for (String x : vals)
					sb.append(Util.ellipsize(x, 100) + ",");
				sb.append("}");
			}
			sb.append(" ");

			for (String val : vals)
				link += (str1 + "=" + vals[0] + "&");
		}

		sb.append(" link: " + link);
		return sb.toString();
	}

	public static void logRequest(HttpServletRequest request)
	{
		log.info("NEW " + getRequestDescription(request, true));
	}

	public static void logRequestComplete(HttpServletRequest request)
	{
		log.info("COMPLETED " + getRequestDescription(request, true));
		String page = request.getServletPath();
		Thread.currentThread().setName("done-" + page);
	}

	public static Map<String,String> convertRequestToMapMultipartData(HttpServletRequest request) throws FileUploadException, IOException {
		Map<String, String> params = new LinkedHashMap<>();

		DiskFileItemFactory factory = new DiskFileItemFactory();
		File repository = new File(System.getProperty("java.io.tmpdir"));
		factory.setRepository(repository);
		ServletFileUpload upload = new ServletFileUpload(factory);

		int filesUploaded = 0;
		// Parse the request
		String error = null;
		String archiveID=null;
		InputStream istream = null;
		String docsetID=null;
		List<FileItem> items = upload.parseRequest(request);
		for (FileItem item : items) {
			if (item.isFormField()) {
				params.put(item.getFieldName(), item.getString());
//				if ("archiveID".equals(item.getFieldName()))
//					archiveID = item.getString();
			} else {
				//store the filecontent in a temp file and put the filename as second argument.
				String tmp_file = Archive.TEMP_SUBDIR + File.separator + item.getFieldName();
				Util.copy_stream_to_file(item.getInputStream(), tmp_file);
				params.put(item.getFieldName(), tmp_file);
			}
		}

		return params;
	}

	public static Multimap<String, String> convertRequestToMap(HttpServletRequest request) throws UnsupportedEncodingException {
		Multimap<String, String> params = LinkedHashMultimap.create();
		{
			if (true) {
				// regular file encoding
				Enumeration<String> paramNames = request.getParameterNames();

				while (paramNames.hasMoreElements()) {
					String param = paramNames.nextElement();
					String[] vals = request.getParameterValues(param);
					if (vals != null)
						for (String val : vals)
							params.put(param, JSPHelper.convertRequestParamToUTF8(val));
				}
			}
		}

		return params;
	}

	/** returns a single value for the given key */
	public static String getParam(Multimap<String, String> params, String key) {
		Collection<String> values = params.get(key);
		if (values.size() == 0)
			return null;
		return values.iterator().next();
	}


	/** returns multiple value for the given key */
	public static Collection<String> getParams(Multimap<String, String> params, String key) {
		Collection<String> values = params.get(key);
		return values;
	}

	/** invoke only from getHTMLForHeader, needs specific context of date etc. */
	public static StringBuilder getHTMLForDate(String archiveID,Date date)
	{
		StringBuilder result = new StringBuilder();

		if (date != null && DateTimeComparator.getDateOnlyInstance().compare(date,EmailFetcherThread.INVALID_DATE)!=0) //display only if date is not INVALID (hacky)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(date);
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH) + 1; // we need 1-based, not 0-based.
			String yearOnclick = "\"javascript:window.location='browse?archiveID="+archiveID+"&year=" + year + "'\"";
			String monthOnclick = "\"javascript:window.location='browse?archiveID="+archiveID+"&year="+year+"&month=" + month + "'\"";
			String monthSpan = "<span class=\"facet\" onclick=" + monthOnclick + ">" + CalendarUtil.getDisplayMonth(c) + "</span> ";
			String yearSpan = "<span class=\"facet\" onclick=" + yearOnclick + ">" + c.get(Calendar.YEAR) + "</span> ";
			result.append("<tr><td width=\"10%\" align=\"right\" class=\"muted\">Date: </td><td>" + monthSpan + " " + c.get(Calendar.DATE) + ", " + yearSpan);
			int hh = c.get(Calendar.HOUR_OF_DAY);
			int mm = c.get(Calendar.MINUTE);
			boolean pm = false;
			if (hh == 0)
				hh = 12;
			else if (hh > 12)
			{
				hh -= 12;
				pm = true;
			}
			result.append(" " + String.format("%d:%02d", hh, mm) + (pm ? "pm" : "am"));
			result.append("\n</td></tr>\n");
		}else
			result.append("<tr><td width=\"10%\" align=\"right\" class=\"muted\">Date: </td><td>" + " " + "\n</td></tr>\n");

		return result;
	}

	public static JSONArray formatAddressesAsJSON(Address addrs[]) throws JSONException
	{
		JSONArray result = new JSONArray();
		if (addrs == null)
			return result;
		int index = 0;
		for (Address a : addrs) {
			if (a instanceof InternetAddress) {
				InternetAddress ia = (InternetAddress) a;
				JSONObject o = new JSONObject();
				o.put("email", Util.maskEmailDomain(ia.getAddress()));
				o.put("name", ia.getPersonal());
				result.put(index++, o);
			}
		}
		return result;
	}

	public static Pair<String, String> getNameAndURL(String archiveID,InternetAddress a, AddressBook addressBook)
	{
		String s = a.getAddress();
		if (s == null)
			s = a.getPersonal();
		if (s == null)
			return new Pair<>("", "");

		// TODO (maybe after archive data structures re-org): below should pass archive ID to browse page
		if (addressBook == null) {
			return new Pair<>(s, "browse?archiveID="+archiveID+"&person=" + s);
		} else {
			Contact contact = addressBook.lookupByEmail(a.getAddress());
			return new Pair<>(s, "browse?archiveID="+archiveID+"&contact=" + addressBook.getContactId(contact));
		}
	}

	/** serve up a file from the cache_dir */
	public static void serveBlob(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		HttpSession session = request.getSession();
		String filename = request.getParameter("file");
		filename = convertRequestParamToUTF8(filename);

		//get archiveID from the request parameter and then get the archive. It must be present
		//use its baseDir.
		Archive archive = JSPHelper.getArchive(request);
		assert archive!=null: new AssertionError("ArchiveID not passed to serveAttachment.jsp");
		String baseDir = archive.baseDir;

		if (filename.contains(".." + File.separator)) // avoid file injection!
		{
			log.warn("File traversal attack !? Disallowing serveFile for illegal filename: " + filename);
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// could check if user is authorized here... or get the userKey directly from session

		String filePath = baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.BLOBS_SUBDIR + File.separator + filename;
		writeFileToResponse(session, response, filePath, true /* asAttachment */);
	}

	/** serve up a file from the cache_dir */
	public static void serveImage(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		HttpSession session = request.getSession();
		String filename = request.getParameter("file");
		filename = convertRequestParamToUTF8(filename);
		String baseDir = (String) getSessionAttribute(session, "cacheDir");
		if (filename.contains(".." + File.separator)) // avoid file injection!
		{
			log.warn("File traversal attack !? Disallowing serveFile for illegal filename: " + filename);
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// could check if user is authorized here... or get the userKey directly from session

		String filePath = baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.IMAGES_SUBDIR + File.separator + filename;
		writeFileToResponse(session, response, filePath, true /* asAttachment */);
	}

	/** serve up a file from the temp dir, mainly used for serving exported mbox files*/
	public static void serveTemp(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		HttpSession session = request.getSession();
		String filename = request.getParameter("file");
		filename = convertRequestParamToUTF8(filename);

		//get archiveID from the request parameter and then get the archive. It must be present
		//use its baseDir.
		Archive archive = JSPHelper.getArchive(request);
		assert archive!=null: new AssertionError("ArchiveID not passed to serveTemp.jsp");
		String baseDir = archive.baseDir;

		if (filename.contains(".." + File.separator)) // avoid file injection!
		{
			log.warn("File traversal attack !? Disallowing serveFile for illegal filename: " + filename);
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// could check if user is authorized here... or get the userKey directly from session

		String filePath = Archive.TEMP_SUBDIR + File.separator + filename;
		writeFileToResponse(session, response, filePath, true /* asAttachment */, archive);
	}


	public static void writeFileToResponse(HttpSession session, HttpServletResponse response, String filePath, boolean asAttachment) throws IOException
	{
		writeFileToResponse(session, response, filePath, asAttachment, null);
	}

	public static void writeFileToResponse(HttpSession session, HttpServletResponse response, String filePath, boolean asAttachment, Archive archive) throws IOException
	{
		// Decode the file name (might contain spaces and on) and prepare file object.
		File file = new File(filePath);

		// Check if file actually exists in filesystem.
		if (!file.exists())
		{
			log.warn("Returing 404, serveFile can't find file: " + filePath);

			// Do your thing if the file appears to be non-existing.
			// Throw an exception, or send 404, or show default/warning page, or just ignore it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}

		// Get content type by filename.

		String contentType = session.getServletContext().getMimeType(file.getName());

		// If content type is unknown, then set the default value.
		// For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
		// To add new content types, add new mime-mapping entry in web.xml.
		if (contentType == null) {
			contentType = "application/octet-stream";
		}
		if(contentType.contains("rss")){
			contentType = "text/xml";
		}


		// Init servlet response.
		int DEFAULT_BUFFER_SIZE = 100000;
		response.reset();
		response.setBufferSize(DEFAULT_BUFFER_SIZE);
		response.setContentType(contentType);

		// not really sure why this is needed, but we have to ensure that these headers are not present when rendering e.g. xwordImage (directly rendered into web browser, instead of piclens)
		if (asAttachment)
		{
			response.setHeader("Content-Length", String.valueOf(file.length()));
			response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
		}
		// Prepare streams.
		BufferedInputStream input = null;
		BufferedOutputStream output = null;

		try {
			// Open streams.
			input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
			output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

			// Write file contents to response.
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
			//If exception is thrown before then this will not be reached
			if (archive != null)
			{
				File f = new File(filePath);
				String fileName = f.getName();
				String suffix = fileName.substring(fileName.length()-5);
				boolean isMboxFile = ".mbox".equals(suffix);
				boolean isMboxExportFromMainMenu = ("all-messages.mbox").equals(fileName) || ("non-restricted-messages.mbox").equals(fileName) || ("restricted-messages.mbox").equals(fileName);

				// Mbox export from main menue is dealt with in statusUpdate.js
				boolean mboxWeDealWithHere = isMboxFile && !isMboxExportFromMainMenu;
				if (mboxWeDealWithHere)
				{
					archive.getEpaddPremis().createEvent(EpaddEvent.EventType.MBOX_EXPORT, "Exported a subset of the collection", "success");
				}
			}
		}
		catch (Exception e)
		{
			if (archive != null)
			{
				File f = new File(filePath);
				String fileName = f.getName();
				String suffix = fileName.substring(fileName.length()-5);
				boolean isMboxFile = ".mbox".equals(suffix);
				boolean isEmlFile = ".zip".equals(suffix);

				boolean isExportFromMainMenu = fileName.startsWith("all-messages") || fileName.startsWith("non-restricted-messages") || fileName.startsWith("restricted-messages");

				// Mbox export from main menue is dealt with in statusUpdate.js
				boolean fileWeDealWithHere = (isMboxFile || isEmlFile) && !isExportFromMainMenu;
				if (fileWeDealWithHere)
				{
					archive.getEpaddPremis().createEvent(EpaddEvent.EventType.MBOX_EXPORT, "Exported a subset of the collection", "Error");
				}
			}
		} finally {
			// Gently close streams.
			Util.close(output);
			Util.close(input);
		}
	}

	/**
	 * makes the page un$able. warning: must be called before any part of the
	 * response body is committed
	 */
	public static void setPageUncacheable(HttpServletResponse response)
	{
		// see http://stackoverflow.com/questions/5139785/how-to-prevent-the-result-of-servlets-from-being-cached
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "-1");

		// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");

		// Set standard HTTP/1.0 no-cache header.
		response.setDateHeader("Expires", 0); //prevent caching at the proxy server
	}



	/** handles create/edit label request received from edit-label.jsp.
	    returns JSON with {status: (0 = passed, non-0 = failed), errorMessage: error message that can be shown to a user}
	 */
	public static String createEditLabels(HttpServletRequest request){
		JSONObject result = new JSONObject();

		try {
			Archive archive = JSPHelper.getArchive(request);
			LabelManager.LabType labelType;
			try {
				labelType = LabelManager.LabType.valueOf(request.getParameter("labelType"));
			} catch (Exception e) {
				result.put("status", 1);
				result.put("errorMessage", "Please specify a type of label.");
				return result.toString();
			}

			String labelName = request.getParameter("labelName");
			if (Util.nullOrEmpty(labelName)) {
				result.put("status", 8);
				result.put("errorMessage", "Label name cannot be empty.");
				return result.toString();
			}

			String description = request.getParameter("labelDescription");

			// check if it is label creation request or label updation one.
			String labelID = request.getParameter("labelID");
			LabelManager labelManager = archive.getLabelManager();
			Label label = labelManager.getLabel(labelID);

			// create the label if it doesn't exist, or update it if it does
			if (label != null) {
				label.update(labelName, description, labelType);
			} else {
				label = labelManager.createLabel(labelName, description, labelType);
			}

			// more processing for restriction labels
			if (labelType == LabelManager.LabType.RESTRICTION) {
				LabelManager.RestrictionType restrictionType = LabelManager.RestrictionType.valueOf(request.getParameter("restrictionType"));

				// read extra fields for restricton labels only
				long restrictedUntil = -1;
				int restrictedForYears = -1;

				// RESTRICTED_UNTIL processing
				if (restrictionType == LabelManager.RestrictionType.RESTRICTED_UNTIL) {
					String restrictedUntilStr = request.getParameter("restrictedUntil"); // this is in the form yyyy-mm-dd
					try {
						if (!Util.nullOrEmpty(restrictedUntilStr)) {
							List<String> startTokens = Util.tokenize(restrictedUntilStr, "-");
							int y = Integer.parseInt(startTokens.get(0));
							int m = Integer.parseInt(startTokens.get(1)) - 1; // -1 because Calendar.MONTH starts from 0, while user will input from 1
							int d = Integer.parseInt(startTokens.get(2));
							Calendar c = new GregorianCalendar();
							c.set(Calendar.YEAR, y);
							c.set(Calendar.MONTH, m);
							c.set(Calendar.DATE, d);
							restrictedUntil = c.getTime().getTime();
						} else {
							// empty string came in for restrictedUntil
							result.put("status", 2);
							result.put("errorMessage", "Please specify the date until which the restriction applies.");
							return result.toString();
						}
					} catch (Exception e) {
						result.put("status", 3);
						result.put("errorMessage", "Invalid date string: " + Util.escapeHTML(restrictedUntilStr) + ". Please use the format yyyy-mm-dd. ");
						return result.toString();
					}
				} else if (restrictionType == LabelManager.RestrictionType.RESTRICTED_FOR_YEARS) {
					// RESTRICTED_FOR_YEARS processing
					String restrictedForYearsStr = request.getParameter("restrictedForYears"); // this is in the form yyyy-mm-dd
					try {
						if (!Util.nullOrEmpty(restrictedForYearsStr)) {
							restrictedForYears = Integer.parseInt(restrictedForYearsStr);
							if (restrictedForYears <= 0) {
								result.put("status", 4);
								result.put("errorMessage", "Invalid value for #years: " + Util.escapeHTML(restrictedForYearsStr) + ". Number of years restricted should be positive.");
								return result.toString();
							}
						} else {
							// empty string came in for restrictedForYears
							result.put("status", 5);
							result.put("errorMessage", "Please specify the number of years for which the restriction applies.");
							return result.toString();
						}
					} catch (Exception e) {
						result.put("status", 6);
						result.put("errorMessage", "Invalid value for #years:  " + Util.escapeHTML(restrictedForYearsStr) + ". Please enter a number.");
						return result.toString();
					}
				}

				String labelAppliesToMessageText = request.getParameter("labelAppliesToMessageText");
				label.setRestrictionDetails (restrictionType, restrictedUntil, restrictedForYears, labelAppliesToMessageText);
			}

			// if no exception happened, status is 0
			result.put ("status", 0);
			result.put ("labelID", label.getLabelID());
			return result.toString();
		} catch (Exception e) {
			result.put ("status", 7);
			result.put ("errorMessage", "Exception while saving label: " + e.getMessage());
			return result.toString();
		}
	}

	public static void removeOperationInfo(HttpSession session, String opID) {
		Map<String,OperationInfo> operationInfoMap = (Map<String,OperationInfo>) session.getAttribute("operationInfoMap");
		if(operationInfoMap==null)
			return;
		operationInfoMap.remove(opID);
	}

	/*
	Helper methods for browse page. This page now loads browseMessage.jsp or browseAttachments.jsp depending upon the parameters passed to it.
	 */


	/*
	This method returns page title and facet title based on the parameters passed to this page.
	 */

	public static Pair<String,String> getTitles(HttpServletRequest request) throws UnsupportedEncodingException {
		//get searchTerm if present
		String searchTerm = JSPHelper.convertRequestParamToUTF8(request.getParameter("term"));

		// compute the title of the page
		String title = request.getParameter("title");
		//String sortBy = request.getParameter("sortBy");

		//<editor-fold desc="Derive title if the original title is not set" input="request" output="title"
		// name="search-title-derivation">
		{
			if (Util.nullOrEmpty(title)) {

				// good to give a meaningful title to the browser tab since a lot of them may be open
				// warning: remember to convert, otherwise will not work for i18n queries!
				String sentiments[] = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("lexiconCategory"));

				String[] persons = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("person"));
				String[] attachments = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("attachment"));

				int month = HTMLUtils.getIntParam(request, "month", -1);
				int year = HTMLUtils.getIntParam(request, "year", -1);
				int cluster = HTMLUtils.getIntParam(request, "timeCluster", -1);

				String sentimentSummary = "";
				if (sentiments != null && sentiments.length > 0)
					for (int i = 0; i < sentiments.length; i++) {
						sentimentSummary += sentiments[i];
						if (i < sentiments.length - 1)
							sentimentSummary += " & ";
					}

				if (searchTerm != null)
					title = "Search: " + searchTerm;
				else if (cluster != -1)
					title = "Cluster " + cluster;
				else if (!Util.nullOrEmpty(sentimentSummary))
					title = sentimentSummary;
				else if (attachments != null && attachments.length > 0)
					title = attachments[0];
				else if (month >= 0 && year >= 0)
					title = month + "/" + year;
				else if (year >= 0)
					title = Integer.toString(year);
				else if (persons != null && persons.length > 0) {
					title = persons[0];
					if (persons.length > 1)
						title += "+" + (persons.length - 1);
				} else
					title = "Browse";
				title = Util.escapeHTML(title);
			}
		}

		return new Pair<>(title,searchTerm);
	}


	public static Pair<Collection<Document>,SearchResult> getSearchResultDocs(Multimap<String,String> params, Archive archive) throws UnsupportedEncodingException {
		//<editor-fold desc="Search archive based on request parameters and return the result" input="archive;request"
		// output="collection:docs;collection:attachments(highlightAttachments) name="search-archive">

			SearchResult inputSet = new SearchResult(archive,params);
			Pair<Collection<Document>,SearchResult> search_result = SearchResult.selectDocsAndBlobs(inputSet);


		return search_result;
	}


	/*
	Return facets information obtained from the docs for message browsing screen.
	 */

	public static Map<String, Collection<DetailedFacetItem>> getFacetItemsForMessageBrowsing(HttpServletRequest request, Collection<Document> docs, Archive archive){
		Map<String, Collection<DetailedFacetItem>> facets;
		//<editor-fold desc="Create facets(categories) based on the search data" input="docs;archive"
		// output="facets" name="search-create-facets">
		{
			facets = IndexUtils.computeDetailedFacetsForMessageBrowsing(docs, archive);
		}
		//</editor-fold>

		return facets;
	}

	/*
	Return facets information obtained from the docs for attachment browsing screen.
	 */

	public static Map<String, Collection<DetailedFacetItem>> getFacetItemsForAttachmentBrowsing(Multimap<String, String> request, Collection<Document> docs, Archive archive){
		Map<String, Collection<DetailedFacetItem>> facets;
		//<editor-fold desc="Create facets(categories) based on the search data" input="docs;archive"
		// output="facets" name="search-create-facets">
		{
			facets = IndexUtils.computeDetailedFacetsForAttachmentBrowsing(request, docs, archive);
		}
		//</editor-fold>

		return facets;
	}

	/*
	Return original query string
	 */
	public static String getOrigQueryString(HttpServletRequest request){
		String origQueryString = request.getQueryString();
		//<editor-fold desc="Massaging the query string (containing all search parameter)" input="request"
		// output="string:origQueryString" name="search-query-string-massaging" >
		{
			if (origQueryString == null)
				origQueryString = "";

			// make sure adv-search=1 is present in the query string since we've now switched over to the new searcher
			if (!origQueryString.contains("adv-search=")) {
				if (origQueryString.length() > 0)
					origQueryString += "&";
				origQueryString += "adv-search=1";
			}

			// remove all the either's because they are not needed, and could mask a real facet selection coming in below
			origQueryString = Util.excludeUrlParam(origQueryString, "direction=either");
			origQueryString = Util.excludeUrlParam(origQueryString, "mailingListState=either");
			origQueryString = Util.excludeUrlParam(origQueryString, "attachmentExtension=");
			origQueryString = Util.excludeUrlParam(origQueryString, "entity=");
			origQueryString = Util.excludeUrlParam(origQueryString, "correspondent=");
			origQueryString = Util.excludeUrlParam(origQueryString, "attachmentFilename=");
			origQueryString = Util.excludeUrlParam(origQueryString, "attachmentExtension=");
			origQueryString = Util.excludeUrlParam(origQueryString, "annotation=");
			origQueryString = Util.excludeUrlParam(origQueryString, "folder=");
			// entity=&correspondent=&correspondentTo=on&correspondentFrom=on&correspondentCc=on&correspondentBcc=on&attachmentFilename=&attachmentExtension=&annotation=&startDate=&endDate=&folder=&lexiconName=general&lexiconCategory=Award&attachmentExtension=gif
		}
		//</editor-fold>
		return origQueryString;
	}
}
