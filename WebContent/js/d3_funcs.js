//data: array of objects with the fields: caption, urlhistogram (histogram captures monthly frequencies, or the x-axis variable)
//start_yy and start_mm are starting dates. start_mm indexed from 0
//assumption: only one stacked graph per page
//totalVolume is required only for Pct view (& tooltip as we have info anyway)
function draw_stacked_graph_d3(canvasDiv_graph, canvasDiv_legend, data, totalVolume, totalWidth, totalHeight, start_yy, start_mm, colorCategory, click_func, graph_type, focusOnly)
{
	// opacity[+is_selected][+is_highlighted] - use + to turn bool to int
	var OPACITY_FILL = [ [0.3, 0.3], [0.9, 1] ];
	var OPACITY_STROKE = [ [0, 0.3], [0.1, 1] ];

	//data.reverse(); // d3 makes first element go to bottom. so, reverse the layers first.
	if (graph_type != 'curvy')
		data.map(function(v) { v.histogram.push(0); }); // append 0 so the right-most bar will have width

	var nMonths = data[0].histogram.length;
	var histogram_sum = [];
	var months = [];
	var i;
	for (i = 0; i < nMonths; i++) {
		histogram_sum[i] = 0;
		months[i] = new Date(start_yy, start_mm+i);
	}
	data.forEach(function(d, idx) {
		d.index = idx;
		d.values = d.histogram.map(function(v, i) {
			histogram_sum[i] += v;
			return {layer: idx, v: v};
		});
		d.count = d3.sum(d.histogram);
		d.selected = (d.count != 0);
	});

	var full_labels = data.map(function(d) { return (typeof d.full_caption != 'undefined' && d.full_caption) ? d.full_caption: d.caption;});

	var y_max = d3.max(histogram_sum);

	var startDate = months[0];
	var endDate = months[nMonths-1];
	var formatDate = d3.time.format("%b %Y");

	var contextHeight = focusOnly ? 18 : 100;
	var yAxisWidth = String(y_max).length*8 + 10;
	var margin = {top: 4, right: 4, bottom: contextHeight, left: yAxisWidth};
	var margin2 = {top: totalHeight-contextHeight+40, right: 4, bottom: 18, left: yAxisWidth};
	var width = totalWidth - margin.left - margin.right;
	var height = totalHeight - margin.top - margin.bottom;
	var height2 = totalHeight - margin2.top - margin2.bottom;

	var x = d3.time.scale().range([0, width]).domain([startDate, endDate]);
	var y = d3.scale.linear().range([height, 0]).domain([0, y_max]);

	var xAxis = d3.svg.axis().scale(x).orient("bottom");
	setNumMonthsPerTick(xAxis, x.domain(), width);
	var nTicksY = d3.min([y_max, Math.floor(height / 40)]); // "40px" per Y tick. also avoid going subdecimal.
	var yAxis = d3.svg.axis().scale(y).orient("left").ticks(nTicksY);

	var stack = d3.layout.stack()
	.offset("zero")
	.values(function(d) { return d.values; })
	.x(function(d, i) { return months[i]; })
	.y(function(d) { return data[d.layer].selected ? d.v : 0; })
	;

	var area = d3.svg.area()
	.interpolate(graph_type=='curvy'?'basis':'step-after')
	.x(function(d, i) { return x(months[i]); })
	.y0(function(d) { return y(d.y0); })
	.y1(function(d) { return y(d.y0 + d.y); })
	;

	var dragging = false;
	var mousedown_highlight = null;
	var current_highlight = null;

	function dragstart() {
		mousedown_highlight = current_highlight;
		dragging = true;
	}

	function dragend() {
		dragging = false;
		if (current_highlight != mousedown_highlight) {
			legend_highlight(mousedown_highlight, false);
			legend_highlight(current_highlight, true);
		}
	}

	var svg = d3.select(canvasDiv_graph).append("svg")
	.attr("width", "100%")
	.attr("height", "100%")
	.attr("viewBox", "0 0 " + totalWidth + " " + totalHeight)
	.on("mouseup", dragend) // catch mouse release even outside the chart area to cancel drag
	;

	svg.append("defs").append("clipPath")
	.attr("id", "clip")
	.append("rect")
	.attr("width", width)
	.attr("height", height);

	var focus = svg.append("g")
	.attr("transform", "translate(" + margin.left + "," + margin.top + ")")
	;
	
	var zoom = d3.behavior.zoom()
	.x(x).y(y)
	.scaleExtent([1, d3.min([width, height2, y_max*2])]) // do not zoom until brush's size is 0 (which cancels zoom)
	.on("zoom", zoom_func)
	;

	// explicit background so it can be dragged and cursor can also be set to "move"
	var background = focus.append("svg:rect")
	.attr("width", width).attr("height", height)
	.style("fill-opacity", 0).style("cursor", "move")
	.on("mousedown", dragstart)
	.call(zoom)
	;

	var layers = stack(data);

	var layer = focus.selectAll(".layer")
	.data(layers)
	.enter()
	.append("path");

	var current_idx = -1;

	layer.attr("class", "layer")
	.attr("clip-path", "url(#clip)")
	.attr("d", function(d) { return area(d.values); })
	.style("fill", function(d, i) { return colorCategory(i); })
	.style("fill-opacity", function(d) { return OPACITY_FILL[+d.selected][0]; })
	.style("stroke", "black")
	.style("stroke-width", 2)
	.style("stroke-opacity", function(d) { return OPACITY_STROKE[+d.selected][0]; })
	.style("cursor", "pointer")
	.on("mouseover", function(d) { current_highlight = d; if (!dragging) legend_highlight(d, true); })
	.on("mousemove", function(d) {
		if (dragging) return;
		var idx = getNumMonths(startDate, x.invert(d3.mouse(this)[0]));
		if (idx == current_idx) return;
		current_idx = idx;
		var date_str = " in " + formatDate(months[idx]);
		tooltip.text(function(d, i) {
			return full_labels[i] + "\n" + d.histogram[idx] + " (of " + muse.pluralize(totalVolume[idx],"message") + date_str + ")";
		});
	})
	.on("mouseout", function(d) { current_highlight = null; if (!dragging) legend_highlight(d, false); })
	.on("click", function(d, layer) {
		var date = months[current_idx];
		if (click_func)
			click_func(current_idx, layer, date.getFullYear(), date.getMonth());
	})
	.on("mousedown", dragstart)
	.call(zoom)
	;

	var tooltip = layer
	.append("svg:title")
	.text(function(d, i) { return full_labels[i]; });

	focus.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height + ")")
	.call(xAxis);

	focus.append("g")
	.attr("class", "y axis")
	.call(yAxis);

	var svg_legend = d3.select(canvasDiv_legend).append("svg")
	.attr("width", "99%") // instead of 100% to avoid sporadically unnecessary horizontal scroll
	.attr("height", "100%");

	var legendEnter = svg_legend.selectAll("g.node")
	.data(data)
	.enter()
	.append("svg:g").attr("class", "node")
	.attr("transform", function(d, index) { return "translate(0," + (data.length-index)*20 + ")"; })
	.style("cursor", "pointer")
	.on("click", legend_click)
	.on("mouseover", function(d) { legend_highlight(d, true); })
	.on("mouseout", function(d) { legend_highlight(d, false); })
	.on("mousedown", function() { d3.event.preventDefault(); d3.event.stopPropagation(); })
	;

	legendEnter.append("svg:title").text("click to hide/show layer");

	var rect_w = 10, rect_h = 10;
	legendEnter
	.append("svg:rect")
	.attr("y", 1-rect_h)
	.attr("width", rect_w).attr("height", 10)
	.style("fill", function(d, index) { return colorCategory(index); })
	.style("fill-opacity", function(d) { return OPACITY_FILL[+d.selected][0]; })
	.style("stroke", "black")
	;

	legendEnter
	.append("svg:text")
	.attr("x", rect_w+4)
	.style("fill-opacity", function(d) { return OPACITY_FILL[+d.selected][0]; })
	.text(function(d) { return d.caption + " (" + d.count + ")"; })
	;

	var width_legend = 0;
	legendEnter.selectAll("text").each(function() {
		var t = d3.select(this);
		t.style("font-weight", "bold");
		width_legend = d3.max([width_legend, this.getBBox().width]);
		t.style("font-weight", "normal");
	});
	svg_legend.attr("viewBox", "0 0 " + (width_legend+rect_w+4+2) + " " + ((data.length+1)*20));

	function get_ith_element(l, i) {
		//return l.filter(":nth-child(" + (i) + ")"); // off by 1 or 2, bug?
		return d3.select(l[0][i]);
	}

	function legend_highlight(d, flag) {
		if (d == null) return; // avoid null exception
		//if (dragging) return; // don't want highlight change while dragging
		var font_weight = (d.selected && flag) ? "bold" : "normal";
		get_ith_element(layer, d.index)
		.style("stroke-opacity", OPACITY_STROKE[+d.selected][+flag])
		.style("fill-opacity", OPACITY_FILL[+d.selected][+flag])
		;
		get_ith_element(layer2, d.index)
		.style("stroke-opacity", OPACITY_STROKE[+d.selected][+flag])
		.style("fill-opacity", OPACITY_FILL[+d.selected][+flag])
		;
		var node = get_ith_element(legendEnter, d.index);
		node.select("rect")
		.style("fill-opacity", OPACITY_FILL[+d.selected][+flag])
		;
		node.select("text")
		.style("fill-opacity", OPACITY_FILL[+d.selected][+flag])
		.style("font-weight", font_weight)
		;
	}

	function legend_click(d, index) {
		var node = d3.select(this);
		d.selected = !d.selected;
		node.select("rect")
		.style("fill", d.selected ? colorCategory(index) : "white")
		.style("stroke-opacity", OPACITY_STROKE[+d.selected][1])
		;

		layers = stack(data); // layers is global var. no need to rebind with .data()
		layer
		//.data(layers)
		.transition().duration(500)
		.attr("d", function(d) { return area(d.values); })
		;
		layer2
		//.data(layers)
		.transition().duration(500)
		.attr("d", function(d) { return area2(d.values); })
		;

		legend_highlight(d, true); // click implies mouseover
	}

	var x2 = d3.time.scale().range([0, width]).domain(x.domain());
	var y2 = d3.scale.linear().range([height2, 0]).domain(y.domain());
	var xAxis2 = d3.svg.axis().scale(x2).orient("bottom");
	setNumMonthsPerTick(xAxis2, x2.domain(), width);

	var area2 = d3.svg.area()
	.interpolate(graph_type=='curvy'?'basis':'step-after')
	.x(function(d, i) { return x2(months[i]); })
	.y0(function(d) { return y2(d.y0); })
	.y1(function(d) { return y2(d.y0 + d.y); });

	var context = svg.append("g")
	.attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");

	var layer2 = context.selectAll(".layer")
	.data(layers)
	.enter().append("path")
	.attr("class", "layer")
	.attr("d", function(d) { return area2(d.values); })
	.style("fill", function(d, i) { return colorCategory(i); })
	.style("fill-opacity", function(d) { return OPACITY_FILL[+d.selected][0]; })
	.style("stroke", "black")
	.style("stroke-width", 2)
	.style("stroke-opacity", function(d) { return OPACITY_STROKE[+d.selected][0]; })
	;

	context.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height2 + ")")
	.call(xAxis2);

	var brush = d3.svg.brush()
	.x(x2).y(y2)
	.on("brush", brush_func)
	;

	var brush_svg = context.append("g")
	.attr("class", "brush")
	.call(brush)
	;

	function redraw() {
		x.domain(brush.empty() ? x2.domain() : brush.extent().map(function(d) {return d[0];}));
		y.domain(brush.empty() ? y2.domain() : brush.extent().map(function(d) {return d[1];}));
		layer.attr("d", function(d) { return area(d.values); });
		setNumMonthsPerTick(xAxis, x.domain(), width);
		yAxis.ticks(d3.min([nTicksY, Math.ceil(y.domain()[1] - y.domain()[0])])); // avoid yAxis ticks to go subdecimal
		focus.select(".x.axis").call(xAxis);
		focus.select(".y.axis").call(yAxis);
	}
	function brush_func() {
		redraw();

		// recompute zoom
		var x_domain = x.domain();
		var y_domain = y.domain();
		var brush_len_x = x_domain[1] - x_domain[0];
		var brush_len_y = y_domain[1] - y_domain[0];
		var org_len_x = x2.domain()[1] - x2.domain()[0];
		var org_len_y = y2.domain()[1] - y2.domain()[0];
		var z_min = d3.max([brush_len_x/org_len_x, brush_len_y/org_len_y]); // don't zoom out until brush overflow
		var z_max = d3.min([x2(x_domain[1]) - x2(x_domain[0]), y2(y_domain[0]) - y2(y_domain[1])]); // don't zoom in until brush empty
		zoom = d3.behavior.zoom().x(x).y(y).scaleExtent([z_min, z_max]).on("zoom", zoom_func);
		layer.call(zoom);
		background.call(zoom);
	}

	var prev_scale = 1;
	var prev_translate = [0, 0];
	function zoom_func() {
		if (d3.event.scale == prev_scale && is_small_move(prev_translate, d3.event.translate)) {
			// for better responsiveness on firefox, don't update too often when drag distance is too small.
			zoom.translate(prev_translate);
			return;
		}

		// new domain of x, y have already been determined by d3.behavior. update brush accordingly.
		var x_domain = x.domain();
		var y_domain = y.domain();
		var brush_len_x = x_domain[1] - x_domain[0];
		var brush_len_y = y_domain[1] - y_domain[0];
		// make sure brush doesn't overflow
		var new_x0 = boundValue(+x_domain[0], brush_len_x, x2.domain().map(function(d) { return +d; })); // "+" to convert date to int as dates don't directly compare (don't use Date.parse() to convert to int since it rounds off hours).
		var new_y0 = boundValue(y_domain[0], brush_len_y, y2.domain());
		var new_x1 = new_x0 + brush_len_x;
		var new_y1 = new_y0 + brush_len_y;

		prev_scale = d3.event.scale;
		//prev_translate = d3.event.translate.map(function(d) { return d; }); // prev_translate = d3.event.translate will get the object reference which will track new d3.event.translate. 
		// adjust translate accordingly if brush is bounded.
		prev_translate = [d3.event.translate[0] - (x(new_x0) - x(x_domain[0])), d3.event.translate[1] - (y(new_y0) - y(y_domain[0]))];
		zoom.translate(prev_translate);

		if (give_same_range([new_x0, new_x1], x2.domain(), x2) && give_same_range([new_y0, new_y1], y2.domain(), y2)) {
			// brush same as x2/y2 = no zoom = no brush needed = force brush to empty
			new_x1 = new_x0; new_y1 = new_y0;
		}

		brush_svg.call(brush.extent([ [new_x0, new_y0], [new_x1, new_y1] ]));
		//brush_svg.call(brush.extent([ [x_domain[0], y_domain[0]], [x_domain[1], y_domain[1]] ])); // to see how the work we have done above makes the difference.
		redraw();
	}
}

