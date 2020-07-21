package com.protecmedia.iter.user;

import com.liferay.portal.kernel.exception.PortalException;

public class LoginException extends PortalException {

	public LoginException() {
	}

	public LoginException(String msg) {
		super(msg);
	}

	public LoginException(Throwable cause) {
		super(cause);
	}

	public LoginException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
