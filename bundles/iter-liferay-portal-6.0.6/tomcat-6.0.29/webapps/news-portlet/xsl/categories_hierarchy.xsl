<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				version="1.0">
<xsl:output method="xml" indent="yes"/>

	<xsl:template match="*"/>

	<xsl:template match="/*">
		<xsl:copy>
			<xsl:call-template name="buildHierarchy">
				<xsl:with-param name="parentVocId" select="0"/>
				<xsl:with-param name="parentCatId" select="0"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="buildHierarchy">
		<xsl:param name="parentVocId"/>
		<xsl:param name="parentCatId"/>
		<xsl:for-each select="//row[@parentVocId=$parentVocId and @parentCatId=$parentCatId]">

			<xsl:element name="{name(.)}">

				<xsl:for-each select="@*[name(.) != 'parentCatId' and name(.) != 'parentVocId' and name(.) != 'children']">
					<!-- Se descartan los atributos parentCatId y parentVocId -->
					<xsl:attribute name='{name(.)}'><xsl:value-of select="string(.)"/></xsl:attribute>
				</xsl:for-each>

				<xsl:attribute name="selected">0</xsl:attribute>
				<xsl:if test="@children != '0'">
					<xsl:choose>
						<xsl:when test="@type='vocabulary'">
							<xsl:call-template name="buildHierarchy">
							<xsl:with-param name="parentVocId" select="@id"/>
							<xsl:with-param name="parentCatId" select="0"/>
							</xsl:call-template>
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="buildHierarchy">
							<xsl:with-param name="parentVocId" select="@parentVocId"/>
							<xsl:with-param name="parentCatId" select="@id"/>
							</xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
			</xsl:element>
		</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>