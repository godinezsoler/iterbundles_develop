package com.protecmedia.iter.news.service.item;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetVocabularyXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.LayoutXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;


/**
 * Esta clase tiene como objetivo gestionar la publicacion de páginas con los criterios de
 * Milenium
 * 
 * Al usar este item y no el LayoutXmlIO, se separan ambos conceptos y se evita crear dependencias a 
 * News en XMLIO
 * @author eduardo
 *
 */
public class MileniumArticleXmlIO extends JournalArticleXmlIO {

	private static Log _log = LogFactoryUtil.getLog(LayoutXmlIO.class);

	public MileniumArticleXmlIO () {		
		super();
	}
	
	public MileniumArticleXmlIO (XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}	
	
	/*
	 * Publish Functions
	 */	
	
	@Override
	protected void publishMileniumContent(XMLIOExport xmlioExport, Element root, Live live){
		
		try{
			if (live.getClassNameValue().equals(IterKeys.CLASSNAME_JOURNALARTICLE)){
				
				publishContent(xmlioExport, root, live, IterKeys.XMLIO_XML_PUBLISH_OPERATION);
			
			}else if (live.getClassNameValue().equals(IterKeys.CLASSNAME_VOCABULARY)){
				
				ItemXmlIO assetVocabularyXmlIO = new AssetVocabularyXmlIO();
				
				assetVocabularyXmlIO.publishContent(xmlioExport, root, live.getId(), IterKeys.XMLIO_XML_PUBLISH_OPERATION);
			
			}
		}
		catch(Exception e){
			_log.error(e);
		}
	}
	
	@Override
	public String[] getRelatedPoolIds(String[] liveItemIds) throws PortalException, SystemException{
		
		return getRelatedPoolIdsList(liveItemIds).toArray(new String[0]);				
				
	}	
	
	@Override
	public List<String> getRelatedPoolIdsList(String[] liveItemIds) throws PortalException, SystemException{
		
		List<String> liveItemIdList = new ArrayList<String>();
				
		long classNameId = ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_JOURNALARTICLE);
				
		for (String liveItemId : liveItemIds){
			
			//Check if it's pending or error
			Live live = LiveLocalServiceUtil.getLive(GetterUtil.getLong(liveItemId));
			if (live.getStatus().equals(IterKeys.PENDING) || live.getStatus().equals(IterKeys.ERROR)){
				//Add JournalArticleLiveId to results
				liveItemIdList.add(liveItemId);
				
				//Get JournalArticleId
				JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(GetterUtil.getLong(live.getGroupId()), live.getLocalId());
				
				//Add associated vocabularies
				List<String> vocList = PageContentLocalServiceUtil.getVocabularyLiveIdsFromJournalArticle(classNameId, journalArticle.getResourcePrimKey());
				for (int i = 0; i < vocList.size(); i++)
				{
					if ( !liveItemIdList.contains( vocList.get(i) ) )
					{
						// Se eliminan las repeticiones
						liveItemIdList.add(vocList.get(i));
					}
				}
			}	
		}	

		return liveItemIdList;
		
	}		
	
}
