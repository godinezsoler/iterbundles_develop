/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.util;

import com.protecmedia.iter.xmlio.service.util.log.ItemLog;

public class XMLIOContext {
	
	private long globalGroupId;	
	private long groupId;	
	private long userId;	
	private long companyId;
	
	public ItemLog itemLog = new ItemLog();	
	
	// Variables para controlar que partes de una publicación se realizarán
	private boolean publishPageContent 			  = true;
	private boolean publishArticles    			  = true;
	private boolean publishCatalogs    			  = true;
	private Long    assetCategoryId               = null;
	private boolean onlyProductsPublication       = false;	
	private boolean onlyQualificationsPublication = false;
	private boolean publishArticleTemplate		  = false;
		
	public boolean isOnlyQualificationsPublication() {	
		return onlyQualificationsPublication;
	}
	
	public void setOnlyQualificationsPublication(boolean onlyQualificationsPublication) {	
		this.onlyQualificationsPublication = onlyQualificationsPublication;
	}

	public boolean isOnlyProductsPublication() {	
		return onlyProductsPublication;
	}
	
	public void setOnlyProductsPublication(boolean onlyProductsPublication) {	
		this.onlyProductsPublication = onlyProductsPublication;
	}

	public Long getAssetCategoryId() {	
		return assetCategoryId;
	}
	
	public void setAssetCategoryId(Long assetCategoryId) {	
		this.assetCategoryId = assetCategoryId;
	}

	public boolean getPublishPageContent() {	
		return publishPageContent;	
	}
	
	public void setPublishPageContent(boolean publishPageContent) {	
		this.publishPageContent = publishPageContent;
	}
	
	public boolean getPublishArticles() {	
		return publishArticles;
	}
	
	public void setPublishArticles(boolean publishArticles) {	
		this.publishArticles = publishArticles;
	}
	
	public boolean getPublishCatalogs() {	
		return publishCatalogs;
	}
	
	public void setPublishCatalogs(boolean publishCatalogs) {	
		this.publishCatalogs = publishCatalogs;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getCompanyId() {
		return companyId;
	}

	public void setCompanyId(long companyId) {
		this.companyId = companyId;
	}

	public void setGlobalGroupId(long globalGroupId) {
		this.globalGroupId = globalGroupId;
	}

	public long getGlobalGroupId() {
		return globalGroupId;
	}

	public boolean isPublishArticleTemplate()
	{
		return publishArticleTemplate;
	}

	public void setPublishArticleTemplate(boolean publishArticleTemplate)
	{
		this.publishArticleTemplate = publishArticleTemplate;
	}

}