package edu.stanford.epadd;

import edu.stanford.muse.util.Util;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

	public static String	admin, holder, holderContact, holderReadingRoom;

	/* default location for dir under which archives are imported/stored. Should not end in File.separator */
	public final static String	REPO_DIR_APPRAISAL			= System.getProperty("user.home") + java.io.File.separator + "epadd-appraisal"; // this needs to be in sync with system property muse.dirname?
	public final static String	REPO_DIR_PROCESSING			= System.getProperty("user.home") + java.io.File.separator + "epadd-processing";
	public final static String	REPO_DIR_DISCOVERY			= System.getProperty("user.home") + java.io.File.separator + "epadd-discovery";
	public final static String	REPO_DIR_DELIVERY			= System.getProperty("user.home") + java.io.File.separator + "epadd-delivery";

	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");

		// Create path components to save the file
		final String path = "/tmp"; // request.getParameter("destination");
		request.getPart("file");

	}

	static {
		Properties props = new Properties();
		String propFilename = edu.stanford.muse.Config.SETTINGS_DIR + "config.properties";
		File f = new File(propFilename);
		if (f.exists() && f.canRead())
		{
			// not using logger as it may lead to circularity in initialization
			System.err.println ("Reading configuration from: " + propFilename);
			try
			{
				InputStream is = new FileInputStream(propFilename);
				props.load(is);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		admin = props.getProperty("admin", "Peter Chan, pchan3@stanford.edu");
		holder = props.getProperty("holder", "The Department of Special Collections and University Archives of Stanford University");
		holderContact = props.getProperty("holderContact", "speccollref@stanford.edu");
		holderReadingRoom = props.getProperty("holderReadingRoom", "Stanford University Special Collections Reading Room");
	}

	public static void main (String args[]) {
		System.out.println (admin);
		System.out.println(Util.filePathTail(Config.REPO_DIR_APPRAISAL));
	}
}