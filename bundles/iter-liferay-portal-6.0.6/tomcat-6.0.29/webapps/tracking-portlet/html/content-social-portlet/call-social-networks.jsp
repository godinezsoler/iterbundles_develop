<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.service.LayoutSetLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingKeys"%>	

<%@ include file="init.jsp"%>	

<c:if test="<%=socialNetworks.length > 0 %>">

<%@ include file="social-networks.jsp"%>

</c:if>