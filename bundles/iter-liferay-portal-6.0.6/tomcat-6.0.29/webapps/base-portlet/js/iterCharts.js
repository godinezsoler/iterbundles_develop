/**************************************************************************
 * 
 * CONTROLADOR DE GRÁFICOS DE VISITAS Y LECTURAS
 * 
 * Clase encargada de gestionar todos los gráficos de las páginas de
 * estadísticas de visitas.
 * 
 * Es capaz de generar gráficos lineales y de barras de visitas, y gráficos
 * de donut con la relación entre visitas y lecturas.
 * 
 * Incluye funciones para navegar mostrar las visitas por horas, días o
 * meses y permite navegar por las fechas de los gráficos.
 * 
 * Añade funcionalidad para mostrar las estadísticas de redes sociales.
 * 
 **************************************************************************/
function iterChartManager(customData, customOptions, polling_rate, refresh_rate, debug)
{
	var data = {
		groupId: 0,			// ID del grupo.
		masIdSite: "",      // ID del sitio de MAS
		pageType: "article",// Tipo de página de estadísticas.
		itemId: 0,			// ID del elemento del que se solicitan estadísticas.
		showVisitsTrend: true,
		maxSections: 5,		// Máximo número de secciones para el ranking de más vistas.
		maxArticles: 5,		// Máximo número de artículos para el ranking de más vistos.
		maxReadings: 5, 
	    maxSharings: 5, 
		maxComments: 5, 
		maxFeedback: 5, 
		vocabularies: "",	// IDs de los vocabularios de los que mostrar rankings.
		showVisits: true	// En las estadísticas del sitio, indica si se quiere visualizar información de artículos o sólo de metadatos.
	};
	
	var options = {
			resolution: "minute",	        // Resolución del gráfico de visitas.
			dateLimit: "",			        // Fecha límite de las estadísticas visualizadas.
			tooltipTotalVisits: "Visits",   // Tooltip para la serie Visitas totales
			tooltipVisits: "Article visits",// Tooltip para la serie Visitas de artículos
			tooltipReads: "Reads",	        // Tooltip para la serie Lecturas de artículos
			tooltipSharings: "Sharings",	// Tooltip para la serie Comparticiones de artículos
			tooltipComments: "Comments",	// Tooltip para la serie Comentarios de artículos
			tooltipRate: "Rate",	        // Tooltip para la serie Valoración de artículos
			thousandSeparator: "",			// Separador de miles
			displayedHours: 24, 			// Fecha de la solicitud inicial de estadísticas
			realTime: true,					// Indica si la agrupación por horas es en tiempo real o debe mostrar el día completo
			annotations: "",                // Indica si se quieren mostrar anotaciones
			emptyVisitsMessage: "",
			readOnly: true,
			masUrl: ""
	};

	// Hora del servidor que devolvió las estadísticas.
	var currentServerTime = null;
	// Fecha actual del gráfico de visitas. Se usa como referencia para la navegación.
	var currentChartTime = null;
	// Indica si se quiere volver a crear los gráficos o sólo hay que actualizarlos.
	var refreshCharts = true;
	// Tiempo de consulta para la actualización de los gráficos en tiempo real.
	var pollingTimeout = null;
	
	// Token de acceso para las estadísticas de MAS
	var masToken = null;

	var chartsList = new Array(); // Listado de gráficos de rankings.	
	var readChart               = null;        // Gráfico circular de lecturas.
	var feedbackChart           = null;        // Gráfico del portlet de valoración.
	
	// Inicialización
	jQryIter.extend(true, data, customData);
	jQryIter.extend(true, options, customOptions);
	if (!options.realTime)
		options.displayedHours = 24;

	initializeCharts();
	update();

	///////////////////////////////////////////////////////////////////////
	//                   INICIALIZACION DE LOS GRAFICOS                  //
	///////////////////////////////////////////////////////////////////////
	
	function initializeCharts()
	{
		Chart.defaults.global.defaultFontFamily = "'Roboto', sans-serif";
		
		var chartOptions = {
			title: " ",
			tooltips: [],
			data: {
				pageType: data.pageType,
				itemType: "",
				itemId: "",
				maxItems: 5,
				thousandSeparator: options.thousandSeparator,
				emptyVisitsMessage: options.emptyVisitsMessage
			},
			style: {
				mainContainerClass: "ranking-chart-container",
				titleContainerClass: "ranking-chart-title-container",
				chartContainerClass: "ranking-chart-canvas",
				messageContainerClass: "warning"
			},
			debug: debug
		};
		
		if (data.showVisits)
		{
			if (data.showVisitsTrend)
			{
				// Crea el gráfico de tendencias de visitas
				var trendOptions = {
					tooltips: [options.tooltipVisits, options.tooltipReads, options.tooltipTotalVisits],
					data: {
						pageType: data.pageType,
						itemType: data.pageType,
						itemId: data.itemId,
						thousandSeparator: options.thousandSeparator,
						annotations: options.annotations
					},
					style: {
						mainContainerClass: "visits-chart-container"
					},
					callback: function(statistics)
					{
						var sumTotalVisits = 0;
						var sumTotalArticleVisits = 0;
						var sumTotalArticleReads = 0;
						
						// Actauliza los datos de los bloques de resumen
						if (statistics.data.totals != undefined)
						{
							jQryIter("#total-visits").closest(".resume-block").show();
							sumTotalVisits = statistics.data.totals.reduce(function(a, b) { return a+b; }, 0);
							jQryIter("#total-visits").text( iterChartFormatNumber(sumTotalVisits, options.thousandSeparator) );
						}
	
						if (statistics.data.visits != undefined)
						{
							jQryIter("#article-visits").closest(".resume-block").show();
							sumTotalArticleVisits = statistics.data.visits.reduce(function(a, b) { return a+b; }, 0);
							jQryIter("#article-visits").text( iterChartFormatNumber(sumTotalArticleVisits, options.thousandSeparator) );
						}
	
						if (statistics.data.reads != undefined)
						{
							jQryIter("#article-reads").closest(".resume-block").show();
							sumTotalArticleReads = statistics.data.reads.reduce(function(a, b) { return a+b; }, 0);
							jQryIter("#article-reads").text( iterChartFormatNumber(sumTotalArticleReads, options.thousandSeparator) );
						}
						
						// Si es una página de estadísticas de artículo, actualiza el gráfico de porcentaje de lecturas
						if ("article" === data.pageType)
						{
							var readStatistics = {
								reads: sumTotalArticleReads,
								unreads: sumTotalArticleVisits
							};
							readChart.setStatistics(readStatistics);
						}
					}
				};
				chartsList.push(new ITER.statistics.TrendChart(trendOptions, "visits-content"));
			}
			
			// Crea el gráfico del ranking de secciones
			if ("group" === data.pageType && data.maxSections > 0)
			{
				chartOptions.tooltips = [options.tooltipTotalVisits, options.tooltipVisits];
				chartOptions.data.itemType = "section";
				chartOptions.data.maxItems = data.maxSections;
				chartsList.push(new ITER.statistics.RankingChart(chartOptions, "visits-content"));
			}
			
			if ("group" === data.pageType || "section" === data.pageType || "metadata" === data.pageType)
			{
				if (data.maxArticles > 0)
				{
					// Crea el gráfico del ranking de artículos más vistos
					var articlesOptions = jQryIter.extend(true, {}, chartOptions);
					articlesOptions.tooltips = [options.tooltipVisits, options.tooltipReads];
					articlesOptions.data.itemType = "article";
					articlesOptions.data.criteria = "visits";
					articlesOptions.data.itemId = data.itemId;
					articlesOptions.data.maxItems = data.maxArticles;
					chartsList.push(new ITER.statistics.RankingChart(articlesOptions, "visits-content"));
				}
				
				if (data.maxReadings > 0)
				{
					// Crea el gráfico del ranking de artículos más leídos
					var articlesOptions = jQryIter.extend(true, {}, chartOptions);
					articlesOptions.tooltips = [options.tooltipVisits, options.tooltipReads];
					articlesOptions.data.itemType = "article";
					articlesOptions.data.criteria = "readings";
					articlesOptions.data.itemId = data.itemId;
					articlesOptions.data.maxItems = data.maxReadings;
					chartsList.push(new ITER.statistics.RankingChart(articlesOptions, "visits-content"));
				}
				
				if (data.maxSharings > 0)
				{
				// Crea el gráfico del ranking de artículos compartidos
				var articlesSharingOptions = jQryIter.extend(true, {}, chartOptions);
				articlesSharingOptions.tooltips = [options.tooltipSharings];
				articlesSharingOptions.data.itemType = "article";
				articlesSharingOptions.data.criteria = "sharings";
				articlesSharingOptions.data.itemId = data.itemId;
				articlesSharingOptions.data.maxItems = data.maxSharings;
				chartsList.push(new ITER.statistics.RankingChart(articlesSharingOptions, "visits-content"));
				}
				
				if (data.maxComments > 0)
				{
					// Crea el gráfico del ranking de artículos más comentados
					var articlesCommentsOptions = jQryIter.extend(true, {}, chartOptions);
					articlesCommentsOptions.tooltips = [options.tooltipComments];
					articlesCommentsOptions.data.itemType = "article";
					articlesCommentsOptions.data.criteria = "comments";
					articlesCommentsOptions.data.itemId = data.itemId;
					articlesCommentsOptions.data.maxItems = data.maxComments;
					chartsList.push(new ITER.statistics.RankingChart(articlesCommentsOptions, "visits-content"));
				}
				
				if (data.maxFeedback > 0)
				{
					// Crea el gráfico del ranking de artículos mejor valorados
					var articlesFeedbackOptions = jQryIter.extend(true, {}, chartOptions);
					articlesFeedbackOptions.tooltips = [options.tooltipRate];
					articlesFeedbackOptions.data.itemType = "article";
					articlesFeedbackOptions.data.criteria = "feedback";
					articlesFeedbackOptions.data.itemId = data.itemId;
					articlesFeedbackOptions.data.maxItems = data.maxFeedback;
					chartsList.push(new ITER.statistics.RankingChart(articlesFeedbackOptions, "visits-content"));
				}
			}
		}
		
		// Rankings de metadatos específicos de las estadísticas del sitio
		if ("group" === data.pageType && data.vocabularies)
		{
			var vocabularies = data.vocabularies.split(",");
			for (var i=0; i<vocabularies.length; i++)
			{
				var vocabulary = vocabularies[i];
				var id = vocabulary.split("_")[0];
				var top = vocabulary.split("_")[1];
				
				chartOptions.tooltips = [options.tooltipTotalVisits];
				chartOptions.data.itemType = "metadata";
				chartOptions.data.itemId = id;
				chartOptions.data.maxItems = top;
				chartsList.push(new ITER.statistics.RankingChart(chartOptions, "visits-content"));
			}
		}
		
		// Gráficos específicos de las estadísticas de un artículo
		if ("article" === data.pageType)
		{
			var container = jQryIter("#visits-content");
			
			// Procesa las encuestas
			processSurveys(container, chartsList);
			
			// Crea el contenedor para la información de experimentos
			jQryIter("<div id='experiment-container' class='visits-statistics-col'></div>").appendTo(container);
			
			// Crea el contenedor para la información de lecturas y redes sociales
			var readsAndSocialContainer = jQryIter("<div class='visits-statistics-row'></div>").appendTo(container);
			
			// Inicializa las opciones del gráfico de lecturas
			var chartOptions = {
				id: "readsStatistics",
				title:  options.titles.titleReadings,
				tooltips: [
				    options.tooltipReads,
				    options.tooltipVisits
				],
				data: {
					thousandSeparator: options.thousandSeparator
				},
				style: {
					mainContainerClass: "visits-statistics-item",
					titleContainerClass: "chart-title-container",
					chartContainerClass: "reads-chart-canvas"
				}
			};
			
			// Crea el gráfico de lecturas
			readChart = new VisitsPercentChart(chartOptions, readsAndSocialContainer);
			
			// Crea el bloque de estadísticas
			socialStatisticsContainer = jQryIter("<div id='socialStatistics' class='visits-statistics-item'></div>");
			// Facebook, twitter
			socialStatisticsContainer.append("<div class='chart-title-container'><span>" + options.titles.titleSocial + "</span></div>");
			socialStatisticsContainer.append('<div><ul id="socialsharesList"><li id="facebook"><div class="social-logo"></div><span id="facebook-statistics"></span></li><li id="twitter"><div class="social-logo"></div><span id="twitter-statistics"></span></li></ul></div>');
			// Disqus
			socialStatisticsContainer.append("<div class='chart-title-container'><span>" + options.titles.titleComments + "</span></div>");
			socialStatisticsContainer.append('<div><ul id="socialsharesList"><li id="disqus"><div class="social-logo"></div><span id="disqus-statistics"></span></li></ul></div>');
			// Añade el bloque a la página
			readsAndSocialContainer.append(socialStatisticsContainer);
		}
	}
	
	function processSurveys(container, chartsList)
	{
		// Si tiene encuestas
		if (data.surveys.length > 0) {
			for (var i = 0; i < data.surveys.length; i++) {
				const survey = data.surveys[i];

				// Crea un contenedor para la encuesta
				const surveyContainer = jQryIter("<div id='survey-" + survey.id + "' class='survey-stats-card'></div>").appendTo(container);
				
				// Añade la pregunta
				jQryIter("<div class='chart-title-container'><span>" + survey.text + "</span></div>").appendTo(surveyContainer);
				
				// Crea un contenedor para el resultado
				const surveyResultsContainer = jQryIter("<div></div>").appendTo(surveyContainer);
				
				// Añade las respuestas
				const chartLabels = [];
				const chartData = [];
				const chartTotalVotes = [];
				var totalVotes = 0;
				for (var j = 0; j < survey.choices.length; j++) {
					const choice = survey.choices[j];
					chartLabels.push(choice.text);
					chartData.push(choice.votes);
					totalVotes += choice.votes;
				}
				for (var votes in chartData)
					chartTotalVotes.push(totalVotes);
				
				// Añade un canvas
				const surveyCanvas = document.createElement("canvas");
				jQryIter(surveyResultsContainer).append(surveyCanvas);
				
				// Calcula la altura del canvas
				var height = ( (survey.choices.length * 25) + 50 ) + "px";
				jQryIter(surveyCanvas).css("max-height", height);
				jQryIter(surveyCanvas).css("min-height", height);
				
				// Inicializa el contexto
				const surveyCtx = surveyCanvas.getContext("2d");
				
				var self = this;
				
				var myBarChart = new Chart(surveyCtx, {
				    type: 'horizontalBar',
				    data: {
				        labels: chartLabels,
				        datasets: [{
				            label: '',
							backgroundColor: "rgba(151,187,205,0.8)",
							hoverBackgroundColor: "rgba(169,194,207,0.5)",
				            data: chartData
				        },
						{
							label: '',
							backgroundColor: "rgba(46,101,179,0.9)",
							hoverBackgroundColor: "rgba(100,133,181,0.5)",
							data: chartTotalVotes
						}]
				    },
				    options: {
						legend: {
							display: false
						},
						animation: false,
						maintainAspectRatio: false,
						scales: {
							xAxes: [{
								stacked: false,
								ticks: {
									beginAtZero: true,
									maxRotation: 0,
				                    callback: function(value, index, values) {
				                    	if (Math.floor(value) === value) {
				                    		return iterChartFormatNumber(value, self.options.thousandSeparator);
				                    	}
				                    }
				                }
							}],
							yAxes: [{
								stacked: true,
								ticks: {
									callback: function(value, index, values) {
										var maxLabelWidth = 150;
										var str = value;
										var strWidth = this.ctx.measureText(str).width;
										
										if (strWidth > maxLabelWidth)
										{
											var len = str.length;
											while (strWidth >= maxLabelWidth && len-->0) {
												str = str.substring(0, len) + "...";
												strWidth = this.ctx.measureText(str).width;
											}
										}
										else if (strWidth < maxLabelWidth)
										{
											while (strWidth < maxLabelWidth) {
												str = " " + str;
												strWidth = this.ctx.measureText(str).width;
											}
											str = str.substr(1);
										}
										return str;
									}
								}
							}]
						},
						tooltips: {
							custom: function(tooltip) {
						        if (!tooltip) return;
						        tooltip.displayColors = false;
						    },
					        callbacks: {
					            title: function(tooltipItem){
					            	return '';
					            },
					    		label: function(tooltipItem, data) {
					    			if (tooltipItem.datasetIndex > 0)
					    				return '';
					    			
					    			var votes = iterChartFormatNumber(data.datasets[0].data[tooltipItem.index], self.options.thousandSeparator);
					    			var total = iterChartFormatNumber(data.datasets[1].data[tooltipItem.index], self.options.thousandSeparator);
					    			var labels = [];
					    			labels.push(this._data.labels[tooltipItem.index]);
					    			labels.push(votes + ' (' + total + ')');
					    			return labels;
					    		}
					        }
					    }
					}
				});
				
				// Crea el gráfico de tendencias de votaciones
				var trendOptions = {
					tooltips: [''],
					data: {
						surveyId: survey.id,
						thousandSeparator: options.thousandSeparator
					},
					style: {
						mainContainerClass: ""
					}
				};
				chartsList.push(new ITER.statistics.SurveyTrendChart(trendOptions, "survey-" + survey.id));
			}
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////
	//                CONTROLADOR PETICIÓN DE ESTADISTICAS               //
	///////////////////////////////////////////////////////////////////////
	function update()
	{
		// Para el polling mientras se están pidiendo estadísticas
		clearTimeout(pollingTimeout);
		
		if (isRequestInProgress())
		{
			if (polling_rate > 0 && options.resolution == "minute" && isRealTimeChart(options.resolution))
			{
				pollingTimeout = setTimeout(update, polling_rate * 1000);
			}
		}
		else
		{
			// Construye la URL para solicitar por AJAX las estadísticas
			var url = buildURL();
			
			// Solicita las estadísticas
			jQryIter.ajax({url:url, dataType:'json', cache:false,
				success: function(statistics)
				{
					// Actualiza la hora actual con la del servidor
					currentServerTime = new Date(getBrowserDate(statistics.serverTime));
					
					// Actualiza la configuración usada para los datos visualizados
					updateMLNConfig(statistics.config);
					
					// Establece las fechas de las estadísticas
					currentChartTime = new Date(getBrowserDate(statistics.statisticsEndDate));
					jQryIter("#visits-date").text(statistics.statisticsStartDate + " / " + statistics.statisticsEndDate);
					
					// Si es un detalle de artículo, procesa los datos de redes sociales y de valoración de usuarios
					if ("article" === data.pageType)
					{
						if (statistics.socialStatistics)
						{
							processArticleSocialStatistics(statistics.socialStatistics);
						}
						if (statistics.feedbackStatistics)
						{
							processArticleFeedbackStatistis(statistics.feedbackStatistics);
						}
						if (statistics.abtesting)
						{
							processABTesting(statistics.abtesting, data, options);
						}
					}
					
					// Pide los gráficos
					var testRequest = {
						baseUrl: "/base-portlet/statistics/",
						group: data.groupId,
						resolution: options.resolution,
						dateLimit: buildDate(),
						displayedHours: options.displayedHours,
						realTime: options.realTime,
						loadingOverlay: refreshCharts
					};
					
					for(var i=0; i<chartsList.length; i++)
						chartsList[i].requestData(testRequest);
					
					// Si se había solicitado el refresco de los gráficos, se desactiva para que se sólo se actualicen.
					refreshCharts = false;
					
					// Si están habilitadas las estadísticas en tiempo real, la resolución es "por minutos"
					// y el rango de horas incluye la hora actual, activa el polling.
					if (polling_rate > 0 && options.resolution == "minute" && isRealTimeChart(options.resolution))
					{
						pollingTimeout = setTimeout(update, polling_rate * 1000);
					}
				}
			});
		}
	};
	
	function isRequestInProgress()
	{
		var inProgress = false;
		
		for(var i=0; i<chartsList.length; i++)
		{
			if (chartsList[i].isRetrievingStatistics())
			{
				inProgress = true;
				break;
			}
		}
		return inProgress;
	}

	///////////////////////////////////////////////////////////////////////
	//              MANEJADORES DE LAS ESTADISTICAS DE MAS               //
	///////////////////////////////////////////////////////////////////////
	this.toggleMAS = function toggleMAS()
	{
		if (typeof data.masIdSite !== 'undefined' && data.masIdSite)
		{
			var iframeContainer = document.getElementById("mas-frame-container");
			
			if (iframeContainer)
			{
				hideMasStatistics();
			}
			else
			{
				if (!masToken)
					masToken = getMasToken(data.masIdSite);
				
				if (masToken)
				{
					var url = buildMasUrl(data, options, masToken);
					showMasStatistics(url);
				}
			}
		}
	};
	
	function getMasToken(masIdSite)
	{
		var endpoint = options.masUrl + "/index.php?module=API&method=UtilsMAS.getTokenAuth&format=JSON";
		var data = "login=" + masIdSite + "&password=" + masIdSite.split("").reverse().join("");
		var token = null;
		
		jQryIter.ajax({
			type: "POST",
			url:endpoint,
			dataType:'json',
			data: data,
			async: false,
			success: function(response)
			{
				if (response.length > 0 && response[0].status === "true")
					token = response[0].token_auth;
				else
					console.log("Unable to get MAS access token.");
			}
		});
		
		return token;
	}
	
	function buildMasUrl(data, options, masToken)
	{
		var period = "";
		var date = "";
		var range = jQryIter("#visits-date").text().split(" / ");
		switch (options.resolution)
		{
		// Si se está visualizando un mes, se agrupa MAS por días para el mes completo.
		case "day":
			period = "range";
			date = range[0].split(" ")[0] + "," + range[1].split(" ")[0];
			break;

		// Si se está visualizando un año, se agrupa MAS por meses hasta diciembre del año indicado.
		case "month":
			// Si son estadísticas de artículo
			period = data.pageType === 'article' ? "year" : "month";
			date = range[1].split(" ")[0];
			break;
		
		// Si se está visualizando un día, se agrupa MAS por días hasta el día indicado.
		default:
			period = "day";
			date = range[1].split(" ")[0];
		}

		var url = options.masUrl + "/index.php?module=Widgetize&action=iframe&readonly=" + options.readOnly;
		
		// Si son estadísticas de artículo
		if (data.pageType === 'article')
			url += "&moduleToWidgetize=CustomDimensions&actionToWidgetize=getCustomDimension&idDimension=2&disableLink=1&widget=1&articleId=" + data.contentIdCrc + data.itemId;
		// Si no se visualizan estadísticas, se pide el plugin de metadatos.
		else if (!data.showVisitsTrend && data.maxArticles==0 && data.maxComments==0 && data.maxFeedback==0 && data.maxReadings==0 && data.maxSections==0 && data.maxSharings==0)
			url += "&moduleToWidgetize=MetadataMAS&actionToWidgetize=getMetadataReport&disableLink=1&widget=1";
		// Si se visualizan estadísticas de visitas, se pide a MAS el dashboard.		
		else
			url += "&moduleToWidgetize=Dashboard&actionToWidgetize=index";
		
		url += "&idSite="     + data.masIdSite
			+  "&period="     + period
			+  "&date="       + date
			+  "&token_auth=" + masToken;
		
		return url;
	}
	
	function showMasStatistics(url)
	{
		// Inicializa el contenedor del iframe
		iframeContainer = document.createElement('div');
		iframeContainer.id = "mas-frame-container";
		
		// Inicializa el iframe
		var iframe = document.createElement('iframe');
		iframe.id = "mas-frame";
		iframe.scrolling = "no";
		
		// Crea el iFrame
		iframe.src = url;
		iframe.setAttribute("frameborder", "0");
		iframe.setAttribute("marginheight", "0");
		iframe.setAttribute("marginwidth", "0");
		iframe.setAttribute("scrolling", "yes");
		iframeContainer.appendChild(iframe);
		
		// Oculta las estadísticas de Iter
		jQryIter("#visits-content").hide();
		jQryIter("#visits-date").hide();
		jQryIter("#visits-buttons").hide();
		jQryIter("#socialStatistics").hide();
		jQryIter(".feedback-chart-container").hide();
		
		// Abre las estadisticas en el iframe
		document.body.appendChild(iframeContainer);
		
		// Colorea el botón
		jQryIter("#mas-button").addClass("enabled");
		
		// Notifica a MLN
		var event = jQryIter("#MCMEvent\\:statisticDetail");
		event.attr("statistictype", "masstatistics");
		event.attr("idcms", "enabled");
		event.attr("titleart", "");
		event.trigger( "click" );
	}
	
	function hideMasStatistics()
	{
		// Desmarca el botón
		jQryIter("#mas-button").removeClass("enabled");
		// Elimina las estadísticas de MAs
		jQryIter(iframeContainer).remove();
		// Muestra las estadísticas de Iter
		jQryIter("#visits-content").show();
		jQryIter("#visits-date").show();
		jQryIter("#visits-buttons").show();
		jQryIter("#socialStatistics").show();
		jQryIter(".feedback-chart-container").show();
		
		// Notifica a MLN
		var event = jQryIter("#MCMEvent\\:statisticDetail");
		event.attr("statistictype", "masstatistics");
		event.attr("idcms", "");
		event.attr("titleart", "");
		event.trigger( "click" );
	}
	
	///////////////////////////////////////////////////////////////////////
	//                MANEJADORES DEL GRÁFICO DE VISITAS                 //
	///////////////////////////////////////////////////////////////////////
	this.changeGroup = function changeGroup(groupId) {
		jQryIter.setQueryParam("groupId", groupId);
	};
	
	this.forceUpdate = function forceUpdate() {
		update();
	};
	
	this.getVisitsByResolution = function getVisitsByResolution(resolution, realTime)
	{
		cancelRequestsInProgress();
		refreshCharts = true;
		options.resolution = resolution;
		options.realTime = realTime;
		currentChartTime = null;
		update();
	};
	
	this.getPrevStatistics = function getPrevStatistics()
	{
		cancelRequestsInProgress();
		refreshCharts = true;

		if (options.resolution == "minute")
			currentChartTime.setHours(currentChartTime.getHours() - options.displayedHours);
		else if (options.resolution == "hour")
			currentChartTime.setHours(currentChartTime.getHours() - 24);
		else if (options.resolution == "day")
			currentChartTime.setMonth(currentChartTime.getMonth() - 1, 1);
		else if (options.resolution == "month")
			currentChartTime.setFullYear(currentChartTime.getFullYear() - 1, 0, 1);
		
		update();
	};
	
	this.getNextStatistics = function getNextStatistics()
	{
		// Si la hora actual está dentro del rango, no se permite avanzar hacia delante
		if (!isRealTimeChart(options.resolution))
		{
			cancelRequestsInProgress();
			refreshCharts = true;
			
			if (options.resolution == "minute")
				currentChartTime.setHours(currentChartTime.getHours() + options.displayedHours);
			else if (options.resolution == "hour")
				currentChartTime.setHours(currentChartTime.getHours() + 24);
			else if (options.resolution == "day")
				currentChartTime.setMonth(currentChartTime.getMonth() + 1, 1);
			else if (options.resolution == "month")
				currentChartTime.setFullYear(currentChartTime.getFullYear() + 1, 0, 1);
			
			update();
		}
	};
	
	function cancelRequestsInProgress()
	{
		for(var i=0; i<chartsList.length; i++)
		{
			chartsList[i].abortUpdate();
		}
	}
	
	///////////////////////////////////////////////////////////////////////
	//                          GESTION GRAFICOS                         //
	///////////////////////////////////////////////////////////////////////
	
	function processArticleSocialStatistics(socialStatistics)
	{
		if (socialStatistics != undefined)
		{			
			jQryIter("#facebook-statistics").text( iterChartFormatNumber(socialStatistics.facebook, options.thousandSeparator) );
			jQryIter("#twitter-statistics").text( iterChartFormatNumber(socialStatistics.twitter, options.thousandSeparator) );
			jQryIter("#disqus-statistics").text( iterChartFormatNumber(socialStatistics.disqus, options.thousandSeparator) );
		}
	}
	
	function processArticleFeedbackStatistis(feedbackStatistics)
	{
		if (feedbackStatistics)
		{
			if (feedbackChart)
			{
				feedbackChart.data.labels = feedbackStatistics.labels;
				feedbackChart.data.datasets[0].data = feedbackStatistics.votes;
				feedbackChart.update();
			}
			else
			{
				var container = document.createElement("div");
				jQryIter(container).addClass("feedback-chart-container");
				jQryIter(document.body).append(container);
				
				// Crea el título del gráfico
				var titleContainer = jQryIter(document.createElement("div"));
				titleContainer.addClass("chart-title-container");
				titleContainer.append("<span>"+options.titles.titleFeedback+"</span>");
				jQryIter(container).append(titleContainer);
				
				// Añade un canvas
				canvas = document.createElement("canvas");
				jQryIter(canvas).addClass("feedback-chart-canvas");
				jQryIter(container).append(canvas);
				
				// Inicializa el contexto
				ctx = canvas.getContext("2d");
				
				var chartData = {
					labels: feedbackStatistics.labels,
					datasets: [{
						data: feedbackStatistics.votes,
						backgroundColor: ["#2e65b3", "#97bbcd", "#00b0ed", "#2e9fff"]
					}]
				};
				
				var chartOptions = {
					maintainAspectRatio: false,
				    legend: {
				    	position: 'left',
				    	onClick: function(event, legendItem) {}
				    },
				    iterOptions: {
				    	maxWidhtForLateralLegend: 300
				    }
				};
				
				feedbackChart = new Chart(ctx, { data: chartData, type: 'pie', options: chartOptions });
			}
		}
	}

	// Comprueba si hay información de ABTesting y, si es así, crea el contenedor para la información, renderiza las variantes e informa los datos.
	function processABTesting(abtestingInfo, data, options) {
		if (typeof abtestingInfo !== 'undefined')
		{
			// Recupera el contenedor
			var container = jQryIter("#experiment-container");
			
			// Si no existe, crea el elemento para la información del experimento
			var experimentInfoContainer = jQryIter("#experiment-info-container");
			if (experimentInfoContainer.length === 0) {
				experimentInfoContainer = jQryIter("<div id='experiment-info-container' class='visits-statistics-item flex-container'></div>").appendTo(container);
				
				jQryIter('<div class="experiment-info-block"><div id="experiment-status" class="experiment-info-header">' + options.experimentLiterals.status + '</div><div class="experiment-info-data"></div></div>').appendTo(experimentInfoContainer);
				jQryIter('<div class="experiment-info-block"><div id="experiment-start" class="experiment-info-header">' + options.experimentLiterals.startDate + '</div><div class="experiment-info-data"></div></div>').appendTo(experimentInfoContainer);
				jQryIter('<div class="experiment-info-block"><div id="experiment-finish" class="experiment-info-header">' + options.experimentLiterals.endDate + '</div><div class="experiment-info-data"></div></div>').appendTo(experimentInfoContainer);
			}
			
			// Si no existe, crea el elemento para renderizar las variantes
			if (jQryIter("#experiment-variants-container").length === 0) {
				
				// Crea el contenedor
				var renderContainer = jQryIter("<div id='experiment-variants-container' class='experiment-preview'></div>").insertAfter(container);
			
				// Renderiza en ella el artículo
				var url = data.virtualHost + "/news-portlet/renderArticle/" + data.itemId + "/" + btoa("/AB/PLANTILLA_AB_TESTING");
				jQryIter.ajax({url:url, dataType:'html',
					context: this,
					success: function(data, textStatus, jqXHR)
					{
						// Inserta las variantes
						renderContainer.append(data);
						
						// Traduce los literales
						jQryIter(".ctr").closest(".experiment-variant-metric").find(".experiment-variant-metric-header").text(options.experimentLiterals.ctr);
						jQryIter(".prints").closest(".experiment-variant-metric").find(".experiment-variant-metric-header").text(options.experimentLiterals.impressions);
						jQryIter(".views").closest(".experiment-variant-metric").find(".experiment-variant-metric-header").text(options.experimentLiterals.views);
						
						// Carga los valores guardados
						if (typeof demoTestAB !== 'undefined') {
							var variantname = Object.keys(demoTestAB);
							for (var i = 0; i < variantname.length; i++ ) {	
								jQryIter(".variant-" + variantname[i] + " .ctr").text(demoTestAB[variantname[i]].ctr);
								jQryIter(".variant-" + variantname[i] + " .prints").text(demoTestAB[variantname[i]].prints);
								jQryIter(".variant-" + variantname[i] + " .views").text(demoTestAB[variantname[i]].visits);
							}
						}
						
						// Establece el ganador
						if (typeof abtestingInfo.winner !== 'undefined') {
							jQryIter(".variant-" + abtestingInfo.winner).addClass("winner");
						} else {
							jQryIter(".experiment-variant-metric").removeClass("winner");
						}
					}
				});
			}
			
			// Calcula el estado del experimento
			var status =  abtestingInfo.startdate && abtestingInfo.finishdate ? options.experimentLiterals.finished : abtestingInfo.startdate ? options.experimentLiterals.runnning : options.experimentLiterals.pending;
			var startDate = abtestingInfo.startdate ? new Date(abtestingInfo.startdate).toLocaleDateString() + ' ' + new Date(abtestingInfo.startdate).toLocaleTimeString() : '-';
			var finishDate = abtestingInfo.finishdate ? new Date(abtestingInfo.finishdate).toLocaleDateString() + ' ' + new Date(abtestingInfo.finishdate).toLocaleTimeString() : '-';

			// Establece la metainformación
			jQryIter("#experiment-status").next().text(status);
			jQryIter("#experiment-start").next().text(startDate);
			jQryIter("#experiment-finish").next().text(finishDate);
			
			// Si el experimento está en curso, añade el botón para pararlo manualmente
			if (status === options.experimentLiterals.runnning) {
				jQryIter('<button type="button" class="experiment-stop-button" title="' + options.experimentLiterals.stop + '" name="" value="" onclick="stopArticleExperiment(' + data.groupId + ', \'' + data.itemId + '\')"></button>').appendTo(jQryIter("#experiment-status").next());
			}
			
			// Establece el ganador
			if (typeof abtestingInfo.winner !== 'undefined') {
				jQryIter(".variant-" + abtestingInfo.winner).addClass("winner");
			} else {
				jQryIter(".experiment-variant-metric").removeClass("winner");
			}
		}
	}
	
	////////////////////////////////////////////////////////////////
	//                    FUNCIONES AUXILIARES                    //
	////////////////////////////////////////////////////////////////
	function buildDate()
	{
		var date = "";
		
		// Si se ha pedido explícitamente una fecha, se usa esta una sola vez
		if (options.dateLimit)
		{
			date = options.dateLimit;
			options.dateLimit = "";
		}
		// Si ya se han pedido estadísticas al menos una vez y no estamos visualizando un gráfico de minutos en tiempo real
		else if (currentChartTime != undefined && !(options.resolution == "minute" && isRealTimeChart(options.resolution)))
		{
			date = currentChartTime.getFullYear() + ("0" + (currentChartTime.getMonth() + 1)).slice(-2) + ("0" + currentChartTime.getDate()).slice(-2) +
					("0" + currentChartTime.getHours()).slice(-2) + ("0" + currentChartTime.getMinutes()).slice(-2) + ("0" + currentChartTime.getSeconds()).slice(-2); 
		}
		
		// En cualquier otro caso, la fecha se genera automáticamente con la fech actual del servidor
		return date;
	}
	
	function buildURL()
	{
		var statsDate = buildDate();
		
		var url = "/base-portlet/statistics/" + data.pageType + "?item=" + data.pageType + "&groupId=" + data.groupId + "&resolution=" + options.resolution;
		
		if (statsDate != "")                 url += "&dateLimit="      + statsDate;
		if ("minute" === options.resolution) {
			url += "&realTime="       + options.realTime;
			if (options.displayedHours != 24) {
				url += "&displayedHours=" + options.displayedHours;
			}
		}
		
		if ("article" === data.pageType)
			url += "&articleId=" + data.itemId;
		
		return url;
	}
	
	function isRealTimeChart(resolution)
	{
		return new Date(currentChartTime).setMinutes(59, 59, 999) >= currentServerTime;
	};
	
	function getBrowserDate(dateTime)
	{
		var dataBrowser = [
            {subString: "Edge", identity: "MS Edge"},
            {subString: "MSIE", identity: "Explorer"},
            {subString: "Trident", identity: "Explorer"},
            {subString: "Firefox", identity: "Firefox"},
            {subString: "Opera", identity: "Opera"},  
            {subString: "OPR", identity: "Opera"},
            {subString: "Chrome", identity: "Chrome"}, 
            {subString: "Safari", identity: "Safari"}       
        ];
		
		var browser = "Others";
		
        for (var i = 0; i < dataBrowser.length; i++)
        {
            if (navigator.userAgent.indexOf(dataBrowser[i].subString) !== -1) {
            	browser = dataBrowser[i].identity;
            	break;
            }
        }

		// ITER-644: se añade Safari en la condición para ajustar el formato de la fecha
		if (browser == "Explorer" || browser == "Firefox" || browser == "Safari")
			dateTime = dateTime.replace(" ", "T");
        
        return dateTime + getBrowserTimezone(dateTime);
    };
    
    function getBrowserTimezone(dateTime)
    {
    	var timezone = "";

    	var offset = new Date(dateTime).getTimezoneOffset();
    	if (offset !== 0)
    	{
    		timezone += offset < 0 ? "+" : "-";

    		var hour = "" + Math.floor((Math.abs(offset) / 60));
    		if (hour.length === 1) hour = '0' + hour;
    		
    		var minute = "" + (Math.abs(offset) % 60);
    		if (minute.length === 1) minute = '0' + minute;
    		
    		timezone += hour + ":" + minute;
    	}
    	
    	return timezone;
    };
    
    function updateMLNConfig(config)
    {
    	// MLN
    	var mlnConfig = jQryIter("#MCMEvent\\:statisticConfig");
    	mlnConfig.attr("resolution", config.resolution);
    	mlnConfig.attr("date",config.date);
    	mlnConfig.attr("displayedHours", config.displayedHours);
    	mlnConfig.attr("realTime", config.realTime);
    	mlnConfig.trigger("click");
    	
    	// iFrame
    	var pass_data = {
    		"operation": "navigation",
    		"data": {
	    		"resolution": config.resolution,
			    "date": config.date,
			    "displayedHours": config.displayedHours,
			    "realTime": config.realTime
    		}
		};
		parent.postMessage(JSON.stringify(pass_data), '*');
    }
}

/**************************************************************************
 * 
 * FUNCIONES AUXILIARES
 * 
 **************************************************************************/

function iterChartFormatNumber(number, separator)
{
	if (separator == null || separator.length == 0)
		return number;
	
	number += '';
    var x = number.split('.');
    var x1 = x[0];
    var x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + separator + '$2');
    }
    return x1 + x2;
}

