define([
	'jquery',
	"view/" + $g.theme + "/pagemap.res.js",
	"controller/PageController",
	"sslplus.util"
	],

function($, $p, ctlPage, $util) {

var PasswordLostController = function() {
	var that = this;
	var ie_ver = parseInt($.browser.version, 10);
	if (ie_ver === 6) {
		$.getScript("../lib/DD_belatedPNG.js");
		$.getScript("../lib/json2.js");

		$("body").addClass("ie6");
	}
	
	this.init = function() {

		$("#txt_id").on("keydown", function(e) {
			if(e.keyCode == 13) {
				e.stopPropagation();
				e.preventDefault();

				$("#btn_sendmail").click();
			}
			else {
				$("#area_id").removeClass("error");
				$("#area_id .help-block").hide();
			}
		})

		$("#btn_sendmail").on("click", function(e) {
			e.stopPropagation();
			e.preventDefault();

			if($("#txt_id").val() == "") {
				showInlineError("아이디를 입력하세요");
			}
			else {

				$.post("/reset/sendmail", $("#sendmail").serialize())
					.success(function(data) {
						try {
							var json = JSON.parse(data);
							if(json.result == "no_user") {
								showMsg(true, "존재하지 않는 사용자 아이디입니다.");
							}
							if(json.result == "null_login_name") {
								showMsg(true, "아이디를 입력하세요.");
							}
							else if(json.result == "no_config") {
								showMsg(true, "서버에서 메일을 보낼 수 없습니다. 관리자에게 문의하십시오.");	
							}
							else if(json.result == "no_email") {
								showMsg(true, $("#txt_id").val() + "님의 이메일이 등록되지 않아서 인증 메일을 보낼 수 없습니다.<br/>관리자에게 문의하십시오.");	
							}
							else if(json.result == "ok") {
								showMsg(false, $("#txt_id").val() + "님의 암호를 재설정할 수 있는 링크를 이메일로 전송하였습니다.");
							}
						}
						catch(e) {
							showMsg(true, "서버에서 메일을 보낼 수 없습니다. (JSON error)");
						}
					})
					.error(function(jqXHR, textStatus, errorThrown) {
						showMsg(true, "서버에서 메일을 보낼 수 없습니다. 관리자에게 문의하십시오.<br/>" + jqXHR.status + " " + errorThrown);
					});
			}
		});

		function showInlineError(msg) {
			$("#area_id").addClass("error");
			$("#area_id .help-block").text(msg).fadeIn();

			$("#txt_id").focus();
		}

		function showMsg(is_error, msg) {
			ctlPage.resizeModal(250, parent);

			$("#area_input").fadeOut();
			$("#area_msgbox").fadeIn();

			that.initMsgbox(msg, is_error);		
		}


	};

	this.initMsgbox = function(msg, is_error) {
		$("#msg").html(msg);

		parent.log(msg);

		$("#ok").on("click", function(e) {
			e.preventDefault();
			e.stopPropagation();

			ctlPage.closeModal();
		});

		var img;

		if(is_error == true) {
			img = $("<img>").attr("src", "/view/common/image/remove.png").prependTo("#msg");
		}
		else {
			img = $("<img>").attr("src", "/view/common/image/info.png").prependTo("#msg");
		}
		img.css("margin-right", "15px")
	}

	this.initReset = function() {
		$("input[type=password]").on("keydown", function(e) {
			$(this).parents(".control-group").removeClass("error")
			$(this).next().text("");
		});

		$("#do-reset").on("click", function(e) {
			e.stopPropagation();
			e.preventDefault();

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
			else {
				$("#resetform").submit();
			}
			

		});
	}
}

return new PasswordLostController();

});