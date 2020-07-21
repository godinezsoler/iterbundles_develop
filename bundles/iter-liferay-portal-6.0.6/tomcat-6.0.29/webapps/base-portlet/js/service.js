Liferay.Service.register("Liferay.Service.Base", "com.protecmedia.iter.base.service");

Liferay.Service.registerClass(
	Liferay.Service.Base, "Iter",
	{
		testIterError: true,
		getSystemInfo: true,
		getSystemInfoEncoded: true,
		getVirtualHostIterAdmin: true,
		saveConfigIterAdmin: true,
		setMobileVersionConf: true,
		getMobileVersionConf: true,
		publishToLiveMobileVersionConf: true,
		importData: true,
		getSystemProperties: true,
		getTraceLevels: true,
		updateLogLevels: true,
		cacheSingle: true,
		cacheMulti: true,
		cacheDb: true,
		reindex: true,
		launchURIPteProcess: true,
		stopURIPteProcess: true,
		getLegacyUrlRules: true,
		setLegacyUrlRules: true,
		getGlobalJournalTemplates: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "Communities",
	{
		importDataSearch: true,
		publishSearch: true,
		importDataLastUpdate: true,
		publishLastUpdate: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "ImageResolution",
	{
		importResolutions: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "DevelopmentTools",
	{
		getLayoutTypeSettings: true,
		clearDBCache: true,
		clearVMCache: true,
		clearAllCaches: true,
		rebuildAssetCategoryTree: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "DLFileEntryMgr",
	{
		getFileEntries: true,
		deleteFileEntries: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "ContextVarsMgr",
	{
		getLayoutVariables: true,
		getCategoryVariables: true,
		setVarContext: true,
		deleteCtxVarImg: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "NewsletterMgr",
	{
		getNewsletters: true,
		exportData: true,
		addNewsletter: true,
		updateNewsletter: true,
		deleteNewsletters: true,
		getScheduleNewsletters: true,
		addScheduleNewsletter: true,
		updateScheduleNewsletter: true,
		deleteScheduleNewsletters: true,
		initSchedules: true,
		schedule: true,
		getScheduleSMTPServers: true,
		addScheduleSMTPServers: true,
		startAlertNewslettersTask: true,
		requestSendAlertNewsletters: true,
		sendAlertNewsletters: true,
		sendNewsletter: true,
		getScheduleUsers: true,
		addScheduleUser: true,
		deleteScheduleUsers: true,
		getScheduleProducts: true,
		addScheduleProducts: true,
		getPageTemplates: true,
		getLiveServers: true,
		requestSchedule: true,
		requestLiveServers: true,
		getMyNewsletters: true,
		getMyLightNewsletters: true,
		manageNewsletter: true,
		manageLightNewsletter: true,
		getNewslettersXML: true,
		importData: true,
		getNewsletterSchedulesList: true,
		getNewsletterConfig: true,
		getNewsletterConfigByName: true,
		setNewsletterConfig: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "SMTPServerMgr",
	{
		getServers: true,
		exportData: true,
		addServer: true,
		updateServer: true,
		deleteServers: true,
		importData: true,
		publishToLive: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "CommentsConfig",
	{
		getConfig: true,
		setConfig: true,
		getDisqusScript: true,
		getDisqusCommentsHTML: true,
		exportData: true,
		importData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "IterForm",
	{
		getForms: true,
		getForm: true,
		addForm: true,
		editForm: true,
		deleteForms: true,
		getFormProducts: true,
		getFormsList: true,
		getFormDefinition: true,
		publishToLive: true,
		importForms: true,
		updateFormFieldsIds: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "IterTabs",
	{
		getTabs: true,
		getTabFields: true,
		addTab: true,
		editTab: true,
		deleteTabs: true,
		updateTabOrder: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "IterField",
	{
		getProfileFields: true,
		getField: true,
		addField: true,
		updateField: true,
		deleteFields: true,
		updateFieldOrder: true,
		checkRegisterForm: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "FormTransform",
	{
		getTransforms: true,
		getUrlXsl: true,
		addTransform: true,
		editTransform: true,
		deleteTransform: true,
		cancelOperation: true,
		publishToLive: true,
		importTransforms: true,
		getTransformsToSection: true,
		addTransformToSectionToList: true,
		deleteTransformToSectionToList: true,
		addTransformToSection: true,
		editTransformToSection: true,
		getRssAdvancedProperties: true,
		addRssAdvanced: true,
		updateRssAdvanced: true,
		deleteRssAdvanced: true,
		getImageFrame: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "IterPortletPreferences",
	{
		getPortletPreferences: true,
		setPortletPreferences: true,
		linkSetup: true,
		getSetups: true,
		deleteSetup: true,
		updateSetup: true,
		restoreSetup: true,
		getLinkedPreferences: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "Captcha",
	{
		getCaptcha: true,
		setCaptcha: true,
		importData: true,
		publish: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "Frames",
	{
		addFrame: true,
		updateFrame: true,
		deleteFrame: true,
		setReplacementContentType: true,
		publishToLive: true,
		importContents: true,
		getFramesByGroup: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "ThemeWebResources",
	{
		preDeliverTheme: true,
		deliverTheme: true,
		getWebResourceByPlidAndPlace: true,
		unlockProcess: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "GroupConfig",
	{
		getRobots: true,
		setRobots: true,
		getGoogleTools: true,
		setGoogleTools: true,
		exportRobots: true,
		importRobots: true,
		exportMetrics: true,
		importMetrics: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "ApacheMgr",
	{
		stopApacheQueueOperations: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "Rss",
	{
		getSectionBasedRss: true,
		setSectionBasedRss: true,
		getSectionBasedRssGroupConfig: true,
		setSectionBasedRssGroupConfig: true,
		getAdvancedRssList: true,
		setAdvancedRss: true,
		deleteAdvancedRss: true,
		getInheritableRss: true,
		exportRSSAdvanced: true,
		exportRSSSections: true,
		importRSSSections: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "BlockerAdBlock",
	{
		getDataBlockerAdBlock: true,
		getConfBlockerAdBlock: true,
		setConfBlockerAdBlock: true,
		exportConf: true,
		importConf: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "VisitsStatistics",
	{
		getVisitsStatisticsConfig: true,
		setVisitsStatisticsConfig: true,
		notifyVocabulariesModificationsToMAS: true,
		exportData: true,
		importData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "Feedback",
	{
		getFeedbackConf: true,
		getFeedbackDisplayConf: true,
		addOrUpdtFeedbackDisplay: true,
		setQuestion: true,
		addChoice: true,
		updateChoice: true,
		updateChoiceOrder: true,
		deleteChoices: true,
		publishFeedbackConf: true,
		importFeedbackConf: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "URLShortener",
	{
		setShorteners: true,
		getShorteners: true,
		exportData: true,
		importData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "AuthorizationKey",
	{
		getAuthorizationKeys: true,
		addAuthorizationKey: true,
		updateAuthorizationKey: true,
		deleteAuthorizationKey: true,
		enableAuthorizationKey: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Base, "Cluster",
	{
		importContents: true
	}
);