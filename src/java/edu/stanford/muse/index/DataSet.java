/*
 * Copyright (C) 2012 The Stanford MobiSocial Laboratory
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.stanford.muse.index;

import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.EmailRenderer;

import java.util.*;

/**
 * a collection of documents, typically the result of a search.
 * each doc has an html representation that is computed lazily and cached.
 * This object will be given an id like "docset-NNNNN" and stored in the session.
 * Need to release memory when the dataset is not being used, which is done by ajax/releaseDataset.jsp
 */
public class DataSet {
    private List<Document> docs = new ArrayList<>();
    private String datasetTitle;
    SearchResult searchResult;
    //String -> <dbId -> dbType>
    private Map<String, Map<String, Short>> authorisedEntities;

    public DataSet(Collection<Document> docs, SearchResult result, String datasetTitle) {
        if(docs!=null) {
            //calling assigning new ArrayList<>(docs) is calling sort on docs by default
            this.docs.addAll(docs);
        }
        this.searchResult = result;
        this.datasetTitle = datasetTitle;

    }

    public void clear() {
        searchResult.clear();
        docs.clear();
    }

    public int size() {
        return docs.size();
    }

    public List<Document> getDocs() {
        return docs;
    }

    public String toString() {
        return "Data set with " + size() + " documents";
    }

    /* returns html for doc i.
    Caches the html once computed (Removed during refactoring. It was done in variable called pages).
     In the front end jog plugin also does caching so removed server sided caching for simplicity*/
    public String getPage(int i, boolean IA_links, boolean inFull, boolean debug,String archiveID) {
//		if (authorisedEntities == null && !ModeConfig.isPublicMode()) {
//			String filename = archive.baseDir + java.io.File.separator + edu.stanford.muse.Config.AUTHORITIES_FILENAME;
//			try {
//				Map<String, Authority> tmpauth = (Map<String, Authority>) Util.readObjectFromFile(filename);
//				for (String str : tmpauth.keySet()) {
//					authorisedEntities.put(str, tmpauth.get(str).sources);
//				}
//			} catch (Exception e) {
//                JSPHelper.log.warn("Unable to find existing authorities file:" + filename + " :" + e.getMessage());
//			}
//		}
        try {
            String pageContent = "";
            //if (inFull ) // inFull==true now means it
            // previously was inFull==false
            // and needs refresh
            {
                // we are assuming one page per doc for now. (true for
                // emails)
                Pair<String, Boolean> htmlResut = EmailRenderer.htmlForDocument(docs.get(i), searchResult, datasetTitle,
                         authorisedEntities, IA_links, inFull, debug,archiveID);
                boolean overflow = htmlResut.second;
                Util.ASSERT(!(inFull && overflow));
                pageContent = htmlResut.first
                        +
                        (overflow ? "<br><span class='nojog' style='color:#500050;text-decoration:underline;font-size:12px' onclick=\"$('#more_spinner').show(); $.fn.jog_page_reload("
                                + i
                                + ", '&inFull=1');\">More<img id='more_spinner' src='/muse/images/spinner3-greenie.gif' style='width:16px;display:none;'/></span><br/>\n"
                                : "");
                //pages.set(i, pageContent);
            }
            //return pages.get(i);
            return pageContent;
        } catch (Exception e) {
            Util.print_exception(e);
            return "Page unavailable";
        }
    }
}
