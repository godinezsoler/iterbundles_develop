package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.MultiReadHttpServletRequest;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.user.util.forms.ExtractorXML;
import com.protecmedia.iter.user.util.forms.HandlerDatabaseForm;
import com.protecmedia.iter.user.util.forms.HandlerEmailForm;
import com.protecmedia.iter.user.util.forms.HandlerFileForm;
import com.protecmedia.iter.user.util.forms.HandlerServletForm;


public class FormReceiver extends HttpServlet implements IFormServlet
{
	private static final long serialVersionUID = 1L;
	
	private static Log _log = LogFactoryUtil.getLog(FormReceiver.class);
	
	protected static final String FORMSERVLET_GRPID 	= "FORMSERVLET_GRPID";
	protected static final String FORMSERVLET_RESULT 	= "FORMSERVLET_RESULT";
	protected static final String FORMSERVLET_MESSAGE 	= "FORMSERVLET_MESSAGE";
	protected static final String FORMSERVLET_ACTION 	= "FORMSERVLET_ACTION";
	protected static final String FORMSERVLET_LOCATION 	= "FORMSERVLET_LOCATION";
	protected static final String FORMSERVLET_ACTIONSDOM= "FORMSERVLET_ACTIONSDOM";
	
			
	public static final String RESULT 			= "result";
	public static final String MSG 				= "msg";
	public static final String ACTION 			= "action";
	public static final String LOCATION 		= "location";
	public static final String FURTHERACTION	= "furtheraction";
	
	private static final String FORM_HANDLER_DATABASE_NAME = "fhdb";
	private static final String FORM_HANDLER_EMAIL_NAME = "fhe";
	private static final String FORM_HANDLER_SERVLET_NAME = "fhs";
	private static final String FORM_HANDLER_FILE_NAME = "fhf";
    private static final String[] HANDLER_NAMES= {FORM_HANDLER_DATABASE_NAME, FORM_HANDLER_EMAIL_NAME, FORM_HANDLER_SERVLET_NAME, FORM_HANDLER_FILE_NAME};  
    private static final String[] COLUMNS_HANDLERS_CDATA= {"fhepara", "fhecc", "fhecco","fhetextsubject", "fhetextbody", "fhffoldername", "fhfformname"};
    private static final String[] COLUMNS_LOCATION_LAYOUT= {"friendlyURL"};

    private static final String GET_SUCCESS_FORM_ACTIONS = "SELECT groupid, successsend FROM form WHERE formid='%s'";
    private static final String GET_ERROR_FORM_ACTIONS = "SELECT groupid, errorsend FROM form WHERE formid='%s'";
    
    private static final String GET_LOCATION_LAYOUT = new StringBuffer()
	.append("SELECT l.friendlyURL \n")
	.append("FROM Layout l \n")
	.append("WHERE l.uuid_ = '%s'\n").toString();
    
    private static final String GET_FORM_GROUPID = new StringBuffer()
	.append("SELECT f.groupid \n")
	.append("FROM form f \n")
	.append("WHERE f.formid = '%s'\n").toString();
    
    private static final String GET_FORM_FORMNAME = new StringBuffer()
	.append("SELECT f.name \n")
	.append("FROM form f \n")
	.append("WHERE f.formid = '%s'\n").toString();

	private static final String GET_FORM_HANDLERS = new StringBuffer()
	
