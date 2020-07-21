package com.protecmedia.iter.news.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.metrics.NewsletterMASTools;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.RelAttributeUtil;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WidgetUtil;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.HtmlOptimizer;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.VelocityUtil;

public class DaylyTopic {
	
	private static Log _log = LogFactoryUtil.getLog(DaylyTopic.class);
	
	// Plantilla velócity a aplicar
	private static final String vm = new StringBuffer(new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath())
    												  .append(File.separator + "news-portlet" + File.separator + "xsl" + File.separator + 
    												  "dayly_topics_default.vm").toString();
	private String  daylyTopicId;	
	private String  displayName;	// Nombre a mostrar
	private Long 	modelId;		// designer_tamplate.id_
	
	// Enlaces
	private String  articleId;		// journalarticle.articleId
	private Long    sectionId;		// layout.plid
	private String  url;	
	private Long    categoryId;		// assetcategory.categoryId
	
	private boolean targetBlank;	// Indica si la url tiene o no target="_blank"	
	
	private final static String GET_DAYLY_TOPICS_BY_PLID = new StringBuffer()
		.append("SELECT daylytopicid, layoutid, modelid, displayname, articleid, sectionid, url, categoryid, targetblank, daylytopics \n")
		.append("FROM daylytopic \n")
		.append("WHERE groupid = %s \n")
		.append("  AND (layoutid %s ) \n")
		.append("ORDER BY sort ASC").toString();
	
	private final static String GET_NAME_FROM_ASSETCATEGORYID = "SELECT name FROM assetcategory WHERE categoryId = %s LIMIT 1";
	
	public DaylyTopic(String daylytopicid, Long  modelId, String displayName, String articleid, Long sectionid, 
		              String url, Long categoryid, boolean targetBlank){
		
		this.daylyTopicId = daylytopicid;		
		this.modelId	  = modelId;
		this.displayName  = displayName;
		this.articleId    = articleid;
		this.sectionId    = sectionid;
		this.url          = url;
		this.categoryId   = categoryid;
		this.targetBlank  = targetBlank;		
	}
	
	// Obtiene el html para el portlet de dayly topics
	public static void getDaylyTopicsHtml(ThemeDisplay themeDisplay, HttpServletResponse response,
		                                  String beforeText, String afterText) 
		                                				  throws SecurityException, SystemException, NoSuchMethodException, 
		                                				  ServiceError, PortalException{
		_log.trace("In getDaylyTopicsHtml");
		
		// Para que llegue la sección actual y funcione en un catálogo, michel tiene que dar de alta el portlet previamente.
		final Layout actualLayout = SectionUtil.getSection(themeDisplay.getRequest());
		
		Long actualLayoutId = null;		
		// En las página de búsqueda y de metas no hay una sección actual
		if (null != actualLayout){
			actualLayoutId = actualLayout.getPlid();	
		}
		 
		final long groupId = themeDisplay.getScopeGroupId();
				
		// Listado de elementos que serán pintados en velócity
		final ArrayList<DaylyTopic> daylyTopics = getDaylyTopicsList(themeDisplay.getRequest(), groupId, actualLayoutId);
		
		if (null != daylyTopics && daylyTopics.size() > 0){
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Daylytopics found: '").append(daylyTopics.size()).append("'"));
			
			// Variables que se inyectarán en velócity
			HashMap<String, Object> variablesToBeInjected = new HashMap<String, Object>();
			
			if(_log.isDebugEnabled())
				_log.debug("Setting variables to be injected in velocity");
			
			variablesToBeInjected.put("daylyTopics",  			daylyTopics );
			variablesToBeInjected.put("themeDisplay", 			themeDisplay);
			variablesToBeInjected.put("beforeText",   			beforeText);
			variablesToBeInjected.put("afterText",    			afterText);
			variablesToBeInjected.put("HtmlOptimizer_isEnabled",HtmlOptimizer.isEnabled());
			
			try {
				// Aplicamos la plantilla velócity
				if(_log.isDebugEnabled())
					_log.debug("Before call merge template to dayly topics");
				
				VelocityUtil.mergeTemplate( new File(vm.toString()), null, variablesToBeInjected, response.getWriter());
				
				if(_log.isDebugEnabled())
					_log.debug("After call merge template to dayly topics");
				
			}catch (Exception e) {
				_log.error("Error applying template", e);
			}
		}else{
			_log.debug("No dayly topics to show");
		}		
	}
	
