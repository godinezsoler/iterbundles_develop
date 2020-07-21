/**
 * 
 */
package com.protecmedia.iter.xmlio.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;

/**
 * @author luism
 *
 */
public class ImportLegacyurlTool implements Runnable
{
	private static Log _log = LogFactoryUtil.getLog(ImportLegacyurlTool.class);
	
	private long _scopeGroupId = 0L;
	private String _line;
	private int _lineNumber;
	private boolean _encodeLegacyUrl = false;
	private boolean _fail = true;
	private String	_articleId;
	private String	_legacyurl;
	private Throwable _e = null;
	
	private String ON_DUPLICATE_KEY = " ON DUPLICATE KEY UPDATE url=VALUES(url), articleid=VALUES(articleid)";
	private String ADD_LEGACYURL_ERROR = "INSERT INTO legacyurlerrors(groupid, articleid, url, errorcode, errordetail) VALUES (%s, '%s', '%s', '%s', '%s')";
	private String DELETE_LEGACYURL_ERRORS = "DELETE FROM legacyurlerrors WHERE groupid=%s";

	public ImportLegacyurlTool(long scopeGroupId, boolean encodeLegacyUrl, String exists)
	{
		this._scopeGroupId = scopeGroupId;
		this._encodeLegacyUrl = encodeLegacyUrl;
		this._fail = exists.equalsIgnoreCase("fail");
	}
	
	public void setLine(String line, int lineNumber)
	{
		this._line = line;
		this._lineNumber = lineNumber;
	}

	@Override
	public void run()
	{
		try
		{
			doImport();
		}
		catch (Throwable e)
		{
			this._e = e;
		}
	}
	
	public void deleteOldErrors()
	{
		String query = String.format(DELETE_LEGACYURL_ERRORS, _scopeGroupId);
		
		if(_log.isDebugEnabled())
			_log.debug("DELETE_LEGACYURL_ERRORS: " + query);
		
		try
		{
			PortalLocalServiceUtil.executeUpdateComittedQuery( query );
		}
		catch (Exception e)
		{
			_log.error(e);
		}
	}

	public void importLegacyUrl() throws SystemException, PortalException
	{
		try
		{
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.submit( this );
			
			executorService.shutdown();
			
			// Se espera a que termine
			while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
			{
				_log.debug( "Waiting to import legacyurl" );
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
	
	private void doImport() throws UnsupportedEncodingException, ServiceError
	{
		String[] data = null;
		
		if(_line.contains(StringPool.SEMICOLON))
			data = _line.split(StringPool.SEMICOLON);
		else
			if(_line.contains(StringPool.COMMA))
				data = _line.split(StringPool.COMMA);
		
		if( data!=null && data.length==2 )
		{
			_articleId = StringUtil.trim( StringUtil.unquote( StringUtil.trim( data[0] ) ) );
			_legacyurl = StringUtil.trim( StringUtil.unquote( StringUtil.trim( data[1] ) ) );
			
			if(_log.isDebugEnabled())
				_log.debug(String.format("Importing url: %s with articleid: %s", _legacyurl, _articleId));
			
			if( !_legacyurl.startsWith(StringPool.SLASH) || _legacyurl.endsWith(StringPool.SLASH)  || _legacyurl.contains(StringPool.POUND))
				insertLegacyErrors(IterErrorKeys.XYZ_BADURL_ZYX, _line);
			else
			{
				if(_encodeLegacyUrl)
				{
					String urlArray[] = _legacyurl.split("/");
					StringBuilder sb = new StringBuilder();
	                for(int i=1; i < urlArray.length; i++)
	                	sb.append(StringPool.SLASH).append( URLEncoder.encode(urlArray[i], IterKeys.UTF8) );
	                
	                _legacyurl = sb.toString();
				}
				
				try
				{
					insertLegacyUrl();
				}
				catch (ORMException orme)
				{
					_log.error(orme);
					insertLegacyErrors(ServiceErrorUtil.getErrorCode(orme), ServiceErrorUtil.getServiceErrorAsXml(orme));
				}
				catch (Exception e)
				{
					_log.error(e);
					insertLegacyErrors(e instanceof NoSuchArticleException ? "XYZ_ITR_FK_LEGACYURL_ARTICLEID_ZYX" : IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, ServiceErrorUtil.getServiceErrorAsXml(e));
				}
			}
		}
		else
		{
			_articleId = StringPool.BLANK;
			_legacyurl = String.valueOf(_lineNumber);
			insertLegacyErrors(IterErrorKeys.XYZ_E_INVALID_LEGACY_ROW_ZYX, _line);
		}
	}

	private void insertLegacyUrl() throws IOException, SQLException, PortalException, SystemException
	{
		// Obtiene los grupos en los que está asignado el artículo
		List<Long> groups = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), _articleId).getScopeGroupIds();
		
		// Crea la query para insertar la URL en todos los grupos
		List<String> values = new ArrayList<String>();
		for (long groupId : groups)
		{
			values.add(String.format(SQLQueries.INSERT_LEGACYURL_VALUES, _legacyurl.toLowerCase().replaceAll("'", "''"), groupId, _articleId));
		}
		String sql = new StringBuilder(SQLQueries.INSERT_LEGACYURL).append(StringUtil.merge(values)).toString();
		
		if(!_fail)
			sql = sql.concat(ON_DUPLICATE_KEY);
		
		if(_log.isDebugEnabled())
			_log.debug("INSERT_LEGACYURL: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	private void insertLegacyErrors(String errorCode, String errorDetail)
	{
		//legacyurlerrors(groupid, articleid, url, errorcode, errordetail)
		String query = String.format(ADD_LEGACYURL_ERROR, _scopeGroupId, _articleId, _legacyurl, errorCode, StringEscapeUtils.escapeSql(errorDetail));
		
		try
		{
			if(_log.isDebugEnabled())
				_log.debug("ADD_LEGACYURL_ERROR: " + query);
			
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		catch (Exception e)
		{
			_log.error(e);
		}
	}
}
