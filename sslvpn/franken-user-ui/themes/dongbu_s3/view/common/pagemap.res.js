define([], function() {
	return {
		"CheckInstallation": {
			"html": "page/checkinstall.html",
			"js": "controller/checkinstall.js",
			"css": [
				{
					"id": "bootstrap",
					"url": $g.bootstrap
				},
				{
					"id": "design",
					"url": "css/common.css"
				}
			],
			"cssie6": [
				{
					"id": "design",
					"url": "css/common_ie6.css"
				}
			],
			"onload": function() {
				
			}
		},
		"LoginStart": {
			"html": "page/login_start.html",
			"js": "controller/login_start.js",
			"css": [
				{
					"id": "bootstrap",
					"url": $g.bootstrap
				},
				{
					"id": "design",
					"url": "css/common.css"
				}
			],
			"cssie6": [
				{
					"id": "design",
					"url": "css/common_ie6.css"
				}
			],
			"onload": function() {

				$("#btn_cldn").on({
					click: function() {
						$(".s_area_cldn").animate({
							top: '-10'
						}, 1000, 'easeOutQuart');

						$(this).fadeOut();
					}
				});

				$(".s_area_cldn .close").on({
					click: function() {
						$(".s_area_cldn").animate({
							top: '-150'
						}, 1000, 'easeOutQuart');

						$("#btn_cldn").fadeIn();
					}
				});

				if(ie_ver === 6) {
					setTimeout(function() {
						DD_belatedPNG.fix('button');
						DD_belatedPNG.fix('.s_top_image');
						DD_belatedPNG.fix('.top_fallback');
						DD_belatedPNG.fix('.seperator');
					}, 100);
				}
			}
		},
		"Applist": {
			"html": "page/applist.html",
			"js": "controller/applist.js",
			"css": [
				{
					"id": "bootstrap",
					"url": $g.bootstrap
				},
				{
					"id": "design",
					"url": "css/common.css"
				}
			],
			"cssie6": [
				{
					"id": "design",
					"url": "css/common_ie6.css"
				}
			],
			"onload": function() {
				if(ie_ver === 6) {
					setTimeout(function() {
						DD_belatedPNG.fix('button');
						DD_belatedPNG.fix('.s_top_image');
						DD_belatedPNG.fix('.top_fallback');
						DD_belatedPNG.fix('.seperator');
					}, 100);
				}
			}
		},
		"NonIE": {
			"html": "page/nonie.html",
			"js": null,
			"css": [
				{
					"id": "bootstrap",
					"url": $g.bootstrap
				},
				{
					"id": "design",
					"url": "css/common.css"
				}
			],
			"cssie6": [
				
			],
			"onload": function() {

			}
		}
	}
});