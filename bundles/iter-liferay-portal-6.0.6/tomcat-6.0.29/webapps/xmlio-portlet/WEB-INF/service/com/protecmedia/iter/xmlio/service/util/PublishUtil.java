package com.protecmedia.iter.xmlio.service.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.model.Group;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class PublishUtil
{
	private static Log _log = LogFactoryUtil.getLog(PublishUtil.class);

	public static void hotConfigDeleteFile(File localFile)
	{
		if (localFile != null && localFile.exists() &&
			HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_ADVERTISEMENT_PUBLICH_DELETE_FILES, true))
		{
			try
			{
				System.gc();
				FileUtils.forceDelete(localFile);
			}
			catch (Throwable th)
			{
				_log.error(th.toString());
				_log.debug(th);
			}
		}
	}
	
	public static File getUnlockedFile(ZipWriter zipWriter)
	{
		File zip = zipWriter.getFile();
		
		// Hasta este punto el fichero zip aún no se ha actualizado del todo pq tarda el refresco del de.schlichtherle.io.File.umount (ZipWritterImpl)
		//	- umount(boolean waitInputStreams, boolean closeInputStreams, boolean waitOutputStreams, boolean closeOutputStreams)
        // 		Updates all archive files in the real file system with the contents of their virtual file system, resets all cached state and deletes all temporary files.
		//	- umount(File archive)
        //		Equivalent to umount(archive, false, true, false, true).	
		// Parece que la siguiente es una forma de forzar dicho refresco
		return new File(zip.getAbsolutePath());
	}
	
	public static String sendFile(LiveConfiguration liveConf, File localFile) throws SystemException
	{
		try
		{
			if (liveConf.getOutputMethod().equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM))
			{
				String remotePath = liveConf.getRemotePath() + File.separatorChar + localFile.getName();
				File remoteFile = new File(remotePath);
				XMLIOUtil.copyFile(localFile, remoteFile);
			}
			else
			{
				String FTPPath = liveConf.getFtpPath();
				String FTPUser = liveConf.getFtpUser();
				String FTPPass = liveConf.getFtpPassword();
				String localPath = localFile.getAbsolutePath();
				FTPUtil.sendFile(FTPPath, FTPUser, FTPPass, localFile.getName(), localPath, StringPool.BLANK);
			}
		}
		catch(Exception e)
		{
			_log.trace(e.toString());
			throw new SystemException(e);
		}
		return localFile.getName();
	}
	
	public static void checkEmptyPublication(Element rs) throws ServiceError, SystemException
	{
		List<Node> nodes = SAXReaderUtil.createXPath("//row").selectNodes(rs);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX);
	}
}
