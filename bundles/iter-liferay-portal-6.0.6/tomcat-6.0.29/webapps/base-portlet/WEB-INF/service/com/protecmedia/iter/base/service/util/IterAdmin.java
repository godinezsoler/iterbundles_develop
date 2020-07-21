package com.protecmedia.iter.base.service.util;

import java.util.List;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class IterAdmin 
{
	private static Log _log = LogFactoryUtil.getLog(IterAdmin.class);

	public static final String PKG_EXTENSION = ".iter";
	public static final String IA_CLASS_SMTPSERVER 			= "smtpserver";
	public static final String IA_CLASS_NEWSLETTER 			= "newsletter";
	public static final String IA_CLASS_NEWSLETTER_SCHEDULE = "schedule";
	public static final String IA_CLASS_COMMENTS 			= "comments";
	public static final String IA_CLASS_FORM 				= "form";
	public static final String IA_CLASS_CAPTCHA 			= "captcha";
	public static final String IA_CLASS_USERPROFILE			= "userprofile";
	public static final String IA_CLASS_USER_REG			= "userreg";
	public static final String IA_CLASS_SOCIAL_CFG			= "socialconfig";
	public static final String IA_CLASS_CTXVARS				= "ctxvars";
	public static final String IA_CLASS_CTXVARS_CATEGORY	= "ctxvarscategory";
	public static final String IA_CLASS_ADS					= "ads";
	public static final String IA_CLASS_ADSLOT				= "adslot";
	public static final String IA_CLASS_ADTAG				= "adtag";
	public static final String IA_CLASS_ADSKIN				= "adskin";
	public static final String IA_CLASS_ADSLOTTAG			= "adslottag";
	public static final String IA_CLASS_ADSLOTTAG_CATEGORY	= "adslottagcategory";
	public static final String IA_CLASS_ADVOCABULARY		= "advocabulary";
	public static final String IA_CLASS_AD_FILEENTRY		= "adfileentry";
	public static final String IA_CLASS_SEARCH_PORTLET		= "searchportlet";
	public static final String IA_CLASS_LASTUPDT_PORTLET	= "lastupdtportlet";
	public static final String IA_CLASS_METADA				= "metada";
	public static final String IA_CLASS_TRANSFORM			= "transform";
	public static final String IA_CLASS_MOBILE_VERSION		= "mobileversion";
	public static final String IA_CLASS_ROBOTS				= "robots";
	public static final String IA_CLASS_PUBLICATION_CFG		= "publicationconfig";
	public static final String IA_CLASS_PAYWALL				= "paywall";
	public static final String IA_CLASS_LOGIN_PORTLET		= "loginportlet";
	public static final String IA_CLASS_SOCIAL_CONFIG		= "social";
	public static final String IA_CLASS_VISITS				= "visits";
	public static final String IA_CLASS_METRICS				= "metrics";
	public static final String IA_CLASS_BLOCKER_AD_BLOCK	= "blockeradblock";
	public static final String IA_CLASS_RSS_ADVANCED		= "rssadvanced";
	public static final String IA_CLASS_RSS_SECTIONS		= "rsssections";
	public static final String IA_CLASS_QUALIFICATION		= "qualification";
	public static final String IA_CLASS_FEEDBACK_PORTLET	= "feedbackportlet";
	public static final String IA_CLASS_EXTERNAL_SERVICE	= "externalservice";
	public static final String IA_CLASS_URL_SHORTENER		= "urlshortener";

	public static String logout(String xmlData) throws Exception
	{
		return xmlData;
	}
	
	public static String getGroups() throws Exception
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		try
		{
			List<Company> companies = CompanyLocalServiceUtil.getCompanies();
			if(companies != null && companies.size() > 0)
			{
				String companyId = String.valueOf(companies.get(0).getCompanyId());
				List<Group> groups = PortletMgr.getGroups(companyId);
				for(Group group:groups)
				{
					Element row = SAXReaderUtil.read("<row/>").getRootElement();
					row.addAttribute("name", 			group.getName());
					row.addAttribute("groupid", 		String.valueOf(group.getGroupId()));
					row.addAttribute("phpEnabled", 		"true");
					row.addAttribute("friendlyURL", 	group.getFriendlyURL() );
					row.addAttribute("delegationid", 	String.valueOf(group.getDelegationId()) );
					
					rs.add(row);
				}
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return rs.asXML();
	}
	
	/**
	 * Método que procesa la información de Exportación/Importación para extraer de esta el groupId y los datos a actualizar 
	 * @param info
	 * @param groupId
	 * @return
	 * @throws ServiceError
	 * @throws DocumentException
	 * @throws PortalException
	 * @throws SystemException
	 */
	public static Document processExportImportInfo(String info, LongWrapper groupId) throws ServiceError, DocumentException, PortalException, SystemException
	{
		Document dom = SAXReaderUtil.read(info);
		Element root = dom.getRootElement();
		
		// Se obtiene el groupId
		String groupName= XMLHelper.getStringValueOf(root, "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		groupId.setValue( GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId() );
		
		// Se eliminan los nodos intrínsicos de la importación
		List<Node> nodesToDel = dom.getRootElement().selectNodes("@*[name(.)='groupName' or name(.)='updtIfExist' or name(.)='importProcess']");
		for (Node node : nodesToDel)
			node.detach();

		return dom;
	}
}