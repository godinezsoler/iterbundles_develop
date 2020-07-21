/*
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
*/
// start variables

bg1 = '#A9A9A9'; // top n bottom of puzzle, headers in help contents
bg2 = '#ff0000'; // puzzle margin
bg3 = '#ffffff'; // puzzle area 
bg4 = '#A9A9A9'; // help contents

goodColor = '#0000ff';
badColor  = '#ff0000';

// end variables

ua   = navigator.userAgent.toLowerCase();
ie6  = (ua.indexOf("msie") 		!= -1 && ua.indexOf("netscape") == -1);
nnf  = (ua.indexOf("netscape")  != -1 && ua.indexOf("gecko") != -1);
nni  = (ua.indexOf("netscape")  != -1 && ua.indexOf("msie") != -1);
nn6  = (ua.indexOf("netscape")  != -1);
gek  = (ua.indexOf("gecko")     != -1);
ff1  = (ua.indexOf("firefox")   != -1);
opr  = (ua.indexOf("opera")     != -1);
mac  = (ua.indexOf("mac")       != -1);
web  = (ua.indexOf("webtv")     != -1);
saf  = (ua.indexOf("safari")    != -1);
kon  = (ua.indexOf("konqueror") != -1);
nn4  = (document.layers);

hDivs = new Array(82);
for(i = 1; i < 82; i++){
	hDivs[i] = new Array(10);
	
	for(j = 1; j < 10; j++){
		hDivs[i][j] = 1;
	}
}



function createGame(){
	
	games = new Array (
		"362498175891756342475312869916523784247861593583947621658234917734189256129675438",
		"819674325563281947742593681638945172971328456254167893185739264396452718427816539",
		"175328496942671385368594271829135647653487912714962538231846759487259163596713824",
		"123456789456789123789123456234567891567891234891234567345678912678912345912345678",
		"846157329723489165195263784531792648462318957978645231689531472317824596254976813",
		"167538942258794631934162587716345298342689175589217463493851726671923854825476319",
		"149386527372154689658927431764215893235698714981473256896541372513762948427839165",
		"694325781825917346731648295178254963956183427342769158269571834413896572587432619",
		"145697823867123549932584176271835964456912387398746251624359718713268495589471632",
		"215349768763158492948627153591873246384296571672415389439561827856732914127984635",
		"912856347476293185583174926761425839259738614834619572197582463625341798348967251",
		"796521384824736591531849627953478162217963458468152973182395746379684215645217839",
		"257841693649537281318629547592176834784395126136482975463918752921754368875263419",
		"768319425415287693329645187653421879871936254942758361137892546586174932294563718"
	);
	
	gameLevel = f.gLevel.value;
	
	gameAnswers = new Array();
	gameAnswers[0] = 0;
	
	gameDefaults = new Array();
	gameDefaults[0] = 0;
	
	// which game
	gameNum =randomNumber(0,13);
	
	strPos = 1;
	for(a = 1; a < 10; a++){
		
		
		getSpaces();
		
		
		for(b = 1; b < 10; b++){
			theDiv = 'pDiv' + strPos;
			
			if(b == spaces[1] || b == spaces[2] || b == spaces[3] || b == spaces[4] || b == spaces[5]|| b == spaces[6]){
				document.getElementById(theDiv).innerHTML = "";
				gameDefaults[strPos] = 0;
			} else {
				document.getElementById(theDiv).innerHTML = games[gameNum].charAt(strPos - 1);
				gameDefaults[strPos] = 1;
			}
			
			gameAnswers[strPos] = games[gameNum].charAt(strPos - 1);
			strPos++;
		}
	}
	
	if(cFocus){
		theDiv = 'cDiv' + cFocus;
		document.getElementById(theDiv).style.borderColor = "#ffffff";
		document.getElementById(theDiv).style.border = "#ffffff solid 2px";
		
		theDiv = 'hDiv' + cFocus;
		document.getElementById(theDiv).style.background = "#ffffff";
		document.getElementById(theDiv).style.borderBottom = "";
		
		theDiv = 'pDiv' + cFocus;
		document.getElementById(theDiv).style.background = "#ffffff";
		document.getElementById(theDiv).style.borderTop    = "";
		
		cFocus = 0;
	}
	
	
	counters = new Array();
	counters[0] = 0;
	counters[1] = 0;
	counters[2] = 0;
	counters[3] = 0;
	counters[4] = 0;
	counters[5] = 0;
	counters[6] = 0;
	counters[7] = 0;
	counters[8] = 0;
	counters[9] = 0;
	
	// count number sets and reset all helpDigits divs 
	strPos  = 1;
	for(a = 1; a < 10; a++){
		for(b = 1; b < 10; b++){
			theDiv = 'pDiv' + strPos;
			
			if(ie6){
				numEntered = document.getElementById(theDiv).innerText;
			} else {
				numEntered = document.getElementById(theDiv).innerHTML;
				numEntered = numEntered.replace(/.*?>/, "");
				numEntered = numEntered.replace(/<.*/, "");
				
			}
			
			if(numEntered){
				counters[numEntered]++;
			}
			document.getElementById('hDiv' + strPos).innerHTML = '';
			strPos++;
		}
	}
	
	

	
	gameInProgress = 1;
}

