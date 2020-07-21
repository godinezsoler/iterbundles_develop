/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.journal;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ABTestingMgr;
import com.liferay.portal.kernel.util.ABTestingMgr.Operation;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Image;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetEntryUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.BinaryRepositoryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.expando.service.persistence.ExpandoRowUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalArticleConstants;
import com.liferay.portlet.journal.model.JournalArticleImage;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.persistence.JournalArticleImageUtil;
import com.liferay.portlet.journal.service.persistence.JournalArticleUtil;
import com.liferay.portlet.journal.util.JournalArticleTools;
import com.liferay.restapi.resource.user.RestApiUserFavoriteUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.apache.URIPte;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetEntryXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoRowXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoValueXmlIO;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;
import com.protecmedia.iter.xmlio.service.util.JournalArticleAnnotationThread;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/* XML example
 * 
<item operation="create" globalid="DiariTerrassaPre_316716" classname="com.liferay.portlet.journal.model.JournalArticle" groupid="The Star">
	<param name="template">&lt;![CDATA[CABECERA-DT-SUSCRIPCION]]&gt;</param>
	<param name="structure">&lt;![CDATA[STANDARD-ARTICLE]]&gt;</param>
	<param name="articleid">&lt;![CDATA[316716]]&gt;</param>
	<param name="indexable">&lt;![CDATA[true]]&gt;</param>
	<param name="expirationdate">&lt;![CDATA[]]&gt;</param>
	<param name="reviewdate">&lt;![CDATA[]]&gt;</param>
	<param name="type">&lt;![CDATA[general]]&gt;</param>
	<param name="url-title">&lt;![CDATA[nuevorrrr]]&gt;</param>
	<param name="displaydate">&lt;![CDATA[]]&gt;</param>
	<param name="version">&lt;![CDATA[1.0]]&gt;</param>
	<param name="title">&lt;![CDATA[nuevorrrr]]&gt;</param>
	<param name="file">&lt;![CDATA[/contents/316716.xml]]&gt;</param>
	<param name="description">&lt;![CDATA[]]&gt;</param>
	<param name="small-image-url">&lt;![CDATA[]]&gt;</param>
	<param name="small-image">&lt;![CDATA[false]]&gt;</param>
	<param name="small-image-file">&lt;![CDATA[]]&gt;</param>
	<param name="DiariTerrassaPre_316720" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.expando.model.ExpandoRow]]&gt;</param>
		<param name="groupname">&lt;![CDATA[Global]]&gt;</param>
	</param>
	<param name="DiariTerrassaPre_316703" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.documentlibrary.model.DLFileEntry]]&gt;</param>
		<param name="groupname">&lt;![CDATA[The Star]]&gt;</param>
	</param>
</item>
*/

