package com.protecmedia.iter.user.util.social;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

public class SocialStatisticsQuotaController
{
	private static Log _log = LogFactoryUtil.getLog(SocialStatisticsQuotaController.class);
	

	private Calendar[] previousTimeReset;
	private Integer[] counter;
	//entendiendo que no puede haber dos quotas diferentes para el mismo periodod de tiempo
	private List<Entry<Long, Integer>> quotaTime_quotaRequests;
	
	
	/**
	 * 	
	 * @param quotaRequests
	 * 	num of requests in quota time
	 * @param quotaTime
	 * 	ms 
	 */
	public SocialStatisticsQuotaController(String quotasStr)
	{
		if(Validator.isNotNull(quotasStr))
		{
			String[] quotas = quotasStr.split(","); 
			int numCuotas = quotas.length;
			
			this.previousTimeReset = new Calendar[numCuotas];
			this.counter = new Integer[numCuotas];
			this.quotaTime_quotaRequests = new ArrayList<Map.Entry<Long,Integer>>();
			
			Map<Long, Integer> quotaTime_quotaRequestsMap = new HashMap<Long, Integer>();
			for(int index = 0; index < quotas.length; index++)
			{
				String quota = quotas[index];
				quotaTime_quotaRequestsMap.put(Long.parseLong(quota.substring(quota.indexOf("/")+1, quota.length())), Integer.parseInt(quota.substring(0, quota.indexOf("/"))));
				this.counter[index] = 0;
				this.previousTimeReset[index] = null;
				
			}
			this.quotaTime_quotaRequests.addAll(quotaTime_quotaRequestsMap.entrySet());
		}
	}
	
	public int getIndexBlockedQuota(){
		int index = -1;
		if(quotaTime_quotaRequests != null)
		{
			synchronized (counter)
			{
				if((index = compareRequests()) < 0){
					plusCounters();
				}
			}
		}
		return index;
		
	}
	
	private int compareRequests(){
		int result = -1;
		if(Validator.isNotNull(quotaTime_quotaRequests) && Validator.isNotNull(previousTimeReset))
		{
			for(int index = 0; index < quotaTime_quotaRequests.size() && index < previousTimeReset.length; index++)
			{
				Entry<Long, Integer> quota = quotaTime_quotaRequests.get(index);
				if(Validator.isNotNull(quota)){
					if(Validator.isNull(previousTimeReset[index])	|| Calendar.getInstance().getTimeInMillis() - previousTimeReset[index].getTimeInMillis() > quota.getKey() )
					{
						_log.trace("reset quota " + index);
						counter[index]  = 0;
						previousTimeReset[index] = Calendar.getInstance();
					}

					if(quota.getValue() <= counter[index]){
						_log.debug("quota " + index + ": " + quota.getValue() + " < counter " + counter[index]);
						result = index;
					}
				}
			}
		}
		return result;
	}
	
	private void plusCounters(){
		if(quotaTime_quotaRequests != null)
		{
			for(int index = 0; index < quotaTime_quotaRequests.size() && index < previousTimeReset.length; index++)
			{
				if(previousTimeReset[index] == null )
					counter[index] =  0;

				counter[index]++;
				
				_log.trace("plus counter: " + index + ", value: " + counter[index]);
			}
		}
	}
	
	public Long getTimeToSleep(int index){
		long time = 0;
		if(quotaTime_quotaRequests != null && quotaTime_quotaRequests.get(index) != null && previousTimeReset[index] != null)
		{
			time = previousTimeReset[index].getTimeInMillis() + quotaTime_quotaRequests.get(index).getKey() - Calendar.getInstance().getTimeInMillis();
			_log.trace("quota  " + index + " exceeded, time to sleep: " + time);
		}
		return time;
	}
	
}
