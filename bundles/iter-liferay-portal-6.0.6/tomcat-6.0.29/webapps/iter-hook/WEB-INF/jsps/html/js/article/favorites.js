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

//Comprueba que esté habilitada la funcionalidad de favoritos y que haya usuario autenticado
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
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
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

// Añade un artículo a los favoritos
ITER.FAVORITE.ARTICLES.add = function(articleid, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.ARTICLES.isEnabled())
		return;
	
	// Llama a la API para añadir el artículo
	var url = "/restapi/user/favorite/articles/add/" + articleid;
	ITER.FAVORITE.CORE.apirequest("POST", url,
		// Si se añadió correctamente
		function()
		{
			// Incluye su id temporalmente en el listado de artículos favoritos
			ITER.FAVORITE.ARTICLES.list.push({"crc":"", "id": articleid});
			
			// Comprueba si se ha inicializado el controlador de envío de estadíticas a MAS y,
			// si es así, envía el objetivo de "Artículo añadido a favoritos".
			if (typeof MASStatsMgr !== 'undefined')
			{
				MASStatsMgr.notifyFavoriteArticleAdded();
			}
			
			// Ejecuta la función callback si se definió
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

// Elimina un artículo de los favoritos del usuario
ITER.FAVORITE.ARTICLES.remove = function(articleid, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.ARTICLES.isEnabled())
		return;
	
	// Llama a la API para eliminar el artículo
	var url = "/restapi/user/favorite/articles/remove/" + articleid;
	ITER.FAVORITE.CORE.apirequest("POST", url,
		// Si se eliminó correctamente
		function()
		{
			// Busca el artículo
			var article = ITER.FAVORITE.CORE.findArticleById(ITER.FAVORITE.ARTICLES.list, articleid);
			// Lo elimina del listado de favoritos
			ITER.FAVORITE.ARTICLES.list.splice(ITER.FAVORITE.ARTICLES.list.indexOf(article), 1);
			// Ejecuta la función callback si se definió
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
				// Compone el id del artículo con su firma
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
 * Permite añadir funciones que se ejecutan tras la carga (correcta o fallida) de los datos de temas favoritos.
 * @param successCallback Función a ejecutar tras la carga correcta de temas favoritos.
 * @param errorCallback Función a ejecutar si no se pueden cargar los datos de temas favoritos.
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
 * Carga los datos de temas favoritos y artículos pendientes del usuario autenticado desde el servidor.
 * @param successCallback Función a ejecutar tras la carga correcta de temas favoritos.
 * @param errorCallback Función a ejecutar si no se pueden cargar los datos de temas favoritos.
 */
ITER.FAVORITE.TOPICS.load = function(successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
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
			// Indica que finalizó la carga de datos
			ITER.FAVORITE.TOPICS.loaded = true;
			// Ejecuta la función de callback si se indicó alguna
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/**
 * Retorna la lista de temas favoritos del usuario.
 * @returns {Array} con la lista de temas favoritos del usuario y sus artículos pendientes.
 */
ITER.FAVORITE.TOPICS.get = function()
{
	var topics = [];
	for (var t in ITER.FAVORITE.TOPICS.DATA.topics)
	{
		// Añade todos los temas que no sean el "leer más tarde"
		if (ITER.FAVORITE.TOPICS.DATA.topics[t].id > 0)
			topics.push(ITER.FAVORITE.TOPICS.DATA.topics[t]);
	}
	return topics;
};

/**
 * Comprueba si el tema es uno de los favoritos del usuario autenticado.
 * @param categoryid El id del tema.
 * @returns {Boolean} true si está incluido en los favoritos del usuario, false en caso contrario.
 */
ITER.FAVORITE.TOPICS.isFavorite = function(id)
{
	if (typeof ITER.FAVORITE.TOPICS.DATA.findById(id) === 'undefined')
		return false;
	
	return true;
};

/**
 * Añade un tema a los favoritos del usuario autenticado. Si está configurada la integración con MAS,
 * envía un hit al objetivo "Suscripción a temas favoritos".
 * @param id El identificador del tema.
 * @param successCallback Función a ejecutar tras añadir correctamente el tema a favoritos.
 * @param errorCallback Función a ejecutar si no se pudo añadir el tema a favoritos.
 */
ITER.FAVORITE.TOPICS.add = function(id, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/topics/add/" + id,
		function()
		{
			// Recupera el nombre del metadato
			var metaInfo = jQryIter("*[data-categoryid='" + id + "'][data-categoryname]");
			var name = metaInfo.size() > 0 ? metaInfo.attr("data-categoryname") : "";
			
			// Añade el tema en la configuración local
			ITER.FAVORITE.TOPICS.DATA.addTopic(id, name);
			
			// Comprueba si se ha inicializado el controlador de envío de estadíticas a MAS y,
			// si es así, envía el objetivo de "Suscripción a temas".
			if (typeof MASStatsMgr !== 'undefined')
			{
				MASStatsMgr.notifyFavoriteTopicAdded();
			}
			
			// Ejecuta la función de callback si se indicó alguna
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/**
 * Elimina un tema de los favoritos del usuario autenticado.
 * @param id El identificador del tema.
 * @param successCallback Función a ejecutar tras eliminar correctamente el tema de favoritos.
 * @param errorCallback Función a ejecutar si no se pudo eliminar el tema de favoritos.
 */
ITER.FAVORITE.TOPICS.remove = function(id, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/topics/remove/" + id,
		function()
		{
			// Elimina el tema de la configuración local
			ITER.FAVORITE.TOPICS.DATA.removeTopic(id);
			
			// Ejecuta la función de callback si se indicó alguna
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

///////////////////////////////////////
//                                   //
//   ARTÍCULOS PENDIENTES DE LEER    //
//                                   //
///////////////////////////////////////

ITER.FAVORITE.TOPICS.onVisitPendingArticle = null;

/**
 * Retorna la lista de artículos pendientes del tema indicado.
 * @param topic El tema del que se quieren obtener sólo sus artículos pendientes.
 * @returns {Array} Lista de artículos pendientes del tema.
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
 * Comprueba si un artículo sugerido está pendiente de leer por parte del usuario autenticado.
 * @param articleid el identificador del artículo.
 * @returns {Boolean} true si está pendiente de leer, false en caso contrario.
 */
ITER.FAVORITE.TOPICS.isPending = function(articleid)
{
	return ITER.FAVORITE.TOPICS.DATA.getArticles(false).indexOf(articleid) >= 0;
};

/**
 * Retorna el número de artículos pendientes de leer del usuario autenticado.
 * @returns El número de artículos pendientes de leer.
 */
ITER.FAVORITE.TOPICS.countPendingArticles = function()
{
	return ITER.FAVORITE.TOPICS.DATA.getArticles(false).length;
};

/**
 * Elimina un artículo del listado de leer más tarde.
 * @param articleid el identificador del artículo.
 * @param successCallback Función a ejecutar tras eliminar correctamente el artículo del listado de leer más tarde.
 * @param errorCallback Función a ejecutar si no se pueden eliminar el artículo del listado de leer más tarde.
 */
ITER.FAVORITE.TOPICS.removePendingArticle = function(articleid, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/articles/removepending/" + articleid, successCallback, errorCallback);
};

/**
 * Marca un artículo pendiente como "visitado".
 * @param articleid El id del artículo visitado.
 */
ITER.FAVORITE.TOPICS.markAsRead = function(articleid)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	// Elimina el artículo del servidor
	ITER.FAVORITE.TOPICS.removePendingArticle(articleid,
		// Si se elimina correctamente, lo marca como leído en local
		function()
		{
			var changes = false;
			// Busca las ocurrencias del artículo
			for (t in ITER.FAVORITE.TOPICS.DATA.topics)
			{
				for (a in ITER.FAVORITE.TOPICS.DATA.topics[t].articles)
				{
					var article = ITER.FAVORITE.TOPICS.DATA.topics[t].articles[a];
					// Si es el artículo indicado, lo marca como leído
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
				
				// Ejecuta la función callback si se definió en el hook
				if (typeof ITER.FAVORITE.TOPICS.onVisitPendingArticle === 'function')
					ITER.FAVORITE.TOPICS.onVisitPendingArticle();
			}
		}
	);
};

///////////////////////////////////////////////////
//                                               //
//         ARTÍCULOS PARA LEER MÁS TARDE         //
//                                               //
///////////////////////////////////////////////////

/**
 * Retorna el listado de los artículos  que el usuario añadió a "leer más tarde".
 * @returns {Array} La lista de artículos pendientes de leer más tarde.
 */
ITER.FAVORITE.TOPICS.getReadLaterArticles = function()
{
	var articles = [];
	// Busca el tema instrumental "leer más tarde"
	var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);
	if (typeof readLaterTopic !== 'undefined')
	{
		// Añade sólo los artículos pendientes
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
* Comprueba si un artículo añadido a "leer más tarde" está pediente de leer por parte del usuario autenticado.
* @param articleid El id del artículo.
* @returns {Boolean} true si está pendiente de leer, false en caso contrario.
*/
ITER.FAVORITE.TOPICS.isReadLater = function(articleid)
{
	// Busca la categoría con id = 0
	var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);

	// Si no existe, retorna falso
	if (typeof readLaterTopic === 'undefined')
		return false;

	// Busca el artículo en el listado de la categoría "readLater"
	var article = ITER.FAVORITE.CORE.findArticleById(readLaterTopic.articles, articleid);

	// Si no existe, retorna falso
	if (typeof article === 'undefined')
		return false;

	// Se considera pendiente si aún no se ha visitado
	return article.visited !== true;
};

/**
 * Retorna el número de artículos pendientes de leer más tarde del usuario autenticado.
 * @returns El número de artículos pendientes de leer más tarde.
 */
ITER.FAVORITE.TOPICS.countReadLaterArticles = function()
{
	return ITER.FAVORITE.TOPICS.getReadLaterArticles().length;
};

/**
* Añade un artículo para leer más tarde.
* @param articleid el identificador del artículo.
* @param successCallback Función a ejecutar tras añadir correctamente el artículo al listado de leer más tarde.
* @param errorCallback Función a ejecutar si no se pueden añadir el artículo al listado de leer más tarde.
*/
ITER.FAVORITE.TOPICS.readLater = function(articleid, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	ITER.FAVORITE.CORE.apirequest("POST", "/restapi/user/favorite/articles/readlater/" + articleid,
		// Si se añadió correctamente
		function()
		{
			// Recarga la configuración
			ITER.FAVORITE.TOPICS.DATA.load();
			
			// Obtiene el tema "leer más tarde". Si no existe, lo crea.
			var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);
			if (typeof readLaterTopic === "undefined")
				readLaterTopic = ITER.FAVORITE.TOPICS.DATA.addTopic(0, "readlater");

			// Busca el artículo
			var article = ITER.FAVORITE.CORE.findArticleById(readLaterTopic.articles, articleid);

			// Si ya existía, le quita el flag "visited"
			if (typeof article !== 'undefined')
				article.visited = false;
			// Si no, añade el artículo
			else
				readLaterTopic.articles.push({"crc":"", "id": articleid});

			// Guarda los cambios
			ITER.FAVORITE.TOPICS.DATA.save();

			// Ejecuta la función callback si se definió
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/**
* Elimina un artículo del listado de "leer más tarde".
* @param articleid el identificador del artículo.
* @param successCallback Función a ejecutar tras eliminar correctamente el artículo del listado de leer más tarde.
* @param errorCallback Función a ejecutar si no se pudo eliminar el artículo del listado de leer más tarde.
*/
ITER.FAVORITE.TOPICS.removeReadLater = function(articleid, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!ITER.FAVORITE.TOPICS.isEnabled())
		return;
	
	// Elimina el artículo del servidor
	ITER.FAVORITE.TOPICS.removePendingArticle(articleid,
		// Si se elimina correctamente, lo borra también de los datos locales
		function()
		{
			// Recarga la configuración
			ITER.FAVORITE.TOPICS.DATA.load();
			
			// Obtiene el tema "leer más tarde"
			var readLaterTopic = ITER.FAVORITE.TOPICS.DATA.findById(0);
			if (typeof readLaterTopic !== "undefined")
			{
				// Busca el artículo
				var article = ITER.FAVORITE.CORE.findArticleById(readLaterTopic.articles, articleid);
				if (typeof readLaterTopic !== "undefined")
				{
					// Elimina el artículo
					readLaterTopic.articles.splice(readLaterTopic.articles.indexOf(article), 1);
				}

				// Guarda los cambios
				ITER.FAVORITE.TOPICS.DATA.save();
			}

			// Ejecuta la función callback si se definió
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

/*********************************
 *         GESTIÓN LOCAL         *
 *********************************/
ITER.FAVORITE.TOPICS.DATA = ITER.FAVORITE.TOPICS.DATA || {};

/**
 * Unix time de la última actualizazción de datos desde el servidor.
 */
ITER.FAVORITE.TOPICS.DATA.lastUpdate = 0;
/**
 * Temas favoritos del usuario en memoria.
 */
ITER.FAVORITE.TOPICS.DATA.topics = [];

/**
 * Carga la configuración almacenada en local en la variable de trabajo.
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
 * Añade un tema si no estaba ya incluído y guarda los cambios en local.
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
	// Recupera la información local
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
 * Recupera los ids de todos los artículos pendientes visitados o sin visitar.
 * @param visited true para buscar artículos pendientes ya visitados, false para los no visitados.
 * @returns {Array} Lista de articlesId de los artículos pendientes visitados o sin visitar.
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