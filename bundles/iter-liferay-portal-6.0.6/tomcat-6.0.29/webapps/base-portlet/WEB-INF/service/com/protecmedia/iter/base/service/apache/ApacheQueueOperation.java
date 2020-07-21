package com.protecmedia.iter.base.service.apache;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.WebKeys;
import com.protecmedia.iter.base.service.thread.ThreadTools;

public class ApacheQueueOperation extends Thread implements Comparable<ApacheQueueOperation>
{
	
	private static final Log _log = LogFactoryUtil.getLog(ApacheQueueOperation.class);
	
	
	private int 					_order				= 0;
	private String 					_operationURL 		= "";
	private String 					_apacheHost			= "";
	private int 					_apacheConnTimeout	= 0;
	private int 					_apacheReadTimeout	= 0;
	private String 					_parentId			= "";
	private final ApacheQueueMgr  	_apacheMgr;
	
	public ApacheQueueOperation(int apacheConnTimeout, int apacheReadTimeout, String apacheHost, String operationURL, int order, ApacheQueueMgr apacheMgr)
	{
		super();
		
		_apacheConnTimeout 	= apacheConnTimeout;
		_apacheReadTimeout 	= apacheReadTimeout;
		_apacheHost			= apacheHost;
		_operationURL		= operationURL;
		_order				= order;	
		_apacheMgr			= apacheMgr;
	}
	
	@Override
	public void run() 
	{
		HttpURLConnection httpConnection = null;
		try
		{
			String hostProperty = new URL(_operationURL).getHost();
			URL operationURL 	= new URL( _operationURL.replaceFirst(hostProperty, _apacheHost) );
						
	        httpConnection = (HttpURLConnection)operationURL.openConnection();
	        httpConnection.setConnectTimeout(_apacheConnTimeout);
	        httpConnection.setReadTimeout(_apacheReadTimeout);
	        
	        httpConnection.setRequestProperty (WebKeys.HOST, hostProperty);
	        httpConnection.setRequestProperty ("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS);
	        httpConnection.setRequestProperty ("Accept-Encoding", ApacheQueueMgr.ENCODING);
	        
			if (_log.isTraceEnabled())
				_log.trace( String.format("%s\n\t%s: %s", this.toString(), WebKeys.HOST, httpConnection.getRequestProperty(WebKeys.HOST)));
			
			
			int responseCode = 0;
			
	        httpConnection.connect();
	        responseCode = httpConnection.getResponseCode();

			// Se consume el buffer
			BufferedReader in = null;
			if (responseCode == HttpURLConnection.HTTP_OK)
				in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
	        else
	        	in = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));

			char[] charBuffer = new char[4096];
			while (in.read(charBuffer, 0, 4096) > 0);
			
			// La traza se muestra una vez consumido el buffer
			if (_log.isDebugEnabled())
			{
				if (responseCode == HttpURLConnection.HTTP_OK)
					_log.debug( this.toString()+ " cached");
				else
		        	_log.debug( this.toString()+ " not cached (" +httpConnection.getResponseMessage()+ ": " +responseCode+ ")");
			}
			
			// close the inputstream
			in.close();
		}
		catch (Exception e)
		{
			_log.error( this.toString() +"\n\t"+ e.toString() );
			
			if (_log.isTraceEnabled())
				_log.trace(e);
		}
		finally
		{
			try
			{
				if (httpConnection != null)
					httpConnection.disconnect();
			}
			catch(Throwable e2){}
			
			if (_apacheMgr != null)
				_apacheMgr.decrementActiveOperations();
		}
	}

	public int getOrder() 
	{
		return _order;
	}

	public void setOrder(int _order) 
	{
		this._order = _order;
	}

	@Override
	public int compareTo(ApacheQueueOperation o1) 
	{
		return o1.getOrder() - this.getOrder();
	}
	
	@Override
	public String toString()
	{
		return "TotalThreads: " + ThreadTools.getCurrentRunningThreads() + "\n\tParentThreadId: " + getParentId() + "\n\tThreadId: " + this.getId() + "\n\tOperOrder: " + this.getOrder() + "\n\tURLpte: " + _operationURL;
	}
	
	public synchronized void setParentId(String value)
	{
		_parentId = value;
	}
	
	public synchronized String getParentId()
	{
		return (_parentId != null && !_parentId.isEmpty()) ? _parentId : this.getName();
	}
}
