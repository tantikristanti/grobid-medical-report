package org.grobid.core.utilities;

import nu.xom.Element;
import org.grobid.core.document.xml.XmlBuilderUtils;

public class XmlBuilderGrobid extends XmlBuilderUtils {
    public static Element teiIDElement(String name) {
        return new Element(name, "http://www.tei-c.org/ns/1.0");
    }
}
