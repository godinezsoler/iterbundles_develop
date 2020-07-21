function executeDatePicker(urlKey, hasPrefix, language)
{
	//Retrieves URL
	var dateFormat = 'yymmdd';
	var currURL = window.document.location.href;
	var qsRegexValue = "";
	if(urlKey=="")
		qsRegexValue = ".*\/date\/(\\d{8})\/.*";
	else
		qsRegexValue = ".*\/" + urlKey + "\/date\/(\\d{8})\/.*";
		
	var qsRegex = new RegExp(qsRegexValue, "g"); 
	
	//Extracts Date from URL
	var now = new Date();
	var qsDate = jQryIter.datepicker.formatDate(dateFormat, now); 
	if (currURL.match(qsRegexValue)!=null)
	{
		 qsDate = currURL.replace(qsRegex, "$1");
	}
	
	jQryIter("#calendar-datepicker").datepicker(
			jQryIter.extend(
			{}, 
			jQryIter.datepicker.regional[language],
			{
	 			onSelect: function(date, inst) { 
		
					var dateURL = window.document.location.href;
					
					var _url = '/';
					if( hasPrefix )
						_url += '-/';
					if( urlKey!="" )
						_url += urlKey + '/';
					
					_url += 'date';
					
					if (dateURL.indexOf(_url) > 0) {
						var regexValue = "([-A-Z0-9+&@#\/%?=~_|!:,.;]*)" + _url.replace(/\//gi, '\/') + "\/\\d{8}([-A-Z0-9+&@#\/%?=~_|!:,.;]*)";
						var replacementValue = "$1" + _url + "/" + date + "$2";
						var regex = new RegExp(regexValue, "g"); 
						dateURL = dateURL.replace(regex, replacementValue);
					}
					else if (hasPrefix && dateURL.indexOf("/-/") > 0) {
						var regexValue ="([-A-Z0-9+&@#\/%?=~_|!:,.;]*)\/-\/([-A-Z0-9+&@#\/%?=~_|!:,.;]*)";
						var replacementValue = "$1" + _url + "/" + date + "$2";
						var regex = new RegExp(regexValue, "g"); 
						dateURL = dateURL.replace(regex, replacementValue);
					}
					else if(dateURL.indexOf("?") > 0){
						var regexValue = "([-A-Z0-9+&@#\/%=~_|!:,.;]*)\?([-A-Z0-9+&@#\/%=~_|!:,.;]*)";
						var replacementValue = "$1" + _url + "/" + date + "?$2";
						var regex = new RegExp(regexValue, "g"); 
						dateURL = dateURL.replace(regex, replacementValue);
					}
					else if (dateURL.lastIndexOf("/") == dateURL.length-1){
						dateURL = dateURL.concat( _url.substring(1) + "/" + date + "/" );
					}
					else{
						dateURL = dateURL.concat( _url + "/" + date + "/");
					}
					
					document.location.href=dateURL;
	 			},
			   	changeYear: true,
		      	maxDate: '0m 0d',
		 		dateFormat: dateFormat,
				defaultDate: qsDate
			}
		)
	);
}