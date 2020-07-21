var ITER = ITER || {};
ITER.EVENT = ITER.EVENT || {};



// Pide los post del evento
ITER.EVENT.getEventPosts = function()
{
	if (typeof ITER.EVENT.articleId === 'undefined')
		return;
	
	var url = "/restapi/livevent/event:get/" + ITER.EVENT.articleId;
	jQryIter.ajax({url:url, dataType:'json',
		success: function(data)
		{
			// Actualiza los posts
			ITER.EVENT.updatePosts(data.posts);
			
			// Una vez actualizados los Posts y enviado el mensaje al Hook, se le notifica a Angular que vuelve a estar listo 
			ITER.EVENT.ACTION.notifyReady();
		}
	});

	// Se reconfigura para ejecutarse a los 30s
	ITER.EVENT.pollingTimeout = setTimeout(ITER.EVENT.getEventPosts, ITER.EVENT.refreshDelay);
}

ITER.EVENT.updatePosts = function(posts)
{
	// Variables para informar al evento
	var created = [];
	var updated = [];
	var deleted = [];
	
	// Ordena los posts por fecha de creación (primero los sticky)
	posts.sort((a,b) => (
		a.sticky > b.sticky ? -1 : (
			a.sticky < b.sticky ? 1 : (
				a.creationdate > b.creationdate) ? -1 : ((b.creationdate > a.creationdate) ? 1 : 0)
			)
		)
	);
	
	// Si ya tenemos datos de post, es una actualización
	if (ITER.EVENT.posts)
	{
		// Variable que almacena el último post procesado, para saber dónde añadir lo nuevos.
		// Se inicializa con el contenedor del directo.
		var lastPost = ITER.EVENT.eventContainer;
		
		// Marca todos los posts para borrado
		ITER.EVENT.markAllPostForDelete();
		
		// Posts nuevos y actualizados
		for (i in posts)
		{
			// Obtiene el post nuevo
			var newPost = posts[i];
			
			// Busca el post en el listado
			var post = ITER.EVENT.findPost(newPost.postid);
			
			// Lo busca en la página
			var renderedPost = ITER.EVENT.getRenderedPost(newPost.postid);
			
			// Existe y está pintado
			if (post && renderedPost.length > 0)
			{
				// Elimina la marca de borrado
				delete post.delete;
				
				// Comprueba si ha sido modificado
				if (moment(post.updatedate, "YYYYMMDDHHmmss").diff(moment(newPost.updatedate, "YYYYMMDDHHmmss")) !== 0)
				{
					// Lo actualiza
					ITER.EVENT.drawPost(newPost, lastPost, renderedPost);
					
					// Actualiza el número de post actualizados
					updated.push(newPost);
				}
			}
			// Es un nuevo post
			else
			{
				// El post existe, pero no está pintado (Puede ser por un error en el renderPost). Elimina la marca de borrado.
				if (post)
					delete post.delete;
				
				// Lo pinta en la posición correspondiente
				ITER.EVENT.drawPost(newPost, lastPost);
				
				// Actualiza el número de post creados
				created.push(newPost);
			}
			
			// Actualiza el último post procesado
			lastPost = newPost.postid;
		}
		
		// Posts eliminados
		deleted = ITER.EVENT.cleanPosts();
	}
	// Pinta todos los posts
	else
	{
		// Pinta cada post
		for (i in posts)
		{
			// Crea una marca temporal para mantener el orden
			var tempPost = jQryIter("<div data-postid=\"" + posts[i].postid + "\"></div>").appendTo(ITER.EVENT.eventContainer);
			
			// Pinta el post sobre la marca
			ITER.EVENT.drawPost(posts[i], tempPost, tempPost);
			
			// Actualiza el número de post creados
			created.push(posts[i]);
		}
	}

	// Guarda todos los posts
	ITER.EVENT.posts = posts;
	
	// Lanza el evento informativo
	jQryIter.hooks.itle.onUpdate(created, updated, deleted);
}

// Pinta el post indicado
ITER.EVENT.drawPost = function(post, location, oldPost)
{
	var url = "/renderPost/" + post.postid + "/" + post.updatedate;
	jQryIter.ajax({url:url, dataType:'html',
		success: function(data, textStatus, jqXHR)
		{
			// Limpia el HTML
			data = data.trim();
			
			// Si location es el contenedor del post, lo inserta dentro. Si no, lo inserta después.
			var postElement = location === ITER.EVENT.eventContainer ?
                              jQryIter(data).prependTo(location) :
                              jQryIter(data).insertAfter(location instanceof jQuery ? location : ITER.EVENT.getRenderedPost(location));
			
			if (ITER.EVENT.ACTION.controlsCode)
			{
				postElement.before(ITER.EVENT.ACTION.controlsCode);
			}
			
			// Elimina el contenido antiguo si se indica.
			if (typeof oldPost !== 'undefined')
			{
				// Comprueba si tiene controles. Si es así, los elimina.
				var oldPostPrevelement = oldPost.prev();
				if (oldPostPrevelement.length > 0 && typeof oldPostPrevelement.attr("data-postid") === 'undefined')
					oldPostPrevelement.detach();
				
				// Elimina el post obsoleto
				oldPost.detach();
			}
		},
		error: function(data)
		{
			console.error("Error rendering post " + url)
		},
		complete: function(data)
		{
			console.log("Post rendering complete for " + url)
		}
	});
}

// Busca un post en el listado dado su identificador.
ITER.EVENT.findPost = function(postId)
{
	// Si el listano no se ha inicializado, retorna undefined.
	if (typeof ITER.EVENT.posts === 'undefined')
		return;
	
	// Busca el post en el listado.
	return ITER.EVENT.posts.find(function(post) {
	    return post.postid === postId;
	});
}

