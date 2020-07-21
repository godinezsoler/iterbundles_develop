<?php

function getDomain($url)
{
    $nowww = ereg_replace('www\.','',$url);
    $info = parse_url($nowww);
    $host = $info['path'];
    $host_names = explode(".", $host);
    return $host_names[count($host_names)-2] . "." . $host_names[count($host_names)-1];
}


// Coge la URL de retorno
$origin = $_GET['origin'];

// Coge las redirecciones
$ssopartners = $_GET['ssopartners'];
		
// Coge la cookie de usuario
$sessioncookie  = isset ( $_GET["sessioncookie"] ) ? $_GET["sessioncookie"] : "";
$sessionexpires = $_GET["sessionexpires"];

// Coge el userId
$userid = $_GET['userid'];

// Calcula el dominio
$domain = getDomain($_SERVER[HTTP_HOST]);

// Pone / quita la cookie del sitio
if ($sessioncookie !== '')
{
	setcookie("USER-DOGTAG", 	   $sessioncookie, 	($sessionexpires > 0) ? $sessionexpires + time() : $sessionexpires, '/', $domain, false, true);
	setcookie("ITR_COOKIE_VSTRID", $userid, 		($sessionexpires > 0) ? $sessionexpires + time() : $sessionexpires, '/', $domain, false, true);
}
else
{
	setcookie("USER-DOGTAG", 		"", -1, '/', $domain);
	setcookie("ITR_COOKIE_VSTRID", 	"", -1, '/', $domain);
}


// Recupera todas las redirecciones
$urls = explode(",", $ssopartners);

// Elimina la actual
$current = strtok( (isset($_SERVER['HTTPS']) ? "https" : "http") . "://$_SERVER[HTTP_HOST]", '?');

// Se queda con la siguiente redireccion
$redirectUrl = $urls[0];

// La elimina del listado
unset($urls [0]);
$ssopartnersParam = implode(",", $urls);

if ($redirectUrl !== '')
{
	$encodedCookie = urlencode( $sessioncookie );
	header("Location: $redirectUrl/restapi/user/impersonate?sessioncookie=$encodedCookie&sessionexpires=$sessionexpires&userid=$userid&origin=$origin&ssopartners=$ssopartnersParam");
}
else
{
	header("Location: $origin");
}

?>