package com.protecmedia.iter.news.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.liferay.portal.kernel.util.MetadataControlUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.MetadataControlUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFileEntryUtil;
import com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.news.model.Metadata;
import com.protecmedia.iter.news.service.MetadataLocalServiceUtil;
import com.protecmedia.iter.news.service.base.MetadataControlLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.PublishUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class MetadataControlLocalServiceImpl extends MetadataControlLocalServiceBaseImpl 
{
	
	private static Log _log = LogFactoryUtil.getLog(MetadataControlLocalServiceImpl.class);
	
	final static String CHARACTERS               = "characters";
	final static String IMAGE					 = "image";
	final static String FIELD                    = "field";
	final static String FILE					 = "file";		
	final static String META					 = "meta";
	final static String OPTION		             = "option"; 
	final static String NCHARS                    = "nchars";
	final static String DEFAULT_TITLE_SIZE       = Long.toString(MetadataControlUtil.DEFAULT_NCHAR ); // Tamaño máximo de cadena por defecto
	final static String DEFAULT_DESCRIPTION_SIZE = Long.toString(MetadataControlUtil.NCHAR_NONECHAR); // Cero
	final static String OG_IMAGE				 = "ogimage";
	final static String PATH					 = "path";
	final static String MOD_DATE				 = "moddate";
	final static String PUB_DATE				 = "pubdate";
	
	//Sólo una publicación al mismo tiempo
	private static ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private static Lock writeLock = rwLock.writeLock();
	private static final String ITER_METADATA_ZIP_FILE_NAME = IterKeys.XMLIO_ZIP_FILE_PREFIX + "metadata_%s.zip";
	
	// Si no hay registro news_metadata para el grupo indicado se crea uno por defecto
	public String getConfig(String groupId) throws Exception
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null");	
		
		long scopeGroupId = Long.parseLong(groupId);
		// Buscamos el registro news_metadata de BBDD para el grupo indicado
		Metadata meta = MetadataLocalServiceUtil.getMetadataByMetadataStructure(scopeGroupId, IterKeys.STRUCTURE_ARTICLE);
		
		// No hay ninguno, damos uno de alta con los valores por defecto
		if (null == meta){
			meta = createNewDefaultMetadataControl(scopeGroupId, IterKeys.STRUCTURE_ARTICLE, true); 
			// Damos de alta el metadato
			MetadataLocalServiceUtil.addMetadata(meta);
		}
		
		// Formamos el xml que necesita flex para pintarlo
		return transformPreferencesToXMLForFlex(scopeGroupId, meta.getPreferences());
	}

	public void setConfig(String groupId, String xmlFlex) throws Exception
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlFlex), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xmlFlex is null");
		
		setConfig(Long.parseLong(groupId), SAXReaderUtil.read(xmlFlex));
	}
	
	private void setConfig(long scopeGroupId, Document docFlex) throws SystemException, ServiceError, DocumentException
	{
		/* Nos llega algo tal que así: (tanto el título, la descripción y la imagen pueden llegar vacíos)
		<rs>
		    <title characters="20"><![CDATA[title]]></title>			
		    <descripcion characters="allChars"><![CDATA[externalLink]]></descripcion>			
		    <image><![CDATA[Image1.nombreEncuadre]]></image>
		    <ogimage path= "/documents/groupid/folderid/imgtitle" modifieddate="" pubdate=""><![CDATA[UUID]]></ogimage>
		</rs> */
		
		if(_log.isDebugEnabled())
			_log.debug("groupid: " + scopeGroupId + "  config: " + docFlex.asXML());
		
		final String titleCharacters       = XMLHelper.getTextValueOf(docFlex, "/rs/" + MetadataControlUtil.META_TITLE       + "/@" + CHARACTERS, null);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(titleCharacters),       IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "titleCharacters is null");	
		
		final String descriptionCharacters = XMLHelper.getTextValueOf(docFlex, "/rs/" + MetadataControlUtil.META_DESCRIPTION + "/@" + CHARACTERS, null);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(descriptionCharacters), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "descriptionCharacters is null");
		
		// Comprobamos que el atributo characters del título y de la descripción son válidos
		checkTitleAndDescriptionCharacters(titleCharacters, descriptionCharacters);
		
		// Obtenemos los valores para el título y la descripción (pueden llegar vacíos)
		final String titleValue       = XMLHelper.getTextValueOf(docFlex, "/rs/" + MetadataControlUtil.META_TITLE       + "/text()", "");
		final String descriptionValue = XMLHelper.getTextValueOf(docFlex, "/rs/" + MetadataControlUtil.META_DESCRIPTION + "/text()", "");	
		final String imgAndFrame      = XMLHelper.getTextValueOf(docFlex, "/rs/" + IMAGE                                + "/text()", "");		
		
		// No llega vacío, comprobamos su formato
		if (Validator.isNotNull(imgAndFrame)){
			// Recibimos un string del siguiente tipo: "nombreImagen.nombreEncuadre" si es distinto, error. (también puede llegar vacío)	
			ErrorRaiser.throwIfFalse(imgAndFrame.indexOf(".") != -1, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid img value");
			
			final String[] twoValues = imgAndFrame.split("\\.");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(twoValues) && twoValues.length == 2, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid img and frame value");
		}
		
		Node ogImageNode = docFlex.selectSingleNode("/rs/" + OG_IMAGE);
		String imgId = XMLHelper.getTextValueOf(ogImageNode, "text()", "");
		String modDate = "";
		String pubDate = "";
		boolean isImportProcess = GetterUtil.getBoolean(XMLHelper.getStringValueOf(docFlex, "/rs/@importProcess"), false);
		
		if(_log.isDebugEnabled())
			_log.debug("setConfig isImportProcess: " + String.valueOf(isImportProcess));
		
		if(isImportProcess)
			modDate = DateUtil.getDBFormat().format(new Date(System.currentTimeMillis()));
		else
		{
			modDate =  XMLHelper.getTextValueOf(ogImageNode, MetadataControlUtil.ATTR_MODDATE, "");
			pubDate =  XMLHelper.getTextValueOf(ogImageNode, MetadataControlUtil.ATTR_PUBDATE, "");
		}
		
		// Modificamos el xml recibido de flex para la columna news_metadata.preferences y se lo asignamos al meta.
		final String preferences = createPreferences(titleCharacters, descriptionCharacters, titleValue, descriptionValue, imgAndFrame, imgId, modDate, pubDate);
				
		Metadata meta = MetadataLocalServiceUtil.getMetadataByMetadataStructure(scopeGroupId, IterKeys.STRUCTURE_ARTICLE);
		
		// Creamos uno nuevo
		if (null == meta){
			meta = createNewDefaultMetadataControl(scopeGroupId, IterKeys.STRUCTURE_ARTICLE, false);
			meta.setPreferences(preferences);
			MetadataLocalServiceUtil.addMetadata(meta);
			
		// Actualizamos el existente
		}else{
			meta.setPreferences(preferences);
			MetadataLocalServiceUtil.updateMetadata(meta);
		}
	}
	
	private Metadata createNewDefaultMetadataControl(long groupId, String structureName, boolean createPreferences) throws SystemException
	{	
		// Obtenemos la siguiente clave primaria
		final long id = CounterLocalServiceUtil.increment();
		Metadata meta = MetadataLocalServiceUtil.createMetadata(id);		
		meta.setGroupId(groupId);
		meta.setStructureName(structureName);
		if (createPreferences){
			meta.setPreferences(MetadataControlUtil.getDefaultArtPreferences(structureName) );
		}		
		return meta;
	}
		
	// Dado un xml con las preferencias (news_metadata.preferences) genera el xml necesario que entiende flex
	private String transformPreferencesToXMLForFlex(long scopeGroupId, String preferences) throws Exception
	{			
		/* Nos llega algo así:		
	
			<rs>
				<meta namevalue="title"       field="defined" defined="Headline"  	custom="" size="allChars" nchars="0" />
				<meta namevalue="description" field="defined" defined="Text"      	custom="" size="nChars"   nchars="40"/>
				<meta namevalue="img"         field="defined" defined="Img.Frame" 	custom=""                            />
				<meta namevalue="ogimg" 	  field="defined" defined="UUID"		custom="" modifieddate="" pubdate=""/>
			</rs>
		
		Y tenemos que devolver algo de este estilo para el flex:
	
			<rs>
			    <title characters="20"><![CDATA[title]]></title>				
			    <descripcion characters="allChars"><![CDATA[externalLink]]></descripcion>				
			    <image><![CDATA[Image1.nombreEncuadre]]></image>
			    <ogimage path="/documents/groupid/folderid/dlFileEntryTitle" modifieddate="" pubdate=""><![CDATA[UUID]]></ogimage>
			</rs> */
		
		if(_log.isDebugEnabled())
			_log.debug("transformPreferencesToXMLForFlex preferences: " + preferences);
		
		final Document docPref = SAXReaderUtil.read(preferences);		
		
		Document doc = SAXReaderUtil.read("<rs/>");
		Element root = doc.getRootElement();
		
		// Título
		Element title = root.addElement(MetadataControlUtil.META_TITLE);
		String aux = XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_TITLE + "']/@" + MetadataControlUtil.ATTR__SIZE);
		if (aux.equals(MetadataControlUtil.ALLCHARS)){
			title.addAttribute(CHARACTERS, MetadataControlUtil.ALLCHARS);
		}else{
			title.addAttribute(CHARACTERS, XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_TITLE + "']/@nchars"));
		}	
		title.addCDATA(XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_TITLE + "']/@" + MetadataControlUtil.DEFINED, ""));
		
		// Descripción
		Element description = root.addElement(MetadataControlUtil.META_DESCRIPTION);
		aux = XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_DESCRIPTION + "']/@" + MetadataControlUtil.ATTR__SIZE);
		if (aux.equals(MetadataControlUtil.ALLCHARS)){
			description.addAttribute(CHARACTERS, MetadataControlUtil.ALLCHARS);
		}else{
			description.addAttribute(CHARACTERS, XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_DESCRIPTION + "']/@nchars"));
		}		
		description.addCDATA(XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_DESCRIPTION + "']/@" + MetadataControlUtil.DEFINED, ""));
		
		// Imagen
		Element image = root.addElement(IMAGE);		
		image.addCDATA(XMLHelper.getTextValueOf(docPref, "/rs/meta[@namevalue='" + MetadataControlUtil.META_IMG + "']/@" + MetadataControlUtil.DEFINED, ""));		
		
		// Imagen por defecto para la etiqueta <og:image/>
		Element ogImage = root.addElement(OG_IMAGE);
		Node ogNode = docPref.selectSingleNode("/rs/meta[@namevalue='" + MetadataControlUtil.META_OGIMG + "']");
		String fileEntryTitle = XMLHelper.getTextValueOf(ogNode, "@" + MetadataControlUtil.DEFINED, "");
		String imgPath = "";
		if( Validator.isNotNull(fileEntryTitle) )
		{
			DLFileEntry dlfe = DLFileEntryLocalServiceUtil.getFileEntryByTitle(scopeGroupId, getFolderId(scopeGroupId, GroupMgr.getDefaultUserId()), fileEntryTitle);
			imgPath = DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(dlfe.getGroupId(), dlfe.getFolderId(), dlfe.getTitle()).concat("?env=preview");
		}
		
		ogImage.addAttribute(PATH, imgPath );
		ogImage.addAttribute(MOD_DATE, XMLHelper.getTextValueOf(ogNode, MetadataControlUtil.ATTR_MODDATE, "") );
		ogImage.addAttribute(PUB_DATE, XMLHelper.getTextValueOf(ogNode, MetadataControlUtil.ATTR_PUBDATE, "") );
		ogImage.addCDATA( fileEntryTitle );
		
		if(_log.isDebugEnabled())
			_log.debug("transformPreferencesToXMLForFlex flexDom: " + doc.asXML());
		
		return doc.asXML();
	}
	
	// Dado el xml de flex, lo transformamos al xml que va en news_metadata.preferences
	private String createPreferences(String titleCharacters, String descriptionCharacters, String titleValue, 
		                             String descriptionValue, String imgAndFrame, String imgId, String modDate, String pubDate) throws DocumentException
	{
		/* Nos llega algo así:
		  
		 	<?xml version="1.0"?> (título, descripción e imagen pueden llegar vacíos)
				<rs>
				    <title characters="allChars"><![CDATA[Headline]]></title>		
				    <descripcion characters="40"><![CDATA[Headline]]></descripcion>		
				    <image><![CDATA[Image1.nombreEncuadre]]></image>
				    <ogimage modifieddate="" pubdate=""><![CDATA[UUID]]></ogimage>
				</rs>
				
		 Y tenemos que devolver algo de este estilo para el flex:
		
			<?xml version="1.0" encoding="UTF-8"?>
			<rs>
				<meta namevalue="title"       field="defined" defined="Headline"    custom="" size="allChars" nchars="0"  />
				<meta namevalue="description" field="defined" defined="Text"        custom="" size="nChars"   nchars="40" />
				<meta namevalue="img"         field="defined" defined="Image.frame" custom=""                             />
				<meta namevalue="ogimg" 	  field="defined" defined="uuid" 	    custom="" modifieddate="" pubdate=""/>
			</rs> */		
		
		Document preferences = SAXReaderUtil.read("<rs/>");
		Element root = preferences.getRootElement();
		
		// Título
		Element meta = root.addElement(META);
		meta.addAttribute(MetadataControlUtil.ATTR__NAMEVALUE, MetadataControlUtil.META_TITLE);
		meta.addAttribute(FIELD,     						   MetadataControlUtil.DEFINED);
		meta.addAttribute(MetadataControlUtil.DEFINED,   	   titleValue);
		meta.addAttribute(MetadataControlUtil.CUSTOM,          "");
		meta.addAttribute(MetadataControlUtil.ATTR__SIZE,      (titleCharacters.equals(MetadataControlUtil.ALLCHARS) ? MetadataControlUtil.ALLCHARS : MetadataControlUtil.NCHARS)      );
		meta.addAttribute(NCHARS,                              (titleCharacters.equals(MetadataControlUtil.ALLCHARS) ? "0"                          : titleCharacters)                 );		
		meta.addAttribute(MetadataControlUtil.DEFINED,         titleValue);		
		
		// Descripción
		meta = root.addElement(META);
		meta.addAttribute(MetadataControlUtil.ATTR__NAMEVALUE, MetadataControlUtil.META_DESCRIPTION);
		meta.addAttribute(FIELD,     						   MetadataControlUtil.DEFINED);
		meta.addAttribute(MetadataControlUtil.DEFINED,   	   descriptionValue);
		meta.addAttribute(MetadataControlUtil.CUSTOM,    	   "");
		meta.addAttribute(MetadataControlUtil.ATTR__SIZE,      (descriptionCharacters.equals(MetadataControlUtil.ALLCHARS) ? MetadataControlUtil.ALLCHARS : MetadataControlUtil.NCHARS));
		meta.addAttribute(NCHARS,                              (descriptionCharacters.equals(MetadataControlUtil.ALLCHARS) ? "0"                          : descriptionCharacters)     );	
		meta.addAttribute(MetadataControlUtil.DEFINED,         descriptionValue);		
		
		// Img
		meta = root.addElement(META);
		meta.addAttribute(MetadataControlUtil.ATTR__NAMEVALUE, MetadataControlUtil.META_IMG);
		meta.addAttribute(FIELD,     						   MetadataControlUtil.DEFINED);
		meta.addAttribute(MetadataControlUtil.DEFINED,   	   imgAndFrame);		
		meta.addAttribute(MetadataControlUtil.CUSTOM,    	   "");
		
		//og Image
		meta = root.addElement(META);
		meta.addAttribute(MetadataControlUtil.ATTR__NAMEVALUE, MetadataControlUtil.META_OGIMG);
		meta.addAttribute(FIELD,     						   MetadataControlUtil.DEFINED);
		meta.addAttribute(MetadataControlUtil.DEFINED,   	   imgId);		
		meta.addAttribute(MetadataControlUtil.CUSTOM,    	   "");
		meta.addAttribute(MetadataControlUtil.ATTR__MODDATE,   modDate);
		meta.addAttribute(MetadataControlUtil.ATTR__PUBDATE,   pubDate);
		
		return preferences.asXML();
	}
	
	// Comprueba el atributo characters para el título y la descripción
	private void checkTitleAndDescriptionCharacters(String titleCharacters, String descriptionCharacters) throws ServiceError
	{		
		if (!titleCharacters.equals(MetadataControlUtil.ALLCHARS) && !titleCharacters.equals(MetadataControlUtil.NCHARS)){
			try{
				Integer.parseInt(titleCharacters);
			}catch(Exception e){
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid title characters: '" + titleCharacters + "'");
			}
		}
		
		if (!descriptionCharacters.equals(MetadataControlUtil.ALLCHARS) && !descriptionCharacters.equals(MetadataControlUtil.NCHARS)){
			try{
				Integer.parseInt(descriptionCharacters);
			}catch(Exception e){
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid description characters: '" + descriptionCharacters + "'");
			}
		}
	}
	
	public Document exportData(Long groupId) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(getConfig(String.valueOf(groupId)));
		return dom;
	}
	
	public void importPublishedData(String fileName) throws Exception
	{
		File importFile 	= null;
		File temporaryDir 	= null;
		
		try
		{
			long companyId = IterGlobal.getCompanyId();
			ErrorRaiser.throwIfFalse(writeLock.tryLock(), IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
			LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			
			String importFilePath = null;
			
			if (liveConf.getOutputMethod().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP))
			{
				String ftpServer = liveConf.getFtpPath();
				String ftpUser = liveConf.getFtpUser();
				String ftpPassword = liveConf.getFtpPassword();
				String localPath = liveConf.getLocalPath();

				importFilePath = FTPUtil.receiveFile(ftpServer, ftpUser, ftpPassword, fileName, localPath, StringPool.BLANK);
			} 
			else
			{
				String remotePath = liveConf.getRemotePath();
				importFilePath = remotePath + File.separatorChar + fileName;	
			}
			
			String zipExtension = ".zip";

			String temporaryDirPath = importFilePath.replace(zipExtension, StringPool.BLANK);
			
			importFile = new File(importFilePath);
			temporaryDir = new File(temporaryDirPath);
			
			ZipUtil.unzip(importFile, temporaryDir);
			
			File iterXmlFile = new File(temporaryDirPath + File.separatorChar + IterKeys.XMLIO_XML_MAIN_FILE_NAME);
			
			Document dom = SAXReaderUtil.read(iterXmlFile);
			
			if(_log.isDebugEnabled())
				_log.debug("importPublishedData iter xml: " + dom.asXML());
			
			Element rs = dom.getRootElement();

			String groupname = XMLHelper.getTextValueOf(rs, "@groupname");
			ErrorRaiser.throwIfNull(groupname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			long scopeGroupId = GroupLocalServiceUtil.getGroup(companyId, groupname).getGroupId();
			
			importDefaultImages(scopeGroupId, temporaryDirPath, dom, true, false);
			
			setConfig(scopeGroupId, dom);
			
		}
		finally
		{
			writeLock.unlock();
			
			//Borramos los ficheros de importación
			PublishUtil.hotConfigDeleteFile(temporaryDir);
			PublishUtil.hotConfigDeleteFile(importFile);
		}
	}
	
	public void importData(String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		
		if(_log.isDebugEnabled())
			_log.debug("importData iter xml: " + dom.asXML());
		
		Element rs = dom.getRootElement();

		long scopeGroupId = XMLHelper.getLongValueOf(rs, "@groupId");
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String imgPath = XMLHelper.getStringValueOf(rs, "@filesPath");
		ErrorRaiser.throwIfNull(imgPath, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean updtIfExist		= GetterUtil.getBoolean(XMLHelper.getStringValueOf(rs, "@updtIfExist"));
		boolean isImportProcess	= GetterUtil.getBoolean(XMLHelper.getStringValueOf(rs, "@importProcess"));
		
		importDefaultImages(scopeGroupId, imgPath, dom, updtIfExist, isImportProcess);
		
		setConfig(scopeGroupId, dom);
	}
	
	private void importDefaultImages(long scopeGroupId, String temporaryDirPath, Document dom, boolean updtIfExists, boolean isImportProcess) throws ServiceError, SystemException
	{
		InputStream is = null;
		try
		{
			Node imgNode = dom.selectSingleNode("/rs/" + OG_IMAGE);
			if( Validator.isNotNull(imgNode) )
			{
				String fileentryTitle = XMLHelper.getTextValueOf(imgNode, "text()", "");
				if(Validator.isNotNull(fileentryTitle))
				{
					String filePath = temporaryDirPath + File.separatorChar + fileentryTitle;
					try
					{
						FileInputStream fis = new FileInputStream(filePath);
						byte[] b = IOUtils.toByteArray(fis);
						is = new ByteArrayInputStream(b);
						long usrId = GroupMgr.getDefaultUserId();
						long folderId = getFolderId(scopeGroupId, usrId);
						
						if(isImportProcess)
						{
							int indexPreriod = fileentryTitle.indexOf(StringPool.PERIOD);
							boolean hasExtension = indexPreriod > 0 && indexPreriod < fileentryTitle.length();
							ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_IMAGE_WITHOUT_EXTENSION_ZYX);
							
							fileentryTitle = SQLQueries.getUUID() + fileentryTitle.substring(indexPreriod, fileentryTitle.length());
							
							createReplaceFileEntry(scopeGroupId, folderId, is, b.length, fileentryTitle, fileentryTitle, usrId, updtIfExists);
							
							imgNode.setText(fileentryTitle);
						}
						else
							createReplaceFileEntry(scopeGroupId, folderId, is, b.length, fileentryTitle, fileentryTitle, usrId, updtIfExists);
						
						
					}
					catch (FileNotFoundException fnfe) 
					{
						_log.trace("importDefaultImages File not found: " + filePath + ". " + fnfe.toString());
						_log.debug(fnfe);
					}
				}
			}
		}
		catch(ORMException orme)
		{
			throw orme;
		}
		catch(ServiceError se)
		{
			throw se;
		}
		catch(Throwable t)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(t.getMessage(), t), t);
		}
		finally
		{
			try
			{
				is.close();
			}
			catch(Throwable tClose){}
		}
	}

	public void publish(long groupId) throws Exception
	{
		if (writeLock.tryLock())
		{
			File localFile = null;
			
			try
			{
				// Se recupera la configuración de la publicación
				LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
				
				Document dom = exportData(groupId);
				Group group = GroupLocalServiceUtil.getGroup(groupId);
				String groupName = group.getName();
				dom.getRootElement().addAttribute("groupname", groupName);
				
				//Generamos el .zip a exportar
				localFile = generateExportFile(liveConf.getLocalPath(), groupId, dom.getRootElement());
				
				//Enviamos por FTP/File System el .zip generado
				XMLIOUtil.sendFile(liveConf, localFile);
				
				// Realizar la llamada al Live para que importe el .zip
				publishToLive("com.protecmedia.iter.news.service.MetadataControlServiceUtil", "importPublishedData", localFile.getName());
				
				// Actualizar la fecha de publicación de la imagen
				updateOgImagePubDate(groupId);
			}
			finally
			{
				writeLock.unlock();
				
				//Borramos el fichero de exportación
				PublishUtil.hotConfigDeleteFile(localFile);	
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
	}
	
	private void updateOgImagePubDate(long groupId) throws SystemException, DocumentException, ParseException
	{
		Metadata meta = MetadataLocalServiceUtil.getMetadataByMetadataStructure(groupId, IterKeys.STRUCTURE_ARTICLE);
		Document preferences = SAXReaderUtil.read( meta.getPreferences() );
		
		_log.debug("updateOgImagePubDate: " +  meta.getPreferences());
		
		Element ogImageNode = (Element) preferences.selectSingleNode("/rs/meta[@namevalue='" + MetadataControlUtil.META_OGIMG + "']");
		if( Validator.isNotNull(ogImageNode) )
		{
			if(_log.isDebugEnabled())
				_log.debug("updateOgImagePubDate ogImageNode: " + ogImageNode.asXML());
			
			Attribute modDateAttr = ogImageNode.attribute(MetadataControlUtil.ATTR__MODDATE);
			String modDateStr = modDateAttr.getValue();
			Attribute pubDateAttr = ogImageNode.attribute(MetadataControlUtil.ATTR__PUBDATE);
			String pubDateStr = pubDateAttr.getValue();
			
			boolean updateImgPubDate = true;
			if( Validator.isNotNull(pubDateStr) )
			{
				Date modDate = DateUtil.getDBFormat().parse( modDateStr );
				Date pubDate = DateUtil.getDBFormat().parse( pubDateStr );
				
				if(_log.isDebugEnabled())
					_log.debug("updateOgImagePubDate modDate: " + modDateStr + "  pubDate: " + pubDateStr);
				
				updateImgPubDate = modDate.getTime() > pubDate.getTime();
			}
			
			if(updateImgPubDate)
			{
				pubDateAttr.setValue( DateUtil.getDBFormat().format(new Date(System.currentTimeMillis())) );
			
				if(_log.isDebugEnabled())
					_log.debug("updateOgImagePubDate metadataPreferencesDom: " + preferences.asXML());
				
				meta.setPreferences( preferences.asXML() );
				MetadataLocalServiceUtil.updateMetadata(meta);
			}
		}
	}

	private File generateExportFile(String localPath, long scopeGroupId, Element rs) throws SecurityException, NoSuchMethodException, IOException, DocumentException, PortalException, SystemException, ParseException, ServiceError
	{
		File exportFile = null;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			String zipFileName = String.format(ITER_METADATA_ZIP_FILE_NAME, Calendar.getInstance().getTimeInMillis());
			ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter(new File(localPath + File.separatorChar + zipFileName));

			Node ogImageNode = rs.selectSingleNode("/rs/" + OG_IMAGE );
			String fileentryUUID = XMLHelper.getTextValueOf(ogImageNode, "text()", "");
			if( Validator.isNotNull(fileentryUUID) )
			{
				boolean publishOgImg = true;
				String modDateAttr = XMLHelper.getTextValueOf(ogImageNode, MetadataControlUtil.ATTR_MODDATE, "");
				String pubDateAttr = XMLHelper.getTextValueOf(ogImageNode, MetadataControlUtil.ATTR_PUBDATE, "");
				if(_log.isDebugEnabled())
					_log.debug("generateExportFile modDate: " + modDateAttr + "  pubDate: " + pubDateAttr);
				
				if( Validator.isNotNull(pubDateAttr) && Validator.isNotNull(modDateAttr) )
				{
					long modDate = DateUtil.getDBFormat().parse(modDateAttr).getTime();
					long pubDate = DateUtil.getDBFormat().parse(pubDateAttr).getTime();
					publishOgImg = modDate > pubDate;
				}
				
				if(publishOgImg)
					//Se añaden los binarios de las imágenes al .zip
					addOgImageToZIP(scopeGroupId, zipWriter, ogImageNode);
			}
			
			//Se añade el .xml de publicacion al .zip
			zipWriter.addEntry(IterKeys.XMLIO_XML_MAIN_FILE_NAME, rs.asXML());

			//Se obtiene el fichero liberado
			exportFile = PublishUtil.getUnlockedFile(zipWriter);
		}
		
		return exportFile;
	}
	
	public void addOgImageToZIP(Long scopeGroupId, ZipWriter zipWriter, Node ogNode) throws PortalException, SystemException, IOException, ServiceError
	{
		String fileEntryUUID = XMLHelper.getTextValueOf(ogNode, "text()", "");
		_log.debug("addOgImageToZIP fileEntryUUID: " + fileEntryUUID);
		
		if( Validator.isNotNull(fileEntryUUID) )
		{
			DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(scopeGroupId, getFolderId(scopeGroupId, GroupMgr.getDefaultUserId()), fileEntryUUID);
			long delegationId = GroupLocalServiceUtil.getGroup(scopeGroupId).getDelegationId();
			InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, dlfileEntry.getUserId(), dlfileEntry.getGroupId(), dlfileEntry.getFolderId(), dlfileEntry.getName());
			zipWriter.addEntry(dlfileEntry.getTitle(), IOUtils.toByteArray(is));
			is.close();
		}
	}

	private void publishToLive(String className, String methodName, String data) throws JSONException, ClientProtocolException, SystemException, SecurityException, IOException, NoSuchMethodException, ServiceError 
    {
          List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
          remoteMethodParams.add(new BasicNameValuePair("serviceClassName", className));
          remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", methodName));
          remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[fileName]"));
          remoteMethodParams.add(new BasicNameValuePair("fileName", data));
          
          JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
    }
	
	public String uploadDefaultOgImage(HttpServletRequest request, HttpServletResponse response, InputStream is, String groupId ) throws Exception
	{
		String result = "";
		
		@SuppressWarnings("unchecked")
		Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
		while (files.hasNext())
		{
	    	FileItem currentFile = files.next();
	    	if (!currentFile.isFormField())
	    	{
	    		is = currentFile.getInputStream();
	    		result = setFileEntry(groupId, currentFile.getName(), currentFile.getSize(), is) ;
	    		break;
	    	}
		}
		return result;
	}
	
	private String setFileEntry(String scopeGroupId, String fileName, long imgSize, InputStream is) throws DocumentException, Exception
	{
		ErrorRaiser.throwIfNull(fileName, 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(imgSize > 0, 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long groupId	= Long.valueOf(scopeGroupId);
		long userId 	= GroupMgr.getDefaultUserId();
		long folderId 	= getFolderId(groupId, userId);
		
		int indexPeriod = fileName.indexOf(StringPool.PERIOD);
		boolean hasExtension = indexPeriod > 0 && indexPeriod < fileName.length();
		ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			    		
		String imgExtension = fileName.substring(indexPeriod, fileName.length());
		
		String imgTitle	= SQLQueries.getUUID() + imgExtension;
		
		DLFileEntry dlfileEntry = createReplaceFileEntry(groupId, folderId, is, imgSize, imgTitle, fileName, userId, true);
		
		// Se pone por título el uuid de la imagen	
		dlfileEntry.setTitle( dlfileEntry.getUuid() + imgExtension );
		DLFileEntryUtil.update(dlfileEntry, false);
		
		Metadata meta = MetadataLocalServiceUtil.getMetadataByMetadataStructure(groupId, IterKeys.STRUCTURE_ARTICLE);

		Document metadataPreferencesDom = SAXReaderUtil.read( meta.getPreferences() );
		
		String modifiedDate = DateUtil.getDBFormat().format(new Date(System.currentTimeMillis()));
		String fileEntryTitle = dlfileEntry.getTitle();
		Element ogImageNode = (Element) metadataPreferencesDom.selectSingleNode("/rs/meta[@namevalue='" + MetadataControlUtil.META_OGIMG + "']");
		if( Validator.isNull(ogImageNode) )
		{
			ogImageNode = metadataPreferencesDom.getRootElement().addElement(META);
			ogImageNode.addAttribute(MetadataControlUtil.ATTR__NAMEVALUE, MetadataControlUtil.META_OGIMG);
			ogImageNode.addAttribute(FIELD,     						  MetadataControlUtil.DEFINED);
			ogImageNode.addAttribute(MetadataControlUtil.DEFINED,   	  fileEntryTitle);		
			ogImageNode.addAttribute(MetadataControlUtil.CUSTOM,    	  "");
			ogImageNode.addAttribute(MetadataControlUtil.ATTR__MODDATE,   modifiedDate);
			ogImageNode.addAttribute(MetadataControlUtil.ATTR__PUBDATE,   "");
		}
		else
		{
			Attribute definedAttr = ogImageNode.attribute(MetadataControlUtil.ATTR__DEFINED);
			definedAttr.setValue( fileEntryTitle );
			Attribute modifiedDateAttr = ogImageNode.attribute(MetadataControlUtil.ATTR__MODDATE);
			modifiedDateAttr.setValue( modifiedDate );
		}
		
		if(_log.isDebugEnabled())
			_log.debug("setFileEntry metadataPreferencesDom: " + metadataPreferencesDom.asXML());
		
		meta.setPreferences( metadataPreferencesDom.asXML() );
		MetadataLocalServiceUtil.updateMetadata(meta);
		
		Document dom = SAXReaderUtil.createDocument();
		Element rs =  dom.addElement("rs");
		Element row = rs.addElement("row");
		row.addAttribute( "fileentrytitle", fileEntryTitle );
		row.addAttribute( "modifieddate", modifiedDate );
		String path = DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(groupId, folderId, dlfileEntry.getTitle()).concat("?env=preview"); 
		row.addAttribute("path", path);
		String result = dom.asXML();
		
		if(_log.isDebugEnabled())
			_log.debug("setFileEntry result: " + result);
		
		return result;
	}
	
	private DLFileEntry createReplaceFileEntry(long groupId, long folderId, InputStream is, long imgSize, String imgTitle, String fileName, long userId, boolean updtIfExist) throws PortalException, SystemException, ServiceError
	{
		if(_log.isDebugEnabled())
			_log.debug(String.format("createReplaceFileEntry groupId: %s folderId: %s imgSize: %s imgTitle: %s fileName: %s", groupId, folderId, imgSize, imgTitle, fileName));
		
		List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(groupId, folderId);
		
		DLFileEntry dlfileEntry = null;
		if (imgList.size() > 0)
		{
			DLFileEntry oldDlfileEntry = imgList.get(0);
			ErrorRaiser.throwIfFalse( updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_METADA, oldDlfileEntry.getUuid()));
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, imgTitle, fileName, StringPool.BLANK, StringPool.BLANK, is, imgSize, new ServiceContext());
			DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(oldDlfileEntry);
		}
		else
		{
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, imgTitle, fileName, StringPool.BLANK, StringPool.BLANK, is, imgSize, new ServiceContext());
		}
		
		return dlfileEntry;
	}
	
	private long getFolderId(long groupId, long userId) throws PortalException, SystemException, ServiceError
	{
	    DLFolder dlFolder = null;
	    
	    try
	    {
	    	dlFolder = DLFolderLocalServiceUtil.getFolder(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, MetadataControlUtil.OG_DLFILENTRY_FOLDER);
	    }
	    catch (NoSuchFolderException nsfe)
	    {
	    	_log.debug("Creating " + MetadataControlUtil.OG_DLFILENTRY_FOLDER + " folder...");
	    	dlFolder = DLFolderLocalServiceUtil.addFolder(userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, MetadataControlUtil.OG_DLFILENTRY_FOLDER, StringPool.BLANK, new ServiceContext());
	    }
	    
	    ErrorRaiser.throwIfNull(dlFolder);
	    
	    return dlFolder.getFolderId();
	}
	
	public DLFileEntry getDefaultOgImage(long scopeGroupId) throws PortalException, SystemException, ServiceError
	{
		DLFileEntry defaultOgImage = null;
		
		long userId 	= GroupMgr.getDefaultUserId();
		long folderId 	= getFolderId(scopeGroupId, userId);
		
		_log.debug("getDefaultOgImage scopeGroupId: " + scopeGroupId + " folderId: "+ folderId);
		
		List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(scopeGroupId, folderId);
		if (imgList.size() > 0)
			defaultOgImage = imgList.get(0);
		else
			_log.debug("There are not files.");
		
		return defaultOgImage;
	}
}
