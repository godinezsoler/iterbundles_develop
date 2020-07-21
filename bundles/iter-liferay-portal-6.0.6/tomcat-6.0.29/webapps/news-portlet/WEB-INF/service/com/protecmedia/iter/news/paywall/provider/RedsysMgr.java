package com.protecmedia.iter.news.paywall.provider;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import sis.redsys.api.ApiMacSha256;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.protecmedia.iter.news.paywall.model.PaywallProductModel;
import com.protecmedia.iter.news.paywall.model.PaywallTransactionModel;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.paywall.utils.PaywallGateway;

public enum RedsysMgr implements IPaywallProvider
{
	INSTANCE;
	private Map<Long, Map<String,String>> configs;
	
	private RedsysMgr()
	{
		configs = new HashMap<Long, Map<String,String>>();
	};
	
	private static Log log = LogFactoryUtil.getLog(RedsysMgr.class);
	
	private static final String MERCHANT_CODE = "Ds_Merchant_MerchantCode";
	private static final String MERCHANT_TERMINAL = "Ds_Merchant_Terminal";
	private static final String SECRET_KEY = "Secret_Key";
	private static final String BTN_LBL = "ButtonLabel";
	
	private static final String SIGNATURE_VERSION = "Ds_SignatureVersion";
	private static final String SIGNATURE = "Ds_Signature";
	private static final String MERCHANT_PARAMETERS = "Ds_MerchantParameters";
	
	private static final String SQL_GET_CONFIG = new StringBuilder()
	.append("SELECT gc.config FROM iterpaywall_payment_gateway_config gc \n")
	.append("INNER JOIN iterpaywall_payment_gateway g ON g.id = gc.gatewayid \n")
	.append("WHERE groupid = %d AND g.name = 'redsys'")
	.toString();
	
	private static final String XPATH_CONFIG = "/rs/row/@config";
	
	public Map<String, String> getConfig(long groupId) throws ServiceError
	{
		// Busca en caché la configuración para el grupo
		Map<String,String> config = null;
		
		// Si no tiene, la carga
		if ((config = configs.get(groupId)) == null)
		{
			loadConfig(groupId);
			config = configs.get(groupId);
		}
		
		return config;
	}
	
