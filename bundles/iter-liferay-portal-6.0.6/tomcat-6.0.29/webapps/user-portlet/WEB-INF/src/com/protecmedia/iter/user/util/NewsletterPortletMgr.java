package com.protecmedia.iter.user.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.CKEditorUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class NewsletterPortletMgr 
{
	private static Log _log = LogFactoryUtil.getLog(NewsletterPortletMgr.class);
	
	static public final String NEWSLETTER_TOGGLE_CODE = new StringBuilder()
	.append("<script type=\"text/javascript\">                                                                          \n")
	.append(" jQryIter(document).ready(function()                                                                       \n")
	.append(" {                                                                                                         \n")
	.append("     ITER.newsletter.checkLightFields();                                                                   \n")
	.append("	  // Si no existe el control es que no está en modo light 												\n")
	.append("     if (!jQryIter('#newsletter_email_ctrl').length) 														\n")	
	.append("     	ITER.newsletter.getNewsletterUser();                                                                \n")
	.append("                                                                                                           \n")
	.append("     jQryIter('.newsletters_cab').click(function()                                                         \n")
	.append("     {                                                                                                     \n")
	.append("         jQryIter(this).next().slideToggle('fast',function()                                               \n")
	.append("         {                                                                                                 \n")
	.append("             if(jQryIter(this).prev().hasClass(\"newsletters_open\"))                                      \n")
	.append("                 jQryIter(this).prev().removeClass(\"newsletters_open\").addClass(\"newsletters_closed\"); \n")
	.append("             else                                                                                          \n")
	.append("                 jQryIter(this).prev().removeClass(\"newsletters_closed\").addClass(\"newsletters_open\"); \n")
	.append("                                                                                                           \n")
	.append("             ITER.newsletter.checkStatusExpanded();                                                        \n")
	.append("         });                                                                                               \n")
	.append("         return false;                                                                                     \n")
	.append("     }).next().hide();                                                                                     \n")
	.append("                                                                                                           \n")
	.append("     jQryIter(\".newsletters_expandCollapse\").click(function()                                            \n")
	.append("     {                                                                                                     \n")
	.append("         var expandCollapseDiv = jQryIter(this);                                                           \n")
	.append("         if(expandCollapseDiv.hasClass(\"newsletters_collapsed\"))                                         \n")
	.append("         {                                                                                                 \n")
	.append("             expandCollapseDiv.removeClass(\"newsletters_collapsed\").addClass(\"newsletters_expanded\");  \n")
	.append("             ITER.newsletter.expandAllNewsletter();                                                        \n")
	.append("         }                                                                                                 \n")
	.append("         else                                                                                              \n")
	.append("         {                                                                                                 \n")
	.append("             expandCollapseDiv.removeClass(\"newsletters_expanded\").addClass(\"newsletters_collapsed\");  \n")
	.append("             ITER.newsletter.collapseAllNewsletter();                                                      \n")
	.append("         }                                                                                                 \n")
	.append("     });                                                                                                   \n")
	.append(" });                                                                                                       \n")
	.append("</script>                                                                                                  \n")
	.toString();
	
	private String _nl_monday			= "";
	private String _nl_tuesday			= "";
	private String _nl_wednesday		= "";
	private String _nl_thursday			= "";
	private String _nl_friday			= "";
	private String _nl_saturday			= "";
	private String _nl_sunday			= "";
	private String _nl_hour 			= "";

	private String _title 				= "";
	private String _description 		= "";
	private String _notsignin 			= "";
	private String _subscribe 			= "";
	private String _unsubscribe 		= "";
	private String _manageerror 		= "";
	
	private String _emailRepeatedError 	= "";
	private String _chkSubsError 		= "";
	private String _chkSubs 	 		= "";
	private boolean _allowAnonymous		= false;
	private String _email				= "";
	private String _licenseAgreement	= "";
	private String _acceptLicenseError	= "";

	
	private boolean _notifySubscription = true;

	public NewsletterPortletMgr()
	{
	}
	
	public NewsletterPortletMgr(String preferences)
	{
		_notifySubscription = true;
		
		try
		{
			fillPreferences(preferences);
		}
		catch (Exception e)
		{
			// Do nothing
		}
	}
	
	public NewsletterPortletMgr(String preferences, boolean notifySubscription) throws ServiceError
	{
		_notifySubscription = notifySubscription;
		
		try
		{
			fillPreferences(preferences);
		}
		catch (Exception e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
	}
	
	private static final String PREFERENCE_XPATH = "/portlet-preferences/preference[name/text() = '%s']/value";
	
	private void fillPreferences(String preferences) throws DocumentException
	{
		Document xmlPrefs = SAXReaderUtil.read(preferences);
		
		_nl_monday			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_monday"), 					"");	
		_nl_tuesday			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_tuesday"), 					""); 
		_nl_wednesday		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_wednesday"), 				"");
		_nl_thursday		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_thursday"), 					"");
		_nl_friday			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_friday"), 					"");
		_nl_saturday		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_saturday"), 					"");
		_nl_sunday			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_sunday"), 					"");
		_nl_hour 			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_hour"), 						"");
		_title 				= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_title"), 					"");
		_description 		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_description"), 				"");
		_notsignin 			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_notsignin-msg"), 			"");
		_subscribe 			= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_subscribe-msg"), 			"");
		_unsubscribe 		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_unsubscribe-msg"), 			"");
		_manageerror 		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_manageerror-msg"), 			"");
		_emailRepeatedError = XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_email-repeated-error-msg"), 	"");
		_chkSubsError 		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_chk-subs-error-msg"), 		"");
		_chkSubs	 		= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_chk-subs-msg"), 				"");
		_allowAnonymous		= GetterUtil.getBoolean( XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_allow_anonymous")) );
		_email 				= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_email"), 					"");
		_licenseAgreement 	= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_license_agreement"), 		"").trim();
		_acceptLicenseError	= XMLHelper.getStringValueOf(xmlPrefs, String.format(PREFERENCE_XPATH, "nl_accept_license-error-msg"), 	"");
	}
	
	
//	private String getPreference(Document preferences, String name)
//	{
//		String xpath = "/portlet-preferences/preference[name/text() = '" + name + "']/value";
//		Node value = preferences.getRootElement().selectSingleNode(xpath);
//		return value != null ? value.getText() : StringPool.BLANK;
//	}
	
	public NewsletterPortletMgr(PortletPreferences preferences)
	{
		_nl_monday			= preferences.getValue("nl_monday", 					"");
		_nl_tuesday			= preferences.getValue("nl_tuesday", 					"");
		_nl_wednesday		= preferences.getValue("nl_wednesday", 					"");
		_nl_thursday		= preferences.getValue("nl_thursday", 					"");
		_nl_friday			= preferences.getValue("nl_friday", 					"");
		_nl_saturday		= preferences.getValue("nl_saturday", 					"");
		_nl_sunday			= preferences.getValue("nl_sunday", 					"");

		_nl_hour 			= preferences.getValue("nl_hour", 						"");

		_title 				= preferences.getValue("nl_title", 						"");
		_description 		= preferences.getValue("nl_description",				"");
		_notsignin 			= preferences.getValue("nl_notsignin-msg", 				"");
		_subscribe 			= preferences.getValue("nl_subscribe-msg", 				"");
		_unsubscribe 		= preferences.getValue("nl_unsubscribe-msg",			"");
		_manageerror 		= preferences.getValue("nl_manageerror-msg",			"");
		
		_emailRepeatedError = preferences.getValue("nl_email-repeated-error-msg",	"");
		_chkSubsError 		= preferences.getValue("nl_chk-subs-error-msg",			"");
		_chkSubs	 		= preferences.getValue("nl_chk-subs-msg",				"");
		_allowAnonymous		= GetterUtil.getBoolean( preferences.getValue("nl_allow_anonymous", "") );
		_email				= preferences.getValue("nl_email",						"");
		_licenseAgreement	= preferences.getValue("nl_license_agreement",			"").trim();
		_acceptLicenseError	= preferences.getValue("nl_accept_license-error-msg",	"");
	}
	
	/**
	 * 
	 * @param typeHour
	 * @param hour
	 * @return
	 */
	private static String setHour(String typeHour, String hour) 
	{
		String hourfinal = "";
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
		calendar.set(Calendar.MINUTE, 0);
		
		if(typeHour.equalsIgnoreCase("h:0 a"))
		{
			DateFormat dateFormat = new SimpleDateFormat(IterKeys.DATEFORMAT_h_m);
			hourfinal = dateFormat.format(calendar.getTime());
		}
		if(typeHour.equalsIgnoreCase("hh:00 a"))
		{
			DateFormat dateFormat = new SimpleDateFormat(IterKeys.DATEFORMAT_hh_mm);
			hourfinal = dateFormat.format(calendar.getTime());
		}
		if(typeHour.equalsIgnoreCase("H:0"))
		{
			DateFormat dateFormat = new SimpleDateFormat(IterKeys.DATEFORMAT_H_M);
			hourfinal = dateFormat.format(calendar.getTime());
		}
		if(typeHour.equalsIgnoreCase("HH:00"))
		{
			DateFormat dateFormat = new SimpleDateFormat(IterKeys.DATEFORMAT_HH_MM);
			hourfinal = dateFormat.format(calendar.getTime());
		}
		return hourfinal;
	}

	/**
	 * 
	 * @param groupId
	 * @param phpEnabled
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws SQLException
	 * @throws DocumentException
	 * @throws ServiceError
	 * @throws IOException
	 */
	private String getNewslettersXML(long groupId, boolean phpEnabled) 
			throws SecurityException, NoSuchMethodException, SQLException,DocumentException, ServiceError, IOException 
	{
		String newsletters = "<newsletters/>";
		
		String xmlData = NewsletterMgrLocalServiceUtil.getNewslettersXML( String.valueOf(groupId) );
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		dataRoot.addAttribute ("phpenabled", String.valueOf(phpEnabled));
		
		dataRoot.addElement("title").					addCDATA(_title);
		dataRoot.addElement("description").				addCDATA(_description);
		dataRoot.addElement("notsignin-msg").			addCDATA(_notsignin);
		dataRoot.addElement("subscribe-msg").			addCDATA(StringEscapeUtils.escapeJavaScript(_subscribe));
		dataRoot.addElement("unsubscribe-msg").			addCDATA(StringEscapeUtils.escapeJavaScript(_unsubscribe));
		dataRoot.addElement("manageerror-msg").			addCDATA(StringEscapeUtils.escapeJavaScript(_manageerror));
		dataRoot.addElement("notifysubscription").		addCDATA(String.valueOf(_notifySubscription));
		
		dataRoot.addElement("emailRepeatedError-msg").	addCDATA(StringEscapeUtils.escapeJavaScript(_emailRepeatedError));
		dataRoot.addElement("chkSubsError-msg").		addCDATA(StringEscapeUtils.escapeJavaScript(_chkSubsError));
		dataRoot.addElement("chkSubs-msg").				addCDATA(_chkSubs);
		// Se procesarán los anónimos si se notifican las suscripciones.
		// Cuando único no se notifican las suscripciones es cuando se está registrando un usuario o se está actualizando
		// y en el formulario de registro se incluye como campo las newsltter, casos en los que NO se quiere el acceso anónimo.
		dataRoot.addElement("allowAnonymous").			addCDATA(String.valueOf(_allowAnonymous && _notifySubscription));
		dataRoot.addElement("email").					addCDATA(_email);
		dataRoot.addElement("licenseAgreement").		addCDATA(_licenseAgreement);
		dataRoot.addElement("acceptLicenseError-msg").	addCDATA(StringEscapeUtils.escapeJavaScript(_acceptLicenseError));

		
		XPath xpath = SAXReaderUtil.createXPath("//option");
		List<Node> options = xpath.selectNodes(dataRoot);
		for(Node option:options)
		{
			String type = GetterUtil.getString(XMLHelper.getTextValueOf(option, "@type"), IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT_XML);
			
			Element eleOption = (Element) option;
			Attribute attrHour = eleOption.attribute("hour");
			
			if (type.equalsIgnoreCase(IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT_XML))
				attrHour.setValue( setHour(_nl_hour, XMLHelper.getTextValueOf(eleOption, "@hour")) );
			else
				eleOption.remove(attrHour);
			
			if (type.equalsIgnoreCase(IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT_XML))
			{
				XPath xpathDays = SAXReaderUtil.createXPath("days/day");
				List<Node> days = xpathDays.selectNodes(eleOption);
				for(Node day:days)
				{
					Element eleDay = (Element) day;
					int id = (int)XMLHelper.getLongValueOf(eleDay, "@id");
	
					switch (id)
					{
					case 1:
						eleDay.addCDATA(_nl_sunday);
						break;
					case 2:
						eleDay.addCDATA(_nl_monday);
						break;
					case 3:
						eleDay.addCDATA(_nl_tuesday);
						break;
					case 4:
						eleDay.addCDATA(_nl_wednesday);
						break;
					case 5:
						eleDay.addCDATA(_nl_thursday);
						break;
					case 6:
						eleDay.addCDATA(_nl_friday);
						break;
					case 7:
						eleDay.addCDATA(_nl_saturday);
						break;
					}
				}
			}
			else
			{
				XPath xpathDays = SAXReaderUtil.createXPath("days");
				List<Node> days = xpathDays.selectNodes(eleOption);
				for(Node day:days)
				{
					Element eleDay = (Element) day;
					eleOption.remove(eleDay);
				}	
			}
		}

		newsletters = dataRoot.asXML();
		_log.debug(newsletters);
		
		return newsletters;
	}
	
	/**
	 * 
	 * @param newslettersXML
	 * @return
	 * @throws ServiceError
	 * @throws NoSuchMethodException
	 */
	private static String getNewsLettersXSL( String newslettersXML ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("user-portlet")
						.append(File.separatorChar).append("html")
						.append(File.separatorChar).append("newsletterportlet")
						.append(File.separatorChar).append("newsletter.xsl").toString();

		result = XSLUtil.transformXML(newslettersXML, xslpath );
		_log.debug(result);
		
		return result;
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param groupId
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws SQLException
	 * @throws DocumentException
	 * @throws ServiceError
	 * @throws IOException
	 */
	public String getNewsletterPortletCode(HttpServletRequest request, HttpServletResponse response, long groupId) 
			throws SecurityException, NoSuchMethodException, SQLException, DocumentException, ServiceError, IOException
	{
		boolean isApacheRequest = PHPUtil.isApacheRequest(request);
		if (isApacheRequest)
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);

		String newslettersXML 	= getNewslettersXML(groupId, isApacheRequest);
		String result 			= getNewsLettersXSL(newslettersXML);

		CKEditorUtil.noInheritThemeCSS(result, request);

		if (response != null)
			response.setContentType("text/html");
		
		return result;
	}
}
