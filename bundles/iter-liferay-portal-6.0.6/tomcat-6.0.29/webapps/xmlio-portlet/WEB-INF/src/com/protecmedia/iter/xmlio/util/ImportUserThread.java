package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.service.ImportMgrLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.impl.ImportMgrLocalServiceImpl;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

public class ImportUserThread extends Thread{		
	private static Log _log = LogFactoryUtil.getLog(ImportUserThread.class);	
	private static final SimpleDateFormat sDFToDB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	
	private File path = null;
	private File backupPath = null; 
		
	private boolean passwordInMD5;
	private boolean deleteUsers;
	private boolean resultToBBDD;
	
	private String groupId  = null;
	
	private long defaultUserId;
	
	private List<Object> userProfile = null;
	private List<String> userProfileNames = null;
	
	private ArrayList<Exception> excepcionList = null;
	
	private Document xmlResult = null;
	
	public ImportUserThread(ThreadGroup tG, String threadName, ArrayList<Exception> excepcionList, Document xmlResult, 
		                       String groupId, long defaultUserId, List<Object> userProfile, File path, File backupPath, 
		                       boolean passwordInMD5, boolean deleteUsers, boolean resultToBBDD) throws ServiceError{	
		super(tG, threadName);
		this.excepcionList = excepcionList;
		this.xmlResult     = xmlResult;
		this.groupId       = groupId;
		this.defaultUserId = defaultUserId;
		this.userProfile   = userProfile;
		this.userProfileNames = getNamesFromUserProfiles(userProfile);
		this.path          = path;
		this.backupPath    = backupPath;	
		this.passwordInMD5 = passwordInMD5;
		this.deleteUsers   = deleteUsers;
		this.resultToBBDD  = resultToBBDD;		
	}
		
