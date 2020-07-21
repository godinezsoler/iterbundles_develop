<%@ include file="metadataLocatorInit.jsp" %>

<p class="autocompleteWrapper">
	<div class="autocompleteLabel"><%= title %></div>
	<div class="autcompleteInputWrapper">
		<input type="text" id="autocomplete" class="ui-autocomplete-input autocompleteInput" 
		   	   autocomplete="off" role="textbox" aria-autocomplete="list" aria-haspopup="true">
	</div>
</p>

<%
if(modelId != -1)
{
%>
	<script type="text/javascript">
		jQryIter(function() {
			jQryIter( "#autocomplete" ).autocomplete( {
				source: function( request, response ) {
					var url = "/news-portlet/metalocator/"   	+ 
							   <%= numMetadata %>  			 	+ "/" +
							  '<%= contentVocabularyIdsJoin %>' + "/" +
							  '<%= contentCategoryIdsJoin %>'   + "/" +
							   <%= onlyMetadataLastLevel %>     + "/" +
							   <%= modelId %>     				+ "/" +
							   <%= scopeGroupId %>     			+ "/" +
							  '<%= contentType %>'				+ "?term=" +
							  request.term;
					jQryIter.ajax({
								   url: url,
								   dataType: "json",
								   success: function(data) {
									   response( jQryIter.map( data, function( item ) {
										   return {
											   id: item.id, 
											   label: item.label, 
											   value: item.value,
											   url: item.url
										   };
									   }));
									   jQryIter("#autocomplete").removeClass("ui-autocomplete-loading");
								   },
								   error: function(data) {
									   jQryIter("#autocomplete").removeClass("ui-autocomplete-loading");
								   }
					});
				},
				select: function( event, ui ) {
					location = ui.item.url;
				},
				delay: <%= milliseconds %>,
				minLength: <%= numCharacters %>,
				html: true,
		        open: function(event, ui) {
		        	jQryIter(".ui-autocomplete").css("z-index", 1000);
		        }
			});
		});
	</script>
<%
}
%>