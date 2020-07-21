package com.protecmedia.iter.news.util;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.render.RenditionMode;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.sectionservers.SectionServersMgr;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class RobotsControlUtil
{
	private static Log _log = LogFactoryUtil.getLog(RobotsControlUtil.class);

	public static final	String PRIMARY_SECTION		= "PRIMARY_SECTION";
	public static final	String SECONDARY_SECTION 	= "SECONDARY_SECTION";
	public static final	String WITHOUT_SECTION		= "WITHOUT_SECTION";
	
	public static final	String PRIMARY_SECTION_VALUE	= "PRIMARY_SECTION_VALUE";
	public static final	String SECONDARY_SECTION_VALUE 	= "SECONDARY_SECTION_VALUE";
	public static final	String WITHOUT_SECTION_VALUE	= "WITHOUT_SECTION_VALUE";
	
	public static final	String NO_INDEX		= "NO INDEX";
	public static final	String NO_FOLLOW 	= "NO FOLLOW";
	
	public static final	String NOINDEX		= "NOINDEX";
	public static final	String NOFOLLOW 	= "NOFOLLOW";
	public static final	String INDEX		= "INDEX";
	public static final	String FOLLOW 		= "FOLLOW";
	
	private final String REL_CANONICAL 		= "canonical";
	private final String REL_ALTERNATE 		= "alternate";
	private final String REL_AMP			= "amphtml";
	
	private static List<KeyValuePair> botsOptions	= null;
	
	public static List<KeyValuePair> getOptions(){
		if(botsOptions==null){
			botsOptions	= new ArrayList<KeyValuePair>();
			botsOptions.add( new KeyValuePair(	(""), 						(INDEX		+ ", " + FOLLOW)) );
			botsOptions.add( new KeyValuePair( (NOINDEX	+","+	FOLLOW), 	(NO_INDEX 	+ ", " + FOLLOW) ) );
			botsOptions.add( new KeyValuePair( (NOINDEX	+","+	NOFOLLOW), 	(NO_INDEX 	+ ", " + NO_FOLLOW) ) );
			botsOptions.add( new KeyValuePair( (INDEX	+","+	NOFOLLOW),	(INDEX		+ ", " + NO_FOLLOW) ) );
		}		
		return botsOptions;
	}
	
	// Documentación ticket: 0009171
	public Document getMetaRobots(JournalArticle ja, ThemeDisplay themeDisplay, HttpServletRequest request) throws SystemException, PortalException, IOException, SQLException, SecurityException, ServiceError, NoSuchMethodException
	{
		_log.trace("In getMetaRobots");				

		long groupId = themeDisplay.getScopeGroupId();
		String host = IterURLUtil.getIterHost();
		
		String detailURL 		= PortalUtil.getCurrentCompleteURL(request);
		String contentSection 	= ParamUtil.getString(request, WebKeys.URL_PARAM_CONTENT_SECTION, "").replace(" ", "/");
		String contentId 		= ParamUtil.getString(request, WebKeys.URL_PARAM_CONTENT_ID, "0");
		String canonicalUrl 	= setCanonicalUrl(host, contentId, groupId, request);
		
		//Se añade la url canónica.
		Document dom 			= SAXReaderUtil.createDocument();
		Element eleRS			= dom.addElement("rs");
		addMetaTagLink( eleRS, REL_CANONICAL, canonicalUrl );

		//Si el sitio tiene habilitadas las urls moviles se pone la url alternativa
		long isMobileRq = IterRequest.isMobileRequest();
		String mobileToken = IterGlobal.getMobileToken(groupId);
		
		if( isMobileRq==0 && mobileToken!="" )
		{
			Element alternateLinkElem = addMetaTagLink( eleRS, REL_ALTERNATE, SectionServersMgr.processMobileURL(host, IterURLUtil.buildMobileURL()) );
			alternateLinkElem.addAttribute( "media", "only screen and (max-width: 640px)");
		}

		// Se añade la URL AMP si procede
		addAMPTagLink(ja, eleRS, host, groupId);
		
		if(_log.isDebugEnabled())
		{
			_log.debug("contentSection: " + contentSection);
			_log.debug("contentId: "      + contentId);
			_log.debug("canonicalUrl: "   + canonicalUrl);
			_log.debug("detailURL: "      + detailURL);
		}
		
		//Regla 1 Robots. Detalles de artículos no indexables(NOINDEX, FOLLOW)
		if( !ja.getIndexable() )
		{
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Article detail not indexable: ").append(NOINDEX).append(", ").append(FOLLOW));
			addMetaTagBot(eleRS, new StringBuilder(NOINDEX).append(", ").append(FOLLOW).toString() );
		}
		else
		{
			try
			{
				// Si no se encuentra el layout levantará una excepción
				Layout layout			= LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, "/"+contentSection);
				PageContent pc			= null;
				boolean defaultSection 	= false;
				
				if(layout != null)
				{
					if(_log.isDebugEnabled())
						_log.debug("layout: " + layout.getUuid());
					
					pc 		  		= PageContentLocalServiceUtil.getPageContentByContentIdLayoutId(groupId, contentId, layout.getUuid());
					defaultSection 	= pc.getDefaultSection();
					
					// http://jira.protecmedia.com:8080/browse/ITER-1160?focusedCommentId=51877&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-51877
					// Si es una petición móvil, si la sección móvil o la sección clásica correspondiente es la sección por defecto, la página será indexable
					if (isMobileRq != 0L)
					{
						if (!defaultSection || pc == null)
						{
							String classicContentSection = contentSection.replace(mobileToken, StringPool.BLANK);
							Layout classiclayout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, classicContentSection);
							
							PageContent classicPC = PageContentLocalServiceUtil.getPageContentByContentIdLayoutId(groupId, contentId, classiclayout.getUuid());
							
							boolean classicDefaultSection = (classicPC != null) ? classicPC.getDefaultSection() : false;
							
							if (_log.isDebugEnabled())
								_log.debug( String.format("Movil section '%s': default section = %b.\nClassic section '%s': default section = %b", 
										contentSection, defaultSection, classicContentSection, classicDefaultSection) );
								
							defaultSection = defaultSection || classicDefaultSection;
							
							// Si la versión móvil no tiene PageContent, se asigna el PageContent de la versión clásica
							if (pc == null)
								pc = classicPC;
						}
					}
				}	
				
				if (pc != null)
				{
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Page content: ").append(pc.getPageContentId()));
					
					//Regla 2 Robots. Estamos en una sección principal (INDEX, FOLLOW)
					if (defaultSection)
					{
						if(_log.isDebugEnabled())
							_log.debug(new StringBuilder("Principal section: ").append(INDEX).append(", ").append(FOLLOW));													
						addMetaTagBot(eleRS, new StringBuilder(INDEX).append(", ").append(FOLLOW).toString() );
					}	
					
					//Regla 3 Robots. Estamos en una sección secundaria (NOINDEX, FOLLOW)
					else
					{						
						if(_log.isDebugEnabled())
							_log.debug(new StringBuilder("Not principal section: ").append(NOINDEX).append(", ").append(FOLLOW));
						addMetaTagBot(eleRS, new StringBuilder(NOINDEX).append(", ").append(FOLLOW).toString() );
					}
				}
				//Regla 4 Robots.Sin sección definida (NOINDEX, NOFOLLOW)
				else
				{
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Without page content: ").append(NOINDEX).append(", ").append(NOFOLLOW));
					addMetaTagBot(eleRS, new StringBuilder(NOINDEX).append(", ").append(NOFOLLOW).toString() );					
				}
			}
			catch (NoSuchLayoutException nsle)
			{			
				// Regla 5 Robots. Si la url es la canónica (INDEX, FOLLOW)
				// Se compara por URL.getPath para despreciar el protocolo y el host. 
				// http://jira.protecmedia.com:8080/browse/ITER-768?focusedCommentId=28655&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-28655
				if (new URL(detailURL).getPath().equalsIgnoreCase( new URL(canonicalUrl).getPath() ))
				{	
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Canonical url: ").append(INDEX).append(", ").append(FOLLOW));
					addMetaTagBot(eleRS, new StringBuilder(INDEX).append(", ").append(FOLLOW).toString() );
				}
				else
				{
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Without section: ").append(NOINDEX).append(", ").append(NOFOLLOW));
					addMetaTagBot(eleRS, new StringBuilder(NOINDEX).append(", ").append(NOFOLLOW).toString() );	
				}
			}
			catch(Exception e)
			{
				_log.error("Unexpecting error getting robots metadata", e);			
			}	
		}
		
		
		return dom;
	}
	
	private void addMetaTagBot(Element parent, String content)
	{
		Element elem = null;
		if (!content.isEmpty())		
		{
			elem = parent.addElement("meta");
			elem.addAttribute("name", "name");
			elem.addAttribute("namevalue", 	"ROBOTS");
			
			Element contentElem = elem.addElement("content");
			contentElem.addText(content);
		}
	}
	
	private Element addMetaTagLink( Element parent, String relType, String href )
	{
		Element elem = parent.addElement("link");
		
		elem.addAttribute( "rel", relType );
		elem.addAttribute( "href", href );
		
		return elem;
	}
	
	private void addAMPTagLink(JournalArticle ja, Element parent, String host, long scopeGroupId)
	{
		try
		{
			String ampURL = IterURLUtil.getRendererArticleURL(RenditionMode.amp, scopeGroupId, ja.getArticleId());
			if (Validator.isNotNull(ampURL))
				addMetaTagLink(parent, REL_AMP, host.concat(ampURL));
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	private String setCanonicalUrl( String host, String articleId, long scopeGroupId, HttpServletRequest request ) throws PortalException, SystemException, ServiceError, SecurityException, NoSuchMethodException
	{
		Document canonicalURLDom = IterURLUtil.getArticleInfoByLayoutUUID(scopeGroupId, articleId, null, false);
		String canonicalURL = host + XMLHelper.getStringValueOf(canonicalURLDom, "/rs/row/@canonicalURL", StringPool.BLANK);
		request.setAttribute(IterKeys.REQUEST_ATTRIBUTE_CANONICAL_URL, canonicalURL);
		
		return canonicalURL;
	}
}