package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ContextVariables;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil;
import com.protecmedia.iter.user.util.SSOLoginUtil;
import com.protecmedia.iter.user.util.UserKeys;
import com.protecmedia.iter.user.util.UserUtil;

public class RefreshUserEntitlements extends HttpServlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(RefreshUserEntitlements.class);
	
	protected void doGet( HttpServletRequest request, HttpServletResponse response )
	{
		String onrefresh = "";
		String ssodata = StringPool.BLANK;
		String errorPage = "";
		long groupId = 0;
		String ssoUrlChain = null;
		
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			groupId = PortalUtil.getScopeGroupId(request);
			
			// Página de destino cuando termine el proceso, si no viene parametrizada en la llamada por defecto se va a la portada
			
			onrefresh = GetterUtil.getString( request.getParameter(IterKeys.ACTION_REDIRECT), "/" );
			
			// Datos cifrados del SSO. Si llegan informados, es un registro / login desde un sistema externo.
			ssodata = GetterUtil.getString( request.getParameter("ssodata"), StringPool.BLANK );
			
			if (Validator.isNull(ssodata))
			{
				request.setAttribute(IterKeys.REQUEST_ATTRIBUTE_ROOT_CONTEXT, getServletContext().getContext("/"));
				
				String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
				errorPage = PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_REFRESHUSERENTITLEMENTS_ON_FAILED, friendlyGroupURL) );
				
				IterRegisterLocalServiceUtil.refreshUserEntitlements(friendlyGroupURL, onrefresh, errorPage, request, response, false);
				
				
				if( Validator.isNotNull(ContextVariables.getRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS).get(UserKeys.ITER_SUBSCRIPTION_SYSTEM_MSG)) )
					UserUtil.forwardTo(request, response, groupId, errorPage);
			}
			else
			{
				SSOLoginUtil ssol = new SSOLoginUtil(groupId);
				ssol.doSingleSingOn(ssodata, request);
				
				try
				{
					if (ssol.isValidProcess())
					{
						// Añade una cookie básica para el login que será sustituida por la definitiva si el proceso va bien.
						ssol.addResponseCookie(request, response);
						// Realiza la autenticación del usuario y consulta sus suscripciones.
						String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
						ssoUrlChain = IterRegisterLocalServiceUtil.refreshUserEntitlements(friendlyGroupURL, onrefresh, onrefresh, request, response, true);
					}
				}
				catch (Throwable th)
				{
					IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "SSO Login process: Error refreshing entitlements", th);
					_log.error(th);
				}
				finally
				{
					// Comprueba si todo ha ido bien y si no es así registra el mensaje en el Monitor y añade una cookie básica para el login
					Map<String, String> subscriptionCtxVars = ContextVariables.getRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS);
					if (subscriptionCtxVars != null && subscriptionCtxVars.size() > 0 && IterKeys.KO.equals(subscriptionCtxVars.get(UserKeys.ITER_SUBSCRIPTION_SYSTEM_CODE)))
					{
						String msg = subscriptionCtxVars.get(UserKeys.ITER_SUBSCRIPTION_SYSTEM_TECH_CODE);
						subscriptionCtxVars.remove(UserKeys.ITER_SUBSCRIPTION_SYSTEM_TECH_CODE);
						IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "SSO Login process: " + (msg == null ? StringPool.BLANK : msg));
						_log.error(msg);
					}
					
					// Siempre retorna un 302 al referal o a la cadena de redirecciones SSO
					response.setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
					if (Validator.isNotNull(ssoUrlChain))
						response.sendRedirect(ssoUrlChain);
					else
					{
						String hasSuccess = onrefresh.equals( request.getParameter(IterKeys.ACTION_REDIRECT) ) ? "#success=false" : "";
						response.sendRedirect(onrefresh.concat(hasSuccess));
					}
				}
			}
			
		}
		catch(Exception e)
		{
			_log.error(e);
			
			Map<String, String> subscriptionCtxVars = ContextVariables.getRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS);
			subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_CODE, IterKeys.KO);
			subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_MSG, UserKeys.INTERNAL_SERVER_ERROR);
			subscriptionCtxVars.put(UserKeys.ITER_REDIRECTURL_PARAM, onrefresh);
			subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_HTTPSTATUSCODE, String.valueOf(HttpStatus.SC_INTERNAL_SERVER_ERROR) );
			subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_HTTPSTATUSLINE, UserKeys.INTERNAL_SERVER_ERROR);
			ContextVariables.setRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS, subscriptionCtxVars);
			
			try
			{
				UserUtil.forwardTo(request, response, groupId, errorPage);
			}
			catch(IOException ioe)
			{
				_log.error(ioe);
			}
		}
		
	}
}
