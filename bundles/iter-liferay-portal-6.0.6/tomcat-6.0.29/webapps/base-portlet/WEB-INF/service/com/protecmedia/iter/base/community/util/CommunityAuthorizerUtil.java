package com.protecmedia.iter.base.community.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.community.shortener.URLShortener;
import com.protecmedia.iter.base.community.shortener.URLShortenerFactory;

public class CommunityAuthorizerUtil
{
	private static Log _log = LogFactoryUtil.getLog(CommunityAuthorizerUtil.class);
			
	// NOMBRES DE REDES SOCIALES
	public static final String FACEBOOK        = "facebook";
	public static final String INSTANT_ARTICLE = "instant article";
	public static final String GOOGLE          = "googleplus";
	public static final String TWITTER         = "twitter";
	public static final String APPLE_NEWS      = "apple";
	public static final String WPN             = "notification";
	
	// PARÁMETROS PARA LA INICIALIZACIÓN
	public static final String REDIRECT_URI             = "redirectURI";
	public static final String COMMUNITY_NAME           = "communityName";
	public static final String GROUP_ID                 = "groupId";
	public static final String FACEBOOK_PAGE_NAME       = "page";
	public static final String FACEBOOK_INSTANT_ARTICLE = "instantArticle";
	
	private CommunityAuthorizerUtil() { throw new AssertionError(); }
	
	public static final String GET_KEY_BY_SOCIAL_TYPE = new StringBuilder()
	.append("SELECT c.%s FROM itersocialconfig c ")
	.append("INNER JOIN itersocial s ON s.itersocialid=c.itersocialid ")
	.append("WHERE c.groupid=%d AND s.socialname='%s'")
	.toString();
	
