define([
	'jquery',
	"view/" + $g.theme + "/pagemap.res.js",
	"controller/PageController",
	"controller/SSLClientController.js",
	"controller/ConnectionController.js",
	"sslplus.util",
	"view/" + $g.theme + "/message.res.js"
	],

function ($, $p, ctlPage, ctlSsl, ctlConn, $util, $l) {

    var PasswordResetController = function () {
        var that = this;

        this.showChange = function (login_name) {

            if ($g.showChangeUI) {
                $g.showChangeUI();
                initUI(null, true, login_name);
                $("#area_old").show();
            }
            else {
                ctlPage.openModal("pwchange.html?type=change&login_name=" + login_name, 450);
            }
        }

        this.forceChange = function (login_name) {
            ctlPage.openModal("pwchange.html?type=force&login_name=" + login_name, 400, null, {
                showCloseButton: false
            });
        }

        this.initChange = function (login_name) {
            $("#change_msg").html(String.format($l.ChangePassword, login_name));
            initUI(null, true, login_name);
            $("#area_old").show();
        }

        this.initForceChange = function (login_name) {
            $("#change_msg").html(String.format($l.ChangePasswordForce, login_name));
            initUI(null, true, login_name);

            $("#change h4").text("암호 설정");
            $("#password_old").val(ctlConn.savedPassword());

            $("#password_new").focus();
        }

        this.hideChange = function () {
            if ($g.hideChangeUI) {
                $g.hideChangeUI();
                $(".top_notice").remove();
            }
            else {
                ctlPage.closeModal();
                $(".top_notice").remove();
            }
        }

        this.showExpired = function (login_name, old_password) {
            $("#change_msg").html(String.format($l.ChangePasswordExpired, login_name));
            initUI(null, false, login_name);

            $("#password_old").val(old_password);
        }

        function initUI(targetwin, is_internal, login_name) {
            if (targetwin == null) targetwin = window;

            var $ = targetwin.$;

            $("#do_change").off("click");
            $("input[type=password]").off("keydown");
            $("input[type=password]").val("");
            $("input[type=hidden]#login_name").val(login_name);

            $("form").on("keydown", function (e) {
                if (e.keyCode === 27) {
                    e.preventDefault();
                    e.stopPropagation();
                }
            });


            $("#do_change").on("click", function (e) {
                e.stopPropagation();
                e.preventDefault();

                var pwold = $("#password_old").val();
                var pw = $("#password_new").val();
                var pwre = $("#password_re").val();

                if (pw == "") {
                    $("#area_new .help-block").text("암호를 입력하세요").fadeIn();
                    $("#area_new").addClass("error");
                    $("#area_new input").focus();
                }
                else if (pwre == "") {
                    $("#area_re .help-block").text("암호를 입력하세요").fadeIn();
                    $("#area_re").addClass("error");
                    $("#area_re input").focus();
                }
                else if (pw !== pwre) {
                    $("#area_re .help-block").text("두 암호가 서로 다릅니다.").fadeIn();
                    $("#area_re").addClass("error");
                    $("#area_re input").focus();
                }
                else if (!$util.checkValidPassword(pw)) {
                    $("#area_new .help-block").text("암호는 9자 이상의 영문, 숫자, 특수문자 조합으로 만들어야 합니다.").fadeIn();
                    $("#area_new").addClass("error");
                    $("#area_new input").focus();
                }
                else if (pwold === pw) {
                    $("#area_new .help-block").text("새 암호가 이전 암호와 같을 수 없습니다.").fadeIn();
                    $("#area_new").addClass("error");
                    $("#area_new input").focus();
                }
                else {

                    if (is_internal) {
                        ctlSsl.RequestInternalHttps("POST", "/reset/change", $("#change").serialize(), 5, function (status, body, header) {
                            log(status);
                            if (status === 200) {
                                log(body);
                                try {
                                    var json = JSON.parse(body);
                                    if (json.result == "success") {
                                        alert("암호를 변경하였습니다.");

                                        that.hideChange();
                                    }
                                    else if (json.result == "old_password_not_match") {
                                        $("#area_old .help-block").text("기존 암호가 틀립니다.").fadeIn();
                                        $("#area_old").addClass("error");
                                        $("#area_old input").focus();
                                    }
                                }
                                catch (e) {
                                    alert("JSON error (" + e + ")\n암호 변경에 실패하였습니다.");
                                }
                            }
                            else {
                                log(status + " Client Error");
                                alert("클라이언트 에러 (" + status + ")\n암호 변경에 실패하였습니다.");
                            }
                        });
                    }
                    else {
                        $.post("/reset/change", $("#change").serialize(), function (json) {
                            parent.log(JSON.stringify(json));

                            if (json.result == "success") {
                                alert("암호를 재설정하였습니다.");

                                //parent.$("input[type=password]").val("");
                                parent.$("#btn_another").click();
                                ctlPage.closeModal();

                            }
                            else if (json.result == "old_password_not_match") {
                                alert("기존 암호가 틀립니다.");

                                ctlPage.closeModal();

                                //parent.$("input[type=password]").focus();
                            }
                        })
					.error(function (jqXHR, textStatus, errorThrown) {
					    parent.log(jqXHR.status + " " + errorThrown);
					    alert(jqXHR.status + " " + errorThrown + "\n암호 재설정에 실패하였습니다.");
					});
                    }
                }
            });

            $("input[type=password]").on("keydown", function (e) {
                var ie_ver = window.ie_ver;
                if (ie_ver == undefined) ie_ver = parent.ie_ver;

                if (e.keyCode == 13 && ie_ver < 8) {
                    $("#do_change").click();
                }
                else {
                    $(this).parents(".control-group").removeClass("error")
                    $(this).next().text("");
                }
            });

            $("#area_old input[type=password]").focus();
        }

    }

    return new PasswordResetController();

});