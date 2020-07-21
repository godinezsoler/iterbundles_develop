package com.protecmedia.portal.util;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

public class JournalTemplateDelete extends JournalTemplateFunction
{
	public JournalTemplateDelete(long groupId, String templateId)
	{
		super(groupId, templateId);
	}
	
	@Override
	protected String getFunctionName()
	{
		return "DELETE";
	}

	@Override
	protected JournalTemplate doCall() throws PortalException, SystemException
	{
		JournalTemplateLocalServiceUtil.deleteTemplateExtra(_groupId, _templateId);
		
		return null;
	}
	
	public static void invoke(long groupId, String templateId) throws PortalException, SystemException
	{
		try
		{
			if (processAsGlobal(templateId))
			{
				deleteGlobalTemplate(groupId, templateId);
			}
			else
			{
				JournalTemplateLocalServiceUtil.deleteTemplateExtra(groupId, templateId);
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
	}

	private static void deleteGlobalTemplate(long groupId, String templateId) throws Exception
	{
		// Se crea una instancia de la clase encargada de la actualización
		JournalTemplateFunction templateFunction = new JournalTemplateDelete(groupId, templateId);
		
		templateFunction.modifyGlobalTemplate();
	}
}
