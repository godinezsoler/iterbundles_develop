<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ page contentType="text/html; charset=UTF-8" %>

<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="java.util.ArrayList"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>

<%
	String paginationhtmlposition = request.getAttribute("paginationhtmlposition").toString();
	int fewperpage = Integer.parseInt(request.getAttribute("fewperpage").toString());
	int totalarticle = Integer.parseInt(request.getAttribute("totalarticle").toString());
	String varshowmore = request.getAttribute("showmore").toString();
	Boolean showmore = false;
	if(varshowmore.equalsIgnoreCase("true"))
		showmore = true;
	String buttonprev = request.getAttribute("buttonprev").toString();
	String buttonnext =  request.getAttribute("buttonnext").toString();
	String buttonshowmore =  request.getAttribute("buttonshowmore").toString();
	int cur	= GetterUtil.getInteger(request.getAttribute("cur").toString(),1);
	String responseNamespace = request.getAttribute("responseNamespace").toString();
	
	int myini = 1;
	int myfin = 11;
	
	//Elementos paginacion
	int endpagination = 0;
	if ((totalarticle % fewperpage) != 0)
		endpagination = (totalarticle / fewperpage) + 1;
	else
		endpagination = totalarticle / fewperpage;
	
	if( endpagination < 10)
		myfin = endpagination+1;
	
	int stepBack = 0;
	if(cur>6)
	{
		if(endpagination > cur+5)
			stepBack = cur - 6;
		else
			stepBack = endpagination - 10;
	}
	if(stepBack < 0) 
		stepBack = 0;
	
	int myfirst = 1;
	int mylast = 11;
	if(cur<=1)
	{
		myfirst = 1;
		mylast=fewperpage+1;
	}
	else
	{
		if (((cur-1) * fewperpage)<totalarticle)
			myfirst = (cur-1) * fewperpage + 1;
		else
		{
			myfirst = totalarticle - fewperpage + 1;
			mylast = endpagination + 1;
		}
			
		if ((myfirst + fewperpage)<=totalarticle)
			mylast = myfirst + fewperpage;
		else
			mylast = (totalarticle+1);
	}
%>

<div id="<%= responseNamespace %>mylistCarrousel" class="aui-button-holder nav-button">
		<c:if test='<%= (showmore == false) %>'>
			<c:if test='<%= (paginationhtmlposition.equalsIgnoreCase("a-s-l")) %>'>
				<div id="<%= responseNamespace %>myPrevButton" class="prev-button">
					<span>
						<%=buttonprev%>
					</span>
				</div>
				<div id="<%= responseNamespace %>myNextButton" class="next-button">
					<span>
						<%=buttonnext%>
					</span>
				</div>
			</c:if>
			<c:if test='<%= (paginationhtmlposition.equalsIgnoreCase("a-l-s")) %>'>
				<div id="<%= responseNamespace %>myPrevButton" class="prev-button">
					<span>
						<%=buttonprev%>
					</span>
				</div>
			</c:if>
			
			
			<ul id="<%= responseNamespace %>myNavButtons">
					<% for (int i = myini; i < myfin; i++) { %>
						<li data-page="<%= i + stepBack %>" id="<%= responseNamespace %>pagtool<%= i%>" >
							<%= i + stepBack%>
						</li>
					<% } %>
			</ul>
			
			
			<c:if test='<%= (paginationhtmlposition.equalsIgnoreCase("l-a-s")) %>'>

				<div id="<%= responseNamespace %>myPrevButton" class="prev-button">
					<span>
						<%=buttonprev%>
					</span>
				</div>
				<div id="<%= responseNamespace %>myNextButton" class="next-button">
					<span>
						<%=buttonnext%>
					</span>
				</div>
			</c:if>
			<c:if test='<%= (paginationhtmlposition.equalsIgnoreCase("a-l-s")) %>'>
				<div id="<%= responseNamespace %>myNextButton" class="next-button">
					<span>
						<%=buttonnext%>
					</span>
				</div>
			</c:if>
		</c:if>
		 <c:if test='<%= (showmore) %>'>
			<div id="<%= responseNamespace %>myMoreButton" class="more-button">
					<span>
						<%=buttonshowmore%>
					</span>
			</div>
		</c:if>
