/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.service.item;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.ProductLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
* 
	<item operation="create" 
		  globalid="DiariTerrassaPre_151926" 
		  classname="com.protecmedia.iter.news.model.Product" 
		  groupid="10132">
		  
		<param name="name">&lt;![CDATA[Premium]]&gt;</param>
		
	</item>
*/

public class ProductXmlIO extends ItemXmlIO
{
	
	private static Log _log = LogFactoryUtil.getLog(ProductXmlIO.class);
	
	private String _className = IterKeys.CLASSNAME_PRODUCT;
	
	public ProductXmlIO()
	{
		super();
	}
	
	public ProductXmlIO(XMLIOContext xmlIOContext)
	{
		super(xmlIOContext);
	}
	
	@Override
	public String getClassName()
	{
		return _className;
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		if(model != null)
		{
			Product product = (Product)model;
			String productId = product.getProductId();
			
			Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(product.getGroupId(), IterKeys.CLASSNAME_GROUP, String.valueOf(product.getGroupId()));
			
			LiveLocalServiceUtil.add(_className, product.getGroupId(), liveGroup.getId(), liveGroup.getId(), 
									 IterLocalServiceUtil.getSystemName() + "_" + productId, productId, 
									 IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		}		
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		if(model != null)
		{
			long globalGroupId = GroupMgr.getGlobalGroupId();
			
			Product product = (Product)model;
			String productId = product.getProductId();
			
			LiveLocalServiceUtil.add(_className, product.getGroupId(), IterLocalServiceUtil.getSystemName() + "_" + productId, productId, 
									 IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		}
	}
	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live)
	{
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();	
				
		try
		{
			attributes.put("classname", getClassName());				
			attributes.put("groupid", 	group.getName());				
			attributes.put("globalid", 	live.getGlobalId());		
			attributes.put("operation", operation);
			attributes.put("id_", 		String.valueOf(live.getId()));
			
			if(!operation.equals(IterKeys.DELETE))
			{
				String name = ProductLocalServiceUtil.getProductNameById(live.getLocalId());
				if(Validator.isNotNull(name))
					params.put("name", name);		
			}
			
			addNode(root, "item", attributes, params);
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
			error = e.toString();
			if(live != null)
				error += " - Cannot export item " + live.getLocalId();
		}

		return error;
	}
	
	@Override
	protected void delete(Element item)
	{
		String globalId = getAttribute(item, "globalid");
		String sGroupId = getAttribute(item, "groupid");		

		try
		{
			long groupId = getGroupId(sGroupId);
			Live live	 = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			
			try
			{
				Element param = SAXReaderUtil.createElement("param");
				Element target = SAXReaderUtil.createElement("target");
				target.addAttribute("id", live.getLocalId());				
				param.add(target);				
				
				ProductLocalServiceUtil.deleteProduct(param.asXML());

				// Clean entry in live table
				LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(), IterKeys.DELETE, 
										 IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
				
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE,
												IterKeys.DONE, IterKeys.DONE, sGroupId);
			}
			catch (Exception e)
			{		
				_log.error(e.toString());
				_log.trace(e);
				
				if (live != null)
				{
					LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(), IterKeys.DELETE, 
							 IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
				}
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, e.toString(), IterKeys.ERROR, sGroupId);
			}
		}
		catch (Exception e)
		{		
			_log.error(e.toString());
			_log.trace(e);
			
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, e.toString(), IterKeys.ERROR, sGroupId);	
		}
	}
	
	@Override
	protected void modify(Element item, Document doc)
	{
		String globalId = getAttribute(item, "globalid");
		String sGroupId = getAttribute(item, "groupid");
		String operation = getAttribute(item, "operation");
		String name = getParamTextByName(item, "name");	
		
		try
		{
			long groupId = getGroupId(sGroupId);
			
			Element param  = SAXReaderUtil.createElement("param");
			Element target = SAXReaderUtil.createElement("target");
			param.add(target);
			param.addAttribute("siteid", String.valueOf(groupId));
			target.addAttribute("name", name);
			
			try
			{ 
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				target.addAttribute("id", live.getLocalId());
				
				ProductLocalServiceUtil.updateProduct(param.asXML());
				
				LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(), operation, 
										 IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
			}
			catch(Exception e)
			{
				String productId = ProductLocalServiceUtil.addProduct(param.asXML());
				
				LiveLocalServiceUtil.add(_className, groupId, globalId, productId, operation, 
					 	 				 IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
			}
			
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE,
											IterKeys.DONE, IterKeys.DONE, sGroupId);
		}
		catch (Exception e)
		{		
			_log.error(e.toString());
			_log.trace(e);
			xmlIOContext.itemLog.addMessage(item, globalId, _className, operation, e.toString() + 
											" - Error modifying element", IterKeys.ERROR, sGroupId);	
		}
	}

	@Override
	public void populateLive(long groupId, long companyId) throws SystemException, PortalException
	{
	}
}