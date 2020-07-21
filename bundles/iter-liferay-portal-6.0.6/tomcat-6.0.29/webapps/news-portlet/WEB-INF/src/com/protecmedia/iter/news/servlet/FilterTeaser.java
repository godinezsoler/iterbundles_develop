package com.protecmedia.iter.news.servlet;

import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.service.util.IterKeys;

public class FilterTeaser extends HttpServlet implements Servlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(FilterTeaser.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			String pathInfo = request.getPathInfo();
			if( Validator.isNotNull(pathInfo) && pathInfo.startsWith("/") && pathInfo.length()>1)
			{
				String[] params = pathInfo.substring(1).split("/");
				JSONObject jsObj = JSONFactoryUtil.createJSONObject( new String(Base64.decode(params[0]), Digester.ENCODING) );
				String filterBy = params[1];
				String filterOption = params[2];
				
				StringBuilder forwardPath = new StringBuilder("/html/teaser-viewer-portlet/teaser_filter.jsp?");
				Iterator<String> itr = jsObj.keys();
				while(itr.hasNext())
				{
					String key = String.valueOf( itr.next() );
					String value = jsObj.getString(key);

					forwardPath.append( key ).append(StringPool.EQUAL).append( value ).append(StringPool.AMPERSAND);
				}
				
				forwardPath.append( IterKeys.FILTER_TYPE ).append(StringPool.EQUAL).append( filterBy ).append(StringPool.AMPERSAND);
				forwardPath.append( IterKeys.FILTER_DATA ).append(StringPool.EQUAL).append( filterOption );
				
				RequestDispatcher rd = request.getRequestDispatcher( forwardPath.toString() );
				rd.forward(request, response);
			}
		}
		catch (Exception e)
		{
			_log.error(e);
		}
		
	}
}
