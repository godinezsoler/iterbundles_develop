package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.MinifyUtil;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class MinifierRSRC extends HttpServlet
{
	/** UID **/
	private static final long serialVersionUID = 6758244769438038385L;

	/** Logger **/
	private static Log _log = LogFactoryUtil.getLog(MinifierRSRC.class);
	
	/** Query para obtener lso recursos a minificar **/
	private final static String GET_RESOURCES_TO_MINIFY_BY_GROUPID = new StringBuffer(
			"SELECT theme_webresource.rsrcid, rsrccontent, rsrccontenttype              			\n").append(
			"FROM theme_webresource                                   								\n").append(
			"INNER JOIN layout_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid	\n").append(	
			"WHERE layout_webresource.groupid = %d                                       			\n").append(
			"  AND rsrccontenttype in ('text/css', 'text/javascript') 								\n").append(
			"  AND rsrccontentspec in ('owned', 'embeded')            								\n").append(
	        "  AND orphandate IS NULL 																\n").toString();
	
	/** Query para actualizar los recursos minificados **/
	private final static String UPDATE_WEBRESOURCE_BY_RSRCID = new StringBuffer(
			"UPDATE theme_webresource                      	\n").append(
			"SET rsrccontent = '%s',                       	\n").append(
			"    rsrcmd5 = CONVERT(MD5('%s') USING utf8)	\n").append(
			"WHERE rsrcid = '%s'                            \n").toString();
	
	/**
	 * Recupera el parametro groupId y obtiene todos los recursos de tipo 'text/css'
	 * y 'text/javascript' del grupo, los minimifica y los actualiza en la base de datos,
	 * invalidando la caché de los recursos minificado recalculando su MD5 usando la fecha
	 * y hora actual del sistema y dejandolos pendientes de publicación.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		if (PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
		{
			// Recupera el groupId del request.
			long groupId = this.getGroupIdFromRequest(request);
			
			if (groupId > 0)
			{
				try
				{
				    // Recupera los recursos del groupId a minificar.
					List<Node> resources = this.getResourcesToMinify(groupId);
				
					// Minifica los recursos.
					for (int i=resources.size() - 1; i>=0; --i)
					{
						this.minifyResource(resources.get(i));
					}
					this.printResult(response, HttpServletResponse.SC_OK, "Done!");
				}
				catch (Exception e)
				{
					// Si ocurre un error durante el proceso: Código de error 500.
					_log.error("Current URL: \"" + request.getRequestURL() + "\" generates unexpected error");
					_log.error(e);
					this.printResult(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errors!");
				}
			}
			else
			{
				// Si no puede recuperar el groupId: Código de error 400.
				_log.error("Unable to retrieve groupId. Current URL: \"" + request.getRequestURL() + "\" is invalid");
				this.printResult(response, HttpServletResponse.SC_BAD_REQUEST, "Unable to retrieve friendlyURL.");
			}
		}
		else
		{
			this.printResult(response, HttpServletResponse.SC_OK, "Service not available in LIVE environment!");
		}
	}
	
	/**
	 * Redirigido a {@link #doGet(HttpServletRequest, HttpServletResponse)}
	 */
	protected void doPost (HttpServletRequest request, HttpServletResponse response)
	{
		this.doGet(request, response);
	}
	
	/**
	 * Recupera el parámetro con la friendlyURL de la URL.
	 * 
	 * La URL debe tener el formato /base-portlet/minifierrsrc/[friendlyURL]
	 * 
	 * @param request HttpServletRequest para recuperar el parámetro groupId.
	 * @return long groupId de la friendlyURL.
	 *         0 en cualquier otro caso.
	 */
	private long getGroupIdFromRequest(HttpServletRequest request)
	{
		long groupId = 0;
		
		String uri 		  = request.getRequestURI();
		String[] uriArray = uri.split(StringPool.SLASH);
		
		if(uriArray != null && uriArray.length == 4)
		{
			long companyId     = IterGlobal.getCompanyId();
			String friendlyURL = "/" + uriArray[3];
			
			try
			{
				Group group = GroupLocalServiceUtil.getFriendlyURLGroup(companyId, friendlyURL);
				groupId = group.getGroupId();
			}
			catch (Exception e)
			{
				// Do nothing. Retorna groupId = 0 y el servlet devuelve un 400.
			}
		}
		
		return groupId;
	}
	
	/**
	 * Recupera de theme_webresource los recursos del grupo groupId con
	 * rsrccontenttype con valor 'text/css' o 'text/javascript'.
	 * 
	 * @param groupId El Id de grupo de los recursos.
	 * @return List<Node> Con los recursos recuperados.
	 * @throws SecurityException Si ocurre un error al ejecutar la query.
	 * @throws NoSuchMethodException Si ocurre un error al ejecutar la query.
	 */
	private List<Node> getResourcesToMinify(long groupId) throws SecurityException, NoSuchMethodException
	{
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_RESOURCES_TO_MINIFY_BY_GROUPID, groupId) );
		return dom.selectNodes("//row");
	}
	
	/**
	 * Minifica el recurso resource y lo actualiza en theme_webresource.
	 * 
	 * @param resource Node con el recurso a minificar que contiene como atrubutos
	 *        rsrcid, rsrccontent y rsrccontenttype.
	 * @throws ServiceError Si ocurre un error durante el proceso de minificado.
	 * @throws IOException Si ocurre un error al minificar.
	 * @throws SQLException Si ocurre un error al actualizar el recurso en BD.
	 */
	private void minifyResource(Node resource) throws ServiceError, IOException, SQLException
	{
		// Recupera rsrcid, rsrccontent y rsrccontent del recurso a minificar.
		String rsrcid			= XMLHelper.getTextValueOf(resource, "@rsrcid");
		String rsrccontent		= XMLHelper.getTextValueOf(resource, "@rsrccontent");
		String rsrccontenttype	= XMLHelper.getTextValueOf(resource, "@rsrccontenttype");

		// Minifica el rsrccontent del recurso.
		rsrccontent = MinifyUtil.minifyContentOnDeliverTheme(rsrccontent, rsrccontenttype);
		
		// Guardar el recurso mificado actualizando su rsrcmd5 y como pendiente de publicacion.
		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_WEBRESOURCE_BY_RSRCID, rsrccontent, (new Date()).getTime(), rsrcid) );
	}
	
	/**
	 * Establece el estado status en la respuesta e imprime msg.
	 * Si no puede recuperar el printer, solo establece el estado.
	 * 
	 * @param response HttpServletResponse con el resposne del servlet.
	 * @param status El estado de la respuesta.
	 * @param msg El mensaje a imprimir por pantalla.
	 */
	private void printResult(HttpServletResponse response, int status, String msg)
	{
		response.setStatus(status);
		PrintWriter out = null;
		try
		{
			out = response.getWriter();
		    out.println(msg);
			out.flush();
		}
		catch (IOException e)
		{
			// Do nothing.
		}
		finally
		{
			if (out != null)
				out.close();
		}
	}
}
