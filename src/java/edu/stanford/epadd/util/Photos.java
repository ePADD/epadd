package edu.stanford.epadd.util;

import edu.stanford.muse.Config;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;

import edu.stanford.muse.webapp.ModeConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;

public class Photos
{
    private static final Logger log =  LogManager.getLogger(JSPHelper.class);

	/**
	 * serve up an image file.
	 * Be very careful about security. The file param should always be an offset from a known and controlled dir and should not allow ".." to prevent file traversal attacks.
	 * this method is a slightly different version of a similar function in Muse's JSPHelper.
	 * this method is required to support providing images without loading an archive.
	 * it loads file with relative path from current archive/images by default
	 * unless given type=discovery/delivery/processing in which case it looks for file path relative to ModeConfig.REPO_DIR_DISCOVERY/DELIVERY/PROCESSING.
	 * Note: file always has forward slashes for path separator, regardless of platform
	 */
	public static void serveImage(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String filename = request.getParameter("file");
		if (Util.nullOrEmpty(filename)) {
			log.warn("Empty filename sent to serveImage");
			return;
		}

		if (filename.contains(".." + File.separator)) // avoid file injection!
		{
			log.warn("File traversal attack !? Disallowing serveFile for illegal filename: " + filename);
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		filename = JSPHelper.convertRequestParamToUTF8(filename);

		// the request always has /. On Windows we may need to change that to \
		if (File.separator.equals("\\"))
			filename = filename.replaceAll("/", "\\\\");
		else
			filename = filename.replaceAll("/", File.separator);
		HttpSession session = request.getSession();
		String baseDir, filePath;

		Archive archive = JSPHelper.getArchive(request);
		if(archive!=null){
				baseDir = archive.baseDir;
				filePath = baseDir + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+ Archive.IMAGES_SUBDIR + File.separator + filename;

		}else if (ModeConfig.isProcessingMode())
		{
			baseDir = edu.stanford.muse.Config.REPO_DIR_PROCESSING;
			filePath = baseDir + File.separator + filename;
		}
		else if (ModeConfig.isDiscoveryMode())
		{
			baseDir = edu.stanford.muse.Config.REPO_DIR_DISCOVERY;
			filePath = baseDir + File.separator + filename;
		}
		else if (ModeConfig.isDeliveryMode())
		{
			baseDir = edu.stanford.muse.Config.REPO_DIR_DELIVERY;
			filePath = baseDir + File.separator + filename;
		}
		else {
			baseDir = Config.REPO_DIR_APPRAISAL;//filename is coming with 'user' in it. so no need to put it here.
			filePath = baseDir +  File.separator + filename;
		}

		// could check if user is authorized here... or get the userKey directly from session
		// log.info("Serving image from: " + filePath + " and filename is: " + filename);
		JSPHelper.writeFileToResponse(session, response, filePath, true /* asAttachment */);
	}

	public static void main(String[] args) {
		String str = "ePADD archive of Gov. Sarah Palin/images/landingPhoto.png";
		str = str.replaceAll("/", "\\\\");
		System.err.println(str);
	}
}
