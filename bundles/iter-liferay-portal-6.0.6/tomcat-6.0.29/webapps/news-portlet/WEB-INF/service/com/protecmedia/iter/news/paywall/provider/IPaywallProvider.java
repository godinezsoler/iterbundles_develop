package com.protecmedia.iter.news.paywall.provider;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonObject;
import com.liferay.portal.kernel.error.ServiceError;
import com.protecmedia.iter.news.paywall.model.PaywallProductModel;
import com.protecmedia.iter.news.paywall.model.PaywallTransactionModel;

public interface IPaywallProvider
{
	public void loadConfig(long groupId) throws ServiceError;
	public boolean validateConfiguration(JsonObject configuration);
	public abstract PaywallTransactionModel getTransactionDetails(long groupId, String transactionData) throws ServiceError;
	public abstract PaywallTransactionModel getTransacctionFromWebhook(long groupId, HttpServletRequest request) throws ServiceError;
	public abstract String getPaymentButtonCode(PaywallProductModel product) throws ServiceError;
}
