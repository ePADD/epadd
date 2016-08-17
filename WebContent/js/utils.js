initialiseqtip = function(){
	$('.qtip')
		.remove();
	//TODO: use a proper selector
    var qtip_params = {
        hide:{delay:'200',fixed:true},
        style: {width: '450px',padding: 7,color: 'black',textAlign: 'left',border: {radius: 2,color: 'black'}}
    };

    //There can be many nested spans, in that case just consider the outer most span element
	$('div.muse-doc-body span[data-ignore!=][title!=]')
        .filter(function(){
            return this.parentNode.tagName!=="SPAN"
        })
        .qtip(qtip_params);

    $('div.muse-doc-header span[data-ignore!=][title!=]')
        .filter(function(){
            //dont add qtip to highlighted terms in the header like emaill addresses
            return this.parentNode.tagName!=="SPAN" && this.className.contains("custom");
        })
        .qtip(qtip_params);
};

expand = function(name,docId,id){
	epadd.log("Expanding name: "+name);
	epadd.log(docId+", "+id);
	$.ajax({
		type: "POST",
		url: "./ajax/expandname.jsp",
		dataType: "html",
		async: false,
		data: {
			"name": name,
			"docId": docId 
		},
		success: function(data,status,jqXHR){
			epadd.log("Response "+data);
			json = JSON.parse(data);
	        if(typeof(json.result)==="undefined")
                json.result = "No confident matches!";
			$("#expand_"+id).html(json.result);
            //reinitialiseqtip();
		},
	    error: function(jqXHR,exception, error){
	    	epadd.log("Error while expanding word."+exception+" error:"+error);
	  		return null;
		}
	});
};

sentenceTokenize = function(text){
	$.ajax({
		type: "POST",
		url: "./ajax/nlphelper.jsp",
		contentType: "application/x-www-form-urlencoded;charset=ISO-8859-15",
		dataType: "text",
		async: false,
		data: {
			"text": text,
			"action": "sentenceTokenize" 
		},
		success: function(data,status,jqXHR){
			sents = JSON.parse(data);
		},
	    error: function(jqXHR,exception, error){
	    	epadd.log("Error while tokenizing sentence."+exception+" error:"+error);
	  		return null;
		}
	});
};

insertSummary = function(data,u,name,elementId){
	desc = "";
	img = "";
	
	paras = $(data).find("p");
	if(paras.length<=0){
		document.getElementById(elementId).innerHTML = "<div style='text-align: center;color: red;'>Unable to reach network</div>";
		return;
	}
	paras.each(function(i,d){
		if(i==0){
			sentenceTokenize($(d).text());
			if(sents == null||sents.length == 0)
				desc = $(d).text().split(".")[0]+".";
			else
				desc = sents[0];
		}
	});
	src = $($(data).find(".infobox img")[0]).attr("src");
	img = "http:"+src;
	malePicture = "images/male_icon.png";
	femalePicture = "images/female_icon.png";
	if(src==null||typeof(src)=="undefined"){
		if(data!=null&&typeof(data)!="undefined"){
			if(data.split(" his ").length>data.split(" her ").length)
				img=malePicture;
			else
				img = femalePicture;
		}
	}
	t = $($(data).find("title")[0]).text();
	
	html = "<div class='wikiPage'>" +"<table style='margin: 4px;'><tr><td>"
	 +"<img alt='?' src='" + img
	+ "' width='100px'></td>"
	+ "<td><table style='width:100%;margin:10px;padding: 10px;'><tr><td><a href='" + u + "'>" + name
	+ "</a></td></tr>" + "<tr><td>" + desc
	+ "</td></tr></table></td></tr></table></div>";
	//$("#pages").append("<div style='float:right' onclick='rem("+cnt+")' id='rem_"+cnt+"'><img width=16 height=16 src='images/close.png'></img></div>");
	document.getElementById(elementId).innerHTML = html;
	//$("#page_"+name).css("width", "260");
};

