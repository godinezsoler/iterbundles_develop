package com.protecmedia.iter.xmlio.service.item.portal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.EmailAddress;
import com.liferay.portal.model.Group;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class EmailAddressXmlIO extends ItemXmlIO{

	private static Log _log = LogFactoryUtil.getLog(EmailAddressXmlIO.class);
	private String _className = IterKeys.CLASSNAME_EMAILADDRESS;

	public EmailAddressXmlIO() {
		super();
	}
	
	public EmailAddressXmlIO(XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}
	
	@Override 
	public String getClassName(){
		return _className;
	}
	
	/*
	 * Live Functions
	 */
	@Override
	public void populateLive(long groupId, long companyId)
			throws SystemException, PortalException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		EmailAddress emailAddress = (EmailAddress)model;
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		EmailAddress emailAddress = (EmailAddress)model;
	}

	/*
	 * Export functions
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
			
		//Put necessary parameters for each kind of operation.	
		
		return error;
	}	

	/*
	 * Import Functions
	 */	
	@Override
	protected void delete(Element item) {
	}	
	
	@Override
	protected void modify(Element item, Document doc) {
	}
	
	private void create(){
	}
	
	private void update(){
	}
}
