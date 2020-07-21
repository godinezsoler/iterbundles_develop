<%@page import="com.google.gson.JsonArray"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.util.survey.IterSurveyUtil"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GroupMgr"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portal.service.LayoutSetLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.util.URLSigner"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="java.net.URI"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ page contentType="text/html; charset=UTF-8" %>

<%!
	private static Log _log = LogFactoryUtil.getLog("tracking-portlet.docroot.errors.visits-statistics-dashboard.jsp");
%>

<%
	// ID del grupo
	long groupId = ParamUtil.getLong(request, "groupId", 0);

	// http://jira.protecmedia.com:8080/browse/ITER-1093?focusedCommentId=54083&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-54083
	boolean isDemo = PropsValues.ITER_DEMO_FORCE_STATISTICS_RESOLUTION &&
						LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost().toLowerCase().endsWith(PropsValues.ITER_DEMO_SITENAME);

	// Id de sección
	long plid = ParamUtil.getLong(request, "plid", 0);

	// Id de metadato
	long categoryId = ParamUtil.getLong(request, "categoryId", 0);
	
	// Id del artículo
	String contentId = ParamUtil.getString(request, "contentId", StringPool.BLANK);
	String contentIdCrc = "";
	
	// Tipo de página
	String pageType = "group";
	if (contentId != null && !contentId.isEmpty())
		pageType = "article";
	else if (categoryId > 0)
		pageType = "metadata";
	else if (plid > 0)
		pageType = "section";

	// Vocabularios a mostrar
	String vocabularies = ParamUtil.getString(request, "vocabularies", StringPool.BLANK);
	
	// Indica si debe mostrarse el grafico de visitas
	boolean showVisits = ParamUtil.getBoolean(request, "showVisits", true);
	
	// Indica si el dashboard de las estadísticas de MAS es de sólo lectura (true) o puede modificarse (false).
	boolean readOnly = ParamUtil.getBoolean(request, "readOnly", true);
	
	boolean showVisitsTrend = showVisits ? ParamUtil.getBoolean(request, "showVisitsTrend", true) : false;
	// Limite de secciones a mostrar en el ranking
	int maxSections = showVisits ? ParamUtil.getInteger(request, "maxSections", 5) : 0;
	// Limite de artículos a mostrar en el ranking
	int maxArticles = showVisits ? ParamUtil.getInteger(request, "maxArticles", 5) : 0;
	// Limite de artículos a mostrar en el ranking de más leídos
	int maxReadings = showVisits ? ParamUtil.getInteger(request, "maxReadings", 5) : 0;
	// Limite de artículos a mostrar en el ranking de más compartidos
	int maxSharings = showVisits ? ParamUtil.getInteger(request, "maxSharings", 5) : 0;
	// Limite de artículos a mostrar en el ranking de más comentados
	int maxComments = showVisits ? ParamUtil.getInteger(request, "maxComments", 5) : 0;
	// Limite de artículos a mostrar en el ranking de mejor valorados
	int maxFeedback = showVisits ? ParamUtil.getInteger(request, "maxFeedback", 5) : 0;

	// Titulos
	String titleTotalVisits = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/texts/totalvisits/text()"), "Visitas totales");
	String titleReadings       = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/texts/readings/text()"), "Lecturas");
	String titleSocialNetworks = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/texts/socialNetworks/text()"), "Redes sociales");
	String titleComments       = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/texts/comments/text()"), "Comentarios");
	String titleFeedback       = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/texts/feedbacktitle/text()"), "Valoración de los usuarios");;

	// Botones
	String btnLastHours = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/buttons/lasthours/text()"), "Última hora");
	String btnLastDay   = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/buttons/lastday/text()"),   "Último día");
	String btnLastMonth = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/buttons/lastmonth/text()"), "Último mes");
	String btnLastYear  = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/buttons/lastyear/text()"),  "Último año");

	// Tooltips
	String tooltipTotalVisits     = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/tooltips/visits/text()"),        "Visitas totales");
	String tooltipArticleVisits   = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/tooltips/articlevisits/text()"), "Visitas de artículos");
	String tooltipArticleReads    = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/tooltips/reads/text()"),         "Lecturas");
	String tooltipArticleSharings = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/tooltips/shared/text()"),        "Comparticiones");
	String tooltipArticleComments = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/tooltips/comments/text()"),      "Comentarios");
	String tooltipArticleRate     = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/tooltips/assessment/text()"),    "Valoración");

	// Textos
	String emptyVisitsMessage = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/texts/emptyVisitsMessage/text()"), "No se dispone de información de visitas.");
	
	// Experimentos
	String expStatus      = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/status/text()"), "Estado");
	String expStartDate   = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/startdate/text()"), "Fecha de inicio");
	String expEndDate     = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/enddate/text()"), "Fecha de fin");
	String expPending     = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/pending/text()"), "Pendiente");
	String expRunning     = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/running/text()"), "En curso");
	String expFinished    = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/finished/text()"), "Finalizado");
	String expStop        = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/stop/text()"), "Parar experimento");
	String expCtr         = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/ctr/text()"), "CTR");
	String expImpressions = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/impressions/text()"), "Impresiones");
	String expViews       = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/experiments/views/text()"), "Visitas");
	
	// Modo Debug
	boolean debug = ParamUtil.getBoolean(request, "debug", false);
	// Polling Rate
	long pollingRate = GetterUtil.getLong( PortalUtil.getPortalProperties().getProperty("iter.statistics.pollingrate"), 30);
	// Refresh Rate
	long refreshRate = GetterUtil.getLong( PortalUtil.getPortalProperties().getProperty("iter.statistics.refreshrate"), 300);
	// Separador de miles
	String thousandSeparator = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/@thousandseparator"), StringPool.BLANK);
	thousandSeparator = thousandSeparator.equals("point") ? StringPool.PERIOD : thousandSeparator.equals("comma") ? StringPool.COMMA : StringPool.BLANK;
	// Agrupación de estadísticas
	String resolution = ParamUtil.getString(request, "resolution", (isDemo) ? "hour" : "minute");
	// Tiempo real
	boolean realTime = ParamUtil.getBoolean(request, "realTime", true);
	// Número de horas a mostrar
	int displayedHours = ParamUtil.getInteger(request, "displayedHours", 1);
	// Fecha de la solicitud inicial de estadísticas
	String initialDateLimit = ParamUtil.getString(request, "date", StringPool.BLANK);
	// Mostar anotaciones
	String annotations = ParamUtil.getString(request, "annotations", StringPool.BLANK);
	
	// Integracíon con MAS
	String masUrl = StringPool.BLANK;
	String masIdSite = StringPool.BLANK;
	boolean masStatsEnabled = GetterUtil.getBoolean(GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@enablemetrics"), false);
	if (masStatsEnabled)
	{
		masIdSite = GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@appid");

		try
		{
			URI masUri = new URI(PropsValues.ITER_MAS_PIWIK_URL);
			masUrl = masUri.getScheme() + "://" + masUri.getAuthority();
		}
		catch (Throwable th)
		{
			_log.error("Unparseable MAS URL from property " + PropsKeys.ITER_MAS_PIWIK_URL + ": " + PropsValues.ITER_MAS_PIWIK_URL);
		}
	}
	
	// Virtual Host del entorno LIVE
	// URL absoluta independiente del protocolo
	String virtualHost = "//" + LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
	
	// Array JSON para encuestas
	JsonArray surveys = new JsonArray();
%>
<!DOCTYPE html>
<html>
<head>
	<link href="https://fonts.googleapis.com/css?family=Roboto&display=swap" rel="stylesheet">
	<link rel="stylesheet" type="text/css" href="/tracking-portlet/css/visits-statistics.css?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview">
	<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview'></script>
	<script type='text/javascript'>var jQryIter = jQuery.noConflict(true);</script>
	<script type='text/javascript' src='/html/js/iter/jqryiter-ext-footer.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview'></script>
	<script type="text/javascript" src="/base-portlet/js/Chart.bundle.min.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/base-portlet/js/statistics/VisitsPercentChart.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
	<script type="text/javascript" src="/base-portlet/js/iterCharts.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>&env=preview"></script>
</head>
<body>

	<div id="MCMEvent:statisticDetail" name="MCMEvent:statisticDetail" statisticType="" groupId="<%=groupId%>" idcms="" titleart="" style="display: none;"></div>
	<div id="MCMEvent:statisticConfig" name="MCMEvent:statisticConfig" resolution="" date="" displayedHours="" realTime="" style="display: none;"></div>
	<div id="MCMEvent:statisticAnnotation" name="MCMEvent:statisticAnnotation" groupId="" startDate="" endDate="" idart="" mode="" style="display: none;"></div>

	<div id="visits-header">
<%
		if (Validator.isNotNull(masIdSite) && ("group".equals(pageType) || "article".equals(pageType)))
		{
			if ("article".equals(pageType))
				contentIdCrc = URLSigner.generateSign(contentId, IterURLUtil.ARTICLEID_CRC_LENGTH);
%>
		<button type="button" name="mas-button" value="" id="mas-button" onclick="chartMgr.toggleMAS()" title=""></button>
<%
		}
%>
		<div class="fill"></div>
<%
        if ("article".equals(pageType))
        {
        	JournalArticle ja = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), contentId);
        	List<Long> groups = ja.getScopeGroupIds();
