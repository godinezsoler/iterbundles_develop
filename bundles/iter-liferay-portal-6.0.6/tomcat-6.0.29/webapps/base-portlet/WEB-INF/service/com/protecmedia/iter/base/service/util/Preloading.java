package com.protecmedia.iter.base.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.util.PortalUtil;

public class Preloading 
{
	private static Log _log = LogFactoryUtil.getLog(Preloading.class);
	
	private static final String ITERRSRC_XPATH  = "/rs/row[@version='%s']/@iter_rsrc";
	private static final String THEMERSRC_XPATH = "/rs/row[@version='%s']/@theme_rsrc";
	
	private static String requiredVersion()
	{
		return (IterRequest.isMobileRequest() == 1L) ? "mobile" : "desktop";
	}
	
	public static boolean iterRsrc(long groupId) throws DocumentException, Exception
	{
		Document dom = SAXReaderUtil.read( getConfig(groupId) );
		String xpath = String.format(ITERRSRC_XPATH, requiredVersion());
		
		boolean canPreload = GetterUtil.getBoolean( XMLHelper.getStringValueOf(dom, xpath) );
		return canPreload;
	}
	
	public static String themeRsrcContentTypes(long groupId) throws DocumentException, Exception
	{
		Document dom = SAXReaderUtil.read( getConfig(groupId) );
		String xpath = String.format(THEMERSRC_XPATH, requiredVersion());
		
		String contentTypes = XMLHelper.getStringValueOf(dom, xpath, "");
		
		contentTypes = contentTypes.replaceAll("([^,\\s]*)(\\s*)(,)(\\s*)([^,\\s]*)", "$1'$3'$5");
		return contentTypes;
	}
	
	public static void setDefaultConfig(long groupId) throws Exception
	{
		String pathFile = String.format("%sWEB-INF%s%s", 
							PortalUtil.getPortalWebDir(),
							File.separatorChar,
							WebKeys.PRELOADING_FILE_NAME);
				
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Path to get the preload types map: ").append(pathFile));
		
		File preloadingFile = new File(pathFile);
		
		ErrorRaiser.throwIfFalse(preloadingFile != null && preloadingFile.exists() && preloadingFile.canRead(), 
						         IterErrorKeys.XYZ_E_INVALID_FILE_PATH_ZYX, 
						         new StringBuilder("File not found to get the default preloading cofiguration: ").append(pathFile).toString());		

		InputStream preloadingIS = new FileInputStream(preloadingFile);
		ErrorRaiser.throwIfNull(preloadingIS);
		
		String config = SAXReaderUtil.read(preloadingIS).asXML();
		
		_log.debug(config);
		
		setConfig(groupId, config);
	}
	
	public static void setConfig(long groupId, String preloading) throws Exception
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupId), IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(preloading), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = SAXReaderUtil.read(preloading);
		
		// Guarda la configuración
		GroupConfigTools.setGroupConfigField(groupId, GroupConfigConstants.FIELD_RESOURCES_PRELOADING, StringEscapeUtils.escapeSql(dom.asXML()));
	}
	
	public static String getConfig(long groupId) throws Exception
	{
		String config = GroupConfigTools.getGroupConfigField(groupId, GroupConfigConstants.FIELD_RESOURCES_PRELOADING);
		
		if (Validator.isNull(config))
		{
			// Se trata de un grupo nuevo, se le asigna la configuración por defecto
			setDefaultConfig(groupId);
			
			// Se lee dicha configuración
			config = GroupConfigTools.getGroupConfigField(groupId, GroupConfigConstants.FIELD_RESOURCES_PRELOADING);
		}
		
		return config;
	}
}
