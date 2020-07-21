<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="labelbefore">
		<xsl:value-of disable-output-escaping="yes" select="."/>
	</xsl:template>

	<xsl:template match="labelafter">
		<xsl:value-of disable-output-escaping="yes" select="."/>
	</xsl:template>
</xsl:stylesheet>