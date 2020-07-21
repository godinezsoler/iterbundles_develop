package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.news.util.TeaserContentUtil;
import com.protecmedia.iter.news.util.TopicsUtil;

public class GetFilterOpts extends HttpServlet implements Servlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(GetFilterOpts.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			List<String> userOptions = new ArrayList<String>();
			
			String options = request.getPathInfo();
			if( Validator.isNotNull(options) )
			{
				options = options.substring(1);
				
				JSONObject jsObj = JSONFactoryUtil.createJSONObject( new String(Base64.decode(options), Digester.ENCODING) );
				String filterType = jsObj.getString(IterKeys.FILTER_TYPE);
				String displayOption = jsObj.getString(IterKeys.FILTER_DISPLAY_OPT);
				
				if( filterType.equalsIgnoreCase(IterKeys.CATEGORIES) )
				{
					String select = "";
					List<String> catIds = Arrays.asList( jsObj.getString(IterKeys.FILTER_CAT_DATA).split(",") );
					List<String> vocIds = Arrays.asList( jsObj.getString(IterKeys.FILTER_VOC_DATA).split(",") );
					
					if( displayOption.equalsIgnoreCase( IterKeys.FILTER_SELECTED )  ) 
					{
						
						select = String.format( SQLQueries.GET_METADATA_SELECTED , jsObj.getString(IterKeys.FILTER_CAT_DATA)  );
					}
					else if( displayOption.equalsIgnoreCase(IterKeys.FILTER_DESCENDENTS)  )
					{
						select = String.format( SQLQueries.GET_METADATA_DESCENDENTS, 
													CategoriesUtil.getDiscreteCategoriesJoin(catIds),
													TopicsUtil.getSelectedExcludedTopicsSQL(vocIds, Validator.isNotNull(catIds),null, null, true) );
					}
					else if( displayOption.equalsIgnoreCase(IterKeys.FILTER_LEAFS)  ) 
					{
						// Se recuperan los metadatos situados en el último nivel de la jerarquía
						select = String.format( SQLQueries.GET_METADATA_LEAFS, 
													CategoriesUtil.getDiscreteCategoriesJoin(catIds),
													TopicsUtil.getSelectedExcludedTopicsSQL(vocIds, Validator.isNotNull(catIds), null, null) );
					}
					
					userOptions.addAll( TeaserContentUtil.getUserOptions( select ) );
				}
				else if( filterType.equalsIgnoreCase(IterKeys.SECTIONS) )
				{
					String select = "";
					if( displayOption.equalsIgnoreCase( IterKeys.FILTER_SELECTED )  ) 
					{
						select = String.format( SQLQueries.GET_SECTION_SELECTED , jsObj.getString(IterKeys.FILTER_DATA));
					}
					else if( displayOption.equalsIgnoreCase(IterKeys.FILTER_DESCENDENTS)  ) 
					{
						select = String.format( SQLQueries.GET_SECTION_DESCENDENTS, jsObj.getString(IterKeys.FILTER_DATA) );
					}
					
					userOptions = TeaserContentUtil.getUserOptions( select );
				}
					
			}
			
			if(userOptions.size() > 0)
				addResponse(response, HttpServletResponse.SC_OK,  StringUtils.join(userOptions.iterator(), "\n") );
			else
				addResponse(response, HttpServletResponse.SC_OK,  StringPool.BLANK );
			
		}
		catch (Exception e)
		{
			addResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,  StringPool.BLANK );
			_log.error(e);
		}
	}
	
	private void addResponse(HttpServletResponse response, int responseStatus, String data )
	{
		try
		{
			response.setStatus(responseStatus);

			if(Validator.isNotNull(data))
			{
				response.setContentType("text/xml");
				ServletOutputStream out = response.getOutputStream();
				out.write( data.getBytes() );
				out.flush();
			}
		}
		catch (IOException ioe)
		{
			_log.error(ioe);
		}
	}
}
