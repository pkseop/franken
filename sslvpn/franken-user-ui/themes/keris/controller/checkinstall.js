define([
	
], 

function() {

	var init = function() {
		$.get("/external/notice_image").error(function() {
			$g.noNoticeImage = true;
			$(".s_notice_image").addClass("notice_fallback");
		});


		$.get("/external/top_image").error(function() {
			$g.noTopImage = true;
			$(".s_top_image").addClass("top_fallback");
		});
	}

	return {
		init: init
	};

});