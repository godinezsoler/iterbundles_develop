package com.protecmedia.iter.services.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.portlet.BaseConfigurationAction;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;

public class FinanceStockMarketAction extends BaseConfigurationAction {
	
	@Override
	public void processAction(PortletConfig portletConfig,
			ActionRequest actionRequest, ActionResponse actionResponse)
			throws Exception {

		String portletResource = ParamUtil.getString(actionRequest, "portletResource");
		PortletPreferences preferences = PortletPreferencesFactoryUtil.getPortletSetup(actionRequest, portletResource);
		
		//obtenemos los parametros de config
		String tradesListPlain = ParamUtil.getString(actionRequest, "tradesListPlain", "");		
		int updateFrecuency = ParamUtil.getInteger(actionRequest, "updateFrecuency", 30000);
		boolean showChart = ParamUtil.getBoolean(actionRequest, "showChart");
			
		//salvamos las preferencias
		preferences.setValue("tradesListPlain", tradesListPlain);
		preferences.setValue("updateFrecuency", String.valueOf(updateFrecuency));
		preferences.setValue("showChart", String.valueOf(showChart));
		
		preferences.store();		

		SessionMessages.add(actionRequest, portletConfig.getPortletName() + ".doConfigure");
		
	}
	
	public String render(PortletConfig portletConfig, RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		return "/html/financestockmarketportlet/configuration.jsp";
	}
	
}
