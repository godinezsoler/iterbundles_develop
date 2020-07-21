package com.protecmedia.iter.user.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.protecmedia.iter.base.service.CaptchaLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.service.base.CaptchaFormLocalServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CaptchaFormLocalServiceImpl extends CaptchaFormLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(CaptchaFormLocalServiceImpl.class);
	
	/* Documentacion de Google: https://developers.google.com/recaptcha/docs/java	
	*  groupId:        IUdentificador del grupo 				 (group_.groupId)
	*  challengeValue: Valor de la pregunta/reto 				 (request.recaptcha_challenge_field)
	*  responseValue:  Valor de la entrada/respuesta del usuario (request.recaptcha_response_field)
	*  remoteAddress:  Direccion ip del usuario 				 (request.getRemoteAddr()) 
	*/		
	public boolean isValid(Long groupId, String responseValue, String remoteAddress) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		_log.trace("In CaptchaFormLocalServiceImpl.isValid");
		boolean valid = false;
		ErrorRaiser.throwIfNull(groupId);
		ErrorRaiser.throwIfNull(remoteAddress);
		final String privateKey = getPrivateKey(groupId);
		
		if (Validator.isNotNull(privateKey))
		{
			// Llama a la API de reCaptcha
			IterHttpClient httpClient = new IterHttpClient
				.Builder(IterHttpClient.Method.POST, "https://www.google.com/recaptcha/api/siteverify")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.payLoad(String.format("secret=%s&response=%s&remoteip=%s", privateKey, responseValue, remoteAddress))
				.build();
			String response = httpClient.connect();
			
			// Si la respuesta es correcta...
			if (httpClient.validResponse())
			{
				try
				{
					// Parsea la respuesta JSON
					JSONObject jsonResponse = JSONUtil.createJSONObject(response);
					// Recupera el virtualhost
					String virtualHost = LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
					// El captcha es válido si viene del dominio de sitio y la API lo ha validado
					boolean success = jsonResponse.getBoolean("success");
					boolean validHost = virtualHost.equals(jsonResponse.getString("hostname"));
					// TODO log
					valid = success && validHost;
				}
				catch (Throwable th)
				{
					_log.error(String.format("Error validating captcha. Unparseable response:\n%s", response));
					th.printStackTrace();
				}
			}
			else
			{
				_log.error(String.format("Error validating captcha. Response was: %d", httpClient.getResponseStatus()));
			}
		}
		else
		{
			_log.debug("Receibed captcha private key: null or empty");
		}
		
		_log.debug(new StringBuffer("Captcha returns: ").append(valid ? "true" : "false"));
		return valid;		
	}
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws ServiceError
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws DocumentException
	 */
	private String getPrivateKey(Long groupId) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
		
		if (Validator.isNull(groupId)){
			_log.debug("groupId is null");
		}
		ErrorRaiser.throwIfNull(groupId);	
		
		String privateKey = null;
		
		final String result = CaptchaLocalServiceUtil.getCaptcha(groupId);
		
		final Document d = SAXReaderUtil.read(result).getRootElement().getDocument();
		
		privateKey = XMLHelper.getTextValueOf(d, "/rs/row/@secretkey");
		if (Validator.isNull(privateKey)){
			_log.debug("privateKey is null");
		}
		ErrorRaiser.throwIfNull(privateKey);
		
		_log.debug(new StringBuffer("PrivateKey returned: ").append(privateKey));
		return privateKey;
	}
	
}