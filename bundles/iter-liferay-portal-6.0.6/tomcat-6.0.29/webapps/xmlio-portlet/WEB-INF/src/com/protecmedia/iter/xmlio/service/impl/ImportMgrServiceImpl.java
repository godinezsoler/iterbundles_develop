package com.protecmedia.iter.xmlio.service.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.xmlio.service.base.ImportMgrServiceBaseImpl;

//Esta clase gestiona las tablas importation e importationdetails y da algunos métodos útiles/comunes a las importaciones
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ImportMgrServiceImpl extends ImportMgrServiceBaseImpl
{
	
	// IMPORTACIÓN DE USUARIOS			
		// Obtiene los importations solicitados, filtra, ordena y limita. No necesita los ids de los importations
		public String getImportsList(String groupId, String type, String xmlFilters, String startIn, String limit, String sort) 
						throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
		{
			String result = null;
			try
			{			
				result = importMgrLocalService.getImportsList(groupId, type, xmlFilters, startIn, limit, sort);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		} 
		
		/* Borra importations y sus importationdetails por cascade. Recibe un xml como este:	
		<rs>
			<row id=""/>	 	
			...
	 	</rs> */
		public String deleteImports(String importType, String xmlWithIds) throws ServiceError, IOException, SQLException, DocumentException
		{
			String result = null;
			try
			{			
				result = importMgrLocalService.deleteImports(importType, xmlWithIds);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		/* Borra importationdetails. Al hacerlo, hay que actualizar la cuenta de las importaciones correctas e incorrectas de importation. 
		   Si fallase el conteo de las importaciones correctas/incorrectas se hace rollback del borrado de importationdetails para no descuadrar las cuentas
		   Recibe un xml como este:
		    <rs>
				<row id=""/>	 	
				...
		 	</rs> */
		public String deleteImportDetails(String importType, String xmlWithIds) throws ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException, DocumentException{
			String result = null;
			try
			{			
				result = importMgrLocalService.deleteImportDetails(importType, xmlWithIds);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		// Listado de detalles de importacion de usuarios
		public String getDetailsUsersImportsList(String importId, String xmlFilters, String startIn, String limit, String sort) 
						throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
			String result = null;
			try
			{			
				result = importMgrLocalService.getDetailsUsersImportsList(importId, xmlFilters, startIn, limit, sort);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		// Importa usuarios
		public String importUsers(String path, String pathIsFile, String backupPath, String groupId, String passwordInMD5, String deleteUsers) throws Exception
        {
			String result = null;
			try
			{			
				result = importMgrLocalService.importUsers(path, pathIsFile, backupPath, groupId, passwordInMD5, deleteUsers);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}

		// Para una importacion de usuarios
		public void stopUserImport(String importId, String host) throws ServiceError, ClientProtocolException, IOException, JSONException, SystemException{
			try
			{			
				importMgrLocalService.stopUserImport(importId, host);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
		}		
	// IMPORTACIÓN DE USUARIOS
	
	// IMPORTACION DE ARTÍCULOS	
		// Listado de importaciones de artículos
		public String getArticlesImportsList(String groupId, String xmlFilters, String startIn, String limit, String sort) 
					throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
			String result = null;
			try
			{			
				result = importMgrLocalService.getArticlesImportsList(groupId, xmlFilters, startIn, limit, sort);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		// Listado de detalles de importacion de artículos
		public String getDetailsArticlesImportsList(String importId, String xmlFilters, String startIn, String limit, String sort) 
						throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
			String result = null;
			try
			{			
				result = importMgrLocalService.getDetailsArticlesImportsList(importId, xmlFilters, startIn, limit, sort);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}	
		
		/*
		deleteArticles						Indica si la operación es de importación o de borrado
		ifArticleExists 					Si existe ya el artículo: 
												0: borrarlo
												1: fallar
		ifLayoutNotExists 					Si no existe el layout:
												0: Crearlo
												1: Fallar
		importationStart, importationFinish	Intervalo de fechas en las que se deben encontrar la fecha de creación y modificación del artículo. Si no se cumple, error.
		ifNoMetadata						Si no hay metadata:
												c: Continuar
												a: Avisar
												f: Fallar
		ifNoSuscription 					Si no hay suscripciones: continuar/avisar/fallar 
												c: Continuar
												a: Avisar
												f: Fallar */
		public String importArticles(String path, String pathIsFile, String backupPath, String groupId, String deleteArticles,
			                         String ifArticleExists, String ifLayoutNotExists, String startDate, String finishDate,
			                         String ifNoCategory, String ifNoSuscription, String legacyIsEncoded) throws Exception{
			String result = null;
			try
			{			
				result = importMgrLocalService.importArticles(path, pathIsFile, backupPath, groupId, deleteArticles,
                    										  ifArticleExists, ifLayoutNotExists, startDate, finishDate,
                    										  ifNoCategory, ifNoSuscription, legacyIsEncoded);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		// Para una importacion de usuarios
		public void stopArticleImport(String xml) throws ServiceError, ClientProtocolException, IOException, JSONException, SystemException, DocumentException{
			try
			{			
				importMgrLocalService.stopArticleImport(xml);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
		}
		
		// Borra los artículos encontrados en la consulta dada. 
		synchronized public String selectToDeleteArticles(String groupId, String sql, String delete) throws ServiceError, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException, IllegalArgumentException, IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, SQLException, DocumentException{
			String result = null;
			try
			{			
				result = importMgrLocalService.selectToDeleteArticles(groupId, sql, GetterUtil.getBoolean(delete));
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}		
		
		public String deleteArticlesFromBatchsList(String groupId, String xmlBatchsIds) throws Exception{
			String result = null;
			try
			{			
				result = importMgrLocalService.deleteArticlesFromBatchsList(groupId, xmlBatchsIds);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		public String deleteArticlesFromDetailsList(String groupId, String importId, String xmlArticlesIds) throws Exception{
			String result = null;
			try
			{			
				result = importMgrLocalService.deleteArticlesFromDetailsList(groupId, importId, xmlArticlesIds);
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
		
		public String getDeletedPercentage() throws DocumentException, ServiceError{
			String result = null ;
			try
			{			
				result = importMgrLocalService.getDeletedPercentage();
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			return result;
		}
	// IMPORTACION DE ARTÍCULOS	
}