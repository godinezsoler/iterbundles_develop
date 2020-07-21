// SCRIPTS DE COMPROBACIONES				
//	Descripción: Realiza una validación del elemento del formulario 
//	dependiendo de sus caracteristicas, comprobando si ha sido duplicado.
// 	Parametros de entrada: 
//	_idform: ID del formulario - String -
//	_name: Name del elemento/s - String -
//	_type: Tipo de elemento - String - (field/fieldtype)
//	_required: Si es requerido - Boolean - (field/required)
//	_confirm: Si necesita confirmacion - Boolean - (field/needconfirm)
//	_val_type: Tipo de validacion - String - (validator/type)
//	_val_format: Formato de validación - String - (validator/format)
//	_max: valor maximo para validar - Number - (validator/max)
//	_min: valor minimo para validar - Number - (validator/min)
// Salida: true/false


//  ACTIVAR DEBUG DE VALIDACION -----------------------------------
// Muestra alert con info del error 
    
    var _debug = false;

//-----------------------------------------------------------------

function validar_field(_idform, _id, _name, _type, _required, _confirm, _val_type, _val_format, _max, _min){
	var _elements = jQryIter(".field_elem[name=" + _name +"]");
	var _elements_rep = jQryIter(".field_elem[name=" + _id +"_rep]");
	var _col_elem_OK = true;
	
	var _msg = ""
		
	for (var i = 0; i < _elements.length; ++i) {
			var ele_rep = null;
			if(_confirm && _elements_rep.length > 0){
				ele_rep = _elements_rep[i];
			}
			var _element = _elements[i];
			var _elementInputType = _element.type;
			var _elem_OK = true;
			var _elem_Confirm_OK = true;
			    	
			// es requerido
			if(_required){
					if(_elementInputType == "checkbox"){
						if(_element.checked == false){_elem_OK = false;_msg += "\n Es requerido";}
					}
					else{
						if(_element.value == ""){_elem_OK = false;_msg += "\n Es requerido";}
					}
			}
			// confirmado
			if(_confirm){
				if((jQryIter(_element).attr("orig_value").trim() != "") && (jQryIter(_element).attr("orig_value") != _element.value)){
					//Este es el caso de que el campo tenía un valor al cargar la página y ha sido modificado por el usuario 
					//En este caso hay que comprobar el campo de confirmación 
					if(_element.value != ""){
						if(confirmar_campo(_element) == false) {
							_elem_Confirm_OK = false;_msg += "\n Necesita confirmacion";
						}
					}
				}else if((jQryIter(_element).attr("orig_value").trim() == "") && (_element.value != "")){
				//Este el caso en el que el campo estaba vacío y al enviar el formulario el campo tiene un valor distinto a vacío
					if(confirmar_campo(_element) == false) {
						_elem_Confirm_OK = false;_msg += "\n Necesita confirmacion";
					}
				}
			}
						
			// regexp
			if(_val_type == "regexp" && _element.value != ""){
				var val_a = _val_format;
				if(!val_a.test(_element.value)) {
					_elem_OK = false;
					_msg += "\n Formato incorrecto (Exp. Reg.)";
				}
			}
						
			// email
			if(_val_type == "email" && _element.value != ""){
				if(is_Email(_element.value) == false){_elem_OK = false;_msg += "\n No es E-Mail";}
			}

            // url
            if (_val_type == "url" && _element.value != "") {
                if (is_URL(_element.value) == false) { _elem_OK = false; _msg += "\n No es URL";}
            }
            		
			// dateformat
			if(_val_type == "dateformat" && _element.value != ""){
					try {

						var type = jQryIter(_element).attr("input_type");
						if (type == "calendar") {
						    var l = jQryIter(_element).attr("language")
						    jQryIter.datepicker.parseDate(_val_format, _element.value, {
						        dayNamesShort: jQryIter.datepicker.regional[l].dayNamesShort,
						        dayNames: jQryIter.datepicker.regional[l].dayNames,
						        monthNamesShort: jQryIter.datepicker.regional[l].monthNamesShort,
						        monthNames: jQryIter.datepicker.regional[l].monthNames
						    });
						}
						else {
						    jQryIter.datepicker.parseDate(_val_format, _element.value);
						}
					}
					catch(err){
						_elem_OK = false
						_msg += "\n Formato fecha incorrecto";;
					}
			}


            //timeformat
            if (_val_type == "timeformat" && _element.value != "") {
                if (is_time(_element.value, _val_format) == false) { _elem_OK = false; _msg += "\n Formato hora incorrecto";}
            }

						
			// binary
			if(_type == "binary" && _element.value != ""){
				if(_element.type == "file" && _max > 0){
					if(size_file(_element.value, _max, _min, _element.files[0].size) == false){
							_elem_OK = false;
							_msg += "\n El archivo no cumple las normas";
					}
				}
			}
						
			// string
			if(_type == "string" && _element.value != "" && _val_type == "size"){	
				if(validar_campo(_element, _max, _min) == false){_elem_OK = false;_msg += "\n Nº de caracteres incorrecto";}
			}
					
			// number
			if (_type == "number" && _element.value != "") {
			    if (_val_format != "") {
			        var val_a = _val_format;
			        if (!val_a.test(_element.value)) { _elem_OK = false; _msg += "\n Formato de número incorrecto";}
			    }
			    else {
			        if (jQryIter.isNumeric(_element.value) == false) { _elem_OK = false; _msg += "\n Formato de numero incorrecto";}
			    }
				if(_val_type == "size"){
					if(validar_campo(_element, _max, _min) == false){_elem_OK = false;_msg += "\n Valor de numero incorrecto";}
				}
				if(_val_type == "numberrange"){
					if(validar_rango(_element, _max, _min) == false){_elem_OK = false;_msg += "\n Rango numerico incorrecto";}
				}
                
            }

            // int
            if (_type == "int" && _element.value != "") {
                if (jQryIter.isNumeric(_element.value) == false) { _elem_OK = false; _msg += "\n No es númerico";}
                if (_val_type == "size") {
                    if (validar_campo(_element, _max, _min) == false) { _elem_OK = false; _msg += "\n Valor de numero incorrecto";}
                }
                if (_val_type == "numberrange") {
                    if (validar_rango(_element, _max, _min) == false) { _elem_OK = false; _msg += "\n Rango numerico incorrecto";}
                }
            }
			// array
			if(_type == "array" && _element.value != ""){
				if(_val_type == "size"){
					var n_select = jQryIter(_element).find("option:selected").length;
					if(n_select > _max || n_select < _min){_elem_OK = false;_msg += "\n Nº de elementos seleccionados incorrecto";}
				}
			}
			// modificar clase
			if(_elem_OK){
				jQryIter(_element).removeClass("campo_incorrecto");
			}
			else
			{
				jQryIter(_element).addClass("campo_incorrecto");
				_col_elem_OK = false;
			}
			
			// modificar clase campos confirmación
			if(_elem_Confirm_OK){
				jQryIter(_element).parents(".field_form").first().find(".confirm_field input").removeClass("campo_incorrecto");
			}
			else
			{
				jQryIter(_element).parents(".field_form").first().find(".confirm_field input").addClass("campo_incorrecto");
				_col_elem_OK = false;
			}
			
	}
			
	if(_col_elem_OK){
		return true;
	}
	else
	{
		if(_debug){alert("ERROR AL VALIDAR\n\nID FIELD= " + _id + "\nVALOR FIELD= " + _element.value + "\nCAUSA DEL ERROR:" + _msg);}
		return false;
	}
}


// EXPRESIONES REGULARES 						
var er_telefono = /(^([0-9\s\+\-]+)|^)$/;
var er_email = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.([a-zA-Z]{2,4})+$/;
var er_url = /^(ht|f)tps?:\/\/\w+([\.\-\w]+)?\.([a-z]{2,4}|travel)(:\d{2,5})?(\/.*)?$/i;

function is_Tlf(_valor){
    if(!er_telefono.test(_valor) ) {
        return false;
    }
    return true;
}

function is_Email(_valor){
    if(!er_email.test(_valor)) {
        return false
    }
    return true;
}
function is_URL(_valor) {
    if (!er_url.test(_valor)) {
        return false;
    }
    return true;
}

function size_file(_value, _max, _min, _bytes)
{
    if( _value != "")
    {
        if(_bytes > _max * 1024 )
	        return false;
        
        if(_bytes < _min * 1024)
	        return false;
    }
    return true;
}


