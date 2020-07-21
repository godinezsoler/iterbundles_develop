package com.protecmedia.iter.base.service.util;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.JS;
import com.liferay.portal.util.MinifyUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.NoSuchTemplateException;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.base.service.WebResourceLocalServiceUtil;

public class WebResourceUtil
{
	public final static String HEADER = "header";
	public final static String FOOTER = "footer";
		
	private final static String JS_PATH_FILE = new StringBuffer()
						.append("iter-hook").append(File.separatorChar)
						.append("WEB-INF").append(File.separatorChar)
						.append("jsps").append(File.separatorChar)
						.append("html").append(File.separatorChar)
						.append("js")
						.toString();
	
	private final static String JS_BASE_PATH_FILE = new StringBuffer()
						.append("base-portlet").append(File.separatorChar)
						.append("js")
						.toString();
	
	private final static String JS_NEWS_PATH_FILE = new StringBuffer()
						.append("news-portlet").append(File.separatorChar)
						.append("js")
						.toString();
	
	private final static String VM_PATH_FILE = new StringBuffer()
						.append("iter-hook").append(File.separatorChar)
						.append("WEB-INF").append(File.separatorChar)
						.append("jsps").append(File.separatorChar)
						.append("html").append(File.separatorChar)
						.append("vm")
						.toString();
	
	public static String getContentTypeByType(String type)
	{
		String contentType = ContentTypes.TEXT_PLAIN_UTF8;
		if(Validator.isNotNull(type))
		{
			String contentTypeStored = IterGlobal.getContentTypeByExtension(type);
			contentType = Validator.isNotNull(contentTypeStored) ? contentTypeStored : contentType;
		}
		
		return contentType;
	}
	
	public static void startGenerateJSWebResources() throws NoSuchMethodException, SecurityException, NoSuchAlgorithmException, IOException, SQLException, ServiceError, DocumentException
	{
		boolean stageMode = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(
				IterKeys.PORTAL_PROPERTIES_KEY_ITER_STAGE), IterKeys.STAGE_PRODUCTION).equalsIgnoreCase(IterKeys.STAGE_DEVELOPMENT);
		
		if(stageMode)
			WebResourceLocalServiceUtil.deleteAllVersionsAndResources();
		
