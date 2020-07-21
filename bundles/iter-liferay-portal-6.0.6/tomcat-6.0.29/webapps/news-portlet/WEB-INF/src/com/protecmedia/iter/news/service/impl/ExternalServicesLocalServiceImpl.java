/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.liferay.portal.util.IterRulesUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.news.service.base.ExternalServicesLocalServiceBaseImpl;
import com.protecmedia.iter.news.util.ExternalServiceUtil;

/**
 * The implementation of the external services local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.ExternalServicesLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.ExternalServicesLocalServiceUtil} to access the external services local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.ExternalServicesLocalServiceBaseImpl
 * @see com.protecmedia.iter.news.service.ExternalServicesLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class ExternalServicesLocalServiceImpl extends ExternalServicesLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(ExternalServicesLocalServiceImpl.class);
	
	private static final String SQL_GET_SERVICES_BY_GROUP = new StringBuilder()
	.append("SELECT serviceid,                               \n")
	.append("       name,                                    \n")
	.append("       url,                                     \n")
	.append("       method httpmethod,                       \n")
	.append("       proxy useproxy,                          \n")
	.append("       payload,                                 \n")
	.append("       headers httpheaders,                     \n")
	.append("       disabled,                                \n")
	.append("       CASE WHEN publicationDate IS NULL        \n")
	.append("              OR publicationDate < modifiedDate \n")
	.append("	         THEN 'false'                        \n")
	.append("            ELSE 'true'                         \n")
	.append("       END published                            \n")
	.append("FROM externalservice                            \n")
	.append("WHERE groupId=%s                                \n")
	.toString();
	
	public Document getExternalServices(String groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida la entrada
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId) && Validator.isNumber(groupId) && Long.valueOf(groupId) > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera lo servicios externos del grupo
		String sql = String.format(SQL_GET_SERVICES_BY_GROUP, groupId);
		String[] cdataNodes = new String[]{"payload", "httpheaders"};
		Document services = PortalLocalServiceUtil.executeQueryAsDom(sql, cdataNodes);
		
		// Monta el nodo httpheaders
		for (Node service : services.selectNodes("/rs/row"))
		{
			Node httpheaders = service.selectSingleNode("./httpheaders");
			if (httpheaders != null)
			{
				try
				{
					Document httpheadersDoc = SAXReaderUtil.read(httpheaders.getText());
					httpheaders.detach();
					((Element) service).add(httpheadersDoc.getRootElement().detach());
				}
				catch (DocumentException e)
				{
					httpheaders.setText(StringPool.BLANK);
				}
			}
		}
		
		// Retorna los servicios en formato XML
		return services;
	}
	
	private static final String SQL_GET_SERVICES = new StringBuilder()
	.append("SELECT serviceid,                               \n")
	.append("       groupId,                                 \n")
	.append("       name,                                    \n")
	.append("       url,                                     \n")
	.append("       method httpmethod,                       \n")
	.append("       proxy useproxy,                          \n")
	.append("       payload,                                 \n")
	.append("       headers httpheaders,                     \n")
	.append("       disabled,                                \n")
	.append("       CASE WHEN publicationDate IS NULL        \n")
	.append("              OR publicationDate < modifiedDate \n")
	.append("	         THEN 'false'                        \n")
	.append("            ELSE 'true'                         \n")
	.append("       END published                            \n")
	.append("FROM externalservice                            \n")
	.append("%s"                                                )
	.toString();
	
	private static final String SQL_GET_SERVICES_BY_ID = String.format(SQL_GET_SERVICES, "WHERE serviceid IN (%s)");
	private static final String SQL_GET_PENDING_PUBLICATION_SERVICES = String.format(SQL_GET_SERVICES, "WHERE groupId=%d AND (publicationDate IS NULL OR publicationDate < modifiedDate)");
	
	private Document getServicesById(String serviceIds) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida la entrada
		ErrorRaiser.throwIfNull(serviceIds, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String sqlServicesIds = StringUtil.merge(serviceIds.split(StringPool.COMMA), StringPool.COMMA, StringPool.APOSTROPHE);
		ErrorRaiser.throwIfNull(sqlServicesIds, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera los servicios indicados
		String sql = String.format(SQL_GET_SERVICES_BY_ID, sqlServicesIds);
		String[] cdataNodes = new String[]{"payload", "httpheaders"};
		Document services = PortalLocalServiceUtil.executeQueryAsDom(sql, cdataNodes);
		
		// Retorna los servicios en formato XML
		return buildHeaders(services);
	}
	
	private Document getPendingPublicationServices(long groupId) throws SecurityException, NoSuchMethodException
	{
		// Recupera los servicios indicados
		String[] cdataNodes = new String[]{"payload", "httpheaders"};
		Document services = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_PENDING_PUBLICATION_SERVICES, groupId), cdataNodes);
		
		// Retorna los servicios en formato XML
		return buildHeaders( services);
	}
	
	private Document buildHeaders(Document services)
	{
		// Monta el nodo httpheaders
		for (Node service : services.selectNodes("/rs/row"))
		{
			Node httpheaders = service.selectSingleNode("./httpheaders");
			if (httpheaders != null)
			{
				try
				{
					Document httpheadersDoc = SAXReaderUtil.read(httpheaders.getText());
					httpheaders.detach();
					((Element) service).add(httpheadersDoc.getRootElement().detach());
				}
				catch (DocumentException e)
				{
					httpheaders.setText(StringPool.BLANK);
				}
			}
		}
		
		// Retorna los servicios en formato XML
		return services;
	}
	
	private static final String SQL_INSERT_UPDATE_SERVICE = new StringBuilder()
	.append("INSERT INTO externalservice (serviceId, groupId, name, method, url, headers, payload, proxy)                \n")
	.append("VALUES('%s', %d, '%s', '%s', '%s', '%s', '%s', '%s')                                                        \n")
	.append("ON DUPLICATE KEY UPDATE name=VALUES(name), method=VALUES(method), url=VALUES(url), headers=VALUES(headers), \n")
	.append("                        payload=VALUES(payload), proxy=VALUES(proxy), modifiedDate=NOW() \n")
	.toString();
	
	private static final String SQL_SELECT_SERVICES_BY_GROUP = "SELECT serviceId, url, proxy FROM externalservice WHERE groupId=%d";
	
	private static final String SQL_DELETE_CACHED_CONTENT = "DELETE FROM externalservicecontent WHERE serviceId='%s' AND md5values<>'%s'";

	private static final String XPATH_GET_SERVICEID_BY_URL = "/rs/row[@url='%s']/@serviceId";
	private static final String XPATH_GET_USESPROXY_BY_URL = "/rs/row[@url='%s']/@proxy";
	
	public String setExternalServices(String xmlData) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Document serviceDoc = null;
		try { serviceDoc = SAXReaderUtil.read(xmlData); }
		catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		Node rootNode = serviceDoc.getRootElement();
		
		long groupId = XMLHelper.getLongValueOf(rootNode, "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String name = XMLHelper.getStringValueOf(rootNode, "row/@name");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(name), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String method = XMLHelper.getStringValueOf(rootNode, "row/@httpmethod");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(method), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		method = method.toUpperCase();
		ErrorRaiser.throwIfFalse("POST".equals(method) || "GET".equals(method), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String url = XMLHelper.getStringValueOf(rootNode, "row/@url");
		try { new URL(url); }
		catch (MalformedURLException e) {ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);}
		
		Node headers = rootNode.selectSingleNode("/rs/row/httpheaders");
		ErrorRaiser.throwIfFalse(headers != null && Validator.isNotNull(headers.asXML()), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Node payload = rootNode.selectSingleNode("/rs/row/payload");
		ErrorRaiser.throwIfFalse(payload != null && Validator.isNotNull(payload.asXML()), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String proxy = XMLHelper.getStringValueOf(rootNode, "row/@useproxy", "false");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(proxy) && ("true".equals(proxy) || "false".equals(proxy)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String serviceid = XMLHelper.getStringValueOf(rootNode, "row/@serviceid");
		
		// Recupera los servicios actuales del grupo
		Document currentServices = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_SERVICES_BY_GROUP, groupId));
		
		boolean needIterRulesChange = false;
		
		// Si no se indica serviceid, es un servicio nuevo
		if (Validator.isNull(serviceid))
		{
			// Comprueba si el servicio existe (Misma URL)
			serviceid = XMLHelper.getStringValueOf(currentServices, String.format(XPATH_GET_SERVICEID_BY_URL, url));
			// Si no existe, crea un nuevo id
			if (Validator.isNull(serviceid))
				serviceid = SQLQueries.getUUID();
			// Si existe, lanza un error
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_EXTERNAL_SERVICE_DUPLICATE_ZYX);
			
			// Si es un servicio nuevo y usa proxy, hay que regenerar las reglas de los Apache en el LIVE
			if ("true".equals(proxy))
				needIterRulesChange = true;
		}
		// Si es una modificación
		else
		{
			// Comprueba si ha cambiado el uso de proxy o si sigue usándolo pero ha cambiado su URL
			String oldProxy = XMLHelper.getStringValueOf(currentServices, String.format(XPATH_GET_USESPROXY_BY_URL, url));
			if ( (Validator.isNull(oldProxy) && "true".equals(proxy)) || (!proxy.equals(oldProxy)) )
				needIterRulesChange = true;
			
			// Comprueba si usa variables de contexto
			if (!(usesCtxVars(url) || usesCtxVars(headers.asXML()) || usesCtxVars(payload.asXML())))
			{
				// Si no las usa, se elimina todo el contenido cacheado para variables de contexto que pudiera tener
				PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_DELETE_CACHED_CONTENT, serviceid, ExternalServiceUtil.emptyStringMD5));
			}
		}
		
		// Inserción / Actualización
		String sqlName    = name.replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
		String sqlUrl     = url.replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
		String sqlPayload = payload.getText().replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
		String sqlHeaders = headers.asXML().replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
		String sql = String.format(SQL_INSERT_UPDATE_SERVICE, serviceid, groupId, sqlName, method, sqlUrl, sqlHeaders, sqlPayload, proxy);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		// Retorna el servicio insertado / actualizado, añadiendo la marca para regenerar las reglas de los apache si estamos en el LIVE
		if (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_LIVE) && needIterRulesChange)
		{
			Document service = getServicesById(serviceid);
			service.getRootElement().addAttribute("iterrules", "true");
			return service.asXML();
		}
		else
			return getServicesById(serviceid).asXML();
	}
	
	private static final String SQL_DELETE_SERVICES = "DELETE FROM externalservice WHERE serviceid IN (%s)";
	private static final String SQL_INSERT_DELETEDSERVICES = "INSERT INTO externalservicedeleted (groupId, serviceId) VALUES \n";
	private static final String SQL_CHECK_USE_PROXY = "SELECT count(*) useProxy FROM externalservice WHERE serviceId IN (%s) AND proxy = 'true'";
	
	public String deleteExternalServices(String xmlData) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, JSONException, SystemException, DocumentException
	{
		// Valida la entrada
		Document servicesDoc = null;
		try
		{
			servicesDoc = SAXReaderUtil.read(xmlData);
		}
		catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		
		long groupId = XMLHelper.getLongValueOf(servicesDoc.getRootElement(), "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		List<Node> services = servicesDoc.selectNodes("/rs/row");
		ErrorRaiser.throwIfFalse(services != null && services.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Comprueba si se están usando
		List<String> servicesInUse = new ArrayList<String>();
		for (Node service : services)
		{
			String serviceid = XMLHelper.getStringValueOf(service, "@id");
			Document dependencies = searchDependencies(serviceid);
			if (dependencies.selectNodes("/rs/row").size() > 0)
			{
				servicesInUse.add(serviceid);
			}
		}
		if (servicesInUse.size() > 0)
		{
			String servicesIds = StringUtil.merge(servicesInUse, StringPool.COMMA);
			Document servicesInUseDoc = getServicesById(servicesIds);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_EXTERNAL_SERVICE_IN_USE_ZYX, StringUtil.merge(XMLHelper.getStringValues(servicesInUseDoc.selectNodes("/rs/row"), "@name"), StringPool.NEW_LINE));
		}
		
		// Recoge los IDs de los ervicios a borrar
		String[] servicesIds = XMLHelper.getStringValues(services, "@id");
		// Si estamos en el LIVE, comprueba si alguno usa proxy, para regenerar las reglas de los Apache
		if (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_LIVE))
		{
			String checkSql = String.format(SQL_CHECK_USE_PROXY, StringUtil.merge(servicesIds, StringPool.COMMA, StringPool.APOSTROPHE));
			if (XMLHelper.getLongValueOf(PortalLocalServiceUtil.executeQueryAsDom(checkSql), "/rs/row[1]/@useProxy") > 0)
			{
				servicesDoc.getRootElement().addAttribute("iterrules", "true");
			}
		}
		// Elimina los servicios
		String sqlServicesIds = StringUtil.merge(servicesIds, StringPool.COMMA, StringPool.APOSTROPHE);
		ErrorRaiser.throwIfNull(sqlServicesIds, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Elimina los servicios indicados del PREVIEW
		String sql = String.format(SQL_DELETE_SERVICES, sqlServicesIds);
		PortalLocalServiceUtil.executeUpdateQuery(sql);

		// Si es el PREVIEW, da de alta los servicios pendientes de borrar en el LIVE
		if (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			StringBuilder sqlDeleted = new StringBuilder(SQL_INSERT_DELETEDSERVICES);
			for (int i=0; i<servicesIds.length; i++)
			{
				if (i > 0) sqlDeleted.append(StringPool.COMMA_AND_SPACE);
				String serviceId = servicesIds[i];
				sqlDeleted.append(String.format("(%d, '%s')", groupId, serviceId));
			}
			PortalLocalServiceUtil.executeUpdateQuery(sqlDeleted.toString());
		}
		
		return servicesDoc.asXML();
	}
	
	private static final String SQL_SET_DISABLED_SERVICE = "UPDATE externalservice SET disabled='%s', modifiedDate=NOW() WHERE serviceid='%s'";
	
	public void disableExternalService(String xmlData) throws ServiceError, IOException, SQLException
	{
		Document serviceDoc = null;
		try
		{
			serviceDoc = SAXReaderUtil.read(xmlData);
		}
		catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }

		String serviceid = XMLHelper.getStringValueOf(serviceDoc.getRootElement(), "/row/@serviceid");
		ErrorRaiser.throwIfNull(serviceid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String disable = XMLHelper.getStringValueOf(serviceDoc.getRootElement(), "/row/@disable");
		ErrorRaiser.throwIfNull(disable, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		disable = disable.toLowerCase();
		ErrorRaiser.throwIfFalse("true".equals(disable) || "false".equals(disable), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = String.format(SQL_SET_DISABLED_SERVICE, disable, serviceid);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	public void contentRequest(String serviceid, String environment) throws JSONException, SecurityException, ClientProtocolException, SystemException, ServiceError, NoSuchMethodException, IOException, DocumentException
	{
		boolean refreshBack = "PREVIEW".equals(environment) || "BackAndLive".equals(environment);
		boolean refreshLive = "LIVE".equals(environment)    || "BackAndLive".equals(environment);
		
		if (refreshBack && PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			Document dependencies = searchDependencies(serviceid);
			String[] plids = ArrayUtil.distinct(XMLHelper.getStringValues(dependencies.selectNodes("/rs/row[@environment='PREVIEW']"), "@plid"));
			for (String plid : plids)
			{
				new ExternalServiceUtil(serviceid).getServiceContent( Long.parseLong(plid));
			}
		}
		
		if (refreshLive)
		{
			if (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_LIVE))
			{
				Document dependencies = searchDependencies(serviceid);
				String[] plids = ArrayUtil.distinct(XMLHelper.getStringValues(dependencies.selectNodes("/rs/row[@environment='LIVE']"), "@plid"));
				for (String plid : plids)
				{
					new ExternalServiceUtil(serviceid).getServiceContent( Long.parseLong(plid));
				}
			}
			else
			{
				List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		        remoteMethodParams.add(new BasicNameValuePair("serviceClassName", "com.protecmedia.iter.news.service.ExternalServicesServiceUtil"));
		        remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "contentRequest"));
		        remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[serviceid, environment]"));
		        remoteMethodParams.add(new BasicNameValuePair("serviceid", serviceid));
		        remoteMethodParams.add(new BasicNameValuePair("environment", environment));
	        	JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
			}
		}
	}
	
	private static final String SQL_SEARCH_SERVICE_DEPENDENCIES = new StringBuilder()
	.append(" SELECT DISTINCT                                                                                                        \n")
	.append(" 	   '%%1$s' environment,                                                                                              \n")
	.append("       CASE WHEN Designer_PageTemplate.id_ > 0 THEN                                                                     \n")
	.append("               'Model'                                                                                                  \n")
	.append("             WHEN CatalogPage.plid IS NOT NULL THEN                                                                     \n")
	.append(" 	          'Catalog'                                                                                                  \n")
	.append("             WHEN PortletPreferences.plid = 0 AND ownerType = 5 THEN                                                    \n")
	.append("               'Shared Configuration'                                                                                   \n")
	.append("             ELSE                                                                                                       \n")
	.append("               'Section'                                                                                                \n")
	.append("       END element,                                                                                                     \n")
	.append("       CASE WHEN Designer_PageTemplate.id_ > 0 THEN                                                                     \n")
	.append("               Designer_PageTemplate.name                                                                               \n")
	.append("             WHEN CatalogPage.plid IS NOT NULL THEN                                                                     \n")
	.append(" 	            Layout.friendlyUrl                                                                                       \n")
	.append("             ELSE                                                                                                       \n")
	.append("               extractvalue(Layout.name, '/root/name[1]')                                                               \n")
	.append(" 	    END name,                                                                                                        \n")
	.append("       CASE WHEN PortletPreferences.plid = 0 AND ownerType = 5 THEN                                                     \n")
	.append("            PortletItem.name                                                                                            \n")
	.append("       ELSE                                                                                                             \n")
	.append("            CASE extractvalue(preferences, '/portlet-preferences/preference[name=''portlet-setup-title-es_ES'']/value') \n")
	.append("            WHEN '' THEN portletpreferences.portletId                                                                   \n")
	.append("            ELSE extractvalue(preferences, '/portlet-preferences/preference[name=''portlet-setup-title-es_ES'']/value') \n")
	.append("            END                                                                                                         \n")
	.append(" 	    END portlet,                                                                                                     \n")
	.append(" 	    Layout.plid plid                                                                                                 \n")
	.append(" FROM portletpreferences                                                                                                \n")
	.append(" LEFT JOIN Layout ON PortletPreferences.plid = Layout.plid						                                         \n")
	.append(" LEFT JOIN Designer_PageTemplate ON Designer_PageTemplate.layoutId = Layout.plid                                        \n")
	.append(" LEFT JOIN CatalogPage ON CatalogPage.plid = Layout.plid 						                                         \n")
	.append(" LEFT JOIN Catalog ON CatalogPage.catalogId = Catalog.catalogId					                                     \n")
	.append(" LEFT JOIN PortletItem ON PortletItem.portletItemId = PortletPreferences.ownerId                                        \n")
	.append("                                                                                                                        \n")
	.append(" WHERE portletpreferences.portletId LIKE 'proxyportlet_WAR_newsportlet%%%%'                                             \n")
	// ESTÁ USANDO EL SERVICIO O ESTÁ ASOCIADO A UNA CONFIGURACIÓN COMPARTIDA QUE HACE USO DEL SERVICIO
	.append("   AND (                                                                                                                \n")
	.append("      portletpreferences.preferences REGEXP CONCAT('<value>', '%%2$s', '</value>')                                      \n")
	.append(" 	   OR (                                                                                                              \n")
	.append("         portletpreferences.ownerType = 3                                                                               \n")
	.append("         AND                                                                                                            \n")
	.append(" 	      portletpreferences.preferences REGEXP (                                                                        \n")
	.append("            SELECT GROUP_CONCAT(userName SEPARATOR '|')                                                                 \n")
	.append("            FROM portletpreferences                                                                                     \n")
	.append("            INNER JOIN PortletItem ON PortletItem.portletItemId = PortletPreferences.ownerId                            \n")
	.append("            WHERE portletpreferences.preferences REGEXP CONCAT('<value>', '%%2$s', '</value>')                          \n")
	.append("         )                                                                                                              \n")
	.append("      )                                                                                                                 \n")
	.append("   )                                                                                                                    \n")
	// ES UNA CONFIGURACIÓN COMPARTIDA O EL PORTLET  SE USA EN ALGÚN LAYOUT
	.append("   AND (                                                                                                                \n")
	.append("      %s                                                                                                                \n")
	.append("      OR typeSettings LIKE CONCAT('%%%%', PortletPreferences.portletId, '%%%%')                                         \n")
	.append("   )                                                                                                                    \n")
	.toString();
	
	private static final String SQL_SEARCH_SHARED_CONFIG_IN_USE = new StringBuilder()
	.append("(                                                                                                                       \n")
	.append("         portletpreferences.ownerType = 5                                                                               \n")
	.append("         AND (                                                                                                          \n")
	.append("            SELECT COUNT(*) inUse                                                                                       \n")
	.append("            FROM PortletPreferences                                                                                     \n")
	.append("            INNER JOIN Layout ON PortletPreferences.plid = Layout.plid                                                  \n")
	.append("	                         AND typeSettings LIKE CONCAT('%%', PortletPreferences.portletId, '%%')                      \n")
	.append("            WHERE ownerType = 3 AND type_ = 'portlet'                                                                   \n")
	.append("            AND preferences REGEXP CONCAT('<value>', PortletItem.userName, '</value>')                                  \n")
	.append("		  )                                                                                                              \n")
	.append("      )                                                                                                                 \n")
	.toString();
	
	private static final String SQL_SEARCH_SERVICE_DEPENDENCIES_BACK = String.format(SQL_SEARCH_SERVICE_DEPENDENCIES, "portletpreferences.ownerType = 5 \n");
	private static final String SQL_SEARCH_SERVICE_DEPENDENCIES_LIVE = String.format(SQL_SEARCH_SERVICE_DEPENDENCIES, SQL_SEARCH_SHARED_CONFIG_IN_USE);
	
	public Document searchDependencies(String serviceid) throws ServiceError, SecurityException, NoSuchMethodException, JSONException, ClientProtocolException, SystemException, IOException, DocumentException
	{
		ErrorRaiser.throwIfNull(serviceid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW) ? 
				     String.format(SQL_SEARCH_SERVICE_DEPENDENCIES_BACK, PropsValues.ITER_ENVIRONMENT, serviceid) :
				     String.format(SQL_SEARCH_SERVICE_DEPENDENCIES_LIVE, PropsValues.ITER_ENVIRONMENT, serviceid);
		Document dependencies = PortalLocalServiceUtil.executeQueryAsDom(sql);
		Element dependenciesRoot = dependencies.getRootElement();
		
		// Si estamos en el PREVIEW, pide las dependencias al LIVE
		if (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
	        remoteMethodParams.add(new BasicNameValuePair("serviceClassName", "com.protecmedia.iter.news.service.ExternalServicesServiceUtil"));
	        remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "searchDependencies"));
	        remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[serviceid]"));
	        remoteMethodParams.add(new BasicNameValuePair("serviceid", serviceid));
        
        	JSONObject liveResponse = JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
        	String liveResponseValue = liveResponse.getString("returnValue");
        	if (Validator.isNotNull(liveResponseValue))
        	{
        		Document liveDependencies = SAXReaderUtil.read(liveResponseValue);
        		for (Node liveDependency : liveDependencies.selectNodes("/rs/row"))
        		{
        			dependenciesRoot.add(liveDependency.detach());
        		}
        	}
		}
		
		return dependencies;
	}

	private static final String SQL_GET_SERVICES_TO_DELETE_IN_LIVE = "SELECT serviceId id FROM externalservicedeleted WHERE groupId=%s";
	private static final String SQL_GET_CLEAR_DELETES = "DELETE FROM externalservicedeleted WHERE groupId=%s AND serviceId IN (%s)";
	private static final String SQL_GET_UPDATE_PUBLICATION_DATE = "UPDATE externalservice SET publicationDate=NOW(), modifiedDate=NOW() WHERE serviceId IN (%s)";
	
	public void publishToLive(String groupId, String serviceids) throws SecurityException, ServiceError, NoSuchMethodException, JSONException, ClientProtocolException, SystemException, IOException, SQLException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId) && Validator.isNumber(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long scopeGroupId = Long.valueOf(groupId);
		String groupName = null;
		try
		{
			groupName = GroupLocalServiceUtil.getGroup(scopeGroupId).getName();
		}
		catch (Throwable th) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		
		Document exportData = SAXReaderUtil.createDocument();
		Element exportRoot = exportData.addElement("services");
		exportRoot.addAttribute("groupName", groupName);
		Element exportPublish = exportRoot.addElement("topublish");
		Element exportDeletes = exportRoot.addElement("todelete");
		
		// Añade los servicios a publicar. Si no llegan ids, coge lo pendiente.
		Document servicesToPublishDoc = null;
		if (Validator.isNull(serviceids))
			servicesToPublishDoc = getPendingPublicationServices(scopeGroupId);
		else
			servicesToPublishDoc = getServicesById(serviceids);
		
		List<Node> servicesToPublish = servicesToPublishDoc.selectNodes("/rs/row");
		boolean publications = servicesToPublish.size() > 0;
		if (publications)
		{
			for (Node serviceToPublish : servicesToPublish)
			{
				Element rs = exportPublish.addElement("rs");
				rs.add(serviceToPublish.detach());
			}
		}
		
		// Añade los servicios a eliminar
		String sql = String.format(SQL_GET_SERVICES_TO_DELETE_IN_LIVE, groupId);
		Document servicesToDelete = PortalLocalServiceUtil.executeQueryAsDom(sql);
		boolean deletes = servicesToDelete.selectNodes("/rs/row").size() > 0;
		if (deletes)
			exportDeletes.add(servicesToDelete.getRootElement().detach());
		
		// Publica en el LIVE
		if (publications || deletes)
		{
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
	        remoteMethodParams.add(new BasicNameValuePair("serviceClassName", "com.protecmedia.iter.news.service.ExternalServicesServiceUtil"));
	        remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "importData"));
	        remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[data]"));
	        remoteMethodParams.add(new BasicNameValuePair("data", exportData.asXML()));
        
	        JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
        
	        // Elimina los borrados pendientes
	        if (deletes)
	        {
				String[] servicesIds = XMLHelper.getStringValues(exportDeletes.selectNodes("./rs/row"), "@id");
				String sqlServicesIds = StringUtil.merge(servicesIds, StringPool.COMMA, StringPool.APOSTROPHE);
				String sqlClearDeletes = String.format(SQL_GET_CLEAR_DELETES, groupId, sqlServicesIds);
				if (Validator.isNotNull(sqlClearDeletes))
					PortalLocalServiceUtil.executeUpdateQuery(sqlClearDeletes);
	        }
			
			// Actualiza la fecha de publicación
	        if (publications)
	        {
				String[] servicesIds = XMLHelper.getStringValues(servicesToPublish, "@serviceid");
				String sqlServicesIds = StringUtil.merge(servicesIds, StringPool.COMMA, StringPool.APOSTROPHE);
				String sqlUpdatePublishDate = String.format(SQL_GET_UPDATE_PUBLICATION_DATE, sqlServicesIds);
				PortalLocalServiceUtil.executeUpdateQuery(sqlUpdatePublishDate);
	        }
		}
	}
	
	public void importData(String data) throws ServiceError, SecurityException, IOException, SQLException, NoSuchMethodException, JSONException, SystemException, DocumentException, InterruptedException, ExecutionException
	{
		ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document importDoc = null;
		try
		{
			importDoc = SAXReaderUtil.read(data);
		}
		catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		
		boolean importProcess = GetterUtil.getBoolean(XMLHelper.getStringValueOf(importDoc.getRootElement(), "@importProcess"), false);
		
		if (importProcess)
		{
			importFromIterAdmin(importDoc);
		}
		else
		{
			importFromPublication(importDoc);
		}
	}
	
	private void importFromIterAdmin(Document importDoc) throws SecurityException, ServiceError, NoSuchMethodException, IOException, SQLException
	{
		Element root = importDoc.getRootElement();
		long groupId = XMLHelper.getLongValueOf(root, "@groupId");
		boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist"), false);
		
		// Recupera los servicios actuales
		Document currentServices = getExternalServices(String.valueOf(groupId));
		
		// Procesa los servicios a importar
		List<Node> services = importDoc.selectNodes("/rs/row");
		for(Node service : services)
		{
			String url = XMLHelper.getStringValueOf(service, "@url");
			Node existingService = currentServices.selectSingleNode("/rs/row[@url='" + url + "']");
			
			// No existe el servicio
			if (existingService == null)
			{
				((Element) service).addAttribute("serviceid", StringPool.BLANK);
			}
			// Existe, pero se puede actualizar
			else if (updtIfExist)
			{
				((Element) service).addAttribute("serviceid", XMLHelper.getStringValueOf(existingService, "@serviceid"));
			}
			// Existe y no se puede actualizar
			else
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
	 					String.format("%s(%s)", IterAdmin.IA_CLASS_EXTERNAL_SERVICE, XMLHelper.getStringValueOf(service, "@url")));
			}
			setExternalServices( String.format("<rs groupid=\"%d\">%s</rs>", groupId, service.asXML()) );
		}
	}
	
	private void importFromPublication(Document importDoc) throws ServiceError, SecurityException, IOException, SQLException, NoSuchMethodException, JSONException, SystemException, DocumentException, InterruptedException, ExecutionException
	{
		// Recupera el grupo
		String groupName = XMLHelper.getStringValueOf(importDoc.getRootElement(), "@groupName");
		long groupId = 0;
		try
		{
			groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		}
		catch (Throwable th) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean regenerateIterRules = false;
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		
		class MyCallable implements Callable<Boolean>
		{
			private long groupId;
			private Document importDoc;
			
			public MyCallable (long groupId, Document importDoc)
	        {
				this.groupId = groupId;
				this.importDoc = importDoc;
	        }
			
			@Override
	        public Boolean call() throws SecurityException, DocumentException, ServiceError, IOException, SQLException, NoSuchMethodException, JSONException, SystemException
			{
			 	Boolean regenerateIterRules = false;
			 	
				// Procesa los servicios a crear / actualizar
				List<Node> servicesToPublish = importDoc.selectNodes("/services/topublish/rs");
				for (Node service : servicesToPublish)
				{
					((Element) service).addAttribute("groupid", String.valueOf(groupId));
					regenerateIterRules = regenerateIterRules || modifyIterRules(setExternalServices(service.asXML()));
				}
				
				// Procesa los servicios a eliminar
				Node toDelete = importDoc.selectSingleNode("/services/todelete/rs");
				if (Validator.isNotNull(toDelete))
				{
					((Element) toDelete).addAttribute("groupid", String.valueOf(groupId));
					regenerateIterRules = regenerateIterRules || modifyIterRules(deleteExternalServices(toDelete.asXML()));
				}
				
				return regenerateIterRules;
	        }
	    };
	    
	    MyCallable callable = new MyCallable(groupId, importDoc);
	    Future<Boolean> future = executor.submit(callable);
	    regenerateIterRules = future.get();
	    executor.shutdown();
		
		// Si se hamodificado alguna configuración que afecte al uso de proxy, regenera las reglas en los Apache
		if (regenerateIterRules)
			IterRulesUtil.regenerateApacheIterRules(groupId);
	}
	
	private boolean modifyIterRules(String xmlService) throws DocumentException
	{
		Document service = SAXReaderUtil.read(xmlService);
		return "true".equals(XMLHelper.getStringValueOf(service, "/rs/@iterrules"));
	}
	
	public String exportData(Long groupId) throws SecurityException, ServiceError, NoSuchMethodException
	{
		return getExternalServices(String.valueOf(groupId)).asXML();
	}
	
	private boolean usesCtxVars(String content)
	{
		Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
		Matcher m = p.matcher(content);
		
		if (m.find()) return true;
		
		return false;
	}
}