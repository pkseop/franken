<?
include_once ('common.php');

function main() {
	$apikey = $_REQUEST["apikey"];
	$url = $_REQUEST["url"]; // ex) "http://����ּ�ip";
	$id = $_REQUEST["id"];
	$params = array($apikey, $id);
	$rs_getTunnels = call($url, "xmlrpc", "tunnel.killTunnelById", "xml", $params);
}
main();
?>
