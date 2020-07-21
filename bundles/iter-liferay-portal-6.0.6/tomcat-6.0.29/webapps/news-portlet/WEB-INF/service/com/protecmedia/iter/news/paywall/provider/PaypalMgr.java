package com.protecmedia.iter.news.paywall.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.news.paywall.model.PaywallProductModel;
import com.protecmedia.iter.news.paywall.model.PaywallTransactionModel;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.paywall.utils.PaywallGateway;
import com.protecmedia.iter.news.service.ProductLocalServiceUtil;

public enum PaypalMgr implements IPaywallProvider
{
	INSTANCE;
	private Map<Long, Map<String,String>> configs;
	private Map<Long, String> tokens;
	
	private PaypalMgr()
	{
		configs = new HashMap<Long, Map<String,String>>();
		tokens = new HashMap<Long, String>();
	};
	
	private static Log log = LogFactoryUtil.getLog(PaypalMgr.class);
	
	private static final String CONF_SNDB = "SandboxMode";
	private static final String CONF_CLIENTID = "ClientId";
	private static final String CONF_SECRET = "Secret";
	
	private static final String API_URL_SANDBOX = "api.sandbox.paypal.com";
	private static final String API_URL_LIVE = "api.paypal.com";
	
	private static final String SQL_GET_CONFIG = new StringBuilder()
	.append("SELECT gc.config FROM iterpaywall_payment_gateway_config gc \n")
	.append("INNER JOIN iterpaywall_payment_gateway g ON g.id = gc.gatewayid \n")
	.append("WHERE groupid = %d AND g.name = 'paypal'")
	.toString();
	
	private static final String XPATH_CONFIG = "/rs/row/@config";
	
	DateFormat paypalDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	public Map<String,String> getConfig(long groupId) throws ServiceError
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

