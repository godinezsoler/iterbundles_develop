package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.util.WebsiteIOMgr;

public class IterAdminExportMgr extends WebsiteIOMgr
{
	private static Log _log = LogFactoryUtil.getLog(IterAdminExportMgr.class);

	private AtomicBoolean 	_isAborted	= new AtomicBoolean();
	private Document 		_specDom 	= null;
	private Document 		_exportDom	= null;
	private ZipWriter 		_zipWriter	= null;
	
	public IterAdminExportMgr(long siteId) throws ServiceError
	{
		super(siteId);
	}

	public byte[] exportObjects(Document specDom) throws Exception
	{
		_log.info( String.format("Starting export from ITERAdmin of website %d", _siteId) );
		track("ExportObjects-Input", specDom);
		
		_specDom = specDom;
		byte[] data = export();
		
		_log.info( String.format("The export from ITERAdmin of website %d has finished", _siteId) );
		return data;
	}
	
	public byte[] exportAllObjects() throws Exception
	{
		_log.info( String.format("Starting export all from ITERAdmin of website %d", _siteId) );
		
		byte[] data = export();
		
		_log.info( String.format("The export all from ITERAdmin of website %d has finished", _siteId) );
		return data;
	}
	
	private byte[] export() throws Exception
	{
		_exportDom = SAXReaderUtil.read(EMPTY_OBJS);
		_zipWriter = ZipWriterFactoryUtil.getZipWriter();

		_exportSmtpServers();
		_exportNewsletters();
		_exportComments();
		_exportUserProfiles();
		_exportTransforms();
		_exportForms();
		_exportCaptcha();
		_exportSeatchPortlet();
		_exportLastUpdatePortlet();
		_exportMetadata();
		_exportMobileVersion();
		_exportRobots();
		_exportLoginPortlet();
		_exportUserReg();
		_exportPaywall();
		_exportSocialConfig();
		_exportAds();
		_exportVisits();
		_exportMetrics();
		_exportBlockerAdBlock();
		_exportRSSAdvanced();
		_exportRSSSections();
		_exportFeedbackPortlet();
		_exportExternalServices();
		_exportUrlShorteners();
		
		_exportDom.getRootElement().addAttribute(IterKeys.PREFS_ITERWEBCMS_VERSION, IterGlobal.getIterWebCmsVersion());
		_zipWriter.addEntry("iteradmin.xml", _exportDom.asXML());
		
		// Se desbloquea el zip
		File zip = _zipWriter.getUnlockedFile();
		
		// Se escribe el ZIP en el buffer de salida
		FileInputStream inputStream = new FileInputStream(zip);

		byte[] data = IOUtils.toByteArray(inputStream);
		
		inputStream.close();
		zip.setWritable(true);
		FileUtils.deleteQuietly(zip);
		
		return data;
	}
	
