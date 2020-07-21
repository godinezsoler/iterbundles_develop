package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.service.ImportMgrLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.JournalArticleImportLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;

public class ImportContentThread extends Thread
{
	private static Log _log = LogFactoryUtil.getLog(ImportContentThread.class);	
	private static final SimpleDateFormat sDFToDB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);

	private File path = null;
	private File backupPath = null;
	
	private boolean deleteArticles;
	private boolean legacyIsEncoded;
	private boolean ifArticleExists;
	private boolean ifLayoutNotExists;
	
	private String groupId         = null;
	private String expColGrp       = null;
	private String expColMeta      = null;
	private String expandoTableId  = null;
	private String ifNoCategory    = null;
	private String ifNoSuscription = null;
	
	private long defaultUserId;
	private long globalGroupId;
	private long jaClassNameId;
	
	private Date importationStart  = null;
	private Date importationFinish = null;
	
	private ArrayList<Exception> excepcionList = null;
	
	private Document xmlResult = null;

	// Array de importaciones para que el hilo se registre durante su ejecución.
	private ArrayList<ImportContentThread> importThreads;
	// Listado de importId procesados para la cancelación controlada.
	private HashSet<String> importIds = new HashSet<String>();
	// Flag para la cancelación controlada.
	private boolean run = true;
	// Flag para finalizar la espera por el primer import.
	private boolean wait = true;
	
	public ImportContentThread(ThreadGroup tG, String threadName, ArrayList<Exception> excepcionList, Document xmlResult,
						       String groupId, long defaultUserId, long globalGroupId, long jaClassNameId, String expColGrp, 
						       String expColMeta, String expandoTableId, File path, File backupPath, Date importationStart, 
						       Date importationFinish, boolean deleteArticles, boolean legacyIsEncoded,
						       boolean ifArticleExists, String ifNoCategory, boolean ifLayoutNotExists, String ifNoSuscription,
						       ArrayList<ImportContentThread> importThreads){
		super(tG, threadName);
		this.excepcionList     = excepcionList;
		this.xmlResult         = xmlResult;
		this.groupId           = groupId;
		this.defaultUserId     = defaultUserId;
		this.globalGroupId     = globalGroupId; 
		this.jaClassNameId     = jaClassNameId;
		this.expColGrp         = expColGrp;
		this.expColMeta        = expColMeta;
		this.expandoTableId    = expandoTableId;
		this.path              = path;
		this.backupPath        = backupPath;
		this.importationStart  = importationStart;
		this.importationFinish = importationFinish;
		this.deleteArticles    = deleteArticles;
		this.legacyIsEncoded   = legacyIsEncoded;
		this.ifArticleExists   = ifArticleExists;
		this.ifNoCategory      = ifNoCategory;
		this.ifLayoutNotExists = ifLayoutNotExists;
		this.ifNoSuscription   = ifNoSuscription;
		this.importThreads     = importThreads;
	}
	
	@Override
	public void run()
	{
		// Se registra en el listado de hilos importadores.
		synchronized (importThreads)
		{
			importThreads.add(this);
		}
		// Procesa la importación / borrado.
		importOrDeleteArticles();
		// Se borra del listado de hilos importadores.
		synchronized (importThreads)
		{
			importThreads.remove(this);
		}
	}
	
	// Importa o borra artículos
	private void importOrDeleteArticles()
	{
		_log.trace(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null) + " in ImportContentThread.importArticles()");
		
		final Calendar t0 = Calendar.getInstance();
		
		try
		{
			final String actualIp 	 	= PropsValues.ITER_LIVE_SERVERS_OWNURL;
			final long allImportsTime	= Calendar.getInstance().getTimeInMillis();
			
			// Comprueba los zips a importar.
			final File[] zipFiles = prepareZipFiles();
			// Crea el directorio temporar para descomprimir los zips.
			final File tempDirectory = createTempDirectory();
			// Recorre los ficheros ZIP
			File zipFile = null;
			for (int z = 0; z < zipFiles.length && run; z++)
			{
				boolean allzipOk = true;
				String importId = null;
				Calendar z0 = Calendar.getInstance();

				try
				{
					zipFile = zipFiles[z];
					// Prepara los ficheros XML.
					final File[] xmlFiles = prepareXmlFiles(zipFile, tempDirectory);
					
					// Genera y registra el ID de la importación actual.
					importId = initializeImport(zipFile, actualIp, z0);
					// Mapa para los artículos que se han importado correctamente y que serán indexados al finalizar la importación del zip.
					Map<String, String> articlesToIndex = new HashMap<String, String>();
					// Recorre los ficheros XML
					for (int xmlFile = 0; xmlFile < xmlFiles.length && run; xmlFile++)
					{
						File file = xmlFiles[xmlFile];

						try
						{
							if(file.exists() && file.isFile() && file.canRead())
							{
								// Leemos el xml
								Document importDom = SAXReaderUtil.read(file);
								
								// Obtiene el tamaño para el recorte de las imagenes siguiendo el orden: raiz del xml de importación > portal-ext.properties > por defecto se emplea 600x400 
								int maxImgWidth	 = GetterUtil.getInteger( XMLHelper.getTextValueOf(importDom, "/articles/@maxwidth", PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_IMGIMPORT_MAXWIDTH)), 600);
								int maxImgHeight = GetterUtil.getInteger( XMLHelper.getTextValueOf(importDom, "/articles/@maxheight", PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_IMGIMPORT_MAXHEIGHT)), 400);						
								
								// Obtenemos todos los artículos y los recorremos para importarlos
								final List<Node> articles = importDom.selectNodes("/articles/article");
								ErrorRaiser.throwIfFalse(null != articles && articles.size() != 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId) + "No articles found in the xml file: " + file.getAbsolutePath());
								
								// Recorre los articulos
								for(int a = 0; a < articles.size() && run; a++)
								{
									final Node article = articles.get(a);

									// Importa / borra el artículo.
									Document xmlArticleImportResult = importArticle(importId, article, tempDirectory, maxImgWidth, maxImgHeight);
									// Verifica el resultado de la importación / borrado.
									allzipOk = checkImportResult(importId, xmlArticleImportResult, article, articlesToIndex) && allzipOk;
								}
							}
							else
							{
								// Si no se puede abrir el XML, se lanza un error.
								ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_XML_EXCEPTION, new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId)).append(" The file:  ").append(file.getName()).append(" can not be read").toString());
							}
						}
						// Errores de los XMLs
						catch(Exception e)
						{
							allzipOk = false;
							_log.error(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId) + " Error processing XML File " + (null != file && null != file.getName() ? " " + file.getName() : ""), e);
							registerError(importId, zipFile, file.getName(), e, t0);
						}
					}
					
					if (_log.isDebugEnabled())
						_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null)).append(" Total elapsed time in the import/s of the zip '").append(zipFile.getName()).append("': ").append((Calendar.getInstance().getTimeInMillis() - allImportsTime)).append(" ms"));
					
					// Reindexacion de los contenidos de los articulos importados correctamente.
					reindexArticles(articlesToIndex);
					
					// Una vez finalizado el procesamiento del zip, y si no se ha solicitado cancelación del proceso,
					// se elimina el directorio temporal y se mueve a backups.
					if (run)
						cleanWorkingDirectory(tempDirectory, zipFile, allzipOk);
					
				}
				// Errores de los zips individuales
				catch(Exception e)
				{
					_log.error(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId) + " Error processing ZIP File " + (null != zipFile && null != zipFile.getName() ? " " + zipFile.getName() : ""), e);
					registerError(importId, zipFile, null, e, t0);
				}
				finally
				{
					// Modificamos la fecha fin de la importacion
					if (null != importId)
					{
						try
						{
							ImportMgrLocalServiceUtil.updateImportarticlesFinishTime(importId);
						}
						catch(Exception e)
						{
							_log.error(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Error updating the importarticles finisht time"), e);
						}
					}
				}
			}
		}
		// Errores al acceder a los zips. Sólo deberían llegar ServiceError controlados. Estas excepciones son alerts. Hay que liberar el wait.
		catch(ServiceError e)
		{
			_log.error(new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null)).append(" unexpected error"), e);
			notifyException(e);
		}
		if (_log.isDebugEnabled())
			_log.debug(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null) + " Total import zip elapsed time: " + (Calendar.getInstance().getTimeInMillis() - t0.getTimeInMillis()) + " ms\n");		
	}

	/**
	 * Si el atributo path contiene una carpeta, recupera todos los ficheros con extensión ZIP que haya en el directorio.
	 * Si es un fichero, lo retorna e inicializa el atributo path con su directorio.
	 * <p>El atributo path debe estar inicializado y ser un directorio o un fichero. En caso contrario lanzará una excepción.</p>
	 * @return los ficheros ZIPs a procesar.
	 * @throws ServiceError si no hay ficheros o no se tiene acceso a ellos (XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX).
	 */
	private File[] prepareZipFiles() throws ServiceError
	{
		File[] zipFiles = null;
		// Si es una carpeta, recupera todos los ZIPs que encuentre en ella.
		if (path != null)
		{
			if (path.isDirectory())
			{
				zipFiles = path.listFiles(new FilenameFilter(){
					public boolean accept(File xmlsDirectory, String name){
						return name.toLowerCase().endsWith(XmlioKeys.ZIP_EXTENSION); 
					}
				});
			}
			// Si es un fichero, guarda su directorio.
			else
			{
				zipFiles = new File[1];
				zipFiles[0] = path;
				// La función que importa artículos necesita un directorio, no un archivo para obtener los archivos adjuntos.
				this.path = path.getParentFile();
			}
		}
		// Si no hay ficheros zips, lanza un XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX
		ErrorRaiser.throwIfFalse(null != zipFiles && zipFiles.length > 0, XmlioKeys.XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX, "No zip files found to import articles in: '" + path + "'");
		return zipFiles;
	}
	
	/**
	 * Crea un directorio temporal para descomprimir un fichero ZIP.
	 * @return el directorio creado.
	 * @throws ServiceError si ocurre un error durante la creación del directorio, como falta de permisos (XYZ_IMPORT_IO_EXCEPTION_ZYX).
	 */
	private File createTempDirectory() throws ServiceError
	{
		File tempDirectory = null;
		// Crea un directorio temporal donde descomprimir el zip.
		try
		{
			tempDirectory = XMLIOUtil.createTempDirectory();
		}
		// Si no puede, lanza un XYZ_IMPORT_IO_EXCEPTION_ZYX.
		catch (IOException e)
		{
			ErrorRaiser.throwIfError(XmlioKeys.XYZ_IMPORT_IO_EXCEPTION_ZYX, "Imposible to create temp directory.");
		}
		return tempDirectory;
	}
	
	/**
	 * Descomprime un fichero ZIP en el directorio indicado y recupera todos los ficheros XML que contenga.
	 * @param zipFile el fichero ZIP a descomprimir que contiene los ficheros XML a importar.
	 * @param tempDirectory el directorio temporal donde descomprimir el fichero ZIP.
	 * @return la lista de ficheros XML que contiene el fichero ZIP indicado.
	 * @throws ServiceError si ocurre un error al descomprimir el zip  o este no contiene ficheros XML.
	 */
	private File[] prepareXmlFiles(File zipFile, File tempDirectory) throws ServiceError
	{
		// Descomprime el fichero.
		try
		{
			ZipUtil.unzip(zipFile, tempDirectory);
		}
		catch (Exception e)
		{
			ErrorRaiser.throwIfError(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_ZIP_EXCEPTION, e.getMessage());
		}
		
		// Obtiene los archivos xml.
		final File[] xmlFiles = tempDirectory.listFiles(new FilenameFilter(){
				public boolean accept(File xmlsDirectory, String name){
    				return name.toLowerCase().endsWith(XmlioKeys.XML_EXTENSION);
    			}
			}
		);
		
		// Si no hay XMLs, lanza un XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX
		ErrorRaiser.throwIfFalse(null != xmlFiles && xmlFiles.length > 0, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_ZIP_EXCEPTION, XmlioKeys.XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX);
		
		return xmlFiles;
	}
	
	/**
	 * Genera el imporId e inserta el registro inicial de la importación en BBDD.
	 * @param zipFile el fichero zip que se está procesando.
	 * @param hostIp la IP del servidor actual.
	 * @param z0 el momento en el que comenzó la importaicón.
	 * @return el importId generado.
	 * @throws ServiceError si ocurre un error durante la inserción del registro de importación.
	 */
	private String initializeImport(File zipFile, String hostIp, Calendar z0) throws ServiceError
	{
		// Genera el ID de la importación actual.
		String importId = PortalUUIDUtil.newUUID();
		// Registra el ID en la lista de procesados.
		synchronized (importIds)
		{
			importIds.add(importId);
		}
		// Inserta el registro en la tabla importationarticles.	
		try
		{
			ImportMgrLocalServiceUtil.insertArticleImport(importId, this.groupId, zipFile, hostIp);
		}
		catch (Exception e)
		{
			throw ErrorRaiser.toServiceError(e);
		}
		finally
		{
			// Si es el primer fichero procesado, devuelve el control a flex.
			if (wait)
			{
				notifyResult(importId, zipFile, z0, hostIp);
			}
		}
		
		return importId;
	}

	/**
	 * Importa o borra un articulo.
	 * @param importId el imporId del ZIP que se está procesando.
	 * @param article el articulo a importar / borrar.
	 * @param tempDirectory el directorio temporal donde se ha descomprimido el ZIP para cargar los recursos del artículo.
	 * @param maxImgWidth el ancho para el recorte de imágenes.
	 * @param maxImgHeight el alto para el recorte de imágenes.
	 * @return el documento xml con el resultado de la importación.
	 */
	private Document importArticle(String importId, Node article, File tempDirectory, int maxImgWidth, int maxImgHeight)
	{
		Document xmlArticleImportResult = SAXReaderUtil.createDocument();
		xmlArticleImportResult.addElement("d");
		final long tA = Calendar.getInstance().getTimeInMillis();
		try
		{
			if (this.deleteArticles)
				JournalArticleImportLocalServiceUtil.deleteArticle(xmlArticleImportResult, this.globalGroupId, article);
			else
				JournalArticleImportLocalServiceUtil.importArticle(xmlArticleImportResult, article, this.groupId, this.globalGroupId, this.defaultUserId, this.jaClassNameId, this.expandoTableId, this.expColGrp, this.expColMeta, tempDirectory, this.importationStart, this.importationFinish, maxImgWidth, maxImgHeight, this.legacyIsEncoded, this.ifArticleExists, this.ifNoCategory, this.ifLayoutNotExists, this.ifNoSuscription);
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId)).append(" Import article elapsed time: ").append(XMLHelper.getTextValueOf(article, "@articleid")).append(", ").append((Calendar.getInstance().getTimeInMillis() - tA)).append(" ms\n"));
		}
		catch(Exception e)
		{	
			_log.error(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId) + " article " + XMLHelper.getTextValueOf(article, "@articleid") + " not imported");
		}
		return xmlArticleImportResult;
	}
	
	/**
	 * Revisa el resultado de la carga / borrado de un artículo y actualiza el estado de la importación.
	 * @param importId el imporId del fichero ZIP que se está procesando.
	 * @param xmlArticleImportResult el documento xml con el resultado de la importación del artículo.
	 * @param article el artículo que se ha importado.
	 * @param articlesToIndex el listado de artículos a reindexar.
	 * @return Verdadero si la importación finalizó correctamente. Falso en caso contrario.
	 */
	private boolean checkImportResult(String importId, Document xmlArticleImportResult, Node article, Map<String, String> articlesToIndex)
	{
		boolean ok = true;
		try
		{
			// Si viene @errorCode en el XML, se contabiliza un error en la importación.
			if (Validator.isNotNull(XMLHelper.getTextValueOf(xmlArticleImportResult, "/d/@errorCode", null)))
			{											
				ok = false;
				ImportMgrLocalServiceUtil.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_ARTICLES, importId, "0", "1");
			}
			// Si no, se contabiliza un artículo correcto.
			else
			{
				// Si se está importando, se guarda el artículo importado para indexarlo.
				if (!this.deleteArticles)
					articlesToIndex.put(XMLHelper.getTextValueOf(article, "@articleid"), XMLHelper.getTextValueOf(article, "./metadata/properties/@indexable" ));
				
				ImportMgrLocalServiceUtil.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_ARTICLES, importId, "1", "0");						
			}
			// Anotamos en base de datos (importationdetail) el resultado de la operacion
			ImportMgrLocalServiceUtil.insertArticleDetail(importId, groupId, xmlArticleImportResult.selectSingleNode("/d"));
		}
		catch(Exception e)
		{
			_log.error(new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId)).append(" Error updating import status"), e);
		}
		
		return ok;
	}
	
	/**
	 * Si el proceso es de importación y no de borrado, reindexa todos los artículos indicados.
	 * @param articlesToIndex el listado de artículos a reindexar.
	 */
	private void reindexArticles(Map<String, String> articlesToIndex)
	{
		if (!this.deleteArticles)
		{
			if(_log.isDebugEnabled())
				_log.debug(XmlioKeys.PREFIX_ARTICLE_LOG + " Indexing articles...");
			
			long tR = Calendar.getInstance().getTimeInMillis();
			
			try
			{
				JournalArticleImportLocalServiceUtil.reindexArticleContent(globalGroupId, articlesToIndex);
			}
			catch (Exception e)
			{
				_log.error(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null) + " Error indexing articles ");
				_log.error(e);
			}
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Total articles index time: ").append((Calendar.getInstance().getTimeInMillis()-tR)).append(" ms."));
		}	
	}
	
	/**
	 * Elimina el directorio temporal donde se descomprimió el fichero ZIP y, si la importación fue correcta,
	 * mueve este al directorio de backups.
	 * @param tempDirectory el directorio temporal donde se descomprimió el ZIP.
	 * @param zipFile el fichero ZIP a mover si todo su contenido se importó correctamente.
	 * @param allzipOk si se importó correctamente o no todo el contenido del zip; 
	 */
	private void cleanWorkingDirectory(File tempDirectory, File zipFile, boolean allzipOk)
	{
		System.gc();
		final long tD = Calendar.getInstance().getTimeInMillis();
		FileUtils.deleteQuietly(tempDirectory);
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null)).append(" Time deleting temporal file '").append(tempDirectory).append("': ").append((Calendar.getInstance().getTimeInMillis() - tD)).append(" ms\n"));
		
		try
		{
			// Si todo el zip se ha importado correctamente movemos el zip
			if (allzipOk && null != zipFile && null != this.backupPath)
			{
				final long tM = Calendar.getInstance().getTimeInMillis();
				ImportMgrLocalServiceUtil.moveImportedFiles(zipFile, this.backupPath);
				
				if(_log.isDebugEnabled())
					_log.debug(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null) + " Time moving zip '" + zipFile + "': " + (Calendar.getInstance().getTimeInMillis() - tM) + " ms\n");
			}
			else
			{
				if(_log.isDebugEnabled())
				{
					_log.debug(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null) + new StringBuffer(" Not moving zip files because: ")
						.append(!allzipOk               ? "not all the articles were imported " : "")
				        .append(null == zipFile		    ? "zip file is null"                    : "")
				        .append(null == this.backupPath ? "backupDirectory is null"             : "").toString());
				}
			}			
		}
		catch(Exception e)
		{
			_log.error(new StringBuilder(ImportMgrLocalServiceUtil.buildArticlePrefixLog(null)).append(" error moving the zip file: '").append(zipFile.getName()).append("'"), e);
		}
	}
	
