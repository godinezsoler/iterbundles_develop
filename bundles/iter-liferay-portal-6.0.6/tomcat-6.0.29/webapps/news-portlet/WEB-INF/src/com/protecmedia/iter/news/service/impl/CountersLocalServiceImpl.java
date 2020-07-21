/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.model.Counters;
import com.protecmedia.iter.news.service.base.CountersLocalServiceBaseImpl;


/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CountersLocalServiceImpl extends CountersLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(CountersLocalServiceImpl.class);

	private static final String INCREMENT_RATINGS = "INSERT INTO News_Counters(id_, contentId, groupId, counter, value, counterLast, operation, date_) " + 
													"VALUES (%s, '%s', %s, 1, %s, 0, " + IterKeys.OPERATION_RATINGS + ", NOW()) " +
													"ON DUPLICATE KEY UPDATE counter=counter + 1, value=value + %s";
	
	private static final String GET_RATINGS = "SELECT counter, value FROM News_Counters WHERE contentId='%s' AND groupId=%s AND operation=" + IterKeys.OPERATION_RATINGS;
	
	private static final String GET_USERS_FEEDBACK = "SELECT optionid, votes FROM feedbackvotes WHERE articleid='%s' AND groupId=%s";
		
	public List<Counters> getAllCounters() throws SystemException {
		return countersPersistence.findAll();
	}
	
	public List<Counters> findByCountersArticleGroupContentFinder(String contentId, long groupId) throws SystemException, PortalException {
		return countersPersistence.findByGroupContentIdFinder(groupId, contentId);	
	}
	
	public Counters findByCountersArticleGroupOperationFinder(String contentId, long groupId, int opt) throws SystemException, PortalException {
		return countersPersistence.fetchByContentGroupFinder(contentId, groupId, opt);		
	}
	
	public List<JournalArticle> findByGroupOperationFinder(long groupId, int opt, int start, int end) 
			throws SystemException, PortalException
	{
		List<Counters> listJV = countersPersistence.findByGroupFinder(groupId, opt, start, end);
		
		List<JournalArticle> listJA = new ArrayList<JournalArticle>();
		for (int i = 0; i < listJV.size(); i++)
		{
			Counters result = listJV.get(i);			
			try 
			{
				listJA.add(JournalArticleLocalServiceUtil.getArticle(groupId, result.getContentId()));
			} catch (Exception e) {}
		}
		
		return listJA;
	}
	
	public long incrementCounter(HttpServletRequest request, String articleId, long groupId, int opt)
	{
		long count = 0;

		try
		{
			if(opt == IterKeys.OPERATION_RATINGS)
			{
				Counters counter = countersPersistence.fetchByContentGroupFinder(articleId, groupId, opt);
				if (counter != null) 
				{
					count = counter.getCounter() +1;
					counter.setCounter(count);
					counter.setDate(new Date());
					counter = countersPersistence.update(counter, false);
				} 
				else 
				{			
					count++;
					long id = counterLocalService.increment();
					Counters c = countersPersistence.create(id);
					c.setContentId(articleId);
					c.setGroupId(groupId);
					c.setCounter(count);
					c.setCounterLast(0);
					c.setDate(new Date());
					c.setOperation(opt);
					counter = countersPersistence.update(c, false);
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Unable to get visits (Passive task)");
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return count;
	}
	
	public long incrementValue(HttpServletRequest request, String articleId, long groupId, int opt, long value) throws SystemException, PortalException {
		
		long val = 0;
		
		try
		{
			Counters counter = countersPersistence.fetchByContentGroupFinder(articleId, groupId, opt);
			if (counter != null) 
			{	
				val = counter.getValue() + value;
				counter.setValue(val); 
				counter = countersPersistence.update(counter, false);
			}
			else
			{			
				val+=value;
				
				// Creamos el contador
				long id = counterLocalService.increment();
				Counters c = countersPersistence.create(id);
				c.setContentId(articleId);
				c.setGroupId(groupId);
				c.setCounter(val);
				c.setCounterLast(0);
				c.setDate(new Date());
				c.setOperation(opt);
				c.setValue(value);
				
				counter = countersPersistence.update(c, false);
			}	
		}
		catch(Exception e)
		{
			_log.debug("Cannot increment counter for " + articleId);
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return val;
	}

	public void deleteCounters(long groupId, String articleId) throws SystemException 
	{
		countersPersistence.removeByGroupContentIdFinder(groupId, articleId);
	}
	
	public long[] incrementRating(long groupId, String articleId, long currentRating)
	{
		long[] rating = {0, 0};
		try
		{
			String query = String.format(INCREMENT_RATINGS, counterLocalService.increment(), StringEscapeUtils.escapeSql(articleId), groupId, currentRating, currentRating);
			PortalLocalServiceUtil.executeUpdateQuery(query);
			rating = getRating(groupId, articleId);
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return rating;
	}
	
	public long[] getRating(long groupId, String articleId)
	{
		long[] rating = {0, 0};
		List<Object> results = PortalLocalServiceUtil.executeQueryAsList(String.format(GET_RATINGS, articleId, groupId));
		if(results != null && results.size() > 0)
		{
			Object resultsArray[] = (Object[])results.get(0);
			if(resultsArray != null)
			{
				rating[0] = Long.valueOf(resultsArray[0].toString());
				rating[1] = Long.valueOf(resultsArray[1].toString());
			}
		}
		return rating;
	}
	
	/************************************************************************************************************************************************/
	/*****													user feedback methods																*****/
	/************************************************************************************************************************************************/
	
	public JSONObject getUsersFeedback(String scopeGroupId, String articleId)
	{
		String query = String.format(GET_USERS_FEEDBACK, articleId, scopeGroupId);
		
		if(_log.isDebugEnabled())
			_log.debug("GET_USERS_FEEDBACK: " + query);
		
		List<Map<String,Object>> result = PortalLocalServiceUtil.executeQueryAsMap(query);
		return getOptionResults(result);
	}
	
	public JSONObject setUserFeedback(String scopeGroupId, String articleId, String userId, String optionId) throws IOException, SQLException, ServiceError
	{
		String query = String.format("CALL ITR_SET_USER_FEEDBACK(%s, '%s', '%s', '%s', '%s')", scopeGroupId, articleId, userId, optionId, SQLQueries.getCurrentDate());
		
		if(_log.isDebugEnabled())
			_log.debug("ITR_SET_USER_FEEDBACK: " + query);
		
		List<Map<String,Object>> result = PortalLocalServiceUtil.executeQueryAsMap(query);
		
		return getOptionResults(result);
	}
	
	private JSONObject getOptionResults(List<Map<String,Object>> data)
	{
		JSONObject retVal = null;
		
		if(data!=null && data.size()>0)
		{
			retVal = JSONFactoryUtil.createJSONObject();
			long counterTotal = 0;
			for(Map<String,Object> m : data)
				counterTotal += Long.valueOf( String.valueOf(m.get("votes")) ); 
			
			List<Integer> percentages = new ArrayList<Integer>();
			int totalPercent = 0;	// Acumula los porcentajes asignados para comprobar que suman 100%
			int maxPercent = 0;		// Guarda en mayor porcentaje hasta el momento para saber a qué elemento pertenece
			int maxPercentItem = 0;	// Guarda el elemento con el mayor porcentaje para sumarle la diferencia en caso de no llegar al 100%
			for (int i=0; i<data.size(); i++)
			{
				// Calcula el porcentaje
				Map<String,Object> m = data.get(i);
				float optionVotes = Float.valueOf(m.get("votes").toString());
				int percentage = Math.round(optionVotes / counterTotal * 100);
				percentages.add(percentage);
				// Suma el porcentaje asignado al total
				totalPercent += percentage;
				// Si es el mayor porcentaje hasta el momento, guarda el elemento
				if (percentage > maxPercent)
				{
					maxPercent = percentage;
					maxPercentItem = i;
				}
			}
			
			// Si no se llega al 100% sumando todo, añade la diferencia al más votado
			int difference = 0;
			if ( (difference = 100 - totalPercent) > 0 )
			{
				percentages.set(maxPercentItem, percentages.get(maxPercentItem) + difference);
			}
			
			// Crea la respuesta JSON
			JSONArray options = JSONFactoryUtil.createJSONArray();
			for (int i=0; i<data.size(); i++)
			{
				Map<String,Object> m = data.get(i);
				JSONObject option = JSONFactoryUtil.createJSONObject();
				option.put("optionid", String.valueOf(m.get("optionid")));
				option.put("votes", percentages.get(i) );
				options.put(option);
			}
			
			retVal.put("options", options);
			
			Map<String,Object> m = data.get(0);
			String vigencia = String.valueOf(m.get("vigencia"));
			retVal.put("vigencia", vigencia);
		}
		
		return retVal;
	}
}
