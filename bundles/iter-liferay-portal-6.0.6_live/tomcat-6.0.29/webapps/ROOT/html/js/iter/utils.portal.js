function refreshPage()
{
	window.location.reload(true);
}

function closeWindow()
{
	unloadIfExistsSWF();
}

function unloadIfExistsSWF()
{
	if( document.getElementById("flex-other") || document.getElementById("flex-msie").getAttribute("classid"))
		unloadSWF();
}

function unloadSWF()
{
	var target_element = document.getElementById("flex-msie");
	var div = document.createElement("div");
	var divFlexMsie = "<div id='flex-msie'></div>";
	div.innerHTML = divFlexMsie;
	target_element.parentNode.replaceChild(div.firstChild, target_element);			
	jQryIter("#currentPortletConfig").hide();
	placeSWF('restore', 0);
}

function placeSWF(type, headerHeight)
{
	var styleValue = "";
	var topPos = "";
	
	if(type=='minimize')
		topPos = jQryIter(window).height() - headerHeight;
	else 
		if (type == 'restore')
			topPos = 0;
	
	styleValue = jQryIter("#currentPortletConfig").attr("style").replace(/top:.+?px;/i, "TOP:" + topPos + "px;");
	jQryIter("#currentPortletConfig").attr("style", styleValue);
}