function repetir_field(id_field, destino, id_elem, txt_confirm, _value){

    // clona el field_form
    var copia = jQryIter("#"+id_field).clone(true);
    var id_new = "";
    var id_ele_new = "";
    var n_tab = 0;
    if(jQryIter("#"+ destino).find(".field_form").length > 0){
        id_new = jQryIter("#"+destino).find(".field_form")[jQryIter("#"+destino).find(".field_form").length -1].id+"c";
        id_ele_new = jQryIter("#"+destino).find(".field_elem")[jQryIter("#"+destino).find(".field_elem").length -1].id+"c";
        n_tab = jQryIter("#"+destino).find(".field_elem").last().attr("tabindex");
    }
    else{
        id_new = copia[0].id+"_c";
        id_ele_new =  jQryIter(copia[0]).find(".field_elem").attr("id")+"_c";
        n_tab = jQryIter(copia[0]).find(".field_elem").attr("tabindex");
    }	
    copia[0].id = id_new;
    jQryIter(copia[0]).find(".field_elem").attr("id", id_ele_new);
	
	
	/* nuevo campo de confirmacion */
	if (jQryIter(copia[0]).find(".field_elem").attr("iter_needconfirm") == "true") {
		jQryIter(copia[0]).find(".field_elem_rep").attr("id", id_ele_new + "_rep");
		jQryIter(copia[0]).find(".field_elem_rep").attr("tabindex", (parseInt(n_tab)+11));
	}
    
	
	jQryIter(copia[0]).find(".field_elem").attr("tabindex", (parseInt(n_tab)+10));
	
	
    copia.find(':input').each(function(){
        this.value = "";
    });
    
    if(_value != ""){
        jQryIter(copia[0]).find(".field_elem")[0].value = _value;
    }

    copia.find('.repetir_elem').append("<input type='button' value='' class='btt_borrar' onclick='borrar_field(\"#"+id_new+"\")' />");
    copia.appendTo("#"+ destino);
	
    if(jQryIter("#"+id_ele_new).attr("calendar") == "true"){
        var format = jQryIter("#" + id_ele_new).attr("iter_format");
        var idioma = jQryIter("#" + id_ele_new).attr("iter_language");
        jQryIter("#"+id_ele_new).removeClass("hasDatepicker");
        jQryIter("#"+id_ele_new).datepicker(jQryIter.extend({ autoSize: true, changeYear: true, dateFormat: format }, jQryIter.datepicker.regional[idioma])); 	
    }
}


// abre el dialogo para confirmar el campo
//function confirmar_campo(elem, val_ini, tit_dialog) {

function confirmar_campo(elem) {
	if ($("#" + elem.id + "_rep").val() != $(elem).val())
		return false;
	else
		return true;
}



function check_value(_input, id_btt_ok) {
    var id_elem = jQryIter(_input).attr("id_elem");
    if (jQryIter(_input).val() == jQryIter("#" + id_elem).val()) {
        jQryIter("#" + id_btt_ok).show();
    }
    else {
        jQryIter("#" + id_btt_ok).hide();
    }
}
	
    


//  borrar_field()  
//	Descripción: Borrar un elemento del formulario creado por este 
//	id_elem: ID del elemento a borrar - String
	function borrar_field(id_field){
			jQryIter(id_field).remove();
	}
	

	function validar_campo(valor, t_max, t_min){
		if(valor.value.length > t_max || valor.value.length < t_min)
			return false;
		else
			return true;
	}

	function validar_rango(valor, t_max, t_min) {
	    var n_n = parseFloat(valor.value);
        if (n_n > t_max || n_n < t_min)
			return false;
		else
			return true;
	}


	function navigator_form(_id_form, _n_pages) {
	    jQryIter('#pag_' + _id_form).jqPagination({
		    link_string : '/?page={page_number}',
		    max_page: _n_pages,
            current_page : 1,
            paged : function(page) {
                jQryIter('#' + _id_form + ' #pages ._current').removeClass('_current').hide();
			    jQryIter('#' + _id_form + ' #page-'+ page).addClass('_current').show();
		    }
	    });
	}

	function is_time(str, format) {
	    if (format == "h:m a" || format == "hh:mm a") {
	        var at = str.split(":");
	        if (parseInt(at[0]) > 12) { return false; }
	        var m = at[1].split(" ");
	        if (parseInt(m[0]) > 60) { return false; }
	        if (m[1].toUpperCase().indexOf("A") > -1 && m[1].toUpperCase().indexOf("P") > -1) {return false;}
	    }
	    if (format == "h:m:s a" || format == "hh:mm:ss a") {
	        var at = str.split(":");
	        if (parseInt(at[0]) > 12) { return false; }
	        if (parseInt(at[1]) > 60) { return false; }
	        var m = at[2].split(" ");
	        if (parseInt(m[0]) > 60) { return false; }
	        if (m[1].toUpperCase().indexOf("A") > -1 && m[1].toUpperCase().indexOf("P") > -1) {return false;}
	    }
	    if (format == "H:m" || format == "HH:mm") {
	        var at = str.split(":");
	        if (parseInt(at[0]) > 24) { return false; }
	        if (parseInt(at[1]) > 60) { return false; }
	    }
	    if (format == "H:m:s" || format == "HH:mm:ss") {
	        var at = str.split(":");
	        if (parseInt(at[0]) > 24) { return false; }
	        if (parseInt(at[1]) > 60) { return false; }
	        if (parseInt(at[2]) > 60) { return false; }
	    }
	    return true;
	}
	
	function activar_confirm (input){
	
		var id_rep = "#" + input.id + "_rep";
		if(input.value != "")
			jQryIter(id_rep).prop('disabled', false);
		else
			jQryIter(id_rep).prop('disabled', true);
	}
	
	
	function otp_generation(telephoneid, okLabel)
	{
		var button = jQryIter(event.target);
		
		// Elementos que se pasan por parámetros al servlet de OTPServlet
		var data =
		{
			otp_phone : jQuery("#"+telephoneid).val(),
		};
		
		jQryIter.ajax(
		{
		  type: "POST",
		  url: "/user-portlet/otp/generation",
		  data: data,
		  dataType: "json",
		  beforeSend: function()
		  {
			  button.prop( "disabled", true );
		  },
	      error: function(xhr, status, error) 
	      {
	    	  var exception = error;
	    	  try
	    	  {
	    		  exception = JSON.parse(xhr.responseText).exception;
	    	  }
	    	  catch (e){}
	    	  
	    	  showError("", okLabel, exception);
	      },
	      complete: function()
	      {
	    	  button.prop( "disabled", false );
	      }
		});
	}
	
	
	

	
	
