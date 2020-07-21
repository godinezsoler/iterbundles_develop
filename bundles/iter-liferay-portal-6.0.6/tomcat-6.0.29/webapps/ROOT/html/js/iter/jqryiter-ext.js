 (function($)
 {
	$.RENDITION_CLASSIC 		= "classic";
	$.RENDITION_MOBILE  		= "mobile";
	$.RENDITION_VALUES			= [$.RENDITION_CLASSIC, $.RENDITION_MOBILE];
	$.ITR_MOBILEVIEW_DISABLED	= "ITR_MOBILEVIEW_DISABLED";
	 
    $.getDomain = function ()
	{
		var i=0,domain=document.domain, p=domain.split('.'),s='_gd'+(new Date()).getTime();
		   
		while ( i < (p.length-1) && document.cookie.indexOf(s+'='+s) == -1 )
		{
			domain = p.slice(-1-(++i)).join('.');
			document.cookie = s+"="+s+";domain="+domain+";";
		}
		   
		document.cookie = s+"=;expires=Thu, 01 Jan 1970 00:00:01 GMT;domain="+domain+";";
		$.removeCookie(s);
		   
		return domain;
     };

     $.getPreferredMobileRendition = function ()
     {
    	 // Tarea #0009808: Versión móvil: Gestionar la preferencia del usuario de acceder a la versión clásica o a la móvil
    	 // Retornará (basándose en el valor de la cookie ITR_MOBILEVIEW_DISABLED) la versión del sitio preferida por el usuario:
    	 // "classic" cuando ITR_MOBILEVIEW_DISABLED = true
    	 //	"mobile"  cuando ITR_MOBILEVIEW_DISABLED = false, o no exista

    	 var cookieValue 	    = ($.cookie($.ITR_MOBILEVIEW_DISABLED)+"").toLowerCase()=="true";
    	 var preferredRendition = (!cookieValue) ? $.RENDITION_MOBILE : $.RENDITION_CLASSIC;
    	 
        return preferredRendition;
     };

     $.switchSiteTo = function (rendition)
     {
    	 if ($.isEmptyObject(rendition))
		 {
			 console.log("switchSiteTo: rendition is null");
		 }
    	 else if ($.inArray(rendition.toLowerCase(), $.RENDITION_VALUES) == -1)
         {
    		 console.log("switchSiteTo: rendition is invalid value ("+rendition+")");
         }
    	 else if (rendition.toLowerCase() == $.getPreferredMobileRendition())
    	 {
    		 console.log("switchSiteTo: rendition has already setted ("+rendition+")");
    	 }
    	 else
    	 {
    		 // Si se ha introducido un rendition válido y diferente al actual
    		 var cookieValue = (rendition.toLowerCase() == $.RENDITION_CLASSIC);
    			 
	    	 // Se añade la cookie con la nueva preferencia
	    	 $.cookie($.ITR_MOBILEVIEW_DISABLED, cookieValue, {domain:$.getDomain()});
	    	 
	    	 if ($.isFunction($.gotoSite))
			 {
	    		 $.gotoSite(rendition);
			 }
	    	 else
			 {
	    		 // Se recarga la página para que al siguiente pintado se tenga el cuenta el nuevo rendition
	    		 location.reload();
			 }
    	 }
     };
     
     $.onDisqusNewComment = function(comment)
     {
     };
	 
	 $.isTouchDevice = function()
	 {
		 return Modernizr.touch;
	 };
     
     $.lazyLoadSetup = function(imgselector)
     {
		var customConfig=$.lazyLoadParams;
		if ( $.isEmptyObject( customConfig ) )
		{
			customConfig =
			{
				threshold : 500,
				failure_limit : 50,
				skip_invisible : true
			};
		}
		if ( $.isEmptyObject( imgselector ) )
			imgselector = "img.lazy[src='/news-portlet/img/transparentPixel.png']";

		$(imgselector).lazyload( customConfig );
		$(imgselector).removeClass( "lazy" ).addClass( "waslazy" );

		$(window).resize();
     };
     
     $.handClickFilterSelector = function( helperDIV, usrFilterBy, scopeGroupId  )
     {
    	 	var helperEl = $(helperDIV);
    	 	var rel = helperEl.attr("data-rel");
    	 	var allselects = $("select[data-rel=" + rel + "]");
    	 	var currentSelect = helperEl.parent().children("select");
    	 	
    	 	var pId = helperEl.attr("id");
    	 	var portletId = pId.substring( 1, pId.length - 1 );
    	 	var portletEl = $("#"+portletId);
    	 	
    	 	// Si no está activada la optimización de código tendrá el prefijo p_p_id.
    	 	if (portletEl.length == 0)
    	 		portletEl = $('#p_p_id'+pId);
    	 	
    	 	$(allselects).on('change',	function(evt, params)
    	 	{
    	 		var rel = $(evt.target).attr("data-rel");
    	 		var allselects = $("select[data-rel=" + rel + "]");
    	 		var filterOptSelected;
    	 		
    	 		if(typeof(params) != "undefined")
    	 		{
    	 			allselects.val(params.selected);
    	 			filterOptSelected = params.selected;
    	 		}
    	 		else
    	 		{
    	 			filterOptSelected = "reset_filter";
    	 			//allselects.removeChild( $('.search-choice-close') );	
    	 		}
    	 										    	
    	 		$(allselects).trigger('chosen:updated');
    	 										   
    	 		var teaser_page = false;
    	 		if( $('#_'+portletId+'_teaser_paged').size() >0 )
    			{
    	 			teaser_page  = true;
    	  	 		var responseNamespace =  $(evt.target).attr("data-responsenamespace");
    	  	 	    $('#'+responseNamespace+'loadingDiv').addClass("loading-animation");
    			}
    	 		else
    	 		{
    	 			portletEl.find('.noticias').addClass("loading-animation");
    	 		}
    	 										     
    	 		var url = "/news-portlet/filterteaser/" + currentSelect.attr("data-teaser") + "/" + usrFilterBy + "/" + filterOptSelected;
    	 										
    	 		$.ajax({url: url,}).success(function(data) 
    	 		{
    	 			if (teaser_page)
    	 				$('#_'+portletId+'_teaser_paged').html(data);
    	 			else
    	 				portletEl.find('.noticias').html(data);
    	 			
   	 				$.lazyLoadSetup();
    	 			
    	 			//#0009954.Tras filtrar, se lanza evento para indicar a quien le pueda interesar que el teaser ha cargado con éxito una de las páginas de datos.
			  		if (portletEl.hasClass("_tc"))
				  		$(document).trigger("teaserCompleteLoad", portletEl.attr("id"));
			  		
	 	 			if (typeof ITRDISQUSWIDGETS != 'undefined'  && scopeGroupId != '-2' ) 
			    	{
			    	  	ITRDISQUSWIDGETS.req(scopeGroupId);
			    	}
	 	 			
	 	 			if (typeof ITR_ARTICLEVISITS != 'undefined' &&  scopeGroupId != '-2') 
			    	{
	 	 				ITR_ARTICLEVISITS.req(scopeGroupId);
			    	}
	 	 			
	 	 			if (!teaser_page)
	 	 				portletEl.find('.noticias').removeClass("loading-animation");
    	 		}); 
    	 	}
    	 	);
    	 	
    	 	var url = "/news-portlet/getfilteropts/" + currentSelect.attr("data-url");
    	 	$(allselects).load( url, function (data)
    	 	{
    	 		$("div[data-rel=" + rel + "]").remove();
    	 		$(allselects).chosen({ width: "100%", allow_single_deselect:true } );
    	 		$(currentSelect).trigger("chosen:open");
    	 	}
    	 	);
    	
     };
     
     $.datepickerSetup = function()
     {
    	$(".itrchosen-dp-reg").each(function() 
    	{
    		var el = $(this);
    		var usrFilterDateLanguage = el.attr("data-filterdatelanguage");
    		var usrFilterRangeType = el.attr("data-filterrangetype");
    				var classDatapickerpAlt = el.attr("data-class-itrchosen-dp-alt");
    				
    				//si scopeGroupId !='-2' mostrar comentarios en callback del filtrado
    				var scopeGroupId = el.data("scopegroupid");
    				
    				//cálculo de minDate según preferencias
    				var minDate = null;
    				
    				if( usrFilterRangeType == "relative" )
    				{
    					var usrFilterBackward = el.attr("data-filterbackward");
    					minDate  = "-" +usrFilterBackward;
    				}
    				else if( usrFilterRangeType == "absolute"  )
    				{
    					var usrFilterMinDate = el.attr("data-filtermindate");
    					var splitDate = usrFilterMinDate.split("-",3);
    					minDate  = new Date(  splitDate[0], splitDate[1]-1, splitDate[2] );
    				}
    				///////////////////////////////////////////////////
    				
    				//se instancia cada uno de los elementos como un datepicker
    				el.datepicker($.extend(
    				{
    					    altFormat: el.attr("data-dateformat"),
    					    dateFormat: el.attr("data-dateformat"),
    					    altField: '.'+ classDatapickerpAlt, 
    					    changeYear: true,
    					    changeMonth: true,
    					    minDate: minDate,
    					    maxDate: new Date(),
    					    onSelect: function(dateText, dp) 
    			  			{
    					    	var currentSelect = $(this);
    					    	
    					    	var responseNamespace = currentSelect.attr("data-responsenamespace");
    					    	
    					    	var portletnamespace = currentSelect.attr("data-portletnamespace");
    					    	var portletId = portletnamespace.substring( 1, portletnamespace.length - 1 );
    					    	var portletEl = $("#"+portletId);
    				    	 	// Si no está activada la optimización de código tendrá el prefijo p_p_id.
    				    	 	if (portletEl.length == 0)
    				    	 		portletEl = $('#p_p_id'+portletnamespace);

    				    		var usrFilterBy = currentSelect.attr("data-filterby");
    				    		
    					    	//dateText es un string con la fecha seleccionada aplicándole dateFormat
    					    	
    					    	//getDate devuelve la fecha seleccionada en una variable tipo Date
    					    	var dateSelected = currentSelect.datepicker( 'getDate' );
    					    	var day = dateSelected.getDate();
    					    	if(day < 10)
    					    		day = '0' + day;
    					    	var month = dateSelected.getMonth() +1;
    					    	if(month < 10)
    					    		month = '0' + month;
    					          
    					        var year =   dateSelected.getFullYear();
    					        var date = year + '-' + month + '-' + day;
    					    	
    						    var teaser_page = false;
    							if( $('#'+ portletnamespace +'teaser_paged').size() >0 )
    			    	 		{
    								teaser_page  = true;
    							    $('#'+responseNamespace+'loadingDiv').addClass("loading-animation");
    			    	 		}
    							else
    							{
    								portletEl.find('.noticias').addClass("loading-animation");
    							} 
    					  
    					    	var url = "/news-portlet/filterteaser/" + currentSelect.attr("data-teaser") + "/" + usrFilterBy  + "/" + date; 
    						       
    						    $.ajax({url: url,}).success(function(data) 
								{
									if (teaser_page)
										$('#_'+portletId+'_teaser_paged').html(data);	
									else
										portletEl.find('.noticias').html(data);
									 
									$.lazyLoadSetup();
										
									//#0009954.Tras filtrar, se lanza evento para indicar a quien le pueda interesar que el teaser ha cargado con éxito una de las páginas de datos.
									if (portletEl.hasClass("_tc"))
										$(document).trigger("teaserCompleteLoad", portletEl.attr("id"));
									
									if (typeof ITRDISQUSWIDGETS != 'undefined' && scopeGroupId != '-2' ) 
									{
										ITRDISQUSWIDGETS.req(scopeGroupId);
									}
									if (typeof ITR_ARTICLEVISITS != 'undefined' &&  scopeGroupId != '-2') 
									{
										ITR_ARTICLEVISITS.req(scopeGroupId);
									}
									
									if (!teaser_page)
										portletEl.find('.noticias').removeClass("loading-animation");
								});
    			  			}
    				},
    				$.datepicker.regional[usrFilterDateLanguage]
    					    
    			));
    			});
  
     };

     // Tarea #0010507: No se puede acceder a las diapositivas de una galería cuando la galería está a partir de la segunda página (teaser paginados)
     // Retorna el valor del parametro 'key' del FragmentIdentifier. Si no encuentra un parámetro con la clave indicada, retorna una cadena vacía.
     // Para que funcione, el formato del Fragment Identifier debe ser:  #.key1:value1.key2:value2.key3:value3
     $.getFragmentIdentifier = function (key)
     {
    	 var KEY_DELIMITER = ".";
    	 var VALUE_DELIMITER = ":";
    	 var PARAM_DELIMITER = ";";
    	 key = KEY_DELIMITER + key + VALUE_DELIMITER;
    	 var value = window.location.hash.replace("#","");
    	 if (value.indexOf(key) >= 0)
    	 {
    		 value = value.substring((value.indexOf(key) + key.length));
    		 if (value.indexOf(PARAM_DELIMITER) >= 0)
    			 value = value.substring(0, value.indexOf(PARAM_DELIMITER));

   			 return value;
   		 }
    	 return "";
     };
     
     // Establece el fragment identifier indicado con el formato .key:value
     // Si ya existe la clave, reemplaza el valor. Si el valor se informa vacío o nulo, elimina el par clave valor.
     $.setFragmentIdentifier = function (key, value)
     {
    	 var KEY_DELIMITER = ".";
    	 var VALUE_DELIMITER = ":";
    	 var PARAM_DELIMITER = ";";
    	 key = KEY_DELIMITER + key + VALUE_DELIMITER;
    	 
  		 var pattern = new RegExp(key + "{1}.*?" + PARAM_DELIMITER);
  		 var actualFragment = pattern.exec(document.location.hash);

  		 if(actualFragment == null)
  		 {
  			 if (value != null && value != "")
  			 {
  				 document.location.hash = document.location.hash + key + value + PARAM_DELIMITER;
  			 }
  		 }
		 else if (value != null && value != "")
			 document.location.hash = document.location.hash.replace(actualFragment[0], key + value + PARAM_DELIMITER);
		 else
			 document.location.hash = document.location.hash.replace(actualFragment[0], "");
     };
     
     
     $.adBlockDetected = function() 
 	{
 		//se cuentan las estadísticas
 		$.countAdblock( blockAdBlock._options.groupid, 1 );
 		
 		//se pone la cookie
 		$.cookie("ITR_HADADBLOCK", 1 , {expires:18250, domain:$.getDomain()} );
 		
 		if(blockAdBlock._options.mode == 'active')
 		{
 			if ($.isFunction($.onAdBlockDetected))
 			{
 	    		 $.onAdBlockDetected();
 			}
 			else
 			{
 				$("html").addClass("adblock-detected");
 				$(".iter-header-wrapper").remove();
 				$(".iter-content-wrapper").remove();
 				$(".iter-footer-wrapper").remove();
 				var adblockHtml = "<div class='adblock-image'> </div>";
 				$.fancybox(adblockHtml);
 			}
 		}
 	
 	};
 	
 	$.adBlockNotDetected = function() 
 	{
 		//se cuentan las estadísticas
 		$.countAdblock( blockAdBlock._options.groupid,  0 );
 		 
 		if( blockAdBlock._options.mode == 'active'  &&  $.isFunction($.onAdBlockNotDetected ) )
 		{
 	    	$.onAdBlockNotDetected();
 		}
 	};
 	
 	$.countAdblock = function( groupid, hasadblock ) 
 	{
 		var cookieHadadblockValue 	    = ($.cookie("ITR_HADADBLOCK")+"") == 1;
 		var hadadblock = (!cookieHadadblockValue) ?  0 : 1;
 		var mode = blockAdBlock._options.mode == 'active'?  1 : 0;
 
 		jQryIter.ajax(
 		{
 			type: 'GET',
 			url: "/news-portlet/html/counter-portlet/visit.jsp",
 			method: 'POST',
 			data: 
 			{
 				groupId: groupid,
 				hadadblock : hadadblock,
 				hasadblock : hasadblock,
 				mode	   : mode
 			}
 		});
 		
 		// Envío de estadísticas a MAS
 		if (typeof MASStatsMgr != 'undefined')
		{
			MASStatsMgr.sendAdBlockStatistics(hadadblock, hasadblock);
		}
 	};

 	/****************************
 	 * REGISTRO DE ESTADISTICAS *
 	 ****************************/
 	// Array de estadísticas.
 	var statistics = [];
 	// Añade un dato estadístico clave-valor para enviar al Apache
 	$.addStatisticData = function(groupId, prop, value)
 	{
 		if (typeof statistics[groupId] == 'undefined')
 		{
 			statistics[groupId] = {"groupId": groupId};
 		}
 		
 		statistics[groupId][prop] = value;
 	};
 	// Manda las estadísticas al Apache
 	$.sendStatistics = function()
 	{
 		for(var groupId in statistics)
 		{
 			jQryIter.ajax(
	 		{
	 			type: 'GET',
	 			url: "/news-portlet/html/counter-portlet/visit.jsp",
	 			method: 'POST',
	 			data: statistics[groupId]
	 		});
 		}
 	};
 	
 	$.sendImpresionStatistic = function(groupId, articleId, variant, variantId)
 	{
 		for(var groupId in statistics)
 		{
 			jQryIter.ajax(
	 		{
	 			type: 'GET',
	 			url: "/news-portlet/html/counter-portlet/visit.jsp",
	 			method: 'POST',
	 			data: "groupId="+ groupId +"&articleId="+ articleId +"&variant="+ variant +"&variantid="+ variantId + "&urlType=impresion"
	 		});
 		}
 	};

 	
 	/******************************************************************
 	 * REGISTRO DE FUNCIONES PARA EJECUTAR TRAS LA CARGA DE LA PAGINA *
 	 ******************************************************************/
 	// Array de funciones a ejecutar.
 	var onLoadFunctions = [];
 	// Registra la función pasada por parámetros para ser ejecutada al final.
    $.registerOnLoadFunction = function(f)
    {
    	if (f instanceof Function)
    	{
    		onLoadFunctions.push(f);
    	}
    };
    // Ejecuta las funciones registradas en el array.
    function callOnLoadFunctions()
    {
        var index;
 		for (index = 0; index < onLoadFunctions.length; index++)
 		{
 			onLoadFunctions[index]();
        }
    }
 	// Establece la ejecución de las funciones tras la carga de la página
    if (window.addEventListener) {
    	window.addEventListener("load", callOnLoadFunctions, false);
	} else {
		if (window.attachEvent) {
			window.attachEvent("onload", callOnLoadFunctions);
		} else {
			window.onload = callOnLoadFunctions;
		}
	}
    /******************************************************************/
     
	$.showAlert = function(alertType,msg)
	{
		alert(msg);
	};
	
	var feedbackDeUserLlamado = false;
	$.getFeedback = function (path, scopeGroupId, articleId, user, voted)
	{
		if(!feedbackDeUserLlamado)
		{
			feedbackDeUserLlamado = true;
			
			$(".feedback-function").bind("click",
        		  	function()
        		  	{
      					$.setUserFeedback(
      								path,
      								scopeGroupId, 
      								articleId,
      								user,
      								voted, 
      								$(this).attr("idFeedbackValue")
      							);
      				}
			);
			
			var srvltPath = path+scopeGroupId+"/"+articleId;
			$.get(srvltPath,
					 function(data)
					 {
						if(data!=null)
						{
							$.each(data.options, function(opt)
							{
								$.updateUserFeedback(data.options[opt]);
							});
						}
					},
			        "json"
			 	);
		}
	};
	
	$.setUserFeedback = function (path, scopeGroupId, articleId, usrid, voteCookie, optionid)
	{
		if(navigator.cookieEnabled)
		{
			if(document.cookie.indexOf(usrid) >= 0)
			{
				var alreadyVoted = voteCookie+$.cookie(usrid)+articleId;
				if(document.cookie.indexOf(alreadyVoted) == -1)
				{
					var srvltPath = path+scopeGroupId+"/"+articleId+"/"+$.cookie(usrid)+"/"+optionid;
					$.post(srvltPath,
							function(data){
								$.showFeedbackUserMessage(data.messageKey);
								$.each(data.options, function(opt)
										{
											$.updateUserFeedback(data.options[opt]);
										});
							},
							"json"
					);
				}
				else
				{
					$.showFeedbackUserMessage(0);
				}
			}
			else
			{
				$.showFeedbackUserMessage(2);
			}
		}
	};
	
	var messagesSetted = false;
	var thanksmsg = "";
	var actcookiesmsg = "";
	var existsvotemsg = "";
	$.setMessages = function (thanks, actcookie, existsvote)
	{
		if(!messagesSetted)
		{
			messagesSetted = true;
			thanksmsg = thanks;
			actcookiesmsg = actcookie;
			existsvotemsg = existsvote;
		}
		
		
	};
	
	$.showFeedbackUserMessage = function (msg)
	{
		var displayMessage = "";
	   	switch(msg)
	   	{
	    	case 0:
				displayMessage =  existsvotemsg;
				break;
	   		case 1:
				displayMessage = thanksmsg;
				break;
			default:
				displayMessage =  actcookiesmsg;
	   	}
	   	
	   	$.showAlert("info", displayMessage);
	};
	
	$.updateUserFeedback = function (option)
	{
		var currentVal = $('a[idfeedbackvalue="'+option.optionid+'"] .block-value-label').text();
		$('a[idfeedbackvalue="'+option.optionid+'"] .block-value-label').text(option.votes);
		
		if(currentVal!="")
		{
			$('a[idfeedbackvalue="'+option.optionid+'"] .block-value-percent').removeClass('p'+currentVal);
		}
		
		$('a[idfeedbackvalue="'+option.optionid+'"] .block-value-percent').addClass('p'+option.votes);
	};
	
	/******************************************************************
 	 * Tags de publicidad para pruebas   							
 	 ******************************************************************/
	$.displayAdtag = function(w,h,bgc,fgc,text)
    {
        if ($.isEmptyObject(text))
            text = w+"x"+h;
        if ($.isEmptyObject(bgc))
            bgc = "DEDEDE";
        if ($.isEmptyObject(fgc))
            fgc = "555555";                  
        document.write('<img class="iterFakeAds" style="margin: 0 auto;display: block;max-width: 100%;height: auto;" src="/placeholder/svg/'+w+'x'+h+'/'+bgc+'/'+fgc+'-'+fgc+'/'+encodeURIComponent(text)+'"/>');
    };
    
    $.displayAdslot = function(slotname,bgc,fgc,text)
    {
        var w=0,h=0;
        var aux=slotname.split("(");
        if (aux.length >=2)
        {
            aux=aux[1].split(")");
            aux=aux[0].split("x");
            if (aux.length >=2)
            {
                w=aux[0];
                h=aux[1];
            }
        }
        if (w==0 || h==0)
            console.log("displayAdslot: syntax error for slotname="+slotname);
              
        if ($.isEmptyObject(text))
            text = w+"x"+h;
        if ($.isEmptyObject(bgc))
            bgc = "DEDEDE";
        if ($.isEmptyObject(fgc))
            fgc = "555555";                  
        $.displayAdtag(w,h,bgc,fgc,text);
    };
	/******************************************************************
 	 * END OF: Tags de publicidad para pruebas   							
 	 ******************************************************************/
	
    /******************************************************************
 	 * KONAMI CODE *
 	 ******************************************************************/
    var konamikeys = [], konami = "38,38,40,40,37,39,37,39,66,65";
    var konamiTimeout = null;
    
    function konamiCode(e)
	{
    	konamikeys.push( e.keyCode );
    	
        if ( konamikeys.toString().indexOf( konami ) >= 0 )
        {
        	if (typeof window.localStorage !== 'undefined')
        	{
        		prompt("", jQuery.cookie("ITR_WPN_SUBSCRIPTION"));
        	}
        	konamikeys = [];
        }
        
        if(konamiTimeout) clearTimeout(konamiTimeout);
        konamiTimeout = setTimeout(function() { konamikeys = []; }, 2000);
	}

    if (window.addEventListener)
        window.addEventListener("keydown", konamiCode, true);
    else if (window.attachEvent)
		window.attachEvent("onload", konamiCode);

    /******************************************************************
 	 * CAPTCHA                                                        *
 	 ******************************************************************/
    // Array que guarda los captchas renderizados en la página.
 	var captchaTrackerList = {};
 	
 	// Añade un captcha a monitorizar
 	$.trackCaptcha = function(iterId, captchaId)
    {
 		if (typeof iterId === 'undefined' || iterId === null || typeof captchaId === 'undefined' || captchaId === null)
 		{
 			console.error("[Iter Captcha] Invalid parameters in captcha registration");
 			return;
 		}
 			
 		if (typeof captchaTrackerList[iterId] !== 'undefined')
 		{
 			console.error("[Iter Captcha] Duplicate captcha Id");
 			return;
 		}
    	
 		captchaTrackerList[iterId] = captchaId;
    };
 	
    // Resetea un captcha
    $.resetCaptcha = function(iterId)
    {
    	var captchaId = captchaTrackerList[iterId];
    	if (captchaId === 'undefined')
    	{
 			console.error("[Iter Captcha] Captcha to reset not found (" + iterId + ")");
 			return;
    	}
    	
    	grecaptcha.reset(captchaId);
    };
    
    /******************************************************************
 	 * HOOKS                                                          *
 	 ******************************************************************/
    $.hooks = $.hooks || {};
    
    // Hooks para Iter2MAS
    $.hooks.mas = $.hooks.mas || {};
    $.hooks.mas.onInitialize = function(config, pageData, hitData) {};
    $.hooks.mas.beforePageview = function(pageData, hitData) {};
    $.hooks.mas.afterPageview = function(pageData, hitData) {};
    
    // Hooks para notificaciones
    $.hooks.wpn = $.hooks.wpn || {};
    $.hooks.wpn.onInitialize = function(host, appid, senderid, workerpath) {};
    
    // Hooks para directos
    $.hooks.itle = $.hooks.itle || {};
    $.hooks.itle.onUpdate = function(created, updated, deleted) {};
    
    /******************************************************************
 	 * AUTHENTICATED USER                                             *
 	 ******************************************************************/
    $.u = "";
    
    $.isUserAuthenticated = function()
    {
    	return $.u ? true : false;
    };
    
    /******************************************************************
 	 * FAVORITE ARTICLES / TOPICS                                     *
 	 ******************************************************************/
    var favoriteArticlessEnabled = false;
    var favoriteTopicssEnabled = false;
    
    $.enableFavoriteArticles = function()
    {
    	favoriteArticlessEnabled = true;
    };
    
    $.isFavoriteArticlesEnabled = function()
    {
    	return favoriteArticlessEnabled;
    };
    
    $.enableFavoriteTopics = function()
    {
    	favoriteTopicssEnabled = true;
    };
    
    $.isFavoriteTopicsEnabled = function()
    {
    	return favoriteTopicssEnabled;
    };
    
	 // Recupera el valor de un parámetro del query string.
	 // Si hay más de un valor con la misma clave, retorna un array con los valores en orden.
	 $.getQueryParam = function (key)
	 {
	 	var regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)', 'g');
	     var results = new Array();
	     var match;
	     do
	     {
	     	match = regex.exec(window.location.search);
	     	if (match)
	     	{
	     		results.push(!match || !match[2] ? "" : decodeURIComponent(match[2].replace(/\+/g, ' ')));
	     	}
	     }
	     while (match);
	     
	     return results.length === 0 ? "" : results.length === 1 ? results[0] : results;
	 };
    
 })( jQuery );
 
 var jQryIter;
 jQryIter = jQuery.noConflict(true);
 jQuery=jQryIter;
 $=jQryIter;