function sumArrays(ar1, ar2)
{
	var sum = [];
	if (ar1.length === ar2.length) {
		for (var i = 0; i < ar1.length; i++)
		{
			sum.push(ar1[i] + ar2[i]);
		}
	}
	return sum;
}

function stopArticleExperiment(groupId, articleId) {
	var url = "/base-portlet/live/endpoint";
	var data = '<http-rpc><invoke clsid="com.liferay.portal.kernel.util.ABTestingMgr" dispid="1" methodName="finishABTesting"><params>'
		     + '<param index="0" vt="3">' + groupId + '</param>'
		     + '<param index="1" vt="8">' + articleId + '</param>'
		     + '</params></invoke></http-rpc>';
	
	jQryIter.ajax({type: "POST", url: url, data: data, dataType:'xml',
		// Si ha ido bien, Crea / Actualiza el gráfico
		success: function(statistics, textStatus, jqXHR)
		{
			chartMgr.forceUpdate();
		}
	});
}

//Plugin para poner un porcentaje dentro de un gráfico de donut
Chart.plugins.register({
	afterDraw: function(chart)
	{
		if (chart.config.options.elements.centerPercentText)
		{
			var ctx = chart.chart.ctx;
			
			// Calcula el valor
			var charData = chart.chart.config.data.datasets[0].data;
			var totalVisits = charData[0] + charData[1];
			var reads = chart.chart.config.data.datasets[0].data[0];
			var percentReads = Math.round( reads / totalVisits * 100 );
			var percentText = isNaN(percentReads) ? "" : percentReads + "%";
			
			if (percentText != "")
			{
				// Guarda los parámetros del canvas
				ctx.save();
				
				// Calcula el tamaño óptimo del texto
				var fontSizes = [72, 36, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 5, 2];
				var i = 0;
				ctx.font = "bold " + fontSizes[i] + "px Roboto";
				var fontMeasure = ctx.measureText(percentText).width;
				var fontMaxWidth = chart.innerRadius * 2;
				while (fontMeasure > fontMaxWidth && i <= fontSizes.length)
				{
					ctx.font = "bold " + fontSizes[++i] + "px Roboto";
					fontMeasure = ctx.measureText(percentText).width;
				}
				
				// Pinta el texto
				ctx.textAlign = 'center';
				ctx.textBaseline = 'middle';
				ctx.fillStyle = "#747474";
				ctx.fillText(percentText, chart.chart.width / 2, (chart.chartArea.top + chart.chart.height) / 2);
				
				// Restablece los parámetros del canvas
				ctx.restore();
			}
		}
	}
});

