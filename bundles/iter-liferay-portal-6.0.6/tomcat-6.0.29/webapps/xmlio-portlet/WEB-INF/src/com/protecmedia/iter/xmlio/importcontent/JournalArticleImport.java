package com.protecmedia.iter.xmlio.importcontent;

import java.io.File;
import java.util.Calendar;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.JournalArticleImportServiceUtil;
import com.protecmedia.iter.xmlio.service.util.ImportTracking;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

public class JournalArticleImport extends Thread
{
	final private static Log _log = LogFactoryUtil.getLog(JournalArticleImport.class);
	
	final private Node		article 		;
	final private String	scopeGroupId 	;
	final private long		globalGroupId 	;
	final private long 		defaultUserId 	;
	final private long 		jaClassNameId 	;
	final private String 	expandoTableId 	;
	final private String 	expColGrp 		;
	final private String 	expColMeta 		;
	final private File 		tempDirectory 	;
	final private int 		maxImgWidth 	;
	final private int 		maxImgHeight 	;
	final private ImportTracking	tracking;
	
	public JournalArticleImport(ImportTracking tracking, Node article, String scopeGroupId, long globalGroupId, long defaultUserId, long jaClassNameId,
			String expandoTableId, String expColGrp, String expColMeta, File tempDirectory, int maxImgWidth, int maxImgHeight)
	{
		super();
		
		this.article = article;
		this.scopeGroupId = scopeGroupId;
		this.globalGroupId = globalGroupId;
		this.defaultUserId = defaultUserId;
		this.jaClassNameId = jaClassNameId;
		this.expandoTableId = expandoTableId;
		this.expColGrp = expColGrp;
		this.expColMeta = expColMeta;
		this.tempDirectory = tempDirectory;
		this.maxImgWidth = maxImgWidth;
		this.maxImgHeight = maxImgHeight;
		this.tracking = tracking;
	}

	@Override
	public void run()
	{
		long tIni = Calendar.getInstance().getTimeInMillis();
		try
		{
//			JournalArticleImportServiceUtil.importArticle(tracking, article, scopeGroupId, globalGroupId,defaultUserId,
//					jaClassNameId, expandoTableId, expColGrp, expColMeta, tempDirectory, maxImgWidth, maxImgHeight);
			
			_log.debug("\t" + XmlioKeys.PREFIX_ARTICLE_LOG + " Import article elapsed time: " + XMLHelper.getTextValueOf(article, "@articleid") + ", " + (Calendar.getInstance().getTimeInMillis() - tIni) + " ms\n");
		}
		catch (Exception e)
		{
			_log.debug("\t" + XmlioKeys.PREFIX_ARTICLE_LOG + " Error importing the article: " + XMLHelper.getTextValueOf(article, "@articleid") + ", " + (Calendar.getInstance().getTimeInMillis() - tIni) + " ms\n");
            _log.error(XmlioKeys.PREFIX_ARTICLE_LOG + " article " + XMLHelper.getTextValueOf(article, "@articleid") + " not imported");
		}
	}
}
