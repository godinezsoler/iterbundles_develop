/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.polls.service.PollsVoteLocalServiceUtil;
import com.liferay.util.survey.IterSurveyModel;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.ArticlePollLocalServiceUtil;
import com.protecmedia.iter.news.util.MyPollsVoteUtil;
import com.protecmedia.iter.user.service.CaptchaFormLocalServiceUtil;


public class PollsCaptchaVoteServlet extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactoryUtil.getLog(PollsCaptchaVoteServlet.class);

	/////////////
	// ERRORES //
	/////////////
	private static String ERR_CODE_BAD_REQUEST = "400";
	private static String ERR_CODE_UNSUPPORTED_MEDIA_TYPE = "415";
	private static String ERR_CODE_UNEXPECTED = "500";
	
	private static final String ERROR_FORMAT = "{\"error\":\"%s\"}";
	/** Error inesperado al procesar la operación */
	private static final String ERR_API_RESOURCE_UNEXPECTED = "{ \"error\": \"Unable to execute resource operation\" }";
	/** Tipo de contenido no soportado */
	private static final String ERR_API_RESOURCE_UNSUPPORTED_MEDIA_TYPE = "{ \"error\": \"Unsupported Media Type\" }";
	/** Petición incorrecta */
	private static final String ERR_API_RESOURCE_BAD_PAYLOAD = "{ \"error\": \"Payload is not a valid JSON\" }";
	/** Petición incorrecta */
	private static final String ERR_API_RESOURCE_BAD_REQUEST = "{ \"error\": \"Invalid parameter '%s'\" }";
	
	
	public PollsCaptchaVoteServlet()
	{
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			
			// Recupera los parámetros
			Map<String, String> params = getParameters(request);
			
			// Obtiene el grupo
			long groupId = PortalUtil.getScopeGroupId(request);
			
			// Valida el captcha si es necesario
			if (useCaptcha(request))
			{
				// Obtiene el captcha response
				String userResponseCaptcha = getParam(params, "g-recaptcha-response");
				// Valida el captcha
				ErrorRaiser.throwIfFalse(validateCaptcha(groupId, userResponseCaptcha, request.getRemoteAddr()), ERR_CODE_BAD_REQUEST, String.format(ERROR_FORMAT, "invalid_captcha"));
			}
			
			boolean retryInLiferay = false;
			if (IterSurveyUtil.isEnabled(groupId))
			{
				try
				{
					// Obtiene el id de la encuesta
					String surveyId = getParam(params, "questionId");
					// Obtiene el id de la respuesta
					String choiceId = getParam(params, "choiceId");
					// El id del usuario se obtiene de la cookie ITR_COOKIE_USRID
					String userId = CookieUtil.get(request, IterKeys.COOKIE_NAME_ITR_COOKIE_USRID);
					
					// Registra el voto
					IterSurveyUtil.addVote(surveyId, choiceId, userId);
					
					// Obtiene los resultados
					String resultPayload = IterSurveyModel.getResultsAsJson(surveyId);
					buildResponse(response, HttpServletResponse.SC_OK, resultPayload);
				}
				catch (Throwable th)
				{
					if (th instanceof ServiceError)
					{
						// No se encuentra la encuesta
						if (((ServiceError) th).getErrorCode().equals(IterErrorKeys.XYZ_E_ITER_SURVEY_NOT_FOUND_ZYX))
						{
							retryInLiferay = true;
						}
						// Voto duplicado
						else if (((ServiceError) th).getErrorCode().equals(IterErrorKeys.XYZ_E_ITER_SURVEY_DUPLICATE_VOTE_ZYX))
						{
							buildResponse(response, HttpServletResponse.SC_CONFLICT, String.format(ERROR_FORMAT, "duplicatevote"));
						}
						// Encuesta cerrada
						else if (((ServiceError) th).getErrorCode().equals(IterErrorKeys.XYZ_E_ITER_SURVEY_CLOSED_ZYX))
						{
							buildResponse(response, HttpServletResponse.SC_CONFLICT, String.format(ERROR_FORMAT, "closedsurvey"));
						}
						else
						{
							int status = Integer.valueOf(((ServiceError) th).getErrorCode());
							String msg = (((ServiceError) th).getMessage());
							buildResponse(response, status, msg);
						}
					}
					else
					{
						buildResponse(response, HttpServletResponse.SC_BAD_REQUEST, String.format(ERROR_FORMAT, "invalid_request"));
					}
				}
			}
			
			if (!IterSurveyUtil.isEnabled(groupId) || retryInLiferay)
			{
				try
				{
					// Obtiene el id de la encuesta
					long questionId = Long.valueOf(getParam(params, "questionId"));
					// Obtiene el id de la respuesta
					long choiceId = Long.valueOf(getParam(params, "choiceId"));
					// Genera un id de usuario
					long userId = CounterLocalServiceUtil.increment();
					
					PollsVoteLocalServiceUtil.addVote(userId, questionId, choiceId, new ServiceContext());							
				 	MyPollsVoteUtil.saveVote(request, questionId);
	
					String resultPayload = ArticlePollLocalServiceUtil.getPollResultsAsJson(GroupMgr.getGlobalGroupId(), questionId);
					buildResponse(response, HttpServletResponse.SC_OK, resultPayload);
				}
				catch (Throwable th)
				{
					buildResponse(response, HttpServletResponse.SC_BAD_REQUEST, String.format(ERROR_FORMAT, "invalid_request"));
				}
			}
		}
		catch (Throwable th)
		{
			log.error(th);
			if (th instanceof ServiceError)
			{
				int status = Integer.valueOf(((ServiceError) th).getErrorCode());
				String msg = (((ServiceError) th).getMessage());
				buildResponse(response, status, msg);
			}
			else
			{
				buildResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format(ERROR_FORMAT, "internal_error"));
			}
		}
	}
	
	private Map<String, String> getParameters(HttpServletRequest request) throws ServiceError
	{
		Map<String, String> params = new HashMap<String, String>();
		
		// Obtiene el tipo de contenido
		String contentType = request.getHeader("Content-Type");
		if (Validator.isNotNull(contentType))
			contentType = contentType.split(StringPool.SEMICOLON)[0];
		
		if ("application/x-www-form-urlencoded".equals(contentType))
		{
			// Obtiene el id de la encuesta
			getFormParam(request, params, "questionId");
			
			// Obtiene el id de la respuesta
			getFormParam(request, params, "choiceId");
			
			// Obtiene el captcha response
			getFormParam(request, params, "g-recaptcha-response");
		}
		else if ("application/json".equals(contentType))
		{
			// Obtiene el payload
			JSONObject payload = getPayload(request);
			
			// Obtiene el id de la encuesta
			getJsonParam(payload, params, "questionId");
			
			// Obtiene el id de la respuesta
			getJsonParam(payload, params, "choiceId");
			
			// Obtiene el captcha response
			getJsonParam(payload, params, "g-recaptcha-response");
		}
		else
		{
			ErrorRaiser.throwIfError(ERR_CODE_UNSUPPORTED_MEDIA_TYPE, ERR_API_RESOURCE_UNSUPPORTED_MEDIA_TYPE);
		}
		
		return params;
	}
	
	private void getFormParam(HttpServletRequest request, Map<String, String> paramsMap, String paramName) throws ServiceError
	{
		String paramValue = ParamUtil.get(request, paramName, null);
		paramsMap.put(paramName, paramValue);
	}

	private void getJsonParam(JSONObject payload, Map<String, String> paramsMap, String paramName) throws ServiceError
	{
		String paramValue = payload.getString(paramName);
		paramsMap.put(paramName, paramValue);
	}
	
	private String getParam(Map<String, String> paramsMap, String paramName) throws ServiceError
	{
		String paramValue = paramsMap.get(paramName);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(paramValue), ERR_CODE_BAD_REQUEST, String.format(ERR_API_RESOURCE_BAD_REQUEST, paramName));
		return paramValue;
	}
	
	/**
	 * <p>Recupera el payload de la petición.</p>
	 * 
	 * @param request {@code HttpServletRequest} de la petición.
	 * @return {@code JSONObject} con el payload.
	 * @throws ServiceError {@code ERR_API_RESOURCE_UNEXPECTED} si ocurre un error al recuperar el payload.
	 */
	private JSONObject getPayload(HttpServletRequest request) throws ServiceError
	{
		try
		{
			StringBuilder payloadBuilder = new StringBuilder();
			BufferedReader reader = request.getReader();
			String line;
		    while ((line = reader.readLine()) != null)
		    {
		    	payloadBuilder.append(line);
		    }
		    
		    return JSONFactoryUtil.createJSONObject(payloadBuilder.toString());
		}
		catch (IOException e)
		{
			ErrorRaiser.throwIfError(ERR_CODE_UNEXPECTED, ERR_API_RESOURCE_UNEXPECTED);
		}
		catch (JSONException e)
		{
			ErrorRaiser.throwIfError(ERR_CODE_BAD_REQUEST, ERR_API_RESOURCE_BAD_PAYLOAD);
		}
		
	    return null;
	}
	
	private boolean useCaptcha(HttpServletRequest request)
	{
		return Validator.isNotNull(request.getAttribute("REQUEST_ATTRIBUTE_VALIDATE_CAPTCHA")) ?
				((Boolean) request.getAttribute("REQUEST_ATTRIBUTE_VALIDATE_CAPTCHA")).booleanValue() :
				false;
	}
	
	private boolean validateCaptcha(long groupId, String userResponseCaptcha, String remoteAddress)
	{
		boolean correctCaptcha = false;
		if (Validator.isNotNull(userResponseCaptcha))
		{
			try
			{
				// Valida el captcha
				correctCaptcha = CaptchaFormLocalServiceUtil.isValid(groupId, userResponseCaptcha, remoteAddress);
			}
			catch (Throwable th)
			{
				log.error(th);
			}
		}
		return correctCaptcha;
	}
	
	private void buildResponse(HttpServletResponse response, int status, String payload)
	{
		try
		{
			response.setStatus(status);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
	
		    PrintWriter out;
			out = response.getWriter();
			out.print(payload);
			out.flush();
			out.close();
		}
		catch (Throwable e)
		{
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(e);
		}
	}
}

