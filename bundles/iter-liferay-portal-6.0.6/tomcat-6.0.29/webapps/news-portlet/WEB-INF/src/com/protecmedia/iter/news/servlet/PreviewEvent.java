package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.restapi.resource.live.post.RestApiPostModel;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;


public class PreviewEvent extends HttpServlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log log = LogFactoryUtil.getLog(PreviewEvent.class);
	private static final String ERR_GROUP_ID = "ERR_GROUP_ID";
	private static final String ERR_ARTICLE_NOT_FOUND = "ERR_ARTICLE_NOT_FOUND";
	private static final String ERR_SECTION_NOT_FOUND = "ERR_SECTION_NOT_FOUND";
	private static final String ERR_POST_NOT_FOUND = "ERR_POST_NOT_FOUND";
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// Establece el layoutset
		PortalUtil.setVirtualHostLayoutSet(request);
		
		try
		{
			// Obtiene el grupo
			long groupId = getGroupId(request);
			
			// Recupera el identificador del directo
			String articleId = getArticleId(request);

			// Comprueba que exista el artículo
			validateArticle(articleId);
			
			// Recupera la sección principal del artículo
			long plid = getArticleMainSection(groupId, articleId);
			
			// Recupera el identificador del post si se ha indicado uno
			String postId = getPostId(request);
			
			String html = Validator.isNotNull(postId) ? renderPost(groupId, postId.split(StringPool.SLASH)[0], request) : StringPool.BLANK;

			// Añade el header para que se procese PHP
			response.setHeader(WebKeys.ITER_RESPONSE_NEEDS_PHP, "1");
			
			// Pasa el control al JSP
			request.setAttribute("articleId", articleId);
			request.setAttribute("plid", plid);
			request.setAttribute("postId", postId);
			request.setAttribute("html", html);
			RequestDispatcher dispatcher = request.getRequestDispatcher("/html/event/previewevent.jsp");
			dispatcher.forward(request, response);
		}
		catch (ServiceError e)
		{
			String error = e.getErrorCode();
			
			if (ERR_GROUP_ID.equals(error))
			{
				buildErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);
			}
			else if (ERR_ARTICLE_NOT_FOUND.equals(error) || ERR_SECTION_NOT_FOUND.equals(error) || ERR_POST_NOT_FOUND.equals(error))
			{
				buildErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, error);
			}
		}
	}
	
	private long getGroupId(HttpServletRequest request) throws ServiceError
	{
		try
		{
			return PortalUtil.getScopeGroupId(request);
		}
		catch (Throwable th)
		{
			log.error(th);
			throw new ServiceError(ERR_GROUP_ID, ERR_GROUP_ID);
		}
		
	}
	
	private String[] getParams(HttpServletRequest request)
	{
		String path = request.getPathInfo();
		if (path.startsWith(StringPool.SLASH))
			path = path.substring(1);
		return path.split(StringPool.SLASH);
	}
	
	private String getArticleId(HttpServletRequest request) throws ServiceError
	{
		// Obtiene los parámetros
		String[] params = getParams(request);
		
		// Comprueba que exista el artículo
		validateArticle(params[0]);
		
		// Retorna el articleId
		return params[0];
	}
	
	private String getPostId(HttpServletRequest request) throws ServiceError
	{
		// Obtiene los parámetros
		String[] params = getParams(request);
		
		// Si no se indica postId, retorna blanco
		if (params.length < 2)
			return StringPool.BLANK;
		
		// Si se indica un postId, comprueba que exista el post y obtiene su fecha de última modificación
		String updatedate = validatePost(params[1]);
		
		// Retorna el postId y la fecha de modificación en el formato requerido por el servlet RenderPost
		return params[1] + StringPool.SLASH + updatedate;
	}

	private void validateArticle(String articleId) throws ServiceError
	{
		try
		{
			JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
		}
		catch (Throwable th)
		{
			log.error(th);
			throw new ServiceError(ERR_ARTICLE_NOT_FOUND, ERR_ARTICLE_NOT_FOUND);
		}
	}

	private String validatePost(String postId) throws ServiceError
	{
		try
		{
			RestApiPostModel post = new RestApiPostModel(postId);
			return post.toJson().getString("updatedate");
		}
		catch (Throwable th)
		{
			log.error(th);
			throw new ServiceError(ERR_POST_NOT_FOUND, ERR_POST_NOT_FOUND);
		}
	}

	private long getArticleMainSection(long groupId, String articleId) throws ServiceError
	{
		try
		{
			PageContent pagecontent = PageContentLocalServiceUtil.getDefaultPageContentByContentId(groupId, articleId);
			Layout layout = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(pagecontent.getLayoutId(), groupId);
			return layout.getPlid();
		}
		catch (Throwable th)
		{
			log.error(th);
			throw new ServiceError(ERR_SECTION_NOT_FOUND, ERR_SECTION_NOT_FOUND);
		}
	}
	
	private void buildErrorResponse(HttpServletResponse response, int status, String error)
	{
		response.setContentType("text/plain");
		response.setCharacterEncoding(StringPool.UTF8);
		response.setStatus(status);
		
	    PrintWriter out = null;
		try
		{
			out = response.getWriter();
			out.print(error);
		}
		catch (IOException e)
		{
			log.error(e);
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

	private static String renderPost(long groupId, String postId, HttpServletRequest request)
	{
		String html = StringPool.BLANK;
		try
		{
			MethodKey _getMethodKey1 = new MethodKey("com.liferay.restapi.resource.RestApiLiveManager", "renderPost", java.lang.Long.class, java.lang.String.class, javax.servlet.http.HttpServletRequest.class);
			html = (String) PortalClassInvoker.invoke(false, _getMethodKey1, groupId, postId, request);
		}
		catch (Throwable th)
		{
			log.error(th);
		}
		return html;
	}
}
