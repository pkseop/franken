define(["jquery","view/"+$g.theme+"/message.res.js"],function(e,t){var n=function(){var t=this,n=null;this.init=function(e){n=e},this.SetHostname=function(){window.location.hostname!="localhost"&&n.SetHostname(window.location.hostname)},this.RequestVpnInfo=function(t){n.RequestVPNInfo({success:function(n){log("TSGSSLClient: RequestVPNInfo 성공"),log(n);var r;try{r=JSON.parse(n)}catch(i){alert("VPN 정보를 받아올 수 없습니다. (JSON error)");return}var s={auth_method:0,id_label:r.id_label,encryptions:"SEED",internal_ip:r.internal_ip};r=e.extend(s,r),t(r),!r.page_title||/^\s*$/.test(r.page_title)||(document.title=r.page_title)},failed:function(e){log("TSGSSLClient: RequestVPNInfo 실패"),log(e),alert("VPN 정보를 받아올 수 없습니다. (ActiveX error)")}})},this.RequestAppList=function(e){n.RequestAppList({success:function(t){log(t),log("[RequestAppList] success");var n;try{n=JSON.parse(t)}catch(r){alert("앱리스트를 받아올 수 없습니다. (JSON error)");return}e(n)},failed:function(){alert("앱리스트를 받아올 수 없습니다. (ActiveX error)")}})},this.RequestUserInfo=function(e){n.RequestUserInfo({success:function(t){log(t),log("[RequestUserInfo] success");var n;try{n=JSON.parse(t)}catch(r){alert("사용자 정보를 받아올 수 없습니다. (JSON error)");return}e(n)},failed:function(){alert("사용자 정보를 받아올 수 없습니다. (ActiveX error)")}})},this.StartNativeApp=function(e){n.StartNativeApp(e)},this.RequestInternalHttps=function(e,t,r,i,s){n.RequestInternalHttps(e,t,r,i,s)},this.LaunchPrivCertCenter=function(){n.LaunchPrivCertCenter()}};return new n})