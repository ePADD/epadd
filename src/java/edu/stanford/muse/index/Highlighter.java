package edu.stanford.muse.index;

import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.EmailRenderer;
import edu.stanford.muse.webapp.ModeConfig;
import edu.stanford.muse.webapp.SimpleSessions;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Use this class to get the HTML content of an email document.
 * Given the content, terms to highlight, terms to hyperlink and entities in the doc, it generates the HTML of the content with highlights and hyperlinks.
 * Bugs
 * 1. while highlighting preset query -- a regexp like: \d{3}-\d{2}-\d{4} highlights 022-29), 1114 as <B>022-29), 11</B>14.
 *      This is due to improper offsets in token stream or could be because lucene highlighter is considering endoffset like startoffset+token.length()
 */
class Highlighter {
    private static Log log = LogFactory.getLog(Highlighter.class);

    private static Random			randnum			= new Random();
    static {
        randnum.setSeed(123456789);
    }
    /**
     * @param content - text to be highlighted
     * @param term - This can be a generic query passed to the Lucene search, for example: elate|happy|invite, hope, "Robert Creeley", /guth.+/ , /[0-9\\-]*[0-9]{3}[- ][0-9]{2}[- ][0-9]{4}[0-9\\-]+/ are all valid terms
     * @param preTag - HTML pre-tag, for ex: <B>
     * @param postTag - HTML post-tag, for ex: </B>
     * The highlighted content would have [pre Tag] matching term [post tag]
     * When the term is "Robert Creeley", the output is "On Tue, Jun 24, 2014 at 11:56 AM, [preTag]Robert Creeley's[postTag] <creeley@acsu.buffalo.edu> wrote:"
     */

    /** debug method only */
    private static String dumpTokenStream(Analyzer analyzer, TokenStream tokenStream) throws IOException {
        // taken from https://stackoverflow.com/questions/2638200/how-to-get-a-token-from-a-lucene-tokenstream
        OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        StringBuilder sb = new StringBuilder();
        sb.append ("Tokens:\n");
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            int startOffset = offsetAttribute.startOffset();
            int endOffset = offsetAttribute.endOffset();
            String term = charTermAttribute.toString();
            sb.append (term + "(offsets: " + startOffset + ", " + endOffset + ")\n");
        }

