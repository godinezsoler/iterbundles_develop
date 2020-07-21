/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.base.portlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.base.model.Communities;
import com.protecmedia.iter.base.model.Iter;
import com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.util.CommunitiesValidator;

public class BasePortlet extends MVCPortlet {

	protected String editVideoCod;
	protected String editAppKey;
	
	@Override
	public void init() throws PortletException { 
		super.init();
		
		editVideoCod = getInitParameter("edit-videocod-jsp");	
		editAppKey = getInitParameter("edit-appkey-jsp");
	}
	
	@Override
	public void doView(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		super.doView(renderRequest, renderResponse);
	}
	
	@Override
	public void processAction(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
				
		String tab = ParamUtil.get(actionRequest, "tabs1", "system");	
		
		if (tab.equals("communities")){
			processActionCommunities(actionRequest, actionResponse);
		}
		else {
			actionSystem(actionRequest, actionResponse);
		}
		
	}
	
	/* ******************************
	 *          ITER
	 * ******************************/
	
	/*
	 * Actions
	 */
	private void actionSystem(ActionRequest actionRequest,
			ActionResponse actionResponse) {
		
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");

		try {
			if (action.equals("updateSystem")) {
				updateSystem(actionRequest, actionResponse);
			} else {
				
			}
		} catch (Exception e) {}
	}
	
	private void updateSystem(ActionRequest actionRequest,
			ActionResponse actionResponse) throws PortalException, SystemException {
		
		long id = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		/* Version */
		String version = ParamUtil.getString(actionRequest, "version", "");

		/* Name */
		String name = ParamUtil.getString(actionRequest, "name", "");
		
		/* Public Key */
		String publicKey = ParamUtil.getString(actionRequest, "publicKey", "");
		
		if (id != -1) {
			Iter iter = IterLocalServiceUtil.getIter(id);
			//This fields can only be updated via Database
			//iter.setName(name);
			//iter.setPublicKey(publicKey);	
			//iter.setVersion(version);
			
			IterLocalServiceUtil.updateIter(iter);
		}
		
	}
	
	/* ****************************************************
	 *   
	 * Tab Communities
	 *   
	 * ****************************************************/
	
	/**
	 * @param actionRequest
	 * @param actionResponse
	 */
	private void processActionCommunities(ActionRequest actionRequest, ActionResponse actionResponse) {
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		try {
			if (action.equals("addCommunity")) {				
				addCommunity(actionRequest, actionResponse);
			}
		} catch (PortalException e) {
			;
		} catch (SystemException e) {
			;
		}
		actionResponse.setRenderParameter("tabs1", "communities");
	}
	
	/***
	 * 
	 * Actualizamos el Canal
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void addCommunity(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {

		long communityGroupId = ParamUtil.getLong(actionRequest, "communityGroupId", -1);
		
		if(communityGroupId != -1){
			
			String privateSearchUrl = ParamUtil.getString(actionRequest,"privateSearchUrl", "");
			String publicSearchUrl = ParamUtil.getString(actionRequest,"publicSearchUrl", "");	
			boolean fuzzySearch = ParamUtil.getBoolean(actionRequest,"fuzzySearch", false);	
			
			Communities com = CommunitiesLocalServiceUtil.getCommunitiesByGroup(communityGroupId);	
			
			if(com == null){
				long id = CounterLocalServiceUtil.increment();			
				com = CommunitiesLocalServiceUtil.createCommunities(id);	
				com.setGroupId(communityGroupId);
			}
			
			com.setPrivateSearchUrl(privateSearchUrl);
			com.setPublicSearchUrl(publicSearchUrl);	
			com.setFuzzySearch(fuzzySearch);
			
			ArrayList<String> errors = new ArrayList<String>();
			if (CommunitiesValidator.validateCommunities(com, errors)) {
				CommunitiesLocalServiceUtil.updateCommunities(com);
				SessionMessages.add(actionRequest, "base-communities-updated-success");	
			} else {
				for (String error : errors) {
					SessionErrors.add(actionRequest, error);
				}
					
				actionRequest.setAttribute("errorCommunities", com);
			}
		
		}
		
		actionResponse.setRenderParameter("tabs1", "communities");
		
	}
	
}
