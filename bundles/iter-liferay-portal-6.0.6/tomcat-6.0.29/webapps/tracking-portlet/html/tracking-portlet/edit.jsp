<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ include file="init.jsp"%>

<%@page import="java.math.BigDecimal"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionMessages"%>
<%@page import="com.liferay.portlet.journal.service.persistence.JournalTemplateFinderUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.protecmedia.iter.news.service.DateCountersLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.liferay.portal.kernel.util.CalendarFactoryUtil"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.service.UserLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.OrderByComparator"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticleDisplay"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>

<%

	//Vemos si estamos en la vista de milenium
	String milenium = ParamUtil.getString(request, "milenium", "");

	String contentId = ParamUtil.get(renderRequest, "contentId", "");

	String tabs1 = ParamUtil.getString(request, "tabs1");
	
	PortletURL portletURL = renderResponse.createActionURL();		
	portletURL.setParameter("contentId", contentId);
	portletURL.setParameter("javax.portlet.action", "editTracking");
	portletURL.setParameter("milenium", milenium);

	String link = "";
	String title = "";
%>			

<c:if test='<%= milenium.equals("true") %>'>								
	<%@include file="milenium-styles.jsp"%>
</c:if>

<c:if test='<%= (!contentId.equals("")) %>'>
<%
	JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
	String names = "statistics,comments";
	long sends = 0;
	long views = 0;
	double ratings = 0;
%>		
		<c:if test="<%= (article != null) %>">
