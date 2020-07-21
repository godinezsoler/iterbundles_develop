package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.plugin.Version;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.FormTransformLocalServiceUtil;
import com.protecmedia.iter.base.service.SMTPServerMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.util.WebsiteIOMgr;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;

public class IterAdminImportMgr extends WebsiteIOMgr
{
	private static Log _log = LogFactoryUtil.getLog(IterAdminImportMgr.class);

	private static final String PKG_TMP_PREFFIX  = "iteradmin_";
	private static final String PKG_TMP_SUFIX 	 = ".zip";
	
	private static final String XPATH_CLASS_TRANSFORM_FROM_PROPS  	= "/rs/row[@transformname='%s']";
	private static final String XPATH_CLASS_TRANSFORM_FROM_ID 	  	= String.format(XPATH_CLASS_BACK, IterAdmin.IA_CLASS_TRANSFORM).concat("/rs/row[@transformid = '%s']");

	private static final String XPATH_CLASS_SMTPSERVER_FROM_PROPS 	= "/rs/row[@host='%s' and @port='%s' and @tls='%s' and @auth='%s' and @username='%s' and @password='%s']";
	private static final String XPATH_CLASS_SMTPSERVER_FROM_ID 	  	= String.format(XPATH_CLASS_BACK, IterAdmin.IA_CLASS_SMTPSERVER).concat("/rs/row[@smtpserverid = '%s']");
	
	private static final String XPATH_CLASS_FORM_SMTPSERVER       		= String.format(XPATH_CLASS_BACK, IterAdmin.IA_CLASS_FORM).concat("/forms/form/handlers/emailhandler/@smtpserver[string-length(.) > 0]"); 
	private static final String XPATH_CLASS_FORM_TRANSFORM        		= String.format(XPATH_CLASS_BACK, IterAdmin.IA_CLASS_FORM).concat("/forms/form/handlers//*[string-length(@transformid) > 0]/@transformid");
	private static final String XPATH_CLASS_FORM_TRANSFORM_NAME    		= String.format(XPATH_CLASS_BACK, IterAdmin.IA_CLASS_FORM).concat("/forms/form/handlers//*[string-length(@transformid) > 0]/@transformname");
	private static final String XPATH_CLASS_RSS_SECTIONS_TRANSFORM  	= String.format(XPATH_CLASS_LIVE, IterAdmin.IA_CLASS_RSS_SECTIONS).concat("//row[string-length(@xslid) > 0]/@xslid");
	private static final String XPATH_CLASS_RSS_SECTIONS_TRANSFORM_NAME = String.format(XPATH_CLASS_LIVE, IterAdmin.IA_CLASS_RSS_SECTIONS).concat("//row[string-length(@xslid) > 0]/@xslname");
	private static final String XPATH_CLASS_RSS_ADVANCED_TRANSFORM  	= String.format(XPATH_CLASS_LIVE, IterAdmin.IA_CLASS_RSS_ADVANCED).concat("/rs/row[string-length(@transformid) > 0]/@transformid");
	private static final String XPATH_CLASS_RSS_ADVANCED_TRANSFORM_NAME = String.format(XPATH_CLASS_LIVE, IterAdmin.IA_CLASS_RSS_ADVANCED).concat("/rs/row[string-length(@transformid) > 0]/@transformname");
	
	private static final String XPATH_CLASS_NEWSLETTER_SMTPSERVER 	= String.format(XPATH_CLASS_LIVE, IterAdmin.IA_CLASS_NEWSLETTER).concat("/rs/row/row/servers/row/@smtpserverid[string-length(.) > 0]");
	private static final String XPATH_CLASS_USER_REG_SMTPSERVER   	= String.format(XPATH_CLASS_LIVE, IterAdmin.IA_CLASS_USER_REG).concat("/rs/row/*[(local-name()='registersmptserverid' or local-name()='forgetsmptserverid') and string-length(.) > 0]");
	
	
	private Document 	_iterAdminDOM 		= null;
	
	private File 		_pkgDir 				= null;
	private boolean 	_updtIfExist 			= false;
	private boolean		_shouldRefreshBack 		= false;
	private boolean		_shouldRefreshLive 		= false;
	private List<Node>	_publishedSmtpServers 	= null;
	private List<Node>	_publishedTransforms	= null;
	
