package com.protecmedia.iter.user.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.base.service.CaptchaLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;

public class FormUtil
{
	private static Log _log = LogFactoryUtil.getLog(FormUtil.class);
	
	private static final String GET_PHONE_FIELD_ID = String.format(new StringBuilder( 
		"SELECT FormField.fieldid															\n").append(
		"FROM FormField																		\n").append(
		"INNER JOIN FormTab ON FormField.tabId = FormTab.tabId								\n").append(
		"INNER JOIN UserProfile ON userprofile.profilefieldid = formfield.profilefieldid	\n").append(
		"  WHERE formId = '%%s'																\n").append(
		"    AND userprofile.fieldname = '%s'												\n").append(
		"    AND structured	\n").toString(), UserUtil.PRF_FIELD_TELEPHONE);
	    
	static private final String GET_FORM 			= new StringBuilder(
			"SELECT groupid, formid, name, navigationtype, css, usechaptcha, successsend AS ok, errorsend AS ko, \n"	).append(
			"		submitlabel, usesubmit, resetlabel, restricted, GET_FORM_PRODUCTS('%s') products, \n"				).append(
			"		oklabel, cncllabel, confirmpaneltitle \n" 															).append(  
			"FROM form WHERE formid='%s'"																				).toString();
	
	static private final String GET_REGISTER_FORM 	= new StringBuilder(
			"SELECT groupid, formid, name, navigationtype, css, usechaptcha, successsend AS ok, errorsend AS ko, \n"	).append(
					"submitlabel, usesubmit, resetlabel, restricted, oklabel, cncllabel, confirmpaneltitle\n"			).append( 
			"FROM form \n"																								).append(
			"	WHERE groupid=%s and formtype='registro'"																).toString();
	
	static private final String GET_TABS 			= "SELECT tabid, name FROM formtab WHERE formid='%s' ORDER BY taborder";
	
	static private final String GET_FIELDS 			= new StringBuilder()
	.append("SELECT userprofile.fieldname, userprofile.structured, fieldid,formfield.datafieldid,htmlname, IFNULL(df.fieldtype, dfup.fieldtype) fieldtype, \n"	)
	.append("inputctrl, validator, labelbefore,labelafter, tooltip, defaultvalue, editable,                 \n")
	.append("formfield.required, needconfirm, repeatable, css                                               \n")
	.append("FROM formfield                                                                                 \n")
	.append("LEFT JOIN datafield df   ON formfield.datafieldid = df.datafieldid                             \n")
	.append("LEFT JOIN userprofile    ON formfield.profilefieldid = userprofile.profilefieldid              \n")
	.append("LEFT JOIN datafield dfup ON userprofile.datafieldid = dfup.datafieldid                         \n")
	.append("	WHERE tabid='%s'                                                                            \n")
	.append("	ORDER BY fieldorder                                                                         \n")
	.toString();
	
//	-- Campos obligatorios del formulario de registro de un grupo, que tengan dichos "fieldIds", excepto
//	-- aquellos con un determinado fieldName
	static private final String GET_REQUIRED_FIELDS 			= new StringBuilder(
			"SELECT IF(CHAR_LENGTH(formfield.htmlname)>0, formfield.htmlname, formfield.fieldid) fieldid\n"											).append(
			"FROM formfield \n" 																						).append(
			"INNER JOIN formtab ON (formfield.tabid = formtab.tabid) \n" 												).append(
			"INNER JOIN form ON (form.formid = formtab.formid) \n" 														).append(
			"INNER JOIN userprofile ON (formfield.datafieldid = userprofile.datafieldid) \n"							).append(
			"	WHERE form.groupId = %1$d AND form.formtype = 'registro' AND formfield.required=1 \n"					).append(
			"	%2$s AND fieldname NOT IN (%3$s) "											).toString();
	
	static private final String GENERIC_FORM_SERVLET 	= "/user-portlet/FormReceiver";
	
	static private final String ELEM_FORM 				= "form";
	static private final String ELEM_SERVLET 			= "servlet";
	static private final String ELEM_INVALID_FIELD_MSG 	= "invalidfieldmsg";
	static private final String ELEM_RESTRICTIONS 		= "restrictions";
	static private final String ELEM_FIELDS_GRP 		= "fieldsgroup";
	static private final String ELEM_FIELD 				= "field";
	static private final String ELEM_LBL_BEFORE 		= "labelbefore";
	static private final String ELEM_TEXT_LBL 			= "textlabel";
	static private final String ELEM_LBL_AFTER 			= "labelafter";
	static private final String ELEM_TOOLTIP 			= "tooltip";
	static private final String ELEM_INPUT_CTRL 		= "inputctrl";
	static private final String ELEM_DEFAULT_VALUE 		= "defaultvalue";
	static private final String ELEM_LANGUAGE 			= "language";
	static private final String ELEM_MINDATE 			= "mindate";
	static private final String ELEM_MAXDATE 			= "maxdate";
	static private final String ELEM_OPTIONS 			= "options";
	static private final String ELEM_OPTION 			= "option";
	static private final String ELEM_VALIDATOR 			= "validator";
	
