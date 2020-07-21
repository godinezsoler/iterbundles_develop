/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;
import com.protecmedia.iter.news.model.impl.PageContentImpl;


public class PageContentDynamicQuery extends BasePersistenceImpl {
	
	public PageContentDynamicQuery() {
		setSessionFactory((SessionFactory)PortalBeanLocatorUtil.getBeanLocator().locate(PageContentImpl.SESSION_FACTORY));
	}
	
	public List< Map<String, Object> > executeQueryEx(final String sql)
	{
		List< Map<String, Object> > result = null;
		Session session = null;
		
		try
		{
			session = openSession();
			result = session.doWork(sql);
		}
        finally 
        {
            //must have this to close the session.. if you fail to do this.. you will have alot of open session..                                                             
            closeSession(session);
        }
		return result;
	}
	
	public List<Object> executeQuery(String sql) 
	{
		Session session = null;
		List<Object> listResult = null;
        try 
        {
        	//open a new session                                                                                                                                                         
        	session = openSession();
 
        	//create a SQLQuery object                                                                                                                                                   
            Query sqlQuery = session.createSQLQuery(sql);
                        
            listResult = sqlQuery.list();    		
        } 
        finally 
        {
            //must have this to close the session.. if you fail to do this.. you will have alot of open session..                                                             
            closeSession(session);
        }
		        
		return listResult;

	}
}
