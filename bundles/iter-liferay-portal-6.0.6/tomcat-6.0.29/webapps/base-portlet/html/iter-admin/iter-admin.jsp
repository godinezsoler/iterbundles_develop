<%@page import="com.protecmedia.iter.xmlio.model.LiveConfiguration"%>
<%@page import="com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.util.BaseUtil"%>
<%@page import="java.io.File"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.service.CompanyLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.Company"%>
<%@page import="java.util.List"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>

<html lang="en">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<title></title>
	</head>
	<style>
		body { margin: 0px; overflow:hidden;  height:100%}
	</style>
	
	<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?env=preview'></script>
<%
	boolean debug = ParamUtil.getBoolean(request, IterKeys.REQUEST_PARAMETER_FLEX_DEBUG);
	String currentURL = request.getRequestURL().toString();
	String urlPortal = currentURL.substring(0, currentURL.indexOf(request.getContextPath()));
%>	
	<script type='text/javascript'> 
		var jQryIter = jQuery.noConflict(true);
		var debug = "<%=debug%>";
	</script> 
	
	<input id="disqusConfMileniumMsg" hidden="true"/>
	
	<script type="text/javascript">	
		window.onload = function()
		{
			setTimeout("setAppFocus()",1000);
		}
		function setAppFocus() 
		{
			document["mySwf"].focus();
		}
		
		function openEditCKEditor(lg, w, h, titleW, param, cbinheritcssmsg) 
		{
			if(cbinheritcssmsg)
				cbinheritcssmsg = encodeURIComponent(escape(cbinheritcssmsg));

			jQryIter("#disqusConfMileniumMsg").attr('value', param);

			var popup = window.open('/base-portlet/html/components/ckEditor.jsp?w='+w+'&h='+h+'&lg='+lg+
									'&input=disqusConfMileniumMsg&cbinheritcssmsg=' + cbinheritcssmsg,
									titleW, 'width='+w+',height='+h+',scrollbars=no,resizable=yes,location=no');
		}
		function llamaAlFlex(param)
		{
			document["mySwf"].llamaAlFlexCallback(param);
		}
		
		/**
		* Función que llamará al método catalogPageCallback del swf cuando se ha ejecutado con éxito 
			el guardado en ventana de maquetación de un elemento de catálogo.
		**/
		function catalogPageCallback(data)
		{
			document["mySwf"].catalogPageCallback(data);
		}
		
		/**
		* Función que llamará al método catalogPageCloseCallback del swf cuando se ha pulsado el botón cerrar
			en ventana de maquetación de un elemento de catálogo.
		**/
		function catalogPageCloseCallback()
		{
			document["mySwf"].catalogPageCloseCallback();
		}

		
		/**
		* Función que devuelve 'true' si el obj está definido, no es null y tiene asignado algún valor.
		**/
		function isNotEmpty(obj)
		{
			return (typeof obj != 'undefined') && (obj != null) && (obj.toString().length > 0);
		}
		
		/**
		* Función que carga una ventana HTML con un SWF embebido
		*
		* @wh: 				ancho y alto de la ventana
		* @title: 			título de la ventana
		* @swfname: 		nombre del swf que cargará
		* @includeHeadJSP:	ruta del JSP que se incluirá en el head del HTML
		* @includeBodyJSP:	ruta del JSP que se incluirá en el body del HTML
		* @urlParams:		parámetros específicos que necesita la aplicación
		**/
		function openFlexHost(w, h, title, swfname, includeHeadJSP, includeBodyJSP, urlParams)
		{
			var params ='w='+w+'&h='+h+'&'+"<%=IterKeys.REQUEST_PARAMETER_FLEX_DEBUG%>"+'='+debug+
						'&'+"<%=IterKeys.REQUEST_PARAMETER_SWFNAME%>"+'='+swfname;
			
			// Path del jsp a incluir en la cabecera
			if ( isNotEmpty(includeHeadJSP) )
				params += '&'+"<%=IterKeys.REQUEST_PARAMETER_INCLUDE_HEAD_JSP%>"+'='+includeHeadJSP;
			
			// Path del jsp a incluir en el body
			if ( isNotEmpty(includeBodyJSP) )
				params += '&'+"<%=IterKeys.REQUEST_PARAMETER_INCLUDE_BODY_JSP%>"+'='+includeHeadJSP;
			
			// Parámetros específicos de la aplicación/ventana que se abrirá
			if ( isNotEmpty(urlParams) )
				params += '&'+urlParams;
			
			var windowHeight = screen.availHeight;
			 
			var popup = window.open("<%=urlPortal%>" +'/base-portlet/html/components/flexHost/flexHost.jsp?env=preview&'+params,
					title, 'width='+w+',height='+windowHeight+',scrollbars=no,location=no');
			
			popup.moveTo(0,0);
			popup.resizeTo( w,windowHeight );
			
		}
		
		/**
		* Función que abre una ventana HTML con una app Flex, que a su vez tiene un iFrame con el HTML
		* de edición de una página de Iter.
		**/
		function openCatalogPageDesigner(w, h, title, catalogpageurl, catalogpageid , scopeGroupId, catalogname, catalogpagename )
		{
			var login = '<%=PropsValues.ITER_LOGIN_USER%>';
			var password = '<%=PropsValues.ITER_LOGIN_PASSWORD%>';
			
			var urlParams = "<%=IterKeys.REQUEST_PARAMETER_CATALOG_PAGE_URL%>"  +'='+catalogpageurl
			urlParams    += '&'+"<%=IterKeys.REQUEST_PARAMETER_CATALOG_PAGEID%>"+'='+catalogpageid;
			urlParams    += '&'+"<%=IterKeys.REQUEST_PARAMETER_CATALOG_SCOPEGROUPID%>"+'='+scopeGroupId;
			urlParams    += '&'+"<%=IterKeys.REQUEST_PARAMETER_CATALOG_NAME%>"+'='+catalogname;
			urlParams    += '&'+"<%=IterKeys.REQUEST_PARAMETER_CATALOG_PAGE_NAME%>"+'='+catalogpagename;
			if( login != "")
				urlParams    += '&'+"<%=IterKeys.REQUEST_PARAMETER_LOGIN%>"+'='+login ;
			if( password != ""  )
				urlParams    += '&'+"<%=IterKeys.REQUEST_PARAMETER_PASSWORD%>"+'='+password;
			
			openFlexHost(w, h, title, "catalogpage_designer", "catalogpage_designer_head.jsp", null, urlParams);
		}
		
		/* ****************************************************************************************************************/
		/* Funciones para la gestion del mensaje de aviso que se mostrará antes de salir de la página de configuración de IterAdmin 
		/* ****************************************************************************************************************/
		
		//mensaje
		var exitPagemsg = "";
		
		function setExitPageMsg(msg)
		{
			exitPagemsg = msg;
		}
		
		function showMessage()
		{
			var result = exitPagemsg;
			return result;
		}
		
		/**
		* Función que se llamará desde el swf cuando estamos en la página de configuración de IterAdmin 
			para que se muestre un mensaje de aviso al salir
		**/
		function showMsgBeforeUnload()
		{
			window.onbeforeunload = showMessage;
		}
		
		/**
		* Función que se llamará desde el swf cuando estamos en la página de login
			para que no se muestre un mensaje de aviso al salir
		**/
		function notShowMsgBeforeUnload()
		{
			window.onbeforeunload = null;
		}
		
		/* ****************************************************************************************************************/

	</script>
	<body scroll="no">
