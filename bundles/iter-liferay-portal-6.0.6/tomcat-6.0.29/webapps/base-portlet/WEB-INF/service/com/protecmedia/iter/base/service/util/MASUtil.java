package com.protecmedia.iter.base.service.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.xmlio.NoSuchLiveException;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

public class MASUtil extends Thread 
{
	private static Log _log = LogFactoryUtil.getLog(MASUtil.class);

	private List<String> _masSecrets = null;
	private String[] _vocabularies = null;
	
	private boolean _globalIds = false;
	private String _url = GetterUtil.getString(PropsValues.ITER_PROTECMOBILEMAS_NOTIFYVOCABULARYCHANGE_URL, "http://mas.protecmobile.es/notifyvocabularychange");
	
	/**
	 * Inicialización del proceso de notificación de catalogaciones modificadas a MAS.
	 * 
	 * @param vocabularies	Id del grupo y lista de vocabularios modificados. El formato debe ser:
	 * 						<pre>scopeGroupId:vocabularyId1,vocabularyId2,...,vocabularyIdN</pre>
	 * @param globalIds		Booleano que indica si los IDs de los vocabularios son globales o locales al
	 * 						entorno LIVE. Si es verdadero, se realiza la conversión del globalId al vocabularyId del LIVE.
	 */
	public MASUtil(String vocabularies, boolean globalIds)
	{
		// Si el proceso no está desactivado en el portal-ext.properties y llegan vocabularios
		if (Validator.isNotNull(vocabularies) && !"disabled".equals(_url))
		{
			_log.debug("Initializing MAS vocabularies notifier...");
			
			_globalIds = globalIds;
			
			// Separa el grupo que viene en primer lugar
			String[] aux = vocabularies.split(StringPool.COLON);
			
			// Comprueba si están habilitadas las métricas de MAS
			_masSecrets = getMetricsEnabled(aux[0]);
			if (Validator.isNotNull(_masSecrets))
			{
				_vocabularies = aux[1].split(StringPool.COMMA);
				_log.debug("MAS vocabularies notifier initialized.");
			}
			else
			{
				_log.debug("No MAS metrics enabled. Skipping Process.");
			}
		}
	}
	
	public void run() 
	{
		if (Validator.isNotNull(_masSecrets) && Validator.isNotNull(_vocabularies))
		{
			_log.info("Sending vocabularies changes to MAS...");
			
			// Si es un globalId, lo traduce al ID del Live
			translateGlobalIds();
				
			// Puebla la caché llamando al servlet a través del Apache con el user-agent *ITERWEBCMS*FULL*
			populateCache();
						
			// Manda la notificación a los servidores de MAS
			notifyToMAS();
			
			_log.info("Notify vocabularies to MAS completed.");
		}
		else
		{
			_log.debug("No vocabularies to notify.");
		}
	}

