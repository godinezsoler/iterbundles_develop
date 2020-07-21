<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<div class="tuentiShare" >
	<%-- Source: http://www.tuenti.com/developers/ --%>		
	<script type="text/javascript" src="http://widgets.tuenti.com/widgets.js"></script>
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
		
		tuenti.widget.shareButton("tuenti_share", { "icon-style": "<%= tIcon %>",  "language": "<%= tuenti_lng %>", "share-url": canonical_url });
	</script>
	<a href="http://www.tuenti.com/share" id="tuenti_share"></a>
</div>
