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
package edu.stanford.muse.lens;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.util.Util;

/** class to keep track of browser preferences of highlighted terms. 
 * right now a singleton, but can consider having multiple instances in the future. #boost */

public class LensPrefs implements Serializable {
	private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(LensPrefs.class);
    
    // key structure: stores term -> page -> score map.
    // default score is 1. If a term is cancelled, it's score gets set to 0.
    // if a term is boosted, it's score gets incremented by 1.
    private Map<String, Map<String, Float>> prefs = new LinkedHashMap<>();
    
	private final static String PREFS_FILENAME = "lens.prefs"; // TODO: decide which directory this goes in
	private final String pathToPrefsFile;
		
    public LensPrefs(String cacheDir)
    {
    	pathToPrefsFile = cacheDir + File.separatorChar + PREFS_FILENAME;
    	String pathToPrefsFileTxt = pathToPrefsFile + ".txt"; 
		log.info("Trying to load lens prefs from: " + pathToPrefsFileTxt);
    	// read serialized state from file
		try {
			log.info("Trying to load lens prefs from: " + pathToPrefsFileTxt);
			loadPrefs(pathToPrefsFile + ".txt");
		} catch (IOException ioe) {
			log.info ("Unable to load text lens prefs from " + pathToPrefsFileTxt);
			try {
				log.info("Trying to load lens prefs from: " + pathToPrefsFileTxt);
				FileInputStream fis = new FileInputStream(pathToPrefsFile);
			    ObjectInputStream ois = new ObjectInputStream(fis);
			    prefs = (Map<String, Map<String, Float>>) ois.readObject();
			    ois.close();
			} catch (FileNotFoundException e) {
				// hilitePrefs = new ArrayList<BrowserPrefs>();
				log.info("No existing browser preferences file: " + pathToPrefsFile);
			} catch (Exception e) {
				log.warn ("exception reading " + pathToPrefsFile + ": " + Util.stackTrace(e));
			}
		}
		log.info (prefs.size() + " terms have user-specified weights");
		if (log.isDebugEnabled())
			log.debug (" User prefs are : " + this); 
    }

	public void cancelTerm(String pageURL, String term)
	{
        Map<String, Float> map = prefs.computeIfAbsent(term, k -> new LinkedHashMap<>());
        map.put(pageURL, 0.0f);
		savePrefs();
	}

	public void boostTerm(String pageURL, String term)
	{
        Map<String, Float> map = prefs.computeIfAbsent(term, k -> new LinkedHashMap<>());
        Float F = map.get(pageURL);
        if (F == null)
            map.put (pageURL, 2.0f); // default is 1, so boost to 2
        else
            map.put (pageURL, F + 1.0f); // boost prev. score by 1

		savePrefs();
	}
	
	private void savePrefs()
	{
		try { 
			FileOutputStream fos = new FileOutputStream(pathToPrefsFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(prefs);
			oos.flush();
			oos.close();
			
			PrintStream ps = new PrintStream(new FileOutputStream(pathToPrefsFile + ".txt"));
			for (String term: prefs.keySet())
			{
				Map<String, Float> map = prefs.get(term);
				for (String url: map.keySet())
					ps.println(term + "\t" + url + "\t" + map.get(url));
			}
			ps.close();
		}
		catch(Exception e) { e.printStackTrace(); }
	}	
	
	private void loadPrefs(String file) throws IOException 
	{
		List<String> lines = Util.getLinesFromFile(file, true /*ignore comment lines starting with # */);
		for (String line: lines) 
		{
			StringTokenizer st = new StringTokenizer(line, "\t");
			if (st.countTokens() != 3)
			{
				log.warn("Dropping bad line in lens prefs file: " + file + ": " + line);
				continue;
			}
			String term = st.nextToken(), url = st.nextToken(), weight = st.nextToken();
			Float W = Float.parseFloat(weight);
            Map<String, Float> map = prefs.computeIfAbsent(term, k -> new LinkedHashMap<>());
            map.put(url, W);
		}
	}
	
	public float getBoost(String pageURL, String term)
    {
		Map<String, Float> mapForTerm = prefs.get(term);
        if (mapForTerm == null)
            return 1.0f; // not seen term, return default boost

        Float F = mapForTerm.get(pageURL);
        if (F == null)
            return 1.0f; // default boost
        else
            return F;
    }
	
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		
		for (String term: prefs.keySet())
		{
			Map<String, Float> map = prefs.get(term);
			for (String url: map.keySet())
				sb.append(term + "\t" + url + "\t" + map.get(url) + "\n");
		}
		return sb.toString();
	}
}
