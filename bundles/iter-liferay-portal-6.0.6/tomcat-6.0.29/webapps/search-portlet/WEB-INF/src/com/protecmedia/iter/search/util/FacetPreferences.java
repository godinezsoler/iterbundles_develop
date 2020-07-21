package com.protecmedia.iter.search.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.util.xml.CDATAUtil;

public class FacetPreferences
{
	private List<String> _selectedItems;
	private String _segmentationType;
	private String _title;
	private String _prefix;
	private String _sufix;
	private int _minArticle;
	private boolean _showEmpty;
	private int _columns;
	private String _dateformat;
	private String _datelanguage;
	private boolean _autoSegmentation = false;
	private String _autoSegmentationLimit = "0";
	
	private Map<String, RangePreferences> _rangePrefs;
	
	public FacetPreferences(String segmentationType)
	{
		this._segmentationType = segmentationType;
		this._selectedItems = new ArrayList<String>();
	}
	
	public String getSegmentationType()
	{
		return this._segmentationType;
	}
	
	public List<String> getSelectedItems()
	{
		return _selectedItems;
	}
	
	public void addItem(String item)
	{
		this._selectedItems.add(item);
	}

	public String getTitle()
	{
		return _title;
	}
	
	public void setTitle(String title)
	{
		this._title = CDATAUtil.strip(title);
	}
	
	public String getPrefix()
	{
		return _prefix;
	}
	
	public void setPrefix(String prefix)
	{
		this._prefix = prefix;
	}
	
	public String getSufix()
	{
		return _sufix;
	}
	
	public void setSufix(String sufix)
	{
		this._sufix = sufix;
	}
	
	public int getMinArticle()
	{
		return _minArticle;
	}
	
	public void setMinArticle(int minArticle)
	{
		this._minArticle = minArticle;
	}
	
	public boolean showEmpty()
	{
		return _showEmpty;
	}
	public void showEmpty(boolean showEmpty)
	{
		this._showEmpty = showEmpty;
	}
	
	public int getColumns()
	{
		return _columns;
	}
	
	public void setColumns(int columns)
	{
		this._columns = columns;
	}
	
	public String getDateformat()
	{
		return this._dateformat;
	}
	public void setDateformat(String dateformat)
	{
		this._dateformat = dateformat;
	}
	public String getDatelanguage()
	{
		return this._datelanguage;
	}
	public void setDatelanguage(String datelanguage)
	{
		this._datelanguage = datelanguage;
	}
	
	public void setRangePreferences(String rangeId, String label, String gapUnit, int gapValue)
	{
		if(this._rangePrefs==null)
			this._rangePrefs = new HashMap<String, RangePreferences>(); 
		
		this._rangePrefs.put(rangeId, new RangePreferences(label, gapUnit, gapValue));
		addItem(rangeId);
	}
	
	public RangePreferences getRangePreferences(String key)
	{
		return this._rangePrefs.get(key);
	}

	public class RangePreferences
	{
		private String _gapUnit;
		private int _gapValue;
		private String _label;
		
		public RangePreferences(String label, String gapUnit, int gapValue)
		{
			this._label = label;
			this._gapUnit = gapUnit;
			this._gapValue = gapValue;
		}
		
		public String getGapUnit()
		{
			return this._gapUnit;
		}
		
		public int getGapValue()
		{
			return this._gapValue;
		}
		
		public String getLabel()
		{
			return this._label;
		}
		
	}

	public boolean is_autoSegmentation()
	{
		return _autoSegmentation;
	}

	public void set_autoSegmentation(boolean autoSegmentation)
	{
		this._autoSegmentation = autoSegmentation;
	}

	public String getAutoSegmentationLimit()
	{
		return _autoSegmentationLimit;
	}

	public void setAutoSegmentationLimit(String autoSegmentationLimit)
	{
		this._autoSegmentationLimit = autoSegmentationLimit;
	}
}
