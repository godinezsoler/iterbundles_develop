/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/

/**
 * IMPORTANTE!! paquete com.protecmedia.iter.base.service.util se coloca en este caso dentro de /Service 
 * para que se incluya en el Jar del base-portlet-service y sea accesible al resto de portlets. 
 * 
 * 
 */

package com.protecmedia.iter.base.service.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.util.CookieConstants;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.theme.ThemeConstants;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.journal.model.JournalStructureConstants;
import com.liferay.portlet.journal.model.JournalTemplateConstants;

/**
 * 
 * @author eduardo
 * Clase de definiciones de claves del producto
 * 
 */
public class IterKeys 
{
	// Propiedades del fichero de configuración en caliente (hotconfig.xml)
	public static final String HOTCONFIG_KEY_APACHE_QUEUE_MAXOPERATIONS						= "apache.queue.maxoperations";
	public static final String HOTCONFIG_KEY_APACHE_QUEUE_ACTIVE							= "apache.queue.active";
	public static final String HOTCONFIG_KEY_APACHE_QUEUE_OFFLINETIMEOUT					= "apache.queue.offlinetimeout";
	public static final String HOTCONFIG_KEY_APACHE_QUEUE_KEEPALIVE_TIMEOUT					= "apache.queue.keepalive.timeout";
	public static final String HOTCONFIG_KEY_ADVERTISEMENT_PUBLICH_DELETE_FILES				= "advertisement.publish.delete.files";
	public static final String HOTCONFIG_KEY_LOGIN_ALOUMD5 									= "iter.login.aloumd5";
	public static final String HOTCONFIG_KEY_REFRESH_CACHE_SYNCHRONOUSLY 					= "refresh.cache.synchronously";
	public static final String HOTCONFIG_KEY_PUBLICATION_PROXY_SPLIT_PKG					= "iter.publication-proxy.split-pkg.size";
	
	
	//Propiedades en portal-ext.properties
	public static final String PORTAL_PROPERTIES_KEY_ITER_WEBCONTENT_ENABLED							= "iter.webcontentdisplay.enabled";
	public static final String PORTAL_PROPERTIES_KEY_ITER_ENVIRONMENT 									= "iter.environment";
	public static final String PORTAL_PROPERTIES_KEY_ITER_STAGE		 									= "iter.stage";
	public static final String PORTAL_PROPERTIES_KEY_ITER_NEWS_PAGECONTENT_ENABLE_ORDER 				= "iter.news.pagecontent.enableOrder";
	public static final String PORTAL_PROPERTIES_KEY_ITER_NEWS_PAGECONTENT_COMPATIBILITY_ADDPAGECONTENT	= "iter.news.pagecontent.compatibility.addPageContent";
	public static final String PORTAL_PROPERTIES_KEY_ITER_GROUP_CFG_ENABLE_PUBLICATIONDATE				= "iter.group.cfg.enablePublicationdate";
	public static final String PORTAL_PROPERTIES_KEY_ITER_QUERY_OUTDATED_VALUE							= "iter.query.outdated.value";
	public static final String PORTAL_PROPERTIES_KEY_ITER_QUERY_OUTDATED_UNIT							= "iter.query.outdated.unit";
	public static final String PORTAL_PROPERTIES_KEY_ITER_QUERY_OUTDATED_NUMINTERVALS					= "iter.query.outdated.numIntervals";
	public static final String PORTAL_PROPERTIES_KEY_ITER_FLEX_PORTLETS_DEBUG							= "iter.flexportlets.debug";
	public static final String PORTAL_PROPERTIES_KEY_ITER_PORTLET_CONFIGURATION_FLEX_RELATED_VIEWER		= "iter.portlet.configuration.flex.related";
	public static final String PORTAL_PROPERTIES_KEY_ITER_FLEX_PORTLETS_REGISTRY	 					= "iter.flex.portlets.registry";
	public static final String PORTAL_PROPERTIES_KEY_ITER_RSS_PORTLET_XSL_FOR	 						= "iter.RSS-portlet.xsl-for%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_RSS_PORTLET_OUT_CONTENTTYPE_FOR				= "iter.RSS-portlet.out-contenttype-for%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_RSS_PORTLET_OUT_ENCODING_FOR 					= "iter.RSS-portlet.out-encoding-for%s";
	public static final String PORTAL_PROPERTIES_KEY_TASK_WAIT 											= "iter.statistics.collector.wait";
	public static final String PORTAL_PROPERTIES_KEY_SEARCH_MULTISITE 									= "iter.search.multisite";
	public static final String PORTAL_PROPERTIES_KEY_XMLIO_BLOCKING_PUBLICATION							= "iter.xmlio.blockingPublication";
	public static final String PORTAL_PROPERTIES_KEY_ITER_IMGIMPORT_MAXWIDTH							= "iter.imgimport.maxwidth";
	public static final String PORTAL_PROPERTIES_KEY_ITER_IMGIMPORT_MAXHEIGHT							= "iter.imgimport.maxheight";
	public static final String PORTAL_PROPERTIES_KEY_ITER_COOKIE_EXPIRES_DEFAULT 						= "iter.cookie.expires.default";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_CONN_TIMEOUT 					= "iter.subscription.server.conexiontimeout%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_READ_TIMEOUT 					= "iter.subscription.server.responsetimeout%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_URL 								= "iter.subscription.server.url%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_AUTHENTICATION_URL 							= "iter.authentication.server.url%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_AUTHENTICATION_ON_REGISTER		= "iter.subscription.authentication.on-register%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_AUTHENTICATION_ON_FAILED_CONTINUE= "iter.subscription.authentication.on-failed.continue%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_REFRESHUSERENTITLEMENTS_ON_FAILED= "iter.subscription.refreshuserentitlements.on-failed.redirect%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SCALE_ON_THE_FLY_PREFEREDMODE					= "iter.image.scale-on-the-fly.preferedmode";
	public static final String PORTAL_PROPERTIES_KEY_ITER_RATING_CONTENT_TYPE							= "iter.content-rating.content-type";
	public static final String PORTAL_PROPERTIES_KEY_ITER_RSS_PORTLET_IMAGE_FOR 						= "iter.RSS-portlet.image-for%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SERVERS_URLS 									= "iter.servers.urls";	
	public static final String PORTAL_PROPERTIES_KEY_ITER_SERVERS_CLEAR_OWNCACHE 						= "iter.servers.clear-owncache";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SEARCH_FRIENDLY_URL 							= "iter.search.results.friendlyurl";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SEARCH_TYPE_BASIC 							= "iter.search.basic.type";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SEARCH_FUZZY 									= "iter.search.fuzzy.search";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SERVER_PORT 									= "iter.server.port";
	public static final String PORTAL_PROPERTIES_KEY_ITER_USER_EXPIRES									= "iter.user.expires.for.%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_USER_NO_AVATAR_IMAGE							= "iter.noavatar.image-for.%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_USER_RESET_CREDENTIALS_TIME					= "iter.user.reset.credentials.time.for.%s";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIAL_MANAGER_FACEBOOK_IAPROCESSINGWAIT      = "iter.social.manager.facebook.iaprocessingwait";
			
	// Recolección de estadísticas
	public static final int NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK = 5;	// minutos para el siguiente intento de conextion con la red social en caso de caida
	public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_HISTORY_INSERT 			 = "iter.socialstatistic.history.insert";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_HISTORY_REMOVE			 = "iter.socialstatistic.history.remove";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_HISTORY_INTERVAL			 = "iter.socialstatistic.history.interval";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TIMEOUT_CONNECT			 = "iter.socialstatistic.timeout.connect";
	public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TIMEOUT_READ				 = "iter.socialstatistic.timeout.read";
	
	// Disqus
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_MAXTHREADS      = "iter.socialstatistic.disqus.maxthreads";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_MAXARTICLES	 = "iter.socialstatistic.disqus.maxarticles";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_QUOTA			 = "iter.socialstatistic.disqus.ratelimit";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_DELAY			 = "iter.socialstatistic.disqus.delay";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_TIMEOUT_CONNECT = "iter.socialstatistic.disqus.timeout.connect";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_TIMEOUT_READ	 = "iter.socialstatistic.disqus.timeout.read";
		
	// Facebook
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_MAXTHREADS    	= "iter.socialstatistic.facebook.maxthreads";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_MAXARTICLES   	= "iter.socialstatistic.facebook.maxarticles";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_QUOTA		 	= "iter.socialstatistic.facebook.ratelimit";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_DELAY		 	= "iter.socialstatistic.facebook.delay";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_TIMEOUT_CONNECT	= "iter.socialstatistic.facebook.timeout.connect";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_TIMEOUT_READ		= "iter.socialstatistic.facebook.timeout.read";
		
	// Twitter
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_MAXTHREADS 	    = "iter.socialstatistic.twitter.maxthreads";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_MAXARTICLES	    = "iter.socialstatistic.twitter.maxarticles";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_QUOTA			 	= "iter.socialstatistic.twitter.ratelimit";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_DELAY			 	= "iter.socialstatistic.twitter.delay";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_TIMEOUT_CONNECT	= "iter.socialstatistic.twitter.timeout.connect";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_TIMEOUT_READ		= "iter.socialstatistic.twitter.timeout.read";
		
	// Google+
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_MAXTHREADS	 	= "iter.socialstatistic.googleplus.maxthreads";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_MAXARTICLES 	= "iter.socialstatistic.googleplus.maxarticles";	
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_QUOTA		 	= "iter.socialstatistic.googleplus.ratelimit";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_DELAY 		 	= "iter.socialstatistic.googleplus.delay";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_TIMEOUT_CONNECT= "iter.socialstatistic.googleplus.timeout.connect";
		public static final String PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_TIMEOUT_READ	= "iter.socialstatistic.googleplus.timeout.read";
	// Recolección de estadísticas
	
	
	public static final String PORTAL_PROPERTIES_KEY_ITER_MAIL_SMTP_TIMEOUT 							= "iter.mail.smtp.timeout";
	public static final String PORTAL_PROPERTIES_KEY_ITER_MAIL_SMTP_CONNECTIONTIMEOUT 					= "iter.mail.smtp.connectiontimeout";
	
	public static final String PORTAL_PROPERTIES_KEY_ITER_ENABLE_CSS_PORTLETS 							= "iter.portlets.enable.css";

	
	public static final String PORTAL_PROPERTIES_KEY_ITER_METALOCATOR_RESPONSE_CONTENT_TYPE 			= "iter.metalocator.contenttype";
	
	public static final String PORTAL_PROPERTIES_KEY_ITER_PAYWALL_DATACLEANUP_FREQUENCY					= "iter.paywall.datacleanup.frequency";
	public static final String PORTAL_PROPERTIES_KEY_ITER_PAYWALL_DATACLEANUP_SCHEDULE					= "iter.paywall.datacleanup.schedule";
	public static final String PORTAL_PROPERTIES_KEY_ITER_PAYWALL_DATACLEANUP_THRESHOLD					= "iter.paywall.datacleanup.threshold";
	
	public static final Integer TASK_WAIT_DEFAULT = 120;
	
	//CKEditor
	public static final String CKEDITOR_NO_INHERIT_THEME_CSS	= "ckeditor-wrapper-content";
	
	//Edit profile intents
	public static final String EDIT_PROFILE_INTENT_REGISTRY	= "registry";
	public static final String EDIT_PROFILE_INTENT_EDITION	= "edition";
	public static final String EDIT_PROFILE_INTENT_FORGOT	= "forgot";
	public static final String EDIT_PROFILE_INTENT_CONFIRM	= "confirm";
	
	public static final String INTENT_CONFIRM_VERSION		= "0";
	public static final String INTENT_FORGOT_VERSION		= "0";
	
	//Designer PageTemplates
	public static final String DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE	=	"article-template";
	
	//Roles
	public static final String ROLE_TYPE_SYSTEM	= "admin";
	public static final String ROLE_TYPE_USER	= "user";
	
	// XSL transform
	public static final String XSL_FORM_FOLDER	= "form_xsl";
	
	// Disqus
	public static final String DISQUS_IDENTIFIER_TYPE_URL												= "disqus_url";
	public static final String DISQUS_IDENTIFIER_TYPE_ID												= "disqus_identifier";
	public static final String DISQUS_LOGIN_DLFILENTRY_FOLDER											= "disqus_login_image";
	
	//Advertisement
	public static final String ADVERTISEMENT_SKINS_FOLDER 	= DLFolderConstants.FOLDER_NAME_ADIMAGE;
	public static final String TRAD_SECTIONNAME_TRAD 		= "XYZ_SECTIONNAME_ZYX";
	public static final String TRAD_CATEGORYNAME_TRAD 		= "XYZ_CATEGORYNAME_ZYX";
	public static final String TRAD_SLOTNAME_TRAD  			= "XYZ_SLOTNAME_ZYX";
	public static final String TRAD_TAGNAME_TRAD  			= "XYZ_TAGNAME_ZYX";
	public static final String TRAD_SKINNAME_TRAD   		= "XYZ_SKINNAME_ZYX";
	public static final String TRAD_DEFAULT_VALUE_TRAD   	= "XYZ_DEFAULT_VALUE_ZYX";

	//SMPT references
	public static final String TRAD_SCHEDULE_NEWSLETTER_TRAD 		= "XYZ_SCHEDULE_NEWSLETTER_ZYX";
	public static final String TRAD_FORM_TRAD 						= "XYZ_FORM_ZYX";
	public static final String TRAD_REGISTER_FORGOT_USER_TRAD 		= "XYZ_REGISTER_FORGOT_USER_ZYX";
	
