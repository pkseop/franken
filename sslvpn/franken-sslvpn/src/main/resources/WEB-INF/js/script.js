
function doError() {
	$("#message").fadeIn();
	$("#license").addClass("gotError").focus().blur(function() {
		clearError();
	});
}

function clearError() {
	$("#license").removeClass();
	$("#message").hide();
}

$.get("/serial", function(resp) {
	$("#serial").val(resp);
}).error(function() {
	alert("Can't load Serial");
});

$("#submitButton").click(function() {
	$.ajax({
		url: "/activate",
		type: "POST",
		data: {
			"serial": $("#serial").val(),
			"license_key": $("#license").val() 
		},
		success: function() {
			window.location.href = "/admin";
			
		},
		error: doError
	})
});