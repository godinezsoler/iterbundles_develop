<script language="javaScript" type="text/javascript">
	var newLocation = "${mobileurl}";
	if ( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|OperaMini/i.test(navigator.userAgent) && newLocation != window.location.pathname)
	{
		window.location.href = newLocation;
	}
</script>


