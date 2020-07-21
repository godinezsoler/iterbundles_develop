package com.protecmedia.iter.user.service.impl;

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.IterRS;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterUserBackupMgr;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.protecmedia.iter.base.metrics.NewslettersMetricsUtil;
import com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.user.service.base.IterUserMngLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = { Exception.class })
public class IterUserMngLocalServiceImpl extends IterUserMngLocalServiceBaseImpl
{
	private static Log			_log				= LogFactoryUtil.getLog(IterUserMngLocalServiceImpl.class);
	// Tipo de formulario en el que se basa el listado de usuarios
	private static final String	FORM_REGISTER_TYPE	= "registro";
	// Nombre de la xsl a utilizar
	private static final String	XSL_DETAIL_USER		= "iterUserDetail.xsl";
	// Campo por el que se ordena la primera vez
	private static final String	DEFAULT_FIELD_ORDER	= UserUtil.PRF_FIELD_USRNAME;
	private static final String	BINARY_TYPE			= "binary";
	
	private static final String PROFILE = "prf_";
	
	// Consulta para obtener los nombres de las columnas
	private static final String	GET_HEADERS_COLUMNS	= new StringBuilder()
	.append("SELECT ft.tabid, ft.name tabname, ff.fieldid, df.fieldtype 'type', f.formid, up.fieldname identificator, \n") 
	.append("up.fieldname 'columnname', extractvalue(ff.inputctrl, '/inputctrl/@type') inputctrltype, structured      \n")
	.append("FROM userprofile up                                                                                      \n")
	.append("INNER JOIN datafield df ON df.datafieldid = up.datafieldid                                               \n")
	.append("INNER JOIN formfield ff ON ff.profilefieldid = up.profilefieldid                                         \n")
	.append("INNER JOIN formtab ft   ON ft.tabid = ff.tabid                                                           \n")
	.append("INNER JOIN form f       ON f.formid = ft.formid                                                          \n")
	.append("WHERE f.formtype = '%s'                                                                                  \n")
	.append("  AND f.groupid  = %s                                                                                    \n")
	// Campos que no queremos que se muestren 
	.append("  AND up.fieldname not in('").append( StringUtil.merge(UserUtil.PRF_HIDE_FIELDS, "','") ).append("', ").append(StringUtil.apostrophe(UserUtil.PRF_FIELD_USRPWD)).append(") \n")
	// Importante ordenar por el orden de la pestania y del campo para que salga en el mismo orden que definio el formulario
	.append("ORDER BY ft.taborder, ff.fieldorder \n").toString();

	
	// Sql para ids de usuarios		
		// Funcion que realizara la consulta: ITR_FETCH_FUNCT_USERS_REPORT(groupid BIGINT(20), predicates LONGTEXT, sortField VARCHAR(42), sortOrder VARCHAR(5), newsletters LONGTEXT)
		private static final String GENERATE_QUERY = " SELECT ITR_FETCH_FUNCT_USERS_REPORT(%s, \"%s\", \"%s\", \"%s\", \"%s\") ";
	
		// Filtro por columna variable
		private static final String OPTIONAL_FILTER = new StringBuilder()
	    .append("\n 0 %s (SELECT COUNT(*) 																							                    \n")
	    .append("	 	    FROM userprofile userprofile1																						        \n")
    	.append(" 		      INNER JOIN formfield formfield1                 ON formfield1.profilefieldid    = userprofile1.profilefieldid             \n")
		.append("             INNER JOIN formtab formtab1                     ON formtab1.tabid               = formfield1.tabid                        \n")
		.append("             INNER JOIN form form1                           ON form1.formid                 = formtab1.formid                         \n")
		.append("             INNER JOIN userprofilevalues userprofilevalues1 ON userprofile1.profilefieldid  = userprofilevalues1.profilefieldid       \n")
			// form.groupid
		.append("           WHERE form1.groupid = %s 																						            \n")
		.append("             AND userprofilevalues1.usrid = iterusers.usrid 																	        \n")
			// formfield.fieldid
		.append("             AND formfield1.fieldid = '%s' 																				            \n")
			// condicion filtro
		.append("             AND ( %s)																			        \n")
	    .append("          ) \n").toString();
	            
	    // Filtro por campo binario
		private static final String OPTIONAL_BINARY_FILTER = new StringBuilder()
		.append("\n 0 %s (SELECT COUNT(*) 																											    \n")
        .append("           FROM userprofile userprofile1 																							    \n")
        .append("             INNER JOIN formfield formfield1                 ON formfield1.profilefieldid          = userprofile1.profilefieldid       \n")
		.append("             INNER JOIN formtab formtab1                     ON formtab1.tabid                     = formfield1.tabid                  \n")
		.append("             INNER JOIN form form1                           ON form1.formid                       = formtab1.formid                   \n")
		.append("             INNER JOIN userprofilevalues userprofilevalues1 ON userprofile1.profilefieldid        = userprofilevalues1.profilefieldid \n")
		.append("             INNER JOIN dlfileentry dlfileentry1             ON userprofilevalues1.binfieldvalueid = dlfileentry1.fileEntryId 		    \n")
			// form.groupid
	    .append("           WHERE form1.groupid = %s 																								    \n")
    	.append("             AND userprofilevalues1.usrid = iterusers.usrid 																		    \n") 
    		// formfield.fieldid
		.append("             AND formfield1.fieldid = '%s' 																							\n")
			// condicion filtro
		.append("             AND  %s 																						    \n")
		.append("          ) \n" ).toString();
	// Sql para ids de usuarios
	

	// Obtiene los datos fijos y opcionales de los usuarios
	private static final String	USERS_DATA = new StringBuilder()
										     .append(" SELECT ITR_FETCH_DISCRETE_FUNCT_USERS_REPORT(%s, \"%s\", \"%s\", \"%s\")").toString();
	

	// Consulta para borrar usuarios
	private static final String	DELETE_USERS = new StringBuilder()
		.append("DELETE FROM iterusers \n")
		.append("WHERE usrid in(%s)    \n").toString();

	// Obtiene la definicion del formulario: fieldid, fieldname
	private static final String	IDS_FROM_REGISTER_FORM	= new StringBuilder()
		.append("SELECT ff.fieldid 											 \n")
		.append("FROM form f, formtab ft, formfield ff 						 \n")
		.append("WHERE f.formid = ft.formid 								 \n")
		.append("  AND ft.tabid = ff.tabid 									 \n")
		.append("  AND f.formtype = '").append(FORM_REGISTER_TYPE).append("' \n")
		.append("  AND f.groupid = '%s' 									 \n")
		.append("ORDER by ft.taborder, ff.fieldorder").toString();

	private final String GET_USER_INFO = new StringBuilder(" SELECT usrid, usrname, pwd, aboid, email, firstname, lastname, secondlastname, \n")
										 .append(" userexpires, registerdate, lastlogindate, updateprofiledate ")
										 .append(" FROM iterusers WHERE usrid='%s' ").toString();

	private final String UPDT_USER_INFO	= "UPDATE iterusers SET %s WHERE usrid='%s'";
	
	private final String GET_UPDTED_USER_FIELDS	= "SELECT usrid, %s FROM iterusers WHERE usrid='%s'";
	
	private final String GET_PASSWORD_SUPERUSER	= "SELECT screenName, password_ FROM user_ WHERE emailAddress='%s'";
	private final String SET_SCREENNAME_SUPERUSER	= "UPDATE user_ SET screenName='%s' WHERE emailAddress='%s'";
	
	private final String GET_SUPERUSER      = "SELECT userId FROM user_ WHERE emailAddress = '%s'";

	// Importacion de usuarios	
		private final SimpleDateFormat SDF_TO_DB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
		
