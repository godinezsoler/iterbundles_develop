package com.protecmedia.iter.services.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.TermQuery;
import com.liferay.portal.kernel.search.TermQueryFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
 
public class GeolocationUtil {
	
	private static Log _log = LogFactoryUtil.getLog(GeolocationUtil.class);
	
	public static String ipServiceUrl = "http://freegeoip.net/xml/";
	
	
	
	public static boolean validateZoom(String zoom){
		
		boolean valid = true;
		try{
			if ((Integer.valueOf(zoom)<0) || (Integer.valueOf(zoom)>21)){
				valid = false;
			} 
			
		}catch(Exception e){
			valid = false;
		}
		return valid;
	}
	
	public static boolean validateMapType(String mapType){
		
		boolean valid = true;
		try{
			if (mapType.equalsIgnoreCase("r")||(mapType.equalsIgnoreCase("s"))||
			   (mapType.equalsIgnoreCase("h"))||(mapType.equalsIgnoreCase("t"))){
			}
			else{
				valid = false;
			}
		}
		catch(Exception e){
			valid = false;
			
		}
		return valid;
	}
	
	public static Geolocated findZoomAndmapType(String configurationZoom, String configurationMapType, List<Geolocated> geolocationFieldsProccessed){
		
		Geolocated geolocated = new Geolocated();
		boolean foundedZoom = false;
		boolean foundedMapType = false;
		int counter = 0;
		String zoom = "";
		String mapType = "";
		while (!foundedZoom && !foundedMapType && counter<geolocationFieldsProccessed.size()){
			Geolocated actualField = geolocationFieldsProccessed.get(counter);
			zoom = actualField.getZoom();
			mapType = actualField.getMapType();
			if (!zoom.equals("") && validateZoom(zoom))
				foundedZoom = true;
			if (!mapType.equals("") && validateMapType(mapType))
				foundedMapType = true;
			
			counter++;
		}
		//Tiene prioridad lo encontrado en el campo de Geolocation
		if ( (zoom.equals("")) || (!zoom.equals("")&&!validateZoom(zoom)) ){
			geolocated.setZoom(configurationZoom);
		}
		else if (!zoom.equals("")&&(validateZoom(zoom))){
			geolocated.setZoom(zoom);
		}
		//Aqui es igual
		if ( (mapType.equals("")) || (!mapType.equals("")&&!validateMapType(mapType)) ){
			geolocated.setMapType(configurationMapType);
		}
		else if (!mapType.equals("")&&(validateMapType(mapType))){
			geolocated.setMapType(mapType);
		}
	
		//Establezco el valor real que interpreta el servicio de google Maps
		mapType = geolocated.getMapType();
//		if (!mapType.equals("")){
//			if (mapType.equalsIgnoreCase("r")){
//				geolocated.setMapType("G_NORMAL_MAP");
//			}
//			if (mapType.equalsIgnoreCase("s")){
//				geolocated.setMapType("G_SATELLITE_MAP");
//			}
//			if (mapType.equalsIgnoreCase("h")){
//				geolocated.setMapType("G_HYBRID_MAP");
//			}
//			if (mapType.equalsIgnoreCase("t")){
//				geolocated.setMapType("G_PHYSICAL_MAP");
//			}
//		}
		if (!mapType.equals("")){
			if (mapType.equalsIgnoreCase("r")){
				geolocated.setMapType("1");
			}
			if (mapType.equalsIgnoreCase("s")){
				geolocated.setMapType("2");
			}
			if (mapType.equalsIgnoreCase("h")){
				geolocated.setMapType("3");
			}
			if (mapType.equalsIgnoreCase("t")){
				geolocated.setMapType("4");
			}
		}
		
		return geolocated;
	}
	
	public static Geolocated processOptions(String options){
		
		Geolocated geolocated = new Geolocated();
		StringTokenizer st = new StringTokenizer(options,",");
		while (st.hasMoreTokens()){
			String option = st.nextToken();
			StringTokenizer sto = new StringTokenizer(option,"='");
			String key = "";
			String value = "";
			if (sto.hasMoreTokens()){
				key = sto.nextToken(); 
			}
			if (sto.hasMoreTokens()){
				value = sto.nextToken(); 
			}
			if (key.equalsIgnoreCase("l")){
				geolocated.setMarkerText(value);
			}
			if (key.equalsIgnoreCase("z")){
				geolocated.setZoom(value);
			}
			if (key.equalsIgnoreCase("m")){
				geolocated.setMapType(value);
			}
		}
		

		return geolocated;
		
	}
	
