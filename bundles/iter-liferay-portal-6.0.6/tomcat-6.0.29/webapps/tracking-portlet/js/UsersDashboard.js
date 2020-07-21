var ITER = ITER || {};

ITER.statistics.UsersDashboard = function(customOptions)
{	
	this.options = {
		groupId: 0,
		resolution:        "hour",
		realTime:          "true",
		dateLimit:         "",
		displayedHours:    24
	};
	
	// Añade las opciones personalizadas
	jQryIter.extend(true, this.options, customOptions);

	this.startChartTime = null;
	this.currentChartTime = null;
	this.currentServerTime = null;
	
	this.trendChart = null;
	this.registersChart = null;
	this.bindingsChart = null;
	this.favoritesResumeChart = null;
	this.favoritesTrendChart = null;
	this.favoriteTopicsTrendChart = null;
	this.suggestedArticlesTrendChart = null;
	
	// Request de la petición de estadísticas
	this.xhr = null;
	
	// Arranca el dashboard
	this.update();
};

ITER.statistics.UsersDashboard.prototype.update = function()
{
	// Preserva el elemento this
	var self = this;
	
	// Solicita las estadísticas
	this.xhr = jQryIter.ajax({url:ITER.statistics.UsersDashboard.Util.buildUrl(this), dataType:'json', cache:false,
		success: function(statistics)
		{
			self.xhr = null;
			
			// Establece la fecha del servidor 
			self.currentServerTime = moment(statistics.serverTime);
			
			// Establece las fechas de las estadísticas
			self.startChartTime = moment(statistics.statisticsStartDate);
			self.currentChartTime = moment(statistics.statisticsEndDate);
			jQryIter("#visits-date").text(statistics.statisticsStartDate + " / " + statistics.statisticsEndDate);
			
			// Crea / Actualiza los gráficos
			self.processResume(statistics);
			self.processTrendChart(statistics);
			self.processRegistrationChart(statistics);
			self.processSocialRankingChart(statistics);
			self.processFavoritesResumeChart(statistics);
			self.processFavoritesTrendChart(statistics);
			self.processFavoriteTopicsTrendChart(statistics);
			self.processSuggestedArticlesTrendChart(statistics);
		}
	});
};

ITER.statistics.UsersDashboard.prototype.processResume = function(statistics)
{
	if (statistics.users && statistics.users.resume)
	{
		ITER.statistics.UsersDashboard.UI.addOrUpdateResumeBlock("registrations",           statistics.users.resume.registrations,         this.options.ui.metrics.registrations,         this.options.ui.thousandSeparator, 0);
		ITER.statistics.UsersDashboard.UI.addOrUpdateResumeBlock("social-registrations",    statistics.users.resume.socialRegistrations,   this.options.ui.metrics.socialRegistrations,   this.options.ui.thousandSeparator, 1);
		ITER.statistics.UsersDashboard.UI.addOrUpdateResumeBlock("profile-modifications",   statistics.users.resume.profileModifications,  this.options.ui.metrics.profileModifications,  this.options.ui.thousandSeparator, 2);
		ITER.statistics.UsersDashboard.UI.addOrUpdateResumeBlock("social-account-bindigns", statistics.users.resume.socialAccountBindings, this.options.ui.metrics.socialAccountBindings, this.options.ui.thousandSeparator, 3);
		ITER.statistics.UsersDashboard.UI.addOrUpdateResumeBlock("recoveries",              statistics.users.resume.recoveries,            this.options.ui.metrics.recoveries,            this.options.ui.thousandSeparator, 4);
		ITER.statistics.UsersDashboard.UI.addOrUpdateResumeBlock("account-deletes",         statistics.users.resume.accountDeletes,        this.options.ui.metrics.accountDeletes,        this.options.ui.thousandSeparator, 5);
	}
};

