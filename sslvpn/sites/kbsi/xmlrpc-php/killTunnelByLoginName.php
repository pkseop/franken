<?
include_once ('common.php');

function main() {
	$apikey = $_REQUEST["apikey"];
	$url = $_REQUEST["url"]; // ex) "http://장비주소ip";
	$login_name = $_REQUEST["login_name"];
	$params = array($apikey, $login_name);
	$rs_getTunnels = call($url, "xmlrpc", "tunnel.killTunnelByLoginName", "xml", $params);
}
main();
?>

