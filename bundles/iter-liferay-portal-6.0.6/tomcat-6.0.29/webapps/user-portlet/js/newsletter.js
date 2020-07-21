var urlService = "/c/portal/json_service";

//namespace global
var ITER = ITER || {};
ITER.newsletter = ITER.newsletter || {};
ITER.newsletter.error = ITER.newsletter.error || {};

ITER.newsletter.error.EMAIL_REPEATED = "XYZ_FIELD_USER_EMAIL_REPEATED_ZYX";
ITER.newsletter.error.ACCEPT_LICENSE = "XYZ_E_NEWSLETTER_ACCEPT_LICENSE_ZYX";


ITER.newsletter.expandAllNewsletter = function ()
{
	jQryIter(".newsletters .newsletters_options:hidden").prev().removeClass("newsletters_closed").addClass("newsletters_open");
	jQryIter(".newsletters .newsletters_options:hidden").slideToggle("fast");
};

ITER.newsletter.collapseAllNewsletter = function()
{
	jQryIter(".newsletters .newsletters_options:visible").prev().removeClass("newsletters_open").addClass("newsletters_closed");
	jQryIter(".newsletters .newsletters_options:visible").slideToggle("fast");
};

ITER.newsletter.checkStatusExpanded = function()
{
	if(jQryIter(".newsletters .newsletters_options:hidden").size() == jQryIter(".newsletters .newsletters_options").size())
	{
		jQryIter(".expandCollapse").removeClass("newsletters_expanded").addClass("newsletters_collapsed");
		jQryIter(".newsletters_cab").removeClass("newsletters_open").addClass("newsletters_closed");
	}

	if(jQryIter(".newsletters .newsletters_options:visible").size() == jQryIter(".newsletters .newsletters_options").size())
	{
		jQryIter(".expandCollapse").removeClass("newsletters_collapsed").addClass("newsletters_expanded");
		jQryIter(".newsletters_cab").removeClass("newsletters_closed").addClass("newsletters_open");
	}
};

ITER.newsletter.disableLightSchedulers = function(value, checked)
{
	jQryIter(".newsletters_check :input").each(function()
	{
		jQryIter(this).prop("disabled", value);
		
		if (!(checked === undefined))
		{
			jQryIter(this)[0].checked = checked;
		}
	});
};

ITER.newsletter.checkLightFields = function()
{
	var emailId = '#newsletter_email_ctrl';
	
	// El correo solo existirá en modo Anónimo (light)
	if (jQryIter(emailId).length)
	{
		var checked = (!jQryIter('#newsletter_license_check').length || jQryIter('#newsletter_license_check').prop('checked'));
		
		// Si el correo electrónico está vacío o no está marcada la licencia se deshabilita el botón, en caso contrario se habilita
		if (jQryIter(emailId).val().length == 0 || !checked)
		{
			if (jQryIter('#newsletter_chk_subs_ctrl').length)
				jQryIter('#newsletter_chk_subs_ctrl').css("pointer-events", "none");
			
			ITER.newsletter.disableLightSchedulers(true);
		}
		else
		{
			if (jQryIter('#newsletter_chk_subs_ctrl').length)
				jQryIter('#newsletter_chk_subs_ctrl').css("pointer-events", "auto");
			
			ITER.newsletter.disableLightSchedulers(false);
		}
	}
};

ITER.newsletter.getLightNewsletterUser = function(emailRepeatedError, chkSubsError, acceptLicenseError)
{
	// Se ha aceptado si existe el control y está pulsado
	var licenseAcepted = (jQryIter('#newsletter_license_check').length && jQryIter('#newsletter_license_check').prop('checked'));
	
	var dataService = 
	{
		email: jQryIter('#newsletter_email_ctrl').val(),	
		licenseAcepted:licenseAcepted,
		ITER_HquetpesZ3rvl3tR3qu3z_: "",
		ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
		serviceClassName: "com.protecmedia.iter.base.service.NewsletterMgrServiceUtil",
		serviceMethodName:"getMyLightNewsletters",
		serviceParameters:"['email','licenseAcepted','ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_']",
		doAsUserId: ""
	};
	
	jQryIter.ajax(
	{
		type: "POST",
		url: urlService,
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) 
		{
			jQryIter.showAlert("error", error);
		},
		success: function(data)
		{
			var resultData = JSON.parse(data["returnValue"]);
			if (resultData.result === "OK")
			{
				// Se habilitan las programaciones de las Newsletters
				ITER.newsletter.disableLightSchedulers(false, false);
				
				// Se marcan aquellas a las que se ha suscrito previamente
				var options = resultData.options;
				for (var option in options)
					jQryIter('#' + options[option].id)[0].checked = true;
			}
			else
			{
				console.log(resultData.cause);
				
				if (resultData.cause == ITER.newsletter.error.EMAIL_REPEATED)
					jQryIter.showAlert("error", emailRepeatedError);
				else if (resultData.cause == ITER.newsletter.error.ACCEPT_LICENSE)
					jQryIter.showAlert("error", acceptLicenseError);
				else
					jQryIter.showAlert("error", chkSubsError);
			}
		}
	});
};

