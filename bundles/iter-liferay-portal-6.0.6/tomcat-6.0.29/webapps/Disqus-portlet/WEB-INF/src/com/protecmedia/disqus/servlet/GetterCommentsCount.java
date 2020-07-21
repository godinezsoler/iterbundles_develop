package com.protecmedia.disqus.servlet;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.CommentsConfigServiceUtil;
import com.protecmedia.iter.base.service.StatisticMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;

public class GetterCommentsCount  extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 5556207878292153828L;

	private static Log _log = LogFactoryUtil.getLog(GetterCommentsCount.class);

	private static String HTML_BY_AJAX = "text/html-by-ajax; charset=UTF-8";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		try
		{
			String[] ids = request.getParameterValues("id");
			ErrorRaiser.throwIfNull(ids);
			Long groupId = GetterUtil.getLong(request.getParameter("group"), -1);
			ErrorRaiser.throwIfFalse(groupId != null && groupId >= 0);
			Document domTexts = CommentsConfigServiceUtil.getDisqusCommentsHTML(groupId);
			String zeroComments = GetterUtil.getString(XMLHelper.getTextValueOf(domTexts, "/rs/row/zerocommentshtml"), "");
			String oneComments = GetterUtil.getString(XMLHelper.getTextValueOf(domTexts, "/rs/row/onecommentshtml"), "");
			String nComments = GetterUtil.getString(XMLHelper.getTextValueOf(domTexts, "/rs/row/ncommentshtml"), "");
			
			JSONObject json = JSONFactoryUtil.createJSONObject();
			json.put("zero", zeroComments);
			json.put("multiple", nComments);
			json.put("one", oneComments);
			
			JSONArray jsonListIds = JSONFactoryUtil.createJSONArray();
			Document dom = StatisticMgrLocalServiceUtil.getStatisticsInIds(ids);
			if(Validator.isNotNull(dom))
			{
				for (String id : ids)
				{
					Node row = dom.selectSingleNode("/rs/row[@contentId="+id+"]");
					Long counter = XMLHelper.getLongValueOf(row, "@statisticCounter", -1);
					JSONObject jsonRow = JSONFactoryUtil.createJSONObject();
					jsonRow.put("id", id);
					jsonRow.put("counter", (Validator.isNotNull(row) && counter != null && counter >= 0) ? counter : 0);
					jsonListIds.put(jsonRow);
				}
			}
			json.put("counts", jsonListIds);
			
			response.setStatus(HttpStatus.SC_OK);
			response.getWriter().print(json.toString());
			response.setContentType(HTML_BY_AJAX);
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
	}
}
