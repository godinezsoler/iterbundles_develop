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

package com.protecmedia.iter.xmlio.service.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.Social.FacebookConstants;
import com.liferay.portal.kernel.Social.FacebookTools;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.cluster.ClusterTools;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
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
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.community.publisher.CommunityPublisherController;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.community.util.CommunityHttpClient;
import com.protecmedia.iter.base.service.ServerAffinityLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.xmlio.service.base.CommunityPublisherLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;

/**
 * The implementation of the community publisher local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.CommunityPublisherLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil} to access the community publisher local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.CommunityPublisherLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CommunityPublisherLocalServiceImpl extends CommunityPublisherLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(CommunityPublisherLocalServiceImpl.class);
	private static ItemXmlIO itemXmlIO = new JournalArticleXmlIO();

	////////////////////////////////////////////////////////////////////////////////
	//               SERVICIOS PARA GESTIONAR LAS PROGRAMACIONES                  //
	////////////////////////////////////////////////////////////////////////////////
	public void updateSchedule()
	{
		try
		{
			ClusterTools.notifyCluster(true, "com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil", "updateScheduleNonClustered");
		}
		catch (Exception e)
		{
			IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update scheduled", e);
			_log.error(e);
		}
	}
	
	public void updateScheduleNonClustered()
	{
		if (ServerAffinityLocalServiceUtil.isServerAffinity("social"))
		{
			Runnable task = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						// Recupera los artículos cuyas planificaciones han sido modificadas
						String sql = "SELECT articleId, publicationId FROM pending_actions WHERE kind='publication schedule' AND articleId IS NOT NULL";
						Document articlesDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
						String[] articles = XMLHelper.getStringValues(articlesDom.selectNodes("/rs/row"), "@articleId");
						String[] publicationsIds = XMLHelper.getStringValues(articlesDom.selectNodes("/rs/row"), "@publicationId");
						
						// Recupera las publicaciones de los artículos que han sufrido modificaciones en algún PageContent para recalcular sus fechas 
						String sql_updates = new StringBuilder()
						.append(" SELECT DISTINCT sp.publicationId, sp.groupId, sp.articleId, sp.schedule, sp.originalSchedule \n")
						.append(" FROM pending_actions pa                                                                      \n")
						.append(" INNER JOIN news_pagecontent npc ON npc.id_ = pa.pageContentId                                \n")
						.append(" INNER JOIN schedule_publication sp ON sp.articleId = npc.contentId AND processId IS NULL     \n")
						.append(" WHERE pa.kind='publication schedule'                                                         \n")
						.toString();
						articlesDom = PortalLocalServiceUtil.executeQueryAsDom(sql_updates);
						List<Node> publicationsAffected = articlesDom.selectNodes("/rs/row");
						publicationsIds = ArrayUtil.append(publicationsIds, XMLHelper.getStringValues(publicationsAffected, "@publicationId"));
						
						// Si hay artículos o posibles modificaciones de vigencia...
						if (articles.length > 0 || publicationsAffected.size() > 0)
						{	
							// Crea el XML para actualizar las planificaciones
							Document schedule = SAXReaderUtil.createDocument();
							Element scheduleRoot = schedule.addElement("publications");
							
							// Para cada artículo...
							for (String articleId : articles)
							{
								try
								{
									// Recupera las planificaciones
									JournalArticle article = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
									Element articleSchedule = article.getScheduledPublications();
									List<Node> articlePublications = articleSchedule.selectNodes("publication");
									// Las añade al XML para actualizar la planificación
									for (Node publication : articlePublications)
									{
										if (!"AMP".equalsIgnoreCase(XMLHelper.getStringValueOf(publication, "@accounttype")))
										{
											((Element) publication).addAttribute("articleid", articleId);
											scheduleRoot.add(publication.detach());
										}
									}
								}
								catch (Throwable th)
								{
									IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update scheduled publications for article " + articleId, articleId, th);
									_log.error(th);
								}
							}
							
							// Para cada posible cambio en la vigencia
							for (Node publication : publicationsAffected)
							{	
								Element publicationChange = scheduleRoot.addElement("publicationChange");
								publicationChange.addAttribute("publicationId", XMLHelper.getStringValueOf(publication, "@publicationId"));
								publicationChange.addAttribute("groupId", XMLHelper.getStringValueOf(publication, "@groupId"));
								publicationChange.addAttribute("articleId", XMLHelper.getStringValueOf(publication, "@articleId"));
								publicationChange.addAttribute("schedule", XMLHelper.getStringValueOf(publication, "@schedule"));
								publicationChange.addAttribute("originalSchedule", XMLHelper.getStringValueOf(publication, "@originalSchedule"));
							}
							
							// Informa al planificador de los cambios
							CommunityPublisherController.INSTANCE.updateSchedule(schedule);
						
							// Elimina los artículos y pagecontents de la tabla de planificaciones pendientes
							String deleteSql = "DELETE FROM pending_actions WHERE (publicationId IN (%s) AND kind='publication schedule') OR createDate < NOW() - INTERVAL 2 DAY";
							PortalLocalServiceUtil.executeUpdateQuery(String.format(deleteSql, StringUtil.merge(publicationsIds, StringPool.COMMA_AND_SPACE, StringPool.APOSTROPHE)));
						}
					}
					catch (Throwable th)
					{
						IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update scheduled publications", th);
						_log.error(th);
					}
				}
			};
			new Thread(task, "Publication Scheduler").start();
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	//                         SERVICIO PARA MILLENIUM                            //
	////////////////////////////////////////////////////////////////////////////////
	/**
	 * Guarda la configuración de publicaciones.
	 * 
	 * @param articleId
	 * @param schedule
	 * @throws Exception 
	 */
	public void schedulePublications(String articleId, String schedule) throws Exception
	{
		schedulePublications(articleId, schedule, false);
	}
	
	public void deleteSchedulePublications(final String articleId, final String schedule)
	{
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					schedulePublications(articleId, schedule, true);
				} catch (Throwable t) {
					_log.error(t);
				}
			}
		};
		
		Thread t = new Thread(task, "Scheduled Publications Cleaner");
		t.start();
		
		try { t.join(); }
		catch (InterruptedException e) { _log.error("Interrupted!"); }
	}
	
	private void schedulePublications(String articleId, String schedule, boolean deleteSchedules) throws Exception
	{
		// Comprueba que el artículo con id [articleId] exista.
		JournalArticle article = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
		ErrorRaiser.throwIfNull(article, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean articleHasProducts = deleteSchedules ? false : Validator.isNotNull(PortalLocalServiceUtil.getProductsByArticleId(articleId));
		
		// Recupera las publicaciones programadas del artículo.
		Element articleSchedule = article.getScheduledPublications();
		if (articleSchedule == null)
			articleSchedule = SAXReaderUtil.createElement("publications");
		
		// Procesa las planificaciones del artículo.
		boolean publicationPending = processSchedule(articleSchedule, schedule, deleteSchedules, articleHasProducts);
		
		// Actualiza el artículo.
		article.setScheduledPublications(articleSchedule);
		JournalArticleLocalServiceUtil.updateJournalArticle(article);
		
		// Si estamos en el BACK, se elimina la cache de Liferay para que no queden cambios sin actualizar en futuras publicaciones del artículo.
		if(PropsValues.IS_PREVIEW_ENVIRONMENT && PropsValues.COMMUNITYPUBLISHER_CLEAR_CACHE)
			MultiVMPoolUtil.clear(false);
		
		// Si no se están borrando planificaciones, asegurar que queda pendiente de publicación
		if (!deleteSchedules && publicationPending)
			itemXmlIO.createLiveEntry(article);
	}
	
	// LITERALES //
	private static final String ATTR_GROUPID     = "@groupid";
	private static final String ATTR_ACCOUNTID   = "@accountid";
	private static final String ATTR_ACCOUNTNAME = "@accountname";
	private static final String ATTR_ACCOUNTTYPE = "@accounttype";
	private static final String ATTR_CREDENTIALS = "@credentials";
	private static final String ATTR_SCHEDULE    = "@schedule";
	private static final String ATTR_CANCEL      = "@cancel";
	private static final String ATTR_RENDERERID  = "@rendererid";
	private static final String ELEM_TITLE       = "title";
	
	private static final String XPATH_GET_PUBLICATION = "./publication";
	private static final String XPATH_GET_SCHEDULE_FOR_UPDATE = "./publication[" + ATTR_GROUPID + "='%d' and " + ATTR_ACCOUNTID + "='%s']";
	private static final String XPATH_AND = " and ";
	private static final String XPATH_OR = " or ";
	private static final String XPATH_NOT = "not(%s)";
	
	private String buildXpathForDeleteSchedule(long groupid, String accountid, String accounttype, String credentials, String schedule, String title, boolean deleteSchedules)
	{
		StringBuilder xpath = new StringBuilder();
		xpath.append(XPATH_GET_PUBLICATION).append(StringPool.OPEN_BRACKET);
		
		// Campos obligatorios
		xpath.append(ATTR_GROUPID).append(StringPool.EQUAL).append(StringUtil.apostrophe(String.valueOf(groupid)));
		xpath.append(XPATH_AND).append(ATTR_ACCOUNTID).append(StringPool.EQUAL).append(StringUtil.apostrophe(accountid));
		xpath.append(XPATH_AND).append(ATTR_ACCOUNTTYPE).append(StringPool.EQUAL).append(StringUtil.apostrophe(accounttype));
		
		// Campos opcionales (pueden no existir)
		buildXpathOptionalFieldForDeleteSchedule(xpath, ATTR_CREDENTIALS, credentials);
		buildXpathOptionalFieldForDeleteSchedule(xpath, ELEM_TITLE, title);
		// Si es un borrado tras la publicación en el Live, lo elimina sólo si no se ha modificado a posteriori la planificación
		if (deleteSchedules)
			buildXpathOptionalFieldForDeleteSchedule(xpath, ATTR_SCHEDULE, schedule);
		
		xpath.append(StringPool.CLOSE_BRACKET);
		return xpath.toString();
	}
	
	private void buildXpathOptionalFieldForDeleteSchedule(StringBuilder xpath, String fieldName, String fieldValue)
	{
		xpath.append(XPATH_AND);
		
		if (Validator.isNotNull(fieldValue))
			xpath.append(fieldName).append(StringPool.EQUAL).append(StringUtil.apostrophe(fieldValue));
		else
			xpath.append(StringPool.OPEN_PARENTHESIS)
				.append(String.format(XPATH_NOT, fieldName)).append(XPATH_OR)
				.append(fieldName).append(StringPool.EQUAL).append(StringPool.DOUBLE_APOSTROPHE)
			.append(StringPool.CLOSE_PARENTHESIS);
	}
	
	/** Formato de fecha de la programación. */
	private static final DateFormat scheduleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * Procesa el XML de la planificación, validando que tenga el formato adecuado.
	 * 
	 * @param schedule
	 * @param scheduleChanges
	 * @param deleteSchedules
	 * @return
	 * @throws ServiceError 
	 * @throws DocumentException 
	 */
	private boolean processSchedule(Element articleSchedule, String scheduleChanges, boolean deleteSchedules, boolean articleHasProducts) throws ServiceError, DocumentException
	{
		boolean publicationPending = false;
		
		// Procesa el XML.
		Document d = SAXReaderUtil.read(scheduleChanges);
		
		// Procesa las planificaciones.
		List<Node> publications = d.selectNodes("/publications/publication");
		for (Node publication : publications)
		{
			String accounttype = XMLHelper.getStringValueOf(publication, ATTR_ACCOUNTTYPE);
			
			long groupid       = XMLHelper.getLongValueOf(publication, ATTR_GROUPID, 0);
			String accountid   = XMLHelper.getStringValueOf(publication, ATTR_ACCOUNTID);
			String accountname = XMLHelper.getStringValueOf(publication, ATTR_ACCOUNTNAME);
			String schedule    = XMLHelper.getStringValueOf(publication, ATTR_SCHEDULE);
			String credentials = XMLHelper.getStringValueOf(publication, ATTR_CREDENTIALS);
			String rendererid  = XMLHelper.getStringValueOf(publication, ATTR_RENDERERID);
			boolean cancel     = GetterUtil.getBoolean(XMLHelper.getStringValueOf(publication, ATTR_CANCEL), false);
			String title       = XMLHelper.getStringValueOf(publication, ELEM_TITLE);

			// Valida la planificación.
			validateSchedule(groupid, accountid, accountname, accounttype, schedule, credentials, rendererid, articleHasProducts, cancel);
			
			Node oldPublication = null;
			// Elimina la planificación si no se ha modificado.
			if (deleteSchedules || cancel)
			{
				oldPublication = articleSchedule.selectSingleNode(buildXpathForDeleteSchedule(groupid, accountid, accounttype, credentials, schedule, title, deleteSchedules));
			}
			// Crea, modifica o añade la planificación en las propiedades del artículo.
			else
			{
				oldPublication = articleSchedule.selectSingleNode(String.format(XPATH_GET_SCHEDULE_FOR_UPDATE, groupid, accountid));
				// Se añade la programación nueva.
				articleSchedule.add(publication.detach());
				publicationPending = true;
			}

			// Elimina la antigua
			if (oldPublication != null)
				oldPublication.detach();
		}
		
		return publicationPending;
	}
	
	/**
	 * <p>Valida la planificación.</p>
	 * <ul>
	 * <li>El Id del grupo debe ser un número mayor que 0.</li>
	 * <li>El nombre de la cuenta no puee ser nulo ni estar vacío.</li>
	 * <li>El nombre de la red social no puede ser nulo ni estar vacío.</li>
	 * <li>La fecha de publicación debe tener formato yyyy-MM-dd HH:mm:ss</li>
	 * <li>Las credenciales de publicación no pueden ser nulas ni estar vacías.</li>
	 * </ul>
	 * 
	 * @param groupid       El Id del grupo desde el que se publicará en la red social.
	 * @param account       El nombre de la cuenta en la que se publicará el artículo.
	 * @param community     El nombre de la red social en la que se publicará el artículo.
	 * @param schedule      La fecha y hora de la programación.
	 * @param credentials   Las credenciales con permisos para publicar en la red social.
	 * 
	 * @throws ServiceError XYZ_E_INVALIDARG_ZYX si algún parámetro no es correcto.
	 */
	private void validateSchedule(long groupid, String accountid, String accountname, String accounttype, String schedule, String credentials, String rendererid, boolean articleHasProducts, boolean cancel) throws ServiceError
	{
		// Valida el grupo. Debe ser un long mayor que 0.
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Valida el ID de la cuenta. Tiene que venir informado.
		ErrorRaiser.throwIfNull(accountid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Valida el nombre de la cuenta. Tiene que venir informado.
		ErrorRaiser.throwIfNull(accountname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Valida la red social. Tiene que venir informada.
		ErrorRaiser.throwIfNull(accounttype, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Valida la fecha de publicación. Debe tener formato correcto o ser nula (Publica al momento)
		if (Validator.isNotNull(schedule))
		{
			try { scheduleDateFormat.parse(schedule); }
			catch (ParseException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		}
		// Valida que se informen las credenciales. No pueden venir vacíos.
		if (!"AMP".equalsIgnoreCase(accounttype))
			ErrorRaiser.throwIfNull(credentials, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Valida que se informe rendererId cuando el tipo de cuenta es AMP, Apple News o Facebook IA
		boolean facebookIA = "Facebook".equalsIgnoreCase(accounttype)
				          && Validator.isNotNull(credentials)
				          && credentials.split(StringPool.COMMA).length == 3
						  && ( "InstantArticle".equalsIgnoreCase(credentials.split(StringPool.COMMA)[2])
						    || "InstantArticle_debug".equalsIgnoreCase(credentials.split(StringPool.COMMA)[2]))
				          ? true : false;		
		if ("AMP".equalsIgnoreCase(accounttype) || "Apple".equalsIgnoreCase(accounttype) || facebookIA)
			ErrorRaiser.throwIfNull(rendererid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Ya no se lanza excepción si el artículo está restringido y es una red social (AMP,fbIA, ANF)
		// http://jira.protecmedia.com:8080/browse/ITER-1373?focusedCommentId=68893&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-68893
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//                         SERVICIOS PARA ITER ADMIN                          //
	////////////////////////////////////////////////////////////////////////////////
	private final static String SQL_SELECT_SCHEDULED_PUBLICATIONS = new StringBuilder()
		.append(" SELECT T.publicationId,                                      \n")
		.append("        T.articleId,                                          \n")
		.append("        j.title article,                                      \n")
		.append("        T.accountId,                                          \n")
		.append("        T.accountName,                                        \n")
		.append("        T.accountType,                                        \n")
		.append("        T.schedule,                                           \n")
		.append("        T.originalSchedule,                                   \n")
		.append("        MIN(n.vigenciadesde) vigenciadesde,                   \n")
		.append("        T.processId                                           \n")
		.append(" FROM (                                                       \n")
		.append(" 	SELECT publicationId, articleId, accountId, accountName,   \n")
		.append(" 	       accountType, schedule, credentials,                 \n")
		.append("          originalSchedule, processId                         \n")
		.append(" 	FROM schedule_publication                                  \n")
		.append("     WHERE groupId=%d                                         \n") // Group filter
		.append(" ) T                                                          \n")
		.append(" INNER JOIN journalarticle j ON j.articleId = T.articleId     \n")
		.append(" INNER JOIN News_PageContent n ON n.contentId = T.articleId   \n")
		.append(" %s "                                                            ) // Where
		.append(" GROUP BY publicationId                                       \n")
		.append(" %s "                                                            ) // Order
		.append(" %s "                                                            ) // Limits
		.toString();
	
	/**
	 * <p>Recupera las publicaciones de contenido en redes sociales programadas para un sitio.</p>
	 * <p>Permite ordenar por cualquier columna y aplicar filtros por artículo, cuenta, red social y fecha de publicación plrogramada, así
	 * como controlar los registros devueltos para paginación.</p>
	 * 
	 * @param groupId           El Id del grupo del que se quiere recuperar sus publicaciones programadas.
	 * @param filtersDefinition Los filtros a aplicar a la consulta.
	 * @param sortDefinition    El orden a aplicar a la consulta.
	 * @param beginRegister     El registro inicial a devolver (paginación).
	 * @param maxRegisters      El máximo número de registros a devolver (paginación).
	 * 
	 * @return Document con las publicaciones programadas encontradas.
	 * 
	 * @throws SecurityException     Si ocurre un error al acceder a BBDD.
	 * @throws NoSuchMethodException Si ocurre un error al acceder a BBDD.
	 * @throws DocumentException     Si no se pueden procesar los filtros o el orden.
	 * @throws ServiceError          XYZ_E_INVALIDARG_ZYX si el {@code groupId}, {@code beginRegister} o {@code maxRegisters}
	 *                               no son un número mayor que 0.
	 */
	public Document getSchedulePublications(long groupId, String filtersDefinition, String sortDefinition, long beginRegister, long maxRegisters) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError
	{
		// Valida la entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(beginRegister >= 0 && maxRegisters > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Procesa los Instant Article que estén pendietes
		try
		{
			checkProcessStatus(null);
		}
		catch (Exception e)
		{
			_log.error("Unable to check pending instant articles status");
			_log.error(e);
		}
		
		// Calcula los filtros
		StringBuilder where = SQLFilterUtil.buildWhere(filtersDefinition);
		
		// Calcula el orden
		StringBuilder orderBy = SQLFilterUtil.buildOrderBy(sortDefinition);
		
		// Calcula los límites
		StringBuilder limits = new StringBuilder("LIMIT ").append(beginRegister).append(StringPool.COMMA_AND_SPACE).append(maxRegisters);

		// Recupera los datos
		String sql = String.format(SQL_SELECT_SCHEDULED_PUBLICATIONS, groupId, where, orderBy, limits);
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		return result;
	}
	
	private final static String SQL_SELECT_SCHEDULED_VALIDITY = new StringBuilder()
		.append(" SELECT ExtractValue(l.name, '/root/name[1]/text()') section,  \n")
		.append("        n.vigenciadesde,                                       \n")
		.append("        n.vigenciahasta                                        \n")
		.append(" FROM News_PageContent n                                       \n")
		.append(" INNER JOIN layout l ON l.uuid_ = n.layoutId                   \n")
		.append(" WHERE n.contentId='%s'                                          ")
		.toString();
	
	private final static String SQL_SELECT_PUBLICATION_SCHEDULES = new StringBuilder()
		.append(" SELECT publicationId,     \n")
		.append("        accountName,       \n")
		.append("        accountType,       \n")
		.append("        schedule           \n")
		.append(" FROM schedule_publication \n")
		.append(" WHERE articleId='%s'      \n")
		.append("   AND publicationId <>'%s'  ")
		.toString();
	
	/**
	 * <p>Permite consultar toda la información referente a una publicación, así como las fechas de vigencia del artículo a publicar y un
	 * resumen de todas sus publicaciones programadas pendientes.</p>
	 * 
	 * @param publicationId El Id de la publicación programada a consultar.
	 * @param articleId     El Id del artículo al que corresponde la programación.
	 * @return String con la información detallada de la publicación.
	 * @throws SystemException Si el Id de publicación no es correcto u ocurre un error al consultar la información.
	 */
	public Document getSchedulePublicationDetail(String publicationId, String articleId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida el ID de la publicación.
		ErrorRaiser.throwIfNull(publicationId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document publicationDetails = SAXReaderUtil.createDocument();
		publicationDetails.addElement("details");
		
		// Recupera las fechas de vigencia del artículo
		String sql = String.format(SQL_SELECT_SCHEDULED_VALIDITY, articleId);
		Document publicationValidity = PortalLocalServiceUtil.executeQueryAsDom(sql, true, "effectivedates", "date");
		publicationDetails.getRootElement().add( publicationValidity.getRootElement().detach() );

		// Recupera las otras publicaciones del artículo
		sql = String.format(SQL_SELECT_PUBLICATION_SCHEDULES, articleId, publicationId);
		Document publicationSchedules = PortalLocalServiceUtil.executeQueryAsDom(sql, true, "schedules", "schedule");
		publicationDetails.getRootElement().add( publicationSchedules.getRootElement().detach() );
		
		return publicationDetails;
	}
	
	/**
	 * <p>Cancela las publicaciones indicadas, informando al proceso de publicación de que debe replanificarse.</p>
	 * 
	 * @param schedulesToCancel XML con los IDs de laa publicaciones a eliminar.
	 * @throws Exception 
	 */
	public String cancelSchedulePublication(String schedulesToCancel) throws Exception 
	{
		Object[] args = new Object[1];
		args[0] = schedulesToCancel;
		ClusterTools.notifyCluster(true, "com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil", "cancelSchedulePublicationNonClustered", args);
		
		// Retorna el mismo XML que recibió como parámetro.
		return schedulesToCancel;
	}
	
	public void cancelSchedulePublicationNonClustered(String schedulesToCancel) throws ServiceError
	{
		if (ServerAffinityLocalServiceUtil.isServerAffinity("social"))
		{
			// Valida la entrada y construye el documento XML con las planificaciones a cancelar.
			ErrorRaiser.throwIfFalse(Validator.isNotNull(schedulesToCancel), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			Document schedulesDom = null;
			try { schedulesDom = SAXReaderUtil.read(schedulesToCancel); }
			catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
	
			// Elimina las publicaciones programadas del planificador.
			CommunityPublisherController.INSTANCE.updateSchedule(schedulesDom);
		}
	}
	
	synchronized public String checkProcessStatus(String schedulesToCheck) throws ServiceError, SecurityException, NoSuchMethodException, SystemException, IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException
	{
		// Comrpueba si se le indican publicationsIds
		Document schedulesDom = getPublicationsToCheck(schedulesToCheck);
		
		// Recupera las publicaciones en proceso
		Document publicationsInProcess = getPublicationsInProcess(schedulesDom);
		
		schedulesDom = SAXReaderUtil.createDocument();
		schedulesDom.addElement("rs");
		
		// Para cada publicación...
		List<Node> publications = publicationsInProcess.selectNodes("rs/row");
		for (Node publication : publications)
		{
			// Recupera la información necesaria para consultar el estado
			String publicationId 	= XMLHelper.getStringValueOf(publication, "@publicationId"	);
			long groupId 			= XMLHelper.getLongValueOf(  publication, "@groupId"		);
			String articleId 		= XMLHelper.getStringValueOf(publication, "@articleId"		);
			String accountId 		= XMLHelper.getStringValueOf(publication, "@accountId"		);
			String accountName 		= XMLHelper.getStringValueOf(publication, "@accountName"	);
			String credentials 		= XMLHelper.getStringValueOf(publication, "@credentials"	);
			String processId 		= XMLHelper.getStringValueOf(publication, "@processId"		);
			String token 			= credentials.split(StringPool.COMMA)[0];
			String secretKey 		= CommunityAuthorizerUtil.getConfigSecretKey(CommunityAuthorizerUtil.FACEBOOK, groupId);
			String appSecretProof 	= FacebookTools.get_appsecret_proof(token, secretKey);
			
			// Llama a la API de Facebook para consultar su estado
			JSONObject response = new CommunityHttpClient.Builder(FacebookConstants.API_GRAPH + "/" + processId)
									.queryString("access_token", token)
									.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, appSecretProof)
									.build()
									.get();

			// Recoge el estado
			String status = null;
			String trace = null;
			try
			{
				status = response.getString("status");
				trace = response.toString(4);
			}
			catch (JSONException e)
			{
				_log.error(e);
			}
			
			// Si no está en progreso, informa el resultado en IterMonitor y lo elimina
			if (Validator.isNotNull(status) && !status.equals("IN_PROGRESS"))
			{
				Event eventKind = status.equals("FAILED") ? Event.ERROR : Event.INFO;
				IterMonitor.logEvent(groupId, eventKind, new Date(), "Facebook Publisher: Facebook Instant Article process result", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, trace));
				schedulesDom.getRootElement().addElement("row").addAttribute("id", publicationId);
			}
			// Sigue en progreso o no se ha podido comprobar. No hay que eliminarlo
			else
			{
				publication.detach();
			}
		}
		
		// Elimina los registros que ya se han procesado
		deleteProcessedPublications(publicationsInProcess);
		
		// Devuelve sólo las publicaciones procesadas
		return schedulesDom.asXML();
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//                              UTILIDADES                                    //
	////////////////////////////////////////////////////////////////////////////////
	
	private Document getPublicationsToCheck(String schedulesToCheck) throws ServiceError
	{
		Document schedulesDom = null;
		
		if (Validator.isNotNull(schedulesToCheck))
		{	
			// Valida que el formato sea correcto
			try { schedulesDom = SAXReaderUtil.read(schedulesToCheck); }
			catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		}
		
		return schedulesDom;
	}
	
	private static final String SQL_SELECT_ALL_PUBLICATIONS_IN_PROGRESS = "SELECT publicationId, groupId, articleId, accountId, accountName, credentials, processId FROM schedule_publication WHERE processId IS NOT NULL";
	private static final String SQL_SELECT_PUBLICATIONS_IN_PROGRESS = SQL_SELECT_ALL_PUBLICATIONS_IN_PROGRESS + " AND publicationId IN (%s)";
	
	private Document getPublicationsInProcess(Document schedulesDom) throws SecurityException, NoSuchMethodException
	{
		String sql = StringPool.BLANK;
		
		if (schedulesDom != null)
		{
			// Recupera las publicaciones a comprobar
			String[] publicationsIds = XMLHelper.getStringValues(schedulesDom.selectNodes("/rs/row"), "@id");
			if (publicationsIds.length > 0)
			{
				String formattedIds = StringUtil.merge(publicationsIds, StringPool.COMMA_AND_SPACE, StringPool.APOSTROPHE);
				sql = String.format(SQL_SELECT_PUBLICATIONS_IN_PROGRESS, formattedIds);
			}
		}
		else
		{
			sql = SQL_SELECT_ALL_PUBLICATIONS_IN_PROGRESS;
		}
		
		return PortalLocalServiceUtil.executeQueryAsDom(sql);
	}
	
	private void deleteProcessedPublications(Document publicationsInProcess) throws IOException, SQLException
	{
		// Elimina los registros que ya se han procesado
		String[] publicationsIds = XMLHelper.getStringValues(publicationsInProcess.selectNodes("/rs/row"), "@publicationId");
		if (publicationsIds.length > 0)
		{
			String formattedIds = StringUtil.merge(publicationsIds, StringPool.COMMA_AND_SPACE, StringPool.APOSTROPHE);
			String SQL_DELETE_PUBLICATIONS_PROCESSED = "DELETE FROM schedule_publication WHERE publicationId IN (%s) AND processId IS NOT NULL";
			String sql = String.format(SQL_DELETE_PUBLICATIONS_PROCESSED, formattedIds);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	private static class SQLFilterUtil
	{
		private static final Map<String, String> columns;
		private static final Map<String, String> dateOperators;
		private static final Map<String, String> selectOperators;
		private static final Map<String, String> textOperators;
		static
		{
			columns = new HashMap<String, String>();
			columns.put("article",     "j.title");
			columns.put("accountId", "accountId");
			columns.put("accountName", "accountName");
			columns.put("accountType", "accountType");
			columns.put("schedule",    "schedule");
			
			dateOperators = new HashMap<String, String>();
			dateOperators.put("equals",     " = ");
			dateOperators.put("beforedate", " < ");
			dateOperators.put("afterdate",  " > ");
			dateOperators.put("fromdate",   " >= ");
			dateOperators.put("todate",     " <= ");
			
			selectOperators = new HashMap<String, String>();
			selectOperators.put("equals",     " IN (%s) ");
			selectOperators.put("distinct",   " NOT IN (%s) ");
			
			textOperators = new HashMap<String, String>();
			textOperators.put("equals",     " = '%s'");
			textOperators.put("distinct",   " <> '%s'");
			textOperators.put("startBy",    " LIKE '%s%%' ");
			textOperators.put("endBy",      " LIKE '%%%s' ");
			textOperators.put("contain",    " LIKE '%%%s%%' ");
			textOperators.put("notcontain", " NOT LIKE '%%%s%%' ");
		}
		
		private static StringBuilder buildOrderBy(String sortDefinition) throws DocumentException
		{
			StringBuilder orderBy = new StringBuilder(StringPool.BLANK);
			
			if (Validator.isNotNull(sortDefinition))
			{
				Document domSort = SAXReaderUtil.read(sortDefinition);
				String column    = XMLHelper.getTextValueOf(domSort, "/order/@columnid");
				String order     = XMLHelper.getLongValueOf(domSort, "/order/@asc") == 1 ? "ASC" : "DESC";
				
				if (Validator.isNotNull(column) && Validator.isNotNull(order))
				{
					orderBy.append("ORDER BY ").append(column).append(StringPool.SPACE).append(order);
					// Siempre aplica una segunda ordenación por fecha ascendente
					if (!"schedule".equals(column))
						orderBy.append(", schedule ASC");
					orderBy.append(StringPool.NEW_LINE);
				}
			}
			
			// Ordenación por defecto
			if (orderBy.length() == 0)
				orderBy.append("ORDER BY schedule ASC, accountName ASC").append(StringPool.NEW_LINE);
			
			return orderBy;
		}
		
		private static StringBuilder buildWhere(String filters) throws DocumentException
		{
			StringBuilder where = new StringBuilder(StringPool.BLANK);
			
			if (Validator.isNotNull(filters))
			{
				Document domFilter = SAXReaderUtil.read(filters);
				List<Node> filterNodes = domFilter.getRootElement().selectNodes("filter");
				for (Node filter : filterNodes)
				{
					String column = columns.get( ((Element) filter).attributeValue("columnid") );
					
					if (Validator.isNotNull(column))
					{
						if (where.length() == 0)
							where.append("WHERE ");
						else
							where.append("  AND ");
							
						// Fechas
						if ("schedule".equals(column))
						{
							String operator = dateOperators.get(((Element) filter).attributeValue("operator"));
							String value    = StringUtil.apostrophe( XMLHelper.getTextValueOf(filter, "values/value/text()") );
							if (Validator.isNotNull(operator) && Validator.isNotNull(value))
								where.append(column).append(operator).append(value).append(StringPool.NEW_LINE);
						}
						// Red Social
						else if ("accountType".equals(column))
						{
							String operator = selectOperators.get(((Element) filter).attributeValue("operator"));
							List<Node> accountTypeNodes = ((Element) filter).selectNodes("values/value");
							StringBuilder value = new StringBuilder(StringPool.BLANK);
							for (Node accountType : accountTypeNodes)
							{
								if (value.length() > 0)
									value.append(StringPool.COMMA_AND_SPACE);	
								value.append( StringUtil.apostrophe(((Element) accountType).getText()) );
							}
							if (Validator.isNotNull(operator) && value.length() > 0)
								where.append(column).append(String.format(operator, value)).append(StringPool.NEW_LINE);
						}
						// Título del artículo / Nombre de la cuenta / Id de cuenta
						else
						{
							String operator = textOperators.get(((Element) filter).attributeValue("operator"));
							String value    = XMLHelper.getTextValueOf(filter, "values/value/text()");
							
							// Escapa las comillas
							value = value.replace(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
							
							if (Validator.isNotNull(operator) && Validator.isNotNull(value))
								where.append(column).append( String.format(operator, value) ).append(StringPool.NEW_LINE);
						}
					}
				}
			}
			return where;
		}
	}
	
	public void cancelProcessChecker(String processId)
	{
		if (Validator.isNull(processId))
			CommunityPublisherController.INSTANCE.cancelAllProcessCheckers();
		else
			CommunityPublisherController.INSTANCE.cancelProcessChecker(processId);
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//                          SERVICIOS PARA DIRECTOS                           //
	////////////////////////////////////////////////////////////////////////////////
	public void publishPost(String schedule)
	{
		try
		{
			ClusterTools.notifyCluster(true, "com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil", "publishPostNonClustered", new Object[] {schedule});
		}
		catch (Exception e)
		{
			IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update scheduled", e);
			_log.error(e);
		}
	}
	
	public void publishPostNonClustered(String schedule)
	{
		if (ServerAffinityLocalServiceUtil.isServerAffinity("social"))
		{
			final String scheduleInfo = schedule;
			Runnable task = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Document scheduleXml = SAXReaderUtil.read(scheduleInfo);
						CommunityPublisherController.INSTANCE.updateSchedule(scheduleXml);

					}
					catch (Throwable th)
					{
						IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update scheduled publications for live event", th);
						_log.error(th);
					}
				}
			};
			new Thread(task, "Live Event Publication Scheduler").start();
		}
	}
}