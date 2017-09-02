<?
include_once ('common.php');

function main() {
	$apikey = $_REQUEST["apikey"];
	$url = $_REQUEST["url"]; // ex) "http://장비주소ip";
	$ip = $_REQUEST["ip"];
	$params = array($apikey, $ip);
	$rs_getTunnels = call($url, "xmlrpc", "tunnel.findTunnelByIp", "xml", $params);
}

main();
?>
