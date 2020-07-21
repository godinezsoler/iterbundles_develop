package com.protecmedia.iter.news.service.item;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.InstrumentalContentUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetVocabularyXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.LayoutXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;


/**
 * Esta clase tiene como objetivo gestionar la publicacion de páginas con los criterios de
 * Milenium
 * 
 * Al usar este item y no el LayoutXmlIO, se separan ambos conceptos y se evita crear dependencias a 
 * News en XMLIO
 * @author eduardo
 *
 */
public class MileniumSectionXmlIO extends LayoutXmlIO {

	private static Log _log = LogFactoryUtil.getLog(LayoutXmlIO.class);

	public MileniumSectionXmlIO () {		
		super();
	}
	
	public MileniumSectionXmlIO (XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}	
	
	/*
	 * Publish Functions
	 */	
	
	@Override
	protected void publishMileniumContent(XMLIOExport xmlioExport, Element root, Live live){
		
		try{
			if (live.getClassNameValue().equals(IterKeys.CLASSNAME_LAYOUT)){
				
				publishContent(xmlioExport, root, live, IterKeys.XMLIO_XML_PUBLISH_OPERATION);
			
			}else if (live.getClassNameValue().equals(IterKeys.CLASSNAME_JOURNALARTICLE)){
				
				ItemXmlIO journalArticleXmlIO = new JournalArticleXmlIO();
				
				journalArticleXmlIO.publishContent(xmlioExport, root, live.getId(), IterKeys.XMLIO_XML_PUBLISH_OPERATION);
			
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
		_log.trace("In MileniumSectionXmlIO.getRelatedPoolIdsList");
		
		// Obtenemos el contexto
		XMLIOContext context = this.getXMLIOContext(null);
		
		//TODO: Ver si se puede optimizar más usando DynamicQuery
		List<String> liveItemIdList = new ArrayList<String>();
		
		for (String liveItemId : liveItemIds){
			Live live = LiveLocalServiceUtil.getLive(GetterUtil.getLong(liveItemId));
			Layout currentLayout = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));
			List<Layout> layoutList = new ArrayList<Layout>(); 
			layoutList.add(currentLayout);
			layoutList.addAll(currentLayout.getAllChildren());
			
			for (Layout layout : layoutList){
				
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder("Working with layout: '").append(layout.getFriendlyURL()).append("' (").append(layout.getPlid()).append(")"));
				
				try{
					Live liveLayout = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layout.getPlid()));
					
					List<String> articleLiveIdList = new ArrayList<String>();
					
					// Artículo instrumental					
					String instrumentalArticleId = InstrumentalContentUtil.getInstrumentalArticleId(layout.getGroupId(), layout.getPlid());
					if(_log.isDebugEnabled())
						_log.debug(new StringBuilder("Instrumental article id: '").append( (null == instrumentalArticleId ? "null" : instrumentalArticleId) ).append("'"));
					
					if (null != instrumentalArticleId){
						Live liveJournalArticle = LiveLocalServiceUtil.getLiveByLocalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, instrumentalArticleId);
						if (!liveItemIdList.contains(String.valueOf(liveJournalArticle.getId())) 
										&& (liveJournalArticle.getStatus().equals(IterKeys.PENDING) 
										|| liveJournalArticle.getStatus().equals(IterKeys.ERROR))){
									articleLiveIdList.add(String.valueOf(liveJournalArticle.getId()));							
								}
					}
					
					/* Para publicar la sectionproperties del layout se ha de forzar la publicación del layout en sí.
					   Comprobamos si la sectionproperties está por publicar (forceLayout = true) */					
					boolean forceLayoutBySectionProperties = forceLayoutPublication(layout);
										
					/* Layout
					 	Cuando haya un catálogo que publicar (menu, cabecera o pie) el layout estará pendiente de publicar, 
					 	así que no hay que condicionar nada a ese respecto */
					if (forceLayoutBySectionProperties || 
						liveLayout.getStatus().equals(IterKeys.PENDING) || liveLayout.getStatus().equals(IterKeys.ERROR)){
						liveItemIdList.add(String.valueOf(liveLayout.getId()));
					}
					
					// PageContents
					if (null != context && context.getPublishPageContent() ){
						_log.debug("Getting pagecontents ids");
						
						List<PageContent> layoutPCs = PageContentLocalServiceUtil.findPageLayoutFinder(layout.getUuid(), layout.getGroupId());
						for (PageContent layoutPC : layoutPCs)
						{
							Live liveJournalArticle = LiveLocalServiceUtil.getLiveByLocalId(layoutPC.getContentGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, layoutPC.getContentId());
							//Obtengo todos los JournalArticle (solo añado los pending y error que no se hayan añadido previamente)
							if(liveJournalArticle!=null)
							{
								if (!liveItemIdList.contains(String.valueOf(liveJournalArticle.getId())) 
										&& (liveJournalArticle.getStatus().equals(IterKeys.PENDING) 
										|| liveJournalArticle.getStatus().equals(IterKeys.ERROR)))
								{
									articleLiveIdList.add(String.valueOf(liveJournalArticle.getId()));							
								}
							}
							else
							{
								_log.error( String.format(IterErrorKeys.XYZ_E_ITER_JIRA_NAI_ZYX, 240, 161690) + String.format("\tArticle %s has no entry in xmlio_live table", layoutPC.getContentId()) );
							}
						}
					}
					
					// Artículos
					if (null != context && (context.getPublishArticles() || Validator.isNotNull(instrumentalArticleId) )){
						if(articleLiveIdList.size() > 0){
							ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(IterKeys.CLASSNAME_MILENIUMARTICLE);
							liveItemIdList.addAll(itemXmlIO.getRelatedPoolIdsList(articleLiveIdList.toArray(new String[0])));
						}
					}
				
				}
				catch(Exception e){
					_log.error(e);
				}
			}
		}	

		return liveItemIdList;		
	}
	
	// Comprueba si la sectionproperties está para publicar
	private boolean forceLayoutPublication(Layout layout) throws SecurityException, NoSuchMethodException, ParseException{
		_log.trace("In forceLayoutPublication");
		
		boolean forced = false;
		// Busca la section properties con fecha de publicacion nula o fecha de modificacion > fecha publicacion
		Document doc = LayoutLocalServiceUtil.getSectionProperties(layout.getGroupId(), layout.getPlid(), true);
		Node sectionProperties = doc.getRootElement().selectSingleNode("/rs/row");
		
		if (Validator.isNotNull(sectionProperties)){
			
			String data = XMLHelper.getTextValueOf(sectionProperties, "@plid");
			// Si encuentra algún dato es que la section properties está por publicar
			if (Validator.isNotNull(data)){
				forced = true;
			}
		}
		
		return forced;
	}	
}
