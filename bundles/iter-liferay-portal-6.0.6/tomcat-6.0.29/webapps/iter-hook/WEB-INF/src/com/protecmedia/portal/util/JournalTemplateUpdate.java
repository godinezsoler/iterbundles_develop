package com.protecmedia.portal.util;

import java.io.File;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.liferay.portlet.journal.util.GlobalJournalTemplateMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;

public class JournalTemplateUpdate extends JournalTemplateFunction
{
	private String 			_structureId;
	private String 			_name; 
	private String 			_description;
	private String 			_xsl;
	private boolean 		_formatXsl;
	private String 			_langType; 
	private boolean 		_cacheable; 
	private boolean 		_smallImage;
	private String 			_smallImageURL;
	private File 			_smallFile;
	private ServiceContext 	_serviceContext;
	
	public JournalTemplateUpdate(long groupId, String templateId, String structureId, String name, 
			String description, String xsl, boolean formatXsl, String langType, boolean cacheable, 
			boolean smallImage, String smallImageURL, File smallFile, ServiceContext serviceContext)
	{
		super(groupId, templateId);
		
		_structureId 	= structureId;
		_name 			= name; 
		_description 	= description;
		_xsl 			= xsl;
		_formatXsl 		= formatXsl;
		_langType 		= langType;
		_cacheable 		= cacheable;
		_smallImage 	= smallImage;
		_smallImageURL 	= smallImageURL;
		_smallFile 		= smallFile;
		_serviceContext = serviceContext;
	}
	
	@Override
	protected String getFunctionName()
	{
		return "UPDATE";
	}
	
	@Override
	protected JournalTemplate doCall() throws PortalException, SystemException
	{
		return JournalTemplateLocalServiceUtil.updateTemplateExtra(_groupId, _templateId, _structureId, _name, 
				 			  _description, _xsl, _formatXsl, _langType, _cacheable, 
				 			  _smallImage, _smallImageURL, _smallFile, _serviceContext);
	}

	public static JournalTemplate invoke(long groupId, String templateId, String structureId, String name, 
									     String description, String xsl, boolean formatXsl, String langType, boolean cacheable, 
									     boolean smallImage, String smallImageURL, File smallFile, ServiceContext serviceContext) throws PortalException, SystemException
	{
		JournalTemplate template = null;
		
		try
		{
			if (GlobalJournalTemplateMgr.isGlobalTemplate(templateId))
				checkVelocitySyntax(xsl);
			
			if (processAsGlobal(templateId))
			{
				template = updateGlobalTemplate(groupId, templateId, structureId, name, 
											description, xsl, formatXsl, langType, cacheable, 
											smallImage, smallImageURL, smallFile, serviceContext);
	
			}
			else
			{
				template = JournalTemplateLocalServiceUtil.updateTemplateExtra(groupId, templateId, structureId, name, 
						 										description, xsl, formatXsl, langType, cacheable, 
						 										smallImage, smallImageURL, smallFile, serviceContext);
			}
			
			LiveLocalServiceUtil.updatePublicationDate(IterKeys.CLASSNAME_JOURNALTEMPLATE, groupId);		
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (Throwable th)
		{
			// Las excepciones que NO sean PortalException ni SystemException, o los errores, se lanzan como SystemException
			throw new SystemException(th);
		}
		return template;
	}
	
	private static JournalTemplate updateGlobalTemplate(long groupId, String templateId, String structureId, String name, 
													   String description, String xsl, boolean formatXsl, String langType, boolean cacheable, 
													   boolean smallImage, String smallImageURL, File smallFile, ServiceContext serviceContext) throws Exception
	{
		// Se crea una instancia de la clase encargada de la actualización
		JournalTemplateFunction templateFunction = new JournalTemplateUpdate(groupId, templateId, structureId, name, 
																			 description, xsl, formatXsl, langType, cacheable, 
																			 smallImage, smallImageURL, smallFile, serviceContext);
		
		return templateFunction.modifyGlobalTemplate();
	}
}
