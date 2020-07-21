/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.news.model.PageContent;

public class PageContentValidator {
	
	public static boolean validatePageContent(PageContent pageContent, List<String> errors) throws SystemException {
		boolean valid = true;
		
		if (pageContent.getContentId().equals("")) 
		{
			errors.add("page-content-journal-id-required");			
			valid = false;
		} 
		else if (pageContent.getTypeContent() == "")
		{
			// Si no hay contenido ni se comprueba el tipo de contenido
			errors.add("page-content-type-content-required");
			valid = false;
		}

		if (pageContent.getLayoutId() == "") {			
			errors.add("page-content-template-id-required");
			valid = false;
		}
		if (pageContent.getVigenciadesde().getTime() >= pageContent.getVigenciahasta().getTime()) {			
			errors.add("page-content-error-date");
			valid = false;
		}		
		if (pageContent.getQualificationId() == "")
		{
			errors.add("page-content-qualification-id-required");
			valid = false;
		}
		if (pageContent.getArticleModelId() < 0 )
		{
			errors.add("page-content-article-model-id-required");
			valid = false;
		}
		return valid;
	}
}
