var unregisterDialog;

function showUnregisterDialog(
		panelTitle, unregisterConfirmButton, enterPasswordText, 
		confirmPasswordText, unregisterSuccessMsg, unregisterErrorMsg, 
		badCredentialsMsg, okDialogButton)
{
	if(!unregisterDialog)
	{
		unregisterDialog = jQryIter('<div></div>').html(
				'<form id="unregisterForm">' + 
						'<div class="passwordWrapper">' +
						'<label class="passwordLabel">' + enterPasswordText + '</label>' + 
						'<input id="unregisterInputPassword1" class="inputPassword" type="password" required>' + 
					'</div>' + 
					'<div class="passwordWrapper">' +
						'<label class="passwordLabel">' + confirmPasswordText + '</label>' + 
						'<input id="unregisterInputPassword2" class="inputPassword" type="password" required>' + 
					'</div>' + 
				'</form>').dialog({
					dialogClass: "d-modal",
					modal: true,
					autoOpen: false,
					title: panelTitle,
					open: function(event, ui)
					{
						jQryIter(".ui-widget-overlay").css({
							background: "rgb(54, 54, 54)",
							background: "rgba(54, 54, 54, 0.5)",
							height: "100%",
							width: "100%",
							left: "0",
							top: "0",
							position: "fixed",
							"z-index": "9999999"
						});
						jQryIter("input[id*='unregisterInputPassword']").on('input', function() {checkUnregisterButton();});
					},
					buttons:[
					          	{text: unregisterConfirmButton, click: function() {unregisterUser(unregisterSuccessMsg, unregisterErrorMsg, badCredentialsMsg, panelTitle, okDialogButton);}}
					        ]
				});
	}
	
	unregisterDialog.dialog('open');
	jQryIter("#unregisterInputPassword1").val("");
	jQryIter("#unregisterInputPassword2").val("");
	checkUnregisterButton();
};

function checkUnregisterButton()
{
	var value1 = jQryIter("#unregisterInputPassword1").val();
	var value2 = jQryIter("#unregisterInputPassword2").val();

	if(value1 && value2 && value1 === value2)
		jQryIter('.ui-dialog-buttonpane').find('button:first').css('visibility', 'visible');
	else
		jQryIter('.ui-dialog-buttonpane').find('button:first').css('visibility', 'hidden');
}

function unregisterUser(unregisterSuccessMsg, unregisterErrorMsg, badCredentialsMsg, panelTitle, okDialogButton)
{
	var password1 = jQryIter("#unregisterInputPassword1").val();
	var password2 = jQryIter("#unregisterInputPassword2").val();
	
	var dataService = {
		ITER_HquetpesZ3rvl3tR3qu3z_: "",
		ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
		password1: password1,
		password2: password2,
		serviceClassName: "com.protecmedia.iter.user.service.IterRegisterServiceUtil",
		serviceMethodName:"unregisterUser",
		serviceParameters:"['ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_','password1','password2']",
		doAsUserId: ""
	};

	jQryIter.ajax({
		type: "POST",
		url: "/c/portal/json_service",
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) {
			alert(error);
		},
		success: function(data){
			var resultData = data["returnValue"];
			var json = JSON.parse(resultData);
			if (json.result === "OK")
				showInfo(panelTitle, okDialogButton, unregisterSuccessMsg, json.sso ? json.sso : "", true);
			else if(json.result === "KO")
				showError(panelTitle, okDialogButton, unregisterErrorMsg);
			else
				showError(panelTitle, okDialogButton, badCredentialsMsg);
		}
	});
}