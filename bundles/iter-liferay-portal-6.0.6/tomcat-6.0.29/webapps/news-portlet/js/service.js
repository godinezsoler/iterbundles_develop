Liferay.Service.register("Liferay.Service.News", "com.protecmedia.iter.news.service");

Liferay.Service.registerClass(
	Liferay.Service.News, "Qualification",
	{
		addQualification: true,
		removeQualification: true,
		updateQualification: true,
		getQualification: true,
		getQualifications: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "PageContent",
	{
		insertPageContentArticleModel: true,
		addPageContentArticleModel: true,
		updatePageContentQualification: true,
		updatePageContentArticleModel: true,
		updatePageContentDate: true,
		updatePageContentQualificationDate: true,
		deletePageContent: true,
		activatePageContent: true,
		deactivatePageContent: true,
		changePageContentPosition: true,
		setDefaultPageContent: true,
		reorderPageContents: true,
		changeLayoutOrder: true,
		changeLayoutPosition: true,
		getViewerUrl: true,
		movePageContents: true,
		getArticleContextInfo: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "ArticlePoll",
	{
		getPollResults: true,
		getPollResultsAsJson: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "Comments",
	{
		getComments: true,
		enableComment: true,
		disableComment: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "Categorize",
	{
		addVocabulary: true,
		deleteVocabulary: true,
		updateVocabulary: true,
		addCategory: true,
		updateCategory: true,
		deleteCategory: true,
		addExpandoTable: true,
		getExpandoTable: true,
		deleteExpandoTable: true,
		addBooleanValue: true,
		addStringValue: true,
		addDoubleValue: true,
		addDateValue: true,
		updateWebContentCategories: true,
		updateWebContentCategories2: true,
		asignArticleAsAboutCategoryArticle: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "JournalArticle",
	{
		addContent2: true,
		addContent3: true,
		addContent4: true,
		addContent5: true,
		addContent6: true,
		addContent7: true,
		updateContent: true,
		updateContent2: true,
		updateContent3: true,
		updateContent4: true,
		deleteContent: true,
		deleteContent2: true,
		deleteJournalArticle: true,
		deleteJournalArticleAndRefresh: true,
		deleteContentFromPublicURL: true,
		deleteContentFromPublicURL2: true,
		deleteContentFromURL: true,
		reIndexJournalArticles: true,
		deleteIndexedArticles: true,
		getIndexingProgress: true,
		stopIndexation: true,
		getEditArticle: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "DLFileEntry",
	{
		deleteFileEntry: true,
		deleteDLFileEntry: true,
		deleteFileEntry2: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "Product",
	{
		addProduct: true,
		deleteProduct: true,
		updateProduct: true,
		setProductsOfJournalArticle: true,
		updateProductsOfJournalArticle: true,
		setProductsOfFileEntry: true,
		updateProductsOfFileEntry: true,
		getPaywallProducts: true,
		addPaywallProduct: true,
		updatePaywallProduct: true,
		deletePaywallProduct: true,
		getSelectedProducts: true,
		getSessionsByUser: true,
		getPaywallProductsType1AccessByUser: true,
		getPaywallProductsType2AccessByUser: true,
		initPaywallDeleteSessionsTask: true,
		setPaywallStatusMsgs: true,
		getPaywallStatusMsgs: true,
		updatePaywall: true,
		getPaywallMode: true,
		exportData: true,
		importData: true,
		asignArticle: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "Layout",
	{
		setHidden: true,
		addLayout: true,
		updateLayout: true,
		setGroupDefaultProperties: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "Catalog",
	{
		getCatalogs: true,
		addCatalog: true,
		updateCatalog: true,
		deleteCatalogs: true,
		getCatalogPages: true,
		getCatalogPage: true,
		getHeaderPages: true,
		getMenuPages: true,
		getFooterPages: true,
		getBodyPages: true,
		addCatalogPage: true,
		addCatalogPageAndElements: true,
		getCatalogPageURL: true,
		updateCatalogPage: true,
		deleteCatalogPages: true,
		addCatalogElements: true,
		getDataCatalogElement: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "MetadataControl",
	{
		getConfig: true,
		setConfig: true,
		importData: true,
		publish: true,
		importPublishedData: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "DaylyTopicMgr",
	{
		setDaylyTopics: true
	}
);

Liferay.Service.registerClass(
	Liferay.Service.News, "ExternalServices",
	{
		getExternalServices: true,
		setExternalServices: true,
		deleteExternalService: true,
		disableExternalService: true,
		contentRequest: true,
		searchDependencies: true,
		publishToLive: true,
		importData: true
	}
);