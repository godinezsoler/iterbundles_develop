package com.protecmedia.iter.base.service.config;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.ImageResolutionLocalServiceUtil;



public class ImageResolutionMgr{
	
	
	public static final String INSERT_RESOLUTION = "INSERT INTO ImageResolution(resolutionName, width, height) VALUES ('%s', %d, %d)";
	public static final String CHECK_DIMENSIONS = "SELECT resolutionName FROM ImageResolution WHERE resolutionName != \"%s\" AND ";    
	public static final String FETCH_RESOLUTIONS = "SELECT * FROM ImageResolution ORDER BY resolutionName ASC";
	public static final String DELETE_RESOLUTIONS = "DELETE FROM ImageResolution WHERE resolutionName IN (%s)";
	public static final String EDIT_RESOLUTION = "UPDATE ImageResolution SET resolutionName=\"%s\",width=%d,height=%d WHERE resolutionName = \"%s\"";
	
	

	public static String addImgResolution(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException{
		
		addOrEditData("add",xmlData);
	
		return getResolutions();
	}
	
	public static String editImgResolution(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException{
		 
		addOrEditData("edit",xmlData);
		
		return getResolutions();
	}
	
	public static String deleteImgResolution(String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException{
		
		Document xmlDoc = SAXReaderUtil.read(xmlData);		
		
		String xPathQuery = "//rs/row";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
		
		String resolutionsIds = "";
		for (int i = 0; i < nodes.size(); i++){
			Element elem = (Element) nodes.get(i);
			if (elem.attribute("resolutionName")!= null){
				if (i == 0)
					resolutionsIds +=  "\"" +elem.attribute("resolutionName").getValue()+"\"";
				else
					resolutionsIds += "," + "\"" +elem.attribute("resolutionName").getValue()+"\"";
			}
		}
		
		String query = String.format(DELETE_RESOLUTIONS, resolutionsIds);
		
		DB db = DBFactoryUtil.getDB();
		db.runSQL(query);
	
		return getResolutions();
	}
	
	public static void addOrEditData(String operation, String xmlData) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException{
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		long checkDimensions = XMLHelper.getLongValueOf(dataRoot, "@checkDimensions");
		ErrorRaiser.throwIfNull(checkDimensions, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document xmlDoc = SAXReaderUtil.read(xmlData);		
		
		String xPathQuery = "//rs/row";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
		
		Node node = nodes.get(0);
		Element elem = (Element)node;
		
		String originalName = "";
		String name = "";
		int height = -1;
		int width = -1;
		String query = "";
		
		if (elem.attribute("originalName")!= null){
			originalName = elem.attribute("originalName").getValue();
		}
		if (elem.attribute("resolutionName")!= null){
			name = elem.attribute("resolutionName").getValue();
		}
		if ((elem.attribute("height")!= null) && (!elem.attribute("height").getValue().equals(""))){
			height = Integer.parseInt(elem.attribute("height").getValue());
		}
		if ((elem.attribute("width")!= null) && (!elem.attribute("width").getValue().equals(""))){
			width = Integer.parseInt(elem.attribute("width").getValue());
		}
		
		if (checkDimensions == 1)
			checkDimensions(originalName,width,height);
		
		if (operation.equals("add"))
			query = getQuery("add",null,name,width,height);
		else
			query = getQuery("edit",originalName,name,width,height);
		
		DB db = DBFactoryUtil.getDB();
		db.runSQL(query);
	
	}
	
	public static String getResolutions() throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException{
		
		Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom(String.format(FETCH_RESOLUTIONS));
			
		return tmplDom.asXML();
	}

	protected static String getQuery(String operation, String originalName, String name, int width, int height) throws SystemException{
		
		String query = "";
		if (operation.equals("edit")){
			if (height == -1)
				query = String.format(EDIT_RESOLUTION, name, width, null, originalName);
			else if (width == -1)
				query = String.format(EDIT_RESOLUTION, name, null, height, originalName);
			else{
				query = String.format(EDIT_RESOLUTION, name, width, height, originalName);
			}
		}
		else{
			if (height == -1)
				query = String.format(INSERT_RESOLUTION, name, width, null);
			else if (width == -1)
				query = String.format(INSERT_RESOLUTION, name, null, height);
			else
				query = String.format(INSERT_RESOLUTION, name, width, height);
		}
		
		return query;
	}
	
	protected static void checkDimensions(String name, int width, int height) throws SystemException, SecurityException, NoSuchMethodException, ServiceError{
		
		String query = "";
		if (height == -1)
			query = String.format(CHECK_DIMENSIONS+" width=%d and height is null", name,width);
		else if (width == -1)
			query = String.format(CHECK_DIMENSIONS+" height=%d and width is null", name,height);
		else
			query = String.format(CHECK_DIMENSIONS+" width=%d and height=%d", name,width, height);
		
		Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom(query);
		
		List<Node> nodes = tmplDom.selectNodes("rs/row[@resolutionName]");
		ErrorRaiser.throwIfFalse(nodes.size() == 0, IterErrorKeys.XYZ_E_DIMENSIONS_ALREADY_EXIST_ZYX);
	}
	
	public static String publishToLive() throws Exception{
		
		ImageResolutionLocalServiceUtil.publishToLive(getResolutions());
		return getResolutions();
	
	}
	
	
}
