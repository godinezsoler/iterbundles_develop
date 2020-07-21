package com.protecmedia.iter.xmlio.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.xmlio.service.JournalArticleImportLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.JournalArticleImportServiceUtil;
import com.protecmedia.iter.xmlio.service.base.ImportMgrLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;
import com.protecmedia.iter.xmlio.util.ImportContentThread;
import com.protecmedia.iter.xmlio.util.ImportLegacyurlTool;
import com.protecmedia.iter.xmlio.util.ImportUserThread;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ImportMgrLocalServiceImpl extends ImportMgrLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(ImportMgrLocalServiceImpl.class);	
	
	private static final SimpleDateFormat sDFToDB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);	
	private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";	
	
	// Artículos
	private static final int WAITING_TO_FLEX          = 100;  	// Tiempo de espera a que se inserte el primer importation para contestar a flex	
	private static final int MAX_WAITING_FLEX_TIME    = 15000;	// Tiempo máximo de espera de flex. Para evitar bucles infinito.	
	private final static int MAX_IMPORT_DETAILS_TITLE = 150;	// Longitud máxima para el título del detalle
	private final static String ERROR_DETAIL_LENGTH   = "800";	// Longitud del detalle del error a traerse (errordetail)
	private final static int MAX_ARTICLEID            = 75;		// Longitud máxima para el id del articulo
	private static double deletedPercentage			  = 0;		// Porcentaje de artículos borrados desde el borrado avanzado (ctrl+q)
	
	// Usuarios
	private static final int MAX_USRNAME = 255;
	private static final int MAX_EMAIL   = 255;
	
	// Campo y sentido de ordenación por defecto para los listados
	private static final String FIELD_TO_ORDER = "finishtime";
	private static final String FIELD_ORDER    = "desc";	
	
	// Grupos para los hilos.
	private static ThreadGroup usersThreadGroup    = null;
	private static ThreadGroup articlesThreadGroup = null;
	private static ArrayList<ImportContentThread> importThreads = new ArrayList<ImportContentThread>();
		
	// COMÚN	
		// Borra importaciones y sus detalles por cascade
		private final String DELETE_IMPORTATIONS = new StringBuffer()
			.append("DELETE FROM %s \n")
			.append("WHERE importid in(%s)").toString();
		
		// Borra detalles de una importación
		private final String DELETE_IMPORTATIONDETAILS = new StringBuffer()
			.append("DELETE FROM %s \n")
			.append("WHERE importdetailid in(%s)").toString();
	
		// Obtiene los ids de las importaciones involucradas al borrar los detalles de una importación
		private final String IMPORTS_INVOLVED = new StringBuffer()
			.append("SELECT DISTINCT(importid) importationid \n")
			.append("FROM %s                                 \n")
			.append("WHERE importdetailid IN(%s)").toString();
		
		// Cuentas las importaciones correctas e incorrectas de varias importaciones
		private final String COUNT_OK_AND_KO_IMPORTED = new StringBuffer()
			.append("SELECT importid, 				 			 \n")
	        .append("  ifnull(sum(ifnull(errorcode, 1)),  0) ok, \n") 
	    	.append("  ifnull(sum(!ifnull(errorcode, 1)), 0) ko  \n")
	    	.append("FROM %s id              		 			 \n")
	    	.append("WHERE importid in(%s) 		     			 \n")
	    	.append("  AND importdetailid not in(%s) 			 \n")
		    .append("GROUP BY importid").toString();
	
		// Actualiza las cuentas de ok y ko de una importacion
		public static final String UPDATE_OK_AND_KO_IMPORTED = new StringBuffer()
			.append("UPDATE %s             \n")
			.append("SET ok = %s, ko = %s  \n")
			.append("WHERE importid = %s   \n") 
			.append("LIMIT 1").toString();
				
		// Aumenta la cuenta de importaciones correctas e incorrectas
		private final String UPDATE_OK_KO_IMPORTS = new StringBuffer()
			.append("UPDATE %s SET \n")
			.append(" ok = ok + %s,         \n")
			.append(" ko = ko + %s          \n")
			.append("WHERE importid = '%s'  \n")
			.append("LIMIT 1 ").toString();
		
		public static int getWaitingForFirstInsertImportation()
		{		
			return WAITING_TO_FLEX;
		}
		
		private String getIdsToSqlFromXml(String xml, boolean quotes) throws DocumentException
		{			
			final Document doc = SAXReaderUtil.read(xml);
			final Element e = doc.getRootElement();
			final String[] values = XMLHelper.getStringValues(e.selectNodes("//row"), "@id");
			
			StringBuilder result = new StringBuilder();			
			final int size = values.length;
			for (int i = 0; i < size; i++)
			{
				result.append(quotes ? "'" : "").append(values[i]).append(quotes ? "'" : "");
				if (i < size -1)
				{
					result.append(", ");
				}
			}
			return result.toString();
		} 
		
		// Obtiene la ip actual
		private String getActualIp()
		{
			return PropsValues.ITER_LIVE_SERVERS_OWNURL;
		}		

		// Borra registros de importaciones, NO lo datos importados.		
		public String deleteImports(String importType, String xmlWithIds) throws ServiceError, IOException, SQLException, DocumentException
		{
			_log.trace("In deleteImports");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlWithIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Imports ids is null"    );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Import type ids is null");
			
			final long t0 = Calendar.getInstance().getTimeInMillis();
			
			final Document doc = SAXReaderUtil.read(xmlWithIds);	
			/* Recibimos un xml como este: 
		 	<rs>
				<row id=""/>	 	
 				...
	 		</rs> */
		
			final String[] auxIds = XMLHelper.getStringValues(doc.selectNodes("//@id"));
			
			String sql = null;			
			if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_USERS))
				sql = String.format(DELETE_IMPORTATIONS, XmlioKeys.USERS_TABLE_IMPORT, generateStringWithIdsToMysqlInFunction(auxIds, true));
			else if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES))
				sql = String.format(DELETE_IMPORTATIONS, XmlioKeys.ARTICLES_TABLE_IMPORT, generateStringWithIdsToMysqlInFunction(auxIds, true));
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid import type");
			
			_log.debug("Query to delete importations: \n" + sql);			
			PortalLocalServiceUtil.executeUpdateQuery(sql);	
			
			_log.debug("Time to delete importations: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");
			
			return xmlWithIds;
		}
		
		/* Borra detalles de importaciones. Hay que actualizar la cuenta de las importaciones correctas e incorrectas de cada importación. 
		   Si fallase el conteo de las importaciones correctas/incorrectas se hace rollback del borrado para no descuadrar las cuentas. */
		public String deleteImportDetails(String importType, String xmlWithIds) throws ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException, DocumentException
		{
			_log.trace("In deleteImportDetails");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Import type is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlWithIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Ids are null"       );
			
			final long t0 = Calendar.getInstance().getTimeInMillis();
			
			/* Recibimos un xml como este:
			   <rs>
 			   	 <row id=""/>	 	
		 		 ...
			   </rs> */
			Document doc = SAXReaderUtil.read(xmlWithIds);
			final String[] auxIds = XMLHelper.getStringValues(doc.selectNodes("//@id"));
			final String idsToDelete = generateStringWithIdsToMysqlInFunction(auxIds, true);
			String sql = null;
			
			// Buscamos las importaciones afectadas por el borrado de alguno de sus detalles
			if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_USERS))
				sql = String.format(IMPORTS_INVOLVED, XmlioKeys.USERS_TABLE_DETAILS_IMPORT,    idsToDelete);
			else if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES))
				sql = String.format(IMPORTS_INVOLVED, XmlioKeys.ARTICLES_TABLE_DETAILS_IMPORT, idsToDelete);
			
			_log.debug("Query to get importation envolved in the delete:\n" + sql);			
			doc = PortalLocalServiceUtil.executeQueryAsDom(sql);		
			ErrorRaiser.throwIfNull(doc, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Not importation envolved found");
			
			final String[] receivedIds = XMLHelper.getStringValues(doc.getRootElement().selectNodes("//@importationid"));		
			ErrorRaiser.throwIfFalse(Validator.isNotNull(receivedIds), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Not importation envolved found");
			final String importsIds = generateStringWithIdsToMysqlInFunction(receivedIds, true);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importsIds), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Error building imports ids");
			
			// Borramos los detalles
			if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_USERS))
				sql = String.format(DELETE_IMPORTATIONDETAILS, XmlioKeys.USERS_TABLE_DETAILS_IMPORT,    idsToDelete);
			else if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES))
				sql = String.format(DELETE_IMPORTATIONDETAILS, XmlioKeys.ARTICLES_TABLE_DETAILS_IMPORT, idsToDelete);
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid import type");
			
			_log.debug("Query to delete importationdetail:\n" + sql);			
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Consulta que nos devuelve las importaciones correctas e incorrectas por cada importación (importid, ok, ko)			
			if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_USERS))
				sql = String.format(COUNT_OK_AND_KO_IMPORTED, XmlioKeys.USERS_TABLE_DETAILS_IMPORT,    importsIds, idsToDelete);
			else if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES))
				sql = String.format(COUNT_OK_AND_KO_IMPORTED, XmlioKeys.ARTICLES_TABLE_DETAILS_IMPORT, importsIds, idsToDelete);
			
			_log.debug("Query to count ok and ko imported:\n " + sql);			
			doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(doc), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Error counting the ok and ko importations");
			
			// Recorremos los datos y actualizamos las cuentas de ok y ko de cada importacion
			final List <Node> importations = doc.getRootElement().selectNodes("/rs/row");		
			
			// Si el listado es vacio significa que no quedan detalles correctos ni incorrectos
			if (0 == importations.size())
			{
				if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_USERS))
					sql = String.format(UPDATE_OK_AND_KO_IMPORTED, XmlioKeys.USERS_TABLE_IMPORT,    "0", "0", importsIds);
				else if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES))
					sql = String.format(UPDATE_OK_AND_KO_IMPORTED, XmlioKeys.ARTICLES_TABLE_IMPORT, "0", "0", importsIds);
			}
			else
			{
				for (int i = 0; i < importations.size(); i++)
				{
					final Node importation = importations.get(i);
					
					if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_USERS))
					{
						sql = String.format(UPDATE_OK_AND_KO_IMPORTED, 
							                XmlioKeys.USERS_TABLE_IMPORT,
						                	XMLHelper.getTextValueOf(importation, "@ok"),
										    XMLHelper.getTextValueOf(importation, "@ko"),
										    "'" + XMLHelper.getTextValueOf(importation, "@importid") + "'"
										   );	
					}
					else if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES))
					{
						sql = String.format(UPDATE_OK_AND_KO_IMPORTED, 
											XmlioKeys.ARTICLES_TABLE_IMPORT,
										    XMLHelper.getTextValueOf(importation, "@ok"),
										    XMLHelper.getTextValueOf(importation, "@ko"),
										    "'" + XMLHelper.getTextValueOf(importation, "@importid") + "'"
										   );
					}
				}
			}		
						
			// Actualizamos las columnas de ok y ko
			if(_log.isDebugEnabled())
				_log.debug("Query to update ok and ko count imported:\n" + sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			if(_log.isDebugEnabled())
				_log.debug("Time to delete importDetails: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");			
			return xmlWithIds;
		}
		
		/* Escribe el resultado de la importación y sus detalles en disco.
		 * Es importante no guardar el archivo generado con extensión xml para que no se lea en las proximas importaciones.*/
		public void writeImportResultIntoDisk(String importId, String importType, String groupId, File file, Date startTime, Date finishTime, Document importDetail, File workingDirectory) 
						throws ServiceError, DocumentException, IOException
		{
			_log.trace("In writeImportResultIntoDisk");		
			
			ErrorRaiser.throwIfNull(importId,         IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId is null"        );
			ErrorRaiser.throwIfNull(importType,       IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importType is null"      );
			ErrorRaiser.throwIfNull(groupId,          IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null"         );
			ErrorRaiser.throwIfNull(file,             IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "file is null"            );
			ErrorRaiser.throwIfNull(workingDirectory, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "workingDirectory is null");
			ErrorRaiser.throwIfNull(importDetail,     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importDetail is null"    );
			
			// Creamos el xml que se grabará en disco
			Document xml = SAXReaderUtil.read("<import/>");
			Element root = xml.getRootElement();
			
			final SimpleDateFormat sDF = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
			
			// Incorporamos los datos generales de la importación
			root.addAttribute("id",    importId  );
			root.addAttribute("type",  importType);
			root.addAttribute("group", groupId   );
			root.addAttribute("file",  file.getName());
			root.addAttribute("startTime",  null == startTime  ? "" : sDF.format(startTime)  );
			root.addAttribute("finishTime", null == finishTime ? "" : sDF.format(finishTime) );
			
			// Aniadimos los detallas al xml
			root.add(importDetail.getRootElement());
			
			// Escribimos el xml en disco
			final byte[] bytes = xml.asXML().getBytes();		
		    FileOutputStream fileOuputStream = null;
		    
		    // Formamos el nombre del archivo. Es importante no guardar el archivo generado con extensión xml para que no se lea en las proximas importaciones.
		    final File resultFile = new File(workingDirectory.getAbsolutePath() + File.separator + "import_" + importId);

		    try
		    {	    	
	    		fileOuputStream = new FileOutputStream(resultFile); 

		        if (null != fileOuputStream)
		            fileOuputStream.write(bytes); 
		    }
		    catch(Exception e)
		    {
		    	_log.error("Error writing the file: " + resultFile.getAbsolutePath());
		    	_log.error(e);
		    }
		    finally
		    {
		        if (null != fileOuputStream)
		            fileOuputStream.close();
		    }
		}
			
		// Mueve un fichero al directorio de backup indicado. Se utiliza cuando una importación ha ido 100% bien.
		public void moveImportedFiles(File file, File backupDirectory) throws ServiceError
		{
			_log.trace("In moveImportedFiles");
			ErrorRaiser.throwIfNull(file, 			 IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "file is null"             );
			ErrorRaiser.throwIfNull(backupDirectory, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "backup directory is null ");
			
			if (file.exists()            && file.isFile()                 && file.canRead()            &&
				backupDirectory.exists() && backupDirectory.isDirectory() && backupDirectory.canWrite()  )
			{
				try
				{
					if (FileUtil.move(file, new File(new StringBuffer(backupDirectory.getAbsolutePath()).append(File.separator).append(file.getName()).toString())))
					{
					}
					else
					{
						_log.error(new StringBuffer("Imposible to move: ").append(file.getName()).append(" to ").append(backupDirectory.getAbsolutePath()).toString()); 
					}
				}
				catch(Exception e)
				{
					_log.error(new StringBuffer("Error moving the file: ")
						.append(null == file ? "" : ": " + file)
						.append(" into: ")
						.append(backupDirectory).toString());							
				}
			}
			else if(!file.exists())
				_log.error("File " + file.getAbsolutePath()            + " can't be moved because it doesn't exists"   );
			else if(!file.isFile())
				_log.error("File " + file.getAbsolutePath()            + " can't be moved because it isn't a file"     );
			else if(!file.canRead())
				_log.error("File " + file.getAbsolutePath()            + " can't be moved because it can't be read"    );
			else if(!backupDirectory.exists())
				_log.error("File " + backupDirectory.getAbsolutePath() + " can't be moved because it doesn't exists"   );
			else if(!backupDirectory.isDirectory())
				_log.error("File " + backupDirectory.getAbsolutePath() + " can't be moved because it isn't a directory");
			else if(!backupDirectory.canWrite())
				_log.error("File " + backupDirectory.getAbsolutePath() + " can't be moved because it can't be written" );
			else
				_log.error("File and/or directory not available to be read/moved");
		}

		// Recibe un String[] de ids y monta una cadena del tipo: "'id1', 'id2', 'id3', ..." para usarse en la función in de mysql
		private String generateStringWithIdsToMysqlInFunction(String[] ids, boolean quotes)
		{	
			_log.trace("In generateStringWithIdsToMysqlInFunction");
			
			StringBuffer aux = new StringBuffer();
			final int idsSize = ids.length;
			
			for (int i = 0; i < idsSize; i++)
			{				
				if (quotes)
					aux.append("'" + ids[i] + "'");
				else
					aux.append(ids[i]);
				
				if (i < idsSize -1)
					aux.append(", ");
			}			
			return aux.toString();
		}
		
		// Obtiene el campo y ordenacion por la que ordenar
		private String getSqlSort(String flexSort)
		{
			_log.trace("In getSqlSort");			
			
			StringBuffer sqlSort = new StringBuffer("");

			if (Validator.isNotNull(flexSort))
			{
				/* Llega una cadena como esta:
				columnid=totalOk asc=0 */
				
				// Obtenemos la columna 
				final String column = flexSort.split(" ")[0].split("=")[1];
				// Obtenemos el sentido
				final String auxOrder = flexSort.split(" ")[1].split("=")[1];				
				final String order = auxOrder.equals("1") ? "asc" : "desc";				
				
				sqlSort.append(column + " " + order);
			}
			
			// Utilizamos la ordenacion por defecto
			else
			{
				sqlSort.append(FIELD_TO_ORDER + " " + FIELD_ORDER);
			}
			
			return sqlSort.toString();
		}
		
		// Actualiza las cuentas de ok y ko para una importacion
		public void updateOkAndKoImportCount(String importType, String importId, String ok, String ko) throws ServiceError, IOException, SQLException
		{
			_log.trace("In updateOkAndKoImportCount");			
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importType is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId),   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(ok), 		  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ok is null"      );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(ko), 		  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ko is null"      );
			
			String table = null;			
			if (importType.equalsIgnoreCase(XmlioKeys.IMPORT_TYPE_ARTICLES) )
			{
				table = XmlioKeys.ARTICLES_TABLE_IMPORT;
			}
			else
			{
				table = XmlioKeys.USERS_TABLE_IMPORT;
			}
			
			final String sql = String.format(UPDATE_OK_KO_IMPORTS, table, ok, ko, importId);
			if(_log.isDebugEnabled())
				_log.debug("Query to update ok and ko import count: \n" + sql);			
			
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}

		/* Java puede devolver más hilos de los que realmente hay en un grupo, por eso contamos los hilos a mano:
		   "An estimate of the number of active threads in this thread group AMD IN ANY OTHER THREAD GROUP THAT 
		   HAS THIS THREAD GROUP AS AN ANCESTOR" */		
		private int countActiveThreads(ThreadGroup tG, String identicator)
		{
			_log.trace("In countActiveThreads");
			int active = 0;
			
			try
			{
				// Obtenemos los hilos del grupo activos (pueden venir algunos del sistema de más)
				final Thread [] threads = new Thread [tG.activeCount()];
				tG.enumerate(threads);
				if (null != threads && threads.length > 0)
				{
					_log.debug("Threads found: " + threads.length);					
					
					for (int t = 0; t < threads.length; t++)
					{
						final Thread thread = threads[t];
						
						if (null != thread && thread.isAlive())
						{							
							_log.debug("Thread name: '" + thread.getName() + "'");
							
							if (null != thread.getName() && thread.getName().indexOf(identicator) != -1)
							{	
								_log.debug("Thread with name '" + thread.getName() + "' is from thread group '" + tG.getName() + "'");
								active++;
								break;
							}
						}					
					}
				}
				else
				{
					_log.debug("No active threads found for '" + tG.getName() + "'");
				}
			}
			catch(Exception e)
			{
				_log.error("Error counting active threads: " + e.getMessage());
			}
			return active;			
		}		
	// COMÚN
	
	// IMPORTACIÓN DE USUARIOS		
		// Listado de las importaciones que se desean cancelar
		private static List<String> userImportationsToCancel;
		
		public static List<String> getUserImportationsToCancel()
		{
			return ImportMgrLocalServiceImpl.userImportationsToCancel;
		}
		
		// Inserccion de importacion de usuario (importation)
		private final String INSERT_USER_IMPORT = new StringBuffer()
			.append("INSERT INTO importation(importid, groupid, filename, starttime, host) VALUES \n")
			.append("('%s', %s, %s, sysdate(), '%s')").toString();
		
		// Inserccion de un detalle de una importacion de usuario (importationdetail)
		private final String INSERT_IMPORTDETAIL = new StringBuffer()			
			.append("INSERT INTO importationdetail(importdetailid, importid, starttime, finishtime, usrname, email, errorcode, errordetail) VALUES \n")
			.append("(%s, %s, %s, %s, %s, %s, %s, %s)").toString();
		
		// Obtenemos los datos comunes de todas las importaciones de usuarios
		public final static String GET_USER_PROFILES = new StringBuffer() 
			// Debido a un error en el metodo que realiza la consulta, tenemos que hacer el cast  
		.append("SELECT up.profilefieldid, up.fieldname, ff.required,                          \n")
		.append("       CAST(df.fieldtype AS CHAR CHARACTER SET utf8) fieldtype, up.structured \n")
		.append("FROM form f                                                                   \n")
		.append("INNER JOIN formtab ft ON f.formid = ft.formid                                 \n")
		.append("INNER JOIN formfield ff ON ft.tabid = ff.tabid                                \n")
		.append("INNER JOIN userprofile up ON ff.profilefieldid = up.profilefieldid            \n")
		.append("INNER JOIN datafield df ON df.datafieldid = up.datafieldid                    \n")
		.append("WHERE f.formtype = 'registro'                                                 \n")
		.append("  AND f.groupid = %s                                                          \n")
		// ¡¡IMPORTANTE!!, el campo ABOID NO debe salir en esta consulta, de lo contrario el algoritmo de importación falla (se repite una columna en el insert)
		.append("  AND up.fieldname not in('XYZ_FIELD_ABOID_ZYX')                              \n")
		.append("ORDER BY up.required desc                                                     \n")
		.toString();
		
		// Actualiza la fecha de finalizacion de la importacion
		private final String UPDATE_FINISHTIME_IMPORT = new StringBuffer()
			.append("UPDATE importation \n")
			.append("SET finishtime = sysdate() \n")
			.append("WHERE importid = '%s' \n")
			.append("LIMIT 1").toString();
						
		// Consulta para listado de importaciones de usuarios
		private final String GET_IMPORTATIONS_LIST = new StringBuffer()
			.append("SELECT importid id, filename fn, starttime st, finishtime ft, ok, ko, ok + ko t, host \n")
			.append("FROM importation                                                                      \n")
			.append("WHERE groupid = %s       											                   \n")
			// Filtros
			.append("%s")
			.append("ORDER BY %s                                                                           \n")
			.append("LIMIT %s, %s").toString();
		
		// Consulta para listado de detalles de importaciones de usuarios
		private final String GET_DETAILS_USERS_IMPORTATIONS_LIST = new StringBuffer() 
			.append("SELECT importdetailid id, starttime st, finishtime ft, usrname u, email e, errorcode ec, \n") 
		    .append("       left(errordetail, " + ERROR_DETAIL_LENGTH + ") ed                                 \n")
			.append("FROM importationdetail																	  \n")
			.append("WHERE importid = '%s' 																	  \n")
			// Filtros
			.append("%s")
			.append("ORDER BY %s                                         									  \n")
			.append("LIMIT %s, %s").toString();
					
		public static ThreadGroup getUsersThreadGroup()
		{		
			return usersThreadGroup;
		}

		// Actualiza la fecha de finalizacion de la importacion
		public void updateImportationFinishTime(String importId) throws ServiceError, IOException, SQLException
		{
			_log.trace("In updateImportationFinishTime");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId ids is null");
			final String sql = String.format(UPDATE_FINISHTIME_IMPORT, importId);
			
			_log.debug("Query to update importation finish time: \n" + sql);
			
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
		
		// Listado de importación de usuarios
		public String getImportsList(String groupId, String type, String xmlFilters, String startIn, String limit, String sort) 
						throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
		{
			_log.trace("In getImportsList");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId ids is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(type),    IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "type ids is null"   );
			
			final long t0 = Calendar.getInstance().getTimeInMillis();
			
			final String sql = String.format(GET_IMPORTATIONS_LIST, groupId, SQLQueries.buildFilters(xmlFilters, false),
											 getSqlSort(sort),                             
											 (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                             
				                             (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit))				                             
				                            );
			_log.debug("Query to get user importations: \n" + sql);
			
			final String data = PortalLocalServiceUtil.executeQueryAsDom(sql).asXML();	
			
			_log.debug("Time to importation list: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");
			
			return data;
		}
		
		// Listado de detalles importación de usuarios
		public String getDetailsUsersImportsList(String importId, String xmlFilters, String startIn, String limit, String sort) 
						throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
		{
			_log.trace("In getDetailsUsersImportsList");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Import id is null");			
			
			final long t0 = Calendar.getInstance().getTimeInMillis();
			
			final String sql = String.format(GET_DETAILS_USERS_IMPORTATIONS_LIST, importId, SQLQueries.buildFilters(xmlFilters, true),
				                       getSqlSort(sort),
				                       (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                       
				                       (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit)));
			_log.debug("Query to get details users importations details: \n" + sql);			
			final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			_log.debug("Time to details users importation list: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms\n\n");
			
			return result.asXML();
		}
		
		// Importa usuarios.
		public String importUsers(String path, String pathIsFile, String backupPath, String groupId, String passwordInMD5, String deleteUsers) throws Exception{
			_log.trace(buildUserPrefixLog(null) + " In ImportMgrLocalServiceImpl.importUsers");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(path),          IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xmlDirectory is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(pathIsFile),    IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "pathIsFile is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(backupPath),    IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xmlBackupDirectory is null");
			ErrorRaiser.throwIfFalse(!path.equalsIgnoreCase(pathIsFile), XmlioKeys.XYZ_IMPORT_PATH_SAME_AS_BACKUP_PATH_ZYX, "path and backup path can not be the same");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), 		 IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(passwordInMD5), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "passwordInMD5 is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(deleteUsers),   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "deleteUsers is null");
			
			final long t0 = Calendar.getInstance().getTimeInMillis(); 
					
	        // Comprobamos las rutas existen y estan preparadas
			final File workingPath = new File(path);				
			ErrorRaiser.throwIfFalse(workingPath.exists() && workingPath.canRead(), XmlioKeys.XYZ_IMPORT_FILE_NOT_FOUND_ZYX, "Error reading: " + workingPath);
			
			final File backupDirectory  = new File(backupPath);
			ErrorRaiser.throwIfFalse(backupDirectory.exists() && backupDirectory.canWrite(), XmlioKeys.XYZ_IMPORT_BACKUP_FOLDER_NOT_FOUND_ZYX, "Error reading: " + backupDirectory);
			
			// Comprobamos que la ruta es lo que el cliente dice ser (directorio o archivo)
			final boolean isFile = GetterUtil.getBoolean(pathIsFile);				
			if (isFile)				
				ErrorRaiser.throwIfFalse(workingPath.isFile(), XmlioKeys.XYZ_IMPORT_FILE_NOT_FOUND_ZYX, "path given is not a file: " + workingPath);
			else
				ErrorRaiser.throwIfFalse(workingPath.isDirectory(), XmlioKeys.XYZ_IMPORT_FOLDER_NOT_FOUND_ZYX, "path given is not a directory: " + workingPath);	
			
			// Obtenemos los userprofiles del formulario de registro.
			final String sql = String.format(GET_USER_PROFILES, groupId);
			_log.debug(new StringBuffer(buildUserPrefixLog(null)).append("Query to get user profile of register form: \n").append(sql).toString());
			final List<Object> userProfile = PortalLocalServiceUtil.executeQueryAsList(sql);
	        ErrorRaiser.throwIfFalse(null != userProfile && userProfile.size() > 0, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, buildUserPrefixLog(null) + "No userprofiles found in the register form or there is not a register form");	
			
			// Creamos el grupo de hilos si no existe
			if (null == usersThreadGroup){
				_log.debug(buildUserPrefixLog(null) + "User import thread group created");
				ImportMgrLocalServiceImpl.usersThreadGroup = new ThreadGroup(XmlioKeys.IMPORT_TYPE_USERS); // USERS
			}
			
			Document xmlResult = SAXReaderUtil.read("<rs/>");
			
			// Pasamos este objeto para detectar errores en el hilo ImportUserThreadMgr
			ArrayList<Exception> excepcionList = new ArrayList<Exception>();						
			
			// El nombre del hilo (nombre del grupo + uui) nos servirá para luego
			ImportUserThread iUTM = new ImportUserThread(usersThreadGroup, usersThreadGroup.getName() + PortalUUIDUtil.newUUID(), 
		                                                 excepcionList, xmlResult, groupId, 
				                                         GroupMgr.getDefaultUserId(), userProfile, workingPath, 
				                                         backupDirectory, GetterUtil.getBoolean(passwordInMD5, false), 
				                                         GetterUtil.getBoolean(deleteUsers, false), true);
			// Comenzamos el hilo
			iUTM.start();
			_log.debug(buildUserPrefixLog(null) + "Thread ImportUserThreadMgr starts");			
			
			final Element root = xmlResult.getRootElement();
			// Mientras el hilo ImportUserThread no modifique la variable xmlResult (se haya insertado un importation o haya un error)
			_log.debug(buildUserPrefixLog(null) + "waiting in ImportMgrLocalServiceImpl for the first importation insert or a error");
			
			final long fW = Calendar.getInstance().getTimeInMillis();
			
			/* Este continua mientras:
			 		- No se produzca un error en la primera importacion (solo en la primera) 
			 		- Mientras no se haya insertado el primer registro importation en base de datos y
			 		- Mientas no haya pasado el tiempo máximo establecido en MAX_WAITING_FLEX_TIME (por bucle infinito) */
			while (excepcionList.size() == 0                               && 
				   null == XMLHelper.getTextValueOf(root, "@toFlex", null) && 
				   (Calendar.getInstance().getTimeInMillis() - fW < ImportMgrLocalServiceImpl.MAX_WAITING_FLEX_TIME)
				  ){	
				Thread.sleep(WAITING_TO_FLEX);
			}
			_log.debug(buildUserPrefixLog(null) + "'toFlex' received in ImportMgrLocalServiceImpl from ImportUserThreadMgr.java");			
			_log.debug(buildUserPrefixLog(null) + "Total ImportMgrLocalServiceImpl.importUsers time elapsed: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");
			
			// Nos ha llegado un error antes de insertar el primer importation (los siguientes errores irán por BBDD), mandamos la excepción a flex
			if (excepcionList.size() > 0){
				throw excepcionList.get(0);
			}			
			return xmlResult.asXML();
		}
		
		// Para de forma controlada una importacion de usuarios
		public void stopUserImport(String importId, String host) throws ServiceError, ClientProtocolException, IOException, JSONException, SystemException{
			_log.trace("In stopUserImport");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId is null");	
			ErrorRaiser.throwIfFalse(Validator.isNotNull(host),     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "host is null");
			
			// Obtengo la ip del servidor actual
			final String actualIp = getActualIp();			
			ErrorRaiser.throwIfNull(actualIp, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "PropsKeys.ITER_LIVE_SERVERS_OWNURL is null");
					
			// El hilo se lanzó en el servidor actual
			if (actualIp.equals(host)){
				_log.debug("The user import " + importId + " thread was created in the actual server: " + actualIp);
				
				ErrorRaiser.throwIfFalse(Validator.isNotNull(usersThreadGroup), XmlioKeys.XYZ_IMPORT_ALREADY_FINISHED_ZYX, "They are not user importation running to stop");
				_log.debug("Import user thread group found");
				
				// Vemos si hay hilos activos del grupo (hay que hacerlo "a mano", java contabiliza a veces hilos del sistema también)			
				final int threadsCount = countActiveThreads(ImportMgrLocalServiceImpl.usersThreadGroup, ImportMgrLocalServiceImpl.usersThreadGroup.getName());				
				_log.debug("Import user threads found: " + threadsCount);
				ErrorRaiser.throwIfFalse(threadsCount > 0, XmlioKeys.XYZ_IMPORT_ALREADY_FINISHED_ZYX, "The user import '" + importId + "' on the server '" + host + "' already finished");
				
				if (null == userImportationsToCancel){
					userImportationsToCancel = new ArrayList<String>();
				}
				// Añadimos la importación a la lista de cancelación
				if (!userImportationsToCancel.contains(importId)){
					userImportationsToCancel.add(importId);
				}
			// El hilo se lanzó en otro servidor (parte pareja a NewsletterMgrLocalServiceImpl.requestSendAlertNewsletters)
			}else{
				_log.debug("The user import thread was created in other server: " + actualIp);
				
				List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();								
				remoteMethodParams.add(new BasicNameValuePair("serviceClassName",  "com/protecmedia/iter/xmlio/service/impl/ImportMgrServiceUtil"));				
				remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "stopUserImport"));
				remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[importId, host]"));
				remoteMethodParams.add(new BasicNameValuePair("importId",	       importId));
				remoteMethodParams.add(new BasicNameValuePair("host",              host));
				
				final int connTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(PropsKeys.ITER_LIVE_SERVERS_CONEXIONTIMEOUT), 2) *  1000;
				final int readTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(PropsKeys.ITER_LIVE_SERVERS_RESPONSETIMEOUT), 30) * 1000;
				
				final URL url = new URL(host);
				HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());				
				JSONUtil.executeMethod(targetHost, "/base-portlet/json", remoteMethodParams, connTimeout, readTimeout);	
			}
		}
		
		// Una vez cancelada la importación de usuario, se borra de la lista de importaciones a cancelar.
		public static void removeUserImportationToCancel(String userImportId){
			if (Validator.isNotNull(userImportationsToCancel) && userImportationsToCancel.contains(userImportId)){
				userImportationsToCancel.remove(userImportId);
			}
		}
		
		// Inserta un registro en importation
		public void insertUserImport(String importId, String groupId, File file, String serverIp) throws ServiceError, IOException, SQLException{
			_log.trace("In insertUserImport");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ImportId is null" );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId),   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "GroupId is null"  );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(serverIp),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "serverIp is null" );
			
			final String sql = String.format(INSERT_USER_IMPORT, importId, groupId, 
											 // Reemplazamos las barras \ por / para que en la base de datos salgan las barras cuando se importa desde windows
	                                         (null != file ? "'" + file.getAbsolutePath().replaceAll("\\\\", "/") + "'" : "null"), 
	                                         serverIp
	                                        );
			_log.debug(new StringBuffer("Query to insert user import(importation):\n").append(sql).toString());
			PortalLocalServiceUtil.executeUpdateQuery(sql);	
		}
			
		// Escribe un detalle de una importacion en base de datos
		public void insertUserDetail(String importId, String groupId, Node detail) throws ServiceError, IOException, SQLException, DocumentException
		{
			_log.trace("In insertImportDetail");
												
			final String detailId    = XMLHelper.getTextValueOf(detail, "@id"               );							
			final String errorCode   = XMLHelper.getTextValueOf(detail, "@errorCode"        );
			final String errordetail = XMLHelper.getTextValueOf(detail, "errordetail/text()");  

			// Es un xml que identifica la importación
			final Document subjectDocument = SAXReaderUtil.read(XMLHelper.getTextValueOf(detail, "./subject"));							
						
			final String userName       = XMLHelper.getTextValueOf(subjectDocument, "/u/@name"   );			
			final String userEmail      = XMLHelper.getTextValueOf(subjectDocument, "/u/@email"  );
			final String userStartTime  = XMLHelper.getTextValueOf(detail,          "@starttime" );
			final String userFinishTime = XMLHelper.getTextValueOf(detail,          "@finishtime");
			
			final String sql = String.format(INSERT_IMPORTDETAIL, 
					            "'" + detailId       + "'",
						        "'" + importId       + "'",
						        "'" + userStartTime  + "'",
							    "'" + userFinishTime + "'",
							    getValue(MAX_USRNAME, userName),
								getValue(MAX_EMAIL, userEmail),
								(Validator.isNull(errorCode)   ? "null" : "'" + StringEscapeUtils.escapeSql(errorCode)   + "'"),				   
								(Validator.isNull(errordetail) ? "null" : "'" + StringEscapeUtils.escapeSql(errordetail) + "'") 
							   );
					
			// Insertamos el detalle 
			if(_log.isDebugEnabled())
				_log.debug(new StringBuffer("Query to insert import detail:\n").append(sql).toString());	
			PortalLocalServiceUtil.executeUpdateQuery(sql);						
		}
		
		private String getValue(int maxSize, String value)
		{
			String retVal = "null";
			
			if(Validator.isNotNull(value))
			{
				retVal = value;
				
				if(retVal.length() > maxSize)
					retVal = retVal.substring(0, maxSize - 1);
				
				retVal = retVal.replaceAll("[{|}]", StringPool.BLANK);
				
				retVal = StringEscapeUtils.escapeSql(retVal);
				
				retVal = StringUtil.apostrophe(retVal);
			}
				
			return retVal;
		}
		
		// Crea/unifica el inicio de cada salida de log de usuarios
		public String buildUserPrefixLog(String importId){
			StringBuffer result = new StringBuffer(XmlioKeys.PREFIX_USER_LOG);
			
			if (Validator.isNotNull(importId)){
				result.append("importId: ").append(importId).append(". ");
			}
			return result.toString();
		}
	// IMPORTACIÓN DE USUARIOS	
		
	
	// IMPORTACIÓN DE ARTICULOS	
		private static final String GET_IMPORTID_FROM_IMPORTARTDETAILS_IMPORTDETAILID = new StringBuilder()
			.append("SELECT importid \n")
			.append("FROM importartdetails \n") 
			.append("WHERE importdetailid = '%s'").toString();			
		
		private static final String UPDATE_STARTTIME_IMPORTARTICLE = new StringBuilder()
			.append("UPDATE importarticles \n")
			.append("SET starttime = sysdate() \n")
			.append("WHERE importid = '%s' \n")
			.append("LIMIT 1").toString();
		
		private static final String UPDATE_FINISHTIME_IMPORTARTICLE = new StringBuilder()
			.append("UPDATE importarticles \n")
			.append("SET finishtime = sysdate() \n")
			.append("WHERE importid = '%s' \n")
			.append("LIMIT 1").toString();
		
		private static final String UPDATE_STARTTIME_AND_FINISHTIME_IMPORTARTICLE = new StringBuilder()
			.append("UPDATE importarticles \n")
			.append("SET starttime = '%s', \n")
			.append(" finishtime = '%s' \n")
			.append("WHERE importid = '%s'").toString();
		
		private static final String UPDATE_STARTTIME_AND_FINISHTIME_IMPORTARTICLEDETAIL = new StringBuilder()
			.append("UPDATE importartdetails \n")
			.append("SET starttime = '%s', \n")
			.append(" finishtime = '%s' \n")
			.append("WHERE importdetailid = '%s'").toString();
		
		private static final String MARK_IMPORTATION_AS_DELETING = new StringBuilder()
			.append("UPDATE importarticles \n")
			.append("SET starttime = null, \n")
			.append(  "finishtime = null, \n")
			.append("  host = '%s' \n")
			.append("WHERE importid in (%s)").toString();
		
		private static final String MARK_IMPORTARTDETAIL_AS_DELETING = new StringBuilder()
			.append("UPDATE importartdetails \n")
			.append("SET starttime = null, \n")
			.append("  finishtime = null \n")
			.append("WHERE importdetailid in (%s)").toString();
						
		private static final String GET_ARTICLES_IMPORTATIONS_LIST = new StringBuffer()
			.append("SELECT importid id, filename fn, starttime st, finishtime ft, ok, ko, ok + ko t, host \n")
			.append("FROM importarticles                                                                   \n")
			.append("WHERE groupid = %s                                                                    \n")
			// Filtros opcionales
			.append("%s")
			.append("ORDER BY %s    																       \n")
			.append("LIMIT %s, %s").toString();
		
		private static final String GET_DETAILS_ARTICLES_IMPORTATIONS_LIST = new StringBuffer() 
			.append("SELECT importdetailid id, starttime st, finishtime ft, articleid ai, title t, errorcode ec, " + 
		                   " left(errordetail, " + ERROR_DETAIL_LENGTH + ") ed \n")
			.append("FROM importartdetails \n")
			.append("WHERE importid = '%s' \n")
			// Filtros
			.append("%s")
			.append("ORDER BY %s \n")
			.append("LIMIT %s, %s").toString();
				
		private static final String GET_EXPANDO_TABLE_ID    = "\n\t SELECT tableId FROM expandotable WHERE classNameId=%s AND name='%s'";		
		private static final String GET_EXPANDO_COLUMNS_IDS = "\n\t SELECT name, columnid FROM expandocolumn WHERE tableid=%s AND name IN ('%s', '%s')";
		
		private static final String INSERT_ARTICLE_IMPORT_DETAIL = new StringBuffer()
			.append("INSERT INTO importartdetails(importdetailid, importid, starttime, finishtime, articleid, title, errorcode, errordetail) VALUES \n")
			.append("(%s, %s, %s, %s, %s, %s, %s, %s)").toString();
		
		private static final String INSERT_ARTICLE_IMPORT = new StringBuffer()
			.append("INSERT INTO importarticles(importid, groupid, filename, starttime, host) VALUES \n")
			.append("('%s', %s, %s, sysdate(), '%s')").toString();
		
		private static final String GET_ARTICLES_FROM_BATCHS_AND_DETAILS = new StringBuffer()
			.append("SELECT ia.importid, iad.articleid, iad.importdetailid \n")
			.append("FROM importarticles ia, importartdetails iad, journalarticle j  \n")
			.append("WHERE ia.importid = iad.importid \n")
			.append("  AND iad.articleid = j.articleId \n")
			.append("  AND ia.groupid = %s \n")
			.append("  AND ia.importid = '%s' \n")			
			.append("  AND (iad.errorcode LIKE '%%warning%%' OR iad.errorcode IS NULL) \n")
			.append("%s").toString();
		
		public static final String UPDATE_DELETED_ARTICLE = new StringBuffer()
			.append("UPDATE importartdetails \n")
			.append(" SET errorcode = '").append(XmlioKeys.DETAIL_ERROR_CODE_DELETED)      .append("', \n")
			.append(" errordetail = '")  .append(XmlioKeys.DESC_ERR_ARTICLE_DELETE_BY_USER).append("', \n")
			.append("  starttime = '%s', \n")
			.append("  finishtime = sysdate() \n")			
			.append("WHERE importdetailid = '%s' \n")
			.append("LIMIT 1").toString();
		
		private static final String GET_BEFORE_IMPORTS_TIMES = new StringBuilder()
			.append("SELECT importid id, starttime, finishtime \n") 
		    .append("FROM importarticles \n")
		    .append("WHERE importid in (%s)").toString();
		
		private static final String GET_BEFORE_DETAILS_IMPORT_TIMES = new StringBuilder()
			.append("SELECT importdetailid id, starttime, finishtime \n")
			.append("FROM importartdetails \n")
			.append("WHERE importdetailid in (%s)").toString();
		
		private static final String GET_IMPORTS_CANCELED = new StringBuilder()
			.append("SELECT importid id \n")
			.append("FROM importarticles\n")
			.append("WHERE starttime IS NULL OR finishtime IS NULL \n")
			.append("  AND importid in(%s)").toString();
		
		private static final String GET_IMPORTSDETAIL_CANCELED = new StringBuilder()
			.append("SELECT importdetailid id \n")
			.append("FROM importartdetails \n")
			.append("WHERE starttime IS NULL OR finishtime IS NULL \n")
			.append("  AND importdetailid in(%s)").toString();
		
		// Listado de las importaciones/borrados de artículos que se desean cancelar
		private static List<String> articleImportationsToCancel;
		
		// Devuelve la lista de importaciones a cancelar
		synchronized public static List<String> getArticleImportationsToCancel(){
			return articleImportationsToCancel;
		}
		
		// Una vez cancelada la importación de artículos, se borra de la lista de importaciones a cancelar.
		synchronized public static void removeArticleImportationToCancel(String articleImportId){
			if (Validator.isNotNull(articleImportationsToCancel) && articleImportationsToCancel.contains(articleImportId)){
				articleImportationsToCancel.remove(articleImportId);
			}
		}	
		
		// Actualiza la fecha de iniciación de importarticles
		private void updateImportArticlesStartTime(String importId) throws ServiceError, IOException, SQLException{
			_log.trace("In updateImportArticlesStartTime");			
		
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId ids is null");
			
			final String sql = String.format(UPDATE_STARTTIME_IMPORTARTICLE, importId);		
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to update importarticles start time: \n").append(sql));		
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
		
		// Actualiza la fecha de finalizacion de la importarticles
		public void updateImportarticlesFinishTime(String importId) throws ServiceError, IOException, SQLException{
			_log.trace("In updateImportarticles");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId ids is null");
			
			final String sql = String.format(UPDATE_FINISHTIME_IMPORTARTICLE, importId);
			if(_log.isDebugEnabled())
				_log.debug("Query to update importarticles finish time: \n" + sql);			
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
				
		// Creamos el grupo de hilos si no existe
		private void createArticlesThreadGroup(){
			if (null == articlesThreadGroup){
				if(_log.isDebugEnabled())
					_log.debug(buildArticlePrefixLog(null) + "Article import thread group created");
				ImportMgrLocalServiceImpl.articlesThreadGroup = new ThreadGroup(XmlioKeys.IMPORT_TYPE_ARTICLES); // ARTICLES
			}
		}
		
		// Inserta un registro en importarticles
		public void insertArticleImport(String importId, String groupId, File file, String serverIp) throws ServiceError, IOException, SQLException{
			_log.trace("In insertArticleImport");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ImportId is null" );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId),   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "GroupId is null"  );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(serverIp),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "serverIp is null" );
			
			final String sql = String.format(INSERT_ARTICLE_IMPORT, importId, groupId, 
											 // Reemplazamos las barras \ por / para que en la base de datos salgan las barras cuando se importa desde windows
	                                         (null != file ? "'" + file.getAbsolutePath().replaceAll("\\\\", "/") + "'" : "null"),
	                                         serverIp
	                                        );
			if(_log.isDebugEnabled())
				_log.debug(new StringBuffer("Query to insert article import(importarticles):\n").append(sql).toString());
			PortalLocalServiceUtil.executeUpdateQuery(sql);	
		}
		
		// Listado de importación de artículos
		public String getArticlesImportsList(String groupId, String xmlFilters, String startIn, String limit, String sort) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
		{
			_log.trace("In getArticlesImportsList");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId ids is null");			
			
			final long t0 = Calendar.getInstance().getTimeInMillis();
						
			final String sql = String.format(GET_ARTICLES_IMPORTATIONS_LIST, groupId, SQLQueries.buildFilters(xmlFilters, false),
									         getSqlSort(sort),                             
								         	 (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                             
				                             (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit)) );
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to get articles importations: \n").append(sql));			
			final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);			
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Time to articles importation list: ").append((Calendar.getInstance().getTimeInMillis() - t0)).append(" ms\n\n"));
			
			return result.asXML();
		}
		
		// Listado de detalles importación de artículos
		public String getDetailsArticlesImportsList(String importId, String xmlFilters, String startIn, String limit, String sort) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
		{
			_log.trace("In getDetailsArticlesImportsList");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Import id is null");			
			
			final long t0 = Calendar.getInstance().getTimeInMillis();
			
			final String sql = String.format(GET_DETAILS_ARTICLES_IMPORTATIONS_LIST, importId, SQLQueries.buildFilters(xmlFilters, true),
										     getSqlSort(sort),
									     	 (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                             
				                             (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit)) );
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to get details articles importations details: \n").append(sql));			
			final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Time to details articles importation list: ").append((Calendar.getInstance().getTimeInMillis() - t0)).append(" ms\n\n"));
			
			return result.asXML();
		}
		
		// Escribe un detalle de una importacion en base de datos
		public void insertArticleDetail(String importId, String groupId, Node detail) throws ServiceError, IOException, SQLException, DocumentException{
			_log.trace("In insertImportDetail");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId),   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ImportId is null");		
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId),    IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "GroupId is null" );	
			ErrorRaiser.throwIfFalse(Validator.isNotNull(detail),     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Detail is null"  );
										
			final String detailId    = XMLHelper.getTextValueOf(detail, "@id"         );							
			final String errorCode   = XMLHelper.getTextValueOf(detail, "@errorCode"  );
			final String errordetail = XMLHelper.getTextValueOf(detail, "@errordetail");  

			// Es un xml que identifica la importación
			final Document subjectDocument = SAXReaderUtil.read(XMLHelper.getTextValueOf(detail, "./subject"));							
						
			String articleId         = XMLHelper.getTextValueOf(subjectDocument, "/a/@articleId", "");
			articleId = (articleId.length() > MAX_ARTICLEID ? articleId.substring(0, MAX_ARTICLEID - 1) : articleId);			
			final String articleStartTime  = XMLHelper.getTextValueOf(detail,          "@starttime"   );
			final String articleFinishTime = XMLHelper.getTextValueOf(detail,          "@finishtime"  );
			String urlTitle      	   = XMLHelper.getTextValueOf(subjectDocument, "/a/@title", "");
			
			// controla que el limite del campo title de la tabla importartdetails no sea sobrepasado
			urlTitle = (urlTitle.length() > MAX_IMPORT_DETAILS_TITLE ? urlTitle.substring(0, MAX_IMPORT_DETAILS_TITLE - 1) : urlTitle);
			
			final String sql = String.format(INSERT_ARTICLE_IMPORT_DETAIL, 
							    "'" + detailId          + "'",
							    "'" + importId          + "'",
							    "'" + articleStartTime  + "'",
							    "'" + articleFinishTime + "'",
							    "'" + articleId 		+ "'",
			            		(null == urlTitle ? "null" : "'" + StringEscapeUtils.escapeSql(urlTitle) + "'"),  
						        (null == errorCode    ? "null" : "'" + StringEscapeUtils.escapeSql(errorCode)    + "'"),  
								(null == errordetail  ? "null" : "'" + StringEscapeUtils.escapeSql(errordetail)  + "'")
							   );	
			
			// Insertamos el detalle 
			if(_log.isDebugEnabled())
				_log.debug(new StringBuffer("Query to insert import detail:\n").append(sql).toString());	
			PortalLocalServiceUtil.executeUpdateQuery(sql);						
		}
		
		public String importArticles(String path, String pathIsFile, String backupPath, String groupId, String deleteArticles,
            				         String ifArticleExists, String ifLayoutNotExists, String startDate, String finishDate,
            				         String ifNoCategory, String ifNoSuscription, String legacyIsEncoded) throws Exception{
			
			_log.trace(new StringBuilder(buildArticlePrefixLog(null)).append(" In importArticles"));
			
			ErrorRaiser.throwIfNull(path,   			IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "working directory is null"  );
			ErrorRaiser.throwIfNull(pathIsFile, 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "pathIsFile is null"         );			
			ErrorRaiser.throwIfNull(backupPath, 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "backup directory  is null"  );
			ErrorRaiser.throwIfFalse(!path.equalsIgnoreCase(pathIsFile), XmlioKeys.XYZ_IMPORT_PATH_SAME_AS_BACKUP_PATH_ZYX, "path and backup path can not be the same");
			ErrorRaiser.throwIfNull(groupId,    		IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "group friendly name is null");
			ErrorRaiser.throwIfNull(deleteArticles,    	IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "deleteArticles is null"     );			
			
			ErrorRaiser.throwIfNull(legacyIsEncoded, 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "legacyIsEncoded is null"    );
			
			final boolean operationIsDelete = GetterUtil.getBoolean(deleteArticles, false);		
			boolean ifTheArticleExists   = false;
			boolean ifTheLayoutNotExists = true;
			if(!operationIsDelete){
				ErrorRaiser.throwIfNull(startDate, 		   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importationStart is null" );
				ErrorRaiser.throwIfNull(finishDate, 	   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importationFinish is null");
				ErrorRaiser.throwIfNull(ifArticleExists,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ifArticleExists is null"  );
				ErrorRaiser.throwIfNull(ifLayoutNotExists, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ifLayoutNotExists is null");
				ErrorRaiser.throwIfNull(ifNoCategory, 	   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ifNoCategory is null"     );
				ErrorRaiser.throwIfNull(ifNoSuscription,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "ifNoSuscription is null"  );
				
				ifTheArticleExists   = GetterUtil.getBoolean(ifArticleExists,   false); // borrarlo: 0 fallar: 1
				ifTheLayoutNotExists = GetterUtil.getBoolean(ifLayoutNotExists, true ); // crearlo:  0 fallar: 1
			}
			
			final boolean legacyEncoded = GetterUtil.getBoolean(legacyIsEncoded, false); // Indica si legacy viene ya codificado o no. 			
			
			// Comprobamos que las rutas existen y estan preparadas
			final File workingPath = new File(path);				
			ErrorRaiser.throwIfFalse(workingPath.exists() && workingPath.canRead(), XmlioKeys.XYZ_IMPORT_FILE_NOT_FOUND_ZYX, buildArticlePrefixLog(null) + "Error reading: " + workingPath);
			
			final File backupDirectory  = new File(backupPath);
			ErrorRaiser.throwIfFalse(backupDirectory.exists() && backupDirectory.canWrite(), XmlioKeys.XYZ_IMPORT_BACKUP_FOLDER_NOT_FOUND_ZYX, buildArticlePrefixLog(null) + "Error reading: " + backupDirectory);
			
			// Comprobamos que la ruta es lo que el cliente dice ser (directorio o archivo)
			final boolean isFile = GetterUtil.getBoolean(pathIsFile);				
			if (isFile)				
				ErrorRaiser.throwIfFalse(workingPath.isFile(), XmlioKeys.XYZ_IMPORT_FILE_NOT_FOUND_ZYX, "path given is not a file: " + workingPath);
			else
				ErrorRaiser.throwIfFalse(workingPath.isDirectory(), XmlioKeys.XYZ_IMPORT_BACKUP_FOLDER_NOT_FOUND_ZYX, "path given is not a directory: " + workingPath);
			
			// Comprobamos que las fechas de inicio y fin de importación son válidas.
			Date importationStart  = null;
			Date importationFinish = null;
			if(!operationIsDelete){				
				try{
					final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT);
					importationStart = SDF.parse(startDate + " 00:00:00");
				}catch(Exception e){
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid importation start date");
				}	
				
				try{
					final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT);
					importationFinish = SDF.parse(finishDate + " 23:59:59");
				}catch(Exception e){
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid importation finish date");
				}
				// La fecha de inicio ha de ser anterior a la de fin
				ErrorRaiser.throwIfFalse(importationStart.before(importationFinish), XmlioKeys.XYZ_IMPORT_START_DATE_AFTER_FINISH_IMPORT_DATE_ZYX, "Importation start date can not be after finish date");
			}			
			
			// Creamos el grupo de hilos si no existe
			createArticlesThreadGroup();
			
			// Elementos comunes para el proceso de importación
			final long tIni          = Calendar.getInstance().getTimeInMillis();
			final long globalGroupId = GroupMgr.getGlobalGroupId();
			final long defaultUserId = GroupMgr.getDefaultUserId();
			final long jaClassNameId = PortalUtil.getClassNameId(JournalArticle.class.getName());
			
			String sql = String.format(GET_EXPANDO_TABLE_ID, jaClassNameId, ExpandoTableConstants.DEFAULT_TABLE_NAME);
			final List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
			ErrorRaiser.throwIfFalse( Validator.isNotNull(result) && Validator.isNotNull(result.get(0)) , IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_ARTICLE_LOG + "Expando table not found");			
			final String expandoTableId = ((Object)result.get(0)).toString();
			
			sql = String.format(GET_EXPANDO_COLUMNS_IDS, expandoTableId, IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID, WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS);
			final Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
			final String xpathExpr  = "/rs/row[@name='%s']/@columnid";
			
			Node expColGrp  = d.selectSingleNode( String.format(xpathExpr, IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID) );
			Node expColMeta = d.selectSingleNode( String.format(xpathExpr, WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS) );
			//ticket 0009891. Mostrar error si en la tabla 'expandoColumn' no existe un registro con name 'expandoScopeGroupId' o 'mainMetadatasIds' 
			ErrorRaiser.throwIfFalse( Validator.isNotNull(expColGrp) && Validator.isNotNull(expColMeta) , XmlioKeys.XYZ_IMPORTATION_NOT_STARTED_NECESARY_PUBLISH_TO_LIVE_ZYX);			
			
			final String expColGrpColumnId  =  expColGrp.getStringValue();
			final String expColMetaColumnId  = expColMeta.getStringValue();
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" Time getting the common information to the importation: ").append((Calendar.getInstance().getTimeInMillis() - tIni)).append(" ms\n"));	
			
			// Documento que se modificará para indicar cómo ha ido la operación
			Document xmlResult = SAXReaderUtil.read("<rs/>"); 			
			// Pasamos este objeto para detectar errores en el hilo ImportUserThreadMgr
			ArrayList<Exception> excepcionList = new ArrayList<Exception>();
			// El nombre del hilo (nombre del grupo + uuid) no servira para despues.
			ImportContentThread importContent = new ImportContentThread(articlesThreadGroup, articlesThreadGroup.getName() + PortalUUIDUtil.newUUID(), 
				                                                        excepcionList, xmlResult, 
				                                                        groupId, defaultUserId, globalGroupId, jaClassNameId, expColGrpColumnId, 
				                                                        expColMetaColumnId, expandoTableId, workingPath, backupDirectory, 
				                                                        importationStart, importationFinish,
				                                                        operationIsDelete, legacyEncoded, ifTheArticleExists,
				                                                        ifNoCategory, ifTheLayoutNotExists, ifNoSuscription, 
				                                                        importThreads);	
			// Lanzamos el hilo
			importContent.start();
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(buildArticlePrefixLog(null)).append("waiting in ImportMgrLocalServiceImpl for the first importarticles insert or a error"));

			// Espera a que se registre el primer ZIP o a que ocurra antes un error.
			try
			{
				importContent.waitForFirstImport(WAITING_TO_FLEX);
			}
			catch (InterruptedException e)
			{
				_log.debug("Waiting interrupted.");
			}
	
			// Nos ha llegado un error antes de insertar el primer importation (los siguientes errores irán por BBDD), mandamos la excepción a flex
			if (excepcionList.size() > 0){
				throw excepcionList.get(0);
			}
			return xmlResult.asXML();
	}
		
		// Para de forma controlada las importaciones de artículos
		public void stopArticleImport(String xml) throws ServiceError, ClientProtocolException, IOException, JSONException, SystemException, DocumentException{
			_log.trace("In stopArticleImport");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xml), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId is null");			
			
			Document xmlDoc = SAXReaderUtil.read(xml);			
			List<Node> importations = xmlDoc.getRootElement().selectNodes("//row");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importations), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xml is null");
			
			// Obtengo la ip del servidor actual
			final String actualIp = getActualIp();			
			ErrorRaiser.throwIfNull(actualIp, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "PropsKeys.ITER_LIVE_SERVERS_OWNURL is null");
			
			for (int i = 0; i < importations.size(); i++){
				final Node importation = importations.get(i);				
				
				String importId   = XMLHelper.getTextValueOf(importation, "@id");
				final String host = XMLHelper.getTextValueOf(importation, "@host", null);
				
				/* Se quiere parar el borrado de artículos desde el listado de detalles de la importación,
				 * nos llega el importartdetails.importdetailid en lugar del importarticles.importid,
				 * buscamos a qué importación pertenece.
				 */
				if (Validator.isNull(host)){
					final String sql = String.format(GET_IMPORTID_FROM_IMPORTARTDETAILS_IMPORTDETAILID, importId);
					importId = (String)PortalLocalServiceUtil.executeQueryAsList(sql).get(0);
				}
				
				// El hilo se lanzó en el servidor actual. Host llegará nulo cuando se quiere parar la eliminación de artículos desde el listado de detalles
				if (null == host || actualIp.equals(host)){
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("The article import ").append(importId).append(" thread was created in the actual server: '").append(actualIp).append("'"));
								
					// Manda la orden de parada a todas las importaciones para que se detenga la que esté procesando el importId indicado.
					synchronized (importThreads)
					{
						// Si no hay importaciones activas, lanza XYZ_IMPORT_ALREADY_FINISHED_ZYX. 
						ErrorRaiser.throwIfFalse(importThreads.size() > 0, XmlioKeys.XYZ_IMPORT_ALREADY_FINISHED_ZYX, "The article import '" + importId + "' on the server '" + host + "' already finished");
						// Manda la orden de parada a todas las importaciones activas. Sólo la que haya procesado el imporId indicado se detendrá.
						for (ImportContentThread t : importThreads)
						{
							if (t.isAlive())
							{
								t.stopImport(importId);
							}
						}
					}
					
				// El hilo se lanzó en otro servidor (parte pareja a NewsletterMgrLocalServiceImpl.requestSendAlertNewsletters)
				}else{
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("The article import thread was created in other server: '").append(host).append("'") );
					
					List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();								
					remoteMethodParams.add(new BasicNameValuePair("serviceClassName",  "com/protecmedia/iter/xmlio/service/impl/ImportMgrServiceUtil"));				
					remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "stopArticleImport"));
					remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[importId, host]") );
					remoteMethodParams.add(new BasicNameValuePair("importId",	       importId)           );
					remoteMethodParams.add(new BasicNameValuePair("host",              host)               );
					
					final int connTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(PropsKeys.ITER_LIVE_SERVERS_CONEXIONTIMEOUT),  2) * 1000;
					final int readTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(PropsKeys.ITER_LIVE_SERVERS_RESPONSETIMEOUT), 30) * 1000;
					
					final URL url = new URL(host);
					HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
					
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Calling server '").append(host).append("' to stop the import"));
					
					JSONUtil.executeMethod(targetHost, "/base-portlet/json", remoteMethodParams, connTimeout, readTimeout);	
				}
			}
		}
		
		// Crea/unifica el inicio de cada salida de log de articulos
		public String buildArticlePrefixLog(String importId){
			StringBuilder result = new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG);
			
			if (Validator.isNotNull(importId)){
				result.append("importId: ").append(importId).append(". ");
			}
			return result.toString();
		}
		
		// Comprueba que una sql es válida para borrar artículos
		private boolean queryIsASelect(String sql){
			boolean ok = false;
			
			final String sqlLowerCase = sql.toLowerCase();
			
			if (sqlLowerCase.indexOf("select "  ) != -1 &&
				sqlLowerCase.indexOf("articleid") != -1 &&
				sqlLowerCase.indexOf("delete "  ) == -1 &&
				sqlLowerCase.indexOf("create "  ) == -1 &&
				sqlLowerCase.indexOf("dropt "   ) == -1 &&
				sqlLowerCase.indexOf("update "  ) == -1 &&
				sqlLowerCase.indexOf("insert "  ) == -1 &&
				sqlLowerCase.indexOf("alter "   ) == -1 &&
				sqlLowerCase.indexOf("grant "   ) == -1 &&
				sqlLowerCase.indexOf("revoque " ) == -1 ){				
				ok = true;
			}
			return ok;
		}
		
		// Esta función no modifica registros en importarticles ni importartdetails al no conocer en que lote están los artículos obtenidos de la consulta
		synchronized public String selectToDeleteArticles(String groupId, String sql, boolean delete) throws ServiceError, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException, IllegalArgumentException, IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, SQLException, DocumentException{
			_log.trace("In selectToDeleteArticles");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(sql),     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "sql is null"    );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(sql),     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "sql is null"    );
			
			// Comprobamos que la consulta es un select			
			ErrorRaiser.throwIfFalse(queryIsASelect(sql), XmlioKeys.XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_INVALID_ZYX, "Invalid query");
			
			// Datos para flex
			Document result = SAXReaderUtil.read("<result/>");
			Element root = result.getRootElement();
			root.addElement("query").addCDATA(sql);
			
			Document doc = null;			
			try{
				doc = PortalLocalServiceUtil.executeQueryAsDom(sql);				
				ErrorRaiser.throwIfFalse(Validator.isNotNull(doc), XmlioKeys.XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_NO_RESULT_ZYX, "Query with not result");
			}catch(Exception e){
				ErrorRaiser.throwIfError(XmlioKeys.XYZ_INCORRECT_SQL_ZYX, "Invalid sql setence");
			}				
			
			final Element auxRoot = doc.getRootElement();
			final List<Node> articles = auxRoot.selectNodes("/rs/row");
			ErrorRaiser.throwIfFalse(null != articles && articles.size() > 0, XmlioKeys.XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_NO_RESULT_ZYX, "Query with not result");				
			root.addAttribute("found", Integer.toString(articles.size()) );
				
			if (delete){
				deletedPercentage = 0;
				
				// Para pasar estos parametros al hilo las variables tienen que ser final (Anonymous classes)
				final long group = GroupMgr.getGlobalGroupId();
				
				// Creamos el grupo de hilos si no existe
				createArticlesThreadGroup();				
				final String threadName = articlesThreadGroup.getName() + PortalUUIDUtil.newUUID();
				
				root.addElement("threadName").addCDATA(threadName);
				root.addElement("host").addCDATA(getActualIp());
				
				// Creamos el hilo en el grupo				
				final Thread t = new Thread(articlesThreadGroup, new Runnable(){
					
		            public void run() {		            	
		            	final int articlesSize = articles.size();
		    			for (int i = 0; i < articlesSize && continueDeleting(threadName); i++){
		    				String articleId = null;
		    				// Try para que aunque falle el borrado de un artículo siga con el resto
		    				try{
		    					final Node article = articles.get(i);					
		    					articleId = XMLHelper.getTextValueOf(article, "@articleid", XMLHelper.getTextValueOf(article, "@articleId", null));
		    					if (Validator.isNotNull(articleId)){
		    						// Quitamos el artículo con todas sus dependencias
		    						JournalArticleImportServiceUtil.deleteArticle(group, articleId, true) ;
		    						deletedPercentage = ( (double)(i+1) / articles.size()) * 100;
		    						if(_log.isDebugEnabled())
		    							_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" article with articleId: '").append(articleId).append("' and group = '").append(group).append("' deleted in selectToDeleteArticles"));
		    					}else{
		    						_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + " no article id found in the query");
		    					}
		    				}catch(Exception e){
		    					_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + "Error deleting article: '" + articleId + "' with group: '" + group + "' ", e);
		    				}		    				
		    			}
		            } 
		        });
				t.setName(threadName);	
		        t.start();
			}
			return result.asXML();
		}

		// Marca los registros de los listados (artículos y detalles) como borrándose
		public void markAsDeleting(String importsIds, String importDetailIds) throws IOException, SQLException, DocumentException{
			String sql;			
			if (Validator.isNotNull(importsIds)){				
				sql = String.format(MARK_IMPORTATION_AS_DELETING, getActualIp(), importsIds);
			}else{				
				sql = String.format(MARK_IMPORTARTDETAIL_AS_DELETING, importDetailIds);
			}
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Mark sql:\n").append(sql));
			
			PortalLocalServiceUtil.executeUpdateQuery(sql);				
		}
		
		/* Borrar artículos desde la pantalla de lotes y desde la pantalla de detalles de los lotes.
    	   Solo se pueden pasar variables finales a un hilo creado anonimamente */
		public String deleteArticlesFromBatchsAndDetailsList(String groupId, String importId, String xmlBatchsIds, String xmlArticlesDetailsIds) throws Exception{
			_log.trace("In deleteArticlesFromBatchAndDetailsList");	
			
			String lotesIds = null;
			if(Validator.isNotNull(xmlBatchsIds))
				lotesIds = getIdsToSqlFromXml(xmlBatchsIds, true);
			final String finalBatchsIds = lotesIds;
			
			String detallesIds = null;
			if (Validator.isNotNull(xmlArticlesDetailsIds))
				detallesIds = getIdsToSqlFromXml(xmlArticlesDetailsIds, true);
			final String finalDetailsIds = detallesIds;
			
			// Obtenemos el listado de las importaciones directamente con los ids de los lostes o forzamos un documento con el id de la importacion a la que pertenecen los artículos que se quieren borrar.
			Document batchsDocument = null;	            		
			if (Validator.isNotNull(xmlArticlesDetailsIds)){				
				batchsDocument = SAXReaderUtil.read("<rs/>");		    				
				batchsDocument.getRootElement().addElement("row").addAttribute("id", importId);				
			}else{
				batchsDocument = SAXReaderUtil.read(xmlBatchsIds);
			}
        	final List<Node> batchsIdsList = batchsDocument.selectNodes("/rs/row");   
        	
			final String finalGroup   = groupId;			
			final StringBuffer toFlex = new StringBuffer("");
			final long globalGroupId  = GroupMgr.getGlobalGroupId();
			final String finalXmlArticlesDetailsIds  = xmlArticlesDetailsIds;
			final ArrayList<Exception> excepcionList = new ArrayList<Exception>();								
					
			Runnable r = new Runnable(){	
	            public void run() {
	            	String importId = null;
	            	HashMap<String, String[]> beforeTimes = null;	// Tiempos anteriores al borrado (por si hubiese que restaurarlos)	            	
	            	
	            	try{
		            	int batch;		            	
		            	int batchSize = batchsIdsList.size();
		    			for (batch = 0; batch < batchSize; batch++){
		    				boolean atLeastOneImportDetailDeleted = false;
		    				try{		    					
		    					importId = XMLHelper.getTextValueOf(batchsIdsList.get(batch), "@id");
		    					
		    					// Obtenemos los tiempos anteriores de las importaciones y detalles
		    					if (batch == 0)
		    						beforeTimes = getBeforeTimes(importId, finalBatchsIds, finalDetailsIds);
		    					
		    					if (!continueDeleting(importId))
		    						break;
		    					
		    					// Marcamos como eliminandose la importacion y los detalles
		    					markAsDeleting( (null == finalBatchsIds ? ("'" + importId + "'") : finalBatchsIds), null);
			            		if(Validator.isNotNull(finalDetailsIds))	        			
			            			markAsDeleting(null, finalDetailsIds);	        		
		    					
			    				// Sql para obtener los artículos
								String sql = getSqlArticles(finalGroup, importId, finalXmlArticlesDetailsIds);				    			
			    				final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
			    				
			    				final List<Node> articlesIds = result.selectNodes("/rs/row");		    			 	
			    				// Lanzamos error si no hay artículos que borrar (porque ya se han borrado antes por ejemplo)
								ErrorRaiser.throwIfFalse(null != articlesIds && articlesIds.size() > 0, XmlioKeys.XYZ_NO_ARTICLES_TO_DELETE_ZYX, "No articles to delete");	

								// Actualizamos la fecha de inicio de la importacion
								updateImportArticlesStartTime(importId);
								
								// Devolvemos el control a flex
								if (batch == 0)
									toFlex.append("1");
			    			
				    			// Recorremos los artículos con un try catch para que aunque falle uno continue
				    			for (int a = 0; a < articlesIds.size() && continueDeleting(importId); a++){
				    				final Date t0 = Calendar.getInstance().getTime();	
				    				final String articleId = XMLHelper.getTextValueOf(articlesIds.get(a), "@articleid");
				    				
				    				try{
					    				final String importArtDetailId = XMLHelper.getTextValueOf(articlesIds.get(a), "@importdetailid");
					    				
					    				// Quitamos el artículo con todas sus dependencias
				    					JournalArticleImportLocalServiceUtil.deleteArticle(globalGroupId, articleId, true);
				    					
				    					if(_log.isDebugEnabled())
				    						_log.debug(new StringBuilder(XmlioKeys.PREFIX_ARTICLE_LOG).append(" article '").append(importArtDetailId).append("' deleted"));
				    					
				    					// Actualizamos su detalle: fecha inicio, fecha de fin, borramos su errordetail y errorcode = "DELETED"
				    					sql = String.format(UPDATE_DELETED_ARTICLE, sDFToDB.format(t0), importArtDetailId);				    					
				    					if(_log.isDebugEnabled())
				    						_log.debug(new StringBuilder("Query to update importartdetails after delete the article:\n").append(sql));				    					
				    					PortalLocalServiceUtil.executeUpdateQuery(sql);
				    					
				    					// Actualizamos el registro la importacion (importarticles) (ok = ok -1 y ko = ko + 1)
				    					sql = String.format(UPDATE_OK_AND_KO_IMPORTED, XmlioKeys.ARTICLES_TABLE_IMPORT,  "if(ok -1 < 0, 0, ok - 1) ", "ko + 1", "'" + importId + "'");				    					
				    					if(_log.isDebugEnabled())
				    						_log.debug("Query to update importarticles after delete the article:\n" + sql);				    					
				    					PortalLocalServiceUtil.executeUpdateQuery(sql);
				    					
				    					atLeastOneImportDetailDeleted = true;
				    				}catch(Exception e){	    					
				    					_log.error(XmlioKeys.PREFIX_ARTICLE_LOG + "Error deleting article: '" + articleId + "':", e);
				    					// Solo nos interesa saber si falla el primera
				    					if (a == 0){
				    						excepcionList.add(e);
				    					}
				    				}
				    			}
			    			}catch(Exception e){
			    				if(batch==0){
			    					excepcionList.add(e);
			    				}
			    			}finally{
			    				// Si se ha llegado a borrar un artículo de la importación actualizamos la fecha fin de la misma
			    				if (atLeastOneImportDetailDeleted)
			    					updateImportarticlesFinishTime(importId);
			    			}
		    			}
	            	}catch(Exception e){
	            		_log.error(e);
	            	}finally{            			
	            		try {
	            			// comprobamos que ninguna importacion/detalle se haya quedado sin fecha de inicio ni de fin
							restoreCanceledTimes(beforeTimes, importId, finalBatchsIds, finalDetailsIds);							
						}catch (Exception e){
							_log.error("Error restoring before times", e);
						}
	            	}
	            } 
	        };
	        
	        // Creamos el grupo de hilos si no existe
	        createArticlesThreadGroup();
						
			// Lanzamos en un hilo la tarea
			final Thread t = new Thread(articlesThreadGroup, r);
			t.setName(articlesThreadGroup.getName() + PortalUUIDUtil.newUUID());			
	        t.start();	
	        
	        final long fW = Calendar.getInstance().getTimeInMillis();
	        while(excepcionList.size() == 0 && toFlex.length() == 0 && (Calendar.getInstance().getTimeInMillis() - fW < ImportMgrLocalServiceImpl.MAX_WAITING_FLEX_TIME)){
	        	Thread.sleep(WAITING_TO_FLEX);
	        }
	        
	        if (excepcionList.size() > 0){
				throw excepcionList.get(0);
			}else{				
				// Obtener los datos del lote/detalles y devolverlos en un xml para que flex actualice las filas afectadas
				String sql = null;
				if(Validator.isNull(xmlArticlesDetailsIds)){
			        sql = String.format(GET_ARTICLES_IMPORTATIONS_LIST, groupId, new StringBuilder("  AND importid in (").append(lotesIds).append(") "), "id", "0", "200");		        
				}else{
					sql = String.format(GET_DETAILS_ARTICLES_IMPORTATIONS_LIST,  importId , new StringBuilder("  AND importdetailid in (").append(detallesIds).append(")"), "id", "0", "200");   
			        
				}
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("Get new state from the import:\n").append(sql));
				return PortalLocalServiceUtil.executeQueryAsDom(sql).asXML();
			}
		}		
		
		// Borra los artículos de los lotes indicados
		public String deleteArticlesFromBatchsList(String groupId, String xmlBatchsIds) throws Exception{			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId),      IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null"     );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlBatchsIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xmlBatchsIds is null");			
			return deleteArticlesFromBatchsAndDetailsList(groupId, null, xmlBatchsIds, null);			
		}
		
		// Borrado de artículos desde el detalle
		public String deleteArticlesFromDetailsList(String groupId, String importId, String xmlArticlesIds) throws Exception{					
			ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId),        IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null"     );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(importId),       IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "importId is null"    );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlArticlesIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xmlBatchsIds is null");			
			return deleteArticlesFromBatchsAndDetailsList(groupId, importId, null, xmlArticlesIds);
		}	
		
		// Busca los artículos de un lote
		private String getSqlArticles(String groupId, String importId, String xmlArticlesIds) throws DocumentException{			
			String sql = null;
			
			// Estamos eliminando desde el listado de lotes
			if (null == xmlArticlesIds){
				sql = String.format(GET_ARTICLES_FROM_BATCHS_AND_DETAILS, groupId, importId, "");
				
			// Estamos eliminando desde el detalle de articulos
			}else{
				List<Node> articlesIds = SAXReaderUtil.read(xmlArticlesIds).selectNodes("//row");
				
				String[] values = XMLHelper.getStringValues(articlesIds, "@id");
				
				StringBuilder auxArticlesIds = new StringBuilder();
				for (int n = 0; n < values.length; n++){
					auxArticlesIds.append("'" + values[n] + "'");
					if (n < values.length -1){
						auxArticlesIds.append(", ");
					}				
				}
				sql = String.format(GET_ARTICLES_FROM_BATCHS_AND_DETAILS, groupId, importId, new StringBuilder("\n  AND iad.importdetailid in(").append(auxArticlesIds).append(")"));
			}
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to get articles from batch:\n").append(sql));
			 
			return sql;
		}
		
		// Obtiene los tiempos (inicio y fin) de las importaciones por si hubiese que restaurarlos en la cancelación del borrado de lotes/artículos
		private HashMap<String, String[]> getBeforeTimes(String importId, String importsIds, String importDetailIds) throws SecurityException, NoSuchMethodException, ServiceError{
			_log.trace("In getBeforeTimes");
			
			HashMap<String, String[]> result = new HashMap<String, String[]>();
			
			// Importaciones
			StringBuilder sql = new StringBuilder(String.format(GET_BEFORE_IMPORTS_TIMES, Validator.isNull(importsIds) ? "'" + importId + "'" : importsIds));
			
			// Detalles	
			if(Validator.isNotNull(importDetailIds)){				
				sql.append("\n  UNION \n ").append(String.format(GET_BEFORE_DETAILS_IMPORT_TIMES, importDetailIds));	
			}
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to get before times:\n").append(sql));
			final Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql.toString());
			final List<Node> rows = doc.getRootElement().selectNodes("//row");			
			ErrorRaiser.throwIfFalse(null != rows && rows.size() > 0, com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Error getting before times");
			
			final int size = rows.size();
			for (int i = 0; i < size; i++){
				final Node row = rows.get(i);		
				final String id         = XMLHelper.getTextValueOf(row, "@id");
				final String startTime  = XMLHelper.getTextValueOf(row, "@starttime",  "");
				final String finishTime = XMLHelper.getTextValueOf(row, "@finishtime", "");				
				final String dates[]    = {startTime, finishTime};
				result.put(id, dates);
			}						
			return result;
		}
		
		// Restaura los tiempos de los lotes/detalles que se han quedado sin fecha de inicio y de fin (porque se han cancelado)
		private void restoreCanceledTimes(HashMap<String, String[]>beforeTimes, String importId, String batchsIds, String detailsIds) throws SecurityException, NoSuchMethodException, IOException, SQLException{
			_log.trace("In restoreCanceledTimes");
			
			// Lotes
			try{
				String sql = String.format(GET_IMPORTS_CANCELED, (Validator.isNull(batchsIds) ?  ("'" + importId + "'") : batchsIds));
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder("Query to get imports canceled:\n").append(sql));
				final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);			
				final List<Node> nodes = result.getRootElement().selectNodes("//row");
				
				if(null != nodes && nodes.size() > 0){
					final int size = nodes.size();
					
					for (int i = 0; i < size; i++){
						// id de la importación del lote
						final String id = XMLHelper.getTextValueOf(nodes.get(i), "@id");
						// Obtenemos los tiempos anteriores a su cancelacion para el lote
						final String[] times = beforeTimes.get(id);
						
						sql = String.format(UPDATE_STARTTIME_AND_FINISHTIME_IMPORTARTICLE, times[0], times[1], id);
						if(_log.isDebugEnabled())
							_log.debug(new StringBuilder("Query to restore import times:\n").append(sql));					
						PortalLocalServiceUtil.executeUpdateQuery(sql);	
					}
				}else{
					_log.debug("No import times to be restored");
				}
			}catch(Exception e){
				_log.error("Error restoring batchs imports times", e);
			}
			
			// Detalles
			try{
				String sql = String.format(GET_IMPORTSDETAIL_CANCELED, detailsIds);
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder("Query to get imports details canceled:\n").append(sql));
				final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);			
				final List<Node> nodes = result.getRootElement().selectNodes("//row");
				
				if(null != nodes && nodes.size() > 0){
					final int size = nodes.size();
					
					for (int i = 0; i < size; i++){
						// id de la importación del detalle
						final String id = XMLHelper.getTextValueOf(nodes.get(i), "@id");
						// Obtenemos los tiempos anteriores a su cancelacion para el detalle
						final String[] times = beforeTimes.get(id);
						
						sql = String.format(UPDATE_STARTTIME_AND_FINISHTIME_IMPORTARTICLEDETAIL, times[0], times[1], id);
						if(_log.isDebugEnabled())
							_log.debug(new StringBuilder("Query to restore import details times:\n").append(sql));					
						PortalLocalServiceUtil.executeUpdateQuery(sql);	
					}
				}else{
					_log.debug("No import details times to be restored");
				}
			}catch(Exception e){
				_log.error("Error restoring batchs imports details times", e);
			}				
		}		
		
		public boolean continueDeleting(String importId){
			boolean result = true;			
			if (Validator.isNotNull(articleImportationsToCancel) && articleImportationsToCancel.contains(importId)){
				result = false;	
				removeArticleImportationToCancel(importId); 
			}
			return result;
		}
		
		// Obtiene el porcentaje de borrado de artículos desde el borrado avanzado (ctrl+q) de flex
		public String getDeletedPercentage() throws DocumentException, ServiceError{
			Document doc = SAXReaderUtil.read("<result/>");
			doc.getRootElement().addAttribute("percentage", Long.toString(Math.round(deletedPercentage)));
			return doc.asXML();
		}
	// IMPORTACIÓN DE ARTÍCULOS