// Plugin para anotaciones
Chart.plugins.register({
	afterDatasetsDraw: function(chartInstance, easing)
	{
		var iterOptions = chartInstance.options.iterOptions;
		if (iterOptions)
		{
			// Recupera las opciones de las anotaciones
			var annotationOptions = iterOptions.annotation;
			if (annotationOptions != undefined)
			{
				// Busca la escala en la que pintar las anotaciones
				var scale = chartInstance.scales[annotationOptions.scaleId];
				if (scale != undefined)
				{
					var sysannotations = annotationOptions.sysannotations;
					if (sysannotations != undefined && sysannotations.length > 0)
					{
						// Prepara el área de pintado
						var ctx = chartInstance.chart.ctx;
						ctx.save();

						// Recorre las anotaciones
						for (var i = 0; i < sysannotations.length; i++)
						{
							// Calcula la posición
							var annotation = sysannotations[i];
							var label =  annotation.label;
							var x0;
							if (label.length == 5) // hh:mm
							{
								// Comprueba si existe un tick para la etiqueta
								if (!isNaN(scale.getPixelForValue(label)))
								{
									x0 = scale.getPixelForValue(label);
								}
								// Si no, calcula la posición entre tikcs
								else
								{
									var hour = label.split(':')[0];
									var nextHour = parseInt(hour) == 23 ? "00" : parseInt(hour) + 1;
									nextHour =  "00".substring(0, 2 - nextHour.toString().length) + nextHour;
									var labelFromPosition = scale.getPixelForValue(hour + ":00h");
									var labelToPosition = scale.getPixelForValue(nextHour + ":00h");
									
									var range = labelToPosition - labelFromPosition;
									var minutes = parseInt(label.split(':')[1]);
									x0 = Math.floor(labelFromPosition + (range * minutes / 60)) + 0.5;
								}
							}
							else
								x0 = Math.floor(scale.getPixelForValue(annotation.label)) + 0.5;
							
							var y0 = chartInstance.chartArea.top - 5;
							var y1 = chartInstance.chartArea.bottom;
							var collisionAreaWidth = 2;
							
							// Agrupa las anotaciones que se solapan.
							// Para ello, comprueba si el área del hover se solapa con el de otra anotación.
							// Si es así, fusiona el texto al de la anotación ya pintada y elimina la actual.
							var overlapped = false;
							for (var j = 0; j < i; j++)
							{
								var previousAnnotation = sysannotations[j];
								
								var rect1 = {x0: x0 - collisionAreaWidth, x1: x0 + collisionAreaWidth};
								var rect2 = {x0: previousAnnotation.position.x[0], x1: previousAnnotation.position.x[1]};
								
								if (rect1.x0 < rect2.x1 && rect1.x1 > rect2.x0)
								{
									overlapped = true;
									previousAnnotation.note += "\n" + annotation.note;
									sysannotations.splice(i, 1);
									i--;
									break;
								}
							}
							
							// Si no hubo solape...
							if (!overlapped)
							{
								// Pinta la anotación
								ctx.lineWidth = 1;
								ctx.strokeStyle = '#ff0000';
								
								ctx.beginPath();
								ctx.moveTo(x0,y0);
								ctx.lineTo(x0,y1);
								ctx.stroke();
								
								// Añade la posición para el tooltip
								annotation.position = {x: [x0 - collisionAreaWidth, x0 + collisionAreaWidth], y: [y0, y1]};
							}
						}
						
						// restablece el área de pintado
						ctx.restore();
					}
					
					var annotations = annotationOptions.annotations;
					var imgSrc = iterOptions.annotation.icon;
					// Si hay anotaciones y se ha definido una imagen
					if (annotations != undefined && annotations.length > 0 && imgSrc != undefined)
					{
						// Prepara el área de pintado
						var ctx = chartInstance.chart.ctx;
						ctx.save();
						
						// Recorre las anotaciones
						for (var i = 0; i < annotations.length; i++)
						{
							// Calcula la posición
							var annotation = annotations[i];
							var x0 = scale.getPixelForValue(annotation.label);
							var y0 = chartInstance.chartArea.bottom;
							
							// Pinta la anotación
							if (!isNaN(x0))
							{
								var img = iterOptions.annotation.iconImg;
								if (img)
								{
									ctx.drawImage(img, x0 - img.width / 2, y0 - img.height);
									// Añade la posición para el hover
									annotation.position = {x: [x0 - img.width / 2, x0 + img.width / 2], y: [y0 - img.height, y0]};
								}
								else
								{
									img = new Image();
									img.onload = function() {
										ctx.drawImage(img, x0 - img.width / 2, y0 - img.height);
										iterOptions.annotation.iconImg = img;
										// Añade la posición para el hover
										annotation.position = {x: [x0 - img.width / 2, x0 + img.width / 2], y: [y0 - img.height, y0]};
									};
									img.src = imgSrc;
								}
							}
						}
						
						// restablece el área de pintado
						ctx.restore();
					}
				}
			}
		}
	}
});


