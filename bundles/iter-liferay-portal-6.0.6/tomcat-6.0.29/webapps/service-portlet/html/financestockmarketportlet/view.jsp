
<%@ include file="init.jsp" %>   

<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@ page import="com.liferay.portal.model.Layout"%>
<%@ page import="javax.portlet.PortletURL"%>
<%@ page import="com.liferay.portal.NoSuchLayoutException"%>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>

<portlet:defineObjects />

<%	
	
	//String tradesListPlain = "IBEX,IXIC,DJA,N225";
	//int updateFrecuency = 10000;
	//boolean showChart = true;

	int counterTabs = 1;
	int counterPanels = 1;
	int i = 0;
	String[] tradesList = tradesListPlain.split(",");
	
	List <String> tradesListId = new ArrayList<String>();
	List <String> tradesListName = new ArrayList<String>();
	
	for (String currentTrade: tradesList) { 
		//System.out.println("prueba= " +currentTrade.split("#").length);
		tradesListId.add(currentTrade.split("#")[0].trim());
		
		if (currentTrade.split("#").length > 1 ){
			tradesListName.add(currentTrade.split("#")[1].trim());
		}else{
			tradesListName.add("");
		}
	}	
	
	%>


<div id="stocks-markets" style="display:none">
	<h4>Bolsas</h4>
	<div id="tabs">
	    <ul>
		<% for (String currentTrade: tradesListId) {%>		
	        <li><a href="#stockmarket-<%= counterTabs %>"><span class="nameTrade_<%=currentTrade%>"></span></a></li>
        <% counterTabs++; } %>
	    </ul>
	    
	    <% for (String currentTrade: tradesListId) { %>
	    
		    <div id="stockmarket-<%=counterPanels %>">
				<div class="stocks-markets-content">
					<div class="stocks-markets-content-title">
						<h1><span class="nameTradeComplete_<%=currentTrade%>"></span></h1>
						<h2 id="arrowUpDown_<%=currentTrade%>" class="positiva">
							<span id="lastTradePrice_<%=currentTrade%>"></span></h2>
					</div>
					<div class="stocks-markets-content-values">
						<strong>Var.   </strong><span class="stocks-markets-content-value-first" id="change_<%=currentTrade%>"></span><br />
						<div class="stocks-markets-content-separator"></div>
						<strong>Var%. </strong><span class="stocks-markets-content-value-second" id="percentChange_<%=currentTrade%>"></span>
					</div>
					<c:if test="<%= showChart %>">
						<div class="stocks-markets-content-chart">
							<span id="image_Trade_container_<%=currentTrade%>"></span>
						</div>
					</c:if>
				</div>
		    </div>
	    
        <% counterPanels++; } %>	
	</div>
</div>


<script language="javascript" type="text/javascript">
var all_trades_success = true;
<% 
for (String currentTrade: tradesListId) { %>
	if (updateTrade("<%=currentTrade%>", "<%=tradesListName.get(i)%>"))
	{
		setInterval('updateTrade("<%=tradesListId%>" , "<%=tradesListName%>")', <%=updateFrecuency%>);
	}else {
		all_trades_success = all_trades_success && false;
	}
<% i++;} %>
if (all_trades_success){
	//Mostrar el bloque
	$('#stocks-markets').attr('style','display:block');
}
</script>