	public static String getConfigPublicKey(String socialName, long groupId) throws SecurityException, NoSuchMethodException
	{
		return XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_KEY_BY_SOCIAL_TYPE, "publicKey", groupId, socialName)), "/rs/row/@publicKey");
	}
	
	public static String getConfigSecretKey(String socialName, long groupId) throws SecurityException, NoSuchMethodException
	{
		return XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_KEY_BY_SOCIAL_TYPE, "secretKey", groupId, socialName)), "/rs/row/@secretKey");
	}
	
	private static final String GET_URL_SHORTENER_BY_SOCIAL_TYPE = new StringBuilder()
	.append(" SELECT c.shortener                                         \n")
	.append(" FROM itersocialconfig c                                    \n")
	.append(" LEFT JOIN DLFileEntry d ON c.loginwithfileentry = d.uuid_  \n")
	.append(" INNER JOIN itersocial s ON s.itersocialid = c.itersocialid \n")
	.append(" WHERE c.groupid=%d AND s.socialname='%s' LIMIT 1           \n")
	.toString();
	
	public static URLShortener getConfigUrlShortener(String socialName, long groupId) throws SecurityException, NoSuchMethodException
	{
		URLShortener shortener = null;
		
		String shortenerService = XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_URL_SHORTENER_BY_SOCIAL_TYPE, groupId, socialName)), "/rs/row/@shortener");
		if (Validator.isNotNull(shortenerService))
		{
			shortener = URLShortenerFactory.getShortener(shortenerService, groupId);
		}
		
		return shortener;
	}
	
	public static String getArticle(long groupId, String articleId) throws SystemException
	{
		return getArticle(groupId, articleId, null);
	}
	
	public static String getArticle(long groupId, String articleId, URLShortener shortener) throws SystemException
	{
		return getArticle(groupId, StringPool.BLANK, articleId, null);
	}
	
	public static String getArticle(long groupId, String prefix, String articleId, URLShortener shortener) throws SystemException
	{
		try
		{
			// Recupera el contenido
			String articleUrl = IterURLUtil.getArticleURL(groupId, articleId, null, false, false);
			
			if (Validator.isNotNull(articleUrl))
			{
				articleUrl = new StringBuilder(IterSecureConfigTools.getConfiguredHTTPS(groupId) ? Http.HTTPS_WITH_SLASH : Http.HTTP_WITH_SLASH)
					.append(LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost())
					.append(prefix)
					.append(articleUrl)
					.toString();
				
				// Utiliza acortador de URLs
				if (shortener != null)
				{
					articleUrl = shortener.shorten(articleUrl, articleId);
				}
			}
			else
			{
				throw new SystemException("XYZ_UNABLE_TO_RETRIEVE_ARTICLE_URL_ZYX");
			}
			
			return articleUrl;
		}
		catch (Throwable th)
		{
			_log.error(th.getMessage());
			_log.trace(th);
			throw new SystemException("XYZ_UNABLE_TO_RETRIEVE_ARTICLE_ZYX", th);
		}
	}
	
	public static void buildResponsePage(HttpServletResponse response, String account, String credentials) throws IOException
	{
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(ContentTypes.TEXT_HTML_UTF8);
		PrintWriter printer = response.getWriter();
		
		printer.print("<!dDOCTYPE html>");
		printer.print("<html>");
		printer.print("<head>");
		printer.print("  <script type='text/javascript' src='/html/js/jquery/jqueryiter.js?v=" + IterGlobal.getLastIterWebCmsVersion() + "&env=preview'></script>");
		printer.print("  <script type='text/javascript'>var jQryIter = jQuery.noConflict(true);</script>");
		printer.print("</head>");
		printer.print("<body>");
		printer.print("  <div id='MCMEvent:credentials' name='MCMEvent:credentials' account='" + account + "' credentials='" + credentials + "' style='display: none;'></div>");
		
		printer.print("  <script>");
		printer.print("    jQryIter(document).ready(function() {");
		printer.print("      var event = jQryIter(\"#MCMEvent\\\\:credentials\");");
		printer.print("      event.click(function(e){ });");
		printer.print("      event.trigger('click');");
		printer.print("    });");
		printer.print("  </script>");
		
		printer.print("</body>");
		printer.print("</html>");
		
		printer.flush();
		printer.close();
	}
	
	public static void buildResponseErrorPage(HttpServletResponse response, int status, String code, Throwable trace)
	{
		buildResponseErrorPage(response, status, code, ExceptionUtils.getStackTrace(trace));
	}
	
	public static void buildResponseErrorPage(HttpServletResponse response, int status, String code, String msg)
	{
		try
		{
			response.setStatus(status);
			response.setContentType(ContentTypes.TEXT_HTML_UTF8);
			PrintWriter printer = response.getWriter();
			
			printer.print("<!dDOCTYPE html>");
			printer.print("<html>");
			printer.print("<head>");
			printer.print("  <script type='text/javascript' src='/html/js/jquery/jqueryiter.js?v=" + IterGlobal.getLastIterWebCmsVersion() + "&env=preview'></script>");
			printer.print("  <script type='text/javascript'>var jQryIter = jQuery.noConflict(true);</script>");
			printer.print("</head>");
			printer.print("<body>");
			printer.print("  <div id='MCMEvent:error' name='MCMEvent:error' code='" + code + "' msg='" + StringEscapeUtils.escapeXml(msg) + "' style='display: none;'></div>");
			
			printer.print("  <script>");
			printer.print("    jQryIter(document).ready(function() {");
			printer.print("      var event = jQryIter(\"#MCMEvent\\\\:error\");");
			printer.print("      event.click(function(e){ });");
			printer.print("      event.trigger('click');");
			printer.print("    });");
			printer.print("  </script>");
			
			printer.print("</body>");
			printer.print("</html>");
		    
			printer.flush();
			printer.close();
		}
		catch (IOException e)
		{
			_log.error(e);
		}
	}
	
	public static HashMap<String, String> mapQueryString(String queryString)
	{
		// Inicializa el mapa de parámetros.
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		// Si tiene QueryString, procesa los parámetros.
		if (Validator.isNotNull(queryString))
		{
			// Separa los parámetros.
			String[] queryStringParams = queryString.split(StringPool.AMPERSAND);
			for (String param : queryStringParams)
			{
				// Añade el parámetro al TreeMap.
				String[] pair = param.split(StringPool.EQUAL);
				parameters.put(pair[0], pair.length == 2 ? pair[1] : StringPool.BLANK);
			}
		}
		// Devuelve el mapa de parámetros.
		return parameters;
	}
	
	public static String getMonitorTrace(String accountId, String accountName, Throwable exception)
	{
		String trace = ExceptionUtils.getStackTrace(exception);
		return getMonitorTrace(accountId, accountName, trace);
	}
	
	public static String getMonitorTrace(String accountId, String accountName, String result)
	{
		Document xmlTrace = SAXReaderUtil.createDocument();
		Element root = xmlTrace.addElement("publication");

		root.addElement("account").addText(accountId);
		root.addElement("accountName").addText(accountName);
		root.addElement("result").addCDATA(result);
		
		return xmlTrace.asXML();
	}
}
