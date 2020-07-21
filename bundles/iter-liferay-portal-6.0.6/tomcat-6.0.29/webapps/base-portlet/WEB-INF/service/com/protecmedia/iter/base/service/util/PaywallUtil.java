package com.protecmedia.iter.base.service.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

public class PaywallUtil
{
	private static Log _log = LogFactoryUtil.getLog(PaywallUtil.class);
	
	private static final SimpleDateFormat MYSQL_DATE_FORMAT 				= new SimpleDateFormat(WebKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss_S);
	private static final SimpleDateFormat ISO_DATE_FORMAT	 				= new SimpleDateFormat(IterKeys.DATEFORMAT_YYYYMMDDHHMMss);
	private static final SimpleDateFormat DATE_DEFAULT_FORMAT 				= new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	
	private static final String NO_SIGNED_HTML								= "<div class=\"paywallstatus_notsignin-msg\">%s</div>";
	
	public static final int DEFAULT_DAYS_BEFORE 							= 30;
	public static final int SESSION_TYPE 									= 0;
	public static final int TYPE1 											= 1;
	public static final int TYPE2 											= 2;
	public static final int COOKIE_TYPE										= 3;
	
	public static final String ORDER_CLAUSE									= "ORDER BY %s %s";
	
	public static final String GET_SESSIONS_BY_USER							= new StringBuilder(	"SELECT i.*, ipp.pname FROM iterpaywall_product ipp "				).append(
																									"INNER JOIN iterpaywall_session i ON i.paywallprodid = ipp.id " 	).append(
																									"WHERE i.userid='%s' %s %s %s"										).toString();
	
	public static final String GET_PAYWALL_PRODUCTS_ACCESS_TYPE1_BY_USER	= new StringBuilder(	"SELECT i.*, ipp.pname FROM iterpaywall_product ipp "				).append(
																									"INNER JOIN iterpaywall_tipo1 i ON i.paywallprodid = ipp.id " 		).append(
																									"WHERE i.userid='%s' %s %s %s"										).toString();

	public static final String GET_PAYWALL_PRODUCTS_ACCESS_TYPE2_BY_USER	= new StringBuilder(	"SELECT i.*, ipp.pname, j.urlTitle FROM iterpaywall_product ipp "	).append(
																									"INNER JOIN iterpaywall_tipo2 i ON i.paywallprodid = ipp.id " 		).append(
																									"INNER JOIN JournalArticle j ON i.idart = j.articleId " 			).append(
																									"WHERE i.userid='%s' %s %s %s"										).toString();
	
	
	public static String getPaywallStatusHTML(String phpenabled, int viewMode, String userid, String title, 
											  String cookieName, String dateName, String userAgentName, String ipName, 
											  String productName, String noSignedMsg, String articleName, int daysBefore,
											  String expiresDateName, HttpServletRequest request) 
													  throws SecurityException, DocumentException, ServiceError, NoSuchMethodException
	{
		String html = StringPool.BLANK;
		
		try
		{
			HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
			Object showPaywallStatusObj = PublicIterParams.get(originalRequest, IterKeys.REQUEST_ATTRIBUTE_IS_FORWARDED_PAGE);
			ErrorRaiser.throwIfNull(showPaywallStatusObj, IterErrorKeys.XYZ_ITR_E_PAYWALL_STATUS_INVALID_ACCESS_ZYX);
			
			boolean showPaywallStatus = GetterUtil.getBoolean(showPaywallStatusObj.toString());
			ErrorRaiser.throwIfFalse(showPaywallStatus, IterErrorKeys.XYZ_ITR_E_PAYWALL_STATUS_INVALID_ACCESS_ZYX);
			
			Document xmlData = SAXReaderUtil.read("<rs/>");
			if(Validator.isNotNull(userid))
			{
				if(viewMode == SESSION_TYPE)
				{
					xmlData = PortalLocalServiceUtil.executeQueryAsDom(
							String.format(GET_SESSIONS_BY_USER, userid, getLimitDateQuery(daysBefore), 
									String.format(ORDER_CLAUSE, "i.fecha", "DESC"), StringPool.BLANK), new String[]{"pname"});
				}
				else if(viewMode == TYPE1)
				{
					xmlData = PortalLocalServiceUtil.executeQueryAsDom(
							String.format(GET_PAYWALL_PRODUCTS_ACCESS_TYPE1_BY_USER, userid, getLimitDateQuery(daysBefore), 
									String.format(ORDER_CLAUSE, "i.fecha", "DESC"), StringPool.BLANK), new String[]{"pname"});
				}
				else if(viewMode == TYPE2)
				{
					xmlData = PortalLocalServiceUtil.executeQueryAsDom(
							String.format(GET_PAYWALL_PRODUCTS_ACCESS_TYPE2_BY_USER, userid, getLimitDateQuery(daysBefore), 
									String.format(ORDER_CLAUSE, "i.fecha", "DESC"), StringPool.BLANK), new String[]{"pname"});
				}
				else if(viewMode == COOKIE_TYPE)
				{
					List<String[]> productsAndExpires = PayCookieUtil.getProductsAndExpires(request);
					if(productsAndExpires.size() > 0)
					{
						Element result = SAXReaderUtil.read("<rs/>").getRootElement();
						for(String[] currentProductAndExpires:productsAndExpires)
						{
							Element row = result.addElement("row");
							String pname = new String(Base64.decode(currentProductAndExpires[0]));
							String expires = DATE_DEFAULT_FORMAT.format(ISO_DATE_FORMAT.parse(currentProductAndExpires[1]));
							
							row.addElement("pname").addCDATA(pname);
							row.addAttribute("expires", expires);
						}
						xmlData = result.getDocument();
					}
				}
			}
				
			Element dataRoot = xmlData.getRootElement();
			dataRoot.addAttribute("phpenabled", phpenabled);
			dataRoot.addAttribute("type", String.valueOf(viewMode));
			dataRoot.addElement("title").addCDATA(title);
			dataRoot.addElement("cookieName").addCDATA(cookieName);
			dataRoot.addElement("dateName").addCDATA(dateName);
			dataRoot.addElement("userAgentName").addCDATA(userAgentName);
			dataRoot.addElement("ipName").addCDATA(ipName);
			dataRoot.addElement("productName").addCDATA(productName);
			dataRoot.addElement("noSignedMsg").addCDATA(noSignedMsg);
			dataRoot.addElement("articleName").addCDATA(articleName);
			dataRoot.addElement("expiresDateName").addCDATA(expiresDateName);
			dataRoot.addElement("typeDiv").addCDATA(getDivByType(viewMode));
			dataRoot.addElement("typeDivClose").addCDATA("</div>");
			
			XPath xpath = SAXReaderUtil.createXPath("//row");
			List<Node> nodes = xpath.selectNodes(dataRoot);
			Map<String, Integer> sessionMap = new HashMap<String, Integer>();
			int index = 0;
			for(Node node:nodes)
			{
				Element e = (Element)node;
				
				//cookieid
				Attribute cookieidAttr = e.attribute("cookieid");
				if(cookieidAttr != null)
				{
					String cookieid = cookieidAttr.getValue();
					Integer currentId = sessionMap.get(cookieid);
					if(currentId != null)
					{
						cookieidAttr.setValue(currentId.toString());
					}
					else
					{
						sessionMap.put(cookieid, index);
						cookieidAttr.setValue(String.valueOf(index));
						index++;
					}
				}
				
				//useragent
				Attribute useragentAttr = e.attribute("useragent");
				if(useragentAttr != null)
				{
					String useragent = normalizedUserAgent(useragentAttr.getValue());
					useragentAttr.setValue(useragent);
				}
				
				//fecha
				if(viewMode == SESSION_TYPE || viewMode == TYPE2)
				{
					Attribute fecha = e.attribute("fecha");
					if(fecha != null)
						fecha.setValue(DATE_DEFAULT_FORMAT.format(MYSQL_DATE_FORMAT.parse(fecha.getValue())));
				}
			}
				
			html = transformXML(dataRoot.asXML());
		}		
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
			
			html = String.format(NO_SIGNED_HTML, noSignedMsg);
		}
		
