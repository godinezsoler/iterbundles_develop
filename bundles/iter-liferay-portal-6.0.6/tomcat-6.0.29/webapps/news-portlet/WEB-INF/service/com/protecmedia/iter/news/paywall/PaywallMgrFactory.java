package com.protecmedia.iter.news.paywall;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.news.paywall.provider.IPaywallProvider;
import com.protecmedia.iter.news.paywall.provider.PaypalMgr;
import com.protecmedia.iter.news.paywall.provider.RedsysMgr;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.paywall.utils.PaywallGateway;

public class PaywallMgrFactory
{
	private static Log log = LogFactoryUtil.getLog(PaywallMgrFactory.class);
	
	/**
	 * Recupera la instancia del manejador de la pasarela de pago a partir de su nombre.
	 * @param gatewayName El nombre del proveedor de la pasarela de pago.
	 * @return El manejador de la pasarela de pago. 
	 * @throws ServiceError Si se especifica un proveedor no implementado.
	 */
	public static IPaywallProvider getProvider(String gatewayName) throws ServiceError
	{
		PaywallGateway gateway = PaywallGateway.valueOf(gatewayName.toUpperCase());
		return getProvider(gateway);
	}
	
	/**
	 * Recupera la instancia del manejador de la pasarela de pago indicada.
	 * @param gateway El proveedor de la pasarela de pago.
	 * @return El manejador de la pasarela de pago. 
	 * @throws ServiceError Si se especifica un proveedor no implementado.
	 */
	public static IPaywallProvider getProvider(PaywallGateway gateway) throws ServiceError
	{
		IPaywallProvider provider = null;	
		switch (gateway)
		{
			case PAYPAL:
				provider = PaypalMgr.INSTANCE;
				break;
			case REDSYS:
				provider = RedsysMgr.INSTANCE;
				break;
			default:
				ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_PROVIDER_NOT_SUPPORTED);
				break;
		}
		return provider;
	}
	
	/**
	 * Recarga la configuración en memoria de la pasarela de pago para el grupo indicado. 
	 * @param groupId El identificador del grupo.
	 * @param gateway El nombre de la pasarela de pago.
	 */
	public static void reloadConfig(String groupId, String gateway)
	{
		try
		{
			getProvider(gateway).loadConfig(Long.valueOf(groupId));
		}
		catch (ServiceError e)
		{
			log.error(e);
		}
	}
}
