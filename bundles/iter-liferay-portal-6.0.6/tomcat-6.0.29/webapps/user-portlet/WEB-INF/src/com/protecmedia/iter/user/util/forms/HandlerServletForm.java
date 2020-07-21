package com.protecmedia.iter.user.util.forms;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ContextVariables;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.MimeTypeTools;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil;

public class HandlerServletForm extends Thread
{

	private static Log _log = LogFactoryUtil.getLog(HandlerServletForm.class);
	private final List<Integer> responseStatusOk =  Arrays.asList(new Integer[]{HttpStatus.SC_OK, HttpStatus.SC_CREATED, HttpStatus.SC_ACCEPTED , HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION , HttpStatus.SC_NO_CONTENT, HttpStatus.SC_RESET_CONTENT });
	
	private boolean processdata;
	private String xslid;
	private Document xmlDom;
	private String serverURL;
	private HttpServletRequest request;
	private Map<String, ArrayList> adjuntos;
	private List<Throwable> exceptionsHandlers;
	private Semaphore semaphore;
	private String formReceivedId;
	private long groupId;

	public HandlerServletForm(long groupId, boolean processdata, Document xmlDom, Map<String, ArrayList> adjuntos, String xslid, HttpServletRequest request, String serverURL, String formReceivedId, List<Throwable> exceptionsHandlers, Semaphore semaphore) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(serverURL), "ITER_E_SERVER_URL_IS_NULL");
		
		this.groupId = groupId;
		this.serverURL = replaceVarsForm(serverURL, xmlDom);
		this.formReceivedId = formReceivedId;
		this.processdata = processdata;
		if (Validator.isNotNull(xmlDom))
		{
			this.xmlDom = SAXReaderUtil.createDocument();
			this.xmlDom.setRootElement(xmlDom.getRootElement().createCopy());
		}
		if (Validator.isNotNull(adjuntos))
		{
			this.adjuntos = adjuntos;
		}
		if (Validator.isNotNull(request))
		{
			this.request = request;
		}
		if (Validator.isNotNull(xslid))
		{
			this.xslid = xslid;
		}
		this.exceptionsHandlers = exceptionsHandlers;
		this.semaphore = semaphore;
	}

	@Override
	public void run()
	{
		try
		{
			localRun();
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(this.groupId, Event.ERROR, new Date(System.currentTimeMillis()), "Form handler servlet error", "", th);
			_log.error(th);
			if (Validator.isNotNull(exceptionsHandlers))
			{
				try
				{
					semaphore.acquire();
					exceptionsHandlers.add(th);
					_log.error("throw Exception from thread HandlerServletForm to FormReceiver (critic HandlerServletForm)");
					semaphore.release();
				}
				catch (InterruptedException e1)
				{
				}
			}
			else
				_log.error("no critic HandlerServletForm");
		}
	}

	private void localRun() throws ServiceError, PortalException, SystemException, NoSuchMethodException, SecurityException, TransformerException, DocumentException, IOException, ParseException, FileUploadException
	{
		int response;

		// Si no viene la url para el post no se puede hacer nada
		if (_log.isDebugEnabled() && Validator.isNull(serverURL))
		{
			_log.debug("server url value is null");
		}
		ErrorRaiser.throwIfFalse(Validator.isNotNull(serverURL), IterErrorKeys.XYZ_ITR_E_SERVLET_FORM_HANDLER_SERVER_URL_IS_NULL_ZYX);

		// Se quiere procesar
		if (processdata)
		{
			if (Validator.isNotNull(xslid))
			{// Post con transformado y adjuntos
				_log.trace("Post with transformed and attachtments");
				long delegationId = GroupLocalServiceUtil.getGroup(this.groupId).getDelegationId();
				InputStream isXSL = DLFileEntryLocalServiceUtil.getFileAsStreamByUuid(xslid, delegationId);
				ErrorRaiser.throwIfNull(isXSL);

				Element dataRoot = SAXReaderUtil.read(isXSL).getRootElement();
				String typeXSLT = XMLHelper.getTextValueOf(dataRoot, "/xsl:stylesheet/xsl:output/@method", null);
				if(Validator.isNull(typeXSLT)){
					_log.trace("XSL not contain 'xsl:output/@method'. default: xml");
					typeXSLT = "xml";
				}
				
				final String transformed = XSLUtil.transform(XSLUtil.getSource(xmlDom.asXML()), XSLUtil.getSource(dataRoot.asXML()), typeXSLT);
				
				response = sendPost(transformed, typeXSLT);
			}
			else
			{// Post con solo xml y adjuntos
				_log.trace("Post with xml and attachtments");
				if (_log.isDebugEnabled() && Validator.isNull(xmlDom))
				{
					_log.debug("xmlDom is null");
				}
				ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlDom));

				response = sendPost(xmlDom.asXML(), "xml");
			}

		}
		else
		{// Post con request
			_log.trace("Post with request");
			if (Validator.isNull(request))
			{
				_log.error("request is null");
			}
			ErrorRaiser.throwIfFalse(Validator.isNotNull(request));
			
			response = sendPost();
		}

		// Si la respuesta no es un 200 o 206levantamos excepcion
		ErrorRaiser.throwIfFalse(responseStatusOk.contains(response), IterErrorKeys.XYZ_E_NOT_CORRECT_RESPONSE_ZYX);
	}

	private int sendPost() throws ServiceError 
	{
		int result = 500;
		HttpClient client = null;
		HttpResponse httpResponse = null;
		
		try{
			client = new DefaultHttpClient();
			HttpPost post = new HttpPost(serverURL);
			//seteamos el contenido
			post.setEntity(new ByteArrayEntity(IOUtils.toByteArray(request.getInputStream())));
			//seteamos las cabeceras recibidas
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements())
			{
				String headerName = headerNames.nextElement();
				if(!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)){
					if(!post.containsHeader(headerName)){
						post.addHeader(headerName, request.getHeader(headerName));
					}else{
						post.setHeader(headerName, request.getHeader(headerName));
					}
				}
			}
			
			// Enviamos el post y nos quedamos con el codigo de respuesta.
			httpResponse = client.execute(post);
			result = processResponse(httpResponse);
		}catch(Throwable th){
			_log.error(th);
			ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}finally{
			if(Validator.isNotNull(client)){
				// Cerramos la conexion
				client.getConnectionManager().shutdown();
			}
		}
		
		return result;
	}

	/**
	 * 
	 * @param xmlDom
	 * @param adjuntos
	 * @param xslid
	 * @param request
	 * @param serverURL
	 * @return
	 * @throws ParseException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws FileUploadException
	 * @throws ServiceError
	 */
	// Funcion principal que realiza el envio de post a un servidor
	private int sendPost(String content, String extension) throws ParseException, ClientProtocolException, IOException, FileUploadException, ServiceError
	{
		int result = HttpStatus.SC_INTERNAL_SERVER_ERROR;
		HttpClient client = null;
		HttpResponse httpResponse = null;
		try{
			client = new DefaultHttpClient();
			// Indicamos el protocolo a usar
			client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	
			HttpPost post = new HttpPost(serverURL);
			// El post sera de tipo multiparte
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			
			String contentName = new StringBuffer(xmlDom.getRootElement().attributeValue("formname", "form")).append(".").append(extension).toString();
			// Lo "almacenamos" al post
			ContentBody cb = new ByteArrayBody(content.getBytes(), MimeTypeTools.getMimeType(extension), contentName);
			entity.addPart(contentName , cb);
	
			// "Adjuntamos" los binarios
			if (Validator.isNotNull(adjuntos) && !adjuntos.isEmpty())
			{
				java.util.Iterator<Entry<String, ArrayList>> it = adjuntos.entrySet().iterator();
	
				while (it.hasNext())
				{
					Map.Entry<String, ArrayList> e = (Map.Entry<String, ArrayList>) it.next();
					ArrayList aL = (ArrayList) e.getValue();
					// Nombre original del fichero
					// String nameAttachment = (String)aL.get(0);
					String nameAttachment = (String) e.getKey();
					String nameFile = (String) aL.get(0);
					byte[] attachmentValue = (byte[]) aL.get(1);
					// Creamos un contentBody
					cb = new ByteArrayBody(attachmentValue, MimeTypeTools.getMimeTypeFileName(nameFile), nameFile);
					// Lo "almacenamos" al post
					entity.addPart(nameAttachment, cb);
				}
			}
	
			// Aniadimos los parametros y archivos al post
			post.setEntity(entity);
			// Enviamos el post y nos quedamos con el codigo de respuesta.
			httpResponse = client.execute(post);
			result = processResponse(httpResponse);
		}catch(Throwable th){
			_log.error(th);
			ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}finally{
			if(Validator.isNotNull(client)){
				// Cerramos la conexion
				client.getConnectionManager().shutdown();
			}
		}
		return result;
	}

	
	private int processResponse(HttpResponse httpResponse) throws ParseException, IOException
	{
		int responseCode = 500;
		if(httpResponse != null){
			
			// Codigo de respuesta
			responseCode = httpResponse.getStatusLine().getStatusCode();
			// Cuerpo de la respuesta
			String responseBody = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
			
			if(responseStatusOk.contains(responseCode))
			{//ok
				_log.debug(new StringBuffer("Received response code: ").append(Integer.toString(responseCode)));
				_log.debug("received response: " + responseBody);
			}
			else
			{//error
				_log.error(new StringBuffer("Received response code: ").append(Integer.toString(responseCode)));
				_log.error("received response: " + responseBody);
			}
		}else{
			_log.error("Received response: null"); 
		}
		
		return responseCode;
	}
	
	public String replaceVarsForm(String text, Document xml)
	{
		_log.trace(new StringBuffer().append("into replaceVarsForm replacing in ").append(text).toString());
		if (Validator.isNull(text))
		{
			return "";
		}
		Map<String, String> ctxVars = new HashMap<String, String>();

		ctxVars.put("deliveryid", formReceivedId);
		ctxVars.put("delivery-id", formReceivedId);

		Element xmlElement = xml.getRootElement();
		ctxVars.put("formid", xmlElement.attributeValue("formid", ""));
		ctxVars.put("form-id", xmlElement.attributeValue("formid", ""));
		ctxVars.put("formname", xmlElement.attributeValue("formname", ""));
		ctxVars.put("form-name", xmlElement.attributeValue("formname", ""));
		Node deliveryInfoNode = xmlElement.selectSingleNode("/formdata/delivery-info");
		if (Validator.isNotNull(deliveryInfoNode))
		{
			ctxVars.put("datesent", deliveryInfoNode.valueOf("date-sent"));
			ctxVars.put("date-sent", deliveryInfoNode.valueOf("date-sent"));
			Node sendingUserNode = deliveryInfoNode.selectSingleNode("sending-user");

			// El usuario esta logado
			if (Validator.isNotNull(sendingUserNode))
			{
				ctxVars.put("usrid", sendingUserNode.valueOf("usrid"));
				ctxVars.put("usrname", sendingUserNode.valueOf("usrname"));
				ctxVars.put("usremail", sendingUserNode.valueOf("usremail"));
				ctxVars.put("usr1stname", sendingUserNode.valueOf("usr1stname"));
				ctxVars.put("usrlastname", sendingUserNode.valueOf("usrlastname"));
				ctxVars.put("usrlastname2", sendingUserNode.valueOf("usrlastname2"));

				// Usuario anonimo
			}
			else
			{
				ctxVars.put("usrid", "");
				ctxVars.put("usrname", "");
				ctxVars.put("usremail", "");
				ctxVars.put("usr1stname", "");
				ctxVars.put("usrlastname", "");
				ctxVars.put("usrlastname2", "");
			}
		}

		return ContextVariables.replaceCtxVars(text, ctxVars);
	}
}