ITER.statistics.UsersDashboard.prototype.processTrendChart = function(statistics)
{
	// Crea el gráfico
	if (statistics.users && statistics.users.data)
	{
		if (this.trendChart)
		{
			// Actualiza los datos
			this.trendChart.data.datasets[0].data = statistics.users.data.registrations;
			this.trendChart.data.datasets[1].data = statistics.users.data.socialRegistrations;
			this.trendChart.data.datasets[2].data = statistics.users.data.profileModifications;
			this.trendChart.data.datasets[3].data = statistics.users.data.socialAccountBindings;
			this.trendChart.data.datasets[4].data = statistics.users.data.recoveries;
			this.trendChart.data.datasets[5].data = statistics.users.data.accountDeletes;
			
			// Actualiza las etiquetas
			this.trendChart.data.labels = statistics.users.data.labels;
			
			// Actualiza el gráfico
			this.trendChart.update();
		}
		else
		{
			var self = this;
			
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
		                    		return ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, self.options.ui.thousandSeparator);
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
                    		return item.label + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(item.data[tooltipItem.index], self.options.ui.thousandSeparator);
		        		}
		        	}
		        }
			};
			
			var datasets = new Array();
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet(this.options.ui.metrics.registrations,         statistics.users.data.registrations));
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet(this.options.ui.metrics.socialRegistrations,   statistics.users.data.socialRegistrations));
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet(this.options.ui.metrics.profileModifications,  statistics.users.data.profileModifications));
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet(this.options.ui.metrics.socialAccountBindings, statistics.users.data.socialAccountBindings));
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet(this.options.ui.metrics.recoveries,            statistics.users.data.recoveries));
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet(this.options.ui.metrics.accountDeletes,        statistics.users.data.accountDeletes));
			
			var chartData = {
					labels: statistics.users.data.labels,
					datasets: datasets
			};
			
			// Crea el contenedor del gráfico				
			var container = jQryIter("<div></div>").appendTo("body");
			container.addClass("visits-chart-container");
			
			var ctx = ITER.statistics.UsersDashboard.UI.addChartCanvas(container);
			this.trendChart = new Chart(ctx, {type: 'line', data: chartData, options: chartOptions });
		}
	}
};

ITER.statistics.UsersDashboard.prototype.processRegistrationChart = function (statistics)
{
	if (this.registersChart)
	{
		// Actualiza los datos
		this.registersChart.data.datasets[0].data = [statistics.users.resume.socialRegistrations, statistics.users.resume.registrations];
		
		// Repinta el gráfico
		this.registersChart.update();
	}
	else
	{
		var self = this;
		
		var chartOptions = {
			maintainAspectRatio: false,
			cutoutPercentage : 35,
	        tooltips: {
	        	callbacks: {
	        		label: function(tooltipItem, data) {
                		return data.labels[tooltipItem.index] + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index], self.options.ui.thousandSeparator);
	        		}
	        	}
	        }
		};
		
		var chartData = {
			labels: [
			    this.options.ui.metrics.socialRegistrations.title,
			    this.options.ui.metrics.registrations.title
			],
			datasets: [
			    {
			        data: [statistics.users.resume.socialRegistrations, statistics.users.resume.registrations],
			        backgroundColor: ["#2E65B3", "#97BBCD"],
			        hoverBackgroundColor: ["#6485B5", "#A9C2CF"]
			    }
			]
		};
		
		var parent = ITER.statistics.UsersDashboard.UI.getParent("registrations");
		
		// Crea el contenedor del gráfico
		var container = jQryIter("<div></div>").appendTo(parent);
		container.attr('id', 'registration-chart-container');
		container.addClass("chart-container");
		
		var titleDiv = jQryIter("<div></div>").appendTo(container);
		titleDiv.addClass("chart-title-container");
		var title = jQryIter("<span></span>").appendTo(titleDiv);
		title.text(this.options.ui.titles.registrationComparisonTitle);
		
		// Añade un canvas
		var canvas = document.createElement("canvas");
		container.append(canvas);
		
		// Inicializa el contexto
		var ctx = canvas.getContext("2d");
		
		this.registersChart = new Chart(ctx, { type: 'doughnut', data: chartData, options: chartOptions });
	}
};

