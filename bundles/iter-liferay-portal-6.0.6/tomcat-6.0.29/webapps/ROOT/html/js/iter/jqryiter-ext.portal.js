 (function($)
 {
	$.RENDITION_CLASSIC 		= "classic";
	$.RENDITION_MOBILE  		= "mobile";
	$.RENDITION_VALUES			= [jQryIter.RENDITION_CLASSIC, jQryIter.RENDITION_MOBILE];
	$.ITR_MOBILEVIEW_DISABLED	= "ITR_MOBILEVIEW_DISABLED";
	 
    $.getDomain = function ()
	{
		var i=0,domain=document.domain, p=domain.split('.'),s='_gd'+(new Date()).getTime();
		   
		while ( i < (p.length-1) && document.cookie.indexOf(s+'='+s) == -1 )
		{
			domain = p.slice(-1-(++i)).join('.');
			document.cookie = s+"="+s+";domain="+domain+";";
		}
		   
		document.cookie = s+"=;expires=Thu, 01 Jan 1970 00:00:01 GMT;domain="+domain+";";
		jQryIter.removeCookie(s);
		   
		return domain;
     };

     $.getPreferredMobileRendition = function ()
     {
    	 // Tarea #0009808: Versión móvil: Gestionar la preferencia del usuario de acceder a la versión clásica o a la móvil
    	 // Retornará (basándose en el valor de la cookie ITR_MOBILEVIEW_DISABLED) la versión del sitio preferida por el usuario:
    	 // "classic" cuando ITR_MOBILEVIEW_DISABLED = true
    	 //	"mobile"  cuando ITR_MOBILEVIEW_DISABLED = false, o no exista

    	 var cookieValue 	    = (jQryIter.cookie(jQryIter.ITR_MOBILEVIEW_DISABLED)+"").toLowerCase()=="true";
    	 var preferredRendition = (!cookieValue) ? jQryIter.RENDITION_MOBILE : jQryIter.RENDITION_CLASSIC;
    	 
        return preferredRendition;
     };

     $.switchSiteTo = function (rendition)
     {
    	 if (jQryIter.isEmptyObject(rendition))
		 {
			 console.log("switchSiteTo: rendition is null");
		 }
    	 else if (jQryIter.inArray(rendition.toLowerCase(), jQryIter.RENDITION_VALUES) == -1)
         {
    		 console.log("switchSiteTo: rendition is invalid value ("+rendition+")");
         }
    	 else if (rendition.toLowerCase() == jQryIter.getPreferredMobileRendition())
    	 {
    		 console.log("switchSiteTo: rendition has already setted ("+rendition+")");
    	 }
    	 else
    	 {
    		 // Si se ha introducido un rendition válido y diferente al actual
    		 var cookieValue = (rendition.toLowerCase() == jQryIter.RENDITION_CLASSIC);
    			 
	    	 // Se añade la cookie con la nueva preferencia
	    	 jQryIter.cookie(jQryIter.ITR_MOBILEVIEW_DISABLED, cookieValue, {domain:jQryIter.getDomain()});
	    	 
	    	 if (jQryIter.isFunction(jQryIter.gotoSite))
			 {
	    		 jQryIter.gotoSite(rendition);
			 }
	    	 else
			 {
	    		 // Se recarga la página para que al siguiente pintado se tenga el cuenta el nuevo rendition
	    		 location.reload();
			 }
    	 }
     };
     
     $.onDisqusNewComment = function(comment)
     {
     };

 })( jQryIter );