<%		
			try
			{
				title = article.getTitle();		
				
				
				if ( article.getStructureId().equals(IterKeys.STRUCTURE_POLL)) {
					names += ",votings";
				}
				
				Counters counter = CountersLocalServiceUtil.findByCountersArticleGroupOperationFinder(article.getArticleId(), scopeGroupId, IterKeys.OPERATION_RATINGS);
				if (counter != null) 
				{
					ratings = TrackingUtil.round((double)counter.getValue()/counter.getCounter(), 1);
				}

				counter = CountersLocalServiceUtil.findByCountersArticleGroupOperationFinder(article.getArticleId(), scopeGroupId, IterKeys.OPERATION_SENT);
				if (counter != null) 
				{
					sends = counter.getCounter();
				}
				
				counter = CountersLocalServiceUtil.findByCountersArticleGroupOperationFinder(article.getArticleId(), scopeGroupId, IterKeys.OPERATION_VIEW);
				if (counter != null) 
				{
					views = counter.getCounter();
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
			}
%>
			<portlet:renderURL var="redirectURL" >	
				<portlet:param name="milenium" value="<%= milenium %>" />
			</portlet:renderURL>
		
			<liferay-ui:tabs  
				names="<%= names %>"
				param="tabs1"
				url="<%= portletURL.toString() %>"
				backURL="<%= redirectURL %>"
			/>

			<div class="taglib-header "> 
				<h1 class="header-title"> 
					<%-- <span><a href='<%= link %>' target='_blank'><%= title %></a></span>--%>
					<span><%= title %></span>
				</h1>
			</div>
			
			<c:choose>
				<c:when test='<%= tabs1.equals("statistics") %>'>
					<span class="aui-field">
						<span class="aui-field-content">							
							<label class="aui-field-label"><liferay-ui:message key="tracking-edit-statistics-sends" /> <span class="sends"><%= sends %></span> <liferay-ui:message key="tracking-edit-statistics-times" /></label>
						</span>
					</span>
					<span class="aui-field">
						<span class="aui-field-content">							
							<label class="aui-field-label"><liferay-ui:message key="tracking-edit-statistics-average" /> (<span class="sends"><%= ratings %></span>)</label>
							<span><liferay-ui:ratings-score score="<%= ratings %>"/></span>
						</span>
					</span>
					
					<span class="aui-field">
						<span class="aui-field-content">							
							<label class="aui-field-label"><liferay-ui:message key="tracking-edit-statistics-views" /> <span class="sends"><%= views %></span></label>
						</span>
					</span>
					
					
				<%
					int hours = 12;
					List<Object[]> resultLastNHours = DateCountersLocalServiceUtil.findDateCountersLastHours(article.getArticleId(), scopeGroupId, IterKeys.OPERATION_VIEW, 0, hours);		

					String resultHours = "";
					Object[] objStatsTmp = resultLastNHours.get(0);       
				    resultHours += "{ hour: '" +
				    	String.valueOf(objStatsTmp[0]) + "h', visits: " + String.valueOf(objStatsTmp[1]) + "}";
					for (int indexStat = 0; indexStat < resultLastNHours.size(); indexStat++) {
						Object[] objStats = resultLastNHours.get(indexStat);       
				        resultHours += ",\n{ hour: '" + String.valueOf(objStats[0]) + "h', visits: " + String.valueOf(objStats[1]) + "}";
					}	
	
				%>
					
					<span class="aui-field">
						<span class="aui-field-content">							
							<label class="aui-field-label"><liferay-ui:message key="tracking-edit-statistics-chart" /></label>
							<div class="chart" id="lastHoursChart"></div>
						</span>
					</span>										
																	
					<c:if test="<%= (resultLastNHours.size() > 0) %>">
	
						<script type="text/javascript">
					
							YAHOO.widget.Chart.SWFURL = "/tracking-portlet/swf/charts.swf";
						
						//--- data
						
							hourlyExpenses = [<%= resultHours %>];
						
							var hourlyDataSource = new YAHOO.util.DataSource( hourlyExpenses );
							hourlyDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
							hourlyDataSource.responseSchema =
							{
								fields: [ "hour", "visits" ]
							};
						
						//--- chart
						
							var seriesHourlyDef =
							[
								{ displayName: "visits", yField: "visits" }
							];
						
							getHourlyDataTipText = function( item, index, series )
							{
								var toolTipText = series.displayName + " for " + item.hour;
								toolTipText += "\n" + item[series.yField];
								return toolTipText;
							}
						
							var currencyHourlyAxis = new YAHOO.widget.NumericAxis();
							currencyHourlyAxis.minimum = 0;
						
							var hourchart = new YAHOO.widget.LineChart( "lastHoursChart", hourlyDataSource,
							{
								series: seriesHourlyDef,
								xField: "hour",
								yAxis: currencyHourlyAxis,
								dataTipFunction: getHourlyDataTipText,
								wmode: 'transparent'
							});
						
						</script>
					</c:if>
				</c:when>
				
				<c:when test='<%= tabs1.equals("comments") %>'>
				
					<%
						DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
					
						String keyword = ParamUtil.get(renderRequest, "keyword", "");
					 	String moderate = ParamUtil.get(renderRequest, "pendingModeration", "");
					 	Date start = ParamUtil.getDate(renderRequest, "startDate", df, null);
					 	Date end = ParamUtil.getDate(renderRequest, "endDate", df, null);
					 	String screenName = ParamUtil.get(renderRequest, "screenName", "");
					 	String emailAddress = ParamUtil.get(renderRequest, "emailAddress", "");					
					 	
						PortletURL filterURL = renderResponse.createActionURL();
						filterURL.setParameter("javax.portlet.action", "filterCommentsTracking");
						filterURL.setParameter("contentId", article.getArticleId());					
						filterURL.setParameter("tabs1", "comments");
						filterURL.setParameter("milenium", milenium);
												
					%>
					<liferay-portlet:renderURL varImpl="iteratorURL">
						<liferay-portlet:param name="view" value="filter" />
						<liferay-portlet:param name="tabs1" value="comments" />
						<liferay-portlet:param name="contentId" value="<%= article.getArticleId() %>" />
						<liferay-portlet:param name="keyword" value="<%= keyword %>" />
						<liferay-portlet:param name="pendingModeration" value="<%= moderate %>" />
						<liferay-portlet:param name="startDate" value="<%= (start!= null) ? df.format(start) : null %>" />
						<liferay-portlet:param name="endDate" value="<%= (end != null) ? df.format(end) : null %>" />
						<liferay-portlet:param name="screenName" value="<%= screenName %>" />
						<liferay-portlet:param name="emailAddress" value="<%= emailAddress %>" />
						<liferay-portlet:param name="milenium" value="<%= milenium %>" />
					</liferay-portlet:renderURL>
				
					<%-- *************************************** --%>
					<%-- *    Filter                           * --%>
					<%-- *************************************** --%>	
					<%
							Calendar endDate = CalendarFactoryUtil.getCalendar(timeZone, locale);
							if (end != null) {
								endDate.setTime(end);
							}
							
							Calendar startDate = CalendarFactoryUtil.getCalendar(timeZone, locale);
							startDate.add(Calendar.DAY_OF_MONTH, -1);
							if (start != null) {
								startDate.setTime(start);
							}
				
							// Start Date
							int monthStartDate = startDate.get(Calendar.MONTH);						
							
							int dayStartDate = startDate.get(Calendar.DATE);						
				
							int yearStartDate = startDate.get(Calendar.YEAR);									
							int yearRangeStartStartDate = yearStartDate - 5;
							int yearRangeEndStartDate = yearStartDate + 5;
				
							int firstDayOfWeekStartDate = startDate.getFirstDayOfWeek() - 1;			
				
							// End Date
							int monthEndDate = endDate.get(Calendar.MONTH);						
							
							int dayEndDate = endDate.get(Calendar.DATE);						
				
							int yearEndDate = endDate.get(Calendar.YEAR);									
							int yearRangeStartEndDate = yearStartDate - 5;
							int yearRangeEndEndDate = yearStartDate + 5;
				
							int firstDayOfWeekEndDate = endDate.getFirstDayOfWeek() - 1;			
										
							// Show Time 
							String timeFormatPattern = ((SimpleDateFormat)(DateFormat.getTimeInstance(DateFormat.SHORT, locale))).toPattern();
				
							boolean timeFormatAmPm = true;
				
							if (timeFormatPattern.indexOf("a") == -1) {
								timeFormatAmPm = false;
							}
							
							int startHour = startDate.get(Calendar.HOUR_OF_DAY);
							int endHour = endDate.get(Calendar.HOUR_OF_DAY);
				
							if (timeFormatAmPm) {
								startHour = endDate.get(Calendar.HOUR);
								endHour = endDate.get(Calendar.HOUR);
							}
				
							int startMinute = startDate.get(Calendar.MINUTE);
							int endMinute = endDate.get(Calendar.MINUTE);
							
							int amPm = Calendar.AM;
							if (timeFormatAmPm) {
								amPm = endDate.get(Calendar.AM_PM);
							}
							


						%>			
					<aui:form name="fm" method="post" action="<%= filterURL %>" cssClass="filter-form">
						<aui:input type="text" name="keyword" label="tracking-edit-filter-text" value="<%= keyword %>" />
						
						<aui:fieldset cssClass="user-field">
							<aui:column columnWidth="50">
								<aui:input type="text" name="screenName" label="tracking-edit-filter-screen-name" value="<%= screenName %>" />
							</aui:column>
							
							<aui:column columnWidth="50">
								<aui:input type="text" name="emailAddress" label="tracking-edit-filter-email-address" value="<%= emailAddress %>" />
							</aui:column>
						</aui:fieldset>
						
						<aui:select name="pendingModeration" label="tracking-edit-filter-pending-moderation">
							<aui:option selected='<%= moderate.equals("") %>' label="tracking-edit-filter-all" value="" />
							<aui:option selected='<%= moderate.equals("true") %>' label="tracking-edit-filter-true" value="true" />
							<aui:option selected='<%= moderate.equals("false") %>' label="tracking-edit-filter-false" value="false" />
						</aui:select>
						
						
				
						<span class="aui-field aui-field-date">
							<span class="aui-field-content">							
								<label class="aui-field-label"><liferay-ui:message key="tracking-edit-filter-start-date" /></label>
								<span class="aui-field-element">						
									<liferay-ui:input-date
										dayParam="dateStartDay"
										dayValue="<%= dayStartDate %>"
										firstDayOfWeek="<%= firstDayOfWeekStartDate %>"
										formName="fm1"			
										monthParam="dateStartMonth"
										monthValue="<%= monthStartDate %>"
										yearParam="dateStartYear"
										yearValue="<%= yearStartDate %>"
										yearRangeStart="<%= yearRangeStartStartDate %>"
										yearRangeEnd="<%= yearRangeEndStartDate %>"
									/>		
						
									<liferay-ui:input-time
										amPmParam="dateStartAmPm"
										amPmValue="<%= amPm %>"						
										hourParam="dateStartHour"
										hourValue="<%= startHour %>"
										minuteParam="dateStartMinute"
										minuteValue="<%= startMinute %>"				
									/>
								</span>
							</span>
						</span>		
						
						<span class="aui-field aui-field-date">
							<span class="aui-field-content">
								<label class="aui-field-label"><liferay-ui:message key="tracking-edit-filter-end-date" /></label>
								<span class="aui-field-element">
									<liferay-ui:input-date
										dayParam="dateEndDay"
										dayValue="<%= dayEndDate %>"
										firstDayOfWeek="<%= firstDayOfWeekEndDate %>"
										formName="fm1"			
										monthParam="dateEndMonth"
										monthValue="<%= monthEndDate %>"
										yearParam="dateEndYear"
										yearValue="<%= yearEndDate %>"
										yearRangeStart="<%= yearRangeStartEndDate %>"
										yearRangeEnd="<%= yearRangeEndEndDate %>"
									/>
						
									<liferay-ui:input-time
										amPmParam="dateEndAmPm"
										amPmValue="<%= amPm %>"						
										hourParam="dateEndHour"
										hourValue="<%= endHour %>"
										minuteParam="dateEndMinute"
										minuteValue="<%= endMinute %>"				
									/>
								</span>
							</span>
						</span>
						
						<aui:button-row>
							<aui:button type="submit" value="tracking-view-filter" />
						</aui:button-row>
						
					</aui:form>
					
					<div class="separator article-separator"></div>
					
					<%-- *************************************** --%>
					<%-- *    Comments List                    * --%>
					<%-- *************************************** --%>
					<liferay-ui:search-container emptyResultsMessage="tracking-view-empty-result" iteratorURL="<%= iteratorURL %>" >
						<liferay-ui:search-container-results >
							<%
							results = new ArrayList<Comments>();
							total = 0;
							List<Object> usersOBj = new ArrayList<Object>();
							User user_ = null;
							try{
								//1. Compruebo si ha introducido un email
								if (!emailAddress.equalsIgnoreCase("")){
									user_ = UserLocalServiceUtil.getUserByEmailAddress(company.getCompanyId(),emailAddress);
								} 
								//2. Compruebo si ha introducido un nombre de usuario
								if ((user_ == null) && (!screenName.equalsIgnoreCase(""))){
									user_ = UserLocalServiceUtil.getUserByScreenName(company.getCompanyId(),screenName);
								}
								if (user_ != null){
									usersOBj.add(user_.getUserId());
								} 
								results = CommentsLocalServiceUtil.getCommentsSearchTracking(globalGroupId, contentId, keyword, 
														moderate, start, end, 
														usersOBj, "publicationDate", "asc", 
														com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS,
														com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS);
								total = results.size();
							}catch(Exception e){
								results = new ArrayList<Comments>();
								total = 0;
							} 
							
							pageContext.setAttribute("results", results);
							pageContext.setAttribute("total", total);
							%>
						</liferay-ui:search-container-results>
						
						<liferay-ui:search-container-row className="com.protecmedia.iter.news.model.Comments" modelVar="comment" >	
							<liferay-ui:search-container-column-text title="" name="tracking-edit-comment-message" value="<%= comment.getMessage() %>" />
							<liferay-ui:search-container-column-text title="" name="tracking-edit-comment-date" value="<%= df.format(comment.getPublicationDate()) %>" />
							<liferay-ui:search-container-column-text title="" name="tracking-edit-comment-name" value="<%= comment.getUserName() %>" />
							<liferay-ui:search-container-column-text title="" name="tracking-edit-comment-state" value="<%= String.valueOf(comment.getModerated()) %>" />
							<liferay-ui:search-container-column-text title="" name="tracking-edit-comment-activated">
								<%
									String cssClass = "deactivate";
									if (comment.getActive()) {
										cssClass = "activate";
									}
								%>						
								<div class="<%= cssClass %>">
									<span><%= comment.getActive() %></span>
								</div>	
							</liferay-ui:search-container-column-text>
							
							<liferay-ui:search-container-column-jsp align="right" path="/html/tracking-portlet/edit_actions.jsp" />															
						</liferay-ui:search-container-row>
							
						<liferay-ui:search-iterator  />
					</liferay-ui:search-container>
				
				
				</c:when>
				<c:when test='<%= tabs1.equals("votings") %>'>
					<%
						try
						{
							String viewMode = "";						
							String html = "";
							String xmlRequest = PortletRequestUtil.toXML(renderRequest, renderResponse);
							String pollTemplate = article.getTemplateId();
							JournalArticleDisplay articleDisplay = JournalArticleLocalServiceUtil.getArticleDisplay(globalGroupId, 
									article.getArticleId(), pollTemplate, viewMode, themeDisplay.getLanguageId(), themeDisplay);
							if(articleDisplay != null)
							{
								html =  articleDisplay.getContent();
							}
  					 %>
  					 <%= html %>
  					 <%
						}catch(Exception e){
							System.out.println(e.getMessage());
						}
  					 %>
				</c:when>
			</c:choose>
			
		</c:if>
</c:if>
