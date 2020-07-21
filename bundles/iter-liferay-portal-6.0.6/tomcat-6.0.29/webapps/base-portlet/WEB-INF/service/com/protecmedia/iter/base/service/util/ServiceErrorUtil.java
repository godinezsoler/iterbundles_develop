package com.protecmedia.iter.base.service.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;

public class ServiceErrorUtil 
{
	
	public static String getServiceErrorAsXml(String errorCode, Throwable errorSource)
	{
		return com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(errorCode, errorSource);
	}

	/**
	 * @param msg
	 * @return devuelve la excepción de Iter (XML que tiene la estructura devuelta en el getServiceErrorAsXml)
	 */
	public static String containIterException(String msg)
	{
		return com.liferay.portal.kernel.error.ServiceErrorUtil.containIterException(msg);
	}
	
	/**
	 * 
	 * @param e
	 * @return null si e no contiene de alguna forma un ServiceError 
	 */
	public static ServiceError toServiceError(Throwable e)
	{
		return new ServiceError(com.liferay.portal.kernel.error.ServiceErrorUtil.toServiceError(e));
	}
	/**
	 * 
	 * @param e
	 * @return
	 */
	public static ServiceError toServiceError(String msg)
	{
		return new ServiceError(com.liferay.portal.kernel.error.ServiceErrorUtil.toServiceError(msg));
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	public static ServiceError toServiceError(com.liferay.portal.kernel.xml.Document doc)
	{
		return new ServiceError(com.liferay.portal.kernel.error.ServiceErrorUtil.toServiceError(doc));
	}

	/**
	 * @param e
	 * @return true si el mensaje de la Excepción es un XML que tiene la estructura devuelta en el getServiceErrorAsXml
	 */
	public static boolean isIterException(Throwable e)
	{
		return isIterException(e.getMessage());
	}
	/**
	 * @param msg
	 * @return true si la cadena es un XML que tiene la estructura devuelta en el getServiceErrorAsXml
	 */
	public static boolean isIterException(String msg)
	{
		return com.liferay.portal.kernel.error.ServiceErrorUtil.isIterException(msg);
	}
	/**
	 * @param doc
	 * @return true si el XML tiene la estructura devuelta en el getServiceErrorAsXml 
	 */
	public static boolean isIterException(com.liferay.portal.kernel.xml.Document doc)
	{
		return com.liferay.portal.kernel.error.ServiceErrorUtil.isIterException(doc);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// public static String getStackTrace(Throwable e) 
	//
	// Devuelve como cadena el stack de una excepción
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public static String getStackTrace(Throwable e) 
	{
		return com.liferay.portal.kernel.error.ServiceErrorUtil.getStackTrace(e); 
	}
	
	public static void throwSQLIterException(ORMException e) throws ServiceError
	{
		try
		{
			com.liferay.portal.kernel.error.ServiceErrorUtil.throwSQLIterException(e);
		}
		catch (com.liferay.portal.kernel.error.ServiceError serviceError)
		{
			throw new ServiceError(serviceError);
		}
	}
	
	public static String getErrorCode(ORMException e)
	{
		return com.liferay.portal.kernel.error.ServiceErrorUtil.getErrorCode(e);
	}
}