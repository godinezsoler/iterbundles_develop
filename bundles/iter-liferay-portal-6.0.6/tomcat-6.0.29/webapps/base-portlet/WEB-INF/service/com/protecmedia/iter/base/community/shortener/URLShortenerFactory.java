package com.protecmedia.iter.base.community.shortener;



public class URLShortenerFactory
{
	public static enum Shortener
	{
		BITLY;
	}
	
	// Previene instancación, inluidos ataques por reflection.
	private URLShortenerFactory() { throw new AssertionError(); }
	
	public static URLShortener getShortener(String shortener, long groupId)
	{
		URLShortener instance = null;
		try
		{
			Shortener s = Shortener.valueOf(shortener.toUpperCase());
			instance = getShortener(s, groupId);
		}
		catch (Throwable th)
		{
			// Do nothing
		}
		
		return instance;
	}
	
	// Instancia el acortador indicado con la configuración establecida en el grupo
	public static URLShortener getShortener(Shortener shortener, long groupId)
	{
		URLShortener urlShortener = null;
		
		switch (shortener)
		{
			case BITLY:
				urlShortener = new BitlyURLShortener(groupId);
		}
		
		return urlShortener;
	}
}
