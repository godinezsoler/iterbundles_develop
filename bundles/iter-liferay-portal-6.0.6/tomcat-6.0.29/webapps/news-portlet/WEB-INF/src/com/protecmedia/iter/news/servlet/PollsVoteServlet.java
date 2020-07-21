/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.polls.service.PollsVoteLocalServiceUtil;
import com.protecmedia.iter.news.util.MyPollsVoteUtil;


/**
 * This is a File Upload Servlet that is used with AJAX
 * to monitor the progress of the uploaded file. It will
 * return an XML object containing the meta information
 * as well as the percent complete.
 */
public class PollsVoteServlet extends HttpServlet implements Servlet {
   
	private static final long serialVersionUID = 2740693677625051632L;

	public PollsVoteServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		long questionId = ParamUtil.get(request, "questionId", -1);
		long choiceId = ParamUtil.get(request, "choiceId", -1);
		long userId = ParamUtil.get(request, "userId", -1);
		
		ServiceContext serviceContext = new ServiceContext();
		
		try
		{
			if (userId == 0 || userId == -1)
			{
				userId = CounterLocalServiceUtil.increment();
			}
			
			PollsVoteLocalServiceUtil.addVote(userId, questionId, choiceId, serviceContext);
			
			MyPollsVoteUtil.saveVote(request, questionId);
			
			SessionMessages.add(request, "poll-vote-add-vote");
		}
		catch (Exception e)
		{
			SessionErrors.add(request, "poll-vote-none-checked");
		}
		
		// Redirect
		String redirect = ParamUtil.get(request, "redirect", "");

		if (!redirect.equals(""))
		{
			response.sendRedirect(redirect);
		}
	}
}

