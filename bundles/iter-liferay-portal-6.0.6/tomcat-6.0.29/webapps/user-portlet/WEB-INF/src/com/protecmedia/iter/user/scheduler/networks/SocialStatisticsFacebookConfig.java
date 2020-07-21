package com.protecmedia.iter.user.scheduler.networks;

import com.liferay.portal.kernel.Social.FacebookConstants;
import com.liferay.portal.kernel.Social.FacebookTools;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class SocialStatisticsFacebookConfig extends SocialStatisticsConfig
{
	public SocialStatisticsFacebookConfig(String groupId)
	{
		super(groupId);
	}

	@Override
	public void init()
	{	
		name = "Facebook";
		statistics_op 	= IterKeys.OPERATION_FB;

		quota 			= GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_QUOTA), "1/1000");
		
		delay 			= GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_DELAY), 1000);
		max_threads 	= GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_MAXTHREADS), 1);
		max_articles 	= GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_MAXARTICLES), 1);
		
		connectTimeout 	= GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_TIMEOUT_CONNECT), connectTimeout);
		readTimeout 	= GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_TIMEOUT_READ), readTimeout);
		
		api_url 		= StringPool.BLANK; // Se configura en el método prepare()
		response_count_field = "engagement.share_count";
	}
	
	@Override
	public boolean prepare()
	{
		boolean isOK 		= false;
		String accessToken 	= getAccessToken();
		if (Validator.isNotNull(accessToken))
		{
			api_url = FacebookConstants.API_GRAPH.concat("?").concat(accessToken).concat("&id=%s&fields=engagement");
			isOK	= true;
		}
		else
		{
			log.error("Unable to retrieve access_token from Facebook.");
		}
		return isOK;
	}
	
	private String getAccessToken()
	{
		String accessToken = StringPool.BLANK;
		
		try
		{
			// Recupera el clientId y clientSecret configurado
			long lGroupId 		= Long.valueOf(this.groupId);
			String clientId 	= CommunityAuthorizerUtil.getConfigPublicKey(CommunityAuthorizerUtil.FACEBOOK, lGroupId);
			String clientSecret = CommunityAuthorizerUtil.getConfigSecretKey(CommunityAuthorizerUtil.FACEBOOK, lGroupId);
			
			if (Validator.isNotNull(clientId) && Validator.isNotNull(clientSecret))
			{
				// Monta la url para solicitar el token
				String url = String.format("%s/oauth/access_token?client_id=%s&client_secret=%s&grant_type=client_credentials", FacebookConstants.API_GRAPH, clientId, clientSecret);
				
				// Solicita el token
				IterHttpClient iterHttpClient = new IterHttpClient.Builder(IterHttpClient.Method.GET, url).build();
				String response = iterHttpClient.connect();
				
				// Recoge el token
				JSONObject json = JSONUtil.createJSONObject(response);
				accessToken = json.getString("access_token");
				
				accessToken = String.format("access_token=%s&%s=%s", 
								accessToken, FacebookConstants.PARAM_APPSECRET_PROOF, 
								FacebookTools.get_appsecret_proof(accessToken, clientSecret));
			}
			else
			{
				log.error("Facebook application not configured!");
			}
		}
		catch (Throwable th)
		{
			// Do nothing
		}
		
		return accessToken;
	}
}