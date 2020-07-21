package com.protecmedia.iter.search.util;

import java.util.Locale;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ArrayUtil;

public class CalendarUtil {
	
	public static String getJQueryLanguageId(Locale locale){
		
		Locale[] availableLocales = LanguageUtil.getAvailableLocales();
		
		if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) || !ArrayUtil.contains(availableLocales, locale)){
			return "";
		}

		return locale.getLanguage();
		
	}

}