ITER.statistics.UsersDashboard.prototype.processSocialRankingChart = function(statistics)
{
	if (this.bindingsChart)
	{
		// Actualiza los datos
		this.bindingsChart.data.datasets[0].data = statistics.users.bindings.data;
		
		// Actualiza el tamaño por si hubiera cambiado
		this.bindingsChart.resize();
		
		// Repinta el gráfico
		this.bindingsChart.update();
	}
	else
	{
		var self = this;
		
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
	                    		return ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, self.options.ui.thousandSeparator);
	                    	}
	                    }
	                }
				}]
			},
	        tooltips: {
	        	callbacks: {
	        		title: function(tooltipItems, data) { return ""; },
	        		label: function(tooltipItem, data) {
                		return data.datasets[tooltipItem.datasetIndex].label + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index], self.options.ui.thousandSeparator);
	        		}
	        	}
	        }
		};
		
		var rankingData = {
			labels: statistics.users.bindings.labels,
			datasets: [
	           {
	               label: this.options.ui.texts.rankingTooltip,
	               backgroundColor: statistics.users.bindings.colors,
	               borderColor: statistics.users.bindings.colors,
	               borderWidth: 1,
	               data: statistics.users.bindings.data,
	           }
	       ]
		};
		
		var parent = ITER.statistics.UsersDashboard.UI.getParent("bindigns");
		
		var container = jQryIter("<div></div>").appendTo(parent);
		container.attr('id', 'bindings-chart-container');
		container.addClass("chart-container");
		
		var titleDiv = jQryIter("<div></div>").appendTo(container);
		titleDiv.addClass("chart-title-container");
		var title = jQryIter("<span></span>").appendTo(titleDiv);
		title.text(this.options.ui.titles.socialAccountsRankingTitle);
	
		var canvas = document.createElement("canvas");
		container.append(canvas);
		var ctx = canvas.getContext("2d");
		
		this.bindingsChart = new Chart(ctx, { type: 'horizontalBar', data: rankingData, options: rankingOptions });
	}
};

ITER.statistics.UsersDashboard.prototype.processFavoritesResumeChart = function (statistics)
{
	if (statistics.users && statistics.users.favorites && statistics.users.favorites.resume)
	{
		if (this.favoritesResumeChart)
		{
			// Actualiza los datos
			this.favoritesResumeChart.data.datasets[0].data = [statistics.users.favorites.resume.usingFavorites, statistics.users.favorites.resume.total - statistics.users.favorites.resume.usingFavorites];
			
			// Repinta el gráfico
			this.favoritesResumeChart.update();
		}
		else
		{
			var self = this;
			
			var chartOptions = {
				maintainAspectRatio: false,
				cutoutPercentage : 35,
		        tooltips: {
		        	callbacks: {
		        		label: function(tooltipItem, data) {
	                		return data.labels[tooltipItem.index] + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index], self.options.ui.thousandSeparator);
		        		}
		        	}
		        }
			};
			
			var chartData = {
				labels: [
				    this.options.ui.texts.favoriteResumeUsingTooltip,
				    this.options.ui.texts.favoriteResumeTotalTooltip
				],
				datasets: [
				    {
				        data: [statistics.users.favorites.resume.usingFavorites, statistics.users.favorites.resume.total - statistics.users.favorites.resume.usingFavorites],
				        backgroundColor: ["#2E65B3", "#97BBCD"],
				        hoverBackgroundColor: ["#6485B5", "#A9C2CF"]
				    }
				]
			};
			
			var parent = ITER.statistics.UsersDashboard.UI.getParent("favorites");
			
			// Crea el contenedor del gráfico
			var container = jQryIter("<div></div>").appendTo(parent);
			container.attr("id", "favorites-resume-chart-container");
			container.addClass("chart-container");
			
			var titleDiv = jQryIter("<div></div>").appendTo(container);
			titleDiv.addClass("chart-title-container");
			var title = jQryIter("<span></span>").appendTo(titleDiv);
			title.text(this.options.ui.titles.favoritesResumeTitle);
			
			// Añade un canvas
			var canvas = document.createElement("canvas");
			container.append(canvas);
			
			// Inicializa el contexto
			var ctx = canvas.getContext("2d");
			
			this.favoritesResumeChart = new Chart(ctx, { type: 'doughnut', data: chartData, options: chartOptions });
		}
	}
};

ITER.statistics.UsersDashboard.prototype.processFavoritesTrendChart = function (statistics)
{
	// Crea el gráfico
	if (statistics.users && statistics.users.favorites && statistics.users.favorites.trend)
	{
		if (this.favoritesTrendChart)
		{
			// Actualiza los datos
			this.favoritesTrendChart.data.datasets[0].data = statistics.users.favorites.trend.articles;
			
			// Actualiza las etiquetas
			this.favoritesTrendChart.data.labels = statistics.users.favorites.trend.labels;
			
			// Actualiza el gráfico
			this.favoritesTrendChart.update();
		}
		else
		{
			var self = this;
			
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
		                    		return ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, self.options.ui.thousandSeparator);
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
                    		return item.label + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(item.data[tooltipItem.index], self.options.ui.thousandSeparator);
		        		}
		        	}
		        }
			};
			
			var datasets = new Array();
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet({title: this.options.ui.texts.favoriteTrendTooltip, color: "#2e65b3"}, statistics.users.favorites.trend.articles));
			
			var chartData = {
					labels: statistics.users.favorites.trend.labels,
					datasets: datasets
			};
			
			var parent = ITER.statistics.UsersDashboard.UI.getParent("favorites");
			
			// Crea el contenedor del gráfico				
			var container = jQryIter("<div></div>").appendTo(parent);
			container.attr("id", "favorites-trend-chart-container");
			container.addClass("chart-container");
			
			var titleDiv = jQryIter("<div></div>").appendTo(container);
			titleDiv.addClass("chart-title-container");
			var title = jQryIter("<span></span>").appendTo(titleDiv);
			title.text(this.options.ui.titles.favoritesTrendTitle);
			
			var canvas = document.createElement("canvas");
			container.append(canvas);
			var ctx = canvas.getContext("2d");
			
			this.favoritesTrendChart = new Chart(ctx, {type: 'line', data: chartData, options: chartOptions });
		}
	}
};

