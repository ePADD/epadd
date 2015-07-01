<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Random" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.epadd.util.NLPUtils" %>
<%@ page import="edu.stanford.muse.ie.ie.NER" %>
<%@ page import="java.io.*" %>
<%--
  Created by IntelliJ IDEA.
  User: viharipiratla
  Date: 20/05/15
  Time: 12:20 PM
  To change this template use File | Settings | File Templates.
  Use this file to set NER benchmark
  Collects n docs randomly such that the number of sentences in the originial text of these messages is ~15K
--%>

<%@include file="../getArchive.jspf" %>

<html>
<body>
<%--<jsp:include page="../header.jspf"/>--%>
<%
  boolean random = request.getParameter("random")!=null;
  String fldr = System.getProperty("user.home") + File.separator + "epadd-data" + File.separator + "ner-benchmarks";
  String fldr2 = "benchmark-" + archive.addressBook.getBestNameForSelf();

  //Archive archive = JSPHelper.getArchive(session);
  Set<String> docIds = new HashSet<String>();

  if(!random){
    System.out.println("Trying to read from the dumped files...");
    out.println("Trying to read from the dumped files...<br>");
    try {
      BufferedReader br = new BufferedReader(new FileReader(new File(fldr + File.separator + fldr2 + File.separator + "docIds.txt")));
      String line = null;
      while ((line = br.readLine()) != null) {
        docIds.add(line.trim());
      }
      br.close();
    }catch(Exception e){
      e.printStackTrace();
      out.println("Looks like you dont have random docs set try randomising!<br>");
      return;
    }
  }
  else {
    List<Document> docs = archive.getAllDocs();
    Random r = new Random();
    Set<Document> rdocs = new HashSet<Document>();
    int numSent = 0, THRESH = 1500;

    while (true) {
      int ri = r.nextInt(docs.size());


      Document rdoc = docs.get(ri);
      if (rdocs.contains(rdoc))
        continue;
      //unfortunately we cannot just see the original content as the names that are added to the index contains names extracted from teh entire message
      String contents = archive.getContents(rdoc, false);
      String[] sents = NLPUtils.SentenceTokenizer(contents);
      if (sents != null)
        numSent += sents.length;

      rdocs.add(rdoc);

      if ((numSent > THRESH) || (rdocs.size() == docs.size()))
        break;
      System.err.println("Collected " + rdocs.size() + "  and " + numSent + " sentences");
    }

    System.out.println("Collected " + rdocs.size() + "  and " + numSent + " sentences");

    if (!new File(fldr).exists())
      new File(fldr).mkdir();
    File f = new File(fldr + File.separator + fldr2);
    if (!f.exists())
      f.mkdir();

    String[] types = new String[]{NER.EPER, NER.ELOC, NER.EORG};
    //String s1 = fldr + File.separator + fldr2 + File.separator + types[0], s2 = fldr + File.separator + fldr2 + File.separator + types[1], s3 = fldr + File.separator + fldr2 + File.separator + types[2];
//    String[] temp = new String[]{s1, s2, s3};
//    for (String t : temp)
//      if (!new File(t).exists())
//        new File(t).mkdir();

    int id = 0;
    //this mapping is defined so that there would be least editing in the benchmark files
    Map<String,String> mapping = new LinkedHashMap<String,String>();
    mapping.put(NER.EPER,"p");
    mapping.put(NER.ELOC,"l");
    mapping.put(NER.EORG,"o");

    for (Document doc : rdocs) {
      id++;
      docIds.add(doc.getUniqueId());
      String uidfull = doc.getUniqueId();
      String[] fs = uidfull.split("(/|\\\\)");
      //throw exception if the length is <1 which is not normal
      String uid = fs[fs.length - 1];
      File eF = new File(fldr+File.separator+fldr2+File.separator+uid + ".txt"); //new File(s2 + File.separator + uid + ".txt"), new File(s3 + File.separator + uid + ".txt")};
      try{
        FileWriter fw = new FileWriter(eF);
        for (int ti = 0; ti < types.length; ti++) {
          List<String> entities = archive.indexer.getEntitiesInDoc(doc, types[ti]);
          Set<String>eset = new HashSet<String>();
          for(String e: entities)
            eset.add(e);
          for (String e : eset)
            fw.write(e + " ::: " + mapping.get(types[ti])+ "\n");
        }
        fw.close();

      } catch (Exception e) {
        System.err.println("Problem while writing entities!!");
        e.printStackTrace();
      }
    }


    id = 0;
    f = new File(fldr + File.separator + fldr2 + File.separator + "docIds.txt");
    FileWriter fw = new FileWriter(f);
    for (String docId : docIds) {
      id++;
      fw.write(docId+ "\n");
    }
    fw.close();
  }

  request.getSession().setAttribute("dIdLKey",docIds);
  response.sendRedirect("../browse?dIdLKey=dIdLKey&debug=1");
%>
<jsp:include page="../footer.jsp"/>
</body>
</html>