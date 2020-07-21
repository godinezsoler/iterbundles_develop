jQryIter.b64EncodeUnicode = function(str) {
    // first we use encodeURIComponent to get percent-encoded UTF-8,
    // then we convert the percent encodings into raw bytes which
    // can be fed into btoa.
    return encodeURIComponent(
    		btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
    				function toSolidBytes(match, p1) {
    					return String.fromCharCode('0x' + p1);
    })));
};


jQryIter.b64DecodeUnicode = function(str) {
    // Going backwards: from bytestream, to percent-encoding, to original string.
    return decodeURIComponent(atob(decodeURIComponent(str)).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
};

jQryIter(".iter-faceted-search .element").bind("click",
		function()
		{
			var newUrl = "";
			
			var currentUrl = jQryIter(location).attr('href');
			var clickedItem = jQryIter(this).attr("data-url");
			var urlSearchSeparator = jQryIter(this).closest('.iter-faceted-search').attr('data-separator');
			
			var arrayOfStrings = currentUrl.split(urlSearchSeparator);
			var searchParams = arrayOfStrings[1].split("/");
			
			var filterQuery = searchParams[8];
			
			if(filterQuery=="0")
			{
				// Si no habia ninguna consulta de filtro se añade el valor del elemento seleccionado
				filterQuery = clickedItem;
			}
			else
				{
					filterQuery = jQryIter.b64DecodeUnicode(filterQuery);
					var items = clickedItem.split(":");
					var clickedValue = items[1];
					
					var numItems = items.length;
                    if(numItems>2)
                  	  for(var itm=2;itm<numItems;itm++)
                  		clickedValue = clickedValue + ":" + items[itm];
                    
					var queryFields = filterQuery.split("$");
					var newFilterQuery = [];
					
					//Si el elemento esta seleccionado hay que quitarlo de la url para no filtrar por el. 
					if(jQryIter(this).hasClass("checked"))
					{
						jQryIter(this).removeClass("checked");
						var itemSearch = clickedItem.replace(/[).?+^$|({[\\]\*/g, '\\$&').replace(/:/g, ':.\*');
						var search = new RegExp(itemSearch , "gi");
						
						//Para cada campo de la consulta de filtro se comprueba si encaja con el patron
						jQryIter.each(queryFields, function(idx, value)
					                       {
					                           if(value.match(search))
					                           {
					                        	  // Se elimina la cadena que nos interesa y se limpia el caracter ^
					                              var newValue = value.replace(clickedValue, "");
					                              newValue = newValue.replace(/(:\^)|(\^\^)|(^\$)/g,function replacer(match)
								                                                                 {
								                                                                     var retval = "";
								                                                                     if(match!="")
								                                                                     {
								                                                                         if(match.indexOf(":")!=-1)
								                                                                             retval=":";
								                                                                         else if(match.indexOf("$")!=-1)
								                                                                             retval="$";
								                                                                         else
								                                                                             retval="^";
								                                                                     }
								                                                                     
								                                                                     return retval;   
								                                                                 }
					                              								);
					                              // Si el campo termina en : quiere decir que se ha quedado sin valores, asi que no se añade a la nueva consulta de filtro.
					                              if( !newValue.trim().match(/.*:$/) )
					                            	  newFilterQuery.push( newValue );
					                           }
					                           else
					                           {
					                        	   // Se añade a la nueva consulta de filtro
					                        	   newFilterQuery.push( queryFields[idx] );
					                           }
					                       }
								);
						
						filterQuery = newFilterQuery.join("$");
					}
					else
					{
						// Si es un elemento nuevo se añade a la consulta de filtro.
						jQryIter(this).addClass("checked");
						var itemSearch = items[0]+":.\*";
						var search = new RegExp( itemSearch, "gi");
						
						if(filterQuery.search(search)!=-1)
						{
							// Se busca el campo y se añade el nuevo valor al final.
							jQryIter.each(queryFields, function(idx, value)
								                       {
								                           if(value.match(search))
								                           {
								                              var newValue = value + "^" + items[1];
								                              var numItems = items.length;
								                              if(numItems>2)
								                            	  for(var itm=2;itm<numItems;itm++)
								                            		  newValue = newValue + ":" + items[itm];
								                              
								                              newFilterQuery.push( newValue );
								                           }
								                           else
								                           {
								                        	   newFilterQuery.push( queryFields[idx] );
								                           }
								                       }
										);
							filterQuery = newFilterQuery.join("$");
						}
						else
						{
							// Si el nombre del campo no está en la consulta previa se añade al final.
							filterQuery = filterQuery + "$" + clickedItem;
						}	
					}
				}
			
			if( filterQuery.lastIndexOf("^")==filterQuery.length-1 )
				filterQuery = filterQuery.substring(0, filterQuery.length-1);
			
			newUrl = arrayOfStrings[0] + urlSearchSeparator;
			jQryIter.each(searchParams, function(idx, value)
										{
											if(idx==8)
											{
												if(filterQuery.length==0)
													filterQuery = "0";
												else
													filterQuery = jQryIter.b64EncodeUnicode(filterQuery);
												
												newUrl = newUrl + filterQuery + "/";
											}
											else if(idx==13)
												newUrl = newUrl + "0/";
											else if(idx==14)
												newUrl = newUrl + "1";
											else
												newUrl = newUrl + value + "/";
										}
						);
			
			window.location.href = newUrl;
			
//	alert("HAS HECHO CLICK EN LA FACETA CON ID: " + jQryIter(this).parents(".iter-faceted-search").first().attr("ID") + " \nY EN EL ELEMENTO: " + jQryIter(this).attr("id"));
		}
);

jQryIter(".iter-faceted-search-reset").bind("click",
		function()
		{
			var currentUrl = jQryIter(location).attr('href');
			var facetedType = jQryIter(this).attr("data-type");
			var urlSearchSeparator = jQryIter(this).closest('.iter-faceted-search').attr('data-separator');
			var arrayOfStrings = currentUrl.split(urlSearchSeparator);
			var searchParams = arrayOfStrings[1].split("/");
			
			var filterQuery = jQryIter.b64DecodeUnicode(searchParams[8]);
			var regExpr = "";
			if( filterQuery.toLowerCase().indexOf(facetedType.toLowerCase())>=0 )
			{
				regExpr = new RegExp("(.*)" + facetedType + ":[^\\$]+\\$?(.*)");
				filterQuery = filterQuery.replace(regExpr, "$1$2");
				
				if(filterQuery.indexOf("$")==0)
					filterQuery = filterQuery.substring(1, filterQuery.length);
				if(filterQuery.lastIndexOf("$")==filterQuery.length-1)
					filterQuery = filterQuery.substring(0, filterQuery.length-1);
				
				var newUrl = arrayOfStrings[0] + urlSearchSeparator;
				jQryIter.each(searchParams, function(idx, value)
											{
												if(idx==8)
												{
													if(filterQuery.length==0)
														filterQuery = "0";
													else
														filterQuery = jQryIter.b64EncodeUnicode(filterQuery);
													
													newUrl = newUrl + filterQuery + "/";
												}
												else if(idx==13)
													newUrl = newUrl + "0/";
												else if(idx==14)
													newUrl = newUrl + "1";
												else
													newUrl = newUrl + value + "/";
											}
				);
				
				window.location.href =  newUrl;
			}
		}
);