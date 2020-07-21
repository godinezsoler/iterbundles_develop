package com.protecmedia.iter.tracking.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.protecmedia.iter.tracking.util.UserFeedbackTools;

public class RatingsServlet extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(RatingsServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if(request.getServletPath().equalsIgnoreCase("/feedback"))
		{
			try
			{
				IterRequest.setOriginalRequest(request);
				UserFeedbackTools.getUsersFeedback(request, response);
			}
			finally
			{
				IterRequest.unsetOriginalRequest();
			}
		}
		else
		{
			//Rating de las estrellas.
			RequestDispatcher rd = request.getRequestDispatcher("/html/content-ratings-portlet/ratings_results.jsp");
			String pathInfo = request.getPathInfo();
			if(Validator.isNotNull(pathInfo))
			{
				String[] params = pathInfo.split(StringPool.SLASH);
				if(params != null && params.length > 2)
				{
					request.setAttribute("scopeGroupId", GetterUtil.get(params[1], "0"));
					request.setAttribute("contentId", GetterUtil.get(params[2], "0"));
					rd.forward(request, response);
				}
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if(request.getServletPath().equalsIgnoreCase("/feedback"))
		{
			UserFeedbackTools.setUserFeedback(request, response);
		}
		else
		{
			RequestDispatcher rd = request.getRequestDispatcher("/html/content-ratings-portlet/vote.jsp");
			try
			{
				rd.forward(request, response);
			}
			catch (Throwable th)
			{
				_log.error(th);
			}
		}
	}
}