/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;


/**
 * This is a File Upload Servlet that is used with AJAX
 * to monitor the progress of the uploaded file. It will
 * return an XML object containing the meta information
 * as well as the percent complete.
 */
public class MileniumTrackingStatistics extends HttpServlet implements Servlet {
   
	private static final long serialVersionUID = 2740693677625051632L;

	public MileniumTrackingStatistics() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String contentId = ParamUtil.get(request, "contentId", "");
		long groupId = ParamUtil.getLong(request, "groupId", -1L);
		long plid = ParamUtil.getLong(request, "plid", 0);
		long categoryId = ParamUtil.getLong(request, "categoryId", 0);
		RequestDispatcher dispatcher = null;
		
		if (groupId > 0)
		{
			request.setAttribute("groupId", groupId);
			
			String layer = ParamUtil.get(request, "layer", StringPool.BLANK);
			
			//////////////////////////////			
			// ESTADISTICAS DE USUARIOS //
			//////////////////////////////
			if ("users".equals(layer))
			{
				dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/users-statistics-dashboard.jsp");
			}
			/////////////////////////////////
			// ESTADISTICAS DE NEWSLETTERS //
			/////////////////////////////////
			else if ("newsletters".equals(layer))
			{
				dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/newsletters-statistics-dashboard.jsp");
			}
			/////////////////////////////
			// ESTADISTICAS DE VISITAS //
			/////////////////////////////
			else
			{
				// Se piden estadísticas de un artículo
				if (Validator.isNotNull(contentId))
				{
					request.setAttribute("contentId", contentId);
					dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/visits-statistics-dashboard.jsp");
				}
				// Se piden estadísticas de una sección
				else if (Validator.isNotNull(plid))
				{
					request.setAttribute("plid", plid);
					long maxArticles = ParamUtil.getLong(request, "maxArticles", 5);
					request.setAttribute("maxArticles", maxArticles);
					dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/visits-statistics-dashboard.jsp");
				}
				// Se piden estadísticas de un metadato
				else if (Validator.isNotNull(categoryId))
				{
					request.setAttribute("categoryId", categoryId);
					dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/visits-statistics-dashboard.jsp");
				}
				// Se piden estadísticas del sitio
				else
				{
					String vocabularies = ParamUtil.getString(request, "vocabularies", StringPool.BLANK);
					request.setAttribute("vocabularies", vocabularies);
					long maxArticles = ParamUtil.getLong(request, "maxArticles", 5);
					request.setAttribute("maxArticles", maxArticles);
					boolean showVisits = ParamUtil.getBoolean(request, "showVisits", true);
					request.setAttribute("showVisits", showVisits);
					dispatcher = request.getRequestDispatcher("/html/milenium-tracking-portlet/visits-statistics-dashboard.jsp");
				}
			}
		}
		
		if (dispatcher != null) {
			dispatcher.forward(request, response);
		} else {
			System.out.println("Error: dispathcer is null");
		}
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	}
	

}

