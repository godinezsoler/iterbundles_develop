/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

import java.util.Comparator;

public class TrackingTypeComparator implements  Comparator<TrackingSearchObject> {

	public TrackingTypeComparator() {
		this(false);
	}

	public TrackingTypeComparator(boolean ascending) {
		_ascending = ascending;
	}

	public int compare(TrackingSearchObject tracking1, TrackingSearchObject tracking2) {

		int value = tracking1.getType().compareTo(tracking2.getType());

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