	private static ArrayList<DaylyTopic> getDaylyTopicsList(HttpServletRequest request, long groupId, Long layoutPlid) throws SecurityException, NoSuchMethodException, ServiceError, SystemException, PortalException
	{
		_log.trace("In getDaylyTopics");
		
		List<Node> rows = null;
		
		// No hay sección actual si la página es de resultados o de metas. Buscamos los dailytopics del TPU (aquellos sin sección)
		if (null == layoutPlid)
		{			
			final String sql = String.format(GET_DAYLY_TOPICS_BY_PLID, groupId, "is null");
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to get TPU daily topics:\n").append(sql));
			
			final Document auxResult = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			// Hemos encontrado dayly topics
			if (null != auxResult)
			{
				rows = auxResult.getRootElement().selectNodes("/rs/row");
				
				// Hemos encontrado dayly topics
				if (null != rows && rows.size() > 0)
				{					
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Dayly topics found: '").append(rows.size()).append("'"));						
				}
			}
		}
		else
		{		
			/* Obtenemos todos los padres de la sección actual. 
			Llega algo tal que así: "1120101,121017,121008,0" donde el primer valor es actual plid y los siguientes son sus padres */
			String auxPlids = SectionUtil.getPlidHierarchy(request, layoutPlid);		
			ErrorRaiser.throwIfFalse(Validator.isNotNull(auxPlids), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "SectionUtil.getPlidHierarchy returns null or empty with plid: '" + layoutPlid + "'");
			
			// Quitamos la última sección (0)
			auxPlids = auxPlids.replaceAll(",0", "");			
			final String[] parentsPlids = auxPlids.split(",");	
			
			if (Validator.isNotNull(parentsPlids))
			{
				// Vamos a la base de datos una vez aunque nos tengamos que traer posiblemente más datos de la cuenta.
				final String sql = String.format(GET_DAYLY_TOPICS_BY_PLID, groupId,
												 new StringBuilder("IN(").append(auxPlids).append(") \n  OR layoutid IS NULL \n").toString());					 
					
				if (_log.isDebugEnabled())
				{
					_log.debug(new StringBuilder("Query to get daily topics:\n").append(sql));
				}				
				Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
				Node root = result.getRootElement();				
				
				// Recorremos el arbol de secciones hasta encontrar algún daylytopic o hasta que se indique que no se quiere mostrar ninguno.
				for (int i = 0; i < (parentsPlids.length + 1); i++)
				{	
					// Como última opción, obtenemos los daylytopics sin sección (del TPU)
					if (i == parentsPlids.length)
					{ 
						rows = root.selectNodes(new StringBuilder("//row[@layoutid='']").toString());
					}
					else
					{
						rows = root.selectNodes(new StringBuilder("//row[@layoutid='").append(parentsPlids[i]).append("']").toString());
					}					
						
					// Hemos encontrado dayly topics
					if (null != rows && rows.size() > 0)
					{
						if(_log.isDebugEnabled())
						{
							_log.debug(new StringBuilder("Dayly topics found: '").append(rows.size()).append("'"));
						}
						break;
					}			
				}
				
			}
			else if(_log.isDebugEnabled())
			{
				_log.info(new StringBuilder("No daily topics found for section '").append(layoutPlid).append("'"));
			}			
		}
		
		ArrayList<DaylyTopic> daylyTopics = null;
		
		if (null != rows && rows.size() > 0)
		{
			daylyTopics = new ArrayList<DaylyTopic>();		
			
			if(_log.isDebugEnabled())
				_log.debug("Creating the list with the dayly topics");
			
			final int size = rows.size();
			for (int i = 0; i < size; i++)
			{
				final Node row = rows.get(i);
				
				final String auxId = XMLHelper.getTextValueOf(row, "@daylytopicid");
				
				// Comprobamos si no se quieren daylytopics (daylytopics = 0) para la sección
				if (!GetterUtil.getBoolean(XMLHelper.getTextValueOf(rows.get(i), "@daylytopics"), true))
				{
					if (_log.isDebugEnabled())
						_log.debug(new StringBuilder("Section does not want dayly topics. (daylytopic register: '").append(auxId).append("' has column daylytopics value = '0')"));
					
					// Vaciamos el array por si se hubiese metido ya algún daylytopic en una iteración anterior
					daylyTopics = new ArrayList<DaylyTopic>();
					break;
				}				
								
				final Long    auxModelId     = XMLHelper.getLongValueOf(row,                        "@modelid",     -1   );
				final String  auxDisplayName = XMLHelper.getTextValueOf(row,                        "@displayname"       );
				final String  auxArticleid   = XMLHelper.getTextValueOf(row,                        "@articleid",   null );
				final long    auxSectionid   = XMLHelper.getLongValueOf(row,                        "@sectionid",   -1   );
				final String  auxUrl         = XMLHelper.getTextValueOf(row,                        "@url"               );
				final long    auxCategoryid  = XMLHelper.getLongValueOf(row,                        "@categoryid",  -1   );
				final boolean auxTargetblank = GetterUtil.getBoolean((XMLHelper.getTextValueOf(row, "@targetblank", "0")));
				
				daylyTopics.add(new DaylyTopic(auxId,
											   (auxModelId               == -1 ? null : auxModelId  ),
									           auxDisplayName,
									           (Validator.isNull(auxArticleid) ? null : auxArticleid),
									           (auxSectionid             == -1 ? null : auxSectionid),
									           auxUrl,
									           (auxCategoryid            == -1 ? null : auxCategoryid),
									           auxTargetblank) );
			}
		}
		return daylyTopics;		
	}	
	