		return html;
	}
	
	private static String transformXML(String xml) throws ServiceError, NoSuchMethodException
	{
		String xslPath = new StringBuilder().append(File.separatorChar).append("user-portlet").append(File.separatorChar)
											.append("html").append(File.separatorChar).append("paywall-status-portlet")
											.append(File.separatorChar).append("paywallstatus.xsl").toString();

		return XSLUtil.transformXML(xml, xslPath);
	}
	
	private static String getLimitDateQuery(int daysBefore)
	{
		Calendar limitCalendar = Calendar.getInstance();
		limitCalendar.add(Calendar.DAY_OF_MONTH, -daysBefore);
		
		String limitDate = MYSQL_DATE_FORMAT.format(limitCalendar.getTime());

		return new StringBuilder().append(" AND i.fecha > '").append(limitDate).append("' ").toString();
	}
	
	private static String getDivByType(int type) throws ServiceError, NoSuchMethodException
	{
		String div = "<div class=\"paywallstatus_type_session\">";
		if(type == TYPE1)
			div = "<div class=\"paywallstatus_type1\">";
		else if(type == TYPE2)
			div = "<div class=\"paywallstatus_type2\">";
		else if(	type == COOKIE_TYPE)
			div = "<div class=\"paywallstatus_type_cookie\">";
		return div;
	}
	
	public static String normalizedUserAgent(String useragent)
	{
		String normalized = "Other";
		try
		{
			useragent = useragent.toLowerCase();
			if(useragent.contains("firefox") && !useragent.contains("seamonkey"))
			{
				normalized = "Firefox";
			}
			else if(useragent.contains("seamonkey"))
			{
				normalized = "Seamonkey";
			}
			else if(useragent.contains("chrome") && !useragent.contains("chromium"))
			{
				normalized = "Chrome";
			}
			else if(useragent.contains("chromium"))
			{
				normalized = "Chromium";
			}
			else if(useragent.contains("safari")&& !useragent.contains("chromium") && !useragent.contains("chrome"))
			{
				normalized = "Safari";
			}
			else if(useragent.contains("opera"))
			{
				normalized = "Opera";
			}
			else if(useragent.contains("msie"))
			{
				normalized = "Internet Explorer";
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return normalized;
	}
	
}