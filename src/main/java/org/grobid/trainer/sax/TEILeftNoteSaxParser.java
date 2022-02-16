package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format left-note data encoded for training. Normally all training data for the left-note-medical-report model
 * should be in this unique format (which replaces for instance the CORA format).
 * Segmentation of tokens must be
 * identical as the one from pdf2xml files so that training and online input tokens are aligned.
 * <p>
 * <p>
 * Tanti, 2020
 */
public class TEILeftNoteSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;

    private String fileName = null;
    //public TreeMap<String, String> pdfs = null;
    private String pdfName = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> endTags = Arrays.asList("idno", "address", "orgName", "email", "phone", "fax", "ptr", "medic", "note");

    private List<String> intermediaryTags = Arrays.asList("byline", "org", "lb", "tei", "teiHeader","listOrg",
        "fileDesc", "text", "person", "p");

    private List<String> ignoredTags = Arrays.asList("page", "location", "web");

    public TEILeftNoteSaxParser() {
        labeled = new ArrayList<String>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void setFileName(String name) {fileName = name;}

    public String getPDFName() {
        return pdfName;
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(String uri,
                           String localName,
                           String qName) throws SAXException {
        if (endTags.contains(qName)) {
            writeData();
            accumulator.setLength(0);
        } else if (qName.equals("org")) {
            // write remaining test as <other>
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    currentTag = "<other>";
                    writeData();
                }
            }
            accumulator.setLength(0);
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
        } else if (ignoredTags.contains(qName)) {
            // do nothing
        } else {
            System.out.println(" **** Warning **** Unexpected closing tag " + qName);
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
        throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" ");
        } else {
            // add acumulated text as <other>
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    currentTag = "<other>";
                    writeData();
                }
            }
            accumulator.setLength(0);
        }

        if (qName.equals("affiliation")) {
            currentTag = "<affiliation>";
            accumulator.setLength(0);
        } else if (qName.equals("idno")) {
            currentTag = "<idno>";
        } else if (qName.equals("orgName")) {
            int length = atts.getLength();
            if (length > 0) {
                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("ghu")) {
                                currentTag = "<ghu>";
                            } else if (value.equals("chu")) {
                                currentTag = "<chu>";
                            } else if (value.equals("dmu")) {
                                currentTag = "<dmu>";
                            } else if (value.equals("pole")) {
                                currentTag = "<pole>";
                            } else if (value.equals("hospital")) {
                                currentTag = "<hospital>";
                            } else if (value.equals("center")) {
                                currentTag = "<center>";
                            } else if (value.equals("service")) {
                                currentTag = "<service>";
                            } else if (value.equals("department")) {
                                currentTag = "<department>";
                            } else if (value.equals("sub")) {
                                currentTag = "<sub>";
                            } else
                                currentTag = "<organization>"; // we take org with the type other than center, service, department or administration as <org>
                        }
                    }
                }
            } else
                currentTag = "<organization>"; // we take org without any attribute as in the header model
            accumulator.setLength(0);
        }  else if (qName.equals("address")) {
            currentTag = "<address>";
            accumulator.setLength(0);
        } else if (qName.equals("phone")) {
            currentTag = "<phone>";
        } else if (qName.equals("fax")) {
            currentTag = "<fax>";
        } else if (qName.equals("email")) {
            currentTag = "<email>";
        } else if (qName.equals("ptr")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("web")) {
                            currentTag = "<web>";
                        }
                    }
                } else
                    currentTag = "<other>";
            }
        } else if (qName.equals("medic")) {
            currentTag = "<medic>";
        } else if (qName.equals("note")) {
            int length = atts.getLength();

            if (length == 0) {
                currentTag = "<note>";
            } else {
                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if ((name != null) && (value != null)) {
                        if (name.equals("type")) {
                            if (value.equals("short")) {
                                currentTag = "<note>";
                            }
                        }
                    }
                }
            }
        } else if (qName.equals("fileDesc")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("xml:id")) {
                        pdfName = value;
                    }
                }
            }
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
        } else if (ignoredTags.contains(qName)) {
            // do nothing
            currentTag = "<other>";
        } else {
            System.out.println("Warning: Unexpected starting tag " + qName);
            currentTag = "<other>";
        }
    }

    private void writeData() {
        if (currentTag == null) {
            return;
        }

        String text = getText();
        // we segment the text
        StringTokenizer st = new StringTokenizer(text, TextUtilities.delimiters, true);
        boolean begin = true;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken().trim();
            if (tok.length() == 0)
                continue;

            String content = tok;
            int i = 0;
            if (content.length() > 0) {
                if (begin) {
                    labeled.add(content + " I-" + currentTag + "\n");
                    begin = false;
                } else {
                    labeled.add(content + " " + currentTag + "\n");
                }
            }
            begin = false;
        }
        accumulator.setLength(0);
    }

}