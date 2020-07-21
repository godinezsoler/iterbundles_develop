package com.protecmedia.iter.tracking.util;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.QualificationTools;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.velocity.VelocityContext;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class RankingUtil
{

	private static Log _log = LogFactoryUtil.getLog(RankingUtil.class);
	
	public static String getRankingViewerList(String bodyClass, long globalGroupId, long scopeGroupId, List<String> structures, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia, String templateIdArticleRestricted, String templateIdGalleryRestricted,
			String templateIdPollRestricted, String templateIdMultimediaRestricted, int modeArticle, int modeGallery, int modePoll, int modeMultimedia, int startIndex, int numElements, Date modifiedDate, String[] orderFields, int typeOrder, long[] categoriesId, String[] qualificationId,
			String[] layoutIds, boolean defaultMode, long modelId, int sectionToShow, HttpServletRequest request, ThemeDisplay themeDisplay, String xmlRequest, Locale locale)
	{

		StringBuffer sb = new StringBuffer();

		try
		{
			Date validityDate = GroupMgr.getPublicationDate(scopeGroupId);
			List<String[]> results = PageContentLocalServiceUtil.getFilterArticles(scopeGroupId, structures, startIndex, numElements, validityDate, modifiedDate, orderFields, typeOrder, categoriesId, qualificationId, layoutIds);

			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, results.size());
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, results.size());
			
			//se pide el qualificationName para insertarlo en el request
			String qualifId = qualificationId != null && qualificationId.length > 0 ? qualificationId[0]  :  "";
			String qualificationName = QualificationTools.getQualificationName( qualifId);
			
			request.setAttribute( VelocityContext.VELOCITYVAR_ITER_QUALIFICATION, qualificationName );

			for (int i = 0; i < results.size(); i++)
			{
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, results.get(i)[0].toString());

				try
				{
					sb.append(getContent( bodyClass, globalGroupId, scopeGroupId, templateIdArticle, templateIdGallery, templateIdPoll, templateIdMultimedia, templateIdArticleRestricted, templateIdGalleryRestricted, templateIdPollRestricted, templateIdMultimediaRestricted, modeArticle, modeGallery, modePoll,
							modeMultimedia, layoutIds, defaultMode, modelId, sectionToShow, request, themeDisplay, xmlRequest, locale, article, results.size(), i));
				}
				catch (Exception e)
				{
					if (article != null && Validator.isNotNull(article.getArticleId()))
						_log.error("Current articleId: " + article.getArticleId());

					_log.error(e.toString());
					_log.trace(e);
				}

			}

		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}

		return sb.toString();
	}

	private static String getContent( String bodyClass, long globalGroupId, long scopeGroupId, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia, String templateIdArticleRestricted, String templateIdGalleryRestricted, String templateIdPollRestricted,
			String templateIdMultimediaRestricted, int modeArticle, int modeGallery, int modePoll, int modeMultimedia, String[] layoutIds, boolean defaultMode, long modelId, int sectionToShow, HttpServletRequest request, ThemeDisplay themeDisplay, String xmlRequest, Locale locale,
			JournalArticle article, int total, int current) throws PortalException, SystemException, SecurityException, com.liferay.portal.kernel.error.ServiceError, NoSuchMethodException, ParseException, UnsupportedEncodingException, MalformedURLException, DocumentException
	{
		StringBuffer html = new StringBuffer();
		String currentHTML = "";

		
		String[] ctxLayouts = (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;

		if (checkTemplateByArticleStructure(article, templateIdArticle, templateIdGallery, templateIdPoll, templateIdMultimedia))
		{

			String faTemplateId = PageContentLocalServiceUtil.getTemplateId(article, templateIdArticle, templateIdGallery, templateIdPoll, templateIdMultimedia);

			String raTemplateId = PageContentLocalServiceUtil.getTemplateId(article, templateIdArticleRestricted, templateIdGalleryRestricted, templateIdPollRestricted, templateIdMultimediaRestricted);

			
			String viewMode = PageContentLocalServiceUtil.getArticleContextInfo(themeDisplay.getScopeGroupId(), article.getArticleId(), ctxLayouts);

			int templateMode = PageContentLocalServiceUtil.getTemplateMode(article, modeArticle, modeGallery, modePoll, modeMultimedia);

			currentHTML = PageContentLocalServiceUtil.getArticleContent(article, faTemplateId, raTemplateId, viewMode, themeDisplay, xmlRequest, templateMode, request, current + 1, total);
		}
		else
		{
			String viewMode = IterURLUtil.getArticleURLByLayoutUUID(scopeGroupId, article.getArticleId(), ctxLayouts);
			String headline = PageContentLocalServiceUtil.getWebContentField(article, "Headline", locale.toString(), 0);
			if (Validator.isNull(headline))
				headline = article.getTitle();

			currentHTML = "<a href=\"" + viewMode + "\">" + headline + "</a>";
		}

		String idx = String.valueOf(current + 1);
		String classValues = "element " + PageContentLocalServiceUtil.getCSSClass(Integer.valueOf(idx).intValue(), total) + " " + 
										  PageContentLocalServiceUtil.getCSSAccessClass(article, request);
		//iteridart
		String articleId 		 = article.getArticleId();
		String signedArticleId 	 =  IterURLUtil.getSignedArticleId(articleId);
		
		
		html.append("<div class=\"" + classValues + bodyClass+ "\" iteridart='" + signedArticleId + "'>");
		html.append("<span class='teaserItemPosition "+ HtmlUtil.getRemovableClass() +"'>" + idx + "</span>");
		html.append(currentHTML);
		html.append("</div>");

		return html.toString();
	}

	private static boolean checkTemplateByArticleStructure(JournalArticle article, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia)
	{
		boolean templateSelected = false;

		if (article.getStructureId().equals(IterKeys.STRUCTURE_ARTICLE) && Validator.isNotNull(templateIdArticle))
			templateSelected = true;
		else
			if (article.getStructureId().equals(IterKeys.STRUCTURE_GALLERY) && Validator.isNotNull(templateIdGallery))
				templateSelected = true;
			else
				if (article.getStructureId().equals(IterKeys.STRUCTURE_POLL) && Validator.isNotNull(templateIdPoll))
					templateSelected = true;
				else
					if (article.getStructureId().equals(IterKeys.STRUCTURE_MULTIMEDIA) && Validator.isNotNull(templateIdMultimedia))
						templateSelected = true;

		return templateSelected;
	}

	public static Date getModifiedDate(String modifiedDateRangeTimeValue, String modifiedDateRangeTimeUnit, long scopeGroupId)
	{

		if (!modifiedDateRangeTimeValue.equals("0") && !modifiedDateRangeTimeValue.equals(""))
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(GroupMgr.getPublicationDate(scopeGroupId));

			int nModifiedDateRangeTimeValue = Integer.valueOf(modifiedDateRangeTimeValue) * (-1);

			if (modifiedDateRangeTimeUnit.equals(IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR))
			{
				cal.add(Calendar.HOUR, nModifiedDateRangeTimeValue);
			}
			else
				if (modifiedDateRangeTimeUnit.equals(IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_DAY))
				{
					cal.add(Calendar.DATE, nModifiedDateRangeTimeValue);
				}
				else
					if (modifiedDateRangeTimeUnit.equals(IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_MONTH))
					{
						cal.add(Calendar.MONTH, nModifiedDateRangeTimeValue);
					}

			return cal.getTime();
		}

		return null;

	}

	public static String getStatisticHTML(String bodyClass, long globalGroupId, long scopeGroupId, List<String> structures, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia, String templateIdArticleRestricted, String templateIdGalleryRestricted,
			String templateIdPollRestricted, String templateIdMultimediaRestricted, int modeArticle, int modeGallery, int modePoll, int modeMultimedia, int startIndex, int numElements, Date modifiedDate, int tabId, long[] categoriesId, String[] qualificationId, String[] layoutIds,
			boolean defaultMode, long modelId, int sectionToShow, HttpServletRequest request, ThemeDisplay themeDisplay, String xmlRequest, Locale locale)
	{
		Date dateI = null;
		if(_log.isDebugEnabled()){
			dateI = new Date();
		}
		
		StringBuffer html = new StringBuffer();
		HttpURLConnection httpConnection = null;

		try
		{
			Date validityDate = GroupMgr.getPublicationDate(scopeGroupId);
			String order = Integer.toString(getStatisticOrder(tabId));
			List<String[]> results = 
			PageContentLocalServiceUtil.getFilterArticles(scopeGroupId, null, structures, startIndex, numElements, 
					 validityDate, false, new String[]{order}, 0, categoriesId, null, qualificationId, layoutIds, 
					 true, Integer.toString(numElements), null, modifiedDate);

			if(_log.isTraceEnabled()){
				Date date2 = new Date();
				_log.trace("tracking/RankingUtil getFilterArticles: " + (date2.getTime() - dateI.getTime()) + " ms");
			}
				
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, results.size());
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, results.size());
			
			//se pide el qualificationName para insertarlo en el request
			String qualifId = qualificationId != null && qualificationId.length > 0 ? qualificationId[0]  :  "";
			String qualificationName = QualificationTools.getQualificationName( qualifId);
			
			request.setAttribute( VelocityContext.VELOCITYVAR_ITER_QUALIFICATION, qualificationName );
			

			boolean isOrderShared = IterKeys.ORDER_TO_OPERATION.get(order).contains(Integer.toString(IterKeys.OPERATION_SHARED));

			for (int i = 0; i < results.size(); i++)
			{
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, results.get(i)[0].toString());

				if( isOrderShared )
				{
					//se usan -2, -3. -4 porque el result aparece con longitud +1, con el ultimo valor a null
					request.setAttribute(VelocityContext.VELOCITYVAR_ITER_FB_COUNT, results.get(i)[results.get(i).length-4]);
					request.setAttribute(VelocityContext.VELOCITYVAR_ITER_GP_COUNT, results.get(i)[results.get(i).length-3]);
					request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TW_COUNT, results.get(i)[results.get(i).length-2]);
				}
				
				try
				{
					html.append(getContent( bodyClass,globalGroupId, scopeGroupId, templateIdArticle, templateIdGallery, templateIdPoll, templateIdMultimedia, templateIdArticleRestricted, templateIdGalleryRestricted, templateIdPollRestricted, templateIdMultimediaRestricted, modeArticle, modeGallery, modePoll,
							modeMultimedia, layoutIds, defaultMode, modelId, sectionToShow, request, themeDisplay, xmlRequest, locale, article, results.size(), i));
				}
				catch (Exception e)
				{
					if (article != null && Validator.isNotNull(article.getArticleId()))
						_log.error("Current articleId: " + article.getArticleId());

					_log.error(e.toString());
					_log.trace(e);
				}
				finally
				{
					// Borrar los contadores de redes sociales para asegurar que no se inyectan valores falsos en las otras pestañas
					if( isOrderShared )
					{
						request.removeAttribute( VelocityContext.VELOCITYVAR_ITER_FB_COUNT );
						request.removeAttribute( VelocityContext.VELOCITYVAR_ITER_GP_COUNT );
						request.removeAttribute( VelocityContext.VELOCITYVAR_ITER_TW_COUNT );
					}
				}

			}

		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
		if(_log.isDebugEnabled())
		{
			Date dateF = new Date();
			_log.debug("tracking/RankingUtil.getStatisticHTML: " + (dateF.getTime() - dateI.getTime()) + " ms");
		}
		return html.toString();
	}

	private static int getStatisticOrder(int tabId)
	{
		switch(tabId){
			case AnalayzerConstants.TABCOMMENT:
				return IterKeys.ORDER_COMMENT;
			case AnalayzerConstants.TABSHARED:
				return IterKeys.ORDER_SHARED;
			default:
				return 0;
		}
		
	}
}