	public IterAdminImportMgr(long siteId, File pkg) throws Exception
	{
		super(siteId);
		
		_pkgDir = XMLIOUtil.createTempDirectory(PKG_TMP_PREFFIX);
		ZipUtil.unzip(pkg, _pkgDir);
		_iterAdminDOM = SAXReaderUtil.read( new File(_pkgDir.getPath().concat(File.separator).concat(ITERADMIN_FILENAME)) );
	}

	public Document checkObjectToImport() throws DocumentException, ServiceError
	{
		_log.info( String.format("Starting check of ITERAdmin objects: website(%d)", _siteId) );
		
		// Se comprueba la versión
		String requiredVersion 	= "3.0.0.10";
		String exportVersion 	= XMLHelper.getTextValueOf(_iterAdminDOM.getRootElement(), StringPool.AT.concat(IterKeys.PREFS_ITERWEBCMS_VERSION), "1.5.0.0");
		if (Version.compare(exportVersion, requiredVersion) < 0)
		{
			String errMsg = String.format("Package version: %s\nRequired version: %s", exportVersion, requiredVersion);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALID_ITERADMINCONFIG_EXPORT_PKG_VERSION_ZYX, errMsg);
		}
				
		Document result		= SAXReaderUtil.read(EMPTY_OBJS);
		List<Node> objects 	= _iterAdminDOM.selectNodes(String.format("/%s/%s", ELEM_OBJS, ELEM_OBJ));
		
		for (Node obj : objects)
		{
			Element newObj = result.getRootElement().addElement(ELEM_OBJ);
			newObj.addAttribute(ELEM_CLASS, XMLHelper.getStringValueOf(obj, ATTR_CLASS));
		}
		
