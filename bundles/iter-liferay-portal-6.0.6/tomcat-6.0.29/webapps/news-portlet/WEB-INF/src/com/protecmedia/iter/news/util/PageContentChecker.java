/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.dao.search.RowChecker;
import com.liferay.portal.kernel.util.Validator;

public class PageContentChecker extends RowChecker {


	public PageContentChecker(RenderResponse renderResponse) {
		super(renderResponse);
	}

	public String getRowCheckBox(boolean checked, String primaryKey) {
				
		StringBuilder sb = new StringBuilder();

		if (checked) {			
			;
		} else {
			sb.append("<input ");
			sb.append("name=\"");
			sb.append(super.getRowId());
			sb.append("\" type=\"checkbox\" value=\"");
			sb.append(primaryKey);
			sb.append("\" ");

			if (Validator.isNotNull(super.getAllRowsId())) {
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

}
