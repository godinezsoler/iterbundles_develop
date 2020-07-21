package com.protecmedia.iter.base.service.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;

public class IterFileUtil
{
	private static Log _log = LogFactoryUtil.getLog(IterFileUtil.class);
	
	private static final String HEX_FORMAT = "%02X%02X%02X";
	private static final String LESS_THAN_UNICODE_1 = "3C";
	private static final String LESS_THAN_UNICODE_2 = "FFFE3C";
	private static final String LESS_THAN_UNICODE_3 = "FEFF3C";
	private static final String SVG = "svg";
	private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
	
	public static String getExtensionFromBytes(byte[] bytes) throws IOException, ServiceError
	{
		String extension 	 = null;
		ImageInputStream iis = null;
		
		try
		{
			iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
			Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
	
			while(it.hasNext())
			{
				ImageReader imageReader = it.next();
				extension = imageReader.getFormatName();
				break;
		    }
		}
		finally
		{
			if(iis != null)
				iis.close();
		}

		// Comprueba si es una imagen con formato SVG
		if(Validator.isNull(extension) && bytes.length >= 3)
		{
			// Primero valida que el fichero empiece por el carácter '<'
			String hex = String.format(HEX_FORMAT, bytes[0], bytes[1], bytes[2]);
			if (hex.startsWith(LESS_THAN_UNICODE_1) || hex.startsWith(LESS_THAN_UNICODE_2) || hex.startsWith(LESS_THAN_UNICODE_3))
			{
				try
				{
					// Carga el fichero en un Document y recupera el elemento raiz y el namespace
					Document svg = SAXReaderUtil.read(new String(bytes, StringPool.UTF8));
					String rootelement = svg.getRootElement().getName();
					String namespace = svg.getRootElement().getNamespace().getStringValue();
					
					// Comprueba que el elemento raiz sea <svg> y que esté definido el namespace 'http://www.w3.org/2000/svg'
					if (SVG.equalsIgnoreCase(rootelement) && SVG_NAMESPACE.equalsIgnoreCase(namespace))
						extension = SVG;
				}
				catch (Throwable th)
				{
					// Do nothing. No es un SVG correcto.
				}
			}
		}
		
		if(Validator.isNull(extension))
		{
			_log.error("Image not supported");
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		else
		{
			extension = StringPool.PERIOD + extension.toLowerCase();
		}

		return extension;
	}

}
