package com.protecmedia.iter.news.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.protecmedia.iter.news.service.base.DaylyTopicMgrServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DaylyTopicMgrServiceImpl extends DaylyTopicMgrServiceBaseImpl {
	
	public void setDaylyTopics(String xml) throws Exception{
				
		try{	
			daylyTopicMgrLocalService.setDaylyTopics(xml);			
		}catch(Throwable th){
           throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
	}	
}