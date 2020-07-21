package com.protecmedia.iter.news.util;

import java.net.MalformedURLException;
import java.util.List;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IMetadataControlUtil;
import com.liferay.portal.kernel.util.MetadataControlUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.theme.ThemeDisplay;
import com.protecmedia.iter.news.service.CategorizeLocalServiceUtil;


public class CategoryMetadataControlUtil extends MetadataControlUtil implements IMetadataControlUtil
{
	private static Log _log = LogFactoryUtil.getLog(CategoryMetadataControlUtil.class);
	
	private String _categoryIds = "";
	
	///////////////////////////////////////////////////////////////////////////////
	public CategoryMetadataControlUtil(ThemeDisplay themeDisplay, List<String> categoryIds) throws DocumentException, SystemException, PortalException, MalformedURLException
	{
		super(themeDisplay);

		// Se obtienen los nombres separados por comas
		if (categoryIds.size() == 1)
			_categoryIds = categoryIds.get(0);
		else if (categoryIds.size() > 1)
		{
			StringBuffer sb = new StringBuffer();
			for (String categoryId : categoryIds)
			{
				sb.append(categoryId).append(",");
			}
			_categoryIds = sb.substring(0, sb.length()-1);
		}
	}
	
	/**
	 * @return Los nombres de todas las categorías indicadas en la URL separadas por comas
	 * @see com.protecmedia.iter.news.util.MetadataControlUtil#getPageTitle()
	 */
	@Override
	public String getPageTitle()
	{
		if (_metaTitle == null)
			_metaTitle = CategorizeLocalServiceUtil.getCategoryNames(_categoryIds);;
		
		return _metaTitle;
	}

	/**
	 * La descripción concidirá con el título
	 * @see com.protecmedia.iter.news.util.MetadataControlUtil#getPageDescription()
	 */
	@Override
	public String getPageDescription()
	{
		return getPageTitle();
	}
	
	/**
	 * @return Los nombres de los ancestros sin repetición de las categorías indicadas en la URL.
	 * @see com.protecmedia.iter.news.util.MetadataControlUtil#getPageKeywords()
	 */
	@Override
	public String getPageKeywords()
	{
		return CategorizeLocalServiceUtil.getCategoryAncestorNames(_categoryIds);
	}
	
	/**
	 * @return Imagen configurada por defecto para el website 
	 * @throws MalformedURLException 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @see com.protecmedia.iter.news.util.MetadataControlUtil#getPageImage()
	 */
	@Override
	public String getPageImage() throws PortalException, SystemException, MalformedURLException
	{
		return getDefaultPageImage();
	}
	
	/**
	 * @throws MalformedURLException 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ServiceError 
	 * @see com.protecmedia.iter.news.util.MetadataControlUtil#getPageOpenGraphs()
	 */
	public Document getPageOpenGraphs() throws PortalException, SystemException, MalformedURLException, ServiceError
	{
		return super.getPageOpenGraphs();
	}
}