        tokenStream.reset();
        return sb.toString();
    }

    private static String highlight(String content, String term, String preTag, String postTag) throws IOException, ParseException, InvalidTokenOffsetsException{
        //The Lucene Highlighter is used in a hacky way here, it is intended to be used to retrieve fragments from a matching Lucene document.
        //The Lucene Highlighter introduces tags around every token that matched the query, hence it is required to merge these fragmented annotations into one inorder to fit our needs.
        //To truly differentiate contiguous fragments that match a term supplied we add a unique id to the pretag, hence the randum instance
        //TODO: Explain what is happening here
        //Version lv = Indexer.LUCENE_VERSION;
        //hell with reset close, stuff. initialized two analyzers to evade the problem.
        //TODO: get rid of two analyzers.
        Analyzer snAnalyzer, snAnalyzer2;
        snAnalyzer = new EnglishNumberAnalyzer( CharArraySet.EMPTY_SET);
        snAnalyzer2 = new EnglishNumberAnalyzer( CharArraySet.EMPTY_SET);

        Fragmenter fragmenter = new NullFragmenter();
        QueryParser qp = new MultiFieldQueryParser(new String[]{""}, snAnalyzer2);

        BooleanQuery.Builder querybuilder = new BooleanQuery.Builder();
        TokenStream stream = snAnalyzer.tokenStream(null, new StringReader(content));
        int r = randnum.nextInt();
        String upreTag = preTag.replaceAll(">$", " data-ignore=" + r + " >");
        Formatter formatter = new SimpleHTMLFormatter(upreTag, postTag);
        //Parse exception may occur while parsing terms like "AND", "OR" etc.
        try {
            querybuilder.add(new BooleanClause(qp.parse(term), BooleanClause.Occur.SHOULD));
        }catch(ParseException pe){
            if(log.isDebugEnabled())
                log.debug("Exception while parsing: "+term,pe);
            return content;
        }
        Scorer scorer = new QueryScorer(querybuilder.build());
        org.apache.lucene.search.highlight.Highlighter highlighter = new org.apache.lucene.search.highlight.Highlighter(formatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Math.max(org.apache.lucene.search.highlight.Highlighter.DEFAULT_MAX_CHARS_TO_ANALYZE,content.length()));
        String result = highlighter.getBestFragment(stream, content);
        snAnalyzer.close();
        snAnalyzer2.close();

        if(result!=null) {
            result = mergeContiguousFragments(result, term, upreTag, postTag);
            //and then remove the extra info. we appended to the tags
            result = result.replaceAll(" data-ignore=" + r + " >", ">");
            return result;
        }
        else return content;
    }

    private static String highlightBatch(String content, String[] terms, String preTag, String posTag) {
        for(String term: terms){
            try {
                content = highlight(content, term, preTag, posTag);
            } catch(IOException|ParseException|InvalidTokenOffsetsException e){
                Util.print_exception("Exception while highlighting for the term: "+content, e, log);
            }
        }
        return content;
    }

    /**
     * Just highlighting content with Lucene Highlighter will fragment annotations around tokens
     * for example ...<B>Rep</B>. <B>Simmons</B>... when the term is "Rep. Simmons"
     * ...<B>X</B> & <B>Y</B>- <B>Z</B> when the term is "X & Y- Z"
     * It is required to merge all such fragments into one so as to add hyperlink or other manipulation over the annotations
     * In this particular case, regexp replacing is much easier than html parsing.
     * [preTag]Robert[postTag] [preTag]Creeley's[postTag] ==> [preTag]Robert Creeley's[postTag]
     * [preTag]Robert Creeley[posTag] [preTag]UB[postTag]
     * */
    private static String mergeContiguousFragments(String result, String term, String preTag, String postTag) {
        Pattern patt;
        //Note: it is assumed that postTag starts with '<', else this method is being misused and hence will fail
        patt = Pattern.compile(preTag+"([^<]*?)"+postTag+"(\\W+)"+preTag);

        String prevResult;
        do {
            Matcher m = patt.matcher(result);
            StringBuffer sb = new StringBuffer();
            if(m.find()) {
                String grp1 = m.group(1), grp2 = m.group(2);
                if (term.contains(grp1 + grp2))
                    m.appendReplacement(sb, preTag + grp1 + grp2);
                //System.out.println("Found: " + m.group() + " replacing with " + preTag + grp1 + grp2);
            }
            m.appendTail(sb);
            prevResult = result;
            result = sb.toString();
        }while(!result.equals(prevResult));
        return result;
    }

    private static String annotateRegex(String content, String regexToHighlight, String preTag, String postTag) {
        if (Util.nullOrEmpty(regexToHighlight))
            return content;

        //We expand the query to match any numbers to the left and right of queried regular exp as the chunker is aggressive and chunks any numbers occurring together into one even if they are in different lines
        //qs = new String[] { "[0-9]{3}[- ][0-9]{2}[- ][0-9]{4}", "3[0-9]{3}[-. ][0-9]{6}[-. ][0-9]{5}" };
        String[] queries = new String[1];
        queries[0] = "/" + regexToHighlight + "/";

        String result = null;
        try {
            result = highlightBatch(content, queries, preTag, postTag);
        } catch (Exception e) {
            Util.print_exception("Exception while highlighting sensitive stuff", e, log);
        }
        if (result == null) {
            System.err.println("Result is null!!");
            return content;
        }
        return result;
    }

    /**
     * @param contents is the content to be annotated, typically the text in email body
     * A convenience method to do the bulk job of annotating all the terms in termsToHighlight, termsToHyperlink and entitiesWithId
     * Also hyperlinks any URLs found in the content
     * @param regexToHighlight - the output will highlight all the strings matching regexToHighlight
     * @param showDebugInfo - when set will append to the output some debug info. related to the entities present in the content and passed through entitiesWithId
     *
     * Note: DO not modify any of the objects passed in the parameter
     *       if need to be modified then clone and modify a local copy
     * */
    //TODO: can also get rid of termsToHyperlink
    public static String getHTMLAnnotatedDocumentContents(String contents, Date d, String docId, String regexToHighlight,
                                                          Set<String> termsToHighlight, Map<String, EmailRenderer.Entity> entitiesWithId,
                                                          Set<String> termsToHyperlink, boolean showDebugInfo) {
        Set<String> highlightTerms = new LinkedHashSet<>(), hyperlinkTerms = new LinkedHashSet<>();
        if(termsToHighlight!=null) highlightTerms.addAll(termsToHighlight);
        if(termsToHyperlink!= null) hyperlinkTerms.addAll(termsToHyperlink);

        // remove these special words from the list of hyperlinkterms, otherwise, we just mess up the HTML if any of them is in the hyperlinkTerms


        if(log.isDebugEnabled())
            log.debug("DocId: "+docId+"; Highlight terms: " + highlightTerms+"; Entities: " + entitiesWithId+"; Hyperlink terms: " + hyperlinkTerms);
        //System.err.println("DocId: " + docId + "; Highlight terms: " + highlightTerms + "; Entities: " + entitiesWithId + "; Hyperlink terms: " + hyperlinkTerms);

        short HIGHLIGHT = 0, HYPERLINK = 1;
        //pp for post process, as we cannot add complex tags which highlighting
        String preHighlightTag = "<span class='hilitedTerm rounded' >", postHighlightTag = "</span>";
        String preHyperlinkTag = "<span data-process='pp'>", postHyperlinkTag = "</span>";

        //since the urls are not tokenized as one token, it is not possible to highlight with Lucene highlighter.
        Pattern p = Pattern.compile("https?://[^\\s\\n]*");
        Matcher m = p.matcher(contents);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String link = m.group();
            String url = link;
            if (d != null) {
                Calendar c = new GregorianCalendar();
                c.setTime(d);
                String archiveDate = c.get(Calendar.YEAR) + String.format("%02d", c.get(Calendar.MONTH)) + String.format("%02d", c.get(Calendar.DATE))
                        + "120000";
                url = "http://web.archive.org/web/" + archiveDate + "/" + link;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement("<a target=\"_blank\" href=\"" + url + "\">" + link + "</a> "));
        }
        m.appendTail(sb);
        contents = sb.toString();

        if (!Util.nullOrEmpty (regexToHighlight)) {
            contents = annotateRegex(contents, regexToHighlight, preHighlightTag, postHighlightTag);
        }

        List<String> catchTerms = Arrays.asList("class","span","data","ignore");
        Set<String> ignoreTermsForHyperlinking = catchTerms.stream().map (String::toLowerCase).collect (Collectors.toSet());

        //entitiesid stuff is already canonicalized with tokenize used with analyzer
        if (entitiesWithId != null)
            hyperlinkTerms.addAll(entitiesWithId.keySet().stream().filter (term -> !ignoreTermsForHyperlinking.contains (term.trim().toLowerCase())).map(term -> "\"" + term + "\"").collect(Collectors.toSet()));

        //If there are overlapping annotations, then they need to be serialised.
        //This is serialized order for such annotations.
        //map strings to be annotated -> boolean denoting whether to highlight or hyperlink.
        List<Pair<String, Short>> order = new ArrayList<>();
        //should preserve order so that highlight terms are seen before hyperlink
        Set<String> allTerms = new LinkedHashSet<>();
        allTerms.addAll(highlightTerms);

		/*
		 * We ant to assign order in which terms are highlighted or hyperlinked.
		 * for example: if we want to annotate both "Robert" and "Robert Creeley", and if we annotate "Robert" first then we may miss on "Robert Creeley"
		 * so we assign order over strings that share any common words as done in the loop below
		 * TODO:
		 * This test can still miss cases when a regular expression that eventually matches a word already annotated and
		 * when two terms like "Robert Creeley" "Mr Robert" to match a text like: "Mr Robert Creeley".
		 * TODO: Give pref. to highlighter over hyperlink
		 * TODO: remove order and simplify
		 * In such cases one of the terms may not be annotated.
		 * Terms that are added to o are those that just share at-least one word
		 */
        //should preserve order so that highlight terms that are added first stay that way
        Map<Pair<String, Short>, Integer> o = new LinkedHashMap<>();
        //prioritised terms


        //Note that a term can be marked both for highlight and hyperlink
        Set<String> consTermsHighlight = new HashSet<>(), consTermsHyperlink = new HashSet<>();
        for (String at : allTerms) {
            //Catch: if we are trying to highlight terms like class, span e.t.c,
            //we better annotate them first as it may go into span tags and annotate the stuff, causing the highlighter to break
            Set<String> substrs = IndexUtils.computeAllSubstrings(at);
            for (String substr : substrs) {
                if(at.equals(substr) || at.equals("\""+substr+"\""))
                    continue;

                boolean match = catchTerms.contains(substr.toLowerCase());
                int val = match?Integer.MAX_VALUE:substr.length();
                //remove it from terms to be annotated.
                //The highlight or hyperlink terms may have quotes, specially handling below is for that.. is there a better way?
                if (highlightTerms.contains(substr) || highlightTerms.contains("\""+substr+"\"")) {
                    highlightTerms.remove(substr);
                    highlightTerms.remove("\""+substr+"\"");
                    //there should be no repetitions in the order array, else it leads to multiple annotations i.e. two spans around one single element
                    if(!consTermsHighlight.contains(substr)) {
                        o.put(new Pair<>(substr, HIGHLIGHT), val);
                        consTermsHighlight.add(substr);
                    }
                }
                if (hyperlinkTerms.contains(substr) || hyperlinkTerms.contains("\""+substr+"\"")) {
                    hyperlinkTerms.remove(substr);
                    hyperlinkTerms.remove("\"" + substr + "\"");
                    if(!consTermsHyperlink.contains(substr)) {
                        o.put(new Pair<>(substr, HYPERLINK), val);
                        consTermsHyperlink.add(substr);
                    }
                }
            }
        }

        //now sort the phrases from longest length to smallest length
        List<Pair<Pair<String, Short>, Integer>> os = Util.sortMapByValue(o);
        order.addAll(os.stream().map(pair -> pair.first).collect(Collectors.toSet()));
        //System.err.println(order+" hit: "+highlightTerms+" -- hyt: "+hyperlinkTerms);

        //annotate whatever is left in highlight and hyperlink Terms.
