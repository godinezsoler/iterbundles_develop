<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<%@page import="java.util.Map"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.lang.Math"%>
<%@page import="java.lang.Integer"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>


<!DOCTYPE html>
<html>
	<head>
		<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?env=preview'></script>
		<script type="text/javascript" src="/base-portlet/js/Chart.min.js?env=preview"></script>
		<script type='text/javascript'>
             var jQryIter = jQuery.noConflict(true);
        </script>

		<style>
		
			body {
				font-size: 14px;
				background-color: #fff;
				padding: 0;
    			margin: 0;
			}
			
			
			.aui-field {
				display: block;
			    clear: both;
			    padding: 0;
    			margin: 0;
			}
			
			.aui-field-label {
			    font-weight: bold;
			    margin-bottom: 5px;
			    display: block;
			    float: none;
			    text-align: left;
			    width: auto;
			}
			
			
			.aui-field-content {
			    display: block;
			
			}
		
			.taglib-ratings.score {
			    float: left;
			    margin-bottom: 5px;
			    white-space: nowrap;
			    width: 100%;
			}
			
			.taglib-ratings .aui-rating-content {
			    display: block;
			    margin-bottom: 5px;
			}
			
			.aui-rating-label-element {
			    display: block;
			    font-size: 12px;
			    padding: 0 2px;
			}
		
			
			.overlay-chart {
				background-color: rgba(255,255,255,1);
				display: block;
				clear: both;
				margin: 0px auto;
				align-content: center;
			    margin: 0 10px;
			}
			
			.overlay-chart-old {
				border: 5px solid rgba(200,200,200,1);
				margin-top: 15px;
			}
			
			#statistics-chart{
				width: 100%
			}
			
			 #chartjs-tooltip {
				opacity: 1;
				position: absolute;
				background: rgba(0, 0, 0, .8);
				color: white;
				padding: 6px;
				border-radius: 6px;
				-webkit-transition: all .1s ease;
				transition: all .1s ease;
				pointer-events: none;
				-webkit-transform: translate(-50%, 0);
				transform: translate(-50%, 0);
			}
			.chartjs-tooltip-key{
				display:inline-block;
				width:10px;
				height:10px;
				
			}
			.chartjs-tooltip-day
			{
				font-weight:bold;
				text-align:center;
			}
		
		</style>

	</head>
	<body>
	<%
		//se obtiene del request el id del grupo, el intervalo de fechas y las etiquetas para el chart
		long scopeGroupIdRequest =  Long.parseLong(GroupMgr.getScopeGroupId(ParamUtil.get(request, "groupid", 0)));
		String fromdate =  			ParamUtil.get(request, "fromdate", "");
		String todate =   			ParamUtil.get(request, "todate", "");
		String changeModeLb =  		ParamUtil.get(request, "lbchangemode", "");
		changeModeLb = 				StringEscapeUtils.escapeJavaScript(changeModeLb);
		String totalLb =  			StringEscapeUtils.escapeJavaScript(ParamUtil.get(request, "lbtotal", ""));
		String hadadblockLb =  		StringEscapeUtils.escapeJavaScript(ParamUtil.get(request, "lbhadadblock", ""));
		String notadblockLb =  		StringEscapeUtils.escapeJavaScript(ParamUtil.get(request, "lbnotadblock", ""));
		String adblockLb =  		StringEscapeUtils.escapeJavaScript(ParamUtil.get(request, "lbadblock", ""));
		String tableHeight =  		ParamUtil.get(request, "tableHeight", "");
		tableHeight = tableHeight.length() > 0 ? "style=\"height: " + tableHeight + "px\"" : "";
	%>
		<span class="aui-field">
			<span id="chart-location" class="aui-field-content">
			</span>
		</span>
		
		<script type="text/javascript">
			
				Chart.types.Line.extend(
				{
					name: "LineWithLine",
					draw: function () 
					{
						Chart.types.Line.prototype.draw.apply(this, arguments);
						
						var scale = this.scale;
						
						this.chart.ctx.beginPath();
						this.chart.ctx.strokeStyle = '#000000';
						
						this.chart.ctx.pointColor = '#000000';
						
						var posYText=2;
						for (var i = 0 ; i < this.options.lineAtIndex.length ; i++) 
						{
							
							if( this.options.lineAtIndex[i] == 0 )
							{
								/*si el índice donde está la línea es el 0(primer día), se alinea el texto a la izquierda */
								this.chart.ctx.textAlign = 'left';
							}
							else if( this.options.lineAtIndex[i] == this.options.allLabeldays.length -1 )
							{
								/* si el índice donde está la línea es el último día, se alinea a la derecha*/
								this.chart.ctx.textAlign = 'right'; 
							} 
							else
							{
								this.chart.ctx.textAlign = 'center';
							}
							
							var point = this.datasets[0].points[this.options.lineAtIndex[i]];
						
							// draw line
							this.chart.ctx.moveTo(point.x, scale.startPoint + 24);
							this.chart.ctx.lineTo(point.x, scale.endPoint);
							this.chart.ctx.stroke();
							
							// Fill with gradient
							 this.chart.ctx.fillStyle = '#000000';
							
							// write TODAY
							this.chart.ctx.fillText("<%=changeModeLb%>", point.x, scale.startPoint + posYText);
							
							posYText = posYText +10;
						}
					}
				});

				var url = "/base-portlet/statisticsBlockadblock/<%=scopeGroupIdRequest%>/<%=fromdate%>/<%=todate%>?env=live";
				
				//llamada por ajax al servlet(StatisticsBlockadblockServlet.java) para obtener las estadísticas
				jQryIter.ajax({url:url, dataType:'json', cache:false, success: function(counters) 
				{
					jQryIter("#chart-location").append(jQryIter('<div class="<%=tableHeight.length() > 0 ? "overlay-chart" : "overlay-chart overlay-chart-old"%>" <%=tableHeight%>><canvas id="statistics-chart" <%=tableHeight%>></canvas></div>'));
					jQryIter("#chart-location").append(jQryIter('<div id="chartjs-tooltip"></div>'));
					
					var ctx = document.getElementById("statistics-chart").getContext("2d");
					
					
					Chart.defaults.global.responsive = true;
					Chart.defaults.global.customTooltips = function(tooltip)
					{
						var tooltipEl = jQryIter('#chartjs-tooltip');
						if (!tooltip) {
							tooltipEl.css({
								opacity: 0
							});
							return;
						}
						tooltipEl.removeClass('above below');
						tooltipEl.addClass(tooltip.yAlign);
						  var innerHtml = '<div class="chartjs-tooltip-day">'
								+'<span>' +tooltip.title+'</span>'+
							'</div>';
						for (var i = tooltip.labels.length - 1; i >= 0; i--) {
							innerHtml += [
								'<div class="chartjs-tooltip-section">',
								'	<span class="chartjs-tooltip-key" style="background-color:' + tooltip.legendColors[i].fill  + '"></span>',
								'	<span class="chartjs-tooltip-value">' + tooltip.labels[i] + ' '+ data.datasets[i].label +'</span>',
								'</div>'
							].join('');
						}
						tooltipEl.html(innerHtml);

						var tooltip_left;
						if(tooltip.x <= tooltip.chart.canvas.offsetWidth / 2 )
						{
							tooltip_left = 50  + tooltip.x + 'px';
						}
						else
						{
							tooltip_left = tooltip.x -10 + 'px';
						}
							
			            tooltipEl.css({
			                          	   opacity: 1,
			                               left: tooltip_left,
			                               top: tooltip.chart.canvas.offsetTop + tooltip.y + 'px',
			                               fontFamily: tooltip.fontFamily,
			                               fontSize: tooltip.fontSize,
			                               fontStyle: tooltip.fontStyle,
			             });
					};
					
					
			
					var options = {
							scaleLineColor : "rgba(0,0,0,.1)",//líneas de escala
							scaleGridLineColor : "rgba(0,0,0,.1)",//líneas horizontales
							scaleShowVerticalLines: false,
							lineAtIndex: counters.modeChanges,
							allLabeldays:counters.days,
							datasetStrokeWidth: 3,
							bezierCurve: false,
							pointHitDetectionRadius : 0,
							scaleFontColor: "rgba(0,0,0,1)"//negro, color fuente
						};
	
	
					var data = {
							labels: counters.days,
							datasets: [
							{
									label: "<%=totalLb%>",
									type: "line",
									fillColor: "rgba(19, 79, 236,0)",
									strokeColor: "rgba(19, 79, 236,1)",
									pointColor: "rgba(19, 79, 236,1)",
									pointStrokeColor: "#fff",
									pointHighlightFill: "#fff",
									pointHighlightStroke: "rgba(220,220,220,1)",
									data: counters.all	
								},
								{
									label: "<%=hadadblockLb%>",
									type: "line",
									fillColor: "rgba(251, 249, 145,0)",
									strokeColor: "rgba(245, 229, 10,1)",
									pointColor: "rgba(245, 229, 10,1)",
									pointStrokeColor: "#fff",
									pointHighlightFill: "#fff",
									pointHighlightStroke: "rgba(220,220,220,1)",
									data: counters.hadAdblock
								},
								{
									label: "<%=adblockLb%>",
									type: "line",
									fillColor: "rgba(254, 169, 156,0)",
									strokeColor: "rgba(252, 34, 3,1)",
									pointColor: "rgba(252, 34, 3,1)",
									pointStrokeColor: "#fff",
									pointHighlightFill: "#fff",
									pointHighlightStroke: "rgba(220,220,220,1)",
									data: counters.adblock
								},
								{
									label: "<%=notadblockLb%>",
									type: "line",
									fillColor: "rgba(92, 170, 170,0)",
									strokeColor: "rgba(0, 122, 122,1)",
									pointColor: "rgba(51, 149, 149,1)",
									pointStrokeColor: "#fff",
									pointHighlightFill: "#fff",
									pointHighlightStroke: "rgba(220,220,220,1)",
									data: counters.notAdblock
								},
							]
						};
				
					var myLineChart = new Chart(ctx).LineWithLine(data, options);
				
				}});
			
		</script>
		
	</body>
</html>