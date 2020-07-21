<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%
	// ID del grupo
	long groupId = ParamUtil.getLong(request, "groupId", 0);
	// ID de la programación
	String scheduleId = ParamUtil.getString(request, "scheduleId", "");
	// Fecha desde
	String startDate = ParamUtil.getString(request, "startDate", "");
	// Fecha hasta
	String endDate = ParamUtil.getString(request, "endDate", "");
%>
<!DOCTYPE html>
<html>
<head>
	<link rel="stylesheet" type="text/css" href="/tracking-portlet/css/visits-statistics.css?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview">
	<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview'></script>
	<script type="text/javascript" src="/base-portlet/js/Chart.bundle.min.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/base-portlet/js/iterCharts.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/html/js/moment/moment.min.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/tracking-portlet/js/NewslettersDashboard.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	
	<script type='text/javascript'>
        var jQryIter = jQuery.noConflict(true);
    </script>
    
    <style>
    	.resume-logo {
    		width: 64px;
    		height: 42px;
    	}
    	
    	#resume-users .resume-logo {
    	    background: url(/tracking-portlet/img/statistics/registros.redes.svg) no-repeat center;
    	}
    	
    	#resume-subscribers .resume-logo {
    	    background: url(/tracking-portlet/img/statistics/vinculacion.redes.svg) no-repeat center;
    	}
    	
		.variance-up {
    		width: 0;
    		height: 0;
    		border-left: 15px solid transparent;
    		border-right: 15px solid transparent;
    		border-bottom: 15px solid #4caf50;
    		margin: 12px 10px 0 0;
    	}
		.variance-down {
    		width: 0;
    		height: 0;
    		border-left: 15px solid transparent;
    		border-right: 15px solid transparent;
    		border-top: 15px solid #f44336;
    		margin: 12px 10px 0 0;
    	}
		.variance-equal {
    		width: 20px;
		    height: 8px;
		    border-left: 1px solid transparent;
		    border-right: 1px solid transparent;
		    border-top: 6px solid #9E9E9E;
		    border-bottom: 6px solid #9E9E9E;
    		margin: 12px 10px 0 0;
    	}
    </style>
</head>
<body>
	<script type="text/javascript">
		var dashboard = new ITER.statistics.NewslettersDashboard({
			scheduleId: "<%=scheduleId%>",
			startDate:  "<%=startDate%>",
			endDate:    "<%=endDate%>"
		});
	</script>
</body>
</html>