public class JournalArticleXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(JournalArticleXmlIO.class);
	private String _className = IterKeys.CLASSNAME_JOURNALARTICLE;
	private boolean _isValid = true;
	
	public static final String GET_GLOBAL_PRODUCTS_BY_ARTICLEID = "SELECT l.globalId FROM Xmlio_Live l\n" +
														   		  "INNER JOIN ArticleProduct ap ON l.localId=ap.productId\n" + 
														   		  "WHERE l.classnamevalue='" + IterKeys.CLASSNAME_PRODUCT + "'\n" +
														   		  "AND ap.articleId='%s'";
	
	public static final String DELETE_ARTICLE_PRODUCTS = "DELETE FROM ArticleProduct\n" +
														 "WHERE articleId='%s'";
	
	public static final String INSERT_ARTICLE_PRODUCTS = "INSERT INTO ArticleProduct\n" + 
														 "SELECT l.localId, '%s' FROM Xmlio_Live l\n" + 
														 "WHERE l.classnamevalue='" + IterKeys.CLASSNAME_PRODUCT + "'\n" +
														 "AND l.globalId IN %s";
	
	private static final String GET_PRODUCTS_LOCAL_ID = "SELECT localId FROM Xmlio_Live WHERE globalId IN (%s)";
	
	private static final String GET_DELETE_PENDING_BINARIES_BY_ARTICLE_ID = "SELECT binaryId from binariesdeleted WHERE articleID = '%s'";
	
	public static final String INSERT_URIPTES = new StringBuilder(
		"INSERT INTO uriptes (groupId, uripte)		\n").append(
		"	VALUES	(%1$d, '%2$s'), 				\n").append(
		"			(%1$d, '%3$s')				\n").append(
		"	ON DUPLICATE KEY UPDATE groupId=groupId	\n").toString();
		
	public JournalArticleXmlIO() {
		super();
	}
	
	public JournalArticleXmlIO(XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}
	
	@Override 
	public String getClassName(){
		return _className;
	}

	/*
	 * Live Functions
	 */
	@Override
	public void populateLive(long groupId, long companyId)
			throws SystemException 
	{
		try
		{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			//A. Global group	
			List<JournalArticle> globalArticleList = JournalArticleLocalServiceUtil.getArticles(globalGroupId);
			for (JournalArticle globalArticle : globalArticleList)
			{			
				try 
				{
					//1. Populate globalArticle's AssetEntry
					try 
					{
						AssetEntry assetEntry = AssetEntryUtil.fetchByC_C(ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_JOURNALARTICLE), globalArticle.getResourcePrimKey());
						if (assetEntry != null)
						{
							ItemXmlIO entryItem = new AssetEntryXmlIO();
							entryItem.createLiveEntry(assetEntry);
						}
					} 
					catch (PortalException e) 
					{
						_log.error("Can't add Live, AssetEntry, article: " + globalArticle.getArticleId());
					}	
					
					//2. Populate globalArticle's ExpandoRow					
					List<ExpandoTable> expandoTableList = ExpandoTableLocalServiceUtil.getTables(companyId, IterKeys.CLASSNAME_JOURNALARTICLE);
					ItemXmlIO expandoRowItem = new ExpandoRowXmlIO();
					for (ExpandoTable expandoTable : expandoTableList)
					{	
						ExpandoRow expandoRow = ExpandoRowUtil.fetchByT_C(expandoTable.getTableId(), globalArticle.getPrimaryKey());
						if (expandoRow != null)
						{
							try 
							{
								expandoRowItem.createLiveEntry(expandoRow);
							} 
							catch (PortalException e) 
							{
								_log.error("Can't add Live, ExpandoRow: " + expandoRow.getRowId());
							}					
							
							//3. Populate globalArticle's ExpandoValue
							List<ExpandoValue> expandoValueList = ExpandoValueLocalServiceUtil.getRowValues(expandoRow.getRowId());
							ItemXmlIO expandoValueItem = new ExpandoValueXmlIO();
							for (ExpandoValue expandoValue : expandoValueList)
							{				
								try
								{
									expandoValueItem.createLiveEntry(expandoValue);
								} 
								catch (PortalException e) 
								{
									_log.error("Can't add Live, ExpandoValue: " + expandoValue.getValueId());
								}
							}
						}
					}
					
					//4. Populate globalArticle
					createLiveEntry(globalArticle);			
				} 
				catch (Exception e) 
				{
					_log.error("Can't add Live, JournalArticle: " + globalArticle.getArticleId());
				}
			}
		} 
		catch (PortalException e) 
		{
			_log.error("Can't add Live, JournalArticles from Global group ");
		}
		
		//B. Current group
		List<JournalArticle> articleList = JournalArticleLocalServiceUtil.getArticles(groupId);
		for (JournalArticle article : articleList)
		{		
			try 
			{			
				//1. Populate article's AssetEntry
				try 
				{
					AssetEntry assetEntry = AssetEntryUtil.fetchByC_C(ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_JOURNALARTICLE), article.getResourcePrimKey());
					if (assetEntry != null)
					{
						ItemXmlIO entryItem = new AssetEntryXmlIO();
						entryItem.createLiveEntry(assetEntry);
					}
				} 
				catch (PortalException e) 
				{
					_log.error("Can't add Live, AssetEntry, article: " + article.getArticleId());
				}	
				
				//2. Populate article's ExpandoRow					
				List<ExpandoTable> expandoTableList = ExpandoTableLocalServiceUtil.getTables(companyId, IterKeys.CLASSNAME_JOURNALARTICLE);
				ItemXmlIO expandoRowItem = new ExpandoRowXmlIO();
				for (ExpandoTable expandoTable : expandoTableList)
				{
					ExpandoRow expandoRow = ExpandoRowUtil.fetchByT_C(expandoTable.getTableId(), article.getPrimaryKey());
					if (expandoRow != null)
					{
						try 
						{
							expandoRowItem.createLiveEntry(expandoRow);
						} 
						catch (PortalException e) 
						{
							_log.error("Can't add Live, ExpandoRow: " + expandoRow.getRowId());
						}					
						
						//3. Populate article's ExpandoValue
						List<ExpandoValue> expandoValueList = ExpandoValueLocalServiceUtil.getRowValues(expandoRow.getRowId());
						ItemXmlIO expandoValueItem = new ExpandoValueXmlIO();
						for (ExpandoValue expandoValue : expandoValueList)
						{				
							try 
							{
								expandoValueItem.createLiveEntry(expandoValue);
							} 
							catch (PortalException e) 
							{
								_log.error("Can't add Live, ExpandoValue: " + expandoValue.getValueId());
							}
						}
					}
				}	
				
				//1. Populate article
				createLiveEntry(article);		
			} 
			catch (Exception e) 
			{
				_log.error("Can't add Live, JournalArticle: " + article.getArticleId());
			}
		}
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		JournalArticle article 	= (JournalArticle)model;
		String id  				= article.getArticleId();

		String status = LiveLocalServiceUtil.canSetJournalArticleToDraft(article) ? IterKeys.DRAFT : IterKeys.PENDING;
		
		// Añade JournalArticle
		Live liveArticle = LiveLocalServiceUtil.add(_className, article.getGroupId(), -1, 0, 
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.CREATE, status, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
		// Dependencias del artículo
		if (PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			try
			{
				updateExpandoPool( article );
			}
			catch(Exception err)
			{
				_log.debug("Expando data could not be added as dependency of article " + id);
			}
				
			try
			{
				// Actualiza el pool en el AssetEntry del JournalArticle
				AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(IterKeys.CLASSNAME_JOURNALARTICLE, article.getResourcePrimKey());
				Live liveEntry = LiveLocalServiceUtil.getLiveByLocalId(assetEntry.getGroupId(), IterKeys.CLASSNAME_ENTRY, String.valueOf(assetEntry.getEntryId()));
				LivePoolLocalServiceUtil.createLivePool(liveArticle.getId(), liveArticle.getId(), liveEntry.getId(), false);
			}
			catch(Exception err)
			{
				_log.debug("AssetEntry data could not be added as dependency of article " + id);
			}			
			
			List<DLFileEntry> dlfeList 	= XMLIOUtil.getWebContentFileEntries(article);
			Document dom 				= null;
			try 
			{
				dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SQLQueries.SEL_FILEENTRIES_ID, article.getArticleId()) );
			} 
			catch (Exception e) 
			{
				throw new SystemException(e);
			} 
			
			// Solo se añaden aquellos que previamente NO existian
			for (DLFileEntry dlfe : dlfeList)
			{
				if ( dom.selectSingleNode("/rs/row[@fileEntryId="+Long.toString(dlfe.getFileEntryId())+"]") == null)
				{
					try
					{
						// Se crea la asociación del fileEntry con el JournalArticle				
						Live liveDLFE = LiveLocalServiceUtil.getLiveByLocalId(dlfe.getGroupId(), IterKeys.CLASSNAME_DLFILEENTRY, String.valueOf(dlfe.getFileEntryId()));
						LivePoolLocalServiceUtil.createLivePool(liveArticle.getId(), liveArticle.getId(), liveDLFE.getId(), false);
					}
					catch(Exception err)
					{
						_log.error("File " + dlfe.getTitle() + " could not be added as dependency of article " + id, err);
					}
                }
			}	
		}
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		JournalArticle article  = (JournalArticle)model;
		boolean deleteJustNow 	= isArchivableArticle(article.getArticleId());
		
		deleteLiveEntry(model, deleteJustNow);
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException
	{
		JournalArticle article  = (JournalArticle)model;
		String id 				= article.getArticleId();
		
		Live live = LiveLocalServiceUtil.getLiveByLocalId(article.getGroupId(), _className, id);
		
		// Como es un borrado si no existe NO se creará
		if (live != null)
		{
			_log.trace( getTraceDeleteLiveEntry(live.getGlobalId(), deleteJustNow) );
			
			if (deleteJustNow)
			{
				LiveLocalServiceUtil.deleteLive(live.getGroupId(), live.getClassNameValue(), live.getGlobalId());
			}
			else
			{
				String environment 	= IterLocalServiceUtil.getEnvironment();
				String status 		= environment.equals(IterKeys.ENVIRONMENT_PREVIEW) ? IterKeys.PENDING : IterKeys.DONE;

				LiveLocalServiceUtil.add(live.getClassNameValue(), live.getGroupId(),
						live.getGlobalId(), id, IterKeys.DELETE, status, new Date(), environment);			
			}
		}
	}
	

	
	@Override
	public void updateStatusLiveEntry(BaseModel<?> model, String status) throws PortalException, SystemException
	{
		JournalArticle article 	= (JournalArticle)model;
		String globalId 		= IterLocalServiceUtil.getSystemName() + "_" + article.getArticleId();
		
		Live live = LiveLocalServiceUtil.getLiveByGlobalId(article.getGroupId(), _className, globalId);
		if (live != null)
			LiveLocalServiceUtil.updateLive(live, IterKeys.UPDATE, IterKeys.PENDING, new Date(), -1, -1, false);
	}
	
	/*
	 * Export Functions
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		
		StringBuilder error = new StringBuilder();
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		setCommonAttributes(attributes, group.getName(), live, operation);
			
		//Put necessary parameters for each kind of operation.
		if (operation.equals(IterKeys.CREATE))
		{
			JournalArticle content = null;
			try {
				content = JournalArticleLocalServiceUtil.getArticle(group.getGroupId(), live.getLocalId());
			
				String fileName = "/contents/" + content.getArticleId().toLowerCase() + ".xml";
				
				params.put("articleid", content.getArticleId());
				params.put("template", content.getTemplateId());
				params.put("structure", content.getStructureId());
				params.put("version", String.valueOf(content.getVersion()));
				params.put("file", fileName);
				params.put("title", content.getTitle());
				params.put("url-title", content.getUrlTitle());
				params.put("description", content.getDescription());	
				params.put("type", content.getType());
				
				params.put("createdate", 		content.getCreateDate() 	!= null ? df.format(content.getCreateDate()) 	: "" );
				params.put("modifieddate", 		content.getModifiedDate() 	!= null ? df.format(content.getModifiedDate()) 	: "" );
				params.put("displaydate", 		content.getDisplayDate() 	!= null ? df.format(content.getDisplayDate()) 	: "" );	
				params.put("expirationdate", 	content.getExpirationDate() != null ? df.format(content.getExpirationDate()): "" );	
				
				params.put("indexable",  		String.valueOf(content.getIndexable()));	
				params.put("small-image-url", 	content.getSmallImageURL());
				params.put("small-image", 		String.valueOf(content.getSmallImage()));		
				params.put("small-image-id", 	String.valueOf(content.getSmallImageId()) );
				
				params.put(IterKeys.XMLIO_XML_PRODUCTS, getGlobalProductsString(content.getArticleId()));	
					
				Element itemElement = addNode(root, "item", attributes, params);
			
				try{
					List<JournalArticleImage> imagesArticle = JournalArticleImageUtil.findByG_A_V(group.getGroupId(), content.getArticleId(), content.getVersion());				
					
					for (JournalArticleImage image : imagesArticle) {
						try {
							Map<String, String> imageAttributes = new HashMap<String, String>();
							
							Image img = ImageLocalServiceUtil.getImage(image.getArticleImageId());
							
							String imageFile = "/contents/images/img_" + img.getImageId() + "." + img.getType();						
							xmlioExport.addResource(imageFile,  img.getTextObj());
							
							String imageName = image.getElInstanceId() + "_" + image.getElName();
							
							imageAttributes.put("type", "image");				
							imageAttributes.put("name", imageName);
							
							addNode(itemElement, "param", imageAttributes, imageFile);
						} catch (Exception e) {	
							//Este error no se captura ya que se debe a imagenes en desuso.
							//error.append((error.length()==0?"":";") + "Cannot export image " + image.getArticleImageId());
						}
					}
				} catch (Exception e) {
					//Este error no se captura ya que se debe a imagenes en desuso.
					//error.append("Cannot find images");
				}
				String c = transformLocalToGlobal(content, xmlioExport, itemElement) ;
			
				xmlioExport.addResource(fileName, c.getBytes());
				
				addDependencies(itemElement , live.getId());
				
			} 
			catch (Exception e1) 
			{
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");	
				_log.error(e1);
				error.append("Cannot export item: ").append(e1.toString());
			}
		}
		else if(operation.equals(IterKeys.UPDATE))
		{
			try
			{
				JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(group.getGroupId(), live.getLocalId());
				params.put("articleid", journalArticle.getArticleId());
				Element itemElement = addNode(root, "item", attributes, params);

				addDependencies(itemElement , live.getId());
				
				// Añade los cambios en los binarios del artículo.
				try
				{
					// Recupera los binarios referenciados por el artículo.
					Document content = SAXReaderUtil.read(journalArticle.getContent());
					String xPath = "//dynamic-element[@type='document_library' and starts-with(dynamic-content, '/binrepository/')]/dynamic-content";
					List<Node> nodes = content.selectNodes(xPath);
					List<String> articleBinaries = new ArrayList<String>();
					for (Node node : nodes)
					{
						String fileurl = node.getText();
						String[] str = fileurl.split("/");
						articleBinaries.add(str[2]);
					}
					String delegationId = String.valueOf(journalArticle.getDelegationId());
					// Añade los binarios nuevos / actualizados
					addBinariesToXML(articleBinaries, delegationId, xmlioExport, itemElement);
					// Añade los binarios pendientes de borrar
					addDeletePendingBinariesToXML(live.getLocalId(), delegationId, xmlioExport, itemElement);
				}
				catch (Throwable th)
				{
					_log.error(th);
					error.append("Cannot export article binaries: ").append(th.toString());
				}
			}
			catch (Throwable th1) 
			{
				_log.error(th1);
				error.append("Cannot export item: ").append(th1.toString());
			}
		}
		else
		{
			addNode(root, "item", attributes, params);		
		}		
		
		_log.debug("Content exported: " + live.getLocalId());
		
		return error.toString();
	}	

	/*
	 * Import Functions
	 */	
	@Override
	protected void delete(Element item) 
	{
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
						
		try
		{
			long groupId = getGroupId(sGroupId);
			
			try
			{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			
				try 
				{
					JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(groupId, live.getLocalId());
					
					try
					{
						// El ItemXMLIO correspondiente se borra al borrar el JournalArticle
						// La lógica del borrado la tiene el BACK, si al LIVE llega un borrado se borra
						JournalArticleLocalServiceUtil.deleteArticle(journalArticle, null, null);
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);					
					} 
					catch (Exception e) 
					{				
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
					}
				} 
				catch (Exception e) 
				{	
					if (live != null)
					{
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);
				}
			} 
			catch (Exception e) 
			{				
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}
		} 
		catch (Exception e)
		{
			xmlIOContext.itemLog.addMessage(item, globalId,_className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}	
	
	@Override
	protected void modify(Element item, Document doc) 
	{
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar now = Calendar.getInstance();	
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");
		
		_log.trace("\n");
		_log.trace("JArticle: Begin");
		
		double version 		= GetterUtil.getDouble(getParamTextByName(item, "version"), JournalArticleConstants.DEFAULT_VERSION);			
		String articleId 	= getParamTextByName(item, "articleid");
		String file 		= getParamTextByName(item, "file");
		String structure 	= getParamTextByName(item, "structure");
		String template 	= getParamTextByName(item, "template");
		boolean acceptComments = GetterUtil.getInteger( getParamTextByName(item, "small-image-id"), 1) == 0 ? false : true;
		String smallImageUrl= getParamTextByName(item, "small-image-url");
		String title 		= getParamTextByName(item, "title");
		String urlTitle 	= getParamTextByName(item, "url-title");
		String legacyUrl 	= getParamTextByName(item, "legacy-url");
		String description 	= getParamTextByName(item, "description");
		String type 		= GetterUtil.getString(getParamTextByName(item, "type"), "general");
		
		Date createdate 	= GetterUtil.getDate(getParamTextByName(item, 	"createdate"), 		df, now.getTime());
		Date modifieddate 	= GetterUtil.getDate(getParamTextByName(item, 	"modifieddate"), 	df, now.getTime());
		Date displayDate 	= GetterUtil.getDate(getParamTextByName(item, 	"displaydate"), 	df, now.getTime());
		Date expirationDate = GetterUtil.getDate(getParamTextByName(item, 	"expirationdate"), 	df, now.getTime());
		
		Date lastPublication 	= GetterUtil.getDate(now.getTime(), df );	
		
		long delegationId	= -1;

		//boolean indexable 	= GetterUtil.getBoolean(getParamTextByName(item,"indexable"), true);
		boolean indexable = false;
		boolean smallImage 	= GetterUtil.getBoolean(getParamTextByName(item,"small-image"), false);
		List<String[]> imageList = getParamListByType(item, "image");
		
		_log.trace("JArticle: After extract params");
		try
		{
			long groupId = getGroupId(sGroupId);
			
			Map<String, byte[]> images = new HashMap<String, byte[]>();
			
			for (String[] imagen : imageList) 
			{					
				
				byte[] bytes = XMLIOImport.getFileAsBytes(imagen[1]);

				if ((bytes != null) && (bytes.length > 0)) 
				{
					images.put(imagen[0], bytes);
				}
				
			}	
			_log.trace("JArticle: After extract images");
			
			//--- Service context
			ServiceContext serviceContext = new ServiceContext();					
			serviceContext.setAddCommunityPermissions(acceptComments);
			serviceContext.setAddGuestPermissions(true);
			serviceContext.setScopeGroupId(groupId);
			//---
			
			//--- Dates
			TimeZone tz = TimeZone.getDefault ();
			//TimeZone tz = TimeZone.getTimeZone(SystemProperties.get("user.timezone"))
			Calendar dd = Calendar.getInstance (tz);
			dd.setTime (displayDate);
			
			Calendar ed = Calendar.getInstance (tz);
			ed.setTime (expirationDate);
			
			Calendar pd = Calendar.getInstance ();
			pd.setTime (lastPublication);
			//---   			
			_log.trace("JArticle: After time settings");
			
			try 
			{
				
				// Get live to get the element localId
				// Durante la actualización NO se utiliza el objeto live por si NO existe el live pero sí el JournalArticle
				// ITER-403	No se puede publicar un artículo ya que da error de id duplicado
				// De esta forma si existe el JournalArticle y no el Live se actualizará el artículo y se creará eñ registro del XmlIO_Live
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				_log.trace("JArticle: After get LiveByGlobalId");
				try
				{	
					JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
					version = journalArticle.getVersion();
					
					//---UPDATE---------------------	
					try
					{
						String content = transformGlobalToLocalURL(XMLIOImport.getFileAsString(file, "UTF-8"));
						_log.trace("JArticle: After transformGlobalToLocal");
						
						// Si están activadas las estadísticas de visitas, se queda con el contenido antiguo para comprobar tras
						// la actualización si hay que insertar una anotación automática.
						String oldContent = (PropsValues.ITER_STATISTICS_ENABLED) ? journalArticle.getContent() : null;

						journalArticle = JournalArticleLocalServiceUtil.updateArticle(xmlIOContext.getUserId(), groupId, articleId, version, title, 
								description, content, type, structure, template, dd.get(Calendar.MONTH), dd.get(Calendar.DAY_OF_MONTH), 
								dd.get(Calendar.YEAR), dd.get(Calendar.HOUR_OF_DAY), dd.get(Calendar.MINUTE), 
								ed.get(Calendar.MONTH), ed.get(Calendar.DAY_OF_MONTH), ed.get(Calendar.YEAR), 
								ed.get(Calendar.HOUR_OF_DAY), ed.get(Calendar.MINUTE), true, 
								pd.get(Calendar.MONTH), pd.get(Calendar.DAY_OF_MONTH), pd.get(Calendar.YEAR), 
								pd.get(Calendar.HOUR_OF_DAY), pd.get(Calendar.MINUTE), false, indexable, smallImage, 
								smallImageUrl, null, images, StringPool.BLANK, serviceContext);
						
						journalArticle.setCreateDate(createdate);
						journalArticle.setModifiedDate(modifieddate);
						journalArticle.setDisplayDate(displayDate);
						journalArticle.setStatus(WorkflowConstants.STATUS_APPROVED);
						journalArticle.setUrlTitle(urlTitle);
						
						delegationId = journalArticle.getDelegationId();
						_log.trace("JArticle: After updateArticle");
						
						//Persists changes
						JournalArticleUtil.update(journalArticle, true);
						_log.trace("JArticle: After update(journalArticle, true)");

						if(legacyUrl!=null && !legacyUrl.isEmpty() && xmlIOContext.getGroupId() > 0)
						{
							PortalLocalServiceUtil.executeUpdateQuery( String.format(SQLQueries.UPDATE_LEGACYURL,legacyUrl.toLowerCase().replaceAll("'", "''"), xmlIOContext.getGroupId(),journalArticle.getArticleId()) );
						}
/*
						journalArticle = JournalArticleLocalServiceUtil.updateStatus(xmlIOContext.getUserId(), 
								groupId, journalArticle.getArticleId(),	journalArticle.getVersion(),  
								WorkflowConstants.STATUS_APPROVED, urlTitle, serviceContext);
						_log.trace("JArticle: After updateStatus");
*/						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								articleId, IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
				
						_log.trace("JArticle: After add");

						// Importación de los binarios
						importBinaries(item, IterKeys.UPDATE);
						_log.trace("JArticle: After binaries import");
						
						//Comprobamos las dependencias
						//RENDIMIENTO!!!!!!!!!!
						try 
						{
							_log.trace("JArticle: Before evaluateDependencies");
							boolean bDependencies = evaluateDependencies(item, doc);
							_log.trace("JArticle: After evaluateDependencies");
							
							//Creamos/modificamos sus dependencias	
							if (!bDependencies )
							{		
								//Visibility: change entry in live table
								LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT); 
								_log.trace("JArticle: After updateStatus");
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);
								_log.trace("JArticle: After itemLog.addMessage");
							}
							else
							{	
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
								// Añade las publicaciones programadas al log de resultado
								logScheduledPublications(journalArticle);
								_log.trace("JArticle: After itemLog.addMessage");
							}
						} 
						catch (DocumentException err) 
						{
							LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT); 
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
						}
						
						// Inserta una anotación en las estadísticas del artículo
						if (oldContent != null)
						{
							new JournalArticleAnnotationThread(articleId, journalArticle.getScopeGroupIds(), oldContent, content).start();
						}
					} 
					catch (Exception ex) 
					{
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + ex.toString(), IterKeys.ERROR, sGroupId);
					}
					//------------------------
				} 
				catch (Exception e)
				{
					if (live == null || !live.getOperation().equals(IterKeys.DELETE))
					{
						//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
						if (live != null)
						{
							LiveLocalServiceUtil.deleteLiveById(live.getId());
						}
						
						//----CREATE-----------------					
						try 
						{
							_log.trace("JArticle: After catch!!!");
							String content = transformGlobalToLocalURL(XMLIOImport.getFileAsString(file, "UTF-8"));		
							_log.trace("JArticle: After transformGlobalToLocal");

							JournalArticle journalArticle =	JournalArticleLocalServiceUtil.addArticle(xmlIOContext.getUserId(), groupId, articleId, false, version, title, 
									description, content, type, structure, template, dd.get(Calendar.MONTH), dd.get(Calendar.DAY_OF_MONTH), 
									dd.get(Calendar.YEAR), dd.get(Calendar.HOUR_OF_DAY), dd.get(Calendar.MINUTE), 
									ed.get(Calendar.MONTH), ed.get(Calendar.DAY_OF_MONTH), ed.get(Calendar.YEAR), 
									ed.get(Calendar.HOUR_OF_DAY), ed.get(Calendar.MINUTE), true, 
									pd.get(Calendar.MONTH), pd.get(Calendar.DAY_OF_MONTH), pd.get(Calendar.YEAR), 
									pd.get(Calendar.HOUR_OF_DAY), pd.get(Calendar.MINUTE), false, indexable, smallImage, 
									smallImageUrl, null, images, StringPool.BLANK, serviceContext);
							
							journalArticle.setCreateDate(createdate);
							journalArticle.setModifiedDate(modifieddate);
							journalArticle.setDisplayDate(displayDate);
							journalArticle.setStatus(WorkflowConstants.STATUS_APPROVED);
							journalArticle.setUrlTitle(urlTitle);
							delegationId = journalArticle.getDelegationId();
							_log.trace("JArticle: After addArticle");
							
							//Persists changes
							JournalArticleUtil.update(journalArticle, false);

							_log.trace("JArticle: After update(journalArticle, false)");

							if(legacyUrl!=null && !legacyUrl.isEmpty() && xmlIOContext.getGroupId() > 0)
							{
								PortalLocalServiceUtil.executeUpdateQuery( String.format(SQLQueries.INSERT_LEGACYURL, legacyUrl.toLowerCase().replaceAll("'", "''"), xmlIOContext.getGroupId(), journalArticle.getArticleId()) );
							}
/*
							journalArticle = JournalArticleLocalServiceUtil.updateStatus(xmlIOContext.getUserId(), 
									groupId, journalArticle.getArticleId(),	journalArticle.getVersion(),  
									WorkflowConstants.STATUS_APPROVED, urlTitle, serviceContext);			
							_log.trace("JArticle: After updateStatus");
*/							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, globalId, journalArticle.getArticleId(), 
													 IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							_log.trace("JArticle: After add");

							// Importación de los binarios
							importBinaries(item, IterKeys.CREATE);
							_log.trace("JArticle: After binaries import");
							
							try
							{
								//Update globalId.
								LiveLocalServiceUtil.updateGlobalId(groupId, _className, journalArticle.getArticleId(), globalId);
								_log.trace("JArticle: After updateGlobalId");
							}
							catch(Exception e3)
							{
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, sGroupId);
							}
							
							//Comprobamos las dependencias
							try 
							{
								_log.trace("JArticle: Before evaluateDependencies");
								boolean bDependencies = evaluateDependencies(item, doc);
								_log.trace("JArticle: After evaluateDependencies");

								//Creamos/modificamos sus dependencias	
								if (! bDependencies)
								{		
									//Visibility: change entry in live table
									LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);	
									_log.trace("JArticle: After updateStatus");
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);
									_log.trace("JArticle: After itemLog.addMessage");
								}
								else
								{	
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
									// Añade las publicaciones programadas al log de resultado
									logScheduledPublications(journalArticle);
									_log.trace("JArticle: After itemLog.addMessage");
								}
							} 
							catch (DocumentException err) 
							{
								//Visibility: change entry in live table
								LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);								
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
							}
							
							// Inserta los artículos sugeridos
							insertPendingArticle(item);
						} 
						catch (Exception ex)
						{
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: "+ ex.toString(), IterKeys.ERROR, sGroupId);
						}
						//------------------------
					}
				}
				
			} 
			catch (Exception e) 
			{
				xmlIOContext.itemLog.addMessage(item, globalId,_className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);		
			}
			
			checkABTesting(item, delegationId);
		}
		catch (Exception e)
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
		
		// Se ha modificado el artículo, se añade la URL canónica y la móvil como URIPtes
		insertURIPtes(item);
		
		_log.trace("JArticle: Before deleteInsertProducts");
		
		//Comprobar las asociaciones Product/JournalArticle
		String[] products = splitComa(getParamTextByName(item, IterKeys.XMLIO_XML_PRODUCTS));
		deleteInsertProducts(articleId, products);
		
		_log.trace("JArticle: End\n");
		_log.trace("\n");
	}
	
	private void insertPendingArticle(Element item)
	{
		// Sólo para el LIVE
		if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			// Si no falló la publicación del artículo
			String globalId = getAttribute(item, "globalid");
			String sGroupId = getAttribute(item, "groupid");
			if (validateLog(sGroupId, globalId))
			{
				String articleId = getParamTextByName(item, "articleid");
				
				RestApiUserFavoriteUtil.insertPendingArticle(articleId);
			}
		}
	}
	
	private void checkABTesting(Element item, long delegationId) throws Exception
	{
		_log.trace("checkABTesting: Start");
		
		String globalId = getAttribute(item, "globalid");
		String sGroupId = getAttribute(item, "groupid");		

		// Si no ha fallado se analiza
		if (ABTestingMgr.canCRUD(xmlIOContext.getGroupId()) && validateLog(sGroupId, globalId))
		{
			String articleId = getParamTextByName(item, "articleid");
			if (!ABTestingMgr.crud(xmlIOContext.getGroupId(), articleId).equals(Operation.NONE))
				item.addAttribute("reloadTypConAlternative", String.valueOf(delegationId));
		}
		
		_log.trace("JArticle: Before deleteInsertProducts");
	}
	private void insertURIPtes(Element item)
	{
		// Se comprueba el entorno para descartar la importación de artículos de ejemplo que se 
		// produce en el LIVE cuando se carga el sistema por primera vez
		if (PropsValues.ITER_APACHE_QUEUE_GETURIPTES_SERVLET_ENABLED && !PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			String globalId = getAttribute(item, "globalid");
			String sGroupId = getAttribute(item, "groupid");		
	
			// Si no ha fallado la publicación del artículo se añade a la lista de URIPtes
			if (validateLog(sGroupId, globalId))
			{
				String articleId = getParamTextByName(item, "articleid");
				
				// El prefijo es necesario para crear la URL móvil. Se pone uno vacío y luego se restaura
				Object iterUrlPrefix = PublicIterParams.get(WebKeys.REQUEST_ITER_URL_PREFIX);
				PublicIterParams.set(WebKeys.REQUEST_ITER_URL_PREFIX, WebKeys.REQUEST_ITER_URL_PREFIX);
				
				try
				{
					// Solo se añadirán artículos que tengan al menos un pageContent vigente
					if (JournalArticleTools.isActiveArticle(articleId))
					{
						long groupId = xmlIOContext.getGroupId();
						
						// Se añade la URL canónica
						String virtualhost 	= LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
						ErrorRaiser.throwIfFalse(Validator.isNotNull(virtualhost), IterErrorKeys.XYZ_E_UNDEFINED_VIRTUALHOST_ZYX, String.valueOf(groupId));
						
						String canonicalURL = IterURLUtil.getArticleURLMLN(groupId, articleId, null);
						String mobileURL	= IterURLUtil.buildMobileURL(groupId, canonicalURL, true, null);
						
						canonicalURL = URIPte.ITER_PROTOCOL.concat(virtualhost).concat(canonicalURL);
						mobileURL 	 = URIPte.ITER_PROTOCOL.concat(virtualhost).concat(mobileURL);
						
						String sql = String.format(INSERT_URIPTES, groupId, canonicalURL, mobileURL);
						
						if (_log.isDebugEnabled())
							_log.debug("New URIPte:\n".concat(sql));
						PortalLocalServiceUtil.executeUpdateQuery(sql);
					}
				}
				catch (Exception e)
				{
					String errorMsg = String.format("Can not insert article '%s' as URIpte. ", articleId);
					_log.error( errorMsg.concat(e.toString()) );
					_log.debug( errorMsg, e );
				}
				finally
				{
					PublicIterParams.set(WebKeys.REQUEST_ITER_URL_PREFIX, iterUrlPrefix);
				}
			}
		}
	}
	
	@Override
	protected void updateDependencies(Element item, Document doc)
	{
		String articleId = "";
		String sGroupId = "";
		long groupId = 0;
		List<String> classNameDependencies = new ArrayList<String>();
		List<PageContent> pageContents = new ArrayList<PageContent>();
		
		try
		{
			sGroupId = getAttribute(item, "groupid");		
			groupId = getGroupId(sGroupId);
			
			List<Node> nodes = item.selectNodes(".//param[@name='classname']");
			for(Node node : nodes)
			{
				String classname = StringUtils.trim(node.getStringValue());
				classname = CDATAUtil.strip(classname);
				if (Validator.isNotNull(classname))
				{
					classNameDependencies.add(classname);
					if( classname.equalsIgnoreCase(IterKeys.CLASSNAME_PAGECONTENT) )
					{
						String pageContentGroupName = node.getParent().selectSingleNode("./param[@name='groupname']").getStringValue();
						pageContentGroupName = CDATAUtil.strip(pageContentGroupName);
						long pageContentGroupId = getGroupId(pageContentGroupName);
						String pageContentGlobalId = node.getParent().attributeValue("name");
						
						Live live = LiveLocalServiceUtil.getLiveByGlobalId(pageContentGroupId, IterKeys.CLASSNAME_PAGECONTENT, pageContentGlobalId);
						if(live!=null)
						{
							PageContent pgCnt = PageContentLocalServiceUtil.getPageContent(pageContentGroupId, live.getLocalId());
							if(pgCnt!=null)
								pageContents.add((PageContent) pgCnt.clone());
						}
					}
				}
			}
		}
		catch (Exception e) 
		{
			_log.error( e.toString() );
			_log.debug(e);
		}
		
		// Importación de los binarios
		try
		{
			importBinaries(item, IterKeys.UPDATE);
		}
		catch (Exception e)
		{
			_log.error("Can not update article binaries for articleId " + articleId + " last publication date (reviewdate): " + e.toString());
			_log.debug(e);
		}
		
		super.updateDependencies(item, doc);
		
		// Se ha modificado el artículo, se añade la URL canónica y la móvil como URIPtes
		insertURIPtes(item);
		
		try
		{
			String globalId = getAttribute(item, "globalid");
			

			if( classNameDependencies.contains(IterKeys.CLASSNAME_ENTRY) )
			{
				updateLastPublicationDate(groupId, globalId);
			}
			else if( classNameDependencies.contains(IterKeys.CLASSNAME_PAGECONTENT) )
			{
				int numPgContents = pageContents.size();
				if(numPgContents>0)
				{
					for(int i=0; i<numPgContents; i++)
					{
						PageContent oldPageContent = pageContents.get(i);
						PageContent newPageContent = PageContentLocalServiceUtil.getPageContent(oldPageContent.getId());
						
						if( oldPageContent.getOrden()					!=	newPageContent.getOrden() 					|| 
							oldPageContent.getArticleModelId()			!=	newPageContent.getArticleModelId() 			||
							!oldPageContent.getQualificationId().equalsIgnoreCase( newPageContent.getQualificationId() )||
							oldPageContent.getVigenciadesde().getTime()	!=	newPageContent.getVigenciadesde().getTime()	||
							oldPageContent.getVigenciahasta().getTime()	!=	newPageContent.getVigenciahasta().getTime()	||
							(oldPageContent.getOnline()					!=	newPageContent.getOnline())
						)
						{
							updateLastPublicationDate(groupId, globalId);
							break;
						}
					}
				}
				else
				{
					updateLastPublicationDate(groupId, globalId);
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Can not update article " + articleId + " last publication date (reviewdate): " + e.toString());
			_log.debug(e);
		}
	}
	
	private boolean isValid()
	{
		return _isValid;
	}
	private boolean validateLog(String groupName, String globalId)
	{
		return validateLog(getClassName(), groupName, globalId);
	}
	private boolean validateLog(String className, String groupName, String globalId)
	{
		_isValid = isValid() && xmlIOContext.itemLog.validateLog(className, groupName, globalId);
		return isValid();
	}


	private void updateLastPublicationDate(long groupId, String globalId) throws SystemException, PortalException
	{
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date lastPublication 	= GetterUtil.getDate(Calendar.getInstance().getTime(), df );
		
		Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
		if(live!=null)
		{
			String articleId = live.getLocalId();
			
			JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
			journalArticle.setReviewDate( lastPublication );
			
			JournalArticleUtil.update(journalArticle, false);
		}
		else
			_log.error( String.format("No LIVE entry for JournalArticle with groupId='%s' and globalId='%s'. Can not update last publication date (reviewdate).", groupId, globalId) );
	}

	/**
	 * 
	 * @param content
	 * @return
	 */
	private String transformLocalToGlobal(JournalArticle content, XMLIOExport xmlioExport, Element itemElement)
	{
		Document doc = null;
			
		try
		{
			//1. Creamos un XML content
			doc = SAXReaderUtil.read(content.getContent().substring(0));
			//2. Obtenemos los elemento de tipo document_library
			String xPath = "//dynamic-element[@type='document_library']/dynamic-content";
			List<Node> nodes = doc.selectNodes(xPath);
			List<String> articleBinaries = new ArrayList<String>();
			for (Node node : nodes)
			{
				String fileurl = node.getText();
				String[] str = fileurl.split("/");
				
				// Si se gestiona por el repositorio de binarios de Iter, se añaden a la lista de binarios a exportar
				if (str.length == 3 && "binrepository".equals(str[1]))
				{
					articleBinaries.add(str[2]);
				}
				//Si es un DLFileEntry, modificamos su nodo "fileurl" para cambiar los ids locales por globales
				else if(str!=null && str.length>1)
				{
					try
					{
					
						String localGrpId = str[2];
						String grpName = GroupLocalServiceUtil.getGroup( GetterUtil.getLong(localGrpId) ).getName();
						String fileName = str[str.length-1];
						String localFolderId = str[str.length-2];
						Live live = LiveLocalServiceUtil.getLiveByLocalId(GetterUtil.getLong(localGrpId), IterKeys.CLASSNAME_DLFOLDER, localFolderId);
						String globalFolderId = "0";
						if(live!=null)
							globalFolderId = live.getGlobalId();
						
						try{
							DLFileEntry dlFile = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(fileName, GetterUtil.getLong(localGrpId));
							if(dlFile!=null){
								fileName =  dlFile.getTitle();
							}
						}catch(NoSuchFileEntryException e){}
	
						String globalFileUrl = "/" + str[1] + "/" + grpName + "/" + globalFolderId + "/" + fileName;
						
						node.setText(globalFileUrl);
						
					}catch(Exception err){
						_log.error("Cannot convert local url " + fileurl + " to global");
					}
				}
				
			}
			
			// Artículo con ediciones. Será necesario añadir a la transformación el listado de groupIds que puedan tener los "Tipos de contenido".
			nodes = doc.selectNodes("/root/dynamic-element[@groups]");
			for (Node node : nodes)
			{
				Attribute attr				= ((Element)node).attribute("groups");
				String[] localGroups 		= attr.getValue().split(",");
				List<String> globalGroups 	= new ArrayList<String>();
				
				for (String localGroupId : localGroups)
				{
					boolean isNegative = localGroupId.startsWith("-");
					if (isNegative)
						localGroupId = localGroupId.substring(1);
					
					// Existe el registro en el XMLIO_Live y dicho grupo se ha publicado
					Live live = LiveLocalServiceUtil.getLiveByLocalId(Long.parseLong(localGroupId), IterKeys.CLASSNAME_GROUP, localGroupId);
					if (live != null && "S".equalsIgnoreCase(live.getExistInLive()))
					{
						String globalId = live.getGlobalId();
						if (isNegative)
							globalId = "-".concat(globalId);
						
						globalGroups.add(globalId);
					}
				}
				
				// Si no tiene grupos publicado al Live se elimina el atributo
				if (globalGroups.size() == 0)
					attr.detach();
				else
					attr.setValue( StringUtil.merge(globalGroups) );
			}
			
			// Añade los binarios nuevos / actualizados
			addBinariesToXML(articleBinaries, String.valueOf(content.getDelegationId()), xmlioExport, itemElement);
			// Añade los binarios pendientes de borrar
			addDeletePendingBinariesToXML(content.getArticleId(), String.valueOf(content.getDelegationId()), xmlioExport, itemElement);
		}
		catch (Exception e)
		{
			_log.error(e);
		}
		
		return doc.asXML();
	}
	
	/**
	 * 
	 * @param content
	 * @return
	 */
	private String transformGlobalToLocalURL(String content) 
	{
		
		Document doc = null;
		
		try 
		{
			//1. Creamos un XML content
			doc = SAXReaderUtil.read(content);
			
			//2. Obtenemos los elemento de tipo DLFILEENTRY
			String xPath = "//dynamic-element[@type='document_library' and not(starts-with(dynamic-content, '/binrepository/'))]/dynamic-content";
			List<Node> nodes = doc.selectNodes(xPath);
			
			//3. Para cada uno de ellos modificamos su nodo "fileurl" para cambiar los ids locales por globales
			for (Node node : nodes) 
			{
				
				String fileurl = node.getText();
				
				String[] str = fileurl.split("/");
				if(str!=null && str.length>1)
				{
					try
					{
						String grpName = str[2];
						long localGrpId = getGroupId(grpName);
						String fileName = str[str.length-1];
						String globalFolderId = str[str.length-2];					
						
						String folderPath = "/0";
						if(!globalFolderId.equals("0"))
						{
							Live live = LiveLocalServiceUtil.getLiveByGlobalId(localGrpId, IterKeys.CLASSNAME_DLFOLDER, globalFolderId);
							folderPath = XMLIOUtil.buildFolderLocalUrl(GetterUtil.getLong(live.getLocalId()));
						}
						
						String localFileUrl = "/" + str[1] + "/" + localGrpId + folderPath + "/" + fileName;
						
						node.setText(localFileUrl);
					}
					catch(Exception err)
					{
						_log.error("Cannot convert global url " + fileurl + " to local");
					}
				}
			}
			
			// Artículo con ediciones. Será necesario añadir a la transformación el listado de groupIds que puedan tener los "Tipos de contenido".
			nodes = doc.selectNodes("/root/dynamic-element[@groups]");
			for (Node node : nodes)
			{
				Attribute attr				= ((Element)node).attribute("groups");
				String[] globalGroups 		= attr.getValue().split(",");
				List<String> localGroups 	= new ArrayList<String>();
				
				for (String globalGroupId : globalGroups)
				{
					boolean isNegative = globalGroupId.startsWith("-");
					if (isNegative)
						globalGroupId = globalGroupId.substring(1);

					// Existe el registro en el XMLIO_Live
					List<Live> lives = LiveLocalServiceUtil.getLiveByGlobalId(IterKeys.CLASSNAME_GROUP, globalGroupId);
					
					if (lives.size() > 0)
					{
						String localId = lives.get(0).getLocalId();
						if (isNegative)
							localId = "-".concat(localId);

						localGroups.add(localId);
					}
				}
				
				// Si no tiene grupos se elimina el atributo
				if (localGroups.size() == 0)
					attr.detach();
				else
					attr.setValue( StringUtil.merge(localGroups) );
			}
		} 
		catch (Exception e) 
		{
			_log.error(e);
		}
		
		return doc.asXML();
	}
	
	
	/**
	 * 	Determina si existe un pagecontent para dicho artículo que cumpla la condición de hemeroteca:
	 *	Que exista al menos un PageContent "publicable" y "NO vigente"
	 *
	 * 	Michel: 15/04/2013. Se anula el concepto de hemeroteca, si se borra un artículo se elimina su AssetEntry y Expandos, 
	 *  no se ponen a pending. Si se quiere eliminar del LIVE ya enviará el BACK al LIVE una orden de borrado del artículo
	 */
 	static public boolean isArchivableArticle(String contentId)
 	{
 		return true;
 	}
 	
 	public static String getGlobalProductsString(String articleId)
 	{
 		StringBuffer productsString = new StringBuffer();
 		if(Validator.isNotNull(articleId))
 		{
	 		String query = String.format(GET_GLOBAL_PRODUCTS_BY_ARTICLEID, articleId);
			try
			{
				List<Object> products = PortalLocalServiceUtil.executeQueryAsList(query);
				if(products != null && products.size() > 0)
				{
					for(int i = 0; i < products.size(); i++)
					{
						Object productData = products.get(i);
						if(productData != null)
						{
							String productId = productData.toString();
							if(!productId.isEmpty())
							{
								if(i < products.size() - 1)
									productsString.append(productId + ",");
								else
									productsString.append(productId);
							}
							else
							{
								_log.error("Empty Xmlio_Live/Product entry associated with JournalArticle " + articleId);
							}
						}
						else
						{
							_log.error("Empty Xmlio_Live/Product entry associated with JournalArticle " + articleId);
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.error(e.toString());
				_log.trace(e);
			}
 		}
 		
 		return productsString.toString();
 	}
 	
 	public static void deleteInsertProducts(String articleId, String[] globalProducts)
 	{
		try
		{
			if(Validator.isNotNull(articleId))
			{
				//Delete
				String query = String.format(DELETE_ARTICLE_PRODUCTS, articleId);
				PortalLocalServiceUtil.executeUpdateQuery(query);
				
				if(globalProducts != null && globalProducts.length > 0)
				{
					//Insert
					String inClause = getInClauseSQL(globalProducts);
					query = String.format(INSERT_ARTICLE_PRODUCTS, articleId, inClause);
					PortalLocalServiceUtil.executeUpdateQuery(query);
				}
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
 	}
 
 	private void addBinariesToXML(List<String> articleBinaries, String delegationId, XMLIOExport xmlioExport, Element itemElement) throws SystemException, ServiceError
 	{
		if (articleBinaries.size() > 0)
		{
			// Comprueba los binarios que hay que exportar
			if (!exportAllDependencies)
			{
				String binaryNames = StringUtil.merge(articleBinaries.toArray(), StringPool.COMMA, StringPool.APOSTROPHE);
				// Recupera los que no tienen que publicarse.
				String query = "SELECT title FROM binary_repository WHERE title IN (%s) AND publishDate IS NOT NULL AND publishDate > modifiedDate";
				List<Object> excludedBinaries = PortalLocalServiceUtil.executeQueryAsList(String.format(query, binaryNames));
				// Los elimina de la lista de publicables.
				for (Object obj : excludedBinaries)
				{
					if (obj != null)
					{
						articleBinaries.remove((String) obj);
					}
				}
			}
			
			if (articleBinaries.size() > 0)
			{
				Element binaries = itemElement.addElement("binaries");
				binaries.addAttribute("delegationId", delegationId);
				
				// Exporta los binarios
				for (String binaryTitle : articleBinaries)
				{	
					// Añade el fichero
					xmlioExport.addResource(new StringBuilder("/binrepository/").append(binaryTitle).toString(),
							                BinaryRepositoryLocalServiceUtil.getBinary(binaryTitle, delegationId));
					
					// Añade la dependencia
					Element binary = binaries.addElement("binary");
					binary.addAttribute("name", binaryTitle);
					binary.addAttribute("operation", IterKeys.CREATE);
					
					// Busca los productos asociados al binario y los añade
					List<Object> products = BinaryRepositoryLocalServiceUtil.getBinaryProductsByTitle(binaryTitle, true);
					if (products.size() > 0)
					{
						StringBuilder productsList = new StringBuilder();
						for (Object product : products)
						{
							if (productsList.length() > 0) productsList.append(StringPool.COMMA);
							productsList.append(product.toString());
						}
						binary.addElement("products").addCDATA(productsList.toString());
					}
				}
			}
		}
 	}
 	
 	private void addDeletePendingBinariesToXML(String articleId, String delegationId, XMLIOExport xmlioExport, Element itemElement)
 	{
 		// Recupera los binarios pendientes de borrar
 		String sql = String.format(GET_DELETE_PENDING_BINARIES_BY_ARTICLE_ID, articleId);
 		List<Object> deletePendingBinaries = PortalLocalServiceUtil.executeQueryAsList(sql);
 		if (deletePendingBinaries.size() > 0)
 		{
 			Element binaries = itemElement.element("binaries");
 			if (binaries == null)
 			{
 				binaries = itemElement.addElement("binaries");
				binaries.addAttribute("delegationId", delegationId);
 			}
 				
 			for (Object binaryId : deletePendingBinaries)
 			{
	 			Element binary = binaries.addElement("binary");
				binary.addAttribute("operation", IterKeys.DELETE);
				binary.addAttribute("id", binaryId.toString());
 			}
 		}
 	}
 	
 	private void importBinaries(Element item, String operation) throws Exception
 	{
 		// Recupera la delegación y el grupo.
 		String delegationId = XMLHelper.getStringValueOf(item, "binaries/@delegationId");
		String groupName = XMLHelper.getStringValueOf(item, "@groupid");
 		
 		// Si es una operación de actualización, puede que haya que borrar binarios en el LIVE.
 		if (IterKeys.UPDATE.equals(operation))
 		{
 			List<Node> binariesToDelete = item.selectNodes("binaries/binary[@operation='" + IterKeys.DELETE + "']");
 			if (binariesToDelete.size() > 0)
 			{
 				for (Node binary : binariesToDelete)
 				{
 					// Lo elimina del LIVE.
 					String binaryId = ((Element) binary).attributeValue("id");
 					BinaryRepositoryLocalServiceUtil.deleteBinary(binaryId, delegationId);
 					// Añade el mensaje de confirmación de importación.
 					Element binaryItem = SAXReaderUtil.createElement("binary").addAttribute("id_", binaryId);
					xmlIOContext.itemLog.addMessage(binaryItem, binary.getText(), "binary", IterKeys.DELETE, "Done", IterKeys.DONE, groupName);
 				}
 			}
 		}
 		
 		// Binarios pendientes de publicar en el LIVE
		List<Node> binaries = item.selectNodes("binaries/binary[@operation='" + IterKeys.CREATE + "']");
		if (binaries.size() > 0)
		{
			for (Node binary : binaries)
			{
				InputStream is = null;
				try
				{
					String binaryName = ((Element) binary).attributeValue("name");
					// Busca el binario físico
					String filePath = XMLIOImport.getFile(new StringBuilder("binrepository/").append(binaryName).toString());
					is = new FileInputStream(filePath);
					// Si el fichero existe...
					if (is != null)
					{
						// Lo crea / actualiza en el repositorio
						String binaryId = BinaryRepositoryLocalServiceUtil.addBinary(is, binaryName, delegationId);
						
						// Procesa sus productos
						String products = ((Element) binary).elementText("products");
						if (Validator.isNotNull(products))
						{
							// Transforma los Ids Globales en Locales
							String productsGlobalIDs = StringUtil.merge(products.split(StringPool.COMMA), StringPool.COMMA, StringPool.APOSTROPHE);
							String query = String.format(GET_PRODUCTS_LOCAL_ID, productsGlobalIDs);
							List<Object> productList = PortalLocalServiceUtil.executeQueryAsList(query);
							
							if (productList.size() > 0)
							{
								// Crea el XML para añadirlos por el servicio
								Document xmlProducts = SAXReaderUtil.createDocument();
								Element root = xmlProducts.addElement("param");
								root.addElement("target").addAttribute("id", binaryId);
								// Añade los productos
								for (Object product : productList)
								{
									root.addElement("product").addAttribute("id", product.toString());
								}
								BinaryRepositoryLocalServiceUtil.setBinaryProducts(xmlProducts.asXML(), true);
							}
						}
						// Añade el mensaje de confirmación de importación
						Element binaryItem = SAXReaderUtil.createElement("binary").addAttribute("id_", binaryId);
						xmlIOContext.itemLog.addMessage(binaryItem, binary.getText(), "binary", IterKeys.CREATE, "Done", IterKeys.DONE, groupName);
					}
				}
				finally
				{
					// Cierra el InputStream
					try { if (is != null) is.close();} catch (Throwable th) { _log.error(th); };
				}
			}
		}
 	}
 	
 	private void logScheduledPublications(JournalArticle journalArticle)
 	{	
		// Recupera las publicaciones programadas del artículo
		Node articlePublications = journalArticle.getScheduledPublications();
		// Si tiene publicaciones programadas...
		if (articlePublications != null)
		{
			// Obtiene el elemento que contiene las publicaciones
	 		Node publications = xmlIOContext.itemLog.toXML().selectSingleNode("/iter/scheduledPublications");
	 		// Si no existe, lo crea
			if (publications == null)
				publications = xmlIOContext.itemLog.toXML().getRootElement().addElement("scheduledPublications");
			// Añade el articleId
			((Element) articlePublications).addAttribute("articleId", journalArticle.getArticleId());
			// Añade las publicaciones programadas al log de resultado de la publicación
			((Element) publications).add( articlePublications );
		}
 	}
}