function randomNumber(inf,sup){ 
   	numPos = sup - inf; 
   	rand = Math.random() * numPos; 
   	rand = Math.round(rand);
   	return parseInt(inf) + rand ;
} 

function getSpaces(){
	RANDS = new Array(1,2,3,4,5,6,7,8,9);
	
	spaces = new Array();
	spaces[0] = 0;
	spaces[1] = 0;
	spaces[2] = 0;
	spaces[3] = 0;
	spaces[4] = 0;
	spaces[5] = 0;
	spaces[6] = 0;
	
	
	
	if(gameLevel == 'easy'){
		spaces[1] = RANDS[Math.floor(Math.random() * 9)];
		while(spaces[2] == 0 || spaces[2] == spaces[1]){
			spaces[2] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[3] == 0 || spaces[3] == spaces[1] || spaces[3] == spaces[2]){
			spaces[3] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[4] == 0 || spaces[4] == spaces[1] || spaces[4] == spaces[2] || spaces[4] == spaces[3]){
			spaces[4] = RANDS[Math.floor(Math.random() * 9)];
		}
	} else if (gameLevel == 'medium'){
		spaces[1] = RANDS[Math.floor(Math.random() * 9)];
		while(spaces[2] == 0 || spaces[2] == spaces[1]){
			spaces[2] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[3] == 0 || spaces[3] == spaces[1] || spaces[3] == spaces[2]){
			spaces[3] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[4] == 0 || spaces[4] == spaces[1] || spaces[4] == spaces[2] || spaces[4] == spaces[3]){
			spaces[4] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[5] == 0 || spaces[5] == spaces[1] || spaces[5] == spaces[2] || spaces[5] == spaces[3] || spaces[5] == spaces[4]){
			spaces[5] = RANDS[Math.floor(Math.random() * 9)];
		}
		
	} else if (gameLevel == 'hard'){
		spaces[1] = RANDS[Math.floor(Math.random() * 9)];
		while(spaces[2] == 0 || spaces[2] == spaces[1]){
			spaces[2] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[3] == 0 || spaces[3] == spaces[1] || spaces[3] == spaces[2]){
			spaces[3] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[4] == 0 || spaces[4] == spaces[1] || spaces[4] == spaces[2] || spaces[4] == spaces[3]){
			spaces[4] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[5] == 0 || spaces[5] == spaces[1] || spaces[5] == spaces[2] || spaces[5] == spaces[3] || spaces[5] == spaces[4]){
			spaces[5] = RANDS[Math.floor(Math.random() * 9)];
		}
		while(spaces[6] == 0 || spaces[6] == spaces[1] || spaces[6] == spaces[2] || spaces[6] == spaces[3] || spaces[6] == spaces[4] || spaces[6] == spaces[5]){
			spaces[6] = RANDS[Math.floor(Math.random() * 9)];
		}
		
	}
}

