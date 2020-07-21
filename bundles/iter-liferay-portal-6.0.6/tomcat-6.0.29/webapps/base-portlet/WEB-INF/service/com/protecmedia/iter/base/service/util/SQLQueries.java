package com.protecmedia.iter.base.service.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;


public class SQLQueries {
	
	private static Log _log = LogFactoryUtil.getLog(SQLQueries.class);	
	
	private static final String MYSQL_SHORT_DATE_FORMAT   	= "'%Y-%m-%d'";
	private static final String MYSQL_MEDIUM_DATE_FORMAT 	= "'%Y-%m-%d %H:%i'";
	
	private static final SimpleDateFormat sDFShort  = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT         ); 
	private static final SimpleDateFormat sDFMedium = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT_EXT     );
	
	/*
	 *  Devuelve todas las páginas, menos las que son modelos de artículo o de página. 
	 */
	
	private static final String GET_LAYOUTS 		= new StringBuffer 
			  ("SELECT Layout.uuid_, Layout.layoutId, Layout.friendlyURL, Layout.plid\n").
		append("FROM Layout \n").
		append("WHERE groupId='%d' AND privateLayout='%s' AND parentLayoutId='%d' AND type_<>'%s' \n").
		append("AND Layout.plid NOT IN \n").
		append("\t(SELECT layoutId FROM Designer_PageTemplate WHERE groupId='%d')").toString(); 
	
	
	public static final String SEL_FILEENTRIES_ID 	= new StringBuffer
			  ("-- Se hace un Join con DLFileEntry para descartar los ficheros apuntados en XmlIO_Live y Xmlio_LivePool\n").
		append("-- pero que NO existen en DLFileEntry (Ej. un borrado de una imagen de una operación anterior,que existe\n").
		append("-- en Xmlio_Live hasta que se publique al entorno Live)\n").
		append("SELECT DISTINCT DLFileEntry.fileEntryId\n").
		append("FROM DLFileEntry\n").
		append("INNER JOIN Xmlio_Live        ON (DLFileEntry.fileEntryId = Xmlio_Live.localId AND\n").
		append("                                 Xmlio_Live.classNameValue = '").append(IterKeys.CLASSNAME_DLFILEENTRY).append("')\n").
		append("INNER JOIN Xmlio_LivePool    ON (Xmlio_Live.id_ = Xmlio_LivePool.liveChildId)\n").
		append("INNER JOIN Xmlio_LivePool lp ON (Xmlio_LivePool.livePoolId = lp.livePoolId)\n").
		append("INNER JOIN Xmlio_Live lv     ON (lp.livechildId = lv.id_ AND\n").
		append("                                 lv.classNameValue = '").append(IterKeys.CLASSNAME_JOURNALARTICLE).append("')\n").
		append("    WHERE lv.localId = '%s'").toString();

	
	public static final String SEL_FILEENTRY_BY_GLOBALID = new StringBuffer 
			  ("-- Obtiene las propiedades de un DLFileEntry a partir del globalId y el grupo\n").
		append("SELECT DLFileEntry.groupId, DLFileEntry.folderId, DLFileEntry.name\n").	
		append("FROM DLFileEntry\n").
		append("INNER JOIN Xmlio_Live ON (DLFileEntry.fileEntryId = Xmlio_Live.localId AND Xmlio_Live.classNameValue = '").append(IterKeys.CLASSNAME_DLFILEENTRY).append("')\n").
		append("    WHERE Xmlio_Live.groupId = %d AND globalId = '%s'\n").	
		append("LIMIT 1").toString();
	
	public static final String UPDT_EXIST_IN_LIVE = "UPDATE Xmlio_Live SET existInLive='%s' WHERE id_ IN (%s)";
	
	public static final String DEL_LIVE = "DELETE FROM Xmlio_Live WHERE id_ IN (%s)";
	
	public static final String UPDATE_LEGACYURL = "UPDATE LegacyUrl set url='%s', scopeGroupId=%d WHERE articleid='%s'";
	public static final String INSERT_LEGACYURL = "INSERT INTO LegacyUrl(url, scopeGroupId, articleid) VALUES ";
	public static final String INSERT_LEGACYURL_VALUES = "('%s', %d, '%s')";
	
	public static final String SELECT_MAIN_CATEGORIES = "SELECT data_ FROM ExpandoValue ev LEFT JOIN JournalArticle ja ON ev.classpk=ja.id_  LEFT JOIN ExpandoColumn ec ON ec.columnId=ev.columnId WHERE ja.articleId='%s' AND ec.name='%s'";

	//adfileentry
	public static final String GET_ADFILEENTRY_FILEENTRYUUIDS_BY_DLFILEENTRYUUID = "SELECT fileentryuuid FROM adfileentry WHERE dlfileentryuuid='%s'";
	public static final String ADD_ADFILEENTRY = "INSERT INTO adfileentry (fileentryuuid, dlfileentryuuid, modifieddate, publicationdate) VALUES ('%s', '%s', '%s', null)";
	public static final String UPDATE_ADFILEENTRY = "UPDATE adfileentry SET dlfileentryuuid='%s', modifieddate='%s' WHERE dlfileentryuuid='%s'";
	
	public static final String CHECK_DUPLICATE_FILEENTRY_DESC = "SELECT COUNT(*) FROM DLFileEntry WHERE description='%s' AND groupId=%s AND folderId=%s";
	public static final String CHECK_DUPLICATE_FILEENTRY_DESC_ON_UPDATE = "SELECT COUNT(*) FROM DLFileEntry WHERE uuid_!='%s' AND description='%s'";
	
	//layout
	public static final String GET_LOCAL_ID = "SELECT localId FROM Xmlio_Live WHERE globalId='%s' AND classNameValue='%s'";
	
	//secciones seleccionadas
	public static final String GET_SECTION_SELECTED = new StringBuilder(
		"-- secciones seleccionadas \n"									).append(
		"SELECT uuid_ AS value, \n"										).append(
		"ExtractValue(name, '/root/name[1]/text()') AS name \n"			).append( 
		"FROM Layout\n"													).append(
		"WHERE uuid_ IN ('%s') \n"										).append(
		"ORDER BY name ASC	\n"											).toString();
	
	//secciones hijas(primer nivel) de las seleccionadas		
	public static final String GET_SECTION_DESCENDENTS = new StringBuilder(
		"-- secciones hijas de las seleccionadas \n"											).append(
		"SELECT Layout.uuid_ AS value, \n"														).append(
		"ExtractValue(Layout.NAME, '/root/name[1]/text()') AS name \n"							).append( 
		"FROM Layout\n"																			).append(
		"INNER JOIN Layout ParentLayout ON (Layout.parentLayoutId = ParentLayout.layoutId )\n"	).append(
		"WHERE ParentLayout.uuid_ IN ('%s') \n"													).append(
		"ORDER BY name ASC	\n"																	).toString();
		
	public static final String GET_SECTION_DESCENDENTS_WITH_EXCEPTIONS = new StringBuilder(
		"-- secciones hijas de las seleccionadas que no esten excluidas \n"						).append(
		"SELECT Layout.uuid_ AS value, \n"														).append(
		"ExtractValue(Layout.NAME, '/root/name[1]/text()') AS name \n"							).append( 
		"FROM Layout\n"																			).append(
		"INNER JOIN Layout ParentLayout ON (Layout.parentLayoutId = ParentLayout.layoutId )\n"	).append(
		"WHERE ParentLayout.uuid_ IN ('%s') \n"													).append(
		"AND Layout.uuid_ NOT IN ('%s') \n"														).append(
		"ORDER BY name ASC	\n"																	).toString();
	
	//metadatos seleccionados
	public static final String GET_METADATA_SELECTED = new StringBuilder(
		"-- categorías seleccionadas \n"								).append(
		"SELECT categoryId AS value, \n"								).append(
		"name \n"														).append( 
		"FROM AssetCategory \n"											).append(
		"WHERE categoryId IN ( %s )	 \n"								).append(
		"ORDER BY name ASC	\n"											).toString();
	
	//metadatos hijos de seleccionados.
	//La claúsula where se pone a true para reaprovechar la select de las hojas de los seleccionados
	public static final String GET_METADATA_DESCENDENTS = new StringBuilder(
		"-- Categorías hijas de las seleccionadas 						\n").append(
		"SELECT c.categoryId as value, c.name 							\n").append( 
		"FROM AssetCategory c \n%s										\n").append(
		"	WHERE true	 												\n").append(
		"	%s  														\n").append(
		"GROUP BY c.name												\n").toString();
	
	//metadatos situados en el último nivel de la jerarquía
	public static final String GET_METADATA_LEAFS= new StringBuilder(
		"-- Categorías hojas de las seleccionadas 						\n").append(
		"SELECT c.categoryId as value, c.name 							\n").append(
		"FROM AssetCategory c \n%s										\n").append(
		"	WHERE (c.rightCategoryId - c.leftCategoryId = 1) 			\n").append(
		"	%s  														\n").append(
		"GROUP BY c.name												\n").toString();
	
	
	public static void getPaginas(long idPadre, long idGrupo, List<Object> paginas) throws Exception  {
		List<Object> layoutList = getLayouts(idGrupo, false, idPadre);
	
		for (int i = 0; i < layoutList.size(); i++)
		{
			Object[] _layout = (Object[])layoutList.get(i);
			paginas.add(_layout);
			long layoutId = Long.valueOf( String.valueOf(_layout[1]) ); 
			getPaginas(layoutId, idGrupo, paginas);
		}	
	}

	public static List<Object> getLayouts(long groupId, boolean privateLayout, long parentLayoutId) throws Exception 
	{
		String query = String.format(GET_LAYOUTS, groupId, privateLayout, parentLayoutId, IterKeys.CUSTOM_TYPE_TEMPLATE, groupId );
		return PortalLocalServiceUtil.executeQueryAsList(query);
	}
	
	public static String getLayouts(long groupId) throws Exception 
	{
		String query = String.format(GET_LAYOUTS, groupId, "0", 0, IterKeys.CUSTOM_TYPE_TEMPLATE, groupId);
		return PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
	}
	
	public static String getCurrentDate() throws ServiceError
	{
		return new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format(Calendar.getInstance().getTime());
	}
	
	public static String getUUID()
	{
		return PortalUUIDUtil.newUUID();
	}
	
	public static Element getLiveWellFormedRowAsElement(long scopeGroupId, Node row) throws DocumentException, ServiceError
	{
		return getLiveWellFormedRowAsElement(scopeGroupId, row, "plid", IterKeys.CLASSNAME_LAYOUT, IterErrorKeys.XYZ_E_LAYOUT_NOT_FOUND_IN_LIVE_ZYX);
	}
	
	public static Element getLiveWellFormedRowAsElement(long scopeGroupId, Node row, String attrName, String classnamevalue, String errorNoExistsInLive) throws DocumentException, ServiceError
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		Element eRow = (Element)row.detach();
		//Recuperación id local
		Attribute attrVal = eRow.attribute( attrName );
		if(attrVal != null && Validator.isNotNull(attrVal.getValue()))
		{
			List<Object> localId = PortalLocalServiceUtil.executeQueryAsList(String.format(GET_LOCAL_ID, attrVal.getValue(), classnamevalue));
			ErrorRaiser.throwIfFalse((localId != null && localId.size() == 1 && localId.get(0) != null), errorNoExistsInLive, attrVal.getValue());
			attrVal.setValue(localId.get(0).toString());
		}
		
		eRow.addAttribute("groupid", String.valueOf(scopeGroupId));
		rs.add((Node)eRow);
		
		return rs;
	}
	
	public static String getLiveWellFormedRow(long scopeGroupId, Node row) throws DocumentException, ServiceError
	{
		return getLiveWellFormedRowAsElement(scopeGroupId, row).asXML();
	}
	
