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
package edu.stanford.muse.index;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.muse.email.Filter;
import edu.stanford.muse.util.Util;

class IndexOptions implements Serializable {
	private final static long serialVersionUID = 1L;

	private List<String> inputPrefixes = new ArrayList<String>();
//	String outputPrefix;
	boolean monthsNotYears = true, noRecipients = false;
	private boolean do_NER = false;
	private boolean do_allText = true;
	private boolean locationsOnly = false;
	private boolean orgsOnly = false;
	Filter filter;
	boolean includeQuotedMessages = false;
	boolean ignoreDocumentBody = false;
	boolean incrementalTFIDF = false;
	boolean categoryBased = false;
	boolean indexAttachments = true;
	int subjectWeight = Indexer.DEFAULT_SUBJECT_WEIGHT;
	
//	int subjectWeight = Indexer.DEFAULT_SUBJECT_WEIGHT;

	/** 
		"Usage: [-i inputPrefix] [-o outputPrefix] [-d dict words file] [-s stop words file] [-i identity] [-t #threads] -a [alternate email addresses]");
	*/
	public void parseArgs(String args[])
	{
		if (args == null)
			return; // all defaults
        int optind;
        for (optind = 0; optind < args.length; optind++)
        {
        	if (args[optind].equals("-i")) {
        		inputPrefixes.add(args[++optind]);
        	} else if (args[optind].equals("-o")) {
        	//	outputPrefix = args[++optind];
         	} else if (args[optind].equals("-norecipients")) {
        		noRecipients = true;
         	} else if (args[optind].equals("-noattachments")) {
        		indexAttachments = false;
         	} else if (args[optind].equals("-ignoreDocumentBody")) {
        		ignoreDocumentBody = true;
        	} else if (args[optind].equals("-sentOnly")) {
//        		if (filter == null)
 //       			filter = new Filter(null, false, null, null);
  //      		filter.setSentMessagesOnly(true);
        	} else if (args[optind].equals("-yearly")) {
        		monthsNotYears = false;
        	} else if (args[optind].equals("-NER")) {
        		do_NER = true;
        	} else if (args[optind].equals("-subjectWeight")) {
        		String wStr = args[++optind];
        		subjectWeight = Integer.parseInt(wStr);
        	} else if (args[optind].equals("-noalltext")) {
        		do_allText = false;
        	} else if (args[optind].equals("-locationsOnly")) {
        		locationsOnly = do_NER = true; // force NER true
        	} else if (args[optind].equals("-orgsOnly")) {
        		orgsOnly = do_NER = true; // force NER true
        	} else if (args[optind].equals("-date")) {
        		if (filter == null)
        			filter = new Filter(null, false, null, null);
        		filter.setupDateRange(args[++optind]);
        	} else if (args[optind].equals("-incrementalTFIDF")) {
        		incrementalTFIDF = true;
        	} else if (args[optind].equals("-includeQuotedMessages")) {
        		includeQuotedMessages = true;
        	} else if (args[optind].equals("-categoryBased")) {
        		categoryBased = true;
        	} else if (args[optind].equals("-filter")) {
        		if (filter == null)
        			filter = new Filter(null, false, null, null);
        		filter.addNameOrEmail(args[++optind]);
        	} else if (args[optind].equals("-keywords")) {
        		if (filter == null)
        			filter = new Filter(null, false, null, null);
        		filter.setupKeywords(args[++optind]);
        	}
        	else
        		Indexer.log.warn("Invalid indexer option: " + args[optind]);
        }
	}

	public String toString()
	{
		return toString(true);
	}

	public String toString(boolean blur)
	{
		StringBuilder sb = new StringBuilder();
		sb.append ("\nInput prefixes: ");

		for (String s : inputPrefixes)
			sb.append ((blur ? Util.blurPath(s):s) + " ");
	//	sb.append ("\nOutput prefix: " + (blur ? Util.blurPath(outputPrefix) : outputPrefix));

		StringBuilder tsb = new StringBuilder();
		StringBuilder fsb = new StringBuilder();
		
		String s = "Months/!Years "; if (monthsNotYears) tsb.append(s); else fsb.append(s);
		s = "Include quoted messages "; if (includeQuotedMessages) tsb.append(s); else fsb.append(s);
		s = "Incremental TF-IDF "; if (incrementalTFIDF) tsb.append(s); else fsb.append(s);
		s = "Do-NER "; if (do_NER) tsb.append(s); else fsb.append(s);
		s = "Locations only "; if (locationsOnly) tsb.append(s); else fsb.append(s);
		s = "Orgs only "; if (orgsOnly) tsb.append(s); else fsb.append(s);
		s = "Category based "; if (categoryBased) tsb.append(s); else fsb.append(s);
		s = "Ignore document body "; if (ignoreDocumentBody) tsb.append(s); else fsb.append(s);
		s = "Index Attachments "; if (indexAttachments) tsb.append(s); else fsb.append(s);
		sb.append ("True: " + tsb + " False: " + fsb);
		
		sb.append ((filter == null) ? "No filter" : "Filter: " + filter);

		return sb.toString();
	}
}
