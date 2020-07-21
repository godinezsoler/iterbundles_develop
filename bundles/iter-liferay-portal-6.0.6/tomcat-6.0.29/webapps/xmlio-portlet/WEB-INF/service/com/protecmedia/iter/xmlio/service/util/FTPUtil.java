package com.protecmedia.iter.xmlio.service.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.util.PortalUtil;

import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;

public class FTPUtil {
	
	private static Log _log = LogFactoryUtil.getLog(FTPUtil.class);

    /**
     * Recibe un fichero desde un servidor FTP remoto
     * @param server
     * @param username
     * @param password
     * @param remote
     * @param local
     * @throws XMLIOException 
     */
	public static String receiveFile(String server, String username, String password, String remoteFileName, String localPath, String remoteFileRegex) throws Exception
	{
		return ftpClientAction(server, username, password, remoteFileName, localPath, remoteFileRegex, false, true);
	}
	
	/**
	 * Envía un fichero a un servidor FTP remoto
	 * @param server
	 * @param username
	 * @param password
	 * @param remote
	 * @param local
	 * @throws XMLIOException 
	 */
	public static void sendFile(String server, String username, String password, String remotePath, String localPath, String localFileRegex) throws Exception
	{
		ftpClientAction(server, username, password, remotePath, localPath, localFileRegex, true, true);
	}
	
	/**
	 * Método obtenido de los ejemplos de Apache.Commons.Net 
	 * @param server
	 * @param username
	 * @param password
	 * @param remote
	 * @param local
	 * @param storeFile
	 * @param binaryTransfer
	 * @param error
	 * @throws ServiceError 
	 */
	private static String ftpClientAction(
			String server, 
			String username, 
			String password,
			String remote, 
			String local,
			String regex,
			boolean storeFile,
			boolean binaryTransfer) throws Exception
    {
		String result = "";
        FTPClient ftp = new FTPClient();
        
        String ftpHost	= "";
        int ftpPort 	= 0;
        String ftpPath 	= "";
        
        if(!server.contains("ftp://"))
        	server = "ftp://" + server;
        
        try
        {
            int reply;
            
            //Get URL from String like ftp://localhost:8926/path
            URL url = new URL(server);
            ftpHost = url.getHost();
            ftpPort = url.getPort();
            if(ftpPort == -1) ftpPort = FTP.DEFAULT_PORT;
            ftpPath = url.getPath();
    
            ftp.connect(ftpHost, ftpPort);

            _log.info("Connected to " + server + ".");

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();
            ErrorRaiser.throwIfFalse(FTPReply.isPositiveCompletion(reply), IterErrorKeys.XYZ_E_FTP_REFUSED_CONNECTION_ZYX);
            ErrorRaiser.throwIfFalse(ftp.login(username, password), IterErrorKeys.XYZ_E_FTP_LOGIN_FAILED_ZYX);
            
            _log.info("Remote system is " + ftp.getSystemName());

            ftp.changeWorkingDirectory(ftpPath);
            
            if (binaryTransfer)
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

            /*
             * Apache recomienda el uso del modo pasivo en FTP porque es el más común
             * 
             * No obstante, por cuestiones de seguridad se permite via portal-ext.properties
             * configurar el modo activo
             * 
             */
            String ftpModeProperty = PortalUtil.getPortalProperties().getProperty("iter.ftp.mode");
            if (ftpModeProperty != null && ftpModeProperty.equalsIgnoreCase("active"))
            {
            	ftp.enterLocalActiveMode();
            	_log.info("FTP mode set to active");
            }
            else
            {
                // Use passive mode as default because most of us are
                // behind firewalls these days.
                ftp.enterLocalPassiveMode();
            }

            // Upload
            if (storeFile)
            {
                InputStream input;
                input = new FileInputStream(local);
                Boolean success = ftp.storeFile(remote, input);
                input.close();
                ErrorRaiser.throwIfFalse(success, IterErrorKeys.XYZ_E_FTP_STORE_FILE_FAILED_ZYX, "From "+local+" to "+remote);
                
				try
				{
					// Se intena borrar si todo hasta el momento ha ido OK. El borrado del fichero NO es tan importante como para ser elevado
				 	FileUtil.delete(local);
				 	_log.info("Local file removed");
				}
				catch(Exception err)
				{
					_log.error("Cannot remove local file", err);
				}
				
				result = remote;
            }
            // Download
            else
            {
                String [] remoteFileNames = ftp.listNames();
         
                // Si no tenemos nombre de fichero remoto, se busca el primero que coincida con la expresion regular
                if (remote==null || remote.equals(""))
                {
	                for (String remoteFileName : remoteFileNames)
	                {
	                	if (remoteFileName.matches(regex))
	                	{
	                		remote = remoteFileName;
	                		break;
	                	}
	                }
                }
                
                ErrorRaiser.throwIfNull(remote, IterErrorKeys.XYZ_E_FTP_REMOTE_FILE_NOT_FOUND_ZYX);
                ErrorRaiser.throwIfFalse(!remote.equals(""), IterErrorKeys.XYZ_E_FTP_REMOTE_FILE_NOT_FOUND_ZYX);
                result = local + "/" + remote;
                
            	OutputStream output;
            	output = new FileOutputStream(result);
            	Boolean success = ftp.retrieveFile(remote, output);
            	output.close();
            	ErrorRaiser.throwIfFalse(success, IterErrorKeys.XYZ_E_FTP_RETRIEVE_FILE_FAILED_ZYX, "From "+remote+" to "+result);
            	
				try
				{
					// Se intena borrar si todo hasta el momento ha ido OK. El borrado del fichero NO es tan importante como para ser elevado
					ftp.deleteFile(remote);
					_log.info("Remote FTP file removed");
				}
				catch(Exception err)
				{
					_log.error("Cannot remove remote FTP file", err);
				}
            }
        }
        finally
        {
        	if (ftp.isConnected())
            {
            	try
            	{
            		ftp.logout();
                }
                catch (IOException f){}

                try
                {
                    ftp.disconnect();
                }
                catch (IOException f){}
            }
        } 
        return result;
    }
	
