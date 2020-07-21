var ITER = ITER || {};
ITER.PORTLET_COMPLETE = ITER.PORTLET_COMPLETE || {};

ITER.PORTLET_COMPLETE.launchAll = function()
{
	jQuery("._tc, ._rc").each(
	function()
	{
	    var el = jQuery(this);
	    var id = el.attr("id");
	    var type = "";
	    
	    if (el.hasClass("_tc"))
	    	type = "teaserCompleteLoad";
	    
	    else if (el.hasClass("_rc"))
	    	type = "rankingCompleteLoad";
	    
	    jQuery(document).trigger(type, id);
	});
};

jQryIter(document).ready(ITER.PORTLET_COMPLETE.launchAll);


