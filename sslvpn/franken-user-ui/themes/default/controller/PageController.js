define([
	'jquery',
	"view/" + $g.theme + "/pagemap.res.js",
	"controller/ConnectionController.js",
	"sslplus.util"
	],

function ($, $p, ctlConn, $util) {

    var PageController = function () {
        if (getURLvars("logger") == "true") {
            $util.logger.on();
        }

        var that = this;
        var jqxhr = null;
        var ssl = document.getElementById("TSGSSL");

        var how_many_css_loaded = 0;

        this.loadCss = function (id, url, callback) {
            var link = document.createElement("link");
            $(link).attr("data-id", id);
            link.type = "text/css";
            link.rel = "stylesheet";
            link.href = url;
            link.onload = function () {
                if (callback == false) {
                }
                else {
                    how_many_css_loaded++;

                    if (callback) callback(url);
                }
            }

            document.getElementsByTagName("head")[0].appendChild(link);
        }

        this.html = function (html) {
            $("#pageview").empty().html(html);
        }

        this.load = function (url, callback) {
            if (jqxhr) jqxhr.abort();

            jqxhr = $.get(url, function (data) {
                $("#pageview").html(data);
            })
        }

        this.preload = function (url, callback) {
            //if(jqxhr) jqxhr.abort();

            jqxhr = $.get(url, function (data) {
                callback(data);
            });
        }

        this.loadNotice = function () {

            jqxhr = $.get("external/notice", function (data) {
                $(".s_notice_content").html(data);
            });

        }

        this.preloadStatic = function (key, callback) {
            var dir = "view/" + $g.theme + "/";
            var _s = $p[key];

            if (jqxhr) jqxhr.abort();

            function OnCompleted(data) {
                var stylez = [];

                $.each(_s.css, function (i, cssobj) {
                    if ($("link[data-id=" + cssobj.id + "]").length === 0) {
                        $.get(dir + cssobj.url, function (css) {
                            stylez.push(css);
                        });
                    }
                });

                if (ie_ver === 6) {
                    $.each(_s.cssie6, function (i, cssobj) {
                        $.get(dir + cssobj.url, function (css) {
                            stylez.push(css);
                        });
                    });
                }

                if (callback != null) {
                    if (_s.js != null) {
                        require(["../../" + _s.js], function (pageCtl) {
                            var pageCxt = {
                                load: function (load_callback) {
                                    $("#pageview").empty().html(data);

                                    $.each(stylez, function (i, csstxt) {
                                        $("<style>").text(csstxt).prependTo("#pageview");
                                    });

                                    pageCtl.init();
                                    if (load_callback != null) {
                                        load_callback();
                                    }
                                },
                                ctx: pageCtl
                            }
                            callback(pageCxt);
                        });
                    }
                    else {
                        callback();
                    }
                }

            }

            function xhrcall() {
                jqxhr = $.ajax({
                    url: dir + _s.html,
                    timeout: 10000
                })
				.done(OnCompleted)
				.error(function (jqXHR, textStatus, errorThrown) {
				    retry_count++;

				    if (retry_count > 2) {
				        window._jqXHR = jqXHR;

				        if (jqXHR.status === 0 && jqXHR.readyState === 0) {
				            alert("웹페이지 연결 상태에 문제가 발생했습니다");
				            $(window).off();
				            ctlConn.CancelLogin();

				            location.reload();

				        }
				    }
				    else {
				        xhrcall();
				    }

				});
            }

            xhrcall();
        }

        this.loadStatic = function (key, callback) {
            var dir = "view/" + $g.theme + "/";
            var _s = $p[key];

            if (jqxhr) jqxhr.abort();

            function OnCompleted(data) {

                $.each(_s.css, function (i, cssobj) {

                    if ($("link[data-id=" + cssobj.id + "]").length > 0) {
                        how_many_css_loaded++;

                        if (how_many_css_loaded === _s.css.length) {
                            cssLoaded(dir + cssobj.url);
                        }
                    }
                    else {
                        that.loadCss(cssobj.id, dir + cssobj.url, cssLoaded);
                        if ($.browser.webkit) {
                            setTimeout(function () {
                                how_many_css_loaded++;
                                cssLoaded(dir + cssobj.url);
                            }, 500);
                        }
                    }

                });

                if (ie_ver === 6) {
                    $.each(_s.cssie6, function (i, cssobj) {
                        that.loadCss(cssobj.id, dir + cssobj.url, false);
                    });
                }

                function cssLoaded(url) {
                    log("cssload:" + how_many_css_loaded + "/" + _s.css.length);

                    $("#pageview").empty();

                    if (how_many_css_loaded === _s.css.length) {
                        log("css loaded!");

                        $("#pageview").html(data);

                        if (_s.onload != null) {
                            _s.onload();
                        }

                        if (callback != null) {
                            if (_s.js != null) {
                                require(["../../" + _s.js], function (pageCtl) {
                                    pageCtl.init();
                                    callback(pageCtl);

                                    $("form").on("keydown", function (e) {
                                        if (e.keyCode === 27) {
                                            e.preventDefault();
                                            e.stopPropagation();
                                        }
                                    });
                                });
                            }
                            else {
                                callback();
                            }
                        }


                        how_many_css_loaded = 0;

                        if ($("#pageview").text() == "") {
                            //alert(data);
                            $("#pageview").html(data);
                        }
                    }
                }

            }

            var retry_count = 0;

            function xhrcall() {
                jqxhr = $.ajax({
                    url: dir + _s.html,
                    timeout: 10000
                })
				.done(OnCompleted)
				.error(function (jqXHR, textStatus, errorThrown) {
				    retry_count++;

				    if (retry_count > 2) {
				        window._jqXHR = jqXHR;

				        if (jqXHR.status === 0 && jqXHR.readyState === 0) {
				            alert("웹페이지 연결 상태에 문제가 발생했습니다");
				            $(window).off();
				            ctlConn.CancelLogin();

				            location.reload();
				        }
				    }
				    else {
				        xhrcall();
				    }

				});
            }

            xhrcall();
        }

        function vis(el, bool) {
            if (bool == true) {
                $(el).show();
            }
        }

        this.linkClientDownload = function () {
            //$(".s_cldn").hide();
            $.get("/external/global_config", function (data) {
                try {
                    log(data);
                    var obj = JSON.parse(data).global_config;
                    //obj.show_manual_download = true;

                    vis(".s_cldn_cont", obj.show_manual_download);
                    vis(".s_cldn.ios", obj.show_ios_client_download);
                    vis(".s_cldn.windows", obj.show_windows_client_download);
                    vis(".s_cldn.android", obj.show_android_client_download);
                    vis(".s_cldn.linux", obj.show_linux_client_download);
                }
                catch (e) {
                    $(".s_cldn").show();
                }
            })
			.error(function () {
			    $(".s_cldn").show();
			})
			.complete(function () {
			    $(".s_cldn.windows").on("click", function () {

			        var host = document.domain;
			        try {
			                location.href = "/winsetup_" + document.domain + ".exe";
			        }
			        catch (e) {
			            location.href = "/setup_" + document.domain + ".exe?version=2";
			        }
			    });

			    $(".s_cldn.linux").on("click", function () {
			        location.href = "/spclient-1.0.1-201111.i386.tgz";
			        //location.href = "https://" + document.domain + ":7010/spclient-1.0.1-201111.i386.tgz";
			    });

			    $(".s_cldn.ios").on("click", function () {
			        location.href = "itms-services://?action=download-manifest&url=https://" + document.location.host + "/ios/SSLplus.plist";
			    });

			    $(".s_cldn.android").on("click", function () {
			        location.href = "/SSLplus.apk";
			        //location.href = "https://" + document.domain + ":7010/SSLplus.apk";
			    });

			    $(".s_cldn_cont").on("click", function () {
			        //location.href = "/sslplus.pdf";
					window.open("/sslplus.pdf");
			    });
			})
        }

        this.openModal = function (url, height, callback, options) {
            var showCloseButton = true;
            if (options != null) {
                if (options.hasOwnProperty("showCloseButton")) {
                    showCloseButton = options.showCloseButton;
                }
            }

            $("<div>").addClass("modal-backdrop").appendTo("body");
            var modal = $("<div>").addClass("modal")
								.height(height)
								.appendTo("body");

            var btnClose = $("<button>").addClass("modal-close")
						.on("click", function () {
						    return that.closeModal();
						});

            if (!showCloseButton) {
                btnClose.hide();
            }
            btnClose.appendTo(modal);

            var ifrm = $("<iframe>").addClass("modal-iframe")
						.attr("frameborder", "0")
						.attr("allowTransparency", "true")
						.attr("src", url)
						.appendTo(modal);

            var ifrmwin = ifrm.get(0).contentWindow;
            ifrm.load(function () {
                if (callback != null)
                    callback(ifrmwin);
            });

            return ifrmwin;
        }


        this.resizeModal = function (height, window_host) {
            if (window_host == undefined) {
                window_host = window;
            }

            window_host.$(".modal").animate({ "height": height + "px" }, 500, "easeOutQuart");
        }

        this.closeModal = function () {
            var $ = parent.$;
            if ($ == undefined) { $ = parent.parent.$; }

            $(".modal").remove();
            $(".modal-backdrop").remove();
        }

        this.referer = function () {
            return window.location.pathname;
        }
    };

    function getURLvars(key) {
        var hash, vars = [];
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');

        for (var i = 0; i < hashes.length; i++) {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }

        if (key != null) {
            return vars[key];
        }
        else {
            return vars;
        }
    }

    return new PageController();
})