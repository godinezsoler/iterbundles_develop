<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.protecmedia.iter.news.service.CountersLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="java.util.GregorianCalendar"%>

<%
try
{
	String articleId = ParamUtil.get(request, "articleId", "");
	long groupId = ParamUtil.get(request, "groupId", -1);
	//leer parámetros estadísticas de adblock(groupId, hadadblock, hasadblock, mode)
	int hadadblock =  ParamUtil.get(request, "hadadblock", 0);
	int hasadblock =  ParamUtil.get(request, "hasadblock", 0);
	int mode  =  ParamUtil.get(request, "mode", 0);
	
	
	if (!articleId.equals("") && articleId != null && groupId != -1 && !articleId.equals(IterKeys.EXAMPLEARTICLEID)) 
	{
		CountersLocalServiceUtil.incrementCounter(request, articleId, groupId, IterKeys.OPERATION_VIEW);			
	}
}
catch(Exception err){}
%>
