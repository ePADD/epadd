package edu.stanford.muse.webapp;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import edu.stanford.muse.AnnotationManager.AnnotationManager;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.index.*;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

/** This class has util methods to display an email message in an html page */

public class EmailRenderer {

	private static final int TEXT_WRAP_WIDTH = 80;

	public static Pair<DataSet, JSONArray> pagesForDocuments(Collection<Document> docs, SearchResult result,
															 String datasetTitle) {
		return pagesForDocuments(docs, result, datasetTitle, MultiDoc.ClusteringType.MONTHLY);
	}

	/**
	 * format given addresses as comma separated html, linewrap after given
	 * number of chars
	 *
	 * @param addressBook
	 */
	private static String formatAddressesAsHTML(String archiveID, Address addrs[], AddressBook addressBook, int lineWrap, Set<String> highlightUnstemmed, Set<String> highlightNames, Set<String> highlightAddresses) {
		StringBuilder sb = new StringBuilder();
		int outputLineLength = 0;
		for (int i = 0; i < addrs.length; i++) {
			String thisAddrStr;

			Address a = addrs[i];
			if (a instanceof InternetAddress) {
				InternetAddress ia = (InternetAddress) a;
				Pair<String, String> p = JSPHelper.getNameAndURL(archiveID, (InternetAddress) a, addressBook);
				String url = p.getSecond();
				String str = ia.toUnicodeString(); //changed from toString to toUnicode string for correct display of special characters in the address.
				String addr = ia.getAddress();
				boolean match = false;
				if (str != null) {
					//The goal here is to explain why a doc is selected and hence we should replicate Lucene doc selection and Lucene is case insensitive most of the times
					String lc = str.toLowerCase();
					if (highlightUnstemmed != null)
						for (String hs : highlightUnstemmed) {
							String hlc = hs.toLowerCase().replaceAll("^\\W+|\\W+$", "");
							if (lc.contains(hlc)) {
								match = true;
								break;
							}
						}
					if (!match && highlightNames != null)
						for (String hn : highlightNames) {
							String hlc = hn.toLowerCase().replaceAll("^\\W+|\\W+$", "");
							if (lc.contains(hlc)) {
								match = true;
								break;
							}
						}
				}
				if (addr != null) {
					if (!match && highlightAddresses != null)
						for (String ha : highlightAddresses)
							if (addr.contains(ha)) {
								match = true;
								break;
							}
				}

				if (match)
					thisAddrStr = ("<a href=\"" + url + "\"><span class=\"hilitedTerm rounded\">" + Util.escapeHTML(str) + "</span></a>");
				else
					thisAddrStr = ("<a href=\"" + url + "\">" + Util.escapeHTML(str) + "</a>");

				if (str != null)
					outputLineLength += str.length();
			} else {
				String str = a.toString();
				thisAddrStr = str;
				outputLineLength += str.length();
				JSPHelper.log.warn("Address is not an instance of InternetAddress - is of instance: " + a.getClass().getName() + ", highlighting won't work.");
			}

			if (i + 1 < addrs.length)
				outputLineLength += 2; // +2 for the comma that will follow...

			if (outputLineLength + 2 > lineWrap) {
				sb.append("<br/>\n");
				outputLineLength = 0;
			}
			sb.append(thisAddrStr);
			if (i + 1 < addrs.length)
				sb.append(", ");
		}

		return sb.toString();
	}

