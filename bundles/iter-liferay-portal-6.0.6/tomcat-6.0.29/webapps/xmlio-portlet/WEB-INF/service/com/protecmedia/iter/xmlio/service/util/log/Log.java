package com.protecmedia.iter.xmlio.service.util.log;

import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Branch;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;


public class Log 
{
	private static com.liferay.portal.kernel.log.Log _log = LogFactoryUtil.getLog(Log.class);
	
	private String type;
	
	private String message;
	
	private String itemId;
	
	private String operation;
	
	private String messageType;
	
	private String groupId;
	
	private Element _item = null;
	
	

	public Log() 
	{
	}
	public Log(Element item, String type, String message, String itemId, String operation, String messageType, String groupName) 
	{
		super();
		init(item, type, message, itemId, operation, messageType, groupName);
		
	}
	public Log(String type, String message, String itemId, String operation, String messageType, String groupName)
	{
		super();
		init(null, type, message, itemId, operation, messageType, groupName);
	}
	
	private void init(Element item, String type, String message, String itemId, String operation, String messageType, String groupName)
	{
		try
		{
			//Group group = GroupLocalServiceUtil.getGroup(groupId);
			this._item			= item;
			this.type 			= type;
			this.message 		= message;
			this.itemId 		= itemId;
			this.operation 		= operation;
			this.messageType 	= messageType;
			this.groupId 		= groupName;		
		}
		catch(Exception e)
		{
			_log.error("Error al general el log de XMLIO", e);
		}
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the itemId
	 */
	public String getItemId() {
		return itemId;
	}

	/**
	 * @param itemId the itemId to set
	 */
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	/**
	 * @return the operation
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}		
	
	/**
	 * @return the messageType
	 */
	public String getMessageType() {
		return messageType;
	}

	/**
	 * @param messageType the messageType to set
	 */
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public Element appendToDom(Document dom)
	{
		return appendToDom(dom, null);
	}
	public Element appendToDom(Document dom, Node parentNode)
	{
		Element eleItem = null;
		try
		{
			if (parentNode != null && parentNode instanceof Branch)
				eleItem = ((Branch) parentNode).addElement("item");
			else
				eleItem = dom.addElement("item");
			
			if (_item != null)
			{
				String id_ = _item.attributeValue("id_");
				
				if (id_ != null && !id_.isEmpty())
					eleItem.addAttribute("id_", id_);
			}
			
			eleItem.addAttribute("operation", 	operation);
			eleItem.addAttribute("globalId", 	itemId);
			eleItem.addAttribute("groupId", 	groupId);
			eleItem.addAttribute("classname", 	type);
			eleItem.addAttribute("status", 		messageType);
			
			eleItem.addCDATA(message);
		}
		catch(Exception e)
		{
			_log.error("Error while trying to parse the log", e);
		}
		return eleItem;
	}
	
	public Document toXML()
	{
		Document dom = null;
		
		try
		{
			//create an instance of DOM
			dom = SAXReaderUtil.createDocument();
			
			appendToDom(dom);
		}
		catch(Exception e)
		{
			_log.error("Error while trying to parse the log", e);
		}

		return dom;
	}
	
	public String toXMLString()
	{
		String strValue = "<item/>";
		try
		{
			Document dom = toXML();
			
			if (dom != null)
				strValue = dom.asXML();
		}
		catch(Exception e)
		{
			_log.error("Error while trying to parse the log", e);
		}
		
		return strValue;
	}
	
	@Override
	public String toString() {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append(messageType.toUpperCase());
		sb.append(": ");
		sb.append(message);
		sb.append(",");
		sb.append(" ItemId: "); 
		sb.append(itemId);
		sb.append(",");
		sb.append(" Type: "); 
		sb.append(type);
		sb.append(",");
		sb.append(" Operation: "); 
		sb.append(operation);		
		
		return sb.toString();
	}
	
}
