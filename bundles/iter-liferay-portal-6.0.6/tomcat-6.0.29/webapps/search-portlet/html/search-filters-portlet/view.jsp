<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.protecmedia.iter.search.util.SearchOptions"%>
<%@ include file="init.jsp" %> 

<%
	//Estructuras
	boolean checkArticle = ParamUtil.getBoolean(renderRequest, "checkArticle", false);
	boolean checkPoll = ParamUtil.getBoolean(renderRequest, "checkPoll", false);
	
	//Fechas
	DateFormat df = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT);

	String startDate = ParamUtil.getString(renderRequest, "startDate");
	String endDate = ParamUtil.getString(renderRequest, "endDate");

	try
	{
		df.parse(endDate);
	}
	catch (Exception e)
	{
		Calendar cal = Calendar.getInstance();	
		cal.setTime(new Date());
		endDate = df.format(cal.getTime());
		
		cal.add(Calendar.YEAR, 1970);
		startDate = df.format(cal.getTime());
	}
	
	try
	{
		df.parse(startDate);
	}
	catch (Exception e)
	{
		Calendar cal = Calendar.getInstance();	
		cal.setTime(new Date());
		endDate = df.format(cal.getTime());
		
		cal.add(Calendar.YEAR, 1970);
		startDate = df.format(cal.getTime());
	}
	
	//Ordenacion
	String order = ParamUtil.getString(renderRequest, "order", SearchUtil.SEARCH_ORDER_DEFAULT);
	
	//Categories
	List<Long> listCategoryIds = CategoriesUtil.getCategoriesIdsLong(ParamUtil.getString(renderRequest, "categoryIds", ""));
%>
<span class="iter-field iter-field-text iter-field-inline">
	<span class="iter-field-content" >
		<span class="iter-field-element ">
			<input class="iter-field-input iter-field-input-text" id="<portlet:namespace />keywords" name="<portlet:namespace />keywords" type="text" value onkeydown="javascript:<portlet:namespace />onKeyEnterFilterSearch(event)"/>
		</span>
	</span>
</span>
<span class="iter-button iter-button-submit">
	<span class="iter-button-content ">
		<input class="iter-button-input iter-button-input-submit" id="<portlet:namespace />search" name="<portlet:namespace />search" type="submit" value="<%=searchLabel %>" onclick="javascript:<portlet:namespace />onClickFilterSearch()"/>
	</span>
</span>

<script type="text/javascript">
function getFormattedNumber(stringNumber, increment)
{
	var number = parseInt(stringNumber);
	var formattedNumber = number;

	if(increment)
		formattedNumber = formattedNumber + 1;

	if(formattedNumber < 10)
		formattedNumber = '0' + formattedNumber;
		
	return formattedNumber;
}

