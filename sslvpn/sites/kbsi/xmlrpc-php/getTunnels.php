<?
include_once ('common.php');

function main() {
	$apikey = $_REQUEST["apikey"];
	$url = $_REQUEST["url"]; // ex) "http://����ּ�ip";
	$params = array($apikey);
	$rs_getTunnels = call($url, "xmlrpc", "tunnel.getTunnels", "xml", $params);
}
main();
?>
