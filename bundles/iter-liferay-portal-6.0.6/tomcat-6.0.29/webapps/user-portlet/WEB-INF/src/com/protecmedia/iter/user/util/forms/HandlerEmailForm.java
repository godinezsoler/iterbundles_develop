package com.protecmedia.iter.user.util.forms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ContextVariables;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.MailUtil;
import com.protecmedia.iter.base.service.util.MimeTypeTools;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil;

@SuppressWarnings("unused")
public class HandlerEmailForm  extends Thread{

	private static Log _log = LogFactoryUtil.getLog(HandlerEmailForm .class);

	private static final String MAIL_NOT_SEND = "notsend";
	private static final String MAIL_ATTACHED = "attached";
	private static final String MAIL_BODY = "";
	final static String XPATH_BINLOCATOR = "/formdata/fieldsgroup/field/data/binary/binlocator[starts-with(text(),'%s')]";
	
	private boolean processdata;
	private Document xmlDom;
	private String xslid;
	
	private HttpServletRequest request;
	private String formname;
	private String type;
	private String para;
	private String cc;
	private String cco;
	private String smtpserver; 
	private String textsubject;
	private String textbody;
	private String formReceivedId;
	private List<Throwable> exceptionsHandlers;
	private Semaphore semaphore;
	private Map<String, ArrayList> attachments;
	private long groupId;
	
	private String GET_SMTPSERVER = new StringBuffer().append("SELECT s.* FROM smtpserver s WHERE s.smtpserverid = '%s'").toString();

	/**
	 * @param processdata 
	 * @param xmlDom
	 * @param attachments
	 * @param xslid
	 * @param request
	 * @param formname
	 * @param type
	 * @param para
	 * @param cc
	 * @param cco
	 * @param smtpserver
	 * @param textsubject
	 * @param textbody
	 * @param formReceivedId
	 * @param exceptionsHandlers
	 * @param semaphore
	 * @throws DocumentException 
	 */
	public HandlerEmailForm(long groupId, boolean processdata, Document xmlDom, Map<String, ArrayList> attachments, String xslid, HttpServletRequest request, String formname,
			String type, String para, String cc,	String cco, String smtpserver, String textsubject,	String textbody, String formReceivedId,
			List<Throwable> exceptionsHandlers, Semaphore semaphore)  
	{
		this.groupId = groupId;
		this.processdata = processdata;
		this.formname = formname;

		this.xmlDom = SAXReaderUtil.createDocument();
		this.xmlDom.setRootElement(xmlDom.getRootElement().createCopy()); 
		
		this.xslid = xslid;              
		this.request = request;
		this.type = type;               
		this.para= para;               
		this.cc = cc;                 
		this.cco = cco;                
		this.smtpserver = smtpserver;         
		this.formReceivedId = formReceivedId;
		this.textsubject = replaceVarsForm(textsubject, xmlDom);        
		this.textbody = replaceVarsForm(textbody, xmlDom);         
		this.exceptionsHandlers = exceptionsHandlers;
		this.semaphore = semaphore;
		if(attachments == null)
		{
			this.attachments = new HashMap<String, ArrayList>();
		}
		else
		{
			this.attachments = attachments;
		}
	}


	@Override
	public void run() 
	{
		super.run();
		
		try 
		{
			localRun();
		}
		catch (Throwable th) 
		{
			IterMonitor.logEvent(this.groupId, Event.ERROR, new Date(System.currentTimeMillis()), "Form handler email error", "", th);
			_log.error(th);
			if(Validator.isNotNull(exceptionsHandlers))
			{
				try 
				{
					semaphore.acquire();
					exceptionsHandlers.add(th);
					_log.error("throw Exception from thread HandlerEmailForm to FormReceiver (critic HandlerEmailForm)");
					semaphore.release();
				}
				catch (InterruptedException e1) 
				{
				}
			}
			else
				_log.error("no critic HandlerEmailForm");
		}
	}


