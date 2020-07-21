Liferay.Portlet = {
	list: [],

	isStatic: function(portletId) {
		var instance = this;

		var id = Liferay.Util.getPortletId(portletId.id || portletId);

		return (id in instance._staticPortlets);
	},

	_defCloseFn: function(event) {
		var instance = this;

		var A = AUI();

		event.portlet.remove(true);

		A.io.request(
			themeDisplay.getPathMain() + '/portal/update_layout',
			{
				data: {
					cmd: 'delete',
					doAsUserId: event.doAsUserId,
					p_l_id: event.plid,
					p_p_id: event.portletId
				}
			}
		);
	},

	_staticPortlets: {}
};

Liferay.provide(
	Liferay.Portlet,
	'add',
	function(options) {
		var instance = this;

		var A = AUI();

		Liferay.fire('initLayout');

		var plid = options.plid || themeDisplay.getPlid();
		var portletId = options.portletId;
		var doAsUserId = options.doAsUserId || themeDisplay.getDoAsUserIdEncoded();

		var placeHolder = options.placeHolder;

		if (!placeHolder) {
			placeHolder = A.Node.create('<div class="loading-animation" />');
		}
		else {
			placeHolder = A.one(placeHolder);
		}

		var positionOptions = options.positionOptions;
		var beforePortletLoaded = options.beforePortletLoaded;
		var onComplete = options.onComplete;

		var container = A.one(Liferay.Layout.options.dropContainer);

		if (!container) {
			return;
		}

		var portletPosition = 0;
		var currentColumnId = 'column-1';

		if (options.placeHolder) {
			var column = placeHolder.get('parentNode');

			placeHolder.addClass('portlet-boundary');

			portletPosition = column.all('.portlet-boundary').indexOf(placeHolder);

			currentColumnId = Liferay.Util.getColumnId(column.attr('id'));
		}

		var url = themeDisplay.getPathMain() + '/portal/update_layout';

		var data = {
			cmd: 'add',
			dataType: 'json',
			doAsUserId: doAsUserId,
			p_l_id: plid,
			p_p_col_id: currentColumnId,
			p_p_col_pos: portletPosition,
			p_p_id: portletId,
			p_p_isolated: true
		};

		var firstPortlet = container.one('.portlet-boundary');
		var hasStaticPortlet = (firstPortlet && firstPortlet.isStatic);

		if (!options.placeHolder && !options.plid) {
			if (!hasStaticPortlet) {
				container.prepend(placeHolder);
			}
			else {
				firstPortlet.placeAfter(placeHolder);
			}
		}

		if (themeDisplay.isFreeformLayout()) {
			container.prepend(placeHolder);
		}

		data.currentURL = Liferay.currentURL;

		return instance.addHTML(
			{
				beforePortletLoaded: beforePortletLoaded,
				data: data,
				onComplete: onComplete,
				placeHolder: placeHolder,
				url: url
			}
		);
	},
	['aui-base']
);

Liferay.provide(
	Liferay.Portlet,
	'addHTML',
	function(options) {
		var instance = this;

		var A = AUI();

		var portletBoundary = null;

		var beforePortletLoaded = options.beforePortletLoaded;
		var data = options.data;
		var dataType = 'html';
		var onComplete = options.onComplete;
		var placeHolder = options.placeHolder;
		var url = options.url;

		if (data && data.dataType) {
			dataType = data.dataType;
		}

		var addPortletReturn = function(html) {
			var container = placeHolder.get('parentNode');

			var portletBound = A.Node.create('<div></div>');

			portletBound.plug(A.Plugin.ParseContent);

			portletBound.setContent(html);
			portletBound = portletBound.get('firstChild');

			var id = portletBound.attr('id');

			var portletId = Liferay.Util.getPortletId(id);

			portletBound.portletId = portletId;

			placeHolder.hide();
			placeHolder.placeAfter(portletBound);

			placeHolder.remove();

			instance.refreshLayout(portletBound);

			Liferay.Util.addInputType(portletBound);

			/** Comentado el 4 de julio de 2013
			 * Si la url tiene hash (como con la paginación) y se modifica un portlet, borra el contenido del hash 
			 * y añade p_portletId a la url. 
			 * */
//			if (window.location.hash) {
//				window.location.hash = 'p_' + portletId;
//			}

			portletBoundary = portletBound;

			if (Liferay.Layout) {
				Liferay.Layout.updateCurrentPortletInfo(portletBoundary);

				if (container) {
					Liferay.Layout.syncEmptyColumnClassUI(container);
				}
			}

			if (onComplete) {
				onComplete(portletBoundary, portletId);
			}

			return portletId;
		};

		if (beforePortletLoaded) {
			beforePortletLoaded(placeHolder);
		}

		A.io.request(
			url,
			{
				data: data,
				dataType: dataType,
				on: {
					success: function(event, id, obj) {
						var instance = this;

						var response = this.get('responseData');

						if (dataType == 'html') {
							addPortletReturn(response);
						}
						else {
							if (response.refresh) {
								location.reload();
							}
							else {
								addPortletReturn(response.portletHTML);
							}
						}
					}
				}
			}
		);
	},
	['aui-io-request', 'aui-parse-content']
);

