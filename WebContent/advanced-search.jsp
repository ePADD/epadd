<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<%--date time picker component used from http://eonasdan.github.io/bootstrap-datetimepicker/--%>
    <title>Advanced Search</title>
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width, initial-scale=1">
	<meta charset="utf-8">
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link rel="stylesheet" href="js/jquery-ui/jquery-ui.css">
	<link rel="stylesheet" href="js/jquery-ui/jquery-ui.theme.css">

		<jsp:include page="css/css.jsp"/>

	<link rel="stylesheet" href="css/main.css">

	<!-- main.css from Lollypop has reset some of the btn-cta styles, which we didn't want ! :-( -->
    <style>
        .btn-cta {
            color: #fff;
            background-color: #0075bb;
            text-shadow: none;
            border-radius: 3px;
            -moz-transition: 0.3s ease;
            transition: 0.3s ease;
            text-transform: uppercase;
            font-weight: bold;
            box-shadow: 2px 2px 2px rgba(0,0,0,0.15);
            height: 47px;
            padding-top: 10px;
            /*	width:120px; */
        }
        .btn-cta i{ opacity:0.5  }
        .btn-cta:hover, .btn-cta:focus {
            background-color: #0075bb;
            color: #fff;
            box-shadow:0px 0px 0px rgba(0,0,0,0.15);

            -moz-transition:0.3s ease;
            transition:0.3s ease;
        }
        .btn-cta:hover i{ opacity:1;}
    </style>
    <!--scripts-->

	<script  src="js/jquery-1.12.1.min.js"></script>

	<script src="js/jquery.autocomplete.js" type="text/javascript"></script>

	<script  src="js/jquery-migrate-1.2.1.min.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/jquery-ui/jquery-ui.js"></script>

	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>
	<script src="js/selectpicker.js"></script>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>

</head>
<%
	String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
