package com.protecmedia.iter.xmlio.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.protecmedia.iter.xmlio.service.base.CatalogPublicationServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class CatalogPublicationServiceImpl extends CatalogPublicationServiceBaseImpl
{
	
}