
var statisticsChartStyles = null;

function showStatistics(gatewayURL, groupId)
{	
	jQryIter(window).scrollEnd(function(){
		drawStatistics(gatewayURL, groupId);
	}, 200);
	drawStatistics(gatewayURL, groupId);
};

function drawStatistics(gatewayURL, groupId)
{
	var libUrl = gatewayURL + '/base-portlet/js/Chart.bundle.min.js?env=preview';
	jQryIter.when( jQryIter.ajax({url:libUrl, dataType:'script', cache:true, async:false}) )
	.then(function()
	{
		// Crea el plugin para anotaciones
		defineChartsPlugins();
		
		// Comienza a generar los graficos
		jQryIter('*[iteridart]').each(function() {
			var articleNode = jQryIter(this);
			var articleId = articleNode.attr('iteridart');
			var url = gatewayURL + "/base-portlet/statistics/article" + "?env=live&chartType=trend&groupId="+ groupId + "&articleId=" + articleId + "&overlay=true&item=article&resolution=minute&realTime=true&displayedHours=1";
			
			if (isScrolledIntoView(articleNode))
			{
				jQryIter.ajax({
					url:url,
					dataType:'json',
					cache:false,
					success: function(data) { printchart(articleId+(Math.round(Math.random() * 10000)), articleNode, data.data); }
				});
			}
		});
	});
}

function printchart(id, articleNode, counters)
{
	var chartId = "statistics_chart_" + id;
	var contentNode = articleNode;
	
	if (contentNode.find('canvas').length == 0 && contentNode.height() >= 100 && contentNode.width() >= 150)
	{	
		var chartDiv = jQryIter('<canvas id="' + chartId + '" class="overlay-chart"></canvas>');
		contentNode.prepend(chartDiv);
		
		chartDiv.css('z-index', 100);
		chartDiv.css('position', 'absolute');
		chartDiv.css('background-color', "rgba(255,255,255, 0.5)");
		
		var canvas = jQryIter("#" + chartId).get(0);
		var ctx = canvas.getContext("2d");

		// OPTIONS
		var options = {
			legend: {
				display: false
			},
			responsive: true,
			maintainAspectRatio: false,
			scales: {
				xAxes: [{
					gridLines: {
					   display: false,
    				   color: "black"
					},
					ticks: {
						fontColor: "black",
						beginAtZero: true
					}
				}],
	            yAxes: [{
	    			gridLines: {
    				   color: "black"
    				},
	                ticks: {
	                	fontColor: "black",
	                    beginAtZero:true,
	                    callback: function(value) {
	                    	if (value % 1 === 0) {
	                    		return value;
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
	            		return item.data[tooltipItem.index];
	        		}
	        	}
	        },
	        iterOptions: {
	        	annotation: {
		        	icon: "/tracking-portlet/img/statistics/annotation.png",
		        	scaleId: 'x-axis-0',
		            annotations: counters.annotations,
		            sysannotations: counters.sysannotations
	        	}
	        }
		};
		
		var hours = new Array();
		for ( var i=0; i< counters.hours.length; i++)
		{
			hours.push(counters.hours[i]);
		}
		
		// DATA
		var data = {
			labels: hours,
			datasets: [
				{
					backgroundColor: "rgba(92, 170, 170, 0.4)",
					borderColor: "rgba(0, 122, 122, 1)",
					pointBackgroundColor: "rgba(51, 149, 149, 1)",
					pointBorderColor: "#fff",
					lineTension: 0.3,
	        		borderWidth: 2,
					data: counters.visits
				}
			]
		};
		
		var chart = new Chart(ctx, {type: 'line', data: data, options: options });
		
		// Establece el hover para mostrar el tooltips de notificaciones
		setAnnotationHover(chartDiv, canvas, chart);
	}
};


function setAnnotationHover(chartDiv, canvas, chart)
{
	chartDiv.mousemove(function(e) { handleMouseMove(e, canvas, chart); });
    // show tooltip when mouse hovers over dot
    function handleMouseMove(e, canvas, chart)
    {
    	var iterOptions = chart.options.iterOptions;
    	var ctx = canvas.getContext("2d");
    	
    	if (iterOptions && iterOptions.annotation)
    	{
    		// Añade un evento para elimiar el tooltip si se presiona ESC
	    	jQryIter(document).keyup(function(e) {
	    		if (e.keyCode == 27 && jQryIter("#iterchart-tooltip"))
	    			jQryIter("#iterchart-tooltip").remove();
	    	});
	    	
    		jQryIter("#iterchart-tooltip").remove();
    		jQryIter(canvas).css("cursor", "default");
    		
    		var rect = canvas.getBoundingClientRect();
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
			    		jQryIter(canvas).css("cursor", "pointer");
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
		    	    	var canvasTop = window.pageYOffset + rect.top + chart.chartArea.top - canvasHeight;
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

		    	        jQryIter(canvas).css("cursor", "help");
		    	        
			    	    break;
		    	    }
				}
	    	}
    	}
    }
}

