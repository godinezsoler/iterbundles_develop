<%--
*Copyright (c) 2013 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.expando.model.ExpandoValue"%>
<%@page import="com.protecmedia.iter.services.util.GeolocationUtil"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.protecmedia.iter.services.util.Geolocated"%>


<%
	boolean withoutKey = true;
	String contentTitle = "";
	List<Geolocated> geolocationFieldsProccessed = new ArrayList<Geolocated>();
	boolean emptyFields = true;
	try
	{
		articleId = (String) renderRequest.getParameter("content-id");	
		JournalArticle webContent = JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId);
		
		List<String> geolocationFields = PageContentLocalServiceUtil.getWebContentField
				 (webContent, IterKeys.STANDARD_ARTICLE_GEOLOCATION, locale.toString());
		emptyFields = true;
		for (String field : geolocationFields)
		{
			if (!field.equals(""))
				emptyFields = false;
		}
		
		if (!emptyFields)
		{
			geolocationFieldsProccessed = GeolocationUtil.proccessGeolocationFields(geolocationFields);
			Geolocated zoomAndMapType = GeolocationUtil.findZoomAndmapType(zoom,mapType,geolocationFieldsProccessed);
			zoom = zoomAndMapType.getZoom();
			if(zoom.equals(""))
				zoom = "-1";
			
			//Carga zoomStyle
			if(zoomControlStyle.equals("")||zoomControlStyle.equals(null))
				zoomControlStyle = "0";
			else
			{
				if(zoomControlStyle.equals("small")) 	
					zoomControlStyle = "0";
				if(zoomControlStyle.equals("tall")) 	
					zoomControlStyle = "1";
				if(zoomControlStyle.equals("Default"))  
					zoomControlStyle = "2";
			}
			
			//Carga zoomPosition
			if(zoomControl.equals("")||zoomControl.equals(null))
				zoomControl = "false";
			
			//Carga zoomPosition
			if(zoomControlPosition.equals("")||zoomControlPosition.equals(null))
				zoomControlPosition = "0";
			else
			{
	 			if(zoomControlPosition.equals("Default")) 	
	 				zoomControlPosition = "0";
				if(zoomControlPosition.equals("BC")) 		
					zoomControlPosition = "1";
				if(zoomControlPosition.equals("BL")) 		
					zoomControlPosition = "2";
				if(zoomControlPosition.equals("BR"))		
					zoomControlPosition = "3";
				if(zoomControlPosition.equals("LB"))		
					zoomControlPosition = "4";
				if(zoomControlPosition.equals("LC"))		
					zoomControlPosition = "5";
				if(zoomControlPosition.equals("LT"))		
					zoomControlPosition = "6";
				if(zoomControlPosition.equals("RB"))		
					zoomControlPosition = "7";
				if(zoomControlPosition.equals("RC"))		
					zoomControlPosition = "8";
				if(zoomControlPosition.equals("RT"))		
					zoomControlPosition = "9";
				if(zoomControlPosition.equals("TC"))		
					zoomControlPosition = "10";
				if(zoomControlPosition.equals("TL"))		
					zoomControlPosition = "11";
				if(zoomControlPosition.equals("TR"))		
					zoomControlPosition = "12";
			} 
			
			//Carga zoomStyle
			mapType = zoomAndMapType.getMapType();
			if(mapType.equals("")||mapType.equals(null))
				mapType = "1";
			/* else
			{
				if(mapType.equals("r")) 
					mapType = "1";
				if(mapType.equals("s")) 	
					mapType = "2";
				if(mapType.equals("h"))  		
					mapType = "3";
				if(mapType.equals("t"))  		
					mapType = "4";
			} */
			
			//Carga mapTypeStyle
			if(mapTypeControlStyle.equals("")||mapTypeControlStyle.equals(null))
				mapTypeControlStyle = "0";
			else
			{
				if(mapTypeControlStyle.equals("horizontalBar")) 
					mapTypeControlStyle = "0";
				if(mapTypeControlStyle.equals("dropdownmenu")) 	
					mapTypeControlStyle = "1";
				if(mapTypeControlStyle.equals("Default"))  		
					mapTypeControlStyle = "2";
			}
			
			//Carga zoomPosition
			if(mapTypeControl.equals("")||mapTypeControl.equals(null))
				mapTypeControl = "false";
			
			//Carga zoomPosition
			if(mapTypeControlPosition.equals("")||mapTypeControlPosition.equals(null))
				mapTypeControlPosition = "0";
			else
			{
	 			if(mapTypeControlPosition.equals("Default")) 	
	 				mapTypeControlPosition = "0";
				if(mapTypeControlPosition.equals("BC")) 		
					mapTypeControlPosition = "1";
				if(mapTypeControlPosition.equals("BL")) 		
					mapTypeControlPosition = "2";
				if(mapTypeControlPosition.equals("BR"))			
					mapTypeControlPosition = "3";
				if(mapTypeControlPosition.equals("LB"))			
					mapTypeControlPosition = "4";
				if(mapTypeControlPosition.equals("LC"))			
					mapTypeControlPosition = "5";
				if(mapTypeControlPosition.equals("LT"))			
					mapTypeControlPosition = "6";
				if(mapTypeControlPosition.equals("RB"))			
					mapTypeControlPosition = "7";
				if(mapTypeControlPosition.equals("RC"))			
					mapTypeControlPosition = "8";
				if(mapTypeControlPosition.equals("RT"))			
					mapTypeControlPosition = "9";
				if(mapTypeControlPosition.equals("TC"))			
					mapTypeControlPosition = "10";
				if(mapTypeControlPosition.equals("TL"))			
					mapTypeControlPosition = "11";
				if(mapTypeControlPosition.equals("TR"))			
					mapTypeControlPosition = "12";
			} 
			
			//Carga zoomPosition
			if(panControl.equals("")||panControl.equals(null))
				panControl = "false";
			
			if(panPosition.equals("")||panPosition.equals(null))
				panPosition = "0";
			else
			{
	 			if(panPosition.equals("Default")) 	
	 				panPosition = "0";
				if(panPosition.equals("BC")) 		
					panPosition = "1";
				if(panPosition.equals("BL")) 		
					panPosition = "2";
				if(panPosition.equals("BR"))		
					panPosition = "3";
				if(panPosition.equals("LB"))		
					panPosition = "4";
				if(panPosition.equals("LC"))		
					panPosition = "5";
				if(panPosition.equals("LT"))		
					panPosition = "6";
				if(panPosition.equals("RB"))		
					panPosition = "7";
				if(panPosition.equals("RC"))		
					panPosition = "8";
				if(panPosition.equals("RT"))		
					panPosition = "9";
				if(panPosition.equals("TC"))		
					panPosition = "10";
				if(panPosition.equals("TL"))		
					panPosition = "11";
				if(panPosition.equals("TR"))		
					panPosition = "12";
			}
			
			//Carga scalePosition
			
			if(scaleControl.equals("")||scaleControl.equals(null))
				scaleControl = "false";
			
			if(scalePosition.equals("")||scalePosition.equals(null))
				scalePosition = "0";
			else
			{
	 			if(scalePosition.equals("Default")) 
	 				scalePosition = "0";
				if(scalePosition.equals("BC")) 		
					scalePosition = "1";
				if(scalePosition.equals("BL")) 		
					scalePosition = "2";
				if(scalePosition.equals("BR"))		
					scalePosition = "3";
				if(scalePosition.equals("LB"))		
					scalePosition = "4";
				if(scalePosition.equals("LC"))		
					scalePosition = "5";
				if(scalePosition.equals("LT"))		
					scalePosition = "6";
				if(scalePosition.equals("RB"))		
					scalePosition = "7";
				if(scalePosition.equals("RC"))		
					scalePosition = "8";
				if(scalePosition.equals("RT"))		
					scalePosition = "9";
				if(scalePosition.equals("TC"))		
					scalePosition = "10";
				if(scalePosition.equals("TL"))		
					scalePosition = "11";
				if(scalePosition.equals("TR"))		
					scalePosition = "12";
			}
		
			//Carga Overview
			if(overviewControl.equals("")||overviewControl.equals(null))
				overviewControl = "false";
			
			if(overviewOpened.equals("")||overviewOpened.equals(null))
				overviewOpened = "false";
			
			//Carga StreetView
			if(streetViewControl.equals("")||streetViewControl.equals(null))
				streetViewControl = "false";
			
			if(streetViewPosition.equals("")||streetViewPosition.equals(null))
				streetViewPosition = "0";
			else
			{
	 			if(streetViewPosition.equals("Default"))	
	 				streetViewPosition = "0";
				if(streetViewPosition.equals("BC")) 	 	
					streetViewPosition = "1";
				if(streetViewPosition.equals("BL")) 		
					streetViewPosition = "2";
				if(streetViewPosition.equals("BR"))			
					streetViewPosition = "3";
				if(streetViewPosition.equals("LB"))			
					streetViewPosition = "4";
				if(streetViewPosition.equals("LC"))			
					streetViewPosition = "5";
				if(streetViewPosition.equals("LT"))			
					streetViewPosition = "6";
				if(streetViewPosition.equals("RB"))			
					streetViewPosition = "7";
				if(streetViewPosition.equals("RC"))			
					streetViewPosition = "8";
				if(streetViewPosition.equals("RT"))			
					streetViewPosition = "9";
				if(streetViewPosition.equals("TC"))			
					streetViewPosition = "10";
				if(streetViewPosition.equals("TL"))			
					streetViewPosition = "11";
				if(streetViewPosition.equals("TR"))			
					streetViewPosition = "12";
			}
			
			//Carga rotate
			if(rotateControl.equals("")||rotateControl.equals(null))
				rotateControl = "false";
			
			if(rotatePosition.equals("")||rotatePosition.equals(null))
				rotatePosition = "0";
			else
			{
	 			if(rotatePosition.equals("Default"))	
	 				rotatePosition = "0";
				if(rotatePosition.equals("BC")) 	 	
					rotatePosition = "1";
				if(rotatePosition.equals("BL")) 		
					rotatePosition = "2";
				if(rotatePosition.equals("BR"))			
					rotatePosition = "3";
				if(rotatePosition.equals("LB"))			
					rotatePosition = "4";
				if(rotatePosition.equals("LC"))			
					rotatePosition = "5";
				if(rotatePosition.equals("LT"))			
					rotatePosition = "6";
				if(rotatePosition.equals("RB"))			
					rotatePosition = "7";
				if(rotatePosition.equals("RC"))			
					rotatePosition = "8";
				if(rotatePosition.equals("RT"))			
					rotatePosition = "9";
				if(rotatePosition.equals("TC"))			
					rotatePosition = "10";
				if(rotatePosition.equals("TL"))			
					rotatePosition = "11";
				if(rotatePosition.equals("TR"))			
					rotatePosition = "12";
			}
		
			contentTitle = PageContentLocalServiceUtil.getWebContentField(webContent, IterKeys.STANDARD_ARTICLE_HEADLINE, locale.toString(), 0);
			if (!contentTitle.equals(""))
			{
				contentTitle = webContent.getTitle();
			}
		}
	}
	catch (Exception e)
	{
		System.out.println("Geolocation Map: Error getting parameters for the map");
	}
