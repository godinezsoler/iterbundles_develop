<%@page import="javax.servlet.ServletOutputStream,javax.xml.transform.OutputKeys,java.io.PrintWriter,javax.xml.parsers.ParserConfigurationException,org.w3c.dom.Attr,javax.xml.transform.TransformerException,javax.xml.transform.TransformerConfigurationException,javax.xml.transform.TransformerFactoryConfigurationError,javax.xml.transform.Transformer,javax.xml.transform.TransformerFactory,javax.xml.transform.stream.StreamResult,java.io.StringWriter,javax.xml.transform.dom.DOMSource,org.w3c.dom.Node,org.w3c.dom.Element,org.w3c.dom.Document,org.w3c.dom.NodeList,javax.xml.parsers.DocumentBuilder,javax.xml.parsers.DocumentBuilderFactory,java.io.File"%><%
	response.setContentType("text/xml");
	PrintWriter out2 = response.getWriter();
	
	String result 	= "";
 	try
	{
		/*parameters*/
		String user 		= request.getParameter("usrid");
	
		
		DocumentBuilder db 	= DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc 		= null;
		
		File fs = new File( application.getRealPath("IterGetEntitlements/usrid=" + user + ".xml") );
		
		if (fs.exists())
		{
			doc = db.parse( fs );
		}
		else
		{
			doc = db.parse( application.getRealPath("IterGetEntitlements/nouser.xml") );
		}
		
		result = getDomAsString(doc);
	}
	catch( Exception e )
	{
		result = getDomAsString(getResponseErrorDom("0x80020101", e.toString()));
	}
 	out2.write( result );
	out2.close();
%><%!
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getDomAsString(Document doc) throws 	TransformerFactoryConfigurationError, 
														TransformerConfigurationException, TransformerException
	{
		// Se obtiene el DOM como cadena para ponerlo en la respuesta
		DOMSource domSource = new DOMSource(doc);                
		StringWriter writer = new StringWriter();                
		StreamResult result = new StreamResult(writer);   
		
		TransformerFactory tf 	= TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();  
		transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		transformer.transform(domSource, result);                
	
		String strValue = writer.toString();
		return strValue;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public Document getResponseErrorDom( String errCode, String errDesc ) throws Exception
	{
		DocumentBuilder db 	= DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document dom		= db.newDocument();
	
		Node node = dom.appendChild( dom.createElement( "error-response" ) );
		
		Element error = dom.createElement( "error" );
		node = node.appendChild( error );
		
		Attr attr = dom.createAttribute("code");
		attr.setNodeValue(errCode);
		error.setAttributeNode(attr);
		
		error.appendChild(dom.createTextNode(errDesc));
		
		return dom;
	}
%>