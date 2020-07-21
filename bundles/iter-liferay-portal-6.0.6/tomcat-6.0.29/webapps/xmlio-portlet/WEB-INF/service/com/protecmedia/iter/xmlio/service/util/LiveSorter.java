package com.protecmedia.iter.xmlio.service.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;

/**
 * Clase que permite la ordenación de una lista de Live (beans) 
 * @author michel
 *
 */
public class LiveSorter implements Comparator<Live>
{
	private static final String ORDER_BY_LAYOUT_DEEP = String.format(new StringBuilder(
		"SELECT XmlIO_Live.id_, ITR_GET_LAYOUT_DEEP(plid) pos												\n").append(
		"FROM Layout																						\n").append(
		"INNER JOIN XmlIO_Live ON (Layout.plid = XmlIO_Live.localId AND XmlIO_Live.classNameValue = '%s')	\n").append(
		"	WHERE XmlIO_Live.id_ IN (%%s)																	\n").append(
		"	ORDER BY pos ASC																				\n").toString(), IterKeys.CLASSNAME_LAYOUT);

	static public enum LiveSorterCriterion
	{
		LAYOUT_DEEP
	}
	
	private static String XPATH_ORDER 	= "/rs/row[@id_='%d']/@pos";
	private Document _orderDom 			= null;

	public LiveSorter(Document orderDom)
	{
		_orderDom = orderDom;
	}
	
	static public void sort(List<Live> list, LiveSorterCriterion orderBy) throws SecurityException, NoSuchMethodException
	{
		// Si es null o una lista vacía, se devuelve tal cual
		if (Validator.isNotNull(list))
		{
			switch(orderBy)
			{
			case LAYOUT_DEEP:
				StringBuilder ids = new StringBuilder();
				for (Live live : list)
					ids.append(live.getId()).append(",");
				
				ids.deleteCharAt(ids.length()-1);
				
				Document orderDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(ORDER_BY_LAYOUT_DEEP, ids.toString()) );
				Collections.sort(list, new LiveSorter(orderDom));
				break;
			}
		}
	}
	
	@Override
	public int compare(Live live1, Live live2)
	{
		long order1 = XMLHelper.getLongValueOf( _orderDom, String.format(XPATH_ORDER, live1.getId()) );
		long order2 = XMLHelper.getLongValueOf( _orderDom, String.format(XPATH_ORDER, live2.getId()) );
		
		return order1 < order2 ? -1 : (order1 > order2 ? 1 : 0);
	}
}
