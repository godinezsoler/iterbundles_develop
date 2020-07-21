package com.protecmedia.iter.user.util;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.liferay.portal.kernel.image.ImageBag;
import com.liferay.portal.kernel.image.ImageProcessorUtil;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.imagegallery.ImageSizeException;

/**
 * This is a File Upload Servlet that is used with AJAX
 * to monitor the progress of the uploaded file. It will
 * return an XML object containing the meta information
 * as well as the percent complete.
 */
public class FileUploadServlet extends HttpServlet implements Servlet {
   
	private static final long serialVersionUID = 2740693677625051632L;
	protected static String CLASS = FileUploadServlet.class.getName();
    protected static Logger logger = Logger.getLogger(CLASS);
	public FileUploadServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// create file upload factory and upload servlet
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		int widthMax =0;
		int heightMax = 0;
		String widthMaxParam = request.getParameter("widthMax"); 
		String heightMaxParam = request.getParameter("heightMax"); 
		if (widthMaxParam !=null) {
			widthMax=  Integer.parseInt(widthMaxParam.toString());
		}else{
			widthMax =50;
		}
		if (heightMaxParam !=null){
			heightMax = Integer.parseInt(heightMaxParam.toString());
		}else{
			heightMax = 50;
		}
		

		List<?> uploadedItems = null;
		FileItem fileItem = null;
		String nameSystemOperative= System.getProperty("os.name");
		String filePath ="";
		if (nameSystemOperative.contains("Windows")){
			 filePath = getServletContext().getRealPath("/") + "tmp_uploads\\";
		}else{
			
			 filePath = getServletContext().getRealPath("/") + "tmp_uploads/";
		}
		
		String uriImg = "";

		try {
			// iterate over all uploaded files
			uploadedItems = upload.parseRequest(request);
			
			for (int i = 0; i < uploadedItems.size(); i++) {
				fileItem = (FileItem) uploadedItems.get(i);

				if (fileItem.isFormField() == false) {
					if (fileItem.getSize() > 0) {
						File uploadedFile = null;
						String myFullFileName = fileItem.getName(),
						myFileName = "",
						slashType = (myFullFileName.lastIndexOf("\\") > 0) ? "\\" : "/";    // Windows or UNIX
						int startIndex = myFullFileName.lastIndexOf(slashType);

						// Ignore the path and get the filename
						myFileName = myFullFileName.substring(startIndex + 1, myFullFileName.length());

						String p = (new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss")).format(new Date());
						// Create new File object
						uploadedFile = new File(filePath, p + "-" +myFileName);			
						uriImg = p + "-" +myFileName;
						byte[] file = null;
						if ((fileItem != null) && (fileItem.get().length > 0)) {					
							
							 String[] index = fileItem.getName().split("\\.");
							 String ext= index[1];
							 if (ext.equalsIgnoreCase("jpg")
									 || ext.equalsIgnoreCase("png")
									 || ext.equalsIgnoreCase("jpeg")
									 || ext.equalsIgnoreCase("gif")
									 || ext.equalsIgnoreCase("ico")
									 || ext.equalsIgnoreCase("psd")
									 || ext.equalsIgnoreCase("bmp")
									 ){
								 file= resizeImage(fileItem.get(), heightMax, widthMax);
							 }else if (ext.equalsIgnoreCase("wmv")
									 || ext.equalsIgnoreCase("avi")
									 || ext.equalsIgnoreCase("MPEG-4")
									 || ext.equalsIgnoreCase("mov")
									 || ext.equalsIgnoreCase("mkv")
									 || ext.equalsIgnoreCase("wav")
									 || ext.equalsIgnoreCase("mp3")
									 || ext.contains("mpeg")
									 || ext.equalsIgnoreCase("mp4")
									 || ext.equalsIgnoreCase("wma")
									 || ext.equalsIgnoreCase("aac")
									 || ext.equalsIgnoreCase("swf")
									 || ext.equalsIgnoreCase("flv")
									 || ext.equalsIgnoreCase("ac3")
									 || ext.equalsIgnoreCase("ogg")
									 || ext.equalsIgnoreCase("flac")
									 || ext.equalsIgnoreCase("jpeg")
									 || ext.equalsIgnoreCase("jpeg")
									 ){
								 file = fileItem.get();
							 }
							 
						}	
						

						FileOutputStream fos = new FileOutputStream(uploadedFile);
						fos.write(file);
						fos.close();
					}
				}
			}
		} catch (FileUploadException e) {
			logger.logp(Level.WARNING, CLASS, "doPost",
					"Couldn't upload file", e);
		}catch (Exception e) {
			logger.logp(Level.WARNING, CLASS, "doPost",
					"Couldn't upload file", e);
		} 
				
		PrintWriter out = response.getWriter();
		StringBuffer buffy = new StringBuffer();

		response.setContentType("text/html");
		
		buffy.append("{'url': '" + uriImg + "'}");		

		out.println(buffy.toString());
		out.flush();
		out.close();
	}
	
	private byte[] resizeImage(byte[] bytes, int heightMax, int widthMax) throws ImageSizeException {
		try {
			ImageBag imageBag = ImageProcessorUtil.read(bytes);

			RenderedImage renderedImage = imageBag.getRenderedImage();

			if (renderedImage == null) {
				return null;
			}

			renderedImage = ImageProcessorUtil.scale(renderedImage, heightMax, widthMax);

			String contentType = imageBag.getType();
			
			return ImageProcessorUtil.getBytes(renderedImage, contentType);
		} catch (IOException ioe) {
			throw new ImageSizeException(ioe);
		}		
	}
}

