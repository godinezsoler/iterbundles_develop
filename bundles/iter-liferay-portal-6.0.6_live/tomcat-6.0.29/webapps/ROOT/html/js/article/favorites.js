var ITER = ITER || {};
ITER.FAVORITE = ITER.FAVORITE || {};


ITER.FAVORITE.CORE = ITER.FAVORITE.CORE || {};

ITER.FAVORITE.CORE.apirequest = function(method, url, successCallback, errorCallback)
{
	var xhr = new XMLHttpRequest();
	xhr.open(method, url);
	xhr.onload = function()
	{
		if (xhr.status === 200)
		{
			if (typeof successCallback === 'function')
				successCallback(xhr);
		}
		else
		{
			if (typeof errorCallback === 'function')
				errorCallback(xhr);
		}
	};
	xhr.send();
};

ITER.FAVORITE.CORE.findArticleById = function(list, articleid)
{
	return list.find(
		function(article)
		{
			return article.id == this;
		},
		articleid
	);
};

ITER.FAVORITE.CORE.render = function(articleid, templateid, container)
{
	var url = "/news-portlet/renderArticle/" + articleid + "/" + btoa(templateid);
	jQryIter.ajax({url:url, dataType:'html',
		success: function(data, textStatus, jqXHR)
		{
			container.append(data);
		}
	});
};


/*********************************
 *      ARTICULOS FAVORITOS      *
 *********************************/
ITER.FAVORITE.ARTICLES = ITER.FAVORITE.ARTICLES || {};

ITER.FAVORITE.ARTICLES.list = null;

//Comprueba que est� habilitada la funcionalidad de favoritos y que haya usuario autenticado
ITER.FAVORITE.ARTICLES.isEnabled = function()
{
	return jQryIter.isFavoriteArticlesEnabled() && jQryIter.isUserAuthenticated();
};

ITER.FAVORITE.ARTICLES.onLoad = function(successCallback, errorCallback)
{
	if (ITER.FAVORITE.ARTICLES.list === null)
	{
		ITER.FAVORITE.ARTICLES.load(successCallback, errorCallback);
	}
	else
	{
		if (typeof successCallback === 'function')
			successCallback();
	}
};

