package org.grobid.trainer.sax;

import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.lang.Language;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.analyzers.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * SAX parser for the XML format for the patient data.
 * Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 *
 * Tanti, 2021
 */

public class TEIPatientSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text
    private StringBuffer allContent = new StringBuffer();

    private String output = null;
    private String currentTag = null;

    private List<String> labeled = null; // store token by token the labels
    private List<List<String>> allLabeled = null; // list of labels
    private List<LayoutToken> tokens = null;
    private List<List<LayoutToken>> allTokens = null; // list of LayoutToken segmentation
    public int nbPatients = 0;

    public TEIPatientSaxParser() {
        allTokens = new ArrayList<List<LayoutToken>>();
        allLabeled = new ArrayList<List<String>>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
        if (allContent != null) {
            allContent.append(buffer, start, length);
        }
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public List<List<String>> getLabeledResult() {
        return allLabeled;
    }

    public List<List<LayoutToken>> getTokensResult() {
        return allTokens;
    }

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        qName = qName.toLowerCase();

        if ((qName.equals("idno")) || (qName.equals("persname")) || (qName.equals("sex")) ||
            (qName.equals("birth")) || (qName.equals("death")) || (qName.equals("address")) ||
            (qName.equals("country")) || (qName.equals("settlement")) || (qName.equals("phone")) || (qName.equals("note"))
        ) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("patient")) {
            String text = getText();
            currentTag = "<other>";
            if (text.length() > 0) {
                writeField(text);
            }
            nbPatients++;
            allLabeled.add(labeled);
            allTokens.add(tokens);
            allContent = null;
        }

        accumulator.setLength(0);
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
        throws SAXException {
        String text = getText();
        if (text.length() > 0) {
            currentTag = "<other>";
            writeField(text);
        }
        accumulator.setLength(0);

        qName = qName.toLowerCase();
        if (qName.equals("idno")) {
            currentTag = "<idno>";
        } else if ((qName.equals("persname") || (qName.equals("name")))) {
            currentTag = "<persname>";
        } else if (qName.equals("sex")) {
            currentTag = "<sex>";
        } else if (qName.equals("birth")) {
            currentTag = "<birth>";
        } else if (qName.equals("death")) {
            currentTag = "<death>";
        } else if (qName.equals("address")) {
            currentTag = "<address>";
        } else if (qName.equals("country")) {
            currentTag = "<country>";
        } else if (qName.equals("settlement")) {
            currentTag = "<settlement>";
        } else if (qName.equals("phone")) {
            currentTag = "<phone>";
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
                            if (value.equals("patient")) {
                                currentTag = "<note>";
                            }
                        }
                    }
                }
            }
        } else if (qName.equals("patient")) {
            accumulator = new StringBuffer();
            allContent = new StringBuffer();
            labeled = new ArrayList<String>();
            tokens = new ArrayList<LayoutToken>();
        }
        accumulator.setLength(0);
    }

    private void writeField(String text) {
        if (tokens == null) {
            // nothing to do, text must be ignored
            return;
        }

        // we segment the text
        List<LayoutToken> localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        if (isEmpty(localTokens)) {
            localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language("en", 1.0));
        }

        if (isEmpty(localTokens)) {
            return;
        }

        boolean begin = true;
        for (LayoutToken token : localTokens) {
            tokens.add(token);
            String content = token.getText();
            if (content.equals(" ") || content.equals("\n")) {
                labeled.add(null);
                continue;
            }

            content = UnicodeUtil.normaliseTextAndRemoveSpaces(content);
            if (content.trim().length() == 0) {
                labeled.add(null);
                continue;
            }

            if (content.length() > 0) {
                if (begin) {
                    labeled.add("I-" + currentTag);
                    begin = false;
                } else {
                    labeled.add(currentTag);
                }
            }
        }
    }
}