	/**
	 * @throws ServiceError
	 * @throws PortalException
	 * @throws SystemException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws TransformerException
	 * @throws DocumentException
	 * @throws IOException
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void localRun() throws ServiceError, PortalException, SystemException, NoSuchMethodException, SecurityException, TransformerException, DocumentException, IOException, AddressException, MessagingException {
		Document domSMTPServer = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SMTPSERVER, smtpserver));
		Node SMTPnode = domSMTPServer.selectSingleNode("/rs/row");
		ErrorRaiser.throwIfFalse((GetterUtil.getBoolean(XMLHelper.getTextValueOf(SMTPnode, "@enabled", "false"))));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(para) || Validator.isNotNull(cc) || Validator.isNotNull(cco));
		if(Validator.equals(type,MAIL_NOT_SEND)){
			_log.trace(new StringBuffer().append("type Send is not SEND ").toString());
//			sendEmailNotification(domSMTPServer);
			MailUtil.sendEmail(SMTPnode, textsubject, textbody,"text/plain", getReceivers(para), getReceivers(cc), getReceivers(cco));
		}else{
			if(processdata) 
			{//processData
				//cambio los binlocators
				List<Node> binarysNodes = SAXReaderUtil.createXPath("/formdata/fieldsgroup/field/data/binary").selectNodes(xmlDom);
				for (Node binaryNode : binarysNodes) {
					Element binaryElement = (Element) binaryNode; 
					String name =  binaryElement.element("name").getText();
					Element binlocatorElement = binaryElement.element("binlocator");
					binlocatorElement.attribute("type").setValue("attachment");
					if(binarysNodes.size() == 1)
						binlocatorElement.setText(name);
					else
						binlocatorElement.setText(new StringBuffer(binlocatorElement.getText()).append("_").append(name).toString());
				}
				
				if(Validator.isNotNull(xslid)) 
				{//XSLT
					_log.trace("Tranform XML with xslt");
					long delegationId = GroupLocalServiceUtil.getGroup(this.groupId).getDelegationId();
					InputStream isXSL = DLFileEntryLocalServiceUtil.getFileAsStreamByUuid(xslid, delegationId);
					ErrorRaiser.throwIfNull(isXSL);
	
					Element dataRoot = SAXReaderUtil.read(isXSL).getRootElement();
					String typeXSLT = XMLHelper.getTextValueOf(dataRoot, "/xsl:stylesheet/xsl:output/@method", null);
					if(Validator.isNull(typeXSLT)){
						_log.trace("XSL not contain 'xsl:output/@method'. default: xml");
						typeXSLT = "xml";
					}

					_log.debug(new StringBuffer().append("XSLT go to generate file ").append(typeXSLT).toString());
					String transformed = XSLUtil.transform(XSLUtil.getSource(xmlDom.asXML()), XSLUtil.getSource(dataRoot.asXML()), typeXSLT);
					sendEMail(SMTPnode, transformed.getBytes(), typeXSLT);
						
				}
				else
				{//XML
					_log.trace("XML send");
//					List<Node> binarysNodes = SAXReaderUtil.createXPath("/formdata/fieldsgroup/field/data/binary").selectNodes(xmlDom);
//					for (Node binaryNode : binarysNodes) {
//						Element binaryElement = (Element) binaryNode; 
//						String name =  binaryElement.attributeValue("name");
//						
//						Element binlocatorElement = binaryElement.element("binlocator");
//						binaryElement.attribute("type").setText("attachment");
//						if(binarysNodes.size() == 1)
//							binlocatorElement.setText(name);
//						else
//							binlocatorElement.setText(new StringBuffer(binlocatorElement.getText()).append(name).toString());
//					}
					sendEMail(SMTPnode, xmlDom.asXML().getBytes(), "xml");
				}
			}
			else
			{ //rawData -> request
				_log.trace("rawData send");
				ErrorRaiser.throwIfNull(request);
				byte[] rawData = IOUtils.toByteArray(request.getInputStream());
				sendEMail(SMTPnode,rawData,"bin");
			}
		}
	}

	/**
	 * @param domSMTPServer
	 * @param body
	 * @param extension
	 * @throws AddressException
	 * @throws MessagingException
	 * @throws ServiceError
	 * @throws IOException
	 */
	private void sendEMail(Node domSMTPServer, byte[] body,	String extension) throws AddressException, MessagingException, ServiceError, IOException {
		if(Validator.equals(type, MAIL_ATTACHED)){
			_log.debug(new StringBuffer().append("sendEmail: attach body with name ").append(formname).append(".").append(extension).toString());
			MailUtil.sendEmail(domSMTPServer, textsubject, textbody, "text/plain; charset=utf-8", getReceivers(para), getReceivers(cc), getReceivers(cco),getEntriesAttachment(body, extension));
		}
		else{
			String MIMEType = new StringBuffer().append(getMimeType(extension)).append("; charset=utf-8").toString();
			_log.trace(new StringBuffer().append("Handler send ").append(MIMEType).append(" email with extension ").append(extension).toString());
		
			if(!attachments.isEmpty())
			{
				_log.debug("sendEmail: with attachments.\n body is : " + new String(body) + " with mime type "+ MIMEType);
				MailUtil.sendEmail(domSMTPServer, textsubject, new String(body), MIMEType, getReceivers(para), getReceivers(cc), getReceivers(cco), getEntriesAttachment(new ArrayList<Map.Entry<String, ArrayList>>(attachments.entrySet())));
			}
			else
			{//Send 
				_log.debug("sendEmail: without attachments.\n body is: " + new String(body)  + " with mime type "+  MIMEType);
				MailUtil.sendEmail(domSMTPServer, textsubject, new String(body), MIMEType, getReceivers(para), getReceivers(cc), getReceivers(cco));
			}
		}
	}
	



