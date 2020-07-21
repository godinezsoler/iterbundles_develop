/**
 * A jQuery plugin providing "endless scrolling" based on a regular HTML pager
 *
 * @author Gerard van Helden <drm@melp.nl>
 */
;(function($)
{
    $.fn.extend(
	{
        'wlazyloadPager': function(pager, itemSelector, threshold)
		{
            var threshold = threshold || 200;
            return $(this).each(function(i, e)
			{
                $(pager, this).hide();
                
                var loading = false, endreached = false;
                
                $(window).scroll(function()
				{
                    if(loading || endreached)
                        return;
                    if($(document).height() - $(window).height() <= $(window).scrollTop() + threshold) 
					{
                        loading = true;
                        var next = $(pager).find('div.wlazyload').first();
                        if (next.length) 
						{
                        	var url = next.attr('src');
                        	$.ajax({url: url,}).success(function(data) 
							{
								var dom = $(data);
								$(pager, e).replaceWith(dom.find(pager).hide());
								$(e).append(dom.find(itemSelector));
								loading = false
                            });
                        } 
						else 
						{
                            endreached = true;
                        }
                    }
                });
            });
        }
    });
})(jQryIter);