// MANEJADORES DE ERRORES ////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Modifica la variable xmlResult que se pasa por callback para devolver el control a flex y que siga la importación en segundo plano.
	 * @param importId el importId de la importación actual.
	 * @param zipFile el fichero zip que se está procesando.
	 * @param startTime el momento en el que comenzó la importaicón.
	 * @param serverIp la dirección IP del servidor actual.
	 */
	private void createResultDocument(String importId, File zipFile, Date startTime, String serverIp)
	{		
		// Si this.xmlResult es null es porque no es el primer xml de la importacion, no hay que hacer nada con el.
		if (Validator.isNotNull(this.xmlResult))
		{
			synchronized (xmlResult)
			{
				Element root = this.xmlResult.getRootElement();
				// Este atributo le dice al padre del hilo (ImportMgrLocalServiceUtil) que puede devolver el control a flex.  
				root.addAttribute("toFlex", "1");	
				
				if (_log.isDebugEnabled())
					_log.debug(ImportMgrLocalServiceUtil.buildArticlePrefixLog(importId) + "'toFlex' set in ImportContentThread");
				
				if (Validator.isNotNull(importId))
				{
					Element row = root.addElement("row");
					row.addAttribute("id",   importId);
					row.addAttribute("fn",   zipFile.getAbsolutePath().replaceAll("\\\\", "/"));
					row.addAttribute("st",   sDFToDB.format(startTime));
					row.addAttribute("host", serverIp);			
				}
			}
		}
	}
	
	/**
	 * Registra un error en el sistema provocado durante el procesamiento de un ZIP o de un XML.
	 * 
	 * @param importId el identificador del proceso de importaicón / borrado de artículos.
	 * @param zipName el nombre del fichero ZIP que se estaba procesando cuando se produjo el error.
	 * @param xmlName el nombre del fichero XML que se estaba procesando cuando se produjo el error.
	 *                Si el error se produjo en un ZIP, este puede ser nulo.
	 * @param error la excepción que se ha producido.
	 * @param t0 el momento en el que comenzó la importación / borrado de artículos.
	 */
	private void registerError(String importId, File zipFile, String xmlName, Exception error, Calendar t0)
	{
		// Obtiene la IP actual.
		String hostIp = PropsValues.ITER_LIVE_SERVERS_OWNURL;
		try
		{
			StringBuffer errorDetail = new StringBuffer().append(error.getMessage());
			StringBuffer errorCode   = new StringBuffer();
			
			// Si es un fallo al procesar un ZIP, todavía no se ha generado el importId
			if (Validator.isNull(importId))
			{
				// Genera el importId.
				importId = PortalUUIDUtil.newUUID();
				// Lo registra como procesado (para la cancelación).
				synchronized (importIds)
				{
					importIds.add(importId);	
				}
				// Registra la importación
				ImportMgrLocalServiceUtil.insertArticleImport(importId, this.groupId, zipFile, hostIp);
			}
			
			if (error instanceof DocumentException) // DocumentException se genera al procesar un XML
				errorCode.append(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MALFORMED_XML);
			else if (error instanceof ZipException) // Se producen al descomprimir
				errorCode.append(XmlioKeys.DETAIL_ERROR_CODE_IMPORT_ZIP_EXCEPTION);
			else if (error instanceof ServiceError)
				errorCode.append(((ServiceError)error).getErrorCode());
			else
				errorCode.append("UNEXPECTED ERROR");
			
			Document xmlError = createFileError(zipFile.getName(), xmlName, errorCode, errorDetail, t0.getTimeInMillis());
			
			ImportMgrLocalServiceUtil.insertArticleDetail(importId, groupId, xmlError.selectSingleNode("/d"));
			ImportMgrLocalServiceUtil.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_ARTICLES, importId, "0", "1");
			ImportMgrLocalServiceUtil.updateImportarticlesFinishTime(importId);
		}
		catch (Exception e)
		{
			_log.error("Impossible to create and record the error.");
		}
		notifyResult(importId, zipFile, t0, hostIp);
	}
	
	/**
	 * Crea un XML con la estructura:
	 * <pre>
	 * {@code
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <d id="" errorCode="" errordetail='' starttime="" finishtime="">
	 *     <subject><![CDATA[<a articleId="" title=""/>]]></subject>
	 * </d>
	 * }
	 * </pre>
	 * @param zipName el nombre del fichero ZIP que se estaba procesando cuando se produjo el error.
	 * @param xmlName el nombre del fichero XML que se estaba procesando cuando se produjo el error.
	 *                Si el error se produjo en un ZIP, este puede ser nulo.
	 * @param errorCode el codigo de error.
	 * @param errorDetail la descripción detallada del error.
	 * @param t0 el momento en el que comenzó la importación / borrado de artículos.
	 * @return el documento XML para el registro del error en el sistema.
	 */
	private Document createFileError(String zipName, String xmlName, StringBuffer errorCode, StringBuffer errorDetail, long t0)
	{
		SimpleDateFormat df = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
		Document xmlResult = SAXReaderUtil.createDocument();
		xmlResult.addElement("d");
		Element nodeRoot = xmlResult.getRootElement();			
		nodeRoot.addAttribute("id", PortalUUIDUtil.newUUID());			
		nodeRoot.addAttribute("errorCode",   null == errorCode   ? "" : errorCode.toString());
		nodeRoot.addAttribute("errordetail", null == errorDetail ? "" : errorDetail.toString());			
		nodeRoot.addAttribute("starttime",   df.format(t0));
		nodeRoot.addAttribute("finishtime",  df.format(Calendar.getInstance().getTime()));
		Element subject = nodeRoot.addElement("subject");
		subject.addCDATA(
				new StringBuffer()
				.append("<a articleId=\"").append(null == xmlName ? "" : StringEscapeUtils.escapeSql(xmlName))
		         	.append("\" title=\"")    .append(null == zipName ? "" : StringEscapeUtils.escapeSql(zipName))
		         	.append("\"/>").toString());
		return xmlResult;
	}