	//Request attributes
	public static final String REQUEST_ATTRIBUTE_ORIGINAL_REQUEST_URI 		= "0r161N4LR3qU357uR1";
	public static final String REQUEST_PARAMETER_FLEX_DEBUG 				= "debug";
	public static final String REQUEST_PARAMETER_FACEBOOK_LANGUAGE 			= "FacebookLanguage";
	public static final String REQUEST_ATTRIBUTE_CANONICAL_URL 				= WebKeys.REQUEST_ATTRIBUTE_CANONICAL_URL;
	public static final String REQUEST_ATTRIBUTE_ROOT_CONTEXT 				= "rootContext";
	public static final String REQUEST_ATTRIBUTE_IS_FORWARDED_PAGE 			= "forwardedPage";
	public static final String REQUEST_PARAMETER_SWFNAME					= "swfName";
	public static final String REQUEST_PARAMETER_INCLUDE_HEAD_JSP			= "includeHeadJSP";
	public static final String REQUEST_PARAMETER_INCLUDE_BODY_JSP			= "includeBodyJSP";
	public static final String REQUEST_PARAMETER_CATALOG_PAGE_URL			= "catalogpageurl";
	public static final String REQUEST_PARAMETER_CATALOG_PAGEID				= "catalogpageid";
	public static final String REQUEST_PARAMETER_CATALOG_SCOPEGROUPID		= "scopegroupid";
	public static final String REQUEST_PARAMETER_CATALOG_NAME				= "catalogname";
	public static final String REQUEST_PARAMETER_CATALOG_PAGE_NAME			= "catalogpagename";
	public static final String REQUEST_PARAMETER_DEFAULT_PLID				= "defaultplid";
	public static final String REQUEST_PARAMETER_CALL_FROM_MLN				= "callfromMln";
	public static final String REQUEST_PARAMETER_LOGIN						= "login";
	public static final String REQUEST_PARAMETER_PASSWORD					= "password";
	public static final String REQUEST_ATTRIBUTE_COOKIE_ID					= "c00k131d";
	public static final String REQUEST_ATTRIBUTE_FACETS						= "facets";
	public static final String REQUEST_ATTRIBUTE_SSO_DESTINATION_URL		= "iterssodestinationurl";
	
	//Expando column
	public static final String EXPANDO_COLUMN_NAME_SCOPEGROUPID = "expandoScopeGroupId";	
	
	//Newsletters
	// NEWSLETTER_SCHEDULE_TYPE_DEFAULT y NEWSLETTER_SCHEDULE_TYPE_DEFAULT_XML son lo mismo, se utiliza el segundo
	// en NewsletterMgrLocalServiceUtil.getNewslettersXML para NewsletterPortletMgr.getNewslettersXML
	public static final String NEWSLETTER_SCHEDULE_TYPE_DEFAULT 		= "default";
	public static final String NEWSLETTER_SCHEDULE_TYPE_ALERT 			= "alert";
	public static final String NEWSLETTER_SCHEDULE_TYPE_DEFAULT_XML		= "scheduled";
	
	//
	public static final String DELETE_CACHE_URL = "/delete-cache";
	
	//
	public static final String ROBOTS = "ROBOTS";
	
	//Grupos
	public static final String GLOBAL_GROUP_NAME = GroupConstants.GLOBAL_GROUP_NAME;
	public static final String GUEST_GROUP_NAME = "Guest";
	public static final String CONTROL_PANEL_GROUP_NAME = "Control Panel";
	
	// javascript privados
	public static final String PREFIX_PRIVATE_JS = "iter_js_";
	
	/*
	 * Contenidos
	 */
	
	// Árboles
	public static final String TREE_SOURCE_DATA_PROPERTY 	= "treeSourceData";
	public static final String TREE_SOURCE_METADATA 		= "treeSourceMetadata";
	public static final String TREE_SOURCE_LAYOUT 			= "treeSourceLayout";
	public static final String TREE_SOURCE_ALL_LAYOUTS 		= "treeSourceAllLayouts";
	public static final String TREE_TOP_LEVEL 				= "top-level";
	public static final String TREE_NO_TOP_LEVEL 			= "no-top-level";
	public static final String TREE_SELECTED_NODE 			= "selected-node";
	public static final String TREE_NODE 					= "node";
	
	// Contenidos de ejemplo
	public static final String EXAMPLEARTICLEID = "EXAMPLE-ARTICLE";
	public static final String EXAMPLEGALLERYID = "EXAMPLE-GALLERY";
	public static final String EXAMPLEPOLLID = "EXAMPLE-POLL";
	public static final String EXAMPLEMULTIMEDIAID = "EXAMPLE-MULTIMEDIA";
	
	public static final String [] EXAMPLE_IDS = {EXAMPLEARTICLEID, EXAMPLEGALLERYID, EXAMPLEPOLLID, EXAMPLEMULTIMEDIAID};
	
	// Id para la estructura de milenium
	public static final String STRUCTURE_ARTICLE 			= JournalStructureConstants.STRUCTURE_ARTICLE;
	public static final String STRUCTURE_GALLERY 			= JournalStructureConstants.STRUCTURE_GALLERY; 
	public static final String STRUCTURE_POLL 				= JournalStructureConstants.STRUCTURE_POLL;
	public static final String STRUCTURE_MULTIMEDIA 		= JournalStructureConstants.STRUCTURE_MULTIMEDIA;
	public static final String STRUCTURE_INSTRUMENTAL 		= JournalStructureConstants.STRUCTURE_INSTRUMENTAL;
	
	public static final String [] MILENIUM_STRUCTURES 		= {STRUCTURE_ARTICLE, STRUCTURE_GALLERY, STRUCTURE_POLL, STRUCTURE_MULTIMEDIA};
		
	// Structure values	
	public static final String STRUCTURE_ARTICLE_DESCRIPTION = "Standard Article";
	public static final String STRUCTURE_ARTICLE_NAME = "Standard Article";	
			
	public static final String STRUCTURE_GALLERY_DESCRIPTION = "Standard Gallery";
	public static final String STRUCTURE_GALLERY_NAME = "Standard Gallery";
			
	public static final String STRUCTURE_POLL_DESCRIPTION = "Standard Poll";
	public static final String STRUCTURE_POLL_NAME = "Standard Poll";
			
	public static final String STRUCTURE_MULTIMEDIA_DESCRIPTION = "Standard Multimedia";
	public static final String STRUCTURE_MULTIMEDIA_NAME = "Standard Multimedia";
	
	// Example Article values	
	public static final String EXAMPLEARTICLE_TITLE = "Example Article";
	public static final String EXAMPLEGALLERY_TITLE = "Example Gallery";
	public static final String EXAMPLEPOLL_TITLE = "Example Poll";
	
	// Article Structure Fields
	public static final String STANDARD_ARTICLE_HEADLINE 			= JournalStructureConstants.STANDARD_ARTICLE_HEADLINE;
	public static final String STANDARD_ARTICLE_SUBHEADLINE 		= JournalStructureConstants.STANDARD_ARTICLE_SUBHEADLINE;
	public static final String STANDARD_ARTICLE_TEXT 				= JournalStructureConstants.STANDARD_ARTICLE_TEXT;
	public static final String STANDARD_ARTICLE_BYLINE 				= JournalStructureConstants.STANDARD_ARTICLE_BYLINE;
	public static final String STANDARD_ARTICLE_IMAGE_LOW 			= JournalStructureConstants.STANDARD_ARTICLE_IMAGE_LOW;
	public static final String STANDARD_ARTICLE_IMAGE_MEDIUM 		= JournalStructureConstants.STANDARD_ARTICLE_IMAGE_MEDIUM;
	public static final String STANDARD_ARTICLE_IMAGE_HIGH 			= JournalStructureConstants.STANDARD_ARTICLE_IMAGE_HIGH;
	public static final String STANDARD_ARTICLE_IMAGE_CUTLINE		= JournalStructureConstants.STANDARD_ARTICLE_IMAGE_CUTLINE;
	public static final String STANDARD_ARTICLE_IMAGE_BYLINE		= JournalStructureConstants.STANDARD_ARTICLE_IMAGE_BYLINE;
	public static final String STANDARD_ARTICLE_GEOLOCATION 		= JournalStructureConstants.STANDARD_ARTICLE_GEOLOCATION;
	public static final String STANDARD_ARTICLE_MULTIMEDIA 			= JournalStructureConstants.STANDARD_ARTICLE_MULTIMEDIA;
	public static final String STANDARD_ARTICLE_MULTIMEDIA_CUTLINE 	= JournalStructureConstants.STANDARD_ARTICLE_MULTIMEDIA_CUTLINE;
	public static final String STANDARD_ARTICLE_MULTIMEDIA_BYLINE 	= JournalStructureConstants.STANDARD_ARTICLE_MULTIMEDIA_BYLINE;
	public static final String STANDARD_ARTICLE_MULTIMEDIA_PREVIEW 	= JournalStructureConstants.STANDARD_ARTICLE_MULTIMEDIA_PREVIEW;
	public static final String STANDARD_ARTICLE_SOURCE 				= JournalStructureConstants.STANDARD_ARTICLE_SOURCE;
	public static final String STANDARD_ARTICLE_EXTENSION 			= JournalStructureConstants.STANDARD_ARTICLE_EXTENSION;
	public static final String STANDARD_ARTICLE_QUESTION 			= JournalStructureConstants.STANDARD_ARTICLE_QUESTION;
	public static final String STANDARD_ARTICLE_LEAD 				= JournalStructureConstants.STANDARD_ARTICLE_LEAD;
	
	//TODO: Añadir el resto de campos de las estructuras para poder acceder a ellos de forma controlada
	
	//Article Index Fields
	public static final String STANDARD_ARTICLE_INDEX_GEOLOCATION = "geolocation";
	public static final String STANDARD_ARTICLE_INDEX_STRUCTUREID = "structureId";
	public static final String STANDARD_ARTICLE_INDEX_LAYOUTSPLID = "layoutsPlid";
	public static final String STANDARD_ARTICLE_INDEX_STARTVALIDITY = "startValidity";
	
	// Operaciones lógicas en las consultas para obtener contenidos
	public static final int LOGIC_IGNORE= -1;
	public static final int LOGIC_AND = 0;
	public static final int LOGIC_OR = 1;
	public static final int LOGIC_NOT = 2;
		
	// Constantes para el origen de los artículos en los teasers
	public static final String SOURCE_CURRENT= "current";
	public static final String SOURCE_DEFAULT= "default";
	public static final String SOURCE_ALL= "all";
	
	// Constantes para el orden de los teaser viewers
	public static final int ORDER_STARTDATE 		= Sort.ORDERBY_STARTDATE;
	public static final int ORDER_ORDEN 			= 1;
	public static final int ORDER_DISPLAYDATE 		= 2;
	public static final int ORDER_EXPIRATIONDATE 	= 3;
	public static final int ORDER_MODIFICATIONDATE 	= Sort.ORDERBY_MODIFICATIONDATE;
	public static final int ORDER_VIEW 				= 5;
	public static final int ORDER_RATINGS 			= 6;
	public static final int ORDER_SENT 				= 7;
	public static final int ORDER_COMMENT 			= 8;
	public static final int ORDER_SHARED 			= 11;
	public static final int ORDER_QUALIFICATION 	= 13;
	
	// Constantes para el orden de los article topics
	public static final int ORDER_NUM_ARTICLES = 9;
	public static final int ORDER_ALPHABETICAL = 10;
	
	// Constante para el orden de los related viewers. Ordena artículos relacionados según orden indicado en MLN
	public static final int ORDER_MLN = 12;
	
	// Constantes para orden de sentencias SQL
	public static final int ORDER_ASC  = Sort.ORDERTYPE_ASC;
	public static final int ORDER_DESC = Sort.ORDERTYPE_DESC;
	
	// Operaciones para las estadísticas
	public static final int OPERATION_VIEW = 0;
	public static final int OPERATION_RATINGS = 1;
	public static final int OPERATION_RECENT = 2;
	public static final int OPERATION_SENT = 3;
	
	//Constantes para los relacionados
	public static final String INTERNAL_LINK = "InternalLink";
	public static final String EXTERNAL_LINK = "ExternalLink";
	
	public static final String INTERNAL_LINK_TYPE_CLASS = "internal-link";
	public static final String EXTERNAL_LINK_TYPE_CLASS = "external-link";
	public static final String METADATA_LINK_TYPE_CLASS = "metadata-link";
	
	public static final String METADATA_LINK_CLASS = "meta";

	@Deprecated
	public static final int OPERATION_COMMENT = 100;
	public static final int OPERATION_SHARED = 11;
	public static final int OPERATION_FB = 20;
	public static final int OPERATION_TW = 30;
	public static final int OPERATION_GP = 40;
	public static final int OPERATION_DISQUS = 50;
	
