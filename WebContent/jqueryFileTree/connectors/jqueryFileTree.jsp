<%@ page import="java.io.File,java.io.FilenameFilter,java.util.Arrays"%>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%
/**
  * jQuery File Tree JSP Connector
  * Version 1.0
  * Copyright 2008 Joshua Gould
  * 21 April 2008
*/
    String dir = request.getParameter("dir");
    if (dir == null || dir.length() == 0 || "/".equals(dir.trim())) {
		if (Util.nullOrEmpty(dir))
			dir = "/";

		// modified by SGH. if dir parameter is empty or /, just list all the roots.
		java.io.File[] rootFiles = java.io.File.listRoots();

		if (rootFiles.length  > 1) { // this happens on Windows
			JSPHelper.log.info ("Listing RootFiles = "  + String.join (",", Arrays.stream(rootFiles).map(f -> f.toString()).collect (Collectors.toList())));
			out.print("<ul class=\"jqueryFileTree\" style=\"display: none;\">");
			for (java.io.File f : rootFiles) {
				String rootFile = f.toString();
				out.print("<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + rootFile + "/\">"
						+ f.toString() + "</a></li>");
			}
			out.print("</ul>");
			return;
		}
	}
	
	if (dir.length() > 0) {
		if (dir.charAt(dir.length() - 1) == '\\') {
			dir = dir.substring(0, dir.length() - 1) + "/";
		} else if (dir.charAt(dir.length() - 1) != '/') {
			dir += "/";
		}
	}

	dir = java.net.URLDecoder.decode(dir, "UTF-8");	
	
	File f = new File(dir);
    if (f.exists() && f.canRead()) {
		String[] files = f.list(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
				return name.charAt(0) != '.';
		    }
		});
		Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
		out.print("<ul class=\"jqueryFileTree\" style=\"display: none;\">");
		// All dirs
		for (String file : files) {
		    if (new File(dir, file).isDirectory()) {
				out.print("<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + dir + file + "/\">"
					+ file + "</a></li>");
		    }
		}
		// All files
		for (String file : files) {
		    if (!new File(dir, file).isDirectory()) {
				int dotIndex = file.lastIndexOf('.');
				String ext = dotIndex > 0 ? file.substring(dotIndex + 1) : "";
				out.print("<li class=\"file ext_" + ext + "\"><a href=\"#\" rel=\"" + dir + file + "\">"
					+ file + "</a></li>");
		    	}
		}
		out.print("</ul>");
    }
%>