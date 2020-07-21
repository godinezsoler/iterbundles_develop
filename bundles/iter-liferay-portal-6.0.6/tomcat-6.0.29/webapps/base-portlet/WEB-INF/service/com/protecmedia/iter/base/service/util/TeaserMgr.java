package com.protecmedia.iter.base.service.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ImageServletTokenUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutTemplate;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.service.LayoutTemplateLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.asset.model.AssetVocabularyConstants;
import com.liferay.portlet.journal.model.JournalTemplateConstants;

public class TeaserMgr 
{ 
	private static Log _log = LogFactoryUtil.getLog(TeaserMgr.class);
	
	public static final String FETCH_RSSGROUP = new StringBuffer("SELECT rssGroupId, name"						).append(
			 													"\nFROM Rss_Group "								).append(
			 													"\n\t WHERE groupId = %s %s"					).toString();
	
	public static final String FETCH_SERVICES = new StringBuffer("SELECT serviceId, title name"						).append(
																 "\nFROM Services_Service "							).append(
																 "\n\t WHERE groupId = %s %s"						).toString();
	
	public static final String FETCH_TEMPLATES = new StringBuffer(
		"SELECT templateId id_, structureId, name, description, smallImage, smallImageId, smallImageURL	\n").append(
		"FROM JournalTemplate 																			\n").append(
		"	WHERE companyId = %d																		\n").append(
		"		AND groupId = %d																		\n").append(
		"		AND structureId IN (%s)																	\n").append(
		"		AND %s 																					\n").append(
		"	ORDER BY name												  								  ").toString();	
	
	public static final String FETCH_DISCRET_TEMPLATES = new StringBuffer("SELECT templateId id_, structureId, name, description, smallImage, smallImageId, smallImageURL"		).append(
																		  "\nFROM JournalTemplate "																				).append(
																		  "\n\t WHERE companyId = %d"																			).append(
																	      "\n\t\t AND groupId = %d"																				).append(
																		  "\n\t\t AND templateId IN (%s)"																		).toString();	
	
	public static final String FETCH_PAGETEMPLATES = new StringBuffer("SELECT pageTemplateId id_, id_ idPK, name, description, imageId"				).append(
																	  "\nFROM Designer_PageTemplate "												).append(
																	  "\n\t\t WHERE groupId = %d AND"												).append(
																	  "\n\t\t defaultTemplate != 1 AND"												).append(
																	  "\n\t\t type_ = \"%s\" order by name"											).toString();	
	

	
	public static final String FETCH_DISCRET_PAGETEMPLATES = new StringBuffer("SELECT pageTemplateId id_, name, description, imageId"				).append(
																			  "\nFROM Designer_PageTemplate "										).append(
																			  "\n\t\t WHERE groupId = %d AND"										).append(
																			  "\n\t\t id_ = \"%s\" AND"												).append(
																			  "\n\t\t type_ = \"%s\""												).toString();
	
	
	public static final String FETCH_QUALIFICATIONS = new StringBuffer("SELECT qualifId, name"		).append(
																	   "\nFROM News_Qualification "	).append(
																	   "\n\t WHERE groupId = %s"	).toString();
	