// Plugin para modificar la posición de la leyenda en el gráfico de valoración
Chart.plugins.register({
	resize: function(chartInstance, newChartSize) {
		if (chartInstance.options.iterOptions && chartInstance.options.iterOptions.maxWidhtForLateralLegend)
		{
			if (newChartSize.width < chartInstance.options.iterOptions.maxWidhtForLateralLegend && chartInstance.options.legend.position === "left")
			{
				chartInstance.options.legend.position = "top";
			}
			else if (newChartSize.width >= chartInstance.options.iterOptions.maxWidhtForLateralLegend && chartInstance.options.legend.position === "top")
			{
				chartInstance.options.legend.position = "left";
			}
		}
	}
});

/**************************************************************************
 * 
 * GRÁFICOS DE VISITAS Y LECTURAS
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 **************************************************************************/
//namespace global
var ITER = ITER || {};

ITER.statistics = {
	Chart: function(customOptions, parentElementId)
	{	
		// Gráfico
		this.chart = null;
		// Div contenedor del gráfico
		this.container = null;
		// Contenedor del título del gráfico
		this.titleContainer = null;
		// Canvas en el que se pinta el gráfico
		this.canvas = null;
		// Contexto del canvas en el que se pinta el gráfico
		this.ctx = null;
		
		// Opciones del gráfico
		this.options = {
			id: "",
			title: "",
			tooltips: [],
			data: {
				pageType: "",
				itemType: "",
				itemId: "",
				thousandSeparator: "",
				annotations: false
			},
			style: {
				mainContainerClass: "",
				titleContainerClass: "",
				chartContainerClass: "",
				messageContainerClass: ""
			},
			callback: null
		};
		
		// Indidor de que se están pidiendo estadísticas
		this.retievingStatistics = false;
		// Request actual de petición de estadísticas
		this.xhr = null;
		
		/**
		 * Crea el contenedor y el canvas e inicializa el contexto.
		 */ 
		this.createContext = function()
		{
			var self = this;
			
			// Busca el elemento padre
			var parentElement = null;
			if (parentElementId != undefined)
				parentElement = jQryIter("#" + parentElementId);

			// Si no lo encuentra o no se ha informado, usa el body
			if (parentElement == undefined)
				parentElement = jQryIter(document.body);
			
			// Crea el contenedor del gráfico
			this.container = document.createElement("div");
			jQryIter(this.container).addClass(this.options.style.mainContainerClass);
			parentElement.append(this.container);
			
			// Crea el título del gráfico si se indica
			if (this.options.title)
			{
				this.titleContainer = jQryIter(document.createElement("div"));
				if (this.options.style.titleContainerClass)
					this.titleContainer.addClass(this.options.style.titleContainerClass);
				this.titleContainer.append("<span>"+this.options.title+"</span>");
				this.titleContainer.click( function() {
					jQryIter(this).next().toggle();
					if (self.chart) {
						self.chart.resize(); // Actualiza el tamaño del gráfico
						self.chart.update(); // Repinta el gráfico
					}
				});
				jQryIter(this.container).append(this.titleContainer);
			}
		};

		
		// Inicialización
		// Añade las opciones personalizadas
		jQryIter.extend(true, this.options, customOptions);
		// Inicializa el contexto
		this.createContext();
	}
};

