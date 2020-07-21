package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.util.Base64;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.sectionservers.SectionServersMgr;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.MinifyUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.protecmedia.iter.base.service.WebResourceLocalServiceUtil;
import com.protecmedia.iter.base.service.util.WebResourceUtil;
import com.protecmedia.iter.base.util.OrphanResourceThread;
import com.protecmedia.iter.base.util.RsrcKind;

public class WebRSRC extends HttpServlet 
{
	private static Log _log = LogFactoryUtil.getLog(WebRSRC.class);

	private static final long serialVersionUID = 1L;

	private final static String GET_WEBRESOURCE_BY_MD5 = new StringBuffer(
		"SELECT rsrcid, rsrccontent, rsrccontenttype, orphandate	\n").append(
		"FROM theme_webresource  									\n").append(
		"	WHERE delegationid = %d AND rsrcmd5 = '%s' Limit 1		\n").toString();
    
	private final static String GET_RENDERERRSRC = new StringBuffer(
		"SELECT rsrcid, rsrccontent, rsrccontenttype, orphandate	\n").append(
		"FROM RendererRsrc  										\n").append(
		"	WHERE rsrcid = '%s'										\n").toString();

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		response.setCharacterEncoding(StringPool.UTF8);
		
		boolean invalidURL = false;

		try
		{
			String type		 	= null;
			String rsrccontent 	= null;
			byte[] content		= null;
			String uri 			= request.getRequestURI();
			
			if (Validator.isNotNull(uri))
			{
				PortalUtil.setVirtualHostLayoutSet(request);
				long groupId = GetterUtil.getLong( (Serializable) IterRequest.getAttribute(WebKeys.SCOPE_GROUP_ID) );
				
				if(groupId==0L)
					groupId = LayoutSetTools.getScopeGroupId( request.getHeader(WebKeys.HOST) );
				
				long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
				
				String[] uriArray = uri.split(StringPool.SLASH);
				if(uriArray != null && uriArray.length > 0)
				{
					String idWithExtension = uriArray[uriArray.length - 1];
					String idArray[] = idWithExtension.split("\\.");
					if (idArray.length == 2)
					{
						String id = idArray[0];
						RsrcKind rsrcKind = RsrcKind.getKind(uriArray.length == 5 ? uriArray[3] : RsrcKind.system);
						
						if (rsrcKind.equals(RsrcKind.theme) || rsrcKind.equals(RsrcKind.renderer))
						{
							ErrorRaiser.throwIfNull(id);
							
							String sql = rsrcKind.equals(RsrcKind.theme) 							? 
											String.format(GET_WEBRESOURCE_BY_MD5, delegationId, id) : 
											String.format(GET_RENDERERRSRC, id);			
							
							Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
							type 				= XMLHelper.getTextValueOf(dom, "/rs/row/@rsrccontenttype");
							rsrccontent 		= XMLHelper.getTextValueOf(dom, "/rs/row/@rsrccontent");
							
							if(Validator.isNotNull(rsrccontent))
								content = Base64.decodeBase64(rsrccontent);
							
							if ( PropsValues.ITER_THEME_RSRC_MINIFY_ON_THE_FLY )
							{
								content = MinifyUtil.minifyContent(request, content, type);
							}
							
							// Solo a los recursos que YA son huérfanos se les actualiza la fecha de orfandad.
							// Esta fecha también se utiliza como fecha de último acceso al recursos, de modo 
							// que se puede saber desde cuando no se consulta un recurso huérfano
							if (Validator.isNotNull(XMLHelper.getTextValueOf(dom, "/rs/row/@orphandate")))
							{
								OrphanResourceThread orphanRsrc = new OrphanResourceThread(XMLHelper.getTextValueOf(dom, "/rs/row/@rsrcid"), rsrcKind);
								orphanRsrc.start();
							}
						}
						else if (rsrcKind.equals(RsrcKind.ctxvar))
						{
							try
							{
								DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(id, groupId);
								type = WebResourceUtil.getContentTypeByType(dlFileEntry.getExtension());
								
								content = IOUtils.toByteArray(DLFileEntryLocalServiceUtil.getFileAsStream(
										delegationId, dlFileEntry.getUserId(), dlFileEntry.getGroupId(), 
										dlFileEntry.getFolderId(),  dlFileEntry.getName()));
							}
							catch (Exception e)
							{
								//Se ha producido una excepción( no se encuentra el archivo,etc)--> content será null y status 404
								_log.error(e);
							}
						}
						else if (rsrcKind.equals(RsrcKind.system))
						{
							//uri no contiene theme ni ctxvar, es del tipo: host +/base-portlet/webrsrc/67a238963d15cc3d7aebe2a6e647bc.js
							Document document 	= WebResourceLocalServiceUtil.getWebResourceFromMD5(id);
							type 				= XMLHelper.getTextValueOf(document, "/rs/row/@rsrctype");
							rsrccontent 		= XMLHelper.getTextValueOf(document, "/rs/row/@rsrccontent");
							
							if(Validator.isNotNull(rsrccontent))
								content = Base64.decodeBase64(rsrccontent);
						}
						else
						{
							//uriArray[3] no es ni theme ni ctxvar -->  url inválida(status 400)
							invalidURL = true;
						}
						
						if( Validator.isNull(content) || content.length == 0 )
						{
							//se controla que si es una url inválida, no se ponga status 404 ni se indique que el archivo es vacío
							if( !invalidURL )
							{
								response.setStatus(HttpServletResponse.SC_NOT_FOUND);
								response.setContentLength(1);
								response.setContentType("text/html");
								PrintWriter out = null;
								try 
								{
									out = response.getWriter();
									out.print(".");
									out.flush();
								}
								finally
								{
									if (out != null)
										out.close();
								}
								_log.error("Current URL resource content: \"" + request.getRequestURL() + "\" is empty");
							}
						}
						else
						{
							if(Validator.isNull(type))
							{
								_log.error("Current resource type: \"" + request.getRequestURL() + "\" is null");
							}
							else
							{
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType(type);
								response.addHeader("Access-Control-Allow-Origin", 
										SectionServersMgr.needProcessMobile(groupId) ? "*" :
										IterURLUtil.getIterProtocol().concat(LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost()));

								ServletOutputStream out = response.getOutputStream();

								long ini = System.currentTimeMillis();

								if ( rsrcKind.equals(RsrcKind.theme) && 
									(type.equalsIgnoreCase(ContentTypes.TEXT_CSS) || type.equalsIgnoreCase(ContentTypes.TEXT_JAVASCRIPT)) &&
									PropsValues.ITER_ENVIRONMENT.equalsIgnoreCase(WebKeys.ENVIRONMENT_LIVE))
								{
									LayoutSetTools.addStaticServers(groupId, out, content, uri);
									if (_log.isDebugEnabled())
										_log.debug( String.format("Added static servers. URI: %s\t%d", uri,(System.currentTimeMillis()-ini)) );
								}
								else
								{
									out.write(content);
									if (_log.isDebugEnabled())
										_log.debug( String.format("Without static servers. URI: %s\t%d", uri, (System.currentTimeMillis()-ini)) );
								}
								
								out.flush();
							}
						}
					}
					else
					{
						invalidURL = true;
					}
				}
				else
				{
					invalidURL = true;
				}
			}
			else
			{
				invalidURL = true;
			}
		}
		catch (Exception e)
		{
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(1);
			response.setContentType("text/html");
			PrintWriter out = null;
			try {
				out = response.getWriter();
				out.print(".");
				out.flush();
			}
			catch (IOException e1)
			{
				_log.error(e1);
			}
			finally
			{
				if (out != null)
					out.close();
			}
			_log.error("Current URL: \"" + request.getRequestURL() + "\" generates unexpected error");
			_log.error(e);
		}
		finally
		{
			IterRequest.unsetOriginalRequest();
		}
		
		if(invalidURL)
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentLength(1);
			response.setContentType("text/html");
			PrintWriter out = null;
			try {
				out = response.getWriter();
				out.print(".");
				out.flush();
			}
			catch (IOException e1)
			{
				_log.error(e1);
			}
			finally
			{
				if (out != null)
					out.close();
			}
			_log.error("Current URL: \"" + request.getRequestURL() + "\" is invalid");
		}
	}
}
