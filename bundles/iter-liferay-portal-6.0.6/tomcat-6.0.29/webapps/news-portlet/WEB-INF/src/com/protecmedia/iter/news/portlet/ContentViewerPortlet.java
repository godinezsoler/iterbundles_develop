/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.portlet;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

public class ContentViewerPortlet extends MVCPortlet {

	@Override
	public void doView(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
	
		boolean standardArticleCheck = ParamUtil.getBoolean(renderRequest, "standardArticleCheck", true);
		boolean standardGalleryCheck = ParamUtil.getBoolean(renderRequest, "standardGalleryCheck", true);
		boolean standardPollCheck = ParamUtil.getBoolean(renderRequest, "standardPollCheck", true);		
		boolean standardMultimediaCheck = ParamUtil.getBoolean(renderRequest, "standardMultimediaCheck", true);
		
		boolean defaultValue = ParamUtil.getBoolean(renderRequest, "defaultValue", true);		
		
		renderRequest.setAttribute("standardArticleCheck", standardArticleCheck);
		renderRequest.setAttribute("standardGalleryCheck", standardGalleryCheck);
		renderRequest.setAttribute("standardPollCheck", standardPollCheck);
		renderRequest.setAttribute("standardMultimediaCheck", standardMultimediaCheck);
		
		renderRequest.setAttribute("defaultValue", defaultValue);
		
		super.doView(renderRequest, renderResponse);
	}
	
}
