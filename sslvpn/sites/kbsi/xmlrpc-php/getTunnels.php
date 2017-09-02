<?
include_once ('common.php');

function main() {
	$apikey = $_REQUEST["apikey"];
	$url = $_REQUEST["url"]; // ex) "http://장비주소ip";
	$params = array($apikey);
	$rs_getTunnels = call($url, "xmlrpc", "tunnel.getTunnels", "xml", $params);
}
main();
?>
