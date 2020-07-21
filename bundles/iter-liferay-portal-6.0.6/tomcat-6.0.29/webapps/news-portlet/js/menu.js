//IMPORTANTE: para que funcione correctamente en dispositivos móviles o tablets es necesario añadir la clase mobileBrowser al tag html si el dispositivo es un móvil o tablet.
function ie7HideMenu(target){	
	jQryIter(target).hover(
		function (){
			setTimeout(function(){ie7HideMenu(target)}, 200);
			
		},
		function (){
			jQryIter(target).removeClass("hover");
			var liNav = jQryIter(target).find(".child-nav");
			jQryIter(liNav).removeClass("mouseenter");
			jQryIter(liNav).addClass("mouseleave");
			if(jQryIter(liNav).is(":visible")){
				jQryIter(liNav).hide();
			}
		}
	);	
}


function mouseOverAndLeaveToMenu(idMenu){
	if(jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav").size() > 0 && !jQryIter.isTouchDevice()) {
		jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav").each(function(){
			var menuTime;
			jQryIter(this).on("mouseover", function(event) {
				
				if(!jQryIter(this).hasClass("hover")){
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav").removeClass("hover");
					jQryIter(this).addClass("hover");
					var liNav = jQryIter(this).find(".child-nav");
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .child-nav").removeClass("mouseenter");
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .child-nav").css('display','none');
					jQryIter(liNav).removeClass("mouseleave");
					jQryIter(liNav).addClass("mouseenter");
					if(!jQryIter(liNav).is(":visible")){
						menuTime = setTimeout(function(){
							if(jQryIter(liNav).hasClass("mouseenter")) {
								jQryIter(liNav).css('display','block');
							}
						},200);
					}
				}
			});		
			
			jQryIter(this).on("mouseleave", function(event)
			{
				var ie7 = false;
				if (/MSIE (\d+\.\d+);/.test(navigator.userAgent))
				{
					var ieversion=new Number(RegExp.$1);
					if (ieversion<=7)
						ie7= true;
				}
				if (ie7)
					setTimeout(function(){ie7HideMenu(event.srcElement)}, 200);
				else
				{
					jQryIter(this).removeClass("hover");
					var liNav = jQryIter(this).find(".child-nav");
					jQryIter(liNav).removeClass("mouseenter");
					jQryIter(liNav).addClass("mouseleave");
					if(jQryIter(liNav).is(":visible"))
					{
						jQryIter(liNav).hide();
						clearTimeout(menuTime);
					}
				}				
			});			
		});
	} 
	
	if(jQryIter.isTouchDevice()) {
		
		jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav").each(function(){
			
			//Navegacion en IOS
			jQryIter(this).on("touchstart click", function(event) {
				if(!jQryIter(this).hasClass("hover") && jQryIter(this).find(".child-nav").size() > 0) {
					// ocultar segundo nivel
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .child-nav").hide();
					// ocultar tercer nivel
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .level-3").attr("style","display:none!important");
					
					//Quitar clase hover a otros menús y esconder las posibles opciones de menú que estuvieran desplegadas.
					jQryIter(".tabnav.hover").removeClass("hover");
					jQryIter(".child-nav").hide();
										
					jQryIter(this).addClass("hover");
					
					var liNav = jQryIter(this).find(".child-nav");
					jQryIter(liNav).show();
					event.preventDefault();
					event.stopPropagation();
				}
				
				if(jQryIter(this).find(".child-nav").size() == 0) {
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .child-nav").hide();
					// ocultar tercer nivel
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .level-3").attr("style","display:none!important");
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav").removeClass("hover");
					//ir a secciÃ³n
					if(jQryIter(this).children("a").size() > 0) {
						window.location.href = jQryIter(this).children("a").attr("href");
					}
				}
				
			});
		});		
	}
}

