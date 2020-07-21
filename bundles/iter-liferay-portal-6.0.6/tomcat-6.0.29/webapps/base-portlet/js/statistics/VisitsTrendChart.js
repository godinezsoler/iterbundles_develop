function VisitsTrendChart(thousandSeparator, parentElementId)
{
	// Gr�fico
	var chart = null;
	// Div contenedor del gr�fico
	var container = null;
	// Canvas en el que se pinta el gr�fico
	var canvas = null;
	// Contexto del canvas en el que se pinta el gr�fico
	var ctx = null;
	
	// Inicializaci�n.
	createContext();
	
	/**
	 * Establece los datos del gr�fico y lo crea o actualiza si ya existe.
	 */ 
	this.setStatistics = function(statistics, refresh)
	{
		var displacement = false;
		
		if (chart == null || refresh)
			createChart(statistics);
		else
			displacement = updateChart(statistics);
		
		// A�ade los eventos de click
		attachStatisticsDetailEvent(statistics);
		
		return displacement;
	};
	
	/**
	 * Crea el contenedor y el canvas e inicializa el contexto.
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
		container.classList = "visits-chart-container";
		jQryIter(container).addClass("visits-chart-container");
		container.style = "min-height: 250px; max-height: 250px;";
		parentElement.append(container);
		
		// Crea el canvas para pintar el gr�fico
		canvas = document.createElement("canvas");
		jQryIter(container).append(canvas);
		
		// Inicializa el contexto
		ctx = canvas.getContext("2d");
	}
	
	/**
	 * Crea un nuevo gr�fico en el contexto seleccionado.
	 */
	function createChart(statistics)
	{		
		if (ctx != null)
		{
			var options = {
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
		                    		return iterChartFormatNumber(value, thousandSeparator);
		                    	}
		                    }
		                }
		            }]
		        },
		        tooltips: {
		        	mode: "x-axis"
		        },
		        annotation: {
		        	icon: "/tracking-portlet/img/statistics/annotation.png",
		        	scaleId: 'x-axis-0',
		            annotations: statistics.annotations
		        }
			};
	
			var hours = new Array();
			for ( var i=0; i< statistics.hours.length; i++)
			{
				hours.push(statistics.hours[i]);
			}
	
			var datasets = new Array();
			if (statistics.visits != undefined)
			{
				var aux = {
					label: this.options.tooltipVisits,
					backgroundColor: "rgba(151,187,205,0.2)",
					borderColor: "rgba(151,187,205,1)",
					pointBackgroundColor: "rgba(151,187,205,1)",
					pointBorderColor: "#fff",
					lineTension: 0,
					data: statistics.visits
				};
				datasets.push(aux);
			}
			if (statistics.reads != undefined)
			{
				var aux = {
					label: this.options.tooltipReads,
					backgroundColor: "rgba(46,101,179,0.9)",
					borderColor: "rgba(46,101,179,1)",
					pointBackgroundColor: "rgba(46,101,179,1)",
					pointBorderColor: "#fff",
					lineTension: 0,
					data: statistics.reads
				};
				datasets.push(aux);
			}
			if (statistics.totals != undefined)
			{
				var aux = {
					label: this.options.tooltipTotalVisits,
					backgroundColor: "rgba(0,0,0,0)",
					borderColor: "rgba(129,94,219,1)",
					pointBackgroundColor: "rgba(129,94,219,1)",
					pointBorderColor: "#fff",
					lineTension: 0,
					data: statistics.totals
				};
				datasets.push(aux);
			}
			
			var data = {
					labels: hours,
					datasets: datasets
			};
	
			// Si ya existe un gr�fico, lo elimina
			if (chart != null) {
				chart.destroy();
			}
			
			// Crea el gr�fico
			chart = new Chart(ctx, {type: 'line', data: data, options: options });
		}
	}
	
	/**
	 * Actualiza los datos del gr�fico.
	 * 
	 * Si ha cambiado el �ltimo elemento (Ha cambiado la hora), elimina el dato de
	 * la izquierda y a�ade el nuevo dato por la derecha.
	 * 
	 * @param statistics JSON con las estad�sticas a mostrar en el gr�fico.
	 * @returns {Boolean} Indica si ha cambiado el �ltimo elemento.
	 */
	function updateChart(statistics)
	{	
		// Actualiza los datos
		chart.data.datasets[0].data = statistics.visits;
		if (typeof statistics.reads != "undefined")
			chart.data.datasets[1].data = statistics.reads;
		if (typeof statistics.totals != "undefined")
			chart.data.datasets[2].data = statistics.totals;
		
		// Actualiza las etiquetas
		chart.data.labels = statistics.hours;
		
		// Actualiza el gr�fico
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
			var clickedElement = chart.getElementsAtXAxis(evt);
			if (clickedElement != null && clickedElement.length > 0)
			{
				// Recupera la fecha sobre la que se hizo click
				var index = clickedElement[0]._index;
				var date = stats.hours[index];
				
				// Busca si hay anotaciones para esa fecha
				var annotationFound = false;
				if (stats.annotations)
				{
					for (var i=0; i<stats.annotations.length; i++)
					{
						var annotation = stats.annotations[i];
						if (annotation.label === date)
						{
							// Si hay alguna anotaci�n, deja los datos para consultarlas en el campo MCMEvent:statisticAnnotation
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
				
				if (!annotationFound)
				{
					var event = jQryIter("#MCMEvent\\:statisticAnnotation");
					event.attr("groupId", "");
					event.attr("startDate", "");
					event.attr("endDate", "");
					event.attr("idart", "");
				}
			}
		};
	}
}