	public static final Map<String, List<String>> ORDER_TO_OPERATION;
    static
    {
    	ORDER_TO_OPERATION = new HashMap<String, List<String>>();
    	ORDER_TO_OPERATION.put(Integer.toString(ORDER_VIEW),			Arrays.asList(new String[]{	Integer.toString(OPERATION_VIEW)}));
    	ORDER_TO_OPERATION.put(Integer.toString(ORDER_RATINGS), 		Arrays.asList(new String[]{	Integer.toString(OPERATION_RATINGS)}));
    	ORDER_TO_OPERATION.put(Integer.toString(ORDER_COMMENT), 		Arrays.asList(new String[]{	Integer.toString(OPERATION_DISQUS)}));
    	ORDER_TO_OPERATION.put(Integer.toString(ORDER_SENT), 			Arrays.asList(new String[]{	Integer.toString(OPERATION_SENT)}));
    	ORDER_TO_OPERATION.put(Integer.toString(ORDER_SHARED), 			Arrays.asList(new String[]{	Integer.toString(OPERATION_SHARED)}));
    }
	
	// Prefijos de los templates de contenidos
	public static final String TEASER 			= JournalTemplateConstants.FREFIX_TEASER;
	public static final String TEMPLATE_CONTENT = JournalTemplateConstants.FREFIX_CONTENT;
	
	// Dimensiones por defecto para imagenes de usuario
	public static final int USERS_IMAGE_MAX_HEIGHT 	= 500;
	public static final int USERS_IMAGE_MAX_WIDTH 	= 500;
	
	//Visor por defecto
	public static final String DEFAULT_VIEWER_NAME 	= "Default Viewer";
	public static final String DEFAULT_VIEWER_URL 	= "/default-viewer";

	// Layout por defecto
	public static final String DEFAULT_LAYOUT_NAME 	= "Home";
	public static final String DEFAULT_LAYOUT_URL 	= "/home";
	
	// Tema por defecto
	public static final String DEFAULT_THEME = ThemeConstants.DEFAULT_THEMEID;
	
	// Nombres de portlets
	public static final String TEASER_VIEWER_PORTLET_NAME = "teaserviewerportlet_WAR_newsportlet";
	public static final String CONTENT_VIEWER_PORTLET_INSTANCE = "contentviewerportlet_WAR_newsportlet_INSTANCE_news";
	
	//Usuario por defecto
	public static final String DEFAULT_USER_EMAIL = "default@liferay.com";
	
	/*
	 * Portlet Base
	 */
	// System Name Default
	public static final String SYSTEM_NAME_DEFAULT = "ITERWebcms";
	
	// System Version
	public static final String SYSTEM_VERSION = "0";
	
	// Products
	public static final String [] SYSTEM_PRODUCTS = {"Advertisement", "Delivery", "Demo", "Designer", "News", "Search", "Service", "Statistics"};
	  
	//Licencia
	public static final String PRODUCT_VERSION = "20.0";
	public static final String PRODUCT_NAME = "ITERWEBCMS";
	public static final String LICENSE_DATE_FORMAT = "yyyy-MM-dd";
	
	/*
	 * Layout Types
	 */
	// 
	public static final String CUSTOM_TYPE_NEWSLETTER = "newsletter";
	public static final String CUSTOM_TYPE_TEMPLATE = "template";
	public static final String CUSTOM_TYPE_PORTLET = "portlet";
	public static final String CUSTOM_TYPE_CATALOG = "catalog";

	/*
	 * Plugins tipo portlet
	 */
	public static final String ADVERTISEMENT_PLUGIN = "ITER.advertisement";
	public static final String DESIGNER_PLUGIN = "ITER.designer";
	public static final String NEWS_PLUGIN = "ITER.news";
	public static final String SEARCH_PLUGIN = "ITER.search";
	public static final String SERVICE_PLUGIN = "ITER.service";
	public static final String TRACKING_PLUGIN = "ITER.tracking";
	
	/*
	 * Constantes para el Live
	 */
	//operations
	public static final String CREATE = "create";
	public static final String UPDATE = "update";
	public static final String DELETE = "delete";	
	
	//status
	public static final String PENDING = "pending";
	public static final String DONE = "done";
	public static final String DRAFT = "draft";
	public static final String INTERRUPT = "interrupt";
	public static final String ERROR = "error";
	public static final String CORRUPT = "corrupt";
	public static final String PROCESSING = "processing";
	
	
	/*
	 * Portlet Delivery
	 */
	public static final int TYPE_USER = 0; 
	public static final int TYPE_GROUP_USER = 1;
	public static final int TYPE_ROLE = 2;
	public static final int TYPE_ORGANIZATION = 3;
	public static final int TYPE_COMMUNITY = 4;
	
	public static final String [] TYPES = {"TYPE_USER", "TYPE_GROUP_USER", "TYPE_ROLE", "TYPE_ORGANIZATION", "TYPE_COMMUNITY"};
	
	/*
	 * User/Content Access
	 */
	public static final String USER_ACCESS_LEVEL_VOCABULARY = "Iter Access";
	public static final String USER_ACCESS_LEVEL_VALUE_PROPERTY = "Value";
	public static final String USER_ACCESS_TEMPLATE_SUFFIX = "-NO-ACCESS";
	public static final String USER_ACCESS_GENERAL_TEMPLATE = "GENERAL-NO-ACCESS";
	
	/*
	 * User Role
	 */
	public static final String ROLE_ADMINISTRATOR = "Administrator";	
	
	/*
	 * PHP Variables
	 */
	public static final String PHP_VARIABLE_ENCRYPT_KEY = "encryptKey";
	public static final String PHP_VARIABLE_USER_ID = "userId";
	public static final String PHP_VARIABLE_USER_EMAIL = "userEmail";
	public static final String PHP_VARIABLE_USER_NAME = "userName";
	public static final String PHP_VARIABLE_USER_ACCESS = "userAccess";
	public static final String PHP_VARIABLE_USER_FAV = "userFav";
	public static final String PHP_VARIABLE_USER_GEO = "userGeo";
	public static final String PHP_VARIABLE_USER_NEWSLETTER = "userNewsletter";
	
	/*
	 * Cookies
	 */
	public static final String COOKIE_NAME							= "COOKIE_NAME";
	public static final String COOKIE_NAME_USER_DOGTAG 				= CookieConstants.COOKIE_NAME_USER_DOGTAG;
	public static final String COOKIE_NAME_USERID_DOGTAG            = "USERID-DOGTAG";
	public static final String COOKIE_NAME_USER_ID 					= CookieConstants.COOKIE_NAME_USER_ID;
	public static final String COOKIE_NAME_USER_EMAIL 				= "USER_EMAIL";
	public static final String COOKIE_NAME_USER_NAME 				= "USER_NAME";
	public static final String COOKIE_NAME_USER_ACCESS 				= "USER_ACCESS";
	public static final String COOKIE_NAME_USER_FAV 				= "USER_FAV";
	public static final String COOKIE_NAME_USER_GEO 				= "USER_GEO";
	public static final String COOKIE_NAME_USER_NEWSLETTER  		= "USER_NEWSLETTER";
	public static final String COOKIE_ELEMENT_SEPARATOR				= ";";
	public static final String COOKIE_NAME_ALREADY_RATED 			= "ITR_COOKIE_Hdsf78231nlkasfe%s9CzB348NSR59";
	public static final String COOKIE_NAME_SOCIAL_REDIRECT			= "Bdsf78231nlkaS0C1ALCzB348NSR59";
	public static final String COOKIE_NAME_VERSION					= "VERSION";
	public static final String COOKIE_NAME_EXPIRE					= "EXPIRE";
	public static final String COOKIE_NAME_INTENT					= "INTENT";
	public static final String COOKIE_NAME_EXTRADATA				= "EXTRADATA";
	public static final String COOKIE_NAME_TIMESTAMP				= "TIMESTAMP";
	public static final String COOKIE_NAME_PRODUCTS					= "PRODUCTS";
	public static final String COOKIE_NAME_CHECKSUM					= "CHECKSUM";
	public static final String COOKIE_NAME_USR_1ST_NAME				= "USR_1ST_NAME";
	public static final String COOKIE_NAME_USR_LASTNAME				= "USR_LASTNAME";
	public static final String COOKIE_NAME_USR_LASTNAME_2			= "USR_LASTNAME_2";
	public static final String COOKIE_NAME_USR_AVATAR_URL			= "USR_AVATAR_URL";
	public static final String COOKIE_NAME_ABO_ID					= "ABO_ID";
	public static final String COOKIE_NAME_SESS_ID					= "SESS_ID";
	public static final String COOKIE_NAME_ITR_COOKIE_USRID			= "ITR_COOKIE_USRID";
	public static final String COOKIE_NAME_CONTENT_VOTED			= "ITR_COOKIE_%s%s";
	// Version 3
	public static final String COOKIE_NAME_AGE						= "USER_AGE";
	public static final String COOKIE_NAME_BIRTHDAY					= "USER_BIRTHDAY";
	public static final String COOKIE_NAME_GENDER					= "USER_GENDER";
	public static final String COOKIE_NAME_MARITALSTATUS			= "USER_MARITALSTATUS";
	public static final String COOKIE_NAME_LANGUAGE					= "USER_LANGUAGE";
	public static final String COOKIE_NAME_COORDINATES				= "USER_COORDINATES";
	public static final String COOKIE_NAME_COUNTRY					= "USER_COUNTRY";
	public static final String COOKIE_NAME_REGION					= "USER_REGION";
	public static final String COOKIE_NAME_CITY						= "USER_CITY";
	public static final String COOKIE_NAME_ADDRESS					= "USER_ADDRESS";
	public static final String COOKIE_NAME_POSTALCODE				= "USER_POSTALCODE";
	public static final String COOKIE_NAME_TELEPHONE				= "USER_TELEPHONE";
	
	/*
	 * Flags
	 */
	public static final String FLAG_COMPLETE_USER_PROFILE			= "FLAG_COMPLETE_USER_PROFILE";
	
	/*
	 * Environments
	 */
	// 
	public static final String ENVIRONMENT_LIVE = "LIVE";
	public static final String ENVIRONMENT_PREVIEW = "PREVIEW";
	
	/*
	 * Stage
	 */
	// 
	public static final String STAGE_DEVELOPMENT = "DEVELOPMENT";
	public static final String STAGE_PRODUCTION = "PRODUCTION";
	
	/*
	 * URL Params
	 * 
	 */
	public static final String URL_PARAM_DATE_FORMAT = "yyyyMMdd";
	public static final String URL_PARAM_DATE_FORMAT_EXT_HH = "yyyyMMddHH";
	public static final String URL_PARAM_DATE_FORMAT_EXT = "yyyyMMddHHmm";	
	public static final String URL_PARAM_TAGS_SEPARATOR = "-";
	
	/*
	 * RSS Params
	 */
	public static final String RSS_FILTER_STRUCTURE = "STRUCTURE-FILTER";
	public static final String RSS_FILTER_QUALIFICATION = "QUALIFICATION-FILTER";
	public static final String RSS_FILTER_ORDER_BY = "ORDER_BY-FILTER";
	public static final String RSS_FILTER_ORDER_BY_TYPE = "ORDER_BY_TYPE-FILTER";
	public static final String RSS_FILTER_LAYOUT = "LAYOUT-FILTER";
	public static final String RSS_DESCRIPTION_TEMPLATE = "FULL-CONTENT-COMPLETE";
	
	/*
	 * Advertisement
	 */
	
	public static final String BANNERFLASH = "banner-flash";

	public static final String BANNERIMAGE = "banner-image";
	
	public static final String THEME_BANNER_POSITION_TOP = "top";
	public static final String THEME_BANNER_POSITION_BOTTOM = "bottom";
	public static final String THEME_BANNER_POSITION_LEFT = "left";
	public static final String THEME_BANNER_POSITION_RIGHT = "right";
	
	public static final String SOURCE_URL = "url";
	public static final String SOURCE_FILE = "file";	
	public static final String SOURCE_LIBRARY = "library";
	public static final String SOURCE_EDITOR = "editor";
	
	public static final String TYPE_IMAGE = "image";
	public static final String TYPE_FLASH = "flash";
	public static final String TYPE_HTML = "html";
	
	/* 
	 * XMLIO Constants
	 */
	public static final String XMLIO_VALID_INPUT_FILE_PATTERN = "([^\\s]+(\\.(?i)(zip|xml))$)";
	public static final String XMLIO_ZIP_FILE_PREFIX = "iter_";
	public static final String XMLIO_XML_MAIN_FILE_NAME = "iter.xml";
	public static final String XMLIO_XML_ELEMENT_ROOT = "iter";
	public static final String XMLIO_XML_SCOPEGROUPID_ATTRIBUTE = "scopegroupid";
	public static final String XMLIO_XML_ELEMENT_LIST = "list";
	public static final String XMLIO_XML_ELEMENT_POOL = "pool";
	public static final int XMLIO_PUBLISH_TIMEOUT_CONNECTION = 60000;
	public static final int XMLIO_PUBLISH_TIMEOUT_SOCKET = 600000;
	public static final String XMLIO_XML_IMPORT_OPERATION = "import";
	public static final String XMLIO_XML_EXPORT_OPERATION = "export";
	public static final String XMLIO_XML_PUBLISH_OPERATION = "publish";
	public static final String XMLIO_XML_MANUAL = "manual";
	public static final String XMLIO_XML_AUTOMATIC = "automatic";
	public static final String XMLIO_XML_PRODUCTS = "products";
	public static final String GROUPS2UPDT_ROOT = "groups";
	public static final String GROUPS2UPDT_NODE = "group";

	public static final String XMLIO_XML_LAST_UPDATE_ATTRIBUTE 		= "lastupdate";
	public static final String XMLIO_XML_LAST_UPDATE_ATTRIBUTE_ATTR = StringPool.AT.concat(XMLIO_XML_LAST_UPDATE_ATTRIBUTE);
	