Liferay.provide(
	Liferay.Portlet,
	'close',
	function(portlet, skipConfirm, options) {

		var A = AUI();

		portlet = A.one(portlet);

		if (portlet && (skipConfirm || confirm(Liferay.Language.get('are-you-sure-you-want-to-remove-this-component')))) {
			options = options || {};

			options.plid = options.plid || themeDisplay.getPlid();
			options.doAsUserId = options.doAsUserId || themeDisplay.getDoAsUserIdEncoded();
			options.portlet = portlet;
			options.portletId = portlet.portletId;
			
			unloadIfExistsSWF();

			Liferay.fire('closePortlet', options);
		}
		else {
			self.focus();
		}
	},
	['aui-io-request']
);

Liferay.provide(
	Liferay.Portlet,
	'minimize',
	function(portlet, el, options) {
		var instance = this;

		var A = AUI();

		options = options || {};

		var plid = options.plid || themeDisplay.getPlid();
		var doAsUserId = options.doAsUserId || themeDisplay.getDoAsUserIdEncoded();

		portlet = A.one(portlet);

		if (portlet) {
			var content = portlet.one('.portlet-content-container');

			if (content) {
				var restore = content.hasClass('aui-helper-hidden');

				content.toggle();
				portlet.toggleClass('portlet-minimized');

				var link = A.one(el);

				if (link) {
					var img = link.one('img');

					if (img) {
						var title = (restore) ? Liferay.Language.get('minimize') : Liferay.Language.get('restore');

						var imgSrc = img.attr('src');

						if (restore) {
							imgSrc = imgSrc.replace(/restore.png$/, 'minimize.png');
						}
						else {
							imgSrc = imgSrc.replace(/minimize.png$/, 'restore.png');
						}

						img.attr('alt', title);
						img.attr('title', title);

						link.attr('title', title);
						img.attr('src', imgSrc);
					}
				}

				var html = '';
				var portletBody = content.one('.portlet-body');

				if (portletBody) {
					html = portletBody.html();
				}

				var hasBodyContent = !!(A.Lang.trim(html));

				if (hasBodyContent) {
					content.unplug(A.Plugin.IO);
				}
				else {
					content.plug(
						A.Plugin.IO,
						{
							autoLoad: false,
							data: {
								doAsUserId: doAsUserId,
								p_l_id: plid,
								p_p_id: portlet.portletId,
								p_p_state: 'exclusive'
							},
							showLoading: false,
							uri: themeDisplay.getPathMain() + '/portal/render_portlet'
						}
					);
				}

				A.io.request(
					themeDisplay.getPathMain() + '/portal/update_layout',
					{
						after: {
							success: function() {
								if (restore && content.io) {
									content.io.start();
								}
							}
						},
						data: {
							cmd: 'minimize',
							doAsUserId: doAsUserId,
							p_l_id: plid,
							p_p_id: portlet.portletId,
							p_p_restore: restore
						}
					}
				);
			}
		}
	},
	['aui-io']
);

