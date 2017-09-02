define(["jquery"], function ($) {

    var ActiveXController = function () {
        var that = this;
        var isSetupCompleted = false;
        var isCallbackFired = false;
        var callback;

        this.ssl = null;
        this.sslclient = null;


        this.init = function (elid) {
            that.ssl = document.getElementById(elid);
        }

        // attach event for ActiveX
        this.attachEventSSL = function (elid) {
            var ssl = document.getElementById(elid);

            if (ssl.attachEvent) {
                ssl.attachEvent("FireNoticeMessage", function () {
                    log("TSGSSL: FireNoticeMessage");
                });

                ssl.attachEvent("OnLogMessage", function (msg) {
                    log("TSGSSL: " + msg);
                });

                ssl.attachEvent("OnSetupCompleted", function (error_code) {
                    log("OnSetupCompleted");
                    OnSetupCompleted(error_code);
                });
            }
        }

        var attachEventSSLClient = function () {
            var sslclient = that.sslclient;

            sslclient.attachEvent("FireNoticeMessage", function () {
                log("TSGSSLClient: FireNoticeMessage");

                if (!isCallbackFired && isSetupCompleted) {
                    log("TSGSSLClient 로딩 완료: FireNoticeMessage");
                    callback.success();
                    isCallbackFired = true;
                }
            });

            that.sslclient.attachEvent("OnLogMessage", function (msg) {
                log("TSGSSLClient: " + msg);
            });
        }

        this.checkInstallation = function (_callback) {
            callback = _callback;
            var host = document.domain;
            //var setupFileUrl = "/" + host + "/setup_" + host + ".exe";
			var setupFileUrl = "/" + host + "/setup_" + host + ".exe?version=2";
            that.ssl.DownloadAndSetup(setupFileUrl);
        }

        var OnSetupCompleted = function (err_code) {
            var sslclient = that.sslclient;
            var isLoaded = false;
            isSetupCompleted = true;
            // 0 : 최신버전인 상태 
            // -1 : 리프레시가 필요한 상태
            if (err_code == 0) {
                try {
                    var sslclient = $('<object classid="clsid:DEC74742-F01C-4F8F-9C6C-9CB8BB8F262A" id="TSGSSLClient"></object>').appendTo("#axs");

                    if (logger.status() === true) {
                        sslclient.width(15).height(15);
                    }
                    else {
                        sslclient.width(0).height(0);
                    }

                    that.sslclient = sslclient.get(0);
                    attachEventSSLClient();
                    that.sslclient.IsLoaded();

                    log("TSGSSLClient 버전: " + that.sslclient.getrevnum());

                    if (!isCallbackFired) {
                        log("TSGSSLClient의 로딩 완료: OnSetupCompleted");
                        isLoaded = true;
                    }
                }
                catch (e) {
                    // 5초를 기다려서 TSGSSLClient의 fireNoticeMessage가 오지 않으면, 로딩이 안될것으로 간주하고 페이지를 새로고침하도록 유도.
                    setTimeout(function () {

                        if (!isCallbackFired) {
                            log("TSGSSLClient의 로딩 실패");
                            callback.failed();
                            isCallbackFired = true;
                        }

                    }, 5000);
                }
                finally {
                    if (isLoaded) {
                        callback.success();
                        isCallbackFired = true;
                    }
                }
            }
            else if (err_code == -1) {
                location.reload();
            }
            else {
                log("unexpected error code from OnSetupCompleted")
            }
        }

    }

    return new ActiveXController();

})