	/**
	 * returns a string for documents - in message browsing screen.
	 *
	 * @param
	 * @throws Exception
	 */
	//TODO: inFull, debug params can be removed
	//TODO: Consider a HighlighterOptions class
	public static Pair<String, Boolean> htmlForDocument(Document d, SearchResult searchResult, String datasetTitle,
														Map<String, Map<String, Short>> authorisedEntities,
														boolean IA_links, boolean inFull, boolean debug, String archiveID) throws Exception {
		JSPHelper.log.debug("Generating HTML for document: " + d);
		EmailDocument ed = null;
		Archive archive = searchResult.getArchive();
		String html = null;
		boolean overflow = false;
		if (d instanceof EmailDocument) {
			// for email docs, 1 doc = 1 page
			ed = (EmailDocument) d;
			List<Blob> highlightAttachments = searchResult.getAttachmentHighlightInformation(d);
			StringBuilder page = new StringBuilder();
			page.append("<div class=\"muse-doc\">\n");

			page.append("<div class=\"muse-doc-header\">\n");
			page.append(EmailRenderer.getHTMLForHeader(ed, searchResult, IA_links, debug));
			page.append("</div>"); // muse-doc-header

			/*
			 * Map<String, List<String>> sentimentMap =
			 * indexer.getSentiments(ed); for (String emotion:
			 * sentimentMap.keySet()) { page.append ("<b>" + emotion +
			 * "</b>: "); for (String word: sentimentMap.get(emotion))
			 * page.append (word + " "); page.append ("<br/>\n");
			 * page.append("<br/>\n"); }
			 */
			//get highlight terms from searchResult object for this document.
			Set<String> highlightTerms = searchResult.getHLInfoTerms(ed);

			page.append("\n<div class=\"muse-doc-body\">\n");
			Pair<StringBuilder, Boolean> contentsHtml = searchResult.getArchive().getHTMLForContents(d, ((EmailDocument) d).getDate(),
					d.getUniqueId(), searchResult.getRegexToHighlight(), highlightTerms,
					authorisedEntities, IA_links, inFull, true);

			StringBuilder htmlMessageBody = contentsHtml.first;
			overflow = contentsHtml.second;
			// page.append(ed.getHTMLForContents(indexer, highlightTermsStemmed,
			// highlightTermsUnstemmed, IA_links));
			page.append(htmlMessageBody);
			page.append("\n</div> <!-- .muse-doc-body -->\n"); // muse-doc-body

			// page.append("\n<hr class=\"end-of-browse-contents-line\"/>\n");
			List<Blob> attachments = ed.attachments;
			if (attachments != null && attachments.size() > 0) {
				// show thumbnails of all the attachments

				if (ModeConfig.isPublicMode()) {
					page.append(attachments.size() + " attachment" + (attachments.size() == 1 ? "" : "s") + ".");
				} else {
					page.append("<hr/>\n<div class=\"attachments\">\n");
					int i = 0;
					for (; i < attachments.size(); i++) {
						Blob attachment = attachments.get(i);
						boolean highlight = highlightAttachments != null && highlightAttachments.contains(attachment);
						String css_class = "attachment" + (highlight ? " highlight" : "");
						page.append("<div class=\"" + css_class + "\">");

						String thumbnailURL = null, attachmentURL = null;
						boolean is_image = Util.is_image_filename(archive.getBlobStore().get_URL_Normalized(attachment));
						BlobStore attachmentStore = searchResult.getArchive().getBlobStore();
						if (attachmentStore != null) {
							String contentFileDataStoreURL = attachmentStore.get_URL_Normalized(attachment);
							attachmentURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(contentFileDataStoreURL);
							String tnFileDataStoreURL = attachmentStore.getViewURL(attachment, "tn");
							if (tnFileDataStoreURL != null)
								thumbnailURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(tnFileDataStoreURL);
							else {
								if (archive.getBlobStore().is_image(attachment))
									thumbnailURL = attachmentURL;
								else
									thumbnailURL = "images/sorry.png";
							}
						} else
							JSPHelper.log.warn("attachments store is null!");

						// toString the filename in any case,
						String url = archive.getBlobStore().full_filename_normalized(attachment, false);
						// cap to a length of 25, otherwise the attachment name
						// overflows the tn
						String display = Util.ellipsize(url, 25);
						page.append("&nbsp;" + "<span title=\"" + Util.escapeHTML(url) + "\">" + Util.escapeHTML(display) + "</span>&nbsp;");
						page.append("<br/>");

						css_class = "attachment-preview" + (is_image ? " img" : "");
						String leader = "<img class=\"" + css_class + "\" ";

						// punt on the thumbnail if the attachment tn or content
						// URL is not found
						if (thumbnailURL != null && attachmentURL != null) {
							// d.hashCode() is just something to identify this
							// page/message
							page.append("<a rel=\"page" + d.hashCode() + "\" title=\"" + Util.escapeHTML(url) + "\" href=\"" + attachmentURL + "\">");
							page.append(leader + "href=\"" + attachmentURL + "\" src=\"" + thumbnailURL + "\"></img>\n");
							page.append("<a>\n");
						} else {
							// page.append
							// ("&nbsp;<br/>&nbsp;<br/>Not fetched<br/>&nbsp;<br/>&nbsp;&nbsp;&nbsp;");
							// page.append("<a title=\"" + attachment.filename +
							// "\" href=\"" + attachmentURL + "\">");
							page.append(leader + "src=\"images/no-attachment.png\"></img>\n");
							// page.append ("<a>\n");

							if (thumbnailURL == null)
								JSPHelper.log.info("No thumbnail for " + attachment);
							if (attachmentURL == null)
								JSPHelper.log.info("No attachment URL for " + attachment);
						}
						BlobStore bstore = archive.getBlobStore();
						//if cleanedup.notequals(normalized) then normalization happened. Download original file (cleanedupfileURL)
						//origina.notequals(normalized) then only name cleanup happened.(originalfilename)
						//so the attributes are either only originalfilename or cleanedupfileURL or both.
						String cleanedupname = bstore.full_filename_cleanedup(attachment);
						String normalizedname = bstore.full_filename_normalized(attachment);
						String cleanupurl = bstore.get_URL_Cleanedup(attachment);
						boolean isNormalized = !cleanedupname.equals(normalizedname);
						boolean isCleanedName = !cleanedupname.equals(bstore.full_filename_original(attachment));
						if (isNormalized || isCleanedName) {
							String completeurl_cleanup = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(cleanupurl);

							page.append("<span class=\"glyphicon glyphicon-info-sign\" id=\"normalizationInfo\" ");
							if (isNormalized) {
								page.append("data-originalurl=" + "\"" + completeurl_cleanup + "\" ");
								page.append("data-originalname=" + "\"" + bstore.full_filename_original(attachment, false) + "\" ");
							}
							if (isCleanedName) {
								page.append("data-originalname=" + "\"" + bstore.full_filename_original(attachment, false) + "\"");
							}
							page.append("></span>");
						}

						page.append("</div>");
					}
					page.append("\n</div>  <!-- .muse-doc-attachments -->\n"); // muse-doc-attachments
				}

			}
			page.append("\n</div>  <!-- .muse-doc -->\n"); // .muse-doc
			html = page.toString();
		} else if (d instanceof DatedDocument) {
			/*
			 * DatedDocument dd = (DatedDocument) d; StringBuilder page = new
			 * StringBuilder();
			 *
			 * page.append (dd.getHTMLForHeader()); // directly jam in contents
			 * page.append ("<div class=\"muse-doc\">\n"); page.append
			 * (dd.getHTMLForContents(indexer)); // directly jam in contents
			 * page.append ("\n</div>"); // doc-contents return page.toString();
			 */
			html = "To be implemented";
		} else {
			JSPHelper.log.warn("Unsupported Document: " + d.getClass().getName());
			html = "";
		}

		return new Pair<>(html, overflow);
	}


