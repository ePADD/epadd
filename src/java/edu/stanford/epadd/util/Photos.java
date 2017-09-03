package edu.stanford.epadd.util;

import edu.stanford.muse.index.Archive;
import edu.stanford.muse.webapp.JSPHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;

/**
 * Created by hangal on 4/19/15.
 */
public class Photos
{
	public static Log	log	= LogFactory.getLog(JSPHelper.class);

	/**
	 * serve up an image file.
	 * Be very careful about security. The file param should always be an offset from a known and controlled dir and should not allow ".." to prevent file traversal attacks.
	 * this method is a slightly different version of a similar function in Muse's JSPHelper.
	 * this method is required to support providing images without loading an archive.
	 * it loads file with relative path from current archive/images by default
	 * unless given type=discovery/delivery/processing in which case it looks for file path relative to ModeConfig.REPO_DIR_DISCOVERY/DELIVERY/PROCESSING.
	 * Note: file always has forward slashes for path separator, regardless of platform
	 */
	public static void serveImage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String filename = request.getParameter("file");
		filename = JSPHelper.convertRequestParamToUTF8(filename);
		if (filename.indexOf(".." + File.separator) >= 0) // avoid file injection!
		{
			log.warn("File traversal attack !? Disallowing serveFile for illegal filename: " + filename);
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// the request always has /. On Windows we may need to change that to \
		if (File.separator.equals("\\"))
			filename = filename.replaceAll("/", "\\\\");
		else
			filename = filename.replaceAll("/", File.separator);
		HttpSession session = request.getSession();
		String mode = request.getParameter("mode");
		String baseDir, filePath;

		if ("processing".equalsIgnoreCase(mode))
		{
			baseDir = edu.stanford.muse.Config.REPO_DIR_PROCESSING;
			filePath = baseDir + File.separator + filename;
		}
		else if ("discovery".equalsIgnoreCase(mode))
		{
			baseDir = edu.stanford.muse.Config.REPO_DIR_DISCOVERY;
			filePath = baseDir + File.separator + filename;
		}
		else if ("delivery".equalsIgnoreCase(mode))
		{
			baseDir = edu.stanford.muse.Config.REPO_DIR_DELIVERY;
			filePath = baseDir + File.separator + filename;
		}
		else
		{ //get archiveID from the request parameter and then get the archive. It must be present
			//use its baseDir.
			Archive archive = JSPHelper.getArchive(request);
			assert archive!=null: new AssertionError("If no mode is set then the archiveID must be passed to serveImage.jsp");
			baseDir = archive.baseDir;
			filePath = baseDir + File.separator + Archive.IMAGES_SUBDIR + File.separator + filename;
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
