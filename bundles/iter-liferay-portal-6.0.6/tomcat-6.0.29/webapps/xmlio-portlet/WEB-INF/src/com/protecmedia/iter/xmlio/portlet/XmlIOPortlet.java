/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.portlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.NoSuchChannelControlException;
import com.protecmedia.iter.xmlio.model.Channel;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.model.impl.ChannelImpl;
import com.protecmedia.iter.xmlio.service.ChannelControlLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.ChannelControlLogLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.util.ChannelValidator;
import com.protecmedia.iter.xmlio.util.LiveConfigurationValidator;


public class XmlIOPortlet extends MVCPortlet {
	
	private static Log _log = LogFactoryUtil.getLog(XmlIOPortlet.class);
	protected String editChannel;
	protected String liveConfiguration;
	protected String liveControl;
	protected String liveItem;
	protected String channelControl;
	protected String channelControlLog;

	public void init() {
		try {			
			super.init();
			editChannel = getInitParameter("edit-channel-jsp");		
			liveConfiguration = getInitParameter("live-configuration-jsp");
			liveControl = getInitParameter("live-control-jsp");
			liveItem = getInitParameter("live-item-jsp");
			channelControl = getInitParameter("channel-control-jsp");
			channelControlLog = getInitParameter("channel-control-log-jsp");
		} catch (PortletException e) {
			;
		}		
	}	
	
	public void doView(RenderRequest renderRequest,	RenderResponse renderResponse) throws IOException, PortletException {		

		String tab = ParamUtil.get(renderRequest, "tabs1", "manual");
		
		if (tab.equals("manual")) {
			showTabManual(renderRequest, renderResponse);
		} 
		else if (tab.equals("automatic")){ 
			showTabAutomatic(renderRequest, renderResponse);
		}
		else if (tab.equals("live")){
			showTabLive(renderRequest, renderResponse);
		}
	}
	
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		String tab = ParamUtil.get(actionRequest, "tabs1", "manual");
		
