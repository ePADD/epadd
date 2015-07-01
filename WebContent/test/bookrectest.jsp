<%@page import="opennlp.tools.cmdline.parser.*"%>
<%@page import="opennlp.tools.parser.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<%
Archive archive = JSPHelper.getArchive(session);
Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
Indexer indexer = archive.indexer;
SentenceDetectorME	sentenceDetector;
InputStream SentStream = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-sent.bin");
SentenceModel model = null;
try {
	model = new SentenceModel(SentStream);
} catch (Exception e) {
	e.printStackTrace();
	System.err.println("Exception in init'ing sentence model");
}
sentenceDetector = new SentenceDetectorME(model);

ParserModel pmodel = new ParserModelLoader().load(new File("/Users/viharipiratla/repos/opennlp/models/en-parser-chunking.bin"));
opennlp.tools.parser.Parser parser = ParserFactory.create(pmodel, AbstractBottomUpParser.defaultBeamSize, AbstractBottomUpParser.defaultAdvancePercentage);

int i=0;
for(EmailDocument ed: docs){
	String contents = indexer.getContents(ed,false);
	String[] lines = contents.split("\\n+");
	for(String line: lines)
		if(line.contains("book")||line.contains("books")){
			String[] sentences = sentenceDetector.sentDetect(line);	
			for(String sent: sentences)
				if(sent.contains("book")||sent.contains("books")){
					System.err.println(i+", sent len: "+sent.length());
					Parse[] parses = ParserTool.parseLine(line, parser, 1);

					for (int pi = 0, pn = parses.length; pi < pn; pi++) {
					
						//parses[pi].show();
						Set<String> govs = new HashSet<String>();
						String[] vargovs = new String[] { "book", "books" };
						for (String vargov : vargovs)
							govs.add(vargov);
						//parses[pi].showConnectedNodesTo(govs);
						List<Parse> nodes = parses[pi].getNodesWithHeadOrGovernedBy(govs);
						out.println(sent);	
						String books = "";
						for(Parse p: nodes)
							books += p.getCoveredText()+" ::: ";
						out.println("<br>Recognised books: "+books+"<br>");
					}
				}
		}
	if(++i>100)
		break;	
}
%>