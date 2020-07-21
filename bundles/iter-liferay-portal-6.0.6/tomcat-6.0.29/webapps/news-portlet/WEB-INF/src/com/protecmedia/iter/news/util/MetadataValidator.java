package com.protecmedia.iter.news.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.MetadataControlUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.model.Metadata;

public class MetadataValidator 
{
	public static final	String MSG_UPDATED 						= "metadata-control-msg-updated";
	
	public static final	String ERR_UNEXPECTED 					= "metadata-control-err-unexpected";
	public static final	String ERR_INVALID_STRUCT 				= "metadata-control-err-invalid-struct";
	public static final	String ERR_INVALID_PREF_DOM				= "metadata-control-err-invalid-pref-dom";
	public static final	String ERR_INVALID_TITLE_NCHAR 			= "metadata-control-err-invalid-title-nchar";
	public static final	String ERR_INVALID_DESC_NCHAR 			= "metadata-control-err-invalid-desc-nchar";
	public static final	String ERR_INVALID_JOURNALSTRUCT		= "metadata-control-err-invalid-journalstruct";
	public static final	String ERR_INVALID_JOURNALSTRUCT_DOM	= "metadata-control-err-invalid-journalstruct-dom";
	
	private static Log _log = LogFactoryUtil.getLog(MetadataValidator.class);
	
	public static boolean validateMetadata(Metadata metadata, List<String> errors, String portletName) throws SystemException 
	{
		try
		{
			// Sea una estructura bien conocida
			boolean validStruct = ( metadata.getStructureName().equals(IterKeys.STRUCTURE_ARTICLE) 	||
									metadata.getStructureName().equals(IterKeys.STRUCTURE_POLL) 	||	
									metadata.getStructureName().equals(IterKeys.STRUCTURE_GALLERY) 	||
									metadata.getStructureName().equals(IterKeys.STRUCTURE_MULTIMEDIA));
			ErrorRaiser.throwIfFalse(validStruct, getKey(ERR_INVALID_STRUCT, portletName) );
			
			// Las preferencias se guarden en un DOM válido
			Document doc = MetadataControlUtil.loadArtPreferences(metadata.getPreferences(), metadata.getStructureName(), false);
			ErrorRaiser.throwIfFalse(doc != null, getKey(ERR_INVALID_PREF_DOM, portletName));
			
			// Se comprueba que el número de caracteres del título sea un valor numérico positivo
			Node nodePref 	= MetadataControlUtil.getPreferenceNode(doc, MetadataControlUtil.META_TITLE);
			try
			{
				long nchars = MetadataControlUtil.getNodeValue(nodePref, MetadataControlUtil.ATTR_NCHARS, -1);
				if (nchars < 0)
					errors.add( getKey(ERR_INVALID_TITLE_NCHAR, portletName) );
			}
			catch(NumberFormatException nfe)
			{
				_log.error(nfe.toString());
				errors.add( getKey(ERR_INVALID_TITLE_NCHAR, portletName) );
			}
			
			// Se comprueba que el número de caracteres de la descripción sea un valor numérico positivo			
			nodePref 		= MetadataControlUtil.getPreferenceNode(doc, MetadataControlUtil.META_DESCRIPTION);
			try
			{
				long nchars = MetadataControlUtil.getNodeValue(nodePref, MetadataControlUtil.ATTR_NCHARS, -1);
				if (nchars < 0)
					errors.add( getKey(ERR_INVALID_DESC_NCHAR, portletName) );
			}
			catch(NumberFormatException nfe)
			{
				_log.error(nfe.toString());
				errors.add( getKey(ERR_INVALID_DESC_NCHAR, portletName) );
			}
		}
		catch (ServiceError se)
		{
			errors.add( se.getErrorCode() );
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			errors.add( getKey(ERR_UNEXPECTED, portletName) );
		}

		return errors.size() == 0;
	}
	
	public static void validateRequest(HttpServletRequest request, PortletRequest renderRequest, String portletName)
	{
		// Se añaden todos los errores de este portlet
		Iterator<String> itErrors = SessionErrors.iterator(request);
		while (itErrors.hasNext())
		{
		    String errorKey =  itErrors.next();
		    if (errorKey.startsWith(portletName)) 
		    	SessionErrors.add(renderRequest, errorKey);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public static String getKey(String msg, String portletName)
	{
		return portletName+"."+msg;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Devuelve la lista de clave/msg de errores asociados a un portlet
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public static KeyValuePair[] getErrors(PortletRequest renderRequest, String portletName)
	{
		return getIteratorList(SessionErrors.iterator(renderRequest), portletName);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public static KeyValuePair[] getSuccess(PortletRequest renderRequest, String portletName)
	{
		return getIteratorList(SessionMessages.iterator(renderRequest), portletName);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static KeyValuePair[] getIteratorList(Iterator<String> it, String portletName)
	{
		List<KeyValuePair> list = new ArrayList<KeyValuePair>();
		
		while (it.hasNext())
		{
		    String errorKey =  it.next();
		    if (errorKey.startsWith(portletName)) 
		    {
		        String errorMessage = StringUtils.substringAfter(errorKey, portletName+".");
		       
		        KeyValuePair pair = new KeyValuePair();
				pair.setKey(errorKey);
				pair.setValue(errorMessage);
				list.add(pair);
		    }
		}

		return list.toArray(new KeyValuePair[list.size()]);
	}
}
