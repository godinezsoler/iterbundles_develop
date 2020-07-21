<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:template match="*"/>

	<xsl:template match="/*">
		<xsl:copy>
			<xsl:call-template name="buildRsrcList"/>
			<xsl:call-template name="buildLayoutRsrc"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="buildRsrcList">
		<xsl:element name="rsrc-list">
			<xsl:for-each select="rsrc-list/rsrc">
				<xsl:element name="rsrc">
					<xsl:attribute name="md5"><xsl:value-of select="@md5"/></xsl:attribute>
					<xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
					<xsl:attribute name="kind"><xsl:value-of select="@kind"/></xsl:attribute>
					<xsl:attribute name="async"><xsl:value-of select="@async"/></xsl:attribute>
					<xsl:element name="content"><xsl:value-of select="content"/></xsl:element>
					<xsl:if test="./jscallback/text() != ''">
						<xsl:element name="jscallback"><xsl:value-of select="jscallback"/></xsl:element>
					</xsl:if>
				</xsl:element>
			</xsl:for-each>
		</xsl:element>
	</xsl:template>

	<xsl:template name="buildLayoutRsrc">
		<xsl:element name="layout-rsrc">
			<xsl:attribute name="globalids">true</xsl:attribute>
			<xsl:for-each select="layout-rsrc/itm">
				<xsl:element name="itm">
					<xsl:attribute name="themeid"><xsl:value-of select="@themeid"/></xsl:attribute>
					<xsl:choose>
						<xsl:when test="./@id != ''">
							<xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
							<xsl:attribute name="class">layout</xsl:attribute>
						</xsl:when>	
						<xsl:otherwise>	
							<xsl:attribute name="class">default</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:attribute name="rsrc"><xsl:value-of select="@rsrc"/></xsl:attribute>
					<xsl:if test="./@place != ''">
						<xsl:attribute name="place"><xsl:value-of select="@place"/></xsl:attribute>
					</xsl:if>
				</xsl:element>
			</xsl:for-each>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>