		_log.info( String.format("Checking of ITERAdmin objects has finished: website(%d)", _siteId) );
		return result;
	}
	
	public void importObjects(boolean updtIfExist) throws Exception
	{
		_log.info( String.format("Starting import of ITERAdmin objects: website(%d) updtIfExist(%b)", _siteId, updtIfExist) );
		
		try
		{
			_updtIfExist = updtIfExist;
			_importSmtpServers();
			_importNewsletters();
			_importComments();
			_importUserProfiles();
			_importTransforms();
			_importCaptcha();
			_importForms();
			_importSeatchPortlet();
			_importLastUpdatePortlet();
			_importMetadata();
			_importMobileVersion();
			_importRobots();
			_importLoginPortlet();
			_importUserReg();
			_importPaywall();
			_importSocialConfig();
			_importAds();
			_importVisits();
			_importMetrics();
			_importBlockerAdBlock();
			_importRSSAdvanced();
			_importRSSSections();
			_importFeedbackPortlet();
			_importExternalServices();
			_importUrlShorteners();
		}
		catch (Exception e)
		{
			_deletePublishedTransforms();
			_deletePublishedSmtpServers();
			
			throw e;
		}
		
		_updatePublicationDate();
		
		_log.info( String.format("The import of ITERAdmin objects has finished: website(%d) updtIfExist(%b)", _siteId, updtIfExist) );
	}
	
	public void abortImport()
	{
		_log.info( String.format("The import of ITERAdmin objects has aborted: website(%d)", _siteId) );
	}
	
	public void finishImport()
	{
		// Se borra el directorio que se creó en el constructor
		FileUtils.deleteQuietly(_pkgDir);
	}
	
	public static File createTmpPkg() throws IOException
	{
		File tmpPkg = File.createTempFile(PKG_TMP_PREFFIX, PKG_TMP_SUFIX);
		_log.info("Creating ITERAdmin package: ".concat(tmpPkg.getPath()));
		
		return tmpPkg;
	}
	
	@Override
	protected void track(String prefix, Document data) throws IOException
	{
		super.track("IA_Imp_".concat(prefix), data);
	}
	
	private void _importSmtpServers() throws Exception
	{
		_publishedSmtpServers = null;
		Document domBack = _importInBack(IterAdmin.IA_CLASS_SMTPSERVER);
		if (domBack != null)
		{
			// Se sustituyen aquellos SMTPServers que son referenciados en otras partes de la configuración del BACK
			List<Node> serverNodes = _iterAdminDOM.selectNodes(XPATH_CLASS_FORM_SMTPSERVER);
			replaceSMTPServers(serverNodes, domBack);
			
			// Se sustituyen aquellos SMTPServers que son referenciados en otras partes de la configuración del LIVE
			serverNodes = new ArrayList<Node>();
			serverNodes.addAll(_iterAdminDOM.selectNodes(XPATH_CLASS_USER_REG_SMTPSERVER));
			serverNodes.addAll(_iterAdminDOM.selectNodes(XPATH_CLASS_NEWSLETTER_SMTPSERVER));
			Set<String> newSMTPServerIds = replaceSMTPServers(serverNodes, domBack);
			
			if (!newSMTPServerIds.isEmpty())
			{
				// Se publican aquellos servidores SMTP que se necesitarán para importar contenidos en el LIVE
				SMTPServerMgrLocalServiceUtil.publishToLive( StringUtil.merge(newSMTPServerIds) );
				_publishedSmtpServers = serverNodes;
			}
		}
	}
	
	private void _deletePublishedSmtpServers()
	{
		if (Validator.isNotNull(_publishedSmtpServers))
		{
			try
			{
				Document dom = SAXReaderUtil.read("<rs checkReferences='false'/>");
				Element root = dom.getRootElement();
				for (Node node : _publishedSmtpServers)
				{
					root.addElement("row").add( node.detach() );
				}
				
				PortalClassInvoker.invoke(false, new MethodKey("com.protecmedia.iter.base.service.SMTPServerMgrServiceUtil", "deleteServers", String.class), dom.asXML());
			}
			catch (Exception e)
			{
				_log.error(e);
			}
			
		}
	}
	
	private void _deletePublishedTransforms()
	{
		if (Validator.isNotNull(_publishedTransforms))
		{
			try
			{
				Document dom = SAXReaderUtil.read("<rs/>");
				Element root = dom.getRootElement();
				for (Node node : _publishedSmtpServers)
				{
					root.addElement("row").add( node.detach() );
				}
				
				PortalClassInvoker.invoke(false, new MethodKey("com.protecmedia.iter.base.service.FormTransformLocalServiceUtil", "deleteTransform", String.class), dom.asXML());
			}
			catch (Exception e)
			{
				_log.error(e);
			}
		}
	}
	
	private Set<String> replaceSMTPServers(List<Node> serverNodes, Document domServers) throws ServiceError
	{
		Set<String> newSMTPServerIds = new HashSet<String>();
		
		for (int i = 0; i < serverNodes.size(); i++)
		{
			// Se localiza el SMTPServer, que según el origen, es necesario importar
			String oldSMTPServerId = serverNodes.get(i).getStringValue();
			Element smtpServer = (Element)_iterAdminDOM.selectSingleNode( String.format(XPATH_CLASS_SMTPSERVER_FROM_ID, oldSMTPServerId) );
			
			ErrorRaiser.throwIfNull(smtpServer, IterErrorKeys.XYZ_E_ITERADMIN_IMPORT_REF_NON_FOUND_ELEMENT_ZYX,
				String.format("%s(%s)", IterAdmin.IA_CLASS_SMTPSERVER, oldSMTPServerId));
			
			// Se busca el nuevo SMTPServerId del SMTPServer recién importado
			// /rs/row[@host='%s' and @port='%s' and @tls='%s' and @auth='%s' and @username='%s' and @password='%s']/@smtpserverid
			String newSMTPServerId = ((Element)domServers.selectSingleNode(String.format(XPATH_CLASS_SMTPSERVER_FROM_PROPS, 
									  smtpServer.attributeValue("host"), 	smtpServer.attributeValue("port"),
									  smtpServer.attributeValue("tls"), 	smtpServer.attributeValue("auth"),
									  smtpServer.attributeValue("username"),smtpServer.attributeValue("password")))).attributeValue("smtpserverid");
			
			serverNodes.get(i).setText(newSMTPServerId);
			newSMTPServerIds.add(newSMTPServerId);
		}
		
		return newSMTPServerIds;
	}
	
	private void _importComments() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_COMMENTS);
		_importInBack(IterAdmin.IA_CLASS_COMMENTS);
	}
	
	private void _importUserProfiles() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_USERPROFILE);
	}
	
	private void _importTransforms() throws Exception
	{
		_publishedTransforms = null;
		Document domBack = _importInBack(IterAdmin.IA_CLASS_TRANSFORM);
		if (domBack != null)
		{
			// Se sustituyen aquellas transformaciones que son referenciadas en otras partes de la configuración del BACK
			List<Node> idList   = _iterAdminDOM.selectNodes(XPATH_CLASS_FORM_TRANSFORM);
			List<Node> nameList = _iterAdminDOM.selectNodes(XPATH_CLASS_FORM_TRANSFORM_NAME);
			replaceTransforms(idList, nameList, domBack);
			
			// Se sustituyen aquellas transformaciones que son referenciadas en otras partes de la configuración del LIVE
			idList = new ArrayList<Node>();
			idList.addAll(_iterAdminDOM.selectNodes(XPATH_CLASS_RSS_SECTIONS_TRANSFORM));
			idList.addAll(_iterAdminDOM.selectNodes(XPATH_CLASS_RSS_ADVANCED_TRANSFORM));
			
			nameList = new ArrayList<Node>();
			nameList.addAll(_iterAdminDOM.selectNodes(XPATH_CLASS_RSS_SECTIONS_TRANSFORM_NAME));
			nameList.addAll(_iterAdminDOM.selectNodes(XPATH_CLASS_RSS_ADVANCED_TRANSFORM_NAME));
			
			Set<String> newTransformIds = replaceTransforms(idList, nameList, domBack);
			
			if (!newTransformIds.isEmpty())
			{
				// Se publican aquellas transformaciones que se necesitarán para importar contenidos en el LIVE
				FormTransformLocalServiceUtil.publishToLive( StringUtil.merge(newTransformIds) );
				_publishedTransforms = idList;
			}
		}
	}
	
	private Set<String> replaceTransforms(List<Node> oldNodes, List<Node> nameList, Document domNewNodes) throws ServiceError
	{
		Set<String> newIds = new HashSet<String>();
		
		for (int i = 0; i < oldNodes.size(); i++)
		{
			String oldId = oldNodes.get(i).getStringValue();
			Element transform = (Element)_iterAdminDOM.selectSingleNode( String.format(XPATH_CLASS_TRANSFORM_FROM_ID, oldId) );
			
			if (transform == null)
			{
				String transformName = GetterUtil.getString(nameList.get(i).getStringValue(), "");
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ITERADMIN_IMPORT_REF_NON_FOUND_ELEMENT_ZYX, 
						String.format("%s(%s-%s)", IterAdmin.IA_CLASS_TRANSFORM, transformName, oldId));
			}

			// Se busca el transformid de la transformación recién importada
			String newTransformId = ((Element)domNewNodes.selectSingleNode(String.format(XPATH_CLASS_TRANSFORM_FROM_PROPS, transform.attributeValue("transformname"))))
					.attributeValue("transformid");
			
			oldNodes.get(i).setText(newTransformId);
			newIds.add(newTransformId);
		}
		
		return newIds;
	}
	
	private void _importForms() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_FORM);
	}
	
	private void _importNewsletters() throws PortalException, SystemException, SecurityException, UnsupportedEncodingException, ClientProtocolException, NoSuchMethodException, ServiceError, IOException
	{
		_importInLive(IterAdmin.IA_CLASS_NEWSLETTER);
	}
	
	private void _importCaptcha() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_CAPTCHA);
	}
	
	private void _importSeatchPortlet() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_SEARCH_PORTLET);
	}
	
	private void _importLastUpdatePortlet() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_LASTUPDT_PORTLET);
	}
	
	private void _importMetadata() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_METADA);
	}
	
	private void _importMobileVersion() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_MOBILE_VERSION);
	}
	
	private void _importRobots() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_ROBOTS);
	}
	
	private void _importLoginPortlet() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_LOGIN_PORTLET);
	}
	
	private void _importUserReg() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_USER_REG);
	}
	
	private void _importPaywall() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_PAYWALL);
	}
	
	private void _importVisits() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_VISITS);
	}
	
	private void _importMetrics() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_METRICS);
	}

	private void _importBlockerAdBlock() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_BLOCKER_AD_BLOCK);
	}
	
	private void _importRSSAdvanced() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_RSS_ADVANCED);
	}

	private void _importRSSSections() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_RSS_SECTIONS);
	}

	private void _importFeedbackPortlet() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_FEEDBACK_PORTLET);
	}
	
	private void _importSocialConfig() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_SOCIAL_CONFIG);
	}
	
	private void _importAds() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_ADS);
	}

	private void _importExternalServices() throws Exception
	{
		_importInBack(IterAdmin.IA_CLASS_EXTERNAL_SERVICE);
	}

	private void _importUrlShorteners() throws Exception
	{
		_importInLive(IterAdmin.IA_CLASS_URL_SHORTENER);
	}
	
	private Document _importInBack(String objClass) throws Exception
	{
		Document result = null;
		Node data = _iterAdminDOM.selectSingleNode(String.format(XPATH_CLASS_BACK, objClass).concat("/*[1]"));
		if (data != null && data.hasAnyContent())
		{
			Document domToSend = SAXReaderUtil.createDocument();
			domToSend.add( ((Element)data).createCopy() );
			domToSend.setXMLEncoding("ISO-8859-1");
			Element root = domToSend.getRootElement();
			
			root.addAttribute("groupId", 		String.valueOf(_siteId));
			root.addAttribute("updtIfExist", 	String.valueOf(_updtIfExist));
			root.addAttribute("importProcess", 	String.valueOf(true));
			
			if ( objClass.equals(IterAdmin.IA_CLASS_ADS) || objClass.equals(IterAdmin.IA_CLASS_METADA) )
			{
				root.addAttribute("filesPath", _pkgDir.getPath());
			}

			String[] ieMethods = IMPORT_EXPORT_METHODS.get(objClass);
			ErrorRaiser.throwIfFalse(ieMethods != null && ieMethods.length == 3);
	
			Object returnValue = PortalClassInvoker.invoke(false, new MethodKey(ieMethods[0], ieMethods[2], String.class), domToSend.asXML());
			if (returnValue != null)
				result = (returnValue instanceof Document) ? (Document)returnValue : SAXReaderUtil.read(returnValue.toString());
			
			_shouldRefreshBack = true;
		}
		
		return result;
	}
	
	private String _importInLive(String objClass) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException
	{
		String result = null;
		Node data = _iterAdminDOM.selectSingleNode(String.format(XPATH_CLASS_LIVE, objClass).concat("/*[1]"));
		if (data != null && data.hasAnyContent())
		{
			Document domToSend = SAXReaderUtil.createDocument();
			domToSend.add( ((Element)data).createCopy() );
			domToSend.setXMLEncoding("ISO-8859-1");
			Element root = domToSend.getRootElement();
			
			root.addAttribute("groupName", 		getSiteName());
			root.addAttribute("updtIfExist", 	String.valueOf(_updtIfExist));
			root.addAttribute("importProcess", 	String.valueOf(true));
	
			String[] ieMethods = IMPORT_EXPORT_METHODS.get(objClass);
			ErrorRaiser.throwIfFalse(ieMethods != null && ieMethods.length == 3);
	
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	ieMethods[0]));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	ieMethods[2]));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[data]"));
			remoteMethodParams.add(new BasicNameValuePair("data", 				domToSend.asXML()));
			
			result = JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams).getString("returnValue");
			
			_shouldRefreshLive = true;
		}
		
		return result;
	}

	private void _updatePublicationDate() throws IOException, SQLException, ServiceError
	{
		if (_shouldRefreshBack)
			GroupMgr.updatePublicationDate(_siteId, new Date() );
		
		// NO se refresca la fecha de última publicación en el LIVE ya que se refrescará en la siguiente publicación.
		// La importación es un proceso sensible que NO se aconseja en plena producción, y tras la cuál se debería realizar una publicación,
		// momento en el cuál se actualizará dicha fecha
		 if (_shouldRefreshLive)
			 _log.trace("shouldRefreshLive");
	}
}
