package com.protecmedia.iter.xmlio.service.util;


public class XmlioKeys {

	public static final String PREFIX_USER_LOG    = "-USERS_IMPORTATION- ";
	public static final String PREFIX_ARTICLE_LOG = "-ARTICLES_IMPORTATION- ";
	
	// Listado principal de importaciones
	public static final String DEFAULT_NUMBER_LIMIT                   = "45";

	// Tipos de importaciones
	public static final String IMPORT_TYPE_USERS    = "USERS";		// Importacion de usuarios
	public static final String IMPORT_TYPE_ARTICLES = "ARTICLES";	// Importacion de articulos

	
	// Tablas
	public static final String USERS_TABLE_IMPORT            = "importation";
	public static final String ARTICLES_TABLE_IMPORT         = "importarticles";
	public static final String USERS_TABLE_DETAILS_IMPORT    = "importationdetail";
	public static final String ARTICLES_TABLE_DETAILS_IMPORT = "importartdetails";	
	
	// Codigos de resultado para el detalle de una importacion
	public final static String DETAIL_ERROR_CODE_IMPORT_ZIP_EXCEPTION     = "ERROR IN ZIP" ; // Errores al descomprimir un fichero zip
	public final static String DETAIL_ERROR_CODE_IMPORT_XML_EXCEPTION     = "ERROR IN XML" ; // Errores al abrir un fichero XML
	public final static String DETAIL_ERROR_CODE_IMPORT_MALFORMED_XML     = "MALFORMED XML"; // Errores de formato de ficheros XML
	public static final String DETAIL_ERROR_CODE_IMPORT_SQL		          = "SQL"          ; // Error en consulta
	public static final String DETAIL_ERROR_CODE_IMPORT_FORMAT            = "FORMAT"       ; // Error de formato
	public static final String DETAIL_ERROR_CODE_IMPORT_BINARY            = "BINARY"       ; // Error relacionado con binarios/adjuntos
	public static final String DETAIL_ERROR_CODE_IMPORT_REPEATED          = "REPEATED"     ; // Elemento a importar ya lo estaba.
	public static final String DETAIL_ERROR_CODE_IMPORT_MANDATORY         = "MANDATORY"    ; // Falta campo obligatorio
	public static final String DETAIL_ERROR_CODE_IMPORT_UNEXPECTED        = "UNEXPECTED"   ; // Error inesperado
	public static final String DETAIL_ERROR_CODE_IMPORT_DATE_FORMAT       = "DATE_FORMAT"  ; // Error de formato en una fecha
	public static final String DETAIL_ERROR_CODE_IMPORT_NUMBER_FORMAT     = "NUMBER_FORMAT"; // Error de formato en un número		
	
	// Usuarios
	public static final String DETAIL_ERROR_PASSWORD_NOT_IN_MD5			  = "XYZ_INVALID_MD5_PASSWORD_ZYX";	// La contraseña no es un MD5
	
	// Artículos
		// Codigos de error para el listado de importaciones
		public static final String DETAIL_WARNING_CATEGORIES              = "WARNING-CATEGORIES"; 	// El artículo no tiene metadata(category) asociado
		public static final String DETAIL_WARNING_SUSCRIPTIONS            = "WARNING-SUSCRIPTIONS"; // El artículo no tiene suscripciones
		public static final String DETAIL_ERROR_CODE_DATE_CONTROL         = "DATE_CONTROL" ;      	// Fecha fuera del rango de control aceptado
		public static final String DETAIL_ERROR_CODE_IMPORT_TEMPLATE      = "TEMPLATE"     ;
		public static final String DETAIL_ERROR_CODE_IMPORT_STRUCTURE     = "STRUCTURE"    ;
		public static final String DETAIL_ERROR_CODE_IMPORT_SECTIONS      = "SECTIONS"     ;
		public static final String DETAIL_ERROR_CODE_IMPORT_LAYOUT        = "LAYOUT"       ;	
		public static final String DETAIL_ERROR_CODE_IMPORT_CATEGORIES    = "CATEGORIES"   ;	 
		public static final String DETAIL_ERROR_CODE_IMPORT_SUBSCRIPTIONS = "SUBSCRIPTIONS"; 
		public static final String DETAIL_ERROR_CODE_IMPORT_BINARIES      = "BINARIES"; 
		public static final String DETAIL_ERROR_CODE_IMPORT_NOT_EXISTS    = "NOT_EXISTS"   ; // El artículo no existe (se ha solicitado borrar)	
		public static final String DETAIL_ERROR_CODE_DELETED              = "DELETED"      ; // El artículo ha sido borrado por petición del usuario
		public static final String DETAIL_ERROR_CODE_LINK				  = "LINK";			 // Vínculo relacionado incorrecto
		public static final String DETAIL_ERROR_IMPORT_COLISSION  		  = "IMPORT-COLLISION";	 
		
