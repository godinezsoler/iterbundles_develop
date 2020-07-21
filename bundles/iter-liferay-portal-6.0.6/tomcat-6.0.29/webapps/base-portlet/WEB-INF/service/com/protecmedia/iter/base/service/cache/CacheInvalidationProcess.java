package com.protecmedia.iter.base.service.cache;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;

public class CacheInvalidationProcess implements Runnable
{
	private static final Log _log = LogFactoryUtil.getLog(CacheInvalidationProcess.class);
	
	static private final String GET_NEXT_INVALIDATION = "CALL ITR_CACHE_GET_NEXTINVALIDATION(%d)";
	
	/** Lock held by wait4NextInvalidation */
	private final ReentrantLock wait4NextInvalidationLock = new ReentrantLock();
	
    /** Wait queue for waiting wait4Empty */
    private final Condition nextInvalidation = wait4NextInvalidationLock.newCondition();
    
    private String 		_nextInvalidation 		= null;
    private String[]	_groupsToInvalidate		= null;

	@Override
	public void run()
	{
		_log.info("Starting the Cache Invalidation mechanism");
		while (_isEnable())
		{
			try
			{
				reschedule();
				wait4NextInvalidation();
				invalidateCache();
			}
			catch (InterruptedException ie)
			{
				Thread.currentThread().interrupt();
				_log.trace(ie.toString());
			}
			catch (Throwable th)
			{
				_log.error(th);
			}
		}
		_log.info("The Cache Invalidation mechanism has been finished");
	}
	
	public void reschedule() throws SecurityException, NoSuchMethodException, IOException, SQLException
	{
		_log.info("Reschedule: Begin");
		
		final ReentrantLock wait4NextInvalidationLock = this.wait4NextInvalidationLock;
		wait4NextInvalidationLock.lock();
		
		try
		{
			// Se obtienen las invalidaciones, la primera indicará la hora de la siguiente invalidación, pero se obtienen todos los registros para obtner todos los grupos a invalidar
			String sql = String.format(GET_NEXT_INVALIDATION, PropsValues.ITER_CACHE_AUTOINVALIDATE_THRESHOLD_TIME);
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			_nextInvalidation	= XMLHelper.getStringValueOf(dom, "/rs/row[1]/@nextInvalidation");
			_groupsToInvalidate = (Validator.isNotNull(_nextInvalidation)) ? XMLHelper.getStringValues(dom.selectNodes("/rs/row/@groupId")) : null;
			
			if (_log.isDebugEnabled())
			{
				_log.debug( String.format("Reschedule: Next invalidation(%s)\t\tGroups to invalidate(%s)", 
							GetterUtil.getString2(_nextInvalidation, " "), 
							Validator.isNull(_groupsToInvalidate) ? " " : StringUtils.join(_groupsToInvalidate)) );
			}
			
			nextInvalidation.signal();
		}
		finally
		{
			wait4NextInvalidationLock.unlock();
			_log.info("Reschedule: End");
		}
	}
	
	/**
	 * 
	 * @param date
	 * @return  -1: si es necesario esperar indefinidamente<br/>
	 * 			 0: si NO es necesario esperar.
	 * 			>0: tiempo en milisegundos que es necesaro esperar
	 * @throws ParseException 
	 */
	private static long calcDelay(String date)
	{
		long delay = -1;
		
		if (Validator.isNotNull(date))
		{
			try
			{
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				
				delay = Math.max(0, df.parse(date).getTime() - (new Date()).getTime());
			}
			catch (ParseException pe)
			{
				_log.error(pe);
				
				// Si hay un fallo es preferible que se invalide la caché en el momento, a que no se haga
				delay = 0;
			}
		}
		return delay;
	}
	
	/**
	 * Método que espera hasta la fecha de próxima invalidación. Si dicha fecha ya ha pasado deja de esperar.<br/> 
	 * Si no hay fecha configurada espera indefinidamente hasta que se llame externamente a <code>reschedule</code>.
	 *
	 * @throws InterruptedException
	 */
	private void wait4NextInvalidation() throws InterruptedException
	{
		final ReentrantLock wait4NextInvalidationLock = this.wait4NextInvalidationLock;
		wait4NextInvalidationLock.lockInterruptibly();
		
		try
		{
			long delay = 0;
			try 
			{
				while (Validator.isNull(_groupsToInvalidate) || (delay = calcDelay(_nextInvalidation)) != 0)
				{
					if (Validator.isNull(_groupsToInvalidate) || delay < 0)
					{
						_log.debug("Waiting undefinilly");
						nextInvalidation.await();
					}
					else
					{
						_log.debug( String.format("Waiting %d milliseconds", delay) );
						nextInvalidation.await(delay, TimeUnit.MILLISECONDS);
					}
				}
			} 
			catch (InterruptedException ie) 
			{
				nextInvalidation.signal(); // propagate to a non-interrupted thread
				throw ie;
			}
			
			_log.info("The Cache Invalidation mechanism has waked up");
			
			if (Validator.isNotNull(_groupsToInvalidate) && delay == 0)
				nextInvalidation.signal();
		}
		finally 
		{
			wait4NextInvalidationLock.unlock();
        }
	}
	
	/**
	 * Realiza la invalidación de la caché de los TPUs correspondientes
	 * @throws Exception 
	 */
	private void invalidateCache() throws Exception
	{
		_log.info("InvalidateCache: Begin");
		
		final ReentrantLock wait4NextInvalidationLock = this.wait4NextInvalidationLock;
		wait4NextInvalidationLock.lock();
		
		try
		{
			Element root = SAXReaderUtil.read("<groups/>").getRootElement();
			for (String groupId : _groupsToInvalidate)
			{
				long group_id = Long.valueOf(groupId);
				
				Element group = root.addElement("group");
				group.addAttribute("groupId",		groupId);
				group.addAttribute("lastUpdate", 	String.valueOf(GroupMgr.getPublicationDate(group_id).getTime()));
				group.addAttribute("groupName",		GroupLocalServiceUtil.getGroup(group_id).getName());
				
				// Se actualiza la fecha de última publicación
				TomcatUtil.updatePublicationDateNoException(GroupMgr.getCompanyId(), group_id);
			}
			
			CacheRefresh cacheRefresh = new CacheRefresh(0, root);
			// El proceso de planificación no puede relanzar otra replanificación
			cacheRefresh.setRescheduleCacheInvalidation(false);
			
			ChannelLocalServiceUtil.refreshCache(cacheRefresh);
		}
		finally
		{
			wait4NextInvalidationLock.unlock();
			_log.info("InvalidateCache: End");
		}
	}
	
	private boolean _isEnable()
	{
		return 	Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted() &&
				Heartbeat.canLaunchProcesses();
	}

}
