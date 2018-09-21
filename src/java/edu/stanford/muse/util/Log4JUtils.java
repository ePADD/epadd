/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

public class Log4JUtils {
    private static Log log = LogFactory.getLog(Log4JUtils.class);
    private static boolean initialized = false;

	public static String LOG_FILE; // = System.getProperty("user.home") + File.separatorChar + ".muse" + File.separatorChar + "muse.log";

	// NOTE: EpaddInitializer should already have run before this method is called
	public static synchronized void initialize()
	{
		// NOTE: do not use logger calls inside this method, as logging is still being set up
		if (initialized)
			return;

		// LOG FILE will be set only once, either to <home>/.muse/muse.log (default) or overwritten with the help of muse.dirname and muse.log system props, typically to <home>/ePADD/epadd.log
		LOG_FILE = System.getProperty("user.home") + File.separatorChar + ".muse" + File.separatorChar + "muse.log";
		String newLogFile = System.getProperty("muse.log"); // for epadd this will be epadd.log, set in EpaddInitializer
		if (!Util.nullOrEmpty(newLogFile))
			LOG_FILE = newLogFile;

		File parent = new File(LOG_FILE).getParentFile();
		
	   	// check the parent directory of the log file first...
		// if the directory does not exist, create it
    	if (!parent.exists()) {
    		System.out.println("Creating " + parent);
    		boolean result = parent.mkdirs();
    		if (!result)
    		{
    			System.out.println ("Sorry, unable to create: " + parent.getAbsolutePath());
    			return;
    		}
    	}
    	else if (!parent.isDirectory())
    	{
    		System.out.println ("Sorry, this needs to be a folder, not a file: " + parent.getAbsolutePath());
    		return;
    	}
    	else if (!parent.canWrite())
    	{
    		System.out.println ("Sorry, this folder is not writable: " + parent.getAbsolutePath());
    		return;
    	}

    	// now rename, truncate or create the actual log file
		try {
			/*
			try {
				File f = new File(LOG_FILE);
				if (f.exists())
				{
					// save the previous log file if it exists (shouldn't the rolling file appender take care of this??)
					RandomAccessFile raf = new RandomAccessFile(f, "rwd");
					raf.setLength(0);
					raf.close();
				}
			} catch (Exception e) { Util.print_exception(e);}
			*/
			addLogFileAppender(LOG_FILE);
			addLogFileAppender("edu.stanford.muse.ner.EntityExtractionManager",	System.getProperty("user.home") + File.separatorChar + ".muse" + File.separatorChar + "entityExtractor.log");

			// write a line so we can distinguish a new run in the log file
			String message = "________________________________________________________________________________________________ ";
			System.out.println(message);
			log.info (message);
            
			message = "Log messages will be recorded in " + LOG_FILE;
			System.out.println(message);
			log.info (message);
		} catch (Exception e) { Util.print_exception(e);}
		initialized = true;
	}

	/** adds a new file appender to the root logger. expects root logger to have at least a console appender from which it borrows the layout */
 	private static void addLogFileAppender(String filename)
 	{
 	    try
 	    {
            Logger rootLogger = LogManager.getLoggerRepository().getRootLogger();
            Enumeration allAppenders = rootLogger.getAllAppenders();
            while(allAppenders.hasMoreElements())
            {
                Object next = allAppenders.nextElement();
                if (next instanceof ConsoleAppender)
                {
                	Layout layout = ((ConsoleAppender) next).getLayout();
                	RollingFileAppender rfa = new RollingFileAppender(layout, filename);
                	rfa.setMaxFileSize("10MB");
                	rfa.setMaxBackupIndex(10); // do we
                	rfa.setEncoding("UTF-8");
                	rootLogger.addAppender (rfa);
                }
            }
 	    }
 	    catch(Exception e)
 	    {
 	        log.error("Failed creating log appender in " + filename);
 	        System.err.println("Failed creating log appender in " + filename);
 	    }
 	}

	/** adds a new file appender to the root logger. expects root logger to have at least a console appender from which it borrows the layout */
	public static void addLogFileAppender(String packagename, String filename)
	{
		try
		{
			Logger packageLogger = LogManager.getLoggerRepository().getLogger(packagename);
			Logger rootLogger = LogManager.getRootLogger();
			Enumeration allAppenders = rootLogger.getAllAppenders();
			while(allAppenders.hasMoreElements())
			{
				Object next = allAppenders.nextElement();
				if (next instanceof ConsoleAppender)
				{
					Layout layout = ((ConsoleAppender) next).getLayout();
					RollingFileAppender rfa = new RollingFileAppender(layout, filename);
					rfa.setMaxFileSize("10MB");
					rfa.setMaxBackupIndex(10); // do we
					rfa.setEncoding("UTF-8");
					packageLogger.addAppender (rfa);
				}
			}
		}
		catch(Exception e)
		{
			log.error("Failed creating log appender in " + filename);
			System.err.println("Failed creating log appender in " + filename);
		}
	}
 	public static void setLoggingLevel (Logger logger, String level)
 	{
 		if ("debug".equalsIgnoreCase(level))
	    	logger.setLevel(Level.DEBUG);
	    else if ("info".equalsIgnoreCase(level))
	    	logger.setLevel(Level.INFO);
	    else if ("warn".equalsIgnoreCase(level))
	    	logger.setLevel(Level.WARN);
	    else if ("error".equalsIgnoreCase(level))
	    	logger.setLevel(Level.ERROR);
	    else if ("trace".equalsIgnoreCase(level))
	    	logger.setLevel(Level.TRACE);
	    else
	    	log.warn ("Unknown logging level: for " + logger + " to " + level);
 		
 		log.info ("Effective logging level for " + logger.getName() + " is " + logger.getEffectiveLevel());
	}

 	/** taken from: http://stackoverflow.com/questions/3060240/how-do-you-flush-a-buffered-log4j-fileappender */
 	public static void flushAllLogs()
 	{
 	    try
 	    {
 	        Set<FileAppender> flushedFileAppenders = new LinkedHashSet<>();
 	        Enumeration currentLoggers = LogManager.getLoggerRepository().getCurrentLoggers();
 	        while (currentLoggers.hasMoreElements())
 	        {
 	            Object nextLogger = currentLoggers.nextElement();
 	            if (nextLogger instanceof Logger)
 	            {
 	                Logger currentLogger = (Logger) nextLogger;
 	                Enumeration allAppenders = currentLogger.getAllAppenders();
 	                while(allAppenders.hasMoreElements())
 	                {
 	                    Object nextElement = allAppenders.nextElement();
 	                    if(nextElement instanceof FileAppender)
 	                    {
 	                        FileAppender fileAppender = (FileAppender) nextElement;
 	                        if(!flushedFileAppenders.contains(fileAppender) && !fileAppender.getImmediateFlush())
 	                        {
 	                            flushedFileAppenders.add(fileAppender);
 	                            //log.info("Appender "+fileAppender.getName()+" is not doing immediateFlush ");
 	                            fileAppender.setImmediateFlush(true);
 	                            currentLogger.info("Flushing appender: " + fileAppender.getName() + "\n" + fileAppender);
 	                            fileAppender.setImmediateFlush(false);
 	                        }
 	                        else
 	                        {
 	                            //log.info("fileAppender"+fileAppender.getName()+" is doing immediateFlush");
 	                        }
 	                    }
 	                }
 	            }
 	        }
 	    }
 	    catch(RuntimeException e)
 	    {
 	        log.error("Failed flushing logs", e);
 	    }
 	}
}