//        String result = contents;
        String result = highlightBatch(contents, highlightTerms.toArray(new String[highlightTerms.size()]), preHighlightTag, postHighlightTag);
        result = highlightBatch(result, hyperlinkTerms.toArray(new String[hyperlinkTerms.size()]), preHyperlinkTag, postHyperlinkTag);

//        System.out.println("Terms to highlight: " + termsToHighlight);
//        System.out.println("Terms to hyperlink: "+termsToHyperlink);
//        System.out.println("order: "+order);

        //need to post process.
        //now highlight terms in order.
        for (Pair<String, Short> ann : order) {
            short type = ann.second;
            String term = ann.first;
            String preTag = null, postTag = null;
            if (type == HYPERLINK) {
                preTag = preHyperlinkTag;
                postTag = postHyperlinkTag;
            } else if (type == HIGHLIGHT) {
                preTag = preHighlightTag;
                postTag = postHighlightTag;
            }

            try {
                result = highlight(result, term, preTag, postTag);
            } catch (IOException | InvalidTokenOffsetsException | ParseException e) {
                Util.print_exception("Exception while adding html annotation: " + ann.first, e, log);
                e.printStackTrace();
            }
        }
        //do some line breaking and show overflow.
        String[] lines = result.split("\\n");
        StringBuilder htmlResult = new StringBuilder();
        boolean overflow = false;
        for (String line : lines) {
            htmlResult.append(line);
            htmlResult.append("\n<br/>");
        }
        if (overflow)
        {
            htmlResult.append("</div>\n");
            // the nojog class ensures that the jog doesn't pop up when the more
            // button is clicked
            htmlResult
                    .append("<span class=\"nojog\" style=\"color:#500050;text-decoration:underline;font-size:12px\" onclick=\"muse.reveal(this, false);\">More</span><br/>\n");
        }

        //Now do post-processing to add complex tags that depend on the text inside. title, link and cssclass
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlResult.toString());
        Elements elts = doc.select("[data-process]");

        for (int j = 0; j < elts.size(); j++) {
            Element elt = elts.get(j);
            Element par = elt.parent();
            //Do not touch nested entities
            if(par!=null && par.attr("data-process")==null)
            //(preHighlightTag.contains(par.tagName())||preHyperlinkTag.contains(par.tagName())))
                continue;
            String entity = elt.text();
            int span_j = j;

            String link = "browse?adv-search=1&termBody=on&termSubject=on&termAttachments=on&termOriginalBody=on&term=\"" + Util.escapeHTML(entity) + "\"";
            //note &quot here because the quotes have to survive
            //through the html page and reflect back in the URL
            link += "&initDocId=" + docId; // may need to URI escape docId?

            String title = "";
            try {
                String cssclass = "";
                EmailRenderer.Entity info = entitiesWithId.get(entity);
                if (info != null) {
                    if (info.ids != null) {
                        title += "<div id=\"fast_" + info.ids + "\"></div>";
                        title += "<script>getFastData(\"" + info.ids + "\");</script>";
                        cssclass = "resolved";
                    }
                    else {
                        //the last three are the OpenNLPs'
                        //could have defined overlapping sub-classes, which would have reduced code repetitions in css file; but this way more flexibility
                        String[] types = new String[] { "cp", "cl", "co", "person", "org", "place", "acr"};
                        String[] cssclasses = new String[] { "custom-people", "custom-loc", "custom-org", "opennlp-person", "opennlp-org", "opennlp-place", "acronym" };
                        outer:
                        for (String et : info.types) {
                            for (int t = 0; t < types.length; t++) {
                                String type = types[t];
                                if (type.equals(et)) {
                                    if (t < 3) {
                                        cssclass += cssclasses[t] + " ";
                                        //consider no other class
                                        continue outer;
                                    }
                                    else {
                                        cssclass += cssclasses[t] + " ";
                                    }
                                }
                            }
                        }
                    }
                } else {
                    cssclass += " unresolved";
                }

                // enables completion (expansion) of words while browsing of messages.
                if (entity != null) {
                    //enable for only few types
                    if (cssclass.contains("custom-people") || cssclass.contains("acronym") || cssclass.contains("custom-org") || cssclass.contains("custom-loc")) {
                        //TODO: remove regexs
                        entity = entity.replaceAll("(^\\s+|\\s+$)", "");
                        if (!entity.contains(" ")) {
                            //String rnd = rand.nextInt() + "";
                            //<img src="images/spinner.gif" style="height:15px"/>
                            //<script>expand("" + entity + "\",\"" + StringEscapeUtils.escapeJava(docId) + "\",\"" + rnd + "");</script>
//                            if(info.expandsTo!=null)
//                                title += "<div class=\"resolutions\" id=\"expand_" + rnd + "\"><a href='browse?term=\""+info.expandsTo+"\"'>"+info.expandsTo+"</a></div>";
                            cssclass += " expand";
                        }
                    }
                }

                for (int k = j; k <= span_j; k++) {
                    elt = elts.get(k);
                    //don't annotate nested tags-- double check if the parent tag is highlight related tag or entity related annotation
                    if(elt.parent().tag().getName().toLowerCase().equals("span") && elt.parent().classNames().toString().contains("custom")) {
                        continue;
                    }

                    String cc = elt.attr("class");
                    elt.attr("class", cc + " " + cssclass);
                    elt.attr("title", title);
                    elt.attr("onclick", "window.location='" + link + "'");
                    //A tag may have nested tags in it and is involved to get the text in it.
                    elt.attr("data-text", entity);
                    elt.attr("data-docId", StringEscapeUtils.escapeHtml(docId));
                }
            } catch (Exception e) {
                Util.print_exception("Some unknown error while highlighting", e, log);
            }
        }
        //The output Jsoup .html() will dump each tag in separate line
        String html = doc.html();

        if (showDebugInfo) {
            String debug_html = html + "<br>";
            debug_html += "<div class='debug' style='display:none'>";
            debug_html += "docId: "+docId;
            debug_html += "<br>-------------------------------------------------<br>";
            for (String str : entitiesWithId.keySet())
                debug_html += str + ":" + entitiesWithId.get(str).types + ";;; ";
            debug_html += "<br>-------------------------------------------------<br>";
            String[] opennlp = new String[] { "person", "place", "org" };
            String[] custom = new String[] { "cp", "cl", "co" };
            for (int j = 0; j < opennlp.length; j++) {
                String t1 = opennlp[j];
                String t2 = custom[j];
                Set<String> e1 = new HashSet<>();
                Set<String> e2 = new HashSet<>();
                for (String str : entitiesWithId.keySet()) {
                    Set<String> types = entitiesWithId.get(str).types;
                    if (types.contains(t1) && !types.contains(t2))
                        e1.add(entitiesWithId.get(str).name);
                    else if (types.contains(t2) && !types.contains(t1))
                        e2.add(entitiesWithId.get(str).name);
                }
                debug_html += opennlp[j] + " entities recognised by only opennlp: " + e1;
                debug_html += "<br>";
                debug_html += opennlp[j] + " entities recognised by only custom: " + e2;
                debug_html += "<br><br>";
            }
            debug_html += "-------------------------------------------------<br>";
            lines = contents.split("\\n");
            for (String line : lines)
                debug_html += line + "<br>";
            debug_html += "</div>";
            debug_html += "<button onclick='$(\".debug\").style(\"display\",\"block\");'>Show Debug Info</button>";
            return debug_html;
        }

        return html;
    }

    public static void main(String args[])
    {
        try {
            String text = "On Tue, Jun 24, 2014 at 11:56 AM, Aparna Vedant's UCB <aparna.vedant@XXX.edu.in> wrote: Rachelle K. Learner W.S. Merwin\nVery elated to see you yesterday. Can wee meet over a cup of coffee?\nI am hoping to hear back from you. BTW I also invited Tanmai Gopal to join.\n---Aparna\nUniversity of Florida" +
                    "-- ....., ... .... ........ .. . ...... ... Rep. Bill Andrews ... ... .... ..... ......  Mandate Year- Round Schools.\n" +
                    ".. ... ........ ... ...... .. ....: SQL Server Agent.CORP1.HealthFirst@......-.........; Jeb Bush. U.S. .... . ........... " +
                    ".. Advanced Registered Nurse Practitioners.\n" +
                    "Colonel Price..............." +
                    ".....Standard & Poors Publication...... Mandate Year- Round Schools\n" +
                    "U.S\n" +
                    ". .... . .......... ...... ... ... ...! .... ...., ...... ...-."
                    ;
            //List<String> arr = Arrays.asList( "\"Keep it to yourself\"","\"University of Florida\"","\"Aparna Vedant\"", "\"tanmai gopal\"","\"W.S. Merwin\"","Rachelle K. Learner", "elate|happy|invite", "hope","met", "/guth.*/", "/[0-9\\-]*[0-9]{3}[- ][0-9]{2}[- ][0-9]{4}[0-9\\-]*/ ", "you", "yours" );
            ModeConfig.mode = ModeConfig.Mode.DISCOVERY;
            String[] hts = new String[]{"\"Aparna Vedant\"","\"Rep. Bill Andrews\"","Mandate Year- Round Schools",
                    "SQL Server Agent.CORP1.HealthFirst","U.S.","Advanced Registered Nurse Practitioners","Bill","Standard & Poors Publication"};
            String preHighlightTag = "<B>", postHighlightTag = "</B>";
            //String str = highlightBatch(text, hts, preHighlightTag, postHighlightTag);
            Set<String> htSet = new LinkedHashSet<>();
            Collections.addAll(htSet, hts);
            String[] testDocIds = new String[]{"/Users/vihari/epadd-data/Bush 01 January 2003/Top of Outlook data file.mbox-706",
                "/Users/vihari/epadd-data/Bush 01 January 2003/Top of Outlook data file.mbox-710"};
                    //"/Users/vihari/epadd-data/Bush 01 January 2003/Top of Outlook data file.mbox-38",
                    //"/Users/vihari/epadd-data/Bush 01 January 2003/Top of Outlook data file.mbox-110"};
            String[][] highlightTerms = new String[][]{new String[]{"Hillsborough County"},new String[]{"Hillsborough County"}};
            String fldr = System.getProperty("user.home")+ File.separator+"epadd-processing"+File.separator+"ePADD archive of ";
            Archive archive = SimpleSessions.readArchiveIfPresent(fldr);
            System.out.println("Archive folder: "+fldr);
            String tmpDir = System.getProperty("java.io.tmpdir");
            int fi = 0;
            System.out.println("Content: " + archive.getLuceneDoc(testDocIds[0]));
            for(String td: testDocIds) {
                EmailDocument ed = archive.docForId(td);
                String content = archive.getContents(ed,false);
                Map<String, EmailRenderer.Entity> ewid = new LinkedHashMap<>();
                Util.tokenize(archive.getLuceneDoc(ed.getUniqueId()).get("names"), Indexer.NAMES_FIELD_DELIMITER).forEach(t -> {
                    if (t == null) {
                        log.warn("Found null content while parsing entity spans!!!");
                    }
                    if (t.equals(""))
                        return;
                    String[] fields = t.split(";");
                    if (fields.length != 7 && fields.length != 5) {
                        log.warn("Unexpected number of fields in content: " + text);
                        return;
                    }
                    Set<String> types = new HashSet<>();
                    short type = Short.parseShort(fields[3]);
                    //for (short ct : SequenceModel.mappings.keySet())
                    //    if (Arrays.asList(SequenceModel.mappings.get(ct)).contains(type)) {
                          //  types.add(ct == FeatureUtils.PERSON ? "cp" : ((ct == FeatureUtils.PLACE) ? "cl" : "co"));
                      //      break;
                        //}
                    short ct = NEType.getCoarseType(type).getCode();
                    types.add(ct == NEType.Type.PERSON.getCode() ? "cp" : ((ct == NEType.Type.PLACE.getCode()) ? "cl" : "co"));
                    ewid.put(fields[0], new EmailRenderer.Entity(fields[0], null, types));
                });
                Set<String> termsToHighlight = new HashSet<>();
                for(String[] ht: highlightTerms)
                        termsToHighlight.addAll(Arrays.asList(ht));
//                archive.getEntitiesInDoc(ed, edu.stanford.muse.ner.NER.EPER).forEach(e->ewid.put(e,new Archive.Entity(e,null,Arrays.asList("cp").stream().collect(Collectors.toSet()))));
//                archive.getEntitiesInDoc(ed, edu.stanford.muse.ner.NER.ELOC).forEach(e->ewid.put(e,new Archive.Entity(e,null,Arrays.asList("cl").stream().collect(Collectors.toSet()))));
//                archive.getEntitiesInDoc(ed, edu.stanford.muse.ner.NER.EORG).forEach(e->ewid.put(e,new Archive.Entity(e, null, Arrays.asList("co").stream().collect(Collectors.toSet()))));
                String htmlcontent = getHTMLAnnotatedDocumentContents("<body>"+content+"</body>",ed.date,ed.getUniqueId(),null /* regex to highlight */,termsToHighlight,ewid,null,true);
                System.out.println("Done highlighting.");
                htmlcontent += "<br>------<br>"+content.replaceAll("\n","<br>\n");
                htmlcontent = "<link href=\"epadd.css\" rel=\"stylesheet\" type=\"text/css\"/>\n"+htmlcontent;
                FileWriter fw = new FileWriter(new File(tmpDir+(fi++)+".html"));
                fw.write(htmlcontent);
                System.out.println("wrote to " + tmpDir + (fi - 1) + ".html");
                fw.close();
            }
            //String str = getHTMLAnnotatedDocumentContents(text, new Date(), "", false, htSet, null, null, false);
            //str = Highlighter.highlightBatch(str, new String[] {"Aparna"}, "<B >", "</B>");
            //System.out.println("Highlighted content: " + str);
            //getHTMLAnnotatedDocumentContents("", new Date(), "", false, new LinkedHashSet<>(Arrays.asList("Robert Creeley")), null, new LinkedHashSet<>(Arrays.asList("Charles", "Susan Howe", "Betty", "Charles Bernstein", "Carl Dennis", "Joseph Conte", "Bob Creeley", "Residence", "Uday", "LWOP", "U Penn", "Joseph", "Betty Capaldi", "Capen Chair")), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}