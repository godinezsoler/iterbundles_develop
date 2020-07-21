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

package com.protecmedia.iter.services.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.service.item.PageTemplateXmlIO;
import com.protecmedia.iter.services.service.base.ServiceLocalServiceBaseImpl;
import com.protecmedia.iter.services.service.item.ServiceXmlIO;
import com.protecmedia.iter.services.DuplicateServiceException;
import com.protecmedia.iter.services.NoSuchServiceException;
import com.protecmedia.iter.services.model.Service;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ServiceLocalServiceImpl extends ServiceLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(ServiceLocalServiceImpl.class);
	private static ItemXmlIO itemXmlIO = new ServiceXmlIO();
	
	/*
	 * GET FUNCTIONS
	 */
	public Service getServiceByPrimaryKey(long primaryKey) throws SystemException, NoSuchServiceException {
		return servicePersistence.findByPrimaryKey(primaryKey);
	}
	
	public Service getServiceByServiceId(long groupId, String serviceId) throws SystemException{
		return servicePersistence.fetchByServiceId(groupId, serviceId);
	}
	
	public List<Service> getServices(long groupId, int start, int end) throws SystemException {
		return servicePersistence.findByGroupId(groupId, start, end);
	}
	
	public int getServicesCount(long groupId) throws SystemException {
		return servicePersistence.countByGroupId(groupId);
	}		
	
	public List<Service> getServices(long groupId) throws SystemException {
		return servicePersistence.findByGroupId(groupId);
	}	
	
	/*
	 * ADD FUNCTIONS
	 */
	public Service addService(long groupId, String serviceId, String name, long link, File imageFile ) throws SystemException, IOException, DuplicateServiceException {
				
		Service service = null;
		try{
			validate(groupId, name);
		
			long id = counterLocalService.increment();
			
			service = servicePersistence.create(id);
			
			if (serviceId!= null && !serviceId.equals("")){
				service.setServiceId(serviceId);
			}else{
				service.setServiceId(String.valueOf(id));
			}
			
			service.setTitle(name);
			service.setLinkId(link);			
			service.setGroupId(groupId);
			
			long imageId = CounterLocalServiceUtil.increment();
			service.setImageId(imageId);
			
			byte[] imageBytes = null;
			imageBytes = FileUtil.getBytes(imageFile);
			ImageLocalServiceUtil.updateImage(service.getImageId(), imageBytes);
			
			servicePersistence.update(service, false);
			
			try{
				itemXmlIO.createLiveEntry(service);
			}catch(Exception e){
				_log.error("Add service to live: " + e.toString());
			}
			
		}catch(Exception e){
			_log.error("Add service: " + e.toString());
		}
		
		return service;
		
	}
	
	public Service addService(long groupId, String serviceId, String name, long link, long imageId ) throws SystemException, IOException, DuplicateServiceException {
		
		Service service = null;
		try{
			validate(groupId, name);
		
			long id = counterLocalService.increment();
			
			service = servicePersistence.create(id);
			
			if (serviceId!= null && !serviceId.equals("")){
				service.setServiceId(serviceId);
			}else{
				service.setServiceId(String.valueOf(id));
			}
			
			service.setTitle(name);
			service.setLinkId(link);			
			service.setGroupId(groupId);
			service.setImageId(imageId);
			
			servicePersistence.update(service, false);
			
			try{
				itemXmlIO.createLiveEntry(service);
			}catch(Exception e){
				_log.error("Add service to live: " + e.toString());
			}
			
		}catch(Exception e){
			_log.error("Add service: " + e.toString());
		}
		
		return service;
		
	}
	
	private void validate(long groupId, String name) throws SystemException, DuplicateServiceException {
		
		Service service = servicePersistence.fetchByName(groupId, name);
		
		if (service != null) {
			_log.error("Duplicate service");
			throw new DuplicateServiceException();
		}
	}

	/*
	 * UPDATE Functions
	 */
	
	public Service updateService(Service service) throws SystemException {
		
		Service s = null;
		
		try{
			s = super.updateService(service);
			//Insercion en la tabla de Live
			try {
				itemXmlIO.createLiveEntry(s);
			} catch (PortalException e) {
				_log.error("Update service in live: " + e.toString());
			}
		}catch (Exception e) {
			_log.error("Update service: " + e.toString());
		}
		
		return s;
	}
	
	public Service updateService( Service service, long groupId, String name, long link, long imageId, File imageFile ) throws IOException, Exception {
		
		Service s = null;
		
		try{
			if(Validator.isNull(name)|| name.equals("")){
				_log.error("Service`s name is null");
				throw new Exception();
			}
			
			service.setTitle(name);
			service.setLinkId(link);
			service.setImageId(imageId);
			
			if (imageFile != null) {			
				
				byte[] imageBytes = FileUtil.getBytes(imageFile);

				if (imageBytes != null) {
					ImageLocalServiceUtil.updateImage(imageId, imageBytes);
				}
			}
			
			s = servicePersistence.update(service, true);
			
			//Insercion en la tabla de Live
			try {
				itemXmlIO.createLiveEntry(s);
			} catch (PortalException e) {
				_log.error("Update service in live: " + e.toString());
			}
		}catch (Exception e) {
			_log.error("Update service: " + e.toString());
		}
		
		return s;
			
	}
	
	public Service updateService( Service service, long groupId, String name, long link, long imageId ) throws IOException, Exception {
		
		Service s = null;
		
		try{
			if(Validator.isNull(name)|| name.equals("")){
				_log.error("Service`s name is null");
				throw new Exception();
			}
			
			service.setTitle(name);
			service.setLinkId(link);
			service.setImageId(imageId);
			
			s = servicePersistence.update(service, true);
			
			//Insercion en la tabla de Live
			try {
				itemXmlIO.createLiveEntry(s);
			} catch (PortalException e) {
				_log.error("Update service in live: " + e.toString());
			}
		}catch (Exception e) {
			_log.error("Update service: " + e.toString());
		}
		
		return s;
			
	}
	
	/*
	 * DELETE Functions
	 */
	
	public void deleteService(long serviceId) throws PortalException, SystemException {
		
		try{
			Service service = getService(serviceId);
		
			deleteService(service);
		}catch(Exception e){
			_log.error("Delete service: " + e.toString());
		}
	}
	
	public void deleteServiceByLinkId(long groupId, long linkId){
		try{
			List<Service> sList = servicePersistence.findByLinkId(groupId, linkId);
			for(Service s : sList){
				deleteService(s);
			}
		}
		catch(Exception e){
			_log.error("Delete service: " + e.toString());
		}
	}
	
	public void deleteService(Service service) throws SystemException {
		
		try{
			if (service != null) {
				super.deleteService(service);
				//Insercion en la tabla de Live
				try {
					itemXmlIO.deleteLiveEntry(service);
				} catch (PortalException e) {
					_log.error("Delete service from live: " + e.toString());
				}
			}
		}catch(Exception e){
			_log.error("Delete service: " + e.toString());
		}
	}
}
