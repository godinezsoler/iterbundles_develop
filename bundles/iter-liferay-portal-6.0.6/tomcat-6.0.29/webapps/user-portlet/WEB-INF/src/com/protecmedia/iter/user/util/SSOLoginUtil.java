package com.protecmedia.iter.user.util;

import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.IterMonitor;
import com.protecmedia.iter.base.service.AuthorizationKeyLocalServiceUtil;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil;

public class SSOLoginUtil
{
	/** Log */
	private static Log _log = LogFactoryUtil.getLog(SSOLoginUtil.class);
	
	/** Id del grupo */
	private long groupId;
	/** Friendly URL del grupo para acceder a las propiedades */
	String groupFriendlyUrl;
	/** Id de la delegación del grupo */
	private long delegationId;
	/** Indicador de si se debe recuperar el aboId tras el registro del usuario */
	boolean checkAboId;
	
	/** Clave pública asociada a la clave privada usada por el sistema externo */
	private String publicKey = StringPool.BLANK;
	
	/** Clave privada usada por el sistema externo para encriptar los datos */
	private String privateKey = StringPool.BLANK;
	
	/** Datos encriptados del usuario enviados por el sistema externo */
	private String encryptedUserData = StringPool.BLANK;
	
	/** 
	 * Version del SSO data 
	 * Es el primer fragmento del ssodata. En las versiones que no vengan tres grafmentos se asumirá versión 0
	 * 
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1383 <br/>
	 *      refreshUserEntitlements: PHP Fatal error: Uncaught Error: Length must be greater than 0 
	 * 
	 * */
	private int ssodata_version = 0;
	
	/** Indicador de errores */
	private boolean validProcess = true;
	
	/** Id del usuario en Iter */
	private String usrId = StringPool.BLANK;
	/** Nombre de usuario */
	private String usrName = StringPool.BLANK;
	/** Contraseña en MD5 */
	private String pwdmd5 = StringPool.BLANK;
	/** Correo electrónico */
	private String email = StringPool.BLANK;
	/** Id del usuario en el sistema externo */
	private String extid = StringPool.BLANK;
	/** Datos adicionales de la autoridad de autenticación para el registro desde iPad */
	JSONObject extralogindata = null;
	
	// CONSTANTES PARA EL DESCIFRADO Y PROCESADO DE LOS DATOS
	private static final String MD5  = "MD5";
	private static final String AES  = "AES";
	private static final String UTF8 = "UTF-8";
	private static final String CIPHER_TRANSFORM_ECB = "AES/ECB/PKCS5Padding";
	private static final String CIPHER_TRANSFORM_CBC = "AES/CBC/PKCS5Padding";
	
	private static final String CIPHER_PROVIDER  = "SunJCE";
	
	// CONSTANTES DEL FORMATO DEL JSON DE DATOS DE USUARIO
	private static final String JSON_USRNAME = "usrname";
	private static final String JSON_PASSWRD = "pwdmd5";
	private static final String JSON_USRMAIL = "email";
	private static final String JSON_EXTID   = "extid";
	
	// CONSTANTES PARA LAS CONSULTAS CONTRA SQL
	private static final String SQL_SELECT_USR = "SELECT usrid FROM iterusers WHERE extid='%s' AND delegationid=%d";
	private static final String SQL_INSERT_USR = "INSERT INTO iterusers (usrid, usrname, pwd, email, registerdate, delegationid, extid) VALUES ('%s', '%s', '%s', '%s', NOW(), %d, '%s')";
	private static final String SQL_UPDATE_USR = "UPDATE iterusers SET usrname='%s', pwd='%s', email='%s', updateprofiledate=NOW() WHERE usrid='%s'";
	private static final String XPATH_USRID    = "/rs/row/@usrid";
	
	// CONSTANTES PARA LAS COOKIES
	private static final String COOKIE_HEADER           = "Set-Cookie";
	private static final String COOKIE_HEADER_ATTR      = "Set-Cookie-Data";
	private static final String COOKIE_HEADER_SSODATA   = "ssodata=";
	private static final String COOKIE_HEADER_EXTRADATA = "extralogindata=";
	
	// CONSTANTES PARA EL PROCESADO DE DATOS EXTRAS PARA EL LOGIN DESDE IPAD
	private static final String PLIST_KEY     = "key";
	private static final String PLIST_INTEGER = "integer";
	private static final String PLIST_REAL    = "real";
	private static final String PLIST_TRUE    = "true";
	private static final String PLIST_FALSE   = "false";
	private static final String PLIST_DICT    = "dict";
	private static final String PLIST_ARRAY   = "array";
	private static final String PLIST_STRING  = "string";
	
