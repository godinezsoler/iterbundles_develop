<?php
	class ITR_Metatags
	{
		const   OG_IMAGE_DEFAULT 			= "%2$s";
		const   OG_URLDEFAULT 				= "%3$s";
		const 	ITR_METATAGS_LOG_ENABLED 	= 'ITR_METATAGS_LOG_ENABLED';
	
		private static $_log_enabled		= null;
		private static $_ogImage 		 	= null;
		private static $_ogImageWidth	 	= null;
		private static $_ogImageHeight	 	= null;
		private static $_abtesting		 	= null;
		private static $_outputQueries	 	= null;
		private static $_showRelNext 		= true;
		private static $_showRelPrev 		= true;
			
		public static function init($abtesting, $showRelNext, $showRelPrev)
		{
			// Se obtienen las QUeryStrings para el OGURL
			$querystring = parse_url($_SERVER['REQUEST_URI'], PHP_URL_QUERY);
			$queries = array();
			parse_str($querystring, $queries);
			
			$queriesObject = new ArrayObject($queries);
			self::$_outputQueries = $queriesObject->getArrayCopy();
			
			self::$_log_enabled = isset($_COOKIE[self::ITR_METATAGS_LOG_ENABLED]) && strtolower($_COOKIE[self::ITR_METATAGS_LOG_ENABLED]) === 'true';
			self::$_abtesting = $abtesting;
			
			self::$_showRelNext = $showRelNext;
			self::$_showRelPrev = $showRelPrev;
		}
			
		private static function log($prefix, $str=NULL)
		{
			if (isset(self::$_log_enabled) && self::$_log_enabled)
			{
				error_log('ITR_Metatags: '. (isset($prefix)?$prefix:""). (isset($str)?$str:"") );
			}
		}
			
		public static function getOGImageWidth() 
		{
			if (self::$_ogImageWidth == null)
			{
				self::calcOGImageDimensions(); 
			}
			return self::$_ogImageWidth;
        }
        
		public static function getOGImageHeight() 
		{
			if (self::$_ogImageHeight == null)
			{
				self::calcOGImageDimensions(); 
			}
			return self::$_ogImageHeight;
        }
        
		public static function getOGImage()
		{
			if (self::$_ogImage == null)
			{
				$value = self::OG_IMAGE_DEFAULT;
	
				if (method_exists("ITR_Slideshow", "getOGImage"))
				{
					// Si hay definido ITR_Slideshow
					$value = ITR_Slideshow::getOGImage();
				}
				else if (isset(self::$_abtesting) && method_exists(self::$_abtesting, "getOGImage"))
				{
					// Si hay definido ABTesting
					$value = self::$_abtesting->getOGImage();
				}
				
				self::$_ogImage = $value;
			}
			return self::$_ogImage;
		}
		
		public static function getOGURL()
		{
			$value = self::OG_URLDEFAULT;
			
			if (isset(self::$_outputQueries) && !getenv('ITER_UA_IS_ROBOT'))
			{
				$value = $value.'?'.http_build_query(self::$_outputQueries);
			}
			
			return $value;
		}
		
		public static function writePrevAndNext()
		{
			if (method_exists("ITR_Slideshow", "getSlideshowURL"))
			{
				if (self::$_showRelNext)
				{ 
					$next = ITR_Slideshow::getSlideshowURL('NEXT');
					if ($next !== ITR_Slideshow::URL_EMPTY)
					{
						echo '<link rel="next" href="'.$next.'"/>';
					}
				}
							
				if (self::$_showRelPrev)
				{				
					$prev = ITR_Slideshow::getSlideshowURL('PREV');
					if ($prev !== ITR_Slideshow::URL_EMPTY)
					{
						echo '<link rel="prev" href="'.$prev.'"/>';
					}
				}
			}
		}
		
		private static function calcOGImageDimensions()
        {
            $url = self::getOGImage();
				
			if (strlen($url) > 0)
			{
				$posDoc = strpos($url, "/documents/");
				$posBin = strpos($url, "/binrepository/");

				if ($posDoc !== false)
				{
					$chunks = explode("/", substr($url, $posDoc+ strlen("/documents/")));

					if (sizeof($chunks) == 9)
					{
						self::calcImageDimensions($chunks[2], $chunks[4]);
					}	
				}
				else if ($posBin !== false)
				{
					$chunks = explode("/", substr($url, $posBin+ strlen("/binrepository/")));
					if (sizeof($chunks) == 7)
					{
						self::calcImageDimensions($chunks[0], $chunks[2]);
					}	
				}

				if (!isset(self::$_ogImageWidth) || !isset(self::$_ogImageHeight))
				{
					self::log("XYZ_E_INVALID_IMG_URL_ZYX: ".$url);
				}
			}
        }

		private static function calcImageDimensions($resize, $cropping)
		{
			do
			{
				$resizeDimensions = explode("x", $resize);
				if (sizeof($resizeDimensions) != 2)
				{
					self::log("XYZ_E_INVALID_IMG_URL_RESIZE_CHUNK_ZYX");
					break;
				}

				$croppingDimensions = explode("d", $cropping);
				if (sizeof($croppingDimensions) != 2)
				{
					self::log("XYZ_E_INVALID_IMG_URL_CROPPING_CHUNK_ZYX");
					break;
				}

				if ($croppingDimensions[0] === "0" && $croppingDimensions === "0")
				{
					self::$_ogImageWidth  = $resizeDimensions[0];
					self::$_ogImageHeight = $resizeDimensions[1];
				}
				else
				{
					self::$_ogImageWidth  = $croppingDimensions[0];
					self::$_ogImageHeight = $croppingDimensions[1];
				}
			}
			while(false);
		}
	}
?>	

<?php
ITR_Metatags::init( isset($ITR_ABTesting_%1$s) ? $ITR_ABTesting_%1$s : null, %4$b, %5$b);
ITR_Metatags::writePrevAndNext();
?>
				
