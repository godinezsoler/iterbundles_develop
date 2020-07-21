package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.protecmedia.iter.news.service.MetadataControlLocalServiceUtil;

public class GetOgImage extends HttpServlet implements Servlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(GetOgImage.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			long scopegroupId = PortalUtil.getScopeGroupId(request);
			
			DLFileEntry image = MetadataControlLocalServiceUtil.getDefaultOgImage(scopegroupId);
			
			long delegationId = GroupLocalServiceUtil.getGroup(scopegroupId).getDelegationId();
			InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, image.getUserId(), image.getGroupId(), image.getFolderId(), image.getName());
			
			addResponse(response, HttpServletResponse.SC_OK, IOUtils.toByteArray(is), MimeTypesUtil.getContentType(image.getTitle()));
		}
		catch(Exception e)
		{
			_log.error(e);
			addResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ".".getBytes(), "text/plain");
		}
		finally
		{
			
		}
	}
	
	private void addResponse(HttpServletResponse response, int responseStatus, byte[] b, String contentType )
	{
		try
		{
			response.setStatus(responseStatus);

			response.setContentType( contentType );
			ServletOutputStream out = response.getOutputStream();
			out.write( b );
			out.flush();
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
