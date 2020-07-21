<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes"/>
	<xsl:param name="globalgroupid"/>
	<xsl:template match="/">
		<root>
			<xsl:apply-templates/>
		</root>
	</xsl:template>
	<!-- Buscamos el nodo content y recorremos todos sus nodos component -->
	<xsl:template match="content">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="component">
		<xsl:element name="dynamic-element">
			<xsl:attribute name="name">
				<xsl:value-of select="@name"/>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:text>text</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="index-type">
				<xsl:if test="(@index=1) or (@index='true')">
					<xsl:text>text</xsl:text>
				</xsl:if>
			</xsl:attribute>
			<xsl:if test="@sites != ''">
				<xsl:attribute name="groups">
					<xsl:value-of select="@sites"/>
				</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="@name='Question'">
				<xsl:if test="@questionid != ''">
					<xsl:attribute name="questionid">
						<xsl:value-of select="@questionid"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="@opendate != ''">
					<xsl:attribute name="opendate">
						<xsl:value-of select="@opendate"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="@closedate != ''">
					<xsl:attribute name="closedate">
						<xsl:value-of select="@closedate"/>
					</xsl:attribute>
				</xsl:if>
			</xsl:if>
			
			<xsl:if test="@name='Answer'">
				<xsl:if test="@choiceid != ''">
					<xsl:attribute name="choiceid">
						<xsl:value-of select="@choiceid"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="@votes != ''">
					<xsl:attribute name="votes">
						<xsl:value-of select="@votes"/>
					</xsl:attribute>
				</xsl:if>
			</xsl:if>
			
			<xsl:if test="not (@name='InternalLink' or @name='ExternalLink' or @name='Class' or @name='Link' or @name='ContentId')">
				<xsl:element name="dynamic-element">
					<xsl:attribute name="index-type">
						<xsl:text></xsl:text>
					</xsl:attribute>
					<xsl:attribute name="name">
						<xsl:text>Milenium</xsl:text>
					</xsl:attribute>
					<xsl:attribute name="type">
						<xsl:text>text</xsl:text>
					</xsl:attribute>
					<xsl:element name="dynamic-content">
						<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
							<xsl:text>XYZ_ITR_ZYX;</xsl:text>
							<xsl:text>order=</xsl:text>
							<xsl:value-of select="format-number(count(preceding-sibling::component)+1,'.0')" disable-output-escaping="yes"/>
						<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
					</xsl:element>
					
				</xsl:element>
			</xsl:if>
			<xsl:element name="dynamic-content"> <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text><xsl:value-of select="normalize-space(text())" disable-output-escaping="yes"/> <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text></xsl:element>
			<xsl:choose>
				<xsl:when test="file">
					<xsl:choose>
						<!-- Adjuntos y videos-->
						<xsl:when test="file/@kind='generic' or file/@kind='multimedia'">
							<xsl:call-template name="createMultimediaElement"></xsl:call-template>
						</xsl:when>
						<!-- Imagenes(kind = image o sin kind) -->
						<xsl:otherwise>
							<xsl:call-template name="createImageElement"></xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
			</xsl:choose>
			<xsl:apply-templates select="component"/>
		</xsl:element>
	</xsl:template>
	<!-- Funcion para los videos (multimedia) -->
	<xsl:template name="createMultimediaElement">
		<xsl:element name="dynamic-element">
			<xsl:attribute name="index-type">
				<xsl:text></xsl:text>
			</xsl:attribute>
			<xsl:attribute name="name">
				<xsl:text>Document</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:text>document_library</xsl:text>
			</xsl:attribute>
			<xsl:element name="dynamic-content">
				<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
				<xsl:variable name="current-path">
					<xsl:call-template name="getFileNameFromPath">
						<xsl:with-param name="path" select="./file/@path"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat('/binrepository/', $current-path)" disable-output-escaping="yes"/>
				<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
			</xsl:element>
		</xsl:element>
		<!-- Si el video tiene preview -->
		<xsl:choose>
			<xsl:when test="./file/preview/@path">
				<xsl:element name="dynamic-element">
					<xsl:attribute name="index-type">
						<xsl:text></xsl:text>
					</xsl:attribute>
					<xsl:attribute name="name">
						<xsl:text>Preview</xsl:text>
					</xsl:attribute>
					<xsl:attribute name="type">
						<xsl:text>document_library</xsl:text>
					</xsl:attribute>
					<xsl:element name="dynamic-content">
						<xsl:variable name="current-path2">
							<xsl:call-template name="getFileNameFromPath">
								<xsl:with-param name="path" select="./file/preview/@path"/>
							</xsl:call-template>
						</xsl:variable>
						<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
						<xsl:value-of select="concat('/binrepository/', $current-path2)" disable-output-escaping="yes"/>
						<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
					</xsl:element>
				</xsl:element>
			</xsl:when>
		</xsl:choose>
		<xsl:element name="dynamic-element">
			<xsl:attribute name="name">
				<xsl:text>Extension</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:text>text</xsl:text>
			</xsl:attribute>
			<xsl:element name="dynamic-content">
				<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
				<xsl:variable name="current-extension">
					<xsl:call-template name="getExtensionFileFromPath">
						<xsl:with-param name="path" select="./file/@path"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="$current-extension" disable-output-escaping="yes"/>
				<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
			</xsl:element>
		</xsl:element>
	</xsl:template>
	<!-- Funcion para las imagenes -->
	<xsl:template name="createImageElement">
		<xsl:element name="dynamic-element">
			<xsl:attribute name="name">
				<xsl:text>_img-Binary_</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:text>document_library</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="index-type"/>
			<xsl:element name="dynamic-content">
				<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
				<xsl:variable name="current-path">
					<xsl:call-template name="getFileNameFromPath">
						<xsl:with-param name="path" select="./file/@path"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat('/binrepository/', $current-path)" disable-output-escaping="yes"/>
				<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
			</xsl:element>
		</xsl:element>
		<xsl:element name="img-info">
			<xsl:attribute name="width"/>
			<xsl:attribute name="height"/>
		</xsl:element>
	</xsl:template>
	<!-- Funcion que dado un path devuelve el nombre del fichero con su extension -->
	<xsl:template name="getFileNameFromPath">
		<xsl:param name="path"/>
		<xsl:choose>
			<xsl:when test="contains($path,'\')">
				<xsl:call-template name="getFileNameFromPath">
					<xsl:with-param name="path" select="substring-after($path,'\')"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="contains($path,'/')">
				<xsl:call-template name="getFileNameFromPath">
					<xsl:with-param name="path" select="substring-after($path,'/')"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$path"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Funcion que dado un path devuelve la extension del archivo -->
	<xsl:template name="getExtensionFileFromPath">
		<xsl:param name="path"/>
		<xsl:choose>
			<xsl:when test="contains($path, '/')">
				<xsl:call-template name="getExtensionFileFromPath">
					<xsl:with-param name="path" select="substring-after($path, '/')"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="contains($path, '.')">
				<xsl:call-template name="getExtensionFileFromPath">
					<xsl:with-param name="path" select="substring-after($path, '.')"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$path"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>