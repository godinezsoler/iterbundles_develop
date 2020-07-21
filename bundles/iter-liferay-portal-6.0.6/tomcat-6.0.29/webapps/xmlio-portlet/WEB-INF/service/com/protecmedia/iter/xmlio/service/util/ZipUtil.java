package com.protecmedia.iter.xmlio.service.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class ZipUtil 
{
	private static Log _log = LogFactoryUtil.getLog(ZipUtil.class);
	private static final int BUFFER = 1024;
	
	public static void unzip(File zipFile, File destDir) throws Exception 
	{
		int BUFFER = 2048;

		ZipFile zip = new ZipFile(zipFile);
		Enumeration zipFileEntries = zip.entries();
	    
		// Process each entry
		while (zipFileEntries.hasMoreElements()) 
		{
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
		    String currentEntry = entry.getName();
		    
		    File destFile = new File(destDir, currentEntry);

		    if (!entry.isDirectory()) 
		    {
			    mkFileDirs(destFile);

		    	try
		    	{
		            BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
		            int currentByte;
		            // establish buffer for writing file
		            byte data[] = new byte[BUFFER];
		         
		            // write the current file to disk
		            FileOutputStream fos = new FileOutputStream(destFile);
		            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
		
		            // read and write until last byte is encountered
		            while ((currentByte = is.read(data, 0, BUFFER)) != -1) 
		            {
		            	dest.write(data, 0, currentByte);
		            }
		            dest.flush();
		            dest.close();
		            is.close();
		    	}		    	
		    	catch(Exception err)
		    	{
		    		_log.error(err);
		    		throw new Exception("Error extracting the file: '" + zipFile.getAbsolutePath() + "', entry: '" + entry.getName() +"', error: '"+ err.toString() + "'");
		    	}
		    }
		}
	}
	
	private static void mkFileDirs(File file) throws Exception
	{
		try
		{
			File fileDir = file.getParentFile();
			
			//Obtiene la ruta
			ArrayList<File> pathDirList = new ArrayList<File>();
			while(!fileDir.exists())
			{
				pathDirList.add(0, fileDir);
				fileDir = fileDir.getParentFile();
			}
			
			//Genera los directorios
			for (File pathDir : pathDirList)
			{
				pathDir.mkdir();
			}
		}
		catch(Exception err)
		{
			_log.error("Error extracting file: "+ file.getName() +": "+ err.toString());
			throw err;
		}
	}
	
}
