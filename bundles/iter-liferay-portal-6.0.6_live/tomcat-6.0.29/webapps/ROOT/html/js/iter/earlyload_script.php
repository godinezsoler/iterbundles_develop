<?php
	class ITR_Redirect 
	{
		const RENDITION_CLASSIC			= 'classic';
		const RENDITION_MOBILE 			= 'mobile';
		const ITR_MOBILEVIEW_DISABLED 	= 'ITR_MOBILEVIEW_DISABLED';
		const ITR_REDIRECT_LOG_ENABLED 	= 'ITR_REDIRECT_LOG_ENABLED';
		
		public $classic_url;
		public $mobile_url;
		public $origin_url;
		public $is_origin_mobile;
		public $preferredRendition;
		public $redirectTo;
		public $log_enabled;
		public $query_string;
		
		
		function __construct($classic_url, $mobile_url) 
		{
			$this->log_enabled = isset($_COOKIE[self::ITR_REDIRECT_LOG_ENABLED]) && strtolower($_COOKIE[self::ITR_REDIRECT_LOG_ENABLED]) === 'true';
		
			$this->classic_url 	= $classic_url;
			$this->log("classic_url=", $this->classic_url);
			
			$this->mobile_url 	= $mobile_url;
			$this->log("mobile_url=", $this->mobile_url);
			
			$this->origin_url 	= $this->getUrl();
			$this->log("origin_url=", $this->origin_url);
			
			$this->setOriginType();
			$this->setPreferrerRendition();
			$this->setRedirectTo();
		}
		
		function setRedirectTo()
		{
			$this->redirectTo = null;
			if($this->is_origin_mobile)
			{
				if($this->preferredRendition === self::RENDITION_MOBILE && $this->origin_url !== $this->mobile_url)
				{
					// En entorno móvil, prefiere ver la versión móvil y NO es la URL actual
					$this->redirectTo = $this->mobile_url;
				}
				elseif($this->preferredRendition === self::RENDITION_CLASSIC && $this->origin_url !== $this->classic_url)
				{
					// En entorno móvil, prefiere ver la versión clásica y NO es la URL actual
					$this->redirectTo = $this->classic_url;
				}
			}
			elseif($this->origin_url !== $this->classic_url)
			{
				// En un entorno clásico SIEMPRE tiene que ir la URL clásica
				$this->redirectTo = $this->classic_url;
			}
			
			$this->log("redirectTo=", $this->redirectTo);
		}
		
		function getRedirectTo()
		{
			if ($this->redirectTo != null && strlen($this->query_string) > 0)
			{
				return $this->redirectTo . '?' . $this->query_string;
			}

			return $this->redirectTo;
		}
		
		function setOriginType()
		{
			$this->is_origin_mobile = preg_match("/(Android|webOS|iPhone|iPod|BlackBerry|IEMobile|OperaMini)/i", $_SERVER["HTTP_USER_AGENT"]);
			$this->log("is_origin_mobile=", $this->is_origin_mobile);
		}
		
		function setPreferrerRendition()
		{
			$cookie_mobile_view_disabled = isset($_COOKIE[self::ITR_MOBILEVIEW_DISABLED]) && strtolower($_COOKIE[self::ITR_MOBILEVIEW_DISABLED]) === 'true';
			$this->preferredRendition = !$cookie_mobile_view_disabled ? self::RENDITION_MOBILE : self::RENDITION_CLASSIC;
			$this->log("preferredRendition=", $this->preferredRendition);
		}
		
		function log($prefix, $str)
		{
			if ($this->log_enabled)
			{
				error_log('ITR_Redirect: '. (isset($prefix)?$prefix:""). (isset($str)?$str:"") );
			}
		}
		
		function getProtocolWithHost()
		{
			$this->log("HTTP_HOST=", 	isset($_SERVER['HTTP_HOST']) 	? $_SERVER['HTTP_HOST'] 	: '');
			$this->log("SERVER_NAME=", 	isset($_SERVER['SERVER_NAME']) 	? $_SERVER['SERVER_NAME'] 	: '');
			
			// No se puede usar el HTTP_HOST porque cuando el contenido no está cacheado llega el host clásico y no el móvil 
			// por la redirección que se hace en el vhost.conf del Apache (RequestHeader set Host virtualhost env=mobile)
			// $host     = isset($_SERVER['HTTP_HOST']) ? $_SERVER['HTTP_HOST'] : null;
			// #$host     = isset($host) ? $host : $_SERVER['SERVER_NAME'];
			
			return $_SERVER['SERVER_NAME'];
		}

		function getUrl()
		{
            $url = $this->getProtocolWithHost().$_SERVER['REQUEST_URI'];
			$this->query_string = parse_url($url, PHP_URL_QUERY);
			return strtok($url,'?');
		}
	}
?>
