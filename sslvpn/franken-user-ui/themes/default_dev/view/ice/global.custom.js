$g.showMessageBox = function(selector, msg) {
	alert(msg);

}

$g.showChangeUI = function() {
	$("#cont_applist").hide();
	$("#cont_change").show();
}


$g.hideChangeUI = function() {
	$("#cont_applist").show();
	$("#cont_change").hide();
}

$g.renderAppInfo = function(o, startfn) {
	var li = $("<li>");
	var a = $("<a>").appendTo(li);

	var exe_path = $("<span>").addClass("underline").addClass("path").appendTo(a);
	//var img = $("<img>").appendTo(a);

	//var typeimg = $("<img>").addClass("typeimg");

	if(o.app_type == "WebApp") {
		a.attr("href", o.app_data.url)
		 .attr("target", "_blank");

		//typeimg.attr("src", "/view/" + $g.theme + "/image/app_web.png");
		exe_path.text(o.app_data.url);
	}
	else {
		a.attr("href", "#")
		 .click(function() {
		 	startfn();
		 })

		//typeimg.attr("src", "/view/" + $g.theme + "/image/app_windows.png");
		//exe_path.text(o.app_data.exe_path);
	}

	//img.attr("src", "data:image/png;base64," + o.icon);

	$("<span>" + o.name + "</span>").addClass("underline").appendTo(a);
	//typeimg.appendTo(a);

	li.on({
		mouseover: function() { 
			$(this).addClass("hover");
		},
		mouseout: function() { 
			$(this).removeClass("hover");
		}
	});

	return li;
}