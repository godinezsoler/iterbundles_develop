package com.protecmedia.iter.base.service.apache;

import java.net.MalformedURLException;
import java.net.URL;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.sectionservers.SectionServersMgr;
import com.liferay.portal.util.PortalUtil;

public class URIPte implements Comparable<URIPte>
{
	private static Log _log = LogFactoryUtil.getLog(URIPte.class);
	
	static public final String ITER_PROTOCOL 		= "ittp://";
	
	private String 	_apacheHost		= StringPool.BLANK;
	private String 	_url			= StringPool.BLANK;
	private String 	_originalURL	= StringPool.BLANK;
	private String  _host  			= StringPool.BLANK;
	private int 	_order			= 0;
	private long	_groupId 		= 0;
	private boolean _isIterURIPte 	= false;
	
	
	public URIPte(String apacheHost, String url, int order) throws MalformedURLException, PortalException, SystemException
	{
		_apacheHost 	= apacheHost;
		_originalURL	= url;
		_url 			= SectionServersMgr.processMobileURL( _originalURL.replaceFirst(ITER_PROTOCOL, "http://") );
		setOrder(order);
		_isIterURIPte 	= _originalURL.startsWith(ITER_PROTOCOL);
		
		_host 			= new URL(getURL()).getHost();
		_groupId 		= PortalUtil.getScopeGroupId(getHost(), getURL());
	}
	
	public long getGroupId()
	{
		return _groupId;
	}
	
	public String getHost()
	{
		return _host;
	}
	
	public String getOriginalURL()
	{
		return _originalURL;
	}
	
	public int getOrder()
	{
		return _order;
	}
	
	private void setOrder(int value)
	{
		_order = value;
	}
	
	public boolean isIterURIPte()
	{
		return _isIterURIPte;
	}
	public String getURL()
	{
		return _url;
	}
	public String getApacheHost()
	{
		return _apacheHost;
	}

	@Override
	public int compareTo(URIPte o1) 
	{
		int sort = o1.getOrder() - this.getOrder();
		
		// Si tienen el mismo peso se ordena por URL
		if (sort == 0)
			sort = this.getURL().compareToIgnoreCase(o1.getURL());

		return sort;
	}
	
	@Override
	public boolean equals(Object o)
	{
		boolean isEquals = (o == this);
		
		// Si no son el mismo objeto pero sí de la misma clase
		if (!isEquals && (o instanceof URIPte))
		{
			URIPte o1 = (URIPte)o;
			// Se comparan las URLs y no las OriginalURLs porque la URL es la que realmente se utiliza para llamar al Apache,
			// y al crear las URIPts a partir de artículos (ITER-1139) se utiliza el Host de la URL, y así se añaden tanto las 
			// versiones móviles como las clásicas de las secciones del artículo. 
			// Las móviles las añadirá la URL móvil del detalle, y las clásicas la URL clásica de dicho detalle.
			if ( (isEquals = this.getURL().equals(o1.getURL())) )
			{
				// Si tienen la misma URL original es la misma URIPte, e igualan el orden al mayor
				int newOrder = Math.max(this.getOrder(), o1.getOrder());
				
				this.setOrder( newOrder );
				o1.setOrder( newOrder );
			}
		}
		
		return isEquals;
	}
	
	@Override
	public String toString()
	{
		String str = String.format("%s (groupId=%d|ittp=%b|order=%d)", getURL(), getGroupId(), isIterURIPte(), getOrder());
		
		if (_log.isTraceEnabled())
			str = String.format("%s\n%s", str, getOriginalURL());
		
		return str;
	}
	
	@Override
	/**
	 * Se devuelve el mismo hashCode para forzar la comparación mediante equals
	 * @see https://www.arquitecturajava.com/java-override-hashcode-y-curiosidades/
	 * @see Idea from effective Java : Item 9
	 */
	public int hashCode() 
	{
		int result = 17;
		result = 31 * result + this.getURL().hashCode();
		
	    return result;
	}

}
