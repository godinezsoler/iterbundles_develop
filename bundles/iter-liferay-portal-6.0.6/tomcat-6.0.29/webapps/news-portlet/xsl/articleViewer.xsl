<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

	<xsl:param name="articleid"/>
	<xsl:param name="groupid"/>
	<xsl:param name="url"/>

	<xsl:template match="/root">
		<article>
			<xsl:attribute name="articleid">
				<xsl:value-of select="$articleid"/>
			</xsl:attribute>
			<xsl:attribute name="groupid">
				<xsl:value-of select="$groupid"/>
			</xsl:attribute>
			<xsl:apply-templates select="metadata"/>
			<xsl:apply-templates select="relatedcontent"/>
			<content>
				<xsl:apply-templates select="./dynamic-element"/>
			</content>
		</article>
	</xsl:template>

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="dynamic-element">
		<xsl:if test="@name!='ExternalLink' and @name!='InternalLink'">
			<xsl:element name="component">
				<xsl:attribute name="name">
					<xsl:value-of select="@name"/>
				</xsl:attribute>
				
				<xsl:if test="@name='Question' and @questionid">
					<xsl:attribute name="questionid">
						<xsl:value-of select="@questionid"/>
					</xsl:attribute>
					<xsl:if test="@opendate">
						<xsl:attribute name="opendate">
							<xsl:value-of select="@opendate"/>
						</xsl:attribute>
					</xsl:if>
					<xsl:if test="@closedate">
						<xsl:attribute name="closedate">
							<xsl:value-of select="@closedate"/>
						</xsl:attribute>
					</xsl:if>
				</xsl:if>
				
				<xsl:choose>
					<xsl:when test="not(./dynamic-element[@type='document_library'])">
						<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
						<xsl:value-of select="dynamic-content/text()" disable-output-escaping="yes"/>
						<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="count(./dynamic-element[@name='Extension'])=0">
								<xsl:for-each select="./dynamic-element[@type='document_library']">
									<xsl:element name="remoteContent">
										<xsl:attribute name="href">
											<xsl:value-of select="concat($url, dynamic-content/text())" disable-output-escaping="yes"/>
										</xsl:attribute>
										<xsl:attribute name="rendition">
											<xsl:value-of select="@name"/>
										</xsl:attribute>
									</xsl:element>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<xsl:element name="remoteContent">
									<xsl:attribute name="href">
										<xsl:value-of select="concat($url, ./dynamic-element[@name='Document']/dynamic-content/text())" disable-output-escaping="yes"/>
									</xsl:attribute>
									<xsl:attribute name="preview">
										<xsl:value-of select="concat($url, ./dynamic-element[@name='Preview']/dynamic-content/text())" disable-output-escaping="yes"/>
									</xsl:attribute>
								</xsl:element>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:for-each select="./dynamic-element">
					<xsl:if test="@type!='document_library' and @name!='Milenium' and @name!='Extension'">
						<xsl:element name="component">
							<xsl:attribute name="name">
								<xsl:value-of select="@name"/>
							</xsl:attribute>
				
							<xsl:if test="@name='Answer'">
								<xsl:if test="@choiceid">
									<xsl:attribute name="choiceid">
										<xsl:value-of select="@choiceid"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:if test="@votes">
									<xsl:attribute name="votes">
										<xsl:value-of select="@votes"/>
									</xsl:attribute>
								</xsl:if>
							</xsl:if>
							
							<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
							<xsl:value-of select="./dynamic-content/text()" disable-output-escaping="yes"/>
							<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
						</xsl:element>
					</xsl:if>
				</xsl:for-each>
			</xsl:element>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>