	public static boolean checkFiles(String server, String username, String password, String regExp) throws Exception
	{
		String remote = "";
		
		FTPClient ftp = new FTPClient();
        
        String ftpHost	= "";
        int ftpPort 	= 0;
        String ftpPath 	= "";
        
        if(!server.contains("ftp://"))
        	server = "ftp://" + server;
        
        
        try
        {
            int reply;
            
            
            //Get URL from String like ftp://localhost:8926/path
            URL url = new URL(server);
            ftpHost = url.getHost();
            ftpPort = url.getPort();
            if(ftpPort == -1) ftpPort = FTP.DEFAULT_PORT;
            ftpPath = url.getPath();
    
            ftp.connect(ftpHost, ftpPort);

            _log.info("Connected to " + server + ".");

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();
            ErrorRaiser.throwIfFalse(FTPReply.isPositiveCompletion(reply), IterErrorKeys.XYZ_E_FTP_REFUSED_CONNECTION_ZYX);
            ErrorRaiser.throwIfFalse(ftp.login(username, password), IterErrorKeys.XYZ_E_FTP_LOGIN_FAILED_ZYX);
            
            _log.info("Remote system is " + ftp.getSystemName());

            ftp.changeWorkingDirectory(ftpPath);
            
            /*
             * Apache recomienda el uso del modo pasivo en FTP porque es el más común
             * 
             * No obstante, por cuestiones de seguridad se permite via portal-ext.properties
             * configurar el modo activo
             * 
             */
            String ftpModeProperty = PortalUtil.getPortalProperties().getProperty("iter.ftp.mode");
            if (ftpModeProperty != null && ftpModeProperty.equalsIgnoreCase("active"))
            {
            	ftp.enterLocalActiveMode();
            	_log.info("FTP mode set to active");
            }
            else
            {
                // Use passive mode as default because most of us are
                // behind firewalls these days.
                ftp.enterLocalPassiveMode();
            }
            
            String [] remoteFileNames = ftp.listNames();
            // Si no tenemos nombre de fichero remoto, se busca el primero que coincida con la expresion regular
            for (String remoteFileName : remoteFileNames)
            {
            	if (remoteFileName.matches(regExp))
            	{
            		remote = remoteFileName;
            		break;
            	}
            }
        }
        finally
        {
        	if (ftp.isConnected())
            {
                try
                {
                    ftp.disconnect();
                }
                catch (IOException f){}
            }
        	try
        	{
        		ftp.logout();
            }
            catch (IOException f){}
        }
        
        return remote!="";
	}
	
}