define(["jquery"], function($) {
	var logger = (function() {
		return {
			on: function() {
				$("#log").show();
				$("#log").css("bottom", "0px");

				$("#TSGSSL").width(15).height(15);
				$("#TSGSSLClient").width(15).height(15);
			},
			off: function() {
				$("#log").hide();

				$("#TSGSSL").width(0).height(0);
				$("#TSGSSLClient").width(0).height(0);
			},
			status: function() {
				return !$("#log").is(":hidden");
			}
		}
	})();

	function log(str) {
		str = "<span style='color:blue'>[" + (new Date()).getISODateString() + "]</span> " + str;
		$("<p>" + str + "</p>")
			.hide()
			.insertBefore("#log p:first-child")
			.fadeIn('slow');
	}

	function secondsToTime(secs)
	{
		var hours = Math.floor(secs / (60 * 60));

		var divisor_for_minutes = secs % (60 * 60);
		var minutes = Math.floor(divisor_for_minutes / 60);

		var divisor_for_seconds = divisor_for_minutes % 60;
		var seconds = Math.ceil(divisor_for_seconds);

		var obj = {
			"h": pad(hours),
			"m": pad(minutes),
			"s": pad(seconds)
		};
		return obj;
	}

	function pad(n) { return n < 10 ? '0' + n : n }

	Date.prototype.getISODateString = function() {

		return this.getFullYear() + '/'
			+ pad(this.getMonth() + 1) + '/'
			+ pad(this.getDate()) + ' '
			+ pad(this.getHours()) + ':'
			+ pad(this.getMinutes()) + ':'
			+ pad(this.getSeconds()) + ' '
			+ pad(this.getMilliseconds());
	}

	String.format = function() {
		var s = arguments[0];
		for (var i = 0; i < arguments.length - 1; i++) {
			var reg = new RegExp("\\{" + i + "\\}", "gm");
			s = s.replace(reg, arguments[i + 1]);
		}

		return s;
	}
	
	String.prototype.isMatch = function(matchpoint) {
		var has = false;
		var result = this.match(matchpoint);
		
		if(result != null) {
			if(result.constructor == Array) {
				if(result.length > 0) {
					has = true;
				}
				else {
					has = false;
				}
			}
			else {
				has = false;
			}
		}
		
		return has;
	};

	function checkValidPassword(val) {
		var has = val.isMatch(/[0-9]+/);
		has = (has && val.isMatch(/[a-zA-Z]+/));
		has = (has && val.isMatch(/[^0-9a-zA-Z]+/));
		has = (has && !(val.length < 9));
		
		return has;
	}
	
	$.urlParam = function(name) {
		var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
		return results[1] || 0;
	}

	$.easing['jswing'] = $.easing['swing'];

	$.extend( $.easing,
	{
		def: 'easeOutQuart',
		swing: function (x, t, b, c, d) {
			//alert($.easing.default);
			return $.easing[$.easing.def](x, t, b, c, d);
		},
		easeOutQuart: function (x, t, b, c, d) {
			return -c * ((t=t/d-1)*t*t*t - 1) + b;
		}
	});

	window.logger = logger;
	window.looger = logger;
	window.log = log;

	return {
		logger: logger,
		log: log,
		secondsToTime: secondsToTime,
		checkValidPassword: checkValidPassword
	}
})