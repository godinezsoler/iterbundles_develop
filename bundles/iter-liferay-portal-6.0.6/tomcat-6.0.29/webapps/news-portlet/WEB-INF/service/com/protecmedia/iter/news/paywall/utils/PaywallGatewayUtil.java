package com.protecmedia.iter.news.paywall.utils;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.liferay.portal.kernel.cluster.ClusterTools;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.news.paywall.PaywallMgrFactory;

public class PaywallGatewayUtil
{
	private static Log log = LogFactoryUtil.getLog(PaywallGatewayUtil.class);
	
	private static final String XPATH_CONFIG = "/rs/row";
	private static final String XPATH_CONFIG_NAME = "@name";
	private static final String XPATH_CONFIG_ENABLE = "@enabled";
	private static final String XPATH_CONFIG_CONFIG = "config";
	
	private static final String SQL_GET_GATEWAYS = "SELECT name FROM iterpaywall_payment_gateway";
	
	private static final String SQL_GET_ENABLED_GATEWAYS = new StringBuilder(SQL_GET_GATEWAYS)
	.append("\n INNER JOIN iterpaywall_payment_gateway_config gc \n")
	.append("ON iterpaywall_payment_gateway.id = gc.gatewayid    \n")
	.append("WHERE gc.groupid = %s AND gc.enabled")
	.toString();
	
	private static final String SQL_GET_CONFIGS = new StringBuilder()
	.append("SELECT g.name, gc.enabled, gc.config FROM iterpaywall_payment_gateway_config gc    \n")
	.append("INNER JOIN iterpaywall_payment_gateway g ON g.id = gc.gatewayid WHERE groupid = %s \n")
	.toString();
	
	private static final String SQL_GET_CONFIG = new StringBuilder(SQL_GET_CONFIGS).append(" AND g.name = '%s'").toString();
	
	private static final String SQL_SET_CONFIG = new StringBuilder()
	.append("INSERT INTO iterpaywall_payment_gateway_config (groupid, gatewayid, enabled, config)  \n")
	.append("VALUES (%s, (SELECT id FROM iterpaywall_payment_gateway WHERE name = '%s'), %s, '%s') \n")
	.append("ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), config = VALUES(config)"              )
	.toString();
	
	private static final String SQL_SET_ENABLED = new StringBuilder()
	.append("UPDATE iterpaywall_payment_gateway_config SET enabled = %b                       \n")
	.append("WHERE groupid = %s                                                               \n")
	.append("  AND gatewayid = (SELECT id FROM iterpaywall_payment_gateway WHERE name = '%s')"   )
	.toString();
	
	/**
	 * Retorna las pasarelas de pago soportadas por ITER.
	 * @return
	 */
	public static String getGateways()
	{
		return getGateways(null);
	}
	
	/**
	 * Retorna las pasarelas de pago configuradas y activadas para el grupo indicado.
	 * @param groupId
	 * @return
	 */
	public static String getGateways(String groupId)
	{
		JsonObject result = new JsonObject();
		JsonArray gateways = new JsonArray();
		
		String sql = Validator.isNull(groupId) ? SQL_GET_GATEWAYS : String.format(SQL_GET_ENABLED_GATEWAYS, groupId);
		try
		{
			Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
			String[] rawGateways = XMLHelper.getStringValues(d.selectNodes(XPATH_CONFIG), XPATH_CONFIG_NAME);
			for (String gateway : rawGateways)
			{
				gateways.add(new JsonPrimitive(gateway));
			}
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		
		result.add("gateways", gateways);
		return result.toString();
	}
	
	/**
	 * Retorna las configuraciones de las pasarelas de pago del grupo indicado.
	 * @param groupId
	 * @return
	 * @throws ServiceError 
	 */
	public static String getConfigs(String groupId) throws ServiceError
	{
		return getConfig(groupId, null);
	}
	
	/**
	 * Retorna la configuración de una pasarela de pago para el grupo indicado.
	 * @param groupId
	 * @param gateway
	 * @return
	 * @throws ServiceError 
	 */
	public static String getConfig(String groupId, String gateway) throws ServiceError
	{
		JsonObject configs = new JsonObject();
		String sql = Validator.isNull(gateway) ? String.format(SQL_GET_CONFIGS, groupId) : String.format(SQL_GET_CONFIG, groupId, gateway);
		
		try
		{
			Document d = PortalLocalServiceUtil.executeQueryAsDom(sql, new String[]{"config"});
			List<Node> rawConfigs = d.getRootElement().selectNodes(XPATH_CONFIG);
			
			for (Node rawConfig : rawConfigs)
			{
				boolean enabled = GetterUtil.getBoolean(XMLHelper.getStringValueOf(rawConfig, XPATH_CONFIG_ENABLE), false);
				
				JsonObject config = new JsonObject();
				config.addProperty("enabled", enabled);
				config.add("config", new JsonParser().parse(XMLHelper.getStringValueOf(rawConfig, XPATH_CONFIG_CONFIG)));
				configs.add(XMLHelper.getStringValueOf(rawConfig, XPATH_CONFIG_NAME), config);
			}
		}
		catch (Throwable th)
		{
			log.error(th);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		
		return configs.toString();
	}
	
	/**
	 * Establece la configuración de una pasarela de pago para un grupo.
	 * @param groupId
	 * @param gatewayName
	 * @param config
	 * @return
	 * @throws ServiceError
	 */
	public static String setConfig(String groupId, String gatewayName, boolean enabled, String config) throws ServiceError
	{
		JsonObject jsonConfig = null;
		// Comprueba que la entrada sea un objeto JSON
		try
		{
			jsonConfig = new JsonParser().parse(config).getAsJsonObject();
		}
		catch (Throwable th)
		{
			log.error(th);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		// Comprueba que están todos los parámetros obligatorios de la pasarela de pago
		if (PaywallMgrFactory.getProvider(gatewayName).validateConfiguration(jsonConfig))
		{
			try
			{
				// Guarda la configuración
				PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_SET_CONFIG, groupId, gatewayName, enabled, StringEscapeUtils.escapeSql(config)));
			}
			catch (Throwable th)
			{
				log.error(th);
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			}
			
			// Recarga la configuración en todos los Tomcats
			reloadConfig(groupId, gatewayName);
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		return getConfig(groupId, gatewayName);
	}
	
	/**
	 * Habilita o dehsabilita una pasarela de pago para un grupo.
	 * @param groupId
	 * @param gateway
	 * @throws ServiceError 
	 */
	public static void enableGateway(String groupId, String gateway, boolean enabled) throws ServiceError
	{	
		// Actualiza el estado
		String sql = String.format(SQL_SET_ENABLED, enabled, groupId, gateway);
		try
		{
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
		catch (Throwable th)
		{
			log.error(th);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
	}
	
	/**
	 * Recarga la configuración en todos los tomcats
	 * @throws ServiceError 
	 */
	public static void reloadConfig(String groupId, String gateway) throws ServiceError
	{
		try
		{
			ClusterTools.notifyCluster(true, "com.protecmedia.iter.news.paywall.PaywallMgrFactory", "reloadConfig", new Object[] {groupId, gateway});
		}
		catch (Throwable th)
		{
			throw ErrorRaiser.toServiceError(th);
		}
	}
}