	/*
	 * XMLIO Channel
	 */
	public static final String XMLIO_CHANNEL_TYPE_INPUT = "input";
	public static final String XMLIO_CHANNEL_TYPE_OUTPUT = "output";
	public static final String XMLIO_CHANNEL_TYPE_OUTPUT_MILENIUM = "output-milenium";
	public static final String XMLIO_CHANNEL_MODE_FTP = "ftp";
	public static final String XMLIO_CHANNEL_MODE_FILE_SYSTEM = "file-system";
	public static final int XMLIO_CHANNEL_AUTO_TIME_MILLIS = 15*60*1000;
	public static final String XMLIO_CHANNEL_RANGE_TIME_UNIT_MONTH = "month";
	public static final String XMLIO_CHANNEL_RANGE_TIME_UNIT_DAY = "day";
	public static final String XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR = "hour";
	public static final String XMLIO_CHANNEL_RANGE_TYPE_ALL = "all";
	
	/*
	 * PortletIds Constants
	 */
	public static final String PORTLETID_RANKING 	= "rankingviewerportlet_WAR_trackingportlet";
	public static final String PORTLETID_RELATED	= "relatedviewerportlet_WAR_newsportlet";
	public static final String PORTLETID_TEASER		= "teaserviewerportlet_WAR_newsportlet";
	
	/*
	 * Class Name Constants
	 */
	public static final String CLASSNAME_UNKNOWN = "CLASSNAME_UNKNOWN";
	public static final String CLASSNAME_GROUP = "com.liferay.portal.model.Group";	
	public static final String CLASSNAME_LAYOUT = "com.liferay.portal.model.Layout";
	public static final String CLASSNAME_PORTLET = "com.liferay.portal.model.Portlet";
	public static final String CLASSNAME_JOURNALSTRUCTURE = "com.liferay.portlet.journal.model.JournalStructure";
	public static final String CLASSNAME_JOURNALTEMPLATE = "com.liferay.portlet.journal.model.JournalTemplate";
	public static final String CLASSNAME_QUALIFICATION = "com.protecmedia.iter.news.model.Qualification";
	public static final String CLASSNAME_DLFOLDER = "com.liferay.portlet.documentlibrary.model.DLFolder";
	public static final String CLASSNAME_DLFILEENTRY = "com.liferay.portlet.documentlibrary.model.DLFileEntry";
	public static final String CLASSNAME_JOURNALARTICLE = "com.liferay.portlet.journal.model.JournalArticle";
	public static final String CLASSNAME_PAGECONTENT = "com.protecmedia.iter.news.model.PageContent";
	public static final String CLASSNAME_VOCABULARY = "com.liferay.portlet.asset.model.AssetVocabulary";
	public static final String CLASSNAME_CATEGORY = "com.liferay.portlet.asset.model.AssetCategory";
	public static final String CLASSNAME_CATEGORYPROPERTY = "com.liferay.portlet.asset.model.AssetCategoryProperty";
	public static final String CLASSNAME_EXPANDOTABLE = "com.liferay.portlet.expando.model.ExpandoTable";
	public static final String CLASSNAME_EXPANDOCOLUMN = "com.liferay.portlet.expando.model.ExpandoColumn";
	public static final String CLASSNAME_EXPANDOROW = "com.liferay.portlet.expando.model.ExpandoRow";
	public static final String CLASSNAME_EXPANDOVALUE = "com.liferay.portlet.expando.model.ExpandoValue";
	public static final String CLASSNAME_PAGETEMPLATE = "com.protecmedia.iter.designer.model.PageTemplate";
	public static final String CLASSNAME_SERVICE = "com.protecmedia.iter.services.model.Service";
	public static final String CLASSNAME_USERGROUP = "com.liferay.portal.model.UserGroup";
	public static final String CLASSNAME_USER = "com.liferay.portal.model.User";
	public static final String CLASSNAME_WEBSITE = "com.liferay.portal.model.Website";
	public static final String CLASSNAME_ADDRESS = "com.liferay.portal.model.Address";
	public static final String CLASSNAME_PHONE = "com.liferay.portal.model.Phone";
	public static final String CLASSNAME_EMAILADDRESS = "com.liferay.portal.model.EmailAddress";
	public static final String CLASSNAME_ENTRY = "com.liferay.portlet.asset.model.AssetEntry";	
	public static final String CLASSNAME_PRODUCT = "com.protecmedia.iter.news.model.Product";
		
	//Casos especiales. NO INCLUIR EN CLASSNAME_TYPES!!	
	public static final String CLASSNAME_MILENIUMSECTION = "com.protecmedia.iter.news.model.MileniumSection"; //para la publicacion de paginas en modo "Milenium"
	public static final String CLASSNAME_MILENIUMARTICLE =	"com.protecmedia.iter.news.model.MileniumArticle"; //para la publicacion de artículos en modo "Milenium"
	public static final String CLASSNAME_CONTACT = "com.liferay.portal.model.Contact"; //Para la creación de usuarios
	public static final String CLASSNAME_COMPANY = "com.liferay.portal.model.Company"; //Para el grupo Global.
	
	public static String[] CLASSNAME_TYPES = {CLASSNAME_GROUP, CLASSNAME_LAYOUT, CLASSNAME_QUALIFICATION, CLASSNAME_PORTLET, 
		CLASSNAME_JOURNALSTRUCTURE, CLASSNAME_DLFOLDER, CLASSNAME_DLFILEENTRY, 
		CLASSNAME_JOURNALARTICLE, CLASSNAME_PAGECONTENT, CLASSNAME_VOCABULARY, CLASSNAME_CATEGORY,
		CLASSNAME_CATEGORYPROPERTY, CLASSNAME_EXPANDOTABLE, CLASSNAME_EXPANDOCOLUMN,
		CLASSNAME_EXPANDOROW, CLASSNAME_EXPANDOVALUE, CLASSNAME_PAGETEMPLATE,
		CLASSNAME_SERVICE, CLASSNAME_USERGROUP, CLASSNAME_USER, CLASSNAME_WEBSITE, CLASSNAME_ADDRESS, CLASSNAME_PHONE,
		CLASSNAME_EMAILADDRESS, CLASSNAME_ENTRY, CLASSNAME_PRODUCT};
	
	public static String[] DELETE_CLASSNAME_TYPES = {CLASSNAME_GROUP, CLASSNAME_LAYOUT, CLASSNAME_QUALIFICATION, 
		CLASSNAME_JOURNALSTRUCTURE, CLASSNAME_DLFOLDER, CLASSNAME_DLFILEENTRY, 
		CLASSNAME_ENTRY, CLASSNAME_JOURNALARTICLE, CLASSNAME_PAGECONTENT, CLASSNAME_VOCABULARY, CLASSNAME_CATEGORY,
		CLASSNAME_CATEGORYPROPERTY, CLASSNAME_EXPANDOTABLE, CLASSNAME_EXPANDOCOLUMN,
		CLASSNAME_EXPANDOROW, CLASSNAME_EXPANDOVALUE, CLASSNAME_PAGETEMPLATE, CLASSNAME_SERVICE, 
		CLASSNAME_USERGROUP, CLASSNAME_USER, CLASSNAME_WEBSITE, CLASSNAME_ADDRESS, CLASSNAME_PHONE,
		CLASSNAME_EMAILADDRESS, CLASSNAME_PRODUCT};
	
	public static String[] GLOBAL_CLASSNAME_TYPES = {CLASSNAME_JOURNALSTRUCTURE, 
		CLASSNAME_DLFOLDER, CLASSNAME_DLFILEENTRY, CLASSNAME_JOURNALARTICLE, CLASSNAME_VOCABULARY, 
		CLASSNAME_CATEGORY,	CLASSNAME_CATEGORYPROPERTY, CLASSNAME_EXPANDOTABLE, CLASSNAME_EXPANDOCOLUMN,
		CLASSNAME_EXPANDOROW, CLASSNAME_EXPANDOVALUE, CLASSNAME_USER, CLASSNAME_WEBSITE, CLASSNAME_ADDRESS, 
		CLASSNAME_PHONE, CLASSNAME_EMAILADDRESS, CLASSNAME_ENTRY, CLASSNAME_PRODUCT};
	
	public static String[] LOCAL_CLASSNAME_TYPES = {CLASSNAME_GROUP, CLASSNAME_LAYOUT, CLASSNAME_PORTLET, 
		CLASSNAME_QUALIFICATION, CLASSNAME_PAGECONTENT, CLASSNAME_PAGETEMPLATE, CLASSNAME_SERVICE};
	
	public static String[] MAIN_CLASSNAME_TYPES_EXPORT_SPLIT_PKG = {CLASSNAME_GROUP, CLASSNAME_VOCABULARY, CLASSNAME_LAYOUT, CLASSNAME_JOURNALARTICLE};
	
	public static String[] MAIN_CLASSNAME_TYPES_EXPORT = {CLASSNAME_GROUP, CLASSNAME_LAYOUT, CLASSNAME_PAGETEMPLATE, CLASSNAME_JOURNALARTICLE, 
														  CLASSNAME_VOCABULARY, CLASSNAME_USER, CLASSNAME_SERVICE};
	
	public static String[] MAIN_CLASSNAME_TYPES_IMPORT = {CLASSNAME_GROUP, CLASSNAME_VOCABULARY, CLASSNAME_LAYOUT, CLASSNAME_PAGETEMPLATE,  
														  CLASSNAME_DLFILEENTRY, CLASSNAME_JOURNALARTICLE, CLASSNAME_USER, 
														  CLASSNAME_SERVICE};
	
	//Cambio el orden para poblar live correctamente
	public static String[] CLASSNAME_TYPES_POPULATE = {
		CLASSNAME_GROUP,
		CLASSNAME_EXPANDOTABLE, CLASSNAME_EXPANDOCOLUMN,		 
		CLASSNAME_VOCABULARY, CLASSNAME_CATEGORY, 
		CLASSNAME_LAYOUT, 
		CLASSNAME_QUALIFICATION, 
		CLASSNAME_PORTLET, 
		CLASSNAME_JOURNALSTRUCTURE, 
		CLASSNAME_DLFOLDER, 
		CLASSNAME_DLFILEENTRY, 
		CLASSNAME_JOURNALARTICLE,
		CLASSNAME_USERGROUP, CLASSNAME_USER, CLASSNAME_WEBSITE, CLASSNAME_ADDRESS, CLASSNAME_PHONE,CLASSNAME_EMAILADDRESS, 
		CLASSNAME_PAGECONTENT, 
		CLASSNAME_PAGETEMPLATE, 
		CLASSNAME_SERVICE, 
		CLASSNAME_PRODUCT};
	
	
	/* 
	 * ClassName for AssetEntry 
	 */
	//TODO: Añadir CLASSNAME_DLFILEENTRY
	public static String[] ENTRY_CLASSNAME_TYPES = {CLASSNAME_USER, 
		CLASSNAME_JOURNALARTICLE, CLASSNAME_GROUP};
	
	/* 
	 * ClassName for CustomFields 
	 */
	//TODO: Añadir CLASSNAME_DLFILEENTRY
	public static String[] CUSTOMFIELD_CLASSNAME_TYPES = {CLASSNAME_LAYOUT, CLASSNAME_USER, 
		CLASSNAME_JOURNALARTICLE};
	
	/* 
	 * ClassName para Publicabilidad.
	 * Estos classname controlan la publicabilidad de sus elementos padre 
	 */
	public static String[] CUSTOMFIELD_CLASSNAME_DRAFTTOPENDING = {CLASSNAME_PAGECONTENT};
	
	/* These are pending CustomFields. We don't know if its implementation if is really necessary 
	 * 
	public static final String CLASSNAME_ORGANIZATION = "com.liferay.portal.model.Organization";	
	public static final String CLASSNAME_BLOGSENTRY = "com.liferay.portlet.blogs.model.BlogsEntry";
	public static final String CLASSNAME_BOOKMARKSENTRY = "com.liferay.portlet.bookmarks.model.BookmarksEntry";
	public static final String CLASSNAME_BOOKMARKSFOLDER = "com.liferay.portlet.bookmarks.model.BookmarksFolder";
	public static final String CLASSNAME_CALEVENT = "com.liferay.portlet.calendar.model.CalEvent";
	public static final String CLASSNAME_IGFOLDER = "com.liferay.portlet.imagegallery.model.IGFolder";
	public static final String CLASSNAME_IGIMAGE = "com.liferay.portlet.imagegallery.model.IGImage";
	public static final String CLASSNAME_MBCATEGORY = "com.liferay.portlet.messageboards.model.MBCategory";
	public static final String CLASSNAME_MBMESSAGE = "com.liferay.portlet.messageboards.model.MBMessage";
	public static final String CLASSNAME_WIKIPAGE = "com.liferay.portlet.wiki.model.WikiPage";
	
	public static String[] CUSTOMFIELD_CLASSNAME_TYPES = {CLASSNAME_LAYOUT, CLASSNAME_ORGANIZATION,
		CLASSNAME_USER, CLASSNAME_BLOGSENTRY, CLASSNAME_BOOKMARKSENTRY, CLASSNAME_BOOKMARKSFOLDER,
		CLASSNAME_CALEVENT, CLASSNAME_DLFILEENTRY, CLASSNAME_DLFOLDER, CLASSNAME_IGFOLDER,
		CLASSNAME_IGIMAGE, CLASSNAME_JOURNALARTICLE, CLASSNAME_MBCATEGORY, CLASSNAME_MBMESSAGE,
		CLASSNAME_WIKIPAGE};
	*/	
	
