package com.protecmedia.iter.base.community.manager;

import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.exception.SystemException;

public interface CommunityManager
{
	public void authorize(HttpServletResponse response);
	public void grant(HttpServletResponse response, String code) throws SystemException;
	public void publish(String articleId, HashMap<String, String> params);
}
