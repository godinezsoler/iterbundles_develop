<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="/html/sudoku/init.jsp" %>


<%
String bg1 = "#A9A9A9"; // top n bottom of puzzle, headers in help contents
String bg2 = "#ff0000"; // puzzle margin
String bg3 = "#ffffff"; // puzzle area 
String bg4 = "#A9A9A9"; // help contents


%>

<div class='portletsudoku'>

<form name='_controls' class='formsudoku'>
	
	<div class="">	
		<div class="title">
			<%=titlePortlet %>
		</div>
		<br />
		<div class="spacer">
			<br />
		</div>
		
		
		<div class="bodytable" id='pTable'>
			
			
			<table class="tablesudoku">
				<%
					int cID = 1; 
					String mBox= "";
					for(int a = 1; a < 10; a++){
						mBox += "<tr>";
						
						for(int b = 1; b < 10; b++){
							
							String cStyle = "";
							
							if(a < 9){
								cStyle += "borderbottom1 ";
							}
							if(a == 3 || a == 6){
								cStyle += "borderbottom3 ";
							}
							if(b < 9){
								cStyle += "borderright1 ";
							}
							if(b == 3 || b == 6){
								cStyle += "borderright3 ";
							}
							
							mBox += "<td  id='cell" + cID + "' class='" + cStyle +"' onclick='boxFocus(" + cID + ")'>";
							mBox += "<div id='cDiv" + cID + "' class='cDiv'>";
							mBox += "<div id='hDiv" + cID + "' class='hDiv' onclick='dFocus = 2'></div>";
							mBox += "<div id='pDiv" + cID + "' class='pDiv' onclick='dFocus = 2'></div>";
							mBox += "</div>";
							mBox += "</td>";
							
							cID++;
						}
						
						mBox += "</tr>";
						
					}
					out.println(mBox);
				%>
			</table>
			
		</div>	
		
		<div class="spacer"></div>
		<div class="button">
			<!--  <input onclick='createGame();' type='Button' value='New Game' style='width: 50px;'>-->
			<!-- <input onclick='pause();' type='Button' value='Pause' style='width: 50px;' id='pButton'>-->
			<!-- <input onclick='Restart();' type='Button' value='Restart' style='width: 50px;'>-->
			<!--  <div id='clock' class='clock'>00:00</div>-->
			<input name="gLevel" type="hidden" value="<%=level %>"/>
		</div>
		<br />
	</div>
			
</form>	
	
</div>
<script type="text/javascript">
	f = document._controls;
	cFocus = 0;
	dFocus = 0;
	createGame();
</script>
	
