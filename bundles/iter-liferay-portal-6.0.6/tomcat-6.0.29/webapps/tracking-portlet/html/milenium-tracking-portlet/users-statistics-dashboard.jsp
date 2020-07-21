<%@page import="com.protecmedia.iter.base.metrics.UserMetricsUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@page import="com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%
	// ID del grupo
	long groupId = ParamUtil.getLong(request, "groupId", 0);
	
	// Botones
	String btnLastHours = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/buttons/lasthours/text()"), "Última hora");
	String btnLastDay   = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/buttons/lastday/text()"),   "Último día");
	String btnLastMonth = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/buttons/lastmonth/text()"), "Último mes");
	String btnLastYear  = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/buttons/lastyear/text()"),  "Último año");
	String btnCSV       = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/buttons/export/text()"),    "Exportar datos a CSV");

	// Configuración
	String UIConfig = UserMetricsUtil.getUIConfiguration(groupId);
	
	// Agrupación de estadísticas
	String resolution = ParamUtil.getString(request, "resolution", "hour");
	// Tiempo real
	boolean realTime = ParamUtil.getBoolean(request, "realTime", true);
	// Número de horas a mostrar
	int displayedHours = ParamUtil.getInteger(request, "displayedHours", 24);
	// Fecha de la solicitud inicial de estadísticas
	String initialDateLimit = ParamUtil.getString(request, "date", StringPool.BLANK);
%>
<!DOCTYPE html>
<html>
<head>
	<link rel="stylesheet" type="text/css" href="/tracking-portlet/css/visits-statistics.css?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview">
	<link rel="stylesheet" type="text/css" href="/tracking-portlet/css/users-statistics.css?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview">
	<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview'></script>
	<script type="text/javascript" src="/base-portlet/js/Chart.bundle.min.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/base-portlet/js/iterCharts.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/tracking-portlet/js/UsersDashboard.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/base-portlet/js/statistics/VisitsPercentChart.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/html/js/moment/moment.min.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	
	<script type='text/javascript'>
        var jQryIter = jQuery.noConflict(true);
    </script>
</head>
<body>

	<div id="visits-header">
		<div id="extra-buttons">
			<button type="button" name="" value="" class="csv-export" onclick="dashboard.exportData()" title="<%=btnCSV%>"></button>
		</div>
		<div id="visits-date"></div>
		<div id="visits-buttons">
			<button type="button" name="" value="" class="arrow-left" onclick="dashboard.getPrevStatistics()" title=""></button>
			<button type="button" name="" value="" class="arrow-right" onclick="dashboard.getNextStatistics()" title=""></button>
			<button type="button" name="" value="" class="button-realtime" onclick="dashboard.getByResolution('hour', true)" title="<%=btnLastHours%>"></button>
			<button type="button" name="" value="" class="button-day" onclick="dashboard.getByResolution('hour', false)" title="<%=btnLastDay%>"></button>
			<button type="button" name="" value="" class="button-month" onclick="dashboard.getByResolution('day', false)" title="<%=btnLastMonth%>"></button>
			<button type="button" name="" value="" class="button-year" onclick="dashboard.getByResolution('month', false)" title="<%=btnLastYear%>"></button>
		</div>
	</div>

	<script type="text/javascript">
		var options = {
			groupId: <%=groupId%>,
			resolution:         "<%=resolution%>",
			realTime:           "<%=realTime%>",
			<% if (Validator.isNotNull(initialDateLimit)) { %> dateLimit: "<%=initialDateLimit%>", <% } %>
			displayedHours:     <%=displayedHours%>,
			ui:                 <%=UIConfig%>
		};
	
		var dashboard = new ITER.statistics.UsersDashboard(options);
		
	</script>
</body>
</html>