		private final SimpleDateFormat sDF = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);	
		
		private final String GET_USER_AVATAR = new StringBuilder()
			.append("SELECT avatarurl \n")
			.append("FROM iterusers iu \n")
			.append("WHERE iu.usrname IN ('%s','%s')").toString();
		
		private final String GET_FILEENTRIES_ID_FROM_USER = new StringBuilder()
			.append("SELECT upv.binfieldvalueid binfieldvalueid \n")
			.append("FROM iterusers iu, userprofilevalues upv   \n")
			.append("WHERE iu.usrid = upv.usrid                 \n")
            .append("  AND iu.usrname IN ('%s','%s')            \n")
			.append("  AND upv.binfieldvalueid is not null").toString();
					
		// Campos posibles en el nodo /us/u/f del xml de la importación
		private final List<String> FIELDS_IN_F_NODE = 
				Arrays.asList("aboid", "disqusid", "facebookid", "googleplusid", "twitterid", "registerdate", 
                              "lastlogindate", "updateprofiledate", "firstname", "lastname", "secondlastname", 
                              "avatarurl", "usrname", "pwd", "email", "age", "birthday", "gender", "maritalstatus",
                              "language", "coordinates", "country", "region", "city", "address", "postalcode", "telephone");
		
		// Campos de la tabla iterusers que no estan en el formulario de registro pero que aún así se pueden importar al xml
		private final String[] FIELDS_NOT_IN_REGISTRATION_FORM = new String[]{"aboid",
																			  "disqusid",	
				        													  "facebookid", 			
				        													  "googleplusid", 		
				        													  "twitterid", 			
				        													  "registerdate", 		
				        													  "lastlogindate", 		
				        													  "updateprofiledate"};
		// Campos de FIELDS_NOT_IN_REGISTRATION_FORM que son de tipo fecha
		ArrayList<String> FIELDS_NOT_IN_REGISTRATION_FORM_DATE = new ArrayList<String>(asList("registerdate", "lastlogindate", "updateprofiledate"));		
		
		// Borrado de usuarios
		private final String DELETE_USER_BEFORE_IMPORT = new StringBuilder()
			.append("DELETE               \n")
			.append("FROM iterusers       \n") 
			.append("WHERE usrname = '%s' \n").toString();
	
		// Obtiene los aboid de los usuarios
		/*private final String GET_USER_ABOID = new StringBuilder("SELECT aboid   \n")
													   .append("FROM iterusers \n")
													   .append("WHERE aboid is not null").toString();*/
		
		// Primera parte de la insercion en la tabla de iterusers
		private final String INSERT_ITERUSERS = "INSERT INTO iterusers(usrid, %s, delegationid) VALUES";
		private final String GET_DELEGATION_ID = ", (SELECT IF (typesettings = '', NULL, typesettings) typesettings FROM group_ where groupId = ";
		
		// Primera parte de la insercion en la tabla userprofilevalues al importar usuarios
		private final String INSERT_USERPROFILEVALUES = new StringBuilder()
			.append("INSERT INTO userprofilevalues(profilevalueid, usrid, profilefieldid, fieldvalue, binfieldvalueid) VALUES \n")
			.append("%s").toString();
	
	private static final String GROUPCONCAT_PARAMS = String.format(new StringBuilder(
		"	ORDER BY 								\n").append(                                               
	    "    (CASE WHEN up.fieldname = '%s' THEN 0 	\n").append(   
	    "          WHEN up.fieldname = '%s' THEN 1 	\n").append(   
	    "          WHEN up.structured       THEN 2 	\n").append(   
	    "          ELSE 3 							\n").append(                                                      
	    "    END) ASC, 								\n").append(                                                         
	    "    ft.taborder, 							\n").append(                                                      
	    "    ff.fieldorder 							\n").append(                                                     
	    " SEPARATOR ',' 							\n").toString(), UserUtil.PRF_FIELD_USRNAME, UserUtil.PRF_FIELD_USREMAIL);
	
	private static final String GROUPCONCAT_PARAMS_SPECIAL_SEPARATOR = String.format(new StringBuilder(
		"	ORDER BY 								\n").append(                                               
	    "    (CASE WHEN up.fieldname = '%s' THEN 0 	\n").append(   
	    "          WHEN up.fieldname = '%s' THEN 1 	\n").append(   
	    "          WHEN up.structured       THEN 2 	\n").append(   
	    "          ELSE 3 							\n").append(                                                      
	    "    END) ASC, 								\n").append(                                                         
	    "    ft.taborder, 							\n").append(                                                      
	    "    ff.fieldorder 							\n").append(                                                     
	    " SEPARATOR '§' 							\n").toString(), UserUtil.PRF_FIELD_USRNAME, UserUtil.PRF_FIELD_USREMAIL);
	
	/**
	 * Consulta para recuperar la cabecera del fichero CSV utilizando el formulario de registro.	
	 */
	private static final String	CSV_EXPORT_GET_HEADER = String.format(new StringBuilder()
		.append("SELECT group_concat( up.profilefieldid                            	\n")
		.append("            %1$s                                               	\n")
		.append("       ) AS columnIds,                                         	\n")
		.append("       group_concat( CONCAT('\"', up.fieldname, '\"')      \n")
		.append("            %2$s                                               	\n")
		.append("       ) AS columnNames,                                 \n")
		.append("       group_concat( up.structured                            		\n")
		.append("            %1$s                                               	\n")
		.append("       ) AS structured                                         	\n")
		.append("FROM formfield ff                                              	\n")
		.append("INNER JOIN formtab ft     ON ff.tabid = ft.tabid               	\n")
		.append("INNER JOIN userprofile up ON ff.profilefieldid = up.profilefieldid \n")
		.append("WHERE formid IN (                                              	\n")
		.append("    SELECT formid                                              	\n")
		.append("    FROM form WHERE formtype = 'registro'                      	\n")
		.append("     AND groupid = %%s                                         	\n")
		.append(")                                                              	\n")
		.append("AND up.fieldname not in('%3$s', '%4$s')							\n").toString(), 
		GROUPCONCAT_PARAMS, GROUPCONCAT_PARAMS_SPECIAL_SEPARATOR, UserUtil.PRF_FIELD_USRPWD, UserUtil.PRF_FIELD_EXTRAVALIDATOR);
	
	/**
	 * Consulta que retorna los CASE WHEN para recuperar el valor de iteruser o de userprofilevalue
	 * en función de si son campos estructurados o no.
	 */
	private static final String CSV_EXPORT_GET_FIELD_ORIGIN = new StringBuilder()
	.append(" SELECT GROUP_CONCAT(                                                                                                      \n")
	.append("     CASE                                                                                                                  \n")
	.append("         WHEN fieldname IN ('pwd', 'otp_code', 'otp_button', 'newsletter', 'XYZ_FIELD_EXTRAVALIDATOR_ZYX') THEN            \n")
	.append("             CONCAT ('WHEN ''', fieldname, ''' THEN ''''')                                                                 \n")
	.append("         ELSE                                                                                                              \n")
	.append("             CONCAT ('WHEN ''', fieldname, ''' THEN CONCAT(''\"'', REPLACE(u.', fieldname, ', ''\"'', ''\"\"''), ''\"'')') \n")
	.append("     END                                                                                                                   \n")
	.append("     SEPARATOR '\n'                                                                                                        \n")
	.append(" ) AS mapping                                                                                                              \n")
	.append(" FROM userprofile                                                                                                          \n")
	.append(" WHERE structured                                                                                                          \n")
	.toString();
	
	/**
	 * §;§
	 */
	private static final String ROWS_SEPARATOR = String.format("%1$s%2$s%1$s", StringPool.SECTION, StringPool.SEMICOLON);
	
	/**
	 * Consulta para recuperar toda la informacion de los usuarios a exportar. Compone una fila con los datos separados por
	 * el caracter ';' de forma que puedan volcarse al fichero y no haya que procesarlos.
	 */
	private static final String	CSV_EXPORT_GET_ROWS	= String.format(new StringBuilder()
		.append("SELECT CONCAT(                                                                                             	  				\n")
		.append("    GROUP_CONCAT(                                                                                                				\n")
		.append("    IFNULL(                                                                                                      				\n")
		.append("        CASE p.fieldname                                                                                         				\n")
		.append("            %%1$s                                                                                                 				\n")
		.append("            ELSE CONCAT('\"', REPLACE(v.fieldvalue, '\"', '\"\"'), '\"')                                         				\n")
		.append("        END                                                                                                      				\n")
		.append("        , '')                                                                                                    				\n")
		.append("        ORDER BY FIND_IN_SET(p.profilefieldid, '%%2$s') SEPARATOR '%1$s'                                          				\n")
		.append("       ),                                                														  				\n")
		.append("       '%1$s',                                                													  				\n")
		.append("       IFNULL (CONCAT('\"', DATE_FORMAT(u.userexpires, 		'%%%%Y-%%%%m-%%%%d %%%%H:%%%%i:%%%%s'), '\"','%1$s'),'%1$s'), 	\n")
		.append("       IFNULL (CONCAT('\"', DATE_FORMAT(u.registerdate, 		'%%%%Y-%%%%m-%%%%d %%%%H:%%%%i:%%%%s'), '\"','%1$s'),'%1$s'), 	\n")
		.append("       IFNULL (CONCAT('\"', DATE_FORMAT(u.lastlogindate, 		'%%%%Y-%%%%m-%%%%d %%%%H:%%%%i:%%%%s'), '\"','%1$s'),'%1$s'), 	\n")
		.append("       IFNULL (CONCAT('\"', DATE_FORMAT(u.updateprofiledate, 	'%%%%Y-%%%%m-%%%%d %%%%H:%%%%i:%%%%s'), '\"'),'')  				\n")
		.append("    ) AS csvrow                                                                                                  				\n")
		.append("FROM iterusers u                                                                                                 				\n")
		.append("JOIN userprofile p                                                                                               				\n")
		.append("LEFT JOIN userprofilevalues v on  v.usrid = u.usrid                                                              				\n")
		.append("                              AND v.profilefieldid = p.profilefieldid                                            				\n")
		.append("%%5$s                                                                                                             				\n") // Filtros para newsletters
		.append("WHERE IFNULL(u.delegationid, 0) = %%4$d AND FIND_IN_SET(p.profilefieldid, '%%2$s')                                 			\n")
		.append("%%3$s                                                                                                             				\n")
		.append("GROUP BY u.usrid;                                                                                                				\n")
		.toString(), ROWS_SEPARATOR);
	
	/**
	 * Plantilla para componer los filtros para la consulta CSV_EXPORT_GET_ROWS. Se basa en OPTIONAL_FILTER pero
	 * no usa la tabla DLFileEntry.
	 */
	private static final String OPTIONAL_FILTER_WITH_IFNULL = new StringBuilder()
    	.append("\n 0 %s (SELECT COUNT(*) 																							          \n")
	    .append("         FROM userprofile userprofile1																						  \n")
		.append("         INNER JOIN formfield formfield1                 ON formfield1.profilefieldid    = userprofile1.profilefieldid       \n")
		.append("         INNER JOIN formtab formtab1                     ON formtab1.tabid               = formfield1.tabid                  \n")
		.append("         INNER JOIN form form1                           ON form1.formid                 = formtab1.formid                   \n")
		.append("         INNER JOIN userprofilevalues userprofilevalues1 ON userprofile1.profilefieldid  = userprofilevalues1.profilefieldid \n")
		.append("         WHERE form1.groupid = %s 																						      \n")
		.append("             AND userprofilevalues1.usrid = iterusers.usrid 																  \n")
		.append("             AND formfield1.fieldid = '%s' 																				  \n")
		.append("             AND ( %s	)															  \n")
	    .append("         )\n")
	    .toString();
	
	/**
	 * Codificacion del fichero CSV.
	 */
	final static private byte[] BOM_UTF8 = new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF};
	/**
	 * Sirven para componer el fichero CSV antes de adjuntarlo al response.
	 * Existen como atributo para que sea accesible desde los metodos initCSVFile() y buildCSVFile() que 
	 * son ejecutados como callback desde executeQueryAsResultSet().
	 */
	private HttpServletResponse _response;
	private String _colummNames;
	private String _structured;
	private String _translatedColumns;
	//nombre de las columnas fijas de fecha
	String[] _columnsDateNames  =  {"userexpires", "registerdate", "lastlogindate", "updateprofiledate"};
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ServiceError
	 * @throws DocumentException
	 */
	// IMPORTANTE, estas cabeceras unicamente valen para la respuesta al flex, no vale como guia para montar el excel ni el detalle de usuario
	private Document getHeaders(String groupId) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException
	{
		_log.trace(new StringBuilder("Into getHeads"));
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null");

		// Consulta final
		final String query = String.format(GET_HEADERS_COLUMNS, FORM_REGISTER_TYPE, groupId);
		_log.debug(new StringBuilder("Query: ").append(query));
		Document xmlData = PortalLocalServiceUtil.executeQueryAsDom(query);
		ErrorRaiser.throwIfNull(xmlData, "Result query is null");

		// Obtenemos el elemento root
		Element dataRoot = xmlData.getRootElement();

		final XPath xpath = SAXReaderUtil.createXPath("/rs/row");

		List<Node> nodes = xpath.selectNodes(dataRoot);

		/* Formato que debe tener el xml: 
		 * <rs> 
		 * 	<columns avatarurl="7fda8ee9-362f-11e3-a409-f70f62be92a0" firstname="546173a3-362e-11e3-a409-f70f62be92a0" formid="2ebe84b3319a11e38d920017a44e2b78"
		 * 		lastname="8cfc0b9c-362e-11e3-a409-f70f62be92a0" secondlastname="b862789b-362e-11e3-a409-f70f62be92a0"> 
		 * 		<tab id="2ebf6aaa319a11e38d920017a44e2b78" name="XYZ_DEFAULT_TAB_NAME_ZYX"> 
		 * 			<column id="3209cf12-325e-11e3-b39b-3618d4e1e9ec" inputctrltype="text" name="provincia" type="string"></column> 
		 * 			<column id="35dae4ca-3259-11e3-b39b-3618d4e1e9ec" inputctrltype="text" name="telefono"  type="string"></column> 
		 * 			... 
		 * 		</tab>
		 * 		...
		 * </columns>
		 * </rs>
		 */

		// xml que devolveremos
		Document xml = SAXReaderUtil.read("<rs/>");

		// Obtenemos el nodo padre
		Element nodeRoot = xml.getRootElement();
		Element columns = nodeRoot.addElement("columns");

		String beforeTabId = null;
		// Para quedarnos con el tab que estamos trabajando
		Element actualTab = null;

		// Controla que se haya puesto el nombre del tab de los campos obligatorios
		boolean tabNameForMandatoryFieldsPuPost = false;

		// Recorremos los datos
		int indexLabel = 1;
		for (int n = 0; n < nodes.size(); n++)
		{
			// Pintamos el id del formulario la primera vez
			if (n == 0)
			{
				final String formId = XMLHelper.getTextValueOf(nodes.get(n), 	"@formid"		);
				ErrorRaiser.throwIfNull(formId, "formid is null");
				columns.addAttribute("formid", formId);
			}

			final String tabId = XMLHelper.getTextValueOf(nodes.get(n), 		"@tabid"		);
			ErrorRaiser.throwIfNull(tabId, 			"tabId is null");

			final String tabName = XMLHelper.getTextValueOf(nodes.get(n), 		"@tabname"		);
			ErrorRaiser.throwIfNull(tabName, 		"tabName is null");

			final String type = XMLHelper.getTextValueOf(nodes.get(n), 			"@type"			);
			ErrorRaiser.throwIfNull(type, 			"type is null");

			final String formFieldId = XMLHelper.getTextValueOf(nodes.get(n), 	"@fieldid"		);
			ErrorRaiser.throwIfNull(formFieldId, 	"profileFieldId is null");

			final String columnname = XMLHelper.getTextValueOf(nodes.get(n), 	"@columnname"	);
			ErrorRaiser.throwIfNull(columnname, 	"columName is null");

			final String inputctrltype = XMLHelper.getTextValueOf(nodes.get(n), "@inputctrltype");
			ErrorRaiser.throwIfNull(inputctrltype, 	"inputctrltype is null");

			final String identificator = XMLHelper.getTextValueOf(nodes.get(n), "@identificator");
			
			final boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(nodes.get(n), "@structured") );

			// Primer tab que recibimos que contiene un campo estructurado
			if (!tabNameForMandatoryFieldsPuPost && structured)
			{
				List<Node> nodeAux = nodeRoot.selectNodes("/rs/columns");
				((Element) nodeAux.get(0)).addAttribute("tabname", tabName);
				tabNameForMandatoryFieldsPuPost = true;
			}

			// Los siguientes campos fijos (tabla iterusers) se colocan como atributos del usuario en lugar de columnas
			if (structured)
			{
				columns.addAttribute(PROFILE.concat(identificator), identificator);
			}
			else
			{
				// Campos NO estructurados
				// El tab es nuevo
				if (Validator.isNull(beforeTabId) || !tabId.equals(beforeTabId))
				{
					beforeTabId = tabId;
					// Creamos el nuevo tab
					actualTab = columns.addElement("tab");
					actualTab.addAttribute("id",   tabId);
					actualTab.addAttribute("name", tabName);
				}

				// Aniadimos los campos de la columna
				Element column = actualTab.addElement("column");
				column.addAttribute("l", "i"+(indexLabel));
				indexLabel++;
				column.addAttribute("id",            formFieldId);
				column.addAttribute("name",          columnname);
				column.addAttribute("fieldtype",     type);
				column.addAttribute("inputctrltype", inputctrltype);
			}
		}

		return xml;
	}
	
	
	/**
	 * 
	 * @param groupId
	 * @param xmlQueryFiltersAndOrders
	 * @return
	 * @throws ServiceError
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws DocumentException
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	// Monta la consulta para obtener los ids de los usuarios ordenados y filtrados
	private String getQueryToUsersId(String groupId, String xmlQueryFiltersAndOrders) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{		
		_log.trace(new StringBuilder("In getQueryToUsersId"));
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null, 2");
		
		// Predicados para la tabla de iterusers
		StringBuilder fixPredicates = new StringBuilder("");
		// Predicados para campos fecha de la tabla de iterusers
		StringBuilder datePredicates = new StringBuilder("");
		// Filtro para newsletters
		String newsletterFilter = StringPool.BLANK;
		// Subconjunto de usuarios
		String usrsubset = StringPool.BLANK;
		// Predicados para la tabla de userprofilevalues
		StringBuilder optionalPredicates = new StringBuilder("");
		// Sentido de ordenacion por defecto
		String ascDesc = "asc";
		// Campo de ordenacion por defecto
		String sortField = "usrname";				
		
		// Nos llegan filtros y/o ordenaciones
		if (Validator.isNotNull(xmlQueryFiltersAndOrders))
		{
			_log.trace("Query with filters and/or order");
			Document xmlFiltersAndOrder = SAXReaderUtil.read(xmlQueryFiltersAndOrders);
			Element rootNode = xmlFiltersAndOrder.getRootElement();

			final String fieldToOrderFor = XMLHelper.getTextValueOf(rootNode, "order/@columnid");
			ascDesc 		 			 = (XMLHelper.getLongValueOf(rootNode, "order/@asc", 0) == 0) ? "asc" : "desc";

			if (Validator.isNotNull(fieldToOrderFor))
				sortField = fieldToOrderFor;
			else
				_log.debug("Ordenation found but is not correct, using default order");

			// Recorremos los filtros
			List<Node> filters = rootNode.selectNodes("/rs/filters/filter");

			for (int f = 0; f < filters.size(); f++)
			{
				String fieldToFilterWith = XMLHelper.getTextValueOf(filters.get(f), "@columnid");

				if (Validator.isNotNull(fieldToFilterWith))
				{
					final boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(filters.get(f), "@structured") );
					
					// Se quiere filtrar por un campo fijo(de tabla iterusers que forman parte del form registro) 
					if (structured)
					{
						// Si la operacion que se pide es notcontain o distinct aniadimos un ifnull para que salgan los campos con valor null
						final String operator = XMLHelper.getTextValueOf(filters.get(f), ("@operator"));
						String mysqlOperation = null;

						if (operator.equalsIgnoreCase("notcontain") || operator.equalsIgnoreCase("distinct"))
						{
							// Convertimos la operacion de flex a mysql. 
							mysqlOperation = flexOperationToMysqlOperation( String.format("ifnull(%s, '') ", fieldToFilterWith), filters.get(f).asXML() );
						}
						else
						{
							mysqlOperation = flexOperationToMysqlOperation(fieldToFilterWith, filters.get(f).asXML());
						}
						ErrorRaiser.throwIfNull(mysqlOperation, "Imposible to transform flex operation into mysql operation");

						if (fixPredicates.length() > 1)
						{
							fixPredicates.append(" AND ").append(mysqlOperation);
						}
						else
						{
							fixPredicates.append(mysqlOperation);
						}					
					}
					//se quiere filtrar por un campo fecha (de tabla iterusers pero que no forman parte del form registro)
					else if(isIUDateColumn(fieldToFilterWith))
					{
						String mysqlOperation = null;
						mysqlOperation = getDateFilterOperation(fieldToFilterWith,filters.get(f).asXML());
						
						if (datePredicates.length() > 1)
						{
							datePredicates.append(" AND ");
						}
						datePredicates.append(mysqlOperation);

						ErrorRaiser.throwIfNull(mysqlOperation, "Imposible to transform flex operation into mysql operation, part 2");
					}
					// Se requiere filtrar por newsletter
					else if ("schedulenewsletter".equals(fieldToFilterWith))
					{
						newsletterFilter = getNewsletterFilter(xmlFiltersAndOrder);
					}
					// Se quiere filtrar por un campo opcional (tabla userprofilevalues)
					else
					{
						// Obtenemos el tipo que se nos manda ya que los binarios necesitan otra consulta.
						final String type = XMLHelper.getTextValueOf(filters.get(f), "@fieldtype");
						// Obtenemos la operacion que se quiere realizar. Se compondra la squl de diferente manera
						String operator = XMLHelper.getTextValueOf(filters.get(f),   "@operator");
						
						String booleanOperator = (operator.equals("notcontain") || operator.equals("distinct")) ? " = " : " < ";
						
						String mysqlOperation;
						
						if (type.equalsIgnoreCase("binary"))
						{
							mysqlOperation = flexOperationToMysqlOperation(" dlfileentry1.description ", filters.get(f).asXML());
							
							mysqlOperation = String.format(OPTIONAL_BINARY_FILTER, booleanOperator, groupId, fieldToFilterWith, mysqlOperation);
							
							if (operator.equals("notcontain") || operator.equals("distinct"))
							{
								mysqlOperation = mysqlOperation.replaceAll("not like", "like");
								mysqlOperation = mysqlOperation.replaceAll("!=", "=");
								mysqlOperation = mysqlOperation.replaceAll(" NOT IN ", " IN ");
							}
						}
						else
						{
							if (operator.equals("notcontain") || operator.equals("distinct"))
							{
								mysqlOperation = flexOperationToMysqlOperation(" IFNULL(userprofilevalues1.fieldvalue, '') ", filters.get(f).asXML());
								
								mysqlOperation = String.format(OPTIONAL_FILTER_WITH_IFNULL,        booleanOperator, groupId, fieldToFilterWith, mysqlOperation);
								mysqlOperation = mysqlOperation.replaceAll("not like", "like");
								mysqlOperation = mysqlOperation.replaceAll("!=", "=");
								mysqlOperation = mysqlOperation.replaceAll(" NOT IN ", " IN ");
							}
							else
							{
								mysqlOperation = flexOperationToMysqlOperation(" userprofilevalues1.fieldvalue ", filters.get(f).asXML());
								
								mysqlOperation = String.format(OPTIONAL_FILTER,        booleanOperator, groupId, fieldToFilterWith, mysqlOperation);
							}
						}
						
						if (optionalPredicates.length() > 1)
						{
							optionalPredicates.append(" AND ");
						}
						optionalPredicates.append(mysqlOperation);

						ErrorRaiser.throwIfNull(mysqlOperation, "Imposible to transform flex operation into mysql operation, part 3");
					}
				}
			}
			
			// Recupera, si se ha indicado, el subconjunto de usuarios sobre el que trabajar
			usrsubset = StringUtil.merge(XMLHelper.getStringValues(rootNode.selectNodes("usrsubset/user"), "@usrid"),StringPool.COMMA, StringPool.APOSTROPHE);
			if (Validator.isNotNull(usrsubset))
				usrsubset = String.format("iterusers.usrid IN (%s)", usrsubset);
		}
		else
		{
			_log.debug("Withoug filters nor ordenation");
		}
		
		StringBuilder predicates = new StringBuilder(fixPredicates);
		if (optionalPredicates.length() > 0)
		{
			if (predicates.length() > 0) predicates.append(" AND ");
			predicates.append(optionalPredicates);
		}
		if(datePredicates.length() > 0 )
		{
			if ( predicates.length() > 0) 
				predicates.append(" AND ");
			predicates.append(datePredicates);
		}
		if (usrsubset.length() > 0)
		{
			if (predicates.length() > 0) 
				predicates.append(" AND ");
			predicates.append(usrsubset);
		}
		
		String predicatesStr = predicates.toString().replaceAll("\"", "\"\"");
		
		String previousSql = String.format(GENERATE_QUERY, groupId, predicatesStr, sortField, ascDesc, newsletterFilter);
		_log.debug(new StringBuilder("Previous query: ").append(previousSql));	
		
		List<Object> list = PortalLocalServiceUtil.executeQueryAsList(previousSql);		
		ErrorRaiser.throwIfNull(previousSql);		
		
		String finalSql = (String)list.get(0);
		_log.debug(new StringBuilder("getQueryToUsersId returns: ").append(finalSql));
		return finalSql;
	}

	
	/**
	 * 
	 * @param groupId
	 * @param xmlQueryFiltersAndOrders
	 * @return
	 * @throws ServiceError
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws DocumentException
	 */
	// Monta la sql para obtener los datos del usuario, tanto los opcionales como los fijos
	private String getQueryToUsersData(String groupId, Document xml, Document usersIds, Boolean includeInSelectiusrDates) 
			throws ServiceError, SecurityException, NoSuchMethodException,DocumentException
	{
		_log.trace(new StringBuilder("In getQueryToUsersData"));
		
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null, 3");

		// Obtengo los ids de las columnas (formfield.profilefieldid)
		List<Node> nodes = xml.selectNodes("/rs/columns/tab/column");

		// Identificador de columnas opcionales (userprofilevalues)
		StringBuilder varColumnIds = new StringBuilder();

		for (int n = 0; n < nodes.size(); n++)
		{
			if (varColumnIds.length() > 0)
			{
				varColumnIds.append(",");
			}
			varColumnIds.append("'").append(XMLHelper.getTextValueOf(nodes.get(n), "@id")).append("'");
		}

		// Identificador de columnas fijas (iterusers)
		StringBuilder auxFixColumnNames = new StringBuilder();
		Element columns = (Element)xml.selectSingleNode("/rs/columns");
		
		for (Attribute attr : columns.attributes())
		{
			if (attr.getName().startsWith(PROFILE))
				auxFixColumnNames.append( String.format("iterusers.%s, ", attr.getValue()) );
		}

		// En funcion de los campos declarados en el formulario de registro, cambiamos los campos pedidos en el select sobre la tabla iterusers.
//		if (Validator.isNotNull(XMLHelper.getTextValueOf(xml, "/rs/columns/@aboid"         )))
//		{
//			auxFixColumnNames.append("iterusers.aboid, ");
//		}
//		if (Validator.isNotNull(XMLHelper.getTextValueOf(xml, "/rs/columns/@firstname"     )))
//		{
//			auxFixColumnNames.append("iterusers.firstname, ");
//		}
//		if (Validator.isNotNull(XMLHelper.getTextValueOf(xml, "/rs/columns/@avatarurl"     )))
//		{
//			auxFixColumnNames.append("iterusers.avatarurl, ");
//		}
//		if (Validator.isNotNull(XMLHelper.getTextValueOf(xml, "/rs/columns/@lastname"	   )))
//		{
//			auxFixColumnNames.append("iterusers.lastname, ");
//		}
//		if (Validator.isNotNull(XMLHelper.getTextValueOf(xml, "/rs/columns/@secondlastname")))
//		{
//			auxFixColumnNames.append("iterusers.secondlastname, ");
//		}
		
		// Cuando includeInSelectiusrDates la select debe incluir las columnas de iterusers(aboinfoexpires, userexpires, lastlogindate, updateprofiledate )
		if (includeInSelectiusrDates)
		{
			auxFixColumnNames.append("iterusers.userexpires, ");
			auxFixColumnNames.append("iterusers.registerdate, ");
			auxFixColumnNames.append("iterusers.lastlogindate, ");
			auxFixColumnNames.append("iterusers.updateprofiledate, ");
		}

		// Quitamos la ultima comilla
		String fixColumnNames = "";
		if (Validator.isNotNull(auxFixColumnNames) && auxFixColumnNames.length() > 0)
		{
			fixColumnNames = auxFixColumnNames.substring(0, auxFixColumnNames.length() - 1);
		}

		// Filtros para campos opcionales
		StringBuilder predicates = new StringBuilder();
		if (Validator.isNotNull(usersIds))
		{
			List<Node> ids = usersIds.selectNodes("/rs/row");
			predicates.append( String.format(" iterusers.usrid in('%s') ", StringUtils.join(XMLHelper.getStringValues(ids, "@id"), "','")) );
		}		
		
		String previousSql = String.format(USERS_DATA, groupId, fixColumnNames, varColumnIds.toString(), predicates);
		_log.debug(previousSql);		
		
		List<Object> list = PortalLocalServiceUtil.executeQueryAsList(previousSql);		

		final String sql = (String)list.get(0);
		_log.debug(sql);
		
		return sql;
	}


	// Monta un xml con la cabecera y los ids de los usuarios segun los filtros y ordenaciones pasados
	public String getUsersId(String groupId, String xmlQueryFiltersAndOrders) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		_log.trace("In getUsersId function");		
		
		Long t1 = Calendar.getInstance().getTimeInMillis();
		Document xml = getHeaders(groupId);
		
		// Obtenemos la consulta
		final String sql = getQueryToUsersId(groupId, xmlQueryFiltersAndOrders);
		Long l1 = Calendar.getInstance().getTimeInMillis();
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql, true, "users", "user");
		
		if (_log.isDebugEnabled())
			_log.debug( String.format("Query getUsersId: %dms", Calendar.getInstance().getTimeInMillis()-l1) );

    	// Obtenemos el nodo padre (rs)
    	Element rsNode = xml.getRootElement();    	
    	// Meto el xml de datos del usuario dentro del xml que se va a devolver (el que ya contenia la cabecera)
    	rsNode.add(result.getRootElement());
    	
    	// Aniadimos los filtros al xml resultante
    	if (Validator.isNotNull(xmlQueryFiltersAndOrders))
    	{
			Element xmlFiltersAndOrders = SAXReaderUtil.read(xmlQueryFiltersAndOrders).getRootElement();			
			
			// Obtenemos los filtros pasados
			Node allFilters = xmlFiltersAndOrders.selectSingleNode("/rs/filters");
			if (Validator.isNotNull(allFilters))
				rsNode.add(allFilters.detach());
			
			Node nodoOrder = xmlFiltersAndOrders.selectSingleNode("/rs/order");			
			if (Validator.isNotNull(nodoOrder))
				rsNode.add(nodoOrder.detach());	
			else
			{
				Element order = rsNode.addElement("order");
				order.addAttribute("columnid", DEFAULT_FIELD_ORDER); 
			}					
		}
    	else
    	{
    		// No nos han mandado filtros ni la ordenacion, los aniadimos a mano
			rsNode.addElement("filters");			
			Element order = rsNode.addElement("order");
			order.addAttribute("columnid", DEFAULT_FIELD_ORDER); 
		}
    	
    	String toReturn = xml.asXML(); 
    	
    	_log.debug(toReturn);
    	
    	if (_log.isDebugEnabled())
    		_log.debug( String.format("Total getUsersId: %dms", Calendar.getInstance().getTimeInMillis()-t1) );
    	
		return toReturn;
	}	
	
	
	// Monta un xml con la cabecera y todos los datos de los usuarios solicitados
	public String getGridValues(String xmlGroupIdAndUsersIds) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		Long t1 = Calendar.getInstance().getTimeInMillis();
		_log.trace("In getGridValues function");		
		
		Document groupAndIds = SAXReaderUtil.read(xmlGroupIdAndUsersIds);
		String groupId 		 = XMLHelper.getTextValueOf(groupAndIds, "/rs/@id");
		
		// Obtenemos las cabeceras
		Document xml = getHeaders(groupId);
		
		final String sql 	 = getQueryToUsersData(groupId, xml, groupAndIds, true);
    	String[] columsCData = XMLHelper.getStringValues(xml.selectNodes("/rs/columns/tab//column/@l"));
		
		Long l1 = Calendar.getInstance().getTimeInMillis();
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql, true, "users", "user", columsCData, true);
		if (_log.isDebugEnabled())
			_log.debug( String.format("Query getGridValues: %dms", Calendar.getInstance().getTimeInMillis()-l1) );
		
		decryptGDPR(result, "/users/user");
    	
    	// Obtenemos el nodo padre (rs) y metemos el xml de resultados dentro del xml que se va a devolver (el que ya contenia las columnas) 
    	xml.getRootElement().add(result.getRootElement());    	

    	String toReturn = xml.asXML();
    	if (_log.isDebugEnabled())
    		_log.debug( String.format("Total getGridValues: %dms", Calendar.getInstance().getTimeInMillis()-t1) );
    	
		return toReturn;		
	}
	
	// Desencripta todos los atributos y nodos texto del XML
	private void decryptGDPR(Document dom, String xpath) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		if (_log.isDebugEnabled())
			_log.debug( String.format("Before decryptGDPR users data:\n %s", dom.asXML()) );
		
		// Lo ideal hubiese sido un XPATH como "/child::element()/child::element()/child::element()" pero el XPATH 1.0 no lo soporta
		// Se desencriptan los valores de los campos no estructurados
		List<Node> nonStructured = dom.selectNodes( String.format("%s/*", xpath) );
		for (Node node : nonStructured)
		{
		
			if (node instanceof Element)
			{
				String oldText = node.getStringValue();
				String newText = IterUserTools.decryptGDPR(oldText);
				if (oldText != newText)
				{
					// Se borra
					((Element)node).setText("");
					// Se añade como CDATA (No hay forma de sustituir directamente el CDATA)
					((Element)node).addCDATA(newText);
				}
			}
		}
		
		List<Node> structured = dom.selectNodes( String.format("%s/@*", xpath) );
		for (Node node : structured)
		{
			node.setText( IterUserTools.decryptGDPR(node.getStringValue()) );
		}
		
