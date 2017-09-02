define(["controller/SSLClientController.js",
    	"controller/ConnectionController.js"
    	], 
function(ctlSsl, ctlConn) {
	
	return {
		"CheckInstallation": {
			"html": "../common/page/checkinstall.html",
			"js": "controller/checkinstall.js",
			"css": [
				{
					"id": "bootstrap",
					"url": $g.bootstrap
				},
				{
					"id": "common_design",
					"url": "../common/css/common.css"
				}
			],
			"cssie6": [
				{
					"id": "common_design",
					"url": "../common/css/common_ie6.css"
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
					"id": "design",
					"url": "css/vpn.css"
				},
				{
					"id": "design2",
					"url": "css/vpn2.css"
				}
			],
			"cssie6": [
				
			],
			"onload": function($p) {
				$("link[data-id=common_design]").remove();
				$("link[data-id=bootstrap]").remove();
				$("link[data-id=applist]").remove();

				if(ie_ver === 6) {
					setTimeout(function() {
						DD_belatedPNG.fix('button.modal-close');
					}, 100);
				}
				
				var ctlPage;
				require(["controller/PageController.js"], function(_ctlPage) {
					//console.log("loaded");
					ctlPage = _ctlPage;
				});
				
				var auth_method_json = {};
				var areaId, areaConfirm, areaPassword, areaError;
				var txtId, txtPassword, lblConfirm, lblError, btnLoginStart, btnLogin, btnAnother, btnLostPassword;

				var htmlLoading, htmlVersion;
				var ctxApplist;

				var assignControls = function() {
					areaId 			= $("div#area_id");
					areaConfirm 	= $("div#area_confirm");
					areaPassword 	= $("div#area_password");
					areaError 		= $("div#area_error");

					txtId 			= $("input[type=text]#txt_id");
					txtPassword 	= $("input[type=password]#txt_password");
					
					lblConfirm 		= $("span.help-block#lbl_confirm");
					lblError 		= $("span.help-block#lbl_error");

					btnLoginStart	= $("button#btn_login_start")
					btnLogin		= $("button#btn_login");
					btnAnother		= $("button#btn_another");
					btnLostPassword = $("button#btn_lost_password");
					btnCertCenter	= $("button#btn_cert_center");

					btnVersion		= $(".s_version");
				}
				
				// assign controls
				assignControls();
				
				ctlSsl.SetHostname();

				// request vpn info
				ctlSsl.RequestVpnInfo(function(resp) {					
					auth_method_json.encryptions = resp.encryptions;
					auth_method_json.auth_method = resp.auth_method;
					auth_method_json.internal_ip = resp.internal_ip;

					// success					
					if(resp.auth_method === 1 || resp.auth_method === 3 || resp.auth_method === 5) {

						if(resp.encryptions != "") {

							log("no need to request auth_method");
							
							drawLoginUIby();
							
							txtId.focus();
							btnAnother.hide();
							btnLostPassword.show();
						}
					}

					log(resp.id_label);
					if(resp.id_label != null) {
						if(!(/^\s*$/.test(resp.id_label))) {
							$("label[for=txt_id]").text(resp.id_label);
						}
					}					
				});
				
				ctlPage.preload("view/" + $g.theme + "/page/loading.html", function(data) {
					htmlLoading = data;
				});

				ctlPage.preload("view/" + $g.theme + "/page/version.html", function(data) {
					htmlVersion = data;
				})
				
				ctlPage.loadNotice();
				
				if($g.noTopImage) {
					$(".s_top_image").addClass("top_fallback");
				}

				if($g.noNoticeImage) {
					$(".s_notice_image").addClass("notice_fallback");
				}

				//ctlPage.linkClientDownload();				

				if($("iframe#ocxs").length === 0) {
					var ifrm = $("<iframe id='ocxs' style='border:0px solid;'>").width(0).height(0).appendTo("body");					
					ifrm.attr("src", "/view/ice/page/activex_frame.html");
					//console.log("iframe#ocxs: length zero "+ ifrm);
				} else {
					if(typeof ifrm == "undefined") {						
						ifrm = $("<iframe id='ocxs' style='border:0px solid;'>").width(0).height(0).appendTo("body");					
						ifrm.attr("src", "/view/ice/page/activex_frame.html");
						//console.log("iframe#ocxs: undefined"+ ifrm);
					}
				}

				$.ajax({
					type: 'GET',
					url: '/verify',
					data:"",
					success: function(response) {
						//console.log("verify servlet: " + response);
					}
				});
				
				function getCookieVal(cookieName) {
					var allCookies = document.cookie;
					var pos = allCookies.indexOf(cookieName+'=');
					if (pos == -1) return null;
					var valueStart = pos + (cookieName.length+1);
					var valueEnd = allCookies.indexOf(";",valueStart);
					if (valueEnd == -1) valueEnd = allCookies.length;
					var value = allCookies.substring(valueStart, valueEnd);
					value = unescape(value);
					if (value == "") {
						return null;
					} else {
						return value;
					}
				}
	
	
				$("#btn_login_sga").on("click",function(e) {
					e.stopPropagation();
					e.preventDefault();

					// SGA 모듈 결과값 가져오기
					var iframe = ifrm.get(0);
					iframe.contentWindow.SetupObjECT(true);
					
					var id = $("#txt_id").val();
					var pw = $("#txt_password").val();

					// id pw 가져오기
					//console.log("id: " + id);
					//console.log("pw: " + pw)
					
					// 세션 아이디
					var session_id = getCookieVal("JSESSIONID");
					//console.log("session id: "+session_id);
					
					// 서버인증서
					var certificate_env = "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";
					
					var dd = iframe.contentWindow.RequestSession(certificate_env, "3DES", session_id);
					//console.log(dd);
					
					$.ajax({
						type: 'POST',
						url: '/verify',
						data:"id=" + id + "&pw=" + pw + "&strSignedData=" + dd + "&session_id=" + session_id,
						success: function(response) {
							//console.log("success");
							//console.log(response);

							try {
								response = JSON.parse(response);
							} catch(err) {
								alert("올바르지 않은 형식의 응답입니다.");
							}
							
							if(!response.error === "true") {
								var response_id = response.id;
								var response_pw = response.pw;								
								ctlConn.RequestLogin(auth_method_json, response_id, response_pw, RequestLogin_callback);
							}
							else {
								//console.log("response message: " + response.msg);
							}							
						},
						failed: function(response) {
							//console.log("failed");
							//console.log(response);
						}
					});
					
					//////////////
					var RequestLogin_callback = {
						success: function() {
							//console.log("[RequestLogin_callback] success");
						},
						canceled: function() {
							log("[RequestLogin_callback] canceled");
							
							if (auth_method_json.auth_method === 2 || auth_method_json.auth_method === 4) {
								btnLoginStart.prop("disabled", false);
								txtId.prop("disabled", false).select().focus();
							}
							else { 
								txtPassword.prop("disabled", false);
								btnLogin.prop("disabled", false);
								btnAnother.prop("disabled", false);

								txtPassword.select().focus();
							}
						},
						failed: function(code, errorJson) {
							log("[RequestLogin_callback] failed");
							log(errorJson);
							log(code);

							var error;

							try {
								error = JSON.parse(errorJson);
							}
							catch(e) {
								if(code >= 8000 && code < 9000) {
									if(code == ctlConn.AXM.AXM_VPN_SESSION_INIT_FAILED) {
										alert("VPN 연결을 초기화할 수 없습니다. 드라이버가 제대로 설치되지 않았거나, 서비스가 중지되었을 수 있습니다. (8003)");
									}
									else {
										alert("기타 에러 (" + code + ")");
									}
								}
								else {
									alert("알수 없는 에러 (" + code + ")");
								}

								//btnAnother.click();
								return;
							}
							
							var msg = error.authCode_usermsg;

							if(error.authCode == 24 && error.authCode_debugmsg == "password-expired") {
								ctlPage.openModal("pwchange.html?type=expired&login_name=" + txtId.val(), 470);
							}
							
							if(error.authCode == 2 || error.authCode == 25) {
								msg = error.authCode_usermsg + " (암호 연속 실패: " + error.authOpt + "회)";
							}
							
							// npki
							if (auth_method_json.auth_method === 2 || auth_method_json.auth_method === 4) {
								btnLoginStart.prop("disabled", false);
								txtId.prop("disabled", false).select().focus();
							}
							else { 
								txtPassword.prop("disabled", false);
								btnLogin.prop("disabled", false);
								btnAnother.prop("disabled", false);

								txtPassword.select().focus();
							}
							//}

							areaError.show();
							$g.showMessageBox('#area_error', msg);
						},
						loginSuccess: function() {
							log("[RequestLogin_callback] loginSuccess");

							$(window).on({
								beforeunload: function() {
									return "이 페이지를 나가면 로그아웃 되면서 SSLVPN 연결이 끊어집니다. 정말 이 페이지를 나가시겠습니까?"
								},
								unload: function() {
									ctlConn.CancelLogin();
								}
							})
							
							
							//ctlPage.load("view/" + $g.theme + "/page/loading.html");
							ctlPage.html(htmlLoading);

							if($g.noTopImage) {
								$(".s_top_image").addClass("top_fallback");
							}

							if($g.noNoticeImage) {
								$(".s_notice_image").addClass("notice_fallback");
							}

							// 로그인만 성공. 아이피 받을 때 까지 대기해야 함. 
							// 로그인 성공. 남은 연결 과정을 진행중입니다. 
						},
						connCompleted: function(ip_addr, a, b, c) {
							log("[RequestLogin_callback] connCompleted");
							log("[RequestLogin_callback] ip address: " + ip_addr);
							log("[RequestLogin_callback] others: " + a + ", " + b + ", " + c);

							// 아이피를 다 받았음. 앱리스트 요청 가능 
							
							ctlPage.loadStatic("Applist", function($p) {
								$p.setIp(ip_addr);
							});
							
							/*
							ctxApplist.load(function() {
								ctxApplist.ctx.setIp(ip_addr);
							});
							*/
							
						},
						mutexFailed: function() {
							log("[OnCompleted_RequestLogin] mutexFailed");
							if (auth_method_json.auth_method === 2 || auth_method_json.auth_method === 4) {
								btnLoginStart.prop("disabled", false);
								txtId.prop("disabled", false).select().focus();
							}
							else { 
								txtPassword.prop("disabled", false);
								btnLogin.prop("disabled", false);
								btnAnother.prop("disabled", false);

								txtPassword.select().focus();
							}
							//}

							areaError.show();
							$g.showMessageBox('#area_error', $l.MutexFailed);
							// TODO: 다른 클라이언트가 로그인중일 때 (네트워크 성립 이전 타이밍 포함) 오는
							// 로그인 실패 콜백. 따로 에러코드를 할당하기 애매한 콜백이라 별도로 넣음.
						},
						setAuthMethodFailed: function() {
							log("[OnCompleted_RequestLogin] setAuthMethodFailed");
							if (auth_method_json.auth_method === 2 || auth_method_json.auth_method === 4) {
								btnLoginStart.prop("disabled", false);
								txtId.prop("disabled", false).select().focus();
							}
							else { 
								txtPassword.prop("disabled", false);
								btnLogin.prop("disabled", false);
								btnAnother.prop("disabled", false);

								txtPassword.select().focus();
							}
							//}

							areaError.show();
							$g.showMessageBox('#area_error', "모든 브라우저를 닫고 다시 시도해주세요.");
						}
					}					
					//////////////
				});
				
				
			}
		},
		"Applist": {
			"html": "page/applist.html",
			"js": "controller/applist.js",
			"css": [
				{
					"id": "applist",
					"url": "css/applist.css"
				}
			],
			"cssie6": [
				
			],
			"onload": function() {
				$("link[data-id=design]").remove();
				$("link[data-id=design2]").remove();

				if(ie_ver === 6) {
					setTimeout(function() {
						DD_belatedPNG.fix('.titlebanner');
						DD_belatedPNG.fix('.top_right img');
						DD_belatedPNG.fix('.bigtab');
						DD_belatedPNG.fix('.bigtab img');
					}, 100);
				}

				$("#btn_bookmark").on("click", function() {
					$("#cont_change").hide();
					$("#cont_applist").show();
				})	
			}
		},
		"NonIE": {
			"html": "../common/page/nonie.html",
			"js": null,
			"css": [
				{
					"id": "bootstrap",
					"url": $g.bootstrap
				},
				{
					"id": "design",
					"url": "../common/css/common.css"
				}
			],
			"cssie6": [
				
			],
			"onload": function() {
				
			}
		}
	}
});