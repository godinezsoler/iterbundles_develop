package com.protecmedia.iter.tracking.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.comments.CommentsConfigBean;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.CommentsConfigServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.NoSuchJournalArticleException;
import com.protecmedia.iter.news.model.Comments;
import com.protecmedia.iter.news.service.CommentsLocalServiceUtil;


public class MileniumTrackingComments extends HttpServlet implements Servlet {
   
	private static final long serialVersionUID = 2740693677625051632L;

	private static Log _log = LogFactoryUtil.getLog(MileniumTrackingComments.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException 
	{
		PortalUtil.setVirtualHostLayoutSet(request);
		
		String contentId = ParamUtil.get(request, "contentId", "");
		long groupId = ParamUtil.getLong(request, "groupId", -1L);
		try
		{
			JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), contentId);
			CommentsConfigBean commentsConfig = new CommentsConfigBean(groupId, request);
			
			if(commentsConfig.useDisqusConfig())
			{
				String  fullHtmlDisquspage = commentsConfig.getFullHTMLDisqusPage(contentId);
				response.setContentType( IterKeys.TEXT_HTML_CONTENT_TYPE );
				response.getWriter().print( fullHtmlDisquspage );
				
			}	
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		catch(NoSuchArticleException e)
		{
			_log.trace(e);
			RequestDispatcher dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/view-comments.jsp");
			dispatcher.forward(request, response);
		}
		catch(Exception e)
		{
			_log.trace(e);
		    e.printStackTrace(response.getWriter());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException 
	{
		IterRequest.setOriginalRequest(request);
	}	
	
	protected int devuelveEstado(Comments comment){
		
		boolean activo = comment.getActive();
		boolean moderado = comment.getModerated();
		int estadoActual = 4;
		
		if (activo && moderado){
			estadoActual = 1;
		}
		if (activo && !moderado){
			estadoActual = 2;
		}
		if (!activo && moderado){
			estadoActual = 3;
		}
		if (!activo && !moderado){
			estadoActual = 4;
		}
		return estadoActual;
		
	}
	
	protected void actualizaEstado(long commentId, int nuevoEstado) throws SystemException{
		if (nuevoEstado == 1){
			CommentsLocalServiceUtil.enableComment(commentId);
		}
		if (nuevoEstado == 2){
			CommentsLocalServiceUtil.enableComment(commentId);
		}
		if (nuevoEstado == 3){
			CommentsLocalServiceUtil.disableComment(commentId);
		}
		if (nuevoEstado == 4){
			CommentsLocalServiceUtil.disableComment(commentId);
		}
	}

}

