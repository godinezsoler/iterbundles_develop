package com.protecmedia.iter.news.service.impl;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.news.service.base.MetadataControlServiceBaseImpl;

// Esta clase escucha las peticiones del flex para obtener y cambiar la configuración de los metatags (artículos)
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class MetadataControlServiceImpl extends MetadataControlServiceBaseImpl {

	private static Log _log = LogFactoryUtil.getLog(MetadataControlServiceImpl.class);
	
	public String getConfig(String groupId) throws Exception
	{
		String result = null;
		try
		{			
			result = metadataControlLocalService.getConfig(groupId);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	public void setConfig(String groupId, String xmlFlex) throws Exception
	{
		try
		{			
			 metadataControlLocalService.setConfig(groupId, xmlFlex);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public void importData(String data) throws SystemException
    {
          try
          {
        	  metadataControlLocalService.importData(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void publish(long groupId) throws SystemException
    {
          try
          {
        	  metadataControlLocalService.publish(groupId);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public String uploadDefaultOgImage(HttpServletRequest request, HttpServletResponse response, InputStream is, String groupId ) throws SystemException
	{
		String retVal = "";
		try
		{
			retVal = metadataControlLocalService.uploadDefaultOgImage(request, response, is, groupId);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public void importPublishedData(String fileName) throws Exception
	{
		try
        {
			metadataControlLocalService.importPublishedData(fileName);
        }
        catch (Throwable th)
        {
        	_log.error(th);
            throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
        }
	}
}