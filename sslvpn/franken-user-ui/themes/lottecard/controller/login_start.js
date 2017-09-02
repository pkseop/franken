define([
	"controller/SSLClientController.js",
	"controller/ConnectionController.js",
	"controller/PageController.js",
	"view/" + $g.theme + "/message.res.js"
],

function (ctlSsl, ctlConn, ctlPage, $l) {
    var auth_method_json = {};

    var areaId, areaConfirm, areaPassword, areaError;
    var txtId, txtPassword, lblConfirm, lblError, btnLoginStart, btnLogin, btnAnother, btnLostPassword;

    var htmlLoading, htmlVersion;
    var ctxApplist;

    var assignControls = function () {
        areaId = $("div#area_id");
        areaConfirm = $("div#area_confirm");
        areaPassword = $("div#area_password");
        areaError = $("div#area_error");

        txtId = $("input[type=text]#txt_id");
        txtPassword = $("input[type=password]#txt_password");

        lblConfirm = $("span.help-block#lbl_confirm");
        lblError = $("span.help-block#lbl_error");

        btnLoginStart = $("button#btn_login_start")
        btnLogin = $("button#btn_login");
        btnAnother = $("button#btn_another");
        btnLostPassword = $("button#btn_lost_password");
        btnCertCenter = $("button#btn_cert_center");

        btnVersion = $(".s_version");
    }

    var init = function () {
        log("init loginstart");

        ctlConn.CancelLogin();

        // assign controls
        assignControls();

        // view notice
		btnVersion.hide();

        // attach event
        attachEventToButtons();

        ctlSsl.SetHostname();

        // request vpn info

        setTimeout(function () {
            ctlSsl.RequestVpnInfo(function (resp) {

                auth_method_json.encryptions = resp.encryptions;
                auth_method_json.auth_method = resp.auth_method;
                auth_method_json.internal_ip = resp.internal_ip;

                // success
                if (resp.auth_method === 1 || resp.auth_method === 3 || resp.auth_method === 5) {

                    if (resp.encryptions != "") {

                        log("no need to request auth_method");

                        drawLoginUIby();

                        txtId.focus();
                        btnAnother.hide();
                        btnLostPassword.show();
                    }
                }

                log(resp.id_label);
                if (resp.id_label != null) {
                    if (!(/^\s*$/.test(resp.id_label))) {
                        $("label[for=txt_id]").text(resp.id_label);
                    }
                }
            });
        }, 100);

        txtId.focus();

        /*
        ctlPage.preloadStatic("Applist", function(ctx) {
        ctxApplist = ctx;
        });
        */

        ctlPage.preload("view/" + $g.theme + "/page/loading.html", function (data) {
            htmlLoading = data;
        });

        ctlPage.preload("view/" + $g.theme + "/page/version.html", function (data) {
            htmlVersion = data;
        })

        ctlPage.loadNotice();


        if ($g.noTopImage) {
            $(".s_top_image").addClass("top_fallback");
        }

        if ($g.noNoticeImage) {
            $(".s_notice_image").addClass("notice_fallback");
        }

        ctlPage.linkClientDownload();


    }

    var attachEventToButtons = function () {
        btnLoginStart.on("click", btnLoginStart_click);
        btnAnother.on("click", btnAnother_click);
        btnLogin.on("click", btnLogin_click);
        btnLostPassword.on("click", btnLostPassword_click);

        txtId.on("keydown", txtId_keydown);
        txtPassword.on("keydown", txtPassword_keydown);

        btnCertCenter.on("click", btnCertCenter_click);

        btnVersion.on("click", btnVersion_click);
    }

    var btnVersion_click = function () {
        ctlPage.openModal("about:blank", 240, function () {
            $(".modal-iframe").remove();
            var verdiv = $("<div>").html(htmlVersion).appendTo(".modal");

            verdiv.find("#lblverTSGSSL").text(TSGSSL.GetRevNum());
            verdiv.find("#lblverTSGSSLClient").text(TSGSSLClient.GetRevNum());

            $.get("/version.js.txt", function (data) {

                verdiv.find("#lblverWebLogin").text(data.substring(8));
            });

            $("#btnVersionWinClose").on("click", function () {
                ctlPage.closeModal();
            });
        });
    }

    var btnCertCenter_click = function (e) {
        ctlSsl.LaunchPrivCertCenter();
    }

    var txtId_keydown = function (e) {
        if (e.keyCode == 13 && ie_ver < 9) {
            e.stopPropagation();
            e.preventDefault();

            if (btnLoginStart.is(":hidden")) {
                btnLogin.click();
            }
            else {
                btnLoginStart.click();
            }
        }

    }

    var txtPassword_keydown = function (e) {
        if (e.keyCode == 13 && ie_ver < 9) {
            e.stopPropagation();
            e.preventDefault();

            btnLogin.click();
        }

    }

    var btnLostPassword_click = function (e) {
        ctlPage.openModal("pwlost.html", 350);
    }

    var btnLoginStart_click = function (e) {
        e.stopPropagation();
        e.preventDefault();

        txtId.prop("disabled", true);
        btnLoginStart.prop("disabled", true);

        // ID가 비었을 때
        if (!validateLoginName(txtId.val())) return;

        ctlConn.RequestAuthMethod(txtId.val(), {
            success: function (resp) {
                //auth_method_json.auth_method = resp.auth_method;

                auth_method_json = $.extend(auth_method_json, resp);
                log("auth_method_override: " + JSON.stringify(auth_method_json));

                log("[RequestAuthMethod] success");

                drawLoginUIby();
                if (resp.password_resetable) {
                    btnLostPassword.fadeIn();
                }
            },
            failed: function (textStatus, errorThrown) {
                txtId.prop("disabled", false);

                if (errorThrown == "Not Found") {
                    $g.showMessageBox("#area_id", $l.NoLoginName);
                    txtId.select().focus();
                }
                else if (errorThrown == "Internal Server Error") {
                    $g.showMessageBox("#area_id", $l.RequireLoginName);
                    txtId.select().focus();
                }
                else {
                    $g.showMessageBox("#area_id", $l.UnknownError + "<br/>" + textStatus + "<br/>" + errorThrown);
                    txtId.select().focus();
                }

                btnLoginStart.prop("disabled", false);
            }
        })
    }


    var btnAnother_click = function (e) {
        e.stopPropagation();
        e.preventDefault();

        txtId.val("").prop("disabled", false).focus();
        txtPassword.val("");
        areaPassword.hide();

        btnAnother.hide();
        btnLogin.hide();
        btnLoginStart.fadeIn().prop("disabled", false);

        areaError.hide();

    }

    var btnLogin_click = function (e) {
        e.stopPropagation();
        e.preventDefault();

        var id = txtId.val();
        var pw = txtPassword.val();

        if (!validateLoginName(id)) return;

        log("[btnLogin_click] " + id);

        if (auth_method_json.auth_method == 2 || auth_method_json.auth_method == 4) {
            ctlConn.RequestLogin(auth_method_json, id, pw, RequestLogin_callback);
        }
        else {

            if (pw == undefined || pw == "") {
                log("password is blank");
                $g.showMessageBox('#area_password', $l.RequirePassword);

                txtPassword.focus();
                return;
            }

            txtPassword.prop("disabled", true);
            btnLogin.prop("disabled", true);
            btnAnother.prop("disabled", true);

            var doLogin = ctlConn.RequestLogin(auth_method_json, id, pw, RequestLogin_callback);
            ctlConn.savedPassword();
            ctlConn.savedPassword(pw);

            if (!doLogin) {
                txtPassword.prop("disabled", false);
                btnLogin.prop("disabled", false);
                btnAnother.prop("disabled", false);

                txtPassword.select().focus();
            }
        }
    }

    var RequestLogin_callback = {
        success: function () {
            log("[RequestLogin_callback] success");
        },
        canceled: function () {
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
        failed: function (code, errorJson) {
            log("[RequestLogin_callback] failed");
            log(errorJson);
            log(code);

            var error;

            try {
                error = JSON.parse(errorJson);
            }
            catch (e) {
                if (code >= 8000 && code < 9000) {
                    if (code == ctlConn.AXM.AXM_VPN_SESSION_INIT_FAILED) {
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

            if (error.authCode == 24 && error.authCode_debugmsg == "password-expired") {
                ctlPage.openModal("pwchange.html?type=expired&login_name=" + txtId.val(), 470);
            }

            if (error.authCode == 2 || error.authCode == 25) {
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
        loginSuccess: function () {
            log("[RequestLogin_callback] loginSuccess");

            $(window).on({
                beforeunload: function () {
                    return "이 페이지를 나가시겠습니까? 인터넷창 종료시 VPN연결이 유지되고, 작업표시줄의 SSLplus 아이콘에서 접속 종료할 수 있습니다."
                    //return "이 페이지를 나가면 로그아웃 되면서 SSLVPN 연결이 끊어집니다. 정말 이 페이지를 나가시겠습니까?"
                },
                unload: function () {
                    ctlConn.ActivexTrayEnabled();
                    //ctlConn.CancelLogin();
                }
            })

            //ctlPage.load("view/" + $g.theme + "/page/loading.html");
            ctlPage.html(htmlLoading);

            if ($g.noTopImage) {
                $(".s_top_image").addClass("top_fallback");
            }

            if ($g.noNoticeImage) {
                $(".s_notice_image").addClass("notice_fallback");
            }

            // 로그인만 성공. 아이피 받을 때 까지 대기해야 함. 
            // 로그인 성공. 남은 연결 과정을 진행중입니다. 
        },
        connCompleted: function (ip_addr, a, b, c) {
            log("[RequestLogin_callback] connCompleted");
            log("[RequestLogin_callback] ip address: " + ip_addr);
            log("[RequestLogin_callback] others: " + a + ", " + b + ", " + c);

            // 아이피를 다 받았음. 앱리스트 요청 가능 

            //로그인이 완료 되면 url 리다이렉션 창을 띄운다.
            function isEmpty(s) {
                var pt = /[\S]/;
                return !pt.test(s);
            }
            var url = auth_method_json.popup_url
            if(!isEmpty(url))
                window.open(url);
            ctlPage.loadStatic("Applist", function ($p) {
                $p.setIp(ip_addr);
            });

            /*
            ctxApplist.load(function() {
            ctxApplist.ctx.setIp(ip_addr);
            });
            */

        },
        mutexFailed: function () {
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
        setAuthMethodFailed: function () {
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

    var validateLoginName = function (login_name) {

        if (login_name == undefined || login_name == "") {
            $g.showMessageBox("#area_id", $l.RequireLoginName);

            btnLoginStart.prop("disabled", false);
            txtId.prop("disabled", false).focus();

            return false;
        }
        else {
            return true;
        }
    }

    var drawLoginUIby = function () {

        // password
        if (auth_method_json.auth_method === 1) {
            areaPassword.fadeIn();
            btnLoginStart.hide();
            btnLogin.fadeIn();
            btnAnother.fadeIn();
            txtPassword.focus();
        }
        // npki
        else if (auth_method_json.auth_method === 2 || auth_method_json.auth_method === 4) {

            var doLogin = ctlConn.RequestLogin(auth_method_json, txtId.val(), txtPassword.val(), RequestLogin_callback);
            if (!doLogin) {
                btnLoginStart.prop("disabled", false);
                txtId.prop("disabled", false).select().focus();
            }

        }
        // password + npki
        else if (auth_method_json.auth_method === 3 || auth_method_json.auth_method === 5) {
            areaPassword.fadeIn();
            btnLoginStart.hide();
            btnLogin.fadeIn();
            btnAnother.fadeIn();
            txtPassword.focus();
        }
        else {

        }



    }


    return {
        init: init
    };

})