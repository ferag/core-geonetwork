package org.fao.geonet.handle.client;

import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.Namespace;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HandleXslTest {

    private static final Namespace GMD = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private static final Namespace GMX = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    @Test
    public void addsHandleWhenMissingAndReplacesExisting() throws Exception {
        Path styleSheet = Paths.get("../schemas/iso19139/src/main/plugin/iso19139/process/handle-add.xsl")
            .toAbsolutePath().normalize();
        Element xml = Xml.loadString(buildMetadata("https://hdl.handle.net/old/123"), false);

        Map<String, Object> params = new HashMap<>();
        params.put("handle", "prefix/new-456");
        params.put("handleProxy", "https://hdl.handle.net/");

        Element transformed = Xml.transform(xml, styleSheet, params);
        List<Element> identifiers = transformed.getChild("identificationInfo", GMD)
            .getChild("MD_DataIdentification", GMD)
            .getChild("citation", GMD)
            .getChild("CI_Citation", GMD)
            .getChildren("identifier", GMD);

        assertEquals("One identifier retained after replacing existing handle", 1, identifiers.size());
        Element anchor = identifiers.get(0).getChild("MD_Identifier", GMD)
            .getChild("code", GMD).getChild("Anchor", GMX);
        assertTrue(anchor.getAttributeValue("href", XLINK).contains("prefix/new-456"));
        assertEquals("prefix/new-456", anchor.getTextTrim());
    }

    private String buildMetadata(String handleHref) {
        return "<gmd:MD_Metadata xmlns:gmd='http://www.isotc211.org/2005/gmd' " +
            "xmlns:gco='http://www.isotc211.org/2005/gco' " +
            "xmlns:gmx='http://www.isotc211.org/2005/gmx' " +
            "xmlns:xlink='http://www.w3.org/1999/xlink'>" +
            "<gmd:identificationInfo>" +
            "  <gmd:MD_DataIdentification>" +
            "    <gmd:citation>" +
            "      <gmd:CI_Citation>" +
            "        <gmd:identifier>" +
            "          <gmd:MD_Identifier>" +
            "            <gmd:code>" +
            "              <gmx:Anchor xlink:href='" + handleHref + "'>old</gmx:Anchor>" +
            "            </gmd:code>" +
            "          </gmd:MD_Identifier>" +
            "        </gmd:identifier>" +
            "      </gmd:CI_Citation>" +
            "    </gmd:citation>" +
            "  </gmd:MD_DataIdentification>" +
            "</gmd:identificationInfo>" +
            "</gmd:MD_Metadata>";
    }
}

