/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.util.List;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;
import com.protecmedia.iter.news.model.impl.CountersImpl;

public class CountersDynamicQuery extends BasePersistenceImpl {
	
	public CountersDynamicQuery() {
		setSessionFactory((SessionFactory)PortalBeanLocatorUtil.getBeanLocator().locate(CountersImpl.SESSION_FACTORY));
	}
	
	public List<Object[]> executeQuery(String sql) {
		Session session = null;
		List<Object[]> listResult = null;
        try {
        	//open a new session                                                                                                                                                         
        	session = openSession();
 
        	//create a SQLQuery object                                                                                                                                                   
            Query sqlQuery = session.createSQLQuery(sql);
                        
            listResult = sqlQuery.list();    		
        } finally {
            //must have this to close the session.. if you fail to do this.. you will have a lot of open session..                                                             
            closeSession(session);
        }
		        
		return listResult;

	}

}