	// CONSTANTES PARA EL REGISTRO DE ERRORES DEL PROCESO
	private static final String SSO_MSG_PREFIX      = "SSO Login process: %s";
	// Error de inicialización al comprobar el grupo
	private static final String ERR_INITIALIZATION  = "Unable to retrieve group with groupId %d";
	// Error al solicitar los datos al SSO
	private static final String ERR_SSO_REQUEST     = "SSO bad request for URL %s";
	private static final String ERR_SSO_RESPONSE    = "SSO bad response for URL %s";
	private static final String ERR_SSO_INVALID_DATA= "Invalid user data from SSO data request for URL %s";
	private static final String ERR_SSO_INVALID_EXTRADATA= "Invalid extra login data for URL %s";
	// Errores durante la validación de los datos del SSO
	private static final String ERR_GET_DELEGATION  = "Unable to retrieve delegationId for group %d";
	private static final String ERR_GET_KEY         = "Unable to retrieve private key from public key [%s]";
	private static final String ERR_INVALID_KEY     = "Invalid public key [%s]";
	private static final String ERR_INVALID_SSO_DATA= "Invalid SSO data: %s";
	private static final String ERR_DECRYPT_DATA    = "Unable to decrypt SSO data";
	private static final String ERR_PARSING_DATA    = "SSO data is not a valid JSON: %s";
	private static final String ERR_INVALID_DATA    = "SSO data is invalid";
	// Errores durante el proceso
	private static final String ERR_CHECKING_USR    = "Unable to check if user exists [extid=%s, delegationId=%d]";
	private static final String ERR_REGISTERING_USR = "Unable to register user in IterWeb";
	private static final String ERR_CREATING_COOKIE = "Unable to create user cookie";
	private static final String ERR_ADDING_COOKIE   = "Unable to add user cookie to response";
	private static final String ERR_ADDING_EXTRADATA= "Error appending extra login data to iPad output";
	
