package com.protecmedia.iter.user.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.imageio.ImageIO;


import org.apache.commons.io.FilenameUtils;


import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.FileUtil;

import com.liferay.portal.kernel.util.ParamUtil;

public class MileniumContentUtil {
	
	/*
	 * 
	 *  Funciones para crear el contenido en Milenium
	 *  TODO: Disponer de una API que permita crear los XML a más alto nivel usand DOM
	 *  TODO: Sacar todos los nombres e IDs de contenidos a constantes en IterKeys
	 * 
	 */
	
	/**
	 * 
	 */
	public static void saveXML(String prefix, String path, String contentTitle, String contentText, String author, String photoAuthor, List<String> photos, 
			String metadata, String name, String lastName, String email, String comment, List<String> cutline, List<String> videos, List<String> cutlineVideo) {		
		
		String xml = createXMLMilenium(contentTitle + " - " + prefix, contentTitle, contentText, author, photoAuthor, photos, 
				 	metadata,  name,  lastName,  email,  comment, prefix, cutline, videos, cutlineVideo);			
		
		path = path  + prefix + ".xml";
		
		saveFile(path, xml);
	}
	
	
	/**
	 * 
	 * @param publicacion
	 * @param section
	 * @param model
	 * @param title
	 * @param subtitle
	 * @param antetitulo
	 * @param content
	 * @param entradilla
	 * @param author
	 * @param namePhoto
	 * @param authorPhoto
	 * @param pieFoto
	 * @return
	 */
	private static String createXMLMilenium(
			String articleName, 
			String title, 
			String content, 
			String author, 
			String photoAuthor, 
			List<String> photos,
			String metadata, 
			String name, 
			String lastName, 
			String email, 
			String comment, 
			String prefix, 
			List<String> cutline, 
			List<String> videos, 
			List<String> cutlineVideo ) {
		
		StringBuffer xml = new StringBuffer();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n");
		xml.append("<MileniumXML xmlns:msxsl=\"urn:schemas-microsoft-com:xslt\" creator=\"Milenium\" type=\"Article\" version=\"2\"> \n");
		xml.append(" <Article name=\"" + articleName + "\"> \n");					
		xml.append(createTextComponent("Text", "1", "Headline", title));
		xml.append(createTextComponent("Text", "4", "Text", content));
		xml.append(createTextComponent("Text", "7", "Byline", author));
		
		if (photos!=null &&photos.size()>0){
			for (int i = 0; i < photos.size(); i++) {
				if (cutline.get(i)==null || (cutline.get(i)).equalsIgnoreCase("")){
					xml.append(createPhotoComponent(photos.get(i), author, prefix, "" ));
				}else{
					xml.append(createPhotoComponent(photos.get(i), author, prefix, cutline.get(i) ));
				}
				
			}
		}
		if (videos!=null && videos.size()>0){
			for (int i = 0; i < videos.size(); i++) {
				if (cutlineVideo.get(i)==null || (cutlineVideo.get(i)).equalsIgnoreCase("")){
					xml.append(createVideoComponent(videos.get(i), author, prefix, "" ));
				}else{
					xml.append(createVideoComponent(videos.get(i), author, prefix, cutline.get(i) ));
				}
				
			}
		}
		xml.append(createTextComponent("Text", "2160", "UserData", name+" "+lastName));
		xml.append(createTextComponent("Text", "2162", "Email", email));
		xml.append(createTextComponent("Text", "2161", "Comment", comment));
		xml.append(metadata);
		xml.append(" </Article> \n");
		xml.append("</MileniumXML>");
		
		return xml.toString();
	}
	
