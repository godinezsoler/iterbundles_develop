function ArticleReadingController(groupId, articleId, wpm, iterStatsEnabled)
{
	// Selector de contenidos marcado
	var MILESTONE_CONTENT_SELECTOR   = '.content-viewer-read-milestone';
	// Selector de contenido
	var PORTLET_CONTENT_SELECTOR     = ".portlet-content,.portlet-boundary";
	// Selector de contenido auxiliar
	var ALTERNATIVE_CONTENT_SELECTOR = '.content-viewer-portlet.portlet-boundary,.content-viewer-portlet .portlet-content';
	// Selector de contenido restringifo
	var PORTLET_NO_ACCESS_CONTENT    = '.content-viewer-portlet.portlet-boundary.no-access,.content-viewer-portlet .portlet-content div.no-access';
	
	// Si el artículo está restringido, no procesa lecturas 
	if (jQryIter(PORTLET_NO_ACCESS_CONTENT).length > 0)
		return;
	
	// Id del grupo para el envío de la estadística
	var group = groupId;
	// Id del artículo para el envío de la estadística
	var article = articleId;
	
	// Posición del final del artículo
	var articleBottom = 0;
	
	// Tiempo mínimo de lectura
	var wordsPerMinute = wpm;
	var articleWordsCount = 0;
	var minTimeToRead = 0;
	// Cuando se encuentren content-viewers marcados, se contabiliza el texto de todos ellos.
	var milestoneFound = true;
	
	// Flag de tiempo mínimo de lectura cumplido
	var article_readingTime = false;
	// Flag de fin del texto del artículo alcanzado
	var article_bottomContentReached = false;
	
	///////////////////////////////////////////////////////////////////
	//                        INICIALIZACIÓN                         //
	///////////////////////////////////////////////////////////////////
	
	// Busca el content-viewer legítimo e inicializa los parámetros
	getArticle();
	
	// Añade el evento "onScrollEnd"
	jQryIter.fn.scrollEnd = function(callback, timeout) {          
	  jQryIter(this).on('scroll', function(){
	    var $this = jQryIter(this);
	    if ($this.data('scrollTimeout')) {
	      clearTimeout($this.data('scrollTimeout'));
	    }
	    $this.data('scrollTimeout', setTimeout(callback,timeout));
	  });
	};

	// Establece el timeout de lectura
	setTimeout(minReadingTimeExpired, minTimeToRead);
	// Establece la comprobación de fin de artículo
	jQryIter(window).scrollEnd(bottomArticleReached, 200);
	// Comprueba una al inicio si se llegó al final del artículo (Para cuando no hay scroll)
	bottomArticleReached();

	///////////////////////////////////////////////////////////////////
	//                            MÉTODOS                            //
	///////////////////////////////////////////////////////////////////
	
	function getArticle()
	{
		var candidates = jQryIter(MILESTONE_CONTENT_SELECTOR).closest(PORTLET_CONTENT_SELECTOR);
		if (candidates.length == 0)
		{
			candidates = jQryIter(ALTERNATIVE_CONTENT_SELECTOR);
			milestoneFound = false;
		}
		
		jQryIter.each(candidates, function(index, item)
		{
			var content = jQryIter(item);
			var contentTop = content.offset().top;
			var contentBottom = contentTop + content.height();
			
			if (milestoneFound)
			{
				// Añade el contenido al cálculo del tiempo mínimo de lectura
				var contentWithoutScripts = jQryIter( content ).clone().find( "script" ).remove().end();
				articleWordsCount += contentWithoutScripts.text().split(/\s+/).length;
				minTimeToRead = (articleWordsCount / wordsPerMinute) * 60 * 1000;
				
				// Comprueba si es el final del artículo
				articleBottom = contentBottom > articleBottom ? contentBottom : articleBottom;
			}
			else
			{
				// Si está visible, es un candidato válido
				if (content.is(":visible"))
				{
					// Elimina los scripts del contenido
					var contentWithoutScripts = jQryIter( content ).clone().find( "script" ).remove().end();
					// Recupera el texto del contenido
					var contentText = contentWithoutScripts.text();
					if (contentText != null) contentText = contentText.trim();
					// Si el contenido tiene texto, comprueba las palabras
					if (contentText)
					{
						// Calcula el número de palabras del contenido
						var contentWordsCount = contentText.split(/\s+/).length;
						// Si el número de palabras es mayor que el último dado por definitivo, se queda con este
						if (contentWordsCount > articleWordsCount)
						{
							// Actualiza el número de palabas del artículo
							articleWordsCount = contentWordsCount;
							// Calcula el tiempo mínimo de lectura
							minTimeToRead = (articleWordsCount / wordsPerMinute) * 60 * 1000;
							// Calcula la posición del final del artículo
							articleBottom = contentBottom;
						}
					}
				}
			}
		});
	}
	
	// Activa el flag de tiempo mínimo de lectura vencido y verifica si ya se había
	// alcanzado el final del artículo.
	// Es lanzado por el timeout de tiempo minimo de lectura
	function minReadingTimeExpired()
	{
		article_readingTime = true;
		checkRead();
	}
	
	// Comprueba si se ha alcanzado el final del artículo y, en caso afirmativo, activa
	// el flag correspondiente y verifica si ya se ha complido el tiempo mínimo de lectura.
	// Es lanzado por el evento "onScrollEnd".
	function bottomArticleReached()
	{
		var docWindow = jQryIter(window);
		var docViewTop = docWindow.scrollTop();
		var docViewBottom = docViewTop + docWindow.height();
		
		if (articleBottom <= docViewBottom)
			article_bottomContentReached = true;
		
		checkRead();
	}
	
	// comprueba si ambos flags están activos y, en caso afirmativo, envía la confirmación
	// de lectura y desactiva los flags y los eventos.
	function checkRead()
	{
		if (article_readingTime && article_bottomContentReached)
		{
			// Envía el aviso de lectura a Iter si están activadas las estadísticas
			if (iterStatsEnabled)
			{
				jQryIter.ajax(
		 		{
		 			type: 'GET',
		 			url: "/news-portlet/html/counter-portlet/visit.jsp",
		 			method: 'POST',
		 			data: "groupId=" + group + "&articleId=" + article + "&urlType=readArticle"
		 		});
			}
			
			// Envía las estadísticas a MAS
			sendStatisticsToMAS();

			// Elimina el evento
			article_readingTime = false;
			article_bottomContentReached = false;
		}
	}
	
	// Comprueba si se ha inicializado el controlador de envío de estadíticas 
	// a MAS y, si es así, envía la confirmación de lectura.
	function sendStatisticsToMAS()
	{
		if (typeof MASStatsMgr != 'undefined')
		{
			MASStatsMgr.sendReadStatistics();
		}
	}
}