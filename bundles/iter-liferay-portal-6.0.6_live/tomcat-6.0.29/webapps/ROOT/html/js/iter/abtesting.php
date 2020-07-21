<?php
	class ITR_ABTesting_%1$s 
	{
		const ITR_ABTESTING_LOG_ENABLED 	= 'ITR_ABTESTING_LOG_ENABLED';
		private $log_enabled;
		private $_numVariants = array();
		private $_variantIds  = array();
		private $_variantname = "";	// Será el variantId de la página de detalle
		private $_querystring;
		private $_queries;
		
					
		function __construct() 
		{
			date_default_timezone_set("UTC");
			$this->log_enabled = isset($_COOKIE[self::ITR_ABTESTING_LOG_ENABLED]) && strtolower($_COOKIE[self::ITR_ABTESTING_LOG_ENABLED]) === 'true';
		}
		
		function initFromURL()
		{
			$this->_querystring = parse_url($_SERVER['REQUEST_URI'], PHP_URL_QUERY);
			$this->_queries = array();
			parse_str($this->_querystring, $this->_queries);
			
			if (isset($this->_queries["variant"]))
				$this->_variantname = $this->_queries["variant"];
		}
		
		function log($prefix, $str)
		{
			if ($this->log_enabled)
			{
				error_log('ITR_ABTesting: '. (isset($prefix)?$prefix:""). (isset($str)?$str:"") );
			}
		}
		
		/**
		* Establece el número de variantes que tendrá dicho artículo
		**/
		function setNumVariants($articleId, $numVariants)
		{
			$this->_numVariants[$articleId] = $numVariants;
		}
		
		/**
		* Devuelve el número de variantes que tendrá dicho artículo
		**/
		function getNumVariants($articleId)
		{
			$numVar = 0;
			
			try
			{
				// Si la clave no existe NO se captura con un catch la excepción, se considera un error
				if (array_key_exists($articleId, $this->_numVariants))
				{
					$numVar = $this->_numVariants[$articleId];
				}
			}
			catch (Exception $e)
			{
				$this->log("getNumVariants: ", $e->getMessage());
			}
			return $numVar;
		}
		
		function getVariant($articleId, $offset)
		{
			$index = 0;
			$numVariants = $this->getNumVariants($articleId);
			
			if ($numVariants > 0)
			{
				$index = (date("s", time()) + $offset) %% $numVariants;
			}
			return $index;
		}
		
		function getVariantNameByPos($index)
		{
			return chr(65+$index);
		}
		
		/**
		* Devuelve el nombre de la variante
		**/
		function getVariantName($articleId, $offset)
		{
			$index = $this->getVariant($articleId, $offset);
			return $this->getVariantNameByPos($index);
		}
		
		
		
		function setVariantId($articleId, $name, $variantId)
		{
			$this->_variantIds[$articleId.$name] = $variantId;
		}
		
		function getVariantId($articleId, $offset)
		{
			$name = $this->getVariantName($articleId, $offset);
			
			$variantId = 0;
			
			try
			{
				// Si la clave no existe NO se captura con un catch la excepción, se considera un error
				if (array_key_exists($articleId.$name, $this->_variantIds))
				{
					$variantId = $this->_variantIds[$articleId.$name];
				}
			}
			catch (Exception $e)
			{
				$this->log("getVariantId: ", $e->getMessage());
			}
			
			
			return $variantId;
		}
		
		function mb_parse_url($url)
		{
        	$enc_url = preg_replace_callback(
                                             '%%[^:/@?&=#]+%%usD',
											function ($matches)
											{
												return urlencode($matches[0]);
											},
											$url);

           $parts = parse_url($enc_url);

           if($parts === false)
           {
				throw new \InvalidArgumentException('Malformed URL: ' . $url);
           }

           foreach($parts as $name => $value)
           {
                $parts[$name] = urldecode($value);
           }

           return $parts;
       }
%2$s	
	}	
?>
<?php	
	$ITR_ABTesting_%1$s = new ITR_ABTesting_%1$s(); 
	%3$s
?>