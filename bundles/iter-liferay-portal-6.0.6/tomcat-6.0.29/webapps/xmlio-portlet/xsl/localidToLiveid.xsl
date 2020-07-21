<?xml version='1.0'?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="@siteglobalid">
	
</xsl:template>

<xsl:template match="@sitelocalid">
	<xsl:attribute name="siteliveid">
		<xsl:value-of select="."/>
	</xsl:attribute>
</xsl:template>

<xsl:template match="@localId">
	<xsl:attribute name="liveid">
		<xsl:value-of select="."/>
	</xsl:attribute>
</xsl:template>


<xsl:template match="@*|node()">
	<xsl:copy>
		<xsl:apply-templates select="@*|node()"/>
	</xsl:copy>
</xsl:template>

</xsl:stylesheet>