	@Override
	public void run(){
		_log.trace(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(null)).append("In ImportUserThread.run()"));
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		try{	
			File[] xmlFiles = null;				
			if (path.isDirectory()){					
				xmlFiles = path.listFiles(new FilenameFilter(){
					public boolean accept(File xmlsDirectory, String name){
						return name.toLowerCase().endsWith(XmlioKeys.XML_EXTENSION); 
					}
				});
			}else{
				xmlFiles = new File[1];
				xmlFiles[0] = path;
				// La función que importa usuarios necesita un directorio, no un archivo para obtener los archivos adjuntos
				this.path = path.getParentFile();
			}
			
			// Comprobamos que hemos encontrado al menos un archivo
			ErrorRaiser.throwIfFalse(null != xmlFiles && xmlFiles.length > 0, XmlioKeys.XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX, ImportMgrLocalServiceUtil.buildUserPrefixLog(null) + "No xml files found to import users in: '" + path + "'");		
			
			// IP del servidor actual
			final String actualIp = PropsValues.ITER_LIVE_SERVERS_OWNURL;
			
			File xmlFile = null;			
			
			// Recorremos los xml
			for (int f = 0; f < xmlFiles.length; f++){			
				boolean ok = true;
				try{							// Try dentro del for para que siga aunque un xml de problemas				
					xmlFile = xmlFiles[f];
					
					if (!xmlFile.canRead()){
						ok = false;
						_log.error(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(null)).append("The file: '").append(xmlFile.getName()).append("' can not be read"));
						
						// Si es el primer archivo hay que notificarselo al flex, el resto vale con notificarlo por log
						if (f == 0)
							ErrorRaiser.throwIfFalse(xmlFile.canRead(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, ImportMgrLocalServiceUtil.buildUserPrefixLog(null) + "The file: '" + xmlFile.getName() + "' can not be read");					
					}else{
						final String importId = PortalUUIDUtil.newUUID();						
						final Calendar xmlT0  = Calendar.getInstance();
						try{									
							final Document xml = SAXReaderUtil.read(xmlFile);								
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
								
								// Anotamos en base de datos el registro (importation)
								if (this.resultToBBDD){	
									ImportMgrLocalServiceUtil.insertUserImport(importId, groupId, xmlFile, actualIp);
								}								
								// Una vez insertado un registro en la tabla importation devolvemos el control a flex
								if (f == 0){
									createResultDocument(importId, xmlFile, xmlT0.getTime(), actualIp);
								}
								
								// Obtenemos los usuarios a importar del xml actual
								final List<Node> users = xml.getRootElement().selectNodes("/us/u");
								ErrorRaiser.throwIfFalse(null != users && users.size() != 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + "No users found in the xml file: " + xmlFile.getAbsolutePath());
								
								// Mientras haya usuarios y no se haya cancelado la importación desde flex
								for (int u = 0; u < users.size() && continueImporting(importId); u++){	
									
									if(_log.isDebugEnabled())
										_log.debug(new StringBuilder(XmlioKeys.PREFIX_USER_LOG).append(" user: ").append(u));
									
									Node user = users.get(u);
									
									// Llamada dinámica para evitar referencias cruzadas									
									Class<?> comObject = Class.forName("com.protecmedia.iter.user.service.IterUserMngServiceUtil");									
					
									Method method = comObject.getMethod("importUser", java.lang.Object.class, java.lang.Object.class,
										                                              java.lang.Object.class, java.lang.Object.class,
										                                              java.lang.Object.class, java.lang.Object.class,
										                                              java.io.File.class,     boolean.class, 
										                                              boolean.class,          java.lang.Object.class);
																		
									ErrorRaiser.throwIfNull(method, ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + "Mehod IterUserMngServiceUtil.importUser not found");
									
									Document xmlUserImportResult = SAXReaderUtil.read("<d/>");
									
									// Parametros del metodo
									Object[] methodParams = new Object[10];
									methodParams[0] = xmlUserImportResult;
									methodParams[1] = userProfile;
									methodParams[2] = actualIp;
									methodParams[3] = user;
									methodParams[4] = this.groupId;
									methodParams[5] = this.defaultUserId;
									methodParams[6] = this.path;
									methodParams[7] = this.passwordInMD5;
									methodParams[8] = this.deleteUsers;		
									methodParams[9] = this.userProfileNames;
									
									String userName  = null;
									final long userImportT0 = Calendar.getInstance().getTimeInMillis();
									
									try{
										// Importamos el usuario
										method.invoke(comObject, methodParams);
										userName = XMLHelper.getTextValueOf(user, "./f/usrname", null);																				
									}catch(Exception e){	
			                            _log.error(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + " user not imported" + (Validator.isNull(userName) ? "" : ": '" + userName + "'"));
									}
									
									if(_log.isDebugEnabled())
										_log.debug(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + "Total time importing the user '" + userName + "': " + (Calendar.getInstance().getTimeInMillis() -userImportT0) + "ms" );
									
									// Vemos si la importacion del usuario ha fallado y contabilizamos
									try{
										if (Validator.isNotNull(XMLHelper.getTextValueOf(xmlUserImportResult, "/d/@errorCode", null))){
											ok = false;	
											if (this.resultToBBDD)
												ImportMgrLocalServiceUtil.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_USERS, importId, "0", "1");
										}else{		
											if (this.resultToBBDD)
												ImportMgrLocalServiceUtil.updateOkAndKoImportCount(XmlioKeys.IMPORT_TYPE_USERS, importId, "1", "0");						
										}	
									}catch(Exception e){
										_log.debug(new StringBuilder("XML received for import: \n").append(xmlUserImportResult.asXML()));
										_log.error(e);
										_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId)).append(" Error updating ok and ko import count"));
									}																		
									
									// Anotamos en base de datos (importationdetail) el resultado de la operacion
									if (this.resultToBBDD){
										try{
											ImportMgrLocalServiceUtil.insertUserDetail(importId, groupId, xmlUserImportResult.selectSingleNode("/d"));
										}catch(Exception e){
											_log.debug(new StringBuilder("XML received for importdetail: \n").append(xmlUserImportResult.asXML()));
											_log.error(e);
											_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId)).append(" Error inserting import detail"));
										}
									}
								}	
							}catch(DocumentException e){
								ok = false; 					
								_log.error(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + "xml malformed").append(null != xmlFile ? ": " + xmlFile.getName()   : "").toString());
								this.excepcionList.add(e);
							}catch(Exception e){
								ok = false;						
								_log.error(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + "Error with the file").append(null != xmlFile ? ": " + xmlFile.getName()   : "").toString());
								this.excepcionList.add(e);
							}finally{								
								// Modificamos la fecha fin de la importacion
								if (null != importId && this.resultToBBDD){
									try{
										ImportMgrLocalServiceUtil.updateImportationFinishTime(importId);
									}catch(Exception e){
										_log.error(e);
										_log.debug(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + " Error updating the import finisht time");
									}
								}	
															
								// Si se ha procesado correctamente el archivo lo movemos
								if (ok && null != xmlFile && null != this.backupPath){					
									try{
										ImportMgrLocalServiceUtil.moveImportedFiles(xmlFile, this.backupPath);
									}catch (ServiceError e){
										_log.error(e);
									}
								}else{
									_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId)).append("Not moving files because: ")
									           .append(!ok ? "not total users were imported " : "")
									           .append(null == xmlFile ? "file is null" : "")
									           .append(null == this.backupPath ? "backupDirectory is null" : "").toString());							           
								}						
							}
						
						if(_log.isDebugEnabled())
							_log.debug(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId) + "Total  ImportUser.run() time elapsed: " + (Calendar.getInstance().getTimeInMillis() - xmlT0.getTimeInMillis() ) + " ms");
					}
				}catch(Exception t){
					_log.error(t);
					_log.error(ImportMgrLocalServiceUtil.buildUserPrefixLog(null) + "unexpected error working with '" + xmlFile.getName() + "'");
					
					// Solo capturamos el error para el primer xml
					if (f == 0){
						this.excepcionList.add(t);
					}
				}
			}
		}catch(Exception e){
			_log.error(e);
			_log.error(ImportMgrLocalServiceUtil.buildUserPrefixLog(null) + "unexpected error in ImportUserThread");
			this.excepcionList.add(e);
		}finally{
			if(_log.isDebugEnabled())
				_log.debug(ImportMgrLocalServiceUtil.buildUserPrefixLog(null) + "Total ImportUserThread.run time elapsed: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms");
		}
	}
	
	// Modifica la variable xmlResult que se pasa por callback para devolver el control a flex y que siga la importación en segundo plano.
	private void createResultDocument(String importId, File xmlFile, Date startTime, String serverIp){		
		// Si this.xmlResult es null es porque no es el primer xml de la importacion, no hay que hacer nada con el.
		if (Validator.isNotNull(this.xmlResult)){
			
			Element root = this.xmlResult.getRootElement();
			// Este atributo le dice al padre del hilo (ImportMgrLocalServiceUtil) que puede devolver el control a flex.  
			root.addAttribute("toFlex", "1");
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(ImportMgrLocalServiceUtil.buildUserPrefixLog(importId)).append("'toFlex' set in ImportUserThread"));
			
			if (Validator.isNotNull(importId)){
				Element row = root.addElement("row");
				row.addAttribute("id", importId);
				row.addAttribute("fn", xmlFile.getAbsolutePath().replaceAll("\\\\", "/"));
				row.addAttribute("st", sDFToDB.format(startTime));
				row.addAttribute("host", serverIp);			
			}
		}							
	}
	
	
	// Controla si la importación actual se quiere detener desde el flex
	private boolean continueImporting(String userImportId){		
		List<String> userImportationsToCancel = ImportMgrLocalServiceImpl.getUserImportationsToCancel(); 
		
		if (Validator.isNotNull(userImportationsToCancel) && userImportationsToCancel.contains(userImportId)){
			ImportMgrLocalServiceImpl.removeUserImportationToCancel(userImportId); 
			return false;
		}else{
			return true;
		}
	}	
	
	/* Crea una lista con los nombres de los campos (servirá para comprobar que en el xml no hay campos que no estén en el formulario de registro)
	   Se calcula aquí para que no se haga en cada importación de usuarios */
	private List<String> getNamesFromUserProfiles(List<Object> userprofiles) throws ServiceError
	{
		_log.trace("In getNamesFromUserProfiles");
		
		ErrorRaiser.throwIfFalse(null != userProfile && userProfile.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No user profiles given");
		
		List<String> names = new ArrayList<String>();
		
		for (int i = 0; i < userProfile.size(); i++)			
		{
			// El segundo campo es el nombre
			names.add((((Object[])userprofiles.get(i))[1]).toString());
		}
		return names;
	}
}