ITER.statistics.Chart.prototype.isRetrievingStatistics = function()
{
	return this.retievingStatistics;
};

ITER.statistics.Chart.prototype.abortUpdate = function()
{
	if(this.retievingStatistics)
		this.xhr.abort();
};

ITER.statistics.Chart.prototype.loadingOverlay = function(status)
{
	if (status && jQryIter(this.container).find("#loading-overlay").length == 0)
	{
		if (parseInt(jQryIter(this.container).css("minHeight").replace(/[^-\d\.]/g, '')) == 0)
			jQryIter(this.container).css("minHeight", "75px");
			
		jQryIter(this.container).append('<div id="loading-overlay" style="display: none;"><div class="overlay-content"><div class="ball"></div><div class="ball1"></div></div></div>');
		jQryIter(this.container).find("#loading-overlay").fadeIn(500);
	}
	else if (jQryIter(this.container).find("#loading-overlay").length > 0)
	{
		if (parseInt(jQryIter(this.container).css("minHeight").replace(/[^-\d\.]/g, '')) == 75)
			jQryIter(this.container).css("minHeight", "");
		
		jQryIter(this.container).find("#loading-overlay").remove();
	}
};

ITER.statistics.Chart.prototype.requestData = function(request)
{
	// Preserva el elemento this
	var self = this;
	
	// Construye la URL
	var url = this.buildRequestURL(request);
	
	// Pide las estadísticas
	this.xhr = jQryIter.ajax({url:url, dataType:'json', cache:false,
		// Antes de realizar la petición, pone el overlay de carga si se solicita
		beforeSend: function(jqXHR, setting)
		{
			self.retievingStatistics = true;
			if (request.loadingOverlay)
				self.loadingOverlay(true);
		},
		// Si ha ido bien, Crea / Actualiza el gráfico
		success: function(statistics, textStatus, jqXHR)
		{
			// Actualiza el título
			if (self.titleContainer)
				self.titleContainer.find("span").text(statistics.title);
			
			// Crea o actualiza el área de pintado
			self.initializeCanvas(statistics);
			
			if (self.chart == null)
				self.chart = self.createChart(statistics.data);
			else
				self.updateChart(statistics.data);
			
			// Añade los eventos de click
			self.attachStatisticsDetailEvent(statistics.data);
			
			// Si se indica un callback al finalizar la petición, lo llama
			if (self.options.callback)
				self.options.callback(statistics);
		},
		// Al finalizar, elimina el overlay de carga si se había solicitado
		complete: function(jqXHR, textStatus)
		{
			self.retievingStatistics = false;
			if (request.loadingOverlay)
				self.loadingOverlay(false);
		}
	});
};

////////////////////////////////////////////////////////////////////
// Métodos a sobreescribir por las clases específicas de gráficos //
////////////////////////////////////////////////////////////////////
// Construye la URL de solicitud de estadísticas
ITER.statistics.Chart.prototype.buildRequestURL = function(request) { };
//Crea o actualiza el área de pintado
ITER.statistics.Chart.prototype.initializeCanvas = function(data) { };
// Crea el gráfico y asigna los datos
ITER.statistics.Chart.prototype.createChart = function(data) { };
// Actualiza los datos del gráfico
ITER.statistics.Chart.prototype.updateChart = function(data) { };
//Añade los eventos de click
ITER.statistics.Chart.prototype.attachStatisticsDetailEvent = function(data) { };