</script>
	<%
		for (int a=0;a<filterIds.length;a++)
		{
	%>
	<div class="iter-widget iter-component iter-panel">
		<div class="iter-panel-content iter-widget-stdmod">
<!-- 			<div class="iter-widget-hd iter-helper-clearfix iter-panel-hd"> </div>-->
			<div class="iter-widget-bd iter-panel-bd">
				<div>
	<%	
			switch (Integer.valueOf(filterIds[a]))
			{ 
				case 0:
	%>
	
				<fieldset class="iter-fieldset"> 
					<div class="iter-fieldset-content "> 
						<span class="iter-field iter-field-choice">
							<span class="iter-field-content"> 
								<span class="iter-field-element iter-field-label-right"> 
									<input <%if(checkArticle) {%> checked <%} %> class="iter-field-input iter-field-input-choice" id="<portlet:namespace />checkArticleCheckbox" name="<portlet:namespace />checkArticleCheckbox" type="checkbox"> 
								</span> 
								<label class="iter-field-label" for="<portlet:namespace />checkArticleCheckbox"> <%= articlesLabel %> </label> 
							</span> 
						</span> 
						<span class="iter-field iter-field-choice">
							<span class="iter-field-content">
								<span class="iter-field-element iter-field-label-right">
									<input <%if(checkPoll) {%> checked <%} %> class="iter-field-input iter-field-input-choice" id="<portlet:namespace />checkPollCheckbox" name="<portlet:namespace />checkPollCheckbox" type="checkbox"> 
								</span> 
								<label class="iter-field-label" for="<portlet:namespace />checkPollCheckbox"> <%= pollsLabel %> </label>
							</span>
						</span> 

	<%			
					break;
				case 1: 
					Calendar defaultEndDate = Calendar.getInstance();
					defaultEndDate.setTime(df.parse(endDate));
					Calendar defaultStartDate = Calendar.getInstance();
					defaultStartDate.setTime(df.parse(startDate));
					String[] months = new DateFormatSymbols(new Locale(customLanguage)).getMonths();
					
					Calendar today = Calendar.getInstance();
					int YearValue = today.get(Calendar.YEAR);
					int MonthValue = today.get(Calendar.MONTH)+1;
					int DayValue = today.get(Calendar.DAY_OF_MONTH);
	%>
			<div class="iter-field iter-field-wrapper"> 
					<div class="iter-field-wrapper-content"> 
						<label class="iter-field-label"> <%= fromLabel %> </label> 
						<div class="iter-datepicker iter-datepicker-display iter-helper-clearfix  iter-widget iter-component" id="ixfm_displayDate">
							<div class="iter-datepicker-content iter-datepicker-display-content" id="ixfm_displayDateContent"> 
								<div id="<portlet:namespace />start" class="iter-datepicker-select-wrapper"> 
									<select id="<portlet:namespace />startDay" name="<portlet:namespace />startDay" class="iter-datepicker-day">
										<%
											int startDayValue = defaultStartDate.get(Calendar.DAY_OF_MONTH);
											int startDaysOfmonth = new GregorianCalendar().getMaximum(Calendar.DAY_OF_MONTH);
											for (int i = 1; i <= startDaysOfmonth; i++) {
										%>
											<option <%= (startDayValue == i) ? "selected" : "" %> value="<%= i %>"><%= i %></option>
										<%
											}
										%>
									 </select> 
									
									<select id="<portlet:namespace />startMonth" name="<portlet:namespace />startMonth" class="iter-datepicker-month">
										<%
											int startMonthValue = defaultStartDate.get(Calendar.MONTH);
											for (int i = 0; i < months.length && i<12; i++) {
										%>
											<option <%= (startMonthValue == i) ? "selected" : "" %> value="<%= i+1 %>"><%= months[i] %></option>
										<%
											}
										%>
									</select> 
									
									<select id="<portlet:namespace />startYear" name="<portlet:namespace />startYear" class="iter-datepicker-year">
									    <%
									    	int startYearValue = defaultStartDate.get(Calendar.YEAR);
											for (int i = 1970; i <= YearValue; i++) {
										%>
											<option <%= (startYearValue == i) ? "selected" : "" %> value="<%= i %>"><%= i %></option>
										<%
											}
										%>
									</select> 
								</div>
								<div class="iter-datepicker-button-wrapper">
									<button class="iter-buttonitem iter-buttonitem-content iter-buttonitem-icon-only iter-component iter-state-default iter-widget iter-buttonitem-focused" 
										id="buttonStartDate" 
										type="button" title="" onclick=" jQryIter('#selectedStartDatepicker').click()">
										  <span class="iter-buttonitem-icon iter-icon iter-icon-calendar"></span>
										  <span class="iter-buttonitem-label iter-helper-hidden" ></span>
									</button>
								</div>
								<div id = "startdate-pick" style='display:none'></div>
							</div>
						</div>
					</div>
					
				</div>
				<script type="text/javascript">
				
				var maxDate = <%= MonthValue %> +'-'+ <%= DayValue %> +'-'+ <%= YearValue %>;
				var minDate = '01-01-1970';
				
				jQryIter(document).ready(function(){	
					jQryIter("#buttonStartDate").click(function(){
						jQryIter("#startdate-pick").toggle();
					}); 
					
					jQryIter("#startdate-pick").css("position", "absolute");
					jQryIter("#startdate-pick").css("z-index", "1000");
					jQryIter("#startdate-pick").css("top", jQryIter('#buttonStartDate').position().top + jQryIter('#buttonStartDate').height());
					jQryIter("#startdate-pick").css("left", jQryIter('#buttonStartDate').position().left);
					
					jQryIter("#startdate-pick").datepicker( 
							jQryIter.extend(
									{
										showMonthAfterYear: false,
										showWeek: false,
										createButton:false,
										showOtherMonths: false,
										dateFormat: "mm-dd-yy",
										defaultDate: getFormattedNumber(jQryIter('#<portlet:namespace />startMonth').val(), false)+'-'+getFormattedNumber(jQryIter('#<portlet:namespace />startDay').val(), false)+'-'+jQryIter('#<portlet:namespace />startYear').val(),
										minDate: minDate, 
										maxDate: maxDate,
										// associate the link with a date picker
										onSelect: function(date) {
											updateStartSelects(date);
											jQryIter("#startdate-pick").toggle();
										},
									},
									jQryIter.datepicker.regional['<%= customLanguage %>']
							),
					{
					}).hover( 
							function() {
								buttonStartDateOver = true;
							  }, function() {
								  buttonStartDateOver = false;
								  jQryIter("#buttonStartDate").focus();
							  }
					);

					var buttonStartDateOver = false;
					jQryIter("#buttonStartDate").focusout(function(){
						if(!buttonStartDateOver){
							jQryIter("#startdate-pick").hide();
						}

					});
					
					
					
					var updateStartSelects = function (selectedDateStr)
					{
						var selectedDate = new Date(selectedDateStr.split("-")[2], selectedDateStr.split("-")[0]-1, selectedDateStr.split("-")[1] );
						selectOptionStartDayAndUpdate(selectedDate);
						jQryIter('#<portlet:namespace />startMonth option[value=' + (selectedDate.getMonth()+1) + ']').attr('selected', 'selected');
						jQryIter('#<portlet:namespace />startYear option[value=' + selectedDate.getFullYear() + ']').attr('selected', 'selected');
					};
			
					jQryIter('#<portlet:namespace />startDay, #<portlet:namespace />startMonth, #<portlet:namespace />startYear')
						.bind('change',
						function()
						{							
							var month = getFormattedNumber(jQryIter('#<portlet:namespace />startMonth').val(), false);
							var day = getFormattedNumber(jQryIter('#<portlet:namespace />startDay').val(), false);
							var year = jQryIter('#<portlet:namespace />startYear').val();
							
							var maxDay = new Date(year, month, 0).getDate();
							if(day > maxDay){
								day = maxDay;
							}
							
							jQryIter('#startdate-pick').datepicker( "setDate" , month + '-' + day + '-' + year);
							jQryIter('#startdate-pick').datepicker( "refresh" );
							
							selectOptionStartDayAndUpdate(new Date(year, (month-1), day));
						}
					);
					
					var selectOptionStartDayAndUpdate = function (selectedDate){
						var year = selectedDate.getFullYear();
						var month = selectedDate.getMonth();

						var day = selectedDate.getDate();
						var maxDay = new Date(year, month+1, 0).getDate();
						if(day > maxDay){
							day = maxDay;
						}
						
						var childs = "";
						
						for(var i = 1; i <= maxDay ; i++)
						{
							if(day == i){
								childs += "<option selected value='" + i + "'>" + i + "</option>";
							}else{
								childs += "<option value='" + i + "'>" + i + "</option>";
							}
													
						}
						jQryIter('#<portlet:namespace />startDay').empty();
						jQryIter('#<portlet:namespace />startDay').append( jQryIter.parseHTML( childs ) );
					};
				});
				</script>
				
				<div class="iter-field iter-field-wrapper"> 
					<div class="iter-field-wrapper-content"> 
						<label class="iter-field-label"> <%= toLabel %> </label> 
						<div class="iter-datepicker iter-datepicker-display iter-helper-clearfix  iter-widget iter-component" id="ixfm_displayDate">
							<div class="iter-datepicker-content iter-datepicker-display-content" id="ixfm_displayDateContent"> 
								<div id="<portlet:namespace />end" class="iter-datepicker-select-wrapper"> 
									
									<select id="<portlet:namespace />endDay" name="<portlet:namespace />endDay" class="iter-datepicker-day">
										<%
											int endDayValue = defaultEndDate.get(Calendar.DAY_OF_MONTH);
											int endDaysOfmonth = new GregorianCalendar().getMaximum(Calendar.DAY_OF_MONTH);
											for (int i = 1; i <= endDaysOfmonth; i++) {
										%>
											<option <%= (endDayValue == i) ? "selected" : "" %> value="<%= i %>"><%= i %></option>
										<%
											}
										%>
									 </select> 
									 
									<select id="<portlet:namespace />endMonth" name="<portlet:namespace />endMonth" class="iter-datepicker-month">
										<%
											int endMonthValue = defaultEndDate.get(Calendar.MONTH);
											for (int i = 0; i < months.length && i<12; i++) {
										%>
											<option <%= (endMonthValue == i) ? "selected" : "" %> value="<%= i+1 %>"><%= months[i] %></option>
										<%
											}
										%>
									</select> 
									
									<select id="<portlet:namespace />endYear" name="<portlet:namespace />endYear" class="iter-datepicker-year">
									    <%
									    	int endYearValue = defaultEndDate.get(Calendar.YEAR);
											for (int i = 1970; i <= YearValue; i++) {
										%>
											<option <%= (endYearValue == i) ? "selected" : "" %> value="<%= i %>"><%= i %></option>
										<%
											}
										%>
									</select> 
								</div>
								<div class="iter-datepicker-button-wrapper">
									<button class="iter-buttonitem iter-buttonitem-content iter-buttonitem-icon-only iter-component iter-state-default iter-widget iter-buttonitem-focused" 
										id="buttonEndDate" 
										type="button" title="" onclick=" jQryIter('#selectedEndDatepicker').click()">
										  <span class="iter-buttonitem-icon iter-icon iter-icon-calendar"></span>
										  <span class="iter-buttonitem-label iter-helper-hidden" ></span>
										  
									</button>
								</div>
								<div id = "enddate-pick" style='display:none'></div>
							</div>
						</div>
					</div>
					
				</div>
				<script type="text/javascript">
				var maxDate = <%= MonthValue %> +'-'+ <%= DayValue %> +'-'+ <%= YearValue %>;
				
				jQryIter(document).ready(function(){	
					
					jQryIter("#buttonEndDate").click(function(){					
						jQryIter("#enddate-pick").toggle();
					}); 
					
					jQryIter("#enddate-pick").css("position", "absolute");
					jQryIter("#enddate-pick").css("z-index", "1000");
					jQryIter("#enddate-pick").css("top", jQryIter('#buttonEndDate').position().top + jQryIter('#buttonEndDate').height());
					jQryIter("#enddate-pick").css("left", jQryIter('#buttonEndDate').position().left);
										
					
					jQryIter("#enddate-pick").datepicker(
						jQryIter.extend(
						{	
							// associate the link with a date picker
							showMonthAfterYear: false,
							showWeek: false,
							createButton:false,
							showOtherMonths: false,
							dateFormat: "mm-dd-yy",
							defaultDate: getFormattedNumber(jQryIter('#<portlet:namespace />endMonth').val(), false)+'-'+getFormattedNumber(jQryIter('#<portlet:namespace />endDay').val(), false)+'-'+jQryIter('#<portlet:namespace />endYear').val(),
							minDate: minDate, 
							maxDate: maxDate,
							onSelect: function(date) {
								updateEndSelects(date);
								jQryIter("#enddate-pick").toggle();
							},
						},
						jQryIter.datepicker.regional['<%= customLanguage %>']
						),{
						}).hover( 
							function() {
								buttonEndDateOver = true;
							}, 
							function() {
								buttonEndDateOver = false;
								jQryIter("#buttonEndDate").focus();
							}
						);
						
					var buttonEndDateOver = false;
					jQryIter("#buttonEndDate").focusout(function(){
						if(!buttonEndDateOver){
							jQryIter("#enddate-pick").hide();
						}

					});
				
					var updateEndSelects = function (selectedDateStr)
					{
						var selectedDate = new Date(selectedDateStr.split("-")[2], selectedDateStr.split("-")[0]-1, selectedDateStr.split("-")[1] );
// 						jQryIter('#<portlet:namespace />endDay option[value=' + selectedDate.getDate() + ']').attr('selected', 'selected');
						selectOptionEndDayAndUpdate(selectedDate);
						jQryIter('#<portlet:namespace />endMonth option[value=' + (selectedDate.getMonth()+1) + ']').attr('selected', 'selected');
						jQryIter('#<portlet:namespace />endYear option[value=' + selectedDate.getFullYear() + ']').attr('selected', 'selected');
					};
			
					jQryIter('#<portlet:namespace />endDay, #<portlet:namespace />endMonth, #<portlet:namespace />endYear')
						.bind('change',
						function()
						{							
							var month = getFormattedNumber(jQryIter('#<portlet:namespace />endMonth').val(), false);
							var day = getFormattedNumber(jQryIter('#<portlet:namespace />endDay').val(), false);
							var year = jQryIter('#<portlet:namespace />endYear').val();
							
							var maxDay = new Date(year, month, 0).getDate();
							if(day > maxDay){
								day = maxDay;
							}
							
							jQryIter('#enddate-pick').datepicker( "setDate", month+'-'+day+'-'+year);
							jQryIter('#enddate-pick').datepicker( "refresh" );
							

							
							selectOptionEndDayAndUpdate(new Date(year, (month-1), day));
						}
					);
					
					var selectOptionEndDayAndUpdate = function (selectedDateStr){
						var selectedDate = new Date(selectedDateStr);
						
						var year = selectedDate.getFullYear();
						var month = selectedDate.getMonth();

						var day = selectedDate.getDate();
						var maxDay = new Date(year, month+1, 0).getDate();
						if(day > maxDay){
							day = maxDay;
						}
						var childs = "";
						
						for(var i = 1; i <= maxDay ; i++)
						{
							if(day == i ){
								childs += "<option selected value='" + i + "'>" + i + "</option>";
							}else{
								childs += "<option value='" + i + "'>" + i + "</option>";
							}
													
						}
						jQryIter('#<portlet:namespace />endDay').empty();
						jQryIter('#<portlet:namespace />endDay').append( jQryIter.parseHTML( childs ) );
					};	
				});
				</script>
			
				
	<%				break;
				case 2:
	%>
					<fieldset class="iter-fieldset display-settings "> 
						<div class="iter-fieldset-content "> 
							<%@ include file="tree.jsp" %> 
						</div>
					</fieldset>
	<%				break;
			}
	%>
	</div></div></div></div>
	<%	
		}
	%>

