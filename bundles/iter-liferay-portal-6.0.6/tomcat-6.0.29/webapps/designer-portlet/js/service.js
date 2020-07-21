Liferay.Service.register("Liferay.Service.Designer", "com.protecmedia.iter.designer.service");

Liferay.Service.registerClass(
	Liferay.Service.Designer, "PageTemplate",
	{
		getPageTemplates: true,
		getPageTemplatesType: true,
		getPageTemplateByPageTemplateId: true,
		loadPageTemplate: true,
		addPageTemplate: true,
		addPageTemplate2: true,
		addPageTemplate3: true,
		addPageTemplateParentId: true,
		deletePageTemplate: true,
		clearLayout: true,
		updatePageTemplateName: true,
		updatePageTemplate: true,
		updatePageTemplate1: true,
		getURLPageTemplate: true,
		compareLayoutPageTemplate: true,
		setDefaultPageTemplate: true,
		setDefaultPageTemplateMobile: true
	}
);