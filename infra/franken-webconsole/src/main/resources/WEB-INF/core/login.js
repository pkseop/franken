define(['/core/connection.js','/lib/sha256.js'], function(socket, CryptoJS) {



function doLogin(login_name, password, loginCallback) {
	socket.send("kr.co.future.dom.msgbus.LoginPlugin.hello", {}, function(m,b,c) {

		var nonce = m.body.nonce;
		var hashedpwd = CryptoJS.SHA256(password).toString(CryptoJS.enc.Hex);
		var hash = CryptoJS.SHA256(hashedpwd + nonce).toString(CryptoJS.enc.Hex);

		socket.send("kr.co.future.dom.msgbus.LoginPlugin.login", {
			"nick": login_name,
			"hash": hash,
			"force": false
		}, loginCallback);
	});
}

return {
	"doLogin": doLogin
}

});