	@Override
	public void loadConfig(long groupId) throws ServiceError
	{
		try
		{
			Map<String,String> config = new HashMap<String,String>();
			Document d = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_CONFIG, groupId));
			String configData = XMLHelper.getStringValueOf(d, XPATH_CONFIG);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(configData), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			
			JsonObject jsonConfig = new JsonParser().parse(configData).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : jsonConfig.entrySet())
			{
				config.put(entry.getKey(), entry.getValue().getAsString());	
			}

			ErrorRaiser.throwIfFalse(Validator.isNotNull(config.get(MERCHANT_CODE)), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(config.get(MERCHANT_TERMINAL)), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(config.get(SECRET_KEY)), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			
			configs.put(groupId, config);
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
		}
	}
	
	
	
	DateFormat redsysDateFormat = new SimpleDateFormat("dd/MM/yyyyHH:mm");
	
	@Override
	public PaywallTransactionModel getTransactionDetails(long groupId, String transactionData) throws ServiceError
	{
		PaywallTransactionModel transaction = null;
		
		Map<String, String> config = getConfig(groupId);
		
		// Parámetros de la URL cuando tras la devolución del control por parte del TPV
		Map<String, String> params = IterHttpClient.Util.deserializeQueryString(transactionData);
		ErrorRaiser.throwIfNull(params, PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);
		String signatureVersion = params.get(SIGNATURE_VERSION);
		String signature = params.get(SIGNATURE);
		String merchantParameters = params.get(MERCHANT_PARAMETERS);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(signatureVersion) && Validator.isNotNull(signature) && Validator.isNotNull(merchantParameters), PaywallErrorKeys.PAYWALL_E_BAD_REQUEST);
		
		try
		{
			// Obtiene los parámetros
			ApiMacSha256 apiMacSha256 = new ApiMacSha256();
			String decodec = URLDecoder.decode(apiMacSha256.decodeMerchantParameters(merchantParameters), StringPool.UTF8);
			
			// Comprueba la firma
			apiMacSha256.createMerchantSignatureNotif(config.get(SECRET_KEY), merchantParameters);
			
			// Crea la transacción
			JsonObject jsonTransactionData = new JsonParser().parse(decodec).getAsJsonObject();
			
			String order = jsonTransactionData.get("Ds_Order").getAsString();
			Date transactionDate = redsysDateFormat.parse(jsonTransactionData.get("Ds_Date").getAsString() + jsonTransactionData.get("Ds_Hour").getAsString());
			String response = jsonTransactionData.get("Ds_Response").getAsString();
			BigDecimal amount = jsonTransactionData.get("Ds_Amount").getAsBigDecimal().divide(new BigDecimal(100));
			String encodedIterData = StringEscapeUtils.unescapeHtml(jsonTransactionData.get("Ds_MerchantData").getAsString());
			Map<String, String> iterData = IterHttpClient.Util.deserializeQueryString(encodedIterData);
			
			transaction = new PaywallTransactionModel.Builder(groupId, PaywallGateway.REDSYS, order, decodec)
			.userid(iterData.get("userid"))
			.productid(Long.valueOf(iterData.get("productid")))
			.date(transactionDate)
			.amount(amount)
			.status(response)
			.completed("0000".equals(response))
			.errorUrl(iterData.get("errorurl"))
			.build();
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_TRANSACTION_NOT_PROCESSED);
		}
		
		return transaction;
	}
	
	@Override
	public PaywallTransactionModel getTransacctionFromWebhook(long groupId, HttpServletRequest request) throws ServiceError
	{
		String version = request.getParameter(SIGNATURE_VERSION);
		String params = request.getParameter(MERCHANT_PARAMETERS);
		String signature = request.getParameter(SIGNATURE);
		
		String transactionData = String.format("%s=%s&%s=%s&%s=%s&", SIGNATURE_VERSION, version, MERCHANT_PARAMETERS, params, SIGNATURE, signature);
		
		return getTransactionDetails(groupId, transactionData);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// BOTÓN DE PAGO
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final String REDSYS_FORM_TEMPLATE = new StringBuilder()
	.append("<?php                                                                                   \n")
	.append("    include_once $_SERVER['DOCUMENT_ROOT'] . '/apiRedsys.php';                          \n")
	.append("                                                                                        \n")
	.append("    if(!function_exists('generateOrder'))                                               \n")
	.append("    {                                                                                   \n")
	.append("        function generateOrder ()                                                       \n")
	.append("        {                                                                               \n")
	.append("            $uuid = uniqid();                                                           \n")
	.append("            $fnvPrime = 16777619;                                                       \n")
	.append("            $hash = 2166136261;                                                         \n")
	.append("                                                                                        \n")
	.append("            for ($i = 0, $c = strlen($uuid); $i < $c; $i++)                             \n")
	.append("            {                                                                           \n")
	.append("                $charCode = ord (substr($uuid, $i, 1));                                 \n")
	.append("                $hash = ($hash * $fnvPrime);                                            \n")
	.append("                $hash = $hash ^ $charCode;                                              \n")
	.append("            }                                                                           \n")
	.append("                                                                                        \n")
	.append("            return ($hash & 0x00000000ffffffff);                                        \n")
	.append("        }                                                                               \n")
	.append("    }                                                                                   \n")
	.append("                                                                                        \n")
	.append("    $button_label = '%s';                                                               \n")
	.append("    $merchantData = 'userid=' . getenv('ITER_USER_ID') . '&productid=%s&errorurl=%s';   \n")
	.append("                                                                                        \n")
	.append("    $redsys = new RedsysAPI;                                                            \n")
	.append("    $redsys->setParameter('DS_MERCHANT_AMOUNT', '%s');                                  \n")
	.append("    $redsys->setParameter('DS_MERCHANT_ORDER', generateOrder());                        \n")
	.append("    $redsys->setParameter('DS_MERCHANT_MERCHANTCODE', '%s');                            \n")
	.append("    $redsys->setParameter('DS_MERCHANT_CURRENCY', '%s');                                \n")
	.append("    $redsys->setParameter('DS_MERCHANT_TRANSACTIONTYPE', '%s');                         \n")
	.append("    $redsys->setParameter('DS_MERCHANT_TERMINAL', '%s');                                \n")
	.append("    $redsys->setParameter('DS_MERCHANT_MERCHANTURL', '%s');                             \n")
	.append("    $redsys->setParameter('DS_MERCHANT_URLOK', '%s');                                   \n")
	.append("    $redsys->setParameter('DS_MERCHANT_URLKO', '%s');                                   \n")
	.append("    $redsys->setParameter('DS_MERCHANT_MERCHANTDATA', $merchantData);                   \n")
	.append("                                                                                        \n")
	.append("    $params = $redsys->createMerchantParameters();                                      \n")
	.append("    $signature = $redsys->createMerchantSignature('%s');                                \n")
	.append("?>                                                                                      \n")
	.append("                                                                                        \n")
	.append("<form name='from' action='%s' method='POST'>                                            \n")
	.append("    <input type='hidden' name='Ds_SignatureVersion' value='HMAC_SHA256_V1' />           \n")
	.append("    <input type='hidden' name='Ds_MerchantParameters' value='<?php echo $params; ?>' /> \n")
	.append("    <input type='hidden' name='Ds_Signature' value='<?php echo $signature; ?>' />       \n")
	.append("    <input type='submit' value='<?php echo $button_label; ?>' />                        \n")
	.append("</form>                                                                                 \n")
	.toString();
	
	private static final String REDSYS_ENDPOINT_TEST = "https://sis-t.redsys.es:25443/sis/realizarPago";
	private static final String REDSYS_ENDPOINT_LIVE = "https://sis.redsys.es/sis/realizarPago";
	private static final String DS_MERCHANT_URLOKKO = "%s/news-portlet/paymentfulfillment/redsys";
	private static final String DS_MERCHANT_MERCHANTURL = "%s/news-portlet/paymentnotification/redsys";
	private static final String REDSYS_TRANSACTIONTYPE = "0";
	
	@Override
	public String getPaymentButtonCode(PaywallProductModel product) throws ServiceError
	{
		String code = StringPool.BLANK;
		
		// Comprueba que le producto esté configurado para redsýs
		if (isProductConfiguredForRedsys(product))
		{
			long groupId = product.getGroupId();
			
			try
			{
				Map<String, String> config = getConfig(groupId);
				
				String redsysUrl = GetterUtil.getBoolean(config.get("SandboxMode"), true) ? REDSYS_ENDPOINT_TEST : REDSYS_ENDPOINT_LIVE;
				String merchantCode = config.get(MERCHANT_CODE);
				String terminal = config.get(MERCHANT_TERMINAL);
				String secretKey = config.get(SECRET_KEY);
				String buttonLabel = config.get(BTN_LBL);
				
				String fulfillmentUrl = String.format(DS_MERCHANT_URLOKKO, IterURLUtil.getIterHost());
				String notificationUrl = String.format(DS_MERCHANT_MERCHANTURL, IterURLUtil.getIterHost());
				
				code = String.format(REDSYS_FORM_TEMPLATE,
					buttonLabel,
					product.id(),
					product.getErrorPageFriendlyUrl(),
					product.get("price").toString().replace(StringPool.PERIOD, StringPool.BLANK),
					merchantCode,
					product.get("currencynumeric"),
					REDSYS_TRANSACTIONTYPE,
					terminal,
					notificationUrl,
					fulfillmentUrl,
					fulfillmentUrl,
					secretKey,
					redsysUrl
				);
			}
			catch (Throwable th)
			{
				log.error(th);
			}
		}
		else
		{
			log.error("Paywall product not configured for Redsys");
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PRODUCT_NOT_CONFIGURED);
		}
		
		return code;
	}

	/**
	 * Para que se pueda crear un botón de redsýs, el producto tiene que tener configurado urlname, precio y moneda, además de un periodo
	 * de validez para poder actualizar los derechos del usuario tras la compra.
	 * @return
	 */
	private boolean isProductConfiguredForRedsys(PaywallProductModel product)
	{
		return Validator.isNotNull(product.get("urlname"))  && Validator.isNotNull(product.get("price"))
			&& Validator.isNotNull(product.get("currencynumeric")) && Validator.isNotNull(product.get("validity"));
	}

	@Override
	public boolean validateConfiguration(JsonObject configuration)
	{
		return Validator.isNotNull(configuration.get(MERCHANT_CODE))
		    && Validator.isNotNull(configuration.get(MERCHANT_TERMINAL))
		    && Validator.isNotNull(configuration.get(SECRET_KEY));
	}
}
