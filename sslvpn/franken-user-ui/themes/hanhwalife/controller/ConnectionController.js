define([
	"jquery",
	"view/" + $g.theme + "/message.res.js",
],

function ($, $l) {

    var ConnectionController = function () {
        var that = this;
        var AXM = {
            AXM_NOT_IMPLEMENTED: 8001,
            AXM_NO_HOSTNAME: 8002,
            AXM_VPN_SESSION_INIT_FAILED: 8003,
            AXM_INTERNAL_ERROR: 8004,
            AXM_LOGIN_USER_CANCELLED: 8005,
            AXM_CONN_DISCONNECTED: 8100,
            AXM_CONN_TERMINATED: 8101,
            AXM_CONN_IDLETIME_EXPIRED: 8102
        };
        this.AXM = AXM;

        var sslclient = null;
        var logoutCallback;


        this.init = function (_sslclient, _logoutCallback) {
            sslclient = _sslclient;
            logoutCallback = _logoutCallback;
        }

        this.CancelLogin = function () {
            sslclient.CancelLogin();
            log("[CancelLogin]");
            $(window).off();

            if (timer != null) {
                clearTimeout(timer);
                timer = null;
            }
        };

        this.ActivexTrayEnabled = function () {
            sslclient.ActivexTrayEnabled();
            log("[ActivexTrayEnabled]");
        };

        this.PwdReset_HanHwaLife = function () {
            sslclient.PwdReset_HanHwaLife();
            log("[PwdReset_HanHwaLife]");
        };

        this.Logout = function () {
            log("[Logout]");
            that.CancelLogin();
            logoutCallback();
        }

        this.Logout_KillallIe = function () {
            log("[Logout_KillallIe]");
            sslclient.KillAllIe();
            logoutCallback();
        }

        this.ConfirmLogout = function () {
            var result = confirm("정말 로그아웃 하시겠습니까?");
            //var result = confirm("로그아웃 하면 VPN 연결이 중지되며, 현재 열려있는 모든 Internet Explore 창이 종료됩니다. 계속 하시겠습니까?");

            if (result) {
                this.Logout();
            }
        };

        this.RequestAuthMethod = function (login_name, custom_callback) {

            var callback = $.extend({
                success: function (response) { },
                failed: function (textStatus, errorThrown) {
                    log("[LoginHelper.RequestAuthMethod] failed, " + textStatus + ", " + errorThrown);
                }
            }, custom_callback);

            log('[LoginHelper.RequestAuthMethod] request auth info');

            setTimeout(function () {
                $.ajax("/external/authmethod?login_name=" + login_name, {
                    success: function success(data) {
                        var response;
                        //var data = '{"force_password_change":false,"password_fail_limit":5,"encryptions":"SEED","auth_method":6,"use_auto_reconnect":false,"password_resetable":true,"internal_ip":"192.168.0.1"}';
                        log(data);

                        try {
                            response = JSON.parse(data);
                        }
                        catch (err) {
                            callback.failed('error', 'invalid json');
                        }

                        callback.success(response);
                    },
                    error: function error(jqXHR, textStatus, errorThrown) {
                        callback.failed(textStatus, errorThrown);
                    }
                });
            }, 400);
        }

        this.RequestLogin = function (auth_method_json, id, pw, callback, otp,idn) {
            //that.CancelLogin();

            var jsontext = JSON.stringify(auth_method_json);
            log("[SetAuthMethod]")
            log(jsontext);
            try {
                sslclient.SetAuthMethod(jsontext);
            }
            catch (e) {
                callback.setAuthMethodFailed();
                return;
            }


            sslclient.SetConnCallback(function (msg, wparam, lparam) {
                log("[RequestLogin] ConnCallback, " + msg + ", " + wparam + ", " + lparam);

                if (msg == AXM.AXM_CONN_DISCONNECTED) {
                    that.Logout();
                }
                else if (msg == AXM.AXM_CONN_TERMINATED) {
                    $(".modal").remove();
                    $(".modal-backdrop").remove();

                    if (lparam == 0) {
                        alert($l.ConnTerminatedForced);
                    }
                    else if (lparam == 2) {
                        alert($l.ConnTerminatedUnstable);
						that.CancelLogin();
                        location.reload();
                        return;
                    }
                    that.Logout();
                }
            });

            log("[RequestLogin] " + auth_method_json.auth_method + ", " + id);

            try {
                sslclient.SetOtp(otp);
                sslclient.SetIdnInfo(idn);
            }
            catch (e) {
            }


            var doLogin = sslclient.RequestLogin(auth_method_json.auth_method, id, pw, callback);
            return doLogin;
        }

        var timer;

        this.pollIdleTimeout = function (idle_timeout, interval) {
            if (timer != null) {
                clearTimeout(timer);
                timer = null;
            }

            if (idle_timeout === 0) {
                return;
            }

            timer = setInterval(function () {
                var itime = sslclient.getUserInputIdleTime();

                if (idle_timeout < itime) {
                    that.Logout();
                    alert($l.IdleTimeout);
                }

                if (interval != null) {
                    var remains = idle_timeout - itime;
                    interval(remains);
                }

            }, 1000);
        }

        this.savedPassword = function (pw) {
            if (!window.__t3mpw0rd && !!pw) {
                window.__t3mpw0rd = pw;
            }
            else {
                var ret = parent.window.__t3mpw0rd;
                try {
                    delete parent.window.__t3mpw0rd;
                }
                catch (e) {
                    parent.window["__t3mpw0rd"] = undefined;
                }
                return ret;
            }

        }
    }

    return new ConnectionController();

})