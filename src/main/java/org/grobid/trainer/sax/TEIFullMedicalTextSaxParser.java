package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format for fulltext data encoded for training. Normally all training data should 
 * be in this unique format for the fulltext model.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 *
 * This class is taken and adapted from the TEIFulltextSaxParser class (@author Patrice Lopez)
 *
 * Tanti, 2021
 */
public class TEIFullMedicalTextSaxParser extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(TEIFulltextSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    private Stack<String> currentTags = null;
	private String currentTag = null;

    private boolean figureBlock = false;
	private boolean tableBlock = false;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    public TEIFullMedicalTextSaxParser() {
        labeled = new ArrayList<String>();
        currentTags = new Stack<String>();
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
		if ( (!qName.equals("lb")) && (!qName.equals("pb")) && (!qName.equals("space")) ) {
            writeData(qName, true);
			if (!currentTags.empty()) {
				currentTag = currentTags.peek();
			}
        }

        if (qName.equals("figure")) {
            figureBlock = false;
			tableBlock = false;
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {
        if (qName.equals("lb")) {
            //accumulator.append(" +LINE+ ");
            accumulator.append(" ");
        } 
		else if (qName.equals("space")) {
            accumulator.append(" ");
        } 
		else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(qName, false);
                }
            }
            accumulator.setLength(0);

            if (qName.equals("div")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("paragraph")) {
                                currentTags.push("<paragraph>");
								currentTag = "<paragraph>";
                            } else {
                                logger.error("Invalid attribute value for element div: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element div: " + name);
                        }
                    }
                }
            } 
			else if (qName.equals("p") ) {
                currentTags.push("<paragraph>");
				currentTag = "<paragraph>";
            }
            else if (qName.equals("note")) { // for the head and foot notes still found in the body part
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("place")) {
                            if (value.equals("footnote") || value.equals("foot")) {
                                currentTags.push("<footnote>");
                                currentTag = "<footnote>";
                            } else if (value.equals("headnote")|| value.equals("head")) {
                                currentTags.push("<headnote>");
                                currentTag = "<headnote>";
                            } else {
                                logger.error("Invalid attribute value for element note: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element note: " + name);
                        }
                    }
                }
            }
			else if (qName.equals("ref")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("figure")) {
                                currentTags.push("<figure_marker>");
								currentTag = "<figure_marker>";
                            } else if (value.equals("table")) {
								currentTags.push("<table_marker>");
								currentTag = "<table_marker>";
							} else if (value.equals("section")) {
                                currentTags.push("<section_marker>");
                                currentTag = "<section_marker>";
                            } else {
                                logger.error("Invalid attribute value for element ref: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element ref: " + name);
                        }
                    }
                }
            } 
			else if (qName.equals("head")) {
                currentTags.push("<section>");
                currentTag = "<section>";
            }
            else if (qName.equals("table")) {
                currentTags.push("<table>");
				currentTag = "<table>";
            } 
			else if (qName.equals("item")) {
//                currentTags.push("<paragraph>");
//				currentTag = "<paragraph>";
                currentTags.push("<item>");
                currentTag = "<item>";
            } 
			else if (qName.equals("figure")) {
	            figureBlock = true;
	            int length = atts.getLength();

	            // Process each attribute
	            for (int i = 0; i < length; i++) {
	                // Get names and values for each attribute
	                String name = atts.getQName(i);
	                String value = atts.getValue(i);

	                if (name != null) {
	                    if (name.equals("type")) {
	                        if (value.equals("table")) {
	                            tableBlock = true;
	                        } else {
                                logger.error("Invalid attribute value for element figure: " + name + "=" + value);
                            }
	                    } else {
                            logger.error("Invalid attribute name for element figure: " + name);
                        }
	                }
	            }
				if (tableBlock) {
					figureBlock = false;
	                currentTags.push("<table>");
					currentTag = "<table>";
				}
				else {
	                currentTags.push("<figure>");
					currentTag = "<figure>";
				}
	        }
            else if (qName.equals("other")) {
                currentTags.push("<other>");
                currentTag = "<other>";
            }
            else if (qName.equals("text")) {
                currentTags.push("<other>");
                currentTag = "<other>";
            } else {
                if (!qName.equals("tei") && !qName.equals("teiHeader") && !qName.equals("fileDesc") && !qName.equals("list")) {
                    logger.error("Invalid element name: " + qName + " - it will be mapped to the label <other>");
                    currentTags.push("<other>");
                    currentTag = "<other>";
                }
            }
        }
		
    }

    private void writeData(String qName, boolean pop) {
        if ( (qName.equals("other")) || (qName.equals("p")) || 
                (qName.equals("ref")) || (qName.equals("head")) ||
                (qName.equals("figure")) ||  (qName.equals("table")) ||
                (qName.equals("paragraph")) || (qName.equals("div")) ||
                (qName.equals("item")) || (qName.equals("note"))
                ) {
			if (currentTag == null) {
				return;
			}
	
            if (pop) {
				if (!currentTags.empty()) {
					currentTags.pop();
				}
            }

			// adjust tag (conservative)
			if (tableBlock) {
				currentTag = "<table>";
			}
			else if (figureBlock) {
				currentTag = "<figure>";
			}

            String text = getText();
            // we segment the text
            StringTokenizer st = new StringTokenizer(text, TextUtilities.delimiters, true);
            boolean begin = true;
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().trim();
                if (tok.length() == 0) 
					continue;

                /*if (tok.equals("+LINE+")) {
                    labeled.add("@newline\n");
                } else*/ {
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
                }
                begin = false;
            }
            accumulator.setLength(0);
        }
    }

}