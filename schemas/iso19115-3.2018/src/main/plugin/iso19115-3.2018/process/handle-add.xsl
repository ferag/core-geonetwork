<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                  xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
                  xmlns:gcx="http://standards.iso.org/iso/19115/-3/gcx/1.0"
                  xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
                  xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
                  xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
                  xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/2.0"
                  xmlns:lan="http://standards.iso.org/iso/19115/-3/lan/1.0"
                  xmlns:xlink="http://www.w3.org/1999/xlink"
                  xmlns:geonet="http://www.fao.org/geonetwork"
                  exclude-result-prefixes="#all">

  <!-- Insert a Handle PID in the metadata record as a citation identifier. -->
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="handle"
             select="''"/>
  <xsl:param name="handleProxy"
             select="'https://hdl.handle.net/'"/>

  <xsl:template match="mdb:identificationInfo[1]/*/mri:citation/*" priority="2">
    <xsl:copy>
      <xsl:copy-of select="@*"/>

      <xsl:copy-of select="cit:title
                           |cit:alternateTitle
                           |cit:date
                           |cit:edition
                           |cit:editionDate
                           |cit:identifier
                          "/>
      <xsl:copy-of select="cit:identifier[not(.//gcx:Anchor[contains(@xlink:href, 'hdl.handle.net')])
                                     and not(.//gco:CharacterString[contains(text(), 'hdl.handle.net')])]"/>

      <cit:identifier>
        <mcc:MD_Identifier>
          <mcc:code>
            <gcx:Anchor xlink:href="{concat($handleProxy, $handle)}">
              <xsl:value-of select="$handle"/>
            </gcx:Anchor>
          </mcc:code>
        </mcc:MD_Identifier>
      </cit:identifier>

      <xsl:copy-of select="cit:citedResponsibleParty
                           |cit:presentationForm
                           |cit:series
                           |cit:otherCitationDetails
                           |cit:collectiveTitle
                           |cit:ISBN
                           |cit:ISSN
                          "/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="geonet:*" priority="2"/>

</xsl:stylesheet>
