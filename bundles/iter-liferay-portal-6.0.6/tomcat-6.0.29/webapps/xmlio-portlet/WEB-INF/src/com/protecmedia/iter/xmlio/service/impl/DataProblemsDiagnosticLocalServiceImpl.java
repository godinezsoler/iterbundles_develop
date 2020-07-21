/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.xmlio.service.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.Timer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.base.DataProblemsDiagnosticLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.PublishUtil;
import com.protecmedia.iter.xmlio.util.ProblemsDiagnosticTools;


/**
 * The implementation of the data problems diagnostic local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticLocalServiceUtil} to access the data problems diagnostic local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.DataProblemsDiagnosticLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DataProblemsDiagnosticLocalServiceImpl extends DataProblemsDiagnosticLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(DataProblemsDiagnosticLocalServiceImpl.class);
	
	private static Lock _lock 				= new ReentrantLock();
	
	ProblemsDiagnosticTools pdtThread; 
	
	AtomicBoolean atomicBoolean_captData = new AtomicBoolean(false);
	AtomicBoolean atomicBoolean_captLogs = new AtomicBoolean(false);
	
	private Document domXmlioLiveConfig;
	private Timer timer;
	
	
	private void lockProcess(String errCode) throws ServiceError
	{
		ErrorRaiser.throwIfFalse( _lock.tryLock(), errCode );
	}
	
	private void unlockProcess()
	{
		_lock.unlock();
	}
	
	public String getStateCaptureProcess() throws DocumentException
	{
		Element rootelem = SAXReaderUtil.read("<rs/>").getRootElement();
		
		String capDataState = atomicBoolean_captData.get() ?  "capturing" : "stopped";
		String capLogsState = atomicBoolean_captLogs.get() ?  "capturing" : "stopped";
		rootelem.addAttribute("capdatastate", capDataState);
		rootelem.addAttribute("caplogsstate", capLogsState);
		
		return rootelem.asXML();	
	}
	
	
	/* ***************************************
	 * INICIAR CAPTURA DE DATOS
	 *************************************** */
	
	/*Inicia los procesos de captura de datos en los entornos PREVIEW y LIVE.
		Este método es invocado desde IterAdmin para el entorno PREVIEW */
	public void captureData(String onlyLogs) throws Throwable
	{
		//se comprueba que no hay un proceso de captura de datos en curso
		ErrorRaiser.throwIfFalse( !atomicBoolean_captData.get() &&  !atomicBoolean_captLogs.get(), IterErrorKeys.XYZ_E_DATA_CAPTURE_ALREADY_IN_PROCESS_ZYX  );
		lockProcess(IterErrorKeys.XYZ_E_DATA_CAPTURE_ALREADY_IN_PROCESS_ZYX);		
		
		try
		{
			String catPrevFileName = "catalina.preview.out";
			String catLiveFileName = "catalina.live.out";
				
			//se lanza el proceso de captura de datos en PREVIEW
			startCaptureData(onlyLogs, catPrevFileName);
				
			//se lanza el proceso de captura de datos en LIVE
			startCaptureDataRemote(onlyLogs, catLiveFileName);
			
			if(onlyLogs.equals("true"))
				atomicBoolean_captLogs.set(true);//iniciada ok la captura de datos     
			else
				atomicBoolean_captData.set(true);//iniciada ok la captura de datos     
			
			//se inicia el cronómetro para parar la captura en un día(si no se ha parado antes)
			startTimerCaptureProcess();
		}
		finally
		{
			unlockProcess();
		}
	}
	
	public void startCaptureData(String onlyLogs, String catFileName) throws Throwable
	{
		
		String mcmPath = getMcmPath();
	
		String grepFilePath    		= mcmPath + File.separatorChar + "nameFilesCreated.txt";
		String catFilePath 			= mcmPath + File.separatorChar + catFileName;
		String startDateFilePath  	= mcmPath + File.separatorChar + "fechainicio.txt";
		
		String command 	= "tail -F /liferay/tomcat-6.0.29/logs/catalina.out";
		
		//prueba local, lanzar comando date
		//String command 	= "cmd /c date";
		
		ArrayList<Throwable> excepcionList = new ArrayList<Throwable>();	
		
		pdtThread  = new ProblemsDiagnosticTools( Boolean.parseBoolean(onlyLogs), catFilePath, grepFilePath, startDateFilePath ,command, excepcionList);
		pdtThread.start();
	
		//////////////
		/*esperar a que el hilo notifique para seguir con la ejecución. Se pueden producir alguna de estas situaciones:
			1. Si pdtThread.excepcionList.size()  = 0 --> el hilo lanzado ha llegado a la acción que tiene que realizar(quedarse capturando los datos del log 'catalina.out')
			2. Si pdtThread.excepcionList.size()  > 0 --> se ha producido una excepción en el hilo lanzado*/
		synchronized(pdtThread.excepcionList)
		{
			if(excepcionList.size() == 0)
				pdtThread.excepcionList.wait();
        }
	
		if (pdtThread.excepcionList.size() > 0)
			throw pdtThread.excepcionList.get(0);
		
	}
	
	private void startCaptureDataRemote(String onlyLogs, String catLiveFileName) throws Throwable
	{
		try
		{
			String remoteMethodPath 	= "/xmlio-portlet/secure/json";
			
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"startCaptureData"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[onlyLogs,catFileName]"));
			remoteMethodParams.add(new BasicNameValuePair("onlyLogs", 			onlyLogs));
			remoteMethodParams.add(new BasicNameValuePair("catFileName", 		catLiveFileName));
			
			JSONUtil.executeMethod(remoteMethodPath, remoteMethodParams);
		}
		catch(Exception e)
		{
			//El proceso en entorno LIVE no se ha iniciado, se para el proceso iniciado en PREVIEW
			stopProcess();
			cleanData();
			
			throw e;
		}
	}
	
	/* ***************************************
	 * PARAR CAPTURA DE DATOS
	 *************************************** */
	
	/*Para los procesos de captura de datos en los entornos PREVIEW y LIVE y genera un .zip con los archivos generados.
		Este método es invocado desde IterAdmin para el entorno PREVIEW */
	public void stopCaptureData(HttpServletRequest request, HttpServletResponse response) throws Throwable
	{
	
		//se controla que haya un proceso de captura de datos en curso
		ErrorRaiser.throwIfFalse( atomicBoolean_captData.get() || atomicBoolean_captLogs.get(),  IterErrorKeys.XYZ_E_NOT_EXISTS_DATA_CAPTURE_IN_PROCESS_ZYX ); 
		lockProcess(IterErrorKeys.XYZ_E_NOT_EXISTS_DATA_CAPTURE_IN_PROCESS_ZYX);		
		
		try
		{
			
			//se para el proceso de captura de datos en PREVIEW
			stopProcess();
			
			//se para el proceso de captura de datos en LIVE
			stopProcessRemote();
			
			if(pdtThread.excepcionList.size() > 0)
			{
				Throwable e = pdtThread.excepcionList.get(0);
				cleanAllData();
				throw e;	
			}
			
			if(Validator.isNotNull(response))
			{
				try
				{
					byte[] data  = getCapturedData();
					
					//Se escribe el ZIP en el buffer de salida
					OutputStream output = response.getOutputStream();
					output.write(data);
					output.flush();
				}
				catch (Exception e) 
				{
					cleanData();
					throw e;
				}
			}	
		}
		finally
		{
			atomicBoolean_captData.set(false);
			atomicBoolean_captLogs.set(false);
			
			if(Validator.isNotNull(response))
				timer.stop();
			else
				cleanAllData();
			
			unlockProcess();
		}
	}
	
	public void stopProcess() throws Throwable
	{
		pdtThread.stopProcess();
		pdtThread.join();	
		
		//Se evita lanzar una excepción producida en el PREVIEW hasta que se haya parado el proceso en el LIVE
		if( PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_LIVE) )
		{
			if(pdtThread.excepcionList.size() > 0)
				throw pdtThread.excepcionList.get(0);
		}	
	}
	
	private void stopProcessRemote() throws Exception
	{
		try
		{
			String remoteMethodPath 	= "/xmlio-portlet/secure/json";
			
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"stopProcess"));
			
			JSONUtil.executeMethod(remoteMethodPath, remoteMethodParams);
		}
		catch(Exception e)
		{
			//Se añade, a la lista de excepciones, la excepción producida al parar el proceso arrancado en el entorno LIVE.
			pdtThread.excepcionList.add(e);	
		}
	}
	
	
	/* ***************************************
	 * OBTENER LOS DATOS CAPTURADOS
	 *************************************** */
	
	
	
	private byte[] getCapturedData() throws Exception
	{
		byte[] data;
		
		//nombres( separados por ',' de los paquetes '.zip' generados en la importación
		String namesPkgImportation   = "";
		
		if(atomicBoolean_captData.get())
		{
			//Se obtienen los nombres de los paquetes '.zip' generados en la importación
			namesPkgImportation = getNamesPackagesImportation();
				if(_log.isDebugEnabled())
				{
					if(Validator.isNotNull(namesPkgImportation) )
						_log.debug("Importation packages names: " + namesPkgImportation);
					else
						_log.debug("Not found importation packages");
				}		
		}
			
		//Invocacion remota del método createZipLiveData que generará el archivo 'liveData.zip' en la carpeta mcm del LIVE
		createRemoteZip(namesPkgImportation, pdtThread.getImpFilesNames());
	
		InputStream liveDataZipIS  = getLiveDataZip();
		
		//Se crea el '.zip' descargable
		File zipFile = createZipToDownload(liveDataZipIS, namesPkgImportation);
		
		// Se escribe el ZIP en el buffer de salida
		FileInputStream inputStream = new FileInputStream(zipFile);
		data = IOUtils.toByteArray(inputStream);
		
		inputStream.close();
//		zipFile.setWritable(true);
		
		FileUtils.deleteQuietly(zipFile);
		
		return data;	
	}
	
	private InputStream getLiveDataZip() throws Exception
	{
		InputStream catLiveFileIS = null;
		
		if(domXmlioLiveConfig == null)
			domXmlioLiveConfig = JSONUtil.getLiveConfiguration();
	 	String remoteIterServer = JSONUtil.getLiveConfRemoteIterServer(domXmlioLiveConfig);
	 	int conexionTimeout = JSONUtil.getLiveConfConexionTimeout(domXmlioLiveConfig);
	 	int responseTimeout = JSONUtil.getLiveResponseTimeout(domXmlioLiveConfig);
	 	
	 	String []url 		= remoteIterServer.split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		
	 	URL liveServiceURL 			 = new URL( targetHost.toString() + "/mcm/liveData.zip");
        URLConnection connection 		 = liveServiceURL.openConnection();
        HttpURLConnection httpConnection = (HttpURLConnection)connection;
        httpConnection.setRequestMethod("POST"); 
        httpConnection.setConnectTimeout( conexionTimeout );
        httpConnection.setReadTimeout( responseTimeout );
	    httpConnection.setRequestProperty("Content-Type", "text/xml"); 
	    httpConnection.setDoOutput(true);
	    httpConnection.setDoInput(true);
	    
		if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			catLiveFileIS = httpConnection.getInputStream();
		}
		else
		{
			httpConnection.disconnect();
			cleanAllData();
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_UNABLE_TO_OBTAIN_DATA_LIVE_ZYX);	
		}
		
		return catLiveFileIS;
	}
	
	//Crea y retorna un .zip con los archivos que se han generado en el proceso de captura de datos
	private File createZipToDownload( InputStream liveDataZipIS, String namesPkgImportation ) throws Exception
	{
		File zipFile;
		
        ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter();     
		
        addFileToZip(pdtThread.getCatalinaFile(),"catalina.preview.out", zipWriter );
        
        if( atomicBoolean_captData.get() )
        	addFileToZip(pdtThread.getStartDateFile(),"fechainicio.txt", zipWriter );
		
		//Se añade al .zip el zip del live
		zipWriter.addEntry("liveData.zip", liveDataZipIS);
		liveDataZipIS.close();
		
		if( Validator.isNotNull(namesPkgImportation) )
			addToZipPkgFoundInPrev(zipWriter, namesPkgImportation);
			
		cleanAllData();
		
		zipFile = PublishUtil.getUnlockedFile(zipWriter);
		return zipFile;
	}
	
	private void addToZipPkgFoundInPrev(ZipWriter zipWriter,  String namesPkgImportation ) throws NoSuchMethodException, SecurityException, IOException
	{
		if(domXmlioLiveConfig == null)
			domXmlioLiveConfig = JSONUtil.getLiveConfiguration();
		String import_localPath = JSONUtil.getLocalPath(domXmlioLiveConfig);
		
		addFilesToZip( namesPkgImportation, import_localPath +  File.separatorChar , zipWriter);
	}
	
	private void addFileToZip( File file, String filename, ZipWriter zipWriter ) throws IOException
	{
		try
		{
			InputStream fileIS = new FileInputStream(file);
			zipWriter.addEntry(filename, fileIS);
			fileIS.close();
		}
		catch(FileNotFoundException e)
		{
			if(_log.isDebugEnabled())
				_log.debug("Not found the file: " + file.getAbsolutePath());
		}
	}
	
	private String getMcmPath() throws ServiceError
	{
		 String mcmPath = "";
		 File rootDir = new File(PortalUtil.getPortalWebDir());
	     ErrorRaiser.throwIfNull(rootDir);
	        
	     File webappsDir = rootDir.getParentFile();
	     ErrorRaiser.throwIfNull(webappsDir);
	        
	     String webappsPath =  webappsDir.getAbsolutePath();
	     ErrorRaiser.throwIfNull(webappsPath);
	     
	     mcmPath = new StringBuilder(webappsPath).append(File.separatorChar).append("mcm").toString();
	     return mcmPath;
	}
	
	private String getNamesPackagesImportation() throws IOException
	{
		String names= "";
		String s = null;
		
		Pattern pat = Pattern.compile("iter_[\\d]+.zip");
		
		BufferedReader reader_grepfile = new BufferedReader(new FileReader(pdtThread.getGrepFile()));
		
		//se busca el patrón 'iter_xxxxxx.zip' para obtener el nombre o nombres de archivos generados en la importación
		while ((s = reader_grepfile.readLine()) != null) 
	    {
			Matcher mat = pat.matcher(s);
			if(mat.find())
			{
				String name = mat.group();
				if(_log.isDebugEnabled())
					_log.debug("Found import package with name: "+ name );
				names = names.concat( name ).concat(",");	
			}
	    }
		
		reader_grepfile.close();
		
		//se elimina el caracter ',' que sobra al final
		if(Validator.isNotNull(names))
			names = names.substring(0, names.length() -1);	

		return names;
	}
	
	private void createRemoteZip( String namesPkgImportation, String nameImpFiles ) throws JSONException, ClientProtocolException, SystemException, SecurityException, NoSuchMethodException, IOException, ServiceError
	{
		String remoteMethodPath 	= "/xmlio-portlet/secure/json";
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"createZipLiveData"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[namesPkgImportation, nameImpFiles]"));
		remoteMethodParams.add(new BasicNameValuePair("namesPkgImportation", namesPkgImportation));
		remoteMethodParams.add(new BasicNameValuePair("nameImpFiles", nameImpFiles));
		JSONUtil.executeMethod(remoteMethodPath, remoteMethodParams);
	}
	
	/* Se ejecuta en el entorno LIVE. 
	 * Dados todos los nombres de los archivos generados en la importación, crea, en la carpeta mcm, un '.zip' con nombre 'liveData.zip' que contiene todos los archivos que se encuentren en el LIVE. */
	public void createZipLiveData( String namesPkgImportation, String nameImpFiles  ) throws Exception
	{
		ZipWriter zipWriter = null;
		try
		{
			String liveDataZipPath = getMcmPath() + File.separatorChar + "liveData.zip";
			
			deleteLiveDataZip(liveDataZipPath);
			zipWriter = ZipWriterFactoryUtil.getZipWriter(new File(liveDataZipPath)); 
			
			domXmlioLiveConfig  = JSONUtil.getLiveConfiguration();
			String import_localPath = JSONUtil.getLocalPath(domXmlioLiveConfig);
			
			//Se añade a 'liveData.zip' el log del catalina('catalina.live.out')
			File catFileLive = new File( getMcmPath() +  File.separatorChar + "catalina.live.out");
			addFileToZip(catFileLive ,"catalina.live.out", zipWriter );
			
			//Se añade a 'liveData.zip' los paquetes '.zip' de la importación que se encuentren en LIVE(si la publicación falló esos paquetes no estarán)
			if(namesPkgImportation != null)
			{
				addFilesToZip( namesPkgImportation, import_localPath +  File.separatorChar + "imported" + File.separatorChar, zipWriter);
			}
			//Se añade a 'liveData.zip' los ficheros(iter_JournalTemplatesxxx.xml,iter_LayoutTemplatesxxx.xml,iter_WebThemesxxx.xml )
			if(nameImpFiles != null)
			{

				addFilesToZip( nameImpFiles, import_localPath +  File.separatorChar, zipWriter);	
			}
		
			// Se desbloquea el zip
			File liveZipFile = PublishUtil.getUnlockedFile(zipWriter);
			
		}
		catch (Exception e) 
		{
			File liveZipFile = PublishUtil.getUnlockedFile(zipWriter);
			cleanData();
			throw e;
		}
	

	}

	//procesa los nombres de los archivos y los añade al .zip
	private void addFilesToZip(String namesFiles, String pathname, ZipWriter zipWriter  ) throws IOException
	{
		String[]namesArray = namesFiles.split(",");
		for(int i = 0; i < namesArray.length; i++ )
		{
			String namefile = namesArray[i].toString();
			File impfile = new File( pathname + namefile);
			
			if(_log.isDebugEnabled())
				_log.debug("adding the file " + "'"+impfile.getAbsolutePath() +"'" + " to zip");
			 
			addFileToZip(impfile ,namefile, zipWriter );
		}
	}

	/* ***********************************************************
	 * Borrado de ficheros creados en el proceso de captura de datos
	 ************************************************************* */
	public void cleanData() throws ServiceError
	{
		if(pdtThread != null)
		{
			pdtThread.deleteFiles();
			pdtThread = null;
		}
		
		if( PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_LIVE) )
		{
			deleteLiveDataZip(getMcmPath() + File.separatorChar + "liveData.zip");
		}	
	}
	
	/* Elimina los ficheros y variables utilizadas en los entornos preview y live */
	private void cleanAllData() throws Exception
	{
		cleanData();
		
		String remoteMethodPath 	= "/xmlio-portlet/secure/json";
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"cleanData"));
		JSONUtil.executeMethod(remoteMethodPath, remoteMethodParams);
	}
	
	private void deleteLiveDataZip(String liveDataZipPath) throws ServiceError
	{
		File liveDataZip = new File( liveDataZipPath );
		liveDataZip.delete();
	}
	
	/* *****************************************************
	 * Tiempo máximo para el proceso de captura de datos
	 ******************************************************* */
	private void startTimerCaptureProcess()
	{
		timer = new Timer( 86400000, new ActionListener() 
		{
			public void actionPerformed(ActionEvent ev) 
			{
				timer.stop();
				
				//se ha agotado el tiempo máximo(1 día). Se para el proceso de captura de datos
				_log.info("Time out for data capture process");
				abortDataCapture(); 	
			}
		} ) ; 
		
		timer.start();
	}
	
	//Aborta el proceso de captura de datos en caso de que se lanzase el proceso de captura y tras 24 horas no se haya parado
	private void abortDataCapture() 
	{
		try
		{
			stopCaptureData(null, null);
		}
		catch(Throwable th)
		{
			_log.debug(th); 
		}	
	}
	
}