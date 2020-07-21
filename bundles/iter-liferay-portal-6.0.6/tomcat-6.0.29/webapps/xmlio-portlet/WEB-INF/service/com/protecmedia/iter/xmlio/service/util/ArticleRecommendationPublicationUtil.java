package com.protecmedia.iter.xmlio.service.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.restapi.resource.article.RestApiRecommendationsUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

public class ArticleRecommendationPublicationUtil
{
	private static final String SQL_GET_RECOMMENDATION_CONFIG = "SELECT id, groupid, name, description, config, UNIX_TIMESTAMP(publicationdate) publicationdate, UNIX_TIMESTAMP(deletedate) deletedate FROM suggestions_config WHERE id = '%s'";
	private static final String SQL_GET_RECOMMENDATION_DETAILED_CONFIGS = "SELECT id, groupid, name, description, config, UNIX_TIMESTAMP(publicationdate) publicationdate, UNIX_TIMESTAMP(deletedate) deletedate FROM suggestions_config WHERE groupId = %d";
	private static final String SQL_UPDATE_PUBLICATIONDATE = "UPDATE suggestions_config SET publicationdate = CURRENT_TIMESTAMP WHERE id in ('%s')";
	private static final String SQL_DELETE_CONFIGS = "DELETE FROM suggestions_config WHERE id in ('%s')";
	
