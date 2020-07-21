package com.protecmedia.iter.xmlio.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.xmlio.service.ImportMgrLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.impl.ImportMgrLocalServiceImpl;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

public class ImportUserServlet extends HttpServlet{
	private static final long serialVersionUID = 1L;
	
	private static Log	_log = LogFactoryUtil.getLog(ImportUserServlet.class);
		
	// Solo va a haber una company en base de datos, luego podemos buscar el grupo por su nombre
	private final String GET_GROUPID_BY_FRIENDLY_URL = new StringBuffer()
		.append("SELECT groupId           \n")
		.append("FROM group_              \n")
		.append("WHERE friendlyURL = '%s' \n").toString();
				
	// Importacion de usuarios
	@Deprecated
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		_log.trace(XmlioKeys.PREFIX_USER_LOG + "In ImportUserServlet.doPost");
		
		final Long t0 = Calendar.getInstance().getTimeInMillis();		
		try
		{			
			final String xmlDirectory = request.getParameter("xmlPath");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlDirectory), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid xml directory");
			
			final String xmlBackupDirectory = request.getParameter("backupPath");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlBackupDirectory),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Backup directory is null");

			// Buscamos el id del grupo (como solo hay un company, se puede buscar por su nombre)
			final String friendlyUrl = request.getParameter("groupName");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(friendlyUrl), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupName is null");
			String sql = String.format(GET_GROUPID_BY_FRIENDLY_URL, StringEscapeUtils.escapeSql(friendlyUrl));
			_log.debug(XmlioKeys.PREFIX_USER_LOG + "Query to get the group id: \n" + sql);
			List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);	
			ErrorRaiser.throwIfNull(result, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_USER_LOG + "group '" + friendlyUrl + "' not fount");	
			String groupId = ((Object)result.get(0)).toString();		
			
	        final boolean passwordInMD5 = GetterUtil.getBoolean(request.getParameter("passwordInMD5"), false);        
	        final boolean deleteUsers   = GetterUtil.getBoolean(request.getParameter("deleteUsers"),   false);	        
	        final File workingDirectory = new File(xmlDirectory);
	        final File backupDirectory  = new File(xmlBackupDirectory);
		
			if (workingDirectory.exists() && workingDirectory.isDirectory() && workingDirectory.canRead()){		
				// Obtenemos los archivos xml que estan en el directorio
				File[] files = workingDirectory.listFiles(new FilenameFilter(){
				    public boolean accept(File xmlsDirectory, String name){
				        return name.toLowerCase().endsWith(".xml"); 
				    }
				});		
				
				if (null != files && files.length > 0){							
					File file = null;
					boolean ok;
					
					// Obtenemos los userprofiles del formulario de registro.
					sql = String.format(ImportMgrLocalServiceImpl.GET_USER_PROFILES, groupId);
					_log.debug(new StringBuffer(XmlioKeys.PREFIX_USER_LOG).append("Query to get user profile of register form: \n").append(sql).toString());
					final List<Object> userprofiles = PortalLocalServiceUtil.executeQueryAsList(sql);
					ErrorRaiser.throwIfFalse(null != userprofiles && userprofiles.size() > 0, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, XmlioKeys.PREFIX_USER_LOG + "No userprofiles found in the register form or there is not a register form");		
					
					final String serverIp = PropsValues.ITER_LIVE_SERVERS_OWNURL;					
					final long defaultUserId = GroupMgr.getDefaultUserId();
					
					String importId = null;
					ImportMgrLocalServiceImpl iMLSI = new ImportMgrLocalServiceImpl();
					
					// Recorremos los xml
					for (int f = 0; f < files.length; f++ ){
						ok = true;					
						
						try{
							file = files[f];		
							
							if (file.canRead()){									
								final Document xml = SAXReaderUtil.read(file);								
								/* Debería llegarnos un xml con el siguiente formato:
								  
								 <?xml version="1.0"?>
						        	<us>        
							            <u>     
							                <f> 							                
							                	<!-- El campo aboid es opcional y puede venir aunque no lo este en el formulario de registro -->
							                	<aboid><![CDATA[psr]]></aboid>							
							                    <usrname><![CDATA[psr]]></usrname>
							                    <pwd><![CDATA[0be6714763d1092832e13c29feeddba0]]></pwd>
							                    <email><![CDATA[]]></email>
							                    <firstname><![CDATA[Paco]]></firstname>
							                    <lastname><![CDATA[Sanchez]]></lastname>
							                    <secondlastname><![CDATA[Roble]]></secondlastname>
							                    <avatarurl><![CDATA[http://www.direccion.com/imagen.jpg]]></avatarurl>
							                    <disqusid><![CDATA[]]></disqusid>
							                    <facebookid><![CDATA[]]></facebookid>
							                    <googleplusid><![CDATA[]]></googleplusid>
							                    <twitterid><![CDATA[]]></twitterid>
							                    <registerdate><![CDATA[20090410125354]]></registerdate>
							                    <lastlogindate><![CDATA[20131101092733]]></lastlogindate>
							                    <updateprofiledate><![CDATA[20131101104543]]></updateprofiledate>
							                </f>
							                <o>                                               
							                    <i n="aficiones">   
							                        <v><![CDATA[fotografía]]></v>   
							                        <v><![CDATA[lectura]]></v>   
							                        <v><![CDATA[fútbol]]></v>   
							                    </i>   
							                    <i n="estatura">
							                        <v><![CDATA[1.86]]></v>
							                    </i>   
							                    <i n="nacimiento">
							                        <v><![CDATA[19850410000000]]></v>
							                    </i>   
							                    ...
							                </o>
							            </u>
							            ...
							        </us> */								
									
								importId = SQLQueries.getUUID();
								// Insertamos un registro importation
								iMLSI.insertUserImport(importId, groupId, file, serverIp);
								
								// Obtenemos los usuarios a importar del xml actual
								final List<Node> users = xml.getRootElement().selectNodes("/us/u");	
								
								if (null == users || users.size() == 0){
									ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, XmlioKeys.PREFIX_USER_LOG + "No users found in the xml file: " + file.getAbsolutePath());
								}								
								for (int u = 0; u < users.size(); u++){	
									_log.debug(XmlioKeys.PREFIX_USER_LOG + " user: " + u);		
									Node user = users.get(u);
																		
									// Llamada dinámica para evitar referencias cruzadas									
									Class<?> comObject = Class.forName("com.protecmedia.iter.user.service.IterUserMngServiceUtil");									
					
									Method method = comObject.getMethod("importUser",           java.lang.Object.class,
																		java.lang.Object.class, java.lang.Object.class,
								                                        java.lang.Object.class, java.lang.Object.class,
								                                        java.lang.Object.class, java.io.File.class, 
								                                        boolean.class,          boolean.class);
																		
									ErrorRaiser.throwIfNull(method, XmlioKeys.PREFIX_USER_LOG + "Mehod IterUserMngServiceUtil.importUser not found");
									
									Document xmlUserImportResult = SAXReaderUtil.read("<d/>");
									
									// Parametros del metodo
									Object[] methodParams = new Object[9];
									methodParams[0] = xmlUserImportResult;
									methodParams[1] = userprofiles;
									methodParams[2] = serverIp;
									methodParams[3] = user;
									methodParams[4] = groupId;
									methodParams[5] = defaultUserId;
									methodParams[6] = workingDirectory;
									methodParams[7] = passwordInMD5;
									methodParams[8] = deleteUsers;		
									
									String userName = null;
									final long userImportT0 = Calendar.getInstance().getTimeInMillis();
									
									try{
										// Importamos el usuario
										method.invoke(comObject, methodParams);
										userName = XMLHelper.getTextValueOf(user, "./f/usrname", null);																				
									}catch(Exception e){	
			                            _log.error(XmlioKeys.PREFIX_USER_LOG + " user not imported" + (Validator.isNull(userName) ? "" : ": '" + userName + "'"));
									}									
									_log.debug(XmlioKeys.PREFIX_USER_LOG + "Total time importing the user '" + userName + "': " + (Calendar.getInstance().getTimeInMillis() -userImportT0) + "ms" );
									
									// Vemos si la importacion del usuario ha fallado y contabilizamos
									try{
										if (Validator.isNotNull(XMLHelper.getTextValueOf(xmlUserImportResult, "/d/@errorCode", null))){
											ok = false;	
											iMLSI.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_USERS, importId, "0", "1");
										}else{		
											iMLSI.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_USERS, importId, "1", "0");	
										}
									}catch(Exception e){
										_log.error(e);
										_log.error(XmlioKeys.PREFIX_USER_LOG + " Error updating ok and ko import count");
									}																		
									
									// Anotamos en base de datos (importationdetail) el resultado de la operacion									
									try{
										iMLSI.insertUserDetail(importId, groupId, xmlUserImportResult.selectSingleNode("/d"));
									}catch(Exception e){
										_log.error(e);
										_log.error(XmlioKeys.PREFIX_USER_LOG + " Error inserting import detail");
									}																	
								}
							}else{ 
								ok = false;
								_log.error(new StringBuffer("The file: ").append(file.getName()).append(" can not be read").toString());
							} 
						}catch(DocumentException e){
							ok = false;
							_log.error(e);						
							_log.error(new StringBuffer(XmlioKeys.PREFIX_USER_LOG + "xml malformed").append(null != file ? ": " + file.getName()   : "").toString());
						}catch(Exception e){
							ok = false;
							_log.error(e);						
							_log.error(new StringBuffer(XmlioKeys.PREFIX_USER_LOG + "Error with the file").append(null != file ? ": " + file.getName()   : "").toString());						
						}finally{							
							// Modificamos la fecha fin de la importacion
							if (null != importId){
								try{
									ImportMgrLocalServiceUtil.updateImportationFinishTime(importId);
								}catch(Exception e){
									_log.error(e);
									_log.error(XmlioKeys.PREFIX_USER_LOG + " Error updating the import finisht time");
								}
							}
														
							// Si se ha procesado correctamente el archivo lo movemos
							if (ok && null != file && null != backupDirectory){
								
								ImportMgrLocalServiceUtil.moveImportedFiles(file, backupDirectory);
							}else{
								_log.debug(new StringBuffer(XmlioKeys.PREFIX_USER_LOG).append("Not moving files because: ")
								           .append(!ok ? "not total users were imported " : "")
								           .append(null == file ? "file is null" : "")
								           .append(null == backupDirectory ? "backupDirectory is null" : "").toString());							           
							}						
						}
					}				
				}else{
					_log.error(new StringBuffer(XmlioKeys.PREFIX_USER_LOG + "No xml files found to import users in: ").append(workingDirectory).toString());
				}
			}else{
				_log.error(new StringBuffer(XmlioKeys.PREFIX_USER_LOG + "Error reading the directory: ").append(workingDirectory));
			}
			_log.debug(new StringBuffer(XmlioKeys.PREFIX_USER_LOG + "Total elapsed time in the import/s: ").append(Calendar.getInstance().getTimeInMillis() - t0));
		}
		catch(Exception e){
			_log.error(XmlioKeys.PREFIX_USER_LOG + " unexpected error");
			_log.error(e);
		}
		finally{
			_log.debug(XmlioKeys.PREFIX_USER_LOG + "Total dopost time elapsed: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");
		}
	}
		
	// Ninguna petición será atendida por get
	@Deprecated
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{ 
		_log.trace(XmlioKeys.PREFIX_USER_LOG + "In ImportUserServlet.doGet");	 
		final String text = "Only post request are served";
		_log.trace(XmlioKeys.PREFIX_USER_LOG + text);
		
		response.setContentType("text/html");
	    PrintWriter out = response.getWriter();
	    out.println(text);
	} 

}