		// Descripciones de error para el listado de importaciones
			public static final String DESC_ERR_ARTICLE_DELETE_BY_USER  = "XYZ_ARTICLE_DELETE_BY_USER_ZYX"; 													// "Article deleted by user"
			public static final String DESC_ERR_INVALID_URLTITLE        = "XYZ_ATTRIBUTE_URLTITLE_IS_EMPTY_ORNULL_ZYX"; 										// "Attribute urlTitle is empty or does not exists
			public static final String DESC_ERR_ARTICLE_NOT_IN_DDBB     = "XYZ_THE_ARTICLE_IS_NOT_IN_THE_DATA_BASE_ZYX";	  									// "The article is not in the database"
			public static final String DESC_ERR_SQL_ERROR				= "XYZ_ERROR_IN_SQL_ZYX";				  												// "Error in sql: "
			public static final String DESC_ERR_READING_PREVIEW_FILE    = "XYZ_ERROR_READING_PREVIEW_FILE_ZYX";	  												// "Error reading preview file"
			public static final String DESC_ERR_INVALID_ATTRIBUTE_PATH  = "XYZ_ATTRIBUTE_PATH_IS_EMPTY_OR_DOES_NOT_EXISTS_ZYX";  								// "Attribute path is empty or does not exists"
			public static final String DESC_ERR_FILE_CAN_NOT_BE_READ	= "XYZ_FILE_CAN_NOT_BE_READ_ZYX";	  													// "File can not be read/found"
			public static final String DESC_ERR_READING_FILE			= "XYZ_ERROR_READING_ZYX";			  													// "Error reading"
			public static final String DESC_ERR_INVALID_ARTICLEID       = "XYZ_ATTRIBUTE_ARTICLEID_IS_EMPTY_OR_DOES_NOT_EXISTS_OR_BIGGER_THAN_75_CHARACTERS_ZYX"; // "Attribute articleid is empty or does not exists"
			public static final String DESC_ERR_INVALID_URLTITLE2       = "XYZ_ATTRIBUTE_URLTITLE_IS_EMPTY_OR_BIGGER_THAN_150_CHARACTERS_ZYX";       			// "Attribute urlTitle is empty or bigger than 150 characters"
			public static final String DESC_ERR_ARTICLE_IN_DDBB         = "XYZ_THE_ARTICLE_IS_ALREADY_IN_THE_DATA_BASE_ZYX";         							// "The article is already in the database"
			public static final String DESC_ERR_INVALID_TITLE 			= "XYZ_ATTRIBUTE_TITLE_IS_EMPTY_OR_BIGGER_THAN_300_CHARACTERS_ZYX"; 		  			// "Attribute title is empty or bigger than 300 characters"
			public static final String DESC_ERR_ARTICLE_CONTENT         = "XYZ_ARTICLE_CONTENT_MISSING_ZYX";         											// "Article content missing"
			public static final String DESC_ERR_LEGACYURL               = "XYZ_ATTRIBUTE_LEGACYURL_IS_BIGGER_THAN_255_CHARACTERS_ZYX";               			// "Attribute legacyUrl is bigger than 255 characters"
			public static final String DESC_ERR_XSL                     = "XYZ_ERROR_WITH_THE_XSL_TRANSFORMATION_ZYX";                     						// Error with the xsl transformation
			public static final String DESC_ERR_IMPORTING_ARTICLE       = "XYZ_ERROR_IMPORTING_THE_ARTICLE_ZYX"; 	  											// "Error importing the article"
		    public static final String DESC_ERR_NORMALIZING_URLTITLE    = "XYZ_ERROR_NORMALIZING_THE_URLTITLE_ZYX";    										    // Error normalizing urlTitle
			public static final String DESC_ERR_LEGACYURL_EXISTS        = "XYZ_THE_LEGACYURL_ALREADY_EXISTS_ZYX";        										// The legacyurl already exists
			public static final String DESC_ERR_IMG_NOT_FOUNT_TO_SIZE   = "XYZ_THE_IMAGE_COULD_NOT_BE_FOUN_IN_THE_JOURNAL.CONTENT_TO_UPDATE_ITS_SIZE_ZYX";   	// The image could not be found in the journal.content to update its size
			public static final String DESC_ERR_EMPTY_DATE              = "XYZ_DATE_IS_EMPTY_ZYX";              												// Date is empty
			public static final String DESC_ERR_INVALID_DATE            = "XYZ_INVALID_DATE_FORMAT_FROM_ZYX";            										// "Invalid date format from"
			public static final String DESC_ERR_INVALID_START_VALIDITY  = "XYZ_START_VALIDITY_IS_BEFORE_THE_ARTICLE_CREATE_DATE_ZYX";  							// Start validity is before the article create date
			public static final String DESC_ERR_INVALID_START_VALIDITY2 = "XYZ_FINISH_VALIDITY_IS_BEFORE_ARTICLE_START_VALIDITY_ZYX"; 							// El finl de vigencia del artículo es anterior al inicio de vigencia del artículo
			public static final String DESC_ERR_JA_CREATED_DATE         = "XYZ_JOURNAL_ARTICLE_CREATED_DATE_IS_BEFORE_THAN_IMPORTATION_START_ZYX";         		// Journal article created date is before than importation start
			public static final String DESC_ERR_JA_MODIFIED_DATE        = "XYZ_JOURNAL_ARTICLE_MODIFIED_DATE_IS_BEFORE_THAN_IMPORTATION_START_ZYX";        		// Journal article modified date is before than importation start
			public static final String DESC_ERR_JA_CREATED_DATE2        = "XYZ_JOURNAL_ARTICLE_CREATED_DATE_IS_AFTER_THAN_IMPORTATION_FINISH_ZYX";        		// Journal article created date is after than importation finish
			public static final String DESC_ERR_JA_MODIFIED_DATE2       = "XYZ_JOURNAL_ARTICLE_MODIFIED_DATE_IS_AFTER_THAN_IMPORTATION_FINISH_ZYX";       		// Journal article modified date is after than importation finish
			public static final String DESC_ERR_ART_MODIFIED_DATE       = "XYZ_JOURNAL_ARTICLE_MODIFIED_DATE_IS_BEFORE_THAN_JOURNAL_ARTICLE_CREATE_DATE_ZYX";   // Journal article modified date is before than journal article created date
			public static final String DESC_ERR_NO_DEFAULT_SECTION      = "XYZ_THE_ARTICLE_HAS_NO_DEFACULT_SECTION_ZYX";      									// The article has no default section
			public static final String DESC_ERR_MORE_ONE_DEF_SECTION    = "XYZ_MORE_THAN_ONE_DEFAULT_SECTION_ZYX";    											// More than one default section
			public static final String DESC_ERR_INVALID_QUALIFICATION   = "XYZ_THE_QUALIFICATION_GIVEN_IS_NULL_OR_EMPTY_ZYX";   								// The qualification given is null or empty
			public static final String DESC_ERR_INVALID_URL             = "XYZ_THE_LAYOUT_URL_GIVEN_IS_NULL_OR_EMPTY_ZYX";             						    // The layout (attribute url) given is null or empty
			public static final String DESC_ERR_INVALID_PAGETEMPLATE    = "XYZ_PAGETEMPLATE_GIVEN_NOT_FOUND_OR_THERE_ARE_MORE_THAN_ONE_WITH_THIS_NAME_ZYX";     // No pagetemplate found or there are more than one with this name
			public static final String DESC_ERR_INVALID_QUALIFICATION2  = "XYZ_THE_QUALIFICATION_DOES_NOT_EXISTS_ZYX";  										// The qualification does not exits
			public static final String DESC_ERR_INVALID_LAYOUT          = "XYZ_THERE_IS_NOT_A_LAYOUT_WITH_THIS_FRIENDLY_URL_ZYX";          						// There is not a layout with this friendly url
			public static final String DESC_ERR_INVALID_VOCABULAY       = "XYZ_THIS_VOCABULARY_DOES_NOT_EXISTS_ZYX";       									    // This vocabulary does not exists
			public static final String DESC_ERR_NO_METADATA             = "XYZ_ARTICLE_HAS_NO_METADATA_ZYX";             										// Article has no metadata (category) assigned
			public static final String DESC_ERR_INVALID_CATEGORY        = "XYZ_THIS_CATEGORY_DOES_NOT_EXIST_ZYX";        										// This category does not exist
			public static final String DESC_ERR_INVALID_SUSCRIPTION     = "XYZ_THERE_IS_NOT_A_SUSCRIPTION_WITH_THE_NAME_ZYX";     								// There is not a suscription with the name
			public static final String DESC_ERR_ART_WITH_NO_SUSCRIPTIONS= "XYZ_THE_ARTICLE_HAS_NO_SUSCRIPTIONS_ZYX";											// The article has no suscriptions
			public static final String DESC_ERR_INVALID_NUMBER          = "XYZ_INCORRECT_NUMBER_FORMAT_ZYX";          											// Incorrect number format
			public static final String DESC_ERR_REPEATED_ARTICLE        = "XYZ_ARTICLE_REPEATED_ZYX";        													// Article repeated
			public static final String DESC_ERR_FILE_ALLREADY_EXISTS    = "XYZ_FILE_ALREADY_EXISTS_ZYX";    													// File already exists
			public static final String DESC_ERR_START_LAYOUT            = "XYZ_LAYOUT_DOES_NOT_START_WITH_SLASH_ZYX";            								// Layout does not start with '/'
			public static final String DESC_ERR_INVALID_URL_LAYOUT      = "XYZ_INVALID_URL_FOR_LAYOUT_ZYX";      												// Invalid url for layout
			public static final String DESC_ERR_CREATING_LAYOUT         = "XYZ_ERROR_CREATING_THE_LAYOUT_ZYX";         											// Error creating the layout
			public static final String DESC_ERR_PREVIEW_MAX_FILE_NAME   = "XYZ_PREVIEW_FILE_NAME_TOO_LONG_ZYX";
			public static final String DESC_ERR_MAX_FILE_NAME           = "XYZ_FILE_NAME_TOO_LONG_ZYX";
			public static final String DESC_ERR_MAX_SECTION_LENGTH      = "XYZ_THE_SECTION_IS_MORE_THAN_255_CHARACTERS_LENGTH_ZYX";      						// The section length is more than 255 characters			
			public static final String DESC_ERR_ARTICLEID_FORMAT	    = "XYZ_CHARACTER_NOT_ALLOWED_IN_ARTICLEID_ZYX";      									// The Articleid cannot contain a "-" character
			public static final String DESC_ERR_LAYOUT_REPEATED_IN_ARTICLE         = "XYZ_LAYOUT_REPEATED_IN_THE_SAME_ARTICLE_ZYX";    							// Misma sección definida para un artículo
			public static final String DESC_ERR_SUBSCRIPTION_REPEATED_IN_ARTICLE   = "XYZ_SUSCRIPTION_REPEATED_IN_THE_SAME_ARTICLE_ZYX";						// Suscripción repetida para el mismo artículo
			public static final String DESC_ERR_SUBSCRIPTION_REPEATED_IN_FILEENTRY = "XYZ_SUSCRIPTION_REPEATED_IN_THE_SAME_FILEENTRY_ZYX";						// Suscripción repetida para el mismo adjunto/fileentry
			public static final String DESC_ERR_IMPORT_COLLISION                   = "XYZ_IMPORT_COLLISION_ZYX";												// Dos importaciones difrentes van a crear el mismo registro a la vez
			public static final String DESC_ERR_IMAGE_CORRUPTED                    = "XYZ_CORRUPTED_IMAGE_ZYX";	
			public static final String DESC_ERR_IMAGE_REPEATED					   = "XYZ_IMG_REPEATED_IN_THE_SAME_ARTICLE_ZYX";
			public static final String DESC_ERR_IMAGE_FORMAT_NOT_SUPPORTED		   = "XYZ_IMAGE_FORMAT_NOT_SUPPORTED_ZYX";
			public static final String DESC_ERR_INVALID_COMPONENT_TAG              = "XYZ_DESC_ERR_INVALID_COMPONENT_TAG_ZYX";
			public static final String DESC_ERR_INVALID_COMPONENT_TAG_2            = "XYZ_DESC_ERR_INVALID_COMPONENT_TAG_2_ZYX";
			public static final String DESC_ERR_INVALID_GROUPS_VIRTUALHOST         = "XYZ_INVALID_GROUPS_VIRTUALHOST_ZYX";										// El virtualhost indicado en el atributo groups no existe
			public static final String DESC_ERR_INVALID_GROUPS_DELEGATION          = "XYZ_INVALID_GROUPS_DELEGATION_ZYX";										// El virtualhost indicado en el atributo groups no pertenece a la misma delegación
	// Artículos
	
