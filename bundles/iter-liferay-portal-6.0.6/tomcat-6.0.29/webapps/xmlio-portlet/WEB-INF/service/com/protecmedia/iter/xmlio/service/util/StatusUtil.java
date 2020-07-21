package com.protecmedia.iter.xmlio.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Channel;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.model.LiveControl;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

public class StatusUtil {
	
	public static String getStatus(){
		
		StringBuilder sb = new StringBuilder();
		
		appendHeader(sb);
		appendBlock(sb, "Server status", getServerStatus());
		appendBlock(sb, "System configuration", getServerConfiguration());
		appendBlock(sb, "Iter configuration", getIterConfiguration());
		appendBlock(sb, "Xmlio configuration", getXmlioConfiguration());
		appendBlock(sb, "Xmlio channel configuration", getXmlioChannelConfiguration());
		appendBlock(sb, "Xmlio status", getXmlioStatus());
		appendBlock(sb, "File version status", getFileStatus());
		
		return sb.toString();
	}
	
	protected static String getServerStatus(){
		
		StringBuilder sb = new StringBuilder();
		
		appendKeyValue(sb, "Server Uptime", PortalUtil.getUptime().toString());
		
		//Memory
		Runtime runtime = Runtime.getRuntime();
		appendKeyValue(sb, "Server Total Memory (Bytes)", String.valueOf(runtime.totalMemory()));
		appendKeyValue(sb, "Server Free Memory (Bytes)", String.valueOf(runtime.freeMemory()));
		appendKeyValue(sb, "Server Used Memory (Bytes)", String.valueOf(runtime.totalMemory() - runtime.freeMemory()));
		appendKeyValue(sb, "Server Max Memory (Bytes)", String.valueOf(runtime.maxMemory()));
		return sb.toString();
		
	}
	
	protected static String getServerConfiguration(){
	
		StringBuilder sb = new StringBuilder();
		
		appendKeyValue(sb, "Server Address", PortalUtil.getComputerAddress());
		appendKeyValue(sb, "Server Name", PortalUtil.getComputerName());
		appendKeyValue(sb, "Server Path Main", PortalUtil.getPathMain());
		appendKeyValue(sb, "Server Liferay Lib Dir", PortalUtil.getPortalLibDir());
		appendKeyValue(sb, "Server Liferay Web Dir", PortalUtil.getPortalWebDir());
		appendKeyValue(sb, "Server Global Lib Dir", PortalUtil.getGlobalLibDir());
		
		return sb.toString();
		
	}
	
	protected static String getIterConfiguration(){

		StringBuilder sb = new StringBuilder();
		
		try{
			appendKeyValue(sb, "Iter System Name", IterLocalServiceUtil.getSystemName());
			appendKeyValue(sb, "Iter Environment", IterLocalServiceUtil.getEnvironment());
			appendKeyValue(sb, "Iter Version", IterLocalServiceUtil.getLastIter().getVersion());
		}
		catch(Exception err){
			appendError(sb, "Cannot get Iter Configuration", err.getMessage());
		}
		
		return sb.toString();
		
	}
	
	protected static String getXmlioConfiguration(){
		
		StringBuilder sb = new StringBuilder();
		
		try {
			LiveConfiguration lc;
			List<LiveConfiguration> lcList = LiveConfigurationLocalServiceUtil.getLiveConfigurations(0, 1);
			if (lcList != null && lcList.size() > 0){
				lc = lcList.get(0);
				appendKeyValue(sb, "Xmlio Live Remote Iter Server", 	lc.getRemoteIterServer2());
				appendKeyValue(sb, "Xmlio Live Gateway Host", 			lc.getGatewayHost());
				appendKeyValue(sb, "Xmlio Live Local Path", 			lc.getLocalPath());
				appendKeyValue(sb, "Xmlio Live Output Method", 			lc.getOutputMethod());
				appendKeyValue(sb, "Xmlio Live Destination Type", 		lc.getDestinationType());
				appendKeyValue(sb, "Xmlio Live Remote Path", 			lc.getRemotePath());
				appendKeyValue(sb, "Xmlio Live Remote User", 			lc.getRemoteUserName());
				appendKeyValue(sb, "Xmlio Live Remote Company Id", 		String.valueOf(lc.getRemoteCompanyId()));
				appendKeyValue(sb, "Xmlio Live Remote Global Group Id", String.valueOf(lc.getRemoteGlobalGroupId()));
				appendKeyValue(sb, "Xmlio Live Connection Timeout", 	String.valueOf(lc.getConnectionTimeOut()));
				appendKeyValue(sb, "Xmlio Live Operation Timeout", 		String.valueOf(lc.getOperationTimeOut()));
				appendKeyValue(sb, "Xmlio Ftp Server", 					lc.getFtpPath());
				appendKeyValue(sb, "Xmlio Ftp User", 					lc.getFtpUser());
			}
			else{
				appendError(sb, "Xmlio is not configured", "");
			}
		}
		catch(Exception err){
			appendError(sb, "Cannot get Xmlio Configuration", err.getMessage());
		}
		return sb.toString();
	}
	