	/**
	 * Publica la configuración indicada (O todas las pendientes) al entorno Live.
	 * @param groupId  El identificador del grupo.
	 * @param configId El identificador (opcional) de la configuración a publicar.
	 * @throws ServiceError si ocurre un error al procesar las configuraciones o en la respuesta del Live.
	 * @throws SystemException si ocurre un error al procesar las configuraciones, crear el documento de exportación o en la respuesta del Live.
	 */
	public static void publishRecommendationConfig(String groupId, String configId) throws ServiceError, SystemException
	{
		if (PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			// Obtiene las configuraciones a publicar
			List<String> deletes = new ArrayList<String>();
			List<String> updates = new ArrayList<String>();
			Document exportData = createExportDocument(groupId, configId, updates, deletes);
			
			// Si hay configuraciones, las publica
			if (updates.size() > 0 || deletes.size() > 0)
			{
				// Manda la publicación al Live
				IterHttpClient ihttpc = JSONUtil.createHttpLiveConnection("com.protecmedia.iter.xmlio.service.util.ArticleRecommendationPublicationUtil", "1", "importData", new String[]{exportData.asXML()});
				String response = ihttpc.connect();
				if (HttpServletResponse.SC_OK == ihttpc.getResponseStatus())
				{
					// Comprueba si hubo errores
					JSONUtil.checkHttpLiveConnectionError(response);
					
					try
					{
						// Elimina del PREVIEW los borrados del LIVE
						PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_DELETE_CONFIGS, StringUtil.merge(deletes, "', '")));
						
						// Actualiza la fecha de publicación
						PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_PUBLICATIONDATE, StringUtil.merge(updates, "', '")));
					}
					catch(Throwable th)
					{
						throw new SystemException(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th);
					}
				}
				else
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Live respond with " + ihttpc.getResponseStatus());
				}
			}
		}
	}
	
	/**
	 * Crea el documento de configuraciones a publicar.
	 * @param groupId  El identificador del grupo.
	 * @param configId El identificador de la configuración (opcional).
	 * @param updates  Array que se rellena con las configuraciones a crear / actualizar.
	 * @param deletes  Array que se rellena con las configuraciones a eliminar.
	 * @return {@code Document} con las configuraciones a publicar.
	 * @throws ServiceError    Si no se informa o no se puede recuperar el grupo indicado o si ocurre un error al traducir los IDs.
	 * @throws SystemException Si no se pueden recuperar las configuraciones.
	 */
	private static Document createExportDocument(String groupId, String configId, List<String> updates, List<String> deletes) throws ServiceError, SystemException
	{
		// Valida la entrada
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Obtiene el nombre del grupo
		long scopeGroupId = Long.valueOf(groupId);
		String groupName = null;
		try
		{
			groupName = GroupLocalServiceUtil.getGroup(scopeGroupId).getName();
		}
		catch (Throwable th) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		
		// Crea el documento de publicación
		Document exportData = SAXReaderUtil.createDocument();
		Element exportRoot = exportData.addElement("configs");
		exportRoot.addAttribute("groupName", groupName);
		
		// Obtiene las configuraciones a publicar
		Document configs = null;
		try
		{
			configs = PortalLocalServiceUtil.executeQueryAsDom(
				Validator.isNotNull(configId) ? String.format(SQL_GET_RECOMMENDATION_CONFIG, configId) : String.format(SQL_GET_RECOMMENDATION_DETAILED_CONFIGS, scopeGroupId),
				new String[]{"config"}
			);
		}
		catch (Throwable th)
		{
			throw new SystemException(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th);
		}
		
		// Añade las configuraciones al documento
		List<Node> configToPublish = configs.selectNodes("/rs/row");
		if (configToPublish.size() > 0)
		{
			for (int i = 0; i < configToPublish.size(); i++)
			{
				Node config = configToPublish.get(i);
				if (XMLHelper.getLongValueOf(config, "@deletedate", 0L) > 0L)
				{
					deletes.add(XMLHelper.getStringValueOf(config, "@id"));
				}
				else
				{
					JSONObject jsonConfig = new JSONObject(XMLHelper.getStringValueOf(config, "config"));
					translateIds(scopeGroupId, jsonConfig, true);
					config.selectSingleNode("config").setText(jsonConfig.toString());
					updates.add(XMLHelper.getStringValueOf(config, "@id"));
				}
				exportRoot.add(config.detach());
			}
		}
		
		return exportData;
	}
	
	/**
	 * <p>Recorre todos los orígenes de todos los perfiles buscando las configuraciónes de artículos promocionados.</p>
	 * <p>Traduce los IDs de todas las secciones, metadatos y calificaciones que encuentre.</p>
	 * @param groupId       El identificador del grupo.
	 * @param jsonConfig    El {@code JSON} con la configuración a analizar.
	 * @param export        {@code boolean} que indica si se quiere traducir del Preview al Global ({@code true}) o del Global al Live ({@code false}).
	 * @throws ServiceError Si la configuración no tiene perfiles u orígenes o si ocurre un error al traducir algún ID.
	 */
	private static void translateIds(Long groupId, JSONObject jsonConfig, boolean export) throws ServiceError
	{
		// Valida los parámetros
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId) && groupId > 0 && Validator.isNotNull(jsonConfig), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recorre los perfiles
		JSONArray profiles = jsonConfig.getJSONArray("profiles");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(profiles), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		for (int i = 0; i < profiles.length(); i++)
		{
			// Busca la fuente "promocionados"
			JSONObject profile = profiles.getJSONObject(i);
			JSONArray sources = profile.getJSONArray("sources");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(sources), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			for (int j = 0; i < sources.length(); j++)
			{
				JSONObject source = sources.getJSONObject(j);
				if ("promoted".equals(source.getString("name")))
				{
					// Traduce las secciones
					translateSections(groupId, source.getJSONArray("sections"), export);
					
					// Traduce los metadatos
					translateMetadata(groupId, source.getJSONArray("metadata"), export);
					
					// Traduce las calificaciones
					translateQualifications(groupId, source.getJSONArray("qualifications"), export);
					
					break;
				}
			}
		}
	}

	/**
	 * Dada una lista de secciones, traduce todos sus IDs del Preview al Global o del Global al Live.
	 * @param groupId       El identificador del grupo.
	 * @param sections      {@code JSONArray} con los identificadores de las secciones a traducir.
	 * @param export        {@code boolean} que indica si se quiere traducir del Preview al Global ({@code true}) o del Global al Live ({@code false}).
	 * @throws ServiceError Si ocurre un error al traducir algún ID.
	 */
	private static void translateSections(Long groupId, JSONArray sections, boolean export) throws ServiceError
	{
		if (sections != null && sections.length() > 0)
		{
			for (int i = 0; i < sections.length(); i++)
			{
				try
				{
					if (export)
					{
						// Obtiene el layout
						Layout layoutPref = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(sections.getString(i), groupId);
						ErrorRaiser.throwIfFalse(Validator.isNotNull(layoutPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
						// Obtiene el id del Live
						Live liveLayoutPref = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_LAYOUT, String.valueOf(layoutPref.getPlid()));
						ErrorRaiser.throwIfFalse(Validator.isNotNull(liveLayoutPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
						// Lo actualiza
						sections.put(i, liveLayoutPref.getGlobalId());
					}
					else
					{
						// Obtiene el Id del Live
						Live liveLayoutPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, sections.getString(i));
						ErrorRaiser.throwIfFalse(Validator.isNotNull(liveLayoutPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
						// Obtiene el layout
						Layout layoutPref = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(liveLayoutPref.getLocalId()));
						ErrorRaiser.throwIfFalse(Validator.isNotNull(layoutPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
						// Lo actualiza
						sections.put(i, layoutPref.getUuid());
					}
				}
				catch (Throwable th)
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX, "Section: " + sections.getString(i), th.getStackTrace());
				}
			}
		}
	}

	/**
	 * Dada una lista de categorías, traduce todos sus IDs del Preview al Global o del Global al Live.
	 * @param groupId       El identificador del grupo.
	 * @param sections      {@code JSONArray} con los identificadores de las categorías a traducir.
	 * @param export        {@code boolean} que indica si se quiere traducir del Preview al Global ({@code true}) o del Global al Live ({@code false}).
	 * @throws ServiceError Si ocurre un error al traducir algún ID.
	 */
	private static void translateMetadata(Long groupId, JSONArray metadata, boolean export) throws ServiceError
	{
		if (metadata != null && metadata.length() > 0)
		{
			for (int i = 0; i < metadata.length(); i++)
			{
				try
				{
					if (export)
					{
						Live liveCategoryPref = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_CATEGORY, metadata.getString(i));
						if (liveCategoryPref != null)
						{
							metadata.put(i, liveCategoryPref.getGlobalId());
						}
						else
						{
							// Intentamos con el grupo global
							liveCategoryPref = LiveLocalServiceUtil.getLiveByLocalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_CATEGORY, metadata.getString(i));
							ErrorRaiser.throwIfFalse(Validator.isNotNull(liveCategoryPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
							metadata.put(i, liveCategoryPref.getGlobalId());
						}
					}
					else
					{
						Live liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_CATEGORY, metadata.getString(i));
						if(liveCategoryPref != null)
						{
							metadata.put(i, liveCategoryPref.getLocalId());
						}
						else
						{
							liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_CATEGORY, metadata.getString(i));
							ErrorRaiser.throwIfFalse(Validator.isNotNull(liveCategoryPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
							metadata.put(i, liveCategoryPref.getLocalId());
						}
					}
				}
				catch (Throwable th)
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX, "Category: " + metadata.getString(i), th.getStackTrace());
				}
			}
		}
	}

	/**
	 * Dada una lista de calificaciones, traduce todos sus IDs del Preview al Global o del Global al Live.
	 * @param groupId       El identificador del grupo.
	 * @param sections      {@code JSONArray} con los identificadores de las calificaciones a traducir.
	 * @param export        {@code boolean} que indica si se quiere traducir del Preview al Global ({@code true}) o del Global al Live ({@code false}).
	 * @throws ServiceError Si ocurre un error al traducir algún ID.
	 */
	private static void translateQualifications(Long groupId, JSONArray qualifications, boolean export) throws ServiceError
	{
		if (qualifications != null && qualifications.length() > 0)
		{
			for (int i = 0; i < qualifications.length(); i++)
			{
				try
				{
					Live liveQualificationPref = export
											   ? LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_QUALIFICATION, qualifications.getString(i))
											   : LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_QUALIFICATION, qualifications.getString(i));
					ErrorRaiser.throwIfFalse(Validator.isNotNull(liveQualificationPref), IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX);
					qualifications.put(i, export ? liveQualificationPref.getGlobalId() : liveQualificationPref.getLocalId());
				}
				catch (Throwable th)
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_RECOMMENDATION_DEPENDENCIES_ZYX, "Qualification: " + qualifications.getString(i), th.getStackTrace());
				}
			}
		}
	}
	
