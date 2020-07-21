// Declara el espacio de nombres ITER
var ITER = ITER || {};

// Declara el subespacio de nombres ITER.WPN
ITER.WPN = ITER.WPN || {};

// Declare ITER.WPN.conf
ITER.WPN.conf = ITER.WPN.conf || {};
ITER.WPN.conf.host = "";
ITER.WPN.conf.appid = "";
ITER.WPN.conf.senderid = "";
ITER.WPN.conf.workerpath = "";


/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//                          FUNCI�N DE INICIALIZACI�N                          //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

// Inicializa el proceso de petici�n de permisos.
// Inicializa las librer�as de firebase, registra el service worker y lanza el proceso de petici�n de permisos.
ITER.WPN.initialize = function(host, appid, senderid, workerpath)
{
	// Almacena los datos de configuraci�n
	ITER.WPN.conf.host = host;
	ITER.WPN.conf.appid = appid;
	ITER.WPN.conf.senderid = senderid;
	ITER.WPN.conf.workerpath = window.location.origin + workerpath;
	
	// Ejecuta el callback onInitialize
	jQryIter.hooks.wpn.onInitialize.apply(this, [host, appid, senderid, ITER.WPN.conf.workerpath]);
	
	// Si el navegador no soporta Service Workers o Cookies, no contin�a.
	if (!ITER.WPN.isCompatible())
	{
		console.log('The browser does not support Service Workers / Cookies');
		return;
	}
	
	// Carga el estado de la suscripci�n
	ITER.WPN.subscription.load();
	
	// Initializa Firebase
	firebase.initializeApp({
		messagingSenderId: ITER.WPN.conf.senderid
	});
	
	// Recupera el objeto Firebase Messaging
	var messaging = firebase.messaging();
    
    // Callback para cuando se actualiza un token
    messaging.onTokenRefresh(function()
    {
    	messaging.getToken()
    	.then(function(refreshedToken)
    	{
    		// Indica que el token aun no se ha enviado al servidor
    		ITER.WPN.subscription.unsuscribe();
    		// Manda el token de instancia al servidor de aplicaciones
    		ITER.WPN.sendFCMTokenToServer(refreshedToken);
    	})
    	['catch'](function(err)
    	{
    		console.log('Unable to retrieve refreshed token ', err);
    	});
    });
    
    // Callback para manejar los mensajes en foreground
    messaging.onMessage(function(payload)
    {
    	// T�tulo
    	var title = payload.data.title;
    	
    	// Opciones por defecto
    	var options = {
    		body: payload.data.body,
    		icon: payload.data.icon,
    		data: {
    			click_action: payload.data.click_action
    		}
    	};
    	
    	if (typeof payload.data.tag !== 'undefined')
    		options.tag = payload.data.tag;
    	
    	// Imagen grande
    	if (payload.data.image)
    		options.image = payload.data.image;
    	
    	new Notification(title, options).onclick = function(event)
    	{
    		event.preventDefault();
    		event.target.close();
    		window.open(event.target.data.click_action, '_blank');
    	};
    });
	
	// Registra el service worker
    navigator.serviceWorker
    .register(ITER.WPN.conf.workerpath)
    .then(function(registration) {
    	// Actualiza el service worker en el objeto de firebase
		messaging.useServiceWorker(registration);
		// Lanza la solicitud de permisos para el env�o de notificaciones
		ITER.WPN.requestPermissionDialog();
	})
    ['catch'](function(err) {
        console.log('Service worker registration failed:', err);
    });
};


/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//     FUNCIONES PARA PEDIR PERMISOS DE ENV�O DE NOTIFICACIONS AL CLIENTE      //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

