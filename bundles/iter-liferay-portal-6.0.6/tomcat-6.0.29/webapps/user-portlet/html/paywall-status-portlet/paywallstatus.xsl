<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" omit-xml-declaration="yes"></xsl:output>
	<xsl:template match="/rs">
		<xsl:variable name="noSignedMsg" select="./noSignedMsg/text()"/>
 		<xsl:variable name="type" select="@type"/>
		<xsl:variable name="cookieName" select="./cookieName/text()"/>
		<xsl:variable name="productName" select="./productName/text()"/>
		<xsl:variable name="dateName" select="./dateName/text()"/>
		<xsl:variable name="userAgentName" select="./userAgentName/text()"/>
		<xsl:variable name="ipName" select="./ipName/text()"/>
		<xsl:variable name="articleName" select="./articleName/text()"/>
		<xsl:variable name="expiresDateName" select="./expiresDateName/text()"/>
		<xsl:variable name="typeDiv" select="./typeDiv/text()"/>
		<xsl:variable name="typeDivClose" select="./typeDivClose/text()"/>
		<xsl:choose>
			<xsl:when test="@phpenabled='true'">
				<xsl:processing-instruction name="php"> if (user_is_signedin()===true){ ?</xsl:processing-instruction>
					<div class="paywallstatus">
						<div class="paywallstatus_title">
							<h2>
								<xsl:value-of disable-output-escaping="yes" select="./title"/>
							</h2>
						</div>
						<div class="paywallstatus_table">
							<div class="paywallstatus_row">
								<xsl:if test="$type!='3'">
								 	<div class="paywallstatus_header">
										<xsl:value-of disable-output-escaping="yes" select="$cookieName"/>
									</div>
									<div class="paywallstatus_header">
										<xsl:value-of disable-output-escaping="yes" select="$dateName"/>
									</div>
									<div class="paywallstatus_header">
										<xsl:value-of disable-output-escaping="yes" select="$userAgentName"/>
									</div>
									<div class="paywallstatus_header">
										<xsl:value-of disable-output-escaping="yes" select="$ipName"/>
									</div>
								</xsl:if>
								<xsl:if test="$type='2'">
									<div class="paywallstatus_header">
										<xsl:value-of disable-output-escaping="yes" select="$articleName"/>
									</div>
								</xsl:if>
								<div class="paywallstatus_header">
									<xsl:value-of disable-output-escaping="yes" select="$productName"/>
								</div>
								<xsl:if test="$type='3'">
									<div class="paywallstatus_header">
										<xsl:value-of disable-output-escaping="yes" select="$expiresDateName"/>
									</div>
								</xsl:if>
							</div>
							<xsl:for-each select="./row">
								<div class="paywallstatus_row">
									<xsl:if test="$type!='3'">
									 	<div class="paywallstatus_cell">
											<xsl:value-of disable-output-escaping="yes" select="@cookieid"/>
										</div>
										<div class="paywallstatus_cell">
											<xsl:value-of disable-output-escaping="yes" select="@fecha"/>
										</div>
										<div class="paywallstatus_cell">
											<xsl:value-of disable-output-escaping="yes" select="@useragent"/>
										</div>
										<div class="paywallstatus_cell">
											<xsl:value-of disable-output-escaping="yes" select="@ip"/>
										</div>
									</xsl:if>
									<xsl:if test="$type='2'">
										<div class="paywallstatus_cell">
											<xsl:value-of disable-output-escaping="yes" select="@urlTitle"/>
										</div>
									</xsl:if>
									<div class="paywallstatus_cell">
										<xsl:value-of disable-output-escaping="yes" select="./pname/text()"/>
									</div>
									<xsl:if test="$type='3'">
										<div class="paywallstatus_cell">
											<xsl:value-of disable-output-escaping="yes" select="@expires"/>
										</div>
									</xsl:if>
								</div>
							</xsl:for-each>
						</div>
					</div>
				<xsl:processing-instruction name="php"> }else{ ?</xsl:processing-instruction>
					<div class="paywallstatus_notsignin-msg">
						<xsl:value-of disable-output-escaping="yes" select="$noSignedMsg"/>
					</div>
				<xsl:processing-instruction name="php"> } ?</xsl:processing-instruction>
			</xsl:when>
			<xsl:otherwise>
				<div class="paywallstatus_notsignin-msg">
					<xsl:value-of disable-output-escaping="yes" select="$noSignedMsg"/>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>