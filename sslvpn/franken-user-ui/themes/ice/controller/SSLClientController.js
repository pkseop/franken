define([
	"jquery",
	"view/" + $g.theme + "/message.res.js",
],

function($, $l) {

var SSLClientController = function() {
	var that = this;
	var sslclient = null;
	

	this.init = function(_sslclient) {
		sslclient = _sslclient;
	}

	this.SetHostname = function() {
		if(window.location.hostname != "localhost") {
			sslclient.SetHostname(window.location.hostname);
		}
	}

	this.RequestVpnInfo = function(success_callback) {
		sslclient.RequestVPNInfo({
			success: function(response) {
				log("TSGSSLClient: RequestVPNInfo 성공");
				log(response);

				var json;

				try {
					json = JSON.parse(response);
				}
				catch(e) {
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
				
				if(!!json.page_title) {
					if(!(/^\s*$/.test(json.page_title))) {
						document.title = json.page_title;
					}
				}
			},
			failed: function(response) {
				log("TSGSSLClient: RequestVPNInfo 실패");
				log(response);

				alert("VPN 정보를 받아올 수 없습니다. (ActiveX error)");
			}
		});
	}

	this.RequestAppList = function(success_callback) {
		sslclient.RequestAppList({
			success: function(resp) {
				log(resp);
				log("[RequestAppList] success");

				var obj;
				try {
					obj = JSON.parse(resp);
				}
				catch(e) {
					alert("앱리스트를 받아올 수 없습니다. (JSON error)");
					return;
				}

				success_callback(obj);
			},
			failed: function() {
				alert("앱리스트를 받아올 수 없습니다. (ActiveX error)");
			}
		})
	}

	this.RequestUserInfo = function(success_callback) {
		sslclient.RequestUserInfo({
			success: function(resp) {
				log(resp);
				log("[RequestUserInfo] success");

				var obj;

				try {
					obj = JSON.parse(resp);
				}
				catch(e) {
					alert("사용자 정보를 받아올 수 없습니다. (JSON error)");
					return;
				}	

				success_callback(obj);
			},
			failed: function() {
				alert("사용자 정보를 받아올 수 없습니다. (ActiveX error)")
			}
		})
	}

	this.StartNativeApp = function(idx) {
		sslclient.StartNativeApp(idx);
	}

	this.RequestInternalHttps = function(type, url, data, timeout, fn) {
		sslclient.RequestInternalHttps(type, url, data, timeout, fn);
	}

	this.LaunchPrivCertCenter = function() {
		sslclient.LaunchPrivCertCenter();
	}

}

return new SSLClientController();

});