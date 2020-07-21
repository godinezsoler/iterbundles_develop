// Declara el espacio de nombres ITER
var ITER = ITER || {};

// Declara el subespacio de nombres ITER.SURVEYS
ITER.SURVEYS = ITER.SURVEYS || (function() {
	
	makeAjaxCall = function(methodType, url, payload) {
		return new Promise(function(resolve, reject) {
			var xhr = new XMLHttpRequest();
			xhr.open(methodType, url, true);
			xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
			xhr.send(payload);
		
			xhr.onreadystatechange = function(){
				if (xhr.readyState === 4){
					if (xhr.status === 200){
						resolve(xhr.responseText);
					} else {
						if (typeof xhr.responseText === 'undefined' || !xhr.responseText)
							reject(new Error(xhr.status));
						else {
							jsonError = JSON.parse(xhr.responseText);
							reject(new Error(jsonError.error));
						}
					}
				}
			};
		});
	};
	
	checkCanVote = function(surveyId) {
		// Comprueba que estén habilitadas las cookies
		if(navigator.cookieEnabled)
		{
			// Comprueba el parámetro
			if (typeof surveyId !== 'undefined' && surveyId)
			{
				// Recupera el contenido de la cookie IsVoted
				var cookie = jQuery.cookie("IsVoted");
				
				// Si existe la cookie, comprueba si ya se votó para ese surveyId
				if (typeof cookie !== 'undefined' && cookie)
					return cookie.split(",").indexOf(surveyId) === -1;
				
				// Si no existe la cookie, retorna true
				return true;
			}
		}
		// Si no se informa surveyId, retorna false
		return false;
	};
	
	setVoted = function(surveyId)
	{
		// Comprueba que se informe surveyId
		if (typeof surveyId !== 'undefined' && surveyId)
		{
			// Recupera el contenido de la cookie IsVoted
			var cookie = jQuery.cookie("IsVoted");
			
			// Si existe la cookie y no está registrado ese surveyId, lo añade
			if (typeof cookie !== 'undefined' && cookie)
			{
				if (cookie.split(",").indexOf(surveyId) === -1)
					cookie += "," + surveyId;
			}
			// Si no existe la cookie, la inicializa con el surveyId
			else
				cookie = surveyId;
			
			// Actualiza la cookie
			jQuery.cookie("IsVoted", cookie, { expires : 5 });
		}
	};
	
	normalizeForm = function(form) {
		// Comprueba que se informe el parámetro
		if (typeof form !== 'undefined' && form)
		{
			// Si no es un objeto de jQryIter o jQuery, lo convierte
			if (!(form instanceof jQryIter || form instanceof jQuery))
			{
				form = jQryIter(form);
			}
			
			// Comprueba que sea un elemento "form"
			if (form.is("form")) {
				
				// Transforma el formulario en un objeto JSON
				const jsonForm = {};
				form.serialize().split("&").map(function(param) {
					const paramPair = param.split("=");
					if (paramPair.length == 2) {
						jsonForm[paramPair[0]] = paramPair[1];
					}
				});
				
				// Comprueba que existan los parámetros obligatorios
				if (typeof jsonForm.questionId === 'undefined' || typeof jsonForm.choiceId === 'undefined')
					return null;
				
				// Retorna el payload
				return jsonForm;
			}
		}
		
		// Si no se cumplen las condiciones, retorna null
		return null;
	};
	
	return {
		vote: function(form, masEventAction, masEventName) {
			return new Promise(function(resolve, reject) {
				// Valida el formulario de la encuesta
				if ((form = normalizeForm(form)) === null)
					return reject(new Error("invalidform"));
				
				// Comprueba que estén habilitadas las cookies
				if(!navigator.cookieEnabled)
					return reject(new Error("cantvote"));
				
				// Comprueba si ya ha votado
				if (!checkCanVote(form.questionId))
					return reject(new Error("duplicatevote"));
				
				// Comprueba si usa o no captcha y selecciona el endpoint apropiado
				const url = typeof form["g-recaptcha-response"] !== 'undefined' ? "/restapi/poll/captchavote" : "/restapi/poll/vote";
	
				// Realiza la votación
				makeAjaxCall("POST", url, JSON.stringify(form))
				.then(function(results) {
					// Marca la encuesta como votada
					setVoted(form.questionId);
					// Si hay integración con MAS y se indica Acción y Nombre , manda el evento
					if (typeof MASStatsMgr != 'undefined' && typeof masEventAction != 'undefined' && typeof masEventName != 'undefined') {
						MASStatsMgr.sendEvent("Encuestas", masEventAction, masEventName);
					}
					// Resuelve la promesa
					resolve(JSON.parse(results));
				})
				['catch'](function(error) {
					reject(error);
				});
			});
		},
		getResults: function(surveyId) {
			return new Promise(function(resolve, reject) {
				makeAjaxCall("POST", "/restapi/poll/getpoll/" + surveyId, null)
				.then(function(results) {
					resolve(JSON.parse(results));
				})
				['catch'](function(error) {
					reject(error);
				});
			});
		}
	};
}());