$g.showMessageBox=function(e,t){$(e).addClass("error");var n=$(e).find(".help-block");n.html(t),n.fadeIn();var r=$(e).find("input");if(r.length==0){var i=$("form input");i.on("keydown",function(){$("#area_error").find(".help-block").hide(),$("#area_error").removeClass("error"),$("#area_error").hide(),i.off()})}else r.on("keydown",function(){n.hide(),$(e).removeClass("error"),r.off()})}