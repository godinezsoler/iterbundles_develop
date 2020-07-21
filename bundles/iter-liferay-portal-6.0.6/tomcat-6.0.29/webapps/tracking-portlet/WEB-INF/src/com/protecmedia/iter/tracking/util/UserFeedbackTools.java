package com.protecmedia.iter.tracking.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.CountersLocalServiceUtil;

public class UserFeedbackTools implements Runnable
{
	private static Log _log = LogFactoryUtil.getLog(UserFeedbackTools.class);
	
	private String _scopeGrpId;
	private String _articleId;
	
	public UserFeedbackTools(String scopeGroupId, String articleId)
	{
		this._scopeGrpId = scopeGroupId;
		this._articleId = articleId;
	}
	
	private static String GET_QUESTION_ANSWERS = new StringBuilder()
		.append(" SELECT q.label AS question, q.thanksmsg, q.actcookiesmsg, q.existsvotemsg, q.anonymousrating, opt.label, opt.choiceid,  opt.choiceorder \n")
		.append(" FROM feedbackquestion q INNER JOIN feedbackchoice opt \n")
		.append(" \tON(q.questionid=opt.questionid AND q.groupid=%s) \n")
		.append(" ORDER BY opt.choiceorder ASC ")
		.toString();
	
	public static List<Map<String,Object>> getQuestionAnswers(HttpServletRequest request, long scopeGroupId)
	{
		HttpServletRequest original_request = PortalUtil.getOriginalServletRequest(request);
		
		@SuppressWarnings("unchecked")
		List<Map<String,Object>> feedbackData = (List<Map<String,Object>>) original_request.getAttribute( "feedbackData" );
		
		if(feedbackData==null)
		{
			String query = String.format(GET_QUESTION_ANSWERS, scopeGroupId);
			
			if(_log.isDebugEnabled())
				_log.debug("GET_QUESTION_ANSWERS: " + query);
			
			feedbackData = PortalLocalServiceUtil.executeQueryAsMap(query);
			original_request.setAttribute( "feedbackData", feedbackData );
		}
		
		return feedbackData;
	}
	
	public static void getUsersFeedback(HttpServletRequest request, HttpServletResponse response)
	{
		boolean error = false;
		
		try
		{
			String pathInfo = request.getPathInfo();
			if(Validator.isNotNull(pathInfo))
			{
				pathInfo=pathInfo.substring(1);
				String[] params = pathInfo.split(StringPool.SLASH);
				if(params != null && params.length == 2)
				{
					String scopeGroupId	= GetterUtil.get(params[0], "0");
					String articleId	= GetterUtil.get(params[1], "0");
					
					if(Validator.isNotNull(articleId) && Validator.isNotNull(scopeGroupId))
					{
						JSONObject results =  CountersLocalServiceUtil.getUsersFeedback(scopeGroupId, articleId);
						
						response.setContentType("application/x-feedback");
						if(PHPUtil.isApacheRequest(request))
							response.setHeader(WebKeys.ITER_RESPONSE_NEEDS_PHP, "1");
						
						response.getWriter().print(results);
						response.getWriter().flush();
					}
					else 
					{
						error = true;
						if(_log.isDebugEnabled())
							_log.debug( String.format("Invalid paramenters article: %s, group: %s", articleId, scopeGroupId) );
					}
				}
				else
				{
					error = true;
					if(_log.isDebugEnabled())
						_log.debug("Incorrect number of params. Expected 2, obtained " + (params != null ? params.length : "0") );
				}
			}
			else
			{
				error = true;
				if(_log.isDebugEnabled())
					_log.debug("Path info is null or empty: " + pathInfo);
			}
		}
		catch(Exception e)
		{
			_log.error(e);
			error = true;
		}
		
		if(error)
			response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
	}
	
