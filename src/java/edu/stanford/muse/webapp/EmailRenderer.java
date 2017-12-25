package edu.stanford.muse.webapp;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.AddressBookManager.AddressBook;
import edu.stanford.muse.email.AddressBookManager.Contact;
import edu.stanford.muse.email.LabelManager.LabelManager;
import edu.stanford.muse.index.*;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.JSONUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;
import netscape.javascript.JSObject;
import org.json.simple.JSONValue;

/** This class has util methods to display an email message in an html page */

public class EmailRenderer {

	private static final int	TEXT_WRAP_WIDTH	= 80;	// used to be 80, but that wraps
												// around too soon. 120 is too
												// much with courier font.

    /*public static Pair<DataSet, String> pagesForDocuments(Collection<Document> ds, Archive archive, String datasetTitle,
                                                          Set<String> highlightTerms)
            throws Exception{
        return pagesForDocuments(ds, archive, datasetTitle, null, highlightTerms, null, MultiDoc.ClusteringType.MONTHLY);
    }

    public static Pair<DataSet, String> pagesForDocuments(Collection<Document> ds, Archive archive, String datasetTitle,
                                                          Set<String> highlightTerms, Collection<Blob> highlightAttachments)
            throws Exception{
        return pagesForDocuments(ds, archive, datasetTitle, null, highlightTerms, highlightAttachments,
                MultiDoc.ClusteringType.MONTHLY);
    }

    public static Pair<DataSet, String> pagesForDocuments(Collection<Document> ds, Archive archive, String datasetTitle,
														  Set<Integer> highlightContactIds, Set<String> highlightTerms)
			throws Exception{
		return pagesForDocuments(ds, archive, datasetTitle, highlightContactIds, highlightTerms, null, MultiDoc.ClusteringType.MONTHLY);
	}
*/
	public static Pair<DataSet, String> pagesForDocuments(SearchResult result,
                                                          String datasetTitle)
            throws Exception{
        return pagesForDocuments(result,datasetTitle,MultiDoc.ClusteringType.MONTHLY);
    }

