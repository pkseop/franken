<?
function encode_xmlrpc_request($method, $params) {
	$xml = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>";
	$xml .= "\n<methodCall>";
	$xml .= "\n<methodName>". $method ."</methodName>";
	$xml .= "\n<params>";
	foreach ($params as $value){
		$xml .= "\n <param>";
		$xml .= "\n  <value>";
		$xml .= "\n   <string>";
		$xml .= $value;
		$xml .= "</string>";
		$xml .= "\n  </value>";
		$xml .= "\n </param>";
	}
	$xml .= "\n</params>";
	$xml .= "\n</methodCall>";

	return $xml;
}

function decode_xmlrpc_response($response) {

	$xml = simplexml_load_string($response);

	$child_name = (string)$xml->children()->getName();

	$response = array();

	if($child_name == "fault") {
		$fault = $xml->fault;
		$response[] = decode_value($fault->value);	
	} else {
		$params = $xml->params;

		foreach($params->param as $param) {
			$response[] = decode_value($param->value);
		}
	}

	return $response;
}

function decode_value($value) {
	$child = $value->children();

	switch ($child->getName()) {
		case "array":
			return decode_array($value);
		case "base64":
			return decode_base64($value);
		case "boolean":
			return decode_boolean($value);
		case "dateTime.iso8601":
			return decode_dateTime($value);
		case "double":
			return decode_double($value);
		case "struct":
			return decode_struct($value);
		case "i4":
		case "Int":
			return decode_integer($value);
		case "string":
			return decode_string($value); 
	}
}

function decode_array($value) {
	$result = array();
	foreach($value->array as $array) {
		$data = $array->data;
		foreach($data->value as $child_value) {
			$result[] = decode_value($child_value);
		}
	}
	return $result;
}

function decode_struct($value) {
	$result = array();
	foreach($value->struct as $struct) {
		foreach($struct->member as $member) {
			$m = decode_member($member);
			foreach($m as $k=>$v) {
				$result[$k] = $v;
			}
		}
	}
	return $result;
}

function decode_member($value) {
	$result = array();
	$name = (string) $value->name;
	$v = decode_value($value->value);
	$result[$name] = $v;
	return $result;
	
}

function decode_base64($value) {
	return $value->base64;
}

function decode_boolean($value) {
	return (boolean)$value->boolean;
}

function decode_dateTime($value) {
	return $value->dateTime.iso8601;
}

function decode_double($value) {
	return (double)$value->double;
}

function decode_string($value) {
	return (string)$value->string;
}

function decode_integer($value) {
	$name = $value->children()->getName();
	$v = $value->$name;
	if($name == "i4" || $name == "int")	
		return (int) $v;		

}
/**
 * call
 */
function call($url, $bundle, $method, $type, $params) {

	if (count($params) == 0) {
		$body = array('' => '');
	} else {
		$body = $params;
	}

	if($type == "xml") {
		//$xml = xmlrpc_encode_request($method, $params);
		$xml = encode_xmlrpc_request($method, $params);
		$msg = $xml;

	} elseif ($type == "json") {
		$json = json_encode($params);
		$msg = $json;
	}
	
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, $url."/".$bundle);
	curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: text/'.$type.''));
	curl_setopt($ch, CURLOPT_POST, 1);
	curl_setopt($ch, CURLOPT_POSTFIELDS, $msg);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch, CURLOPT_FORBID_REUSE, 1);
	curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);
	curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
	$response = curl_exec($ch);

	curl_close($ch);

	//$result = xmlrpc_decode($result);
	$result = decode_xmlrpc_response($response);


	print_r($result);

	return $result;

}
?>