	// Extensiones
	public static final String XML_EXTENSION = ".xml";
	public static final String ZIP_EXTENSION = ".zip";
	
	// Parámetro utilizado en la xsl createContent.xsl
	public static final String XSL_PARAM_GLOBAL_GRP_ID = "globalgroupid";
	
	// Errores en la importación para Flex
	public static final String XYZ_INCORRECT_SQL_ZYX                              		= "XYZ_INCORRECT_SQL_ZYX";
	public static final String XYZ_NO_ARTICLES_TO_DELETE_ZYX                      		= "XYZ_NO_ARTICLES_TO_DELETE_ZYX";
	public static final String XYZ_IMPORT_PATH_SAME_AS_BACKUP_PATH_ZYX           		= "XYZ_IMPORT_PATH_SAME_AS_BACKUP_PATH_ZYX";
	public static final String XYZ_SELECT_TO_DELETE_ARTICLES_WITH_NO_IDS_ZYX      		= "XYZ_SELECT_TO_DELETE_ARTICLES_WITH_NO_IDS_ZYX"; 
	public static final String XYZ_IMPORT_START_DATE_AFTER_FINISH_IMPORT_DATE_ZYX 		= "XYZ_IMPORT_START_DATE_AFTER_FINISH_IMPORT_DATE_ZYX";
	public static final String XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_INVALID_ZYX   		= "XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_INVALID_ZYX";
	public static final String XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_NO_RESULT_ZYX 		= "XYZ_IMPORT_SELECT_TO_DELETE_ARTICLES_NO_RESULT_ZYX";
	public static final String XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX                    		= "XYZ_IMPORT_NO_XML_TO_IMPORT_ZYX";
	public static final String XYZ_IMPORT_ALREADY_FINISHED_ZYX                    		= "XYZ_IMPORT_ALREADY_FINISHED_ZYX";
	public static final String XYZ_IMPORT_FILE_NOT_FOUND_ZYX                      		= "XYZ_IMPORT_FILE_NOT_FOUND_ZYX";
	public static final String XYZ_IMPORT_FOLDER_NOT_FOUND_ZYX                    		= "XYZ_IMPORT_FOLDER_NOT_FOUND_ZYX";                                                     
	public static final String XYZ_IMPORT_BACKUP_FOLDER_NOT_FOUND_ZYX            		= "XYZ_IMPORT_BACKUP_FOLDER_NOT_FOUND_ZYX";
	public static final String XYZ_IMPORT_IO_EXCEPTION_ZYX                        		= "XYZ_IMPORT_IO_EXCEPTION_ZYX";
	public static final String XYZ_IMPORT_ERROR_WITH_THE_ZIP_ZYX                  		= "XYZ_IMPORT_ERROR_WITH_THE_ZIP_ZYX";
	public static final String XYZ_IMPORTATION_NOT_STARTED_NECESARY_PUBLISH_TO_LIVE_ZYX = "XYZ_IMPORTATION_NOT_STARTED_NECESARY_PUBLISH_TO_LIVE_ZYX";
	public static final String XYZ_ARTICLEID_IS_NUMERIC_ZYX 							= "XYZ_ARTICLEID_IS_NUMERIC_ZYX";
	
	
	// Para llamadas Json
	public static final String SERVICE_CLASS_NAME  = "serviceClassName";	
	public static final String SERVICE_METHOD_NAME = "serviceMethodName";
	public static final String SERVICE_PARAMETERS  = "serviceParameters";
	