	/*
	 * returns pages and html for a collection of docs, which can be put into a
	 * jog frame. indexer clusters are used to
	 *
	 * Changed the first arg type from: Collection<? extends EmailDocument> to Collection<Document>, as we get C
	 * ollection<Document> in browse page or from docsforquery, its a hassle to make them all return EmailDocument
	 * especially when no other document type is used anywhere
	 */
    public static Pair<DataSet, String> pagesForDocuments(SearchResult result,
                                                          String datasetTitle,
                                                          MultiDoc.ClusteringType coptions)
			throws Exception
    {
		StringBuilder html = new StringBuilder();
		int pageNum = 0;
		List<String> pages = new ArrayList<String>();

		// need clusters which map to sections in the browsing interface
		List<MultiDoc> clusters;

        // indexer may or may not have indexed all the docs in ds
		// if it has, use its clustering (could be yearly or monthly or category
		// wise)
		// if (indexer != null && indexer.clustersIncludeAllDocs(ds))
		// if (indexer != null)
		clusters = result.getArchive().clustersForDocs(result.getDocumentSet(), coptions);
		/*
		 * else { // categorize by month if the docs have dates if
		 * (EmailUtils.allDocsAreDatedDocs(ds)) clusters =
		 * IndexUtils.partitionDocsByInterval(new ArrayList<DatedDocument>((Set)
		 * ds), true); else // must be category docs clusters =
		 * CategoryDocument.clustersDocsByCategoryName((Collection) ds); }
		 */

		List<Document> datasetDocs = new ArrayList<>();
		// we build up a hierarchy of <section, document, page>
		for (MultiDoc md : clusters)
		{
			if (md.docs.size() == 0)
				continue;

			String description = md.description;
			description = description.replace("\"", "\\\""); // escape a double
																// quote if any
																// in the
																// description
			html.append("<div class=\"section\" name=\"" + description + "\">\n");

			List<List<String>> clusterResult = new ArrayList<>();

			for (Document d : md.docs)
			{
				String pdfAttrib = "";
				/*
				 * if (d instanceof PDFDocument) pdfAttrib = "pdfLink=\"" +
				 * ((PDFDocument) d).relativeURLForPDF + "\"";
				 */
				html.append("<div class=\"document\" " + pdfAttrib + ">\n");

				datasetDocs.add(d);
				pages.add(null);
				clusterResult.add(null);
				// clusterResult.add(docPageList);
				// for (String s: docPageList)
				{
					String comment = Util.escapeHTML(d.comment);
					html.append("<div class=\"page\"");
					if (!Util.nullOrEmpty(comment))
						html.append(" comment=\"" + comment + "\"");

					if (!Util.nullOrEmpty(comment) && (d instanceof EmailDocument))
					{
						String messageId = d.getUniqueId();
						html.append(" messageID=\"" + messageId + "\"");
					}
					if (d.isLiked())
						html.append(" liked=\"true\"");
					/*
					if (d instanceof EmailDocument && ((EmailDocument) d).doNotTransfer)
						html.append(" doNotTransfer=\"true\"");
					if (d instanceof EmailDocument && ((EmailDocument) d).transferWithRestrictions)
						html.append(" transferWithRestrictions=\"true\"");
					if (d instanceof EmailDocument && ((EmailDocument) d).reviewed)
						html.append(" reviewed=\"true\"");
					*/
					//getting labels for this document and setting them under different attributes, i.e. systemlabels, restrlabels and genlabels.
					//also make sure that browse.jsp (the jsp calling this function) should have a map of LabelID to Label Name, Label type in javascript
					if(d instanceof EmailDocument) {
						Set<Integer> systemlabels = result.getArchive().getLabels((EmailDocument) d, LabelManager.LabType.SYSTEM_LAB);
						Set<Integer> restrlabels = result.getArchive().getLabels((EmailDocument) d, LabelManager.LabType.RESTR_LAB);
						Set<Integer> genlabels = result.getArchive().getLabels((EmailDocument) d, LabelManager.LabType.GEN_LAB);
						Set<Integer> labels = Util.setUnion(Util.setUnion(systemlabels,restrlabels),genlabels);
						if (!Util.nullOrEmpty(labels)) {
							String val = systemlabels.stream().map(f -> f.toString()).collect(Collectors.joining(","));
							html.append(" labels=\"" + val +"\"");
						}
					}


					//////////////////////////////////////////DONE reading labels///////////////////////////////////////////////////////////////////////////
					if (d instanceof EmailDocument && ((EmailDocument) d).addedToCart)
						html.append(" addToCart=\"true\"");
					if (d instanceof EmailDocument)
						html.append(" pageId='" + pageNum++ + "' " + " signature='" + Util.hash (((EmailDocument) d).getSignature()) + "' docId='" + d.getUniqueId() + "'></div>\n");
				}

				html.append("</div>"); // document
			}
			html.append("</div>\n"); // section
		}

		DataSet dataset = new DataSet(datasetDocs, result, datasetTitle);

		return new Pair<>(dataset, html.toString());
	}

