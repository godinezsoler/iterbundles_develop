<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>


<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.UnrepeatableArticlesMgr"%>
<%@page import="com.liferay.portal.kernel.util.QualificationTools"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@ include file="init.jsp"%>


	<%
	//Portlet sin configuracion
	if (context.equals("")){
		if (environment.equals(IterKeys.ENVIRONMENT_PREVIEW)){%>	
			<div class="portlet-msg-info">
				<span class="displaying-help-message-tpl-holder">
					<liferay-ui:message key="please-contact-with-your-administrator-to-configure-this-portlet" />
				</span>
			</div>
	  <%}
	}
	//Portlet configurado
	else{
		List<String[]> results = null;
	
		if( context.equals("layout-context") )
		{
			// Si layoutIds es null, es que el portlet está configurado para obtener datos 
			//de la sección actual y no tenemos sección
			if( layoutIds!=null )
			{
				UnrepeatableArticlesMgr.active(PortalUtil.getPortletId(request));
				
				//Filtro por páginas
				results = TeaserContentUtil.getFilterArticles(scopeGroupId, null, structures, 0, numAlerts, 
															  GroupMgr.getPublicationDate(scopeGroupId), 
															  false, new String[]{"-1"}, IterKeys.ORDER_DESC,
															  null, null, qualificationId, layoutIds, true);
				
				UnrepeatableArticlesMgr.add(results, PortalUtil.getPortletId(request));
			}
		}
		else if(context.equals("metadata-context"))
		{
			UnrepeatableArticlesMgr.active(PortalUtil.getPortletId(request));
			
			//Filtro por categorías
			results = TeaserContentUtil.getFilterArticles(scopeGroupId, null, structures, 0, numAlerts, 
														  GroupMgr.getPublicationDate(scopeGroupId), 
														  false, new String[]{"-1"}, IterKeys.ORDER_DESC,
														  contentCategoryIdsLong, null, qualificationId, 
														  null, true);
			
			UnrepeatableArticlesMgr.add(results, PortalUtil.getPortletId(request));
		}
		
		if (results != null && results.size() > 0)
		{
			//caso no vacío 
			
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, results.size());
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, request.getAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT));
			
			//se pide el qualificationName para insertarlo en el request
			String qualifId = qualificationId != null && qualificationId.length > 0 ? qualificationId[0]  :  "";
			String qualificationName = QualificationTools.getQualificationName( qualifId);
			
			request.setAttribute( VelocityContext.VELOCITYVAR_ITER_QUALIFICATION, qualificationName );
		
		%>
			
				<div id="alertslides" style="display:none;">
			      <%
			      	for (int i = 0; i < results.size(); i++)
			      	{
			      %>
						<div class="alertslide">
							<%
								StringBuffer alertHTMLContent = new StringBuffer();
							
								try
								{
									String[] data = results.get(i);
									JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, data[0]);
									
									String articleId 		 = data[0];
									String signedArticleId 	 =  IterURLUtil.getSignedArticleId(articleId);
									
									alertHTMLContent.append("<div class=\"alert " + TeaserContentUtil.getCSSAccessClass(article, request) + "\" "+ "iteridart=" + signedArticleId + ">");
									
									if(!templateId.isEmpty())
									{
										String viewMode = PageContentLocalServiceUtil.getArticleContextInfo(scopeGroupId, article.getArticleId(), ctxLayouts);
		
										String xmlRequest = PortletRequestUtil.toXML(renderRequest, renderResponse);
		
										alertHTMLContent.append(PageContentLocalServiceUtil.getArticleContent(article, templateId, templateIdRestricted, viewMode,
																										  	  themeDisplay, xmlRequest, modeArticle, 
																										  	  request, i + 1, results.size()));
									}
									else
									{
										// Montamos la plantilla por defecto
										String viewModeUrl = IterURLUtil.getArticleURLByLayoutUUID(scopeGroupId, article.getArticleId(), layoutIds);
										
										String headline = PageContentLocalServiceUtil.getWebContentField(article, "Headline", locale.toString(), 0);
										if (headline.equals(""))
											headline = article.getTitle();
										
										alertHTMLContent.append("<div class=\"titulo\">" + 
																	title + 
																"</div>" + 
																"<div class=\"texto\">" + 
																	"<a href=\"" + viewModeUrl + "\">" + headline +"</a>" + 
																"</div>");
									}
									
									alertHTMLContent.append("</div>");
									
									out.print(alertHTMLContent);
								}
								catch (Exception e)
								{
									_log.error(e);
								}
							%>
						</div>
				  <%
					}
			      %>
			    </div>
			<% }
		else
		{%>
		
		<%-- Caso vacio --%>
	
		<c:if test='<%= showDefaultTextHTML && !defaultTextHTML.equals("")%>'>
			<div>
				<%=defaultTextHTML%>
			</div>
		</c:if>
			
		<%}
	}%>	
			


