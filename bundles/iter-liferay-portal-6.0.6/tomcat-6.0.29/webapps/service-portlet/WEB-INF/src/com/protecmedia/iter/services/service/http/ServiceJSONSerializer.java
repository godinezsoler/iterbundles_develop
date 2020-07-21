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

package com.protecmedia.iter.services.service.http;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import com.protecmedia.iter.services.model.Service;

import java.util.List;

/**
 * @author    Protecmedia
 * @generated
 */
public class ServiceJSONSerializer {
	public static JSONObject toJSONObject(Service model) {
		JSONObject jsonObj = JSONFactoryUtil.createJSONObject();

		jsonObj.put("id", model.getId());
		jsonObj.put("groupId", model.getGroupId());
		jsonObj.put("linkId", model.getLinkId());
		jsonObj.put("serviceId", model.getServiceId());
		jsonObj.put("title", model.getTitle());
		jsonObj.put("imageId", model.getImageId());

		return jsonObj;
	}

	public static JSONArray toJSONArray(
		com.protecmedia.iter.services.model.Service[] models) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		for (Service model : models) {
			jsonArray.put(toJSONObject(model));
		}

		return jsonArray;
	}

	public static JSONArray toJSONArray(
		com.protecmedia.iter.services.model.Service[][] models) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		for (Service[] model : models) {
			jsonArray.put(toJSONArray(model));
		}

		return jsonArray;
	}

	public static JSONArray toJSONArray(
		List<com.protecmedia.iter.services.model.Service> models) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		for (Service model : models) {
			jsonArray.put(toJSONObject(model));
		}

		return jsonArray;
	}
}