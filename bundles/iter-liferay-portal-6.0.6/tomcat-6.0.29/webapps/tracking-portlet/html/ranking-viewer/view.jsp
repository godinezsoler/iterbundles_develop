<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp" %>

<%
	// Se comprueba si hay que generar una llamada ServerSide a un WidgetShared
	// a) No exista la variable RENDER_WIDGET
	// a) Sea una preferencia compartida
	// b) La preferencia compartida NO incluya la "Secci�n Actual"; ya que en dicho caso no se
	//		gana rendimiento porque en cada secci�n se pintar� algo distinto. Ser�a �til solamente 
	//		si tuviesen varios portlets con configuraci�n compartida en la misma secci�n, pero eso
	//		es algo improbable.
	boolean genWidget = (request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_RENDER_WIDGET) == null) &&
						(!portletItem.isEmpty() && !defaultLayout);
	
	String widgetContent = "";
	if (false && genWidget)
	{
		// Se genera el WidgetShared
		widgetContent = PortletMgr.getSharedWidgetContent(request, themeDisplay, portletItem);
	}
	
	
	if (!widgetContent.isEmpty())
	{
		out.print(widgetContent);
	}
	else
	{
%>	
		<%@ include file="ranking_render.jsp" %>
<%	
	}
%>