	public static final List<String> UNUPDATE_PUBLICATION_DATE_CLASSNAMES = Arrays.asList(new String[]
			{
				CLASSNAME_UNKNOWN,
				CLASSNAME_EXPANDOTABLE,     
				CLASSNAME_EXPANDOCOLUMN,        
				CLASSNAME_EXPANDOROW,           
				CLASSNAME_EXPANDOVALUE,         
				CLASSNAME_WEBSITE,              
				CLASSNAME_ADDRESS,              
				CLASSNAME_PHONE,                
				CLASSNAME_EMAILADDRESS,         
				CLASSNAME_ENTRY,                
				CLASSNAME_MILENIUMSECTION,      
				CLASSNAME_MILENIUMARTICLE,      
				CLASSNAME_CONTACT,              
				CLASSNAME_COMPANY 			
			});



	
	/*
	 * LIVE CONFIGURATION
	 */
	public static final String LIVE_CONFIG_DESTINATION_TYPE_CHANNEL = "channel";
	public static final String LIVE_CONFIG_DESTINATION_TYPE_EXPLICIT = "explicit";
	public static final String LIVE_CONFIG_OUTPUT_METHOD_FTP = "ftp";
	public static final String LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM = "file-system";
	
	/*
	 * LIVE CONTROL
	 */
	public static final String MASIVE = "masive";
	public static final String PARTIAL = "partial";
	
	/*
	 * VIDEO CODECS
	 */ 
	public static final int BASE_VIDEO_CODIFICATION_AUDIO_MONO = 1;
	public static final int BASE_VIDEO_CODIFICATION_AUDIO_STEREO = 2;
	
	public static final String VCODEC_H264 = "libx264";
	public static final String VCODEC_THEORA = "libtheora";
	
	public static final String[] BASE_VIDEO_CODIFICATION_CODECS_VIDEO = {VCODEC_H264, VCODEC_THEORA};
	
	public static final String ACODEC_AAC = "aac";
	public static final String ACODEC_VORBIS = "vorbis";
	
	public static final String[] BASE_VIDEO_CODIFICATION_CODECS_AUDIO = {ACODEC_AAC, ACODEC_VORBIS};
	
	public static final String BOOL_YES	= "S";
	public static final String BOOL_NO	= "N";
		
	// Formatos de fechas
	public static final String DATEFORMAT_YYYY_MM_DD_HH_MM_00 		= "yyyy-MM-dd HH:mm:00"; //2012-04-12 08:15:00
	public static final String DATEFORMAT_YYYY_MM_DD_HH_MM_ss 		= "yyyy-MM-dd HH:mm:ss";
	public static final String DATEFORMAT_YYYYMMDDHHMMss 			= "yyyyMMddHHmmss";
	
	public static final String DATEFORMAT_YYYY_MM_DD 				= "yyyy-MM-dd";
	public static final String DATEFORMAT_YYYY_MM_DD_T_HH_MM_ss_Z	= "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final String DATEFORMAT_EEE_D_MMM_YYYY_HH_MM_SS_GMT	= "EEE, d-MMM-yyyy HH:MM:ss 'GMT'";
	
	public static final String DATEFORMAT_H_M = "H:m";
	public static final String DATEFORMAT_HH_MM = "HH:mm";
	public static final String DATEFORMAT_h_m = "h:m a";
	public static final String DATEFORMAT_hh_mm = "hh:mm a";
	
	// Calendar
	public static final String CALENDAR_UNIT_DAY	= "day";
	public static final String CALENDAR_UNIT_HOUR	= "hour";
	public static final String CALENDAR_UNIT_MONTH	= "month";
	public static final String CALENDAR_UNIT_YEAR	= "year";
	public static final String CALENDAR_UNIT_WEEK	= "week";
	
	public static final String QUERY_TOKEN_CHK_VALIDITYDATE		= " /*CHK_VALIDITYDATE*/ ";
	public static final String QUERY_INDEX_PAGECONTENT_VIGHASTA	= " USE INDEX (XYZ_ITR_IX_NEWS_PAGECONTENT_VIGHASTA_ZYX) ";
	public static final String QUERY_CHUNK_VIGHASTA				= " AND n.vigenciahasta >= ";

	// Opciones de iter.image.scale-on-the-fly.preferedmode
	public static final String SCALE_ON_THE_FLY_MODE_QSTRFMT	= "qstrfmt";
	public static final String SCALE_ON_THE_FLY_MODE_QSTRWH		= "qstrwh";
	public static final String SCALE_ON_THE_FLY_MODE_URLFMT		= "urlfmt";
	public static final String SCALE_ON_THE_FLY_MODE_URLWH		= "urlwh";
	
	// Constantes de las preferencias
	public static final String	 PREFS_ITERWEBCMS_VERSION			= "iterWebCmsVersion";
	public static final String	 PREFS_PORTLETITEM 					= PortletKeys.PREFS_PORTLETITEM;
	public static final String	 PREFS_LAYOUT_IDS 					= PortletKeys.PREFS_LAYOUT_IDS;
	public static final String 	 PREFS_LAYOUT_PLID 					= PortletKeys.PREFS_LAYOUT_PLID;
	public static final String 	 PREFS_LAYOUT_IDSFILTER 			= PortletKeys.PREFS_LAYOUT_IDSFILTER;
	public static final String 	 PREFS_DEPTH_STARTAT 				= PortletKeys.PREFS_DEPTH_STARTAT;
	public static final List<String> PREFS_LAYOUTS					= PortletKeys.PREFS_LAYOUTS;
	
	public static final String 	 PREFS_CAT_ASSET_CATEGORY_IDS		= PortletKeys.PREFS_CAT_ASSET_CATEGORY_IDS;
	public static final String 	 PREFS_CAT_CONTENT_CATEGORY_IDS		= PortletKeys.PREFS_CAT_CONTENT_CATEGORY_IDS;
	public static final String 	 PREFS_CAT_STICKY_CATEGORY_IDS		= PortletKeys.PREFS_CAT_STICKY_CATEGORY_IDS;
	public static final String 	 PREFS_CAT_EXCLUDE_CATEGORY_IDS		= PortletKeys.PREFS_CAT_EXCLUDE_CATEGORY_IDS;
	public static final String 	 PREFS_CAT_CATEGORY_IDS_FILTER		= PortletKeys.PREFS_CAT_CATEGORY_IDS_FILTER;
	public static final List<String> PREFS_CATS						= PortletKeys.PREFS_CATS;
	
	public static final String 	 PREFS_VOC_CONTENT_VOCABULARY_IDS	= PortletKeys.PREFS_VOC_CONTENT_VOCABULARY_IDS;
	public static final String 	 PREFS_VOC_EXCLUDE_VOCABULARY_IDS	= PortletKeys.PREFS_VOC_EXCLUDE_VOCABULARY_IDS;
	public static final String 	 PREFS_VOC_VOCABULARY_IDS_FILTER	= PortletKeys.PREFS_VOC_VOCABULARY_IDS_FILTER;
	public static final List<String> PREFS_VOCS						= PortletKeys.PREFS_VOCS;

	public static final String 	 PREFS_QUA_QUALIFICATION_ID			= PortletKeys.PREFS_QUA_QUALIFICATION_ID;
	public static final List<String> PREFS_QUAS						= PortletKeys.PREFS_QUAS;
	
	public static final String 	 PREFS_CATALOG_TABS 				= PortletKeys.PREFS_CATALOG_TABS;
	public static final String 	 PREFS_MODELID 						= PortletKeys.PREFS_MODELID;
	
	public static final String 	 PREFS_SUBSCRIPTIONS				= PortletKeys.PREFS_SUBSCRIPTIONS;
	public static final String 	 PREFS_SUBSCRIPTIONS4SHOW			= PortletKeys.PREFS_SUBSCRIPTIONS4SHOW;
	public static final String 	 PREFS_SUBSCRIPTIONS4NOTSHOW		= PortletKeys.PREFS_SUBSCRIPTIONS4NOTSHOW;
	public static final List<String> PREFS_SUBSCRIPTIONS_LIST		= PortletKeys.PREFS_SUBSCRIPTIONS_LIST;
	
	public static final String PREF_EXCLUDE_VOC_IDS 				= PortletKeys.PREF_EXCLUDE_VOC_IDS;
	public static final String PREF_EXCLUDE_CAT_IDS 				= PortletKeys.PREF_EXCLUDE_CAT_IDS;
	
	public static final String PREF_JOURNALTEMPLATES_TEMPLATEID			= PortletKeys.PREF_JOURNALTEMPLATES_TEMPLATEID; 		// Alert Portlet
	public static final String PREF_JOURNALTEMPLATES_TEMPLATEIDARTICLE	= PortletKeys.PREF_JOURNALTEMPLATES_TEMPLATEIDARTICLE; 	// Teasers, Rankings y Related
	public static final String PREF_JOURNALTEMPLATES_TEMPLATE_ARTICLE	= PortletKeys.PREF_JOURNALTEMPLATES_TEMPLATE_ARTICLE; 	// Content Viewer y Search Result
	public static final String PREF_JOURNALTEMPLATES_ARTICLERESTRICTED	= PortletKeys.PREF_JOURNALTEMPLATES_ARTICLERESTRICTED;
	public static final String PREF_JOURNALTEMPLATES_TEMPLATEIDPOLL		= PortletKeys.PREF_JOURNALTEMPLATES_TEMPLATEIDPOLL;
	public static final String PREF_JOURNALTEMPLATES_POLLRESTRICTED		= PortletKeys.PREF_JOURNALTEMPLATES_POLLRESTRICTED;
	public static final List<String> PREF_JOURNALTEMPLATES				= PortletKeys.PREF_JOURNALTEMPLATES;
	
			

	/*
	 * App Keys
	 */
	public static final int APPKEY_TYPE_GENERAL = -1;
	public static final int APPKEY_TYPE_GOOGLE_MAPS = 0;
	
	public static final Map<Integer, String> APPKEY_TYPES;
    static {
        Map<Integer, String> aMap = new HashMap<Integer, String>();
        aMap.put(APPKEY_TYPE_GENERAL, "base-appkey-type-general");
        aMap.put(APPKEY_TYPE_GOOGLE_MAPS, "base-appkey-type-googlemaps");
        APPKEY_TYPES = Collections.unmodifiableMap(aMap);
    }
    
    /*
     * Acceso general a valores de mapas de definicion tipo one-to-one
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    
   /*
    * ViewMode Velocity Injections
    */       
    public static final int SECTION_TO_SHOW_NO = PortletKeys.SECTION_TO_SHOW_NO;  
    public static final int SECTION_TO_SHOW_SOURCE = PortletKeys.SECTION_TO_SHOW_SOURCE;
    public static final int SECTION_TO_SHOW_DEFAULT = PortletKeys.SECTION_TO_SHOW_DEFAULT;  
    public static final int SECTION_TO_SHOW_CURRENT = PortletKeys.SECTION_TO_SHOW_CURRENT;
    
    public static final String VM_ROOT = "articlectx";
    public static final String VM_SECTION_NAME = "section-name";
	public static final String VM_URL = "url";
	public static final String VM_SECTION = "section";
	public static final String VM_REF_PAGE_URL = "ref-page-url";
	public static final String VM_REF_PAGE_NAME = "ref-page-name";
	public static final String VM_METADATA = "metadata";
	public static final String VM_KEY = "key";
	
	// Codifications
	public static final String UTF8 = "UTF-8"; 
	
