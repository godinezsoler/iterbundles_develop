package org.apache.jsp.html.metadata_002dpage_002dportlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import com.liferay.portal.kernel.util.sectionservers.SectionServersMgr;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.news.util.SectionMetadataControlUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import java.util.Locale;
import com.liferay.portal.kernel.language.LanguageUtil;
import java.util.ArrayList;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.news.util.CategoryMetadataControlUtil;
import com.liferay.portal.kernel.util.IMetadataControlUtil;
import com.protecmedia.iter.news.util.TopicsUtil;
import java.util.List;
import com.liferay.portal.kernel.xml.Element;
import com.protecmedia.iter.news.util.RobotsControlUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.protecmedia.iter.news.util.MetadataValidator;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.liferay.portal.kernel.util.MetadataControlUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public final class view_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


private static Log _log = LogFactoryUtil.getLog("news-portlet.docroot.html.metadata-page-portlet.viewMetadataPage_jsp");

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(6);
    _jspx_dependants.add("/WEB-INF/tld/liferay-portlet.tld");
    _jspx_dependants.add("/WEB-INF/tld/c.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-portlet-ext.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-theme.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-ui.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-util.tld");
  }

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fportlet_005fdefineObjects_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fliferay_002dutil_005fhtml_002dbottom;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fliferay_002dui_005ferror_0026_005fmessage_005fkey_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fliferay_002dui_005fsuccess_0026_005fmessage_005fkey_005fnobody;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005fportlet_005fdefineObjects_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fliferay_002dutil_005fhtml_002dbottom = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fliferay_002dui_005ferror_0026_005fmessage_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fliferay_002dui_005fsuccess_0026_005fmessage_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005fportlet_005fdefineObjects_005fnobody.release();
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody.release();
    _005fjspx_005ftagPool_005fliferay_002dutil_005fhtml_002dbottom.release();
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
    _005fjspx_005ftagPool_005fliferay_002dui_005ferror_0026_005fmessage_005fkey_005fnobody.release();
    _005fjspx_005ftagPool_005fliferay_002dui_005fsuccess_0026_005fmessage_005fkey_005fnobody.release();
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  portlet:defineObjects
      com.liferay.taglib.portlet.DefineObjectsTag _jspx_th_portlet_005fdefineObjects_005f0 = (com.liferay.taglib.portlet.DefineObjectsTag) _005fjspx_005ftagPool_005fportlet_005fdefineObjects_005fnobody.get(com.liferay.taglib.portlet.DefineObjectsTag.class);
      _jspx_th_portlet_005fdefineObjects_005f0.setPageContext(_jspx_page_context);
      _jspx_th_portlet_005fdefineObjects_005f0.setParent(null);
      int _jspx_eval_portlet_005fdefineObjects_005f0 = _jspx_th_portlet_005fdefineObjects_005f0.doStartTag();
      if (_jspx_th_portlet_005fdefineObjects_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fportlet_005fdefineObjects_005fnobody.reuse(_jspx_th_portlet_005fdefineObjects_005f0);
        return;
      }
      _005fjspx_005ftagPool_005fportlet_005fdefineObjects_005fnobody.reuse(_jspx_th_portlet_005fdefineObjects_005f0);
      javax.portlet.ActionRequest actionRequest = null;
      javax.portlet.ActionResponse actionResponse = null;
      javax.portlet.EventRequest eventRequest = null;
      javax.portlet.EventResponse eventResponse = null;
      javax.portlet.PortletConfig portletConfig = null;
      java.lang.String portletName = null;
      javax.portlet.PortletPreferences portletPreferences = null;
      java.util.Map portletPreferencesValues = null;
      javax.portlet.PortletSession portletSession = null;
      java.util.Map portletSessionScope = null;
      javax.portlet.RenderRequest renderRequest = null;
      javax.portlet.RenderResponse renderResponse = null;
      javax.portlet.ResourceRequest resourceRequest = null;
      javax.portlet.ResourceResponse resourceResponse = null;
      actionRequest = (javax.portlet.ActionRequest) _jspx_page_context.findAttribute("actionRequest");
      actionResponse = (javax.portlet.ActionResponse) _jspx_page_context.findAttribute("actionResponse");
      eventRequest = (javax.portlet.EventRequest) _jspx_page_context.findAttribute("eventRequest");
      eventResponse = (javax.portlet.EventResponse) _jspx_page_context.findAttribute("eventResponse");
      portletConfig = (javax.portlet.PortletConfig) _jspx_page_context.findAttribute("portletConfig");
      portletName = (java.lang.String) _jspx_page_context.findAttribute("portletName");
      portletPreferences = (javax.portlet.PortletPreferences) _jspx_page_context.findAttribute("portletPreferences");
      portletPreferencesValues = (java.util.Map) _jspx_page_context.findAttribute("portletPreferencesValues");
      portletSession = (javax.portlet.PortletSession) _jspx_page_context.findAttribute("portletSession");
      portletSessionScope = (java.util.Map) _jspx_page_context.findAttribute("portletSessionScope");
      renderRequest = (javax.portlet.RenderRequest) _jspx_page_context.findAttribute("renderRequest");
      renderResponse = (javax.portlet.RenderResponse) _jspx_page_context.findAttribute("renderResponse");
      resourceRequest = (javax.portlet.ResourceRequest) _jspx_page_context.findAttribute("resourceRequest");
      resourceResponse = (javax.portlet.ResourceResponse) _jspx_page_context.findAttribute("resourceResponse");
      out.write('\r');
      out.write('\n');
      //  liferay-theme:defineObjects
      com.liferay.taglib.theme.DefineObjectsTag _jspx_th_liferay_002dtheme_005fdefineObjects_005f0 = (com.liferay.taglib.theme.DefineObjectsTag) _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody.get(com.liferay.taglib.theme.DefineObjectsTag.class);
      _jspx_th_liferay_002dtheme_005fdefineObjects_005f0.setPageContext(_jspx_page_context);
      _jspx_th_liferay_002dtheme_005fdefineObjects_005f0.setParent(null);
      int _jspx_eval_liferay_002dtheme_005fdefineObjects_005f0 = _jspx_th_liferay_002dtheme_005fdefineObjects_005f0.doStartTag();
      if (_jspx_th_liferay_002dtheme_005fdefineObjects_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody.reuse(_jspx_th_liferay_002dtheme_005fdefineObjects_005f0);
        return;
      }
      _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody.reuse(_jspx_th_liferay_002dtheme_005fdefineObjects_005f0);
      com.liferay.portal.theme.ThemeDisplay themeDisplay = null;
      com.liferay.portal.model.Company company = null;
      com.liferay.portal.model.Account account = null;
      com.liferay.portal.model.User user = null;
      com.liferay.portal.model.User realUser = null;
      com.liferay.portal.model.Contact contact = null;
      com.liferay.portal.model.Layout layout = null;
      java.util.List layouts = null;
      java.lang.Long plid = null;
      com.liferay.portal.model.LayoutTypePortlet layoutTypePortlet = null;
      java.lang.Long scopeGroupId = null;
      com.liferay.portal.security.permission.PermissionChecker permissionChecker = null;
      java.util.Locale locale = null;
      java.util.TimeZone timeZone = null;
      com.liferay.portal.model.Theme theme = null;
      com.liferay.portal.model.ColorScheme colorScheme = null;
      com.liferay.portal.theme.PortletDisplay portletDisplay = null;
      java.lang.Long portletGroupId = null;
      themeDisplay = (com.liferay.portal.theme.ThemeDisplay) _jspx_page_context.findAttribute("themeDisplay");
      company = (com.liferay.portal.model.Company) _jspx_page_context.findAttribute("company");
      account = (com.liferay.portal.model.Account) _jspx_page_context.findAttribute("account");
      user = (com.liferay.portal.model.User) _jspx_page_context.findAttribute("user");
      realUser = (com.liferay.portal.model.User) _jspx_page_context.findAttribute("realUser");
      contact = (com.liferay.portal.model.Contact) _jspx_page_context.findAttribute("contact");
      layout = (com.liferay.portal.model.Layout) _jspx_page_context.findAttribute("layout");
      layouts = (java.util.List) _jspx_page_context.findAttribute("layouts");
      plid = (java.lang.Long) _jspx_page_context.findAttribute("plid");
      layoutTypePortlet = (com.liferay.portal.model.LayoutTypePortlet) _jspx_page_context.findAttribute("layoutTypePortlet");
      scopeGroupId = (java.lang.Long) _jspx_page_context.findAttribute("scopeGroupId");
      permissionChecker = (com.liferay.portal.security.permission.PermissionChecker) _jspx_page_context.findAttribute("permissionChecker");
      locale = (java.util.Locale) _jspx_page_context.findAttribute("locale");
      timeZone = (java.util.TimeZone) _jspx_page_context.findAttribute("timeZone");
      theme = (com.liferay.portal.model.Theme) _jspx_page_context.findAttribute("theme");
      colorScheme = (com.liferay.portal.model.ColorScheme) _jspx_page_context.findAttribute("colorScheme");
      portletDisplay = (com.liferay.portal.theme.PortletDisplay) _jspx_page_context.findAttribute("portletDisplay");
      portletGroupId = (java.lang.Long) _jspx_page_context.findAttribute("portletGroupId");
      out.write("\r\n");
      out.write("\r\n");
      //  liferay-util:html-bottom
      com.liferay.taglib.util.HtmlBottomTag _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0 = (com.liferay.taglib.util.HtmlBottomTag) _005fjspx_005ftagPool_005fliferay_002dutil_005fhtml_002dbottom.get(com.liferay.taglib.util.HtmlBottomTag.class);
      _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.setPageContext(_jspx_page_context);
      _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.setParent(null);
      int _jspx_eval_liferay_002dutil_005fhtml_002dbottom_005f0 = _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.doStartTag();
      if (_jspx_eval_liferay_002dutil_005fhtml_002dbottom_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        if (_jspx_eval_liferay_002dutil_005fhtml_002dbottom_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
          out = _jspx_page_context.pushBody();
          _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
          _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.doInitBody();
        }
        do {
          out.write("\r\n");
          out.write("\r\n");
          out.write("\t");

if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())
{
		long globalGroupId 		 = company.getGroup().getGroupId();
		boolean isDateOrMetaURL  = false;
		String urlSeparator 	 = PortalUtil.getUrlSeparator();
		String contentId 		 = null;
		String _url 			 = themeDisplay.getURLCurrent();
		String portletID		 = (String)renderRequest.getAttribute(WebKeys.PORTLET_ID);
		boolean isSectionLayout		 = false;
		IMetadataControlUtil metaUtil = null;
		
		List<String> categoryIds = new ArrayList<String>();
		
		if ( _url.contains( urlSeparator ) )
		{
			int pos = _url.indexOf( urlSeparator );
			if(pos!=-1)
			{
				String auxUrl = _url.substring(pos+urlSeparator.length());
				int endIdx = auxUrl.indexOf("/")!=-1 ? auxUrl.indexOf("/") : auxUrl.length();
				if ( !auxUrl.substring(0, endIdx).equals("date") && !auxUrl.substring(0, endIdx).equals("meta") )
				{
					contentId = request.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
					
					if ((contentId == null || contentId.equals("")) && themeDisplay.isSignedIn() && (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))) 
					{
						contentId = IterKeys.EXAMPLEARTICLEID;
					}
				}
				else if (auxUrl.substring(0, endIdx).equals("meta"))
				{
					List<String> tmpCategoryIds = TopicsUtil.getCategoriesIds(renderRequest);

					if (! (categoryIds != null && categoryIds.size() == 1 && categoryIds.get(0).equals("-1")) )
						categoryIds = tmpCategoryIds;
				}
			}
		}
		else 
			if( !_url.contains( PortalUtil.getSearchSeparator() ) )
				isSectionLayout = (layout!=null);

	
          out.write("\r\n");
          out.write("   \r\n");
          out.write("   ");
          //  c:if
          org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
          _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
          _jspx_th_c_005fif_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0);
          // /html/metadata-page-portlet/view.jsp(117,3) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fif_005f0.setTest( !categoryIds.isEmpty() || (contentId != null && !contentId.equals("")) || isSectionLayout);
          int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
          if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\t\r\n");
              out.write("   ");

   		// Es una página de detalle de un artículo, de detalle de categoría o una página de sección
	   	try
	    {
	   		Document docMetaRobots		 = null;

	   		if(!categoryIds.isEmpty())
	   		{
	   			_log.debug("CategoryMetadataControlUtil");
	   			
	   			// Detalle de categoría
	   			metaUtil 		= ((IMetadataControlUtil) new CategoryMetadataControlUtil(themeDisplay, categoryIds));
	   		}
	   		else
	   		{
	   			if(contentId != null && !contentId.equals(""))
		   		{
	   				_log.debug("MetadataControlUtil");
	   				
		   			// Detalle de artículo
		   			JournalArticle webContent = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
		   			metaUtil 		= ((IMetadataControlUtil) new MetadataControlUtil(themeDisplay, webContent, locale.toString()));
		   			try
		   			{
		   	   			docMetaRobots 	= new RobotsControlUtil().getMetaRobots(webContent, themeDisplay, request);
		   			}
		   			catch (Throwable th)
		   			{
		   				_log.error(th);
		   			}
		   		}
	   			else if(isSectionLayout)
	   			{
	   				// Página de sección
	   				_log.debug("SectionMetadataControlUtil");
		   			metaUtil = ( (IMetadataControlUtil) new SectionMetadataControlUtil(themeDisplay, request, layout, (contentId != null && !contentId.equals(""))) );
	   			}
	   		}
	   			   		
	   		if(metaUtil != null)
	   		{
	   	   		String pageTitle = metaUtil.getPageTitle();
	   	   		if (!pageTitle.isEmpty())
	   	   			PortalUtil.setPageTitle( pageTitle, request );
	   	   		
	   	   		String pageDescription = metaUtil.getPageDescription();
	   	   		if (!pageDescription.isEmpty())
	   	   			PortalUtil.setPageDescription( pageDescription,	request );
	   	   		
	   	   		String pageKeywords = metaUtil.getPageKeywords();
	   	   		if (!pageKeywords.isEmpty())
	   	   			PortalUtil.addPageKeywords(pageKeywords, request);
	   	   		
	   	   		Document doc = metaUtil.getPageOpenGraphs(request);
	   	   		
	   	   		//ticket: 0009741. Añadir '<link rel="canonical"...' en las páginas de sección y de metadatos
	   	   		if( !categoryIds.isEmpty() || isSectionLayout )
	   	   		{
	   	   			//Página de sección o detalle de categoría. 
	   	   			
	   	   			//se extrae la url canonica de doc 
	   	   			String canonicalURL = XMLHelper.getStringValueOf( doc, "/rs/meta[@namevalue='og:url']/content");
	   	   			
	   	   			//si layout es la sección home
	   	   			String host = IterURLUtil.getIterHost();
	   	   			
	   	   			String isLayoutHome =  (String) request.getAttribute( WebKeys.IS_LAYOUT_HOME );
	   	   			if( Validator.isNotNull(isLayoutHome) && isLayoutHome.equals("true") )
	   	   			{
	   	   				canonicalURL = host.concat(IterURLUtil.getIterURLPrefix());
	   	   			}
	
	   	   			
	   	   			//se rellena docMetaRobots con la información de la url canónica
	   	   			docMetaRobots  	 = 	SAXReaderUtil.createDocument();
	   	 			Element eleRS	 = 	docMetaRobots.addElement("rs");
	   	 			
	   	 			Element elem = eleRS.addElement("link");
	   			
	   				elem.addAttribute( "rel", "canonical" );
	   				elem.addAttribute( "href", canonicalURL );
	   	   			
	   				if( IterRequest.isMobileRequest()==0 && IterGlobal.getMobileToken(themeDisplay.getScopeGroupId())!="" )
	   				{
	   					String mobileURL = SectionServersMgr.processMobileURL(host, IterURLUtil.buildMobileURL());
	   					
	   					if( !mobileURL.equalsIgnoreCase(canonicalURL) )
	   					{
		   					elem = eleRS.addElement("link");
		   					
		   					elem.addAttribute( "rel", "alternate" );
			   				elem.addAttribute( "href", mobileURL );
			   				elem.addAttribute( "media", "only screen and (max-width: 640px)");
	   					}
	   				}
	   	   		}
	   	   		if (docMetaRobots != null)
	   	   		{
	   	   			Element elemRoot	= docMetaRobots.getRootElement();
	   	   			Element docRoot		= doc.getRootElement();
	   	   			List<Element> eList = elemRoot.elements();
	   	   			
	   	   			for (Element e : eList)
	   	   			{
	   	   			 	docRoot.add(e.createCopy());
	   	   			}
	   	   		}
		   	   		
	   	   		_log.debug(doc.asXML());
	   	   		PortalUtil.setPageOpenGraphs( doc.asXML(),	request );
	   		}
   		}
  		catch(PortalException e)
   		{
  			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
  			{
		   		SessionErrors.add(renderRequest, MetadataValidator.getKey(MetadataValidator.ERR_INVALID_JOURNALSTRUCT, 	portletID));
  			}
  			_log.error(e);
   		}
   		catch(SystemException e)
   		{
   			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
   			{
		   		SessionErrors.add(renderRequest, MetadataValidator.getKey(MetadataValidator.ERR_INVALID_JOURNALSTRUCT, 	portletID));
   			}
   			_log.error(e);
   		}
   		catch(DocumentException e)
   		{
   			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
   			{
		   		SessionErrors.add(renderRequest, MetadataValidator.getKey(MetadataValidator.ERR_INVALID_JOURNALSTRUCT_DOM, portletID));
   			}
   			_log.error(e);
   		}
  		catch(Exception e)
  		{
  			_log.error(e);
  		}
	      

   		// Se pasan al PortletRequest los errores del HttpRequest
   		MetadataValidator.validateRequest(request, renderRequest, portletID);

   		// Se añaden todos los errores de este portlet
   		KeyValuePair[] list = MetadataValidator.getErrors(renderRequest, portletID);
   		
   		for (KeyValuePair pair : list)
   		{
	   		
              out.write("\r\n");
              out.write("   \t        ");
              //  liferay-ui:error
              com.liferay.taglib.ui.ErrorTag _jspx_th_liferay_002dui_005ferror_005f0 = (com.liferay.taglib.ui.ErrorTag) _005fjspx_005ftagPool_005fliferay_002dui_005ferror_0026_005fmessage_005fkey_005fnobody.get(com.liferay.taglib.ui.ErrorTag.class);
              _jspx_th_liferay_002dui_005ferror_005f0.setPageContext(_jspx_page_context);
              _jspx_th_liferay_002dui_005ferror_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
              // /html/metadata-page-portlet/view.jsp(269,12) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_liferay_002dui_005ferror_005f0.setKey(pair.getKey());
              // /html/metadata-page-portlet/view.jsp(269,12) name = message type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_liferay_002dui_005ferror_005f0.setMessage(pair.getValue());
              int _jspx_eval_liferay_002dui_005ferror_005f0 = _jspx_th_liferay_002dui_005ferror_005f0.doStartTag();
              if (_jspx_th_liferay_002dui_005ferror_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005fliferay_002dui_005ferror_0026_005fmessage_005fkey_005fnobody.reuse(_jspx_th_liferay_002dui_005ferror_005f0);
                return;
              }
              _005fjspx_005ftagPool_005fliferay_002dui_005ferror_0026_005fmessage_005fkey_005fnobody.reuse(_jspx_th_liferay_002dui_005ferror_005f0);
              out.write("\r\n");
              out.write("   \t    \t");

   		}
   		
   		// Se añaden los mensajes
   		list = MetadataValidator.getSuccess(renderRequest, portletID);
   		for (KeyValuePair pair : list)
   		{
   	        
              out.write("\r\n");
              out.write("   \t        ");
              //  liferay-ui:success
              com.liferay.taglib.ui.SuccessTag _jspx_th_liferay_002dui_005fsuccess_005f0 = (com.liferay.taglib.ui.SuccessTag) _005fjspx_005ftagPool_005fliferay_002dui_005fsuccess_0026_005fmessage_005fkey_005fnobody.get(com.liferay.taglib.ui.SuccessTag.class);
              _jspx_th_liferay_002dui_005fsuccess_005f0.setPageContext(_jspx_page_context);
              _jspx_th_liferay_002dui_005fsuccess_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
              // /html/metadata-page-portlet/view.jsp(278,12) name = key type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_liferay_002dui_005fsuccess_005f0.setKey(pair.getKey());
              // /html/metadata-page-portlet/view.jsp(278,12) name = message type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_liferay_002dui_005fsuccess_005f0.setMessage(pair.getValue());
              int _jspx_eval_liferay_002dui_005fsuccess_005f0 = _jspx_th_liferay_002dui_005fsuccess_005f0.doStartTag();
              if (_jspx_th_liferay_002dui_005fsuccess_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005fliferay_002dui_005fsuccess_0026_005fmessage_005fkey_005fnobody.reuse(_jspx_th_liferay_002dui_005fsuccess_005f0);
                return;
              }
              _005fjspx_005ftagPool_005fliferay_002dui_005fsuccess_0026_005fmessage_005fkey_005fnobody.reuse(_jspx_th_liferay_002dui_005fsuccess_005f0);
              out.write("\r\n");
              out.write("   \t    \t");

   		}
   	
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fif_005f0.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fif_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
          out.write("\r\n");
          out.write("\t\r\n");
          out.write("\t");
          //  c:if
          org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
          _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
          _jspx_th_c_005fif_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0);
          // /html/metadata-page-portlet/view.jsp(284,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fif_005f1.setTest( layout!=null );
          int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
          if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write('\r');
              out.write('\n');
              out.write('	');

		SectionMetadataControlUtil sm = null;	
		if( metaUtil instanceof SectionMetadataControlUtil)
			sm = (SectionMetadataControlUtil)metaUtil;
		else
			sm = new SectionMetadataControlUtil(themeDisplay, request, layout, (contentId != null && !contentId.equals("")));
  		
  		PortalUtil.setPageMetaTags( PortalUtil.getOriginalServletRequest(request), sm.getCommonMetaTags().asXML() );
	
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fif_005f1.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fif_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f1);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f1);
          out.write("\r\n");
          out.write(" \r\n");

} //if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())

          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
        if (_jspx_eval_liferay_002dutil_005fhtml_002dbottom_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
          out = _jspx_page_context.popBody();
        }
      }
      if (_jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fliferay_002dutil_005fhtml_002dbottom.reuse(_jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0);
        return;
      }
      _005fjspx_005ftagPool_005fliferay_002dutil_005fhtml_002dbottom.reuse(_jspx_th_liferay_002dutil_005fhtml_002dbottom_005f0);
      out.write('\r');
      out.write('\n');
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
