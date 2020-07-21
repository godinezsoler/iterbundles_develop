package com.protecmedia.portal.service;

import java.util.Date;
import java.util.List;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.service.JournalStructureLocalService;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceWrapper;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalStructureXmlIO;

public class MyJournalStructureLocalService extends JournalStructureLocalServiceWrapper{
	
	private static Log _log = LogFactoryUtil.getLog(MyJournalStructureLocalService.class);
	private static ItemXmlIO itemXmlIO = new JournalStructureXmlIO();
	
	public MyJournalStructureLocalService(
			JournalStructureLocalService journalStructureLocalService) {
		super(journalStructureLocalService);

	}
	

	/*
	 * Add Functions 
	 * --------------
	 */	
	
	//Este es el que se ejecuta
	@Override
	public JournalStructure addStructure(long userId, long groupId, String structureId, boolean autoStructureId, 
		    String parentStructureId, String name, String description, String xsd, 
		    ServiceContext serviceContext) throws PortalException, SystemException {
		
		JournalStructure structure = super.addStructure(userId, groupId, structureId, autoStructureId, parentStructureId, 
						name, description, xsd, serviceContext);		
		
		//Add to Live
		itemXmlIO.createLiveEntry(structure);	
						
		return structure;
	}
	
	@Override
	public JournalStructure addJournalStructure(JournalStructure journalStructure){
		
		JournalStructure structure = null;
		try {
		
			structure = super.addJournalStructure(journalStructure);
			
			//Add to Live
			itemXmlIO.createLiveEntry(structure);	
			
		} catch (Exception e) {			
			_log.error("Live Error", e);
		}
		
		return structure;
	}
	

	/*
	 * Update Functions
	 */
	
	//Lo ejecuta: INTERFAZ GRAFICA y MILENIUM
	@Override
	public JournalStructure updateStructure(long groupId, String structureId, String parentStructureId, String name, String description, String xsd, ServiceContext serviceContext){
		
		JournalStructure structure = null;
		try {
			structure = super.updateStructure(groupId, structureId, parentStructureId, name, description, xsd, serviceContext);
			
			//Add to Live
			itemXmlIO.createLiveEntry(structure);	
			
		} catch (Exception e) {			
			_log.error("Live Error", e);
		}
		return structure;
	}
	
	
	/*
	 * Delete Functions
	 */
	
	@Override
	public void deleteJournalStructure(long id){
		
		try {
			JournalStructure structure = JournalStructureLocalServiceUtil.getJournalStructure(id);
						
			//Delete from Live
			itemXmlIO.deleteLiveEntry(structure);
			
			super.deleteJournalStructure(id);			
			
		} catch (Exception e) {			
			_log.error("Live Error", e);
		}
	}
	
	
	@Override
	public void deleteJournalStructure(JournalStructure structure){
		
		try {	
			//Delete from Live
			itemXmlIO.deleteLiveEntry(structure);
			
			super.deleteJournalStructure(structure);
			
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
	}
	
	
	@Override
	public void deleteStructure(JournalStructure structure){
		
		try {
			//Delete from Live
			itemXmlIO.deleteLiveEntry(structure);
		
			super.deleteJournalStructure(structure);
		
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
	}
	
	//Este es el que se ejecuta
	@Override
	public void deleteStructure(long groupId, String structureId){
		
		try {	
			JournalStructure structure = JournalStructureLocalServiceUtil.getStructure(groupId, structureId);
			
			//Delete from Live
			itemXmlIO.deleteLiveEntry(structure);
			
			super.deleteStructure(groupId, structureId);

		} catch (Exception e) {			
			_log.error("Live Error", e);
		}
	}

	public void deleteStructures(long groupId){
		
		try {
			List<JournalStructure> structuresList = JournalStructureLocalServiceUtil.getStructures(groupId);
			
			for(int a=0;a<structuresList.size();a++){
				
				JournalStructure structure = structuresList.get(a);
				
				//Delete from Live
				itemXmlIO.deleteLiveEntry(structure);
				
				super.deleteStructure(structure);		
			}
			
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
		
	}
		
}