// Comprueba si debe lanzarse el proceso de petici�n de permisos, y si hay definido un di�logo
// personalizado o se usa directamente el del navegador.
ITER.WPN.requestPermissionDialog = function()
{
	// Si las notificaciones est�n bloqueadas...
	if (Notification.permission === "denied")
	{
		// Si el usuario est� suscrito, se anula su subscripci�n
		if (ITER.WPN.subscription.isSubscribed())
		{
			ITER.WPN.subscription.unsubscribe();
		}
		
		// Termina
		return;
	}

	// Comprueba que la cookie ITR_WPN_DELAYED_REQUEST_PERMISSION no est� vigente 
	if (typeof jQryIter.cookie("ITR_WPN_DELAYED_REQUEST_PERMISSION") === "undefined")
	{
		// Si el usuario est� suscrito, realiza la validaci�n del token si es necesario
		if (ITER.WPN.isUserSubscribed())
		{
			ITER.WPN.subscription.validate();
		}
		// Si no est� suscrito y las notificaciones no est�n deshabilitadas...
		else if (ITER.WPN.areNotificationsEnabled())
		{
			// Comprueba si debe mostrarse el di�logo personalizado
			if (ITER.WPN.dialog.canShowCustomDialog())
			{
				var options = jQryIter.webPushNotificationPermissionDialogOptions();
				// Valida que las opciones m�nimas sean correctas
				if (ITER.WPN.dialog.validate(options))
					ITER.WPN.dialog.show(options);
				else
					console.log('Invalid notification permission request dialog options');
			}
			// Si no es as�, lanza la petici�n de notificaciones por defecto.
			else
			{
				ITER.WPN.requestPermission();
			}
		}
	}
};

// Lanza el proceso de petici�n de permisos del navegador
ITER.WPN.requestPermission = function()
{
	return new Promise(function(resolve, reject)
	{
		// Recupera el objeto de Firebase Messaging
		const messaging = firebase.messaging();
		
		// Solicita los permisos para recibir notificaciones
		messaging.requestPermission()
		.then(function()
		{
			console.log('Notification permission granted.');
		  
			// Si se conceden los permisos, obtiene un token de instancia
			messaging.getToken()
			.then(function(currentToken)
			{
				// Si se obtiene un token, lo registra en el servidor de aplicaciones
			    if (currentToken)
			    {
			    	ITER.WPN.sendFCMTokenToServer(currentToken)
			    	.then(function()
			    	{
			    		resolve();
			    	})
					['catch'](function(err)
					{
						reject(err);
					});
			    }
			    // Si no, muestra un error
			    else
			    {
			    	ITER.WPN.subscription.unsuscribe();
			    	console.log('No Instance ID token available. Request permission to generate one.');
				    reject('No Instance ID token available. Request permission to generate one.');
			    }
			})
			['catch'](function(err)
			{
				ITER.WPN.subscription.unsuscribe();
			    console.log('An error occurred while retrieving token. ', err);
			    reject('An error occurred while retrieving token.');
			});
		})
		['catch'](function(err)
		{
		  console.log('Unable to get permission to notify.', err);
		  reject('Unable to get permission to notify.');
		});
	});
};

/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//     FUNCIONES PARA EL DIALOGO PERSONALIZADO DE PETICI�N DE PERMISOS         //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

ITER.WPN.dialog = {
	//Comprueba si se ha definido un di�logo personalizado para la petici�n de permisos. Si es as�, se mostrar�
	//cuando el estado del permiso sea "preguntar" o cuando est�n suspendidas las notificaciones pero la cookie haya expirado
	canShowCustomDialog: function()
	{
		var existCustomDialog = typeof jQryIter.webPushNotificationPermissionDialogOptions != 'undefined';
		var notificationsGranted = Notification.permission !== "granted";
		var expiredNotificationsSuspension = ITER.WPN.subscription.status === "suspended" && typeof jQryIter.cookie("ITR_WPN_DELAYED_REQUEST_PERMISSION") === "undefined";
		
		return existCustomDialog && (notificationsGranted || expiredNotificationsSuspension);
	},
	
	// Valida que las opciones personalizadas para el di�logo tenga definido al menos
	// el cuerpo y el texto de los botones.
	validate: function(options)
	{
		return !!options.body             && !!options.body.trim()             // Mensaje del di�logo
			&& !!options.buttons["agree"] && !!options.buttons["agree"].trim() // Etiqueta del bot�n "Aceptar"
			&& !!options.buttons["later"] && !!options.buttons["later"].trim() // Etiqueta del bot�n "M�s tarde"
			&& !!options.buttons["deny"]  && !!options.buttons["deny"].trim(); // Etiqueta del bot�n "Cancelar"
	},
	
	// Crea un di�logo de JQueryUI y lo muestra
	show: function(options)
	{
		var wpnDlg = jQryIter("<div>");
		var wpnDlgOptions = options;
		
		wpnDlgOptions.closeOnEscape = false;
		
		wpnDlgOptions.open = function(event, ui)
		{
			jQryIter(this).html(options.body);
			jQryIter(".ui-dialog-titlebar-close", ui.dialog | ui).hide();
		};
		
		wpnDlgOptions.close = function(event, ui)
		{
		};
		
		wpnDlgOptions.buttons = [
		    {
				text: options.buttons["agree"],
				click: function() {
					// Cierra el di�logo
	  				jQryIter(this).dialog("close");
	  				// Lanza la petici�n de permisos
	  				ITER.WPN.requestPermission();
	  			}
		    },
		    {
		    	text: options.buttons["later"],
				click: function() {
					// Cierra el di�logo
	  				jQryIter(this).dialog("close");
	  				// Suspende durante 1 d�a la suscripci�n
	  				ITER.WPN.subscription.suspend(1);
	  			}
		    },
			{
				text: options.buttons["deny"],
				click: function() {
					// Cierra el di�logo
	  				jQryIter(this).dialog("close");
	  				// Suspende durante 30 d�as la suscripci�n
	  				ITER.WPN.subscription.suspend(30);
	  			}
			}
	    ];
		
		wpnDlg.dialog(wpnDlgOptions);
	}
};

