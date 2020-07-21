package com.protecmedia.iter.base.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.HealthMgr;
import com.protecmedia.iter.base.cluster.Heartbeat;

public class HealthCheck extends HttpServlet 
{
	private static final long serialVersionUID = -767331061596700648L;

	private static Log _log = LogFactoryUtil.getLog(HealthCheck.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			// ITER-1054 Soporte Cloud: Solo el servidor responsable de los procesos desatendidos informará que está saludable
			buildResponse(response, Heartbeat.canLaunchProcesses() && HealthMgr.isHealthy() ? 
							HttpServletResponse.SC_OK 										:
							HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		catch (Throwable th)
		{
			_log.error(th);
			buildResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		this.doGet(request, response);
	}

	private void buildResponse(HttpServletResponse response, int status)
	{
		response.setStatus(status);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain; charset=UTF-8");
	}
}