	// Codificaciones HTMLs sensibles a codificación para poder ser cargadas en un XML
	// http://www.w3.org/TR/html4/sgml/entities.html#h-24.2.1
	public static final Map<String,String> HTML_XML_CODECS;
	static
	{
		 Map<String, String> map = new HashMap<String, String>();
		 map.put("&nbsp;",			"&#160;"); 	// no-break space = non-breaking space,U+00A0 ISOnum -->
		 map.put("&iexcl;",			"&#161;"); 	// inverted exclamation mark, U+00A1 ISOnum -->
		 map.put("&cent;",			"&#162;"); 	// cent sign, U+00A2 ISOnum -->
		 map.put("&pound;",			"&#163;"); 	// pound sign, U+00A3 ISOnum -->
		 map.put("&curren;",		"&#164;"); 	// currency sign, U+00A4 ISOnum -->
		 map.put("&yen;",			"&#165;"); 	// yen sign = yuan sign, U+00A5 ISOnum -->
		 map.put("&brvbar;",		"&#166;"); 	// broken bar = broken vertical bar,U+00A6 ISOnum -->
		 map.put("&sect;",			"&#167;"); 	// section sign, U+00A7 ISOnum -->
		 map.put("&uml;",			"&#168;"); 	// diaeresis = spacing diaeresis,U+00A8 ISOdia -->
		 map.put("&copy;",			"&#169;"); 	// copyright sign, U+00A9 ISOnum -->
		 map.put("&ordf;",			"&#170;"); 	// feminine ordinal indicator, U+00AA ISOnum -->
		 map.put("&laquo;",			"&#171;"); 	// left-pointing double angle quotation mark= left pointing guillemet, U+00AB ISOnum -->
		 map.put("&not;",			"&#172;"); 	// not sign, U+00AC ISOnum -->
		 map.put("&shy;",			"&#173;"); 	// soft hyphen = discretionary hyphen,U+00AD ISOnum -->
		 map.put("&reg;",			"&#174;"); 	// registered sign = registered trade mark sign,U+00AE ISOnum -->
		 map.put("&macr;",			"&#175;"); 	// macron = spacing macron = overline= APL overbar, U+00AF ISOdia -->
		 map.put("&deg;",			"&#176;"); 	// degree sign, U+00B0 ISOnum -->
		 map.put("&plusmn;",		"&#177;"); 	// plus-minus sign = plus-or-minus sign,U+00B1 ISOnum -->
		 map.put("&sup2;",			"&#178;"); 	// superscript two = superscript digit two= squared, U+00B2 ISOnum -->
		 map.put("&sup3;",			"&#179;"); 	// superscript three = superscript digit three= cubed, U+00B3 ISOnum -->
		 map.put("&acute;",			"&#180;"); 	// acute accent = spacing acute,U+00B4 ISOdia -->
		 map.put("&micro;",			"&#181;"); 	// micro sign, U+00B5 ISOnum -->
		 map.put("&para;",			"&#182;"); 	// pilcrow sign = paragraph sign,U+00B6 ISOnum -->
		 map.put("&middot;",		"&#183;"); 	// middle dot = Georgian comma= Greek middle dot, U+00B7 ISOnum -->
		 map.put("&cedil;",			"&#184;"); 	// cedilla = spacing cedilla, U+00B8 ISOdia -->
		 map.put("&sup1;",			"&#185;"); 	// superscript one = superscript digit one,U+00B9 ISOnum -->
		 map.put("&ordm;",			"&#186;"); 	// masculine ordinal indicator,U+00BA ISOnum -->
		 map.put("&raquo;",			"&#187;"); 	// right-pointing double angle quotation mark= right pointing guillemet, U+00BB ISOnum -->
		 map.put("&frac14;",		"&#188;"); 	// vulgar fraction one quarter= fraction one quarter, U+00BC ISOnum -->
		 map.put("&frac12;",		"&#189;"); 	// vulgar fraction one half= fraction one half, U+00BD ISOnum -->
		 map.put("&frac34;",		"&#190;"); 	// vulgar fraction three quarters= fraction three quarters, U+00BE ISOnum -->
		 map.put("&iquest;",		"&#191;"); 	// inverted question mark= turned question mark, U+00BF ISOnum -->
		 map.put("&Agrave;",		"&#192;"); 	// latin capital letter A with grave= latin capital letter A grave,U+00C0 ISOlat1 -->
		 map.put("&Aacute;",		"&#193;"); 	// latin capital letter A with acute,U+00C1 ISOlat1 -->
		 map.put("&Acirc;",			"&#194;"); 	// latin capital letter A with circumflex,U+00C2 ISOlat1 -->
		 map.put("&Atilde;",		"&#195;"); 	// latin capital letter A with tilde,U+00C3 ISOlat1 -->
		 map.put("&Auml;",			"&#196;"); 	// latin capital letter A with diaeresis,U+00C4 ISOlat1 -->
		 map.put("&Aring;",			"&#197;"); 	// latin capital letter A with ring above= latin capital letter A ring,U+00C5 ISOlat1 -->
		 map.put("&AElig;",			"&#198;"); 	// latin capital letter AE= latin capital ligature AE,U+00C6 ISOlat1 -->
		 map.put("&Ccedil;",		"&#199;"); 	// latin capital letter C with cedilla,U+00C7 ISOlat1 -->
		 map.put("&Egrave;",		"&#200;"); 	// latin capital letter E with grave,U+00C8 ISOlat1 -->
		 map.put("&Eacute;",		"&#201;"); 	// latin capital letter E with acute,U+00C9 ISOlat1 -->
		 map.put("&Ecirc;",			"&#202;"); 	// latin capital letter E with circumflex,U+00CA ISOlat1 -->
		 map.put("&Euml;",			"&#203;"); 	// latin capital letter E with diaeresis,U+00CB ISOlat1 -->
		 map.put("&Igrave;",		"&#204;"); 	// latin capital letter I with grave,U+00CC ISOlat1 -->
		 map.put("&Iacute;",		"&#205;"); 	// latin capital letter I with acute,U+00CD ISOlat1 -->
		 map.put("&Icirc;",			"&#206;"); 	// latin capital letter I with circumflex,U+00CE ISOlat1 -->
		 map.put("&Iuml;",			"&#207;"); 	// latin capital letter I with diaeresis,U+00CF ISOlat1 -->
		 map.put("&ETH;",			"&#208;"); 	// latin capital letter ETH, U+00D0 ISOlat1 -->
		 map.put("&Ntilde;",		"&#209;"); 	// latin capital letter N with tilde,U+00D1 ISOlat1 -->
		 map.put("&Ograve;",		"&#210;"); 	// latin capital letter O with grave,U+00D2 ISOlat1 -->
		 map.put("&Oacute;",		"&#211;"); 	// latin capital letter O with acute,U+00D3 ISOlat1 -->
		 map.put("&Ocirc;",			"&#212;"); 	// latin capital letter O with circumflex,U+00D4 ISOlat1 -->
		 map.put("&Otilde;",		"&#213;"); 	// latin capital letter O with tilde,U+00D5 ISOlat1 -->
		 map.put("&Ouml;",			"&#214;"); 	// latin capital letter O with diaeresis,U+00D6 ISOlat1 -->
		 map.put("&times;",			"&#215;"); 	// multiplication sign, U+00D7 ISOnum -->
		 map.put("&Oslash;",		"&#216;"); 	// latin capital letter O with stroke= latin capital letter O slash,U+00D8 ISOlat1 -->
		 map.put("&Ugrave;",		"&#217;"); 	// latin capital letter U with grave,U+00D9 ISOlat1 -->
		 map.put("&Uacute;",		"&#218;"); 	// latin capital letter U with acute,U+00DA ISOlat1 -->
		 map.put("&Ucirc;",			"&#219;"); 	// latin capital letter U with circumflex,U+00DB ISOlat1 -->
		 map.put("&Uuml;",			"&#220;"); 	// latin capital letter U with diaeresis,U+00DC ISOlat1 -->
		 map.put("&Yacute;",		"&#221;"); 	// latin capital letter Y with acute,U+00DD ISOlat1 -->
		 map.put("&THORN;",			"&#222;"); 	// latin capital letter THORN,U+00DE ISOlat1 -->
		 map.put("&szlig;",			"&#223;"); 	// latin small letter sharp s = ess-zed,U+00DF ISOlat1 -->
		 map.put("&agrave;",		"&#224;"); 	// latin small letter a with grave= latin small letter a grave,U+00E0 ISOlat1 -->
		 map.put("&aacute;",		"&#225;"); 	// latin small letter a with acute,U+00E1 ISOlat1 -->
		 map.put("&acirc;",			"&#226;"); 	// latin small letter a with circumflex,U+00E2 ISOlat1 -->
		 map.put("&atilde;",		"&#227;"); 	// latin small letter a with tilde,U+00E3 ISOlat1 -->
		 map.put("&auml;",			"&#228;"); 	// latin small letter a with diaeresis,U+00E4 ISOlat1 -->
		 map.put("&aring;",			"&#229;"); 	// latin small letter a with ring above= latin small letter a ring,U+00E5 ISOlat1 -->
		 map.put("&aelig;",			"&#230;"); 	// latin small letter ae= latin small ligature ae, U+00E6 ISOlat1 -->
		 map.put("&ccedil;",		"&#231;"); 	// latin small letter c with cedilla,U+00E7 ISOlat1 -->
		 map.put("&egrave;",		"&#232;"); 	// latin small letter e with grave,U+00E8 ISOlat1 -->
		 map.put("&eacute;",		"&#233;"); 	// latin small letter e with acute,U+00E9 ISOlat1 -->
		 map.put("&ecirc;",			"&#234;"); 	// latin small letter e with circumflex,U+00EA ISOlat1 -->
		 map.put("&euml;",			"&#235;"); 	// latin small letter e with diaeresis,U+00EB ISOlat1 -->
		 map.put("&igrave;",		"&#236;"); 	// latin small letter i with grave,U+00EC ISOlat1 -->
		 map.put("&iacute;",		"&#237;"); 	// latin small letter i with acute,U+00ED ISOlat1 -->
		 map.put("&icirc;",			"&#238;"); 	// latin small letter i with circumflex,U+00EE ISOlat1 -->
		 map.put("&iuml;",			"&#239;"); 	// latin small letter i with diaeresis,U+00EF ISOlat1 -->
		 map.put("&eth;",			"&#240;"); 	// latin small letter eth, U+00F0 ISOlat1 -->
		 map.put("&ntilde;",		"&#241;"); 	// latin small letter n with tilde,U+00F1 ISOlat1 -->
		 map.put("&ograve;",		"&#242;"); 	// latin small letter o with grave,U+00F2 ISOlat1 -->
		 map.put("&oacute;",		"&#243;"); 	// latin small letter o with acute,U+00F3 ISOlat1 -->
		 map.put("&ocirc;",			"&#244;"); 	// latin small letter o with circumflex,U+00F4 ISOlat1 -->
		 map.put("&otilde;",		"&#245;"); 	// latin small letter o with tilde,U+00F5 ISOlat1 -->
		 map.put("&ouml;",			"&#246;"); 	// latin small letter o with diaeresis,U+00F6 ISOlat1 -->
		 map.put("&divide;",		"&#247;"); 	// division sign, U+00F7 ISOnum -->
		 map.put("&oslash;",		"&#248;"); 	// latin small letter o with stroke,= latin small letter o slash,U+00F8 ISOlat1 -->
		 map.put("&ugrave;",		"&#249;"); 	// latin small letter u with grave,U+00F9 ISOlat1 -->
		 map.put("&uacute;",		"&#250;"); 	// latin small letter u with acute,U+00FA ISOlat1 -->
		 map.put("&ucirc;",			"&#251;"); 	// latin small letter u with circumflex,U+00FB ISOlat1 -->
		 map.put("&uuml;",			"&#252;"); 	// latin small letter u with diaeresis,U+00FC ISOlat1 -->
		 map.put("&yacute;",		"&#253;"); 	// latin small letter y with acute,U+00FD ISOlat1 -->
		 map.put("&thorn;",			"&#254;"); 	// latin small letter thorn,U+00FE ISOlat1 -->
		 map.put("&yuml;",			"&#255;"); 	// latin small letter y with diaeresis,U+00FF ISOlat1 -->
		 map.put("&fnof;",			"&#402;"); 	// latin small f with hook = function  = florin, U+0192 ISOtech -->

		 // Greek -->
		 map.put("&Alpha;",			"&#913;"); 	// greek capital letter alpha, U+0391 -->
		 map.put("&Beta;",			"&#914;"); 	// greek capital letter beta, U+0392 -->
		 map.put("&Gamma;",			"&#915;"); 	// greek capital letter gamma,  U+0393 ISOgrk3 -->
		 map.put("&Delta;",			"&#916;"); 	// greek capital letter delta,  U+0394 ISOgrk3 -->
		 map.put("&Epsilon;",		"&#917;"); 	// greek capital letter epsilon, U+0395 -->
		 map.put("&Zeta;",			"&#918;"); 	// greek capital letter zeta, U+0396 -->
		 map.put("&Eta;",			"&#919;"); 	// greek capital letter eta, U+0397 -->
		 map.put("&Theta;",			"&#920;"); 	// greek capital letter theta,  U+0398 ISOgrk3 -->
		 map.put("&Iota;",			"&#921;"); 	// greek capital letter iota, U+0399 -->
		 map.put("&Kappa;",			"&#922;"); 	// greek capital letter kappa, U+039A -->
		 map.put("&Lambda;",		"&#923;"); 	// greek capital letter lambda,  U+039B ISOgrk3 -->
		 map.put("&Mu;",			"&#924;"); 	// greek capital letter mu, U+039C -->
		 map.put("&Nu;",			"&#925;"); 	// greek capital letter nu, U+039D -->
		 map.put("&Xi;",			"&#926;"); 	// greek capital letter xi, U+039E ISOgrk3 -->
		 map.put("&Omicron;",		"&#927;"); 	// greek capital letter omicron, U+039F -->
		 map.put("&Pi;",			"&#928;"); 	// greek capital letter pi, U+03A0 ISOgrk3 -->
		 map.put("&Rho;",			"&#929;"); 	// greek capital letter rho, U+03A1 -->
		 // there is no Sigmaf, and no U+03A2 character either -->
		 map.put("&Sigma;",			"&#931;"); 	// greek capital letter sigma,  U+03A3 ISOgrk3 -->
		 map.put("&Tau;",			"&#932;"); 	// greek capital letter tau, U+03A4 -->
		 map.put("&Upsilon;",		"&#933;"); 	// greek capital letter upsilon,  U+03A5 ISOgrk3 -->
		 map.put("&Phi;",			"&#934;"); 	// greek capital letter phi,  U+03A6 ISOgrk3 -->
		 map.put("&Chi;",			"&#935;"); 	// greek capital letter chi, U+03A7 -->
		 map.put("&Psi;",			"&#936;"); 	// greek capital letter psi,  U+03A8 ISOgrk3 -->
		 map.put("&Omega;",			"&#937;"); 	// greek capital letter omega,  U+03A9 ISOgrk3 -->

		 map.put("&alpha;",			"&#945;"); 	// greek small letter alpha,  U+03B1 ISOgrk3 -->
		 map.put("&beta;",			"&#946;"); 	// greek small letter beta, U+03B2 ISOgrk3 -->
		 map.put("&gamma;",			"&#947;"); 	// greek small letter gamma,  U+03B3 ISOgrk3 -->
		 map.put("&delta;",			"&#948;"); 	// greek small letter delta,  U+03B4 ISOgrk3 -->
		 map.put("&epsilon;",		"&#949;"); 	// greek small letter epsilon,  U+03B5 ISOgrk3 -->
		 map.put("&zeta;",			"&#950;"); 	// greek small letter zeta, U+03B6 ISOgrk3 -->
		 map.put("&eta;",			"&#951;"); 	// greek small letter eta, U+03B7 ISOgrk3 -->
		 map.put("&theta;",			"&#952;"); 	// greek small letter theta,  U+03B8 ISOgrk3 -->
		 map.put("&iota;",			"&#953;"); 	// greek small letter iota, U+03B9 ISOgrk3 -->
		 map.put("&kappa;",			"&#954;"); 	// greek small letter kappa,  U+03BA ISOgrk3 -->
		 map.put("&lambda;",		"&#955;"); 	// greek small letter lambda,  U+03BB ISOgrk3 -->
		 map.put("&mu;",			"&#956;"); 	// greek small letter mu, U+03BC ISOgrk3 -->
		 map.put("&nu;",			"&#957;"); 	// greek small letter nu, U+03BD ISOgrk3 -->
		 map.put("&xi;",			"&#958;"); 	// greek small letter xi, U+03BE ISOgrk3 -->
		 map.put("&omicron;",		"&#959;"); 	// greek small letter omicron, U+03BF NEW -->
		 map.put("&pi;",			"&#960;"); 	// greek small letter pi, U+03C0 ISOgrk3 -->
		 map.put("&rho;",			"&#961;"); 	// greek small letter rho, U+03C1 ISOgrk3 -->
		 map.put("&sigmaf;",		"&#962;"); 	// greek small letter final sigma,  U+03C2 ISOgrk3 -->
		 map.put("&sigma;",			"&#963;"); 	// greek small letter sigma,  U+03C3 ISOgrk3 -->
		 map.put("&tau;",			"&#964;"); 	// greek small letter tau, U+03C4 ISOgrk3 -->
		 map.put("&upsilon;",		"&#965;"); 	// greek small letter upsilon,  U+03C5 ISOgrk3 -->
		 map.put("&phi;",			"&#966;"); 	// greek small letter phi, U+03C6 ISOgrk3 -->
		 map.put("&chi;",			"&#967;"); 	// greek small letter chi, U+03C7 ISOgrk3 -->
		 map.put("&psi;",			"&#968;"); 	// greek small letter psi, U+03C8 ISOgrk3 -->
		 map.put("&omega;",			"&#969;"); 	// greek small letter omega,  U+03C9 ISOgrk3 -->
		 map.put("&thetasym;",		"&#977;"); 	// greek small letter theta symbol,  U+03D1 NEW -->
		 map.put("&upsih;",			"&#978;"); 	// greek upsilon with hook symbol,  U+03D2 NEW -->
		 map.put("&piv;",			"&#982;"); 	// greek pi symbol, U+03D6 ISOgrk3 -->

		 // General Punctuation -->
		 map.put("&bull;",			"&#8226;"); 	// bullet = black small circle,   U+2022 ISOpub  -->
		 // bullet is NOT the same as bullet operator, U+2219 -->
		 map.put("&hellip;",		"&#8230;"); 	// horizontal ellipsis = three dot leader,   U+2026 ISOpub  -->
		 map.put("&prime;",			"&#8242;"); 	// prime = minutes = feet, U+2032 ISOtech -->
		 map.put("&Prime;",			"&#8243;"); 	// double prime = seconds = inches,   U+2033 ISOtech -->
		 map.put("&oline;",			"&#8254;"); 	// overline = spacing overscore,   U+203E NEW -->
		 map.put("&frasl;",			"&#8260;"); 	// fraction slash, U+2044 NEW -->

		 // Letterlike Symbols -->
		 map.put("&weierp;",		"&#8472;"); 	// script capital P = power set   = Weierstrass p, U+2118 ISOamso -->
		 map.put("&image;",			"&#8465;"); 	// blackletter capital I = imaginary part,   U+2111 ISOamso -->
		 map.put("&real;",			"&#8476;"); 	// blackletter capital R = real part symbol,   U+211C ISOamso -->
		 map.put("&trade;",			"&#8482;"); 	// trade mark sign, U+2122 ISOnum -->
		 map.put("&alefsym;",		"&#8501;"); 	// alef symbol = first transfinite cardinal,   U+2135 NEW -->
		 // alef symbol is NOT the same as hebrew letter alef,  U+05D0 although the same glyph could be used to depict both characters -->

		 // Arrows -->
		 map.put("&larr;",			"&#8592;"); 	// leftwards arrow, U+2190 ISOnum -->
		 map.put("&uarr;",			"&#8593;"); 	// upwards arrow, U+2191 ISOnum-->
		 map.put("&rarr;",			"&#8594;"); 	// rightwards arrow, U+2192 ISOnum -->
		 map.put("&darr;",			"&#8595;"); 	// downwards arrow, U+2193 ISOnum -->
		 map.put("&harr;",			"&#8596;"); 	// left right arrow, U+2194 ISOamsa -->
		 map.put("&crarr;",			"&#8629;"); 	// downwards arrow with corner leftwards   = carriage return, U+21B5 NEW -->
		 map.put("&lArr;",			"&#8656;"); 	// leftwards double arrow, U+21D0 ISOtech -->
		 // ISO 10646 does not say that lArr is the same as the 'is implied by' arrow  but also does not have any other character for that function. So ? lArr can  be used for 'is implied by' as ISOtech suggests -->
		 map.put("&uArr;",			"&#8657;"); 	// upwards double arrow, U+21D1 ISOamsa -->
		 map.put("&rArr;",			"&#8658;"); 	// rightwards double arrow,   U+21D2 ISOtech -->
		 // ISO 10646 does not say this is the 'implies' character but does not have  another character with this function so ? rArr can be used for 'implies' as ISOtech suggests -->
		 map.put("&dArr;",			"&#8659;"); 	// downwards double arrow, U+21D3 ISOamsa -->
		 map.put("&hArr;",			"&#8660;"); 	// left right double arrow,   U+21D4 ISOamsa -->

		 // Mathematical Operators -->
		 map.put("&forall;",		"&#8704;"); 	// for all, U+2200 ISOtech -->
		 map.put("&part;",			"&#8706;"); 	// partial differential, U+2202 ISOtech  -->
		 map.put("&exist;",			"&#8707;"); 	// there exists, U+2203 ISOtech -->
		 map.put("&empty;",			"&#8709;"); 	// empty set = null set = diameter,   U+2205 ISOamso -->
		 map.put("&nabla;",			"&#8711;"); 	// nabla = backward difference,   U+2207 ISOtech -->
		 map.put("&isin;",			"&#8712;"); 	// element of, U+2208 ISOtech -->
		 map.put("&notin;",			"&#8713;"); 	// not an element of, U+2209 ISOtech -->
		 map.put("&ni;",			"&#8715;"); 	// contains as member, U+220B ISOtech -->
		 // should there be a more memorable name than 'ni'? -->
		 map.put("&prod;",			"&#8719;"); 	// n-ary product = product sign,   U+220F ISOamsb -->
		 // prod is NOT the same character as U+03A0 'greek capital letter pi' though the same glyph might be used for both -->
		 map.put("&sum;",			"&#8721;"); 	// n-ary sumation, U+2211 ISOamsb -->
		 // sum is NOT the same character as U+03A3 'greek capital letter sigma' though the same glyph might be used for both -->
		 map.put("&minus;",			"&#8722;"); 	// minus sign, U+2212 ISOtech -->
		 map.put("&lowast;",		"&#8727;"); 	// asterisk operator, U+2217 ISOtech -->
		 map.put("&radic;",			"&#8730;"); 	// square root = radical sign,   U+221A ISOtech -->
		 map.put("&prop;",			"&#8733;"); 	// proportional to, U+221D ISOtech -->
		 map.put("&infin;",			"&#8734;"); 	// infinity, U+221E ISOtech -->
		 map.put("&ang;",			"&#8736;"); 	// angle, U+2220 ISOamso -->
		 map.put("&and;",			"&#8743;"); 	// logical and = wedge, U+2227 ISOtech -->
		 map.put("&or;",			"&#8744;"); 	// logical or = vee, U+2228 ISOtech -->
		 map.put("&cap;",			"&#8745;"); 	// intersection = cap, U+2229 ISOtech -->
		 map.put("&cup;",			"&#8746;"); 	// union = cup, U+222A ISOtech -->
		 map.put("&int;",			"&#8747;"); 	// integral, U+222B ISOtech -->
		 map.put("&there4;",		"&#8756;"); 	// therefore, U+2234 ISOtech -->
		 map.put("&sim;",			"&#8764;"); 	// tilde operator = varies with = similar to,   U+223C ISOtech -->

		 // tilde operator is NOT the same character as the tilde, U+007E, although the same glyph might be used to represent both  -->
		 map.put("&cong;",			"&#8773;"); 	// approximately equal to, U+2245 ISOtech -->
		 map.put("&asymp;",			"&#8776;"); 	// almost equal to = asymptotic to,   U+2248 ISOamsr -->
		 map.put("&ne;",			"&#8800;"); 	// not equal to, U+2260 ISOtech -->
		 map.put("&equiv;",			"&#8801;"); 	// identical to, U+2261 ISOtech -->
		 map.put("&le;",			"&#8804;"); 	// less-than or equal to, U+2264 ISOtech -->
		 map.put("&ge;",			"&#8805;"); 	// greater-than or equal to,   U+2265 ISOtech -->
		 map.put("&sub;",			"&#8834;"); 	// subset of, U+2282 ISOtech -->
		 map.put("&sup;",			"&#8835;"); 	// superset of, U+2283 ISOtech -->

		 // note that nsup, 'not a superset of, U+2283' is not covered by the Symbol font encoding and is not included. Should it be, for symmetry?  It is in ISOamsn  --> 
		 map.put("&nsub;",			"&#8836;"); 	// not a subset of, U+2284 ISOamsn -->
		 map.put("&sube;",			"&#8838;"); 	// subset of or equal to, U+2286 ISOtech -->
		 map.put("&supe;",			"&#8839;"); 	// superset of or equal to,   U+2287 ISOtech -->
		 map.put("&oplus;",			"&#8853;"); 	// circled plus = direct sum,   U+2295 ISOamsb -->
		 map.put("&otimes;",		"&#8855;"); 	// circled times = vector product,   U+2297 ISOamsb -->
		 map.put("&perp;",			"&#8869;"); 	// up tack = orthogonal to = perpendicular,   U+22A5 ISOtech -->
		 map.put("&sdot;",			"&#8901;"); 	// dot operator, U+22C5 ISOamsb -->
		 // dot operator is NOT the same character as U+00B7 middle dot -->

		 // Miscellaneous Technical -->
		 map.put("&lceil;",			"&#8968;"); 	// left ceiling = apl upstile,   U+2308 ISOamsc  -->
		 map.put("&rceil;",			"&#8969;"); 	// right ceiling, U+2309 ISOamsc  -->
		 map.put("&lfloor;",		"&#8970;"); 	// left floor = apl downstile,   U+230A ISOamsc  -->
		 map.put("&rfloor;",		"&#8971;"); 	// right floor, U+230B ISOamsc  -->
		 map.put("&lang;",			"&#9001;"); 	// left-pointing angle bracket = bra,   U+2329 ISOtech -->
		 // lang is NOT the same character as U+003C 'less than'  or U+2039 'single left-pointing angle quotation mark' -->
		 map.put("&rang;",			"&#9002;"); 	// right-pointing angle bracket = ket,   U+232A ISOtech -->
		 // rang is NOT the same character as U+003E 'greater than'  or U+203A 'single right-pointing angle quotation mark' -->

		 // Geometric Shapes -->
		 map.put("&loz;",			"&#9674;"); 	// lozenge, U+25CA ISOpub -->

		 // Miscellaneous Symbols -->
		 map.put("&spades;",		"&#9824;"); 	// black spade suit, U+2660 ISOpub -->
		 // black here seems to mean filled as opposed to hollow -->
		 map.put("&clubs;",			"&#9827;"); 	// black club suit = shamrock,   U+2663 ISOpub -->
		 map.put("&hearts;",		"&#9829;"); 	// black heart suit = valentine,   U+2665 ISOpub -->
		 map.put("&diams;",			"&#9830;"); 	// black diamond suit, U+2666 ISOpub -->

		 // C0 Controls and Basic Latin -->
		 map.put("&quot;",			"&#34;"); 	// quotation mark = APL quote,  U+0022 ISOnum -->
		 map.put("&amp;",			"&#38;"); 	// ampersand, U+0026 ISOnum -->
		 map.put("&lt;",			"&#60;"); 	// less-than sign, U+003C ISOnum -->
		 map.put("&gt;",			"&#62;"); 	// greater-than sign, U+003E ISOnum -->

		 // Latin Extended-A -->
		 map.put("&OElig;",			"&#338;"); 	// latin capital ligature OE,  U+0152 ISOlat2 -->
		 map.put("&oelig;",			"&#339;"); 	// latin small ligature oe, U+0153 ISOlat2 -->
		 // ligature is a misnomer, this is a separate character in some languages -->
		 map.put("&Scaron;",		"&#352;"); 	// latin capital letter S with caron,  U+0160 ISOlat2 -->
		 map.put("&scaron;",		"&#353;"); 	// latin small letter s with caron,  U+0161 ISOlat2 -->
		 map.put("&Yuml;",			"&#376;"); 	// latin capital letter Y with diaeresis,  U+0178 ISOlat2 -->

		 // Spacing Modifier Letters -->
		 map.put("&circ;",			"&#710;"); 	// modifier letter circumflex accent,  U+02C6 ISOpub -->
		 map.put("&tilde;",			"&#732;"); 	// small tilde, U+02DC ISOdia -->

		 // General Punctuation -->
		 map.put("&ensp;",			"&#8194;"); 	// en space, U+2002 ISOpub -->
		 map.put("&emsp;",			"&#8195;"); 	// em space, U+2003 ISOpub -->
		 map.put("&thinsp;",		"&#8201;"); 	// thin space, U+2009 ISOpub -->
		 map.put("&zwnj;",			"&#8204;"); 	// zero width non-joiner,  U+200C NEW RFC 2070 -->
		 map.put("&zwj;",			"&#8205;"); 	// zero width joiner, U+200D NEW RFC 2070 -->
		 map.put("&lrm;",			"&#8206;"); 	// left-to-right mark, U+200E NEW RFC 2070 -->
		 map.put("&rlm;",			"&#8207;"); 	// right-to-left mark, U+200F NEW RFC 2070 -->
		 map.put("&ndash;",			"&#8211;"); 	// en dash, U+2013 ISOpub -->
		 map.put("&mdash;",			"&#8212;"); 	// em dash, U+2014 ISOpub -->
		 map.put("&lsquo;",			"&#8216;"); 	// left single quotation mark,  U+2018 ISOnum -->
		 map.put("&rsquo;",			"&#8217;"); 	// right single quotation mark,  U+2019 ISOnum -->
		 map.put("&sbquo;",			"&#8218;"); 	// single low-9 quotation mark, U+201A NEW -->
		 map.put("&ldquo;",			"&#8220;"); 	// left double quotation mark,  U+201C ISOnum -->
		 map.put("&rdquo;",			"&#8221;"); 	// right double quotation mark,  U+201D ISOnum -->
		 map.put("&bdquo;",			"&#8222;"); 	// double low-9 quotation mark, U+201E NEW -->
		 map.put("&dagger;",		"&#8224;"); 	// dagger, U+2020 ISOpub -->
		 map.put("&Dagger;",		"&#8225;"); 	// double dagger, U+2021 ISOpub -->
		 map.put("&permil;",		"&#8240;"); 	// per mille sign, U+2030 ISOtech -->
		 map.put("&lsaquo;",		"&#8249;"); 	// single left-pointing angle quotation mark,  U+2039 ISO proposed -->
		 // lsaquo is proposed but not yet ISO standardized -->
		 map.put("&rsaquo;",		"&#8250;"); 	// single right-pointing angle quotation mark,  U+203A ISO proposed -->
		 // rsaquo is proposed but not yet ISO standardized -->
		 map.put("&euro;",			"&#8364;"); 	// euro sign, U+20AC NEW -->     
		 HTML_XML_CODECS = Collections.unmodifiableMap(map);
	}
	
