/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

import java.util.Comparator;

public class TrackingViewsComparator implements  Comparator<TrackingSearchObject> {

	public TrackingViewsComparator() {
		this(false);
	}

	public TrackingViewsComparator(boolean ascending) {
		_ascending = ascending;
	}

	public int compare(TrackingSearchObject tracking1, TrackingSearchObject tracking2) {

		Long l1 = new Long(tracking1.getViews());
		Long l2 = new Long(tracking2.getViews());
		
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
