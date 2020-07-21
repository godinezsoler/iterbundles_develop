package com.protecmedia.iter.base.service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class IterWebCmsVersionUtil
{

	private static Log _log = LogFactoryUtil.getLog(IterWebCmsVersionUtil.class);
	private static ComparatorIterWebCmsVersion  comparatorIterWebCmsVersion = new ComparatorIterWebCmsVersion();
			
	
	public static List<Object> orderIterWebCmsVersion(List<Object> versions){
		List<Object> localVersions = new ArrayList<Object>(versions);
		Collections.sort(localVersions, comparatorIterWebCmsVersion);
		return localVersions;
	}
	
	protected static class ComparatorIterWebCmsVersion implements Comparator<Object>{

		@Override
		public int compare(Object v0, Object v1) {
			int result;
			String v0String = v0.toString();
			String v1String = v1.toString();
			List<String> subVersions0 = Arrays.asList(v0String.split("\\."));
			List<String> subVersions1 = Arrays.asList(v1String.split("\\."));
			for (int i = 0; i < subVersions0.size() && i < subVersions1.size();i++) {
				if((result = subVersions0.get(i).compareTo(subVersions1.get(i))) != 0)
					return -result;
			}
			if((result = subVersions0.size() - subVersions1.size()) != 0)
				return -result;
			
			return 0;
		}
		
	}
	
	
}
