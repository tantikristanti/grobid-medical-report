package org.grobid.trainer.sax;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnicodeUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * SAX parser for patients sequences encoded in the TEI format data.
 * Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 * Tanti, 2021
 */
public class TEIPatientSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String currentTag = null;

    private List<String> labeled = null; // store token by token the labels
    private List<List<String>> allLabeled = null; // list of labels
    private List<LayoutToken> tokens = null;
    private List<List<LayoutToken>> allTokens = null; // list of LayoutToken segmentation

    public int n = 0;

    public TEIPatientSaxParser() {
        allTokens = new ArrayList<List<LayoutToken>>();
        allLabeled = new ArrayList<List<String>>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
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

    public void endElement(String uri,
                           String localName,
                           String qName) throws SAXException {
        if ((
            (qName.equals("idno")) || (qName.equals("persName")) || (qName.equals("address"))
        ) & (currentTag != null)) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("patient")) {
            String text = getText();
            if (text.length() > 0) {
                currentTag = "<other>";
                writeField(text);
            }
            allLabeled.add(labeled);
            allTokens.add(tokens);
            n++;
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

        if (qName.equals("idno") ) {
            currentTag = "<idno>";
        } else if (qName.equals("persName")) {
            currentTag = "<persName>";
        } else if (qName.equals("address")) {
            currentTag = "<address>";
        } else if (qName.equals("patient")) {
            accumulator = new StringBuffer();
            labeled = new ArrayList<String>();
            tokens = new ArrayList<LayoutToken>();
        } else if (!qName.equals("analytic") && !qName.equals("biblStruct") &&
            !qName.equals("sourceDesc") && !qName.equals("fileDesc") &&
            !qName.equals("teiHeader") && !qName.equals("TEI") &&
            !qName.equals("tei") && !qName.equals("patient") &&
            !qName.equals("idno") && !qName.equals("persName") &&
            !qName.equals("address") && !qName.equals("lb")) {
            System.out.println("Warning, invalid tag: <" + qName + ">");
        }
    }

    private void writeField(String text) {
        // we segment the text
        //List<String> tokens = TextUtilities.segment(text, TextUtilities.punctuations);
        List<LayoutToken> localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        if ( (localTokens == null) || (localTokens.size() == 0) )
            localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language("fr", 1.0));
        if  ( (localTokens == null) || (localTokens.size() == 0) )
            return;

        boolean begin = true;
        for (LayoutToken token : localTokens) {
            if (tokens == null) {
                // should not be the case, it can indicate a structure problem in the training XML file
                tokens = new ArrayList<LayoutToken>();
                System.out.println("Warning: list of LayoutToken not initialized properly, parsing continue... ");
            }
            if (labeled == null) {
                // should not be the case, it can indicate a structure problem in the training XML file
                labeled = new ArrayList<String>();
                System.out.println("Warning: list of labels not initialized properly, parsing continue... ");
            }
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
