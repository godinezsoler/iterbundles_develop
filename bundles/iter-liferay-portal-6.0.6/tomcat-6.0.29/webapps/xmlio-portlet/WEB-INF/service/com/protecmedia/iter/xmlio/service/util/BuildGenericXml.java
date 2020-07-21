package com.protecmedia.iter.xmlio.service.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class BuildGenericXml {
	

	
	public static void addParamsMapToDocument(Document doc, Element itemNode, Map<String,String> mapParams){
		
		for(int a=0;a<mapParams.size();a++){
			Set<String> keys = mapParams.keySet();
			Object[] objectsArray = keys.toArray();
			String paramName = (String) objectsArray[a];
			String paramValue = mapParams.get(paramName);
			Element paramNode = doc.createElement("param");
			paramNode.setAttribute("name", paramName);
			Text value = doc.createTextNode(paramValue);
			paramNode.appendChild(value);
			itemNode.appendChild(paramNode);
		}
		
	}
	
	public static void addHeaderMapToDocument(Document doc, Element itemNode, Map<String,String> mapHeader){
		
		for(int a=0;a<mapHeader.size();a++){
			Set<String> keys = mapHeader.keySet();
			Object[] objectsArray = keys.toArray();
			String paramName = (String) objectsArray[a];
			String paramValue = mapHeader.get(paramName);
			itemNode.setAttribute(paramName, paramValue);
		}
		
	}
	
	
	public static void insertListItems(Document doc, Element list,List<Item> itemsList){
		
		for(int a=0;a<itemsList.size();a++){
			Item item = itemsList.get(a);
			Element itemNode = doc.createElement("item");
			addHeaderMapToDocument(doc,itemNode,item.getHeader());
			addParamsMapToDocument(doc,itemNode,item.getParams());
			list.appendChild(itemNode);
		
		}
	}
	
	
	public static Document buildFile(List<List<Item>> multipleList) throws ParserConfigurationException{
		
		 //new Document 
		 DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
         DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
         Document doc = docBuilder.newDocument();
         
         //Add root element
         //(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())));
         Element root = doc.createElement("iter");
         root.setAttribute("version", "1.606.1");
         String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
         root.setAttribute("generated", date);

         doc.appendChild(root);
         
		 for(int a=0;a<multipleList.size();a++){
	        Element list = doc.createElement("list");
	        insertListItems(doc,list,multipleList.get(a));
	        root.appendChild(list);
		 }
			
		return doc;
	}
	
	

}
