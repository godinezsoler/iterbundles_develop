package com.protecmedia.iter.news.util;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.IMetadataControlUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.MetadataControlUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.theme.ThemeDisplay;

public class SectionMetadataControlUtil extends MetadataControlUtil implements IMetadataControlUtil
{
	private static Log _log = LogFactoryUtil.getLog(SectionMetadataControlUtil.class);
	
	private final String META_NAME_KEYWORDS = "keywords";
	private final String META_NAME_DESCRIPTION = "description";
	private final String META_NAME_ROBOTS = "robots";
	
	private Layout _layout = null;
	private HttpServletRequest _request = null;
	private String _currentLanguageId = "";
	private String _defaultLanguageId = "";
	private String _metaRobots = "";
	private String _metaDescription = "";
	private String _metaKeywords = "";
	
	public SectionMetadataControlUtil(ThemeDisplay themeDisplay, HttpServletRequest request, Layout layout, boolean isDetail) throws PortalException, SystemException, MalformedURLException
	{
		super(themeDisplay);
		
		_layout = layout;
		_request = request;
		
		_currentLanguageId = LanguageUtil.getLanguageId(_request);
		Locale defaultLocale = LocaleUtil.getDefault();
		_defaultLanguageId = LocaleUtil.toLanguageId(defaultLocale);
		
		if(_layout!=null)
		{
			getMetaDescription();
			getMetaKeywords();
			if(!isDetail)
				getMetaRobots();
		}
	}
	
	private void getMetaKeywords()
	{
		_metaKeywords = _layout.getTypeSettingsProperties().getProperty("meta-keywords_" + _currentLanguageId);

		if (Validator.isNull(_metaKeywords))
		{
			_metaKeywords = _layout.getTypeSettingsProperties().getProperty("meta-keywords_" + _defaultLanguageId);
		}

		List<String> dynamicMetaKeywords = (List<String>)_request.getAttribute(WebKeys.PAGE_KEYWORDS);

		if (dynamicMetaKeywords != null)
		{
			if (Validator.isNotNull(_metaKeywords))
			{
				StringBundler sb = new StringBundler(4);

				sb.append(_metaKeywords);
				sb.append(StringPool.COMMA);
				sb.append(StringPool.SPACE);
				sb.append(StringUtil.merge(dynamicMetaKeywords));

				_metaKeywords = sb.toString();
			}
			else {
				_metaKeywords = StringUtil.merge(dynamicMetaKeywords);
			}
		}
	}

	private void getMetaDescription()
	{
		_metaDescription = _layout.getTypeSettingsProperties().getProperty("meta-description_" + _currentLanguageId);

		if (Validator.isNull(_metaDescription))
		{
			_metaDescription = _layout.getTypeSettingsProperties().getProperty("meta-description_" + _defaultLanguageId);
		}

		String dynamicMetaDescription = (String)_request.getAttribute(WebKeys.PAGE_DESCRIPTION);

		if (Validator.isNotNull(dynamicMetaDescription))
		{
			if (Validator.isNotNull(_metaDescription))
			{
				StringBundler sb = new StringBundler(4);
				sb.append(_metaDescription);
				sb.append(StringPool.PERIOD);
				sb.append(StringPool.SPACE);
				sb.append(dynamicMetaDescription);

				_metaDescription = sb.toString();
			}
			else
			{
				_metaDescription = dynamicMetaDescription;
			}
		}
	}

	private void getMetaRobots()
	{
		_metaRobots = _layout.getTypeSettingsProperties().getProperty("meta-robots_" + _currentLanguageId);

		if (Validator.isNull(_metaRobots))
		{
			_metaRobots = _layout.getTypeSettingsProperties().getProperty("meta-robots_" + _defaultLanguageId);
		}
	}

	@Override
	public String getPageTitle()
	{
		return LayoutConstants.getPageTitle(_layout);
	}
	
	@Override
	public String getPageDescription()
	{
		return Validator.isNotNull(_metaDescription) ? _metaDescription : "";
	}
	
	@Override
	public String getPageKeywords()
	{
		return Validator.isNotNull(_metaKeywords) ? _metaKeywords : "";
	}
	
	@Override
	public String getPageImage() throws PortalException, SystemException, MalformedURLException
	{
		return getDefaultPageImage();
	}
	
	public Document getPageOpenGraphs() throws PortalException, SystemException, MalformedURLException, ServiceError
	{
		return super.getPageOpenGraphs();
	}
	
	public Document getCommonMetaTags()
	{
		Document dom 	= SAXReaderUtil.createDocument();
		Element rs	= dom.addElement("rs");
		
		if(Validator.isNotNull(_metaKeywords))
		{
			for (String meta : HtmlUtil.escape(_metaKeywords).split(","))
			{
				String[] category = meta.split(StringPool.SECTION);
				String categoryId = category.length > 1 ? category[1] : null;
				String vocabularyName = null;
				String vocabularyId = null;
				if (category.length == 4)
				{
					vocabularyName = category[2];
					vocabularyId = category[3];
				}
				if (Validator.isNotNull(category[0]))
					addMetaTag(rs, META_NAME_KEYWORDS, category[0], categoryId, vocabularyName, vocabularyId);
			}
		}
		
		addMetaTag(rs, META_NAME_ROBOTS,		HtmlUtil.escape(_metaRobots),		null, null, null);
		addMetaTag(rs, META_NAME_DESCRIPTION,	HtmlUtil.escape(_metaDescription),	null, null, null);

		return dom;
	}
	
	private void addMetaTag(Element root, String metaName, String metaContent, String metaId, String vocName, String vocId)
	{
		if( Validator.isNotNull(metaContent) )
		{
			Element e = root.addElement("meta");
			e.addAttribute("name", metaName);
			e.addAttribute("content", metaContent);
			if (Validator.isNotNull(metaId))
				e.addAttribute("data-id", metaId);
			if (Validator.isNotNull(vocName))
				e.addAttribute("data-voc-name", vocName);
			if (Validator.isNotNull(vocId))
				e.addAttribute("data-voc-id", vocId);
		}
	}
}
