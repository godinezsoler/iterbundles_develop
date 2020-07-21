/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.portlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.services.DuplicateServiceException;
import com.protecmedia.iter.services.model.Service;
import com.protecmedia.iter.services.model.impl.ServiceImpl;
import com.protecmedia.iter.services.service.ServiceLocalServiceUtil;
import com.protecmedia.iter.services.util.ServicesValidator;

public class ServicePortlet extends MVCPortlet {
	
	@Override
	public void init() throws PortletException {
		super.init();
		editServiceJSP = getInitParameter("edit-service-jsp");
	}

	@Override
	public void doView(RenderRequest renderRequest,	RenderResponse renderResponse) throws IOException, PortletException {
		String view = (String) renderRequest.getParameter("view");
		if (view == null || view.equals("")) {
			try {
				showViewDefault(renderRequest, renderResponse);
			} catch (SystemException e) {
				SessionErrors.add(renderRequest, "error-retrieving-service");
			}
		} else if (view.equalsIgnoreCase("editService")) {
			try {
				showViewEditService(renderRequest, renderResponse);
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					;
				}
			}
		}						
	}
	
	public void showViewDefault(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException,	PortletException, SystemException {

		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();

		PermissionChecker permissionChecker = themeDisplay.getPermissionChecker();
		boolean hasAddPermission = permissionChecker.hasPermission(groupId,	"com.protecmedia.iter.service.model", groupId, "ADD_SERVICE");
		boolean hasConfigurePermission = permissionChecker.hasPermission(groupId, Group.class.getName(), groupId, ActionKeys.PERMISSIONS);		
		
		renderRequest.setAttribute("hasAddPermission", hasAddPermission);

		PortletURL addServiceURL = renderResponse.createActionURL();
		addServiceURL.setParameter("javax.portlet.action", "newService");
		renderRequest.setAttribute("addServiceURL", addServiceURL.toString());

		include(viewJSP, renderRequest, renderResponse);
	}
	
	/**
	 * 
	 * @param renderRequest
	 * @param renderResponse
	 * @throws Exception
	 */
	public void showViewEditService(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
		
		PortletURL editURL = renderResponse.createActionURL();
		String editType = (String) renderRequest.getParameter("editType");
		if (editType.equalsIgnoreCase("edit")) {
			editURL.setParameter("javax.portlet.action", "updateService");
			long serviceId = ParamUtil.getLong(renderRequest, "serviceID", 0);
			Service service = ServiceLocalServiceUtil.getServiceByPrimaryKey(serviceId);
			renderRequest.setAttribute("service", service);
		} else {
			editURL.setParameter("javax.portlet.action", "addService");
			Service errorService = (Service) renderRequest.getAttribute("errorService");
			if (errorService != null) {
				renderRequest.setAttribute("service", errorService);
			} else {
				renderRequest.setAttribute("service", new ServiceImpl());
			}

		}
		renderRequest.setAttribute("editURL", editURL.toString());
		include(editServiceJSP, renderRequest, renderResponse);
	}
	
	
	@Override
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws IOException, PortletException {	
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		try {	
			if (action.equals("newService")) {				
				newService(actionRequest, actionResponse);
			} else if (action.equals("addService")) {								
				addService(actionRequest, actionResponse);						
			} else if (action.equals("editService")) {
				editService(actionRequest, actionResponse);
			} else if (action.equals("updateService")) {
				updateService(actionRequest, actionResponse);
			} else if (action.equals("deleteService")) {
				deleteService(actionRequest, actionResponse);
			}	
		} catch (Exception e) {
		}
	}
	
	public void newService(ActionRequest request, ActionResponse response) {
		response.setRenderParameter("view", "editService");
		response.setRenderParameter("editType", "add");
	}

	public void addService(ActionRequest request, ActionResponse response) throws Exception {		
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		
		long groupId = themeDisplay.getScopeGroupId();
		
		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(request);

		File imageFile = uploadRequest.getFile("imageFile");
		
		String title = ParamUtil.getString(uploadRequest, "title", "");
		
		long idLink = ParamUtil.getLong(uploadRequest, "serviceUrl", -1);
		
		Service service = null;
		
		try{
			service = ServiceLocalServiceUtil.addService(groupId, null, title, idLink, imageFile);
			response.setRenderParameter("view", "");
			SessionMessages.add(request, "service-added");
		}catch (Exception e) {
			if(IOException.class.isInstance(e)){
				service.setImageId(-1);
			}
			ArrayList<String> errors = new ArrayList<String>();
			ServicesValidator.validateServices(service, errors);
			for (String error : errors) {
				SessionErrors.add(request, error);
			}
			response.setRenderParameter("view", "editService");
			response.setRenderParameter("editType", "add");

			request.setAttribute("errorService", service);
		}

	}

	public void editService(ActionRequest request, ActionResponse response) throws Exception {
		long serviceId = ParamUtil.getLong(request, "resourcePrimKey");
		response.setRenderParameter("serviceID", Long.toString(serviceId));
		response.setRenderParameter("view", "editService");
		response.setRenderParameter("editType", "edit");
	}
	
	public void updateService(ActionRequest request, ActionResponse response)
			throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		
		long groupId = themeDisplay.getScopeGroupId();
		
		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(request);
		File imageFile = uploadRequest.getFile("imageFile");
		
		String title = ParamUtil.getString(uploadRequest, "title", "");
		
		long idLink = ParamUtil.getLong(uploadRequest, "serviceUrl", -1);
		
		long imageId = ParamUtil.getLong(uploadRequest, "imageId");
				
		long id = ParamUtil.getLong(uploadRequest, "serviceId");
		
		Service s = ServiceLocalServiceUtil.getServiceByPrimaryKey(id);
		Service service = null;
		
		try{
			service = ServiceLocalServiceUtil.updateService(s, groupId, title, idLink, imageId, imageFile);
			response.setRenderParameter("view", "");
			SessionMessages.add(request, "service-updated");
		}catch (Exception e) {
			ArrayList<String> errors = new ArrayList<String>();
			ServicesValidator.validateServices(service, errors);
			for (String error : errors) {
				SessionErrors.add(request, error);
			}
			response.setRenderParameter("serviceID", Long.toString(service.getPrimaryKey()));
			response.setRenderParameter("view", "editService");
			response.setRenderParameter("editType", "edit");
			request.setAttribute("errorService", service);
		}
	}	
	
	public void deleteService(ActionRequest request, ActionResponse response)
			throws Exception {

		long serviceId = ParamUtil.getLong(request, "resourcePrimKey");
		if (Validator.isNotNull(serviceId)) {
			
			Service service = ServiceLocalServiceUtil.getServiceByPrimaryKey(serviceId);			
			ServiceLocalServiceUtil.deleteService(service);
			
			if (Validator.isNotNull(service.getImageId())) {
				ImageLocalServiceUtil.deleteImage(service.getImageId());
			}
			
			SessionMessages.add(request, "service-deleted");
		} else {
			SessionErrors.add(request, "error-deleting");
		}
	}
	
	
	protected String editServiceJSP;
}
