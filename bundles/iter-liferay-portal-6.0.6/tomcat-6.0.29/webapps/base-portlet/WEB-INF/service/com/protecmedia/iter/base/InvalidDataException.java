package com.protecmedia.iter.base;

import com.liferay.portal.kernel.exception.PortalException;

public class InvalidDataException extends PortalException {

	public InvalidDataException() {
		super();
	}

	public InvalidDataException(String msg) {
		super(msg);
	}

	public InvalidDataException(Throwable cause) {
		super(cause);
	}

	public InvalidDataException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
