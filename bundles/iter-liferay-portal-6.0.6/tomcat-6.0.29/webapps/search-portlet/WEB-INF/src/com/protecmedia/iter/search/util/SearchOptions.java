/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.search.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class SearchOptions {

	//Búsqueda difusa
	private boolean fuzzy = false;
	
	//delegación donde buscar
	private long delegationId = 0l;
	
	//estructura
	private boolean checkArticle = false;
	private boolean checkPoll = false;
	
	//cadena a buscar
	private String text = "";
	
	private List<Long> categoriesIds = new ArrayList<Long>();
	
	//páginas de los resultados
	private List<Long> layoutsPlid = new ArrayList<Long>();
	
	//fechas en las que se publican
	private int auxAgno = 2000; //Fecha anterior
	private int auxMes = 1; 
	private int auxDia = 1;
	Calendar calendar = new GregorianCalendar(auxAgno, auxMes, auxDia); 
	private Date startDate = new java.sql.Date(calendar.getTimeInMillis());
	private Date endDate = new Date();
	
	//rango de resultados
	private int itemsPerPage = 100; 
	private int page = 1; 
	
	//orden de los resultados
	private String order = "";
	
	//campos para facetar
	private String filterquery = "";

	//campo libre
	private String wildcard = "";
	
	//CONSTANTES
	public static final String ORDER_BY_RELEVANCE = "relevance";
	public static final String ORDER_BY_DATE = "date";
	public static final String ORDER_BY_TITLE = "title";
	public static final String ORDER_BY_VIEWS = "views";

	public long getDelegationId() {
		return delegationId;
	}

	public void setDelegationId(long delegationId) {
		this.delegationId = delegationId;
	}

	public boolean isCheckArticle() {
		return checkArticle;
	}

	public void setCheckArticle(boolean checkArticle) {
		this.checkArticle = checkArticle;
	}

	public boolean isCheckPoll() {
		return checkPoll;
	}

	public void setCheckPoll(boolean checkPoll) {
		this.checkPoll = checkPoll;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<Long> getCategoriesIds() {
		return categoriesIds;
	}

	public void setCategoriesIds(List<Long> categoriesIds) {
		this.categoriesIds = categoriesIds;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public int getItemsPerPage() {
		return itemsPerPage;
	}

	public void setItemsPerPage(int itemsPerPage) {
		this.itemsPerPage = itemsPerPage;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public List<Long> getLayoutsPlid() {
		return layoutsPlid;
	}

	public void setLayoutsPlid(List<Long> layoutsPlid) {
		this.layoutsPlid = layoutsPlid;
	}

	public boolean isFuzzy() {
		return fuzzy;
	}

	public void setFuzzy(boolean fuzzy) {
		this.fuzzy = fuzzy;
	}

	public String getFilterquery()
	{
		return filterquery;
	}

	public void setFilterquery(String filterquery)
	{
		this.filterquery = filterquery;
	}

	public String getWildcard()
	{
		return wildcard;
	}

	public void setWildcard(String wildcard)
	{
		this.wildcard = wildcard;
	}
	
}
