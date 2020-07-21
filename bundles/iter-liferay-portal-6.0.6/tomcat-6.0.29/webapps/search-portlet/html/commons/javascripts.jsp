function <portlet:namespace />cleanKeywords(keywords)
{
	var cleanKeywords = '';
	var cleanKeywordsAux = '';
	for(var i = 0; i < keywords.length; i++)
	{
		var currentChar = keywords[i];
		if(!( currentChar == '.' || currentChar == '/' || currentChar == '\\' || 
			  currentChar == '~' || currentChar == '!' || currentChar == '('  || 
			  currentChar == ')'))
		{
			cleanKeywordsAux += currentChar;
		}
		else
		{
			cleanKeywordsAux += ' ';
		}
	}
	
	cleanKeywordsAux = encodeURIComponent(cleanKeywordsAux);
	
	for(var i = 0; i < cleanKeywordsAux.length; i++)
	{
		var currentChar = cleanKeywordsAux[i];
		if(currentChar == '\'')
		{
			cleanKeywords += '%27';
		}
		else
		{
			cleanKeywords += currentChar;
		}
	}
	
	return cleanKeywords;
}