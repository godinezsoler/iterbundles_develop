package com.protecmedia.iter.news.paywall.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.news.paywall.PaywallMgrFactory;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.paywall.utils.PaywallGateway;

public class PaywallProductModel extends LinkedHashMap<String, Object>
{
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactoryUtil.getLog(PaywallProductModel.class);
	
	private final Pattern validityPattern = Pattern.compile("^(\\d+)([dmy])$");
	DateFormat validityDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private long id;
	private long groupId;
	private String articleId = null;
	private Object articleTemplateContent = null;
	
	private String paypalCode = null;
	private String redsysCode = null;

	private String errorPageFriendlyUrl = null;
	
	public PaywallProductModel(Node productNode, String errorPageFriendlyUrl)
	{
		this(productNode);
		this.errorPageFriendlyUrl = errorPageFriendlyUrl;
	}
	
	public PaywallProductModel(Node productNode)
	{
		// Identificador del producto
		id = XMLHelper.getLongValueOf(productNode, "@id");
		
		// Recupera la información del producto que estará disponible para el pintado del portlet
		put("name", XMLHelper.getStringValueOf(productNode, "pname"));
		put("description", XMLHelper.getStringValueOf(productNode, "pdescription"));
		put("urlname", XMLHelper.getStringValueOf(productNode, "@urlname"));
		put("price", XMLHelper.getStringValueOf(productNode, "@price"));
		put("currencyalpha", XMLHelper.getStringValueOf(productNode, "@currencyAlpha"));
		put("currencynumeric", XMLHelper.getStringValueOf(productNode, "@currencyNumeric"));
		put("validity", XMLHelper.getStringValueOf(productNode, "@validity"));
		put("producturl", "/-/pay/" + get("urlname"));
		put("tags", StringUtil.merge(XMLHelper.getStringValues(productNode.selectNodes("products/product"), "@nameBase64"), StringPool.SEMICOLON));
	
		// Obtiene el identificador del grupo al que pertenece el producto
		groupId = XMLHelper.getLongValueOf(productNode, "@groupid");
		
		// Recupera el Id del artículo instrumental asociado al producto
		articleId = XMLHelper.getStringValueOf(productNode, "@articleid");
	}
	
	public long id() { return id; };
	public long getGroupId() { return groupId; };
	public String getErrorPageFriendlyUrl() { return errorPageFriendlyUrl; };
	
	/**
	 * Recupera el artículo instrumental asociado al producto y lo carga en un objeto válido para las plantillas Velocity.
	 * @return <ul><li>{@code IterTemplateContent} con el artículo instrumental del producto.</li>
	 *         <li>{@code null} en caso de no tener artículo instrumental o si ocurre un error al recuperarlo.</li></ul>
	 */
	public Object getArticle()
	{
		// Si hay un artículo instrumental asociado y aún no se ha cargado
		if (Validator.isNotNull(articleId) && Validator.isNull(articleTemplateContent))
		{
			try
			{
				// Obtiene el artículo
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
				
				// Lo carga en un TemplateContent
				MethodKey method = new MethodKey("com.liferay.portal.util.InstrumentalContentUtil", "getIterTemplateContent", String.class, String.class, long.class);
				articleTemplateContent = PortalClassInvoker.invoke(false, method, article.getContent(), article.getArticleId(), groupId);
			}
			catch (Throwable th)
			{
				log.error(th);
			}
		}
		
		return articleTemplateContent;
	}
	
	/**
	 * Retorna el código necesario para renderizar el botón de PayPal.
	 * @return El código necesario para renderizar el botón de PayPal.
	 */
	public String getPaypalButton()
	{
		if (Validator.isNull(paypalCode))
		{
			try
			{
				paypalCode = PaywallMgrFactory.getProvider(PaywallGateway.PAYPAL).getPaymentButtonCode(this);
			}
			catch (ServiceError e)
			{
				log.error(e);
			}
		}

		return paypalCode;
	}
	
	/**
	 * Retorna el código necesario para renderizar el botón de Redsys.
	 * @return El código necesario para renderizar el botón de Redsys.
	 */
	public String getRedsysButton()
	{
		if (Validator.isNull(redsysCode))
		{
			try
			{
				redsysCode = PaywallMgrFactory.getProvider(PaywallGateway.REDSYS).getPaymentButtonCode(this);
			}
			catch (ServiceError e)
			{
				log.error(e);
			}
		}

		return redsysCode;
	}
	
	/**
	 * Calcula la fecha de expiración de un producto en formato yyyyMMddHHmmss teniendo en cuenta la fecha actual y su validez.
	 * @return La fecha de expiración desde hoy en formato yyyyMMddHHmmss.
	 * @throws ServiceError Si el producto no tiene configurada fecha de expiración.
	 */
	public String getCalculatedValidity() throws ServiceError
	{
		String validity = null;
		
		if (get("validity") != null)
		{
			Matcher m = validityPattern.matcher(get("validity").toString());
			if (m.find())
			{
				int amount = Integer.valueOf(m.group(1));
				String p = m.group(2);
				
				GregorianCalendar c = new GregorianCalendar();
				if ("d".equals(p))
					c.add(GregorianCalendar.DAY_OF_MONTH, amount);
				else if ("m".equals(p))
					c.add(Calendar.MONTH, amount);
				else if ("y".equals(p))
					c.add(Calendar.YEAR, amount);
			
				validity = validityDateFormat.format(c.getTime());
			}
		}
		else
		{
			ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PRODUCT_NOT_CONFIGURED);
		}
		
		return validity;
	}
}
