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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.File;

public class Log4JUtils {
    private static final Logger log = LogManager.getLogger(Log4JUtils.class);
    private static boolean initialized = false;
	private static final String BACKUP_FILE_SIZE = "300M";
	private static final int N_BACKUP_FILES = 30;

	public static String LOG_FILE, WARNINGS_LOG_FILE; // = System.getProperty("user.home") + File.separatorChar + ".muse" + File.separatorChar + "muse.log";

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
            WARNINGS_LOG_FILE = LOG_FILE + ".warnings"; // this includes error/fatal also
            addLogFileAppender(LOG_FILE, WARNINGS_LOG_FILE);

            // write a line so we can distinguish a new run in the log file
            String line = "__________________________________________________________________________________________________________________________________________________________________________ ";
            String message = line + "\n" + " Fresh deployment of ePADD started\n" + line;
            System.out.println(message);
            log.info (message);
            log.warn (message);

            message = "Log messages will be recorded in " + LOG_FILE;
            System.out.println(message);

            message = "Warning messages will also be recorded in " + WARNINGS_LOG_FILE;
            System.out.println(message);
            log.info (message);
            log.warn ("Warnings being logged to " + LOG_FILE);
        } catch (Exception e) { Util.print_exception(e);}

		initialized = true;
	}

	/** adds a new file appender to the root logger. expects root logger to have at least a console appender from which it borrows the layout.
	 * also puts warnings and errors into a separate warningsFilename which allows operator to look at warnings/errors/fatal separately */
	private static void addLogFileAppender(String filename, String warningsFilename)
	{

		try{
			ConfigurationBuilder<BuiltConfiguration> builder =
					ConfigurationBuilderFactory.newConfigurationBuilder();
			builder.setConfigurationName("RollingBuilder");
			LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
					.addAttribute("pattern", "%d{dd MMM HH:mm:ss} %c{1} %-5p - %m%n");


			//Console
			AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
					org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_OUT);

			appenderBuilder.add(layoutBuilder);
			builder.add(appenderBuilder);
			//file = all
			ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
					.addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
					.addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", BACKUP_FILE_SIZE));
			appenderBuilder = builder.newAppender("rollingAll", "RollingFile")
					.addAttribute("fileName", filename)
					.addAttribute("filePattern", filename+"-%d{MM-dd-yy}.log.gz")
					.add(layoutBuilder)
					.addComponent(triggeringPolicy);
			builder.add(appenderBuilder);
			//file=warning
			appenderBuilder = builder.newAppender("rollingWarning", "RollingFile")
					.addAttribute("fileName", warningsFilename)
					.addAttribute("filePattern", filename+"-%d{MM-dd-yy}.log.warnings.gz")
					.add(layoutBuilder)
					.addComponent(triggeringPolicy);
			builder.add(appenderBuilder);

			//file=entityRecognition
			appenderBuilder = builder.newAppender("entityRecognition", "RollingFile")
					.addAttribute("fileName", filename+"-EntityRecognition.log")
					.addAttribute("filePattern", filename+"-%d{MM-dd-yy}.log.ER.gz")
					.add(layoutBuilder)
					.addComponent(triggeringPolicy);
			builder.add(appenderBuilder);

			//All level messages from ePADD will be written to stdout and filename; third-party libraries (Tika, POI, ...) log at INFO and above
			builder.add(builder.newLogger("edu.stanford", org.apache.logging.log4j.Level.ALL));
			builder.add(builder.newRootLogger( org.apache.logging.log4j.Level.INFO).
					add(builder.newAppenderRef("Stdout")).
					add(builder.newAppenderRef("rollingAll").addAttribute("level", org.apache.logging.log4j.Level.ALL)).
					add(builder.newAppenderRef("rollingWarning").addAttribute("level", org.apache.logging.log4j.Level.WARN)));

			builder.add(builder.newLogger("edu.stanford.muse.ner.model.NBModel",org.apache.logging.log4j.Level.ALL).
					add(builder.newAppenderRef("entityRecognition").addAttribute("level",org.apache.logging.log4j.Level.ALL))
					);
			LoggerContext lc = (LoggerContext) LogManager.getContext(false);
			lc.setConfiguration(builder.build());
			lc.updateLoggers();

			} catch(Exception e) {
 	        log.error("Failed creating log appender in " + filename);
 	        System.err.println("Failed creating log appender in " + filename);
 	    }
	}
}