/**
 * Dado un elemento, comprueba que sea visible y que no tenga ya un gráfico.
 */
function isScrolledIntoView(elem)
{
	if (elem != null && elem.find('canvas[class="overlay-chart"]').length == 0)
	{
		var docWindow = jQryIter(window);

		var docViewTop = docWindow.scrollTop();
		var docViewBottom = docViewTop + docWindow.height();

		var elemTop = elem.offset().top;

		return ((elemTop <= docViewBottom) && (elemTop >= docViewTop));
	}
	return false;
};

function hideStatistics()
{
	// Elimina las estadisticas
	jQryIter(document).find('canvas.overlay-chart').each(function() {
		jQryIter(this).remove();
	});
	// Elimina el evento
	jQryIter(window).scrollEnd(function(){});
}

jQryIter.fn.scrollEnd = function(callback, timeout) {          
  jQryIter(this).on('scroll', function(){
    var $this = jQryIter(this);
    if ($this.data('scrollTimeout')) {
      clearTimeout($this.data('scrollTimeout'));
    }
    $this.data('scrollTimeout', setTimeout(callback,timeout));
  });
};

function hasVisists(data)
{
	var count = 0;
	
	for (counter in data)
	{
		count += counter.visits;
	}
	
	return count > 0;
};

window.addEventListener("message", receiveOverlayStatisticsMessage, false);

function receiveOverlayStatisticsMessage(event)
{
	try
	{
		var jsonMessage = JSON.parse(event.data);
		var module = jsonMessage.module;
		
		if (module && module === "overlay-statistics")
		{
			var action = jsonMessage.action;
			switch (action)
			{
				case "show":
					if (jsonMessage.data)
					{
						var gatewayURL  = jsonMessage.data.gateway;
						var groupId     = jsonMessage.data.idcms;
						if (gatewayURL && groupId)
						{
							showStatistics(gatewayURL, groupId);
						}
					}
				break;
				
				case "hide":
					hideStatistics();
				break;
			}
		}
	}
	catch(err)
	{
	    // Do nothing
	}
}


function defineChartsPlugins()
{
	//Plugin para anotaciones
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
									var hour = label.split(':')[0];
									var nextHour = parseInt(hour) == 23 ? "00" : parseInt(hour) + 1;
									nextHour =  "00".substring(0, 2 - nextHour.toString().length) + nextHour;
									var labelFromPosition = scale.getPixelForValue(hour + ":00h");
									var labelToPosition = scale.getPixelForValue(nextHour + ":00h");
									
									var range = labelToPosition - labelFromPosition;
									var minutes = parseInt(label.split(':')[1]);
									x0 = Math.floor(labelFromPosition + (range * minutes / 60)) + 0.5;
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
}
