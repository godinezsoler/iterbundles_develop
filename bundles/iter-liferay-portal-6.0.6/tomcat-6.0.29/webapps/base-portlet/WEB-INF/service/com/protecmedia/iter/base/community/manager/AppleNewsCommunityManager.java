package com.protecmedia.iter.base.community.manager;

import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.render.PageRenderer;
import com.liferay.portal.kernel.render.RenditionMode;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.community.util.SimpleOAuthClient;

public class AppleNewsCommunityManager implements CommunityManager
{
	private static Log _log = LogFactoryUtil.getLog(AppleNewsCommunityManager.class);
	String SQL_UPDATE_APPLEID = "INSERT INTO applenewsarticle (articleId, appleId) VALUES ('%s', '%s') ON DUPLICATE KEY UPDATE appleId=VALUES(appleId)";
	String SQL_SELECT_APPLEID = "select appleId from applenewsarticle WHERE articleId='%s'";
	
	//---------------------------------------------------------------------------------------------------------
	// END POINTS de la API de Apple News
	//---------------------------------------------------------------------------------------------------------
	// Host
	private static final String HOST_URL        = "https://news-api.apple.com";
	// End Points
	private static final String ENDPOINT_SECTIONS = "/channels/%s/sections"; // {channel_id}
	private static final String ENDPOINT_CREATE   = "/channels/%s/articles"; // {channel_id}
	private static final String ENDPOINT_UPDATE   = "/articles/%s";          // {article_id}
	private static final String ENDPOINT_READ     = "/articles/%s";          // {article_id}
	
	//---------------------------------------------------------------------------------------------------------
	// Parámetros de configuración de Apple News
	//---------------------------------------------------------------------------------------------------------
	private long   groupId;
	private String articleId;
	private JournalArticle article;
	private String accountId;
	private String accountName;
	private String channelId;
	private String APIKey;
	private String APISecret;
	
	private String metadataContent = StringPool.BLANK;
	private String articleContent = StringPool.BLANK;
	private boolean updatingArticle = false;
	private String appleId = StringPool.BLANK;
	private String revision = StringPool.BLANK;
	
	@Override
	public void authorize(HttpServletResponse response)
	{
		// Do nothing
	}

	@Override
	public void grant(HttpServletResponse response, String code) throws SystemException
	{
		// Do nothing
	}
	
