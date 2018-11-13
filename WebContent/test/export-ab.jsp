<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page import="java.io.*"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page import="edu.stanford.muse.AddressBookManager.Contact" %>
<%@ page import="java.util.Collection" %>
<%@page contentType="text/html; charset=UTF-8"%>
<%@include file="../getArchive.jspf" %>
<%
	AddressBook addressBook = archive.addressBook;
	Collection<Contact> contacts = addressBook.allContacts();
	int cid = 1;
	PrintWriter out1 = new PrintWriter(new FileOutputStream("/tmp/ab.csv"));
	for (Contact c: contacts) {
	    Collection<String> names = c.getNames(), emails = c.getEmails();
	    if (names != null)
	        for (String name: names)
			    out1.println (cid + ",\"" + name + "\"");
		if (emails != null)
			for (String email: emails)
				out1.println (cid + ",\"" + email + "\"");
		cid++;
	}
	out1.close();

%>
