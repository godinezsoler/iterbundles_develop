<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml"/>

	<xsl:strip-space elements="*"/>

	  <xsl:template match="@*|node()">
	    <xsl:copy>
			<!-- Se generan los atributos -->
			<xsl:apply-templates select="node()[starts-with(local-name(), '_') and local-name()!='__text']"/>
			<!-- Se generan los textos -->
			<xsl:apply-templates select="__text"/>
			<!-- Se copia el resto de nodos -->
			<xsl:apply-templates select="@*|node()[ not(starts-with(local-name(), '_') or local-name()='__text') ]"/>
	    </xsl:copy>
	  </xsl:template>

	<!-- Atributos -->
	<xsl:template match="*[starts-with(local-name(), '_') and local-name()!='__text']">
		<xsl:variable name="attrName" select="substring(local-name(),2)"/>
		<xsl:attribute name="{$attrName}">
    		<xsl:value-of select="text()"/>
  		</xsl:attribute>
		<xsl:apply-templates select="@*|*"/>
	</xsl:template>

	<!-- Nodos texto -->
	<xsl:template match="__text">
		<xsl:value-of select="text()"/>
		<!--<xsl:text><xsl:value-of select="text()"/></xsl:text>-->
		<xsl:apply-templates select="@*|*"/>
	</xsl:template>

</xsl:stylesheet><!-- Stylus Studio meta-information - (c) 2004-2009. Progress Software Corporation. All rights reserved.

<metaInformation>
	<scenarios>
		<scenario default="yes" name="mxlsimple2xml" userelativepaths="yes" externalpreview="no" url="simpleXML.xml" htmlbaseurl="" outputurl="complexXML.xml" processortype="saxon8" useresolver="yes" profilemode="0" profiledepth="" profilelength=""
		          urlprofilexml="" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext="" validateoutput="no" validator="internal"
		          customvalidator="">
			<advancedProp name="sInitialMode" value=""/>
			<advancedProp name="bXsltOneIsOkay" value="true"/>
			<advancedProp name="bSchemaAware" value="true"/>
			<advancedProp name="bXml11" value="false"/>
			<advancedProp name="iValidation" value="0"/>
			<advancedProp name="bExtensions" value="true"/>
			<advancedProp name="iWhitespace" value="0"/>
			<advancedProp name="sInitialTemplate" value=""/>
			<advancedProp name="bTinyTree" value="true"/>
			<advancedProp name="bWarnings" value="true"/>
			<advancedProp name="bUseDTD" value="false"/>
			<advancedProp name="iErrorHandling" value="fatal"/>
		</scenario>
	</scenarios>
	<MapperMetaTag>
		<MapperInfo srcSchemaPathIsRelative="yes" srcSchemaInterpretAsXML="no" destSchemaPath="" destSchemaRoot="" destSchemaPathIsRelative="yes" destSchemaInterpretAsXML="no"/>
		<MapperBlockPosition></MapperBlockPosition>
		<TemplateContext></TemplateContext>
		<MapperFilter side="source"></MapperFilter>
	</MapperMetaTag>
</metaInformation>
-->