ITER.statistics.UsersDashboard.prototype.processFavoriteTopicsTrendChart = function (statistics)
{
	// Crea el gráfico
	if (statistics.users && statistics.users.favorites && statistics.users.favorites.topicsTrend)
	{
		if (this.favoriteTopicsTrendChart)
		{
			// Actualiza los datos
			this.favoriteTopicsTrendChart.data.datasets[0].data = statistics.users.favorites.topicsTrend.topics;
			
			// Actualiza las etiquetas
			this.favoriteTopicsTrendChart.data.labels = statistics.users.favorites.topicsTrend.labels;
			
			// Actualiza el gráfico
			this.favoriteTopicsTrendChart.update();
		}
		else
		{
			var self = this;
			
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
		                    		return ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, self.options.ui.thousandSeparator);
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
                    		return item.label + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(item.data[tooltipItem.index], self.options.ui.thousandSeparator);
		        		}
		        	}
		        }
			};
			
			var datasets = new Array();
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet({title: this.options.ui.texts.favoriteTopicsTrendTooltip, color: "#2e65b3"}, statistics.users.favorites.topicsTrend.topics));
			
			var chartData = {
					labels: statistics.users.favorites.topicsTrend.labels,
					datasets: datasets
			};
			
			var parent = ITER.statistics.UsersDashboard.UI.getParent("favoriteTopics");
			
			// Crea el contenedor del gráfico				
			var container = jQryIter("<div></div>").appendTo(parent);
			container.attr("id", "favorite-topics-trend-chart-container");
			container.addClass("chart-container");
			
			var titleDiv = jQryIter("<div></div>").appendTo(container);
			titleDiv.addClass("chart-title-container");
			var title = jQryIter("<span></span>").appendTo(titleDiv);
			title.text(this.options.ui.titles.favoriteTopicsTrendTitle);
			
			var canvas = document.createElement("canvas");
			container.append(canvas);
			var ctx = canvas.getContext("2d");
			
			this.favoriteTopicsTrendChart = new Chart(ctx, {type: 'line', data: chartData, options: chartOptions });
		}
	}
};

ITER.statistics.UsersDashboard.prototype.processSuggestedArticlesTrendChart = function (statistics)
{
	// Crea el gráfico
	if (statistics.users && statistics.users.favorites && statistics.users.favorites.suggestionsTrend)
	{
		if (this.suggestedArticlesTrendChart)
		{
			// Actualiza los datos
			this.suggestedArticlesTrendChart.data.datasets[0].data = statistics.users.favorites.suggestionsTrend.articles;
			
			// Actualiza las etiquetas
			this.suggestedArticlesTrendChart.data.labels = statistics.users.favorites.suggestionsTrend.labels;
			
			// Actualiza el gráfico
			this.suggestedArticlesTrendChart.update();
		}
		else
		{
			var self = this;
			
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
		                    		return ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, self.options.ui.thousandSeparator);
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
                    		return item.label + ": " + ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(item.data[tooltipItem.index], self.options.ui.thousandSeparator);
		        		}
		        	}
		        }
			};
			
			var datasets = new Array();
			datasets.push(ITER.statistics.UsersDashboard.Util.createDataSet({title: this.options.ui.texts.suggestedArticlesTrendTooltip, color: "#2e65b3"}, statistics.users.favorites.suggestionsTrend.articles));
			
			var chartData = {
					labels: statistics.users.favorites.suggestionsTrend.labels,
					datasets: datasets
			};
			
			var parent = ITER.statistics.UsersDashboard.UI.getParent("suggestedArticles");
			
			// Crea el contenedor del gráfico				
			var container = jQryIter("<div></div>").appendTo(parent);
			container.attr("id", "suggested-articles-trend-chart-container");
			container.addClass("chart-container");
			
			var titleDiv = jQryIter("<div></div>").appendTo(container);
			titleDiv.addClass("chart-title-container");
			var title = jQryIter("<span></span>").appendTo(titleDiv);
			title.text(this.options.ui.titles.suggestedArticlesTrendTitle);
			
			var canvas = document.createElement("canvas");
			container.append(canvas);
			var ctx = canvas.getContext("2d");
			
			this.suggestedArticlesTrendChart = new Chart(ctx, {type: 'line', data: chartData, options: chartOptions });
		}
	}
};

