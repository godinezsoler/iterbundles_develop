package com.protecmedia.iter.news.paywall.utils;

public enum PaywallGateway
{
	PAYPAL, REDSYS;
	
	public static PaywallGateway get(String name)
	{
		return PaywallGateway.valueOf(name.toUpperCase());
	}
}