///////////////////////////////////////////////////////////////
//             GRÁFICO DE TENDENCIAS DE VISITAS              //
///////////////////////////////////////////////////////////////
ITER.statistics.TrendChart = function(customOptions, parentElementId)
{
	ITER.statistics.Chart.call(this, customOptions, parentElementId);
};
ITER.statistics.TrendChart.prototype = Object.create(ITER.statistics.Chart.prototype);
//Construye la URL de solicitud de estadísticas
ITER.statistics.TrendChart.prototype.buildRequestURL = function(request)
{
	var url = request.baseUrl + this.options.data.pageType + "?chartType=trend&groupId=" + request.group+ "&item=" + this.options.data.itemType + "&resolution=" + request.resolution;
	
	switch(this.options.data.pageType) {
	    case "section":  url += "&plid="       + this.options.data.itemId; break;
	    case "article":  url += "&articleId="  + this.options.data.itemId; break;
	    case "metadata": url += "&categoryId=" + this.options.data.itemId; break;
	}
	
	if (request.dateLimit)               url += "&dateLimit="      + request.dateLimit;
	if ("minute" === request.resolution) {
		url += "&realTime=" + request.realTime;
		if (request.displayedHours != 24) {
			url += "&displayedHours=" + request.displayedHours;
		}
	}
	if (this.options.data.annotations)   url += "&annotations="    + this.options.data.annotations;
	
	return url;
};

ITER.statistics.TrendChart.prototype.initializeCanvas = function(data)
{
	//Crea el canvas si no existe
	if (this.canvas == null)
	{
		// Añade un canvas
		this.canvas = document.createElement("canvas");
		jQryIter(this.container).append(this.canvas);
		
		// Inicializa el contexto
		this.ctx = this.canvas.getContext("2d");
		
		var self = this;
		jQryIter(this.canvas).mousemove(function(e){handleMouseMove(e);});
	    // show tooltip when mouse hovers over dot
	    function handleMouseMove(e)
	    {
	    	var iterOptions = self.chart.options.iterOptions;
	    	
	    	if (iterOptions && iterOptions.annotation)
	    	{
	    		// Añade un evento para elimiar el tooltip si se presiona ESC
    	    	jQryIter(document).keyup(function(e) {
    	    		if (e.keyCode == 27 && jQryIter("#iterchart-tooltip"))
    	    			jQryIter("#iterchart-tooltip").remove();
    	    	});
    	    	
	    		jQryIter("#iterchart-tooltip").remove();
	    		jQryIter(self.canvas).css("cursor", "default");
	    		
	    		var rect = self.canvas.getBoundingClientRect();
	    		var mouseX = parseInt(e.clientX - rect.left);
	    	    var mouseY = parseInt(e.clientY - rect.top);
	    	    
	    	    var hit = false;
	    	    
	    	    if (iterOptions.annotation.annotations)
	    	    {
	    	    	var annotations = iterOptions.annotation.annotations;
	    	    	for (var i = 0; i < annotations.length; i++)
					{
	    	    		var annotation = annotations[i];
			    	    if (mouseX > annotation.position.x[0] &&
			    	    	mouseX < annotation.position.x[1] &&
			    	    	mouseY > annotation.position.y[0] &&
			    	    	mouseY < annotation.position.y[1])
			    	    {
				    		jQryIter(self.canvas).css("cursor", "pointer");
				    		hit = true;
				    		break;
			    	    }
					}
	    	    }
	    	    
		    	if (!hit && iterOptions.annotation.sysannotations)
		    	{
		    		var timestampRegex = /^(\[\d{2}:\d{2}:\d{2}\])(.*)/;
		    		var annotations = iterOptions.annotation.sysannotations;
		    	    
		    	    for (var i = 0; i < annotations.length; i++)
					{
		    	    	var annotation = annotations[i];
			    	    if (mouseX > annotation.position.x[0] &&
			    	    	mouseX < annotation.position.x[1] &&
			    	    	mouseY > annotation.position.y[0] &&
			    	    	mouseY < annotation.position.y[1])
			    	    {
			    	    	// Inicializa el canvas
			    	    	var tooltip = document.createElement("canvas");
			    	    	var context = tooltip.getContext('2d');
			    	    	
			    	    	// Parámetros configurables
			    	    	var textLineHeight = 12;
			    	    	var textPadding = 5;
			    	    	var tooltipPadding = 15;
			    	    	var cornerRadius = 2;
			    	    	
			    	    	context.save();
			    	    	// Separa los saltos de línea del texto
			    	    	var text = annotation.note.split("\n");
			    	    	// Calcula en ancho necesario para el texto
			    	    	var textWidth = 0;
			    	    	for (var i = 0; i < text.length; i++)
			    	    	{
			    	    		var textArray = timestampRegex.exec(text[i]);
			    	    		if (textArray)
			    	    		{
			    	    			context.font = "bolder " + textLineHeight + "px monospace";
			    	    			var completeTextWidth = context.measureText(textArray[1]).width;
			    	    			context.font = textLineHeight + "px monospace";
			    	    			completeTextWidth += context.measureText(textArray[2]).width;
			    	    			textWidth = Math.max(textWidth, completeTextWidth);
			    	    		}
			    	    		else
			    	    		{
			    	    			context.font = textLineHeight + "px monospace";
			    	    			textWidth = Math.max(textWidth, context.measureText(text[i]).width);
			    	    		}
			    	    	}
			    	    	context.restore();
			    	    	// Calcula el alto necesario para el texto
			    	    	var textHeight = text.length * textLineHeight;
			    	    	
			    	    	var x0 = tooltipPadding;
			    	    	var y0 = tooltipPadding;
			    	    	var x1 = tooltipPadding + textWidth + 2 * textPadding + cornerRadius;
			    	    	var y1 = tooltipPadding + textHeight + 2 * textPadding + cornerRadius;
			    	    	
			    	    	var arrowX0 = (x0 + x1) / 2;
			    	    	var arrowX1 = arrowX0 - 5;
			    	    	var arrowX2 = arrowX0 + 5;
			    	    	var arrowY0 = y1;
			    	    	var arrowY1 = y1 + 5;
			    	    	
			    	    	var textX = tooltipPadding + textPadding + (cornerRadius / 2);
			    	    	var textY = tooltipPadding + textPadding + (cornerRadius / 2) + textLineHeight;
			    	    	textY -= 2; // Ajuste baseline
	
			    	    	var canvasWidth = x0 + x1;
			    	    	var canvasHeight = y0 + y1;
			    	    	
			    	    	var windowsWidth = document.body.clientWidth;
			    	    	var canvasLeft = ((annotation.position.x[0] + annotation.position.x[1]) / 2) - (canvasWidth / 2) + rect.left;
			    	    	if (canvasLeft < 0)
			    	    	{
		    	    			arrowX0 += canvasLeft;
		    	    			arrowX1 += canvasLeft;
		    	    			arrowX2 += canvasLeft;
			    	    		canvasLeft = 0;
			    	    	}
			    	    	else
			    	    	{
			    	    		var overflow = canvasLeft + canvasWidth - windowsWidth;
			    	    		if (overflow > 0)
			    	    		{
			    	    			arrowX0 += overflow;
			    	    			arrowX1 += overflow;
			    	    			arrowX2 += overflow;
			    	    			canvasLeft -= overflow;
			    	    		}
			    	    	}	
			    	    	var canvasTop = window.pageYOffset + rect.top + self.chart.chartArea.top - canvasHeight;
			    	    	if (canvasTop < window.pageYOffset)
			    	    		canvasTop = window.pageYOffset;
			    	    	
			    	    	// Ajusta el tamaño, posición y estilo del canvas
			    	    	context.canvas.width = canvasWidth;
			    	    	context.canvas.height = canvasHeight;
			    	    	jQryIter(tooltip).attr("id", "iterchart-tooltip");
			    	    	jQryIter(tooltip).css("position", "absolute");
			    	    	jQryIter(tooltip).css("left", canvasLeft);
			    	    	jQryIter(tooltip).css("top", canvasTop);
			    	    	jQryIter(tooltip).css("cursor", "help");
			    	    	jQryIter("body").append(tooltip);

			    	    	context.save();
			    	    	
			    	    	// Pinta el contenedor del tooltip
			    	    	context.strokeStyle="#737353";
			    	        context.fillStyle = "#FFFFC8";
			    	        context.lineJoin = "round";
			    	        context.lineWidth = cornerRadius;
			    	        context.beginPath();
			    	        context.moveTo(x0, y0);
			    	        context.lineTo(x0, y1);
			    	        context.lineTo(arrowX1, arrowY0);
			    	        context.lineTo(arrowX0, arrowY1);
			    	        context.lineTo(arrowX2, arrowY0);
			    	        context.lineTo(x1, y1);
			    	        context.lineTo(x1, y0);
			    	        context.closePath();
			    	        context.shadowColor = '#000';
			    	        context.shadowBlur = 10;
			    	        context.shadowOffsetX = 0;
			    	        context.shadowOffsetY = 0;
			    	        context.stroke();
			    	        
			    	        // Rellena el contenedor
			    	        ctx.shadowColor = "";
			    	        context.shadowBlur = 0;
			    	        var grd = context.createRadialGradient(canvasWidth * 0.5, 
										    	        		   canvasHeight * 0.25,
										    	        		   Math.max(canvasWidth, canvasHeight) * 0.1,
										    	        		   canvasWidth * 0.5,
										    	        		   canvasHeight * 0.25,
										    	        		   Math.max(canvasWidth, canvasHeight) * 0.5);
			    	        grd.addColorStop(0, '#FFFFC8');
			    	        grd.addColorStop(1, '#FFFF8C');
			    	        context.fillStyle = grd;
			    	        context.fill();
			    	        
			    	        context.restore();
			    	        
			    	        // Escribe el texto del tooltip
			    	        context.save();
			    	        context.fillStyle = 'rgba(60, 60, 60, 1)';
			    	        context.font = textLineHeight + "px Arial";
			    	        for (var i = 0; i < text.length; i++)
			    	        {
			    	        	var textArray = timestampRegex.exec(text[i]);
			    	        	if (textArray)
			    	    		{
			    	        		context.font = "bolder " + textLineHeight + "px monospace";
			    	    			var timestampWidth = context.measureText(textArray[1]).width;
			    	    			context.fillText(textArray[1], textX, textY + (i * textLineHeight));
			    	    			context.font = textLineHeight + "px monospace";
			    	    			context.fillText(textArray[2], textX + timestampWidth, textY + (i * textLineHeight));
			    	    		}
			    	        	else
			    	        	{
			    	        		context.font = textLineHeight + "px monospace";
			    	        		context.fillText(text[i], textX, textY + (i * textLineHeight));
			    	        	}
			    	        }
			    	        context.restore();
	
			    	        jQryIter(self.canvas).css("cursor", "help");
			    	        
				    	    break;
			    	    }
					}
		    	}
	    	}
	    }
	}
};

