package com.protecmedia.iter.base.community.shortener;

public interface URLShortener
{
	public boolean isConfigured();
	public String shorten(String longUrl, String articleId);
}
