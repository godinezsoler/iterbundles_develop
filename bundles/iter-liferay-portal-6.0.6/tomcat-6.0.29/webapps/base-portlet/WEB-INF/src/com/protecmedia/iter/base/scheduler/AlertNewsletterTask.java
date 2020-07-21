package com.protecmedia.iter.base.scheduler;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil;
import com.protecmedia.iter.base.util.NewsletterTools;

/**
 * <p>Tarea encargada del envío de un boletín.</p>
 * <p>Realiza el envío de la newsletter y cuando finaliza, lo notifica a {@link AlertNewsletterMgr}.</p>
 * @author Protecmedia
 * @see com.protecmedia.iter.base.scheduler.AlertNewsletterMgr
 */
public class AlertNewsletterTask extends Thread
{
	/** Logger. */
	private static Log log = LogFactoryUtil.getLog(AlertNewsletterTask.class);
	
	/** Identificador de la programación de la newsletterr */
	private final String scheduleId;
	
	/** Última fecha de publicación del grupo en milisegundos. */
	private final String lastUpdate;
	
	private final String content;
	
	/** Sentencia {@code SQL} para actualizar la fecha de y el resultado de la última ejecución de la newsletter. */
	private static final String SQL_UPDATE_SCHEDULE_NEWSLETTER_STATUS = "UPDATE schedule_newsletter SET lasttriggered=%s, laststatus='%s' WHERE scheduleid='%s'";
	
	/** Sentencia {@code SQL} para recuperar el {@code newsletterId} correspondiente a un {@code scheduleId}. */
	private static final String SQL_SELECT_SCHEDULEID = "SELECT newsletterid FROM schedule_newsletter WHERE scheduleid = '%s'";
	
	/**
	 * <p>Inicializa una tarea de envío de newsletter.</p>
	 * @param scheduleId El identificador de la programación de la newsletter.
	 * @param lastUpdate La última fecha de publicación del grupo en milisegundos.
	 */
	public AlertNewsletterTask(String scheduleId, String lastUpdate, String content)
	{
		super(new StringBuilder("Alert Newsletter Task [").append(scheduleId).append(" - ").append(lastUpdate).append("]").toString());
		this.scheduleId = scheduleId;
		this.lastUpdate = lastUpdate;
		this.content    = content;
	}
	
	@Override
	public void run()
	{
		try
		{
			sendNewsletter();
		}
		catch (Throwable e)
		{
			log.error(e);
		}
		finally
		{
			try
			{
				AlertNewsletterMgr.notifyFinish(scheduleId, lastUpdate);
			}
			catch (Throwable th)
			{
				log.error(th);
			}
		}
		
	}
	
	private void sendNewsletter() throws PortalException, SystemException, Exception
	{
		// Recupera el Id de la newsletter
		String newsletterId = getNewsletterId(scheduleId);
		
		if (Validator.isNotNull(content))
		{
			// Si no es el mismo que el del último envío
			if (NewsletterTools.checkNewsletterContent(newsletterId, new StringBuffer(content)))
			{
				// Envía la newsletter
				NewsletterMgrLocalServiceUtil.sendNewsletter(scheduleId, content, lastUpdate, false);
			}
			else
			{
				log.error("Schedule ".concat(scheduleId).concat(" has repeated content"));
			}
		}
		else
		{
			log.error("Schedule ".concat(scheduleId).concat(" has no content to send"));
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_SCHEDULE_NEWSLETTER_STATUS, "NULL", "No content to send", scheduleId));
		}
	}
	
	/**
	 * <p>Recupera el {@code newsletterId} correspondiente a un {@code scheduleId}.
	 * @param scheduleId El identificador de la programación de la newsletter.
	 * @return {@code String} con el contenido {@code newsletterId} de la newsletter.
	 * @throws SecurityException     Si ocurre un error al recuperar el {@code newsletterId} de la Base de Datos
	 * @throws NoSuchMethodException Si ocurre un error al recuperar el {@code newsletterId} de la Base de Datos
	 */
	private static String getNewsletterId(String scheduleId) throws SecurityException, NoSuchMethodException
	{
		Document newsletter = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_SCHEDULEID, scheduleId));
		return XMLHelper.getTextValueOf(newsletter, "/rs/row/@newsletterid");
	}
}
