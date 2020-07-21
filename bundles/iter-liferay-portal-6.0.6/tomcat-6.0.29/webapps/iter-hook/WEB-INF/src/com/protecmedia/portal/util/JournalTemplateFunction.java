package com.protecmedia.portal.util;

import java.io.StringWriter;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.velocity.VelocityContext;
import com.liferay.portal.kernel.velocity.VelocityEngineUtil;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.util.GlobalJournalTemplateMgr;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;


public abstract class JournalTemplateFunction implements Callable<JournalTemplate>
{
	private static Log _log = LogFactoryUtil.getLog(JournalTemplateFunction.class);
	
	protected Exception _e 			= null;
	protected long 		_groupId	= 0;
	protected String 	_templateId = StringPool.BLANK;
	
	public JournalTemplateFunction(long groupId, String templateId)
	{
		_groupId	= groupId;
		_templateId = templateId;
	}
	
	public JournalTemplate call ()
	{
		JournalTemplate template = null;
		try
		{
			template = doCall();
		}
		catch (Exception e)
		{
			_e = e;
		}
		return template;
	}
	
	/**
	 * - Carga en un thread la clase oportuna (ADD, UPDATE, DELETE).<br/>
	 * - Espera a que termine la operación (ADD, UPDATE, DELETE).<br/>
	 * - Actualiza la marca de tiempo (lastModified) indicando que las plantillas de tiempo han sido modificadas.<br/>
	 * - Devuelve el JournalTemplate recién modificado.<br/>
	 * 
	 * @return Devuelve el JournalTemplate recién modificado
	 * @throws Exception 
	 */
	protected JournalTemplate modifyGlobalTemplate() throws Exception
	{
		// Se lanza la tarea
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<JournalTemplate> future  = executorService.submit(this);
		
		executorService.shutdown();
		
		// Se espera a que termine
		while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
		{
			_log.warn( String.format("Waiting for %s %s", getFunctionName(), _templateId) );
		}
		
		if (_e != null)
			throw _e;
			
		// Actualiza la marca de tiempo
		GlobalJournalTemplateMgr.modify(_templateId);
		
		return future.get();
	}
	
	/**
	 * En el entorno LIVE no es necesario ejecutar la función en otro hilo, ya que se actualizará el
	 * lastModified una vez por todas las plantillas, antes de refrescar la caché
	 * 
	 * @param templateId
	 * @return
	 */
	protected static boolean processAsGlobal(String templateId)
	{
		return PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW) && GlobalJournalTemplateMgr.isGlobalTemplate(templateId);
	}
	
	protected static void checkVelocitySyntax(String xsl) throws Exception
	{
		VelocityContext velocityContext = VelocityEngineUtil.getEmptyContext();
		
		StringWriter sw = new StringWriter();
        
		VelocityEngineUtil.mergeTemplate(String.valueOf(new Date().getTime()), xsl, velocityContext, sw);	
		sw.close();
	}
	
	protected abstract JournalTemplate doCall() throws PortalException, SystemException;
	protected abstract String getFunctionName();
}
