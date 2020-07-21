package com.protecmedia.iter.base.service.util;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.util.PortalUtil;

public class CKEditorUtil
{
	public static void noInheritThemeCSS(String html, HttpServletRequest request)
	{
		if (Validator.isNotNull(html) && html.contains(IterKeys.CKEDITOR_NO_INHERIT_THEME_CSS))
		{
			PublicIterParams.set( PortalUtil.getOriginalServletRequest(request), WebKeys.REQUEST_ATTRIBUTE_NO_INHERIT_THEME_CSS, true ); 
		}
	}

}