//Crea el gráfico y asigna los datos
ITER.statistics.TrendChart.prototype.createChart = function(data)
{	
	if (this.ctx != null)
	{
		var self = this;
		
		var chartOptions = {
			animation: false,
			maintainAspectRatio: false,
			scales: {
				xAxes: [{
					ticks: {
						beginAtZero: true,
						maxRotation: 0
					}
				}],
	            yAxes: [{
	                ticks: {
	                    beginAtZero:true,
	                    callback: function(value, index, values) {
	                    	if (Math.floor(value) === value) {
	                    		return iterChartFormatNumber(value, self.options.data.thousandSeparator);
	                    	}
	                    }
	                }
	            }]
	        },
	        tooltips: {
	        	mode: "x-axis",
	        	callbacks: {
	        		label: function(tooltipItem, data) {
	            		var item = data.datasets[tooltipItem.datasetIndex];
	            		return item.label + ": " + iterChartFormatNumber(item.data[tooltipItem.index], self.options.data.thousandSeparator);
	        		}
	        	}
	        },
	        iterOptions: {
	        	annotation: {
		        	icon: "/tracking-portlet/img/statistics/annotation.png",
		        	scaleId: 'x-axis-0',
		            annotations: data.annotations,
		            sysannotations: data.sysannotations
	        	}
	        }
		};

		var hours = new Array();
		for ( var i=0; i< data.hours.length; i++)
		{
			hours.push(data.hours[i]);
		}

		var datasets = new Array();
		
		if (data.visits != undefined)
		{
			var aux = {
				label: this.options.tooltips[0],
				backgroundColor: "rgba(151,187,205,0.2)",
				borderColor: "rgba(151,187,205,1)",
				pointBackgroundColor: "rgba(151,187,205,1)",
				pointBorderColor: "#fff",
				lineTension: 0,
				data: data.visits
			};
			datasets.push(aux);
		}
		if (data.reads != undefined)
		{
			var aux = {
				label: this.options.tooltips[1],
				backgroundColor: "rgba(46,101,179,0.5)",
				borderColor: "rgba(46,101,179,1)",
				pointBackgroundColor: "rgba(46,101,179,1)",
				pointBorderColor: "#fff",
				lineTension: 0,
				data: data.reads
			};
			datasets.push(aux);
		}
		if (data.totals != undefined)
		{
			var aux = {
				label: this.options.tooltips[2],
				backgroundColor: "rgba(0,0,0,0)",
				borderColor: "rgba(129,94,219,1)",
				pointBackgroundColor: "rgba(129,94,219,1)",
				pointBorderColor: "#fff",
				lineTension: 0,
				data: data.totals
			};
			datasets.push(aux);
		}

		if (typeof data.abtesting !== 'undefined')
		{
			if (typeof data.abtesting.variants !== 'undefined' && data.abtesting.variants.length > 0)
			{	
				// Variable para guardar los datos iniciales en caso de que el render de las variantes termine más tarde.
				demoTestAB = {};
				
				var color = ["#e44f45", "#55f474", "#FF9F0F", "##FF05FF", "#14FFFB"];
				for (var i=0; i<data.abtesting.variants.length; i++) {
					var aux = {
						label: data.abtesting.variants[i].variantname,
						fill: false,
						borderColor: color[i % 5],
						pointBackgroundColor: color[i % 5],
						pointBorderColor: "#fff",
						lineTension: 0,
						data: sumArrays(data.abtesting.variants[i].visits, data.abtesting.variants[i].extvisits)
					};
					datasets.push(aux);
					
					// Actualiza los datos del render
					var ctr = data.abtesting.variants[i].ctr  + "%";
					var prints = data.abtesting.variants[i].totalprints;
					var visits = data.abtesting.variants[i].totalvisits;
					jQryIter(".variant-" + data.abtesting.variants[i].variantname + " .ctr").text(ctr);
					jQryIter(".variant-" + data.abtesting.variants[i].variantname + " .prints").text(prints);
					jQryIter(".variant-" + data.abtesting.variants[i].variantname + " .views").text(visits);
					
					// Guarda los valores en la variable para el render de las variantes.
					demoTestAB[data.abtesting.variants[i].variantname] = {ctr: ctr, prints: prints, visits: visits};
				}
			}
		}
		
		var chartData = {
				labels: hours,
				datasets: datasets
		};

		// Si ya existe un gráfico, lo elimina
		if (this.chart != null) {
			this.chart.destroy();
		}
		
		// Crea el gráfico
		return new Chart(this.ctx, {type: 'line', data: chartData, options: chartOptions });
	}
};
// Actualiza los datos del gráfico
ITER.statistics.TrendChart.prototype.updateChart = function(data)
{
	if (typeof data.totals !== "undefined" && typeof data.visits === "undefined" && typeof data.reads === "undefined")
	{
		// Metadato
		this.chart.data.datasets[0].data = data.totals;
	}
	else if (typeof data.totals !== "undefined" && typeof data.visits !== "undefined" && typeof data.reads !== "undefined")
	{
		// Sección / Portada
		this.chart.data.datasets[0].data = data.visits;
		this.chart.data.datasets[1].data = data.reads;
		this.chart.data.datasets[2].data = data.totals;
	}
	else if (typeof data.totals === "undefined" && typeof data.visits !== "undefined" && typeof data.reads !== "undefined")
	{
		// Artículo
		this.chart.data.datasets[0].data = data.visits;
		this.chart.data.datasets[1].data = data.reads;
		
		// Update de experimentos
		if (typeof data.abtesting !== 'undefined') {
			if (typeof data.abtesting.variants !== 'undefined' && data.abtesting.variants.length > 0) {
				
				for (var i = 0; i < data.abtesting.variants.length; i++) {
					// Actualiza los datos del gráfico
					this.chart.data.datasets[i + 2].data = data.abtesting.variants[i].visits;

					// Actualiza los datos del render
					jQryIter(".variant-" + data.abtesting.variants[i].variantname + " .ctr").text(data.abtesting.variants[i].ctr + "%");
					jQryIter(".variant-" + data.abtesting.variants[i].variantname + " .prints").text(data.abtesting.variants[i].totalprints);
					jQryIter(".variant-" + data.abtesting.variants[i].variantname + " .views").text(data.abtesting.variants[i].totalvisits);
				}
			}
		}
	}
	
	// Actualiza las etiquetas
	this.chart.data.labels = data.hours;

	// Actualiza las anotaciones
	this.chart.options.iterOptions.annotation.annotations = data.annotations;
	this.chart.options.iterOptions.annotation.sysannotations = data.sysannotations;
	
	// Actualiza el gráfico
	this.chart.update();
};
// Manejador de eventos
ITER.statistics.TrendChart.prototype.attachStatisticsDetailEvent = function(stats)
{
	if (this.chart)
	{
		var self = this;
		// Captura el evento onclick del canvas que contiene el gráfico.
		this.chart.chart.canvas.onclick = function(e)
		{
			// Evita el comportamiento por defecto del evento
			e.preventDefault();
			
			var iterOptions = self.chart.options.iterOptions;
	    	if (iterOptions && iterOptions.annotation && iterOptions.annotation.annotations)
	    	{
				var event = jQryIter("#MCMEvent\\:statisticAnnotation");
				event.attr("groupId", "");
				event.attr("startDate", "");
				event.attr("endDate", "");
				event.attr("idart", "");
	    		
	    		var rect = self.canvas.getBoundingClientRect();
	    		var mouseX = parseInt(e.clientX - rect.left);
	    	    var mouseY = parseInt(e.clientY - rect.top);
	    	    
	    	    var annotations = iterOptions.annotation.annotations;
    	    	for (var i = 0; i < annotations.length; i++)
				{
    	    		var annotation = annotations[i];
		    	    if (mouseX > annotation.position.x[0] &&
		    	    	mouseX < annotation.position.x[1] &&
		    	    	mouseY > annotation.position.y[0] &&
		    	    	mouseY < annotation.position.y[1])
		    	    {
		    	    	// Si hay alguna anotación, deja los datos para consultarlas en el campo MCMEvent:statisticAnnotation
						var event = jQryIter("#MCMEvent\\:statisticAnnotation");
						event.attr("groupId", annotation.info.groupId);
						event.attr("startDate", annotation.info.startDate);
						event.attr("endDate", annotation.info.endDate);
						event.attr("idart", annotation.info.idart);
						event.attr("mode", annotation.info.mode);

						// Notifica a Milenium
						event.trigger( "click" );
						
						annotationFound = true;
						break;
		    	    }
				}
    	    }
		};
	}
};

///////////////////////////////////////////////////////////////
//             GRÁFICO DE RANKING DE ELEMENTOS               //
///////////////////////////////////////////////////////////////
ITER.statistics.RankingChart = function(customOptions, parentElementId)
{
	ITER.statistics.Chart.call(this, customOptions, parentElementId);
};
ITER.statistics.RankingChart.prototype = Object.create(ITER.statistics.Chart.prototype);

//Construye la URL de solicitud de estadísticas
ITER.statistics.RankingChart.prototype.buildRequestURL = function(request)
{
	var url = request.baseUrl + this.options.data.pageType + "?chartType=ranking&groupId=" + request.group + "&item=" +
	          this.options.data.itemType + "&criteria=" + this.options.data.criteria + "&resolution=" + request.resolution;
	
	switch(this.options.data.pageType) {
		case "group":
			if ("metadata" === this.options.data.itemType)
			url += "&vocabularyId=" + this.options.data.itemId;
			break;
		
		case "section":
			if ("article" === this.options.data.itemType && this.options.data.itemId)
			url += "&plid=" + this.options.data.itemId;
			break;
		
		case "metadata":
			if ("article" === this.options.data.itemType)
			url += "&categoryId=" + this.options.data.itemId;
			break;
	}
	
	if (request.dateLimit)               url += "&dateLimit="      + request.dateLimit;
	if ("minute" === request.resolution) {
		url += "&realTime=" + request.realTime;
		if (request.displayedHours != 24) {
			url += "&displayedHours=" + request.displayedHours;
		}
	}
	if (this.options.data.maxItems)      url += "&maxItems="       + this.options.data.maxItems;
	
	return url;
};

