var ITR_ARTICLEVISITS;
typeof ITR_ARTICLEVISITS == "undefined" && (ITR_ARTICLEVISITS = function ()
{
    var func = {};
    func.req = function (group)
    {
        var MAX_LENGTH_URL = 1900;//2048 - el margen suficiente para el path, el grupo y el ultimo identificador
        var listadoElementosA;
        listadoElementosA = jQryIter("a[iteridart]");

        var paramString = "";
        if (listadoElementosA != null && listadoElementosA != undefined)
        {
	        for (var index = 0; index < listadoElementosA.length; index++) 
	        {
	            paramString = paramString.concat("&id=").concat(listadoElementosA.get(index).getAttribute("iteridart"));
	            if (paramString.length >= MAX_LENGTH_URL)
	            	break;
	        }
        }
        if (paramString.length > 0)
        {
             jQryIter.get("/base-portlet/articlevisits?group=".concat(group).concat(paramString), 
                function(data,status)
                {
                    if(status == 'success')
                    {
                    	ITR_ARTICLEVISITS.resp(data);
                    }
                }
            );
        }
    };
    
    func.resp = function (data)
    {
 	   var jsonOb = JSON.parse(data);

 	    jQryIter.each(jsonOb.counts, function(index, value)
 	    {
             var elems = jQryIter("a[iteridart='"+value.id+"']"); 
             if (elems != null && elems != undefined)
             {
                 for(var index = 0; index < elems.length; index++)
                 {
                     var elem = elems[index];
                     
                     // Si la función está definida esta se encargará de pintar el tag en función del número de visitas, de lo contrario se inserta solo el valor
                     elem.innerHTML = ($.isFunction($.showArticleVisits)) ? $.showArticleVisits(value.counter) : value.counter;
                  }
             }	  
        });  
    };
    
    return func;
}());