package com.protecmedia.iter.user.util.export_formats;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

public class CSVUtil
{
	final static private String NEXT_CELL 			= ";";
	final static private String LINE_BREAK 			= "\r\n";
	final static private String SIMPLE_LINE_BREAK 	= "\n";	
	final static private String MULTILINE_ENCLOSER	= "\"";
	final static private byte[] BOM_UTF8 = new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF};
		
	private static Log _log = LogFactoryUtil.getLog(CSVUtil.class);

	/**
	 * @param values
	 * @return
	 */
	public static String getCSV(List< List<Object> > values)
	{
		StringBuilder csvValue = new StringBuilder("");
		
		for (int iRow = 0; iRow < values.size(); iRow++)
		{
			int iNumColumns = values.get(iRow).size();
			for (int iColumn = 0; iColumn < iNumColumns; iColumn++)
			{
				Object obj = values.get(iRow).get(iColumn);
				
				if (obj instanceof String)
				{
					// Celda monolínea
					csvValue.append(MULTILINE_ENCLOSER);
					csvValue.append( String.valueOf(obj) );
					csvValue.append(MULTILINE_ENCLOSER);
				}
				else if (obj instanceof List<?>)
				{
					// Celda multilínea
					@SuppressWarnings("unchecked")
					List<String> cellValues = (List<String>)obj;
					if(!cellValues.isEmpty() && !(cellValues.size() == 1 && Validator.isNull(cellValues.get(0))))
					{
						csvValue.append(MULTILINE_ENCLOSER);
						for (int iCellValue = 0; iCellValue < cellValues.size(); iCellValue++)
						{
							csvValue.append(cellValues.get(iCellValue));
							
							if (iCellValue < (cellValues.size()-1))
								csvValue.append(SIMPLE_LINE_BREAK);
						}
						csvValue.append(MULTILINE_ENCLOSER);
					}
				}
				
				if (iColumn < (iNumColumns-1))
					csvValue.append(NEXT_CELL);
			}
			
			csvValue.append(LINE_BREAK);
		}
		
		return csvValue.toString();
	}
	
	
	
	
	
	/**
	 * 
	 * @param response
	 * @param csvValue
	 * @throws IOException
	 */
	public static void writeCSV(HttpServletResponse response, String csvValue) throws IOException
	{
	    response.setHeader("Content-Type", "text/csv");
	    response.setHeader("Content-Disposition", "attachment;filename=\"data.csv\"");

		response.setContentType("application/csv");
		
		ServletOutputStream out = null;
		try
		{
			out = response.getOutputStream();
			out.write( BOM_UTF8 );
			out.write( csvValue.getBytes() );
			out.flush();
		}
		catch (IOException ioe)
		{
			_log.error(new StringBuffer("Error while sending csv content"));
			throw ioe;
		}
		finally
		{
			if (out != null)
				out.close();
		}
	}
}
