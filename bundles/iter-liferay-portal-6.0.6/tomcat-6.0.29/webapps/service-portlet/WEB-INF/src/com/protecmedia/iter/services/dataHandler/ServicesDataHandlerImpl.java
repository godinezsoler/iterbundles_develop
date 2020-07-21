/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.dataHandler;

import java.util.List;

import javax.portlet.PortletPreferences;

import com.liferay.portal.kernel.lar.BasePortletDataHandler;
import com.liferay.portal.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.lar.PortletDataException;
import com.liferay.portal.kernel.lar.PortletDataHandlerBoolean;
import com.liferay.portal.kernel.lar.PortletDataHandlerControl;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.service.persistence.JournalStructureUtil;
import com.protecmedia.iter.services.model.Service;
import com.protecmedia.iter.services.service.persistence.ServiceUtil;

public class ServicesDataHandlerImpl extends BasePortletDataHandler {

	public String exportData(PortletDataContext context, String portletId,
			javax.portlet.PortletPreferences preferences)
			throws PortletDataException {
		// TODO Auto-generated method stub
		System.out.println("export");
		return super.exportData(context, portletId, preferences);
	}
	
	public javax.portlet.PortletPreferences importData(
			PortletDataContext context, String portletId,
			javax.portlet.PortletPreferences preferences, String data)
			throws PortletDataException {
		// TODO Auto-generated method stub
		System.out.println("import");
		return super.importData(context, portletId, preferences, data);
	}
	
	public PortletDataHandlerControl[] getExportControls()
			throws PortletDataException {
		return new PortletDataHandlerControl[] {_services};
	}
	
	public PortletDataHandlerControl[] getImportControls()
			throws PortletDataException {
		return new PortletDataHandlerControl[] {_services};
	}	
	
	protected String doExportData(PortletDataContext context, String portletId,
			PortletPreferences preferences) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("doex");
		
		context.addPermissions(
				"com.protecmedia.iter.services", context.getScopeGroupId());

		Document document = SAXReaderUtil.createDocument();

		Element rootElement = document.addElement("services-data");
		
		rootElement.addAttribute(
				"group-id", String.valueOf(context.getScopeGroupId()));
		
		List<Service> services = ServiceUtil.findByGroupId(context.getScopeGroupId());
		
		for (Service service : services) {
			Element serviceElement = rootElement.addElement("service");
			serviceElement.addAttribute("serviceId", String.valueOf(service.getPrimaryKey()));
		}
		
		return document.formattedString();
		//return super.doExportData(context, portletId, preferences);
	}
	
	protected PortletPreferences doImportData(PortletDataContext context,
			String portletId, PortletPreferences preferences, String data)
			throws Exception {
		
		context.importPermissions(
				"com.liferay.portlet.journal", context.getSourceGroupId(),
				context.getScopeGroupId());
		
		System.out.println("doim");
		
		Document document = SAXReaderUtil.read(data);
		
		return null;//super.doImportData(context, portletId, preferences, data);
	}
		
	////////VARIABLES Y CONSTANTES
	public boolean isAlwaysExportable() {
		return _ALWAYS_EXPORTABLE;
	}
	
	public boolean isPublishToLiveByDefault() {
		return _DEFAULT_EXPORT;
	}
	
	private static final boolean _ALWAYS_EXPORTABLE = true;
	
	private static final boolean _DEFAULT_EXPORT = true;
	
	private static final String _NAMESPACE = "services";
	
	private static Log _log = LogFactoryUtil.getLog(
			ServicesDataHandlerImpl.class);
	
	private static PortletDataHandlerBoolean _services =
		new PortletDataHandlerBoolean(_NAMESPACE, "services", true);		
	
}