<c:if test="<%= displaySettings %>">

	<fieldset class="iter-fieldset display-settings "> 
		<div class="iter-fieldset-content "> 
			<span class="iter-field iter-field-select iter-field-menu"> 
				<span class="iter-field-content"> 
					<label class="iter-field-label" for="<portlet:namespace />order"> <%= orderByLabel %> </label> 
					<span class="iter-field-element "> 
						<select class="iter-field-input iter-field-input-select iter-field-input-menu" id="<portlet:namespace />order" name="<portlet:namespace />order"> 
							<option <% if(order.equals(SearchOptions.ORDER_BY_RELEVANCE)){ %> selected<% } %> value="<%= SearchOptions.ORDER_BY_RELEVANCE %>"> <%= relevanceLabel %> </option>
							<option <% if(order.equals(SearchOptions.ORDER_BY_TITLE)){ %> selected<% } %> value="<%= SearchOptions.ORDER_BY_TITLE %>"> <%= titleLabel %> </option> 
							<option <% if(order.equals(SearchOptions.ORDER_BY_DATE)){ %> selected <% } %>  value="<%= SearchOptions.ORDER_BY_DATE %>"> <%= dateLabel %> </option>
							<option <% if(order.equals(SearchOptions.ORDER_BY_VIEWS)){ %> selected <% } %>  value="<%= SearchOptions.ORDER_BY_VIEWS %>"> <%= viewsLabel %> </option>
						 </select>
					</span>
				</span>
			</span>
		</div>
	</fieldset>

