/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.model;


import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;


/**
 * @author Lars Ermert
 * loosely based on the class by Brian Wing Shun Chan
 */

@SuppressWarnings("serial")
public class WeatherDay implements Serializable {
	
	private static Log _log = LogFactoryUtil.getLog(WeatherDay.class);

	private static String 	NODATA_HUMIDITY		= "nodatahumidity"; 
	private static String 	NODATA_WIND			= "nodatawind";
	private static String	NODATA_ICONURL		= "nodataiconurl"; 
	private static String	NODATA_CONDITIONS	= "nodataconditions"; 

	public static String	TEMP_HIGH	= "temphigh"; 
	public static String	TEMP_LOW	= "templow"; 
	public static String	TEMP_CURR	= "tempcurr"; 
	
	private Date		date;
	private String		dateformat	= "EEEEEEEEEE";
	private String		units		= Temperature.UNITS_SI;
	private String		windcond	= "";
	private Temperature	tempHigh	= null;
	private Temperature	tempLow		= null;
	private Temperature	current		= null;
	private String		searchkey		= "";
	
	private String 	_iconURL;
	private String 	_conditions;
	private String 	humidity;


	/**
	 * Constructor with subset of necessary data used for forecasts.
	 *
	 * @param cal		date whose data this objects represents
	 * @param searchkey		zip code or name of the city
	 * @param temp		the current temperature
	 * @param high 		high temperature
	 * @param low		low temperature
	 * @param iconURL 	url of the weather image to display
	 * @param cond		weather conditions
	 */
	public WeatherDay(Date cal, String searchkey,
			Temperature high, Temperature low,
			String iconURL, String conditions) {
		this(cal, searchkey, new Temperature(), high, low, iconURL, conditions,
				NODATA_HUMIDITY, NODATA_WIND);
	}

	/**
	 * Constructor with subset of necessary data used for current conditions.
	 *
	 * @param cal 		date whose data this objects represents
	 * @param searchkey 		zip code or name of the city
	 * @param temp		the current temperature
	 * @param iconURL 	url of the weather image to display
	 * @param cond		weather conditions
	 * @param hum 		humidity
	 * @param wind 		wind conditions
	 */
	public WeatherDay(Date cal, String searchkey, Temperature temp, String iconURL, String cond, String hum, String wind) {
		this(cal, searchkey, temp, new Temperature(), new Temperature(),iconURL, cond, hum, wind);
	}
	public WeatherDay(Date cal, String searchkey, Temperature temp, Temperature high, Temperature low, String iconURL) {
		this(cal, searchkey, temp, high, low,iconURL,
				NODATA_CONDITIONS, NODATA_HUMIDITY,
				NODATA_WIND);
	}

	/**
	 * Constructor with all necessary data.
	 *
	 * @param cal 			date whose data this objects represents
	 * @param searchkey 			zip code or name of the city
	 * @param temp 			the current temperature
	 * @param high 			high temperature
	 * @param low			low temperature
	 * @param iconURL 		url of the weather image to display
	 * @param conditions	weather conditions
	 * @param hum 			humidity
	 * @param wind 			wind conditions
	 */
	public WeatherDay(
			Date cal, String skey,
			Temperature temp, Temperature high, Temperature low,
			String iconURL, String conditions,
			String hum, String wind) {

		searchkey 		= skey;
		
		setDate(cal);
		setCurrent(temp);
		setTempHigh(high);
		setTempLow(low);
		setWindcond(wind);

		_iconURL 		= iconURL;
		_conditions 	= conditions;
		
		humidity 		= hum;
	}

	public String getSearchkey() {
		return searchkey;
	}
	public void setsearchkey(String s) {
		searchkey = s;
	}
	public String getCityName() {
//		String ret = searchkey.split(",")[0].trim().toLowerCase();
//		return ret.substring(0, 1).toUpperCase() + ret.substring(1, ret.length());
		return searchkey.split(",")[0].trim();
	}
	
	public String getIconURL() {
		return _iconURL;
	}
	public String getIconFilename() {
		if (_iconURL!=null) {
			Pattern pattern = Pattern.compile("[^/]*(?=\\.[(gif)(html)]*$)");
			Matcher matcher = pattern.matcher(_iconURL);
			if (matcher.find())
				return matcher.group().replace("-", "").replace("_of_", "");
		}
		return _iconURL;
	}
	public void setIconURL(String iconURL) {
		_iconURL = iconURL;
	}

