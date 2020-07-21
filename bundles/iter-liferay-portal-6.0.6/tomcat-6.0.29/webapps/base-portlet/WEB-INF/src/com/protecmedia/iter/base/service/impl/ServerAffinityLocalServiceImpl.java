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

package com.protecmedia.iter.base.service.impl;

import java.io.IOException;
import java.sql.SQLException;

import com.liferay.portal.kernel.cluster.ClusterTools;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterDNS;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.community.publisher.CommunityPublisherController;
import com.protecmedia.iter.base.service.affinity.IServerAffinityProcess;
import com.protecmedia.iter.base.service.base.ServerAffinityLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.cache.CacheInvalidationController;
import com.protecmedia.iter.base.service.util.ServerAffinityConstants;

/**
 * The implementation of the server affinity local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ServerAffinityLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ServerAffinityLocalServiceUtil} to access the server affinity local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ServerAffinityLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ServerAffinityLocalServiceUtil
 */
public class ServerAffinityLocalServiceImpl extends ServerAffinityLocalServiceBaseImpl implements ServerAffinityConstants
{
	private static Log _log = LogFactoryUtil.getLog(ServerAffinityLocalServiceImpl.class);
	
	
	/**
	 * <p>Inicia tareas de tipo global.</p>
	 * <p>Si no hay servidor de afinidad configurado, se registra aquel con la
	 * propiedad {@code scheduler.enabled} a {@code true} y arranca la tarea.</p>
	 * 
	 * @param taskKind El nombre de la tarea a arrancar.
	 * 
	 * @throws ServiceError          Si el {@code taskKind} no corresponde a ninguna tarea. 
	 * @throws NoSuchMethodException Si ocurre un error al recuperar el servidor de afinidad de la tarea.
	 * @throws SecurityException     Si ocurre un error al recuperar el servidor de afinidad de la tarea.
	 * @throws SQLException          Si ocurre un error al registrarse como servidor de afinidad de la tarea.
	 * @throws IOException           Si ocurre un error al registrarse como servidor de afinidad de la tarea. 
	 */
	public void initServerGlobalTask(String taskKind) throws ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		if (Heartbeat.canLaunchProcesses())
		{
			ErrorRaiser.throwIfFalse(Validator.isNotNull(taskKind), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera la información de la tarea
			Document doc = getServerAffinityAsDom(GroupMgr.getGlobalGroupId(), TASKKIND.valueOf(taskKind));
			if (doc != null)
			{
				String serverName = XMLHelper.getStringValueOf(doc.getRootElement(), XPATH_ATTR_SERVERNAME);

				// Hay configuración
				if (Validator.isNotNull(serverName))
				{
					String state = XMLHelper.getStringValueOf(doc.getRootElement(), "affinity/@state");
					
					if (ON.equals(state))
					{
						_log.info( String.format("Launching the server affinity process: %s", taskKind) );
						getServerTask(taskKind).start();
					}
				}
				else
				{
					// Crea el registro en la tabla serveraffinity
					serverName = PropsValues.ITER_LIVE_SERVERS_OWNURL.split("://")[1]; // Elimina el protocolo y se queda sólo con IP:Puerto
					PortalLocalServiceUtil.executeUpdateQuery(String.format(SET_SERVER_AFFINITY, GroupMgr.getGlobalGroupId(), taskKind, ON, serverName));
					
					// Inicia la tarea
					_log.info( String.format("Launching the server affinity process: %s", taskKind) );
					getServerTask(taskKind).start();
				}
			}
		}
	}
	
