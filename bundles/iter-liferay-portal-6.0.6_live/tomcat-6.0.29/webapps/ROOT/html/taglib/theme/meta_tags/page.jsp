<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.protecmedia.iter.news.service.MetadataLocalServiceUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.MetaTagsUtil"%>
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

<%@ include file="/html/taglib/init.jsp" %>

<c:if test="<%= layout != null %>">
	<%
		Document commonTags = MetaTagsUtil.getDocumentFromXML( (String)request.getAttribute(WebKeys.PAGE_META_TAGS) );
		
		String[] commonTagsList = MetaTagsUtil.getCommonMetaTags(commonTags);
		for (String commonTag : commonTagsList)
		{
			%>
			<%= commonTag %>
			<%
		}
		
		
		
		Document doc = MetaTagsUtil.getDocumentFromXML( (String)request.getAttribute(WebKeys.PAGE_OPENGRAPHS) );
		
		String[] metatagsList = MetaTagsUtil.getMetaTags( doc );
		for (String meta : metatagsList)
		{
			%>
			<%= meta %>
			<%
		}
		
		String[] linkTagsList = MetaTagsUtil.getLinkTags( doc );
		for (String link : linkTagsList)
		{
			%>
			<%= link %>
			<%
		}
	%>
</c:if>
