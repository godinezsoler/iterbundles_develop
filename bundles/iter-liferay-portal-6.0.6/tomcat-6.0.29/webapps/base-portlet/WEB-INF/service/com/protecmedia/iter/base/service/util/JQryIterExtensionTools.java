package com.protecmedia.iter.base.service.util;

import java.util.List;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

public class JQryIterExtensionTools
{
	private static Log _log = LogFactoryUtil.getLog(JQryIterExtensionTools.class);
	
	private static final String GET_LINKS_TO_CURRENT_LAYOUT = new StringBuilder()
					.append(" SELECT l1.plid \n")
					.append(" FROM Layout l1 INNER JOIN Layout l2 ON \n\t (l1.groupid=l2.groupid AND l1.iconimageid=l2.layoutid) \n")
					.append(" WHERE l1.type_='%s' AND l1.groupid=%s \n")
					.append("\t AND l2.plid IN (%s) ")
					.toString();
	
	public static String getContextSections()
	{
		JSONArray objectArray = JSONFactoryUtil.createJSONArray();
		
		try
		{
			StringBuilder plidHierarchy = new StringBuilder();
			
			ThemeDisplay themeDisplay = ((ThemeDisplay)IterRequest.getAttribute(WebKeys.THEME_DISPLAY));
			
			long layoutplid = SectionUtil.getSectionPlid(IterRequest.getOriginalRequest());
			
			String urlType = StringPool.BLANK;
			if (layoutplid == 0)
			{
				urlType = SectionUtil.getURLType( IterRequest.getOriginalRequest() );
				
				if (urlType.equals(SectionUtil.URLTYPE_DATE))
						layoutplid = themeDisplay.getOriginalPlid();
			}
				
			if(layoutplid!=0)
			{
				String sql = String.format( GET_LINKS_TO_CURRENT_LAYOUT, LayoutConstants.TYPE_LINK_TO_LAYOUT, themeDisplay.getScopeGroupId(), layoutplid );
				
				_log.debug("GET_LINKS_TO_CURRENT_LAYOUT: " + sql);
				
				List<Object> linksToCurrentLayout = PortalLocalServiceUtil.executeQueryAsList(sql);
				
				for(Object linktolayout : linksToCurrentLayout)
				{
					String linkerPlid = String.valueOf(linktolayout);
	
					if( plidHierarchy.length()>0 )
						plidHierarchy.append( StringPool.COMMA );
					
					plidHierarchy.append( linkerPlid );
				}
			}
			
			if( plidHierarchy.length()>0 )
				plidHierarchy.append( StringPool.COMMA );
			
			plidHierarchy.append( 
									(layoutplid == 0)	? 
											"0"			: 
											SectionUtil.getPlidHierarchy(IterRequest.getOriginalRequest(), layoutplid) 
								);
			
			String plids = plidHierarchy.toString();
			
			if(_log.isDebugEnabled())
				_log.debug("layoutplid: " + layoutplid + ", urlType: " + urlType + ", plidHierarchy: " + plids + ", scopeGroupId:" + themeDisplay.getScopeGroupId());
			
			String[] contextSections = plids.equals("0") ? new String[]{} : SectionUtil.getMD5SectionNamesHierarchy(plids, themeDisplay.getScopeGroupId());
		
			String ctxSect = "";
			
			for(String contextSection : contextSections)
			{
				JSONObject object = JSONFactoryUtil.createJSONObject();
				object.put("sectid", contextSection);
				objectArray.put(object);
					
					if(_log.isDebugEnabled())
						ctxSect += " | " + contextSection;
			}

			if( _log.isDebugEnabled() )
				_log.debug("contextSections: " + ctxSect + " for plidHierarchy: " + plids);
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.debug(e);
		}
		
		return objectArray.toString();
	}
	
}
