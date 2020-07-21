package com.protecmedia.iter.news.util;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.kernel.velocity.VelocityContext;
import com.liferay.portal.kernel.velocity.VelocityEngineUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.news.paywall.PaywallMgrFactory;
import com.protecmedia.iter.news.paywall.model.PaywallTransactionModel;
import com.protecmedia.iter.news.paywall.provider.IPaywallProvider;

public class PaywallUtil
{
	public static String renderProducts(HttpServletRequest request, String templateId, String errorLayout) throws Exception
	{
		String html = StringPool.BLANK;
		
		// Obtiene el identificador del grupo
		long groupId = PortalUtil.getScopeGroupId(request);
		
		// Recupera la plantilla
		JournalTemplate template = JournalTemplateLocalServiceUtil.getTemplate(groupId, templateId);
		
		// Recupera la página de error
		Layout errorPage = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(errorLayout, groupId);
		String errorPageUrl = errorPage.getFriendlyURL();
		
        // Obtiene el producto indicado como parámetro en la URL
        String selectedProduct = SectionUtil.getPublicRenderParameter(request, WebKeys.URL_PARAM_CONTENT_TAGS);
		
		// Crea el objeto al que se le inyectarán los productos
		VelocityContext velocityContext = VelocityEngineUtil.getEmptyContext();
		
		// Inyección iterVelocityTools
        velocityContext.put(VelocityContext.ITER_VELOCITY_TOOLS, new IterVelocityTools(velocityContext));
        
        // Inyección de los productos
		velocityContext.put("productToolbox", new PaywallTools(groupId, selectedProduct, errorPageUrl));
		
		// Aplica la transformación velocity
		StringWriter sw = new StringWriter();
        VelocityEngineUtil.mergeTemplate((new Random(10000)).toString(), template.getXsl(), velocityContext, sw);	
        html = sw.toString();
		
		return html;
	}
	
	public static void updateUserEntitlements(String userId, String entitle, String expirationDate) throws NoSuchMethodException, SecurityException, DocumentException, IOException, SQLException
	{
		// Recupera los derechos actuales del usuario
		String sql = "SELECT entitlements from iterusers WHERE usrid='%s'";
		Document d = PortalLocalServiceUtil.executeQueryAsDom(String.format(sql, userId));
		
		Document docEntitlements = null;
		Node productsRoot = null;
		
		// Comprueba si tiene productos
		String currentEntitlements = XMLHelper.getTextValueOf(d.getRootElement(), "/rs/row/@entitlements");
		if (Validator.isNotNull(currentEntitlements))
		{
			// Parsea el documento
			docEntitlements = SAXReaderUtil.read(currentEntitlements);
			// Recupera la raiz de los productos
			productsRoot = docEntitlements.selectSingleNode("/itwresponse/field[@name='output']/array");
		}
		
		// Tiene productos
		if (Validator.isNotNull(productsRoot))
		{
			// Busca el producto
			Node currentEntitle = productsRoot.selectSingleNode(String.format("object[field/@name='entitle' and field/string='%s']", entitle));
			if (Validator.isNotNull(currentEntitle))
			{
				// Busca el nodo que contiene la fecha de expiración
				Node currentExpirationDate = currentEntitle.selectSingleNode("field[@name='expires']/string");
				
				// Actualiza la fecha de expiración
				if (Validator.isNotNull(currentExpirationDate))
					currentExpirationDate.setText(expirationDate);
				// Si está mal creado, regenera el nodo completo del entitlement
				else
				{
					currentEntitle.detach();
					createUserEntitlement(productsRoot, entitle, expirationDate);
				}
			}
			// Crea el producto
			else
			{
				createUserEntitlement(productsRoot, entitle, expirationDate);
			}
		}
		// No tiene entitlements
		else
		{
			docEntitlements = SAXReaderUtil.createDocument();
			Element root = docEntitlements.addElement("itwresponse").addAttribute("version", "1.0");
			root.addElement("field").addAttribute("name", "code").addElement("string").setText("OK");
			createUserEntitlement(root.addElement("field").addAttribute("name", "output").addElement("array"), entitle, expirationDate);
		}
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format("UPDATE iterusers SET aboid='%2$s', entitlements='%1$s' WHERE usrid='%2$s'", StringEscapeUtils.escapeSql(docEntitlements.asXML()), userId));
	}
	
	private static void createUserEntitlement(Node productsRoot, String entitle, String expirationDate)
	{
		Element object = ((Element) productsRoot).addElement("object");
		Element field = null;
		
		field = object.addElement("field");
		field.addAttribute("name", "type");
		field.addElement("string").setText("basic");
		
		field = object.addElement("field");
		field.addAttribute("name", "entitle");
		field.addElement("string").setText(entitle);
		
		field = object.addElement("field");
		field.addAttribute("name", "expires");
		field.addElement("string").setText(expirationDate);
	}
	
	public static PaywallTransactionModel processPayment(long groupId, String providerName, String transactionData) throws SecurityException, ServiceError, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		return processPayment(groupId, providerName, transactionData, null);
	}
	
	public static PaywallTransactionModel processPayment(long groupId, String providerName, HttpServletRequest request) throws SecurityException, ServiceError, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		return processPayment(groupId, providerName, null, request);
	}
	
	private static PaywallTransactionModel processPayment(long groupId, String providerName, String transactionData, HttpServletRequest request) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		PaywallTransactionModel transaction = null;
		
		// Obtiene la pasarela de pago
		IPaywallProvider provider = PaywallMgrFactory.getProvider(providerName);

		// Recupera los datos de la transacción
		transaction = request == null ? provider.getTransactionDetails(groupId, transactionData) : provider.getTransacctionFromWebhook(groupId, request);
		
		// Guarda la transacción
		if (Validator.isNotNull(transaction.getId()) && transaction.save())
		{
			// Valida que la transacción sea correcta
			if (transaction.isCompleted())
			{
				// Actualiza los derechos de la suscripción del usuario
				updateUserEntitlements(
					transaction.getUser(),
					transaction.getProduct().get("name").toString(),
					transaction.getProduct().getCalculatedValidity()
				);
			}
		}
		
		return transaction;
	}
}
