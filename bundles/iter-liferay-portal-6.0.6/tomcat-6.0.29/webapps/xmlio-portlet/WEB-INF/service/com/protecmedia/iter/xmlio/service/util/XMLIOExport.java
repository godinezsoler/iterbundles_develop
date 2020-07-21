package com.protecmedia.iter.xmlio.service.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.protecmedia.iter.base.service.util.IterKeys;


public class XMLIOExport {
	
	private static Log _log = LogFactoryUtil.getLog(XMLIOExport.class);
	
	private String content;
	private Map<String, byte[]> resources = new HashMap<String, byte[]>();
	private Set<String> _tpls = new HashSet<String>();
	private long _groupId = 0;
	
	public XMLIOExport(long groupId)
	{
		_groupId = groupId;
	}
	
	public long getGroup()
	{
		return _groupId;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public Map<String, byte[]> getResources() {
		return this.resources;
	}	
	
	public void addResource(String name, byte[] bytes) {
		this.resources.put(name, bytes);
	}
	
	public void generateFiles(String filePath) throws IOException
	{
		generateFiles(filePath, "");
	}
	
	public void addTpl(String tplId)
	{
		_tpls.add(tplId);
	}
	
	public void generateFiles(String filePath, String xslPath) throws IOException
	{
		File tempDir = XMLIOUtil.createTempDirectory();
		String tempDirPath = tempDir.getAbsolutePath();
		
		String mainFileContent = content;
		
		// Create main File
		FileWriter fstream = new FileWriter(tempDirPath + "/" + IterKeys.XMLIO_XML_MAIN_FILE_NAME );
		BufferedWriter bw = new BufferedWriter(fstream);
		bw.write(mainFileContent);
		bw.close();
		Iterator<Entry<String, byte[]>> it = resources.entrySet().iterator();
	
		while (it.hasNext()) {
			Map.Entry<String, byte[]> resource = (Map.Entry<String, byte[]>) it.next();
			
			if(resource.getKey().contains("/")){
                String path = resource.getKey().substring(0, resource.getKey().lastIndexOf("/"));
                if(!path.equals("")){
                	new File(tempDirPath+"/"+path).mkdirs();
                }
			}
			
			// write the files to the disk
			BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(tempDirPath + "/" + resource.getKey()));
			
			out.write( resource.getValue() );
			out.close();
		}
		
		//If using xsl
		if (!xslPath.equals("") && FileUtil.exists(xslPath))
		{
			try{
				System.out.println(mainFileContent);
				XMLIOUtil.transformXsl(content, xslPath, tempDirPath);
				copyToInputServer(tempDir, filePath);
			}
			catch(Exception err){
				_log.error("XSL transform failed", err);
			}
		}
	}
	
	public File generateZip() throws Exception 
	{
		return generateZip("", "");
	}
	
	public File generateZip(String filePath) throws Exception
	{
		return generateZip(filePath, "");
	}
	
	private void sendPublishableTplInfo() throws Exception
	{
		String templateIds = null;
		if ( Validator.isNotNull(templateIds = StringUtil.merge(_tpls)) && _groupId > 0 && _groupId != GroupMgr.getGlobalGroupId() )
		{
			File publishableFile = LayoutTemplatePublisher.sendPublishableInfo(_groupId, templateIds);
			
			Document dom = SAXReaderUtil.read(content);
			dom.getRootElement().addAttribute("tpls", publishableFile.getName());
			
			content = dom.asXML();
		}
	}
	
	public File generateZip(String filePath, String xslPath) throws Exception
	{
		String iterFileName = IterKeys.XMLIO_XML_MAIN_FILE_NAME;
		String zipFileName	= filePath + "/" + IterKeys.XMLIO_ZIP_FILE_PREFIX + (new Date()).getTime() + ".zip";
		
		File zipFile = new File(zipFileName);
		ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter(zipFile);
		
if(_log.isDebugEnabled())
	_log.debug("ZipWriterFactoryUtil.getZipWriter(zipFile)");

		sendPublishableTplInfo();
		String mainFileContent = content;
		
		// Generate zip File
		zipWriter.addEntry(iterFileName, mainFileContent);

if(_log.isDebugEnabled())
	_log.debug("zipWriter.addEntry");
		
		Iterator<Entry<String, byte[]>> it = resources.entrySet().iterator();
		while (it.hasNext()) 
		{
			Map.Entry<String, byte[]> resource = (Map.Entry<String, byte[]>) it.next();
			
			zipWriter.addEntry(resource.getKey(), resource.getValue());
		}

if(_log.isDebugEnabled())
	_log.debug("zipWriter.addEntry AFTER WHILE");
		

		File zip = zipWriter.getFile();

if(_log.isDebugEnabled())
	_log.debug("zipWriter.getFile()");
				
		
		// Hasta este punto el fichero zip aún no se ha actualizado del todo pq tarda el refresco del de.schlichtherle.io.File.umount (ZipWritterImpl)
		//	- umount(boolean waitInputStreams, boolean closeInputStreams, boolean waitOutputStreams, boolean closeOutputStreams)
        // 		Updates all archive files in the real file system with the contents of their virtual file system, resets all cached state and deletes all temporary files.
		//	- umount(File archive)
        //		Equivalent to umount(archive, false, true, false, true).	
		// Parece que la siguiente es una forma de forzar dicho refresco
		return new File(zip.getAbsolutePath());
	}
	
	/**
	 * @param tempDir
	 * @param pathname
	 */
	private void copyToInputServer(File tempDir, String pathname)
	{
		/*
		 * Como los xml pueden ser consumidos por milenium al instante, primero se compian los recursos y luego los xml.
		 */
		
		tempDir.setExecutable(true, false);
		tempDir.setWritable(true, false);
		tempDir.setReadable(true, false);
		
		try{
			FileFilter folderFilter = new FileFilter() {
			    public boolean accept(File file) {
			        return file.isDirectory() && ( file.getName().equals("documents") || file.getName().equals("images") );
			    }
			};
	
			//Copiamos las carpetas con los recursos necesarios para los xml
			File[] files = tempDir.listFiles(folderFilter);
			for (File sourceFile : files)
				FileUtils.copyDirectory(sourceFile, new File(pathname+"/"+sourceFile.getName()), true);
			
			FileFilter xmlFilter = new FileFilter() {
			    public boolean accept(File file) {
			        return !file.isDirectory() && !file.getName().equals(IterKeys.XMLIO_XML_MAIN_FILE_NAME)
			        	&& file.getName().endsWith(".xml");
			    }
			};
	
			//Copiamos los xml de los artículos
			files = tempDir.listFiles(xmlFilter);
			for (File sourceFile : files)
				FileUtils.copyFileToDirectory(sourceFile, new File(pathname), true);
			
			
		}catch(Exception e){
			_log.error("Copy from temp directory failed: ", e);
		}
	}
}