///////////////////////////////////////////////////////////////////////
//                  MANEJADORES PARA LA NAVEGACION                   //
///////////////////////////////////////////////////////////////////////
ITER.statistics.UsersDashboard.prototype.getPrevStatistics = function()
{
	// Cancela la petición en progreso si la hubiera
	if (this.xhr) this.xhr.abort();

	if (this.options.resolution == "hour")
		this.currentChartTime.subtract(this.options.displayedHours, 'hours');
	else if (this.options.resolution == "day")
		this.currentChartTime.subtract(1, 'month');
	else if (this.options.resolution == "month")
		this.currentChartTime.subtract(1, 'year');
	
	this.update();
};

ITER.statistics.UsersDashboard.prototype.getNextStatistics = function()
{
	// Cancela la petición en progreso si la hubiera
	if (this.xhr) this.xhr.abort();
	
	if (this.currentChartTime.isBefore(this.currentServerTime, 'hour'))
	{
		if (this.options.resolution == "hour")
			this.currentChartTime.add(this.options.displayedHours, 'hours');
		else if (this.options.resolution == "day")
			this.currentChartTime.add(1, 'month');
		else if (this.options.resolution == "month")
			this.currentChartTime.add(1, 'year');
		
		this.update();
	}
};

ITER.statistics.UsersDashboard.prototype.getByResolution = function(resolution, realTime)
{
	// Cancela la petición en progreso si la hubiera
	if (this.xhr) this.xhr.abort();
	
	this.options.resolution = resolution;
	this.options.realTime = realTime;
	this.currentChartTime = null;
	this.update();
};

ITER.statistics.UsersDashboard.prototype.exportData = function()
{	
	var fileName = "user-metrics-from-" + this.startChartTime.format("YYYYMMDDHHmmss") + "-to-" + this.currentChartTime.format("YYYYMMDDHHmmss") + "-grouped-by-" + this.options.resolution + ".csv";
	var csvContent = "Metric," + this.trendChart.data.labels.join(",");
	this.trendChart.data.datasets.forEach(function(dataset) {
		csvContent += "\n" + dataset.label + "," + dataset.data.join(",");
	});
	
	// IE10+
	if (navigator.msSaveBlob)
	{
		var blobObject = new Blob([csvContent]);
		navigator.msSaveBlob(blobObject, fileName);
	}
	//html5 A[download]
	else if ('download' in document.createElement('a'))
	{
		var encodedUri = encodeURI("data:text/csv;charset=utf-8," + csvContent);
		var link = document.createElement("a");
		link.setAttribute("href", encodedUri);
		link.setAttribute("download", fileName);
		document.body.appendChild(link); // Required for FF
		link.click();
		link.parentNode.removeChild(link);
	}
	// IE9-
	else
	{
		var frame = document.createElement('iframe');
	    document.body.appendChild(frame);

	    frame.contentWindow.document.open("text/html", "replace");
	    frame.contentWindow.document.write(csvContent);
	    frame.contentWindow.document.close();
	    frame.contentWindow.focus();
	    frame.contentWindow.document.execCommand('SaveAs', true, fileName);

	    document.body.removeChild(frame);
	}
};

ITER.statistics.UsersDashboard.prototype.toggleDataset = function(index)
{
	if (index >= 0 && index < this.trendChart.data.datasets.length)
	{
		this.trendChart.data.datasets[index]._meta[0].hidden = !this.trendChart.data.datasets[index]._meta[0].hidden;
		this.trendChart.update();
	}
};

///////////////////////////////////////////////////////////////////////
//                            UTILIDADES                             //
///////////////////////////////////////////////////////////////////////