%>
<body>
	<jsp:include page="header.jspf"/>
	<script>epadd.nav_mark_active('Search');</script>

	<!--Advanced Search-->
	<div class="advanced-search">
		<form id="adv-search-form" action="browse" method="post">
		<input type="hidden" name="adv-search" value="1"/>
			<%--hidden parameter passed as archiveID--%>
			<input type="hidden" name="archiveID" value="<%=archiveID%>"/>
			<!--container-->
		<div class="container">
			<!--row-->
			<div class="row">
				<div class="heading-wraper">
					<h1>Advanced Search</h1>
					<!--sidebar icon-->
					<div class="nav-toggle1 sidebar-icon">
						<img src="images/sidebar.png" alt="sidebar">
					</div>
					<!--/sidebar icon-->
				</div> <!--end of sidebar-wraper-->

				<!--sidebar content-->
				<nav class="menu1" role="navigation">
					<h2>Search Tips</h2>
					<!--close button-->
					<a class="nav-toggle1 show-nav1" href="#">
						<img src="images/close.png" class="close" alt="close">
					</a>

					<!--phrase-->
					<div class="search-tips">
						<img src="images/pharse.png" alt="">
						<p>
							Use quotation marks to search as a <strong>phrase.</strong> (Word-stemming will still be applied to words within the phrase).
						</p>
					</div>

					<!--requered-->
					<div class="search-tips">
						<img src="images/requered.png" alt="">
						<p>
							Use <strong>+</strong> (no space) before a word to make it <strong>required.</strong>
							<br/><br/>
							Adding a semicolon in any free text field will return results that contain any of the selected terms.
							<br/><br/>
							Selection across categories will return results that meet ALL of the selected options.
							<br/><br/>
							Selecting multiple checkboxes (where available) will search return results that meet any of the selected options.

						</p>
					</div>


					<!--Boolean-->
					<div class="search-tips">
						<img src="images/boolean.png" alt="">
						<p>
							Use <strong>OR, AND &amp; NOT</strong> (Uppercase) to create <strong>Boolean
						</strong>logic within a field. You can use parentheses in complex expressions.
						</p>
					</div>

					<!--Truncated-->
					<div class="search-tips">
						<img src="images/truncated.png" alt="">
						<p>
							Use <strong>+</strong> to search a <strong>truncated term.</strong> (Word-stemming is done automatically).
						</p>
					</div>

					<!--wildcard-->
					<div class="search-tips">
						<img src="images/wildcard.png" alt="">
						<p>
							Use <strong>?</strong> as a <strong>wildcard</strong> to replace a character in a word.
						</p>
					</div>

					<!--stemming-->
					<div class="search-tips">
						<img src="images/stemming.png" alt="">
						<p>
							<strong>Word-stemming</strong> automatically includes plural and singular forms, and common suffix and tense variations.
						</p>
					</div>

				</nav>
				<!--/sidebar-->

				<!--Text-->
				<div class="text search-wraper">

					<h4>Text</h4><br/>

					<div class="row">
						<!--message-->
						<div class="margin-btm  col-sm12">

							<!--input form-->
							<div class="form-group">
								<label for="term">Terms</label>
								<input name="term" id="term" type="text" class="form-control">
							</div>

							<!--checkboxes-->
							<div class="checkbox-wraper ">
								<fieldset>
									<legend class="sr-only">Message filters using checkbox
									</legend>

									<div class="checkbox-inline">
										<label>
											<input type="checkbox" name="termBody" checked>
											<span class="label-text">Search body</span>
										</label>
									</div>

									<div class="checkbox-inline">
										<label>
											<input type="checkbox" name="termSubject" checked>
											<span class="label-text">Search subject</span>
										</label>
									</div>

                                    <% if (!ModeConfig.isDiscoveryMode()) { %>
                                        <div class="checkbox-inline">
                                            <label>
                                                <input type="checkbox" name="termAttachments" checked>
                                                <span class="label-text">Search attachments</span>
                                            </label>
                                        </div>

                                        <div class="checkbox-inline">
                                            <label>
                                                <input type="checkbox" name="termOriginalBody" checked>
                                                <span class="label-text">Search original text</span>
                                            </label>
                                        </div>
                                    <% } %>

									<!--
									<div class="checkbox-inline">
										<label>
											<input type="checkbox" name="termRegex">
											<span class="label-text">Regular Expression</span>
										</label>
									</div>
									-->
								</fieldset>
							</div>
						</div>
						<!--/message-->

					</div>
                    <br/>
					<div class="row">
						<div class="form-group col-sm-12">
							<label for="entity">Entity</label>
							<input id="entity" name="entity" type="text" class="form-control">
						</div>
					</div>

					<%--Disabling the Attachment entity following issue number #134 on github--%>
					<%--<div class="row">
						<div class="form-group col-sm-12">
							<label for="attachmentEntity">Attachment Entity</label>
							<input id="attachmentEntity" name="attachmentEntity" type="text" class="form-control">
						</div>
					</div>--%>
				</div>
				<!--/Text-->

				<!--Correspondent-->
				<div class="correspondent search-wraper">
					<h4>Correspondents</h4>

					<!--To-->
					<div class="row">
						<div class="form-group col-sm-12">
							<label for="correspondent">Name or email</label>
							<input id="correspondent" name="correspondent" type="text" class="form-control">
						</div>

						<div class="checkbox-inline">
							<label style="margin-left:10px;">
								<input type="checkbox" name="correspondentTo" checked>
								<span class="label-text">To</span>
							</label>
						</div>
						<div class="checkbox-inline">
							<label style="margin-left:10px;">
								<input type="checkbox" name="correspondentFrom" checked>
								<span class="label-text">From</span>
							</label>
						</div>
						<div class="checkbox-inline">
							<label style="margin-left:10px;">
								<input type="checkbox" name="correspondentCc" checked>
								<span class="label-text">Cc</span>
							</label>
						</div>
						<div class="checkbox-inline">
							<label style="margin-left:10px;">
								<input type="checkbox" name="correspondentBcc" checked>
								<span class="label-text">Bcc</span>
							</label>
						</div>
					</div>

					<br/>

                    <!--
                    <div class="row">
                        <div class="form-group col-sm-12">
                            <label for="emailAddressList">Upload email addresses</label>
                            <input id="emailAddressList" type="file" name="emailAddressList" type="text" class="form-control">
                        </div>
                    </div>
                    -->

					<div class="row">
						<!--mailing list-->
						<div class="form-group col-sm-6">
							<label for="mailing-list">Mailing List</label>

							<fieldset id="mailing-list" class="comman-radio">
								<legend class="sr-only">Mailing lists</legend>

								<label class="radio-inline">
									<input type="radio" name="mailingListState" value="yes">
									<span class="text-radio">Yes</span>
								</label>

								<label class="radio-inline">
									<input type="radio" name="mailingListState" value="no">
									<span class="text-radio">No</span>
								</label>

								<label class="radio-inline">
									<input id="mailingListState-either" type="radio" name="mailingListState" value="either" checked>
									<span class="text-radio">Either</span>
								</label>

							</fieldset>
						</div>
					</div>
					<!--/mailing list-->
				</div>
				<!--/Correspondent-->

				<!--Attachment-->
                <% if (!ModeConfig.isDiscoveryMode()) { %>
	                <div class="attachments search-wraper clearfix">
					<h4>Attachments</h4>
					<!--form-wraper-->
					<div class="form-wraper clearfix">

						<div class="row"> <!-- not sure why this div.row is needed. without it, the attachments panel looks bad -->
							<!--File Name-->
							<div class="margin-btm col-sm-6">

								<!--input box-->
								<div class="form-group">
									<label for="attachmentFilename">File Name</label>
									<input name="attachmentFilename" id="attachmentFilename" type="text" class="form-control">
								</div>

								<!--checkboxes-->
								<div class="checkbox-wraper">
									<fieldset>
										<legend class="sr-only">file name filters</legend>

										<div class="checkbox-inline">
											<label>
												<input id="attachmentFilenameRegex" name="attachmentFilenameRegex" type="checkbox">
												<span class="label-text">Reg Ex</span>
											</label>
										</div>

									</fieldset>
								</div>
							</div>
							<!--File Size-->
							<div class="form-group col-sm-6">
								<label for="attachmentFilesize">File Size</label>
								<select id="attachmentFilesize" name="attachmentFilesize" class="form-control selectpicker">
									<option value="" selected disabled>Select</option>
									<option value="1">&lt; 5KB</option>
									<option value="2">5-20KB</option>
									<option value="3">20-100KB</option>
									<option value="4">100KB-2MB</option>
									<option value="5">&gt; 2MB</option>
								</select>
							</div>
						</div>

						<%
							Map<String,String> attachmentTypeOptions= Config.attachmentTypeToExtensions;
						%>
							<!--Type-->
						<div class="row">

						<div class="form-group col-sm-6">
								<label for="attachmentType">Type</label>
								<select name="attachmentType" id="attachmentType" class="form-control multi-select selectpicker" title="Select" multiple>
									<option value="" selected disabled>Select</option>
									<%
										for (Map.Entry<String,String> opt : attachmentTypeOptions.entrySet()){
											if(opt.getKey().toLowerCase().equals(opt.getValue().toLowerCase())){
									%>
									<option value = "<%=opt.getValue()%>"><%=opt.getKey()%></option>
									<%
									}else{
									%>

									<option value = "<%=opt.getValue()%>"><%=opt.getKey()+" ("+opt.getValue()+")"%></option>
									<%} }%>
								</select>
							</div>

							<!--Extension-->
							<div class="form-group col-sm-6">
								<label for="attachmentExtension">Other extension</label>
								<input name="attachmentExtension" id="attachmentExtension" type="text" class="form-control">
							</div>
						</div>
					</div>
				</div>
                <% } %>

                <!--/Attachment-->

				<!--Actions-->

                <div class=" actions search-wraper clearfix">

					<h4>Actions</h4>

					<!--form-wraper-->
					<div class="form-wraper clearfix">

						<!--Annotation-->
						<div class="form-group col-sm-6">

							<div class="form-group">
								<label for="annotation">Annotation</label>
								<input id="annotation" name="annotation" type="text" class="form-control">
							</div>

							<div class="checkbox-inline">
								<label>
									<input id="anyAnnotationCheck" name="anyAnnotationCheck" type="checkbox">
									<span class="label-text">Any annotation</span>
								</label>
							</div>
					    </div>

						<div class="form-group col-sm-6">
							<div class="form-group">

							<label for="labelIDs">Labels</label>
							<select name="labelIDs" id="labelIDs" class="label-selectpicker form-control multi-select selectpicker" title="Select" multiple>
								<option value="" selected disabled>Select</option>
								<optgroup label="Restriction Labels">
								<%
									Set<Label> restrlabels = archive.getLabelManager().getAllLabels(LabelManager.LabType.RESTRICTION);
									//get general labels
									Set<Label> genlabels = archive.getLabelManager().getAllLabels(LabelManager.LabType.GENERAL);
									for (Label opt : restrlabels){
								%>
										<option value = "<%=opt.getLabelID()%>"><%=opt.getLabelName()%></option>
								<%}%>
								</optgroup>
								<optgroup label="General Labels">
								<%
									for (Label opt : genlabels){
								%>
								<option value = "<%=opt.getLabelID()%>"><%=opt.getLabelName()%></option>
								<%}%>
								</optgroup>
								<optgroup label="Expired restrictions (except 'Cleared for release')">
								<option value="<%=LabelManager.ALL_EXPIRED%>">All restrictions expired</option>
								</optgroup>
							</select>

							</div>
							<div class="checkbox-inline">
								<label>
									<input id="multiLabelsCheck" name="multiLabelsCheck" type="checkbox">
									<span class="label-text">More than one restriction label</span>
								</label>
							</div>
						</div>

                    </div>
				</div>
               <!--/Actions-->
				<!--Others-->
				<div class="others search-wraper">
					<h4>Miscellaneous</h4>
					<div class="row">

						<!--Time Range-->
						<div class="form-group col-sm-6">
							<label for="time-range">Time Range</label>
							<div id="time-range" class="date-input-group">
							<%--	<div class='input-group date' id='startDate'>
									<input type='text' class="form-control" />
									<span class="input-group-addon">
                        			<span class="glyphicon glyphicon-calendar"></span>
                    				</span>
								</div>--%>
								<input type = "text"  id="startDate" name="startDate" class="form-control" placeholder="YYYY - MM - DD">
								<label for="endDate">To</label>
								<input type = "text"  id="endDate" name="endDate"  class="form-control" placeholder="YYYY - MM - DD">
							</div>
						</div>

						<!--Message Directions-->
						<div class="form-group col-sm-6">
							<label for="direction">Message Direction</label>
							<fieldset name="direction" id="direction" class="comman-radio">
								<legend class="sr-only">Message Direction filters
								</legend>
								<label class="radio-inline">
									<input value="in" type="radio" name="direction">
									<span class="text-radio">Incoming</span>
								</label>

								<label class="radio-inline">
									<input value="out" type="radio" name="direction">
									<span class="text-radio">Outgoing</span>
								</label>

								<label class="radio-inline">
									<input id="direction-either" value="either" type="radio" name="direction" checked>
									<span class="text-radio">Either</span>
								</label>
							</fieldset>
						</div>
					</div>
					<! -- row -->
					<!--Email Source-->
					<div class="row">
						<div class="form-group col-sm-6">
							<label for="emailSource">Email Source</label>
							<select id="emailSource" name="emailSource" class="form-control selectpicker">
								<option value="" selected disabled>Select</option>
								<% Collection<String> emailSources = archive.getAllEmailSources();
									for (String emailSource : emailSources) { %>
										<option value="<%=emailSource%>"><%=emailSource%></option>
								<% }%>
							</select>
						</div>

						<!--Message Folder-->
						<div class="form-group col-sm-6">
							<label for="messageFolder">Message Folder</label>
							<input name="folder" id="messageFolder" type="text" class="form-control">
						</div>

					</div>
                    <% if (!ModeConfig.isDiscoveryMode()) { %>
                        <div class="row">
                            <!--Lexicons-->
                            <div class="form-group col-sm-6">
                                <label for="lexiconName">Lexicons</label>
                                <select id="lexiconName" name="lexiconName" class="form-control selectpicker">
                                    <option value="" selected disabled>Select</option>
                                    <% Collection<String> lexiconNames = archive.getAvailableLexicons();
                                        for (String lexiconName : lexiconNames) { %>
                                    <option value="<%=lexiconName%>"><%=lexiconName%>
                                    </option>
                                    <% }%>
                                </select>
                            </div>

                            <!--Lexicons Category-->
                            <div class="form-group col-sm-6">
                                <label for="lexiconCategory">Lexicon: Category</label>
                                <select id="lexiconCategory" name="lexiconCategory" class="form-control selectpicker">
                                    <option value="" selected disabled>Select</option>
                                </select>
                            </div>
                        </div>
                    <% } %>

					<div class="row">
						<!--Lexicons-->
						<div class="form-group col-sm-6">
							<label for="entityType">Entity type</label>
							<select id="entityType" name="entityType" class="form-control selectpicker">
								<option value="" selected disabled>Select</option>
								<% Set<NEType.Type> entityTypes = archive.getEntityTypes();
									for (NEType.Type type : entityTypes) { %>
										<option value="<%=type.getCode()%>"><%=type.getDisplayName()%></option>
								<% }%>
							</select>
						</div>

						<div class="form-group col-sm-6">
							<label for="uniqueId">Message ID</label>
							<input id="uniqueId" name="uniqueId" type="text" class="form-control">
							</input>
						</div>

					</div>
					<% if(!ModeConfig.isAppraisalMode()){%>
					<div class="row">
						<!--accessions--, only in modes other than appraisal-->
						<div class="form-group col-sm-6">
							<label for="accessionIDs">Accessions</label>
							<select id="accessionIDs" name="accessionIDs" class="form-control selectpicker">
								<option value="" selected disabled>Select</option>
								<% List<Archive.AccessionMetadata> ams= archive.collectionMetadata.accessionMetadatas;
									for (Archive.AccessionMetadata am: ams) { %>
								<option value="<%=am.id%>"><%=am.id%></option>
								<% }%>
							</select>
						</div>

					</div>
					<%}%>

				</div>
				<!--/Others-->

				<!--Sort Results By-->
				<div class="text search-wraper">
					<div class="row"/>
					<div class="margin-btm  col-sm-6">
						<!--slect box-->
						<div class="form-group ">
							<label for="sortBy">Sort results by</label>
							<select id="sortBy" class="form-control selectpicker" name="sortBy">
								<option value="" selected disabled>Select</option>
								<option value="relevance">Most relevant</option>
								<option value="recent">Newest first</option>
								<option value="chronological">Oldest first</option>
							</select>
						</div>

						<!--checkboxes-->
						<!--
						<div class="checkbox-wraper">
							<fieldset>
								<legend class="sr-only">Save this search</legend>

								<div class="checkbox-inline">
									<label>
										<input type="checkbox" name="check-search">
										<span class="label-text">Save this search</span>
									</label>
								</div>
							</fieldset>
						</div>
						-->
					</div>
					</div>
				</div>
				<!--/Sort Results By-->

				<!--Search and clear form buttons-->
				<div class="search-clear-btns">

                    <button class="btn btn-cta" type="submit" id="search-button">Search <i class="icon-arrowbutton"></i> </button>
					<!--clear btn-->
					<button style="margin-left:20px" id="clear-form" class="clear-btn">
						<img src="images/Clear Form.png" alt="clear-icon">
						Clear Form
					</button>
				</div>
				<!--/Search and clear form buttons-->

        </div>
            <!--/container-->
        </form>

	</div>
	<!--/Advanced Search-->



		<script>
			$('#clear-form').click(function() {
				$('input').val('');
			});
		</script>
	   	<!-- selectpicker -->
	<script>
		$('#clear-form').click(function() {
			$('#adv-search.form input').val();
			return false;
		});

		$('#search-button').click(function() {
			$('#adv-search.form').submit();
		});

         var autocomplete_params = {
			serviceUrl: 'ajax/correspondentAutoComplete.jsp?archiveID=<%=archiveID%>',
			onSearchError: function (query, jqXHR, textStatus, errorThrown) {epadd.log(textStatus+" error: "+errorThrown);},
			preventBadQueries: false,
			showNoSuggestionNotice: true,
			preserveInput: true,
			ajaxSettings: {
				"timeout":3000,
				dataType: "json"
			},
			dataType: "text",
			//100ms
			deferRequestsBy: 100,
			onSelect: function(suggestion) {
				var existingvalue = $(this).val();
				var idx = existingvalue.lastIndexOf(';');
				if (idx <= 0)
					$(this).val(suggestion.name);
				else
					$(this).val (existingvalue.substring (0, idx+1) + ' ' + suggestion.name); // take everything up to the last ";" and replace after that
			},
			onHint: function (hint) {
				$('#autocomplete-ajax-x').val(hint);
			},
			onInvalidateSelection: function() {
				epadd.log('You selected: none');
			}
		};

		$('#correspondent').autocomplete(autocomplete_params);

		var emailSourceAutoCompleteParams = $.extend({}, autocomplete_params);
		emailSourceAutoCompleteParams.serviceUrl = 'ajax/emailSourceAutoComplete.jsp?archiveID=<%=archiveID%>';
		$('#emailSource').autocomplete(emailSourceAutoCompleteParams);

		var folderAutoCompleteParams = $.extend({}, autocomplete_params);
		folderAutoCompleteParams.serviceUrl = 'ajax/folderAutoComplete.jsp?archiveID=<%=archiveID%>';
		$('#messageFolder').autocomplete(folderAutoCompleteParams);

		var entitiesAutoCompleteParams = $.extend({}, autocomplete_params);
		entitiesAutoCompleteParams.serviceUrl = 'ajax/entitiesAutoComplete.jsp?archiveID=<%=archiveID%>';
		$('#entity').autocomplete(entitiesAutoCompleteParams);

		//Disabling the following code as per issue number #134 on github
		//There was no file named attachmentEntitiesAutoComplete.jsp there
		/*
		var attachmentEntitiesAutoCompleteParams = $.extend({}, autocomplete_params);
		attachmentEntitiesAutoCompleteParams.serviceUrl = 'ajax/attachmentEntitiesAutoComplete.jsp';
		$('#attachmentEntity').autocomplete(attachmentEntitiesAutoCompleteParams);
		*/
		var annotationAutoCompleteParams = $.extend({}, autocomplete_params);
		annotationAutoCompleteParams.serviceUrl = 'ajax/annotationAutoComplete.jsp?archiveID=<%=archiveID%>';
		$('#annotation').autocomplete(annotationAutoCompleteParams);

		var attachmentAutoCompleteParams = $.extend({}, autocomplete_params);
		attachmentAutoCompleteParams.serviceUrl = 'ajax/attachmentAutoComplete.jsp?archiveID=<%=archiveID%>';
		$('#attachmentFilename').autocomplete(attachmentAutoCompleteParams);
		$('#attachmentFilenameRegex').change(function() {
			if (this.checked) {
				$('#attachmentFilename').autocomplete().disable();
			} else {
				$('#attachmentFilename').autocomplete().enable();
			}
		});

		var attachmentExtAutoCompleteParams = $.extend({}, autocomplete_params);
		attachmentExtAutoCompleteParams.serviceUrl = 'ajax/attachmentAutoComplete.jsp?extensions=1?archiveID=<%=archiveID%>';
		$('#attachmentExtension').autocomplete(attachmentExtAutoCompleteParams);

		$('#lexiconName').change(function() {
			$options = $('#lexiconCategory option');
			$options.each (function (i, e) { if (i > 0) $(e).remove(); }); // remove all except the first option (which is "select")

			$.ajax({type: 'POST',
				dataType: 'json',
				url: 'ajax/getLexiconCategories.jsp',
				data: {lexicon: $('#lexiconName').val(), archiveID:'<%=archiveID%>' },
				cache: false,
				success: function (response) {
					if (response && (response.status == 0)) {
						var status = 'Success! ' + response.message;
						var cats = response.categories;
                        cats = cats.substring(1,cats.length-1).split(",");
						for (var i = 0; i < cats.length; i++) {
							$('#lexiconCategory').append('<option value="' + cats[i] + '">' + cats[i] + '</option>');
						}
						$('.selectpicker').selectpicker('refresh');
					}
					else {
						if (response)
							alert('Error! Code ' + response.status + ', Message ' + response.error);
						else
							alert ('Error! No response from ePADD.');
					}
				},
				error: function() {
					epadd.alert ("Sorry, something went wrong. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report this to epadd_project@stanford.edu.");
				}});

		});

		$('#clear-form').click(function() {
			$('input[type="text"]').val('');

//			$('input[name="mailingListState"]').val(["either"]);
//			$('input[name="reviewed"]').val(["either"]);
//			$('input[name="doNotTransfer"]').val(["either"]);
//			$('input[name="transferWithRestrictions"]').val(["either"]);

			$('input[name="termSubject"], input[name="termBody"], input[name="termAttachments"], input[name="termOriginalBody"]').prop('checked', true);
			$('input[name="termRegex"]').prop('checked', false);

			$('input[name="correspondentTo"], input[name="correspondentFrom"], input[name="correspondentCc"], input[name="correspondentBcc"]').prop('checked', true);
			$('#termSubject, #termBody, #termOriginalBody, #termAttachments').prop('checked', true);
			$('#doNotTransfer-either, #reviewed-either, #transferWithRestrictions-either, #direction-either, #mailingListState-either').prop ('checked', true);
			// TODO: reset the other fields also, esp. the select picker
			$('#attachmentFilesize, #emailSource, #lexiconName, #lexiconCategory, #sortBy,#entityType').prop ('selectedIndex', 0);
			$('#attachmentType').prop ('selectedIndex', -1); // 0 for this one is not ok
			$('#labelIDs').prop('selectedIndex',-1);
			$('.selectpicker').selectpicker('refresh');
		});

	</script>

	<script>

        $(document).ready(function() {
            //installing input tags as datepickers
            //setting min date as 1 jan 1960. The format is year,month,date. Month is 0 based and all other are 1 based
            $('#startDate').datepicker({

                minDate: new Date(1960, 1 - 1, 1),
                dateFormat: "yy-mm-dd"
			});
            $('#endDate').datepicker({

                minDate: new Date(1960, 1 - 1, 1),
                dateFormat: "yy-mm-dd"
            });

        });
	</script>
	</body>
</html>