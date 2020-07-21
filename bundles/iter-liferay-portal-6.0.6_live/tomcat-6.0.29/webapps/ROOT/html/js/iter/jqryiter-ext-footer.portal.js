// Recupera el valor de un parámetro del query string.
// Si hay más de un valor con la misma clave, retorna un array con los valores en orden.
jQryIter.getQueryParam = function (key)
{
	var regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)', 'g');
    var results = new Array();
    var match;
    do
    {
    	match = regex.exec(window.location.search);
    	if (match)
    	{
    		results.push(!match || !match[2] ? "" : decodeURIComponent(match[2].replace(/\+/g, ' ')));
    	}
    }
    while (match);
    
    return results.length === 0 ? "" : results.length === 1 ? results[0] : results;
};

// Establece el valor de un parámetro del query string.
// Si ya existe la clave, sustituye el valor, si no, lo añade.
// Si hay más de un parámetro con la misma clave, sustituye todos.
// Puede indicarse un array de valores, en cuyo caso sustituirá en orden los parámetros con la misma clave y,
// si le sobran, insertará el resto al final.
// Nótese que modificar el query string implica una recarga de la página.
jQryIter.setQueryParam = function (key, value)
{
	var regex = new RegExp("([?&]" + key + "=)[^?&]", "g");
	
	var found = false;
	var replacesCount = 0;
    
	// Recupera el query string
    var qs = window.location.search || "?";
    
    // Realiza las sustituciones oportunas
	qs = qs.replace(regex,
		function(match, inmutable)
		{
			found = true;
			return inmutable + (value instanceof Array ? value[replacesCount++] : value);
		}
	);
	
	// Si se encontró el parámetro indicado...
	if(found)
	{
		// ... pero quedan valores que añadir, los concatena al final
		if (value instanceof Array && value.length > replacesCount)
		{
			for (var v = replacesCount; v < value.length; v++)
			{
				if (qs !== '?') qs += "&";
				qs += key + "=" + value[v];
			}
		}
	}
	// Si no se encontró un parámetro con la clave indicada, lo añade al final
	else
	{
		if (value instanceof Array && value.length > 1)
		{
			for (var v = 0; v < value.length; v++)
			{
				if (qs !== '?') qs += "&";
				qs += key + "=" + value[v];
			}
		}
		else
		{
			var v = value instanceof Array ? value[0] : value;
			if (qs !== '?') qs += "&";
			qs +=  key + "=" + v;
		}
	}
	
	// Recompone la URL
	window.location.href = [location.protocol, '//', location.host, location.pathname, qs, window.location.hash].join('');
};