/*	DATEPICKER ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
	jQryIter(function (jQryIter) {
		jQryIter.datepicker.regional['it'] = {
		closeText: "Chiudi",
		prevText: "&#x3C;Prec",
		nextText: "Succ&#x3E;",
		currentText: "Oggi",
		monthNames: [ "Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
			"Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre" ],
		monthNamesShort: [ "Gen","Feb","Mar","Apr","Mag","Giu",
			"Lug","Ago","Set","Ott","Nov","Dic" ],
		dayNames: [ "Domenica","Lunedì","Martedì","Mercoledì","Giovedì","Venerdì","Sabato" ],
		dayNamesShort: [ "Dom","Lun","Mar","Mer","Gio","Ven","Sab" ],
		dayNamesMin: [ "Do","Lu","Ma","Me","Gi","Ve","Sa" ],
		weekHeader: "Sm",
		dateFormat: "dd/mm/yy",
		firstDay: 1,
		isRTL: false,
		showMonthAfterYear: false,
		yearSuffix: "" };
	});
	
jQryIter(function (jQryIter) {
	jQryIter.datepicker.regional['de'] = {
	    closeText: 'schließen',
	    prevText: '&#x3c;zurück',
	    nextText: 'Vor&#x3e;',
	    currentText: 'heute',
	    monthNames: ['Januar', 'Februar', 'M\u00E4rz', 'April', 'Mai', 'Juni',
            'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'],
	    monthNamesShort: ['Jan', 'Feb', 'M\u00E4r', 'Apr', 'Mai', 'Jun',
            'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'],
	    dayNames: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag'],
	    dayNamesShort: ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'],
	    dayNamesMin: ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'],
	    weekHeader: 'Wo',
	    firstDay: 1,
	    isRTL: false,
	    showMonthAfterYear: false,
	    yearSuffix: ''
	};
});
jQryIter(function (jQryIter) {
	jQryIter.datepicker.regional['en'] = {
	    closeText: 'Done',
	    prevText: 'Prev',
	    nextText: 'Next',
	    currentText: 'Today',
	    monthNames: ['January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'],
	    monthNamesShort: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
            'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
	    dayNames: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
	    dayNamesShort: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
	    dayNamesMin: ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
	    weekHeader: 'Wk',
	    firstDay: 1,
	    isRTL: false,
	    showMonthAfterYear: false,
	    yearSuffix: ''
	};
});
jQryIter(function (jQryIter) {
	jQryIter.datepicker.regional['es'] = {
	    closeText: 'Cerrar',
	    prevText: '&#x3c;Ant',
	    nextText: 'Sig&#x3e;',
	    currentText: 'Hoy',
	    monthNames: ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
            'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
	    monthNamesShort: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
            'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
	    dayNames: ['Domingo', 'Lunes', 'Martes', 'Mi\u00E9rcoles', 'Jueves', 'Viernes', 'S\u00E1bado'],
	    dayNamesShort: ['Dom', 'Lun', 'Mar', 'Mi\u00E9', 'Juv', 'Vie', 'S\u00E1b'],
	    dayNamesMin: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'S\u00E1'],
	    weekHeader: 'Sm',
	    firstDay: 1,
	    isRTL: false,
	    showMonthAfterYear: false,
	    yearSuffix: ''
	};
});
jQryIter(function (jQryIter) {
	jQryIter.datepicker.regional['fr'] = {
	    closeText: 'Fermer',
	    prevText: '&#x3c;Préc',
	    nextText: 'Suiv&#x3e;',
	    currentText: 'Courant',
	    monthNames: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
            'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
	    monthNamesShort: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun',
            'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'],
	    dayNames: ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'],
	    dayNamesShort: ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'],
	    dayNamesMin: ['Di', 'Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa'],
	    weekHeader: 'Sm',
	    firstDay: 1,
	    isRTL: false,
	    showMonthAfterYear: false,
	    yearSuffix: ''
	};
});
jQryIter(function (jQryIter) {
	jQryIter.datepicker.regional['pt'] = {
	    closeText: 'Fechar',
	    prevText: '&#x3c;Anterior',
	    nextText: 'Seguinte',
	    currentText: 'Hoje',
	    monthNames: ['Janeiro', 'Fevereiro', 'Mar\u00E7o', 'Abril', 'Maio', 'Junho',
	    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'],
	    monthNamesShort: ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
	    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'],
	    dayNames: ['Domingo', 'Segunda-feira', 'Ter\u00E7a-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'S\u00E1bado'],
	    dayNamesShort: ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'S\u00E1b'],
	    dayNamesMin: ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'S\u00E1b'],
	    weekHeader: 'Sem',
	    firstDay: 0,
	    isRTL: false,
	    showMonthAfterYear: false,
	    yearSuffix: ''
	};
});


/* jQryIter Form */
/*!
 * jQryIter Form Plugin
 * version: 3.40.0-2013.08.13
 * @requires jQryIter v1.5 or later
 * Copyright (c) 2013 M. Alsup
 * Examples and documentation at: http://malsup.com/jQryIter/form/
 * Project repository: https://github.com/malsup/form
 * Dual licensed under the MIT and GPL licenses.
 * https://github.com/malsup/form#copyright-and-license
 */
