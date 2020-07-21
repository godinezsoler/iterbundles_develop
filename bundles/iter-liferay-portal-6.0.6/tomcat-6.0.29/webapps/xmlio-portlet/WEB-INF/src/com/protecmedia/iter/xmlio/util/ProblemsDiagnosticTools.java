package com.protecmedia.iter.xmlio.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;



public class ProblemsDiagnosticTools extends Thread
{
	private static Log _log = LogFactoryUtil.getLog(ProblemsDiagnosticTools.class);
	
	private boolean _captureOnlyLogs    = false;
	private String _catFilepath 		= "";
	private String _grepFilepath 		= "";
	private String _startDateFilepath 	= "";
	private String _command 			= "";
	public ArrayList<Throwable> excepcionList = null;
	
	/* Cadena que contiene los nombres de los archivos, separados por comas, generados en la importación. 
	 La cadena sólo se rellena si el hilo se está ejecutando en el entorno PREVIEW, esto es porque el nombre de esos ficheros solo aparecen en el log del catalina del preview*/
	private String _impFilesNames = "";
	
	
	private Process captureDataProcess;
	
	private File _catalinaFile;
	private File _grepFile;
	private File _startDateFile;
	
	
	public ProblemsDiagnosticTools( boolean captureOnlyLogs, String catfilepath, String grepFilepath, String startDateFilepath ,String command, ArrayList<Throwable> excepcionList )
	{
		_captureOnlyLogs    = captureOnlyLogs;
		_catFilepath 		= catfilepath;
		_grepFilepath 		= grepFilepath;
		_startDateFilepath 	= startDateFilepath;
		_command 	 		= command;
		this.excepcionList	= excepcionList;
	}
	
	@Override
	public void run() 
	{
		initProcess();
 
	}
	
	private void initProcess()
	{
		try
		{
			// Determinar en qué SO estamos
		    String so = System.getProperty("os.name");
		    ErrorRaiser.throwIfFalse( so.equals("Linux") , IterErrorKeys.XYZ_ITR_E_FUNCTIONALITY_NOT_AVAILABLE_ZYX);
		    
		    String s = null;
				
		    _catalinaFile = new File(_catFilepath);		
				
		    BufferedWriter output_cat = new BufferedWriter(new FileWriter(_catalinaFile));	
				
		    BufferedWriter output_filesc 	= null;
			
		    if ( !_captureOnlyLogs  && PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW))
		    {
		    	_grepFile = new File(_grepFilepath);
		    	output_filesc = new BufferedWriter(new FileWriter(_grepFile));
		    	
		    	createStartDateFile();
		    	
		    }

		    	//Se ejecuta el comando
		        captureDataProcess = Runtime.getRuntime().exec(_command);
		       
		        if(_log.isDebugEnabled())
		        	_log.debug("launch command 'tail -F' 'catalina.out'");
		                 

		        BufferedReader stdInput = new BufferedReader(new InputStreamReader(
		            		captureDataProcess.getInputStream()));

		        BufferedReader stdError = new BufferedReader(new InputStreamReader(
		            		captureDataProcess.getErrorStream()));
		            
		        Boolean firstLine = true;
		        
		        if(_log.isDebugEnabled())
		        	_log.debug("reading exit of 'tail -F' command");
		        
		        while ((s = stdInput.readLine()) != null) 
		        {
		           
		            //escribir la salida en archivo
			        output_cat.write(s);
			        output_cat.newLine();
			            	
			       String strtofind_package		= "Local file created at";
			       String strtofind_subpackage	= "Processing sub-package";
			            	
			       //si en PREVIEW se encuentra la cadena 'Local file created at', escribir en archivo 'grepFile'
			        if( !_captureOnlyLogs && (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW) )  && output_filesc != null )
			        {
			        	if(s.contains(strtofind_package) || s.contains(strtofind_subpackage) )
			        	{
			        		 output_filesc.write(s);
					         output_filesc.newLine();
			        	}
			        	else
			        		checkContainsImpFileName(s);
			        }
			        if(firstLine)
			        {
			        	if(_log.isDebugEnabled())
					        _log.debug("First line of file 'catalina.out' written in "+ _catFilepath);
			        	
			           firstLine = false;
			           notifyException(null);
			        }
		       }
		           	