	static private final String ATTR_ID 				= "id";
	static private final String ATTR_CSS 				= "css";
	static private final String ATTR_USE_CAPTCHA 		= "usecaptcha";
	static private final String ATTR_CAPTCHA_KEY 		= "captchakey";
	static private final String ATTR_CAPTCHA_LANG 		= "captchalang";
	static private final String ATTR_CAPTCHA_THEME 		= "captchatheme";
	static private final String ATTR_FIELDSGRP_NAV 		= "fieldsgroup-navigation";
	static private final String ATTR_USE_SUBMIT 		= "use-submit";
	static private final String ATTR_SUBMIT_LBL 		= "submitlabel";
	static private final String ATTR_RESET_LBL 			= "resetlabel";
	static private final String ATTR_NEXT_LBL 			= "nextlabel";
	static private final String ATTR_PREV_LBL 			= "prevlabel";
	static private final String ATTR_REFERER 			= "referer";
	static private final String ATTR_NAME 				= "name";
	static private final String ATTR_FIELD_TYPE 		= "fieldtype";
	static private final String ATTR_EDITABLE 			= "editable";
	static private final String ATTR_REQUIRED 			= "required";
	static private final String ATTR_NEED_CONFIRM 		= "needconfirm";
	static private final String ATTR_REPEATABLE			= "repeatable";
	static private final String ATTR_LINK_URL 			= "linkurl";
	static private final String ATTR_LINK_TARGET 		= "linktarget";
	static private final String ATTR_TYPE 				= "type";
	static private final String ATTR_SIZE 				= "size";
	static private final String ATTR_MULTIPLE 			= "multiple";
	static private final String ATTR_SELECTED 			= "selected";
	static private final String ATTR_VALUE 				= "value";
	static private final String ATTR_FORMAT 			= "format";
	static private final String ATTR_MIN 				= "min";
	static private final String ATTR_MAX 				= "max";
	static private final String ATTR_OK 				= "oklabel";
	static private final String ATTR_CANCEL 			= "cncllabel";
	static private final String ATTR_CONFIRM_PANEL_TITLE= "confirmpaneltitle";
	static private final String ATTR_RESTRICTED 		= "restricted";
	static private final String ATTR_PRODUCTS 			= "products";
	
	static private final String INPUTCTRL_TYPE_RADIO_BUTTON		= "radiobutton";
	static private final String INPUTCTRL_TYPE_LIST_CTRL		= "listctrl";
	static private final String INPUTCTRL_TYPE_DROPDOWN_LIST	= "dropdownlist";
	static private final String INPUTCTRL_TYPE_CHECK_BOX		= "checkbox";
	
	
	static private final String GET_REFERER_FUNC 		= "get_referer()";
	
	static public final String KEY_NAME="name";
	static public final String KEY_ATTACH="attach";
	
	
	static public Document getFormXml( String formid, HttpServletRequest rq, String[] fields, String[] requiredFields ) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, IOException
	{
		String query = String.format(GET_FORM, formid, formid);
		
		return buildFormXml(query, rq, GENERIC_FORM_SERVLET, fields, requiredFields, true);
	}
	
	static public Document getFormXml( long scopeGrpid, HttpServletRequest rq, String endPoint, String[] fields, String[] requiredFields) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, IOException
	{
		return getFormXml( scopeGrpid, rq, endPoint, fields, requiredFields, true);
	}
	
	static public Document getFormXml( long scopeGrpid, HttpServletRequest rq, String endPoint, String[] fields, String[] requiredFields, boolean defaultValues) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, IOException
	{
		String query = String.format(GET_REGISTER_FORM, scopeGrpid);
		
		return buildFormXml(query, rq, endPoint, fields, requiredFields, defaultValues);
	}
	
	
	static public void updateCSSAttrWithEditProfileIntent(String editProfileIntent, Document dom)
	{
		String cssModifier = StringPool.BLANK;
		if (IterKeys.EDIT_PROFILE_INTENT_FORGOT.equals(editProfileIntent))
			cssModifier = "form-reset-credentials";
		else if ((IterKeys.EDIT_PROFILE_INTENT_REGISTRY.equals(editProfileIntent)))
			cssModifier = "form-register-user";
		else if ((IterKeys.EDIT_PROFILE_INTENT_EDITION.equals(editProfileIntent)))
			cssModifier = "form-update-profile";

		if (!cssModifier.isEmpty())
		{
			String cssValue = XMLHelper.getTextValueOf(dom, "/form/@css", StringPool.BLANK);
			if (!cssValue.isEmpty())
				cssModifier = cssValue.concat(" ").concat(cssModifier);
				
			dom.getRootElement().addAttribute(ATTR_CSS, cssModifier);
		}
	}
	