</c:if>

<script type="text/javascript">

<%@ include file="../commons/javascripts.jsp" %>

var categoriesList = new Array();

<%
	for(Long currentCategoryId:listCategoryIds)
	{
%>
		var currentId = '<%=currentCategoryId.toString() %>';
		if(currentId && currentId != '0' && currentId != '-1')
			categoriesList.push(currentId);
<%
	}
%>

var layoutsList = new Array();

<%
	for(String currentLayoutId:layoutsPlid)
	{
%>
		var currentId = '<%=currentLayoutId %>';
		if(currentId && currentId != '0' && currentId != '-1')
			layoutsList.push(currentId);
<%
	}
%>

function <portlet:namespace />onClickFilterSearch()
{
<%
	if(Validator.isNotNull(resultsLayoutURL))
	{
%>
		var keywords = <portlet:namespace />cleanKeywords(jQryIter("#<portlet:namespace />keywords").val());
		if(keywords.length > 0)
		{
			//Dates
			var startDate = '<%=startDate %>';
			var endDate =  '<%= endDate%>';
			
			//Order
			var order = '<%= order%>';
			
			//Structures
			var checkArticle 	= '<%= checkArticle%>';
			var checkPoll		= '<%= checkPoll%>';
			
			
			
			if(jQryIter("#<portlet:namespace />startYear").val())
			{
				startDate = jQryIter("#<portlet:namespace />startYear").val() + getFormattedNumber(jQryIter("#<portlet:namespace />startMonth").val(), false) + getFormattedNumber(jQryIter("#<portlet:namespace />startDay").val(), false);
			}
			
			if(jQryIter("#<portlet:namespace />endYear").val())
			{
				endDate = jQryIter("#<portlet:namespace />endYear").val() + getFormattedNumber(jQryIter("#<portlet:namespace />endMonth").val(), false) + getFormattedNumber(jQryIter("#<portlet:namespace />endDay").val(), false);
			}
			
			if(jQryIter("#<portlet:namespace />order").val())
			{
				order = jQryIter("#<portlet:namespace />order").val();
			}
			
			if(jQryIter("#<portlet:namespace />checkArticleCheckbox").val())
			{
				checkArticle = jQryIter("#<portlet:namespace />checkArticleCheckbox").is(":checked");
			}

			if(jQryIter("#<portlet:namespace />checkPollCheckbox").val())
			{
				checkPoll = jQryIter("#<portlet:namespace />checkPollCheckbox").is(":checked");
			}
			
			var resultsSearchURL = '<%= resultsLayoutURL %>';
			resultsSearchURL += keywords;
			resultsSearchURL += '/false' + '/<%=showFuzzyButton %>';
			resultsSearchURL += '/' + startDate + '/' + endDate;
			resultsSearchURL += '/' + order;
			resultsSearchURL += '/' + checkArticle + '/' + checkPoll;
			resultsSearchURL += '/' + 0 + '/' + 0;
			resultsSearchURL += '/meta/';

			if(categoriesList && categoriesList.length > 0)
				resultsSearchURL += categoriesList.join('-');
			else
				resultsSearchURL += '0';
			
			resultsSearchURL += '/';
			
			if(layoutsList && layoutsList.length > 0)
				resultsSearchURL += layoutsList.join('-');
			else
				resultsSearchURL += '0';

			resultsSearchURL += '/0';
			resultsSearchURL += '/1';
			
			window.location.href = resultsSearchURL;
		}
<%
	}
%>

}

function <portlet:namespace />onKeyEnterFilterSearch(event)
{
	if(event.keyCode==13)
		<portlet:namespace />onClickFilterSearch();
}


</script>