	// Portlets de topics antiguos
	public static final String FETCH_CATEGORIES_OLD = new StringBuffer(
		"SELECT v.vocabularyId id, 0 parentCatId, 0 parentVocId, 																				\n").append(
		"		").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" name,															\n").append(
		"		'vocabulary' type,																												\n").append(
		"		(SELECT COUNT(1) FROM AssetCategory vchild WHERE vchild.vocabularyId = v.vocabularyId AND vchild.parentCategoryId=0) children	\n").append(	
		"FROM AssetVocabulary v																													\n").append(
		"	WHERE v.companyId = %s																												\n").append(
		"		AND v.groupId = %s																												\n").append(
		"																																		\n").append(		
		"UNION ALL																																\n").append(
		"																																		\n").append(
		"SELECT c.categoryId id, parentCategoryId parentCatId, vocabularyId parentVocId, c.name, 'category' type,								\n").append(
		"		(SELECT COUNT(1) FROM AssetCategory vchild WHERE vchild.parentCategoryId = c.categoryId) children								\n").append(
		"FROM AssetCategory c																													\n").append(
		"	WHERE c.companyId = %s																												\n").append(
		"		AND c.groupId = %s																												\n").append(
		"	ORDER BY TYPE DESC, NAME ASC																										\n").toString();
	
	public static final String FETCH_VOCABULARIES = new StringBuffer(
		"SELECT Voc.name path, Voc.*																		\n").append(
		"FROM (																								\n").append(
		"	SELECT 	v.vocabularyId id, 																		\n").append(
		"		    ").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" name,					\n").append(
		"			'").append(IterKeys.TREE_TOP_LEVEL).append("' type,										\n").append(
		"			-- Siempre rama																			\n").append(
		"			'true' isBranch,																		\n").append(
		"			(SELECT IF((SELECT COUNT(0) 															\n").append(
		"						FROM AssetCategory c2 														\n").append(
		"						WHERE c2.vocabularyId=v.vocabularyId) > 0, 'true', 'false')) hasChildren,	\n").append(
		"			-- Habilitado o no																		\n").append(
		"			%1$s																					\n").append(
		"	FROM AssetVocabulary v																			\n").append(
		"	").append(AssetVocabularyConstants.getDelegationRestriction("%2$s", "v.name")					   ).append(		
		"		WHERE v.companyId = %3$s AND v.groupId = %4$s												\n").append(																						
		"		ORDER BY v.name ASC, v.vocabularyId ASC														\n").append(	
		"	 )	Voc																							\n").toString();
	
	private static final String FETCH_DISCRETE_VOCABULARIES = new StringBuffer(
		"SELECT Voc.name path, Voc.*																		\n").append(
		"FROM (																								\n").append(
		"	SELECT 	v.vocabularyId id, 																		\n").append(
		"		    ").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" name,					\n").append(
		"			'").append(IterKeys.TREE_TOP_LEVEL).append("' type,										\n").append(
		"			v.companyId, v.groupId																	\n").append(						
		"	FROM AssetVocabulary v																			\n").append(
		"		WHERE companyId = %s AND groupId = %s														\n").append(
		"			AND v.vocabularyId IN %s																\n").append(
		"	) Voc																							\n").toString();

	private static final String FETCH_DISCRETE_CATEGORIES = new StringBuffer(
		"SELECT ITR_GET_CATEGORY_PATH(c.categoryId) path, c.categoryId id, c.name, 							\n").append(
	   	" 		'").append(IterKeys.TREE_NO_TOP_LEVEL).append("' type,										\n").append(
	   	"		c.companyId, c.groupId																		\n").append(				
	   	"FROM AssetCategory c																				\n").append(
	   	"	WHERE companyId = %s AND groupId = %s 															\n").append(
	   	"		AND c.categoryId IN %s																		\n").toString();

	public static final String FETCH_CATEGORIES = new StringBuffer("SELECT c.categoryId id, c.name, " 																		).append(
			 													   "ITR_GET_CATEGORY_PATH(c.categoryId) path, " 															).append(																 
			 													   "'" + IterKeys.TREE_NO_TOP_LEVEL + "' type, "															).append(
			 													   //Rama u hoja
																   "\n\t(SELECT IF((SELECT COUNT(0) FROM AssetCategory c2 WHERE c2.leftCategoryId > c.leftCategoryId AND "	).append(
			 													   "c2.rightCategoryId < c.rightCategoryId) > 0, 'true', 'false')) isBranch,"								).append(
			 													   "\n\t(SELECT IF((SELECT COUNT(0) FROM AssetCategory c2 WHERE c2.leftCategoryId > c.leftCategoryId AND "	).append(
							 									   "c2.rightCategoryId < c.rightCategoryId) > 0, 'true', 'false')) hasChildren,"							).append(
																   //Habilitado o no
																   "%1$s"																										).append(
																   "\nFROM AssetCategory c																				\n").append(
																   "INNER JOIN AssetVocabulary v ON c.vocabularyId = v.vocabularyId										\n").append(
																   "	").append(AssetVocabularyConstants.getDelegationRestriction("%2$s", "v.name")						   ).append(		   
																   "\nWHERE c.companyId = %3$s AND c.groupId = %4$s" 															).append(
																   //Filtro por vocabulario o por categoría
																   "%5$s"																										).append(																						
																   "\nORDER BY c.name ASC, c.categoryId ASC"																).toString();
	
	private static final String FETCH_METADATA_BY_NAME = new StringBuffer(
		"SELECT id, name, path, type 																				\n").append(
		"FROM (																										\n").append(
		"		SELECT	ITR_GET_CATEGORY_PATH(c.categoryId) path, c.categoryId id, c.name,	 						\n").append(
		"			 	'").append(IterKeys.TREE_NO_TOP_LEVEL).append("' type										\n").append(
		"		FROM AssetCategory c																				\n").append(
		"		INNER JOIN AssetVocabulary v ON v.vocabularyId = c.vocabularyId										\n").append(		
		"		").append(AssetVocabularyConstants.getDelegationRestriction("%4$s", "v.name")						   ).append(
		"			WHERE c.companyId=%1$s AND c.groupId=%2$s AND c.name LIKE '%3$s'								\n").append(
		"																											\n").append(		
		"		UNION																								\n").append(
		"																											\n").append(
		"		SELECT Voc.name path, Voc.*																			\n").append(
		"		FROM (																								\n").append(		
		"				SELECT 	v.vocabularyId id, 																	\n").append(
		"		    			").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" name,				\n").append(
		"						'").append(IterKeys.TREE_TOP_LEVEL).append("' type									\n").append(
		"				FROM AssetVocabulary v																		\n").append(
		"				").append(AssetVocabularyConstants.getDelegationRestriction("%4$s", "v.name")				   ).append(		
		"					WHERE v.companyId=%1$s 																	\n").append(
		"						AND v.groupId=%2$s 																	\n").append(
		"						AND ").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" LIKE '%3$s'	\n").append(
		"			 ) Voc																							\n").append(
		"	  ) allNodes																							\n").append(
		"ORDER BY name ASC, id ASC 																					\n").append(
		"LIMIT 0, 10																								\n").toString();
	
	private static final String FETCH_ALL_LAYOUTS = String.format(new StringBuffer(
		"SELECT l.plid, name, l.friendlyURL path																	\n").append(
		"FROM Layout l																								\n").append(
		"	WHERE l.groupId=%%1$s AND privateLayout=false															\n").append(
		"     AND l.type_ = '%s'																					\n").append(
		"	ORDER BY path ASC, name ASC \n").toString(), LayoutConstants.TYPE_PORTLET); 

	/**
	 * Se descartan los layouts temporales creados desde MLN durante la edición.
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1396?focusedCommentId=61549&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-61549
	 */
	private static final String FETCH_LAYOUTS = new StringBuffer(
		"SELECT l.uuid_ id, l.plid, name, l.friendlyURL path, 																					\n").append(
		"		'" + IterKeys.TREE_NO_TOP_LEVEL + "' type, 																						\n").append(
		"		'true' enabled, 																												\n").append(
		"		IF(((SELECT COUNT(0) FROM Layout l2 WHERE l2.groupId=%1$s AND l2.parentLayoutId=l.layoutId) > 0),'true','false') isBranch,		\n").append(
		"		IF(((SELECT COUNT(0) FROM Layout l2 WHERE l2.groupId=%1$s AND l2.parentLayoutId=l.layoutId) > 0),'true','false') hasChildren	\n").append(
		"FROM Layout l																															\n").append(
		"	WHERE l.groupId=%1$s AND privateLayout=false																						\n").append(
		"		-- Se descartan los layouts temporales creados desde MLN																		\n").append(	
		" 		AND l.friendlyurl NOT REGEXP '^/.+_[0-9]+_bak$'   																				\n").append(			
		"		AND l.parentLayoutId=%2$s																										\n").append(
		"		AND type_  NOT IN (%3$s)																										\n").append(
		"		AND l.plid NOT IN (																												\n").append(
		"							SELECT d.layoutId 																							\n").append(
		"							FROM Designer_PageTemplate d 																				\n").append(
		"								WHERE d.groupId=%1$s %4$s																				\n").append(
		"																																		\n").append(		
		"							UNION																										\n").append(
		"																																		\n").append(		
		"							SELECT catalogpage.plid																						\n").append(
		"							FROM catalog																								\n").append(
		"							INNER JOIN catalogpage ON (catalog.catalogid = catalogpage.catalogid)										\n").append(
		"								WHERE catalog.groupid=%1$s																				\n").append(
		"						 )																												\n").append(
		"	%5$s																																\n").append(
		"	ORDER BY name ASC, id ASC																											\n").toString();
	
	
	private static final String FETCH_LAYOUTS_BY_NAME = new StringBuffer("SELECT l.uuid_ id, l.plid, name, l.friendlyURL path, "																					).append(
																		 "'" + IterKeys.TREE_NO_TOP_LEVEL + "' type, "																						).append(
																		 "'true' enabled"																													).append(
																	     "\nFROM Layout l"																													).append(
																	     "\nWHERE l.groupId=%s AND privateLayout=false"																						).append(
																		 "\n\tAND type_ NOT IN ('" + IterKeys.CUSTOM_TYPE_TEMPLATE + "','"  + IterKeys.CUSTOM_TYPE_CATALOG +	"')"						).append(
																	     "\n\tAND l.name LIKE '%s'"																											).append(
																	     "\n\tAND l.plid NOT IN "																											).append(
																	     "\n\t(SELECT d.layoutId FROM Designer_PageTemplate d WHERE d.groupId=%s"															).append(
																		 "\n\tUNION"																														).append(
																		 "\n\tSELECT catalogpage.plid"																										).append(
																		 "\n\tFROM catalog"																													).append(
																		 "\n\tINNER JOIN catalogpage ON (catalog.catalogid = catalogpage.catalogid)"														).append(
																		 "\n\tWHERE catalog.groupid=%s)"																									).append(						
																		 "\nORDER BY friendlyURL ASC, id ASC LIMIT 0, 10"																					).toString(); 
	
	private static final String FETCH_LAYOUTS_BY_ID = new StringBuffer("SELECT l.uuid_ id, l.plid, name, l.friendlyURL path, "																						).append(
																	   "'" + IterKeys.TREE_NO_TOP_LEVEL + "' type, "																						).append(
																	   "'true' enabled"																														).append(
																       "\nFROM Layout l"																													).append(
																       "\nWHERE l.groupId=%s AND privateLayout=false"																						).append(
																	   "\n\tAND type_ NOT IN ('" + IterKeys.CUSTOM_TYPE_TEMPLATE + "','"  + IterKeys.CUSTOM_TYPE_CATALOG +	"')%s"							).append(
																       "\n\tAND l.plid NOT IN "																												).append(
																       "\n\t(SELECT d.layoutId FROM Designer_PageTemplate d WHERE d.groupId=%s"																).append(
																	   "\n\tUNION"																															).append(
																	   "\n\tSELECT catalogpage.plid"																										).append(
																	   "\n\tFROM catalog"																													).append(
																	   "\n\tINNER JOIN catalogpage ON (catalog.catalogid = catalogpage.catalogid)"															).append(
																	   "\n\tWHERE catalog.groupid=%s)"																										).append(													
																	   "\nORDER BY name ASC, id ASC"																										).toString(); 
	
	
	
	public static String getPageTemplatesNestedPortlet(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, com.liferay.portal.kernel.error.ServiceError, UnsupportedEncodingException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		long groupId = XMLHelper.getLongValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		//System.out.println("QUERY..>"+String.format(FETCH_DISCRET_PAGETEMPLATES, scopeGroupId, pageTemplateId));
		
		Element rootList = SAXReaderUtil.read("<rs/>").getRootElement();
		
		
		List<LayoutTemplate> layoutTemplates = LayoutTemplateLocalServiceUtil.getLayoutTemplates( IterKeys.DEFAULT_THEME, groupId ); 
		
		List<String> unsupportedLayoutTemplates = ListUtil.fromArray(PropsUtil.getArray(PropsKeys.NESTED_PORTLETS_LAYOUT_TEMPLATE_UNSUPPORTED));

		//remove unsupported templates
		for (int i = 0; i < layoutTemplates.size(); i++) 
		{
			LayoutTemplate curLayoutTemplate = (LayoutTemplate)layoutTemplates.get(i);

			if (unsupportedLayoutTemplates.contains(curLayoutTemplate.getLayoutTemplateId())) 
			{
				layoutTemplates.remove(i);
			}
		}

		for ( int i = 0; i < layoutTemplates.size(); i++ )
		{
			LayoutTemplate curLayoutTemplate = (LayoutTemplate)layoutTemplates.get(i);
			String id = curLayoutTemplate.getLayoutTemplateId();
			String name = curLayoutTemplate.getName();
			
			Element row = rootList.addElement("row");
			row.addAttribute("id", id);
			row.addAttribute("name", name);

		}
		
	
		return rootList.getDocument().asXML();
	}
	
	public static String getGroups(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(scopeGroupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type = XMLHelper.getTextValueOf(dataRoot, "@type");
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String result = "<rs/>";
		
		XPath xpathSelectedNode = SAXReaderUtil.createXPath("//" + IterKeys.TREE_NODE);
		List<Node> selectedNode = xpathSelectedNode.selectNodes(dataRoot);
		
		String queryExtended = "";
		if(selectedNode != null && selectedNode.size() > 0)
		{
			//Selected
			if (type.equalsIgnoreCase("selected"))
				queryExtended = " AND rssGroupId IN " + getInClauseSQLForLongs(selectedNode, true) +
								" ORDER BY FIELD(rssGroupId, " +  getInClauseSQLForLongs(selectedNode, false) + ")"; 
			//Available
			else
				queryExtended = " AND rssGroupId NOT IN " + getInClauseSQL(selectedNode);
		}
		
		if (type.equalsIgnoreCase("available") || (selectedNode != null && selectedNode.size() > 0)){
			//System.out.println("QUERY..>"+String.format(FETCH_RSSGROUP, scopeGroupId, queryExtended));
			Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(FETCH_RSSGROUP, scopeGroupId, queryExtended) );
			return tmplDom.asXML();
		}
		
		return result;
		
		
	}
	
	public static String getServices(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException{
	
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(scopeGroupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type = XMLHelper.getTextValueOf(dataRoot, "@type");
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	
		String result = "<rs/>";
		
		XPath xpathSelectedNode = SAXReaderUtil.createXPath("//" + IterKeys.TREE_NODE);
		List<Node> selectedNode = xpathSelectedNode.selectNodes(dataRoot);
		
		String queryExtended = "";
		if(selectedNode != null && selectedNode.size() > 0)
		{
			//Selected
			if (type.equalsIgnoreCase("selected"))
				queryExtended = " AND serviceId IN " + getInClauseSQLForLongs(selectedNode, true) +
								" ORDER BY FIELD(ServiceId, " +  getInClauseSQLForLongs(selectedNode, false) + ")"; 
			//Available
			else
				queryExtended = " AND serviceId NOT IN " + getInClauseSQL(selectedNode);
		}
		
		if (type.equalsIgnoreCase("available") || (selectedNode != null && selectedNode.size() > 0)){
			//System.out.println("QUERY..>"+String.format(FETCH_SERVICES, scopeGroupId, queryExtended));
			Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(FETCH_SERVICES, scopeGroupId, queryExtended) );
			return tmplDom.asXML();
		}
		
		return result;
	}
	
	
	static public String getDiscretPageTemplates(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String pathImage = XMLHelper.getTextValueOf(dataRoot, "@pathImage");
		ErrorRaiser.throwIfNull(pathImage, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type = XMLHelper.getTextValueOf(dataRoot, "@type");
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long scopeGroupId = XMLHelper.getLongValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
		///////////////////////////////////////////////
		Document xmlDoc = SAXReaderUtil.read(xmlData);		
		
		String xPathQuery = "//rs/row";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
		
		String id = "";
		Node node = nodes.get(0);
		Element elem = (Element)node;
		
		if (elem.attribute("id")!= null){
			id = elem.attribute("id").getValue();
		}
		
		//System.out.println("QUERY..>"+String.format(FETCH_DISCRET_PAGETEMPLATES, scopeGroupId, pageTemplateId));
		Document tmplDom = null;
		tmplDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(FETCH_DISCRET_PAGETEMPLATES, scopeGroupId, id, type) );
		
		//Se forma url con la imagen por defecto para los que no la tienen
		nodes = tmplDom.selectNodes("rs/row[@imageId='-1' or @imageId='0']");
		for (int i = 0; i < nodes.size(); i++){
			elem = (Element)nodes.get(i);
			String defaultURL = "/news-portlet/img/No_Preview.png";
			elem.addAttribute("imageURL", defaultURL);
			//elem.attribute("smallImageURL").setValue(defaultURL);
		}

		//Se forma la url para los que si tienen imagen
		nodes = tmplDom.selectNodes("rs/row[@imageId!='-1' and @imageId!='0']");
		for (int i = 0; i < nodes.size(); i++){
			elem = (Element)nodes.get(i);
			long smallImageId = elem.numberValueOf("@imageId").longValue();
			String newURL = pathImage + "/journal/template?img_id=" + smallImageId + "&t=" + ImageServletTokenUtil.getToken(smallImageId);
			elem.addAttribute("imageURL", newURL);
		}
	
		return tmplDom.asXML();
	}
	
	
	static public String getPageTemplates(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
	
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String pathImage = XMLHelper.getTextValueOf(dataRoot, "@pathImage");
		ErrorRaiser.throwIfNull(pathImage, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type = XMLHelper.getTextValueOf(dataRoot, "@type");
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long scopeGroupId = XMLHelper.getLongValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		//System.out.println("QUERY..>"+String.format(FETCH_PAGETEMPLATES, scopeGroupId, type));
		Document tmplDom = null;
		tmplDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(FETCH_PAGETEMPLATES, scopeGroupId, type) );
		
		//Se forma url con la imagen por defecto para los que no la tienen
		List<Node> nodes = tmplDom.selectNodes("rs/row[@imageId='-1' or @imageId='0']");
		for (int i = 0; i < nodes.size(); i++)
		{
			Element elem = (Element)nodes.get(i);
			String defaultURL = "/news-portlet/img/No_Preview.png";
			elem.addAttribute("imageURL", defaultURL);
			elem.addAttribute("preview", "false");
			//elem.attribute("smallImageURL").setValue(defaultURL);
		}

		//Se forma la url para los que si tienen imagen
		nodes = tmplDom.selectNodes("rs/row[@imageId!='-1' and @imageId!='0']");
		for (int i = 0; i < nodes.size(); i++)
		{
			Element elem = (Element)nodes.get(i);
			long smallImageId = elem.numberValueOf("@imageId").longValue();
			String newURL = pathImage + "/journal/template?img_id=" + smallImageId + "&t=" + ImageServletTokenUtil.getToken(smallImageId);
			elem.addAttribute("imageURL", newURL);
			elem.addAttribute("preview", "true");
		}
	
		return tmplDom.asXML();
		
	}
	
	static public String getDiscretTemplates(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String pathImage = XMLHelper.getTextValueOf(dataRoot, "@pathImage");
		ErrorRaiser.throwIfNull(pathImage, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long companyId = GroupMgr.getCompanyId();
		ErrorRaiser.throwIfFalse(companyId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type = dataRoot.valueOf("@templateType");
		ErrorRaiser.throwIfFalse(type!=null && !type.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//String prefix = (type.equals("TEASER") ? IterKeys.TEASER : IterKeys.TEMPLATE_CONTENT)+"%";
		long scopeGroupId = XMLHelper.getLongValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		long groupId = (PropsValues.JOURNAL_TEMPLATE_USE_SCOPEGROUPID) ? scopeGroupId : CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();

	
		///////////////////////////////////////////////
		Document xmlDoc = SAXReaderUtil.read(xmlData);		
		 
		String xPathQuery = "//rs/row";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
		
		String templatesIds = "";
		for (Node node : nodes) {
			Element elem = (Element)node;
			
			if (elem.attribute("articleTemplateId")!= null){
				templatesIds +=  "\"" +elem.attribute("articleTemplateId").getValue()+"\"";
			}
			if (elem.attribute("pollTemplateId")!= null){
				templatesIds +=  "\"" +elem.attribute("pollTemplateId").getValue()+"\"";
			}
			if (elem.attribute("galleryTemplateId")!= null){
				templatesIds +=  "\"" +elem.attribute("galleryTemplateId").getValue()+"\"";
			}
			if (elem.attribute("multimediaTemplateId")!= null){
				templatesIds +=  "\"" +elem.attribute("multimediaTemplateId").getValue()+"\"";
			}
		}
		
	
		Document tmplDom = null;
		
		//System.out.println("QUERY..>"+String.format(FETCH_DISCRET_TEMPLATES, companyId, groupId, templatesIds));

		tmplDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(FETCH_DISCRET_TEMPLATES, companyId, groupId, templatesIds) );
		
		// Se crea la URL de la imagen
		nodes = tmplDom.selectNodes("rs/row[@smallImageURL='']");
		String URL;
		for (int i = 0; i < nodes.size(); i++)
		{
			Element elem = (Element)nodes.get(i);
			
			String defaultURL = "/news-portlet/img/No_Preview.png";
			
			//Si no tiene imagen
			if (elem.attributeValue("smallImage").equals("0")){
				URL = defaultURL;
			} 
			//Si tiene presuntamente...
			else{
				long smallImageId = elem.numberValueOf("@smallImageId").longValue();
				//Si no existe la imagen
				if (ImageLocalServiceUtil.getImage(smallImageId) == null){
					URL = defaultURL;
				}
				//Si existe la imagen
				else{
					URL = pathImage + "/journal/template?img_id=" + smallImageId + "&t=" + ImageServletTokenUtil.getToken(smallImageId);
				}
			}
			
			//elem.attribute("imageURL").setValue(URL);
			elem.addAttribute("imageURL", URL);
		}

		return tmplDom.asXML();
		
	}
	
	static public String getTemplates(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String pathImage = XMLHelper.getTextValueOf(dataRoot, "@pathImage");
		ErrorRaiser.throwIfNull(pathImage, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long companyId = GroupMgr.getCompanyId();
		ErrorRaiser.throwIfFalse(companyId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long scopeGroupId = XMLHelper.getLongValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		 
		String type = dataRoot.valueOf("@templateType");
		ErrorRaiser.throwIfFalse(type!=null && !type.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String prefix = JournalTemplateConstants.getTemplateClause(scopeGroupId, type);
		
		long groupId = (PropsValues.JOURNAL_TEMPLATE_USE_SCOPEGROUPID) ? scopeGroupId : CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
		
		boolean articleFilter 	= GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@articleFilter") );
		boolean galleryFilter 	= GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@galleryFilter") );
		boolean pollFilter 		= GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@pollFilter") );
		boolean multimediaFilter= GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@multimediaFilter") );

		Document tmplDom = null;
		if (articleFilter || galleryFilter || pollFilter || multimediaFilter)
		{
			String structureIds	= "";
			if (articleFilter)
				//structureIds.concat( "\""+IterKeys.STRUCTURE_ARTICLE+"\"");
				structureIds +=  "\"" +IterKeys.STRUCTURE_ARTICLE+"\"";
			if (galleryFilter)
				//structureIds.concat( ",\"" +IterKeys.STRUCTURE_GALLERY+"\"");
				structureIds +=  ",\"" +IterKeys.STRUCTURE_GALLERY+"\"";
			if (pollFilter)
				//structureIds.concat( ",\"" +IterKeys.STRUCTURE_POLL+"\"");
				structureIds +=  ",\"" +IterKeys.STRUCTURE_POLL+"\"";
			if (multimediaFilter)
				//structureIds.concat( ",\"" +IterKeys.STRUCTURE_MULTIMEDIA+"\"");
				structureIds +=  ",\"" +IterKeys.STRUCTURE_MULTIMEDIA+"\"";
			
			// Se elimina la primera coma
			if (!articleFilter) 
				structureIds = structureIds.substring(1);
			
			//System.out.println("QUERY..>"+String.format(FETCH_TEMPLATES, companyId, groupId, structureIds, prefix));
			tmplDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(FETCH_TEMPLATES, companyId, groupId, structureIds, prefix) );
			
			// Se crea la URL de la imagen
			List<Node> nodes = tmplDom.selectNodes("rs/row[@smallImageURL='']");
			String URL;
			String preview;
			for (int i = 0; i < nodes.size(); i++)
			{
				Element elem = (Element)nodes.get(i);
				
				String defaultURL = "/news-portlet/img/No_Preview.png";
			
				//Si no tiene imagen
				if (elem.attributeValue("smallImage").equals("0"))
				{
					URL = defaultURL;
					preview = "false";
				}
				//Si tiene presuntamente...
				else
				{
					long smallImageId = elem.numberValueOf("@smallImageId").longValue();
					//Si no existe la imagen
					if (ImageLocalServiceUtil.getImage(smallImageId) == null)
					{
						URL = defaultURL;
						preview = "false";
					}
					//Si existe la imagen
					else
					{
						URL = pathImage + "/journal/template?img_id=" + smallImageId + "&t=" + ImageServletTokenUtil.getToken(smallImageId);
						preview = "true";
					}
				}
				
				//elem.attribute("smallImageURL").setValue(URL);
				elem.addAttribute("imageURL", URL);
				elem.addAttribute("preview", preview);
			}
	
		}
		else
		{
			tmplDom = SAXReaderUtil.read("<rs/>");
		}
		return tmplDom.asXML();
	}
	
	static public String getLayouts(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String groupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String result = SAXReaderUtil.read("<rs/>").asXML();
		
		try 
		{
			result = SQLQueries.getLayouts(Long.parseLong(groupId));
		}
		catch (Exception e) 
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return result;
		
	}
	
	static public String getQualifications(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String groupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(FETCH_QUALIFICATIONS, groupId);
		
		_log.trace(query);

		Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom(query);
		
		return tmplDom.asXML();
		
	}
	
	static public String getCategories(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String companyId = String.valueOf(GroupMgr.getCompanyId());
		ErrorRaiser.throwIfNull(companyId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String groupId = XMLHelper.getTextValueOf(dataRoot, "@groupId");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(FETCH_CATEGORIES_OLD, companyId, groupId, companyId, groupId);
		
		_log.trace(query);

		Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom(query);

		String transformXML = XSLUtil.transformXML(tmplDom.asXML(), File.separatorChar + "news-portlet" + 
																	File.separatorChar + "xsl" + 
																	File.separatorChar + "categories_hierarchy.xsl");
		
		return transformXML;
		
	}
	
	static public String getLevelByNodeId(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(scopeGroupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String originData = XMLHelper.getTextValueOf(dataRoot, "@" + IterKeys.TREE_SOURCE_DATA_PROPERTY);
		ErrorRaiser.throwIfNull(originData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String result = "<rs/>";
		String query = null;
		
		XPath xpathSelectedNode = SAXReaderUtil.createXPath("//" + IterKeys.TREE_SELECTED_NODE);
		List<Node> selectedNode = xpathSelectedNode.selectNodes(dataRoot);
		
		XPath xpathNoTopNodes = SAXReaderUtil.createXPath("//" + IterKeys.TREE_NO_TOP_LEVEL + "/" + IterKeys.TREE_NODE);
		List<Node> noTopNodes = xpathNoTopNodes.selectNodes(dataRoot);
		
		if(originData.equals(IterKeys.TREE_SOURCE_METADATA))
		{
			String companyId = String.valueOf(GroupMgr.getCompanyId());
			ErrorRaiser.throwIfNull(companyId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			String groupId = String.valueOf(GroupMgr.getGlobalGroupId());
			
			
			String enabledClause = "\n\t'true' enabled";

			if(selectedNode != null && selectedNode.size() == 1)
			{
				Node selectedNodeSource = selectedNode.get(0);
				String selectedNodeId = selectedNodeSource.getStringValue();
				
				if(selectedNodeSource != null && !selectedNodeId.isEmpty())
				{
					if(selectedNodeSource.getParent() != null)
					{
						if(noTopNodes != null && noTopNodes.size() > 0)
						{
							enabledClause = "\n\t(SELECT IF((SELECT COUNT(0) FROM AssetCategory c3 " +
											"WHERE c3.leftCategoryId > c.leftCategoryId AND c3.rightCategoryId < c.rightCategoryId " + 
											"AND c3.categoryId IN " + getInClauseSQL(noTopNodes) + ") = 0, 'true', 'false')) enabled";
						}
						
						if(selectedNodeSource.getParent().getName().equals(IterKeys.TREE_NO_TOP_LEVEL))
							query = String.format(FETCH_CATEGORIES, enabledClause, scopeGroupId, companyId, groupId, " AND c.parentCategoryId=" + selectedNodeId);
						else
							query = String.format(FETCH_CATEGORIES, enabledClause, scopeGroupId, companyId, groupId, " AND c.parentCategoryId=0 AND c.vocabularyId=" + selectedNodeId);
					}
				}
			}
			else
			{
				if(noTopNodes != null && noTopNodes.size() > 0)
				{
					enabledClause = "\n\t(SELECT IF((SELECT COUNT(0) FROM AssetCategory c " +
									"WHERE c.vocabularyId=v.vocabularyId AND " +
									"c.categoryId IN " + getInClauseSQL(noTopNodes) + ") = 0, 'true', 'false')) enabled";
				}
				query = String.format(FETCH_VOCABULARIES, enabledClause, scopeGroupId, companyId, groupId);
			}
			
			if(query != null)
			{
				_log.trace(query);
				result = PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
			}
		}
		else if(originData.equals(IterKeys.TREE_SOURCE_LAYOUT))
		{
			String queryParent = "0";
			if(selectedNode != null && selectedNode.size() == 1)
			{
				queryParent = "(SELECT l3.layoutId FROM Layout l3 WHERE l3.groupId=%s AND l3.uuid_='%s')";
				queryParent = String.format(queryParent, scopeGroupId, selectedNode.get(0).getStringValue());
			}
			
			StringBuilder excludedLayoutTypes = new StringBuilder(StringPool.APOSTROPHE).append(IterKeys.CUSTOM_TYPE_CATALOG).append(StringPool.APOSTROPHE);
			
			boolean getTemplates = GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@newsletters"), false );
			String getNewslettersLayouts = "";
			
			if(!getTemplates)
				excludedLayoutTypes.append(StringPool.COMMA_AND_SPACE).append(StringPool.APOSTROPHE).append(IterKeys.CUSTOM_TYPE_TEMPLATE).append(StringPool.APOSTROPHE);
			else
				getNewslettersLayouts = "AND d.type_!='"+ IterKeys.CUSTOM_TYPE_NEWSLETTER + "'";
			
			boolean rssEnabled = GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@autorss"), false );
			String rssLayouts = rssEnabled ? "AND l.plid IN (SELECT sectionproperties.plid FROM sectionproperties WHERE autorss = 1)" : StringPool.BLANK;
			
			query = String.format(FETCH_LAYOUTS, scopeGroupId, queryParent, excludedLayoutTypes.toString(), getNewslettersLayouts, rssLayouts);
			_log.trace(query);
			
			result = transformLayoutsName(PortalLocalServiceUtil.executeQueryAsDom(query)).asXML();
		}
		else if(originData.equals(IterKeys.TREE_SOURCE_ALL_LAYOUTS))
		{
			query = String.format(FETCH_ALL_LAYOUTS, scopeGroupId);
			_log.trace(query);
			
			result = transformLayoutsName(PortalLocalServiceUtil.executeQueryAsDom(query)).asXML();
		}

		return result;
	}
	
	
	
	static public String getNodesByName(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(scopeGroupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String originData = XMLHelper.getTextValueOf(dataRoot, "@" + IterKeys.TREE_SOURCE_DATA_PROPERTY);
		
		String result = "<rs/>";
		String query = null;
		
		XPath xpathSelectedNode = SAXReaderUtil.createXPath("//" + IterKeys.TREE_SELECTED_NODE);
		List<Node> selectedNode = xpathSelectedNode.selectNodes(dataRoot);
		
		if(originData.equals(IterKeys.TREE_SOURCE_METADATA))
		{
			if(selectedNode != null && selectedNode.size() == 1)
			{
				String companyId = String.valueOf(GroupMgr.getCompanyId());
				ErrorRaiser.throwIfNull(companyId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				String groupId = String.valueOf(GroupMgr.getGlobalGroupId());
				
				Node selectedNodeSource = selectedNode.get(0);
				String selectedNodeName = selectedNodeSource.getStringValue();
				
				if(selectedNodeSource != null && !selectedNodeName.isEmpty())
				{
					String nameQuery = selectedNodeName + "%";
					query = String.format(FETCH_METADATA_BY_NAME, companyId, groupId, nameQuery, scopeGroupId);
				}
			}
			
			if(query != null)
			{
				_log.trace(query);
				result = PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
			}
		}
		else if(originData.equals(IterKeys.TREE_SOURCE_LAYOUT))
		{
			if(selectedNode != null && selectedNode.size() == 1)
			{
				query = String.format(FETCH_LAYOUTS_BY_NAME, scopeGroupId, "%>" + selectedNode.get(0).getStringValue() + "%", scopeGroupId, scopeGroupId);
				_log.trace(query);
				
				result = transformLayoutsName(PortalLocalServiceUtil.executeQueryAsDom(query)).asXML();
			}
		}
		
		return result;
	}
	
	static public String getDiscreteNodesById(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException
	{
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
				
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfNull(scopeGroupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String originData = XMLHelper.getTextValueOf(dataRoot, "@" + IterKeys.TREE_SOURCE_DATA_PROPERTY);
		
		String result = "<rs/>";
		String query = null;
		
		XPath xpathSelectedNode = SAXReaderUtil.createXPath("//" + IterKeys.TREE_NODE);
		List<Node> selectedNode = xpathSelectedNode.selectNodes(dataRoot);
		
		if(selectedNode != null && selectedNode.size() > 0)
		{
			if(originData.equals(IterKeys.TREE_SOURCE_METADATA))
			{
				String companyId = String.valueOf(GroupMgr.getCompanyId());
				ErrorRaiser.throwIfNull(companyId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
				String groupId = String.valueOf(GroupMgr.getGlobalGroupId());
				
				//TOP LEVEL
				XPath xpathSelector = SAXReaderUtil.createXPath("//" + IterKeys.TREE_TOP_LEVEL + "/" + IterKeys.TREE_NODE);
				List<Node> topNodes = xpathSelector.selectNodes(dataRoot);
				String topClause = "";
				if (topNodes != null && topNodes.size() > 0)
				{
					topClause =  String.format(FETCH_DISCRETE_VOCABULARIES, companyId, groupId, getInClauseSQL(topNodes));
				}
				
				//NO TOP LEVELS
				xpathSelector = SAXReaderUtil.createXPath("//" + IterKeys.TREE_NO_TOP_LEVEL + "/" + IterKeys.TREE_NODE);
				List<Node> noTopNodes = xpathSelector.selectNodes(dataRoot);	
				String noTopClause = "";
				if(noTopNodes != null && noTopNodes.size() > 0)
				{
					noTopClause = String.format(FETCH_DISCRETE_CATEGORIES, companyId, groupId, getInClauseSQL(noTopNodes));
				}
				
				String union = "";
				if(topNodes != null && topNodes.size() > 0 && 
				   noTopNodes != null && noTopNodes.size() > 0)
				{
					union = "\nUNION\n";
				}
					
				query = topClause + union + noTopClause;
				query = "SELECT id, name, path, type FROM (" + query + ")allMetadata ORDER BY name ASC, id ASC";

				_log.trace(query);
				
				result = PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
			}
			else if(originData.equals(IterKeys.TREE_SOURCE_LAYOUT))
			{
				String queryIn = "";
				if(selectedNode != null && selectedNode.size() > 0)
				{
					queryIn = " AND l.uuid_ IN " + getInClauseSQL(selectedNode);
				}
				
				query = String.format(FETCH_LAYOUTS_BY_ID, scopeGroupId, queryIn, scopeGroupId, scopeGroupId);
				_log.trace(query);
				
				result = transformLayoutsName(PortalLocalServiceUtil.executeQueryAsDom(query)).asXML();
			}
		}
		
		return result;
	}
	
	static public String getInClauseSQL(List<Node> ids)
	{
		StringBuffer query = new StringBuffer();
		
		if(ids != null && ids.size() > 0)
		{
			for(int i = 0; i < ids.size(); i++)
			{
				String currentId = ids.get(i).getStringValue();
	
				if(i == 0)
				{
					query.append("('" + currentId + "'");
				}				
				if(i == ids.size() - 1)
				{
					if(ids.size() > 1)
					{
						query.append(", '" + currentId + "') ");
					}
					else
					{
						query.append(") ");
					}
				}
				if (i > 0 && i < ids.size() - 1)
				{
					query.append(", '" + currentId + "'");
				}
			}
		}
		else
		{
			query.append("('')");
		}
		return query.toString();
	}
	
	public static String getInClauseSQLForString(List<String> ids)
	{
		StringBuffer query = new StringBuffer();
		if(ids != null)
		{
			for(int i = 0; i < ids.size(); i++)
			{
				String currentId = ids.get(i);
	
				if(i == 0)
				{
					query.append("('" + currentId + "'");
				}				
				if(i == ids.size() - 1)
				{
					if(ids.size() > 1)
					{
						query.append(", '" + currentId + "') ");
					}
					else
					{
						query.append(") ");
					}
				}
				if (i > 0 && i < ids.size() - 1)
				{
					query.append(", '" + currentId + "'");
				}
			}
		}
		return query.toString();
	}
	
	public static String getInClauseSQLForLongs(List<Node> ids, boolean mainQuery){
		
		StringBuffer query = new StringBuffer();
		
		if(ids != null && ids.size() > 0)
		{
			for(int i = 0; i < ids.size(); i++)
			{
				String currentId = ids.get(i).getStringValue();
	
				if(i == 0)
				{
					query.append("(" + currentId);
				}				
				if(i == ids.size() - 1)
				{
					if(ids.size() > 1)
					{
						query.append(", " + currentId + ")");
					}
					else
					{
						query.append(")");
					}
				}
				if (i > 0 && i < ids.size() - 1)
				{
					query.append(", " + currentId);
				}
			}
		}
		else
		{
			query.append("('')");
		}
		if (mainQuery)
			return query.toString();
		else
			return query.toString().substring(1, query.toString().length()-1);
		
	}
	
	static private Document transformLayoutsName(Document initialDoc)
	{
		try
		{
			if(initialDoc != null)
			{
				XPath xpath = SAXReaderUtil.createXPath("//row/@name");
				List<Node> xmlNameList = xpath.selectNodes(initialDoc);
				if(xmlNameList != null)
				{
					for(Node xmlName:xmlNameList)
					{
						if(xmlName != null && Validator.isNotNull(xmlName.getStringValue()))
						{
							Document nameDoc = SAXReaderUtil.read(xmlName.getStringValue());
							xpath = SAXReaderUtil.createXPath("/root/name/text()");
							
							Node nameValue = xpath.selectSingleNode(nameDoc);
							if(nameValue != null && Validator.isNotNull(nameValue.getStringValue()))
								xmlName.setText(nameValue.getStringValue());
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return initialDoc;
	}
}
