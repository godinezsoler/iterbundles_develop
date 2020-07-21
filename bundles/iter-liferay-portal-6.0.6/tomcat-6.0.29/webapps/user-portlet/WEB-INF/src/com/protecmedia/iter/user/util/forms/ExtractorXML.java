package com.protecmedia.iter.user.util.forms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.user.service.CaptchaFormLocalServiceUtil;
import com.protecmedia.iter.user.util.UserUtil;


public class ExtractorXML {

	private static Log _log = LogFactoryUtil.getLog(ExtractorXML.class);
	
	private static final Pattern newsletterNameChecker = Pattern.compile("^iter_newsletter_[0-9a-f]{32}$");
	
	// Consulta para obtener la definicion de los campos del formulario que nos llega
	private static final String GET_INPUTS_CONFIGURATION = new StringBuffer()	
	.append("SELECT ft.formid, f.name formName, ft.name tab, ft.tabid, fd.fieldid,     \n")
	.append("IFNULL(d.fieldtype, ud.fieldtype) fieldtype, fd.htmlname,                 \n")
	// Labelbefore
    .append("ExtractValue(labelbefore, \"/labelbefore/textlabel/text()\") labelbefore, \n")
   	// Labelafter
	.append("ExtractValue(labelafter, \"/labelafter/textlabel/text()\") labelafter     \n")
	.append("FROM form f                                                               \n")
	.append("INNER JOIN formtab     ft ON ft.formid = f.formid                         \n")
	.append("INNER JOIN formfield   fd ON fd.tabid = ft.tabid                          \n")
	.append("LEFT JOIN  datafield    d ON d.datafieldid = fd.datafieldid               \n")
	.append("LEFT JOIN  userprofile up ON up.profilefieldid = fd.profilefieldid        \n")
	.append("LEFT JOIN  datafield   ud ON ud.datafieldid = up.datafieldid              \n")
	.append("WHERE f.formid = '%s'                                                     \n")
	// Buscamos por el id del campo o por su nombre
	.append("  AND (fd.fieldid in(%s) OR fd.htmlname in(%s))                           \n")
	// Ordeno por orden de pestania y tambien por su id porque el orden de pestania a veces no es unico.  
	.append("ORDER BY ft.taborder, ft.tabid, fd.fieldorder, fd.fieldid").toString();
	
	private static final String GET_FORM_USECAPTCHA = new StringBuffer()
    .append("SELECT f.usechaptcha, f.groupid \n")
    .append("FROM form f 					 \n")
    .append("WHERE f.formid = '%s' \n").toString();

	private static final String GET_FORM_FIELDS_REQUIRED = new StringBuffer()
    .append("SELECT IF(CHAR_LENGTH(ff.htmlname > 0), ff.htmlname, ff.fieldid) fieldid \n")
    .append("FROM formfield ff 							   \n")
    .append("INNER JOIN formtab ft ON ff.tabid = ft.tabid  \n")
    .append("INNER JOIN form f ON ft.formid = f.formid     \n")
    .append("WHERE f.formid = '%s' AND ff.required=1 \n").toString();
	
	private static final String GET_NEWSLETTER_FORM_FIELD = new StringBuilder()
	.append(" SELECT formfield.fieldid                                                        \n")
	.append(" FROM form                                                                       \n")
	.append(" INNER JOIN formtab on formtab.formid = form.formid                              \n")
	.append(" INNER JOIN formfield on formfield.tabid = formtab.tabid                         \n")
	.append(" INNER JOIN userprofile ON userprofile.profilefieldid = formfield.profilefieldid \n")
	.append(" WHERE form.formid = '%s' AND userprofile.fieldname = ").append(StringUtil.apostrophe(UserUtil.PRF_FIELD_NEWSLETTER))
	.toString();
	
	/**
	 * 
	 * @param request
	 * @param camposObligatorios
	 * @return
	 * @throws Exception 
	 */
	public static Document createXML(HttpServletRequest request) throws Exception {	
		return createXML(request, false, null);
	}	
	
	/**
	 * 
	 * @param request
	 * @param generarXML
	 * @param camposObligatorios
	 * @return
	 * @throws Exception 
	 */	
	public static Document createXML(HttpServletRequest request, boolean generarXML) throws Exception {		
		return createXML(request, generarXML, null);
	}

