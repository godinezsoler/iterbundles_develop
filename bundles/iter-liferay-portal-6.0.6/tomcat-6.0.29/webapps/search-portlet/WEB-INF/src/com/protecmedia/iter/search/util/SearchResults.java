/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.search.util;

import com.liferay.portal.kernel.search.Hits;

public class SearchResults{

	private int total;
	
	private Hits results;

	public SearchResults() {
		super();
		this.total = 0;
		this.results = null;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Hits getResults() {
		return results;
	}

	public void setResults(Hits results) {
		this.results = results;
	}
}