		if (tab.equals("manual")) {
			processActionTabManual(actionRequest, actionResponse);
		} 
		else if (tab.equals("automatic")){
			processActionTabAutomatic(actionRequest, actionResponse);
		}
		else if (tab.equals("live")){
			processActionTabLive(actionRequest, actionResponse);
		}
	}
	
	
	/* **************************************************************************************************
	 * 
	 * Tab Manual
	 * 
	 * **************************************************************************************************/

	/**
	 * 
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 */
	private void showTabManual(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
		
		String view = (String) renderRequest.getParameter("view");
		
		if (view == null || view.equals("")) {
			try {
				showManualViewDefault(renderRequest, renderResponse);
			} catch (SystemException e) {
				SessionErrors.add(renderRequest, "error-retrieving-channel");
			}
		} else if (view.equalsIgnoreCase("channelControl")) {
			try {
				showChannelControl(renderRequest, renderResponse);			
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					SessionErrors.add(renderRequest, "error-retrieving-channel");
				}
			}
		} else if (view.equalsIgnoreCase("channelControlLog")) {
			try {
				showChannelControlLog(renderRequest, renderResponse);			
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					SessionErrors.add(renderRequest, "error-retrieving-channel");
				}
			}
		}

		renderRequest.setAttribute("tabs1", "manual");
	}
	
	public void showManualViewDefault(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException, SystemException {		
		PortletURL editURL = renderResponse.createActionURL();
		editURL.setParameter("javax.portlet.action", "processManualOperation");
		editURL.setParameter("editType", "add");	
		editURL.setParameter("tabs1", "manual");
		
		PortletURL channelControlURL = renderResponse.createRenderURL();
		channelControlURL.setParameter("view", "channelControl");	
		channelControlURL.setParameter("tabs1", "manual");
		channelControlURL.setParameter("tabName", "xmlio-channel-control-manual");
		renderRequest.setAttribute("channelControlURL", channelControlURL.toString());
		
		PortletURL channelControlLogURL = renderResponse.createRenderURL();
		channelControlLogURL.setParameter("view", "channelControlLog");	
		channelControlLogURL.setParameter("tabs1", "manual");
		channelControlLogURL.setParameter("tabName", "xmlio-channel-control-manual");
		renderRequest.setAttribute("channelControlLogURL", channelControlLogURL.toString());
		
		renderRequest.setAttribute("editURL", editURL.toString());
		renderRequest.setAttribute("tabs1", "manual");
		
		include(viewJSP, renderRequest, renderResponse);
	}
	
	public void showChannelControl(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL channelControlURL = renderResponse.createActionURL();
		String currentTab = (String) renderRequest.getParameter("tabs1");

		channelControlURL.setParameter("javax.portlet.action", "channelControl");
		renderRequest.setAttribute("channelControlURL", channelControlURL.toString());		
		
		include(channelControl, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", currentTab);
	}
	
	public void showChannelControlLog(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL channelControlLogURL = renderResponse.createActionURL();
		String currentTab = (String) renderRequest.getParameter("tabs1");
		String channelControlId = (String) renderRequest.getParameter("channelControlId");

//		channelControlLogURL.setParameter("javax.portlet.action", "channelControlLog");
		renderRequest.setAttribute("channelControlLogURL", channelControlLogURL.toString());		
		
		include(channelControlLog, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", currentTab);
		renderRequest.setAttribute("channelControlId", channelControlId);
	}
	
	/**
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 */	
	private void processActionTabManual(ActionRequest actionRequest, ActionResponse actionResponse) {
		
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		
		try {
			if (action.equals("processManualOperation")) {
				processManualOperation(actionRequest, actionResponse);
			} else if (action.equals("viewChannelControlLog")){
				viewChannelControlLog(actionRequest, actionResponse);
			} else if (action.equals("deleteChannelControlItem")) {
				deleteChannelControlItem(actionRequest, actionResponse);
			} else if (action.equals("clearChannelControl")) {
				clearChannelControlHistory(actionRequest, actionResponse);
			}
		} catch (Exception e) {}
	}

	private void processManualOperation(ActionRequest actionRequest, ActionResponse actionResponse) 
	{
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		String type = ParamUtil.getString(actionRequest, "type", "");
		String sourcePath = ParamUtil.getString(actionRequest, "source", "");
		String destinationPath = ParamUtil.getString(actionRequest, "destination", "");
		String xslPath = ParamUtil.getString(actionRequest, "xsl", "");
		
		long ccId 		 = -1;
		String resultLog = "";
		String errInf	 = "";
		
		if (type.equals(IterKeys.XMLIO_CHANNEL_TYPE_INPUT) && !sourcePath.equals("")) 
		{
			try
			{
				ccId = ChannelControlLocalServiceUtil.startOperation(themeDisplay.getScopeGroupId(), themeDisplay.getUserId(), IterKeys.XMLIO_XML_MANUAL, IterKeys.XMLIO_XML_IMPORT_OPERATION);
				if(ccId!=0){
					// Se quieren mantener los ficheros de entrada de la importación manual
					resultLog = ChannelLocalServiceUtil.importContent(sourcePath, xslPath, themeDisplay.getCompanyId(), 
																	  themeDisplay.getUserId(), GroupMgr.getGlobalGroupId(), 
																	  true, false);
					processLog(themeDisplay.getCompanyId(), ccId, resultLog);
				}
				else
				{
					SessionErrors.add(actionRequest, "xmlio-manual-operation-in-process");
				}
			}
			catch (Exception cie) 
			{
				_log.error(cie);
				errInf = cie.toString();
				try
				{
					ChannelControlLocalServiceUtil.finishOperation(ccId, -1, -1, errInf);
				}
				catch(Exception e)
				{
					_log.error(e);
				}
			}
			finally
			{
				if (errInf.isEmpty())
					SessionMessages.add(actionRequest, "xmlio-manual-import-success");
				else
					SessionErrors.add(actionRequest, "xmlio-manual-import-error");
			}
		} 
		else if (type.equals(IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT) && !destinationPath.equals(""))
		{
			try
			{
				String channelRangeType = ParamUtil.getString(actionRequest,"channelRangeType", IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL);
				if (channelRangeType.equals(IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL)) 
					channelRangeType = null;
			
				//TODO: Date Range
				boolean channelRangeTimeAll = ParamUtil.getBoolean(actionRequest, "channelRangeTimeAll",   false);
				int channelRangeTimeValue 	= ParamUtil.getInteger(actionRequest, "channelRangeTimeValue", -1);
				String channelRangeTimeUnit = ParamUtil.getString(actionRequest,  "channelRangeTimeUnit",  IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR);

				ccId 	= ChannelControlLocalServiceUtil.startOperation(themeDisplay.getScopeGroupId(), themeDisplay.getUserId(), IterKeys.XMLIO_XML_MANUAL, IterKeys.XMLIO_XML_EXPORT_OPERATION);
				if(ccId!=0){
					File f 	= ChannelLocalServiceUtil.exportContent(destinationPath, xslPath, themeDisplay.getCompanyId(),  themeDisplay.getUserId(), themeDisplay.getScopeGroupId(), true, channelRangeType);
					//Al no tener informacion de los items exportados pasamos '-1' y asi en al GUI veremos un '-'
					ChannelControlLocalServiceUtil.finishOperation(ccId, -1, -1 , "");
					//ChannelControlLocalServiceUtil.updateFileSize(ccId, f.length());
				}
				else{
					SessionErrors.add(actionRequest, "xmlio-manual-operation-in-process");
				}
			}
			catch (Exception cee) 
			{
				_log.error(cee);
				errInf = cee.toString();
				
				try
				{
					ChannelControlLocalServiceUtil.finishOperation(ccId, -1, -1, errInf);
				}
				catch(Exception e)
				{
					_log.error(e);
				}
			}
			finally
			{
				if (errInf.isEmpty())
					SessionMessages.add(actionRequest, "xmlio-manual-export-success" );
				else
					SessionErrors.add(actionRequest, "xmlio-manual-export-error");
			}
		} 
		else
		{
			SessionErrors.add(actionRequest, "xmlio-manual-no-file-error");
		}
		
		actionResponse.setRenderParameter("tabs1", "manual");
	}
	
	private void processLog(long companyId, long channelControlId, String importLog) throws DocumentException, SystemException, NoSuchChannelControlException
	{
		if(!importLog.equals(""))
		{
			Document doc;
			List<Node> errorNodes = null;
			List<Node> nodes = null;
			
			doc = SAXReaderUtil.read(importLog);
			
			String xPath = "/iter/logs/item[@status='" + IterKeys.ERROR + "']";
			XPath xpathSelector = SAXReaderUtil.createXPath(xPath);
		    errorNodes = xpathSelector.selectNodes(doc);
		    
		    xPath = "/iter/logs/item";
		    xpathSelector = SAXReaderUtil.createXPath(xPath);
		    nodes = xpathSelector.selectNodes(doc);
			
			ChannelControlLocalServiceUtil.finishOperation(channelControlId, nodes!=null?nodes.size():-1, errorNodes!=null?errorNodes.size():-1 , "");
			if(errorNodes.size()>0)
			{
				ChannelControlLogLocalServiceUtil.addErrorLog( companyId, channelControlId, errorNodes );
			}
		}
	}
	
	private void viewChannelControlLog(ActionRequest actionRequest, ActionResponse actionResponse) {
		long channelControlId = ParamUtil.getLong(actionRequest, "resourcePrimKey");
		String currentTab =  ParamUtil.getString(actionRequest, "tabs1");

		actionResponse.setRenderParameter("channelControlId", Long.toString(channelControlId));
		actionResponse.setRenderParameter("view", "channelControlLog");
		actionResponse.setRenderParameter("tabs1", currentTab);
	}
	
	private void deleteChannelControlItem(ActionRequest actionRequest, ActionResponse actionResponse) {
		
		long channelControlId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		String tab = ParamUtil.getString(actionRequest, "tabs1");

		if (channelControlId != -1) {
			try {
				ChannelControlLocalServiceUtil.deleteChannelControl(channelControlId);
				SessionMessages.add(actionRequest, "xmlio-channel-control-deleted");
			} catch (Exception e) {
				SessionErrors.add(actionRequest, "xmlio-channel-control-id-delete-error");
			}
		} else {
			SessionErrors.add(actionRequest, "xmlio-channel-control-id-not-exist");
		}
		
		actionResponse.setRenderParameter("tabs1", tab);
		actionResponse.setRenderParameter("view", "channelControl");
		
	}
	
	private void clearChannelControlHistory(ActionRequest actionRequest, ActionResponse actionResponse) {
		
		String tab = ParamUtil.getString(actionRequest, "tabs1");
		String type = tab.equals(IterKeys.XMLIO_XML_MANUAL) ? IterKeys.XMLIO_XML_MANUAL : IterKeys.XMLIO_XML_AUTOMATIC; 

		try {
			ChannelControlLocalServiceUtil.deleteChannelControlHistory(type);
			SessionMessages.add(actionRequest, "xmlio-channel-control-clear-history-success");
		} catch (Exception e) {
			SessionErrors.add(actionRequest, "xmlio-channel-control-clear-history-error");
		}
		
		actionResponse.setRenderParameter("tabs1", "manual");
		actionResponse.setRenderParameter("view", "channelControl");
	}
	
	/* ****************************************************
	 *   
	 * Tab Automatic
	 *   
	 * ****************************************************/
	/**
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 */
	private void showTabAutomatic(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
		String view = (String) renderRequest.getParameter("view");
				
		if (view == null || view.equals("")) {
			try {
				showViewDefault(renderRequest, renderResponse);
			} catch (SystemException e) {
				SessionErrors.add(renderRequest, "error-retrieving-channel");
			}
		} else if (view.equalsIgnoreCase("editChannel")) {
			try {
				showViewEditChannel(renderRequest, renderResponse);			
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					SessionErrors.add(renderRequest, "error-retrieving-channel");
				}
			}
		}else if (view.equalsIgnoreCase("channelControl")) {
			try {
				showChannelControl(renderRequest, renderResponse);
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					SessionErrors.add(renderRequest, "error-retrieving-channel");
				}
			}
		}else if (view.equalsIgnoreCase("channelControlLog")) {
			try {
				showChannelControlLog(renderRequest, renderResponse);
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					SessionErrors.add(renderRequest, "error-retrieving-channel");
				}
			}
		}
		
		renderRequest.setAttribute("tabs1", "automatic");
	}
	
	
	
	/**
	 * 
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 * @throws SystemException
	 */	
	public void showViewDefault(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException, SystemException {			
		
		PortletURL addChannelURL = renderResponse.createRenderURL();
		addChannelURL.setParameter("view", "editChannel");
		addChannelURL.setParameter("editType", "add");		
		addChannelURL.setParameter("tabs1", "automatic");
		renderRequest.setAttribute("addChannelURL", addChannelURL.toString());
		
		PortletURL deleteChannelsURL = renderResponse.createActionURL();		
		deleteChannelsURL.setParameter("javax.portlet.action", "deleteSelectedChannels");
		deleteChannelsURL.setParameter("tabs1", "automatic");
		renderRequest.setAttribute("deleteChannelsURL", deleteChannelsURL.toString());
		
		PortletURL liveConfigurationURL = renderResponse.createRenderURL();
		liveConfigurationURL.setParameter("view", "liveConfiguration");	
		liveConfigurationURL.setParameter("tabs1", "live");
		renderRequest.setAttribute("liveConfigurationURL", liveConfigurationURL.toString());
		
		PortletURL liveControlURL = renderResponse.createRenderURL();
		liveControlURL.setParameter("view", "liveControl");	
		liveControlURL.setParameter("tabs1", "live");
		renderRequest.setAttribute("liveControlURL", liveControlURL.toString());
		
		PortletURL channelControlURL = renderResponse.createRenderURL();
		channelControlURL.setParameter("view", "channelControl");	
		channelControlURL.setParameter("tabs1", "automatic");
		channelControlURL.setParameter("tabName", "xmlio-channel-control-automatic");
		renderRequest.setAttribute("channelControlURL", channelControlURL.toString());
		
		PortletURL channelControlLogURL = renderResponse.createRenderURL();
		channelControlLogURL.setParameter("view", "channelControlLog");	
		channelControlLogURL.setParameter("tabs1", "automatic");
		channelControlLogURL.setParameter("tabName", "xmlio-channel-control-automatic");
		renderRequest.setAttribute("channelControlLogURL", channelControlLogURL.toString());

		include(viewJSP, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", "automatic");
	}

	public void showViewEditChannel(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL editChannelURL = renderResponse.createActionURL();
		
		String editType = (String) renderRequest.getParameter("editType");	
		if (editType.equalsIgnoreCase("edit")) {			
			editChannelURL.setParameter("javax.portlet.action", "updateChannel");
			Channel errorChannel = (Channel) renderRequest.getAttribute("errorChannel");
			if (errorChannel != null) {
				renderRequest.setAttribute("channel", errorChannel);
			} else {
				long channelId = Long.parseLong(renderRequest.getParameter("channelId"));
				Channel channel = ChannelLocalServiceUtil.getChannel(channelId);				
				renderRequest.setAttribute("channel", channel);
			}
			
			editChannelURL.setParameter("tabs1", "automatic");
			renderRequest.setAttribute("editChannelURL", editChannelURL.toString());		
		} else {
			editChannelURL.setParameter("javax.portlet.action", "addChannel");
			Channel errorChannel = (Channel) renderRequest.getAttribute("errorChannel");
			if (errorChannel != null) {
				renderRequest.setAttribute("channel", errorChannel);
			} else {				
				renderRequest.setAttribute("channel", new ChannelImpl());
			}	
			
			editChannelURL.setParameter("tabs1", "automatic");
			renderRequest.setAttribute("editChannelURL", editChannelURL.toString());				
		}
		
		include(editChannel, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", "automatic");
	}		

	/**
	 * @param actionRequest
	 * @param actionResponse
	 */
	private void processActionTabAutomatic(ActionRequest actionRequest, ActionResponse actionResponse) {
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		try {
			if (action.equals("deleteChannel")) {				
				deleteChannel(actionRequest, actionResponse);
			} else if (action.equals("deleteSelectedChannels")) {
				deleteSelectedChannels(actionRequest, actionResponse);
			} else if (action.equals("editChannel")) {								
				editChannel(actionRequest, actionResponse);	
			} else if (action.equals("updateChannel")){
				updateChannel(actionRequest, actionResponse);
			} else if (action.equals("addChannel")) {
				addChannel(actionRequest, actionResponse); 
			} else if (action.equals("activateChannel")) {				
				activateChannel(actionRequest, actionResponse);
			} else if (action.equals("deactivateChannel")) {				
				deactivateChannel(actionRequest, actionResponse);
			} else if (action.equals("clearChannelControl")) {
				clearChannelControlHistory(actionRequest, actionResponse);
			} else if (action.equals("viewChannelControlLog")){
				viewChannelControlLog(actionRequest, actionResponse);
			} else if (action.equals("deleteChannelControlItem")) {
				deleteChannelControlItem(actionRequest, actionResponse);
			} 
		} catch (PortalException e) {
			;
		} catch (SystemException e) {
			;
		}
		actionResponse.setRenderParameter("tabs1", "automatic");
	}

	/***
	 * 	
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void deleteChannel(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long channelId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);

		if (channelId != -1) {
			ChannelLocalServiceUtil.deleteChannel(channelId);
			SessionMessages.add(actionRequest, "channel-deleted");
		} else {
			SessionErrors.add(actionRequest, "channel-id-not-exist");
		}
		actionResponse.setRenderParameter("tabs1", "automatic");
	}
	
	/***
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void deleteSelectedChannels(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {		
		long[] channelIds = ParamUtil.getLongValues(actionRequest, "rowIds");
		
		if (channelIds != null) {
			for (int i = 0; i < channelIds.length; i++) {
				ChannelLocalServiceUtil.deleteChannel(channelIds[i]);
			}
			SessionMessages.add(actionRequest, "channel-deleted-all");
		} else {
			SessionErrors.add(actionRequest, "channel-deleted-all-error");
		}
		actionResponse.setRenderParameter("tabs1", "automatic");
	}
	
	/***
	 * 
	 * @param request
	 * @param response
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void editChannel(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long channelId = ParamUtil.getLong(actionRequest, "resourcePrimKey");

		actionResponse.setRenderParameter("channelId", Long.toString(channelId));
		actionResponse.setRenderParameter("view", "editChannel");
		actionResponse.setRenderParameter("tabs1", "automatic");
		actionResponse.setRenderParameter("editType", "edit");
	}
		
	/***
	 * 
	 * Añade un Canal
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void addChannel(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {		
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();
		
		String channelName = ParamUtil.getString(actionRequest,"channelName", "");
		String channelDescription = ParamUtil.getString(actionRequest,"channelDescription", "");
		String channelType = ParamUtil.getString(actionRequest,"channelType", IterKeys.XMLIO_CHANNEL_TYPE_INPUT);
		String channelMode = ParamUtil.getString(actionRequest,"channelMode", IterKeys.XMLIO_CHANNEL_MODE_FILE_SYSTEM);
		String channelFilePath = ParamUtil.getString(actionRequest,"channelFilePath", "");
		String channelXslPath = ParamUtil.getString(actionRequest,"channelXslPath", "");
		String channelFtpServer = ParamUtil.getString(actionRequest,"channelFtpServer", "");
		String channelFtpUser = ParamUtil.getString(actionRequest,"channelFtpUser", "");
		String channelFtpPassword = ParamUtil.getString(actionRequest,"channelFtpPassword", "");
		boolean channelStatus = ParamUtil.getBoolean(actionRequest,"channelStatus", false);
		boolean channelProgram = ParamUtil.getBoolean(actionRequest,"channelProgram", false);
		int channelProgramHour = ParamUtil.getInteger(actionRequest,"channelProgramHour", -1);
		int channelProgramMin = ParamUtil.getInteger(actionRequest,"channelProgramMin", -1);
		String channelRangeType = ParamUtil.getString(actionRequest, "channelRangeType", IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL);
		boolean channelRangeTimeAll = ParamUtil.getBoolean(actionRequest, "channelRangeTimeAll", false);
		int channelRangeTimeValue = ParamUtil.getInteger(actionRequest,"channelRangeTimeValue", -1);
		String channelRangeTimeUnit = ParamUtil.getString(actionRequest, "channelRangeTimeUnit", IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR);

		long id = CounterLocalServiceUtil.increment();
		
		Channel channel = ChannelLocalServiceUtil.createChannel(id);
		channel.setGroupId(groupId);
		channel.setName(channelName);
		channel.setDescription(channelDescription);
		channel.setType(channelType);
		channel.setMode(channelMode);
		channel.setFilePath(channelFilePath);
		channel.setXslPath(channelXslPath);
		channel.setFtpServer(channelFtpServer);
		channel.setFtpUser(channelFtpUser);
		channel.setFtpPassword(channelFtpPassword);
		channel.setStatus(channelStatus);
		channel.setProgram(channelProgram);
		channel.setProgramHour(channelProgramHour);
		channel.setProgramMin(channelProgramMin);
		channel.setRangeType(channelRangeType);
		channel.setRangeTimeAll(channelRangeTimeAll);
		channel.setRangeTimeUnit(channelRangeTimeUnit);
		channel.setRangeTimeValue(channelRangeTimeValue);
		
		ArrayList<String> errors = new ArrayList<String>();
		if (ChannelValidator.validateChannel(channel, errors)) {
			ChannelLocalServiceUtil.addChannel(channel);
			SessionMessages.add(actionRequest, "xmlio-channel-added-success");
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}
			actionResponse.setRenderParameter("view", "editChannel");
			actionResponse.setRenderParameter("editType", "add");
			actionRequest.setAttribute("errorChannel", channel);
		}
		
		actionResponse.setRenderParameter("tabs1", "automatic");
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
	public void updateChannel(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {

		long channelId = ParamUtil.getLong(actionRequest, "channelId", -1);
		
		String channelName = ParamUtil.getString(actionRequest,"channelName", "");
		String channelDescription = ParamUtil.getString(actionRequest,"channelDescription", "");
		String channelType = ParamUtil.getString(actionRequest,"channelType", IterKeys.XMLIO_CHANNEL_TYPE_INPUT);
		String channelMode = ParamUtil.getString(actionRequest,"channelMode", IterKeys.XMLIO_CHANNEL_MODE_FILE_SYSTEM);
		String channelFilePath = ParamUtil.getString(actionRequest,"channelFilePath", "");
		String channelXslPath = ParamUtil.getString(actionRequest,"channelXslPath", "");
		String channelFtpServer = ParamUtil.getString(actionRequest,"channelFtpServer", "");
		String channelFtpUser = ParamUtil.getString(actionRequest,"channelFtpUser", "");
		String channelFtpPassword = ParamUtil.getString(actionRequest,"channelFtpPassword", "");
		boolean channelStatus = ParamUtil.getBoolean(actionRequest,"channelStatus", false);
		boolean channelProgram = ParamUtil.getBoolean(actionRequest,"channelProgram", false);
		int channelProgramHour = ParamUtil.getInteger(actionRequest,"channelProgramHour", -1);
		int channelProgramMin = ParamUtil.getInteger(actionRequest,"channelProgramMin", -1);
		String channelRangeType = ParamUtil.getString(actionRequest, "channelRangeType", IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL);
		boolean channelRangeTimeAll = ParamUtil.getBoolean(actionRequest, "channelRangeTimeAll", false);
		int channelRangeTimeValue = ParamUtil.getInteger(actionRequest,"channelRangeTimeValue", -1);
		String channelRangeTimeUnit = ParamUtil.getString(actionRequest, "channelRangeTimeUnit", IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR);

		Channel channel = ChannelLocalServiceUtil.getChannel(channelId);
		channel.setName(channelName);
		channel.setDescription(channelDescription);
		channel.setType(channelType);
		channel.setMode(channelMode);
		channel.setFilePath(channelFilePath);
		channel.setXslPath(channelXslPath);
		channel.setFtpServer(channelFtpServer);
		channel.setFtpUser(channelFtpUser);
		channel.setFtpPassword(channelFtpPassword);
		channel.setStatus(channelStatus);
		channel.setProgram(channelProgram);
		channel.setProgramHour(channelProgramHour);
		channel.setProgramMin(channelProgramMin);
		channel.setRangeType(channelRangeType);
		channel.setRangeTimeAll(channelRangeTimeAll);
		channel.setRangeTimeUnit(channelRangeTimeUnit);
		channel.setRangeTimeValue(channelRangeTimeValue);
		
		ArrayList<String> errors = new ArrayList<String>();
		if (ChannelValidator.validateChannel(channel, errors)) {
			ChannelLocalServiceUtil.updateChannel(channel);
			SessionMessages.add(actionRequest, "xmlio-channel-updated-success");	
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}
			
			actionResponse.setRenderParameter("view", "editChannel");
			actionResponse.setRenderParameter("editType", "edit");			
			actionRequest.setAttribute("errorChannel", channel);
		}
		
		actionResponse.setRenderParameter("tabs1", "automatic");
		
	}

	public void activateChannel(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException{
		long channelId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		Channel channel = ChannelLocalServiceUtil.getChannel(channelId);
		channel.setStatus(true);
		ChannelLocalServiceUtil.updateChannel(channel);
		actionResponse.setRenderParameter("tabs1", "automatic");
	}
	
	public void deactivateChannel(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException{
		long channelId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		Channel channel = ChannelLocalServiceUtil.getChannel(channelId);
		channel.setStatus(false);
		ChannelLocalServiceUtil.updateChannel(channel);
		actionResponse.setRenderParameter("tabs1", "automatic");
	}	
	
	/* ****************************************************
	 *   
	 * Tab Live
	 *   
	 * ****************************************************/
	
	/**
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 */
	private void showTabLive(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {

		String view = (String) renderRequest.getParameter("view");
		
		if (view == null || view.equals("")) {
			try {
				showViewDefault(renderRequest, renderResponse);
			} catch (SystemException e) {
				SessionErrors.add(renderRequest, "error-retrieving-channel");
			}
		} else if (view.equalsIgnoreCase("liveConfiguration")) {
			try {
				showLiveConfiguration(renderRequest, renderResponse);			
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					;
				}
			}
		} else if (view.equalsIgnoreCase("liveControl")) {
			try {
				showLiveControl(renderRequest, renderResponse);			
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					;
				}
			}
		} else if (view.equalsIgnoreCase("liveItem")) {
				try {
					showLiveItem(renderRequest, renderResponse);			
				} catch (Exception ex) {
					;
					try {
						showViewDefault(renderRequest, renderResponse);
					} catch (SystemException ex1) {
						;
					}
				}
			}
		renderRequest.setAttribute("tabs1", "live");
	
	}
	
	/**
	 * @param actionRequest
	 * @param actionResponse
	 */
	private void processActionTabLive(ActionRequest actionRequest, ActionResponse actionResponse) {
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		
		try {
			if (action.equals("publishToLive")) {
				publishToLive(actionRequest, actionResponse);
			} else if (action.equals("populateLive")){
				populateLive(actionRequest, actionResponse);
			} else if (action.equals("cleanLive")) {
				cleanLive(actionRequest, actionResponse);
			} else if (action.equals("setLiveFilter")){
				setLiveFilter(actionRequest, actionResponse);
			} else if (action.equals("viewLiveItem")){
				liveItem(actionRequest, actionResponse);
			} else if (action.equals("updateLiveConfiguration")){
				updateLiveConfiguration(actionRequest, actionResponse);
			} else if (action.equals("clearLiveControl")){
				clearLiveControl(actionRequest, actionResponse);
			}else if (action.equals("unlockLiveControl")){
				unlockLiveControl(actionRequest, actionResponse);
			}
		} catch (Exception e) {}
	}
	
	
	/***
	 * 
	 * Publica los contenidos a Live
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void publishToLive(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {

		long companyId = ParamUtil.getLong(actionRequest, "companyId", -1);
		long groupId = ParamUtil.getLong(actionRequest, "groupId", -1);
		long userId = ParamUtil.getLong(actionRequest, "userId", -1);
		String livePublishRangeType = ParamUtil.getString(actionRequest,"liveRangeType", IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL).equals(IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL) ? null : ParamUtil.getString(actionRequest,"liveRangeType", IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL);
		String livePublishItemId = ParamUtil.getString(actionRequest, "resourcePrimKey", "");
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		long scopeGroupId = themeDisplay.getDoAsGroupId();
		
		String [] livePublishItemIds = null;
		if (!livePublishItemId.equals("")){
			try{
				livePublishItemIds = new String[] {livePublishItemId};
			}
			catch(Exception err){}
		}
		
		List<String> publishErrors = new ArrayList<String>();
		LiveLocalServiceUtil.publishToLiveGUI(companyId, groupId, userId, livePublishRangeType, livePublishItemIds, publishErrors, scopeGroupId);
		
		if (publishErrors.isEmpty()){
			SessionMessages.add(actionRequest, "xmlio-live-group-publish-success");	
		}
		else{
			SessionErrors.add(actionRequest, "xmlio-live-group-publish-error");
			for(String error : publishErrors){
				SessionErrors.add(actionRequest, error);
			}
		}
		actionResponse.setRenderParameter("tabs1", "live");
	}
	
	public void populateLive(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {

		long companyId = ParamUtil.getLong(actionRequest, "companyId", -1);
		long groupId = ParamUtil.getLong(actionRequest, "groupId", -1);

		try{
			LiveLocalServiceUtil.populateLive(groupId, companyId);
			SessionMessages.add(actionRequest, "xmlio-live-populate-live-success");
		}
		catch(Exception err){
			SessionErrors.add(actionRequest, "xmlio-live-populate-live-error");
			_log.error(err);
		}
		finally{
			actionResponse.setRenderParameter("tabs1", "live");
		}	
	}
	
	public void cleanLive(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {

		long companyId = ParamUtil.getLong(actionRequest, "companyId", -1);
		long groupId = ParamUtil.getLong(actionRequest, "groupId", -1);

		try{			
			LiveLocalServiceUtil.deleteLive(groupId, companyId);
			SessionMessages.add(actionRequest, "xmlio-live-clean-live-success");
		}
		catch(Exception err){
			SessionErrors.add(actionRequest, "xmlio-live-clean-live-error");
		}
		finally{
			actionResponse.setRenderParameter("tabs1", "live");
		}	
	}
	
	public void setLiveFilter(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {
		long liveGroupId = ParamUtil.getLong(actionRequest, "liveGroupId", -1);
		String liveClassName = ParamUtil.getString(actionRequest, "liveClassName", "");
		String liveLocalId = ParamUtil.getString(actionRequest, "liveLocalId", "");
		String orderByCol = ParamUtil.getString(actionRequest, "orderByCol", "");
		String orderByType = ParamUtil.getString(actionRequest, "orderByType", "");
		actionResponse.setRenderParameter("liveGroupId", Long.toString(liveGroupId));
		actionResponse.setRenderParameter("liveClassName", liveClassName);
		actionResponse.setRenderParameter("liveLocalId", liveLocalId);
		actionResponse.setRenderParameter("orderByCol", orderByCol);
		actionResponse.setRenderParameter("orderByType", orderByType);
		actionResponse.setRenderParameter("tabs1", "live");
	}

	public void showLiveItem(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL liveItemURL = renderResponse.createActionURL();
					
		liveItemURL.setParameter("javax.portlet.action", "liveItem");
		renderRequest.setAttribute("liveItemlURL", liveItemURL.toString());		
		
		include(liveItem, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", "live");
	}	
	
	public void liveItem(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long liveId = ParamUtil.getLong(actionRequest, "resourcePrimKey");

		actionResponse.setRenderParameter("liveId", Long.toString(liveId));
		actionResponse.setRenderParameter("view", "liveItem");
		actionResponse.setRenderParameter("tabs1", "live");
		actionResponse.setRenderParameter("editType", "view");
	}

	/* ****************************************************
	 *   
	 * Live Control & Live Configuration Views
	 *   
	 * ****************************************************/

	public void showLiveConfiguration(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL liveConfigurationURL = renderResponse.createActionURL();
				
					
		liveConfigurationURL.setParameter("javax.portlet.action", "liveConfiguration");
		renderRequest.setAttribute("liveConfigurationURL", liveConfigurationURL.toString());		
		
		include(liveConfiguration, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", "live");
	}	
	
	public void showLiveControl(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL liveControlURL = renderResponse.createActionURL();
				
					
		liveControlURL.setParameter("javax.portlet.action", "liveControl");
		renderRequest.setAttribute("liveControlURL", liveControlURL.toString());		
		
		include(liveControl, renderRequest, renderResponse);
		renderRequest.setAttribute("tabs1", "live");
	}	
	
	/***
	 * 
	 * Crea o actualiza LiveConfiguration
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 * @throws ServiceError 
	 */
	public void updateLiveConfiguration(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException, ServiceError {
		
		long id = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		long companyId = ParamUtil.getLong(actionRequest, "companyId", -1);
		String localPath = ParamUtil.getString(actionRequest, "localPath", "");
		String remoteIterServer = ParamUtil.getString(actionRequest, "remoteIterServer", "");
		String gatewayHost = ParamUtil.getString(actionRequest, "gatewayHost", "");
		//DestinationType = Channel or Explicit
		String destinationType = ParamUtil.getString(actionRequest, "destinationType", "");
		long remoteChannelId = ParamUtil.getLong(actionRequest, "remoteChannelId", -1);
		long remoteUserId = ParamUtil.getLong(actionRequest, "remoteUserId", -1);
		String remoteUserName = ParamUtil.getString(actionRequest, "remoteUserName", "");
		String remoteUserPassword = ParamUtil.getString(actionRequest, "remoteUserPassword", "");
		
		long remoteCompanyId = ParamUtil.getLong(actionRequest, "remoteCompanyId", -1);
		if (remoteCompanyId == -1) remoteCompanyId = companyId;
		long remoteGlobalGroupId = ParamUtil.getLong(actionRequest, "remoteGlobalGroupId", -1);
		if (remoteGlobalGroupId == -1) remoteGlobalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
		//OutputMethod = FileSystem or FTP
		String outputMethod = ParamUtil.getString(actionRequest, "outputMethod", "");
		String remotePath = ParamUtil.getString(actionRequest, "remotePath", "");
		String ftpPath = ParamUtil.getString(actionRequest, "ftpPath", "");
		String ftpUser = ParamUtil.getString(actionRequest, "ftpUser", "");
		String ftpPassword=ParamUtil.getString(actionRequest, "ftpPassword", "");
		boolean archive = ParamUtil.getBoolean(actionRequest, "archive", false);
		long connectionTimeout = ParamUtil.getLong(actionRequest, "connectionTimeout", -1);
		long operationTimeout = ParamUtil.getLong(actionRequest, "operationTimeout",-1);
		
		
		if (connectionTimeout == -1){
			connectionTimeout = IterKeys.XMLIO_PUBLISH_TIMEOUT_CONNECTION;
		}else{
			connectionTimeout = connectionTimeout * 1000;
		}
		
		if (operationTimeout == -1){
			operationTimeout = IterKeys.XMLIO_PUBLISH_TIMEOUT_SOCKET;
		}else{
			operationTimeout = operationTimeout * 1000;
		}
		
		LiveConfiguration liveConf = null;
		
		if (id == -1){
			id = CounterLocalServiceUtil.increment();
			liveConf = LiveConfigurationLocalServiceUtil.createLiveConfiguration(id);
		}else{
			liveConf = LiveConfigurationLocalServiceUtil.getLiveConfiguration(id);
		}
		
		liveConf.setCompanyId(companyId);
		liveConf.setLocalPath(localPath);
		liveConf.setRemoteIterServer(remoteIterServer);
		liveConf.setGatewayHost(gatewayHost);
		
		liveConf.setDestinationType(destinationType);
		liveConf.setRemoteChannelId(remoteChannelId);
		liveConf.setRemotePath(remotePath);
		liveConf.setOutputMethod(outputMethod);
		liveConf.setFtpPath(ftpPath);
		liveConf.setFtpUser(ftpUser);
		liveConf.setFtpPassword(ftpPassword);
		liveConf.setRemoteUserId(remoteUserId);
		liveConf.setRemoteUserName(remoteUserName);
		liveConf.setRemoteUserPassword(remoteUserPassword);
		liveConf.setRemoteCompanyId(remoteCompanyId);
		liveConf.setRemoteGlobalGroupId(remoteGlobalGroupId);
		liveConf.setArchive(archive);
		liveConf.setConnectionTimeOut(connectionTimeout);
		liveConf.setOperationTimeOut(operationTimeout);
		
		ArrayList<String> errors = new ArrayList<String>();
		if (LiveConfigurationValidator.validateLiveConfiguration(liveConf, errors)) {
			LiveConfigurationLocalServiceUtil.updateLiveConfiguration(liveConf);
			SessionMessages.add(actionRequest, "xmlio-live-configuration-updated");	
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}	
		}
		actionResponse.setRenderParameter("tabs1", "live");
		actionResponse.setRenderParameter("view", "liveConfiguration");
	}
	
	public void clearLiveControl(ActionRequest actionRequest, ActionResponse actionResponse){
		try{
			LiveControlLocalServiceUtil.clearLiveControl();
			SessionMessages.add(actionRequest, "xmlio-live-control-clear-success");
		}
		catch(Exception err){
			SessionMessages.add(actionRequest, "xmlio-live-control-clear-error");
		}
		finally{
			actionResponse.setRenderParameter("tabs1", "live");
			actionResponse.setRenderParameter("view", "liveControl");
		}
	}
	
	public void unlockLiveControl(ActionRequest actionRequest, ActionResponse actionResponse){
		try{
			String processId = ParamUtil.getString(actionRequest, "processId", "");			
			
			if (processId.equals("all")){
				LiveControlLocalServiceUtil.unlockAllLiveControl();
			}else{
				LiveControlLocalServiceUtil.unlockLiveControl(processId);
			}
			SessionMessages.add(actionRequest, "xmlio-live-control-unlock-success");
		}
		catch(Exception err){
			SessionMessages.add(actionRequest, "xmlio-live-control-unlock-error");
		}
		finally{
			actionResponse.setRenderParameter("tabs1", "live");
			actionResponse.setRenderParameter("view", "liveControl");
		}
	}
}