	private static String createPhotoComponent(String namePhoto, String author, String articleId, String cutline) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(" <Component class=\"Image\" idtype=\"8\" name=\"" + namePhoto + "\" path=\"" +articleId + "_" + namePhoto + "\" typename=\"Image\"> \n");
		sb.append(" <Author abrv=\"\" byline=\"" + author + "\" name=\"" + author + "\" /> \n");
		sb.append(" </Component> \n");
		if (cutline==null || cutline.equalsIgnoreCase("")){ 
		}else{
			sb.append(createTextComponent("Text", "6", "Cutline", cutline ));
		}
		return sb.toString();
	}
	
	private static String createVideoComponent(String nameVideo, String author, String articleId, String cutline) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(" <Component class=\"Multimedia\" idtype=\"19\" name=\"" + nameVideo + "\" path=\"" +articleId + "_" + nameVideo + "\" typename=\"Multimedia\"> \n");
		
		sb.append(" <Author abrv=\"\" byline=\"" + author + "\" name=\"" + author + "\" /> \n");
		sb.append(" </Component> \n");
		if (cutline==null || cutline.equalsIgnoreCase("")){ 
			
		}else{
			sb.append(createTextComponent("Text", "6", "Cutline", cutline ));
		}
		return sb.toString();
	}
	
	private static String createTextComponent(String classField, String typeid, String typename, String value) {
		String[] paragraphs = value.split("\\n");
		
		StringBuffer sb = new StringBuffer();
		sb.append(" <Component class=\"" + classField + "\" typeid=\"" + typeid + "\" typename=\"" + typename +"\"> \n");
		sb.append(" <Paragraphs> \n");
		for (int i = 0; i < paragraphs.length; i++) {
			sb.append(" <P> \n");
			sb.append(" <C>" + paragraphs[i] + "</C> \n");		
			sb.append(" </P> \n");
		}
		sb.append(" </Paragraphs> \n");
		sb.append(" </Component> \n");	
		
		return sb.toString();
	}
	

	/**
	 * @param articleId
	 * @param path
	 * @param xml
	 */
	private static void saveFile(String path, String content) {
						
		try {
			
			File file = new File(path);
			
			PrintWriter writer = new PrintWriter(file);			
			writer.print(content);
			writer.close();
		} catch (FileNotFoundException e) {
			;
		}
	}
	
	/**
	 * 
	 * @param uploadRequest
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static List<String> saveImages(UploadPortletRequest uploadRequest, String origenPath, String destinoPath, String prefix) throws Exception {
		if (destinoPath!=null && !destinoPath.equalsIgnoreCase("")){
			List<String> images = new ArrayList<String>();				
			
			String imagePrefix = "structure_image_";
			String coordenadasPrefix = "coordValue";
			
			Enumeration<String> enu = uploadRequest.getParameterNames();				
	
			int count = 0;
	
			while (enu.hasMoreElements()) {
				String name = enu.nextElement();
							
				if (name.startsWith(imagePrefix)) {
					
					String fileName = ParamUtil.getString(uploadRequest, name, "");
					String coordCropping = ParamUtil.getString(uploadRequest, coordenadasPrefix + count, "");
	
					if (!fileName.equals("")) {
						String fileImage = origenPath + fileName;
	                    String fileExtension = FilenameUtils.getExtension(fileName);
	                    
	                    if (fileExtension.equalsIgnoreCase("")) {
	                    	fileExtension = "jpg";
	                    	
	                    }
						File file = new File(fileImage);
						
						if(!coordCropping.equalsIgnoreCase("")){
							String[] arraCoords = coordCropping.split("-");
							BufferedImage outImage = ImageIO.read(file);
							BufferedImage cropped=outImage.getSubimage(Integer.parseInt(arraCoords[0]),Integer.parseInt(arraCoords[1]), Integer.parseInt(arraCoords[4]), Integer.parseInt(arraCoords[5]));
							ByteArrayOutputStream out=new ByteArrayOutputStream();
							ImageIO.write(cropped,fileExtension, out);
							ImageIO.write(cropped,fileExtension,new File(destinoPath + prefix + "_" + file.getName() ));
							images.add(fileName);
							count++;
							
						}
	
						else{
							byte[] bytes = FileUtil.getBytes(file);							
							
							if ((bytes != null) && (bytes.length > 0)) {
						
								copyFile(file, new File(destinoPath +  prefix + "_" + file.getName()));
								
								images.add(fileName);
								
								count++;
							}
						}
					}
				}
			}
			return images;
		}else{
			return null;
		}
		
	}
	
	/**
	 * 
	 * @param uploadRequest
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static List<String> saveVideos(UploadPortletRequest uploadRequest, String origenPath, String destinoPath, String prefix) throws Exception {
		if (destinoPath!=null && !destinoPath.equalsIgnoreCase("")){
			List<String> videos = new ArrayList<String>();				
			
			
			String videoPrefix = "structure_video_";
			
			Enumeration<String> enu = uploadRequest.getParameterNames();				
			
			int count = 0;
			while (enu.hasMoreElements()) {
				String name = enu.nextElement();
							
				if (name.startsWith(videoPrefix)) {
					
					String fileName = ParamUtil.getString(uploadRequest, name, "");
					
					if (!fileName.equals("")) {
						String fileVideo = origenPath + fileName;
	
						File file = new File(fileVideo);
						
						byte[] bytes = FileUtil.getBytes(file);							
						
						if ((bytes != null) && (bytes.length > 0)) {					
							
							copyFile(file, new File(destinoPath + prefix + "_" + file.getName()));
							
							videos.add(fileName);
							
							count++;
						}
					}
					
				}
			}
			
			return videos;
		}else{
			return null;
		}	
	}

	
	private static void copyFile(File fileFrom, File fileTo) {
		try {
			/* Abrir el archivo */
			FileInputStream OrigenF = new FileInputStream(fileFrom);
			FileOutputStream DestinoF = new FileOutputStream(fileTo);
			
			/* Creamos un buffer para leer del archivo en bloques de 4096 bytes */
			BufferedInputStream Bin = new BufferedInputStream(OrigenF, 4096);
			BufferedOutputStream Bout = new BufferedOutputStream(DestinoF);
			int c;
			while( ( c = Bin.read() ) != -1 ) {
				Bout.write(c);
			}
			
			Bin.close();
			Bout.close();
		} catch (FileNotFoundException exception) {
			;
		} catch (IOException exception) {	
	        ;	
	    }	
		
	}
	
	
	private static void copyFileImageFromURL(String url, String destino) {

		InputStream in = null;
		OutputStream out = null;
		try {
			/* definimos la URL de la cual vamos a leer */
			URL intlLogoURL = new URL(url);
	
			/* llamamos metodo para que lea de la URL y lo escriba en le fichero pasado */
			
			in = intlLogoURL.openStream();
			out = new FileOutputStream(new File(destino));
			
			int c;
			while ((c = in.read()) != -1) {
				out.write(c);
			}
		} catch (MalformedURLException e) {
			;
		} catch (FileNotFoundException e) {
			;
		} catch (IOException e) {
			;
		} finally {
			if (in != null) { 
				try{ 
					in.close(); 
				} catch (Exception e) {
					
				} 
			}
			
			if (out != null) { 
				try { 
					out.close(); 
				} catch (Exception e) {
					
				} 
			}
		}
	}

}