// CONTROL DE EJECUCIÓN //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Para el proceso actual de importación / borrado de artículos.
	 * 
	 * <p>Comprueba si está procesando o ha procesado el imporId y en caso
	 * afirmativo pone el flag de parada a verdadero.</p>
	 * 
	 * @param importId el importid del fichero que está siendo importado.
	 */
	public void stopImport(String importId)
	{
		if (Validator.isNotNull(importIds))
		{
			synchronized (importIds)
			{
				if (importIds.contains(importId))
				{
					this.run = false;
				}		
			}
		}
	}
	
	/**
	 * El hilo actual se queda a la espera hasta que ocurre uno de los siguientes eventos:
	 * <ul>
	 * <li>Se produce un error antes de procesar los ZIPs. EJ: No hay ZIPs a procesar o no se puede crear la carpeta
	 * temporal para descomprimirlos.</li>
	 * <li>Comienza el proceso del primer fichero ZIP.</li>
	 * </ul>
	 * @param SLEEPING_TIME el tiempo de espera entre cada comprobación.
	 * @throws InterruptedException si algun hilo interrumpe al hilo actual.
	 */
	public void waitForFirstImport(int SLEEPING_TIME) throws InterruptedException
	{
		while (wait )
		{
			Thread.sleep(SLEEPING_TIME);
		}
	}
	
	/**
	 * Notifica una excepción al servicio que invocó el hilo y está esperando.
	 * <p>Añade una exepción al listado de excepciones y baja el flag de espera.</p>
	 * @param e la excepción que se quiere mostrar en IterAdmin
	 */
	private void notifyException(Exception e)
	{
		synchronized (excepcionList)
		{
			this.excepcionList.add(e);	
		}
		wait = false;
	}
	
	/**
	 * Notifica un resultado parcial al servicio que invocó el hilo y está esperando.
	 * <p>Para ello, crea un documento xml de respuesta y baja el flag de espera.</p>
	 * @param importId el importId de la importación actual.
	 * @param zipFile el fichero zip que se está procesando.
	 * @param z0 el momento en el que comenzó la importación.
	 * @param hostIp la IP del servidor actual.
	 */
	private void notifyResult(String importId, File zipFile, Calendar z0, String hostIp)
	{
		createResultDocument(importId, zipFile, z0.getTime(), hostIp);
		wait = false;
	}
}