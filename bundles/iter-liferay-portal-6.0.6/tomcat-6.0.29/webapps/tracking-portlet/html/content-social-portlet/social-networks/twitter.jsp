<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<div class="twitterShare" >
	<a href="https://twitter.com/share" class="twitter-share-button"  
		data-lang="<%= twitter_lng %>"
		data-size="<%= twButtonsize %>" 
		data-count="<%= twShowCounter %>">Tweet</a>

	<script>
		var canonical_url= "";
		if (jQryIter("link[rel=canonical]").size()>0)
		{
			canonical_url = jQryIter("link[rel=canonical]").attr("href");
		}
		else
		{
			canonical_url = document.URL; //Url completa y válida de la nota a comentar document.URL
		}
		jQryIter("div.twitterShare a.twitter-share-button").attr("data-counturl", canonical_url);
		jQryIter("div.twitterShare a.twitter-share-button").attr("data-url", canonical_url);
		
		!function(d,s,id){
			var js,fjs=d.getElementsByTagName(s)[0];
			if(!d.getElementById(id)){
				js=d.createElement(s);
				js.id=id;
				js.src="http://platform.twitter.com/widgets.js";
				fjs.parentNode.insertBefore(js,fjs);
			}
		}
		(document,"script","twitter-wjs");
	</script>

<!--<script type="text/javascript" src="http://platform.twitter.com/widgets.js"></script>  -->
</div>