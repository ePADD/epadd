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

import com.google.common.collect.Multimap;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.EmailRenderer;
import edu.stanford.muse.webapp.JSPHelper;

import java.util.*;

/**
 * a collection of documents, typically the result of a search.
 * each doc has an html representation that is computed lazily and cached.
 * This object will be given an id like "docset-NNNNN" and stored in the session.
 * Need to release memory when the dataset is not being used, which is done by ajax/releaseDataset.jsp
 */
public class DataSet {
    private final List<Document> docs = new ArrayList<>();
    private final String datasetTitle;
    private final SearchResult searchResult;
    private final Multimap<String,String> queryParams; //We will also store the query params obtained from the query that resulted in this dataset.
    //String -> <dbId -> dbType>
    private Map<String, Map<String, Short>> authorisedEntities;

    public DataSet(Collection<Document> docs, SearchResult result, String datasetTitle, Multimap<String, String> queryparams) {
        if(docs!=null) {
            //calling assigning new ArrayList<>(docs) is calling sort on docs by default
            this.docs.addAll(docs);
        }
        this.searchResult = result;
        this.datasetTitle = datasetTitle;
        this.queryParams=queryparams;

    }

    public void clear() {
        searchResult.clear();
        docs.clear();
    }

    public Multimap<String,String> getQueryParams(){
        return queryParams;
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

   /* returns message editing in textarea for doc i.
    Caches the message editing once computed (Removed during refactoring. It was done in variable called pages).
     In the front end jog plugin also does caching so removed server sided caching for simplicity*/

    public String getPageForEdit(int i) {

        try {
            String editContent = "";

            {
                // we are assuming one page per doc for now. (true for
                // emails)
                editContent = EmailRenderer.textForDocument(docs.get(i), searchResult);

            }

            return editContent;

        } catch (Exception e) {
            return "Message unavailable, please try to reload this page.<br/>" + e.toString(); // also reflect this back to the user
        }
    }

    /* returns message browsing html for doc i.
    Caches the html once computed (Removed during refactoring. It was done in variable called pages).
     In the front end jog plugin also does caching so removed server sided caching for simplicity*/
    public Pair <String, Boolean> getPageForMessages(int i, boolean IA_links, boolean inFull, boolean debug, String archiveID, boolean isPreserve) {
        Boolean redacted ;
//		if (authorisedEntities == null && !ModeConfig.isPublicMode()) {
//			String filename = archive.baseDir + java.io.File.separator + edu.stanford.muse.Config.AUTHORITIES_FILENAME;
//			try {
//				Map<String, Authority> tmpauth = (Map<String, Authority>) Util.readObjectFromFile(filename);
//				for (String str : tmpauth.keySet()) {
//					authorisedEntities.put(str, tmpauth.get(str).sources);
//				}
//			} catch (Exception e) {
//                JSPHelper.doConsoleLoggingWarnings("Unable to find existing authorities file:" + filename + " :" + e.getMessage());
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
                Pair<String, Pair<Boolean, Boolean>> htmlResult = EmailRenderer.htmlForDocument(docs.get(i), searchResult, datasetTitle,
                         authorisedEntities, IA_links, inFull, debug,archiveID, isPreserve);
                boolean overflow = htmlResult.second.first;
                redacted = htmlResult.second.second;
                Util.ASSERT(!(inFull && overflow));
                pageContent = htmlResult.first
                        +
                        (overflow ? "<br><span class='nojog' style='color:#500050;text-decoration:underline;font-size:12px' onclick=\"$('#more_spinner').show(); $.fn.jog_page_reload("
                                + i
                                + ", '&inFull=1');\">More<img id='more_spinner' src='/muse/images/spinner3-greenie.gif' style='width:16px;display:none;'/></span><br/>\n"
                                : "");
                //pages.set(i, pageContent);
            }
            //return pages.get(i);
            return new Pair <String, Boolean> (pageContent, redacted);
        } catch (Exception e) {
            StringBuilder debugString = new StringBuilder();
            debugString.append ("Problem information: archiveID " + archiveID);
            debugString.append (" dataset " + datasetTitle);
            debugString.append (" has " + ((docs == null) ? "(null)" : docs.size()) + " messages");
            debugString.append (" requested index " + i);
            Util.print_exception("Exception getting page in dataset " + debugString.toString(), e, JSPHelper.log);
            return new Pair<String, Boolean>("Message unavailable, please try to reload this page.<br/>" + debugString.toString(), false); // also reflect this back to the user
        }
    }


    /* returns attachment browsing html for year.
    Caches the html once computed (Removed during refactoring. It was done in variable called pages).
     In the front end jog plugin also does caching so removed server sided caching for simplicity*/
    public String getPageForAttachments(int year, boolean isHackyYearPresent, String archiveID, Multimap<String, String> queryparams) {
//		if (authorisedEntities == null && !ModeConfig.isPublicMode()) {
//			String filename = archive.baseDir + java.io.File.separator + edu.stanford.muse.Config.AUTHORITIES_FILENAME;
//			try {
//				Map<String, Authority> tmpauth = (Map<String, Authority>) Util.readObjectFromFile(filename);
//				for (String str : tmpauth.keySet()) {
//					authorisedEntities.put(str, tmpauth.get(str).sources);
//				}
//			} catch (Exception e) {
//                JSPHelper.doConsoleLoggingWarnings("Unable to find existing authorities file:" + filename + " :" + e.getMessage());
//			}
//		}
        try {
            {

                // htmlForAttachments returns us 2 components: the html and the js to be injected on the page
                Pair<String, String> htmlAndJs = EmailRenderer.htmlAndJSForAttachments(docs, year, isHackyYearPresent, searchResult,queryparams);
                String pageContent = htmlAndJs.first + "\n<script>" + htmlAndJs.second + "</script>";
                return pageContent;
            }
        } catch (Exception e) {
            StringBuilder debugString = new StringBuilder();
            debugString.append ("Problem information: archiveID " + archiveID);
            debugString.append (" dataset " + datasetTitle);
            debugString.append (" has " + ((docs == null) ? "(null)" : docs.size()) + " messages");
            debugString.append (" requested year " + year);
            Util.print_exception("Exception getting page/year in dataset " + debugString.toString(), e, JSPHelper.log);
            return "Message (attachments) unavailable, please try to reload this page.<br/>" + debugString.toString(); // also reflect this back to the user
        }
    }

}
