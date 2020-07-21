/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdate;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdateFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.service.persistence.BasePersistence;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;
import com.protecmedia.iter.xmlio.model.impl.LiveImpl;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;


@SuppressWarnings({ "rawtypes"})
public class XmlioLiveDynamicQuery extends BasePersistenceImpl {
	
	public XmlioLiveDynamicQuery() 
	{
		setSessionFactory((SessionFactory)PortalBeanLocatorUtil.getBeanLocator().locate(LiveImpl.SESSION_FACTORY));
	}
	
	/**
	 * Ejecuta sentencias de tipo SELECT
	 * 
	 * @param sql
	 * @return Devuelve la lista de elementos como resultado de la consulta SQL
	 */
	@SuppressWarnings("unchecked")
	public List<Object> executeSelectQuery(String sql) 
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
	
	/**
	 * Ejecuta sentencias de tipo INSERT, DELETE, UPDATE. Cualquier sentencia que modifique los datos de la BBDD
	 * @param sql
	 */
	static public void executeUpdateQuery1(String sql)  throws IOException, SQLException
	{
		boolean cacheRegistryActive = CacheRegistryUtil.isActive();
		
		try 
		{
			CacheRegistryUtil.setActive(false);
			
			DB db = DBFactoryUtil.getDB();
			db.runSQL(sql);
		}
		finally 
		{
			CacheRegistryUtil.setActive(cacheRegistryActive);
		}
	}
	
	public void executeUpdateQuery2(String sql)  throws SystemException
	{
		try 
		{
			DataSource dataSource = getDataSource();
			SqlUpdate sqlUpdate	  = SqlUpdateFactoryUtil.getSqlUpdate(dataSource, sql, new int[0]);
			sqlUpdate.update();
		}
		catch (Exception e) 
		{
			throw new SystemException(e);
		}
	}
	
	public void executeUpdateQuery3(BasePersistence<?> persistence, String sql)  throws SystemException
	{
		try 
		{
			DataSource dataSource = persistence.getDataSource();
			SqlUpdate sqlUpdate	  = SqlUpdateFactoryUtil.getSqlUpdate(dataSource, sql, new int[0]);
			sqlUpdate.update();
		}
		catch (Exception e) 
		{
			throw new SystemException(e);
		}
	}
	
	static public void executeUpdateQuery4(String sql) throws Exception 
	{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try 
		{
			con = DataAccess.getConnection();

			ps = con.prepareStatement(sql);

			ps.executeUpdate();
			ps.executeUpdate();
		}
		finally 
		{
			DataAccess.cleanUp(con, ps, rs);
		}
	}
	
	public static String getInClauseSQL(String[] ids)
	{
		StringBuffer query = new StringBuffer();
		if(ids != null)
		{
			for(int i = 0; i < ids.length; i++)
			{
				String currentId = ids[i];
	
				if(i == 0)
				{
					query.append("('" + currentId + "'");
				}				
				if(i == ids.length - 1)
				{
					if(ids.length > 1)
					{
						query.append(", '" + currentId + "') ");
					}
					else
					{
						query.append(") ");
					}
				}
				if (i > 0 && i < ids.length - 1)
				{
					query.append(", '" + currentId + "'");
				}
			}
		}
		return query.toString();
	}
}