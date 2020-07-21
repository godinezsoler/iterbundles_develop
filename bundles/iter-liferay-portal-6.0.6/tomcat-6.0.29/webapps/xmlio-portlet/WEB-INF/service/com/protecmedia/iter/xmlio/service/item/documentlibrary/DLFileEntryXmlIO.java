/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.documentlibrary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/* XML example:
 * 
<item operation="create" globalid="DiariTerrassaPre_316703" classname="com.liferay.portlet.documentlibrary.model.DLFileEntry" groupid="The Star">
	<param name="folder">&lt;![CDATA[0]]&gt;</param>
	<param name="extension">&lt;![CDATA[jpg]]&gt;</param>
	<param name="extrasettings">&lt;![CDATA[]]&gt;</param>
	<param name="title">&lt;![CDATA[Lighthouse.jpg]]&gt;</param>
	<param name="description">&lt;![CDATA[]]&gt;</param>
	<param name="name">&lt;![CDATA[105701]]&gt;</param>
	<param name="fileurl">&lt;![CDATA[/documents/The Star/0/Lighthouse.jpg]]&gt;</param>
	<param name="DiariTerrassaPre_316709" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.asset.model.AssetEntry]]&gt;</param>
		<param name="groupname">&lt;![CDATA[The Star]]&gt;</param>
	</param>
	<param name="DiariTerrassaPre_316705" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.expando.model.ExpandoRow]]&gt;</param>
		<param name="groupname">&lt;![CDATA[Global]]&gt;</param>
	</param>
</item>
 * 
*/

