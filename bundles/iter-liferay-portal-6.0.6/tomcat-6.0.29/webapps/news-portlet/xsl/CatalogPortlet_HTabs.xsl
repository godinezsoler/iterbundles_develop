<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" version="1.0" encoding="utf-8" omit-xml-declaration="yes"/>

<xsl:template match="/">
	<xsl:variable name="portletId" select="/rs/@portletid"/>
			<script>
				$(function() 
				{
					<xsl:text>jQryIter( "#</xsl:text><xsl:value-of select="$portletId"/><xsl:text>" ).tabs();</xsl:text>

				});
			</script>

			<div id="{$portletId}" class="iter-tabs iter-tabs-horizontal">
				<div class="iter-tabs-leftColumn"></div>
					<ul>
					<xsl:for-each select="//tab">
						<li>
							<a>
								<xsl:attribute name="href">#<xsl:value-of select="@elementid"/></xsl:attribute>
								<xsl:value-of select="title" disable-output-escaping="yes"/>
							</a>
						</li>
					</xsl:for-each>
					</ul>
					<xsl:apply-templates select="//tab"/>
			</div>
</xsl:template>

<xsl:template match="tab">	
	<div>
		<xsl:attribute name="id"><xsl:value-of select="@elementid"/></xsl:attribute>

		<div class="iter-tab-content"><xsl:value-of select="html" disable-output-escaping="yes"/></div>
	</div>
</xsl:template>
</xsl:stylesheet>