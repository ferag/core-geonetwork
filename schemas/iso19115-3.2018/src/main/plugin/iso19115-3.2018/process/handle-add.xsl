<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/2.0"
                xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
                xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/2.0"
                xmlns:gcx="http://standards.iso.org/iso/19115/-3/gcx/1.0"
                xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
                xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
                xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:geonet="http://www.fao.org/geonetwork"
                exclude-result-prefixes="#all">

  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="handle"
             select="''"/>
  <xsl:param name="handleProxy"
             select="'https://hdl.handle.net/'"/>
  <xsl:param name="handleProtocol" select="'HANDLE'"/>
  <xsl:param name="handleName" select="'Handle Persistent Identifier (PID)'"/>

  <xsl:variable name="isHandleAlreadySet"
                select="count(//mdb:identificationInfo/*/mri:citation/*/
                              cit:identifier/*/mcc:code[
                                contains(*/text(), 'hdl.handle.net')
                                or contains(*/text(), 'handle.net')
                                or contains(*/@xlink:href, 'handle.net')]) > 0"/>

  <xsl:template match="mdb:identificationInfo[1]/*/mri:citation/*[not($isHandleAlreadySet)]"
                priority="2">
    <xsl:copy>
      <xsl:copy-of select="@*"/>

      <xsl:copy-of select="cit:title
                           |cit:alternateTitle
                           |cit:date
                           |cit:edition
                           |cit:editionDate
                           |cit:identifier
                          "/>
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

  <xsl:template match="mdb:distributionInfo[not($isHandleAlreadySet) and position() = 1]"
                priority="2">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <mrd:MD_Distribution>
        <xsl:apply-templates select="*/@*"/>
        <xsl:apply-templates select="*/mrd:distributionFormat"/>
        <xsl:apply-templates select="*/mrd:distributor"/>
        <xsl:apply-templates select="*/mrd:transferOptions"/>
        <mrd:transferOptions>
          <mrd:MD_DigitalTransferOptions>
            <mrd:onLine>
              <cit:CI_OnlineResource>
                <cit:linkage>
                  <gco:CharacterString><xsl:value-of select="concat($handleProxy, $handle)"/></gco:CharacterString>
                </cit:linkage>
                <cit:protocol>
                  <gco:CharacterString><xsl:value-of select="$handleProtocol"/></gco:CharacterString>
                </cit:protocol>
                <cit:name>
                  <gco:CharacterString><xsl:value-of select="$handleName"/></gco:CharacterString>
                </cit:name>
              </cit:CI_OnlineResource>
            </mrd:onLine>
          </mrd:MD_DigitalTransferOptions>
        </mrd:transferOptions>
      </mrd:MD_Distribution>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="geonet:*" priority="2"/>

</xsl:stylesheet>
