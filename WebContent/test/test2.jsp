<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.util.zip.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="javax.mail.Address"%>
<%@page language="java" import="javax.mail.internet.InternetAddress"%>
<%@page language="java" import="java.util.regex.Matcher"%>
<%@page language="java" import="java.util.regex.Pattern"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.groups.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.Pair"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>

<%@include file="getArchive.jspf" %>
<html>
<head>
<title>Test</title>
</head>
<body>
<%
	try{
		Indexer indexer = archive.indexer;
		Indexer.QueryType qt = Indexer.QueryType.FULL;
		List<Document> docs = archive.getAllDocs();	
		
		String home = System.getProperty("user.home");
		String sep = File.separator;	
		File f = new File(home+sep+"temp");
		if(!f.exists())
			f.mkdir();
		FileWriter fw = new FileWriter(new File(home+sep+"temp"+sep+"mails.txt"));
		int i=0;
		String mails = "",contents = "";
		AddressBook ab = archive.addressBook;
		Pattern pat = Pattern.compile("(.*?) wrote:");
		int sz = docs.size();
		for(Document doc: docs){
			EmailDocument ed = (EmailDocument)doc;
			int sent_or_received = ed.sentOrReceived(ab);
			if((sent_or_received&EmailDocument.SENT_MASK)!=0){
				InternetAddress[] tos = (InternetAddress[])ed.to;
				InternetAddress[] froms = (InternetAddress[])ed.from;
				String content = archive.getContents(doc, false);
				content = content.replaceAll("'ve", " have").replaceAll("'ld", " would").replaceAll("'ll"," will").replaceAll("'d"," would");
				
				int index = content.indexOf("----- Original Message -----");
				if(index>=0)
					content = content.substring(0, index);
				
				String toPeople="",fromPeople="";	
				
				String[] lines = content.split("\\n+");
				//tocontent is what compiled by toaddress and fromcontent likewise
				String toContent="",fromContent = "";
				for(String line: lines){
					Matcher mat = pat.matcher(line);
					if(mat.matches()){
						System.err.println("Matches: "+mat.group());
						toPeople = mat.group(1);
					}
					if(line.startsWith("> >"))
						;
					else if(line.startsWith(">"))
						toContent += line.replaceAll("> ","") +" ";
					else
						fromContent += line+ " ";
				}
				int p=0;
				if(toPeople==null||toPeople.equals("")){
					if(tos!=null)
						for(InternetAddress to: tos){
							if(to.getPersonal()!=null)
								toPeople += to.getPersonal();
							else if(to.getAddress()!=null)
								toPeople += to.getAddress();
							if(p<tos.length-1)
								toPeople+=", ";
							p++;
						}
				}
				p=0;
				if(froms!=null)
					for(InternetAddress from: froms){
						fromPeople += from.getPersonal();
						if(p<froms.length-1)
							fromPeople+=", ";
						p++;
					}
		
				//handle 've 'ld and other pronoun forms.
				if(!toPeople.equals("")&&!toPeople.contains("null")){
					toContent = toContent.replaceAll("I am", toPeople+" is");
					toContent = toContent.replaceAll("I ", toPeople.toString()+" ");
					if(!fromPeople.contains("null"))
						toContent = toContent.replaceAll("you ", fromPeople.toString()+" ");
				}
				if(!fromPeople.equals("")&&!fromPeople.contains("null")){
					fromContent = fromContent.replaceAll("I am", fromPeople+" is");
					fromContent = fromContent.replaceAll("I ", fromPeople.toString()+" ");
					if(!toPeople.contains("null"))
						fromContent = fromContent.replaceAll("you ", toPeople.toString()+" ");
				}
				//TODO: also add a mapping from email address to person mentioned in the first line.
				/*mails += "TO: "+toPeople+"\n";
				mails += "FROM: "+fromPeople+"\n";
				mails += "To message.\n"+toContent+"\n";
				mails += "From message.\n"+fromContent+"\n";*/
				mails += toContent+"\n"+fromContent+"\n";
				//mails += "\n\n:::::::::::::::::::::::::\n\n";
				contents += content+"\n\n:::::::::::::::::::::::::\n\n";
				//out.println("<div id='some'>Successfully written #"+i+" mails to file.</div>");
				i++;
			}	
			if(i%1000==0){
					System.err.println("("+i+"/"+sz+")");
					fw.write(mails);
			}
			i++;
		}
		fw.write(mails);
		fw.close();
		fw = new FileWriter(new File(home+sep+"temp"+sep+"mailContents.txt"));
		fw.write(contents);
		fw.close();
		out.println("<div id='some'>Successfully written #"+i+" mails to file.</div>");
	}catch(Exception e){
		e.printStackTrace();
		out.println(e);	
	}
%>
</body>
</html>