	/**
	 * returns a string for documents - on attachment browsing screen.
	 *
	 * @param
	 * @throws Exception
	 */
	//TODO: inFull, debug params can be removed
	//TODO: Consider a HighlighterOptions class
	public static Pair<String, Boolean> htmlForAttachments(List<Document> docs,int year, SearchResult searchResult, String datasetTitle,
														Map<String, Map<String, Short>> authorisedEntities,
														boolean IA_links, boolean inFull, boolean debug, String archiveID) throws Exception {
		JSPHelper.log.debug("Generating HTML for attachments in year: " + year);
		EmailDocument ed = null;
		Archive archive = searchResult.getArchive();
		String html = null;
		boolean overflow = false;
		Map<Document,List<Blob> > docAttachmentMap = new LinkedHashMap<>();
		StringBuilder page = new StringBuilder();
		page.append("<div class=\"muse-doc\">\n");
		//Document d = docs.get(0);
		// for attachments 1 year = 1 page.
		//step 1: get the set of attachments for docs in year.
		for (Document doc : docs) {
			if (doc instanceof EmailDocument) {
				// for email docs, 1 doc = 1 page
				ed = (EmailDocument) doc;
				//collect attachments only if the year of this document is same as passed argument 'year'
				Calendar calendar = Calendar.getInstance();//should set timezone too.. @TODO
				calendar.setTime(ed.date);
				if (calendar.get(Calendar.YEAR) == year) {
					//prepare a list of attachments (not set) keeping possible repetition but also count unique attachments here.
					docAttachmentMap.put(ed,ed.attachments);
					//attachments.addAll(ed.attachments);
				}
			}
		}

		/***** @TODO
		//get HTML for attachment page header are
		page.append(EmailRenderer.getHTMLForHeaderAttachmentPage(ed, searchResult, IA_links, debug));

		//get HTML for attachment page content area
		Pair<StringBuilder, Boolean> contentsHtml = searchResult.getArchive().getHTMLForContentsAttachmentPage(d, ((EmailDocument) d).getDate(),
				d.getUniqueId(), searchResult.getRegexToHighlight(), highlightTerms,
				authorisedEntities, IA_links, inFull, true);

		StringBuilder htmlMessageBody = contentsHtml.first;
		overflow = contentsHtml.second;
		//setup event handlers

		 ****/
		page.append("<hr/>\n<div class=\"attachments\">\n");
		int i = 0;
		page.append("<div class=\"container\" style=\"width: inherit;\">");
		page.append("<div class=\"row\" >");

		for (Document d:docAttachmentMap.keySet()) {
			for (Blob attachment : docAttachmentMap.get(d)) {
				//boolean highlight = highlightAttachments != null && highlightAttachments.contains(attachment);
				String css_class = "attachment";// + (highlight ? " highlight" : "");
				//page.append("<div class=\"" + css_class + "\">");
				String url = archive.getBlobStore().full_filename_normalized(attachment, false);
			//Now put filename below this box.
				// toString the filename in any case,
				// cap to a length of 25, otherwise the attachment name
				// overflows the tn
				String display = Util.ellipsize(url, 15);
				page.append("<div  class=\"square col-sm-2\" onmouseenter=\"hoverin_squarebox(this)\" onmouseleave=\"hoverout_squarebox(this)\" data-filename=\""+url+"\"  data-displayname=\""+display+"\">");
//
				String thumbnailURL = null, attachmentURL = null;
				boolean is_image = Util.is_image_filename(archive.getBlobStore().get_URL_Normalized(attachment));
				BlobStore attachmentStore = searchResult.getArchive().getBlobStore();
				if (attachmentStore != null) {
					String contentFileDataStoreURL = attachmentStore.get_URL_Normalized(attachment);
					attachmentURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(contentFileDataStoreURL);
					String tnFileDataStoreURL = attachmentStore.getViewURL(attachment, "tn");
					if (tnFileDataStoreURL != null)
						thumbnailURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(tnFileDataStoreURL);
					else {
						if (archive.getBlobStore().is_image(attachment))
							thumbnailURL = attachmentURL;
						else if(Util.is_doc_filename(contentFileDataStoreURL)){
								thumbnailURL="images/doc_icon.svg";
						}else if(Util.is_pdf_filename(contentFileDataStoreURL)){
								thumbnailURL="images/pdf_icon.svg";
						}else if(Util.is_ppt_filename(contentFileDataStoreURL)){
								thumbnailURL="images/ppt_icon.svg";
						}else if(Util.is_zip_filename(contentFileDataStoreURL)){
								thumbnailURL="images/zip_icon.svg";
						}else{
								thumbnailURL="images/sorry.svg";
						}
					}

				} else
					JSPHelper.log.warn("attachments store is null!");


				/*BlobStore bstore = archive.getBlobStore();
				//if cleanedup.notequals(normalized) then normalization happened. Download original file (cleanedupfileURL)
				//origina.notequals(normalized) then only name cleanup happened.(originalfilename)
				//so the attributes are either only originalfilename or cleanedupfileURL or both.
				String cleanedupname = bstore.full_filename_cleanedup(attachment);
				String normalizedname = bstore.full_filename_normalized(attachment);
				String cleanupurl = bstore.get_URL_Cleanedup(attachment);
				boolean isNormalized = !cleanedupname.equals(normalizedname);
				boolean isCleanedName = !cleanedupname.equals(bstore.full_filename_original(attachment));
				if (isNormalized || isCleanedName) {
					String completeurl_cleanup = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(cleanupurl);

					page.append("<span class=\"glyphicon glyphicon-info-sign\" id=\"normalizationInfo\" ");
					if (isNormalized) {
						page.append("data-originalurl=" + "\"" + completeurl_cleanup + "\" ");
						page.append("data-originalname=" + "\"" + bstore.full_filename_original(attachment, false) + "\" ");
					}
					if (isCleanedName) {
						page.append("data-originalname=" + "\"" + bstore.full_filename_original(attachment, false) + "\"");
					}
					page.append("></span>");
				}
*/
				//attachment icon/preview goes here, inside another square box.
				page.append("<div class=\"insidesquare\" style=\"overflow:hidden\" data-fancybox=\"gallery\">");

				/** Attachment preview related html**/
				css_class = "attachment-preview" + (is_image ? " img" : "");
				String leader = "<img class=\"" + css_class + "\"  ";

				// punt on the thumbnail if the attachment tn or content
				// URL is not found
				if (thumbnailURL != null && attachmentURL != null) {
					// d.hashCode() is just something to identify this
					// page/message
					page.append(leader + "href=\"" + attachmentURL + "\"  src=\"" + thumbnailURL + "\"></img>\n");
					/*page.append("<a rel=\"page" + d.hashCode() + "\" title=\"" + Util.escapeHTML(url) + "\" href=\"" + attachmentURL + "\">");
					page.append("<a>\n");*/
				} else {
					page.append(leader + "src=\"images/no-attachment.png\"></img>\n");
					// page.append
					// ("&nbsp;<br/>&nbsp;<br/>Not fetched<br/>&nbsp;<br/>&nbsp;&nbsp;&nbsp;");
					// page.append("<a title=\"" + attachment.filename +
					// "\" href=\"" + attachmentURL + "\">");
//					page.append(leader + "src=\"images/no-attachment.png\"></img>\n");
					// page.append ("<a>\n");

					if (thumbnailURL == null)
						JSPHelper.log.info("No thumbnail for " + attachment);
					if (attachmentURL == null)
						JSPHelper.log.info("No attachment URL for " + attachment);
				}





				page.append("</div>");//closing of inside square box.

				page.append("&nbsp;" + "<span id=\"display-name\" name=\"display-name\" style=\"font-size:12px;\" title=\"" + Util.escapeHTML(url) + "\">" + Util.escapeHTML(display) + "</span>&nbsp;");
				page.append("<br/>");



				page.append("</div>");
			}
		}
		page.append("</div>");
		page.append("</div>");
		page.append("\n</div>  <!-- .muse-doc-attachments -->\n"); // muse-doc-attachments

		//close html tags.
		page.append("\n</div>  <!-- .muse-doc -->\n"); // .muse-doc
		html = page.toString();

		return new Pair<>(html, overflow);

	}



