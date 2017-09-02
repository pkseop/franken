$g.showMessageBox = function(selector, msg) {
	$(selector).addClass("error");

	var invalidbox = $(selector).find(".help-block");
	invalidbox.html(msg);
	invalidbox.fadeIn();

	var inputbox = $(selector).find('input');

	if(inputbox.length == 0) {
		var allinputbox = $("form input");

		allinputbox.on("keydown", function() {
			$("#area_error").find(".help-block").hide();
			$("#area_error").removeClass("error");
			$("#area_error").hide();
			
			allinputbox.off();
		});
	}
	else {
		inputbox.on("keydown", function() {
			invalidbox.hide();
			$(selector).removeClass("error");
			inputbox.off();
		});
	}

}