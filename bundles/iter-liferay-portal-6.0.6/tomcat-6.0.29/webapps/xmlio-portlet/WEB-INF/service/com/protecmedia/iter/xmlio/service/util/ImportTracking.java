package com.protecmedia.iter.xmlio.service.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;

public class ImportTracking
{
	final private static Log _log = LogFactoryUtil.getLog(ImportTracking.class);
	
	private static final String OUTPUT_BBDD = "BBDD";
	private static final String OUTPUT_DISK = "DISK";
	private static final String OUTPUT_LOG = "LOG";
	
	private String[] outputs = null;
	private String scopeGroupId = "";
	
	
	private final String INSERT_IMPORT = new StringBuffer()
						 .append("INSERT INTO importation(importid, groupid, type, filename, starttime) VALUES \n")
						 .append("('%s', %s, '%s', %s, sysdate())").toString();
	
	private final String INSERT_IMPORTDETAIL = new StringBuffer()
						.append("INSERT INTO importationdetail(importdetailid, importid, subject, errorcode, errordetail) VALUES \n")
						.append(" ('%s', '%s', '%s', %s, %s) ").toString();

	
	private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	public static Lock writeDomLock = readWriteLock.writeLock();
	public static Lock readDomLock = readWriteLock.readLock();
	
	private String importationId = "";
	
	public ImportTracking(String[] outputs, String scopeGroupId, String importType, String fileName) throws DocumentException, ServiceError, IOException, SQLException
	{
		this.outputs = outputs;
		this.scopeGroupId = scopeGroupId;
		
		for (int s = 0; s < outputs.length; s++)
		{
			String exitCode = outputs[s];
			
			if (OUTPUT_BBDD.equalsIgnoreCase(exitCode))
			{
				importationId = SQLQueries.getUUID();
				String sql = String.format(INSERT_IMPORT, importationId, scopeGroupId, importType, 
	                       Validator.isNotNull(fileName) ? StringUtil.apostrophe(fileName) : StringPool.NULL);
				_log.debug(new StringBuffer("Query to insert importation: \n").append(sql).toString());
				PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
			}
			if(OUTPUT_DISK.equalsIgnoreCase(exitCode))
			{
				// Crear un fichero para escribir los resultados.
			}
		}
	}
	
	public void writeArticleResult(String articleId, String errorCode, String errorDetail) throws ServiceError, IOException, SQLException
	{
		for (int s = 0; s < outputs.length; s++)
		{
			String exitCode = outputs[s];
			
			if(OUTPUT_BBDD.equalsIgnoreCase(exitCode))
			{
				Element rootElement = SAXReaderUtil.createElement("a");
				Document subject = SAXReaderUtil.createDocument(rootElement);
				
				rootElement.addAttribute("articleId", articleId);
				rootElement.addAttribute("scopeGroupId", scopeGroupId);
				
				
				String sql = String.format(INSERT_IMPORTDETAIL, SQLQueries.getUUID(), importationId, subject.asXML(), 
											Validator.isNotNull(errorCode) ? StringUtil.apostrophe(errorCode) : StringPool.NULL, 
											Validator.isNotNull(errorDetail) ? StringUtil.apostrophe(errorDetail) : StringPool.NULL);
				_log.debug(new StringBuffer("Query to insert importation detail: \n").append(sql).toString());
				PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
			}
			if(OUTPUT_LOG.equalsIgnoreCase(exitCode))
			{
//				_log.info(XmlioKeys.PREFIX_ARTICLE_IMPORTED_LOG + " article: " + articleId + ",\tstatus: " + (Validator.isNull(errorCode) ? " OK " : " KO ") +
//						(Validator.isNotNull(errorDetail) ? "\tdetail: " + errorDetail : StringPool.BLANK) + "\n" );
			}
			if(OUTPUT_DISK.equalsIgnoreCase(exitCode))
			{
				
			}
		}

	}
}
