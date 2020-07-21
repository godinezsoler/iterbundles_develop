<?php
	class ITR_SplitTest 
	{
		private $log_enabled;
		const variants = 
			array	(
						"idArt3" => array
							(
								array
									(
										"TeaserHeadline" => "Title 01",
										"TeaserText" 	 => "Text 01",
										"TeaserImage"  	 => "/documents/1.jpg"
									),
									
								array
									(
										"TeaserHeadline" => "Title 02",
										"TeaserText" 	 => "Text 02",
										"TeaserImage"  	 => "/documents/2.jpg"
									),
								
								array
									(
										"TeaserHeadline" => "Title 03",
										"TeaserText" 	 => "Text 03",
										"TeaserImage"  	 => "/documents/3.jpg"
									)
							),
							
						"idArt4" => array
							(
								array
									(
										"TeaserHeadline" => "Title 401",
										"TeaserText" 	 => "Text 401",
										"TeaserImage"  	 => "/documents/41.jpg"
									),
									
								array
									(
										"TeaserHeadline" => "Title 402",
										"TeaserText" 	 => "Text 402",
										"TeaserImage"  	 => "/documents/42.jpg"
									)
							)
						
					);
					
		// Variable que alberga cu�l es la variante elegida para cada uno de los art�culos con experimentos			
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
		
		
		function getImageTagData($articleId, $contentType, $data)
		{
			if ($articleId == 1 variante == 0)
			{
				if (getenv('ITER_UA_IS_ROBOT') || (isset($_SERVER['HTTP_X_REQUESTED_WITH']) && $_SERVER['HTTP_X_REQUESTED_WITH'] == 'XMLHttpRequest') ) 
				{
					<img srca="a">
				}
				else
				{
					<img srca="a">
				}
			}
		}
		
		/**
		* Devuelve el valor del getTag para dicho contentType.
		* Tomar� la variante que se est� pintando en la p�gina para el art�culo. 
		*/
		function getTagData($articleId, $contentType, $data)
		{
			$result = "";
			 
			// Tiene que controlar que NO se trate de un robot, en cuyo caso devolver�a $data
			if (getenv('ITER_UA_IS_ROBOT'))
			{
				$this->log("getTagData: ", "Is robot");
				$result = $data; 
			}
			else
			{
				$variant = $this->getVariant($articleId);
				
				// Puede que este art�culo no tenga experimento/variantes o experimento
				if ($variant == null)
				{
					$this->log("getTagData: ", "Doesn`t have experiment");
					$result = $data; 
				}
				else
				{
					$result  = $variant[$contentType];
					if ($result == null)
						$result = "";
				}
			}
			
			if ($this->log_enabled)
				$this->log("getTagData: ", "articleId='".$articleId."' contentType='".$contentType."' result='".$result."'");
			
			return $result;
		}
		
		/**
		* Devuelve la variante que se est� utilizando del art�culo
		**/
		function getVariant($idArt)
		{
			$variant = $this->current_variant($idArt);
			
			// Si no tiene variante registrada puede ser que se la primera vez que se pida
			if ($variant == null)
			{
				$variants = $ITR_SplitTest.variants[idArt];
				if ($variants != null)
				{
					// Se calcula la variante a tomar en funci�n de los segundos y el total de variantes para dicho art�culo
					$index 	 = $Time.seconds/$Math.mod($variants.length);
					$variant = $variants.get($index);
					
					// Se guarda para futuras consultas, la variante del art�culo en la p�gina
					$this->current_variant($idArt) = $variant;
				}
			}
			
			// Puede ser que no exista experimento para dicho art�culo
			return $variant;
		}
	}
	
	$ITR_SplitTest = new ITR_SplitTest(); 
?>
