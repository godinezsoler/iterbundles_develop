package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.URLSigner;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.NoSuchTemplateException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.liferay.util.portlet.PortletRequestUtil;
import com.liferay.util.servlet.ServletResponseUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class RenderArticle extends HttpServlet 
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(RenderArticle.class);
	
	private static String ERROR_MSG = String.format("%s: %%s", RenderArticle.class.getName());
	
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException 
	{
		long globalGroupId 	= GroupMgr.getGlobalGroupId();
		long scopeGroupId 	= globalGroupId;
		try
		{
			ErrorRaiser.throwIfFalse( PortalUtil.setVirtualHostLayoutSet(request) );
			
			scopeGroupId 	= PortalUtil.getScopeGroupId(request);
			String path 	= GetterUtil.getString(request.getPathInfo());
			String[] chunks = path.split("/");
			ErrorRaiser.throwIfFalse(chunks.length > 2, IterErrorKeys.XYZ_E_INVALID_URL_ZYX, path);
			
			String articleId = getArticleId(chunks[1]);
			JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId);

			JournalTemplate template = null;
			try
			{
				long templateId  = Long.parseLong( chunks[2] );
				template = JournalTemplateLocalServiceUtil.getJournalTemplate(templateId);
			}
			catch(NumberFormatException e)
			{
				String templateId = new String(Base64.decode(chunks[2]));
				
				if (WebKeys.ABTESTING_TEMPLATE.equals(templateId))
				{
					template = JournalTemplateLocalServiceUtil.getTemplate(GroupMgr.getGlobalGroupId(), templateId);
				}
				else
				{
					template = JournalTemplateLocalServiceUtil.getTemplate(scopeGroupId, templateId);	
				}
			}
			
			HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
			
			// Recupera el plid y el tipo de contenido
			long plid = 0;
			String contentType = ContentTypes.TEXT_HTML_UTF8;
			if (chunks.length > 3)
			{
				// Recorre los parámetros (Se aceptan entre 0 y 2)
				for (int i = 3; i < Math.min(chunks.length, 5); i++)
				{
					// Si es numérico, es un plid
					if (StringUtils.isNumeric(chunks[i]))
					{
						plid = Long.parseLong(chunks[i]);
					}
					// Si no, es personalizado
					else
					{
						// Comprueba si es un tipo de contenido
						if (chunks[i].startsWith("content-type:"))
						{
							String encodedData = chunks[i].split(StringPool.COLON)[1];
							contentType = new String(Base64.decode(encodedData), StringPool.UTF8);
						}
					}
				}
			}
			
			// Si el PLID es 0 es porque la plantilla NO necesitan la sección actual, 
			// así que igualmente se inicializa con este valor.
			SectionUtil.setSectionPlid( originalRequest, plid );
			
			ThemeDisplay themeDisplay = buildThemeDisplay(request, scopeGroupId, plid);
			
	        String xmlRequest 	= PortletRequestUtil.toXML(request);
			String viewMode 	= buildViewMode(request, scopeGroupId, articleId);
			
			// Inicializa la respuesta
			StringBuilder html = new StringBuilder();
			// Añade el HTML renderizado
			html.append(PageContentLocalServiceUtil.getArticleContent(article, template.getTemplateId(), null, viewMode, themeDisplay, xmlRequest, -1, request, -1, 1));
			// Si requiere PHP de control de acceso, lo inserta al inicio
			html.insert(0, PHPUtil.getCheckAccessPHPCode());
			
			// Si la plantilla velocity NO quiere cachear el contenido, no lo debería cachear el Apache
			// De momento se quiere que SIEMPRE SE CACHEE
//			if ( !GetterUtil.getBoolean(String.valueOf(IterRequest.getAttribute(WebKeys.ITER_CACHE_TRANSFORM))) )
//			{
//				response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
//				if (_log.isTraceEnabled())
//					_log.trace( String.format("Article's content (%s) for group %d with template %s and plid %d must'nt cache", 
//								articleId, scopeGroupId, template.getTemplateId(), plid) );
//			}

			response.setHeader(WebKeys.ITER_RESPONSE_NEEDS_PHP, "1");
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			_log.debug(html.toString());
			ServletResponseUtil.sendFile(request, response, null, html.toString().getBytes(StringPool.UTF8), contentType);
		}
		catch (NoSuchArticleException nsae)
		{
			_log.error(nsae);
			IterMonitor.logEvent(scopeGroupId, Event.ERROR, new Date(), String.format(ERROR_MSG, nsae.toString()) , nsae);
			PortalUtil.sendError(HttpServletResponse.SC_NOT_FOUND, nsae, request,response);
		}
		catch (NoSuchTemplateException nste)
		{
			_log.error(nste);
			IterMonitor.logEvent(scopeGroupId, Event.ERROR, new Date(), String.format(ERROR_MSG, nste.toString()) , nste);
			PortalUtil.sendError(HttpServletResponse.SC_NOT_FOUND, nste, request,response);
		}
		catch(Exception e)
		{
			_log.error(e);
			IterMonitor.logEvent(scopeGroupId, Event.ERROR, new Date(), String.format(ERROR_MSG, e.toString()) , e);
			PortalUtil.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e, request,response);
		}
		finally
		{
			IterRequest.unsetOriginalRequest();
		}
	}
	
	private static String getArticleId(String crc_articleId) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(crc_articleId.length() > IterURLUtil.ARTICLEID_CRC_LENGTH, IterErrorKeys.XYZ_E_INVALID_ARTICLEID_ZYX);
		
		String crc 		 = crc_articleId.substring(0, IterURLUtil.ARTICLEID_CRC_LENGTH);
		String articleId = crc_articleId.substring(IterURLUtil.ARTICLEID_CRC_LENGTH);
		
		// Si el CRC que viene en la URL tiene coincide con el calculado, retorna el resto. Si no, retorna el articleId tal cual. 
		return crc.equalsIgnoreCase(URLSigner.generateSign(articleId, IterURLUtil.ARTICLEID_CRC_LENGTH)) ? articleId : crc_articleId;
	}
	
	private String buildViewMode(HttpServletRequest request, long scopeGroupId, String articleId) throws Exception
	{
		Layout section = SectionUtil.getSection(request);
		
		// En layoutIds irán los Los Layout.uuid_ de las secciones seleccionadas, o de la sección actual
		// Si dicha lista está vacía o se ha elegido DEFAULT, se tomará la sección por defecto del artículo
		String[] layoutIds = (section == null) ? null : new String[]{section.getUuid()};
		return PageContentLocalServiceUtil.getArticleContextInfo(scopeGroupId, articleId, layoutIds);
	}

	private ThemeDisplay buildThemeDisplay(HttpServletRequest request, long scopeGroupId, long plid) throws Exception
	{
		Company company 	= CompanyLocalServiceUtil.getCompany( GroupMgr.getCompanyId() );
		User user 			= company.getDefaultUser();
		boolean isSecure 	= IterSecureConfigTools.getHTTPS(scopeGroupId);
		Locale locale		= user.getLocale();
		
		HttpSession session = request.getSession();
		session.setAttribute(WebKeys.USER_ID, user.getUserId());
		session.setAttribute("org.apache.struts.action.LOCALE", locale);

		String portalURL 	= PortalUtil.getPortalURL(request, isSecure);
		
		String cdnHost 		= (isSecure) ? PortalUtil.getCDNHostHttps() : PortalUtil.getCDNHostHttp();
		cdnHost = ParamUtil.getString(request, "cdn_host", cdnHost);
		
		String contextPath 	= PortalUtil.getPathContext();

		ThemeDisplay themeDisplay = IterLocalServiceUtil.rebuildThemeDisplayForAjax(
										scopeGroupId, company.getCompanyId(), LocaleUtil.toLanguageId(locale), plid, isSecure, user.getUserId(), 
										true, PortalUtil.getPathFriendlyURLPublic(), PortalUtil.getPathFriendlyURLPrivateUser(), PortalUtil.getPathFriendlyURLPrivateGroup(), 
										request.getServerName(), cdnHost, cdnHost.concat(PortalUtil.getPathImage()), 
										PortalUtil.getPathMain(), PortalUtil.getPathContext(), portalURL.concat(contextPath), "", request);
		return themeDisplay;
	}
}
