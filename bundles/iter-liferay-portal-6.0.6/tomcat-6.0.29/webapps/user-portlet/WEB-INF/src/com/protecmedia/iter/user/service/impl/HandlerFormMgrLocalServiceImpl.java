package com.protecmedia.iter.user.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.user.service.base.HandlerFormMgrLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.FormUtil;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class HandlerFormMgrLocalServiceImpl extends HandlerFormMgrLocalServiceBaseImpl
{
	
	private static Log _log = LogFactoryUtil.getLog(HandlerFormMgrLocalServiceImpl.class);
	
	
	public void startHandlerDatabaseForm(Map<String, ArrayList> adjuntos, Document xmlDom, Long groupId, String formReceivedId) throws ServiceError, IOException, SQLException, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(xmlDom), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_XMLDOM_IS_NULL_ZYX );
		ErrorRaiser.throwIfFalse( Validator.isNotNull(formReceivedId), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_FORMRECEIVEDID_IS_NULL_ZYX );		
		
		// Comentazamos a leer el xml (Documento) recibido		
		
		// Leemos los datos del formulario
		final String formId = XMLHelper.getTextValueOf(xmlDom, "/formdata/@formid");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(formId), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_FORMID_IS_NULL_ZYX );
		
		/*final String formName = XMLHelper.getTextValueOf(xmlDom, "/formdata/@forname");
		 * if (Validator.isNull(formName)){
			_log.debug("formName is null");
		}
		ErrorRaiser.throwIfNull(formName);*/
		
		final String dateSent = XMLHelper.getTextValueOf(xmlDom, "/formdata/delivery-info/date-sent");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(dateSent), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_DATESENT_IS_NULL_ZYX);
		
		// Datos de la cookie, id del usuario. Si es null, el formulario es anonimo
		final String usrId = XMLHelper.getTextValueOf(xmlDom, "/formdata/delivery-info/sending-user/usrid");
		
		StringBuilder sql = new StringBuilder();	
		
		// Insertamos el formulario en base de datos
		sql.append("INSERT into formreceived (formreceivedid, formid, datesent, sendingusr) VALUES (\n")
		.append("'").append(formReceivedId).append("'").append(", ")
		.append("'").append(formId)        .append("'").append(", ")
		.append("'").append(dateSent)      .append("'").append(", ")		
		.append(null == usrId || usrId.isEmpty() ? "null" : ("'" + usrId + "'"))
		.append(")");
		

		_log.debug(new StringBuffer("Query: ").append(sql.toString()));
		PortalLocalServiceUtil.executeUpdateQuery(sql.toString());		
		
		// Inicio de consulta para insert multiple
		sql = new StringBuilder();
		sql.append("INSERT into fieldreceived (fieldreceivedid, formreceivedid, fieldid, fieldvalue, binfieldvalueid) VALUES \n");
		
		// Recorremos los fieldsgroup
		List<Node> fieldsGroupNodes = xmlDom.selectNodes("/formdata/fieldsgroup");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(fieldsGroupNodes), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_FIELDSGROUPNODES_IS_NULL_ZYX);
		
		for (int t = 0; t < fieldsGroupNodes.size(); t++)
		{
			final String tabName = XMLHelper.getTextValueOf(fieldsGroupNodes.get(t), "@name");	
			ErrorRaiser.throwIfFalse( Validator.isNotNull(tabName), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_TABNAME_IS_NULL_ZYX);
			
			List<Node> fieldsNodes = fieldsGroupNodes.get(t).selectNodes("field");	
			ErrorRaiser.throwIfFalse( Validator.isNotNull(fieldsNodes), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_FIELDSNODES_IS_NULL_ZYX);
						
			final long defaultUserId = GroupMgr.getDefaultUserId();
			
			// Recorremos los inputs/fields
			for (int i = 0; i < fieldsNodes.size(); i++){				
				
				final String fieldId = XMLHelper.getTextValueOf(fieldsNodes.get(i), "@id");
				
				// Comprobamos si tiene valores, en funcion de si hay o no y de cuantos se aplica una logica distinta				
				List<Node> fieldsValues = fieldsNodes.get(i).selectNodes("data/value");
				
				// Es un binario, se aniade solo un input
				if (Validator.isNull(fieldsValues) || fieldsValues.isEmpty()){
					
					// No puede ser que nos lleguen inputs binarios y que no tengamos adjuntos
					ErrorRaiser.throwIfNull(adjuntos, IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_ADJUNTOS_IS_NULL_ZYX);
					
					Map<String,	Object> attachment = FormUtil.getAttachment(fieldsNodes.get(i), adjuntos);
					
					String name = (String) attachment.get(FormUtil.KEY_NAME);
					InputStream is = (InputStream) attachment.get(FormUtil.KEY_ATTACH);
					
					DLFileEntry  dlfileEntry = DLFileEntryMgrLocalServiceUtil.addDLFileEntry(groupId, defaultUserId, IterKeys.FORMS_ATTACHMENTS_FOLDER, name, is);
					ErrorRaiser.throwIfNull(dlfileEntry, IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_DLFILEENTRY_IS_NULL_ZYX);
					
					final String filePointer = Long.toString(dlfileEntry.getFileEntryId());
					ErrorRaiser.throwIfFalse( Validator.isNotNull(filePointer), IterErrorKeys.XYZ_ITR_E_DATABASE_FORM_HANDLER_FILEPOINTER_IS_NULL_ZYX);
					
					sql.append("('").append(SQLQueries.getUUID()).append("', ")
					.append("'").append(formReceivedId).append("', ")
					.append("'").append(fieldId).append("', ")					
					.append("null").append(", ")
					// Valor del binario
					.append(filePointer)
					.append("), \n");
				}else{
					// Se aniaden tantos inputs como valores tenga el input/field
					for (int v = 0; v < fieldsValues.size(); v++){		
						
						final String value = fieldsValues.get(v).getStringValue();
						
						sql.append("('").append(SQLQueries.getUUID()) .append("', ")
						.append("'").append(formReceivedId).append("', ")
						.append("'").append(fieldId).append("', ")
						// Escapamos el valor del input para que no hagan sqlInjection
						.append("'").append(StringEscapeUtils.escapeSql(value)).append("', ")
						.append("null), \n");
					}
				} 
			}			
		}
				
		_log.debug(new StringBuffer("Query: ").append(sql.toString().substring(0, sql.toString().lastIndexOf(","))));
		// Quitamos la ultima coma de la consulta y hacemos el insert multiple		
		PortalLocalServiceUtil.executeUpdateQuery(sql.toString().substring(0, sql.toString().lastIndexOf(",")));
	}

}