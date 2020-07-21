function LoginForm() {};

LoginForm.sendForm = function( event, ref )
{
	var form    = jQryIter(jQryIter(event.target).parents("form")[0]);
    var usrName = form.find(".userInput").val();
    var pass    = form.find(".inputPassword").val();
    var alive   = form.find(".chkKeep").is(':checked');
	        	  
	function cbDoLogin(form, json) 
	{
    	var exception = json.exception;
    	
    	if(!exception)
 		{
    		// Comprueba si es un login para una compra
    		var afterAuthUrl = jQryIter.cookie("ITR_COOKIE_AFTER_AUTHENTICATION_URL");
    		if(afterAuthUrl)
    		{
    			jQryIter.removeCookie("ITR_COOKIE_AFTER_AUTHENTICATION_URL", { domain: jQryIter.getDomain(), path: "/" });
    			jQryIter(window.location).attr('href', afterAuthUrl);
    		}
    		// Si hay mensaje
    		else if(json.returnValue.infomsg)
			{
    			form.find(".closeWrapper").click();
	
	   			showWarn( form.siblings(".errTitle").val(), "OK", json.returnValue.sso ? json.returnValue.sso : ref, json.returnValue.infomsg );
			}
			else if (json.returnValue.furtheraction && json.returnValue.furtheraction.action == "redirect")
			{
				jQryIter(window.location).attr('href', json.returnValue.furtheraction.location);
			}
			else if (json.returnValue.sso)
			{
				jQryIter(window.location).attr('href', json.returnValue.sso);
			}
	    	else
	    	{
	    		var hayAbridor=true;
	    		try
	    		{
	    			hayAbridor = window.opener != null;
	    		}
	    		catch (e) {}
	    	
	    		if(hayAbridor)
		    		window.close();
		    	else if(ref!="")
	      			window.location.href = ref;
		    	else
		    		window.location.reload(true);
	    	}
 		}
    	else
   		{
    		var idx = exception.indexOf(":");
    		var strExcep = exception.substring(idx+1);
    		showError(form.siblings(".errTitle").val(), "OK", strExcep);
   		}	            
    }

      	 
    var dataService = 
  	{
  		username : usrName,
  		password: pass,
  		keepAlive: alive,
  		origin: ref
  	};
   	 
 	jQryIter.ajax(
 	{
		type: "POST",
		url: "/restapi/user/login",
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) 
		{
			jQryIter.showAlert("error", error);
		},
		success: function(data)
		{
			cbDoLogin(form, data);
		}
	});
};

LoginForm.onKeyDown = function(event)
{
	var keycode = (event.keyCode ? event.keyCode : event.which);
	// keycode = 9 = TAB
	if (keycode == 9)
	{
		var item = jQryIter(event.target);
		
		// Si se ha hecho TAB en el usuario enfoca el botón Login
		if (event.shiftKey && item.attr('class')=="userInput")
		{
			event.preventDefault();
			jQryIter(event.target).parents("form").find(".btnLogin").focus();
		}
		
		// Si se ha hecho TAB en el botón Login enfoca el usuario
		else if (!event.shiftKey && item.attr('class')=="btnLogin")
		{
			event.preventDefault();
			jQryIter(event.target).parents("form").find(".userInput").focus();
		}
	}
};

LoginForm.onKeyPress = function(event)
{
	var keycode = (event.keyCode ? event.keyCode : event.which);
	// keycode = 13 = ENTER
	if (keycode==13)
	{
		// Si ha pulsado el ENTER en "Mantener logueado" es como pinchar en el botón login
		jQryIter(event.target).parents("form").find(".btnLogin").click();
	}
};