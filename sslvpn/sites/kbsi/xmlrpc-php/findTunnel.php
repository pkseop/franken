<?
include_once ('common.php');

function main() {
	$apikey = $_REQUEST["apikey"];
	$url = $_REQUEST["url"]; // ex) "http://����ּ�ip";
	$ip = $_REQUEST["id"];
	$params = array($apikey, $ip);
	$rs_getTunnels = call($url, "xmlrpc", "tunnel.findTunnel", "xml", $params);
}

main();
?>
