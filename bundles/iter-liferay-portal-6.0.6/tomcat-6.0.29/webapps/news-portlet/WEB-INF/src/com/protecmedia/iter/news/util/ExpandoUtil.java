package com.protecmedia.iter.news.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;

public class ExpandoUtil 
{
	private static Log _log = LogFactoryUtil.getLog(ExpandoUtil.class);
	
	public static void checkExpandoRequirements(long groupId, String className, String columnName)
	{
		try
		{
			ExpandoTable expandoTable = null;
			long companyId = GroupLocalServiceUtil.getGroup(groupId).getCompanyId();
			try 
			{
				expandoTable = ExpandoTableLocalServiceUtil.getDefaultTable(companyId, className);
			}
			catch(Exception e) 
			{
				try
				{
					expandoTable = ExpandoTableLocalServiceUtil.addDefaultTable(companyId, className);
				}
				catch(Exception e2)
				{
					_log.error(e2.toString());
					_log.trace(e2);
				}
			}
			finally 
			{
				if (expandoTable == null) 
				{
					_log.error("Could not get/create expando table for " +  className);
				}
				else
				{
					ExpandoColumn expandoColumn = null;
					try 
					{
						expandoColumn = ExpandoColumnLocalServiceUtil.getColumn(expandoTable.getTableId(), columnName);
					}
					catch (Exception e) 
					{
						if (expandoColumn == null) 
						{
							try 
							{
								expandoColumn = ExpandoColumnLocalServiceUtil.addColumn(expandoTable.getTableId(), 
																						columnName, 
																						ExpandoColumnConstants.STRING);
							}
							catch(Exception e2)
							{
								_log.error(e2.toString());
								_log.trace(e2);
							}
						}
					}
					finally 
					{
						if (expandoColumn == null) 
						{
							_log.error("Could not get/create expando column for " +  className);
						}
					}
				}
			}
		} 
		catch (Exception e) 
		{
			_log.error(e.toString());
			_log.trace(e);
		}
	}
}
