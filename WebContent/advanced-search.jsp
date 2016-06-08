<%@ page import="java.util.Collection" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<!--
	<style>
		td > div {
			padding: 5px;
		}
		.option { margin-right:15px;}
	</style>
	-->
		<title>Advanced Search</title>
		<meta name="description" content="">
	<link rel="stylesheet" href="css/main.css">
</head>
	<body>
	<jsp:include page="header.jspf"/>
	<script>epadd.nav_mark_active('Search');</script>


	<!--Advanced Search-->
    	<div class="advanced-search">
    	    <!--container-->
    		<div class="container">
    		   <!--row-->
    			<div class="row">
                    <div class="heading-wraper">
						<h1 >Advanced Search</h1>
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
					 	<h4>Text</h4>
						<br/>

					   <!--message-->
						<div class="margin-btm  col-sm-6">

							<!--input form-->
						   <div class="form-group">
								<label for="message">Message</label>
								<input name="term" id="message" type="text" class="form-control">
							</div>

							 <!--checkboxes-->
							 <div class="checkbox-wraper ">
								<fieldset>
									<legend class="sr-only">Message filters using checkbox
									</legend>

									<div class="checkbox-inline">
									  <label>
										<input type="checkbox" name="check">
										<span class="label-text">Reg Ex</span>
										</label>
									</div>

									<div class="checkbox-inline">
									  <label>
										<input type="checkbox" name="check">
										<span class="label-text">Limit to Orig</span>
										</label>
									</div>
								</fieldset>
							</div>
						</div>
						<!--/message-->

						<!--subject-->
						<div class=" margin-btm  col-sm-6">

							<!--input box-->
							<div class="form-group">
								<label for="subject">Subject</label>
								<input id="subject" type="text" class="form-control">
							</div>

							<!--checkboxes-->
							 <div class="checkbox-wraper">
								<fieldset>
									<legend class="sr-only">Subject filters </legend>

									 <div class="checkbox-inline">
									  <label>
										<input type="checkbox" name="check2">
										<span class="label-text">Reg Ex</span>
									  </label>
									</div>

								</fieldset>
							</div>

						</div>
						<!--/subject-->

						<!--Entity-->
						<div class="form-group col-sm-12">
							<label for="subject">Entity</label>
							<input type="text" class="form-control">
						</div>
					</div>
					<!--/Text-->

					<!--Coresspondent-->
					<div class="correspondent search-wraper">
					 	<h4>Correspondent</h4>

						   <!--To-->
						 	<div class="form-group col-sm-6">
		 					    <label for="to">To</label>
							    <input id="to" type="text" class="form-control">
						  	</div>

						  	<!--cc/Bcc-->
						  	<div class="form-group col-sm-6">
		 					    <label for="cc-bcc">CC/BCC</label>
							    <input id="cc-bcc" type="text" class="form-control">
						  	</div>

						  	<!--mailing list-->
						  	<div class="form-group col-sm-6">
		 					    <label for="mailing-list">Mailing List</label>

		 					     <fieldset id="mailing-list" class="comman-radio">
									<legend class="sr-only">Mailing lists</legend>

								    <label class="radio-inline">
								      <input type="radio" name="optradio">
								       <span class="text-radio">None</span>
								    </label>

								    <label class="radio-inline">
								      <input type="radio" name="optradio">
								       <span class="text-radio">At least one</span>
								    </label>

								    <label class="radio-inline">
								      <input type="radio" name="optradio">
								       <span class="text-radio">All</span>
								    </label>

							    </fieldset>
						  	</div>
						  	<!--/mailing list-->
					 </div>
					 <!--/Correspondent-->

					 <!--Attachment-->
					<div class=" attachment search-wraper clearfix">

					 	<h4>Attachment</h4>

					 	<fieldset class="show-form">
							<legend class="sr-only">Show attachment </legend>
						  	 <div class="checkbox-inline">
						      <label>
						      	<input type="checkbox" name="show-check" >
						      	<span class="label-text">Has Attachment</span>
						      </label>
						    </div>
						</fieldset>


						<!--form-wraper-->
						 <div class="form-wraper clearfix">

						   <!--Content-->
							<div class="margin-btm  col-sm-6">

								<!--input box-->
								<div class="form-group">
									<label for="Content">Content</label>
									<input id="content" type="text" class="form-control">
								</div>

								 <!--checkboxes-->
								 <div class="checkbox-wraper">
									<fieldset>
										<legend class="sr-only">content filters
										</legend>

										<div class="checkbox-inline">
										  <label>
											<input type="checkbox" name="check-attachment">
											<span class="label-text">Reg Ex</span>
											</label>
										</div>
									</fieldset>
								</div>

							</div>
							  	<!--/Content-->

							<!--File Name-->
							<div class="margin-btm col-sm-6">

								<!--input box-->
								 <div class="form-group">
									<label for="attachment-name">File Name</label>
									<input id="attachment-name" type="text" class="form-control">
								</div>

								<!--checkboxes-->
								 <div class="checkbox-wraper">
									<fieldset>
										<legend class="sr-only">file name filters </legend>

										 <div class="checkbox-inline">
										  <label>
											<input type="checkbox" name="check-attachment">
											<span class="label-text">Reg Ex</span>
										  </label>
										</div>

									</fieldset>
								</div>

							</div>

							<!--Type-->
							<div class="form-group col-sm-6">
								<label for="attachment-type">Type</label>
								  <select id="attachment-type" class="form-control multi-select selectpicker"
								  name="attachment_type" title="Select" multiple>
									<option value="avi,mp4">Video (avi, mp4, etc.)</option>
									<option value="mp3,ogg">Audio (mp3, etc.)</option>
									<option value="jpg,png,gif,bmp">Graphics (jpg, png, bmp, gif, etc.)</option>
									<option value="fmp,db,mdb,accdb">Database (fmp, db, mdb, accdb, etc.)</option>
									  <option value="ppt,pptx,key">Presentation (ppt, pptx, etc.)</option>
									  <option value="xls,xlsx,numbers">Spreadsheet (pptx, ppt, etc.)</option>
									  <option value="doc,docx,pages">Document (doc, docx, pdf, etc.)</option>
									  <option value="html,css,js">Internet file (html, etc.)</option>
									  <option value="zip,7z,tar,tgz">Zip (zip, 7zip, tar, ar, etc.)</option>
								  </select>
							</div>

							<!--Extension-->
							 <div class="form-group col-sm-6">
								 <label for="attachment-extension">Other extension</label>
								 <input name="attachment-extension" id="attachment-extension" type="text" class="form-control">
							 </div>

							 <!--File Size-->
							<div class="form-group col-sm-6">
								<label for="attachment-filesize">File Size</label>
								<select id="attachment-filesize" name="attachment-filesize" class="form-control selectpicker" name="Choose-file">
								   <option value="" selected disabled>Choose File Size</option>
									<option value="1">&lt; 5KB</option>
									<option value="2">5-20KB</option>
									<option value="3">20-100KB</option>
									<option value="4">100KB-2MB</option>
									<option value="5">&gt; 2MB</option>
								  </select>
							</div>
						</div>
					 </div>
					 <!--/Attachment-->

					  <!--Actions-->
					<div class=" actions search-wraper clearfix">

					 	<h4>Actions</h4>

					 	<fieldset class="show-form">
							<legend class="sr-only">Show attachment </legend>
						  	 <div class="checkbox-inline">
						      <label>
						      	<input type="checkbox" name="show-check">
						      	<span class="label-text">Include Action</span>
						      </label>
						    </div>
						</fieldset>


						<!--form-wraper-->
						 <div class="form-wraper clearfix">

						   <!--Annotation-->
							<div class="margin-btm form-group col-sm-6">
								<label for="annotation">Annotation</label>
								<input id="annotation" name="annotation" type="text" class="form-control">
							</div>

							<!--Reviewed-->
							<div class="form-group col-sm-6">
								<label for="reviewed">Reviewed</label>

								 <fieldset name="subject" id="reviewed" class="comman-radio">
									<legend class="sr-only">Reviewed flters </legend>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">Yes</span>
									</label>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">No</span>
									</label>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">Do Not Apply</span>
									</label>

								</fieldset>
							</div>
							<!--/Reviwed-->

							<!--Restricted-->
							<div class="form-group col-sm-6">
								<label for="restricted">Restricted</label>

								 <fieldset id="restricted" name="restricted" class="comman-radio">
									<legend class="sr-only">Restricted flters </legend>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">Yes</span>
									</label>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">No</span>
									</label>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">Do Not Apply</span>
									</label>

								</fieldset>
							</div>
							<!--/Restricted-->

							<!--Transfer-->
							<div class="form-group col-sm-6">
								<label for="Transfer">Transfer</label>

								 <fieldset id="transfer" name="transfer" class="comman-radio">
									<legend class="sr-only">Transfer flters </legend>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">Yes</span>
									</label>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">No</span>
									</label>

									<label class="radio-inline">
									  <input type="radio" name="optradio">
									   <span class="text-radio">Do Not Apply</span>
									</label>

								</fieldset>
							</div>
							<!--/Transfer-->
						</div>
					</div>
					<!--/Actions-->

					<!--Others-->
					<div class="others search-wraper">
					 	<h4>Others</h4>

					   <!--Time Range-->
						<div class="form-group col-sm-6">
							<label id="time-range" for="Time Range">Time Range</label>
							<div class="date-input-group">
								<input id="Time Range" type="date" class="form-control" placeholder="YYYY / MM / DD">
								<label for="to">To</label>
								<input type="date" class="form-control" placeholder="YYYY / MM / DD">
							</div>
						</div>

						<!--Message Directions-->
						<div class="form-group col-sm-6">
							<label for="direction">Message Direction</label>
							 <fieldset name="direction" id="direction" class="comman-radio">
								<legend class="sr-only">Message Direction filters
								</legend>
								<label class="radio-inline">
								  <input type="radio" name="optradio">
								   <span class="text-radio">Incoming</span>
								</label>

								<label class="radio-inline">
								  <input type="radio" name="optradio">
								   <span class="text-radio">Outgoing</span>
								</label>

								<label class="radio-inline">
								  <input type="radio" name="optradio">
								   <span class="text-radio">Both</span>
								</label>
							 </fieldset>
						</div>

						<!--Email Source-->
						<div class="form-group col-sm-6">
							<label for="email-source">Email Source</label>
							<input name="email-source" id="email-source" type="text" class="form-control">
						</div>

						<!--Message Folder-->
						<div class="form-group col-sm-6">
							<label for="Message Folder">Message Folder</label>
							<input id="Message Folder" type="text" class="form-control">
						</div>

						<!--Lexicons-->
						<div class="form-group col-sm-6">
							<label for="Lexicons">Lexicons</label>
							<select id="Lexicons" class="form-control selectpicker" name="Choose-file">
							   <option value="" selected disabled>Select</option>
								<% 	Collection<String> lexiconNames = archive.getAvailableLexicons();
								for (String lexiconName: lexiconNames) { %>
									<option value="<%=lexiconName%>"> <%=lexiconName%></option>
								<% }%>
							  </select>
						</div>

						<!--Lexicons Category-->
						<div class="form-group col-sm-6">
							<label for="Lexicons Category">Lexicons Category</label>
							<select id="Lexicons Category" class="form-control selectpicker" name="Choose-file">
							   <option value="" selected disabled>Select</option>
								<option  value="1">placeholder1</option>
								<option  value="2">placeholder2</option>
								<option  value="3">placeholder3</option>
								<option  value="4">placeholder4</option>
							  </select>
						</div>
					 </div>
					 <!--/Others-->


					 <!--Sort Results By-->
					 <div class="text search-wraper">
						<div class="margin-btm  col-sm-6">
							<!--slect box-->
							 <div class="form-group ">
								<label for="message">Message</label>
								 <select class="form-control selectpicker" name="Choose-file">
								   <option value="" selected disabled>Select</option>
									<option  value="1">1</option>
									<option  value="2">2</option>
									<option  value="3">3</option>
									<option  value="4">4</option>
								  </select>
							  </div>

							  <!--checkboxes-->
							 <div class="checkbox-wraper">
								<fieldset>
									<legend class="sr-only">Save this search </legend>

									 <div class="checkbox-inline">
									  <label>
										<input type="checkbox" name="check-search">
										<span class="label-text">Save this search</span>
									  </label>
									</div>
								</fieldset>
							</div>
						</div>
					 </div>
						<!--/Sort Results By-->

					 <!--Search and clear form buttons-->
					 <div class="search-clear-btns">
					    <!--search btn-->
					 	<button type="submit" class="search-btn">
						 	<h5>Search</h5>
						 	<img src="images/search-icon.png" allt="search-icon">
					 	</button>

					 	<!--clear btn-->
					 	<button id="clear-form" type="submit" class="clear-btn">
					 	    <img src="images/Clear Form.png" alt="clear-icon">
					 		<h5>Clear Form</h5>
					 	</button>
					 </div>
					<!--/Search and clear form buttons-->

				</div>
    			<!--/row-->
    		</div>
    		<!--/container-->
    	</div>
    	<!--/Advanced Search-->


	   <!--scripts-->
	   	<script  src="js/jquery-1.12.1.min.js"></script>
	   	<script  src="js/jquery-migrate-1.2.1.min.js"></script>
<!--	   	<script src="js/bootstrap.min.js"></script> -->
	   	<script src="js/main.js"></script>
	   	<script src="js/modernizr.min.js"></script>
		<script>
			$('#clear-form').click(function() {
				$('input').val('');
			});
		</script>
	   	<!-- selectpicker -->
		<script src="js/selectpicker.js"></script>
  
	</body>
</html>