<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.liferay.portal.kernel.util.StringBundler"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.portlet.LiferayWindowState"%>

<div class="set-page-template">
	<portlet:actionURL var="loadURL">	
		<portlet:param name="action" value="load" />
	</portlet:actionURL>
	
	<liferay-portlet:renderURL varImpl="iteratorURL">
		<liferay-portlet:param name="view" value="load" />		
	</liferay-portlet:renderURL>
	
	<aui:fieldset label="set-page-template-view-page-templates-list" >
		<br />
		<div class="portlet-msg-info">
			<span class="displaying-help-message-page-template-holder">
				<liferay-ui:message key="set-page-template-please-select-a-page-template" />
			</span>
		
			<span class="displaying-page-template-id-holder aui-helper-hidden">
				<liferay-ui:message key="displaying-page-content" />: <span class="displaying-page-template-id"></span>
			</span>
		</div>
		
		<aui:form name="fm" method="post" action="<%= loadURL %>" cssClass="customlayout-viewer">
			<aui:input name="id" type="hidden" value="" />	
			
			<div id="templates-content">			
			</div>

			<aui:button-row>
				<aui:button type="submit" value="set-page-template-view-load-template" onclick='<%= renderResponse.getNamespace() + "submitForm();" %>' />
			</aui:button-row>
		</aui:form>
	</aui:fieldset>
</div>

<portlet:renderURL windowState="<%= LiferayWindowState.EXCLUSIVE.toString() %>" var="renderURL" >
	<portlet:param name="view" value="templatesResult" />
</portlet:renderURL>

<aui:script>

	Liferay.provide(
		window,
		'<portlet:namespace />submitForm',
		function <portlet:namespace />submitForm() {			
			submitForm(document.<portlet:namespace />form1);
			window.close();
		},
		['aui-base']
	);

	Liferay.provide(
		window,
		'<portlet:namespace />selectPageTemplate',
		function(id, name) {
			var A = AUI();

			document.<portlet:namespace />fm.<portlet:namespace />id.value = id;
			
			A.one('.displaying-page-template-id-holder').show();
			A.one('.displaying-help-message-page-template-holder').hide();					

			var displayPageTemplateId = A.one('.displaying-page-template-id');

			displayPageTemplateId.set('innerHTML', name + ' (<%= LanguageUtil.get(pageContext, "modified") %>)');
			displayPageTemplateId.addClass('modified');
		},
		['aui-base']
	);
	
	var ajaxFunction = function (numPage) {
			AUI().use('aui-io-request', function(A) {

				var funcion = A.io.request(
					'<%= renderURL.toString() %>',
					{
						data: {							
							numPage: numPage
						},
						on: {
							start: function () {
								var contentNode = A.one('#templates-content');
								if(contentNode) {
									contentNode.html('<div class="loading-animation upload-images"></div>');
								}
							},
							success: function(event, id, obj) {
								var responseData = this.get('responseData');
								
								var contentNode = A.one('#templates-content');
								if(contentNode) {
									contentNode.html(responseData);
								}						
							}
						}
					}
				);
			});
		}
		
		ajaxFunction(1);
</aui:script>
