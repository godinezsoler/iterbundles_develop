#if (!$themeDisplay.isWidget())
<div class="columns-max" id="main-content" role="main">
	<div class="portlet-layout">
		<div class="portlet-column portlet-column-only" id="column-1">
#end

			$processor.processMax("portlet-column-content portlet-column-content-only")

#if (!$themeDisplay.isWidget())
		</div>
	</div>
</div>
#end