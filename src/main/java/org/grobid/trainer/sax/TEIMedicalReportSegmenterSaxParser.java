package org.grobid.trainer.sax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format for the training data for the medical model.
 * Normally all training data should be in this unique format.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 * <p>
 * Tanti, 2020
 */
public class TEIMedicalReportSegmenterSaxParser extends DefaultHandler {

  	/* TEI --> sax label mapping (11 labels for this model)
        <titlePage> cover page --> <cover>  : cover page (not much present in the APHP corpus, but there are some, ex. "701955797.pdf"), optionally under front,
        <front> document header --> <header>  : document header,
        <note place="headnote"> page header --> <headnote> : note type headnote,
        <note place="footnote"> page footer --> <footnote>: note type footnote,
        <note place="left"> left note --> <leftnote> : note type left,
        <note place="right"> right note  --> <rightnote> : note type right,
        <body> document body --> <body> : body,
        <page> page number --> <page> : page,
        <div type="annex"> annexes --> <annex> : annex,
        <div type="acknowledgment"> acknowledgement --> <acknowledgment> : acknowledgement,
        <other> other <other> : unknown entities
 	*/

    private static final Logger logger = LoggerFactory.getLogger(TEIMedicalReportSegmenterSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String currentTag = null;
    private String upperQname = null;
    private String upperTag = null;
    private List<String> labeled = null; // store line by line the labeled data

    public TEIMedicalReportSegmenterSaxParser() {
        labeled = new ArrayList<String>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        if (accumulator != null) {
            //System.out.println(accumulator.toString().trim());
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public List<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(String uri,
                           String localName,
                           String qName) throws SAXException {
        if ((!qName.equals("lb")) && (!qName.equals("pb"))) {
            writeData(qName, currentTag);
        }
        if (qName.equals("cover") ||
            qName.equals("front") ||
            qName.equals("body") ||
            qName.equals("div") ||
            qName.equals("other")) {
            currentTag = null;
            upperTag = null;
        } else if (qName.equals("note") ||
            qName.equals("page") ||
            qName.equals("pages") ||
            qName.equals("titlePage")) {
            currentTag = upperTag;
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
        throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("space")) {
            accumulator.append(" ");
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(upperQname, upperTag);
                }
            }
            //accumulator.setLength(0);

            if (qName.equals("titlePage")) {
                currentTag = "<cover>";
                //upperTag = currentTag;
                //upperQname = "titlePage";
            } else if (qName.equals("front")) {
                currentTag = "<header>";
                upperTag = currentTag;
                upperQname = "front";
            } else if (qName.equals("body")) {
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("other")) {
                currentTag = "<other>";
            } else if (qName.equals("note")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("place")) {
                            if (value.equals("footnote") || value.equals("foot")) {
                                currentTag = "<footnote>";
                            } else if (value.equals("headnote") || value.equals("head")) {
                                currentTag = "<headnote>";
                            } else if (value.equals("leftnote") || value.equals("left")) {
                                currentTag = "<leftnote>";
                            } else if (value.equals("rightnote") || value.equals("right")) {
                                currentTag = "<rightnote>";
                            } else {
                                logger.error("Invalid attribute value for element note: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element note: " + name);
                        }
                    }
                }
            } else if (qName.equals("div")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            // Note: funding and data availability annexes not fully annotated in the training corpus
                            // when it will be the case, we can add specific label for this
                            if (value.equals("annex")) {
                                currentTag = "<annex>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (value.equals("acknowledgement") || value.equals("acknowledgements") || value.equals("acknowledgment")
                                || value.equals("acknowledgments")) {
                                currentTag = "<acknowledgement>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else {
                                logger.error("Invalid attribute value for element div: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element div: " + name);
                        }
                    }
                }
            } else if (qName.equals("page") || qName.equals("pages")) {
                currentTag = "<page>";
            } else if (qName.equals("text")) {
                currentTag = "<other>";
                upperTag = null;
                upperQname = null;
            }
        }
    }

    private void writeData(String qName, String surfaceTag) {
        if (qName == null) {
            qName = "other";
            surfaceTag = "<other>";
        }
        if (((qName.equals("titlePage")) || qName.equals("front")) || (qName.equals("note")) ||
            (qName.equals("page")) || (qName.equals("pages")) || (qName.equals("body")) ||
            (qName.equals("div")) || (qName.equals("other"))
        ) {
            String text = getText();
            text = text.replace("\n", " ");
            text = text.replace("\r", " ");
            text = text.replace("  ", " ");
            boolean begin = true;
            //System.out.println(text);
            // we segment the text line by line first
            String[] tokens = text.split("\\+L\\+");
            //while (st.hasMoreTokens()) {
            boolean page = false;
            for (int p = 0; p < tokens.length; p++) {
                //String line = st.nextToken().trim();
                String line = tokens[p].trim();
                if (line.length() == 0)
                    continue;
                if (line.equals("\n") || line.equals("\r"))
                    continue;
                if (line.indexOf("+PAGE+") != -1) {
                    // page break should be a distinct feature
                    //labeled.add("@newpage\n");
                    line = line.replace("+PAGE+", "");
                    page = true;
                }

                //StringTokenizer st = new StringTokenizer(line, " \t");
                StringTokenizer st = new StringTokenizer(line, " \t\f\u00A0");
                if (!st.hasMoreTokens())
                    continue;
                String tok = st.nextToken();

                if (tok.length() == 0)
                    continue;

                if (surfaceTag == null) {
                    // this token belongs to a chunk to ignored
                    //System.out.println("\twarning: surfaceTag is null for token '"+tok+"' - it will be tagged with label <other>");
                    surfaceTag = "<other>";
                }

                if (begin && (!surfaceTag.equals("<other>"))) {
                    labeled.add(tok + " I-" + surfaceTag + "\n");
                    begin = false;
                } else {
                    labeled.add(tok + " " + surfaceTag + "\n");
                }

                if (page) {
                    page = false;
                }
            }
            accumulator.setLength(0);
        }
    }
}