public class DLFileEntryXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(DLFileEntryXmlIO.class);
	private String _className = IterKeys.CLASSNAME_DLFILEENTRY;
	
	public static final String GET_GLOBAL_PRODUCTS_BY_FILEID = "SELECT l.globalId FROM Xmlio_Live l\n" +
													   		   "INNER JOIN FileEntryProduct fp ON l.localId=fp.productId\n" + 
													   		   "WHERE l.classnamevalue='" + IterKeys.CLASSNAME_PRODUCT + "'\n" +
													   		   "AND fp.fileEntryId='%s'";

	public static final String DELETE_FILE_PRODUCTS = "DELETE FROM FileEntryProduct\n" +
														 "WHERE fileEntryId='%s'";
	
	public static final String INSERT_FILE_PRODUCTS = "INSERT INTO FileEntryProduct\n" + 
														 "SELECT l.localId, '%s' FROM Xmlio_Live l\n" + 
														 "WHERE l.classnamevalue='" + IterKeys.CLASSNAME_PRODUCT + "'\n" +
														 "AND l.globalId IN %s";
	
	public DLFileEntryXmlIO() {
		super();
	}
	
	public DLFileEntryXmlIO(XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}

	@Override
	public String getClassName(){
		return _className;
	}
	
	/*
	 * Live Functions
	 */
	/**
	 * @see DLFolderXmlIO
	 */
	@Override
	public void populateLive(long groupId, long companyId) throws SystemException{
		//Files in DLFolders are populated via DLFolderXmlIO.populateLive
		
		try{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			List<DLFileEntry> globalFileEntryList = DLFileEntryLocalServiceUtil.getFileEntries(globalGroupId, 0);
			for (DLFileEntry globalFileEntry : globalFileEntryList){	
				try {
					createLiveEntry(globalFileEntry);
				} catch (PortalException e) {
					_log.error("Can't add Live, DLFileEntry: " + globalFileEntry.getFileEntryId());
				}
			}
		} catch (PortalException e) {
			_log.error("Can't add Live, DLFileEntries from Global group");
		}
			
		List<DLFileEntry> fileEntryList = DLFileEntryLocalServiceUtil.getFileEntries(groupId, 0);
		for (DLFileEntry fileEntry : fileEntryList){	
			try {
				createLiveEntry(fileEntry);
			} catch (PortalException e) {
				_log.error("Can't add Live, DLFileEntry: " + fileEntry.getFileEntryId());
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		DLFileEntry fileEntry = (DLFileEntry)model;
		
		String id = String.valueOf(fileEntry.getFileEntryId());
		
		//Comprobamos que no existe
		Live liveFE = LiveLocalServiceUtil.getLiveByLocalId(fileEntry.getGroupId(), _className, id);
		if (liveFE == null){
			//insert element in LIVE. No se crea entrada en LivePool aqui, la crea el contenido al que se asocia el DLFE
			LiveLocalServiceUtil.add(_className, fileEntry.getGroupId(), 0, 0, 
					IterLocalServiceUtil.getSystemName() + "_" + id, id, 
					IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		}else{
			//Poner live a CREATE/PENDING
			LiveLocalServiceUtil.updateLive(liveFE.getGroupId(), liveFE.getClassNameValue(), liveFE.getLocalId(), IterKeys.CREATE, IterKeys.PENDING, new Date());
		}
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException
	{
		DLFileEntry fileEntry 	= (DLFileEntry)model;
		String id 				= String.valueOf(fileEntry.getFileEntryId());
		
		Live live = LiveLocalServiceUtil.getLiveByLocalId(fileEntry.getGroupId(), _className, id);
		
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
		DLFileEntry fileEntry = (DLFileEntry)model;
		String globalId 		= IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(fileEntry.getFileEntryId());
		
		LiveLocalServiceUtil.updateStatus(fileEntry.getGroupId(), _className, globalId, status);
	}


	/*
	 * Export Functions
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
		
		//Put necessary parameters for each kind of operation.
		try{
			if (operation.equals(IterKeys.CREATE)){	
					
				DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(GetterUtil.getLong(live.getLocalId()));
				
				String extension = dlFileEntry.getExtension();
				String title = dlFileEntry.getTitle();								
				String fileUrl = "";
									
				try 
				{
					long delegationId = (xmlioExport.getGroup() > 0) 													? 
											GroupLocalServiceUtil.getGroup(xmlioExport.getGroup()).getDelegationId()	:
											group.getDelegationId();
					InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, dlFileEntry.getUserId(), group.getGroupId(), dlFileEntry.getFolderId(), dlFileEntry.getName());
					
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
					int nRead;
					byte[] data = new byte[16384];
	
					while ((nRead = is.read(data, 0, data.length)) != -1) {
						buffer.write(data, 0, nRead);
					}
	
					if(extension.trim().equals(""))
						extension = FileUtil.getExtension(title);
					
					
					String sourceName = dlFileEntry.getFileEntryId() + ( !extension.equals("") ? "." + extension : "");
					String folderUrl = XMLIOUtil.buildFolderGlobalIdsUrl(dlFileEntry.getFolderId());
					if (folderUrl.equals("")) {
						folderUrl = "/0";
					}
					fileUrl = "/documents/" + group.getName() + folderUrl + "/" + sourceName;
						
					xmlioExport.addResource(fileUrl, buffer.toByteArray());	
					
					params.put("delegationId", String.valueOf(delegationId));
					params.put("title", title);
					params.put("description", dlFileEntry.getDescription());								
					params.put("fileurl", fileUrl);	
					params.put("extrasettings", dlFileEntry.getExtraSettings());	
					params.put("extension", extension);	
					params.put("name", dlFileEntry.getName());	
					params.put("sourcename", sourceName);
					
					params.put(IterKeys.XMLIO_XML_PRODUCTS, getGlobalProductsString(String.valueOf(dlFileEntry.getFileEntryId())));	
					
				} catch (IOException e) {
					//_log.error (e);
					//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
					error = "Cannot export item";
				}	
				
				String folderId = String.valueOf(dlFileEntry.getFolderId());
				if(!folderId.equals("0")){
					Live liveFolder = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_DLFOLDER, folderId);
					folderId = liveFolder.getGlobalId();
				}	
				params.put("folder", folderId);	
				
				addDependencies(addNode(root, "item", attributes, params), live.getId());
			
			}else if (operation.equals(IterKeys.UPDATE)){
				
				addDependencies(addNode(root, "item", attributes, params), live.getId());
				
			}else{
				
				addNode(root, "item", attributes, params);	
				
			}	
		
		} catch (Exception e) {
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");	
			error = "Cannot export item";
		}
	
		_log.debug("XmlItem OK");
		
		return error;
	}
	
	/*
	 * Import function
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
					String fileEntryId = live.getLocalId(); 
					DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(GetterUtil.getLong(fileEntryId));
					
					try
					{
						// El ItemXMLIO correspondiente se borra al borrar el FileEntry
						ServiceContext serviceContext = new ServiceContext();
						serviceContext.setDelegationId(GroupLocalServiceUtil.getGroup(groupId).getDelegationId());
						DLFileEntryLocalServiceUtil.deleteFileEntry(dlFileEntry, serviceContext);
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					}
					catch(Exception e2)
					{
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e2.toString(), IterKeys.ERROR, sGroupId);
					}
				}
				catch(Exception e1)
				{
					if (live != null)
					{
						// clean entry in live table
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
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	
	@Override
	protected void modify(Element item, Document doc) 
	{
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
		
		_log.trace("\n");
		_log.trace("DLFileEntry: Begin");

		int imgMaxWidth	= 0;
		int imgMaxHeight= 0;
		
		String value = null;
		
		value = getAttribute(item, "maxwidth");
		if(value!=null && !value.isEmpty())
			imgMaxWidth	= Integer.valueOf( value ).intValue();
		
		value = getAttribute(item, "maxheight");
		if(value!=null && !value.isEmpty())
			imgMaxHeight = Integer.valueOf( value ).intValue();

		if ( imgMaxHeight==0 && imgMaxWidth==0 )
		{
			value = getAttribute(doc.getRootElement(), "maxwidth");
			if(value!=null && !value.isEmpty())
				imgMaxWidth	= Integer.valueOf( value ).intValue();
			
			value = getAttribute(doc.getRootElement(), "maxheight");
			if(value!=null && !value.isEmpty())
				imgMaxHeight = Integer.valueOf( value ).intValue();
		}

		long delegationId	= GetterUtil.getLong(getParamTextByName(item, "delegationId"));
		String title 		= getParamTextByName(item, "title");
		String description 	= getParamTextByName(item, "description");
		String fileUrl 		= getParamTextByName(item, "fileurl");
		String folder 		= getParamTextByName(item, "folder");
		String extraSettings= GetterUtil.getString(getParamTextByName(item, "extrasettings"), "");
		String extension 	= getParamTextByName(item, "extension");
		String sourceName 	= getParamTextByName(item, "sourcename");
		
		_log.trace("DLFileEntry: After setting params");
		
		long fileEntryId = -1;
		
		try
		{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try 
			{
				byte[] bytes = XMLIOImport.getFileAsBytes(fileUrl);
				
				_log.trace("DLFileEntry: After XMLIOImport.getFileAsBytes");
				
				if (bytes != null) 
				{
					long folderId = 0;
					if (!folder.equals("0"))
					{
						Live livep = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_DLFOLDER, folder);
						folderId = GetterUtil.getLong(livep.getLocalId()); 
						_log.trace("DLFileEntry: After LiveLocalServiceUtil.getLiveByGlobalId");
					}
					
					
					Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);					
					ServiceContext sc = new ServiceContext();
					sc.setDelegationId(delegationId);
					_log.trace("DLFileEntry: After new ServiceContext()");
			
					//Se comprueba si existe el FileEntry en Live (por localId) y si no en el sistema (por titulo)
					DLFileEntry fileEntry = null;
					try
					{
						fileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(GetterUtil.getLong(live.getLocalId()));
						_log.trace("DLFileEntry: After getDLFileEntry");
					}
					catch(Exception err)
					{
						try
						{
							fileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(groupId, folderId, title);
							_log.trace("DLFileEntry: After getFileEntryByTitle");
						}
						catch(Exception dfe)
						{
							//Va al create.							
						}
					}
					
					if(fileEntry != null)
					{
						//UPDATE
						try
						{
							_log.trace("DLFileEntry: Before ImageLocalServiceUtil.isImage");
							
							//Escalado de la imagen
							if(ImageLocalServiceUtil.isImage(title))
							{
								float quality = PropsValues.ITER_IMAGE_SCALE_QUALITY;
								bytes = ImageLocalServiceUtil.scaleImage(bytes, imgMaxWidth, imgMaxHeight, fileEntry, quality);
								_log.trace("DLFileEntry: After ImageLocalServiceUtil.scaleImage");
							}
							
							fileEntry = DLFileEntryLocalServiceUtil.updateFileEntry(xmlIOContext.getUserId(), groupId, folderId, fileEntry.getName(), sourceName, title, description, "", true, extraSettings, bytes, sc);
							_log.trace("DLFileEntry: After DLFileEntryLocalServiceUtil.updateFileEntry");
						
							//Actualiza el id (por si se borra y se crea en vez de actualizar)
							if(live != null)
							{
								LiveLocalServiceUtil.updateLocalId(groupId, IterKeys.CLASSNAME_DLFILEENTRY, live.getLocalId(), String.valueOf(fileEntry.getFileEntryId()));
								_log.trace("DLFileEntry: After LiveLocalServiceUtil.updateLocalId");
							}
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, globalId, String.valueOf(fileEntry.getFileEntryId()), 
													 IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							_log.trace("DLFileEntry: After LiveLocalServiceUtil.add");
							
							fileEntryId = fileEntry.getFileEntryId();
						}
						catch(Exception dfe)
						{
							_log.error(dfe);
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + dfe.toString(), IterKeys.ERROR, group.getName());
						}
					}
					else
					{
						_log.trace("DLFileEntry: Before fileEntry == null");
						if (live == null || !live.getOperation().equals(IterKeys.DELETE))
						{	
							// Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
							if (live != null)
							{
								LiveLocalServiceUtil.deleteLiveById(live.getId());
								_log.trace("DLFileEntry: After LiveLocalServiceUtil.deleteLiveById");
							}
							
							//CREATE							
							try
							{
								_log.trace("DLFileEntry: Before ImageLocalServiceUtil.isImage");
								
								//Escalado de la imagen
								if(ImageLocalServiceUtil.isImage(title))
								{
									float quality = PropsValues.ITER_IMAGE_SCALE_QUALITY;
									bytes = ImageLocalServiceUtil.scaleImage(bytes, imgMaxWidth, imgMaxHeight, quality);
									_log.trace("DLFileEntry: After ImageLocalServiceUtil.scaleImage");
								}
								
								DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.addFileEntry(xmlIOContext.getUserId(), group.getGroupId(), folderId, sourceName, title, description, "", extraSettings, bytes, sc);
								dlFileEntry.setExtension(extension);
								_log.trace("DLFileEntry: After DLFileEntryLocalServiceUtil.addFileEntry");

								//update entry in live table
								LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId, String.valueOf(dlFileEntry.getFileEntryId()), 
														 IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								_log.trace("DLFileEntry: After LiveLocalServiceUtil.add");
								try
								{
									//Update globalId.
									LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(dlFileEntry.getFileEntryId()), globalId);
									_log.trace("DLFileEntry: After LiveLocalServiceUtil.updateGlobalId");
								}
								catch(Exception e3)
								{
									_log.error(e3);
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, group.getName());
								}
								
								fileEntryId = dlFileEntry.getFileEntryId();
								
							}
							catch(Exception dfe)
							{
								_log.error(dfe);
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + dfe.toString(), IterKeys.ERROR, group.getName());
							}		
						}					
					}
					
					//Creamos/modificamos sus dependencias	
					try 
					{
						_log.trace("DLFileEntry: Before evaluateDependencies");
						
						if (! evaluateDependencies(item, doc))
						{
							LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
							_log.trace("DLFileEntry: After updateStatus");
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
						}
						else
						{									
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						}
					} 
					catch (DocumentException err) 
					{
						LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
					}	
					
				}
				else
				{
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "Error: Can't read file", IterKeys.ERROR, sGroupId);
				}				
			} 
			catch (Exception e) 
			{		
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "GroupId not found", IterKeys.ERROR, sGroupId);
		}
		
		if(fileEntryId > -1)
		{
			_log.trace("JArticle: Before deleteInsertProducts");
			
			// Comprobar las asociaciones Product/DLFileEntry
			String[] products = splitComa(getParamTextByName(item, IterKeys.XMLIO_XML_PRODUCTS));
			deleteInsertProducts(String.valueOf(fileEntryId), products);
		}
		
		
		_log.trace("DLFileEntry: End");
		_log.trace("\n");
	}		
	
	@Override
	public void validateContents(Element item, Document doc)
	{
		_log.trace("DLFileEntry: validate: Begin");
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
	
		try
		{
			long groupId = getGroupId(sGroupId);
			
			Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			_log.trace("DLFileEntry: validate: getLiveByGlobalId");
			if (live==null)
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "File Entry not found", IterKeys.ERROR, sGroupId);
				_log.trace("DLFileEntry: validate: itemLog.addMessage");
			}
					
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "File Entry not found", IterKeys.ERROR, sGroupId);
		}
		_log.trace("DLFileEntry: validate: End");
	}	
	
	@Override
	public long   getGroupId(BaseModel<?> model)
	{
		return ((DLFileEntry)model).getGroupId();
	}
	@Override
	public String getLocalId(BaseModel<?> model)
	{
		return String.valueOf( ((DLFileEntry)model).getFileEntryId() );
	}
	
	public static String getGlobalProductsString(String articleId)
 	{
 		StringBuffer productsString = new StringBuffer();
 		if(Validator.isNotNull(articleId))
 		{
	 		String query = String.format(GET_GLOBAL_PRODUCTS_BY_FILEID, articleId);
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
								_log.error("Empty Xmlio_Live/Product entry associated with DLFIleEntry " + articleId);
							}
						}
						else
						{
							_log.error("Empty Xmlio_Live/Product entry associated with DLFIleEntry " + articleId);
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
				String query = String.format(DELETE_FILE_PRODUCTS, articleId);
				PortalLocalServiceUtil.executeUpdateQuery(query);
				
				if(globalProducts != null && globalProducts.length > 0)
				{
					//Insert
					String inClause = getInClauseSQL(globalProducts);
					query = String.format(INSERT_FILE_PRODUCTS, articleId, inClause);
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

}
