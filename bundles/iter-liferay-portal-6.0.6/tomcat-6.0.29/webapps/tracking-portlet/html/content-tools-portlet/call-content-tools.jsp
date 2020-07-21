


<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<c:if test='<%= ((articleId != null && !articleId.isEmpty()) &&
				(portalURL != null && !portalURL.isEmpty()) &&
				(currentURL != null && !currentURL.isEmpty()))
			%>'>

<%
	String host = IterURLUtil.getIterHost();
	Document canonicalURLDom = IterURLUtil.getArticleInfoByLayoutUUID(scopeGroupId, articleId, null, true);
	String shortURL = host + XMLHelper.getStringValueOf(canonicalURLDom, "/rs/row/@canonicalURL", StringPool.BLANK);
%>

	<portlet:renderURL windowState="<%= LiferayWindowState.POP_UP.toString() %>" var="sendMailURL" >
		<portlet:param name="view" value="send-mail" />
		<portlet:param name="shortURL" value="<%=shortURL %>" />
	</portlet:renderURL>
	
	<div class="">
		<ul class="nav nav_herramientas">
			<c:if test='<%= (shortURL != null && !shortURL.isEmpty())%>'>
				<li class="enviar">
					<a href="javaScript:newPopup('<%= sendMailURL.toString() %>')"><liferay-ui:message key="content-tools-send-to-a-friend" /></a>
				</li>
			</c:if>
			<li class="imprimir">
				<a href="javascript:imprimir()"	title="<liferay-ui:message key="content-tools-print-this-new" />"><liferay-ui:message key="content-tools-print" /></a>
			</li>
		</ul>
	</div>

	<div class="text_size">
		<ul class="herramientas">
			<li class="disminuyeletra"><a href="javascript:disminuyeLetra()" title="<liferay-ui:message key="content-tools-decrease-text-size" />"><liferay-ui:message key="content-tools-decrease-text-size" /></a></li>
			<li class="aumentaletra"><a href="javascript:aumentaLetra()" title="<liferay-ui:message key="content-tools-increase-text-size" />"><liferay-ui:message key="content-tools-increase-text-size" /></a></li>
		</ul>
	</div>

	<script type="text/javascript">
	
		// Aumentar/Disminuir letra
	
		var tamanoLetrapordefecto = 6;
		var tamanoLetra = tamanoLetrapordefecto;
		var tamanoLetraminimo = 4;
		var tamanoLetramaximo = 8;
		var identidadLetra;
		var popupWindow;
	
		function aumentaLetra() {
		    if (tamanoLetra < tamanoLetramaximo) {
			    tamanoLetra += 1;
			    identidadLetra = document.getElementById('size');
			    identidadLetra.className = 'texto sizeletra' + tamanoLetra;
		    }
		}
	
		function disminuyeLetra() {
		    if (tamanoLetra > tamanoLetraminimo) {
			    tamanoLetra -= 1;
			    identidadLetra = document.getElementById('size');
			    identidadLetra.className = 'texto sizeletra' + tamanoLetra;
		    }
		}
	
		// Imprimir
	
		function imprimir() {
		    window.print();
		}

		// Popup
		
		function newPopup(url) {
			popupWindow = window.open(
				url,'newPopup','height=350,width=300,left=10,top=10,resizable=no' +
					',scrollbars=yes,toolbar=no,menubar=no,location=no,directories=no,status=yes');
		}

	</script>
	
</c:if>