// IMPORTACION DE URLS DEL PARALELO
	@SuppressWarnings("unchecked")
	public String importLegacyUrls(HttpServletRequest request, HttpServletResponse response, InputStream is, String xmlData ) throws Exception
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX );
		Document dom = SAXReaderUtil.read(xmlData);
		Node row = dom.selectSingleNode("/rs/row");
		ErrorRaiser.throwIfNull(row, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long scopeGrpId = XMLHelper.getLongValueOf(row, "@groupid", 0L);
		ErrorRaiser.throwIfFalse(scopeGrpId>0L, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		GroupLocalServiceUtil.getGroup(scopeGrpId);
		boolean encodeLegacyUrl = GetterUtil.getBoolean( XMLHelper.getStringValueOf(row, "@encodelegacyurl"), false);
		String exists = XMLHelper.getStringValueOf(row, "@imported", StringPool.BLANK);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(exists) , IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Atributte 'imported' is empty or null" );
		
		 ImportLegacyurlTool ilt = new ImportLegacyurlTool(scopeGrpId, encodeLegacyUrl, exists);
		 ilt.deleteOldErrors();

		Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
		while (files.hasNext())
		{
	    	FileItem currentFile = files.next();
	    	if (!currentFile.isFormField())
	    	{
	    		InputStream fis = currentFile.getInputStream();
	    		BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
    	        String line = "";
    	        int rowCount = 1;
    	        while ((line = reader.readLine()) != null)
    	        {
	    			ilt.setLine(line, rowCount++);
    	        	ilt.importLegacyUrl();
    	        }
    	        reader.close();
	    		break;
	    	}
		}
		
		return getLegacyErrors( String.valueOf(scopeGrpId) );
	}
	
	public String getLegacyErrors(String scopeGroupId) throws NoSuchMethodException, SecurityException
	{
		String retVal = StringPool.BLANK;
		
		String query = String.format("SELECT * FROM legacyurlerrors WHERE groupid=%s", scopeGroupId);
		retVal = PortalLocalServiceUtil.executeQueryAsDom(query, new String[]{"errordetail"}).asXML();
		
		return retVal;
	}

}