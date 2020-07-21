package com.protecmedia.iter.xmlio.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.NotSupportedException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.liferay.counter.service.CounterLocalService;
import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.documentlibrary.DuplicateFileException;
import com.liferay.portal.ImageTypeException;
import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.JournalIndexerMgr;
import com.liferay.portal.kernel.search.JournalIndexerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetCategoryConstants;
import com.liferay.portlet.documentlibrary.model.DLFileEntryConstants;
import com.liferay.portlet.documentlibrary.service.BinaryRepositoryLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.journal.DuplicateArticleIdException;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalStructureConstants;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.util.survey.IterSurveyModel;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.service.ImportMgrLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.JournalArticleImportLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = { Exception.class })
public class JournalArticleImportLocalServiceImpl extends JournalArticleImportLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(JournalArticleImportLocalServiceImpl.class);
	
	private final SimpleDateFormat SDF_TO_DB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	private final static String DOCUMENT_LIBRARY = "document_library";
	
	private final static int MAX_TITLE         = 300;	// Este limite viene determinad por el campo title de la tabla binary_repository.	
	private final static int MAX_URL_TITLE     = 150;		 
	private final static int MAX_LEGACY_URL    = 255;
	private final static int MAX_ARTICLE_TITLE = 300;	
	private final static int MAX_ARTICLEID     = 75;	// Longitud maxima del identificador del articulo
	private final static int MAX_FRIENDLYURL   = 255;	// Longitud maxima de la friendlyURL
	// Formato de los articleId importados: Al menos una letra seguida de números
	private final static Pattern ARTICLEID_FORMAT = Pattern.compile("^[A-Z]{2,}\\d+$");

	private final static String VOC_ID                = "vocid";
	private final static String ATTR_ID               = "id";
	private final static String ATTR_PG_TMPL_ID       = "pgtmplid";
	private final static String ATTR_QUALIF_ID        = "qualifid";
	private final static String ATTR_LAYOUT_UUID      = "layoutuuid";
	private final static String ATTR_SITE             = "site";
	private final static String STANDARD_ARTICLE      = "STANDARD-ARTICLE";		   // De momento sólo se importan artículos	
	private final static String TEMPLATE              = "FULL-CONTENT-MAIN-COMPLETE";
	private final static int    ORDER                 = 1;						       // El orden en la importación es indiferente
	private final static long	DETAULT_PARENT_LAYOUT = 0;							   // Uuid del padre de todos los layouts
	private final static String PORTLET_TYPE          = "portlet";					   // Tipo por defecto de los nuevos portlets
	private final static String IMAGE                 = "image";
	private final static String DEFAULT_PAGE_TEMPLATE = "-1";						   // Page template por defecto	
	
	/* De momento las importaciones se realizaran solo con estructura  STANDARD-ARTICLE
	 * private final String GET_JOURNAL_STRUCTURE = "SELECT COUNT(*) AS c FROM JournalStructure \n\t\t WHERE groupId = %s AND structureId = '%s'"; */
	private final String GET_QUALIFICATION = "SELECT qualifId FROM News_Qualification \n WHERE groupId = %s AND name = '%s'";
	private final String GET_PAGETEMPLATE  = "SELECT id_ FROM Designer_PageTemplate \n WHERE groupId = %s AND name='%s' AND type_ = 'article-template' ";
	private final String GET_VOCABULARY    = "SELECT vocabularyId, name FROM AssetVocabulary \n WHERE groupId = %s AND name IN ('%s')";
		 
	private final String GET_CATEGORY      = "SELECT categoryId, parentCategoryId, name FROM AssetCategory \n WHERE groupId = %s AND parentCategoryId = %s AND name IN ('%s') AND vocabularyId = %s";
	private final String GET_PRODUCT       = "SELECT productId, name FROM product \n WHERE name IN ('%s')";	
	
	private final String INSERT_ARTICLE_PRODUCT     = "INSERT INTO articleproduct (productId, articleId) \n  VALUES %s";
	private final String DELETE_LEGACYURL 			= "DELETE FROM LegacyUrl WHERE articleId = '%s'";
	
	// Nombre de la última imagen importada.
	private String lastImage = "";
	
	private final String INSERT_PAGE_CONTENT = new StringBuffer()
		.append("\n\t INSERT INTO News_PageContent (uuid_, id_, pageContentId, contentId, contentGroupId, qualificationId, layoutId, groupId, defaultSection, online_, typeContent, orden, articleModelId, modifiedDate, vigenciahasta, vigenciadesde)\n\t ")
		.append("\t\t  VALUES (ITR_UUID(), %s, ITR_UUID(), '%s', %s, '%s', '%s', %s, %s, %s, '%s', %s, %s, '%s', '%s', '%s')\n\n ").toString();
	private final String DELETE_PAGE_CONTENT = "DELETE FROM News_PageContent WHERE contentId = '%s'";
	
	private final String XPATH_VALIDATE_XML_COMPONENTS = ".//component[(not(file) and (not(@name) or @name='' or ((@name!='InternalLink' and @name!='ExternalLink' and @name!='ContentId' and @name!='Link' and @name!='Class') and (not(@index) or @index='')) )) or (file and (not(@name) or @name=''))]";
	private final String XPATH_VALIDATE_XML_COMPONENTS_2 = ".//component[(not(file) and ((@name='InternalLink' or @name='ExternalLink' or @name='ContentId' or @name='Link' or @name='Class') and @index))]";
	
	public List<String> deleteArticle(long groupId, String articleId, boolean deleteFiles) throws PortalException, SystemException, IllegalArgumentException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, IOException, SQLException, DocumentException, com.liferay.portal.kernel.error.ServiceError{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In deleteArticle(long groupId, String articleId, boolean deleteFiles)");
		
		final long tIni = Calendar.getInstance().getTimeInMillis();
		
		final JournalArticle ja = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
		
		// Borrado de las imagenes del articulo.
		List<String> deletePendingBinaries = new ArrayList<String>();
		if(deleteFiles)
			deleteImages(ja);
		else
			deletePendingBinaries = XMLIOUtil.getWebContentBinaryTitles(ja);
		
		// Borrado de los pagecontent
		deletePageContent(articleId);
		
		// Borrado del articulo
		JournalArticleLocalServiceUtil.deleteImportedArticle(ja, StringPool.BLANK, new ServiceContext(), false);
		
		//Borrado de la legacyurl
		deleteLegacyUrls(articleId);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Time deleting the article ").append(articleId).append(", ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms elapsed"));
		
		return deletePendingBinaries;
	}
	
	private void deleteLegacyUrls(String articleId) throws IOException, SQLException
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + " In deleteLegacyUrls");
		
		final long tIni = Calendar.getInstance().getTimeInMillis();

		final String sql = String.format(DELETE_LEGACYURL, articleId);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("\t Query to delete legacyurl: \n").append(sql) );
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Time deleting the legacyurl ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms."));
	}
	
	private List<String> _deleteArticle(long groupId, Node article, boolean deleteFiles) throws IOException, SQLException, PortalException, SystemException, IllegalArgumentException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, DocumentException, com.liferay.portal.kernel.error.ServiceError
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In _deleteArticle");
		
		final long tIni = Calendar.getInstance().getTimeInMillis();
		
		// Se recupera el articulo mediante el articleId
		final String articleId  = XMLHelper.getTextValueOf(article, "@articleid");
		
		List<String> deletePendingBinaries = deleteArticle(groupId, articleId, deleteFiles);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Time deleting the article ").append(articleId).append(", ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms."));
		
		return deletePendingBinaries;
	}
	
	// Se borra el articulo, sus dlfileentries, las asignaciones a páginas
	public void deleteArticle(Document xmlResult, long groupId, Node article) throws Exception 
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In deleteArticle(Document xmlResult, long groupId, Node article)");
		
		// StringBuffer en lugar de String para poder pasarlo por callback
		StringBuffer errorCode   = new StringBuffer();
		StringBuffer errorDetail = new StringBuffer();
		String articleId         = null;
		String urlTitle          = null;
		
		final long t0 = Calendar.getInstance().getTimeInMillis();		
		
		try
		{
			ErrorRaiser.throwIfNull(article,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " article is null"           );
			ErrorRaiser.throwIfNull(groupId,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " groupId is null"           );
			ErrorRaiser.throwIfNull(xmlResult, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " totailImportReport is null");

			articleId = XMLHelper.getTextValueOf(article, "@articleid");
			urlTitle  = XMLHelper.getTextValueOf(article, "metadata/properties/@urltitle");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(urlTitle) && urlTitle.length() <= MAX_URL_TITLE, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY,  XmlioKeys.DESC_ERR_INVALID_URLTITLE);
			_deleteArticle(groupId, article, true);
		}
		catch(NoSuchArticleException n)
		{
			errorCode.append(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_NOT_EXISTS);
			errorDetail.append(XmlioKeys.DESC_ERR_ARTICLE_NOT_IN_DDBB);			
			_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + " The article is not in the database", n);			
			throw n;
		}
		catch(SQLException s)
		{
			errorCode.append(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL);
			errorDetail.append(XmlioKeys.DESC_ERR_SQL_ERROR + s.getMessage() + " " + ExceptionUtils.getStackTrace(s));			
			_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + " Error in sql: " + s.getMessage() + "\n", s);			
			throw s;
		}
		catch (ORMException ormEx)
		{
			errorDetail.append(ExceptionUtils.getStackTrace(ormEx));
			
			Matcher m = Pattern.compile("CONSTRAINT `.*.` FOREIGN KEY").matcher(errorDetail);
			if(m.find())
				errorCode.append("XYZ_E_FK_CONSTRAINT_FAILS_ZYX");
			else
				errorCode.append(ServiceErrorUtil.getErrorCode(ormEx));
			
			_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + "\n" + errorDetail);			
			throw ormEx;
		}
		catch(ServiceError sE)
		{			
			errorCode.append(sE.getErrorCode());
			errorDetail.append(sE.getMessage());			
			_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + "\n" + sE);			
			throw sE;
		}
		catch(Exception t)
		{	
			errorCode.append(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED);
			errorDetail.append(t.getMessage() + "\n" + ExceptionUtils.getStackTrace(t));			
			_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + " " + t.getMessage() + "\n", t);
			throw t;
		}
		finally
		{			
			createResponse(xmlResult, errorCode, errorDetail, articleId, urlTitle, t0);		
			if(_log.isDebugEnabled())
				_log.debug(new StringBuffer(XmlioKeys.PREFIX_ARTICLE_LOG + " delete article elapsed: ").append(Calendar.getInstance().getTimeInMillis()-t0).append(" ms"));
		}
	}
	
	// Se borran los dlfileentry del articulo y, en cascada, sus asignaciones a suscripciones. 
	private void deleteImages(JournalArticle ja) throws PortalException, SystemException, DocumentException, com.liferay.portal.kernel.error.ServiceError
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In deleteImages");
		final long tIni = Calendar.getInstance().getTimeInMillis();
		long delegationId = ja.getDelegationId();
		
		// Recupera las imágenes a eliminar
		List<String> binaries = XMLIOUtil.getWebContentBinaryTitles(ja);
		
		// Elimina las imágenes
		for (String binaryTitle : binaries)
		{
			BinaryRepositoryLocalServiceUtil.deleteBinaryByTitle(binaryTitle, String.valueOf(delegationId));
		}
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Time deleting the image/s ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms."));
	}
	
	// Se borran las asignaciones del articulo a páginas
	private void deletePageContent(String articleId) throws ClassNotFoundException, ServiceError, IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException, SQLException
	{
		_log.debug(XmlioKeys.PREFIX_ARTICLE_LOG + " In deletePageContent");
		
		final long tIni = Calendar.getInstance().getTimeInMillis();

		final String sql = String.format(DELETE_PAGE_CONTENT, articleId);
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("\t Query to delete page content: \n").append(sql));
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Time deleting the pagecontent ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms."));
	}
	
	/* Importa archivos desde nodos con el siguiente formato:	
	<file maxwidth="200" maxheight="200" path="/dir1/dir2/img.jpg" kind="image|multimedia|generic" maxwidth="ancho de la imagen en píxeles" maxheight="alto de la imagen en pixeles">
		<preview path="/dir1/preview.png"/>
		<products>
			<subscription name="productoA" id="4b58f378-9bb3-11e2-9ddd-0017a44e2b78"/>
			<subscription name="productoZ" id="76e4d901-9bcc-11e2-9ddd-0017a44e2b78"/>
		</products>
	</file> */	
	private List<Map<String, String>> importFiles(List<String> deletePendingBinaries, List<Node> files, long globalGroupId, long defaultUserId, 
		                             File workingDirectory, int defaultMaxImgWidth, int defaultMaxImgHeight, long scopeGroupId, String articleId) throws Exception{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + " In importImages");		
		long tIni = Calendar.getInstance().getTimeInMillis();		
		
		List<Map<String, String>> replaces = new ArrayList<Map<String,String>>();
		
		boolean ok = true;
		Exception exception = null;
		final int filesSize = files.size();
		long delegationId = GroupLocalServiceUtil.getGroup(scopeGroupId).getDelegationId();
		
		try
		{
			// Se lee la calidad de la propiedad iter.image.scale-on-the-fly.jpgq
			final float quality = PropsValues.ITER_IMAGE_SCALE_QUALITY;
			
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(filesSize).append(" files to import"));
			
			for (int i = 0; i < filesSize; i++)
			{
				final Node fileNode = files.get(i);
				
				// Importamos la imagen de vista previa si la tiene. No hay que tratar la imagen
				final String previewPath = XMLHelper.getTextValueOf(fileNode, "preview/@path", null);
				if (Validator.isNotNull(previewPath))
				{
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("file with preview"));
					final File previewFile = new File(workingDirectory + File.separator + previewPath);
					ErrorRaiser.throwIfFalse(previewFile.exists() && previewFile.isFile() && previewFile.canRead(), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, "Preview file can not be read/found: " + previewFile.getAbsolutePath());
					
					byte[] fileContent = IOUtils.toByteArray(new FileInputStream(previewFile));
					ErrorRaiser.throwIfFalse(Validator.isNotNull(fileContent), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, XmlioKeys.DESC_ERR_READING_PREVIEW_FILE + " '" + previewFile.getName() + "'");
					
					String fileId = new StringBuilder("MG").append(CounterLocalServiceUtil.increment()).toString();
					String fileName = generateMigratedBinaryName(articleId, previewFile.getName(), fileId);
					ErrorRaiser.throwIfFalse(fileName.length() <= MAX_TITLE, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, new StringBuilder(XmlioKeys.DESC_ERR_PREVIEW_MAX_FILE_NAME).append(": '").append(previewFile.getName()).append("'").toString() );
					
					final InputStream is = new ByteArrayInputStream(fileContent);
					BinaryRepositoryLocalServiceUtil.addBinary(is, fileName, String.valueOf(delegationId));
					
					// Se anota para cambiar el nombre
					Map<String, String> replace = new HashMap<String, String>();
					replace.put("name", "/binrepository/" + previewFile.getName());
					replace.put("newName", "/binrepository/" + fileName);
					replaces.add(replace);
				}
				
				// Importamos el archivo en sí.
				final String filePath = XMLHelper.getTextValueOf(fileNode, "@path", null);		
				ErrorRaiser.throwIfNull(filePath, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, XmlioKeys.DESC_ERR_INVALID_ATTRIBUTE_PATH);
				final File file = new File(workingDirectory + File.separator + filePath);
				this.lastImage = file.getAbsolutePath();
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" file ").append(i).append(": ").append(file.getAbsolutePath()));
				ErrorRaiser.throwIfFalse(file.exists() && file.isFile() && file.canRead(), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, XmlioKeys.DESC_ERR_FILE_CAN_NOT_BE_READ + ": '" + file.getAbsolutePath() + "'");

				// Contenido del archivo
				byte[] fileContent = IOUtils.toByteArray(new FileInputStream(file));
				ErrorRaiser.throwIfFalse(Validator.isNotNull(fileContent), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, XmlioKeys.DESC_ERR_READING_FILE + ": '" + file.getName() + "'");
				
				// Indica cómo tratar el archivo (image, multimedia, generic). Por defecto, será una imagen.
				final String fileKind = XMLHelper.getTextValueOf(fileNode, "@Kind", XMLHelper.getTextValueOf(fileNode, "@kind", IMAGE));
			
				String fileId = new StringBuilder("MG").append(CounterLocalServiceUtil.increment()).toString();
				String fileName = generateMigratedBinaryName(articleId, file.getName(), fileId);
				ErrorRaiser.throwIfFalse(fileName.length() <= MAX_TITLE, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, new StringBuilder(XmlioKeys.DESC_ERR_MAX_FILE_NAME).append(": '").append(file.getName()).append("'").toString() );
				
				Map<String, String> replace = new HashMap<String, String>();
				replace.put("name", "/binrepository/" + file.getName().toLowerCase());
				replace.put("newName", "/binrepository/" + fileName);
				
				// Si se quiere tratar como imagen
				if(fileKind.equalsIgnoreCase(IMAGE))
				{
					checkImageFormat(fileContent);
					
					final int maxWidth  = GetterUtil.getInteger( XMLHelper.getTextValueOf(fileNode, "@maxwidth"),  defaultMaxImgWidth );
					final int maxHeight = GetterUtil.getInteger( XMLHelper.getTextValueOf(fileNode, "@maxheight"), defaultMaxImgHeight);

					// Escalamos la imagen
					final long scaleIni = Calendar.getInstance().getTimeInMillis();
					if (maxWidth > 0 && maxHeight > 0)
					{							
						fileContent = ImageLocalServiceUtil.scaleImage(fileContent, (int)maxWidth, (int)maxHeight, quality);
						
						// Nos quedamos con el tamaño de la imagen para modificar a posteriori el journalarticle.content (xml)						
						final Image image = ImageLocalServiceUtil.getImage(fileContent);
						
						replace.put("width", String.valueOf(image.getWidth()));
						replace.put("height", String.valueOf(image.getHeight()));
						
						if (_log.isDebugEnabled())
							_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" image ").append(file.getName()).append(" escaled to ").append(image.getWidth()).append("x").append(image.getHeight()).append(" Time ").append(Calendar.getInstance().getTimeInMillis() - scaleIni).append(" ms."));
					}
					else
					{
						if (_log.isDebugEnabled())
							_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" image ").append(file.getName()).append(" not scaled"));
					}
				}

				replaces.add(replace);
				
				// Importación del binario
				final InputStream is = new ByteArrayInputStream(fileContent);
				BinaryRepositoryLocalServiceUtil.addBinary(is, fileName, String.valueOf(delegationId));
				
				// Importante, los ids de los productos se añaden al xml en la funcion validateSubscriptions, no vienen desde el origen
				final String[] productsId = XMLHelper.getStringValues(fileNode.selectNodes("subscriptions/subscription/@" + ATTR_ID));
				
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(productsId == null ? "0 " : productsId.length).append(" products found"));
				
				// Añadir a la tabla fileentryproduct las duplas (productId, fileEntryId)
				if (Validator.isNotNull(productsId))
				{
					long prodIni = Calendar.getInstance().getTimeInMillis();
					
					StringBuilder xmlProducts = new StringBuilder("<param><article id=\"").append(articleId).append("\"/>").append("<target id=\"").append(fileId).append("\"/>");
					for (String productId : productsId)
					{
						xmlProducts.append("<product id=\"").append(productId).append("\"/>");
					}
					xmlProducts.append("</param>");

					BinaryRepositoryLocalServiceUtil.setBinaryProducts(xmlProducts.toString(), true);

					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time elapsed adding image and its suscriptions ").append(Calendar.getInstance().getTimeInMillis()-prodIni).append(" ms."));
				}
			}
		}
		catch(ImageTypeException i)
		{
			ok = false;			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Image corrupted: ").append(this.lastImage).toString(), i);
			exception = i;
		}
		catch(Exception e)
		{
			ok = false;
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("error importing the fileentries"), e);			
			exception = e;
		}
		finally
		{	
			// Importación correcta: Borra los antiguos binarios (No entran en conflicto con los nuevos porque generan nuevo ID) 
			if (ok)
			{
				if (null != deletePendingBinaries && deletePendingBinaries.size() > 0)
				{
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Deleting old fileentries"));
					for (String binaryTitle : deletePendingBinaries)
					{
						BinaryRepositoryLocalServiceUtil.deleteBinaryByTitle(binaryTitle, String.valueOf(delegationId));
					}
				}
				
			// Importación incorrecta: Elimina los nuevos que haya insertado (No entran en conflicto con los antiguos porque generan nuevo ID)
		    }
			else
			{
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Deleting new fileentries"));
				for (Map<String, String> binary : replaces)
				{
					BinaryRepositoryLocalServiceUtil.deleteBinaryByTitle(binary.get("newName"), String.valueOf(delegationId));
				}
				throw exception;
			}							
		}
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time int importFiles ").append(filesSize).append(" article file/s: ").append(Calendar.getInstance().getTimeInMillis() - tIni).append(" ms\n"));
		
		return replaces;		
	}
	
	private String generateMigratedBinaryName(String articleId, String originalName, String fileId) throws SystemException
	{
		int extensionIndex = originalName.lastIndexOf(StringPool.PERIOD);
		
		return new StringBuilder(originalName.substring(0, extensionIndex))
					.append(StringPool.UNDERLINE).append(articleId)
					.append(StringPool.UNDERLINE).append(fileId)
					.append(originalName.substring(extensionIndex))
					.toString();
	}
	
	private void checkImageFormat(byte[] fileContent) throws Exception
	{
		if (Validator.isNull(fileContent) || fileContent.length < 4)
			throw new ImageTypeException();
		
		boolean error = false;
		String[] hexArray = new String[4];
		
		for (int i = 0; i < 4; i++)
		{
			hexArray[i] = Integer.toHexString(fileContent[i]);
		}
		
		if ( (Arrays.equals(DLFileEntryConstants.TIFF_SIGNAUTRE_1, hexArray) || Arrays.equals(DLFileEntryConstants.TIFF_SIGNAUTRE_2, hexArray)))
			error = true;
		
		if (error)
		{
			throw new NotSupportedException();
		}
	}
	
	// Inserta suscripciones para artículos
	private void insertArticleProducts(String articleId, String[] productsId) throws Exception
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + " In insertArticleProducts");
		
		long tIni = Calendar.getInstance().getTimeInMillis();
		
		StringBuffer aux = new StringBuffer();
		
		for (int p = 0; p < productsId.length; p++)
		{
			aux.append("('" + productsId[p] + "', '" + articleId + "')");
			if (p < productsId.length -1)
				aux.append(", ");
		}
		
		final String sql = String.format(INSERT_ARTICLE_PRODUCT, aux);
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Sql to insert in articleproduct: ").append(sql));
		try{
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}catch(Exception e){
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Error inserting the articleproduct for the article: '").append(articleId).append("'"));
			throw e;
		}
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time assigning the suscriptions: ").append(Calendar.getInstance().getTimeInMillis() - tIni).append(" ms\n"));
	}

	private void importSections(List<Node> sections, String journalArticleId, long globalGroupId, Date createDate) 
					throws ServiceError, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, 
                           InvocationTargetException, SystemException, IOException, SQLException
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + " In importSections");
		
		long tIni = Calendar.getInstance().getTimeInMillis();
		
		/* Ejemplo de los nodos que llegan
		<section order="numero-de-orden" pagetemplate="nombre-del-page-template" qualification="nombre-calificacion" 
				 url="friendly-url-de-la-pagina" (layout) 
				 defaultSection="1|0|true|false" 
				 datefrom="yyyy/MM/dd HH:mm:ss" 
				 dateto="yyyy/MM/dd HH:mm:ss"/> */
		
		final SimpleDateFormat sDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		int numSections = sections.size();
		CounterLocalService counter = null; 
		
		for (int s = 0; s < numSections; s++)
		{
			Node section = sections.get(s);
			
			// Las propiedades sectionId, templateId y qualificationId fueron añadidas al xml al realizar validaciones del mismo
			long scopeGroupId      = XMLHelper.getLongValueOf(section, "@" + ATTR_SITE);
			String layoutUuid      = XMLHelper.getTextValueOf(section, "@" + ATTR_LAYOUT_UUID);			 
			long templateId        = GetterUtil.getLong( XMLHelper.getTextValueOf(section, "@" + ATTR_PG_TMPL_ID), -1L);
			String qualificationId = XMLHelper.getTextValueOf(section, "@" + ATTR_QUALIF_ID);
			
			// Fecha de inicio y fin de vigencia del artículo.
			final String jADateValidityFrom = XMLHelper.getTextValueOf(section, "@datefrom", null);
			final String jADateValidityTo   = XMLHelper.getTextValueOf(section, "@dateto"  , null);			
			validateValidity(sDF, createDate, jADateValidityFrom, jADateValidityTo);		
			
			boolean isDefaultSection = GetterUtil.getBoolean( XMLHelper.getTextValueOf(section, "@defaultSection"), false);

			// Si el atributo online no existe su valor por defecto es TRUE
			boolean online = GetterUtil.getBoolean( XMLHelper.getTextValueOf(section, "@online"), true );
			
			if( Validator.isNull(counter) )
			{
				// Para evitar referencias cruzadas, llamamos a la funcion importPageContent de forma dinámica				
				Class<?> clazz = Class.forName("com.protecmedia.iter.news.service.PageContentLocalServiceUtil");
				String methodName = "getCounter";							
				Method[] methods  = clazz.getDeclaredMethods();							
				Method m = null;
				
				// Buscamos el metodo
				for (int i = 0; i < methods.length; i++)
				{
				   if (methods[i].getName().equals(methodName))
				   {
				      m = methods[i];
				      break;
				   }
				}
				
				ErrorRaiser.throwIfNull(m, XmlioKeys.PREFIX_ARTICLE_LOG, new StringBuilder("Method '").append(methodName).append("' not found to create the pagecontent").toString());
				
				// Ejecutamos el metodo
				counter = (CounterLocalService) m.invoke(clazz);
			}
			
			final long id_ = counter.increment();
			
			//(, , , , , , , groupId, defaultSection, online_, typeContent, orden, articleModelId, modifiedDate, vigenciahasta, vigenciadesde)					
			final String sql = String.format(INSERT_PAGE_CONTENT, id_, journalArticleId, globalGroupId, qualificationId, 
			         layoutUuid, scopeGroupId, isDefaultSection, online, STANDARD_ARTICLE, ORDER, templateId, sDF.format(new Date()), 
			         jADateValidityTo, jADateValidityFrom);
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("\t Query to insert page content: \n\t\t").append(sql));
			PortalLocalServiceUtil.executeUpdateQuery(sql);

		}		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time importing ").append(numSections).append(" sections, ").append(Calendar.getInstance().getTimeInMillis() - tIni).append(" ms\n"));
	}
	
	/* Importa artículos. Tiene la lógica para detectar si se borra previamente el artículo, si hay que actualizarlo... 
	   Modifica el xml pasado para añadir el detalle de la importacion del artículo
		<d id="" errorCode="" errordetail="">
			<subject><![CDATA[<a id=""/>]]></subject>
		</d>
			
		Donde errorCode es el codigo de error si hubiese dado error la importacion y subject es la identificacion del artículo */
	public void importArticle(Document xmlResult, Node article, String scopeGroupId, long globalGroupId, long defaultUserId, long jaClassNameId, 
		                      String expandoTableId, String expColGrp, String expColMeta, File workingDirectory, Date importationStart, 
        					  Date importationFinish, int maxImgWidth, int maxImgHeight, boolean legacyIsEncoded, boolean ifArticleExists,
        					  String ifNoCategory, boolean ifLayoutNotExists, String ifNoSuscription) throws Exception
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + " In importArticle");		
		
		final long t0 = Calendar.getInstance().getTimeInMillis();		
		
		String[] errorCodeAndErrorDetail = null;
		// StringBuffer en lugar de Strings para poder pasarlos por callback
		StringBuffer errorCode   = new StringBuffer("");
		StringBuffer errorDetail = new StringBuffer("");
		String articleId         = null;    
		String urlTitle          = null;
		boolean articleExists    = false;
		
		try
		{			
			ErrorRaiser.throwIfNull(article,          IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " article is null"         );		
			ErrorRaiser.throwIfNull(scopeGroupId, 	  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " groupId is null"         );
			ErrorRaiser.throwIfNull(globalGroupId, 	  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " globalGroupId is null"   );
			ErrorRaiser.throwIfNull(defaultUserId, 	  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " defaultUserId is null"   );
			ErrorRaiser.throwIfNull(jaClassNameId, 	  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " jaClassNameId is null"   );
			ErrorRaiser.throwIfNull(expandoTableId,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " expandoTableId is null"  );
			ErrorRaiser.throwIfNull(expColGrp, 	      IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " expColGrp is null"       );
			ErrorRaiser.throwIfNull(expColMeta, 	  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " expColMeta is null"      );
			ErrorRaiser.throwIfNull(workingDirectory, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + " workingDirectory is null");
			
			articleId = XMLHelper.getTextValueOf(article, "@articleid");
			urlTitle = XMLHelper.getTextValueOf(article, "metadata/properties/@urltitle");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(articleId), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY, XmlioKeys.DESC_ERR_INVALID_ARTICLEID);
			
			// Comprueba que el formato del articleId sea de al menos una letra seguida de dígitos
			Matcher matcher = ARTICLEID_FORMAT.matcher(articleId);
			ErrorRaiser.throwIfFalse(matcher.find(), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.DESC_ERR_ARTICLEID_FORMAT);
			
			// Comprueba que la longitud no supere los 75 caracteres
			ErrorRaiser.throwIfFalse(Validator.isNotNull(articleId) && articleId.length() <= MAX_ARTICLEID, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.DESC_ERR_INVALID_ARTICLEID);
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(urlTitle) && urlTitle.length() <= MAX_URL_TITLE, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY, XmlioKeys.DESC_ERR_INVALID_URLTITLE2);
			
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" working with the article: ").append(articleId));
			
			// Comprobamos que no se repite ninguna imagen para un artículo (ticket: 9613)
			checkNotRepeatedImg(article);
			
			List<String> deletePendingBinaries = null;
			articleExists = journalArticleExists(globalGroupId, articleId);			
			if(articleExists){
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Article '").append(articleId).append("' found in the database")); 
				// Se borra el artículo si ya estaba en base de datos
				if (!ifArticleExists){
					deletePendingBinaries = _deleteArticle(globalGroupId, article, false);
				// Error
				}else{
					ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED, XmlioKeys.DESC_ERR_ARTICLE_IN_DDBB);
				}
			}
			_importNewArticle(articleExists, errorCode, errorDetail, workingDirectory, scopeGroupId, globalGroupId, defaultUserId, jaClassNameId, expandoTableId, 
				              expColGrp, expColMeta, article, importationStart, importationFinish, maxImgWidth, maxImgHeight, legacyIsEncoded, 
				              ifNoCategory, ifLayoutNotExists, ifNoSuscription, deletePendingBinaries);

		// Capturamos las excepciones para anotar el codigo de error en el xml y luego volvemos a lanzar la excepción para que se realice el rollback
		}
		catch(Exception e)
		{
			// Elimina el artículo si ya se ha importado y no existía previamente
			if (!articleExists && journalArticleExists(globalGroupId, articleId))
			{
				_deleteArticle(globalGroupId, article, false);
			}
			// Registra el error
			errorCodeAndErrorDetail = getErrorCodeAndErrorDetails(e, articleId);
			errorCode.append(errorCodeAndErrorDetail[0]);
			errorDetail.append(errorCodeAndErrorDetail[1]);
			throw e;
		}
		finally
		{
			createResponse(xmlResult, errorCode, errorDetail, articleId, urlTitle, t0);
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuffer(XmlioKeys.PREFIX_ARTICLE_LOG + " importArticle elapsed: ").append(Calendar.getInstance().getTimeInMillis()-t0).append(" ms"));
		}
	}

	// Da de alta un articulo y todas sus relaciones (No crea secciones, categorias ni suscripciones nuevas)
	private void _importNewArticle(boolean articleExists, StringBuffer errorCode, StringBuffer errorDetail, File workingDirectory, String groupId, 
		                           long globalGroupId, long defaultUserId, long jaClassNameId, String expandoTableId, String expColGrp, 
		                           String expColMeta, Node article, Date importationStart, Date importationFinish, 
		                           int maxImgWidth, int maxImgHeight, boolean legacyIsEncoded, String ifNoCategory, 
		                           boolean ifLayoutNotExists, String ifNoSuscription, List<String> deletePendingBinaries) throws Exception{		
		
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + " In _importNewArticle");
		
		final long t0 = Calendar.getInstance().getTimeInMillis();	
		
		// Metadata, Properties (estos datos se comprueban en las siguientes validaciones)
		// Viene seguro el id y la urTitle del artículo porque ya se han comprobado en la función importArticle
		final String articleId = XMLHelper.getTextValueOf(article, "@articleid");
		
		final String title = XMLHelper.getTextValueOf(article, "./metadata/properties/@title");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(title) && title.length() <= MAX_ARTICLE_TITLE, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY, XmlioKeys.DESC_ERR_INVALID_TITLE);
		
		// Ya se ha validado en importArticle
		final String urlTitle  = XMLHelper.getTextValueOf(article, "./metadata/properties/@urltitle");
				
		final Node content = article.selectSingleNode("content");
		ErrorRaiser.throwIfNull(content, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY, XmlioKeys.DESC_ERR_ARTICLE_CONTENT);
		
		// Obtenemos y validamos (son obligatorias) las fechas de creación y modificación del artículo
	    final Date createDate   = validateDate(XMLHelper.getTextValueOf(article, "./metadata/properties/@createdate"),   "createdate",   SDF);
	    final Date modifiedDate = validateDate(XMLHelper.getTextValueOf(article, "./metadata/properties/@modifieddate"), "modifieddate", SDF);	    
	    validateCreateAndModifiedDate(importationStart, importationFinish, createDate, modifiedDate);
		
		// Opcional, si viene ha de ser <= 255
		String legacyUrl  = XMLHelper.getTextValueOf(article, "./metadata/properties/@legacyurl");		
		if (null != legacyUrl )
		{
			if(legacyUrl.length() > MAX_LEGACY_URL)
				ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.PREFIX_ARTICLE_LOG + XmlioKeys.DESC_ERR_LEGACYURL);
			if( !legacyUrl.startsWith(StringPool.SLASH) || legacyUrl.startsWith(StringPool.DOUBLE_SLASH) || legacyUrl.endsWith(StringPool.SLASH)  || legacyUrl.contains(StringPool.POUND))
				ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.PREFIX_ARTICLE_LOG + com.liferay.portal.kernel.error.IterErrorKeys.XYZ_BADURL_ZYX);
				
		}	    
	    
		/**
		 *	Se indexan al final del proceso de importación del paquete ZIP.
		 *	Independientemente del valor de la propiedad en el XML se pasará un false para evirtar la indexación en este punto
		 **/
		//final boolean indexable = GetterUtil.getBoolean(XMLHelper.getTextValueOf(article, "./metadata/properties/@indexable"), true);
		boolean indexable = false;
		
		boolean commentable = GetterUtil.getBoolean(XMLHelper.getTextValueOf(article, "./metadata/properties/@commentable"), true);
		
		boolean avoidads = GetterUtil.getBoolean(XMLHelper.getTextValueOf(article, "./metadata/properties/@avoidads"), false);
		
		/* Por ahora la estructura del artículo será siempre fija. Cuando esto cambie, habrá que validar su estructura:
		final String structure  = XMLHelper.getTextValueOf(article, "./metadata/properties/@structure", STANDARD_ARTICLE); */
