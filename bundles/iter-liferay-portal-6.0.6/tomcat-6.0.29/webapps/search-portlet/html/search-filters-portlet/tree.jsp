<script type="text/javascript">

	function setBranch(id,branch)
	{
		jQryIter('#'+id).find("span[class='itemLabel']").after(branch);
		jQryIter(document).ready(function(){
			jQryIter('#'+id).find("ul").branch();
		});
	}

	(function(jQryIter) {

		//Function for first Level
		jQryIter.fn.topLevel = function() {
			
			function ajaxRequest(id)
			{
				<portlet:namespace />loadTeasersByPage(id,"topLevel");	
			}
		
			this.each(function() 
			{		   
				var $root = this;
							   
				// Initial state
				jQryIter("li", jQryIter(this)).prepend('<span>&nbsp;</span>');
				
				jQryIter("li[haschildren='1'] > span[class!='itemLabel']", jQryIter(this)).addClass('bulletItem collapsed').html('+');
				jQryIter("li[haschildren='0'] > span[class!='itemLabel']", jQryIter(this)).addClass('bulletItem expanded').html('-');
				
				// Expand/Collapse
				jQryIter("li > span", jQryIter(this)).click(function(){
						
					if (jQryIter(this).is(".collapsed")) {
						jQryIter("> ul", jQryIter(this).parent("li")).removeClass('hide');
						jQryIter(this).removeClass("bulletItem collapsed").addClass("bulletItem expanded").html('-');
						
						//Loads level just once from server
						if (jQryIter(this).attr('expanded') != "yes"){
							ajaxRequest(jQryIter(this).parent("li").attr('id'));
							jQryIter(this).attr("expanded", "yes");
						}
					
					} else if (jQryIter(this).is(".expanded")) {
						if (jQryIter(this).parent("li[haschildren='1']").length > 0){
							jQryIter("> ul", jQryIter(this).parent("li")).addClass('hide');
							jQryIter(this).removeClass("bulletItem expanded").addClass("bulletItem collapsed").html('+');
						}
					}
					
				});
				
			});
			
			return this;
			
		};
		
		//Function for the rest of levels
		jQryIter.fn.branch = function() {
		
			function ajaxRequest(id)
			{
				<portlet:namespace />loadTeasersByPage(id,"nonTopLevel");	
			}
			
			function addCategory(id)
			{
				categoriesList.push(id);
			}
			
			function removeCategory(id)
			{
				categoriesList.pop(id);
			}
	
			this.each(function() {
				
				// Initial state
				jQryIter("li", jQryIter(this)).prepend('<span>&nbsp;</span>');
				
				jQryIter("li[haschildren='1'] > span[class!='itemLabel']", jQryIter(this)).addClass('bulletItem collapsed').html('+');
				jQryIter("li[haschildren='0'] > span[class!='itemLabel']", jQryIter(this)).addClass('bulletItem expanded').html('-');
				
				// Checkbox function
				jQryIter("input[type='checkbox']", jQryIter(this)).click(function(){
					
					if (jQryIter(this).is(":checked")) {
						addCategory(jQryIter(this).parent("li").attr('id'));
					}
					else {
						removeCategory(jQryIter(this).parent("li").attr('id'));
					}
					
				});
				
				// Expand/Collapse
				jQryIter("li > span", jQryIter(this)).click(function(){
					
					if (jQryIter(this).is(".collapsed")) {
						jQryIter("> ul", jQryIter(this).parent("li")).removeClass('hide');
						jQryIter(this).removeClass("bulletItem collapsed").addClass("bulletItem expanded").html('-');
						
						//Loads level just once from server
						if (jQryIter(this).attr('expanded') != "yes"){
							ajaxRequest(jQryIter(this).parent("li").attr('id'));
							jQryIter(this).attr("expanded", "yes");
						}
					
					} else if (jQryIter(this).is(".expanded")) {
						if (jQryIter(this).parent("li[haschildren='1']").length > 0){
							jQryIter("> ul", jQryIter(this).parent("li")).addClass('hide');
							jQryIter(this).removeClass("bulletItem expanded").addClass("bulletItem collapsed").html('+');
						}
					}
					
				});
			
				
			});
			
			return this;
			
		};

	})(jQryIter);

</script>

<style>
	#buttons {
		margin-bottom:20px;
	}
	#mainTree ul {
		margin:0!important;
		padding:0 0 0 20px!important;
	}
	#mainTree ul.hide {
		display:none;
	}
	#mainTree span {
		color:#000000;
		font-family:"Courier New", Courier, monospace;
		cursor:default;
		font-weight:bold;
	}
	#mainTree span.expanded, #mainTree span.collapsed {
		cursor:pointer;
	}
</style>

<ul id="mainTree" class="AdvancedSearchCategoryTree">
<%   	
	List<KeyValuePair> vocabularies = SearchUtil.getVocabularies(excludedVocabularyIds, companyId, groupId); 
	for (KeyValuePair voc : vocabularies)
	{
		String value = voc.getValue().substring(1);
		String hasChildren = voc.getValue().substring(0,1);
%>
		<li id="<%=voc.getKey()%>" class="topLevel listLevel_deep_1" haschildren="<%= hasChildren %>">
			 <input type="checkbox" class="checkItem" disabled/>
			 <span class="itemLabel">
				<%=value%>
			 </span>
		</li>
<%
	}
%>
</ul>
 	
<script type="text/javascript">
	jQryIter(document).ready(function(){
		jQryIter('ul#mainTree').topLevel();
	});
</script>

<script type="text/javascript">
	var <portlet:namespace />loadTeasersByPage = function (id,type) {
		jQryIter.ajax(
		{
			type: 'GET',
			url: '/search-portlet/html/search-filters-portlet/branch.jsp',
			data: 
				{
					portletId: '<%=PortalUtil.getPortletId(request)%>',
					id: id,
					type: type,
					excludedCategoryIds: '<%=excludedCategoryIds%>',
					selectedCategoryIds: categoriesList.join('-'),
					scopeGroupId: '<%=scopeGroupId%>',
					companyId: '<%=companyId%>',
					groupId: '<%=groupId%>'
				},
   			success: 
   				function(response) {
					setBranch(id,response);
   				},
			error: 
				function(xhr, status, error) 
	  				{}
		});
	};
</script>