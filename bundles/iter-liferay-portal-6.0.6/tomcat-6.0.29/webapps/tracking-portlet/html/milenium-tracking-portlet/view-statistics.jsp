<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.ratings.model.RatingsStats"%>
<%@page import="com.liferay.portlet.ratings.service.RatingsStatsLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.news.model.Counters"%>
<%@page import="com.protecmedia.iter.news.service.CountersLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil" %>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalTemplate"%>
<%@page import="com.liferay.portlet.polls.service.PollsQuestionLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.service.ArticlePollLocalServiceUtil" %>
<%@page import="com.liferay.portlet.polls.service.PollsChoiceLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.ArticlePoll"%>
<%@page import="com.liferay.portlet.polls.model.PollsQuestion"%>
<%@page import="com.liferay.portlet.polls.model.PollsChoice"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.lang.Math"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil"%>



<html>
	<head>
		<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?env=preview'></script>
		<script type="text/javascript" src="/base-portlet/js/Chart.min.js?env=preview"></script>
		<script type='text/javascript'>
             var jQryIter = jQuery.noConflict(true);
        </script>

		<style>
		<!--
			body {
				font-size: 14px;
				background-color: #EEEEEE;
			}
			
			.sends {
				color: red;
			}
			
			.aui-field {
				display: block;
			    margin-bottom: 10px;
			    clear: both;
			}
			
			.aui-field-label {
			    font-weight: bold;
			    margin-bottom: 5px;
			    display: block;
			    float: none;
			    text-align: left;
			    width: auto;
			}
		
			.aui-rating-element {
			    background: url("/tracking-portlet/img/rating.png") no-repeat scroll 0 0 transparent;
			    display: block;
			    float: left;
			    font-size: 0;
			    height: 16px;
			    text-indent: -9999em;
			    width: 17px;
			}
			
			.aui-rating-element-on {
		    	background-position: 0 -16px;
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
		-->
		
			/* ---------------- */
			/*   Estilos Poll   */
			/* ---------------- */
			
			
			.vote {
				color:#666666;
				font-family:Arial,Helvetica,sans-serif;
				font-size:14px;
				font-weight:normal;
				line-height:20px;
				text-align:justify;
				border: 1px solid #CCCCCC;
			    margin: 5px;
			    padding: 10px;
			}
			
			.pregunta {
				margin-bottom:10px;
				overflow:hidden; 
				width:20%;
				margin:0 0 12px;
			}
			
			.tracking-portlet .poll .entradilla p {
				color:#666666;
				font-family:Arial,Helvetica,sans-serif;
				font-size:14px;
				font-weight:normal;
				line-height:20px;
				text-align:justify;
			}
			
			.totalvotes span {
				color:#666666;
				font-family:Arial,Helvetica,sans-serif;
				font-size:14px;
				font-weight:bold;
				line-height:20px;
			}
			
			.vote .respuesta {
				padding:5px 0;
			}
			
			.vote .respuesta label {
				padding-left:10px;
			}
			
			.vote .respuesta input {
				cursor:pointer;
				float:left;
				margin-top:4px;
			}
			
			.vote .button {
				text-align:center;
				margin:10px 0;
			}
			
			.tracking-portlet .poll .link {
				text-align:right;
			}
			
			.poll .link a:hover {
				color:#5B677D;
			}
			
			.vote .respuesta {
				padding:5px 0;
				overflow: hidden;
			}
			
			.vote .respuesta .opcion {
				float: left;
				margin-right: 2%;
				margin-top:6px;
			}
			
			.vote .respuesta .barrarespuesta {
				border:1px solid #CCCCCC;
				float:left;
				height:30px;
				width: 40%;
				margin-right: 2%;
			}
			
			.respuesta .porcentajerespuesta {
				float: left;
				width: 15%;
				margin-top: 0px;
			}
			
			.overlay-chart {
				background-color: rgba(255,255,255,1);
				border: 5px solid rgba(200,200,200,1);
				display: block;
				clear: both;
			}
		
		</style>

	</head>
	<body>
		<%
			//http://localhost:8080/tracking-portlet/milenium/tracking/statistics?groupId=10333&contentId=COVER1
		
			int hours = 12;	
			String contentId = ParamUtil.get(request, "contentId", "");
			long scopeGroupIdRequest =  Long.parseLong(GroupMgr.getScopeGroupId(ParamUtil.get(request, "groupId", 0)));
			long globalGroupIdRequest = GroupMgr.getGlobalGroupId();
			
			JournalArticle content = JournalArticleLocalServiceUtil.getArticle(globalGroupIdRequest, contentId);
			String contentStructure = content.getStructureId();
			
			Counters counterRatings = CountersLocalServiceUtil.findByCountersArticleGroupOperationFinder(content.getArticleId(), scopeGroupIdRequest, IterKeys.OPERATION_RATINGS);
			Counters counterSends = CountersLocalServiceUtil.findByCountersArticleGroupOperationFinder(content.getArticleId(), scopeGroupIdRequest, IterKeys.OPERATION_SENT);
			
			long views = VisitsStatisticsLocalServiceUtil.getTotalArticleVisits(scopeGroupIdRequest, content.getArticleId());
			
			double ratings = 0;
			if (counterRatings != null) 
			{
				ratings = TrackingUtil.round((double)counterRatings.getValue()/counterRatings.getCounter(), 1);
			}
			
			long sends = 0;
			if (counterSends != null) 
			{
				sends = counterSends.getCounter();
			}	
		%>
		
		<span class="aui-field">
			<span class="aui-field-content">							
				<label class="aui-field-label">This content has been sent <span class="sends"><%= sends %></span> times</label>
			</span>
		</span>
		<span class="aui-field">
			<span class="aui-field-content">							
				<label class="aui-field-label">Average (<span class="sends"><%= ratings %></span>)</label>
				<span>
					<div class="taglib-ratings score" id="averageRating">
						<div class="aui-helper-clearfix" id="averageRatingContent">	
						<%
							for (int i = 1; i <= 5; i++)
							{
						%>	
								<a class="aui-rating-element <%= (i <= ratings) ? "aui-rating-element-on" : StringPool.BLANK %>" href="javascript:;"></a>	
						<%
							}
						%>	
						</div>
					</div>
				</span>
			</span>
		</span>
		
		<span class="aui-field">
			<span class="aui-field-content">							
				<label class="aui-field-label">Total Views <span class="sends"><%= views %></span></label>
			</span>
		</span>									

		<span class="aui-field">
			<span id="chart-location" class="aui-field-content">
			</span>
		</span>
		
		<script type="text/javascript">
			var url = "/base-portlet/statistics/<%=scopeGroupIdRequest%>/<%=contentId%>?env=live";

			jQryIter.ajax({url:url, dataType:'json', cache:false, success: function(counters) {

				jQryIter("#chart-location").append(jQryIter('<label class="aui-field-label">Visits in the last 12 hours</label>'))
				jQryIter("#chart-location").append(jQryIter('<div class="overlay-chart"><canvas id="statistics-chart"></canvas></div>'))
				
				var ctx = document.getElementById("statistics-chart").getContext("2d");
				
				Chart.defaults.global.responsive = true;
		
				var options = {
					scaleLineColor : "rgba(0,0,0,1)",
					scaleGridLineColor : "rgba(0,0,0,0.5)",
					scaleShowVerticalLines: false,
					scaleFontColor: "rgba(0,0,0,1)"
				}

				var hours = new Array();
				for ( var i=0; i< counters.hours.length; i++)
				{
					hours.push(counters.hours[i]);
				}

				var data = {
					labels: hours,
					datasets: [
						{
							label: "Visits",
							fillColor: "rgba(92, 170, 170,0.4)",
							strokeColor: "rgba(0, 122, 122,1)",
							pointColor: "rgba(51, 149, 149,1)",
							pointStrokeColor: "#fff",
							pointHighlightFill: "#fff",
							pointHighlightStroke: "rgba(220,220,220,1)",
							data: counters.visits
						}
					]
				};
			
				var myLineChart = new Chart(ctx).Line(data, options);
			}});
			
			function hasVisists(counters)
			{
				var count = 0;
				
				for (visits in counters.visits)
				{
					count += visits;
				}
				
				return count > 0;
			};
		</script>
		
		
		<%	
			//Todo lo siguiente solo se muestra si el contentido es una encuesta
			if (contentStructure.equalsIgnoreCase("STANDARD-POLL")){
				ArticlePoll poll = ArticlePollLocalServiceUtil.getArticlePollByArticleId(globalGroupIdRequest, contentId);
				PollsQuestion pollQuestion = PollsQuestionLocalServiceUtil.getPollsQuestion(poll.getPollId());
				double totalVotes = pollQuestion.getVotesCount();
				String pregunta = pollQuestion.getDescription();
				List<PollsChoice> respuestas = PollsChoiceLocalServiceUtil.getChoices(pollQuestion.getQuestionId());
		%>
		

        
 	<div class="pregunta">            
	      <div class="texto">
	         <h1><%= pregunta %></h1>
	      </div>
    </div>
   
	<div class="vote">
  
       <div class="respuestasWrapper">
              <div class="respuestasWrapper2">
        
	        	<% for (int a=0;a<respuestas.size();a++){
	        	   Map<Locale,String> mapDescription = respuestas.get(a).getDescriptionMap();
	        	   String respuesta = mapDescription.get(LocaleUtil.getDefault());
	        	   double parcialVotes =  respuestas.get(a).getVotesCount();
	        	   double percentage = 0;
	        	   if (totalVotes != 0){
	        		   percentage = (parcialVotes / totalVotes) * 100;
	        	   }
	           %>
        	  
	           <div class="respuesta">
	               <div class="opcion">                 
	                     <input name="choiceId" type="radio" value="<%= respuesta %>" />
	                     <label><%= respuesta %></label>
	               </div>
	              
	               	 <%	
	               	 	int b = a+1;
	               		String src = "/tracking-portlet/img/barra"+b+".png";
	            	 %>
	              
	               <div class="barrarespuesta">
	                   <img style="width:<%= percentage %>%;height:100%" src="<%= src %>"/>
	               </div>
	              
	               <div class="porcentajerespuesta">                 
	                   <span><%= Math.round(percentage) %>%</span>
	               </div>
	               <br>
	               <br>
	               <br>
	            </div>
	           
	           <%}%>
	           
	           
	          </div>
	          </div>
	   
	       </div>  	
       
	       <div class="totalvotes">
	       		<span>
	       			<%= (int) totalVotes  %>
	       		</span>
	       		<span>
	      			Votos
				</span>
		   </div>
	 <% } %>	
   
	</body>
</html>
