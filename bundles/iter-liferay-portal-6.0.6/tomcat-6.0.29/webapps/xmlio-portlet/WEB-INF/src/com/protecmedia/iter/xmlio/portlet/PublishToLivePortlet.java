package com.protecmedia.iter.xmlio.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

@Deprecated
public class PublishToLivePortlet extends MVCPortlet {

	@Override
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws IOException, PortletException 
	{
//		
//		super.processAction(actionRequest, actionResponse);
//		
//		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
//		Layout layout = themeDisplay.getLayout();
//		long companyId = themeDisplay.getCompanyId();
//		long userId = themeDisplay.getUserId();
//		long groupId = layout.getGroupId();
//		
//		//String action = ParamUtil.getString(actionRequest, "action", "");
//		String publishMode = ParamUtil.getString(actionRequest, "publishMode", "publish-group");
//		HttpServletRequest httpReq = PortalUtil.getHttpServletRequest(actionRequest);
//		
//		//Operacion
//		List<String> publishErrors = new ArrayList<String>();
//		String publishLog = "";
//		if (publishMode.equals("publish-group")){
//			publishLog = LiveLocalServiceUtil.publishToLive(companyId, groupId, userId, publishErrors);	
//		}
//		else{	
//			try{
//				Live liveLayout = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_LAYOUT, String.valueOf(layout.getPlid()));
//				publishLog = LiveLocalServiceUtil.publishToLive(companyId, groupId, userId, IterKeys.CLASSNAME_LAYOUT, new String[]{String.valueOf(liveLayout.getId())}, publishErrors);
//			}catch(Exception err){
//				SessionMessages.add(httpReq.getSession(), "xmlio-live-publish-error");
//			}
//		}
//		
//		//Logs
//		if (publishErrors.isEmpty()){	
//			SessionMessages.add(httpReq.getSession(), "xmlio-live-publish-success");
//		}
//		else{
//			//TODO Añadir el log recibido
//			SessionMessages.add(httpReq.getSession(), "xmlio-live-publish-error");
//			/*for(String error : publishErrors){
//				SessionMessages.add(actionRequest, error);
//			}*/
//		}
	}
}
