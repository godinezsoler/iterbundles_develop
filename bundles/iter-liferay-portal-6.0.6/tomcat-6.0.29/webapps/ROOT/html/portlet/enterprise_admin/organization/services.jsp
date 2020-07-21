<%
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
%>

<%@ include file="/html/portlet/enterprise_admin/init.jsp" %>

<%
Organization organization = (Organization)request.getAttribute(WebKeys.ORGANIZATION);

long organizationId = (organization != null) ? organization.getOrganizationId() : 0;

List<OrgLabor> orgLabors = Collections.EMPTY_LIST;

int[] orgLaborsIndexes = null;

String orgLaborsIndexesParam = ParamUtil.getString(request, "orgLaborsIndexes");

if (Validator.isNotNull(orgLaborsIndexesParam)) {
	orgLabors = new ArrayList<OrgLabor>();

	orgLaborsIndexes = StringUtil.split(orgLaborsIndexesParam, 0);

	for (int orgLaborsIndex : orgLaborsIndexes) {
		OrgLabor orgLabor = new OrgLaborImpl();

		orgLabor.setSunOpen(-1);
		orgLabor.setSunClose(-1);
		orgLabor.setMonOpen(-1);
		orgLabor.setMonClose(-1);
		orgLabor.setTueOpen(-1);
		orgLabor.setTueClose(-1);
		orgLabor.setWedOpen(-1);
		orgLabor.setWedClose(-1);
		orgLabor.setThuOpen(-1);
		orgLabor.setThuClose(-1);
		orgLabor.setFriOpen(-1);
		orgLabor.setFriClose(-1);
		orgLabor.setSatOpen(-1);
		orgLabor.setSatClose(-1);

		orgLabors.add(orgLabor);
	}
}
else {
	if (organizationId > 0) {
		orgLabors = OrgLaborServiceUtil.getOrgLabors(organizationId);

		orgLaborsIndexes = new int[orgLabors.size()];

		for (int i = 0; i < orgLabors.size() ; i++) {
			orgLaborsIndexes[i] = i;
		}
	}

	if (orgLabors.isEmpty()) {
		orgLabors = new ArrayList<OrgLabor>();

		OrgLabor orgLabor = new OrgLaborImpl();

		orgLabor.setSunOpen(-1);
		orgLabor.setSunClose(-1);
		orgLabor.setMonOpen(-1);
		orgLabor.setMonClose(-1);
		orgLabor.setTueOpen(-1);
		orgLabor.setTueClose(-1);
		orgLabor.setWedOpen(-1);
		orgLabor.setWedClose(-1);
		orgLabor.setThuOpen(-1);
		orgLabor.setThuClose(-1);
		orgLabor.setFriOpen(-1);
		orgLabor.setFriClose(-1);
		orgLabor.setSatOpen(-1);
		orgLabor.setSatClose(-1);

		orgLabors.add(orgLabor);

		orgLaborsIndexes = new int[] {0};
	}

	if (orgLaborsIndexes == null) {
		orgLaborsIndexes = new int[0];
	}
}

Format timeFormat = FastDateFormatFactoryUtil.getSimpleDateFormat("HH:mm", locale);
%>

<liferay-ui:error-marker key="errorSection" value="services" />

<h3><liferay-ui:message key="services" /></h3>

<liferay-ui:error key="<%= NoSuchListTypeException.class.getName() + Organization.class.getName() + ListTypeConstants.ORGANIZATION_SERVICE %>" message="please-select-a-type" />