	/**
	 * Convierte los IDs globales almacenados en el parámetro _vocabularies al vocabularyID del LIVE.
	 */
	private void translateGlobalIds()
	{
		if (_globalIds)
		{
			for (int i = _vocabularies.length-1; i >= 0; --i)
			{
				try
				{
					Live liveVocabulary = LiveLocalServiceUtil.getLiveByGlobalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_VOCABULARY, _vocabularies[i]);
					if (liveVocabulary != null)
					{
						_vocabularies[i] = liveVocabulary.getLocalId();
					}	
				}
				catch (NoSuchLiveException e)
				{
					_log.error(e);
				}
				catch (SystemException e)
				{
					_log.error(e);
				}
			}
		}
	}
	
	/**
	 * Para cada vocabulario modificado, llama al servlet getVocabulary en todos los apaches
	 * con el user-agent *ITERWEBCMS*FULL para poblar la caché antes de notificar a MAS.
	 */
	private void populateCache()
	{
		_log.debug("Populating categories hierarchy caché...");
		
		try
		{
			String[] masterList = ApacheHierarchy.getInstance().getMasterList();
			String html = StringPool.BLANK;
			 
			for (int i = _vocabularies.length-1; i >= 0; --i)
			{
				for (String apacheUrl : masterList)
				{
					_log.debug("Populating " + apacheUrl + " caché for vocabularyId " + _vocabularies[i]);
					
					// Construye la URL para llamar al servlet getVocabulary
					StringBuilder url = new StringBuilder().append(apacheUrl).append("/news-portlet/getVocabulary/").append(_vocabularies[i]);
					
					// Petición del vocabulario con el user-agent *ITERWEBCMS*FULL*
					HttpURLConnection httpConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
			        httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
			        httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
			       	httpConnection.setRequestMethod("GET");
			        httpConnection.addRequestProperty("User-Agent", "*ITERWEBCMS*FULL");
			        httpConnection.connect();
			        HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_GET_PAGECONTENT_ZYX);
			        html = StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
			        _log.trace("Apache response:");
			        _log.trace(html);
				}
			}
		}
		catch (IOException e)
		{
			_log.error(e);
		}
		catch (ServiceError e)
		{
			_log.error(e);
		}
	}
	
	/**
	 * Notifica mediante un POST los IDs de los vocabularios del LIVE que han sufrido modificaciones.
	 */
	private void notifyToMAS()
	{
		String postData = StringUtil.merge(_vocabularies);

		_log.debug("Notyfying Vocabularies <" + postData + "> to MAS at URL: " + _url);
		
		// Añade los MAS Secrets a la URL
		for(String masSecret : _masSecrets)
		{
			_url = new StringBuilder(_url).append(StringPool.SLASH).append(masSecret).toString();
		}
		
		try
		{
			// http://jira.protecmedia.com:8080/browse/ITER-410?focusedCommentId=16926&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-16926
			// Pedro respondió a esta nota con que SIEMPRE sea HTTP
			String url = HttpUtil.forceProtocol(_url, Http.HTTP_WITH_SLASH);
					
			// Inicializa la conexión
			HttpURLConnection httpConnection = (HttpURLConnection)(new URL(url).openConnection());
			httpConnection.setConnectTimeout(5000);
			httpConnection.setReadTimeout(5000);
			httpConnection.setRequestMethod("POST");
			// Establece las propiedades
			httpConnection.addRequestProperty("Content-Type", "text/plain");
			httpConnection.addRequestProperty("Content-Length", String.valueOf(postData.length()));
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			// Carga el PayLoad
			DataOutputStream outputStream = new DataOutputStream(httpConnection.getOutputStream());
	        outputStream.writeBytes(postData.toString());
	        outputStream.flush();
	        outputStream.close();
	        // Realiza el POST
	        httpConnection.connect();
	        HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_GET_PAGECONTENT_ZYX);
	        String html = StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
	        _log.trace("MAS response:");
	        _log.trace(html);
		}
		catch (Throwable e)
		{
			_log.error(e);
		}
	}
	
	private List<String> getMetricsEnabled(String scopeGroup)
	{	
		List<String> masSecrets = new ArrayList<String>();
		try
		{
			// Recupera el Id del grupo. Si se indican ids globales llegará su nombre. Si no, llegará como Id.
			long scopeGroupId = _globalIds ? GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), scopeGroup).getGroupId() : Long.parseLong(scopeGroup);
			
			// Recupera todos los grupos de la misma delegación.
			List<Long> groups = GroupLocalServiceUtil.getGroup(scopeGroupId).getDelegationSiblings();
			
			// SI no es el grupo global, también lo añade.
			if (GroupMgr.getGlobalGroupId() != scopeGroupId)
				groups.add(scopeGroupId);
			
			for (long groupId : groups)
			{
				// Comprueba si el grupo tiene habilitadas las métricas
				boolean masStatsEnabled = GetterUtil.getBoolean(GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@enablemetrics"), false);
				if (masStatsEnabled)
				{
					String groupMasSecret = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@appid"), StringPool.BLANK);
					if (Validator.isNotNull(groupMasSecret))
						masSecrets.add(addCRC(groupMasSecret));
				}
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return masSecrets;
	}
	
	private String addCRC(String in)
	{
		String crc = StringPool.BLANK;
		
		if (Validator.isNotNull(in))
		{
			int xorSum = in.charAt(0);
			for (int i=1; i<in.length(); i++)
			{
				int ascii = in.charAt(i);
				xorSum = xorSum ^ ascii;
			}
			
			crc = Integer.toHexString(xorSum);
			crc = ("00" + crc).substring(crc.length());
		}
		
		return (new StringBuilder(in)).append(crc).toString();
	}
}