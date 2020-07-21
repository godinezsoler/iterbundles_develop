package com.protecmedia.iter.base.service.util;

import java.io.File;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.util.PortalUtil;

public class HotConfigUtil {

	private static Log _log = LogFactoryUtil.getLog(HotConfigUtil.class);
	private static final String configFile = File.separatorChar + "hotconfiguration.xml"; 
	
	private static ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	
	private static Document _dom = null;
	
	public static void init() throws DocumentException
	{
		loadFile();
	}
	
	public static String getKey(String keyName)
	{
		String val = null;
		Document dom = null;

		try {
			dom = loadFile( configFile );
			
			XPath xpath = SAXReaderUtil.createXPath("/hotconfiguration/key[@name='" + keyName + "']");
			Node nod = xpath.selectSingleNode(dom);
			if(nod!=null)
				val = nod.getText();
			
		} catch (DocumentException e) {
			_log.error(e.toString());
		}
		
		return val;
	}
	
	private static String _getKey(String keyName, boolean reloadConfig)
	{
		String val = null;
		try 
		{
			if (reloadConfig)
				loadFile();
			
			_lock.readLock().lock();
			try
			{
				val = XMLHelper.getStringValueOf(_dom, String.format("/hotconfiguration/key[@name='%s']", keyName));
			}
			finally
			{
				_lock.readLock().unlock();
			}
		} 
		catch (DocumentException e) 
		{
			_log.error(e.toString());
		}
		
		return val;
	}
	
	public static long getKey(String keyName, long defaultValue)
	{
		return GetterUtil.get(getKey(keyName), defaultValue);
	}
	
	public static boolean getKey(String keyName, boolean defaultValue)
	{
		return GetterUtil.get(getKey(keyName), defaultValue);
	}
	
	public static String getKey(String keyName, String defaultValue)
	{
		return GetterUtil.get(getKey(keyName), defaultValue);
	}

	public static String getKey(String keyName, String defaultValue, boolean reloadConfig)
	{
		return GetterUtil.get(_getKey(keyName, reloadConfig), defaultValue);
	}
	
	private static void loadFile() throws DocumentException
	{
		_lock.writeLock().lock();
		try
		{
			_dom = loadFile( configFile );
		}
		finally
		{
			_lock.writeLock().unlock();
		}
	}

	private static Document loadFile(String path) throws DocumentException
	{
		File webappsFile = new File( PortalUtil.getPortalWebDir() ).getParentFile().getParentFile().getParentFile();
		File hotConfigfile = new File( webappsFile.getAbsolutePath() + path );
		
		return SAXReaderUtil.read( hotConfigfile );
	}

    public static void wait4Properties(String propertyName) throws InterruptedException
    {
    	while (!HotConfigUtil.getKey(propertyName, false))
    	{
    		Thread.sleep(1000);
    	}
    }
}
