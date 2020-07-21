package com.protecmedia.iter.base.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.util.PortalUtil;

public class MailUtil
{
	private static Log _log = LogFactoryUtil.getLog(MailUtil.class);

	/**
	 * @param smtpNode
	 * @param subject
	 * @param body
	 * @param emails
	 * @param type 
	 *		{@value RecipientType.TO}
	 * 		{@value RecipientType.CC}
	 * 		{@value RecipientType.BCC}
	 * 
	 * @throws AddressException
	 * @throws MessagingException
	 * @throws ServiceError
	 */
	public static void sendEmail(Node smtpNode, String subject, String body,String mimeTypeCharset, List<String> emails, RecipientType type) throws AddressException, MessagingException, ServiceError, IOException
	{
		List<String> emailsTO = null;
		List<String> emailsCC = null;
		List<String> emailsCCO = null;
		if(type.equals(RecipientType.TO))
		{
			emailsTO = emails;
		}
		else if(type.equals(RecipientType.CC))
		{
			emailsCC = emails;
		}
		else if(type.equals(RecipientType.BCC))
		{
			emailsCCO = emails;
		}
		else
		{
			_log.error("invalid received type");
			ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_ITR_E_INVALID_EMAIL_RECIPIENT_TYPE_ZYX);
		}
		
