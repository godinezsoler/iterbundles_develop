<?php
	class ITR_ABTesting 
	{
		const ITR_ABTESTING_LOG_ENABLED 	= 'ITR_ABTESTING_LOG_ENABLED';
		private $log_enabled;
					
		// Variable que alberga cuál es la variante elegida para cada uno de los artículos con experimentos			
		private $current_variant;			
		
		function __construct() 
		{
			$this->log_enabled = isset($_COOKIE[self::ITR_ABTESTING_LOG_ENABLED]) && strtolower($_COOKIE[self::ITR_ABTESTING_LOG_ENABLED]) === 'true';
			$this->current_variant = array();
			$this->variants = array();
		}
		
		function log($prefix, $str)
		{
			if ($this->log_enabled)
			{
				error_log('ITR_ABTesting: '. (isset($prefix)?$prefix:""). (isset($str)?$str:"") );
			}
		}
		
		function getImageTag($articleId)
		{
			$index = date("i", time()) %% %d;
?>			
			%s
<?php						
		}
	}
	
	$ITR_ABTesting = new ITR_ABTesting(); 
?>