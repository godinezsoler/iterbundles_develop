<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%
	StringBuilder urlParams = (StringBuilder)request.getAttribute("urlParams");
	
	String catalogPageUrl 	= ParamUtil.getString(request, IterKeys.REQUEST_PARAMETER_CATALOG_PAGE_URL, ""  );
	String catalogPageId = ParamUtil.getString(request, IterKeys.REQUEST_PARAMETER_CATALOG_PAGEID, "0" );
	String defaultplid = ParamUtil.getString(request, IterKeys.REQUEST_PARAMETER_DEFAULT_PLID, "" );
	String scopegroupid = ParamUtil.getString(request, IterKeys.REQUEST_PARAMETER_CATALOG_SCOPEGROUPID, "" );
	
	if (Validator.isNotNull(catalogPageUrl))
		request.setAttribute( "urlParams", urlParams.append("&catalogpageurl=").append(catalogPageUrl) );

//	if (Validator.isNotNull( defaultplid ) )
		request.setAttribute( "urlParams", urlParams.append("&defaultplid=").append(defaultplid) );
		
	if (Validator.isNotNull(scopegroupid))
			request.setAttribute( "urlParams", urlParams.append("&scopeGroupId=").append(scopegroupid) );
	
%>


<script language="javascript">	
	var catalogpageid = '<%=catalogPageId%>';
	var catalogpageurl= '<%=catalogPageUrl%>';
	
	
// 	window.addEventListener("beforeunload", function (e) 
// 	{
// 		return confirm("Are you sure??");
// // 		if (/Firefox[\/\s](\d+)/.test(navigator.userAgent) && new Number(RegExp.$1) >= 4) 
// // 		{
// // 	        if(confirm("Are you Sure do you want to leave?")) 
// // 	        {
// // 	            history.go();
// // 	        } 
// // 	        else 
// // 	        {
// // 	            window.setTimeout(function() 
// // 	            {
// // 	                window.stop();
// // 	            }, 1);
// // 	        }
// // 	    } 
// // 		else 
// // 		{
// // 	        return "Are you Sure do you want to leave?";
// // 	    }
		
		
		
// // 		  var confirmationMessage = "Hello world";

// // 		  (e || window.event).returnValue = confirmationMessage;     //Gecko + IE
// // 		  return confirmationMessage;                                //Webkit, Safari, Chrome etc.
// 	});
/* 	window.onbeforeunload=function()
	{
   	return "Are you sure??";
	} */
	

	function catalogPageCallback(data)
	{
		document['<%=ParamUtil.getString(request,IterKeys.REQUEST_PARAMETER_SWFNAME)%>'].catalogPageCallback(data);
	}
	
</script>