/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.portlet;

import javax.mail.internet.InternetAddress;

import java.io.IOException;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.mail.service.MailServiceUtil;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.Counters;
import com.protecmedia.iter.news.service.CountersLocalServiceUtil;

/**
 * Portlet implementation class SocialNetworkPortlet
 */
public class ContentToolsPortlet extends MVCPortlet {
	
	private static ResourceBundle rb =  ResourceBundle.getBundle("content.Language");
	
	@Override
	public void processAction(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
		
		super.processAction(actionRequest, actionResponse);
		
		String action = (String) actionRequest.getParameter("action");
		
		if ("send_Mail".equals(action)) 
		{
			UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(actionRequest);
			String destinatario = ParamUtil.getString(uploadRequest, "destinatario", "");
			String origen = ParamUtil.getString(uploadRequest, "remitente", "");
			String comentarios = ParamUtil.getString(uploadRequest, "comentarios", "");
			
			String shortURL = (String) actionRequest.getParameter("shortURL");
			String articleId = (String) actionRequest.getParameter("articleId");
			long scopeGroupId = Long.parseLong(actionRequest.getParameter("scopeGroupId"));
			
			actionResponse.setRenderParameter("shortURL", shortURL);
			generarYEnviarCorreo(shortURL, destinatario, origen, comentarios, 
								 actionRequest, actionResponse,
								 articleId, scopeGroupId);
		}
	}
	
	@Override
	public void init() throws PortletException 
	{	
		try 
		{
			super.init();
			sendMail = getInitParameter("sendMail-jsp");
		} 
		catch (Exception e) 
		{
			
		}
	}
	
	@Override
	public void doView(RenderRequest renderRequest,RenderResponse renderResponse) throws IOException, PortletException 
	{
		String view = (String) renderRequest.getParameter("view");
		String close = (String) renderRequest.getParameter("close");
		String shortURL = (String) renderRequest.getParameter("shortURL");
		renderRequest.setAttribute("close", "");
		renderRequest.setAttribute("view", "");
		renderRequest.setAttribute("shortURL", shortURL);
		
		WindowState w = renderRequest.getWindowState();

		if(close != null && close.equals("true") && w.toString().equals("pop_up"))
		{
			renderRequest.setAttribute("close", close);
			SessionMessages.add(renderRequest, "content-tools-send-mail-success");
			include(sendMail, renderRequest, renderResponse);
		}else if ((view != null && view.equals("send-mail") && w.toString().equals("pop_up"))){
			include(sendMail, renderRequest, renderResponse);
		} else {
			super.doView(renderRequest, renderResponse);
		}
	}
	
	public static void generarYEnviarCorreo(String link, String destinatario, 
											String remitente, String comentarios, 
											ActionRequest actionRequest, ActionResponse actionResponse,
											String articleId, long scopeGroupId) 
	{
		try {
			if ((link != null && !link.isEmpty()) && 
				(destinatario != null && !destinatario.isEmpty()) && 
				(remitente != null && !remitente.isEmpty()))
			{
					
				if(checkEmailSyntax(remitente) && checkEmailSyntax(destinatario)) 
				{	
				
					rb = ResourceBundle.getBundle("content.Language", actionRequest.getLocale());
					
					String subject = remitente + " " +  rb.getString("content-tools-someone-mail");
					
					String newline = System.getProperty("line.separator");
					
					String message = remitente + " " + rb.getString("content-tools-someone-article") + ":<br><br>" + 
									 link + newline + "<br><br><b>" + 
									 rb.getString("content-tools-send-mail-comments") + ":</b><br><br>" + 
									 comentarios;
					
					enviarCorreoADestinatario(remitente, subject, message, 
											  destinatario, link, 
											  actionRequest, actionResponse,
											  articleId, scopeGroupId);
					
					actionResponse.setRenderParameter("close", "true");
					actionResponse.setRenderParameter("view", "");
				}else{
					// direcciones de correo inválidas
					actionResponse.setRenderParameter("close", "");
					actionResponse.setRenderParameter("view", "send-mail");
					SessionErrors.add(actionRequest, "content-tools-send-mail-failed-address");
				}
			} else {
				// campos vacíos
				actionResponse.setRenderParameter("close", "");
				actionResponse.setRenderParameter("view", "send-mail");
				SessionErrors.add(actionRequest, "content-tools-send-mail-empty-fields");
			}
		} catch (Exception e){
			// no se pudo enviar el email asegurese de que el mail del
			// destinatario esta bien
			actionResponse.setRenderParameter("close", "");
			actionResponse.setRenderParameter("view", "send-mail");
			SessionErrors.add(actionRequest, "content-tools-send-mail-error");
		}			
	}
	
	public static void enviarCorreoADestinatario(String remitente, String subject, String message, 
												 String destinatario, String link, 
												 ActionRequest actionRequest, ActionResponse actionResponse,
												 String articleId, long scopeGroupId) throws Exception 
	{
		InternetAddress sender = new InternetAddress(remitente, remitente);
		InternetAddress receiver = new InternetAddress(destinatario, null);
		MailMessage mailMessage = new MailMessage(sender, receiver, subject, message, true);
		MailServiceUtil.sendEmail(mailMessage);
		
		if(Validator.isNotNull(articleId) && scopeGroupId > -1)
		{
			Counters counter = CountersLocalServiceUtil.findByCountersArticleGroupOperationFinder(articleId, scopeGroupId, IterKeys.OPERATION_SENT);
			if (counter != null) 
			{
				// Ya ha sido enviado
				long currentNumCounts = counter.getCounter();
				counter.setCounter(currentNumCounts + 1);
				counter.setDate(new Date());
				counter = CountersLocalServiceUtil.updateCounters(counter, false);
			}
			else 
			{
				// La primera vez que se envía
				long id = CounterLocalServiceUtil.increment();
				counter = CountersLocalServiceUtil.createCounters(id);
				counter.setContentId(articleId);
				counter.setGroupId(scopeGroupId);
				counter.setCounter(1);
				counter.setCounterLast(0);
				counter.setOperation(IterKeys.OPERATION_SENT);
				counter.setDate(new Date());
				counter = CountersLocalServiceUtil.updateCounters(counter, false);
			}
		}
	}
	
	private static boolean checkEmailSyntax(String email)
	{
		boolean result = true;
		Pattern rfc2822 = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
		if (!rfc2822.matcher(email).matches()) {
		    result = false;
		}
		return result;
	}
	
	protected String sendMail;

}
