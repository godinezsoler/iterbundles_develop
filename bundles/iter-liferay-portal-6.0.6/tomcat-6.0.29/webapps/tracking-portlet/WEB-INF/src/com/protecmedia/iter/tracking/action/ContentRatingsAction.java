/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.portlet.BaseConfigurationAction;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;

public class ContentRatingsAction extends BaseConfigurationAction {
	public void processAction(PortletConfig portletConfig, ActionRequest actionRequest,	ActionResponse actionResponse) throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		if (!cmd.equals(Constants.UPDATE)) {
			return;
		}

		String portletResource = ParamUtil.getString(actionRequest, "portletResource");
		PortletPreferences preferences = PortletPreferencesFactoryUtil.getPortletSetup(actionRequest, portletResource);
				
		String ratingReadAccessLevelCategoryId =ParamUtil.get(actionRequest, "ratingReadAccessLevelCategoryId", "-1");
		String ratingAddAccessLevelCategoryId = ParamUtil.get(actionRequest, "ratingAddAccessLevelCategoryId", "-1");
		
		preferences.setValue("ratingReadAccessLevelCategoryId", ratingReadAccessLevelCategoryId);
		preferences.setValue("ratingAddAccessLevelCategoryId", ratingAddAccessLevelCategoryId);
		preferences.store();		

		SessionMessages.add(actionRequest, portletConfig.getPortletName() + ".doConfigure");
	}
	
	public String render(PortletConfig portletConfig, RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {

		return "/html/content-ratings-portlet/configuration.jsp";
	}
}
