package org.apache.jsp.html.common.themes;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import com.liferay.portal.kernel.util.ContentTypes;
import com.protecmedia.iter.base.service.util.Preloading;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.advertisement.SlotAssignment;
import com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools;
import com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.comments.CommentsConfigBean;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portlet.TeaserUtil;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portlet.ContextVariables;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portlet.AdvertisementUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.WebResourceUtil;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.captcha.CaptchaMaxChallengesException;
import com.liferay.portal.kernel.captcha.CaptchaTextException;
import com.liferay.portal.kernel.bean.BeanParamUtil;
import com.liferay.portal.kernel.bean.BeanPropertiesUtil;
import com.liferay.portal.kernel.cal.Recurrence;
import com.liferay.portal.kernel.configuration.Filter;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.search.DAOParamUtil;
import com.liferay.portal.kernel.dao.search.DisplayTerms;
import com.liferay.portal.kernel.dao.search.ResultRow;
import com.liferay.portal.kernel.dao.search.RowChecker;
import com.liferay.portal.kernel.dao.search.ScoreSearchEntry;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.dao.search.SearchEntry;
import com.liferay.portal.kernel.dao.search.TextSearchEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.unsync.UnsyncStringReader;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.language.LanguageWrapper;
import com.liferay.portal.kernel.language.UnicodeLanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.log.LogUtil;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.portlet.DynamicRenderRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletMode;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.servlet.BrowserSnifferUtil;
import com.liferay.portal.kernel.servlet.ImageServletTokenUtil;
import com.liferay.portal.kernel.servlet.ServletContextUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.servlet.StringServletResponse;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.BooleanWrapper;
import com.liferay.portal.kernel.util.BreadcrumbsUtil;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.CalendarUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IntegerWrapper;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.KeyValuePairComparator;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.util.MathUtil;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.ObjectValuePairComparator;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderedProperties;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PrefsParamUtil;
import com.liferay.portal.kernel.util.PropertiesParamUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.Randomizer;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.ServerDetector;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.SortedProperties;
import com.liferay.portal.kernel.util.StackTraceUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringComparator;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.TimeZoneUtil;
import com.liferay.portal.kernel.util.UnicodeFormatter;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.language.LanguageImpl;
import com.liferay.portal.model.*;
import com.liferay.portal.model.impl.*;
import com.liferay.portal.security.auth.AuthTokenUtil;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.*;
import com.liferay.portal.service.permission.GroupPermissionUtil;
import com.liferay.portal.service.permission.LayoutPermissionUtil;
import com.liferay.portal.service.permission.LayoutPrototypePermissionUtil;
import com.liferay.portal.service.permission.LayoutSetPrototypePermissionUtil;
import com.liferay.portal.service.permission.PortletPermissionUtil;
import com.liferay.portal.struts.StrutsUtil;
import com.liferay.portal.theme.PortletDisplay;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.ContentUtil;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.JavaScriptBundleUtil;
import com.liferay.portal.util.Portal;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletCategoryKeys;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.util.SessionClicks;
import com.liferay.portal.util.SessionTreeJSClicks;
import com.liferay.portal.util.ShutdownUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.portal.util.WebKeys;
import com.liferay.portal.util.comparator.PortletCategoryComparator;
import com.liferay.portal.util.comparator.PortletTitleComparator;
import com.liferay.portlet.InvokerPortlet;
import com.liferay.portlet.PortalPreferences;
import com.liferay.portlet.PortletConfigFactoryUtil;
import com.liferay.portlet.PortletInstanceFactoryUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.PortletResponseImpl;
import com.liferay.portlet.PortletSetupUtil;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.portlet.PortletURLUtil;
import com.liferay.portlet.RenderParametersPool;
import com.liferay.portlet.RenderRequestFactory;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseFactory;
import com.liferay.portlet.RenderResponseImpl;
import com.liferay.portlet.UserAttributes;
import com.liferay.portlet.portletconfiguration.util.PortletConfigurationUtil;
import com.liferay.util.CreditCard;
import com.liferay.util.Encryptor;
import com.liferay.util.JS;
import com.liferay.util.PKParser;
import com.liferay.util.PwdGenerator;
import com.liferay.util.State;
import com.liferay.util.StateUtil;
import com.liferay.util.TextFormatter;
import com.liferay.util.UniqueList;
import com.liferay.util.format.PhoneNumberUtil;
import com.liferay.util.log4j.Levels;
import com.liferay.util.mail.InternetAddressUtil;
import com.liferay.util.portlet.PortletRequestUtil;
import com.liferay.util.servlet.DynamicServletRequest;
import com.liferay.util.servlet.SessionParameters;
import com.liferay.util.servlet.UploadException;
import com.liferay.util.xml.XMLFormatter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.portlet.MimeResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceURL;
import javax.portlet.UnavailableException;
import javax.portlet.ValidatorException;
import javax.portlet.WindowState;
import org.apache.commons.math.util.MathUtils;
import org.apache.struts.Globals;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.protecmedia.iter.base.service.util.JQryIterExtensionTools;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.util.WebKeys;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.Http;
import java.net.URL;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.CatalogUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.theme.ThemeDisplay;

public final class top_005fhead_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


	private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.html.common.themes.top_head_jsp");

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(29);
    _jspx_dependants.add("/html/common/init.jsp");
    _jspx_dependants.add("/html/common/init-ext.jsp");
    _jspx_dependants.add("/html/common/themes/top_meta.jspf");
    _jspx_dependants.add("/html/common/themes/top_meta-ext.jsp");
    _jspx_dependants.add("/html/common/themes/top_js.jspf");
    _jspx_dependants.add("/html/common/themes/JQryIterExtension.jspf");
    _jspx_dependants.add("/html/portlet/dockbar/dockbar_ngportlets.jspf");
    _jspx_dependants.add("/WEB-INF/tld/displaytag.tld");
    _jspx_dependants.add("/WEB-INF/tld/c.tld");
    _jspx_dependants.add("/WEB-INF/tld/fmt.tld");
    _jspx_dependants.add("/WEB-INF/tld/fn.tld");
    _jspx_dependants.add("/WEB-INF/tld/sql.tld");
    _jspx_dependants.add("/WEB-INF/tld/x.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-portlet.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-aui.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-portlet-ext.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-security.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-theme.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-ui.tld");
    _jspx_dependants.add("/WEB-INF/tld/liferay-util.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-bean.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-bean-el.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-html.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-html-el.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-logic.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-logic-el.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-nested.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-tiles.tld");
    _jspx_dependants.add("/WEB-INF/tld/struts-tiles-el.tld");
  }

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fliferay_002dtheme_005fmeta_002dtags_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fchoose;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fotherwise;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fmeta_002dtags_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fchoose = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fotherwise = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fdefineObjects_005fnobody.release();
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fmeta_002dtags_005fnobody.release();
    _005fjspx_005ftagPool_005fc_005fchoose.release();
    _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.release();
    _005fjspx_005ftagPool_005fc_005fotherwise.release();
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
      response.setContentType("text/html; charset=UTF-8");
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

/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
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
      out.write('\n');
      out.write('\n');

/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

      out.write('\r');
      out.write('\n');

/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

      out.write("\r\n");
      out.write("\r\n");

/**
 * #0010258
 *
 * PRdV
 * Elimino el tag que declara la codificación de la página para llevarlo hasta justo antes del script de redirección móvil en portal_normal.vm
 * <meta content="<%= ContentTypes.TEXT_HTML_UTF8 >" http-equiv="content-type" />
 **/

      out.write("\r\n");
      out.write("\r\n");

String refreshRate = request.getParameter("refresh_rate");

      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f0.setParent(null);
      // /html/common/themes/top_meta.jspf(31,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f0.setTest( (refreshRate != null) && (!refreshRate.equals("0")) );
      int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
      if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t<meta content=\"");
          out.print( refreshRate );
          out.write(";\" http-equiv=\"Refresh\" />\r\n");
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
      out.write("\r\n");

