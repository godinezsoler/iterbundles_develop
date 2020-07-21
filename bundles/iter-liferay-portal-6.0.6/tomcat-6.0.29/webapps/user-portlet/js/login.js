function doLogout()
{
	function cbDoLogout(msg) 
	{
		var exception = msg.exception;
		
		if(!exception)
		{
			if (msg.returnValue.sso)
				window.location.assign(msg.returnValue.sso);
			else
				window.location.reload(true);
		}
		else
		{
			var idx = exception.indexOf(":");
			var strExcep = exception.substring(idx+1);
			showError(jQryIter(".errTitle")[0].val(), "OK", strExcep);
		}	            
	}

	jQryIter.ajax(
	{
		type: "POST",
		url: "/restapi/user/logout",
		dataType: "json",
		error: function(xhr, status, error) {
			jQryIter.showAlert("error", error);
		},
		success: function(data, status, xhr){
			cbDoLogout(data);
		}
	});
}