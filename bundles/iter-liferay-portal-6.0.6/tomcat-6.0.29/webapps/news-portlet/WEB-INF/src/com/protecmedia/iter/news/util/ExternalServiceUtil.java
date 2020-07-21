package com.protecmedia.iter.news.util;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portlet.CtxvarUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class ExternalServiceUtil
{
	private static Log _log = LogFactoryUtil.getLog(ExternalServiceUtil.class);
	
	private String serviceId;
	private long groupid;
	private boolean disabled;
	private IterHttpClient.Method method;
	private String url;
	private String payload;
	private HashMap<String, String> header = new HashMap<String, String>();
	private boolean proxy;
	
	private boolean initialized = false;
	
	Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
	
	private static final String SQL_GET_SERVICE_BY_ID = new StringBuilder()
	.append("SELECT serviceid, groupId, name, url, method, proxy, payload, headers, disabled \n")
	.append("FROM externalservice WHERE serviceid='%s'                                         ")
	.toString();

	private static final String SQL_SET_SERVICE_LAST_REQUEST = "UPDATE externalservice SET lastRequestDate=NOW() WHERE serviceId='%s'";
	private static final String SQL_GET_CONTENT_FROM_CACHE = "SELECT content FROM externalservicecontent WHERE serviceId='%s' AND md5values='%s'";
	private static final String SQL_SET_CONTENT_IN_CACHE = new StringBuilder()
		.append("INSERT INTO externalservicecontent (serviceId, md5values, content, modifiedDate) \n")
		.append("VALUES ('%s', '%s', '%s', NOW())                                                 \n")
		.append("ON DUPLICATE KEY UPDATE content=VALUES(content), modifiedDate=NOW()                ")
		.toString();
	
	public static final String emptyStringMD5 = "d41d8cd98f00b204e9800998ecf8427e";
	
	public ExternalServiceUtil(String serviceId)
	{	
		try
		{
			// Valida la entrada
			ErrorRaiser.throwIfNull(serviceId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			this.serviceId = serviceId;
			
			// Recupera el servicio
			String sql = String.format(SQL_GET_SERVICE_BY_ID, serviceId);
			Document service = PortalLocalServiceUtil.executeQueryAsDom(sql);
			Node serviceConfig = service.selectSingleNode("/rs/row");
			// Si no hay servicio, lanzar excepción para registrar en monitor
			ErrorRaiser.throwIfNull(serviceConfig, "XYZ_E_SERVICE_NOT_FOUND_ZYX");
	
			// Recupera la configuración del servicio
			groupid = XMLHelper.getLongValueOf(serviceConfig, "@groupId");
			disabled = GetterUtil.getBoolean(XMLHelper.getStringValueOf(serviceConfig, "@disabled"), false);
			method = IterHttpClient.Method.valueOf(XMLHelper.getStringValueOf(serviceConfig, "@method"));
			payload = XMLHelper.getStringValueOf(serviceConfig, "@payload");
			processHeaders(XMLHelper.getStringValueOf(serviceConfig, "@headers"));
			processUrl(serviceConfig);
			
			initialized = true;
		}
		catch (Throwable th)
		{
			initialized = false;
			// Registra el error en el monitor
			IterMonitor.logEvent(groupid, IterMonitor.Event.ERROR, new Date(), "Unable to initialize external service", serviceId, th);
		}
	}
	
	private void processUrl(Node serviceConfig) throws IOException, MalformedURLException, PortalException, SystemException
	{
		url = XMLHelper.getStringValueOf(serviceConfig, "@url");
		proxy = GetterUtil.getBoolean(XMLHelper.getStringValueOf(serviceConfig, "@proxy"), false);
		
		// Si usa proxy y estamos en el LIVE
		if (proxy && PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_LIVE))
		{
			// Recupera la IP del Apache que se encuentra en modo AJP
			ApacheHierarchy ah = new ApacheHierarchy();
			String[] apacheMasterList = ah.getMasterList();
			
			if (apacheMasterList.length > 0)
			{
				// Construye la URL para llamar a través del Apache
				URL serviceUrl = new URL(url);
				StringBuilder urlBuilder = new StringBuilder(apacheMasterList[0]).append("/_external_").append(serviceUrl.getPath());
				if (serviceUrl.getQuery() != null)
					urlBuilder.append(StringPool.QUESTION).append(serviceUrl.getQuery());
				url = urlBuilder.toString();
			}
			
			// Recupera el nombre del sitio para el header Host
			String virtualhost = LayoutSetLocalServiceUtil.getLayoutSet(groupid, false).getVirtualHost();
			
			// Añade el header Host
			header.put(WebKeys.HOST, virtualhost);
			
			// Añade el User Agent *ITERWEBCMS*
			header.put(HttpHeaders.USER_AGENT, WebKeys.USER_AGENT_ITERWEBCMS);
		}
	}
	
	private void processHeaders(String httpheaders) throws DocumentException
	{
		if (httpheaders != null)
		{
			Document httpheadersDoc = SAXReaderUtil.read(httpheaders);
			List<Node> headers = httpheadersDoc.selectNodes("/httpheaders/header");
			if (headers.size() > 0)
			{
				for (Node httpHeader : headers)
				{
					String name = XMLHelper.getStringValueOf(httpHeader, "name");
					String value = XMLHelper.getStringValueOf(httpHeader, "value");
					
					if (Validator.isNotNull(name) && Validator.isNotNull(value))
						header.put(name, value);
				}
			}
		}
	}
	
	public String getServiceContent(long plid)
	{
		return getServiceContent(plid, false);
	}
	
	public String getServiceContent(long plid, boolean forceRequest)
	{
		String htmlContent = StringPool.BLANK;
		
		if (initialized)
		{
			try
			{
				// Sustituye las posibles variables de contexto y genera el MD5 del valor de las variables
				String md5values = processContextVars(plid);
				
				// Si el servicio está deshabilitado, recupera el contenido cacheado
				if (disabled && !forceRequest)
				{
					htmlContent = getContentFromCache(md5values);
				}
				// Si el servicio está habilitado, pide el contenido y si es correcto lo cachea
				else
				{
					// Actualiza la fecha de última petición
					{
						String sql = String.format(SQL_SET_SERVICE_LAST_REQUEST, serviceId);
						PortalLocalServiceUtil.executeUpdateQuery(sql);
					}
					
					// Pide el contenido
					IterHttpClient iterHttpClient = new IterHttpClient.Builder(method, url).payLoad(payload).headers(header).build();
					htmlContent = iterHttpClient.connect();
					
					// Si la respuesta es correcta
					if (iterHttpClient.validResponse())
					{
						// Cachea el contenido
						String sql = String.format(SQL_SET_CONTENT_IN_CACHE, serviceId, md5values, new String(Base64.encodeBase64(htmlContent.getBytes())).trim());
						PortalLocalServiceUtil.executeUpdateQuery(sql);
					}
					// Si la respuesta es errónea, recupera el contenido cacheado
					else
					{
						htmlContent = getContentFromCache(md5values);
					}
				}
			}
			catch (Throwable th)
			{
				_log.error(th);
				String text = Validator.isNotNull(htmlContent) ? "Serving cached content." : "No cached content to serve.";
				IterMonitor.logEvent(groupid, IterMonitor.Event.ERROR, new Date(), "External service content request failed. " + text, serviceId, th);
			}
		}
		
		return htmlContent;
	}
	
	private String processContextVars(long plid) throws SecurityException, NoSuchMethodException, NoSuchAlgorithmException
	{
		StringBuilder varValues = new StringBuilder();
		
		// Recupera la jerarquía de plids
		String plidHierarchy = SectionUtil.getPlidHierarchy3(groupid, plid);
		
		// Sustituye las variables de contexto de la url
		url = replaceContextVars(url, plidHierarchy, varValues);
		
		// Sustituye las variables de contexto del payload
		payload = replaceContextVars(payload, plidHierarchy, varValues);
		
		// Sustituye las variables de contexto de los headers
		for (String key : header.keySet())
		{
			header.put(key, replaceContextVars(header.get(key), plidHierarchy, varValues));
		}
		
		// Genera el MD5 del valor de las variables
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] messageDigest = md.digest(varValues.toString().getBytes());
		String md5values = new BigInteger(1, messageDigest).toString(16);
		
		return md5values;
	}
	
	private String replaceContextVars(String text, String plidHierarchy, StringBuilder varValues)
	{
		// Recupera las variables de contexto del servicio
		Matcher m = p.matcher(text);
		
		while (m.find())
		{
			String varName = m.group(1);
			String varValue = CtxvarUtil.getContextVarValue(groupid, varName, plidHierarchy);
			
			// Si está definida y tiene valor, lo sustituye
			if (varValue != null)
			{
				// Anota el valor de la variable para calcular el MD5
				varValues.append(varValue);
				// Sustituye la variable de contexto por su valor
				text = text.replaceFirst("\\$\\{" + varName + "\\}", varValue);
			}
		}
		
		return text;
	}
	
	private String getContentFromCache(String md5values) throws SecurityException, NoSuchMethodException
	{
		String htmlContent = StringPool.BLANK;
		
		String sql = String.format(SQL_GET_CONTENT_FROM_CACHE, serviceId, md5values);
		Document contentDoc = PortalLocalServiceUtil.executeQueryAsDom(sql);
		htmlContent = XMLHelper.getStringValueOf(contentDoc, "/rs/row/@content");
		
		if (htmlContent != null)
			htmlContent = new String(Base64.decodeBase64(htmlContent));
		
		return htmlContent;
	}
}
