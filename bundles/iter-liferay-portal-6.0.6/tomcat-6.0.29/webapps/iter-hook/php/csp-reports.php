<?php
	if ( $HTTP_RAW_POST_DATA )
	{
		$json = json_decode($HTTP_RAW_POST_DATA);
		if ( $json )
		{
			$cspreport = $json->{'csp-report'};
			if ( $cspreport )
			{
				$logtext = "";
				
				foreach ( $cspreport as $key => $val )
				{
					switch ($key)
					{
						case "document-uri":
						case "referrer":
						case "blocked-uri":
						case "line-number":
						case "column-number":
						case "source-file":
							$logtext = $logtext . "$key: $val ";
							break;
					}
				}
				
				if ( strlen ( $logtext ) > 0 )
				{
					openlog("Iter HTTPS report", LOG_PID | LOG_ODELAY, LOG_LOCAL0);
						
					$exclusionList = array(
						"exc1" => "/blocked-uri:.+\/documents\//i",
						"exc2" => "/blocked-uri:.+\/binrepository\//i",
						"exc3" => "/blocked-uri:.+\/base-portlet\/webrsrc\//i",
						"exc4" => "/blocked-uri:.+\/news-portlet\//i",
						"exc5" => "/blocked-uri:.+\/tracking-portlet\//i",
						"exc6" => "/blocked-uri:.+\/embed.js /i",
						"exc7" => "/blocked-uri:.+\/c\/portal\/json_service/i"
					);
					
					// Aplica las exclusiones
					$excluded = false;
					foreach ( $exclusionList as $pattern )
					{
						if ( preg_match($pattern, $logtext) )
						{
							$excluded = true;
							break;
						}
					}
					
					if ( !$excluded )
					{
						syslog(LOG_WARNING, "${logtext}");
					}
				}
			}
		}
	}
?>