%>

<c:choose>
	<c:when test="<%=withoutKey%>">

		<style type="text/css">
			.ie6 .maps-content img {behavior: expression(this.pngSet=true);}
		</style> 
			
		<% if (!emptyFields)
		{ %>
			
			<script src="http://maps.google.com/maps/api/js?sensor=false" type="text/javascript"></script>
					<div class="map" id="map" style="height: 300px; width: 100%;" cssClass="map-content-class"></div>
			<script type="text/javascript">>
			
			var array_global = new Array();
			<% for (int i=0;i<geolocationFieldsProccessed.size();i++)
			{
					String fieldTitle = geolocationFieldsProccessed.get(i).getMarkerText();
					if (fieldTitle.equals(""))
					{%>
						array_global.push( { address: '<%= geolocationFieldsProccessed.get(i).getPostalAddress().replace("'", "&#39;") %>', latlng: null, title: '<%= geolocationFieldsProccessed.get(i).getPostalAddress().replace("'", "&#39;") %>' } );
				  <%}
					else
					{%>
				  		array_global.push( { address: '<%= geolocationFieldsProccessed.get(i).getPostalAddress().replace("'", "&#39;") %>', latlng: null, title: '<%= fieldTitle %>' } );
				  <%}
			 } %>
		
			var notFirst;
			
			if (notFirst)
				InitializeMap();

			function InitializeMap()
			{
				<!-- zoom -->
				var mapZoom = <%= zoom %>;
				
				var zoomControlStyle = <%= zoomControlStyle %>;
				switch(zoomControlStyle)
				{
					case 0:
	  					zoomStyle = google.maps.ZoomControlStyle.SMALL;
	  					break;
					case 1:
					 	zoomStyle = google.maps.ZoomControlStyle.LARGE;
	  					break;
	  				case 2:
					 	zoomStyle = google.maps.ZoomControlStyle.DEFAULT;
	  					break;
	  				default:
	  					zoomStyle = google.maps.ZoomControlStyle.DEFAULT;
	  					break;
				}
				
				var zoomPosition = returnPosition(<%= zoomControlPosition %>);
				if(zoomPosition=="Default")
					zoomPosition = google.maps.ControlPosition.TOP_LEFT;
				
				<!-- typeMap -->
				var typeMap = <%= mapType %>;
				switch(typeMap)
				{
					case 1:
	  					mapTypeId = google.maps.MapTypeId.ROADMAP;
	  					break;
					case 2:
					 	mapTypeId = google.maps.MapTypeId.SATELLITE;
	  					break;
	  				case 3:
					 	mapTypeId = google.maps.MapTypeId.HYBRID;
	  					break;
	  				case 4:
					 	mapTypeId = google.maps.MapTypeId.TERRAIN;
	  					break;
	  				default:
	  					mapTypeId = google.maps.MapTypeId.ROADMAP;
	  					break;
				}
				
				var mapTypeControlStyle = <%= mapTypeControlStyle %>;
				switch(mapTypeControlStyle)
				{
					case 1:
	  					mapTypeStyle = google.maps.MapTypeControlStyle.HORIZONTAL_BAR;
	  					break;
					case 2:
					 	mapTypeStyle = google.maps.MapTypeControlStyle.DROPDOWN_MENU;
	  					break;
	  				case 3:
					 	mapTypeStyle = google.maps.MapTypeControlStyle.DEFAULT;
	  					break;
	  				default:
	  					mapTypeStyle = google.maps.MapTypeControlStyle.DEFAULT;
	  					break;
				}
				
				var mapTypePosition = returnPosition(<%= mapTypeControlPosition %>);
				if(mapTypePosition=="Default")
					mapTypePosition = google.maps.ControlPosition.TOP_RIGHT;
					
				<!-- PAN CONTROL -->
				var panPosition = returnPosition(<%= panPosition %>);
				if(panPosition=="Default")
					panPosition = google.maps.ControlPosition.TOP_LEFT;
				
				<!-- SCALE CONTROL -->
				var scalePosition = returnPosition(<%= scalePosition %>);
				if(scalePosition=="Default")
					scalePosition = google.maps.ControlPosition.BOTTOM_LEFT;
					
				<!-- streetView CONTROL -->
				var streetViewPosition = returnPosition(<%= streetViewPosition %>);
				if(streetViewPosition=="Default")
					streetViewPosition = google.maps.ControlPosition.TOP_LEFT;
					
				<!-- rotate CONTROL -->
				var rotatePosition = returnPosition(<%= rotatePosition %>);
				if(rotatePosition=="Default")
					rotatePosition = google.maps.ControlPosition.TOP_LEFT;
				
    			var myOptions = {
			        zoom: mapZoom,
					zoomControl: <%= zoomControl %>,
				  	zoomControlOptions: {
    					style: zoomStyle,
    					position: zoomPosition
  					},
  					
  					mapTypeId: mapTypeId,
					mapTypeControl: <%= mapTypeControl %>,
					mapTypeControlOptions: {
        				style: mapTypeStyle,
        				position: mapTypePosition
        			},
   					
   					panControl: <%= panControl %>,
					panControlOptions: {
        				position: panPosition
					},
					
					scaleControl: <%= scaleControl %>,
					scaleControlOptions: {
						position: scalePosition
					},
				  	
				  	streetViewControl: <%= streetViewControl %>,
				  	streetViewControlOptions: {
						position: streetViewPosition
					},
				  	
				  	overviewMapControl: <%= overviewControl %>,
				  	overviewMapControlOptions: {
						opened: <%= overviewOpened %>
					},
					
					rotateControl: <%= rotateControl %>,
					rotateControlOptions: {
						position: streetViewPosition
					},
			    };
			
			    var map = new google.maps.Map(document.getElementById("map"), myOptions);
				var array_LatLong = new Array();
				var bounds = new google.maps.LatLngBounds; 
				
				for (var i = 0; i < array_global.length; i++)
				{
					geocoder = new google.maps.Geocoder();
					var address = array_global[i].address;
					var title = array_global[i].title;
    				
    				geocoder = new google.maps.Geocoder();
          
			        geocoder.geocode({'address': address}, (function(title, array_LatLong) 
			        {
				    	return function(results, status) 
				    	{
				        	if (status == google.maps.GeocoderStatus.OK) 
				        	{
				        		array_LatLong[i] = results[0].geometry.location;
				        		map.setCenter(array_LatLong[i]);
					    		var marker = new google.maps.Marker({
					        		position: results[0].geometry.location,
									map: map,
									title: title
					    		});
					    		
					    		if (array_LatLong[i])
									bounds.extend(array_LatLong[i]);
													
								if (mapZoom == "-1" || mapZoom == "0")
									map.fitBounds(bounds);
					    		
					    		var infoWindow = new google.maps.InfoWindow({
					    			content: title
								});
					    		
							    google.maps.event.addListener(marker, 'click', function() {
							    	map.setCenter(marker.position);
		    						infoWindow.open(map, this);
								});
							}
				    	};
					}(title, array_LatLong)));
				}
				notFirst = true;
			}
			
			function returnPosition(theposition)
			{
					var position;
					switch(theposition)
					{	
						case 0:
							position = "Default";
							break;
						case 1:
		  					position = google.maps.ControlPosition.BOTTOM_CENTER;
		  					break;
						case 2:
						 	position = google.maps.ControlPosition.BOTTOM_LEFT;
		  					break;
		  				case 3:
						 	position = google.maps.ControlPosition.BOTTOM_RIGHT;
		  					break;
		  				case 4:
						 	position = google.maps.ControlPosition.LEFT_BOTTOM;
		  					break;
		  				case 5:
		  					position = google.maps.ControlPosition.LEFT_CENTER;
		  					break;
						case 6:
						 	position = google.maps.ControlPosition.LEFT_TOP;
		  					break;
		  				case 7:
						 	position = google.maps.ControlPosition.RIGHT_BOTTOM;
		  					break;
		  				case 8:
						 	position = google.maps.ControlPosition.RIGHT_CENTER;
		  					break;
		  				case 9:
		  					position = google.maps.ControlPosition.RIGHT_TOP;
		  					break;
						case 10:
						 	position = google.maps.ControlPosition.TOP_CENTER;
		  					break;
		  				case 11:
						 	position = google.maps.ControlPosition.TOP_LEFT;
		  					break;
		  				case 12:
						 	position = google.maps.ControlPosition.TOP_RIGHT;
		  					break;
		  				default:
							position = "Default";
							break;
					}
					return position;
				}
				
				google.maps.event.addDomListener(window, 'load', InitializeMap);
				
				
				window['saveMapAddress'] = function(address) 
				{
					jQryIter.ajax(
						{
							type: 'GET',
							url: '<portlet:actionURL><portlet:param name="<%= Constants.CMD %>" value="saveMapAddress" /></portlet:actionURL>',
							data: 
								{
									mapAddress: address
								}
						});
				}; 
				
			</script>
			
			
			
			<%}
			else{%>
				<c:if test="<%= (themeDisplay.isSignedIn()) %>">
				<%
					if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
					{
				%>
					<div>
						<%=defaultTextHTML%>
					</div>
				<%
					}
					else if( environment.equals(IterKeys.ENVIRONMENT_PREVIEW) )
					{
					%>
						<div class="map-content-no-results">
							<span class="portlet-msg-info">
					    		<liferay-ui:message key="map-content-no-results" />
					    	</span>
					    </div>	
					<%}%>
				</c:if>
			<%}%>
	</c:when>
	<c:otherwise>
		<c:if test="<%= themeDisplay.isSignedIn() %>">
			<liferay-ui:message key="please-contact-the-administrator-to-setup-this-portlet" />
		</c:if>
	</c:otherwise>
</c:choose>