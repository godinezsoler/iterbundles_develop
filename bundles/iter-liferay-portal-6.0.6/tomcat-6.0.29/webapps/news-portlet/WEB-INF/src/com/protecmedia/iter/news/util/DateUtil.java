package com.protecmedia.iter.news.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.protecmedia.iter.base.service.util.PortletMgr;

public class DateUtil 
{
	
	private static Log _log = LogFactoryUtil.getLog(DateUtil.class);
	
	public static String formatDate(Date date, String format, Locale locale)
	{
		String formatedDate = "";
		
		if(date != null && format != null)
		{
			SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
			formatedDate = sdf.format(date);
		}

		return formatedDate;
	}
	
	
	public static DatePortletPreferences fillPreferencesPortlet(long scopeGroupId)
	{	
		DatePortletPreferences serverPrefs = new DatePortletPreferences();
		
		try
		{
			Document defaultPreferencesDom = PortletMgr.getCommunityConfig(String.valueOf(scopeGroupId));
			Node lastUpdateData = defaultPreferencesDom.selectSingleNode("//row/lastUpdated");
			
			if (lastUpdateData != null && lastUpdateData.getText() != null && !lastUpdateData.getText().isEmpty())
			{
				Document newDefaultPreferencesDom = SAXReaderUtil.read(lastUpdateData.getText());
				List<Node> nodeNames = newDefaultPreferencesDom.selectNodes("//preference/name");
				List<Node> nodeValues = newDefaultPreferencesDom.selectNodes("//preference/value");
				for(int i = 0; i < nodeNames.size(); i++)
				{
					String currentPrefName = nodeNames.get(i).getText();
					String currentPrefValue =  nodeValues.get(i).getText();
					if(currentPrefName.equals("textBefore"))
					{
						serverPrefs.setTextBefore(currentPrefValue);
					}
					else if(currentPrefName.equals("textAfter"))
					{
						serverPrefs.setTextAfter(currentPrefValue);
					}
					else if(currentPrefName.equals("dateWithoutHourFormat"))
					{
						serverPrefs.setDateWithoutHourFormat(currentPrefValue);
					}
					else if(currentPrefName.equals("hourFormat"))
					{
						serverPrefs.setHourFormat(currentPrefValue);
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return serverPrefs;
	}
}