		.append("SELECT (count(fhdb.handlerid)>0 OR count(fhe.handlerid)>0 OR count(fhf.handlerid) > 0 OR fhs.processdata)  'isProcess', \n")
		.append("fhdb.handlerid 'hdbid', fhdb.enabled 'fhdbenabled', fhdb.critic 'fhdbcritic', \n")
		.append("fhe.handlerid 'fheid', fhe.enabled 'fheenabled', fhe.critic 'fhecritic', fhe.processdata 'fheprocessdata', fhe.type 'fhetype', fhe.para 'fhepara', fhe.cc 'fhecc', fhe.cco 'fhecco', fhe.smtpserver 'fhesmtpserver', fhe.textsubject 'fhetextsubject', fhe.textbody 'fhetextbody', (SELECT fxsltFHE.xsluuid FROM formxsltransform fxsltFHE INNER JOIN formhandleremail fheXSL ON fheXSL.transformid = fxsltFHE.transformid WHERE fheXSL.formid = fhe.formid) 'fhexslid', \n")
		.append("fhf.handlerid 'fhfid', fhf.enabled 'fhfenabled', fhf.critic 'fhfcritic', fhf.processdata 'fhfprocessdata', (SELECT fxsltFHF.xsluuid FROM formxsltransform fxsltFHF INNER JOIN formhandlerfile fhfXSL ON fhfXSL.transformid = fxsltFHF.transformid WHERE fhfXSL.formid = fhf.formid) 'fhfxslid', fhf.foldername 'fhffoldername', fhf.formname 'fhfformname', \n")
		.append("fhs.handlerid 'fhsid' , fhs.enabled 'fhsenabled', fhs.critic 'fhscritic', fhs.url 'fhsurl', fhs.processdata 'fhsprocessdata', (SELECT fxsltFHS.xsluuid FROM formxsltransform fxsltFHS INNER JOIN formhandlerservlet fhsXSL ON fhsXSL.transformid = fxsltFHS.transformid WHERE fhsXSL.formid = fhs.formid) 'fhsxslid' \n")
		.append("FROM form f, formhandlerdb fhdb, formhandleremail fhe, formhandlerfile fhf, formhandlerservlet fhs \n")
		.append("WHERE f.formid = '%s' AND fhdb.formid = f.formid AND fhe.formid = f.formid AND fhf.formid = f.formid AND fhs.formid = f.formid \n")
		.toString();
	
    
    public FormReceiver() {
        super();
    }
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		JSONObject json = null;
		