//it seems canvasDiv must appear on the page before the script that calls draw_chart(). 
function draw_chart(canvasDiv, data_bottom, data_top, normalizer, startYear, startMonth, totalWidth, totalHeight, focusOnly, browseParams)
{
	if (data_top.length != data_bottom.length) {
		alert("Problem found in visualization. Please contact developers.");
		data_top.length = data_bottom.length = d3.min([data_top.length, data_bottom.length]);
	}

	var do_top_bottom = true;

	var nMonths = data_top.length;
	var max = normalizer>0 ? normalizer : d3.max(data_top.concat(data_bottom));
	var startDate = new Date(startYear, startMonth);
	var endDate = new Date(startYear, startMonth + nMonths - 1);
	var formatDate = d3.time.format("%b %Y");

	var contextHeight = focusOnly ? 18 : 100;
	var yAxisWidth = String(max).length*8 + 10;
	var margin = {top: 4, right: 4, bottom: contextHeight, left: yAxisWidth};
	var margin2 = {top: totalHeight-contextHeight+40, right: 4, bottom: 18, left: yAxisWidth};
	var width = totalWidth - margin.left - margin.right;
	var height = totalHeight - margin.top - margin.bottom;
	var height_half = do_top_bottom ? height/2 : height;
	var height2 = totalHeight - margin2.top - margin2.bottom;
	var height2_half = do_top_bottom ? height2/2 : height2;

	var x = d3.time.scale().range([0, width]).domain([startDate, endDate]);
	var x2 = d3.time.scale().range([0, width]).domain(x.domain());
	var y_top = d3.scale.sqrt().range([height_half, 0]).domain([0, max]);
	var y_bottom = d3.scale.sqrt().range([0, do_top_bottom ? height_half : -height_half]).domain([0, max]);
	var y2_top = d3.scale.sqrt().range([height2_half, 0]).domain(y_top.domain());
	var y2_bottom = d3.scale.sqrt().range([0, do_top_bottom ? height2_half : -height2_half]).domain(y_bottom.domain());

	var xAxis = d3.svg.axis().scale(x).orient("bottom");
	setNumMonthsPerTick(xAxis, x.domain(), width);
	var xAxis2 = d3.svg.axis().scale(x2).orient("bottom");
	setNumMonthsPerTick(xAxis2, x.domain(), width);
	var nTicksY = d3.min([max, Math.ceil(Math.sqrt(height_half / 8))]); // "8px" per Y tick
	var yAxis_top = d3.svg.axis().scale(y_top).orient("left").ticks(nTicksY);

	var svg = d3.select(canvasDiv).append("svg")
	.attr("width", totalWidth)
	.attr("height", totalHeight);

	svg.append("defs").append("clipPath")
	.attr("id", "clip")
	.append("rect")
	.attr("width", width)
	.attr("height", height);

	var focus = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

//	var text_outgoing = svg.append("text").text("→ " + d3.sum(data_top) + " outgoing");
//	text_outgoing.attr("transform", "rotate(-90)translate(" + -(height_half + text_outgoing.node().getBBox().width)/2 + ",10)");
//	var text_incoming = svg.append("text").text(d3.sum(data_bottom) + " incoming ←");
//	text_incoming.attr("transform", "rotate(-90)translate(" + -(height + height_half + text_incoming.node().getBBox().width)/2 + ",10)");

	var area_top = d3.svg.area()
	.interpolate("monotone")
	.x(function(d) { return x(new Date(startYear, startMonth+d)); })
	.y0(height_half)
	.y1(function(d) { return y_top(data_top[d]); });

	var area_bottom = d3.svg.area()
	.interpolate("monotone")
	.x(function(d) { return x(new Date(startYear, startMonth+d)); })
	.y0(height_half)
	.y1(function(d) { return height_half+y_bottom(data_bottom[d]); });

	var path_top = focus.append("path")
	.datum(d3.range(nMonths))
	.attr("class", "top_path")
	.attr("clip-path", "url(#clip)")
	.attr("d", area_top);

	var path_bottom = focus.append("path")
	.datum(d3.range(nMonths))
	.attr("class", "bottom_path")
	.attr("clip-path", "url(#clip)")
	.attr("d", area_bottom);

	focus.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height + ")")
	.call(xAxis);

	focus.append("g")
	.attr("class", "y axis")
	.call(yAxis_top);

	if (do_top_bottom) {
		var yAxis_bottom = d3.svg.axis().scale(y_bottom).orient("left").ticks(nTicksY);
		focus.append("g")
		.attr("class", "y axis")
		.attr("transform", "translate(0, " + (height_half) + ")")
		.call(yAxis_bottom);
	}