		sendEmail(smtpNode, subject, body, mimeTypeCharset, emailsTO, emailsCC, emailsCCO);
	}
	
	public static void sendEmail(Node smtpNode, String subject, String body,String mimeTypeCharset, List<String> emailsTO, List<String> emailsCC, List<String> emailsCCO) throws AddressException, MessagingException, ServiceError, IOException
	{
		sendEmail(smtpNode, subject, body, mimeTypeCharset, emailsTO, emailsCC, emailsCCO, null);
	}

	public static void sendEmail(Node smtpNode, String subject, String body,String mimeTypeCharset, List<String> emailsTO, List<String> emailsCC, List<String> emailsCCO, List<Entry<String, InputStream>> attachments) throws AddressException, MessagingException, ServiceError, IOException
	{
		_log.trace(new StringBuffer().append("into MailUtil sendMail: send mail").toString());
		
		if(Validator.isNull(smtpNode)){
			_log.error("sendEmail smtpNode is null");
		}
		ErrorRaiser.throwIfNull(smtpNode, IterErrorKeys.XYZ_ITR_E_SMTP_SERVER_IS_NULL_ZYX);
		
		boolean SMTPEnabled = GetterUtil.getBoolean(XMLHelper.getTextValueOf(smtpNode, "@enabled", "false"));
		
		if(SMTPEnabled)
		{
			final String SMTPhost = XMLHelper.getTextValueOf(smtpNode, "@host", null);
			final String SMTPport = XMLHelper.getTextValueOf(smtpNode, "@port", null);
			final String SMTPtls = String.valueOf(GetterUtil.getBoolean(XMLHelper.getTextValueOf(smtpNode, "@tls"), false));
			final String SMTPauth = String.valueOf(GetterUtil.getBoolean(XMLHelper.getTextValueOf(smtpNode, "@auth"), false));
			final String SMTPusername = XMLHelper.getTextValueOf(smtpNode, "@username", null);
			final String SMTPpassword = XMLHelper.getTextValueOf(smtpNode, "@password", null);
			
			final String emailfrom = XMLHelper.getTextValueOf(smtpNode, "@emailfrom");
			
			sendEmail(SMTPhost, SMTPport, SMTPtls, SMTPauth, SMTPusername, SMTPpassword, emailfrom, subject, body, mimeTypeCharset, emailsTO, emailsCC, emailsCCO, attachments);
		}
		else
			_log.error("sendEmail smtpserver "+ XMLHelper.getTextValueOf(smtpNode, "@smtpserverid", null) +" is disabled");
	}
	
	//GENERICO
	
	//MailUtil.sendEmail("10.15.20.117", "25", "0", "0", null, null, "iterwebcms@protecmedia.com", "probando el mail inutil", "probando/testing", "text/html; charset=utf-8",  Arrays.asList(new String[]{"aruiz@protecmedia.com"}),  null , Arrays.asList(new String[]{"lmperez@protecmedia.com, jesteban@protecmedia.com"}, null);	
	public static void sendEmail(String host, String port, String tls, String auth, String username, String password, String emailfrom, String subject, String body, String mimeTypeCharset, List<String> emailsTO, List<String> emailsCC, List<String> emailsCCO, List<Entry<String, InputStream>> attachments) throws AddressException, MessagingException, ServiceError, IOException
	{
		Session session = getEmailSession(host, port, tls, auth, username, password);
		
		MimeMessage message = newMimeMessage(session, emailfrom, subject, emailsTO, emailsCC, emailsCCO);

		if(attachments == null || attachments.isEmpty()){
			setContent(session, message, body, mimeTypeCharset);
		}else{
			setContent(session, message, body, mimeTypeCharset, attachments);
		}
		Transport.send(message);
	}
		
	public static void setContent(Session session, MimeMessage message , String body , String mimeTypeCharset) throws MessagingException, ServiceError
	{
		message.setContent(body, mimeTypeCharset);
	}
	
	public static void setContent(Session session, MimeMessage message , String body , String mimeTypeCharset, List<Entry<String, InputStream>> attachments)  throws MessagingException, ServiceError, IOException
	{
		Multipart mp = new MimeMultipart();

		if(body== null)	body = "";
			
		MimeBodyPart mbpBody = new MimeBodyPart();
		mbpBody.setContent(body, mimeTypeCharset);
		mp.addBodyPart(mbpBody);
		
		for (Entry<String,InputStream> entryAttach :attachments) 
		{
			MimeBodyPart mbp = new MimeBodyPart();
			String filename = entryAttach.getKey();
			DataSource ds = new ByteArrayDataSource(IOUtils.toByteArray(entryAttach.getValue()), MimeTypeTools.getMimeTypeFileName(filename));
			mbp.setDataHandler(new DataHandler(ds));
			mbp.setFileName(filename);
			mp.addBodyPart(mbp);
		}
		message.setContent(mp);
	}
	
	//MimeMessage
	
	/**
	 * Construye el email básico a partir de los campos del formulario, sin cuerpo
	 * 
	 * @param session
	 * @param emailfrom
	 * @param textSubject
	 * @param emailsTo
	 * @param emailsCC
	 * @param emailsCCO
	 * @return
	 * @throws AddressException
	 * @throws MessagingException
	 * @throws ServiceError 
	 */
	private static MimeMessage newMimeMessage(Session session, String emailfrom, String textSubject, List<String> emailsTo, List<String> emailsCC, List<String> emailsCCO) throws AddressException, MessagingException, ServiceError{
		_log.trace(new StringBuffer().append("into MailUtil newMimeMessage: create generic MimeMessage").toString());

		MimeMessage message = new MimeMessage(session);
		
		if(Validator.isNull(emailfrom))
			_log.error("newMimeMessage recipient is empty");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(emailfrom), IterErrorKeys.XYZ_ITR_E_EMAIL_FROM_IS_EMPTY_ZYX );

		message.setFrom(new InternetAddress(emailfrom));
		
		if(Validator.isNotNull(textSubject)&& !textSubject.isEmpty())
		{
			message.setSubject(textSubject);
		}
		
		if( Validator.isNull(emailsTo) && Validator.isNull(emailsCC) && Validator.isNull(emailsCCO) )
			_log.error("newMimeMessage recipient is empty");
		ErrorRaiser.throwIfFalse( !(Validator.isNull(emailsTo) && Validator.isNull(emailsCC) && Validator.isNull(emailsCCO)), IterErrorKeys.XYZ_ITR_E_EMAIL_RECIPIENTS_LIST_IS_EMPTY_ZYX );
		
		if(Validator.isNotNull(emailsTo))
		{
			for (String recipient : emailsTo) 
				message.addRecipient(RecipientType.TO, new InternetAddress(recipient.trim()));
		}
		if(Validator.isNotNull(emailsCC))
		{
			for (String recipient : emailsCC) 
				message.addRecipient(RecipientType.CC, new InternetAddress(recipient.trim()));
		}
		if(Validator.isNotNull(emailsCCO))
		{
			for (String recipient : emailsCCO) 
				message.addRecipient(RecipientType.BCC, new InternetAddress(recipient.trim()));
		}
		
		if(_log.isTraceEnabled())
		{
			_log.trace(new StringBuffer()
				.append("\n\n MailUtil Creating mail...\n")
				.append("\nfrom: ").append(emailfrom)
				.append("\nto: ").append(Arrays.toString(message.getRecipients(RecipientType.TO)))
				.append("\ncc: ").append(Arrays.toString(message.getRecipients(RecipientType.CC)))
				.append("\nbcc: ").append(Arrays.toString(message.getRecipients(RecipientType.BCC)))
				.toString()
			);
		}

		return message;
	}
	
	//SMTP
	
	private static Session getEmailSession(final String host, final String port, final String tls, final String auth, final String username, final String password) throws ServiceError
	{
		_log.trace(new StringBuffer().append("into MailUtil getEmailSession: opens session whith SMTPServer").toString());
		
		Properties props = getEmailSMTPSessionProperties(host, port, tls, auth);

		Session session = null;
		
		if(GetterUtil.getBoolean(auth))
		{
			
			session = Session.getInstance(props, new javax.mail.Authenticator() {
													protected PasswordAuthentication getPasswordAuthentication() {
														return new PasswordAuthentication(username, password);}});
		}
		else
		{
			session = Session.getInstance(props);
		}
		
		return session;
	}
		
	private static Properties getEmailSMTPSessionProperties(final String host, final String port, final String tls, final String auth) throws ServiceError{
		
		if (Validator.isNull(port))
		{
			_log.error("getEmailSMTPSessionProperties port is null");
		}
		ErrorRaiser.throwIfFalse( Validator.isNotNull(port), IterErrorKeys.XYZ_ITR_E_EMAIL_SMTP_PORT_IS_EMPTY_ZYX );
		
		if (Validator.isNull(host))
		{
			_log.error("getEmailSMTPSessionProperties host is null");
		}
		ErrorRaiser.throwIfFalse( Validator.isNotNull(host), IterErrorKeys.XYZ_ITR_E_EMAIL_SMTP_HOST_IS_EMPTY_ZYX );
		
		if (Validator.isNull(tls))
		{
			_log.error("getEmailSMTPSessionProperties tls is null");
		}
		ErrorRaiser.throwIfFalse( Validator.isNotNull(tls), IterErrorKeys.XYZ_ITR_E_EMAIL_SMTP_TLS_IS_EMPTY_ZYX );
		
		if (Validator.isNull(auth))
		{
			_log.error("getEmailSMTPSessionProperties auth is null");
		}
		ErrorRaiser.throwIfFalse( Validator.isNotNull(auth), IterErrorKeys.XYZ_ITR_E_EMAIL_SMTP_AUTH_IS_EMPTY_ZYX );
		
		if(_log.isTraceEnabled())
		{
			_log.trace(new StringBuffer()
				.append("\n\n MailUtil creating session SMTP...\n") 	 
				.append("\nhost: ").append(host) 		  
				.append("\nport: ").append(port)
				.append("\ntls: ").append(tls)
				.append("\nauth: ").append(auth)
				.toString()
			);
		}
		
		String timeout = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_MAIL_SMTP_TIMEOUT), "0");
		String connectiontimeout = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_MAIL_SMTP_CONNECTIONTIMEOUT), "0");
		
		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.starttls.enable", tls);
		props.put("mail.smtp.auth", auth);
		
		if(Validator.isNotNull(connectiontimeout) && Integer.valueOf(connectiontimeout).intValue() > 0)
		{ 
			props.put("mail.smtp.connectiontimeout", connectiontimeout);
		}
		
		if(Validator.isNotNull(timeout) && Integer.valueOf(timeout).intValue() > 0)
		{
			props.put("mail.smtp.timeout", timeout);
		}
		
		return props;
	}
	
	public static boolean isValidEmailAddress(String email)
	{
	   boolean retVal = true;
	   
	   try 
	   {
	      InternetAddress emailAddr = new InternetAddress(email);
	      emailAddr.validate();
	   }
	   catch (AddressException ex) 
	   {
		   retVal = false;
		   _log.debug("Invalid email address: " + email);
	   }
	   
	   return retVal;
	}
	
}




