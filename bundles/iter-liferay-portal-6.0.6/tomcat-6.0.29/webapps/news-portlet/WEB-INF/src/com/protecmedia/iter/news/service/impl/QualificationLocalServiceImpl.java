/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.news.service.impl;

import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.QualificationTools;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.news.DuplicateQualificationException;
import com.protecmedia.iter.news.NoSuchQualificationException;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;
import com.protecmedia.iter.news.service.base.QualificationLocalServiceBaseImpl;
import com.protecmedia.iter.news.service.item.QualificationXmlIO;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class QualificationLocalServiceImpl
	extends QualificationLocalServiceBaseImpl {

	private static Log _log = LogFactoryUtil.getLog(QualificationLocalServiceImpl.class);
	private static ItemXmlIO itemXmlIO = new QualificationXmlIO();
	/*
	 * 
	 * Add functions
	 * 
	 */
	//Utilizada por MILENIUM
	public Qualification addQualification(long groupId, String qualificationName) throws Exception  
	{				
		
		return addQualification(groupId, qualificationName, "", true);		
	}
	
	public Qualification addQualification(long groupId, String qualificationName, String qualificationId, boolean refreshMap) throws Exception  
	{				
		long id = counterLocalService.increment();
		
		Qualification qualification = qualificationLocalService.createQualification(id);		
		
		//qualificationId es != "" en publicación. De esta forma se mantiene que el qualifId en live sea el mismo que el del back
		if (qualificationId.equals(""))
		{
			qualificationId = String.valueOf(qualification.getId());
		}
		
		
		qualification.setName(qualificationName);
		qualification.setGroupId(groupId);
		qualification.setModifiedDate(new Date());
		qualification.setQualifId(qualificationId);
				
		qualificationPersistence.update(qualification, false);
		
		//insercion en la tabla de LIVE
		itemXmlIO.createLiveEntry(qualification);
		
		if (refreshMap)
			QualificationTools.addQualificationMap( qualificationId, qualificationName );
	
		
		return qualification;
		
	}
	
	/*
	 * 
	 * Update funtions
	 * 
	 */
	
	//Utilizada por MILENIUM
	public Qualification updateQualification(long qualificationId, String name) throws Exception 
	{
		return updateQualification( qualificationId, name, true );
	}
	
	public Qualification updateQualification(long qualificationId, String name, boolean refreshMap ) throws Exception 
	{
		
		Qualification qualification = qualificationPersistence.fetchByPrimaryKey(qualificationId);
		
		if (qualification != null)
		{
			qualification.setName( name );
			qualification.setModifiedDate(new Date());
		
			qualificationPersistence.update(qualification, false);
			
			//insercion en la tabla de LIVE
			itemXmlIO.createLiveEntry(qualification);
		}
		
		if (refreshMap)
			QualificationTools.updateQualificationMap( Long.toString(qualificationId), name  );

		
		return qualification;
		
	}
	
	/*
	 * 
	 * Get Qualifications
	 * 
	 */
	public Qualification getQualification(long qualificationId) throws SystemException {
		Qualification qualification = qualificationPersistence.fetchByPrimaryKey(qualificationId);
		
		return qualification;
	}
	
	
	
	public Qualification getQualification(long groupId, String qualificationName) throws SystemException {
		Qualification qualification = qualificationPersistence.fetchByNameFinder(groupId, qualificationName);
		
		return qualification;				
	}
	 
	public Qualification getQualificationByQualifId(long groupId, String qualificationId) throws SystemException {
		Qualification qualification = qualificationPersistence.fetchByQualifIdFinder(groupId, qualificationId);
		
		return qualification;
	}
	
	public Qualification getQualificationByUuid(long groupId, String uuid) throws SystemException {
		Qualification qualification = qualificationPersistence.fetchByUUID_G(uuid, groupId);
		
		return qualification;
	}
	
	public List<Qualification> getQualifications(long groupId) throws SystemException {
		return qualificationPersistence.findByGroupFinder(groupId);
	}
	
	
	/*
	 * 
	 * Delete qualifications
	 * 
	 */	
	public void deleteQualification(long groupId, String qualificationId) throws PortalException, SystemException {		
		
		//insercion en la tabla de LIVE
		Qualification qualification = QualificationLocalServiceUtil.getQualification(groupId, qualificationId);
		
		if (qualification != null){
			itemXmlIO.deleteLiveEntry(qualification);
			
			qualificationPersistence.removeByQualifIdFinder(groupId, qualificationId);
		}else{
			throw new NoSuchQualificationException();
		}
	}
	
	//Utilizada por MILENIUM
	public void deleteQualification(long qualificationId) throws Exception
	{
		deleteQualification(qualificationId, true);
	}
	public void deleteQualification(long qualificationId, boolean refreshMap) throws Exception 
	{		
		
		try{
			//insercion en la tabla de LIVE
			Qualification qualification = QualificationLocalServiceUtil.getQualification(qualificationId);
			itemXmlIO.deleteLiveEntry(qualification);
			qualificationPersistence.remove(qualificationId);
			
			if (refreshMap)
				QualificationTools.deleteQualificationMap( Long.toString(qualificationId) );
		}
		catch(Exception e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_NO_SUCH_QUALIFICATION_ZYX, "NO QUALIFICATION EXISTS WITH THE PRIMARY KEY "+String.valueOf(qualificationId));
		}
	}
	
	public void deleteQualifications(long groupId) throws SystemException {
		qualificationPersistence.removeByGroupFinder(groupId);
	}
	
}