Liferay.provide(
	Liferay.Portlet,
	'onLoad',
	function(options) {
		var instance = this;

		var A = AUI();

		var canEditTitle = options.canEditTitle;
		var columnPos = options.columnPos;
		var isStatic = (options.isStatic == 'no') ? null : options.isStatic;
		var namespacedId = options.namespacedId;
		var portletId = options.portletId;
		var refreshURL = options.refreshURL;
		/*
		 *  ITERWEB	Luis Miguel
		 *  Parámetros necesarios para la carga de los SWF
		 */
		var flashVars = options.flashVars;
		var urlSWF = options.urlSWF;
		
		if (isStatic) {
			instance.registerStatic(portletId);
		}

		var portlet = A.one('#' + namespacedId);

		if (portlet && !portlet.portletProcessed) {
			portlet.portletProcessed = true;
			portlet.portletId = portletId;
			portlet.columnPos = columnPos;
			portlet.isStatic = isStatic;
			portlet.refreshURL = refreshURL;

			// Functions to run on portlet load

			if (canEditTitle) {
				Liferay.Util.portletTitleEdit(
					{
						doAsUserId: themeDisplay.getDoAsUserIdEncoded(),
						obj: portlet,
						plid: themeDisplay.getPlid(),
						portletId: portletId
					}
				);
			}

			if (!themeDisplay.layoutMaximized) {
				var configurationLink = portlet.all('#portlet-topper-toolbar_' + portlet.portletId + ' .portlet-configuration a');

				configurationLink.on(
					'click',
					function(event)
					{
						// Comprueba si es el portlet de Paywall, de recomendaciones o de feedback (Sólo tiene configuración angular)
						var isPaywallPortlet = portlet.portletId.indexOf("paywallportlet_WAR_newsportlet") === 0;
						var isRecommendationPortlet = portlet.portletId.indexOf("articlerecommendationsportlet_WAR_newsportlet") === 0;
						var isFeedbackPortlet = portlet.portletId.indexOf("userfeedbackportlet_WAR_trackingportlet") === 0;
						
						// Comprueba si el navegador es IE11 o anterior
						var ie = window.navigator.userAgent.indexOf('MSIE ') > 0 || window.navigator.userAgent.indexOf('Trident/') > 0;
						
						// Comprueba si se pulsó la combinación mano-pie-nariz
						var alternative = event.ctrlKey && event.altKey && event.shiftKey;
						
						// Comprueba si vienen ambas URL (Flex y Angular)
						var splittedUrl = urlSWF.split("|");
						
						// Se lanza la configuración Flex si:
						//   - Es la URL antigua
						//   - No es el portlet de Paywall ni de recomendaciones (Sólo tiene configuración angular) y
						//     - Estamos en un IE11 y no se pide modo alternativo
						//     - No estamos en IE11 y se pide modo alternativo
						if (splittedUrl.length == 1 || (!isPaywallPortlet && !isRecommendationPortlet && !isFeedbackPortlet && ((ie && !alternative) || (!ie && alternative))))
						{
							var configurationURL = event.currentTarget.attr('href');
							/*
							 *  ITERWEB	Luis Miguel
							 *  Se comprueba que el objeto que lanzó el evento es el mismo que lo está escuchando,
							 *  	de ésta manera evitamos que se lancen dos o más ventanas de configuración en los casos de los portelts anidados
							 *  Se pasan los parámetros del swf a la configuración
							 */
							if( event.currentTarget.attr('id').indexOf(portletId)!=-1 )
								instance.openConfiguration(portlet, portletId, configurationURL, namespacedId, splittedUrl[0], flashVars);
						}
						// Se lanza la configuración angular
						else
						{
							// Se le añade el portletId para que pueda refrescarlo cuando cambie la configuración
							flashVars += "&portletId=" + namespacedId;
							// Se llama a la aplicación angular
							ngPortletsData.callNgPortlets(splittedUrl[1] + "/" + btoa(flashVars));
						}
						
						event.preventDefault();
					}
				);

				var minimizeLink = portlet.one('.portlet-minimize a');

				if (minimizeLink) {
					minimizeLink.on(
						'click',
						function(event) {
							instance.minimize(portlet, minimizeLink);

							event.halt();
						}
					);
				}

				var maximizeLink = portlet.one('.portlet-maximize a');

				if (maximizeLink) {
					maximizeLink.on(
						'click',
						function(event) {
							submitForm(document.hrefFm, event.currentTarget.attr('href'));

							event.halt();
						}
					);
				}

				var closeLink = portlet.one('.portlet-close a');

				if (closeLink) {
					closeLink.on(
						'click',
						function(event) {
							instance.close(portlet);

							event.halt();
						}
					);
				}

				var refreshLink = portlet.one('.portlet-refresh a');

				if (refreshLink) {
					refreshLink.on(
						'click',
						A.bind(instance.refresh, instance, portlet)
					);
				}

				var printLink = portlet.one('.portlet-print a');

				if (printLink) {
					printLink.on(
						'click',
						function(event) {
							location.href = event.currentTarget.attr('href');

							event.halt();
						}
					);
				}

				var portletCSSLink = portlet.one('.portlet-css a');

				if (portletCSSLink) {
					portletCSSLink.on(
						'click',
						function(event) {
							instance._loadCSSEditor(portletId);
						}
					);
				}
			}

			Liferay.fire(
				'portletReady',
				{
					portlet: portlet,
					portletId: portletId
				}
			);

			var list = instance.list;

			var index = A.Array.indexOf(list, portletId);

			if (index > -1) {
				list.splice(index, 1);
			}

			if (!list.length) {
				Liferay.fire(
					'allPortletsReady',
					{
						portletId: portletId
					}
				);
			}
		}
	},
	['aui-base']
);

