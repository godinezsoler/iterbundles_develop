// Inicializa el objeto que contiene las métricas para el script de traking de Piwik
var _qap = _qap || [];

//////////////////////////////////////////////////////////////////////////////
//                                                                          //
// Controlador de envío de estadísticas a MAS.                              //
//                                                                          //
//////////////////////////////////////////////////////////////////////////////
function Iter2MAS()
{
	////////////////////////////////////////////////
	//                 CONSTANTES                 //
	////////////////////////////////////////////////
	// Nombre de la cookie para no repetir lecturas
	var ARTICLE_COOKIE_NAME = "iter_article";
	// Acción de Piwik para registrar un page view
	var ACTION_PAGE_VIEW = "trackPageView";
	// Acción de Piwik para registrar objetivos
	var ACTION_GOAL = "trackGoal";
	// Acción de Piwik para registrar eventos
	var ACTION_EVENT = "trackEvent";
	// Id del objetivo "Lectura"
	var READING_GOAL_ID = 1;
	// Id del objetivo "AdBlock"
	var ADBLOCK_GOAL_ID = 2;
	// Id del objetivo "Web Push Notification"
	var WPN_GOAL_ID = 3;
	// Id del objetivo "Añadir artículo favorito"
	var FAV_ADDED_GOAL_ID = 4;
	// Id del objetivo "Visitar artículo favorito"
	var FAV_VISIT_GOAL_ID = 5;
	// Id del objetivo "Suscripción a temas"
	var FAV_TOPIC_ADDED_GOAL_ID = 6;
	// Id del objetivo "Vista por suscripción a tema"
	var FAV_TOPIC_VISIT_GOAL_ID = 7;
	
	////////////////////////////////////////////////
	//        Datos de la página visitada         //
	////////////////////////////////////////////////
	var pageData = {
			type: "",
			url: "",
			title: "",
			uid: "",
			searchPrefix: ""
		};
	
	////////////////////////////////////////////////
	//        Datos específicos del HIT           //
	////////////////////////////////////////////////
	var hitData = {
			piwikUrl: "",
			siteId: "",
			dimensions: {
				dimension1: "",
				dimension2: "",
				pv_epoch: "",
				metadata: []
			}
		};
	
	////////////////////////////////////////////////////////////////////////////
	//                      FUNCIONES DE INICIALIZACIÓN                       //
	////////////////////////////////////////////////////////////////////////////
	
	// Iniciaiza el objeto Iter2MAS y lo retorna
	this.initialize = function(config)
	{
		// Establece el tipo de página (detail, meta, mainSection, search)
		pageData.type = config.pageType;
		
		// Establece la url de la página
		pageData.url = getUrl();
		
		// Establece el título de la página
		pageData.title = document.title;
		
		// Establece el prefijo de las páginas de búsqueda
		pageData.searchPrefix = config.searchPrefix;
		
		// Establece el identificador único del usuario que genera la petición
		pageData.uid = config.uid;
	
		// Establece la URL de piwik
		hitData.piwikUrl = config.piwikUrl;
		
		// Establece el siteId
		hitData.siteId = config.siteId;
		
		// Establece la fecha del HIT
		hitData.dimensions.pv_epoch = new Date().getTime();
		
		// Inicializa el objeto global _qap
		_qap.push(["setTrackerUrl", hitData.piwikUrl]); // URL a la que mandar el HIT
		_qap.push(["setSiteId", hitData.siteId]);       // ID del sitio
		
		// La petición será correcta cuando el parámetro uid valga exactamente lo mismo que el usrid del usuario registrado en ITER o, 
		// si el usuario no está logueado en ITER, cuando el parámetro no exista en la petición.
		// http://jira.protecmedia.com:8080/browse/ITER-639?focusedCommentId=23856&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-23856
		if (pageData.uid.indexOf('ITER_USER_ID') < 0)
			_qap.push(['setUserId', pageData.uid]);         // ID de usuario
		
		// Ejecuta el callback onInitialize
		jQryIter.hooks.mas.onInitialize.apply(this, [config, pageData, hitData]);
		
		return this;
	};
	
	////////////////////////////////////////////////////////////////////////////
	//            FUNCIONES PÚBLICAS PARA ENVIAR LAS ESTADÍSTICAS             //
	////////////////////////////////////////////////////////////////////////////
	
	// Envía las estadísticas de visitas
	this.sendVisitHit = function()
	{
		// Si es un detalle de artículo, establece el ID con matrícula en la dimensión 2
		if ("detail" === pageData.type)
		{
			hitData.dimensions.dimension1 = "Article";
			hitData.dimensions.dimension2 = "Article: " + getArticleId();
			hitData.dimensions.metadata = getMetaKeywords();
		}
		else
		{
			// La página no es un detalle de artículo. Limpia la cookie.
			cleanArticleCookie();
			
			// Establece las dimensiones en función del tipo de página
			switch (pageData.type)
			{
				case "mainSection":
					hitData.dimensions.dimension1 = "Section";
					hitData.dimensions.dimension2 = "Section: " + pageData.title;
				break;
				
				case "meta":
					hitData.dimensions.dimension1 = "Metadata";
					hitData.dimensions.dimension2 = "Metadata: " + getMetadata();
				break;
				
				case "search":
					hitData.dimensions.dimension1 = "Search";
	                hitData.dimensions.dimension2 = "Terms: " + getSearchTerms();
				break;
			}
		}
		
		// Ejecuta el callback onInitialize
		jQryIter.hooks.mas.beforePageview.apply(this, [pageData, hitData]);
		
		// Añade el hit de lectura al objeto global _qap
		_qap.push([ ACTION_PAGE_VIEW,
		            pageData.title,
		            hitData.dimensions,
		            function() {
						jQryIter.hooks.mas.afterPageview.apply(this, [pageData, hitData]);
					}
		]);
	};
	
	// Envía las estadísticas de lectura
	this.sendReadStatistics = function()
	{
		// Comprueba si la última lectura que se envió corresponde al mismo artículo. Si es así, no repite la lectura.
		if (getArticleCookie() !== document.URL)
        {
			// Añade el hit de objetivo de lectura al objeto global _qap
			_qap.push([ ACTION_GOAL, READING_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
			
			// Establece la URL del artículo en la cookie para no repetir lecturas en los refrescos de la página
			setArticleCookie(document.URL);
        }
	};
	
	// Envía las estadíticas de adBlock
	this.sendAdBlockStatistics = function(hadAdBlock, hasAdBlock)
	{
		// Calcula el estado
		var status = "No usa";
		if (hasAdBlock === 1)
		{
			status = "Activado";
		}
		else if (hadAdBlock === 1)
		{
			status = "Desactivado";
		}
		
		// Añade el hit de objetivo de adBlock al objeto global _qap
		_qap.push([ ACTION_GOAL, ADBLOCK_GOAL_ID, 0, { adblockUse: status, pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Envía la notificación de suscripción a notificaciones web
	this.notifySubscriptionToNotifications = function()
	{
		// Añade el hit de objetivo de adBlock al objeto global _qap
		_qap.push([ ACTION_GOAL, WPN_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Envía el hit de objetivo de "Artículo añadido a favoritos"
	this.notifyFavoriteArticleAdded = function()
	{
		_qap.push([ ACTION_GOAL, FAV_ADDED_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Envía el hit de objetivo de "Artículo favorito visitado"
	this.notifyFavoriteArticleVisited = function()
	{
		_qap.push([ ACTION_GOAL, FAV_VISIT_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Envía el hit de objetivo de "Suscripción a temas"
	this.notifyFavoriteTopicAdded = function()
	{
		_qap.push([ ACTION_GOAL, FAV_TOPIC_ADDED_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Envía el hit de objetivo de "Vista por suscripción a tema"
	this.notifyFavoriteTopicArticleVisited = function()
	{
		_qap.push([ ACTION_GOAL, FAV_TOPIC_VISIT_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Envía el objetivo indicado en idGoal
	this.sendGoal = function(idGoal)
	{
		if (typeof idGoal === 'number' && idGoal > -1) {
			_qap.push([ ACTION_GOAL, idGoal, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
		} else {
			console.warn("sendGoal: Invalid args");
		}
	};
	
	// Envía el evento indicado
	this.sendEvent = function(category, action, name)
	{
		if (!category || !action || !name) {
			console.warn("sendEvent: Invalid args");
			return;
		}
		_qap.push([ ACTION_EVENT, category, action, name ]);
	};
	
	////////////////////////////////////////////////////////////////////////////
	//                          FUNCIONES AUXILIARES                          //
	////////////////////////////////////////////////////////////////////////////

	// Recupera la URL de la página
	function getUrl()
	{
		// Prueba a recuperar la URL canonica
		var url = document.querySelector('link[rel="canonical"]');
		if (url != null && url.href)
			return url.href;
		
		// Si no puede, intenta buscarla en el meta de open graph
		url = document.querySelector('meta[property="og:url"]');
		if (url != null && url.content)
			return url.content;
		
		// Si no la encuentra, retorna la url del navegador
		return window.location.href;
	}
	
	// Recupera el Id del artículo
	function getArticleId()
	{
		return pageData.url.substr(pageData.url.lastIndexOf('-') + 1);
	}
	
	// Recupera el metadato de la página
	function getMetadata()
	{
		return decodeURIComponent(pageData.url.substr(pageData.url.lastIndexOf('/') + 1));
	}
	
	// Recupera los términos de búsqueda de la página
	function getSearchTerms()
	{
		var index = pageData.url.indexOf(pageData.searchPrefix) + pageData.searchPrefix.length;
		var aux = pageData.url.slice(index);
		
		return decodeURIComponent(aux.substring(0, aux.indexOf('/'))).trim();
	}
	
	// Recupera los meta keywords del detalle de un artículo
	function getMetaKeywords()
	{
		var metadata = [];
		jQryIter("meta[name=keywords]").each( function()
		{
			metadata.push(
				{
					"cn":  jQryIter(this).attr("content"),
					"cid": jQryIter(this).attr("data-id"),
					"vn":  jQryIter(this).attr("data-voc-name"), 
					"vid": jQryIter(this).attr("data-voc-id")
				}
			);
		});
		return metadata;
	}
	
	// Establece la cookie ARTICLE_COOKIE_NAME
	function setArticleCookie(value)
	{
		return jQryIter.cookie(ARTICLE_COOKIE_NAME, value, { path: '/' });
	}
	
	// Recupera el valor de la cookie ARTICLE_COOKIE_NAME
	function getArticleCookie()
  	{
     	return jQryIter.cookie(ARTICLE_COOKIE_NAME);
    }
	
	// Vacía la cookie ARTICLE_COOKIE_NAME
	function cleanArticleCookie()
	{
		if (getArticleCookie() !== "")
            setArticleCookie("");
	}
}