	public static List<Geolocated> proccessGeolocationFields(List<String> geolocationFields){
	
		List<Geolocated> proccesedList = new ArrayList<Geolocated>();
		String mapOptions = "";
		String address = "";
		for (String field : geolocationFields){
			StringTokenizer st = new StringTokenizer(field,"[]");
			mapOptions = "";
			if (st.hasMoreTokens()){
				address = st.nextToken();
			}
			if (st.hasMoreTokens()){
				mapOptions = st.nextToken().replaceAll(" ", "");
			}
			Geolocated geolocated = processOptions(mapOptions);
			geolocated.setPostalAddress(address);
			proccesedList.add(geolocated);
		}
		
		return proccesedList;
		
	}
	
	public static Map<String,String> getLatitudeLongitudeFromField(long groupId, String articleId, String language){
		
		//Extraigo contenido del campo geolocation del articulo en formato de direccion postal
		Map<String,String> map = new HashMap<String,String>();
		String field = IterKeys.STANDARD_ARTICLE_GEOLOCATION;
		List<String> geolocation = PageContentLocalServiceUtil.getWebContentField(groupId, articleId, field, language);
		StringTokenizer st = new StringTokenizer(geolocation.get(0),", ");
		String tokenList = "";
		while (st.hasMoreTokens()){
			if (tokenList.equalsIgnoreCase("")){
				tokenList = st.nextToken();
			}
			else{
				tokenList = tokenList+"+"+st.nextToken();
			}
		}
		//Uso servicio API de Google para hallar latitud y longitud a partir de la direccion postal
		String xml = "";
		String url ="http://maps.google.com/maps/api/geocode/xml?address=";
		try {
			xml = HttpUtil.URLtoString(url+tokenList+"&sensor=false");
			Document doc = null;
			doc = SAXReaderUtil.read(xml);
			Element root = doc.getRootElement();
			//Location
			Element location = root.element("result").element("geometry").element("location");
			Element lat = location.element("lat");
			Element lng = location.element("lng");
			String latitude = lat.getData().toString();
			String longitude = lng.getData().toString();
			map.put("latitude", latitude);
			map.put("longitude", longitude);
		
		} catch (IOException io) {
			_log.error("Cannot access geolocation service at " + url);
		} catch (DocumentException d) {
			_log.error("Parsing response xml failed");
		} catch (Exception e) {
			_log.error("Cannot obtain geolocation data");
			_log.debug(e);
		}
		
		return map;
	}
	
	
	public static Map<String,String> getIpData(String ip){
		
		Map<String,String> map = new HashMap<String,String>();
		
			//Para pruebas:
			//ip = "193.47.76.31";
			String xml = "";
			String url = ipServiceUrl+ip;
			try {
				xml = HttpUtil.URLtoString(url);
				Document doc = null;
				doc = SAXReaderUtil.read(xml);
				Element root = doc.getRootElement();
				//Ciudad
				Element city = root.element("City");
				String ciudad = city.getData().toString();
				//Region
				Element reg = root.element("RegionName");
				String region = reg.getData().toString();
				//Pais
				Element country = root.element("CountryName");
				String pais = country.getData().toString();
				
				map.put("city", ciudad.toLowerCase());
				map.put("region", region.toLowerCase());
				map.put("country", pais.toLowerCase());
			} catch (IOException io) {
				_log.error("Cannot access geolocation service at " + url);
			} catch (DocumentException d) {
				_log.error("Parsing geolocation service XML response failed");
			} catch (Exception e) {
				_log.error("IP " + ip + " is incorrect or local");
			}
	
		return map;
	}
	
	
	public static List<String> getIdsFromHits(Hits hits){
		
		List<String> list = new LinkedList<String>();
		for (int i = 0; i < hits.getDocs().length; i++) {
			com.liferay.portal.kernel.search.Document result = hits.doc(i);
			String articleId = result.get(Field.ENTRY_CLASS_PK);
			list.add(articleId);
		}
		return list;
	}
	
	
	public static List<String> listIntersection(long scopeGroupId, List<String> idsList, List<String> hitsList, int topNeeded){
		
		int topHits = hitsList.size();
		int topActual; 
		int counter = 0; //counter se incrementa cada vez que se añade un elemento a idsList
		if (topHits < topNeeded){
			topActual = topHits;
		}
		else{
			topActual = topNeeded;
		} 
		try{
			Iterator it = hitsList.iterator();
			while ((counter < topActual) && (counter < topHits) && (it.hasNext())){
				String hit = String.valueOf(it.next());
				if ((!idsList.contains(hit)) && (PageContentLocalServiceUtil.getFirstPageContent(scopeGroupId, hit, true, new Date()) != null)){
					idsList.add(hit);
					counter++;
				}
			}
		}
		catch (Exception e){
			_log.error("Error in ListIntersection Method");
		}
		return idsList;

	}

	
	