	/**
	 * @param receivers
	 * @return
	 */
	private List<String> getReceivers(String receivers){
		List<String> lreceivers = null;
		if(Validator.isNotNull(receivers)){
			lreceivers = Arrays.asList(receivers.split(","));
		}
		return lreceivers;
	}
	
	
	private List<Entry<String, InputStream>> getEntriesAttachment(byte[] body, String extension) throws ServiceError
	
	{
		ArrayList bodyAttachment = new ArrayList();
		bodyAttachment.add(new StringBuffer().append(formname).append(".").append(extension).toString());
		bodyAttachment.add(body);
		
		Map<String, ArrayList> attachBody = new HashMap<String, ArrayList>();
		attachBody.put("body",bodyAttachment);
		
		List<Map.Entry<String, ArrayList>> mailAttachments = new ArrayList<Map.Entry<String, ArrayList>>(attachBody.entrySet());
		mailAttachments.addAll(attachments.entrySet());
		
		return getEntriesAttachment(mailAttachments);
	}
	
	/**
	 * @return
	 * @throws ServiceError 
	 */
	private List<Entry<String, InputStream>> getEntriesAttachment(List<Entry<String, ArrayList>> mailAttachments) throws ServiceError{
		List<Entry<String, InputStream>> result = new ArrayList<Map.Entry<String,InputStream>>();
		if(!mailAttachments.isEmpty())
		{
			for (Entry<String,ArrayList> entry : mailAttachments) 
			{
				Map<String, InputStream> mapAux = new HashMap<String, InputStream>();
				Node binlocator = SAXReaderUtil.createXPath(String.format(XPATH_BINLOCATOR, entry.getKey())).selectSingleNode(xmlDom);
				if(Validator.isNull(binlocator)){
					mapAux.put((String) entry.getValue().get(0), new ByteArrayInputStream((byte[]) entry.getValue().get(1)));
				}else{
					mapAux.put(binlocator.getText(), new ByteArrayInputStream((byte[]) entry.getValue().get(1)));
				}
				result.addAll(mapAux.entrySet());
			}
		}
		return result;
	}
	
	/**
	 * @param text
	 * @param xml
	 * @return
	 */
	public String replaceVarsForm(String text, Document xml){
		_log.trace(new StringBuffer().append("into replaceVarsForm replacing in ").append(text).toString());
		if(Validator.isNull(text)){
			return "";
		}
		Map<String, String> ctxVars = new HashMap<String, String>();
		ctxVars.put("form-name", formname);
		ctxVars.put("delivery-id",formReceivedId);
		
		Element xmlElement = xml.getRootElement();
		ctxVars.put("form-id", xmlElement.attributeValue("formid", ""));
		Node deliveryInfoNode = xmlElement.selectSingleNode("/formdata/delivery-info");
				
		if(Validator.isNotNull(deliveryInfoNode)){
			ctxVars.put("date-sent",deliveryInfoNode.valueOf("date-sent"));
			Node sendingUserNode = deliveryInfoNode.selectSingleNode("sending-user");
			if(Validator.isNotNull(sendingUserNode)){
				ctxVars.put("usrid",sendingUserNode.valueOf("usrid"));
				ctxVars.put("usrname",sendingUserNode.valueOf("usrname"));
				ctxVars.put("usremail",sendingUserNode.valueOf("usremail"));
				ctxVars.put("usr1stname",sendingUserNode.valueOf("usr1stname"));
				ctxVars.put("usrlastname",sendingUserNode.valueOf("usrlastname"));
				ctxVars.put("usrlastname2",sendingUserNode.valueOf("usrlastname2"));
			}
		}
		
		return ContextVariables.replaceCtxVars(text, ctxVars);
	}
	
	/**
	 * @param extension
	 * @return
	 */
	private String getMimeType(String extension){
		if(Validator.equals(extension, "xml")){
			_log.debug("getMimeType from xml tranform to text/plain");
			return "text/plain";
		}
		else
		{
			_log.debug(new StringBuffer("getMimeType MimeTypes from ").append(extension)
					.append(" tranform to ").append(MimeTypeTools.getMimeType(extension))
					.toString());
			return MimeTypeTools.getMimeType(extension);
			
		}
	}
	
}