//	focus.append("line")
//	.attr("class", "axis")
//	.attr("x1", 0)
//	.attr("x2", width)
//	.attr("y1", height_half)
//	.attr("y2", height_half);

	var brush = null;

	var circle_top = svg.append("circle").attr("fill", "red");
	var text_top = svg.append("text");
	var circle_bottom = svg.append("circle").attr("fill", "red");
	var text_bottom = svg.append("text");

	var prev_px = -1;

	svg.on("mouseout", function() {
		circle_top.attr("r",0); text_top.text("");
		circle_bottom.attr("r",0); text_bottom.text("");
		prev_px = -1;
	});

	var get_idx = function(px) {
		//var domain = (brush == null || brush.empty()) ? x2.domain() : brush.extent();
		//var date = (+domain[0]) + (domain[1] - domain[0]) * (px - margin.left) / width;
		var date = x.invert(px - margin.left);
		var idx = Math.round((date - startDate) * (data_bottom.length-1) / (endDate - startDate));
		return idx;
	};

	var font_height = 12;
	var x_range = [margin.left, margin.left+width];
	var y_range_top = [margin.top+font_height, margin.top+height_half];
	var y_range_bottom = do_top_bottom ? [margin.top+height_half+font_height, margin.top+height] : y_range_top;

	svg.on("mousemove", function() {
		var idx = get_idx(d3.mouse(this)[0]);
		//var px = Math.floor(idx * width / (data_bottom.length-1)) + margin.left;
		var date = new Date(startYear, startMonth+idx);
		var px = Math.floor(x(date)) + margin.left;
		if (px == prev_px || px < x_range[0] || px > x_range[1]) return;
		prev_px = px;
		var py_top = Math.floor(y_top(data_top[idx]) + margin.top);
		var py_bottom = Math.floor(y_bottom(data_bottom[idx]) + margin.top) + height_half;
		var date_str = " in " + formatDate(date);
		circle_top.attr("r", 3).attr("cx", px).attr("cy", py_top);
		setText(text_top, px+5, py_top-font_height/2, muse.pluralize(data_top[idx],"message") + date_str, x_range, y_range_top);
		circle_bottom.attr("r", 3).attr("cx", px).attr("cy", py_bottom);
		setText(text_bottom, px+5, py_bottom+font_height, muse.pluralize(data_bottom[idx],"message") + date_str, x_range, y_range_bottom);
	});

    if (focusOnly == true) return;

    // clicking on small focusOnly chart already brings up prettyPhoto zoom. don't want to bring up another browse window on same click.
	svg.on("click", function() {
		var px = prev_px;
		if (px < x_range[0] || px > x_range[1]) return;
		var py = d3.mouse(this)[1];
		var py_top = circle_top.attr("cy");
		var py_bottom = circle_bottom.attr("cy");
		if (py < py_top || py > py_bottom) return;
		var idx = get_idx(px);
		var date = new Date(startYear, startMonth+idx);
		if (typeof browseParams == "function") {
			browseParams(date.getFullYear(), date.getMonth());
		} else {
			window.open('/muse/browse?' + browseParams + '&startYear=' + date.getFullYear() + '&startMonth=' + (date.getMonth()+1));
		}
	});

	brush = d3.svg.brush().x(x2).on("brush", function() {
		x.domain(brush.empty() ? x2.domain() : brush.extent());
		path_top.attr("d", area_top);
		path_bottom.attr("d", area_bottom);
		setNumMonthsPerTick(xAxis, x.domain(), width);
		focus.select(".x.axis").call(xAxis);
	});

    var context = svg.append("g").attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");

	var area2_top = d3.svg.area()
	.interpolate("monotone")
	.x(function(d) { return x2(new Date(startYear, startMonth+d)); })
	.y0(height2_half)
	.y1(function(d) { return y2_top(data_top[d]); });

	var area2_bottom = d3.svg.area()
	.interpolate("monotone")
	.x(function(d) { return x2(new Date(startYear, startMonth+d)); })
	.y0(height2_half)
	.y1(function(d) { return height2_half+y2_bottom(data_bottom[d]); });

	context.append("path")
	.datum(d3.range(nMonths))
	.attr("class", "top_path")
	.attr("d", area2_top);

	context.append("path")
	.datum(d3.range(nMonths))
	.attr("class", "bottom_path")
	.attr("d", area2_bottom);

	context.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height2 + ")")
	.call(xAxis2);

	context.append("g")
	.attr("class", "x brush")
	.call(brush)
	.selectAll("rect")
	.attr("y", -6) // size of the brush cursor
	.attr("height", height2 + 7);

