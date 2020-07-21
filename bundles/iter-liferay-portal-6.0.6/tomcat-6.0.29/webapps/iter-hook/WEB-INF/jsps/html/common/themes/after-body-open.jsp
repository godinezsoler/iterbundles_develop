<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>


<%@ include file="/html/common/init.jsp" %>

<%
	
	HttpServletRequest original_request = PortalUtil.getOriginalServletRequest(request);
	Object isNewsletterPageObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_IS_NEWSLETTER_PAGE);
	boolean isNewsletterPage = false;
	if(isNewsletterPageObject != null)
		isNewsletterPage = GetterUtil.getBoolean(isNewsletterPageObject.toString());

	boolean isApacheRequest = PHPUtil.isApacheRequest(original_request);
	
	String scopeGroupFriendlyURL = themeDisplay.getScopeGroupFriendlyURL();

%>
<%	
	if ( !isNewsletterPage && scopeGroupFriendlyURL != "/control_panel"  && scopeGroupFriendlyURL != "/guest"  &&  scopeGroupFriendlyURL != "/null")
	{
		String blockAdBlockMode = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "blockeradblock", "@mode");
		//scopeGroupFriendlyURL = "/null" para el grupo con nombre "Global".
				
		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
		{
			//se obtienen las preferencias con GroupConfigTools.java
			String enableUse = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/tagmanager/@enableuse");
			String gtmContainerId = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/tagmanager/@containerid");
			if( "true".equalsIgnoreCase(enableUse) && !StringPool.BLANK.equals(gtmContainerId) )
			{
%>			
				<!-- Google Tag Manager -->
				<noscript><iframe src="//www.googletagmanager.com/ns.html?id=<%=gtmContainerId%>"
										height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
				<script>
						(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
						new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
						j=d.createElement(s),dl=l!="dataLayer"?'&l='+l:'';j.async=true;j.src=
						'//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
						})(window,document,'script',"<%=PropsValues.GOOGLE_GTM_DATALAYER_NAME%>","<%=gtmContainerId%>");
				</script>
				<!-- End Google Tag Manager -->

<%	
			}	
		}
		
		// ITER-1076 El fondo de publicidad no hace un vínculo hacía el destino configurado
		// Se inserta el link del fondo de publicidad
		// http://jira.protecmedia.com:8080/browse/ITER-1076?focusedCommentId=45776&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-45776
		String clickUrl = String.valueOf(original_request.getAttribute("clickUrl"));
		if (Validator.isNotNull(clickUrl))
		{
			String clickScript = String.valueOf(original_request.getAttribute("clickScript"));
			if (Validator.isNotNull(clickScript))
			{
%>
				<script type="text/javascript">
				function SkinCountingScript() {};
				SkinCountingScript.call = function()
				{
					<%=clickScript%>
				};
				</script>
<%	
			}
%>
			<a
                href="<%=clickUrl%>"
<%			
            if (Validator.isNotNull(clickScript))
            {
%>
				onclick="SkinCountingScript.call()"
<%           	
            }
%>           
                target="_blank" style="width: 100%; height: 100%; position: fixed;"/>
<%                
		}
%>
		<script type="text/javascript">
<%
			if (!PropsValues.ITER_RESPONSIVE_LAZYLOAD)
			{
%>
               document.body.addEventListener
				(
					"load",
					function(event)
					{
					   var tgt = event.target;
					   if( tgt.tagName == "IMG")
					   {
						   var srcVal = jQryIter(tgt).attr("src");
						   if ( srcVal == "/news-portlet/img/transparentPixel.png" )
							   jQryIter.lazyLoadSetup( tgt );
					   }
                    },
                    true 
                );
<%
			}
%>
                jQryIter(document).on
                (
                    "click",
                    ".ui-accordion-header",
                    function()
                    {
                    	jQryIter(window).resize();
                    }
                );
                
<%
                if ("active".equals(blockAdBlockMode) || "passive".equals(blockAdBlockMode))
                {
%>
                	window.blockAdBlock = new BlockAdBlock
	               	(
	               		{
	               			checkOnLoad: true,
	               			resetOnEnd: true, 
	               			mode: '<%=blockAdBlockMode%>', 
	               			groupid: <%=scopeGroupId%>
	               		}
	               	);
	
	                if( typeof blockAdBlock === 'undefined' )
	                	jQryIter.adBlockDetected();
	                else
	                	blockAdBlock.onDetected(jQryIter.adBlockDetected).onNotDetected(jQryIter.adBlockNotDetected);
<%
            	}
%>               
		</script>		
<%
	}
%>


