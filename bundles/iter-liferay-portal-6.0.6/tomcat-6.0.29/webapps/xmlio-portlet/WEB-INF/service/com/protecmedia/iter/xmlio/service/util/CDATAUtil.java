package com.protecmedia.iter.xmlio.service.util;

import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Alexander Chow
 */
public class CDATAUtil {

	public static String wrap(String s) 
	{
		return new StringBuilder(StringPool.CDATA_OPEN).append(s).append(StringPool.CDATA_CLOSE).toString();
	}

	public static String strip(String s) 
	{
		String retVal;

		if (Validator.isNotNull(s) && s.startsWith(StringPool.CDATA_OPEN) && s.endsWith(StringPool.CDATA_CLOSE)) 
		{
			retVal = s.substring( StringPool.CDATA_OPEN.length(), s.length() - StringPool.CDATA_CLOSE.length() );
		}
		else 
		{
			retVal = s;
		}

		return retVal;
	}

}
