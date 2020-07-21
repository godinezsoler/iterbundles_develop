package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.asset.NoSuchEntryException;
import com.liferay.portlet.asset.model.AssetCategoryConstants;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;

public class FixAssetEntryCategories extends HttpServlet
{	
	private static final long serialVersionUID = 1L;

	private static Log _log = LogFactoryUtil.getLog(FixAssetEntryCategories.class);
	
	private static boolean continueFixing = true;
	private static boolean fixing         = false;
	
	private final static String ATTR_ID = "id";
	private final static String VOC_ID  = "vocid";
	
	private static final String JOURNAL_ARTICLE_CLASSNAME_VALUE = "com.liferay.portlet.journal.model.JournalArticle";
	
	private final String GET_VOCABULARY = "SELECT vocabularyId, name FROM AssetVocabulary \n WHERE groupId = %s AND name IN ('%s')";
	private final String GET_CATEGORY   = "SELECT categoryId, parentCategoryId, name FROM AssetCategory \n WHERE groupId = %s AND parentCategoryId = %s AND name IN ('%s') AND vocabularyId = %s";
	
	@Override 
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		init(request);
	}
	
	@Override 
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		init(request);
	}
	
	// Para o iniciar el proceso
	synchronized private void init (HttpServletRequest request)
	{
		String stop = request.getParameter("s");
		
		if (Validator.isNotNull(stop))
		{
			continueFixing = false;
			_log.debug("FIX-Process stopped");
		}
		else
		{
			try
			{
				if (!fixing)
				{
					continueFixing = true;
					fixIt(request);
				}
				else
				{
					_log.error("FIX-Another instance is running");
				}				
			}
			catch(Exception e)
			{
				_log.error("FIX-Error. Unexpected error", e);
			}			
		}
	}
	
	private void fixIt(HttpServletRequest request) throws Exception
	{				
		_log.trace("FIX-In fixIt");
		
		ErrorRaiser.throwIfFalse(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, 
                				 "FIX-Error. Operation only allowed in live server");
		
		final String folderPath = request.getParameter("p");
		
		Runnable r = new Runnable(){	
            public void run() 
            {			
            	try
            	{
            		fixing = true;
            		
            		ErrorRaiser.throwIfFalse(Validator.isNotNull(folderPath), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "FIX-No folder path is given");
            		_log.debug(new StringBuilder("FIX-Received path: '").append(folderPath).append("'"));
            		
            		// Directorio con los xml
            		File folder = new File(folderPath);		
            		ErrorRaiser.throwIfFalse(folder.exists() && folder.isDirectory() && folder.canRead(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, 
            				                 new StringBuilder("FIX-Error. Directory can not be read/found '").append(folder.getAbsolutePath()).append("'").toString() );		
            		
            		File[] zipFiles = folder.listFiles(new FilenameFilter(){
            			public boolean accept(File xmlsDirectory, String name){
            				return name.toLowerCase().endsWith(XmlioKeys.ZIP_EXTENSION); 
            			}
            		});			
            			
            		// Comprobamos que hemos encontrado al menos un zip
            		ErrorRaiser.throwIfFalse(null != zipFiles && zipFiles.length > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, 
            				                 new StringBuilder("FIX-Error. No zip files found in: '").append(folderPath).append("'").toString() );
            		
            		// Obtenemos el className para un journalarticle
    				ClassName jAClassName = ClassNameLocalServiceUtil.getClassName(JOURNAL_ARTICLE_CLASSNAME_VALUE);
    				ErrorRaiser.throwIfNull(jAClassName, new StringBuilder("FIX-Error. Journal article classname not found with value: '") 
    				                                                       .append(JOURNAL_ARTICLE_CLASSNAME_VALUE).append("'").toString());
    				
    				_log.debug(new StringBuilder("FIX-Classname found: ").append(jAClassName.getPrimaryKey()).append(", '").append(jAClassName.getClassName()).append("'"));
    				
    				long globalGroupId = GroupMgr.getGlobalGroupId();
            		
            		// Se crea un directorio temporal donde descomprimir el zip
            		final File tempDirectory = XMLIOUtil.createTempDirectory();
            		
            		File zipFile = null;
            		
            		for (int z = 0; z < zipFiles.length && continueFixing; z++)
            		{
            			// Try para que aunque falle un zip siga con el siguiente
            			try
            			{
            				zipFile = zipFiles[z];
            				_log.debug(new StringBuilder("\n\nZip: '").append(zipFile.getName()).append("'. (").append(z+1).append(" of ").append(zipFiles.length).append(")"));
            				
            				ZipUtil.unzip(zipFile, tempDirectory);
            				
            				// Obtenemos los archivos xml que estan dentro del zip
            				final File[] xmlFiles = tempDirectory.listFiles
            				(new FilenameFilter(){
            						public boolean accept(File xmlsDirectory, String name){
            		    				return name.toLowerCase().endsWith(XmlioKeys.XML_EXTENSION); 
            		    			}
            					}
            				);		
            				
            				// Comprobamos que dentro del zip hay al menos 1 xml			
            				ErrorRaiser.throwIfFalse(null != xmlFiles && xmlFiles.length > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, 
            						                 new StringBuilder("FIX-Error. Zip with no xmls: '").append(folder.getAbsolutePath()).append("'").toString());
            		
            				_log.debug(new StringBuilder("FIX-xmls found in '").append(zipFile.getName()).append(" ': ").append(xmlFiles.length));		
            		
            				File xmlFile = null;
            				
            				for (int x = 0; x < xmlFiles.length && continueFixing; x++)
            				{
            					xmlFile = xmlFiles[x];            			
            					_log.debug(new StringBuilder("\n\nFIX-Xml: '").append(xmlFile.getName()).append("'. (").append(x+1).append(" of ").append(xmlFiles.length).append(")"));
            			            								
            					try
            					{
            						ErrorRaiser.throwIfFalse(xmlFile.exists() && xmlFile.isFile() && xmlFile.canRead(), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, 
							                                 new StringBuilder("FIX-Error. File can not be read/found: '").append(xmlFile.getAbsolutePath()).append("'").toString());
            						
            						Document doc = SAXReaderUtil.read(xmlFile);
            				
            						List<Node> articlesNodes = doc.getRootElement().selectNodes("//article");		
            						ErrorRaiser.throwIfFalse(null != articlesNodes && articlesNodes.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, 
            								                 "FIX-Error. No articles found in xml");
            						_log.debug(new StringBuilder("FIX-Articles found: ").append(articlesNodes.size()));
            				
            						for (int a = 0; a < articlesNodes.size() && continueFixing; a++)
            						{
            							Node articleNode = articlesNodes.get(a);
            							String articleId = XMLHelper.getTextValueOf(articleNode, "@articleid");			
            					
            							try
            							{				
            								ErrorRaiser.throwIfFalse(Validator.isNotNull(articleId), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, 
            										                 "FIX-Error. The article has no 'articleid'");			
            								_log.debug(new StringBuilder("FIX-Article: '").append(articleId).append("', (").append(a+1).append(" of ").append(articlesNodes.size()).append(")"));
            						
            								// Comprobamos que tiene categorías asignadas. Lanzará una excepción si no encuentra categorías.
            								validateCategories(globalGroupId, articleNode.selectSingleNode("./metadata/categories"));				
            						
            								// Obtenemos las categorías asignadas
            								String[] categoriesId = XMLHelper.getStringValues(articleNode.selectNodes("./metadata/categories/vocabulary//category[@set=\"1\" or @set=\"true\"]/@" + ATTR_ID));
            						
            								ErrorRaiser.throwIfFalse(null != categoriesId && categoriesId.length > 0, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, 
            														 new StringBuilder("FIX-Error. No categories found in article '").append(articleId).append("'").toString() );
            								
            								StringBuilder aux = new StringBuilder("FIX-Categories found: ")
            									.append(categoriesId.length)
            									.append(": (");
            									for (int c = 0; c < categoriesId.length; c++)
            									{
            										aux.append(categoriesId[c]);
            										
            										if (c < categoriesId.length -1)
            										{
            											aux.append(", ");
            										}
            									}
            									aux.append(")");
            								_log.debug(aux);
            						
            								// Obtenemos el artículo. Levantará excepción si no lo encuentra
            								JournalArticle jA = JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId);
            							
            								// Obtenemos el assetentry que representa al artículo. Levantará excepción si no lo encuentra. 
            								AssetEntry jAAssetEntry = AssetEntryLocalServiceUtil.getEntry(jAClassName.getClassName(), jA.getResourcePrimKey());
            						
            								// Actualizamos el assetentrycategories
            								createAssetEntryCategories(jAAssetEntry, categoriesId);						
            							}
            							
            							// Artículo
            							catch (NoSuchArticleException nSAE) 
            							{
            								_log.error(new StringBuilder("FIX-Error. Article not found with articleid: '").append(articleId).append("'"), nSAE);					
            							}
            							catch(NoSuchEntryException nSEE)
            							{
            								_log.error(new StringBuilder("FIX-Error. AssetEntry not found with articleid '").append(articleId).append("'"), nSEE);
            							}
            							catch(Exception e)
            							{
            								_log.error(new StringBuilder("FIX-Error. Unexpected error with articleid '").append(articleId).append("'"), e);
            							}
            						}
            					}
            					
            					// xml
            					catch(DocumentException dE)
            					{
            						_log.error(new StringBuilder("FIX-Error. Incorrect format file: '").append(xmlFile).append("'"), dE);
            					}
            					catch(Exception e)
            					{
            						_log.error(new StringBuilder("FIX-Error. Unexpected error with file: '").append(xmlFile).append("'"), e);
            					}
            					_log.debug(new StringBuilder("FIX-xml finished: '").append(xmlFile.getName()).append("'"));
            				}	
            				
            			// Zip
            			}catch(Exception e)
            			{
            				_log.error(new StringBuilder("FIX-Error. Unexpected error in '").append(zipFile.getName()).append("'"), e);
            			}
            			_log.debug(new StringBuilder("FIX-Zip finished: '").append(zipFile.getName()).append("'"));
            		}
            	}
            	catch(Exception e)
            	{            		
            		_log.error("FIX-Error.", e);
            	}
            	finally
    			{
            		fixing = false;
    				_log.debug("FIX-fixIt finished");
    			}
            }
		};
		
		// Lanzamos en un hilo la tarea
		final Thread t = new Thread(r);					
        t.start();	
	}	
	
	// Devuelve true si hay al menos una categoría (category) asignada al artículo. Lanza excepción si algún vocabulario no existe.
	private void validateCategories(long globalGroupId, Node categories) throws Exception
	{
		_log.trace("FIX-In validateCategories");
		
		boolean categoryAsigned = false;
		
		if( Validator.isNotNull(categories) ){
			List<Node> vocabularies = categories.selectNodes("vocabulary");
			
			String[] vocNames = XMLHelper.getStringValues(vocabularies, "@name");
			final String sql = String.format(GET_VOCABULARY, globalGroupId, StringUtil.merge(vocNames, "','"));
			/*if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(" Query to check vocabulary: ").append(sql));*/
			Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);			
			 
			for(int i = 0; i < vocabularies.size(); i++){
				Element voc = (Element)vocabularies.get(i);
				String vocId = XMLHelper.getTextValueOf(d, String.format("/rs/row[@name='%s']/@vocabularyId", voc.attributeValue("name")), "-1");
				ErrorRaiser.throwIfFalse(!vocId.equals("-1"), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, "Error. This vocabulary does not exists: '" + voc.attributeValue("name") + "'");				
	
				voc.addAttribute(ATTR_ID, vocId);
				
				// Colocamos el atributo vocabularyId en todas las categorías del vocabulario. Lo necesitaremos más adelante para comprobar las categorías.
				final List<Node> categoriesVocabulary = categories.selectNodes("vocabulary[@name='" + voc.attributeValue("name") + "']//category");
				for (int c = 0; c < categoriesVocabulary.size(); c++){
					((Element)categoriesVocabulary.get(c)).addAttribute(VOC_ID, vocId);
				}
			}			
			categoryAsigned = getCategoriesIds(globalGroupId, AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, categories.selectNodes("vocabulary/category"));
		}		
		
		// Si el artículo no tiene asignado ningún metadata (category)
		if (!categoryAsigned){
			ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, "FIX-Error. Article without metadata assigned (category)");			
		}		
	}
	
	// Devuelve true si hay al menos una categoría (category) asignada al artículo. Lanza excepción si alguna categoría no existe.
	private boolean getCategoriesIds(long globalGroupId, long parentCatId, List<Node> categories) throws SecurityException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError
	{
		// _log.debug("In getCategoriesIds");
		
		boolean categoryAsigned = false;

		if( Validator.isNotNull(categories) && categories.size() > 0){
			
			for(int i = 0; i < categories.size(); i++){
				Element cat = (Element)categories.get(i);
				String categoryName = XMLHelper.getTextValueOf(cat, "@name");
				
				// Atributo colocado anteriormente en validateCategories
				String vocabularyId = XMLHelper.getTextValueOf(cat, new StringBuilder("@").append(VOC_ID).toString());	
				
				final String sql = String.format(GET_CATEGORY, globalGroupId, parentCatId, categoryName, vocabularyId);
				/*if(_log.isDebugEnabled())
					_log.debug(new StringBuilder(" Query to check category:").append(sql));*/
				Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
				
				String catId = XMLHelper.getTextValueOf(d.getRootElement(), "/rs/row/@categoryId", "-1");				
				ErrorRaiser.throwIfFalse(!catId.equals("-1"), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, "FIX-Error. This category does not exists:'" + categoryName + "'");				
				cat.addAttribute(ATTR_ID, catId);
				
				// Comprobamos si tiene al menos una categoría asignada
				if (cat.attributeValue("set", "0").equals("1")){
					categoryAsigned = true;
				}
				
				if (getCategoriesIds(globalGroupId, Long.valueOf(catId), cat.selectNodes("category"))){
					categoryAsigned = true;
				}
			}
		}
		
		return categoryAsigned;
	}	
	
	// Creamos las relaciones entre el assetentry del journalarticle y las categorías
	private void createAssetEntryCategories(AssetEntry jAAssetEntry, String[] categoriesId) throws SystemException, PortalException, ServiceError, IOException, SQLException
	{
		_log.trace(new StringBuilder("FIX-In updateAssetEntryCategories. AssetEntry: '").append(jAAssetEntry.getPrimaryKey()).append("'"));
		
		long[] categoriesLongId = new long[categoriesId.length];
		for (int c = 0; c < categoriesId.length; c++)
			categoriesLongId[c] = Long.parseLong(categoriesId[c]);		
		
		// Actualizamos el assetentry
		AssetEntryLocalServiceUtil.importEntry(jAAssetEntry, categoriesLongId);				
	}
	
}