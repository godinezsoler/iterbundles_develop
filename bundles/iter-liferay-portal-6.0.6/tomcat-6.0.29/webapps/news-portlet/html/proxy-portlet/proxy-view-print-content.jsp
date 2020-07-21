<%
	htmlContent = new ExternalServiceUtil(serviceId).getServiceContent(plid);

	if (Validator.isNotNull(htmlContent))
	{
		out.print(htmlContent);
	}
	else if (IterKeys.ENVIRONMENT_PREVIEW.equals(environment))
	{
		//TODO no hay contenido
		out.print("No hay contenido");
	}
%>