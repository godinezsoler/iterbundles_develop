function VisitsRankingChart(customOptions, parentElementId)
{
	// Gr�fico
	var chart = null;
	// Div contenedor del gr�fico
	var container = null;
	// Canvas en el que se pinta el gr�fico
	var canvas = null;
	// Contexto del canvas en el que se pinta el gr�fico
	var ctx = null;
	
	// N�mero de elementos
	var datasetLength = 0;
	
	// Opciones del gr�fico
	var options = {
		id: "",
		title: "",
		tooltips: [],
		data: {
			type: "",
			thousandSeparator: "",
			emptyVisitsMessage: ""
		},
		style: {
			mainContainerClass: "",
			titleContainerClass: "",
			chartContainerClass: "",
			messageContainerClass: ""
		},
		debug: false
	};
	
	// Inicializaci�n.
	jQryIter.extend(options, customOptions);
	createContext();
	
	/**
	 * Establece los datos del gr�fico y lo crea o actualiza si ya existe.
	 */ 
	this.setStatistics = function(statistics)
	{
		// Crea o actualiza el �rea de pintado
		initializeCanvas(statistics.id.length);
		
		if (datasetLength > 0)
		{
			// Elimina el mensaje informativo que indica que no hay visitas
			jQryIter(container).find(".warning").remove();
			
			// Crea / Actualiza el gr�fico
			if (chart == null)
				chart = createChart(statistics);
			else
				updateChart(statistics);

			// A�ade los eventos de click
			attachStatisticsDetailEvent(statistics);
		}
	};
	
	/**
	 * Crea el contenedor del gr�fico, estableciendo su t�tulo y eventos.
	 */ 
	function createContext()
	{
		// Busca el elemento padre
		var parentElement = null;
		if (parentElementId != undefined)
			parentElement = jQryIter("#" + parentElementId);

		// Si no lo encuentra o no se ha informado, usa el body
		if (parentElement == undefined)
			parentElement = jQryIter(document.body);
		
		// Crea el contenedor del gr�fico
		container = document.createElement("div");
		if (options.style.mainContainerClass)
			jQryIter(container).addClass(options.style.mainContainerClass);
		parentElement.append(container);
		
		// Crea el t�tulo del gr�fico
		var titleContainer = jQryIter(document.createElement("div"));
		if (options.style.titleContainerClass)
			titleContainer.addClass(options.style.titleContainerClass);
		titleContainer.append("<span>"+options.title+"</span>");
		titleContainer.click( function() {
			jQryIter(this).next().toggle();
			if (chart) chart.resize(); // Actualiza el tama�o del gr�fico
		});
		jQryIter(container).append(titleContainer);
	}
	
	/**
	 * Inicializa el �rea de pintado del gr�fico.
	 * 
	 * Si hay datos, crea si es necesario un canvas y calcula el alto en funci�n del
	 * n�mero de elementos a mostrar.
	 * 
	 * Si no hay datos, elimina el gr�fico y el canvas si existen, y pone el mensaje informativo
	 * si no se estaba mostrando.
	 */
	function initializeCanvas(newDatasetLength)
	{
		// Si no hay datos...
		if (newDatasetLength == 0)
		{
			// Si contiene un gr�fico, elimina y limpia el contenido
			if (chart != null)
			{
				// Destruye el gr�fico
				chart.destroy();
				chart = null;
				
				// Elimina el canvas
				ctx = null;
				jQryIter(canvas).remove();
				canvas = null;
			}
			
			// Si no existe, a�ade el mensaje informativo que indica que no hay visitas
			if (jQryIter(container).find(".warning").length == 0)
			{
				var messageContainer = jQryIter(document.createElement("div"));
				if (options.style.messageContainerClass)
					messageContainer.addClass(options.style.messageContainerClass);
				jQryIter(messageContainer).append('<span>' + options.data.emptyVisitsMessage + '</span>');
				jQryIter(container).append(messageContainer);
			}
		}
		// Si hay datos...
		else
		{
			// Crea el canvas si no existe
			if (canvas == null)
			{
				// A�ade un canvas
				canvas = document.createElement("canvas");
				jQryIter(container).append(canvas);
				
				// Inicializa el contexto
				ctx = canvas.getContext("2d");
			}

			// Si ha cambiado el n�mero de elementos, hay que modificar el tama�o del canvas	
			if (datasetLength != newDatasetLength)
			{
				// Calcula la altura del canvas
				var height = ( (newDatasetLength * 25) + 25 ) + "px";
				jQryIter(canvas).css("max-height", height);
				jQryIter(canvas).css("min-height", height);
			}
		}
		
		// Actualiza el n�mero de elementos actuales
		datasetLength = newDatasetLength;
	}
	
	/**
	 * Crea un nuevo gr�fico en el contexto seleccionado.
	 * 
	 * @param statistics JSON con las estad�sticas a mostrar en el gr�fico con formato: { id: [...], name: [...], visits: [...], reads: [...] }
	 */
	function createChart(statistics)
	{		
		if (ctx != null)
		{	
			var rankingData = {
				labels: statistics.name,
				datasets: [
					{
						label: options.tooltips[0],
						backgroundColor: "rgba(151,187,205,0.8)",
						hoverBackgroundColor: "rgba(169,194,207,0.5)",
						data: statistics.visits
					}
				]
			};
			
			if (typeof statistics.reads != 'undefined')
			{
				rankingData.datasets.unshift(
					{
						label: options.tooltips[1],
						backgroundColor: "rgba(46,101,179,0.9)",
						hoverBackgroundColor: "rgba(100,133,181,0.5)",
						data: statistics.reads
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
						ticks: {
							beginAtZero: true,
							maxRotation: 0,
		                    callback: function(value, index, values) {
		                    	if (Math.floor(value) === value) {
		                    		return iterChartFormatNumber(value, options.data.thousandSeparator);
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
								var strWidth = this.ITER_Context.measureText(str).width;
								
								if (strWidth > maxLabelWidth)
								{
									var len = str.length;
									while (strWidth >= maxLabelWidth && len-->0) {
										str = str.substring(0, len) + "...";
										strWidth = this.ITER_Context.measureText(str).width;
									}
								}
								else if (strWidth < maxLabelWidth)
								{
									while (strWidth < maxLabelWidth) {
										str = " " + str;
										strWidth = this.ITER_Context.measureText(str).width;
									}
									str = str.substr(1);
								}
								return str;
							},
							ITER_Context: ctx
						}
					}]
				},
				tooltips: {
			        callbacks: {
			            title: function(tooltipItem){
			                return this._data.labels[tooltipItem[0].index];
			            }
			        }
			    }
			};
			
			return new Chart(ctx, { type: 'horizontalBar', data: rankingData, options: rankingOptions });
		}
	}
	
	/**
	 * Actualiza los datos del gr�fico y realiza un repintado.
	 * 
	 * @param statistics JSON con las estad�sticas a mostrar en el gr�fico con formato: { id: [...], name: [...], visits: [...], reads: [...] }
	 */
	function updateChart(statistics)
	{
		// Actualiza los datos
		chart.data.labels = statistics.name;
		if (statistics.reads != undefined)
		{
			chart.data.datasets[0].data = statistics.reads;
			chart.data.datasets[1].data = statistics.visits;
		}
		else
			chart.data.datasets[0].data = statistics.visits;

		
		// Actualiza el tama�o por si hubiera cambiado
		chart.resize();
		
		// Repinta el gr�fico
		chart.update();
	}
	

	
	///////////////////////////////////////////////////////////////////////
	//                       MANEJADOR DE EVENTOS                        //
	///////////////////////////////////////////////////////////////////////
	function attachStatisticsDetailEvent(stats)
	{
		// Captura el evento onclick del canvas que contiene el gr�fico.
		chart.chart.canvas.onclick = function(evt)
		{
			// Evita el comportamiento por defecto del evento
			evt.preventDefault();
			// Recupera el elemento sobre el que se hizo clic
			var clickedBar = chart.getElementAtEvent(evt);
			if (clickedBar != null && clickedBar.length > 0)
			{
				// Recupera el ID del elemento
				var index = clickedBar[0]._index;
				var idcms = stats.id[index];
				// Deja los datos en MCMEvent:statisticDetail
				var event = jQryIter("#MCMEvent\\:statisticDetail");
				event.attr("statistictype", options.data.type);
				event.attr("idcms", idcms);
				event.attr("titleart", "article" == options.data.type ? stats.name[index] : "");
				event.trigger( "click" );
				// Si es debug, pide las estad�sticas del elemento
				if (options.debug)
				{
					var config = jQryIter("#MCMEvent\\:statisticConfig");
					if (options.data.type == "section")
						window.location.href = "/tracking-portlet/milenium/tracking/statistics?groupId=" + data.groupId + "&plid=" + idcms + "&debug=true" + "&resolution=" + config.attr("resolution") + "&date=" + config.attr("date") + "&displayedHours=" + config.attr("displayedHours");
					else if (options.data.type == "article")
						window.location.href = "/tracking-portlet/milenium/tracking/statistics?groupId=" + data.groupId + "&contentId=" + idcms + "&debug=true" + "&resolution=" + config.attr("resolution") + "&date=" + config.attr("date") + "&displayedHours=" + config.attr("displayedHours");
					else if (options.data.type == "metadata")
						window.location.href = "/tracking-portlet/milenium/tracking/statistics?groupId=" + data.groupId + "&categoryId=" + idcms + "&debug=true" + "&resolution=" + config.attr("resolution") + "&date=" + config.attr("date") + "&displayedHours=" + config.attr("displayedHours");
				}
			}
		};
	}
}