<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<div class="modulo-servicios">
	<div class="recuadro">
	    <h5><liferay-ui:message key="service-front-end-title" /></h5>
	    <ul class="servicios">
	    <%	    		       			   	
	       	for (int i=0; i < servicesIds.length; i++) {
		       	try{
		    	   Service service = ServiceLocalServiceUtil.getServiceByServiceId(scopeGroupId, servicesIds[i]);  
		    	   Layout _layout = LayoutLocalServiceUtil.getLayout(service.getLinkId());	    	      
			    %>
			        <li class="servicio">	                   
			       		<a href="<%= PortalUtil.getLayoutFriendlyURL(_layout, themeDisplay) %>">        
			        		<c:if test="<%= (service.getImageId() != -1) %>">
			        			<div class="imagen-servicios"> 
			            			<img src="/image/journal/article?img_id=<%=service.getImageId()%>" /> 
			            		</div>
			        		</c:if>	       
			            	<span class="txt_service"><%= service.getTitle() %></span>        
			        	</a>	        
			        </li>
			    <%
		       	}
		       	catch(Exception err){
		       		System.out.println("Service Viewer Portlet : cannot retrieve service " + servicesIds[i]);
		       	}
	       }
	    %>
	    </ul>
	</div>
</div>
<div class="spacer"></div>