	public String getConditions() {
		return _conditions;
	}
	public void setConditions(String conditions) {
		_conditions = conditions;
	}

	public String getHumidity() {
		return humidity;
	}
	public void setHumidity(String hum) {
		humidity = hum;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	public Date getDate() {
		return date;
	}
	public String getDateString(String locstr) {
		return getDateString(dateformat, locstr);
	}
	public String getDateString(String format, String locstr) {
		DateFormat formatter = null;
		try {
			Locale loc = new Locale(locstr);
			formatter = new SimpleDateFormat(format, loc);
			return formatter.format(getDate());
		} catch (Exception e) {
			_log.error("Can't parse date to String.");
		}
		return "Error";
	}
	
	@Override
	public String toString() {
		return "WeatherDay [date=" + date + ", units=" + units + ", windcond="
				+ windcond + ", tempHigh=" + tempHigh + ", tempLow=" + tempLow
				+ ", current=" + current + ", searchkey=" + searchkey + ", _iconURL="
				+ _iconURL + ", _conditions=" + _conditions + ", _humidity="
				+ humidity + "]";
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public String getUnits() {
		return units;
	}



	public void setTempHigh(Temperature th) {
		tempHigh = new Temperature(th);
	}

	public void setTempLow(Temperature tl) {
		tempLow = new Temperature(tl);
	}

	public void setCurrent(Temperature tc) {
		current = new Temperature(tc);
	}

	/**
	 * Returns a String containing the temperature and the corresponding HTML
	 * temperature unit sign
	 * @param type	which temperature to show (TEMP_HIGH, TEMP_LOW, TEMP_CURR)
	 * @param u		in which unit should the temperature be shown
	 * @return
	 */
	public String getTempString(String type, String u) {
		
		int temp = (int) Temperature.NODATA_TEMP;

		if (type.equalsIgnoreCase(TEMP_CURR))
			temp = Math.round(getCurrent(u));
		if (type.equalsIgnoreCase(TEMP_LOW))
			temp = Math.round(getTempLow(u));
		if (type.equalsIgnoreCase(TEMP_HIGH))
			temp = Math.round(getTempHigh(u));
		
		return "" + temp + Temperature.getHTMLDegree(u);
	}

	/**
	 * Returns true if the temperature is below 0C
	 * @param type
	 * @return
	 */
	public boolean tempIsFreezing(String type) {
		
		int temp = (int) Temperature.NODATA_TEMP;

		if (type.equalsIgnoreCase(TEMP_CURR))
			temp = Math.round(getCurrent(Temperature.UNITS_SI));
		if (type.equalsIgnoreCase(TEMP_LOW))
			temp = Math.round(getTempLow(Temperature.UNITS_SI));
		if (type.equalsIgnoreCase(TEMP_HIGH))
			temp = Math.round(getTempHigh(Temperature.UNITS_SI));
		
		return (temp<=0 ? true : false);
	}
	
	public float getTempHigh() {
		return getTempHigh(getUnits());
	}
	public float getTempHigh(String u) {
		return tempHigh.getTemp(u);
	}
	public float getTempLow() {
		return getTempLow(getUnits());
	}
	public float getTempLow(String u) {
		return tempLow.getTemp(u);
	}
	public float getCurrent() {
		return getCurrent(getUnits());
	}
	public float getCurrent(String u) {
		return current.getTemp(u);
	}
	
	public String getHTMLUnitSign(String u) {
		return (u.equalsIgnoreCase(Temperature.UNITS_SI)) ? Temperature.HTML_CELSIUS : Temperature.HTML_FAHRENHEIT;
	}
	public String getHTMLUnitSign() {
		return getHTMLUnitSign(getUnits());
	}

	public void setWindcond(String windcond) {
		this.windcond = windcond;
	}

	public String getWindcond() {
		return windcond;
	}

	public void setDateformat(String dateformat) {
		this.dateformat = dateformat;
	}

	public String getDateformat() {
		return dateformat;
	}

}
