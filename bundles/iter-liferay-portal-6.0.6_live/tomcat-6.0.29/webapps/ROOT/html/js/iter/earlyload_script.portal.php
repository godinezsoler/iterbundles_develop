<?php
	class ITR_Redirect {
		const RENDITION_CLASSIC				= 'classic';
		const RENDITION_MOBILE 				= 'mobile';
		const ITR_MOBILEVIEW_DISABLED = 'ITR_MOBILEVIEW_DISABLED';
		
		public $classic_url;
		public $mobile_url;
		public $origin_url;
		public $is_origin_mobile;
		public $preferredRendition;
		public $redirectTo;
		
		function __construct($classic_url, $mobile_url) {
			$this->classic_url = $classic_url;
			$this->mobile_url = $mobile_url;
			$this->origin_url = $this->getUrl();
			$this->setOriginType();
			$this->setPreferrerRendition();
			$this->setRedirectTo();
		}
		
		function setRedirectTo(){
			$this->redirectTo = null;
			if($this->is_origin_mobile){
				if($this->preferredRendition === self::RENDITION_MOBILE && $this->origin_url !== $this->mobile_url){
					// En entorno móvil, prefiere ver la versión móvil y NO es la URL actual
					$this->redirectTo = $this->mobile_url;
				}elseif($this->preferredRendition === self::RENDITION_CLASSIC && $this->origin_url !== $this->classic_url){
					// En entorno móvil, prefiere ver la versión clásica y NO es la URL actual
					$this->redirectTo = $this->classic_url;
				}
			}elseif($this->origin_url !== $this->classic_url){
				// En un entorno clásico SIEMPRE tiene que ir la URL clásica
				$this->redirectTo = $this->classic_url;
			}
		}
		
		function getRedirectTo(){
			return $this->redirectTo;
		}
		
		function setOriginType(){
			$this->is_origin_mobile = preg_match("/(Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|OperaMini)/i", $_SERVER["HTTP_USER_AGENT"]);
		}
		
		function setPreferrerRendition(){
			$cookie_mobile_view_disabled = isset($_COOKIE[self::ITR_MOBILEVIEW_DISABLED]) && strtolower($_COOKIE[self::ITR_MOBILEVIEW_DISABLED]) === 'true';
			$this->preferredRendition = !$cookie_mobile_view_disabled ? self::RENDITION_MOBILE : self::RENDITION_CLASSIC;
		}
		
		function getProtocolWithHost(){
			$ssl      = !empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] == 'on';
			$sp       = strtolower($_SERVER['SERVER_PROTOCOL']);
			$protocol = substr($sp, 0, strpos($sp, '/')) . (($ssl) ? 's' : '' );
			$port     = $_SERVER['SERVER_PORT'];
			$port     = ((!$ssl && $port == '80') || ($ssl && $port == '443')) ? '' : ':'.$port; // Not showing the default port 80 for HTTP and port 443 for HTTPS
			$host     = isset($_SERVER['HTTP_HOST']) ? $_SERVER['HTTP_HOST'] : null;
			$host     = isset($host) ? $host : $_SERVER['SERVER_NAME'] . $port;
			return $protocol . '://' . $host;
		}

		function getUrl(){
			return $this->getProtocolWithHost().$_SERVER['REQUEST_URI'];
		}
	}
?>