	/*
	 * returns pages and a json object for a collection of docs, which can be put into a
	 * jog frame.
	 *
	 * Changed the first arg type from: Collection<? extends EmailDocument> to Collection<Document>, as we get C
	 * ollection<Document> in browse page or from docsforquery, its a hassle to make them all return EmailDocument
	 * especially when no other document type is used anywhere.
	 * The second result is a json array of objects, one for each message. each message's object has metadata for it such as
	 * id, labels and annotations.
	 */
	public static Pair<DataSet, JSONArray> pagesForDocuments(Collection<Document> docs, SearchResult result,
															 String datasetTitle,
															 MultiDoc.ClusteringType coptions) {

		// need clusters which map to sections in the browsing interface
		List<MultiDoc> clusters;

		// indexer may or may not have indexed all the docs in ds
		// if it has, use its clustering (could be yearly or monthly or category
		// wise
		// if (indexer != null && indexer.clustersIncludeAllDocs(ds))
		// if (indexer != null)
		//IMP: instead of searchResult.getDocsasSet() use the docs that is already ordered by
		//the sortBy order (in SearchResult.selectDocsAndBlobs method.
		clusters = result.getArchive().clustersForDocs(docs, coptions);
		/*
		 * else { // categorize by month if the docs have dates if
		 * (EmailUtils.allDocsAreDatedDocs(ds)) clusters =
		 * IndexUtils.partitionDocsByInterval(new ArrayList<DatedDocument>((Set)
		 * ds), true); else // must be category docs clusters =
		 * CategoryDocument.clustersDocsByCategoryName((Collection) ds); }
		 */
		JSONArray resultObj = new JSONArray();
		int resultCount = 0;

		List<Document> datasetDocs = new ArrayList<>();
		AnnotationManager annotationManager = result.getArchive().getAnnotationManager();
		// we build up a hierarchy of <section, document, page>
		for (MultiDoc md : clusters) {
			if (md.docs.size() == 0)
				continue;

			List<List<String>> clusterResult = new ArrayList<>();

			for (Document d : md.docs) {
				String pdfAttrib = "";
				datasetDocs.add(d);
				clusterResult.add(null);
				// clusterResult.add(docPageList);
				// for (String s: docPageList)
				{
					JSONObject jsonObj = new JSONObject();

					String comment = Util.escapeHTML(annotationManager.getAnnotation(d.getUniqueId()));
					if (!Util.nullOrEmpty(comment))
						jsonObj.put("annotation", comment);
					Set<String> labels = result.getArchive().getLabelIDs((EmailDocument) d);
					if (!Util.nullOrEmpty(labels)) {
						JSONArray labs = new JSONArray();
						int i = 0;
						for (String l : labels) {
							labs.put(i++, l);
						}
						jsonObj.put("labels", labs);
					}

					if (d instanceof EmailDocument) {
					    EmailDocument ed = (EmailDocument) d;
						jsonObj.put("id", ed.getUniqueId());
                        jsonObj.put ("threadID", ed.threadID);
                        jsonObj.put("msgInThread",result.getArchive().docsWithThreadId(ed.threadID).size());//docsWithThreadID is not expensive method as it caches the result for future queries
						jsonObj.put ("nAttachments", ed.attachments != null ? ed.attachments.size(): 0);
					}
					resultObj.put(resultCount++, jsonObj);
				}
			}
		}

		DataSet dataset = new DataSet(datasetDocs, result, datasetTitle);
		return new Pair<>(dataset, resultObj);
	}

