package edu.stanford.muse.webapp;

import edu.stanford.muse.index.*;
import edu.stanford.muse.util.Util;

import java.io.*;

public class SimpleSessions {

	public static String getVarOrDefault(String prop_name, String default_val)
	{
		String val = System.getProperty(prop_name);
		if (!Util.nullOrEmpty(val))
			return val;
		else
			return default_val;
	}
	public static String getDefaultCacheDir()
	{
		return ArchiveReaderWriter.CACHE_DIR;
	}

	public static String getDefaultRootDir()
	{
		return ArchiveReaderWriter.CACHE_BASE_DIR;
	}

	public static String getSessionSuffix(){ return ArchiveReaderWriter.SESSION_SUFFIX;}

	/** saves the archive in the current session to the cachedir *//*
	public static boolean saveArchive(String archiveID) throws IOException
	{
		Archive archive =  SimpleSessions.getArchiveForArchiveID(archiveID);
		assert archive!=null : new AssertionError("No archive for archiveID = " + archiveID + ". Is an archive loaded?");
		// String baseDir = (String) session.getAttribute("cacheDir");
		return saveArchive(archive.baseDir, "default", archive);
	}*/


	public static void main(String args[]) throws IOException
	{
		// just use as <basedir> <string to find>
		Archive a = ArchiveReaderWriter.readArchiveIfPresent(args[0]);
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