	// Recaptcha
	public static final String RECAPTCHA_CHALLENGE_FIELD_NAME = "recaptcha_challenge_field";	// Reto
	public static final String RECAPTCHA_RESPONSE_FIELD_NAME = "recaptcha_response_field";		// Respuesta del usuario
    
	
	public static final String 	PARAM_SEPARATOR 		= "|";
	public static final String 	PRODUCT_SEPARATOR 		= ";";
	public static final String 	PRODUCT_DATE_SEPARATOR 	= ",";
	public static final int 	PRODUCTS_POS 			= 4;
	
	public static final String FIELD_TYPE_STRING 		= "string";
	public static final String FIELD_TYPE_BOOLEAN 		= "boolean";
	public static final String FIELD_TYPE_INT 			= "int";
	public static final String FIELD_TYPE_NUMBER 		= "number";
	public static final String FIELD_TYPE_DATE 			= "date";
	public static final String FIELD_TYPE_BINARY 		= "binary";
	public static final String FIELD_TYPE_ARRAY 		= "array";
	
	public static final String FIELD_CLASS_SYSTEM 		= "system";
	public static final String FIELD_CLASS_USER 		= "user";
		
	public static final String FORMS_ATTACHMENTS_FOLDER 	= "form_attachments";
	
	// Servlets de formularios
	public static final String KO 	= "KO";
	public static final String OK	= "OK";
	public static final String ACTION_NONE			= "none";
	public static final String ACTION_EXTERNAL_PAGE	= "externalpage";
	public static final String ACTION_INTERNAL_PAGE	= "internalpage";
	public static final String ACTION_REFERER		= "referer";
	public static final String ACTION_REDIRECT		= "redirect";
	public static final String ACTION_BACK2REFERER	= "backtoreferer";
	
