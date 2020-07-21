package com.protecmedia.iter.xmlio.service.util;

import java.util.HashMap;
import java.util.Map;

public class Item {
	
	Map<String,String> header;
	Map<String,String> params;
	
	public Item(){
		header = new HashMap<String,String>();
		params = new HashMap<String,String>();
	}
	
	public void setHeader(Map<String,String> header){
		this.header = header;
	}
	
	public void setParams(Map<String,String> params){
		this.params = params;
	}
	
	public Map<String,String> getHeader(){
		return this.header;
	}
	
	public Map<String,String> getParams(){
		return params;
	}
}
