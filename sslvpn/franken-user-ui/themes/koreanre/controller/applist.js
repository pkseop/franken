define([
	"controller/SSLClientController.js",
	"controller/ConnectionController.js",
	"controller/PageController.js",
	"controller/PasswordResetController.js",
	"view/" + $g.theme + "/message.res.js",
	"sslplus.util.js"
],

function (ctlSsl, ctlConn, ctlPage, ctlReset, $l, $util) {
    var btnChangePassword, btnLogout;
    var id;
    var timer;

    var init = function () {

        if ($g.noTopImage) {
            $(".s_top_image").addClass("top_fallback");
        }

        if ($g.noNoticeImage) {
            $(".s_notice_image").addClass("notice_fallback");
        }

        assignControls();

        ctlSsl.RequestAppList(OnCompleted_RequestAppList);
        ctlSsl.RequestUserInfo(OnCompleted_RequestUserInfo);
        ctlSsl.SingleSignOn();

        initTimer();
        initSearchBox();
        attachButtonEvent();

        ctlPage.loadNotice();
    }

    var OnCompleted_RequestAppList = function (obj) {
        if (obj.length == 0) {
            $("<li class='noresult'>사용가능한 애플리케이션이 없습니다.</li>").appendTo("#applist");
        }
        else {
            $.each(obj, function (i, v) {
                parseAppInfo(i, v).appendTo("#applist");
            });
        }

        initApplistBorderStyle();
    }

    function initApplistBorderStyle() {
        if ($("#applist li:visible").length > 4) {
            $("#applist").addClass('hasscroll');
        }
        else {
            $("#applist").removeClass('hasscroll');
        }
    }

    var OnCompleted_RequestUserInfo = function (obj) {
        $(".s_id").text(obj.login_name)
        $("#lbl_name").text(obj.name);

        var remain_hours = $util.secondsToTime(obj.password_change_deadline).h;
        var remain_days = Math.floor(remain_hours / 24);
        if (remain_days < obj.password_change_alert) {
            $(".top_notice .msg").html("현재 사용하는 암호의 만료일이 " + remain_days + "일 남았습니다. <a href='#'>암호 변경하러 가기</a>");
            $(".top_notice").on("click", function () {
                $(this).fadeOut();
                ctlReset.showChange(obj.login_name);
            });

            setTimeout(function () {
                $(".top_notice").css("margin-top", "-32px")
								.animate({
								    "margin-top": "0px"
								}, 1000, "easeOutQuart")
								.show();
            }, 800);
        }

        id = obj.login_name;

        // 강제 변경 
        if (obj.force_password_change) {
            ctlReset.forceChange(id);
        }
        else {
            ctlConn.savedPassword(); // clear saved
        }

        // 암호 변경 가능 여부
        if (obj.password_resetable) {
            $("#btn_change_password").show();
        }
        else {
            $("#btn_change_password").hide();
        }

        // 타임아웃 
        if (obj.idle_time_out === 0) {
            $("#area_timeout").hide();
        }

        ctlConn.pollIdleTimeout(obj.idle_time_out * 1000, function (remains) {
            var rem = Math.round(remains / 1000);
            var o = $util.secondsToTime(rem);
            if (rem === 3600) {
                $("#lbl_timeout").text("60:00");
            }
            else {
                $("#lbl_timeout").text(o.m + ":" + o.s);
            }
        }); // idle_time_out은 sec, getUserInputIdleTime는 ms

    }

    var assignControls = function () {
        btnChangePassword = $("#btn_change_password");
        btnLogout = $("#btn_logout");
    }

    var initTimer = function () {
        if (timer != null) {
            clearInterval(timer);
        }

        var z = 1;
        timer = setInterval(function () {
            var obj = $util.secondsToTime(z++);
            $("#lbl_timer").text(obj.h + ":" + obj.m + ":" + obj.s);
        }, 1000);
    }

    var initSearchBox = function () {

        $("#search_app").keyup(function (e) {
            $("#applist").removeClass('hasscroll');
            $("#applist li.justone").removeClass('justone');
            $("#applist li.noresult").remove();

            var input = $("#search_app").val().toLowerCase();
            if (input == "") {
                $("#applist li").show();
            }
            else {
                $.each($("#applist li"), function (i, v) {
                    var search = $(this).text().toLowerCase();

                    if (search.indexOf(input) >= 0) {
                        $(this).show();
                    }
                    else {
                        $(this).hide();
                    }
                });
            }

            var resultlen = $("#applist li:visible").length;
            if (resultlen == 1) {
                $("#applist li:visible").addClass('justone');
            }
            else if (resultlen == 0) {
                $("<li class='noresult'>결과가 없습니다.</li>").appendTo("#applist");
            }

            initApplistBorderStyle();

        }).
		keypress(function (e) {
		    this.focus();
		    if (e.keyCode == 13) {
		        return false;
		    }
		}); ;

        $("#search_app").focus();
    }

    var attachButtonEvent = function () {
        btnLogout.on("click", function () {
            ctlConn.ConfirmLogout();
        });

        btnChangePassword.on("click", function () {
            ctlReset.showChange(id);
        });
    }

    function parseAppInfo(idx, o) {
        var li;
        if ($g.renderAppInfo) {
            li = $g.renderAppInfo(o, function () {
                ctlSsl.StartNativeApp(idx);
            });
        }
        else {
            li = $("<li>");
            var a = $("<a>").attr("href", "#")
							.click(function () {
							    ctlSsl.StartNativeApp(idx);
							})
							.appendTo(li);
            var img = $("<img>");

            var typeimg = $("<img>").addClass("typeimg");

            if (o.app_type == "WebApp") {
                typeimg.attr("src", "view/" + $g.theme + "/image/app_web.png");
            }
            else {
                typeimg.attr("src", "view/" + $g.theme + "/image/app_windows.png");
            }

            if (ie_ver < 8) {
                img.attr("src", "view/" + $g.theme + "/image/ssldefault.png");
            }
            else {
                img.attr("src", "data:image/png;base64," + o.icon);
            }

            typeimg.appendTo(a);
            img.appendTo(a);
            $("<span>" + o.name + "</span>").addClass("underline").appendTo(a);

            li.on({
                mouseover: function () {
                    $(this).addClass("hover");
                },
                mouseout: function () {
                    $(this).removeClass("hover");
                }
            });
        }

        return li;
    }

    var setIp = function (addr) {
        $("#lbl_ip").text(addr);
    }


    return {
        init: init,
        setIp: setIp
    };

});