	public static void setUserFeedback(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			String pathInfo = request.getPathInfo();
			if(Validator.isNotNull(pathInfo))
			{
				pathInfo=pathInfo.substring(1);
				String[] params = pathInfo.split(StringPool.SLASH);
				if(params != null && params.length == 4)
				{
					String scopeGroupId	= GetterUtil.get(params[0], "0");
					String articleId 	= GetterUtil.get(params[1], "0");
					String usrid 		= GetterUtil.get(params[2], "0");
					String optionid 	= GetterUtil.get(params[3], "0");
					
					JSONObject results = null;
					
					//0 - Already voted
					//1 - Thanks for voting
					//2 - Cannot vote
					int messageKey = 2;
					
					if(Validator.isNotNull(articleId) && Validator.isNotNull(scopeGroupId) && Validator.isNotNull(usrid) && Validator.isNotNull(optionid) )
					{
						if( TrackingUtil.isCookie(request, IterKeys.COOKIE_NAME_ITR_COOKIE_USRID) )
						{
							if( !TrackingUtil.isCookie(request, String.format(IterKeys.COOKIE_NAME_CONTENT_VOTED, usrid, articleId)) )
							{
								try
								{
									results = CountersLocalServiceUtil.setUserFeedback(scopeGroupId, articleId, usrid, optionid);
									
									int maxage = 30*24*3600;
									if(results!=null)
									{
										SimpleDateFormat sdf = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
										String vigencia = results.getString("vigencia");
										if(Validator.isNotNull(vigencia))
										{
											Date maxageDate = sdf.parse( vigencia );
											maxage = (int)( (maxageDate.getTime()-Calendar.getInstance().getTimeInMillis())/1000 );
										}
									}
									
									Cookie contentVotedCookie = new Cookie(String.format(IterKeys.COOKIE_NAME_CONTENT_VOTED, usrid, articleId), null);
									contentVotedCookie.setPath(TrackingUtil.getportletName(request) + "/feedback");
									contentVotedCookie.setMaxAge( maxage );
									response.addCookie(contentVotedCookie);
									
									messageKey = 1;
									response.setContentType("text/javascript");
									response.setHeader("Cache-Control","no-store, no-cache, must-revalidate");
									
									if(PHPUtil.isApacheRequest(request))
									{
										UserFeedbackTools uft = new UserFeedbackTools(scopeGroupId, articleId);
										uft.invalidateApachesCache();
									}
								}
								catch (Exception e)
								{
									String err = e.getCause().toString();
									if(err.contains("XYZ_ITR_UNQ_FEEDBACKUSER_USERID_ARTICLEID_ZYX"))
										messageKey = 0;
									
									_log.error(err);
									_log.debug(e);
								}
							}
							else
							{
								messageKey = 0;
								if(_log.isDebugEnabled())
								{
									_log.debug( String.format("User %s has already voted for article %s", usrid, articleId) );
								}
							}
						}
						else
						{
							messageKey = 0;
							if(_log.isDebugEnabled())
							{
								_log.debug( "STICKY cookie does not exists" );
							}
						}
					}
					else if(_log.isDebugEnabled())
					{
						_log.debug( String.format("Invalid paramenters article: %s, group: %s, user: %s, option: %s", articleId, scopeGroupId, usrid, optionid) );
					}
					
					if(results==null)
						results = JSONFactoryUtil.createJSONObject();
					
					results.put("messageKey", messageKey);
					response.getWriter().print(results);
					response.getWriter().flush();
				}
				else if(_log.isDebugEnabled())
				{
					_log.debug("Incorrect number of params. Expected 4, obtained " + (params != null ? params.length : "0") );
				}
			}
			else if(_log.isDebugEnabled())
			{
				_log.debug("Path info is null or empty: " + pathInfo);
			}
		}
		catch(Throwable th)
		{
			_log.error(th);
		}
	}

	private void invalidateApachesCache() throws SystemException
	{
		try
		{
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.submit( this );
			
			executorService.shutdown();
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			String virtualHost = LayoutSetLocalServiceUtil.getLayoutSet(Long.valueOf(_scopeGrpId), false).getVirtualHost();
			if(Validator.isNotNull(virtualHost))
			{
				// http://jira.protecmedia.com:8080/browse/ITER-410
				// "Las peticiones que realiza el tomcat al apache DEBEN seguir siendo http (y puerto 80) ..."
				String invalidateURL = new StringBuilder("http://")
											.append(virtualHost)
											.append("/kintra-portlet/feedback/")
											.append(_scopeGrpId)
											.append("/")
											.append(_articleId)
											.toString();

				ApacheUtil.notifyToAllApaches(invalidateURL);
			}
		}
		catch (Exception e)
		{
			_log.error(e);
		}
	}
}
