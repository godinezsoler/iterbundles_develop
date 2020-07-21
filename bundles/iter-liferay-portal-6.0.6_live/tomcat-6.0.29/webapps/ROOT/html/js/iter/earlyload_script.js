	function Rendition(){};
	Rendition.RENDITION_CLASSIC 		= "classic";
	Rendition.RENDITION_MOBILE  		= "mobile";
	Rendition.RENDITION_VALUES			= [Rendition.RENDITION_CLASSIC, Rendition.RENDITION_MOBILE];
	Rendition.ITR_MOBILEVIEW_DISABLED	= "ITR_MOBILEVIEW_DISABLED";

	Rendition.readCookie = function(name) 
	{
		var nameEQ 		= name + "=";
		var ca 			= document.cookie.split(';');
		var cookieValue = null;
		
		for (var i=0; i < ca.length;i++) 
		{
			var c = ca[i];
			while (c.charAt(0)==' ') 
				c = c.substring(1,c.length);
			
			if (c.indexOf(nameEQ) == 0) 
			{
				cookieValue = c.substring(nameEQ.length, c.length);
				break;
			}
		}
		return cookieValue;
	};
	
	Rendition.getPreferredMobileRendition = function ()
	{
		 var cookieValue 	    = (Rendition.readCookie(Rendition.ITR_MOBILEVIEW_DISABLED)+"").toLowerCase()=="true";
		 var preferredRendition = (!cookieValue) ? Rendition.RENDITION_MOBILE : Rendition.RENDITION_CLASSIC;
		 
	   return preferredRendition;
	};
	
	// Se mantiene el queryString y el hash (ITER-732 Se pierden los datos de campa�a cuando act�a el script de redirecci�n m�vil)
	var urlRest 			= window.location.search+window.location.hash;
	var mobileLocation 		= "${mobileurl}";
	var classicLocation		= "${classicurl}";
	var isMobileEnvironment = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|OperaMini/i.test(navigator.userAgent);
	var preferredRendition	= Rendition.getPreferredMobileRendition();
	
	if (isMobileEnvironment)
	{
		if (preferredRendition == Rendition.RENDITION_MOBILE && window.location.pathname != mobileLocation)
		{
			// En entorno m�vil, prefiere ver la versi�n m�vil y NO es la URL actual
			window.location.href = mobileLocation+urlRest;
		}
		else if (preferredRendition == Rendition.RENDITION_CLASSIC && window.location.pathname != classicLocation)
		{
			// En entorno m�vil, prefiere ver la versi�n cl�sica y NO es la URL actual
			window.location.href = classicLocation+urlRest;
		}
	}
	else if (window.location.pathname != classicLocation)
	{
		// En un entorno cl�sico SIEMPRE tiene que ir la URL cl�sica
		window.location.href = classicLocation+urlRest;
	}