	@Override
	public void publish(String articleId, HashMap<String, String> params)
	{
		try
		{
			// Establece los parámetros de la publicación
			setPublicationParams(articleId, params);

			// Comprueba si ya existe la publicación y, en caso afirmativo, guarda el appleId y la revisión del artículo.
			checkExistingArticle();
			
			// Recupera el contenido a publicar
			buildContent();
			
			String response = null;
			if (updatingArticle)
			{
				// Si ya existe, la actualiza
				response = updateArticle();
			}
			else
			{
				// Si no existe, la crea
				response = createArticle();
			} 
			
			// Procesa la respuesta
			processPublicationResponse(response);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	private static final String JSON_FIELD_DATA = "data";
	private static final String JSON_FIELD_NAME = "name";
	private static final String JSON_FIELD_ID   = "id";
	private static final String JSON_FIELD_LINKS = "links";
	private static final String JSON_FIELD_SECTIONS = "sections";
	private static final String JSON_FIELD_REVISION = "revision";
	
	private String getAppleSectionId()
	{
		String appleSectionId = StringPool.BLANK;

		try
		{
			long articleMainSectionPlid 	= SectionUtil.getMainSectionPlid(groupId, articleId);
			Layout sectionLayout 			= LayoutLocalServiceUtil.getLayout(articleMainSectionPlid);
			String articleMainSectionName 	= sectionLayout.getName( LocaleUtil.getDefault() );
			
			if (Validator.isNotNull(articleMainSectionName))
			{
				// Recupera las secciones del canal
				String url = HOST_URL + String.format(ENDPOINT_SECTIONS, channelId);
				String rawResponse = new SimpleOAuthClient.Builder(SimpleOAuthClient.Method.GET, url)
													   .apiKey(APIKey)
													   .apiSecret(APISecret)
													   .build()
													   .connect();
			
				JSONObject response = JSONUtil.createJSONObject(rawResponse);
				JSONArray data = response.getJSONArray(JSON_FIELD_DATA);
				for (int i = 0; i < data.length(); i++)
				{
					JSONObject section = data.getJSONObject(i);
					String sectionName = section.getString(JSON_FIELD_NAME);
					
					if (articleMainSectionName.equals(sectionName))
					{
						String sectionId = section.getString(JSON_FIELD_ID);
						ErrorRaiser.throwIfFalse(Validator.isNotNull(sectionId), IterErrorKeys.XYZ_E_COMMUNITY_APPLE_INVALID_SECTIONID_ZYX, sectionId);
						
						appleSectionId = sectionId;
						break;
					}
				}
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			IterMonitor.logEvent(groupId, Event.WARNING, new Date(), "Apple News Publisher Warning: Problems with channel sections", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
		}
		
		return appleSectionId;
	}
	
	/**
	 * Comprueba si el artículo tiene un appleArticleId en el Descriptión.
	 * Si no es así, comprueba si existe en Apple News.
	 * @return
	 */
	private void checkExistingArticle()
	{
		try
		{
			// Busca el article-id de Apple en el description
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_APPLEID, article.getArticleId()));
			appleId = XMLHelper.getStringValueOf(dom.getRootElement(), "/rs/row[1]/@appleId");
			
			if (Validator.isNotNull(appleId))
			{
				// Llama a la API para comprobar si existe
				String url = HOST_URL + String.format(ENDPOINT_READ, appleId);
				String rawResponse = new SimpleOAuthClient.Builder(SimpleOAuthClient.Method.GET, url)
													   .apiKey(APIKey)
													   .apiSecret(APISecret)
													   .build()
													   .connect();
			
				JSONObject response 	= JSONUtil.createJSONObject(rawResponse);
				JSONObject data 		= response.getJSONObject(JSON_FIELD_DATA);
				String appleArticleId 	= data.getString(JSON_FIELD_ID);

				if (appleArticleId.equals(appleId))
				{
					updatingArticle = true;
					revision = data.getString(JSON_FIELD_REVISION);
				}
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			IterMonitor.logEvent(groupId, Event.WARNING, new Date(), "Apple News Publisher Warning: Problems checking if article exist", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
		}
	}
	
	private void buildContent() throws Throwable
	{
		try
		{
			String rendererContent = PageRenderer.getArticle(RenditionMode.anf, groupId, articleId);
			JSONObject jsonArticleContent = JSONUtil.createJSONObject(rendererContent);
			
			// Extrae el elemento "data" del contenido del renderer o lo crea si no existe
			JSONObject data = jsonArticleContent.getJSONObject(JSON_FIELD_DATA);
			if (data != null)
				jsonArticleContent.remove(JSON_FIELD_DATA);
			else
				data = JSONFactoryUtil.createJSONObject();
			
			// Comprueba que exista la sección principal del artículo en el canal. Si no es así, lo publicará sin sección
			String appleSectionId = getAppleSectionId();
			if (Validator.isNotNull(appleSectionId))
			{
				JSONArray sections = JSONFactoryUtil.createJSONArray();
				sections.put(HOST_URL + "/sections/" + appleSectionId);
				JSONObject links = JSONFactoryUtil.createJSONObject();
				links.put(JSON_FIELD_SECTIONS, sections);
				data.put(JSON_FIELD_LINKS, links);
			}
			
			// Si es una actualización, añade el campo "revision"
			if (updatingArticle)
				data.put(JSON_FIELD_REVISION, revision);
			
			// Crea los elementos artículo y metadata
			JSONObject metadata = JSONFactoryUtil.createJSONObject();
			metadata.put(JSON_FIELD_DATA, data);
			
			articleContent = jsonArticleContent.toString();
			metadataContent = metadata.toString();
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(groupId, Event.ERROR, new Date(), "Apple News Publisher Error: Unable to generate content", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}
	}
	
	private String createArticle()
	{
		return publishArticle(String.format(ENDPOINT_CREATE, channelId));
	}
	
	private String updateArticle()
	{
		return publishArticle(String.format(ENDPOINT_UPDATE, appleId));
	}
	
	private String publishArticle(String endPoint)
	{
		String url = HOST_URL + endPoint;
		String response = new SimpleOAuthClient.Builder(SimpleOAuthClient.Method.POST, url)
											   .apiKey(APIKey)
											   .apiSecret(APISecret)
											   .contentType(SimpleOAuthClient.ContentType.MULTIPART)
											   .metadata(metadataContent)
											   .payLoad(articleContent)
											   .build()
											   .connect();
		
		return response;
	}
	
	private void processPublicationResponse(String response)
	{
		try
		{
			// Parsea la respuesta JSON
			JSONObject jsonRespone = JSONUtil.createJSONObject(response);
			
			try
			{
				// Si ha ido bien, recupera el appleID del artículo y lo guarda
				String appleId = jsonRespone.getJSONObject(JSON_FIELD_DATA).getString(JSON_FIELD_ID);
				PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_APPLEID, article.getArticleId(), appleId));
				
				String resultTrace = jsonRespone.toString(4);
				IterMonitor.logEvent(groupId, Event.INFO, new Date(), "Apple News Publisher: Content published", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, resultTrace));
			}
			catch (NullPointerException e)
			{
				// Si ocurrió un error, lo registra
				String resultTrace = jsonRespone.toString(4);
				IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "Apple News Publisher Error", article.getArticleId(), CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, resultTrace));
			}
		}
		catch (Throwable e)
		{
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "Apple News Publisher Error: Unparseable response", article.getArticleId(), CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, e));
			_log.error(e);
		}
	}
	
	private void setPublicationParams(String articleId, HashMap<String, String> params) throws Throwable
	{
		try
		{
			// Recupera el Id de la cuenta para trazarla en el Monitor
			this.accountId = params.get("accountId");
			ErrorRaiser.throwIfNull(this.accountId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera el nombre de la cuenta para trazarla en el Monitor
			this.accountName = params.get("accountName");
			ErrorRaiser.throwIfNull(this.accountName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera el grupo
			this.groupId = GetterUtil.getLong(params.get("groupId"));
			ErrorRaiser.throwIfFalse(this.groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera el artículo
			this.articleId = articleId;
			ErrorRaiser.throwIfNull(this.articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			this.article   = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), this.articleId);
			
			// Recupera las credenciales.
			String[] credentials = params.get("credentials").split(StringPool.COMMA);
			if (credentials.length == 3)
			{
				this.channelId = credentials[0];
				this.APIKey    = credentials[1];
				this.APISecret = credentials[2];
				ErrorRaiser.throwIfFalse(Validator.isNotNull(this.channelId) && Validator.isNotNull(this.APIKey) && Validator.isNotNull(this.APISecret), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
			else
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(this.groupId, Event.ERROR, new Date(), "Apple News Publisher Error: Wrong credentials", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}
	}
}
