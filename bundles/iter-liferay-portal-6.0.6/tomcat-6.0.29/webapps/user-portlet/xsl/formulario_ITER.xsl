<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="html"></xsl:output>
	<xsl:template name="format_d">
		<xsl:param name="format_in"/>
		<xsl:choose>
			<xsl:when test="$format_in='dd/MM/yyyy'">
				<xsl:value-of select="'dd/mm/yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dd-MM-yyyy'">
				<xsl:value-of select="'dd-mm-yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dd.MM.yyyy'">
				<xsl:value-of select="'dd.mm.yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dd/MM/yy'">
				<xsl:value-of select="'dd/mm/y'"/>
			</xsl:when>
			<xsl:when test="$format_in='dd-MM-yy'">
				<xsl:value-of select="'dd-mm-y'"/>
			</xsl:when>
			<xsl:when test="$format_in='dd.MM.yy'">
				<xsl:value-of select="'dd.mm.y'"/>
			</xsl:when>
			<xsl:when test="$format_in='d/MM/yy'">
				<xsl:value-of select="'d/mm/y'"/>
			</xsl:when>
			<xsl:when test="$format_in='d-MM-yy'">
				<xsl:value-of select="'d-mm-y'"/>
			</xsl:when>
			<xsl:when test="$format_in='d.MM.yy'">
				<xsl:value-of select="'d.mm.y'"/>
			</xsl:when>
			<xsl:when test="$format_in='d/M/yy'">
				<xsl:value-of select="'d/m/y'"/>
			</xsl:when>
			<xsl:when test="$format_in='d-M-yy'">
				<xsl:value-of select="'d-m-y'"/>
			</xsl:when>
			<xsl:when test="$format_in='d.M.yy'">
				<xsl:value-of select="'d.m.y'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy/MM/dd'">
				<xsl:value-of select="'y/mm/dd'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy-MM-dd'">
				<xsl:value-of select="'y-mm-dd'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy.MM.dd'">
				<xsl:value-of select="'y.mm.dd'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy/MM/d'">
				<xsl:value-of select="'y/mm/d'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy-MM-d'">
				<xsl:value-of select="'y-mm-d'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy.MM.d'">
				<xsl:value-of select="'y.mm.d'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy/M/d'">
				<xsl:value-of select="'y/m/d'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy-M-d'">
				<xsl:value-of select="'y-m-d'"/>
			</xsl:when>
			<xsl:when test="$format_in='yy.M.d'">
				<xsl:value-of select="'y.m.d'"/>
			</xsl:when>
			<xsl:when test="$format_in='yyyy/MM/dd'">
				<xsl:value-of select="'yy/mm/dd'"/>
			</xsl:when>
			<xsl:when test="$format_in='yyyy-MM-dd'">
				<xsl:value-of select="'yy-mm-dd'"/>
			</xsl:when>
			<xsl:when test="$format_in='yyyy.MM.dd'">
				<xsl:value-of select="'yy.mm.dd'"/>
			</xsl:when>
			<xsl:when test="$format_in='dddd MMMM dd yyyy'">
				<xsl:value-of select="'DD MM dd yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dddd, MMMM dd yyyy'">
				<xsl:value-of select="'DD, MM dd yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dddd dd MMMM yyyy'">
				<xsl:value-of select="'DD dd MM yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dddd, dd MMMM yyyy'">
				<xsl:value-of select="'DD, dd MM yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='dd MMMM yyyy'">
				<xsl:value-of select="'dd MM yy'"/>
			</xsl:when>
			<xsl:when test="$format_in='MMMM dd yyyy'">
				<xsl:value-of select="'MM dd yy'"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>


	<xsl:template match="form">
		<!-- variables generales -->
		<xsl:variable name="id_form" select="@id"/>
		<xsl:variable name="usecaptcha" select="@usecaptcha"/>
		<xsl:variable name="captchakey" select="@captchakey"/>
		<xsl:variable name="captchalang" select="@captchalang"/>
		<xsl:variable name="captchatheme" select="@captchatheme"/>
		<xsl:variable name="captchasize" select="@captchasize"/>
		<xsl:variable name="use-submit" select="@use-submit"/>
		<xsl:variable name="submitlabel" select="@submitlabel"/>
		<xsl:variable name="resetlabel" select="@resetlabel"/>
		<xsl:variable name="nextlabel" select="@nextlabel"/>
		<xsl:variable name="prevlabel" select="@prevlabel"/>
		<xsl:variable name="navigation" select="@fieldsgroup-navigation"/>
		<xsl:variable name="error" select="invalidfieldmsg"/>
		<xsl:variable name="ok_label" select="@oklabel"/>
		<xsl:variable name="cancel_label" select="@cncllabel"/>
		<xsl:variable name="confir_label" select="@confirmpaneltitle"/>

		<xsl:variable name="otpCodeFieldId">
			<xsl:value-of select="$id_form"/>_<xsl:value-of select="/form//field[@fieldtype='otp_code']/inputctrl/@id"/></xsl:variable>
		<xsl:variable name="otpButtonFieldId">
			<xsl:value-of select="$id_form"/>_<xsl:value-of select="/form//field[@fieldtype='otp_button']/inputctrl/@id"/></xsl:variable>
		<xsl:variable name="phoneFieldId">
			<xsl:value-of select="$id_form"/>_<xsl:value-of select="/form//field[@fieldtype='otp_button']/inputctrl/@id_phone"/></xsl:variable>


		<script type="text/javascript">function otp_enable_logic()
		{
			var phoneCtrlId		 = "#<xsl:value-of select="$phoneFieldId"/>"
			var phoneCtrlId2	 = "#<xsl:value-of select="$phoneFieldId"/>_rep";
						
			var otpCodeCtrlId 	 = "#<xsl:value-of select="$otpCodeFieldId"/>";
			var otpButtonCtrlId  = "#<xsl:value-of select="$otpButtonFieldId"/>";
			
			var originalTelephone= jQryIter(phoneCtrlId).val();
			checkSameValue();
			
			jQryIter(phoneCtrlId).keyup(function(event)
			{
				checkSameValue();    
			});
			
			// Si existe la confirmaci�n del campo
			if ( jQryIter(phoneCtrlId2).length )
			{
				jQryIter(phoneCtrlId2).keyup(function(event)
				{
					checkSameValue();    
				});
			}
			
			function checkSameValue()
			{
				
				if( ( (!jQryIter(phoneCtrlId2).length) || jQryIter(phoneCtrlId).val() == jQryIter(phoneCtrlId2).val() ) &amp;&amp; originalTelephone != jQryIter(phoneCtrlId).val())
				{
					console.log("Los tel�fonos coinciden");
					
					jQryIter(otpCodeCtrlId).prop("disabled",false);
					jQryIter(otpButtonCtrlId).prop("disabled",false);
				}
				else
				{
					console.log("Los tel�fonos no coinciden");
					
					jQryIter(otpCodeCtrlId).prop("disabled",true);
					jQryIter(otpButtonCtrlId).prop("disabled",true);
				}
			}
		}

		function documentReady_<xsl:value-of select="$id_form"/>() 
		{ 
			var dataresponseform = {};
			var options = {
				dataType: "json",
				beforeSubmit:  function()
				{
					if (validarform_<xsl:value-of select="$id_form"/>() == false)
						return false;
				},
				success:  function(data) {
					if(data.msg!=""){
						jQryIter('#loading').remove();
						//alert(data.msg);
						jQryIter( "#other_<xsl:value-of select="$id_form"/> .label_error").text(data.msg);
						dataresponseform = data.furtheraction;
						jQryIter( "#other_<xsl:value-of select="$id_form"/>" ).dialog({
							height: 180,
							width: 360,
							modal: true,
							dialogClass: "dialog_error",
							close: function(event, ui)
							{
								if (dataresponseform.location != "none" &amp;&amp; dataresponseform.location != "")
								{
									window.location.href = dataresponseform.location;
								}
							},
							buttons: [
								{
								  text: '<xsl:value-of select="$ok_label"/>',
								  click: function(event) {
									switch(dataresponseform.action)
									{
										case "none":
											//Nothing to Do
											jQryIter(this).dialog('close');
											break;
										case "refresh":
											location.reload(false);
											break;
										case "close":
											window.close();
											break;
										case "backtoreferer":
			<xsl:if test="@referer!=''">var formReferer = '<xsl:processing-instruction name="php">echo <xsl:value-of select="@referer"/>;?</xsl:processing-instruction>';
												location.href = formReferer;</xsl:if>break;
										case "redirect":
											if (dataresponseform.location != "none" &amp;&amp; dataresponseform.location != ""){
												location.href = dataresponseform.location;
											}
											break;
										default:
											jQryIter(this).dialog('close');
									}
								  }
								}
							 ]
						});
					}
					jQryIter.resetCaptcha("captcha_<xsl:value-of select="$id_form"/>");
					 
				}
			};
			jQryIter('#<xsl:value-of select="@id"/>').submit(function() {
				jQryIter(this).ajaxSubmit(options);
				jQryIter('#pages').prepend('<div id="loading"></div>');
				return false; 
			}); 
			
			
			jQryIter('#<xsl:value-of select="@id"/> input[iter_needconfirm="true"]').each(function(){
					jQryIter(this).attr("orig_value",jQryIter(this).val())
					activar_confirm(jQryIter(this)[0]);
			})
			
			
		}
		
		jQryIter(document).ready(function() 
		{
			documentReady_<xsl:value-of select="$id_form"/>();
			<xsl:if test="./@css='form-reset-credentials' or @css='form-register-user' or @css='form-update-profile'">otp_enable_logic();</xsl:if>
		});
		</script>

		<form id="{@id}" action="{servlet}" method="POST" class="{@css}" enctype="multipart/form-data">
			<div id="pages">
				<!-- INICIO TABS SOLAPAS -->
				<xsl:if test="$navigation='tab'">
					<ul>
						<xsl:for-each select="fieldsgroup">
							<xsl:variable name="i" select="position()"/>
							<li>
								<a href="{concat('#page-', $i)}">
									<xsl:value-of select="@name"/>
								</a>
							</li>
						</xsl:for-each>
					</ul>
				</xsl:if>
				<!-- FIN TABS SOLAPAS -->
				<xsl:variable name="cont" select="0"/>
				<!-- INICIO GRUPOS-->
				<xsl:for-each select="fieldsgroup">
					<xsl:variable name="i2" select="position()"/>
					<div id="{concat('page-', $i2)}" class="field_form" name="{@name}">
						<!-- Ocultamos las paginas -->
						<xsl:if test="$navigation='page'">
							<xsl:choose>
								<xsl:when test="$i2='1'">
									<xsl:attribute name="style"></xsl:attribute>
									<xsl:attribute name="class">field_form _current</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="style">display:none</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:if>
						<!-- Cabeceras de los bloques de campos -->
						<div id="{concat('textheader-', $i2)}" class="tab" style="display:none;">
							<span class="number">
								<xsl:value-of select="$i2"/>
							</span>
							<div class="text"></div>
						</div>
						<div id="{concat('block-', $i2)}" class="blocksfields">
							<!-- INICIO CAMPOS -->
							<xsl:for-each select="field">
								<!-- propiedades del elemento -->
								<xsl:variable name="editable" select="@editable"/>
								<xsl:variable name="obligatorio" select="@required"/>
								<xsl:variable name="repetir" select="@repeatable"/>
								<xsl:variable name="confirmar" select="@needconfirm"/>
								<xsl:variable name="id_element">
									<xsl:value-of select="$id_form"/>_<xsl:value-of select="inputctrl/@id"/></xsl:variable>
								<xsl:variable name="tab_order" select="concat(/form/@id, $i2, position(),'000')"/>

								<div id="field_{$id_element}" class="field_form {@css}">


									<!-- MARCA OBLIGATORIO -->
									<div class="campo_obligatorio">&#xA0;
										<xsl:if test="$obligatorio='true'">
											<div class="text_obligatorio"></div>
										</xsl:if>
									</div>
									<!-- /MARCA OBLIGATORIO -->

									<!-- LABEL ANTERIOR -->

									<div class="label_ant">
										<xsl:if test="labelbefore/textlabel!=''">
											<xsl:choose>
												<xsl:when test="labelbefore/textlabel/@linkurl!=''">
													<a href="{labelbefore/textlabel/@linkurl}" target="{labelbefore/textlabel/@linktarget}">
														<xsl:value-of select="labelbefore/textlabel"/>
													</a>
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="labelbefore/textlabel"/>
												</xsl:otherwise>
											</xsl:choose>
										</xsl:if>
									</div>

									<!-- / LABEL ANTERIOR -->

									<!-- ELEMENTO -->
									<div class="element_form">
										<xsl:choose>
											<!-- LISTADO dropdownlist -->
											<xsl:when test="inputctrl/@type='dropdownlist'">
												<select id="{$id_element}" name="{inputctrl/@name}" title="{tooltip}" tabindex="{$tab_order}" class="field_elem">
													<xsl:if test="inputctrl/options/@multiple='true'">
														<xsl:attribute name="multiple">true</xsl:attribute>
													</xsl:if>
													<xsl:if test="@editable='false'">
														<xsl:attribute name="disabled">true</xsl:attribute>
													</xsl:if>
													<xsl:if test="inputctrl/options/@size!=''">
														<xsl:attribute name="size">
															<xsl:value-of select="inputctrl/options/@size"/>
														</xsl:attribute>
													</xsl:if>
													<!-- OPCIONES -->
													<xsl:for-each select="inputctrl/options/option">
														<option>
															<!-- verificar value-->
															<xsl:choose>
																<xsl:when test="@value=''">
																	<xsl:attribute name="value">
																		<xsl:value-of select="."/>
																	</xsl:attribute>
																</xsl:when>
																<xsl:otherwise>
																	<xsl:attribute name="value">
																		<xsl:value-of select="@value"/>
																	</xsl:attribute>
																</xsl:otherwise>
															</xsl:choose>
															<xsl:if test="@selected='true'">
																<xsl:attribute name="selected">true</xsl:attribute>
															</xsl:if>
															<xsl:value-of select="."/>
														</option>
													</xsl:for-each>
													<!-- / OPCIONES -->
												</select>
											</xsl:when>
											<!-- / LISTADO dropdownlist -->

											<!-- LISTADO listctrl -->
											<xsl:when test="inputctrl/@type='listctrl'">
												<select id="{$id_element}" name="{inputctrl/@name}" size="3" title="{tooltip}" tabindex="{$tab_order}" class="field_elem">
													<xsl:if test="inputctrl/options/@multiple='true'">
														<xsl:attribute name="multiple">true</xsl:attribute>
													</xsl:if>
													<xsl:if test="inputctrl/options/@size!=''">
														<xsl:attribute name="size">
															<xsl:value-of select="inputctrl/options/@size"/>
														</xsl:attribute>
													</xsl:if>
													<xsl:if test="@editable='false'">
														<xsl:attribute name="disabled">true</xsl:attribute>
													</xsl:if>
													<!-- OPCIONES -->
													<xsl:for-each select="inputctrl/options/option">
														<option>
															<!-- verificar value-->
															<xsl:choose>
																<xsl:when test="@value=''">
																	<xsl:attribute name="value">
																		<xsl:value-of select="."/>
																	</xsl:attribute>
																</xsl:when>
																<xsl:otherwise>
																	<xsl:attribute name="value">
																		<xsl:value-of select="@value"/>
																	</xsl:attribute>
																</xsl:otherwise>
															</xsl:choose>
															<xsl:if test="@selected='true'">
																<xsl:attribute name="selected">true</xsl:attribute>
															</xsl:if>
															<xsl:value-of select="."/>
														</option>
													</xsl:for-each>
													<!-- / OPCIONES -->
												</select>
											</xsl:when>
											<!-- / LISTADO listctrl -->

											<!-- checkbox -->
											<xsl:when test="inputctrl/@type='checkbox'">
												<!-- OPCIONES -->
												<xsl:variable name="name_check" select="inputctrl/@name"/>
												<xsl:variable name="editable_check" select="@editable"/>
												<xsl:for-each select="inputctrl/options/option">
													<div class="check_option">

														<input id="{$id_element}" type="checkbox" name="{$name_check}" title="{tooltip}" tabindex="{number($tab_order) + position()}" class="field_elem">
															<!-- verificar value-->
															<xsl:choose>
																<xsl:when test="@value=''">
																	<xsl:attribute name="value">
																		<xsl:value-of select="."/>
																	</xsl:attribute>
																</xsl:when>
																<xsl:otherwise>
																	<xsl:attribute name="value">
																		<xsl:value-of select="@value"/>
																	</xsl:attribute>
																</xsl:otherwise>
															</xsl:choose>
															<xsl:if test="@selected='true'">
																<xsl:attribute name="checked">checked</xsl:attribute>
															</xsl:if>
															<xsl:if test="$editable_check='false'">
																<xsl:attribute name="disable">true</xsl:attribute>
															</xsl:if>
														</input>
														<div class="checkbox_label">
															<xsl:value-of select="."/>
														</div>
													</div>
												</xsl:for-each>
												<!-- / OPCIONES -->
											</xsl:when>
											<!-- / checkbox -->

											<!-- radiobutton -->
											<xsl:when test="inputctrl/@type='radiobutton'">
												<xsl:variable name="name_radio" select="inputctrl/@name"/>
												<xsl:variable name="editable_radio" select="@editable"/>
												<!-- OPCIONES -->
												<xsl:for-each select="inputctrl/options/option">

													<div class="radio_option">
														<input type="radio" name="{$name_radio}" id="{$id_element}" title="{tooltip}" tabindex="{number($tab_order) + position()}" class="field_elem">
															<!-- verificar value-->
															<xsl:choose>
																<xsl:when test="@value=''">
																	<xsl:attribute name="value">
																		<xsl:value-of select="."/>
																	</xsl:attribute>
																</xsl:when>
																<xsl:otherwise>
																	<xsl:attribute name="value">
																		<xsl:value-of select="@value"/>
																	</xsl:attribute>
																</xsl:otherwise>
															</xsl:choose>
															<xsl:if test="@selected='true'">
																<xsl:attribute name="checked">checked</xsl:attribute>
															</xsl:if>
															<xsl:if test="$editable_radio='false'">
																<xsl:attribute name="disable">true</xsl:attribute>
															</xsl:if>
														</input>
														<div class="radiobutton_label">
															<xsl:value-of select="."/>
														</div>
													</div>
												</xsl:for-each>
												<!-- / OPCIONES -->
											</xsl:when>
											<!-- / radiobutton -->

											<!-- textarea -->
											<xsl:when test="inputctrl/@type='textarea'">
												<textarea id="{$id_element}" name="{inputctrl/@name}" maxlength="{validator/@max}" cols="40" rows="5" placeholder="{tooltip}" title="{tooltip}" tabindex="{$tab_order}" class="field_elem">
													<xsl:if test="@editable='false'">
														<xsl:attribute name="readonly">true</xsl:attribute>
													</xsl:if>
													<xsl:value-of select="inputctrl/defaultvalue"/>
												</textarea>
											</xsl:when>
											<!-- / textarea -->

											<!-- calendar -->
											<xsl:when test="inputctrl/@type='calendar'">
												<xsl:element name="input">
													<xsl:attribute name="id">
														<xsl:value-of select="$id_element"/>
													</xsl:attribute>
													<xsl:attribute name="title">
														<xsl:value-of select="tooltip"/>
													</xsl:attribute>
													<xsl:attribute name="tabindex">
														<xsl:value-of select="$tab_order"/>
													</xsl:attribute>
													<xsl:attribute name="class">field_elem</xsl:attribute>
													<xsl:attribute name="type">text</xsl:attribute>
													<xsl:attribute name="calendar">true</xsl:attribute>
													<xsl:attribute name="format">
														<xsl:call-template name="format_d">
															<xsl:with-param name="format_in" select="validator/@format"/>
														</xsl:call-template>
													</xsl:attribute>
													<xsl:attribute name="language">
														<xsl:value-of select="inputctrl/language"/>
													</xsl:attribute>
													<xsl:attribute name="name">
														<xsl:value-of select="inputctrl/@name"/>
													</xsl:attribute>
													<xsl:attribute name="input_type">
														<xsl:value-of select="inputctrl/@type"/>
													</xsl:attribute>
													<xsl:attribute name="value">
														<xsl:value-of select="inputctrl/defaultvalue"/>
													</xsl:attribute>
													<xsl:if test="@editable='false'">
														<xsl:attribute name="readonly">true</xsl:attribute>
													</xsl:if>
												</xsl:element>
											</xsl:when>
											<!-- / calendar -->

											<!-- newsletter -->
											<xsl:when test="inputctrl/@type='newsletter'">
												<xsl:value-of disable-output-escaping="yes" select="inputctrl/defaultvalue"/>
											</xsl:when>
											<!-- / newsletter -->

											<!-- otp_button -->
											<xsl:when test="inputctrl/@type='otp_button'">
												<xsl:variable name="id_phone">
													<xsl:value-of select="$id_form"/>_<xsl:value-of select="inputctrl/@id_phone"/></xsl:variable>
												<button id="{$id_element}" type="button" id_modal="{$id_element}_mod" confirmado="true" title="{tooltip}" placeholder="{tooltip}" tabindex="{$tab_order}" class="field_elem">
													<xsl:attribute name="onclick">otp_generation('<xsl:value-of select="$id_phone"/>', '<xsl:value-of select="$ok_label"/>');</xsl:attribute>
													<xsl:value-of disable-output-escaping="yes" select="inputctrl/defaultvalue"/>
												</button>
											</xsl:when>
											<!-- / otp_button -->


											<xsl:otherwise>
												<input id="{$id_element}" name="{inputctrl/@name}" type="{inputctrl/@type}" value="{inputctrl/defaultvalue}" id_modal="{$id_element}_mod" confirmado="true" title="{tooltip}" placeholder="{tooltip}"
												       tabindex="{$tab_order}" class="field_elem">
													<xsl:if test="@editable='false'">
														<xsl:attribute name="readonly">true</xsl:attribute>
													</xsl:if>
													<xsl:if test="@needconfirm='true'">
														<xsl:attribute name="iter_needconfirm">true</xsl:attribute>
														<xsl:attribute name="onkeyup">activar_confirm(this);</xsl:attribute>
													</xsl:if>
												</input>
											</xsl:otherwise>
										</xsl:choose>
									</div>
									<!-- / ELEMENTO  '#<xsl:value-of select="$id_element"/>_mod', '#<xsl:value-of select="$id_element"/>' -->


									<!-- LABEL POSTERIOR -->
									<div class="label_pos">
										<xsl:if test="labelafter/textlabel!=''">
											<xsl:choose>
												<xsl:when test="labelafter/textlabel/@linkurl!=''">
													<a href="{labelafter/textlabel/@linkurl}" target="{labelafter/textlabel/@linktarget}">
														<xsl:value-of select="labelafter/textlabel"/>
													</a>
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="labelafter/textlabel"/>
												</xsl:otherwise>
											</xsl:choose>
										</xsl:if>
									</div>
									<!-- / LABEL POSTERIOR -->

									<!-- CONFIRMAR   ( sin nombre para no enviar por form ) -->
									<xsl:if test="@needconfirm='true'">
										<div class="confirm_field">
											<div class="element_form">
												<input id="{$id_element}_rep" type="{inputctrl/@type}" placeholder="{tooltip}" value="" class="field_elem_rep" tabindex="{number($tab_order) + 1}" disabled="true"/>
											</div>
										</div>
									</xsl:if>

									<!-- / CONFIRMAR   -->


									<!-- BOTON REPETIR CAMPOS -->
									<xsl:if test="$repetir='true'">
										<div class="repetir_elem">
											<input type="button" value="" class="btt_repetir" onclick="repetir_field('field_{$id_element}', 'repetidos_{$id_form}_{inputctrl/@id}', '{$id_element}','{$confir_label}','')"/>
										</div>
									</xsl:if>
									<!-- / BOTON REPETIR CAMPOS -->
								</div>
								<!-- DIV PARA REPETIR CAMPOS -->
								<xsl:if test="$repetir='true'">
									<div id="repetidos_{$id_element}" class="rep_elements"></div>
								</xsl:if>
								<!-- / DIV PARA REPETIR CAMPOS -->

								<!-- REPETIMOS LOS CAMPOS QUE VIENEN EN EL XML-->
								<xsl:for-each select="inputctrl">
									<xsl:if test="position()&gt;1">
										<script type="text/javascript">jQryIter(document).ready(function() {repetir_field('field_<xsl:value-of select="$id_element"/>', 'repetidos_<xsl:value-of select="$id_form"/>_<xsl:value-of select="@id"/>', '<xsl:value-of select="$id_element"/>','<xsl:value-of select="$confir_label"/>','<xsl:value-of select="defaultvalue"/>');});</script>
									</xsl:if>
								</xsl:for-each>
								<!-- / REPETIMOS LOS CAMPOS QUE VIENEN EN EL XML-->

								<!-- INICALIZAMOS LLAMADA DATEPICKET -->
								<xsl:if test="inputctrl/@type='calendar'">
									<script type="text/javascript">jQryIter(function() {
										var f_min = dame_fmin('<xsl:value-of select="inputctrl/mindate"/>');
										var f_max = dame_fmax('<xsl:value-of select="inputctrl/maxdate"/>');
										var f_range = dame_rango(f_max, f_min);
										
										jQryIter("#<xsl:value-of select="$id_form"/>_<xsl:value-of select="inputctrl/@id"/>").datepicker(jQryIter.extend({
											autoSize: true, 
											changeYear: true, 
											yearRange: f_range, 
											maxDate : f_max, 
											minDate: f_min, 
											dateFormat: "<xsl:call-template name="format_d"><xsl:with-param name="format_in" select="validator/@format"/></xsl:call-template>"}, 
											jQryIter.datepicker.regional["<xsl:value-of select="inputctrl/language"/>"]
										));
						
									});</script>
								</xsl:if>
								<!-- / INICALIZAMOS LLAMADA DATEPICKET -->
							</xsl:for-each>
							<!-- FIN CAMPOS -->
						</div>
						<!-- BOTONES DE FORMULARIO -->
						<div class="btts_forms">
							<!-- BOTON SUBMIT EN TODAS-->
							<xsl:if test="$use-submit='all'">
								<div class="submit_form">
									<input type="submit" value="{$submitlabel}"/>
								</div>

								<!-- BOTON RESET -->
								<xsl:if test="$resetlabel!=''">
									<div class="reset_form">
										<input type="reset" value="{$resetlabel}"/>
									</div>
								</xsl:if>
								<!-- / BOTON RESET -->
							</xsl:if>
							<!-- / BOTON SUBMIT EN TODAS-->

							<!-- BOTON SUBMIT AL FINAL -->
							<xsl:if test="$use-submit='last'">
								<xsl:if test="count(../fieldsgroup) = position()">
									<input type="submit" value="{$submitlabel}"/>
									<!-- BOTON RESET -->
									<xsl:if test="$resetlabel!=''">
										<div class="reset_form">
											<input type="reset" value="{$resetlabel}"/>
										</div>
									</xsl:if>
									<!-- / BOTON RESET -->
								</xsl:if>
							</xsl:if>
							<!-- / BOTON SUBMIT AL FINAL -->
						</div>
						<!-- / BOTONES DE FORMULARIO -->
					</div>
					<script type="text/javascript">var cont_pages_<xsl:value-of select="$id_form"/> = <xsl:value-of select="position()"/>;</script>
				</xsl:for-each>
				<!-- FIN GRUPOS -->


				<!-- RECAPTHA -->
				<xsl:if test="$usecaptcha='true'">
					<div class="g-recaptcha" data-iterid="captcha_{$id_form}" data-sitekey="{$captchakey}" data-theme="{$captchatheme}" data-size="{$captchasize}"></div>
				</xsl:if>
				<!-- / RECAPTHA -->
			</div>
			<!-- pages -->

			<xsl:if test="$navigation='page'">
				<div id="paginado_{$id_form}" class="form_paginate">
					<div class="gigantic pagination" id="pag_{$id_form}">
						<!-- a href="#" class="first" data-action="first">Primer</a -->
						<a href="#" class="previous" data-action="previous">
							<xsl:value-of select="$prevlabel"/>
						</a>
						<!--input type="text" readonly="readonly" data-max-page="40" /-->
						<a href="#" class="next" data-action="next">
							<xsl:value-of select="$nextlabel"/>
						</a>
						<!-- a href="#" class="last" data-action="last">Ulti;</a -->
					</div>
				</div>
			</xsl:if>

			<script type="text/javascript">
				<xsl:if test="$navigation='tab'">jQryIter(function() {jQryIter("#pages").tabs();});</xsl:if>

				<xsl:if test="$navigation='page'">navigator_form('<xsl:value-of select="$id_form"/>', cont_pages_<xsl:value-of select="$id_form"/>);</xsl:if>

				<!-- SCRIPT DE COMPROBACIONES -->function validarform_<xsl:value-of select="$id_form"/>(){
					try{
						var todos_ok = true;
				<xsl:for-each select="fieldsgroup">
					<xsl:for-each select="field">
						<xsl:choose>
							<xsl:when test="validator/@max!=''">var _max = <xsl:value-of select="validator/@max"/>;</xsl:when>
							<xsl:otherwise>var _max = 0;</xsl:otherwise>
						</xsl:choose>
						<xsl:choose>
							<xsl:when test="validator/@min!=''">var _min = <xsl:value-of select="validator/@min"/>;</xsl:when>
							<xsl:otherwise>var _min = 0;</xsl:otherwise>
						</xsl:choose>
						<xsl:choose>
							<xsl:when test="validator/@type='dateformat'">var _format = "<xsl:call-template name="format_d"><xsl:with-param name="format_in" select="validator/@format"/></xsl:call-template>";</xsl:when>
							<xsl:when test="validator/@type='regexp'">var _format = <xsl:value-of select="validator/@format"/>;</xsl:when>
							<xsl:when test="validator/@type='numberrange'">
								<xsl:if test="validator/@format!=''">var _format = <xsl:value-of select="validator/@format"/>;</xsl:if>
								<xsl:if test="validator/@format=''">var _format = "";</xsl:if>
							</xsl:when>
							<xsl:otherwise>var _format = "<xsl:value-of select="validator/@format"/>";</xsl:otherwise>
						</xsl:choose>

						<xsl:if test="inputctrl/@type!='hidden'">if(validar_field('#<xsl:value-of select="$id_form"/>', '<xsl:value-of select="$id_form"/>_<xsl:value-of select="inputctrl/@id"/>','<xsl:value-of select="inputctrl/@name"/>', '<xsl:value-of select="@fieldtype"/>', <xsl:value-of select="@required"/>, <xsl:value-of select="@needconfirm"/>, '<xsl:value-of select="validator/@type"/>', _format, _max, _min) == false){todos_ok = false;}</xsl:if>
					</xsl:for-each>
				</xsl:for-each>if(todos_ok){
							return true;
						}
						else{
							jQryIter( "#error_<xsl:value-of select="$id_form"/>").dialog({ modal: true },{ width: 360 }, { height: 180 });
							jQryIter( "#error_<xsl:value-of select="$id_form"/>" ).dialog({ dialogClass: "dialog_error" });
							jQryIter( "#error_<xsl:value-of select="$id_form"/>" ).dialog({ closeText: '<xsl:value-of select="/form/@cancellabel"/>' });
							jQryIter.resetCaptcha("captcha_<xsl:value-of select="$id_form"/>");
							return false;
						}
					}
					catch(err){
						return false;
					}
				}</script>
		</form>

		<div id="error_{$id_form}" class="error" style="display:none;">
			<div class="label_error">
				<xsl:value-of select="invalidfieldmsg"/>
			</div>
			<div class="btts_error">
				<input type="button" value="{$cancel_label}" class="btt_repetir" onclick="jQryIter('#error_{$id_form}').dialog('close'); "/>
			</div>
		</div>

		<div id="other_{$id_form}" class="error other" style="display:none;">
			<div class="label_error">
				<!-- Fill With jQuery before Show dialog -->
			</div>
			<div class="btts_error">
				<!--<input type="button" value="Aceptar" class="btt_repetir other" onclick="jQryIter('#other_{$id_form}').dialog('close'); "/>-->
			</div>
		</div>
	</xsl:template>
</xsl:stylesheet><!-- Stylus Studio meta-information - (c) 2004-2009. Progress Software Corporation. All rights reserved.

<metaInformation>
	<scenarios>
		<scenario default="yes" name="Scenario1" userelativepaths="no" externalpreview="no" url="file:///c:/Users/pedroruiz/Desktop/form.xml" htmlbaseurl="" outputurl="" processortype="saxon8" useresolver="yes" profilemode="0" profiledepth=""
		          profilelength="" urlprofilexml="" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext="" validateoutput="no" validator="internal"
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