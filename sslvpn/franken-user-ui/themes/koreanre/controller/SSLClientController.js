define([
	"jquery",
	"view/" + $g.theme + "/message.res.js",
],

function ($, $l) {

    var SSLClientController = function () {
        var that = this;
        var sslclient = null;


        this.init = function (_sslclient) {
            sslclient = _sslclient;
        }

        this.SetHostname = function () {
            if (window.location.hostname != "localhost") {
            sslclient.SetHostname(window.location.hostname);
            }
        }

        this.RequestVpnInfo = function (success_callback) {
            sslclient.RequestVPNInfo({
                success: function (response) {
                    log("TSGSSLClient: RequestVPNInfo 성공");
                    log(response);

                    var json;

                    try {
                        json = JSON.parse(response);
                    }
                    catch (e) {
                        alert("VPN 정보를 받아올 수 없습니다. (JSON error)");
                        return;
                    }

                    var defaultValue = {
                        "auth_method": 0,
                        "id_label": json.id_label,
                        "encryptions": "SEED",
                        "internal_ip": json.internal_ip
                    };

                    json = $.extend(defaultValue, json);
                    success_callback(json);

                    if (!!json.page_title) {
                        if (!(/^\s*$/.test(json.page_title))) {
                            document.title = json.page_title;
                        }
                    }
                },
                failed: function (response) {
                    log("TSGSSLClient: RequestVPNInfo 실패");
                    log(response);

                    alert("VPN 정보를 받아올 수 없습니다. (ActiveX error)");
                }
            });
        }

        this.RequestAppList = function (success_callback) {
            sslclient.RequestAppList({
                success: function (resp) {
                    log(resp);
                    log("[RequestAppList] success");

                    var obj;
                    try {
                        obj = JSON.parse(resp);
                    }
                    catch (e) {
                        alert("앱리스트를 받아올 수 없습니다. (JSON error)");
                        return;
                    }

                    success_callback(obj);
                },
                failed: function () {
                    alert("앱리스트를 받아올 수 없습니다. (ActiveX error)");
                }
            })
        }

        this.RequestUserInfo = function (success_callback) {
            sslclient.RequestUserInfo({
                success: function (resp) {
                    log(resp);
                    log("[RequestUserInfo] success");

                    var obj;

                    try {
                        obj = JSON.parse(resp);
                    }
                    catch (e) {
                        alert("사용자 정보를 받아올 수 없습니다. (JSON error)");
                        return;
                    }

                    success_callback(obj);
                },
                failed: function () {
                    alert("사용자 정보를 받아올 수 없습니다. (ActiveX error)")
                }
            })
        }

        this.SingleSignOn = function () {

            var sData = sslclient.GetSingleSignOnInfo();
            var obj = JSON.parse(sData);
            var token = obj.token;
            //var url = obj.url;

            function isEmpty(s) {
                var pt = /[\S]/;
                return !pt.test(s);
            }

            if (!isEmpty(token)) {

                try {
                    function fn_getServerUrl() {
                        var strChar = '/install/';
                        //var strServerUrl = window.location.href;
                        var strServerUrl = "http://solomon.koreanre.co.kr:8080/KoreanreWeb/install/";
                        strServerUrl = strServerUrl.substr(0, strServerUrl.indexOf(strChar));

                        return strServerUrl;
                    }
                    var XLauncher = document.getElementById("XLauncher");

                    var strServerUrl = fn_getServerUrl();
                    XLauncher.key = "KOREANRE";
                    XLauncher.componentpath = "%COMPONENT%\\koreanre\\";
                    XLauncher.loadingimage = strServerUrl + "/install/images/loading_koreanre.gif";
                    XLauncher.xadl = strServerUrl + "/xp/koreanre.xadl";
                    XLauncher.globalvalue = "gv_lang:g=ko-kr,gv_encryptToken:g=" + token;
                    XLauncher.onlyone = true;
					XLauncher.enginesetupkey = "{C70C2CBA-C8CE-4269-9C8D-69424C2E3156}_is1";
                    XLauncher.launch();
                }
                catch (e) {
                    //window.open(obj.url);
                }
            }
        }

        this.StartNativeApp = function (idx) {
            sslclient.StartNativeApp(idx);
        }

        this.RequestInternalHttps = function (type, url, data, timeout, fn) {
            sslclient.RequestInternalHttps(type, url, data, timeout, fn);
        }

        this.LaunchPrivCertCenter = function () {
            sslclient.LaunchPrivCertCenter();
        }

    }

    return new SSLClientController();

});