	/**
	 * 
	 * @param groupId
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	static public Document getEditUserProfileFormXml(long groupId, HttpServletRequest request, String userId, String[] fields, String editProfileIntent) throws Exception
	{
		boolean defaultValues = !IterKeys.EDIT_PROFILE_INTENT_EDITION.equals(editProfileIntent);
		
		// Si existe la cookie como atributo se añade a la URL del servlet para que este también la tenga
		String endPoint = String.format("/user-portlet/edit-user-profile?%s=false", IterKeys.USE_CAPTCHA);
		
		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
		String plainCookie = String.valueOf( PublicIterParams.get(originalRequest, IterKeys.COOKIE_NAME_USERID_DOGTAG) );
		if (Validator.isNotNull(plainCookie))
			endPoint = String.format("%s&%s=%s", endPoint, IterKeys.COOKIE_NAME_USERID_DOGTAG, plainCookie);
		
		String mode = GroupConfigTools.getGroupConfigFieldFromDB(groupId, GroupConfigConstants.FIELD_REGISTER_CONFIRMATION_MODE);
		boolean isOTP 	= GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(mode);
		boolean isEMAIL	= GroupConfigConstants.REGISTER_CONFIRMATION_MODE.email.toString().equals(mode);

		// Campos que NO serán obligatorios en el formulario de edición
		List<String> nonRequiredFilesList = new ArrayList<String>();
		
		// Si la edición NO proviene de un OLVIDO de contraseña, la contraseña NO será obligatoria
		if (!IterKeys.EDIT_PROFILE_INTENT_FORGOT.equals( editProfileIntent ))
			nonRequiredFilesList.add( StringUtil.apostrophe(UserUtil.PRF_FIELD_USRPWD) );

		if ( !IterKeys.EDIT_PROFILE_INTENT_REGISTRY.equals(editProfileIntent) )
		{
			// Si la edición NO proviene de un COMPLETADO DE REGISTRO POR REDES SOCIALES, el aboid NO será obligatorio, ni el validador ni el avatar.
			nonRequiredFilesList.add( StringUtil.apostrophe(UserUtil.PRF_FIELD_ABOID) );
			nonRequiredFilesList.add( StringUtil.apostrophe(UserUtil.PRF_FIELD_EXTRAVALIDATOR) );
			nonRequiredFilesList.add( StringUtil.apostrophe(UserUtil.PRF_FIELD_AVATARURL) );
		}
		else
		{
			// Si la edición proviene de un COMPLETADO DE REGISTRO POR REDES SOCIALES, el XtraValidator será obligatorio si y solo sí, el aboid también lo es
			String query = String.format(String.format(IterRegisterQueries.GET_COUNT_FORMFIELD_REGISTRY_REQUIRED, UserUtil.PRF_FIELD_ABOID), groupId);
			Document domAboid = PortalLocalServiceUtil.executeQueryAsDom(query);
			
			if ( XMLHelper.getLongValueOf(domAboid, "/rs/row/@numregistries") == 0 )
				nonRequiredFilesList.add( StringUtil.apostrophe(UserUtil.PRF_FIELD_EXTRAVALIDATOR) );
		}
		
		String nonRequiredFiles = StringUtil.merge(nonRequiredFilesList.toArray(new String[nonRequiredFilesList.size()])); 
		
		String reqQuery = (fields != null) ? String.format(" AND formfield.fieldid IN ('%s') ", StringUtil.merge(fields,"','")) : "";
		String query 	= String.format(GET_REQUIRED_FIELDS, groupId, reqQuery, nonRequiredFiles);
		Document domRequired = PortalLocalServiceUtil.executeQueryAsDom(query);
		List<Node> nodes = domRequired.selectNodes("//row/@fieldid");
		String[] requiredFields = XMLHelper.getStringValues(nodes);
			
		Document dom = FormUtil.getFormXml(groupId, request, endPoint, fields, requiredFields, defaultValues);
		
		// Se modifica las CSS en función de la intención para variar su comportamiento
		updateCSSAttrWithEditProfileIntent(editProfileIntent, dom);
		
		List<Node> listFieldIds = dom.selectNodes("/form/fieldsgroup/field/inputctrl/@id");
		
		// Se obtiene la lista de fieldid
		String formFieldIDs = StringUtil.apostrophe(StringUtil.merge(XMLHelper.getStringValues(listFieldIds),"','" ));
		ErrorRaiser.throwIfFalse(listFieldIds.size() > 0 && formFieldIDs.length() > 0, IterErrorKeys.XYZ_ITR_E_FORM_WITHOUT_INPUTCTRLS_ZYX);

		// Para cada FieldId, se obtiene su valor por defecto
		Document domValues 			= UserUtil.getUserProfileFields(groupId, userId, formFieldIDs);
		List<Node> listFieldValues 	= domValues.selectNodes("//row");
		
		List<String> repeteableInputCtrls = new ArrayList<String>();
		
		for (int i = 0; i < listFieldValues.size(); i++)
		{
			boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(listFieldValues.get(i), "@structured") );
			 
			String fieldName	= XMLHelper.getTextValueOf(listFieldValues.get(i), "@fieldname");
			ErrorRaiser.throwIfNull(fieldName);

			String fieldId 		= XMLHelper.getTextValueOf(listFieldValues.get(i), "@fieldid");
			ErrorRaiser.throwIfNull(fieldId);
			
			String defaultValue = XMLHelper.getTextValueOf(listFieldValues.get(i), "defaultvalue");
			
			Element field = (Element)dom.selectSingleNode( String.format("/form/fieldsgroup/field[inputctrl[@id='%s']]", fieldId) );
			Element inputCtrl = (Element)field.selectSingleNode("inputctrl");
			ErrorRaiser.throwIfNull(inputCtrl);
			
			boolean isRepeteable = GetterUtil.getBoolean(XMLHelper.getTextValueOf(field, "@repeatable"));
			// Es un control repetible y ya se ha añadido un valor para él
			if (isRepeteable && repeteableInputCtrls.contains(fieldId))
			{
				inputCtrl = inputCtrl.createCopy();
				field.add(inputCtrl);
			}
			else
			{
				repeteableInputCtrls.add(fieldId);
			}
			
			String inputCtrlType = XMLHelper.getTextValueOf(inputCtrl, "@type");
			ErrorRaiser.throwIfNull(inputCtrlType);
			
	//		Element elemValue = null;
			if (inputCtrlType.equals(UserUtil.PRF_FIELD_OTP_BUTTON))
			{
				String defValNodeTxt = String.valueOf(PortalLocalServiceUtil.executeQueryAsList( String.format("SELECT defaultvalue from FormField where fieldId = '%s'", fieldId) ).get(0));

				if ( Validator.isNotNull(defValNodeTxt) )
				{
					Element defVal =  SAXReaderUtil.read( defValNodeTxt ).getRootElement();
					defaultValue = XMLHelper.getStringValueOf(defVal, "value/@name", " ");
				}

				((Element)inputCtrl.selectSingleNode("defaultvalue")).setText( GetterUtil.getString2(defaultValue, ""));
				
				String phoneId = XMLHelper.getTextValueOf(domValues, String.format("/rs/row[@fieldname='%s' and @structured='1']/@fieldid", UserUtil.PRF_FIELD_TELEPHONE));
				inputCtrl.addAttribute("id_phone", 	phoneId);
			}

			if (Validator.isNotNull(defaultValue))
			{
				if (inputCtrlType.equals(INPUTCTRL_TYPE_RADIO_BUTTON) 	|| inputCtrlType.equals(INPUTCTRL_TYPE_LIST_CTRL) ||
						 inputCtrlType.equals(INPUTCTRL_TYPE_DROPDOWN_LIST) || inputCtrlType.equals(INPUTCTRL_TYPE_CHECK_BOX))
				{
					// options: Agrupa los valores por defecto para los inputctrl de type radiobutton, listctrl, dropdownlist o checkbox
					// Se comprueba si admite multiselección
					// boolean multiple = GetterUtil.getBoolean( XMLHelper.getTextValueOf(inputCtrl, "options/@multiple") );
					List<Node> options = inputCtrl.selectNodes("options/option");
					for (int iOpt = 0; iOpt < options.size(); iOpt++)
					{
						// El valor de la opción coincide con el valor por defecto, se marca como seleccionable
						String optValue = XMLHelper.getTextValueOf(options.get(iOpt), "@value");
						if (Validator.isNull(optValue))
							optValue = XMLHelper.getTextValueOf(options.get(iOpt), "text()");
						
						if ( defaultValue.equals(optValue) )
						{
							options.get(iOpt).selectSingleNode("@selected").setText("true");
						}
					}
				}
				else if ( !(structured && fieldName.equals(UserUtil.PRF_FIELD_AVATARURL)) )
				{
					// Si es una fecha, se le da el formato indicado en el campo del formulario
					if ("calendar".equalsIgnoreCase(inputCtrlType))
					{
						String format = XMLHelper.getStringValueOf(field, "validator/@format");
						if (Validator.isNotNull(format))
						{
							try
							{
								DateFormat formDf = new SimpleDateFormat(format);
								defaultValue = formDf.format(DateUtil.getDBFormat().parse(defaultValue));
							}
							catch (ParseException e)
							{
								_log.debug(String.format("Default value of field %s is not a valida date", fieldId));
							}
						}
					}
					
					// defaultvalue: El valor de este elemento será el valor con el que aparecerán los campos 
					// cuyo inputctrl sea de type password, textarea, text, none, calendar o hidden
					((Element)inputCtrl.selectSingleNode("defaultvalue")).setText(defaultValue);
				}
			}
			
			if (structured)
			{
				// Si es el Nombre, la contraseña o el correo electrónico, necesitan confirmación
				if (fieldName.equals(UserUtil.PRF_FIELD_USRNAME)				|| 
					fieldName.equals(UserUtil.PRF_FIELD_USRPWD) 				|| 
					(isEMAIL && fieldName.equals(UserUtil.PRF_FIELD_USREMAIL))	||
					(isOTP   && fieldName.equals(UserUtil.PRF_FIELD_TELEPHONE)))
				{
					Node nodeConfirm = inputCtrl.selectSingleNode("../@needconfirm");
					nodeConfirm.setText("true");
					
					if (fieldName.equals(UserUtil.PRF_FIELD_USRPWD))
					{
						// La contraseña NO será obligatoria durante la edición
						setNonRequired(inputCtrl, fieldName, nonRequiredFilesList);
					}
				}
				else if (fieldName.equals(UserUtil.PRF_FIELD_ABOID))
				{
					// El código de abonado NO será obligatorio durante la edición
					setNonRequired(inputCtrl, fieldName, nonRequiredFilesList);
				}
				else if (fieldName.equals(UserUtil.PRF_FIELD_EXTRAVALIDATOR))
				{
					// El validador de abonado NO será obligatorio durante la edición si NO lo es el código de abonado
					setNonRequired(inputCtrl, fieldName, nonRequiredFilesList);
				}
				else if (fieldName.equals(UserUtil.PRF_FIELD_AVATARURL) && Validator.isNotNull(defaultValue))
				{
					Node nodeLabel = inputCtrl.selectSingleNode("../labelbefore/textlabel");
					if (nodeLabel == null)
						nodeLabel = inputCtrl.selectSingleNode("../labelafter/textlabel");
					
					if (nodeLabel != null)
					{
						Node nodeLinkURL = nodeLabel.selectSingleNode("@linkurl");
						nodeLinkURL.setText(defaultValue);
						
						Node nodeLinkTarget = nodeLabel.selectSingleNode("@linktarget");
						nodeLinkTarget.setText("_blank");
					}
					
					setNonRequired(inputCtrl, fieldName, nonRequiredFilesList);
				}
			}
		}
		
		// Se eliminan todos los campos binarios (excepto el AVATAR) y el código de subscriptor
		String avatarFieldId = XMLHelper.getTextValueOf(domValues, String.format("/rs/row[@fieldname = '%s']/@fieldid",  UserUtil.PRF_FIELD_AVATARURL), "0");
		List<Node> listNodesToDelete = dom.selectNodes(String.format("/form/fieldsgroup/field[@fieldtype = 'binary' and inputctrl/@id!='%s']", avatarFieldId));
		for (int iDel = 0; iDel < listNodesToDelete.size(); iDel++)
			listNodesToDelete.get(iDel).detach();
		
		// Si es una edición del perfil (podría venir de un registro desde redes sociales con campos obligatorios no rellenos), no se muestran las newsletters
		if (!defaultValues)
		{
			List<Node> newsletterNodes = dom.selectNodes("/form/fieldsgroup/field[@fieldtype='newsletter']");
			for (int iDel = 0; iDel < newsletterNodes.size(); iDel++)
				newsletterNodes.get(iDel).detach();
		}
		
//		// Se elimina el código de subscriptor
//		String aboId = XMLHelper.getTextValueOf(domValues, String.format("/rs/row[@fieldname = '%s']/@fieldid",  UserUtil.XYZ_FIELD_ABOID_ZYX), UserUtil.XYZ_FIELD_ABOID_ZYX);
//		listNodesToDelete = dom.selectNodes(String.format("/form/fieldsgroup/field[inputctrl/@id='%s']", aboId));
//		for (int iDel = 0; iDel < listNodesToDelete.size(); iDel++)
//			listNodesToDelete.get(iDel).detach();
		
		// Se eliminan los tabs que NO tengan campos
		listNodesToDelete = dom.selectNodes("/form/fieldsgroup[count(./field) = 0]");
		for (int iDel = 0; iDel < listNodesToDelete.size(); iDel++)
			listNodesToDelete.get(iDel).detach();
		
		// La validación del CAPTCHA no es requerida
		Node nodeCaptcha = dom.selectSingleNode("/form/@usecaptcha");
		nodeCaptcha.setText("false");
		
		if (_log.isDebugEnabled())
		{
	        String pathRoot = new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath();
	        String ruta = new StringBuilder(pathRoot).append(File.separatorChar)
										  .append("user-portlet").append(File.separatorChar)
										  .append("xsl").append(File.separatorChar)
										  .append("registryform2Transform.xml").toString();
	        
	        FileWriter fichero = new FileWriter(ruta);
	        fichero.write(dom.asXML());
	        fichero.close();
		}
		
		return dom;
	}
	
	/**
	 * Si el campo es el esperado y está en la lista de elementos NO obligatorios, se marca como tal en el XML
	 * @param node
	 * @param fieldName
	 * @param expectedFieldName
	 * @param nonRequiredFilesList
	 * @return
	 */
	static private boolean setNonRequired(Node node, String fieldName, List<String> nonRequiredFilesList)
	{
		boolean set = nonRequiredFilesList.contains( StringUtil.apostrophe(fieldName) );
		
		if (set)
		{
			// El validador de abonado NO será obligatorio durante la edición si NO lo es el código de abonado
			Node nodeRequired = node.selectSingleNode("../@required");
			nodeRequired.setText("false");
		}
		return set;
	}
	
