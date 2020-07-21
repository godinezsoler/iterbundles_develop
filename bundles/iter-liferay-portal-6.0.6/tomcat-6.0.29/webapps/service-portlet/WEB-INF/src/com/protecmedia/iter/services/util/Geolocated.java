package com.protecmedia.iter.services.util;

public class Geolocated {
	
	private String postalAddress;
	private String markerText;
	private String mapType;
	private String zoom;
	
	
	public Geolocated(){
		postalAddress = "";
		markerText = "";
		mapType = "";
		zoom = "";
	}
	
	public String getPostalAddress() {
		return postalAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}
	
	public String getMarkerText() {
		return markerText;
	}

	public void setMarkerText(String markerText) {
		this.markerText = markerText;
	}
	
	public String getMapType() {
		return mapType;
	}

	public void setMapType(String mapType) {
		this.mapType = mapType;
	}
	
	public String getZoom() {
		return zoom;
	}

	public void setZoom(String zoom) {
		this.zoom = zoom;
	}


}
