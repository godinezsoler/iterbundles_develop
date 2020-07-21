package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.protecmedia.iter.base.service.BlockerAdBlockLocalServiceUtil;


public class StatisticsBlockadblockServlet extends HttpServlet
{
	private static final long serialVersionUID = -1160022437564203858L;
	
	private static Log _log = LogFactoryUtil.getLog(StatisticsBlockadblockServlet.class);
	private static final int DEFAULT_INTERVAL = -30;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			// Inicialización
			String[] splitUri = request.getRequestURI().split(StringPool.SLASH);
			
			// Parámetros de búsqueda
			long scopeGroupId = Long.parseLong(splitUri[3]);

			DateFormat df = new SimpleDateFormat("yyyyMMdd");
			Calendar initialDate = Calendar.getInstance();
			Calendar endDate = Calendar.getInstance();
			try
			{
				initialDate.setTime(df.parse(splitUri[4]));
				endDate.setTime(df.parse(splitUri[5]));
			}
			catch (Exception e)
			{
				if (_log.isDebugEnabled())
					_log.debug("Date interval not informed or incorrect format. Searching statistics for last 30 days.");
				
				initialDate = Calendar.getInstance();
				initialDate.add(Calendar.DAY_OF_MONTH, DEFAULT_INTERVAL);
			}
			String dbInitialDate = df.format(initialDate.getTime());
			String dbEndDate = df.format(endDate.getTime());
			
			// Inicializa la respuesta JSON
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
			
			// Recupera las estadísticas de las peticiones con/sin bloqueo
			List<Object> counters = BlockerAdBlockLocalServiceUtil.getCounters(scopeGroupId, dbInitialDate, dbEndDate);
			JSONArray jsonDaysArray = buildCountersResponse(initialDate, endDate, counters, jsonObject);
			
			// Recupera los días que hubo cambio de modo
			List<Object> modeChanges = BlockerAdBlockLocalServiceUtil.getModeChanges(scopeGroupId, dbInitialDate, dbEndDate);
			buildModeChangesResponse(modeChanges, jsonDaysArray, jsonObject);
			
			// Construye la respuesta
			buildResponse(response, HttpServletResponse.SC_OK, jsonObject);
			
		}
		catch (Throwable th)
		{
			_log.debug(th);
			_log.error(th);
			buildResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
	}
	
	/**
	 * Redirigido a {@link #doGet(HttpServletRequest, HttpServletResponse)}
	 */
	protected void doPost (HttpServletRequest request, HttpServletResponse response)
	{
		this.doGet(request, response);
	}
	
	private void buildResponse(HttpServletResponse response, int status, JSONObject result)
	{
		response.setStatus(status);
		response.addHeader("Access-Control-Allow-Origin", "*");
		
		PrintWriter out = null;
		try
		{
			out = response.getWriter();
			if (null == result)
			{
				response.setContentLength(1);
				out.print(StringPool.PERIOD);
			}
			else
			{
				response.setContentType("application/json");
				out.print(result.toString());
			}
			out.flush();
		}
		catch (IOException e)
		{
		}
		finally
		{
			if (null != out)
				out.close();
		}
	}
	
	private JSONArray buildCountersResponse(Calendar initialDate, Calendar endDate, List<Object> counters, JSONObject jsonObject) throws ParseException
	{
		JSONArray jsonDaysArray = JSONFactoryUtil.createJSONArray();
		JSONArray jsonAllRequestArray = JSONFactoryUtil.createJSONArray();
		JSONArray jsonRequestAdblockArray = JSONFactoryUtil.createJSONArray();
		JSONArray jsonRequestNotAdblockArray = JSONFactoryUtil.createJSONArray();
		JSONArray jsonRequestHadAdblockArray = JSONFactoryUtil.createJSONArray();
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		for (int i = 0; i < counters.size(); i++)
		{
			Object[] counter = ((Object[])counters.get(i));
			
			// Si faltan días, los rellena a 0
			Calendar counterDate = Calendar.getInstance();
			counterDate.setTime(df.parse(counter[0].toString()));
			
			while (initialDate.before(counterDate))
			{
				jsonDaysArray.put(df.format(initialDate.getTime()));
				jsonAllRequestArray.put(0);
				jsonRequestAdblockArray.put(0);
				jsonRequestNotAdblockArray.put(0);
				jsonRequestHadAdblockArray.put(0);
				initialDate.add(Calendar.DAY_OF_MONTH, 1);
			}
			
			// Rellena las estadísticas de ese día
			jsonDaysArray.put(counter[0].toString());
			jsonAllRequestArray.put(counter[1].toString());
			jsonRequestAdblockArray.put(counter[2].toString());
			jsonRequestNotAdblockArray.put(counter[3].toString());
			jsonRequestHadAdblockArray.put(counter[4].toString());
			initialDate.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		// Si faltan días, los rellena a 0
		while (!initialDate.after(endDate))
		{
			jsonDaysArray.put(df.format(initialDate.getTime()));
			jsonAllRequestArray.put(0);
			jsonRequestAdblockArray.put(0);
			jsonRequestNotAdblockArray.put(0);
			jsonRequestHadAdblockArray.put(0);
			initialDate.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		jsonObject.put("days", jsonDaysArray);
		jsonObject.put("all", jsonAllRequestArray);
		jsonObject.put("adblock", jsonRequestAdblockArray);
		jsonObject.put("notAdblock", jsonRequestNotAdblockArray);
		jsonObject.put("hadAdblock", jsonRequestHadAdblockArray);
		
		return jsonDaysArray;
	}
	
	private void buildModeChangesResponse(List<Object> modeChanges, JSONArray jsonDaysArray, JSONObject jsonObject)
	{
		JSONArray jsonModeChanges = JSONFactoryUtil.createJSONArray();
		boolean mode = false;
		boolean prevMode = false;
		for (int i = 0; i < modeChanges.size(); i++)
		{
			mode = Boolean.parseBoolean(((Object[])modeChanges.get(i))[1].toString());
			
			if (i == 0)
			{
				prevMode = mode;
			}
			else if (mode != prevMode)
			{
				int index = -1;
				String date = ((Object[])modeChanges.get(i))[0].toString();
				for (int j = 0; j < jsonDaysArray.length(); j++)
				{
					if (jsonDaysArray.getString(j).equals(date))
					{
						index = j;
						break;
					}
				}
				if (index >= 0)
				{
					jsonModeChanges.put(index);
					prevMode = mode;
				}
			}
		}
		jsonObject.put("modeChanges", jsonModeChanges);
	}
}