function boxFocus(cID){
	if(!gameInProgress) return;
	
	if(cFocus){
		theDiv = 'cDiv' + cFocus;
		document.getElementById(theDiv).style.border = "#ffffff solid 2px";
		
		theDiv = 'hDiv' + cFocus;
		document.getElementById(theDiv).style.background   = "#ffffff";
		document.getElementById(theDiv).style.borderBottom = "#ffffff solid 1px";
		
		theDiv = 'pDiv' + cFocus;
		document.getElementById(theDiv).style.background   = "#ffffff";
		document.getElementById(theDiv).style.borderTop    = "#ffffff solid 1px";
	}
	
	if(gameDefaults[cID] || cID == 0){
		cFocus = 0;
		return;
	}
	
	cFocus = cID;
	
	theDiv = 'cDiv' + cID;
	document.getElementById(theDiv).style.border = "#ffffff ridge 2px";
	
	if(dFocus == 1){
		theDiv = 'hDiv' + cID;
		document.getElementById(theDiv).style.background   = bg1;
		document.getElementById(theDiv).style.borderBottom = "#ffffff solid 1px";
		
		theDiv = 'pDiv' + cID;
		document.getElementById(theDiv).style.background   = "#ffffff";
		document.getElementById(theDiv).style.borderTop    = "#ffffff solid 1px";
	}
	if(dFocus == 2){
		theDiv = 'hDiv' + cID;
		document.getElementById(theDiv).style.background   = "#ffffff";
		document.getElementById(theDiv).style.borderBottom = "#ffffff solid 1px";
		
		theDiv = 'pDiv' + cID;
		document.getElementById(theDiv).style.background   = bg3;
		document.getElementById(theDiv).style.borderTop    = bg3 + " solid 1px";
	}
}

function checkEntry(event){
	if(!gameInProgress) return;
	
	hColor = goodColor;
	
	if(ie6){
		ASCIICode = window.event.keyCode;
	} else {
		ASCIICode = event.keyCode;
	}
	
	//alert(ASCIICode)
	
	// context key
	if(ASCIICode == 93 || ASCIICode == 96){
		return false;
	}
	
	
	if(ASCIICode == 8 && cFocus){
		theDiv = 'pDiv' + cFocus;
		document.getElementById(theDiv).innerHTML = '';
		return false;
	}
	// escape key
	if(ASCIICode == 27 && cFocus){
		theDiv = 'pDiv' + cFocus;
		document.getElementById(theDiv).innerHTML = '';
		return;
	}
	
	// keypads
	if(ASCIICode == 97){
		theNum = String(1);
	} else if(ASCIICode == 98){
		theNum = String(2);
	} else if(ASCIICode == 99){
		theNum = String(3);
	} else if(ASCIICode == 100){
		theNum = String(4);
	} else if(ASCIICode == 101){
		theNum = String(5);
	} else if(ASCIICode == 102){
		theNum = String(6);
	} else if(ASCIICode == 103){
		theNum = String(7);
	} else if(ASCIICode == 104){
		theNum = String(8);
	} else if(ASCIICode == 105){
		theNum = String(9);
	} else {
		theNum = String.fromCharCode(ASCIICode);
	}
	
	if(theNum.search(/\D+/) >= 0 || theNum.search(/0/) >= 0){
		return;
	}
	
	if(cFocus){
		if(dFocus == 1){
			theDiv = 'hDiv' + cFocus;
			
			if(hDivs[cFocus][theNum]){
				// remove it
				hDivs[cFocus][theNum] = 0;
			} else {
				// add it
				hDivs[cFocus][theNum] = theNum;
			}
			
			hStr = '';
			for(a = 0; a < hDivs[cFocus].length; a++){
				if(hDivs[cFocus][a] > 0){
					hStr += hDivs[cFocus][a];
				}
			}
			
			document.getElementById(theDiv).innerHTML = hStr;
		}
		
		if(dFocus == 2){
			
				if(gameAnswers[cFocus] != theNum){
					hColor = badColor;
				}
			
			
			theDiv = 'pDiv' + cFocus;
			document.getElementById(theDiv).innerHTML = '<font color="' + hColor + '">' + theNum + '</font>';
		}
	}
	
	// completed ?
	counters = new Array();
	counters[0] = 0;
	counters[1] = 0;
	counters[2] = 0;
	counters[3] = 0;
	counters[4] = 0;
	counters[5] = 0;
	counters[6] = 0;
	counters[7] = 0;
	counters[8] = 0;
	counters[9] = 0;
	
	strPos  = 1;
	filled  = 0;
	for(a = 1; a < 10; a++){
		for(b = 1; b < 10; b++){
			theDiv = 'pDiv' + strPos;
			
			if(ie6){
				numEntered = document.getElementById(theDiv).innerText;
			} else {
				numEntered = document.getElementById(theDiv).innerHTML;
				numEntered = numEntered.replace(/.*?>/, "");
				numEntered = numEntered.replace(/<.*/, "");
				
			}
			
			if(numEntered){
				filled++;
				counters[numEntered]++;
			}
			strPos++;
		}
	}

	
	for(a = 1; a < counters.length; a++){
		if(counters[a] == 9){
			cbClr = '#00cc00';
		} else {
			cbClr = badColor;
		}
		//document.getElementById('cb'+a).innerHTML = '&nbsp;' + counters[a] + '&nbsp;';
		//document.get.getElementById('cb'+a).style.background = cbClr;
	}
	
	strPos  = 1;
	correct = 0;
	if(filled == 81){
		//stopCounter();
		
		for(a = 1; a < 10; a ++){
			for(b = 1; b < 10; b ++){
				theDiv = 'pDiv' + strPos;
				if(ie6){
					if(document.getElementById(theDiv).innerText == gameAnswers[strPos]){
						correct++;
					}
				} else {
					numEntered = document.getElementById(theDiv).innerHTML;
					numEntered = numEntered.replace(/.*?>/, "");
					numEntered = numEntered.replace(/<.*/, "");
					
					if(numEntered == gameAnswers[strPos]){
						correct++;
					}
				}
				strPos++;
			}
		}
		
		if(correct == 81){
			alert("Well done!");
		} else {
			incorrect = 81 - correct;
			incText   = incorrect == 1 ? "square" : "squares";
			alert("Sorry, you have " + incorrect + " incorrect " + incText);
		}


	}
}