	private void _exportCaptcha() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_CAPTCHA);
	}
	
	private void _exportSeatchPortlet() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_SEARCH_PORTLET);
	}
	
	private void _exportLastUpdatePortlet() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_LASTUPDT_PORTLET);
	}
	
	private void _exportMetadata() throws Exception
	{
		Element metadataRoot = _exportFromBack(IterAdmin.IA_CLASS_METADA);
		
		// Se exporta el binario de la imagen (og image)
		Node ogImageNode = metadataRoot.selectSingleNode("//ogimage");
		PortalClassInvoker.invoke(false, new MethodKey("com.protecmedia.iter.news.service.MetadataControlLocalServiceUtil", "addOgImageToZIP", Long.class, ZipWriter.class, Node.class), _siteId, _zipWriter, ogImageNode);
	}
	
	private void _exportMobileVersion() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_MOBILE_VERSION);
	}
	
	private void _exportRobots() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_ROBOTS);
	}
	
	private void _exportLoginPortlet() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_LOGIN_PORTLET);
	}
	
	private void _exportUserReg() throws Exception
	{
		Element userReg = _exportFromLive(IterAdmin.IA_CLASS_USER_REG);

		if (userReg != null)
		{
			// Se detectan los servidores SMTP asociados para exportarlos también
			String[] ids = XMLHelper.getStringValues(userReg.selectNodes("//*[(local-name()='registersmptserverid' or local-name()='forgetsmptserverid') and string-length(.) > 0]"), ".");
			_exportSmtpServers(ids);
		}
	}
	
	private void _exportPaywall() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_PAYWALL);
	}
	
	private void _exportVisits() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_VISITS);
	}
	
	private void _exportMetrics() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_METRICS);
	}

	private void _exportBlockerAdBlock() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_BLOCKER_AD_BLOCK);
	}
	
	private void _exportRSSAdvanced() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_RSS_ADVANCED);
	}
	private void _exportRSSSections() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_RSS_SECTIONS);
	}
	
	private void _exportFeedbackPortlet() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_FEEDBACK_PORTLET);
	}
	
	private void _exportExternalServices() throws Exception
	{
		_exportFromBack(IterAdmin.IA_CLASS_EXTERNAL_SERVICE);
	}
	
	private void _exportUrlShorteners() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_URL_SHORTENER);
	}

	private void _exportSocialConfig() throws Exception
	{
		Element socialConf = _exportFromLive(IterAdmin.IA_CLASS_SOCIAL_CONFIG);
		if (socialConf != null)
		{
			// Se detectan los userprofiles asociados para exportarlos también
			List<Node> profiles = socialConf.selectNodes("//rs[@table='itersocialconfigfield']/row/@profilefieldid");
			String[] ids = XMLHelper.getStringValues(profiles, ".");
			
			_exportUserProfiles(ids);
		}
	}
	
	private void _exportComments() throws Exception
	{
		_exportFromLive(IterAdmin.IA_CLASS_COMMENTS);
		_exportFromBack(IterAdmin.IA_CLASS_COMMENTS);
	}
	
	private void _exportUserProfiles() throws Exception
	{
		_exportUserProfiles(null);
	}
	private void _exportUserProfiles(String[] ids) throws Exception
	{
		List<String> unexportedIds = null;
		
		if (ids != null)
		{
			// Se piden solo aquellos servidores que NO estén ya exportados
			unexportedIds = new ArrayList<String>();
			for (int i = 0; i < ids.length; i++)
			{
				long exist = XMLHelper.getLongValueOf( _exportDom, String.format("count(/objects/obj[@class='%s']/*/row[@profilefieldid='%s'])", IterAdmin.IA_CLASS_USERPROFILE, ids[i]) );
				if (exist == 0)
					unexportedIds.add(ids[i]);
			}
		}
		
		if (unexportedIds == null || unexportedIds.size() > 0)
			_exportFromBack(IterAdmin.IA_CLASS_USERPROFILE, unexportedIds);
	}
	
	
	private void _exportTransforms() throws Exception
	{
		_exportTransforms(null);
	}
	private void _exportTransforms(String[] ids) throws Exception
	{
		List<String> unexportedIds = null;
		
		if (ids != null)
		{
			unexportedIds = new ArrayList<String>();
			for (int i = 0; i < ids.length; i++)
			{
				long exist = XMLHelper.getLongValueOf( _exportDom, String.format("count(/objects/obj[@class='%s']/*/row[@transformid='%s'])", IterAdmin.IA_CLASS_TRANSFORM, ids[i]) );
				if (exist == 0)
					unexportedIds.add(ids[i]);
			}
		}
		
		if (unexportedIds == null || unexportedIds.size() > 0)
			_exportFromBack(IterAdmin.IA_CLASS_TRANSFORM, unexportedIds);
	}

	private void _exportForms() throws Exception
	{
		Element forms = _exportFromBack(IterAdmin.IA_CLASS_FORM);
		
		if (forms != null)
		{
			// Se localizan todos los UserProfiles y se exportan si no lo han hecho ya
			List<Node> nodes = forms.selectNodes("form[@formtype='registro']/tabs/row/fields/field/@profilefieldid");
			String[] ids = XMLHelper.getStringValues(nodes, ".");
			
			_exportUserProfiles(ids);
			
			nodes = forms.selectNodes("form/handlers//@transformid");
			ids = XMLHelper.getStringValues(nodes, ".");
			
			_exportTransforms(ids);
			
			nodes = forms.selectNodes("form/handlers/emailhandler//@smtpserver");
			ids = XMLHelper.getStringValues(nodes, ".");
			
			_exportSmtpServers(ids);
		}
	}
	
	private void _exportSmtpServers() throws Exception
	{
		_exportSmtpServers(null);
	}
	private void _exportSmtpServers(String[] ids) throws Exception
	{
		List<String> unexportedIds = null;
		
		if (ids != null)
		{
			// Se piden solo aquellos servidores que NO estén ya exportados
			unexportedIds = new ArrayList<String>();
			for (int i = 0; i < ids.length; i++)
			{
				long exist = XMLHelper.getLongValueOf( _exportDom, String.format("count(/objects/obj[@class='%s']/*/row[@smtpserverid='%s'])", IterAdmin.IA_CLASS_SMTPSERVER, ids[i]) );
				if (exist == 0)
					unexportedIds.add(ids[i]);
			}
		}
		
		if (unexportedIds == null || unexportedIds.size() > 0)
			_exportFromBack(IterAdmin.IA_CLASS_SMTPSERVER, unexportedIds);
	}
	
	private void _exportNewsletters() throws Exception
	{
		Element newsletter = _exportFromLive(IterAdmin.IA_CLASS_NEWSLETTER);

		if (newsletter != null)
		{
			// Se detectan los servidores SMTP asociados para exportarlos también
			List<Node> servers = newsletter.selectNodes("row//servers/row/@smtpserverid");
			String[] ids = XMLHelper.getStringValues(servers, ".");
			
			_exportSmtpServers(ids);
		}
	}
	
	private void _exportAds() throws Exception
	{
		Element adsRoot = _exportFromBack(IterAdmin.IA_CLASS_ADS);
		
		// Una vez obtenidos los datos, se exportan los binarios de las variables de contexto
		Element ctxvars = (Element)adsRoot.selectSingleNode("ctxvars");
		PortalClassInvoker.invoke(false, new MethodKey("com.protecmedia.iter.base.service.ContextVarsMgrLocalServiceUtil", "addImagesToZIP", ZipWriter.class, Node.class), _zipWriter, ctxvars);
		
		// Se exportan los binarios de la publicidad
		Element ads = (Element)adsRoot.selectSingleNode("ads");
		PortalClassInvoker.invoke(false, new MethodKey("com.protecmedia.iter.advertisement.service.AdvertisementMgrLocalServiceUtil", "addImagesToZIP", Long.class, ZipWriter.class, Node.class), _siteId, _zipWriter, ads);
	}
	
	private Element _exportFromBack(String objClass) throws Exception
	{
		return _exportFromBack(objClass, null);
	}
	private Element _exportFromBack(String objClass, List<String> ids) throws Exception
	{
		if ((ids == null || ids.size() == 0) && _specDom != null)
		{
			List<Node> objList = _specDom.selectNodes( String.format(XPATH_CLASS_BACK, objClass) );
			// Se inicializa ids si y solo si se ha referenciado a algún elemento de de la clase "objClass"
			if (objList.size() > 0)
				ids = new ArrayList<String>( Arrays.asList(XMLHelper.getStringValues(objList, ATTR_ID)) );
		}

		Element root = null;
		
		// Es una exportación global, o es una local y existen elementos de tipo objClass a exportar
		if (_specDom == null || ids != null)
		{
			String[] ieMethods 	= IMPORT_EXPORT_METHODS.get(objClass);
			ErrorRaiser.throwIfFalse(ieMethods != null && ieMethods.length == 3);
			
			Object result = null;
			if (ids == null || ids.size() == 0)
				result = PortalClassInvoker.invoke(false, new MethodKey(ieMethods[0], ieMethods[1], Long.class), _siteId);
			else
				result = PortalClassInvoker.invoke(false, new MethodKey(ieMethods[0], ieMethods[1], List.class), ids);
			
			root = ((result instanceof String) ? SAXReaderUtil.read((String)result) : ((Document) result)).getRootElement();
			
			if (root.hasAnyContent())
			{
				Node currentNode = _exportDom.getRootElement().selectSingleNode( String.format(XPATH_CLASS_BACK, objClass).concat("/*[1]") );
				if (currentNode != null)
				{
					((Element)currentNode).appendContent(root);
				}
				else
				{
					Element elemObj = _exportDom.getRootElement().addElement(ELEM_OBJ);
					elemObj.addAttribute(ELEM_CLASS, 	objClass);
					if (!objClass.equals(IterAdmin.IA_CLASS_SMTPSERVER))
					{
						elemObj.addAttribute(ELEM_ENV, 		WebKeys.ENVIRONMENT_PREVIEW);
					}
					elemObj.add(root);
				}
			}
		}

		return root;
	}
	
	private Element _exportFromLive(String objClass) throws ClientProtocolException, SystemException, SecurityException, IOException, NoSuchMethodException, DocumentException, PortalException, ServiceError
	{
		return _exportFromLive(objClass, null);
	}
	
	private Element _exportFromLive(String objClass, List<String> ids) throws ClientProtocolException, SystemException, SecurityException, IOException, NoSuchMethodException, DocumentException, PortalException, ServiceError
	{
		if ((ids == null || ids.size() == 0) && _specDom != null)
		{
			List<Node> objList = _specDom.selectNodes( String.format(XPATH_CLASS_LIVE, objClass) );
			// Se inicializa ids si y solo si se ha referenciado a algún elemento de de la clase "objClass"
			if (objList.size() > 0)
				ids = new ArrayList<String>( Arrays.asList(XMLHelper.getStringValues(objList, ATTR_ID)) );
		}

		Element root = null;
		
		// Es una exportación global, o es una local y existen elementos de tipo objClass a exportar
		if (_specDom == null || ids != null)
		{		
			String params = (ids != null && ids.size() > 0) 												? 
								String.format("<rs ids='%s'/>", StringUtil.merge((Collection<?>)ids,",") ) 	: 
								String.format("<rs groupName='%s'/>",  getSiteName());

			Document paramsDom = SAXReaderUtil.read(params);	
			paramsDom.setXMLEncoding("ISO-8859-1");
	
			String[] ieMethods = IMPORT_EXPORT_METHODS.get(objClass);
			ErrorRaiser.throwIfFalse(ieMethods != null && ieMethods.length == 3);
			
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	ieMethods[0]));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	ieMethods[1]));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[params]"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameterTypes", "[java.lang.String]"));
			remoteMethodParams.add(new BasicNameValuePair("params",				paramsDom.asXML()));
	
			JSONObject json = JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
			String returnValue = json.getString("returnValue");
			if (Validator.isNotNull(returnValue))
			{
				root = (Element)SAXReaderUtil.read(returnValue).getRootElement().detach();
				
				Node currentNode = _exportDom.getRootElement().selectSingleNode( String.format(XPATH_CLASS_LIVE, objClass).concat("/*[1]") );
				if (currentNode != null)
				{
					((Element)currentNode).appendContent(root);
				}
				else
				{
					Element elemObj = _exportDom.getRootElement().addElement(ELEM_OBJ);
					elemObj.addAttribute(ELEM_CLASS, 	objClass);
					elemObj.addAttribute(ELEM_ENV, 		WebKeys.ENVIRONMENT_LIVE);
					elemObj.add(root);
				}
			}
		}

		return root;
	}

	public void abortExport()
	{
		_isAborted.set(true);
		_log.info( String.format("The export from ITERAdmin of website %d has been aborted", _siteId) );
	}

	@Override
	protected void track(String prefix, Document data) throws IOException
	{
		super.track("IA_Exp_".concat(prefix), data);
	}
}
