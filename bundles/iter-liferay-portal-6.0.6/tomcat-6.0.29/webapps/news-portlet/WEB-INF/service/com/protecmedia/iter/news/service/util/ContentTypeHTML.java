package com.protecmedia.iter.news.service.util;
import java.io.IOException;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class ContentTypeHTML{
public String getTextContentTypeHTML(String groupId)
	{
		long groupid = Long.parseLong(groupId);
		String values= "";
		return getTextContentTypeHTML(groupid, IterKeys.STRUCTURE_ARTICLE, values).toString();
	}
	
	public String getTextContentTypeHTML(long groupId, String structureId, String contentTypesHTML)
	{
		JournalStructure struct = null;
		Document doc = null;
		try {
			struct = JournalStructureLocalServiceUtil.getStructure(groupId, structureId);
			doc = SAXReaderUtil.read(struct.getXsd());
		} catch (PortalException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SystemException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XPath xpath 	 = SAXReaderUtil.createXPath("/root/dynamic-element[@type = 'text_area' and @index-type='']/@name");
		List<Node> nodes = xpath.selectNodes(doc);
		
		Document results = SAXReaderUtil.createDocument();
		Element rootList = results.addElement("preference");
		
		for (Node node : nodes)
		{
			Element row =  rootList.addElement("value");
			row.addText(node.getStringValue());
		}
		
		{
			// En el caso de las galerías se añaden también los de segundo nivel
			xpath = SAXReaderUtil.createXPath("/root/dynamic-element/dynamic-element[@type = 'text_area' and @name!='Milenium' and @index-type='']/@name");
			nodes = xpath.selectNodes(doc);
			
			for (Node subnode : nodes)
			{
				String value = subnode.valueOf("../../@name").concat(".").concat(subnode.getStringValue());
				Element row =  rootList.addElement("value");
				row.addText(value);
			}
		}
		
		// Se añaden los tipos de contenidos de la estructura padre
		String parentStructureId = struct.getParentStructureId();
		if (parentStructureId != null && !parentStructureId.isEmpty())
			getTextContentTypeHTML(groupId, parentStructureId, contentTypesHTML);
		
		String s = "";
		try {
			s = results.formattedString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}
}