package com.protecmedia.iter.user.service.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.service.base.FormReceivedToCsvLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.forms.ExtractorXML;

/* Para probar esta clase desde el navegador: 
 * http://127.0.0.1:8080/base-portlet/urlendpoint?clsid=com.protecmedia.iter.user.service.FormReceivedToCsvServiceUtil&methodName=generateCSV&dispid=0&instanceID=null&p1=86bb1ccd-1eca-11e3-8d92-0017a44e2b78&p2=20140101235959&p3=20100101235959 
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
 public class FormReceivedToCsvLocalServiceImpl extends FormReceivedToCsvLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(ExtractorXML.class);
	
	final private SimpleDateFormat sDF = new SimpleDateFormat("yyyyMMddhhmmss");
	final private SimpleDateFormat sDFWithMls = new SimpleDateFormat("yyyyMMddhhmmssSSS");
	final private SimpleDateFormat sDFToDBAndCSV = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	
	final private String nextCell = ";";
	final private String lineBreak = "\n";
	
	final String GET_HEAD = new StringBuffer()
	.append("SELECT labelbefore, labelafter \n")
	.append("FROM \n")
	.append("(SELECT fr.datesent datesent, fr.formreceivedid, ffl.fieldid, flr.fieldreceivedid, \n") 
	.append("   EXTRACTVALUE(ffl.labelbefore, \"/labelbefore/textlabel/text()\") labelbefore, \n") 
	.append("	 EXTRACTVALUE(ffl.labelafter, \"/labelafter/textlabel/text()\") labelafter, df.fieldtype \n")
	.append(" FROM form f, formreceived fr, fieldreceived flr, formfield ffl, datafield df \n")
	.append(" WHERE f.formid = fr.formid \n") 
	.append("   AND fr.formreceivedid = flr.formreceivedid \n")
	.append("	AND ffl.fieldid = flr.fieldid \n") 
	.append("	AND df.datafieldid = ffl.datafieldid \n") 
	// Filtramos por el tipo de formulario pasado
	.append("   AND f.formid = '%s' \n" ) 
	  // Filtros dinamicos que se forman mas abajo
	.append("%s")
	.append("	AND df.fieldtype != 'array' \n") 
	.append("GROUP BY ffl.labelbefore, ffl.labelafter, df.fieldtype \n")
		 
	.append("UNION ALL \n")

	.append("SELECT fr.datesent datesent, fr.formreceivedid, ffl.fieldid, flr.fieldreceivedid, \n") 
	.append("EXTRACTVALUE(ffl.labelbefore, \"/labelbefore/textlabel/text()\") labelbefore, \n") 
	.append("EXTRACTVALUE(ffl.labelafter, \"/labelafter/textlabel/text()\") labelafter, df.fieldtype \n")
	.append("FROM form f, formreceived fr, fieldreceived flr, formfield ffl, datafield df \n")
	.append("WHERE f.formid = fr.formid \n") 
	.append("  AND fr.formreceivedid = flr.formreceivedid \n") 
	.append("  AND ffl.fieldid = flr.fieldid \n") 
	.append("  AND df.datafieldid = ffl.datafieldid \n") 
	// Filtramos por el tipo de formulario pasado
	.append("  AND f.formid = '%s' \n" ) 
	  // Filtros dinamicos que se forman mas abajo
	.append("%s \n")
	.append("  AND df.fieldtype = 'array' \n")
	.append("GROUP BY ffl.labelbefore, ffl.labelafter, df.fieldtype) result \n")
	.append("ORDER BY datesent, formreceivedid, fieldid, fieldreceivedid \n").toString();
	
	
	// Consulta para obtener los datos
	static String GET_DATES_FROM_FORM_RECEIVED = new StringBuffer()	
	.append("SELECT fr.formreceivedid, fr.datesent, u.usrname, flr.fieldreceivedid, flr.fieldvalue, flr.binfieldvalueid, df.fieldtype, ffl.fieldid \n")	
	.append("FROM form f, formreceived fr left outer join lportal.iterusers u on u.usrid = fr.sendingusr, fieldreceived flr, formfield ffl, datafield df \n")
	.append("WHERE f.formid = fr.formid \n")
	.append(" AND fr.formreceivedid = flr.formreceivedid \n") 
	.append(" AND ffl.fieldid = flr.fieldid \n")
	.append(" AND df.datafieldid = ffl.datafieldid \n")
	// Filtramos por el tipo de formulario pasado
	.append(" AND f.formid = '%s' \n" ) 
	  // Filtros dinamicos que se forman mas abajo
	.append(" %s \n")
	.append("ORDER BY fr.datesent, fr.formreceivedid, ffl.fieldid, flr.fieldreceivedid \n").toString();
	
	
	public void generateCSV(HttpServletRequest request, HttpServletResponse response, String formTypeId, String dateBefore, String dateAfter) throws Exception{
		generateCSVTranslated(request, response, formTypeId, dateBefore, dateAfter, null, null);
	}
	
	public void generateCSVTranslated(HttpServletRequest request, HttpServletResponse response, String formTypeId, String dateBefore, String dateAfter, String nameForDateSentColumn, String nameForUserColum) throws Exception{
		_log.trace("In generateCSV");
		
		if (Validator.isNull(formTypeId)){
			_log.debug("formTypeId is null");
		}		
		ErrorRaiser.throwIfNull(formTypeId);
		
		if (Validator.isNull(response)){
			_log.debug("response is null");
		}
		ErrorRaiser.throwIfNull(response);

		ServletOutputStream out = null;
        ByteArrayInputStream byteArrayInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        
        try {
        	StringBuffer dateFilters = new StringBuffer().append("");
        	// Aniadimos filtro de fecha        	
        	if (Validator.isNotNull(dateBefore)){        		
        		try{
        			final Date d = sDF.parse(dateBefore);
        			dateFilters.append("AND fr.datesent >= '").append(dateBefore).append("' \n");
        		}catch(Exception e){
        			_log.debug("Incorrect date before format");
        		}
        		
        	}
        	
        	// Aniadimos filtro de fecha
        	if (Validator.isNotNull(dateAfter)){
        		try{
        			final Date d = sDF.parse(dateAfter);
        			dateFilters.append("AND fr.datesent <= '").append(dateAfter).append("' \n");	
        		}catch(Exception e){
        			_log.debug("Incorrect date after format");
        		}
        	}
        	
        	// Obtenemos los datos de la cabecera
        	_log.debug(new StringBuffer("Query to get head: ").append(String.format(GET_HEAD, formTypeId, dateFilters.toString(), formTypeId, dateFilters.toString())));
        	Document result = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_HEAD, formTypeId, dateFilters.toString(), formTypeId, dateFilters.toString()), new String[]{"labelbefore", "labelafter"});
        	if (Validator.isNull(result)){
        		_log.debug("Result of head is null");
        	}
        	ErrorRaiser.throwIfNull(result);
        	
        	// Leemos los datos obtenidos en base de datos
        	List<Node> nodes = result.selectNodes("/rs/row");
        	if (Validator.isNull(nodes)){
    			_log.debug("Nodes head are null or empty");	
    		}
    		ErrorRaiser.throwIfNull(nodes);
    		
    		// Contenido del csv
        	StringBuffer cuerpoCSV = new StringBuffer();        
    		
    		// Si vienen nombres de columna para el envio y el usuario los colocamos.
       	 	if (Validator.isNotNull(nameForDateSentColumn)){
       	 		cuerpoCSV.append(buildCell(nameForDateSentColumn, false, true));
       	 	}else{
       	 		cuerpoCSV.append(buildCell("date sent", false, true));
       	 	}
       	 	if (Validator.isNotNull(nameForUserColum)){
       	 		cuerpoCSV.append(buildCell(nameForUserColum, false, true));
       	 	}else{
       	 		cuerpoCSV.append(buildCell("user name", false, true));
       	 	}
        	
        	// Escribimos las cabeceras del csv. Se coloca el labelbefore, de no existir el labelafter, de no existir vacío.        	
        	for (int n = 0; n < nodes.size(); n++){
        		final String labelbefore = XMLHelper.getTextValueOf(nodes.get(n), "labelbefore");
        		final String labelafter  = XMLHelper.getTextValueOf(nodes.get(n), "labelafter");
        		
        		// Labelbefore
        		if (Validator.isNotNull(labelbefore)){
        			cuerpoCSV.append(buildCell(labelbefore, false, true));
    			}else if (Validator.isNotNull(labelafter)){
    				cuerpoCSV.append(buildCell(labelafter, false, true));
    			}else{
    				cuerpoCSV.append(buildCell("", false, true));
    			}
        	}
      	
        	
        	// Obtenemos los datos de los formularios
        	_log.debug(new StringBuffer("Query: ").append(String.format(GET_DATES_FROM_FORM_RECEIVED, formTypeId, dateFilters.toString())));
        	result = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_DATES_FROM_FORM_RECEIVED, formTypeId, dateFilters.toString()));
        	if (Validator.isNull(result)){
        		_log.debug("Result of query is null");
        	}
        	ErrorRaiser.throwIfNull(result);
        	
        	// Leemos lo obtenido en base de datos
        	nodes = result.selectNodes("/rs/row");
        	if (Validator.isNull(nodes)){
    			_log.debug("Nodes are null or empty");	
    		}
    		ErrorRaiser.throwIfNull(nodes);
        	
        	  
        	// Para controlar si es el mismo formulario y si es el mismo input con diferentes valores
        	String beforeFormdId = "";
        	String beforeFormFieldId = "";
        	
        	// Recorremos los datos de la consulta y vamos componiendo el csv
        	for (int i = 0; i < nodes.size(); i++){
        		
        		// Obtenemos los datos del registro
        		String formReceivedId = XMLHelper.getTextValueOf(nodes.get(i), "@formreceivedid");
        		if (Validator.isNull(formReceivedId)){
        			_log.debug("formReceivedId is null");	
        		}
        		ErrorRaiser.throwIfNull(formReceivedId);
        		
        		String actualFieldid = XMLHelper.getTextValueOf(nodes.get(i), "@fieldid");
        		if (Validator.isNull(actualFieldid)){
        			_log.debug("fieldid is null");	
        		}
        		ErrorRaiser.throwIfNull(actualFieldid);
        		
        		
        		final String dataSent = XMLHelper.getTextValueOf(nodes.get(i), "@datesent");
        		if (Validator.isNull(dataSent)){
        			_log.debug("dataSent is null");
        		}
        		ErrorRaiser.throwIfNull(dataSent);
        		
        		final String userName = XMLHelper.getTextValueOf(nodes.get(i), "@usrname");
        		
        		final String actualFieldId = XMLHelper.getTextValueOf(nodes.get(i), "@fieldreceivedid");
        		if (Validator.isNull(actualFieldId)){
					_log.debug("fieldId is null");
        		}         		
				ErrorRaiser.throwIfNull(actualFieldId);
				
        		final String fieldType = XMLHelper.getTextValueOf(nodes.get(i), "@fieldtype");
        		if (Validator.isNull(fieldType)){
        			_log.debug("fieldType is null");	
        		}
        		
        		// Valor para input de tipo texto
        		String fieldTextValue = XMLHelper.getTextValueOf(nodes.get(i), "@fieldvalue");
        		// Valor para input de tipo binario
        		String fieldBinaryValue = XMLHelper.getTextValueOf(nodes.get(i), "@binfieldvalueid");
				// Si no nos llega ningun valor
				if (Validator.isNull(fieldTextValue) && Validator.isNull(fieldBinaryValue)){
					_log.debug("Field value is null");
        		}
				ErrorRaiser.throwIfNull(Validator.isNull(fieldTextValue) && Validator.isNull(fieldBinaryValue));
        		        		
        		// Cuando cambia el formulario
        		if (!formReceivedId.equals(beforeFormdId)){
        			// La primera vez no saltamos de linea
        			//if (i != 0){
        				cuerpoCSV.append(buildCell("", true, false));
        			//}
        			
        			beforeFormdId = formReceivedId;
        			Date date = sDFWithMls.parse(dataSent);
        			cuerpoCSV.append(buildCell(sDFToDBAndCSV.format(date), false, true));
        			
        			if (Validator.isNotNull(userName)){
        				cuerpoCSV.append(buildCell(userName, false, true));
        			}else{
        				cuerpoCSV.append(buildCell("", false, true));
        			}
        		}

        		// Si el input es de tipo array, seguimos recorriendo hasta que se acabe el input, el formulario o los datos
        		if (fieldType.equals("array")){
        			List<String> valuesCell = new ArrayList<String>();
        			
        			// Forzamos la entrada
        			beforeFormFieldId = actualFieldId;
                		
        			while(beforeFormFieldId.equals(actualFieldId) && formReceivedId.equals(beforeFormdId) && i < nodes.size()){
        				String value = null;
        				
        				formReceivedId = XMLHelper.getTextValueOf(nodes.get(i), "@formreceivedid");
        				ErrorRaiser.throwIfNull(formReceivedId);
        				
        				fieldTextValue = XMLHelper.getTextValueOf(nodes.get(i), "@fieldvalue");
        				fieldBinaryValue = XMLHelper.getTextValueOf(nodes.get(i), "@binfieldvalueid");
        				ErrorRaiser.throwIfNull(Validator.isNull(fieldTextValue) && Validator.isNull(fieldBinaryValue));
        				
        				if(fieldTextValue != null){
        					value = fieldTextValue;
        				}else{        	
            				// Obtenemos el enlace al binario
        					value = getUrlBinary(actualFieldId, request);
        				}
        				
        				
        				// Para no saltarnos registros si cambia el formulario
        				if (formReceivedId.equals(beforeFormdId)){
        					i += 1;
        					valuesCell.add(value);
        				}
        				
        				if (i < nodes.size() -2){            				          			
            				actualFieldid = XMLHelper.getTextValueOf(nodes.get(i), "@fieldid");
            				ErrorRaiser.throwIfNull(actualFieldid);
            				
        				// Para salir del bucle en caso de que el ultimo registro que viene de la base de datos sea de tipo array y no haya cambiado el 
            			}else{
            				actualFieldid = "";
            			}        				
        			}
        			
        			// Con el bucle while avanzamos uno de mas siempre. De no colocar esto nos saltariamos un registro
        			i -= 1;
        			
        			// Nos quedamos con el ultimo
        			beforeFormFieldId = actualFieldid;
        			
        			// Componemos la celda con varios valores
        			cuerpoCSV.append("\"");        			
        			for (int a = 0; a < valuesCell.size(); a++){
        				cuerpoCSV.append((String)valuesCell.get((a)));
        				if (a != valuesCell.size() -1){        					
        					cuerpoCSV.append(lineBreak);
        				}        				
        			}        			
        			cuerpoCSV.append("\"");
        			cuerpoCSV.append(nextCell);
        		
        		// No es de tipo array
        		}else{
        			// Si es de tipo texto
        			if(Validator.isNotNull(fieldTextValue)){
        				if(Validator.equals(fieldType, "date"))
        				{
                			Date date = sDFWithMls.parse(fieldTextValue);
                			cuerpoCSV.append(buildCell(sDFToDBAndCSV.format(date), false, true));	
        				}
        				else
        				{
        					cuerpoCSV.append(buildCell(fieldTextValue, false, true));
        				}
        			// Es binario
        			}else{
        				// Obtenemos el enlace al binario
        				String value = getUrlBinary(actualFieldId, request);        				

        				cuerpoCSV.append(buildCell(value, false, true));
        			}  
        		}  			
        	}

        	
        	// Especificamos que la respuesta sera de tipo csv
            response.setContentType("text/csv");
            final String disposition = "attachment; fileName=data.csv";
            response.setHeader("Content-Disposition", disposition);
            // Esta linea dara error si se llama desde un jsp
            out = response.getOutputStream();
        	
        	// Escribimos el contenido ya formado
            out.write(cuerpoCSV.toString().getBytes());
            		
            out.flush();
            out.close();

        } catch (Exception e) {
        	_log.debug(new StringBuffer("Error: ").append(e.getStackTrace()));
        	throw e;

        } finally {
        	try{
	            if (out != null){
	                out.close();
	            }
	            if (byteArrayInputStream != null) {
	                byteArrayInputStream.close();
	            }
	            if (bufferedOutputStream != null) {
	                bufferedOutputStream.close();
	            }
        	}catch(Exception e){
        		_log.debug(new StringBuffer("Error while closing: ").append(e.getStackTrace()));
        	}
        }
	}

	/**
	 * 
	 * @param text
	 * @param withLineBreak
	 * @param withNextCell
	 * @return
	 */
	private String buildCell(String text, boolean withLineBreak, boolean withNextCell){
		StringBuffer aux = new StringBuffer();		
		aux.append(text);
		
		// Texto y salto de lina para ir a la fila de abajo.
		if (withLineBreak){
			aux.append(lineBreak);
			
		// Texto y ; para continuar en la celda de la derecha
		}else if(withNextCell){			
			aux.append(nextCell);
		}
		
		return aux.toString().toString();
	}
	
	/**
	 * 
	 * @param formFieldReceiverId
	 * @param serverName
	 * @return
	 * @throws ServiceError 
	 */
	// Obtiene la direccion que apunta al binario
	private String getUrlBinary(String fieldReceivedId, HttpServletRequest request) throws ServiceError{
		String urlString = null;
		
		if(Validator.isNull(request)){
			_log.debug(new StringBuffer("Request in getUrlBinary is null"));
		}
		ErrorRaiser.throwIfNull(request);
		
		try {
			URL url = FileFormReceivedMgrLocalServiceImpl.getURLFileReceived(fieldReceivedId, request);
			if(Validator.isNull(url)){
				_log.debug(new StringBuffer("getUrlBinary returns null with: ").append(fieldReceivedId).toString());
			}
			ErrorRaiser.throwIfNull(url);
			
			urlString = url.toString();
		}catch (Exception e){
			_log.debug(new StringBuffer("Error in getUrlBinary: ").append(e.getStackTrace().toString()));
		}finally{
			return urlString;
		}
	}
	
//	/**
//	 * 
//	 * @param link
//	 * @return
//	 */
//	private String getHiperlink(String link){		
////		StringBuffer sB = new StringBuffer();
////		sB.append("=HIPERVINCULO(\"").append(link).append("\")");		
////		return sB.toString();
//		return link;
//	}
	
}