<aui:fieldset>

	<%
	Calendar cal = CalendarFactoryUtil.getCalendar();
	String[] days = CalendarUtil.getDays(locale);
	String[] paramPrefixes = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};

	for (int i = 0; i < orgLaborsIndexes.length; i++) {
		int orgLaborsIndex = orgLaborsIndexes[i];

		OrgLabor orgLabor = orgLabors.get(i);

		int[] openArray = new int[paramPrefixes.length];

		for (int j = 0; j < paramPrefixes.length; j++) {
			openArray[j] = ParamUtil.getInteger(request, paramPrefixes[j] + "Open" + orgLaborsIndex, BeanPropertiesUtil.getInteger(orgLabor, paramPrefixes[j] + "Open", -1));
		}

		int[] closeArray = new int[paramPrefixes.length];

		for (int j = 0; j < paramPrefixes.length; j++) {
			closeArray[j] = ParamUtil.getInteger(request, paramPrefixes[j] + "Close" + orgLaborsIndex, BeanPropertiesUtil.getInteger(orgLabor, paramPrefixes[j] + "Close", -1));
		}
	%>

		<aui:model-context bean="<%= orgLabor %>" model="<%= OrgLabor.class %>" />

		<div class="lfr-form-row">
			<div class="row-fields">
				<aui:input name='<%= "orgLaborId" + orgLaborsIndex %>' type="hidden" />

				<aui:select label="type" name='<%= "orgLaborTypeId" + orgLaborsIndex %>' listType="<%= ListTypeConstants.ORGANIZATION_SERVICE %>" />

				<table class="org-labor-table">
				<tr>
					<td></td>

					<%
					for (String day : days) {
					%>

						<th>
							<label><%= day %></label>
						</th>

					<%
					}
					%>

				</tr>
				<tr>
					<td>
						<label><liferay-ui:message key="open" /></label>
					</td>

					<%
					for (int j = 0; j < days.length; j++) {
						String curParam = paramPrefixes[j];
						int curOpen = openArray[j];
					%>

						<td>
							<aui:select label="" name='<%= curParam + "Open" + orgLaborsIndex %>'>
								<aui:option value="-1" />

								<%
								cal.set(Calendar.HOUR_OF_DAY, 0);
								cal.set(Calendar.MINUTE, 0);
								cal.set(Calendar.SECOND, 0);
								cal.set(Calendar.MILLISECOND, 0);

								int today = cal.get(Calendar.DATE);

								while (cal.get(Calendar.DATE) == today) {
									String timeOfDayDisplay = timeFormat.format(cal.getTime());
									int timeOfDayValue = GetterUtil.getInteger(StringUtil.replace(timeOfDayDisplay, StringPool.COLON, StringPool.BLANK));

									cal.add(Calendar.MINUTE, 30);
								%>

									<aui:option label="<%= timeOfDayDisplay %>" selected="<%= (curOpen == timeOfDayValue) %>" value="<%= timeOfDayValue %>" />

								<%
								}
								%>

							</aui:select>
						</td>

					<%
					}
					%>

				</tr>
				<tr>
					<td>
						<label><liferay-ui:message key="close" /></label>
					</td>

					<%
					for (int j = 0; j < days.length; j++) {
						String curParam = paramPrefixes[j];
						int curClose = closeArray[j];
					%>

						<td>
							<aui:select label="" name='<%= curParam + "Close" + orgLaborsIndex %>'>
								<aui:option value="-1" />

								<%
								cal.set(Calendar.HOUR_OF_DAY, 0);
								cal.set(Calendar.MINUTE, 0);
								cal.set(Calendar.SECOND, 0);
								cal.set(Calendar.MILLISECOND, 0);

								int today = cal.get(Calendar.DATE);

								while (cal.get(Calendar.DATE) == today) {
									String timeOfDayDisplay = timeFormat.format(cal.getTime());
									int timeOfDayValue = GetterUtil.getInteger(StringUtil.replace(timeOfDayDisplay, StringPool.COLON, StringPool.BLANK));

									cal.add(Calendar.MINUTE, 30);
								%>

									<aui:option label="<%= timeOfDayDisplay %>" selected="<%= (curClose == timeOfDayValue) %>" value="<%= timeOfDayValue %>" />

								<%
								}
								%>

							</aui:select>
						</td>

					<%
					}
					%>

				</tr>
				</table>
			</div>
		</div>

	<%
	}
	%>

</aui:fieldset>

<aui:script use="liferay-auto-fields">
	Liferay.once(
		'enterpriseAdmin:revealservices',
		function() {
			new Liferay.AutoFields(
				{
					contentBox: '#services > fieldset',
					fieldIndexes: '<portlet:namespace />orgLaborsIndexes'
				}
			).render();
		}
	);
</aui:script>