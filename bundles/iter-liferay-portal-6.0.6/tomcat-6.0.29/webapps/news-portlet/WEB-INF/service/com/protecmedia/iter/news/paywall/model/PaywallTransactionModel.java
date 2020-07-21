package com.protecmedia.iter.news.paywall.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.news.paywall.utils.PaywallErrorKeys;
import com.protecmedia.iter.news.paywall.utils.PaywallGateway;
import com.protecmedia.iter.news.service.ProductLocalServiceUtil;

public class PaywallTransactionModel
{
	private static Log log = LogFactoryUtil.getLog(PaywallTransactionModel.class);
	
	private long groupid;
	private PaywallGateway gateway;
	private String id;
	private String userid;
	private long productid;
	private String rawData;
	private Date date;
	private BigDecimal amount;
	private String status;
	private boolean completed;
	private String errorUrl;

	private PaywallProductModel product;
	
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//                                       CONSTRUCTOR                                      //
	////////////////////////////////////////////////////////////////////////////////////////////
	public static class Builder
	{
		// Required parameters
		private long groupid;
		private PaywallGateway gateway;
		private String id;
		private String rawData;
		
		// Optional parameters
		private String userid;
		private long productid;
		private Date date;
		private BigDecimal amount;
		private String status;
		private boolean completed;
		private String errorUrl;
		
		public Builder(long groupid, PaywallGateway gateway, String id, String rawData)
		{
			this.groupid = groupid;
		    this.gateway   = gateway;
		    this.id        = id;
		    this.rawData   = rawData;
		}

		public Builder userid(String userid)        { this.userid = userid; return this; }
		public Builder productid(long productid)    { this.productid = productid; return this; }
		public Builder date(Date date)              { this.date = date; return this; }
		public Builder amount(BigDecimal amount)    { this.amount = amount; return this; }
		public Builder status(String status)        { this.status = status; return this; }
		public Builder completed(boolean completed) { this.completed = completed; return this; }
		public Builder errorUrl(String errorUrl)    { this.errorUrl = errorUrl; return this; }
		public PaywallTransactionModel build() throws ServiceError   { return new PaywallTransactionModel(this); }
	}
	
	private PaywallTransactionModel(Builder builder) throws ServiceError
	{	
		// Asigna el valor a los atributos
		this.groupid   = builder.groupid;
		this.gateway   = builder.gateway;
		this.id        = builder.id;
	    this.userid    = builder.userid;
	    this.productid = builder.productid;
		this.rawData   = builder.rawData;
		this.date      = builder.date;
		this.amount    = builder.amount;
		this.status    = builder.status;
		this.completed = builder.completed;
		this.errorUrl    = builder.errorUrl;
		
		if (productid > 0)
		{
			try
			{
				Document d = ProductLocalServiceUtil.getPaywallProduct(productid);
				product = new PaywallProductModel(d.selectSingleNode("/rs/row"));
			}
			catch (Throwable th)
			{
				log.error(th);
				ErrorRaiser.throwIfError(PaywallErrorKeys.PAYWALL_E_PRODUCT_NOT_CONFIGURED);
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//                                         LOGICA                                         //
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public PaywallTransactionModel(PaywallGateway gateway, String id, String rawData)
	{
		this.gateway = gateway;
		this.id = id;
		this.rawData = rawData;
	}
	
	public String getId() { return id; }
	public String getUser() { return userid; }
	public PaywallProductModel getProduct() { return product; }
	public String getStatus() { return status; }
	public boolean isCompleted() { return completed; }
	public String getErrorUrl() {
		return errorUrl
			+ StringPool.QUESTION + "gateway=" + gateway.toString().toLowerCase()
			+ StringPool.AMPERSAND + "transaction=" + id
			+ StringPool.AMPERSAND + "status=" + status;
	}
	
	public BigDecimal getProductPrice()
	{
		return amount;
	}
	
	
	private static final String SQL_INSERT_TRANSACTION = new StringBuilder()
	.append("INSERT INTO iterpaywall_transaction (id, groupid, gateway, transactionid, completed, user, product, amount, purchasedate, transactiondata) \n")
	.append("VALUES (UUID(), %d, '%s', '%s', %b, '%s', %d, %s, '%s', '%s')")
	.toString();
	
	private static final DateFormat SQL_DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public boolean save() throws ServiceError
	{
		boolean saved = false;
		
		String sql = String.format(SQL_INSERT_TRANSACTION,
			groupid,
			gateway.toString(),
			id,
			completed,
			userid,
			productid,
			amount.toString(),
			SQL_DF.format(date),
			StringEscapeUtils.escapeSql(rawData)
		);
		
		try
		{
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			saved = true;
		}
		catch (Throwable th)
		{
			if (th instanceof ORMException)
			{
				String errorCode = ServiceErrorUtil.getErrorCode((ORMException) th);
				if ("XYZ_ITR_UNQ_ITERPAYWALLTRANSACTION_TRANSACTIONID_ZYX".equals(errorCode))
					log.info("Paywall transaction " + id + " already registered");
				else
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			}
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		
		return saved;
	}
}
