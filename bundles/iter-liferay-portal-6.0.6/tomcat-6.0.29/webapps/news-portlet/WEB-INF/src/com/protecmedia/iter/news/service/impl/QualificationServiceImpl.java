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

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.base.QualificationServiceBaseImpl;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class QualificationServiceImpl extends QualificationServiceBaseImpl {

	/*
	 *
	 * Add functions
	 *
	 */
	public Qualification addQualification(long groupId, String qualificationName) throws Exception {
		try{
			return qualificationLocalService.addQualification(groupId, qualificationName);	
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_QUALIFICATION_ZYX, ex));
		}
	}
	
	/*
	 * 
	 * Delete functions
	 * 
	 */
	public void removeQualification(long qualificationId) throws Exception {
		try{
			qualificationLocalService.deleteQualification(qualificationId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_REMOVE_QUALIFICATION_ZYX, ex));
		}
	}
	
	public void removeQualification(long groupId, String qualificationName) throws Exception {
		try {
			qualificationLocalService.deleteQualification(groupId, qualificationName);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_REMOVE_QUALIFICATION_ZYX, ex));
		}
	}
	
	/*
	 * 
	 * Update functions
	 * 
	 */
	public Qualification updateQualification(long qualificationId, String qualificationName) throws Exception {
		try{
			return qualificationLocalService.updateQualification(qualificationId, qualificationName);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_QUALIFICATION_ZYX, ex));
		}
	}
	
	/*
	 * 
	 *  Get functions 
	 * 
	 */
	public Qualification getQualification(long qualificationId) throws Exception {		
		try{
			return qualificationLocalService.getQualification(qualificationId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_QUALIFICATION_ZYX, ex));
		}
	}

	public Qualification getQualification(long groupId, String qualificationName) throws Exception {		
		try{
			return qualificationLocalService.getQualification(groupId, qualificationName);				
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_QUALIFICATION_ZYX, ex));
		}
	}

	public List<Qualification> getQualifications(long groupId) throws Exception {
		try{
			return qualificationLocalService.getQualifications(groupId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_QUALIFICATIONS_ZYX, ex));
		}
	}
}
