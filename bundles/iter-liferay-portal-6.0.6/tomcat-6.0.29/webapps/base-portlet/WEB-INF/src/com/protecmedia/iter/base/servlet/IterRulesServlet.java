package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.IterRulesUtil;

public class IterRulesServlet extends HttpServlet
{
	private static final long serialVersionUID = -767331061596700647L;
	
	private static Log _log = LogFactoryUtil.getLog(IterRulesServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		StringBuilder iterRules = new StringBuilder();
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			long groupId = PortalUtil.getScopeGroupId(request);
			
			////////////////////////
			// REGLAS DEL SISTEMA //
			////////////////////////
			
			// Reglas de HTTPS
			iterRules.append(IterRulesUtil.getITerRulesHTTPS(groupId, PortalUtil.getHost(request))).append(StringPool.NEW_LINE);
			
			// Reglas de servicios externos
			iterRules.append(IterRulesUtil.getITerRulesExternalServices(groupId)).append(StringPool.NEW_LINE);
			
			// Reglas de RSS y ArticlesOf
			iterRules.append(IterRulesUtil.getIterRulesJson(groupId)).append(StringPool.NEW_LINE);

			///////////////////////
			// REGLAS DE USUARIO //
			///////////////////////
			
			iterRules.append(IterRulesUtil.getITerRulesUser(groupId)).append(StringPool.NEW_LINE);
			
			buildResponse(response, iterRules.toString());
		}
		catch (Throwable th)
		{
			_log.error(th);
			buildErrorResponse(response);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		this.doGet(request, response);
	}
	
	private void buildResponse(HttpServletResponse response, String result)
	{
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain; charset=UTF-8");
		
		PrintWriter out = null;
		try
		{
			out = response.getWriter();
			if (null != result)
			{
				out.print(result);
			}
			out.flush();
		}
		catch (IOException e)
		{
			_log.error(e);
		}
		finally
		{
			if (null != out)
				out.close();
		}
	}
	
	private void buildErrorResponse(HttpServletResponse response)
	{
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain; charset=UTF-8");
	}
}
