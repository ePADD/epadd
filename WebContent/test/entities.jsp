<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.ner.NER" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="com.google.gson.reflect.TypeToken" %>
<%@ page import="java.lang.reflect.Type" %>
<%@ page import="edu.stanford.muse.ner.Entity" %>
<!--
General API to request for entities
Just navigate to test/entities.jsp to use it
Options:
1. Cutoff score
2. Semantic types to be included [only one]
3. Semantic types to be excluded [only one]
4. Maximum number of email documents from which entities would be collected
-->
<%
    class Some{
        List<Short> parse(String[] excS) {
            List<Short> exc = null;
            if (excS != null) {
                exc = new ArrayList<>();
                int ei = 0;
                for (String es : excS)
                    exc.add(Short.parseShort(es));
            }
            return exc;
        }
    }

    String cutoff = request.getParameter("cutoff");
    if(cutoff==null||cutoff.equals("")){
        String options = "";
        options += "<option value='none'>NONE</option>";
        for(Short t: FeatureDictionary.allTypes)
            options += "<option value='"+t+"'>"+FeatureDictionary.desc.get(t)+"</option>";
%>
<html>
<head><title>Entities</title></head>
<body>
<style>
    body{
        text-align: center;
        padding-top:30px;
        line-height: 200%;
    }
</style>
<%
    if(cutoff!=null && cutoff.equals(""))
        out.println("<span style='color:red'>Please specify cutoff for quality score</span><br>");
    out.println("Cutoff&nbsp&nbsp<span style='color:red'>*</span> &nbsp<input name='cutoff'></input><br>");
    out.println("Include types <select name='include'>" + options + "</select><br>");
    out.println("Exclude types <select name='exclude'>"+options+"</select><br>");
    out.println("Maximum Email Documents <input size='5' name='maxdoc' placeholder='1000'></input>");
%>
<br>
<button onclick="trigger()" style='height:30px;font-size:20px'>Fetch</button>
<script>
    function trigger(){
        names = ["cutoff","include","exclude","maxdoc"];
        params="";
        for(var i=0;i<names.length;i++){
            val = document.getElementsByName(names[i])[0].value;
            if(names[i]=="maxdoc" && val=="")
                val=1000;
            if(val!=="none") {
                params += names[i] + "=" + val;
                if (i < names.length - 1)
                    params += "&";
            }
        }
        var href = window.location.href;
        window.location.href=href.substr(0,href.indexOf("?"))+"?"+params;
    }
</script>
</body>
</html>
<%
    }
    else{
        String excS = request.getParameter("exclude");
        String incS = request.getParameter("include");
        String mds = request.getParameter("maxdoc");
        int md = 10;
        if(mds!=null)
            md = Integer.parseInt(mds);

        List<Short> exc = new ArrayList<>(), inc = new ArrayList<>();
        if(incS==null) {
            List<Short> tmp = new ArrayList<>();
            for(short t: FeatureDictionary.allTypes)
                if(t!=FeatureDictionary.OTHER)
                    tmp.add(t);
            inc = tmp;
        }
        else
            inc.add(Short.parseShort(incS));
        if(excS!=null){
            exc.add(Short.parseShort(excS));
        }
        double theta = Double.parseDouble(cutoff);
        JSPHelper.log.info("Params: " +
                "Threshold: " + theta +
                "\nexclude: " + exc +
                "\ninclude: " + inc +
                "\nmaxdoc: " + mds);
        Archive archive = JSPHelper.getArchive(session);
        Map<String,Entity> entities = new LinkedHashMap();
        int di=0;
        for(Document doc: archive.getAllDocs()){
            if(di++>md)
                break;
            //System.err.println("120");
            Map<Short,Map<String,Double>> es = NER.getEntities(archive.getDoc(doc),true);
            //System.err.println("122");
            for(Short type: inc)
                if(!exc.contains(type)){
                    if(es != null && es.containsKey(type))
                        for(String e: es.get(type).keySet()) {
                            double s = es.get(type).get(e);
                            if(s<theta)
                                continue;
                            if (!entities.containsKey(e))
                                entities.put(e, new Entity(e, s));
                            else
                                entities.get(e).freq++;
                        }
                }
        }
        Map<Entity, Double> vals = new LinkedHashMap<>();
        for(Entity e: entities.values()) {
            vals.put(e, e.score);
            //System.err.println("Putting: "+e+", "+e.score);
        }
        List<Pair<Entity,Double>> lst = Util.sortMapByValue(vals);
        List<Entity> lst1 = new ArrayList<>();
        for(Pair<Entity,Double> p: lst)
            lst1.add(p.first);
        Gson gson = new Gson();
//        for(Pair<Entity,Double> p: lst)
//            out.println(p.getFirst()+"<br>");
        Type listType = new TypeToken<List<Entity>>() {
        }.getType();
        out.println(gson.toJson(lst1, listType));
    }
%>