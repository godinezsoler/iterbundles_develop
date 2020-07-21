/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portlet.polls.NoSuchVoteException;
import com.liferay.portlet.polls.model.PollsQuestion;
import com.liferay.portlet.polls.service.PollsVoteLocalServiceUtil;

public class MyPollsVoteUtil {
	
	public static boolean hasVoted(long userId, long questionId) throws PortalException, SystemException {
		try {
			PollsVoteLocalServiceUtil.getVote(
				questionId, userId);
		}
		catch (NoSuchVoteException nsve) {
			return false;
		}

		return true;
	}	
	
	public static boolean hasVoted(HttpServletRequest request, long userId, boolean signedIn, long questionId) throws PortalException, SystemException {
		if (signedIn) {
			try {
				PollsVoteLocalServiceUtil.getVote(
					questionId, userId);
			}
			catch (NoSuchVoteException nsve) {
				return false;
			}
	
			return true;
		}
		else {
			HttpSession session = request.getSession();
	
			Boolean hasVoted = (Boolean)session.getAttribute(
				PollsQuestion.class.getName() + "." + questionId);
	
			if ((hasVoted != null) && (hasVoted.booleanValue())) {
				return true;
			}
			else {
				return false;
			}
		}
	}	
	
	public static void saveVote(HttpServletRequest request, long questionId) {
		HttpSession session = request.getSession();

		session.setAttribute(PollsQuestion.class.getName() + "." + questionId, Boolean.TRUE);
	}
}
