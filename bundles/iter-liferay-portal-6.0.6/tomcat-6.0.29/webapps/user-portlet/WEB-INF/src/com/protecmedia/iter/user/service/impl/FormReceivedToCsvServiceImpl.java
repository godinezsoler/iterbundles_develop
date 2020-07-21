package com.protecmedia.iter.user.service.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.protecmedia.iter.user.service.base.FormReceivedToCsvServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FormReceivedToCsvServiceImpl extends FormReceivedToCsvServiceBaseImpl {
	
	
	public void generateCSV(HttpServletRequest request, HttpServletResponse response, String formTypeId, String dateBefore, String dateAfter) throws Exception{
		formReceivedToCsvLocalService.generateCSV(request, response, formTypeId, dateBefore, dateAfter);
	}
	
	public void generateCSVTranslated(HttpServletRequest request, HttpServletResponse response, String formTypeId, String dateBefore, String dateAfter, String nameForDateSentColumn, String nameForUserColum) throws Exception{
		formReceivedToCsvLocalService.generateCSVTranslated(request, response, formTypeId, dateBefore, dateAfter, nameForDateSentColumn, nameForUserColum);
	}
	
}