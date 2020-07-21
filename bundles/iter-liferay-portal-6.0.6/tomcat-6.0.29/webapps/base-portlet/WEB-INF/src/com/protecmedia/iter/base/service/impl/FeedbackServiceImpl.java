/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.base.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.base.FeedbackServiceBaseImpl;

/**
 * The implementation of the feedback remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.FeedbackService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.FeedbackServiceUtil} to access the feedback remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.FeedbackServiceBaseImpl
 * @see com.protecmedia.iter.base.service.FeedbackServiceUtil
 */

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FeedbackServiceImpl extends FeedbackServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(FeedbackServiceImpl.class);
	
	public String getFeedbackConf( long groupId ) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.getFeedbackConf( groupId).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getFeedbackDisplayConf( long groupid, String questionid ) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.getFeedbackDisplayConf( groupid, questionid).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addOrUpdtFeedbackDisplay(String xmlData, long groupid, String questionid) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.addOrUpdtFeedbackDisplay( xmlData, groupid, questionid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String setQuestion( String xmlData ) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.setQuestion( xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addChoice( String xmlData ) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.addChoice( xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String updateChoice( String xmlData ) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.updateChoice( xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String updateChoiceOrder( String xmlData ) throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.updateChoiceOrder( xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String deleteChoices( String xmlData )throws SystemException
	{
		String result= "";
		try
		{
			result = feedbackLocalService.deleteChoices( xmlData );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void publishFeedbackConf(String groupId) throws SystemException
	{
		try
		{
			feedbackLocalService.publishFeedbackConf(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importFeedbackConf(String xmlData) throws SystemException
	{
		try
		{
			feedbackLocalService.importFeedbackConf(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	
}