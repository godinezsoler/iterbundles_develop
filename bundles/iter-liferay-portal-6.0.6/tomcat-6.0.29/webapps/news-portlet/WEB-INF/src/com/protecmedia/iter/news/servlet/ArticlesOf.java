package com.protecmedia.iter.news.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.IterRulesUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.news.util.ArticleViewerUtil;

public class ArticlesOf extends HttpServlet implements Servlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(ArticlesOf.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		String result 	= "";
		boolean isJSON 	= "json".equalsIgnoreCase( FilenameUtils.getExtension(request.getPathInfo()) );
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			
			// Comprueba si deben añadirse las reglas de los JSON en el Apache
			long groupId = PortalUtil.getScopeGroupId(request);
			if (isJSON && IterRulesUtil.needJsonrules(groupId))
				IterRulesUtil.regenerateApacheIterRules(groupId);

			result = ArticleViewerUtil.getArticlesOfSection(request, response);
			
			addResponse(response, HttpServletResponse.SC_OK, result, isJSON);
		}
		catch (NoSuchLayoutException nsle)
		{
			_log.error(nsle);
			addResponse(response, HttpServletResponse.SC_NOT_FOUND, result, isJSON);
		}
		catch(Exception e)
		{
			_log.error(e);
			addResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result, isJSON);
		}
	}
	
	private void addResponse(HttpServletResponse response, int responseStatus, String domAsXML, boolean isJSON)
	{
		try
		{
			response.setStatus(responseStatus);

			if(Validator.isNotNull(domAsXML))
			{
				response.setContentType( isJSON ? "application/json" : "text/xml" );
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
