<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="org.apache.commons.lang.ArrayUtils"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="com.liferay.portal.kernel.util.advertisement.SlotAssignment"%>
<%@page import="com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.protecmedia.iter.base.service.util.ServiceError"%>
<%@page import="com.protecmedia.iter.base.service.util.ErrorRaiser"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.liferay.portal.kernel.util.PropsUtil"%>
<%@page import="com.liferay.portal.kernel.json.JSONObject"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portlet.ContextVariables"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>


<%@ include file="init.jsp"%>

<%!
	private static Log _log = LogFactoryUtil.getLog("advertisement-portlet.docroot.html.adcontainer-portlet.adcontainer.jsp");
%>

<%
	if (!slotId.equals(""))
	{
		try
		{
			SlotAssignment slotAssignment = AdvertisementUtil.getAdContainer(slotId);
			if(Validator.isNotNull(slotAssignment))
			{
				boolean isEnabled = slotAssignment.getEnabled();
				String tagscript  = AdvertisementUtil.hideAdvWithFake() ? slotAssignment.getFakeTagScript() : slotAssignment.getTagScript();
		
				if (isEnabled && Validator.isNotNull(tagscript))
				{
					String superid = slotAssignment.getSuperId();
					String tagtype = slotAssignment.getTagtype();
		%>
					<c:if test="<%= showAdvMsg %>">
						<div class="advertisement_banner_show_label" >
							<c:choose>
								<c:when test="<%= !advMsg.isEmpty() %>">
									 <%= advMsg %>
								</c:when>
								<c:otherwise>
									<liferay-ui:message key="advertisement-banner-show-advertisement-label" />
								</c:otherwise>
							</c:choose>
						</div>
					</c:if>
	
					<c:choose>
						<c:when test='<%= tagtype.equalsIgnoreCase(IterKeys.TYPE_IMAGE) %>'>
							<a href="<%= tagscript %>" target="_blank">
					       		<img width="100%" src="<%= tagscript %>"/>
							</a>
						</c:when>
						<c:when test='<%= tagtype.equalsIgnoreCase(IterKeys.TYPE_FLASH) %>'>
							<a href="javascript:;" target="_blank">						
								<div id="<portlet:namespace />flashcontent" style="height: 100%; width: 100%">
								</div>
							</a>
												
							<script type="text/javascript">
								jQryIter(document).ready(function(){
									jQryIter("#<portlet:namespace />flashcontent").flash(
										{
											swf: '<%= tagscript %>',
											wmode: 'opaque',
											height: '100%',
											width: '100%',
											hasVersion: 9.115,
											flashvars:{
											}
										}		
									);
								});
							</script>						
							
						</c:when>
						<c:when test='<%= tagtype.equalsIgnoreCase(IterKeys.TYPE_HTML) %>'>
							<div>
								<%= tagscript %>
							</div>
						</c:when>
					</c:choose>
		
		<%
					if( Validator.isNotNull( superid ) )
					{
						//Se añade el id del tag global al request
						AdvertisementUtil.add2AttributeValueList(request, AdvertisementUtil.PARENT_TAGS_IDS, superid);
					}
				}
			}
		}
		catch(ServiceError se)
		{
			_log.debug(se);
		}
		catch(Exception e)
		{
			_log.error(e);
		}
	}
%>