		String iterWebCmsVersion = IterGlobal.getIterWebCmsVersion();
		WebResourceLocalServiceUtil.createEmptyWebResourceVersion(iterWebCmsVersion, HEADER);
		WebResourceLocalServiceUtil.createEmptyWebResourceVersion(iterWebCmsVersion, FOOTER);
		WebResourceLocalServiceUtil.generateJSWebResourcesVersion(iterWebCmsVersion, Arrays.asList(new String[]{HEADER, FOOTER}), 2);
	}
	
	public static  Map<String, String> getJSWebResourceContent() 
			throws NoSuchAlgorithmException, DocumentException, NoSuchMethodException, SecurityException, IOException, ServiceError
	{
		Map<String, String> result 	= new HashMap<String, String>();
		StringBuffer minifyJSHeader = new StringBuffer();
		StringBuffer minifyJSFooter = new StringBuffer();
		
		File rootDir = new File(PortalUtil.getPortalWebDir());
		ErrorRaiser.throwIfNull(rootDir);
		
		File webappsDir = rootDir.getParentFile();
		ErrorRaiser.throwIfNull(webappsDir);
		
		String webappsPath =  webappsDir.getAbsolutePath();
		ErrorRaiser.throwIfNull(webappsPath);
		
		//jQuery
		String jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("jquery.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//jQuery-ui
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("jquery-ui.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//autocomplete
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("autocomplete.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//cookie plugin
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("cookiePlugin.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//lazyload
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("lazyLoad.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//chosen
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("chosen.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//datepicker_language
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("datepicker_language.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
						
		//modernizr-latest
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("modernizr-latest.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));

		//swfobject
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("swf").append(File.separatorChar)
			.append("swfobject.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//iter
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("iter").append(File.separatorChar)
			.append("utils.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		// blockadblock.js
		jsPath = new StringBuffer()
		.append(webappsPath).append(File.separatorChar)
		.append(JS_PATH_FILE).append(File.separatorChar)
		.append("iter").append(File.separatorChar)
		.append("blockadblock.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
						
		//jquery.bxslider.iter.js
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("jquery.bxslider.iter.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
						
		//jquery.fancybox.iter.js
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("jquery.fancybox.iter.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//loadrespimage.js para llamar si es necesario a respimage.min.js
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("responsive").append(File.separatorChar)
			.append("loadrespimage.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//lazysizes.min.js
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("responsive").append(File.separatorChar)
			.append("lazysizes.min.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//moment.min.js
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("moment").append(File.separatorChar)
			.append("moment.min.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//		
		// jqryiter-ext.js debe ser el último fichero antes de empezar a usar jQryIter
		//
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("iter").append(File.separatorChar)
			.append("jqryiter-ext.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		//		
		// jqryiter-ext-footer.js extiende jqryiter-ext.js y debe ser el primer fichero del footer antes de empezar a usar jQryIter
		//
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("iter").append(File.separatorChar)
			.append("jqryiter-ext-footer.js").toString();
		minifyJSFooter.append(getJSContent(jsPath));
				
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("jquery").append(File.separatorChar)
			.append("widgetLazyload.js").toString();
		minifyJSFooter.append(getJSContent(jsPath));
		
		// Eventos teaserCompleteLoad y rankingCompleteLoad
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("iter").append(File.separatorChar)
			.append("portletComplete.js").toString();
		minifyJSFooter.append(getJSContent(jsPath));


		
		// ITER-580	Mostrar del número de visualizaciones de los artículos
		jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("article").append(File.separatorChar)
			.append("itr-article_visits.js").toString();
		minifyJSHeader.append(getJSContent(jsPath));
		
		// Sólo PREVIEW
		if (PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			// Overlay de estadísticas
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_BASE_PATH_FILE).append(File.separatorChar)
				.append("statistics.js").toString();
			minifyJSHeader.append(getJSContent(jsPath));
			
			// Conector con IterWebEditor
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_BASE_PATH_FILE).append(File.separatorChar)
				.append("iterwebeditor-connectors.js").toString();
			minifyJSHeader.append(getJSContent(jsPath));
		}
		// Sólo LIVE
		else
		{
			// Notificaciones Web Push
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_PATH_FILE).append(File.separatorChar)
				.append("wpn").append(File.separatorChar)
				.append("notifications.js").toString();
			minifyJSHeader.append(getJSContent(jsPath));
			
			// Tracker de visitas de MAS
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_PATH_FILE).append(File.separatorChar)
				.append("mas").append(File.separatorChar)
				.append("qapcore.js").toString();
			minifyJSFooter.append(getJSContent(jsPath));
			
			// Artículos favoritos
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_PATH_FILE).append(File.separatorChar)
				.append("article").append(File.separatorChar)
				.append("favorites.js").toString();
			minifyJSFooter.append(getJSContent(jsPath));
			
			// Sistema de recomendaciones
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_NEWS_PATH_FILE).append(File.separatorChar)
				.append("article-recommendations-portlet").append(File.separatorChar)
				.append("article-recommendations-min.js").toString();
			minifyJSFooter.append(getPreminifiedJSContent(jsPath));
			
			// Sistema de encuestas
			jsPath = new StringBuffer()
				.append(webappsPath).append(File.separatorChar)
				.append(JS_NEWS_PATH_FILE).append(File.separatorChar)
				.append("surveys").append(File.separatorChar)
				.append("iter-surveys.js").toString();
			minifyJSFooter.append(getJSContent(jsPath));
		}
		
		//
		//Vaciado de la función lazyLoadSetup, si está activado el lazyload responsive
		//
		if(PropsValues.ITER_RESPONSIVE_LAZYLOAD)
		{
			jsPath = new StringBuffer()
			.append(webappsPath).append(File.separatorChar)
			.append(JS_PATH_FILE).append(File.separatorChar)
			.append("iter").append(File.separatorChar)
			.append("deleteLazyloadSetup.js").toString();
			minifyJSHeader.append(getJSContent(jsPath));
		}
		
		Document dom  = WebResourceLocalServiceUtil.getPortletsNames();
		Map<String, Set<String>> plugin_portlets = mappingPlugins(XMLHelper.getStringValues(dom.selectNodes("/rs/row"), "@portletId"));
		List<String> jsPortletHeaderUsed = new ArrayList<String>();
		List<String> jsPortletFooterUsed = new ArrayList<String>();
		
		boolean usePortletsOwnResources = 
				GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_PORTLETS_USE_OWN_RESOURCES), false);
		
		if(!usePortletsOwnResources)
		{
			for(Entry<String, Set<String>> entry : plugin_portlets.entrySet())
			{
				String pluginDirName = null;
				String plugin = entry.getKey();
				for(String pluginName : webappsDir.list())
				{
					if(JS.getSafeName(pluginName).equals(plugin))
					{
						pluginDirName = pluginName;
						break;
					}
				}
				
				if(pluginDirName != null)
				{
					String project_path = new StringBuffer().append(webappsPath).append(File.separatorChar).append(pluginDirName).toString();
					String xml_path = new StringBuffer(project_path).append(File.separatorChar).append("WEB-INF").append(File.separatorChar).append("liferay-portlet.xml").toString();
					
					String xmlContent = FileUtil.read(xml_path);
					if(Validator.isNotNull(xmlContent))
					{
						xmlContent = xmlContent.replaceFirst("<\\?xml .*((\\r)?\\n)*<!DOCTYPE .*((\\r)?\\n)*\\.dtd\">((\\r)?\\n)*" , "").trim();
						Document domXML = SAXReaderUtil.read(xmlContent.toString());
						if(domXML != null)
						{
							Set<String> portlets = entry.getValue();
							List<Node> portletsNode =domXML.selectNodes("/liferay-portlet-app/portlet");
							jsPortletHeaderUsed.clear();
							jsPortletFooterUsed.clear();
							for (Node portletNode : portletsNode)
							{
								if(portletNode != null && portlets.contains(JS.getSafeName(portletNode.selectSingleNode("portlet-name").getText())))
								{
									List<Node> jsHeader = portletNode.selectNodes("header-portal-javascript");
									for (Node node : jsHeader)
									{
										String jsName = node.getText();
										if(!jsName.contains(IterKeys.PREFIX_PRIVATE_JS) && !jsPortletHeaderUsed.contains(jsName)){
											String fileContent = FileUtil.read(new StringBuffer(project_path).append(jsName).toString());
											if(Validator.isNotNull(fileContent))
											{
												String fileMinContent = MinifyUtil.minifyJavaScript(fileContent);
												minifyJSHeader.append(fileMinContent);
												jsPortletHeaderUsed.add(jsName);
											}
										}
									}
									
									jsHeader = portletNode.selectNodes("header-portlet-javascript");
									for (Node node : jsHeader)
									{
										String jsName = node.getText();
										if(!jsName.contains(IterKeys.PREFIX_PRIVATE_JS) && !jsPortletHeaderUsed.contains(jsName)){
											String fileContent = FileUtil.read(new StringBuffer(project_path).append(jsName).toString());
											if(Validator.isNotNull(fileContent))
											{
												String fileMinContent = MinifyUtil.minifyJavaScript(fileContent);
												minifyJSHeader.append(fileMinContent);
												jsPortletHeaderUsed.add(jsName);
											}
										}
									}
									
									List<Node> jsFooter = portletNode.selectNodes("footer-portal-javascript");
									for (Node node : jsFooter)
									{
										String jsName = node.getText();
										if(!jsName.contains(IterKeys.PREFIX_PRIVATE_JS) && !jsPortletFooterUsed.contains(jsName)){
											String fileContent = FileUtil.read(new StringBuffer(project_path).append(jsName).toString());
											if(Validator.isNotNull(fileContent))
											{
												String  fileMinContent = MinifyUtil.minifyJavaScript(fileContent);
												minifyJSFooter.append(fileMinContent);
												jsPortletFooterUsed.add(jsName);
											}
										}
									}
									
									jsFooter = portletNode.selectNodes("footer-portlet-javascript");
									for (Node node : jsFooter)
									{
										String jsName = node.getText();
										if(!jsName.contains(IterKeys.PREFIX_PRIVATE_JS) && !jsPortletFooterUsed.contains(jsName)){
											String fileContent = FileUtil.read(new StringBuffer(project_path).append(jsName).toString());
											if(Validator.isNotNull(fileContent))
											{
												String  fileMinContent = MinifyUtil.minifyJavaScript(fileContent);
												minifyJSFooter.append(fileMinContent);
												jsPortletFooterUsed.add(jsName);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		String a  = minifyJSHeader.toString();
		
		result.put(HEADER, minifyJSHeader.toString());
		result.put(FOOTER, minifyJSFooter.toString());
		return result;
	}
	
	private static Map<String, Set<String>> mappingPlugins(String[] plugin_portlets)
	{
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		for (String plugin_portlet : plugin_portlets)
		{
			String portlet = plugin_portlet.split(PortletConstants.WAR_SEPARATOR)[0];
			String plugin = plugin_portlet.split(PortletConstants.WAR_SEPARATOR)[1];
			if(result.containsKey(plugin)){
				result.get(plugin).add(portlet);
			}else{
				result.put(plugin, new HashSet<String>(Arrays.asList(new String[]{ portlet})));
			}
		}
		
		return result;
	}

	public static String getJSContent(String path) throws IOException
	{
		String fileContentJquery = FileUtil.read(path);
		if(Validator.isNotNull(fileContentJquery))
		{
			String fileMinContent = MinifyUtil.minifyJavaScript(fileContentJquery);
			return fileMinContent;
		}
		return "";
	}

	public static String getPreminifiedJSContent(String path) throws IOException
	{
		String fileContentJquery = FileUtil.read(path);
		return Validator.isNotNull(fileContentJquery) ? fileContentJquery : "";
	}
	
	public static String getMD5WebResource(String kind) throws ServiceError, NoSuchMethodException, SecurityException
	{
		String iterWebCmsVersion = IterGlobal.getIterWebCmsVersion();
		Document dom = WebResourceLocalServiceUtil.getMD5WebResoruceVersion(iterWebCmsVersion, kind);
		
		String rsrcmd5 = XMLHelper.getTextValueOf(dom, "/rs/row/@rsrcmd5");
		ErrorRaiser.throwIfNull(rsrcmd5);
		
		return rsrcmd5;
	}
	
	public static void setABTestingTemplate() throws ServiceError, IOException, PortalException, SystemException
	{
		File rootDir = new File(PortalUtil.getPortalWebDir());
		ErrorRaiser.throwIfNull(rootDir);
		
		File webappsDir = rootDir.getParentFile();
		ErrorRaiser.throwIfNull(webappsDir);
		
		String webappsPath =  webappsDir.getAbsolutePath();
		ErrorRaiser.throwIfNull(webappsPath);
		
		String path = (new StringBuilder(webappsPath))
			.append(File.separatorChar).append(VM_PATH_FILE)
			.append(File.separatorChar).append("ABTesting.vm")
			.toString();
	
		String fileContent = FileUtil.read(path);
		if(Validator.isNotNull(fileContent))
		{
			ServiceContext serviceContext = new ServiceContext();
			serviceContext.setAddCommunityPermissions(true);
			serviceContext.setAddGuestPermissions(true);
			
			// Comprueba si existe.
			try
			{
				JournalTemplateLocalServiceUtil.getTemplate(GroupMgr.getGlobalGroupId(), WebKeys.ABTESTING_TEMPLATE);
				
				// Existe. Actualiza la plantilla.
				JournalTemplateLocalServiceUtil.updateTemplate(
						GroupMgr.getGlobalGroupId(),// groupId,
						WebKeys.ABTESTING_TEMPLATE,	// templateId,
						"STANDARD-ARTICLE",			// structureId,
						"ABTesting template",		// name,
						StringPool.BLANK,			// description,
						fileContent,				// xsl,
						false,						//formatXsl, ??
						"vm",						// langType,
						true,						// cacheable,
						false,						// smallImage,
						StringPool.BLANK,			// smallImageURL,
						null,						// smallFile,
						serviceContext
				);
			}
			// No existe. Crea la plantilla.
			catch (NoSuchTemplateException nste)
			{
				JournalTemplateLocalServiceUtil.addTemplate(
					GroupMgr.getDefaultUserId(),// userId,
					GroupMgr.getGlobalGroupId(),// groupId,
					WebKeys.ABTESTING_TEMPLATE,	// templateId,
					false,						// autoTemplateId, ??
					"STANDARD-ARTICLE",			// structureId,
					"ABTesting template",		// name,
					StringPool.BLANK,			// description,
					fileContent,				// xsl,
					false,						//formatXsl, ??
					"vm",						// langType,
					true,						// cacheable,
					false,						// smallImage,
					StringPool.BLANK,			// smallImageURL,
					null,						// smallFile,
					serviceContext
				);
			}
		}
	}
}
