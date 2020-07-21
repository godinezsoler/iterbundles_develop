<%@page import="java.net.URLDecoder"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<!DOCTYPE html>
<html>
	<head>
		
		<script type='text/javascript' src='/html/js/jquery/jquery.js'></script>
		<script src="ckeditor/ckeditor.js"></script>
		<script type="text/javascript" src="ckeditor/config.js"></script>
		
		<link rel="stylesheet" type="text/css" href="ckeditor/skins/moono/editor_gecko.css">
		<script type="text/javascript" src="ckeditor/styles.js"></script>
		<script type="text/javascript" src="ckeditor/adapters/jquery.js"></script>
	</head>
	<body>	
		
		<%			 
			String widht 			= request.getParameter("w");
			String height 			= request.getParameter("h");
			String leng 			= request.getParameter("lg");
			String input 			= request.getParameter("input");
			String cbinheritcssmsg 	= request.getParameter("cbinheritcssmsg");
		%>
		
		<script type="text/javascript">

			var mywidth 		 	= '<%= widht %>';
			var myheight 		 	= '<%= height %>';
			var lg 				 	= '<%= leng %>';
			var cbinheritcssmsg		= decodeURIComponent(unescape('<%= cbinheritcssmsg %>'));
			var cbinheritcssvalue 	= 'checked';
			
			jQuery(document).ready( function() {
				var opener = window.opener;
				if(opener)
				{
					var parentWindow = jQuery(opener.document.body);
					var inputId = '#'+'<%= input %>';
					var initialDataCKEditor = jQuery(parentWindow).find(inputId).attr("value");

					if(cbinheritcssmsg && cbinheritcssmsg != '')
					{
						var ckeditorStyleHTML = document.createElement('div');
						ckeditorStyleHTML.innerHTML = initialDataCKEditor;
						
						if(ckeditorStyleHTML.firstChild)
						{
							var ckeditorStyleClass = ckeditorStyleHTML.firstChild.getAttribute("class");
							if(ckeditorStyleClass == 'ckeditor-wrapper-content')
								cbinheritcssvalue = '';
						}
					}
				}

				$( 'textarea#idEditor1' ).ckeditor({
					width: mywidth,
					height: myheight,
					language: lg,
					removeButtons: "Maximize",
					
					on: {

						instanceReady: function() {
							this.execCommand('maximize');
							this.setData(initialDataCKEditor);
						},
					}
				});

				if(cbinheritcssmsg && cbinheritcssmsg != '')
				{
					var iterckeditortool = document.getElementById("iterckeditortool");
					iterckeditortool.innerHTML = '<input ' + cbinheritcssvalue + ' style="vertical-align:-15%; " type="checkbox" id="cbinheritcss">' + cbinheritcssmsg + '</input>';
				}
			});

			//Ejecutar función antes del cierre de la ventana para guardar el data del editor en un campo input hidden
			window.onbeforeunload = function(event)
			{
				//Data del ckEditor
				var dataCKEditor = CKEDITOR.instances.idEditor1.getData();
				
				if(cbinheritcssmsg && cbinheritcssmsg != '')
				{
					var cbinheritcsschecked = jQuery('#cbinheritcss').is(':checked');
					if(!cbinheritcsschecked)
					{
						var htmlWrapper = document.createElement('div');
						htmlWrapper.setAttribute('class', 'ckeditor-wrapper-content');
						htmlWrapper.innerHTML = dataCKEditor;
						dataCKEditor = htmlWrapper.outerHTML;
					}
				}
				
				if(window.opener)
				{
					var parentWindow = jQuery(window.opener.document.body);
					window.opener.llamaAlFlex(dataCKEditor);
				}
			};

		</script>


		<textarea class="ckeditor" id="idEditor1" name="editor1"></textarea>
<% 
	if(Validator.isNotNull(cbinheritcssmsg))
	{
%>
		<div id="iterckeditortool" class="cke_path_item" style="font-size: 13px; position: fixed; right: 5px; bottom: 2px; z-index: 10000;"></div>
<%
	}
%>	
	</body>
</html>