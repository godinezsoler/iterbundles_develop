AUI.add("aui-drawing-safari",function(B){B.Drawing.prototype.safari=function(){var A=this;var C=A.rect(-99,-99,A.get("width")+99,A.get("height")+99).attr({stroke:"none"});setTimeout(function(){C.remove();},0);};},"1.0.1",{requires:["aui-drawing-base"]});