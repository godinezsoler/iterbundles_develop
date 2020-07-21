package com.protecmedia.iter.news;

import com.liferay.portal.kernel.exception.PortalException;

public class PageContentExistsException extends PortalException {

	public PageContentExistsException() {
		super();
	}

	public PageContentExistsException(String msg) {
		super(msg);
	}

	public PageContentExistsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public PageContentExistsException(Throwable cause) {
		super(cause);
	}
	
}
