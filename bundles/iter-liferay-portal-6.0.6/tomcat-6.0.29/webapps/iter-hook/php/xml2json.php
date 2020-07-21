<?php
	if (extension_loaded('zlib') && substr_count($_SERVER['HTTP_ACCEPT_ENCODING'], 'gzip'))
	{
		ob_start("ob_gzhandler");
		header('Content-Encoding: gzip');
	}
	else
	{
		ob_start();
	}

	// XML
	$urlLen = strlen($_SERVER["REQUEST_URI"]) - 5;
	$xmlURL  = "http://" . $_SERVER["HTTP_HOST"] . substr($_SERVER["REQUEST_URI"], 0, $urlLen) . ".xml";

	if( file_get_contents_curl($xmlURL, $status, $headers, $xmlData) )
	{
		$result = xml_2_json($xmlData);

		if ($result)
		{
			header('Content-Type: application/json; charset=utf-8');
			header('Cache-Control: no-store, no-cache, must-revalidate');
			header('Vary: Accept-Encoding');
			header('Access-Control-Allow-Origin: *');

			echo $result;
		}
		else
		{
			header(':', true, 500);
			header('Cache-Control: max-age=1');
			header('Access-Control-Allow-Origin: *');
			header('Vary: Accept-Encoding');
			header('Content-Type: text/html;charset=utf-8');

			echo "An unexpected error occurred when adapting the RSS to JSON";
		}
	}
	else
	{
		// Pone el HTTP Status code
		header(':' . $status, true, $status);

		// Pone las cabeceras
		header('Cache-Control: max-age=1');
		header('Access-Control-Allow-Origin: *');
		header('Vary: Accept-Encoding');
		header('Content-Type: text/html;charset=utf-8');

		echo $xmlData;
	}


	function file_get_contents_curl($url, &$status, &$headers, &$content)
	{
		$headers = array();

		$options = array(
			CURLOPT_URL            => $url,                    // set the URL
			CURLOPT_CUSTOMREQUEST  => "GET",                   // set request type post or get
			CURLOPT_POST           => false,                   // set to GET
			CURLOPT_USERAGENT      => get_user_agent(),        // set user agent
			CURLOPT_COOKIE         => $_SERVER['HTTP_COOKIE'], // set cookies
			CURLOPT_RETURNTRANSFER => true,                    // return web page
			CURLOPT_HEADER         => false,                   // don't return headers
			CURLOPT_FOLLOWLOCATION => true,                    // follow redirects
			CURLOPT_ENCODING       => "",                      // handle all encodings
			CURLOPT_AUTOREFERER    => true,                    // set referer on redirect
			CURLOPT_CONNECTTIMEOUT => 120,                     // timeout on connect
			CURLOPT_TIMEOUT        => 120,                     // timeout on response
			CURLOPT_MAXREDIRS      => 10,                      // stop after 10 redirects
		);

		$ch = curl_init();
		curl_setopt_array( $ch, $options );

		curl_setopt($ch, CURLOPT_HEADERFUNCTION,
			function($curl, $header) use (&$headers)
			{
				$len = strlen($header);

				$response_status = curl_getinfo($curl, CURLINFO_HTTP_CODE);
				if ($response_status !== 301 && $response_status !== 302)
				{
					$header = explode(':', $header, 2);
					if (count($header) < 2) // ignore invalid headers
					  return $len;

					$name = trim($header[0]);


					if (!array_key_exists($name, $headers))
					{
						$headers[$name] = array();
						$headers[$name][] = trim($header[1]);
					}
					else
						$headers[$name][] = trim($header[1]);
				}

				return $len;
			}
		);

		$content = curl_exec($ch);
		$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);

		curl_close($ch);

		return $status === 200;
	}

	function xml_2_json($xml)
	{
		// Carga la respuesta en un documento XML
		$xmlDOM = new DOMDocument();
		$xmlDOM->loadXML($xml);

		// Carga la XSL
		$xslDOM = new DOMDocument();
		$xslDOM->load('xml2json.xsl');

		// Transformación
		$xslt = new XSLTProcessor;
		$xslt->importStylesheet($xslDOM);
		return $xslt->transformToXML($xmlDOM);
	}

	function get_user_agent()
	{
		$default_user_agent = '*ITERWEBCMS*';
		$current_user_agent = $_SERVER['HTTP_USER_AGENT'];

		if ( substr($current_user_agent, 0, strlen($default_user_agent) ) === $default_user_agent )
			return $current_user_agent;
		else
			return $default_user_agent;
	}
?>
