Liferay.Service.register("Liferay.Service.Xmlio", "com.protecmedia.iter.xmlio.service");

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "Channel",
	{
		importToLive: true,
		importWebThemesToLive: true,
		importJournalTemplatesToLive: true,
		importLayoutTemplatesToLive: true,
		refreshCache: true,
		importDefaultSectionProperties: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "Live",
	{
		publishGroupToLive: true,
		publishContentToLive: true,
		publishLayoutToLive: true,
		changeLiveStatus: true,
		populateLive: true,
		getLiveItemIdsFromLocalIds: true,
		getPublicationListFlex: true,
		getPublicationDetailsFlex: true,
		getKeyFieldsFlex: true,
		publishToLiveSelectiveFlex: true,
		publishToLiveMassiveFlex: true,
		publishToLive: true,
		publishCatalogsIter: true,
		idGlobalToIdLocal: true,
		localBackToLocalLive: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "LiveConfiguration",
	{
		getLiveConfiguration: true,
		setLiveConfiguration: true,
		getRemoteSystemInfo: true,
		getArchiveByCompanyId: true,
		setArchiveByCompanyId: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "LiveControl",
	{
		unlockLiveControl: true,
		getAllRecordsFlex: true,
		interruptPublication: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "ContextVarsPublish",
	{
		publishToLive: true,
		publishCtxVarsToLive: true,
		importContents: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "JournalArticleImport",
	{
		reindexArticleContent: true,
		deleteArticle: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "ImportMgr",
	{
		getImportsList: true,
		deleteImports: true,
		deleteImportDetails: true,
		getDetailsUsersImportsList: true,
		importUsers: true,
		stopUserImport: true,
		getArticlesImportsList: true,
		getDetailsArticlesImportsList: true,
		importArticles: true,
		stopArticleImport: true,
		selectToDeleteArticles: true,
		deleteArticlesFromBatchsList: true,
		deleteArticlesFromDetailsList: true,
		getDeletedPercentage: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "DaylyTopicsPublication",
	{
		publish: true,
		publishInLive: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "CategoriesPropertiesPublication",
	{
		importContents: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "WebsiteIO",
	{
		computeXPortDependencies: true,
		exportObjects: true,
		abortExport: true,
		importPreProcessInfo: true,
		importObject: true,
		importPostProcessInfo: true,
		resetImport: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "IterAdminIO",
	{
		finishImport: true,
		unsetOngoingImportSubprocess: true,
		finishExport: true,
		exportObjects: true,
		exportAllObjects: true,
		getLastExportError: true,
		importObjects: true,
		abortImport: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "DataProblemsDiagnostic",
	{
		getStateCaptureProcess: true,
		captureData: true,
		startCaptureData: true,
		stopCaptureData: true,
		stopProcess: true,
		getLastDownloadError: true,
		cleanData: true,
		createZipLiveData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.Xmlio, "CommunityPublisher",
	{
		getSchedulePublications: true,
		getSchedulePublicationDetail: true,
		cancelSchedulePublication: true,
		checkProcessStatus: true
	}
);