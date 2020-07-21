package com.protecmedia.iter.xmlio.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class LiveListUtil {
	
	private static Log _log = LogFactoryUtil.getLog(XMLIOUtil.class);
	
	@SuppressWarnings("unchecked")
	public static void orderList(List lista, final String property, final boolean asc) {  
		  Collections.sort(lista, new Comparator()  {  
		     
		   public int compare(Object obj1, Object obj2) {  
			
			   	int value = 0;
				Class clase = obj1.getClass();  
				String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);  
				try {
					Method getproperty = clase.getMethod(getter);  
					   
					Object property1 = getproperty.invoke(obj1);  
					Object property2 = getproperty.invoke(obj2);  
					  
					if(property1 instanceof Comparable && property2 instanceof Comparable) {
						Comparable prop1 = (Comparable)property1;  
						Comparable prop2 = (Comparable)property2;  
						value = prop1.compareTo(prop2);  
					}//CASO DE QUE NO SEA COMPARABLE  
						else {  
							if(property1.equals(property2))  
								value = 0;  
							else  
								value = 1;  
					  
						}  
					}  
					catch(Exception e) {  
					   _log.error(e);
					}  
					
					if(asc)
						return value;

					return -value;
			   }
			}
		);
	}   

}
