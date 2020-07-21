package com.protecmedia.iter.search.util;

public class FacetedResult implements Comparable<FacetedResult>
{
	private String _id;
	private String _label;
	private long _count;
	private String _filter;
	private boolean _isSelected = false;

	public FacetedResult(String label, long count)
	{
		this._label = label;
		this._count = count;
	}

	public String getLabel()
	{
		return _label;
	}

	public long getCount()
	{
		return _count;
	}
	
	public String getId()
	{
		return this._id;
	}
	
	public String getFilter()
	{
		return this._filter;
	}
	
	public void setId(String value)
	{
		this._id = value;
	}
	
	public void setFilter(String value)
	{
		this._filter = value;
	}

	public boolean isSelected()
	{
		return _isSelected;
	}

	public void setSelected(boolean isSelected)
	{
		this._isSelected = isSelected;
	}
	
	public String getSelected()
	{
		return _isSelected ? "checked" : "";
	}

	@Override
	public int compareTo(FacetedResult fr)
	{
		return (int)fr.getCount() - (int)this.getCount();
	}

}
