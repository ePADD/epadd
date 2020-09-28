package edu.stanford.muse.webapp;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.AnnotationManager.AnnotationManager;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.index.*;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** This class has util methods to display an email message in an html page */

public class EmailRenderer {

	private static final int TEXT_WRAP_WIDTH = 80;
	public static String EXCLUDED_EXT = "[0-9]+";

	public static Pair<DataSet, JSONArray> pagesForDocuments(Collection<Document> docs, SearchResult result,
															 String datasetTitle, Multimap<String,String> queryparams) {
		return pagesForDocuments(docs, result, datasetTitle, MultiDoc.ClusteringType.MONTHLY,queryparams);
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
			Pair<StringBuilder, Boolean> contentsHtml = archive.getHTMLForContents(d, ((EmailDocument) d).getDate(),
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
						BlobStore attachmentStore = archive.getBlobStore();

						boolean is_image = false;
						if (attachmentStore != null) {
							is_image = Util.is_image_filename(attachmentStore.get_URL_Normalized(attachment));
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
						} else {
							JSPHelper.log.warn("attachments store is null!");
							// no return, soldier on even if attachments unavailable for some reason
						}

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

						//if cleanedup.notequals(normalized) then normalization happened. Download original file (cleanedupfileURL)
						//origina.notequals(normalized) then only name cleanup happened.(originalfilename)
						//so the attributes are either only originalfilename or cleanedupfileURL or both.
						String cleanedupname = attachmentStore.full_filename_cleanedup(attachment);
						String normalizedname = attachmentStore.full_filename_normalized(attachment);
						String cleanupurl = attachmentStore.get_URL_Cleanedup(attachment);
						boolean isNormalized = !cleanedupname.equals(normalizedname);
						boolean isCleanedName = !cleanedupname.equals(attachmentStore.full_filename_original(attachment));
						if (isNormalized || isCleanedName) {
							String completeurl_cleanup = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLtail(cleanupurl);

							page.append("<span class=\"glyphicon glyphicon-info-sign\" id=\"normalizationInfo\" ");
							if (isNormalized) {
								page.append("data-originalurl=" + "\"" + completeurl_cleanup + "\" ");
								page.append("data-originalname=" + "\"" + attachmentStore.full_filename_original(attachment, false) + "\" ");
							}
							if (isCleanedName) {
								page.append("data-originalname=" + "\"" + attachmentStore.full_filename_original(attachment, false) + "\"");
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
	 * this method returns html and js to be injected when a particular year is reached in browseAttachments.jspf
	 * These variables are then used to setup fancy box for attachment browsing process.
	 */
	public static Pair<String, String> htmlAndJSForAttachments(List<Document> docs, int year, boolean isHackyYearPresent, SearchResult searchResult, Multimap<String, String> queryparams) {
		JSPHelper.log.debug("Generating HTML for attachments in year: " + year);
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(EmailRenderer.EXCLUDED_EXT);
		} catch (Exception e) {
			Util.report_exception(e);
		}
		Archive archive = searchResult.getArchive();
		//get the set of attachments types that the user is interested in.
		Set<String> attachmentTypesOfInterest = IndexUtils.getAttachmentExtensionsOfInterest(queryparams);
		Map<Document, List<Blob>> docAttachmentMap = new LinkedHashMap<>();
		int numAttachments = 0;
		//step 1: get the set of attachments and their number for docs in year.
		for (Document doc : docs) {
			if (doc instanceof EmailDocument) {
				// for email docs, 1 doc = 1 page
				EmailDocument ed = (EmailDocument) doc;
				//collect attachments only if the year of this document is same as passed argument 'year'
				Calendar calendar = new GregorianCalendar();//should set timezone too.. @TODO
				calendar.setTime(ed.date);
				if (calendar.get(Calendar.YEAR) == year) {
					//prepare a list of attachments (not set) keeping possible repetition but also count unique attachments here.
					//docAttachmentMap.put(ed, ed.attachments);
					//NOTE: Don't put all attachments present here. We want to display only the attachments of interest to the user otherwise user will see all
					//the attachments present in a message that had at least one attachment type of interest. This was causing confusion to the users.
					List<Blob> attachments = ed.attachments;
					List<Blob> attachmentsOfInterest = new LinkedList<>();
					if (attachments != null)
						for (Blob b : attachments) {
							String ext = Util.getExtension(archive.getBlobStore().get_URL_Normalized(b));
							if (ext == null)
								ext = "Unidentified";
							ext = ext.toLowerCase();
							//Exclude any attachment whose extension is of the form EXCLUDED_EXT
							//or whose extension is not of interest.. [because it was not passed in attachmentType or attachmentExtension query on input.]

							if (pattern.matcher(ext).find()) {
								continue;//don't consider any attachment that has extension of the form [0-9]+
							}

							if (attachmentTypesOfInterest != null && !attachmentTypesOfInterest.contains(ext))
								continue;

							//else add it in the set of attachments to display for this doc.
							attachmentsOfInterest.add(b);

						}
					docAttachmentMap.put(ed, attachmentsOfInterest);

					numAttachments += attachmentsOfInterest.size();
				}
			}
		}

		StringBuilder html = new StringBuilder();

		html.append("<div class=\"muse-doc\">\n");



		JsonArray attachmentDetails = new JsonArray(); // this will be injected into the page directly, so fancybox js has access to it

		// create HTML for tiles view and append to page, also populate attachmentDetails
		boolean isNormalized = false; //a flag to detect if any of the attachment has normalization or cleanup info present because if it is then the
		int dupCount = 0;// Number of duplicate attachments (we just count the number of messages in which one attachment appears - we assume that one attachment can not be duplicated in one message)
		{
			int attachmentIndex = 0;
			//array to keep information about the attachments for display in UI.
			//information need to be presented to the user and hence the structure of the ui elements (like number of columns in the table) will change accordingly.
			//A variable to store number of messages in which an attachment appears. It also stores one of the attachments if it appears multiple times across messages.
			Map<String,Pair<JsonObject,Integer>> countMessagesMap = new LinkedHashMap<>();
			for (Document d : docAttachmentMap.keySet()) {
				//don't forget to increase msgIndex at the end of the following inner loop.
				for (Blob attachment : docAttachmentMap.get(d)) {
					JsonObject attachmentInfo = getAttachmentDetails(archive, attachment, d);
					//Insert attachmentInfo in an array of JSONObject (attachmentDetails) only if it is not seen previously.
					//If it already exists then increase the count of seen messages by 1.
					JsonElement attachmentName = attachmentInfo.get("filenameWithIndex");
					Pair<JsonObject,Integer> info;
					if(countMessagesMap.containsKey(attachmentName.toString())){
						info = countMessagesMap.get(attachmentName.toString());
						dupCount++;
					}else{
					 	info = new Pair(attachmentInfo,0);
					}
					//increment count by 1.
					info.second = info.second+1;
					countMessagesMap.put(attachmentName.toString(),info);
				}
			}
			//Now iterate over countMessagesMap to put JsonElement in the array.
			for (Pair<JsonObject,Integer> info: countMessagesMap.values()) {
				JsonObject attachmentInfo = info.first;
				attachmentInfo.addProperty("numMessages",info.second);
				attachmentInfo.addProperty("index",attachmentIndex);
				attachmentIndex++; //This index is used when selecting a specific tile that user clicks on the grid view.
				attachmentInfo.remove("filenameWithIndex");//no need to send it to the front end as it is only used to decide if two attachments were same or not.
				attachmentDetails.add(attachmentInfo);
				if (attachmentInfo.get("info") != null)
					isNormalized = true;//once set it can not be reset.
			}

		}


		//For list view and tile view buttons..
		{
			html.append("<div style=\"display:flex\">\n");
			if(dupCount!=0){
			    if(isHackyYearPresent && year==1960)
                    html.append("<div style=\"text-align:left;width:87%;margin-top:10px;font-family:\"Open Sans\",sans-serif;color:#666;font-size:16px;\">" + numAttachments + " attachments ("+dupCount+" duplicates) in undated messages </div>\n");
                else
                    html.append("<div style=\"text-align:left;width:87%;margin-top:10px;font-family:\"Open Sans\",sans-serif;color:#666;font-size:16px;\">" + numAttachments + " attachments ("+dupCount+" duplicates) in " + year + "</div>\n");

            }else{
                if(isHackyYearPresent && year==1960)
                    html.append("<div style=\"text-align:left;width:87%;margin-top:10px;font-family:\"Open Sans\",sans-serif;color:#666;font-size:16px;\">" + numAttachments + " attachments in undated messages </div>\n");
                else
                    html.append("<div style=\"text-align:left;width:87%;margin-top:10px;font-family:\"Open Sans\",sans-serif;color:#666;font-size:16px;\">" + numAttachments + " attachments in " + year + "</div>\n");
            }

			html.append("<div class=\"gallery_viewchangebar\" style=\"justify-content:flex-end\">\n");
			html.append("  <div title=\"List View\" class=\"listView\" onclick=\"showListView()\">\n" +
					"    <img class=\"fbox_toolbarimg\" id=\"listviewimg\" style=\"border-right:none;padding-left:10px;\" src=\"images/list_view.svg\"></img>\n" +
					"  </div>\n" +
					"  <div title=\"Grid View\"  class=\"tileView\" onclick=\"showTileView()\">\n" +
					"    <img class=\"fbox_toolbarimg\" id=\"tileviewimg\" style=\"height:28px;\" src=\"images/tile_view.svg\" ></img>\n" +
					"  </div>\n");
			html.append("</div>");
			html.append("</div>");
		}

		html.append("<hr/>\n<div class=\"attachments\">\n");
		StringBuilder tilediv = new StringBuilder();

		tilediv.append("<div id=\"attachment-tiles\" style=\"display:none\">");
		tilediv.append("</div> <!-- closing of attachment-tile div-->\n");
		//add tilediv to page.
		html.append(tilediv);
		// create HTML for list view and append to page
		{
			StringBuilder listdiv = new StringBuilder();
			listdiv.append("<div id=\"attachment-list\" style=\"display:none\">");
			listdiv.append("<table id=\"attachment-table\">\n");
			listdiv.append("<thead>\n");
			listdiv.append("<tr>\n");
			listdiv.append("<th>Subject</th>\n");
			listdiv.append("<th>Date</th>\n");
			listdiv.append("<th>Size</th>\n");
			listdiv.append("<th>Attachment name</th>\n");
			//add a field conditionally only if the information is also present for any attachment to be displayed to the user.
			if (isNormalized)
				listdiv.append("<th>More Information</th>\n");
			listdiv.append("</tr>\n");
			listdiv.append("</thead>\n");
			listdiv.append("<tbody>\n");
			listdiv.append("</tbody>\n");
			listdiv.append("</table>\n");
			listdiv.append("</div> <!-- closing of attachment--->\n");

			//add listdiv to page.
			html.append(listdiv);
		}

		// close html divs.
		html.append("\n</div>  <!-- .attachments -->\n"); // muse-doc-attachments
		html.append("\n</div>  <!-- .muse-doc -->\n"); // .muse-doc

		StringBuilder js = new StringBuilder();
		{

			if (isNormalized)
				js.append("isNormalized=true\n"); //to pass this information to the front end we assign it to a JS variable.
			js.append("var attachmentDetails=" + attachmentDetails.toString() + ";\n"); // note: no quotes should be present around attachmentDetails - it is simply a JS object in json notation
			// js.append("attachmentDetails=eval(attachmentDetailsStr);\n");
		/*	js.append("loadAttachmentTiles();\n"); //invoke method to setup tiles with attachmentDetails data.
			js.append("loadAttachmentList();\n"); //invoke method to setup datatable with attachmentDetails data.
			js.append("if(isListView){ $('#attachment-tiles').hide(); $('#attachment-list').show();} else{$('#attachment-list').hide(); $('#attachment-tiles').show() }\n");//hide the list
*/			//page.append("$('#attachment-list').hide();\n");//hide the list
		}

		return new Pair<>(html.toString(), js.toString());
	}

	 /*
		Method to extract some key details from an attachment that needs to be displayed in a fancybox in the gallery feature.
		@chinmay, can we get rid of these escapeHTML and escapeJSON and URLEncode?
	 */
	private static JsonObject getAttachmentDetails(Archive archive, Blob attachment, Document doc) {
		//prepare json object of the information. The format is
		//{index:'',href:'', from:'', date:'', subject:'',filename:'',downloadURL:'',tileThumbnailURL:'',msgURL:'',info:''}
		//here info field is optional and present only for those attachments which were converted or normalized during the ingestion and therefore need
		//to be notified to the user.
		JsonObject result = new JsonObject();
		String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);

		//Extract mail information
		//Extract few details like sender, date, message body (ellipsized upto some length) and put them in result.
		EmailDocument ed = (EmailDocument)doc;
		//A problematic case when converted json object was throwing error in JS.
		/*
		Case 1: {"from":"Δρ. Θεόδωρος Σίμος \r\n\t- Dr. ***** (redacted)","date":"Jan 5, 2005"}
		Solution: escapeJson will escape these to \\r\\n\\t.

		Case 2: {"from":"李升荧","date":"Dec 19, 2012","subject":"shangwu@jxdyf.com，Please find
			."}
			Problem: There is a newline in subject between find and .
			Solution: escapejson will put \ at the end of find which will make it parsed correctly.
		 */

		String sender = Util.escapeHTML(ed.getFromString());//escaping because we might have the name like <jbush@..> in the sender.
		sender = Util.escapeJSON(sender);
		String date = Util.escapeHTML(ed.dateString());
		String subject= Util.escapeHTML(ed.description);
		subject=Util.escapeJSON(subject);
		String docId = ed.getUniqueId();
		//for caption of the assignment
		BlobStore attachmentStore = archive.getBlobStore();
		String filename = attachmentStore.full_filename_normalized(attachment, false);

		//IMP: We want to open set of all those messages which have this attachment. Therefore we don't use docID to open the message.
		//String messageURL = "browse?archiveID="+archiveID+"&docId=" + docId;
		//Use browse?archiveID=...&adv-search=1&attachmentFilename= as the msgurl.
		result.addProperty("filename",Util.escapeHTML(filename));
		String numberedFileName = attachmentStore.full_filename_normalized(attachment,true);
		String messageURL = "browse?archiveID="+archiveID+"&adv-search=1&attachmentFileWithNumber="+Util.URLEncode(numberedFileName);
		result.addProperty("filenameWithIndex",numberedFileName);

		result.addProperty("from",sender);
		result.addProperty("date",date);
		result.addProperty("subject",subject);
		result.addProperty("msgURL",messageURL);

		//Extract few details like attachment src, thumnbail, search for message url etc and put them in result.
		//tilethumbnailURL is the url of the image displayed on small tile in the gallery landing page
		//thumbnailURL is the url of the image displayed in the gallery mode (inside fancybox). For now both are same but they can be made different
		//later therefore the distinction here.
		String thumbnailURL = null, downloadURL = null, tileThumbnailURL=null;
		if (attachmentStore != null) {
			String contentFileDataStoreURL = attachmentStore.get_URL_Normalized(attachment);
			//IMP: We need to do URLEncode otherwise if filename contains (') then the object creation from json data fails in the frontend.
			//EX. If file's name is Jim's
			downloadURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLEncode(Util.URLtail(contentFileDataStoreURL));
			String tnFileDataStoreURL = attachmentStore.getViewURL(attachment, "tn");
			if (tnFileDataStoreURL != null) {
				thumbnailURL = "serveAttachment.jsp?archiveID=" + archiveID + "&file=" + Util.URLEncode(Util.URLtail(tnFileDataStoreURL));
				//set tile's thumbnail (on the landing page of gallery) also same.
				tileThumbnailURL = thumbnailURL;
			}
			else {
				if (archive.getBlobStore().is_image(attachment)) {
					//We need special handling for tif images. Because they are not being
					//rendered by chrome or firefox but are supported only by safari. However, we are not going to add browser specific code here
					//and may be wait for the day when both chrome and firefox start supporting them.
					if(Util.getExtension(contentFileDataStoreURL).equals("tif")){
						//handle it like non-previewable file.
						thumbnailURL="images/tiff_icon.svg";
						tileThumbnailURL="images/tiff_icon.svg";
					}else {
						thumbnailURL = downloadURL;
						tileThumbnailURL = thumbnailURL;//may be we need to reduce it's size.@TODO
					}
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


		if(thumbnailURL==null)
			thumbnailURL = "images/large_sorry_img.svg";
		//downloadURL should never be null.
		boolean isNormalized = attachmentStore.isNormalized(attachment);
		boolean isCleanedName = attachmentStore.isCleaned(attachment);
		String cleanupurl = attachmentStore.get_URL_Cleanedup(attachment);

		String info="";
		if(isNormalized || isCleanedName){
			String completeurl_cleanup ="serveAttachment.jsp?archiveID="+archiveID+"&file=" + Util.URLEncode(Util.URLtail(cleanupurl));
			if(isNormalized){
				info="This file was converted during the preservation process. Its original name was "+attachmentStore.full_filename_original(attachment,false)+". Click <a href="+completeurl_cleanup+">here </a> to download the original file";
			}
			else if(isCleanedName){
				info="This file name was cleaned up during the preservation process. The original file name was "+attachmentStore.full_filename_original(attachment,false);
			}
		}
		//{index:'',href:'', from:'', date:'', subject:'',filename:'',downloadURL:'',tileThumbnailURL:'',info:'',size:''}

		result.addProperty("size",attachment.size);
		result.addProperty("href",thumbnailURL);
		result.addProperty("downloadURL",downloadURL);
		result.addProperty("tileThumbnailURL",tileThumbnailURL);
		if(!Util.nullOrEmpty(info)) //add this field only if this is non-empty. (That is the beauty of json, non-fixed structure for the data).
			result.addProperty("info",info);

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
															 MultiDoc.ClusteringType coptions, Multimap<String,String> queryparams) {

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

		DataSet dataset = new DataSet(datasetDocs, result, datasetTitle,queryparams);
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
		result.append("\n</td></tr>\n");

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
        Arrays.stream(names).filter(n -> recMap.containsKey(NEType.getCoarseType(n.type).getCode()))
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
        public Set<String> types;

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
    public static void main(String args[]){
    	/*Test to show that we should use JsonObject instead of JSONObject as it correctly escapes the things needed to create Json object back in JS*/
		JSONObject result = new JSONObject();
		result.put("a","my name ");
		result.put("b","I'am a");
		JsonObject res = new JsonObject();
		JsonArray arr = new JsonArray();
		res.addProperty("a","<jbush@..>" +
				".");
		res.addProperty("b","I'm a");
		arr.add(res);
		arr.add(res);
		System.out.println("var bb = '" + new Gson().toJson(arr)+"'");
	}
}
