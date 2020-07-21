/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletURL;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class TrackingUtil {
	private static Log _log = LogFactoryUtil.getLog(TrackingUtil.class);
	
	public static int getUserAccess(long userId)
	{
		return IterLocalServiceUtil.getUserAccess(userId);
	}
	
	//0 numComment
	//1 numUnmoderated
	//2 title
	//3 typeContent
	//4 visits
	//5 contentId
	//6 votes
	//7 rank
	//8 sends
	
	public static List<TrackingSearchObject> getTrackingsFromObjects(List<Object> listArticles)
	{
		List<TrackingSearchObject> listTrackings = new ArrayList<TrackingSearchObject>();
		if(listArticles != null && listArticles.size() > 0)
		{
			for (Object currentArticle : listArticles) 
			{
				
				try{
					Object[] currentData = (Object[])currentArticle;
					if(currentData != null)
					{
						long views = 0;
						if (currentData[4] != null) 
						{
							views = Long.parseLong(currentData[4].toString());
						}
							
						if(views > 0)
						{
							String articleId = null;
							if(currentData[5] != null)
							{
								articleId = currentData[5].toString();
							}
							
							int numComment = 0;
							if(currentData[0] != null)
							{
								numComment = Integer.parseInt(currentData[0].toString());
							}
							
							boolean moderated = true;
							if(currentData[1] != null)
							{
								int numUnmoderated = Integer.parseInt(currentData[1].toString());
								
								if(numUnmoderated > 0)
								{
									moderated = false;
								}
							}
								
							long votings = 0;
							String currentType = IterKeys.STRUCTURE_ARTICLE;
							if(currentData[3] != null)
							{
								currentType = currentData[3].toString();
								if (currentType.equals(IterKeys.STRUCTURE_POLL)) 
								{
										
									if(currentData[6] != null)
									{
										votings =  Long.parseLong(currentData[6].toString());
									}
								}
							}
							
							Double currentAvgScore = 0.0;
							
							if(currentData[7] != null)
							{
								currentAvgScore = Double.parseDouble(currentData[7].toString());
							}
							
							String title = null;
							if(currentData[2] != null)
							{
								title = currentData[2].toString();
							}
							
							long sends = 0;
							if(currentData[8] != null)
							{
								sends = Long.parseLong(currentData[8].toString());
							}
							
							TrackingSearchObject tso = new TrackingSearchObject();
							
							tso.setComments(numComment);
							tso.setModeration(moderated);
							tso.setName(title);
							tso.setRating(currentAvgScore);
							tso.setSent(sends);
							tso.setType(currentType);
							tso.setViews(views);
							tso.setVotings(votings);
							tso.setContentId(articleId);
							
							listTrackings.add(tso);
						}
					}
				}catch(Exception e){
					_log.error(e);
				}
			}
		}
		return listTrackings;
	}
	
	public static List<Object> getPageTrackingData(String layoutId, long groupId, String orderCol, String order, int startLimit, int endLimit)
	{
		
		String layoutFilter = "";
		
		if(layoutId != null && !layoutId.isEmpty())
		{
			layoutFilter = " AND p.layoutId='" + layoutId + "'";
		}
		
		String orderColDB = getOrderColumn(orderCol);
		
		String notId = "j.articleId NOT IN ('" + IterKeys.EXAMPLEARTICLEID + "','" + 
						IterKeys.EXAMPLEGALLERYID + "','" + IterKeys.EXAMPLEMULTIMEDIAID + "','" +
						IterKeys.EXAMPLEPOLLID + "')";
		
		int numEle = endLimit-startLimit;
		
		String sqlQuery = "(SELECT DISTINCT" +
				"(SELECT COUNT(*) FROM News_Comments m1 WHERE m1.contentId=j.articleId) AS numComment," +
				"(SELECT COUNT(*) FROM News_Comments m2 WHERE m2.contentId=j.articleId AND m2.moderated=FALSE) AS numUnmoderated, "+
				"j.title, p.typeContent, " + 
				"(SELECT SUM(c1.counter) FROM News_Counters c1 WHERE p.contentId=c1.contentId AND c1.operation=" + IterKeys.OPERATION_VIEW + ") AS visits, " +
				"p.contentId, " +
				"(SELECT COUNT(*) FROM PollsVote v WHERE v.questionId=o.pollId) AS votes, " +
				"(SELECT SUM(c2.value/c2.counter) FROM News_Counters c2 WHERE p.contentId=c2.contentId AND c2.operation=" + IterKeys.OPERATION_RATINGS + ") AS rank, " +
				"(SELECT SUM(c3.counter) FROM News_Counters c3 WHERE p.contentId=c3.contentId AND c3.operation=" + IterKeys.OPERATION_SENT + ") AS sents " +
				"FROM News_PageContent p " + 
				"LEFT JOIN News_Counters c1 ON p.contentId=c1.contentId " +
				"LEFT JOIN News_ArticlePoll o ON p.contentId=o.contentId " +
				"LEFT JOIN JournalArticle j ON p.contentId=j.articleId " +
		   		"WHERE " + 
		   		"(SELECT SUM(c4.counter) FROM News_Counters c4 WHERE p.contentId=c4.contentId AND c4.operation=" + IterKeys.OPERATION_VIEW + ") > 0 AND " 
		   		+ notId + " AND p.groupId=" + groupId + layoutFilter +
		   		" ORDER BY " + orderColDB + " " +  order +
		   		" LIMIT " + startLimit + "," + numEle + ")";
		
		List<Object> resultData = new ArrayList<Object>();
		
		try{
			resultData = PageContentLocalServiceUtil.sqlQuery(sqlQuery);
		}catch(Exception e){
			_log.error(e);
		}
		
		return resultData;
	}
	
	public static int getSizePageTrackingData(String layoutId, long groupId)
	{
		String layoutFilter = "";
		
		if(layoutId != null && !layoutId.isEmpty())
		{
			layoutFilter = " AND p.layoutId='" + layoutId + "'";
		}
		
		String notId = "j.articleId NOT IN ('" + IterKeys.EXAMPLEARTICLEID + "','" + 
				IterKeys.EXAMPLEGALLERYID + "','" + IterKeys.EXAMPLEMULTIMEDIAID + "','" +
				IterKeys.EXAMPLEPOLLID + "')";
		
		String sqlQuery = "SELECT COUNT(DISTINCT p.contentId) " +
				   "FROM News_PageContent p " +
				   "LEFT JOIN News_Counters c1 ON p.contentId=c1.contentId " +
				   "LEFT JOIN News_ArticlePoll o ON p.contentId=o.contentId " +
				   "LEFT JOIN JournalArticle j ON p.contentId=j.articleId " +
				   "WHERE (SELECT SUM(c2.counter) FROM News_Counters c2 WHERE p.contentId=c2.contentId AND c2.operation=" 
				   + IterKeys.OPERATION_VIEW + ") > 0 AND " + 
				   notId + " AND p.groupId=" + groupId + layoutFilter;
		
		int sizeData = 0;
		try{
			sizeData = Integer.parseInt(PageContentLocalServiceUtil.sqlQuery(sqlQuery).get(0).toString());
		}catch(Exception e){
			_log.error(e);
		}
		return sizeData;
	}
	
	public static List<Object> getArticleTrackingData(long groupId, String orderCol, String order, int startLimit, int endLimit, String queryFilters, String moderation)
	{
		
		String orderColDB = getOrderColumn(orderCol);
		
		String notId = "j.articleId NOT IN ('" + IterKeys.EXAMPLEARTICLEID + "','" + 
						IterKeys.EXAMPLEGALLERYID + "','" + IterKeys.EXAMPLEMULTIMEDIAID + "','" +
						IterKeys.EXAMPLEPOLLID + "')";
		
		int numEle = endLimit-startLimit;
		
		String sqlQuery = "(SELECT DISTINCT" +
				"(SELECT COUNT(*) FROM News_Comments m1 WHERE m1.contentId=j.articleId) AS numComment," +
				"(SELECT COUNT(*) FROM News_Comments m2 WHERE m2.contentId=j.articleId AND m2.moderated=FALSE) AS numUnmoderated, "+
				"j.title, p.typeContent, " + 
				"(SELECT SUM(c1.counter) FROM News_Counters c1 WHERE p.contentId=c1.contentId AND c1.operation=" + IterKeys.OPERATION_VIEW + ") AS visits, " +
				"p.contentId, " +
				"(SELECT COUNT(*) FROM PollsVote v WHERE v.questionId=o.pollId) AS votes, " +
				"(SELECT SUM(c2.value/c2.counter) FROM News_Counters c2 WHERE p.contentId=c2.contentId AND c2.operation=" + IterKeys.OPERATION_RATINGS + ") AS rank, " +
				"(SELECT SUM(c3.counter) FROM News_Counters c3 WHERE p.contentId=c3.contentId AND c3.operation=" + IterKeys.OPERATION_SENT + ") AS sents " +
				"FROM News_PageContent p " + 
				"LEFT JOIN News_Counters c1 ON p.contentId=c1.contentId " +
				"LEFT JOIN News_ArticlePoll o ON p.contentId=o.contentId " +
				"LEFT JOIN JournalArticle j ON p.contentId=j.articleId " +
		   		"WHERE " +
		   		"(SELECT SUM(c4.counter) FROM News_Counters c4 WHERE p.contentId=c4.contentId AND c4.operation=" + IterKeys.OPERATION_VIEW + ") > 0 AND " 
				+ notId + " AND p.groupId=" + groupId + " AND " + queryFilters + getModerationFilter(moderation) +
		   		" ORDER BY " + orderColDB + " " +  order +
		   		" LIMIT " + startLimit + "," + numEle + ")";
		
		List<Object> resultData = new ArrayList<Object>();
		
		try{
			resultData = PageContentLocalServiceUtil.sqlQuery(sqlQuery);
		}catch(Exception e){
			_log.error(e);
		}
		
		return resultData;
	}
	
	public static int getSizeArticleTrackingData(long groupId, String queryFilters, String moderation)
	{

		String notId = "j.articleId NOT IN ('" + IterKeys.EXAMPLEARTICLEID + "','" + 
				IterKeys.EXAMPLEGALLERYID + "','" + IterKeys.EXAMPLEMULTIMEDIAID + "','" +
				IterKeys.EXAMPLEPOLLID + "')";
		
		String sqlQuery = "SELECT COUNT(DISTINCT j.articleId) " +
				"FROM News_PageContent p " + 
				"LEFT JOIN News_Counters c1 ON p.contentId=c1.contentId " +
				"LEFT JOIN News_ArticlePoll o ON p.contentId=o.contentId " +
				"LEFT JOIN JournalArticle j ON p.contentId=j.articleId " +
				"WHERE (SELECT SUM(c2.counter) FROM News_Counters c2 WHERE p.contentId=c2.contentId AND c2.operation=" + 
				IterKeys.OPERATION_VIEW + ") > 0 AND " + 
				notId + " AND p.groupId=" + groupId + " AND " + queryFilters + getModerationFilter(moderation);
		
		int sizeData = 0;
		try{
			sizeData = Integer.parseInt(PageContentLocalServiceUtil.sqlQuery(sqlQuery).get(0).toString());
		}catch(Exception e){
			_log.error(e);
		}
		return sizeData;
	}
	
	private static String getModerationFilter(String moderation)
	{
		String moderationFilter = "";
		
		if(moderation != null && !moderation.isEmpty()){
			if(Boolean.parseBoolean(moderation)){
				moderationFilter = " AND (SELECT COUNT(*) FROM News_Comments m4 WHERE m4.contentId=j.articleId AND m4.moderated=FALSE) = 0";
			}else{
				moderationFilter = " AND (SELECT COUNT(*) FROM News_Comments m4 WHERE m4.contentId=j.articleId AND m4.moderated=FALSE) > 0";
			}
		}
		
		return moderationFilter;
	}
	
	public static String getSqlStructureFilter(boolean standardArticleCheck, boolean standardGalleryCheck, boolean standardPollCheck, boolean standardMultimediaCheck) 
	{
		String sqlFilter = "";
		if (standardArticleCheck || standardGalleryCheck || standardPollCheck || standardMultimediaCheck) {
			
			boolean addComa = false;
			sqlFilter += "j.structureId IN (";
			
			if (standardArticleCheck) {
				sqlFilter += "'" + IterKeys.STRUCTURE_ARTICLE + "'";
				addComa = true;
			}
			
			if (standardGalleryCheck) {
				if(addComa)
				{
					sqlFilter += ",";
				}
				sqlFilter += "'" + IterKeys.STRUCTURE_GALLERY + "'";
				addComa = true;
			}
			
			if (standardPollCheck) {
				if(addComa)
				{
					sqlFilter += ",";
				}
				sqlFilter += "'" + IterKeys.STRUCTURE_POLL + "'";
				addComa = true;
			}
			
			if (standardMultimediaCheck) {
				if(addComa)
				{
					sqlFilter += ",";
				}
				sqlFilter += "'" + IterKeys.STRUCTURE_MULTIMEDIA + "'";
				addComa = true;
			}
			sqlFilter += ")";
		} else {
			sqlFilter += "j.structureId='" + IterKeys.STRUCTURE_ARTICLE + "'";
		}
		
		return sqlFilter;
	}
	
	public static String getSqlDateFilter(Date start, Date end)
	{
		String sqlFilter = "";
		if (start != null && end != null) {
			SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
			try {
				sqlFilter += " AND j.createDate BETWEEN '" + format.format(start) + "' AND '"+ format.format(end) + "'";
			}catch(Exception e) {
				System.out.println(e.getMessage());
		    }
		}
		return sqlFilter;
	}
	
	public static String getSqlKeywordsFilter(String articleKeywords, String commentKeywords)
	{
		String sqlFilter = "";
		if (articleKeywords != null && !articleKeywords.isEmpty()) {
			articleKeywords = StringEscapeUtils.escapeSql(articleKeywords);
			sqlFilter += " AND (j.title LIKE '%" + articleKeywords + "%' OR j.content LIKE '%" + articleKeywords + "%')";
		}
		if (commentKeywords != null && !commentKeywords.isEmpty()) {
			commentKeywords = StringEscapeUtils.escapeSql(commentKeywords);
			sqlFilter += " AND (SELECT (SELECT COUNT(*) FROM News_Comments m3 " + 
						 "WHERE j.articleId=m3.contentId AND m3.message LIKE '%" + commentKeywords + "%') > 0)";
		}
		return sqlFilter;
	}
	
	private static String getOrderColumn(String colValue)
	{
		String orderCol = "title";
		if (colValue.equals("type")) {
			orderCol = "typeContent";
		} else if (colValue.equals("rating")) {
			orderCol = "rank";
		} else if (colValue.equals("views")) {
			orderCol = "counter";
		}else if (colValue.equals("comments")) {
			orderCol = "numComment";
		} else if (colValue.equals("moderation")) {
			orderCol = "numUnmoderated";
		} else if (colValue.equals("votings")) {
			orderCol = "votes";
		}
		return orderCol;
	}
	
	public static String getRelativeURL(PortletURL url)
	{
		String decodeURL = null;
		if(url != null)
		{
			decodeURL = url.toString();
			try{
				decodeURL = HttpUtil.decodeURL(decodeURL);
				URL urlAbsolute = new URL(decodeURL);
				decodeURL = urlAbsolute.getFile();
			}catch(Exception e){
				_log.error(e);
			}
		}
		return decodeURL;
	}
	
	public static double round(double value, int places) 
	{
		double result = 0;
	    if(places > -1)
	    {
		    long factor = (long) Math.pow(10, places);
		    value = value * factor;
		    long tmp = Math.round(value);
		    result = (double) tmp / factor;
	    }
	    return result;
	}
	
	public static List<String> getListPreference(PortletPreferences preferences, String preferenceName){
		String[] preferenceArray = preferences.getValues(preferenceName, null);
		List<String> preferenceList = null;
		if(preferenceArray != null && preferenceArray.length > 0)
		{
			preferenceList = Arrays.asList(preferenceArray);
		}
		
		return preferenceList;
	}
	
	public static boolean isCookie(HttpServletRequest request, String name)
	{
		boolean isCookie = false;
		
		Cookie[] cookies = request.getCookies();
		if(cookies != null && cookies.length > 0)
		{
			for(Cookie cookie:cookies)
			{
				if(cookie.getName().equals(name))
				{
					isCookie = true;
					break;
				}
			}
		}
		return isCookie;
	}
	
	public static String getportletName(HttpServletRequest request)
	{
		String retVal = "/tracking-portlet";
		
		if(PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_LIVE) && PHPUtil.isApacheRequest(request))
			retVal = "/kintra-portlet";
		
		return retVal;
	}

}

