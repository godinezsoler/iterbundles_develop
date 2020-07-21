package com.protecmedia.iter.user.util.forms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
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
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil;

public class HandlerFileForm  extends Thread{
	
	private static Log _log = LogFactoryUtil.getLog(HandlerFileForm .class);
	
	private boolean processdata;
	private Document xmlDom;
	private String xslid;
	private HttpServletRequest request;
	private String folderName;
	private String fileName;
	private Map adjuntos;
	private List<Throwable> exceptionsHandlers;
	private String formReceivedId;
	private Semaphore semaphore;
	private long groupId;


	// Obtenemos el separador del sistema (normalmente "/" o "\")
	static final String separator = System.getProperty("file.separator");
	
	public HandlerFileForm(long groupId, boolean processdata, Document xmlDom, Map<String, ArrayList> adjuntos, String xslid, HttpServletRequest request,  String folderName, String fileName, String formReceivedId, List<Throwable> exceptionsHandlers, Semaphore semaphore) 
	{
		this.groupId = groupId;
		this.xmlDom = SAXReaderUtil.createDocument();
		this.xmlDom.setRootElement(xmlDom.getRootElement().createCopy());   
		this.xslid 				= xslid;              
		this.request 			= request;
		this.formReceivedId		= formReceivedId;
		this.adjuntos 			= adjuntos;
		this.exceptionsHandlers = exceptionsHandlers;
		this.semaphore 			= semaphore;
		this.processdata 		= processdata;
		this.folderName 		= replaceVarsForm(folderName, xmlDom);
		// El nombre del archivo no puede llevar barras
		this.fileName 			= replaceVarsForm(fileName,   xmlDom).replaceAll("/", "_").replaceAll("\\\\", "_");
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
			IterMonitor.logEvent(this.groupId, Event.ERROR, new Date(System.currentTimeMillis()), "Form handler file error", "", th);
			_log.error(th);
			if(Validator.isNotNull(exceptionsHandlers))
			{
				try 
				{
					semaphore.acquire();
					exceptionsHandlers.add(th);
					_log.trace("throw Exception from thread HandlerFileForm to FormReceiver (critic HandlerFileForm)");
					semaphore.release();
				}
				catch (InterruptedException e1) 
				{
				}
			}
			else
				_log.trace("no critic HandlerFileForm");
		}
	}


	private void localRun() throws ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException, TransformerException, DocumentException, IOException {
		// Document
		if(processdata){
			//Se cambia tipo de adjunto en el xml y se cambia su referencia
			if(Validator.isNotNull(adjuntos)){
				List<Node> binarysNodes = SAXReaderUtil.createXPath("/formdata/fieldsgroup/field/data/binary").selectNodes(xmlDom);
				for (Node binaryNode : binarysNodes) {
					Element binaryElement = (Element) binaryNode; 
					Element binlocatorElement = binaryElement.element("binlocator");
					binlocatorElement.attribute("type").setValue("file");
					String keyIdImg= binlocatorElement.getText();
					ErrorRaiser.throwIfNull(keyIdImg);
					String nameFile =  binaryElement.element("name").getText();
//					String nameFile = (String) ((ArrayList) adjuntos.get(keyIdImg)).get(0);
					ErrorRaiser.throwIfNull(nameFile);
					String completeNameFile = buildNameFile(formReceivedId, keyIdImg, this.fileName, nameFile);
					binlocatorElement.setText(completeNameFile);
				}
			}
			// Transformacion
			if(xslid != null)
			{
				long delegationId = GroupLocalServiceUtil.getGroup(this.groupId).getDelegationId();
				InputStream isXSL = DLFileEntryLocalServiceUtil.getFileAsStreamByUuid(xslid, delegationId);
				ErrorRaiser.throwIfNull(isXSL);

				Element dataRoot = SAXReaderUtil.read(isXSL).getRootElement();
				String typeXSLT = XMLHelper.getTextValueOf(dataRoot, "/xsl:stylesheet/xsl:output/@method", null);
				if(Validator.isNull(typeXSLT)){
					_log.trace("XSL not contain 'xsl:output/@method'. default: xml");
					typeXSLT = "xml";
				}
				
				String transformed = XSLUtil.transform(XSLUtil.getSource(xmlDom.asXML()), XSLUtil.getSource(dataRoot.asXML()), typeXSLT);
				ErrorRaiser.throwIfNull(transformed);
				toFile(this.fileName, this.folderName, transformed.getBytes(), new StringBuffer(".").append(typeXSLT).toString(), this.adjuntos);								
				
			// XML		
			}else{	
				toFile(this.fileName, this.folderName,  xmlDom.asXML().getBytes(), ".xml", this.adjuntos);
			}
		// Request	
		}else{	

			ErrorRaiser.throwIfNull(request);
			toFile(this.fileName, this.folderName, IOUtils.toByteArray(this.request.getInputStream()), ".bin", this.adjuntos);
		}
	}
	
	final static String XPATH_BINLOCATOR = "/formdata/fieldsgroup/field/data/binary/binlocator[contains(text(),'%s')]";
	
	/**
	 * 
	 * @param formName
	 * @param pathFiles
	 * @param content
	 * @param extension
	 * @param adjuntos
	 * @return
	 * @throws ServiceError
	 * @throws IOException
	 * @throws DocumentException
	 */
	public boolean toFile(String formName, String pathFiles, byte[] content, String extension, Map<String, ArrayList> adjuntos) throws ServiceError, IOException, DocumentException{
		
		if (Validator.isNull(pathFiles)){
			_log.debug("Path file is null");
		}
		ErrorRaiser.throwIfNull(pathFiles);
		
		boolean ok = true;
		
		writeFile(buildPathFile(separator, pathFiles, buildNameFile(formReceivedId, SQLQueries.getUUID(), formName, extension)), content);			
		
		// Guardamos los adjuntos	
		if (Validator.isNotNull(adjuntos) && !adjuntos.isEmpty()){
			for (Entry<String,ArrayList> entry : adjuntos.entrySet()) 
			{
				StringBuffer keybinlocator = new StringBuffer().append(formReceivedId).append("_").append(entry.getKey()).append("_");
				Node binlocator = SAXReaderUtil.createXPath(String.format(XPATH_BINLOCATOR, keybinlocator)).selectSingleNode(xmlDom);
				ErrorRaiser.throwIfNull(binlocator);
				
				byte[] attachmentValue = (byte[]) entry.getValue().get(1);
				ErrorRaiser.throwIfNull(attachmentValue);

				if (!writeFile(buildPathFile(separator, pathFiles,binlocator.getText()), attachmentValue)){
					ok = false;
				}else{
					_log.trace(new StringBuffer("File writed in disk: ").append(binlocator.getText()));
				}
			}		
		}
		
		return ok;	
	}
	
	/**
	 * 
	 * @param filePath
	 * @param dataFile
	 * @return
	 * @throws ServiceError
	 * @throws IOException
	 */
	// Escribe un archivo en disco
	private boolean writeFile(String filePath, byte[] dataFile) throws ServiceError, IOException{
		ErrorRaiser.throwIfNull(filePath);
		ErrorRaiser.throwIfNull(dataFile);
		
		boolean ok = false;
		
		FileOutputStream fileOuputStream = null;
		
		try{
			File f = new File(filePath);
			
			// Si no existe el arbol de directorios, se crea.
			if (!f.exists()){				
				f.getParentFile().mkdirs();
			}
			
			fileOuputStream = new FileOutputStream(filePath); 
			ErrorRaiser.throwIfNull(fileOuputStream);		
		
			fileOuputStream.write(dataFile);
			
			ok = true;
			
		}catch(Exception e){			
			String text = new StringBuffer("Error writing the file in disk. Invalid route or name: ").append(filePath).toString();
			_log.error(text);
			ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_E_INVALID_FILE_PATH_ZYX, text);
		}finally{
			if (Validator.isNotNull(fileOuputStream)){
				fileOuputStream.close();
			}
		}
		
		return ok;	
	}
	
	// Compone la ruta del archivo: binlocator_UUID_nombrePuestoDesdeIterAdmin_nombreArchivoSubido.extension 
	/**
	 * @param separator
	 * @param originalPath
	 * @param completeNameFile
	 * @return
	 * @throws ServiceError
	 */
	private static String buildPathFile(String separator, String originalPath, String completeNameFile) throws ServiceError{
		
		ErrorRaiser.throwIfNull(separator);
		ErrorRaiser.throwIfNull(originalPath);
		
		// Formamos la ruta completa donde se guardara
		String finalPath = new StringBuffer()
							.append(originalPath)
							.append((originalPath.endsWith(separator)) ? "" : separator)
							.append(completeNameFile)
							.toString();
		
		// Si la ruta no comienza por barra la ruta es invalida, se la ponemos. En produccion NUNCA va a correr en un windows.
		if (finalPath.charAt(0) != '/'){
			finalPath = new StringBuffer("/").append(finalPath).toString();
		}
		return finalPath;
	}
	
	private static String buildNameFile(String formreceivedid, String uuid, String formName, String filename_extension) throws ServiceError{
		_log.trace("In buildNameFile");
		
		ErrorRaiser.throwIfNull(formreceivedid);
		ErrorRaiser.throwIfNull(formName);
		ErrorRaiser.throwIfNull(filename_extension);
		
		/* El nombre de los ficheros deben tener el siguiente formato: 
		 		ruta/delivery-id_id-unico_nombreArchivo.extension	 		
		   Ademas puede contener variables de contexto del sistema que seran sustituidas por su correspondiente valor.
		   Lista de variables del sistema: http://10.15.20.59:8090/pages/viewpage.action?pageId=21987358
		 */
		
		StringBuffer completeNameFile = new StringBuffer()
		// Identificador del formulario recibido
		.append(formreceivedid).append("_");
		
		if(Validator.isNotNull(uuid)){
			// Identificador unico
			completeNameFile.append(uuid).append("_");
		}
		
		// ¿?
		completeNameFile.append(formName)
		// Punto
		.append((filename_extension.startsWith(".")) ? "" :"_")
		// Extension
		.append(filename_extension);	
		
		return completeNameFile.toString();
	}


	/**
	 * 
	 * @param text
	 * @return
	 */
	public String replaceVarsForm(String text, Document xml){
		_log.trace(new StringBuffer().append("into replaceVarsForm replacing in ").append(text).toString());
		if(Validator.isNull(text)){
			return "";
		}
		Map<String, String> ctxVars = new HashMap<String, String>();

		ctxVars.put("deliveryid",	formReceivedId);
		ctxVars.put("delivery-id",	formReceivedId);
		
		Element xmlElement = xml.getRootElement();
		ctxVars.put("formid", 	xmlElement.attributeValue("formid", ""));
		ctxVars.put("form-id", 	xmlElement.attributeValue("formid", ""));
		ctxVars.put("formname", 	xmlElement.attributeValue("formname", ""));
		ctxVars.put("form-name", 	xmlElement.attributeValue("formname", ""));
		Node deliveryInfoNode = xmlElement.selectSingleNode("/formdata/delivery-info");
		if(Validator.isNotNull(deliveryInfoNode)){
			ctxVars.put("datesent",		deliveryInfoNode.valueOf("date-sent"));
			ctxVars.put("date-sent",	deliveryInfoNode.valueOf("date-sent"));
			Node sendingUserNode = deliveryInfoNode.selectSingleNode("sending-user");
			
			// El usuario esta logado
			if(Validator.isNotNull(sendingUserNode)){
				ctxVars.put("usrid",			sendingUserNode.valueOf("usrid"));
				ctxVars.put("usrname",			sendingUserNode.valueOf("usrname"));
				ctxVars.put("usremail",			sendingUserNode.valueOf("usremail"));
				ctxVars.put("usr1stname",		sendingUserNode.valueOf("usr1stname"));
				ctxVars.put("usrlastname",		sendingUserNode.valueOf("usrlastname"));
				ctxVars.put("usrlastname2",		sendingUserNode.valueOf("usrlastname2"));
				
			// Usuario anonimo
			}else{
				ctxVars.put("usrid",			"");
				ctxVars.put("usrname",			"");
				ctxVars.put("usremail",			"");
				ctxVars.put("usr1stname",		"");
				ctxVars.put("usrlastname",		"");
				ctxVars.put("usrlastname2",		"");
			}
		}

		return ContextVariables.replaceCtxVars(text, ctxVars);
	}
	
}