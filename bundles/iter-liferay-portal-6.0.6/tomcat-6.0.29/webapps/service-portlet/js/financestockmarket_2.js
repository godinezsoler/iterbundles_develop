
function updateTrade(value, name) {
	  var value_encoded = encodeURI("^"+value);
	  var success = true;
	  var subfix = value;
	  var subname = name;
	  if (subname==""){
		  subname= subfix;
	  }
	  jQryIter.getJSON("http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22"+value_encoded+"%22)&format=json&env=http%3A%2F%2Fdatatables.org%2Falltables.env&callback=?", 
    function(json){  
		  try{
			  jQryIter('#change_'+subfix).text(json.query.results.quote.Change); 
			  jQryIter('.nameTrade_'+subfix).text(subname); 
			  jQryIter('.nameTradeComplete_'+subfix).text(json.query.results.quote.Name);
			  jQryIter('#lastTradePrice_'+subfix).text(json.query.results.quote.LastTradePriceOnly);  
			  jQryIter('#percentChange_'+subfix).text(json.query.results.quote.ChangeinPercent);  
			  jQryIter('#lastTradeTime_'+subfix).text(json.query.results.quote.LastTradeTime);  
			  jQryIter('#image_Trade_container_'+subfix).html("<img src='http://chart.finance.yahoo.com/t?s="+value_encoded+"&lang=es-ES&region=ES&width=400&height=200&timestamp="+(new Date()).getTime()+"'>");
	      
		      var change_Percent = json.query.results.quote.ChangeinPercent;  
		      var arrow_direction = change_Percent.substring(0, 1);
		      jQryIter('#arrowUpDown_'+subfix).removeClass('positiva negativa');
		      if (arrow_direction == "+"){
		    	  jQryIter('#arrowUpDown_'+subfix).addClass('positiva');
		      }else{
		    	  jQryIter('#arrowUpDown_'+subfix).addClass('negativa');
		      }
		  } catch (error) {
			  success = false;
		  }
	  });
	  return success;
}