	// Publicaciones
	public static final String XYZ_E_GROUP_NOT_PUBLISHED_IN_LIVE_ZYX				 = "XYZ_E_GROUP_NOT_PUBLISHED_IN_LIVE_ZYX";
	public static final String XYZ_E_SECTION_NOT_PUBLISHED_IN_LIVE_ZYX               = "XYZ_E_SECTION_NOT_PUBLISHED_IN_LIVE_ZYX";
	public static final String XYZ_E_ARTICLE_NOT_PUBLISHED_IN_LIVE_ZYX               = "XYZ_E_ARTICLE_NOT_PUBLISHED_IN_LIVE_ZYX";
	public static final String XYZ_E_ASSET_CATEGORY_NOT_PUBLISHED_IN_LIVE_ZYX        = "XYZ_E_ASSET_CATEGORY_NOT_PUBLISHED_ZYX";
	public static final String XYZ_E_ASSET_DESIGN_TEMPLATE_NOT_PUBLISHED_IN_LIVE_ZYX = "XYZ_E_ASSET_DESIGN_TEMPLATE_NOT_PUBLISHED_IN_LIVE_ZYX";
	public static final String XYZ_E_MIXED_PUBLICATION_MODELS_ZYX	                 = "XYZ_E_MIXED_PUBLICATION_MODELS_ZYX";
}