			ErrorRaiser.throwIfFalse(Validator.isNotNull(config.get(CONF_SNDB)), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(config.get(CONF_CLIENTID)), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(config.get(CONF_SECRET)), PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
			
			configs.put(groupId, config);
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PAYMENT_GATEWAY_NOT_CONFIGURED);
		}
	}
	
	@Override
	public PaywallTransactionModel getTransactionDetails(long groupId, String transactionData) throws ServiceError
	{
		PaywallTransactionModel transaction = null;

		try
		{
			JsonElement jelement = new JsonParser().parse(transactionData);
			JsonObject jobject = jelement.getAsJsonObject();
	
			String id = jobject.get("id").getAsString();
			String status = jobject.get("state").getAsString();
			Date transactionDate = new Date();
			transactionDate = paypalDateFormat.parse(jobject.get("create_time").getAsString());
			JsonObject t = jobject.get("transactions").getAsJsonArray().get(0).getAsJsonObject();
			BigDecimal amount = t.get("amount") != null ? t.get("amount").getAsJsonObject().get("total").getAsBigDecimal() : new BigDecimal(0);
			long productid = t.get("item_list") != null ? t.get("item_list").getAsJsonObject().get("items").getAsJsonArray().get(0).getAsJsonObject().get("sku").getAsLong() : 0L;
			Map<String,String> custom = IterHttpClient.Util.deserializeQueryString(t.get("custom").getAsString());
			String usrid = custom.get("userid");
			String errorurl = custom.get("errorurl");
			
			transaction = new PaywallTransactionModel.Builder(groupId, PaywallGateway.PAYPAL, id, transactionData)
			.userid(usrid)
			.productid(productid)
			.date(transactionDate)
			.amount(amount)
			.status(status)
			.completed("approved".equals(status))
			.errorUrl(errorurl)
			.build();
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_TRANSACTION_NOT_PROCESSED);
		}
		
		return transaction;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// BOTÓN DE PAGO
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public String getPaymentButtonCode(PaywallProductModel product) throws ServiceError
	{
		String code = StringPool.BLANK;
		
		if (isProductConfiguredForPaypal(product))
		{
			code = "<div class=\"paypal-button\" data-product=\"" + product.get("urlname") + "\" data-errorurl=\"" + product.getErrorPageFriendlyUrl() + "\"></div>";
			IterRequest.setAttribute(WebKeys.CONTAINS_PAYPAL_BUTTON, Boolean.TRUE);
		}
		else
		{
			log.error("Paywall product not configured for PayPal");
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PRODUCT_NOT_CONFIGURED);
		}
		
		return code;
	}
	
	private boolean isProductConfiguredForPaypal(PaywallProductModel product)
	{
		return Validator.isNotNull(product.get("urlname"))  && Validator.isNotNull(product.get("price"))
			&& Validator.isNotNull(product.get("currencyalpha")) && Validator.isNotNull(product.get("validity"));
	}

	@Override
	public boolean validateConfiguration(JsonObject configuration)
	{
		return Validator.isNotNull(configuration.get(CONF_SNDB))
		    && Validator.isNotNull(configuration.get(CONF_CLIENTID))
		    && Validator.isNotNull(configuration.get(CONF_SECRET));
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// MÉTODOS PROPIOS PARA PAYPAL
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private String getAccessToken(long groupId, boolean invalidateCurrentToken) throws Throwable
	{
		String accessToken = invalidateCurrentToken ? null : tokens.get(groupId);
		
		if (accessToken == null)
		{
			try
			{
				Map<String, String> config = getConfig(groupId);
				String cliendId = config.get(CONF_CLIENTID);
				String cliendSecret = config.get(CONF_SECRET);
				
				String apiUrl = buildApiUrl(groupId, "oauth2/token");
				IterHttpClient httpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, apiUrl)
				.connectionTimeout(10000)
				.readTimeout(10000)
				.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
				.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
				.header("Accept", "application/json")
				.header("Accept-Language", "en_US")
				.header("Authorization", "Basic " + new String(Base64.encode((cliendId + StringPool.COLON + cliendSecret).getBytes())))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.payLoad("grant_type=client_credentials")
				.build();
				
				String response = httpc.connect();
				if (HttpServletResponse.SC_OK == httpc.getResponseStatus())
				{
					// Recupera el token
					JsonElement jelement = new JsonParser().parse(response);
					JsonObject  jobject = jelement.getAsJsonObject();
					accessToken = jobject.get("access_token").getAsString();
					
					// Lo guarda en la configuración
					tokens.put(groupId, accessToken);
				}
				else
				{
					// Recupera la respuesta de error
					JsonObject jobject = parseErrorResponse(response);
					if (jobject == null)
						log.error("Unable to retrieve PayPal access token. Response status: " + httpc.getResponseStatus());
					else
					{
						String error = jobject.get("error").getAsString();
						String errorDescription = jobject.get("error_description").getAsString();
						log.error("Unable to retrieve PayPal access token. Response status: " + httpc.getResponseStatus() + " | Error: " + error + " | Description: " + errorDescription);
					}
					ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PAYPAL_TOKEN);
				}
			}
			catch (Throwable th)
			{
				IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_E_PAYPAL_TOKEN , th);
				throw th;
			}
		}
		
		return accessToken;
	}
	
	public String createPayment(long groupId, String productUrlName, String userId, String errorUrl) throws ServiceError
	{
		String paymentId = StringPool.BLANK;
		
		try
		{
			// Crea la transacción
			String transactionDetails = createPaymentPayload(groupId, productUrlName, userId, errorUrl);
			
			// Recupera el token de acceso
			String accessToken = getAccessToken(groupId, false);
			
			// Crea el pago
			String apiUrl = buildApiUrl(groupId, "payments/payment");
			IterHttpClient httpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, apiUrl)
				.connectionTimeout(10000)
				.readTimeout(10000)
				.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
				.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
				.header("Accept", "application/json")
				.header("Accept-Language", "en_US")
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.payLoad(transactionDetails)
				.build();
            
			String response = httpc.connect();
			
			if (HttpServletResponse.SC_UNAUTHORIZED == httpc.getResponseStatus())
			{
				// Pide un nuevo token
				accessToken = getAccessToken(groupId, true);
				
				// Vuelve a intentar crear el pago
				httpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, apiUrl)
					.connectionTimeout(10000)
					.readTimeout(10000)
					.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
					.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
					.header("Accept", "application/json")
					.header("Accept-Language", "en_US")
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json")
					.payLoad(transactionDetails)
					.build();
				
				response = httpc.connect();
			}
			
			if (HttpServletResponse.SC_OK == httpc.getResponseStatus() || HttpServletResponse.SC_CREATED == httpc.getResponseStatus())
			{
				// Recupera el paymentId
				paymentId = new JsonParser().parse(response).getAsJsonObject().get("id").getAsString();
			}
			else
			{
				log.error("Unable to create PayPal payment. Response status: " + httpc.getResponseStatus());
				JsonObject jobject = parseErrorResponse(response);
				if (jobject == null)
				{
					// Construye una transacción errónea genérica
					paymentId = buildErrorTransactionDetails(null, paymentId, userId, errorUrl, PaywallErrorKeys.PAYWALL_E_PAYPAL_CREATE_PAYMENT);
				}
				else
				{
					// Contruye una transacción errónea
					paymentId = buildErrorTransactionDetails(jobject, paymentId, userId, errorUrl, null);
				}
				IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_E_PAYPAL_CREATE_PAYMENT, paymentId);
			}
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_E_PAYPAL_CREATE_PAYMENT, th);
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PAYPAL_CREATE_PAYMENT);
		}
		
		return paymentId;
	}
	
	public String executePayment(long groupId, String paymentId, String payerId, String userId, String errorUrl) throws ServiceError
	{
		String response = StringPool.BLANK;
		
		try
		{
			// Recupera el token de acceso
			String accessToken = getAccessToken(groupId, false);

			String apiUrl = buildApiUrl(groupId, "payments/payment/" + paymentId + "/execute");
			String transactionDetails = String.format("{\"payer_id\": \"%s\"}", payerId);
			
			// Crea el pago
			IterHttpClient httpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, apiUrl)
				.connectionTimeout(10000)
				.readTimeout(10000)
				.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
				.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.payLoad(transactionDetails)
				.build();
				
			response = httpc.connect();
			
			if (HttpServletResponse.SC_UNAUTHORIZED == httpc.getResponseStatus())
			{
				// Pide un nuevo token
				accessToken = getAccessToken(groupId, true);
				
				// Vuelve a intentar crear el pago
				httpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, apiUrl)
					.connectionTimeout(10000)
					.readTimeout(10000)
					.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
					.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json")
					.payLoad(transactionDetails)
					.build();
				
				response = httpc.connect();
			}
			
			if (HttpServletResponse.SC_OK != httpc.getResponseStatus())
			{
				log.error("Unable to execute PayPal payment. Response status: " + httpc.getResponseStatus());
				JsonObject jobject = parseErrorResponse(response);
				if (jobject == null)
				{
					// Construye una transacción errónea genérica
					response = buildErrorTransactionDetails(null, paymentId, userId, errorUrl, PaywallErrorKeys.PAYWALL_E_PAYPAL_EXECUTE_PAYMENT);
				}
				else
				{
					// Contruye una transacción errónea
					response = buildErrorTransactionDetails(jobject, paymentId, userId, errorUrl, null);
				}
				IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_E_PAYPAL_EXECUTE_PAYMENT, response);
			}
		}
		catch(Throwable th)
		{
			IterMonitor.logEvent(groupId, Event.ERROR, new Date(), PaywallErrorKeys.PAYWALL_E_PAYPAL_EXECUTE_PAYMENT, th);
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PAYPAL_EXECUTE_PAYMENT);
		}
		
		return response;
	}
	
	// https://developer.paypal.com/docs/integration/direct/payments/paypal-payments/#create-paypal-payment
	private String createPaymentPayload(long groupId, String productUrlName, String userId, String errorUrl) throws ServiceError
	{
		// Crea el JSON de la transacción
		JsonObject payment = new JsonObject();
		
		try
		{
			// Recupera el producto
			Document product = ProductLocalServiceUtil.getPaywallProductByUrlName(String.valueOf(groupId), productUrlName);
			
			String total = XMLHelper.getStringValueOf(product, "/rs/row/@price");
			String currency = XMLHelper.getStringValueOf(product, "/rs/row/@currencyAlpha");
			String name = XMLHelper.getStringValueOf(product, "/rs/row/pname");
			String desc = XMLHelper.getStringValueOf(product, "/rs/row/pdescription");
			String id = XMLHelper.getStringValueOf(product, "/rs/row/@id");
			
			// Intent
			payment.addProperty("intent", "sale");
			
			// Method
			JsonObject payer = new JsonObject();
			payer.addProperty("payment_method", "paypal");
			payment.add("payer", payer);
			
			// Redirect URLs
			JsonObject urls = new JsonObject();
			urls.addProperty("return_url", IterURLUtil.getIterHost() + "/user-portlet/refreshuserentitlements"); // TODO no estoy seguro de que sirva para algo
			urls.addProperty("cancel_url", IterURLUtil.getIterHost() + "/user-portlet/refreshuserentitlements"); // TODO no estoy seguro de que sirva para algo
			payment.add("redirect_urls", urls);
			
			// Transaction
			JsonObject transaction = new JsonObject();
			
			// Total
			JsonObject amount = new JsonObject();
			amount.addProperty("total", total);
			amount.addProperty("currency", currency);
			transaction.add("amount", amount);
			
			// Items
			JsonObject itemList = new JsonObject();
			JsonArray items = new JsonArray();
			JsonObject item = new JsonObject();
			item.addProperty("quantity", 1);
			item.addProperty("name", name);
			item.addProperty("description", desc);
			item.addProperty("sku", id);
			item.addProperty("price", total);
			item.addProperty("currency", currency);
			items.add(item);
			itemList.add("items", items);
			transaction.add("item_list", itemList);
			
			// Custom
			String custom = "userid=" + userId + "&errorurl=" + errorUrl;
			transaction.addProperty("custom", custom);
			
			JsonArray transactions = new JsonArray();
			transactions.add(transaction);
			payment.add("transactions", transactions);
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError("ITR_E_PRODUCT_NOT_FOUND");
		}
		
		return payment.toString();
	}
	
	@Override
	public PaywallTransactionModel getTransacctionFromWebhook(long groupId, HttpServletRequest request) throws ServiceError
	{
		PaywallTransactionModel transaction = null;
		
		try
		{
			// Recupera el payload
			StringBuilder webhookData = new StringBuilder();
			BufferedReader reader = request.getReader();
			String line;
		    while ((line = reader.readLine()) != null)
		    {
		    	webhookData.append(line);
		    }
		    
			JsonObject jsonData = new JsonParser().parse(webhookData.toString()).getAsJsonObject();
			String paymentId = jsonData.get("resource").getAsJsonObject().get("parent_payment").getAsString();
			
			// Recupera el token de acceso
			String accessToken = getAccessToken(groupId, false);
			
			String apiUrl = buildApiUrl(groupId, "payments/payment/" + paymentId);
			
			// Pide los detalles del pago
			IterHttpClient httpc = new IterHttpClient.Builder(IterHttpClient.Method.GET, apiUrl)
				.connectionTimeout(10000)
				.readTimeout(10000)
				.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
				.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.build();
				
			String transactionData = httpc.connect();
			
			if (HttpServletResponse.SC_UNAUTHORIZED == httpc.getResponseStatus())
			{
				// Invalida el token
				tokens.put(groupId, null);
				
				// Pide un nuevo token
				accessToken = getAccessToken(groupId, true);
				
				// Pide los detalles del webhook
				httpc = new IterHttpClient.Builder(IterHttpClient.Method.GET, apiUrl)
					.connectionTimeout(10000)
					.readTimeout(10000)
					.header(WebKeys.HOST, LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
					.header("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS)
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json")
					.build();
				
				transactionData = httpc.connect();
			}
			
			if (HttpServletResponse.SC_OK == httpc.getResponseStatus())
			{
				transaction = getTransactionDetails(groupId, transactionData);
			}
			else
			{
				JsonObject jobject = parseErrorResponse(transactionData);
				if (jobject == null)
				{
					log.error("Unable to retrieve PayPal transacntion. Response status: " + httpc.getResponseStatus());
				}
				else
				{
					String name = jobject.get("name").getAsString();
					String message = jobject.get("message").getAsString();
					log.error("Unable to retrieve PayPal transacntion. Response status: " + httpc.getResponseStatus() + " | Error: " + name + " | Detail: " + message);
				}
				ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PAYPAL_RETRIEVE_PAYMENT);
			}
			
		}
		catch (Throwable th)
		{
			throw ErrorRaiser.toServiceError(th);
		}
		
		return transaction;
	}
	
	final String PAYPAL_ERROR_REGEX = "(?s)(\\{.*\\})";
	
	private JsonObject parseErrorResponse(String response)
	{
		JsonObject jobject = null;
		try
		{
			Pattern pattern = Pattern.compile(PAYPAL_ERROR_REGEX, Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(response);
			if (matcher.find())
			{
				response = matcher.group(1);
				jobject = new JsonParser().parse(response).getAsJsonObject();
			}
		}
		catch (Throwable th)
		{
			log.error(th);
		}
		
		return jobject;
	}
	
	private String buildErrorTransactionDetails(JsonObject errorData, String paymentId, String userId, String errorUrl, String genericError)
	{
		// Respuesta
		JsonObject errorResponse = new JsonObject();
		
		// Contruye una transacción errónea
		errorResponse.addProperty("id", paymentId);
		errorResponse.addProperty("state", errorData != null ? errorData.get("name").getAsString() : genericError);
		errorResponse.addProperty("create_time", paypalDateFormat.format(new Date()));
		JsonArray transactions = new JsonArray();
		JsonObject transaction = new JsonObject();
		String custom = "userid=" + userId + "&errorurl=" + errorUrl;
		transaction.addProperty("custom", custom);
		transactions.add(transaction);
		errorResponse.add("transactions", transactions);
		if (errorData != null)
			errorResponse.add("error", errorData);
		
		return errorResponse.toString();
	}
	
	private static final String PAYPAL_RENDER_BUTTON_CODE = new StringBuilder()
	.append("	<script src='https://www.paypalobjects.com/api/checkout.js'></script>                                                \n")
	.append("	                                                                                                                     \n")
	.append("	<script>                                                                                                             \n")
	.append("		jQryIter('.paypal-button').each(function(i, element)                                                             \n")
	.append("		{                                                                                                                \n")
	.append("			var product = jQryIter(element).attr('data-product');                                                        \n")
	.append("			var errorurl = jQryIter(element).attr('data-errorurl');                                                      \n")
	.append("			                                                                                                             \n")
	.append("			paypal.Button.render({                                                                                       \n")
	.append("				env: '%s',                                                                                               \n")
	.append("		        style: jQryIter.hooks.pay.paypalButtonStyle(),                                                           \n")
	.append("				payment: function(data, actions) {                                                                       \n")
	.append("					return actions.request.post('/news-portlet/paymentfulfillment/paypal/create-payment/' + product, {   \n")
	.append("						errorUrl:  errorurl                                                                              \n")
	.append("					})                                                                                                   \n")
	.append("					.then(function(res) {                                                                                \n")
	.append("						if(typeof res.id != 'undefined')                                                                 \n")
	.append("						    return res.id;                                                                               \n")
	.append("						else                                                                                             \n")
	.append("						    window.location.href = res.url;                                                              \n")
	.append("					});                                                                                                  \n")
	.append("				},                                                                                                       \n")
	.append("				experience: {                                                                                            \n")
	.append("	                input_fields: {                                                                                      \n")
	.append("	                    no_shipping: 1                                                                                   \n")
	.append("	                }                                                                                                    \n")
	.append("	            },                                                                                                       \n")
	.append("				commit: true,                                                                                            \n")
	.append("				onAuthorize: function(data, actions) {                                                                   \n")
	.append("					return actions.request.post('/news-portlet/paymentfulfillment/paypal/execute-payment/' + product, {  \n")
	.append("						paymentID: data.paymentID,                                                                       \n")
	.append("						payerID:   data.payerID,                                                                         \n")
	.append("						errorUrl:   errorurl                                                                             \n")
	.append("					})                                                                                                   \n")
	.append("					.then(function(result) {                                                                             \n")
	.append("						window.location.href = result.url;                                                               \n")
	.append("					});                                                                                                  \n")
	.append("				}                                                                                                        \n")
	.append("			}, element);                                                                                                 \n")
	.append("		});                                                                                                              \n")
	.append("	</script>                                                                                                            \n")
	.toString();
	
	public String getPaypalRenderButtonCode(long groupId) throws ServiceError
	{
		Map<String, String> config = getConfig(groupId);
		boolean sandbox = GetterUtil.getBoolean(config.get(CONF_SNDB), true);
		
		return String.format(PAYPAL_RENDER_BUTTON_CODE, sandbox ? "sandbox" : "production");
	}
	
	private String buildApiUrl(long groupId, String path) throws ServiceError, PortalException, SystemException, IOException
	{
		Map<String, String> config = getConfig(groupId);
		boolean sandbox = GetterUtil.getBoolean(config.get(CONF_SNDB), true);
		
		String[] masterList = ApacheHierarchy.getInstance().getMasterList();
		ErrorRaiser.throwIfFalse( masterList.length > 0, IterErrorKeys.XYZ_E_APACHE_MASTERS_NOT_FOUND_ZYX);
		
		return masterList[0] + "/_https_/" + (sandbox ? API_URL_SANDBOX : API_URL_LIVE) + "/v1/" + path;
	}
}