/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//       FUNCIONES PARA EL ENVIO DEL TOKEN AL SERVIDOR DE APLICACIONES         //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

// Env�a el token al servidor de aplicaciones.
// Si todo va bien, activa el flag que indica que ya se ha enviado el token y,
// si el token no estaba ya registrado, manda un hit con el objetivo 3 al MAS.
ITER.WPN.sendFCMTokenToServer = function(currentToken)
{
	// Instancia una promesa de env�o del token al servidor de aplicaciones.
	return new Promise(function(resolve, reject)
	{
		ITER.WPN.server.promiseSendFCMTokenToServer(currentToken)
		.then( function(response)
		{
			// Guarda en el 'local storage' el tokenId generado.
			ITER.WPN.subscription.subscribe(response.tokenid, currentToken);
			
			// Si el token era nuevo, env�a el 'goal' de suscripci�n a notificaciones a MAS si est� activa la integraci�n.
			if (response.status === 201)
				ITER.WPN.notifySubscriptionToNotifications();
			
			// Resuelve la promesa
			resolve();
		})
		['catch']( function()
		{
			// Desactiva en el 'local storage' el flag que indica que ya se envi� el token al servidor.
			ITER.WPN.subscription.unsubscribe();
			reject('Error sending token to server');
		});
	});
};

/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//                             FUNCIONES AUXILIARES                            //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

// Comprueba si el navegador soporta las notificaciones
ITER.WPN.isCompatible = function()
{
	return "Notification" in window && "serviceWorker" in navigator && navigator.cookieEnabled;
};

// Las notificaciones se consideran suspendidas si el token tiene el valor "suspended"
// y la cookie ITR_WPN_DELAYED_REQUEST_PERMISSION est� vigente
ITER.WPN.areNotificationsEnabled = function()
{
	return ITER.WPN.subscription.status !== "suspended" || typeof jQryIter.cookie("ITR_WPN_DELAYED_REQUEST_PERMISSION") === "undefined";
};

ITER.WPN.isUserSubscribed = function()
{
	return Notification.permission === "granted" && ITER.WPN.subscription.isSubscribed();
};

// Comprueba si se ha inicializado el controlador de env�o de estad�ticas.
// a MAS y, si es as�, env�a el objetivo de suscripci�n a notificaciones.
ITER.WPN.notifySubscriptionToNotifications = function()
{
	if (typeof MASStatsMgr != 'undefined')
	{
		MASStatsMgr.notifySubscriptionToNotifications();
	}
};

// Recupera el Id del visitante en MAS
ITER.WPN.getVisitorId = function()
{
	if (typeof Piwik !== 'undefined' && typeof Piwik.getTracker === 'function' && typeof Piwik.getTracker().getVisitorId === 'function')
		return Piwik.getTracker().getVisitorId();
	else
		return "";
};

