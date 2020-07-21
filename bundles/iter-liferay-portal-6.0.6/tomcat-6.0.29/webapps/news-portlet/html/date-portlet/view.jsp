<%--
*Copyright (c) 2012 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@ include file="init.jsp" %>

<%
	Locale l = locale;
	String lang = GroupConfigTools.getGroupConfigField(scopeGroupId, "lang");
	if(Validator.isNotNull(lang))
	{
		if(lang.contains(StringPool.DASH))
			lang = lang.replace(StringPool.DASH, StringPool.UNDERLINE);
		else
			lang = lang.concat(StringPool.UNDERLINE).concat(StringUtil.upperCase(lang));
		
		l = LocaleUtil.fromLanguageId(lang);
	}
	
	String siteDate = DateUtil.formatDate(currentDate, dateWithoutHourFormat, l);
	String siteTime = DateUtil.formatDate(currentDate, hourFormat, l);
%>	

<div class="updatedSiteBlock">
    <div class="updatedSiteTextBefore"><%=textBefore%></div>
    <div class="updatedSiteDate"><%=siteDate%></div>
    <div class="updatedSiteTime"><%=siteTime%></div>
    <div class="updatedSiteTextAfter"><%=textAfter%></div>
</div>