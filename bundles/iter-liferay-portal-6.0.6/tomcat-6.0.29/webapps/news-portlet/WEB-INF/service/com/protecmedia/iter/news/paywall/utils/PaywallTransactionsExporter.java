package com.protecmedia.iter.news.paywall.utils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.dao.orm.IterRS;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class PaywallTransactionsExporter
{
	private static Log log = LogFactoryUtil.getLog(PaywallTransactionsExporter.class);
	
	private HttpServletResponse response;
	private HashMap<String, String> headerTranslationDictionary = new HashMap<String, String>();
	private boolean extended = false;
	
	private static final String HEADER_DATE = "purchasedate";
	private static final String HEADER_GATEWAY = "gateway";
	private static final String HEADER_TXID = "transactionid";
	private static final String HEADER_TXCOMPLETED = "completed";
	private static final String HEADER_USRID = "user";
	private static final String HEADER_USRNAME = "usrname";
	private static final String HEADER_PRODUCTID = "product";
	private static final String HEADER_PRODUCTNAME = "pname";
	private static final String HEADER_AMOUNT = "amount";
	private static final String HEADER_DETAILS = "details";
	
	private static String SQL_EXPORT_SELECT = new StringBuilder()
	.append("SELECT GROUP_CONCAT(                                                   \n")
	.append("    CONCAT('\"', t.purchasedate, '\";'),                               \n")
	.append("    CONCAT('\"', t.gateway, '\";'),                                    \n")
	.append("    CONCAT('\"', t.transactionid, '\";'),                              \n")
	.append("    CONCAT('\"', t.completed, '\";'),                                  \n")
	.append("    CONCAT('\"', IFNULL(t.user, ''), '\";'),                           \n")
	.append("    CONCAT('\"', REPLACE(IFNULL(u.usrname, ''), '\"', '\"\"'), '\";'), \n")
	.append("    CONCAT('\"', IFNULL(t.product, ''), '\";'),                        \n")
	.append("    CONCAT('\"', REPLACE(IFNULL(p.pname, ''), '\"', '\"\"'), '\";'),   \n") 
	.append("    CONCAT('\"', t.amount, '\";')                                      \n")
	.append(") AS csvrow                                                            \n")
    .toString();
	
	private static String SQL_EXPORT_EXTENDED_SELECT = new StringBuilder()
	.append("SELECT GROUP_CONCAT(                                                   \n")
	.append("    CONCAT('\"', t.purchasedate, '\";'),                               \n")
	.append("    CONCAT('\"', t.gateway, '\";'),                                    \n")
	.append("    CONCAT('\"', t.transactionid, '\";'),                              \n")
	.append("    CONCAT('\"', t.completed, '\";'),                                  \n")
	.append("    CONCAT('\"', IFNULL(t.user, ''), '\";'),                           \n")
	.append("    CONCAT('\"', REPLACE(IFNULL(u.usrname, ''), '\"', '\"\"'), '\";'), \n")
	.append("    CONCAT('\"', IFNULL(t.product, ''), '\";'),                        \n")
	.append("    CONCAT('\"', REPLACE(IFNULL(p.pname, ''), '\"', '\"\"'), '\";'),   \n") 
	.append("    CONCAT('\"', t.amount, '\";'),                                     \n")
	.append("    CONCAT('\"', REPLACE(t.transactiondata, '\"', '\"\"'), '\";')      \n")
	.append(") AS csvrow                                                            \n")
    .toString();
	
	private static String SQL_EXPORT_FROM = new StringBuilder()
	.append("FROM iterpaywall_transaction t                            \n")
    .append("LEFT OUTER JOIN iterusers u ON u.usrid = t.user           \n")
    .append("LEFT OUTER JOIN iterpaywall_product p ON p.id = t.product \n")
    .toString();
	
	private static String SQL_GROUP_CLAUSULE = "GROUP BY t.transactionid \n";
	
	public PaywallTransactionsExporter(HttpServletResponse response, String translatedColumns) throws ServiceError
	{
		ErrorRaiser.throwIfNull(response, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(translatedColumns, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		this.response = response;
		
		String[] headerPair = translatedColumns.split(StringPool.SEMICOLON);
        for (int i = 0; i < headerPair.length; i++)
        {
        	String[] header = headerPair[i].split(StringPool.EQUAL);
        	headerTranslationDictionary.put(header[0], header[1]);
        }
        
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_DATE)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_GATEWAY)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_TXID)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_TXCOMPLETED)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_USRID)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_USRNAME)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_PRODUCTID)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_PRODUCTNAME)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_AMOUNT)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        ErrorRaiser.throwIfFalse(Validator.isNotNull(headerTranslationDictionary.get(HEADER_DETAILS)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	}
	
	public void exportTransactions(long groupId, String whereClausule, String orderClausule, boolean extended) throws Throwable
	{
		this.extended = extended;
		
        try
        {
        	String query = new StringBuilder(extended ? SQL_EXPORT_EXTENDED_SELECT: SQL_EXPORT_SELECT)
        	.append(SQL_EXPORT_FROM)
        	.append(whereClausule)
        	.append(SQL_GROUP_CLAUSULE)
        	.append(orderClausule)
        	.toString();
        	
			PortalLocalServiceUtil.executeQueryAsResultSet(query, this, "initCSVFile", "writeCSVFile", "endCSVFile");
		}
		catch (Throwable th)
		{
			log.error("An error occurs while retrieving the csv row", th);
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/html");
			
			ServletOutputStream out = response.getOutputStream();
			out.write(StringPool.PERIOD.getBytes());
			out.flush();

			throw th;
		}
        finally
        {
        	ServletOutputStream csvout = response.getOutputStream();
    		csvout = response.getOutputStream();
    		csvout.flush();
        }
	}
	
	/**
	 * Codificacion del fichero CSV.
	 */
	final static private byte[] BOM_UTF8 = new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF};
	
	/**
	 * Inicializa el fichero CSV incluyendo la fila de cabecera.
	 */
	public void initCSVFile() throws IOException
	{
		// Inicializacion del fichero
		ServletOutputStream csvout = response.getOutputStream();
		response.setHeader("Content-Type", "text/csv");
	    response.setHeader("Content-Disposition", "attachment;filename=\"data.csv\"");
	    response.setContentType("application/csv");
		csvout.write( BOM_UTF8 );
		
		// Incluye la fila de cabecera.
		String csvHeader = getCSVHeader();
		csvout.write( csvHeader.getBytes() );
	}
	
	/**
	 * Recupera la fila del ResultSet y la incluye en el fichero CSV.
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public void writeCSVFile(IterRS resultset) throws IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		ServletOutputStream csvout = response.getOutputStream();
		csvout.write( new StringBuilder((String) resultset.getObject(1)).append(StringPool.RETURN_NEW_LINE).toString().getBytes() );
	}
	
	/**
	 * Vuelca el fichero CSV al response.
	 */
	public void endCSVFile() throws IOException
	{
		ServletOutputStream csvout = response.getOutputStream();
		csvout.flush();
	}
	
	private String getCSVHeader()
	{
		StringBuilder header = new StringBuilder();
		
		header.append(headerTranslationDictionary.get(HEADER_DATE)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_GATEWAY)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_TXID)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_TXCOMPLETED)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_USRID)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_USRNAME)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_PRODUCTID)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_PRODUCTNAME)).append(StringPool.SEMICOLON);
		header.append(headerTranslationDictionary.get(HEADER_AMOUNT)).append(StringPool.SEMICOLON);
		
		if (extended)
		{
			header.append(headerTranslationDictionary.get(HEADER_DETAILS)).append(StringPool.SEMICOLON);
		}
		
		header.append(StringPool.RETURN_NEW_LINE);
		
		return header.toString();
	}
}
