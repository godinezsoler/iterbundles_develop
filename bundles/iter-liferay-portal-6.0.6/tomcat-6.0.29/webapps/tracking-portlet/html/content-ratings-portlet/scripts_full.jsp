<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<script type="text/javascript">

function <portlet:namespace />getRatings()
{
	jQryIter.get('<%=path%>',
		 function(data)
		 {
			jQryIter("span[name='star-total']").each(function(){jQryIter(this).html(data.total);});
			showRedStar(data.average);

			if(navigator.cookieEnabled)
			{
					if(document.cookie.indexOf('<%=IterKeys.COOKIE_NAME_ITR_COOKIE_USRID %>') >= 0)
						<portlet:namespace />rating(data.average);
			}
			else
			{
					<portlet:namespace />showRatingsMessage(2);
			}
		},
        "json"
 	);
}

function <portlet:namespace />showRatingsMessage(message)
{
   	var displayMessage = "";
   	switch(message)
   	{
    	case 0:
			displayMessage =  "<%=StringEscapeUtils.escapeJavaScript(alreadyVoted)%>";
			break;
   		case 1:
			displayMessage = "<%=StringEscapeUtils.escapeJavaScript(thanksMessage)%>";
			break;
		default:
			displayMessage =  "<%=StringEscapeUtils.escapeJavaScript(deactivatedCookies)%>";
   	}
   	
   	jQryIter.showAlert("info", displayMessage);
}	

function <portlet:namespace />rating(star_value)
{
	jQryIter("a span[name^='star-rating']").unbind('mouseover');
	jQryIter("a span[name^='star-rating']").mouseover(function(){showRedStar(jQryIter(this).prop('title'));});
		        
	jQryIter("a span[name^='star-rating']").unbind('mouseout');
	jQryIter("a span[name^='star-rating']").mouseout(function(){showRedStar(star_value);});
		        
	jQryIter("a span[name^='star-rating']").unbind('click');
	jQryIter("a span[name^='star-rating']").click(function()
	{
		if( document.cookie.indexOf('<%=IterKeys.COOKIE_NAME_ITR_COOKIE_USRID %>') >= 0)
      	{
      		if(document.cookie.indexOf('<%=cookieAlready %>') < 0)
      		{
           		var selected_star = jQryIter(this).prop('title');
            	jQryIter.post('<%=servletPath %>',{
            		star: selected_star, 
            		contentId: '<%=contentId%>',
				    scopeGroupId: '<%=themeDisplay.getScopeGroupId()%>'
            		},
                	function(data)
                	{
                		jQryIter("span[name='star-total']").each(function(){jQryIter(this).html(data.total);});
                		showRedStar(data.average);
                		<portlet:namespace />rating(data.average);
	                    <portlet:namespace />showRatingsMessage(data.message);
               		},
                	"json"
            	);
	        }
	        else
	        {
	        	<portlet:namespace />showRatingsMessage(0);
	        }
       }
       else
       {
       		<portlet:namespace />showRatingsMessage(2);
       }
   });
}

<portlet:namespace />getRatings();

</script>