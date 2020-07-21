AUI.add("aui-color-picker-grid-plugin",function(D){var G=D.Lang,B=G.isString,O="colorpickergrid",M="cpgrid",H="ColorPickerGridPlugin",E=D.ClassNameManager.getClassName,N="item",L=E(O),Q=E(O,N),J=E(O,N,"content"),I="",P='"></span></span>',F='"><span class="'+J+'" style="background-color:#',K='<span class="'+Q+'" data-color="';var C=D.Component.create({NAME:O,NS:M,ATTRS:{colors:{value:"websafe",setter:"_setColors"}},EXTENDS:D.Plugin.Base,prototype:{initializer:function(){var A=this;var R=A.get("host");R.set("cssClass",L);A.beforeHostMethod("_renderSliders",A._preventHostMethod);A.beforeHostMethod("_renderControls",A._preventHostMethod);A.beforeHostMethod("bindUI",A._beforeBindUI);A.beforeHostMethod("syncUI",A._beforeSyncUI);A.afterHostMethod("_renderContainer",A._afterRenderContainer);A.after("colorsChange",A._afterColorsChange);},_afterColorsChange:function(R){var A=this;A._uiSetColors(R.newVal);},_afterRenderContainer:function(){var A=this;var R=A.get("host");A._uiSetColors(A.get("colors"));R.after("hexChange",R._updateRGB);R.after("rgbChange",R._updateRGB);var S=R._pickerContainer;S.delegate("click",function(T){R.set("hex",T.currentTarget.attr("data-color"));},"."+Q);},_beforeBindUI:function(){var A=this;var R=A.get("host");R.constructor.superclass.bindUI.apply(R,arguments);return A._preventHostMethod();},_beforeSyncUI:function(){var A=this;var R=A.get("host");R.constructor.superclass.syncUI.apply(R,arguments);return A._preventHostMethod();},_getHex:function(S,R,A){return(16777216|A|(R<<8)|(S<<16)).toString(16).slice(1);},_getWebSafeColors:function(){var R=this;var X=0;var W=0;var A=0;var V=R._getHex;var S=[V(X,W,A)];for(var U=0,T=1;U<256;U+=51,T++){if(X==255&&W==255&&A==255){break;}if(W>255){X+=51;W=U=0;S[T++]=V(X,W,A);}if(A>=255){W+=51;if(W>255){X+=51;W=0;}A=U=0;S[T++]=V(X,W,A);}A+=51;S[T]=V(X,W,A);}return S;},_preventHostMethod:function(){var A=this;return new D.Do.Prevent(null,null);},_setColors:function(R){var A=this;if(R=="websafe"){R=A._getWebSafeColors();}else{if(!G.isArray(R)){R=D.Attribute.INVALID_VALUE;}}return R;},_uiSetColors:function(U){var A=this;var R=[];var S=[K,I,F,I,P];D.each(U,function(W,V,X){S[1]=S[3]=W;R[V]=S.join(I);});var T=A.get("host")._pickerContainer;T.setContent(R.join(I));}}});D.Plugin.ColorPickerGrid=C;},"1.0.1",{requires:["aui-color-picker","plugin"],skinnable:true});