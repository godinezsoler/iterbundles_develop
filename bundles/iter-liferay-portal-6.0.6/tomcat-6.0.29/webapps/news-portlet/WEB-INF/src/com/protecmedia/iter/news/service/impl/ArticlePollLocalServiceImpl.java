/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.news.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.polls.QuestionChoiceException;
import com.liferay.portlet.polls.QuestionDescriptionException;
import com.liferay.portlet.polls.QuestionExpirationDateException;
import com.liferay.portlet.polls.QuestionTitleException;
import com.liferay.portlet.polls.model.PollsChoice;
import com.liferay.portlet.polls.model.PollsQuestion;
import com.liferay.portlet.polls.model.PollsVote;
import com.liferay.portlet.polls.service.PollsChoiceLocalServiceUtil;
import com.liferay.portlet.polls.service.PollsQuestionLocalServiceUtil;
import com.liferay.portlet.polls.service.PollsVoteLocalServiceUtil;
import com.liferay.util.survey.IterSurveyModel;
import com.liferay.util.survey.IterSurveyModel.IterSurveyChoiceModel;
import com.protecmedia.iter.news.NoSuchArticlePollException;
import com.protecmedia.iter.news.model.ArticlePoll;
import com.protecmedia.iter.news.service.ArticlePollLocalServiceUtil;
import com.protecmedia.iter.news.service.base.ArticlePollLocalServiceBaseImpl;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ArticlePollLocalServiceImpl extends ArticlePollLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(ArticlePollLocalServiceImpl.class);
	
	/*
	 * ADD
	 */
	public ArticlePoll addArticlePoll(long groupId, String contentId, long pollId) throws SystemException {
		
		long id = counterLocalService.increment();
		
		ArticlePoll articlePoll = articlePollPersistence.create(id);
		
		articlePoll.setGroupId(groupId);
		articlePoll.setContentId(contentId);
		articlePoll.setPollId(pollId);
		
		articlePollPersistence.update(articlePoll, false);
		
		return articlePoll;
	}
	
	/*
	 * DELETE
	 */
	public boolean deleteArticlePoll (long groupId, String contentId) {
		try {
			articlePollPersistence.removeByGroupContentIdFinder(groupId, contentId);
		} catch (NoSuchArticlePollException e) {
			return false;
		} catch (SystemException e) {
			return false;
		}
		return true;
	}
	
	
	/*
	 * GETTERS 
	 */
	public ArticlePoll getArticlePollByArticleId(long groupId, String contentId) throws SystemException {
		ArticlePoll ap = articlePollPersistence.fetchByGroupContentIdFinder(groupId, contentId);
		return ap;
	}	
	
	public long getPoll(long groupId, String contentId) throws SystemException {		
		ArticlePoll ap = articlePollPersistence.fetchByGroupContentIdFinder(groupId, contentId);
		
		if (ap != null) {
			return ap.getPollId();
		}
		return -1;		
	}
		
	public String getPollResults(long groupId, String contentId) throws SystemException 
	{
		ArticlePoll ap = articlePollPersistence.fetchByGroupContentIdFinder(groupId, contentId);
		
		if (ap != null) 
		{
			// ITER-629 NAI171965 Orden de resultados de votaciones no es igual a la pregunta.
			// Se sobreescribe el método PollsChoiceLocalServiceUtil.getChoices porque se llama desde varios sitios, incluso desde plantillas globales (/GLOBAL/PROYECT #macro(initPoll))
			List<PollsChoice> choices = PollsChoiceLocalServiceUtil.getChoices(ap.getPollId());
			
			StringBuffer sb = new StringBuffer();
						
			sb.append("<root>");

			for (PollsChoice choice : choices) 
			{
				sb.append("<choice votes=\"");
				sb.append(choice.getVotesCount());
				sb.append("\" name=\"");
				sb.append(choice.getName());
				sb.append("\" />");
			}
			
			sb.append("</root>");
			
			return sb.toString();
		}  
			
		return null;
	}
	
	public String getPollResultsAsJson(long groupId, String contentId) throws SystemException 
	{
		ArticlePoll ap = articlePollPersistence.fetchByGroupContentIdFinder(groupId, contentId);
		return getPollResultsAsJson(groupId, ap.getPollId());
	}
	
	public String getPollResultsAsJson(Long groupId, Long pollId) throws SystemException 
	{
		return getPollResultsAsJson(groupId.longValue(), pollId.longValue());
	}
	
	public String getPollResultsAsJson(long groupId, long pollId) throws SystemException 
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		
		// ITER-629 NAI171965 Orden de resultados de votaciones no es igual a la pregunta.
		// Se sobreescribe el método PollsChoiceLocalServiceUtil.getChoices porque se llama desde varios sitios, incluso desde plantillas globales (/GLOBAL/PROYECT #macro(initPoll))
		List<PollsChoice> choices = PollsChoiceLocalServiceUtil.getChoices(pollId);
		JSONArray jsonChoices = JSONFactoryUtil.createJSONArray();
		for (PollsChoice choice : choices)
		{
			JSONObject jsonChoice = JSONFactoryUtil.createJSONObject(); 
			jsonChoice.put("choiceId",  choice.getChoiceId());
			jsonChoice.put("votes", choice.getVotesCount());
			jsonChoices.put(jsonChoice);
		}
		result.put("choices", jsonChoices);
		
		return result.toString();
	}
	
	public Document addPollInfoToArticleContent(String articleId, Document articleContent) throws SystemException
	{
		List<IterSurveyModel> surveys = IterSurveyModel.getAll(articleId);
		if (surveys.size() > 0)
		{
			for (IterSurveyModel survey : surveys)
			{
				// Recupera el nodo
				Node questionElement = articleContent.selectSingleNode("/root/dynamic-element[@name='Question' and @questionid='" + survey.getSurveyId() + "']");
				if (questionElement != null)
				{
					// Añade las fechas
					if (survey.getOpenDate() > 0L)
						((Element) questionElement).addAttribute("opendate", String.valueOf(survey.getOpenDate()));
					if (survey.getCloseDate() > 0L)
						((Element) questionElement).addAttribute("closedate", String.valueOf(survey.getCloseDate()));
					
					// Procesa las respuestas
					List<IterSurveyChoiceModel> choices = survey.getChoices();
					for (IterSurveyChoiceModel choice : choices)
					{
						// Obtiene el nodo
						Node choiceElement = questionElement.selectSingleNode("dynamic-element[@name='Answer' and @choiceid = '" + choice.getChoiceId() + "']");
						if (choiceElement != null)
						{
							// Añade los votos
							((Element) choiceElement).addAttribute("votes", String.valueOf(choice.getVotes()));
						}
					}
				}
			}
		}
		else
		{
			// Obtiene el nodo Question
			Node questionElement = articleContent.selectSingleNode("/root/dynamic-element[@name='Question']");
			if (Validator.isNotNull(questionElement))
			{
				// Obtiene la encuesta
				ArticlePoll articlePoll = ArticlePollLocalServiceUtil.getArticlePollByArticleId(GroupMgr.getGlobalGroupId(), articleId);
				if (Validator.isNotNull(articlePoll))
				{
					// Inserta el questionId
					((Element) questionElement).addAttribute("questionid", String.valueOf(articlePoll.getPollId()));
					
					// Obtiene los nodos Answer
					List<Node> choiceElements = questionElement.selectNodes("dynamic-element[@name='Answer']");
					if (Validator.isNotNull(choiceElements) && choiceElements.size() > 0)
					{
						// Obtiene las respuestas
						List<PollsChoice> pollChoices = PollsChoiceLocalServiceUtil.getChoices(articlePoll.getPollId());
						
						if (Validator.isNotNull(pollChoices) && pollChoices.size() == choiceElements.size())
						{
							for (int i = 0; i < pollChoices.size(); i++)
							{
								PollsChoice choice = pollChoices.get(i);
								Element choiceElement = ((Element) choiceElements.get(i));
								
								// Inserta el atributo choiceId
								choiceElement.addAttribute("choiceid", String.valueOf(choice.getChoiceId()));
								choiceElement.addAttribute("votes", String.valueOf(choice.getVotesCount()));
							}
						}
					}
				}
			}
		}
		return articleContent;
	}
	
	/*
	 * MANAGE LIFERAY POLLS
	 */
	
	/**
	 * Update a Poll
	 */
	public long updatePoll(String articleId, String title, long userId, long groupId, String content,
			int expirationDateMonth, int expirationDateDay,
			int expirationDateYear, int expirationDateHour,
			int expirationDateMinute, boolean neverExpire) {
		
		ServiceContext serviceContextPoll = new ServiceContext();
		serviceContextPoll.setCommand("update");
		serviceContextPoll.setAddCommunityPermissions(true);
		serviceContextPoll.setAddGuestPermissions(true);
		serviceContextPoll.setScopeGroupId(groupId);
		serviceContextPoll.setUserId(userId);

		Map<Locale, String> headlineMap = new HashMap<Locale, String>();
		Map<Locale, String> titleMap = new HashMap<Locale, String>();
		
		//Control de title vacio (QuestionTitleException)
		if (Validator.isNull(title)) {
			title = "POLL_" + (new Date()).getTime();
		}
		titleMap.put(LocaleUtil.getDefault(), title);
		
		List<PollsChoice> choices = new ArrayList<PollsChoice>();

		Document doc = null;

		try {
			
			ArticlePoll articlePoll = ArticlePollLocalServiceUtil.getArticlePollByArticleId(groupId, articleId);
			long questionId = articlePoll.getPollId();
			
			doc = SAXReaderUtil.read(content);

			Element root = doc.getRootElement();

			for (Element el : root.elements()) {
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("Headline")) {
					headlineMap = getElementText(el);
					
				} else if (elName.equals("Question")) {
					int cont = 0;
					for (Element eleAnswers : el.elements("dynamic-element")) {

						String eleAnswersName = eleAnswers.attributeValue(
								"name", StringPool.BLANK);

						if (eleAnswersName.equals("Answer")) {

							Map<Locale, String> descriptionChoiceMap = getElementText(eleAnswers);
							//Control de Preguntas Vacias
							String value = descriptionChoiceMap.get(LocaleUtil.getDefault());
							if (value.equalsIgnoreCase("")){
								descriptionChoiceMap = titleMap;
							}

							long id = CounterLocalServiceUtil.increment();

							// choices
							PollsChoice choice = PollsChoiceLocalServiceUtil
									.createPollsChoice(id);

							choice.setName(String.valueOf(cont));
							choice.setDescriptionMap(descriptionChoiceMap);

							choices.add(choice);
						}
						cont++;
					}

				}
			}
			
			//Control de Headline Vacio
			String value = headlineMap.get(LocaleUtil.getDefault());
			if ((value == null) || (value.equalsIgnoreCase(""))){
				headlineMap = titleMap;
			}
				
			//Actualizo encuesta
			PollsQuestion question = updateQuestion
			(userId, questionId, titleMap, headlineMap, 
			expirationDateMonth, expirationDateDay, expirationDateYear, expirationDateHour, 
			expirationDateMinute, neverExpire, choices, serviceContextPoll);
			
			return question.getQuestionId();
			
		} catch(QuestionTitleException qte){
			_log.error("QuestionPoll has no title");
		} catch(QuestionDescriptionException qde){
			_log.error("QuestionPoll has no description");
		} catch(QuestionChoiceException qce){
			qce.printStackTrace();
		} catch (Exception e) {
			_log.error(e);
		}

		return -1;
	}
	
	public long createPoll(String title, long userId, long groupId, String content,
			int expirationDateMonth, int expirationDateDay,
			int expirationDateYear, int expirationDateHour,
			int expirationDateMinute, boolean neverExpire) {

		ServiceContext serviceContextPoll = new ServiceContext();
		serviceContextPoll.setCommand("add");
		serviceContextPoll.setAddCommunityPermissions(true);
		serviceContextPoll.setAddGuestPermissions(true);
		serviceContextPoll.setScopeGroupId(groupId);
		serviceContextPoll.setUserId(userId);

		Map<Locale, String> headlineMap = new HashMap<Locale, String>();
		Map<Locale, String> titleMap = new HashMap<Locale, String>();
		
		//Control de title vacio (QuestionTitleException)
		if (Validator.isNull(title)) {
			title = "POLL_" + (new Date()).getTime();
		}
		titleMap.put(LocaleUtil.getDefault(), title);
		
		List<PollsChoice> choices = new ArrayList<PollsChoice>();

		Document doc = null;

		try {
			doc = SAXReaderUtil.read(content);

			Element root = doc.getRootElement();

			for (Element el : root.elements()) {
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("Headline")) {
					headlineMap = getElementText(el);
					
				} else if (elName.equals("Question")) {
					int cont = 0;
					for (Element eleAnswers : el.elements("dynamic-element")) {

						String eleAnswersName = eleAnswers.attributeValue(
								"name", StringPool.BLANK);

						if (eleAnswersName.equals("Answer")) {

							Map<Locale, String> descriptionChoiceMap = getElementText(eleAnswers);
							//Control de Preguntas Vacias
							String value = descriptionChoiceMap.get(LocaleUtil.getDefault());
							if (value.equalsIgnoreCase("")){
								descriptionChoiceMap = titleMap;
							}

							long id = CounterLocalServiceUtil.increment();

							// choices
							PollsChoice choice = PollsChoiceLocalServiceUtil
									.createPollsChoice(id);

							choice.setName(String.valueOf(cont));
							choice.setDescriptionMap(descriptionChoiceMap);

							choices.add(choice);
						}
						cont++;
					}

				}
			}
			
			//Control de Headline Vacio
			String value = headlineMap.get(LocaleUtil.getDefault());
			if ((value == null) || (value.equalsIgnoreCase(""))){
				headlineMap = titleMap;
			}
				
			//Añado encuesta usando los servicios de Brian Chan
			
			PollsQuestion question = PollsQuestionLocalServiceUtil.addQuestion(
					userId, titleMap, headlineMap, expirationDateMonth,
					expirationDateDay, expirationDateYear, expirationDateHour,
					expirationDateMinute, neverExpire, choices,
					serviceContextPoll);
			
			return question.getQuestionId();
			
		} catch(QuestionTitleException qte){
			_log.error("QuestionPoll has no title");
		} catch(QuestionDescriptionException qde){
			_log.error("QuestionPoll has no description");
		} catch(QuestionChoiceException qce){
			_log.error("QuestionPoll has no choices");
		} catch (Exception e) {
			_log.error(e);
		}

		return -1;
	}
	
	/**
	 * Create a Poll
	 */
	private PollsQuestion updateQuestion(
			long userId, long questionId, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, int expirationDateMonth,
			int expirationDateDay, int expirationDateYear,
			int expirationDateHour, int expirationDateMinute,
			boolean neverExpire, List<PollsChoice> choices,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		User user = UserLocalServiceUtil.getUser(userId);

		Date expirationDate = null;

		if (!neverExpire) {
			expirationDate = PortalUtil.getDate(
				expirationDateMonth, expirationDateDay, expirationDateYear,
				expirationDateHour, expirationDateMinute, user.getTimeZone(),
				new QuestionExpirationDateException());
		}

		validate(titleMap, descriptionMap, choices);

		PollsQuestion question = PollsQuestionLocalServiceUtil.getPollsQuestion(questionId);

		question.setModifiedDate(serviceContext.getModifiedDate(null));
		question.setTitleMap(titleMap);
		question.setDescriptionMap(descriptionMap);
		question.setExpirationDate(expirationDate);

		PollsQuestionLocalServiceUtil.updatePollsQuestion(question);
		
		LocaleUtil.getDefault();
		if (choices != null) {
			int oldChoicesCount = PollsChoiceLocalServiceUtil.getChoicesCount(questionId);
			//Borrado
			if (oldChoicesCount > choices.size()) {
				deleteOldChoices(questionId,choices);
			}
			//Añadir o actualizar
			//else{
				List<PollsChoice> allChoices;
				for (PollsChoice choice : choices) {
					String choiceName = choice.getName();
					String choiceDescription = choice.getDescription();
					allChoices = PollsChoiceLocalServiceUtil.getChoices(questionId);
					choice = getChoiceByName(allChoices,choiceName);
					if (choice == null) {
						PollsChoiceLocalServiceUtil.addChoice(questionId, choiceName, choiceDescription, new ServiceContext());
					}
					else {
						PollsChoiceLocalServiceUtil.updateChoice(choice.getChoiceId(), questionId, choiceName, choiceDescription);
					}
				}
			//}
		}
		return question;
	}
	
	private PollsChoice getChoiceByName(List<PollsChoice> allChoices, String name){
		
		PollsChoice choice = null;
		boolean founded = false;
		int counter = 0;
		while (!founded && (counter < allChoices.size())){
			if (allChoices.get(counter).getName().equalsIgnoreCase(name)){
				choice = allChoices.get(counter);
				founded = true;
			}
			counter++;
		}
		return choice;
	}
	
	private boolean mustBeDeleted(PollsChoice choice, List<PollsChoice> newChoices){
		boolean founded = false;
		boolean mustBe = true;
		int counter = 0;
		Locale locale = LocaleUtil.getDefault();
		while (!founded && counter < newChoices.size()){
			if (newChoices.get(counter).getDescription(locale).equalsIgnoreCase(choice.getDescription(locale))){
				mustBe = false;
				founded = true;
			}
			counter++;
		}
		return mustBe;
	}
	
	private List<PollsChoice> orderChoices(long questionId){
		
		List<PollsChoice> orderedChoices = new ArrayList<PollsChoice>();
		List<PollsChoice> actualChoices = null;
		PollsChoice minor = null;
		try {
			actualChoices = PollsChoiceLocalServiceUtil.getChoices(questionId);
			for (int a=0;a<actualChoices.size();a++){
				minor = actualChoices.get(a);
				for (int b=a+1;b<actualChoices.size();b++){
					if (Integer.valueOf(actualChoices.get(a).getName())>Integer.valueOf(actualChoices.get(b).getName())){
						minor = actualChoices.get(b);
					}
				}
				orderedChoices.add(minor);
			}
		} catch (SystemException e) {
			_log.error("Error ordering choices");
		}
		return orderedChoices;
	}
	
	private void processNameColumn(long questionId){
		int cont = 1;
		try {
			List<PollsChoice> orderedChoices = orderChoices(questionId);
			for (PollsChoice choice : orderedChoices){
				choice.setName(String.valueOf(cont));
				PollsChoiceLocalServiceUtil.updatePollsChoice(choice);
				cont++;
			}
		} catch (SystemException e) {
			_log.error("Error ordering the name column");
		}
	}
	
	private void deleteOldChoices(long questionId, List<PollsChoice>newChoices){
		try {
			List<PollsChoice> actualChoices = PollsChoiceLocalServiceUtil.getChoices(questionId);
			for (PollsChoice choice : actualChoices){
				if (mustBeDeleted(choice,newChoices)){
					PollsChoiceLocalServiceUtil.deletePollsChoice(choice.getChoiceId());
					List<PollsVote> voteChoices = PollsVoteLocalServiceUtil.getChoiceVotes(choice.getChoiceId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);
					for (PollsVote vote : voteChoices){
						PollsVoteLocalServiceUtil.deletePollsVote(vote.getVoteId());
					}
				}
			}
			processNameColumn(questionId);
		} catch (SystemException e) {
			_log.error("Error deleting old votes");
		} catch (PortalException e1) {
			_log.error("Error deleteing old choices");
		}
	}

	private void validate(
			Map<Locale, String> titleMap, Map<Locale, String> descriptionMap,
			List<PollsChoice> choices)
		throws PortalException {

		Locale locale = LocaleUtil.getDefault();

		String title = titleMap.get(locale);

		if (Validator.isNull(title)) {
			throw new QuestionTitleException();
		}

		String description = descriptionMap.get(locale);

		if (Validator.isNull(description)) {
			throw new QuestionDescriptionException();
		}

		if ((choices != null) && (choices.size() < 2)) {
			throw new QuestionChoiceException();
		}

		if (choices != null) {
			for (PollsChoice choice : choices) {
				String choiceDescription = choice.getDescription(locale);

				if (Validator.isNull(choiceDescription)) {
					throw new QuestionChoiceException();
				}
			}
		}
	}

	/**
	 * GET a Element Map
	 * 
	 * @param el
	 * @return
	 */
	private Map<Locale, String> getElementText(Element el) {
		Map<Locale, String> texts = new HashMap<Locale, String>();

		List<Element> contents = el.elements("dynamic-content");
		Locale locale = LocaleUtil.getDefault();

		for (Element ele : contents) {
			texts.put(locale, ele.getText());
		}

		return texts;
	}

		
}
