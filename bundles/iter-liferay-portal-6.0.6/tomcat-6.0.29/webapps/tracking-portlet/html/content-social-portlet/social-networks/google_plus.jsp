<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<div class="googleShare" >
	<%-- Source: http://www.google.com/webmasters/+1/button/ (HTML5 valid) --%>
	<div class="g-plusone" data-size="<%= gpSize %>" data-annotation="<%= gpAnnotation %>"></div>
	
	<script type="text/javascript">
		var canonical_url= "";
		if (jQryIter("link[rel=canonical]").size()>0)
		{
			canonical_url = jQryIter("link[rel=canonical]").attr("href");
		}
		else
		{
			canonical_url = document.URL; //Url completa y válida de la nota a comentar document.URL
		}
		jQryIter("div.g-plusone").attr("data-href", canonical_url);
	
		window.___gcfg = {lang: '<%= gplus_lng %>'};
	  (function() {
	    var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
	    po.src = 'https://apis.google.com/js/plusone.js';
	    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
	  })();
	</script>
</div>