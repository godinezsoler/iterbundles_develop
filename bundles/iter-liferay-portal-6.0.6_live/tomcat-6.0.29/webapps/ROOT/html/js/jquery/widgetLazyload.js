(function($)
{
	$.fn.extend(
	{
        'wlazyloadPager': function(itemSelector, threshold)
		{
            threshold = threshold || 200;
            
			// Comprueba que existan widget bajo demanda en la página
            var widgets = $(document).find(itemSelector);
			if (widgets.size() == 0)
				return;
			
			// Se queda con el primero
			elem = widgets.first();
            	
			// Comprueba si está visible (Teniendo en cuenta el umbral)
			var docWindow = $(window);
			var docViewTop = docWindow.scrollTop();
			var docViewBottom = docViewTop + docWindow.height();
			var elemTop = elem.offset().top;
			if (docViewBottom + threshold > elemTop)
			{
				// Carga el contenido
				var url = elem.attr('src');
				$.ajax({url: url,}).success(function(data) 
				{
					if (data != null)
						data = data.trim();
					
					var dom = $(data);
					var elemParent = elem.parent();
					
					elem.remove();
					elemParent.append(dom);
					
					// Se lanzan los eventos notificando que se han cargado los elementos del módulo
					ITER.PORTLET_COMPLETE.launchAll();
					$(document).trigger("widgetCompleteLoad", [dom]);
					
					// Vuelve a lanzar el método para ver si quedan widgets visibles en la página
					$('body.public-page').wlazyloadPager(itemSelector, threshold);
				});
			}
			// Si no está visible, añade la comprobación al evento scroll
			else
			{
				$(window).one("scroll", function()
				{
					$('body.public-page').wlazyloadPager(itemSelector, threshold);
				});
			}
        }
    });
})(jQryIter);