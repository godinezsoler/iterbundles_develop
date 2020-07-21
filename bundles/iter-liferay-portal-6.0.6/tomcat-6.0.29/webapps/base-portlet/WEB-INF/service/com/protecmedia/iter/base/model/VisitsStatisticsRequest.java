package com.protecmedia.iter.base.model;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class VisitsStatisticsRequest
{
	private static Log _log = LogFactoryUtil.getLog(VisitsStatisticsRequest.class);
	
	/** Formato de fecha de la petición */
	private final DateFormat servletDF = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/** Formato de fecha para SQL */
	private final DateFormat sqlDF     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/** Elementos que registran estadísticas */
	public enum StatisticItem { GROUP, SECTION, ARTICLE, METADATA, USER, NEWSLETTER, ABTESTING, SURVEY; }
	
	/** Tipos de agrupaciones de estadísticas en el gráfico */
	public enum Resolution { MINUTE, HOUR, DAY, MONTH; }
	
	/** Modos de visualización de anotaciones */
	public enum Annotation { NONE, GLOBAL, ARTICLE, ALL; }
	
	/** Horas por defecto a mostrar en la agrupación por horas */
	private static final int DEFAULT_DISPLAYED_HOURS = 24;

	/** Parámetros del servlet */
	private static final String PARAM_GROUP_ID      = "groupId";
	private static final String PARAM_PLID          = "plid";
	private static final String PARAM_ARTICLE_ID    = "articleId";
	private static final String PARAM_VOCABULARY_ID = "vocabularyId";
	private static final String PARAM_CATEGORY_ID   = "categoryId";
	private static final String PARAM_SCHEDULE_ID   = "scheduleId";
	private static final String PARAM_OVERLAY       = "overlay";
	private static final String PARAM_OVERLAY_TIME  = "overlayTime";
	private static final String PARAM_DATE_LIMIT    = "dateLimit";
	private static final String PARAM_DATE_FROM     = "dateFrom";
	private static final String PARAM_MAX_ARTICLES  = "maxArticles";
	private static final String PARAM_MAX_SECTIONS  = "maxSections";
	private static final String PARAM_MAX_ITEMS     = "maxItems";
	private static final String PARAM_VOCABULARIES  = "vocabularies";
	private static final String PARAM_EXTRA_DATA    = "extraData";
	private static final String PARAM_SHOW_VISITS   = "showVisits";
	private static final String PARAM_DISPL_HOURS   = "displayedHours";
	private static final String PARAM_REAL_TIME     = "realTime";
	private static final String PARAM_SURVEY_ID     = "surveyId";
	
	/** Tipo de página de estadísticas que realiza la solicitud */
	private StatisticItem pageType;
	
	/** Tipo de gráfico */
	private String chartType;
	
	/** Elemento del que se solicitan estadísticas */
	private StatisticItem item;
	
	/** Criterio del ranking */
	private String criteria;
	
	/** Agrupación de las estadísticas */
	private Resolution resolution;
	
	/** Fecha y Hora actuales */
	private Date currentDate;
	
	/** Fecha inicial */
	private Calendar startDate;
	/** Fecha inicial en formato SQL */
	private String sqlStartDate;
	
	/** Fecha final */
	private Calendar endDate;
	/** Fecha final en formato SQL */
	private String sqlEndDate;
	
	/** Recuperar estadísticas de la última hora */
	private boolean realTime;

	/** ID del grupo */
	private long groupId;
	/** ID de la delegación del grupo */
	private long delegationId = 0;
	/** ID de la sección */
	private long plid;
	/** Indica si la sección es la principal */
	private boolean homeSection;
	/** ID del artículo */
	private String articleId;
	/** ID del vocabulario */
	private long vocabularyId;
	/** ID de la categoría */
	private long categoryId;
	/** ID de la encuesta */
	private String surveyId;
	/** ID de la programación de la newsletter */
	private String scheduleId;
	
	/** Fecha indicada para mostrar las estadísticas */
	private String dateLimit;
	/** Máximo número de artículos del Top de más vistos */
	private int maxArticles;
	/** Máximo número de secciones del Top de más vistas */
	private int maxSections;
	/** Máximo número de elementos a mostrar en el ranking */
	private int maxItems;
	/** IDs de Vocabularios y máximo número de categorías para los Tops de más vistos */
	private String vocabularies;
	/** Solicitud de datos extra (Redes sociales, Comentarios, Valoraciones...) */
	private boolean extraData;
	/** Indica si se quiere mostrar el gráfico de visitas y Top de artículos o sólo los Tops de categiorías */
	private boolean showVisits;
	/** Horas a mostrar en el gráfico de visitas agrupada por horas */
	private int displayedHours;
	/** Indica si que quiere mostrar el día completo en la resolución por horas */
	private boolean realTimeDay;
	/** Indica si se quieren mostrar anotaciones en el gráfico de tendencias */
	private Annotation annotations;

	private static final String SELECT_ABTESTING = new StringBuilder(
		"SELECT startdate, finishdate, variantid, variantname, winner, visits, extvisits, prints 	\n").append(
		"FROM abtesting																				\n").append(	
		"INNER JOIN abtesting_variant ON abtesting.testid = abtesting_variant.testid				\n").append(
		"  	WHERE groupId = %d AND articleId = '%s'													\n").append(
		"	ORDER BY variantname ASC																\n").toString();

	public class ABTestingVariant
	{
		public long  	id			= 0;
		public String	name		= "";
		public boolean 	isWinner 	= false;
		public String   ctr			= "0.00";
		public long 	prints		= 0;
		public long 	visits		= 0;
		
		private ABTestingVariant(Element variant)
		{
			id 		= XMLHelper.getLongValueOf(variant, "@variantid");
			name 	= XMLHelper.getTextValueOf(variant, "@variantname");
			isWinner= GetterUtil.getBoolean(XMLHelper.getTextValueOf(variant, "@winner"));
			prints	= XMLHelper.getLongValueOf(variant, "@prints");
			visits	= XMLHelper.getLongValueOf(variant, "@visits");
			ctr		= calcCtr(prints, visits - XMLHelper.getLongValueOf(variant, "@extvisits"));
		}
		
//		public void addPrints(long prints)
//		{
//			_prints += prints;
//		}
//		public void addVisits(long visits)
//		{
//			_visits += visits;
//		}
		public String calcCtr(long prints, long visits) 
		{
			double ctr = (prints > 0) ?	(visits*100.) / (prints*1.):
							0.00;
			DecimalFormat f = new DecimalFormat("0.00");
			
			return f.format(ctr);
		}
//		public double getCtr()
//		{
//			return calcCtr(_prints, _visits);
//		}
	}
	public class ABTesting
	{
		private Calendar 					_startDate 	= null;
		private Calendar 					_finishDate = null;
		private TreeMap<Long, ABTestingVariant> _variants	= null;
		private long						_winner		= 0;
		public boolean 						hasExperiment = false;
		
		private ABTesting() throws SecurityException, NoSuchMethodException, ParseException
		{
			hasExperiment = isArticleRequest() && 
							(getResolution().equals(Resolution.MINUTE) || getResolution().equals(Resolution.HOUR) || getResolution().equals(Resolution.DAY) );
			
			if (hasExperiment)
			{
				String sql = String.format(SELECT_ABTESTING, groupId, articleId);
				_log.trace(sql);
				
				Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
				List<Node> variants = dom.selectNodes("/rs/row");
				
				hasExperiment = variants.size() > 0;
				if (hasExperiment)
				{
					_variants = new TreeMap<Long, ABTestingVariant>();
					for (int i = 0; i < variants.size(); i++)
					{
						Element variant = (Element)variants.get(i);
						ABTestingVariant var = new ABTestingVariant(variant);
						
						_variants.put(var.id, var);
						
						if (i == 0)
						{
							String date = variant.attributeValue("startdate");
							if (Validator.isNotNull(date))
							{
								_startDate = getCleanDatetime(sqlDF.parse(date), false );
								
								date = variant.attributeValue("finishdate");
								if (Validator.isNotNull(date))
									_finishDate = getCleanDatetime(sqlDF.parse(date), false );
							}
						}
						
						if ( var.isWinner )
							_winner = var.id;
					}
				}
			}
		}
		
		public String getVariantIDs()	
		{
			return (hasExperiment) ? StringUtil.merge(_variants.keySet()) : "";
		}
		
		public Map<Long, ABTestingVariant>	getVariants()	{	return _variants;	}
		public Long 						getWinner()		{	return _winner;		}
		public Calendar 					getStartDate()	{	return _startDate;	}
		public Calendar 					getFinishDate()	{	return _finishDate;	}
	}
	
	private ABTesting _abTesting = null;
	
	/**
	 * <p>Recupera del {@code request} todos los parámetros de la petición de estadísticas.</p>
	 * 
	 * @param request			El {@code HttpServletRequest} de la petición.
	 * @throws ParseException	Si no se pueden procesar las fechas inicial y/o final.
	 * @throws PortalException	Si no se puede recuperar la sección cuando se solicitan estadísticas de una sección en concreto.
	 * @throws SystemException	Si no se puede recuperar la sección cuando se solicitan estadísticas de una sección en concreto.
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public VisitsStatisticsRequest(HttpServletRequest request) throws ParseException, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		// Tipo de página de estadísticas que realiza la solicitud (Sitio, Artículo, Sección o Metadato)
		String[] splitUri = request.getRequestURI().split(StringPool.SLASH);
		this.pageType = StatisticItem.valueOf(splitUri[3].toUpperCase());
		
		// Tipo de gráfico
		this.chartType = ParamUtil.getString(request, "chartType", null);

		// Tipo de elemento del que se solicitan estadísticas (Sitio, Artículo, Sección o Metadato)
		this.item = StatisticItem.valueOf(ParamUtil.getString(request, "item", "group").toUpperCase());
		
		// Tipo de elemento del que se solicitan estadísticas (Sitio, Artículo, Sección o Metadato)
		this.criteria = ParamUtil.getString(request, "criteria", "visits");
		
		// Resolución del gráfico de visitas (Por defecto en horas)
		this.resolution = Resolution.valueOf(ParamUtil.getString(request, "resolution", Resolution.HOUR.name()).toUpperCase());
		
		/////////////////////////////////////////
		//              ELEMENTO               //
		/////////////////////////////////////////
		// Group ID
		this.groupId = ParamUtil.getLong(request, PARAM_GROUP_ID, 0);
		
		// ITER-1095 Desde ITERAdmin, en las Newsletters, al pulsar el botón "Mostrar estadísticas" se muestra una página en blanco sin información alguna
		// http://jira.protecmedia.com:8080/browse/ITER-1095?focusedCommentId=46533&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-46533
		if (this.groupId != 0)
		{
			// Delegarion ID
			this.delegationId = GroupLocalServiceUtil.getGroup(this.groupId).getDelegationId();
		}
		// PLID de la sección
		this.plid = ParamUtil.getLong(request, PARAM_PLID, 0);
		if (this.plid > 0)
		{
			Layout layout = LayoutLocalServiceUtil.getLayout(this.plid);
			if (LayoutConstants.isHome(layout))
				this.homeSection = true;
		}
		// ID del artículo
		this.articleId = ParamUtil.getString(request, PARAM_ARTICLE_ID, null);
		// ID de la encuesta
		this.surveyId = ParamUtil.getString(request, PARAM_SURVEY_ID, null);
		// Overlay
		boolean overlay = ParamUtil.getBoolean(request, PARAM_OVERLAY, false);
		if (overlay)
		{
			this.articleId = this.articleId.substring(2);
			
			// En el Overlay, si está activada la propiedad (por defecto) y es un sitio de Demos la resolución es de horas 
			if (groupId != 0 && PropsValues.ITER_DEMO_FORCE_STATISTICS_RESOLUTION &&
				LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost().toLowerCase().endsWith(PropsValues.ITER_DEMO_SITENAME))
				this.resolution = Resolution.HOUR;
		}
		
		this._abTesting = new ABTesting();
		
		// ID del vocabulario
		this.vocabularyId = ParamUtil.getLong(request, PARAM_VOCABULARY_ID, 0);
		// ID de la categoría
		this.categoryId = ParamUtil.getLong(request, PARAM_CATEGORY_ID, 0);
		// ID de la programación de la newsletter
		this.scheduleId = ParamUtil.getString(request, PARAM_SCHEDULE_ID, null);
		
		/////////////////////////////////////////
		//           CONFIGURACIÓN             //
		/////////////////////////////////////////
		// Máximo número de artículos del ranking de más visitados
		this.maxArticles = ParamUtil.getInteger(request, PARAM_MAX_ARTICLES, 0);
		// Máximo número de artículos del ranking de más visitados
		this.maxSections = ParamUtil.getInteger(request, PARAM_MAX_SECTIONS, 0);
		// Máximo número de elementos del ranking
		this.maxItems = ParamUtil.getInteger(request, PARAM_MAX_ITEMS, 0);
		// Vocabularios y máximo número de categorías para los rankings de más visitadas
		this.vocabularies = ParamUtil.getString(request, PARAM_VOCABULARIES, StringPool.BLANK);
		// Obtención de datos extra (Rankings de artículos y metadatos)
		this.extraData = ParamUtil.getBoolean(request, PARAM_EXTRA_DATA, false);
		// Indica si debe mostrarse el gráfico de visitas
		this.showVisits = ParamUtil.getBoolean(request, PARAM_SHOW_VISITS, true);
		// Recoge las horas a mostrar en el gráfico de visitas (por defecto 24)
		this.displayedHours = ParamUtil.getInteger(request, PARAM_DISPL_HOURS, DEFAULT_DISPLAYED_HOURS);
		// Indica si que quiere mostrar el día completo en la resolución por horas o las N horas anteriores
		this.realTimeDay = ParamUtil.getBoolean(request, PARAM_REAL_TIME, true);
		// Indica si no se quieren mostrar anotaciones, sólo globales, sólo de artículos o ambas
		Annotation annotationDefault = StatisticItem.ARTICLE == this.pageType ? Annotation.ARTICLE : Annotation.GLOBAL;
		this.annotations = Annotation.valueOf(ParamUtil.getString(request, "annotations", annotationDefault.name()).toUpperCase());
		if (!StatisticItem.ARTICLE.equals(this.pageType) && Annotation.ARTICLE.equals(this.annotations) || Annotation.ALL.equals(this.annotations))
			this.annotations = Annotation.GLOBAL;
		
		/////////////////////////////////////////
		//               FECHAS                //
		/////////////////////////////////////////
		// Guarda la fecha indicada por parámetro
		this.dateLimit = ParamUtil.getString(request, PARAM_DATE_LIMIT, StringPool.BLANK);
		// Establece las fechas inicial y final
		if (StatisticItem.NEWSLETTER.equals(this.item))
			setFixedDates(request);
		else
			setDates(request, overlay);
		// Establece si el gráfico es en tiempo real, es decir, si se piden estadísticas para la hora, día o mes actual.
		setRealTime();
	}
	
	private void setFixedDates(HttpServletRequest request) throws ParseException
	{
		String dateFrom = ParamUtil.getString(request, PARAM_DATE_FROM, StringPool.BLANK);
		
		startDate = getCleanDatetime(servletDF.parse(dateFrom));
		endDate = getCleanDatetime(servletDF.parse(dateLimit));
		
		sqlStartDate = sqlDF.format(startDate.getTime());
		sqlEndDate = sqlDF.format(endDate.getTime());
	}
	
	private void setDates(HttpServletRequest request, boolean overlay) throws ParseException
	{
		setCurrentDateLimit(dateLimit);
		
		startDate 	= getCleanDatetime(currentDate, !resolution.equals(Resolution.MINUTE));
		endDate 	= getCleanDatetime(currentDate, !resolution.equals(Resolution.MINUTE));
		
		switch (resolution) 
		{
		case MINUTE:
			if (overlay)
			{
				// Se retrocede en el tiempo. Últimos 30 minutos por defecto
				int defaultOverlayMinutes = ParamUtil.getInteger(request, PARAM_OVERLAY_TIME, 30);
				
				defaultOverlayMinutes = 1-defaultOverlayMinutes;
				
				// Últimas 12 horas (11 * 60 = 660+59 = 719)
				// Últimos 30 minutos 
				startDate.add(Calendar.MINUTE, defaultOverlayMinutes);
			}
			else
			{
				if (this.realTimeDay)
				{
					endDate = Calendar.getInstance();
					endDate.setTime(currentDate);
					endDate.clear(Calendar.MILLISECOND);
					endDate.clear(Calendar.SECOND);
					
					startDate = Calendar.getInstance();
					startDate.setTime(currentDate);
					startDate.add(Calendar.HOUR_OF_DAY, -1);
					startDate.add(Calendar.MINUTE, 1);
					startDate.clear(Calendar.MILLISECOND);
					startDate.clear(Calendar.SECOND);
				}
				else
				{
					startDate.set(Calendar.HOUR_OF_DAY, 0);
					startDate.set(Calendar.MINUTE, 0);
					
					endDate.set(Calendar.HOUR_OF_DAY, 23);
					endDate.set(Calendar.MINUTE, 59);
				}
			}
			break;
			
		case HOUR:
			if (overlay)
			{
				// Se retrocede en el tiempo. Últimos 12 horas por defecto
				int defaultOverlayHours = ParamUtil.getInteger(request, PARAM_OVERLAY_TIME, 12);
				defaultOverlayHours = 1 - defaultOverlayHours;

				startDate.add(Calendar.HOUR_OF_DAY, defaultOverlayHours);
			}
			else
			{
				startDate.set(Calendar.HOUR_OF_DAY, 0);
				endDate.set(Calendar.HOUR_OF_DAY, 23);
			}
			break;
			
		case DAY:
		case MONTH:
			startDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMinimum(Calendar.DAY_OF_MONTH));
			startDate.set(Calendar.HOUR_OF_DAY, 0);
			endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
			endDate.set(Calendar.HOUR_OF_DAY, 23);
			
			if (Resolution.MONTH.equals(resolution))
			{
				startDate.set(Calendar.MONTH, 0);
				endDate.set(Calendar.MONTH, 11);
			}
			
			break;

		default:
			break;
		}
		
		sqlStartDate = sqlDF.format(startDate.getTime());
		sqlEndDate = sqlDF.format(endDate.getTime());
	}
	
	private void setCurrentDateLimit(String dateLimit) throws ParseException
	{
		if (Validator.isNull(dateLimit))
		{
			currentDate = new Date();
		}
		else
		{
			currentDate = servletDF.parse(dateLimit);
		}
	}
	
	/**
	 * <p>Inicializa un {@code Calendar} del momento actual con los minutos, segundos y milisegundos
	 * establecidos a 0.</p>
	 * 
	 * @return	{@code Calendar} inicializado con minutos, segundos y milisegundos establecidos a 0.
	 */
	private Calendar getCleanDateTime()
	{
		return getCleanDatetime(null);
	}
	
	/**
	 * <p>Inicializa un {@code Calendar} del momento actual con los segundos y milisegundos
	 * establecidos a 0.</p>
	 * <p>Si se indica {@code cleanMinutes} a {@code true}, los minutos se establecen a 0.</p>
	 * 
	 * @param cleanMinutes Si se indica a {@code true}, los minutos se establecen a 0.
	 * @return
	 */
	private Calendar getCleanDateTime(boolean cleanMinutes)
	{
		return getCleanDatetime(null, cleanMinutes);
	}
	
	/**
	 * <p>Crea un {@code Calendar} a partir de {@code dateToSet} con los minutos, segundos y milisegundos
	 * establecidos a 0.</p>
	 * <p>Si {@code dateToSet} es nulo, toma la fecha y hora actual.</p>
	 * 
	 * @param dateToSet	La fecha con la que inicializar el calendario.
	 * @return			{@code Calendar} inicializado con minutos, segundos y milisegundos establecidos a 0. 
	 */
	private Calendar getCleanDatetime(Date dateToSet)
	{
		return getCleanDatetime(dateToSet, true);
	}
	
	/**
	 * <p>Crea un {@code Calendar} a partir de {@code dateToSet} con los minutos, segundos y milisegundos
	 * establecidos a 0.</p>
	 * <p>Si {@code dateToSet} es nulo, toma la fecha y hora actual.</p>
	 * <p>Si se indica {@code cleanMinutes} a {@code true}, los minutos se establecen a 0.</p>
	 * 
	 * @param dateToSet    La fecha con la que inicializar el calendario.
	 * @param cleanMinutes Si se indica a {@code true}, los minutos se establecen a 0.
	 * @return             {@code Calendar} inicializado.
	 */
	private Calendar getCleanDatetime(Date dateToSet, boolean cleanMinutes)
	{
		Calendar calendar = Calendar.getInstance();
		if (dateToSet != null)
			calendar.setTime(dateToSet);
		calendar.clear(Calendar.MILLISECOND);
		calendar.clear(Calendar.SECOND);
		if (cleanMinutes)
			calendar.clear(Calendar.MINUTE);
		return calendar;
	}
	
	private void setRealTime()
	{
		this.realTime = false;
		
		if (this.resolution == Resolution.MINUTE || this.item == StatisticItem.USER)
		{
			Calendar now = this.resolution == Resolution.MINUTE ? getCleanDateTime(false) : getCleanDateTime();
			if (this.realTimeDay)
				this.realTime = this.endDate.compareTo(now) == 0;
			else
				this.realTime = this.startDate.compareTo(now) <= 0 && this.endDate.compareTo(now) >= 0;
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean isGroupRequest()
	{
		return StatisticItem.GROUP.equals(pageType) && groupId > 0;
	}
	
	public boolean isSectionRequest()
	{
		return StatisticItem.SECTION.equals(pageType) && groupId > 0 && plid > 0;
	}
	
	public boolean isArticleRequest()
	{
		return StatisticItem.ARTICLE.equals(pageType) && groupId > 0 && Validator.isNotNull(articleId);
	}
	
	public boolean isMetadataRequest()
	{
		return StatisticItem.METADATA.equals(pageType) && groupId > 0 && categoryId > 0;
	}
	
	public boolean isUserRequest()
	{
		return StatisticItem.USER.equals(pageType) && groupId > 0;
	}
	
	public boolean isNewsletterRequest()
	{
		return StatisticItem.NEWSLETTER.equals(pageType) && Validator.isNotNull(scheduleId);
	}
	

	public ABTesting getABTesting()	  { return _abTesting; }
	public boolean isRealTime()       { return realTime; }
	public boolean isRealTimeDay()    { return realTimeDay; }
	public boolean isHomeSection()    { return homeSection; }
	public boolean isExtraData()      { return extraData; }
	public boolean isShowVisits()     { return showVisits; }
	public StatisticItem getPageType(){ return pageType; }
	public String getChartType()      { return chartType; }
	public StatisticItem getItem()    { return item; }
	public String getCriteria()       { return criteria; }
	public Resolution getResolution() { return resolution; }
	public Calendar getStartDate()    { return startDate; }
	public Calendar getEndDate()      { return endDate; }
	public long getGroupId()          { return groupId; }
	public long getDelegationId()     { return delegationId; }
	public long getPlid()             { return plid; }
	public String getArticleId()      { return articleId; }
	public String getSurveyId()       { return surveyId; }
	public long getVocabularyId()     { return vocabularyId; }
	public long getCategoryId()       { return categoryId; }
	public String getScheduleId()     { return scheduleId; }
	public int getMaxArticles()       { return maxArticles; }
	public int getMaxSections()       { return maxSections; }
	public int getMaxItems()          { return maxItems; }
	public String getVocabularies()   { return vocabularies; }
	public String getSqlStartDate()   { return sqlStartDate; }
	public String getSqlEndDate()     { return sqlEndDate; }
	public String getDateLimit()      { return dateLimit; }
	public int getDisplayedHours()    { return displayedHours; }
	public Annotation getAnnotations(){ return annotations; }
}