	/**
	 * 
	 * @param requestAux
	 * @param generarXML
	 * @param camposObligatorios
	 * @param adjuntos
	 * @return
	 * @throws Exception 
	 */
	public static Document createXML(HttpServletRequest request, boolean generarXML, Map<String, ArrayList> adjuntos) throws Exception
	{		
		final String formId = request.getParameter("formid");
		
		// Miramos si hay que usar el captcha
		boolean useCaptcha = false;
		
		String sql = String.format(GET_FORM_USECAPTCHA, formId);
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuffer( "Query: ").append(sql));
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);	
		ErrorRaiser.throwIfNull(result);
		
		String attrCaptcha = (String) request.getParameter(IterKeys.USE_CAPTCHA);
		if( Validator.isNotNull(attrCaptcha) )
		{
			useCaptcha = GetterUtil.getBoolean(attrCaptcha);
		}
		else
		{
			useCaptcha = GetterUtil.getBoolean(XMLHelper.getTextValueOf(result, "/rs/row/@usechaptcha"));
		}

		final Long groupId =XMLHelper.getLongValueOf(result, "/rs/row/@groupid");
		_log.debug(new StringBuffer("Use catpcha: ").append(useCaptcha ? "true" : "false"));
		 
		List<String> camposObligatorios = null;
		
		String requiredFields = request.getParameter(IterKeys.REQ_FIELDS);
		if( requiredFields!=null )
		{
			camposObligatorios = new ArrayList<String>();
			if( !requiredFields.isEmpty() )
				camposObligatorios.addAll( Arrays.asList(requiredFields.split(StringPool.COMMA)) );
		}
		else
			camposObligatorios = getformFieldsRequired(formId);
		
		final boolean comprobarObligatorios = (Validator.isNotNull(camposObligatorios) && !camposObligatorios.isEmpty());
		// Request no puede ser null
		if (Validator.isNull(request)){ 
			_log.error("Request is null");
		}
		ErrorRaiser.throwIfNull(request);
		
		// Documento que se devolvera
		Document xml = null;
		
		// Clases que se utilizan para leer el multipart
		ServletFileUpload upload = new ServletFileUpload();	
		FileItemIterator iterator = upload.getItemIterator(request);
		
		// Contendra los inputs del formulario. Lo hacemos aqui para ahorrarnos recorrerlos de nuevo si se quiere generar el xml
		// Sera un hashmap de arraylist para los inputs de tipo texto y hasmpa de arraylist de array para los inputs de tipo file
		HashMap<String, ArrayList<Object>> inputs = new HashMap<String, ArrayList<Object>>();
		// Listado de newsletter a las que se quiere suscribir el usuario
		List<String> newsletters = new ArrayList<String>();
		// Claves, nos sirve para la posterior consulta sql. Lo hacemos aqui para ahorrarnos recorrerlos de nuevo si se quiere generar el xml
		StringBuilder keys = new StringBuilder();	
		
		final String codificacion = "UTF8";
		// Token del captcha
		String userResponseCaptcha = null;
		
		// Recorremos los campos del formulario multipart que nos llegan
		_log.trace("Going over fields");
		while (iterator.hasNext()) {
			FileItemStream item = iterator.next();
			InputStream stream = item.openStream();
			
			// Nombre del input
			final String fileName = item.getFieldName();			
			
			// Comprobamos si el input que nos llega corresponde al captcha TODO iterkeys
			if (useCaptcha && fileName.equals("g-recaptcha-response"))//(fileName.equals(IterKeys.RECAPTCHA_CHALLENGE_FIELD_NAME) || fileName.equals(IterKeys.RECAPTCHA_RESPONSE_FIELD_NAME))){
			{
				// Obtenemos el valor del captcha
				String value = getStringFromInputStream(stream);
				ErrorRaiser.throwIfNull(value);	
				userResponseCaptcha = new String(value.getBytes(), codificacion);
			}
			// El input no es el captcha
			else
			{
				// Id del item
				final String itemId = item.getFieldName();
				if (Validator.isNull(itemId)){
					_log.error("ItemId is null");
				}			
				ErrorRaiser.throwIfNull(itemId);	
				
				ArrayList<Object> valoresDeItemsConElMismoId = null;
				
				// Si ya tenemos el id del item en inputs
				if (inputs.containsKey(itemId))
				{
					valoresDeItemsConElMismoId = (ArrayList)inputs.get(itemId);
				// El id que llega es nuevo
				}
				else
				{
					valoresDeItemsConElMismoId = new ArrayList<Object>();					
				}				
				
				// No es un archivo
				if (item.isFormField()) 
				{					
					String value = getStringFromInputStream(stream);
					if (Validator.isNull(value))
					{
						_log.debug("String value is null");
					}
					ErrorRaiser.throwIfNull(value);
					
					// Si es un check de newsletter, añade el scheduleId al listado de newsletters
					Matcher m = newsletterNameChecker.matcher(itemId);
					if (m.find())
					{
						newsletters.add(value);
					}
					// Si no...
					else
					{
						valoresDeItemsConElMismoId.add(new String(value.getBytes(), codificacion));
						// Almacenamos el input (nombre, valor)
						inputs.put(itemId, valoresDeItemsConElMismoId);
						
						// Vamos borrando los encontrados
						if( comprobarObligatorios && camposObligatorios.contains(item.getFieldName()) && !new String(value).isEmpty() )
							camposObligatorios.remove(item.getFieldName());
						
						// Recogemos la clave para una consulta sql posterior
						keys.append("'");
						// Escapamos el codigo sql para evitar problemas en la consulta
						keys.append(StringEscapeUtils.escapeSql(item.getFieldName()));
						keys.append("'");
						keys.append(",");
					}
				}
				// Es un archivo
				else 
				{
					// Para el caso de los archivos necesitamos un array para guardar su nombre y su UUID
					String[] datosDelArchivo = new String[2];
					// Nombre del archivo
					String nombreArchivo = FilenameUtils.getName(item.getName());
					
					// El nombre del archivo viene vacio si el usuario no ha usado el input file. No es un error
					if (Validator.isNull(nombreArchivo))
					{
						_log.debug(new StringBuffer("User did not use input file with formfield.fieldid: ").append(itemId).toString());
					}
					else
					{
						// Si el nombre no es valido, le forzamos uno. Cuando se vaya a escribir en disco, se le concatenan cadenas para que el archivo sea unico
						if (!isFileValid(nombreArchivo)){							
							_log.debug(new StringBuffer("File name: \"").append(nombreArchivo).append("\" is not correct. We change it to a valid"));
							nombreArchivo = ".bin";
						}
						//ErrorRaiser.throwIfFalse(isAValidName);				
						
						datosDelArchivo[0] = nombreArchivo;
						// Identificador unico para relacionar el archivo con su item del xml. Se debe usar la clase SQLQueries en lugar de java.util.UUID
						String uuid = SQLQueries.getUUID();	
						if (Validator.isNull(uuid))
						{
							_log.error("UUID generated is null");
						}
						ErrorRaiser.throwIfNull(uuid);		
						
						datosDelArchivo[1] = uuid;
						
						// Modificamos el objeto pasado por referencia para que tenga los adjuntos con su UUID
						if (Validator.isNotNull(adjuntos))
						{
							//adjuntos.put(uuid, item) ;					
							/* Si guardamos directamente el item, cuando en otra clase vaya a recuperar el contenido del archivo (inputStream) 
							 * dara error porque su puntero ya ha avanzado. Para solucionar esto, nos creamos un array de objetos para mandar 
							 * en la primera posicion el nombre del archivo y en la segunda posicion el inputStreamReader */
							ArrayList datosAdjunto = new ArrayList();
							datosAdjunto.add(nombreArchivo);
							/* En lugar de duplicar el contenido se intento pasar como referencia pero daba siempre una FileItemStream.ItemSkippedException
							 * porque el puntero ya habia pasado */
							byte[] bytes = IOUtils.toByteArray(item.openStream());
							datosAdjunto.add(bytes);
							adjuntos.put(uuid, datosAdjunto);
						}
						
						valoresDeItemsConElMismoId.add(datosDelArchivo); 	           
		                inputs.put(itemId, valoresDeItemsConElMismoId);		                
		                
		                // Vamos borrando los encontrados
		                if(comprobarObligatorios && camposObligatorios.contains(item.getFieldName())){
		                	camposObligatorios.remove(item.getFieldName());
		                }    
		                
		                // Recogemos la clave para una consulta sql posterior
		                keys.append("'");
		                // Escapamos el codigo sql para evitar problemas en la consulta
		                keys.append(StringEscapeUtils.escapeSql(item.getFieldName()));
		                keys.append("'");
		                keys.append(",");		                
					}					
				}		
			}
		}
		
		
		// Si se usa captcha, lo comprobamos
		if (useCaptcha)
		{
			_log.trace("In captcha validation");
			boolean correctCaptcha = false;
			try
			{
				// Si hay que usar el captcha y no es correcto no seguimos
				correctCaptcha = CaptchaFormLocalServiceUtil.isValid(groupId, userResponseCaptcha, request.getRemoteAddr());
			}
			catch (Throwable t)
			{
				_log.debug(t);
			}
			finally
			{
				ErrorRaiser.throwIfFalse(correctCaptcha, IterErrorKeys.XYZ_ITR_E_CAPTCHAINVALID_ZYX);
			}
		}

		
		if (Validator.isNull(keys)){
			_log.error("Keys is null");
		}
		ErrorRaiser.throwIfNull(keys);
		// Borramos la ultima coma que no es necesaria para formar bien posteriormente el sql
		String claves = keys.length() > 0 ? keys.toString().substring(0, keys.length()-1) : null;
		
		// No tiene todos los campos obligatorios
		ErrorRaiser.throwIfFalse( !(comprobarObligatorios && !camposObligatorios.isEmpty()), IterErrorKeys.XYZ_ITR_E_REQUIREDFIELD_ZYX);
		
		if (Validator.isNull(formId)){
			_log.error("formId is null");
		}
		ErrorRaiser.throwIfNull(formId);
		
		
		if (generarXML)
		{	
			_log.trace("Generating XML");
			
			// Si no hay claves, es que sólo se han enviado newsletters
			if (Validator.isNull(claves))
			{
				Document newsletterField = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_NEWSLETTER_FORM_FIELD, formId));
				claves = StringUtil.apostrophe(XMLHelper.getTextValueOf(newsletterField, "/rs/row[1]/@fieldid"));
			}
			
			// Ejecutamos la consulta, las columnas labelbefore y labelafter contienen html
			_log.debug(new StringBuffer( "Query: ").append(String.format(GET_INPUTS_CONFIGURATION, formId, claves, claves)));
			result = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_INPUTS_CONFIGURATION, formId, claves, claves), new String[]{"labelbefore", "labelafter"});			
			ErrorRaiser.throwIfNull(result);
						
			final String nombreFormulario = XMLHelper.getTextValueOf(result, "/rs/row[1]/@formName");
			if (Validator.isNull(nombreFormulario)){
				_log.error("formName is null");
			}
			ErrorRaiser.throwIfNull(nombreFormulario);
			
			// Creamos el xml
			xml = SAXReaderUtil.read("<formdata/>");
			Element formData = xml.getRootElement();					
			
			formData.addAttribute("formid", formId);
			formData.addAttribute("formname", nombreFormulario);
			
			Element deliveryInfo = formData.addElement("delivery-info");
			Element dateSent = deliveryInfo.addElement("date-sent");
			final SimpleDateFormat sDF = (new SimpleDateFormat("yyyyMMddHHmmssSSS"));
			dateSent.addText(sDF.format(Calendar.getInstance().getTime()));
			
			
			// Obtenemos los datos del usuario de cookie, si esta no llega, el usuario sera anonimo			
			final String cookie = PayCookieUtil.getPlainCookie(request, IterKeys.COOKIE_NAME_USER_DOGTAG);					
			if (Validator.isNotNull(cookie))
			{
				_log.trace("Getting infor from the cookie");
				Map<String, String> cookieValues = PayCookieUtil.getCookieAsMap(cookie, IterKeys.COOKIE_NAME_USER_DOGTAG);
				
				Element sendingUser = deliveryInfo.addElement("sending-user");
				
				Element auxElement = sendingUser.addElement("usrid");
				auxElement.addText(cookieValues.get(IterKeys.COOKIE_NAME_USER_ID));

				auxElement = sendingUser.addElement("usrname");
				auxElement.addText(cookieValues.get(IterKeys.COOKIE_NAME_USER_NAME));
				
				auxElement = sendingUser.addElement("usremail");						
				auxElement.addText(cookieValues.get(IterKeys.COOKIE_NAME_USER_EMAIL));
				
				String usr1stname = cookieValues.get(IterKeys.COOKIE_NAME_USR_1ST_NAME);
				if (Validator.isNotNull(usr1stname))
				{
					auxElement = sendingUser.addElement("usr1stname");						
					auxElement.addText(usr1stname);
				}
				
				String usrlastname = cookieValues.get(IterKeys.COOKIE_NAME_USR_LASTNAME);
				if (Validator.isNotNull(usrlastname))
				{
					auxElement = sendingUser.addElement("usrlastname");						
					auxElement.addText(usrlastname);
				}
				
				String usrlastname2 = cookieValues.get(IterKeys.COOKIE_NAME_USR_LASTNAME_2);
				if (Validator.isNotNull(usrlastname2))
				{
					auxElement = sendingUser.addElement("usrlastname2");
					auxElement.addText(usrlastname2);
				}
				
				String usravatarurl =  cookieValues.get(IterKeys.COOKIE_NAME_USR_AVATAR_URL);
				if (Validator.isNotNull(usravatarurl))
				{
					auxElement = sendingUser.addElement("usravatarurl");						
					auxElement.addText(usravatarurl);
				}
				
				String aboid =  cookieValues.get(IterKeys.COOKIE_NAME_ABO_ID);
				if (Validator.isNotNull(aboid))
				{
					auxElement = sendingUser.addElement("aboid");						
					auxElement.addText(aboid);
				}
				
				String sessid =  cookieValues.get(IterKeys.COOKIE_NAME_SESS_ID);
				if (Validator.isNotNull(sessid))
				{
					auxElement = sendingUser.addElement("sessid");						
					auxElement.addText(sessid);
				}
			}				
					
			// Recorremos los inputs de la definicion de base de datos
			Element fieldsGroup = null;
			String lastTabName 	= "";
			List<Node> nodes 	= result.selectNodes("/rs/row");
			
			// Tipos de datos, hay algunos mas, pero no afecta a nuestra logica
			final String binaryType     = "binary";
			final String arrayType      = "array";
			final String binlocatorType = "memory";	
			
			_log.trace("Going over the definition fields");
			for (int i = 0; i < nodes.size(); i++)
			{
				final String fieldId               = XMLHelper.getTextValueOf(nodes.get(i), "@fieldid");					
				final String fieldType             = XMLHelper.getTextValueOf(nodes.get(i), "@fieldtype");		
				final String fieldLabelBeforeValue = XMLHelper.getTextValueOf(nodes.get(i), "labelbefore");
				final String fieldLabelAfterValue  = XMLHelper.getTextValueOf(nodes.get(i), "labelafter");
				
				// Si no es el campo especial "newsletter", lo extrae
				if (!UserUtil.PRF_FIELD_NEWSLETTER.equals(fieldType))
				{
					final String tabName = XMLHelper.getTextValueOf(nodes.get(i), "@tab");
					if (Validator.isNull(tabName)){
						_log.error("tabName is null");
					}
					ErrorRaiser.throwIfNull(tabName);
					
					if (!lastTabName.equals(tabName)){
						fieldsGroup = formData.addElement("fieldsgroup");
						fieldsGroup.addAttribute("name", tabName);
						lastTabName = tabName;
					}
					
					ArrayList<?> itemData = null;
					
					// Recuperamos el input por su id
					if (inputs.containsKey(fieldId))
					{
						itemData = (ArrayList<?>)inputs.get(fieldId);
						
					// No se ha encontrado el input por su id, es posible que se pasase el htmlname
					}
					else
					{
						final String htmlName = XMLHelper.getTextValueOf(nodes.get(i), "@htmlname");
						
						if (Validator.isNotNull(htmlName))
						{						
							if (inputs.containsKey(htmlName))
							{
								itemData = (ArrayList<?>)inputs.get(htmlName);
							}
							else
							{
								_log.error("Impossible to recover formfield by its id and htmlname");
							}
							
							// No se ha podido recuperar el formfield ni por el id ni por su html
						}
						else
						{
							_log.error("Impossible to recover formfield by its id and its htmlname was null");
						}
					}				
					ErrorRaiser.throwIfNull(itemData);
						
					final int datosPorItem = itemData.size();
					
					// Si el input es de tipo array, su xml tiene otro tipo de construccion, se crea un item con sus n valores.
					if (fieldType.equals(arrayType))
					{
						
						// Se añade el field correspondiente
						Element field = fieldsGroup.addElement("field");
						field.addAttribute("id", fieldId);
						field.addAttribute("fieldtype", fieldType);
						
						if (Validator.isNotNull(fieldLabelBeforeValue)){
							Element labelBefore = field.addElement("labelbefore");
							labelBefore.addCDATA(fieldLabelBeforeValue);	
						}
						if (Validator.isNotNull(fieldLabelAfterValue)){
							Element fieldLabelAfter = field.addElement("labelafter");
							fieldLabelAfter.addCDATA(fieldLabelAfterValue);
						}	
						
						Element data = field.addElement("data");
						for (int d = 0; d < datosPorItem; d++){
							Element value = data.addElement("value");
							value.addCDATA((String)itemData.get(d));	
						}	
						
					// Input con un unico valor, se crean tantos input como vengan
					}
					else
					{
						for (int d = 0; d < datosPorItem; d++)
						{
							String[] datosArchivo = null;
							
							if (fieldType.equals(binaryType))
							{
								try
								{
									datosArchivo = (String[])itemData.get(d);
								}
								catch (Exception e)
								{
									// Si es falla el casting es porque se trata de un binario sin fichero escogido
									continue;
								}
							}
							// Se añade el field correspondiente
							Element field = fieldsGroup.addElement("field");
							field.addAttribute("id", fieldId);
							field.addAttribute("fieldtype", fieldType);
							
							if (Validator.isNotNull(fieldLabelBeforeValue))
							{
								Element labelBefore = field.addElement("labelbefore");
								labelBefore.addCDATA(fieldLabelBeforeValue);	
							}
							if (Validator.isNotNull(fieldLabelAfterValue))
							{
								Element fieldLabelAfter = field.addElement("labelafter");
								fieldLabelAfter.addCDATA(fieldLabelAfterValue);
							}	
							
							Element data = field.addElement("data");
							
							// El input es tipo binario
							if (fieldType.equals(binaryType))
							{
								Element binary 	= data.addElement("binary");
								Element name 	= binary.addElement("name");
								
								// Datos del archivo (nombre y UUID)
								datosArchivo = (String[])itemData.get(d);
								ErrorRaiser.throwIfNull(datosArchivo);
								
								name.addText((String)datosArchivo[0]);
								Element binlocator = binary.addElement("binlocator");
								binlocator.addAttribute("type", binlocatorType);
								// Rutal del archivo
								binlocator.addText((String)datosArchivo[1]);
								
								// El input no es de tipo binario
							}
							else
							{
								// Input de tipo no binario con multiples valores (por ejemplo un select con varios options seleccionados)
								Element value = data.addElement("value");
								value.addCDATA((String)itemData.get(d));															
							}					
						}
					}
				}
			}
			
			// NEWSLETTERS
			if (newsletters.size() > 0)
			{
				Element row = formData.addElement("selectednewsletters");
				Element newsletterElement = row.addElement("newsletters");
				for (String newsletterId : newsletters)
				{
					newsletterElement.addElement("subscription").addAttribute("scheduleid", newsletterId);
				}
			}
			
			_log.debug("Generated xml: " + xml.asXML());	
		}			
		return xml; 		 
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	// Detecta si es un nombre de archivo valido
	private static boolean isFileValid(String input){
	    final String re = "^[^|?<>:*&%\"]+$";
	    final Pattern pattern = Pattern.compile(re);
	    return pattern.matcher(input).matches();
	}
	
	
	private static List<String> getformFieldsRequired(String formId){
        List<String> result = new ArrayList<String>();
        
        _log.debug(new StringBuffer("Query: ").append(String.format(GET_FORM_FIELDS_REQUIRED, formId)));
        List<Object> resutlQuery = PortalLocalServiceUtil.executeQueryAsList(String.format(GET_FORM_FIELDS_REQUIRED, formId));
        if(resutlQuery != null && !resutlQuery.isEmpty()){
               for (Object object : resutlQuery) {
                      result.add(object.toString());
               }
        }
        
        return result;
  }
	
	private static String getStringFromInputStream(InputStream is) {
		 
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
 
		String line;
		try {
 
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
 
		return sb.toString();
 
	}
	
}
