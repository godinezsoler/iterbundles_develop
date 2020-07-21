function getPollResults(groupIdvar, contentIdvar, callbackFunction){
	jQryIter.ajax(
	{
		type: "POST",
		url: "/c/portal/json_service",
		data: {
			groupId : groupIdvar,
			contentId: contentIdvar,
			serviceClassName: "com.protecmedia.iter.news.service.ArticlePollServiceUtil",
			serviceMethodName:"getPollResults",
			serviceParameters:"['groupId','contentId']",
			doAsUserId: ""
		},
		dataType: "json",
		error: function(xhr, status, error) {
			jQryIter.showAlert("error", error);
		},
		success: function(data){
			callbackFunction(data);
		}
	});
}