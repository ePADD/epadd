<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
  <title>Export Cart</title>

  <link rel="icon" type="image/png" href="images/epadd-favicon.png">

  <script src="js/jquery.js"></script>

  <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
  <!-- Optional theme -->
  <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

  <jsp:include page="css/css.jsp"/>
  <script src="js/muse.js"></script>
  <script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>

<div class="panel" style="padding-left:10%">
<%
  List<EmailDocument> selectedDocs = new ArrayList<EmailDocument>();
  for (Document d: archive.getAllDocs()) {
      EmailDocument ed = (EmailDocument) d;
      if (ed.addedToCart)
        selectedDocs.add(ed);
  }

  JSPHelper.log.info ("original browse set has " + selectedDocs.size() + " docs");

  // either we do tags (+ or -) from selectedTags
  // or we do all docs from allDocs
  String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
  String attachmentsStoreDir = cacheDir + File.separator + "blobs" + File.separator;
  BlobStore bs = null;
  try {
    bs = new BlobStore(attachmentsStoreDir);
    JSPHelper.log.info ("Good, found attachments store in dir " + attachmentsStoreDir);
  } catch (IOException ioe) {
    JSPHelper.log.error("Unable to initialize attachments store in directory: " + attachmentsStoreDir + " :" + ioe);
  }

  String rootDir = JSPHelper.getRootDir(request);
  new File(rootDir).mkdirs();
  String userKey = JSPHelper.getUserKey(session);
  String name = request.getParameter("name");
  if (Util.nullOrEmpty(name))
    name = String.format("%08x", EmailUtils.rng.nextInt());
  String filename = name + ".mbox.txt";
  String path = rootDir + File.separator + filename;

  PrintWriter pw = new PrintWriter (path);

  String noAttach = request.getParameter("noattach");
  boolean noAttachments = "on".equals(noAttach);
  boolean stripQuoted = "on".equals(request.getParameter("stripQuoted"));
  for (Document ed: selectedDocs)
    EmailUtils.printToMbox(archive, (EmailDocument) ed, pw, noAttachments ? null: bs, stripQuoted);
  pw.close();
%>

<br/>

A file with <%=Util.pluralize(selectedDocs.size(), "message")%> is ready. <br/>
To save it, right click <a href="<%=userKey%>/<%=filename%>">this link</a> and select "Save Link As...".
<% String message;
  if (selectedDocs.size() > 1000)
    message = "";
  else if (selectedDocs.size() > 50)
    message = "(Left click to view the file. It can be very large!)<br/>";
  else
    message = "(Left click to view the file in the browser.)<br/>";
%>
<%=message%>

<p></p>
This file is in mbox format, and can be accessed with many email clients (e.g. <a href="http://www.mozillamessaging.com/">Thunderbird</a>.)
It can also be viewed with a text editor.<br/>
On Mac OS X, Linux, and other flavors of Unix, you can usually open a terminal window and type the command: <br/>
<i>mail -f &lt;saved file&gt;</i>
<br/>
</div>
<jsp:include page="footer.jsp"/>
</body>
</html>