// Actualiza el área de pintado
ITER.statistics.RankingChart.prototype.initializeCanvas = function(data)
{
	var newDatasetLength = data.data.id.length;
	
	// Si no hay datos...
	if (newDatasetLength == 0)
	{
		// Si contiene un gráfico, elimina y limpia el contenido
		if (this.chart != null)
		{
			// Destruye el gráfico
			this.chart.destroy();
			this.chart = null;
			
			// Elimina el canvas
			this.ctx = null;
			jQryIter(this.canvas).remove();
			this.canvas = null;
		}
		
		// Si no existe, añade el mensaje informativo que indica que no hay visitas
		if (jQryIter(this.container).find(".warning").length == 0)
		{
			var messageContainer = jQryIter(document.createElement("div"));
			if (this.options.style.messageContainerClass)
				messageContainer.addClass(this.options.style.messageContainerClass);
			jQryIter(messageContainer).append('<span>' + this.options.data.emptyVisitsMessage + '</span>');
			jQryIter(this.container).append(messageContainer);
		}
	}
	// Si hay datos...
	else
	{
		// Elimina el mensaje informativo que indica que no hay visitas
		jQryIter(this.container).find(".warning").remove();
		
		// Crea el canvas si no existe
		if (this.canvas == null)
		{
			// Añade un canvas
			this.canvas = document.createElement("canvas");
			jQryIter(this.container).append(this.canvas);
			
			// Inicializa el contexto
			this.ctx = this.canvas.getContext("2d");
		}

		// Calcula la altura del canvas
		var height = ( (newDatasetLength * 25) + 25 ) + "px";
		jQryIter(this.canvas).css("max-height", height);
		jQryIter(this.canvas).css("min-height", height);
	}
};

//Crea el gráfico y asigna los datos
ITER.statistics.RankingChart.prototype.createChart = function(data)
{
	if (this.ctx != null)
	{	
		var self = this;
		
		var rankingData = {
			labels: data.label,
			datasets: [
				{
					label: this.options.tooltips[0],
					backgroundColor: "rgba(151,187,205,0.8)",
					hoverBackgroundColor: "rgba(169,194,207,0.5)",
					data: data.dataset1
				}
			]
		};
		
		if (typeof data.dataset2 != 'undefined')
		{
			rankingData.datasets.unshift(
				{
					label: this.options.tooltips[1],
					backgroundColor: "rgba(46,101,179,0.9)",
					hoverBackgroundColor: "rgba(100,133,181,0.5)",
					data: data.dataset2
				}
			);
		}
		
		var rankingOptions = {
			legend: {
				display: false
			},
			animation: false,
			maintainAspectRatio: false,
			scales: {
				xAxes: [{
					stacked: this.options.data.itemType === "section" ? true : false,
					ticks: {
						beginAtZero: true,
						maxRotation: 0,
	                    callback: function(value, index, values) {
	                    	if (Math.floor(value) === value) {
	                    		return iterChartFormatNumber(value, self.options.data.thousandSeparator);
	                    	}
	                    }
	                }
				}],
				yAxes: [{
					stacked: true,
					ticks: {
						callback: function(value, index, values) {
							var maxLabelWidth = 150;
							var str = value;
							var strWidth = this.ctx.measureText(str).width;
							
							if (strWidth > maxLabelWidth)
							{
								var len = str.length;
								while (strWidth >= maxLabelWidth && len-->0) {
									str = str.substring(0, len) + "...";
									strWidth = this.ctx.measureText(str).width;
								}
							}
							else if (strWidth < maxLabelWidth)
							{
								while (strWidth < maxLabelWidth) {
									str = " " + str;
									strWidth = this.ctx.measureText(str).width;
								}
								str = str.substr(1);
							}
							return str;
						}
					}
				}]
			},
			tooltips: {
		        callbacks: {
		            title: function(tooltipItem){
		                return this._data.labels[tooltipItem[0].index];
		            },
		    		label: function(tooltipItem, data) {
		        		return data.datasets[tooltipItem.datasetIndex].label + ": " + iterChartFormatNumber(data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index], self.options.data.thousandSeparator);
		    		}
		        }
		    }
		};
		
		return new Chart(this.ctx, { type: 'horizontalBar', data: rankingData, options: rankingOptions });
	}
};

ITER.statistics.RankingChart.prototype.updateChart = function(data)
{
	// Actualiza los datos
	this.chart.data.labels = data.label;
	if (data.dataset2 != undefined)
	{
		this.chart.data.datasets[0].data = data.dataset2;
		this.chart.data.datasets[1].data = data.dataset1;
	}
	else
		this.chart.data.datasets[0].data = data.dataset1;

	
	// Actualiza el tamaño por si hubiera cambiado
	this.chart.resize();
	
	// Repinta el gráfico
	this.chart.update();
};

//Manejador de eventos
ITER.statistics.RankingChart.prototype.attachStatisticsDetailEvent = function(stats)
{
	if (this.chart)
	{
		var self = this;
		// Captura el evento onclick del canvas que contiene el gráfico.
		this.chart.chart.canvas.onclick = function(evt)
		{
			// Evita el comportamiento por defecto del evento
			evt.preventDefault();
			// Recupera el elemento sobre el que se hizo clic
			var clickedBar = self.chart.getElementAtEvent(evt);
			if (clickedBar != null && clickedBar.length > 0)
			{
				// Recupera el ID del elemento
				var index = clickedBar[0]._index;
				var idcms = stats.id[index];
				// Deja los datos en MCMEvent:statisticDetail
				var event = jQryIter("#MCMEvent\\:statisticDetail");
				event.attr("statistictype", self.options.data.itemType);
				event.attr("idcms", idcms);
				event.attr("titleart", stats.label[index]);
				event.trigger( "click" );
		    	// iFrame
		    	var pass_data = {
			    	"operation": "detail",
			    	"data": {
				    	"statistictype": self.options.data.itemType,
				    	"groupId": data.groupId,
			    		"idcms": idcms,
					    "titleart": stats.label[index]
			    	}
				};
				parent.postMessage(JSON.stringify(pass_data), '*');
				// Si es debug, pide las estadísticas del elemento
				if (self.options.debug)
				{
					var config = jQryIter("#MCMEvent\\:statisticConfig");
					if (self.options.data.itemType == "section")
						window.location.href = "/tracking-portlet/milenium/tracking/statistics?groupId=" + data.groupId + "&plid=" + idcms + "&debug=true" + "&resolution=" + config.attr("resolution") + "&date=" + config.attr("date") + "&displayedHours=" + config.attr("displayedHours") + "&realTime=" + config.attr("realTime");
					else if (self.options.data.itemType == "article")
						window.location.href = "/tracking-portlet/milenium/tracking/statistics?groupId=" + data.groupId + "&contentId=" + idcms + "&debug=true" + "&resolution=" + config.attr("resolution") + "&date=" + config.attr("date") + "&displayedHours=" + config.attr("displayedHours") + "&realTime=" + config.attr("realTime");
					else if (self.options.data.itemType == "metadata")
						window.location.href = "/tracking-portlet/milenium/tracking/statistics?groupId=" + data.groupId + "&categoryId=" + idcms + "&debug=true" + "&resolution=" + config.attr("resolution") + "&date=" + config.attr("date") + "&displayedHours=" + config.attr("displayedHours") + "&realTime=" + config.attr("realTime");
				}
			}
		};
	}
};




///////////////////////////////////////////////////////////////
//           GRÁFICO DE TENDENCIAS DE VOTACIONES             //
///////////////////////////////////////////////////////////////
ITER.statistics.SurveyTrendChart = function(customOptions, parentElementId)
{
	ITER.statistics.Chart.call(this, customOptions, parentElementId);
};
ITER.statistics.SurveyTrendChart.prototype = Object.create(ITER.statistics.Chart.prototype);

//Construye la URL de solicitud de estadísticas
ITER.statistics.SurveyTrendChart.prototype.buildRequestURL = function(request)
{
	var url = request.baseUrl + "survey?chartType=trend&surveyId=" + this.options.data.surveyId + "&resolution=" + request.resolution;
	
	if (request.dateLimit)               url += "&dateLimit="      + request.dateLimit;
	if ("minute" === request.resolution) {
		url += "&realTime=" + request.realTime;
		if (request.displayedHours != 24) {
			url += "&displayedHours=" + request.displayedHours;
		}
	}
	
	return url;
};

ITER.statistics.SurveyTrendChart.prototype.initializeCanvas = function(data)
{
	//Crea el canvas si no existe
	if (this.canvas == null)
	{
		// Añade un canvas
		this.canvas = document.createElement("canvas");
		jQryIter(this.container).append(this.canvas);
		
		// Inicializa el contexto
		this.ctx = this.canvas.getContext("2d");
	}
};

//Crea el gráfico y asigna los datos
ITER.statistics.SurveyTrendChart.prototype.createChart = function(data)
{	
	if (this.ctx != null)
	{
		var self = this;
		
		var chartOptions = {
			legend: {
				display: false
			},
			animation: false,
			maintainAspectRatio: false,
			scales: {
				xAxes: [{
					ticks: {
						beginAtZero: true,
						maxRotation: 0
					}
				}],
	            yAxes: [{
	                ticks: {
	                    beginAtZero:true,
	                    callback: function(value, index, values) {
	                    	if (Math.floor(value) === value) {
	                    		return iterChartFormatNumber(value, self.options.data.thousandSeparator);
	                    	}
	                    }
	                }
	            }]
	        },
	        tooltips: {
	        	mode: "x-axis",
	        	callbacks: {
	        		label: function(tooltipItem, data) {
	            		var item = data.datasets[tooltipItem.datasetIndex];
	            		return item.label + ": " + iterChartFormatNumber(item.data[tooltipItem.index], self.options.data.thousandSeparator);
	        		}
	        	}
	        }
		};
		
		var chartData = {
				labels: data.hours,
				datasets: [{
					label: '',
					backgroundColor: "rgba(151,187,205,0.2)",
					borderColor: "rgba(151,187,205,1)",
					pointBackgroundColor: "rgba(151,187,205,1)",
					pointBorderColor: "#fff",
					lineTension: 0,
					data: data.visits
				}]
		};

		// Si ya existe un gráfico, lo elimina
		if (this.chart != null) {
			this.chart.destroy();
		}
		
		// Crea el gráfico
		return new Chart(this.ctx, {type: 'line', data: chartData, options: chartOptions });
	}
};

ITER.statistics.SurveyTrendChart.prototype.updateChart = function(data)
{
	// Actualiza los datos
	this.chart.data.datasets[0].data = data.visits;
	
	// Actualiza las etiquetas
	this.chart.data.labels = data.hours;
	
	// Actualiza el gráfico
	this.chart.update();
};
