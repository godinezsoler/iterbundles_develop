package com.protecmedia.iter.base.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil;

public class NewsletterTools implements Callable<Boolean>
{
	private static Log _log = LogFactoryUtil.getLog(NewsletterTools.class);
	private Throwable _e = null;
	
	private StringBuffer _body;
	private String _newsletterId;

	public NewsletterTools(String newsletterId, StringBuffer body)
	{
		this._body = body;
		this._newsletterId = newsletterId;
	}
	
	@Override
	public Boolean call()
	{
		Boolean send = Boolean.FALSE;
		
		try
		{
			send = NewsletterMgrLocalServiceUtil.checkContentBeforeSend(_newsletterId, _body);
		}
		catch(Throwable e)
		{
			_e = e;
		}
		
		return send;
	}
	
	public static boolean checkNewsletterContent(String newsletterId, StringBuffer body) throws PortalException, SystemException
	{
		NewsletterTools nlt = new NewsletterTools(newsletterId, body);
		return nlt.ExecutorService_CheckNewsletterContent();
	}

	private boolean ExecutorService_CheckNewsletterContent() throws PortalException, SystemException
	{
		Boolean send = false;
		try
		{
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			Future<Boolean> futureSend = executorService.submit(this);
			
			executorService.shutdown();
			
			// Se espera a que termine
			while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
			{
				_log.debug( "Waiting for check md5 body" );
			}
			
			if (_e != null)
				throw _e;
			
			send = futureSend.get();
			
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (Throwable th)
		{
			// Las excepciones que NO sean PortalException ni SystemException, o los errores, se lanzan como SystemException
			throw new SystemException(th);
		}
		
		return send;
	}
}
