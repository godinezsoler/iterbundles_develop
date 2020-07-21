package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.service.JournalStructureServiceWrapper;
import com.liferay.portlet.journal.service.JournalStructureService;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyJournalStructureService extends JournalStructureServiceWrapper {
	/* (non-Java-doc)
	 * @see com.liferay.portlet.journal.service.JournalStructureServiceWrapper#JournalStructureServiceWrapper(JournalStructureService journalStructureService)
	 */
	public MyJournalStructureService(JournalStructureService journalStructureService) {
		super(journalStructureService);
	}

	@Override
	public JournalStructure updateStructure(
			long groupId, String structureId, String parentStructureId, String name,
			String description, String xsd, ServiceContext serviceContext) throws PortalException, SystemException{
		try{
			return super.updateStructure(groupId, structureId, parentStructureId, name, description, xsd, serviceContext);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_STRUCTURE_ZYX, ex));
		}
	}
	
	@Override 
	public JournalStructure getStructure(long groupId, String structureId) throws PortalException, SystemException{
		try{
			return super.getStructure(groupId, structureId);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_STRUCTURE_ZYX, ex));
		}
	}
}