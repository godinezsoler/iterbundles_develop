<?php

// La idea es controlar que no exista la clase (y la instancia) ya por si se crea desde
// un WidgetFragment
if (!method_exists('ITR_RFV','already_defined')) {
	class ITR_RFV 
	{
    const ITR_RFV_LOG_ENABLED 	        = 'ITR_RFV_LOG_LEVEL';
    const ITR_RFV_ID_NEW_USERS 	        = "RFV_NEW_USERS";
    const ITR_RFV_ID_UNCLASSIFIED_USERS = "RFV_UNCLASSIFIED_USERS";

    const PIWIK_COOKIE_PREFIX           = "_pk_id";
    const ITR_COOKIE_NAME               = "mas_itr_visitor";
    const ITR_COOKIE_VERSION            = 1;
    

    private $log_enabled;
    private $segmentId;
    private $siteId;
    private $visitorId;
    private $piwikCookieName;
    private $rfvData;
    private $rfvHeaders;

		function __construct($host, $siteId, $createCookie=false) 
		{
      $this->host         = $host;
      $this->siteId       = $siteId;
      $this->log_enabled  = isset($_COOKIE[self::ITR_RFV_LOG_ENABLED]) && strtolower($_COOKIE[self::ITR_RFV_LOG_ENABLED]) === 'true';
      $this->log("Versi�n actual de PHP: ", phpversion());

      // Genera la cookie si no existe
      // $this->generateItrVisitorCookie();
    }
    
    function log($prefix, $str) 
    {
      if ($this->log_enabled && (isset($prefix) || isset($str)))
      {
          $this->_log( (isset($prefix)?$prefix:"").(isset($str)?$str:"") );
      }
    }

    function _log($msg)
    {
        error_log(__CLASS__." ". $msg);
    }
    

    /**
     * Funci�n que crea la cookie para el sitio con los datos del visitante
     */
    function generateItrVisitorCookie()
    {
      $cookieName = self::ITR_COOKIE_NAME;
      if (!isset($_COOKIE[$cookieName]))
      {
        // Si no est� la cookie y existen datos del usuario se genera la cookie
        $this->getSegmentId();
        if (isset($this->segmentId))
        {
            // ITER-1292: Se elimina la cookie hasta que se encripte el contenido de la cookie
            // $rfv  = (isset($this->rfvData) && isset($this->rfvData->rfv)) ? $this->rfvData->rfv : '';
                        
            if (isset($this->rfvHeaders) && array_key_exists ("Expires", $this->rfvHeaders))
            {
              $time = strtotime($this->rfvHeaders["Expires"]);
            }
            else
            {
              $time = (isset($this->rfvData) && isset($this->rfvData->expires)) ? $this->rfvData->expires : (time() + 43200);
            }
                        
            $cookieValue = self::ITR_COOKIE_VERSION."|".$this->segmentId; //."|".$rfv;
            $this->log("generateItrVisitorCookie ", $cookieValue);
            
            // 12H
            setcookie($cookieName, base64_encode($cookieValue), $time, "/");
        }
        else
        {
            $this->log("generateItrVisitorCookie ", "Doesn't exist data to create ".$cookieName);
        }
      }      
    }

    
        function getPiwikCookieName()
        {
            if (!isset($this->piwikCookieName))
            {
                if (isset($_COOKIE)) 
                {
                    $prefixCookieName = self::PIWIK_COOKIE_PREFIX."_".$this->siteId."_";
                    foreach($_COOKIE as  $key => $val)
                    {
                        if (substr($key, 0, strlen($prefixCookieName)) === $prefixCookieName)
                        {
                            $this->piwikCookieName = $key;
                            $this->log('$this->piwikCookieName ', $this->piwikCookieName);
                        break;
                        }
                    }
                } 
            }
            return $this->piwikCookieName;
        }

        function getVisitorId()
        {
            if (!isset($this->visitorId))
            {
                $cookieName = $this->getPiwikCookieName();
                
                if (isset($cookieName))
                {
                    $value = $_COOKIE[$cookieName];
                    $this->visitorId = substr($value, 0, stripos($value, "."));
                    $this->log('$this->visitorId ', $this->visitorId);
                }
            }

            return $this->visitorId;
        }

        function getSegmentId()
        {
            if (!isset($this->segmentId))
            {
              // Intenta obtener el segmento de la cookie que coloca ITER
              if (isset($_COOKIE[self::ITR_COOKIE_NAME]))
              {
                $value = base64_decode($_COOKIE[self::ITR_COOKIE_NAME]);
                if ($this->log_enabled)
                {
                    $responseStr = var_export($value, true);
                    $this->log(self::ITR_COOKIE_NAME." value ", $responseStr);
                }

                if (gettype($value) === "string")
                {
                  $cookieFields = explode("|", $value);	
                  $segmentId = $cookieFields[1];
                }
              }
              else
              {
                // Si no existe visitante es porque NO existe Cookie
                $visitorId = $this->getVisitorId();
                if (!isset($visitorId))
                {
                    $segmentId = self::ITR_RFV_ID_NEW_USERS;
                }
                else
                {
                  // Existe visitante, es necesario preguntar a MAS el segmento
                  try
                  {
                      $url = $this->host.'/restapi/user/getVisitorRfv/'.$this->siteId.'/'.$visitorId;
                      $this->log("MAS getVisitorRfv request ", $url);

                      $response = $this->makeGetRequest($url);

                      if ($this->log_enabled)
                      {
                          $responseStr = var_export($response, true);
                          $this->log("MAS getVisitorRfv response ", $responseStr);
                      }
                      
                      // Se analiza la respuesta
                      if ( $response["status"] && gettype($response["data"]) === "object" && $response["data"]->status)
                      {
                          $this->rfvData     = $response["data"]->data;
                          $this->rfvHeaders  = $response["headers"];
                          $segmentId = (isset($response["data"]->data->segmentId)) ? $response["data"]->data->segmentId : self::ITR_RFV_ID_UNCLASSIFIED_USERS;
                      }
                      else 
                      {
                        $this->log("MAS getVisitorRfv response ", "Failed");
                      }
                  }
                  catch (Exception $e)
                  {
                      $this->_log( 'MAS getVisitorRfv exception: '.$e->getMessage() );    
                  }
                }
              }

              if (isset($segmentId))
                  $this->segmentId = $segmentId;
            }
            return $this->segmentId;
        }

        function belongToSegments($segments)
        {
            // Si no existe cookie ser� un segmentId bien conocido
            $segmentId  = $this->getSegmentId();
            $belong2    = isset($segmentId) && in_array($segmentId, $segments);

            error_log(__CLASS__." ". $segmentId);

            if ($this->log_enabled)
            {
                $this->log("belongToSegments:", "segmentId (".$segmentId.") in segments (".implode(", ", $segments).")  = ".$belong2);
            }
            return $belong2;
        }
        
        public static function already_defined() {
        }

        public function makeGetRequest($url, $parameters = array())
        {
          $ch = curl_init();
          
          if ($this->log_enabled)
          {
            $this->log("makeGetRequest curl_init ", var_export($ch, true));
          }

          if ($ch !== false)
          {
            curl_setopt($ch, CURLOPT_URL,             $url.self::stringifyParameters($parameters));
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER,  false);
            curl_setopt($ch, CURLOPT_USERAGENT,       "*ITERWEBCMS*");
            curl_setopt($ch, CURLOPT_RETURNTRANSFER,  true);
            curl_setopt($ch, CURLOPT_HEADER,          true);
            curl_setopt($ch, CURLINFO_HEADER_OUT,     true);
          }

          $response = self::makeRequestAndcheckStatus($ch);

          if ($ch !== false)
          {
            curl_close($ch);
          }
          
          return $response;
        }
      
        public function makePostRequest($url, $parameters, $method = 'POST'){
            $data_json = json_encode($parameters); //, JSON_UNESCAPED_UNICODE
            $ch = curl_init();
            if ($this->log_enabled)
            {
            	$this->log("makePostRequest curl_init ", var_export($ch, true));
            }
            
            if ($ch !== false)
            {
              curl_setopt($ch, CURLOPT_URL, $url);
              curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
              curl_setopt($ch, CURLOPT_HEADER, true);
	            curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json')); //; charset=utf-8
	            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
	            curl_setopt($ch, CURLOPT_POSTFIELDS,$data_json);
              curl_setopt($ch, CURLINFO_HEADER_OUT, true);
            }
            
            $response = $this->makeRequestAndcheckStatus($ch);
            
            if ($ch !== false)
            {
            	curl_close($ch);
            }
            return $response;
          }
        
          public static function stringifyParameters($parameters, $parameterString = '?') {
        
            foreach ($parameters as $key => $value) {
              $key = urlencode($key);
              $value = urlencode($value);
        
              $parameterString .= "$key=$value&";
            }
        
            $parameterString = rtrim($parameterString, '&');
        
            return $parameterString;
          }
        
          protected function makeRequestAndcheckStatus($ch)
          {
          	// Si fallo la inicializaci�n no se ejecuta el curl
            $response = ($ch === false) ? $ch : curl_exec($ch);
            
			if ($this->log_enabled)
			{
				if ($response === false)
				{
					$this->log("makeRequestAndcheckStatus curl_error ", curl_errno($ch)." - ".curl_error($ch));
				}
				$this->log("makeRequestAndcheckStatus curl_exec ", 		var_export($response, true));
				$this->log("makeRequestAndcheckStatus curl_getinfo ", 	var_export(curl_getinfo($ch), true));
			}

            $allGood  = true;
            list($header, $body) = explode("\r\n\r\n", $response, 2);

            if($response === false)
            {
              	$allGood	= false;
              	$body	= curl_errno($ch)." ".curl_error($ch);
            }
            else
            {
            	// A partir de PHP 5.4 es posible hacer referencia al array del resultado de una llamada a una 
            	// funci�n o m�todo directamente. Antes s�lo era posible utilizando una variable temporal.
            	$getinfo = curl_getinfo($ch);
              	$request_status = $getinfo['http_code'];
              	
              	if(empty($request_status))
              	{ 
              		//No HTTP code was returned
                	$allGood = false;
              	}
              	else
              	{
                	if($request_status >= 400)
                	{ 
                		//Ocurri� un error
                  		$allGood = false;
                	}
            	}
            }
            if(empty($request_status))
            {
              $request_status = 400;
            }
            
            return array(
                    'status'    => $allGood,
                    'data'      => json_decode($body),
                    'http_code' => $request_status,
                    'headers'   => self::headers_decode($header)
                   );
          }

          public static function headers_decode($raw_headers)
          {
              $array = explode("\r\n", $raw_headers );

              $headers = array();
              foreach ($array as $number=>$header)
              {
                $key_value = explode(": ", $header, 2);
                if (sizeof($key_value) === 2)
                {
                  $headers[$key_value[0]] = $key_value[1];
                }
              }
              return $headers;
          }
 
        
          public static function print_error_log($info_array, $source){
            $string = "";
            foreach ($info_array as $key => $value) {
              if(is_array($value)){
                $string .= '['.$key.'] = '.implode(" ",$value).' | ';
              }else{
                $string .= '['.$key.'] = '.$value.' | ';
              }
            }
            $string = $source.substr($string, 0, strlen($string)-3);
            error_log($string, 0);
          }
                
    }
}
?>
