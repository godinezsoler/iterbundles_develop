// Declare namespace ITER
var ITER = ITER || {};

// Declare subnamespace ITER.WPN
ITER.WPN = ITER.WPN || {};

// Declare ITER.WPN.conf
ITER.WPN.conf = ITER.WPN.conf || {};
ITER.WPN.conf.host = "";
ITER.WPN.conf.appid = "";
ITER.WPN.conf.senderid = "";
ITER.WPN.conf.workerpath = "";

// Initialization function
ITER.WPN.initialize = function(host, appid, senderid, workerpath)
{
	ITER.WPN.conf.host = host;
	ITER.WPN.conf.appid = appid;
	ITER.WPN.conf.senderid = senderid;
	ITER.WPN.conf.workerpath = workerpath;
	
	// Initialize Firebase
	firebase.initializeApp({
		messagingSenderId: ITER.WPN.conf.senderid
	});
	
	// Retrieve Firebase Messaging object.
	var messaging = firebase.messaging();
	
	// Sets the path of the firebase service worker path
    navigator.serviceWorker
    .register(ITER.WPN.conf.workerpath)
    .then(function(registration) { 
		return messaging.useServiceWorker(registration);
	})
    .catch(function(err) {
        console.log('Service worker registration failed:', err);
    });
    
    // Callback fired if Instance ID token is updated.
    messaging.onTokenRefresh(function()
    {
    	messaging.getToken()
    	.then(function(refreshedToken)
    	{
    		// Indicate that the new Instance ID token has not yet been sent to the app server.
    		ITER.WPN.setFCMTokenSentToServer(false);
    		// Send Instance ID token to app server.
    		ITER.WPN.sendFCMTokenToServer(refreshedToken);
    	})
    	.catch(function(err)
    	{
    		console.log('Unable to retrieve refreshed token ', err);
    	});
    });
    
    // Foreground messages
    messaging.onMessage(function(payload)
    {
    	var notificationOptions = {
    		body: payload.notification.body,
    		icon: payload.notification.icon,
    		click_action: payload.notification.click_action
    	};
    	new Notification(payload.notification.title, notificationOptions);
    });
};


ITER.WPN.requestPermission = function()
{
	if (!ITER.WPN.isFcmTokenToServer() || Notification.permission !== "granted")
	{
		// If permission is denied and a token was previously sent, set the 'sent' flag to false and returns.
		if (ITER.WPN.isFcmTokenToServer() && Notification.permission === "denied")
		{
			ITER.WPN.setFCMTokenSentToServer(false);
			return;
		}

		// Retrieve Firebase Messaging object.
		const messaging = firebase.messaging();
		
		// Request permissions to receive notifications.
		messaging.requestPermission()
		.then(function()
		{
			console.log('Notification permission granted.');
		  
			// Get the token of the instance.
			messaging.getToken()
			.then(function(currentToken)
			{
			    if (currentToken)
			    {
			    	ITER.WPN.sendFCMTokenToServer(currentToken);
			    }
			    else
			    {
			      console.log('No Instance ID token available. Request permission to generate one.');
			      ITER.WPN.setFCMTokenSentToServer(false);
			    }
			})
			.catch(function(err)
			{
			    console.log('An error occurred while retrieving token. ', err);
			    ITER.WPN.setFCMTokenSentToServer(false);
			});
		})
		.catch(function(err)
		{
		  console.log('Unable to get permission to notify.', err);
		});
	}
};

ITER.WPN.sendFCMTokenToServer = function(currentToken)
{
	ITER.WPN.promiseSendFCMTokenToServer(currentToken)
	.then( function()
	{
		ITER.WPN.setFCMTokenSentToServer(true);
	})
	.catch( function()
	{
		ITER.WPN.setFCMTokenSentToServer(false);
	});
};

ITER.WPN.promiseSendFCMTokenToServer = function(currentToken)
{
	return new Promise(function(resolve, reject)
	{
	    var xhr = new XMLHttpRequest();
	    xhr.open('POST', ITER.WPN.conf.host + "/WebPushNotification/instance/" + encodeURI(currentToken) + "/app/" + ITER.WPN.conf.appid);
	    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
	    xhr.onload = function()
	    {
	        if (xhr.status === 201)
	        {
	        	console.log('Token sent to server.');
	        	resolve(xhr.response);
	        }
	        else
	        {
	        	console.log('An error occurred while sending token. Server response was' + xhr.status);
	        	reject('An error occurred while sending token. Server response was' + xhr.status);
	        }
	    };
	    xhr.send();
	});
};

ITER.WPN.isFcmTokenToServer = function()
{
	return window.localStorage.getItem('fcmTokenSentToServer') == 1;
};

ITER.WPN.setFCMTokenSentToServer = function(sent)
{
	window.localStorage.setItem('fcmTokenSentToServer', sent ? 1 : 0);
};