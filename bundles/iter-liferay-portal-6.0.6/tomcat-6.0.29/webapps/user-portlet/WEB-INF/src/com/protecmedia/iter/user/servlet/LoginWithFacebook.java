package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.dao.jdbc.JdbcConnectorUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.user.service.IterRegisterServiceUtil;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.user.util.social.FacebookUtil;
import com.protecmedia.iter.user.util.social.SocialNetworkUtil;


public class LoginWithFacebook extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	
	private static Log _log = LogFactoryUtil.getLog(LoginWithFacebook.class);
	
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{	
		JdbcConnectorUtil.setConnection();
		
		long groupId = 0;
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			
			if(Validator.isNull(PayCookieUtil.getUserId(request)))
			{
				groupId = SocialNetworkUtil.getGroupIdFromRequest(request);
				if(groupId > 0)
				{
					String code = request.getParameter(SocialNetworkUtil.PARAM_CODE);
					if(Validator.isNull(code))
					{
						String error = request.getParameter(SocialNetworkUtil.PARAM_ERROR);
						if(Validator.isNull(error))
						{
							error = request.getParameter(SocialNetworkUtil.PARAM_ERROR_CODE);
							if(Validator.isNull(error))
							{
								SocialNetworkUtil.addRedirectCookie(request, response);
								String scope = request.getParameter(SocialNetworkUtil.PARAM_SCOPE);
								response.sendRedirect(FacebookUtil.getCodeURL(request, scope, groupId));
							}
						}

						if(Validator.isNotNull(error))
						{
							_log.error(URLDecoder.decode(request.getQueryString(), "UTF-8"));
							SocialNetworkUtil.redirectToLoginWithErrorPage(groupId, response);
						}
					}
					else
					{
						request.setAttribute(IterKeys.REQUEST_ATTRIBUTE_ROOT_CONTEXT, getServletContext().getContext("/"));
						
						IterRegisterServiceUtil.preRegisterUser(request, response, 
								SocialNetworkUtil.SOCIAL_NAME_FACEBOOK, groupId, 
								FacebookUtil.getDetailsByCode(request, code, groupId));
						
						IterRegisterServiceUtil.initOrUpdateUserToDeleteTask();
					}
				}
			}
			else
			{
				UserUtil.redirectToCookieReferer(request, response);
			}
		}
		catch(Exception e)
		{
			_log.error(e);
			
			SocialNetworkUtil.redirectToLoginWithErrorPage(groupId, response);
		}
		finally
		{
			JdbcConnectorUtil.unsetConnection();
			IterRequest.unsetOriginalRequest();
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		
	}
}
