package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format header data encoded for training. Normally all training data for the header-medical-report model
 * should be in this unique format (which replaces for instance the CORA format).
 * Segmentation of tokens must be
 * identical as the one from pdf2xml files so that training and online input tokens are aligned.
 * <p>
 * This class is adapted from TEIHeaderSaxParser class (@author Patrice Lopez)
 * <p>
 * Tanti, 2020
 */
public class TEIHeaderMedicalSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;

    private String fileName = null;
    //public TreeMap<String, String> pdfs = null;
    private String pdfName = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> endTags = Arrays.asList("idno", "titlePart", "dateline", "date", "time", "medic", "patient", "affiliation", "address", "email", "phone",
        "fax", "ptr", "note");

    private List<String> intermediaryTags = Arrays.asList("byline", "front", "lb", "tei", "teiHeader", "fileDesc", "text", "person",
        "docTitle", "p");

    private List<String> ignoredTags = Arrays.asList("page", "institution", "location", "title");

    public TEIHeaderMedicalSaxParser() {
        labeled = new ArrayList<String>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void setFileName(String name) {
        fileName = name;
    }

    public String getPDFName() {
        return pdfName;
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        if (endTags.contains(qName)) {
            writeData();
            accumulator.setLength(0);
        } else if (qName.equals("front")) {
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

        if (qName.equals("titlePart")) {
            currentTag = "<title>";
        } else if (qName.equals("idno")) {
            currentTag = "<docnum>";
        } else if (qName.equals("dateline")) {
            currentTag = "<dateline>";
        } else if (qName.equals("date")) {
            currentTag = "<date>";
        } else if (qName.equals("time")) {
            currentTag = "<time>";
        } else if (qName.equals("affiliation")) {
            currentTag = "<affiliation>";
            accumulator.setLength(0);
        } else if (qName.equals("institution")) {
            currentTag = "<institution>";
        } else if (qName.equals("address")) {
            currentTag = "<address>";
            accumulator.setLength(0);
        } else if (qName.equals("medic")) {
            currentTag = "<medic>";
        } else if (qName.equals("patient")) {
            currentTag = "<patient>";
        } /*else if (qName.equals("person")) {
            int length = atts.getLength();
            currentTag = "<other>";

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("medic")) {
                            currentTag = "<medic>";
                        } else if (value.equals("patient")) {
                            currentTag = "<patient>";
                        }
                        else
                            currentTag = "<other>";
                    }
                }
            }
        }*/ else if (qName.equals("email")) {
            currentTag = "<email>";
        } else if (qName.equals("phone")) {
            currentTag = "<phone>";
        } else if (qName.equals("fax")) {
            currentTag = "<fax>";
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
        } else if (qName.equals("note")) {
            int length = atts.getLength();
            currentTag = "<other>";
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("document_type") || value.equals("doctype") || value.equals("docType") ||
                            value.equals("documentType") || value.equals("articleType")) {
                            currentTag = "<doctype>";
                        } else
                            currentTag = "<other>";
                    }
                } else
                    currentTag = "<other>";
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