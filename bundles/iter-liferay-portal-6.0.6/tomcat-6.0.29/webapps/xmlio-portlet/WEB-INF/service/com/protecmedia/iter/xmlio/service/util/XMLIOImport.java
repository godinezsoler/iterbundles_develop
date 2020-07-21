/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.FileUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class XMLIOImport {

	private static Log _log = LogFactoryUtil.getLog(XMLIOImport.class);
	public static ThreadLocal<String> filePath = new ThreadLocal<String>();
	private static ThreadLocal<Boolean> tempDir = new ThreadLocal<Boolean>();
	public static ThreadLocal<String> importedFile = new ThreadLocal<String>();
		
	public static String getFile(String file) 
	{	
		return filePath.get() + StringPool.SLASH + file;
	}
		
	public static String getFileAsString(String file, String encoding) throws IOException
	{
		String fileStr = "";
	    try 
	    {
	    	fileStr = FileUtils.readFileToString(new File(getFile(file)), encoding);
	    } 
	    catch (IOException e) 
	    {
	    	String msg = "Error reading file '"+ file +"' with encoding '"+ encoding+"': ";
	    	_log.error(msg + e.toString());
	    	throw e;
	    }
	    return fileStr;
	}
	
	public static byte[] getFileAsBytes(String file)  throws IOException
	{
		byte[] result = null;
		try
		{
			result = FileUtil.getBytes(new File(getFile(file)));
		}
		catch(IOException e)
		{
			String msg = "Error getting bytes from file '"+ file +"': ";
	    	_log.error(msg + e.toString());
	    	throw e;
		}
		return result;
	}

	public static String getMainFile() 
	{
		return getFile(IterKeys.XMLIO_XML_MAIN_FILE_NAME);
	}
	
	public static void init(File file) throws Exception
	{
		init(file, null);
	}

	public static void init(File file, File xslFile) throws Exception
	{
		ErrorRaiser.throwIfFalse(FileUtil.exists(file), IterErrorKeys.XYZ_E_IMPORT_FILE_NOT_FOUND_ZYX);
		
		tempDir.set(false);
		File initFile;
		
		// Si es directorio, se busca primero el xml y si no el zip
		do
		{
			if (file.isDirectory())
			{
				XMLIOImport.filePath.set(file.getAbsolutePath());
				if (FileUtil.exists(getMainFile()))
				{
					break;
				}
				
				initFile = XMLIOUtil.getFileByExt(file, ".zip");
				if (initFile!=null)
				{
					XMLIOImport.importedFile.set(initFile.getAbsolutePath());
					processZipFile(initFile, xslFile);
					break;
				}
				
				initFile = XMLIOUtil.getFileByExt(file, ".xml");
				if (initFile!=null)
				{
					XMLIOImport.importedFile.set(initFile.getAbsolutePath());
					processXmlFile(initFile, xslFile);
					break;
				}
			}
			else if (file.isFile())
			{
				XMLIOImport.filePath.set(file.getParent());
				if (file.getName().toLowerCase().endsWith(".xml"))
				{
					XMLIOImport.importedFile.set(file.getAbsolutePath());
					processXmlFile(file, xslFile);
					break;
				}
				
				if (file.getName().toLowerCase().endsWith(".zip"))
				{
					XMLIOImport.importedFile.set(file.getAbsolutePath());
					processZipFile(file, xslFile);
					break;
				}
			}
		} 
		while (false);
	}
	
	public static void release(boolean keepFile)throws IOException, ServiceError
	{
		release(keepFile, true); 
	}
	public static void release(boolean keepFile, boolean move2ImportedFolder)throws IOException, ServiceError
	{
		if (!keepFile)
		{
			File xmlFile = new File(XMLIOImport.getMainFile());
			xmlFile.delete();
			
			//Movemos el fichero zip a la carpeta imported.
			//Si no existe la carpeta la creamos en el mismo path donde esta el zip
			String importedFile = XMLIOImport.importedFile.get();
			if(importedFile!=null && !importedFile.isEmpty())
			{
				File fileDir = new File(importedFile);
				if(FileUtil.exists(fileDir))
				{
					String dir = fileDir.getParent();
					ErrorRaiser.throwIfNull(dir);
					
					if (move2ImportedFolder)
					{
						//fileDir.getAbsolutePath().substring(0, fileDir.getAbsolutePath().lastIndexOf("\\"));
						File dirFile = new File(dir+ File.separatorChar +"imported");
						dirFile.mkdirs();
						
						XMLIOUtil.copyFile( fileDir, new File(dirFile.getAbsolutePath()+ File.separatorChar +fileDir.getName()) );
					}
					
					//Es necesario ejecutar el recolector de mierda para garantizar que se puede borrar el fichero enel siguiente paso.
					System.gc();
					int deleteAttempts = 0;
					while(!fileDir.delete() && deleteAttempts<10)
					{
						deleteAttempts++;
					}
				}
			}
		}
		
		eraseTempDir();
	}
	
	public static void rename()
	{
		if(XMLIOImport.importedFile.get()!=null && !XMLIOImport.importedFile.get().isEmpty())
		{
			File fileDir = new File(XMLIOImport.importedFile.get());
			File newFile = new File( fileDir.getAbsolutePath()+ "." + new Date().getTime() +".error" );
			System.gc();
			if( !fileDir.renameTo( newFile ) )
				_log.info("Can not rename file " + fileDir.getAbsolutePath());
		}
		
		eraseTempDir();
		
	}
	
	private static void eraseTempDir()
	{
		if (XMLIOImport.tempDir.get())
		{
			File tempDir = new File(XMLIOImport.filePath.get());
			XMLIOUtil.deleteDir(tempDir);
			XMLIOImport.tempDir.set(false);
		}
	}

	private static void processZipFile(File zipFile, File xslFile) throws Exception
	{
		File tempDirectory = XMLIOUtil.createTempDirectory();
		ZipUtil.unzip(zipFile, tempDirectory);
		XMLIOImport.tempDir.set(true);
		XMLIOImport.filePath.set(tempDirectory.getAbsolutePath());
		_log.info("Importing contents from " + zipFile.getAbsolutePath() + "...");
		
		if ( !FileUtil.exists(getMainFile()) )
		{
			File xmlFile = XMLIOUtil.getFileByExt(tempDirectory, ".xml");
			
			ErrorRaiser.throwIfNull(xmlFile, IterErrorKeys.XYZ_E_IMPORT_XML_NOT_FOUND_IN_ZIP_ZYX);
			processXmlFile(xmlFile, xslFile);
		}
	}
	
	private static void processXmlFile(File xmlFile, File xslFile) throws TransformerException, FileNotFoundException, ServiceError
	{
		ErrorRaiser.throwIfNull(xslFile, IterErrorKeys.XYZ_E_IMPORT_XSL_UNAVAILABLE_ZYX);
		
		File mainFile = new File(XMLIOImport.getMainFile());
		XMLIOUtil.transformXsl(xmlFile, mainFile, xslFile);
	}
}