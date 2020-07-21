package com.protecmedia.iter.news.service.item;

import java.io.Serializable;

import com.liferay.portal.model.impl.BaseModelImpl;

public class Product extends BaseModelImpl<Product>
{

	private static final long serialVersionUID = 1L;

	private String 	productId;
	private String 	name;
	private String 	nameBase64;
	private long	_groupId;
	
	public Product(String productId, String name, String nameBase64, long groupId)
	{
		super();
		this.productId = productId;
		this.name = name;
		this.nameBase64 = nameBase64;
		_groupId = groupId;
	}

	public String getProductId()
	{
		return productId;
	}

	public void setProductId(String productId)
	{
		this.productId = productId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getNameBase64()
	{
		return nameBase64;
	}

	public void setNameBase64(String nameBase64)
	{
		this.nameBase64 = nameBase64;
	}
	
	public long getGroupId()
	{
		return _groupId;
	}
	
	@Override
	public Serializable getPrimaryKeyObj()
	{
		return productId;
	}

	@Override
	public String toXmlString() 
	{
		return null;
	}

	@Override
	public int compareTo(Product o)
	{
		return 0;
	}

	@Override
	public Object clone()
	{
		return null;
	}
}