</div>



<script>
		 /* Variables Globales  */
		
		var <%= responseNamespace %>mynumitems = <%= fewperpage %>;		
		var <%= responseNamespace %>mysize = <%=totalarticle %>;		
		var <%= responseNamespace %>mynumpages =  Math.ceil(<%= responseNamespace %>mysize / <%= responseNamespace %>mynumitems);				
		var <%= responseNamespace %>myfirst = <%=myfirst %>;	
		var <%= responseNamespace %>mylast = <%=mylast %>;
		var <%= responseNamespace %>myini = <%=stepBack + 1%>;	
		var <%= responseNamespace %>myfin = <%=myfin %>;
		var <%= responseNamespace %>showmore = <%=showmore %>;
		var <%= responseNamespace %>myselpag = <%=cur%>;
		var <%= responseNamespace %>contshowmore = <%=cur%>;
		
		var <%= responseNamespace %>loadPerPage = function (<%= responseNamespace %>myselpag)
		{
			// Si el teaser está marcado para recargar la página al paginar y no es del tipo "mostrar más"
			if (<%=!showmore%> && jQryIter('#<%= responseNamespace %>mylistCarrousel').closest(".itr-reloadonpager").length > 0)
			{
				// Calcula la clave en función de si hay más de un teaser paginado en la página
				var key = jQryIter('div[id$="_teaser_paged"]').length == 1 ? "p" : "<%=responseNamespace%>p";
					
				// Si la página deseada es distinta a la actual
				if (jQryIter.getFragmentIdentifier(key) != <%= responseNamespace %>myselpag)
				{
					// Añade al fragment la página destino
					jQryIter.setFragmentIdentifier(key, <%= responseNamespace %>myselpag);
					
					if (jQryIter('div[id$="_teaser_paged"]').closest(".itr-reloadonpager").length == 1)
					{
						// Añade la página al query string con pormato page=N
						jQryIter.setQueryParam("page", <%= responseNamespace %>myselpag);
					}
					else
					{
						// Añade la página al query string con pormato _id_page=N
						jQryIter.setQueryParam("<%=responseNamespace%>page", <%= responseNamespace %>myselpag);
					}					
					
					// Recarga la página
					//window.location.reload();
				}
			}
			
          	jQryIter('#<%= responseNamespace %>mylistCarrousel').trigger("custom", [<%= responseNamespace %>myselpag]);
		};
		
		
	
		/*  Inicializamos el carrusel  */
		var <%= responseNamespace %>init = function() 
		{
			if(<%= responseNamespace %>showmore)
			{
				if (<%= responseNamespace %>mylast > <%= responseNamespace %>mysize)
					jQryIter("#<%= responseNamespace %>myMoreButton").addClass("disable-button");
			}
			else
			{	
				<%= responseNamespace %>cambiarNavItem(<%= responseNamespace %>myini, <%= responseNamespace %>myselpag);
				<%= responseNamespace %>buttonsEnabler();
			}
		}
		
		var <%= responseNamespace %>cambiarNavItem = function (ini, elem) 
		{
			var <%= responseNamespace %>cont = ini;
				jQryIter("#<%= responseNamespace %>myNavButtons li").each(function() 
																{
																	if (<%= responseNamespace %>cont == elem)
																		jQryIter(this).addClass("nav-page-selected");
																	else 
																	{
																		if (jQryIter(this).hasClass("nav-page-selected"))
																			jQryIter(this).removeClass("nav-page-selected");																
																		}
																		<%= responseNamespace %>cont++;
																});
		}
		
		var <%= responseNamespace %>loadedsuccess = function (<%= responseNamespace %>myfirstItem, <%= responseNamespace %>mylastItem, <%= responseNamespace %>myselpag)
		{
			<%= responseNamespace %>myfirst=<%= responseNamespace %>myfirstItem;
			<%= responseNamespace %>mylast=<%= responseNamespace %>mylastItem;
			
			 /* Calculo de carrousel  */
			<%= responseNamespace %>myini = 1;
			
			if(<%= responseNamespace %>showmore)
			{
				if (<%= responseNamespace %>mylast > <%= responseNamespace %>mysize)
					jQryIter("#<%= responseNamespace %>myMoreButton").addClass("disable-button");
			}
			else
			{	 
				if(<%= responseNamespace %>mynumpages>10)
				{
					var improve = parseInt(<%= responseNamespace %>myselpag) + 4;
					var stepBack = parseInt(<%= responseNamespace %>myselpag) - 5;
					
					if((stepBack > 0) && (improve < <%= responseNamespace %>mynumpages))
						<%= responseNamespace %>myini = stepBack;
				
					if( improve >= <%= responseNamespace %>mynumpages )
					{
						if(<%= responseNamespace %>mynumpages - 9 > 0)	
							<%= responseNamespace %>myini = <%= responseNamespace %>mynumpages  - 9;
					}
				}
				
				var cont = <%= responseNamespace %>myini;
				jQryIter("#<%= responseNamespace %>myNavButtons li").each(function() 
					{
						jQryIter(this).attr('data-page', cont);
						jQryIter(this).text(cont);
						cont++;
					});
				
				<%= responseNamespace %>cambiarNavItem(<%= responseNamespace %>myini, <%= responseNamespace %>myselpag);
				
				<%= responseNamespace %>buttonsEnabler();
			}
		}
		
		var <%= responseNamespace %>buttonsEnabler = function ()
		{
			if (<%= responseNamespace %>myfirst == 1)
				jQryIter("#<%= responseNamespace %>myPrevButton").addClass("disable-button");
			else
				if(jQryIter("#<%= responseNamespace %>myPrevButton").hasClass("disable-button"))
					jQryIter("#<%= responseNamespace %>myPrevButton").removeClass("disable-button");
			
			if (<%= responseNamespace %>mylast > <%= responseNamespace %>mysize)
				jQryIter("#<%= responseNamespace %>myNextButton").addClass("disable-button");
			else
				if( jQryIter("#<%= responseNamespace %>myNextButton").hasClass("disable-button"))
					jQryIter("#<%= responseNamespace %>myNextButton").removeClass("disable-button");
		}
		
		<%= responseNamespace %>init();
	
		jQryIter("#<%= responseNamespace %>myPrevButton").on("click",
			function() 
			{		
				if (parseInt(<%= responseNamespace %>myselpag) > 1) 
				{
					<%= responseNamespace %>myselpag=parseInt(<%= responseNamespace %>myselpag)-1;
					<%= responseNamespace %>loadPerPage( <%= responseNamespace %>myselpag );
				}
			}
		);
		
		jQryIter("#<%= responseNamespace %>myNextButton").on("click", 
			function() 
			{
				if (parseInt(<%= responseNamespace %>myselpag) < <%= responseNamespace %>mynumpages) 
				{
					<%= responseNamespace %>myselpag=parseInt(<%= responseNamespace %>myselpag)+1;
					<%= responseNamespace %>loadPerPage( <%= responseNamespace %>myselpag );
				}
			}
		);
		
		jQryIter("#<%= responseNamespace %>myMoreButton").on("click", 
			function() 
			{
				if (<%= responseNamespace %>contshowmore < <%= responseNamespace %>mynumpages)
				{	
					<%= responseNamespace %>contshowmore = <%= responseNamespace %>contshowmore + 1;	
					<%= responseNamespace %>loadPerPage(<%= responseNamespace %>contshowmore);
				}
			}
		);
	 <%
	 	for (int i = myini; i < myfin; i++) {
			mylast = getLastPosition(i,fewperpage);
			myfirst = mylast-fewperpage;
			if (mylast > totalarticle)
				mylast = totalarticle+1;
			
			String temp = "pagtool"+String.valueOf(i);
		%>			
			jQryIter("#<%= responseNamespace %><%= temp %>").on("click", 
				function()
				{	
					<%= responseNamespace %>myselpag = jQryIter("#<%= responseNamespace %><%= temp %>").attr("data-page");
					<%= responseNamespace %>loadPerPage( <%= responseNamespace %>myselpag );
				});
		<% } %>
	
	</script>
	
<%!
	public int getLastPosition(int i, int fewperpage){
		if (i == 1)
			return fewperpage+1;
		else
			return fewperpage+getLastPosition(i-1,fewperpage);
	}
%>
