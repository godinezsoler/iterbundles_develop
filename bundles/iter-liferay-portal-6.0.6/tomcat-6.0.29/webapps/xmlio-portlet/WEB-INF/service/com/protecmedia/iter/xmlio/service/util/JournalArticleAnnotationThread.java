package com.protecmedia.iter.xmlio.service.util;

import java.io.InputStream;
import java.util.List;

import javax.xml.transform.Source;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;

public class JournalArticleAnnotationThread extends Thread
{
	/** Logger */
	private static Log _log = LogFactoryUtil.getLog(JournalArticleAnnotationThread.class);
	
	private String articleId;
	private List<Long> groupsIds;
	private String oldContent;
	private String newContent;

	public JournalArticleAnnotationThread(String articleId, List<Long> groupsIds, String oldContent, String newContent)
	{
		super("Journal Article Annotation Thread (" + articleId + ")");
		this.articleId = articleId;
		this.groupsIds = groupsIds;
		this.oldContent = oldContent;
		this.newContent = newContent;
	}
	
	@Override
	public void run()
	{
		_log.debug("Community Publisher started");
		
		if (isContentModified(oldContent, newContent))
		{
			for (long groupId : groupsIds)
			{
				String annotationLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/annotations/contentmodified/text()"), null);
				if (null == annotationLiteral)
				{
					annotationLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/annotations/content/text()"), "Se ha modificado el contenido del artículo");
				}
				VisitsStatisticsLocalServiceUtil.addArticleStatisticsAnnotation(groupId, articleId, annotationLiteral);
			}
		}
		
		_log.debug("Community Publisher stopped");
	}
	
	private boolean isContentModified(String oldContent, String newContent)
	{
		boolean isModified = false;
		ClassLoader classLoader = PortalClassLoaderUtil.getClassLoader();
		if(classLoader != null)
		{
			InputStream is = null;
			try
			{
				is = classLoader.getResourceAsStream("cleanArticleContent.xsl");
				String xsl = IOUtils.toString(is, "UTF-8");
				
				// Realiza la transformación del XML.
				Source soOld = XSLUtil.getSource(oldContent);
				Source soNew = XSLUtil.getSource(newContent);
				
				String txtOld = XSLUtil.transform(soOld, XSLUtil.getSource(xsl), "xml");
				String txtNew = XSLUtil.transform(soNew, XSLUtil.getSource(xsl), "xml");
				
				isModified = !txtOld.equals(txtNew);
			}
			catch (Throwable th)
			{
				_log.error("Unable to check article content changes for statistics annotation.");
				_log.error(th);
			}
			finally
			{
				IOUtils.closeQuietly(is);
			}
		}
		
		return isModified;
	}
}