	/**
	 * 
	 * @param query
	 * @param rq
	 * @param endPoint
	 * @param fields
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 * @throws DocumentException
	 * @throws IOException
	 */
	static private Document buildFormXml( String query, HttpServletRequest rq, String endPoint, String[] fields, String[] requiredFields, boolean defaultValues )throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, IOException
	{
		Element rootElement = SAXReaderUtil.createElement(ELEM_FORM);
		Document form 		= SAXReaderUtil.createDocument(rootElement);

		Document d 			= PortalLocalServiceUtil.executeQueryAsDom( query, new String[]{"navigationtype", "ok", "ko"} );
		
		Node formNode = d.selectSingleNode("/rs/row");
		ErrorRaiser.throwIfNull(formNode, IterErrorKeys.XYZ_FORM_NOT_EXISTS_ZYX, query);
		
		String grpid = XMLHelper.getTextValueOf(formNode, "@groupid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(grpid) && Validator.isNumber(grpid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = Long.valueOf(grpid);
		
		String idForm = XMLHelper.getTextValueOf(formNode, "@formid");
		ErrorRaiser.throwIfNull(idForm, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		rootElement.addAttribute(ATTR_ID, idForm);
		
		String css = XMLHelper.getTextValueOf(formNode, "@css", "");
		rootElement.addAttribute(ATTR_CSS, css);
		
		String captcha = XMLHelper.getTextValueOf(formNode, "@usechaptcha");
		rootElement.addAttribute(ATTR_USE_CAPTCHA, captcha);
		
		if( captcha.equalsIgnoreCase("true") )
		{
			String captchaInfo 	= CaptchaLocalServiceUtil.getCaptcha( groupId );
			Element captchaElem = SAXReaderUtil.read(captchaInfo).getRootElement().element("row");
			rootElement.addAttribute(ATTR_CAPTCHA_KEY, captchaElem.attributeValue("publickey"));
			rootElement.addAttribute(ATTR_CAPTCHA_LANG, captchaElem.attributeValue("languagecode"));
			String[] themeAndSize = captchaElem.attributeValue("theme").split(StringPool.UNDERLINE);
			rootElement.addAttribute(ATTR_CAPTCHA_THEME, themeAndSize[0]);
			rootElement.addAttribute("captchasize", themeAndSize[1]);
		}
		
		Node navTypeNode 	= d.selectSingleNode("/rs/row/navigationtype");
		Element navigation 	= SAXReaderUtil.read( CDATAUtil.strip( navTypeNode.getText() ) ).getRootElement();
		String typeNav 		= navigation.attributeValue("type");
		
		// En la configuracion de formularios el tipo "page" equivale a una única página, y el tipo "bypage" a navegación por páginas
		if( (typeNav.equals("page"))|| typeNav.equals("bypagegroup") )
			typeNav="";
		else if( typeNav.equals("bypage") )
			typeNav = "page";
		rootElement.addAttribute(ATTR_FIELDSGRP_NAV, typeNav);
		
		String usesubmit = XMLHelper.getTextValueOf(formNode, "@usesubmit");
		rootElement.addAttribute(ATTR_USE_SUBMIT, usesubmit);
		
		String submitlabel = XMLHelper.getTextValueOf(formNode, "@submitlabel");
		ErrorRaiser.throwIfNull(submitlabel, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		rootElement.addAttribute(ATTR_SUBMIT_LBL, submitlabel);
		
		String resetlabel = XMLHelper.getTextValueOf(formNode, "@resetlabel", "");
		rootElement.addAttribute(ATTR_RESET_LBL, resetlabel);
		
		String nextLbl = typeNav.equalsIgnoreCase("page") ? navigation.attributeValue("nextlabel", "") : "";
		rootElement.addAttribute(ATTR_NEXT_LBL, nextLbl);
		
		String prevLbl = typeNav.equalsIgnoreCase("page") ? navigation.attributeValue("prevlabel", "") : "";
		rootElement.addAttribute(ATTR_PREV_LBL, prevLbl);
		
		if (PHPUtil.isApacheRequest(rq))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
			
			rootElement.addAttribute(ATTR_REFERER, GET_REFERER_FUNC);
		}
		else
			rootElement.addAttribute(ATTR_REFERER, GetterUtil.getString(rq.getHeader("referer"), StringPool.BLANK));
		
		String okLabel 		= XMLHelper.getTextValueOf(formNode, "@oklabel", 			"");
		rootElement.addAttribute(ATTR_OK, okLabel);
		
		String cancelLabel 	= XMLHelper.getTextValueOf(formNode, "@cncllabel", 			"");
		rootElement.addAttribute(ATTR_CANCEL, cancelLabel);
		
		String ConfirmTitle = XMLHelper.getTextValueOf(formNode, "@confirmpaneltitle", 	"");
		rootElement.addAttribute(ATTR_CONFIRM_PANEL_TITLE, ConfirmTitle);
		
		// Si no hay definido servlet, se enviará por JS
		Element servlet = SAXReaderUtil.createElement(ELEM_SERVLET);
		if( Validator.isNotNull(endPoint) )
		{
			String separator = (endPoint.indexOf("?") >= 0) ? "&" : "?";
			StringBuilder sbEndPoint = new StringBuilder(endPoint).append(separator).append("formid=").append(idForm);
			
			if ( requiredFields!=null )
				sbEndPoint.append("&").append(IterKeys.REQ_FIELDS).append("=").append( StringUtils.join(requiredFields, StringPool.COMMA) );
			
			if ( fields!=null )
				sbEndPoint.append("&").append(IterKeys.ALL_FIELDS).append("=").append( StringUtils.join(fields, StringPool.COMMA) );
			
			servlet.setText( sbEndPoint.toString() );
		}
		rootElement.add(servlet);
		
		Element invalidFieldMsg = SAXReaderUtil.createElement(ELEM_INVALID_FIELD_MSG);
		Element errorFieldElem 	= SAXReaderUtil.read( d.selectSingleNode("/rs/row/ko").getText() ).getRootElement();
		invalidFieldMsg.addText( errorFieldElem.element("invalidfieldmsg").getText() );
		rootElement.add(invalidFieldMsg);
		
		Element restrictions = SAXReaderUtil.createElement(ELEM_RESTRICTIONS);
		restrictions.addAttribute( ATTR_RESTRICTED, XMLHelper.getTextValueOf(formNode, "@restricted") );
		restrictions.addAttribute( ATTR_PRODUCTS, GetterUtil.getString(XMLHelper.getTextValueOf(formNode, "@products"), "") );
		rootElement.add( restrictions );
		
		query = String.format(GET_TABS, idForm);
		Document tabs = PortalLocalServiceUtil.executeQueryAsDom(query);
		
		List<Node> nodes = tabs.selectNodes("//row");
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		for(Node n : nodes)
		{
			Element fieldsgrp = SAXReaderUtil.createElement(ELEM_FIELDS_GRP);
			String tabName = XMLHelper.getTextValueOf(n, "@name");
			ErrorRaiser.throwIfNull(tabName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			fieldsgrp.addAttribute(ATTR_NAME, tabName);
			
			String tabid = XMLHelper.getTextValueOf(n, "@tabid");
			ErrorRaiser.throwIfNull(tabid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			getTabFields( groupId, fieldsgrp, idForm, tabid, fields, defaultValues );
			
			if(fieldsgrp.elements().size()>0)
				rootElement.add(fieldsgrp);
		}
		
		
		
		if (_log.isDebugEnabled())
		{
	        String pathRoot = new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath();
	        String ruta = new StringBuilder(pathRoot).append(File.separatorChar)
										  .append("user-portlet").append(File.separatorChar)
										  .append("xsl").append(File.separatorChar)
										  .append("form2Transform.xml").toString();
	        
	        FileWriter fichero = new FileWriter(ruta);
	        fichero.write(form.asXML());
	        fichero.close();
		}
		
		return form;
	}
	
	/**
	 * 
	 * @param fieldsGrp
	 * @param tabid
	 * @param fields
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 * @throws DocumentException
	 */
	static private void getTabFields( long groupId, Element fieldsGrp, String formId, String tabid, String[] fields, boolean defaultValues ) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException
	{
		String query = String.format(GET_FIELDS, tabid);
		_log.debug(query);
		Document fieldsDom = PortalLocalServiceUtil.executeQueryAsDom(query, new String[]{"labelbefore", "labelafter", "tooltip", "defaultvalue", "inputctrl", "validator"});
		
		if (_log.isTraceEnabled())
			_log.trace(fieldsDom.asXML());
		
		List<Node> nodes = fieldsDom.selectNodes("//row");
		
		for(Node n : nodes)
		{
			String idField = XMLHelper.getTextValueOf(n, "@fieldid");
			
			if( Validator.isNotNull(fields) && fields.length>0 && !ArrayUtil.contains(fields, idField) )
				continue;
			
			Element field = SAXReaderUtil.createElement(ELEM_FIELD);
			
			String fType 		= XMLHelper.getTextValueOf(n, "@fieldtype");
			field.addAttribute(ATTR_FIELD_TYPE, fType);
			
			String editable 	= XMLHelper.getTextValueOf(n, "@editable");
			field.addAttribute(ATTR_EDITABLE, editable);
			
			String required 	= XMLHelper.getTextValueOf(n, "@required");
			field.addAttribute(ATTR_REQUIRED, required);
			
			String needconfirm 	= XMLHelper.getTextValueOf(n, "@needconfirm");
			field.addAttribute(ATTR_NEED_CONFIRM, needconfirm);
			
			String cssClass 	= XMLHelper.getTextValueOf(n, "@css", "");
			field.addAttribute(ATTR_CSS, cssClass);
			
			// Label before
			Element lblBefore 	= SAXReaderUtil.createElement(ELEM_LBL_BEFORE);
			Element labelBef 	= SAXReaderUtil.read( n.selectSingleNode("labelbefore").getText() ).getRootElement();
			Element txtBef 		= labelBef.element("textlabel");
			
			if( !txtBef.getStringValue().isEmpty() )
			{
				Element txtLblBefore = SAXReaderUtil.createElement(ELEM_TEXT_LBL);
				txtLblBefore.addAttribute(ATTR_LINK_URL, 	txtBef.attributeValue("linkurl", 	""));
				txtLblBefore.addAttribute(ATTR_LINK_TARGET, txtBef.attributeValue("linktarget", ""));
				txtLblBefore.addText( txtBef.getStringValue() );
				lblBefore.add(txtLblBefore);
			}
			field.add(lblBefore);
			
			// Label after
			Element lblAfter 	= SAXReaderUtil.createElement(ELEM_LBL_AFTER);
			Element labelAftr 	= SAXReaderUtil.read( n.selectSingleNode("labelafter").getText() ).getRootElement();
			Element txtAftr 	= labelAftr.element("textlabel");
			
			if( !txtAftr.getStringValue().isEmpty() )
			{
				Element txtLblAfter = SAXReaderUtil.createElement(ELEM_TEXT_LBL);
				txtLblAfter.addAttribute(ATTR_LINK_URL, 	txtAftr.attributeValue("linkurl", 	""));
				txtLblAfter.addAttribute(ATTR_LINK_TARGET, 	txtAftr.attributeValue("linktarget",""));
				txtLblAfter.addText( txtAftr.getStringValue() );
				lblAfter.add(txtLblAfter);
			}
			field.add(lblAfter);
			
			String tooltipValue = n.selectSingleNode("tooltip").getStringValue();
			
			Element tooltip = SAXReaderUtil.createElement(ELEM_TOOLTIP);
			tooltip.addText(tooltipValue);
			
			field.add(tooltip);
			
			Node inputctrlNode = n.selectSingleNode("inputctrl");
			Element iCtrl =  SAXReaderUtil.read( CDATAUtil.strip( inputctrlNode.getText() ) ).getRootElement();
			
			Element inputCtrl = SAXReaderUtil.createElement(ELEM_INPUT_CTRL);
			
			String inputCtrlName = XMLHelper.getTextValueOf(n, "@htmlname");
			if ( Validator.isNull(inputCtrlName) )
				inputCtrlName = XMLHelper.getTextValueOf(n, "@fieldid");
			ErrorRaiser.throwIfNull(inputCtrlName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String fieldid = XMLHelper.getTextValueOf(n, "@fieldid");
			ErrorRaiser.throwIfNull(fieldid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String iControlType = iCtrl.attributeValue("type");
			
			boolean isInputCtrlOptions = (iControlType.equals(INPUTCTRL_TYPE_RADIO_BUTTON) 	|| iControlType.equals(INPUTCTRL_TYPE_LIST_CTRL) ||
					  					  iControlType.equals(INPUTCTRL_TYPE_DROPDOWN_LIST) || iControlType.equals(INPUTCTRL_TYPE_CHECK_BOX));

			boolean repeatable 	= GetterUtil.getBoolean(XMLHelper.getTextValueOf(n, "@repeatable"));
			field.addAttribute(ATTR_REPEATABLE, String.valueOf(repeatable && !isInputCtrlOptions));

			
			inputCtrl.addAttribute(ATTR_NAME, 	inputCtrlName);
			inputCtrl.addAttribute(ATTR_ID, 	fieldid);
			inputCtrl.addAttribute(ATTR_TYPE, 	iControlType);
			
			String iControlLang = iCtrl.attributeValue("lang");
			if( Validator.isNotNull(iControlLang) )
			{
				Element language = SAXReaderUtil.createElement(ELEM_LANGUAGE);
				language.addText( iControlLang );
				inputCtrl.add( language );
			}
			
			/** mindate*/
			String controlminData = iCtrl.elementText("mindate");
			if( Validator.isNotNull(controlminData) && (!controlminData.equalsIgnoreCase("withoutLimit")))
			{
				Element minData = SAXReaderUtil.createElement(ELEM_MINDATE);
				minData.addText( controlminData );
				inputCtrl.add( minData );
			}
			
			/** maxdate*/
			String iControlmaxData = iCtrl.elementText("maxdate");
			if( Validator.isNotNull(iControlmaxData) && (!iControlmaxData.equalsIgnoreCase("withoutLimit")))
			{
				Element maxData = SAXReaderUtil.createElement(ELEM_MAXDATE);
				maxData.addText( iControlmaxData );
				inputCtrl.add( maxData );
			}
			
			Element defaultvalue = SAXReaderUtil.createElement(ELEM_DEFAULT_VALUE);
			Element defVal = null;
			if (_log.isTraceEnabled())
				_log.trace( String.format("ControlType: %s", iControlType) );
			
			if (iControlType.equals("newsletter"))
			{
				try
				{
					String newsletterConfig = GroupConfigTools.getGroupConfigField(groupId, "newsletterconfig");
					String newsletterCode = new StringBuilder()
						.append(new NewsletterPortletMgr(newsletterConfig, false).getNewsletterPortletCode(IterRequest.getOriginalRequest(), null, groupId))
						.append(NewsletterPortletMgr.NEWSLETTER_TOGGLE_CODE)
						.toString();
					defaultvalue.addCDATA( newsletterCode );
				}
				catch (Exception e)
				{
					// Si es el preview, se muestra un mensaje de error en lugar de las newsletter 
					if (PropsValues.IS_PREVIEW_ENVIRONMENT)
					{
						defaultvalue.addCDATA( "The Newsletters portlet has not been configured" );
					}
				}
			}
			else
			{
				Node defaultvalueNode = n.selectSingleNode("defaultvalue");
				String defValNodeTxt = CDATAUtil.strip( defaultvalueNode.getText() );

				if ( Validator.isNotNull(defValNodeTxt) )
					defVal =  SAXReaderUtil.read( defValNodeTxt ).getRootElement();
				
				if ( Validator.isNotNull(defVal) && 
					(iControlType.equalsIgnoreCase("password") 	|| iControlType.equalsIgnoreCase("textarea") || iControlType.equalsIgnoreCase("text") 	||
					 iControlType.equalsIgnoreCase("none") 		|| iControlType.equalsIgnoreCase("calendar") || iControlType.equalsIgnoreCase("hidden")	||
					 iControlType.equals(UserUtil.PRF_FIELD_OTP_BUTTON)) )
				{
					if(defaultValues)
					{
						Node valueNode = defVal.selectSingleNode("value/@name");
						defaultvalue.addText( valueNode.getText() );
					}
				}
				
				if (iControlType.equals(UserUtil.PRF_FIELD_OTP_BUTTON))
				{
					String phoneId = "";
					List<Object> phoneIdList = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_PHONE_FIELD_ID, formId) );
					if ( phoneIdList.size() > 0 && Validator.isNotNull(phoneIdList.get(0)) )
						phoneId = String.valueOf(phoneIdList.get(0));
					
					if (_log.isTraceEnabled())
						_log.trace( String.format("Finding %s id: %s", UserUtil.PRF_FIELD_TELEPHONE, phoneId) );
						
					inputCtrl.addAttribute("id_phone", 	phoneId);
				}
			}
			
			inputCtrl.add(defaultvalue);
			
			if (isInputCtrlOptions)
			{
				Element options = SAXReaderUtil.createElement(ELEM_OPTIONS);
				
				Element opts = iCtrl.element("options");
				if ( Validator.isNotNull(opts) )
				{
					List<Element> optList = opts.elements();
					ArrayList<String> listOptions = null;
					
					if (iControlType.equalsIgnoreCase("listctrl") || iControlType.equalsIgnoreCase("dropdownlist"))
						options.addAttribute(ATTR_SIZE, opts.attributeValue("size"));
					
					options.addAttribute(ATTR_MULTIPLE, opts.attributeValue("multiple", "false"));
					
					
					List<Node> defValNodes = defVal.selectNodes("value/@name");
					listOptions = new ArrayList<String>();
					for (Node valueNode : defValNodes)
					{
						listOptions.add( valueNode.getStringValue() );
					}
					
					for ( Element opt : optList )
					{
						Element option = SAXReaderUtil.createElement(ELEM_OPTION);
						option.addAttribute(ATTR_SELECTED, listOptions.contains( opt.attributeValue("name") ) && defaultValues ?"true":"false" );
						option.addAttribute(ATTR_VALUE, opt.attributeValue("value"));
						option.addText(opt.attributeValue("name"));
						options.add(option);
					}
					
					inputCtrl.add(options);
				}
			}
			
			field.add(inputCtrl);
			
			Element validatorElem 	= SAXReaderUtil.read( n.selectSingleNode("validator").getText() ).getRootElement();
			String validatorType	= validatorElem.attributeValue("type");
			
			if ( Validator.isNotNull(validatorType) )
			{
				Element validator = SAXReaderUtil.createElement(ELEM_VALIDATOR);
				
				validator.addAttribute(ATTR_TYPE, validatorType);
				
				String format = "";
				if( validatorType.equalsIgnoreCase("regexp") )
					format = "/" + validatorElem.getStringValue() + "/";
				else if ( validatorType.equalsIgnoreCase("numberrange") )
						format = createRegExpr(validatorElem);
					else
						format = validatorElem.attributeValue("format");
				
				if( Validator.isNotNull(format) )
					validator.addAttribute(ATTR_FORMAT, format);
				String min = validatorElem.attributeValue("min");
				if( Validator.isNotNull(min) )
					validator.addAttribute(ATTR_MIN, min);
				String max = validatorElem.attributeValue("max");
				if( Validator.isNotNull(max) )
					validator.addAttribute(ATTR_MAX, max);
				
				field.add(validator);
			}
			
			fieldsGrp.add(field);
		}
	}
	
	static private String createRegExpr( Element e  )
	{
		StringBuilder regExpr = new StringBuilder("/");
		
		String thousandSep = e.attributeValue("thousandseparator");
		String decimalSep = e.attributeValue("decimalseparator");
		String maxDecimals = e.attributeValue("maxdecimals");
		
		if( Validator.isNotNull(thousandSep) )
			regExpr.append("^\\d{1,3}(").append(thousandSep).append("\\d{3})*");
		else
			regExpr.append("^\\d+");
		
		if( Validator.isNotNull(decimalSep) && Validator.isNotNull(maxDecimals) && !maxDecimals.isEmpty() && !maxDecimals.equals("0") )
			regExpr.append("(\\").append(decimalSep).append("\\d{0,").append(maxDecimals).append("})?");
		
		regExpr.append("$/");
		
		return regExpr.toString();
	}

	static public String applyXSL( Document form )
	{
		String result = "";
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("user-portlet")
									.append(File.separatorChar).append("xsl")
									.append(File.separatorChar).append("formulario_ITER.xsl").toString();

		result = XSLUtil.transformXML(form.asXML(), xslpath );
		
		return result;
	}
	
	static public List<Object> transformToObjectsList(String unformattedList)
	{
		List<Object> items = new ArrayList<Object>();
		
		StringTokenizer st = new StringTokenizer(unformattedList,",");
		
		while (st.hasMoreTokens())
		{
			String item = st.nextToken();
			items.add(item.substring(1, item.length()-1));
		}
		
		return items;
	}
	/**
	 * 
	 * @param field
	 * @param attach
	 * @return mapa con el nombre del adjunto y el binario
	 * @throws ServiceError 
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Object> getAttachment(Node field, Map<String, ArrayList> attach) throws ServiceError
	{
		Map<String, Object> retVal = new HashMap<String, Object>();
		
		// Obtenemos el "puntero" del valor del binario
		final String binaryDataPointer = XMLHelper.getTextValueOf(field, "data/binary/binlocator");
		if (Validator.isNull(binaryDataPointer)){
			_log.debug("binaryDataPointer is null");
		}
		ErrorRaiser.throwIfNull(binaryDataPointer);
		
		// Obtenemos el adjunto
		ArrayList aux = attach.get(binaryDataPointer);
		if (Validator.isNull(aux)){
			_log.debug("aux is null");
		}
		ErrorRaiser.throwIfNull(aux);
		// Nombre del adjunto
		final String name = (String)aux.get(0);
		
		retVal.put(KEY_NAME, name);
		
		// Valor del adjunto
		byte[] bytes = (byte[])aux.get(1);
		if (Validator.isNull(bytes)){
			_log.debug("Attatchment value is null");
		}
		ErrorRaiser.throwIfNull(bytes);
		
		InputStream is = new UnsyncByteArrayInputStream(bytes);
		
		retVal.put(KEY_ATTACH, is);
		
		return retVal;
	}
		
}
