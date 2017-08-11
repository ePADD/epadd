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
package edu.stanford.muse.datacache;

import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

/** a collection of Data's, for which a html view can be generated. */
public class BlobSet {

private static Log log = LogFactory.getLog(BlobSet.class);

private List<Blob> allBlobs; // all data's known, some of them may be duplicates (in terms of equals()), even though all the items are distinct object
private Map<String, List<Blob>> personToBlobMap = new LinkedHashMap<String, List<Blob>>();
private Map<Blob, List<Blob>> uniqueBlobMap = new LinkedHashMap<Blob, List<Blob>>();
private BlobStore blobStore;
private String rootDir;

/*
private String piclensFormat = "<object id=\"o\" classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" width=\"1200\" height=\"720\">\n" +
						"<param name=\"movie\"\n" +
						"value=\"//apps.cooliris.com/embed/cooliris.swf\" />\n" +
						"<param name=\"flashvars\"\n" +
						"value=\"feed=%s\" />\n" +
						"<param name=\"allowFullScreen\" value=\"true\" />\n" +
						"<param name=\"allowScriptAccess\" value=\"always\" />\n" +
						"<embed type=\"application/x-shockwave-flash\"\n" +
						"src=\"//apps.cooliris.com/embed/cooliris.swf\"\n" +
						"width=\"1200\" height=\"720\"\n" +
						"flashvars=\"feed=%s\"\n" +
						"allowFullScreen=\"true\" allowScriptAccess=\"always\"></embed>\n" +
						"</object>\n";
*/
private static final String photoRSSHeader = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
								"<rss version=\"2.0\" xmlns:media=\"http://search.yahoo.com/mrss/\"\n" +
								"xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
								"<channel>\n";

private static final String photoRSSFooter = "</channel>\n</rss>\n";

// compute the unique d1 -> d2, d1, d3... map.
private void compute_unique_data_map()
{
    for (Blob b: allBlobs)
    {
        List<Blob> blobs = uniqueBlobMap.get(b);
        if (blobs == null)
        {
            blobs = new ArrayList<Blob>();
            uniqueBlobMap.put (b, blobs);
        }
        blobs.add (b);
    }
}

// Instance variable for Blob stats
private Blob.BlobStats stats;

// Public Method to get the Blob stats of the current BlobSet
public Blob.BlobStats getStats(){
	return stats;
}

/** all_datas is a list of data objects, which may have duplicates. */
public BlobSet(String root_dir, List<Blob> allBlobs, BlobStore store) throws IOException
{
	
    this.rootDir = root_dir;
    this.allBlobs = allBlobs;
    this.blobStore = store;
    // be defensive, sometimes due to an error, all_datas gets passed in as null.
    // instead of crashing, better to treat it as an empty dataset.
    if (this.allBlobs == null)
    	this.allBlobs = new ArrayList<Blob>();

    compute_unique_data_map();
    stats = new Blob.BlobStats(0, 0, 0, 0);
    compute_stats(stats);
}

public List<Blob> getSameDatas(Blob b) { return uniqueBlobMap.get(b); }

private void print_map_entries(List<Map.Entry<String, List<Blob>>> l)
{
    long size = 0;
    for (Map.Entry<String, List<Blob>> me : l)
    {
        String id = me.getKey();
        int n_datas = me.getValue().size();
        if (n_datas == 0)
            continue;

        log.info(n_datas + " data" + ((n_datas > 1) ? "s":"") + " for " + id);

        for (Blob b : me.getValue())
        {
           // log.info (b);
            size += b.size;
        }

        log.info ("------------------------------------------");
    }
    log.info ("Total size = " + size);
    log.info ("------------------------------------------");
}

public String index_filename(Blob b)
{
    return blobStore.index(b) + "." + b.filename + ".index.html";
}

private List<Blob> sortBlobsByTime()
{
	// create a map of unique data -> earliest time it was seen (based on any of the others it maps to)
	final Map<Blob,Date> tmpMap = new LinkedHashMap<Blob,Date>();
	for (Map.Entry<Blob,List<Blob>> me : uniqueBlobMap.entrySet())
	{
		Blob unique_data = me.getKey();
		List<Blob> datas_for_this_unique_data = me.getValue();
		Date earliest = unique_data.getModifiedDate();
		for (Blob b: datas_for_this_unique_data)
		{
			Date c = b.getModifiedDate();
			if (c == null)
				b.modifiedDate = new Date(); // dummy date
			if (c != null && earliest != null)
				if (c.before(earliest))
					earliest = c;
		}
		tmpMap.put(unique_data, earliest);
	}

	// now sort the unique_data's by earliest time set in tmpMap
	List<Blob> result = new ArrayList<Blob>();
	log.info ("Checking blobs consistency for sorting...");
	result.addAll(tmpMap.keySet());
	for (Blob b: result)
	{
		if (tmpMap.get(b) == null)
			log.warn ("Blob " + b + " has null in tmpmap!");
		if (!b.equals(b))
			log.warn ("Blob " + b + " failed equals with itself!");
	}
	
	try {
		Collections.sort(result, new Comparator<Blob>() {
			public int compare(Blob b1, Blob b2)
			{
				Date c1 = tmpMap.get(b1);
				Date c2 = tmpMap.get(b2);
				if (c1 == null) {
					return -1;
				}
				if (c2 == null)
					return 1; // this will confuse the sorting, but what the heck...
				if (c1.before(c2))
					return 1;
				else
					return -1;
			}
		});
	} catch (Exception e) {
		log.warn ("Error sorting blobs by time! --");
		Util.print_exception(e);
	}
	return result;
}

/** currently 2 kinds of output. standalone page and piclens page. standalone page not tested.
 * if applicationURL is not null/empty, we'll use full paths to be safe (Cooliris has problems if we use relative paths in its media RSS file  */
private int emit_gallery_page(String prefix, String applicationURL, String extra_mesg)
{
    int nEntriesForPiclens;
    	
    try {
		// copy over sorry.png to the prefix dir so the photos.rss can directly reference it
    	String SORRY_FNAME = "sorry.png";
     	String SORRY_FNAME_URL = SORRY_FNAME;
    	InputStream is = this.getClass().getClassLoader().getResourceAsStream(SORRY_FNAME);
    	if (is == null)
    		log.info ("REAL WARNING: unable to find image conversion sorry file");
    	else
    	{
    		String sorry_fname = rootDir + File.separatorChar + SORRY_FNAME;
    		new File(rootDir).mkdirs(); // just to be safe
    		Util.copy_stream_to_file(is, sorry_fname);
    	}

    	// Make the dir with this dataset name, where we'll copy the data files
    	String dumpDataDir = rootDir + File.separatorChar + prefix + File.separatorChar;
    	new File(dumpDataDir).mkdirs();
    	String piclensRSSPath = rootDir + File.separatorChar + prefix + ".photos.rss";
    	PrintWriter piclensRSS = new PrintWriter (new FileWriter (piclensRSSPath));
    	piclensRSS.println (photoRSSHeader);

    	List<Blob> unique_datas = sortBlobsByTime();
    	List<Blob> noTN_datas = new ArrayList<Blob>();

    	nEntriesForPiclens = 0;
    	
    	for (Blob b: unique_datas)
    	{
    		String title = b.filename;
    		String description = b.filename;
    		String contentURL, thumbURL, linkURL;

    		// copy over the data content and its thumbnail from the data store to the dataset directory
    		if (!blobStore.contains(b))
    		{
    			
    			contentURL = SORRY_FNAME_URL;
    			thumbURL = SORRY_FNAME_URL;
    			linkURL = SORRY_FNAME_URL;
    			continue; // skip the sorry attachments
    		}
    		else
    		{
	        	String contentFileDataStoreURL = blobStore.get_URL(b);
	        	// could either serve up the attachment directly or break out to the message containing the attachment.
	        	// both are reasonable...
	        	contentURL = "serveAttachment.jsp?file=" + Util.URLtail(contentFileDataStoreURL);
	        	linkURL = "browse?attachment=" + Util.URLtail(contentFileDataStoreURL);
				if (!Util.nullOrEmpty(applicationURL)) {
					contentURL = applicationURL + "/" + contentURL;
					linkURL = applicationURL + "/" +  linkURL;
				}

	        	if (title.endsWith(".tif"))
	        		Util.breakpoint();

	    		String thumbFileDataStoreURL = blobStore.getViewURL(b, "tn");

	    		if (thumbFileDataStoreURL == null)
	    		{
	    			if (b.is_image())
	    			{
	    				// if image, we'll just assign the full attachment for the thumbnail and let piclens deal with it
	    				thumbURL = contentURL;
	    			}
	    			else
	    			{
	    				thumbURL = SORRY_FNAME_URL;
	        			noTN_datas.add(b);
	        			continue; // don't show this thumbnail
	    			}
	    		}
	    		else
	    		{
					 thumbURL = "serveAttachment.jsp?file=" + Util.URLtail(thumbFileDataStoreURL);
					// alternate: String thumbURL = "../serveAttachment.jsp?file=" + Util.URLtail(thumbFileDataStoreURL);
					// if given appl URL prepend it, because cooliris sometimes has trouble with just serveAttachment or even ../serveAttachment
					if (!Util.nullOrEmpty(applicationURL))
						thumbURL = applicationURL + "/" +  thumbURL;
	    		}
    		}

    		nEntriesForPiclens++;

    		// make an entry for this content on photo.rss stuff
    		piclensRSS.printf ("<item>%n" +
    				"<title>%s</title>%n" +
    				"<media:description>%s</media:description>%n" +
    				"<link>%s</link>%n" +
    				"<media:thumbnail url=\"%s\"/>%n" +
    				"<media:content url=\"%s\"/>%n" +
    				"</item>%n%n", Util.escapeXML(title), Util.escapeXML(description), Util.escapeXML(linkURL), Util.escapeXML(thumbURL), Util.escapeXML(thumbURL)); // used to be thumbFilename, imgFilename for the last 2 params. but if we do that, the link doesn't show up correctly.
    	}

    	piclensRSS.println (photoRSSFooter);
    	piclensRSS.close();
    } catch (IOException ioe) { Util.print_exception(ioe, log); return 0;}

    return nEntriesForPiclens;
}

private void compute_stats(Blob.BlobStats stats)
{
    stats.unique_data_size = 0;
    for (Map.Entry<Blob, List<Blob>> me : uniqueBlobMap.entrySet())
    {
        stats.n_unique_pics++;
        stats.unique_data_size += me.getKey().size;
        for (Blob b : me.getValue())
        {
            stats.total_data_size += b.size;
            stats.n_total_pics++;
        }
    }
}

public int generate_top_level_page(String prefix) throws IOException { return generate_top_level_page(prefix, "", null); }
public int generate_top_level_page(String prefix, String extra_mesg) throws IOException { return generate_top_level_page(prefix, "", extra_mesg); }

