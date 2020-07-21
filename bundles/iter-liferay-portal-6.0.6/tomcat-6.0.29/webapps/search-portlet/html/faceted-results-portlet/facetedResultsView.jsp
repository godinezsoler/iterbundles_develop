<%@page import="org.apache.commons.net.util.Base64"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.protecmedia.iter.search.util.FacetedResultTools"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.search.util.FacetPreferences.RangePreferences"%>
<%@page import="com.protecmedia.iter.search.util.FacetPreferences"%>
<%@page import="com.liferay.util.xml.CDATAUtil"%>
<%@page import="com.protecmedia.iter.search.util.SearchKeys"%>
<%@page import="com.liferay.portal.kernel.xml.Node"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.search.Field"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.protecmedia.iter.search.util.FacetedResult"%>
<%@page import="com.protecmedia.iter.search.util.SearchUtil"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.liferay.portal.kernel.search.facet.FacetGroup"%>
<%@page import="com.liferay.portal.kernel.search.facet.FacetedFieldImpl"%>
<%@page import="com.liferay.portal.kernel.search.facet.FacetedRangeImpl"%>
<%@page import="com.liferay.portal.kernel.search.facet.FacetedQueryImpl"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.liferay.portal.kernel.search.facet.FacetedField"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portal.kernel.search.Hits"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>

<portlet:defineObjects />

<%!
private static Log _log = LogFactoryUtil.getLog("search-portlet.docroot.html.faceted-results-portlet.facetedResultsView.jsp");
%>

<%
	String portletId = PortalUtil.getPortletId(request);

	long tIni = 0L;
	if(_log.isTraceEnabled())
	{
		_log.trace("SOLR-TIME portlet de segmentacion " + portletId);
		tIni = System.currentTimeMillis();
	}
	
	Hits results = (Hits)request.getAttribute(WebKeys.SEARCH_RESULT);
	
	if(results!=null)
	{
		
		Map<String, FacetPreferences> portletFacets = (Map<String, FacetPreferences>)request.getAttribute(IterKeys.REQUEST_ATTRIBUTE_FACETS);
		
		FacetPreferences facetPref = portletFacets.get(portletId);
		
		String segmentField = facetPref.getSegmentationType();
		boolean autosegmentation = facetPref.is_autoSegmentation();
		int minArticle = facetPref.getMinArticle();
		int columnas = facetPref.getColumns();
		int resultadosPorColumna = 0;
		List<String> selectedItems = facetPref.getSelectedItems();
		
		String filterquery = ParamUtil.getString(renderRequest, "filterquery", "");
		if(Validator.isNotNull(filterquery))
			filterquery = new String(Base64.decodeBase64(filterquery), StringPool.UTF8);
		Map<String, FacetedField> fields = results.getFacetedFiels();
		FacetedField ff = null;
		List<FacetedResult> groupsData = new ArrayList<FacetedResult>();
		
		if( !autosegmentation && selectedItems!=null && selectedItems.size()>0 )
		{
			ff = fields.get(segmentField);
			if(ff instanceof FacetedRangeImpl)
			{
				for(String item : selectedItems)
				{
					RangePreferences rangePref = facetPref.getRangePreferences(item);
					if(rangePref!=null)
						groupsData.addAll( 
									FacetedResultTools.getRangeFacetedResults(segmentField, ff, filterquery, item,
											facetPref.getDateformat(), facetPref.getDatelanguage(),
											rangePref.getLabel(), rangePref.getGapValue(), rangePref.getGapUnit()) 
								);
				}
			}
			else 
					groupsData.addAll( 
								FacetedResultTools.getFacetedResults(segmentField, ff, filterquery, minArticle, selectedItems) 
							);
		}
		else
		{
			ff = fields.get(segmentField);
			if(ff instanceof FacetedRangeImpl)
			{
				for(String item : selectedItems)
				{
					RangePreferences rangePref = facetPref.getRangePreferences(item);
					if(rangePref!=null)
						groupsData.addAll( 
									FacetedResultTools.getRangeFacetedResults(segmentField, ff, filterquery, item,
											facetPref.getDateformat(), facetPref.getDatelanguage(),
											rangePref.getLabel(), rangePref.getGapValue(), rangePref.getGapUnit()) 
								);
				}
			}
			else
			{
				ff = fields.get(WebKeys.PREFIX_AUTO+segmentField);
				groupsData.addAll( 
							FacetedResultTools.getFacetedResultsAuto(segmentField, ff, filterquery, minArticle, selectedItems) 
						);
			}
		}

		boolean hasData = groupsData.size()>0;
		
		if( facetPref.showEmpty() || hasData )
		{
			resultadosPorColumna = groupsData.size()/columnas;
			Iterator<FacetedResult> itr = groupsData.iterator();
			FacetedResult facet = null;
%>

			<div class="iter-faceted-search" id="<portlet:namespace />iter-faceted-search" data-separator="<%= PortalUtil.getSearchSeparator() %>">
				<div class="iter-faceted-search-container facete <%= segmentField %>">
					<div class="iter-faceted-search-header">
						<!--Texto configurable por cada faceta -->
						<span class="iconHeader"></span>
						<%= facetPref.getTitle() %>
					</div>
<% 
if(autosegmentation && hasData)
{
%>
					<div class="iter-faceted-search-breadcrumb">
						<div class="iter-faceted-search-reset" data-type="<%= segmentField %>"></div>
					</div>
<% 
}
%>
					<div class="iter-faceted-search-content">
						<%
							int elementsPos = 1;
						printData:for(int i = 1; i<=columnas; i++)
							{
						%>
								<div class="iter-faceted-serach-content-column-<%=i%>">
						<%
								for(int j = 1; j<=resultadosPorColumna; j++)
								{
									if(itr.hasNext())
									{
										facet = itr.next();
						%>
										<div id="<%=facet.getId()%>" class="element element-<%=elementsPos++%> <%=facet.getSelected()%>" data-url="<%=facet.getFilter()%>">
											<span class="iconCheck"></span>
											<span class="label"><%= facet.getLabel() %></span>
											<span class="number"><%= facetPref.getPrefix() + facet.getCount() + facetPref.getSufix() %>
										</div>
						<%
									}
									else
									{
						%>
								</div>
						<%
										break printData;
									}
								}
						%>
								</div>
						<%
							}
						%>
						<div class="clear"> </div>
					</div>
				</div>
			</div>
<%
		}
	}
	
	if(_log.isTraceEnabled())
		_log.trace("SOLR-TIME portlet de segmentacion " + portletId + " Tiempo de pintado: " + (System.currentTimeMillis()-tIni) + " ms.");
%>