		       output_cat.close();
		       if(output_filesc != null)
		           output_filesc.close();
						
		       try
		       {
		    	   /* Leemos los errores si los hubiera.
		    	   Tras destruir el proceso, los streams del proceso se han cerrado, por lo que stdError.readLine() producirá IOException*/
			       while ( (s = stdError.readLine()) != null) 
			       {
			            _log.error(s);
			       }
		       }
		       catch (IOException e) 
			   {
		    	   if(_log.isDebugEnabled())
		    		   _log.debug("The buffered reader connected to the error output of the subprocess is closed");
			   }

		      //cerrar streams del proceso
		      stdInput.close();
		      stdError.close();
		      captureDataProcess.getOutputStream().close();
		    	
		}
		catch (Throwable th)
		{
			//se añade el error y se notifica para salir del wait
			notifyException(th);
		}
	}
	
	//Libera el wait. Además, en caso de que se haya producido alguna excepción, la añade a la lista.
	private void notifyException(Throwable th)
	{
    	synchronized(excepcionList)
    	{
    		if(th != null)
    			excepcionList.add(th);
    		
    		excepcionList.notify();
    	}
	}
	
	//Si se encuentra el nombre de algún archivo de importación se añade a la lista '_impFilesNames'
	public void checkContainsImpFileName(String str)
	{
		String filename = "";
		//
    	Matcher pat_jtmp = Pattern.compile("iter_JournalTemplates[\\d]+.xml").matcher(str);
    	Matcher pat_lytmp = Pattern.compile("iter_LayoutTemplates[\\d]+.xml").matcher(str);
    	Matcher pat_wthemes = Pattern.compile("iter_WebThemes[\\d]+.xml").matcher(str);
    	
    	if( pat_jtmp.find() )
    		filename = pat_jtmp.group();
    	else if( pat_lytmp.find() )
    		filename = pat_lytmp.group();
    	else if(pat_wthemes.find())
    		filename = pat_wthemes.group();
    	
    	if(!filename.equals(""))
    		addImpFileName(filename);
	}
	
	//rellena '_impFilesNames'
	private void addImpFileName( String filename)
	{
		
    	if(!_impFilesNames.equals("") && !_impFilesNames.contains(filename))
    		_impFilesNames = _impFilesNames.concat("," + filename);
        else if( !_impFilesNames.contains(filename) )
        	_impFilesNames = _impFilesNames.concat( filename );
	}
	
	private void createStartDateFile() throws IOException, ServiceError
	{
		_startDateFile = new File(_startDateFilepath);
    	BufferedWriter output_startDate = new BufferedWriter(new FileWriter(_startDateFile));	
    	output_startDate.write(SQLQueries.getCurrentDate());
    	output_startDate.close();
	}
	
	
	public void stopProcess() throws IOException
	{
		if(captureDataProcess != null)
			captureDataProcess.destroy();
		
	}
	
	public void deleteFiles()
	{
		if(_catalinaFile != null && _catalinaFile.exists() )
			_catalinaFile.delete();
		if(_grepFile != null && _grepFile.exists() )
			_grepFile.delete();	
		if(_startDateFile != null  && _startDateFile.exists())
			_startDateFile.delete();
	}
	
	public File getCatalinaFile()
	{
		return _catalinaFile;
	}
	
	public File getGrepFile()
	{
		return _grepFile;
	}
	
	public File getStartDateFile()
	{
		return _startDateFile;
	}
	
	public String getImpFilesNames()
	{
		return _impFilesNames;
	}

}
