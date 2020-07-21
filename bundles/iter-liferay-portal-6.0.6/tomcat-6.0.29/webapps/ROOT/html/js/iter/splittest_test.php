<?php
	class ITR_SplitTest 
	{
		private $log_enabled;
		private $variants = 
			array	(
					);
					
		// Variable que alberga cuál es la variante elegida para cada uno de los artículos con experimentos			
		private $current_variant;			
		
		function __construct() 
		{
			$this->log_enabled = isset($_COOKIE[self::ITR_ABTESTING_LOG_ENABLED]) && strtolower($_COOKIE[self::ITR_ABTESTING_LOG_ENABLED]) === 'true';
			$this->current_variant = array();
		}
		
		function log($prefix, $str)
		{
			if ($this->log_enabled)
			{
				error_log('ITR_ABTesting: '. (isset($prefix)?$prefix:""). (isset($str)?$str:"") );
			}
		}
		
		function addImageTagData($articleId, $data)
		{
			$variants[$articleId] = $data;
		}
		
		
		function getImageTagData($articleId)
		{
			$data = $variants[$articleId];
			eval("\$data = \"$data\";");
			return $data;
		}
	}
	
	$ITR_SplitTest = new ITR_SplitTest(); 
?>
