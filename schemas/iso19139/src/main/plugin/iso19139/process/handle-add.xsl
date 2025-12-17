<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                  xmlns:gco="http://www.isotc211.org/2005/gco"
                  xmlns:gmd="http://www.isotc211.org/2005/gmd"
                  xmlns:gmx="http://www.isotc211.org/2005/gmx"
                  xmlns:xlink="http://www.w3.org/1999/xlink"
                  xmlns:geonet="http://www.fao.org/geonetwork"
                  exclude-result-prefixes="#all">

  <!-- Insert a Handle PID in the metadata record.

  The default mode here is to add the Handle as a resource identifier using the resolver URL.
  -->
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="handle"
             select="''"/>
  <xsl:param name="handleProxy"
             select="'https://hdl.handle.net/'"/>

  <xsl:template match="gmd:identificationInfo[1]/*/gmd:citation/*" priority="2">
    <xsl:copy>
      <xsl:copy-of select="@*"/>

      <xsl:copy-of select="gmd:title
                           |gmd:alternateTitle
                           |gmd:date
                           |gmd:edition
                           |gmd:editionDate
                           |gmd:identifier
                          "/>
      <xsl:copy-of select="gmd:identifier[not(.//gmx:Anchor[contains(@xlink:href, 'hdl.handle.net')])
                                     and not(.//gco:CharacterString[contains(text(), 'hdl.handle.net')])]"/>

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

  <!-- Do a copy of every nodes and attributes -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Remove geonet:* elements. -->
  <xsl:template match="geonet:*" priority="2"/>

</xsl:stylesheet>