Liferay.provide(
	Liferay.Portlet,
	'refresh',
	function(portlet) {
		var instance = this;

		var A = AUI();

		portlet = A.one(portlet);

		if (portlet && portlet.refreshURL) {
			var url = portlet.refreshURL;
			var id = portlet.attr('portlet');

			var placeHolder = A.Node.create('<div class="loading-animation" id="p_load' + id + '" />');

			portlet.placeBefore(placeHolder);
			portlet.remove(true);

			instance.addHTML(
				{
					data: {
						p_p_state: 'normal'
					},
					onComplete: function(portlet, portletId) {
						portlet.refreshURL = url;
					},
					placeHolder: placeHolder,
					url: url
				}
			);
		}
	},
	['aui-base']
);

Liferay.provide(
	Liferay.Portlet,
	'registerStatic',
	function(portletId) {
		var instance = this;

		var A = AUI();

		var Node = A.Node;

		if (Node && portletId instanceof Node) {
			portletId = portletId.attr('id');
		}
		else if (portletId.id) {
			portletId = portletId.id;
		}

		var id = Liferay.Util.getPortletId(portletId);

		instance._staticPortlets[id] = true;
	},
	['aui-base']
);

Liferay.provide(
	Liferay.Portlet,
	'openConfiguration',
	function(portlet, portletId, configurationURL, namespacedId, urlSWF, flashVars) {
		var instance = this;

		var A = AUI();
		
		portlet = A.one(portlet);
		
		/*
		 *  ITERWEB	Luis Miguel
		 *  Cierre de cualquier otra ventana que estuviera abierta, sólo podrá haber un diálogo modal en pantalla a la vez.
		 *  Comprobación de si la configuración es de tipo "SWF" o "Liferay" en función de que el parámetro urlSWF esté relleno.   
		 */
		closeWindow();

		if(urlSWF!='')
		{
			instance.loadSWF(urlSWF, flashVars);
		}
		else
		{
			//Comprobación en portlets anidados para no obtener dos ventanas de 
			//configuración (Si ya existe una ventana con la misma configurationURL 
			//se descartan las siguientes)
			var configurationFrames = document.getElementsByClassName('configuration-frame');
			var checkFrames = true;
			if (configurationFrames)
			{
				for(var i=0; i < configurationFrames.length; i++)
				{
					var currentSrcUrl = configurationFrames[i].getAttribute('src');
					if(currentSrcUrl == configurationURL)
					{
						checkFrames = false;
					}
				}
			}
			
			if (portlet && configurationURL && checkFrames) {
				var title = portlet.one('.portlet-title-default') || portlet.one('.portlet-title');
				var iframeId = namespacedId + 'configurationIframe';
				var iframeTPL = '<iframe class="configuration-frame" frameborder="0" id="' + iframeId + '" name="' + iframeId + '" src="' + configurationURL + '"></iframe>';
				var iframe = A.Node.create(iframeTPL);
	
				var bodyContent = A.Node.create('<div></div>');
	
				bodyContent.append(iframe);
	
				var fixSize = function(number) {
					return ((parseInt(number, 10) || 0) - 5) + 'px';
				};
	
				var updateIframeSize = function(event) {
					setTimeout(
						function() {
							var bodyHeight = bodyNode.getStyle('height');
	
							iframe.setStyle('height', fixSize(bodyHeight));
	
							bodyNode.loadingmask.refreshMask();
						},
						50
					);
				};
	
				var dialog = new A.Dialog(
					{
						after: {
							heightChange: updateIframeSize,
							widthChange: updateIframeSize
						},
						align: {
							node: null,
							points: ['tc', 'tc']
						},
						bodyContent: bodyContent,
						destroyOnClose: true,
						draggable: true,
						title: title.html() + ' - ' + Liferay.Language.get('configuration'),
						width: 820
					}
				).render();
	
				dialog.move(dialog.get('x'), dialog.get('y') + 100);
	
				var bodyNode = dialog.bodyNode;
	
				bodyNode.plug(A.LoadingMask).loadingmask.show();
	
				iframe.on(
					'load',
					function(event) {
						var iframeDoc = iframe.get('contentWindow.document');
	
						iframeDoc.get('documentElement').setStyle('overflow', 'visible');
	
						var iframeBody = iframeDoc.get('body');
	
						iframeBody.addClass('configuration-popup');
	
						iframe.set('height', iframeBody.get('scrollHeight'));
	
						A.on(
							'key',
							function(event) {
								dialog.close();
							},
							[iframeBody],
							'down:27'
						);
	
						var closeButton = iframeBody.one('.aui-button-input-cancel');
	
						if (closeButton) {
							closeButton.on('click', dialog.close, dialog);
						}
	
						bodyNode.loadingmask.hide();
					}
				);
			}
		}//fin else
	},
	['aui-dialog', 'aui-loading-mask']
);

