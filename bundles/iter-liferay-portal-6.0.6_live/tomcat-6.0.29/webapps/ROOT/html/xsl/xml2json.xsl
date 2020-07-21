<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<!-- Turn off auto-insertion of <?xml> tag and set indenting on -->
	<xsl:output method="text" encoding="utf-8" indent="yes"/>

	<!-- strip whitespace from whitespace-only nodes -->
	<xsl:strip-space elements="*"/>

	<!-- create a key for every element in the document using its name -->
	<xsl:key name="names" match="*" use="concat(generate-id(..),'/',local-name())"/>

	<!-- start with the root element -->
	<xsl:template match="/">
		<!-- first element needs brackets around it as template does not do that -->
		<xsl:text>{ </xsl:text>
		<!-- call the template for elements using one unique name at a time -->
		<xsl:apply-templates select="*[generate-id(.) = generate-id(key('names', concat(generate-id(..),'/',local-name()))[1])]">
		<!--	<xsl:sort select="local-name()"/>-->
		</xsl:apply-templates>
		<xsl:text> }</xsl:text>
	</xsl:template>

	<!-- this template handles elements -->
	<xsl:template match="*">

		<!-- count the number of elements with the same name -->
		<xsl:variable name="kctr" select="count(key('names', concat(generate-id(..),'/',local-name())))"/>

		<!-- iterate through by sets of elements with same name -->
		<xsl:for-each select="key('names', concat(generate-id(..),'/',local-name()))">
			<!-- deal with the element name and start of multiple element block -->
			<xsl:choose>
				<xsl:when test="($kctr > 1) and (position() = 1)">
					<xsl:text>"</xsl:text>
					<xsl:value-of select="local-name()"/>
					<xsl:text>" : [ </xsl:text>
				</xsl:when>
				<xsl:when test="$kctr = 1">
					<xsl:text>"</xsl:text>
					<xsl:value-of select="local-name()"/>
					<xsl:text>" : </xsl:text>
				</xsl:when>
			</xsl:choose>
			<!-- count number of elements, text nodes and attribute nodes -->
			<xsl:variable name="ctr" select="count(*)"/>
			<xsl:variable name="tctr" select="count(text())"/>
			<xsl:variable name="actr" select="count(@*)"/>

			<xsl:choose>
				<xsl:when test="($ctr = 0) and ($tctr = 0) and ($actr = 0)">
					<!-- no contents at all -->
					<xsl:text>""</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					
					<!-- there will be contents so start an object -->
					<xsl:if test="($ctr > 0) or ($actr > 0)">
						<xsl:text>{ </xsl:text>
					</xsl:if>

					<!-- handle attribute nodes -->
					<xsl:if test="$actr &gt; 0">
						<xsl:apply-templates select="@*"/>
						<xsl:if test="($tctr &gt; 0) or ($ctr &gt; 0)">
							<xsl:text>, </xsl:text>
						</xsl:if>
					</xsl:if>
					<!-- handle text nodes -->
					<xsl:if test="$tctr &gt; 0">
						<xsl:if test="($ctr > 0) or ($actr > 0)">
							<xsl:text>"__text" : </xsl:text>
						</xsl:if>
						<xsl:text>"</xsl:text>
						<xsl:apply-templates select="text()"/>
						<xsl:text>"</xsl:text>
					</xsl:if>

					<!-- call template for child elements one unique name at a time -->
					<xsl:if test="$ctr &gt; 0">
						<xsl:if test="$tctr &gt; 0">
							<xsl:text>, </xsl:text>
						</xsl:if>

						<xsl:apply-templates select="*[generate-id(.) = generate-id(key('names', concat(generate-id(..),'/',local-name()))[1])]">
							<!-- <xsl:sort select="local-name()"/> -->
						</xsl:apply-templates>
					</xsl:if>
					<xsl:if test="($ctr > 0) or ($actr > 0)">
						<xsl:text> }</xsl:text>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>

			<xsl:if test="$kctr &gt; 1">
				<xsl:choose>
					<xsl:when test="position() = last()">
						<xsl:text> ]</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>, </xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:for-each>
		<xsl:if test="position() != last()">
			<xsl:text>, </xsl:text>
		</xsl:if>
	</xsl:template>

	<!-- this template handle text nodes -->
	<xsl:template match="text()">
		<xsl:call-template name="escapeQuote"/>
	</xsl:template>

	<!-- this template handles attribute nodes -->
	<xsl:template match="@*">
		<!-- attach prefix to attribute names -->
		<xsl:text>"</xsl:text>
		<xsl:value-of select="local-name()"/>
		<xsl:text>" : "</xsl:text>
		<xsl:call-template name="escapeQuote"/>
		<xsl:text>"</xsl:text>

		<xsl:if test="position() != last()">
			<xsl:text>, </xsl:text>
		</xsl:if>
	</xsl:template>

	<!-- Es necesario sustituir las comillas tanto de los nodos texto como del contenido de los atributos --> 
	<xsl:template name="escapeQuote">
		<xsl:param name="canTrim" select="1"/>
		<xsl:param name="pText" select="."/>

		<xsl:variable name="trimmed">
			<xsl:choose>
				<xsl:when test="canTrim = 1">
					<xsl:call-template name="string-trim">
						<xsl:with-param name="string" select="$pText"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$pText"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:if test="string-length($trimmed) &gt;0">
			<xsl:variable name="escapedText">
				<xsl:value-of select="substring-before(concat($trimmed, '&quot;'), '&quot;')"/>

				<xsl:if test="contains($trimmed, '&quot;')">
					<xsl:text>\"</xsl:text>

					<xsl:call-template name="escapeQuote">
						<xsl:with-param name="canTrim" select="0"/>
						<xsl:with-param name="pText" select="substring-after($trimmed, '&quot;')"/>
					</xsl:call-template>
				</xsl:if>
			</xsl:variable>

			<!-- noLFtext contendrá el texto escapado (sin quotes) y sin LF --> 
			<xsl:variable name="noLFtext">
				<xsl:call-template name="string-replace-all">
					<xsl:with-param name="text" select="$escapedText"/>
					<xsl:with-param name="replace" select="concat('', '&#xA;')"/>
					<xsl:with-param name="by"/>
				</xsl:call-template>
			</xsl:variable>

			<!-- Sustituimos los CR dejamos resultado en la salida --> 
			<xsl:call-template name="string-replace-all">
				<xsl:with-param name="text" select="$noLFtext"/>
				<xsl:with-param name="replace" select="concat('', '&#xD;')"/>
				<xsl:with-param name="by"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<!-- Strips trailing whitespace characters from 'string' -->
	<xsl:variable name="whitespace" select="'&#x9;&#xA;&#xD; '"/>
	<xsl:template name="string-rtrim">
		<xsl:param name="string"/>
		<xsl:param name="trim" select="$whitespace"/>

		<xsl:variable name="length" select="string-length($string)"/>

		<xsl:if test="$length &gt; 0">
			<xsl:choose>
				<xsl:when test="contains($trim, substring($string, $length, 1))">
					<xsl:call-template name="string-rtrim">
						<xsl:with-param name="string" select="substring($string, 1, $length - 1)"/>
						<xsl:with-param name="trim" select="$trim"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$string"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<!-- Strips leading whitespace characters from 'string' -->
	<xsl:template name="string-ltrim">
		<xsl:param name="string"/>
		<xsl:param name="trim" select="$whitespace"/>

		<xsl:if test="string-length($string) &gt; 0">
			<xsl:choose>
				<xsl:when test="contains($trim, substring($string, 1, 1))">
					<xsl:call-template name="string-ltrim">
						<xsl:with-param name="string" select="substring($string, 2)"/>
						<xsl:with-param name="trim" select="$trim"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$string"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<!-- Strips leading and trailing whitespace characters from 'string' -->
	<xsl:template name="string-trim">
		<xsl:param name="string"/>
		<xsl:param name="trim" select="$whitespace"/>
		<xsl:call-template name="string-rtrim">
			<xsl:with-param name="string">
				<xsl:call-template name="string-ltrim">
					<xsl:with-param name="string" select="$string"/>
					<xsl:with-param name="trim" select="$trim"/>
				</xsl:call-template>
			</xsl:with-param>
			<xsl:with-param name="trim" select="$trim"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="string-replace-all">
		<xsl:param name="text" />
		<xsl:param name="replace" />
		<xsl:param name="by" />
		<xsl:choose>
			<xsl:when test="$text = '' or $replace = ''or not($replace)" >
				<!-- Prevent this routine from hanging -->
				<xsl:value-of select="$text" />
			</xsl:when>
			<xsl:when test="contains($text, $replace)">
				<xsl:value-of select="substring-before($text,$replace)" />
				<xsl:value-of select="$by" />
				<xsl:call-template name="string-replace-all">
					<xsl:with-param name="text" select="substring-after($text,$replace)" />
					<xsl:with-param name="replace" select="$replace" />
					<xsl:with-param name="by" select="$by" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>