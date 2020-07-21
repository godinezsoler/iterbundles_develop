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
	
	// Si el art�culo est� restringido, no procesa lecturas 
	if (jQryIter(PORTLET_NO_ACCESS_CONTENT).length > 0)
		return;
	
	// Id del grupo para el env�o de la estad�stica
	var group = groupId;
	// Id del art�culo para el env�o de la estad�stica
	var article = articleId;
	
	// Posici�n del final del art�culo
	var articleBottom = 0;
	
	// Tiempo m�nimo de lectura
	var wordsPerMinute = wpm;
	var articleWordsCount = 0;
	var minTimeToRead = 0;
	// Cuando se encuentren content-viewers marcados, se contabiliza el texto de todos ellos.
	var milestoneFound = true;
	
	// Flag de tiempo m�nimo de lectura cumplido
	var article_readingTime = false;
	// Flag de fin del texto del art�culo alcanzado
	var article_bottomContentReached = false;
	
	///////////////////////////////////////////////////////////////////
	//                        INICIALIZACI�N                         //
	///////////////////////////////////////////////////////////////////
	
	// Busca el content-viewer leg�timo e inicializa los par�metros
	getArticle();
	
	// A�ade el evento "onScrollEnd"
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
	// Establece la comprobaci�n de fin de art�culo
	jQryIter(window).scrollEnd(bottomArticleReached, 200);
	// Comprueba una al inicio si se lleg� al final del art�culo (Para cuando no hay scroll)
	bottomArticleReached();

	///////////////////////////////////////////////////////////////////
	//                            M�TODOS                            //
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
				// A�ade el contenido al c�lculo del tiempo m�nimo de lectura
				var contentWithoutScripts = jQryIter( content ).clone().find( "script" ).remove().end();
				articleWordsCount += contentWithoutScripts.text().split(/\s+/).length;
				minTimeToRead = (articleWordsCount / wordsPerMinute) * 60 * 1000;
				
				// Comprueba si es el final del art�culo
				articleBottom = contentBottom > articleBottom ? contentBottom : articleBottom;
			}
			else
			{
				// Si est� visible, es un candidato v�lido
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
						// Calcula el n�mero de palabras del contenido
						var contentWordsCount = contentText.split(/\s+/).length;
						// Si el n�mero de palabras es mayor que el �ltimo dado por definitivo, se queda con este
						if (contentWordsCount > articleWordsCount)
						{
							// Actualiza el n�mero de palabas del art�culo
							articleWordsCount = contentWordsCount;
							// Calcula el tiempo m�nimo de lectura
							minTimeToRead = (articleWordsCount / wordsPerMinute) * 60 * 1000;
							// Calcula la posici�n del final del art�culo
							articleBottom = contentBottom;
						}
					}
				}
			}
		});
	}
	
	// Activa el flag de tiempo m�nimo de lectura vencido y verifica si ya se hab�a
	// alcanzado el final del art�culo.
	// Es lanzado por el timeout de tiempo minimo de lectura
	function minReadingTimeExpired()
	{
		article_readingTime = true;
		checkRead();
	}
	
	// Comprueba si se ha alcanzado el final del art�culo y, en caso afirmativo, activa
	// el flag correspondiente y verifica si ya se ha complido el tiempo m�nimo de lectura.
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
	
	// comprueba si ambos flags est�n activos y, en caso afirmativo, env�a la confirmaci�n
	// de lectura y desactiva los flags y los eventos.
	function checkRead()
	{
		if (article_readingTime && article_bottomContentReached)
		{
			// Env�a el aviso de lectura a Iter si est�n activadas las estad�sticas
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
			
			// Env�a las estad�sticas a MAS
			sendStatisticsToMAS();

			// Elimina el evento
			article_readingTime = false;
			article_bottomContentReached = false;
		}
	}
	
	// Comprueba si se ha inicializado el controlador de env�o de estad�ticas 
	// a MAS y, si es as�, env�a la confirmaci�n de lectura.
	function sendStatisticsToMAS()
	{
		if (typeof MASStatsMgr != 'undefined')
		{
			MASStatsMgr.sendReadStatistics();
		}
	}
}