	public String getHyperlink(ThemeDisplay themeDisplay) throws ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException, MalformedURLException, ParseException, UnsupportedEncodingException, DocumentException
	{
		_log.trace(new StringBuilder("In getLink() for daylytopic: '").append(this.daylyTopicId + "'"));
		
		String hyperlink = "";
		
		
		if (null != this.modelId && null != this.categoryId)
		{		
			// Enlace a la categoría
			hyperlink = getHyperlinkFromCategoryId(themeDisplay, this.modelId, this.categoryId);
		}
		else if (null != this.articleId)
		{
			// Enlace al artículo
			hyperlink = IterURLUtil.getArticleURLByLayoutUUID( themeDisplay.getScopeGroupId(), this.articleId, null);
		}
		else if(null != this.sectionId)
		{
			// Enlace a la sección
			hyperlink = getHyperlinkFromLayoutId(this.sectionId);
		}
		else if(null != this.url)
		{
			// Enlace puesto a pelo
			hyperlink = NewsletterMASTools.addMark2URL(this.url); 						
		}
		else
		{
			_log.debug(new StringBuilder("The daylyTopic register: '").append(this.daylyTopicId).append("' has no valid columns combination to get it url"));
		}
		
		// Para quitar de la ruta el "/widget/f/" al estar el portlet de dailytopic incluido en un catalogo
		hyperlink = WidgetUtil.getPortalURL(hyperlink, themeDisplay);
		
		// http://jira.protecmedia.com:8080/browse/ITER-410
		// Comprobar que las URLs absolutas que genera el DaylyTopic tengan el protocolo configurado.
		if( hyperlink!="" && !HttpUtil.isAbsolute(hyperlink) && IterRequest.isNewsletterPage() )
			hyperlink = HttpUtil.forceProtocol( IterURLUtil.getIterHost() + hyperlink, IterURLUtil.getIterProtocol() );
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("getHyperlink() returned: '").append(hyperlink).append("'"));
		
		return hyperlink;
	}
	
	// Url para la sección
	private static String getHyperlinkFromLayoutId(long layoutId) throws PortalException, SystemException, UnsupportedEncodingException, ServiceError
	{
		_log.trace("In getHyperlinkFromSectionId");		
	
		Layout layout = LayoutLocalServiceUtil.getLayout(layoutId);
		
		String url = IterURLUtil.getLayoutURL(layout);
		url = NewsletterMASTools.addMark2Section(url, layout);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("getHyperlinkFromLayoutId() returned: '").append(url).append("'"));
		
		return url;
	}
 
	// Url para la categoría. Necesita el modelo para buscar el pageTemplate
	private static String getHyperlinkFromCategoryId(ThemeDisplay themeDisplay, Long modelId, long categoryId) throws ServiceError, SystemException, PortalException, UnsupportedEncodingException
	{
		_log.trace("In getHyperlinkFromCategoryId");
		
		final String sql = String.format(GET_NAME_FROM_ASSETCATEGORYID, Long.toString(categoryId) );
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to get assetcategory name:\n").append(sql));
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		
		ErrorRaiser.throwIfFalse(null != result && result.size() == 1, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, new StringBuilder("No assetcategories found with the categoryId: '").append(categoryId).append("'").toString());
		
		final String name = (String)result.get(0);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("Name '").append(name).append("' found from categoryId: '").append(categoryId).append("' "));	
				
		String url = TopicsUtil.getTopicURLById(modelId, name);
		url = NewsletterMASTools.addMark2Meta(url, name);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("Url returned in getHyperlinkFromCategoryId from categoryId '").append(categoryId).append("': '").append(url).append("'"));
		
		return url;		
	}	
	
	// Obtiene el atributo "rel". (ver ticket mantis: 0009357)
	public String getRelAttribute(){
		
		String rel = "";
		
		// Enlace externo
		if(Validator.isNotNull(url))
		{
			if (!RelAttributeUtil.getExternalFollowPreference().equals(RelAttributeUtil.getNoFollow()))
			{
				rel = new StringBuilder("rel=\"").append(RelAttributeUtil.getFollow()).append("\"").toString();
			}
			else
			{
				rel = new StringBuilder("rel=\"").append(RelAttributeUtil.getNoFollow()).append("\"").toString();
			}
		}
		
		// Enlace interno (artículo, sección o categoría)
		else
		{		
			if (RelAttributeUtil.getInternalFollowPreference().equals(RelAttributeUtil.getNoFollow()))
			{
				rel = new StringBuilder("rel=\"").append(RelAttributeUtil.getNoFollow()).append("\"").toString();
			}
			
		}		
		return rel;
	}
	
	public String getDaylytopicid() {	
		return daylyTopicId;
	}
	
	public String getDisplayName() {	
		return displayName;
	}	
	
	public boolean isTargetBlank() {	
		return targetBlank;
	}
	
	public long getCategoryId() {	
		return categoryId == null ? -1 : categoryId;
	}
}