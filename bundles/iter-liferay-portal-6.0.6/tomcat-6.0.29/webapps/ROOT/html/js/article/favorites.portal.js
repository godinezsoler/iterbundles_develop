var ITER = ITER || {};
ITER.FAVORITE = ITER.FAVORITE || {};
ITER.FAVORITE.ARTICLES = ITER.FAVORITE.ARTICLES || {};

ITER.FAVORITE.ARTICLES.list = null;

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
	if (!(jQryIter.isFavoriteArticlesEnabled() && ITER.FAVORITE.ARTICLES.isUserAuthenticated()))
		return;
	
	var url = "/restapi/user/favorite/articles/get/" + jQryIter.u;
	ITER.FAVORITE.ARTICLES.apirequest("GET", url,
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

ITER.FAVORITE.ARTICLES.findById = function(articleid)
{
	return ITER.FAVORITE.ARTICLES.list.find(
		function(article)
		{
			return article.id == this;
		},
		articleid
	);
};

ITER.FAVORITE.ARTICLES.isFavorite = function(articleid)
{
	return typeof ITER.FAVORITE.ARTICLES.findById(articleid) !== 'undefined';
};

// Añade un artículo a los favoritos
ITER.FAVORITE.ARTICLES.add = function(articleid, successCallback, errorCallback)
{
	// Si no está habilitada la funcionalidad de favoritos o no hay usuario autenticado, no hace nada
	if (!(jQryIter.isFavoriteArticlesEnabled() && ITER.FAVORITE.ARTICLES.isUserAuthenticated()))
		return;
	
	// Llama a la API para añadir el artículo
	var url = "/restapi/user/favorite/articles/add/" + articleid;
	ITER.FAVORITE.ARTICLES.apirequest("POST", url,
		// Si se añadió correctamente
		function()
		{
			// Incluye su id temporalmente en el listado de artículos favoritos
			ITER.FAVORITE.ARTICLES.list.push({"sing":"", "id": articleid});
			
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
	if (!(jQryIter.isFavoriteArticlesEnabled() && ITER.FAVORITE.ARTICLES.isUserAuthenticated()))
		return;
	
	// Llama a la API para eliminar el artículo
	var url = "/restapi/user/favorite/articles/remove/" + articleid;
	ITER.FAVORITE.ARTICLES.apirequest("POST", url,
		// Si se eliminó correctamente
		function()
		{
			// Lo elimina del listado de favoritos
			ITER.FAVORITE.ARTICLES.list.pop(ITER.FAVORITE.ARTICLES.list.indexOf(ITER.FAVORITE.ARTICLES.findById(articleid)));
			// Ejecuta la función callback si se definió
			if (typeof successCallback === 'function')
				successCallback();
		},
		errorCallback
	);
};

ITER.FAVORITE.ARTICLES.isUserAuthenticated = function()
{
	return jQryIter.u ? true : false;
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
					var signedArticleId = ITER.FAVORITE.ARTICLES.list[i].sing + ITER.FAVORITE.ARTICLES.list[i].id;
					ITER.FAVORITE.ARTICLES.render(signedArticleId, templateid, favoriteContainer);
				}
			},
			function() { console.log("Unexpected error rendering favorite articles"); }
	);
};

ITER.FAVORITE.ARTICLES.render = function(articleid, templateid, container)
{
	var url = "/news-portlet/renderArticle/" + articleid + "/" + btoa(templateid);
	jQryIter.ajax({url:url, dataType:'html',
		success: function(data, textStatus, jqXHR)
		{
			container.append(data);
		}
	});
};

ITER.FAVORITE.ARTICLES.apirequest = function(method, url, successCallback, errorCallback)
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