//	context.append("line")
//	.attr("class", "axis")
//	.attr("x1", 0)
//	.attr("x2", width)
//	.attr("y1", height2_half)
//	.attr("y2", height2_half);
}

// adjust pos+length to remain in the allowed range (inclusive on both ends)
function boundValue(pos, length, range)
{
	//range = range.sort();
	return (pos < range[0]) ? range[0] : (pos+length > range[1]) ? range[1]-length : pos;
}

function setText(text, x, y, str, x_range, y_range)
{
	text.text(str);
	var bbox = text.node().getBBox();
	x = boundValue(x, bbox.width, x_range);
	y = boundValue(y, bbox.height, y_range);
	text.attr("x", x).attr("y", y);
}

function getNumMonths(startDate, endDate)
{
	return (endDate.getFullYear() - startDate.getFullYear()) * 12 +
		   (endDate.getMonth() - startDate.getMonth());
}

function setNumMonthsPerTick(axis, focusRange, axisLength)
{
	var nMonthsFocus = getNumMonths(focusRange[0], focusRange[1]);
	var nTicks = axisLength / 80; // 80px per tick
	var result = Math.ceil(nMonthsFocus / nTicks);
	if (result >= 12) {
		axis.ticks(d3.time.years, Math.floor(result/12)); // place ticks only at year boundary
	} else {
		axis.ticks(d3.time.months, 12 / Math.floor(12/result)); // place ticks to nicely divide a year
	}
}

