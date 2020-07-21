package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.CDATATools;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class PublicationProxyBack
{
	private static Log _log = LogFactoryUtil.getLog(PublicationProxyBack.class);

	private long	_minPkgSize = 0;
	private String 	_processId	= null;
	private String  _logPrefix  = null;
	
	// http://confluence.protecmedia.com:8090/x/sAPhAg
	private static final long 	DEFAULT_PKG_SIZE = 275000; 
	private static final XPath 	LAYOUT_XPATH 	 = SAXReaderUtil.createXPath(String.format("count(//item[@classname='%s' and (@operation='%s' or @operation='%s')])", IterKeys.CLASSNAME_LAYOUT, IterKeys.CREATE, IterKeys.UPDATE));
	
	public PublicationProxyBack(String processId)
	{
		_minPkgSize = HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_PUBLICATION_PROXY_SPLIT_PKG, DEFAULT_PKG_SIZE);
		_processId  = processId;
		_logPrefix  = String.format("Process: %s", _processId); 
	}
	
	static public Document publish(String processId, String className, File originalPkg) throws Exception
	{
		return new PublicationProxyBack(processId).publish(className, originalPkg);
	}
	
	private Document publish(String className, File pkg) throws Exception
	{
		Document result = null;
		
		if (Validator.isNull(className) && PropsValues.ITER_PUBLICATION_PROXY_SPLIT_PKG)
		{
			result = processAsSubPkg(pkg);
		}
		else
			result = processPkg(pkg);
			
		return result;
	}
	
	/**
	 * 
	 * @param pkg
	 * @return El DOM con la respuesta de publicar todos los elementos en el LIVE, como si se hubiese publicado el de una vez
	 * @throws Throwable 
	 */
	private Document processAsSubPkg(File pkg) throws Exception
	{
		Document result = null;
		try
		{
			// Se descomprime el paquete
			_log.info( String.format("%s Processing de original package (%s) in sub-packages", _logPrefix, pkg.getName()) );
	    	XMLIOImport.init(pkg, null);
	    	File iterXmlFile = new File(XMLIOImport.getMainFile());
	
	    	Element logs 					= null;
	    	Element groups					= null;
	    	Element scheduledPublications 	= null;
			List<File> pkgs = splitPkg(iterXmlFile);
			for (File subPkg : pkgs)
			{
				_log.info( String.format("%s, Processing sub-package %s", _logPrefix, subPkg.getName()) );
				Document subpkgResult = processPkg(subPkg);
				
				if (result == null)
				{
					// Es el primer paquete
					result 					= subpkgResult;
					logs 					= (Element)result.selectSingleNode("iter/logs");
					groups 					= (Element)result.selectSingleNode("iter/groups");
					scheduledPublications	= (Element)result.selectSingleNode("iter/scheduledPublications");
					if (scheduledPublications == null)
						scheduledPublications = result.getRootElement().addElement("scheduledPublications");
				}
				else
				{
					// Se añaden los logs de respuesta
					List<Node> logList = subpkgResult.selectNodes("/iter/logs/item");
					for (Node log : logList)
						logs.add(log.detach());
					
					// Se mezclan los grupos. Si ya existe se actualiza la fecha, si no existe se añade
					List<Node> groupList = subpkgResult.selectNodes("/iter/groups/group");
					for (int i = 0; i < groupList.size(); i++)
					{
						String xpath = String.format("group[@groupId='%s']", ((Element)groupList.get(i)).attributeValue("groupId"));
						Node group = groups.selectSingleNode(xpath);
						if (group == null)
							groups.add(groupList.get(i).detach());
						else
							((Element)group).addAttribute("lastUpdate", ((Element)groupList.get(i)).attributeValue("lastUpdate"));
					}
					
					// Se añade la planificación de publicación de artículos
		    		List<Node> articleScheduledPublications = subpkgResult.selectNodes("/iter/scheduledPublications/publications");
		    		for (Node publi : articleScheduledPublications)
		    			scheduledPublications.add(publi.detach());
				}
				
	    		// Se copian todos los atributos a la respuesta. Es importante para copioar por ejemplo el atributo tpl, 
				// que vendrá en el primer subpaquete publicado que tenga layouts
				List<Attribute> attrs = subpkgResult.getRootElement().attributes();
	    		for (Attribute attr : attrs)
	    			result.getRootElement().addAttribute(attr.getName(), attr.getValue());
			}
			
		   	_log.info( _logPrefix.concat(" Cleaning up resources...") );
		    XMLIOImport.release(false, false);
		}
		catch (Exception e)
		{
			XMLIOImport.rename();
			throw e;
		}
		
		return result;
	}
	
	/**
	 * Divide el paquete en n subpaquetes.
	 * Tendrán un orden en función del tipo de elementos.
	 * El tamaño máximo de los paquetes será configurable, aunque podrá exceder dicho valor si existen dependencias.
	 *
	 * @param pkg
	 * @return
	 * @throws Exception 
	 */
	private List<File> splitPkg(File iterXmlFile) throws Exception
	{
		List<File> pkgs = new ArrayList<File>();
		
		Document doc 	= SAXReaderUtil.read(iterXmlFile);
		Attribute tpls 	= doc.getRootElement().attribute("tpls");
		
		// Se crea el root sin elementos para copiar en cada paquete
		Element root2Copy  		= SAXReaderUtil.createElement(IterKeys.XMLIO_XML_ELEMENT_ROOT);
		List<Attribute> attrs 	= doc.getRootElement().attributes();
		
		// Solo el primer paquete que contenga operaciones de creación/actualización del Layout contendrá los tpls
		for (Attribute attr : attrs)
		{
			if (!attr.getName().equals("tpls"))
				root2Copy.addAttribute( attr.getName(), attr.getValue() );
		}
		
		long scopeGroupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), root2Copy.attribute("scopegroupid").getValue()).getGroupId();
		
		LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(GroupMgr.getCompanyId());
		String import_localPath = liveConf.getLocalPath();
		
		// En la segunda versión se creará un paquete por tipo de pool
		// 	com.liferay.portal.model.Group   
		// 	com.liferay.portlet.asset.model.AssetVocabulary
		// 	com.liferay.portal.model.Layout                   
		// 	com.liferay.portlet.journal.model.JournalArticle  
		for (String poolType : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT_SPLIT_PKG)
		{
			List<Node> pools = doc.selectNodes( String.format("/iter/list/pool[item/@classname='%s']", poolType) );
			addSubPkg(scopeGroupId, pkgs, iterXmlFile, import_localPath, pools, root2Copy, tpls, !poolType.equals(IterKeys.CLASSNAME_LAYOUT));
		}
		
		// Se añaden los pools que por alguna razón no se hayan añadido antes
		addSubPkg(scopeGroupId, pkgs, iterXmlFile, import_localPath, doc.selectNodes("/iter/list/pool"), root2Copy, tpls, true);
		
		return pkgs;
	}
	
	private void addSubPkg(long scopeGroupId, List<File> pkgs, File iterXmlFile, String import_localPath, List<Node> pools, Element root2Copy, Attribute tpls, boolean checkPkgSize) throws Exception
	{
		int i = 0;
		while (i < pools.size())
		{
			long totalBytes = 0;
			Element pkgRoot = SAXReaderUtil.createDocument( root2Copy.createCopy() ).getRootElement();
			Element pkgList = pkgRoot.addElement(IterKeys.XMLIO_XML_ELEMENT_LIST);
			
			XMLIOExport xmlioExport = new XMLIOExport(scopeGroupId);
			
			for (; i < pools.size(); i++)
			{
				Element pool = (Element)((Element)pools.get(i)).detach();
				pkgList.add( pool );
				
				// El primer paquete que contenga operaciones de creación/actualización del Layout contendrá los tpls
				if (tpls != null && Validator.isNotNull(tpls.getValue()) && LAYOUT_XPATH.numberValueOf(pkgList).longValue() > 0)
				{
					pkgRoot.addAttribute(tpls.getName(), tpls.getValue());
					tpls.setValue("");
				}
				
				// Se copian al subpaquete los recursos que referencia y se contabiliza el tamaño de dichos recursos
				totalBytes += copyResources(xmlioExport, pool, iterXmlFile.getParentFile());
				
				// Si el total de recursos más lo que ocupa el iter.xml supera el máximo configurado se inicia un nuevo paqueta
				if (checkPkgSize && (totalBytes + pkgRoot.asXML().getBytes().length) > _minPkgSize)
				{
					i++;
					break;
				}
			}
			
			xmlioExport.setContent(pkgRoot.asXML());
			
			pkgs.add( xmlioExport.generateZip( import_localPath) );
		}
	}
	
	/**
	 * Añade los recursos referencias en el iter.xml al objeto que gestiona el paquete
	 * @param xmlioExport
	 * @param iterXml
	 * @throws IOException 
	 */
	private long copyResources(XMLIOExport xmlioExport, Element pool, File srcFolder) throws IOException
	{
		long totalBytes = 0;
		srcFolder.listFiles();
		
		// Get all the files from a directory
	    File[] fList = srcFolder.listFiles();
	    
		for (File file: fList)
		{
			if (file.isDirectory())
			{
				// Los binarios tienen una estructura específica
				// 	<binaries delegationId="0">
				// 		<binary name="image_content_3731169_20160413152401.jpg" operation="create"/>
				//	</binaries>
				// Solo se indica el nombre pq es una carpeta "bien conocida"
				if (file.getName().equals("binrepository"))
				{
					String xpath = "item/binaries/binary[@operation='create']/@name";
					List<Node> rsrcNodes = pool.selectNodes( xpath );
					for (Node rsrcNode : rsrcNodes)
					{
						String filePath = "/binrepository/".concat(rsrcNode.getStringValue());
						File rsrc 		= new File(srcFolder.getAbsolutePath().concat(filePath));
						byte[] bytes 	= FileUtil.getBytes(rsrc);
						totalBytes 	   += bytes.length;
						xmlioExport.addResource(filePath, bytes);
					}
				}
				else
				{
					// Se localizan todas las referencias a dicha carpeta
					String xpath = String.format("item/param[starts-with(., '<![CDATA[/%s')]", file.getName());
					List<Node> rsrcNodes = pool.selectNodes( xpath );
					for (Node rsrcNode : rsrcNodes)
					{
						String filePath = CDATATools.strip(((Element) rsrcNode).getTextTrim());
						File rsrc 		= new File(srcFolder.getAbsolutePath().concat(filePath));
						byte[] bytes 	= FileUtil.getBytes(rsrc);
						totalBytes 	   += bytes.length;
						xmlioExport.addResource(filePath, bytes);
					}
				}
			}
		}
		
		return totalBytes;
	}
	
	/**
	 * 
	 * @param pkg
	 * @return
	 * @throws Exception
	 */
	private Document processPkg(File pkg) throws Exception
	{
		String info = String.format("%s, pkg %s: ", _logPrefix, pkg.getName()); 
				
		// Recupera la configuracion
		LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(GroupMgr.getCompanyId());
		
		String remoteFilePath = liveConf.getRemotePath().concat("/").concat(pkg.getName());
		
		// Realiza el envio por el procedimiento elegido
		if (liveConf.getOutputMethod().equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM))
		{
			_log.info( info.concat("Sending publish data via file-system...") );
			//Copia el fichero de la carpeta local a la de destino
			File remoteFile = new File(remoteFilePath);
			XMLIOUtil.copyFile(pkg, remoteFile);
			_log.info( info.concat("Sending publish data via file-system completed") );
		}
		else
		{
			_log.info( info.concat("Sending publish data via ftp...") );
			String ftpPath 		= liveConf.getFtpPath();
			String ftpUser 		= liveConf.getFtpUser();
			String ftpPassword 	= liveConf.getFtpPassword();
			FTPUtil.sendFile(ftpPath, ftpUser, ftpPassword, pkg.getName(), pkg.getAbsolutePath(), "");
			_log.info( info.concat("Sending publish data via ftp completed") );
		}
		
		Document result = checkLiveResponse(XMLIOUtil.executeJSONRemoteImportContent(GroupMgr.getCompanyId(), 
				liveConf.getRemoteCompanyId(),  liveConf.getRemoteGlobalGroupId(), 
				liveConf.getRemoteIterServer2(),liveConf.getRemotePath(), 			pkg.getName(), 
				liveConf.getRemoteUserId(), 	liveConf.getRemoteUserName(), 		liveConf.getRemoteUserPassword()));
		
		if (_log.isTraceEnabled())
			_log.trace(result.asXML());
		
		return result;
	}
	
	/**
	 * Método que analiza la respuesta del Live tras la publicación y determina si se produjo o no un error en dicha operación<br/>
	 * 1. La cadena no puede estar vacía.<br/>
	 * 2. No es una cadena vacía pero NO es un XML.<br/>
	 * 3. Es un XML y no es "correcto" (de la forma /iter/logs/item):<br/>
	 *  3.a. Es un XML de error Iter: <response><error code="errCode"><msg/><techinfo/></error></response><br/>
	 *  3.b. Es un XML "correcto" pero NO tiene items.<br/>
	 *	3.c. Es un XML pero NO es conocido<br/>
	 *
	 * @param response
	 * @return
	 * @throws Exception
	 */
	private Document checkLiveResponse(String response) throws Exception
	{
		_log.trace("In formatResultsForFlex.checkLiveResponse");
		
		// 1. La cadena no puede estar vacía
		ErrorRaiser.throwIfFalse(!response.isEmpty(), IterErrorKeys.XYZ_E_PUBRESPONSE_EMPTY_ZYX);
		
		// 2. No es una cadena vacía pero NO es un XML
		Document doc = null;
		try
		{
			doc = SAXReaderUtil.read(response);
		}
		catch (DocumentException e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBRESPONSE_STR_ERROR_ZYX, response);
		}
		
		// 3. Es un XML y no es "correcto" (de la forma /iter/logs/item):
		ErrorRaiser.throwIfNull(doc);
		XPath xpath = SAXReaderUtil.createXPath("count(/iter/logs/item)");
		long numItems = xpath.numberValueOf(doc).longValue();
		
		//	3.a. Es un XML de error Iter: <response><error code="errCode"><msg/><techinfo/></error></response>.
		//		 Se lanza una excepción con dicho mensaje: ServiceErrorUtil.isIterException(doc)
		//  3.b. Es un XML "correcto" pero NO tiene items.
		//	3.c. Es un XML pero NO es conocido
		ErrorRaiser.throwIfFalse(numItems > 0, IterErrorKeys.XYZ_E_PUBRESPONSE_STR_ERROR_ZYX, response);
		
		return doc;
	}

}