	/** application URL is something like http://localhost:8080/epadd */
public int generate_top_level_page(String prefix, String applicationURL, String extra_mesg) throws IOException
{
    List<Pair<String, List<Blob>>> tmp = new ArrayList<Pair<String, List<Blob>>>();
    for (Map.Entry<String, List<Blob>> entry: personToBlobMap.entrySet())
    		tmp.add (new Pair<String, List<Blob>>(entry.getKey(), entry.getValue()));
    
    Collections.sort (tmp, new Comparator<Pair<?,List<Blob>>>() {
                                public int compare(Pair<?,List<Blob>> p1, Pair<?,List<Blob>> p2) {
                                    return p2.getSecond().size() - p1.getSecond().size();
                                }
                             });	
   // print_map_entries (tmp);

    log.info (stats.n_total_pics + " total attachments, data size = " + stats.total_data_size + " bytes");
    log.info  (stats.n_unique_pics + " unique attachments, data size = " + stats.unique_data_size + " bytes");
    log.info  ((stats.n_total_pics - stats.n_unique_pics) + " redundant attachments = " + (stats.total_data_size - stats.unique_data_size) + " bytes");
 //   generate_sharer_xml();
 //   generate_sharer_json();
    return emit_gallery_page(prefix, applicationURL, extra_mesg); // rootDir has trailing /
}

public void verify()
{
    for (Blob b : uniqueBlobMap.keySet())
    {
        Util.ASSERT (uniqueBlobMap.get(b).get(0) == b);
    }
}

public String getURL(Blob b){
	return (blobStore.get_URL(b));
}

}
