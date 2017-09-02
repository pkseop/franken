require.config({
	baseUrl: ".",
	paths: {
		"jquery": "lib/jquery-1.8.0"
	}
});

require([
	"controller/PageController.js",
	"controller/ActiveXController.js",
	"controller/SSLClientController.js",
	"controller/ConnectionController.js",
	"controller/PasswordLostController.js",
	"controller/PasswordResetController.js",
	"view/" + $g.theme + "/message.res.js",
	"view/" + $g.theme + "/global.custom.js",
	"sslplus.util"
	],

function(ctlPage, ctlAx, ctlSsl, ctlConn, ctlLost, ctlReset, $l) {
	window.$ = $;
	window.ie_ver = parseInt($.browser.version, 10);
	if (ie_ver === 6) {
		$.getScript("../lib/DD_belatedPNG.js");
		$.getScript("../lib/json2.js");

		$("body").addClass("ie6");
	}
	else if (ie_ver === 7) {
		$.getScript("../lib/json2.js");
		$("body").addClass("ie7");
	}

	$.getJSON("/external/vpninfo", function(json) { 
		if(!!json.page_title) {
			if(!(/^\s*$/.test(json.page_title))) {
				document.title = json.page_title;
			}
		}
	});

	if(ctlPage.referer() === "/") {

		function checkInstall() {
			ctlAx.attachEventSSL("TSGSSL");

			try {
				ctlAx.ssl.IsLoaded();
				log("TSGSSL 로딩 성공");

				ctlAx.checkInstallation({
					success: function() {
						
						log("TSGSSLClient의 로딩 완료");
						ctlConn.init(ctlAx.sslclient, function() {
							ctlPage.loadStatic("LoginStart", function($p) { });
						});
						ctlSsl.init(ctlAx.sslclient);

						ctlPage.loadStatic("LoginStart", function($p) { });
					},
					failed: function() {
						log("TSGSSLClient 로딩 실패, 또는 try 구문 에러");
						//alert($l.TSGSSLClientLoadingFailed);
					}
				});
			}
			catch (e) {
				log("TSGSSL 로딩 실패. 클린한 상태 또는 TSGSSL ActiveX 에러");
				//alert($l.TSGSSLLoadingFailed);

				setTimeout(checkInstall, 5000);
			}
		}

		ctlAx.init("TSGSSL");

		if($.browser.msie) {
			
			ctlPage.loadStatic("CheckInstallation", function() {
				ctlPage.linkClientDownload();
				//return;

				checkInstall();
			});
		}
		else {
			log("IE가 아님");

			ctlPage.loadStatic("NonIE", function() {
				ctlPage.loadNotice();
				ctlPage.linkClientDownload();

				$.get("/external/notice_image").error(function() {
					$g.noNoticeImage = true;
					$(".s_notice_image").addClass("notice_fallback");
				});
				
			});
		}

	}
	else if (ctlPage.referer() === "/pwlost.html") {

		ctlLost.init();

	}
	else if (ctlPage.referer() === "/msgbox.html") {

		var title = decodeURIComponent($.urlParam("title"));
		var msg = decodeURIComponent($.urlParam("msg"));
		var is_error = $.urlParam("is_error");
		
		ctlLost.initMsgbox(title, msg, is_error);

	}
	else if (ctlPage.referer() === "/pwchange.html") {
		ctlConn.init(ctlAx.sslclient, function() {
			ctlPage.loadStatic("LoginStart", function($p) { });
		});
		ctlSsl.init(parent.TSGSSLClient);

		var login_name = decodeURIComponent($.urlParam("login_name"));
		var type = $.urlParam("type");

		if(type == "change") {
			ctlReset.initChange(login_name);
		}
		else if(type == "force") {
			ctlReset.initForceChange(login_name);
		}
		else if(type == "expired") {
			ctlReset.showExpired(login_name, parent.$("#txt_password").val());
		}

	}
	else if (ctlPage.referer() === "/reset/reset.html") {

		ctlLost.initReset();

	}
	else {
		alert("referer error");
	}

});