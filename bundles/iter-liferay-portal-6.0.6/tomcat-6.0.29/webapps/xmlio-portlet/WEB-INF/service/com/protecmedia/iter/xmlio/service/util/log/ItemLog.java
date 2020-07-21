package com.protecmedia.iter.xmlio.service.util.log;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.IterKeys;

public class ItemLog 
{
	private Document _dom 		= null;
	private Element  _eleLogs 	= null;
	
	private List<Log> log;
	private static com.liferay.portal.kernel.log.Log _log = LogFactoryUtil.getLog(ItemLog.class);
	
	public ItemLog()
	{
		log = new ArrayList<Log>();
		
		_dom = SAXReaderUtil.createDocument();
		Element eleIter = _dom.addElement("iter");
		_eleLogs = eleIter.addElement("logs");
	}
	
	public void addMessage(String itemId, String type, String operation, String message, String messageType, String groupName) 
	{
		addMessage(null, itemId, type, operation, message, messageType, groupName);
	}
	public void addMessage(Element item, String itemId, String type, String operation, String message, String messageType, String groupName) 
	{
		Log l = new Log(item, type, message, itemId, operation, messageType, groupName);
		log.add(l);
		l.appendToDom(_dom, _eleLogs);
	}

	/*
	public Log getLog(int index) 
	{
		return log.get(index);
	}
		
	public String getLogs() 
	{
		StringBuffer sb = new StringBuffer();
		
		for (Log l : log) {		
			sb.append(l.toString());
			sb.append("\n");			
		}
		
		return sb.toString();		
	}
	*/	
	
	/////////////////////////////////////////////////////////////////////////////////////////
	public Document toXML()
	{
		return _dom;
	}
	
	public String getXMLLogs() 
	{
		String strValue = "<iter><logs/></iter>";
		try
		{
			Document dom = toXML();
			strValue = dom.asXML();
		}
		catch(Exception e)
		{
			_log.error("Error while trying to parse the log", e);
		}
		
		return strValue;
	}
	
	/**
	 * 
	 * @param className
	 * @param groupName
	 * @param globalId
	 * @return true si no existen nodos de error para esa clase, grupo y id
	 */
	public boolean validateLog(String className, String groupName, String globalId)
	{
	    // Se recuperan todas las entradas de elementos que tengan status ERROR o DONE (se ignora INFO)
		// Las interrupciones también son un error, y se generan en JournalArticleXmlIO
		String xpath = String.format("count(//item[@classname='%s' and @groupId=%s and @globalId='%s' and (@status='%s' or @status='%s')])", 
				className, StringUtil.escapeXpathQuotes(groupName), globalId, IterKeys.ERROR, IterKeys.INTERRUPT);
		
		return XMLHelper.getLongValueOf(toXML(), xpath) == 0;
	}
	
	public void updateLog(String className, String groupName, String globalId, String status)
	{
		String xpath = String.format("//item[@classname='%s' and @groupId='%s' and @globalId='%s' and @status!='%s']/@status", 
				className, groupName, globalId, status);
		
		List<Node> nodes = toXML().selectNodes(xpath);
		for (Node node : nodes)
		{
			node.setText(status);
		}
	}

}
