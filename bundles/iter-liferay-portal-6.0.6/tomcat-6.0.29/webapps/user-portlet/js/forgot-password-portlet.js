function enableSendButton()
{
	if( jQryIter("#emailinput").val().trim()!="" && (jQryIter("#namecheck").is(':checked')||jQryIter("#pwdcheck").is(':checked')))
	{
		jQryIter("#sendbtt").removeClass('disabled');
		jQryIter("#sendbtt").removeAttr("disabled");
	}
	else
	{
		jQryIter("#sendbtt").addClass('disabled');
		jQryIter("#sendbtt").attr("disabled", "disabled");
	}
}

function onKeyDown(event)
{
	var keycode = (event.keyCode ? event.keyCode : event.which);
	//keycode = 9 = TAB
	if(keycode==9)
	{
		if(event.shiftKey && event.target.id=="emailinput")
		{
			event.preventDefault();
			jQryIter("#sendbtt").focus();
		}
		else if(!event.shiftKey && event.target.id=="sendbtt")
		{
			event.preventDefault();
			jQryIter("#emailinput").focus();
		}
		
	}
}

function sendReminder(id, referer)
{

	var email = jQryIter("#emailinput").val();
	var namechecked = jQryIter("#namecheck").is(':checked');
	var pwdchecked = jQryIter("#pwdcheck").is(':checked');
	
	 function cbGetCredentials(msg) 
	 {
    	var exception = msg.exception;
    	
    	if(!exception)
 		{
    		jQryIter.each(msg, function(key, value){
    			var jsonObj = JSON.parse(value);
    			if(jsonObj.form)
				{
					drawForm( jsonObj );
				}
    			else
    				{
    					drawEmailMsg(jsonObj.email);
    				}
    		});
 		}
    	else
   		{
    		var idx = msg.exception.indexOf(":");
    		var strExcep = msg.exception.substring(idx+1);
    		showError(jQryIter("#forgotTitle").val(), jQryIter("#forgotBttText").val(), strExcep);
   		}	            
     }
	 
	var dataService = {
		groupid: id,
		email:	email,
		isnamechecked:	namechecked,
		ispwdchecked:	pwdchecked,
		refererurl: referer,
		ITER_HquetpesZ3rvl3tR3qu3z_: "",
		ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
		serviceClassName: "com.protecmedia.iter.user.service.IterRegisterServiceUtil",
		serviceMethodName:"getUserCredentials",
		serviceParameters:"['groupid','email','isnamechecked','ispwdchecked','refererurl','ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_']",
		doAsUserId: ""
	};

	jQryIter.ajax({
		type: "POST",
		url: "/c/portal/json_service",
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) {
			jQryIter.showAlert("error", error);
		},
		success: function(data){
			cbGetCredentials(data);
		}
	});
 
};

function drawForm( jsonObj )
{
	var parentTag = jQryIter("#forgotpasswordform").parent();
	jQryIter("#forgotpasswordform").remove();
	parentTag.append( jsonObj.form );
	
	jQryIter('form').submit(function() 
	{
		function cbCheckChallenge(msg)
		{
			jQryIter.each(msg, function(key, value)
			{
				var cbJsonObj = JSON.parse(value);
				if(!exception)
				{
					showInfo( jQryIter("#forgotTitle").val(), jQryIter("#forgotBttText").val(), cbJsonObj.email, "", false);
				}
				else
				{
					var idx = msg.exception.indexOf(":");
			    	var strExcep = msg.exception.substring(idx+1);
			    	showError(jQryIter("#forgotTitle").val(), jQryIter("#forgotBttText").val(), strExcep);
				}
			});
		}
//		No se pasa por aqui por el momento, se esta utilizando captcha de google. La función del servidor esta comentada.  
//		
//		var dataService = {
//			challresp: jQryIter("#" + jsonObj.challresp).val(),
//			ITER_HquetpesZ3rvl3tR3qu3z_: "",
//			ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
//			serviceClassName: "com.protecmedia.iter.user.service.IterRegisterServiceUtil",
//			serviceMethodName:"checkChallenge",
//			serviceParameters:"['challresp','ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_']",
//			doAsUserId: ""
//		};
//
//		jQryIter.ajax({
//			type: "POST",
//			url: "/c/portal/json_service",
//			data: dataService,
//			dataType: "json",
//			error: function(xhr, status, error) {
//				alert(error);
//			},
//			success: function(data){
//				cbCheckChallenge(data);
//			}
//		});
//		
		return false; 
	});
};

function drawEmailMsg( emailMsgHTML )
{
	var parentTag = jQryIter("#forgotpasswordform").parent();
	jQryIter("#forgotpasswordform").remove();
	parentTag.append( emailMsgHTML );
	
	if(parentTag.html().indexOf("ckeditor-wrapper-content") !== -1)
		ckEditorWrapperContent();
};
