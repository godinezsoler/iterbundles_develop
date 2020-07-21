package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;

import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalStructureConstants;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.news.service.ArticlePollLocalServiceUtil;
import com.protecmedia.iter.news.util.ArticleViewerUtil;

public class GetArticle extends HttpServlet implements Servlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(GetArticle.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		String result  = "";
		boolean isJSON = false;
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			
			long scopegroupId = PortalUtil.getScopeGroupId(request);
			
			long globalGroupId = GroupMgr.getGlobalGroupId();
			String articleId = request.getPathInfo();
			
			String requestURL = request.getRequestURL().toString();
			
			// ITER-889	getArticle no usa el protocolo correcto en los links de relacionados ni artículos instrumentales
			URL url = new URL(requestURL);
			requestURL = new URL(IterSecureConfigTools.getConfiguredHTTPS(scopegroupId) ? Http.HTTPS : Http.HTTP, url.getHost(), url.getFile()).toString();
			
			if(Validator.isNull(articleId))
			{
				if(Validator.isNotNull(requestURL))
				{
					articleId = requestURL.substring( requestURL.lastIndexOf(StringPool.SLASH)+1 );
				}
			}
			else
				articleId = articleId.substring(1);
			
			String ext 	= FilenameUtils.getExtension(articleId);
			isJSON 		= "json".equalsIgnoreCase(ext);
			if ("xml".equalsIgnoreCase(ext) || isJSON)
				articleId = FilenameUtils.removeExtension(articleId);
			
			JournalArticle ja = JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId);
			
			Element articleInfo = SAXReaderUtil.read( ja.getContent() ).getRootElement();
			LayoutSetTools.addStaticServers(scopegroupId, articleInfo);
			
			ArticleViewerUtil.applyRenditions( articleInfo, scopegroupId, globalGroupId, articleId );
			
			Element metadata = ArticleViewerUtil.getMetadataXml(ja, scopegroupId, request, requestURL);
			articleInfo.add(metadata);
			
			Element relatedContent = ArticleViewerUtil.getRelatedContent(ja, requestURL);
			articleInfo.add(relatedContent);
			
			// Si es una encuesta, le añade los IDs de la pregunta y las respuestas
			if (JournalStructureConstants.STRUCTURE_POLL.equals(ja.getStructureId()))
			{
				articleInfo = ArticlePollLocalServiceUtil.addPollInfoToArticleContent(ja.getArticleId(), articleInfo.getDocument()).getRootElement();
			}
			
			addClass2Related(articleInfo);
			
			String articleTransformed = ArticleViewerUtil.transformArticleContent(request, articleInfo, articleId, scopegroupId);
			
			String phpArticle = ArticleViewerUtil.setPhp(request, response, articleId, scopegroupId, articleTransformed, ja.getModifiedDate(), isJSON);
			
			result = Validator.isNotNull(phpArticle) ? phpArticle : articleTransformed;
			
			if (isJSON)
				result = JSONUtil.toJSONString(result);
			
			addResponse(response, HttpServletResponse.SC_OK, result, isJSON);
			
		}
		catch(NoSuchArticleException nsae)
		{
			_log.error(nsae);
			addResponse(response, HttpServletResponse.SC_NOT_FOUND, result, isJSON);
		}
		catch(Throwable e)
		{
			_log.error(e);
			addResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result, isJSON);
		}
		finally
		{
			IterRequest.unsetOriginalRequest();
		}
	}
	
	private static final String INTERNAL_XPATH = "/root/dynamic-element[@name=\"InternalLink\" and dynamic-element[@name=\"ContentId\" and dynamic-content/text()=\"%s\"]]/dynamic-element[@name=\"Class\"]/dynamic-content";
	private static final String EXTERNAL_XPATH = "/root/dynamic-element[@name=\"ExternalLink\" and dynamic-content/text()=\"%s\" and dynamic-element[@name=\"Link\" and dynamic-content/text()=\"%s\"]]/dynamic-element[@name=\"Class\"]/dynamic-content";
	
	/**
	 * Corriere del Ticino ha solicitado incluir la clase de los relacionados que se configura desde MLN.<br/>
	 * @since 27/09/2018
	 * @see ITER-937 REST API: proporcionar la clase de vínculo, establecida desde Milenium
	 */
	private void addClass2Related(Element articleInfo)
	{
		// Se obteien los relacionados en el DOM
		List<Node> links = articleInfo.selectNodes("relatedcontent/link");
		for (Node link : links)
		{
			String xpath;
			String rel = ((Element)link).attributeValue("rel");
			String href= ((Element)link).attributeValue("href");
			
			if (rel.equals("internal"))
			{
				// El href es el getArticle del relacionado y por tanto contiene el articleId
				String id = href.substring(href.lastIndexOf("/")+1);
				int index = id.lastIndexOf(".");
				if (index != -1)
					id = id.substring(0, index);
				
				xpath = String.format(INTERNAL_XPATH, id);
			}
			else
			{
				String name = ((Element)link).getStringValue();
				xpath = String.format(EXTERNAL_XPATH, name, href);
			}
			
			String className = XMLHelper.getStringValueOf(articleInfo, xpath);
			if (Validator.isNotNull(className))
				((Element)link).addAttribute("class", className);
		}
	}
	
	private void addResponse(HttpServletResponse response, int responseStatus, String domAsXML, boolean isJSON )
	{
		try
		{
			response.setStatus(responseStatus);

			if(Validator.isNotNull(domAsXML))
			{
				response.setContentType(isJSON ? "application/json" : "text/xml");
				ServletOutputStream out = response.getOutputStream();
				out.write( domAsXML.getBytes() );
				out.flush();
			}
		}
		catch (IOException ioe)
		{
			_log.error(ioe);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		
	}
}
