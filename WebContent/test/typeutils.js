function submit(){
	cbks = $("input:checkbox");
	entities = [];dLinks = [];
	types = [];
	cbks.map(function(i,d){
		if($(d).is(":checked"))
			types.push($(d).attr("data-type"));
	});
	
	console.log(types);
	types.map(function(d,i){
		d = d.replace(/\|/g,"");
		$("."+d+" a").each(function(i,k){
			href = $(k).attr("href");
			if(typeof(href)!=="undefined"){
				if(href.indexOf("wikipedia")>0)
					dLinks.push(href);
				else
					entities.push($(k).text());
			}
		});
	});
	if(entities.length>0&&dLinks.length>0&&(entities.length==dLinks.length)){
		$.ajax({
			type: "POST",
			url: "./seedinstancegenerator.jsp",
			data: {
				entities: entities,
				dLinks: dLinks
			},
			success: function(data,status){
				var newWindow = window.open();
				newWindow.document.write(data);
				$(".loading").remove();
			},
			error: function(error){
				console.error("Error while sorting the entities");
				$(".loading").remove();
			}
		});
		$("#submit_area").append("<img style='height:15px' class='loading' src='../images/spinner.gif'/>")
	}else{
		console.error("Not submitting request as entities size: "+entities.length+", links size: "+dLinks.length);
	}
}