/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.NoSuchLiveException;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.ChannelControlLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public abstract class ItemXmlIO {
	
	protected static String TRACE_DELETELIVEENTRY = "%s: deleteLiveEntry\n[\n\tglobalId:%s\n\tdeleteJustNow:%s\n]";
	
	private static Log _log = LogFactoryUtil.getLog(ItemXmlIO.class);
	
	protected XMLIOContext xmlIOContext;	
	
	protected boolean exportAllDependencies = true;

	public ItemXmlIO(){
		this.xmlIOContext = new XMLIOContext();
	}
	
	public ItemXmlIO(XMLIOContext xmlIOContext) {
		this.xmlIOContext = xmlIOContext;
	}	
	
	public void setXMLIOContext(XMLIOContext xmlIOContext){
		this.xmlIOContext = xmlIOContext;
	}
	
	public XMLIOContext getXMLIOContext(XMLIOContext xmlIOContext){
		return this.xmlIOContext;
	}
		
	public void setExportAllDependencies(boolean value){
		exportAllDependencies = value;
	}
	/*************************************************************************
	 *  		COMMON ABSTRACT FUNCTIONS TO BE DEFINED IN EACH ITEM
	 *************************************************************************/
	
	public abstract String getClassName();	
	public abstract void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException;
	public abstract void populateLive(long groupId, long companyId) throws SystemException, PortalException;
	protected abstract void modify(Element item, Document doc);
	protected abstract void delete(Element item);
	protected abstract String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live);
	
	/*************************************************************************
	 *  		COMMON FUNCTIONS TO BE DEFINED IN EACH ITEM
	 *************************************************************************/
	// Debería ser un método abstracto pero dado que hay 42 elementos de tipo ItemXML, tardando 8 minutos por 
	// modificar cada uno sale un total de 336 minutos, 5 horas y media de modificación por crear un simple método
	// ¡SE CREAN BAJO DEMANDA!
	public long   getGroupId(BaseModel<?> model)
	{
		return 0;
	}
	public String getLocalId(BaseModel<?> model)
	{
		return "";
	}

	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		deleteLiveEntry(model, false);
	}
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException{}


	public void updateStatusLiveEntry(BaseModel<?> model) throws PortalException, SystemException	
	{
		updateStatusLiveEntry(model, IterKeys.DONE);
	}
	public void updateStatusLiveEntry(BaseModel<?> model, String status) throws PortalException, SystemException{}

	/*************************************************************************
	 *  					COMMON PUBLISH FUNCTIONS	
	 *************************************************************************/	
	/*************************************************************************
	 * getLive(BaseModel<?> model)
	 * 	
	 * @throws SystemException 
	 * @throws NoSuchLiveException 
	 *************************************************************************/	
	public Live getLive(BaseModel<?> model) throws NoSuchLiveException, SystemException
	{
		return LiveLocalServiceUtil.getLiveByLocalId(getGroupId(model), getClassName(), getLocalId(model));
	}
	public String getGlobalId(BaseModel<?> model)
	{
		return getGlobalByLocalId( getLocalId(model) ); 
	}
	
	public static String getGlobalByLocalId(String localId)
	{
		return IterLocalServiceUtil.getSystemName().concat("_").concat(localId);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Ejecuta queries de tipo SELECT donde la query necesita SÍ o SÍ el
	// grupo, className y localId en ese preciso orden, del XmlIO_Live en cuestión
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public Document getLiveData(BaseModel<?> model, String sql) throws SecurityException, NoSuchMethodException
	{
		sql = String.format(sql, getGroupId(model), getClassName(), getLocalId(model) );
		return PortalLocalServiceUtil.executeQueryAsDom(sql);
	}

	// Se homogenizan las trazas de los métodos más comunes
	protected String getTraceDeleteLiveEntry(String globalId, boolean deleteJustNow)
	{
		return String.format(TRACE_DELETELIVEENTRY, getClassName(), globalId, Boolean.toString(deleteJustNow));
	}
	
	/**
	 * Esta es la función que se invoca desde fuera de ItemXmlIO, para evitar un problema
	 * de classLoader con la clase Live como parámetro
	 */
	public void publishContent(XMLIOExport xmlioExport, Element root, long liveId, String operationType){
		try{
			publishContent(xmlioExport, root, LiveLocalServiceUtil.getLive(liveId), operationType);
		}
		catch(Exception err){
			_log.error("No Live item found with id " + liveId);
		}
	}
	
	protected void publishContent(XMLIOExport xmlioExport, Element root, Live live, String operationType){
		
		exportAllDependencies = false;
		
		try{
			exportContent(xmlioExport, root, live, operationType);
			
		}
		catch(Exception err){
			_log.error(err);
		}
	}

	/**
	 * Esta es la función que se invoca desde fuera de ItemXmlIO, para evitar un problema
	 * de classLoader con la clase Live como parámetro
	 */
	public void publishMileniumContent(XMLIOExport xmlioExport, Element root, long liveId){
		try{
			publishMileniumContent(xmlioExport, root, LiveLocalServiceUtil.getLive(liveId));
		}
		catch(Exception err){
			_log.error("No Live item found with id " + liveId);
		}
		
	}
	
	protected void publishMileniumContent(XMLIOExport xmlioExport, Element root, Live live){
		publishContent(xmlioExport, root, live, IterKeys.XMLIO_XML_PUBLISH_OPERATION);
	}
	
	/**
	 * Obtiene todos los elementos pendientes y erróneos del tipo actual,
	 * genera el XML correspondiente y actualiza su estado a Processing
	 */
	public void publishContents(XMLIOExport xmlioExport, Element root, long groupId, String operationType)
	{
		try
		{
			List<Live> pendingErrorContents = LiveLocalServiceUtil.getPendingAndErrorLiveByClassNameGroupId(groupId, getClassName());
	
			if (pendingErrorContents.size() > 0)
			{
				Element list = null;
				List<Node> nodes = getNodeByClassName(root.getDocument(), getClassName());
				if (nodes.size() == 0)
					 list = root.addElement(IterKeys.XMLIO_XML_ELEMENT_LIST);
				else
				{
					Node nd = nodes.get(0);
					// Estamos en un nodo Item y queremos subir hasta el list
					list = nd.getParent().getParent();
				}
				
				for (Live pendingErrorContent : pendingErrorContents)
				{
					publishContent(xmlioExport, list, pendingErrorContent, operationType);
				}
			}
		}
		catch(Exception err)
		{
			_log.error(err);
		}
	}
	
	/*************************************************************************
	 *  					COMMON EXPORT FUNCTIONS	
	 *************************************************************************/	
	
	/**
	 * Esta es la función que se invoca desde fuera de ItemXmlIO, para evitar un problema
	 * de classLoader con la clase Live como parámetro
	 */
	public void exportContent(XMLIOExport xmlioExport, Element root, long liveId, String operationType) {

		try{
			exportContent(xmlioExport, root, LiveLocalServiceUtil.getLive(liveId), operationType);
		}
		catch(Exception err){
			_log.error("No Live item found with id " + liveId);
		}	
	}
	
	protected void exportContent(XMLIOExport xmlioExport, Element root, Live live, String operationType) 
	{
		try 
		{	
			Group group = GroupLocalServiceUtil.getGroup(live.getGroupId());
			exportContent(xmlioExport, root, group, live, operationType);
		} 
		catch (Exception e) 
		{	
			try 
			{			
				//Si es una operación delete se recupera el nombre del grupo de xmlio_live.
				if(live.getOperation().equals(IterKeys.DELETE))
				{
					exportContent(xmlioExport, root, null, live, operationType);
				}
				else
				{				
					//Si no es un delete y el grupo al que pertenece no existe, se borra el elemento de la tabla live
					if (_log.isDebugEnabled())
						_log.debug("LiveLocalServiceUtil.deleteLive");

					LiveLocalServiceUtil.deleteLive(live.getGroupId(), getClassName(), live.getGlobalId());
				}
			} 
			catch (Exception e1) 
			{
				_log.error("Can't export Content", e1);
			}
		}	
	}
	
	protected String exportContent(XMLIOExport xmlioExport, Element root, Group group, Live live, String operation)
	{
		Element pool = null;
		StringBuffer exportErrors = new StringBuffer();

		if(_log.isDebugEnabled())
			_log.debug(" Entra exportContent. live.getId(): " + live.getId() + " live.getClassNameValue(): " +live.getClassNameValue());
		
		try
		{
			String op = live.getOperation();
			if( Arrays.asList( IterKeys.MAIN_CLASSNAME_TYPES_EXPORT ).contains(live.getClassNameValue()) )
			{
				pool = root.addElement(IterKeys.XMLIO_XML_ELEMENT_POOL);
				if( operation.equals(IterKeys.XMLIO_XML_EXPORT_OPERATION) )
					op = live.getOperation().equals(IterKeys.UPDATE) ? IterKeys.CREATE : live.getOperation();
			}
			else
			{
				pool = root;
				//op = live.getOperation();
			}
			
			//1. Limpia el log de error
			LiveLocalServiceUtil.clearLog(live.getId());
			
			//2. Exportamos primero las dependencias
			//Obtiene todas las dependencias del elemento
			try
			{
				List<Live> dependantList = LiveLocalServiceUtil.getLiveByParentId(live.getId());				
				
				Long assetCategoryId = null;				
				if (null != xmlIOContext)
				{
					assetCategoryId = xmlIOContext.getAssetCategoryId();
				}				
				
				for (Live dependant : dependantList)
				{
					if (null != xmlIOContext)
					{
						// Publicación solo de qualifications
						if(xmlIOContext.isOnlyQualificationsPublication() && !dependant.getClassNameValue().equals(IterKeys.CLASSNAME_QUALIFICATION))
						{
							_log.debug(new StringBuilder("The dependant (").append(dependant.getLocalId()).append(") is not wanted to be published (only qualifications)"));
							continue;
						}
						
						// Publicación solo de productos/suscripciones
						if (xmlIOContext.isOnlyProductsPublication() && !dependant.getClassNameValue().equals(IterKeys.CLASSNAME_PRODUCT))
						{
							_log.debug(new StringBuilder("The dependant (").append(dependant.getLocalId()).append(") is not wanted to be published (only products)"));
							continue;
						}						
						
						// Si se quiere publicar una assetcategory (mediante la publicación de un vocabulario) y no es la que queremos, nos la saltamos.
						if (null != assetCategoryId && dependant.getClassNameValue().equals(IterKeys.CLASSNAME_CATEGORY) && 
						   !Long.toString(assetCategoryId).equals(dependant.getLocalId()))
						{
							_log.debug(new StringBuilder("The dependant assetcategory (").append(dependant.getLocalId()).append(") is not wanted to be published"));
							continue;
						}
					}					
					
					//Agrega recursivamente el XML de los elementos que no se encuentran publicados aún
					if (dependant != null && 
							( 
								// Publicacion
								(	operation.equals(IterKeys.XMLIO_XML_PUBLISH_OPERATION) && 
									(
										// No es una operación DONE o es DONE pero se trata de los PORTLETS, que siempre se añaden a los LAYOUTS
										!dependant.getStatus().equals(IterKeys.DONE) ||
										(live.getClassNameValue().equals(IterKeys.CLASSNAME_LAYOUT) && dependant.getClassNameValue().equals(IterKeys.CLASSNAME_PORTLET))
									)
								) || 
								(operation.equals(IterKeys.XMLIO_XML_EXPORT_OPERATION))  
							) 
						)
					{
						try
						{
							ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(dependant.getClassNameValue(), xmlIOContext);
							itemXmlIO.setExportAllDependencies(exportAllDependencies);
							if (group == null && dependant.getOperation().equals(IterKeys.DELETE))
							{
								exportErrors.append((exportErrors.length()==0?"":";") + itemXmlIO.exportContent(xmlioExport, pool, null, dependant, operation));
							}
							else
							{						
								Group dependantGroup = GroupLocalServiceUtil.getGroup(dependant.getGroupId());
								exportErrors.append((exportErrors.length()==0?"":";") +itemXmlIO.exportContent(xmlioExport, pool, dependantGroup, dependant, operation));
							}
						} 
						catch (Exception e) 
						{	
							exportErrors.append((exportErrors.length()==0?"":";") + "Export failed for element " + dependant.getLocalId());
							//_log.error("Export failed for element " + dependant.getLocalId());			
						}
					}
				}
			} 
			catch (Exception e) 
			{	
				exportErrors.append((exportErrors.length()==0?"":";") + "Cannot retrieve dependencies during export:"+ e.toString());
			}
			
			// 3. Llamamos a la creación del item si existe su grupo o lo creamos directamente si no existe
			// Si hay errores en alguna dependencia, no se exporta el item.
			if (exportErrors.length()==0)
			{
				if (group == null)
				{				
					try 
					{
						Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(live.getGroupId(), IterKeys.CLASSNAME_GROUP, String.valueOf(live.getGroupId()));
						
						if (liveGroup != null)
						{
							Map<String, String> attributes = new HashMap<String, String>();
							Map<String, String> params = new HashMap<String, String>();
				
							setCommonAttributes(attributes, liveGroup.getGlobalId(), live, op);			
					
							addNode(pool, "item", attributes, params);				
						}
						else
						{
							//Si no existe el grupo padre ni en la tabla group ni en xmlio_live. Se borra el elemento.
							LiveLocalServiceUtil.deleteLive(live.getGroupId(), getClassName(), live.getGlobalId());
						}
					} 
					catch (Exception e) 
					{				
						exportErrors.append((exportErrors.length()==0?"":";") + "Cannot export item:"+ e.toString());				
					}				
					_log.debug("INFO: " + live.getClassNameValue() + " exported");	
				}
				else
				{
					exportErrors.append((exportErrors.length()==0?"":";") + createItemXML(xmlioExport, pool, op, group, live));
				}
			}
				
			// 4. Change status and store error
			if (exportErrors.length()==0)
			{
				if(operation.equals(IterKeys.XMLIO_XML_PUBLISH_OPERATION))
				{				
					//Actualiza el estado a PROCESSING
					
					// El vocabulario de la categoria que estamos publicando no lo marcamos como PROCESSING porque no se va a actualizar aunque tenga cambios.
					// La pulicación de una categoria NO IMPLICA la actualización del vocabulario en el LIVE
					
					if (xmlIOContext==null || xmlIOContext.getAssetCategoryId()==null || !live.getClassNameValue().equalsIgnoreCase(IterKeys.CLASSNAME_VOCABULARY))
						LiveLocalServiceUtil.updateStatus(live.getGroupId(), live.getClassNameValue(), live.getGlobalId(),	IterKeys.PROCESSING, new Date());
				}
			}
			else
			{
				if(operation.equals(IterKeys.XMLIO_XML_PUBLISH_OPERATION))
				{				
					//Actualiza el estado a CORRUPT
					LiveLocalServiceUtil.updateStatus(live.getGroupId(), live.getClassNameValue(), live.getGlobalId(),	IterKeys.CORRUPT, new Date());	
					
					//Añade el error
					LiveLocalServiceUtil.setError(live.getId(), exportErrors.toString());
				}			
			}
		}
		catch(Exception excep)
		{
			String trace = "Cannot export item: "+excep.toString();	
			exportErrors.append((exportErrors.length()==0?"":";") + trace);	
			_log.error(trace);
		}
		finally
		{
			if ( Arrays.asList( IterKeys.MAIN_CLASSNAME_TYPES_EXPORT ).contains(live.getClassNameValue()) && exportErrors.length()>0 )
			{
				try
				{
					pool.getParent().remove(pool);
					ChannelControlLocalServiceUtil.setErrorLog(exportErrors.toString());
				}
				catch (Exception e)
				{
					_log.error(e);
				}
			}
		}
		
		if(_log.isDebugEnabled())
			_log.debug(" Sale exportContent. live.getId(): " + live.getId() + " live.getClassNameValue(): " +live.getClassNameValue());
		
		return exportErrors.toString();
	}
		
	public void exportContents(XMLIOExport xmlioExport, Element root, long groupId, String operation) {
		exportContents(xmlioExport, root, groupId, operation, IterKeys.XMLIO_XML_EXPORT_OPERATION, null);
	}
	
	public void exportContents(XMLIOExport xmlioExport, Element root, long groupId, String operation, String operationType, Date modifiedDate) {
	
		try {			
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			List<Live> liveList = LiveLocalServiceUtil.getUpdateAndCreateLiveByClassNameGroupId(groupId, getClassName(), modifiedDate);			
			if (liveList.size()>0){
				Element list = null;
				List<Node> nodes = getNodeByClassName(root.getDocument(), getClassName());
				if( nodes.size() == 0 )
					 list = root.addElement(IterKeys.XMLIO_XML_ELEMENT_LIST);
				else{
					Node nd = nodes.get(0);
					// Estamos en un nodo Item y queremos subir hasta el list
					list = nd.getParent().getParent();
				}
				
				
				for (Live live : liveList) {			
					exportContent(xmlioExport, list, group, live, operationType);
				}
			}
		} catch (Exception e) {			
			try {	
				//Si no existe el grupo, se borran los elementos de la tabla live
				List<Live> liveList = LiveLocalServiceUtil.getUpdateAndCreateLiveByClassNameGroupId(groupId, getClassName());
				for (Live live : liveList) {			
					LiveLocalServiceUtil.deleteLive(groupId, getClassName(), live.getGlobalId());							
				}				
			} catch (Exception e1) {
				_log.error("Can't export Item", e1);
			}
		}
	}
	
	
	/*************************************************************************
	 *  					COMMON IMPORT FUNCIONS
	 * @throws SQLException 
	 * @throws IOException 
	 *************************************************************************/
	
	public void importContents(Element item, Document doc) throws IOException, SQLException
	{
		String operation = item.attributeValue("operation");
		if (operation.equals(IterKeys.CREATE))
		{
			modify(item, doc);
		}
		else if (operation.equals(IterKeys.UPDATE))
		{
			updateDependencies(item, doc);
		}
		else if (operation.equals(IterKeys.DELETE))
		{
			delete(item);
		}
	}
	
	//Este metodo se emplea para evaluar si el contenido existe o no, sin importarlo
	public void validateContents(Element item, Document doc){
		;
	}
	
	/*************************************************************************
	 *  					COMMON DEPENDENCY FUNCTIONS	
	 *************************************************************************/
	
	/**
	 * 
	 * @param group
	 * @param root
	 * @param liveId
	 */
	protected void addDependencies(Element root, long parentId)
	{					
		List<Live> dependantList;
		try 
		{
			dependantList = LiveLocalServiceUtil.getLiveByParentId(parentId);
				
			for (Live dependant : dependantList)
			{
				if (dependant != null && (!dependant.getStatus().equals(IterKeys.DONE) || exportAllDependencies))
				{
					try
					{
						Group elemGroup = GroupLocalServiceUtil.getGroup(dependant.getGroupId());
						
						Map<String, String> attributes = new HashMap<String, String>();
						Map<String, String> params = new HashMap<String, String>();
						
						attributes.put("type", "dependency");
						attributes.put("name", dependant.getGlobalId());
						attributes.put("priority", String.valueOf(getPriority(dependant)));
						
						params.put("classname", dependant.getClassNameValue());
						params.put("groupname", elemGroup.getName());
						
						addNode(root, "param", attributes, params);
					}
					catch(Exception e)
					{
						_log.error("Export, adding dependency with className=" + dependant.getClassNameValue() + ", localId=" + dependant.getLocalId());
					}
				}
			}
		} 
		catch (SystemException e1) 
		{
			_log.error("Export, retrieving dependencies for " + parentId, e1);
		}
	}
	
	/**
	 * 
	 * @param item
	 * @param doc
	 * @return true if everything ok, false in case one or more of it's dependencies crash.
	 * @throws DocumentException
	 */
	protected boolean evaluateDependencies(Element item, Document doc) throws DocumentException
	{
		String strClassName = getClassName().substring( getClassName().lastIndexOf('.')+1 );
		_log.trace( String.format("ItemXMLIO %s: Begin evaluateDependencies", strClassName) );
		boolean res = true;
		List<Node> dependencies = getDependenciesByPriority(item, "dependency");
		
		_log.trace( String.format("ItemXMLIO %s: After getDependenciesByPriority", strClassName) );
		for(Node nodeDependency : dependencies)
		{
			Element dependency = (Element)nodeDependency;
			
			String globalId = getAttribute(dependency, "name");
			
			String className = getParamTextByName(dependency, "classname");
			String groupName = getParamTextByName(dependency, "groupname");
			
			/*
			 * Para el caso de los DLFileEntries, como se fuerza que el objeto se cree antes que su Pool (Journal o Campañas)
			 * no se importa el contenido sino que simplemente se comprueba si existe
			 */
			String impClassName = className.substring( className.lastIndexOf('.')+1 );
			_log.trace( String.format("ItemXMLIO %s (%s): Before importContent", strClassName, impClassName) );
			boolean partialResult = ChannelLocalServiceUtil.importContent(doc, className, groupName, globalId, xmlIOContext, className.equals(IterKeys.CLASSNAME_DLFILEENTRY));
			res = res && partialResult;
			_log.trace( String.format("ItemXMLIO %s (%s): After importContent", strClassName, impClassName) );
		}
		_log.trace( String.format("ItemXMLIO %s: End evaluateDependencies", strClassName) );
		return res;
	}	
	
	//El elemento no ha sido modificado, pero alguna de sus dependencias si lo ha sido.
	protected void updateDependencies(Element item, Document doc) {		
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
		
		//Creamos/modificamos sus dependencias		
		try 
		{
			if (! evaluateDependencies(item, doc))
			{
				xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), IterKeys.UPDATE, "Can't create dependency", IterKeys.ERROR, sGroupId);				
			}
			else
			{
				xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), IterKeys.UPDATE, "Done", IterKeys.DONE, sGroupId);				
			}
		} 
		catch (DocumentException e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), IterKeys.UPDATE, "Can't create dependency", IterKeys.ERROR, sGroupId);				
		}
	}
	
	/**
	 * 
	 * @param elem
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	private int getPriority (Live elem) throws PortalException, SystemException{
		
		int priority = 0;
		
		if (elem.getClassNameValue().equals(IterKeys.CLASSNAME_CATEGORY)){
			AssetCategory ac = AssetCategoryLocalServiceUtil.getAssetCategory(GetterUtil.getLong(elem.getLocalId()));
			priority = ac.getAncestors().size();
		}		
		
		return priority;
	}

	/*************************************************************************
	 *  								UTILS	
	 *************************************************************************/
	/**
	 * Obtains related pool Ids, if any
	 */
	public String[] getRelatedPoolIds(String [] liveItemIds) throws PortalException, SystemException{
		return new String[0];
	}
	
	public List<String> getRelatedPoolIdsList(String [] liveItemIds) throws PortalException, SystemException{
		return new ArrayList<String>();
	}
	
	
	/**
	 * Determines whether the item is draft or not
	 * @param groupId
	 * @param localId
	 * @return
	 */
	public boolean isDraft(long groupId, String localId){
		return false;
	}
	
	/**
	 * Obtains the Milenium ITER-ID from localId. Default is localId
	 * @param groupId
	 * @param localId
	 * @return
	 */
	public String getMileniumId(long groupId, String localId){
		return localId;
	}
	
	protected void setCommonAttributes( Map<String, String> attributes, String groupName, Live live, String operation )
	{
		attributes.put("classname", getClassName());				
		attributes.put("groupid", 	groupName);				
		attributes.put("globalid", 	live.getGlobalId());		
		attributes.put("operation", operation);
		attributes.put("id_", 		String.valueOf(live.getId()));
	}
	
	/**
	 * 
	 * @param companyId
	 * @param groupName
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	protected long getGroupId(String groupName) throws PortalException, SystemException
	{
		return GroupLocalServiceUtil.getGroup(xmlIOContext.getCompanyId(), groupName).getGroupId();
	}
	
	/**
	 * 
	 * @param groupId
	 * @param friendlyUrl
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	protected long getLayoutIdFromFriendlyURL(long groupId, String friendlyUrl) throws PortalException, SystemException
	{
		long id = LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;

		if (Validator.isNotNull(friendlyUrl))
		{
			Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, friendlyUrl);
			id = layout.getLayoutId();
		}
		
		return id;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param friendlyUrl
	 * @return
	 */
	protected String getLayoutURL(long groupId, String friendlyUrl) {
		String url = "";
		try {
			Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, friendlyUrl);
			url = layout.getFriendlyURL();
		} catch (Exception e) {}	
		
		return url;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param friendlyUrl
	 * @return
	 */
	protected String getLayoutUUID(long groupId, String friendlyUrl) {
		String id = "";
		try {
			Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, friendlyUrl);
			id = layout.getUuid();
		} catch (Exception e) {}	
		
		return id;
	}
	
	
	/*
	 * -------------
	 * XML Functions
	 * -------------
	 */	
	
	/*
	 * Get
	 */
	
	/**
	 * Devuelve el atributo name de Item
	 * 
	 * @param item
	 * @param name
	 * @return
	 */
	protected String getAttribute(Element item, String name) {		
		return item.attributeValue(name);
	}
		
	protected String getParamTxtByName(Node item, String paramName)
	{
		return getParamTxtByName(item, paramName, null); 
	}
	protected String getParamTxtByName(Node item, String paramName, String defaultValue) 
	{
		String result = defaultValue;
		Node nodeResult = item.selectSingleNode( String.format("param[@name='%s']", paramName) );
		
		if (Validator.isNotNull(nodeResult))
			result = CDATAUtil.strip(((Element) nodeResult).getTextTrim());
		
		return result; 
	}	

	/**
	 * 
	 * @param item
	 * @param paramName
	 * @return
	 */
	protected String getParamTextByName(Element item, String paramName) {
		
		return getParamTextByTypeAndName(item, "", paramName);
	}	
	
	/**
	 * Devuelve el texto del nodo hijo de item, cuyo nombre coincide con el parametro nodeName 
	 * y cuyo atributo "name" coincide con el del parametro paramName.
	 * 
	 * @param item
	 * @param nodeName
	 * @param paramName
	 * @return
	 */
	protected String getParamTextByTypeAndName(Element item, String paramType, String paramName) {
		
		for (Element param : item.elements()) {
			String name = "";
			if(!paramName.equals("")){			
				name = param.attributeValue("name");
			}
			String type = "";
			if(!paramType.equals("")){			
				type = param.attributeValue("type");
			}
			
			if (type != null && type.equals(paramType) && name != null && name.equals(paramName)) 
			{
				String nodeValue = item.selectSingleNode( String.format("param[@name='%s']", paramName)).getText();
				return CDATAUtil.strip( nodeValue.trim() );
			} 				
		}
		
		return null;
	}	
	
	/**
	 * 
	 * @param item
	 * @param paramType
	 * @return
	 */
	protected List<String[]> getParamListByType(Element item, String paramType) {
		
		return getParamListByTypeAndName(item, paramType, "");
	}
	
	/**
	 * 
	 * @param item
	 * @param paramName
	 * @return
	 */
	protected List<String[]> getParamListByName(Element item, String paramName) {
		
		return getParamListByTypeAndName(item, "", paramName);
	}
	
	/**
	 * Devuelve la lista de parametros (key, value) del nodo tipo nodeName cuyo atributo "name" el igual a paramName
	 * 
	 * @param item
	 * @param nodeName
	 * @param paramName
	 * @return
	 */
	protected List<String[]> getParamListByTypeAndName(Element item, String paramType, String paramName) {
		
		List<String[]> paramList = new ArrayList<String[]>();
		
		for (Element param : item.elements()) {
			String name = "";
			if(!paramName.equals("")){			
				name = param.attributeValue("name");
			}
			String type = "";
			if(!paramType.equals("")){			
				type = param.attributeValue("type");
			}
			
			if (type != null && type.equals(paramType) && name != null && name.equals(paramName)) {
				paramList.add(new String[]{param.attributeValue("name"), CDATAUtil.strip(param.getTextTrim())});
			} 				
		}
		return paramList;
	}
	
	/**
	 * 
	 * @param item
	 * @param paramType
	 * @return
	 */
	protected List<Element> getParamElementListByType(Element item, String paramType) {
		return getParamElementListByTypeAndName(item, paramType, "");
	}
	
	/**
	 * 
	 * @param item
	 * @param paramType
	 * @param paramName
	 * @return
	 */
	protected List<Element> getParamElementListByTypeAndName(Element item, String paramType, String paramName) {
		
		List<Element> paramList = new ArrayList<Element>();
	
		for (Element param : item.elements()) {
			
			String name = "";
			if(!paramName.equals("")){			
				name = param.attributeValue("name");
			}
			String type = "";
			if(!paramType.equals("")){			
				type = param.attributeValue("type");
			}
			
			if (type != null && type.equals(paramType) && name != null && name.equals(paramName)) {
				paramList.add(param);
			} 				
		}
		
		return paramList;
	}
	
	/**
	 * 
	 * @param parent
	 * @param paramType
	 * @return
	 */
	protected List<Node> getDependenciesByPriority(Element item, String paramType) {
		
		List<Node> nodeList = new ArrayList<Node>();
		
		//String path = "param[@type='" + paramType + "' and (@priority) and not(@priority <= preceding-sibling::param/@priority) and not(@priority <=following-sibling::param/@priority)]";		
		String path = "param[@type='" + paramType + "' and (@priority) and (not(@priority < preceding-sibling::param/@priority)) and (not(@priority < following-sibling::param/@priority))]";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(path);
		
		List<Node> node = xpathSelector.selectNodes(item);
		
		int lastLevel = 0;	
		if (node.size() > 0){		
			String maxPrior = ((Element)node.get(0)).attributeValue("priority");		
			if (maxPrior != null)	
				lastLevel = GetterUtil.getInteger(maxPrior);			
		}
		
		path = "param[@type='" + paramType + "' and (not(@priority) or @priority = '0')]";						
		xpathSelector = SAXReaderUtil.createXPath(path);
		List<Node> nodes = xpathSelector.selectNodes(item);
		nodeList.addAll(nodes);				 				
				
		for (int i = 1; i <= lastLevel; i++){
					
			path = "param[@type='" + paramType + "' and @priority='" + i + "']";			
			xpathSelector = SAXReaderUtil.createXPath(path);			
			nodes = xpathSelector.selectNodes(item);							
			nodeList.addAll(nodes);				 				
							
		}
		
		return nodeList;
	}
	
	private List<Node> getNodeByClassName(Document doc, String classnamevalue){
		String path = "//item[@classname='" + classnamevalue + "']";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(path);
		return xpathSelector.selectNodes(doc);
	}
	
	/*
	 * Add
	 */	
	
	/**
	 * Añade al root un nodo con los atributos y parametros pasados como parámetros.
	 * Devuelve dicho elemento.
	 * 
	 * @param root
	 * @param nodeName
	 * @param attributes
	 * @param params
	 * @return
	 */
	protected Element addNode(Element root, String nodeName, Map<String, String> attributes, Map<String, String> params) {
		Element ele = SAXReaderUtil.createElement(nodeName);
		
		if (attributes != null) {
			Iterator<Map.Entry<String, String>> it = attributes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> attribute = (Map.Entry<String, String>) it.next();
				ele.addAttribute(attribute.getKey(), attribute.getValue());
			}
		}
		
		if (params != null) {
			Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> paramValues = (Map.Entry<String, String>) it.next();
				
				Element param = SAXReaderUtil.createElement("param");
				param.addAttribute("name", paramValues.getKey());				
				param.setText(CDATAUtil.wrap(paramValues.getValue()));
				
				ele.add(param);								
			}
		}
		
		root.add(ele);
		
		return ele;
	}
	
	/**
	 * 
	 * @param root
	 * @param nodeName
	 * @param attributes
	 * @param innerText
	 * @return
	 */
	protected Element addNode(Element root, String nodeName, Map<String, String> attributes, String innerText) {
		Element ele = SAXReaderUtil.createElement(nodeName);
		
		if (attributes != null) {
			Iterator<Map.Entry<String, String>> it = attributes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> attribute = (Map.Entry<String, String>) it.next();
				ele.addAttribute(attribute.getKey(), attribute.getValue());
			}
		}
		
		if (innerText != null) {
			ele.setText(CDATAUtil.wrap(innerText));			
		}
		
		root.add(ele);
		
		return ele;
	}
	
	/*
	 * --------------------
	 * DEPRECATED FUNCTIONS
	 * --------------------
	 */

	@Deprecated
	protected void addNode(Element root, Map<String, String> attributes, Map<String, String> params) {				
		addNode(root, attributes, params, null, "");
	}
	
	
	@Deprecated
	protected void addNode(Element root, Map<String, String> attributes, Map<String, String> params, Map<String, String> subParams, String subParamsName) {
		Element ele = SAXReaderUtil.createElement("item");
		
		if (attributes != null) {
			Iterator<Map.Entry<String, String>> it = attributes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> attribute = (Map.Entry<String, String>) it.next();
				ele.addAttribute(attribute.getKey(), attribute.getValue());
			}
		}
		
		if (params != null) {
			Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> paramValues = (Map.Entry<String, String>) it.next();
				
				Element param = SAXReaderUtil.createElement("param");
				param.addAttribute("name", paramValues.getKey());				
				param.setText(CDATAUtil.wrap(paramValues.getValue()));
				
				ele.add(param);								
			}
		}
		
		if (subParams != null) {
			Element param = SAXReaderUtil.createElement("param");
			param.addAttribute("name", subParamsName + "s");
			
			Iterator<Map.Entry<String, String>> it = subParams.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> imageValues = (Map.Entry<String, String>) it.next();
				
				Element image = SAXReaderUtil.createElement(subParamsName);
				image.addAttribute("name", imageValues.getKey());
				image.setText(imageValues.getValue());
								
				param.add(image);
			}
			
			ele.add(param);				
		}
		
		root.add(ele);
	}
	
	
	@Deprecated
	protected String getParam(Element item, String paramName) {
		
		for (Element param : item.elements()) {
			String name = param.attributeValue("name");
			
			if (name.equals(paramName)) {
				return CDATAUtil.strip(param.getTextTrim());
			} 				
		}
		
		return null;
	}
	
	
	@Deprecated
	protected List<String[]> getParamList(Element item, String paramName) {
		
		for (Element param : item.elements()) {
			String name = param.attributeValue("name");
			
			if (name.equals(paramName)) {
				
				List<String[]> paramList = new ArrayList<String[]>();
				
				for (Element parameter : param.elements()) {
					
					paramList.add(new String[]{parameter.attributeValue("name"), parameter.getText()});
					
				}
								
				return paramList;
			} 				
		}
		return null;
	}
	
	public void updateExpandoPool( JournalArticle article ) throws PortalException, SystemException
	{
		Live liveArticle = LiveLocalServiceUtil.getLiveByLocalId(article.getGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, article.getArticleId());
		
		if(liveArticle!=null)
		{
			Company company = CompanyLocalServiceUtil.getCompany(article.getCompanyId());
			
			// Actualizo el pool en el ExpandoRow del JournalArticle
			ExpandoTable table = ExpandoTableLocalServiceUtil.getDefaultTable(company.getCompanyId(), IterKeys.CLASSNAME_JOURNALARTICLE);
			ExpandoRow row = ExpandoRowLocalServiceUtil.getRow(table.getTableId(), article.getId());
			Live liveRow = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_EXPANDOROW, String.valueOf(row.getRowId()));
			LivePoolLocalServiceUtil.createLivePool(liveArticle.getId(), liveArticle.getId(), liveRow.getId(), false);
			// Actualizo el pool en los ExpandoValue del JournalArticle
			List<ExpandoValue> expandoValues = ExpandoValueLocalServiceUtil.getRowValues(row.getRowId());
			for(ExpandoValue expandoValue : expandoValues)
			{
				try
				{
					Live liveValue = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_EXPANDOVALUE, String.valueOf(expandoValue.getValueId()));
					LivePoolLocalServiceUtil.createLivePool(liveArticle.getId(), liveRow.getId(), liveValue.getId(), false);
				}
				catch (NoSuchLiveException nsle)
				{
					_log.debug(nsle);
				}
				catch(Exception err)
				{
					_log.debug(err);
				}
			}
		}
	}
	
	public static String getInClauseSQL(String[] ids)
	{
		StringBuffer query = new StringBuffer();
		if(ids != null)
		{
			for(int i = 0; i < ids.length; i++)
			{
				String currentId = ids[i];
	
				if(i == 0)
				{
					query.append("('" + currentId + "'");
				}				
				if(i == ids.length - 1)
				{
					if(ids.length > 1)
					{
						query.append(", '" + currentId + "') ");
					}
					else
					{
						query.append(") ");
					}
				}
				if (i > 0 && i < ids.length - 1)
				{
					query.append(", '" + currentId + "'");
				}
			}
		}
		return query.toString();
	}
	
 	public static String[] splitComa(String param)
 	{
 		String[] products = null;
 		
 		if(Validator.isNotNull(param))
 			products = param.split(",");
 		
 		return products;
 	}
}

