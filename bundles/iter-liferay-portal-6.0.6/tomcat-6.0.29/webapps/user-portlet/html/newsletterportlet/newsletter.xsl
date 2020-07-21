<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"></xsl:output>
	<xsl:variable name="subscribe" 				select="/newsletters/subscribe-msg"/>
	<xsl:variable name="unsubscribe" 			select="/newsletters/unsubscribe-msg"/>
	<xsl:variable name="manageerror" 			select="/newsletters/manageerror-msg"/>
	<xsl:variable name="notifysubscription" 	select="/newsletters/notifysubscription"/>
	
	<xsl:variable name="emailRepeatedError"		select="/newsletters/emailRepeatedError-msg"/>
	<xsl:variable name="chkSubsError"			select="/newsletters/chkSubsError-msg"/>
	<xsl:variable name="chkSubs"				select="/newsletters/chkSubs-msg"/>
	<xsl:variable name="allowAnonymous" 		select="/newsletters/allowAnonymous"/>
	<xsl:variable name="email" 					select="/newsletters/email"/>
	<xsl:variable name="licenseAgreement"		select="/newsletters/licenseAgreement"/>
	<xsl:variable name="acceptLicenseError" 	select="/newsletters/acceptLicenseError-msg"/>
	

	<xsl:template match="/newsletters">
		<div class="newsletters_expandCollapse newsletters_collapsed"/>
		<div class="newsletters">
			<div class="newsletter_general_title">
				<h2>
					<xsl:value-of disable-output-escaping="yes" select="./title"/>
				</h2>
			</div>
			<div class="newsletter_general_desc">
				<xsl:value-of disable-output-escaping="yes" select="./description"/>
			</div>
			<xsl:choose>
				<xsl:when test="@phpenabled = 'true'">
					<xsl:choose>
						<xsl:when test="$notifysubscription!='false'">
							<xsl:processing-instruction name="php">if (user_is_signedin()===true){ ?</xsl:processing-instruction>
						</xsl:when>
						<xsl:otherwise>
							<xsl:processing-instruction name="php">if (true){ ?</xsl:processing-instruction>
						</xsl:otherwise>
					</xsl:choose>

					<xsl:call-template name="templateNewsletters">
						<xsl:with-param name="light" select="'false'"/>
					</xsl:call-template>

					<xsl:processing-instruction name="php">}else { ?</xsl:processing-instruction>
					<xsl:call-template name="templateUnsignedUser"/>
					<xsl:processing-instruction name="php">} ?</xsl:processing-instruction>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="templateUnsignedUser"/>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

	<!-- Plantilla cuando no hay usuario registrado. Se muestra el mensaje configurado o las suscripciones light -->
	<xsl:template name="templateUnsignedUser">
		<xsl:choose>
			<xsl:when test="$allowAnonymous = 'true' and $notifysubscription != 'false'">
				<!-- S crea el control para introducir el correo electrónico--> 
				<div class="newsletter_email">
	               <div class="newsletter_email_label"><xsl:value-of disable-output-escaping="yes" select="$email"/></div>
	               <div class="newsletter_email_div">
				   		<input 	class=	"newsletter_email_ctrl"
								id=		"newsletter_email_ctrl" 
								name= 	"newsletter_email_ctrl"
								type="text"
								onkeyup="ITER.newsletter.checkLightFields();"/>
					</div>
	            </div>
	            <xsl:if test="string-length($chkSubs) > 0">
					<div class="newsletter_chk_subs_div">
					 	<span 	id="newsletter_chk_subs_ctrl" class="newsletter_chk_subs_ctrl" onclick="">
							<xsl:attribute name="onclick">ITER.newsletter.getLightNewsletterUser( 
										'<xsl:value-of select="$emailRepeatedError"/>', 
										'<xsl:value-of select="$chkSubsError"/>',
										'<xsl:value-of select="$acceptLicenseError"/>');
							</xsl:attribute>
							<xsl:value-of disable-output-escaping="yes" select="$chkSubs"/>
						</span>
					</div>
				</xsl:if>
				
				<xsl:if test="string-length($licenseAgreement) > 0">
					<div class="newsletter_license_check_div">
						<input 	class=	"newsletter_license_check"
								id=		"newsletter_license_check"
								name=	"newsletter_license_check"
								type=	"checkbox"
								onchange="ITER.newsletter.checkLightFields();"/>
						<div class="newsletter_license_div">
							<xsl:value-of disable-output-escaping="yes" select="$licenseAgreement"/>
						</div>
					</div>
				</xsl:if>

				<xsl:call-template name="templateNewsletters">
					<xsl:with-param name="light" select="'true'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<div class="newsletters_notsignin-msg">
					<xsl:value-of disable-output-escaping="yes" select="./notsignin-msg"/>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Pintado de las suscripciones light -->
	<xsl:template name="templateNewsletters">
		<xsl:param name="light"></xsl:param>
		<!-- Se obtiene el listado de Newsletter. Los usuarios anónimos no tendrán acceso 
		a las que tienen productos o desactivada la opción "permitir usuarios ançonimos"-->
		<xsl:for-each select="./newsletter[options/option[($light='true' and not(@productlist) and @allowAnonymous='true') or ($light != 'true')]]">
			<div class="newsletters_item">
				<div class="newsletters_cab newsletters_closed" id="">
					<xsl:attribute name="id">
						<xsl:value-of disable-output-escaping="yes" select="@id"/>
					</xsl:attribute>
					<div class="newsletters_title">
						<xsl:value-of disable-output-escaping="yes" select="./name"/>
					</div>
					<div class="newsletters_desc">
						<xsl:value-of disable-output-escaping="yes" select="./description"/>
					</div>
				</div>
				<div class="newsletters_options">
					<xsl:for-each select="./options/option[($light='true' and not(@productlist) and @allowAnonymous='true') or ($light != 'true')]">
						<div class="newsletters_option">
							<xsl:choose>
								<xsl:when test="@productlist">
									<xsl:processing-instruction name="php">if (user_has_access_to_any_of_these_products(NULL, array(<xsl:value-of select="@productlist"/>), NULL)===true){ ?</xsl:processing-instruction>
									<xsl:call-template name="templateFullAcces">
										<xsl:with-param name="id" 		select="@id"/>
										<xsl:with-param name="type" 	select="@type"/>
										<xsl:with-param name="title" 	select="./name"/>
										<xsl:with-param name="desc" 	select="./description"/>
										<xsl:with-param name="day" 		select="./days/day"/>
										<xsl:with-param name="hour" 	select="@hour"/>
										<xsl:with-param name="light" 	select="$light"/>
									</xsl:call-template>
									<xsl:processing-instruction name="php">}else{ ?</xsl:processing-instruction>
									<xsl:call-template name="templateRestricted">
										<xsl:with-param name="title" 	select="./name"/>
										<xsl:with-param name="desc" 	select="./description"/>
									</xsl:call-template>
									<xsl:processing-instruction name="php">} ?</xsl:processing-instruction>
								</xsl:when>
								<xsl:otherwise>
									<xsl:call-template name="templateFullAcces">
										<xsl:with-param name="id" 		select="@id"/>
										<xsl:with-param name="type" 	select="@type"/>
										<xsl:with-param name="title" 	select="./name"/>
										<xsl:with-param name="desc" 	select="./description"/>
										<xsl:with-param name="day" 		select="./days/day"/>
										<xsl:with-param name="hour" 	select="@hour"/>
										<xsl:with-param name="light" 	select="$light"/>
									</xsl:call-template>
								</xsl:otherwise>
							</xsl:choose>
						</div>
						<!--  newsletters_option  -->
					</xsl:for-each>
				</div>
				<!-- newsletters_options -->
			</div>
		</xsl:for-each>
	</xsl:template>

	<!-- Plantilla para el acceso restringido -->
	<xsl:template name="templateRestricted">
		<xsl:param name="title"></xsl:param>
		<xsl:param name="desc"></xsl:param>
		<div class="newsletters_check newsletters_restricted"></div>
		<div class="newsletters_option_title_desc">
			<spam class="newsletters_option_title">
				<xsl:value-of disable-output-escaping="yes" select="$title"/>
			</spam>
			<spam class="newsletters_option_desc">
				<xsl:value-of disable-output-escaping="yes" select="$desc"/>
			</spam>
		</div>
	</xsl:template>
	<!-- Plantilla para el acceso completo -->
	<xsl:template name="templateFullAcces">
		<xsl:param name="id"></xsl:param>
		<xsl:param name="type"></xsl:param>
		<xsl:param name="title"></xsl:param>
		<xsl:param name="desc"></xsl:param>
		<xsl:param name="day"></xsl:param>
		<xsl:param name="hour"></xsl:param>
		<xsl:param name="light"></xsl:param>
		<div class="newsletters_check">
			<xsl:variable name="pos" select="position()"/>
			<input type="checkbox" onchange="" id="" name="" value="">
				<xsl:if test="$notifysubscription!='false'">
					<xsl:choose>
						<xsl:when test="$light!='true'">
							<xsl:attribute name="onchange">ITER.newsletter.manageNewsletter(this.id, this.checked, 
									'<xsl:value-of select="$subscribe"/>', 
									'<xsl:value-of select="$unsubscribe"/>', 
									'<xsl:value-of select="$manageerror"/>');
							</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="onchange">ITER.newsletter.manageLightNewsletter(this.id, this.checked, 
									'<xsl:value-of select="$subscribe"/>', 
									'<xsl:value-of select="$unsubscribe"/>', 
									'<xsl:value-of select="$manageerror"/>',
									'<xsl:value-of select="$emailRepeatedError"/>', 
									'<xsl:value-of select="$acceptLicenseError"/>');
							</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
				<xsl:attribute name="name">
					<xsl:choose>
						<xsl:when test="$notifysubscription!='false'">
							<xsl:value-of select="concat('option_', $pos)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('iter_newsletter_', $id)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="value">
					<xsl:value-of disable-output-escaping="yes" select="$id"/>
				</xsl:attribute>
				<xsl:attribute name="id">
					<xsl:value-of disable-output-escaping="yes" select="$id"/>
				</xsl:attribute>
			</input>
		</div>
		<xsl:if test="$type='alert'">
			<div class="newsletters_option_type newsletters_alert"></div>
		</xsl:if>
		<xsl:if test="$type='scheduled'">
			<div class="newsletters_option_type newsletters_program"></div>
		</xsl:if>
		<div class="newsletters_option_title_desc">
			<spam class="newsletters_option_title">
				<xsl:value-of disable-output-escaping="yes" select="$title"/>
			</spam>
			<spam class="newsletters_option_desc">
				<xsl:value-of disable-output-escaping="yes" select="$desc"/>
			</spam>
		</div>
		<xsl:if test="days">
			<div class="newsletters_option_date">
				<div class="newsletters_option_days">
					<xsl:variable name="length" select="count($day)"/>
					<xsl:for-each select="$day">
						<xsl:variable name="numDays" select="position()"/>
						<xsl:if test="$numDays &lt; $length">
							<xsl:value-of disable-output-escaping="yes" select="./text()"/>
							<xsl:text>-</xsl:text>
						</xsl:if>
						<xsl:if test="$numDays = $length">
							<xsl:value-of disable-output-escaping="yes" select="./text()"/>
						</xsl:if>
					</xsl:for-each>
				</div>
				<div class="newsletters_option_hour">
					<xsl:value-of disable-output-escaping="yes" select="$hour"/>
				</div>
			</div>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>