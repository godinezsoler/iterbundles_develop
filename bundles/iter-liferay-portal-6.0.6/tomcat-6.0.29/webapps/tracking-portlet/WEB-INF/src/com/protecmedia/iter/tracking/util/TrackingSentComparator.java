/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

import java.util.Comparator;

public class TrackingSentComparator implements  Comparator<TrackingSearchObject> {

	public TrackingSentComparator() {
		this(false);
	}

	public TrackingSentComparator(boolean ascending) {
		_ascending = ascending;
	}

	public int compare(TrackingSearchObject tracking1, TrackingSearchObject tracking2) {

		Long l1 = new Long(tracking1.getSent());
		Long l2 = new Long(tracking2.getSent());
		
		int value = l1.compareTo(l2);

		if (_ascending) {
			return value;
		}
		else {
			return -value;
		}
	}

	public boolean isAscending() {
		return _ascending;
	}

	private boolean _ascending;

}