	public static BooleanQuery getStructureQuery(String structureId) throws ParseException{
		
		//Filtramos por la estructura
		BooleanQuery structureIdsQuery = BooleanQueryFactoryUtil.create();
		if (!structureId.equalsIgnoreCase("")) {
			if (structureId.equalsIgnoreCase(IterKeys.STRUCTURE_ARTICLE)){
				TermQuery termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_ARTICLE);
				structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			}
			if (structureId.equalsIgnoreCase(IterKeys.STRUCTURE_GALLERY)){
				TermQuery termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_GALLERY);
				structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			}
			if (structureId.equalsIgnoreCase(IterKeys.STRUCTURE_POLL)){
				TermQuery termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_POLL);
				structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			}
			if (structureId.equalsIgnoreCase(IterKeys.STRUCTURE_MULTIMEDIA)){
				TermQuery termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_MULTIMEDIA);
				structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			}
		} else {
			TermQuery termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_ARTICLE);
			structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_GALLERY);
			structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_POLL);
			structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
			termQueryArticle = TermQueryFactoryUtil.create("structureId", IterKeys.STRUCTURE_MULTIMEDIA);
			structureIdsQuery.add(termQueryArticle,BooleanClauseOccur.SHOULD);
		}
		
		return structureIdsQuery;
	}
	
	public static BooleanQuery getLocationQuery(String location, String structureId){
		
		BooleanQuery query = null;
		try{
			query = BooleanQueryFactoryUtil.create();
			//Añado la query de la estructura
			BooleanQuery structureIdsQuery = getStructureQuery(structureId);
			query.add(structureIdsQuery, BooleanClauseOccur.MUST);
			//TermQuery termText = TermQueryFactoryUtil.create(Field.CONTENT, city);
			TermQuery termText = TermQueryFactoryUtil.create(IterKeys.STANDARD_ARTICLE_INDEX_GEOLOCATION, location);
			query.add(termText, BooleanClauseOccur.MUST); 
		}
		catch(Exception e){
			_log.error("Location error failed for location " + location);
		}
		
		return query;
	}
	
	
	public static List<String> search(long scopeGroupId, String ip, long companyId, String structureId, int numberOfResults){
		
		BooleanQuery fullQuery = null;
		
		//Ordenamos segun el parametro order, por defecto por relevancia
		Sort sort = new Sort();
		sort.setFieldName("ordTitle");
		sort.setReverse(false);
		sort.setType(Sort.STRING_TYPE);
		
		//Obtengo los datos de localizacion a partir de la IP
		Map<String,String> ipData = getIpData(ip);
		
		//Filtramos por el texto de la GEOLOCALIZACION
		List<String> idsList = new LinkedList<String>();
		
		try{
			
			//Busco por ciudad
			int total;
			String city = ipData.get("city");
			fullQuery = getLocationQuery(city,structureId);
			Hits hitsCity = SearchEngineUtil.search(companyId, fullQuery, sort, 
			com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS, com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS);
			List<String> idsCity = getIdsFromHits(hitsCity);
			idsList = listIntersection(scopeGroupId,idsList,idsCity,numberOfResults);
			total = idsList.size();
			
			//Si hay menos resultados de los esperados, busco por region
			if (total < numberOfResults){ 
				String region = ipData.get("region");
				fullQuery = getLocationQuery(region,structureId);
				Hits hitsRegion = SearchEngineUtil.search(companyId, fullQuery, sort, 
				com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS, com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS);
				List<String> idsRegion = getIdsFromHits(hitsRegion);
				idsList = listIntersection(scopeGroupId,idsList,idsRegion,numberOfResults-total);
				total = idsList.size();
				
				//Si hay menos resultados de los esperados, busco por pais
				if (total < numberOfResults){
					String country = ipData.get("country");
					fullQuery = getLocationQuery(country,structureId);
					Hits hitsCountry = SearchEngineUtil.search(companyId, fullQuery, sort, 
					com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS, com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS);
					List<String> idsCountry = getIdsFromHits(hitsCountry);
					idsList = listIntersection(scopeGroupId,idsList,idsCountry,numberOfResults-total);
				}
			} 
			
		}catch(SearchException se){
			_log.error("Searching for geolocated contents failed for ip" + ip);
			_log.debug(se);
		}

		return idsList;
		
	}

}
