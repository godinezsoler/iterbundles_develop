<?php
	class ITR_Slideshow
	{
		private static $_itrslide 		= null;
		private static $_currentURL 	= "";
		private static $_outputQueries 	= null;
		
		const URL_EMPTY	= 'javascript: void(0)';
	
		public static function init($protocol)
		{
			$querystring = parse_url($_SERVER['REQUEST_URI'], PHP_URL_QUERY);
			$queries = array();
			parse_str($querystring, $queries);
			
			// Se obtiene el itrslide
			if (isset($queries["itrslide"]))
				self::$_itrslide = $queries["itrslide"];
				
			// Se obtiene la URL completa SIN queryparams por si es necesario devolverla
			self::$_currentURL = $protocol.$_SERVER['HTTP_HOST'].strtok($_SERVER['REQUEST_URI'], '?');
			
			$queriesObject = new ArrayObject($queries);
			self::$_outputQueries = $queriesObject->getArrayCopy();
		}
	
		public static function getOGImage()
		{
			%1$s
			return $result;
		}
		public static function getSlideshowTag()
		{
			?>%2$s<?php
		}
		public static function getSlideshowSlide($which)
		{
			$itrslide='';
			%3$s
			return $itrslide;
		}
		public static function getSlideshowURL($which)
		{
			$url 	  = self::URL_EMPTY;
			$itrslide = self::getSlideshowSlide($which);
			
			
			// Si está vacío la URL será vacía
			if ($itrslide != "")
			{
				self::$_outputQueries['itrslide'] = $itrslide;
				
				$outputQuerystring = http_build_query(self::$_outputQueries);
				$url = self::$_currentURL.'?'.$outputQuerystring;			
			}
			
			return $url;
		}
	}
	ITR_Slideshow::init('%4$s');
?>	