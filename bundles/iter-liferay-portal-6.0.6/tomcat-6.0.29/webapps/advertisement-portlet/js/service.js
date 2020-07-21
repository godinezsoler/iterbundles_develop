Liferay.Service.register("Liferay.Service.Advertisement", "com.protecmedia.iter.advertisement.service");

Liferay.Service.registerClass(
	Liferay.Service.Advertisement, "AdvertisementMgr",
	{
		getSlots: true,
		addSlot: true,
		updateSlot: true,
		deleteSlots: true,
		getTags: true,
		addTag: true,
		updateTag: true,
		deleteTags: true,
		getSkins: true,
		addSkin: true,
		updateSkin: true,
		deleteSkins: true,
		getSlotTagLayout: true,
		addSlotTagLayout: true,
		updateSlotTagLayout: true,
		updatePrioritySlotTagLayout: true,
		deleteSlotTagLayout: true,
		createDefaultTagConfig: true,
		createDefaultSkinConfig: true,
		publish: true,
		publishToLive: true,
		importContents: true,
		getSlotTagCategory: true,
		addSlotTagCategory: true,
		updateSlotTagCategory: true,
		updatePrioritySlotTagCategory: true,
		deleteSlotTagCategory: true,
		getAdVocBranches: true,
		getVocabularies: true,
		addAdvertisementVocabulary: true,
		deleteAdvertisementVocabulary: true
	}
);