/*TERCER NIVEL DE NAVEGACION*/
function thirdLevelMouseOverAndLeaveToMenu(idMenu){
	if(jQryIter("#"+ idMenu).find(".menu_secc .level-2 .lst-item").size() > 0 && !jQryIter.isTouchDevice()) {
		jQryIter("#"+ idMenu).find(".menu_secc .level-2 .lst-item").each(function(){
			
			jQryIter(this).on("mouseover", function(event) {
				var liNav = jQryIter(this).find(".level-3");
				jQryIter(liNav).removeClass("mouseleave");
				jQryIter(liNav).addClass("mouseenter");
				if(!jQryIter(liNav).is(":visible")){
					if(jQryIter(liNav).hasClass("mouseenter")) {
						jQryIter(liNav).attr("style","display:block!important");
					}
				}
			});
			
			jQryIter(this).on("mouseleave", function(event) {
				var liNav = jQryIter(this).find(".level-3");
				jQryIter(liNav).removeClass("mouseenter");
				jQryIter(liNav).addClass("mouseleave");
				if(jQryIter(liNav).is(":visible")){
					jQryIter(liNav).attr("style","display:none!important");
				}
			});
		});
	}
	
	if(jQryIter.isTouchDevice()) {
		jQryIter("#"+ idMenu).find(".menu_secc .level-2 .lst-item").each(function(){
			
			jQryIter(this).on("touchstart", function(event) {
				if(!jQryIter(this).hasClass("hover") && jQryIter(this).find(".level-3").size() > 0) {
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .level-3").attr("style","display:none!important");
					jQryIter("#"+ idMenu).find(".parent-nav").first().find("> .tabnav .level-2 .lst-item").removeClass("hover");
					
					jQryIter(this).addClass("hover");
					var liNav = jQryIter(this).find(".level-3");
					jQryIter(liNav).attr("style","display:block!important");
					event.stopPropagation();
					event.preventDefault();
				}
			});			
		});
	}
}


/*MEGAMENU COMPLEX*/
function megaMenuMouseOverAndLeaveToMenu(idMenu){
		if(jQryIter("#"+ idMenu).find(".menu_secc.complex .parent-nav .tabnav").size() > 0 && !jQryIter.isTouchDevice()) {
			jQryIter("#"+ idMenu).find(".menu_secc.complex .parent-nav .tabnav").each(function(){
				jQryIter(this).on("mouseover", function(event) {
					if(jQryIter(this).find(".lst-item").size() > 0 && jQryIter(this).find(".lst-item.selected").size() == 0){
						jQryIter(this).find(".lst-item").first().addClass("selected");
						var dataAssociated = "#" + jQryIter(this).find(".lst-item").first().attr('data-related');
						jQryIter(this).find(dataAssociated).css("display","block");						
					}
				});
			});
		};
		
		if(jQryIter("#"+ idMenu).find(".menu_secc.complex .level-2 .lst-item").size() > 0 && !jQryIter.isTouchDevice()) {
			jQryIter("#"+ idMenu).find(".menu_secc.complex .level-2 .lst-item").each(function(){
			
				jQryIter(this).on("mouseover", function(event) {
					var currentMenu = jQryIter(this).parent().parent();
					var currentMenuElement = jQryIter(this);
					currentMenu.find(".lst-item").removeClass("selected");
					currentMenuElement.addClass("selected");
					var dataAssociated = "#" + currentMenuElement.attr('data-related');
					currentMenu.find(".megaComplex").css("display","none");
					currentMenu.find(dataAssociated).css("display","block");
				});				
			});
		};
		
		if(jQryIter.isTouchDevice()) {
			jQryIter("#"+ idMenu).find(".menu_secc.complex .parent-nav .tabnav").each(function(){
				jQryIter(this).on("touchstart click", function(event) {
					if(jQryIter(this).find(".lst-item").size() > 0 && jQryIter(this).find(".lst-item.selected").size() == 0){
						jQryIter(this).find(".lst-item").first().addClass("selected");
						var dataAssociated = "#" + jQryIter(this).find(".lst-item").first().attr('data-related');
						jQryIter(this).find(dataAssociated).css("display","block");						
					}
				});
			});
			jQryIter("#"+ idMenu).find(".menu_secc.complex .level-2 .lst-item").each(function(){
				jQryIter(this).on("touchstart click", function(event) {
					if(!jQryIter(this).hasClass("selected")){
						var currentMenu = jQryIter(this).parent().parent();
						var currentMenuElement = jQryIter(this);
						currentMenu.find(".lst-item").removeClass("selected");
						currentMenuElement.addClass("selected");
						var dataAssociated = "#" + currentMenuElement.attr('data-related');
						currentMenu.find(".megaComplex").css("display","none");
						currentMenu.find(dataAssociated).css("display","block");
						event.stopPropagation();
						event.preventDefault();
					}
				});
			});
		}	
}

//----------FIN NAVEGACION EN MENU PRINCIPAL----------//

//----------BOTÓN DE CERRAR MENÚ PARA IPAD----------------//
function menuCloseButtonsToIpad(idMenu){
	jQryIter("#" + idMenu).find(".menu_secc .closeMenu").each(function(){
		var closeTag = jQryIter(this);
		closeTag.on("touchstart click", function(event) {
			closeTag.parents(".child-nav").css('display','none');
		});
	});
}

// Funcion que llama al resto
function prepareMenuJs(menuId){		
	ie7HideMenu(menuId);
	mouseOverAndLeaveToMenu(menuId);
	thirdLevelMouseOverAndLeaveToMenu(menuId);
	megaMenuMouseOverAndLeaveToMenu(menuId);
	menuCloseButtonsToIpad(menuId);
}