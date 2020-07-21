package com.protecmedia.iter.base.community.manager;

import java.util.HashMap;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;

public class CommunityFactory
{
	// Previene instancación, inluidos ataques por reflection.
	private CommunityFactory() { throw new AssertionError(); }
	
	public static CommunityManager getCommunity(HashMap<String, String> communityParams) throws ServiceError, SystemException
	{
		ErrorRaiser.throwIfNull(communityParams, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera el nombre de la red social (OBLIGATORIO)
		String communityName = communityParams.get(CommunityAuthorizerUtil.COMMUNITY_NAME);
		ErrorRaiser.throwIfNull(communityName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera el grupo (OBLIGATORIO)
		long groupId = GetterUtil.getLong(communityParams.get(CommunityAuthorizerUtil.GROUP_ID));
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera la URI del host para la redirección (OPCIONAL)
		String redirectURI = communityParams.get(CommunityAuthorizerUtil.REDIRECT_URI);
		if (Validator.isNotNull(redirectURI))
			redirectURI = new StringBuilder(IterSecureConfigTools.getHTTPS(groupId) ? Http.HTTPS_WITH_SLASH : Http.HTTP_WITH_SLASH).append(redirectURI).toString();
		
		CommunityManager community = null;
		//-----------------------------------------------------------------------------------------
		// FACEBOOK
		//-----------------------------------------------------------------------------------------
		if (CommunityAuthorizerUtil.FACEBOOK.equalsIgnoreCase(communityName) || CommunityAuthorizerUtil.INSTANT_ARTICLE.equalsIgnoreCase(communityName))
		{
			// Recupera el nombre de la página en la que publicar (OPCIONAL)
			String pageName = communityParams.get(CommunityAuthorizerUtil.FACEBOOK_PAGE_NAME);
			
			// Recupera el formato de los artículos a publicar (OPCIONAL)
			boolean instantArticle = GetterUtil.getBoolean(communityParams.get(CommunityAuthorizerUtil.FACEBOOK_INSTANT_ARTICLE));

			// Instancia el comunnity manager de facebook.
			community = new FacebookCommunityManager(redirectURI, groupId, pageName, instantArticle);
			
		}
		//-----------------------------------------------------------------------------------------
		// TWITTER
		//-----------------------------------------------------------------------------------------
		else if (CommunityAuthorizerUtil.TWITTER.equalsIgnoreCase(communityName))
		{
			// Recupera el token verifier (OPCIONAL)
			String oauthVerifier = communityParams.get("oauth_verifier");
			
			// Instancia el comunnity manager de twitter.
			community = new TwitterCommunityManager(redirectURI, groupId, oauthVerifier);
		}
		//-----------------------------------------------------------------------------------------
		// APPLE NEWS
		//-----------------------------------------------------------------------------------------
		else if (CommunityAuthorizerUtil.APPLE_NEWS.equalsIgnoreCase(communityName))
		{	
			// Instancia el comunnity manager de twitter.
			community = new AppleNewsCommunityManager();
		}
		//-----------------------------------------------------------------------------------------
		// NOTIFICACIONES PUSH
		//-----------------------------------------------------------------------------------------
		else if (CommunityAuthorizerUtil.WPN.equalsIgnoreCase(communityName))
		{	
			// Instancia el comunnity manager de twitter.
			community = new NotificationsCommunityManager(groupId);
		}
		
		// Si no existe la red social indicada, se lanza un XYZ_E_SOCIAL_NETWORK_NOT_INPLEMENTED_ZYX
		if (community == null)
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_INPLEMENTED_ZYX);
		
		return community;
	}
}
