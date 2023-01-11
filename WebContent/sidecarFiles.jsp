<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="java.util.Set" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.util.Map" %>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@page language="java" %>
<%@ page import="java.io.File" %>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <link href="css/selectpicker.css" rel="stylesheet" type="text/css" media="screen" />
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/main.css">
    <link rel="stylesheet" href="css/bootstrap-dialog.css">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>

    <script src="js/jquery.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <!--script src="jqueryFileTree/jqueryFileTree.js"></script-->
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>

    <script src="js/jquery.dataTables.min.js"></script>
    
    <!--script src="js/filepicker.js"></script-->
    <!--script src="js/selectpicker.js"></script-->
    <script src="js/modernizr.min.js"></script>
    <script src="js/sidebar.js"></script>

    <script src="js/muse.js"></script>
    <script src="js/epadd.js"></script>
    <script src="js/bootstrap-dialog.js"></script>

    <style>
	#advanced_options button {width:250px;}
        #advanced_options input {width:250px;}
    </style>
</head>
<!--body style="background-color:white;"-->
<body style="color:gray;">

<%@include file="header.jspf"%>
<title> Sidecar File Management </title>

<%writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "sidecar.management-title"));%>


<script>
//    epadd.nav_mark_active('Export');
</script>

<div id="spinner-div" class="pt-5">
  <div class="spinner-border text-primary" role="status">
  </div>
</div>

<!--div class="nav-toggle1 sidebar-icon">
    <img src="images/sidebar.png" alt="sidebar">
</div-->
<nav class="menu1" role="navigation">
    <h2><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "export.help.head")%></b></h2>
    <!--close button-->
    <a class="nav-toggle1 show-nav1" href="#">
        <img src="images/close.png" class="close" alt="close">
    </a>

    <div class="search-tips" style="display:block">

        <% if (ModeConfig.isAppraisalMode()) { %>
            <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "export.help.appraisal")%>

        <% } else if (ModeConfig.isProcessingMode()) { %>
            <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "export.help.processing")%>

        <% } %>
    </div>
</nav>
<p>

<div id="all_fields" style="margin:auto; width:1100px; padding: 10px">
        <section>
            <div class="panel">
                <div class="panel-heading">Sidecar Files</div>
            
        <table id="example" class="display" style="width:100%">
        <thead>
            <tr>
            <th>File Name</th>
            <th>Last Modified</th>
            <th></th>
            </tr>
        </thead>
        <tfoot>
        </tfoot>
        </table>                

            </div>
            
            
            
        </section>
</div> <!--  all fields -->

<p>

    <script type="text/javascript">
        $(document).ready(function() {
            var table =  $('#example').DataTable({
                    processing: true,
                    ajax: { 
                        url: 'ajax/sidecarFolder',
                        type: 'POST',
                        data: {archive: archiveID}
                    },
                    language: {
//                        emptyTable: "No Uploaded Sidecar File"
                        emptyTable: "<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "sidecar.management-no-file")%>"
                    },
                    searching: false,
                    columnDefs: [
                        {
                            targets: 1,
                            orderable: true,
                            searchable: false
                        },    
                        {
                            targets: -1,
                            data: null,
                            defaultContent: '<button title="delete"><i class="fa fa-trash"></i></button>',
//                          defaultContent: '<i class="fa fa-trash"></i>',
                            className: "text-right",
                            orderable: false,
                            searchable: false
                        }
                    ]
            });
            $('#example tbody').on('click', 'button', function () {
                    var data =  table.row($(this).parents('tr')).data();
//                    alert(data[0] + " deleted");
                    BootstrapDialog.show ({
                        title: 'Delete Sidecar File',
                        message: 'Confirm to delete file, ' + data[0] +'?',
                        type: BootstrapDialog.TYPE_DANGER,
                        closable: false,
                        data: {
//                             'file': data[0]
                        },
                        buttons: [
                        {
                            id: 'btn-yes',   
                            label: 'Yes',
                            cssClass: 'btn-default', 
                            autospin: false,
                            action: function(dialogRef){    
                                $.ajax({
                                    type: "POST",
                                    url: "ajax/sidecarFileDelete",
                                    data: { file: data[0], archive: archiveID },
                                    success: function(response) {
                                        if (response['result'] === "ok") {
                                            BootstrapDialog.alert(
                                            {
                                                message:response['reason'], 
                                                type: BootstrapDialog.TYPE_SUCCESS,
                                                action: function(dialog) {
                                                  dialog.close(); 
                                                },
                                                callback: function(result) {
                                                    table.ajax.reload( null, false );
                                                }    
                                            }   
                                            );
                                            dialogRef.close();
                                        } else {
                                            BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_WARNING});
                                        }
                                    },
                                    error: function() {
                                        BootstrapDialog.show({message:"Error in deleting file" , type: BootstrapDialog.TYPE_WARNING});
                                    }
                                });
                            }   
                        },
                        {
                            id: 'btn-cancel',   
                           label: 'No',
                           cssClass: 'btn-default', 
                           autospin: false,
                           action: function(dialogRef){    
                                dialogRef.close();
                            }    
                        }
                    ]
                });                    
            });         // End on-click       
                
        });
        
    </script>

</body>
</html>
