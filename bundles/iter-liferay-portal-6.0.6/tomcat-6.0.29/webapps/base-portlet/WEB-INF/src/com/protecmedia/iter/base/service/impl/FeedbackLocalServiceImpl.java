/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
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

package com.protecmedia.iter.base.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.FeedbackLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

/**
 * The implementation of the feedback local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.FeedbackLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.FeedbackLocalServiceUtil} to access the feedback local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.FeedbackLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.FeedbackLocalServiceUtil
 */

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FeedbackLocalServiceImpl extends FeedbackLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(FeedbackLocalServiceImpl.class);
	
	/* */
	
	private static final String GET_FEEDBACK_CONF	= new StringBuilder()
	.append(" SELECT q.questionid, q.label AS questionlabel, q.thanksmsg, q.actcookiesmsg, q.existsvotemsg, q.daysdeletepolicy, q.anonymousrating, ch.choiceid, ch.label AS choicelabel, ch.choiceorder  \n")
	.append(" FROM feedbackquestion q	\n")
	.append(" LEFT JOIN feedbackchoice ch ON (q.questionid = ch.questionid AND ch.deletepending = 0 ) \n")
	.append(" WHERE q.groupid = %s ORDER BY ch.choiceorder" ).toString();
	
	
	private static final String INSERT_QUESTION	= new StringBuilder()
	.append(" INSERT INTO feedbackquestion(questionid, groupid, label, thanksmsg, actcookiesmsg, existsvotemsg, daysdeletepolicy, anonymousrating, modifieddate, publicationdate) \n")
	.append(" \t values('%s',  %s, '%s', '%s', '%s', '%s', %s, %s, '%s', NULL )")
	.append(" ON DUPLICATE KEY UPDATE label=VALUES(label), thanksmsg=VALUES(thanksmsg), actcookiesmsg=VALUES(actcookiesmsg), existsvotemsg=VALUES(existsvotemsg), daysdeletepolicy=VALUES(daysdeletepolicy), anonymousrating=VALUES(anonymousrating), modifieddate=VALUES(modifieddate)   ").toString();
	
	
	private static final String INSERT_CHOICE	= new StringBuilder()
	.append(" INSERT INTO feedbackchoice(choiceid, questionid, label, choiceorder, deletepending, modifieddate, publicationdate)  \n")
	.append(" SELECT '%s', '%s', '%s', IFNULL(MAX(choiceorder),0)+1, %s ,'%s', NULL FROM feedbackchoice")
	.append(" WHERE questionid= '%s' ").toString();

	private static final String UPDATE_CHOICE	=  new StringBuilder()
				.append(" UPDATE feedbackchoice \n")
				.append(" SET label = '%s', modifiedDate='%s'    \n")	
				.append(" WHERE choiceid = '%s'").toString();
	
	private static final String GET_CHOICE_ORDER	= new StringBuilder()
				.append(" SELECT choiceorder  \n")
				.append(" FROM feedbackchoice 	\n")
				.append(" WHERE choiceid =  '%s'	\n").toString();
	
	private final String GET_UPDATED_CHOICES = "SELECT choiceid, questionid, choiceorder FROM feedbackchoice WHERE choiceorder>=%d AND choiceorder<=%d AND deletepending=0 AND questionid='%s' ORDER BY choiceorder";
	private final String UPDATE_CHOICES_PRIORITY = "UPDATE feedbackchoice SET choiceorder=choiceorder %s 1, modifiedDate='%s' WHERE questionid='%s' AND choiceid!='%s' AND choiceorder>=%d AND choiceorder<=%d";
	
	private static final String UPDATE_DELETEPENDING_CHOICES	= 	new StringBuilder()
	.append(" UPDATE feedbackchoice \n")	
	.append(" SET deletepending = 1, modifiedDate='%s'  \n")	
	.append(" WHERE choiceid IN %s AND publicationdate IS NOT NULL\n").toString();
	
	private static final String DELETE_CHOICES_NO_PUBLISHED	= 	new StringBuilder()
	.append(" DELETE FROM feedbackchoice \n")	
	.append(" WHERE choiceid IN %s AND publicationdate IS NULL\n").toString();
	
	

    private static ReentrantLock _lock = new ReentrantLock();
	
    
    private static final String GET_CURRENTCHOICES_BY_GROUP = new StringBuilder(
		"-- No se soporta totalmente tener varias preguntas por grupo, se asegura entonces que se trabaja 		\n").append(
		"-- con la última modificada																			\n").append(
		"SELECT question.questionId, question.label questionLabel, choiceid, feedbackchoice.label choiceLabel	\n").append(
		"FROM (SELECT questionId, label 																		\n").append(
		"			FROM feedbackquestion 																		\n").append(
		"				WHERE groupId = %d																		\n").append(
		"				ORDER BY modifieddate DESC LIMIT 1) question											\n").append(
		"LEFT JOIN feedbackchoice ON feedbackchoice.questionId = question.questionId							\n").toString();
    
	private static final String PUBLISH_FEEDBACK_A_ALL = new StringBuilder(
		"SELECT ch.choiceid, ch.label, ch.choiceorder, ch.deletepending, q.questionid 							\n").append(
		"FROM feedbackquestion q 																				\n").append(
		"INNER JOIN feedbackchoice ch ON q.questionid=ch.questionid 											\n").append(
		"	WHERE q.groupid=%d 																					\n").toString();
	
	private static final String _PUBLISH_FEEDBACK_A_PUBLISHABLE = "		AND (ch.publicationdate IS NULL OR ch.modifieddate > ch.publicationdate)";
	private static final String PUBLISH_FEEDBACK_A_PUBLISHABLE 	= PUBLISH_FEEDBACK_A_ALL.concat(_PUBLISH_FEEDBACK_A_PUBLISHABLE);
	private static final String PUBLISH_FEEDBACK_A_EXPORTABLE 	= PUBLISH_FEEDBACK_A_ALL.concat(" AND ch.deletepending=0 ");
	
	
	private static final String PUBLISH_FEEDBACK_Q_ALL = new StringBuilder(
		"SELECT questionid, label AS questionlabel, thanksmsg, actcookiesmsg, existsvotemsg, daysdeletepolicy, anonymousrating 	\n").append(
		"FROM feedbackquestion 																					\n").append(
		"	WHERE groupid=%d 																					\n").toString();
	
	private static final String _PUBLISH_FEEDBACK_Q_PUBLISHABLE = "		AND (publicationdate IS NULL OR modifieddate > publicationdate)";
	private static final String PUBLISH_FEEDBACK_Q_PUBLISHABLE  = PUBLISH_FEEDBACK_Q_ALL.concat(_PUBLISH_FEEDBACK_Q_PUBLISHABLE);

	private static final String	DELETE_FEEDBACK_CHOICE	= "DELETE FROM feedbackchoice WHERE choiceid IN ('%s')";
	
	private static final String IMPORT_CHOICES = new StringBuilder()
				.append(" INSERT INTO feedbackchoice(choiceid, questionid, label, choiceorder, deletepending, modifieddate, publicationdate) \n")
				.append(" VALUES \n")
				.append(" %s \n")
				.append(" ON DUPLICATE KEY UPDATE \n")
				.append(" label = VALUES(label), choiceorder=VALUES(choiceorder), modifieddate=VALUES(modifieddate)")
				.toString();
	
	private static final String UPDATE_QUESTION_PUBDATE = "UPDATE feedbackquestion SET publicationdate='%s' WHERE questionid IN ('%s')";
	private static final String UPDATE_CHOICES_PUBDATE = "UPDATE feedbackchoice SET publicationdate='%s' WHERE choiceid IN ('%s')";
	
	/**
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1292?focusedCommentId=61107&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-61107
	 * La configuración de "Criterior de visibilidad" será por portlet
	 */
	public Document getFeedbackDisplayConf(long groupid, String questionid) throws NoSuchMethodException, SecurityException, ServiceError
	{
		ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNSUPPORTED_ZYX);
		return null;
	}
	
	/**
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1292?focusedCommentId=61107&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-61107
	 * La configuración de "Criterior de visibilidad" será por portlet
	 */
	public String addOrUpdtFeedbackDisplay(String xmlData, long groupid, String questionid) throws ServiceError, DocumentException, IOException, SQLException
	{
		ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNSUPPORTED_ZYX);
		return null;
	}
	
	 /* ************************************** */
			
	/* ******************************************* *
	 * OBTENCIÓN DE CONFIGURACIÓN PARA ITERADMIN *
	 * ******************************************* */
	public Document getFeedbackConf(long groupid) throws NoSuchMethodException, SecurityException, ServiceError
	{
		
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		
		List<String> elementList = new ArrayList<String>();
		elementList.add("questionlabel");
		elementList.add("choicelabel");
		elementList.add("thanksmsg");
		elementList.add("actcookiesmsg");
		elementList.add("existsvotemsg");
	
		
		String []elements = new String[elementList.size()];
		elementList.toArray(elements);
		
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FEEDBACK_CONF, groupid), true, "feedback" ,"choice", elements );
		Element dataRoot = dom.getRootElement();
		
		List<Node> choicesNodes = dataRoot.selectNodes("//choice");
		List<String> questionids = new ArrayList<String>(); 
		
		/*se forma una lista de questionids. 
		 * Actualmente la lista tendrá 1 elemento porque sólo hay una pregunta por sitio web, pero más adelante puede haber varias */
		for(int i= 0; i < choicesNodes.size(); i++)
		{
			String questionid = XMLHelper.getTextValueOf (choicesNodes.get(i), "@questionid");
			if( !questionids.contains(questionid) )
				questionids.add(questionid);
		}
		
		for(int j= 0; j < questionids.size(); j++ )
		{
			// se recorre la lista de questionids y para cada uno de ellos se crea un xml con la información de la pregunta y sus respectivas respuestas
			String questionid = questionids.get(j);
			List<Node> choicesByQuestion = dataRoot.selectNodes( String.format("//choice[@questionid='%s']", questionid ));
			
			Element question = createElemQuestion( choicesByQuestion.get(0) );
			
			//se crea xml con información de todas las respuestas
			Document docChoices = SAXReaderUtil.createDocument();
			Element choices = docChoices.addElement("choices");
			
			for(int k=0; k < choicesByQuestion.size(); k++)
			{
				Node choiceNode = choicesByQuestion.get(k);
				if(k!=0)
					rmvQuestionData(choiceNode);
						
				choiceNode.detach();
				if(!choiceNode.getStringValue().equals(StringPool.BLANK))
					choices.add(choiceNode);	
			}
	
			question.add(choices);	
			dataRoot.add(question);
		}
		
		return dom;
	}
	
	//extrae del nodo pasado por parámetro los atributos/elementos con información de la pregunta y retorna un Element con esa información
	private Element createElemQuestion(Node node)
	{
		//creo el nodo question con sus hijos choices/choice de cada @questionid diferente
		String questionid = XMLHelper.getTextValueOf (node, "@questionid");
		long daysdeletepolicy = XMLHelper.getLongValueOf (node, "@daysdeletepolicy");
		String anonymousrating = XMLHelper.getTextValueOf (node, "@anonymousrating");
		Node questionlabel =  node.selectSingleNode("questionlabel"); 
		
		Node thanksmsg =  node.selectSingleNode("thanksmsg"); 
		Node actcookiesmsg =  node.selectSingleNode("actcookiesmsg"); 
		Node existsvotemsg =  node.selectSingleNode("existsvotemsg"); 
		rmvQuestionData(node);
		
		
		//create nodo question
		Document docQuestion = SAXReaderUtil.createDocument();
		Element question = docQuestion.addElement("question");
		question.addAttribute("questionid", questionid);
		question.addAttribute("daysdeletepolicy", String.valueOf(daysdeletepolicy));
		question.addAttribute("anonymousrating", anonymousrating);
		question.add(questionlabel);
		question.add(thanksmsg);
		question.add(actcookiesmsg);
		question.add(existsvotemsg);
		
		return question;
	}
	
	//Elimina del parámetro 'node' los atributos/elementos con información de la pregunta, para que 'node' sólo contenga información de la respuesta
	private void rmvQuestionData(Node node)
	{
		Element choice = (Element)node;
		choice.remove(choice.attribute("questionid"));
		choice.remove(choice.attribute("daysdeletepolicy"));
		choice.remove(choice.element("questionlabel"));
		choice.remove(choice.element("thanksmsg"));
		choice.remove(choice.element("actcookiesmsg"));
		choice.remove(choice.element("existsvotemsg"));
	}
	
	 /* ************************************** */

	
	public String setQuestion(String xmlData) throws DocumentException, ServiceError, IOException, SQLException
	{
        Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		long groupid = XMLHelper.getLongValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		Node question = dataRoot.selectSingleNode("question");
		
		insertOrUpdateQuestion(question, groupid);
		
		return dataRoot.asXML();
	}
	
	private void insertOrUpdateQuestion(Node question, long groupid ) throws ServiceError, IOException, SQLException
	{
		Node questionlabel 		= question.selectSingleNode("questionlabel");
		Node thanksmsg 			= question.selectSingleNode("thanksmsg");
		Node actcookiesmsg 		= question.selectSingleNode("actcookiesmsg");
		Node existsvotemsg 		= question.selectSingleNode("existsvotemsg");
		
		String questionStr 		= feedbackText(questionlabel, 	IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_QUESTION_ZYX);
		String thanksmsgStr 	= feedbackText(thanksmsg, 		IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_THANKSMSG_ZYX);
		String actcookiesmsgStr = feedbackText(actcookiesmsg, 	IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_ACTCOOKIESMSG_ZYX);
		String existsvotemsgStr = feedbackText(existsvotemsg, 	IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_EXISTSVOTEMSG_ZYX);
	
		long daysdeletepolicy 	= XMLHelper.getLongValueOf (question, "@daysdeletepolicy");
		ErrorRaiser.throwIfFalse(daysdeletepolicy > 0, IterErrorKeys.XYZ_ITR_E_INVALID_FEEDBACK_DEL_POLICY_ZYX);
		
		long anonymousrating = XMLHelper.getTextValueOf(question, "@anonymousrating").equals("true")?  1 : 0;
		
		String questionid 	  	= XMLHelper.getTextValueOf (question, "@questionid");
		if (Validator.isNull(questionid))
		{
			questionid = SQLQueries.getUUID();
			((Element)question).addAttribute("questionid", questionid);
		}
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_QUESTION, questionid, groupid, questionStr, thanksmsgStr, actcookiesmsgStr, existsvotemsgStr, daysdeletepolicy, anonymousrating, SQLQueries.getCurrentDate()) );
	}
	
	public String addChoice(String xmlData) throws DocumentException, IOException, ServiceError, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String questionid = XMLHelper.getTextValueOf (dataRoot, "question/@questionid");
		ErrorRaiser.throwIfNull(questionid, IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_QUESTION_ID_ZYX);
		
		Node choice = dataRoot.selectSingleNode("choice");
		Node choicelabel = choice.selectSingleNode("choicelabel");
		String choiceStr = feedbackText(choicelabel, IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_CHOICE_ZYX);
		
		String choiceid = SQLQueries.getUUID();
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_CHOICE, choiceid, questionid, choiceStr, 0, SQLQueries.getCurrentDate(), questionid ) );
		
		List<Object> orderResult = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_CHOICE_ORDER, choiceid ));
		String order = orderResult.get(0).toString();
		
		//se añaden los atributos 'choiceid' y 'order' al xml de salida
		Element choiceEl = ((Element)choice);
		choiceEl.addAttribute("choiceid", choiceid);
		choiceEl.addAttribute("choiceorder", order);
		
		return choice.asXML();
	}
	
	public String updateChoice(String xmlData) throws DocumentException, IOException, ServiceError, SQLException
	{
		Node node = SAXReaderUtil.read(xmlData).selectSingleNode("/choice");
		
		String choiceid = XMLHelper.getTextValueOf (node, "@choiceid");
		ErrorRaiser.throwIfNull(choiceid, IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_CHOICE_ID_ZYX);
		
		Node choicelabel = node.selectSingleNode("choicelabel");
		String choiceStr = feedbackText(choicelabel, IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_CHOICE_ZYX);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_CHOICE, choiceStr, SQLQueries.getCurrentDate(), choiceid ) );
		
		return xmlData;
	}
	
	public String updateChoiceOrder( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String retVal = "<rs/>";
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String idquest = XMLHelper.getTextValueOf(node, "@questionid");
		
		//Elemento que vamos a mover
		String choiceId = XMLHelper.getTextValueOf(node, "@choiceid");
		ErrorRaiser.throwIfNull(choiceId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = String.format("SELECT choiceid, choiceorder FROM feedbackchoice WHERE choiceid = '%s'", choiceId);
 		List<Object> item = PortalLocalServiceUtil.executeQueryAsList(sql);
 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_SOURCE_NOT_FOUND_ZYX);
 		long currentPriority = Long.parseLong( ((Object[])item.get(0))[1].toString() );
		String currentField = ((Object[])item.get(0))[0].toString();
 		
		//Elemento de referencia. El elemento a mover quedará encima de este.
		String refid = XMLHelper.getTextValueOf(node, "@refid");
		long refPriority = 0;
		
		if( Validator.isNotNull(refid) && !refid.isEmpty() )
		{
			sql = String.format("SELECT choiceorder FROM feedbackchoice WHERE choiceid = '%s'", refid);
			item = PortalLocalServiceUtil.executeQueryAsList(sql);
	 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
	 		refPriority = Long.parseLong( item.get(0).toString() );
		}
		else
		{
			sql = String.format("SELECT IFNULL(MAX(choiceorder),0) FROM feedbackchoice WHERE questionid='%s'", idquest);
			item = PortalLocalServiceUtil.executeQueryAsList(sql);
	 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
	 		refPriority = Long.parseLong( item.get(0).toString() )+1;
		}
 		
 		long ini = 0;
 		long fin = 0;
 		String oper = "";
 		String updtItemIdx = "";
 		String getReorderedItems = "";
 		String modifiedDate = SQLQueries.getCurrentDate();
 		
 		if( refPriority!=currentPriority )
 		{
 			if( refPriority > currentPriority )
 	 		{
 	 			ini = currentPriority+1;
 	 			fin = refPriority-1;
 	 			oper="-";
 	 			updtItemIdx = String.format("UPDATE feedbackchoice SET choiceorder=%d, modifieddate='%s' WHERE choiceid='%s'", fin, modifiedDate, currentField);
 	 			getReorderedItems = String.format(GET_UPDATED_CHOICES, ini-1, fin, idquest);
 	 		}
 	 		else if ( refPriority < currentPriority )
 	 		{
 	 			ini = refPriority;
 	 			fin = currentPriority-1;
 	 			oper="+";
 	 			updtItemIdx = String.format("UPDATE feedbackchoice SET choiceorder=%d, modifieddate='%s' WHERE choiceid='%s'", ini, modifiedDate, currentField);
 	 			getReorderedItems = String.format(GET_UPDATED_CHOICES, ini, fin+1, idquest);
 	 		}
 	 		
 	 		if( ini <= fin)
 	 		{
	 	 		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_CHOICES_PRIORITY, oper, modifiedDate, idquest, currentField, ini, fin) );
	 	 		
	 	 		PortalLocalServiceUtil.executeUpdateQuery( updtItemIdx );
	 	 		
	 	 		Document result = PortalLocalServiceUtil.executeQueryAsDom( getReorderedItems );
	 	 		retVal = result.asXML();
 	 		}
 		}
 		
 		return retVal;
	}
	
	public String deleteChoices( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@choiceid");
		
		List<Node> nodestoDel = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodestoDel != null && nodestoDel.size() > 0), IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_CHOICE_TO_DEL_ZYX);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_DELETEPENDING_CHOICES, SQLQueries.getCurrentDate(), TeaserMgr.getInClauseSQL(nodestoDel)));
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_CHOICES_NO_PUBLISHED,  TeaserMgr.getInClauseSQL(nodestoDel)));
		
		return xmlData;
	}
	
	private String feedbackText(Node nodeText, String errCode ) throws ServiceError
	{
		String text = nodeText.getStringValue();
		ErrorRaiser.throwIfFalse( !text.equals(StringPool.BLANK), errCode);	
		
		text = StringEscapeUtils.escapeSql(CDATAUtil.strip(text));
		return text;
	}
	
	public void publishFeedbackConf(String groupId) throws NoSuchMethodException, SecurityException, DocumentException, NumberFormatException, PortalException, SystemException, ServiceError, ClientProtocolException, IOException, SQLException
	{
		ErrorRaiser.throwIfFalse( PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_XPORTCONTENT_ALL_FAILED_ZYX);
		
		if(_lock.tryLock())
		{
			try
			{
				//Se recupera la configuración de la publicación
				Group scopeGroup = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId));
				
				//Generamos el .xml de exportación
				Document dom = generatePublishDom(scopeGroup, false);
				Element root  = dom.getRootElement();
				if(root.selectNodes("/rs/row").size() > 0)
				{
					executeJSONRemoteCalls(dom);
					updatePublicationDateContents(dom);
				}
			}
			finally
			{
				_lock.unlock();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
	}
	
	private Document generatePublishDom(Group scopeGroup, boolean exportMode) throws DocumentException, NoSuchMethodException, SecurityException, ServiceError
	{
		Document dom = SAXReaderUtil.read("<rs/>");
		dom.setXMLEncoding("ISO-8859-1");
		Element rs = dom.getRootElement();
		
		long groupId = scopeGroup.getGroupId();
		if (!exportMode)
			rs.addAttribute("groupname", scopeGroup.getName());
		
		String query = String.format( exportMode ? PUBLISH_FEEDBACK_Q_ALL : PUBLISH_FEEDBACK_Q_PUBLISHABLE, groupId);
		Document feedbackQueryDom = PortalLocalServiceUtil.executeQueryAsDom( query, new String[]{"questionlabel", "thanksmsg", "actcookiesmsg", "existsvotemsg"});
		List<Node> rows = feedbackQueryDom.selectNodes("/rs/row");
		for(Node row : rows)
			rs.add( row.detach() );
		
		query = String.format( exportMode ? PUBLISH_FEEDBACK_A_EXPORTABLE : PUBLISH_FEEDBACK_A_PUBLISHABLE, groupId);
		Document feedbackAnswersDom = PortalLocalServiceUtil.executeQueryAsDom( query, new String[]{"label"});
		rows = feedbackAnswersDom.selectNodes("/rs/row");
		for(Node row : rows)
			rs.add( row.detach() );
		
		return dom;
	}

	
	private void executeJSONRemoteCalls(Document dom) throws ClientProtocolException, IOException, SystemException, PortalException, DocumentException, SecurityException, NoSuchMethodException, ServiceError
	{
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();			
		remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_CLASS_NAME,  	"com.protecmedia.iter.base.service.FeedbackServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_METHOD_NAME, 	"importFeedbackConf"));
		remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_PARAMETERS,  	"[xmlData]"));
		remoteMethodParams.add(new BasicNameValuePair("xmlData", 						dom.asXML()));
		
		JSONUtil.executeMethod("/xmlio-portlet/secure/json", remoteMethodParams);
	}
	
	public void importFeedbackConf(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, PortalException, SystemException, NoSuchMethodException, SecurityException
	{
		Document dom = SAXReaderUtil.read(xmlData);
		
        long scopeGroupId = XMLHelper.getLongValueOf(dom, "/rs/@groupId");
        if (scopeGroupId <= 0)
        {
        	String groupName = XMLHelper.getStringValueOf(dom.getRootElement(), "@groupname");
        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        	scopeGroupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        }
        ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		// Eliminar todos los registros con deletepending=1.
		String[] choiceids = XMLHelper.getStringValues( dom.selectNodes("/rs/row[@deletepending='true']"), "@choiceid");
		if (choiceids.length > 0)
			PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_FEEDBACK_CHOICE, StringUtil.merge(choiceids, "','")));
		
		// Crear o actualizar la pregunta.
		Node questionNode = dom.selectSingleNode("/rs/row[@daysdeletepolicy]");
		if (questionNode!=null)
			insertOrUpdateQuestion(questionNode, scopeGroupId);
		
		//Crear o actualizar las respuestas.
		List<Node> choicesNodes = dom.selectNodes("/rs/row[@deletepending='false']");
		if (choicesNodes.size() > 0)
		{
			String modifiedDate = SQLQueries.getCurrentDate();
			StringBuilder valuesToInsert = new StringBuilder("");
			for(Node choice : choicesNodes)
			{
				if(valuesToInsert.length() > 0 )
					valuesToInsert.append(",\n");
				
				String value = String.format("('%s', '%s', '%s', %d, 0, '%s', NULL)", 
						// ID de la respuesta
						XMLHelper.getStringValueOf(choice, 	"@choiceid"),
						
						// ID de la pregunta
						XMLHelper.getStringValueOf(choice, 	"@questionid"),
						
						// Label o valor de la respuesta
						feedbackText(choice.selectSingleNode("label"), IterErrorKeys.XYZ_ITR_E_EMPTY_FEEDBACK_CHOICE_ZYX),
						
						// Orden de la respuesta
						XMLHelper.getLongValueOf(choice, 	"@choiceorder"), 
						
						// Fecha de modificación, se comparará con la de publicación para determinar qué elementos se publican
						modifiedDate);

				valuesToInsert.append(value);
			}
			
			String query = String.format(IMPORT_CHOICES, valuesToInsert.toString());
			_log.debug("IMPORT_CHOICES: " + query);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
	}
	
	private void updatePublicationDateContents(Document dom) throws IOException, SQLException, ServiceError
	{
		//Eliminar todos los registros con deletepending=1.
		String[] choiceids = XMLHelper.getStringValues( dom.selectNodes("/rs/row[@deletepending='true']"), "@choiceid");
		if(choiceids.length>0)
			PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_FEEDBACK_CHOICE, StringUtil.merge(choiceids, "','")));
		
		//Actualizar la fecha de publicación de los registros.
		String pubDate = SQLQueries.getCurrentDate();
		
		String[] questionsIds = XMLHelper.getStringValues( dom.selectNodes("/rs/row[@daysdeletepolicy]"), "@questionid" );
		if(questionsIds.length>0)
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_QUESTION_PUBDATE, pubDate, StringUtil.merge(questionsIds, "','")) );
		
		choiceids = XMLHelper.getStringValues( dom.selectNodes("/rs/row[@deletepending='false']"), "@choiceid");
		if(choiceids.length>0)
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_CHOICES_PUBDATE, pubDate, StringUtil.merge(choiceids, "','")));
	}
	
	public Document exportData(Long groupId) throws DocumentException, Exception
	{
		return generatePublishDom(GroupLocalServiceUtil.getGroup(groupId), true);
	}
	
	public void importData(String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		Element root = dom.getRootElement();
		
		List<Node> questionIds = root.selectNodes("row[string-length(@questionid) > 0]/@questionid");
		if (!questionIds.isEmpty())
		{
	        // Busca primero el groupIid en el xml, y si no existe, se busca por groupName
	        long groupId = XMLHelper.getLongValueOf(dom, "/rs/@groupId");
	
			// Se obtiene el ID de la primera pregunta configurada en el grupo
			Document domCurrentConfig = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_CURRENTCHOICES_BY_GROUP, groupId), new String[]{"questionLabel", "choiceLabel"});
			String currentQuestionId = XMLHelper.getTextValueOf(domCurrentConfig, "/rs/row[1]/@questionId");
			
			// Existe una pregunta configurada actualmente
			if (Validator.isNotNull(currentQuestionId))
			{
				boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist", "true"));
				
				// Ya existe una pregunta para el grupo configurada actualmente, si no permite actualizar. Se lanza un fallo si no se permite actualizar
				ErrorRaiser.throwIfFalse(updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
	 					String.format("%s(%s)", IterAdmin.IA_CLASS_FEEDBACK_PORTLET, XMLHelper.getTextValueOf(domCurrentConfig, "/rs/row[1]/questionLabel")));
			}
			else
			{
				currentQuestionId = SQLQueries.getUUID();
			}
			
			// Se sustituyen las referencias al questionId importado por uno recién generado, sino existía una configurado, o por el configurado
			for (Node attrQuestionId : questionIds)
				attrQuestionId.setText(currentQuestionId);
			
			// Se sustituyen las respuestas por IDs nuevos, o por las actuales si coincide el label
			List<Node> choiceList = root.selectNodes("row[@deletepending]");
					
			for (Node choice : choiceList)
			{
				String label = XMLHelper.getTextValueOf(choice, "label");
				
				Node currentChoice = domCurrentConfig.selectSingleNode( String.format("/rs/row[choiceLabel='%s']", label) );
				if (currentChoice != null)
				{
					// La pregunta ya está configurada 
					((Element)choice).addAttribute("choiceid", ((Element)currentChoice).attributeValue("choiceid"));
					
					// Se marca la respuesta actual como que coincide con una de las respuestas a importar
					((Element)currentChoice).addAttribute("match", "1");
				}
				else
				{
					// Es una pregunta que NO está ya configurada
					((Element)choice).addAttribute("choiceid", SQLQueries.getUUID());
				}
			}
			
			// Se marcan como pendientes de borrar las respuestas actualmente configuradas, cuyo literal no coincida con ninguna de las respuestas a importar
			List<Node> unmatchNodes = domCurrentConfig.selectNodes("/rs/row[string-length(@choiceid) > 0 and not(@match)]");
			if (!unmatchNodes.isEmpty())
			{
				Element deleteChoicesRoot = SAXReaderUtil.read("<rs/>").getRootElement();
				for (Node unmatchNode: unmatchNodes)
					deleteChoicesRoot.add(unmatchNode.detach());

				deleteChoices(deleteChoicesRoot.getDocument().asXML());
			}
			
			importFeedbackConf(dom.asXML());
		}
 	}

}
