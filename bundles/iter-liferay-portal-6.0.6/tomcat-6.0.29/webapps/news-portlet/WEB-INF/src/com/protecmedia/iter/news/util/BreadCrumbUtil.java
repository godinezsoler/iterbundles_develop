package com.protecmedia.iter.news.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;



public class BreadCrumbUtil {
	
	private static Log _log = LogFactoryUtil.getLog(BreadCrumbUtil.class);

	public static String getBreadcrumb(HttpServletRequest request,long globalGroupId,boolean showWebName,String webName, String separator) throws SystemException, PortalException{
		
		String breadCrumb = "";
		
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		long scopeGroupId = themeDisplay.getScopeGroupId();
		
		long currentPlid = 0;
		String contentId = "";
		String sectionPath = "";
		if (themeDisplay.getURLCurrent().contains(PortalUtil.getNewsMappingPrefix()) || themeDisplay.getURLCurrent().contains("/-"))
		{
			contentId = request.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
			sectionPath = request.getParameter(WebKeys.URL_PARAM_CONTENT_SECTION);
		}
		
		if(sectionPath==null || sectionPath.equals("") || sectionPath.equals("0") || !sectionIsValid(sectionPath,scopeGroupId, contentId))
		{
			JournalArticle article = null;
			try
			{
				// Estamos dentro del articulo
				if (contentId!=null && !contentId.equals(""))
				{
					article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
					currentPlid = getLayoutPlidFromWebContentId(scopeGroupId,article,themeDisplay.getLayout(),contentId,request);
				}
				// Estamos en una seccion
				else
				{
					currentPlid = SectionUtil.getSectionPlid(request);
				}
			}
			catch(Exception e)
			{
				_log.error("Cannot obtain Breadcrumb", e);
			}
	
			breadCrumb = getBreadcrumbFromLayoutPlid(currentPlid,request,showWebName,webName, separator);
		}
		else
		{
			breadCrumb = getBreadcrumbFromPath(scopeGroupId, sectionPath,request,showWebName, webName, separator);
		}
		
		return breadCrumb;
	
	}
	
	public static boolean sectionIsValid(String sectionPath, long groupId, String contentId) throws SystemException, PortalException{
		
		List<PageContent> pageContents = PageContentLocalServiceUtil.getPageContents(groupId, contentId);
		for (PageContent pageContent : pageContents){
			String lauyoutUuid = pageContent.getLayoutId();
			String section = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(lauyoutUuid, groupId).getFriendlyURL();
			if (section.substring(1).equalsIgnoreCase(sectionPath.replace(" ", "/"))){
				return true;
			}
				
		}
		
		return false;
	}
	
	
	public static String getBreadcrumbFromPath(long groupId, String sectionPath, HttpServletRequest request, boolean showWebName, String webName, String separator){
		
		long plid = 0;
		
		try{
			sectionPath = "/"+sectionPath.replace(' ', '/');
			plid = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, sectionPath ).getPlid();
		}
		catch (Exception e) {
			_log.error("Cannot obtain Breadcrumb from FriendlyURL. " + e.toString());
		}
		