// Añade la propiedad "delete" a todos los posts actuales.
ITER.EVENT.markAllPostForDelete = function()
{
	ITER.EVENT.posts.map(function(e) {
		e.delete = true;
		return e;
	});
}

ITER.EVENT.cleanPosts = function()
{
	// Recupera los posts a borrar
	var postToDelete = ITER.EVENT.posts.filter(post => post.delete === true);
	
	// Para cada uno
	for (i in postToDelete)
	{
		// Lo busca en la página
		var renderedPost = jQryIter("[data-postid='" + postToDelete[i].postid + "']").first();
		
		// Comprueba si tiene controles. Si es así, los elimina.
		var postPrevelement = renderedPost.prev();
		if (typeof postPrevelement.attr("data-postid") === 'undefined')
			postPrevelement.detach();
		
		// Lo elimina de la página
		renderedPost.detach();
		
		// Lo elimina del listado
		ITER.EVENT.posts = ITER.EVENT.posts.splice(ITER.EVENT.posts.indexOf(postToDelete[i]), 1);
	}
	
	return postToDelete;
}

// Establece el contenedor de los posts.
ITER.EVENT.setEventContainer = function(container)
{
	ITER.EVENT.eventContainer = container;
}

// Busca un posts renderizado en la página dado su identificador.
ITER.EVENT.getRenderedPost = function(postId)
{
	return jQryIter("[data-postid='" + postId + "']").first();
}

//Inicializa el array de posts con los posts cosidos
ITER.EVENT.initializeSewnPosts = function()
{
	if (jQryIter("[data-postid]").length > 0)
	{
		ITER.EVENT.posts = jQryIter("[data-postid]").map(function()
		{
			return { postid: jQryIter(this).data("postid"), creationdate: "0", updatedate: "0" };
		}).toArray();
	}
}

ITER.EVENT.initLiveEvent = function(articleId, eventContainer, refreshDelay)
{
	// Establece el identificador del directo
	ITER.EVENT.articleId = articleId;

	// Establece el contenedor
	ITER.EVENT.setEventContainer(eventContainer);
	
	// Estblece el tiempo de refresco
	ITER.EVENT.refreshDelay = typeof refreshDelay !== 'undefined' ? refreshDelay : 30000;
	
	// Comprueba si hay post cosidos
	ITER.EVENT.initializeSewnPosts();
	
	// Si ha sido abierta en un iFrame
	if (window.parent !== window.self)
	{
		// Comienza a escuchar post-messages
		window.addEventListener('message', ITER.EVENT.connectorListener, false);
	
		// Notifica al padre que está listo para recibir mensajes
		ITER.EVENT.ACTION.notifyReady();
	}
	
	// Inicializa el proceso de petición de posts cada 30 segundos
	ITER.EVENT.getEventPosts();
}

/////////////////////////////
// Comunicación con la App //
/////////////////////////////

// Acciones
ITER.EVENT.ACTION = ITER.EVENT.ACTION || {};

// Almacena el código de los controles personalizados
ITER.EVENT.ACTION.controlsCode = "";

ITER.EVENT.ACTION.setControls = function(style)
{
	// Inserta los estilos
	if (typeof style !== 'undefined')
	{
		jQryIter('head').append(style);
	}
	
	// Inserta el código de los controles
	if (ITER.EVENT.ACTION.controlsCode)
	{
		jQryIter("[data-postid]").each(function(i, e) {
			// Inserta los controles antes del post
			jQryIter(e).before(ITER.EVENT.ACTION.controlsCode);
		});
	}
}

ITER.EVENT.ACTION.notifyReady = function(data)
{
	var msg = {
		module: "iter-live-events",
	    action: "ready"
	};
		
	if (typeof data !== "undefined")
	{
		msg["data"] = data;
	}
		
	parent.postMessage(msg, "*");
}

ITER.EVENT.ACTION.deletePost = function(element)
{
	ITER.EVENT.ACTION.sendMessage(element, "delete-post");
}

ITER.EVENT.ACTION.reviewPost = function(element)
{
	ITER.EVENT.ACTION.sendMessage(element, "review-post");
}

ITER.EVENT.ACTION.editPost = function(element)
{
	ITER.EVENT.ACTION.sendMessage(element, "edit-post");
}

ITER.EVENT.ACTION.sendMessage = function(element, action)
{
	var msg = {
		module: "iter-live-events",
	    action: action
	};
	
	if (element)
	{
		msg["data"] = {
			postid: jQryIter(element).parentsUntil(ITER.EVENT.eventContainer).next().attr("data-postid")
		};
	}
	
	parent.postMessage(msg, "*");
}

// Manejador de post-messages
ITER.EVENT.connectorListener = function(event)
{
	try
	{
		if (event.data != null && event.data !== 'undefined')
		{
			if (event.data.module === "iter-live-events")
			{
				// Mensaje para añadir controles a los posts
				if (event.data.action === "set-controls" && event.data.data && event.data.data.code)
				{
					// Si ya se han insertado controles, no hace nada
					if (ITER.EVENT.ACTION.controlsCode)
						return;
					
					// Guarda el código personalizado
					ITER.EVENT.ACTION.controlsCode = event.data.data.code;
					
					// Inserta los controles
					ITER.EVENT.ACTION.setControls(event.data.data.style);
				}
				// Mensaje para solicitar un refresco de los posts
				else if (event.data.action === "refresh" && ITER.EVENT.pollingTimeout)
				{
					// Para el proceso de petición automático
					clearTimeout(ITER.EVENT.pollingTimeout);
					
					// Lanza el evento inicial de actualizar posts
					ITER.EVENT.getEventPosts();
				}
			}
		}
	}
	catch(err)
	{
	    // Do nothing
		console.log("Unexpected error handling a post message");
	}
};