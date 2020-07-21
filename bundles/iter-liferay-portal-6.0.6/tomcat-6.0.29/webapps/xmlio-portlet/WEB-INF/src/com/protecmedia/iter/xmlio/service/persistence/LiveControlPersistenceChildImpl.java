package com.protecmedia.iter.xmlio.service.persistence;

import java.util.List;

import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.QueryPos;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.protecmedia.iter.xmlio.model.LiveControl;

public class LiveControlPersistenceChildImpl extends LiveControlPersistenceImpl
{
	private static final String _SQL_SELECT_LIVECONTROL_WHERE 		= "SELECT liveControl FROM LiveControl liveControl WHERE ";
	private static final String _FINDER_COLUMN_TYPESTATUS_TYPE_1 	= "liveControl.type IS NULL AND ";
	private static final String _FINDER_COLUMN_TYPESTATUS_TYPE_3 	= "(liveControl.type IS NULL OR liveControl.type = ?) AND ";
	private static final String _FINDER_COLUMN_TYPESTATUS_TYPE_2 	= "liveControl.type like ? AND ";
	private static final String _FINDER_COLUMN_TYPESTATUS_STATUS_1 	= "liveControl.status IS NULL";
	private static final String _FINDER_COLUMN_TYPESTATUS_STATUS_3 	= "(liveControl.status IS NULL OR liveControl.status = ?)";
	private static final String _FINDER_COLUMN_TYPESTATUS_STATUS_2 	= "liveControl.status = ?";
	private static final String _ORDER_BY_ENTITY_ALIAS 				= "liveControl.";
	
	@Override
	public List<LiveControl> findByTypeStatus(String type, String status, int start, int end, OrderByComparator orderByComparator) throws SystemException 
	{
		Object[] finderArgs = new Object[] 
										{
											type, status,
											String.valueOf(start), String.valueOf(end),
											String.valueOf(orderByComparator)
										};

		List<LiveControl> list = (List<LiveControl>)FinderCacheUtil.getResult(FINDER_PATH_FIND_BY_TYPESTATUS, finderArgs, this);

		if (list == null) 
		{
			StringBundler query = null;

			if (orderByComparator != null) 
			{
				query = new StringBundler(4 + (orderByComparator.getOrderByFields().length * 3));
			}
			else 
			{
				query = new StringBundler(3);
			}

			query.append(_SQL_SELECT_LIVECONTROL_WHERE);

			if (type == null) 
			{
				query.append(_FINDER_COLUMN_TYPESTATUS_TYPE_1);
			}
			else 
			{
				if (type.equals(StringPool.BLANK)) 
				{
					query.append(_FINDER_COLUMN_TYPESTATUS_TYPE_3);
				}
				else 
				{
					query.append(_FINDER_COLUMN_TYPESTATUS_TYPE_2);
				}
			}

			if (status == null) 
			{
				query.append(_FINDER_COLUMN_TYPESTATUS_STATUS_1);
			}
			else 
			{
				if (status.equals(StringPool.BLANK)) 
				{
					query.append(_FINDER_COLUMN_TYPESTATUS_STATUS_3);
				}
				else 
				{
					query.append(_FINDER_COLUMN_TYPESTATUS_STATUS_2);
				}
			}

			if (orderByComparator != null) 
			{
				appendOrderByComparator(query, _ORDER_BY_ENTITY_ALIAS, orderByComparator);
			}

			String sql = query.toString();

			Session session = null;

			try 
			{
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				if (type != null) 
				{
					qPos.add(type);
				}

				if (status != null) 
				{
					qPos.add(status);
				}

				list = (List<LiveControl>)QueryUtil.list(q, getDialect(), start, end);
			}
			catch (Exception e) 
			{
				throw processException(e);
			}
			finally 
			{
				if (list == null) 
				{
					FinderCacheUtil.removeResult(FINDER_PATH_FIND_BY_TYPESTATUS, finderArgs);
				}
				else 
				{
					cacheResult(list);
					FinderCacheUtil.putResult(FINDER_PATH_FIND_BY_TYPESTATUS, finderArgs, list);
				}

				closeSession(session);
			}
		}

		return list;
	}
}