//	/**
//	 * Crea una conexión HTTP hacia el Tomcat del entorno LIVE para ejecutar el método {@code ArticleRecommendationPublicationUtil.importData()}
//	 * @param exportData       {@code Document} con las configuraciones a publicar en el Live.
//	 * @return                 {@code IterHttpClient} con la conexión hacia el Live.
//	 * @throws SystemException Si ocurre un error al crear la conexión.
//	 */
//	private static IterHttpClient createLiveconnection(Document exportData) throws SystemException
//	{
//		try
//		{
//			Document dom = JSONUtil.getLiveConfiguration();
//			String remoteIterServer = JSONUtil.getLiveConfRemoteIterServer(dom);
//			
//			String url = "http://" + remoteIterServer + "/base-portlet/live/endpoint";
//			JSONObject payload = new JSONObject();
//			JSONObject httpRpc = new JSONObject();
//			JSONObject invoke = new JSONObject();
//	
//			invoke.put("clsid", "com.protecmedia.iter.xmlio.service.util.ArticleRecommendationPublicationUtil");
//			invoke.put("dispid", 1);
//			invoke.put("methodName", "importData");
//			
//			JSONArray params = new JSONArray();
//			params.put(exportData.asXML());
//			invoke.put("params", params);
//	
//			httpRpc.put("invoke", invoke);
//			payload.put("http-rpc", httpRpc);
//			
//			return new IterHttpClient.Builder(IterHttpClient.Method.POST, url)
//			.connectionTimeout(20000)
//			.readTimeout(20000)
//			.header("Content-Type", "application/json")
//			.payLoad(payload.toString())
//			.build();
//		}
//		catch(Throwable th)
//		{
//			throw new SystemException(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th);
//		}
//	}
//	
//	/**
//	 * Comprueba si hay errores en la respuesta del Live y, si es así, lo convierte en una excepción.
//	 * @param endpointResponse {@code String} con la respuesta del Live.
//	 * @throws ServiceError    Si el Live informa de un error.
//	 * @throws SystemException Si la respuesta del Live no tiene el formato esperado.
//	 */
//	private static void checkError(String endpointResponse) throws ServiceError, SystemException
//	{
//		try
//		{
//			JSONObject jsonResponse = new JSONObject(endpointResponse).getJSONObject("response");
//			if (jsonResponse.has("error"))
//			{
//				JSONObject error = jsonResponse.getJSONObject("error");
//				ErrorRaiser.throwIfError(error.getString("code"), error.getString("msg"));
//			}
//		}
//		catch(ServiceError se)
//		{
//			throw se;
//		}
//		catch(Throwable th)
//		{
//			throw new SystemException(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th);
//		}
//	}
	
	/**
	 * Importa las configuraciones indicadas en el entorno Live.
	 * @param data {@code String} con el XML que contiene las configuraciones a importar.
	 * @throws ServiceError si ocurre un error al procesar la entrada, recuperar el grupo o traducir los IDs de secciones, metadatos y calificaciones.
	 * @throws SystemException si ocurre un error al actualizar o eliminar las configuraciones.
	 */
	public static void importData(String data) throws ServiceError, SystemException
	{
		if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			Document importDocument = null;
			try { importDocument = SAXReaderUtil.read(data); }
			catch(Throwable th) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
			
			// Recupera el grupo
			String groupName = XMLHelper.getStringValueOf(importDocument.getRootElement(), "@groupName");
			long groupId = 0;
			try
			{
				groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
			}
			catch (Throwable th) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
			ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Obtiene todos los elementos a eliminar
			List<Node> deletes = importDocument.selectNodes("/configs/row[@deletedate!='']");
			for (int i = 0; i < deletes.size(); i++)
			{
				Node config = deletes.get(i);
				String id = XMLHelper.getStringValueOf(config, "@id");
				RestApiRecommendationsUtil.removeRecommendationConfig(groupId, id);
			}
			
			// Obtiene todos los elementos a publicar
			List<Node> updates = importDocument.selectNodes("/configs/row[@deletedate='']");
			for (int i = 0; i < updates.size(); i++)
			{
				Node config = updates.get(i);
				// Lo transforma a JSON
				JSONObject jsonConfig = new JSONObject();
				jsonConfig.put("id", XMLHelper.getStringValueOf(config, "@id"));
				jsonConfig.put("groupid", groupId);
				jsonConfig.put("name", XMLHelper.getStringValueOf(config, "@name"));
				jsonConfig.put("description", XMLHelper.getStringValueOf(config, "@description"));
				jsonConfig.put("config", new JSONObject(XMLHelper.getStringValueOf(config, "config")));
				// Traduce los ids
				translateIds(groupId, jsonConfig.getJSONObject("config"), false);
				// Lo añade / actualiza
				RestApiRecommendationsUtil.setRecommendationConfig(jsonConfig.toString());
			}
		}
	}
}