Liferay.provide(
	Liferay.Portlet,
	'_loadCSSEditor',
	function(portletId)
	{
		/*
		 *  ITERWEB	Luis Miguel
		 *  Cierre de cualquier otra ventana que estuviera abierta, sólo podrá haber un diálogo modal en pantalla a la vez.
		 */
		closeWindow();
		Liferay.PortletCSS.init(portletId);
	},
	['liferay-look-and-feel']
);

Liferay.publish(
	'closePortlet',
	{
		defaultFn: Liferay.Portlet._defCloseFn
	}
);

// Backwards compatability

Liferay.Portlet.ready = function(fn) {
	Liferay.on(
		'portletReady',
		function(event) {
			fn(event.portletId, event.portlet);
		}
	);
};

/*
 *  ITERWEB	Luis Miguel
 *  
 *  Funciones de carga, descarga y posicionamiento, al miminimzar y restaurar, del SWF    
 */
Liferay.provide(
		Liferay.Portlet,
		'loadSWF',
			function(urlSWF, flashVars)
			{
				var target_element = document.getElementById("flex-msie");
				var obj = createIeObject( urlSWF, flashVars );
				target_element.parentNode.replaceChild(obj, target_element);
				
				jQryIter("#currentPortletConfig").show();
			},
		['aui-base']
);

function createIeObject(urlswf, flashvars){
	   var div = document.createElement("div");
	   var ovejota = "<object classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' width='100%' height='100%' id='flex-msie'>" +
	   					"<param name='movie' value='" + urlswf + "'> " +
	   					"<param name='quality' value='high'>" +
	   		            "<param name='bgcolor' value='#ffffff'>" +
	   		            "<param name='allowScriptAccess' value='sameDomain'>" +
	   		            "<param name='allowFullScreen' value='true'>" +
	   		            "<param name='wmode' value='transparent'>" +
	   		            "<param name='flashVars' value='" + flashvars + "' id='param-flashvars-msie'/>"+
	   		            "<object type='application/x-shockwave-flash' data='" + urlswf + "' width='100%' height='100%' id='flex-other'>" +
	   		                "<param name='quality' value='high'>" +
	   		                "<param name='bgcolor' value='#ffffff'>" +
	   		                "<param name='allowScriptAccess' value='sameDomain'>" +
	   		                "<param name='allowFullScreen' value='true'>" +
	   			            "<param name='wmode' value='transparent'>" +
	   			            "<param name='flashvars' value='" + flashvars + "' id='param-flashvars-other'/>" +
	   		            "</object>" +
	   				 "</object>";
	   
	   div.innerHTML = ovejota;
	   return div.firstChild;
}

/*
 *  ITERWEB	Luis Miguel
 *  
 *  Funciones que se usan como ExternalInterface del SWF para refrescar el portlet que se esta configurando, 
 *  cerrar el diálogo modal, minimizar y restaurar.    
 */
function refreshPortlet(portletID)
{
	if (window.parent) {
		try
		{
			window.parent.Liferay.Portlet.refresh(portletID);
		}
		catch(e)
		{
			Liferay.Portlet.refresh(portletID);
		}
	}
	else
	{
		Liferay.Portlet.refresh(portletID);
	}
}

function minimizeWindow(headerHeight)
{
	placeSWF('minimize', headerHeight);
}

function restoreWindow()
{
	placeSWF('restore', 0);
}


/*
 * Función que se usa para establecer el foco inicial en la aplicación Flex
 * 
 */
