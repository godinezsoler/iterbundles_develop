AUI.add("aui-color-util",function(E){var I=E.Lang,J=I.isArray,Q=I.isObject,D=I.isString,P=function(A){return A&&(A.slice(-3)=="deg"||A.slice(-1)=="\xb0");},M=function(A){return A&&A.slice(-1)=="%";},N={hs:1,rg:1},O=Math,F=O.max,C=O.min,L=/\s*,\s*/,R=/^\s*((#[a-f\d]{6})|(#[a-f\d]{3})|rgba?\(\s*([\d\.]+%?\s*,\s*[\d\.]+%?\s*,\s*[\d\.]+(?:%?\s*,\s*[\d\.]+)?)%?\s*\)|hsba?\(\s*([\d\.]+(?:deg|\xb0|%)?\s*,\s*[\d\.]+%?\s*,\s*[\d\.]+(?:%?\s*,\s*[\d\.]+)?)%?\s*\)|hsla?\(\s*([\d\.]+(?:deg|\xb0|%)?\s*,\s*[\d\.]+%?\s*,\s*[\d\.]+(?:%?\s*,\s*[\d\.]+)?)%?\s*\))\s*$/i,H=/^(?=[\da-f]$)/,B=/^\s+|\s+$/g,K="";var G={constrainTo:function(U,V,S,T){var A=this;if(U<V||U>S){U=T;}return U;},getRGB:E.cached(function(T){if(!T||!!((T=String(T)).indexOf("-")+1)){return new G.RGB("error");}if(T=="none"){return new G.RGB();}if(!N.hasOwnProperty(T.toLowerCase().substring(0,2))&&T.charAt(0)!="#"){T=G._toHex(T);}var Y;var X;var A;var V;var W;var U=T.match(R);var S;if(U){if(U[2]){A=parseInt(U[2].substring(5),16);X=parseInt(U[2].substring(3,5),16);Y=parseInt(U[2].substring(1,3),16);}if(U[3]){A=parseInt((W=U[3].charAt(3))+W,16);X=parseInt((W=U[3].charAt(2))+W,16);Y=parseInt((W=U[3].charAt(1))+W,16);}if(U[4]){S=U[4].split(L);Y=parseFloat(S[0]);if(M(S[0])){Y*=2.55;}X=parseFloat(S[1]);if(M(S[1])){X*=2.55;}A=parseFloat(S[2]);if(M(S[2])){A*=2.55;}if(U[1].toLowerCase().slice(0,4)=="rgba"){V=parseFloat(S[3]);}if(M(S[3])){V/=100;}}if(U[5]){S=U[5].split(L);Y=parseFloat(S[0]);if(M(S[0])){Y*=2.55;}X=parseFloat(S[1]);if(M(S[1])){X*=2.55;}A=parseFloat(S[2]);if(M(S[2])){A*=2.55;}if(P(S[0])){Y/=360;}if(U[1].toLowerCase().slice(0,4)=="hsba"){V=parseFloat(S[3]);}if(M(S[3])){V/=100;}return G.hsb2rgb(Y,X,A,V);}if(U[6]){S=U[6].split(L);Y=parseFloat(S[0]);if(M(S[0])){Y*=2.55;}X=parseFloat(S[1]);if(M(S[1])){X*=2.55;}A=parseFloat(S[2]);if(M(S[2])){A*=2.55;}if(P(S[0])){Y/=360;}if(U[1].toLowerCase().slice(0,4)=="hsla"){V=parseFloat(S[3]);}if(M(S[3])){V/=100;}return G.hsb2rgb(Y,X,A,V);}U=new G.RGB(Y,X,A,V);return U;}return new G.RGB("error");}),hex2rgb:function(S){var A=this;S=String(S).split("#");S.unshift("#");return A.getRGB(S.join(""));},hsb2rgb:function(){var A=this;var S=A._getColorArgs("hsbo",arguments);S[2]/=2;return A.hsl2rgb.apply(A,S);},hsv2rgb:function(){var c=this;var W=c._getColorArgs("hsv",arguments);var V=c.constrainTo(W[0],0,1,0);var e=c.constrainTo(W[1],0,1,0);var a=c.constrainTo(W[2],0,1,0);var A;var X;var Z;var U=Math.floor(V*6);var Y=V*6-U;var T=a*(1-e);var S=a*(1-Y*e);var d=a*(1-(1-Y)*e);switch(U%6){case 0:A=a;X=d;Z=T;break;case 1:A=S;X=a;Z=T;break;case 2:A=T;X=a;Z=d;break;case 3:A=T;X=S;Z=a;break;case 4:A=d;X=T;Z=a;break;case 5:A=a;X=T;Z=S;break;}return new G.RGB(A*255,X*255,Z*255);},hsl2rgb:function(){var a=this;var c=a._getColorArgs("hslo",arguments);var X=c[0];var d=Math.max(Math.min(c[1],1),0);var W=Math.max(Math.min(c[2],1),0);var V=c[3];var A,Y,Z;if(d==0){A=Y=Z=W;}else{var U=a._hue2rgb;var S=W<0.5?W*(1+d):W+d-W*d;var T=2*W-S;A=U(T,S,X+1/3);Y=U(T,S,X);Z=U(T,S,X-1/3);}return new G.RGB(A*255,Y*255,Z*255,V);},rgb2hex:function(Y,X,T){var S=this;var U=S._getColorArgs("rgb",arguments);var W=U[0];var V=U[1];var A=U[2];return(16777216|A|(V<<8)|(W<<16)).toString(16).slice(1);},rgb2hsb:function(){var A=this;var S=A.rgb2hsv.apply(A,arguments);S.b=S.v;return S;},rgb2hsl:function(){var a=this;var X=a._getColorArgs("rgb",arguments);var A=X[0]/255;var V=X[1]/255;var Y=X[2]/255;var Z=Math.max(A,V,Y);var T=Math.min(A,V,Y);var U;var c;var S=(Z+T)/2;if(Z==T){U=c=0;}else{var W=Z-T;c=S>0.5?W/(2-Z-T):W/(Z+T);switch(Z){case A:U=(V-Y)/W+(V<Y?6:0);break;case V:U=(Y-A)/W+2;break;case Y:U=(A-V)/W+4;break;}U/=6;}return{h:U,s:c,l:S,toString:G._hsltoString};},rgb2hsv:function(){var a=this;var W=a._getColorArgs("rgb",arguments);var A=W[0]/255;var U=W[1]/255;var X=W[2]/255;var Y=Math.max(A,U,X);var S=Math.min(A,U,X);var T;var c;var Z=Y;var V=Y-S;c=Y==0?0:V/Y;if(Y==S){T=0;}else{switch(Y){case A:T=(U-X)/V+(U<X?6:0);break;case U:T=(X-A)/V+2;break;case X:T=(A-U)/V+4;break;}T/=6;}return{h:T,s:c,v:Z,toString:G._hsbtoString};},_getColorArgs:function(W,T){var S=this;var V=[];var A=T[0];if(J(A)&&A.length){V=A;}else{if(Q(A)){var Y=W.split("");var X=Y.length;for(var U=0;U<X;U++){V[U]=A[Y[U]];}}else{V=E.Array(T);}}return V;},_hsbtoString:function(){var A=this;return["hs",(("v" in A)?"v":"b"),"(",A.h,A.s,A.b,")"].join("");},_hsltoString:function(){var A=this;return["hsl(",A.h,A.s,A.l,")"].join("");},_hue2rgb:function(T,S,A){if(A<0){A+=1;}if(A>1){A-=1;}if(A<1/6){return T+(S-T)*6*A;}if(A<1/2){return S;}if(A<2/3){return T+(S-T)*(2/3-A)*6;}return T;},_toHex:function(S){var A=this;if(E.UA.ie){A._toHex=E.cached(function(V){var X;var W=E.config.win;try{var a=new W.ActiveXObject("htmlfile");a.write("<body>");a.close();X=a.body;}catch(Z){X=W.createPopup().document.body;}var U=X.createTextRange();try{X.style.color=String(V).replace(B,K);var Y=U.queryCommandValue("ForeColor");Y=((Y&255)<<16)|(Y&65280)|((Y&16711680)>>>16);return"#"+("000000"+Y.toString(16)).slice(-6);}catch(Z){return"none";}});}else{var T=E.config.doc.createElement("i");T.title="AlloyUI Color Picker";T.style.display="none";E.getBody().append(T);A._toHex=E.cached(function(U){T.style.color=U;return E.config.doc.defaultView.getComputedStyle(T,K).getPropertyValue("color");});}return A._toHex(S);}};G.RGB=function(U,T,S,V){var A=this;if(U=="error"){A.error=1;}else{if(arguments.length){A.r=~~U;A.g=~~T;A.b=~~S;A.hex="#"+G.rgb2hex(A);if(isFinite(parseFloat(V))){A.o=V;}}}};G.RGB.prototype={r:-1,g:-1,b:-1,hex:"none",toString:function(){var A=this;return A.hex;}};E.ColorUtil=G;},"1.0.1",{skinnable:false});