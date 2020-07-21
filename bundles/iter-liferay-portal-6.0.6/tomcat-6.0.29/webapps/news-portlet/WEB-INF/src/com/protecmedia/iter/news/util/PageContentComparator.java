package com.protecmedia.iter.news.util;

import com.liferay.portal.kernel.util.OrderByComparator;
import com.protecmedia.iter.news.model.PageContent;

public class PageContentComparator extends OrderByComparator{
	
	public static String ORDER_BY_ASC = "vigenciadesde ASC";

	public static String ORDER_BY_DESC = "vigenciadesde DESC";

	public PageContentComparator() {
		this(false);
	}

	public PageContentComparator(boolean asc) {
		_asc = asc;
	}

	public int compare(Object obj1, Object obj2) {
		PageContent pc1 = (PageContent) obj1;
		PageContent pc2 = (PageContent) obj2;
		
		int value = pc1.getVigenciadesde().compareTo(pc2.getVigenciadesde());
	
		if (_asc) {
			return value;
		} else {
			return -value;
		}
	}

	public String getOrderBy() {
	if (_asc) {
		return ORDER_BY_ASC;
	} else {
		return ORDER_BY_DESC;
		}
	}

	private boolean _asc;

}
