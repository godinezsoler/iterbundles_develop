function loadJS(u){var r = document.getElementsByTagName( "script" )[ 0 ], s = document.createElement( "script" );s.src = u;r.parentNode.insertBefore( s, r );}

if(!window.HTMLPictureElement)
{
                document.createElement('picture');
                loadJS("/html/js/responsive/respimage.min.js");
}