	/**
	 * returns a HTML table string for the doc header
	 * 
	 * @throws IOException
	 */
    private static StringBuilder getHTMLForHeader(EmailDocument ed, SearchResult searchResult,
                                                  boolean IA_links, boolean debug) throws IOException
	{
		AddressBook addressBook = searchResult.getArchive().addressBook;
        Set<String> contactNames = new LinkedHashSet<>();
        Set<String> contactAddresses = new LinkedHashSet<>();
        String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(searchResult.getArchive());
        //get contact ids from searchResult object.
        Set<Integer> highlightContactIds = searchResult.getHLInfoContactIDs().stream().map(Integer::parseInt).collect(Collectors.toSet());
        if(highlightContactIds!=null)
            for(Integer hci: highlightContactIds) {
                if(hci == null)
                    continue;
                Contact c = searchResult.getArchive().addressBook.getContact(hci);
                if(c==null)
                    continue;
                contactNames.addAll(c.getNames());
                contactAddresses.addAll(c.getEmails());
            }
        //get highlight terms from searchResult object for this document.
        Set<String> highlightTerms = searchResult.getHLInfoTerms(ed);

		StringBuilder result = new StringBuilder();
		// header table
		result.append("<table class=\"docheader rounded\">\n");
		// result.append
		// ("<tr><td width=\"100px\" align=\"right\" class=\"muted\">Folder:</td><td>"
		// + this.folderName + "</td></tr>\n");
		if(debug)
			result.append("<tr><td>docId: </td><td>"+ed.getUniqueId()+"</td></tr>\n");
		result.append(JSPHelper.getHTMLForDate(archiveID,ed.date));

		final String style = "<tr><td align=\"right\" class=\"muted\" valign=\"top\">";

		// email specific headers
		result.append(style + "From: </td><td align=\"left\">");
		Address[] addrs = ed.from;
		//get ArchiveID
		if (addrs != null)
		{
			result.append(formatAddressesAsHTML(archiveID,addrs, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses));
		}

		result.append(style + "To: </td><td align=\"left\">");
		addrs = ed.to;
		if (addrs != null)
			result.append(formatAddressesAsHTML(archiveID,addrs, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses) + "");

		result.append("\n</td></tr>\n");

		if (ed.cc != null && ed.cc.length > 0)
		{
			result.append(style + "Cc: </td><td align=\"left\">");
			result.append(formatAddressesAsHTML(archiveID,ed.cc, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses) + "");
			result.append("\n</td></tr>\n");
		}

		if (ed.bcc != null && ed.bcc.length > 0)
		{
			result.append(style + "Bcc: </td><td align=\"left\">");
			result.append(formatAddressesAsHTML(archiveID,ed.bcc, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses) + "");
			result.append("\n</td></tr>\n");
		}

		String x = ed.description;
		if (x == null)
			x = "<None>";

		result.append(style + "Subject: </td>");
		// <pre> to escape special chars if any in the subject. max 70 chars in
		// one line, otherwise spill to next line
		result.append("<td align=\"left\"><b>");
		x = DatedDocument.formatStringForMaxCharsPerLine(x, 70).toString();
		if (x.endsWith("\n"))
			x = x.substring(0, x.length() - 1);

        Span[] names = searchResult.getArchive().getAllNamesInDoc(ed, false);

        // Contains all entities and id if it is authorised else null
        Map<String, Entity> entitiesWithId = new HashMap<>();
        //we annotate three specially recognized types
        Map<Short,String> recMap = new HashMap<>();
        recMap.put(NEType.Type.PERSON.getCode(),"cp");
        recMap.put(NEType.Type.PLACE.getCode(),"cl");
        recMap.put(NEType.Type.ORGANISATION.getCode(),"co");
        Arrays.stream(names).filter(n -> recMap.keySet().contains(NEType.getCoarseType(n.type).getCode()))
                .forEach(n -> {
                    Set<String> types = new HashSet<>();
                    types.add(recMap.get(NEType.getCoarseType(n.type).getCode()));
                    entitiesWithId.put(n.text, new Entity(n.text, null, types));
                });

        x = searchResult.getArchive().annotate(x, ed.getDate(), ed.getUniqueId(), searchResult.getRegexToHighlight(), highlightTerms, entitiesWithId, IA_links, false);

		result.append(x);
		result.append("</b>\n");
		result.append("\n</td></tr>\n");
		// String messageId = Util.hash (ed.getSignature());
		// String messageLink = "(<a href=\"browse?archiveID="+archiveID+"&adv-search=1&uniqueId=" + messageId + "\">Link</a>)";
		// result.append ("\n" + style + "ID: " + "</td><td>" + messageId + " " + messageLink + "</td></tr>");
		result.append("</table>\n"); // end docheader table

		if (ModeConfig.isPublicMode())
			return new StringBuilder(Util.maskEmailDomain(result.toString()));

		return result;
	}

	/** I'm not sure what this is used for -- I think its used only for rendering HTML for the message. */
    public static class Entity {
        public final Map<String, Short> ids;
        //person,places,orgs, custom
        public final String name;
        public Set<String> types = new HashSet<>();

        public Entity(String name, Map<String, Short> ids, Set<String> types) {
            this.name = name;
            this.ids = ids;
            this.types = types;
        }

        @Override
        public String toString() {
            return types.toString();
        }
    }
}
