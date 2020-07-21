package com.protecmedia.iter.user.util;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;

public class NewsletterUtil {

	private static JSONObject jsonDocument;
	
	public NewsletterUtil()  {
		
		jsonDocument = JSONFactoryUtil.createJSONObject();
		
	}
	
	/**
	 * Method contructor overwrite
	 * @param XML_Preferences
	 * @throws SAXException
	 * @throws IOException
	 * @throws DocumentException
	 * @throws ParserConfigurationException 
	 * @throws JSONException 
	 */
	public NewsletterUtil(String XML_Preferences) throws  JSONException {
		
		if (XML_Preferences.equalsIgnoreCase(""))
			jsonDocument = JSONFactoryUtil.createJSONObject();
		else
		jsonDocument = JSONFactoryUtil.createJSONObject(XML_Preferences);
		
	}
	
	
	
	/**
	 * Add to xmlDocument a new newsletter
	 * @param idNewsletter
	 * @param checked
	 * @param idRoll
	 */
	public void createElementNewsletter(String idNewsletter, String idGroupId, String categoryAccess) {
		
		JSONObject newsletter =JSONFactoryUtil.createJSONObject();
		
		newsletter.put("categoryAccess", categoryAccess);
		newsletter.put("idGroupId", idGroupId);
		
		jsonDocument.put(idNewsletter, newsletter );
		
	}
	
	/**
	 *Obtain the xml with all information 
	 * @return newsletterConfigXML
	 */
	public String getFullXMLPreferences(){
		
		return jsonDocument.toString();
			
	}
	
	/**
	 * Funcion para comprobar si el usuario puede ver la newsletter para subscribirse o no
	 * @param cmUser
	 * @param idNewsletter
	 * @return Si el usuario lo puede ver o no
	 * @throws SystemException
	 */
	public boolean isNewsletterVisible(User cmUser, String idNewsletter) throws SystemException{
		
		JSONObject objJson = jsonDocument.getJSONObject(idNewsletter);
		if (objJson!=null){
		String levelCategoryAccess= objJson.getString("categoryAccess");
		if (levelCategoryAccess.equalsIgnoreCase("-1")) return false;
		if (levelCategoryAccess.equalsIgnoreCase("0")) return true;
		//Obtiene el nivel de aceso del usuario
		int levelAccessUser = IterLocalServiceUtil.getUserAccess(cmUser);
		//Comprueba si el usuario lo puede ver o no
		if (levelAccessUser >= Long.parseLong(levelCategoryAccess)){
			return true;
		}
			
		return false;
		}else{
			return false;
		}
	}
	
	
	/**
	 * Obtain the level access value assigned to newsletter
	 * @param idNewsletter
	 * @return level access
	 */
	public static String getLevelAccess(String idNewsletter){
		
		JSONObject objJson = jsonDocument.getJSONObject(idNewsletter);
		if (objJson!=null){
		String levelAccess= objJson.getString("categoryAccess");
				
		return levelAccess;
		}else{
			return "";
		}
		
	}
	
	/**
	 * Obtain the id group value assigned to newsletter
	 * @param idNewsletter
	 * @return id idGroupId
	 */
	public static String getIdGroup(String idNewsletter){
		
		JSONObject objJson = jsonDocument.getJSONObject(idNewsletter);
		if (objJson==null){
			return "";
		}
		String idGroupId= objJson.getString("idGroupId");
				
		return idGroupId;
	}
	
	/**
	 * Check if the registered user has this roll
	 * @param cmRolesUser
	 * @param idRollElem
	 * @return true or false
	 */
	public static boolean isGroupUser(List<UserGroup> cmGroupsUser, String idGroupElem) 
	{
		for (UserGroup cmGroupUser : cmGroupsUser)
		{
			String idCmGroupUser = String.valueOf(cmGroupUser.getUserGroupId());
			if (idCmGroupUser.equalsIgnoreCase(idGroupElem))
			{
				return true;
			}
		}
		return false;
	}
}
