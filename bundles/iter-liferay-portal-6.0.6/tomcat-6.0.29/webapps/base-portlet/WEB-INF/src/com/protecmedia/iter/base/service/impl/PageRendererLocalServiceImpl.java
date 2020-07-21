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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.render.PageRenderer;
import com.liferay.portal.kernel.render.RenditionMode;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.MinifyUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.base.PageRendererLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.WebResourceUtil;

/**
 * The implementation of the page renderer local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.PageRendererLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.PageRendererLocalServiceUtil} to access the page renderer local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.PageRendererLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.PageRendererLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class PageRendererLocalServiceImpl extends PageRendererLocalServiceBaseImpl 
{
	private static final String RSRC_TPL 	= "tpl";
	private static final String RSRC_STYLE 	= "style";
	private static final String RSRC_XPATH	= String.format("rsrc[not(@type) or (@type!='%s' and @type!='%s')]", RSRC_TPL, RSRC_STYLE);
	
	private static final String COMPONENT_XPATH 		= "rsrc[@type='%s']/content";
	private static final String COMPONENT_TPL_XPATH 	= String.format(COMPONENT_XPATH, RSRC_TPL);
	private static final String COMPONENT_STYLE_XPATH 	= String.format(COMPONENT_XPATH, RSRC_STYLE);

	private static final String EXIST_RENDERER = new StringBuilder(
			"SELECT COUNT(*)																		\n").append(
			"FROM Renderer																			\n").append(
			"	WHERE rendererId = '%s'																\n").toString();
		
	private static final String ADD_RENDERER = new StringBuilder(
		"INSERT INTO Renderer (rendererid, groupid, token, renditionmode) 							\n").append(
		"	values ('%s', %d, '%s', '%s')															\n").toString();
	
	private static final String ADD_RENDERER_COMPONENT = new StringBuilder(
		"INSERT INTO RendererComponent (compid, rendererid, compcontent, comptype) 					\n").append(
		"	values ('%s', '%s', '%s', '%s')															\n").toString();

	private static final String ADD_RSRC = new StringBuilder(
		"INSERT INTO RendererRsrc (rsrcid,rsrccontent,rsrccontenttype,orphandate)					\n").append(
		"VALUES %s																					\n").append(
		"ON DUPLICATE KEY UPDATE rsrccontenttype = VALUES(rsrccontenttype), orphandate=NULL			\n").toString();

	private static final String ADD_RSRC_RELATIONSHIP = new StringBuilder(
		"INSERT INTO RendererRsrc_Relationship (rendererId, rsrcId)									\n").append(
		"	VALUES %s																				\n").append(
		"ON DUPLICATE KEY UPDATE rsrcId=rsrcId														\n").toString();

	private static final String UPDATE_RENDERER = new StringBuilder(
		"UPDATE renderer SET %s																		\n").append(
		"	WHERE rendererid = '%s'																	\n").toString();
	
	private static final String UPDATE_RENDERER_COMPONENT = new StringBuilder(
		"UPDATE RendererComponent																	\n").append(
		"SET compcontent='%s'																		\n").append(
		"WHERE rendererId = '%s'																	\n").append( 
		"	AND compType = '%s'																		\n").toString();
				
	private static final String DELETE_RSRC_RELATIONSHIP = new StringBuilder(
		"DELETE FROM rendererrsrc_relationship														\n").append(
		"	WHERE rendererid='%s'																	\n").append(
		"		AND rsrcid IN (%s)																	\n").toString();
	
	private static final String GET_RENDERER_INFO = new StringBuilder(
		"SELECT rendererid, groupid, renditionmode													\n").append(	
		"FROM Renderer																				\n").append(
		"	WHERE rendererid IN ('%s', '%s')														\n").toString();
	
	private static final String CHECK_RENDERER_ARTICLE = new StringBuilder(
		"SELECT COUNT(*) 																			\n").append(
		"FROM Renderer_Article																		\n").append(
		"	WHERE rendererId = '%s'																	\n").toString();
	
	private static Log _log 	= LogFactoryUtil.getLog(PageRendererLocalServiceImpl.class);
	
	public String createTheme(String themeSpec) throws Exception
	{
		_log.debug("createTheme: BEGIN");
		// Lee el DOM con las características del nuevo PageRenderer
		Element root = SAXReaderUtil.read(themeSpec).getRootElement();
		
		if (_log.isTraceEnabled())
			writeFile("rendererTheme_create.xml", root.asXML());
		
		long groupId = XMLHelper.getLongValueOf(root, "@siteid");
		ErrorRaiser.throwIfFalse(groupId > 0 , IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String token = XMLHelper.getStringValueOf(root, "@token");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(token), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		RenditionMode mode = RenditionMode.getMode( XMLHelper.getStringValueOf(root, "@mode") );
		ErrorRaiser.throwIfFalse(!mode.equals(RenditionMode.classic), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String tplContent	= XMLHelper.getStringValueOf(root, COMPONENT_TPL_XPATH);
		ErrorRaiser.throwIfNull(tplContent, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Se crea el Renderer
		String rendererId = PortalUUIDUtil.newUUID();
		executeUpdateQuery("ADD_RENDERER", String.format(ADD_RENDERER, rendererId, groupId, token, mode.toString()));
		
		// Se crea el RendererComponent de la plantilla
		String tplId = PortalUUIDUtil.newUUID();
		executeUpdateQuery("ADD_RENDERER_COMPONENT", String.format(ADD_RENDERER_COMPONENT, tplId, rendererId, tplContent, RSRC_TPL));
		
		// Los ANFs NO tienen estilos, están integrados
		if (!mode.equals(RenditionMode.anf))
		{
			String styleContent = XMLHelper.getStringValueOf(root, COMPONENT_STYLE_XPATH);
			ErrorRaiser.throwIfNull(styleContent, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Se crea el RendererComponent del estilo
			String styleId = PortalUUIDUtil.newUUID();
			executeUpdateQuery("ADD_RENDERER_COMPONENT", String.format(ADD_RENDERER_COMPONENT, styleId, rendererId, styleContent, RSRC_STYLE));
		}

		// Se insertan los recursos del tema
		updateRsrc(rendererId, root.selectNodes(RSRC_XPATH));
		
		// Inserta en memoria el nuevo Renderer
		PageRenderer.mapAddRenderer(groupId, token, rendererId, mode);
	
		_log.debug("createTheme: END");
		
		return rendererId;
	}
	
	public void updateTheme(String themeSpec) throws Exception
	{
		_log.debug("updateTheme: BEGIN");
		// Lee el DOM con las características del nuevo PageRenderer
		Element root = SAXReaderUtil.read(themeSpec).getRootElement();
		
		if (_log.isTraceEnabled())
			writeFile("rendererTheme_update.xml", root.asXML());
		
		String rendererId = XMLHelper.getStringValueOf(root, "@rendererid");
		ErrorRaiser.throwIfNull(rendererId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		ErrorRaiser.throwIfFalse(existTheme(rendererId), IterErrorKeys.XYZ_E_RENDERER_NOT_FOUND_ZYX, rendererId);

		// Actualización del Renderer
		StringBuilder updtRenderer = new StringBuilder();
		String token 			= XMLHelper.getStringValueOf(root, "@token");
		if (Validator.isNotNull(token))
			updtRenderer.append( String.format(" token='%s',", token) );
			
		if (updtRenderer.length() > 0)
			executeUpdateQuery("UPDATE_RENDERER", String.format(UPDATE_RENDERER, updtRenderer.deleteCharAt(updtRenderer.length()-1).toString(), rendererId));
		
		// Actualización del tpl
		String tplContent = XMLHelper.getStringValueOf(root, COMPONENT_TPL_XPATH);
		if (Validator.isNotNull(tplContent))
			executeUpdateQuery("UPDATE_RENDERER_COMPONENT", String.format(UPDATE_RENDERER_COMPONENT, tplContent, rendererId, RSRC_TPL));
		
		// Actualización del estilo
		String styleContent = XMLHelper.getStringValueOf(root, COMPONENT_STYLE_XPATH);
		if (Validator.isNotNull(styleContent))
			executeUpdateQuery("UPDATE_RENDERER_COMPONENT", String.format(UPDATE_RENDERER_COMPONENT, styleContent, rendererId, RSRC_STYLE));

		// Se actualizan los recursos del tema
		updateRsrc(rendererId, root.selectNodes(RSRC_XPATH));
		
		// Si se ha modificado el token, actualiza en memoria el Renderer
		if (Validator.isNotNull(token))
			PageRenderer.mapUpdateRenderer(rendererId, token);

		_log.debug("updateTheme: END");
	}
	
	public void deleteTheme(String rendererId) throws Exception
	{
		_log.debug("deleteTheme: BEGIN");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(rendererId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if (existTheme(rendererId))
		{
			// Se comprueba que dicho tema no esté configurado para ningún artículo
			long numArticles = Long.valueOf( PortalLocalServiceUtil.executeQueryAsList( String.format(CHECK_RENDERER_ARTICLE, rendererId) ).get(0).toString() );
			ErrorRaiser.throwIfFalse(numArticles <= 0, IterErrorKeys.XYZ_E_RENDERER_IN_USE_ZYX, rendererId);
			
			// Se borra el tema
			executeUpdateQuery("ITR_RENDERER_DELETE", String.format("CALL ITR_RENDERER_DELETE('%s', %d)", rendererId, PropsValues.ITER_THEME_RSRC_ORPHAN_DAYS));
			
			// Se borra el tema de la memoria
			PageRenderer.mapDeleteRenderer(rendererId);
		}
		_log.debug("deleteTheme: END");
	}
	
	public void reassignTheme(String srcRendererId, String dstRendererId) throws SecurityException, NoSuchMethodException, ServiceError, IOException, SQLException
	{
		// Se comprueba que ambos temas existan, sean del mismo grupo y del mismo modo
		Element root = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_RENDERER_INFO, srcRendererId, dstRendererId) ).getRootElement();
		
		Node srcNode = root.selectSingleNode(String.format("/rs/row[@rendererid='%s']", srcRendererId));
		ErrorRaiser.throwIfNull(srcNode, IterErrorKeys.XYZ_E_RENDERER_NOT_FOUND_ZYX, srcRendererId);
		
		Node dstNode = root.selectSingleNode(String.format("/rs/row[@rendererid='%s']", dstRendererId));
		ErrorRaiser.throwIfNull(dstNode, IterErrorKeys.XYZ_E_RENDERER_NOT_FOUND_ZYX, dstRendererId);
		
		long srcGroupId = Long.valueOf(((Element)srcNode).attributeValue("groupid"));
		long dstGroupId = Long.valueOf(((Element)dstNode).attributeValue("groupid"));
		ErrorRaiser.throwIfFalse(srcGroupId == dstGroupId, 	IterErrorKeys.XYZ_E_RENDERER_DIFFERENT_GROUPS_ZYX, String.format("%d - %d", srcGroupId, dstGroupId));
		
		RenditionMode srcRendition = RenditionMode.getMode( ((Element)srcNode).attributeValue("renditionmode") );  
		RenditionMode dstRendition = RenditionMode.getMode( ((Element)dstNode).attributeValue("renditionmode") );  
		ErrorRaiser.throwIfFalse(srcRendition.equals(dstRendition), IterErrorKeys.XYZ_E_RENDERER_DIFFERENT_MODE_ZYX, String.format("%s - %s", srcRendition.toString(), dstRendition.toString()));

		executeUpdateQuery("reassignTheme", String.format("UPDATE Renderer_Article SET rendererId = '%s' WHERE rendererId = '%s'", dstRendererId, srcRendererId));
	}
	
	private void updateRsrc(String rendererId, List<Node> rsrcList) throws IOException, SQLException, ServiceError
	{
		_log.debug("updateRsrc: BEGIN");
		
		StringBuilder createRsrc 	= new StringBuilder();
		StringBuilder createRela	= new StringBuilder();
		StringBuilder deleteRsrc	= new StringBuilder();
		
		// Se insertan/actualizan los recursos
		for (Node rsrc : rsrcList)
		{
			String md5 = XMLHelper.getTextValueOf(rsrc, "@md5");
			String rsrcContent = XMLHelper.getTextValueOf(rsrc, "content");
						
			if (rsrcContent != null)
			{
				// Creación. Si existe contenido tiene que existir tipo. No tiene sentido actualizar el tipo manteniendo el contenido
				// Los recursos no se actualizan, se crean o se destruyen porque al cambiar el contenido cambiará el md5 por lo que se 
				// considerarán dos recursos distintos, y llegará un borrado del md5 anterior y la creación del nuevo
				String type = XMLHelper.getTextValueOf(rsrc, "@type");
				ErrorRaiser.throwIfNull(type);
				
				if ( type.equals(ContentTypes.TEXT_CSS) || type.equals(ContentTypes.TEXT_JAVASCRIPT) )
				{
					rsrcContent = MinifyUtil.minifyContentOnDeliverTheme(rsrcContent, type);
				}

				// Existe el tipo así que se trata de un create
				String rsrccontenttype = StringEscapeUtils.escapeSql( WebResourceUtil.getContentTypeByType(type) );

				createRsrc.append( String.format("\n('%s','%s','%s', NULL),", md5, rsrcContent, rsrccontenttype) );
				createRela.append( String.format("('%s','%s'),", rendererId, md5) );
			}
			else
			{
				// Borrado
				deleteRsrc.append( String.format("'%s',", md5) );
			}
		}
			
		// Elementos borrados
		if (deleteRsrc.length() > 0)
			executeUpdateQuery("DELETE_RSRC_RELATIONSHIP", String.format(DELETE_RSRC_RELATIONSHIP, rendererId, deleteRsrc.deleteCharAt(deleteRsrc.length()-1).toString()));
		
		if (createRsrc.length() > 0)
		{
			// Se crean los recursos
			executeUpdateQuery("ADD_RSRC", String.format(ADD_RSRC, createRsrc.deleteCharAt(createRsrc.length()-1).toString()));
			
			// Se crean las relaciones de dichos recursos con el tema
			executeUpdateQuery("ADD_RSRC_RELATIONSHIP", String.format(ADD_RSRC_RELATIONSHIP, createRela.deleteCharAt(createRela.length()-1).toString()));
		}
		
		// Actualiza el estado (huérfano o no) de los recursos
		if (deleteRsrc.length() > 0 || createRsrc.length() > 0)
			executeUpdateQuery("ITR_RENDERER_CHECK_RSRC", String.format("CALL ITR_RENDERER_CHECK_RSRC(%d)", PropsValues.ITER_THEME_RSRC_ORPHAN_DAYS));
		
		_log.debug("updateRsrc: END");
	}
	
	private void executeUpdateQuery(String label, String sql) throws IOException, SQLException
	{
		if (_log.isTraceEnabled())
			_log.trace( label.concat("\n").concat(sql) );
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}

	/**
	 * 
	 * @param fileName
	 * @param fileData
	 * @throws IOException 
	 */
	private void writeFile(String fileName, String fileData) throws IOException
	{
        String pathRoot = new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath();
        String path		= new StringBuilder(pathRoot).append(File.separatorChar)
								  .append("base-portlet").append(File.separatorChar)
								  .append("xsl").append(File.separatorChar)
								  .append(fileName).toString();
        
        FileWriter file = new FileWriter(path);
        file.write(fileData);
        file.close();
	}
	
	private boolean existTheme(String rendererId)
	{
		return Long.valueOf(PortalLocalServiceUtil.executeQueryAsList( String.format(EXIST_RENDERER, rendererId) ).get(0).toString()) > 0;
	}
	
	/**
	 * 
	 */
	public void updateArticlesRenderers() throws SecurityException, NoSuchMethodException, ServiceError
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		Runnable task = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Recupera los artículos cuyas planificaciones han sido modificadas
					final String sql = "SELECT articleId FROM pending_actions WHERE kind='publication schedule' AND articleId IS NOT NULL";
					Document articlesDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
					String[] articles = XMLHelper.getStringValues(articlesDom.selectNodes("/rs/row"), "@articleId");

					// Actualiza para cada artículo los temas asociados
					for (String articleId : articles)
					{
						try
						{
							JournalArticle article = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
							Element articleSchedule = article.getScheduledPublications();
							
							if (articleSchedule != null)
							{
								List<Node> publications = articleSchedule.selectNodes("publication[@rendererid]");
								
								if (publications.size() > 0)
								{
									Element root = SAXReaderUtil.read("<rs/>").getRootElement();
									for (Node node : publications)
										root.add( node.clone() );
									
									String publicationsXML = root.asXML();
									if (_log.isDebugEnabled())
										_log.debug( String.format("Article %s\nRenderer %s", articleId, publicationsXML) );
									
									PortalLocalServiceUtil.executeUpdateQuery( String.format("CALL ITR_RENDERER_UPDATE_ARTICLE('%s', '%s')", articleId, publicationsXML) );
								}
							}
						}
						catch (Throwable th)
						{
							IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Page Renderer: Unable to update themes for article " + articleId, articleId, th);
							_log.error(th);
						}
					}
				}
				catch (Throwable th)
				{
					IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Page Renderer: Unable to update themes for the published articles", th);
					_log.error(th);
				}
			}
		};
		
		// Se lanza la tarea
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(task);
		
		// Se espera, de esta forma se garantiza que llegado a este punto la tabla Renderer_Article ha hecho un commit
		boolean stopOK = false;
		try
		{
			int numAttempts = 0;
			
			executorService.shutdownNow();
			do
			{
				stopOK = executorService.awaitTermination(500, TimeUnit.MILLISECONDS);
				numAttempts++;
				
				if (!stopOK)
					_log.error("The update mechanism of themes for the published articles is still in process");
			}
			while (!stopOK && numAttempts < 3);
		}
		catch (InterruptedException ie)
		{
			_log.error("The update mechanism of themes for the published articles has been interrupted while it was halting");
		}
		finally
		{
			if (stopOK)
				_log.info("The update mechanism of themes for the published articles has finished");
		}
	}
}