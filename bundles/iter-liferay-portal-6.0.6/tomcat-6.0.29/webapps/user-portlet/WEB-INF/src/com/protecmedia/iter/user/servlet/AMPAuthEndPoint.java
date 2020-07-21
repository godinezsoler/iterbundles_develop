package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.user.util.AuthenticateUser;
import com.protecmedia.iter.user.util.IterRegisterQueries;
import com.protecmedia.iter.user.util.UserEntitlementsMgr;
import com.protecmedia.iter.user.util.UserKeys;

public class AMPAuthEndPoint extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(AMPAuthEndPoint.class);
	
	static private final String DB_DATE_FORMAT 		= "yyyy-MM-dd HH:mm:ss.S";
	
	private static final String IS_GRANTED_ADVANCED_PAYWALL = new StringBuilder(
		"SELECT COUNT(1)																									\n").append(
		"FROM iterpaywall_product																							\n").append(
		"INNER JOIN iterpaywall_product_related ON iterpaywall_product_related.paywallproductid = iterpaywall_product.id	\n").append(
		"INNER JOIN product ON iterpaywall_product_related.productid = product.productid									\n").append(
		"INNER JOIN articleproduct ON articleproduct.productId = product.productId											\n").append(
		"  WHERE iterpaywall_product.groupId = %d																			\n").append(
		"    AND iterpaywall_product.pname IN ('%s')																		\n").append(
		"    AND iterpaywall_product.ptype = 0																				\n").append(
		"    AND articleproduct.articleId = '%s'																			\n").toString();

	private static final String IS_GRANTED_PAYWALL = new StringBuilder(
		"SELECT COUNT(1)																									\n").append(
		"FROM product																										\n").append(
		"INNER JOIN articleproduct ON articleproduct.productId = product.productId											\n").append(
		"  WHERE product.groupId = %d																						\n").append(
		"    AND product.name IN ('%s')																						\n").append(
		"    AND articleproduct.articleId = '%s'    																		\n").toString();

	
	private static final String GET_USRID = new StringBuilder(
		"SELECT usrid, email, aboid, entitlements, aboinfoexpires	\n").append(
		"FROM iterusers_readers 									\n").append(
		"INNER JOIN iterusers USING (usrid)							\n").append(
		"  WHERE groupid = %d										\n").append(
		"   AND readerid = '%s'										\n").toString();
	
	private static final String HAS_AVANCED_PRODUCTS = new StringBuilder(
		"SELECT COUNT(1) 			\n").append(
		"FROM iterpaywall_product	\n").append(
		"  WHERE groupId = %d		\n").toString();
	
	private static final String PHP_RESPONSE = new StringBuilder(
		"<?php                     											\n").append(
		"	header('Access-Control-Allow-Origin: *');						\n").append(
		"	header('Access-Control-Allow-Credentials: true');				\n").append(				
		"	header('Access-Control-Max-Age: 86400');						\n").append(		
		"	header('Content-Type: application/json; charset=utf-8');		\n").append(
		"	header('Cache-Control: private, no-store, no-cache, must-revalidate');	\n").append(
		"																	\n").append(		
		"	$result = '%s';													\n").append(
		"	echo $result;													\n").append(
		"?>																	\n").toString();
	

	protected void doGet( HttpServletRequest request, HttpServletResponse response )
	{
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.addHeader("Access-Control-Allow-Credentials", "true");
			response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate"); // HTTP 1.1.
			response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			response.setDateHeader("Expires", 0); // Proxies.
			response.setHeader("Access-Control-Max-Age", "86400");
			response.setHeader(WebKeys.ITER_RESPONSE_NEEDS_PHP, "1");

			String jsonResponse = runAuth(request).toString();
	
			response.setStatus(HttpServletResponse.SC_OK);
			printResponse(jsonResponse, request, response);
		}
		catch (Exception e)
		{
			_log.error(e);
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			printResponse("", request, response);
		}
		finally
		{
			IterRequest.unsetOriginalRequest();
		}
	}
	
	private JSONObject runAuth(HttpServletRequest request) throws PortalException, SystemException, SecurityException, NoSuchMethodException, ServiceError, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, DocumentException, IOException, SQLException, ParseException
	{
		JSONObject jsonResponse = JSONFactoryUtil.createJSONObject();
		
		// Se comprueba si el artículo está restringido
		String articleId = request.getParameter("articleid");
		ErrorRaiser.throwIfNull(articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long groupId = PortalUtil.getScopeGroupId(request);
		String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
		
		// granted: indica si el usuario tiene o no concedido el acceso al artículo.
		// Si no tuviese productos sería accesible
		boolean granted = !PortalLocalServiceUtil.hasProducts(articleId, groupId);
		
		// isRegistered: indica si el usuario está registrado en el sistema.
		boolean isRegistered 	= false;
		
		// isSubscriber: indica si el usuario, además de estar registrado, es un suscriptor (tiene aboid procedente del sistema de suscripciones)
		boolean isSubscriber 	= false;
		
		
		String readerId = request.getParameter("rid");
		ErrorRaiser.throwIfNull(readerId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = String.format(GET_USRID, groupId, readerId);
		_log.debug(sql);
		
		// Se obtiener el usrid a partir del readerId
		Document usrDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
		String usrId = XMLHelper.getStringValueOf(usrDom, "/rs/row/@usrid");
		
		isRegistered = Validator.isNotNull(usrId);
		
		if (isRegistered)
		{
			_log.trace("Registered user");
			String aboId = XMLHelper.getStringValueOf(usrDom, "/rs/row/@aboid");
			
			// Si no está el aboid en el usuario se busca en el sistema de autentificación
			if ( Validator.isNull(aboId) )
			{
				_log.trace("The aboid is missing. Checking with subscription system");
				String email = XMLHelper.getStringValueOf(usrDom, "/rs/row/@email");
				ErrorRaiser.throwIfFalse( Validator.isNotNull(email) );
				
				JSONObject authenticationInfo = AuthenticateUser.doAuthentication(friendlyGroupURL, email, StringPool.BLANK, StringPool.BLANK);

				if (authenticationInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(IterKeys.OK))
				{
					_log.trace("SSubscription system response OK");
					
					// Se recupera el aboid
					aboId = IterUserTools.encryptGDPR( StringEscapeUtils.escapeSql(authenticationInfo.getString(UserKeys.SUSCRIPTOR_ID)) );
					if (Validator.isNotNull(aboId))
					{
						ErrorRaiser.throwIfFalse( Validator.isNotNull(email) );
						
						// Se actualiza la BBDD con el aboid
						sql = String.format( IterRegisterQueries.UPDATE_ABOID, aboId, usrId);
						
						_log.debug(sql);
						PortalLocalServiceUtil.executeUpdateQuery(sql);
					}
				}
			}
			
			isSubscriber = Validator.isNotNull(aboId);
			
			if (_log.isTraceEnabled())
				_log.trace( String.format("Is subscriber %b (%s)", isSubscriber, GetterUtil.getString(aboId, "")));
			
			// Se analiza si el usuario tiene acceso al artículo. Es un artículo con productos y el usuario es suscriptor
			if (!granted && isSubscriber)
			{
				granted = isGranted(usrDom, friendlyGroupURL, aboId, articleId, groupId);
			}
		}
		
		jsonResponse.put("granted", granted);
		
		// grantReason: su valor es fijo y solo aparece cuando granted=true.
		if (granted)
			jsonResponse.put("grantReason", "SUBSCRIBER");
		
		JSONObject jsonData = JSONFactoryUtil.createJSONObject();
		jsonData.put("isRegistered", isRegistered);
		jsonData.put("isSubscriber", isSubscriber);
		
		jsonResponse.put("data", jsonData);
		
		if (_log.isTraceEnabled())
			_log.trace( String.format("runAuth response: %s", jsonResponse.toString()) );

		return jsonResponse;
	}
	
	private boolean isGranted(Document usrDom, String friendlyGroupURL, String aboId, String articleid, long groupId) throws ParseException, UnsupportedEncodingException, SecurityException, DocumentException, NoSuchMethodException, ServiceError
	{
		boolean granted = false;
		
		// Se obtiene la fecha de expiración de los entitlements registrados en BBDD
		String infoExpires 	= XMLHelper.getStringValueOf(usrDom, "/rs/row/@aboinfoexpires");
		long aboinfoexpires = Validator.isNotNull(infoExpires) ? new SimpleDateFormat(DB_DATE_FORMAT).parse( infoExpires ).getTime() : 0L;
		
		String entitlements	= XMLHelper.getStringValueOf(usrDom, "/rs/row/@entitlements");
		_log.trace(entitlements);
		
		// No existen entitlements en BBDD o están caducados
		boolean dataFromCache = !(Validator.isNull(entitlements) || aboinfoexpires <= Calendar.getInstance().getTime().getTime());
		if (!dataFromCache)
		{
			// Hay que renovarlos
			_log.trace("Need refresh entitlements");
			entitlements = UserEntitlementsMgr.getEntitlements(friendlyGroupURL, aboId, StringPool.BLANK);
			_log.trace(entitlements);
		}
		
		JSONObject entitlementsInfo = UserEntitlementsMgr.getEntitlementsInfo(entitlements, aboId, 0L, dataFromCache);
		if (entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(IterKeys.OK))
		{
			SimpleDateFormat sdf = new SimpleDateFormat(UserKeys.DATE_FORMAT);
			long now = Calendar.getInstance().getTime().getTime();

			List<String> productNames = new ArrayList<String>();
			
			// Se obtiene la lista de productos que NO hayan caducado
			List<Node> products = SAXReaderUtil.read(entitlements).selectNodes("/itwresponse/field[@name='output']/array/object");
			for (Node productNode : products)
			{
				String productDate 	= XMLHelper.getTextValueOf(productNode, "field[@name='expires']/string", StringPool.BLANK);
				Date productExpires = sdf.parse(productDate);
				
				if (productExpires.getTime() >= now )
				{
					// Aún no ha expirado
					productNames.add( XMLHelper.getTextValueOf(productNode, "field[@name='entitle']/string", StringPool.BLANK) );
				}
			}
			
			if (!productNames.isEmpty())
			{
				// Hay que determinar si este listado de productos es de Paywall avanzado o no, y si alguno de los productos del usuario lo tiene el artículo 
				String names = StringUtil.merge(productNames, "','");
				String sql	 = String.format(isAvancedPaywall(groupId) ? IS_GRANTED_ADVANCED_PAYWALL : IS_GRANTED_PAYWALL, groupId, names, articleid);
				_log.debug(sql);
				
				granted = Integer.valueOf(PortalLocalServiceUtil.executeQueryAsList(sql).get(0).toString()) > 0;
			}
		}
		else
		{
			// Ha fallado la gestión de los entitlements 
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_GET_USER_ENTITLEMENTS_ZYX, 
			GetterUtil.getString2(entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG), entitlementsInfo.getString(UserKeys.HTTP_STATUS_LINE)));
		}

		
		return granted;
	}
	
	private boolean isAvancedPaywall(long groupId)
	{
		return Integer.valueOf(PortalLocalServiceUtil.executeQueryAsList(String.format(HAS_AVANCED_PRODUCTS, groupId)).get(0).toString()) > 0;
	}

	private void printResponse(String responseBody, HttpServletRequest request, HttpServletResponse response)
	{
	    PrintWriter out = null;
		try
		{
			if (PHPUtil.isApacheRequest(request))
			{
				// Es necesario porque el APACHE elimina 'Cache-Control: private, no-cache'
				responseBody = String.format(PHP_RESPONSE, responseBody);
			}
			
			if (_log.isTraceEnabled())
				_log.trace( String.format("Servlet response (%d)\n %s", responseBody.isEmpty() ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK, responseBody) );
			
			out = response.getWriter();
			out.print(responseBody);
		}
		catch (IOException e)
		{
			_log.error(e);
		}
		finally
		{
			if (out != null)
			{
				out.flush();
				out.close();
			}
		}
	}

}