String cacheControl = request.getParameter("cache_control");

      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f1.setParent(null);
      // /html/common/themes/top_meta.jspf(39,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f1.setTest( (cacheControl != null) && (cacheControl.equals("0")) );
      int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
      if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t<meta content=\"no-cache\" http-equiv=\"Cache-Control\" />\r\n");
          out.write("\t<meta content=\"no-cache\" http-equiv=\"Pragma\" />\r\n");
          out.write("\t<meta content=\"0\" http-equiv=\"Expires\" />\r\n");
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
      out.write("\r\n");
      if (_jspx_meth_liferay_002dtheme_005fmeta_002dtags_005f0(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');

/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

	boolean themeDatabaseEnabled    = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_THEME_DATABASE_ENABLED), true) &&
									  	IterKeys.DEFAULT_THEME.equalsIgnoreCase(themeDisplay.getThemeId());

	boolean usePortletsOwnResources = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_PORTLETS_USE_OWN_RESOURCES), false);

	HttpServletRequest original_request = PortalUtil.getOriginalServletRequest(request);
	Object isNewsletterPageObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_IS_NEWSLETTER_PAGE);
	boolean isNewsletterPage = false;
	if(isNewsletterPageObject != null)
		isNewsletterPage = GetterUtil.getBoolean(isNewsletterPageObject.toString());

	String versionRsrc = (themeDisplay.isSignedIn() || !isNewsletterPage) 															? 
							String.format("/base-portlet/webrsrc/%s.js", WebResourceUtil.getMD5WebResource(WebResourceUtil.HEADER))	:
							""; 

      out.write("\r\n");
      out.write("\r\n");
      out.write('\r');
      out.write('\n');
      out.write(' ');
      out.write('\r');
      out.write('\n');
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f2 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f2.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f2.setParent(null);
      // /html/common/themes/top_head.jsp(62,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f2.setTest(themeDisplay.isSignedIn() || !isNewsletterPage);
      int _jspx_eval_c_005fif_005f2 = _jspx_th_c_005fif_005f2.doStartTag();
      if (_jspx_eval_c_005fif_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:if
          org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f3 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
          _jspx_th_c_005fif_005f3.setPageContext(_jspx_page_context);
          _jspx_th_c_005fif_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f2);
          // /html/common/themes/top_head.jsp(63,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fif_005f3.setTest(Preloading.iterRsrc(themeDisplay.getScopeGroupId()));
          int _jspx_eval_c_005fif_005f3 = _jspx_th_c_005fif_005f3.doStartTag();
          if (_jspx_eval_c_005fif_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t");
              out.print(IterGlobal.getPreloadContent(versionRsrc, ContentTypes.TEXT_JAVASCRIPT) );
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fif_005f3.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fif_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f3);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f3);
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f2.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f2);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f2);
      out.write("\r\n");
      out.write("\r\n");
      out.write('\r');
      out.write('\n');
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f4 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f4.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f4.setParent(null);
      // /html/common/themes/top_head.jsp(69,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f4.setTest( themeDatabaseEnabled && !layout.isTypeControlPanel() && !isNewsletterPage);
      int _jspx_eval_c_005fif_005f4 = _jspx_th_c_005fif_005f4.doStartTag();
      if (_jspx_eval_c_005fif_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          out.print(ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, 
			Preloading.themeRsrcContentTypes(themeDisplay.getScopeGroupId()), true));
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f4.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f4);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f4);
      out.write('\r');
      out.write('\n');
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f5 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f5.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f5.setParent(null);
      // /html/common/themes/top_head.jsp(77,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f5.setTest( themeDatabaseEnabled && !layout.isTypeControlPanel());
      int _jspx_eval_c_005fif_005f5 = _jspx_th_c_005fif_005f5.doStartTag();
      if (_jspx_eval_c_005fif_005f5 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          out.print(ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, ContentTypes.TEXT_HTML));
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f5.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f5);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f5);
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f6 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f6.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f6.setParent(null);
      // /html/common/themes/top_head.jsp(83,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f6.setTest( themeDisplay.isSignedIn() || !themeDatabaseEnabled || layout.isTypeControlPanel());
      int _jspx_eval_c_005fif_005f6 = _jspx_th_c_005fif_005f6.doStartTag();
      if (_jspx_eval_c_005fif_005f6 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t<link href=\"");
          out.print( HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getCDNHost() + themeDisplay.getPathContext() + "/html/portal/css.jsp")) );
          out.write("\" rel=\"stylesheet\" type=\"text/css\" />\r\n");
          int evalDoAfterBody = _jspx_th_c_005fif_005f6.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f6.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f6);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f6);
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  c:choose
      org.apache.taglibs.standard.tag.common.core.ChooseTag _jspx_th_c_005fchoose_005f0 = (org.apache.taglibs.standard.tag.common.core.ChooseTag) _005fjspx_005ftagPool_005fc_005fchoose.get(org.apache.taglibs.standard.tag.common.core.ChooseTag.class);
      _jspx_th_c_005fchoose_005f0.setPageContext(_jspx_page_context);
      _jspx_th_c_005fchoose_005f0.setParent(null);
      int _jspx_eval_c_005fchoose_005f0 = _jspx_th_c_005fchoose_005f0.doStartTag();
      if (_jspx_eval_c_005fchoose_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:when
          org.apache.taglibs.standard.tag.rt.core.WhenTag _jspx_th_c_005fwhen_005f0 = (org.apache.taglibs.standard.tag.rt.core.WhenTag) _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.WhenTag.class);
          _jspx_th_c_005fwhen_005f0.setPageContext(_jspx_page_context);
          _jspx_th_c_005fwhen_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f0);
          // /html/common/themes/top_head.jsp(90,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fwhen_005f0.setTest( !themeDatabaseEnabled || layout.isTypeControlPanel());
          int _jspx_eval_c_005fwhen_005f0 = _jspx_th_c_005fwhen_005f0.doStartTag();
          if (_jspx_eval_c_005fwhen_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t<link class=\"lfr-css-file\" href=\"");
              out.print( HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getPathThemeCss() + "/main.css")) );
              out.write("\" rel=\"stylesheet\" type=\"text/css\" />\r\n");
              out.write("\t");
              int evalDoAfterBody = _jspx_th_c_005fwhen_005f0.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fwhen_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f0);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f0);
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:otherwise
          org.apache.taglibs.standard.tag.common.core.OtherwiseTag _jspx_th_c_005fotherwise_005f0 = (org.apache.taglibs.standard.tag.common.core.OtherwiseTag) _005fjspx_005ftagPool_005fc_005fotherwise.get(org.apache.taglibs.standard.tag.common.core.OtherwiseTag.class);
          _jspx_th_c_005fotherwise_005f0.setPageContext(_jspx_page_context);
          _jspx_th_c_005fotherwise_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f0);
          int _jspx_eval_c_005fotherwise_005f0 = _jspx_th_c_005fotherwise_005f0.doStartTag();
          if (_jspx_eval_c_005fotherwise_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t");
              //  c:if
              org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f7 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
              _jspx_th_c_005fif_005f7.setPageContext(_jspx_page_context);
              _jspx_th_c_005fif_005f7.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fotherwise_005f0);
              // /html/common/themes/top_head.jsp(94,2) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fif_005f7.setTest( !isNewsletterPage );
              int _jspx_eval_c_005fif_005f7 = _jspx_th_c_005fif_005f7.doStartTag();
              if (_jspx_eval_c_005fif_005f7 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                do {
                  out.write("\r\n");
                  out.write("\t\t\t");
                  out.print(ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, ContentTypes.TEXT_CSS));
                  out.write("\t\t\r\n");
                  out.write("\t\t");
                  int evalDoAfterBody = _jspx_th_c_005fif_005f7.doAfterBody();
                  if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                    break;
                } while (true);
              }
              if (_jspx_th_c_005fif_005f7.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f7);
                return;
              }
              _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f7);
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fotherwise_005f0.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fotherwise_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f0);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f0);
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fchoose_005f0.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fchoose_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f0);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f0);
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f8 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f8.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f8.setParent(null);
      // /html/common/themes/top_head.jsp(102,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f8.setTest( (layout != null) && Validator.isNotNull(layout.getCssText()) );
      int _jspx_eval_c_005fif_005f8 = _jspx_th_c_005fif_005f8.doStartTag();
      if (_jspx_eval_c_005fif_005f8 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t<style type=\"text/css\">\r\n");
          out.write("\t\t");
          out.print( layout.getCssText() );
          out.write("\r\n");
          out.write("\t</style>\r\n");
          int evalDoAfterBody = _jspx_th_c_005fif_005f8.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f8.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f8);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f8);
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

	List<Portlet> portlets = null;
	if (layout != null)
	{
		String ppid = ParamUtil.getString(request, "p_p_id");
	
		if (ppid.equals(PortletKeys.PORTLET_CONFIGURATION)) {
			portlets = new ArrayList<Portlet>();
	
			portlets.add(PortletLocalServiceUtil.getPortletById(company.getCompanyId(), PortletKeys.PORTLET_CONFIGURATION));
	
			ppid = ParamUtil.getString(request, PortalUtil.getPortletNamespace(ppid) + "portletResource");
	
			if (Validator.isNotNull(ppid)) {
				portlets.add(PortletLocalServiceUtil.getPortletById(company.getCompanyId(), ppid));
			}
		}
		else if (layout.isTypePortlet()) {
			portlets = layoutTypePortlet.getAllPortlets();
			
			/*
			 *  ITERWEB	Luis Miguel
			 *  
			 *  Se añaden al array de portlets, donde están los de la página, aquellos que están incrustados
			 *	 en el tema y además en la lista configurada del portal-ext.properties para cargar sus css/js
			 */
			String[] embededportlets = PropsUtil.getArray( String.format(PropsKeys.ITER_EMBEDEDPORTLET_GROUPFRIENDLYURL, themeDisplay.getScopeGroup().getFriendlyURL().replace("/", ".")) );
			for(String portletid : embededportlets  )
			{
				portlets.add( PortletLocalServiceUtil.getPortletById(company.getCompanyId(), portletid) );
			}

			if (themeDisplay.isStateMaximized() || themeDisplay.isStatePopUp()) {
				if (Validator.isNotNull(ppid)) {
					Portlet portlet = PortletLocalServiceUtil.getPortletById(company.getCompanyId(), ppid);
	
					if (!portlets.contains(portlet)) {
						portlets.add(portlet);
					}
				}
			}
		}
		else if ((layout.isTypeControlPanel() || layout.isTypePanel()) && Validator.isNotNull(ppid)) {
			portlets = new ArrayList<Portlet>();
	
			portlets.add(PortletLocalServiceUtil.getPortletById(company.getCompanyId(), ppid));
		}
	
		request.setAttribute(WebKeys.LAYOUT_PORTLETS, portlets);
	}

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f9 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f9.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f9.setParent(null);
      // /html/common/themes/top_head.jsp(163,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f9.setTest( portlets != null && (usePortletsOwnResources || layout.isTypeControlPanel()));
      int _jspx_eval_c_005fif_005f9 = _jspx_th_c_005fif_005f9.doStartTag();
      if (_jspx_eval_c_005fif_005f9 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');

	Set<String> headerPortalCssSet = new LinkedHashSet<String>();
	
	List<String> portletsCssEnabled = Arrays.asList(GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_ENABLE_CSS_PORTLETS), "").split(","));
	for (Portlet portlet : portlets) 
	{
		if(portletsCssEnabled.contains(portlet.getPortletName()) || layout.isTypeControlPanel()) 
		{
			for (String headerPortalCss : portlet.getHeaderPortalCss()) 
			{
				if (!HttpUtil.hasProtocol(headerPortalCss)) 
				{
					headerPortalCss = PortalUtil.getStaticResourceURL(request, request.getContextPath() + headerPortalCss, portlet.getTimestamp());
				}
	
				if (!headerPortalCssSet.contains(headerPortalCss)) 
				{
					headerPortalCssSet.add(headerPortalCss);

          out.write("\r\n");
          out.write("\t\t\t\t\t<link href=\"");
          out.print( HtmlUtil.escape(headerPortalCss) );
          out.write("\" rel=\"stylesheet\" type=\"text/css\" />\r\n");

				}
			}
		}
	}

	Set<String> headerPortletCssSet = new LinkedHashSet<String>();

	for (Portlet portlet : portlets) {
		if(portletsCssEnabled.contains(portlet.getPortletName()) || layout.isTypeControlPanel()) 
		{
			for (String headerPortletCss : portlet.getHeaderPortletCss()) {
				if (!HttpUtil.hasProtocol(headerPortletCss)) {
					headerPortletCss = PortalUtil.getStaticResourceURL(request, portlet.getContextPath() + headerPortletCss, portlet.getTimestamp());
				}
	
				if (!headerPortletCssSet.contains(headerPortletCss)) {
					headerPortletCssSet.add(headerPortletCss);

          out.write("\r\n");
          out.write("\t\t\t\t\t<link href=\"");
          out.print( HtmlUtil.escape(headerPortletCss) );
          out.write("\" rel=\"stylesheet\" type=\"text/css\" />\r\n");

				}
			}
		}
	}

          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f9.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f9.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f9);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f9);
      out.write("\r\n");
      out.write("\r\n");
      out.write("\t");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\t");

/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

      out.write("\r\n");
      out.write("\r\n");
 // Control para que no aparezcan etiquetas js vacías: <script type="text/javascript"></script> 
