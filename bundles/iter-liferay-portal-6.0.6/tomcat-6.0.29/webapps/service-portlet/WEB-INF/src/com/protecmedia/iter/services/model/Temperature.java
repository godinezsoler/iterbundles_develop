/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.model;

/**
 * Class for temperature conversions
 * 
 * @author lars ermert 2010
 *
 */
public class Temperature {

	public static final float 	NODATA_TEMP	= (float) -214.0; 
	public static final String	UNITS_SI	= "Si";
	public static final String	UNITS_LOL	= "burros";

	public static final String	HTML_CELSIUS	= "&deg;C";
	public static final String	HTML_FAHRENHEIT	= "&deg;F";
	
	private float 	fahrenheit	= NODATA_TEMP;
	private float 	celsius		= NODATA_TEMP;
	
	/**
	 * Instantiates a new temperature.
	 */
	public Temperature() {
	}

	public Temperature(float temp, String units) {
		set(temp, units);
	}
	
	public Temperature(Temperature temp) {
		if (temp!=null) {
			setFahrenheit(temp.getFahrenheit());
			setCelsius(temp.getCelsius());
		}
	}

	/**
	 * Returns the HTML degree sign and the corresponding temperature abbreviation
	 * @param	u	the temperature unit to use
	 * @return
	 */
	public static String getHTMLDegree(String u) {
		return (u.equalsIgnoreCase(Temperature.UNITS_LOL) ? Temperature.HTML_FAHRENHEIT : Temperature.HTML_CELSIUS);
	}
	
	public void reset() {
		setFahrenheit(NODATA_TEMP);
		setCelsius(NODATA_TEMP);
	}
	
	public void set(float temp, String units) {
		if (units.equalsIgnoreCase(UNITS_SI)) {
			setCelsius(temp);
		} else {
			setFahrenheit(temp);
		}
	}
	
	public float getFahrenheit() {
		if (fahrenheit==NODATA_TEMP) {
			// we don't have the fahrenheit temp, so calculate it from the one
			// that exists
			if (celsius!=NODATA_TEMP) {
				// we have celsius, so do the calculation
				return (celsius*9/5+32);
			}
		}
		return fahrenheit;
	}
	public float getCelsius() {
		if (celsius==NODATA_TEMP) {
			// we don't have the celsius temp, so calculate it from the one
			// that exists
			if (fahrenheit!=NODATA_TEMP) {
				// we have celsius, so do the calculation
				return ((fahrenheit-32)*5/9);
			}
		}
		return celsius;
	}

	public void setFahrenheit(float fahrenheit) {
		this.fahrenheit = fahrenheit;
	}
	public void setCelsius(float celsius) {
		this.celsius = celsius;
	}

	public float getTemp(String units) {
		if (units.equalsIgnoreCase(UNITS_SI))
			return getCelsius();
		else
			return getFahrenheit();
	}

	@Override
	public String toString() {
		return "Temperature [fahrenheit=" + fahrenheit + ", celsius=" + celsius
				+ "]";
	}

}
