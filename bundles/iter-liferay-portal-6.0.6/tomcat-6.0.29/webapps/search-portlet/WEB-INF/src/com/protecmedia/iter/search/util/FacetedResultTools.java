package com.protecmedia.iter.search.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.facet.FacetGroup;
import com.liferay.portal.kernel.search.facet.FacetGroupImpl;
import com.liferay.portal.kernel.search.facet.FacetedField;
import com.liferay.portal.kernel.search.facet.RangeFacetGroup;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.service.util.IterKeys;

public class FacetedResultTools
{

	private static Log _log = LogFactoryUtil.getLog(FacetedResultTools.class);

	public static List<FacetedResult> getFacetedResultsAuto(String field, FacetedField facet, String filterquery, int minArticle, List<String> keys)
	{
		List<FacetedResult> retVal = new ArrayList<FacetedResult>();
		
		try
		{
			if( facet!=null )
			{
				if(Validator.isNotNull(keys))
				{
					@SuppressWarnings("unchecked")
					Collection<String> existingKeys = CollectionUtils.intersection(keys, facet.getKeys());
					
					for(String key : existingKeys)
					{
						List<FacetGroup> facetsGroup = facet.getValues(key);
						_getFacetedResultsAuto(field, minArticle, filterquery, facetsGroup, retVal);
					}
				}
				else
				{
					List<FacetGroup> facetsGroup = facet.getValues();
					_getFacetedResultsAuto(field, minArticle, filterquery, facetsGroup, retVal);
				}
				
				Collections.sort(retVal);
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return retVal;
	}
	
	private static void _getFacetedResultsAuto(String field, int minArticle, String filterquery, List<FacetGroup> facetsGroup, List<FacetedResult> retVal)
	{
		for(FacetGroup fg : facetsGroup)
		{
			if(fg.getCount() >= minArticle)
			{
				FacetGroupImpl fq = (FacetGroupImpl)fg;
				FacetedResult fr = new FacetedResult(fq.getLabel(), fg.getCount());
				String name = fq.getGroupName();
				String prefix = field+StringPool.COLON;
				
				fr.setId( name );
				StringBuilder sb = new StringBuilder();
				
				int idx = name.lastIndexOf(StringPool.SECTION);
				if(idx!=-1)
					sb.append( name.substring( 0, idx ) );
				else
					sb.append( name );
				
				sb.append(StringPool.STAR);
				
				if(filterquery.contains(sb.toString()))
					fr.setSelected(true);
				
				sb.insert(0, prefix);
				
				fr.setFilter( sb.toString() );
				
				retVal.add(fr);
			}
		}
	}
	
	public static List<FacetedResult> getFacetedResults(String field, FacetedField facet, String filterquery, int minArticle, List<String> keys)
	{
		List<FacetedResult> retVal = new ArrayList<FacetedResult>();
		
		try
		{
			if( facet!=null )
			{
				long tIni = 0L;
				if(_log.isTraceEnabled())
					tIni = System.currentTimeMillis();
				
				@SuppressWarnings("unchecked")
				Collection<String> existingKeys = CollectionUtils.intersection(keys, facet.getKeys());
				
				if(_log.isTraceEnabled())
					_log.trace( String.format("SOLR-TIME Mezcla de las dos colecciones de claves. Tiempo %s ms.", System.currentTimeMillis()-tIni));
				
				for(String key : existingKeys)
				{
					List<FacetGroup> facetsGroup = facet.getValues(key);
					for(FacetGroup fg : facetsGroup)
					{
						if(fg.getCount() >= minArticle)
						{
							FacetGroupImpl fq = (FacetGroupImpl)fg;
							FacetedResult fr = new FacetedResult(fq.getLabel(), fg.getCount());
							String name = fq.getGroupName();
							String prefix = field+StringPool.COLON;
							
							fr.setId( key );
							fr.setFilter( prefix+name );
							if(filterquery.contains(key))
								fr.setSelected(true);
							
							retVal.add(fr);
						}
					}
				}
				
				Collections.sort(retVal);
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return retVal;
	}
	
	public static List<FacetedResult> getRangeFacetedResults(String field, FacetedField facet, String filterquery, String key, String dateFormat, String dateLang, String label, int gapInterval, String gapUnit) throws java.text.ParseException
	{
		List<FacetedResult> retVal = new ArrayList<FacetedResult>();
		List<FacetGroup> facetsGroup = null;
		
		try
		{
			if(Validator.isNull(key))
				facetsGroup = facet.getValues();
			else
				facetsGroup = facet.getValues(key);
			
			if(facetsGroup!=null && facetsGroup.size()>0)
			{
				RangeFacetGroup fg = (RangeFacetGroup) facetsGroup.get(0);
				JSONObject dateInfo = labelFormat(fg.getStart(), fg.getEnd(), gapInterval, gapUnit, dateFormat, label);
				String format = dateInfo.getString("format");
				DateFormat df = null;
				if(Validator.isNotNull(format))
					df = new SimpleDateFormat(format, new Locale(dateLang));
				
				DateFormat UTC_format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_T_HH_MM_ss_Z);
				DateFormat urlDateFormatExt = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT_EXT_HH);
				int gapCalendarUnit = dateInfo.getInt("gapCalendarUnit");
				String prefix = field+StringPool.COLON;
				
				for(FacetGroup facetgroup : facetsGroup)
				{
					RangeFacetGroup range = (RangeFacetGroup)facetgroup;
					
					Date rangeIni = range.getStart();
					Date rangeFin = range.getEnd();
					
					rangeIni = UTC_format.parse(range.getGroupName());
					Calendar c = Calendar.getInstance();
					c.setTime(rangeIni);
					c.add(gapCalendarUnit, gapInterval);
					c.add(Calendar.SECOND, -1);
					rangeFin = c.getTime();
					
					String resultLabel = label;
					
					if( Validator.isNull(label) && Validator.isNotNull(df) )
						resultLabel = df.format(rangeIni);
					
					FacetedResult fr = new FacetedResult(resultLabel, range.getCount());
					fr.setId(key);
					
					StringBuilder sb = new StringBuilder()
										.append( urlDateFormatExt.format(rangeIni) )
										.append("TO")
										.append( urlDateFormatExt.format(rangeFin) );
					
					
					if(filterquery.contains(sb.toString()))
						fr.setSelected(true);
					
					sb.insert(0, prefix);
					fr.setFilter( sb.toString() );
					
					retVal.add(fr);
				}
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return retVal;
	}
	
	private static JSONObject labelFormat(Date start, Date end, int gapInterval, String gapUnit, String dateFormat, String label)
	{
		JSONObject retVal = JSONFactoryUtil.createJSONObject();
		
		int diffInDays = Math.abs( (int)( (start.getTime() - end.getTime()) / Time.DAY ) );
		int gapInDays = gapInterval;
		String gapFormat = dateFormat;
		String regExpr = "";
		String separatorRegExpr = "[\\\\\\-,/.\\s]+";
		String separator = "";

		if(gapUnit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_HOUR))
		{
			retVal.put("gapCalendarUnit", Calendar.HOUR_OF_DAY);
			gapInDays = gapInterval / 24;
		}
		else if(gapUnit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_DAY))
		{
			retVal.put("gapCalendarUnit", Calendar.DAY_OF_MONTH);
		}
		else if(gapUnit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_MONTH))
		{
			regExpr = "[^d\\\\\\-,/.\\s]+";
//			gapFormat = dateFormat.replaceAll("d", "");
			gapInDays = gapInterval * 30;
			retVal.put("gapCalendarUnit", Calendar.MONTH);
		}
		else if(gapUnit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_YEAR))
		{
			regExpr = "[^dm\\\\\\-,/.\\s]+";
//			gapFormat = dateFormat.replaceAll("d", "");
//			gapFormat = gapFormat.replaceAll("m", "");
			gapInDays = gapInterval * 365;
			retVal.put("gapCalendarUnit", Calendar.YEAR);
		}
		
		if(diffInDays > gapInDays || Validator.isNull(label))
		{
			if(regExpr!="")
			{
				List<String> groups = new ArrayList<String>(); 
				Pattern p = Pattern.compile( regExpr, Pattern.CASE_INSENSITIVE );
				Matcher m = p.matcher(dateFormat);
				while(m.find())
				{
					groups.add( m.group(0) );
				}
				
				if(groups.size()>1)
				{
					p = Pattern.compile( separatorRegExpr, Pattern.CASE_INSENSITIVE );
					m = p.matcher(dateFormat);
					while( m.find() )
					{
						if(!m.group(0).equals(StringPool.COMMA))
						{
							separator = m.group(0);
							break;
						}
					}
					gapFormat = StringUtils.join(groups.iterator(), separator); 	
				}
				else if(groups.size()==1)
					gapFormat = groups.get(0);
			}
			
			retVal.put("format", gapFormat) ;
		}
		return retVal;
	}

}
