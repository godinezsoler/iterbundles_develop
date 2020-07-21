/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class YahooCoordinates {


	private static Log LOG = LogFactory.getLog(YahooCoordinates.class);
	// some basic configuration parameters
	private static String VAR_LOCALE			= "en_US";
//	private static String VAR_LOCALE			= "de_de";
//	private static String VAR_LOCALE			= "nl_nl";
//	private static String VAR_LOCALE			= "es_es";
	private static String VAR_APPID				= "[yoere]";
	
	// the names of the tags used inside the xml response
	private static String TAG_QUALITY_START		= "<quality>";
	private static String TAG_QUALITY_STOP		= "</quality>";
	private static String TAG_LATITUDE_START	= "<latitude>";
	private static String TAG_LATITUDE_STOP		= "</latitude>";
	private static String TAG_LONGITUDE_START	= "<longitude>";
	private static String TAG_LONGITUDE_STOP	= "</longitude>";
	private static String TAG_CITY_START		= "<city>";
	private static String TAG_CITY_STOP			= "</city>";
	private static String TAG_COUNTRY_START		= "<country>";
	private static String TAG_COUNTRY_STOP		= "</country>";
	
	// the basic search string used for all requests
	private static String webaddress = "http://where.yahooapis.com/geocode?appid="
											+ VAR_APPID 
											+ "&locale=" + VAR_LOCALE;
	// result values

	private int quality = 0;

	// the string used to search and the response
	private String searchstring = null;
	private String response = null;

	// the MapPosition object in which we save the results
	MapPosition mapposition = null;

	public YahooCoordinates (String search) {
		searchstring = search;
		mapposition = new MapPosition();
		parseFromString(search);
	}
	
	private void parseFromString(String search) {
		
		String webaddr = "";
		
		if (search.contains("|"))
			search = search.substring(0, search.indexOf("|"));
		
		try {
			webaddr = webaddress + "&q=" + URLEncoder.encode(search.trim(),"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			LOG.error("getFromName(" + search + "): Can't encode url.");
			return;
		}

		LOG.debug("getFromName(" + search + "): path is '" + webaddr + "'.");
		
		// read the webpage
		response = getHTMLPage(webaddr);

		// get the quality of the result
		parseQuality();
		
		// get the city of the result
		parseCity();

		// get the country of the result
		parseCountry();

		// get the latitude part
		parseLatitude();
		
		// get the longitude part
		parseLongitude();
	}
	
	public String getResultString(boolean originalnames) {
		return getCityName(originalnames).trim() + ", " + getCountryName(originalnames).trim() +
					" | " + mapposition.getLatitude() + ", " + mapposition.getLongitude();
	}
	
	private String getHTMLPage(String urlToRead) {
	      URL url; // The URL to read
	      HttpURLConnection conn; // The actual connection to the web page
	      BufferedReader rd; // Used to read results from the web page
	      String line; // An individual line of the web page HTML
	      String result = ""; // A long string containing all the HTML
	      try {
	         url = new URL(urlToRead);
	         conn = (HttpURLConnection) url.openConnection();
	         conn.setRequestMethod("GET");
	         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	         while ((line = rd.readLine()) != null) {
	            result += line;
	         }
	         rd.close();
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	      return result;
	   }

	public String getCityName(boolean original) {
		if (original) {
			if (searchstring!=null) {
				if (searchstring.contains("|"))
					return searchstring.split("\\|")[0].split(",")[0];
				else
					return searchstring.split(",")[0];
			}
			LOG.error("getCityName(): Couldn't parse '" + searchstring + "'.");
		}
		if (mapposition!=null)
			return mapposition.getCity();
		else return "";
	}
	public String getCountryName(boolean original) {
		if (original) {
			if (searchstring!=null) {
				try {
					if (searchstring.contains("|"))
						return searchstring.split("\\|")[0].split(",")[1];
					else
						return searchstring.split(",")[1];
				} catch (Exception e) {
					LOG.error("getCountryName(): No country data in search String '" + searchstring + "'.");
				}
			}
			LOG.error("getCountryName(): Couldn't parse '" + searchstring + "'.");
		}
		if (mapposition!=null)
			return mapposition.getCountry();
		else return "";
	}
	
	private void parseQuality() {
		String lat = parseXML(TAG_QUALITY_START, TAG_QUALITY_STOP);
		if (lat==null) {
			LOG.error("parseQuality(): Couldn't parse quality from string '" + response + "'.");
		} else {
			setQuality(lat);
		}
	}
	private void setQuality(String str) {
		int i;
		try {
			i = Integer.valueOf(str);
		} catch (NumberFormatException e) {
			LOG.error("setQuality(" + str + "): Can't parse to int.");
			return;
		}
		setQuality(i);
	}
	private void setQuality(int i) {
		quality  = i;
	}
	public int getQuality() {
		return quality;
	}
	
	
	
	
	
	
	private String parseXML(String start, String stop) {
		if (response.indexOf(start)!=-1 && response.indexOf(stop)!=-1 ) {
			LOG.debug("parseXML(): " + response.indexOf(start) + ", " + response.indexOf(stop));
			return response.substring(
					response.indexOf(start) + start.length(),
					response.indexOf(stop));
		} else {
			return null;
		}
	}
	
	private void parseCity() {
		String lat = parseXML(TAG_CITY_START, TAG_CITY_STOP);
		if (lat==null) {
			LOG.error("parseCity(): Couldn't parse latitude from string '" + response + "'.");
		} else {
			mapposition.setCity(lat);
		}
	}

	private void parseCountry() {
		String lat = parseXML(TAG_COUNTRY_START, TAG_COUNTRY_STOP);
		if (lat==null) {
			LOG.error("parseCountry(): Couldn't parse latitude from string '" + response + "'.");
		} else {
			mapposition.setCountry(lat);
		}
	}

	
	private void parseLatitude() {
		String lat = parseXML(TAG_LATITUDE_START, TAG_LATITUDE_STOP);
		if (lat==null) {
			LOG.error("parseLatitude(): Couldn't parse latitude from string '" + response + "'.");
		} else {
			mapposition.setLatitude(lat);
		}
	}


	public Float getLatitude() {
		return mapposition.getLatitude();
	}
	public Float getLongitude() {
		return mapposition.getLongitude();
	}	

	private void parseLongitude() {
			String lat = parseXML(TAG_LONGITUDE_START, TAG_LONGITUDE_STOP);
			if (lat==null) {
				LOG.error("parseLongitude(): Couldn't parse longitude from string '" + response + "'.");
			} else {
				mapposition.setLongitude(lat);
			}
		}


	public MapPosition getMapPosition() {
		return mapposition;
	}
	
	public String getResponse() {
		return response;
	}
	
	public String toString() {
		return "City: " + 
			"\n ---------------------------------" + 
			"\n Lat:      " + mapposition.getLatitude() + 
			"\n Long:     " + mapposition.getLongitude() + 
			"\n Qual:     " + getQuality() +
			"\n City:     " + getCityName(false) +
			"\n City O:   " + getCityName(true) +
			"\n Coutry:   " + getCountryName(false) +
			"\n Coutry O: " + getCountryName(true) +
			"\n Response: " + getResponse() +
			"\n ---------------------------------";
	}
	
}