String javaScriptInitialTag = "<script type=\"text/javascript\">// <![CDATA[";
boolean javaScriptInitialTagUsed = false; 
      out.write("\r\n");
      out.write("\r\n");
      out.write("\t");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f10 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f10.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f10.setParent(null);
      // /html/common/themes/top_js.jspf(21,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f10.setTest( themeDisplay.isSignedIn() || themeDisplay.getScopeGroup().getName().equals(IterKeys.GUEST_GROUP_NAME) );
      int _jspx_eval_c_005fif_005f10 = _jspx_th_c_005fif_005f10.doStartTag();
      if (_jspx_eval_c_005fif_005f10 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t\r\n");
          out.write("\t\t");
 // Tag inicio javascript
		if (!javaScriptInitialTagUsed){
			javaScriptInitialTagUsed = true;
			out.println(javaScriptInitialTag);			 	
		} 
          out.write("\r\n");
          out.write("\r\n");
          out.write("\t\t\tvar Liferay = {\r\n");
          out.write("\t\t\t\tBrowser: {\r\n");
          out.write("\t\t\t\t\tacceptsGzip: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.acceptsGzip(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetMajorVersion: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.getMajorVersion(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetRevision: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( BrowserSnifferUtil.getRevision(request) );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetVersion: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( BrowserSnifferUtil.getVersion(request) );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisAir: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isAir(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisChrome: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isChrome(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisFirefox: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isFirefox(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisGecko: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isGecko(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisIe: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isIe(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisIphone: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isIphone(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisLinux: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isLinux(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisMac: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isMac(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisMobile: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isMobile(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisMozilla: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isMozilla(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisOpera: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isOpera(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisRtf: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isRtf(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisSafari: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isSafari(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisSun: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isSun(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisWap: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isWap(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisWapXhtml: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isWapXhtml(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisWebKit: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isWebKit(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisWindows: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isWindows(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisWml: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( BrowserSnifferUtil.isWml(request) );
          out.write(";\r\n");
          out.write("\t\t\t\t\t}\r\n");
          out.write("\t\t\t\t},\r\n");
          out.write("\t\r\n");
          out.write("\t\t\t\tThemeDisplay: {\r\n");
          out.write("\t\t\t\t\tgetCompanyId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getCompanyId() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetCompanyGroupId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getCompanyGroupId() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetUserId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getUserId() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\r\n");
          out.write("\t\t\t\t\t");
          //  c:if
          org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f11 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
          _jspx_th_c_005fif_005f11.setPageContext(_jspx_page_context);
          _jspx_th_c_005fif_005f11.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f10);
          // /html/common/themes/top_js.jspf(113,5) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fif_005f11.setTest( themeDisplay.isSignedIn() );
          int _jspx_eval_c_005fif_005f11 = _jspx_th_c_005fif_005f11.doStartTag();
          if (_jspx_eval_c_005fif_005f11 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t\t\t\t\tgetUserName: function() {\r\n");
              out.write("\t\t\t\t\t\t\treturn \"");
              out.print( UnicodeFormatter.toString(user.getFullName()) );
              out.write("\";\r\n");
              out.write("\t\t\t\t\t\t},\r\n");
              out.write("\t\t\t\t\t");
              int evalDoAfterBody = _jspx_th_c_005fif_005f11.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fif_005f11.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f11);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f11);
          out.write("\r\n");
          out.write("\t\r\n");
          out.write("\t\t\t\t\tgetDoAsUserIdEncoded: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( UnicodeFormatter.toString(themeDisplay.getDoAsUserId()) );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPlid: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPlid() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\r\n");
          out.write("\t\t\t\t\t");
          //  c:if
          org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f12 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
          _jspx_th_c_005fif_005f12.setPageContext(_jspx_page_context);
          _jspx_th_c_005fif_005f12.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f10);
          // /html/common/themes/top_js.jspf(126,5) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fif_005f12.setTest( layout != null );
          int _jspx_eval_c_005fif_005f12 = _jspx_th_c_005fif_005f12.doStartTag();
          if (_jspx_eval_c_005fif_005f12 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t\t\t\t\tgetLayoutId: function() {\r\n");
              out.write("\t\t\t\t\t\t\treturn \"");
              out.print( layout.getLayoutId() );
              out.write("\";\r\n");
              out.write("\t\t\t\t\t\t},\r\n");
              out.write("\t\t\t\t\t\tgetLayoutURL: function() {\r\n");
              out.write("\t\t\t\t\t\t\treturn \"");
              out.print( PortalUtil.getLayoutURL(layout, themeDisplay) );
              out.write("\";\r\n");
              out.write("\t\t\t\t\t\t},\r\n");
              out.write("\t\t\t\t\t\tisPrivateLayout: function() {\r\n");
              out.write("\t\t\t\t\t\t\treturn \"");
              out.print( layout.isPrivateLayout() );
              out.write("\";\r\n");
              out.write("\t\t\t\t\t\t},\r\n");
              out.write("\t\t\t\t\t\tgetParentLayoutId: function() {\r\n");
              out.write("\t\t\t\t\t\t\treturn \"");
              out.print( layout.getParentLayoutId() );
              out.write("\";\r\n");
              out.write("\t\t\t\t\t\t},\r\n");
              out.write("\t\t\t\t\t");
              int evalDoAfterBody = _jspx_th_c_005fif_005f12.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fif_005f12.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f12);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f12);
          out.write("\r\n");
          out.write("\t\r\n");
          out.write("\t\t\t\t\tgetScopeGroupId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getScopeGroupId() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetParentGroupId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getParentGroupId() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisImpersonated: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( themeDisplay.isImpersonated() );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisSignedIn: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( themeDisplay.isSignedIn() );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetDefaultLanguageId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( LocaleUtil.toLanguageId(LocaleUtil.getDefault()) );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetLanguageId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( LanguageUtil.getLanguageId(request) );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisFreeformLayout: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( themeDisplay.isFreeformLayout() );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisStateExclusive: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( themeDisplay.isStateExclusive() );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisStateMaximized: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( themeDisplay.isStateMaximized() );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tisStatePopUp: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( themeDisplay.isStatePopUp() );
          out.write(";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPathContext: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPathContext() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPathImage: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPathImage() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPathJavaScript: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPathJavaScript() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPathMain: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPathMain() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPathThemeImages: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPathThemeImages() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPathThemeRoot: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getPathThemeRoot() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetURLHome: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"");
          out.print( themeDisplay.getURLHome() );
          out.write("\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetSessionId: function() {\r\n");
          out.write("\t\t\t\t\t\treturn \"\";\r\n");
          out.write("\t\t\t\t\t},\r\n");
          out.write("\t\t\t\t\tgetPortletSetupShowBordersDefault: function() {\r\n");
          out.write("\t\t\t\t\t\treturn ");
          out.print( GetterUtil.getString(theme.getSetting("portlet-setup-show-borders-default"), "true") );
          out.write(";\r\n");
          out.write("\t\t\t\t\t}\r\n");
          out.write("\t\t\t\t},\r\n");
          out.write("\t\r\n");
          out.write("\t\t\t\tPropsValues: {\r\n");
          out.write("\t\t\t\t\tNTLM_AUTH_ENABLED: ");
          out.print( PropsValues.NTLM_AUTH_ENABLED );
          out.write("\r\n");
          out.write("\t\t\t\t}\r\n");
          out.write("\t\t\t};\r\n");
          out.write("\t\r\n");
          out.write("\t\t\tvar themeDisplay = Liferay.ThemeDisplay;\r\n");
          out.write("\t");
          int evalDoAfterBody = _jspx_th_c_005fif_005f10.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f10.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f10);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f10);
      out.write("\r\n");
      out.write("\t\t");

		long javaScriptLastModified = ServletContextUtil.getLastModified(application, "/html/js/", true);
		
      out.write("\r\n");
      out.write("\t\t\r\n");
      out.write("\t");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f13 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f13.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f13.setParent(null);
      // /html/common/themes/top_js.jspf(211,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f13.setTest( themeDisplay.isSignedIn() || themeDisplay.getScopeGroup().getName().equals(IterKeys.GUEST_GROUP_NAME) );
      int _jspx_eval_c_005fif_005f13 = _jspx_th_c_005fif_005f13.doStartTag();
      if (_jspx_eval_c_005fif_005f13 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t\t\t");

			String alloyBaseURL = themeDisplay.getPathJavaScript() + "/aui/";
			String alloyComboURL = PortalUtil.getStaticResourceURL(request, themeDisplay.getPathContext() + "/combo/", javaScriptLastModified);
			
          out.write("\r\n");
          out.write("\t\t\t\r\n");
          out.write("\t\t");
 // Tag inicio javascript
		if (!javaScriptInitialTagUsed){
			javaScriptInitialTagUsed = true;
			out.println(javaScriptInitialTag);			 	
		} 
          out.write("\r\n");
          out.write("\t\r\n");
          out.write("\t\t\tLiferay.AUI = {\r\n");
          out.write("\t\t\t\tgetBasePath: function() {\r\n");
          out.write("\t\t\t\t\treturn '");
          out.print( alloyBaseURL );
          out.write("';\r\n");
          out.write("\t\t\t\t},\r\n");
          out.write("\t\t\t\tgetCombine: function() {\r\n");
          out.write("\t\t\t\t\treturn ");
          out.print( themeDisplay.isThemeJsFastLoad() );
          out.write(";\r\n");
          out.write("\t\t\t\t},\r\n");
          out.write("\t\t\t\tgetComboPath: function() {\r\n");
          out.write("\t\t\t\t\treturn '");
          out.print( alloyComboURL );
          out.write('&');
          out.write('p');
          out.write('=');
          out.print( themeDisplay.getPathJavaScript() );
          out.write("&';\r\n");
          out.write("\t\t\t\t},\r\n");
          out.write("\t\t\t\tgetFilter: function() {\r\n");
          out.write("\t\t\t\t\t");
          //  c:choose
          org.apache.taglibs.standard.tag.common.core.ChooseTag _jspx_th_c_005fchoose_005f1 = (org.apache.taglibs.standard.tag.common.core.ChooseTag) _005fjspx_005ftagPool_005fc_005fchoose.get(org.apache.taglibs.standard.tag.common.core.ChooseTag.class);
          _jspx_th_c_005fchoose_005f1.setPageContext(_jspx_page_context);
          _jspx_th_c_005fchoose_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f13);
          int _jspx_eval_c_005fchoose_005f1 = _jspx_th_c_005fchoose_005f1.doStartTag();
          if (_jspx_eval_c_005fchoose_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t\t\t\t\t");
              //  c:when
              org.apache.taglibs.standard.tag.rt.core.WhenTag _jspx_th_c_005fwhen_005f1 = (org.apache.taglibs.standard.tag.rt.core.WhenTag) _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.WhenTag.class);
              _jspx_th_c_005fwhen_005f1.setPageContext(_jspx_page_context);
              _jspx_th_c_005fwhen_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f1);
              // /html/common/themes/top_js.jspf(235,6) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fwhen_005f1.setTest( themeDisplay.isThemeJsFastLoad() );
              int _jspx_eval_c_005fwhen_005f1 = _jspx_th_c_005fwhen_005f1.doStartTag();
              if (_jspx_eval_c_005fwhen_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                do {
                  out.write("\r\n");
                  out.write("\t\t\t\t\t\t\treturn {\r\n");
                  out.write("\t\t\t\t\t\t\t\treplaceStr: function(match, fragment, string) {\r\n");
                  out.write("\t\t\t\t\t\t\t\t\treturn fragment + 'm=' + (match.split('");
                  out.print( themeDisplay.getPathJavaScript() );
                  out.write("')[1] || '');\r\n");
                  out.write("\t\t\t\t\t\t\t\t},\r\n");
                  out.write("\t\t\t\t\t\t\t\tsearchExp: '(\\\\?|&)/([^&]+)'\r\n");
                  out.write("\t\t\t\t\t\t\t};\r\n");
                  out.write("\t\t\t\t\t\t");
                  int evalDoAfterBody = _jspx_th_c_005fwhen_005f1.doAfterBody();
                  if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                    break;
                } while (true);
              }
              if (_jspx_th_c_005fwhen_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f1);
                return;
              }
              _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f1);
              out.write("\r\n");
              out.write("\t\t\t\t\t\t");
              if (_jspx_meth_c_005fotherwise_005f1(_jspx_th_c_005fchoose_005f1, _jspx_page_context))
                return;
              out.write("\r\n");
              out.write("\t\t\t\t\t");
              int evalDoAfterBody = _jspx_th_c_005fchoose_005f1.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fchoose_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f1);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f1);
          out.write("\r\n");
          out.write("\t\t\t\t}\r\n");
          out.write("\t\t\t};\r\n");
          out.write("\t\r\n");
          out.write("\t\t\twindow.YUI_config = {\r\n");
          out.write("\t\t\t\tcomboBase: Liferay.AUI.getComboPath(),\r\n");
          out.write("\t\t\t\tfetchCSS: false,\r\n");
          out.write("\t\t\t\tfilter: Liferay.AUI.getFilter(),\r\n");
          out.write("\t\t\t\troot: Liferay.AUI.getBasePath()\r\n");
          out.write("\t\t\t};\r\n");
          out.write("\t");
          int evalDoAfterBody = _jspx_th_c_005fif_005f13.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f13.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f13);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f13);
      out.write("\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t");

		String currentURL = PortalUtil.getCurrentURL(request);
		
      out.write('\r');
      out.write('\n');
      out.write('	');
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f14 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f14.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f14.setParent(null);
      // /html/common/themes/top_js.jspf(261,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f14.setTest( themeDisplay.isSignedIn() || themeDisplay.getScopeGroup().getName().equals(IterKeys.GUEST_GROUP_NAME) );
      int _jspx_eval_c_005fif_005f14 = _jspx_th_c_005fif_005f14.doStartTag();
      if (_jspx_eval_c_005fif_005f14 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t\t\r\n");
          out.write("\t\t");
 // Tag inicio javascript
		if (!javaScriptInitialTagUsed){
			javaScriptInitialTagUsed = true;
			out.println(javaScriptInitialTag);			 	
		} 
          out.write("\t\r\n");
          out.write("\t\r\n");
          out.write("\t\tLiferay.currentURL = '");
          out.print( HtmlUtil.escapeJS(currentURL) );
          out.write("';\r\n");
          out.write("\t\tLiferay.currentURLEncoded = '");
          out.print( HttpUtil.encodeURL(currentURL) );
          out.write("';\r\n");
          out.write("\t");
          int evalDoAfterBody = _jspx_th_c_005fif_005f14.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f14.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f14);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f14);
      out.write("\t\r\n");
      out.write("\t\r\n");
 // Tag fin javascript 