function is_small_move(p1, p2) {
	for (var i = 0; i < p1.length; i++) {
		if (Math.abs(p1[i] - p2[i]) > 20) return false;
	}
	return true;
}

function same_pixel(i, j) {
	return (Math.abs(i-j) < 0.01);
}

function give_same_range(d1, d2, d2r) {
	for (var i = 0; i < d1.length; i++) {
		if (!same_pixel(d2r(d1[i]), d2r(d2[i]))) return false;
	}
	return true;
}

// it seems canvasDiv must appear on the page before the script that calls draw_count_chart(). 
// data is array of { name, histogram }
function draw_count_chart(canvasDiv, data, startYear, startMonth, totalWidth, totalHeight)
{
	var nMonths = data[0].histogram.length;
	var max = d3.max(data.map(function(d) { return d3.max(d.histogram); }));
	var startDate = new Date(startYear, startMonth);
	var endDate = new Date(startYear, startMonth + nMonths - 1);
	var formatDate = d3.time.format("%b %Y");

	var focusOnly = true;

	var contextHeight = focusOnly ? 18 : 100;
	var yAxisWidth = String(max).length*8 + 10;
	var margin = {top: 4, right: 4, bottom: contextHeight, left: yAxisWidth};
	var margin2 = {top: totalHeight-contextHeight+40, right: 4, bottom: 18, left: yAxisWidth};
	var width = totalWidth - margin.left - margin.right;
	var height = totalHeight - margin.top - margin.bottom;
	var height2 = totalHeight - margin2.top - margin2.bottom;

	var x = d3.time.scale().range([0, width]).domain([startDate, endDate]);
	var x2 = d3.time.scale().range([0, width]).domain(x.domain());
	var y = d3.scale.sqrt().range([height, 0]).domain([0, max]);
	var y2 = d3.scale.sqrt().range([height2, 0]).domain(y.domain());

	var xAxis = d3.svg.axis().scale(x).orient("bottom");
	setNumMonthsPerTick(xAxis, x.domain(), width);
	var xAxis2 = d3.svg.axis().scale(x2).orient("bottom");
	setNumMonthsPerTick(xAxis2, x.domain(), width);
	var nTicksY = d3.min([max, Math.ceil(Math.sqrt(height / 20))]); // "20px" per Y tick
	var yAxis = d3.svg.axis().scale(y).orient("left").ticks(nTicksY);

	var svg = d3.select(canvasDiv).append("svg")
	.attr("width", totalWidth)
	.attr("height", totalHeight);

	svg.append("defs").append("clipPath")
	.attr("id", "clip")
	.append("rect")
	.attr("width", width)
	.attr("height", height);

	var focus = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

	var lines = data.map(function(i) {
		var start_month = startMonth;
		while (i.histogram[0] == 0) { start_month++; i.histogram.shift(); }
		while (i.histogram[i.histogram.length-1] == 0) { i.histogram.pop(); }
		return d3.svg.line()
				.interpolate("monotone")
				.x(function(d) { return x(new Date(startYear, start_month+d)); })
//				.y0(height)
				.y(function(d) { return y(i.histogram[d]); });
	});

	var paths = lines.map(function(l, i) {
		return focus.append("path")
				.datum(d3.range(data[i].histogram.length))
				.attr("class", "path")
				.attr("clip-path", "url(#clip)")
				.attr("d", l);
	});

	focus.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height + ")")
	.call(xAxis);

	focus.append("g")
	.attr("class", "y axis")
	.call(yAxis);
}
