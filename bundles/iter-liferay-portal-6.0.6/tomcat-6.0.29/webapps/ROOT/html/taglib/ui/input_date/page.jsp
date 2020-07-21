<%
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
%>
<%@ page import="java.util.Locale" %>
<%@ include file="/html/taglib/init.jsp" %>

<%
String randomNamespace = PortalUtil.generateRandomKey(request, "taglib_ui_input_date_page") + StringPool.UNDERLINE;

if (GetterUtil.getBoolean((String)request.getAttribute("liferay-ui:input-date:disableNamespace"))) {
	namespace = StringPool.BLANK;
}

String customLanguage = GetterUtil.getString((String)request.getAttribute("liferay-ui:input-date:customLanguage"));
String cssClass = GetterUtil.getString((String)request.getAttribute("liferay-ui:input-date:cssClass"));
String formName = namespace + request.getAttribute("liferay-ui:input-date:formName");
String monthParam = namespace + request.getAttribute("liferay-ui:input-date:monthParam");
int monthValue = GetterUtil.getInteger((String)request.getAttribute("liferay-ui:input-date:monthValue"));
boolean monthNullable = GetterUtil.getBoolean((String)request.getAttribute("liferay-ui:input-date:monthNullable"));
String dayParam = namespace + request.getAttribute("liferay-ui:input-date:dayParam");
int dayValue = GetterUtil.getInteger((String)request.getAttribute("liferay-ui:input-date:dayValue"));
boolean dayNullable = GetterUtil.getBoolean((String)request.getAttribute("liferay-ui:input-date:dayNullable"));
String yearParam = namespace + request.getAttribute("liferay-ui:input-date:yearParam");
int yearValue = GetterUtil.getInteger((String)request.getAttribute("liferay-ui:input-date:yearValue"));
boolean yearNullable = GetterUtil.getBoolean((String)request.getAttribute("liferay-ui:input-date:yearNullable"));
int yearRangeStart = GetterUtil.getInteger((String)request.getAttribute("liferay-ui:input-date:yearRangeStart"));
int yearRangeEnd = GetterUtil.getInteger((String)request.getAttribute("liferay-ui:input-date:yearRangeEnd"));
String monthAndYearParam = namespace + request.getAttribute("liferay-ui:input-date:monthAndYearParam");
boolean monthAndYearNullable = GetterUtil.getBoolean((String)request.getAttribute("liferay-ui:input-date:monthAndYearNullable"));
int firstDayOfWeek = GetterUtil.getInteger((String)request.getAttribute("liferay-ui:input-date:firstDayOfWeek"));
String imageInputId = GetterUtil.getString((String)request.getAttribute("liferay-ui:input-date:imageInputId"));
boolean disabled = GetterUtil.getBoolean((String)request.getAttribute("liferay-ui:input-date:disabled"));

if (Validator.isNull(imageInputId)) {
	imageInputId = randomNamespace + "imageInputId";
}
else {
	imageInputId = namespace + imageInputId;
}

String dateFormatPattern = ((SimpleDateFormat)(DateFormat.getDateInstance(DateFormat.SHORT))).toPattern();

boolean dateFormatMDY = true;

if (dateFormatPattern.indexOf("y") == 0) {
	dateFormatMDY = false;
}

Date selectedDate = new Date();

Calendar cal = new GregorianCalendar();

cal.setTime(selectedDate);

if (dayValue > 0) {
	cal.set(Calendar.DATE, dayValue);
}

if (monthValue > -1) {
	cal.set(Calendar.MONTH, monthValue);
}

if (yearValue > 0) {
	cal.set(Calendar.YEAR, yearValue);
}

String[] months;
if (customLanguage.equals("es_ES")){
	String[] monthsES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
	months = monthsES;
}
else if (customLanguage.equals("de_DE")){
	String[] monthsDE = {"Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"};
	months = monthsDE;
}
else if (customLanguage.equals("fr_FR")){
	String[] monthsFR = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
	months = monthsFR;
}
else{
	String[] monthsEN = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	months = monthsEN;
	customLanguage = "en_US";
}


%>

<div class="aui-datepicker aui-datepicker-display aui-helper-clearfix <%= Validator.isNotNull(cssClass) ? cssClass : StringPool.BLANK %>" id="<%= randomNamespace %>displayDate">
	<div class="aui-datepicker-content" id="<%= randomNamespace %>displayDateContent">
		<div class="aui-datepicker-select-wrapper">
			<c:choose>
				<c:when test="<%= monthAndYearParam.equals(namespace) %>">

					<%
					int[] monthIds = CalendarUtil.getMonthIds();
					%>

					<%@ include file="select_month.jspf" %>
				</c:when>
			</c:choose>

			<%@ include file="select_day.jspf" %>

			<%@ include file="select_year.jspf" %>
		</div>
		<div class="aui-datepicker-button-wrapper">
			<button class="aui-buttonitem aui-buttonitem-content aui-buttonitem-icon-only aui-component aui-state-default aui-widget" id="buttonTest" type="button">
				<span class="aui-buttonitem-icon aui-icon aui-icon-calendar"></span>
			</button>
		</div>
	</div>
</div>

<input class="<%= disabled ? "disabled" : "" %>" id="<%= imageInputId %>Input" type="hidden" />

<aui:script use="aui-datepicker-select">
		
		//Español
 		A.DataType.Date.Locale['es_ES'] = A.merge(A.DataType.Date.Locale['es'],{
	      B: ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
	       a: ['Do','Lu','Ma','Wi','Ju','Vi','Sa']
	   	});
	   	//Alemán
	 	A.DataType.Date.Locale['de_DE'] = A.merge(A.DataType.Date.Locale['de'],{
	      B: ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'],
	       a: ['So','Mo','Di','Mi','Do','Fr','Sa']
	   	});
	   	//Frances
	   	A.DataType.Date.Locale['fr_FR'] = A.merge(A.DataType.Date.Locale['fr'],{
	      B: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
	       a: ['Di','Lu','Ma','Me','Je','Ve','Sa']
	   	});
	   	//Ingles
	   	A.DataType.Date.Locale['en_US'] = A.merge(A.DataType.Date.Locale['en'],{
	      B: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
	       a: ['Su','Mo','Tu','We','Th','Fr','Sa']
	   	});
	  
	var datePicker = new A.DatePickerSelect(
		{
			appendOrder: <%= dateFormatMDY ? "['m', 'd', 'y']" : "['y', 'm', 'd']" %>,
			boundingBox: '#<%= randomNamespace %>displayDate',
			calendar: {
				locale: '<%= customLanguage %>',
				dates: [
					new Date(
						<%= cal.get(Calendar.YEAR) %>,
						<%= cal.get(Calendar.MONTH) %>,
						<%= cal.get(Calendar.DATE) %>
					)
				],
				dateFormat: '%m/%e/%Y',
				firstDayOfWeek: <%= firstDayOfWeek %>
			},
			dayNode: '#<%= dayParam %>',
			disabled: <%= disabled %>,
			monthNode: '#<%= monthParam %>',
			on: {
				'calendar:select': function(event) {
					var formatted = event.date.formatted[0];

					A.one('#<%= imageInputId %>Input').val(formatted);
				}
			},
			populateMonth: false,
			populateYear: false,
			srcNode: '#<%= randomNamespace %>displayDateContent',
			yearNode: '#<%= yearParam %>',
			yearRange: [<%= yearRangeStart %>, <%= yearRangeEnd %>]
		}
	).render();
</aui:script>