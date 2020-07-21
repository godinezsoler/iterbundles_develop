<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fn="http://www.w3.org/2005/xpath-functions">
  <xsl:output method="xml" indent="yes"/>
  <xsl:param name="outpath"/>
  
  <xsl:template match="*"/>
  
  <xsl:template match="/*">
    <xsl:for-each select="//item[@classname='com.liferay.portlet.journal.model.JournalArticle']">
      <!--Se crea un archivo por cada journalArticle-->

<!--
      <xsl:variable name="rootpath" select="tokenize(document-uri(/), '/')[last()-1]"/>      
      <xsl:variable name="filename" select="concat( $rootpath, '/', @globalid ,'.xml')"/>
-->
      <xsl:variable name="filename" select="concat( $outpath , '/', @globalid ,'.xml')"/>
      <xsl:value-of select="$filename"/>
      <xsl:result-document href="{$filename}" method="xml" indent="yes">
        <!--Se crea el elemento raiz MileniumXML-->
        <xsl:element name="MileniumXML">
          <!--Se añaden sus atributos-->
          <xsl:attribute name="creator">IterWeb</xsl:attribute>
          <xsl:attribute name="type">Article</xsl:attribute>
          <xsl:attribute name="version">2</xsl:attribute>
          <!--Se crea el elemento Article-->
          <xsl:element name="Article">
            <!--Se obtiene el xml donde se define el articulo-->
            <xsl:variable name="filepath">
              <xsl:value-of select="replace(param[@name='file']/text(), '&lt;!\[CDATA\[(.*)\]\]&gt;', '$1')"/>
            </xsl:variable>
            <xsl:variable name="article_xml" select="document( concat($outpath, $filepath) )"/>
            <!--Se añaden sus atributos-->
            <xsl:attribute name="creationdate"></xsl:attribute>
            <xsl:attribute name="id">
              <xsl:value-of select="replace(param[@name='articleid']/text(), '&lt;!\[CDATA\[(.*)\]\]&gt;', '$1')"/>
            </xsl:attribute>
            <xsl:attribute name="name">
              <xsl:value-of select="$article_xml//dynamic-element[@name='Headline']/dynamic-content/text()"/>
            </xsl:attribute>

            <xsl:for-each select="$article_xml/root/dynamic-element[@type='text' and @name!='Image' and @name!='Multimedia' and @name!='TeaserImage' and @name!='ExternalLink' and @name!='InternalLink']">
              <xsl:call-template name="create_text_component"/>
            </xsl:for-each>
            <xsl:for-each select="$article_xml/root/dynamic-element[@name='Image' or @name='TeaserImage']">
              <xsl:call-template name="create_no_text_node">
                <xsl:with-param name="class_name">Image</xsl:with-param>
                <xsl:with-param name="att_path">Medium</xsl:with-param>
              </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="$article_xml/root/dynamic-element[@name='Multimedia']">
              <xsl:call-template name="create_no_text_node">
                <xsl:with-param name="class_name">Multimedia</xsl:with-param>
                <xsl:with-param name="att_path">Document</xsl:with-param>
              </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="$article_xml/root/dynamic-element[@type='text_area']">
              <xsl:call-template name="create_text_component"/>
            </xsl:for-each>
            <xsl:for-each select="$article_xml/root/dynamic-element[@type='text_box']">
              <xsl:call-template name="create_text_component"/>
            </xsl:for-each>
          </xsl:element>          
        </xsl:element>
      </xsl:result-document>
    </xsl:for-each>
    
  </xsl:template>

  <xsl:template name="create_text_component">
    <xsl:element name="Component">
      <xsl:attribute name="class">Text</xsl:attribute>
      <xsl:attribute name="type">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <xsl:call-template name="add_paragraphs"/>
    </xsl:element>
  </xsl:template>

  <xsl:template name="add_paragraphs">
    <xsl:for-each select="tokenize(./dynamic-content/text(), '\s*(&lt;p&gt;)\s*|\s*(&lt;/p&gt;)\s*', 'si')">
      <xsl:variable name="paragraph">
        <xsl:value-of select="."/>
      </xsl:variable>
      <xsl:if test="string-length($paragraph)">
        <xsl:element name="Paragraphs">
          <xsl:element name="P">
            <xsl:element name="C">
              <xsl:value-of select="$paragraph"/>
            </xsl:element>
          </xsl:element>
        </xsl:element>
      </xsl:if>
    </xsl:for-each>  
  </xsl:template>

  <xsl:template name="create_no_text_node">
    <xsl:param name="class_name"/>
    <xsl:param name="att_path"/>

    <xsl:element name="Component">
      <xsl:attribute name="class">
        <xsl:value-of select="$class_name"/>
      </xsl:attribute>
      <xsl:attribute name="typename">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <xsl:attribute name="name">
        <xsl:value-of select="./dynamic-content/text()"/>
      </xsl:attribute>
      <xsl:attribute name="path">
        <xsl:value-of select="replace( substring-after(./dynamic-element[@name=$att_path]/dynamic-content/text(), '/'), '/', '\\' )"/>
      </xsl:attribute>
      <xsl:if test="./dynamic-element[@name='Byline']/dynamic-content/text()!=''">
        <xsl:element name="Author">
          <xsl:attribute name="byline">
            <xsl:value-of select="./dynamic-element[@name='Byline']/dynamic-content/text()"/>
          </xsl:attribute>
        </xsl:element>
      </xsl:if>
    </xsl:element>
    
    <xsl:for-each select="./dynamic-element[@name='Byline' or @name='Cutline']">
      <xsl:if test="./dynamic-content/text()!=''">
        <xsl:call-template name="create_text_component"/>
      </xsl:if>
    </xsl:for-each>
    
  </xsl:template>

</xsl:stylesheet> 