/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//                         CONTROL DE SUSCRIPCION                              //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

ITER.WPN.manageSubscription = function(callback)
{
	if (ITER.WPN.isUserSubscribed())
		ITER.WPN.unsubscribeToNotifications(callback);
	else
		ITER.WPN.subscribeToNotifications(callback);
};


ITER.WPN.subscribeToNotifications = function(callback)
{
	if (Notification.permission === "denied")
	{
		// Si est�n bloqueadas, comprueba si est� definida la funci�n personalizada y la lanza.
		if (typeof jQryIter.webPushNotificationBlokedActions != 'undefined')
			jQryIter.webPushNotificationBlokedActions();

		// Ejecuta la funci�n callback
		if (typeof callback !== 'undefined')
			callback();
	}
	else
	{
		// Reanuda las notificaciones
		ITER.WPN.subscription.unsuspend();
		
		// Lanza el proceso de suscripci�n sin mostrar el di�logo personalizado.
		ITER.WPN.requestPermission()
		.then( function()
		{
			// Ejecuta la funci�n callback
			if (typeof callback !== 'undefined')
				callback();
		})
		['catch']( function()
		{
			console.log("Unable to subscibe to notification");
		});
	}
};

ITER.WPN.unsubscribeToNotifications = function(callback)
{
	// Elimina el token del servidor de aplicaciones.
	var currentToken = ITER.WPN.subscription.token.firebaseToken;
	ITER.WPN.server.promiseDeleteFCMTokenFromServer(currentToken)
	.then( function(response)
	{
		// Indica en la cookie que se suspendieron las notificaciones.
		ITER.WPN.subscription.suspend(30);
		
		// Ejecuta la funci�n callback
		if (typeof callback !== 'undefined')
			callback();
	})
	['catch']( function()
	{
		console.log("Unable to delete token from applications server");
	});
};


/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//                  COMUNICACI�N CON EL SERVIDOR DE APLICACIONES               //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////
ITER.WPN.server = {
	// Crea una promesa que hace uso de la API REST del servidor de aplicaciones para enviar el token.
	promiseSendFCMTokenToServer: function(token)
	{
		return new Promise(function(resolve, reject)
		{
			// Construye la URL del endpoint
			var url = ITER.WPN.conf.host + "/WebPushNotification/instance/" + encodeURI(token) + "/app/" + ITER.WPN.conf.appid;
			var visitorId = ITER.WPN.getVisitorId();
			if (typeof visitorId !== 'undefined' && visitorId != null && visitorId !== "")
				url += "/" + visitorId;
			
			// Inicializa la conexi�n
		    var xhr = new XMLHttpRequest();
		    xhr.open('POST', url);
		    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		    xhr.onload = function()
		    {
		    	// Si la API retorna 201 CREATED (El token no exist�a en el servidor) o 409 CONFLICT (El token ya estaba dado de alta), resuelve la promesa.
		        if (xhr.status === 201 || xhr.status === 409)
		        {
		        	console.log('Token sent to server.');
		        	// Recupera el tokenId
		        	var response = JSON.parse(xhr.responseText);
		        	// Crea el resultado
		        	var result = {
		        		"status": xhr.status,
		        		"tokenid": xhr.status === 201 ? response.token_id : response.error.detail
		        	};
		        	// Resuelve la promesa
		        	resolve(result);
		        }
		        // Si no, registra un error
		        else
		        {
		        	console.log('An error occurred while sending token. Server response was' + xhr.status);
		        	reject('An error occurred while sending token. Server response was' + xhr.status);
		        }
		    };
		    // Manda el token a la API
		    xhr.send();
		});
	},
	
	// Crea una promesa que hace uso de la API REST del servidor de aplicaciones para eliminar el token.
	promiseDeleteFCMTokenFromServer: function(token)
	{
		return new Promise(function(resolve, reject)
		{
			// Inicializa la conexi�n
		    var xhr = new XMLHttpRequest();
		    xhr.open('DELETE', ITER.WPN.conf.host + "/WebPushNotification/instance/" + encodeURI(token));
		    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		    xhr.onload = function()
		    {
		    	// Si la API retorna 204 NO CONTENT o un 404 NOT FOUND, resuelve la promesa.
		        if (xhr.status === 204 || xhr.status === 404)
		        {
		        	console.log('Token removed from the server.');
		        	// Resuelve la promesa
		        	resolve();
		        }
		        // Si no, registra un error
		        else
		        {
		        	console.log('An error occurred while deleting the token. Server response was' + xhr.status);
		        	reject('An error occurred while deleting the token. Server response was' + xhr.status);
		        }
		    };
		    // Manda la petici�n de borrado del token a la API
		    xhr.send();
		});
	}
};

