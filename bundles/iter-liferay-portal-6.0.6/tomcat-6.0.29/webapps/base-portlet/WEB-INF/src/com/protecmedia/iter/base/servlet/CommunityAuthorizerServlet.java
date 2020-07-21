package com.protecmedia.iter.base.servlet;

import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.protecmedia.iter.base.community.manager.CommunityFactory;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;

public class CommunityAuthorizerServlet extends HttpServlet
{
	private static final long serialVersionUID = -1377865435648150820L;
	private static Log _log = LogFactoryUtil.getLog(CommunityAuthorizerServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		HashMap<String, String> parameters = parseRequest(request);
		// Código devuelto por la red social tras la autorización (Puede venir en code o en oauth_token).
		String authCode = parameters.get("code") != null ? parameters.get("code") : parameters.get("oauth_token");
		// Error devuelto por la red social
		String error = parameters.get("error");
		
		IterRequest.setOriginalRequest(request);
		
		//--------------------------------------------------------------------------------------
		// Acceso inicial al servlet
		//--------------------------------------------------------------------------------------
		if (Validator.isNull(authCode) && Validator.isNull(error))
		{
			try
			{
				// Petición de Login
				CommunityFactory.getCommunity(parameters).authorize(response);
				
			}
			catch (Throwable th)
			{
				processError(response, th);
			}
		}
		//--------------------------------------------------------------------------------------
		// Confirmación de la red social
		//--------------------------------------------------------------------------------------
		else
		{
			// Ocurrió un error al solicitar los permisos
			if (Validator.isNotNull(error))
			{
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_OK, IterErrorKeys.XYZ_E_COMMUNITY_PERMISSIONS_ERROR_ZYX, error);
			}
			// Respuesta de la autenticación
			else
			{
				try
				{
					CommunityFactory.getCommunity(parameters).grant(response, authCode);
				}
				catch (Throwable th)
				{
					processError(response, th);
				}
			}
		}
	}
	
	private String getHostURL(HttpServletRequest request)
	{
		return new StringBuilder(request.getServerName()).append(request.getRequestURI()).toString();
	}
	
	/**
	 * Extrae el nombre de la red social de la URL de la petición.
	 * @param request la petición http.
	 * @return String con el nombre de la red social.
	 */
	private String getCommunityName(HttpServletRequest request)
	{
		// Recupera la URL
		String uri = request.getRequestURI().toLowerCase();
		String[] splituri = uri.split(StringPool.SLASH);
		// Devuelve la red social
		return splituri[splituri.length - 1];
	}
	
	private HashMap<String, String> parseRequest(HttpServletRequest request)
	{
		HashMap<String, String> parameters = parseQueryString(request.getQueryString(), true);
		
		// URL del host
		parameters.put(CommunityAuthorizerUtil.REDIRECT_URI, getHostURL(request));
		
		// Nombre de la red social
		parameters.put(CommunityAuthorizerUtil.COMMUNITY_NAME, getCommunityName(request));
		
		return parameters;
	}
	
	/**
	 * <p>Procesa el {@code QueryString} de la petición y lo mapea a un {@code HashMap}.</p>
	 * 
	 * <p>Si llega el parámetro {@code state} y {@code parseStateParam} es {@code true}, se procesa su contenido y se separa en
	 * pares clave-valor que se añaden al mapa.</p>
	 * 
	 * @param queryString     el {@code QueryString} del request.
	 * @param parseStateParam {@code true} si se quiere procesar y separar el contenido del parámetro {@code state}.
	 *                        {@code false} si se quiere mapear el contenido del parámetro {@code state} sin procesar.
	 * @return                {@code HashMap<String, String>} con los pares clave-valor de los parámetros del {@code QueryString}.
	 */
	private HashMap<String, String> parseQueryString(String queryString, boolean parseStateParam)
	{
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		if (Validator.isNotNull(queryString))
		{
			// Separa los pares de parámetros.
			for(String param : queryString.split(StringPool.AMPERSAND))
			{
				// Separa la clave y el valor del parámetro.
				String[] pair = param.split(StringPool.EQUAL);
				if (pair.length == 2)
				{
					// Caso especial. Contiene codificado el querystring de la llamada inicial al servlet.
					if ("state".equals(pair[0]) && parseStateParam)
					{
						// Decodifica el contenido y se parsea como si fuera otro queryString.
						String previousQueryString = HttpUtil.decodeURL(pair[1]);
						parameters.putAll(parseQueryString(previousQueryString, false));
					}
					else
					{
						// Añade el parámetro al mapa.
						parameters.put(pair[0], pair[1]);
					}
				}
			}
		}
		
		return parameters;
	}
	
	private void processError(HttpServletResponse response, Throwable t)
	{
		if (t instanceof ServiceError)
		{
			// 400 PETICIÓN INVÁLIDA.
			CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_BAD_REQUEST, ((ServiceError) t).getErrorCode(), t);
		}
		
		else if (t instanceof SystemException)
		{
			// 501 NOT IMPLEMENTED.
			if (t.getMessage().equals(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_INPLEMENTED_ZYX))
			{
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_NOT_IMPLEMENTED, t.getMessage(), StringPool.BLANK);
			}
			// 500 ERROR INTERNO DEL SERVIDOR.
			else
			{
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage(), StringPool.BLANK);
			}
		}
	}
}