//		// Se desencriptan los valores de los campos no estructurados
//		List<Node> nonStructured = dom.selectNodes("/child::element()/child::element()/child::element()");
//		for (int i = 0; i < nonStructured.size(); i++)
//			((Element)nonStructured.get(i)).addCDATA( IterUserTools.decryptGDPR(nonStructured.get(i).getStringValue()) );
//		
//		// Se desencriptan los valores de los campos estructurados
//		List<Node> structured = dom.selectNodes("/child::element()/child::element()/attribute::*");
//		for (int i = 0; i < structured.size(); i++)
//			((Attribute)structured.get(i)).setValue( IterUserTools.decryptGDPR(((Attribute)structured.get(i)).getValue()) );
//		
//		if (_log.isDebugEnabled())
//			_log.debug( String.format("After decryptGDPR users data:\n %s", dom.asXML()) );
	}
		
	/**
	 * 
	 * @param xml
	 * @return
	 * @throws ServiceError
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws DocumentException
	 * @throws SQLException
	 * @throws IOException
	 */
	// Elimina los usuarios de la base de datos indicados en una transaccion
	public String deleteUsers(String xmlData) throws 	DocumentException, ServiceError, SecurityException, 
														NoSuchMethodException, IOException, SQLException
	{
		_log.trace(new StringBuilder("Into deleteUsers"));

		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xmlData is null");
		Document dom = SAXReaderUtil.read(xmlData);
		long groupId = XMLHelper.getLongValueOf(dom, "/rs/@groupid");
		PortalUtil.setVirtualHostLayoutSet(IterRequest.getOriginalRequest(), groupId);

		String[] userIdsList = XMLHelper.getStringValues( dom.selectNodes("/rs/row/@usrid") );
		ErrorRaiser.throwIfFalse(Validator.isNotNull(userIdsList), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String userIds = String.format("'%s'", StringUtils.join(userIdsList, ',') );

		// Registra el evento de cancelación de cuenta en todas las newsletters en las que están suscritos los usuarios eliminados.
		// Los eventos se registran antes de la cancelación de cuentas, ya que tras el borrado desaparecen las suscripciones
		NewslettersMetricsUtil.allUsersNewslettersHit(userIds, NewslettersMetricsUtil.HIT.ADMIN_CANCEL_ACCOUNT);
		
		// Se realiza una copia de la información del usuario antes de ser borrado
		for (String userId : userIdsList)
			IterUserBackupMgr.backup(userId);

		// Quitamos la ultima coma y lanzamos la consulta
		String sql = String.format(DELETE_USERS, userIds);
		_log.debug(sql);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		return xmlData;
	}
	

	/**
	 * 
	 * @param field
	 * @param xmlFilter
	 * @return
	 * @throws ServiceError
	 * @throws DocumentException
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	// Tracude la operacion que nos manda el flex a mysql. Se usa en el filtrado de usuarios para pintar flex el datagrid
	private String flexOperationToMysqlOperation(String subject, String xmlFilter) throws ServiceError, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		_log.trace("In flexOperationToMysqlOperation");

		ErrorRaiser.throwIfNull(xmlFilter, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "field or/and xmlFilter is null");

		/* Recibimos un filter como este:
		 * 
		 * <filter operator="" fieldtype=""> 
		 * 	<values>
		 * 		<value><![CDATA[Madrid]]></value>
		 * 		<value><![CDATA[Barcelona]]></value> 
		 * 	</values> 
		 * </filter>
		 */

		// Empezamos a leer el xml
		Document xml = SAXReaderUtil.read(xmlFilter);
		Element rootNode = xml.getRootElement();
		
		String field       = XMLHelper.getTextValueOf(rootNode, "/filter/@columnid", "");
		boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(rootNode, ("/filter/@structured")) );

		// Obtenemos la operacion que nos manda el flex
		String operator = XMLHelper.getTextValueOf(rootNode, ("/filter/@operator"));
		ErrorRaiser.throwIfNull(operator, "operator is null or empty");
		
		String originalOperator = operator;

		// Obtenemos los valores
		List<Node> nodeValues = rootNode.selectNodes("/filter/values//value");
		ErrorRaiser.throwIfNull(nodeValues, "values is null or empty");

		StringBuilder values = new StringBuilder();

		// Obtenemos el tipo
		String type = XMLHelper.getTextValueOf(rootNode, ("/filter/@fieldtype"));
		ErrorRaiser.throwIfNull(type, "type is null or empty");

		// IMPORTANTE, de momento las fechas y numeros se comportaran como
		// strings
		if (type.equals("date") || type.equals("number"))
		{
			type = "string";
		}

		// Mapa con las equivalencias
		HashMap<String, String> hM = new HashMap<String, String>();

		// Para varios tipos
		// Fechas
		hM.put("beforedate",	" str_to_date(operando1, '%Y%m%d%H%i%s') < str_to_date('operando2', '%Y%m%d%H%i%s')  ");
		hM.put("afterdate", 	" str_to_date(operando1, '%Y%m%d%H%i%s') > str_to_date('operando2', '%Y%m%d%H%i%s')  ");
		hM.put("fromdate", 		" str_to_date(operando1, '%Y%m%d%H%i%s') <= str_to_date('operando2', '%Y%m%d%H%i%s') ");
		hM.put("todate", 		" str_to_date(operando1, '%Y%m%d%H%i%s') => str_to_date('operando2', '%Y%m%d%H%i%s') ");
		// Numerico // Ponemos el casteo a pelo con 4 decimales, no hay tiempo para hacerlo mejor.
		hM.put("smaller", 		" cast(operando1 AS DECIMAL(65, 4)) < operando2 ");
		hM.put("greater", 		" cast(operando1 AS DECIMAL(65, 4)) > operando2 ");

		// Cadenas
		hM.put("equals", 		" operando1 = operando2 ");
		hM.put("distinct", 		" operando1 != operando2 		   ");
		
		hM.put("contain", 		" operando1 like '%operando2%'     ");
		hM.put("notcontain", 	" operando1 not like '%operando2%' ");
		
		hM.put("startBy", 		" operando1 like 'operando2%' 	   ");
		hM.put("endBy", 		" operando1 like '%operando2'      ");
		
		ErrorRaiser.throwIfFalse(hM.containsKey(operator), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Operator flex is not a know mysql operation");

		ErrorRaiser.throwIfFalse(!(PropsValues.ITER_GDRP_ENCRYPT_USRDATA && (!structured || UserUtil.PRF_ENCRYPTABLE_FIELDS.contains(field)) &&
									UserUtil.INVALID_FILTER_WITH_ENCRYPTABLE_FIELDS.contains(operator) &&
									!type.equalsIgnoreCase("array")), 
				IterErrorKeys.XYZ_E_GDPR_INVALID_FILTER_ZYX, "The filter is not valid with crypted user data"); 
		
		for (int v = 0; v < nodeValues.size(); v++)
		{
			
			operator = XMLHelper.getTextValueOf(rootNode, ("/filter/@operator"));
			
			// Obtenemos el valor y lo escapamos
			String value  	= StringEscapeUtils.escapeSql(nodeValues.get(v).getText());
			
			// No es un binario y:
			// - No es un dato estructurado
			// - Es un dato estrictirado encriptable
			String value2 = (!type.equalsIgnoreCase("binary") && (!structured || UserUtil.PRF_ENCRYPTABLE_FIELDS.contains(field))) 
								? IterUserTools.encryptGDPR(value)
								: value;
			
			// Se comprueba por si no está activada la encriptación
			boolean useObth = !value.endsWith(value2);

			// Los arrays solo tienen dos operaciones, in() y not in()
			if (v == 0 && type.equalsIgnoreCase("array"))
			{
				if (operator.equalsIgnoreCase("contain"))
				{
					values.append(new StringBuilder(subject).append(" in ("));
				}
				else
				{
					values.append(new StringBuilder(subject).append(" not in ("));
				}
			}

			if (type.equalsIgnoreCase("array"))
			{
				values.append("'" + value + "'");
				
				if (useObth)
					values.append(",'" + value2 + "'");
			}
			else
			{
				operator = (String) hM.get(operator);
			}

			if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("binary") || type.equalsIgnoreCase("boolean") ||
				type.equalsIgnoreCase("number") || type.equalsIgnoreCase("date"))
			{
				// Cadena o boolean (se trata el boolean como una cadena). Si no se va a operar con like, ponemos las comillas para que le guste a mysql
				if ((type.equalsIgnoreCase("string") || type.equalsIgnoreCase("binary") || type.equalsIgnoreCase("boolean")) &&
					(operator.indexOf("like") == -1))
				{
					value  = "'" + value  + "'";
					
					if (useObth)
						value2 = "'" + value2 + "'";
				}
				
				if (originalOperator.equals("distinct") && useObth)
				{
					values.append( String.format(" %s NOT IN (%s, %s) ", subject, value, value2) );
				}
				else
				{
					String operatorAux = operator;
					String condition   = operator.replaceAll("operando1", subject).replaceAll("operando2", value);
					
					if (useObth)
						condition = String.format("(%s OR %s)", condition, operatorAux.replaceAll("operando1", subject).replaceAll("operando2", value2));
					values.append(condition);
				}
			}

			// Concatenamos con el siguiente, si es que lo hay
			if (v < nodeValues.size() - 1 && !type.equalsIgnoreCase("array"))
			{
				values.append(" OR userprofilevalues1.fieldvalue ");
			}
			else if (v < nodeValues.size() - 1 && type.equalsIgnoreCase("array"))
			{
				values.append(", ");
			}
		}

		// Cerramos los parentesis (not in('', ''), in('', '') )
		if (type.equalsIgnoreCase("array"))
		{
			values.append(") ");
		}

		return values.toString();
	}

	//Retorna la sentencia sql que se utilizará para filtrar por alguna de las fechas de la tabla iterusers("userexpires","registerdate","lastlogindate","updateprofiledate")
	private String getDateFilterOperation(String subject, String xmlFilter) throws ServiceError, DocumentException
	{
		String predicate = null;
		
		// Empezamos a leer el xml
		Document xml = SAXReaderUtil.read(xmlFilter);
		Element rootNode = xml.getRootElement();

		// Operación a realizar 
		String operator = XMLHelper.getTextValueOf(rootNode, ("/filter/@operator"));
		ErrorRaiser.throwIfNull(operator, "operator is null or empty");

		// Valores recibidos (1-N)
		final String values[] = XMLHelper.getStringValues(rootNode.selectNodes("values//value"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(values), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "values filter is null");

		// Tipo del campo (debe ser date)
		String type = XMLHelper.getTextValueOf(rootNode, ("/filter/@fieldtype"));
		ErrorRaiser.throwIfFalse(type.equals("date"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "type filter not is date");
		
		predicate = SQLQueries.buildFilter(subject, type, operator, values, false, false);
		
		return predicate;
	}
	
	// Muestra el detalle de un usuario (crea una pagina html con los datos del usuario indicado)
	public void getUserDetailById(HttpServletRequest request, HttpServletResponse response, String groupId, String userId, String formId, String translatedColumns) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, TransformerException, IOException, InvalidKeyException, NumberFormatException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{			
		_log.trace("In getUserDetailById");

		ErrorRaiser.throwIfNull(request,  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "request is null"   );
		ErrorRaiser.throwIfNull(response, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "response is null"  );
		ErrorRaiser.throwIfNull(groupId,  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null, 8");
		ErrorRaiser.throwIfNull(userId,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "userId is null"    );
		ErrorRaiser.throwIfNull(formId,   IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "formId is null"    );
		
		// Montamos un mapa con la traduccion de las columnas, viene url encoded y los campos separados por ;
        translatedColumns = java.net.URLDecoder.decode(translatedColumns, IterKeys.UTF8);
        String[] aux = translatedColumns.split(";");
        HashMap<String, String> traducedColumns = new HashMap<String, String>();
        for (int i = 0; i < aux.length; i++)
        {
           traducedColumns.put(aux[i].split("=")[0], aux[i].split("=")[1]);
        }

		// UTILIZAMOS ESTA CABECERA UNICAMENTE PARA SACAR LOS DATOS DEL USUARIO (el orden de los campos es incorrecto)
		Document xmlHeaders = getHeaders(groupId);
		ErrorRaiser.throwIfNull(xmlHeaders);

		// Obtengo los ids de las columnas opcionales (formfield.profilefieldid)
		List<Node> nodes = xmlHeaders.selectNodes("/rs/columns/tab/column");
		ErrorRaiser.throwIfNull(nodes, "headers XML are null");

		// Identificador de columnas opcionales (userprofilevalues)
		StringBuilder varColumnIds = new StringBuilder();
		for (int n = 0; n < nodes.size(); n++)
		{
			if (varColumnIds.length() > 0)
			{
				varColumnIds.append(", ");
			}
			// varColumnIds.append("\"").append(PREFIX_ID).append(XMLHelper.getTextValueOf(nodes.get(n), "@id")).append("\"");
			varColumnIds.append(XMLHelper.getTextValueOf(nodes.get(n), "@id"));
		}		

		// Creamos un documento con el id del usuario para que se filtre por él en la consulta
		Document userFilter = SAXReaderUtil.read("<rs/>");		
		Element row = userFilter.getRootElement().addElement("row");
		row.addAttribute("id", userId);
		String sql = getQueryToUsersData(groupId, xmlHeaders, userFilter, false);
		_log.debug(new StringBuilder("Query: ").append(sql));

		// Ejecutamos la consulta
		String[] columsCData = varColumnIds.toString().split(", ");
		// El true es para que no pinte las columnas vacias
		Document userData = PortalLocalServiceUtil.executeQueryAsDom(sql, columsCData, true);
		ErrorRaiser.throwIfNull(userData, "The result of the query to get the user data was null");
		
		decryptGDPR(userData, "/rs/row");

		// Empezamos a generar el xml que transformaremos
		Document xmlGenerated = SAXReaderUtil.read("<formdata/>");
		Element formData = xmlGenerated.getRootElement();
		formData.addAttribute("formid", formId);

		Element deliveryInfo = formData.addElement("delivery-info");
		deliveryInfo.addElement("date-sent");

		Element fieldsGroup = null;
		String beforeTab = null;

		final String subQuery = String.format(IDS_FROM_REGISTER_FORM, groupId);
		List<Node> rows = UserUtil.getUserProfileFields(Long.parseLong(groupId), userId, subQuery).selectNodes("/rs/row");

		// Columnas ya pintadas. Nos sirve para comprobar que un campo opcional con distintos valores se pinta una unica vez
		HashMap<String, String> AllReadyPainted = new HashMap<String, String>();

		for (int r = 0; r < rows.size(); r++)
		{
			String fielId 			 = XMLHelper.getTextValueOf(rows.get(r), "@fieldid");
			String fieldName 		 = XMLHelper.getTextValueOf(rows.get(r), "@fieldname");
			final boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(rows.get(r), "@structured") );

			// Campos que no se pintan, tampoco si es un campo repetido y ya se ha pintado antes (saldria repetido varias veces).
			if (!fieldName.equals(UserUtil.PRF_FIELD_USRPWD) && !fieldName.equals(UserUtil.PRF_FIELD_EXTRAVALIDATOR) &&
				!AllReadyPainted.containsKey(fielId))
			{
				String tabId = XMLHelper.getTextValueOf(rows.get(r), 	 "@tabid");
				ErrorRaiser.throwIfNull(tabId, "tabId of query is null");
				
				String tabname = XMLHelper.getTextValueOf(rows.get(r), 	 "@tabname");
				String fieldType = XMLHelper.getTextValueOf(rows.get(r), "@fieldtype");				

				if (Validator.isNull(beforeTab) || !tabId.equalsIgnoreCase(beforeTab))
				{
					beforeTab = tabId;

					// Creamos la pestania/tab
					fieldsGroup = formData.addElement("fieldsgroup");
					fieldsGroup.addAttribute("name", tabname);
				}

				Element field = fieldsGroup.addElement("field");
				field.addAttribute("id", 	fielId);
				field.addAttribute("type", 	fieldType);

				// Obtenemos el nombre del campo
				String fieldValue = (structured) ? traducedColumns.get(fieldName) : fieldName;
				
				Element labelBefore = field.addElement("labelbefore");
				labelBefore.addCDATA(fieldValue);

				// Para compatibilidad con la transformación
				Element labelAfter = field.addElement("labelafter");
				labelAfter.addCDATA("");

				Element data = field.addElement("data");

				String xpath = "";
				String values[] = null;
				// Es un campo de la tabla iterusers
				if (structured)
				{
					// Obtenemos el valor de la columna
					xpath = String.format("/rs/row/@%s", fieldName);
				}
				else
				{
					// Obtenemos el valor de la columna
					String aux2  = XMLHelper.getTextValueOf(xmlHeaders, "/rs/columns/tab/column[@id='" + fielId + "']/@l");
					xpath = new StringBuilder("/rs/row/@").append(aux2).toString();
				}

				values = XMLHelper.getStringValues(userData.selectNodes(xpath),"");

				// Si tiene valor y no se ha pintado ya esa columna (evita pintar campos repetibles mas de una vez)
				if (Validator.isNotNull(values))
				{
					if (fieldType.equals(BINARY_TYPE))
					{
						// Obtenemos la ruta y nombre del binario.
						String fileRoute = values[0];
						final String fileName = FilenameUtils.getBaseName(fileRoute);
						final String fileExtension = FilenameUtils.getExtension(fileRoute);

						Element binary = data.addElement("binary");
						Element name = binary.addElement("name");
						name.setText((new StringBuilder(fileName).append(".").append(fileExtension)).toString());
						Element binlocator = binary.addElement("binlocator");
						binlocator.addAttribute("type", "url");
						binlocator.setText(values[0]);

					}
					else
					{
						// Pueden venir varios valores para el mismo dato
						for (int v = 0; v < values.length; v++)
						{
							Element value = data.addElement("value");
							value.addCDATA(values[v]);
						}
					}
					// Lo almacenamos por si es un campo repetible no pintarlo mas de una vez, valor nos da lo mismo solo usarmos la clave
					AllReadyPainted.put(fielId, "");
				}
			}
		}

		_log.trace("Tranform XML with xslt");
		File webappsFile = new File(PortalUtil.getPortalWebDir()).getParentFile();

		// Ruta donde esta la xsl
		String xslPath = new StringBuilder(File.separator)
						.append("user-portlet").append(File.separator).append("xsl")
						.append(File.separator).append(XSL_DETAIL_USER).toString();

		String xslRoute = new StringBuilder(webappsFile.getAbsolutePath()).append(xslPath).toString();
		File xslFile = new File(xslRoute);

		// Comprobamos que la xsl existe y se puede leer
		ErrorRaiser.throwIfFalse(xslFile.exists() && xslFile.canRead(), IterErrorKeys.XYZ_E_IMPORT_XSL_UNAVAILABLE_ZYX);
		InputStream xslIS = new FileInputStream(xslFile);
		ErrorRaiser.throwIfNull(xslIS);

		Element dataRoot = SAXReaderUtil.read(xslIS).getRootElement();
		ErrorRaiser.throwIfNull(dataRoot, IterErrorKeys.XYZ_E_IMPORT_XSL_UNAVAILABLE_ZYX, "xslIS is incorrect");

		String typeXSLT = XMLHelper.getTextValueOf(dataRoot, "/xsl:stylesheet/xsl:output/@method", null);
		if (Validator.isNull(typeXSLT))
		{
			_log.trace("XSL not contain 'xsl:output/@method'. default: xml");
			typeXSLT = "xml";
		}
		
		// Transformamos el xml con la xsl
		String transformed = XSLUtil.transform(XSLUtil.getSource(xmlGenerated.asXML()), XSLUtil.getSource(dataRoot.asXML()), typeXSLT);

		ServletOutputStream out = null;

		try
		{
			// Especificamos que la respuesta sera de tipo csv
			response.setHeader("Content-Type", "text/html; charset=UTF-8");
			// Esta linea dara error si se llama desde un jsp
			out = response.getOutputStream();

			// Escribimos el contenido ya formado
			/*final String codification = "EFBBBF";				
			out.write(codification.getBytes());*/
			out.write(transformed.getBytes());
			out.flush();
			out.close();
		}
		catch (Exception e)
		{
			_log.error("Error while sending transformed csv content", e);
		}
		finally
		{
			try
			{
				if (out != null)
					out.close();
			}
			catch (Exception e)
			{
				_log.error("Error while closing", e);
			}
		}
	}
	
	//Comprueba si el nombre del campo( fieldName) corresponde con una de las fechas de la tabla iterusers que se muestran en la tabla del interfaz de usuarios
	private Boolean isIUDateColumn(String fieldName)
	{
		Boolean iuDateCol = false;
		if("userexpires".equals(fieldName) || "registerdate".equals(fieldName) || "lastlogindate".equals(fieldName) || "updateprofiledate".equals(fieldName))
			iuDateCol = true;
		
		return iuDateCol;
	}
	
	public String getUserInfo(String userId) throws ServiceError, SecurityException, NoSuchMethodException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException 
	{
		ErrorRaiser.throwIfNull(userId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document user = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_USER_INFO, userId));
		
		decryptGDPR(user, "/rs/row");
		return user.asXML();
	}
	
	public String getUserInfo(long groupId, String userId) throws ServiceError, SecurityException, NoSuchMethodException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException 
	{
		ErrorRaiser.throwIfNull(userId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document user = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_USER_INFO, userId));
		decryptGDPR(user, "/rs/row");
		
		getUserNewsletters(groupId, userId, user);
		return user.asXML();
	}
	
	public String updateUserInfo(String xmlData) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, IOException, SQLException, NoSuchAlgorithmException{
		String retVal = "";

		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		xmlData = StringEscapeUtils.escapeSql(xmlData);

		Element row = (Element) SAXReaderUtil.read(xmlData).getRootElement().selectSingleNode("/rs/row");
		String userId = row.attributeValue("usrid");
		ErrorRaiser.throwIfNull(userId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		StringBuilder fields = new StringBuilder();
		StringBuilder updatedFields = new StringBuilder();

		Iterator<Attribute> itrAttr = row.attributeIterator();

		while (itrAttr.hasNext()){
			Attribute attr = itrAttr.next();
			if (!attr.getName().equals("usrid")){
				updatedFields.append(attr.getName()).append(",");
				String attrVal = "";

				fields.append(attr.getName()).append("=");

				if (attr.getName().equals("pwd"))
					attrVal = IterUserTools.encryptPwd(attr.getValue());
				else
					attrVal = attr.getValue();

				if (!attrVal.isEmpty())
					fields.append("'").append(attrVal).append("'");
				else
					fields.append(StringPool.NULL);

				fields.append(",");
			}
		}

		if (fields.length() > 0){
			String query = "";

			try{
				query = String.format(UPDT_USER_INFO, fields.deleteCharAt(fields.lastIndexOf(",")), userId);
				PortalLocalServiceUtil.executeUpdateQuery(query);
			}catch (ORMException e){
				String eCode = ServiceErrorUtil.getErrorCode(e);

				ErrorRaiser.throwIfFalse(!eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX), IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX);
				ErrorRaiser.throwIfFalse(!eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_EMAIL_ZYX), 	   IterErrorKeys.XYZ_ITR_UNQ_EMAIL_ZYX);
				ErrorRaiser.throwIfFalse(!eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_ABO_ID_ZYX),    IterErrorKeys.XYZ_ITR_UNQ_ABO_ID_ZYX);

				Matcher m = Pattern.compile("XYZ_E_COLUMN_.*._CANNOT_BE_NULL_ZYX").matcher(eCode);
				if (m.find())
					ErrorRaiser.throwIfError(eCode, e.toString());

				throw e;
			}

			query = String.format(GET_UPDTED_USER_FIELDS, updatedFields.deleteCharAt(updatedFields.lastIndexOf(",")), userId);
			retVal = PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
		}
		
		// Suscripciones a newsletters
		processNewsletterSubscriptions(row);

		return retVal;
	}
	
	private static final String SQL_SUSCRIBE_USER_TO_NEWSLETTER = new StringBuilder()
	.append("INSERT INTO schedule_user                                                                                    \n")
	.append("SELECT UUID() scheduleuserid, scheduleid, %2$s userid, NOW() modifieddate, NULL publicationdate, 1 enabled \n")
	.append("FROM schedule_newsletter WHERE scheduleid IN (%1$s)                                                          \n")
	.append("ON DUPLICATE KEY UPDATE modifieddate=NOW(), enabled=1"                                                          )
	.toString();
	private static final String SQL_ENABLE_USER_IN_NEWSLETTER = "UPDATE schedule_user SET enabled=1 WHERE scheduleid IN (%s) AND userid=%s";
	private static final String SQL_DISABLE_USER_IN_NEWSLETTER = "UPDATE schedule_user SET enabled=0 WHERE scheduleid IN (%s) AND userid=%s";
	private static final String SQL_CANCEL_USER_SUBSCRIPTION_TO_NEWSLETTER = "DELETE FROM schedule_user WHERE scheduleid IN (%s) AND userid=%s";
	
	public void processNewsletterSubscriptions(Element row) throws IOException, SQLException
	{
		String userId = StringUtil.apostrophe(row.attributeValue("usrid"));
		String[] scheduleIdList = null;
		String sql = null;
		
		// Recupera las nuevas suscripciones
		scheduleIdList = XMLHelper.getStringValues(row.selectNodes("newsletters/subscription[not(@enabled) and not(@delete)]"), "@scheduleid");
		if (scheduleIdList.length > 0)
		{
			String scheduleIds = StringUtil.merge(scheduleIdList, StringPool.COMMA, StringPool.APOSTROPHE);
			sql = String.format(SQL_SUSCRIBE_USER_TO_NEWSLETTER, scheduleIds, userId);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			// Registra el evento en las métricas.
			NewslettersMetricsUtil.hit(scheduleIds, userId, NewslettersMetricsUtil.HIT.ADMIN_SUBSCRIPTION);
		}
		
		// Recupera las suscripciones a activar
		scheduleIdList = XMLHelper.getStringValues(row.selectNodes("newsletters/subscription[@enabled='true']"), "@scheduleid");
		if (scheduleIdList.length > 0)
		{
			String scheduleIds = StringUtil.merge(scheduleIdList, StringPool.COMMA, StringPool.APOSTROPHE);
			sql = String.format(SQL_ENABLE_USER_IN_NEWSLETTER, scheduleIds, userId);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			// Registra el evento en las métricas.
			NewslettersMetricsUtil.hit(scheduleIds, userId, NewslettersMetricsUtil.HIT.ADMIN_ENABLED_SUBSCRIPTION);
		}
		
		// Recupera las suscripciones a desactivar
		scheduleIdList = XMLHelper.getStringValues(row.selectNodes("newsletters/subscription[@enabled='false']"), "@scheduleid");
		if (scheduleIdList.length > 0)
		{
			String scheduleIds = StringUtil.merge(scheduleIdList, StringPool.COMMA, StringPool.APOSTROPHE);
			sql = String.format(SQL_DISABLE_USER_IN_NEWSLETTER, scheduleIds, userId);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			// Registra el evento en las métricas.
			NewslettersMetricsUtil.hit(scheduleIds, userId, NewslettersMetricsUtil.HIT.ADMIN_DISABLED_SUBSCRIPTION);
		}
		
		// Recupera las nuevas suscripciones a cancelar
		scheduleIdList = XMLHelper.getStringValues(row.selectNodes("newsletters/subscription[@delete='1']"), "@scheduleid");
		if (scheduleIdList.length > 0)
		{
			String scheduleIds = StringUtil.merge(scheduleIdList, StringPool.COMMA, StringPool.APOSTROPHE);
			sql = String.format(SQL_CANCEL_USER_SUBSCRIPTION_TO_NEWSLETTER, scheduleIds, userId);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			// Registra el evento en las métricas.
			NewslettersMetricsUtil.hit(scheduleIds, userId, NewslettersMetricsUtil.HIT.ADMIN_CANCEL_SUBSCRIPTION);
		}
	}

	/* Crea un xml con el resultado de la importacion:
		<d id="" errorCode="" errordetail="">
			<subject><![CDATA[<u name="" email=""/>]]></subject>
		</d>
			
		errorCode es el codigo de error ocurrido en la importacion
        errordetail es la descripción del error o la traza del error
        subject es el campo que identifica el usuario */
	public void importUser(Document xmlResult, List<Object> userprofiles, String url, Node user, String groupId, long defaultUserId, 
						   File workingDirectory, boolean passwordInMD5, boolean deleteUsers, List<String>userProfileNames) throws Exception {
		_log.trace(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("In importUsers"));
		
		final long t0 = Calendar.getInstance().getTimeInMillis();	
		String[] errorCodeAndErrorDetail = null;
		long dlFileEntryTime = 0;
		long userInsertsTime = 0;
		String errorCode   = null;
		String errorDetail = null;
		String userEmail   = null;
		String userName    = null;
		String password    = null;
		
		try
		{	
			StringBuilder iterUserFields    = new StringBuilder();
			StringBuilder iterUserValues    = new StringBuilder();	
			StringBuilder userProfileValues = new StringBuilder();
			
			userName  = XMLHelper.getTextValueOf(user, "./f/usrname");
			userEmail = XMLHelper.getTextValueOf(user, "./f/email"  );
			password  = XMLHelper.getTextValueOf(user, "./f/pwd"    );
			
			errorCodeAndErrorDetail = hasUsernameAndEmailAndPassword(userName, userEmail, password);			
			if (Validator.isNotNull(errorCodeAndErrorDetail))
			{
				errorCode   = errorCodeAndErrorDetail[0];
				errorDetail = errorCodeAndErrorDetail[1];
			}
			else
			{
				userName  = userName.trim();
				ErrorRaiser.throwIfFalse(userName.length() <= 255, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, 
						                 new StringBuilder(com.protecmedia.iter.base.service.util.IterErrorKeys.XYZ_FIELD_TOO_LONG_ZYX).append(" 'usrname'").toString());
				userEmail = userEmail.trim();
				ErrorRaiser.throwIfFalse(userEmail.length() <= 255, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, 
		                 				 new StringBuilder(com.protecmedia.iter.base.service.util.IterErrorKeys.XYZ_FIELD_TOO_LONG_ZYX).append(" 'email'").toString());
				password  = password.trim();
				
				// Se nos dice que la contraseña viene ya en MD5, lo comprobamos (32 caracteres del 0 al 9 o de a a f en minúsculas)
				if (passwordInMD5)
				{				
					ErrorRaiser.throwIfFalse(IterUserTools.isEncryptedPwd(password), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, 
							                 new StringBuilder(XmlioKeys.DETAIL_ERROR_PASSWORD_NOT_IN_MD5).append(": '").append(password).append("'").toString() );	
				}
				
				// Se comprueba que no tiene campos que no están en el formulario de registro
				checkDataNotInTheRegistrationForm(userProfileNames, user);
				
				DLFileEntry dlFileEntryIdAvatar          = null;				
				ArrayList<DLFileEntry> userDlFileEntries = null;
				
				if (deleteUsers)
				{
					dlFileEntryIdAvatar = getDlFileEntryAvatar(userName);
					userDlFileEntries   = getUserDlFileEntries(userName);
				
					// Borramos el usuario
					String escapedUser = StringEscapeUtils.escapeSql(userName);
					final String sql = String.format(DELETE_USER_BEFORE_IMPORT, escapedUser);
					if(_log.isDebugEnabled())					
						_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Sql to delete '").append(userName).append("' user before import it:  \n").append(sql));					
					PortalLocalServiceUtil.executeUpdateQuery(sql);	
					
				}
				
				final String userId = PortalUUIDUtil.newUUID();
			
				// Buscamos los campos que no se incluyen en el formulario de registro del usuario y que podrían venir (aboid, disqusid, facebookid, googleplusid, twitterid, registerdate, lastlogindate, updateprofiledate)				 
				for (int i = 0; errorCode == null && i < FIELDS_NOT_IN_REGISTRATION_FORM.length; i++)
				{
					final String fieldName = FIELDS_NOT_IN_REGISTRATION_FORM[i];
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("fieldname: ").append(fieldName));
					
					String value = XMLHelper.getTextValueOf(user, new StringBuilder("./f/").append(fieldName).toString());
					if(_log.isDebugEnabled())
						_log.debug(XmlioKeys.PREFIX_USER_LOG + "fieldValue: " + null == value ? "null" : "");
					
					if (Validator.isNotNull(value))
					{
						value = value.trim();
						
						// El campo es de tipo fecha, comprobamos su formato (asi el codigo de error sera DATE_FORMAT y no de sql)
						if (FIELDS_NOT_IN_REGISTRATION_FORM_DATE.contains(fieldName))
						{									
							try
							{
								sDF.parse(value);
							}
							catch(Exception e)
							{
								errorCode   = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_DATE_FORMAT;
								errorDetail = IterErrorKeys.XYZ_INVALID_FORMAT_OF_ZYX + " " + fieldName;
								if(_log.isDebugEnabled())
									_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Incorrect date format: ").append(fieldName));
							}
						}
						else
						{
							// Comprobamos la longitud del campo
							checkIterUserFieldLength(fieldName, value);
						}
						iterUserFields.append(fieldName).append(","); 
						
						value = StringEscapeUtils.escapeSql(value);
						if (UserUtil.PRF_ENCRYPTABLE_FIELDS.contains(fieldName))
							value = IterUserTools.encryptGDPR(value);
						
						iterUserValues.append("'" + value + "'").append(",");
					}							
				}
					
				// Recorremos los userprofiles del formulario de registro y los vamos buscando en el usuario
				final int totalUserprofiles = userprofiles.size();
				for (int up = 0; errorCode == null && up < totalUserprofiles; up++)
				{
					final String userProfileId = (((Object[])userprofiles.get(up))[0]).toString();
					final String fieldname     = (((Object[])userprofiles.get(up))[1]).toString();
					final String required      = (((Object[])userprofiles.get(up))[2]).toString();
					final String fieldType     = (((Object[])userprofiles.get(up))[3]).toString();
					final boolean structured   = GetterUtil.getBoolean( (((Object[])userprofiles.get(up))[4]).toString() );
					String xpath = null;
					
					// Es un campo fijo (iterusers)
					if (structured)
					{
						iterUserFields.append(fieldname).append(","); 
						xpath = new StringBuilder("./f/").append(fieldname).toString();				
					}
					else
					{
						// Es un campo opcional (userprofilevalues)
						xpath = new StringBuilder("./o/i[@n=\"").append(fieldname).append("\"]/v").toString(); 
					}
							
					final List<Node> values = user.selectNodes(xpath);
							
					// Le falta un campo obligatorio al usuario, no seguimos.
					if (required.equals("true") && (null == values || values.size() == 0))
					{	
						errorCode   = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY;
						errorDetail = IterErrorKeys.XYZ_MISSING_ZYX + ": " + fieldname;
						
						if (_log.isDebugEnabled())
							_log.debug(XmlioKeys.PREFIX_USER_LOG + "mandatory field is missing: " + fieldname); 
					}
					else if (null == values || values.size() == 0)
					{	
						if (structured)
							iterUserValues.append("null,");									
					}
					else
					{
						final int totalValues =  values.size();	// Viene un valor o n valores
						
						// Validamos que los campos de iterusers no superan los límites de la base de datos. Solo viene un valor
						if (structured && totalValues > 0)
							checkIterUserFieldLength(fieldname, values.get(0).getText().trim());
							
						DLFileEntry dlFileentry = null;
						
						for (int v = 0; errorCode == null && v < totalValues; v++)
						{
							// Object porque puede ser un long o un string
							Object value = values.get(v).getText().trim();
							
							// El dato es un binario. No hay que buscar a ver si ya existe, siempre se dan de alta.								
							if (fieldType.equalsIgnoreCase("binary"))
							{
								Object[] resultNewDlFileEntry = createDLFileEntry(workingDirectory, value, fieldname, groupId, defaultUserId, url);
								dlFileentry = (null == resultNewDlFileEntry[0] ? null  : (DLFileEntry) resultNewDlFileEntry[0]);
								value       = (null == resultNewDlFileEntry[1] ? value : ((Object) resultNewDlFileEntry[1]));
								errorCode   = (null == resultNewDlFileEntry[2] ? null  : (String) resultNewDlFileEntry[2]);
								errorDetail = (null == resultNewDlFileEntry[3] ? null  : (String) resultNewDlFileEntry[3]); 
							}
								
							// Solo si ha ido bien
							if(null == errorCode)
							{											
								// Es un campo fijo (iterusers)
								if (structured)
								{
									// Si el campo es la contraseña y esta en texto plano la ciframos en md5
									if ("pwd".equals(fieldname) && !passwordInMD5)
										iterUserValues.append((null == dlFileentry ? "'" + IterUserTools.encryptPwd( StringEscapeUtils.escapeSql(value.toString().trim()) ) + "'" : dlFileentry.getFileEntryId())).append(",");
									else
										iterUserValues.append((null == dlFileentry ? "'" + StringEscapeUtils.escapeSql(value.toString().trim()) 							+ "'" : dlFileentry.getFileEntryId())).append(",");
								}
								else
								{
									// Es un campo opcional (userprofilevalues)
									if (Validator.isNotNull(value))
									{
										userProfileValues.append("('" + PortalUUIDUtil.newUUID() + "', " + "'" + userId + "', " + "'" + userProfileId + "', ");
										
										if (dlFileentry != null)
											userProfileValues.append("null, ").append(dlFileentry.getFileEntryId());
										else
											userProfileValues.append("'" + IterUserTools.encryptGDPR(StringEscapeUtils.escapeSql(value.toString().trim())) + "', null");
										
										userProfileValues.append("),");
									}
									else if (required.equals("true"))
									{	
										errorCode   = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY;
										errorDetail = IterErrorKeys.XYZ_MISSING_ZYX + ": " + fieldname;
										if (_log.isDebugEnabled())
											_log.debug(XmlioKeys.PREFIX_USER_LOG + "mandatory field is missing: " + fieldname);
									}
								}											
							}										
						}
					}								
				}

				// Si el usuario tiene los campos necesarios y no esta repetido
				if (null == errorCode)
				{
					// Insertamos el usuario (iterusers)
					String sql = new StringBuilder(String.format(INSERT_ITERUSERS, 
								   iterUserFields.substring(0, iterUserFields.length()-1).toString() ))
								   .append(StringPool.OPEN_PARENTHESIS)
								   .append(new StringBuilder("'").append(userId).append("'").append(", "))
								   .append(iterUserValues.substring(0, iterUserValues.length()-1))
								   .append(GET_DELEGATION_ID).append(groupId).append(StringPool.CLOSE_PARENTHESIS)
								   .append(StringPool.CLOSE_PARENTHESIS).toString();
					
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Insert query iterusers:\n").append(sql).toString());		
					long i0 = Calendar.getInstance().getTimeInMillis();							
					
					ErrorRaiser.throwIfFalse( (!sql.contains("{") && !sql.contains("}")) , XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL , IterErrorKeys.XYZ_SQL_CONTAINS_BRACES_ZYX);
					
					PortalLocalServiceUtil.executeUpdateQuery(sql);
				
					// Insertamos sus userprofilevalues		
					if (Validator.isNotNull(userProfileValues) && userProfileValues.length() > 0)
					{
						sql = String.format(INSERT_USERPROFILEVALUES, 
								userProfileValues.toString().endsWith(StringPool.COMMA) ? userProfileValues.substring(0, userProfileValues.length() - 1) : userProfileValues);
						if(_log.isDebugEnabled())
							_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Insert query userprofilevalues:\n").append(sql).toString());
						
						ErrorRaiser.throwIfFalse( (!sql.contains("{") && !sql.contains("}")) , XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL , IterErrorKeys.XYZ_SQL_CONTAINS_BRACES_ZYX);
						
						PortalLocalServiceUtil.executeUpdateQuery(sql);
						
						userInsertsTime += Calendar.getInstance().getTimeInMillis()-i0;
					}
					else if(_log.isDebugEnabled())
								_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("No userProfileValues to insert"));
					
					// Borramos los dlfileentries que tenia el usuario (hay que hacerlo al final porque no se puede hacer rollback de esto)
					if (deleteUsers)
					{
						if (Validator.isNotNull(dlFileEntryIdAvatar))
						{
							// Borramos el avatar del usuario
							DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(dlFileEntryIdAvatar);
							
							if(_log.isDebugEnabled())
								_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Avatar from '").append(userName).append("' deleted"));
						}
						
						if (Validator.isNotNull(userDlFileEntries) && userDlFileEntries.size() > 0)
						{
							// Borramos el resto de dlfileentries del usuario
							for (int dl = 0; dl < userDlFileEntries.size(); dl++)
							{
								final long id = ((DLFileEntry)userDlFileEntries.get(dl)).getFileEntryId();
								DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(userDlFileEntries.get(dl));
								
								if(_log.isDebugEnabled())
									_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("User dlfileentry: ").append(id).append(" deleted"));
							}								
						}
					}						
				}	
			}		
		}
		catch(Exception e)
		{
			errorCodeAndErrorDetail = getErrorCodeAndErrorDetails(e, userName, userEmail);
			errorCode   = errorCodeAndErrorDetail[0];
			errorDetail = errorCodeAndErrorDetail[1];
			throw e;
		}
		finally
		{
			if(_log.isDebugEnabled())
			{
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Elapsed time creating dlfileentries: ").append(dlFileEntryTime).append(" ms"));
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Elapsed time in users imports: ")      .append(userInsertsTime).append(" ms"));
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Total elapsed time importing user/s: ").append((Calendar.getInstance().getTimeInMillis()-t0)).append(" ms"));
			}
			
			createResponse(xmlResult, errorCode, errorDetail, userName, userEmail, t0);
		}		
	}
	
	/* [0] = dlfileentry
	   [1] = value
	   [2] = errorCode
	   [3] = errorDetail */
	private Object[] createDLFileEntry(File workingDirectory, Object value, String fieldName, String groupId, long defaultUserId, String url) throws ServiceError, IOException{
		_log.trace(XmlioKeys.PREFIX_USER_LOG + "Creating a new DLFileEntry");
		
		Object[] result = new Object[4];
		DLFileEntry dlFileentry = null;	
		String changedValue  	= null;
		String errorCode        = null;
		String errorDetail      = null;
		
		// Obtenemos el binario
		final File file = new File(workingDirectory + File.separator + value);	
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("File: ").append(file.getAbsolutePath()));
	
		if (null != file && file.exists() && file.isFile() && file.canRead()){
			// FileInputStream no soporta la funcion reset(), daría una excepcion en addDLFileEntry así que utilizamos la clase BufferedInputStream
			FileInputStream fIS = null;
				
			try{
				fIS = new FileInputStream(file);												
				byte fileContent[] = new byte[(int)file.length()];												
				fIS.read(fileContent);	
				InputStream is = new ByteArrayInputStream(fileContent);												
				
				// Pasamos el binario a dlfileentry. Este proceso es muy lento, se lleva más del 90% del tiempo de la importación
				long t0 = Calendar.getInstance().getTimeInMillis();
															
				dlFileentry = DLFileEntryMgrLocalServiceUtil.addDLFileEntry(Long.parseLong(groupId), 
															                defaultUserId,
															                IterKeys.FORMS_ATTACHMENTS_FOLDER, 
															                StringEscapeUtils.escapeSql(value.toString()),  
															                is);
				if(_log.isDebugEnabled())
					_log.debug(XmlioKeys.PREFIX_USER_LOG + "dlfileentry created with id: " + dlFileentry.getFileEntryId() + " ind " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");
				
			}catch(Exception e){
				errorCode   = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
				errorDetail = IterErrorKeys.XYZ_CREATING_DLFILEENTRY_ZYX + ": " + file.getAbsolutePath();
				_log.error(XmlioKeys.PREFIX_USER_LOG + "Error creating the dlfileentry: "+ file.getAbsolutePath());
				_log.error(e);
			}finally{
				if (null != fIS){
					fIS.close();
				}
			}													
		}else{	
			errorCode   = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
			errorDetail = IterErrorKeys.XYZ_IMPOSSIBLE_TO_READ_ZYX + ": " + file.getAbsolutePath();
			_log.error(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("impossible to read: ").append(file.getAbsolutePath()));
		}		
		
		// Si no se ha creado, error.
		if (null == dlFileentry && null == errorCode)
		{
			errorCode   = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
			errorDetail = IterErrorKeys.XYZ_FILEENTRY_NOT_CREATED_ZYX + ": " + file.getAbsolutePath();
			_log.error(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("dlFileEntry not created: ").append(file.getAbsolutePath()));
			
		}
		else if (null != dlFileentry && fieldName.equals(UserUtil.PRF_FIELD_AVATARURL))
		{		
			// Si el campo es el avatar, se guarda la url de la imagen en lugar de apuntar el dlfilentry.id
			changedValue = getUrlFromDLfileentry(url, groupId, dlFileentry.getFolderId(), dlFileentry.getTitle());
		}
		
		result[0] = dlFileentry;
		result[1] = changedValue;
		result[2] = errorCode;
		result[3] = errorDetail;
		
		return result;
	}
	
	private String[] hasUsernameAndEmailAndPassword(String userName, String userEmail, String password){
		String[] errorCodeAndErrorDetail = null;
		
		if (Validator.isNull(userName)){
			errorCodeAndErrorDetail = new String[2];
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY;
			errorCodeAndErrorDetail[1] = "XYZ_MISSING_NAME_ZYX";
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("User without mandatory field (name)"));
									
		}else if (Validator.isNull(userEmail)){	
			errorCodeAndErrorDetail = new String[2];
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY;
			errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_MISSING_EMAIL_ZYX;
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("User without mandatory field (email)"));
			
		}else if(Validator.isNull(password)){		
			errorCodeAndErrorDetail = new String[2];
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_MANDATORY;
			errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_MISSING_PASSWORD_ZYX;
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("User without mandatory field (password)"));
		}
		return errorCodeAndErrorDetail;
	}
	
	private void createResponse(Document xmlResult, String errorCode, String errorDetail, String userName, String userEmail, long t0) throws ServiceError, DocumentException{
		Element nodeRoot = xmlResult.getRootElement();			
		nodeRoot.addAttribute("id",         SQLQueries.getUUID());
		nodeRoot.addAttribute("starttime",  SDF_TO_DB.format(t0));
		nodeRoot.addAttribute("finishtime", SDF_TO_DB.format(Calendar.getInstance().getTime()));
		nodeRoot.addAttribute("errorCode",  Validator.isNull(errorCode) ? "" : errorCode);
		
		// Descripción del error
		Element errorDetailElement = nodeRoot.addElement("errordetail");
		errorDetailElement.addCDATA(Validator.isNull(errorDetail) ? "" : errorDetail);		
		
		Document userDetail    = SAXReaderUtil.read("<u/>");
		Element userDetailRoot = userDetail.getRootElement();
		userDetailRoot.addAttribute("name",  Validator.isNull(userName)  ? "" : userName );
		userDetailRoot.addAttribute("email", Validator.isNull(userEmail) ? "" : userEmail);		
		nodeRoot.addElement("subject").addCDATA(userDetail.asXML());			
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("User response returned by callback: \n").append(xmlResult));
	}
	
	// Da un valor al código de error y descripción del error en función de la excepción 
	private String[] getErrorCodeAndErrorDetails(Exception e, String userName, String userEmail){
		
		final String errorClass = e.getClass().getName().toLowerCase();		
		String[] errorCodeAndErrorDetail = new String[2];
		
		// Excepciones por restricciones en mysql, como foreign key o unique key.
		if (errorClass.equals(ORMException.class.getName().toLowerCase())){			
			final String identificationError = ServiceErrorUtil.getErrorCode((ORMException)e);
			
			// Vemos si el error se ha dado por duplicar el username, email o aboid para personalizar el mensaje
			if (identificationError.equals("XYZ_ITR_UNQ_USER_NAME_ZYX")){
				errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED;
				errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_USERNAME_REPEATED_ZYX + " " + (Validator.isNull(userName) ? "" : ": '" + userName + "'");
			}else if (identificationError.equals("XYZ_ITR_UNQ_EMAIL_ZYX")){
				errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED;
				errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_EMAIL_REPEATED_ZYX + " " + (Validator.isNull(userEmail) ? "" : ": '" + userEmail + "'");
			}else if (identificationError.equals("XYZ_ITR_UNQ_ABO_ID_ZYX")){
				errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_REPEATED;
				errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_ABOID_REPEATED_ZYX;
			}else{
				errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL;
				errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_ORM_EXCEPTION_ZYX + " " + e.getMessage() + " " + ExceptionUtils.getStackTrace(e);
			}							
			_log.error(XmlioKeys.PREFIX_USER_LOG + " ORMException:\n" + ExceptionUtils.getStackTrace(e));
			
		}else if (errorClass.equals((NumberFormatException.class.getName().toLowerCase()))){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_NUMBER_FORMAT;
			errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_NUMBER_FORMAT_ZYX + " " + e.getMessage() + " " + ExceptionUtils.getStackTrace(e);							
			_log.error(XmlioKeys.PREFIX_USER_LOG + " Incorrect number value:\n" + ExceptionUtils.getStackTrace(e));	
			
		}else if (errorClass.equals(FileNotFoundException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
			errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_FILE_NOT_FOUND_ZYX + " : " + e.getMessage() + " " + ExceptionUtils.getStackTrace(e);			
			_log.error(XmlioKeys.PREFIX_USER_LOG + "file not found:\n" + ExceptionUtils.getStackTrace(e));
			
	    }else if (errorClass.equals(IOException.class.getName().toLowerCase())){	    	
	    	errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_BINARY;
	    	errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_INPUT_OUTPUT_ZYX + ": " + e.getMessage() + " " + ExceptionUtils.getStackTrace(e);
	    	_log.error(XmlioKeys.PREFIX_USER_LOG + "Input/output exception:\n" + ExceptionUtils.getStackTrace(e));	
			
		}else if (errorClass.equals(SQLException.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_SQL;
			errorCodeAndErrorDetail[1] = IterErrorKeys.XYZ_SQL_ZYX + ": " + e.getMessage() + " " + ExceptionUtils.getStackTrace(e);			
			_log.error(XmlioKeys.PREFIX_USER_LOG + "SqInput/output exception:\n" + ExceptionUtils.getStackTrace(e));	
			
		}else if (errorClass.equals(ServiceError.class.getName().toLowerCase())){		
			errorCodeAndErrorDetail[0] = ((ServiceError)e).getErrorCode();
			errorCodeAndErrorDetail[1] = e.getMessage();			
			_log.error(XmlioKeys.PREFIX_USER_LOG + IterErrorKeys.XYZ_SERVICE_ERROR_ZYX + ":\n" + ExceptionUtils.getStackTrace(e));			
			
		}else if (errorClass.equals(Exception.class.getName().toLowerCase())){
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED;
			errorCodeAndErrorDetail[1] = e.getMessage() + " " + ExceptionUtils.getStackTrace(e);	
			_log.error(XmlioKeys.PREFIX_USER_LOG + "Exception:\n" + ExceptionUtils.getStackTrace(e));			
		}else{
			errorCodeAndErrorDetail[0] = XmlioKeys.DETAIL_ERROR_CODE_IMPORT_UNEXPECTED;
			errorCodeAndErrorDetail[1] = e.getMessage() + " " + ExceptionUtils.getStackTrace(e);	
			_log.error(XmlioKeys.PREFIX_USER_LOG + "Exception:\n" + ExceptionUtils.getStackTrace(e));
		}
		return errorCodeAndErrorDetail;
	}

	// Obtiene el id del dlfieldentry del avatar del usuario.
	// La función getFileEntryByTitle devuelve una excepción si no ha encontrado el dlfileentry
	private DLFileEntry getDlFileEntryAvatar(String userName) throws SecurityException, NoSuchMethodException, PortalException, SystemException{
		_log.trace(XmlioKeys.PREFIX_USER_LOG + "In getDlFileEntryeIdAvatarFromUser");
		DLFileEntry dlFileEntry = null;
		
		try
		{
			String name 	= StringEscapeUtils.escapeSql(userName);
			String encName  = IterUserTools.encryptGDPR(name);
			final String sql = String.format(GET_USER_AVATAR, name, encName);
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Query to get the dlfileentryid of user avatar: \n").append(sql));
			
			Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);	
			
			if (Validator.isNotNull(doc))
			{
				String avatarUrl = XMLHelper.getTextValueOf(doc.getRootElement(), "/rs/row/@avatarurl");
				
				if (Validator.isNotNull(avatarUrl))
				{
					// Tenemos algo tal que así: http://127.0.0.1:18080/c/portal/json_service/documents/10810/884710/fb8c2e82-cc2b-4c08-a674-25af82bc6c5b.jpg
					String[] aux = avatarUrl.split("/");
					dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(Long.parseLong(aux[aux.length -3]), 
																				  Long.parseLong(aux[aux.length -2]), 
																				  aux[aux.length -1]);			
				}
			}
		}
		catch(Exception e)
		{
			_log.debug("The user '" + userName + "' has no avatar");
		}
		
		return dlFileEntry;
	}

	// Obtiene los dlfileentries de un usuario salvo el de su avatar (consulta sobre la tabla userprofilevalues)
	// La función getDLFileEntry devuelve una excepción si no ha encontrado el dlfileentry
	private ArrayList<DLFileEntry> getUserDlFileEntries(String userName) throws SecurityException, NoSuchMethodException, PortalException, SystemException{
		_log.trace(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("In getUserDlFileEntries"));
		
		ArrayList<DLFileEntry> dLFileEntries = null;
		
		try
		{
			String name = StringEscapeUtils.escapeSql(userName);
			String encName = IterUserTools.encryptGDPR(name);
			final String sql = String.format(GET_FILEENTRIES_ID_FROM_USER, name, encName);
			_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("Query to get dlfileentries from user: '").append(userName).append("':\n").append(sql));
			Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			if (Validator.isNotNull(doc))
			{
				dLFileEntries = new ArrayList<DLFileEntry>();
				List<Node> nodes = doc.getRootElement().selectNodes("/rs/row");
				
				for (int n = 0; n < nodes.size(); n++)
				{				
					DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(XMLHelper.getLongValueOf(nodes.get(n), "@binfieldvalueid"));
					if (Validator.isNotNull(dlFileEntry))
						dLFileEntries.add(dlFileEntry);
				}
			}
		}
		catch(Exception e)
		{
			_log.debug(new StringBuilder("User '").append(userName).append("' has no dLFileEntries"));
		}
		
		return dLFileEntries;
	}
	
	// Compone la url de un dlfilenetry
	private String getUrlFromDLfileentry(String url, String groupId, Long folderId, String dlFileEntryTitle) throws ServiceError {
		_log.trace(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append("In getUrlFromDLfileentry"));
		
		ErrorRaiser.throwIfNull(url,     		  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "url is null"             );
		ErrorRaiser.throwIfNull(groupId,          IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null"         );
		ErrorRaiser.throwIfNull(folderId,         IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "folderId is null"        );
		ErrorRaiser.throwIfNull(dlFileEntryTitle, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "dlFileEntryTitle is null");		
		
		// Componemos la url del binario
		return new StringBuilder()
			.append(url.toString()).append(StringPool.SLASH)
			.append("documents")   .append(StringPool.SLASH)			
			.append(groupId)	   .append(StringPool.SLASH)			
			.append(folderId)	   .append(StringPool.SLASH)			
			.append(dlFileEntryTitle).toString();	
	}
	
	
	// Comprueba si hay campos que no están en el formulario de registro para la importación de usuarios
	private void checkDataNotInTheRegistrationForm(List<String> userprofileNames, Node user) throws ServiceError
	{
		_log.trace("In checkDataNotInTheRegistrationForm");
		
		ErrorRaiser.throwIfFalse(null != userprofileNames && userprofileNames.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No user profiles given");
		ErrorRaiser.throwIfFalse(null != user, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No user data given");
		
		StringBuilder notInRegisterForm = new StringBuilder();
		
		// Comprobamos los campos fijos
		List<Node> mandatoryNodes = user.selectNodes("f/*");
		
		if (null != mandatoryNodes && mandatoryNodes.size() > 0)
		{			
			if(_log.isDebugEnabled())
				_log.debug("Checking mandatory fields");
			
			for (int n = 0; n < mandatoryNodes.size(); n++)
			{
				String nodeName = ((Element)(mandatoryNodes.get(n))).getName();
				if (!FIELDS_IN_F_NODE.contains(nodeName))
				{
					if (notInRegisterForm.length() > 0)
					{
						notInRegisterForm.append(", ");
					}
					notInRegisterForm.append("'").append(nodeName).append("'");			
				}
			}			
		}		
		
		// Comprobamos los campos opcionales
		List<Node> optionalNodes = user.selectNodes("o/i");
		
		if (null != optionalNodes && optionalNodes.size() > 0)
		{
			if(_log.isDebugEnabled())
				_log.debug("Checking optional fields");
			String[] optionalValues = XMLHelper.getStringValues(optionalNodes, "@n");
			
			if (Validator.isNotNull(optionalValues))
			{	
				int size = optionalValues.length;
				for (int v = 0; v < size; v++)
				{
					if (!userprofileNames.contains(optionalValues[v]))
					{
						if (notInRegisterForm.length() > 0)
						{
							notInRegisterForm.append(", ");
						}
						notInRegisterForm.append("'").append(optionalValues[v]).append("'");												
					}
				}
			}
		}
				
		ErrorRaiser.throwIfFalse(notInRegisterForm.length() == 0, XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT, 
                new StringBuilder(IterErrorKeys.XYZ_FIELD_NOT_IN_REGISTER_FORM_ZYX).append(": ").append(notInRegisterForm).toString() );
	}
	
	public Document GetPasswordSuperUser(String user) throws SecurityException, NoSuchMethodException, ServiceError
	{
		Document doc = null;
	    final String sql = String.format(GET_PASSWORD_SUPERUSER, user);
	    _log.debug(new StringBuilder("Query: ").append(sql));
	             
	    doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
	    ErrorRaiser.throwIfFalse( (doc!=null && doc.selectNodes("//row").size()==1) , IterErrorKeys.XYZ_NO_SUPERUSER_ZYX);
	             
	    return doc;
	}
	
	public static final String PASSWORDS_ENCRYPTION_ALGORITHM = GetterUtil.getString(com.liferay.portal.kernel.util.PropsUtil.get(
				PropsKeys.PASSWORDS_ENCRYPTION_ALGORITHM)).toUpperCase();
	       
	public String SetPasswordSuperUser(String superuser, String user, String Pss, boolean changePsw) throws ServiceError, IOException, SQLException, PortalException, SystemException, SecurityException, NoSuchMethodException 
    {    
          String encodePass = Pss;
          if(!(PASSWORDS_ENCRYPTION_ALGORITHM.equalsIgnoreCase("NONE")))
                 encodePass = GetEncodedPass(Pss);
                 
        ErrorRaiser.throwIfNull(superuser, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "SuperUser is null");
        ErrorRaiser.throwIfNull(user, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "UserName is null");
        ErrorRaiser.throwIfNull(Pss, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Password is null");
        
        final String sqluser = String.format(GET_SUPERUSER, superuser);
        _log.debug(new StringBuilder("Query: ").append(sqluser));
        Document doc = PortalLocalServiceUtil.executeQueryAsDom(sqluser);

          XPath xpath = SAXReaderUtil.createXPath("//row");
          Node node = xpath.selectSingleNode(doc);
        
        int userid = Integer.parseInt(XMLHelper.getTextValueOf(node, "@userId"));
                 
        //Cambio password superuser
        if(changePsw)
           UserLocalServiceUtil.updatePassword(userid, Pss, Pss, false);
                 
        //Cambio del nombre de usuario superuser.
        final String sql = String.format(SET_SCREENNAME_SUPERUSER, user, superuser);
        _log.debug(new StringBuilder("Query: ").append(sql));
                 
         PortalLocalServiceUtil.executeUpdateQuery(sql);       

         return encodePass;
    }
	       
	public String GetEncodedPass(String test) 
	{
	     String pss = DigesterUtil.digest(test);
	     return pss;
	}      
	
	
	/* Comprueba la longitud máxima de los campos de la tabla iterusers. Los campos llegan como XYZ_XXXX_ZYX
	   El nombre de usuario y email se validan fuera de esta función */
	private static void checkIterUserFieldLength(String fieldName, String fieldValue) throws ServiceError
	{
		_log.trace("In checkIterUserFieldLength");		
		
		if (UserUtil.ITERUSERS.containsKey(fieldName))
		{
			ErrorRaiser.throwIfFalse(fieldValue.length() <= UserUtil.ITERUSERS.get(fieldName), XmlioKeys.DETAIL_ERROR_CODE_IMPORT_FORMAT,
					String.format("%s '%s'", com.protecmedia.iter.base.service.util.IterErrorKeys.XYZ_FIELD_TOO_LONG_ZYX, fieldName));
		}
	}	

	/**
	 * Exporta usuarios a un fichero CSV.
	 * 
	 * <p>Exporta los usuarios que cumplan las condiciones incluidas en xmlQueryFiltersAndOrders a un fichero CSV.</p>
	 * 
	 * <p>Recupera todos los campos del formulario de registro del sitio indicado en groupId, aplicando la traducción
	 * que corresponda para el caso de los campos propios de Iter.</p>
	 * 
	 * @param request					es el request del servlet.
	 * @param response					es el response del servlet para incluir el CSV.
	 * @param groupId					es el grupo del sitio para recuperar los campos del formulario de registro.
	 * @param xmlQueryFiltersAndOrders	son los filtros a usar en la consulta en formato XML enviados desde flex.
	 * @param translatedColumns			es la traducción de los campos propios de Iter que manda flex.
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws DocumentException 
	 * @throws IOException 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NumberFormatException 
	 * @throws Throwable 
	 */
    public void exportUserMngToXls(HttpServletRequest request, HttpServletResponse response, String groupId, String xmlQueryFiltersAndOrders, String translatedColumns) throws Throwable
    {
    	if (_log.isDebugEnabled())
			_log.debug("Begin user CSV export...");
    	
    	// Validacion de parametros de entrada
    	ErrorRaiser.throwIfNull(request,           IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "request is null"          );
    	ErrorRaiser.throwIfNull(response,          IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "response is null"         );
    	ErrorRaiser.throwIfNull(groupId,           IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null, 6"       );
    	ErrorRaiser.throwIfNull(translatedColumns, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "translatedColumns is null");
    	long delegationId = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId)).getDelegationId();

        // Recupera los ID (separados por ',') y Nombres (separados por ';') de las Cabeceras
        Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(CSV_EXPORT_GET_HEADER, groupId));
        String colummIds    = XMLHelper.getStringValueOf(dom, "//row/@columnIds");
        _colummNames 		= XMLHelper.getStringValueOf(dom, "//row/@columnNames");
        _structured 		= XMLHelper.getStringValueOf(dom, "//row/@structured");
        
        _translatedColumns 	= translatedColumns;
          
        // Crea los filtros para incorporarlos a la clausula WHERE
        String sqlFilter = getCSVSQLFilter(groupId, xmlQueryFiltersAndOrders);
        
        // Recupera los joins necesarios para filtrar por newsletters
        String newsletterFilter = getNewsletterFilter(xmlQueryFiltersAndOrders);
        
        // Compone el fichero
        buildCSVFile(response, colummIds, sqlFilter, newsletterFilter, delegationId);
        
        if (_log.isDebugEnabled())
			_log.debug("Ends user CSV export.");
    }

	/**
	 * Construye los filtros de la consulta para la recuperacion de usuarios a exportar a CSV.
	 * 
	 * @param  groupId Identificador del grupo del sitio.
	 * @param  xmlQueryFiltersAndOrders XML con los filtros a aplicar.
	 * @return Los filtros SQL a incorporar a la consulta de usuarios.
	 * @throws ServiceError si ocurre un error al construir la operacion en la llamada a 
	 *         {@link #flexOperationToMysqlOperation(int, int) flexOperationToMysqlOperation}
	 * @throws DocumentException si ocurre un error al procesar xmlQueryFiltersAndOrders o al construir la
	 *         operacion en la llamada a {@link #flexOperationToMysqlOperation(int, int) flexOperationToMysqlOperation}
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private String getCSVSQLFilter(String groupId, String xmlQueryFiltersAndOrders) throws ServiceError, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		// Recupera los filtros a aplicar.
		StringBuilder filter = new StringBuilder();
		Document xmlFiltersAndOrder = SAXReaderUtil.read(xmlQueryFiltersAndOrders);
		Element rootNode = xmlFiltersAndOrder.getRootElement();
		List<Node> filters = rootNode.selectNodes("/rs/filters/filter");
		String fieldToFilterWith = null;
		
		for (int i = 0; i < filters.size(); i++)
		{
			// Obtiene el campo sobre el que aplicar el filtro.
			fieldToFilterWith = XMLHelper.getTextValueOf(filters.get(i), "@columnid");
			final boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(filters.get(i), "@structured") );

			if (Validator.isNotNull(fieldToFilterWith) && !"schedulenewsletter".equals(fieldToFilterWith))
			{
				String mysqlOperation = null;
				final String operator = XMLHelper.getTextValueOf(filters.get(i), ("@operator"));
				String columName = "";
				
				if (structured)
				{
					// Si es un campo propio de Iter, comprueba si debe englobar el valor en un IFNULL para que se recuperen
					// los valores nulos. Esto sólo es necesario en los comparadores notcontain y distinc, ya que aumenta el
					// coste de la consulta.
					columName = ("notcontain".equalsIgnoreCase(operator) || "distinct".equalsIgnoreCase(operator)) ? 
									String.format("IFNULL(%s, '') ", fieldToFilterWith) : fieldToFilterWith;
					
					mysqlOperation = flexOperationToMysqlOperation(columName, filters.get(i).asXML());
				}
				else if(isIUDateColumn(fieldToFilterWith))
				{
					mysqlOperation = getDateFilterOperation(fieldToFilterWith, filters.get(i).asXML());
				}
				else if (!"schedulenewsletter".equals(fieldToFilterWith))
				{
					// Si es un campo de formulario, hay que tener en cuenta que para el filtro notcontain, por rendimiento,
					// es mejor aplicar 'like' a 'not like', por lo que se sustituye tanto el filtro en si como el operador
					// binario a aplicar.
					// Además, tanto notcontain como distinct requieren envolver el valor en IFNULL para que recupere nulos.
					String booleanOperator, queryTemplate;
					
					if ("notcontain".equalsIgnoreCase(operator) || "distinct".equalsIgnoreCase(operator))
					{
						booleanOperator = " = ";
						queryTemplate   = OPTIONAL_FILTER_WITH_IFNULL;
						columName	 	= " IFNULL(userprofilevalues1.fieldvalue, '') ";
					}
					else
					{
						booleanOperator = " < ";
						queryTemplate   = OPTIONAL_FILTER;
						columName	 	= " userprofilevalues1.fieldvalue ";
					}
					
					mysqlOperation = flexOperationToMysqlOperation(columName, filters.get(i).asXML());
					mysqlOperation = String.format(queryTemplate,  booleanOperator, groupId, fieldToFilterWith, mysqlOperation);
					if ("notcontain".equalsIgnoreCase(operator) || operator.equals("distinct"))
					{
						mysqlOperation = mysqlOperation.replaceAll("not like", "like");
						mysqlOperation = mysqlOperation.replaceAll("!=", "=");
						mysqlOperation = mysqlOperation.replaceAll(" NOT IN ", " IN ");
					}
				}

				filter.append(" AND ").append(mysqlOperation);
			}
		}
		
		// Si hay filtros que aplicar, elimina el primer " AND " y termina de construir el filtro.
		if (filter.length() > 0)
		{
			filter.delete(0, 5);
			filter.insert(0, "AND u.usrid IN ( SELECT iterusers.usrid FROM iterusers WHERE ").append(" )");
		}
		
		String result = filter.toString();
		_log.debug(result);
		
		return result;
	}
	
	/**
	 * Construye la fila de cabecera del CSV, a partir de las que se recuperan del formulario y realizando
	 * las traducciones sobre los campos propios de Iter y los campos de fecha que se reciben desde flex.
	 *  
	 * hacen uso de los siguientes atributos: 
	 *     - _translatedColumns "URL encoded" con las cabeceras que requieren traduccion en formato
	 *       NOMBRE_ORIGINAL_1=nombre traducido 1;...;NOMBRE_ORIGINAL_N=nombre traducido N
	 *     - _colummsNames son las cabeceras originales separadas por ';'
	 * @return Las cabeceras traducidas separadas por ';';
	 */
	private String getCSVHeader()
	{
		// Decodifica la cadena con las traducciones.
	    try
	    {
			_translatedColumns = java.net.URLDecoder.decode(_translatedColumns, IterKeys.UTF8);
		}
	    catch (UnsupportedEncodingException e)
	    {
			_log.error("Unable to recover headers translations.");
			_log.debug(e);
			return _colummNames;
		}
	    // Construye el mapa de traducciones.
	    String[] aux = Validator.isNotNull(_translatedColumns) ? _translatedColumns.split(";") : new String[]{};
	    HashMap<String, String> traducedColumnsNames = new HashMap<String, String>();
	    for (int i = 0; i < aux.length; i++)
	    {
	    	traducedColumnsNames.put(aux[i].split("=")[0], aux[i].split("=")[1]);
	    }
	    
	    // Construye la fila de cabeceras realizando las traducciones necesarias.
	    String[] columnName = _colummNames.split("§");
	    String[] structured = _structured.split(",");
	    
	    StringBuilder translatedHeader = new StringBuilder();
	    
	    for (int i=0; i<columnName.length; i++)
	    {
	    	String unquotedHead = columnName[i].substring(1, columnName[i].length() - 1);
	    	
	    	if ( GetterUtil.getBoolean(structured[i]) )
	    	{
	    		translatedHeader.append( getColumnName(traducedColumnsNames, unquotedHead) ).append(";");
	        }
	    	else
	        {
	    		translatedHeader.append(columnName[i]).append(";");		
	        }		
	    }
	    //se añaden, al final, las columnas de fecha
	    for(int j=0; j<_columnsDateNames.length; j++)
		{
	    	String unquotedHead = _columnsDateNames[j];
			translatedHeader.append( getColumnName(traducedColumnsNames, unquotedHead) ).append(";");
		}
	    
	    translatedHeader.setLength(translatedHeader.length() - 1);
	    
	    return translatedHeader.append("\r\n").toString();
	}
	
	private String getColumnName(HashMap<String, String> traducedColumns, String name)
	{
		return traducedColumns.containsKey(name) ? traducedColumns.get(name) : name; 
	}
	
	/**
	 * 
	 * @param response
	 * @param colummIds son los identificadores de las columnas en orden para la recuperaicon de los registros.
	 * @param sqlFilter es el filtro a aplicar en la consulta de usuarios.
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 * @throws Throwable 
	 */
	private void buildCSVFile(HttpServletResponse response, String colummIds, String sqlFilter, String newsletterFilter, long delegationId) throws Throwable
	{
        _response = response;
        try
        {
        	Document dom = PortalLocalServiceUtil.executeQueryAsDom(CSV_EXPORT_GET_FIELD_ORIGIN);
        	String whenClausule = XMLHelper.getStringValueOf(dom, "/rs/row/@mapping");
        	String query = String.format(CSV_EXPORT_GET_ROWS, whenClausule, colummIds, sqlFilter, delegationId, newsletterFilter);
        	_log.debug(query);
        	
        	PortalLocalServiceUtil.executeQueryAsResultSet(query, this, "initCSVFile", "writeCSVFile", "endCSVFile");
        }
		catch (Throwable th)
		{
			_log.error("An error occurs while retrieving the csv row", th);
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/html");
			
			ServletOutputStream out = response.getOutputStream();
			out.write( StringPool.PERIOD.getBytes() );
			out.flush();

			throw th;
		}
        finally
        {
	        ServletOutputStream csvout = _response.getOutputStream();
    		if (csvout != null)
    			csvout.close();	
        }
	}
	
	/**
	 * Inicializa el fichero CSV incluyendo la fila de cabecera.
	 */
	public void initCSVFile() throws IOException
	{
		String csvHeader = getCSVHeader();
		
		// Inicializacion del fichero
		ServletOutputStream csvout = _response.getOutputStream();
		csvout.write( BOM_UTF8 );
		
		// Incluye la fila de cabecera.
		csvout.write( csvHeader.getBytes() );
	}
	
	/**
	 * Recupera la fila del ResultSet y la incluye en el fichero CSV.
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public void writeCSVFile(IterRS resultset) throws IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		ServletOutputStream csvout = _response.getOutputStream();
		
		// ITER-843	Al descargar los usuarios, el fichero .csv no posee el total de los registros
		// Si la columna contiene el valor ";" la división falla porque se está tomando el ; del valor como separador
		// Puede que la opción sea, si vienen columnas del tipo "v1";"v2";"v3", separar por \";\"
		if (PropsValues.ITER_GDRP_ENCRYPT_USRDATA)
		{
			// El split(";", -1) es para que devuelva resultado de cadenas vacías ("w;w;;" => [w, w, , ])
			String[] columns  = ((String) resultset.getObject(1)).split(ROWS_SEPARATOR, -1);
			StringBuilder row = new StringBuilder();
			for (String col : columns)
			{
				row.append( IterUserTools.decryptGDPR_Quoted(col) ).append(";");
			}
			// Se borra el último ";"
			row.deleteCharAt(row.length()-1);
			
			csvout.write( row.append("\r\n").toString().getBytes() );
		}
		else
		{
			String row = (String) resultset.getObject(1);
			row = row.replaceAll(ROWS_SEPARATOR, ";").concat("\r\n");
			csvout.write( row.getBytes() );
		}
	}
	
	/**
	 * Vuelca el fichero CSV al response.
	 */
	public void endCSVFile() throws IOException
	{
		// Cabecera del response
		_response.setHeader("Content-Type", "text/csv");
	    _response.setHeader("Content-Disposition", "attachment;filename=\"data.csv\"");
	    _response.setContentType("application/csv");

		ServletOutputStream csvout = _response.getOutputStream();
		csvout = _response.getOutputStream();
		csvout.flush();
	}

	private static final String SQL_GET_USER_NEWSLETTER_SUBSCRIPTIONS = new StringBuilder()
	.append("SELECT sn.scheduleid, n.name newsletter, sn.name schedule, su.enabled \n")
	.append("FROM       schedule_user su                                           \n")
	.append("INNER JOIN schedule_newsletter sn ON sn.scheduleid = su.scheduleid    \n")
	.append("INNER JOIN newsletter n           ON n.newsletterid = sn.newsletterid \n")
	.append("                                 AND groupId = %d                     \n")
	.append("WHERE su.userid = '%s'                                                \n")
	.toString();
	
	private void getUserNewsletters(long groupId, String userId, Document user) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(userId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(user, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Node n = user.selectSingleNode("/rs/row");
		ErrorRaiser.throwIfNull(n, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document d = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_USER_NEWSLETTER_SUBSCRIPTIONS, groupId, userId), true, "newsletters", "subscription");
		((Element) n).add(d.getRootElement());
	}
	
	/**
	 * Crea los JOINS necesarios para filtrar por suscripcion a newsletters para la exportación a CSV.
	 * @param xmlQueryFiltersAndOrders
	 * @return
	 * @throws DocumentException
	 */
	private String getNewsletterFilter(String xmlQueryFiltersAndOrders) throws DocumentException
	{
		Document xmlFiltersAndOrder = SAXReaderUtil.read(xmlQueryFiltersAndOrders);
		return getNewsletterFilter(xmlFiltersAndOrder, true);
	}
	
	private String getNewsletterFilter(Document xmlFiltersAndOrder) throws DocumentException
	{
		return getNewsletterFilter(xmlFiltersAndOrder, false);
	}
	
	private static final String NEWSLETTER_NOT_SUBSCRIBE_FILTER = new StringBuilder(
		"INNER JOIN  (																																\n").append(
		"             -- Usuarios que NO pertenecen a dicha programación																			\n").append(
		"             SELECT users.usrid																											\n").append(
		"             FROM iterusers users																											\n").append(
		"               WHERE users.usrid NOT IN (																									\n").append(
		"                                           -- Usuarios que pertenecen a dicha programación													\n").append(
		"                                           SELECT schedule_user.userid																		\n").append(
		"                                           FROM schedule_user																				\n").append(
		"                                           INNER JOIN schedule_newsletter ON schedule_newsletter.scheduleid = schedule_user.scheduleid		\n").append(
		"                                             WHERE schedule_user.enabled = TRUE															\n").append(
		"                                               AND schedule_newsletter.scheduleid IN (%s)													\n").append(
		"                                         )																									\n").append(
		"            ) Notsubscribed ON Notsubscribed.usrid = %s.usrid																				\n").toString();
	
	private static final String NEWSLETTER_SUBSCRIBE_FILTER = new StringBuilder(
		"INNER JOIN schedule_user        ON schedule_user.userid = %s.usrid AND schedule_user.enabled = TRUE										\n").append(
		"INNER JOIN schedule_newsletter  ON schedule_newsletter.scheduleid = schedule_user.scheduleid  AND schedule_newsletter.scheduleid IN (%s)   \n").toString();		

	
	/**
	 * Crea los JOINS necesarios para filtrar por suscripcion a newsletters.
	 * @param xmlFiltersAndOrder
	 * @return
	 * @throws DocumentException
	 */
	private String getNewsletterFilter(Document xmlFiltersAndOrder, boolean csv) throws DocumentException
	{
		StringBuilder f = new StringBuilder(StringPool.BLANK);
		
		List<Node> suscribed    = xmlFiltersAndOrder.selectNodes("/rs/filters/filter[@columnid='schedulenewsletter' and @operator='subscribed']/values/value");
		List<Node> notSuscribed = xmlFiltersAndOrder.selectNodes("/rs/filters/filter[@columnid='schedulenewsletter' and @operator='notsubscribed']/values/value");
		
		if (suscribed.size() > 0 || notSuscribed.size() > 0)
		{
			if (suscribed.size() > 0)
			{
				String newsletterIds = StringUtil.merge(XMLHelper.getStringValues(suscribed, "text()"), StringPool.COMMA, StringPool.APOSTROPHE);
				String sql = String.format(NEWSLETTER_SUBSCRIBE_FILTER, csv ? "u" : "iterusers", newsletterIds);
				f.append(sql);
			}
			
			if (notSuscribed.size() > 0)
			{
				String newsletterIds = StringUtil.merge(XMLHelper.getStringValues(notSuscribed, "text()"), StringPool.COMMA, StringPool.APOSTROPHE);
				String sql = String.format(NEWSLETTER_NOT_SUBSCRIBE_FILTER, newsletterIds, csv ? "u" : "iterusers");
				f.append(sql);
			}
		}
		
		if (_log.isTraceEnabled())
			_log.trace(f.toString());
		
		return f.toString();
	}
	
	private static final String	SQL_STATEMENT_TO_SUBSCRIBE	= new StringBuilder()
	.append("    SELECT UUID() scheduleuserid, scheduleid,      \n")
	.append("           usrid userid, NOW() modifieddate,       \n")
	.append("           NULL publicationdate, 1 enabled FROM    \n")
	.append("    (                                              \n")
	.append("        SELECT scheduleid FROM schedule_newsletter \n")
	.append("        WHERE scheduleid IN (%s)                   \n") // Newsletters
	.append("    ) A                                            \n")
	.append("    JOIN                                           \n")
	.append("    (                                              \n")
	.append("        %s                                         \n") // Usuarios
	.append("    ) B                                            \n")
	.toString();
	
	private static final String	SQL_SUBSCRIBE_USERS = new StringBuilder()
	.append("INSERT INTO schedule_user (scheduleuserid, scheduleid, userid, modifieddate, publicationdate, enabled) \n")
	.append("(                                                                                                      \n")
	.append("%s                                                                                                     \n")
	.append(")                                                                                                      \n")
	.append("ON DUPLICATE KEY UPDATE modifieddate=NOW(), enabled=1"                                                    )
	.toString();
	
	public void subscribeUsersToNewsletters(long groupId, String scheduleIds, String xmlQueryFilters) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Prepara los Ids de las programaciones a las que suscribir a los usuarios
		String sql_scheduleIds = StringUtil.merge(scheduleIds.split(StringPool.COMMA), StringPool.COMMA, StringPool.APOSTROPHE);
		// Obtiene la query para obtener los ids de los usuarios a suscribir
        String sql_users = getQueryToUsersId(String.valueOf(groupId), xmlQueryFilters);
        sql_users = sql_users.substring(0, sql_users.lastIndexOf("ORDER BY"));
        
		// Crea la consulta para los insert en la schedule_user
        String sql_inserts = String.format(SQL_STATEMENT_TO_SUBSCRIBE, sql_scheduleIds, sql_users);
        
        // La inyecta en la query de inserción
		String sql = String.format(SQL_SUBSCRIBE_USERS, sql_inserts);
		
		// Registra el evento en las métricas.
		// Se hace primero porque tiene en cuenta las suscripciones actuales para no registrar hits de usuarios que ya estaban suscritos.
		NewslettersMetricsUtil.multipleUsersSubscriptionHit(sql_scheduleIds, sql_users);
        
		// Ejecuta la inserción de usuarios
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
}