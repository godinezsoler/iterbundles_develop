package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil;
import com.protecmedia.iter.user.util.UserUtil;

public class ResetUserCredentials extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(ResetUserCredentials.class);

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		
		long groupId = 0;
		
		try 
		{
			PortalUtil.setVirtualHostLayoutSet(req);
			groupId = PortalUtil.getScopeGroupId(req);
			
			ErrorRaiser.throwIfNull(req.getParameter("p"));
			String p = URLEncoder.encode( req.getParameter("p"), Digester.ENCODING );
			String plainToken = PayCookieUtil.plainCookieValue(p);
			Map<String, String> tokenMap = PayCookieUtil.getCookieAsMap( plainToken, IterKeys.COOKIE_NAME_USERID_DOGTAG );
			
			if(IterKeys.EDIT_PROFILE_INTENT_FORGOT.equals(tokenMap.get(IterKeys.COOKIE_NAME_INTENT)))
			{
				
				IterRegisterLocalServiceUtil.resetUserCredentials(req, resp, getServletContext().getContext("/"), groupId,
						tokenMap.get(IterKeys.COOKIE_NAME_USER_ID), tokenMap.get(IterKeys.COOKIE_NAME_EXPIRE),
						JSONFactoryUtil.createJSONObject( tokenMap.get(IterKeys.COOKIE_NAME_EXTRADATA) ) );
			}

		} 
		catch (Exception e)
		{
			_log.error(e);
			UserUtil.redirectTo(resp, groupId, UserUtil.FOGET_ERROR_LAYOUT_NAME);
		}
		
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		
	}
	
}
