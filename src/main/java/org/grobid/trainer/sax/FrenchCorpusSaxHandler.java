package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class FrenchCorpusSaxHandler extends DefaultHandler {
    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String currentTag = null;

    private String fileName = null;
    private String pdfName = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> endTags = Arrays.asList("ENAMEX");

    private List<String> intermediaryTags = Arrays.asList("subcorpus", "document", "p");

    public FrenchCorpusSaxHandler() {
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

        if (qName.equals("ENAMEX")) {
            int length = atts.getLength();
            currentTag = "<other>";

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("ANAT")) {
                            currentTag = "<anatomy>";
                        } else if (value.equals("DEVI")) {
                            currentTag = "<drug>";
                        } else if (value.equals("DISO")) {
                            currentTag = "<disorder>";
                        } else if (value.equals("CHEM")) {
                            currentTag = "<drug>";
                        } else if (value.equals("LIVB")) {
                            currentTag = "<living>";
                        } else if (value.equals("GEOG")) {
                            currentTag = "<location>";
                        } else if (value.equals("OBJC")) {
                            currentTag = "<object>";
                        } else if (value.equals("PHEN")) {
                            currentTag = "<phenomena>";
                        } else if (value.equals("PHYS")) {
                            currentTag = "<physiology>";
                        } else if (value.equals("PROC")) {
                            currentTag = "<procedure>";
                        } else
                            currentTag = "<other>";
                    }
                }
            }
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
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
