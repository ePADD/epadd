package edu.stanford.muse.webapp;

import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.google.gson.Gson;
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
					page.append("<hr style=\"margin:10px\"/>\n<div class=\"attachments\">\n");
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
	 * This method (re)fills in three javascript variables present in browseAttachments.jspf
	 * These variables are then used to setup fancy box for attachment browsing process.
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
		StringBuilder tilediv = new StringBuilder();
		StringBuilder listdiv = new StringBuilder();


		int numAttachments=0;
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
					numAttachments+=ed.attachments.size();
				}
			}
		}
		page.append("<div class=\"muse-doc\">\n");

		//For list view and tile view buttons..
		page.append("<div style=\"display:flex\">\n");
		page.append("<div style=\"width:87%;margin-top:10px;font-family:open sans regular;color:#666;font-size:16px;\">"+numAttachments+" attachments for "+year +"</div>\n");
		page.append("<div class=\"gallery_viewchangebar\" style=\"justify-content:flex-end\">\n");
		page.append(  "  <div title=\"List view\" class=\"listView\" onclick=\"showListView()\">\n" +
				"    <img class=\"fbox_toolbarimg\" style=\"border-right:none;padding-left:10px;\" src=\"images/list_view.svg\"></img>\n" +
				"  </div>\n" +
				"  <div title=\"Tile view\"  class=\"tileView\" onclick=\"showTileView()\">\n" +
				"    <img class=\"fbox_toolbarimg\" style=\"height:28px;\" src=\"images/tile_view.svg\" ></img>\n" +
				"  </div>\n" );
		page.append("</div>");
		page.append("</div>");

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
		/*page.append("<div class=\"container\" style=\"width: inherit;\">");
		page.append("<div class=\"row\" >");
*/
		tilediv.append("<div id=\"attachment-tiles\" style=\"display:none\">");
		int attachmentIndex=0;
		//array to keep information about the attachments for display in UI.
		JSONArray attachmentDetails = new JSONArray();
		boolean isNormalized=false; //a flag to detect if any of the attachment has normalization or cleanup info present because if it is then the
		//information need to be presented to the user and hence the structure of the ui elements (like number of columns in the table) will change accordingly.

		for (Document d:docAttachmentMap.keySet()) {
			//don't forget to increase msgIndex at the end of the following inner loop.
			for (Blob attachment : docAttachmentMap.get(d)) {
				JSONObject attachmentInfo = getAttachmentDetails(archive,attachmentIndex,attachment, d);
				//Insert attachmentInfo in an array of JSONObject (attachmentDetails)
				attachmentDetails.put(attachmentIndex,attachmentInfo);
				String info = (String)attachmentInfo.opt("info");
				if(!Util.nullOrEmpty(info))
					isNormalized=true;//once set it can not be reset.

				String filename = (String)attachmentInfo.get("filename");
				tilediv.append("<div  class=\"square\" id=\"square\" onmouseenter=\"hoverin_squarebox(this)\" onmouseleave=\"hoverout_squarebox(this)\" data-index=\""+attachmentIndex+"\" data-filename=\""+filename+"\">");
				//don't forget to increase attachmentIndex
				attachmentIndex++;
				//String attachmentURL = (String)attachmentInfo.get("opts.url");
				String tileThumbnailURL = (String)attachmentInfo.get("tileThumbnailURL");
				boolean is_image = Util.is_image_filename(archive.getBlobStore().get_URL_Normalized(attachment));

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
				tilediv.append("<div class=\"insidesquare\" style=\"overflow:hidden\" >");

				/** Attachment preview related html**/
				String css_class = "attachmenttiles";// + (is_image ? " img" : "");
				String leader = "<img class=\"" + css_class + "\"  ";
				//page.append(leader + "href=\"" + attachmentURL + "\"  src=\"" + thumbnailURL + "\"></img>\n");
				tilediv.append(leader + "  src=\"" + tileThumbnailURL + "\"></img>\n");

				/*// punt on the thumbnail if the attachment tn or content
				// URL is not found
				if (thumbnailURL != null && attachmentURL != null) {
					// d.hashCode() is just something to identify this
					// page/message
					page.append(leader + "href=\"" + attachmentURL + "\"  src=\"" + thumbnailURL + "\"></img>\n");
					*//*page.append("<a rel=\"page" + d.hashCode() + "\" title=\"" + Util.escapeHTML(url) + "\" href=\"" + attachmentURL + "\">");
					page.append("<a>\n");*//*
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
*/

				tilediv.append("</div>");//closing of inside square box.

				tilediv.append("&nbsp;" + "<span id=\"display-name\" name=\"display-name\" class=\"displayname\" title=\"" + Util.escapeHTML(filename) + "\">" + Util.escapeHTML(filename) + "</span>&nbsp;");
				tilediv.append("<br/>");

				tilediv.append("</div>");
			}
		}
		tilediv.append("</div> <!-- closing of attachment-tile div-->\n");
		//add tilediv to page.
		page.append(tilediv);


		listdiv.append("<div id=\"attachment-list\">");
			listdiv.append("<table id=\"attachment-table\">\n");
			listdiv.append("<thead>\n");
			listdiv.append("<tr>\n");
			listdiv.append("<th>Subject</th>\n");
			listdiv.append("<th>Date</th>\n");
			listdiv.append("<th>Size</th>\n");
			listdiv.append("<th>Attachment name</th>\n");
			//add a field conditionally only if the information is also present for any attachment to be displayed to the user.
			if(isNormalized)
				listdiv.append("<th>More Information</th>\n");
			listdiv.append("</tr>\n");
			listdiv.append("</thead>\n");
			listdiv.append("<tbody>\n");
			listdiv.append("</tbody>\n");
			listdiv.append("</table>\n");
		listdiv.append("</div> <!-- closing of attachment--->\n");

		//add listdiv to page.
		page.append(listdiv);

		page.append("\n</div>  <!-- .muse-doc-attachments -->\n"); // muse-doc-attachments

		//close html tags.
		page.append("\n</div>  <!-- .muse-doc -->\n"); // .muse-doc

		//javascript code, convert arrays and maps to corresponding javascript variables.
		page.append("\n<script>\n");
		/*if(attachmentDetails.length()==0){
			page.append("$('#attachmentMsg').html(\"No attachments found in year "+year+"\");");
		}else{
			page.append("$('#attachmentMsg').html(\""+attachmentDetails.length()+" attachments found in year "+year+"\");");
		}*/
		if(isNormalized)
			page.append("isNormalized=true\n"); //to pass this information to the front end we assign it to a JS variable.
		page.append("attachmentDetails=JSON.parse('"+attachmentDetails.toString()+"');\n");
		page.append("loadAttachmentList();\n"); //invoke method to setup datatable with attachmentDetails data.
		page.append("if(isListView){ $('#attachment-tiles').hide(); $('#attachment-list').show();} else{$('#attachment-list').hide(); $('#attachment-tiles').show() }\n");//hide the list
		//page.append("$('#attachment-list').hide();\n");//hide the list
		page.append("\n</script>\n");


		html = page.toString();

		return new Pair<>(html, overflow);

	}

	/*
	Method to extract some key details from an attachment that needs to be displayed in a fancybox in the gallery feature.
	 */

	private static JSONObject getAttachmentDetails(Archive archive, int index, Blob attachment, Document doc) {
		//prepare json object of the information. The format is
		//{index:'',href:'', from:'', date:'', subject:'',filename:'',downloadURL:'',tileThumbnailURL:'',msgURL:'',info:''}
		//here info field is optional and present only for those attachments which were converted or normalized during the ingestion and therefore need
		//to be notified to the user.
		JSONObject result = new JSONObject();
		String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);

		//Extract mail information
		//Extract few details like sender, date, message body (ellipsized upto some length) and put them in result.
		EmailDocument ed = (EmailDocument)doc;
		String sender = Util.escapeHTML(((InternetAddress)ed.from[0]).getPersonal());//escaping because we might have the name like <jbush@..> in the sender.
		String date = Util.escapeHTML(ed.dateString());
		String subject= Util.escapeHTML(ed.description);//replace occurrence of ' because it was causing issue in JSON.parse.
		subject = Util.escapeJSON(subject);
		//then replace all occurrences of \t \r \n etc to
		String docId = ed.getUniqueId();
		String messageURL = "browse?archiveID="+archiveID+"&docId=" + docId;

		result.put("from",sender);
		result.put("date",date);
		result.put("subject",subject);
		result.put("msgURL",messageURL);



		//Extract few details like attachment src, thumnbail, search for message url etc and put them in result.
		//tilethumbnailURL is the url of the image displayed on small tile in the gallery landing page
		//thumbnailURL is the url of the image displayed in the gallery mode (inside fancybox). For now both are same but they can be made different
		//later therefore the distinction here.
		String thumbnailURL = null, downloadURL = null, tileThumbnailURL=null;
		BlobStore attachmentStore = archive.getBlobStore();
		if (attachmentStore != null) {
			String contentFileDataStoreURL = attachmentStore.get_URL_Normalized(attachment);
			downloadURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.escapeHTML(Util.URLtail(contentFileDataStoreURL));
			String tnFileDataStoreURL = attachmentStore.getViewURL(attachment, "tn");
			if (tnFileDataStoreURL != null) {
				thumbnailURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.escapeHTML(Util.URLtail(tnFileDataStoreURL));
				//set tile's thumbnail (on the landing page of gallery) also same.
				tileThumbnailURL = thumbnailURL;
			}
			else {
				if (archive.getBlobStore().is_image(attachment)) {
					thumbnailURL = downloadURL;
					tileThumbnailURL=thumbnailURL;//may be we need to reduce it's size.@TODO
				}
				else if(Util.is_pdf_filename(contentFileDataStoreURL)){  //because pdfs are treated as doc so better to keep it first.
					thumbnailURL="images/pdf_icon.svg";//thumbnailURL of a pdf can be a pdf image @TODO
					tileThumbnailURL="images/pdf_icon.svg";
				}else if(Util.is_ppt_filename(contentFileDataStoreURL)){  //same for ppt
					thumbnailURL="images/ppt_icon.svg";//thumbnailURL of a ppt can be a ppt image @TODO
					tileThumbnailURL="images/ppt_icon.svg";
				}else if(Util.is_doc_filename(contentFileDataStoreURL)){
					thumbnailURL="images/doc_icon.svg"; //thumbnailURL of a doc can be a doc image @TODO
					tileThumbnailURL="images/doc_icon.svg";
				}else if(Util.is_zip_filename(contentFileDataStoreURL)){
					thumbnailURL="images/zip_icon.svg";//thumbnailURL of a zip can be a zip image @TODO
					tileThumbnailURL="images/zip_icon.svg";
				}else{
					thumbnailURL="images/large_sorry_img.svg";
					tileThumbnailURL="images/large_sorry_img.svg";
				}
			}

		} else
			JSPHelper.log.warn("attachments store is null!");

		//for caption of the assignment
		String filename = archive.getBlobStore().full_filename_normalized(attachment, false);


		if(thumbnailURL==null)
			thumbnailURL = "images/large_sorry_img.svg";
		//downloadURL should never be null.
		boolean isNormalized = archive.getBlobStore().isNormalized(attachment);
		boolean isCleanedName = archive.getBlobStore().isCleaned(attachment);
		String cleanupurl = archive.getBlobStore().get_URL_Cleanedup(attachment);

		String info="";
		if(isNormalized || isCleanedName){
			String completeurl_cleanup ="serveAttachment.jsp?archiveID="+archiveID+"&file=" + Util.URLtail(cleanupurl);

			if(isNormalized){
				info="This file was converted during the preservation process. Its original name was "+attachmentStore.full_filename_original(attachment,false)+". Click <a href="+completeurl_cleanup+">here </a> to download the original file";
			}
			else if(isCleanedName){
				info="This file name was cleaned up during the preservation process. The original file name was "+attachmentStore.full_filename_original(attachment,false);
			}
		}
		//{index:'',href:'', from:'', date:'', subject:'',filename:'',downloadURL:'',tileThumbnailURL:'',info:'',size:''}

		result.put("size",attachment.size);
		result.put("href",thumbnailURL);
		result.put("downloadURL",downloadURL);
		result.put("tileThumbnailURL",tileThumbnailURL);
		result.put("filename",Util.escapeHTML(filename));
		if(!Util.nullOrEmpty(info)) //add this field only if this is non-empty. (That is the beauty of json, non-fixed structure for the data).
			result.put("info",info);



		result.put("index",index);



		return result;

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