ITER.newsletter.getNewsletterUser = function()
{
	var dataService = 
	{
		ITER_HquetpesZ3rvl3tR3qu3z_: "",
		ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
		serviceClassName: "com.protecmedia.iter.base.service.NewsletterMgrServiceUtil",
		serviceMethodName:"getMyNewsletters",
		serviceParameters:"['ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_']",
		doAsUserId: ""
	};

	jQryIter.ajax(
	{
		type: "POST",
		url: urlService,
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) {
			jQryIter.showAlert("error", error);
		},
		success: function(data){
			var exception = data["exception"];
			if(exception)
			{
				jQryIter.showAlert("error", exception);
			}
			else
			{
				var returnValue = data["returnValue"];
				if(returnValue)
				{
					var options = JSON.parse(returnValue).options;
					for(var option in options)
						jQryIter('#' + options[option].id)[0].checked = true;
				}
			}
		}
	});
};

ITER.newsletter.manageNewsletter = function(id, isChecked, subscribe, unsubscribe, manageerror)
{
	var dataService = {
		optionid: id,
		suscribe: isChecked,
		ITER_HquetpesZ3rvl3tR3qu3z_: "",
		ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
		serviceClassName: "com.protecmedia.iter.base.service.NewsletterMgrServiceUtil",
		serviceMethodName:"manageNewsletter",
		serviceParameters:"['optionid','suscribe','ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_']",
		doAsUserId: ""
	};

	jQryIter.ajax({
		type: "POST",
		url: urlService,
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) 
		{
			jQryIter.showAlert("error", error);
		},
		success: function(data)
		{
			var resultData = data["returnValue"];
			var result = JSON.parse(resultData).result;
			if (result === "OK")
			{
				if(dataService.suscribe)
					jQryIter.showAlert("info", subscribe);
				else
					jQryIter.showAlert("info", unsubscribe);
			}
			else
			{
				jQryIter.showAlert("error", manageerror);
				jQryIter('#' + dataService.optionid)[0].checked = !dataService.suscribe;
			}
		}
	});
};

ITER.newsletter.manageLightNewsletter = function(id, isChecked, subscribe, unsubscribe, manageerror, emailRepeatedError, acceptLicenseError)
{
	// Se ha aceptado si existe el control y está pulsado
	var licenseAcepted = (jQryIter('#newsletter_license_check').length && jQryIter('#newsletter_license_check').prop('checked'));

	var dataService = {
		email: jQryIter('#newsletter_email_ctrl').val(),
		licenseAcepted: licenseAcepted,	
		optionid: id,
		suscribe: isChecked,
		ITER_HquetpesZ3rvl3tR3qu3z_: "",
		ITER_HquetpesZ3rvl3tR3zp0nz3_: "",
		serviceClassName: "com.protecmedia.iter.base.service.NewsletterMgrServiceUtil",
		serviceMethodName:"manageLightNewsletter",
		serviceParameters:"['email','licenseAcepted','optionid','suscribe','ITER_HquetpesZ3rvl3tR3qu3z_','ITER_HquetpesZ3rvl3tR3zp0nz3_']",
		doAsUserId: ""
	};

	jQryIter.ajax({
		type: "POST",
		url: urlService,
		data: dataService,
		dataType: "json",
		error: function(xhr, status, error) 
		{
			jQryIter.showAlert("error", error);
		},
		success: function(data)
		{
			var resultData = JSON.parse(data["returnValue"]);
			var result = resultData.result;
			if (result === "OK")
			{
				if(dataService.suscribe)
					jQryIter.showAlert("info", subscribe);
				else
					jQryIter.showAlert("info", unsubscribe);
			}
			else
			{
				if (resultData.cause == ITER.newsletter.error.EMAIL_REPEATED)
					jQryIter.showAlert("error", emailRepeatedError);
				else if (resultData.cause == ITER.newsletter.error.ACCEPT_LICENSE)
					jQryIter.showAlert("error", acceptLicenseError);
				else	
					jQryIter.showAlert("error", manageerror);
				
				jQryIter('#' + dataService.optionid)[0].checked = !dataService.suscribe;
			}
		}
	});
};