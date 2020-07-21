/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.services.service.persistence;

import com.liferay.portal.NoSuchModelException;
import com.liferay.portal.kernel.annotation.BeanReference;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.dao.orm.EntityCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderPath;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.QueryPos;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.InstanceFactory;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.service.persistence.BatchSessionUtil;
import com.liferay.portal.service.persistence.ResourcePersistence;
import com.liferay.portal.service.persistence.UserPersistence;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;

import com.protecmedia.iter.services.NoSuchServiceException;
import com.protecmedia.iter.services.model.Service;
import com.protecmedia.iter.services.model.impl.ServiceImpl;
import com.protecmedia.iter.services.model.impl.ServiceModelImpl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The persistence implementation for the service service.
 *
 * <p>
 * Never modify or reference this class directly. Always use {@link ServiceUtil} to access the service persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
 * </p>
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Protecmedia
 * @see ServicePersistence
 * @see ServiceUtil
 * @generated
 */
public class ServicePersistenceImpl extends BasePersistenceImpl<Service>
	implements ServicePersistence {
	public static final String FINDER_CLASS_NAME_ENTITY = ServiceImpl.class.getName();
	public static final String FINDER_CLASS_NAME_LIST = FINDER_CLASS_NAME_ENTITY +
		".List";
	public static final FinderPath FINDER_PATH_FIND_BY_GROUPID = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"findByGroupId",
			new String[] {
				Long.class.getName(),
				
			"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			});
	public static final FinderPath FINDER_PATH_COUNT_BY_GROUPID = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"countByGroupId", new String[] { Long.class.getName() });
	public static final FinderPath FINDER_PATH_FETCH_BY_NAME = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_ENTITY,
			"fetchByName",
			new String[] { Long.class.getName(), String.class.getName() });
	public static final FinderPath FINDER_PATH_COUNT_BY_NAME = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"countByName",
			new String[] { Long.class.getName(), String.class.getName() });
	public static final FinderPath FINDER_PATH_FETCH_BY_SERVICEID = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_ENTITY,
			"fetchByServiceId",
			new String[] { Long.class.getName(), String.class.getName() });
	public static final FinderPath FINDER_PATH_COUNT_BY_SERVICEID = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"countByServiceId",
			new String[] { Long.class.getName(), String.class.getName() });
	public static final FinderPath FINDER_PATH_FIND_BY_LINKID = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"findByLinkId",
			new String[] {
				Long.class.getName(), Long.class.getName(),
				
			"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			});
	public static final FinderPath FINDER_PATH_COUNT_BY_LINKID = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"countByLinkId",
			new String[] { Long.class.getName(), Long.class.getName() });
	public static final FinderPath FINDER_PATH_FIND_ALL = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"findAll", new String[0]);
	public static final FinderPath FINDER_PATH_COUNT_ALL = new FinderPath(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceModelImpl.FINDER_CACHE_ENABLED, FINDER_CLASS_NAME_LIST,
			"countAll", new String[0]);

	/**
	 * Caches the service in the entity cache if it is enabled.
	 *
	 * @param service the service to cache
	 */
	public void cacheResult(Service service) {
		EntityCacheUtil.putResult(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceImpl.class, service.getPrimaryKey(), service);

		FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_NAME,
			new Object[] { new Long(service.getGroupId()), service.getTitle() },
			service);

		FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_SERVICEID,
			new Object[] { new Long(service.getGroupId()), service.getServiceId() },
			service);
	}

	/**
	 * Caches the services in the entity cache if it is enabled.
	 *
	 * @param services the services to cache
	 */
	public void cacheResult(List<Service> services) {
		for (Service service : services) {
			if (EntityCacheUtil.getResult(
						ServiceModelImpl.ENTITY_CACHE_ENABLED,
						ServiceImpl.class, service.getPrimaryKey(), this) == null) {
				cacheResult(service);
			}
		}
	}

	/**
	 * Clears the cache for all services.
	 *
	 * <p>
	 * The {@link com.liferay.portal.kernel.dao.orm.EntityCache} and {@link com.liferay.portal.kernel.dao.orm.FinderCache} are both cleared by this method.
	 * </p>
	 */
	public void clearCache() {
		CacheRegistryUtil.clear(ServiceImpl.class.getName());
		EntityCacheUtil.clearCache(ServiceImpl.class.getName());
		FinderCacheUtil.clearCache(FINDER_CLASS_NAME_ENTITY);
		FinderCacheUtil.clearCache(FINDER_CLASS_NAME_LIST);
	}

	/**
	 * Clears the cache for the service.
	 *
	 * <p>
	 * The {@link com.liferay.portal.kernel.dao.orm.EntityCache} and {@link com.liferay.portal.kernel.dao.orm.FinderCache} are both cleared by this method.
	 * </p>
	 */
	public void clearCache(Service service) {
		EntityCacheUtil.removeResult(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceImpl.class, service.getPrimaryKey());

		FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_NAME,
			new Object[] { new Long(service.getGroupId()), service.getTitle() });

		FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_SERVICEID,
			new Object[] { new Long(service.getGroupId()), service.getServiceId() });
	}

	/**
	 * Creates a new service with the primary key. Does not add the service to the database.
	 *
	 * @param id the primary key for the new service
	 * @return the new service
	 */
	public Service create(long id) {
		Service service = new ServiceImpl();

		service.setNew(true);
		service.setPrimaryKey(id);

		return service;
	}

	/**
	 * Removes the service with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param primaryKey the primary key of the service to remove
	 * @return the service that was removed
	 * @throws com.liferay.portal.NoSuchModelException if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service remove(Serializable primaryKey)
		throws NoSuchModelException, SystemException {
		return remove(((Long)primaryKey).longValue());
	}

	/**
	 * Removes the service with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param id the primary key of the service to remove
	 * @return the service that was removed
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service remove(long id)
		throws NoSuchServiceException, SystemException {
		Session session = null;

		try {
			session = openSession();

			Service service = (Service)session.get(ServiceImpl.class,
					new Long(id));

			if (service == null) {
				if (_log.isWarnEnabled()) {
					_log.warn(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + id);
				}

				throw new NoSuchServiceException(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY +
					id);
			}

			return remove(service);
		}
		catch (NoSuchServiceException nsee) {
			throw nsee;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	protected Service removeImpl(Service service) throws SystemException {
		service = toUnwrappedModel(service);

		Session session = null;

		try {
			session = openSession();

			BatchSessionUtil.delete(session, service);
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}

		FinderCacheUtil.clearCache(FINDER_CLASS_NAME_LIST);

		ServiceModelImpl serviceModelImpl = (ServiceModelImpl)service;

		FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_NAME,
			new Object[] {
				new Long(serviceModelImpl.getGroupId()),
				
			serviceModelImpl.getTitle()
			});

		FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_SERVICEID,
			new Object[] {
				new Long(serviceModelImpl.getGroupId()),
				
			serviceModelImpl.getServiceId()
			});

		EntityCacheUtil.removeResult(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceImpl.class, service.getPrimaryKey());

		return service;
	}

	public Service updateImpl(
		com.protecmedia.iter.services.model.Service service, boolean merge)
		throws SystemException {
		service = toUnwrappedModel(service);

		boolean isNew = service.isNew();

		ServiceModelImpl serviceModelImpl = (ServiceModelImpl)service;

		Session session = null;

		try {
			session = openSession();

			BatchSessionUtil.update(session, service, merge);

			service.setNew(false);
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}

		FinderCacheUtil.clearCache(FINDER_CLASS_NAME_LIST);

		EntityCacheUtil.putResult(ServiceModelImpl.ENTITY_CACHE_ENABLED,
			ServiceImpl.class, service.getPrimaryKey(), service);

		if (!isNew &&
				((service.getGroupId() != serviceModelImpl.getOriginalGroupId()) ||
				!Validator.equals(service.getTitle(),
					serviceModelImpl.getOriginalTitle()))) {
			FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_NAME,
				new Object[] {
					new Long(serviceModelImpl.getOriginalGroupId()),
					
				serviceModelImpl.getOriginalTitle()
				});
		}

		if (isNew ||
				((service.getGroupId() != serviceModelImpl.getOriginalGroupId()) ||
				!Validator.equals(service.getTitle(),
					serviceModelImpl.getOriginalTitle()))) {
			FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_NAME,
				new Object[] { new Long(service.getGroupId()), service.getTitle() },
				service);
		}

		if (!isNew &&
				((service.getGroupId() != serviceModelImpl.getOriginalGroupId()) ||
				!Validator.equals(service.getServiceId(),
					serviceModelImpl.getOriginalServiceId()))) {
			FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_SERVICEID,
				new Object[] {
					new Long(serviceModelImpl.getOriginalGroupId()),
					
				serviceModelImpl.getOriginalServiceId()
				});
		}

		if (isNew ||
				((service.getGroupId() != serviceModelImpl.getOriginalGroupId()) ||
				!Validator.equals(service.getServiceId(),
					serviceModelImpl.getOriginalServiceId()))) {
			FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_SERVICEID,
				new Object[] {
					new Long(service.getGroupId()),
					
				service.getServiceId()
				}, service);
		}

		return service;
	}

	protected Service toUnwrappedModel(Service service) {
		if (service instanceof ServiceImpl) {
			return service;
		}

		ServiceImpl serviceImpl = new ServiceImpl();

		serviceImpl.setNew(service.isNew());
		serviceImpl.setPrimaryKey(service.getPrimaryKey());

		serviceImpl.setId(service.getId());
		serviceImpl.setGroupId(service.getGroupId());
		serviceImpl.setLinkId(service.getLinkId());
		serviceImpl.setServiceId(service.getServiceId());
		serviceImpl.setTitle(service.getTitle());
		serviceImpl.setImageId(service.getImageId());

		return serviceImpl;
	}

	/**
	 * Finds the service with the primary key or throws a {@link com.liferay.portal.NoSuchModelException} if it could not be found.
	 *
	 * @param primaryKey the primary key of the service to find
	 * @return the service
	 * @throws com.liferay.portal.NoSuchModelException if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByPrimaryKey(Serializable primaryKey)
		throws NoSuchModelException, SystemException {
		return findByPrimaryKey(((Long)primaryKey).longValue());
	}

	/**
	 * Finds the service with the primary key or throws a {@link com.protecmedia.iter.services.NoSuchServiceException} if it could not be found.
	 *
	 * @param id the primary key of the service to find
	 * @return the service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByPrimaryKey(long id)
		throws NoSuchServiceException, SystemException {
		Service service = fetchByPrimaryKey(id);

		if (service == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + id);
			}

			throw new NoSuchServiceException(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY +
				id);
		}

		return service;
	}

	/**
	 * Finds the service with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param primaryKey the primary key of the service to find
	 * @return the service, or <code>null</code> if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service fetchByPrimaryKey(Serializable primaryKey)
		throws SystemException {
		return fetchByPrimaryKey(((Long)primaryKey).longValue());
	}

	/**
	 * Finds the service with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param id the primary key of the service to find
	 * @return the service, or <code>null</code> if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service fetchByPrimaryKey(long id) throws SystemException {
		Service service = (Service)EntityCacheUtil.getResult(ServiceModelImpl.ENTITY_CACHE_ENABLED,
				ServiceImpl.class, id, this);

		if (service == null) {
			Session session = null;

			try {
				session = openSession();

				service = (Service)session.get(ServiceImpl.class, new Long(id));
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (service != null) {
					cacheResult(service);
				}

				closeSession(session);
			}
		}

		return service;
	}

	/**
	 * Finds all the services where groupId = &#63;.
	 *
	 * @param groupId the group id to search with
	 * @return the matching services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findByGroupId(long groupId) throws SystemException {
		return findByGroupId(groupId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	/**
	 * Finds a range of all the services where groupId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param start the lower bound of the range of services to return
	 * @param end the upper bound of the range of services to return (not inclusive)
	 * @return the range of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findByGroupId(long groupId, int start, int end)
		throws SystemException {
		return findByGroupId(groupId, start, end, null);
	}

	/**
	 * Finds an ordered range of all the services where groupId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param start the lower bound of the range of services to return
	 * @param end the upper bound of the range of services to return (not inclusive)
	 * @param orderByComparator the comparator to order the results by
	 * @return the ordered range of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findByGroupId(long groupId, int start, int end,
		OrderByComparator orderByComparator) throws SystemException {
		Object[] finderArgs = new Object[] {
				groupId,
				
				String.valueOf(start), String.valueOf(end),
				String.valueOf(orderByComparator)
			};

		List<Service> list = (List<Service>)FinderCacheUtil.getResult(FINDER_PATH_FIND_BY_GROUPID,
				finderArgs, this);

		if (list == null) {
			StringBundler query = null;

			if (orderByComparator != null) {
				query = new StringBundler(3 +
						(orderByComparator.getOrderByFields().length * 3));
			}
			else {
				query = new StringBundler(3);
			}

			query.append(_SQL_SELECT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_GROUPID_GROUPID_2);

			if (orderByComparator != null) {
				appendOrderByComparator(query, _ORDER_BY_ENTITY_ALIAS,
					orderByComparator);
			}

			else {
				query.append(ServiceModelImpl.ORDER_BY_JPQL);
			}

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				list = (List<Service>)QueryUtil.list(q, getDialect(), start, end);
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (list == null) {
					FinderCacheUtil.removeResult(FINDER_PATH_FIND_BY_GROUPID,
						finderArgs);
				}
				else {
					cacheResult(list);

					FinderCacheUtil.putResult(FINDER_PATH_FIND_BY_GROUPID,
						finderArgs, list);
				}

				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Finds the first service in the ordered set where groupId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param orderByComparator the comparator to order the set by
	 * @return the first matching service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByGroupId_First(long groupId,
		OrderByComparator orderByComparator)
		throws NoSuchServiceException, SystemException {
		List<Service> list = findByGroupId(groupId, 0, 1, orderByComparator);

		if (list.isEmpty()) {
			StringBundler msg = new StringBundler(4);

			msg.append(_NO_SUCH_ENTITY_WITH_KEY);

			msg.append("groupId=");
			msg.append(groupId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchServiceException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	/**
	 * Finds the last service in the ordered set where groupId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param orderByComparator the comparator to order the set by
	 * @return the last matching service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByGroupId_Last(long groupId,
		OrderByComparator orderByComparator)
		throws NoSuchServiceException, SystemException {
		int count = countByGroupId(groupId);

		List<Service> list = findByGroupId(groupId, count - 1, count,
				orderByComparator);

		if (list.isEmpty()) {
			StringBundler msg = new StringBundler(4);

			msg.append(_NO_SUCH_ENTITY_WITH_KEY);

			msg.append("groupId=");
			msg.append(groupId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchServiceException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	/**
	 * Finds the services before and after the current service in the ordered set where groupId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param id the primary key of the current service
	 * @param groupId the group id to search with
	 * @param orderByComparator the comparator to order the set by
	 * @return the previous, current, and next service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service[] findByGroupId_PrevAndNext(long id, long groupId,
		OrderByComparator orderByComparator)
		throws NoSuchServiceException, SystemException {
		Service service = findByPrimaryKey(id);

		Session session = null;

		try {
			session = openSession();

			Service[] array = new ServiceImpl[3];

			array[0] = getByGroupId_PrevAndNext(session, service, groupId,
					orderByComparator, true);

			array[1] = service;

			array[2] = getByGroupId_PrevAndNext(session, service, groupId,
					orderByComparator, false);

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	protected Service getByGroupId_PrevAndNext(Session session,
		Service service, long groupId, OrderByComparator orderByComparator,
		boolean previous) {
		StringBundler query = null;

		if (orderByComparator != null) {
			query = new StringBundler(6 +
					(orderByComparator.getOrderByFields().length * 6));
		}
		else {
			query = new StringBundler(3);
		}

		query.append(_SQL_SELECT_SERVICE_WHERE);

		query.append(_FINDER_COLUMN_GROUPID_GROUPID_2);

		if (orderByComparator != null) {
			String[] orderByFields = orderByComparator.getOrderByFields();

			if (orderByFields.length > 0) {
				query.append(WHERE_AND);
			}

			for (int i = 0; i < orderByFields.length; i++) {
				query.append(_ORDER_BY_ENTITY_ALIAS);
				query.append(orderByFields[i]);

				if ((i + 1) < orderByFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(WHERE_GREATER_THAN_HAS_NEXT);
					}
					else {
						query.append(WHERE_LESSER_THAN_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(WHERE_GREATER_THAN);
					}
					else {
						query.append(WHERE_LESSER_THAN);
					}
				}
			}

			query.append(ORDER_BY_CLAUSE);

			for (int i = 0; i < orderByFields.length; i++) {
				query.append(_ORDER_BY_ENTITY_ALIAS);
				query.append(orderByFields[i]);

				if ((i + 1) < orderByFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(ORDER_BY_ASC_HAS_NEXT);
					}
					else {
						query.append(ORDER_BY_DESC_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(ORDER_BY_ASC);
					}
					else {
						query.append(ORDER_BY_DESC);
					}
				}
			}
		}

		else {
			query.append(ServiceModelImpl.ORDER_BY_JPQL);
		}

		String sql = query.toString();

		Query q = session.createQuery(sql);

		q.setFirstResult(0);
		q.setMaxResults(2);

		QueryPos qPos = QueryPos.getInstance(q);

		qPos.add(groupId);

		if (orderByComparator != null) {
			Object[] values = orderByComparator.getOrderByValues(service);

			for (Object value : values) {
				qPos.add(value);
			}
		}

		List<Service> list = q.list();

		if (list.size() == 2) {
			return list.get(1);
		}
		else {
			return null;
		}
	}

	/**
	 * Finds the service where groupId = &#63; and title = &#63; or throws a {@link com.protecmedia.iter.services.NoSuchServiceException} if it could not be found.
	 *
	 * @param groupId the group id to search with
	 * @param title the title to search with
	 * @return the matching service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByName(long groupId, String title)
		throws NoSuchServiceException, SystemException {
		Service service = fetchByName(groupId, title);

		if (service == null) {
			StringBundler msg = new StringBundler(6);

			msg.append(_NO_SUCH_ENTITY_WITH_KEY);

			msg.append("groupId=");
			msg.append(groupId);

			msg.append(", title=");
			msg.append(title);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			if (_log.isWarnEnabled()) {
				_log.warn(msg.toString());
			}

			throw new NoSuchServiceException(msg.toString());
		}

		return service;
	}

	/**
	 * Finds the service where groupId = &#63; and title = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param groupId the group id to search with
	 * @param title the title to search with
	 * @return the matching service, or <code>null</code> if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service fetchByName(long groupId, String title)
		throws SystemException {
		return fetchByName(groupId, title, true);
	}

	/**
	 * Finds the service where groupId = &#63; and title = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param groupId the group id to search with
	 * @param title the title to search with
	 * @return the matching service, or <code>null</code> if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service fetchByName(long groupId, String title,
		boolean retrieveFromCache) throws SystemException {
		Object[] finderArgs = new Object[] { groupId, title };

		Object result = null;

		if (retrieveFromCache) {
			result = FinderCacheUtil.getResult(FINDER_PATH_FETCH_BY_NAME,
					finderArgs, this);
		}

		if (result == null) {
			StringBundler query = new StringBundler(4);

			query.append(_SQL_SELECT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_NAME_GROUPID_2);

			if (title == null) {
				query.append(_FINDER_COLUMN_NAME_TITLE_1);
			}
			else {
				if (title.equals(StringPool.BLANK)) {
					query.append(_FINDER_COLUMN_NAME_TITLE_3);
				}
				else {
					query.append(_FINDER_COLUMN_NAME_TITLE_2);
				}
			}

			query.append(ServiceModelImpl.ORDER_BY_JPQL);

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				if (title != null) {
					qPos.add(title);
				}

				List<Service> list = q.list();

				result = list;

				Service service = null;

				if (list.isEmpty()) {
					FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_NAME,
						finderArgs, list);
				}
				else {
					service = list.get(0);

					cacheResult(service);

					if ((service.getGroupId() != groupId) ||
							(service.getTitle() == null) ||
							!service.getTitle().equals(title)) {
						FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_NAME,
							finderArgs, service);
					}
				}

				return service;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (result == null) {
					FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_NAME,
						finderArgs);
				}

				closeSession(session);
			}
		}
		else {
			if (result instanceof List<?>) {
				return null;
			}
			else {
				return (Service)result;
			}
		}
	}

	/**
	 * Finds the service where groupId = &#63; and serviceId = &#63; or throws a {@link com.protecmedia.iter.services.NoSuchServiceException} if it could not be found.
	 *
	 * @param groupId the group id to search with
	 * @param serviceId the service id to search with
	 * @return the matching service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByServiceId(long groupId, String serviceId)
		throws NoSuchServiceException, SystemException {
		Service service = fetchByServiceId(groupId, serviceId);

		if (service == null) {
			StringBundler msg = new StringBundler(6);

			msg.append(_NO_SUCH_ENTITY_WITH_KEY);

			msg.append("groupId=");
			msg.append(groupId);

			msg.append(", serviceId=");
			msg.append(serviceId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			if (_log.isWarnEnabled()) {
				_log.warn(msg.toString());
			}

			throw new NoSuchServiceException(msg.toString());
		}

		return service;
	}

	/**
	 * Finds the service where groupId = &#63; and serviceId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param groupId the group id to search with
	 * @param serviceId the service id to search with
	 * @return the matching service, or <code>null</code> if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service fetchByServiceId(long groupId, String serviceId)
		throws SystemException {
		return fetchByServiceId(groupId, serviceId, true);
	}

	/**
	 * Finds the service where groupId = &#63; and serviceId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param groupId the group id to search with
	 * @param serviceId the service id to search with
	 * @return the matching service, or <code>null</code> if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service fetchByServiceId(long groupId, String serviceId,
		boolean retrieveFromCache) throws SystemException {
		Object[] finderArgs = new Object[] { groupId, serviceId };

		Object result = null;

		if (retrieveFromCache) {
			result = FinderCacheUtil.getResult(FINDER_PATH_FETCH_BY_SERVICEID,
					finderArgs, this);
		}

		if (result == null) {
			StringBundler query = new StringBundler(4);

			query.append(_SQL_SELECT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_SERVICEID_GROUPID_2);

			if (serviceId == null) {
				query.append(_FINDER_COLUMN_SERVICEID_SERVICEID_1);
			}
			else {
				if (serviceId.equals(StringPool.BLANK)) {
					query.append(_FINDER_COLUMN_SERVICEID_SERVICEID_3);
				}
				else {
					query.append(_FINDER_COLUMN_SERVICEID_SERVICEID_2);
				}
			}

			query.append(ServiceModelImpl.ORDER_BY_JPQL);

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				if (serviceId != null) {
					qPos.add(serviceId);
				}

				List<Service> list = q.list();

				result = list;

				Service service = null;

				if (list.isEmpty()) {
					FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_SERVICEID,
						finderArgs, list);
				}
				else {
					service = list.get(0);

					cacheResult(service);

					if ((service.getGroupId() != groupId) ||
							(service.getServiceId() == null) ||
							!service.getServiceId().equals(serviceId)) {
						FinderCacheUtil.putResult(FINDER_PATH_FETCH_BY_SERVICEID,
							finderArgs, service);
					}
				}

				return service;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (result == null) {
					FinderCacheUtil.removeResult(FINDER_PATH_FETCH_BY_SERVICEID,
						finderArgs);
				}

				closeSession(session);
			}
		}
		else {
			if (result instanceof List<?>) {
				return null;
			}
			else {
				return (Service)result;
			}
		}
	}

	/**
	 * Finds all the services where groupId = &#63; and linkId = &#63;.
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @return the matching services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findByLinkId(long groupId, long linkId)
		throws SystemException {
		return findByLinkId(groupId, linkId, QueryUtil.ALL_POS,
			QueryUtil.ALL_POS, null);
	}

	/**
	 * Finds a range of all the services where groupId = &#63; and linkId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @param start the lower bound of the range of services to return
	 * @param end the upper bound of the range of services to return (not inclusive)
	 * @return the range of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findByLinkId(long groupId, long linkId, int start,
		int end) throws SystemException {
		return findByLinkId(groupId, linkId, start, end, null);
	}

	/**
	 * Finds an ordered range of all the services where groupId = &#63; and linkId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @param start the lower bound of the range of services to return
	 * @param end the upper bound of the range of services to return (not inclusive)
	 * @param orderByComparator the comparator to order the results by
	 * @return the ordered range of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findByLinkId(long groupId, long linkId, int start,
		int end, OrderByComparator orderByComparator) throws SystemException {
		Object[] finderArgs = new Object[] {
				groupId, linkId,
				
				String.valueOf(start), String.valueOf(end),
				String.valueOf(orderByComparator)
			};

		List<Service> list = (List<Service>)FinderCacheUtil.getResult(FINDER_PATH_FIND_BY_LINKID,
				finderArgs, this);

		if (list == null) {
			StringBundler query = null;

			if (orderByComparator != null) {
				query = new StringBundler(4 +
						(orderByComparator.getOrderByFields().length * 3));
			}
			else {
				query = new StringBundler(4);
			}

			query.append(_SQL_SELECT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_LINKID_GROUPID_2);

			query.append(_FINDER_COLUMN_LINKID_LINKID_2);

			if (orderByComparator != null) {
				appendOrderByComparator(query, _ORDER_BY_ENTITY_ALIAS,
					orderByComparator);
			}

			else {
				query.append(ServiceModelImpl.ORDER_BY_JPQL);
			}

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				qPos.add(linkId);

				list = (List<Service>)QueryUtil.list(q, getDialect(), start, end);
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (list == null) {
					FinderCacheUtil.removeResult(FINDER_PATH_FIND_BY_LINKID,
						finderArgs);
				}
				else {
					cacheResult(list);

					FinderCacheUtil.putResult(FINDER_PATH_FIND_BY_LINKID,
						finderArgs, list);
				}

				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Finds the first service in the ordered set where groupId = &#63; and linkId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @param orderByComparator the comparator to order the set by
	 * @return the first matching service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByLinkId_First(long groupId, long linkId,
		OrderByComparator orderByComparator)
		throws NoSuchServiceException, SystemException {
		List<Service> list = findByLinkId(groupId, linkId, 0, 1,
				orderByComparator);

		if (list.isEmpty()) {
			StringBundler msg = new StringBundler(6);

			msg.append(_NO_SUCH_ENTITY_WITH_KEY);

			msg.append("groupId=");
			msg.append(groupId);

			msg.append(", linkId=");
			msg.append(linkId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchServiceException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	/**
	 * Finds the last service in the ordered set where groupId = &#63; and linkId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @param orderByComparator the comparator to order the set by
	 * @return the last matching service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a matching service could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service findByLinkId_Last(long groupId, long linkId,
		OrderByComparator orderByComparator)
		throws NoSuchServiceException, SystemException {
		int count = countByLinkId(groupId, linkId);

		List<Service> list = findByLinkId(groupId, linkId, count - 1, count,
				orderByComparator);

		if (list.isEmpty()) {
			StringBundler msg = new StringBundler(6);

			msg.append(_NO_SUCH_ENTITY_WITH_KEY);

			msg.append("groupId=");
			msg.append(groupId);

			msg.append(", linkId=");
			msg.append(linkId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchServiceException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	/**
	 * Finds the services before and after the current service in the ordered set where groupId = &#63; and linkId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param id the primary key of the current service
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @param orderByComparator the comparator to order the set by
	 * @return the previous, current, and next service
	 * @throws com.protecmedia.iter.services.NoSuchServiceException if a service with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	public Service[] findByLinkId_PrevAndNext(long id, long groupId,
		long linkId, OrderByComparator orderByComparator)
		throws NoSuchServiceException, SystemException {
		Service service = findByPrimaryKey(id);

		Session session = null;

		try {
			session = openSession();

			Service[] array = new ServiceImpl[3];

			array[0] = getByLinkId_PrevAndNext(session, service, groupId,
					linkId, orderByComparator, true);

			array[1] = service;

			array[2] = getByLinkId_PrevAndNext(session, service, groupId,
					linkId, orderByComparator, false);

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	protected Service getByLinkId_PrevAndNext(Session session, Service service,
		long groupId, long linkId, OrderByComparator orderByComparator,
		boolean previous) {
		StringBundler query = null;

		if (orderByComparator != null) {
			query = new StringBundler(6 +
					(orderByComparator.getOrderByFields().length * 6));
		}
		else {
			query = new StringBundler(3);
		}

		query.append(_SQL_SELECT_SERVICE_WHERE);

		query.append(_FINDER_COLUMN_LINKID_GROUPID_2);

		query.append(_FINDER_COLUMN_LINKID_LINKID_2);

		if (orderByComparator != null) {
			String[] orderByFields = orderByComparator.getOrderByFields();

			if (orderByFields.length > 0) {
				query.append(WHERE_AND);
			}

			for (int i = 0; i < orderByFields.length; i++) {
				query.append(_ORDER_BY_ENTITY_ALIAS);
				query.append(orderByFields[i]);

				if ((i + 1) < orderByFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(WHERE_GREATER_THAN_HAS_NEXT);
					}
					else {
						query.append(WHERE_LESSER_THAN_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(WHERE_GREATER_THAN);
					}
					else {
						query.append(WHERE_LESSER_THAN);
					}
				}
			}

			query.append(ORDER_BY_CLAUSE);

			for (int i = 0; i < orderByFields.length; i++) {
				query.append(_ORDER_BY_ENTITY_ALIAS);
				query.append(orderByFields[i]);

				if ((i + 1) < orderByFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(ORDER_BY_ASC_HAS_NEXT);
					}
					else {
						query.append(ORDER_BY_DESC_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						query.append(ORDER_BY_ASC);
					}
					else {
						query.append(ORDER_BY_DESC);
					}
				}
			}
		}

		else {
			query.append(ServiceModelImpl.ORDER_BY_JPQL);
		}

		String sql = query.toString();

		Query q = session.createQuery(sql);

		q.setFirstResult(0);
		q.setMaxResults(2);

		QueryPos qPos = QueryPos.getInstance(q);

		qPos.add(groupId);

		qPos.add(linkId);

		if (orderByComparator != null) {
			Object[] values = orderByComparator.getOrderByValues(service);

			for (Object value : values) {
				qPos.add(value);
			}
		}

		List<Service> list = q.list();

		if (list.size() == 2) {
			return list.get(1);
		}
		else {
			return null;
		}
	}

	/**
	 * Finds all the services.
	 *
	 * @return the services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findAll() throws SystemException {
		return findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	/**
	 * Finds a range of all the services.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param start the lower bound of the range of services to return
	 * @param end the upper bound of the range of services to return (not inclusive)
	 * @return the range of services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findAll(int start, int end) throws SystemException {
		return findAll(start, end, null);
	}

	/**
	 * Finds an ordered range of all the services.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set.
	 * </p>
	 *
	 * @param start the lower bound of the range of services to return
	 * @param end the upper bound of the range of services to return (not inclusive)
	 * @param orderByComparator the comparator to order the results by
	 * @return the ordered range of services
	 * @throws SystemException if a system exception occurred
	 */
	public List<Service> findAll(int start, int end,
		OrderByComparator orderByComparator) throws SystemException {
		Object[] finderArgs = new Object[] {
				String.valueOf(start), String.valueOf(end),
				String.valueOf(orderByComparator)
			};

		List<Service> list = (List<Service>)FinderCacheUtil.getResult(FINDER_PATH_FIND_ALL,
				finderArgs, this);

		if (list == null) {
			StringBundler query = null;
			String sql = null;

			if (orderByComparator != null) {
				query = new StringBundler(2 +
						(orderByComparator.getOrderByFields().length * 3));

				query.append(_SQL_SELECT_SERVICE);

				appendOrderByComparator(query, _ORDER_BY_ENTITY_ALIAS,
					orderByComparator);

				sql = query.toString();
			}
			else {
				sql = _SQL_SELECT_SERVICE.concat(ServiceModelImpl.ORDER_BY_JPQL);
			}

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				if (orderByComparator == null) {
					list = (List<Service>)QueryUtil.list(q, getDialect(),
							start, end, false);

					Collections.sort(list);
				}
				else {
					list = (List<Service>)QueryUtil.list(q, getDialect(),
							start, end);
				}
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (list == null) {
					FinderCacheUtil.removeResult(FINDER_PATH_FIND_ALL,
						finderArgs);
				}
				else {
					cacheResult(list);

					FinderCacheUtil.putResult(FINDER_PATH_FIND_ALL, finderArgs,
						list);
				}

				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Removes all the services where groupId = &#63; from the database.
	 *
	 * @param groupId the group id to search with
	 * @throws SystemException if a system exception occurred
	 */
	public void removeByGroupId(long groupId) throws SystemException {
		for (Service service : findByGroupId(groupId)) {
			remove(service);
		}
	}

	/**
	 * Removes the service where groupId = &#63; and title = &#63; from the database.
	 *
	 * @param groupId the group id to search with
	 * @param title the title to search with
	 * @throws SystemException if a system exception occurred
	 */
	public void removeByName(long groupId, String title)
		throws NoSuchServiceException, SystemException {
		Service service = findByName(groupId, title);

		remove(service);
	}

	/**
	 * Removes the service where groupId = &#63; and serviceId = &#63; from the database.
	 *
	 * @param groupId the group id to search with
	 * @param serviceId the service id to search with
	 * @throws SystemException if a system exception occurred
	 */
	public void removeByServiceId(long groupId, String serviceId)
		throws NoSuchServiceException, SystemException {
		Service service = findByServiceId(groupId, serviceId);

		remove(service);
	}

	/**
	 * Removes all the services where groupId = &#63; and linkId = &#63; from the database.
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @throws SystemException if a system exception occurred
	 */
	public void removeByLinkId(long groupId, long linkId)
		throws SystemException {
		for (Service service : findByLinkId(groupId, linkId)) {
			remove(service);
		}
	}

	/**
	 * Removes all the services from the database.
	 *
	 * @throws SystemException if a system exception occurred
	 */
	public void removeAll() throws SystemException {
		for (Service service : findAll()) {
			remove(service);
		}
	}

	/**
	 * Counts all the services where groupId = &#63;.
	 *
	 * @param groupId the group id to search with
	 * @return the number of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public int countByGroupId(long groupId) throws SystemException {
		Object[] finderArgs = new Object[] { groupId };

		Long count = (Long)FinderCacheUtil.getResult(FINDER_PATH_COUNT_BY_GROUPID,
				finderArgs, this);

		if (count == null) {
			StringBundler query = new StringBundler(2);

			query.append(_SQL_COUNT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_GROUPID_GROUPID_2);

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				count = (Long)q.uniqueResult();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (count == null) {
					count = Long.valueOf(0);
				}

				FinderCacheUtil.putResult(FINDER_PATH_COUNT_BY_GROUPID,
					finderArgs, count);

				closeSession(session);
			}
		}

		return count.intValue();
	}

	/**
	 * Counts all the services where groupId = &#63; and title = &#63;.
	 *
	 * @param groupId the group id to search with
	 * @param title the title to search with
	 * @return the number of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public int countByName(long groupId, String title)
		throws SystemException {
		Object[] finderArgs = new Object[] { groupId, title };

		Long count = (Long)FinderCacheUtil.getResult(FINDER_PATH_COUNT_BY_NAME,
				finderArgs, this);

		if (count == null) {
			StringBundler query = new StringBundler(3);

			query.append(_SQL_COUNT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_NAME_GROUPID_2);

			if (title == null) {
				query.append(_FINDER_COLUMN_NAME_TITLE_1);
			}
			else {
				if (title.equals(StringPool.BLANK)) {
					query.append(_FINDER_COLUMN_NAME_TITLE_3);
				}
				else {
					query.append(_FINDER_COLUMN_NAME_TITLE_2);
				}
			}

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				if (title != null) {
					qPos.add(title);
				}

				count = (Long)q.uniqueResult();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (count == null) {
					count = Long.valueOf(0);
				}

				FinderCacheUtil.putResult(FINDER_PATH_COUNT_BY_NAME,
					finderArgs, count);

				closeSession(session);
			}
		}

		return count.intValue();
	}

	/**
	 * Counts all the services where groupId = &#63; and serviceId = &#63;.
	 *
	 * @param groupId the group id to search with
	 * @param serviceId the service id to search with
	 * @return the number of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public int countByServiceId(long groupId, String serviceId)
		throws SystemException {
		Object[] finderArgs = new Object[] { groupId, serviceId };

		Long count = (Long)FinderCacheUtil.getResult(FINDER_PATH_COUNT_BY_SERVICEID,
				finderArgs, this);

		if (count == null) {
			StringBundler query = new StringBundler(3);

			query.append(_SQL_COUNT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_SERVICEID_GROUPID_2);

			if (serviceId == null) {
				query.append(_FINDER_COLUMN_SERVICEID_SERVICEID_1);
			}
			else {
				if (serviceId.equals(StringPool.BLANK)) {
					query.append(_FINDER_COLUMN_SERVICEID_SERVICEID_3);
				}
				else {
					query.append(_FINDER_COLUMN_SERVICEID_SERVICEID_2);
				}
			}

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				if (serviceId != null) {
					qPos.add(serviceId);
				}

				count = (Long)q.uniqueResult();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (count == null) {
					count = Long.valueOf(0);
				}

				FinderCacheUtil.putResult(FINDER_PATH_COUNT_BY_SERVICEID,
					finderArgs, count);

				closeSession(session);
			}
		}

		return count.intValue();
	}

	/**
	 * Counts all the services where groupId = &#63; and linkId = &#63;.
	 *
	 * @param groupId the group id to search with
	 * @param linkId the link id to search with
	 * @return the number of matching services
	 * @throws SystemException if a system exception occurred
	 */
	public int countByLinkId(long groupId, long linkId)
		throws SystemException {
		Object[] finderArgs = new Object[] { groupId, linkId };

		Long count = (Long)FinderCacheUtil.getResult(FINDER_PATH_COUNT_BY_LINKID,
				finderArgs, this);

		if (count == null) {
			StringBundler query = new StringBundler(3);

			query.append(_SQL_COUNT_SERVICE_WHERE);

			query.append(_FINDER_COLUMN_LINKID_GROUPID_2);

			query.append(_FINDER_COLUMN_LINKID_LINKID_2);

			String sql = query.toString();

			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(sql);

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(groupId);

				qPos.add(linkId);

				count = (Long)q.uniqueResult();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (count == null) {
					count = Long.valueOf(0);
				}

				FinderCacheUtil.putResult(FINDER_PATH_COUNT_BY_LINKID,
					finderArgs, count);

				closeSession(session);
			}
		}

		return count.intValue();
	}

	/**
	 * Counts all the services.
	 *
	 * @return the number of services
	 * @throws SystemException if a system exception occurred
	 */
	public int countAll() throws SystemException {
		Object[] finderArgs = new Object[0];

		Long count = (Long)FinderCacheUtil.getResult(FINDER_PATH_COUNT_ALL,
				finderArgs, this);

		if (count == null) {
			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(_SQL_COUNT_SERVICE);

				count = (Long)q.uniqueResult();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				if (count == null) {
					count = Long.valueOf(0);
				}

				FinderCacheUtil.putResult(FINDER_PATH_COUNT_ALL, finderArgs,
					count);

				closeSession(session);
			}
		}

		return count.intValue();
	}

	/**
	 * Initializes the service persistence.
	 */
	public void afterPropertiesSet() {
		String[] listenerClassNames = StringUtil.split(GetterUtil.getString(
					com.liferay.util.service.ServiceProps.get(
						"value.object.listener.com.protecmedia.iter.services.model.Service")));

		if (listenerClassNames.length > 0) {
			try {
				List<ModelListener<Service>> listenersList = new ArrayList<ModelListener<Service>>();

				for (String listenerClassName : listenerClassNames) {
					listenersList.add((ModelListener<Service>)InstanceFactory.newInstance(
							listenerClassName));
				}

				listeners = listenersList.toArray(new ModelListener[listenersList.size()]);
			}
			catch (Exception e) {
				_log.error(e);
			}
		}
	}

	public void destroy() {
		EntityCacheUtil.removeCache(ServiceImpl.class.getName());
		FinderCacheUtil.removeCache(FINDER_CLASS_NAME_ENTITY);
		FinderCacheUtil.removeCache(FINDER_CLASS_NAME_LIST);
	}

	@BeanReference(type = ServicePersistence.class)
	protected ServicePersistence servicePersistence;
	@BeanReference(type = ResourcePersistence.class)
	protected ResourcePersistence resourcePersistence;
	@BeanReference(type = UserPersistence.class)
	protected UserPersistence userPersistence;
	private static final String _SQL_SELECT_SERVICE = "SELECT service FROM Service service";
	private static final String _SQL_SELECT_SERVICE_WHERE = "SELECT service FROM Service service WHERE ";
	private static final String _SQL_COUNT_SERVICE = "SELECT COUNT(service) FROM Service service";
	private static final String _SQL_COUNT_SERVICE_WHERE = "SELECT COUNT(service) FROM Service service WHERE ";
	private static final String _FINDER_COLUMN_GROUPID_GROUPID_2 = "service.groupId = ?";
	private static final String _FINDER_COLUMN_NAME_GROUPID_2 = "service.groupId = ? AND ";
	private static final String _FINDER_COLUMN_NAME_TITLE_1 = "service.title IS NULL";
	private static final String _FINDER_COLUMN_NAME_TITLE_2 = "service.title = ?";
	private static final String _FINDER_COLUMN_NAME_TITLE_3 = "(service.title IS NULL OR service.title = ?)";
	private static final String _FINDER_COLUMN_SERVICEID_GROUPID_2 = "service.groupId = ? AND ";
	private static final String _FINDER_COLUMN_SERVICEID_SERVICEID_1 = "service.serviceId IS NULL";
	private static final String _FINDER_COLUMN_SERVICEID_SERVICEID_2 = "service.serviceId = ?";
	private static final String _FINDER_COLUMN_SERVICEID_SERVICEID_3 = "(service.serviceId IS NULL OR service.serviceId = ?)";
	private static final String _FINDER_COLUMN_LINKID_GROUPID_2 = "service.groupId = ? AND ";
	private static final String _FINDER_COLUMN_LINKID_LINKID_2 = "service.linkId = ?";
	private static final String _ORDER_BY_ENTITY_ALIAS = "service.";
	private static final String _NO_SUCH_ENTITY_WITH_PRIMARY_KEY = "No Service exists with the primary key ";
	private static final String _NO_SUCH_ENTITY_WITH_KEY = "No Service exists with the key {";
	private static Log _log = LogFactoryUtil.getLog(ServicePersistenceImpl.class);
}