	protected static String getXmlioStatus(){
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)){
		
			StringBuilder sb = new StringBuilder();
		
			//Live Control Status
			try{
				appendTitle(sb, "Live lastest publication process status");
				LiveControl lc = LiveControlLocalServiceUtil.getAllLiveControl().get(LiveControlLocalServiceUtil.getLiveControlsCount()-1);
				appendKeyValue(sb, "Process Id", lc.getProcessId());
				appendKeyValue(sb, "Process Type", lc.getType());
				appendKeyValue(sb, "Process Status", lc.getStatus());
				appendKeyValue(sb, "Process Group Id",  String.valueOf(lc.getGroupId()));
				appendKeyValue(sb, "Process Start Date", lc.getStartDate().toString());
				appendKeyValue(sb, "Process End Date", lc.getEndDate().toString());
				appendKeyValue(sb, "Process Operations", String.valueOf(lc.getOperations()));
				appendKeyValue(sb, "Process File Size", String.valueOf(lc.getFileSize()));
				appendKeyValue(sb, "Process Failed Operations", String.valueOf(lc.getErrors()));
				appendKeyValue(sb, "Process Error Log", lc.getErrorLog());
			}catch(Exception err){
				appendError(sb, "Cannot get Live Lastest publication process status", err.getMessage());
			}
			
			//Live Item errors
			try{
				appendTitle(sb, "Live publication error items (Item className|Group|localId=ErrorLog)");
				
				List<Live> liveList = LiveLocalServiceUtil.getLiveByStatus(IterKeys.ERROR);
				for (Live live : liveList){
					appendKeyValue(sb, live.getClassNameValue() + "|" + live.getGroupId() + "|" + live.getLocalId(),  live.getErrorLog());
				}
			}catch(Exception err){
				appendError(sb, "Cannot get Live publication error items", err.getMessage());
			}
			
			return sb.toString();
		}
		
		return "";
		
	}

	protected static String getXmlioChannelConfiguration(){
		
		StringBuilder sb = new StringBuilder();
		
		//XMLIO Channel Configuration
		try{

			List<Channel> channelList = ChannelLocalServiceUtil.getAllChannels();
			
			for (Channel channel : channelList){
				
				appendKeyValue(sb, "Channel Name", channel.getName());
				appendKeyValue(sb, "\tChannel Type ",  channel.getType());
				appendKeyValue(sb, "\tChannel Active ", "" + channel.getStatus());
				
			}
			
		}catch(Exception err){
			appendError(sb, "Cannot get Xmlio Automatic Channel Configuration", err.getMessage());
		}
		
		return sb.toString();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static String getFileStatus(){

		StringBuilder sb = new StringBuilder();
			
		try {
			//Global Dir --> .JARs. File Name
			appendTitle(sb, "Iter and Liferay ext libraries");
			File globalLibDir = new File(PortalUtil.getGlobalLibDir());
			Collection<File> filesCol = FileUtils.listFiles(globalLibDir, new WildcardFileFilter("*.jar"), TrueFileFilter.INSTANCE);
			
			if(filesCol != null && filesCol.size() > 0)
			{
				List<File> filesList = new ArrayList(filesCol);
				Collections.sort(filesList, new SortFileIgnoreCase());
				for (File file : filesList){
					appendValue(sb, file.getName());
				}
			}
		}catch(Exception err){
			appendError(sb, "Cannot get Iter and Liferay ext libraries", err.getMessage());
		}
		
		try {
			//Web Dir --> .WARs. Liferay-Plugin-Package.properties Content
			appendTitle(sb, "Iter and Liferay plugins");
			File portalWebDir = new File(PortalUtil.getPortalWebDir()).getParentFile();
			Collection<File> filesCol = FileUtils.listFiles(portalWebDir, new WildcardFileFilter("liferay-plugin-package.properties"), TrueFileFilter.INSTANCE);
			
			if(filesCol != null && filesCol.size() > 0)
			{
				List<File> filesList = new ArrayList(filesCol);
				Collections.sort(filesList, new SortFileIgnoreCase());
				for (File file : filesList){
					try{
						Properties properties = new Properties();
						properties.load(new FileInputStream(file.getAbsolutePath()));
						appendValue(sb, file.getParentFile().getParentFile().getName() + "-" 
								+ properties.getProperty("liferay-core-version") + "-" 
								+ properties.getProperty("module-incremental-version") + ".war");
					}
					catch(Exception err){
						appendError(sb, "Cannot get Iter and Liferay plugin data from " + file.getName(), err.getMessage());
					}
				}
			}
		}catch(Exception err){
			appendError(sb, "Cannot get Iter and Liferay plugin package properties", err.getMessage());
		}
		
		//TODO: portal-ext.properties, otros?
		try {
			//Liferay base Dir
			appendTitle(sb, "Iter and Liferay extended properties");
			File serverBaseDir = new File(PortalUtil.getGlobalLibDir()).getParentFile().getParentFile().getParentFile();
			//Se obtiene una lista y se ordena, ya que en Unix listFiles no obtiene los archivos alfabéticamente
			//Como la clase File implementa el interface Comparable, podemos usar Collections.sort. 
			//Según la documentación de File, la comparación se realiza alfabéticamente, aunque en Unix se tienen en cuenta también las mayúsculas en la ordenación
			List<File> fileList = new ArrayList<File>(FileUtils.listFiles(serverBaseDir, new WildcardFileFilter("portal-ext.properties"), FalseFileFilter.INSTANCE));
			Collections.sort(fileList);
			
			for (File file : fileList){

				try{
					appendFileContent(sb, FileUtils.readFileToString(file));
				}
				catch(Exception err){
					appendError(sb, "Cannot read " + file.getName(), err.getMessage());
				}
			}
		}
		catch(Exception err){
			appendError(sb, "Cannot get Iter and Liferay extended properties", err.getMessage());
		}
		
		return sb.toString();

	}

	
	//Util methods
	protected static void appendHeader(StringBuilder sb){
		sb.append("\n==== ITER WEB CMS STATUS " + new Date() + " ====\n");
	}
	
	protected static void appendBlock(StringBuilder sb, String title, String block){
		sb.append("\n\n---- ").append(title.toUpperCase()).append(" ----\n").append(block);
	}
	
	protected static void appendTitle(StringBuilder sb, String title){
		sb.append("\n\n---- ").append(title).append(" ----\n");
	}
	
	protected static void appendError(StringBuilder sb, String error, String detail){
		sb.append("\nERROR: ").append(error).append(".SOURCE:").append(detail);
	}
	
	protected static void appendValue(StringBuilder sb, String value){
		sb.append("\n").append(value);
	}
	
	protected static void appendKeyValue(StringBuilder sb, String key, String value){
		sb.append("\n").append(key).append("=").append(value);
	}
	
	protected static void appendFileContent(StringBuilder sb, String fileContent){
		sb.append("\n\n---- File Content ----\n").append(fileContent);
	}
	
    public static class SortFileIgnoreCase implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
        	File f1 = (File) o1;
        	File f2 = (File) o2;
        	String fName1 = f1.getAbsolutePath();
        	String fName2 = f2.getAbsolutePath();
        	int result = fName1.toLowerCase().compareTo(fName2.toLowerCase());
            return result;
        }
    }

}
