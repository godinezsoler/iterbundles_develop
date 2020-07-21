package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.util.VisitsStatisticsUtil;

public class ArticleVisitsServlet extends HttpServlet 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(ArticleVisitsServlet.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			IterRequest.setOriginalRequest(request);
			
			JSONObject json = JSONFactoryUtil.createJSONObject();
			JSONArray jsonListIds = JSONFactoryUtil.createJSONArray();
			
			json.put("counts", jsonListIds);
			
			long groupId = ParamUtil.getLong(request, "group", 0);
			ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			String[] articleIds = request.getParameterValues("id");
			Document dom = VisitsStatisticsUtil.getTotalArticleVisits(groupId, articleIds);
			
			for (String articleId : articleIds)
			{
				JSONObject jsonRow = JSONFactoryUtil.createJSONObject();
				jsonRow.put("id", 		articleId);
				jsonRow.put("counter",  XMLHelper.getLongValueOf(dom, String.format("/rs/row[@articleid='%s']/@visits", articleId)));
				jsonListIds.put(jsonRow);
			}
			
			// Construye la respuesta del servlet
			buildResponse(response, HttpServletResponse.SC_OK, json);
		}
		catch (Throwable th)
		{
			_log.debug(th);
			_log.error(th.toString());
			buildResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
	}
	
	protected void doPost (HttpServletRequest request, HttpServletResponse response)
	{
		this.doGet(request, response);
	}
	
	private void buildResponse(HttpServletResponse response, int status, JSONObject result)
	{
		response.setStatus(status);
		
		PrintWriter out = null;
		try
		{
			out = response.getWriter();
			if (null == result)
			{
				response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
				response.setContentLength(1);
				out.print(StringPool.PERIOD);
			}
			else
			{
				response.setContentType(WebKeys.CONTENT_TYPE_HTML_BY_AJAX);
				out.print(result.toString());
			}
			out.flush();
		}
		catch (IOException e)
		{
			_log.error(e);
		}
		finally
		{
			if (null != out)
				out.close();
		}
	}

}
