<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%

boolean enviarA = false;

%>
<ul>
	
<%
for (String socialNetwork : socialNetworks){
%>

<c:choose>
	<%--FACEBOOK --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Facebook")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/facebook.jsp"%>
		</li>
	</c:when>
	
	<%--GOOGLE PLUS --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Google plus")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/google_plus.jsp"%>
		</li>
	</c:when>
	
	<%--TUENTI --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Tuenti")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/tuenti.jsp"%>
		</li>
	</c:when>
	
	<%--TWITTER --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Twitter")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/twitter.jsp"%>
		</li>
	</c:when>
	
	<%--MENEAME --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Meneame")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/meneame.jsp"%>
		</li>
		<%enviarA=true;%>
	</c:when>
	
	<%--DELICIOUS --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Delicious")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/delicious.jsp"%>
		</li>
		<%enviarA=true;%>
	</c:when>
	
	<%--MYSPACE --%>
	<c:when test='<%=socialNetwork.equalsIgnoreCase("Myspace")%>'>
		<li class="nav_compartir">
			<%@ include file="social-networks/myspace.jsp"%>
		</li>
		<%enviarA=true;%>
	</c:when>
</c:choose>
<%

	}
%>
</ul>

<c:if test="<%=enviarA%>">
	<script type="text/javascript">
			
			function EnviarA(type) {
				//var href = encodeURIComponent(window.location.href);
				var href = "";
				var title = document.title;
				
				if (jQryIter("link[rel=canonical]").size()>0)
				{
					href = jQryIter("link[rel=canonical]").attr("href");
				}
				else
				{
					href = document.URL; //Url completa y válida de la nota a comentar document.URL
				}
				
				switch (type)
				{
					case 'meneame':
						url = 'http://meneame.net/submit.php?url=' + href + '&title=' + title;
						break;	
					case 'delicious':
						url = 'http://del.icio.us/post?v=4&noui&amp;jump=close&url=' + href + '&title=' + title;
						break;	
					case 'myspace':
						url = 'http://www.myspace.com/Modules/PostTo/Pages/?u=' + href + '&t=' + title;
						break;	
				}
							
				window.open(url);
			}
			
	</script>
</c:if>
