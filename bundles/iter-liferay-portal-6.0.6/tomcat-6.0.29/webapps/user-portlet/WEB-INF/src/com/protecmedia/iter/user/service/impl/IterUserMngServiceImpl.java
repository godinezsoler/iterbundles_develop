package com.protecmedia.iter.user.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.user.service.base.IterUserMngServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class IterUserMngServiceImpl extends IterUserMngServiceBaseImpl {	
	
	// Devuelve un xml con la cabecera e ids de los usuarios segun los filtros y ordenacion
	public String getUsersId(String groupId, String xmlQueryFiltersAndOrders) throws UnsupportedEncodingException, ServiceError, SecurityException, NoSuchMethodException, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException
	{
		String result = "";
		
		try
		{	
			result = iterUserMngLocalService.getUsersId(groupId, xmlQueryFiltersAndOrders);			
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}		
		return result;
	}	
	
	// Devuelve un xml con la cabecera y todos los datos de los usuarios solicitados
	public String getGridValues(String xmlGroupIdAndUsersIds) throws UnsupportedEncodingException, ServiceError, SecurityException, NoSuchMethodException, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException
	{
		String result = "";
		
		try
		{	
			result = iterUserMngLocalService.getGridValues(xmlGroupIdAndUsersIds);			
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}		
		return result;
	}
			
	// Exporta los datos seleccionados a un csv 
	public void exportUserMngToXls(HttpServletRequest request, HttpServletResponse response, String groupId, String xmlQueryFiltersAndOrders, String translatedColumns) throws Throwable 
	{		
		try
		{
			iterUserMngLocalService.exportUserMngToXls(request, response, groupId, xmlQueryFiltersAndOrders, translatedColumns);	
		}
		catch (Throwable th)
		{
			if (th instanceof ORMException)
				ServiceErrorUtil.throwSQLIterException( (ORMException)th );
			else
				throw th;
		}		
	}
	
	// Borra los usuarios solicitados
	public String deleteUsers(String xmlData) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException{
		String result = "";
		
		try{			
			result = iterUserMngLocalService.deleteUsers(xmlData);
		}catch(ORMException orme){
			ServiceErrorUtil.throwSQLIterException(orme);
		}		
		return result;
	}	
	
	// Monta una pagina html con todos los datos de un usuario
	public void getUserDetailById(HttpServletRequest request, HttpServletResponse response, String groupId, String userId, String formId, String translatedColumns) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, TransformerException, IOException, NumberFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException{		
		try{ 
			iterUserMngLocalService.getUserDetailById(request, response, groupId, userId, formId, translatedColumns);
		}catch(ORMException orme){
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public String getUserInfo(long groupId, String userId) throws ServiceError, SecurityException, NoSuchMethodException, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException{
		String result = "";
		
		try	{			
			result = iterUserMngLocalService.getUserInfo( groupId, userId );
		}catch(ORMException orme){
			ServiceErrorUtil.throwSQLIterException(orme);
		}		
		return result;
	}
	
	public String getUserInfo(String userId) throws ServiceError, SecurityException, NoSuchMethodException, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException{
		String result = "";
		
		try	{			
			result = iterUserMngLocalService.getUserInfo( userId );
		}catch(ORMException orme){
			ServiceErrorUtil.throwSQLIterException(orme);
		}		
		return result;
	}
	
	public String updateUserInfo(String xmlData) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, IOException, SQLException, NoSuchAlgorithmException{
		String result = "";
		
		try	{			
			result = iterUserMngLocalService.updateUserInfo( xmlData );
		}catch(ORMException orme){
			ServiceErrorUtil.throwSQLIterException(orme);
		}		
		return result;
	}

	/* Importa un usuario.
	 Paso los parámetros como objects porque se hace un lío la llamada dinámica y no encuentra la función */
	@SuppressWarnings("unchecked")
	public void importUser(	Object xmlResult, Object userprofiles, Object url, Object user, Object groupId, 
							Object defaultUserId, File workingDirectory, boolean passwordInMD5, boolean deleteUsers, 
							Object userProfileNames) throws Exception 
    {
		try	
		{			
			iterUserMngLocalService.importUser((Document)xmlResult, (List<Object>)userprofiles, (String)url, (Node)user, (String)groupId, 
					                           (Long)defaultUserId, workingDirectory, passwordInMD5, deleteUsers, (List<String>)userProfileNames);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public String GetPasswordSuperUser(String user) throws Exception {
		Document result = null;
		try
		{			
			result = iterUserMngLocalService.GetPasswordSuperUser(user);
		}catch (Throwable th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result.asXML();
	}
	
	public String SetPasswordSuperUser(String superuser, String user, String test, Boolean changePss) throws Exception {
		String result = "";
		try	{			
			result = iterUserMngLocalService.SetPasswordSuperUser(superuser, user, test, changePss);
		}catch (Throwable th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String GetEncodedPass(String test) throws Exception{
		String result = "";
		try	{			
			result = iterUserMngLocalService.GetEncodedPass(test);

		}catch (Throwable th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void subscribeUsersToNewsletters(long groupId, String scheduleIds, String xmlQueryFilters) throws SystemException
	{
		try
		{			
			iterUserMngLocalService.subscribeUsersToNewsletters(groupId, scheduleIds, xmlQueryFilters);

		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}