//	public static String getLiveWellFormedRow(long scopeGroupId, Node row, String attrName, String classnamevalue, String errorNoExistsInLive) throws DocumentException, ServiceError
//	{
//		return getLiveWellFormedRowAsElement(scopeGroupId, row, attrName, classnamevalue, errorNoExistsInLive).asXML();
//	}
	
	public static void checkDuplicateNameFileEntry(long scopeGroupId, long folderId, String description, String uuid) throws ServiceError, IOException, PortalException, SystemException
	{
		int duplicateCheckCount = 0;
		String query = null;
		
		if(Validator.isNull(uuid))
			query = String.format(SQLQueries.CHECK_DUPLICATE_FILEENTRY_DESC, description, scopeGroupId, folderId);
		else
			query = String.format(SQLQueries.CHECK_DUPLICATE_FILEENTRY_DESC_ON_UPDATE, uuid, description);
		
		List<Object> duplicateCheck = PortalLocalServiceUtil.executeQueryAsList(query);
		
		if(duplicateCheck != null && duplicateCheck.size() > 0)
			duplicateCheckCount = Integer.valueOf(duplicateCheck.get(0).toString());

		ErrorRaiser.throwIfFalse(duplicateCheckCount == 0, IterErrorKeys.XYZ_E_DUPLICATE_IMAGE_NAME_ZYX);
	}
	
	public static String buildFilters(String xmlFilters) throws ServiceError, DocumentException
	{
		return buildFilters(xmlFilters, false);
	}
	
	// Lee un xml con los filtros y los compone para mysql. Si no llegan filtros devuelve una cadena vacía. 
	// isADetail indica si la consulta es para mostrar el detalle de una importacion (true) o no (false)
	public static String buildFilters(String xmlFilters, Boolean isADetail) throws ServiceError, DocumentException
	{
		_log.trace("In buildFilters");
		
		StringBuffer filters = new StringBuffer("");
		
		if (Validator.isNotNull(xmlFilters))
		{				
			// Recorremos los filtros
			final Document documentFilters = SAXReaderUtil.read(xmlFilters);
			final Element rootNode         = documentFilters.getRootElement();
			final List<Node> nodeFilters   = rootNode.selectNodes("/filters/filter");
			
			for (int f = 0; f < nodeFilters.size(); f++)
			{	
				final Node nodeFilter = nodeFilters.get(f);
				
				// Columna por la que filtrar
				final String subject = XMLHelper.getTextValueOf(nodeFilter, "@columnid");	
				ErrorRaiser.throwIfFalse(Validator.isNotNull(subject), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "field name filter is null");
				
				// Tipo del campo (string, boolean, date, number)
				final String filterType = XMLHelper.getTextValueOf(nodeFilter, "@fieldtype");
				ErrorRaiser.throwIfFalse(Validator.isNotNull(filterType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "type filter is null");
				
				// Operación a realizar (equals, distinct, ...)
				final String operator = XMLHelper.getTextValueOf(nodeFilter, "@operator");
				ErrorRaiser.throwIfFalse(Validator.isNotNull(operator), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "operator filter is null");
				
				// Valores recibidos (1-N)
				final String values[] = XMLHelper.getStringValues(nodeFilter.selectNodes("values//value"));
				ErrorRaiser.throwIfFalse(Validator.isNotNull(values), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "values filter is null");
				
				// Componemos el filtro
				filters.append(buildFilter(subject, filterType, operator, values, isADetail,true));				
			}			
		}
		else
			_log.debug("Query without filters");
		
		return filters.toString();
	}
	
	// Construye un filtro/predicado para mysql. Es llamado por buildFilters.
	// isADetail indica si el filtro es para el listado de un detalle (true) o no (false). En función de este parámetro cambian las columnas de la sql.
	// startAndOperator indica si el filtro va a empezar con el operador AND(true) o no. Su valor irá en función de la query final donde se vaya a añadir el filtro 
	public static String buildFilter(String subject, String subjectType, String operator, String[] values, Boolean isADetail, Boolean startAndOperator) throws ServiceError
	{
		_log.trace("In buildFilter");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(subject),     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Subject filter is null");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(subjectType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Type filter is null"   );
		ErrorRaiser.throwIfFalse(Validator.isNotNull(values),      IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Values filter is null" );
		
		final int valuesSize = values.length;	
		String startStr   = startAndOperator?  " AND ("   : " (";
		StringBuffer filter = new StringBuffer(startStr);	

		for (int v = 0; v < valuesSize; v++)
		{
			final String value =  values[v];
			
			if (v > 0)
				filter.append(" AND ");				
		
			if (subjectType.equals("string"))
			{						
				if (operator.equals("equals"))					
					filter.append(subject + " = '"         + StringEscapeUtils.escapeSql(value) + "' " );
				else if (operator.equals("distinct"))					
					filter.append(subject + " != '"        + StringEscapeUtils.escapeSql(value) + "' " );					
				else if (operator.equals("contain"))
				{
					_log.debug("Building a possible very slow query, with filter: \" like '%xyz%' \" no indexes can be used ");
					filter.append(subject + " like '%"     + StringEscapeUtils.escapeSql(value) + "%' ");
				}
				else if (operator.equals("notcontain"))
				{
					_log.debug("Building a possible very slow query, with filter: \" not like '%xyz%' \" no indexes can be used ");
					filter.append(subject + " not like '%" + StringEscapeUtils.escapeSql(value) + "%' ");
				}
				else if (operator.equals("startBy"))					
					filter.append(subject + " like '"      + StringEscapeUtils.escapeSql(value) + "%' ");					
				else if (operator.equals("endBy"))
				{
					_log.debug("Building a possible very slow query, with filter: \" like '%xyz' \" no indexes can be used ");
					filter.append(subject + " like '%"     + StringEscapeUtils.escapeSql(value) + "' " );
				}
				else
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid operation filter to strings");
			}
			else if (subjectType.equals("date"))
			{				
				Date date              = null;
				SimpleDateFormat sDF   = null;
				String mysqlDateFormat = null;
				
				// La fecha puede venir con hora y minuto o sin ello
				try
				{
					date = sDFMedium.parse(value);
					sDF  = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					mysqlDateFormat = MYSQL_MEDIUM_DATE_FORMAT;
				}
				catch(Exception e2)
				{
					try
					{
						date = sDFShort.parse(value);
						sDF  = new SimpleDateFormat("yyyy-MM-dd");
						mysqlDateFormat = MYSQL_SHORT_DATE_FORMAT;
					}
					catch(Exception e3)
					{
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid date format");
					}
				}
				
				// La fecha que nos llega está en formato distinto al de la BBDD (faltan segundos y/o minutos)
				if (operator.equals("equals"))											
					filter.append(" DATE_FORMAT(" + subject + ", " + mysqlDateFormat + ") = '"  + sDF.format(date) + "'");
				else if (operator.equals("distinct"))
					filter.append(" DATE_FORMAT(" + subject + ", " + mysqlDateFormat + ") != '" + sDF.format(date) + "'");
				else if (operator.equals("beforedate"))
					filter.append(" DATE_FORMAT(" + subject + ", " + mysqlDateFormat + ") < '"  + sDF.format(date) + "'");
				else if (operator.equals("afterdate"))
					filter.append(" DATE_FORMAT(" + subject + ", " + mysqlDateFormat + ") > '"  + sDF.format(date) + "'");
				else if (operator.equals("fromdate"))
					filter.append(" DATE_FORMAT(" + subject + ", " + mysqlDateFormat + ") >= '" + sDF.format(date) + "'");
				else if (operator.equals("todate"))
					filter.append(" DATE_FORMAT(" + subject + ", " + mysqlDateFormat + ") <= '" + sDF.format(date) + "'");
				else
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid operation filter to dates");
			}
			else if (subjectType.equals("boolean") || (subjectType.equals("boolean_data"))  )
			{
				// La consulta no es para un detalle
				if(!isADetail)
				{
					/* Nos llega el campo "result", que equivale a las columnas ok o ko y se traduce al sql según la casuística:
					   LLLEGA		SE TRADUCE COMO
					   result  = 0	ko > 0
					   result != 1	ko > 0									
				       result  = 1	ko = 0				    
				       result != 0	ko = 0 */
					
					if ( (operator.equals("equals")      && value.equals("0")) || (operator.equals("distinct") && value.equals("1")) )
						filter.append("ko > 0 ");
					else if ( (operator.equals("equals") && value.equals("1")) || (operator.equals("distinct") && value.equals("0")) )
						filter.append("ko = 0 ");
					else
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid operation filter to boolean");
				}
				else
				{						
					/* Nos llega el campo "result", que equivale a la columna errorcode y se traduce al sql según la casuística:
					   LLLEGA			SE TRADUCE COMO
					   result  = 0		errorcode is not null
					   result != 1		errorcode is not null									
				       result  = 1		errorcode is null				    
				       result != 0		errorcode is null */
					
					if ( (operator.equals("equals")       && value.equals("0")) || (operator.equals("distinct") && value.equals("1")) )
						filter.append("errorcode is not null ");
					else if ( (operator.equals("equals")  && value.equals("1")) || (operator.equals("distinct") && value.equals("0")) )
						filter.append("errorcode is null ");
					else
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid operation filter to boolean");
				}
			}
			else if (subjectType.equals("number"))
			{
				if (operator.equals("equals"))
					filter.append(subject + " = "  + StringEscapeUtils.escapeSql(value) + " ");
				else if (operator.equals("distinct"))
					filter.append(subject + " != " + StringEscapeUtils.escapeSql(value) + " ");
				else if (operator.equals("smaller"))
					filter.append(subject + " < "  + StringEscapeUtils.escapeSql(value) + " ");
				else if (operator.equals("greater"))
					filter.append(subject + " > "  + StringEscapeUtils.escapeSql(value) + " ");					
				else
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid operation filter to numbers");
			}
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid subject type filter (string, date, boolean, number)");
		}
		
		filter.append(")\n");
		
		return filter.toString(); 
	}
}