package com.protecmedia.iter.base.service.util;

import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.velocity.VelocityContext;
import com.liferay.portal.kernel.velocity.VelocityEngineUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class VelocityUtil {	
	
	private static Log _log = LogFactoryUtil.getLog(VelocityUtil.class);	
	
	private static final String GET_CONTENT_TEMPLATE_FROM_JOURNAL_TEMPLATE = new StringBuffer()
		.append("SELECT xsl \n")
		.append("FROM JournalTemplate \n")
		.append("WHERE groupId = %s \n")
		.append("  AND templateId = '%s'").toString();
	
	// Transforma un string con velócity
	public static void mergeTemplate(String contentTemplate, HashMap<String, Object>variablesToBeInjected, Writer writer) throws Exception{
		_log.trace("In VelocityUtil.mergeTemplate(String)");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(contentTemplate), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Content template given is empty or null");
		ErrorRaiser.throwIfNull(writer, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No writer given");

		// VelocityContext velocityContext = VelocityEngineUtil.getWrappedStandardToolsContext();
		VelocityContext velocityContext  = VelocityEngineUtil.getEmptyContext(); 
		
		//VelocityContext velocityContext = new VelocityContext();
		
		// Metemos las variables pasadas a velócity
		if (null != variablesToBeInjected && variablesToBeInjected.size() > 0){			
			_log.debug("With variables");
			for (Entry<?, ?> e: variablesToBeInjected.entrySet()) {
				velocityContext.put( (String)e.getKey(), (Object)e.getValue() );
				
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("Injecting variable '").append(e.getKey()).append("'"));
			}
		}

		try{
			// Realizamos la transformación.
			// La función mergeTemplate no admite en el primer parámetro null, ni vacío, se pone una cadena a pelo (da igual su valor).
			VelocityEngineUtil.mergeTemplate((new Random(10000)).toString(), contentTemplate, velocityContext, writer);						
		}catch(Exception e){
			_log.error("Error merging the template", e);
			throw e;
		}
	}
	
	// Obtiene el contenido de un archivo (UTF-8 por defecto) y lo transforma con velócity
	public static void mergeTemplate(File fileTemplate, String fileCodification, HashMap<String, Object>variablesToBeInjected, Writer writer) throws Exception{
		_log.trace("In VelocityUtil.mergeTemplate(File)");
		
		ErrorRaiser.throwIfFalse(null != fileTemplate   && 
						         fileTemplate.exists()  && 
						         fileTemplate.isFile()  && 
						         fileTemplate.canRead(), 
						         com.protecmedia.iter.base.service.util.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, 
						         (new StringBuilder("Template: '").append(fileTemplate.getAbsolutePath()).append("' can not be read/found")).toString() );
		
		// Obtenemos el contenido del archivo
		String contentTemplate = FileUtils.readFileToString(fileTemplate, (Validator.isNull(fileCodification) ? IterKeys.UTF8 : fileCodification)); 
		
		mergeTemplate(contentTemplate, variablesToBeInjected, writer);
	}
	
	// Obtiene una plantilla de base de datos (journaltemplate.templateid, grupo dado o grupo global en su defecto) y la transforma con velócity
	public static void mergeTemplate(String templateId, Long groupId, HashMap<String, Object>variablesToBeInjected, Writer writer) throws Exception{
		_log.trace("In VelocityUtil.mergeTemplate(BBDD)");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(templateId));
		
		final String sql = String.format(GET_CONTENT_TEMPLATE_FROM_JOURNAL_TEMPLATE,
										 Long.toString( (null == groupId ? GroupMgr.getGlobalGroupId() : groupId) ),
			                             templateId);
		
		if(_log.isDebugEnabled())
			_log.debug("Query to get content template from journaltemplate:\n" + sql);
		
		final List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		ErrorRaiser.throwIfFalse( (result!=null && result.size() == 1), "No template found");
		
		mergeTemplate((String)result.get(0), variablesToBeInjected, writer);
	}

}