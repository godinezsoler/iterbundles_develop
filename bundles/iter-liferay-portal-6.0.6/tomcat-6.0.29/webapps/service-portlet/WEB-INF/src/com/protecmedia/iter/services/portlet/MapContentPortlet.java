/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.portlet;

import java.io.IOException;


import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.util.ParamUtil;

import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * Portlet implementation class ContentMapPortlet
 */
public class MapContentPortlet extends MVCPortlet {
	public void processAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws IOException, PortletException {
		
		
	}
	
	public void render(RenderRequest request, RenderResponse response)
	throws PortletException, IOException {
		
		String articleId = ParamUtil.get(request, "article-id", "");
		
		
		request.setAttribute("article-id", articleId);
		
		
		include(viewJSP, request, response);
		
		}


}
