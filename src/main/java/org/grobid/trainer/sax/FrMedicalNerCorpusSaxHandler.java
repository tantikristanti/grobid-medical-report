package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/* Callback methods to capture the events from the *.training.french.medical.ner.tei.xml files, join them and write them to a new file
* */

public class FrMedicalNerCorpusSaxHandler extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(FrenchCorpusSaxHandler.class);
    private XMLStreamWriter out;
    private boolean dumping; // we only write the root <corpus> once

    public FrMedicalNerCorpusSaxHandler(XMLStreamWriter out) {
        this.out = out;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)  {
        // we write it once at the beginning, so if find it again and again, we ignore
        if ("corpus".equals(qName)) {
            dumping = true;
        } else {
            try {
                out.writeStartElement(qName);
                // take the attributes and the values for <document>, <p> and entities
                if (qName.equalsIgnoreCase("document")) {
                    String docId = attributes.getValue("name");
                    if (docId != null && docId.length()>0) {
                        out.writeAttribute("name", docId);
                    }

                    String docLang = attributes.getValue("xml:lang");
                    if (docLang != null && docLang.length()>0) {
                        out.writeAttribute("xml:lang", docLang);
                    }
                }
                if (qName.equalsIgnoreCase("p")) {
                    String paragraphId = attributes.getValue("xml:id");
                    if (paragraphId != null && paragraphId.length()>0) {
                        out.writeAttribute("xml:id", paragraphId);
                    }
                }
                if (qName.equalsIgnoreCase("enamex")) {
                    String type = attributes.getValue("type");
                    out.writeAttribute("type", type);
                }
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("corpus".equals(qName)) {
            dumping = false;
        } else {
            try {
                out.writeEndElement();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (!dumping) {
            return;
        }
        try {
            out.writeCharacters(ch, start, length);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

}
