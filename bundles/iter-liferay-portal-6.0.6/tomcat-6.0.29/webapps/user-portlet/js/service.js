Liferay.Service.register("Liferay.Service.User", "com.protecmedia.iter.user.service");

Liferay.Service.registerClass(
	Liferay.Service.User, "Login",
	{
		doLogin: true,
		doLogout: true,
		doSimulationLogout: true,
		importData: true,
		publish: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "UserProfile",
	{
		getUserProfile: true,
		addField: true,
		updateField: true,
		deleteFields: true,
		publishToLive: true,
		importFields: true,
		updateProfileFieldsIds: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "SocialMgr",
	{
		getSocialConfig: true,
		setSocialConfig: true,
		updateScopeSocialConfig: true,
		getProfileAndScopes: true,
		updateProfileSocialField: true,
		getProfileSocialFieldsConnections: true,
		deleteLoginWithFileEntry: true,
		getSocialButtonsHTML: true,
		initStatisticsTasks: true,
		updateSocialStatisticsTask: true,
		stopSocialStatisticsTask: true,
		exportData: true,
		importData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "HandlerFormMgr",
	{
		startHandlerDatabaseForm: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "CaptchaForm",
	{
		isValid: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "FormReceivedToCsv",
	{
		generateCSV: true,
		generateCSVTranslated: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "FileFormReceivedMgr",
	{
		getFile: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "IterUserMng",
	{
		getUsersId: true,
		getGridValues: true,
		exportUserMngToXls: true,
		deleteUsers: true,
		getUserDetailById: true,
		getUserInfo: true,
		updateUserInfo: true,
		GetPasswordSuperUser: true,
		SetPasswordSuperUser: true,
		GetEncodedPass: true,
		subscribeUsersToNewsletters: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "IterRegister",
	{
		preRegisterUser: true,
		deleteExpiredUsers: true,
		initOrUpdateUserToDeleteTask: true,
		getUserCredentials: true,
		getDelayToDeleteExpiredUsers: true,
		unregisterUser: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "UserOperations",
	{
		getConfig: true,
		setConfig: true,
		exportData: true,
		importData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.User, "FormReceivedMgr",
	{
		getForms: true,
		deleteFormsReceivedFromForm: true,
		deleteFormsReceived: true,
		getInputCtrl: true,
		getReceivedDataForm: true,
		getGenericReceivedDataForm: true,
		putCheck: true,
		putListCheck: true,
		getFilesFromFormReceived: true,
		getFormReceivedDetail: true,
		exportFormsToCSV: true,
		getGridValues: true
	}
);