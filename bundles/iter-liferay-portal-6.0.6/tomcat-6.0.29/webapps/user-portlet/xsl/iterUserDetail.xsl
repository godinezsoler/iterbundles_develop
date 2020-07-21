<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" encoding="UTF-8" indent="yes"/>
	<xsl:template match="formdata">
		<html>
			<head></head>
			<body>
				<xsl:for-each select="fieldsgroup">
					<h1>
						<xsl:value-of select="@name"/>
					</h1>
					<hr/>
					<xsl:variable name="numFields" select="count(field)"/>
					<xsl:if test="$numFields &gt; 0">
						<blockquote>
							<xsl:for-each select="field">
								<xsl:choose>
									<xsl:when test="data/binary">
										<p>
											<strong>
												<xsl:value-of select="labelbefore"/>
											</strong>&#xA0;
											<a target="_blank">
												<xsl:attribute name="href"><xsl:value-of select="data/binary/binlocator"/></xsl:attribute>
												<xsl:value-of select="data/binary/name"/>
											</a>
											<strong>
												<xsl:value-of select="labelafter"/>
											</strong>
										</p>
									</xsl:when>
									<xsl:otherwise>
										<xsl:variable name="numValues" select="count(data/value)"/>
										<xsl:choose>
											<xsl:when test="$numValues &gt; 1">
												<p>
													<strong>
														<xsl:value-of select="labelbefore"/>
													</strong>&#xA0;
													<ul>
														<xsl:for-each select="data/value">
															<li><xsl:value-of select="."/></li>
														</xsl:for-each>
													</ul>
												</p>
											</xsl:when>
											<xsl:otherwise>
												<p>
													<strong>
														<xsl:value-of select="labelbefore"/>
													</strong>&#xA0;<xsl:value-of select="data/value"/>
													<strong>
														<xsl:value-of select="labelafter"/>
													</strong>
												</p>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</blockquote>
					</xsl:if>
				</xsl:for-each>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
