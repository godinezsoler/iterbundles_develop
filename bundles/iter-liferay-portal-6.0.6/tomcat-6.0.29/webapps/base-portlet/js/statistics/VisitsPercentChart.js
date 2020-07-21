function VisitsPercentChart(customOptions, parentElementId)
{
	// Gráfico
	var chart = null;
	// Div contenedor del gráfico
	var container = null;
	// Canvas en el que se pinta el gráfico
	var canvas = null;
	// Contexto del canvas en el que se pinta el gráfico
	var ctx = null;
	
	// Opciones del gráfico
	var options = {
		id: "",
		title: "",
		tooltips: [],
		data: {
			type: "",
			thousandSeparator: ""
		},
		style: {
			mainContainerClass: "",
			titleContainerClass: "",
			chartContainerClass: ""
		},
		debug: false
	};
	
	// Inicialización.
	jQryIter.extend(options, customOptions);
	createContext();
	
	/**
	 * Establece los datos del gráfico y lo crea o actualiza si ya existe.
	 */ 
	this.setStatistics = function(statistics)
	{
		// Crea / Actualiza el gráfico
		if (chart == null)
			chart = createChart(statistics);
		else
			updateChart(statistics);
	};
	
	/**
	 * Crea el contenedor del gráfico, estableciendo su título y eventos.
	 */ 
	function createContext()
	{
		// Busca el elemento padre
		var parentElement = null;
		if (parentElementId != undefined)
		{
			if (typeof parentElementId === 'string')
				parentElement = jQryIter("#" + parentElementId);
			else if (typeof parentElementId === 'object' && parentElementId instanceof jQryIter)
				parentElement = parentElementId;
		}

		// Si no lo encuentra o no se ha informado, usa el body
		if (parentElement == undefined)
			parentElement = jQryIter(document.body);
		
		// Crea el contenedor del gráfico
		container = document.createElement("div");
		if (options.id)
			container.id = options.id;
		if (options.style.mainContainerClass)
			jQryIter(container).addClass(options.style.mainContainerClass);
		parentElement.append(container);
		
		// Crea el título del gráfico
		var titleContainer = jQryIter(document.createElement("div"));
		if (options.style.titleContainerClass)
			titleContainer.addClass(options.style.titleContainerClass);
		titleContainer.append("<span>"+options.title+"</span>");
		jQryIter(container).append(titleContainer);
		
		// Añade un canvas
		canvas = document.createElement("canvas");
		if (options.style.chartContainerClass)
			jQryIter(canvas).addClass(options.style.chartContainerClass);
		canvas.height="300";
		canvas.width="500";
		jQryIter(container).append(canvas);
		
		// Inicializa el contexto
		ctx = canvas.getContext("2d");
	}
	
	/**
	 * Crea un nuevo gráfico en el contexto seleccionado.
	 * 
	 * @param statistics JSON con las estadísticas a mostrar en el gráfico con formato: { id: [...], name: [...], visits: [...], reads: [...] }
	 */
	function createChart(statistics)
	{		
		if (ctx != null)
		{
			var chartOptions = {
				maintainAspectRatio: false,
				cutoutPercentage : 35,
				elements: {
					centerPercentText: 1
				}
			};
			
			var chartData = {
				labels: [
				    options.tooltips[0],
				    options.tooltips[1]
				],
				datasets: [
				    {
				        data: [statistics.reads, statistics.unreads],
				        backgroundColor: ["#2E65B3", "#97BBCD"],
				        hoverBackgroundColor: ["#6485B5", "#A9C2CF"]
				    }
				]
			};
			
			return new Chart(ctx, { type: 'doughnut', data: chartData, options: chartOptions });
		}
	}
	
	/**
	 * Actualiza los datos del gráfico y realiza un repintado.
	 * 
	 * @param statistics JSON con las estadísticas a mostrar en el gráfico con formato: { id: [...], name: [...], visits: [...], reads: [...] }
	 */
	function updateChart(statistics)
	{
		// Actualiza los datos
		chart.data.datasets[0].data[0] = statistics.reads;
		chart.data.datasets[0].data[1] = statistics.unreads;
		
		// Repinta el gráfico
		chart.update();
	}
}