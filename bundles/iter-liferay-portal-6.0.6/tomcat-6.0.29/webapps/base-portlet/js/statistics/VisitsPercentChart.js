function VisitsPercentChart(customOptions, parentElementId)
{
	// Gr�fico
	var chart = null;
	// Div contenedor del gr�fico
	var container = null;
	// Canvas en el que se pinta el gr�fico
	var canvas = null;
	// Contexto del canvas en el que se pinta el gr�fico
	var ctx = null;
	
	// Opciones del gr�fico
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
	
	// Inicializaci�n.
	jQryIter.extend(options, customOptions);
	createContext();
	
	/**
	 * Establece los datos del gr�fico y lo crea o actualiza si ya existe.
	 */ 
	this.setStatistics = function(statistics)
	{
		// Crea / Actualiza el gr�fico
		if (chart == null)
			chart = createChart(statistics);
		else
			updateChart(statistics);
	};
	
	/**
	 * Crea el contenedor del gr�fico, estableciendo su t�tulo y eventos.
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
		
		// Crea el contenedor del gr�fico
		container = document.createElement("div");
		if (options.id)
			container.id = options.id;
		if (options.style.mainContainerClass)
			jQryIter(container).addClass(options.style.mainContainerClass);
		parentElement.append(container);
		
		// Crea el t�tulo del gr�fico
		var titleContainer = jQryIter(document.createElement("div"));
		if (options.style.titleContainerClass)
			titleContainer.addClass(options.style.titleContainerClass);
		titleContainer.append("<span>"+options.title+"</span>");
		jQryIter(container).append(titleContainer);
		
		// A�ade un canvas
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
	 * Crea un nuevo gr�fico en el contexto seleccionado.
	 * 
	 * @param statistics JSON con las estad�sticas a mostrar en el gr�fico con formato: { id: [...], name: [...], visits: [...], reads: [...] }
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
	 * Actualiza los datos del gr�fico y realiza un repintado.
	 * 
	 * @param statistics JSON con las estad�sticas a mostrar en el gr�fico con formato: { id: [...], name: [...], visits: [...], reads: [...] }
	 */
	function updateChart(statistics)
	{
		// Actualiza los datos
		chart.data.datasets[0].data[0] = statistics.reads;
		chart.data.datasets[0].data[1] = statistics.unreads;
		
		// Repinta el gr�fico
		chart.update();
	}
}