ITER.statistics.UsersDashboard.Util = {
	buildUrl: function(dashboard)
	{
		var url = "/base-portlet/statistics/user?item=user&groupId=" + dashboard.options.groupId + "&resolution=" + dashboard.options.resolution
				+ "&realTime=" + dashboard.options.realTime;

		if (dashboard.options.displayedHours != 24)  url += "&displayedHours=" + dashboard.options.displayedHours;
		if (dashboard.currentChartTime)              url += "&dateLimit="      + dashboard.currentChartTime.format("YYYYMMDDHHmmss");
		
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
	
	getPerceivedBrightness: function(bgExColor)
	{
		var rgb = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(bgExColor);
		var r = parseInt(rgb[1], 16);
		var g = parseInt(rgb[2], 16);
		var b = parseInt(rgb[3], 16);
		return ((r * 299) + (g * 587) + (b * 114)) / 1000;
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
	}
};

ITER.statistics.UsersDashboard.UI = {
	getParent: function(chartName)
	{
		var parent = null;
		
		switch (chartName) {
		case "resume":
			// Selecciona el contenedor de bloques. Si no existe, lo crea.
			parent = jQryIter("#resume-block-container");
			if (parent.length == 0)
			{
				parent = jQryIter("<div></div>").appendTo("body");
				parent.attr("id", "resume-block-container");
			}
			break;
			
		case "registrations":
		case "bindigns":
			parent = jQryIter("#registration-info-container");
			if (parent.length == 0) {
				parent = jQryIter("<div></div>").appendTo("body");
				parent.attr("id", "registration-info-container");
				parent.addClass("charts-container-2-columns");
			}
			break;
			
		case "favorites":
			parent = jQryIter("#favorite-info-container");
			if (parent.length == 0) {
				parent = jQryIter("<div></div>").appendTo("body");
				parent.attr("id", "favorite-info-container");
				parent.addClass("charts-container-2-columns");
			}
			break;
			
		case "favoriteTopics":
			parent = jQryIter("#favorite-topics-info-container");
			if (parent.length == 0) {
				parent = jQryIter("<div></div>").appendTo("body");
				parent.attr("id", "favorite-topics-info-container");
			}
			break;
			
		case "suggestedArticles":
			parent = jQryIter("#suggested-articles-info-container");
			if (parent.length == 0) {
				parent = jQryIter("<div></div>").appendTo("body");
				parent.attr("id", "suggested-articles-info-container");
			}
			break;
			
		default:
			break;
		}
		
		return parent;
	},
	
	addOrUpdateResumeBlock: function(id, value, metric, separator, index)
	{
		var resumeBlock = jQryIter("#resume-" + id);
		if (resumeBlock.length == 0) resumeBlock = jQryIter("#resume-" + id + "-w");
		
		if (resumeBlock.length > 0)
		{
			resumeBlock.find(".resume-info span").text(ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, separator));
		}
		else
		{
			// Selecciona el contenedor de bloques. Si no existe, lo crea.
			var container = ITER.statistics.UsersDashboard.UI.getParent("resume");
			
			// Crea el bloque
			resumeBlock = jQryIter("<div></div>").appendTo(container);
			resumeBlock.attr("id", ITER.statistics.UsersDashboard.UI.optimalResumeBlockBackground("resume-" + id, metric.color));
			resumeBlock.attr("title", metric.title);
			resumeBlock.addClass("resume-block");
			resumeBlock.css('border', "1px solid " + metric.color);
			resumeBlock.click(function() {
				jQryIter(this).toggleClass("resume-block-disabled");
				dashboard.toggleDataset(index);
			});
			
			var resumeBlockLogo = jQryIter("<div></div>").appendTo(resumeBlock);
			resumeBlockLogo.addClass("resume-logo");
			resumeBlockLogo.css('background-color', metric.color);
			
			var resumeBlockInfo = jQryIter("<div></div>").appendTo(resumeBlock);
			resumeBlockInfo.addClass("resume-info");
			
			var resumeBlockText = jQryIter("<span></span>").appendTo(resumeBlockInfo);
			resumeBlockText.text(ITER.statistics.UsersDashboard.Util.iterChartFormatNumber(value, separator));
		}
	},

	addChartCanvas: function(parent, cssClass)
	{
		// Añade un canvas
		var canvas = document.createElement("canvas");
		if (cssClass) canvas.className = cssClass;
		parent.append(canvas);
		
		// Inicializa el contexto
		var ctx = canvas.getContext("2d");
		
		return ctx;
	},
	
	optimalResumeBlockBackground : function(id, bgColor)
	{
		var b = ITER.statistics.UsersDashboard.Util.getPerceivedBrightness(bgColor);
		return b > 125 ? id : id + "-w";
	}
};