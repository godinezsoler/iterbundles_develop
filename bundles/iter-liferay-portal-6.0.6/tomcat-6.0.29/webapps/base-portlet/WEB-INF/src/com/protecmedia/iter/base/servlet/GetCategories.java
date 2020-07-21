package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.NoSuchVocabularyException;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;

public class GetCategories extends HttpServlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(GetCategories.class);
	
	private final String CAT_ID = "categoryid";
	private final String CAT_NAME = "name";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		StringBuilder result = new StringBuilder("");
		int responseCode = HttpServletResponse.SC_OK;
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			
			String pathInfo = request.getPathInfo();
			
			if(Validator.isNotNull(pathInfo))
			{
				if(pathInfo.startsWith(StringPool.SLASH))
					pathInfo = pathInfo.substring(1);
				
				if(pathInfo.endsWith(StringPool.SLASH))
					pathInfo = pathInfo.substring(0, pathInfo.length()-1);
				
				String[] pathSegs = pathInfo.split(StringPool.SLASH);
				if( pathSegs.length!=1 || Validator.isNull(pathSegs[0]) )
				{
					_log.debug("BAD_REQUEST: " + request.getPathInfo());
					responseCode = HttpServletResponse.SC_BAD_REQUEST;
				}
				else
				{
					long scopegroupId = PortalUtil.getScopeGroupId(request);
					Group grp = GroupLocalServiceUtil.getGroup(scopegroupId);
					long delegationId = grp.getDelegationId();
					
					String vocName = pathSegs[0];
					if(delegationId!=0)
						vocName = delegationId+IterGlobalKeys.DLG_SEPARATOR+vocName;
					
					AssetVocabularyLocalServiceUtil.getGroupVocabulary(GroupMgr.getGlobalGroupId(), vocName);
					
					List<Map<String, Object>> categoriesList = CategoriesUtil.getCategoriesByVocabularyname(vocName);
					
					if(categoriesList!=null && categoriesList.size()>0)
					{
						for(Map<String, Object> row : categoriesList)
						{
							result
								.append( String.valueOf(row.get(CAT_ID)) )
								.append( StringPool.TAB )
								.append( String.valueOf(row.get(CAT_NAME)) )
								.append( StringPool.RETURN_NEW_LINE );
						}
					}
					else
						_log.debug("Vocabulary " + vocName + " does not has categories.");
				}
			}
			else
			{
				_log.debug("BAD_REQUEST: " + request.getPathInfo());
				responseCode = HttpServletResponse.SC_BAD_REQUEST;
			}
			
			
		}
		catch(NoSuchVocabularyException nsve)
		{
			_log.error(nsve.toString());
			_log.debug(nsve);
			responseCode = HttpServletResponse.SC_NOT_FOUND;
		}
		catch (Exception e)
		{
			_log.error(e);
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		finally
		{
			addResponse(response, responseCode, result.toString());
			IterRequest.unsetOriginalRequest();
		}
	}
	
	private void addResponse(HttpServletResponse response, int responseStatus, String data )
	{
		try
		{
			response.setCharacterEncoding(StringPool.UTF8);
			response.setContentType(ContentTypes.TEXT_PLAIN_UTF8);
			response.setStatus(responseStatus);

			byte[] responseData = null;
			
			if ( Validator.isNotNull(data) || responseStatus==HttpServletResponse.SC_OK )
				responseData = data.getBytes();
			else
				responseData = StringPool.PERIOD.getBytes();
			
			ServletOutputStream out = response.getOutputStream();
			out.write( responseData );
			out.flush();
		}
		catch (IOException ioe)
		{
			_log.error(ioe);
		}
	}

}