function counter(){
	numSecs++;
	
	secs = numSecs % 60;
	mins = Math.floor(numSecs / 60);
	ours = Math.floor(mins / 60);
	
	if(ours > 0){ mins = mins - ours * 60; }
	
	if(secs < 10) secs = "0" + secs;
	if(mins < 10) mins = "0" + mins;  
	if(ours < 10) ours = "0" + ours;  
	
	if(ours > 0){
		clockStr = ours + ':' + mins + ':' + secs;
	} else {
		clockStr = mins + ':' + secs;
	}
	
	document.getElementById('clock').innerHTML = clockStr;
	timer = window.setTimeout("counter(" + numSecs + ")", 1000);
}

function stopCounter(){
	if(timer) clearTimeout(timer);
}



function restart(){
	//if(!gameInProgress) return ;
	//if(timerPaused){
	//	counter(numSecs);
	//	timerPaused = 0;
	
	if ((document.getElementById('pButton').value)=="Pause"){
		document.getElementById('pTable').style.display = "";
		document.getElementById('pauseMsg').style.display = "none";
		document.getElementById('pButton').value = "Pause";
	} 
	if ((document.getElementById('pButton').value)=='Restart') {
		document.getElementById('pTable').style.display = "none";
		document.getElementById('pauseMsg').style.display = "";
		document.getElementById('pButton').value = "Restart";
	}
	//} else {
	//	if(timer){ clearTimeout(timer); }
	//	timerPaused = 1;
	//	document.getElementById('pTable').style.display = "none";
	//	document.getElementById('pauseMsg').style.display = "";
	//	document.getElementById('pButton').value = "Restart";
	//}
	}

function pause(){
	var nameButton = document.getElementById('pButton').value;
	
	if (nameButton=="Pause"){
		document.getElementById('pTable').style.display = "none";
		document.getElementById('pauseMsg').style.display = "block";
		document.getElementById('pButton').value = "Restart";
	
	}else{
		document.getElementById('pTable').style.display = "block";
		document.getElementById('pauseMsg').style.display = "none";
		document.getElementById('pButton').value = "Pause";
	}
}



function noContext(){return false;}

f                      = "";
//timer                  = "";
//numSecs                = -1;
gameInProgress         = 0;
//timerPaused            = 0;
//helpVisible            = 0;
document.onkeyup       = checkEntry;
document.oncontextmenu = noContext;