	public static final String XTRADATA_EDIT_MODE				= IterKeys.COOKIE_NAME_INTENT;
	public static final String XTRADATA_USERID					= IterKeys.COOKIE_NAME_USER_ID;
	public static final String XTRADATA_CHANGED_EMAIL			= "changedEmail";
	public static final String XTRADATA_CHANGED_COOKIE_FIELDS 	= "changedCookieFields";
	public static final String XTRADATA_EMAIL					= "email";
	public static final String XTRADATA_MSG						= "msg";
	public static final String XTRADATA_REFERER					= "referer";
	
	public static final String USE_CAPTCHA	= "usecaptcha";
	public static final String REQ_FIELDS	= "reqFields";
	public static final String ALL_FIELDS	= "allFields";
	
	public static final String SUBSCRIPTION_SYSTEM_CONTEXT_VARS = "subscsysctxvars";
	
	//Custom Page Templates
	public static String PARENT_LAYOUT_NAME 			= "Custom Pages Templates";
	public static String PARENT_LAYOUT_URL 				= "/custompagestemplates";
	public static String PARENT_LAYOUT_NAME_NEWSLETTER  = "Newsletter";
	public static String PARENT_LAYOUT_URL_NEWSLETTER 	= "/newsletter";

	public static String ITER		= "ITER";
	public static String MILENIUM 	= "MILENIUM";

	public static final String FILTER_TYPE = "filterType";
	public static final String WITHOUT_FILTER = "withoutFilter";
	public static final String SECTIONS = "sections";
	public static final String CATEGORIES = "categories";
	public static final String DATE = "date";
	public static final String BEFORE = "before";
	public static final String BEFORE_AND_AFTER = "beforeAndAfter";
	public static final String AFTER = "after";
	public static final String FILTER_DISPLAY_OPT = "filterDisplayOpt";
	public static final String FILTER_SELECTED = "selected";
	public static final String FILTER_LEAFS = "leafs";
	public static final String FILTER_DESCENDENTS = "descendents";
	public static final String FILTER_DATA = "filterdata";
	public static final String FILTER_CAT_DATA = "filterCatdata";
	public static final String FILTER_VOC_DATA = "filterVocdata";
	
	public static final String REFPREFERENCEID = "refPreferenceId";
	public static final String PORTLETID = "portletId";
	public static final String CONTENTID = "contentId";
	public static final String CATEGORYIDS = "categoryIds";
	public static final String TEASERTOTALCOUNT = "teasertotalcount";
	public static final String SCOPEGROUPID = "scopeGroupId";
	public static final String COMPANYID = "companyId";
	public static final String LANGUAGEID = "languageId";
	public static final String PLID = "plid";
	public static final String SECTIONPLID = "sectionPlid";
	public static final String SECURE = "secure";
	public static final String USERID = "userId";
	public static final String LIFECYCLERENDER = "lifecycleRender";
	public static final String PATHFRIENDLYURLPUBLIC = "pathFriendlyURLPublic";
	public static final String PATHFRIENDLYURLPRIVATEUSER = "pathFriendlyURLPrivateUser";
	public static final String PATHFRIENDLYURLPRIVATEGROUP = "pathFriendlyURLPrivateGroup";
	public static final String SERVERNAME = "serverName";
	public static final String CDNHOST = "cdnHost";
	public static final String PATHIMAGE = "pathImage";
	public static final String PATHMAIN = "pathMain";
	public static final String PATHCONTEXT = "pathContext";
	public static final String URLPORTAL = "urlPortal";
	public static final String PATHTHEMEIMAGES = "pathThemeImages";
	public static final String SERVERPORT = "serverPort";
	public static final String SCHEME = "scheme";
	public static final String INCLUDECURRENTCONTENT = "includeCurrentContent";
	public static final String FIRSTITEM = "firstItem";
	public static final String LASTITEM = "lastItem";
	public static final String GLOBALFIRSTITEM = "globalFirstItem";
	public static final String GLOBALLASTITEM = "globalLastItem";
	public static final String GLOBALLASTINDEX = "globalLastIndex";
	public static final String RENDERREQUEST = "renderRequest";
	public static final String RESPONSENAMESPACE = "responseNamespace";
	
	//content-type 
	public static final String TEXT_HTML_CONTENT_TYPE = "text/html";
	
	/*
	 * Layout Type settings
	 */
	public static  final String META_DESCRIPTION 			= "meta-description_%s";
	public static  final String META_KEYWORDS 				= "meta-keywords_%s";
	public static  final String META_ROBOTS 				= "meta-robots_%s";
	public static  final String JAVASCRIPT_1 				= "javascript-1";
	public static  final String JAVASCRIPT_2 				= "javascript-2";
	public static  final String JAVASCRIPT_3 				= "javascript-3";
	public static  final String SITEMAP_INCLUDE 			= "sitemap-include";
	public static  final String SITEMAP_PRIORITY 			= "sitemap-priority";
	public static  final String SITEMAP_CHANGE_FREQUENCY	= "sitemap-changefreq";
	public static  final String TARGET						= "target";
	public static  final String LINK_TO_URL					= "linkToUrl";
	public static  final String URL							= "url";
	public static  final String LINK_TO_LAYOUT_ID			= "linkToLayoutId";
	public static  final String PRIVATE_LAYOUT				= "privateLayout";
	
	public static  final String GROUPID						= "groupId";
	public static  final String GROUPNAME					= "groupname";
	public static  final String IMPORT_PROCESS				= "importProcess";
	
	/*
	 * Elementos y atributos para contruir XML
	 */
	public final static String	ELEM_METADATA		= "metadata";
	public final static String	ELEM_VOCABULARY		= "vocabulary";
	public final static String	ELEM_CATEGORIES		= "categories";
	public final static String	ELEM_CATEGORY		= "category";
	public final static String	ELEM_PROPERTIES		= "properties";
	public final static String	ELEM_RELATEDCONTENT	= "relatedcontent";
	public final static String	ELEM_SECTIONS		= "sections";
	public final static String	ELEM_SECTION		= "section";
	public final static String	ELEM_LINK			= "link";
	public final static String	ELEM_URLSET			= "urlset";
	public final static String	ELEM_URL			= "url";
	public final static String	ELEM_LOC			= "loc";
	public final static String	ELEM_LASTMOD		= "lastmod";
	public final static String	ELEM_ARTICLE 		= "article";
	public final static String	ELEM_SUBSCRIPTIONS	= "subscriptions";
	public final static String	ELEM_SUBSCRIPTION	= "subscription";
	public final static String	ELEM_CONTENT		= "content";
	public final static String	ELEM_GROUPS			= "groups";
	public final static String	ELEM_GROUP			= "group";

	public final static String	ATTR_CREATEDATE		= "createdate";
	public final static String	ATTR_MODIFIEDDATE	= "modifieddate";
	public final static String	ATTR_REL			= "rel";
	public final static String	ATTR_HREF			= "href";
	public final static String	ATTR_NAME			= "name";
	public final static String	ATTR_MAIN			= "main";
	public final static String	ATTR_SET			= "set";
	public final static String	ATTR_URL			= "url";
	public final static String	ATTR_Q				= "q";
	public final static String	ATTR_VALIDFROM		= "validfrom";
	public final static String	ATTR_VALIDTHRU		= "validthru";
	public final static String	ATTR_VALUE_SELF		= "self";
	public final static String	ATTR_ABOUT			= "about";
	public final static String	ATTR_ARTICLE_ID 	= "articleid";
	public final static String	ATTR_SITEGLOBAL_ID 	= "siteglobalid";
	public final static String	ATTR_SITELOCALID 	= "sitelocalid";
	public final static String	ATTR_COMMENTS 		= "comments";
	public final static String	ATTR_INDEXABLE 		= "indexable";
	public final static String	ATTR_STRUCTURE 		= "structure";
	public final static String	ATTR_TITLE 			= "title";
	public final static String	ATTR_GID 			= "gid";
	public final static String	ATTR_LOCALID 		= "localid";
	public final static String	ATTR_MODELGID 		= "modelgid";
	public final static String	ATTR_MODELLOCALID 	= "modellocalid";
	public final static String	ATTR_SECTIONGID 	= "sectiongid";
	public final static String	ATTR_SECTIONLOCALID = "sectionlocalid";
	public final static String	ATTR_PUBLICABLE		= "publicable";
	public final static String	ATTR_MODEL			= "model";
	public final static String	ATTR_DEFAULTMODEL	= "defaultmodel";
	public final static String	ATTR_MODELID		= "modelid";
	public final static String	ATTR_HIDEADV		= "hideadv";
}
