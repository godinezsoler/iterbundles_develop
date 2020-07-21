package com.protecmedia.iter.xmlio.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

public class UpdateLiveServlet extends HttpServlet implements Servlet  {

	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(UpdateLiveServlet.class);

	private final String LAYOUT_PARAM = "layout";
	private final String GROUP_PARAM = "group";
	private final String ERROR_EXAMPLE = "Bad request, example request:\n\n" + 
										 "/xmlio-portlet/live?group=CORREO&layout=/portada\n"+
										 "/xmlio-portlet/live?group=CORREO&layout=/servicios/rss\n" + 
										 "/xmlio-portlet/live?group=CORREO&layout=/portada,/servicios/rss";
	
	public UpdateLiveServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer textToPrint = new StringBuffer();
		response.setContentType("text/plain");
	    PrintWriter out = response.getWriter();
	    out.println("==== UpdateLiveServlet ====\n");
		try{
			
			String currentGroupParam = request.getParameter(GROUP_PARAM);
			if(currentGroupParam != null && !currentGroupParam.isEmpty())
			{
				String query = "SELECT groupId FROM Group_ WHERE name='" + currentGroupParam + "'";
				List<Object> results = PortalLocalServiceUtil.executeQueryAsList(query);
				if(results != null && results.size() > 0)
				{
					long groupId = Long.parseLong(results.get(0).toString());
					String currentLayoutParam = request.getParameter(LAYOUT_PARAM);
					if(currentLayoutParam != null && !currentLayoutParam.isEmpty())
					{
						String currentLayouts[] = currentLayoutParam.split(",");
						if(currentLayouts != null && currentLayouts.length > 0)
						{
						
							for(String currentLayout:currentLayouts)
							{
								query = "SELECT plid FROM Layout WHERE friendlyURL='" + currentLayout.toLowerCase() +
										"' AND groupId=" + groupId;
								results = PortalLocalServiceUtil.executeQueryAsList(query);
								
								if(results != null && results.size() > 0)
								{

									String plid =results.get(0).toString();
									List<Live> liveEntries = LiveLocalServiceUtil.getLiveByLocalId(groupId, plid);
									if(liveEntries != null && liveEntries.size() > 0)
									{
										for(Live liveEntry:liveEntries)
										{
											if(	liveEntry.getClassNameValue().equals(IterKeys.CLASSNAME_PORTLET) || 
												liveEntry.getClassNameValue().equals(IterKeys.CLASSNAME_LAYOUT) )
											{
												liveEntry.setStatus(IterKeys.PENDING);
												LiveLocalServiceUtil.updateLive(liveEntry);
											}
										}
										textToPrint.append("Layout " + currentLayout + " with plid: " + plid + " updated\n");
									}else{
										textToPrint.append("No Xmlio_Live entries for groupId: " + 
															groupId + " and localId: " + plid + "\n");
									}
								}else{
									textToPrint.append("No Layout with friendlyURL: " + currentLayout.toLowerCase() + "\n");
								}
							}
						}else{
							textToPrint.append(ERROR_EXAMPLE);
						}
					}else{
						textToPrint.append(ERROR_EXAMPLE);
					}
				}else{
					textToPrint.append("No Group with name: " + currentGroupParam + "\n");
				}
			}else{
				textToPrint.append(ERROR_EXAMPLE);
			}
		}
		catch(Exception e){
			_log.error(e);	
			out.println("Error UpdateLiveServlet:\n");
			e.printStackTrace(out);
			textToPrint = new StringBuffer();
		}finally{
		    out.println(textToPrint);
		    out.flush();
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}	
	
}