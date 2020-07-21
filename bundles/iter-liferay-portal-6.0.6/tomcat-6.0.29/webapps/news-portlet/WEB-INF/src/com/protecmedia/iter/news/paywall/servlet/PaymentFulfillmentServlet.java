package com.protecmedia.iter.news.paywall.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.news.paywall.model.PaywallTransactionModel;
import com.protecmedia.iter.news.paywall.provider.PaypalMgr;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.util.PaywallUtil;

public class PaymentFulfillmentServlet extends HttpServlet   
{
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactoryUtil.getLog(PaymentFulfillmentServlet.class);
	
	/**
	 * Redsys usa este servlet tras efectuar un pago con redirección.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		PortalUtil.setVirtualHostLayoutSet(request);
		long groupId = 0;
		
		try
		{
			// Recupera los parámetros de la llamada
			groupId = PortalUtil.getScopeGroupId(request);
			
			// Recupera el nombre del proveedor de la pasarela de pago
			String[] requestUri = request.getRequestURI().split(StringPool.SLASH);
			ErrorRaiser.throwIfFalse(requestUri.length == 4, PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);
			String providerName = requestUri[3];
			
			// Procesa la transacción
			PaywallTransactionModel transaction = PaywallUtil.processPayment(groupId, providerName, request.getQueryString());
			if (transaction.isCompleted())
			{
				// Manda al usuario a refreshUserEntitlements para actualizar sus derechos en la web
				response.sendRedirect("/user-portlet/refreshuserentitlements");
			}
			else
			{
				// Manda al usuario a la URL de error definida en el portlet
				response.sendRedirect(transaction.getErrorUrl());
			}
		}
		catch (Throwable th)
		{
			log.error(th);
			if (groupId > 0)
				IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_MSG_TRANSACTION_ERROR , th);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * PayPal usa este servlet para crear una transacción y ejecutarla.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		PortalUtil.setVirtualHostLayoutSet(request);
		
		try
		{
			// Recupera los parámetros de la llamada
			long groupId = PortalUtil.getScopeGroupId(request);
			String[] requestUri = request.getRequestURI().split(StringPool.SLASH);
			ErrorRaiser.throwIfFalse(requestUri.length == 6, PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);

			String providerName = requestUri[3];
			ErrorRaiser.throwIfFalse("paypal".equals(providerName), PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);

			String intent = requestUri[4];
			ErrorRaiser.throwIfFalse("create-payment".equals(intent) || "execute-payment".equals(intent), PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);
			
			String product = requestUri[5];
			ErrorRaiser.throwIfFalse(Validator.isNotNull(product), PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);
			
			// Comprueba que el usuario esté autenticado
			Cookie[] requestCookies = CookieUtil.getCookies(request, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG});
			Cookie dogTagCookie = (Cookie)ArrayUtil.getValue(requestCookies, 0);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(dogTagCookie), PaywallErrorKeys.PAYWALL_E_UNAUTHORIZED);
			Map<String, String> dogTag = PayCookieUtil.getCookieAsMap( PayCookieUtil.plainCookieValue(dogTagCookie.getValue()), IterKeys.COOKIE_NAME_USER_DOGTAG );
			
			if ("create-payment".equals(intent))
			{
				String errorUrl  = request.getParameter("errorUrl");
				String paymentId = PaypalMgr.INSTANCE.createPayment(groupId, product, dogTag.get(IterKeys.COOKIE_NAME_USER_ID), errorUrl);
				JsonObject result = new JsonObject();
				// Si es un error
				if (paymentId.startsWith("{"))
				{
					// Procesa la transacción
					PaywallTransactionModel transaction = PaywallUtil.processPayment(groupId, providerName, paymentId);
					// Manda al usuario a la URL de error definida en el portlet
					result.addProperty("url", transaction.getErrorUrl());
				}
				else
				{
					result.addProperty("id", paymentId);
				}
				response.setContentType("application/json");
				printResponse(result.toString(), response);
			}
			else if ("execute-payment".equals(intent))
			{
				String paymentId = request.getParameter("paymentID");
				String payerId  = request.getParameter("payerID");
				String errorUrl  = request.getParameter("errorUrl");
				String transactionData = PaypalMgr.INSTANCE.executePayment(groupId, paymentId, payerId, dogTag.get(IterKeys.COOKIE_NAME_USER_ID), errorUrl);
				
				// Procesa la transacción
				PaywallTransactionModel transaction = PaywallUtil.processPayment(groupId, providerName, transactionData);
				JsonObject result = new JsonObject();
				if (transaction.isCompleted())
				{
					// Manda al usuario a la Url para refrescar sus derechos
					result.addProperty("url", "/user-portlet/refreshuserentitlements");
				}
				else
				{
					// Manda al usuario a la URL de error definida en el portlet
					result.addProperty("url", transaction.getErrorUrl());
				}
				response.setContentType("application/json");
				printResponse(result.toString(), response);
			}
		}
		catch (Throwable th)
		{
			log.error(th);
			
			if (th instanceof ServiceError)
			{
				ServiceError e = (ServiceError) th;
				String errorCode = e.getErrorCode();
				
				if (PaywallErrorKeys.PAYWALL_E_BAD_REQUEST.equals(errorCode))
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				else if (PaywallErrorKeys.PAYWALL_E_UNAUTHORIZED.equals(errorCode))
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				else
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			else
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	private void printResponse(String responseBody, HttpServletResponse response)
	{
		
	    PrintWriter out = null;
		try
		{
			out = response.getWriter();
			out.print(responseBody);
		}
		catch (IOException e)
		{
			log.error(e);
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
