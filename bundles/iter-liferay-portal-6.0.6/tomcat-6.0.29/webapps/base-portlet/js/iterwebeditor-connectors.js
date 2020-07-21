// Declara el espacio de nombres ITER
var ITER = ITER || {};

// Declara el subespacio de nombres ITER.IWE
ITER.IWE = ITER.IWE || {};

//Declara el subespacio de nombres ITER.IWE.STATUS
ITER.IWE.STATUS = ITER.IWE.STATUS || {};

ITER.IWE.STATUS.editMode = false;

// Manejador de post-messages
ITER.IWE.connectorListener = function(event)
{
	try
	{
		if (event.data != null && event.data !== 'undefined')
		{
			var module = event.data.module;
			if (module != null && module !== 'undefined' && module === "iter-web-editor")
			{
				switch(event.data.action)
				{
					case "enable-article-edit-mode":
						ITER.IWE.enableArticleEditMode();
						break;
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

// Activa el modo edici�n para IterWebEditor
ITER.IWE.enableArticleEditMode = function()
{
	// Activa el flag de edici�n
	ITER.IWE.STATUS.editMode = true;
	// Sustituye los enlaces de los art�culos por una notificaci�n del art�culo seleccionado
	ITER.IWE.processArticles();
};

// Vac�a los enlaces de los art�culos para evitar la navegaci�n y a�ade un evento para llamar
// al m�todo ITER.IWE.notifySelectedArticle() pas�ndole el propio art�culo como par�metro
ITER.IWE.processArticles = function()
{
	jQryIter("*[iteridart] a").each(
		function()
		{
			jQryIter(this).attr("href","javascript:void(0)");
			jQryIter(this).attr("onclick", "ITER.IWE.notifySelectedArticle(this); return false;");
		}
	);
};

// Notifica al padre el art�culo seleccionado
ITER.IWE.notifySelectedArticle = function(article)
{
	var element = jQryIter(article).closest("*[iteridart]");
	if (element != null && element !== 'undefined')
	{
		var iteridart = element.attr("iteridart");
		if (iteridart != null && iteridart !== 'undefined')
		{
			var msg = {
				module: "iter-web-editor",
			    action: "selected-article",
			    data: {
			        iteridart: iteridart.substr(2)
			    }
			};
			
			parent.postMessage(msg, "*");
		}
	}
};

// Comienza a escuchar post-messages
window.addEventListener('message', ITER.IWE.connectorListener, false);

// Se engancha al hook widgetCompleteLoad
jQryIter(document).on("widgetCompleteLoad", function(a)
{
	if (ITER.IWE.STATUS.editMode)
	{
		// Sustituye los enlaces de los art�culos por una notificaci�n del art�culo seleccionado
		ITER.IWE.processArticles();
	}
});