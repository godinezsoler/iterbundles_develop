/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

import java.util.Date;

public class RecentResult {

	private String geolocation;
	private String landing_page;
	private Date time;
	private String countryCode;
		
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public RecentResult() {		
	}
	
	public String getGeolocation() {
		return geolocation;
	}
	public void setGeolocation(String geolocation) {
		this.geolocation = geolocation;
	}
	public String getLanding_page() {
		return landing_page;
	}
	public void setLanding_page(String landingPage) {
		landing_page = landingPage;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}				
	public void setTime(long time) {
		this.time = new Date();
		this.time.setTime(time * 1000);
	}
}
