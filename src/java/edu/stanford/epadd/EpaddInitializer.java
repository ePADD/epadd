package edu.stanford.epadd;

import edu.stanford.muse.index.Archive;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

public class EpaddInitializer implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		try {
			// default folder is ~/ePADD
			// Though on MacOSX it should be ~/Library/Application Support/ePADD
			// https://developer.apple.com/library/mac/technotes/tn2002/tn2110.html			
			// but it is not visible in the finder, so we stay with ~/epadd.
			System.setProperty("muse.defaultArchivesDir", edu.stanford.muse.Config.REPO_DIR_APPRAISAL); // this will get picked up by Muse's Sessions.java
			//The muse.log system variable is already being set in TomcatMain.java, dont touch it here
			//System.setProperty("muse.log", edu.stanford.muse.Config.SETTINGS_DIR + File.separator + "epadd.log");

			// ensure existence of the directory
			File f = new File(edu.stanford.muse.Config.REPO_DIR_APPRAISAL);
			if (f.exists() && !f.isDirectory())
			{
				System.err.println ("Sorry, file " + f.getPath() + " is a file, it needs to be a directory!");
				throw new RuntimeException();
			}
			else if (!f.exists())
				f.mkdirs();
			
			// check if it exists in case mkdirs failed...
			if (f.exists()) 
				System.out.println ("ePADD web application context initialized. Setting up default base folder to " + edu.stanford.muse.Config.REPO_DIR_APPRAISAL);
			else
			{
				System.err.println ("Sorry, unable to create ePADD base folder: " + f.getPath());
				throw new RuntimeException();
			}
			edu.stanford.muse.util.Version.appName = "epadd";
			Archive.LEXICONS = new String[]{"general.english.lex.txt", "sensitive.english.lex.txt", "sentiments.english.lex.txt",
					"Persona.academic.administrator.sensitive.duke.english.lex.txt",
					"Persona.author.princeton.english.lex.txt",
					"Persona.composer.NYPL.english.lex.txt",
					"Persona.environmental.artist.projects.stanford.english.lex.txt",
					"Persona.faculty.UCI.english.lex.txt",
					"Persona.journalist.activist.politics.and.travel.UCB.english.lex.txt",
					"Persona.microbiologist.UIUC.english.lex.txt",
					"Persona.writer.theater.NYPL.english.lex.txt"
			};

			System.err.println("muse.log system variable is: "+System.getProperty("muse.log"));
		} catch (Exception e) {
			System.err.println ("ERROR initializing ePADD: " + e);
			e.printStackTrace(System.err);
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		System.out.println ("ePADD web application context destroyed.");	    	
	}
}
