/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.xmlio.service.impl;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.JournalIndexerMgr;
import com.liferay.portal.kernel.search.JournalIndexerUtil;
import com.liferay.portal.kernel.util.ABTestingMgr;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutTemplateLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.UserServiceUtil;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalTemplateConstants;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.base.service.GroupConfigLocalServiceUtil;
import com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.ChannelExportException;
import com.protecmedia.iter.xmlio.model.Channel;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.ChannelLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.LayoutXmlIO;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.CacheRefreshMgr;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.PingGoogle;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.util.RefreshCacheThread;


/**
 * The implementation of the channel local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.ChannelLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil} to access the channel local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author Protecmedia
 * @see com.protecmedia.iter.xmlio.service.base.ChannelLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ChannelLocalServiceImpl extends ChannelLocalServiceBaseImpl 
{
	
	private static Log _log = LogFactoryUtil.getLog(ChannelLocalServiceImpl.class);
	
	static private final String XPATH_TYPCONALTERNATIVE = String.format("//item[@classname='%s' and @reloadTypConAlternative][1]/@reloadTypConAlternative", IterKeys.CLASSNAME_JOURNALARTICLE);
	
	public List<Channel> getChannelsByGroupId(long groupId) throws SystemException {
		return channelPersistence.findByGroupId(groupId);
	}
	
	public List<Channel> getChannelsByGroupId(long groupId, int start, int end) throws SystemException {
		return channelPersistence.findByGroupId(groupId, start, end);
	}
	
	public List<Channel> getAllChannels() throws SystemException{
		return channelPersistence.findAll();
	}
	
	public List<Channel> getChannelsByStatus(boolean status) throws SystemException{
		return channelPersistence.findByStatus(status);
	}
	
	public int getChannelsCountByGroupId(long groupId) throws SystemException {
		return channelPersistence.countByGroupId(groupId);
	}
	
	/*
	 * Métodos generales 
	 */
	
	/* INPUT */
	public String importToLive(long companyId, long userId, long groupId, String fileName) throws Exception
	{
		String importLog 	 = "";
		
		// Se comprueban los parámetros de entrada de la importación
		checkImportParams(companyId, userId, groupId);

		LiveConfiguration lc = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
		
		//Si la configuración local es FTP
		if (lc.getOutputMethod().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP))
		{
			String ftpServer = lc.getFtpPath();
			String ftpUser = lc.getFtpUser();
			String ftpPassword = lc.getFtpPassword();
			String localPath = lc.getLocalPath();
			
			importLog = importContentFTP(ftpServer, ftpUser, ftpPassword, fileName, localPath, null, companyId, userId, groupId, true);
		} 
		else
		{
			String remotePath = lc.getRemotePath();
			String filePath = remotePath + "/" + fileName;	

			// No se quieren mantener los ficheros de entrada del Back
			importLog = importContent(filePath, companyId, userId, groupId, false, true);
		}
		
		return importLog; 
	}
	
	public Document importWebThemesToLive(String scopeGroupName, String fileName) throws Exception
	{
		File sourceFile = XMLIOUtil.getFile(fileName);
		
		Document infoDom = SAXReaderUtil.read(sourceFile);
		
		Node journalTemplates = infoDom.getRootElement().selectSingleNode(JournalTemplateConstants.RS_TAG_NAME);
		if (journalTemplates != null)
		{
			// Si existe nodo de JournalTemplates se importan, y se eliminan del DOM principal para que no interfiera con la importación del tema
			JournalTemplateLocalServiceUtil.importTemplates( (Element)journalTemplates );
			journalTemplates.detach();
		}
		
		ThemeWebResourcesLocalServiceUtil.deliverTheme( infoDom );
		
		Document dom = SAXReaderUtil.read("<rs/>");
		long groupId = GroupLocalServiceUtil.getGroup(IterGlobal.getCompanyId(), scopeGroupName).getGroupId();
		
		String lastUpdate = String.valueOf(GroupMgr.getPublicationDate(groupId).getTime());
		dom.getRootElement().addAttribute(IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE, lastUpdate);
		
		TomcatUtil.updatePublicationDateNoException(IterGlobal.getCompanyId(), groupId);
		return dom;
	}
	
	/**
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void importJournalTemplatesToLive(String fileName) throws Exception
	{
		Document infoDom = SAXReaderUtil.read( XMLIOUtil.getFile(fileName) );
		
		JournalTemplateLocalServiceUtil.importTemplates(infoDom.getRootElement());
		
		TomcatUtil.updatePublicationDateNoException(IterGlobal.getCompanyId(), GroupMgr.getGlobalGroupId());
	}
	
	/**
	 * 
	 */
	public void importLayoutTemplatesToLive(String fileName) throws Exception
	{
		Document infoDom = SAXReaderUtil.read( XMLIOUtil.getFile(fileName) );
		
		LayoutTemplateLocalServiceUtil.importTemplates(infoDom.getRootElement());
		
		TomcatUtil.updatePublicationDateNoException(IterGlobal.getCompanyId(), GroupMgr.getGlobalGroupId());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String importContentFTP(
			String ftpServer, String ftpUser, String ftpPassword, String localPath,
			long companyId, long userId, long groupId) throws Exception
	{
		return importContentFTP(ftpServer, ftpUser, ftpPassword, null, localPath, null, companyId, userId, groupId, false);
	}
	public String importContentFTP(
			String ftpServer, String ftpUser, String ftpPassword, String remoteFileName, String localPath,
			String xslPath, long companyId, long userId, long groupId, boolean enableTomcatCache) throws Exception
	{
		String filePath = FTPUtil.receiveFile(ftpServer, ftpUser, ftpPassword, remoteFileName, localPath, IterKeys.XMLIO_VALID_INPUT_FILE_PATTERN);

		// No se quieren mantener los ficheros de entrada por FTP
		return importContent(filePath, companyId, userId, groupId, false, enableTomcatCache);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	public String importContent(String filePath, long companyId, long userId, long groupId, boolean keepFile, boolean enableTomcatCache) throws Exception
	{
		return importContent(filePath, "", companyId, userId, groupId, keepFile, enableTomcatCache);
	}
	public String importContent(String filePath, String xslPath, long companyId, long userId, long groupId, boolean keepFile, boolean enableTomcatCache) throws Exception
	{
		File sourceFile = new File(filePath);
		File xslFile 	= null;
		
		if (!xslPath.equals("")) 
			xslFile = new File(xslPath);
		
		return importContent(sourceFile, xslFile, companyId, userId, groupId, keepFile, enableTomcatCache);
	}
	public String importContent(File sourceFile, long companyId, long userId, long groupId, boolean keepFile) throws Exception
	{
		return importContent(sourceFile, null, companyId, userId, groupId, keepFile, false);
	}
		
	public String importContent(File sourceFile, File xslFile, long companyId, long userId, long groupId, boolean keepFile, boolean enableTomcatCache) throws Exception
	{
		String result = "";
		boolean hasChanged = false; //bandera para la cache de los tomcats
		boolean articlesImported = false;
		
		// Se comprueban los parámetros de entrada de la importación. En una importación manual o automática no entraría por el importToLive así que se comprueba aquí.
		checkImportParams(sourceFile, companyId, userId, groupId);
		
		XMLIOContext xmlIOContext = new XMLIOContext();
		xmlIOContext.setCompanyId(companyId);
		xmlIOContext.setUserId(userId);
		
		Document groups2UpdateDom = null;
		Document doc = null;
	    try 
	    {	
	    	XMLIOImport.init(sourceFile, xslFile);
	    	File iterXmlFile = new File(XMLIOImport.getMainFile());
	    	
		    doc = SAXReaderUtil.read(iterXmlFile);			    			    
		    String path = "";
		    
		    String scopeGroupIdXML = doc.getRootElement().attributeValue(IterKeys.XMLIO_XML_SCOPEGROUPID_ATTRIBUTE);
		    ErrorRaiser.throwIfFalse(Validator.isNotNull(scopeGroupIdXML), "No scopegroupid found in XML");
		    
		    List<Live> scopeGroupLive = LiveLocalServiceUtil.getLiveByGlobalId(IterKeys.CLASSNAME_GROUP, scopeGroupIdXML);
		    if(scopeGroupLive != null && scopeGroupLive.size() > 0)
		    {
			    long scopeGroupId = Long.parseLong(scopeGroupLive.get(0).getLocalId());
			    xmlIOContext.setGroupId(scopeGroupId);
			    _log.trace("Current scope group: " + scopeGroupIdXML);
		    }
		    else
		    {
		    	_log.trace("Current scope group not found: " + scopeGroupIdXML);
		    }
		    
		    // En caso de ser una importación se comprueba el tamaño para el recorte de las imagenes JPG
		    // En las importaciones el valor de enableTomcatCache es siempre FALSE
		    if( !enableTomcatCache )
		    {
			    String value = null;
			    int imgMaxWidth	= 0;
				int imgMaxHeight= 0;
				
				// Se comprueba si viene especificado en el iter.xml
				value = doc.getRootElement().attributeValue("maxwidth");
				if(value!=null && !value.isEmpty())
					imgMaxWidth	= Integer.valueOf( value ).intValue();
				
				value = doc.getRootElement().attributeValue("maxheight");
				if(value!=null && !value.isEmpty())
					imgMaxHeight = Integer.valueOf( value ).intValue();
			    
				if ( imgMaxHeight==0 && imgMaxWidth==0 )
				{
					//En caso de que no este en el iter.xml se busca en el portal-ext.properties
					imgMaxWidth	= GetterUtil.getInteger( PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_IMGIMPORT_MAXWIDTH) );
					imgMaxHeight = GetterUtil.getInteger( PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_IMGIMPORT_MAXHEIGHT) );
					
					if ( imgMaxHeight==0 && imgMaxWidth==0 )
					{
						//Si no se ha especificado tamaño por defecto se emplea 600x400, y se añade como atributo en la raiz del iter.xml
						doc.getRootElement().addAttribute( "maxwidth", "600" );
						doc.getRootElement().addAttribute( "maxheight", "400" );
					}
					else
					{
						doc.getRootElement().addAttribute( "maxwidth", String.valueOf(imgMaxWidth) );
						doc.getRootElement().addAttribute( "maxheight", String.valueOf(imgMaxHeight) );
					}
				}
		    }
			
		    _log.info("Executing delete operations...");
		    long operationsTime = System.currentTimeMillis();
		    
		    String sType;
		    for (int i = IterKeys.DELETE_CLASSNAME_TYPES.length - 1; i >= 0 ; i--) 
		    {
		    	sType =  IterKeys.DELETE_CLASSNAME_TYPES[i];
		    	
		    	 _log.debug("Executing delete operations for type " + sType + "...");
		    	
		    	path = "//item[@classname='" + sType + "' and @operation='" + IterKeys.DELETE + "']";

				XPath xpathSelector2 = SAXReaderUtil.createXPath(path);
				List<Node> nodes2 = xpathSelector2.selectNodes(doc);
				
				for (Node node2 : nodes2) 
				{
					Element item2 = (Element) node2;
					
					ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(sType, xmlIOContext);
					ErrorRaiser.throwIfNull(itemXmlIO);
					
					hasChanged = true;				
					itemXmlIO.importContents(item2, doc);
				}
		    }
		    
		    _log.info("Executing create/update operations...");
		    List<Node> layoutNodes = null;
		   for (String s : IterKeys.MAIN_CLASSNAME_TYPES_IMPORT) 
		   {
				_log.info("Executing create/update operations for type " + s + "...");
				long singleOperationTime = System.currentTimeMillis();	    	 
				 
				path = "//item[@classname='" + s + "' and (@operation='" + IterKeys.CREATE + "' or @operation='" + IterKeys.UPDATE + "')]";
				
				XPath xpathSelector = SAXReaderUtil.createXPath(path);
				List<Node> nodes 		= xpathSelector.selectNodes(doc);
				
				String defaultPgTmplId_ = "";
				String defaultMobilePgTmplId_ = "";
				
				if (s.equals(IterKeys.CLASSNAME_PAGETEMPLATE) && nodes.size() > 0)
				{
					Class<?> clazz = Class.forName("com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil");
					
					Object[] methodParams = new Object[2];
					methodParams[0] = xmlIOContext.getGroupId();
					methodParams[1] = IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE;
					
					Method method = clazz.getMethod("getDefaultPageTemplate", long.class, java.lang.String.class);
					BaseModel<?> pageTmplModel = (BaseModel<?>) method.invoke(clazz, methodParams);
					if( pageTmplModel!=null )
						defaultPgTmplId_ = pageTmplModel.getPrimaryKeyObj().toString();
					
					method = clazz.getMethod("getDefaultMobilePageTemplate", long.class, java.lang.String.class);
					pageTmplModel = (BaseModel<?>) method.invoke(clazz, methodParams);
					if( pageTmplModel!=null )
						defaultMobilePgTmplId_ = pageTmplModel.getPrimaryKeyObj().toString();
				}
				else if (s.equals(IterKeys.CLASSNAME_LAYOUT) && nodes.size() > 0)
				{
					layoutNodes = nodes;
					
					// Antes de importar los Layouts se importaran los TPLs que necesiten
					String tplFileName = XMLHelper.getStringValueOf(doc.getRootElement(), "@tpls");
					if (Validator.isNotNull(tplFileName))
					{
						importLayoutTemplatesToLive(tplFileName);
						
						// Se refresca el mapa de memoria de TPLs en el tomcat de la publicación, el resto se refrescarán en el RefreshCache
						LayoutTemplateLocalServiceUtil.loadLayoutTemplatesNonClustered();
					}
				}
				
				int numOpUpdate = 0;
				int numOpCreate = 0;
				
				for (Node node : nodes) 
				{
					Element item = (Element) node;
					ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(s, xmlIOContext);
					ErrorRaiser.throwIfNull(itemXmlIO);
					
					hasChanged = true;	
					itemXmlIO.importContents(item, doc);
				
					if(node.valueOf("@operation").equals(IterKeys.CREATE))
						numOpCreate++;
					else
						numOpUpdate++;
				}
				
				if (s.equals(IterKeys.CLASSNAME_LAYOUT))
				{
					String xpath = String.format("count(//item[@classname='%s' and (@operation='%s' or @operation='%s')])", 
									IterKeys.CLASSNAME_PAGETEMPLATE, IterKeys.CREATE, IterKeys.UPDATE);
					
					if (XMLHelper.getLongValueOf(doc, xpath) == 0)
					{
						// NO xisten plantillas a importar, se insertan las propiedades de los layouts en este momento
						importLayoutProperties(xmlIOContext, doc, layoutNodes);
					}
				}
				else if (s.equals(IterKeys.CLASSNAME_PAGETEMPLATE) && nodes.size() > 0)
				{
					// Antes de comprobar  las secciones por defecto se insertan las propiedades de los layouts
					importLayoutProperties(xmlIOContext, doc, layoutNodes);
					
					if (xmlIOContext.isPublishArticleTemplate())
						checkDefaultPageTemplates(xmlIOContext.getGroupId(), defaultPgTmplId_, defaultMobilePgTmplId_);
				}
				
				if (!PropsValues.ITER_ASSET_CATEGORIES_REBUILDTREE_CONTINUALLY && s.equals(IterKeys.CLASSNAME_VOCABULARY) && nodes.size() > 0)
				{
					// Tras importar los vocabularios, y por ende los AssetCategories, se reconstruye el árbol de categorías
					// 0009493: Borrado masivo de metadatos genera transacción de millones de registros
					AssetCategoryLocalServiceUtil.rebuildTree(GroupMgr.getGlobalGroupId(), true);
				}
				
				if( PropsValues.ITER_PINGGOOGLESITEMAP_ENABLED && s.equals(IterKeys.CLASSNAME_JOURNALARTICLE) && nodes.size() > 0 )
					articlesImported = true;
				
				singleOperationTime = System.currentTimeMillis() - singleOperationTime;			
				long relativeTime = (numOpCreate + numOpUpdate == 0 ? 0 : (singleOperationTime / (numOpCreate + numOpUpdate)));
				_log.info("Operation " + s + " complete - Create op: " + numOpCreate + ", Update op: " + numOpUpdate + ", total time: " + XMLIOUtil.toHMS(singleOperationTime) + ", relative time: " + XMLIOUtil.toHMS(relativeTime));
		    }	
			
			//Reindexamos todos los articulos por si han cambiado algunas de sus dependencias
			reindexArticlesFromXML(doc);
			
			// Si es necesario, se insertan los tipos de contenidos alternativos del ABTesting
			insertTypConAlternative(doc);
			
		   
		   	operationsTime = System.currentTimeMillis() - operationsTime;
		   	_log.info("Operations time " + XMLIOUtil.toHMS(operationsTime));
		   
		   	_log.info("Cleaning up resources...");
		    XMLIOImport.release(keepFile);
		    
		    groups2UpdateDom = CacheRefresh.getGroups2Update(xmlIOContext.getGroupId(), xmlIOContext.itemLog.toXML());
		    List<Node> groups2Update  = groups2UpdateDom.selectNodes("/groups/group");
		    
		    if (enableTomcatCache && hasChanged && xmlIOContext.getGroupId() != GroupMgr.getGlobalGroupId())
		    {
		    	for (Node group2Update : groups2Update)
		    	{
		    		long group2UpdateId = XMLHelper.getLongValueOf(group2Update, "@groupId");
		    		((Element)group2Update).addAttribute("lastUpdate", String.valueOf(GroupMgr.getPublicationDate(group2UpdateId).getTime()));
		    		
		    		TomcatUtil.updatePublicationDateNoException(xmlIOContext.getCompanyId(), group2UpdateId);
		    	}
		    }
		    
		    if ( articlesImported && xmlIOContext.getGroupId() != GroupMgr.getGlobalGroupId())
		    {
		    	for (Node group2Update : groups2Update)
		    	{
		    		long group2UpdateId = XMLHelper.getLongValueOf(group2Update, "@groupId");
		    		pingGoogleSitemap( group2UpdateId );
		    	}
		    }
		    
		    result = xmlIOContext.itemLog.getXMLLogs();
		    _log.info("Import process finished");
		    _log.info(result);
	    } 
	    catch (Exception e) 
	    { 
	    	_log.error("ImportContent Error: " + e.toString());
	    	
	    	_log.info("Renaming ZIP file and cleaning up temp resources...");
	    	XMLIOImport.rename();
	    	
	    	throw e;
	    } 
	    finally
	    {
	    	if (Validator.isNotNull(result))
	    	{
	    		if (groups2UpdateDom == null)
	    			groups2UpdateDom = CacheRefresh.getGroups2Update(xmlIOContext.getGroupId(), xmlIOContext.itemLog.toXML());
	    		
	    		Element docRoot = SAXReaderUtil.read(result).getRootElement();
	    		docRoot.add( groups2UpdateDom.getRootElement().detach() );

	    		// Se copian todos los atributos a la respuesta
	    		List<Node> attrList = doc.getRootElement().selectNodes("@*");
	    		for (int i = 0; i < attrList.size(); i++)
	    			docRoot.add( attrList.get(i).detach() );
	    		
	    		// Se insertan las publicaciones programadas de artículos pendientes de replanificar
	    		setPendingPublicationSchedules(docRoot);
	    		
	    		result = docRoot.asXML();
	    	}
	    }
	    
	    return result;
	}
	
	/**
	 * Se importan en los Layouts la información relativa a los portlets, section properties y catálogos
	 * 
	 * @param xmlIOContext
	 * @param doc
	 * @param nodes
	 * @throws Exception
	 */
	private void importLayoutProperties(XMLIOContext xmlIOContext, Document doc, List<Node> nodes) throws Exception
	{
		for (int i = 0; nodes != null && i < nodes.size(); i++) 
		{
			Element item = (Element) nodes.get(i);
			LayoutXmlIO layoutXmlIO = (LayoutXmlIO)XMLIOUtil.getItemByType(IterKeys.CLASSNAME_LAYOUT, xmlIOContext);
			ErrorRaiser.throwIfNull(layoutXmlIO);

			layoutXmlIO.importProperties(item, doc);
		}
		
		for (int i = 0; nodes != null && i < nodes.size(); i++) 
		{
			Element item = (Element) nodes.get(i);
			LayoutXmlIO layoutXmlIO = (LayoutXmlIO)XMLIOUtil.getItemByType(IterKeys.CLASSNAME_LAYOUT, xmlIOContext);
			ErrorRaiser.throwIfNull(layoutXmlIO);

			layoutXmlIO.importSectionProperties(item, doc);
		}
	}
	
	private void insertTypConAlternative(Document doc) throws Exception
	{
		if (doc != null)
		{
			long delegationId = XMLHelper.getLongValueOf(doc, XPATH_TYPCONALTERNATIVE, -1);
			ABTestingMgr.insertTypConAlternative(delegationId);
		}
	}
	
	private void reindexArticlesFromXML(Document doc)
	{
		if(doc != null)
		{
			List<JournalArticle> jaList = new ArrayList<JournalArticle>();
			
			long globalGroupId = GroupMgr.getGlobalGroupId();
			Element root = doc.getRootElement();

			//JournalArticle updates
			XPath xpath = SAXReaderUtil.createXPath("//item[@operation='" + IterKeys.UPDATE + "' and @classname='" + IterKeys.CLASSNAME_JOURNALARTICLE +"']");
			List<Node> nodes = xpath.selectNodes(root);
			if(nodes != null && nodes.size() > 0)	
			{
				for(Node item:nodes)
				{
					String globalId = XMLHelper.getTextValueOf(item, "@globalid");
					try 
					{
						Live nodeLive = LiveLocalServiceUtil.getLiveByGlobalId(globalGroupId, IterKeys.CLASSNAME_JOURNALARTICLE, globalId);
						if (nodeLive!=null)
						{
							JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, nodeLive.getLocalId());
							if (journalArticle != null)
								jaList.add(journalArticle);
						}
					}
					catch (Exception e)
					{
						_log.error( "Update operation. Can not reindex content of article: '" + globalId + "' " + e.toString() );
						_log.debug(e);
					}
				}
			}	

			//JournalArticle creates
			xpath = SAXReaderUtil.createXPath("//item[@operation='" + IterKeys.CREATE + "' and @classname='" + IterKeys.CLASSNAME_JOURNALARTICLE +"']");
			nodes = xpath.selectNodes(root);
			if(nodes != null && nodes.size() > 0)	
			{
				for(Node item:nodes)
				{
					Node articleIdNode = item.selectSingleNode( "param[@name='articleid']");
					if(articleIdNode != null)
					{
						String articleId = CDATAUtil.strip(articleIdNode.getText());
						if(Validator.isNotNull(articleId))
						{
							try
							{
								JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId);
								if(journalArticle != null)
								{
									boolean indexable = true;
									Node indexableNode = item.selectSingleNode("param[@name='indexable']");
									if(indexableNode != null)
										indexable = Boolean.parseBoolean(CDATAUtil.strip(indexableNode.getText()));

									journalArticle.setIndexable(indexable);

									JournalArticleLocalServiceUtil.updateJournalArticle(journalArticle);
									jaList.add(journalArticle);
								}
							}
							catch (Exception e)
							{
								_log.error( "Create operation. Can not reindex content of article: '" + articleId + "' " + e.toString() );
								_log.debug(e);
							}
						}
					}
				}
			}
			
			int numArticles = jaList.size();
			if(numArticles>0)
			{
				try
				{
					JournalIndexerUtil jiu = new JournalIndexerUtil();
					JournalArticle ja = jaList.get(0);
					
					JournalIndexerMgr journalIdxMgr = new JournalIndexerMgr();
					journalIdxMgr.setDelegationId(ja.getDelegationId());
					
					for (int i = 0; i < jaList.size(); i++)
					{
						JournalArticle article = jaList.get(i);
						boolean doCommit = (i == jaList.size()-1);
						
						try
						{
							journalIdxMgr.domsToIndex(jiu.createDom(article), doCommit );
						}
						catch (Exception e)
						{
							journalIdxMgr.domToIndex(null, doCommit);

							_log.error("Can not index article: " + article.getArticleId() + ". " + e, e);
							_log.debug(e);
						}
					}
				}
				catch (Exception e)
				{
					_log.error("Can not index articles " + e, e);
					_log.debug(e);
				}
			}
		}
	}
	
	public boolean importContent(Document doc, String className, String groupName, String globalId, XMLIOContext xmlIOContext){
		return importContent(doc, className, groupName, globalId, xmlIOContext, false);
	}
	
	// Recibe el xml con toda la importación
	public boolean importContent(Document doc, String className, String groupName, String globalId, XMLIOContext xmlIOContext, boolean validateOnly) {
				
	    try 
	    {	    			    
		    String path = "//item[@classname='" + className + "' and (@operation='" + IterKeys.CREATE + "' or @operation='" + IterKeys.UPDATE + "') and @globalid='" + globalId + "' and @groupid=" + StringUtil.escapeXpathQuotes(groupName) + "]";
			
		    _log.trace("\n");
		    _log.trace("importContent: path");
			XPath xpathSelector = SAXReaderUtil.createXPath(path);
			_log.trace("importContent: createXPath");
			List<Node> nodes = xpathSelector.selectNodes(doc);
			_log.trace("importContent: selectNodes");
			ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(className, xmlIOContext);
			_log.trace("importContent: getItemByType");
				
			if (itemXmlIO != null) 
			{
				for (Node node : nodes) 
				{
					Element item = (Element) node;
					if (validateOnly)
					{
						itemXmlIO.validateContents(item,doc);
					}
					else
					{
						itemXmlIO.importContents(item, doc);
					}
				}					
			}
	    } 
	    catch (Exception e) 
	    { 		    	
	    	_log.error("ImportContent Error", e);
	    }
	    
	    _log.trace("importContent: before validateLog.");
	    Document dom = xmlIOContext.itemLog.toXML();
	    boolean valLog = xmlIOContext.itemLog.validateLog(className, groupName, globalId);
	    _log.trace("importContent: after validateLog.");
	    _log.trace("\n");
	    
	    return valLog;		
	}
	
	/*
	 * OUTPUT
	 */
	
	public File exportContentFTP(
			String ftpServer, String ftpUser, String ftpPassword, String localPath,
			String xslPath, long companyId, long userId, long groupId) throws Exception{
	
		File destFile = exportContent(localPath, xslPath, companyId, userId, groupId, true, null);
		FTPUtil.sendFile(ftpServer, ftpUser, ftpPassword, destFile.getName(), destFile.getAbsolutePath(), "");
		
		return destFile;
	}
	
	public File exportContent(String filePath, File xslFile, long companyId, long userId, long groupId) throws ChannelExportException{
		return exportContent(filePath, xslFile.getAbsolutePath(), companyId, userId, groupId);
	}
	
	public File exportContent(String filePath, String xslPath, long companyId, long userId, long groupId) throws ChannelExportException{
		return exportContent(filePath, xslPath, companyId, userId, groupId, true, null);
	}
	
	public File exportContent(String filePath, String xslPath, long companyId, long userId, long groupId, boolean zip, String classname) throws ChannelExportException {
		return exportContent(filePath, xslPath, companyId, userId, groupId, zip, classname, null);
	}
	
	public File exportContent(String filePath, String xslPath, long companyId, long userId, long groupId, boolean zip, String className, Date sinceModifiedDate) throws ChannelExportException 
	{
		try
		{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			XMLIOExport xmlioExport = new XMLIOExport(groupId);
			XMLIOContext xmlioContext = new XMLIOContext();		
			xmlioContext.setCompanyId(companyId);
			xmlioContext.setUserId(userId);
			xmlioContext.setGroupId(groupId);
			
			Document doc = SAXReaderUtil.createDocument();
			
			Element root = doc.addElement(IterKeys.XMLIO_XML_ELEMENT_ROOT);
			
			if (className == null || className.equals("") || className.equals(IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL))
			{
				exportAllContents(xmlioExport, xmlioContext, globalGroupId, root, sinceModifiedDate);
			}
			else
			{
				ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(className, xmlioContext);
				itemXmlIO.exportContents(xmlioExport, root, globalGroupId, IterKeys.CREATE, IterKeys.XMLIO_XML_EXPORT_OPERATION, sinceModifiedDate);
				itemXmlIO.exportContents(xmlioExport, root, groupId, IterKeys.CREATE, IterKeys.XMLIO_XML_EXPORT_OPERATION, sinceModifiedDate);
			}
			
			xmlioExport.setContent(doc.asXML());
			
			if (zip)
			{
				return xmlioExport.generateZip(filePath,  xslPath);
			}
			else
			{
				xmlioExport.generateFiles(filePath,  xslPath);
				return new File(filePath);
			}
		}
		catch(Exception err)
		{
			_log.error("Export failed", err);
			throw new ChannelExportException(err.getMessage());
		}

	}
	
	public void exportAllContents(XMLIOExport xmlioExport, XMLIOContext xmlIOContext, long globalGroupId, 
			Element root, Date sinceModifiedDate) throws Exception
	{
		for (String type : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT) 
		{
			ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(type, xmlIOContext);
			ErrorRaiser.throwIfNull(itemXmlIO);
			
			_log.info("Exporting contents of type " + type);
			itemXmlIO.exportContents(xmlioExport, root, globalGroupId, IterKeys.CREATE, IterKeys.XMLIO_XML_EXPORT_OPERATION, sinceModifiedDate);
			itemXmlIO.exportContents(xmlioExport, root, xmlIOContext.getGroupId(), IterKeys.CREATE, IterKeys.XMLIO_XML_EXPORT_OPERATION, sinceModifiedDate);
	    }
		
	}
	
	/**
	 * Se comprueban los parámetros de entrada de la importación:
	 * Tiene que existir el companyID
	 * El userID tiene que existir, pertener al companyID y tener role de Administrador
	 * Tiene que existir el groupID y pertener al companyID
	 * 
	 * @param sourceFile
	 * @param companyId
	 * @param userId
	 * @param groupId
	 * @throws ServiceError
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	private void checkImportParams(File sourceFile, long companyId, long userId, long groupId) throws ServiceError, PortalException, SystemException
	{
		// Tiene que existir el fichero/directorio con binarios a importar
		ErrorRaiser.throwIfFalse(FileUtil.exists(sourceFile), IterErrorKeys.XYZ_E_IMPORT_FILE_NOT_FOUND_ZYX);

		checkImportParams(companyId, userId, groupId);
	}
	private void checkImportParams(long companyId, long userId, long groupId) throws ServiceError, PortalException, SystemException
	{
		// Tiene que existir el companyID, si no existiese el método getCompanyById devuelve una excepción
		try
		{
			Company company = CompanyLocalServiceUtil.getCompanyById(companyId);
			ErrorRaiser.throwIfNull((Object)company, IterErrorKeys.XYZ_E_IMPORT_INVALID_COMPANYID_ZYX, String.valueOf(companyId));
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_IMPORT_INVALID_COMPANYID_ZYX, String.valueOf(companyId));
		}
		
		// El userID tiene que existir, pertener al companyID y tener role de Administrador
		try
		{
			ErrorRaiser.throwIfFalse( UserServiceUtil.hasRoleUser(companyId, RoleConstants.ADMINISTRATOR, userId, true), IterErrorKeys.XYZ_E_IMPORT_INVALID_USRID_ZYX, String.valueOf(userId));
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_IMPORT_INVALID_USRID_ZYX, String.valueOf(userId));
		}
		
		// Tiene que existir el groupID y pertener al companyID. getGroup ya lanza una excepción si no existe el grupo
		try
		{
			ErrorRaiser.throwIfFalse( GroupLocalServiceUtil.getGroup(groupId).getCompanyId() == companyId, IterErrorKeys.XYZ_E_IMPORT_INVALID_GRPID_ZYX, String.valueOf(groupId));
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_IMPORT_INVALID_GRPID_ZYX, String.valueOf(groupId));
		}
	}
	
	
	public void refreshCache(String cacheRefresh) throws Exception
	{
		CacheRefresh cr = new CacheRefresh(cacheRefresh);
		
		refreshCache(cr);
	}
	
	public void refreshCache(CacheRefresh cacheRefresh) throws Exception
	{
		boolean doSyn = HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_REFRESH_CACHE_SYNCHRONOUSLY, false);
		
		if (doSyn)
		{
			_log.info("Refresh cache synchronously");
			refreshCacheSynchronously( cacheRefresh );
		}
		else
		{
			_log.info("Refresh cache asynchronously");
			RefreshCacheThread refreshCacheThread = new RefreshCacheThread( cacheRefresh );
			refreshCacheThread.start();
		}
	}
	
	public void refreshCacheSynchronously(CacheRefresh cacheRefresh) throws Exception
	{
		CacheRefreshMgr.refresh(cacheRefresh);
	}
	
	private void pingGoogleSitemap(long groupId)
	{
		try
		{
			Date lastPing = GroupConfigLocalServiceUtil.getGooglePing(groupId);
			
			if( (lastPing==null) || ((Calendar.getInstance().getTimeInMillis()-lastPing.getTime())> Time.HOUR) )
			{
				PingGoogle pingGoogle = new PingGoogle( groupId );
				pingGoogle.start();
			}
		}
		catch (Exception e)
		{
			_log.error("Error starting ping google thread. " + e.toString());
			_log.trace("Error starting ping google thread. " + e);
		}
	}
	
	private void checkDefaultPageTemplates(long groupId, String defaultPgTmplId_, String defaultMobilePgTmplId_) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ServiceError
	{
		String errorCode = StringPool.BLANK;
		Class<?> clazz = Class.forName("com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil");
		
		Object[] methodParams = new Object[2];
		methodParams[0] = groupId;
		methodParams[1] = IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE;
		
		Method method = null;
		if( Validator.isNotNull(defaultPgTmplId_) )
		{
			method = clazz.getMethod("getDefaultPageTemplate", long.class, java.lang.String.class);
			if( Validator.isNull(method.invoke(clazz, methodParams)) )
			{
				method = clazz.getMethod("setDefaultPageTemplate", java.lang.String.class);
				method.invoke(clazz, new Object[]{defaultPgTmplId_});
				
				errorCode = IterErrorKeys.XYZ_E_LIVE_ENVIRONMENT_WITHOUT_DEFAULT_PAGETEMPLATE_ZYX ;
			}
		}
		
		if( Validator.isNotNull(defaultMobilePgTmplId_) )
		{
			method = clazz.getMethod("getDefaultMobilePageTemplate", long.class, java.lang.String.class);
			if( Validator.isNull(method.invoke(clazz, methodParams)) )
			{
				method = clazz.getMethod("setDefaultPageTemplateMobile", java.lang.String.class);
				method.invoke(clazz, new Object[]{defaultMobilePgTmplId_});
				
				if( errorCode.equals(StringPool.BLANK) )
					errorCode = IterErrorKeys.XYZ_E_LIVE_ENVIRONMENT_WITHOUT_DEFAULT_MOBILE_PAGETEMPLATE_ZYX;
			}
		}
		
		ErrorRaiser.throwIfError(errorCode);
	}
	
	public void importDefaultSectionProperties(String xml) throws Exception
	{
		_log.debug("Import default section properties: " + xml);
		
		Document dom = SAXReaderUtil.read(xml);
		
		String groupName = XMLHelper.getStringValueOf(dom.getRootElement(), "@groupname");
    	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
    	long scopeGroupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
    	
    	Node sectionProperties = dom.selectSingleNode("/rs/row");
    	
		LayoutLocalServiceUtil.updateSectionProperties(scopeGroupId, 0, 
										XMLHelper.getStringValueOf(sectionProperties, "@menuelementid"), 
										XMLHelper.getStringValueOf(sectionProperties, "@headerelementid"), 
										XMLHelper.getStringValueOf(sectionProperties, "@footerelementid"), 
										XMLHelper.getStringValueOf(sectionProperties, "@aboutid"), 
										XMLHelper.getStringValueOf(sectionProperties, "@autorss"), 
										XMLHelper.getStringValueOf(sectionProperties, "@autorssxsl"), 
										true);
	}

	private void setPendingPublicationSchedules(Element docRoot)
	{
		try
		{
    		String processId = XMLHelper.getStringValueOf(docRoot, "@processid");
    		List<Node> articleScheduledPublications = docRoot.selectNodes("/iter/scheduledPublications/publications");
    		if (articleScheduledPublications.size() > 0)
    		{
    			StringBuilder sql = new StringBuilder("INSERT INTO pending_actions (publicationId, kind, articleId) VALUES \n");
    			String sqlValues = "('%s', 'publication schedule', '%s'),";
    			
	    		for (Node articleScheduledPublicaion: articleScheduledPublications)
	    		{
	    			String articleId = XMLHelper.getStringValueOf(articleScheduledPublicaion, "@articleId");
	    			sql.append(String.format(sqlValues, processId, articleId));
	    		}
	    		
	    		PortalLocalServiceUtil.executeUpdateQuery(sql.deleteCharAt(sql.length()-1).toString());
    		}
		}
		catch (Throwable t)
		{
			// TODO: Monitor
			_log.error(t);
		}
	}
}