	/**
	 * format given addresses as comma separated html, linewrap after given
	 * number of chars
	 * 
	 * @param addressBook
	 */
	private static String formatAddressesAsHTML(Address addrs[], AddressBook addressBook, int lineWrap, Set<String> highlightUnstemmed, Set<String> highlightNames, Set<String> highlightAddresses)
	{
		StringBuilder sb = new StringBuilder();
		int outputLineLength = 0;
		for (int i = 0; i < addrs.length; i++)
		{
			String thisAddrStr;

			Address a = addrs[i];
			if (a instanceof InternetAddress)
			{
				InternetAddress ia = (InternetAddress) a;
				Pair<String, String> p = JSPHelper.getNameAndURL((InternetAddress) a, addressBook);
				String url = p.getSecond();
				String str = ia.toString();
                String addr = ia.getAddress();
                boolean match = false;
                if(str!=null) {
                    //The goal here is to explain why a doc is selected and hence we should replicate Lucene doc selection and Lucene is case insensitive most of the times
                    String lc = str.toLowerCase();
                    if (highlightUnstemmed != null)
                        for (String hs : highlightUnstemmed) {
                            String hlc = hs.toLowerCase().replaceAll("^\\W+|\\W+$","");
                            if (lc.contains(hlc)) {
                                match = true;
                                break;
                            }
                        }
                    if (!match && highlightNames != null)
                        for (String hn : highlightNames) {
                            String hlc = hn.toLowerCase().replaceAll("^\\W+|\\W+$","");
                            if (lc.contains(hlc)) {
                                match = true;
                                break;
                            }
                        }
                }
                if(addr!=null){
                    if (!match && highlightAddresses != null)
                        for (String ha : highlightAddresses)
                            if (addr.contains(ha)) {
                                match = true;
                                break;
                            }
                }

                if(match)
                    thisAddrStr = ("<a href=\"" + url + "\"><span class=\"hilitedTerm rounded\">" + Util.escapeHTML(str) + "</span></a>");
                else
                    thisAddrStr = ("<a href=\"" + url + "\">" + Util.escapeHTML(str) + "</a>");

				if (str != null)
	                outputLineLength += str.length();
			}
			else
			{
				String str = a.toString();
				thisAddrStr = str;
				outputLineLength += str.length();
                JSPHelper.log.warn("Address is not an instance of InternetAddress - is of instance: "+a.getClass().getName() + ", highlighting won't work.");
			}

			if (i + 1 < addrs.length)
				outputLineLength += 2; // +2 for the comma that will follow...

			if (outputLineLength + 2 > lineWrap)
			{
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
	 * returns a string for documents.
	 * 
	 * @param
	 * @throws Exception
	 */
    //TODO: inFull, debug params can be removed
    //TODO: Consider a HighlighterOptions class
	public static Pair<String, Boolean> htmlForDocument(Document d, SearchResult searchResult, String datasetTitle,
                                                        Map<String, Map<String, Short>> authorisedEntities,
			                                            boolean IA_links, boolean inFull, boolean debug,String archiveID) throws Exception
	{
		JSPHelper.log.debug("Generating HTML for document: " + d);
		EmailDocument ed = null;
		String html = null;
		boolean overflow = false;
		if (d instanceof EmailDocument)
		{
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
			if (attachments != null && attachments.size() > 0)
			{
				// show thumbnails of all the attachments

				if (ModeConfig.isPublicMode()) {
					page.append(attachments.size() + " attachment" + (attachments.size() == 1 ? "" : "s") + ".");
				} else {
					page.append("<hr/>\n<div class=\"attachments\">\n");
					page.append("<table>\n");
					int i = 0;
					for (; i < attachments.size(); i++)
					{
						if (i % 4 == 0)
							page.append((i == 0) ? "<tr>\n" : "</tr><tr>\n");
						page.append("<td>");

						Blob attachment = attachments.get(i);
						String thumbnailURL = null, attachmentURL = null;
						boolean is_image = Util.is_image_filename(attachment.filename);
                        BlobStore attachmentStore = searchResult.getArchive().getBlobStore();
						if (attachmentStore!= null)
						{
							String contentFileDataStoreURL = attachmentStore.get_URL(attachment);
							attachmentURL = "serveAttachment.jsp?archiveID="+archiveID+"&file=" + Util.URLtail(contentFileDataStoreURL);
							String tnFileDataStoreURL = attachmentStore.getViewURL(attachment, "tn");
							if (tnFileDataStoreURL != null)
								thumbnailURL = "serveAttachment.jsp?archiveID="+archiveID+"&file=" + Util.URLtail(tnFileDataStoreURL);
							else
							{
								if (attachment.is_image())
									thumbnailURL = attachmentURL;
								else
									thumbnailURL = "images/sorry.png";
							}
						}
						else
							JSPHelper.log.warn("attachments store is null!");

						// toString the filename in any case,
						String s = attachment.filename;
						// cap to a length of 25, otherwise the attachment name
						// overflows the tn
						String display = Util.ellipsize(s, 25);
                        boolean highlight = highlightAttachments != null && highlightAttachments.contains(attachment);
                        page.append("&nbsp;" + "<span title=\"" + Util.escapeHTML(s) + "\" class='" + (highlight?"highlight":"") + "'>"+ Util.escapeHTML(display) + "</span>&nbsp;");
						page.append("<br/>");

						String css_class = "attachment-preview" + (is_image ? " img" : "") + (highlight ? " highlight" : "");
						String leader = "<img class=\"" + css_class + "\" ";

						// punt on the thumbnail if the attachment tn or content
						// URL is not found
						if (thumbnailURL != null && attachmentURL != null)
						{
							// d.hashCode() is just something to identify this
							// page/message
							page.append("<a rel=\"page" + d.hashCode() + "\" title=\"" + attachment.filename + "\" class=\"" + (highlight?"highlight":"") + "\" href=\"" + attachmentURL + "\">");
							page.append(leader + "href=\"" + attachmentURL + "\" src=\"" + thumbnailURL + "\"></img>\n");
							page.append("<a>\n");
						}
						else
						{
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
						page.append("</td>\n");
					}
					if (i % 4 != 0)
						page.append("</tr>");
					page.append("</table>");
					page.append("\n</div>  <!-- .muse-doc-attachments -->\n"); // muse-doc-attachments
				}

			}
			page.append("\n</div>  <!-- .muse-doc -->\n"); // .muse-doc
			html = page.toString();
		}
		else if (d instanceof DatedDocument)
		{
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
		}
		else
		{
			JSPHelper.log.warn("Unsupported Document: " + d.getClass().getName());
			html = "";
		}

		return new Pair<String, Boolean>(html, overflow);
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
        //get contact ids from searchResult object.
        Set<Integer> highlightContactIds = searchResult.getHLInfoContactIDs().stream().map(d->Integer.parseInt(d)).collect(Collectors.toSet());
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
		result.append(JSPHelper.getHTMLForDate(ed.date));

		final String style = "<tr><td align=\"right\" class=\"muted\" valign=\"top\">";

		// email specific headers
		result.append(style + "From: </td><td align=\"left\">");
		Address[] addrs = ed.from;
		if (addrs != null)
		{
			result.append(formatAddressesAsHTML(addrs, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses));
		}

		result.append(style + "To: </td><td align=\"left\">");
		addrs = ed.to;
		if (addrs != null)
			result.append(formatAddressesAsHTML(addrs, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses) + "");

		result.append("\n</td></tr>\n");

		if (ed.cc != null && ed.cc.length > 0)
		{
			result.append(style + "Cc: </td><td align=\"left\">");
			result.append(formatAddressesAsHTML(ed.cc, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses) + "");
			result.append("\n</td></tr>\n");
		}

		if (ed.bcc != null && ed.bcc.length > 0)
		{
			result.append(style + "Bcc: </td><td align=\"left\">");
			result.append(formatAddressesAsHTML(ed.bcc, addressBook, TEXT_WRAP_WIDTH, highlightTerms, contactNames, contactAddresses) + "");
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
        Arrays.asList(names).stream().filter(n -> recMap.keySet().contains(NEType.getCoarseType(n.type).getCode()))
                .forEach(n -> {
                    Set<String> types = new HashSet<>();
                    types.add(recMap.get(NEType.getCoarseType(n.type).getCode()));
                    entitiesWithId.put(n.text, new Entity(n.text, null, types));
                });

        x = searchResult.getArchive().annotate(x, ed.getDate(), ed.getUniqueId(), searchResult.getRegexToHighlight(), highlightTerms, entitiesWithId, IA_links, false);

		result.append(x);
		result.append("</b>\n");
		result.append("\n</td></tr>\n");
		String messageId = Util.hash (ed.getSignature());
		String messageLink = "(<a href=\"browse?adv-search=1&uniqueId=" + messageId + "\">Link</a>)";
		result.append ("\n" + style + "ID: " + "</td><td>" + messageId + " " + messageLink + "</td></tr>");
		result.append("</table>\n"); // end docheader table

		if (ModeConfig.isPublicMode())
			return new StringBuilder(Util.maskEmailDomain(result.toString()));

		return result;
	}

	/** I'm not sure what this is used for -- I think its used only for rendering HTML for the message. */
    public static class Entity {
        public Map<String, Short> ids;
        //person,places,orgs, custom
        public String name;
        public Set<String> types = new HashSet<String>();

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
