function loginloadSWF(urlSWF, flashVars)
{
	var target_element = document.getElementById("flex-msie");
	var obj = logincreateIeObject( urlSWF, flashVars );
	target_element.parentNode.replaceChild(obj, target_element);
	
	jQryIter("#currentPortletConfig").show();
}

function logincreateIeObject(urlswf, flashvars){
   var div = document.createElement("div");
   var ovejota = "<object classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' width='100%' height='100%' id='flex-msie'>" +
   					"<param name='movie' value='" + urlswf + "'> " +
   					"<param name='quality' value='high'>" +
   		            "<param name='bgcolor' value='#ffffff'>" +
   		            "<param name='allowScriptAccess' value='sameDomain'>" +
   		            "<param name='allowFullScreen' value='true'>" +
   		            "<param name='wmode' value='transparent'>" +
   		            "<param name='flashVars' value='" + flashvars + "' id='param-flashvars-msie'/>"+
   		            "<object type='application/x-shockwave-flash' data='" + urlswf + "' width='100%' height='100%' id='flex-other'>" +
   		                "<param name='quality' value='high'>" +
   		                "<param name='bgcolor' value='#ffffff'>" +
   		                "<param name='allowScriptAccess' value='sameDomain'>" +
   		                "<param name='allowFullScreen' value='true'>" +
   			            "<param name='wmode' value='transparent'>" +
   			            "<param name='flashvars' value='" + flashvars + "' id='param-flashvars-other'/>" +
   		            "</object>" +
   				 "</object>";
   
   div.innerHTML = ovejota;
   return div.firstChild;
}