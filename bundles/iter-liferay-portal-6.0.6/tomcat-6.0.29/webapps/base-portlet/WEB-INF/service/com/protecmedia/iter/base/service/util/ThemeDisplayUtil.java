package com.protecmedia.iter.base.service.util;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.theme.ThemeDisplay;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;

public class ThemeDisplayUtil
{
	public static ThemeDisplay buildThemeDisplay ( Document xmlInitParams, HttpServletRequest request ) throws Exception
	{
		Element root = xmlInitParams.getRootElement();

		long scopeGroupId				= GetterUtil.get(root.selectSingleNode("scopeGroupId").getText(), 				0);
		long companyId					= GetterUtil.get(root.selectSingleNode("companyId").getText(), 					0);
		String languageId				= GetterUtil.get(root.selectSingleNode("languageId").getText(), 				"");
		long plid						= GetterUtil.get(root.selectSingleNode("plid").getText(), 						0);
		boolean secure 					= GetterUtil.get(root.selectSingleNode("secure").getText(), 					false);
		long userId						= GetterUtil.get(root.selectSingleNode("userId").getText(), 					0);
		boolean lifecycleRender 		= GetterUtil.get(root.selectSingleNode("lifecycleRender").getText(), 			false);
		String pathFriendlyURLPublic	= GetterUtil.get(root.selectSingleNode("pathFriendlyURLPublic").getText(), 		"");
		String pathFriendlyURLPrivateUsr= GetterUtil.get(root.selectSingleNode("pathFriendlyURLPrivateUser").getText(), "");
		String pathFriendlyURLPrivateGrp= GetterUtil.get(root.selectSingleNode("pathFriendlyURLPrivateGroup").getText(),"");
		String serverName				= GetterUtil.get(root.selectSingleNode("serverName").getText(),					"");
		String cdnHost					= GetterUtil.get(root.selectSingleNode("cdnHost").getText(),					"");
		
		String pathImage				= GetterUtil.get(root.selectSingleNode("pathImage").getText(),					"");
		String pathMain					= GetterUtil.get(root.selectSingleNode("pathMain").getText(),					"");
		String pathContext				= GetterUtil.get(root.selectSingleNode("pathContext").getText(),				"");
		String urlPortal				= GetterUtil.get(root.selectSingleNode("urlPortal").getText(),					"");
		String pathThemeImages			= GetterUtil.get(root.selectSingleNode("pathThemeImages").getText(),			"");
		

		ThemeDisplay themeDisplay = IterLocalServiceUtil.rebuildThemeDisplayForAjax(scopeGroupId, companyId, languageId,
				plid, secure, userId, lifecycleRender, pathFriendlyURLPublic, pathFriendlyURLPrivateUsr, 
				pathFriendlyURLPrivateGrp, serverName, cdnHost, pathImage, pathMain, pathContext, urlPortal,
				pathThemeImages, request);
		
		return themeDisplay;
	}
}
