/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

import java.util.Comparator;

public class TrackingModerationComparator implements  Comparator<TrackingSearchObject> {

	public TrackingModerationComparator() {
		this(false);
	}

	public TrackingModerationComparator(boolean ascending) {
		_ascending = ascending;
	}

	public int compare(TrackingSearchObject tracking1, TrackingSearchObject tracking2) {

		Boolean l1 = new Boolean(tracking1.isModeration());
		Boolean l2 = new Boolean(tracking2.isModeration());
		
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
