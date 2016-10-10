function accuracy(stats){
	ks = Object.keys(stats);
	numCorrect = 0;numTotal = 0;
	for(var i=0;i<ks.length;i++){
		var k = ks[i];
		numTotal += stats[k].length;
		for(var j=0;j<stats[k].length;j++)
			if(stats[k][j]==1)
				numCorrect ++;
	}
	return (numCorrect/numTotal);
}

function averagePos(stats){
	ks = Object.keys(stats);
	pos = 0;numTotal = 0;
	for(var i=0;i<ks.length;i++){
		var k = ks[i];
		numTotal += stats[k].length;
		for(var j=0;j<stats[k].length;j++)
			pos += stats[k][j];
	}
	return (pos/numTotal);
}

function collectStats(withclass){
	stats = {};
	num = 0;
	type="all";
	$("tr").each(function(i,d){
		l=$("td:eq(3)",d);
		pos = parseInt($(l).find("input").val());
		if(stats[type]==undefined){
			if(!isNaN(pos)){
				stats[type] = [];
				stats[type].push(pos);
				num++;
			}
		}else{
			if(!isNaN(pos)){
				stats[type].push(pos);
				num++;
			}
		}
	});
	console.log("Collected stats from: "+num+" records");
	console.log(accuracy(stats));
	console.log(averagePos(stats));
	console.log(stats);
}