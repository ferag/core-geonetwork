<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                  xmlns:gco="http://www.isotc211.org/2005/gco"
                  xmlns:gmd="http://www.isotc211.org/2005/gmd"
                  xmlns:gmx="http://www.isotc211.org/2005/gmx"
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
                select="count(//gmd:identificationInfo/*/gmd:citation/*/
                              gmd:identifier/*/gmd:code[
                                contains(*/text(), 'hdl.handle.net')
                                or contains(*/text(), 'handle.net')
                                or contains(*/@xlink:href, 'handle.net')]) > 0"/>

  <xsl:template match="gmd:identificationInfo[1]/*/gmd:citation/*[not($isHandleAlreadySet)]"
                priority="2">
    <xsl:copy>
      <xsl:copy-of select="@*"/>

      <xsl:copy-of select="gmd:title
                           |gmd:alternateTitle
                           |gmd:date
                           |gmd:edition
                           |gmd:editionDate
                           |gmd:identifier
                          "/>
      <gmd:identifier>
        <gmd:MD_Identifier>
          <gmd:code>
            <gmx:Anchor xlink:href="{concat($handleProxy, $handle)}">
              <xsl:value-of select="$handle"/>
            </gmx:Anchor>
          </gmd:code>
        </gmd:MD_Identifier>
      </gmd:identifier>

      <xsl:copy-of select="gmd:citedResponsibleParty
                           |gmd:presentationForm
                           |gmd:series
                           |gmd:otherCitationDetails
                           |gmd:collectiveTitle
                           |gmd:ISBN
                           |gmd:ISSN
                          "/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="gmd:distributionInfo[not($isHandleAlreadySet) and position() = 1]"
                priority="2">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <gmd:MD_Distribution>
        <xsl:apply-templates select="*/@*"/>
        <xsl:apply-templates select="*/gmd:distributionFormat"/>
        <xsl:apply-templates select="*/gmd:distributor"/>
        <xsl:apply-templates select="*/gmd:transferOptions"/>
        <gmd:transferOptions>
          <gmd:MD_DigitalTransferOptions>
            <gmd:onLine>
              <gmd:CI_OnlineResource>
                <gmd:linkage>
                  <gmd:URL><xsl:value-of select="concat($handleProxy, $handle)"/></gmd:URL>
                </gmd:linkage>
                <gmd:protocol>
                  <gco:CharacterString><xsl:value-of select="$handleProtocol"/></gco:CharacterString>
                </gmd:protocol>
                <gmd:name>
                  <gco:CharacterString><xsl:value-of select="$handleName"/></gco:CharacterString>
                </gmd:name>
              </gmd:CI_OnlineResource>
            </gmd:onLine>
          </gmd:MD_DigitalTransferOptions>
        </gmd:transferOptions>
      </gmd:MD_Distribution>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="geonet:*" priority="2"/>

</xsl:stylesheet>