	/**
	 * <p>Construye una instancia de la clase para regitrar usuarios gestionados
	 * por un sistema externo.</p>
	 * 
	 * <p>Sólo registrará al usuario, sin autenticarlo.</p>
	 * 
	 * @param groupId El Id del grupo en el que registrar al usuario.
	 */
	public SSOLoginUtil(long groupId)
	{
		this.groupId = groupId;
		// Valida que el grupo exista y recupera su friendly url
		try
		{
			this.groupFriendlyUrl = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
		}
		catch (Throwable th)
		{
			String msg = String.format(SSO_MSG_PREFIX, String.format(ERR_INITIALIZATION, groupId));
			_log.error(msg);
			_log.error(th);
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, th);
		}
		// Por defecto, no autentica al usuario para obtener su id de abonado
		this.checkAboId = false;
	}

	/**
	 * <p>Construye una instancia de la clase para regitrar usuarios gestionados
	 * por un sistema externo.</p>
	 * 
	 * <p>Si {@code checkAboId} es {@code true}, realizará la autenticación del
	 * usuario contra el sistema de autenticación tras su registro en Iter.</p>
	 * 
	 * @param groupId    El Id del grupo en el que registrar al usuario.
	 * @param checkAboId Booleano que indica si debe autenticar al usuario tras su registro.
	 */
	public SSOLoginUtil(long groupId, boolean checkAboId)
	{
		this(groupId);
		this.checkAboId = checkAboId;
	}
	
	/**
	 * <p>Solicita los datos del usuario que pide logearse en el sistema al servidor de SSO y,
	 * a continuación, realiza el autologin del usuario.</p>
	 * 
	 * <p>Si ocurre un error en cualquier momento, lo registra en el {@code MONITOR} y cancela el proceso.</p>
	 * 
	 * @see #doSingleSingOn(String ssodata, HttpServletRequest request)
	 * 
	 * @param loginServerUrl La URL del endpoint del servidor que gestiona los usuarios.
	 * @param payload        El contenido del POST que se mandará al servidor de gestión de usuarios.
	 * @param request        El request al que se le añadirá la cookie del usuario.
	 */
	public void doRemoteSingleSingOn(String loginServerUrl, String payload, HttpServletRequest request)
	{
		// Si hay un servidor externo configurado, solicita los datos del usuario al SSO para registrarlo o actualizarlo.
		if (Validator.isNotNull(loginServerUrl))
		{
			if (_log.isDebugEnabled()) _log.debug("Retrieving SSO data from SSO server...");
			
			// Recupera los tiempos de timeouts configurados para la conexión con el SSO
			int conexiontimeout = GetterUtil.getInteger( PropsUtil.get(String.format(PropsKeys.ITER_LOGIN_SERVER_CONEXIONTIMEOUT, groupFriendlyUrl)), 2000 );
			int responsetimeout = GetterUtil.getInteger( PropsUtil.get(String.format(PropsKeys.ITER_LOGIN_SERVER_RESPONSETIMEOUT, groupFriendlyUrl)), 15000 );
			
			try
			{	
				// Recupera los headers originales
				HashMap<String, String> headers = new HashMap<String, String>();
				@SuppressWarnings("unchecked")
				Enumeration<String> headerNames = request.getHeaderNames();
				while (headerNames.hasMoreElements())
				{
					String headerName = headerNames.nextElement();
					// No incluir el header 'host'
					if (!"host".equals(headerName.toLowerCase()))
						headers.put(headerName, request.getHeader(headerName));
				}
				
				// Realiza un POST contra el SSO con el mismo payload y cabecera original
				IterHttpClient iterHttpClient = new IterHttpClient.Builder(IterHttpClient.Method.POST, loginServerUrl)
																  .connectionTimeout(conexiontimeout)
																  .readTimeout(responsetimeout)
																  .headers(headers)
																  .payLoad(payload)
																  .build();
				String ssoresponse = iterHttpClient.connect();
				
				// Si la respuesta es correcta
				if (iterHttpClient.validResponse())
				{
					// Recupera la cabecera Set-Cookie que contenga los datos para el SSO y los datos adicionales
					String ssodata = null;
					extralogindata = null;
					for (String header : iterHttpClient.getHeader(COOKIE_HEADER))
					{
						if (Validator.isNotNull(header))
						{
							// Cabecera con los datos para el Single Sing On
							if (header.startsWith(COOKIE_HEADER_SSODATA))
							{
								ssodata = header.substring(header.indexOf(COOKIE_HEADER_SSODATA) + COOKIE_HEADER_SSODATA.length());
								ssodata = URLDecoder.decode(ssodata, UTF8);
								ssodata = ssodata.split(StringPool.SEMICOLON)[0];
							}
							// Cabecera con los datos adicionales para el login
							else if (header.startsWith(COOKIE_HEADER_EXTRADATA))
							{
								try
								{
									String extradata = header.substring(header.indexOf(COOKIE_HEADER_EXTRADATA) + COOKIE_HEADER_EXTRADATA.length());
									extradata = URLDecoder.decode(extradata, UTF8);
									extradata = extradata.split(StringPool.SEMICOLON)[0];
									// Decodifica los datos en Base64
									extradata = new String(Base64.decode(extradata), UTF8);
									// Transforma los datos en un objeto JSON
									extralogindata = JSONUtil.createJSONObject(extradata);
								}
								// Si ocurre un error, lo registra en el Monitor y en el log del tomcat, pero no aborta el proceso.
								catch (Throwable th)
								{
									String msg = String.format(SSO_MSG_PREFIX, String.format(ERR_SSO_INVALID_EXTRADATA, loginServerUrl));
									_log.error(msg);
									_log.error(th);
									IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, th);
								}
							}
							
							// Si ya tiene los datos para el SSO y los datos extra, deja de buscar
							if (ssodata != null && extralogindata != null)
								break;
						}
					}
					
					// Si localiza los datos del SSO, procede al registro / actualización del usuario en Iter
					if (Validator.isNotNull(ssodata))
					{
						if (_log.isDebugEnabled()) _log.debug("SSO data retrieved from SSO server.");
						doSingleSingOn(ssodata, request);
					}
					// No hay datos de usuario en la respuesta del sistema externo
					else
					{
						String msg = String.format(SSO_MSG_PREFIX, String.format(ERR_SSO_INVALID_DATA, loginServerUrl));
						_log.error(msg);
						IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, null, ssoresponse);
					}
				}
				// Error en la respuesta del sistema externo
				else
				{
					String msg = String.format(SSO_MSG_PREFIX, String.format(ERR_SSO_RESPONSE, loginServerUrl));
					_log.error(msg);
					IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, null, ssoresponse);
				}
			}
			// Error al llamar al sistema externo o al procesar los datos devueltos
			catch (Throwable th)
			{
				String msg =  String.format(SSO_MSG_PREFIX, String.format(ERR_SSO_REQUEST, loginServerUrl));
				_log.error(msg);
				_log.error(th);
				IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, th);
			}
		}
		else
			_log.error("SSO server url not provided.");
	}
	
	/**
	 * <p>Realiza el autologin del usuario. Para ello:</p>
	 * <ol>
	 *   <li>Recupera la delegación del grupo.</li>
	 *   <li>Busca la clave privada asociada a la clave pública indicada por el SSO.</li>
	 *   <li>Desencripta los datos enviados por el SSO.</li>
	 *   <li>Procesa el JSON con los datos del usuario.</li>
	 *   <li>Procede a registrar / actualizar el usuario en Iter.</li>
	 * </ol>
	 * 
	 * <p>Si ocurre un error en cualquier momento, lo registra en el {@code MONITOR} y cancela el proceso.</p>
	 * 
	 * @param groupId			El Id del grupo.
	 * @param ssodata			Los datos sin procesar enviados por el SSO.
	 * @param redirectTarget	La URL de redirección indicada por el SSO.
	 */
	public void doSingleSingOn(String ssodata, HttpServletRequest request)
	{
		try
		{
			// Comprueba que el grupo existe y recupera la delegación
			getDelegationId();
						
			if (Validator.isNotNull(ssodata))
			{
				// Separa la clave pública de los datos encriptados del usuario
				String[] data = ssodata.split(StringPool.COMMA);
				if (data.length > 1)
				{
					// ITER-1383 refreshUserEntitlements: PHP Fatal error: Uncaught Error: Length must be greater than 0
					if (data.length >= 3)
					{
						this.ssodata_version   = Integer.parseInt( data[0] );
						this.publicKey         = data[1];
						this.encryptedUserData = data[2];
					}
					else
					{
						this.ssodata_version   = 0;
						this.publicKey         = data[0];
						this.encryptedUserData = data[1];
					}
					
					// Busca la clave privada asociada a la clave pública informada
					getPrivateKey();
					
					// Desencripta los datos del SSO
					String decryptedUserdata = decryptsUserData();
					
					// Recupera los datos del usuario informados por el SSO
					retrieveUserData(decryptedUserdata);
					
					// Procesa el usuario realizando el login / registro
					processUser(request);
					
					if (_log.isDebugEnabled()) _log.debug("Iter SSO finished!");
				}
				else
				{
					if (_log.isDebugEnabled()) _log.debug("Invalid SSO data.");
					logError(String.format(ERR_INVALID_SSO_DATA, ssodata), null);
				}
			}
			else
				if (_log.isDebugEnabled()) _log.debug("SSO data not provided.");
		}
		catch (Throwable t)
		{
			// Do nothing
			// Los errores se registran en el momento en el que ocurren.
			// Aquí se evita que la excepción se propage.
		}
	}
	
	/**
	 * <p>Si la inicialización fue correcta, se efectúa el proceso de registro de usuario en Iter</p>
	 * 
	 * <p>Para ello, se comprueba si el usuario ya existe en el sistema y, si no es así, se procede
	 * a realizar su alta.</p>
	 * 
	 * <p>Una vez que el usuario está registrado, se genera una cookie de usuario básica para simular
	 * el proceso de registro / login.</p>
	 * 
	 * @param request
	 * @param response
	 */
	private void processUser(HttpServletRequest request)
	{
		try
		{
			// Comprueba si ya existe un usuario con ese extid
			checkUser();
			
			// Si no existe, lo da de alta
			if (Validator.isNull(usrId))
				registerUser();
			else
				updateUser();
			
			// Si se solicita la búsqueda de código de abonado, la obtiene ahora
			if (checkAboId)
				IterRegisterLocalServiceUtil.getUserAboId(usrId, groupFriendlyUrl, email, StringPool.BLANK, request);
			
			// Añade la cookie del usuario
			addRequestCookie(request);
		}
		catch (Throwable th)
		{
			// Invalida el proceso
			validProcess = false;
		}
	}
	
	private void getDelegationId() throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Getting group delegation Id...");
		
		try
		{
			this.delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		}
		catch (Throwable th)
		{
			logError(String.format(ERR_GET_DELEGATION, groupId), th);
		}
	}
	
	private void getPrivateKey() throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Getting private key...");
		
		try
		{
			// Busca la clave privada asociada a la clave pública informada
			this.privateKey = AuthorizationKeyLocalServiceUtil.getPrivateKeyFromPublicKey(publicKey);
		}
		catch (Throwable th)
		{
			// No se puede recuperar la clave
			logError(String.format(ERR_GET_KEY, this.publicKey), th);
		}
		
		// Si la clave indicada no está registrada en el sistema, lanza error
		if (Validator.isNull(this.privateKey))
			logError(String.format(ERR_INVALID_KEY, this.publicKey), null);
	}
	
	private static final Pattern UUID_PATTERN = Pattern.compile("[A-Z0-9]{8}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{12}");
	
	private String decryptsUserData() throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Decrypting sso data...");
		
		
		String decryptedUserdata = StringPool.BLANK;
		try
		{
			// Comprueba si debe derivar la clave. Si es un UUID, deriva la clave privada (36 bytes) para obtener la clave con la que desencriptar los datos (16 bytes)
			Matcher m = UUID_PATTERN.matcher(privateKey);
			byte[] binaryKey = m.find() ? MessageDigest.getInstance(MD5).digest(privateKey.getBytes(UTF8)) : Base64.decode(privateKey);
			SecretKeySpec secretKeySpec = new SecretKeySpec(binaryKey, AES);
			
			// Decodifica en Bbase64 los datos del usuario
			byte[] byteUserData = Base64.decode(encryptedUserData);
			
			if (ssodata_version == 0)
			{
				// Desencripta usando el algoritmo Rijndael de 128 bits en modo ECB
				Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM_ECB, CIPHER_PROVIDER);
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
				decryptedUserdata = new String(cipher.doFinal(byteUserData), UTF8);
			}
			else
			{
				// byte[] binaryKey = DatatypeConverter.parseBase64Binary(secretKey);
	    		IvParameterSpec iv = new IvParameterSpec(binaryKey);
	    		
				// De momento cualquier versión distinta de 0 utiliza el algoritmo Rijndael de 128 bits en modo CBC
				Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM_CBC, CIPHER_PROVIDER);
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
				
				decryptedUserdata = new String(cipher.doFinal(byteUserData), UTF8);
			}
			
			if (_log.isTraceEnabled())
				_log.trace( String.format("ssodata version=%d data=%s", ssodata_version, decryptedUserdata) );
			
		}
		catch (Throwable th)
		{
			logError(String.format(ERR_DECRYPT_DATA,  decryptedUserdata), th);
		}
		
		return decryptedUserdata;
	}
	
	private void retrieveUserData(String decryptedUserdata) throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Retrieving user data...");
		
		try
		{
			// Transforma los datos del usuario en un objeto JSON
			JSONObject userData = JSONUtil.createJSONObject(decryptedUserdata);
			
			// Recupera los datos del usuario
			usrName = userData.getString(JSON_USRNAME);
			pwdmd5  = userData.getString(JSON_PASSWRD);
			email   = userData.getString(JSON_USRMAIL);
			extid   = userData.getString(JSON_EXTID);
		}
		catch (Throwable th)
		{
			logError(String.format(ERR_PARSING_DATA,  decryptedUserdata), th);
		}
		
		// Valida los datos del usuario
		if (Validator.isNull(usrName) || Validator.isNull(pwdmd5) || Validator.isNull(email) || Validator.isNull(extid))
			logError(ERR_INVALID_DATA, null);
	}
	
	private void checkUser() throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Checking user existence...");
		
		try
		{
			Document userDom = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_USR, StringEscapeUtils.escapeSql(extid), delegationId));
			this.usrId = XMLHelper.getStringValueOf(userDom, XPATH_USRID);
		}
		catch (Throwable th)
		{
			logError(String.format(ERR_CHECKING_USR, extid, delegationId), th);
		}
	}
	
	private void registerUser() throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Registering new user...");
		
		try
		{
			// Crea el Id del usuario
			usrId = SQLQueries.getUUID();
			// Registra al usuario en Iter
			String sql = String.format(SQL_INSERT_USR, usrId, StringEscapeUtils.escapeSql(usrName), pwdmd5, email, delegationId, StringEscapeUtils.escapeSql(extid));
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
		catch (Throwable th)
		{
			logError(ERR_REGISTERING_USR, th);
		}
	}
	
	private void updateUser() throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Updating user info...");
		
		try
		{
			// Registra al usuario en Iter
			String sql = String.format(SQL_UPDATE_USR, StringEscapeUtils.escapeSql(usrName), pwdmd5, email, StringEscapeUtils.escapeSql(usrId));
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
		catch (Throwable th)
		{
			logError(ERR_REGISTERING_USR, th);
		}
	}
	
	/**
	 * Crea la cookie del usuario.
	 * @param request
	 * @throws Throwable
	 */
	private Cookie createCookie(HttpServletRequest request) throws Throwable
	{
		Cookie cookie = null;
		
		// Completa los datos vacíos del usuario
		String dummy = StringPool.BLANK;
		String aboid = "0";
		
		try
		{
			// Crea la cookie
			cookie = PayCookieUtil.createUserDogTagCookie(
					request, usrId, usrName, dummy, email, dummy, dummy, dummy, dummy, aboid, null,
					dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy);
		}
		catch (Throwable th)
		{
			logError(ERR_CREATING_COOKIE, th);
		}
		
		return cookie;
	}
	
	private Cookie createVisitorIdCookie(HttpServletRequest request) throws Throwable
	{
		Cookie cookie = null;
		
		try
		{
			// Crea la cookie
			cookie = CookieUtil.createVisitorIdCookie(request, usrId);
		}
		catch (Throwable th)
		{
			logError(ERR_CREATING_COOKIE, th);
		}
		
		return cookie;
	}

	
	private void addRequestCookie(HttpServletRequest request) throws Throwable
	{
		if (_log.isDebugEnabled()) _log.debug("Creating user dogtag cookie...");
		request.setAttribute(COOKIE_HEADER_ATTR, createCookie(request).getValue());
	}
	
	public void addResponseCookie(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			response.setHeader(COOKIE_HEADER, PayCookieUtil.getCookieAsString(createCookie(request)));
			response.addHeader(COOKIE_HEADER, PayCookieUtil.getCookieAsString(createVisitorIdCookie(request)));
		}
		catch (Throwable th)
		{
			try { logError(ERR_ADDING_COOKIE, th); } catch (Throwable e) { /* Do nothing */}
		}
	}
	
	/**
	 * <p>Registra el error en el log del Tomcat y en IterMonitor y si lanza una excepción para abortar el proceso.</p>
	 * 
	 * @param errorMsg		El mensaje de error
	 * @param errorTrace	La traza de error
	 * @throws Throwable	Lanza {@code errorTrace} (o un {@code ServiceError} con
	 *                      el {@code errorMsg} en caso de que no se informe) después
	 *                      de registrar el error en IterMonitor
	 */
	private void logError(String errorMsg, Throwable errorTrace) throws Throwable
	{
		validProcess = false;
		IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), String.format(SSO_MSG_PREFIX, errorMsg), errorTrace);
		
		if (errorTrace == null)
		{
			_log.error(errorMsg);
			ErrorRaiser.throwIfError(errorMsg);
		}
		else
		{
			_log.error(errorTrace);
			throw errorTrace;
		}
	}
	
	/**
	 * <p>Indica si el proceso de registro / actualización del usuario en Iter fue correcto.</p>
	 * <p>Si retorna {@code false}, significa que ha ocurrido un error durante el proceso que
	 * debe de haber quedado registrado en el {@code MONITOR} y el usuario no se ha registrado (o actualizado)
	 * en el sistema y, por tanto, tampoco se ha añadido al {@code request} la cookie {@code USR_DOGTAG}.
	 * @return
	 */
	public boolean isValidProcess()
	{
		return validProcess;
	}
	
	/**
	 * Añade los datos extras pàra el login del iPad informados por el sistema de autorización externo
	 * al XML de salida.
	 * 
	 * @param iPadResponse el XML de salida para el login desde iPad.
	 */
	public void addExtraLoginData(Document iPadResponse)
	{
		if (Validator.isNotNull(iPadResponse) && Validator.isNotNull(extralogindata))
		{
			Document dataDoc = null;
			
			// Parsea los datos extra
			try
			{
				// Crea el documento auxiliar para almacenar los pares
				dataDoc = SAXReaderUtil.createDocument();
				Element dataElem = dataDoc.addElement("extradata");
				// Añade los pares clave-valor
				addPlistPairs(dataElem, extralogindata);
			}
			catch (Throwable th)
			{
				// Si ocurre un error, lo regisrtra en el Monitor y en el log del tomcat
				String msg = String.format(SSO_MSG_PREFIX, ERR_ADDING_EXTRADATA);
				_log.error(msg);
				_log.error(th);
				IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), msg, th);
				dataDoc = null;
			}
			
			
			// Añade los datos a la salida del iPad
			if (dataDoc != null)
			{
				// Recupera el elemento /plist/dict
				Node iPadElement = iPadResponse.getRootElement().selectSingleNode("/plist/dict");
				if (iPadElement != null)
				{
					// Añade los pares clave-valor a la salida del login del iPad
					List<Node> nodes = dataDoc.getRootElement().selectNodes("*");
					for (Node node : nodes)
					{
						node.detach();
						((Element) iPadElement).add((Element)node);
					}
				}
			}
		}
	}

	/**
	 * Añade de forma recursiva todo el contenido del objeto {@code json} al elemento {@code dataElement}.
	 * 
	 * @param dataElement el elemento XML en el que añadir las propiedades.
	 * @param json        el objeto json con las propiedades a añadir.
	 */
	private void addPlistPairs(Element dataElement, JSONObject json)
	{
		Iterator<?> keys = json.keys();
		while(keys.hasNext())
		{
			// Añade la clave de la propiedad
			String key = (String) keys.next();
			Element keyElem = dataElement.addElement(PLIST_KEY);
			keyElem.setText(key);
			
			// Añade el valor de la propiedad
			Object o = json.get(key);
			jsonToPlist(dataElement, o);
		}
	}

	/**
	 * Añade de forma recursiva todo el contenido del objeto {@code json} al elemento {@code dataElement}.
	 * 
	 * @param e el elemento XML en el que añadir las propiedades.
	 * @param a el array json con las propiedades a añadir.
	 */
	private void addPlistPairs(Element e, JSONArray a)
	{
		for (int i = 0; i < a.length(); i++)
		{
			Object o = a.get(i);
			jsonToPlist(e, o);
		}
	}
	
	/**
	 * <p>Añade el valor de la propiedad plist en función del tipo de dato contenido en el JSON.</p>
	 * <p>Diferencia entre los siguientes tipos de datos:</p>
	 * <ul>
	 *   <li>{@code <integer>} Integer o Long</li>
	 *   <li>{@code <real>} Float o Double</li>
	 *   <li>{@code <true/>} o {@code <false/>} Boolean</li>
	 *   <li>{@code <dict>} JSONObject</li>
	 *   <li>{@code <array>} JSONArray</li>
	 *   <li>{@code <string>} Cualquier otro tipo</li>
	 * </ul>
	 * @param e el elemento al que añadir el valor de la propiedad.
	 * @param o el objeto con el valor de la propiedad.
	 */
	private void jsonToPlist(Element e, Object o)
	{
		if (e != null && o != null)
		{
			// Propiedades de tipo entero o long se añaden como un elemento <integer>.
			if (o instanceof Integer || o instanceof Long)
			{
				e.addElement(PLIST_INTEGER).setText(o.toString());
			}
			// Propiedades de tipo float o double se añaden como un elemento <real>.
			else if (o instanceof Float || o instanceof Double)
			{
				e.addElement(PLIST_REAL).setText(o.toString());
			}
			// Propiedades de tipo booleano se añaden como un elemento <true /> o <false />.
			else if (o instanceof Boolean)
			{
				boolean b = Boolean.valueOf(o.toString());
				e.addElement(b ? PLIST_TRUE : PLIST_FALSE);
			}
			// Si es un objeto json, añade el elemento <dict> y procesa recursivamente el contenido.
			else if (o instanceof JSONObject)
			{
				Element dict = e.addElement(PLIST_DICT);
				addPlistPairs(dict, (JSONObject) o);
				
			}
			// Si es un array json, añade el elemento <array> y procesa recursivamente el contenido.
			else if (o instanceof JSONArray)
			{
				Element array = e.addElement(PLIST_ARRAY);
				addPlistPairs(array, (JSONArray) o);
			}
			// Cualquier otro dato se añade como string.
			else
			{
				e.addElement(PLIST_STRING).setText(o.toString());
			}
		}
	}
	
	public String getPwdMd5()
	{
		return this.pwdmd5;
	}
}