/*global ActiveXObject */
;(function(jQryIter) {
"use strict";

/*
    Usage Note:
    -----------
    Do not use both ajaxSubmit and ajaxForm on the same form.  These
    functions are mutually exclusive.  Use ajaxSubmit if you want
    to bind your own submit handler to the form.  For example,

    jQryIter(document).ready(function() {
        jQryIter('#myForm').on('submit', function(e) {
            e.preventDefault(); // <-- important
            jQryIter(this).ajaxSubmit({
                target: '#output'
            });
        });
    });

    Use ajaxForm when you want the plugin to manage all the event binding
    for you.  For example,

    jQryIter(document).ready(function() {
        jQryIter('#myForm').ajaxForm({
            target: '#output'
        });
    });

    You can also use ajaxForm with delegation (requires jQryIter v1.7+), so the
    form does not have to exist when you invoke ajaxForm:

    jQryIter('#myForm').ajaxForm({
        delegation: true,
        target: '#output'
    });

    When using ajaxForm, the ajaxSubmit function will be invoked for you
    at the appropriate time.
*/

/**
 * Feature detection
 */

var feature = {};
feature.fileapi = jQryIter("<input type='file'/>").get(0).files !== undefined;
feature.formdata = window.FormData !== undefined;

var hasProp = !!jQryIter.fn.prop;

// attr2 uses prop when it can but checks the return type for
// an expected string.  this accounts for the case where a form 
// contains inputs with names like "action" or "method"; in those
// cases "prop" returns the element
jQryIter.fn.attr2 = function() {
    if ( ! hasProp )
        return this.attr.apply(this, arguments);
    var val = this.prop.apply(this, arguments);
    if ( ( val && val.jQryIter ) || typeof val === 'string' )
        return val;
    return this.attr.apply(this, arguments);
};

/**
 * ajaxSubmit() provides a mechanism for immediately submitting
 * an HTML form using AJAX.
 */
jQryIter.fn.ajaxSubmit = function(options) {
    /*jshint scripturl:true */

    // fast fail if nothing selected (http://dev.jQryIter.com/ticket/2752)
    if (!this.length) {
        log('ajaxSubmit: skipping submit process - no element selected');
        return this;
    }

    var method, action, url, $form = this;

    if (typeof options == 'function') {
        options = { success: options };
    }
    else if ( options === undefined ) {
        options = {};
    }

    method = options.type || this.attr2('method');
    action = options.url  || this.attr2('action');

    url = (typeof action === 'string') ? jQryIter.trim(action) : '';
    url = url || window.location.href || '';
    if (url) {
        // clean url (don't include hash vaue)
        url = (url.match(/^([^#]+)/)||[])[1];
    }

    options = jQryIter.extend(true, {
        url:  url,
        success: jQryIter.ajaxSettings.success,
        type: method || jQryIter.ajaxSettings.type,
        iframeSrc: /^https/i.test(window.location.href || '') ? 'javascript:false' : 'about:blank'
    }, options);

    // hook for manipulating the form data before it is extracted;
    // convenient for use with rich editors like tinyMCE or FCKEditor
    var veto = {};
    this.trigger('form-pre-serialize', [this, options, veto]);
    if (veto.veto) {
        log('ajaxSubmit: submit vetoed via form-pre-serialize trigger');
        return this;
    }

    // provide opportunity to alter form data before it is serialized
    if (options.beforeSerialize && options.beforeSerialize(this, options) === false) {
        log('ajaxSubmit: submit aborted via beforeSerialize callback');
        return this;
    }

    var traditional = options.traditional;
    if ( traditional === undefined ) {
        traditional = jQryIter.ajaxSettings.traditional;
    }

    var elements = [];
    var qx, a = this.formToArray(options.semantic, elements);
    if (options.data) {
        options.extraData = options.data;
        qx = jQryIter.param(options.data, traditional);
    }

    // give pre-submit callback an opportunity to abort the submit
    if (options.beforeSubmit && options.beforeSubmit(a, this, options) === false) {
        log('ajaxSubmit: submit aborted via beforeSubmit callback');
        return this;
    }

    // fire vetoable 'validate' event
    this.trigger('form-submit-validate', [a, this, options, veto]);
    if (veto.veto) {
        log('ajaxSubmit: submit vetoed via form-submit-validate trigger');
        return this;
    }

    var q = jQryIter.param(a, traditional);
    if (qx) {
        q = ( q ? (q + '&' + qx) : qx );
    }
    if (options.type.toUpperCase() == 'GET') {
        options.url += (options.url.indexOf('?') >= 0 ? '&' : '?') + q;
        options.data = null;  // data is null for 'get'
    }
    else {
        options.data = q; // data is the query string for 'post'
    }

    var callbacks = [];
    if (options.resetForm) {
        callbacks.push(function() { $form.resetForm(); });
    }
    if (options.clearForm) {
        callbacks.push(function() { $form.clearForm(options.includeHidden); });
    }

    // perform a load on the target only if dataType is not provided
    if (!options.dataType && options.target) {
        var oldSuccess = options.success || function(){};
        callbacks.push(function(data) {
            var fn = options.replaceTarget ? 'replaceWith' : 'html';
            jQryIter(options.target)[fn](data).each(oldSuccess, arguments);
        });
    }
    else if (options.success) {
        callbacks.push(options.success);
    }

    options.success = function(data, status, xhr) { // jQryIter 1.4+ passes xhr as 3rd arg
        var context = options.context || this ;    // jQryIter 1.4+ supports scope context
        for (var i=0, max=callbacks.length; i < max; i++) {
            callbacks[i].apply(context, [data, status, xhr || $form, $form]);
        }
    };

    if (options.error) {
        var oldError = options.error;
        options.error = function(xhr, status, error) {
            var context = options.context || this;
            oldError.apply(context, [xhr, status, error, $form]);
        };
    }

     if (options.complete) {
        var oldComplete = options.complete;
        options.complete = function(xhr, status) {
            var context = options.context || this;
            oldComplete.apply(context, [xhr, status, $form]);
        };
    }

    // are there files to upload?

    // [value] (issue #113), also see comment:
    // https://github.com/malsup/form/commit/588306aedba1de01388032d5f42a60159eea9228#commitcomment-2180219
    var fileInputs = jQryIter('input[type=file]:enabled:not([value=""])', this);

    var hasFileInputs = fileInputs.length > 0;
    var mp = 'multipart/form-data';
    var multipart = ($form.attr('enctype') == mp || $form.attr('encoding') == mp);

    var fileAPI = feature.fileapi && feature.formdata;
    log("fileAPI :" + fileAPI);
    var shouldUseFrame = (hasFileInputs || multipart) && !fileAPI;

    var jqxhr;

    // options.iframe allows user to force iframe mode
    // 06-NOV-09: now defaulting to iframe mode if file input is detected
    if (options.iframe !== false && (options.iframe || shouldUseFrame)) {
        // hack to fix Safari hang (thanks to Tim Molendijk for this)
        // see:  http://groups.google.com/group/jQryIter-dev/browse_thread/thread/36395b7ab510dd5d
        if (options.closeKeepAlive) {
            jQryIter.get(options.closeKeepAlive, function() {
                jqxhr = fileUploadIframe(a);
            });
        }
        else {
            jqxhr = fileUploadIframe(a);
        }
    }
    else if ((hasFileInputs || multipart) && fileAPI) {
        jqxhr = fileUploadXhr(a);
    }
    else {
        jqxhr = jQryIter.ajax(options);
    }

    $form.removeData('jqxhr').data('jqxhr', jqxhr);

    // clear element array
    for (var k=0; k < elements.length; k++)
        elements[k] = null;

    // fire 'notify' event
    this.trigger('form-submit-notify', [this, options]);
    return this;

    // utility fn for deep serialization
    function deepSerialize(extraData){
        var serialized = jQryIter.param(extraData, options.traditional).split('&');
        var len = serialized.length;
        var result = [];
        var i, part;
        for (i=0; i < len; i++) {
            // #252; undo param space replacement
            serialized[i] = serialized[i].replace(/\+/g,' ');
            part = serialized[i].split('=');
            // #278; use array instead of object storage, favoring array serializations
            result.push([decodeURIComponent(part[0]), decodeURIComponent(part[1])]);
        }
        return result;
    }

     // XMLHttpRequest Level 2 file uploads (big hat tip to francois2metz)
    function fileUploadXhr(a) {
        var formdata = new FormData();

        for (var i=0; i < a.length; i++) {
            formdata.append(a[i].name, a[i].value);
        }

        if (options.extraData) {
            var serializedData = deepSerialize(options.extraData);
            for (i=0; i < serializedData.length; i++)
                if (serializedData[i])
                    formdata.append(serializedData[i][0], serializedData[i][1]);
        }

        options.data = null;

        var s = jQryIter.extend(true, {}, jQryIter.ajaxSettings, options, {
            contentType: false,
            processData: false,
            cache: false,
            type: method || 'POST'
        });

        if (options.uploadProgress) {
            // workaround because jqXHR does not expose upload property
            s.xhr = function() {
                var xhr = jQryIter.ajaxSettings.xhr();
                if (xhr.upload) {
                    xhr.upload.addEventListener('progress', function(event) {
                        var percent = 0;
                        var position = event.loaded || event.position; /*event.position is deprecated*/
                        var total = event.total;
                        if (event.lengthComputable) {
                            percent = Math.ceil(position / total * 100);
                        }
                        options.uploadProgress(event, position, total, percent);
                    }, false);
                }
                return xhr;
            };
        }

        s.data = null;
            var beforeSend = s.beforeSend;
            s.beforeSend = function(xhr, o) {
                o.data = formdata;
                if(beforeSend)
                    beforeSend.call(this, xhr, o);
        };
        return jQryIter.ajax(s);
    }

    // private function for handling file uploads (hat tip to YAHOO!)
    function fileUploadIframe(a) {
        var form = $form[0], el, i, s, g, id, $io, io, xhr, sub, n, timedOut, timeoutHandle;
        var deferred = jQryIter.Deferred();

        // #341
        deferred.abort = function(status) {
            xhr.abort(status);
        };

        if (a) {
            // ensure that every serialized input is still enabled
            for (i=0; i < elements.length; i++) {
                el = jQryIter(elements[i]);
                if ( hasProp )
                    el.prop('disabled', false);
                else
                    el.removeAttr('disabled');
            }
        }

        s = jQryIter.extend(true, {}, jQryIter.ajaxSettings, options);
        s.context = s.context || s;
        id = 'jqFormIO' + (new Date().getTime());
        if (s.iframeTarget) {
            $io = jQryIter(s.iframeTarget);
            n = $io.attr2('name');
            if (!n)
                 $io.attr2('name', id);
            else
                id = n;
        }
        else {
            $io = jQryIter('<iframe name="' + id + '" src="'+ s.iframeSrc +'" />');
            $io.css({ position: 'absolute', top: '-1000px', left: '-1000px' });
        }
        io = $io[0];


        xhr = { // mock object
            aborted: 0,
            responseText: null,
            responseXML: null,
            status: 0,
            statusText: 'n/a',
            getAllResponseHeaders: function() {},
            getResponseHeader: function() {},
            setRequestHeader: function() {},
            abort: function(status) {
                var e = (status === 'timeout' ? 'timeout' : 'aborted');
                log('aborting upload... ' + e);
                this.aborted = 1;

                try { // #214, #257
                    if (io.contentWindow.document.execCommand) {
                        io.contentWindow.document.execCommand('Stop');
                    }
                }
                catch(ignore) {}

                $io.attr('src', s.iframeSrc); // abort op in progress
                xhr.error = e;
                if (s.error)
                    s.error.call(s.context, xhr, e, status);
                if (g)
                    jQryIter.event.trigger("ajaxError", [xhr, s, e]);
                if (s.complete)
                    s.complete.call(s.context, xhr, e);
            }
        };

        g = s.global;
        // trigger ajax global events so that activity/block indicators work like normal
        if (g && 0 === jQryIter.active++) {
            jQryIter.event.trigger("ajaxStart");
        }
        if (g) {
            jQryIter.event.trigger("ajaxSend", [xhr, s]);
        }

        if (s.beforeSend && s.beforeSend.call(s.context, xhr, s) === false) {
            if (s.global) {
                jQryIter.active--;
            }
            deferred.reject();
            return deferred;
        }
        if (xhr.aborted) {
            deferred.reject();
            return deferred;
        }

        // add submitting element to data if we know it
        sub = form.clk;
        if (sub) {
            n = sub.name;
            if (n && !sub.disabled) {
                s.extraData = s.extraData || {};
                s.extraData[n] = sub.value;
                if (sub.type == "image") {
                    s.extraData[n+'.x'] = form.clk_x;
                    s.extraData[n+'.y'] = form.clk_y;
                }
            }
        }

        var CLIENT_TIMEOUT_ABORT = 1;
        var SERVER_ABORT = 2;
                
        function getDoc(frame) {
            /* it looks like contentWindow or contentDocument do not
             * carry the protocol property in ie8, when running under ssl
             * frame.document is the only valid response document, since
             * the protocol is know but not on the other two objects. strange?
             * "Same origin policy" http://en.wikipedia.org/wiki/Same_origin_policy
             */
            
            var doc = null;
            
            // IE8 cascading access check
            try {
                if (frame.contentWindow) {
                    doc = frame.contentWindow.document;
                }
            } catch(err) {
                // IE8 access denied under ssl & missing protocol
                log('cannot get iframe.contentWindow document: ' + err);
            }

            if (doc) { // successful getting content
                return doc;
            }

            try { // simply checking may throw in ie8 under ssl or mismatched protocol
                doc = frame.contentDocument ? frame.contentDocument : frame.document;
            } catch(err) {
                // last attempt
                log('cannot get iframe.contentDocument: ' + err);
                doc = frame.document;
            }
            return doc;
        }

        // Rails CSRF hack (thanks to Yvan Barthelemy)
        var csrf_token = jQryIter('meta[name=csrf-token]').attr('content');
        var csrf_param = jQryIter('meta[name=csrf-param]').attr('content');
        if (csrf_param && csrf_token) {
            s.extraData = s.extraData || {};
            s.extraData[csrf_param] = csrf_token;
        }

        // take a breath so that pending repaints get some cpu time before the upload starts
        function doSubmit() {
            // make sure form attrs are set
            var t = $form.attr2('target'), a = $form.attr2('action');

            // update form attrs in IE friendly way
            form.setAttribute('target',id);
            if (!method) {
                form.setAttribute('method', 'POST');
            }
            if (a != s.url) {
                form.setAttribute('action', s.url);
            }

            // ie borks in some cases when setting encoding
            if (! s.skipEncodingOverride && (!method || /post/i.test(method))) {
                $form.attr({
                    encoding: 'multipart/form-data',
                    enctype:  'multipart/form-data'
                });
            }

            // support timout
            if (s.timeout) {
                timeoutHandle = setTimeout(function() { timedOut = true; cb(CLIENT_TIMEOUT_ABORT); }, s.timeout);
            }

            // look for server aborts
            function checkState() {
                try {
                    var state = getDoc(io).readyState;
                    log('state = ' + state);
                    if (state && state.toLowerCase() == 'uninitialized')
                        setTimeout(checkState,50);
                }
                catch(e) {
                    log('Server abort: ' , e, ' (', e.name, ')');
                    cb(SERVER_ABORT);
                    if (timeoutHandle)
                        clearTimeout(timeoutHandle);
                    timeoutHandle = undefined;
                }
            }

            // add "extra" data to form if provided in options
            var extraInputs = [];
            try {
                if (s.extraData) {
                    for (var n in s.extraData) {
                        if (s.extraData.hasOwnProperty(n)) {
                           // if using the jQryIter.param format that allows for multiple values with the same name
                           if(jQryIter.isPlainObject(s.extraData[n]) && s.extraData[n].hasOwnProperty('name') && s.extraData[n].hasOwnProperty('value')) {
                               extraInputs.push(
                               jQryIter('<input type="hidden" name="'+s.extraData[n].name+'">').val(s.extraData[n].value)
                                   .appendTo(form)[0]);
                           } else {
                               extraInputs.push(
                               jQryIter('<input type="hidden" name="'+n+'">').val(s.extraData[n])
                                   .appendTo(form)[0]);
                           }
                        }
                    }
                }

                if (!s.iframeTarget) {
                    // add iframe to doc and submit the form
                    $io.appendTo('body');
                    if (io.attachEvent)
                        io.attachEvent('onload', cb);
                    else
                        io.addEventListener('load', cb, false);
                }
                setTimeout(checkState,15);

                try {
                    form.submit();
                } catch(err) {
                    // just in case form has element with name/id of 'submit'
                    var submitFn = document.createElement('form').submit;
                    submitFn.apply(form);
                }
            }
            finally {
                // reset attrs and remove "extra" input elements
                form.setAttribute('action',a);
                if(t) {
                    form.setAttribute('target', t);
                } else {
                    $form.removeAttr('target');
                }
                $(extraInputs).remove();
            }
        }

        if (s.forceSync) {
            doSubmit();
        }
        else {
            setTimeout(doSubmit, 10); // this lets dom updates render
        }

        var data, doc, domCheckCount = 50, callbackProcessed;

        function cb(e) {
            if (xhr.aborted || callbackProcessed) {
                return;
            }
            
            doc = getDoc(io);
            if(!doc) {
                log('cannot access response document');
                e = SERVER_ABORT;
            }
            if (e === CLIENT_TIMEOUT_ABORT && xhr) {
                xhr.abort('timeout');
                deferred.reject(xhr, 'timeout');
                return;
            }
            else if (e == SERVER_ABORT && xhr) {
                xhr.abort('server abort');
                deferred.reject(xhr, 'error', 'server abort');
                return;
            }

            if (!doc || doc.location.href == s.iframeSrc) {
                // response not received yet
                if (!timedOut)
                    return;
            }
            if (io.detachEvent)
                io.detachEvent('onload', cb);
            else
                io.removeEventListener('load', cb, false);

            var status = 'success', errMsg;
            try {
                if (timedOut) {
                    throw 'timeout';
                }

                var isXml = s.dataType == 'xml' || doc.XMLDocument || jQryIter.isXMLDoc(doc);
                log('isXml='+isXml);
                if (!isXml && window.opera && (doc.body === null || !doc.body.innerHTML)) {
                    if (--domCheckCount) {
                        // in some browsers (Opera) the iframe DOM is not always traversable when
                        // the onload callback fires, so we loop a bit to accommodate
                        log('requeing onLoad callback, DOM not available');
                        setTimeout(cb, 250);
                        return;
                    }
                    // let this fall through because server response could be an empty document
                    //log('Could not access iframe DOM after mutiple tries.');
                    //throw 'DOMException: not available';
                }

                //log('response detected');
                var docRoot = doc.body ? doc.body : doc.documentElement;
                xhr.responseText = docRoot ? docRoot.innerHTML : null;
                xhr.responseXML = doc.XMLDocument ? doc.XMLDocument : doc;
                if (isXml)
                    s.dataType = 'xml';
                xhr.getResponseHeader = function(header){
                    var headers = {'content-type': s.dataType};
                    return headers[header.toLowerCase()];
                };
                // support for XHR 'status' & 'statusText' emulation :
                if (docRoot) {
                    xhr.status = Number( docRoot.getAttribute('status') ) || xhr.status;
                    xhr.statusText = docRoot.getAttribute('statusText') || xhr.statusText;
                }

                var dt = (s.dataType || '').toLowerCase();
                var scr = /(json|script|text)/.test(dt);
                if (scr || s.textarea) {
                    // see if user embedded response in textarea
                    var ta = doc.getElementsByTagName('textarea')[0];
                    if (ta) {
                        xhr.responseText = ta.value;
                        // support for XHR 'status' & 'statusText' emulation :
                        xhr.status = Number( ta.getAttribute('status') ) || xhr.status;
                        xhr.statusText = ta.getAttribute('statusText') || xhr.statusText;
                    }
                    else if (scr) {
                        // account for browsers injecting pre around json response
                        var pre = doc.getElementsByTagName('pre')[0];
                        var b = doc.getElementsByTagName('body')[0];
                        if (pre) {
                            xhr.responseText = pre.textContent ? pre.textContent : pre.innerText;
                        }
                        else if (b) {
                            xhr.responseText = b.textContent ? b.textContent : b.innerText;
                        }
                    }
                }
                else if (dt == 'xml' && !xhr.responseXML && xhr.responseText) {
                    xhr.responseXML = toXml(xhr.responseText);
                }

                try {
                    data = httpData(xhr, dt, s);
                }
                catch (err) {
                    status = 'parsererror';
                    xhr.error = errMsg = (err || status);
                }
            }
            catch (err) {
                log('error caught: ',err);
                status = 'error';
                xhr.error = errMsg = (err || status);
            }

            if (xhr.aborted) {
                log('upload aborted');
                status = null;
            }

            if (xhr.status) { // we've set xhr.status
                status = (xhr.status >= 200 && xhr.status < 300 || xhr.status === 304) ? 'success' : 'error';
            }

            // ordering of these callbacks/triggers is odd, but that's how jQryIter.ajax does it
            if (status === 'success') {
                if (s.success)
                    s.success.call(s.context, data, 'success', xhr);
                deferred.resolve(xhr.responseText, 'success', xhr);
                if (g)
                    jQryIter.event.trigger("ajaxSuccess", [xhr, s]);
            }
            else if (status) {
                if (errMsg === undefined)
                    errMsg = xhr.statusText;
                if (s.error)
                    s.error.call(s.context, xhr, status, errMsg);
                deferred.reject(xhr, 'error', errMsg);
                if (g)
                    jQryIter.event.trigger("ajaxError", [xhr, s, errMsg]);
            }

            if (g)
                jQryIter.event.trigger("ajaxComplete", [xhr, s]);

            if (g && ! --jQryIter.active) {
                jQryIter.event.trigger("ajaxStop");
            }

            if (s.complete)
                s.complete.call(s.context, xhr, status);

            callbackProcessed = true;
            if (s.timeout)
                clearTimeout(timeoutHandle);

            // clean up
            setTimeout(function() {
                if (!s.iframeTarget)
                    $io.remove();
                xhr.responseXML = null;
            }, 100);
        }

        var toXml = jQryIter.parseXML || function(s, doc) { // use parseXML if available (jQryIter 1.5+)
            if (window.ActiveXObject) {
                doc = new ActiveXObject('Microsoft.XMLDOM');
                doc.async = 'false';
                doc.loadXML(s);
            }
            else {
                doc = (new DOMParser()).parseFromString(s, 'text/xml');
            }
            return (doc && doc.documentElement && doc.documentElement.nodeName != 'parsererror') ? doc : null;
        };
        var parseJSON = jQryIter.parseJSON || function(s) {
            /*jslint evil:true */
            return window['eval']('(' + s + ')');
        };

        var httpData = function( xhr, type, s ) { // mostly lifted from jq1.4.4

            var ct = xhr.getResponseHeader('content-type') || '',
                xml = type === 'xml' || !type && ct.indexOf('xml') >= 0,
                data = xml ? xhr.responseXML : xhr.responseText;

            if (xml && data.documentElement.nodeName === 'parsererror') {
                if (jQryIter.error)
                    jQryIter.error('parsererror');
            }
            if (s && s.dataFilter) {
                data = s.dataFilter(data, type);
            }
            if (typeof data === 'string') {
                if (type === 'json' || !type && ct.indexOf('json') >= 0) {
                    data = parseJSON(data);
                } else if (type === "script" || !type && ct.indexOf("javascript") >= 0) {
                    jQryIter.globalEval(data);
                }
            }
            return data;
        };

        return deferred;
    }
};

/**
 * ajaxForm() provides a mechanism for fully automating form submission.
 *
 * The advantages of using this method instead of ajaxSubmit() are:
 *
 * 1: This method will include coordinates for <input type="image" /> elements (if the element
 *    is used to submit the form).
 * 2. This method will include the submit element's name/value data (for the element that was
 *    used to submit the form).
 * 3. This method binds the submit() method to the form for you.
 *
 * The options argument for ajaxForm works exactly as it does for ajaxSubmit.  ajaxForm merely
 * passes the options argument along after properly binding events for submit elements and
 * the form itself.
 */
jQryIter.fn.ajaxForm = function(options) {
    options = options || {};
    options.delegation = options.delegation && jQryIter.isFunction(jQryIter.fn.on);

    // in jQryIter 1.3+ we can fix mistakes with the ready state
    if (!options.delegation && this.length === 0) {
        var o = { s: this.selector, c: this.context };
        if (!jQryIter.isReady && o.s) {
            log('DOM not ready, queuing ajaxForm');
            jQryIter(function() {
                jQryIter(o.s,o.c).ajaxForm(options);
            });
            return this;
        }
        // is your DOM ready?  http://docs.jQryIter.com/Tutorials:Introducing_$(document).ready()
        log('terminating; zero elements found by selector' + (jQryIter.isReady ? '' : ' (DOM not ready)'));
        return this;
    }

    if ( options.delegation ) {
        jQryIter(document)
            .off('submit.form-plugin', this.selector, doAjaxSubmit)
            .off('click.form-plugin', this.selector, captureSubmittingElement)
            .on('submit.form-plugin', this.selector, options, doAjaxSubmit)
            .on('click.form-plugin', this.selector, options, captureSubmittingElement);
        return this;
    }

    return this.ajaxFormUnbind()
        .bind('submit.form-plugin', options, doAjaxSubmit)
        .bind('click.form-plugin', options, captureSubmittingElement);
};

// private event handlers
function doAjaxSubmit(e) {
    /*jshint validthis:true */
    var options = e.data;
    if (!e.isDefaultPrevented()) { // if event has been canceled, don't proceed
        e.preventDefault();
        jQryIter(this).ajaxSubmit(options);
    }
}

function captureSubmittingElement(e) {
    /*jshint validthis:true */
    var target = e.target;
    var $el = jQryIter(target);
    if (!($el.is("[type=submit],[type=image]"))) {
        // is this a child element of the submit el?  (ex: a span within a button)
        var t = $el.closest('[type=submit]');
        if (t.length === 0) {
            return;
        }
        target = t[0];
    }
    var form = this;
    form.clk = target;
    if (target.type == 'image') {
        if (e.offsetX !== undefined) {
            form.clk_x = e.offsetX;
            form.clk_y = e.offsetY;
        } else if (typeof jQryIter.fn.offset == 'function') {
            var offset = $el.offset();
            form.clk_x = e.pageX - offset.left;
            form.clk_y = e.pageY - offset.top;
        } else {
            form.clk_x = e.pageX - target.offsetLeft;
            form.clk_y = e.pageY - target.offsetTop;
        }
    }
    // clear form vars
    setTimeout(function() { form.clk = form.clk_x = form.clk_y = null; }, 100);
}


// ajaxFormUnbind unbinds the event handlers that were bound by ajaxForm
jQryIter.fn.ajaxFormUnbind = function() {
    return this.unbind('submit.form-plugin click.form-plugin');
};

/**
 * formToArray() gathers form element data into an array of objects that can
 * be passed to any of the following ajax functions: jQryIter.get, jQryIter.post, or load.
 * Each object in the array has both a 'name' and 'value' property.  An example of
 * an array for a simple login form might be:
 *
 * [ { name: 'username', value: 'jresig' }, { name: 'password', value: 'secret' } ]
 *
 * It is this array that is passed to pre-submit callback functions provided to the
 * ajaxSubmit() and ajaxForm() methods.
 */
jQryIter.fn.formToArray = function(semantic, elements) {
    var a = [];
    if (this.length === 0) {
        return a;
    }

    var form = this[0];
    var els = semantic ? form.getElementsByTagName('*') : form.elements;
    if (!els) {
        return a;
    }

    var i,j,n,v,el,max,jmax;
    for(i=0, max=els.length; i < max; i++) {
        el = els[i];
        n = el.name;
        if (!n || el.disabled) {
            continue;
        }

        if (semantic && form.clk && el.type == "image") {
            // handle image inputs on the fly when semantic == true
            if(form.clk == el) {
                a.push({name: n, value: jQryIter(el).val(), type: el.type });
                a.push({name: n+'.x', value: form.clk_x}, {name: n+'.y', value: form.clk_y});
            }
            continue;
        }

        v = jQryIter.fieldValue(el, true);
        if (v && v.constructor == Array) {
            if (elements)
                elements.push(el);
            for(j=0, jmax=v.length; j < jmax; j++) {
                a.push({name: n, value: v[j]});
            }
        }
        else if (feature.fileapi && el.type == 'file') {
            if (elements)
                elements.push(el);
            var files = el.files;
            if (files.length) {
                for (j=0; j < files.length; j++) {
                    a.push({name: n, value: files[j], type: el.type});
                }
            }
            else {
                // #180
                a.push({ name: n, value: '', type: el.type });
            }
        }
        else if (v !== null && typeof v != 'undefined') {
            if (elements)
                elements.push(el);
            a.push({name: n, value: v, type: el.type, required: el.required});
        }
    }

    if (!semantic && form.clk) {
        // input type=='image' are not found in elements array! handle it here
        var $input = jQryIter(form.clk), input = $input[0];
        n = input.name;
        if (n && !input.disabled && input.type == 'image') {
            a.push({name: n, value: $input.val()});
            a.push({name: n+'.x', value: form.clk_x}, {name: n+'.y', value: form.clk_y});
        }
    }
    return a;
};

/**
 * Serializes form data into a 'submittable' string. This method will return a string
 * in the format: name1=value1&amp;name2=value2
 */
jQryIter.fn.formSerialize = function(semantic) {
    //hand off to jQryIter.param for proper encoding
    return jQryIter.param(this.formToArray(semantic));
};

/**
 * Serializes all field elements in the jQryIter object into a query string.
 * This method will return a string in the format: name1=value1&amp;name2=value2
 */
jQryIter.fn.fieldSerialize = function(successful) {
    var a = [];
    this.each(function() {
        var n = this.name;
        if (!n) {
            return;
        }
        var v = jQryIter.fieldValue(this, successful);
        if (v && v.constructor == Array) {
            for (var i=0,max=v.length; i < max; i++) {
                a.push({name: n, value: v[i]});
            }
        }
        else if (v !== null && typeof v != 'undefined') {
            a.push({name: this.name, value: v});
        }
    });
    //hand off to jQryIter.param for proper encoding
    return jQryIter.param(a);
};

/**
 * Returns the value(s) of the element in the matched set.  For example, consider the following form:
 *
 *  <form><fieldset>
 *      <input name="A" type="text" />
 *      <input name="A" type="text" />
 *      <input name="B" type="checkbox" value="B1" />
 *      <input name="B" type="checkbox" value="B2"/>
 *      <input name="C" type="radio" value="C1" />
 *      <input name="C" type="radio" value="C2" />
 *  </fieldset></form>
 *
 *  var v = jQryIter('input[type=text]').fieldValue();
 *  // if no values are entered into the text inputs
 *  v == ['','']
 *  // if values entered into the text inputs are 'foo' and 'bar'
 *  v == ['foo','bar']
 *
 *  var v = jQryIter('input[type=checkbox]').fieldValue();
 *  // if neither checkbox is checked
 *  v === undefined
 *  // if both checkboxes are checked
 *  v == ['B1', 'B2']
 *
 *  var v = jQryIter('input[type=radio]').fieldValue();
 *  // if neither radio is checked
 *  v === undefined
 *  // if first radio is checked
 *  v == ['C1']
 *
 * The successful argument controls whether or not the field element must be 'successful'
 * (per http://www.w3.org/TR/html4/interact/forms.html#successful-controls).
 * The default value of the successful argument is true.  If this value is false the value(s)
 * for each element is returned.
 *
 * Note: This method *always* returns an array.  If no valid value can be determined the
 *    array will be empty, otherwise it will contain one or more values.
 */
jQryIter.fn.fieldValue = function(successful) {
    for (var val=[], i=0, max=this.length; i < max; i++) {
        var el = this[i];
        var v = jQryIter.fieldValue(el, successful);
        if (v === null || typeof v == 'undefined' || (v.constructor == Array && !v.length)) {
            continue;
        }
        if (v.constructor == Array)
            jQryIter.merge(val, v);
        else
            val.push(v);
    }
    return val;
};

/**
 * Returns the value of the field element.
 */
jQryIter.fieldValue = function(el, successful) {
    var n = el.name, t = el.type, tag = el.tagName.toLowerCase();
    if (successful === undefined) {
        successful = true;
    }

    if (successful && (!n || el.disabled || t == 'reset' || t == 'button' ||
        (t == 'checkbox' || t == 'radio') && !el.checked ||
        (t == 'submit' || t == 'image') && el.form && el.form.clk != el ||
        tag == 'select' && el.selectedIndex == -1)) {
            return null;
    }

    if (tag == 'select') {
        var index = el.selectedIndex;
        if (index < 0) {
            return null;
        }
        var a = [], ops = el.options;
        var one = (t == 'select-one');
        var max = (one ? index+1 : ops.length);
        for(var i=(one ? index : 0); i < max; i++) {
            var op = ops[i];
            if (op.selected) {
                var v = op.value;
                if (!v) { // extra pain for IE...
                    v = (op.attributes && op.attributes['value'] && !(op.attributes['value'].specified)) ? op.text : op.value;
                }
                if (one) {
                    return v;
                }
                a.push(v);
            }
        }
        return a;
    }
    return jQryIter(el).val();
};

/**
 * Clears the form data.  Takes the following actions on the form's input fields:
 *  - input text fields will have their 'value' property set to the empty string
 *  - select elements will have their 'selectedIndex' property set to -1
 *  - checkbox and radio inputs will have their 'checked' property set to false
 *  - inputs of type submit, button, reset, and hidden will *not* be effected
 *  - button elements will *not* be effected
 */
jQryIter.fn.clearForm = function(includeHidden) {
    return this.each(function() {
        jQryIter('input,select,textarea', this).clearFields(includeHidden);
    });
};

/**
 * Clears the selected form elements.
 */
jQryIter.fn.clearFields = jQryIter.fn.clearInputs = function(includeHidden) {
    var re = /^(?:color|date|datetime|email|month|number|password|range|search|tel|text|time|url|week)$/i; // 'hidden' is not in this list
    return this.each(function() {
        var t = this.type, tag = this.tagName.toLowerCase();
        if (re.test(t) || tag == 'textarea') {
            this.value = '';
        }
        else if (t == 'checkbox' || t == 'radio') {
            this.checked = false;
        }
        else if (tag == 'select') {
            this.selectedIndex = -1;
        }
		else if (t == "file") {
			if (/MSIE/.test(navigator.userAgent)) {
				jQryIter(this).replaceWith(jQryIter(this).clone(true));
			} else {
				jQryIter(this).val('');
			}
		}
        else if (includeHidden) {
            // includeHidden can be the value true, or it can be a selector string
            // indicating a special test; for example:
            //  jQryIter('#myForm').clearForm('.special:hidden')
            // the above would clean hidden inputs that have the class of 'special'
            if ( (includeHidden === true && /hidden/.test(t)) ||
                 (typeof includeHidden == 'string' && jQryIter(this).is(includeHidden)) )
                this.value = '';
        }
    });
};

/**
 * Resets the form data.  Causes all form elements to be reset to their original value.
 */
jQryIter.fn.resetForm = function() {
    return this.each(function() {
        // guard against an input with the name of 'reset'
        // note that IE reports the reset function as an 'object'
        if (typeof this.reset == 'function' || (typeof this.reset == 'object' && !this.reset.nodeType)) {
            this.reset();
        }
    });
};

/**
 * Enables or disables any matching elements.
 */
jQryIter.fn.enable = function(b) {
    if (b === undefined) {
        b = true;
    }
    return this.each(function() {
        this.disabled = !b;
    });
};

/**
 * Checks/unchecks any matching checkboxes or radio buttons and
 * selects/deselects and matching option elements.
 */
jQryIter.fn.selected = function(select) {
    if (select === undefined) {
        select = true;
    }
    return this.each(function() {
        var t = this.type;
        if (t == 'checkbox' || t == 'radio') {
            this.checked = select;
        }
        else if (this.tagName.toLowerCase() == 'option') {
            var $sel = jQryIter(this).parent('select');
            if (select && $sel[0] && $sel[0].type == 'select-one') {
                // deselect all other options
                $sel.find('option').selected(false);
            }
            this.selected = select;
        }
    });
};

// expose debug var
jQryIter.fn.ajaxSubmit.debug = false;

// helper fn for console logging
function log() {
    if (!jQryIter.fn.ajaxSubmit.debug)
        return;
    var msg = '[jQryIter.form] ' + Array.prototype.join.call(arguments,'');
    if (window.console && window.console.log) {
        window.console.log(msg);
    }
    else if (window.opera && window.opera.postError) {
        window.opera.postError(msg);
    }
}

})( (typeof(jQryIter) != 'undefined') ? jQryIter : window.Zepto );




/* PAGINATION +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


(function (jQryIter) {
	"use strict";
	
	jQryIter.jqPagination = function (el, options) {
	
		// To avoid scope issues, use 'base' instead of 'this'
		// to reference this class from internal events and functions.
	
		var base = this;

		// Access to jQryIter and DOM versions of element
		base.$el = jQryIter(el);
		base.el = el;
		
		// get input jQryIter object
		base.$input = base.$el.find('input');

		// Add a reverse reference to the DOM object
		base.$el.data("jqPagination", base);

		base.init = function () {

			base.options = jQryIter.extend({}, jQryIter.jqPagination.defaultOptions, options);
			
			// if the user hasn't provided a max page number in the options try and find
			// the data attribute for it, if that cannot be found, use one as a max page number
			
			if (base.options.max_page === null) {
			
				if (base.$input.data('max-page') !== undefined) {
					base.options.max_page = base.$input.data('max-page');
				} else {
					base.options.max_page = 1;
				}
				
			}
			
			// if the current-page data attribute is specified this takes priority
			// over the options passed in, so long as it's a number
			
			if (base.$input.data('current-page') !== undefined && base.isNumber(base.$input.data('current-page'))) {
				base.options.current_page = base.$input.data('current-page');
			}
			
			// remove the readonly attribute as JavaScript must be working by now ;-)
			base.$input.removeAttr('readonly');
			
			// set the initial input value
			// pass true to prevent paged callback form being fired
			
			base.updateInput(true);

			
			 //***************
			// BIND EVENTS
			
			base.$input.on('focus.jqPagination mouseup.jqPagination', function (event) {

				// if event === focus, select all text...
				if (event.type === 'focus') {

					var current_page	= parseInt(base.options.current_page, 10);

					jQryIter(this).val(current_page).select();

				}
			
				// if event === mouse up, return false. Fixes Chrome bug
				if (event.type === 'mouseup') {
					return false;
				}
				
			});
			
			base.$input.on('blur.jqPagination keydown.jqPagination', function (event) {
				
				var $self			= jQryIter(this),
					current_page	= parseInt(base.options.current_page, 10);
				
				// if the user hits escape revert the input back to the original value
				if (event.keyCode === 27) {
					$self.val(current_page);
					$self.blur();
				}
				
				// if the user hits enter, trigger blur event but DO NOT set the page value
				if (event.keyCode === 13) {
					$self.blur();
				}

				// only set the page is the event is focusout.. aka blur
				if (event.type === 'blur') {
					base.setPage($self.val());
				}
				
			});
			
			base.$el.on('click.jqPagination', 'a', function (event) {
			
				var $self = jQryIter(this);

				// we don't want to do anything if we've clicked a disabled link
				// return false so we stop normal link action btu also drop out of this event
				
				if ($self.hasClass('disabled')) {
					return false;
				}

				// for mac + windows (read: other), maintain the cmd + ctrl click for new tab
				if (!event.metaKey && !event.ctrlKey) {
					event.preventDefault();
					base.setPage($self.data('action'));
				}
				
			});
			
		};
		
		base.setPage = function (page, prevent_paged) {
			
			// return current_page value if getting instead of setting
			if (page === undefined) {
				return base.options.current_page;
			}
		
			var current_page	= parseInt(base.options.current_page, 10),
				max_page		= parseInt(base.options.max_page, 10);
							
			if (isNaN(parseInt(page, 10))) {
				
				switch (page) {
				
					case 'first':
						page = 1;
						break;
						
					case 'prev':
					case 'previous':
						page = current_page - 1;
						break;
						
					case 'next':
						page = current_page + 1;
						break;
						
					case 'last':
						page = max_page;
						break;
						
				}
				
			}
			
			page = parseInt(page, 10);
			
			// reject any invalid page requests
			if (isNaN(page) || page < 1 || page > max_page) {

				// update the input element
				base.setInputValue(current_page);
				
				return false;
				
			}
			
			// update current page options
			base.options.current_page = page;
			base.$input.data('current-page', page);
			
			// update the input element
			base.updateInput( prevent_paged );
			
		};
		
		base.setMaxPage = function (max_page, prevent_paged) {
			
			// return the max_page value if getting instead of setting
			if (max_page === undefined) {
				return base.options.max_page;
			}

			// ignore if max_page is not a number
			if (!base.isNumber(max_page)) {
				console.error('jqPagination: max_page is not a number');
				return false;
			}
			
			// ignore if max_page is less than the current_page
			if (max_page < base.options.current_page) {
				console.error('jqPagination: max_page lower than current_page');
				return false;
			}
			
			// set max_page options
			base.options.max_page = max_page;
			base.$input.data('max-page', max_page);
				
			// update the input element
			base.updateInput( prevent_paged );
			
		};
		
		// ATTN this isn't really the correct name is it?
		base.updateInput = function (prevent_paged) {
			
			var current_page = parseInt(base.options.current_page, 10);
							
			// set the input value
			base.setInputValue(current_page);
			
			// set the link href attributes
			base.setLinks(current_page);
			
			// we may want to prevent the paged callback from being fired
			if (prevent_paged !== true) {

				// fire the callback function with the current page
				base.options.paged(current_page);
			
			}
			
		};
		
		base.setInputValue = function (page) {
		
			var page_string	= base.options.page_string,
				max_page	= base.options.max_page;
	
			// this looks horrible :-(
			page_string = page_string
				.replace("{current_page}", page)
				.replace("{max_page}", max_page);
			
			base.$input.val(page_string);
		
		};
		
		base.isNumber = function(n) {
			return !isNaN(parseFloat(n)) && isFinite(n);
		};
		
		base.setLinks = function (page) {
			
			var link_string		= base.options.link_string,
				current_page	= parseInt(base.options.current_page, 10),
				max_page		= parseInt(base.options.max_page, 10);
			
			if (link_string !== '') {
				
				// set initial page numbers + make sure the page numbers aren't out of range
					
				var previous = current_page - 1;
				if (previous < 1) {
					previous = 1;
				}
				
				var next = current_page + 1;
				if (next > max_page) {
					next = max_page;
				}
				
				// apply each page number to the link string, set it back to the element href attribute
				base.$el.find('a.first').attr('href', link_string.replace('{page_number}', '1'));
				base.$el.find('a.prev, a.previous').attr('href', link_string.replace('{page_number}', previous));
				base.$el.find('a.next').attr('href', link_string.replace('{page_number}', next));
				base.$el.find('a.last').attr('href', link_string.replace('{page_number}', max_page));
				
			}

			// set disable class on appropriate links
			base.$el.find('a').removeClass('disabled');

			if (current_page === max_page) {
				base.$el.find('.next, .last').addClass('disabled');
			}

			if (current_page === 1) {
				base.$el.find('.previous, .first').addClass('disabled');
			}

		};
		
		base.callMethod = function (method, key, value) {

			switch (method.toLowerCase()) {

				case 'option':

					// set default object to trigger the paged event (legacy opperation)
					var options = {'trigger': true},
					result = false;

					// if the key passed in is an object
					if(jQryIter.isPlainObject(key) && !value){
						jQryIter.extend(options, key)
					}
					else{ // make the key value pair part of the default object
						options[key] = value;
					}

					var prevent_paged = (options.trigger === false);

					// if max_page property is set call setMaxPage
					if(options.max_page !== undefined){
						result = base.setMaxPage(options.max_page, prevent_paged);
					}

					// if current_page property is set call setPage
					if(options.current_page !== undefined){
						result = base.setPage(options.current_page, prevent_paged);
					}

					// if we've not got a result fire an error and return false
					if( result === false ) console.error('jqPagination: cannot get / set option ' + key);
					return result;
					
					break;

				case 'destroy':

					base.$el
						.off('.jqPagination')
						.find('*')
							.off('.jqPagination');

					break;

				default:

					// the function name must not exist
					console.error('jqPagination: method "' + method + '" does not exist');
					return false;

			}

		};

		// Run initializer
		base.init();
		
	};

	jQryIter.jqPagination.defaultOptions = {
		current_page	: 1,
		link_string		: '',
		max_page		: null,
		page_string		: 'Page {current_page} of {max_page}',
		paged			: function () {}
	};

	jQryIter.fn.jqPagination = function () {

		// get any function parameters
		var self = this,
			args = Array.prototype.slice.call(arguments),
			result = false;

		// if the first argument is a string call the desired function
		// note: we can only do this to a single element, and not a collection of elements

		if (typeof args[0] === 'string') {

			// if we're dealing with multiple elements, set for all
			jQryIter.each(self, function(){
				var $plugin = jQryIter(this).data('jqPagination');

				result = $plugin.callMethod(args[0], args[1], args[2]);
			});

			return result;
		}

		// if we're not dealing with a method, initialise plugin
		self.each(function () {
			(new jQryIter.jqPagination(this, args[0]));
		});
		
	};

})(jQryIter);

// polyfill, provide a fallback if the console doesn't exist
if (!console) {

	var console	= {},
		func	= function () { return false; };

	console.log		= func;
	console.info	= func;
	console.warn	= func;
	console.error	= func;

}

/* PAGINATION +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/



/* DATEPICKER +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
function dame_fmax (v_max) {
	if(v_max.substring(0, 1) != "+" && v_max.substring(0, 1) != "-" && v_max.length == 8) {
		// fecha obsoluta
		var y = v_max.substring(0, 4);
		var m = v_max.substring(4, 6);
		var d = v_max.substring(6, 8);
		var f_max = new Date(y + "/" + m + "/" + d)
	}
	else{
		var f_max = calcula_date(v_max);
	}
	
	return f_max;
}

function dame_fmin (v_min) {
	if(v_min.substring(0, 1) != "+" && v_min.substring(0, 1) != "-" && v_min.length == 8) {
		var y = v_min.substring(0, 4);
		var m = v_min.substring(4, 6);
		var d = v_min.substring(6, 8);
		var f_min = new Date(y + "/" + m + "/" + d)
	}
	else{
		var f_min = calcula_date(v_min);
	}
	return f_min;
	
}

function dame_rango (v_max, v_min) {
	var y_min = "1950";
	var y_max = "2050";
	if(v_max != "") y_max = v_max.getFullYear();	
	if(v_min != "") y_min = v_min.getFullYear();
	var v_range = y_min + ":" + y_max;
	return v_range;
}

function calcula_date (valor) {
	if (valor != ""){
		var tipo = valor.substring(valor.length -1, valor.length);
		var oper = valor.substring(0, 1);
		var n_val = parseInt(valor.substring(1, valor.length-1 ));
		var _fecha = new Date();
		if(tipo == "d" && oper == "+") _fecha.setDate(_fecha.getDate() + n_val);
		if(tipo == "d" && oper == "-") _fecha.setDate(_fecha.getDate() - n_val);
		if(tipo == "w" && oper == "+") _fecha.setMonth(_fecha.getDate() + (n_val * 7));
		if(tipo == "w" && oper == "-") _fecha.setMonth(_fecha.getDate() - (n_val * 7));
		if(tipo == "m" && oper == "+") _fecha.setMonth(_fecha.getMonth() + n_val);
		if(tipo == "m" && oper == "-") _fecha.setMonth(_fecha.getMonth() - n_val);
		if(tipo == "y" && oper == "+") _fecha.setFullYear(_fecha.getFullYear() + n_val);
		if(tipo == "y" && oper == "-") _fecha.setFullYear(_fecha.getFullYear() - n_val);
		return _fecha;
	}
	else{
		return "";
	}	
}
/* DATEPICKER +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
