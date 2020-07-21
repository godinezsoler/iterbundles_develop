package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.user.OTPMgr;
import com.protecmedia.iter.user.util.UserUtil;

public class OTPServlet extends HttpServlet   
{
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(OTPServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		PortalUtil.setVirtualHostLayoutSet(request);
		long groupId = GroupMgr.getGlobalGroupId();

		try
		{
			groupId = PortalUtil.getScopeGroupId(request);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX, IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX);
		}
		catch (Throwable th)
		{
			processError(groupId, "", th, response);
		}
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		String operation = "";
		long groupId 	 = GroupMgr.getGlobalGroupId();
		
		try
		{
			String payload	 = IOUtils.toString(request.getReader());
			PortalUtil.setVirtualHostLayoutSet(request);
			
			String[] urlPath = PortalUtil.getCurrentURL(request).toLowerCase().split("/");
			operation 		 = urlPath[urlPath.length-1];
			groupId 	 	 = PortalUtil.getScopeGroupId(request);
			
			if (operation.equals(OTPMgr.OP_GENERATION))
				OTPMgr.doGeneration(groupId, payload);
			
			else if (operation.equals(OTPMgr.OP_VALIDATION))
				OTPMgr.doValidation(groupId, payload);

			else if (operation.equals(OTPMgr.OP_SENDMSG))
				OTPMgr.doSendMsg(groupId, payload);
			
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX, IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX);				
			
			processOK(response);
		}
		catch (Throwable th)
		{
			processError(groupId, operation, th, response);
		}
	}
	
	private void processOK(HttpServletResponse response)
	{
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		printResponse("{}", response);
	}
	private void processError(long groupId, String operation, Throwable th, HttpServletResponse response)
	{
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		// Errores conocidos
		if (th instanceof ServiceError && ((ServiceError) th).getErrorCode().equals(IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX))
				response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		// Errores inesperados Internal error
		else
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		String msg = th.getMessage();
		try
		{
			// Sea el error que sea, se devuelve la cadena configurada según el tipo de operación
			if (operation.equals(OTPMgr.OP_GENERATION))
				msg = GetterUtil.getString2(GroupConfigTools.getGroupConfigFieldFromDB(groupId, UserUtil.OTP_GENERATION_HAS_FAILED_MSG), IterErrorKeys.XYZ_E_OTP_GENERATION_HAS_FAILED_ZYX);
			
			else if (operation.equals(OTPMgr.OP_VALIDATION))
				msg = GetterUtil.getString2(GroupConfigTools.getGroupConfigFieldFromDB(groupId, UserUtil.OTP_VALIDATION_HAS_FAILED_MSG), IterErrorKeys.XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX);
			
			else if (operation.equals(OTPMgr.OP_SENDMSG))
				msg = GetterUtil.getString2(GroupConfigTools.getGroupConfigFieldFromDB(groupId, UserUtil.OTP_SENDMSG_HAS_FAILED_MSG),    IterErrorKeys.XYZ_E_OTP_SENDMSG_HAS_FAILED_ZYX);
		}
		catch (Exception e)
		{
			_log.error(e);
		}
		
		if (Validator.isNull(msg))
		{
			if (th instanceof ServiceError)
				msg = ((ServiceError) th).getErrorCode();
			else
				msg = th.toString();
		}
		
		msg = String.format("{ \"exception\": \"%s\" }", msg);
		
		_log.error(th);
		IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, null, th);
		
		
		
		printResponse(msg, response);
	}
	
	private void printResponse(String responseBody, HttpServletResponse response)
	{
	    PrintWriter out = null;
		try
		{
			out = response.getWriter();
			
			if (Validator.isNull(responseBody))
			{
				response.setContentType("text/html");
				responseBody = StringPool.PERIOD;
			}
			out.print(responseBody);
		}
		catch (IOException e)
		{
			_log.error(e);
		}
		finally
		{
			if (out != null)
			{
				out.flush();
				out.close();
			}
		}
	}

}
