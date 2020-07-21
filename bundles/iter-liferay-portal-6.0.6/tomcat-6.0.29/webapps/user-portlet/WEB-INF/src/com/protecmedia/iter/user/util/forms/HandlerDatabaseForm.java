package com.protecmedia.iter.user.util.forms;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.user.service.HandlerFormMgrLocalServiceUtil;

// Se hara un rollback de todas las consultas en caso de que alguna salga mal
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class HandlerDatabaseForm extends Thread{

	private static Log _log = LogFactoryUtil.getLog(HandlerDatabaseForm.class);
	
	private Document xmlDom;
	private boolean critic;
	private Map<String, ArrayList> adjuntos;
	private Long groupId;
	private String formReceivedId;
	private List<Throwable> exceptionsHandlers;
	private Semaphore semaphore;
	
	/**
	 * 
	 * @param xmlDom
	 * @param critic
	 * @param adjuntos
	 */
	public HandlerDatabaseForm(Document xmlDom,  Map<String, ArrayList> adjuntos, Long groupId, String formReceivedId, List<Throwable> exceptionsHandlers, Semaphore semaphore) {
		this.xmlDom = SAXReaderUtil.createDocument();
		this.xmlDom.setRootElement(xmlDom.getRootElement().createCopy()); 
		
		this.adjuntos = adjuntos;
		this.groupId = groupId;
		this.exceptionsHandlers = exceptionsHandlers;
		this.semaphore = semaphore;
		this.formReceivedId = formReceivedId;
	}
		
	@Override
	public void run() 
	{
		super.run();
		try 
		{
			localRun();
		}
		catch (Throwable th) 
		{
			IterMonitor.logEvent(this.groupId, Event.ERROR, new Date(System.currentTimeMillis()), "Form handler database error", "", th);
			_log.error(th);
			if(Validator.isNotNull(exceptionsHandlers))
			{
				try 
				{
					semaphore.acquire();
					exceptionsHandlers.add(th);
					_log.error("throw Exception from thread HandlerDatabaseForm to FormReceiver (critic HandlerDatabaseForm)");
					semaphore.release();
				}
				catch (InterruptedException e1) 
				{
				}
			}
			else 
				_log.error("no critic HandlerDatabaseForm");
		}
	}

	/**
	 * 
	 * @throws ServiceError
	 * @throws IOException
	 * @throws SQLException
	 * @throws SystemException 
	 * @throws PortalException 
	 */	
	private void localRun() throws ServiceError, IOException, SQLException, PortalException, SystemException
	{
		HandlerFormMgrLocalServiceUtil.startHandlerDatabaseForm(this.adjuntos, this.xmlDom, this.groupId, this.formReceivedId);
	}
}
