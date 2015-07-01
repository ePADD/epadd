<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--	
    Use this page to train and recognise fine grainied entity types, 
    this is part of experimental module and not properly tested 
    across many archives
-->

<link href="css/epadd.css" rel="stylesheet" type="text/css"/>
<script src="js/jquery.js"></script>
<script src="js/muse.js"></script>
<script src="js/epadd.js"></script>
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>
<script>
params = "";
page = "ajax/trainfinetypes.jsp";
epadd.log(page+params);

//supplying the ready function to make it not redirect the other page and give us the handle of the response data.  
fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), params,null,"test/nerannotator.jsp?type=finetypes&color=red");
</script>
