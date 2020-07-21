// Inicializa el objeto que contiene las m�tricas para el script de traking de Piwik
var _qap = _qap || [];

//////////////////////////////////////////////////////////////////////////////
//                                                                          //
// Controlador de env�o de estad�sticas a MAS.                              //
//                                                                          //
//////////////////////////////////////////////////////////////////////////////
function Iter2MAS()
{
	////////////////////////////////////////////////
	//                 CONSTANTES                 //
	////////////////////////////////////////////////
	// Nombre de la cookie para no repetir lecturas
	var ARTICLE_COOKIE_NAME = "iter_article";
	// Acci�n de Piwik para registrar un page view
	var ACTION_PAGE_VIEW = "trackPageView";
	// Acci�n de Piwik para registrar objetivos
	var ACTION_GOAL = "trackGoal";
	// Acci�n de Piwik para registrar eventos
	var ACTION_EVENT = "trackEvent";
	// Id del objetivo "Lectura"
	var READING_GOAL_ID = 1;
	// Id del objetivo "AdBlock"
	var ADBLOCK_GOAL_ID = 2;
	// Id del objetivo "Web Push Notification"
	var WPN_GOAL_ID = 3;
	// Id del objetivo "A�adir art�culo favorito"
	var FAV_ADDED_GOAL_ID = 4;
	// Id del objetivo "Visitar art�culo favorito"
	var FAV_VISIT_GOAL_ID = 5;
	// Id del objetivo "Suscripci�n a temas"
	var FAV_TOPIC_ADDED_GOAL_ID = 6;
	// Id del objetivo "Vista por suscripci�n a tema"
	var FAV_TOPIC_VISIT_GOAL_ID = 7;
	
	////////////////////////////////////////////////
	//        Datos de la p�gina visitada         //
	////////////////////////////////////////////////
	var pageData = {
			type: "",
			url: "",
			title: "",
			uid: "",
			searchPrefix: ""
		};
	
	////////////////////////////////////////////////
	//        Datos espec�ficos del HIT           //
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
	//                      FUNCIONES DE INICIALIZACI�N                       //
	////////////////////////////////////////////////////////////////////////////
	
	// Iniciaiza el objeto Iter2MAS y lo retorna
	this.initialize = function(config)
	{
		// Establece el tipo de p�gina (detail, meta, mainSection, search)
		pageData.type = config.pageType;
		
		// Establece la url de la p�gina
		pageData.url = getUrl();
		
		// Establece el t�tulo de la p�gina
		pageData.title = document.title;
		
		// Establece el prefijo de las p�ginas de b�squeda
		pageData.searchPrefix = config.searchPrefix;
		
		// Establece el identificador �nico del usuario que genera la petici�n
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
		
		// La petici�n ser� correcta cuando el par�metro uid valga exactamente lo mismo que el usrid del usuario registrado en ITER o, 
		// si el usuario no est� logueado en ITER, cuando el par�metro no exista en la petici�n.
		// http://jira.protecmedia.com:8080/browse/ITER-639?focusedCommentId=23856&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-23856
		if (pageData.uid.indexOf('ITER_USER_ID') < 0)
			_qap.push(['setUserId', pageData.uid]);         // ID de usuario
		
		// Ejecuta el callback onInitialize
		jQryIter.hooks.mas.onInitialize.apply(this, [config, pageData, hitData]);
		
		return this;
	};
	
	////////////////////////////////////////////////////////////////////////////
	//            FUNCIONES P�BLICAS PARA ENVIAR LAS ESTAD�STICAS             //
	////////////////////////////////////////////////////////////////////////////
	
	// Env�a las estad�sticas de visitas
	this.sendVisitHit = function()
	{
		// Si es un detalle de art�culo, establece el ID con matr�cula en la dimensi�n 2
		if ("detail" === pageData.type)
		{
			hitData.dimensions.dimension1 = "Article";
			hitData.dimensions.dimension2 = "Article: " + getArticleId();
			hitData.dimensions.metadata = getMetaKeywords();
		}
		else
		{
			// La p�gina no es un detalle de art�culo. Limpia la cookie.
			cleanArticleCookie();
			
			// Establece las dimensiones en funci�n del tipo de p�gina
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
		
		// A�ade el hit de lectura al objeto global _qap
		_qap.push([ ACTION_PAGE_VIEW,
		            pageData.title,
		            hitData.dimensions,
		            function() {
						jQryIter.hooks.mas.afterPageview.apply(this, [pageData, hitData]);
					}
		]);
	};
	
	// Env�a las estad�sticas de lectura
	this.sendReadStatistics = function()
	{
		// Comprueba si la �ltima lectura que se envi� corresponde al mismo art�culo. Si es as�, no repite la lectura.
		if (getArticleCookie() !== document.URL)
        {
			// A�ade el hit de objetivo de lectura al objeto global _qap
			_qap.push([ ACTION_GOAL, READING_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
			
			// Establece la URL del art�culo en la cookie para no repetir lecturas en los refrescos de la p�gina
			setArticleCookie(document.URL);
        }
	};
	
	// Env�a las estad�ticas de adBlock
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
		
		// A�ade el hit de objetivo de adBlock al objeto global _qap
		_qap.push([ ACTION_GOAL, ADBLOCK_GOAL_ID, 0, { adblockUse: status, pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Env�a la notificaci�n de suscripci�n a notificaciones web
	this.notifySubscriptionToNotifications = function()
	{
		// A�ade el hit de objetivo de adBlock al objeto global _qap
		_qap.push([ ACTION_GOAL, WPN_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Env�a el hit de objetivo de "Art�culo a�adido a favoritos"
	this.notifyFavoriteArticleAdded = function()
	{
		_qap.push([ ACTION_GOAL, FAV_ADDED_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Env�a el hit de objetivo de "Art�culo favorito visitado"
	this.notifyFavoriteArticleVisited = function()
	{
		_qap.push([ ACTION_GOAL, FAV_VISIT_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Env�a el hit de objetivo de "Suscripci�n a temas"
	this.notifyFavoriteTopicAdded = function()
	{
		_qap.push([ ACTION_GOAL, FAV_TOPIC_ADDED_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Env�a el hit de objetivo de "Vista por suscripci�n a tema"
	this.notifyFavoriteTopicArticleVisited = function()
	{
		_qap.push([ ACTION_GOAL, FAV_TOPIC_VISIT_GOAL_ID, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
	};
	
	// Env�a el objetivo indicado en idGoal
	this.sendGoal = function(idGoal)
	{
		if (typeof idGoal === 'number' && idGoal > -1) {
			_qap.push([ ACTION_GOAL, idGoal, 0, { pv_epoch: hitData.dimensions.pv_epoch } ]);
		} else {
			console.warn("sendGoal: Invalid args");
		}
	};
	
	// Env�a el evento indicado
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

	// Recupera la URL de la p�gina
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
	
	// Recupera el Id del art�culo
	function getArticleId()
	{
		return pageData.url.substr(pageData.url.lastIndexOf('-') + 1);
	}
	
	// Recupera el metadato de la p�gina
	function getMetadata()
	{
		return decodeURIComponent(pageData.url.substr(pageData.url.lastIndexOf('/') + 1));
	}
	
	// Recupera los t�rminos de b�squeda de la p�gina
	function getSearchTerms()
	{
		var index = pageData.url.indexOf(pageData.searchPrefix) + pageData.searchPrefix.length;
		var aux = pageData.url.slice(index);
		
		return decodeURIComponent(aux.substring(0, aux.indexOf('/'))).trim();
	}
	
	// Recupera los meta keywords del detalle de un art�culo
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
	
	// Vac�a la cookie ARTICLE_COOKIE_NAME
	function cleanArticleCookie()
	{
		if (getArticleCookie() !== "")
            setArticleCookie("");
	}
}