if (javaScriptInitialTagUsed){ 
      out.write("\t\r\n");
      out.write("\t\t// ]]>\r\n");
      out.write("\t</script>\r\n");
 } 
      out.write("\r\n");
      out.write("\r\n");
      //  c:choose
      org.apache.taglibs.standard.tag.common.core.ChooseTag _jspx_th_c_005fchoose_005f2 = (org.apache.taglibs.standard.tag.common.core.ChooseTag) _005fjspx_005ftagPool_005fc_005fchoose.get(org.apache.taglibs.standard.tag.common.core.ChooseTag.class);
      _jspx_th_c_005fchoose_005f2.setPageContext(_jspx_page_context);
      _jspx_th_c_005fchoose_005f2.setParent(null);
      int _jspx_eval_c_005fchoose_005f2 = _jspx_th_c_005fchoose_005f2.doStartTag();
      if (_jspx_eval_c_005fchoose_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:when
          org.apache.taglibs.standard.tag.rt.core.WhenTag _jspx_th_c_005fwhen_005f2 = (org.apache.taglibs.standard.tag.rt.core.WhenTag) _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.WhenTag.class);
          _jspx_th_c_005fwhen_005f2.setPageContext(_jspx_page_context);
          _jspx_th_c_005fwhen_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f2);
          // /html/common/themes/top_js.jspf(280,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fwhen_005f2.setTest( themeDisplay.isThemeJsFastLoad() );
          int _jspx_eval_c_005fwhen_005f2 = _jspx_th_c_005fwhen_005f2.doStartTag();
          if (_jspx_eval_c_005fwhen_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t");
              //  c:choose
              org.apache.taglibs.standard.tag.common.core.ChooseTag _jspx_th_c_005fchoose_005f3 = (org.apache.taglibs.standard.tag.common.core.ChooseTag) _005fjspx_005ftagPool_005fc_005fchoose.get(org.apache.taglibs.standard.tag.common.core.ChooseTag.class);
              _jspx_th_c_005fchoose_005f3.setPageContext(_jspx_page_context);
              _jspx_th_c_005fchoose_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fwhen_005f2);
              int _jspx_eval_c_005fchoose_005f3 = _jspx_th_c_005fchoose_005f3.doStartTag();
              if (_jspx_eval_c_005fchoose_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                do {
                  out.write("\r\n");
                  out.write("\t\t\t");
                  //  c:when
                  org.apache.taglibs.standard.tag.rt.core.WhenTag _jspx_th_c_005fwhen_005f3 = (org.apache.taglibs.standard.tag.rt.core.WhenTag) _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.WhenTag.class);
                  _jspx_th_c_005fwhen_005f3.setPageContext(_jspx_page_context);
                  _jspx_th_c_005fwhen_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f3);
                  // /html/common/themes/top_js.jspf(282,3) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                  _jspx_th_c_005fwhen_005f3.setTest( !themeDisplay.isThemeJsBarebone()  || themeDisplay.getScopeGroup().getName().equals(IterKeys.GUEST_GROUP_NAME));
                  int _jspx_eval_c_005fwhen_005f3 = _jspx_th_c_005fwhen_005f3.doStartTag();
                  if (_jspx_eval_c_005fwhen_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                    do {
                      out.write("\r\n");
                      out.write("\t\t\t\t<script src=\"");
                      out.print( HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getPathJavaScript() + "/everything.jsp", "minifierBundleId=" + HttpUtil.encodeURL("javascript.everything.files"), javaScriptLastModified)) );
                      out.write("\" type=\"text/javascript\"></script>\r\n");
                      out.write("\t\t\t");
                      int evalDoAfterBody = _jspx_th_c_005fwhen_005f3.doAfterBody();
                      if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                        break;
                    } while (true);
                  }
                  if (_jspx_th_c_005fwhen_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                    _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f3);
                    return;
                  }
                  _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f3);
                  out.write("\r\n");
                  out.write("\t\t");
                  int evalDoAfterBody = _jspx_th_c_005fchoose_005f3.doAfterBody();
                  if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                    break;
                } while (true);
              }
              if (_jspx_th_c_005fchoose_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f3);
                return;
              }
              _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f3);
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fwhen_005f2.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fwhen_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f2);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f2);
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:otherwise
          org.apache.taglibs.standard.tag.common.core.OtherwiseTag _jspx_th_c_005fotherwise_005f2 = (org.apache.taglibs.standard.tag.common.core.OtherwiseTag) _005fjspx_005ftagPool_005fc_005fotherwise.get(org.apache.taglibs.standard.tag.common.core.OtherwiseTag.class);
          _jspx_th_c_005fotherwise_005f2.setPageContext(_jspx_page_context);
          _jspx_th_c_005fotherwise_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f2);
          int _jspx_eval_c_005fotherwise_005f2 = _jspx_th_c_005fotherwise_005f2.doStartTag();
          if (_jspx_eval_c_005fotherwise_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\r\n");
              out.write("\t\t");

		if (!themeDisplay.isThemeJsBarebone() || themeDisplay.getScopeGroup().getName().equals(IterKeys.GUEST_GROUP_NAME)) 
		{
			String[] javaScriptFiles = JavaScriptBundleUtil.getFileNames(PropsKeys.JAVASCRIPT_EVERYTHING_FILES);
			
			for (String javaScriptFile : javaScriptFiles) 
			{
			
              out.write("\r\n");
              out.write("\t\t\t\t<script src=\"");
              out.print( themeDisplay.getPathJavaScript() );
              out.write('/');
              out.print( javaScriptFile );
              out.write('?');
              out.write('t');
              out.write('=');
              out.print( javaScriptLastModified );
              out.write("\" type=\"text/javascript\"></script>\r\n");
              out.write("\t\t\t");

			}
			
		}
		
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fotherwise_005f2.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fotherwise_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f2);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f2);
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fchoose_005f2.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fchoose_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f2);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f2);
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f15 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f15.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f15.setParent(null);
      // /html/common/themes/top_js.jspf(306,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f15.setTest( PropsValues.JAVASCRIPT_LOG_ENABLED );
      int _jspx_eval_c_005fif_005f15 = _jspx_th_c_005fif_005f15.doStartTag();
      if (_jspx_eval_c_005fif_005f15 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t<script src=\"");
          out.print( themeDisplay.getPathJavaScript() );
          out.write("/firebug/firebug.js\" type=\"text/javascript\"></script>\r\n");
          int evalDoAfterBody = _jspx_th_c_005fif_005f15.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f15.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f15);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f15);
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f16 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f16.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f16.setParent(null);
      // /html/common/themes/top_js.jspf(310,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f16.setTest( themeDisplay.isIncludeServiceJs() );
      int _jspx_eval_c_005fif_005f16 = _jspx_th_c_005fif_005f16.doStartTag();
      if (_jspx_eval_c_005fif_005f16 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\t<script src=\"");
          out.print( HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getPathJavaScript() + "/liferay/service.js", javaScriptLastModified)) );
          out.write("\" type=\"text/javascript\"></script>\r\n");
          int evalDoAfterBody = _jspx_th_c_005fif_005f16.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f16.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f16);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f16);
      out.write('\r');
      out.write('\n');
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f17 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f17.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f17.setParent(null);
      // /html/common/themes/top_head.jsp(216,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f17.setTest( themeDisplay.isSignedIn() || !isNewsletterPage);
      int _jspx_eval_c_005fif_005f17 = _jspx_th_c_005fif_005f17.doStartTag();
      if (_jspx_eval_c_005fif_005f17 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\r\n");
          out.write("\t");
          out.write("\r\n");
          out.write("\r\n");
          out.write("\t<script type='");
          out.print(ContentTypes.TEXT_JAVASCRIPT);
          out.write("' src='");
          out.print(versionRsrc);
          out.write("' ></script>\r\n");
          out.write("\r\n");
          out.write("\t<script>\r\n");
          out.write("\t\tjQryIter.u = \"");
          out.print(PHPUtil.isApacheRequest(IterRequest.getOriginalRequest()) ? "<?php echo getenv('ITER_USER_ID');?>" : "");
          out.write("\";\r\n");
          out.write("\t</script>\r\n");
          out.write("\r\n");
          out.write("\t");
          out.write("\r\n");
          out.write("\r\n");
          out.write("\r\n");
          out.write("\r\n");
          out.write("\r\n");
          out.write("<script type=\"text/javascript\">\r\n");
          out.write("     (function($)\r\n");
          out.write("     {\r\n");
          out.write("         $.contextSections = function ()\r\n");
          out.write("         {\r\n");
          out.write("            return ");
          out.print( JQryIterExtensionTools.getContextSections() );
          out.write(";\r\n");
          out.write("         };\r\n");
          out.write("\r\n");
          out.write("         $.contextIsArticlePage = function ()\r\n");
          out.write("         {\r\n");
          out.write("            return ");
          out.print( IterRequest.isDetailRequest() );
          out.write(";\r\n");
          out.write("         };\r\n");
          out.write("         \r\n");
          out.write("         $.articleId = function ()\r\n");
          out.write("         {\r\n");
          out.write("             return \"");
          out.print( IterRequest.getOriginalRequest().getAttribute("ARTICLEURL_ARTICLEID") != null ? IterRequest.getOriginalRequest().getAttribute("ARTICLEURL_ARTICLEID") : StringPool.BLANK );
          out.write("\";\r\n");
          out.write("         };\r\n");
          out.write("         \r\n");
          out.write("         $.contextIs = function (contextType)\r\n");
          out.write("         {\r\n");
          out.write("\t\t\tif (contextType== '");
          out.print( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_HOMEPAGE );
          out.write("' )\r\n");
          out.write("\t\t\t\treturn ");
          out.print( IterVelocityTools.contextIs( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_HOMEPAGE) );
          out.write(";\r\n");
          out.write("\t\t\telse if( contextType== '");
          out.print( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_ARTICLEPAGE );
          out.write("')\r\n");
          out.write(" \t        \treturn ");
          out.print( IterVelocityTools.contextIs( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_ARTICLEPAGE) );
          out.write(";\r\n");
          out.write(" \t        else if( contextType== '");
          out.print(com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_SEARCHPAGE );
          out.write("')\r\n");
          out.write(" \t \t       \treturn ");
          out.print( IterVelocityTools.contextIs( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_SEARCHPAGE) );
          out.write(";\r\n");
          out.write(" \t \t    else if( contextType== '");
          out.print(com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_SECTIONPAGE );
          out.write("')\r\n");
          out.write(" \t \t \t    return ");
          out.print( IterVelocityTools.contextIs( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_SECTIONPAGE) );
          out.write(";\r\n");
          out.write(" \t \t \telse if( contextType== '");
          out.print( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_METADATAPAGE );
          out.write("')\r\n");
          out.write(" \t \t \t \treturn ");
          out.print( IterVelocityTools.contextIs( com.liferay.portal.kernel.util.WebKeys.CONTEXTTYPE_METADATAPAGE) );
          out.write(";     \r\n");
          out.write("         };\r\n");
          out.write("         \r\n");
          out.write("\r\n");
          out.write("     })( jQryIter );\r\n");
          out.write(" </script>\r\n");
          out.write(" \r\n");
          out.write("     \r\n");
          out.write(" ");
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f17.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f17.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f17);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f17);
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f18 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f18.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f18.setParent(null);
      // /html/common/themes/top_head.jsp(229,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f18.setTest( IterVelocityTools.canShowDockbar());
      int _jspx_eval_c_005fif_005f18 = _jspx_th_c_005fif_005f18.doStartTag();
      if (_jspx_eval_c_005fif_005f18 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          out.write('\r');
          out.write('\n');
          out.write('	');
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

Layout docklayout = themeDisplay.getLayout();

Boolean isCatalog = false;
String catalogtype = CatalogUtil.getCatalogType(docklayout.getPlid());
isCatalog = catalogtype != null ?  true : false;

//Si es un catálogo se podría estar simulando un PLID. La aplicación Angular trabaja cn UUID
String simulatedSectionUUID	= "";
String simulatedSectionName	= "";
String simulatedPlid = CookieUtil.get(request, WebKeys.COOKIE_ITR_CURRENT_SECTION_PLID);
if (Validator.isNotNull(simulatedPlid))
{
	Layout simulatedLayout 	= LayoutLocalServiceUtil.getLayout(Long.parseLong(simulatedPlid));
	simulatedSectionUUID 	= simulatedLayout.getUuid();
	simulatedSectionName 	= simulatedLayout.getName(LocaleUtil.getDefault());
}

String urlControlPanel 	= themeDisplay.getURLControlPanel();
String urlSignOut 		= themeDisplay.getURLSignOut();
String isControlPanel 	= String.valueOf(docklayout.getGroup().isControlPanel());


//se obtiene el layout template
String layoutTemplateId = ((LayoutTypePortlet)docklayout.getLayoutType()).getLayoutTemplateId();

URL urlHost 			= new URL(themeDisplay.getURLPortal());
boolean secure 			= request.isSecure();

// Comprueba el header X-Forwarded-Proto
String protocolFromHeader = request.getHeader("X-Forwarded-Proto");
// Será seguro si se indica https en el header. Si no, se usará el valor del request.
secure = secure || Http.HTTPS.equalsIgnoreCase(protocolFromHeader);

String urlPortal = new URL(HttpUtil.getProtocol(secure), urlHost.getHost(), urlHost.getPort(), urlHost.getFile()).toString();

StringBuilder sbParams = new StringBuilder();
sbParams.append("&urlControlPanel=")		.append(urlControlPanel);
sbParams.append("&urlSignOut=")				.append(urlSignOut);
sbParams.append("&isControlPanel=")			.append(isControlPanel);
sbParams.append("&isCatalog=")				.append(isCatalog.toString());
sbParams.append("&urlPortal=")				.append(urlPortal);
sbParams.append("&scopeGroupId=")			.append(themeDisplay.getScopeGroupId());
sbParams.append("&layoutTemplateId=")		.append(layoutTemplateId);
sbParams.append("&plid=")					.append(docklayout.getPlid());
sbParams.append("&simulatedSectionUUID=")	.append(simulatedSectionUUID);
sbParams.append("&simulatedSectionName=")	.append(simulatedSectionName);

String flashvars = sbParams.toString();
String url = Validator.isNotNull(PropsValues.ITER_NGPORLET_DEVELOP_URL) ? PropsValues.ITER_NGPORLET_DEVELOP_URL : String.format("/ngportlets/index.html?v=%s", IterGlobal.getIterWebCmsVersion());	

// http://jira.protecmedia.com:8080/browse/ITER-1281?focusedCommentId=56525&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-56525
// Se especifican inline y con !important los estilos de los ngPortlets para que NO puedan ser sobreescritos por personalizaciones del cliente
// Se le aplican a:
// - DIV que contiene al iframe: ngPortlets_iframe_container
//	 El alto no puede ser important porque de lo contrario no se podría sobreescribir, y el DIV se contrae y expande cada vez que se muestra una configuración y se cierra
//   Funciona en Chrome pero en IE no.
// - iframe: #ngPortlets_iframe_container iframe
// - body

          out.write("\r\n");
          out.write("<!-- ngPortlets -->\r\n");
          out.write("<!-- CSS -->\r\n");
          out.write("<!-- La barra de angular ocupa 36px, se le dejan dos mÃ¡s al body para dejar mÃ¡s -->\t\r\n");
          out.write("<style>\r\n");
          out.write("  .aui-widget.aui-component.aui-panel {\r\n");
          out.write("      top:36px;\r\n");
          out.write("  }\r\n");
          out.write("  .minimizedFrame {\r\n");
          out.write("\t   height: 36px !important;\r\n");
          out.write("  }\r\n");
          out.write("  .maximizedFrame {\r\n");
          out.write("\t   height: 100% !important;\r\n");
          out.write("  }\r\n");
          out.write("  \r\n");
          out.write("</style>\r\n");
          out.write("<!-- Finaliza CSS -->\r\n");
          out.write("\r\n");
          out.write("<!-- JS -->\r\n");
          out.write("<script >\r\n");
          out.write("var ngPortletsData = {\r\n");
          out.write("  iframeUrl: '");
          out.print(url);
          out.write("',\r\n");
          out.write("\tmoduleName: 'ngPortlets',\r\n");
          out.write("\tiframeContainerId: '#ngPortlets_iframe_container',\r\n");
          out.write("//  iframeWin: document.querySelectorAll(\"#ngPortlets_iframe_container > iframe:first-child\")[0].contentWindow,\r\n");
          out.write("\tiframeMinimized: 'minimizedFrame',\r\n");
          out.write("\tiframeMaximized: 'maximizedFrame',\r\n");
          out.write("\tiframeFullHeight: '100%',\r\n");
          out.write("\tnavbarHeight: '36px',\r\n");
          out.write("  dockbarInitParams: '");
          out.print(flashvars);
          out.write("',\r\n");
          out.write("  addWindowOpen: null,\r\n");
          out.write("  htmlFunctionsNames: {\r\n");
          out.write("    showApplicationsDialog: 'showApplicationsDialog'\r\n");
          out.write("  },\r\n");
          out.write("\teventSource: null, \r\n");
          out.write("\teventOrigin: null, \r\n");
          out.write("\t// ComunicaciÃÂÃÂ³n hacia Angular (ngPortlets)\r\n");
          out.write("\tcallNgPortlets: function(ngRoute, data, allowFullScreen){\r\n");
          out.write("\t\tif (typeof data === 'undefined'){ data = ''; }\r\n");
          out.write("\t\tif (typeof allowFullScreen === 'undefined'){ allowFullScreen = true; }\r\n");
          out.write("\t\tthis.allowFullSizeToNgPortlets(allowFullScreen);\r\n");
          out.write("\t\tconsole.log('Data a ser enviada desde HTML a ngPortlets: ');\r\n");
          out.write("\t\tconsole.log(data);\r\n");
          out.write("\t\tvar dataToSend = this.getDataToSendToNgPortlets(ngRoute, data);\r\n");
          out.write("\t\r\n");
          out.write("\t\tthis.eventSource.postMessage(JSON.stringify(dataToSend), this.eventOrigin); \r\n");
          out.write("\t},\r\n");
          out.write("\t// ComunicaciÃÂÃÂ³n desde Angular (ngPortlets)\r\n");
          out.write("\taddEventListener: function(){\r\n");
          out.write("\t\tif (window.addEventListener) {\r\n");
          out.write("\t\t\twindow.addEventListener(\"message\", this.receiveNgPortletsMessage, false);\r\n");
          out.write("\t\t}\r\n");
          out.write("\t\telse {\r\n");
          out.write("\t\t\tconsole.log('Este navegador no permite la comunicaciÃÂÃÂ³n desde/hacia iframes');\r\n");
          out.write("\t\t}\r\n");
          out.write("\t},\r\n");
          out.write("\treceiveNgPortletsMessage: function(event){\r\n");
          out.write("\t\tconsole.log('El HTML ha recibido un mensaje proveniente del siguiente origen: ' + event.origin);\r\n");
          out.write("\t\tvar data = {};\r\n");
          out.write("\t\ttry {\r\n");
          out.write("\t\t\tdata = JSON.parse(event.data);\r\n");
          out.write("\t\t\tif (data.module !== ngPortletsData.moduleName) {\r\n");
          out.write("\t\t\t\tconsole.error('El mensaje recibido no proviene de la aplicaciÃÂÃÂ³n Angular (ngPortlets)');\r\n");
          out.write("\t\t\t}else {\r\n");
          out.write("\t\t\t\tif (data.action === '') {\r\n");
          out.write("\t\t\t\t\tconsole.log(\"Se asigna valor a eventSource y eventOrigin\"); \r\n");
          out.write("\t\t\t\t\tngPortletsData.eventSource = event.source; \r\n");
          out.write("        \tngPortletsData.eventOrigin = event.origin; \r\n");
          out.write("\t\t\t\t\tconsole.log(\"Como data.action viene vacÃÂÃÂ­o, se asume que ngPortlets estÃÂÃÂ¡ informando que ya se encuentra disponible. Se envÃÂÃÂ­a mensaje de inicializaciÃÂÃÂ³n\");\r\n");
          out.write("\t\t\t\t\tngPortletsData.callNgPortlets('', ngPortletsData.dockbarInitParams, false);\r\n");
          out.write("\t\t\t\t}else {\r\n");
          out.write("\t\t\t\t\tif (typeof window[data.action] === \"function\") {\r\n");
          out.write("\t\t\t\t\t\tconsole.log(\"Scope: window\");\r\n");
          out.write("\t\t\t\t\t\tif (ngPortletsData.shouldFunctionBeExecuted(data.action)) {\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log('Se ejecuta la funciÃÂÃÂ³n \"'+data.action+'\"');\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log('Con los siguientes parÃÂÃÂ¡metros:');\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log(data.data);\r\n");
          out.write("\t\t\t\t\t\t\tngPortletsData.executeFunctionByName(data.action, window, data.data);\r\n");
          out.write("\t\t\t\t\t\t\tngPortletsData.postExecutionTreatment(data.action);\r\n");
          out.write("\t\t\t\t\t\t}else {\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log(\"La funciÃÂÃÂ³n (\"+data.action+\") no debe ser ejecutada\");\r\n");
          out.write("\t\t\t\t\t\t}\r\n");
          out.write("\t\t\t\t\t}else {\r\n");
          out.write("\t\t\t\t\t\tif (typeof ngPortletsData[data.action] === \"function\") {\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log(\"Scope: ngPortletsData\");\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log('Se ejecuta la funciÃÂÃÂ³n \"'+data.action+'\"');\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log('Con los siguientes parÃÂÃÂ¡metros:');\r\n");
          out.write("\t\t\t\t\t\t\tconsole.log(data.data);\r\n");
          out.write("\t\t\t\t\t\t\tngPortletsData.executeFunctionByName(data.action, ngPortletsData, data.data);\r\n");
          out.write("\t\t\t\t\t\t}else {\r\n");
          out.write("\t\t\t\t\t\t\tconsole.error('La funciÃÂÃÂ³n \"'+data.action+'\" no estÃÂÃÂ¡ definida');\r\n");
          out.write("\t\t\t\t\t\t}\r\n");
          out.write("\t\t\t\t\t}\r\n");
          out.write("\t\t\t\t}\r\n");
          out.write("\t\t\t}\r\n");
          out.write("\t\t} catch (e) {\r\n");
          out.write("\t\t\tconsole.error('Por el siguiente error, no se pudo procesar el mensaje recibido: '+e.message);\r\n");
          out.write("\t\t}\r\n");
          out.write("\t},\r\n");
          out.write("\tshouldFunctionBeExecuted: function(functionName){\r\n");
          out.write("\t\ttoReturn = true;\r\n");
          out.write("\t\tswitch (functionName) {\r\n");
          out.write("\t\t\tcase ngPortletsData.htmlFunctionsNames.showApplicationsDialog:\r\n");
          out.write("\t\t\t\tif (ngPortletsData.addWindowOpen === true) {\r\n");
          out.write("\t\t\t\t\ttoReturn = false;\r\n");
          out.write("\t\t\t\t}\r\n");
          out.write("\t\t\tbreak;\r\n");
          out.write("\t\t}\r\n");
          out.write("\t\treturn toReturn;\r\n");
          out.write("\t},\r\n");
          out.write("\texecuteFunctionByName: function(functionName, context, args){\r\n");
          out.write("\t\tvar args = Array.prototype.slice.call(arguments, 2);\r\n");
          out.write("\t\tvar namespaces = functionName.split(\".\");\r\n");
          out.write("\t\tvar func = namespaces.pop();\r\n");
          out.write("\t\tfor (var i = 0; i < namespaces.length; i++) {\r\n");
          out.write("\t\t\tcontext = context[namespaces[i]];\r\n");
          out.write("\t\t}\r\n");
          out.write("\t\t//console.log(args);\r\n");
          out.write("\t\targs = args[0];\r\n");
          out.write("\t\treturn context[func].apply(context, args);\r\n");
          out.write("\t},\r\n");
          out.write("\tpostExecutionTreatment: function(functionName){\r\n");
          out.write("\t\tswitch (functionName) {\r\n");
          out.write("\t\t\tcase ngPortletsData.htmlFunctionsNames.showApplicationsDialog:\r\n");
          out.write("\t\t\t\tif (ngPortletsData.addWindowOpen === null) {\r\n");
          out.write("\t\t\t\t\tngPortletsData.bindToAddCloseButton();\r\n");
          out.write("\t\t\t\t}\r\n");
          out.write("\t\t\t\tngPortletsData.addWindowOpen = true;\r\n");
          out.write("\t\t\tbreak;\r\n");
          out.write("\t\t}\r\n");
          out.write("\t},\r\n");
          out.write("\t// RedimensiÃÂÃÂ³n del iframe\r\n");
          out.write("\tallowFullSizeToNgPortlets: function(allow){\r\n");
          out.write("\t\tvar height = allow ? this.iframeFullHeight : this.navbarHeight;\r\n");
          out.write("\t  \tjQryIter(this.iframeContainerId).css('height', height);\r\n");
          out.write("\r\n");
          out.write("\t\tif (allow) {\r\n");
          out.write("\t\t\tjQryIter(this.iframeContainerId).removeClass(this.iframeMinimized);\r\n");
          out.write("\t\t\tjQryIter(this.iframeContainerId).addClass(\t this.iframeMaximized);\r\n");
          out.write("\t\t}\r\n");
          out.write("\t\telse {\r\n");
          out.write("\t\t\tjQryIter(this.iframeContainerId).removeClass(this.iframeMaximized);\r\n");
          out.write("\t\t\tjQryIter(this.iframeContainerId).addClass(\t this.iframeMinimized);\r\n");
          out.write("\t\t}\r\n");
          out.write("\t},\r\n");
          out.write("\t// Al hacer clic en \"Salir\" (cerrar sesiÃÂÃÂ³n / logout)\r\n");
          out.write("\tngPortletsLogout: function(logoutUrl){\r\n");
          out.write("\t\twindow.location.replace(logoutUrl);\r\n");
          out.write("\t},\r\n");
          out.write("\t// Recargar pÃÂÃÂ¡gina\r\n");
          out.write("\tngPortletsReloadPage: function(){\r\n");
          out.write("\t\tlocation.reload();\r\n");
          out.write("\t},\r\n");
          out.write("\t\r\n");
          out.write("\t// Carga una cookie con el PLID de la secciÃ³n que se quiere simular\r\n");
          out.write("\tngSimulateSection: function(plid){\r\n");
          out.write("\t\tjQryIter.cookie(\"ITR_CURRENT_SECTION_PLID\",plid.toString(),{domain: jQryIter.getDomain()});\r\n");
          out.write("\t\tlocation.reload();\r\n");
          out.write("\t},\r\n");
          out.write("\t\r\n");
          out.write("\tbindToAddCloseButton: function(){\r\n");
          out.write("    jQryIter(document).on(\"click\", \"#closethick\", function(){\r\n");
          out.write("\t\t\tngPortletsData.addWindowOpen = false;\r\n");
          out.write("\t\t});\r\n");
          out.write("\t},\r\n");
          out.write("\t/*\r\n");
          out.write("\t * 0 - Add event listener\r\n");
          out.write("\t * 1 - Mostrar controles de ediciÃÂÃÂ³n\r\n");
          out.write("\t * 2 - Asignar src al iframe\r\n");
          out.write("\t * 3 - Establecer comunicaciÃÂÃÂ³n inicial con Angular (ngPortlets)\r\n");
          out.write("\t */\r\n");
          out.write("\tinitNgPortlets: function(){\r\n");
          out.write("\t\t//var self = this;\r\n");
          out.write("\t\tthis.addEventListener();\r\n");
          out.write("\t\tshowControls(true);\r\n");
          out.write("    jQryIter(this.iframeContainerId+' iframe').attr('src', this.iframeUrl);\r\n");
          out.write("\r\n");
          out.write("\t},\r\n");
          out.write("\t// FunciÃÂÃÂ³n que retorna los datos a ser enviados a Angular (ngPortlets)\r\n");
          out.write("\tgetDataToSendToNgPortlets: function(action, data){\r\n");
          out.write("\t\tvar toReturn = {\r\n");
          out.write("\t\t\tmodule: this.moduleName,\r\n");
          out.write("\t\t\taction: action,\r\n");
          out.write("\t\t\tdata: data\r\n");
          out.write("\t\t};\r\n");
          out.write("\t\treturn toReturn;\r\n");
          out.write("\t}\r\n");
          out.write("};\r\n");
          out.write("jQryIter(function(){\r\n");
          out.write("\tngPortletsData.initNgPortlets();\r\n");
          out.write("});\r\n");
          out.write("</script >\r\n");
          out.write("<!-- Finaliza JS -->\r\n");
          out.write("<!-- Finaliza ngPortlets -->");
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f18.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f18.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f18);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f18);
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f19 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f19.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f19.setParent(null);
      // /html/common/themes/top_head.jsp(234,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f19.setTest( themeDatabaseEnabled && !layout.isTypeControlPanel() && !isNewsletterPage);
      int _jspx_eval_c_005fif_005f19 = _jspx_th_c_005fif_005f19.doStartTag();
      if (_jspx_eval_c_005fif_005f19 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          out.write('\r');
          out.write('\n');
          out.write('	');
          out.print(ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, ContentTypes.TEXT_JAVASCRIPT));
          out.write('	');
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f19.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f19.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f19);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f19);
      out.write("\r\n");
      out.write("\r\n");
      //  c:choose
      org.apache.taglibs.standard.tag.common.core.ChooseTag _jspx_th_c_005fchoose_005f4 = (org.apache.taglibs.standard.tag.common.core.ChooseTag) _005fjspx_005ftagPool_005fc_005fchoose.get(org.apache.taglibs.standard.tag.common.core.ChooseTag.class);
      _jspx_th_c_005fchoose_005f4.setPageContext(_jspx_page_context);
      _jspx_th_c_005fchoose_005f4.setParent(null);
      int _jspx_eval_c_005fchoose_005f4 = _jspx_th_c_005fchoose_005f4.doStartTag();
      if (_jspx_eval_c_005fchoose_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:when
          org.apache.taglibs.standard.tag.rt.core.WhenTag _jspx_th_c_005fwhen_005f4 = (org.apache.taglibs.standard.tag.rt.core.WhenTag) _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.WhenTag.class);
          _jspx_th_c_005fwhen_005f4.setPageContext(_jspx_page_context);
          _jspx_th_c_005fwhen_005f4.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f4);
          // /html/common/themes/top_head.jsp(240,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fwhen_005f4.setTest( !usePortletsOwnResources);
          int _jspx_eval_c_005fwhen_005f4 = _jspx_th_c_005fwhen_005f4.doStartTag();
          if (_jspx_eval_c_005fwhen_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t\r\n");
              out.write("\t\t<!-- Iter Portlet Header Javascripts -->\r\n");
              out.write("\t");
              int evalDoAfterBody = _jspx_th_c_005fwhen_005f4.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fwhen_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f4);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f4);
          out.write('\r');
          out.write('\n');
          out.write('	');
          //  c:otherwise
          org.apache.taglibs.standard.tag.common.core.OtherwiseTag _jspx_th_c_005fotherwise_005f3 = (org.apache.taglibs.standard.tag.common.core.OtherwiseTag) _005fjspx_005ftagPool_005fc_005fotherwise.get(org.apache.taglibs.standard.tag.common.core.OtherwiseTag.class);
          _jspx_th_c_005fotherwise_005f3.setPageContext(_jspx_page_context);
          _jspx_th_c_005fotherwise_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f4);
          int _jspx_eval_c_005fotherwise_005f3 = _jspx_th_c_005fotherwise_005f3.doStartTag();
          if (_jspx_eval_c_005fotherwise_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\r\n");
              out.write("\t\t");
              //  c:if
              org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f20 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
              _jspx_th_c_005fif_005f20.setPageContext(_jspx_page_context);
              _jspx_th_c_005fif_005f20.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fotherwise_005f3);
              // /html/common/themes/top_head.jsp(245,2) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fif_005f20.setTest( portlets != null && !isNewsletterPage );
              int _jspx_eval_c_005fif_005f20 = _jspx_th_c_005fif_005f20.doStartTag();
              if (_jspx_eval_c_005fif_005f20 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                do {
                  out.write('\r');
                  out.write('\n');

			Set<String> headerPortalJavaScriptSet = new LinkedHashSet<String>();
		
			for (Portlet portlet : portlets) {
				for (String headerPortalJavaScript : portlet.getHeaderPortalJavaScript()) {
					if (!HttpUtil.hasProtocol(headerPortalJavaScript))
					{
						headerPortalJavaScript = PortalUtil.getStaticResourceURL(request, request.getContextPath() + headerPortalJavaScript, portlet.getTimestamp());
					}
		
					if (!headerPortalJavaScriptSet.contains(headerPortalJavaScript) && !themeDisplay.isIncludedJs(headerPortalJavaScript))
					{
						headerPortalJavaScriptSet.add(headerPortalJavaScript);

                  out.write(" \r\n");
                  out.write("\t\t\t\t\t\t<script src=\"");
                  out.print( HtmlUtil.escape(headerPortalJavaScript) );
                  out.write("\" type=\"text/javascript\"></script>\r\n");

					}
				}
			}
		
			Set<String> headerPortletJavaScriptSet = new LinkedHashSet<String>();
		
			for (Portlet portlet : portlets) {
				for (String headerPortletJavaScript : portlet.getHeaderPortletJavaScript()) {
					if (!HttpUtil.hasProtocol(headerPortletJavaScript))
					{
						headerPortletJavaScript = PortalUtil.getStaticResourceURL(request, portlet.getContextPath() + headerPortletJavaScript, portlet.getTimestamp());
					}
		
					if (!headerPortletJavaScriptSet.contains(headerPortletJavaScript))
					{
						headerPortletJavaScriptSet.add(headerPortletJavaScript);

                  out.write(" \r\n");
                  out.write("\t\t\t\t\t\t<script src=\"");
                  out.print( HtmlUtil.escape(headerPortletJavaScript) );
                  out.write("\" type=\"text/javascript\"></script>\r\n");

					}
				}
			}

                  out.write(" \r\n");
                  out.write("\t\t");
                  int evalDoAfterBody = _jspx_th_c_005fif_005f20.doAfterBody();
                  if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                    break;
                } while (true);
              }
              if (_jspx_th_c_005fif_005f20.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f20);
                return;
              }
              _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f20);
              out.write('\r');
              out.write('\n');
              out.write('	');
              int evalDoAfterBody = _jspx_th_c_005fotherwise_005f3.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fotherwise_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f3);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f3);
          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fchoose_005f4.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fchoose_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f4);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f4);
      out.write("\r\n");
      out.write("\r\n");

	//Javascripts Teaser-Viewer Portlet
	
	if ( !isNewsletterPage )
	{
		//Javascripts Disqus
		Object commentsConfigBeanObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN);
		if(commentsConfigBeanObject != null )
		{
			CommentsConfigBean commentsConfig = (CommentsConfigBean)commentsConfigBeanObject;
			out.print(commentsConfig.getJavascriptDisqusInitCode());
			out.print(commentsConfig.getPHPDisqusHMACSHA1Code());
		}
	}

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

	List<String> markupHeaders = (List<String>)request.getAttribute(MimeResponse.MARKUP_HEAD_ELEMENT);
	if (markupHeaders != null) {
		for (String markupHeader : markupHeaders)
		{

      out.write("\r\n");
      out.write("\t\t\t");
      out.print( markupHeader );
      out.write('\r');
      out.write('\n');

		}
	}

	StringBundler pageTopSB = (StringBundler)request.getAttribute(WebKeys.PAGE_TOP);

      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f21 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f21.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f21.setParent(null);
      // /html/common/themes/top_head.jsp(321,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f21.setTest( pageTopSB != null );
      int _jspx_eval_c_005fif_005f21 = _jspx_th_c_005fif_005f21.doStartTag();
      if (_jspx_eval_c_005fif_005f21 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\r');
          out.write('\n');

	pageTopSB.writeTo(out);

          out.write('\r');
          out.write('\n');
          int evalDoAfterBody = _jspx_th_c_005fif_005f21.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f21.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f21);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f21);
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      //  c:if
      org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f22 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
      _jspx_th_c_005fif_005f22.setPageContext(_jspx_page_context);
      _jspx_th_c_005fif_005f22.setParent(null);
      // /html/common/themes/top_head.jsp(328,0) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_c_005fif_005f22.setTest( portlets != null && !layout.isTypeControlPanel() );
      int _jspx_eval_c_005fif_005f22 = _jspx_th_c_005fif_005f22.doStartTag();
      if (_jspx_eval_c_005fif_005f22 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\r\n");
          out.write("\r\n");

	// Se obtiene la fecha que está en la URL
	String dateToken = "/date/";
	int dateIndex 	 = -1;
	if ( (dateIndex = themeDisplay.getURLCurrent().indexOf(dateToken)) > 0 )
	{
		String[] postDateParams = themeDisplay.getURLCurrent().substring(dateIndex+dateToken.length()).split("/");
		original_request.setAttribute("urlDate", postDateParams[0]);
	}
	
	// Se obtiene el metadato que está en la URL
	String metaToken = "/meta/";
	int metaIndex 	 = -1;
	if ( (dateIndex = themeDisplay.getURLCurrent().indexOf(metaToken)) > 0 )
	{
		String[] postMetaParams = themeDisplay.getURLCurrent().substring(metaIndex+metaToken.length()).split("/");
		original_request.setAttribute("urlMeta", postMetaParams[0]);
	}


	//Se añade el SKIN, 
	//Se añade el javascript necesario para pintar la publicidad 
	//Se calcula el interstitial para pintarlo posteriormente en el bottom.jsp 
	
	if(!isNewsletterPage)
	{
		_log.debug("SKIN");
		long sectionPlid = SectionUtil.getSectionPlid(request);
		
		boolean isEnabled = false;
		boolean allowCtxVars = ContextVariables.ctxVarsEnabled(themeDisplay.getScopeGroupFriendlyURL());
		String advertisementType = AdvertisementUtil.SEGMENTATION_BY_METADATA;
		List<String> advCategories = null; 
		
		SlotAssignment slotAssignment = null;
		String categoryId = StringPool.BLANK;
		String categoryName = StringPool.BLANK;
		String categoryNormalizedName = StringPool.BLANK;
		
		if( AdvertisementUtil.isSkinEnabledForGroup(themeDisplay.getScopeGroup()) )
		{
			_log.debug("SKIN is enabled for this group");
			advCategories = AdvertisementUtil.getAdvertisementCategories(request, themeDisplay.getScopeGroupId());
			
			if( advCategories.size()>0 )
			{
				for(String catId : advCategories)
				{
					if (_log.isDebugEnabled())
						_log.debug( String.format("SKIN for category %s", catId) );
							
					slotAssignment = MetadataAdvertisementTools.getSkin(themeDisplay.getScopeGroupId(),catId);
					if(slotAssignment!=null)
					{
						categoryId = catId;
						categoryName = MetadataAdvertisementTools.getCategoryName(themeDisplay.getScopeGroupId(), catId);
						categoryNormalizedName = MetadataAdvertisementTools.getCategoryNormalizedName(themeDisplay.getScopeGroupId(), catId);
						
						if (_log.isDebugEnabled())
							_log.debug( String.format("SKIN for category %s: categoryName=%s, categoryNormalizedName=%s", catId, categoryName, categoryNormalizedName) );
							
						break;
					}
				}
			}
			
			if(slotAssignment==null)
			{
				advertisementType = AdvertisementUtil.SEGMENTATION_BY_LAYOUT;
				slotAssignment = AdvertisementUtil.getSkin4Slot( request, themeDisplay.getScopeGroup(), sectionPlid );
			}
			
			if(Validator.isNotNull(slotAssignment))
			{
				try
				{
					String imgPath = slotAssignment.getSkinImagePath();
					isEnabled = slotAssignment.getEnabled();
					
					if (_log.isDebugEnabled())
						_log.debug( String.format("SKIN slotAssignment enable=%b, imagePath=%s", isEnabled, GetterUtil.getString2(imgPath, "")) );
					
					if( isEnabled && Validator.isNotNull(imgPath) )
					{
						String skinSuperid	= slotAssignment.getSuperId();
						String bckColor 	= slotAssignment.getSkinBckColor();
						String imgName		= slotAssignment.getSkinName();
						String fEntryId 	= slotAssignment.getSkinFileUuid();
						String dispMode 	= slotAssignment.getSkinDisplayMode();
						String clickUrl 	= slotAssignment.getSkinClickUrl();
						String clickScript	= slotAssignment.getSkinClickScript();
	
						if (_log.isDebugEnabled())
							_log.debug( String.format("SKIN slotAssignment bckColor=%s, dispMode=%s", GetterUtil.getString2(bckColor, ""), GetterUtil.getString2(dispMode, "")) );

						if( allowCtxVars )
						{
							if(Validator.isNotNull(categoryName))
								AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_NAMES_ARRAY, categoryName);
							if(Validator.isNotNull(categoryNormalizedName))
								AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY, categoryNormalizedName);
							
							String slotName = slotAssignment.getSlotName();
							
							Map<String, String> localCtxVars = new HashMap<String, String>();
							localCtxVars.put( ContextVariables.ADSLOT_NAME, slotName );
							
							if(Validator.isNotNull(categoryName))
								localCtxVars.put( ContextVariables.METADATA_NAME, categoryName);
							if(Validator.isNotNull(categoryNormalizedName))
								localCtxVars.put( ContextVariables.METADATA_FRIENDLY_NAME, categoryNormalizedName);
							
							int lastSlashIdx = imgName.lastIndexOf("/");
							imgName = imgName.substring( lastSlashIdx!=-1?lastSlashIdx+1:0 );
							int dotIdx = imgName.indexOf(".");
							if( dotIdx!=-1 )
								imgName = imgName.substring(0, dotIdx);
							
							localCtxVars.put( ContextVariables.SKIN_NAME, imgName );
							
							Map<String, String> globalCtxVars = new HashMap<String, String>();
							if( ContextVariables.findCtxVars(clickUrl) || ContextVariables.findCtxVars(clickScript) || 
									(Validator.isNull( fEntryId ) && ContextVariables.findCtxVars(imgPath)) )
							{
								if(advertisementType.equalsIgnoreCase(AdvertisementUtil.SEGMENTATION_BY_LAYOUT))
									globalCtxVars = AdvertisementUtil.getAdvertisementCtxVars(request);
								else
									globalCtxVars = AdvertisementUtil.getAdvertisementCategoryCtxVars(request, themeDisplay.getScopeGroupId(), categoryId);
								
								ErrorRaiser.throwIfNull(globalCtxVars);
							}
								
							//Se sustituyen las variables por el valor correspondiente y se guardan en el request para pintarlas despues.
							if( Validator.isNotNull(clickUrl) && ContextVariables.findCtxVars(clickUrl) )
							{
								clickUrl = ContextVariables.replaceCtxVars(clickUrl, globalCtxVars);
								clickUrl = ContextVariables.replaceCtxVars(clickUrl, localCtxVars);
							}
							
							if( Validator.isNotNull(clickScript) && ContextVariables.findCtxVars(clickScript) )
							{
								clickScript = ContextVariables.replaceCtxVars(clickScript, globalCtxVars);
								clickScript = ContextVariables.replaceCtxVars(clickScript, localCtxVars);
							}
							
							if( Validator.isNull( fEntryId ) && ContextVariables.findCtxVars(imgPath) )
							{
								imgPath = ContextVariables.replaceCtxVars(imgPath, globalCtxVars);
								imgPath = ContextVariables.replaceCtxVars(imgPath, localCtxVars);
							}
						}
						
						if( Validator.isNotNull(clickUrl) )
							original_request.setAttribute("clickUrl", clickUrl);
						
						if( Validator.isNotNull(clickScript) )
							original_request.setAttribute("clickScript", clickScript);
						
						if( Validator.isNotNull( skinSuperid ) )
							//Se añade el id del tag global al request
							AdvertisementUtil.add2AttributeValueList(request, AdvertisementUtil.PARENT_TAGS_IDS, skinSuperid);
				
          out.write("\r\n");
          out.write("\t\t\t\t\t\t\t<style type=\"text/css\">\r\n");
          out.write("\t\t\t\t\t\t\t\tbody\r\n");
          out.write("\t\t\t\t\t\t\t\t{\r\n");
          out.write("\t\t\t\t");
		if( Validator.isNotNull(bckColor) && !bckColor.isEmpty() ) 
          out.write("\r\n");
          out.write("\t\t\t\t\t\t\t\t\tbackground-color: #");
          out.print( bckColor );
          out.write(";\r\n");
          out.write("\t\t\t\t\t\t\t\t\tbackground-image: url('");
          out.print( imgPath );
          out.write("');\r\n");
          out.write("\t\t\t\t\t\t\t\t\tbackground-repeat: no-repeat;\r\n");
          out.write("\t\t\t\t");
		if( Validator.isNotNull(dispMode) && !dispMode.isEmpty() ) 
          out.write("\r\n");
          out.write("\t\t\t\t\t\t\t\t\tbackground-attachment: ");
          out.print( dispMode );
          out.write(";\r\n");
          out.write("\t\t\t\t\t\t\t\t\tbackground-position: center top;\r\n");
          out.write("\t\t\t\t\t\t\t\t}\r\n");
          out.write("\t\t\t\t\t\t\t</style>\r\n");
          out.write("\t\t\t\t\t\r\n");
          out.write("\t\t\t\t");

					
					}
				}
				catch(ServiceError se)
				{
					_log.debug(se);
				}
				catch(Exception e)
				{
					_log.error(e);
				}
			}
		}
		
		if( AdvertisementUtil.isInterstitialEnabledForGroup(themeDisplay.getScopeGroup()) )
		{
			slotAssignment = null;
			categoryId = StringPool.BLANK;
			categoryName = StringPool.BLANK;
			categoryNormalizedName = StringPool.BLANK;
			advertisementType = AdvertisementUtil.SEGMENTATION_BY_METADATA;
			JSONObject intersInfo = JSONFactoryUtil.createJSONObject();
			
			if(advCategories == null)
				advCategories = AdvertisementUtil.getAdvertisementCategories(request, themeDisplay.getScopeGroupId());
			
			if( advCategories.size()>0 )
			{
				for(String catId : advCategories)
				{
					slotAssignment = MetadataAdvertisementTools.getInterstitial(themeDisplay.getScopeGroupId(), catId);
					if(slotAssignment!=null)
					{
						categoryId = catId;
						categoryName = MetadataAdvertisementTools.getCategoryName(themeDisplay.getScopeGroupId(), catId);
						categoryNormalizedName = MetadataAdvertisementTools.getCategoryNormalizedName(themeDisplay.getScopeGroupId(), catId);
						
						intersInfo.put("categoryId", categoryId);
						intersInfo.put("categoryName", categoryName);
						intersInfo.put("categoryNormalizedName", categoryNormalizedName);
						
						break;
					}
				}
			}
			
			if(slotAssignment==null)
			{
				advertisementType = AdvertisementUtil.SEGMENTATION_BY_LAYOUT;
				slotAssignment = AdvertisementUtil.getInterstitial4Slot( request, themeDisplay.getScopeGroup(), sectionPlid );
			}
			
			if(Validator.isNotNull(slotAssignment))
			{
				String tagscript = AdvertisementUtil.hideAdvWithFake() ? slotAssignment.getFakeTagScript() : slotAssignment.getTagScript();
				isEnabled = slotAssignment.getEnabled();
				
				if( isEnabled && Validator.isNotNull(tagscript) )
				{
					intersInfo.put("advertisementType", advertisementType);
					
					String interSuperId = slotAssignment.getSuperId();
					
					if( Validator.isNotNull( interSuperId ) )
						//Se añade el id del tag global al request
						AdvertisementUtil.add2AttributeValueList(request, AdvertisementUtil.PARENT_TAGS_IDS, interSuperId);
					
					if( allowCtxVars )
					{
						if(Validator.isNotNull(categoryName))
							AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_NAMES_ARRAY, categoryName);
						if(Validator.isNotNull(categoryNormalizedName))
							AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY, categoryNormalizedName);
						
						String tagname = slotAssignment.getTagName();
						if( Validator.isNotNull( tagname ) )
							AdvertisementUtil.add2AttributeValueList(request, ContextVariables.TAGS_NAMES_ARRAY, tagname);
					}
					
					//Se añade el interstitial al request para pintarlo despues
					original_request.setAttribute(AdvertisementUtil.ADV_INTER, slotAssignment);
					original_request.setAttribute(AdvertisementUtil.ADV_INTER_CAT, intersInfo);
				}
			}
		}
	
		//Pintar todos los script globales
		if( Validator.isNotNull(PublicIterParams.get(original_request, AdvertisementUtil.PARENT_TAGS_IDS) ) )
		{
			String value = "";
			Map<String, String> arrayCtxVars = new HashMap<String, String>();
			
			List<String> adslotNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.ADSLOTS_NAMES_ARRAY);
			if( Validator.isNotNull(adslotNames) )
			{
				value = ContextVariables.getListAsString( adslotNames );
				arrayCtxVars.put( ContextVariables.ADSLOTS_NAMES_ARRAY, value );
			}
			
			List<String> tagNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.TAGS_NAMES_ARRAY);
			if( Validator.isNotNull(tagNames) )
			{
				value = ContextVariables.getListAsString( tagNames );
	
				arrayCtxVars.put( ContextVariables.TAGS_NAMES_ARRAY, value );
			}
			
			List<String> metadataNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.METADATA_NAMES_ARRAY);
			if( Validator.isNotNull(metadataNames) )
			{
				value = ContextVariables.getListAsString( metadataNames );
	
				arrayCtxVars.put( ContextVariables.METADATA_NAMES_ARRAY, value );
			}
			
			List<String> metadataFriendlyNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY);
			if( Validator.isNotNull(metadataFriendlyNames) )
			{
				value = ContextVariables.getListAsString( metadataFriendlyNames );
	
				arrayCtxVars.put( ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY, value );
			}
			
			List<String> globalScripts = (List<String>)PublicIterParams.get(original_request, AdvertisementUtil.PARENT_TAGS_IDS);
			if(Validator.isNotNull(globalScripts))
			{
				boolean hideAdvWithFake = AdvertisementUtil.hideAdvWithFake();
				for( String globalScrpt : globalScripts )
				{
					try
					{
						String globalTagScript = "";
						String sql = String.format("select %s from adtags where tagid='%s'", 
										hideAdvWithFake ? "faketagscript" : "tagscript", globalScrpt);
						List<Object> resultList = PortalLocalServiceUtil.executeQueryAsList( String.format( sql, globalScrpt) );
						if (resultList != null && resultList.size() > 0 && Validator.isNotNull(resultList.get(0)))
						{
							globalTagScript = resultList.get(0).toString();
							if (allowCtxVars && ContextVariables.findCtxVars(globalTagScript))
							{
								categoryId 		  = String.valueOf( PublicIterParams.get(original_request, WebKeys.ITER_ADSLOT_CATEGORYID) );
								advertisementType = String.valueOf( PublicIterParams.get(original_request, WebKeys.ITER_ADSLOT_ADTYPE) );
								
								if (Validator.isNull(categoryId) || Validator.isNull(advertisementType))
									advertisementType = AdvertisementUtil.SEGMENTATION_BY_LAYOUT;
								
								Map<String, String> globalCtxVars = null;
								if(advertisementType.equalsIgnoreCase(AdvertisementUtil.SEGMENTATION_BY_LAYOUT))
									globalCtxVars = AdvertisementUtil.getAdvertisementCtxVars(request);
								else
									globalCtxVars = AdvertisementUtil.getAdvertisementCategoryCtxVars(request, scopeGroupId, categoryId);
								ErrorRaiser.throwIfNull(globalCtxVars);
								
								globalTagScript = ContextVariables.replaceCtxVars(globalTagScript, globalCtxVars);
								globalTagScript = ContextVariables.replaceCtxVars(globalTagScript, arrayCtxVars);
							}

          out.write("\r\n");
          out.write("\t\t\t\t\t\t\t");
          out.print( globalTagScript );
          out.write('\r');
          out.write('\n');

						}
					}
					catch(ServiceError se)
					{
						_log.debug(se);
					}
					catch(Exception e)
					{
						_log.error(e);
					}
				}
			}
		}
	}

          out.write("\r\n");
          out.write("\r\n");
          int evalDoAfterBody = _jspx_th_c_005fif_005f22.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fif_005f22.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f22);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f22);
      out.write("\r\n");
      out.write("\r\n");
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

  private boolean _jspx_meth_liferay_002dtheme_005fmeta_002dtags_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  liferay-theme:meta-tags
    com.liferay.taglib.theme.MetaTagsTag _jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0 = (com.liferay.taglib.theme.MetaTagsTag) _005fjspx_005ftagPool_005fliferay_002dtheme_005fmeta_002dtags_005fnobody.get(com.liferay.taglib.theme.MetaTagsTag.class);
    _jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0.setPageContext(_jspx_page_context);
    _jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0.setParent(null);
    int _jspx_eval_liferay_002dtheme_005fmeta_002dtags_005f0 = _jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0.doStartTag();
    if (_jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fliferay_002dtheme_005fmeta_002dtags_005fnobody.reuse(_jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fliferay_002dtheme_005fmeta_002dtags_005fnobody.reuse(_jspx_th_liferay_002dtheme_005fmeta_002dtags_005f0);
    return false;
  }

  private boolean _jspx_meth_c_005fotherwise_005f1(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fchoose_005f1, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:otherwise
    org.apache.taglibs.standard.tag.common.core.OtherwiseTag _jspx_th_c_005fotherwise_005f1 = (org.apache.taglibs.standard.tag.common.core.OtherwiseTag) _005fjspx_005ftagPool_005fc_005fotherwise.get(org.apache.taglibs.standard.tag.common.core.OtherwiseTag.class);
    _jspx_th_c_005fotherwise_005f1.setPageContext(_jspx_page_context);
    _jspx_th_c_005fotherwise_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f1);
    int _jspx_eval_c_005fotherwise_005f1 = _jspx_th_c_005fotherwise_005f1.doStartTag();
    if (_jspx_eval_c_005fotherwise_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("\r\n");
        out.write("\t\t\t\t\t\t\treturn 'raw';\r\n");
        out.write("\t\t\t\t\t\t");
        int evalDoAfterBody = _jspx_th_c_005fotherwise_005f1.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fotherwise_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f1);
    return false;
  }
}
