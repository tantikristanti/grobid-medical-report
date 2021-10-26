package org.grobid.trainer.sax;

import org.grobid.core.data.QuaeroDocument;
import org.grobid.core.data.QuaeroEntity;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/* A class to extract corpus having grobid-ner XML format
* */

public class EnamexCorpusSaxHandler extends DefaultHandler {
    private static final String Tag_Corpus = "corpus";
    private static final String Tag_Document = "document";
    private static final String Tag_Paragraph = "p";
    private static final String Tag_Id = "id";
    private static final String Tag_Text = "text";
    private static final String Tag_Passage = "passage";
    private static final String Tag_Type = "infon";
    private static final String Tag_Location = "location";

    private final Stack<String> tagsStack = new Stack<String>();
    private final StringBuilder accumulator = new StringBuilder();

    private List<QuaeroDocument> documents;
    private List<String> tokens;
    private List<String> labels;
    private QuaeroDocument quaeroDocument;
    private QuaeroEntity quaeroEntity;

    private boolean isType = false;
    private int offset;
    private int length;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        pushTag(qName);
        accumulator.setLength(0);
        if (Tag_Corpus.equalsIgnoreCase(qName)) { // the beginning of the corpus
            tokens = new ArrayList<>();
            labels = new ArrayList<>();
        } else if (Tag_Document.equalsIgnoreCase(qName)) {
            quaeroDocument = new QuaeroDocument();
        } else if (Tag_Paragraph.equalsIgnoreCase(qName)) { // each document contains of paragraphs
            quaeroEntity = new QuaeroEntity();
        } else if (Tag_Type.equalsIgnoreCase(qName)) {
            int length = attributes.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = attributes.getQName(i);
                String value = attributes.getValue(i);

                if (name != null) {
                    if (name.equals("key")) {
                        if (value.equals("type")) {
                            isType = true;
                        } else {
                            isType = false;
                        }
                    }
                }
            }
        } else if (Tag_Location.equalsIgnoreCase(qName)) {
            offset = Integer.valueOf(attributes.getValue("offset"));
            length = Integer.valueOf(attributes.getValue("length"));
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        accumulator.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String tag = peekTag();
        if (!qName.equals(tag)) {
            throw new InternalError();
        }

        popTag();
        String parentTag = peekTag();

        /*if (Tag_Id.equalsIgnoreCase(tag)) {
            quaeroDocument.setId(getText());
        } else if (Tag_Type.equalsIgnoreCase(tag)) {
            if (Tag_Annotation.equalsIgnoreCase(parentTag) && isType) {
                quaeroEntity.setType(getText());
            }
        } else if (Tag_Text.equalsIgnoreCase(tag)) {
            if (Tag_Annotation.equalsIgnoreCase(parentTag)) {
                quaeroEntity.setText(getText());
            } else if (Tag_Passage.equalsIgnoreCase(parentTag)) {
                quaeroDocument.setText(getText());
            }
        } else if (Tag_Location.equalsIgnoreCase(tag)) {
            quaeroEntity.setOffset(offset);
            quaeroEntity.setLength(length);
        } else if (Tag_Annotation.equalsIgnoreCase(tag)) { // every time it achieves the <annotation> tag, we add the elements to the current document information
            quaeroDocument.addEntity(quaeroEntity);
        } else if (Tag_Document.equalsIgnoreCase(tag)) { // add all the document information as a list
            documents.add(quaeroDocument);
        }*/
    }

    @Override
    public void startDocument() {
        pushTag("");
    }

    public List<QuaeroDocument> getDocuments() {
        return documents;
    }

    private void pushTag(String tag) {
        tagsStack.push(tag);
    }

    private String popTag() {
        return tagsStack.pop();
    }

    private String peekTag() {
        return tagsStack.peek();
    }

    public String getText() {
        return accumulator.toString().trim();
    }
}