	/**
	 * <p>Para el grupo actual, arranca un proceso específico (Invalidación de cachés o Publicación en redes sociales) en el servidor de afinidad seleccionado.</p>
	 * 
	 * <ul>
	 * 	<li>Si ya se está ejecutando un proceso de este tipo para el mismo grupo en otro servidor de afinidad, lo detiene.</li>
	 * 	<li>Actualiza si es necesario el servidor de afinidad y el estado en BBDD.</li>
	 * 	<li>Arranca, si estaba parado, este proceso para este grupo en el servidor de afinidad seleccionado.</li>
	 * </ul>
	 * @param groupId	Grupo actual
	 * @param taskKind	Indica el tipo de tarea a arrancar. Los posibles valores son:  social y cache.
	 * @param server	Servidor de afinidad seleccionado.
	 * @throws Exception 
	 */
	public void start(String groupId, String taskKind, String server) throws Exception
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		// Valida la entrada.
		groupId = String.valueOf( validateGroupId(Long.valueOf(groupId), TASKKIND.valueOf(taskKind)) );
		ErrorRaiser.throwIfFalse(Validator.isNotNull(taskKind), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Si todo es correcto, actualiza el servidor y el estado en BBDD
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SET_SERVER_AFFINITY, groupId, taskKind, ON));

		// Arranca el proceso
		if (Heartbeat.canLaunchProcesses())
			startNonClustered(groupId, taskKind);
		else
			notifyCluster(groupId, taskKind, START);
	}
	
	public void startNonClustered(String groupId, String taskKind) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		// Arranca el proceso
		if (Heartbeat.canLaunchProcesses())
			getServerTask(taskKind).start();
	}
	
	/**
	 * <p>Para el grupo actual, detiene un proceso específico (Invalidación de cachés o Publicación en redes sociales) en el servidor de afinidad seleccionado.</p>
	 * 
	 * <ul>
	 * 	<li>Detiene el proceso de este tipo que se esté ejecutando en cualquier servidor de afinidad.</li>
	 * 	<li>Actualiza si es necesario el servidor de afinidad y el estado en BBDD.</li>
	 * </ul>
	 * @param groupId	Grupo actual
	 * @param taskKind	Indica el tipo de tarea a parar. Los posibles valores son:  social y cache.
	 * @param server	Servidor de afinidad seleccionado.
	 * @throws Exception 
	 */
	public void halt(String groupId, String taskKind, String server) throws Exception
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		// Valida la entrada.
		groupId = String.valueOf(validateGroupId(Long.valueOf(groupId), TASKKIND.valueOf(taskKind)));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(taskKind), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		_log.info("Stopping taks " + taskKind + " in all Tomcats");
		
		// Si todo es correcto, actualiza el servidor y el estado en BBDD
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SET_SERVER_AFFINITY, groupId, taskKind, OFF));
		
		if (Heartbeat.canLaunchProcesses())
			haltNonClustered(groupId, taskKind);
		else
			notifyCluster(groupId, taskKind, HALT);
	}
	
	public void haltNonClustered(String groupId, String taskKind) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		// Para el proceso
		if (Heartbeat.canLaunchProcesses())
			getServerTask(taskKind).halt();
	}
	
	private void notifyCluster(String groupId, String taskKind, String method) throws Exception
	{
		Object[] args = new Object[3];
		args[0] = groupId;
		args[1] = taskKind;
		
		ClusterTools.notifyCluster(true, "com.protecmedia.iter.base.service.ServerAffinityLocalServiceUtil", method, args);
	}
	
	private IServerAffinityProcess getServerTask(String taskKind) throws ServiceError
	{
		IServerAffinityProcess serverAffinity = null;
		
		if (TASKKIND.valueOf(taskKind).equals(TASKKIND.social))
			serverAffinity = CommunityPublisherController.INSTANCE;
		else if (TASKKIND.valueOf(taskKind).equals(TASKKIND.cache))
			serverAffinity = CacheInvalidationController.INSTANCE;
				
		ErrorRaiser.throwIfNull(serverAffinity, IterErrorKeys.XYZ_E_TASK_NOT_IMPLEMENTED_ZYX);
		return serverAffinity;
	}
	
	/**
	 * <p>Método que devuelve los servidores de afinidad de un determinado grupo según se indique en parámetros.</p>
	 * 
	 * {@code params} es un XML con el siguiente formato:
	 * <pre>
	 * {@code <rs groupid="10810" social="1" cache="1">}
	 * </pre>
	 * <p>Dónde:</p>
	 * <ul>
	 * 	<li>@groupid: Obligatorio. Grupo del que obtener los servidores de afinidad.</li>
	 * 	<li>@social: Opcional (1 por defecto). Indica si se quiere especificar en la respuesta cuál es el servidor de afinidad configurado para la Publicación en redes sociales y su estado.</li>
	 * 	<li>@cache: Opcional (1 por defecto). Indica si se quiere especificar en la respuesta cuál es el servidor de afinidad configurado para la Invalidación de cachés y su estado.</li>
	 * </ul>
	 * 
	 * @param params XML con los tipos de tareas a consultar.
	 * @return XML con la información de las tareas consultadas.
	 * 
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException
	 */
	public String getServers(String params) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		// Valida la entrada.
		Document paramsDoc = null;
		try
		{
			paramsDoc = SAXReaderUtil.read(params);
		}
		catch (DocumentException e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		Element paramsRoot = paramsDoc.getRootElement();
		// Recoge el grupo.
		long groupId = XMLHelper.getLongValueOf(paramsRoot, XPATH_ATTR_GROUPID);
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Comprueba si hay que recuperar la información del proceso de publicación en redes sociales.
		boolean schedulePublicationInfo = GetterUtil.getBoolean(XMLHelper.getStringValueOf(paramsRoot, XPATH_ATTR_SOCIAL), true);
		
		// Comprueba si hay que recuperar la información del proceso de invalidación de cachés.
		boolean cacheInvalidationInfo = GetterUtil.getBoolean(XMLHelper.getStringValueOf(paramsRoot, XPATH_ATTR_CACHE), true);
		
		// Crea el XML de resultado.
		Document serversDoc = SAXReaderUtil.createDocument();
		Element serversRoot = serversDoc.addElement(ELEM_ROOT);
		
		for (String serverName : IterDNS.getTomcatServersURLs())
		{
			// Registra el servidor
			Element server = serversRoot.addElement(ELEM_SERVER);
			server.addAttribute(ATTR_SERVER_NAME, serverName.split("://")[1]);
		}
		
		// Recupera la información de los procesos
		addServerInfo(serversRoot, (schedulePublicationInfo ? getServerAffinityAsDom(groupId, TASKKIND.social) 	: null));
		addServerInfo(serversRoot, (cacheInvalidationInfo 	? getServerAffinityAsDom(groupId, TASKKIND.cache) 	: null));
		
		return serversDoc.asXML();
	}
	
	private void addServerInfo(Element serversRoot, Document serverInfoDoc)
	{
		if (serversRoot!= null && serverInfoDoc != null)
		{
			// Recupera la información del proceso
			String serverName = XMLHelper.getStringValueOf(serverInfoDoc.getRootElement(), XPATH_ATTR_SERVERNAME);
			String kind = XMLHelper.getStringValueOf(serverInfoDoc.getRootElement(), "affinity/@kind");
			String state = XMLHelper.getStringValueOf(serverInfoDoc.getRootElement(), "affinity/@state");
			
			// Busca el servidor y añade el proceso
			Node server = serversRoot.selectSingleNode("/rs/server[@name='" + serverName + "']");
			if (server != null)
			{
				Element affinity = ((Element) server).addElement(ELEM_AFFINITY);
				affinity.addAttribute(ATTR_KIND, kind);
				affinity.addAttribute(ATTR_SERVERSTATUS, state);
			}
		}
	}
	
	/**
	 * <p>Método que devuelve los datos del servidor de afinidad de un proceso específico (Invalidación de cachés o Publicación en redes sociales).</p>
	 * 
	 * @param groupId	Grupo actual
	 * @param taskKind	Indica el tipo de tarea a consultar. Los posibles valores son:  social y cache.
	 * @return XML con la información de la tarea consultada.
	 * 
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException
	 */
	public String getServerAffinity(long groupId, String taskKind) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		return getServerAffinityAsDom(groupId, TASKKIND.valueOf(taskKind)).asXML();
	}
	
	private Document getServerAffinityAsDom(long groupId, TASKKIND taskKind) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida la entrada.
		groupId = validateGroupId(groupId, taskKind);
		ErrorRaiser.throwIfFalse(taskKind.equals(TASKKIND.social) || taskKind.equals(TASKKIND.cache), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera la información de los procesos
		Document processDoc = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SERVER_AFFINITY_INFO, groupId, taskKind));
		
		// Crea el XML de resultado.
		Document serversDoc = SAXReaderUtil.createDocument();
		Element server = serversDoc.addElement(ELEM_SERVER);
		
		Node process = processDoc.selectSingleNode("(/rs/row)[1]");
		if (process != null)
		{
			String kind 		= XMLHelper.getStringValueOf(process, XPATH_ATTR_KIND);
			String state 		= XMLHelper.getStringValueOf(process, XPATH_ATTR_SERVERSTATUS);
			String servername 	= (Heartbeat.canLaunchProcesses()) ? PropsValues.ITER_LIVE_SERVERS_OWNURL.split("://")[1] : "255.255.255.255";

			server.addAttribute(ATTR_SERVER_NAME, servername);
			Element affinity = server.addElement(ELEM_AFFINITY);
			affinity.addAttribute(ATTR_KIND, kind);
			affinity.addAttribute(ATTR_SERVERSTATUS, state);
		}

		return serversDoc;
	}
	
	private long validateGroupId(long groupId, TASKKIND taskKind) throws ServiceError
	{
		if (taskKind.equals(TASKKIND.social) || taskKind.equals(TASKKIND.cache))
			groupId = GroupMgr.getGlobalGroupId();
		else
			ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		return groupId;
	}
	
	public boolean isServerAffinity(String taskKind)
	{
		return isServerAffinity(GroupMgr.getGlobalGroupId(), taskKind);
	}
	
	public boolean isServerAffinity(long groupId, String taskKind)
	{
		return Heartbeat.canLaunchProcesses();
	}
}