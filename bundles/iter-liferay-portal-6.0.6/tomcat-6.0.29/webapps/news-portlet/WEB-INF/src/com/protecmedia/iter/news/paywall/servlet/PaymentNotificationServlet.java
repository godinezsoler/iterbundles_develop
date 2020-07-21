package com.protecmedia.iter.news.paywall.servlet;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.util.PaywallUtil;

public class PaymentNotificationServlet extends HttpServlet   
{
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactoryUtil.getLog(PaymentNotificationServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		PortalUtil.setVirtualHostLayoutSet(request);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		log.info("Payment webhook notification received");
		PortalUtil.setVirtualHostLayoutSet(request);
		long groupId = 0;
		
		try
		{
			// Obtiene el grupo
			groupId = PortalUtil.getScopeGroupId(request);
			
			// Recupera el nombre del proveedor de la pasarela de pago
			String[] requestUri = request.getRequestURI().split(StringPool.SLASH);
			ErrorRaiser.throwIfFalse(requestUri.length == 4, PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);
			String providerName = requestUri[3];
			
			// Procesa la transacción
			PaywallUtil.processPayment(groupId, providerName, request);
		}
		catch(Throwable th)
		{
			log.error(th);
			if (groupId > 0)
				IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_MSG_WEBHOOK_ERROR , th);
		}
		finally
		{
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}
}