ITER.FAVORITE.ARTICLES.load = function(successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.ARTICLES.isEnabled())
		return;

	var url = "/restapi/user/favorite/articles/get/" + jQryIter.u;
	ITER.FAVORITE.CORE.apirequest("GET", url,
		function(xhr)
		{
			var response = JSON.parse(xhr.responseText);
			ITER.FAVORITE.ARTICLES.list = [];
			for (var i in response.user.articles)
			{
				ITER.FAVORITE.ARTICLES.list.push(response.user.articles[i]);
			}
			
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

ITER.FAVORITE.ARTICLES.get = function()
{
	return ITER.FAVORITE.ARTICLES.list === null ? [] : ITER.FAVORITE.ARTICLES.list;
};

ITER.FAVORITE.ARTICLES.isFavorite = function(articleid)
{
	return typeof ITER.FAVORITE.CORE.findArticleById(ITER.FAVORITE.ARTICLES.list, articleid) !== 'undefined';
};

ITER.FAVORITE.ARTICLES.count = function()
{
	return ITER.FAVORITE.ARTICLES.list === null ? 0 : ITER.FAVORITE.ARTICLES.list.length;
};

// A�ade un art�culo a los favoritos
ITER.FAVORITE.ARTICLES.add = function(articleid, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.ARTICLES.isEnabled())
		return;
	
	// Llama a la API para a�adir el art�culo
	var url = "/restapi/user/favorite/articles/add/" + articleid;
	ITER.FAVORITE.CORE.apirequest("POST", url,
		// Si se a�adi� correctamente
		function()
		{
			// Incluye su id temporalmente en el listado de art�culos favoritos
			ITER.FAVORITE.ARTICLES.list.push({"crc":"", "id": articleid});
			
			// Comprueba si se ha inicializado el controlador de env�o de estad�ticas a MAS y,
			// si es as�, env�a el objetivo de "Art�culo a�adido a favoritos".
			if (typeof MASStatsMgr !== 'undefined')
			{
				MASStatsMgr.notifyFavoriteArticleAdded();
			}
			
			// Ejecuta la funci�n callback si se defini�
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

// Elimina un art�culo de los favoritos del usuario
ITER.FAVORITE.ARTICLES.remove = function(articleid, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.ARTICLES.isEnabled())
		return;
	
	// Llama a la API para eliminar el art�culo
	var url = "/restapi/user/favorite/articles/remove/" + articleid;
	ITER.FAVORITE.CORE.apirequest("POST", url,
		// Si se elimin� correctamente
		function()
		{
			// Busca el art�culo
			var article = ITER.FAVORITE.CORE.findArticleById(ITER.FAVORITE.ARTICLES.list, articleid);
			// Lo elimina del listado de favoritos
			ITER.FAVORITE.ARTICLES.list.splice(ITER.FAVORITE.ARTICLES.list.indexOf(article), 1);
			// Ejecuta la funci�n callback si se defini�
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

ITER.FAVORITE.ARTICLES.show = function(templateid, container)
{
	if (typeof templateid === 'undefined' || !templateid)
	{
		console.log("Parameter 'templateid' is empty");
		return;
	}
	
	if (typeof container === 'undefined' || !(container instanceof jQuery))
	{
		console.log("Parameter 'container' is not a JQuery object");
		return;
	}
	
	if (!jQryIter.u)
	{
		console.log("There is no authenticated user");
		return;
	}
	
	ITER.FAVORITE.ARTICLES.onLoad(
		function()
		{
			for (var i in ITER.FAVORITE.ARTICLES.list)
			{
				var favoriteContainer = jQryIter("<div></div>");
				jQryIter(container).append(favoriteContainer);
				// Compone el id del art�culo con su firma
				var signedArticleId = ITER.FAVORITE.ARTICLES.list[i].crc + ITER.FAVORITE.ARTICLES.list[i].id;
				ITER.FAVORITE.CORE.render(signedArticleId, templateid, favoriteContainer);
			}
		},
		function() { console.log("Unexpected error rendering favorite articles"); }
	);
};

/*********************************
 *        TEMAS FAVORITOS        *
 *********************************/
ITER.FAVORITE.TOPICS = ITER.FAVORITE.TOPICS || {};

/**
 * Flag que indica que se han cargado los datos del servidor.
 */
ITER.FAVORITE.TOPICS.loaded = false;

ITER.FAVORITE.TOPICS.isEnabled = function()
{
	return jQryIter.isFavoriteTopicsEnabled() && jQryIter.isUserAuthenticated();
};

/**
 * Permite a�adir funciones que se ejecutan tras la carga (correcta o fallida) de los datos de temas favoritos.
 * @param successCallback Funci�n a ejecutar tras la carga correcta de temas favoritos.
 * @param errorCallback Funci�n a ejecutar si no se pueden cargar los datos de temas favoritos.
 */
ITER.FAVORITE.TOPICS.onLoad = function(successCallback, errorCallback)
{
	if (ITER.FAVORITE.TOPICS.loaded)
	{
		if (typeof successCallback === 'function')
			successCallback();
	}
	else
	{
		ITER.FAVORITE.TOPICS.load(successCallback, errorCallback);
	}
};

/**
 * Carga los datos de temas favoritos y art�culos pendientes del usuario autenticado desde el servidor.
 * @param successCallback Funci�n a ejecutar tras la carga correcta de temas favoritos.
 * @param errorCallback Funci�n a ejecutar si no se pueden cargar los datos de temas favoritos.
 */
ITER.FAVORITE.TOPICS.load = function(successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	// Carga los datos almacenados en local
	ITER.FAVORITE.TOPICS.DATA.load();
	
	// Carga los datos del servidor
	var url = "/restapi/user/favorite/topics/get/" + jQryIter.u;
	ITER.FAVORITE.CORE.apirequest("GET", url,
		function(xhr)
		{
			// Recupera los datos del servidor
			var response = JSON.parse(xhr.responseText);
			// Los actualiza en local si fuese necesario
			ITER.FAVORITE.TOPICS.DATA.merge(response.user);
			// Indica que finaliz� la carga de datos
			ITER.FAVORITE.TOPICS.loaded = true;
			// Ejecuta la funci�n de callback si se indic� alguna
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/**
 * Retorna la lista de temas favoritos del usuario.
 * @returns {Array} con la lista de temas favoritos del usuario y sus art�culos pendientes.
 */
ITER.FAVORITE.TOPICS.get = function()
{
	var topics = [];
	for (var t in ITER.FAVORITE.TOPICS.DATA.topics)
	{
		// A�ade todos los temas que no sean el "leer m�s tarde"
		if (ITER.FAVORITE.TOPICS.DATA.topics[t].id > 0)
			topics.push(ITER.FAVORITE.TOPICS.DATA.topics[t]);
	}
	return topics;
};

/**
 * Comprueba si el tema es uno de los favoritos del usuario autenticado.
 * @param categoryid El id del tema.
 * @returns {Boolean} true si est� incluido en los favoritos del usuario, false en caso contrario.
 */
ITER.FAVORITE.TOPICS.isFavorite = function(id)
{
	if (typeof ITER.FAVORITE.TOPICS.DATA.findById(id) === 'undefined')
		return false;
	
	return true;
};

/**
 * A�ade un tema a los favoritos del usuario autenticado. Si est� configurada la integraci�n con MAS,
 * env�a un hit al objetivo "Suscripci�n a temas favoritos".
 * @param id El identificador del tema.
 * @param successCallback Funci�n a ejecutar tras a�adir correctamente el tema a favoritos.
 * @param errorCallback Funci�n a ejecutar si no se pudo a�adir el tema a favoritos.
 */
ITER.FAVORITE.TOPICS.add = function(id, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/topics/add/" + id,
		function()
		{
			// Recupera el nombre del metadato
			var metaInfo = jQryIter("*[data-categoryid='" + id + "'][data-categoryname]");
			var name = metaInfo.size() > 0 ? metaInfo.attr("data-categoryname") : "";
			
			// A�ade el tema en la configuraci�n local
			ITER.FAVORITE.TOPICS.DATA.addTopic(id, name);
			
			// Comprueba si se ha inicializado el controlador de env�o de estad�ticas a MAS y,
			// si es as�, env�a el objetivo de "Suscripci�n a temas".
			if (typeof MASStatsMgr !== 'undefined')
			{
				MASStatsMgr.notifyFavoriteTopicAdded();
			}
			
			// Ejecuta la funci�n de callback si se indic� alguna
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/**
 * Elimina un tema de los favoritos del usuario autenticado.
 * @param id El identificador del tema.
 * @param successCallback Funci�n a ejecutar tras eliminar correctamente el tema de favoritos.
 * @param errorCallback Funci�n a ejecutar si no se pudo eliminar el tema de favoritos.
 */
ITER.FAVORITE.TOPICS.remove = function(id, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/topics/remove/" + id,
		function()
		{
			// Elimina el tema de la configuraci�n local
			ITER.FAVORITE.TOPICS.DATA.removeTopic(id);
			
			// Ejecuta la funci�n de callback si se indic� alguna
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

///////////////////////////////////////
//                                   //
//   ART�CULOS PENDIENTES DE LEER    //
//                                   //
///////////////////////////////////////

ITER.FAVORITE.TOPICS.onVisitPendingArticle = null;

/**
 * Retorna la lista de art�culos pendientes del tema indicado.
 * @param topic El tema del que se quieren obtener s�lo sus art�culos pendientes.
 * @returns {Array} Lista de art�culos pendientes del tema.
 */
ITER.FAVORITE.TOPICS.getPendingArticlesFrom = function(topic)
{
	var pendingArticles = [];
	
	for (var a in topic.articles)
	{
		if (topic.articles[a].visited !== true)
			pendingArticles.push(topic.articles[a]);
	}
	
	return pendingArticles;
};

/**
 * Comprueba si un art�culo sugerido est� pendiente de leer por parte del usuario autenticado.
 * @param articleid el identificador del art�culo.
 * @returns {Boolean} true si est� pendiente de leer, false en caso contrario.
 */
ITER.FAVORITE.TOPICS.isPending = function(articleid)
{
	return ITER.FAVORITE.TOPICS.DATA.getArticles(false).indexOf(articleid) >= 0;
};

/**
 * Retorna el n�mero de art�culos pendientes de leer del usuario autenticado.
 * @returns El n�mero de art�culos pendientes de leer.
 */
ITER.FAVORITE.TOPICS.countPendingArticles = function()
{
	return ITER.FAVORITE.TOPICS.DATA.getArticles(false).length;
};

/**
 * Elimina un art�culo del listado de leer m�s tarde.
 * @param articleid el identificador del art�culo.
 * @param successCallback Funci�n a ejecutar tras eliminar correctamente el art�culo del listado de leer m�s tarde.
 * @param errorCallback Funci�n a ejecutar si no se pueden eliminar el art�culo del listado de leer m�s tarde.
 */
ITER.FAVORITE.TOPICS.removePendingArticle = function(articleid, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/articles/removepending/" + articleid, successCallback, errorCallback);
};

/**
 * Marca un art�culo pendiente como "visitado".
 * @param articleid El id del art�culo visitado.
 */
ITER.FAVORITE.TOPICS.markAsRead = function(articleid)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	// Elimina el art�culo del servidor
	ITER.FAVORITE.TOPICS.removePendingArticle(articleid,
		// Si se elimina correctamente, lo marca como le�do en local
		function()
		{
			var changes = false;
			// Busca las ocurrencias del art�culo
			for (t in ITER.FAVORITE.TOPICS.DATA.topics)
			{
				for (a in ITER.FAVORITE.TOPICS.DATA.topics[t].articles)
				{
					var article = ITER.FAVORITE.TOPICS.DATA.topics[t].articles[a];
					// Si es el art�culo indicado, lo marca como le�do
					if (article.id === articleid)
					{
						article.visited = true;
						changes = true;
					}
				}
			}
			
			// Si hubo cambios
			if (changes)
			{
				// Guarda los cambios
				ITER.FAVORITE.TOPICS.DATA.save();
				
				// Ejecuta la funci�n callback si se defini� en el hook
				if (typeof ITER.FAVORITE.TOPICS.onVisitPendingArticle === 'function')
					ITER.FAVORITE.TOPICS.onVisitPendingArticle();
			}
		}
	);
};

///////////////////////////////////////////////////
//                                               //
//         ART�CULOS PARA LEER M�S TARDE         //
//                                               //
///////////////////////////////////////////////////

/**
 * Retorna el listado de los art�culos  que el usuario a�adi� a "leer m�s tarde".
 * @returns {Array} La lista de art�culos pendientes de leer m�s tarde.
 */
ITER.FAVORITE.TOPICS.getReadLaterArticles = function()
{
	var articles = [];
	// Busca el tema instrumental "leer m�s tarde"
	var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);
	if (typeof readLaterTopic !== 'undefined')
	{
		// A�ade s�lo los art�culos pendientes
		for (var a in readLaterTopic.articles)
		{
			var article = readLaterTopic.articles[a];
			if (article.visited !== true)
				articles.push(article);
		}
	}
	return articles;
};

/**
* Comprueba si un art�culo a�adido a "leer m�s tarde" est� pediente de leer por parte del usuario autenticado.
* @param articleid El id del art�culo.
* @returns {Boolean} true si est� pendiente de leer, false en caso contrario.
*/
ITER.FAVORITE.TOPICS.isReadLater = function(articleid)
{
	// Busca la categor�a con id = 0
	var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);

	// Si no existe, retorna falso
	if (typeof readLaterTopic === 'undefined')
		return false;

	// Busca el art�culo en el listado de la categor�a "readLater"
	var article = ITER.FAVORITE.CORE.findArticleById(readLaterTopic.articles, articleid);

	// Si no existe, retorna falso
	if (typeof article === 'undefined')
		return false;

	// Se considera pendiente si a�n no se ha visitado
	return article.visited !== true;
};

/**
 * Retorna el n�mero de art�culos pendientes de leer m�s tarde del usuario autenticado.
 * @returns El n�mero de art�culos pendientes de leer m�s tarde.
 */
ITER.FAVORITE.TOPICS.countReadLaterArticles = function()
{
	return ITER.FAVORITE.TOPICS.getReadLaterArticles().length;
};

/**
* A�ade un art�culo para leer m�s tarde.
* @param articleid el identificador del art�culo.
* @param successCallback Funci�n a ejecutar tras a�adir correctamente el art�culo al listado de leer m�s tarde.
* @param errorCallback Funci�n a ejecutar si no se pueden a�adir el art�culo al listado de leer m�s tarde.
*/
ITER.FAVORITE.TOPICS.readLater = function(articleid, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/articles/readlater/" + articleid,
		// Si se a�adi� correctamente
		function()
		{
			// Recarga la configuraci�n
			ITER.FAVORITE.TOPICS.DATA.load();
			
			// Obtiene el tema "leer m�s tarde". Si no existe, lo crea.
			var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);
			if (typeof readLaterTopic === "undefined")
				readLaterTopic = ITER.FAVORITE.TOPICS.DATA.addTopic(0, "readlater");

			// Busca el art�culo
			var article = ITER.FAVORITE.CORE.findArticleById(readLaterTopic.articles, articleid);

			// Si ya exist�a, le quita el flag "visited"
			if (typeof article !== 'undefined')
				article.visited = false;
			// Si no, a�ade el art�culo
			else
				readLaterTopic.articles.push({"crc":"", "id": articleid});

			// Guarda los cambios
			ITER.FAVORITE.TOPICS.DATA.save();

			// Ejecuta la funci�n callback si se defini�
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/**
* Elimina un art�culo del listado de "leer m�s tarde".
* @param articleid el identificador del art�culo.
* @param successCallback Funci�n a ejecutar tras eliminar correctamente el art�culo del listado de leer m�s tarde.
* @param errorCallback Funci�n a ejecutar si no se pudo eliminar el art�culo del listado de leer m�s tarde.
*/
ITER.FAVORITE.TOPICS.removeReadLater = function(articleid, successCallback, errorCallback)
{
	// Si no est� habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	// Elimina el art�culo del servidor
	ITER.FAVORITE.TOPICS.removePendingArticle(articleid,
		// Si se elimina correctamente, lo borra tambi�n de los datos locales
		function()
		{
			// Recarga la configuraci�n
			ITER.FAVORITE.TOPICS.DATA.load();
			
			// Obtiene el tema "leer m�s tarde"
			var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);
			if (typeof readLaterTopic !== "undefined")
			{
				// Busca el art�culo
				var article = ITER.FAVORITE.CORE.findArticleById(readLaterTopic.articles, articleid);
				if (typeof readLaterTopic !== "undefined")
				{
					// Elimina el art�culo
					readLaterTopic.articles.splice(readLaterTopic.articles.indexOf(article), 1);
				}

				// Guarda los cambios
				ITER.FAVORITE.TOPICS.DATA.save();
			}

			// Ejecuta la funci�n callback si se defini�
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/*********************************
 *         GESTI�N LOCAL         *
 *********************************/
ITER.FAVORITE.TOPICS.DATA = ITER.FAVORITE.TOPICS.DATA || {};

/**
 * Unix time de la �ltima actualizazci�n de datos desde el servidor.
 */
ITER.FAVORITE.TOPICS.DATA.lastUpdate = 0;
/**
 * Temas favoritos del usuario en memoria.
 */
ITER.FAVORITE.TOPICS.DATA.topics = [];

/**
 * Carga la configuraci�n almacenada en local en la variable de trabajo.
 */
ITER.FAVORITE.TOPICS.DATA.load = function()
{
	// Carga los datos del local storage
	var localData = localStorage.getItem("IterPendingArticlesData");
	if (localData !== null)
	{
		localData = JSON.parse(localData);
		// Recupera los datos del usuario autenticado
		var userLocalData = localData[jQryIter.u];
		if (typeof userLocalData !== 'undefined')
		{
			ITER.FAVORITE.TOPICS.DATA.lastUpdate = userLocalData.date;
			ITER.FAVORITE.TOPICS.DATA.topics = userLocalData.topics;
		}
	}
};

/**
 * Recupera un tema buscando por su id. Si no lo encuentra, retorna undefined.
 * @param id El categoryId del tema buscado.
 */
ITER.FAVORITE.TOPICS.DATA.findById = function(id)
{
	return ITER.FAVORITE.TOPICS.DATA.topics.find(
		function(topic)
		{
			return topic.id == this;
		},
		id
	);
};

/**
 * A�ade un tema si no estaba ya inclu�do y guarda los cambios en local.
 * @param id El categoryId del tema.
 * @param name El nombre del tema.
 * @returns El tema creado
 */
ITER.FAVORITE.TOPICS.DATA.addTopic = function(id, name)
{
	ITER.FAVORITE.TOPICS.DATA.load();
	var topic = ITER.FAVORITE.TOPICS.DATA.findById(id);
	if (typeof topic === 'undefined')
	{
		topic = {"id": id, "name": name, articles: []};
		
		ITER.FAVORITE.TOPICS.DATA.topics.push(topic);
		ITER.FAVORITE.TOPICS.DATA.save();
	}
	return topic;
};

/**
 * Elimina un tema favorito y guarda los cambios en local.
 * @param id El categoryId del tema.
 */
ITER.FAVORITE.TOPICS.DATA.removeTopic = function(id)
{
	ITER.FAVORITE.TOPICS.DATA.load();
	var topicToremove = ITER.FAVORITE.TOPICS.DATA.findById(id);
	if (typeof topicToremove !== 'undefined')
	{
		ITER.FAVORITE.TOPICS.DATA.topics.splice(ITER.FAVORITE.TOPICS.DATA.topics.indexOf(topicToremove), 1);
		ITER.FAVORITE.TOPICS.DATA.save();
	}
};

/**
 * Guarda los datos indicados en local para el ususario autenticado.
 * Si no se indican datos, guarda los de las variables de trabajo.
 * @param data
 */
ITER.FAVORITE.TOPICS.DATA.save = function(data)
{	
	// Recupera la informaci�n local
	var localData = localStorage.getItem("IterPendingArticlesData");
	
	// Si no hay nada en local, lo inicializa
	localData = localData === null ? {} : localData = JSON.parse(localData);
	
	// Establece los datos para el usuario autenticado
	localData[jQryIter.u] = typeof data !== 'undefined' ? data : {"date": ITER.FAVORITE.TOPICS.DATA.lastUpdate, "topics": ITER.FAVORITE.TOPICS.DATA.topics};

	// Guarda los datos
	localStorage.setItem("IterPendingArticlesData", JSON.stringify(localData));
};

/**
 * Recarga los datos con los que vienen del servidor cuando estos son nuevos.
 * @param serverData
 */
ITER.FAVORITE.TOPICS.DATA.merge = function(serverData)
{
	// Comprueba que los datos del servidor sean nuevos
	if (ITER.FAVORITE.TOPICS.DATA.lastUpdate < serverData.date)
	{
		// Guarda los cambios en local
		ITER.FAVORITE.TOPICS.DATA.save(serverData);
		
		// Recarla los datos en memoria
		ITER.FAVORITE.TOPICS.DATA.load();
	}
};

/**
 * Recupera los ids de todos los art�culos pendientes visitados o sin visitar.
 * @param visited true para buscar art�culos pendientes ya visitados, false para los no visitados.
 * @returns {Array} Lista de articlesId de los art�culos pendientes visitados o sin visitar.
 */
ITER.FAVORITE.TOPICS.DATA.getArticles = function(visited)
{
	var articles = [];
	
	for (t in ITER.FAVORITE.TOPICS.DATA.topics)
	{
		var topic = ITER.FAVORITE.TOPICS.DATA.topics[t];
		if (topic.id > 0)
		{
			for (a in topic.articles)
			{
				var articleVisited = topic.articles[a].visited !== true ? false : true;
				if (articleVisited === visited)
				{
					if (!articles.includes(topic.articles[a].id))
						articles.push(topic.articles[a].id);
				}
			}
		}
	}
	
	return articles;
};