%>
			<div id="visits-group">
				<select onchange="chartMgr.changeGroup(this.value)">
				<% for (int i = 0; i < groups.size(); i++) { %>
					<option value="<%=groups.get(i)%>" <%=groups.get(i) == groupId ? "selected" : ""%>><%=GroupLocalServiceUtil.getGroup(groups.get(i)).getName()%></option>
				<% } %>
				</select>
			</div>
<%
			// Si tiene encuestas, recupera sus identificadores
			surveys = IterSurveyUtil.getSurveys(ja, String.valueOf(groupId));
        }
%>
		<div id="date-handler">
			<div id="visits-date"></div>
			<div id="visits-buttons">
				<button type="button" name="" value="" class="arrow-left" onclick="chartMgr.getPrevStatistics()" title=""></button>
				<button type="button" name="" value="" class="arrow-right" onclick="chartMgr.getNextStatistics()" title=""></button>
				<button type="button" name="" value="" class="button-realtime" onclick="chartMgr.getVisitsByResolution('minute', true)" title="<%=btnLastHours%>"></button>
				<button type="button" name="" value="" class="button-day" onclick="chartMgr.getVisitsByResolution('hour', false)" title="<%=btnLastDay%>"></button>
				<button type="button" name="" value="" class="button-month" onclick="chartMgr.getVisitsByResolution('day', false)" title="<%=btnLastMonth%>"></button>
				<button type="button" name="" value="" class="button-year" onclick="chartMgr.getVisitsByResolution('month', false)" title="<%=btnLastYear%>"></button>
			</div>
		</div>
	</div>

	<div id="visits-content">
		<div class="resume-block" style="display: none">
			<div class="resume-logo" id="resume-logo-total-visits"></div>
			<div class="resume-info">
				<span><%=titleTotalVisits%></span>
				<span id="total-visits"></span>
			</div>
		</div>
	
		<div class="resume-block" style="display: none">
			<div class="resume-logo" id="resume-logo-article-visits"></div>
			<div class="resume-info">
				<span><%=tooltipArticleVisits%></span>
				<span id="article-visits">0</span>
			</div>
		</div>
	
		<div class="resume-block" style="display: none">
			<div class="resume-logo" id="resume-logo-article-reads"></div>
			<div class="resume-info">
				<span><%=tooltipArticleReads%></span>
				<span id="article-reads">0</span>
			</div>
		</div>
	</div>
	
	<script type="text/javascript">
		var data = {
			pageType: "<%=pageType%>",
			groupId: <%=groupId%>,
			masIdSite: "<%=masIdSite%>",
			virtualHost: "<%=virtualHost%>",
			<% if (Validator.isNotNull(showVisitsTrend))  { %> showVisitsTrend: <%=showVisitsTrend%>, <% } %>
			<% if (Validator.isNotNull(maxSections))  { %> maxSections: <%=maxSections%>, <% } %>
			<% if (Validator.isNotNull(maxArticles))  { %> maxArticles: <%=maxArticles%>, <% } %>
			<% if (Validator.isNotNull(maxReadings))  { %> maxReadings: <%=maxReadings%>, <% } %>
			<% if (Validator.isNotNull(maxSharings))  { %> maxSharings: <%=maxSharings%>, <% } %>
			<% if (Validator.isNotNull(maxComments))  { %> maxComments: <%=maxComments%>, <% } %>
			<% if (Validator.isNotNull(maxFeedback))  { %> maxFeedback: <%=maxFeedback%>, <% } %>
			<% if (Validator.isNotNull(vocabularies)) { %> vocabularies: "<%=vocabularies%>", <% } %>
			<% if ("group".equals(pageType))           { %> showVisits: <%=showVisits%>
			<% } else if ("section".equals(pageType))  { %> itemId: <%=plid%>
			<% } else if ("metadata".equals(pageType)) { %> itemId: <%=categoryId%>
			<% } else if ("article".equals(pageType))  { %> itemId: "<%=contentId%>", contentIdCrc: "<%=contentIdCrc%>", surveys: <%=surveys.toString()%>
			<% } %>
		};
		
		var options = {
			resolution:         "<%=resolution%>",
			realTime:           "<%=realTime%>",
			<% if (Validator.isNotNull(initialDateLimit)) { %> dateLimit: "<%=initialDateLimit%>", <% } %>
			tooltipTotalVisits: "<%=tooltipTotalVisits%>",
			tooltipVisits:      "<%=tooltipArticleVisits%>",
			tooltipReads:       "<%=tooltipArticleReads%>",
			tooltipSharings:    "<%=tooltipArticleSharings%>",
			tooltipComments:    "<%=tooltipArticleComments%>",
			tooltipRate:        "<%=tooltipArticleRate%>",
			thousandSeparator:  "<%=thousandSeparator%>",
			displayedHours:      <%=displayedHours%>,
			annotations:        "<%=annotations%>",
			titles: {
				titleReadings: "<%=titleReadings%>",
				titleSocial:   "<%=titleSocialNetworks%>",
				titleComments: "<%=titleComments%>",
				titleFeedback: "<%=titleFeedback%>"
			},
			emptyVisitsMessage: "<%=emptyVisitsMessage%>",
			experimentLiterals: {
				status:      "<%=expStatus%>",
				startDate:   "<%=expStartDate%>",
				endDate:     "<%=expEndDate%>",
				pending:     "<%=expPending%>",
				runnning:    "<%=expRunning%>",
				finished:    "<%=expFinished%>",
				stop:        "<%=expStop%>",
				ctr:         "<%=expCtr%>",
				impressions: "<%=expImpressions%>",
				views:       "<%=expViews%>"
			},
			readOnly:           <%=readOnly%>,
			masUrl:             "<%=masUrl%>"
		};
	
		var chartMgr = new iterChartManager(data, options, <%=pollingRate%>, <%=refreshRate%>, <%=debug%>);
	</script>
</body>
</html>