geoResolve = function(lat,lon,elementId){
	$.ajax({
		type: "post",
		url: "./ajax/sortgeolocations.jsp",
		data: {
			"latitude": lat,
			"longitude": lon
		},
		success: function(data,status,jqXHR){
			//returns title of the article.	
			document.getElementById(elementId).innerHTML = data;
		},
	    beforeSend: function(jqXHR, settings) {
	    	jqXHR.url = settings.url;
	    	jqXHR.str = settings.str;
	    	jqXHR.name = settings.name;
	    },
	    error: function(jqXHR,exception){
	  		epadd.log("There was an error while fetching page."+exception);
		}
	});
	document.getElementById(elementId).innerHTML = "<div id='temp' style='width:350px;text-align:center;'><img src='images/spinner.gif'></div>";
};

resolve = function(str,elementId){
	name =  decodeURI(str);
	str = decodeURI(str);
	str = str.replace(/ /g,"_");
	str = "http://en.wikipedia.org/wiki/"+str;
	epadd.log("Fetching: "+str);
	$.ajax({
		type: "post",
		url: "./ajax/fetchurl.jsp",
		str: str,
		"name": name,
		data: {
			"url": str
		},
		success: function(data,status,jqXHR){
			insertSummary(data,jqXHR.str,jqXHR.name,elementId);
		},
	    beforeSend: function(jqXHR, settings) {
	    	jqXHR.url = settings.url;
	    	jqXHR.str = settings.str;
	    	jqXHR.name = settings.name;
	    },
	    error: function(jqXHR,exception){
	  		epadd.log("There was an error while fetching page.");
		}
	});
	document.getElementById(elementId).innerHTML = "<div id='temp' style='width:350px;text-align:center;'><img src='images/spinner.gif'></div>";
};

getFastData = function(id){
	epadd.log(id);
	$.ajax({
		type: "POST",
		url: "./ajax/resolveFASTId.jsp",
		id: id,
		data: {
			"id" : id
		},
		success: function(data,status,jqXHR){
			epadd.log(data);
			$("#fast_"+jqXHR.id).html(data);
		},
	    beforeSend: function(jqXHR, settings) {
	    	jqXHR.id = settings.id;
	    },
	    error: function(jqXHR,exception){
	  		epadd.log("There was an error while resolving the fast id:"+jqXHR.id);
		}
	});
	document.getElementById('fast_'+(id)).innerHTML = "<div id='temp' style='width:350px;text-align:center;'><img src='images/spinner.gif'></div>";
};

clean = function(ids){
	epadd.log("Cleaning input fields...");
	ids.map(function(d){$("#"+d).val("");});
};

var manual_assign = function(){
	$(".manual-spinner").fadeIn();

    //TODO: get rid of localstorage usages.
	var entitynum = localStorage.getItem("entitynum"); // ?!$ why use localStorage for this?!

	var in_ids = ["fast","viaf","dbpedia","locSubject","locName","freebase"];
	var ids = [], types = [];

	for (var i=0;i<in_ids.length;i++){
		var val = $("#"+in_ids[i]).val();
		if (val) {
			ids.push(val);
			types.push(in_ids[i]);
		}
	}
	if (ids.length == 0) {
		alert("Please assign at least one ID.");
		return;
	}

	for (var i = 0; i < ids.length; i++) {
		epadd.log('manually assigning id: ' + ids[i] + ", type: " + types[i]);
	}

	$.ajax({
		type: "POST",
		url: "./ajax/manual-authority.jsp",
		id: entitynum,
		data: {"ids" : ids, "idTypes": types},
		success: function(data,status,jqXHR){
			if(data.indexOf("No record found!")==-1){
				epadd.log('data received = ' + data);
				$("#manual_"+entitynum).closest("td").prepend(data+"<br>");
			} else {
				alert("Sorry, we couldn't find that id in our databases. Please report this error to the ePADD development team.");
			}
			clean(in_ids);
			//close the popup
			$(".manual-spinner").hide();
			$("a.fancybox-item.fancybox-close").click();
            qtipreinitialise();
		},
		beforeSend: function(jqXHR, settings) {
			jqXHR.id = settings.id;
		},
		error: function(jqXHR,exception){
			epadd.log("There was an error while assigning authority:"+jqXHR.id);
			clean(in_ids);
			$(".manual-spinner").hide();
			$("a.fancybox-item.fancybox-close").click();
		}
	});
};