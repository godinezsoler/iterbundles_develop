package com.protecmedia.iter.base.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.GroupFriendlyURLException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;

public class IterAdminTools implements Runnable
{
	private static Log _log = LogFactoryUtil.getLog(IterAdminTools.class);
	
	private Throwable _e = null;
	
	private long _scopeGroupId = 0;
	private String _friendlyURL = "";
	private String _publicVirtualHost = "";
	private String _serverAlias = "";
	private String _staticsServers = "";
	
	public IterAdminTools(long scopeGroupId, String friendlyURL, String publicVirtualHost, String serverAlias, String staticsServers)
	{
		this._scopeGroupId = scopeGroupId;
		this._friendlyURL = friendlyURL;
		this._publicVirtualHost = publicVirtualHost;
		this._serverAlias = serverAlias;
		this._staticsServers = staticsServers;
	}
	
	public void saveConfigIterAdmin() throws PortalException, SystemException
	{
		try
		{
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.submit( this );
			
			executorService.shutdown();
			
			// Se espera a que termine
			while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
			{
				_log.debug( "Waiting to save IterAdmin config" );
			}
			
			if (_e != null)
				throw _e;
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
	}

	@Override
	public void run()
	{
		try
		{
			try
			{
				GroupLocalServiceUtil.updateFriendlyURL(_scopeGroupId, _friendlyURL);
			}
			catch (GroupFriendlyURLException gfurle)
			{
				throw ErrorRaiser.buildError(com.liferay.portal.kernel.error.IterErrorKeys.XYZ_ITR_E_FRIENDLYURL_INCORRECT_ZYX, gfurle.toString(), gfurle.getStackTrace());
			}
			
			LayoutSetLocalServiceUtil.updateVirtualHost(_scopeGroupId, false, _publicVirtualHost);
			
			StringBuilder settings = new StringBuilder();
			settings.append(LayoutSetTools.SERVER_ALIAS).append(StringPool.EQUAL).append(_serverAlias.trim().replaceAll(StringPool.NEW_LINE, StringPool.SECTION)).append(StringPool.NEW_LINE);
			settings.append(LayoutSetTools.STATIC_SERVERS).append(StringPool.EQUAL).append(_staticsServers.trim().replaceAll(StringPool.NEW_LINE, StringPool.SECTION));
			
			LayoutSet ls = LayoutSetLocalServiceUtil.updateSettings(_scopeGroupId, false, settings.toString());
			LayoutSetLocalServiceUtil.updateLayoutSet(ls, true);
		}
		catch(Throwable th)
		{
			_e = th;
		}

	}

}
