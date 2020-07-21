package com.protecmedia.iter.user.util;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;

public class UserEntitlementsMgr
{
	private static Log _log = LogFactoryUtil.getLog(UserEntitlementsMgr.class);
	
	public static String getEntitlements(String friendlyGroupURL, String aboid, String unexpectedError) throws DocumentException, ParseException, UnsupportedEncodingException, NoSuchMethodException, SecurityException
	{
		String data = "";
		
		friendlyGroupURL = friendlyGroupURL.replace("/", ".");
		String url = PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_URL, friendlyGroupURL) );
		
		// Modo autónomo. Iter es el gestor de suscripciones.
		if ("self".equals(url))
		{
			String sql = "SELECT entitlements from iterusers WHERE usrid='%s'";
			Document d = PortalLocalServiceUtil.executeQueryAsDom(String.format(sql, aboid));
			data = XMLHelper.getStringValueOf(d.getRootElement(), "/rs/row/@entitlements");
		}
		// Modo externo. Integración de sistema de suscripciones externo.
		else
		{
			String urlParameters = "usrid=" + aboid;
			int connTimeout = GetterUtil.getInteger(PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_CONN_TIMEOUT, friendlyGroupURL) ), 15000);
			int readTimeout = GetterUtil.getInteger(PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_READ_TIMEOUT, friendlyGroupURL) ), 15000);
			
			if(Validator.isNotNull(url))
			{
				data = LoginUtil.connectServer(url, urlParameters, connTimeout, readTimeout, unexpectedError);
			}
			else
			{
				// Generar cookie sin productos
				_log.debug("Not found \"iter.subscription.server.url" + friendlyGroupURL + "\" in portal-ext.properties");
			}
		}
		
		return data;
	}
	
	public static JSONObject getEntitlementsInfo(String entitlements, String aboid, long giftTime, boolean dataFromCache) throws DocumentException, ParseException, UnsupportedEncodingException
	{
		JSONObject productsData = null;
		
		Document xmlDoc = SAXReaderUtil.read(entitlements);
		
		//Mensaje de respuesta del servicio de suscripciones
		//Independientemente del código de la respuesta, siempre hay que recuperar el mensaje para mostrárselo al usuario
		String msg = GetterUtil.getString( XMLHelper.getTextValueOf(xmlDoc, "/itwresponse/field[@name='msg']/string"), StringPool.BLANK);
		
		//Código de respuesta del servicio de suscripciones
		String code = GetterUtil.getString( XMLHelper.getTextValueOf(xmlDoc, "/itwresponse/field[@name='code']/string"), StringPool.BLANK);

		if(code.toUpperCase().equals(IterKeys.OK))
		{
			productsData = getFormattedProductsWithExpires(xmlDoc, giftTime, dataFromCache);
			productsData.put(UserKeys.RAW_PRODUCT_LIST, entitlements);
			productsData.put(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE, IterKeys.OK);
			if( !msg.isEmpty() )
				productsData.put(UserKeys.SUBSCRIPTION_SYSTEM_MSG, msg);
		}
		else if(code.toUpperCase().equals(IterKeys.KO))
		{
			deleteEntitlementsCache( aboid );
			productsData = JSONFactoryUtil.createJSONObject();
			productsData.put(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE, IterKeys.KO);
			productsData.put(UserKeys.SUBSCRIPTION_SYSTEM_MSG, msg );
		}
		else if(code.toUpperCase().equals(UserKeys.ITER_KO))
		{
			productsData = JSONFactoryUtil.createJSONObject();
			productsData.put(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE, UserKeys.ITER_KO);
			productsData.put(UserKeys.SUBSCRIPTION_SYSTEM_MSG, msg );
			productsData.put(UserKeys.HTTP_STATUS_CODE, 
					GetterUtil.getString( XMLHelper.getTextValueOf(xmlDoc, "/itwresponse/field[@name='httpcode']/string"), StringPool.BLANK) );
			productsData.put(UserKeys.HTTP_STATUS_LINE, 
					GetterUtil.getString( XMLHelper.getTextValueOf(xmlDoc, "/itwresponse/field[@name='httpmessage']/string"), StringPool.BLANK) );
		}
		
		return productsData;
	}
	
	private static JSONObject getFormattedProductsWithExpires(Document xml, long giftTime, boolean dataFromCache) throws ParseException, UnsupportedEncodingException
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		
		StringBuffer products = new StringBuffer();
		// Se inicializa a -1 para que sea de sesión
		String expires = "-1";
		// Si hay algún producto ya caducado se pondrá a TRUE
		boolean expiredProduct = false;
		Date minValidProd = null;

		SimpleDateFormat sdf = new SimpleDateFormat(UserKeys.DATE_FORMAT);
		long now = Calendar.getInstance().getTime().getTime();
		Date minimumDate = null;

		List<Node> productsList = xml.getRootElement().selectNodes("field[@name='output']/array/object");
		
		for(Node productNode : productsList)
		{
			//Nombre del producto
			String productName = XMLHelper.getTextValueOf(productNode, "field[@name='entitle']/string", StringPool.BLANK);
			
			if( Validator.isNotNull(productName) )
			{
				String productNameBase64 = Base64.encode(productName.getBytes(Digester.ENCODING));
				
				//Fecha vigencia del producto
				String productDate = XMLHelper.getTextValueOf(productNode, "field[@name='expires']/string", StringPool.BLANK);
				
				Date productExpires = sdf.parse(productDate);
				
				if( dataFromCache && productExpires.getTime() < now )
				{
					expiredProduct = true;
					break;
				}
				
				if( (Validator.isNull(minValidProd) && productExpires.getTime() > now) || 
					    (productExpires.getTime() > now && minValidProd.getTime() > productExpires.getTime()) )
				{
					minValidProd = productExpires;
				}
				
				if( giftTime>0 )
				{
					long currentMSEC = productExpires.getTime();
					if( currentMSEC<=now )
					{
						Date d = new Date( now+giftTime );
						productExpires = sdf.parse( sdf.format(d) );
					}
				}
				
				if( (Validator.isNull(minimumDate) && productExpires.getTime() > now) || 
				    (productExpires.getTime() > now && minimumDate.getTime() > productExpires.getTime()) )
				{
					minimumDate = productExpires;
				}
				
				if(products.length()==0)
					products.append(productNameBase64 + PayCookieUtil.PRODUCT_DATE_SEPARATOR + sdf.format(productExpires));
				else
				{
					products.append(PayCookieUtil.PRODUCT_SEPARATOR + productNameBase64 + PayCookieUtil.PRODUCT_DATE_SEPARATOR + sdf.format(productExpires));
				}
			}
			else
				_log.debug("Product name is empty:\n\t" + productNode.asXML());
		}
		
		if( dataFromCache && (expiredProduct || Validator.isNull(minimumDate) || (minimumDate.getTime() < now)) )
			expires = "";
		else
			expires = getExpires4Cookie(minimumDate, now);

		result.put(UserKeys.ENCODED_PRODUCT_LIST, products.toString());
		result.put(UserKeys.EXPIRES, expires);
		
		if(Validator.isNotNull(minValidProd))
			result.put(UserKeys.MIN_VALID_PROD, minValidProd.getTime());
		else
			result.put(UserKeys.MIN_VALID_PROD, minValidProd);
		
		_log.debug("Product list: " + products.toString());
		_log.debug("First product expires: " + (Validator.isNotNull(minValidProd) ? minValidProd.toString() : StringPool.BLANK) );
		_log.debug("Expires: " + expires);
		
		return result;
	}

	public static String getExpires4Cookie(Date d, long now)
	{
		String expires = "";
		int defaultExpires = 0;
		String cookieExpires = PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_COOKIE_EXPIRES_DEFAULT);
		
		if(Validator.isNotNull(cookieExpires))
			defaultExpires = Integer.parseInt(cookieExpires);
		else
			defaultExpires = UserKeys.COOKIE_EXPIRES;
		
		if(Validator.isNotNull(d) && d.getTime() > now)
	    {
	    	long diff = (d.getTime() - now) / 1000;
	    	if(diff > Integer.MIN_VALUE && diff < Integer.MAX_VALUE )
	    		expires = String.valueOf(diff);
	    	else
	    		expires = String.valueOf(Integer.MAX_VALUE);
	    }
	    else
	    {
	    	expires = String.valueOf(defaultExpires);
	    }
		
		return expires;
	}

	public static void cacheEntitlements(String aboid, String entXml, long minValidProd) throws ParseException
	{
		String updt = "update iterusers set entitlements='%s', aboinfoexpires='%s' where aboid='%s' ";
		
		SimpleDateFormat sdf = new SimpleDateFormat(UserKeys.DATE_FORMAT);
		String timeunit = HotConfigUtil.getKey(UserKeys.TIMESTAMP_UNIT,"day");
		long timevalue = HotConfigUtil.getKey(UserKeys.TIMESTAMP_VALUE,1L);
		long cacheValidtime = getMsecTime(timeunit, timevalue);
		long now =  Calendar.getInstance().getTime().getTime();
		Date EntitlementsCacheExpires = null;
		
		if( Validator.isNotNull(minValidProd) )
			EntitlementsCacheExpires = new Date( Math.min(cacheValidtime+now, minValidProd) );
		else
			EntitlementsCacheExpires = new Date( cacheValidtime+now );
		
		String q = String.format(updt, StringEscapeUtils.escapeSql(entXml), sdf.format(EntitlementsCacheExpires), StringEscapeUtils.escapeSql(aboid));
		
		try 
		{
			PortalLocalServiceUtil.executeUpdateQuery(q);
		} 
		catch (Exception e)
		{
			_log.error("Impossible to save entitlements");
			_log.error(e);
		}
	}
	
	private static void deleteEntitlementsCache(String aboid)
	{
		if( Validator.isNotNull(aboid) )
		{
			String deleteExpires = "update iterusers set entitlements=NULL, aboinfoexpires=NULL where aboid='%s' ";
			
			String q = String.format( deleteExpires, StringEscapeUtils.escapeSql(aboid) );
			
			try 
			{
				PortalLocalServiceUtil.executeUpdateComittedQuery(q);
			} 
			catch (Exception e)
			{
				_log.error("Impossible to delete entitlements");
				_log.error(e);
			}
		}
	}

	
	public static long getMsecTime(String timeunit, long timevalue)
	{
		long msec = 0;
		
		if(timeunit.equalsIgnoreCase("hour"))
		{
			msec = TimeUnit.MILLISECONDS.convert(timevalue, TimeUnit.HOURS);
		}
		else if(timeunit.equalsIgnoreCase("day"))
		{
			msec = TimeUnit.MILLISECONDS.convert(timevalue, TimeUnit.DAYS);
		}
		else if(timeunit.equalsIgnoreCase("month"))
		{
			msec = TimeUnit.MILLISECONDS.convert(timevalue*30, TimeUnit.DAYS);
		}
		
		return msec;
	}
}
