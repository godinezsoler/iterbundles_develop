package com.protecmedia.iter.xmlio.service.impl;

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.xml.Node;
import com.protecmedia.iter.xmlio.service.base.DaylyTopicsPublicationServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class DaylyTopicsPublicationServiceImpl extends DaylyTopicsPublicationServiceBaseImpl {
	
	public synchronized void publish(String scopeGroupId, List<Node> section, Object[] dataToJson) throws Exception
	{
		try
		{
			daylyTopicsPublicationLocalService.publish(scopeGroupId, section, dataToJson);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
	}
	
	// Para el Live
	public void publishInLive(String groupId, String daylyTopicsToBePublished, String daylyTopicsIdsToBeRemoved) throws Exception
	{
		try
		{
			daylyTopicsPublicationLocalService.publishInLive(groupId, daylyTopicsToBePublished, daylyTopicsIdsToBeRemoved);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}		
	}
}