<%@page import="com.liferay.portal.kernel.util.MobileConfig"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.sectionservers.SectionServersMgr"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.DateUtil"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.kernel.util.GroupMgr"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.error.ErrorRaiser"%>
<%@page import="com.liferay.portal.apache.ApacheHierarchy"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="com.liferay.portal.kernel.util.HttpUtil"%>
<%@page import="java.net.URL"%>
<%@page import="com.liferay.portal.kernel.error.IterErrorKeys"%>
<%@page import="com.liferay.portal.apache.ApacheUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portlet.ContextVariables"%>
<%@page import="com.liferay.portlet.CtxvarUtil"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.service.LayoutSetLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.liferay.portal.kernel.util.PropsUtil"%>

<%@ page contentType="text/html; charset=UTF-8" %>

<%
String grpFriendlyUrl = "";
String errPg = "";
String dest = "/";
long grpid =  Long.parseLong(request.getAttribute(WebKeys.SCOPE_GROUP_ID).toString());
request.removeAttribute(WebKeys.SCOPE_GROUP_ID);

String iterUrlDefaultPrefix = String.valueOf( request.getAttribute(WebKeys.REQUEST_ITER_URL_DEFAULT_PREFIX) );
String friendlyurl = String.valueOf(request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_SECTION_FRIENDLY_URL));
if( Validator.isNull(friendlyurl) )
{
	friendlyurl = String.valueOf(request.getAttribute(WebKeys.FRIENDLY_URL));
	int prefixIndex = friendlyurl.indexOf(iterUrlDefaultPrefix);
	if( prefixIndex!=-1 )
		friendlyurl = friendlyurl.substring( prefixIndex+iterUrlDefaultPrefix.length() );
}

long layoutPlid = SectionUtil.getPlidFromFriendlyurl(grpid, friendlyurl);
String plidHierarchy = SectionUtil.getPlidHierarchy3(grpid, layoutPlid);

if(_log.isDebugEnabled())
	_log.debug("groupId: " + grpid + ", friendlyurl: " + friendlyurl + ", plidHierarchy: " + plidHierarchy);

errPg = CtxvarUtil.getContextVarValue(grpid, ContextVariables.ERROR_404, plidHierarchy);

_log.debug("error page: " + errPg );

if(Validator.isNull(errPg) && grpid!=0)
{
	grpFriendlyUrl =  GroupLocalServiceUtil.getGroup( grpid ).getFriendlyURL();
	errPg = PropsUtil.get( String.format(PropsKeys.ITER_NOTFOUND_URLPAGE_FOR_GROUPFRIENDLYURL,  grpFriendlyUrl.replace("/", ".")) );
}

if( Validator.isNull(errPg) )
{
	errPg = "/";
}
else if( !errPg.equalsIgnoreCase(StringPool.SLASH) )
{
	try
	{
		if (StringUtil.endsWith(errPg, StringPool.SLASH))
			errPg = StringUtil.replaceLast(errPg, StringPool.SLASH, StringPool.BLANK);
		LayoutLocalServiceUtil.getFriendlyURLLayout(grpid, false, errPg);
	}
	catch (Exception e)
	{
		_log.error(e);
		errPg = "/";
	}
}

//Si estamos en un contexto móvil
String mobileVirtualHost = null;
if (IterRequest.isMobileRequest() == 1L)
{
	// Obtiene la configuración móvil
	MobileConfig mobileConfig = IterGlobal.getMobileConfig(grpid);
	// Calcula la friendly url de la equivalencia a la página de error con prefijo móvil
	String mobileErrPg = StringPool.SLASH + mobileConfig.getMobileToken();
	if (!StringPool.SLASH.equals(errPg))
		mobileErrPg += errPg;
	
	try
	{
		// Comprueba si existe una equivalencia a la página de error con el prefijo móvil
		LayoutLocalServiceUtil.getFriendlyURLLayout(grpid, false, mobileErrPg);
		// Si existe, comprueba si tiene dominio móvil
		mobileVirtualHost = mobileConfig.getMobileServer();
		// Si no hay dominio, pedirá la página con el prefijo móvil
		if (Validator.isNull(mobileVirtualHost)) {
			errPg = mobileErrPg;
		}
	}
	catch (Exception e)
	{
		_log.debug("Mobile error page not found");
	}
}

String _html = StringPool.BLANK;
HttpURLConnection httpConnection = null;
try
{
	StringBuilder newUrl = new StringBuilder();
	String vh = Validator.isNotNull(mobileVirtualHost) ? mobileVirtualHost : LayoutSetLocalServiceUtil.getLayoutSet(grpid, false).getVirtualHost();
	
	String[] masterList = ApacheHierarchy.getInstance().getMasterList();
	ErrorRaiser.throwIfFalse( masterList.length > 0, IterErrorKeys.XYZ_E_APACHE_MASTERS_NOT_FOUND_ZYX);
	newUrl.append( masterList[0] ).append( errPg );
	dest = newUrl.toString() + "?earlyloadscript=false";
	
	_log.debug("Tomcat, new location : " + dest);
	
	if (IterKeys.ENVIRONMENT_PREVIEW.equals(IterLocalServiceUtil.getEnvironment()))
	{
		Date lastPubDate = GroupMgr.getGroupLastPubDate(grpid);
		String avoidCacheParam = DateUtil.getISOFormat("yyyyMMddHHmmss").format(lastPubDate);
		dest = dest.concat("&publicationDate=" + avoidCacheParam);
	}
	
	httpConnection = (HttpURLConnection)(new URL(dest).openConnection());
	httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
	httpConnection.setReadTimeout(	 ApacheUtil.getApacheReadTimeout());
	httpConnection.setRequestProperty (WebKeys.HOST, vh);
	httpConnection.setRequestProperty ("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS);
	httpConnection.setRequestMethod("GET");
	httpConnection.connect();
	HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_GET_PAGECONTENT_ZYX );
	_html = IOUtils.toString(httpConnection.getInputStream(), "UTF-8");
}
catch (Exception e)
{
	_log.error(e);
}
finally
{
	if (httpConnection != null)
		httpConnection.disconnect();
}
%>

<%= _html %>

<%!
private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.errors.404.jsp");
%>
