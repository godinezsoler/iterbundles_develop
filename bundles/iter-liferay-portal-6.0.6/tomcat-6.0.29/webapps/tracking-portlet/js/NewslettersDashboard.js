var ITER = ITER || {};

ITER.statistics.NewslettersDashboard = function(customOptions)
{	
	this.options = {
		scheduleId: "",
		startDate:  "",
		endDate:    "",
		ui: {
			metrics: {
				subscribers: {
					title: "Suscriptores",
					color: "rgba(151,187,205,1)"
				},
				users: {
					title: "Usuarios del sitio",
					color: "rgba(46,101,179,1)"
				}
			},
			thousandSeparator: "."
		}
	};
	
	// Añade las opciones personalizadas
	jQryIter.extend(true, this.options, customOptions);

	this.startChartTime = null;
	this.currentChartTime = null;
	this.currentServerTime = null;
	
	this.trendChart = null;
	
	// Request de la petición de estadísticas
	this.xhr = null;
	
	// Arranca el dashboard
	this.update();
};

ITER.statistics.NewslettersDashboard.prototype.update = function()
{
	// Preserva el elemento this
	var self = this;
	
	// Solicita las estadísticas
	this.xhr = jQryIter.ajax({url:ITER.statistics.NewslettersDashboard.Util.buildUrl(this), dataType:'json', cache:false,
		success: function(statistics)
		{
			self.xhr = null;
			
			// Establece la fecha del servidor 
			self.currentServerTime = moment(statistics.serverTime);
			
			// Establece las fechas de las estadísticas
			self.startChartTime = moment(statistics.statisticsStartDate);
			self.currentChartTime = moment(statistics.statisticsEndDate);
			
			// Crea / Actualiza los gráficos
			self.processTrendChart(statistics);
			self.processTrendResume(statistics);
		}
	});
};

ITER.statistics.NewslettersDashboard.prototype.processTrendChart = function(statistics)
{
	// Crea el gráfico
	if (statistics.newsletters && statistics.newsletters.data)
	{
		if (this.trendChart)
		{
			// Actualiza los datos
			this.trendChart.data.datasets[0].data = statistics.newsletters.data.subscribers;
			this.trendChart.data.datasets[1].data = statistics.newsletters.data.users;
			
			// Actualiza las etiquetas
			this.trendChart.data.labels = statistics.statistics.data.labels;
			
			// Actualiza el gráfico
			this.trendChart.update();
		}
		else
		{var self = this;
		
		var chartOptions = {
			animation: false,
			maintainAspectRatio: false,
			legend: {
				display: false
			},
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
	                    		return ITER.statistics.NewslettersDashboard.Util.iterChartFormatNumber(value, self.options.ui.thousandSeparator);
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
                		return ITER.statistics.NewslettersDashboard.Util.iterChartFormatNumber(item.data[tooltipItem.index], self.options.ui.thousandSeparator);
	        		}
	        	}
	        }
		};
		
		var datasets = new Array();
		datasets.push(ITER.statistics.NewslettersDashboard.Util.createDataSet(this.options.ui.metrics.subscribers, statistics.newsletters.data.subscribers));
		
		var chartData = {
				labels: statistics.newsletters.data.labels,
				datasets: datasets
		};
		
		// Crea el contenedor del gráfico				
		var container = jQryIter("<div></div>").appendTo("body");
		container.addClass("visits-chart-container");
		
		var ctx = ITER.statistics.NewslettersDashboard.UI.addChartCanvas(container);
		this.trendChart = new Chart(ctx, {type: 'line', data: chartData, options: chartOptions });
		}
	}
};

ITER.statistics.NewslettersDashboard.prototype.processTrendResume = function(statistics)
{
	// Crea el gráfico
	if (statistics.newsletters && statistics.newsletters.data)
	{	
		var s0 = statistics.newsletters.data.subscribers[0];
		var s1 = statistics.newsletters.data.subscribers[statistics.newsletters.data.subscribers.length -1];
		var sVariance = s1 - s0;
		var sPercentVariance = (100 / s0) * sVariance;

		var sText = ITER.statistics.NewslettersDashboard.Util.getSign(sVariance)
		          + ITER.statistics.NewslettersDashboard.Util.iterChartFormatNumber(sVariance, this.options.ui.thousandSeparator);
		if (isFinite(sPercentVariance))
		{
			sPercentVariance = Math.round(sPercentVariance * 100) / 100;
			sText = sText + " (" + ITER.statistics.NewslettersDashboard.Util.getSign(sPercentVariance) + sPercentVariance + "%)";
		}
		
		// Crea el bloque
		resumeBlock = jQryIter("<div></div>").appendTo(jQryIter("body"));
		resumeBlock.attr("id", "resume-subscribers");
		resumeBlock.addClass("resume-block");
		var resumeBlockLogo = jQryIter("<div></div>").appendTo(resumeBlock);
		resumeBlockLogo.addClass("resume-logo");
		var resumeBlockInfo = jQryIter("<div></div>").appendTo(resumeBlock);
		resumeBlockInfo.addClass("resume-info");
		var resumeBlockText = jQryIter("<span></span>").appendTo(resumeBlockInfo);
		resumeBlockText.text(sText, this.options.ui.thousandSeparator);
		var resumeBlockSign = jQryIter("<div></div>").appendTo(resumeBlock);
		resumeBlockSign.addClass(sVariance > 0 ? "variance-up" : sVariance < 0 ? "variance-down" : "variance-equal");
	}
};

ITER.statistics.NewslettersDashboard.Util = {
		buildUrl: function(dashboard)
		{
			var url = "/base-portlet/statistics/newsletter?item=newsletter&scheduleId=" + dashboard.options.scheduleId
					+ "&dateFrom=" + dashboard.options.startDate + "&dateLimit=" + dashboard.options.endDate;
			
			return url;
		},

		createDataSet: function(metric, data)
		{
			return {
				label: metric.title,
				backgroundColor: "rgba(0,0,0,0.0)",
				borderColor: metric.color,
				pointBackgroundColor: metric.color,
				pointBorderColor: "#fff",
				lineTension: 0,
				data: data
			};
		},
		
		iterChartFormatNumber: function(number, separator)
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
		},
		
		getSign: function(number)
		{
			return isFinite(number) && number > 0 ? "+" : "";
		}
};

ITER.statistics.NewslettersDashboard.UI = {
		addChartCanvas: function(parent, cssClass)
		{
			// Añade un canvas
			var canvas = document.createElement("canvas");
			if (cssClass) canvas.className = cssClass;
			parent.append(canvas);
			
			// Inicializa el contexto
			var ctx = canvas.getContext("2d");
			
			return ctx;
		}
};