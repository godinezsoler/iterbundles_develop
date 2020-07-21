<script type="text/javascript">

function <portlet:namespace />getRatings()
{
	jQryIter.get('<%=path%>',
		 function(data)
		 {
			jQryIter("span[name='star-total']").each(function(){jQryIter(this).html(data.total);});
			showRedStar(data.average);
		},
        "json"
 	);
}

<portlet:namespace />getRatings();

</script>