<?xml version='1.0' ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" cdata-section-elements="description url para cc cco smtpserver textsubject textbody"/>
	<xsl:template match="/">
		<form>
		
			<xsl:attribute name="groupName">
				<xsl:value-of select="rs/row/@groupName"/>
			</xsl:attribute>
		
			<xsl:attribute name="formid">
				<xsl:value-of select="rs/row/@formid"/>
			</xsl:attribute>
			<xsl:attribute name="submitlabel">
				<xsl:value-of select="rs/row/@submitlabel"/>
			</xsl:attribute>
			<xsl:attribute name="usesubmit">
				<xsl:value-of select="rs/row/@usesubmit"/>
			</xsl:attribute>
			<xsl:attribute name="resetlabel">
				<xsl:value-of select="rs/row/@resetlabel"/>
			</xsl:attribute>
			<xsl:attribute name="formtype">
				<xsl:value-of select="rs/row/@formtype"/>
			</xsl:attribute>
			<xsl:attribute name="usechaptcha">
				<xsl:value-of select="rs/row/@usechaptcha"/>
			</xsl:attribute>
			<xsl:attribute name="css">
				<xsl:value-of select="rs/row/@css"/>
			</xsl:attribute>
			<xsl:attribute name="name">
				<xsl:value-of select="rs/row/@name"/>
			</xsl:attribute>
			<xsl:attribute name="restricted">
				<xsl:value-of select="rs/row/@restricted"/>
			</xsl:attribute>
			<xsl:attribute name="oklabel">
				<xsl:value-of select="rs/row/@oklabel"/>
			</xsl:attribute>
			<xsl:attribute name="cncllabel">
				<xsl:value-of select="rs/row/@cncllabel"/>
			</xsl:attribute>
			<xsl:attribute name="confirmpaneltitle">
				<xsl:value-of select="rs/row/@confirmpaneltitle"/>
			</xsl:attribute>
			<description>
				<xsl:value-of select="rs/row/description"/>
			</description>
			<xsl:value-of disable-output-escaping="yes" select="rs/row/navigationtype"/>
			<xsl:value-of disable-output-escaping="yes" select="rs/row/ok"/>
			<xsl:value-of disable-output-escaping="yes" select="rs/row/ko"/>
			<handlers>
				<dbhandler>
					<xsl:attribute name="enabled">
						<xsl:value-of select="rs/row/@dbhandlerenabled"/>
					</xsl:attribute>
					<xsl:attribute name="critic">
						<xsl:value-of select="rs/row/@dbhandlercritic"/>
					</xsl:attribute>
					<xsl:attribute name="id">
						<xsl:value-of select="rs/row/@dbhandlerid"/>
					</xsl:attribute>
				</dbhandler>
				<filehandler>
					<xsl:attribute name="formname">
						<xsl:value-of select="rs/row/@fileformname"/>
					</xsl:attribute>
					<xsl:attribute name="foldername">
						<xsl:value-of select="rs/row/@filefoldername"/>
					</xsl:attribute>
					<xsl:attribute name="transformid">
						<xsl:value-of select="rs/row/@filetransformid"/>
					</xsl:attribute>
					<xsl:attribute name="transformname">
						<xsl:value-of select="rs/row/@filetransformname"/>
					</xsl:attribute>
					<xsl:attribute name="processdata">
						<xsl:value-of select="rs/row/@fileprocessdata"/>
					</xsl:attribute>
					<xsl:attribute name="enabled">
						<xsl:value-of select="rs/row/@filehandlerenabled"/>
					</xsl:attribute>
					<xsl:attribute name="critic">
						<xsl:value-of select="rs/row/@filehandlercritic"/>
					</xsl:attribute>
					<xsl:attribute name="id">
						<xsl:value-of select="rs/row/@filehandlerid"/>
					</xsl:attribute>
				</filehandler>
				<servlethandler>
					<xsl:attribute name="transformid">
						<xsl:value-of select="rs/row/@servlettransformid"/>
					</xsl:attribute>
					<xsl:attribute name="transformname">
						<xsl:value-of select="rs/row/@servlettransformname"/>
					</xsl:attribute>
					<xsl:attribute name="processdata">
						<xsl:value-of select="rs/row/@servletprocessdata"/>
					</xsl:attribute>
					<xsl:attribute name="enabled">
						<xsl:value-of select="rs/row/@servlethandlerenabled"/>
					</xsl:attribute>
					<xsl:attribute name="critic">
						<xsl:value-of select="rs/row/@servlethandlercritic"/>
					</xsl:attribute>
					<xsl:attribute name="id">
						<xsl:value-of select="rs/row/@servlethandlerid"/>
					</xsl:attribute>
					<url>
						<xsl:value-of select="rs/row/servleturl"/>
					</url>
				</servlethandler>
				<emailhandler>
					<xsl:attribute name="type">
						<xsl:value-of select="rs/row/@emailtype"/>
					</xsl:attribute>
					<xsl:attribute name="transformid">
						<xsl:value-of select="rs/row/@emailtransformid"/>
					</xsl:attribute>
					<xsl:attribute name="transformname">
						<xsl:value-of select="rs/row/@emailtransformname"/>
					</xsl:attribute>
					<xsl:attribute name="smtpserver">
						<xsl:value-of select="rs/row/@emailsmtpserver"/>
					</xsl:attribute>
					<xsl:attribute name="smtpservername">
						<xsl:value-of select="rs/row/@smtpservername"/>
					</xsl:attribute>
					<xsl:attribute name="processdata">
						<xsl:value-of select="rs/row/@emailprocessdata"/>
					</xsl:attribute>
					<xsl:attribute name="enabled">
						<xsl:value-of select="rs/row/@emailhandlerenabled"/>
					</xsl:attribute>
					<xsl:attribute name="critic">
						<xsl:value-of select="rs/row/@emailhandlercritic"/>
					</xsl:attribute>
					<xsl:attribute name="id">
						<xsl:value-of select="rs/row/@emailhandlerid"/>
					</xsl:attribute>
					<para>
						<xsl:value-of select="rs/row/emailpara"/>
					</para>
					<cc>
						<xsl:value-of select="rs/row/emailcc"/>
					</cc>
					<cco>
						<xsl:value-of select="rs/row/emailcco"/>
					</cco>
					<textsubject>
						<xsl:value-of select="rs/row/emailtextsubject"/>
					</textsubject>
					<textbody>
						<xsl:value-of select="rs/row/emailtextbody"/>
					</textbody>
				</emailhandler>
			</handlers>
		</form>
	</xsl:template>
</xsl:stylesheet>