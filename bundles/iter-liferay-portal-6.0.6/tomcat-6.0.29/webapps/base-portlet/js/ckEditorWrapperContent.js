function ckEditorWrapperContent(classDivWrapper)
{
	if(typeof(classDivWrapper) == 'undefined') {
		//Clase por defecto para los div a reemplazar
		var classDivWrapper = "ckeditor-wrapper-content";
	}
	 
	jQryIter("."+classDivWrapper).each(function(position){
		var iframeID = "iframe-"+(position+1);
		//Contenido del div actual
		var divFullContent = jQryIter(this);
		var extraHeight = 20;
		var extraWidth = 10;
		
		if(navigator.userAgent.match(/MSIE [6-8]\.(?!.*Trident\/[5-9])/) !== null) {
			extraHeight = extraHeight + 10;
		} 
		
		//Creando Iframe
		var iframe = jQryIter('<iframe/>', {id:iframeID, frameborder:'0',border:'0',style:'border:0',height:'100%',width:'100%',scrolling:'no'});
		jQryIter(iframe).load(function(){
			//Reemplazamos el contenido actual del iframe por el contenido final con las imagenes
			jQryIter(iframe).contents().find('body').html("<div class='main-wrapper'>"+jQryIter(divFullContent).html()+"</div>");
			jQryIter(iframe).contents().find('.main-wrapper').css("display", "inline-block");
			
			//Alto inicial del iframe
			jQryIter(iframe).height(jQryIter(iframe).contents().find('.main-wrapper').height()+extraHeight+"px");
			
			//Bucle por las imagenes del contenido, para recuperar el alto y ancho del contenido para ajustar el iframe
			jQryIter(iframe).contents().find(".main-wrapper img").load(function(){
				
				//Recuperando y ajustando ancho del iframe
				var iframeWidth = jQryIter(iframe).width();
				var contentiframeWidth = jQryIter(iframe).contents().find('.main-wrapper').width();
				jQryIter(iframe).width(contentiframeWidth+extraWidth+"px");
								
				//Recuperando y ajustando alto del iframe
				var iframeHeight = jQryIter(iframe).height();
				var contentiframeHeight = jQryIter(iframe).contents().find('.main-wrapper').height();
				jQryIter(iframe).height(contentiframeHeight+extraHeight+"px");
				
			});
		});
		
		//Reemplazando el div por el iframe creado
		jQryIter(this).replaceWith(iframe);
	});	 
}