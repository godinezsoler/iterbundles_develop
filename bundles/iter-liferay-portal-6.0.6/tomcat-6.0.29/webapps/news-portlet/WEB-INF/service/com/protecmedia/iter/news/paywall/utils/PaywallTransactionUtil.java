package com.protecmedia.iter.news.paywall.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class PaywallTransactionUtil
{
	private static Log LOG = LogFactoryUtil.getLog(PaywallTransactionUtil.class);
			
	private static final List<String> FILTERABLE_COLUMNS;
	private static final List<String> SORTABLE_COLUMNS;
	private static final Map<String, String> SQL_COLUMNS;
	private static final Map<String, Map<String, String>> OPERATION;
    static
    {
    	List<String> filterables = new ArrayList<String>();
    	filterables.add("gateway");
    	filterables.add("transactionid");
    	filterables.add("date");
    	filterables.add("userid");
    	filterables.add("username");
    	filterables.add("productid");
    	filterables.add("productname");
    	filterables.add("amount");
    	FILTERABLE_COLUMNS = Collections.unmodifiableList(filterables);

    	List<String> sortables = new ArrayList<String>();
    	sortables.add("gateway");
    	sortables.add("date");
    	sortables.add("productname");
    	sortables.add("amount");
    	SORTABLE_COLUMNS = Collections.unmodifiableList(sortables);
    	
    	Map<String, String> sqlColumns = new HashMap<String, String>();
    	sqlColumns.put("gateway",       "t.gateway");
    	sqlColumns.put("transactionid", "t.transactionid");
    	sqlColumns.put("date",          "t.purchasedate");
    	sqlColumns.put("userid",        "t.user");
    	sqlColumns.put("username",      "u.usrname");
    	sqlColumns.put("productid",     "t.product");
    	sqlColumns.put("productname",   "p.pname");
    	sqlColumns.put("amount",        "t.amount");
    	SQL_COLUMNS = Collections.unmodifiableMap(sqlColumns);
    	
    	Map<String, Map<String, String>> operations = new HashMap<String, Map<String,String>>();
    	
        Map<String, String> stringOperations = new HashMap<String, String>();
        stringOperations.put("eq",       "= '%s'");
        stringOperations.put("dst",      "<> '%s'");
        stringOperations.put("contains", "like '%%%s%%'");
        stringOperations.put("starts",   "like '%%%s'");
        stringOperations.put("ends",     "like '%s%%'");

        Map<String, String> dateOperations = new HashMap<String, String>();
        dateOperations.put("eq",  "= '%s'");
        dateOperations.put("dst", "<> '%s'");
        dateOperations.put("gt",  "> '%s'");
        dateOperations.put("ge",  ">= '%s'");
        dateOperations.put("lt",  "< '%s'");
        dateOperations.put("le",  "<= '%s'");

        Map<String, String> numericOperations = new HashMap<String, String>();
        numericOperations.put("eq",  "= %s");
        numericOperations.put("dst", "<> %s");
        numericOperations.put("gt",  "> %s");
        numericOperations.put("ge",  ">= %s");
        numericOperations.put("lt",  "< %s");
        numericOperations.put("le",  "<= %s");
        
        operations.put("gateway", stringOperations);
        operations.put("transactionid", stringOperations);
        operations.put("date", dateOperations);
        operations.put("userid", stringOperations);
        operations.put("username", stringOperations);
        operations.put("productid", numericOperations);
        operations.put("productname", stringOperations);
        operations.put("amount", numericOperations);
        
        OPERATION = Collections.unmodifiableMap(operations);
    }
    
    private static final String SQL_GET_TRANSACTIONS_FROM = new StringBuilder()
    .append("FROM iterpaywall_transaction t                            \n")
    .append("LEFT OUTER JOIN iterusers u ON u.usrid = t.user           \n")
    .append("LEFT OUTER JOIN iterpaywall_product p ON p.id = t.product \n")
    .toString();
    
    private static final String SQL_GET_TRANSACTIONS_COUNT = new StringBuilder()
    .append("SELECT COUNT(*) total \n")
    .append(SQL_GET_TRANSACTIONS_FROM)
    .toString();
    
    private static final String SQL_GET_TRANSACTIONS_DATA = new StringBuilder()
    .append("SELECT t.id,                                              \n")
    .append("       t.purchasedate,                                    \n")
    .append("       t.gateway,                                         \n")
    .append("       t.transactionid,                                   \n")
    .append("       t.user,                                            \n")
    .append("       u.usrname,                                         \n")
    .append("       t.product,                                         \n")
    .append("       p.pname,                                           \n")
    .append("       t.amount                                           \n")
    .append(SQL_GET_TRANSACTIONS_FROM)
    .toString();
    
	public static String getTransactions(long groupId, String filters, String order, String pagination) throws ServiceError
	{
		JsonObject results = new JsonObject();
		JsonArray transactions = new JsonArray();
		long total = 0;
		
		String[] sql = buildSearchQueries(groupId, filters, order, pagination);
		
		try
		{
			// Calcula el total de registros
			Document c = PortalLocalServiceUtil.executeQueryAsDom(sql[0]);
			total = XMLHelper.getLongValueOf(c, "/rs/row/@total");
			
			// Recupera los registros indicados si al menos hay un registro, evitando realizar una consulta inecesaria si no hay registros.
			if (total > 0)
			{
				Document d = PortalLocalServiceUtil.executeQueryAsDom(sql[1]);
				List<Node> records = d.selectNodes("/rs/row");
				for (Node record : records)
				{
					JsonObject transaction = new JsonObject();
					transaction.addProperty("id", XMLHelper.getStringValueOf(record, "@id"));
					transaction.addProperty("date", XMLHelper.getStringValueOf(record, "@purchasedate"));
					
					JsonObject gateway = new JsonObject();
					gateway.addProperty("name", XMLHelper.getStringValueOf(record, "@gateway"));
					gateway.addProperty("transactionid", XMLHelper.getStringValueOf(record, "@transactionid"));
					transaction.add("gateway", gateway);
					
					JsonObject user = new JsonObject();
					user.addProperty("id", XMLHelper.getStringValueOf(record, "@user"));
					user.addProperty("username", XMLHelper.getStringValueOf(record, "@usrname"));
					transaction.add("user", user);
					
					JsonObject product = new JsonObject();
					product.addProperty("id", XMLHelper.getStringValueOf(record, "@product"));
					product.addProperty("name", XMLHelper.getStringValueOf(record, "@pname"));
					transaction.add("product", product);
					
					transaction.addProperty("amount", XMLHelper.getStringValueOf(record, "@amount"));
					
					transactions.add(transaction);
				}
			}
		}
		catch (Throwable th)
		{
			LOG.error(th);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		
		results.add("transactions", transactions);
		results.addProperty("total", total);
		return results.toString();
	}
	
	private static final String SQL_GET_TRANSACTION_DETAILS = "SELECT transactiondata FROM iterpaywall_transaction WHERE id = '%s'";
	
	public static String getTransactionsDetails(String transactionId) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(transactionId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		JsonObject results = new JsonObject();
		
		try
		{
			Document d = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_TRANSACTION_DETAILS, transactionId));
			String transactionData = XMLHelper.getStringValueOf(d.getRootElement(), "/rs/row/@transactiondata");
			results.addProperty("transactiondata", transactionData);
		}
		catch (Throwable th)
		{
			LOG.error(th);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		
		return results.toString();
	}
	
	public static void exportTransactions(HttpServletRequest request, HttpServletResponse response, String group, String filters, String order, String translatedColumns, String showDetails) throws Throwable
	{
		long groupId = Long.valueOf(group);
		String whereClausule = Validator.isNotNull(filters) ? processWhere(groupId, new JsonParser().parse(filters).getAsJsonArray()) : StringPool.BLANK;
		String orderClausule = Validator.isNotNull(order) ? processOrderBy(new JsonParser().parse(order).getAsJsonArray()) : StringPool.BLANK;
		boolean extended = GetterUtil.getBoolean(showDetails, false);
		
		new PaywallTransactionsExporter(response, translatedColumns)
		.exportTransactions(groupId, whereClausule, orderClausule, extended);
	}
	
	private static String[] buildSearchQueries(long groupId, String filters, String order, String pagination) throws ServiceError
	{
		String[] queries = new String[2];
		StringBuilder sql = new StringBuilder();
		
		if (Validator.isNotNull(filters))
			sql.append(processWhere(groupId, new JsonParser().parse(filters).getAsJsonArray()));
		
		sql.append(StringPool.NEW_LINE);

		queries[0] = new StringBuilder(SQL_GET_TRANSACTIONS_COUNT).append(sql).toString();
		
		if (Validator.isNotNull(order))
			sql.append(processOrderBy(new JsonParser().parse(order).getAsJsonArray()));
		
		sql.append(StringPool.NEW_LINE);
		
		if (Validator.isNotNull(pagination))
			sql.append(processPagination(new JsonParser().parse(pagination).getAsJsonObject()));
		
		queries[1] = new StringBuilder(SQL_GET_TRANSACTIONS_DATA).append(sql).toString();
		
		return queries;
	}

	private static final String WHERE = "WHERE t.groupid = %d";
	private static final String WHERE_TEMPLATE = "\n AND %s %s";
	
	private static String processWhere(long groupId, JsonArray jsonWhere) throws ServiceError
	{
		StringBuilder where = new StringBuilder(String.format(WHERE, groupId));
		
		if (Validator.isNotNull(jsonWhere))
		{
			for (JsonElement filter : jsonWhere)
			{
				String col = filter.getAsJsonObject().get("c").getAsString();
				ErrorRaiser.throwIfFalse(FILTERABLE_COLUMNS.contains(col), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				String op  = filter.getAsJsonObject().get("o").getAsString().toLowerCase();
				String val = filter.getAsJsonObject().get("v").getAsString();
				
				String sqlOp = OPERATION.get(col).get(op);
				String sqlFilter = String.format(WHERE_TEMPLATE, SQL_COLUMNS.get(col), String.format(sqlOp, val));
				
				where.append(sqlFilter);
			}
		}
		
		return where.toString();
	}
	
	private static final String ORDERBY = "ORDER BY ";
	private static final String ORDERBY_TEMPLATE = "%s %s";
	private static final String ORDERBY_SECURE_FIELD = ", t.id ASC";
	private static final String ORDERBY_DEFAULT = "ORDER BY t.purchasedate DESC" + ORDERBY_SECURE_FIELD;
	
	private static String processOrderBy(JsonArray jsonOrderby) throws ServiceError
	{
		StringBuilder orderby = new StringBuilder();
		
		if (Validator.isNotNull(jsonOrderby))
		{
			for (JsonElement filter : jsonOrderby)
			{
				String col = filter.getAsJsonObject().get("c").getAsString();
				ErrorRaiser.throwIfFalse(SORTABLE_COLUMNS.contains(col), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				String order  = filter.getAsJsonObject().get("o").getAsString().toLowerCase();
				
				if (orderby.length() > 0)
					orderby.append(StringPool.COMMA_AND_SPACE);
				
				orderby.append(String.format(ORDERBY_TEMPLATE, SQL_COLUMNS.get(col), order));
			}
		}
		
		if (orderby.length() > 0)
		{
			orderby.insert(0, ORDERBY).append(ORDERBY_SECURE_FIELD);
		}
		else
		{
			orderby = new StringBuilder(ORDERBY_DEFAULT);
		}
		
		return orderby.toString();
	}
	
	private static final String LIMIT = "LIMIT %d, %d";
	
	private static String processPagination(JsonElement jsonOrderby) throws ServiceError
	{
		StringBuilder pagination = new StringBuilder();
		
		if (Validator.isNotNull(jsonOrderby))
		{
			int from = jsonOrderby.getAsJsonObject().get("from").getAsInt();
			ErrorRaiser.throwIfFalse(from >= 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			int amount = jsonOrderby.getAsJsonObject().get("amount").getAsInt();
			ErrorRaiser.throwIfFalse(amount > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			pagination.append(String.format(LIMIT, from, amount));
		}
		
		return pagination.toString();
	}
}
