<%@page import="java.io.File"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>

<html lang="en">
<%
	boolean debug  			= ParamUtil.getBoolean(request, IterKeys.REQUEST_PARAMETER_FLEX_DEBUG);
	String swfName 			= ParamUtil.getString(request,	IterKeys.REQUEST_PARAMETER_SWFNAME);
	String includeHeadJSP 	= ParamUtil.getString(request, 	IterKeys.REQUEST_PARAMETER_INCLUDE_HEAD_JSP);
	String includeBodyJSP 	= ParamUtil.getString(request, 	IterKeys.REQUEST_PARAMETER_INCLUDE_BODY_JSP);
%>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<title></title>
		
		<script type='text/javascript' src='/html/js/jquery/jqueryiter.js?env=preview'></script>
		<script type='text/javascript' src='/html/js/iter/jqryiter-ext.js?env=preview'></script>
		
        <style type="text/css" media="screen"> 
            html, body  { height:100%; }
            body { margin:0; padding:0; overflow:auto; text-align:center; background-color: #ffffff; }   
            object:focus { outline:none; }
            #flashContent { display:none; }
        </style>
        
<%
	String currentURL = request.getRequestURL().toString();
	String urlPortal  = currentURL.substring(0, currentURL.indexOf(request.getContextPath()));
	StringBuilder urlParams  = new StringBuilder("");
	if (Validator.isNotNull(urlPortal))
		urlParams = urlParams.append("urlPortal=").append(urlPortal);

	request.setAttribute("urlParams", urlParams);
	
	if (Validator.isNotNull(includeHeadJSP))
	{
%>
<jsp:include page='<%=includeHeadJSP%>' flush="true"/>
<%		
	}
	
	
	String ext 				= (debug) ? "-debug.swf" : ".swf";
	String swfFullName		= new StringBuilder(swfName).append(ext).toString();
	
	File webappsDir 		= new File(PortalUtil.getPortalWebDir()).getParentFile();
	StringBuilder swfAppPath= new StringBuilder( webappsDir.getAbsolutePath() ).append("/base-portlet/swf/").append(swfFullName);
	File swfAppFile 		= new File( swfAppPath.toString() );
	
	String swfLastModified = swfAppFile.exists() ? ("?t="+swfAppFile.lastModified()+"&env=preview") : "?env=preview";
	String swf = new StringBuilder("../../../swf/").append(swfFullName).append(swfLastModified).toString();
%>
	<script type="text/javascript" src="swfobject.js?env=preview"></script>
	<script type="text/javascript">
           // To use express install, set to playerProductInstall.swf, otherwise the empty string. 
           var flashvars 				= '<%=request.getAttribute("urlParams").toString()%>';
           var params 					= {};
           params.quality 				= "high";
           params.allowscriptaccess 	= "sameDomain";
           params.allowfullscreen 		= "true";
           var attributes 				= {};
           attributes.id 				= '<%=swfName%>';
           attributes.name 				= '<%=swfName%>';
           attributes.align 			= "middle";
           
		swfobject.embedSWF(	'<%=swf%>', "flashContent", "100%", "100%", "11.1.0", "", 
							flashvars, params, attributes );
			
           // JavaScript enabled so display the flashContent div in case it is not replaced with a swf object.
           swfobject.createCSS("#flashContent", "display:block;text-align:left;");
      </script>
	</head>

	<body scroll="no">
		
<%
	if (Validator.isNotNull(includeBodyJSP))
	{
%>
		<jsp:include page='<%=includeBodyJSP%>' flush="true"/>
<%		
	}

	request.removeAttribute("urlParams");
%>
		<div id="flashContent"/>
	</body>
</html>	