		String formid = request.getParameter("formid");
		long scopegroupId = 0L;
		try 
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			scopegroupId = PortalUtil.getScopeGroupId(request);
			MultiReadHttpServletRequest multiRequest = new MultiReadHttpServletRequest(request);
			doInternalPost(multiRequest, response, scopegroupId);
			json = doOkPostAction(request, response, formid, null);
		}
		catch (Throwable e)
		{
			IterMonitor.logEvent(scopegroupId, Event.ERROR, new Date(System.currentTimeMillis()), "Form receiver error", "", e);
			getLog().error(e);
			json = doKoPostAction(request, response, e, formid, null);
		}
		finally
		{
			addResponse(response, json);
		}
	}

	private void doInternalPost(HttpServletRequest request, HttpServletResponse response, long scopegroupId) throws ServiceError, NoSuchMethodException, SecurityException, InterruptedException, Exception  
	{
		//obtain formId
		String formId = request.getParameter("formid");
		ErrorRaiser.throwIfNull(formId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//obtain all necessary data of handlers
		getLog().trace("obtain handlers info");
		getLog().debug(new StringBuffer( "Query: ").append(String.format(GET_FORM_HANDLERS, formId)));
		Document domHandlers = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_FORM_HANDLERS, formId),COLUMNS_HANDLERS_CDATA);
		if (Validator.isNull(domHandlers))
		{ 
			getLog().debug("domHandlers is null");
		}
		ErrorRaiser.throwIfNull(domHandlers);
		
		
		
		
		//check data & generate XML from request if it's necessary
		String generateXML = XMLHelper.getTextValueOf(domHandlers, "/rs/row/@isProcess", null);
		if (Validator.isNull(generateXML))
		{ 
			getLog().debug("formid not exist");
		}
		ErrorRaiser.throwIfNull(generateXML, IterErrorKeys.XYZ_FORM_NOTFOUND_ZYX);
		
		boolean isGenerateXML = GetterUtil.getBoolean(generateXML, false);
		Map<String, ArrayList> adjuntos = new HashMap<String, ArrayList>();
		getLog().trace((isGenerateXML) ? "check data and generate XML " : "only check data");
		Document xmlDom = ExtractorXML.createXML(request, isGenerateXML, adjuntos);
		
		//run handlers
		startHandlers(scopegroupId, formId, request, domHandlers, xmlDom, adjuntos);
	}

	private void startHandlers(long scopegroupId, String formId, HttpServletRequest request, Document domHandlers, Document xmlDom, Map<String, ArrayList> adjuntos) throws ServiceError, NoSuchMethodException, SecurityException, InterruptedException 
	{
		List<Thread> threadsJoins = new ArrayList<Thread>();
		Node nodeHandlers = SAXReaderUtil.createXPath("/rs//row").selectSingleNode(domHandlers);
		
		// Generamos el id del nuevo formulario recibido (formreceived.formreceivedid)
        final String formReceivedId = SQLQueries.getUUID();   
        ErrorRaiser.throwIfNull(formReceivedId);
		
		List<Throwable> exceptionsHandlers = new ArrayList<Throwable>();
		Semaphore semaphore = new Semaphore(1);
		
		for(int i = 0; i < HANDLER_NAMES.length; i++)
		{
			String criticStr = XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("critic").toString());
			ErrorRaiser.throwIfNull(criticStr);			
			boolean critic = GetterUtil.getBoolean(criticStr);
			
			String enabledStr = XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("enabled").toString());
			ErrorRaiser.throwIfNull(enabledStr);			
			boolean enabled = GetterUtil.getBoolean(enabledStr);
			
			if(enabled)
			{
				Thread thread = null;
				
				if(Validator.equals(HANDLER_NAMES[i], FORM_HANDLER_DATABASE_NAME))
				{
					//obtenemos los datos del formulario
//					getLog().debug(new StringBuffer( "Query: ").append(String.format(GET_FORM_GROUPID, formId)));
//					Document domFormGroup = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_FORM_GROUPID, formId));
//					if (Validator.isNull(domFormGroup))
//					{ 
//						getLog().debug("domFormGroup is null");
//					}
//					ErrorRaiser.throwIfNull(domFormGroup);
//					
//					Long groupId = XMLHelper.getLongValueOf(domFormGroup, "/rs/row/@groupid", -1);
//					ErrorRaiser.throwIfFalse(groupId >= 0);
					thread = new HandlerDatabaseForm(xmlDom,adjuntos, scopegroupId, formReceivedId, 
							(critic) ? exceptionsHandlers : null,
							(critic) ? semaphore : null);
				}
				else
				{
					String processDataStr = XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("processdata").toString());
					ErrorRaiser.throwIfNull(processDataStr);			
					boolean processdata = GetterUtil.getBoolean(processDataStr);
					
					if(Validator.equals(HANDLER_NAMES[i], FORM_HANDLER_EMAIL_NAME))
					{
						//obtenemos los el nombre del formulario
						String formname = xmlDom.getRootElement().attributeValue("formname");
						ErrorRaiser.throwIfNull(formname);
						
						thread = new HandlerEmailForm(
								scopegroupId,
								processdata,
								xmlDom, 
								(processdata) ? adjuntos : null,
								(processdata) ? XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("xslid").toString(), null) : null,
								request,
								formname,
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("type").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("para").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("cc").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("cco").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("smtpserver").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("textsubject").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("textbody").toString()),
								formReceivedId,
								(critic) ? exceptionsHandlers : null,
								(critic) ? semaphore : null
							);
					}
					else if(Validator.equals(HANDLER_NAMES[i], FORM_HANDLER_SERVLET_NAME))
					{
						thread = new HandlerServletForm(
								scopegroupId,
								processdata,
								xmlDom,
								(processdata) ? adjuntos : null,
								(processdata) ? XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("xslid").toString(), null) : null,
								request, 
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("url").toString()),
								formReceivedId,
								(critic) ? exceptionsHandlers : null,
								(critic) ? semaphore : null
							);
					}
					else if(Validator.equals(HANDLER_NAMES[i], FORM_HANDLER_FILE_NAME))
					{
						thread = new HandlerFileForm(
								scopegroupId,
								processdata,
								xmlDom,
								(processdata) ? adjuntos : null,
								(processdata) ? XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append("@").append(HANDLER_NAMES[i]).append("xslid").toString(), null) : null,
								request,
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("foldername").toString()),
								XMLHelper.getTextValueOf(nodeHandlers, new StringBuffer().append(HANDLER_NAMES[i]).append("formname").toString()),
								formReceivedId,
								(critic) ? exceptionsHandlers : null,
								(critic) ? semaphore : null
							);
					}		
				}
				ErrorRaiser.throwIfFalse(Validator.isNotNull(thread) || !critic);
				getLog().trace("start thread handler " + HANDLER_NAMES[i]);
				thread.start();
				
				if(critic)
					threadsJoins.add(thread);
			}
		}
		for (Thread thread : threadsJoins) {
			try {
				thread.join();
			} catch (InterruptedException ie) {
			}
		}
		if(!exceptionsHandlers.isEmpty()){
			throw new InterruptedException();
		}
	}

	protected JSONObject createJsonResponse(HttpServletRequest request)
	{
		JSONObject json = JSONFactoryUtil.createJSONObject();
		
		json.put(RESULT,	getFormResult(request));
		json.put(MSG, 		getFormMessage(request));

		JSONObject jsonFurtheraction = JSONFactoryUtil.createJSONObject();
		jsonFurtheraction.put(ACTION,  getFormAction(request));
		
		String location = getFormLocation(request);
		if (Validator.isNotNull(location))
			jsonFurtheraction.put(LOCATION, location);
		
		json.put(FURTHERACTION, jsonFurtheraction);
		
		getLog().debug("returns : " + json.toString());
		
		return json;
	}
	

	protected JSONObject doKoPostAction(HttpServletRequest request, HttpServletResponse response, Throwable t, String formid, Document xtraInfo)
	{
		JSONObject json = null;
		setFormResult(request, IterKeys.KO);
		
		try 
		{
			initFormAction(request, formid);
			json = _doKoPostAction(request, response, t, formid, xtraInfo);
		} 
		catch (Exception e) 
		{
			getLog().error(e);
			setFormMessage(request, XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg", IterErrorKeys.XYZ_FORM_NOTFOUND_ZYX) );
		}
		finally
		{
			if (Validator.isNull(json))
				json = createJsonResponse(request);
			
			long groupId = GroupMgr.getGlobalGroupId();
			try
			{
				groupId = PortalUtil.getScopeGroupId(request);
			}
			catch(Exception e){getLog().error(e);}
			
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new java.util.Date(), json.getString(MSG), null, t);
		}
		
		return json;
	}
	
	/**
	 * Realiza las acciones especificas en caso de fallo
	 * @param t
	 * @param formid
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 */
	protected JSONObject _doKoPostAction(HttpServletRequest request, HttpServletResponse response, 
										 Throwable t, String formid, Document xtraInfo) throws Exception
	{
		String message = StringPool.BLANK;
		Document formActionsDom = getFormActionsDom(request);
		
		if (t instanceof ServiceError)
		{
			ServiceError se = (ServiceError)t;
			String eCode = se.getErrorCode();
			
			if(eCode.equals(IterErrorKeys.XYZ_ITR_E_REQUIREDFIELD_ZYX))
			{
				message = XMLHelper.getTextValueOf(formActionsDom, "/ko/invalidfieldmsg");
				setFormAction(request, IterKeys.ACTION_NONE);
			}
			else if (eCode.equals(IterErrorKeys.XYZ_ITR_E_CAPTCHAINVALID_ZYX))
			{
				setFormAction(request, IterKeys.ACTION_NONE);
				message = XMLHelper.getTextValueOf(formActionsDom, "/ko/captchafailmsg");
			
			}
			else
			{
				getAction(request, formid);	
			}
		}
		else
		{
			getAction(request, formid);	
		}
		
		if( Validator.isNull(message) )
			message = XMLHelper.getTextValueOf(formActionsDom, "/ko/handlerfailmsg");

		setFormMessage(request, message);
		return createJsonResponse(request);
	}
	
	protected JSONObject doOkPostAction(HttpServletRequest request, HttpServletResponse response, String formid, Document xtraInfo)
	{
		JSONObject json = null;
		setFormResult(request, IterKeys.OK);
		
		try 
		{
			initFormAction(request, formid);
			json = _doOkPostAction(request, response, formid, xtraInfo);
		}
		catch (Exception e) 
		{
			getLog().error(e);
		}
		finally
		{
			if (Validator.isNull(json))
				json = createJsonResponse(request);
			
			// Se borran la cookie del referer si existiese
			CookieUtil.deleteCookies( request, response, new String[]{IterKeys.COOKIE_NAME_SOCIAL_REDIRECT} );
		}
		
		return json;
	}
	/**
	 * Realiza las acciones especificas en caso de envío correcto
	 * @param formid
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 */
	protected JSONObject _doOkPostAction(HttpServletRequest request, HttpServletResponse response, String formid, Document xtraInfo) throws Exception
	{
		if( Validator.isNull(getFormMessage(request)) )
			setFormMessage(request, XMLHelper.getTextValueOf(getFormActionsDom(request), "/ok/msg"));
		
		getAction(request, formid);

		return createJsonResponse(request);
	}
	
	protected void initFormAction(HttpServletRequest request, String formid) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError
	{
		Document formActionsDom = getFormActionsDom(request);
		if( Validator.isNull(formActionsDom) )
		{
			Document formActiondom = null;
			String result = getFormResult(request);
					
			if(result.equals(IterKeys.OK))
			{
				String query = String.format( GET_SUCCESS_FORM_ACTIONS, formid);
				formActiondom = PortalLocalServiceUtil.executeQueryAsDom( query, new String[]{UserUtil.SUCCESS_SEND} );
				Node successSendNode = formActiondom.selectSingleNode("/rs/row/successsend");
				formActionsDom = SAXReaderUtil.read( CDATAUtil.strip( successSendNode.getText() ) );
			}
			else if (result.equals(IterKeys.KO))
			{
				String query = String.format( GET_ERROR_FORM_ACTIONS, formid);
				formActiondom = PortalLocalServiceUtil.executeQueryAsDom( query, new String[]{UserUtil.ERROR_SEND} );
				Node errorSendNode 	= formActiondom.selectSingleNode("/rs/row/errorsend");
				ErrorRaiser.throwIfNull(errorSendNode, IterErrorKeys.XYZ_FORM_NOTFOUND_ZYX);
				formActionsDom = SAXReaderUtil.read( CDATAUtil.strip( errorSendNode.getText() ) );
			}
			
			setFormGroupId(request, formActiondom.selectSingleNode("/rs/row/@groupid").getText());
			setFormActionsDom(request, formActionsDom);
		}
	}
	
	protected void getAction(HttpServletRequest request, String formId) throws SecurityException, NoSuchMethodException, ServiceError, MalformedURLException
	{
		String action 	= IterKeys.ACTION_NONE;
		String location = StringPool.BLANK;
		Node nodeActionEndSend = getFormActionsDom(request).selectSingleNode("//action");
		
		if(GetterUtil.getBoolean(XMLHelper.getTextValueOf(nodeActionEndSend, "@enabled"), false))
		{
			action = XMLHelper.getTextValueOf(nodeActionEndSend, "@type");
			if(Validator.equals(action, IterKeys.ACTION_EXTERNAL_PAGE))
			{
				action = IterKeys.ACTION_REDIRECT;
				location = XMLHelper.getTextValueOf(nodeActionEndSend, ".");
			}
			else if(Validator.equals(action, IterKeys.ACTION_INTERNAL_PAGE))
			{
				action = IterKeys.ACTION_REDIRECT;
				
				getLog().debug(new StringBuffer( "Query: ").append(String.format(GET_LOCATION_LAYOUT, formId)));
				Document domFriendlyLayout = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_LOCATION_LAYOUT, XMLHelper.getTextValueOf(nodeActionEndSend, ".")), COLUMNS_LOCATION_LAYOUT);
				ErrorRaiser.throwIfNull(domFriendlyLayout);
				
				location = XMLHelper.getTextValueOf(domFriendlyLayout, "/rs/row/friendlyURL");
			}
			else if(Validator.equals(action, IterKeys.ACTION_REFERER))
			{
				// Si hay un referer en la cookie manda este
				String redirectURLString = CookieUtil.get(request, IterKeys.COOKIE_NAME_SOCIAL_REDIRECT);
				if (Validator.isNotNull(redirectURLString))
				{
					location 	= redirectURLString;
					action 		= IterKeys.ACTION_REDIRECT;
				}
				else
				{
					redirectURLString = GetterUtil.getString2(String.valueOf(request.getAttribute(IterKeys.XTRADATA_REFERER)), StringPool.BLANK);
					if(Validator.isNotNull(redirectURLString))
					{
						location = redirectURLString;
						action 		= IterKeys.ACTION_REDIRECT;
					}
					else
						action = IterKeys.ACTION_BACK2REFERER;
				}
			}
		}
		setFormAction(request, 		action);
		setFormLocation(request, 	location);
	}
	
	protected void addResponse(HttpServletResponse response, JSONObject jsonObj) throws IOException
	{
		response.setContentType("application/json");
		ServletOutputStream out = response.getOutputStream();
		out.write( jsonObj.toString().getBytes() );
		out.flush();
	}

	public Log getLog()
	{
		return _log;
	}
	
	private void setFormActionsDom(HttpServletRequest request, Document value)
	{
		request.setAttribute(FORMSERVLET_ACTIONSDOM, value);
	}
	protected Document getFormActionsDom(HttpServletRequest request)
	{
		return (Document)request.getAttribute(FORMSERVLET_ACTIONSDOM);
	}
	private void setFormGroupId(HttpServletRequest request, String value)
	{
		request.setAttribute(FORMSERVLET_GRPID, value);
	}
	protected String getFormGroupId(HttpServletRequest request)
	{
		return GetterUtil.getString2( String.valueOf(request.getAttribute(FORMSERVLET_GRPID)), StringPool.BLANK );
	}
	private void setFormResult(HttpServletRequest request, String value)
	{
		request.setAttribute(FORMSERVLET_RESULT, value);
	}
	private String getFormResult(HttpServletRequest request)
	{
		return GetterUtil.getString2( String.valueOf(request.getAttribute(FORMSERVLET_RESULT)), IterKeys.KO );
	}
	protected void setFormMessage(HttpServletRequest request, String value)
	{
		request.setAttribute(FORMSERVLET_MESSAGE, value);
	}
	private String getFormMessage(HttpServletRequest request)
	{
		return GetterUtil.getString2( String.valueOf(request.getAttribute(FORMSERVLET_MESSAGE)), StringPool.BLANK );
	}
	protected void setFormAction(HttpServletRequest request, String value)
	{
		request.setAttribute(FORMSERVLET_ACTION, value);
	}
	private String getFormAction(HttpServletRequest request)
	{
		return GetterUtil.getString2( String.valueOf(request.getAttribute(FORMSERVLET_ACTION)), IterKeys.ACTION_NONE );
	}
	protected void setFormLocation(HttpServletRequest request, String value)
	{
		request.setAttribute(FORMSERVLET_LOCATION, value);
	}
	private String getFormLocation(HttpServletRequest request)
	{
		return GetterUtil.getString2( String.valueOf(request.getAttribute(FORMSERVLET_LOCATION)), IterKeys.ACTION_NONE );
	}
}