//		final String structure = STANDARD_ARTICLE;
		// Comprobamos que el artículo tiene estructura correcta (JournalStructure)
		//validateStructure(globalGroupId, structure);
		
		// Las validaciones comprueban y modifican el documento xml añadiendo algunos ids.
		
		// Components
		validateComponents(article);

		// Procesa el atributo 'sites' de los nodos <content> y 'site' de nos nodos <section>
		long delegationId = GroupLocalServiceUtil.getGroup(Long.parseLong(groupId)).getDelegationId();
		translateVirtualHosts(article, groupId, delegationId);
		
		// Sections
		Set<String> sectionGroupIds = validateSections(ifLayoutNotExists, defaultUserId, groupId, article.selectNodes("./metadata/sections/section"));

		// Categories
		validateCategories(errorCode, errorDetail, ifNoCategory, globalGroupId, article.selectSingleNode("./metadata/categories"), delegationId);

		// Suscripciones del artículo. El último parámetro cuenta el número de suscripciones del artículo. 		
		validateSubscriptions(errorCode, errorDetail, article.selectNodes("./metadata/subscriptions/subscription"), ifNoSuscription);
		 // Suscripciones de los productos.
		validateSubscriptions(null, null, article.selectNodes("./content/component/file/subscriptions/subscription"), null);

		// Si está activado el nuevo sistema de encuesta, busca nodos Question y Answer y les añade identificadores
		if (IterSurveyUtil.isEnabled(Long.parseLong(groupId)))
		{
			List<Node> questions = content.selectNodes("//component[@name='Question']");
			for (Node question : questions)
			{
				((Element) question).addAttribute("questionid", UUID.randomUUID().toString());
			}
			List<Node> answers = content.selectNodes("//component[@name='Answer']");
			for (Node answer : answers)
			{
				((Element) answer).addAttribute("choiceid", UUID.randomUUID().toString());
			}
		}
		
		// Importación del artículo. En el nodo "properties" están los parámetros necesarios para crear un artículo.
		// Generar el xml de contenido del artículo a partir del nodo content. (XSL) Posteriormente se añadirán el tamaño de las imágenes		
		String transformedContent = getArticleContent(content, globalGroupId);		
		ErrorRaiser.throwIfFalse( Validator.isNotNull(transformedContent), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, XmlioKeys.DESC_ERR_XSL);
		
		// La función para añadir un artículo está interceptada en el MyJournalArticleLocalService, que en caso de ser una encuesta hace un 
		// tratamiento especial y añade el artículo a XMLIO_LIVE. Para evitar esto hay que llamar a la nueva función importArticle.
		// Parámetros de la llamada al JournalArticleLocalServiceUtil.importArticle
		//   userId : usuario por defecto del sistema
		//	 groupId : id del grupo global
		//	 articleId : el que viene en el xml de importación
		//	 autoArticleId : FALSE
		//	 version : 1.0
		//	 title : el que viene en el xml de importación
		//	 description : vacío			
		//	 content : el que viene en el xml de importación transformado con la XSL
		//	 type : general														 
		//	 structureId : el que viene en el xml de importación
		//	 templateId : el que viene en el xml de importación
		//	 displayDateMonth : mes
		//	 displayDateDay : dia
		//	 displayDateYear : año
		//	 displayDateHour : hora
		//	 displayDateMinute : minuto
		//	 expirationDateMonth : mes
		//	 expirationDateDay : dia
		//	 expirationDateYear :año
		//	 expirationDateHour : hora
		//	 expirationDateMinute : minuto
		//	 neverExpire : TRUE
		//	 reviewDateMonth : mes
		//	 reviewDateDay : dia
		//	 reviewDateYear : año
		//	 reviewDateHour :hora
		//	 reviewDateMinute : minuto
		//	 neverReview : FALSE
		//	 indexable : FALSE
		//	 smallImage : avoidads
		//	 smallImageURL : vacío
		//	 smallFile : NULL
		//	 images :  NULL
		//	 articleURL : vacío
		//   serviceContext
		
		// Importacion de las CATEGORIAS		
		// En el nodo "categories" se define la categorización del artículo. Incluyendo las categorías, de las asignadas, que son principales.
		//   Obtener los categoryId para las categorías del xml.
		//   La llamada a AssetEntryLocalServiceUtil.updateEntry está interceptada en MyAssetEntryLocalService, usar AssetEntryLocalServiceUtil.importEntry
		//   Parámetros de la llamada a AssetEntryLocalServiceUtil.importEntry
		//     userId : usuario por defecto del sistema.
		//     groupId : id del grupo global
		//     className : journalarticle_classnameId
		//     classPK : journalarticle.resourcePrimKey
		//     categoryIds : long[] ids de las categorías
		//     tagNames  : NULL		
		String[] categoriesId = XMLHelper.getStringValues(article.selectNodes("./metadata/categories/vocabulary//category[@set=\"1\" or @set=\"true\"]/@" + ATTR_ID));		
		 
		long[] categoriesLongId = new long[categoriesId.length];
		for (int c = 0; c < categoriesId.length; c++)
			categoriesLongId[c] = Long.parseLong(categoriesId[c]);
		
		// Se pasa un objeto de tipo ServiceContext como el siguiente 
		ServiceContext serviceContext = new ServiceContext();
        serviceContext.setAddCommunityPermissions(commentable);
        serviceContext.setAddGuestPermissions(true);
        serviceContext.setScopeGroupId(Long.parseLong(groupId));
        serviceContext.setAssetCategoryIds(categoriesLongId);
        serviceContext.setModifiedDate(modifiedDate);
        
        final Calendar cal = Calendar.getInstance();        
        long tIni = cal.getTimeInMillis();
        
	    int iDate = cal.get(Calendar.DATE);
	    int iYear = cal.get(Calendar.YEAR);
	    int iHora = cal.get(Calendar.HOUR_OF_DAY);
	    int iMinute = cal.get(Calendar.MINUTE);        
	    int iMonth = cal.get(Calendar.MONTH);
	    
	    // Si está habilitado el nuevo sistema de encuestas y contiene encuestas válidas,
		// se establece el structureId a STANDARD-POLL y el templateId a FULL-CONTENT-POLL
	    List<IterSurveyModel> surveys = null;
	    String structure = STANDARD_ARTICLE;
	    String template = TEMPLATE;
		if (IterSurveyUtil.isEnabled(Long.parseLong(groupId)) && (surveys = IterSurveyModel.processArticleSurveys(articleId, transformedContent)).size() > 0)
		{
			structure = JournalStructureConstants.STRUCTURE_POLL;
			template = "FULL-CONTENT-POLL";
		}
	    
		JournalArticle jA = JournalArticleLocalServiceUtil.importArticle(defaultUserId, globalGroupId, articleId, false, 1.0, title, "", 
																transformedContent, "general", structure, template, 
																iMonth,iDate, iYear, iHora, iMinute, 
																iMonth,iDate, iYear, iHora, iMinute, 
																true, 
																iMonth,iDate, iYear, iHora, iMinute,
																false, indexable, avoidads, "", null, null, "", serviceContext);      
		
		// Crea / Actualiza / Elimina las encuestas del artículo
		if (IterSurveyUtil.isEnabled(Long.parseLong(groupId)))
			IterSurveyModel.saveArticleSurveys(articleId, surveys);
		
		ErrorRaiser.throwIfNull(jA, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED, XmlioKeys.DESC_ERR_IMPORTING_ARTICLE);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" article added, id_: ").append(jA.getId()).append(" in ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms"));

		tIni = Calendar.getInstance().getTimeInMillis();
		
		// Asignar createDate y displayDate (es una fecha del chino. Debe ser igual que la fecha de creación siempre)
		jA.setCreateDate(createDate);
		jA.setDisplayDate(createDate);
		// Forzado siempre. Indicado así por Pedro.
		jA.setExpirationDate(null);
		// Setear el status con WorkflowConstants.STATUS_APPROVED			
		jA.setStatus(WorkflowConstants.STATUS_APPROVED);
		// Setear el urltitle.		
		try{
			// Quitamos los caracteres extraños
			jA.setUrlTitle(JournalArticleLocalServiceUtil.getUniqueUrlTitle(jA.getId(), jA.getGroupId(), jA.getArticleId(), urlTitle));			
		}catch(Exception e){
			_log.error(e);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, new StringBuilder(XmlioKeys.DESC_ERR_NORMALIZING_URLTITLE).append(": '").append(urlTitle).append("'").toString());			
		}
		
		// Update del journalarticle	
		JournalArticleLocalServiceUtil.updateJournalArticle(jA, true);	
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time updating the article: ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms"));
		
		// Añadir la legacyUrl			
		if( Validator.isNotNull(legacyUrl) )
		{
			
			// Codificamos la url
			if (legacyIsEncoded)
			{
				String strArray[] = legacyUrl.substring(1).split(StringPool.SLASH); 
				
				legacyUrl = "";				
                for(int i=0; i < strArray.length; i++) 
                		legacyUrl = legacyUrl + "/" + URLEncoder.encode(strArray[i], IterKeys.UTF8);
                	
			}
			
			tIni = Calendar.getInstance().getTimeInMillis();
			
			for (String sectionGroupId : sectionGroupIds)
			{
				final String sql = String.format(SQLQueries.INSERT_LEGACYURL, legacyUrl.toLowerCase().replaceAll("'", "''"), Long.parseLong(sectionGroupId), articleId);
				
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" query to insert legacyurl: ").append(sql));
				
				/* Controlamos que la legacyurl no esté ya en base de datos.
				 * No lo hacemos en la función getErrorCodeAndErrorDetails porque llegaria un XYZ_PRIMARY_KEY_getErrorCodeAndErrorDetails_ZYX que no nos dice nada. */
				try{
					PortalLocalServiceUtil.executeUpdateQuery(sql);
				}catch(Exception e){
					ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED, new StringBuilder(XmlioKeys.DESC_ERR_LEGACYURL_EXISTS).append(": '").append(legacyUrl).append("'").toString());				
				}
			}
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time to add legacyUrl: ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms"));
		}
		
		// Se crea un expandorow para el artículo. parámetros de la llamada al addRow:
		//	   tableId : id de la tabla
		//	   classPK : journalarticle.id_	
		tIni = Calendar.getInstance().getTimeInMillis();
		ExpandoRowLocalServiceUtil.addRow(Long.parseLong(expandoTableId), jA.getId());
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time to add expandoRow: ").append(Calendar.getInstance().getTimeInMillis()-tIni).append("ms"));
		
		//	Se añade un expando value para el expando column de nombre IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID.
		//	  La llamada a ExpandoValueLocalServiceUtil.addValue está interceptada en MyExpandoValueLocalService. Llamar a ExpandoValueLocalServiceUtil.importValue
		//	  Parámetros de la llamada a ExpandoValueLocalServiceUtil.importValue
		//		classNameId : journalarticle_classnameId
		//		tableId : tableId
		//		columnId : scopegroupId_columnId
		//		classPK : journalarticle.id_
		//		data : scopegroupid 
		tIni = Calendar.getInstance().getTimeInMillis();
		ExpandoValueLocalServiceUtil.importValue(jaClassNameId, Long.parseLong(expandoTableId), Long.parseLong(expColGrp), jA.getId(), StringUtil.merge(sectionGroupIds, StringPool.COMMA));
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time adding the expandovalue of the article group: ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms"));

		// RELACIONES DEL ARTÍCULO			
		
		// Importación de las SECCIONES
		importSections(article.selectNodes("metadata/sections/section"), articleId, globalGroupId, createDate);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time to add the categories: ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms"));
		
		//   Para las categorías con el atributo set="1" y el atributo main="1"
		//     Se añade un expando value para el expando column WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS.
		//     La llamada a ExpandoValueLocalServiceUtil.addValue está interceptada en MyExpandoValueLocalService. Llamar a ExpandoValueLocalServiceUtil.importValue
		//     Parámetros de la llamada a ExpandoValueLocalServiceUtil.importValue.			
		//    classNameId : journalarticle_classnameId
		//    tableId : tableId
		//    columnId : mainMetadatasIds_columnId 
		//    classPK : journalarticle.id_
		//    data : cadena con las categorías (ids) principales separadas por coma	
		tIni = Calendar.getInstance().getTimeInMillis();
		categoriesId = XMLHelper.getStringValues(article.selectNodes("./metadata/categories/vocabulary//category[(@set=\"1\" or @set=\"true\") and (@main=\"1\" or @main=\"true\")]/@" + ATTR_ID));
		StringBuilder idsFormattedCategories = new StringBuilder();
		for (int c = 0; c < categoriesId.length; c++){
			idsFormattedCategories.append(categoriesId[c]);
			if (c < categoriesId.length -1)
				idsFormattedCategories.append(",");
		}
		ExpandoValueLocalServiceUtil.importValue(jaClassNameId, Long.parseLong(expandoTableId), Long.parseLong(expColMeta), jA.getId(), idsFormattedCategories.toString());
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time to add the main categories of the expandovalue: ").append(Calendar.getInstance().getTimeInMillis()-tIni).append(" ms"));
		
		// Importación de SUSCRIPCIONES (PRODUCTOS). En el nodo "subscriptions" están los productos a los que está asociado el artículo.
		final String[] subscriptionsIds = XMLHelper.getStringValues(article.selectNodes("./metadata/subscriptions/subscription/@" + ATTR_ID), null);
		// Recuperar el id de los productos de la tabla product y añadir a la tabla articleproduct las duplas (productId, articleId)	
		if (Validator.isNotNull(subscriptionsIds))
			insertArticleProducts(jA.getArticleId(), subscriptionsIds);
		
		/* Importamos los archivos.
		Importante dejar esto para el final (salvo cambiar el contenido porque depende de la importación de archivos) para poder hacer de ellos un rollback manual */
		final List<Map<String, String>> replaces = importFiles(deletePendingBinaries, article.selectNodes("./content/component/file"), 
			                                            globalGroupId, defaultUserId, workingDirectory, maxImgWidth, maxImgHeight, serviceContext.getScopeGroupId(), jA.getArticleId());

		// Si ha salido todo bien (no hay excepciones) y hay imagenes, cambiamos el journal.content para colocar el width y height de las imágenes
		if (null != replaces && replaces.size() > 0)
		{	
			jA.setContent(replaceAndFillBinaries(Long.toString(globalGroupId), transformedContent, replaces));
			JournalArticleLocalServiceUtil.updateJournalArticle(jA, true);
		}	
		if(_log.isDebugEnabled())
			_log.debug(new StringBuffer(XmlioKeys.PREFIX_ARTICLE_LOG + " time in _importNewArticle: ").append(Calendar.getInstance().getTimeInMillis()-t0).append(" ms"));
	}
	
	// Crea un xml con el resultado de la importación de un artículo
	private void createResponse(Document xmlResult, StringBuffer errorCode, StringBuffer errorDetail, String articleId, String urlTitle, long t0) 
					throws ServiceError, DocumentException{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG  + "In createResponse");
		
		final long time0 = Calendar.getInstance().getTimeInMillis();		
		
		Element nodeRoot = xmlResult.getRootElement();			
		nodeRoot.addAttribute("id", PortalUUIDUtil.newUUID());			
		nodeRoot.addAttribute("errorCode",   null == errorCode   ? "" : errorCode.toString()  );
		nodeRoot.addAttribute("errordetail", null == errorDetail ? "" : errorDetail.toString());			
		nodeRoot.addAttribute("starttime",   SDF_TO_DB.format(t0));
		nodeRoot.addAttribute("finishtime",  SDF_TO_DB.format(Calendar.getInstance().getTime()));
		
		Element subject = nodeRoot.addElement("subject");					
		
		Document a = SAXReaderUtil.read("<a />");
		Element aRoot = a.getRootElement();
		aRoot.addAttribute("articleId", null == articleId ? "" : StringEscapeUtils.escapeSql(articleId));
		aRoot.addAttribute("title", null == urlTitle  ? "" : StringEscapeUtils.escapeSql(urlTitle));
		subject.addCDATA(aRoot.asXML());

		if(_log.isDebugEnabled())
			_log.debug(new StringBuffer(XmlioKeys.PREFIX_ARTICLE_LOG + " createResponse elapsed: ").append(Calendar.getInstance().getTimeInMillis()-time0).append(" ms"));
	}	
	
	private Date validateDate(String date, String valueName, SimpleDateFormat sDF) throws ServiceError{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG  + " In validateDate");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(date), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY, XmlioKeys.DESC_ERR_EMPTY_DATE + ": '" + valueName + "'");
		
		Date d = null;		
		try{
			d = sDF.parse(date);
		}catch(Exception e){
			ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_DATE_FORMAT, XmlioKeys.DESC_ERR_INVALID_DATE + " '" + valueName + ": '" + date + "'");
		}
		return d;
	}
	
	/* Comprueba que:
	   jAcreateDate       <= jADateValidityFrom
	   jADateValidityFrom <= jADateValidityTo */
	private void validateValidity(SimpleDateFormat sDF, Date jAcreateDate, String jAValidityFrom, String jAValidityTo) throws ServiceError{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In validateValidity");
		
		final Date jAValidityDateFrom = validateDate(jAValidityFrom, "start validity",  sDF);
		final Date jAValidityDateTo   = validateDate(jAValidityTo,   "finish validity", sDF);
		
		ErrorRaiser.throwIfFalse(!jAcreateDate.after(jAValidityDateFrom),     XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_INVALID_START_VALIDITY  + ": '" + sDF.format(jAValidityDateFrom) + "', '" + sDF.format(jAcreateDate)     + "'");
		ErrorRaiser.throwIfFalse(!jAValidityDateFrom.after(jAValidityDateTo), XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_INVALID_START_VALIDITY2 + ": '" + sDF.format(jAValidityDateFrom) + "', '" + sDF.format(jAValidityDateTo) + "'");		
	}
	
	/* Comprueba que:
	  		importationStart  <= createDate
	  		importationStart  <= modifiedDate
	  	 	importationFinish >= createDate
	  		importationFinish >= modifiedDate 
	  		createDate        <= modifiedDate 
	   Como no existe la operación >= ni <= con fechas, jugamos negando el contrario. */
	private void validateCreateAndModifiedDate(Date importationStart, Date importationFinish, Date createDate, Date modifiedDate) throws ServiceError{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In validateCreateAndModifiedDate");
		
		ErrorRaiser.throwIfFalse(!importationStart.after(createDate),     XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_JA_CREATED_DATE   + "': '" + SDF_TO_DB.format(createDate)   + "', '" + SDF_TO_DB.format(importationStart)  + "'");		
		ErrorRaiser.throwIfFalse(!importationStart.after(modifiedDate),   XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_JA_MODIFIED_DATE  + "': '" + SDF_TO_DB.format(modifiedDate) + "', '" + SDF_TO_DB.format(importationStart)  + "'");		
		ErrorRaiser.throwIfFalse(!importationFinish.before(createDate),   XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_JA_CREATED_DATE2  + "': '" + SDF_TO_DB.format(createDate)   + "', '" + SDF_TO_DB.format(importationFinish) + "'");		
		ErrorRaiser.throwIfFalse(!importationFinish.before(modifiedDate), XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_JA_MODIFIED_DATE2 + "': '" + SDF_TO_DB.format(modifiedDate) + "', '" + SDF_TO_DB.format(importationFinish) + "'");		
		ErrorRaiser.throwIfFalse(!createDate.after(modifiedDate),         XmlioKeys.DETAIL_ERROR_CODE_DATE_CONTROL, XmlioKeys.DESC_ERR_ART_MODIFIED_DATE + "': '" + SDF_TO_DB.format(modifiedDate) + "', '" + SDF_TO_DB.format(createDate)        + "'");
	}
	
	private void validateComponents(Node article) throws ServiceError
	{
		int inconsistencies = article.selectNodes(XPATH_VALIDATE_XML_COMPONENTS).size();
		ErrorRaiser.throwIfFalse(inconsistencies == 0, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MALFORMED_XML, XmlioKeys.DESC_ERR_INVALID_COMPONENT_TAG);
		inconsistencies = article.selectNodes(XPATH_VALIDATE_XML_COMPONENTS_2).size();
		ErrorRaiser.throwIfFalse(inconsistencies == 0, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MALFORMED_XML, XmlioKeys.DESC_ERR_INVALID_COMPONENT_TAG_2);
	}
	
	private Set<String> validateSections(boolean ifLayoutNotExists, long defaultUserId, String groupId, List<Node> sections) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException{
		_log.trace(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" In validateSections"));

		// Comprueba que cada sitio en el que se vaya a importar indique 1 y sólo 1 sección por defecto
		Set<String> sectionGroups = validateDefaultSections(sections);
		
		long tIni = Calendar.getInstance().getTimeInMillis();
		
		String sql = StringPool.BLANK;
		Document d = null;
		String[] values = null;
		
		List<String> differentsLayouts = new ArrayList<String>();
		for(Node section : sections)
		{
			final String sectionGroupId = XMLHelper.getTextValueOf(section, "@site");
			final String qualification = XMLHelper.getTextValueOf(section, "@qualification");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(qualification), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, XmlioKeys.DESC_ERR_INVALID_QUALIFICATION);
			final String friendlyUrl = XMLHelper.getTextValueOf(section, "@url");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(friendlyUrl), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, XmlioKeys.DESC_ERR_INVALID_URL);
			ErrorRaiser.throwIfFalse(friendlyUrl.length() <= MAX_FRIENDLYURL, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, XmlioKeys.DESC_ERR_MAX_SECTION_LENGTH);
			
			// PageTemplate, opcional (-1 significa el de por defecto)
			final String pagetemplate = XMLHelper.getTextValueOf(section, "@pagetemplate", DEFAULT_PAGE_TEMPLATE);
			// ErrorRaiser.throwIfFalse(null != pagetemplate && !"-1".equals(pagetemplate), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, "Attribute pagetemplate is null or empty");
			String pgTempId = pagetemplate;
			
			// Buscamos el pagetemplate
			if (!DEFAULT_PAGE_TEMPLATE.equals(pagetemplate)){			
				sql = String.format(GET_PAGETEMPLATE, sectionGroupId, pagetemplate);
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Query to check pagetemplate: \n").append(sql));
				d = PortalLocalServiceUtil.executeQueryAsDom(sql);
				values = XMLHelper.getStringValues(d.selectNodes("/rs/row/@id_"));
				ErrorRaiser.throwIfFalse(values.length == 1, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_TEMPLATE, XmlioKeys.DESC_ERR_INVALID_PAGETEMPLATE + ": '" + pagetemplate + "'");			
				pgTempId = values[0];
			}						
			((Element)section).addAttribute(ATTR_PG_TMPL_ID,  pgTempId);
			
			// Qualification
			String qualifId = "";
			sql = String.format(GET_QUALIFICATION, sectionGroupId, qualification);
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Query to get qualification: \n").append(sql));
			d = PortalLocalServiceUtil.executeQueryAsDom(sql);
			values = XMLHelper.getStringValues(d.selectNodes("/rs/row/@qualifId"));
			ErrorRaiser.throwIfFalse(values.length == 1, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS,  XmlioKeys.DESC_ERR_INVALID_QUALIFICATION2 + " '" + qualification + "'");			
			qualifId = values[0];
			((Element)section).addAttribute(ATTR_QUALIF_ID,   qualifId);

			// Layout
			ErrorRaiser.throwIfFalse(!differentsLayouts.contains(friendlyUrl), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, new StringBuilder(XmlioKeys.DESC_ERR_LAYOUT_REPEATED_IN_ARTICLE).append(": '").append(friendlyUrl).append("'").toString() );
			differentsLayouts.add(friendlyUrl);
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Looking for the layout with the friendly url: '").append(friendlyUrl).append("'"));
			String layoutUuid = "";
			try{
				// Existe el layout
				final Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(Long.parseLong(sectionGroupId), false, friendlyUrl);
				layoutUuid = layout.getUuid();
			}catch(NoSuchLayoutException e){
				// No existe el layout, lo creamos
				if (!ifLayoutNotExists){	
					layoutUuid = createNewLayout(defaultUserId, sectionGroupId, friendlyUrl);				
					
				// No existe el layout, error
				}else{
					_log.error(new StringBuilder("The layout given does not exists: '").append(friendlyUrl).append("'"));
					ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, new StringBuilder(XmlioKeys.DESC_ERR_INVALID_LAYOUT).append(": '").append(friendlyUrl).append("'").toString());
				}
			}			
			((Element)section).addAttribute(ATTR_LAYOUT_UUID, layoutUuid);
		}		
		_log.debug(XmlioKeys.PREFIX_ARTICLE_LOG + " Time checking the sections: " + (Calendar.getInstance().getTimeInMillis() - tIni) + " ms\n");
		
		return sectionGroups;
	}
	
	// Devuelve true si hay al menos una categoría (category) asignada al artículo. Lanza excepción si algún vocabulario no existe.
	private void validateCategories(StringBuffer errorCode, StringBuffer errorDetails, String ifNoCategory, long globalGroupId, Node categories, long delegationId) 
					throws SecurityException, NoSuchMethodException, ServiceError
	{
		_log.trace(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" In validateCategories"));
		
		boolean categoryAsigned = false;
		
		final long tIni = Calendar.getInstance().getTimeInMillis();
		if( Validator.isNotNull(categories) ){
			List<Node> vocabularies = categories.selectNodes("vocabulary");
			
			String[] vocNames = XMLHelper.getStringValues(vocabularies, "@name");
			for (int i=vocNames.length-1; i>=0; --i)
			{
				// Concatena el ID de la delegación
				if (delegationId > 0)
					vocNames[i] = delegationId + StringPool.PIPE + vocNames[i];
				
				// Escapa los apóstrofes
				vocNames[i] = vocNames[i].replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
			}

			final String sql = String.format(GET_VOCABULARY, globalGroupId, StringUtil.merge(vocNames, "','"));
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Query to check vocabulary: ").append(sql));
			Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);			
			 
			for(int i = 0; i < vocabularies.size(); i++){
				Element voc = (Element)vocabularies.get(i);
				// Busca el Id del Vocabulario
				String vocId = "-1";
				for (Node n : d.selectNodes("/rs/row"))
				{
					String vocName = XMLHelper.getTextValueOf(n, "@name");
					if (delegationId > 0)
						vocName = vocName.substring(vocName.indexOf(StringPool.PIPE) + 1);
					
					if (vocName.equals(voc.attributeValue("name")))
					{
						vocId =  XMLHelper.getTextValueOf(n, "@vocabularyId");
						break;
					}
				}
				ErrorRaiser.throwIfFalse(!vocId.equals("-1"), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, XmlioKeys.DESC_ERR_INVALID_VOCABULAY + ": '" + voc.attributeValue("name") + "'");				
	
				voc.addAttribute(ATTR_ID, vocId);
				
				// Colocamos el atributo vocabularyId en todas las categorías del vocabulario. Lo necesitaremos más adelante para comprobar las categorías.
				final List<Node> categoriesVocabulary = voc.selectNodes(".//category");
				for (int c = 0; c < categoriesVocabulary.size(); c++){
					((Element)categoriesVocabulary.get(c)).addAttribute(VOC_ID, vocId);
				}
			}			
			categoryAsigned = getCategoriesIds(globalGroupId, AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, categories.selectNodes("vocabulary/category"));
		}		
		
		// Si el artículo no tiene asignado ningún metadata (category)
		if (!categoryAsigned){
			if ("c".equalsIgnoreCase(ifNoCategory) ){				// continue
				_log.debug("Article without metadata, continue.");
			}else if ("a".equalsIgnoreCase(ifNoCategory) ){			// advise. Continuamos pero avisamos
				errorCode.append(XmlioKeys.DETAIL_WARNING_CATEGORIES);
				errorDetails.append("The article has no metadata assigned (category)"); 
			}else{ 												    // Fallo
				_log.error("Article without metadata assigned (category)");
				ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, XmlioKeys.DESC_ERR_NO_METADATA);
			}
		}		
		_log.debug(XmlioKeys.PREFIX_ARTICLE_LOG + " Time checking categories: " + (Calendar.getInstance().getTimeInMillis() - tIni) + " ms\n");
	}
	
	// Devuelve true si hay al menos una categoría (category) asignada al artículo. Lanza excepción si alguna categoría no existe.
	private boolean getCategoriesIds(long globalGroupId, long parentCatId, List<Node> categories) throws SecurityException, NoSuchMethodException, ServiceError
	{
		_log.debug(XmlioKeys.PREFIX_ARTICLE_LOG + " In getCategoriesIds");
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		boolean categoryAsigned = false;

		if( Validator.isNotNull(categories) && categories.size() > 0){
			
			for(int i = 0; i < categories.size(); i++){
				Element cat = (Element)categories.get(i);
				String categoryName = XMLHelper.getTextValueOf(cat, "@name");
				
				// Atributo colocado anteriormente en validateCategories
				String vocabularyId = XMLHelper.getTextValueOf(cat, new StringBuilder("@").append(VOC_ID).toString());	
				
				final String sql = String.format(GET_CATEGORY, globalGroupId, parentCatId, categoryName.replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE), vocabularyId);
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Query to check category:").append(sql));
				Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
				
				String catId = XMLHelper.getTextValueOf(d.getRootElement(), "/rs/row/@categoryId", "-1");				
				ErrorRaiser.throwIfFalse(!catId.equals("-1"), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_CATEGORIES, XmlioKeys.DESC_ERR_INVALID_CATEGORY + " :" + categoryName);				
				cat.addAttribute(ATTR_ID, catId);
				
				// Comprobamos si tiene al menos una categoría asignada
				if (GetterUtil.getBoolean(cat.attributeValue("set", "0")))
				{
					categoryAsigned = true;
				}
				
				if (getCategoriesIds(globalGroupId, Long.valueOf(catId), cat.selectNodes("category")))
				{
					categoryAsigned = true;
				}
			}
		}
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time in getCategoriesIds: ").append(Calendar.getInstance().getTimeInMillis() - t0).append(" ms\n"));
		return categoryAsigned;
	}
	
	private boolean validateSubscriptions(StringBuffer errorCode, StringBuffer errorDetails, List<Node> subscriptions, String ifNoSuscription) throws SecurityException, NoSuchMethodException, ServiceError
	{
		_log.debug(XmlioKeys.PREFIX_ARTICLE_LOG + " In validateSubscriptions");
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		boolean withSuscriptions = false;
		
		if(Validator.isNotNull(subscriptions) && subscriptions.size() > 0){
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" products found in xml: ").append(subscriptions.size()));
			String[] subNames = XMLHelper.getStringValues(subscriptions, "@name");
			final String sql = String.format(GET_PRODUCT, StringUtil.merge(subNames, "','"));
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Query to get products: ").append(sql));
			Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			List<String> differentSubscriptions = new ArrayList<String>();
			for(int i = 0; i < subscriptions.size(); i++){
				Element subscription = (Element)subscriptions.get(i);
				final String subscripId = XMLHelper.getTextValueOf(d, String.format("/rs/row[@name='%s']/@productId", subscription.attributeValue("name")), "-1");
				ErrorRaiser.throwIfFalse(!subscripId.equals("-1"), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SUBSCRIPTIONS, new StringBuilder("There is not a suscription with the name: ").append(subscription.attributeValue("name")).toString());
				// Controlamos que no haya la misma suscripción más de una vez para que no falle la inserción (duplicate key)
				ErrorRaiser.throwIfFalse(!differentSubscriptions.contains(subscripId), 
										 XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SUBSCRIPTIONS, (null == ifNoSuscription ? XmlioKeys.DESC_ERR_SUBSCRIPTION_REPEATED_IN_FILEENTRY : XmlioKeys.DESC_ERR_SUBSCRIPTION_REPEATED_IN_ARTICLE));			
				differentSubscriptions.add(subscripId);
				subscription.addAttribute(ATTR_ID, subscripId);
				withSuscriptions = true;
			}
		}

		if (Validator.isNotNull(ifNoSuscription) && !withSuscriptions){
			if ("c".equalsIgnoreCase(ifNoSuscription) ){				// continue
				_log.debug("Article without suscriptions, continue.");
			}else if ("a".equalsIgnoreCase(ifNoSuscription) ){			// advise. Continuamos pero avisamos
				errorCode.append(XmlioKeys.DETAIL_WARNING_SUSCRIPTIONS);
				errorDetails.append(XmlioKeys.DESC_ERR_ART_WITH_NO_SUSCRIPTIONS); 
			}else{ 												        // Fallo
				_log.error("Article without suscriptions");
				ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SUBSCRIPTIONS, XmlioKeys.DESC_ERR_ART_WITH_NO_SUSCRIPTIONS);
			}
		}
		if(_log.isDebugEnabled())			
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time checking the suscriptions: ").append(Calendar.getInstance().getTimeInMillis() - t0).append(" ms"));
		return withSuscriptions;
	}
	
	private boolean journalArticleExists(long globalGrpId, String articleId) throws ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In journalArticleExists");		
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		boolean exists = false;
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Looking for the article with group: '").append(globalGrpId).append("' and articleId: '").append(articleId).append("'"));
		
		try{
			JournalArticleLocalServiceUtil.getArticle(globalGrpId, articleId);
			exists = true; if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("The article with group: '").append(globalGrpId).append("' and articleId: '").append(articleId).append("' exists"));
		}catch(NoSuchArticleException e){
			if (_log.isDebugEnabled())
				_log.debug(new StringBuilder("The article with group: '").append(globalGrpId).append("' and articleId: '").append(articleId).append("' not exists"));
		}
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time in journalArticleExists: ").append((Calendar.getInstance().getTimeInMillis() - t0)).append(" ms"));
		
		return exists;
	}
	
	private String getArticleContent(Node content, long globalGrpId)
	{
		final long tIni = Calendar.getInstance().getTimeInMillis();
		
		String result = "";
		String xslpath = new StringBuilder("").append(File.separatorChar).append("xmlio-portlet")
							.append(File.separatorChar).append("xsl")
							.append(File.separatorChar).append("createContent.xsl").toString();
		Map<String, String> params = new HashMap<String, String>();
		params.put( XmlioKeys.XSL_PARAM_GLOBAL_GRP_ID, String.valueOf(globalGrpId) );

		result = XSLUtil.transformXML(content.asXML(), xslpath, params);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time transforming the xml and generating the article content: ").append(Calendar.getInstance().getTimeInMillis() - tIni).append(" ms\n"));

		return result;
	}
	
	// Da un valor al código de error y descripción del error en función de la excepción 
	private String[] getErrorCodeAndErrorDetails(Exception e, String articleId){
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In getErrorCodeAndErrorDetails");
		
		final String errorClass = e.getClass().getName().toLowerCase();		
		String[] errorCodeAndErrorDetail = new String[2];
		
		if (errorClass.equals((NumberFormatException.class.getName().toLowerCase()))){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_NUMBER_FORMAT;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_INVALID_NUMBER).append(e.getMessage()).append(" ").append(ExceptionUtils.getStackTrace(e)).toString();							
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Incorrect number format: ").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)));	
			
		}else if (errorClass.equals(SQLException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_SQL_ERROR).append(": ").append(e.getMessage()).append(" ").append(ExceptionUtils.getStackTrace(e)).toString();			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Error in sql: ").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)));		
			
		}else if (errorClass.equals(DuplicateArticleIdException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_REPEATED_ARTICLE).append(null != articleId ? ": " + articleId : "").toString();			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Article repeated:\n").append(ExceptionUtils.getStackTrace(e)));		
			
		}else if (errorClass.equals(DuplicateFileException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_FILE_ALLREADY_EXISTS).append(": ").append(e.getMessage()).append("\n").toString();			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("\n").append(ExceptionUtils.getStackTrace(e)));		
			
		}else if (errorClass.equals(ORMException.class.getName().toLowerCase())){			
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL;
			errorCodeAndErrorDetail[1] = new StringBuilder("ORMException ").append(e.getMessage()).append(" ").append(ExceptionUtils.getStackTrace(e)).toString();
			
		}else if (errorClass.equals(ServiceError.class.getName().toLowerCase())){		
			errorCodeAndErrorDetail[0] = ((ServiceError)e).getErrorCode();
			errorCodeAndErrorDetail[1] = e.getMessage();			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("\n").append(ExceptionUtils.getStackTrace(e)));
			
		}
		// ERROR AL IMPORTAR BINARIOS
		else if (errorClass.equals(com.liferay.portal.kernel.error.ServiceError.class.getName().toLowerCase()))
		{		
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARIES;
			errorCodeAndErrorDetail[1] = new StringBuilder(((com.liferay.portal.kernel.error.ServiceError)e).getErrorCode()).append(":\n").append(e.getMessage()).append(" for article ").append(articleId).toString();			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("\n").append(ExceptionUtils.getStackTrace(e)));
		}
		else if (errorClass.equals(Exception.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED;
			errorCodeAndErrorDetail[1] = e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" ").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)));
			
		/* Dos importaciones simultáneas, están dando de alta el mismo registro en la base de datos y se produce una violación de la primary key o unique key.
		   Como los hilos corren en distintas transacciones, ambos comprueban que el registro a crear no existe, el primer hilo lo crea sin problemas y el segundo falla. 
		   Como la única parte en común es la base de datos, para cuando se quiere comprobar ya es tarde, sacamos un mensaje "amigable" */ 	
		}else if(errorClass.equals(SystemException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_IMPORT_COLISSION;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_IMPORT_COLLISION).append(":\n").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)).toString();			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(XmlioKeys.DESC_ERR_IMPORT_COLLISION).append(":\n").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)).toString());
		
		// Imagen corrupta		
		}else if(errorClass.equals(ImageTypeException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_IMAGE_CORRUPTED).append(" ").append(this.lastImage).append(" ").append(ExceptionUtils.getStackTrace(e)).toString();
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(XmlioKeys.DESC_ERR_IMAGE_CORRUPTED).append(":\n").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)).toString());
		}
		else if (errorClass.equals(NotSupportedException.class.getName().toLowerCase()))
		{
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
			errorCodeAndErrorDetail[1] = new StringBuilder(XmlioKeys.DESC_ERR_IMAGE_FORMAT_NOT_SUPPORTED).append(" ").append(this.lastImage).append(" ").append(ExceptionUtils.getStackTrace(e)).toString();
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(XmlioKeys.DESC_ERR_IMAGE_FORMAT_NOT_SUPPORTED).append(":\n").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)).toString());
		}
		else{
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED;
			errorCodeAndErrorDetail[1] = e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);			
			_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" ").append(e.getMessage()).append("\n").append(ExceptionUtils.getStackTrace(e)));	
		}
	return errorCodeAndErrorDetail;
	}
	
	public void reindexArticleContent(long globalGrpId, Map<String, String> articlesToIndex) throws PortalException, SystemException
	{
		_log.trace(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("In reindexArticleContent"));
		
		long t0 = Calendar.getInstance().getTimeInMillis();
		
		int numArticles = articlesToIndex.size();
		if(numArticles>0)
		{
			try
			{
				int maxCommitArticles = Math.min(numArticles, 500);
				
				JournalIndexerUtil jiu = new JournalIndexerUtil();
				
				JournalIndexerMgr journalIdxMgr = new JournalIndexerMgr();
				journalIdxMgr.setArticlesByPackage( numArticles );
				int idx = 0;
				boolean doCommit = false;
				
				for (Map.Entry<String, String> articleIndexable : articlesToIndex.entrySet())
				{
					JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGrpId, articleIndexable.getKey());
					
					if( Validator.isNotNull(article) )
					{
						article.setIndexable( GetterUtil.getBoolean( articleIndexable.getValue(), true) );
						JournalArticleLocalServiceUtil.updateJournalArticle(article);
						
						if(idx==0)
							journalIdxMgr.setDelegationId(article.getDelegationId());
						
						idx++;
						doCommit = (idx%maxCommitArticles)==0;
						try
						{
							journalIdxMgr.domsToIndex(jiu.createDom(article), doCommit );
						}
						catch (Exception e)
						{
							journalIdxMgr.domToIndex(null, doCommit );
							
							_log.error(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null) + " Error indexing article content for articleid " + article.getArticleId());
							_log.error("Can not index article: " + article.getArticleId() + ". " + e);
						}
					}
				}				
			}
			catch (Exception e)
			{
				_log.error("Can not index articles " + e);
			}
		}
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time reindexArticleContent: ").append(Calendar.getInstance().getTimeInMillis() - t0).append(" ms\n"));		
	}
	
	private String createNewLayout(long defaultUserId, String groupId, String friendlyUrl) throws PortalException, SystemException, ServiceError, SecurityException, NoSuchMethodException{
		_log.trace("In createNewLayout");
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		/* En layout nos llega algo tal que así: /economia/bolsa/ibex35			 
			El layout completo no existe (/economia/bolsa/ibex35), pero sí pueden existir sublayouts (/economia/bolsa). 
			Hay que recorrerlo he ir comprobando cada uno. */
		
		ErrorRaiser.throwIfFalse(friendlyUrl.indexOf("/") != -1, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_LAYOUT, XmlioKeys.DESC_ERR_START_LAYOUT + ": '" + friendlyUrl + "'");
				
		final String[] subLayouts = friendlyUrl.split("/");
		ErrorRaiser.throwIfNull(Validator.isNotNull(subLayouts), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_LAYOUT, XmlioKeys.DESC_ERR_INVALID_URL_LAYOUT + ": '" + friendlyUrl + "'");		
		
		long lastLayout = DETAULT_PARENT_LAYOUT;
		Layout newLayout = null;
		
		// Sí, la primera posición nos la saltamos
		for (int l = 1; l < subLayouts.length; l++){
			
			final String layoutName = subLayouts[l];
			String subLayout = "";
			for (int i = 1; i <= l; i++){
				subLayout = subLayout + "/" + subLayouts[i];
			}
			
			// Comprobamos si el sublayout existe
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Checking if exists the sublayout: '").append(subLayout).append("'"));
			
			try{
				// Existe el layout, nos quedamos con su layoutId
				final Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(Long.parseLong(groupId), false, subLayout);
				lastLayout = layout.getLayoutId();
			
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Sublayout: '").append(subLayout).append("' found: '").append(lastLayout).append("'"));
				
			}catch(NoSuchLayoutException e){
				// No existe el sublayout, lo creamos
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append("Creating new sublayout: '").append(subLayout).append("'"));
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setUuid(PortalUUIDUtil.newUUID()); 
				// Título y descripción van vacíos siempre
				newLayout = LayoutLocalServiceUtil.addLayout(defaultUserId, Long.parseLong(groupId), false, lastLayout, layoutName, 
					                                                       "", "", PORTLET_TYPE, false, subLayout, serviceContext);
				ErrorRaiser.throwIfFalse(null != newLayout, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_LAYOUT, XmlioKeys.DESC_ERR_CREATING_LAYOUT + ": '" + layoutName + "'");				
				lastLayout = newLayout.getLayoutId();
			}		
		}
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time creating layouts: ").append((Calendar.getInstance().getTimeInMillis() - t0)).append(" ms\n"));		
		ErrorRaiser.throwIfNull(newLayout, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_LAYOUT, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_LAYOUT + ": '" + friendlyUrl + "'");
		return newLayout.getUuid().toString();
	}
	
	// Comprueba que no se repita una misma imagen para un artículo (ticket: 9613)
	private void checkNotRepeatedImg(Node article) throws ServiceError
	{
		_log.trace("In checkNotRepeatedImg");
		
		List<Node> pathsNodes = article.selectNodes("content/component/file | content/component/preview");
		
		if (null != pathsNodes && pathsNodes.size() > 0)
		{			
			String[] paths = XMLHelper.getStringValues(pathsNodes, "@path");
			
			HashMap<String, String> differentPaths = new HashMap<String, String>();
			
			for (int p = 0; p < paths.length; p++)
			{
				ErrorRaiser.throwIfFalse(!differentPaths.containsKey(paths[p]), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY, 
										 new StringBuilder(XmlioKeys.DESC_ERR_IMAGE_REPEATED).append("'").append(paths[p]).append("'").toString() );
				differentPaths.put(paths[p], "");
			}
		}
	}
	
	
	
	
	private String replaceAndFillBinaries(String globalGroup, String transformedContent, List<Map<String, String>> replaces) throws DocumentException, ServiceError
	{
		_log.trace(XmlioKeys.PREFIX_ARTICLE_LOG + "In replaceAndFillBinaries");
		
		final long t0 = Calendar.getInstance().getTimeInMillis();		
		
		Document xml = SAXReaderUtil.read(transformedContent);
		Element root = xml.getRootElement();
		
		// Actualiza los nombres de los binarios a minúsculas
		final String xpathImg = "/root//dynamic-element[@type=\"" + DOCUMENT_LIBRARY + "\"]/dynamic-content";
		for (Node imgNode : root.selectNodes(xpathImg))
		{
			String imgPath = imgNode.getText().toLowerCase();
			imgNode.setText(StringPool.BLANK);
			((Element)imgNode).addCDATA(imgPath);
		}
				
		for (Map<String, String> replace :replaces)
		{
			final String fileName = replace.get("name"); 		
			final String newFileName = replace.get("newName");
			final String width = replace.get("width");
			final String height = replace.get("height");
			
			final String xpath = "/root/dynamic-element[dynamic-element[@type=\"" + DOCUMENT_LIBRARY + "\" and dynamic-content[text()='" + fileName + "']]]";
			_log.debug(new StringBuilder("XPATH to get image: '").append(xpath).append("'"));
			Node node = root.selectSingleNode(xpath);
			ErrorRaiser.throwIfFalse(null != node, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED, XmlioKeys.DESC_ERR_IMG_NOT_FOUNT_TO_SIZE + ": '" + fileName + "'");
			
			// Cambia el nombre
			Element dynamicContent = (Element) node.selectSingleNode("dynamic-element/dynamic-content[text()='" + fileName + "']");
			dynamicContent.setText(StringPool.BLANK);
			dynamicContent.addCDATA(newFileName);
			
			// Añade el tamaño
			if (Validator.isNotNull(width) && Validator.isNotNull(height))
			{
				Element imgInfo = (Element) node.selectSingleNode("img-info");
				imgInfo.attribute("width").setValue(width);
				imgInfo.attribute("height").setValue(height);
			}
		}
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuffer(XmlioKeys.PREFIX_ARTICLE_LOG + " setImagesDimensions elapsed: ").append(Calendar.getInstance().getTimeInMillis()-t0).append(" ms"));
		
		return xml.asXML();
	}
	
	private static final Pattern virtualHostPattern = Pattern.compile("(.+?)(?:,\\s*|$)");
	
	
	private static void translateVirtualHosts(Node article, String currentGroupId, long currentDelegationId) throws ServiceError
	{
		// Traduce todas las <sections> que tengan el atributo 'site'.
		translateVirtualHostInNodes(article.selectNodes("metadata/sections/section[@site]"), "site", currentDelegationId, false);
		
		// Añade el grupo desde el que se está importando a las secciones que no lo lleven
		List<Node> sections = article.selectNodes("metadata/sections/section[not(@site) or @site = '']");
		for (Node section : sections)
		{
			((Element) section).addAttribute("site", currentGroupId);
		}
		
		// Traduce todos los <components> que tengan el atributo 'sites'.
		translateVirtualHostInNodes(article.selectNodes("content//component[@sites]"), "sites", currentDelegationId, true);
	}
	
	private static void translateVirtualHostInNodes(List<Node> elements, String attrName, long currentDelegationId, boolean allowsExclusion) throws ServiceError
	{
		for (Node element : elements)
		{
			// Busca los virtualhost y los sustituye por su correspondiente groupId.
			List<String> groupIdList = new ArrayList<String>();
			final Matcher matcher = virtualHostPattern.matcher(XMLHelper.getStringValueOf(element, StringPool.AT + attrName));
			while (matcher.find())
			{
			    for (int i = 1; i <= matcher.groupCount(); i++)
			    {
			    	boolean exclusion = false;
			        String virtualHost = matcher.group(i);
			        if (allowsExclusion && (exclusion = virtualHost.startsWith(StringPool.MINUS)))
			        	virtualHost = virtualHost.substring(1);
			        
			        Long groupId = LayoutSetTools.getGroupIdFromVirtualHost(virtualHost);
			        
			        // Busca la traducción. Si no la encuentra o no se puede recuperar su delegationId lanza un error conocido.
		        	ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.DESC_ERR_INVALID_GROUPS_VIRTUALHOST + StringPool.COLON + StringPool.SPACE + virtualHost);
		        	long delegationId = -1L;
		        	try
		        	{
		        		delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		        	}
		        	catch (Throwable th)
		        	{
		        		ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.DESC_ERR_INVALID_GROUPS_VIRTUALHOST + StringPool.COLON + StringPool.SPACE + virtualHost);
					}
		        	
		        	// Comprueba que la delegación sea la misma que la del grupo desde el que se está importando.
		        	ErrorRaiser.throwIfFalse(delegationId >= 0 && delegationId == currentDelegationId, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, XmlioKeys.DESC_ERR_INVALID_GROUPS_DELEGATION + StringPool.COLON + StringPool.SPACE + virtualHost);
			        
			        // Añade el identificador al listado.
			        groupIdList.add(exclusion ? StringPool.MINUS + groupId : String.valueOf(groupId));
			    }
			}
			
			// Actualiza el atributo 'site'.
			((Element) element).addAttribute(attrName, StringUtil.merge(groupIdList, StringPool.COMMA_AND_SPACE));
		}
	}
	
	private static Set<String> validateDefaultSections(List<Node> sections) throws ServiceError
	{
		// Crea un mapa para ir realizando las cuentas
		Map<String, Integer> counts = new HashMap<String, Integer>();
		for (Node section : sections)
		{
			// Obtiene el grupo
			String site = XMLHelper.getStringValueOf(section, "@site");
			// Obtiene el flag de sección por defecto
			boolean defaultSection = GetterUtil.getBoolean(XMLHelper.getStringValueOf(section, "@defaultSection"), false);
			// Comprueba si ya existe el grupo en el mapa
			if (counts.get(site) != null)
			{
				if (defaultSection)
					counts.put(site, counts.get(site) + 1);
			}
			else
			{
				counts.put(site, defaultSection ? 1 : 0);
			}
		}
		
		for (Entry<String, Integer> defaultSections : counts.entrySet())
		{
			// Si no hay sección por defecto o si hay más de 1 para el artículo, atributo defaultsection con valor 1 o TRUE, se genera un error y no se importa el artículo. (prerrequisito antes de comenzar a importar)
			ErrorRaiser.throwIfFalse(defaultSections.getValue() != 0, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, XmlioKeys.DESC_ERR_NO_DEFAULT_SECTION);
			ErrorRaiser.throwIfFalse(defaultSections.getValue() <= 1, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SECTIONS, XmlioKeys.DESC_ERR_MORE_ONE_DEF_SECTION);
		}
		
		return counts.keySet();
	}
}