<%			 
	long globalId = -1;
	long companyId = -1;
	
	List<Company> companies = CompanyLocalServiceUtil.getCompanies();
	for (Company company : companies) {
		globalId = company.getGroup().getGroupId();
		companyId = company.getCompanyId();
	}


	
	//versión del producto IterWebCMS
	String productVersion =   IterKeys.PRODUCT_VERSION;
	
	//versión de IterWebCMS
	String iterWebCmsVersion = IterGlobal.getIterWebCmsVersion();
	
	//wsdl mileniumWebService
	String wsdlMlnWS = PropsValues.ITER_MLNWS_URL;
	
	//para determinar si el acceso a ITERAdmin se ha producido a través del Tomcat o del apache pasarela
	//Si apache pasarela: request.getHeader("ITER_GATEWAY") ="1". 
	//Si tomcat: request.getHeader("ITER_GATEWAY") no definido
	String headervalue = "";
	if( request.getHeader("ITER_GATEWAY") != null)
	 	headervalue = request.getHeader("ITER_GATEWAY"); 
	
	LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
	String gatewayHost = "";
	if(liveConf!=null)
		gatewayHost = liveConf.getGatewayHost();
	
	String ext = (!debug) ? ".swf" : "-debug.swf";
	
	StringBuilder sbParams = new StringBuilder();
	sbParams.append("&urlPortal=").append(urlPortal);
	sbParams.append("&environment=").append(IterLocalServiceUtil.getEnvironment());
	sbParams.append("&companyId=").append(String.valueOf(companyId));
	sbParams.append("&globalGroupId=").append(String.valueOf(globalId));
	sbParams.append("&productVersion=").append(productVersion );
	sbParams.append("&iterWebVersion=").append(iterWebCmsVersion );
	sbParams.append("&debugMode=").append(String.valueOf(debug));
	sbParams.append("&userId=").append(GroupMgr.getDefaultUserId());
	sbParams.append("&wsdlMlnWS=").append(wsdlMlnWS);
	sbParams.append("&iterGateWay=").append(headervalue);
	sbParams.append("&gatewayHost=").append(gatewayHost);
	
	String urlParams = sbParams.toString();

	
	File webappsDir = new File(PortalUtil.getPortalWebDir()).getParentFile();
	StringBuilder swfAppPath = new StringBuilder( webappsDir.getAbsolutePath() );
	swfAppPath.append("/base-portlet/swf/iter-admin").append(ext);
	File swfAppFile = new File( swfAppPath.toString() );
	
	String swfLastModified = swfAppFile.exists() ? ("?t="+swfAppFile.lastModified()+"&env=preview") : "?env=preview";
	
	String swf = new StringBuilder("/base-portlet/swf/iter-admin").append(ext).append(swfLastModified).toString();	
	
%>
		<object id='mySwf' classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' width='100%' height='100%'>
			<param name='src' value='<%=swf%>' />
			<param name='flashVars' value='<%=urlParams%>' />
			<embed name='mySwf' src='<%=swf%>' pluginspage='http://www.adobe.com/go/getflashplayer' 
				   flashVars='<%=urlParams%>' height="100%" width="100%" />
		</object>
	</body>
</html>