function setFocus(value)
{
	var id_element = null;
	
	if(/Firefox/i.test(navigator.userAgent))
	{
		/*firefox*/
		id_element = document.getElementById("flex-other");
	}
	
	if(/MSIE/i.test(navigator.userAgent))
	{
		/* explorer */
		id_element = document.getElementById("flex-msie");
	}
	
	if(/Chrome/i.test(navigator.userAgent))
	{
		/*chrome */
		id_element = document.getElementById("flex-other");
		id_element.tabIndex = 0;
	}
	
	if (id_element != null )
		id_element.focus();
}

function advertisementControlFullScreen()
{
	jQryIter(".portlet-borderless-container").css("display","none");
	jQryIter(".aui-w75").css("width","100%");
}

function advertisementControlRestoreScreen()
{
	jQryIter(".aui-w75").css("width","75%");
	jQryIter(".portlet-borderless-container").css("display","");
}

/*
 *  Funciones que se usan como ExternalInterface del SWF para la gestion del ckEditor 
 */

function openEditCKEditor(lg, w, h, titleW, param, cbinheritcssmsg) 
{
	if(cbinheritcssmsg)
		cbinheritcssmsg = encodeURIComponent(escape(cbinheritcssmsg));
	
	jQryIter('<input>').attr({id: 'ckEditorMessage', type: 'hidden'}).appendTo('body');
	
	jQryIter("#ckEditorMessage").val(param);
	
	var popup = window.open('/base-portlet/html/components/ckEditor.jsp?w='+w+'&h='+h+'&lg='+lg+
							'&input=ckEditorMessage&cbinheritcssmsg=' + cbinheritcssmsg, titleW, 
							'width='+w+',height='+h+',scrollbars=no,resizable=yes,location=no');
	
}

function llamaAlFlex(param)
{
	var id_element = getId_element("");
	
	document[id_element].llamaAlFlexCallback(param);
}

function getId_element( prefix )
{
	var id_element = null;
	if(/MSIE/i.test(navigator.userAgent))
		id_element = "flex-msie";
	else
		id_element = "flex-other";
	
	return prefix + id_element;
}


/* 
 *  Funciones que se usan como ExternalInterface del SWF de la dockbar    
 */


//Función que añade un escuchador del evento click al botón cerrar el diálogo 'Añadir contenido' para notificarlo al flex
function onClickClosethick()
{	
	jQryIter(document).on
	(
		"click",
		"#closethick",
		function()
		{
			document[getId_element("dockbarswf-")].closeAddContentPanelCallback();
		}
	);

}

function showControls(show)
{
	var visibleControlsClass = 'controls-visible';
	var hiddenControlsClass = 'controls-hidden';
	
	setBodyClass(show, visibleControlsClass, hiddenControlsClass );
}

function setBodyClass(show, visibleClass, hiddenClass )
{
	var docBody = jQryIter(document.body);
	
	docBody.toggleClass(visibleClass, show);
	docBody.toggleClass(hiddenClass, !show);	
}



//Función que muestra el diálogo 'Añadir contenido' para añadir portlets a la página
function showApplicationsDialog(xPos, dialogTitle, searchLabel, msgInfoLabel)
{
	//diálogo 'Añadir contenido'
	var addApplicationsDialog = null;
	
	var yPos = jQryIter("#ngPortlets_iframe_container").offset().top + jQryIter("#ngPortlets_iframe_container").height() -2;
	
		AUI().use(
			    'aui-dialog',
			    'liferay-layout-configuration',
			    function(A) 
			    {
			        // Create the Add Applications dialog
			    	addApplicationsDialog = new A.Dialog({
			            title: dialogTitle,
			            width: 280,
			            visible: false,
			            xy:[xPos,yPos]
			        }).plug(A.Plugin.IO, {
			            after: {
			                success: function(event, id, obj) {
			                    Liferay.LayoutConfiguration._dialogBody = addApplicationsDialog.get('contentBox');
			                    Liferay.LayoutConfiguration._loadContent();
			                }
			            },
			            autoLoad: false,
			            data: {
			                doAsUserId: themeDisplay.getDoAsUserIdEncoded(),
			                p_l_id: themeDisplay.getPlid(),
			                p_p_id: 87,
			                p_p_state: 'exclusive',
			                searchLabel: searchLabel,
			                msgInfoLabel: msgInfoLabel
			            },
			            showLoading: false,
			            uri: themeDisplay.getPathMain() + '/portal/render_portlet'
			        });
			    	
			    	addApplicationsDialog.render().show().io.start();
			    }
			);
}
