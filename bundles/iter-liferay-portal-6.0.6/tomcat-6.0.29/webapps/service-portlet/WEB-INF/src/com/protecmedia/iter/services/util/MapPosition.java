/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.protecmedia.iter.services.model.WeatherDay;




public class MapPosition {

	private static Log LOG = LogFactory.getLog(WeatherDay.class);
	
	private Float latitude	= null;
	private Float longitude	= null;
	private String city		= null;
	private String country	= null;
	
	private boolean initialized = false;

	public MapPosition() {
		initialized = false;
	}

	public MapPosition(String str) {
		parseString(str);
		initialized = true;
	}

	public void parseString(String str) {
		if ((str!=null) && (str.contains("|")) ) {
			String loc = str.split("\\|")[0];
			String crd = str.split("\\|")[1];
			
			if (loc.contains(",")) {
				setCity(loc.split(",")[0].trim());
				setCountry(loc.split(",")[1].trim());
				
				if (crd.contains(",")) {
					setLatitude(crd.split(",")[0].trim());
					setLongitude(crd.split(",")[1].trim());
					return;
				}
			}
		}
		LOG.error("parseString(): Couldn't parse '" + str + "'.");
		initialized = false;
	}

	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		if (latitude!=null)
			this.latitude = latitude;
	}
	public void setLatitude(String substring) {
		Float f;
		try {
			f = Float.valueOf(substring.trim());
		} catch (NumberFormatException e) {
			LOG.error("setLatitude(" + substring + "): Can't parse to float.");
			return;
		}
		latitude = f;
	}
	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
		if (longitude!=null)
			this.longitude = longitude;
	}
	public void setLongitude(String substring) {
		Float f;
		try {
			f = Float.valueOf(substring.trim());
		} catch (NumberFormatException e) {
			LOG.error("setLongitude(" + substring + "): Can't parse to float.");
			return;
		}
		longitude = f;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String str) {
		if (str!=null)
			city = str;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/** Assumes that the string str is of the form
	 * CITY, COUNTRY | LATITUDE, LONGITUDE and strips the geodata to only
	 * return the city, country part of the string
	 * @param str
	 */
	public static String stripGeoData(String str) {
		if (str.contains("|")) {
			return str.split("\\|")[0];
		}
		return str;
	}

}
