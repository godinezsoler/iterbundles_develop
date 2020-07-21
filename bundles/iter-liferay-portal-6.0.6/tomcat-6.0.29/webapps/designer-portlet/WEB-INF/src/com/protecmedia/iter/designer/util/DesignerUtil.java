/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.designer.util;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.protecmedia.iter.base.service.util.IterKeys;

public class DesignerUtil {
	
	public static long getPageTemplateParentId(long groupId, long userId) {
		
		try {
			Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, IterKeys.PARENT_LAYOUT_URL);
			
			return layout.getLayoutId();
		} catch (NoSuchLayoutException e) {		
			try {
				ServiceContext serviceContext = new ServiceContext();
				
				Layout layout = LayoutLocalServiceUtil.addLayout(userId, groupId, false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, IterKeys.PARENT_LAYOUT_NAME, StringPool.BLANK,
						  StringPool.BLANK, IterKeys.CUSTOM_TYPE_TEMPLATE, true, IterKeys.PARENT_LAYOUT_URL, serviceContext);
				return layout.getLayoutId();
			} catch (Exception e1) {}			
		} catch (Exception e) {}
				
		return LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;
	}
	
	public static long getNewsLetterParentId(long groupId, long userId) {
		
		try {
			Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, IterKeys.PARENT_LAYOUT_URL_NEWSLETTER);
			
			return layout.getLayoutId();
		} catch (NoSuchLayoutException e) {		
			try {
				ServiceContext serviceContext = new ServiceContext();
				
				Layout layout = LayoutLocalServiceUtil.addLayout(userId, groupId, false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, IterKeys.PARENT_LAYOUT_NAME_NEWSLETTER, StringPool.BLANK,
						  StringPool.BLANK, IterKeys.CUSTOM_TYPE_TEMPLATE, true, IterKeys.PARENT_LAYOUT_URL_NEWSLETTER, serviceContext);
				return layout.getLayoutId();
			} catch (Exception e1) {}			
		} catch (Exception e) {}
				
		return LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;
	}
	
	public static long getArticleTemplateParentId(long groupId, long parentId) {
		
		try{
			Layout layout = LayoutLocalServiceUtil.getLayout(groupId, false, parentId);
			return layout.getPlid();
		}
		catch (Exception e){}
		
		return LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;
	}

}
