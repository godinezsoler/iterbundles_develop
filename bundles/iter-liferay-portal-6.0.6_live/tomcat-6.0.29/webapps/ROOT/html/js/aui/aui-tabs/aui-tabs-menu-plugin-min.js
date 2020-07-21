AUI.add("aui-tabs-menu-plugin",function(N){var G=N.Lang,J=N.ClassNameManager.getClassName,E="tab",X="tabview",H="tabviewmenu",D="TabViewMenuPlugin",C="contentNode",W="host",K="listNode",B="rendered",S=J(E),I=J(X,"list"),T=J(X,"list","content"),P=J(H,"item"),Q=J(H,"item","label"),O=J(H,"list"),L=J(H,"trigger"),V=J(X,"wrapper"),M="first",R="last",U="<ul></ul>",Z='<li class="'+P+'" data-index="{0}"><a href="javascript:;" class="'+Q+'">{1}</a></li>',F="<div></div>";var Y=N.Component.create({NAME:D,NS:H,EXTENDS:N.Plugin.Base,prototype:{initializer:function(){var A=this;A.afterHostMethod("renderUI",A.renderUI);A.afterHostMethod("bindUI",A.bindUI);A.afterHostMethod("addTab",A.addTab);A.afterHostMethod("removeTab",A.removeTab);A.afterHostMethod("selectTab",A.selectTab);A.afterHostMethod("_onActiveTabChange",A._onActiveTabChange);A.afterHostMethod("_renderTabs",A._renderTabs);A._updateMenuTask=new N.DelayedTask(A._updateMenu,A);A._updateUITask=new N.DelayedTask(A._updateUI,A);},bindUI:function(){var A=this;var a=A.get(W);N.on("windowresize",A._onWindowResize,A);},renderUI:function(){var A=this;var b=A.get(W);var a=b.get(K);var c=A._wrapper;A._listNodeOuterWidth=(parseFloat(a.getComputedStyle("marginLeft"))+parseFloat(c.getComputedStyle("borderLeftWidth"))+parseFloat(a.getComputedStyle("paddingLeft"))+parseFloat(a.getComputedStyle("paddingRight"))+parseFloat(c.getComputedStyle("borderRightWidth"))+parseFloat(a.getComputedStyle("marginRight")));A._updateUITask.delay(1);},addTab:function(b,a){var A=this;var c=A.get(W);if(c.get(B)){A._updateUITask.delay(1);}},removeTab:function(a){var A=this;var b=A.get(W);if(b.get(B)){A._updateUITask.delay(1);}},selectTab:function(a){var A=this;A._updateMenuTask.delay(1);A.fire("selectTab",{index:a});},_hideMenu:function(){var A=this;var b=A.get(W);var a=b.get(K);a.all("."+S).show();if(A._menuOverlay){A._menuOverlay.hide();A._triggerNode.hide();}},_onActiveTabChange:function(a){var A=this;A._updateMenuTask.delay(1);},_onWindowResize:function(b){var a=this;if(a._menuNode){var A=a.get(W).get(C);a._contentWidth=A.get("offsetWidth")-a._listNodeOuterWidth;a._updateMenuTask.delay(1);}else{a._updateUITask.delay(1);}},_renderMenu:function(){var A=this;var a=N.Node.create(F);var b=N.Node.create(U);a.addClass(L);A._wrapper.append(a);var d=new N.OverlayContext({align:{points:["tr","br"]},contentBox:b,cancellableHide:true,cssClass:O,hideDelay:1000,hideOn:"mouseout",showDelay:0,showOn:"click",trigger:a}).render();d.refreshAlign();A._menuNode=b;A._triggerNode=a;A._menuOverlay=d;A.after("selectTab",d.hide,d);var c=A.get(W);b.delegate("click",function(f){var e=f.currentTarget.get("parentNode").attr("data-index");c.selectTab(e);},"li a");},_renderTabs:function(){var a=this;var e=a.get(W);var A=e.get(C);var d=e.get(K);d.removeClass(I);d.addClass(T);var c=e._createDefaultContentContainer();c.addClass(I);var b=e._createDefaultContentContainer();b.addClass(V);b.append(c);A.insert(b,d);c.append(d);a._wrapper=b;a._content=c;},_updateMenu:function(){var n=this;var o=n.get(W);var i=n._menuNode;var c=n._wrapper;if(i){var m=true;var g=c.get("offsetWidth");var j=n._itemsWidth;if(j[j.length-1]>n._contentWidth){var h=o.get(K);var l=h.all("."+S);var f=o.getTabIndex(o.get("activeTab"));var e=(f!=0?j[f]-j[f-1]:0);var A=n._contentWidth;var k=o.selectTab;var d=[];var b=[];l.each(function(q,p,t){var s=(p<f?e:0);if(p!=f&&j[p]+s>A){q.hide();d[0]=p;d[1]=q.get("text");var r=G.sub(Z,d);b.push(r);m=false;}else{q.show();}});i.setContent(b.join(""));var a=i.all("li");a.first().addClass(M);a.last().addClass(R);}if(m){n._hideMenu();}else{n._triggerNode.show();}}},_updateUI:function(){var a=this;var d=a.get(W);a._hideMenu();var A=d.get(C);var c=d.get(K);var b=c.all("."+S);a._contentWidth=A.get("offsetWidth")-a._listNodeOuterWidth;a._itemsWidth=[];var g=a._itemsWidth;var e=(parseFloat(c.getComputedStyle("paddingLeft"))+parseFloat(c.getComputedStyle("paddingRight")));var f=b.size()-1;b.each(function(i,h,k){var l=(parseFloat(i.getComputedStyle("marginRight"))+parseFloat(i.getComputedStyle("marginLeft")));var j=h-1;if(h>0){g[j]=e+l+i.get("offsetLeft");}if(h==f){g[h]=g[j]+i.get("offsetWidth");}});if(g[g.length-1]>a._contentWidth){if(!a._menuOverlay){a._renderMenu();}a._updateMenuTask.delay(1);}}}});N.namespace("Plugin").TabViewMenu=Y;},"1.0.1",{requires:["aui-component","aui-state-interaction","aui-tabs-base","aui-overlay-context","plugin"]});