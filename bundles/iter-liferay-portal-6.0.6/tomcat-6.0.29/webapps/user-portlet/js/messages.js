function showError(wndTitle, okBttLbl, excep)
{
	var errorDlg = jQryIter('<div></div>');
	
	errorDlg.dialog({
		 
		dialogClass: "d-modal",
		modal: true,
		title: wndTitle,
		open: function(event, ui)
		{
			jQryIter(".ui-widget-overlay").css({
				background: "rgb(54, 54, 54)",
				background: "rgba(54, 54, 54, 0.5)",
				height: "100%",
				width: "100%",
				left: "0",
				top: "0",
				position: "fixed",
				"z-index": "9999999"
			});
			
			var textarea = jQryIter('<div id="MenuDialog">' + excep + '</div>');
			jQryIter(this).html(textarea);
		},
		buttons:[
		          	{text: okBttLbl, click: function() {jQryIter(this).dialog("close");}}
              	]
	});

};

function showWarn(wndTitle, okBttLbl, ref, excep)
{
	var warnDlg = jQryIter('<div></div>');
	
	warnDlg.dialog({
		 
		dialogClass: "d-modal",
		modal: true,
		title: wndTitle,
		open: function(event, ui)
		{
			jQryIter(".ui-widget-overlay").css({
												background: "rgb(54, 54, 54)",
												background: "rgba(54, 54, 54, 0.5)",
												height: "100%",
												width: "100%",
												left: "0",
												top: "0",
												position: "fixed",
												"z-index": "9999999"
											});
		    
			var textarea = jQryIter('<div id="MenuDialog">' + excep + '</div>');
			jQryIter(this).html(textarea);
		},
		close: function(event, ui)
		{
			if(ref != "")
				window.location.href = ref;
		},
		buttons: [
		          	{text: okBttLbl, click: function()
		          			{
		          				jQryIter(this).dialog("close");

		          				var hayAbridor=true;
		    					try
		    					{
		    						hayAbridor = window.opener != null;
		    					}
		    					catch (e) {}
		    		
			    				if(hayAbridor)
			    					window.close();
			    				else if(ref!="")
		          					window.location.href = ref;
		          				else
		          					window.location.reload(true);
		          			}
		          	}
              	]
	});

};

function showInfo(wndTitle, okBttLbl, msg, ref, reload)
{
	var errorDlg = jQryIter('<div></div>');
	
	errorDlg.dialog({
		 
		dialogClass: "d-modal",
		modal: true,
		title: wndTitle,
		open: function(event, ui)
		{
			jQryIter(".ui-widget-overlay").css({
				background: "rgb(54, 54, 54)",
				background: "rgba(54, 54, 54, 0.5)",
				height: "100%",
				width: "100%",
				left: "0",
				top: "0",
				position: "fixed",
				"z-index": "9999999"
			});
			
			var textarea = jQryIter('<div id="MenuDialog">' + msg + '</div>');
			jQryIter(this).html(textarea);
		},
		close: function(event, ui)
		{
			if(ref != "")
				window.location.href = ref;
		},
		buttons:[
		          	{text: okBttLbl, click: function()
		          		{
		          			jQryIter(this).dialog("close");
		          			
		          			if(ref!="")
	          					window.location.href = ref;
	          				else
	          					if(reload===true)
	          						window.location.reload(true);
		          		}
		          	}
              	]
	});

};