<script type="text/javascript">

(function ($) {
  $.fn.responsiveSlides = function (options) {
    // Settings
    var settings = {
      'speed' : 4000,
      'fade' : 1000,
      'auto' : true,
      'maxwidth' : 'none',
      'namespace' : 'rs'
    };

    return this.each(function () {
      var $this = $(this);
      if (options) {
        $.extend(settings, options);
      }

      var slideshow = function () {
        var $slide = $this.children('.alertslide'),
          namespace = settings.namespace,
          activeClass = namespace + '_here',
          visibleClass = namespace + '_on',
          slideClassPrefix = namespace + '_s',
          tabsClass = namespace + '_tabs',
          $pagination = $('<ul class="' + tabsClass + '" />'),
          fadetime = parseFloat(settings.fade),
          visible = { 'position': 'relative', 'float': 'left' },
          hidden = { 'position': 'absolute', 'float': 'none' };

        // Only run if there's more than one slide
        if ($this.find($slide).length > 1) {
          $slide.each(function (i) {
            this.id = slideClassPrefix + i;
          });

          $slide.css({
            'top': 0,
            'left': 0,
            'width': '100%',
            'height': 'inherit',
            'position': 'absolute'
          });
          
          $slide.first().css({
        	'position': 'relative' 
          });
          
          $this.css({
            'max-width': parseFloat(settings.maxwidth),
            'width': '100%',
            'overflow': 'hidden',
            'position': 'relative'
          })
            .children().first().siblings().hide();

          // Auto: true
          if (settings.auto === true) {
            setInterval(function () {
              $this.children().first().fadeOut(fadetime, function () {
                $(this).css(hidden);
              }).next($slide).fadeIn(fadetime, function () {
                $(this).css(visible);
              }).end().appendTo($this);
            }, parseFloat(settings.speed));

          // Auto: false
          } else {
            t = '';
            $slide.each(function (i) {
              var n = i + 1;
              t +=
                '<li>' +
                '<a href="#' + slideClassPrefix + n + '"' +
                'class="' + slideClassPrefix + n + '">' + n + '</a>' +
                '</li>';
            });
            $pagination.append(t);

            $this.after($pagination).find(':first-child').addClass(visibleClass);
            $('.' + slideClassPrefix + '1').parent().addClass(activeClass);

            $('.' + tabsClass + ' a').each(function (i) {
              var $el = $(this);
              $el.click(function (e) {
                e.preventDefault();
                // Prevent clicking if animated
                if ($('.' + visibleClass + ':animated').length) {
                  return false;
                }
                if (!($el.parent().hasClass(activeClass))) {
                  $('.' + tabsClass + ' li').removeClass(activeClass);
                  $('.' + visibleClass).stop().fadeOut(fadetime, function () {
                    $(this).removeClass(visibleClass).css(hidden);
                  }).end();
                  $('#' + slideClassPrefix + i).stop().fadeIn(fadetime, function () {
                    $(this).addClass(visibleClass).css(visible);
                  }).end();
                  $el.parent().addClass(activeClass);
                }
              });
            });
          }

        }
      };

      // Fallback to make IE6 support CSS max-width
      var widthSupport = function () {
        var maxwidth = parseFloat(settings.maxwidth);
        if (options && options.maxwidth) {
          if (typeof document.body.style.maxHeight === 'undefined') {
            $this.each(function () {
              $this.css('width', '100%');
              if ($this.width() > maxwidth) {
                $this.css('width', maxwidth);
              } else if ($this.width() < maxwidth) {
                $this.css('width', '100%');
              }
            });
          }
        }
      };

      // Call once
      slideshow();
      widthSupport();
      // Call on resize
      $(window).resize(function () {
        widthSupport();
      });
    });
  };
})(jQryIter);

</script>

<script>
	jQryIter(function() {	
		jQryIter("#alertslides").responsiveSlides({
	      	speed: <%= time * 1000 %>,	    	
	        fade: <%= fade %>		              
	      });
		jQryIter("#alertslides").css('display','block');
		
    });
</script>
