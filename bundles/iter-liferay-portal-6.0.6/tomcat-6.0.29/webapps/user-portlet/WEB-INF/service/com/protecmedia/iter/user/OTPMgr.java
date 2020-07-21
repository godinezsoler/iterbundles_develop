package com.protecmedia.iter.user;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;


public class OTPMgr 
{
	private static Log _log = LogFactoryUtil.getLog(OTPMgr.class);
	
	public static final String OP_GENERATION 	= "generation";
	public static final String OP_VALIDATION 	= "validation";
	public static final String OP_SENDMSG 		= "sendmsg";
	
	/**
	 * 
	 * @param groupId
	 * @param payload
	 * @return
	 * @throws ServiceError 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public static void doGeneration(long groupId, String payload) throws PortalException, SystemException, ServiceError
	{
		callServer(	groupId, payload, 
				PropsKeys.ITER_OTP_GENERATION_SERVER_CONEXIONTIMEOUT, PropsKeys.ITER_OTP_GENERATION_SERVER_RESPONSETIMEOUT, PropsKeys.ITER_OTP_GENERATION_SERVER_URL, 
				IterErrorKeys.XYZ_E_OTP_GENERATION_SERVER_NOT_CONFIGURED_ZYX, IterErrorKeys.XYZ_E_OTP_GENERATION_HAS_FAILED_ZYX);
	}
	
	/**
	 * 
	 * @param groupId
	 * @param payload
	 * @return
	 * @throws ServiceError 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public static void doValidation(long groupId, String payload) throws PortalException, SystemException, ServiceError
	{
		callServer(	groupId, payload, 
				PropsKeys.ITER_OTP_VALIDATION_SERVER_CONEXIONTIMEOUT, PropsKeys.ITER_OTP_VALIDATION_SERVER_RESPONSETIMEOUT, PropsKeys.ITER_OTP_VALIDATION_SERVER_URL, 
				IterErrorKeys.XYZ_E_OTP_VALIDATION_SERVER_NOT_CONFIGURED_ZYX, IterErrorKeys.XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX);
	}
	
	/**
	 * 
	 * @param groupId
	 * @param payload
	 * @return
	 * @throws ServiceError 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public static void doSendMsg(long groupId, String payload) throws PortalException, SystemException, ServiceError
	{
		callServer(	groupId, payload, 
				PropsKeys.ITER_OTP_SENDMSG_SERVER_CONEXIONTIMEOUT, PropsKeys.ITER_OTP_SENDMSG_SERVER_RESPONSETIMEOUT, PropsKeys.ITER_OTP_SENDMSG_SERVER_URL, 
				IterErrorKeys.XYZ_E_OTP_SENDMSG_SERVER_NOT_CONFIGURED_ZYX, IterErrorKeys.XYZ_E_OTP_SENDMSG_HAS_FAILED_ZYX);
	}
	
	private static void callServer(	long groupId, String payload, 
									String connTimeoutTag, String respTimeoutTag, String serverURLTag, 
									String serverConfErrCode, String serverFailErrCode) throws ServiceError, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(payload), IterErrorKeys.XYZ_E_OTP_PAYLOAD_EMPTY_ZYX);
		
		// Recupera los tiempos de timeouts configurados para el grupo
		String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
		
		int conexiontimeout = GetterUtil.getInteger( PropsUtil.get(connTimeoutTag.concat(friendlyGroupURL)), 0 );
		if (conexiontimeout <= 0)
			conexiontimeout = GetterUtil.getInteger( PropsUtil.get(connTimeoutTag), 2000 );
		
		int responsetimeout = GetterUtil.getInteger( PropsUtil.get(respTimeoutTag.concat(friendlyGroupURL)), 0 );
		if (responsetimeout <= 0)
			responsetimeout = GetterUtil.getInteger( PropsUtil.get(friendlyGroupURL), 15000 );

		String serverUrl = PropsUtil.get(serverURLTag.concat(friendlyGroupURL));
		if (Validator.isNull(serverUrl))
		{
			serverUrl = PropsUtil.get(serverURLTag);
			ErrorRaiser.throwIfNull(serverUrl, serverConfErrCode);
		}

		// Realiza un POST contra el SSO con el mismo payload y cabecera original
		IterHttpClient iterHttpClient = new IterHttpClient.Builder(IterHttpClient.Method.POST, serverUrl)
														  .connectionTimeout(conexiontimeout)
														  .readTimeout(responsetimeout)
														  .payLoad(payload)
														  .header("Content-Type", "application/x-www-form-urlencoded")
														  .includeCodeInErrorResponse(true)
														  .build();
		String serviceResponse = iterHttpClient.connect();
		ErrorRaiser.throwIfFalse(iterHttpClient.validResponse(), serverFailErrCode, serviceResponse);
	}
}
