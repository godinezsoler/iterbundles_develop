/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.util.ArrayList;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.dao.search.RowChecker;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class MyContentChecker extends RowChecker 
{
	private ArrayList<String> _selectedContentIDs = null;

	@SuppressWarnings("unchecked")
	public MyContentChecker(RenderRequest renderRequest, RenderResponse renderResponse, String layoutId, long groupId) {
		super(renderResponse);
		_layoutId = layoutId;
		_groupId = groupId;
		
		_selectedContentIDs = (ArrayList<String>) (renderRequest.getAttribute("selectedContentIDs"));
	}

	public String getRowCheckBox(boolean checked, String primaryKey) {
				
		StringBuilder sb = new StringBuilder();

		if (checked) 
		{			
			sb.append("<span class=\"checked\">Ok</span>");
		} 
		else 
		{
			sb.append("<input ");
			sb.append("name=\"");
			sb.append(super.getRowId());
			sb.append("\" type=\"checkbox\" value=\"");
			sb.append(primaryKey);
			sb.append("\" ");
			
			// El check estará habilitado si se ha seleccionado previamente
			if (_selectedContentIDs != null && _selectedContentIDs.contains(primaryKey))
				sb.append("checked=\"true\" ");

			if (Validator.isNotNull(super.getAllRowsId())) 
			{
				sb.append("onClick=\"Liferay.Util.checkAllBox(");
				sb.append("AUI().one(this).ancestor('");
				sb.append("table.taglib-search-iterator'), '");
				sb.append(super.getRowId());
				sb.append("', ");
				sb.append(super.getAllRowsId());
				sb.append(");\"");
			}
	
			sb.append(">");
		}
		
		return sb.toString();
	}

	public boolean isChecked(Object obj) 
	{
		JournalArticle article = null;
		
		if (obj instanceof JournalArticle) 
		{
			article = (JournalArticle) obj;
		} 
		else if (obj instanceof Object[]) 
		{
			article = (JournalArticle)((Object[])obj)[0];
		} 
		else 
		{
			throw new IllegalArgumentException(obj + " is not a JournalArticle");
		}
		
		boolean checked = false;
		try 
		{
			// Se selecciona si existe una asignación de este artículo al pageContent
			checked = PageContentLocalServiceUtil.findPageContentExist(article.getArticleId(), _layoutId, _groupId, article.getStructureId());
		} 
		catch (Exception e) {}				

		return checked;
	}
	
	private String _layoutId;
	
	private long _groupId;

}