		return getBreadcrumbFromLayoutPlid(plid, request, showWebName,webName, separator);
	}
	
	public static String getBreadcrumbFromLayoutPlid(long plid, HttpServletRequest request, boolean showWebName, String webName, String separator){
		 
		String breadCrumbString = "";
		Layout section = null;
		List<KeyValuePair> listPairs = new ArrayList<KeyValuePair>();
		
		while (plid != 0){
			try {
				section = LayoutLocalServiceUtil.getLayout(plid);
				String url = section.getRegularURL(request);
				String name = section.getName(request.getLocale(), true);
				KeyValuePair pair = new KeyValuePair();
				pair.setKey(url);
				pair.setValue(name);
				listPairs.add(pair);
				plid = section.getParentPlid();
				
			} catch (Exception e) {
				_log.error("Cannot obtain Breadcrumb from LayoutPlid", e);
			}
		}
	
		breadCrumbString = generateBreadCrumbHtml(listPairs,request,showWebName, webName, separator);
		return breadCrumbString;
		
	} 
	
	
	public static String generateBreadCrumbHtml(List<KeyValuePair> listPairs,  HttpServletRequest request, boolean showWebName, String webName, String separator)
	{
		
		StringBuffer sb = new StringBuffer();
		
		// ITER-1280 Evitar el marcaje automático con Microdatos para Google Structured Data Tool
		sb.append( String.format("<ol class='iter-theme-news-breadcrumb' %s>", 
					PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED 	? 
					"" 												: 
					"itemscope itemtype='http://schema.org/BreadcrumbList'") );

		if (showWebName)
		{
			ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
			long scopeGroupId = themeDisplay.getScopeGroupId();
			String webUrl = "";
			try 
			{
				//ticket 0009939. 
				String host = IterURLUtil.getIterHost();
				webUrl = host.concat(IterURLUtil.getIterURLPrefix());
				
					/* si webName es "", por defecto tendrá el nombre del sitio web */ 
				if(webName.equals(StringPool.BLANK))
					webName = GroupLocalServiceUtil.getGroup(scopeGroupId).getName();
				
			}
			catch (Exception e) 
			{
				_log.error("Cannot obtain Web Info", e);
			}
			
			KeyValuePair pair = new KeyValuePair();
			pair.setKey(webUrl);
			pair.setValue(webName);
			listPairs.add(pair);

			sb.append( 	String.format("<li class='first' %s>",
						PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED  ?
						"" 												:
						"itemprop='itemListElement' itemscope itemtype='http://schema.org/ListItem'") );
			
			sb.append( 	String.format("<a %s href='%s'><span %s>%s</span></a>",
						PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED	? "" : "itemprop='item'",
						webUrl, 
						PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED	? "" : "itemprop='name'",
						webName) );
			
			sb.append( 	String.format("<meta %s content='1' />",
						PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED	? "" : "itemprop='position'") );
			
			sb.append("</li>");
		}
		for (int a=listPairs.size()-1;a>=0;a--)
		{
			int pos = listPairs.size() - a;
			KeyValuePair pair = listPairs.get(a);
			String url = pair.getKey();
			String name = pair.getValue();
			//Primer nivel
			if (a==listPairs.size()-1)
			{
				if (!showWebName){
					if (a==0)
						sb.append( 	String.format("<li class='first last' %s>",
									PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" :
									"itemprop='itemListElement' itemscope itemtype='http://schema.org/ListItem'") );
					else
						sb.append(	String.format("<li class='first' %s>",
									PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" :
									"itemprop='itemListElement' itemscope itemtype='http://schema.org/ListItem'") );
					
					sb.append(	String.format("<a %s href='%s'><span %s>%s</span></a>", 
								PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='item'",
								url, 
								PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='name'",
								name) );
					
					sb.append(	String.format("<meta %s content='%d' />", 
								PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='position'",
								pos) );
					sb.append("</li>");
				}
				
			}
			//Siguientes niveles
			else
			{
				//ultimo nivel
				if (a==0)
				{
					sb.append(	String.format("<li class='last' %s>",
								PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='itemListElement' itemscope itemtype='http://schema.org/ListItem'") );
				}
				else
				{
					sb.append(	String.format("<li %s>",
								PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='itemListElement' itemscope itemtype='http://schema.org/ListItem'") );
				}
				
				sb.append(separator);
				sb.append("&nbsp;");
				sb.append(	String.format("<a %s href='%s'><span %s>%s</span></a>", 
							PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='item'",
							url, 
							PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='name'",
							name) );
				
				sb.append(	String.format("<meta %s content='%d' />", 
							PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop='position'",
							pos)	);
				sb.append("</li>");
			}
		}
		
		sb.append("</ol>");
		
		return sb.toString();
		
	}

	
	public static long getLayoutPlidFromWebContentId(long groupId, JournalArticle webContent, Layout layout, String contentId, HttpServletRequest request){
		
		long plid = 0;
		
		try{
			
			PageContent pc = PageContentLocalServiceUtil.getDefaultPageContentByContentId(groupId, webContent.getArticleId());
			
			if(pc==null){
				pc = PageContentLocalServiceUtil.getFirstPageContentByModel(groupId, webContent.getArticleId(), layout.getPlid());
				if (pc == null)
					pc  = PageContentLocalServiceUtil.getFirstPageContent(groupId, layout.getUuid());
			}
			
			if (pc != null){
				Layout parentLayout = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(pc.getLayoutId(), groupId);
				plid = parentLayout.getPlid();
			}
		
		}
		catch(Exception e){
			_log.error("Cannot obtain plid from WebContenId", e);
		}
		
		return plid;
		
	}
	

}
