package com.protecmedia.iter.base.community.publisher;

import java.util.Date;

import com.liferay.portal.kernel.Social.FacebookConstants;
import com.liferay.portal.kernel.Social.FacebookTools;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.community.util.CommunityHttpClient;
import com.protecmedia.iter.base.service.util.IterKeys;

public class CommunityPublisherProgressChecker extends Thread
{
	/** Logger */
	private static Log _log = LogFactoryUtil.getLog(CommunityPublisherProgressChecker.class);

	private boolean running;
	private static final int DEFAULT_WAIT_TIME = 5;
	private int waitTime;
	
	private String processId;
	private String publicationId;
	private long groupId;
	private String articleId;
	private String accountId;
	private String accountName;
	private String credentials;
	private String token;
	private String _appSecretProof;
	
	/** Sentencia para recuperar los datos de la publicación */
	private static final String SQL_SELECT_PUBLICATION = "SELECT publicationId, groupId, articleId, accountId, accountName, credentials FROM schedule_publication WHERE processId='%s'";
	
	/** Sentencia para eliminar la publicación cuando ya ha sido procesada */
	private static final String SQL_DELETE_PROCESED_PUBLICATION = "DELETE FROM schedule_publication WHERE publicationId = '%s'";
	
	
	/**
	 * <p>Establece el candado para el acceso sincronizado a BBDD.</p>
	 * <p>Por defecto, el proceso arranca activo y con replanificación pendiente.</p>
	 * @param lock El candado para el acceso a BBDD sincronizado.
	 */
	public CommunityPublisherProgressChecker(String processId)
	{
		super("Community Publisher Process");
		this.processId = processId;
		this.running = true;
		this.waitTime = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIAL_MANAGER_FACEBOOK_IAPROCESSINGWAIT), DEFAULT_WAIT_TIME);
	}
	
	@Override
	public void run()
	{
		_log.debug("Community Publisher Progress Checker [" + processId + "] started");

		// Recupera los datos de la publicación
		if (running && getPublicationInfo())
		{
			while (running)
			{
				// Comprueba si la publicación está siendo procesada
				if (checkProcesingStatus())
				{
					// Si ha terminado, elimina la publicación
					deletePublication();
					running = false;
				}
				// Si no ha terminado, espera un tiempo prudencial
				else
				{
					waitUntilNextCheck();
				}
			}
		}
		_log.debug("Community Publisher Progress Checker [" + processId + "] stopped");
	}

	private boolean getPublicationInfo()
	{
		boolean isOK = false;
		try
		{
			if (Validator.isNotNull(processId))
			{
				_log.debug("Retrieving publication with processId " + processId + "...");
				
				Document rawPublication = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_PUBLICATION, processId));
				Node publication = rawPublication.selectSingleNode("/rs/row");
				
				// Recupera la información necesaria para consultar el estado
				this.publicationId 	= XMLHelper.getStringValueOf(publication, 	"@publicationId");
				this.groupId 		= XMLHelper.getLongValueOf(publication, 	"@groupId");
				this.articleId 		= XMLHelper.getStringValueOf(publication, 	"@articleId");
				this.accountId 		= XMLHelper.getStringValueOf(publication, 	"@accountId");
				this.accountName 	= XMLHelper.getStringValueOf(publication, 	"@accountName");
				this.credentials 	= XMLHelper.getStringValueOf(publication, 	"@credentials");
				this.token 			= credentials.split(StringPool.COMMA)[0];
				
				// Comprueba que esté todo
				if (Validator.isNotNull(processId) && groupId > 0 && Validator.isNotNull(articleId) && Validator.isNotNull(accountId) &&
				    Validator.isNotNull(accountName) && Validator.isNotNull(credentials) && Validator.isNotNull(token))
				{
					isOK = true;
	
					String secretKey 	= CommunityAuthorizerUtil.getConfigSecretKey(CommunityAuthorizerUtil.FACEBOOK, groupId);
					this._appSecretProof= FacebookTools.get_appsecret_proof(token, secretKey);
				}
				else
					_log.debug("Unable to retrieve publication with processId " + processId);
			}
		}
		catch (Throwable th)
		{
			_log.error("Unable to retrieve publication with processId " + processId);
			_log.error(th);
		}
		return isOK;
	}
	
	private boolean checkProcesingStatus()
	{
		boolean finished = false;
		String status = null;
		boolean error = false;
		String trace = null;
		try
		{
			_log.debug("Cheking progress of process " + processId + "...");
			
			// Llama a la API de Facebook para consultar su estado
			JSONObject response = new CommunityHttpClient.Builder(FacebookConstants.API_GRAPH.concat("/").concat(processId))
									.queryString("access_token", token)
									.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, _appSecretProof)
									.build()
									.get();
			
			// Recoge el estado
			status = response.getString("status");
			
			if(Validator.isNull(status) && Validator.isNotNull(response.getJSONObject("error")))
				error = true;
			
			trace = response.toString(4);
		}
		catch (Throwable th)
		{
			_log.debug("Unable to call API for cheking progress of process " + processId);
			_log.error(th);
		}
		
		// Si no está en progreso, informa el resultado en IterMonitor
		if (Validator.isNotNull(status) && !status.equals("IN_PROGRESS"))
		{
			_log.debug("Process " + processId + " finished");
			finished = true;
			Event eventKind = status.equals("FAILED") ? Event.ERROR : Event.INFO;
			IterMonitor.logEvent(groupId, eventKind, new Date(), "Facebook Publisher: Facebook Instant Article process result", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, trace));
		}
		else if (Validator.isNotNull(status))
		{
			_log.debug("Process " + processId + " still in progress");
		}
		else if (error)
		{
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "Facebook Publisher: Facebook Instant Article process result", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, trace));
			finished = true;
		}
		else
		{
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "Facebook Publisher: Facebook Instant Article unkown error", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, trace));
			finished = true;
		}
		
		return finished;
	}
	
	private void deletePublication()
	{
		try 
		{
			_log.debug("Deleting publication with processId " + processId + "...");
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_DELETE_PROCESED_PUBLICATION, publicationId));
		}
		catch (Throwable th)
		{
			_log.debug("Unable to delete publication with processId " + processId);
		}
	}
	
	private void waitUntilNextCheck()
	{
		try
		{
			sleep(waitTime * 1000);
		}
		catch (InterruptedException e)
		{
			_log.debug("Progress Checker Interrupted for processId " + processId);
			IterMonitor.logEvent(groupId, IterMonitor.Event.WARNING, new Date(), articleId, "Facebook Publisher: Facebook Instant Article status process comprobation cancelled");
			running = false;
		}
	}
	
	public void halt()
	{
		// Establece el indicador de encendido a falso.
		running = false;
		// Manda una interrupcion por si estuviera esperando.
		this.interrupt();
	}
}