/////////////////////////////////////////////////////////////////////////////////
//                                                                             //
//                  GESTION DE LA SUSCRIPCI�N EN EL CLIENTE                    //
//                                                                             //
/////////////////////////////////////////////////////////////////////////////////

ITER.WPN.subscription = {
	status: "",
	token: {
		tokenId: "",
		firebaseToken: ""
	},
	
	store: function()
	{
		var value = this.status + ";" + this.token.tokenId + ";" + this.token.firebaseToken;
		jQryIter.cookie("ITR_WPN_SUBSCRIPTION", value, { expires: 18250, path:"/", domain: jQryIter.getDomain() });
	},
	
	load: function()
	{
		var cookieValue = jQryIter.cookie("ITR_WPN_SUBSCRIPTION");
		if (typeof cookieValue !== 'undefined')
		{
			cookieValue = cookieValue.split(";");
			if (cookieValue.length === 3)
			{
				this.status = cookieValue[0];
				this.token.tokenId = cookieValue[1];
				this.token.firebaseToken = cookieValue[2];
			}
		}
		else
		{
			this.status = "unsubscribed";
		}
	},
	
	isSubscribed: function()
	{
		return this.status === "subscribed";
	},
	
	isUnsubscribed: function()
	{
		return this.status === "unsubscribed";
	},
	
	isSuspended: function()
	{
		return this.status === "suspended";
	},
	
	validate: function()
	{
		if (typeof jQryIter.cookie("ITR_WPN_SUBSCRIPTION_VALIDATION") === 'undefined')
		{
			const messaging = firebase.messaging();
			
			messaging.getToken()
			.then(function(newFirebaseToken)
			{
				if (newFirebaseToken)
				{
					if (ITER.WPN.subscription.token.firebaseToken !== newFirebaseToken)
					{
						ITER.WPN.server.promiseSendFCMTokenToServer(newFirebaseToken)
						.then( function(response)
						{
							// Actualiza el token
							ITER.WPN.subscription.subscribe(response.tokenid, newFirebaseToken);
						})
						['catch']( function()
						{
							console.log("Unable to register new token.");
						});
					}
					
					// Pone la cookie para esperar otros 120 min
					var date = new Date();
					var minutes = 120;
					date.setTime(date.getTime() + (minutes * 60 * 1000));
					jQryIter.cookie("ITR_WPN_SUBSCRIPTION_VALIDATION", "true", { expires: date, path:"/", domain: jQryIter.getDomain() });
				}
			})
			['catch'](function(err)
			{
				console.log("Unable to retrieve notification token.");
			});
		}
	},
	
	subscribe: function(tokenId, firebaseToken)
	{
		this.status = "subscribed";
		this.token.tokenId = tokenId;
		this.token.firebaseToken = firebaseToken;
		this.store();
	},
	
	unsubscribe: function()
	{
		this.status = "unsubscribed";
		this.token.tokenId = "";
		this.token.firebaseToken = "";
		this.store();
	},
	
	// Si el usuario est� "suscrito", "suspende" la subscripci�n y elimina la informaci�n del token
	suspend: function(days)
	{
		// Pone una cookie para no mostrarle el mensaje al usuario durante 1 mes.
		jQryIter.cookie("ITR_WPN_DELAYED_REQUEST_PERMISSION", "true", { expires: days, path:"/", domain: jQryIter.getDomain() });
		
		this.status = "suspended";
		this.token.tokenId = "";
		this.token.firebaseToken = "";
		this.store();
	},
	
	// Si el usuario tiene la subscripci�n "suspendida", la cambia a "no suscrito"
	unsuspend: function()
	{
		if (this.status === "suspended")
		{
			this.status = "unsubscribed";
			this.store();
		}
	}
};