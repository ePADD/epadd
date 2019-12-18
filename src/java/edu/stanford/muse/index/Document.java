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


import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class Document implements Serializable, Comparable<Document> {
	private static final long serialVersionUID = 5713436068523824254L;
	private static final Logger log =  LogManager.getLogger(Document.class);
	String id; // just some handle - do not assume anything about values. not unique across folders.
	public String description;
	Document() { /* */ }
	public List<LinkInfo> links; // outgoing links from this doc
	public String comment;
	private boolean liked;
	public Set<String> languages; // languages are currently available after indexing
	
	Document(String id, String s)
	{
		this.id = id;
		this.description = s;
	}

	public String getUniqueId() { return id; }

	public void setComment(String s)
	{
		this.comment = s;
	}
	
	public void setLanguages(Set<String> langs)
	{
		this.languages = langs;
	}
	
	public void setLike() { this.liked = true; }
	public boolean isLiked() { return this.liked; }
	
	public int compareTo(Document other)
	{
		log.warn("The base Document.compareTo() should never be called"); // do we expect actual objects of Document rather than the derived EmailDocument?

		if (this == other) return 0;

		// so docId is not really helpful in comparing two messages.

		int result = Util.compareToNullSafe(description, other.description);
		if (result != 0) return result;

		return result;
	}

	@Override
	public boolean equals(Object other)
	{
		log.warn("The base Document.equals() should never be called"); // do we expect actual objects of Document rather than the derived EmailDocument?
		return this == other;
	}

	public String getSubjectWithoutTitle()
	{
        String sb = (description + "\n") +
                "\n";
        return sb;
	}

//	/** warning: this should be used only by the indexer to interface to the fetcher. */
//	public String getContents() throws ReadContentsException
//	{
//		try {
//			if (url == null)
//				return "";
//			byte[] b = Util.getBytesFromStream(new URL(url).openStream());
//			String contents = new String(b, "UTF-8");
//			return contents;
//		} catch (Exception e) {
//			throw new ReadContentsException(e);
//		}

		//		BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
//		String inputLine;
//		StringBuilder sb = new StringBuilder();
//		while ((inputLine = in.readLine()) != null)
//		    sb.append(inputLine + "\n");
//		in.close();
//
//		return sb.toString();
//	}

	/** gets HTML contents for this document.
	 */
	/*
	public Object getHTMLForContents(Indexer indexer) throws IOException
	{
		String contents = "\n\nContents not downloaded. Please go back to folders and check \"Fetch message text\" under advanced controls.\n\n";
		try { contents = getContents(); }
		catch (ReadContentsException e) { log.warn(e + Util.stackTrace(e)); }

//		final int MAX_CHARS_FOR_REFORMATTING = 30000; // 10k
//		if (contents.length() < MAX_CHARS_FOR_REFORMATTING)
//			contents = formatStringForMaxCharsPerLine(contents, 80).toString();

		if (indexer == null)
		{
			contents = Util.escapeHTML(contents);
			contents = contents.replace("\n", "<p>\n");
		}
		else
		{
			try {
			
			contents = indexer.getHTMLAnnotatedDocumentContents(contents, null, getUniqueId(), null, null);
			// fragile -- we know indexer replaces \n with br, so we replace br with p
			contents = contents.replace("<br/>", "<p>\n");
			} catch (Exception e) { log.warn ("indexer failed to annotate doc contents " + Util.stackTrace(e)); }
		}


		StringBuilder sb = new StringBuilder();
	//	sb.append("\n<pre>\n");
		sb.append (contents);
	//	sb.append("\n</pre>\n");

		return sb;
	}
*/
	public String toString() { return getUniqueId() + " (Subject:" + description + ")"; }

	public String getHeader()
	{
		return description;
	}

	/** rebases the content URL of this doc. 
	 * removes everything up to the last / in the document's content url and replaces it with the given newbase.
	 * useful when the cache files need to move from the position they originally were in when the doc was created.
	 */
//	public void rebaseContentURLDir(String newBase)
//	{
//		// url is something like: jar:file:////Users/hangal/.muse/user/__Users__hangal__Library__Thunderbird__Profiles__63kgspt8.default__Mail__Local__Folders__palin.contents!/15721.content
//		// we want to keep the filename the same, but change the dir part of the url
//		if (this.url == null)
//			return;
//		if (!this.url.startsWith("jar:file:///"))
//			return;
//		
//		int idx = this.url.lastIndexOf("!");
//		if (idx < 0)
//			return;
//
//		this.url.replaceAll("//", "/"); // canonicalize
//		String prefix = this.url.substring(0, idx);
//		String suffix = this.url.substring(idx); // suffix starts with the !
//		
//		int pathSepIdx = prefix.lastIndexOf("/");
//		String file = (pathSepIdx >= 0) ? prefix.substring(pathSepIdx) : prefix; // file includes the leading /
//			
//		this.url = "jar:file:///" + newBase + file + suffix;		
//	}
//	
//	/** rebases the content URL of this doc. 
//	 * removes everything AFTER the last / in the document's content url and replaces it with the given newFile.
//	 * useful when the cache file name changes from what it was when it was originally created.
//	 */
//	public void rebaseContentURLFile(String newFile)
//	{
//		// url is something like: jar:file:////Users/hangal/.muse/user/__Users__hangal__Library__Thunderbird__Profiles__63kgspt8.default__Mail__Local__Folders__palin.contents!/15721.content
//		// we want to change just the filename part of it
//		if (this.url == null)
//			return;
//		if (!this.url.startsWith("jar:file:///"))
//			return;
//		
//		int idx = this.url.lastIndexOf("!");
//		if (idx < 0)
//			return;
//
//		this.url.replaceAll("//", "/"); // canonicalize
//		String prefix = this.url.substring(0, idx);
//		String suffix = this.url.substring(idx); // suffix starts with the !
//		
//		int pathSepIdx = prefix.lastIndexOf("/");
//		String baseDir = (pathSepIdx >= 0) ? prefix.substring(0, pathSepIdx) : prefix; // file includes the leading /
//			
//		this.url = baseDir + "/" + newFile + suffix;		
//	}

	// select docs that contain tag (contain = true) or do not contain tag (contain = false) 
	public static Set<Document> selectDocByTag(Collection<Document> allDocs, String tag, boolean contain)
	{
		Set<Document> result = new LinkedHashSet<>();
		if (tag != null) { // note: empty tag is allowed
			tag = tag.toLowerCase();
			for (Document d : allDocs) {
				boolean found = false;
				if (Util.nullOrEmpty(tag) && Util.nullOrEmpty(d.comment))
					found = true;
				if (!Util.nullOrEmpty(d.comment) && !Util.nullOrEmpty(tag) && d.comment.toLowerCase().contains(tag))
					found = true;
				if (found == contain)
					result.add(d);
			}
		}
		return result;
	}
}