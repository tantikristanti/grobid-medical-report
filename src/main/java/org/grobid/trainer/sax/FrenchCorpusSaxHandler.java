package org.grobid.trainer.sax;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/* SAX parser for the TEI format for the French medical NER data encoded for training.
*
* Tanti, 2021
* */

public class FrenchCorpusSaxHandler extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(FrenchCorpusSaxHandler.class);

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text
    private StringBuffer allContent = new StringBuffer();

    private String output = null;
    private String currentTag = null;

    private List<String> labeled = null; // store token by token the labels
    private List<List<String>> allLabeled = null; // list of labels
    private List<LayoutToken> tokens = null;
    private List<List<LayoutToken>> allTokens = null; // list of LayoutToken segmentation
    public int nbEntities = 0;

    public FrenchCorpusSaxHandler() {
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

        if (qName.equals("enamex")) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("document")) {
            String text = getText();
            currentTag = "<other>";
            if (text.length() > 0) {
                writeField(text);
            }
            nbEntities++;
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
        if (qName.equals("enamex")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    name = name.toLowerCase();
                    if (name.equals("type")) {
                        if (value.equals("anat") || value.equals("anatomy")) {
                            currentTag = "<anatomy>";
                        } else if (value.equals("date")) {
                            currentTag = "<date>";
                        } else if (value.equals("devi") || value.equals("device")) {
                            currentTag = "<device>";
                        } else if (value.equals("dose")) {
                            currentTag = "<dose>";
                        }  else if (value.equals("livb") || value.equals("living")) {
                            currentTag = "<living>";
                        } else if (value.equals("city") || value.equals("country") || value.equals("address") || value.equals("location")) {
                            currentTag = "<location>";
                        } else if (value.equals("measure")) {
                            currentTag = "<measure>";
                        } else if (value.equals("drug") || value.equals("product") || value.equals("medicament")) {
                            currentTag = "<medicament>";
                        } else if (value.equals("objc") || value.equals("object")) {
                            currentTag = "<object>";
                        } else if (value.equals("orgname")) {
                            currentTag = "<orgname>";
                        } else if (value.equals("disorder") || value.equals("pathology")) {
                            currentTag = "<pathology>";
                        } else if (value.equals("persname") || value.equals("name")) {
                            currentTag = "<persname>";
                        } else if (value.equals("phone")) {
                            currentTag = "<phone>";
                        } else if (value.equals("phys") || value.equals("physiology")) {
                            currentTag = "<physiology>";
                        } else if (value.equals("procedure") ) {
                            currentTag = "<procedure>";
                        } else if (value.equals("role") || value.equals("rolename")) {
                            currentTag = "<rolename>";
                        } else if (value.equals("chemical") || value.equals("substance"))  {
                            currentTag = "<substance>";
                        } else if (value.equals("phenomena") || value.equals("symptom")) {
                            currentTag = "<symptom>";
                        } else if (value.equals("unit")) {
                            currentTag = "<unit>";
                        } else if (value.equals("value")) {
                            currentTag = "<value>";
                        } else
                            currentTag = "<other>";
                    }
                } else
                    currentTag = "<other>";
            }
        } else if (qName.equals("document")) {
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
