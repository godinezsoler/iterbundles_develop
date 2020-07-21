// Declara el espacio de nombres ITER
var ITER = ITER || {};

// Declara el subespacio de nombres ITER.RECOMMENDATIONS
ITER.RECOMMENDATIONS = ITER.RECOMMENDATIONS || {};

// Llamadas HTTP
ITER.RECOMMENDATIONS.HTTP = ITER.RECOMMENDATIONS.HTTP || (function() {
	
	makeAjaxCall = function(methodType, url) {
		var promiseObj = new Promise(function(resolve, reject) {
			var xhr = new XMLHttpRequest();
			xhr.open(methodType, url, true);
			xhr.send();
		
			xhr.onreadystatechange = function(){
				if (xhr.readyState === 4){
					if (xhr.status === 200){
						resolve(xhr.responseText);
					} else {
						reject(new Error(xhr.status));
					}
				}
			};
		});
		return promiseObj;
	};
	
	makeFetchCall = function(methodType, url) {
		return new Promise(function(resolve, reject) {
			fetch(url, { method: methodType, redirect: "manual" })
			.then((response) => {
				if (response.type === "opaqueredirect") {
					reject("retry");
				} else {
					resolve(response.text());
				}
			})
			['catch']((error) => {
				reject(error.status)
			});
		});
	};
	
	processHtmlLinks = function(html, configName) {
		const regex = /^.*-\D{2}.[^-]+$/;
		const doc = new DOMParser().parseFromString(html, "text/html");
		const links = doc.getElementsByTagName("a");
		for (let index = 0; index < links.length; index++) {
			let url = links[index].getAttribute("href");
			if (regex.test(url)) {
				const qsIndex = url.indexOf("?");
				const hashIndex = url.indexOf("#");
				
				const hash = hashIndex !== -1 ? url.substring(hashIndex) : "";
				let qs = qsIndex === -1 ? "" : hashIndex === -1 ? url.substring(qsIndex + 1) : url.substring(qsIndex, hashIndex);
				url = qsIndex !== -1 ? url.substring(0, qsIndex) : hashIndex !== -1 ? url.substring(0, hashIndex) : url;
				
				// Obtiene la configuración de estadísticas de MAS
				const masConfig = ITER.RECOMMENDATIONS.CORE.showConfig().masConfig;
				// Crea el json con los datos
				const paramValue = {};
				paramValue.g = masConfig.idgoal;
				paramValue.c = masConfig.eventcategory;
				paramValue.a = masConfig.eventaction;
				paramValue.n = configName;
				// Crea el parámetro "MAS Recommendation Stats" (o mrs) para el QueryString
				const params = "mrs=" + btoa(JSON.stringify(paramValue));
				qs = qs === "" ? "?" + params : qs + params;
				
				url += qs + hash;
				links[index].setAttribute("href", url);
			}
		}
		
		return doc.body.innerHTML;
	};
	
	return {
		request: async(method, path) => {
			let retries = 0;
			path = path.replace(/\/$/, "").replace(/^\//, "");
			let url = "/news-portlet/recommended-articles/" + path;
			const params = path.split("/").length;
			let errorMsg = "";
			while (retries < params) {
				try {
					const response = await makeFetchCall(method, url);
					return response;
				} catch (error) {
					// Si el error fue un 302, reintenta con menos parámetros
					if (error === "retry") {
						errorMsg = "Unable to retrieve recommendations due to server load.";
						console.log(errorMsg);
						console.log("Trying with less options...");
						url = url.substr(0, url.lastIndexOf("/"));
						++retries;
					} else {
						errorMsg = "Unable to retrieve recommendations. Server response: " + error.message;
						break;
					}
				}
			}
			throw new Error(errorMsg);
		},
		render: async(articleid, templateid, configName) => {
			var url = "/news-portlet/renderArticle/" + articleid + "/" + btoa(templateid);
			try {
				var html = await makeAjaxCall("GET", url);
				return processHtmlLinks(html.trim(), configName);
			} catch (error) {
				throw error;
			}
		},
		print: function(articleid, templateid, configName, container) {
			this.render(articleid, templateid, configName)
			.then(function(html) {
				var template = document.createElement('template');
				template.innerHTML = html;
				container.appendChild(template.content.firstChild);
			});
		}
	};
}());

// Núcleo de la aplicación
ITER.RECOMMENDATIONS.CORE = ITER.RECOMMENDATIONS.CORE || (function() {
	
	// Configuración del sistema de recomendaciones
    var config = {
		properties: {
			dbName: "iter_recommendations_db",
			dbVisitorIdStore: "iter_visitor_id_object_Store",
			dbArticlesStore: "iter_visited_articles_object_Store"
		},
        siteId: "",
        visitorId: "",
        masConfig: null
    };
	
	// Datos del sistema de recomendaciones
	var data = {
		visitedArticles: [],
		portlets: []
	};
	
	// Base de datos de artículos vistos
	var db;
	
	// Comprueba que exista el tracker de MAS y recupera los Ids del sitio y del visitante.
	getMasData = function() {
		return new Promise((resolve, reject) => {
			if (typeof Piwik !== "undefined" && typeof Piwik.getTracker === "function") {
				if (typeof Piwik.getTracker().getSiteId === "function") {
					config.siteId = Piwik.getTracker().getSiteId();
				}
				if (typeof Piwik.getTracker().getVisitorId === "function") {
					config.visitorId = Piwik.getTracker().getVisitorId();
				}
			}

			if (config.siteId !== "" && config.visitorId !== "") {
				resolve();
			} else {
				reject("MAS is not configured");
			}
		});
	};
	
	// Busca todos los portlets de recomendaciones de la página.
	findRecommendationsPortlets = function() {
		data.portlets = Array.from(document.querySelectorAll("div.article-recommendations-portlet div[data-config-id][data-template-id]"));
		return data.portlets.length > 0;
	};
	
	updateVisitorData = function() {
		return new Promise((resolve, reject) => {
			getVisitorId()
			.then((result) => {
				// No hay visitorId registrado. Añade el id del visitante actual.
				if(!!result == false) {
					setVisitorId(config.visitorId)
					.then(() => { resolve(); })
					['catch']((error) => reject(error));
				// El visitante ha cambiado. Actualiza el identificador y limpia el listado de artículos vistos.
				} else if (result.value.visitorId !== config.visitorId) {
					Promise.all([setVisitorId(config.visitorId), resetVisitedArticles()])
					.then(() => resolve())
					['catch']((error) => reject(error));
				} else {
					resolve();
				}
			})
			['catch']((error) => reject(error));
		});
	};
	
	createIndexedDB = function() {
		return new Promise((resolve, reject) => {
			// Comprueba que el navegador soporte indexedDB.
			if (!window.indexedDB) {
				reject("This browser doesn\'t support IndexedDB");
				return;
			}
			
			// Si ya está creada la base de datos, resuelve la promesa.
			if (db) {
				resolve();
				return;
			}
			
			// Abre la base de datos.
			var request = indexedDB.open(config.properties.dbName, 1);
			
			// Si no existe, la crea.
			request.onupgradeneeded = function(event) {
				// Crea un almacén para el identificador del visitante
				event.target.result.createObjectStore(config.properties.dbVisitorIdStore, { keyPath: "visitorId" });
				
				// Crea un almacén para los artículos leídos.
				var objectStore = event.target.result.createObjectStore(config.properties.dbArticlesStore, { keyPath: "articleId" });
				
				// Se crea un índice para buscar por fecha de visita. Se podrían tener duplicados por lo que no se puede usar un índice único.
				objectStore.createIndex("visitDate", "visitDate", { unique: false });
			};
			
			request.onsuccess = function(event) {
				db = event.target.result;
				resolve();
			};
			
			request.onerror = function(event) {
				reject("Database error: " + event.target.error);
			};
		});
	};
	
	
	// Recupera el visitorId de la BD
	getVisitorId = function() {
		return new Promise((resolve, reject) => {
			var request = db.transaction([config.properties.dbVisitorIdStore], "readwrite")
			.objectStore(config.properties.dbVisitorIdStore)
			.openCursor();
			request.onsuccess = function(event) { resolve(event.target.result); };
			request.onerror = function(event) { reject("Error retrieving visitorId from database: " + event.target.error); };
		});
	};
	
	// Vacía la tabla del id de visitante e inserta el indicado.
	setVisitorId = function(visitorId) {
		return new Promise((resolve, reject) => {
			var clearRequest = db.transaction([config.properties.dbVisitorIdStore], "readwrite")
			.objectStore(config.properties.dbVisitorIdStore)
			.clear();
			clearRequest.onsuccess = function(event) {
				var insertRequest = event.target.source.add({ visitorId: visitorId });
				insertRequest.onsuccess = function(event) {
					resolve();
				}
				insertRequest.onerror = function(event) {
					reject("Error inserting visitorId in database: " + event.target.error);
				}
			}
			clearRequest.onerror = function(event) {
				reject("Error deleting visitorId in database: " + event.target.error);
			}
		});
	};
	
	checkVisitedArticle = function() {
		return new Promise((resolve, reject) => {
			if (jQryIter.contextIsArticlePage() && jQryIter.articleId()) {
				addVisitedArticle(jQryIter.articleId())
				.then(() => resolve())
				['catch']((error) => reject(error));
			} else {
				resolve();
			}
		});
	};
	
	addVisitedArticle = function(articleId) {
		return new Promise((resolve, reject) => {
			if (articleId) {
				var request = db.transaction([config.properties.dbArticlesStore], "readwrite")
				.objectStore(config.properties.dbArticlesStore)
				.add({ articleId: articleId, visitDate: Math.floor(new Date().getTime() / 1000)});
				request.onsuccess = function(event) {
					resolve();
				};
				request.onerror = function(event) {
					if (event.target.error.name === "ConstraintError") {
						resolve();
					} else {
						reject("Error inserting visited article: " + event.target.error);
					}
				}
			} else {
				resolve();
			}
		});
	};
	
	getVisitedArticles = function() {
		return new Promise((resolve, reject) => {
			var articles = [];
			var transaction = db.transaction([config.properties.dbArticlesStore], "readwrite");
			var objStore = transaction.objectStore(config.properties.dbArticlesStore);
			
			transaction.oncomplete = function(event) {
				data.visitedArticles = articles;
				resolve(articles);
			};
			
			var request = objStore.openCursor();
			request.onsuccess = function(event) {
				var cursor = event.target.result;
				if (cursor) {
					articles.push(cursor.value.articleId);
					cursor.continue();
				}
			};
			request.onerror = function(event) {
				reject("Error retrieving visitorId from database: " + event.target.error);
			};
		});
	};
	
	// Resetea la información de artículos vistos
	resetVisitedArticles = function() {
		return new Promise((resolve, reject) => {
			var request = db.transaction([config.properties.dbArticlesStore], "readwrite")
			.objectStore(config.properties.dbArticlesStore)
			.clear();
			request.onsuccess = function(event) { resolve(); };
			request.onerror = function(event) { reject("Error resetting visited articles: " + event.target.error); }
		})
	};
	
	
	// Crea la base de datos para almacenar los artículos vistos y obtiene los datos del MAS
	setup = function(masConfig) {
		return new Promise((resolve, reject) => {
			config.masConfig = masConfig;
			Promise.all([createIndexedDB(), getMasData()])
			.then(() => resolve())
			['catch']((error) => reject(error));
		});
	};
	
	cleanVisitedArticles = function(date) {
		return new Promise((resolve, reject) => {
			if (!date) {
				resolve();
				return;
			}
			
			var transaction = db.transaction([config.properties.dbArticlesStore], "readwrite");
			var objStore = transaction.objectStore(config.properties.dbArticlesStore);
			var dateIndex = objStore.index('visitDate');
			var keyRng = IDBKeyRange.upperBound(date);
			var cursorRequest = dateIndex.openCursor(keyRng);
			
			transaction.oncomplete = function(event) {
				resolve();
			};
			cursorRequest.onsuccess = function(event) {
				var cursor = event.target.result;
				if (cursor) {
					cursor.delete();
					cursor.continue();
				}
			};
			cursorRequest.onerror = function(event) {
				reject(new Error("Error cleaning visited articles: " + event.target.error));
			};
		});
	};
	
	/////////////////
	// API PÚBLICA //
	/////////////////
    return {
		init: async(masConfig) => {
			try {
				// Si estamos en un detalle de artículo y se accedió desde una recomendación, envía estadísticas a MAS
				ITER.RECOMMENDATIONS.MAS.sendStatistics();
				// Inicializa la aplicación
				await setup(masConfig);
				// Actualiza el visitorId y, si ha cambiado, resetea los artículos vistos
				await updateVisitorData();
				// Si estamos en un detalle, añade el artículo a la lista de visitados
				await checkVisitedArticle();
				// Recupera los portlets de recomendaciones de la página y comprueba que haya al menos uno
				if (findRecommendationsPortlets()) {
					// Inicializa los portlets
					data.portlets = data.portlets.map(portlet =>
						portlet = new ITER.RECOMMENDATIONS.PORTLET(portlet, config.visitorId, jQryIter.articleId())
					);
				}
			} catch(error) {
				console.log(error);
			}
        },
        showConfig: function() {
            return config;
        },
		addVisitedArticle: function(articleId) {
			return addVisitedArticle(articleId);
		},
		processRecommendationResponse: async(response) => {
			// Procesa la respuesta
			const recomendedArticles = JSON.parse(response);

			// Mira la fecha de actualización de artículos visitados y elimina de la BD los anteriores a esa fecha
			await cleanVisitedArticles(recomendedArticles.lastUpdate)
			
			// Actualiza la lista de artículos vistos
			await getVisitedArticles();
			
			// Selecciona los artículos a mostrar
			var articles = [];
			var numPadding = 0;
			recomendedArticles.sources.forEach(source => {
				// Elimina los artículos vistos
				source.articles = source.articles.filter(article => {
					return data.visitedArticles.indexOf(article) === -1;
				});
				while(source.amount > 0 && source.articles.length > 0) {
					// Coge el primer artículo
					const articleId = source.articles[0];
					source.articles.splice(0, 1);
					// Si no está ya en la lista de renderización, lo añade
					if (typeof articles.find(a => a === articleId) === 'undefined') {
						articles.push(articleId);
						source.amount--;
					}
				}
				// Anota la cantidad de artículos de relleno necesarios.
				numPadding += source.amount;
			});
			
			// Si faltan artículos, busca artículos de relleno en orden de prioridad de los orígenes
			if (numPadding > 0) {
				recomendedArticles.sources.forEach(source => {
					while(numPadding > 0 && source.articles.length > 0) {
						// Coge el primer artículo
						const articleId = source.articles[0];
						source.articles.splice(0, 1);
						// Si no está ya en la lista de renderización, lo añade
						if (typeof articles.find(a => a === articleId) === 'undefined') {
							articles.push(articleId);
							numPadding--;
						}
					}
				});
			}
			
			// Retorna la lista de artículos
			return [articles, recomendedArticles.configName];
		}
    };
}());

// Declara el subespacio de nombres ITER.RECOMMENDATIONS
ITER.RECOMMENDATIONS.PORTLET = ITER.RECOMMENDATIONS.PORTLET || (function(container, visitorId, articleId) {
	var config = {
		container: container,
		configId: container.getAttribute("data-config-id"),
		templateId: container.getAttribute("data-template-id"),
		visitorId: visitorId,
		articleId: articleId
	};
	
	var getRecommendations = function() {
		// Construye la URL de recomendaciones
		let url = config.configId + "/" + config.visitorId;
		if (config.articleId) {
			url += "/" + config.articleId;
		}
		
		// Pide las recomendaciones
		ITER.RECOMMENDATIONS.HTTP.request("GET", url)
		// Obtiene los artículos
		.then((response) => ITER.RECOMMENDATIONS.CORE.processRecommendationResponse(response))
		// Pide el HTML de los artículos y los pinta en el portlet
		.then(([articles, configName]) => {
			articles.forEach(articleId => {
				ITER.RECOMMENDATIONS.HTTP.print(articleId, config.templateId, configName, config.container);
			});
		})
		['catch']((error) => {
			console.log(error)
		});
	};
	
	var processKOResponse = function(data) {
		console.log(data);
	};
	
	getRecommendations();
});

ITER.RECOMMENDATIONS.API = ITER.RECOMMENDATIONS.API || (function() {
	
	const getRecommendations = function(configName, articleId) {
		return new Promise((resolve, reject) => {
			// Construye la URL de recomendaciones
			let url = configName + "/" + ITER.RECOMMENDATIONS.CORE.showConfig().visitorId;
			if (articleId) {
				url += "/" + articleId;
			}
			
			// Pide las recomendaciones
			ITER.RECOMMENDATIONS.HTTP.request("GET", url)
			.then((response) => {
				ITER.RECOMMENDATIONS.CORE.processRecommendationResponse(response)
				.then(([articles, masEventName]) => {
					resolve([articles, masEventName]);
				})
				['catch']((error) => {
					console.log(error)
				});
			})
			['catch']((error) => reject(error));
		});
	};
	
	return {
		getRecommendations: function(configName, articleId) {
			if (!configName) return;
			return getRecommendations(btoa(configName), articleId);
		},
		addVisitedArticle: function(articleId) {
			return ITER.RECOMMENDATIONS.CORE.addVisitedArticle(articleId);
		},
		renderArticle: function(articleid, templateid, masEventName) {
			return ITER.RECOMMENDATIONS.HTTP.render(articleid, templateid, masEventName);
		}
	};
}());

ITER.RECOMMENDATIONS.MAS = ITER.RECOMMENDATIONS.MAS || (function() {
	return {
		sendStatistics: function() {
			// Comprueba que estemos en un detalle y esté habilitada la integración con MAS
			if (jQryIter.contextIsArticlePage() && typeof MASStatsMgr != 'undefined') {
				// Obtiene el parámetro del query string que contiene los datos necesarios para las estadísticas
				const mrs = jQryIter.getQueryParam("mrs");
				if (mrs) {
					// Decodifica los datos
					const masData = JSON.parse(atob(jQryIter.getQueryParam("mrs")));
					
					// Si está configurado un identificador de objetivo, envía el hit de Goal
					MASStatsMgr.sendGoal(masData.g);
					
					// Envía el Evento
					MASStatsMgr.sendEvent(masData.c, masData.a, masData.n);
				}
			}
		}
	};
}());