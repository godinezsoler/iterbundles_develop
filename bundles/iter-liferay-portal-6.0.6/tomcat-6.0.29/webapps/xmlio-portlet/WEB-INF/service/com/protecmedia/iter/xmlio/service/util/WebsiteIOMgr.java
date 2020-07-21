package com.protecmedia.iter.xmlio.service.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterAdmin;

public class WebsiteIOMgr
{
	private static Log _logTracking = LogFactoryUtil.getLog(WebsiteIOMgr.class.getName().concat(".FileTracking"));
	
	protected static final Map<String, String[]> IMPORT_EXPORT_METHODS;
	static
	{
		 Map<String, String[]> map = new HashMap<String, String[]>();
		 map.put(IterAdmin.IA_CLASS_SMTPSERVER, 		new String[]{"com.protecmedia.iter.base.service.SMTPServerMgrServiceUtil",		  			"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_NEWSLETTER, 		new String[]{"com.protecmedia.iter.base.service.NewsletterMgrServiceUtil",		  			"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_COMMENTS, 			new String[]{"com.protecmedia.iter.base.service.CommentsConfigServiceUtil",		  			"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_USERPROFILE, 		new String[]{"com.protecmedia.iter.user.service.UserProfileLocalServiceUtil",	  			"exportData", 			"importFields"			});
		 map.put(IterAdmin.IA_CLASS_FORM, 				new String[]{"com.protecmedia.iter.base.service.IterFormLocalServiceUtil",		  			"exportData", 			"importForms"			});
		 map.put(IterAdmin.IA_CLASS_TRANSFORM,			new String[]{"com.protecmedia.iter.base.service.FormTransformLocalServiceUtil",	  			"exportData", 			"importTransforms"		});
		 map.put(IterAdmin.IA_CLASS_CAPTCHA,		    new String[]{"com.protecmedia.iter.base.service.CaptchaLocalServiceUtil",			  		"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_METADA,			  	new String[]{"com.protecmedia.iter.news.service.MetadataControlLocalServiceUtil",	  		"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_MOBILE_VERSION,	  	new String[]{"com.protecmedia.iter.base.service.IterLocalServiceUtil",				  		"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_ROBOTS,			  	new String[]{"com.protecmedia.iter.base.service.GroupConfigServiceUtil",		  			"exportRobots", 		"importRobots"			});
		 map.put(IterAdmin.IA_CLASS_METRICS,			new String[]{"com.protecmedia.iter.base.service.GroupConfigServiceUtil",					"exportMetrics",		"importMetrics"			});
		 map.put(IterAdmin.IA_CLASS_USER_REG,		  	new String[]{"com.protecmedia.iter.user.service.UserOperationsServiceUtil",	  				"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_PAYWALL,		  	new String[]{"com.protecmedia.iter.news.service.ProductServiceUtil",			  			"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_LOGIN_PORTLET,	  	new String[]{"com.protecmedia.iter.user.service.LoginLocalServiceUtil",		  				"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_SOCIAL_CONFIG,	  	new String[]{"com.protecmedia.iter.user.service.SocialMgrServiceUtil",		  				"exportData", 			"importData"			});
		 map.put(IterAdmin.IA_CLASS_SEARCH_PORTLET,	  	new String[]{"com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil",	  			"exportDataSearch",     "importDataSearch"		});
		 map.put(IterAdmin.IA_CLASS_LASTUPDT_PORTLET, 	new String[]{"com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil",	  			"exportDataLastUpdate", "importDataLastUpdate"	});
		 map.put(IterAdmin.IA_CLASS_ADS, 				new String[]{"com.protecmedia.iter.advertisement.service.AdvertisementMgrLocalServiceUtil",	"exportData",			"importData"			});
		 map.put(IterAdmin.IA_CLASS_VISITS,				new String[]{"com.protecmedia.iter.base.service.VisitsStatisticsServiceUtil",				"exportData",			"importData"			});
		 map.put(IterAdmin.IA_CLASS_BLOCKER_AD_BLOCK,	new String[]{"com.protecmedia.iter.base.service.BlockerAdBlockServiceUtil",					"exportConf",			"importConf"			});
		 map.put(IterAdmin.IA_CLASS_RSS_ADVANCED,		new String[]{"com.protecmedia.iter.base.service.RssServiceUtil",							"exportRSSAdvanced",	"setAdvancedRss"		});
		 map.put(IterAdmin.IA_CLASS_RSS_SECTIONS,		new String[]{"com.protecmedia.iter.base.service.RssServiceUtil",							"exportRSSSections",	"importRSSSections"		});
		 map.put(IterAdmin.IA_CLASS_FEEDBACK_PORTLET,	new String[]{"com.protecmedia.iter.base.service.FeedbackLocalServiceUtil",					"exportData",			"importData"			});
		 map.put(IterAdmin.IA_CLASS_EXTERNAL_SERVICE,	new String[]{"com.protecmedia.iter.news.service.ExternalServicesLocalServiceUtil",			"exportData",			"importData"			});
		 map.put(IterAdmin.IA_CLASS_URL_SHORTENER,		new String[]{"com.protecmedia.iter.base.service.URLShortenerServiceUtil",					"exportData",			"importData"			});
		 
		 IMPORT_EXPORT_METHODS = Collections.unmodifiableMap(map);
	}

	protected static final String SEL_PLID_BY_CATALOGELEMENTID = new StringBuilder(
			"SELECT plid 																				\n").append(
			"FROM CatalogPage																			\n").append(
			"INNER JOIN CatalogElement ON CatalogPage.catalogPageId = CatalogElement.catalogPageId		\n").append(
			"	WHERE catalogElementId = '%s'															\n").toString();
		
	protected static final String ELEM_OBJ   		= "obj";
	protected static final String ELEM_OBJS  		= "objects";
	protected static final String ELEM_CLASS 		= "class";
	protected static final String ELEM_ID 	 		= "id";
	protected static final String ELEM_ENV 	 		= "env";
	protected static final String ATTR_CLASS 		= StringPool.AT.concat(ELEM_CLASS);
	protected static final String ATTR_ID	 		= StringPool.AT.concat(ELEM_ID);
	protected static final String ATTR_ENV	 		= StringPool.AT.concat(ELEM_ENV);
	
	protected static final String XPATH_CLASS		= String.format("/%s/%s[%s='%%s']", ELEM_OBJS, ELEM_OBJ, ATTR_CLASS);

	/** Por omisión si no existe el environment será el BACK /objects/obj[@class='newsletter' and (not(@env) or upper-case(@env)='BACK')] **/
	protected static final String XPATH_CLASS_BACK	= String.format("/%1$s/%2$s[%3$s='%%s' and (not(%4$s) or upper-case(%4$s)='%5$s')]", 
			ELEM_OBJS, ELEM_OBJ, ATTR_CLASS, ATTR_ENV, WebKeys.ENVIRONMENT_PREVIEW);

	/** Por omisión si no existe el environment será el LIVE /objects/obj[@class='newsletter' and (not(@env) or upper-case(@env)='LIVE')] **/
	protected static final String XPATH_CLASS_LIVE	= String.format("/%1$s/%2$s[%3$s='%%s' and (not(%4$s) or upper-case(%4$s)='%5$s')]", 
														ELEM_OBJS, ELEM_OBJ, ATTR_CLASS, ATTR_ENV, WebKeys.ENVIRONMENT_LIVE);
	
	protected static final String EMPTY_OBJS= String.format("<%s/>", ELEM_OBJS);
	
	protected static final String ITERADMIN_FILENAME = "iteradmin.xml";
	
	protected static final List<String> 	SUPPORTED_CLASSES		= Arrays.asList( new String[] {	LayoutConstants.CLASS_LAYOUT, 
																									LayoutConstants.CLASS_MODEL, 
																									LayoutConstants.CLASS_CATALOG
																							  	  } );
	
	protected static final List<String> 	IA_SUPPORTED_CLASSES 	= Arrays.asList( new String[] {	IterAdmin.IA_CLASS_SMTPSERVER, 		IterAdmin.IA_CLASS_NEWSLETTER,
																									IterAdmin.IA_CLASS_COMMENTS, 		IterAdmin.IA_CLASS_FORM,
																									IterAdmin.IA_CLASS_CAPTCHA, 		IterAdmin.IA_CLASS_USERPROFILE,
																									IterAdmin.IA_CLASS_USER_REG, 		IterAdmin.IA_CLASS_SOCIAL_CFG, 
																									IterAdmin.IA_CLASS_ADS, 
																									IterAdmin.IA_CLASS_SEARCH_PORTLET,	IterAdmin.IA_CLASS_LASTUPDT_PORTLET, 
																									IterAdmin.IA_CLASS_METADA, 			IterAdmin.IA_CLASS_TRANSFORM, 
																									IterAdmin.IA_CLASS_MOBILE_VERSION,	IterAdmin.IA_CLASS_ROBOTS,
																									IterAdmin.IA_CLASS_PAYWALL,
																									IterAdmin.IA_CLASS_LOGIN_PORTLET,	IterAdmin.IA_CLASS_SOCIAL_CONFIG
		  																						  } );
	
	protected 	long 	_siteId 	= 0;
	private 	String 	_siteName 	= null;
	
	public WebsiteIOMgr(long siteId) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(siteId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		_siteId = siteId;
	}

	protected String getSiteName() throws PortalException, SystemException
	{
		if (Validator.isNull(_siteName))
		{
			_siteName = GroupLocalServiceUtil.getGroup(_siteId).getName();
		}
		return _siteName;
	}
	
	protected void track(String prefix, Document data) throws IOException
	{
		if (_logTracking.isTraceEnabled())
		{
			String fileName = String.format("%s_%d_%d.xml", prefix, _siteId, (new Date()).getTime());
			writeFile(fileName, data.asXML());
		}
	}
	private void writeFile(String fileName, String fileData) throws IOException
	{
        String pathRoot = new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath();
        String path		= new StringBuilder(pathRoot).append(File.separatorChar)
								  .append("xmlio-portlet").append(File.separatorChar)
								  .append(fileName).toString();
        
        FileWriter file = new FileWriter(path);
        file.write(fileData);
        file.close();
	}
}
