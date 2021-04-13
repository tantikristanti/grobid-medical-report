package org.grobid.core.document;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.grobid.core.GrobidMedicalReportModels;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.data.Date;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.FullMedicalTextParser;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.GraphicObject;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.layout.Page;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.matching.EntityMatcherException;
import org.grobid.core.utilities.matching.ReferenceMarkerMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.document.xml.XmlBuilderUtils.addXmlId;
import static org.grobid.core.document.xml.XmlBuilderUtils.textNode;

/**
 * Class for generating a TEI representation of a document.
 *
 * @author Patrice Lopez
 */


public class TEIFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TEIFormatter.class);

    private Document doc = null;
    private FullMedicalTextParser fullTextParser = null;
    public static final Set<TaggingLabel> MARKER_LABELS = Sets.newHashSet(
        TaggingLabels.CITATION_MARKER,
        TaggingLabels.FIGURE_MARKER,
        TaggingLabels.TABLE_MARKER,
        TaggingLabels.EQUATION_MARKER);

    // possible association to Grobid customised TEI schemas: DTD, XML schema, RelaxNG or compact RelaxNG
    // DEFAULT means no schema association in the generated XML documents
    public enum SchemaDeclaration {
        DEFAULT, DTD, XSD, RNG, RNC
    }

    private Boolean inParagraph = false;

    private ArrayList<String> elements = null;

    // static variable for the position of italic and bold features in the CRF model
    private static final int ITALIC_POS = 16;
    private static final int BOLD_POS = 15;

    private static Pattern numberRef = Pattern.compile("(\\[|\\()\\d+\\w?(\\)|\\])");
    private static Pattern numberRefCompact =
        Pattern.compile("(\\[|\\()((\\d)+(\\w)?(\\-\\d+\\w?)?,\\s?)+(\\d+\\w?)(\\-\\d+\\w?)?(\\)|\\])");
    private static Pattern numberRefCompact2 = Pattern.compile("(\\[|\\()(\\d+)(-|‒|–|—|―|\u2013)(\\d+)(\\)|\\])");

    private static Pattern startNum = Pattern.compile("^(\\d+)(.*)");

    public TEIFormatter(Document document, FullMedicalTextParser fullTextParser) {
        this.doc = document;
        this.fullTextParser = fullTextParser;
    }

    public StringBuilder toTEIHeader(HeaderMedicalItem headerItem,
                                     String defaultPublicationStatement,
                                     GrobidAnalysisConfig config) {
        return toTEIHeader(headerItem, SchemaDeclaration.XSD, defaultPublicationStatement,  config);
    }

    public StringBuilder toTEIHeader(HeaderMedicalItem headerItem,
                                     SchemaDeclaration schemaDeclaration,
                                     String defaultPublicationStatement,
                                     GrobidAnalysisConfig config) {
        StringBuilder tei = new StringBuilder();
        tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (config.isWithXslStylesheet()) {
            tei.append("<?xml-stylesheet type=\"text/xsl\" href=\"../jsp/xmlverbatimwrapper.xsl\"?> \n");
        }
        if (schemaDeclaration == SchemaDeclaration.DTD) {
            tei.append("<!DOCTYPE TEI SYSTEM \"" + GrobidProperties.get_GROBID_HOME_PATH()
                + "/schemas/dtd/Grobid.dtd" + "\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.XSD) {
            // XML schema
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" \n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "xsi:schemaLocation=\"http://www.tei-c.org/ns/1.0 " +
                GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/xsd/Grobid.xsd\"" +
                "\n xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.RNG) {
            // standard RelaxNG
            tei.append("<?xml-model href=\"file://" +
                GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/rng/Grobid.rng" +
                "\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\n");
        } else if (schemaDeclaration == SchemaDeclaration.RNC) {
            // compact RelaxNG
            tei.append("<?xml-model href=\"file://" +
                GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/rng/Grobid.rnc" +
                "\" type=\"application/relax-ng-compact-syntax\"?>\n");
        }
        // by default there is no schema association

        if (schemaDeclaration != SchemaDeclaration.XSD) {
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\">\n");
        }

        if (doc.getLanguage() != null) {
            tei.append("\t<teiHeader xml:lang=\"" + doc.getLanguage() + "\">");
        } else {
            tei.append("\t<teiHeader>");
        }

        tei.append("\n\t\t<fileDesc>\n\t\t\t<titleStmt>\n\t\t\t\t<title level=\"a\" type=\"main\"");
        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            tei.append(" xml:id=\"_" + divID + "\"");
        }
        tei.append(">");

        if (headerItem == null) {
            // if the headerItem object is null, we simply create an empty one
            headerItem = new HeaderMedicalItem();
        }

        if (headerItem.getTitle() != null) {
            tei.append(TextUtilities.HTMLEncode(headerItem.getTitle()));
        }

        tei.append("</title>\n\t\t\t</titleStmt>\n");
        if ((headerItem.getAffiliation() != null) || // fill in institutional information with affiliation
            (headerItem.getDocumentDate() != null) ||
            (headerItem.getNormalizedDocumentDate() != null)) {
            tei.append("\t\t\t<publicationStmt>\n");
            if (headerItem.getAffiliation() != null) {
                tei.append("\t\t\t\t<institution>" + TextUtilities.HTMLEncode(headerItem.getAffiliation()) +
                    "</institution>\n");

                tei.append("\t\t\t\t<availability status=\"unknown\">");
                tei.append("<p>Copyright : ");
                //if (headerItem.getPublicationDate() != null)
                //tei.append(TextUtilities.HTMLEncode(headerItem.getAffiliation()) + "</p>\n");
                tei.append(TextUtilities.HTMLEncode("©Assistance Publique – Hôpitaux de Paris (APHP)") + "</p>\n");
                tei.append("\t\t\t\t</availability>\n");
            } else {
                // a dummy publicationStmt is still necessary according to TEI
                tei.append("\t\t\t\t<docOwner/>\n");
                if (defaultPublicationStatement == null) {
                    tei.append("\t\t\t\t<availability status=\"unknown\"><licence/></availability>");
                } else {
                    tei.append("\t\t\t\t<availability status=\"unknown\"><p>" +
                        defaultPublicationStatement + "</p></availability>");
                }
                tei.append("\n");
            }

            if (headerItem.getNormalizedDocumentDate() != null) {
                Date date = headerItem.getNormalizedDocumentDate();
                int year = date.getYear();
                int month = date.getMonth();
                int day = date.getDay();

                String when = "";
                if (year != -1) {
                    if (year <= 9)
                        when += "000" + year;
                    else if (year <= 99)
                        when += "00" + year;
                    else if (year <= 999)
                        when += "0" + year;
                    else
                        when += year;
                    if (month != -1) {
                        if (month <= 9)
                            when += "-0" + month;
                        else
                            when += "-" + month;
                        if (day != -1) {
                            if (day <= 9)
                                when += "-0" + day;
                            else
                                when += "-" + day;
                        }
                    }
                    tei.append("\t\t\t\t<date type=\"issued\" when=\"");
                    tei.append(when + "\">");
                } else
                    tei.append("\t\t\t\t<date>");
                if (headerItem.getDocumentDate() != null) {
                    tei.append(TextUtilities.HTMLEncode(headerItem.getDocumentDate()));
                } else {
                    tei.append(when);
                }
                tei.append("</date>\n");
            } else if ((headerItem.getYear() != null) && (headerItem.getYear().length() > 0)) {
                String when = "";
                if (headerItem.getYear().length() == 1)
                    when += "000" + headerItem.getYear();
                else if (headerItem.getYear().length() == 2)
                    when += "00" + headerItem.getYear();
                else if (headerItem.getYear().length() == 3)
                    when += "0" + headerItem.getYear();
                else if (headerItem.getYear().length() == 4)
                    when += headerItem.getYear();

                if ((headerItem.getMonth() != null) && (headerItem.getMonth().length() > 0)) {
                    if (headerItem.getMonth().length() == 1)
                        when += "-0" + headerItem.getMonth();
                    else
                        when += "-" + headerItem.getMonth();
                    if ((headerItem.getDay() != null) && (headerItem.getDay().length() > 0)) {
                        if (headerItem.getDay().length() == 1)
                            when += "-0" + headerItem.getDay();
                        else
                            when += "-" + headerItem.getDay();
                    }
                }
                tei.append("\t\t\t\t<date type=\"issued\" when=\"");
                tei.append(when + "\">");
                if (headerItem.getDocumentDate() != null) {
                    tei.append(TextUtilities.HTMLEncode(headerItem.getDocumentDate()));
                } else {
                    tei.append(when);
                }
                tei.append("</date>\n");
            } else if (headerItem.getDocumentDate() != null) {
                tei.append("\t\t\t\t<date type=\"issued\">");
                tei.append(TextUtilities.HTMLEncode(headerItem.getDocumentDate())
                    + "</date>");
            }
            tei.append("\t\t\t</publicationStmt>\n");
        } else {
            tei.append("\t\t\t<publicationStmt>\n");
            tei.append("\t\t\t\t<docOwner/>\n");
            tei.append("\t\t\t\t<availability status=\"unknown\"><licence/></availability>\n");
            tei.append("\t\t\t</publicationStmt>\n");
        }
        tei.append("\t\t\t<sourceDesc>\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n");

        // authors + affiliation

        tei.append(headerItem.toTEIMedicBlock(6, config));

        // title
        String title = headerItem.getTitle();
        String language = headerItem.getLanguage();
        if (title != null) {
            tei.append("\t\t\t\t\t\t<title");
            tei.append(" level=\"a\" type=\"main\"");

            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                tei.append(" xml:id=\"_" + divID + "\"");
            }

            // here check the language ?
            tei.append(" xml:lang=\"" + language + "\">" + TextUtilities.HTMLEncode(title) + "</title>\n");
        }

        tei.append("\t\t\t\t\t</analytic>\n");
        tei.append("\t\t\t\t\t<monogr>\n");
        tei.append("\t\t\t\t\t\t<imprint>\n");
        tei.append("\t\t\t\t\t\t\t<date/>\n");
        tei.append("\t\t\t\t\t\t</imprint>\n");
        tei.append("\t\t\t\t\t</monogr>\n");
        tei.append("\t\t\t\t</biblStruct>\n");
        tei.append("\t\t\t</sourceDesc>\n");
        tei.append("\t\t</fileDesc>\n");

        // encodingDesc gives info about the producer of the file
        tei.append("\t\t<encodingDesc>\n");
        tei.append("\t\t\t<appInfo>\n");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String dateISOString = df.format(new java.util.Date());

        tei.append("\t\t\t\t<application version=\"" + GrobidProperties.getVersion() +
            "\" ident=\"GROBID\" when=\"" + dateISOString + "\">\n");
        tei.append("\t\t\t\t\t<desc>GROBID - A machine learning software for extracting information from scholarly documents</desc>\n");
        tei.append("\t\t\t\t\t<ref target=\"https://github.com/kermitt2/grobid\"/>\n");
        tei.append("\t\t\t\t</application>\n");
        tei.append("\t\t\t</appInfo>\n");
        tei.append("\t\t</encodingDesc>\n");

        boolean textClassWritten = false;

        tei.append("\t\t<profileDesc>\n");

        if (textClassWritten)
            tei.append("\t\t\t</textClass>\n");

        tei.append("\t\t</profileDesc>\n");

        tei.append("\t</teiHeader>\n");

        // output pages dimensions in the case coordinates will also be provided for some structures
        try {
            tei = toTEIPages(tei, doc, config);
        } catch (Exception e) {
            LOGGER.warn("Problem when serializing page size", e);
        }

        if (doc.getLanguage() != null) {
            tei.append("\t<text xml:lang=\"").append(doc.getLanguage()).append("\">\n");
        } else {
            tei.append("\t<text>\n");
        }

        return tei;
    }

    public StringBuilder toTEILeftNote(LeftNoteMedicalItem biblio,
                                       String defaultPublicationStatement,
                                       List<BibDataSet> bds,
                                       GrobidAnalysisConfig config) {
        return toTEILeftNote(biblio, SchemaDeclaration.XSD, defaultPublicationStatement, bds, config);
    }

    public StringBuilder toTEILeftNote(LeftNoteMedicalItem biblio,
                                       SchemaDeclaration schemaDeclaration,
                                       String defaultPublicationStatement,
                                       List<BibDataSet> bds,
                                       GrobidAnalysisConfig config) {
        StringBuilder tei = new StringBuilder();
        tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (config.isWithXslStylesheet()) {
            tei.append("<?xml-stylesheet type=\"text/xsl\" href=\"../jsp/xmlverbatimwrapper.xsl\"?> \n");
        }
        if (schemaDeclaration == SchemaDeclaration.DTD) {
            tei.append("<!DOCTYPE TEI SYSTEM \"" + GrobidProperties.get_GROBID_HOME_PATH()
                + "/schemas/dtd/Grobid.dtd" + "\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.XSD) {
            // XML schema
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" \n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                //"\n xsi:noNamespaceSchemaLocation=\"" +
                //GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/xsd/Grobid.xsd\""	+
                "xsi:schemaLocation=\"http://www.tei-c.org/ns/1.0 " +
                GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/xsd/Grobid.xsd\"" +
                "\n xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
//				"\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.RNG) {
            // standard RelaxNG
            tei.append("<?xml-model href=\"file://" +
                GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/rng/Grobid.rng" +
                "\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\n");
        } else if (schemaDeclaration == SchemaDeclaration.RNC) {
            // compact RelaxNG
            tei.append("<?xml-model href=\"file://" +
                GrobidProperties.get_GROBID_HOME_PATH() + "/schemas/rng/Grobid.rnc" +
                "\" type=\"application/relax-ng-compact-syntax\"?>\n");
        }
        // by default there is no schema association

        if (schemaDeclaration != SchemaDeclaration.XSD) {
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\">\n");
        }

        if (doc.getLanguage() != null) {
            tei.append("\t<teiLeftNote xml:lang=\"" + doc.getLanguage() + "\">");
        } else {
            tei.append("\t<teiLeftNote>");
        }

        tei.append("\n\t\t<fileDesc>\n\t\t\t<titleStmt>\n\t\t\t\t<title level=\"a\" type=\"main\"");
        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            tei.append(" xml:id=\"_" + divID + "\"");
        }
        tei.append(">");

        if (biblio == null) {
            // if the biblio object is null, we simply create an empty one
            biblio = new LeftNoteMedicalItem();
        }

        if ((biblio.getInstitution() != null)) {
            tei.append("\t\t\t<publicationStmt>\n");
            if (biblio.getInstitution() != null) {
                tei.append("\t\t\t\t<institution>" + TextUtilities.HTMLEncode(biblio.getInstitution()) +
                    "</institution>\n");

                tei.append("\t\t\t\t<availability status=\"unknown\">");
                tei.append("<p>Copyright ");
                //if (biblio.getPublicationDate() != null)
                tei.append(TextUtilities.HTMLEncode(biblio.getInstitution()) + "</p>\n");
                tei.append("\t\t\t\t</availability>\n");
            } else {
                // a dummy publicationStmt is still necessary according to TEI
                tei.append("\t\t\t\t<docOwner/>\n");
                if (defaultPublicationStatement == null) {
                    tei.append("\t\t\t\t<availability status=\"unknown\"><licence/></availability>");
                } else {
                    tei.append("\t\t\t\t<availability status=\"unknown\"><p>" +
                        defaultPublicationStatement + "</p></availability>");
                }
                tei.append("\n");
            }

            tei.append("\t\t\t</publicationStmt>\n");
        } else {
            tei.append("\t\t\t<publicationStmt>\n");
            tei.append("\t\t\t\t<docOwner/>\n");
            tei.append("\t\t\t\t<availability status=\"unknown\"><licence/></availability>\n");
            tei.append("\t\t\t</publicationStmt>\n");
        }
        tei.append("\t\t\t<sourceDesc>\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n");

        // authors + affiliation
        //biblio.createAuthorSet();
        //biblio.attachEmails();
        //biblio.attachAffiliations();

        tei.append(biblio.toTEIMedicBlock(6, config));

        tei.append("\t\t\t\t\t</analytic>\n");
        tei.append("\t\t\t\t\t<monogr>\n");
        tei.append("\t\t\t\t\t\t<imprint>\n");
        tei.append("\t\t\t\t\t\t\t<date/>\n");
        tei.append("\t\t\t\t\t\t</imprint>\n");
        tei.append("\t\t\t\t\t</monogr>\n");
        tei.append("\t\t\t\t</biblStruct>\n");
        tei.append("\t\t\t</sourceDesc>\n");
        tei.append("\t\t</fileDesc>\n");

        // encodingDesc gives info about the producer of the file
        tei.append("\t\t<encodingDesc>\n");
        tei.append("\t\t\t<appInfo>\n");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String dateISOString = df.format(new java.util.Date());

        tei.append("\t\t\t\t<application version=\"" + GrobidProperties.getVersion() +
            "\" ident=\"GROBID\" when=\"" + dateISOString + "\">\n");
        tei.append("\t\t\t\t\t<desc>GROBID - A machine learning software for extracting information from scholarly documents</desc>\n");
        tei.append("\t\t\t\t\t<ref target=\"https://github.com/kermitt2/grobid\"/>\n");
        tei.append("\t\t\t\t</application>\n");
        tei.append("\t\t\t</appInfo>\n");
        tei.append("\t\t</encodingDesc>\n");

        boolean textClassWritten = false;

        tei.append("\t\t<profileDesc>\n");

        if (textClassWritten)
            tei.append("\t\t\t</textClass>\n");

        tei.append("\t\t</profileDesc>\n");

        tei.append("\t</teiLeftNote>\n");

        // output pages dimensions in the case coordinates will also be provided for some structures
        try {
            tei = toTEIPages(tei, doc, config);
        } catch (Exception e) {
            LOGGER.warn("Problem when serializing page size", e);
        }

        if (doc.getLanguage() != null) {
            tei.append("\t<text xml:lang=\"").append(doc.getLanguage()).append("\">\n");
        } else {
            tei.append("\t<text>\n");
        }

        return tei;
    }

    /**
     * TEI formatting of the body where only basic logical document structures are present.
     * This TEI format avoids most of the risks of ill-formed TEI due to structure recognition
     * errors and frequent PDF noises.
     * It is adapted to fully automatic process and simple exploitation of the document structures
     * like structured indexing and search.
     */
    public StringBuilder toTEIBody(StringBuilder buffer,
                                   String result,
                                   HeaderMedicalItem biblio,
                                   LayoutTokenization layoutTokenization,
                                   List<Figure> figures,
                                   List<Table> tables,
                                   Document doc,
                                   GrobidAnalysisConfig config) throws Exception {
        if ((result == null) || (layoutTokenization == null) || (layoutTokenization.getTokenization() == null)) {
            buffer.append("\t\t<body/>\n");
            return buffer;
        }
        buffer.append("\t\t<body>\n");
        buffer = toTEITextPiece(buffer, result, biblio,true,
            layoutTokenization, figures, tables, doc, config);

        // notes are still in the body
        buffer = toTEINote(buffer, doc, config);

        buffer.append("\t\t</body>\n");

        return buffer;
    }

    private StringBuilder toTEINote(StringBuilder tei,
                                    Document doc,
                                    GrobidAnalysisConfig config) throws Exception {
        // write the notes
        SortedSet<DocumentPiece> documentNoteParts = doc.getDocumentPart(MedicalLabels.FOOTNOTE);
        if (documentNoteParts != null) {
            tei = toTEINote("foot", documentNoteParts, tei, doc, config);
        }
        documentNoteParts = doc.getDocumentPart(MedicalLabels.MARGINNOTE);
        if (documentNoteParts != null) {
            tei = toTEINote("margin", documentNoteParts, tei, doc, config);
        }
        return tei;
    }

    private StringBuilder toTEINote(String noteType,
                                    SortedSet<DocumentPiece> documentNoteParts,
                                    StringBuilder tei,
                                    Document doc,
                                    GrobidAnalysisConfig config) throws Exception {
        List<String> allNotes = new ArrayList<>();
        for (DocumentPiece docPiece : documentNoteParts) {

            List<LayoutToken> noteTokens = doc.getDocumentPieceTokenization(docPiece);
            if ((noteTokens == null) || (noteTokens.size() == 0))
                continue;

            String footText = doc.getDocumentPieceText(docPiece);
            footText = TextUtilities.dehyphenize(footText);
            footText = footText.replace("\n", " ");
            footText = footText.replace("  ", " ").trim();
            if (footText.length() < 6)
                continue;
            if (allNotes.contains(footText)) {
                // basically we have here the "recurrent" headnote/footnote for each page,
                // no need to add them several times (in the future we could even use them
                // differently combined with the header)
                continue;
            }


            // pattern is <note n="1" place="foot" xml:id="no1">
            Matcher ma = startNum.matcher(footText);
            int currentNumber = -1;
            if (ma.find()) {
                String groupStr = ma.group(1);
                footText = ma.group(2);
                try {
                    currentNumber = Integer.parseInt(groupStr);
                    // remove this number from the layout tokens of the note
                    if (currentNumber != -1) {
                        String toConsume = groupStr;
                        int start = 0;
                        for (LayoutToken token : noteTokens) {
                            if ((token.getText() == null) || (token.getText().length() == 0))
                                continue;
                            if (toConsume.startsWith(token.getText())) {
                                start++;
                                toConsume = toConsume.substring(token.getText().length());
                            } else
                                break;

                            if (toConsume.length() == 0)
                                break;
                        }
                        if (start != 0)
                            noteTokens = noteTokens.subList(start, noteTokens.size());

                    }
                } catch (NumberFormatException e) {
                    currentNumber = -1;
                }
            }

            //tei.append(TextUtilities.HTMLEncode(footText));
            allNotes.add(footText);

            Element desc = XmlBuilderUtils.teiElement("note");
            desc.addAttribute(new Attribute("place", noteType));
            if (currentNumber != -1) {
                desc.addAttribute(new Attribute("n", "" + currentNumber));
            }
            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                addXmlId(desc, "_" + divID);
            }

            tei.append("\t\t\t");
            tei.append(desc.toXML());
            tei.append("\n");
        }

        return tei;
    }

    public StringBuilder toTEIAcknowledgement(StringBuilder buffer,
                                              String reseAcknowledgement,
                                              List<LayoutToken> tokenizationsAcknowledgement,
                                              GrobidAnalysisConfig config) throws Exception {
        if ((reseAcknowledgement == null) || (tokenizationsAcknowledgement == null)) {
            return buffer;
        }

        buffer.append("\n\t\t\t<div type=\"acknowledgement\">\n");
        StringBuilder buffer2 = new StringBuilder();

        buffer2 = toTEITextPiece(buffer2, reseAcknowledgement, null, false,
            new LayoutTokenization(tokenizationsAcknowledgement), null, null, doc, config);
        String acknowResult = buffer2.toString();
        String[] acknowResultLines = acknowResult.split("\n");
        boolean extraDiv = false;
        if (acknowResultLines.length != 0) {
            for (int i = 0; i < acknowResultLines.length; i++) {
                if (acknowResultLines[i].trim().length() == 0)
                    continue;
                buffer.append(TextUtilities.dehyphenize(acknowResultLines[i]) + "\n");
            }
        }
        buffer.append("\t\t\t</div>\n\n");

        return buffer;
    }


    public StringBuilder toTEIAnnex(StringBuilder buffer,
                                    String result,
                                    HeaderMedicalItem headerItem,
                                    List<LayoutToken> tokenizations,
                                    Document doc,
                                    GrobidAnalysisConfig config) throws Exception {
        if ((result == null) || (tokenizations == null)) {
            return buffer;
        }

        buffer.append("\t\t\t<div type=\"annex\">\n");
        buffer = toTEITextPiece(buffer, result, headerItem, true,
            new LayoutTokenization(tokenizations), null, null, doc, config);
        buffer.append("\t\t\t</div>\n");

        return buffer;
    }

    public StringBuilder toTEITextPiece(StringBuilder buffer,
                                        String result,
                                        HeaderMedicalItem headerItem,
                                        boolean keepUnsolvedCallout,
                                        LayoutTokenization layoutTokenization,
                                        List<Figure> figures,
                                        List<Table> tables,
                                        Document doc,
                                        GrobidAnalysisConfig config) throws Exception {
        TaggingLabel lastClusterLabel = null;
        int startPosition = buffer.length();

        //boolean figureBlock = false; // indicate that a figure or table sequence was met
        // used for reconnecting a paragraph that was cut by a figure/table

        List<LayoutToken> tokenizations = layoutTokenization.getTokenization();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidMedicalReportModels.FULL_MEDICAL_TEXT, result, tokenizations);

        String tokenLabel = null;
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        List<Element> divResults = new ArrayList<>();

        Element curDiv = teiElement("div");
        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            addXmlId(curDiv, "_" + divID);
        }
        divResults.add(curDiv);
        Element curParagraph = null;
        Element curList = null;
        int equationIndex = 0; // current equation index position
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);
            if (clusterLabel.equals(TaggingLabels.SECTION)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                curDiv = teiElement("div");
                /*if (config.isGenerateTeiIds()) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    addXmlId(curDiv, "_" + divID);
                }*/

                Element head = teiElement("head");
                // section numbers
                org.grobid.core.utilities.Pair<String, String> numb = getSectionNumber(clusterContent);
                if (numb != null) {
                    head.addAttribute(new Attribute("n", numb.b));
                    head.appendChild(numb.a);
                } else {
                    head.appendChild(clusterContent);
                }

                if (config.isGenerateTeiIds()) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    addXmlId(head, "_" + divID);
                }

                curDiv.appendChild(head);
                divResults.add(curDiv);
            } else if (clusterLabel.equals(TaggingLabels.ITEM)) {
                String clusterContent = LayoutTokensUtil.normalizeText(cluster.concatTokens());
                //curDiv.appendChild(teiElement("item", clusterContent));
                Element itemNode = teiElement("item", clusterContent);
                if (!MARKER_LABELS.contains(lastClusterLabel) && (lastClusterLabel != TaggingLabels.ITEM)) {
                    curList = teiElement("list");
                    curDiv.appendChild(curList);
                }
                if (curList != null) {
                    curList.appendChild(itemNode);
                }
            } else if (clusterLabel.equals(TaggingLabels.OTHER)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                Element note = teiElement("note", clusterContent);
                note.addAttribute(new Attribute("type", "other"));
                if (config.isGenerateTeiIds()) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    addXmlId(note, "_" + divID);
                }
                curDiv.appendChild(note);
            } else if (clusterLabel.equals(TaggingLabels.PARAGRAPH)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                if (isNewParagraph(lastClusterLabel, curParagraph)) {
                    curParagraph = teiElement("p");
                    if (config.isGenerateTeiIds()) {
                        String divID = KeyGen.getKey().substring(0, 7);
                        addXmlId(curParagraph, "_" + divID);
                    }
                    curDiv.appendChild(curParagraph);
                }
                curParagraph.appendChild(clusterContent);
            } else if (MARKER_LABELS.contains(clusterLabel)) {
                List<LayoutToken> refTokens = cluster.concatTokens();
                refTokens = LayoutTokensUtil.dehyphenize(refTokens);
                String chunkRefString = LayoutTokensUtil.toText(refTokens);

                Element parent = curParagraph != null ? curParagraph : curDiv;
                parent.appendChild(new Text(" "));

                List<Node> refNodes;
                if (clusterLabel.equals(TaggingLabels.FIGURE_MARKER)) {
                    refNodes = markReferencesFigureTEI(chunkRefString, refTokens, figures,
                        config.isGenerateTeiCoordinates("ref"));
                } else if (clusterLabel.equals(TaggingLabels.TABLE_MARKER)) {
                    refNodes = markReferencesTableTEI(chunkRefString, refTokens, tables,
                        config.isGenerateTeiCoordinates("ref"));
                } else {
                    throw new IllegalStateException("Unsupported marker type: " + clusterLabel);
                }

                if (refNodes != null) {
                    for (Node n : refNodes) {
                        parent.appendChild(n);
                    }
                }
            } else if (clusterLabel.equals(TaggingLabels.FIGURE) || clusterLabel.equals(TaggingLabels.TABLE)) {
                //figureBlock = true;
                if (curParagraph != null)
                    curParagraph.appendChild(new Text(" "));
            }

            lastClusterLabel = cluster.getTaggingLabel();
        }

        // remove possibly empty div in the div list
        if (divResults.size() != 0) {
            for (int i = divResults.size() - 1; i >= 0; i--) {
                Element theDiv = divResults.get(i);
                if ((theDiv.getChildElements() == null) || (theDiv.getChildElements().size() == 0)) {
                    divResults.remove(i);
                }
            }
        }

        if (divResults.size() != 0)
            buffer.append(XmlBuilderUtils.toXml(divResults));
        else
            buffer.append(XmlBuilderUtils.toXml(curDiv));

        // we apply some overall cleaning and simplification
        buffer = TextUtilities.replaceAll(buffer, "</head><head",
            "</head>\n\t\t\t</div>\n\t\t\t<div>\n\t\t\t\t<head");
        buffer = TextUtilities.replaceAll(buffer, "</p>\t\t\t\t<p>", " ");

        //TODO: work on reconnection
        // we evaluate the need to reconnect paragraphs cut by a figure or a table
        int indP1 = buffer.indexOf("</p0>", startPosition - 1);
        while (indP1 != -1) {
            int indP2 = buffer.indexOf("<p>", indP1 + 1);
            if ((indP2 != 1) && (buffer.length() > indP2 + 5)) {
                if (Character.isUpperCase(buffer.charAt(indP2 + 4)) &&
                    Character.isLowerCase(buffer.charAt(indP2 + 5))) {
                    // a marker for reconnecting the two paragraphs
                    buffer.setCharAt(indP2 + 1, 'q');
                }
            }
            indP1 = buffer.indexOf("</p0>", indP1 + 1);
        }
        buffer = TextUtilities.replaceAll(buffer, "</p0>(\\n\\t)*<q>", " ");
        buffer = TextUtilities.replaceAll(buffer, "</p0>", "</p>");
        buffer = TextUtilities.replaceAll(buffer, "<q>", "<p>");

        if (figures != null) {
            for (Figure figure : figures) {
                String figSeg = figure.toTEI(config, doc, this);
                if (figSeg != null) {
                    buffer.append(figSeg).append("\n");
                }
            }
        }
        if (tables != null) {
            for (Table table : tables) {
                String tabSeg = table.toTEI(config, doc, this);
                if (tabSeg != null) {
                    buffer.append(tabSeg).append("\n");
                }
            }
        }

        return buffer;
    }

    private boolean isNewParagraph(TaggingLabel lastClusterLabel, Element curParagraph) {
        return (!MARKER_LABELS.contains(lastClusterLabel) && lastClusterLabel != TaggingLabels.FIGURE
            && lastClusterLabel != TaggingLabels.TABLE) || curParagraph == null;
    }


    /**
     * Return the graphic objects in a given interval position in the document.
     */
    private List<GraphicObject> getGraphicObject(List<GraphicObject> graphicObjects, int startPos, int endPos) {
        List<GraphicObject> result = new ArrayList<GraphicObject>();
        for (GraphicObject nto : graphicObjects) {
            if ((nto.getStartPosition() >= startPos) && (nto.getStartPosition() <= endPos)) {
                result.add(nto);
            }
            if (nto.getStartPosition() > endPos) {
                break;
            }
        }
        return result;
    }

    private org.grobid.core.utilities.Pair<String, String> getSectionNumber(String text) {
        Matcher m1 = BasicStructureBuilder.headerNumbering1.matcher(text);
        Matcher m2 = BasicStructureBuilder.headerNumbering2.matcher(text);
        Matcher m3 = BasicStructureBuilder.headerNumbering3.matcher(text);
        Matcher m = null;
        String numb = null;
        if (m1.find()) {
            numb = m1.group(0);
            m = m1;
        } else if (m2.find()) {
            numb = m2.group(0);
            m = m2;
        } else if (m3.find()) {
            numb = m3.group(0);
            m = m3;
        }
        if (numb != null) {
            text = text.replace(numb, "").trim();
            numb = numb.replace(" ", "");
            return new org.grobid.core.utilities.Pair<>(text, numb);
        } else {
            return null;
        }
    }

    //bounding boxes should have already been calculated when calling this method
    public static String getCoordsAttribute(List<BoundingBox> boundingBoxes, boolean generateCoordinates) {
        if (!generateCoordinates || boundingBoxes == null || boundingBoxes.isEmpty()) {
            return "";
        }
        String coords = Joiner.on(";").join(boundingBoxes);
        return "coords=\"" + coords + "\"";
    }


    public List<Node> markReferencesFigureTEI(String text,
                                              List<LayoutToken> refTokens,
                                              List<Figure> figures,
                                              boolean generateCoordinates) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        List<Node> nodes = new ArrayList<>();

        String textLow = text.toLowerCase();
        String bestFigure = null;

        if (figures != null) {
            for (Figure figure : figures) {
                if ((figure.getLabel() != null) && (figure.getLabel().length() > 0)) {
                    String label = TextUtilities.cleanField(figure.getLabel(), false);
                    if ((label.length() > 0) &&
                        (textLow.contains(label.toLowerCase()))) {
                        bestFigure = figure.getId();
                        break;
                    }
                }
            }
        }

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" "))
            spaceEnd = true;
        text = text.trim();

        String coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = LayoutTokensUtil.getCoordsString(refTokens);
        }

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("type", "figure"));

        if (coords != null) {
            ref.addAttribute(new Attribute("coords", coords));
        }
        ref.appendChild(text);

        if (bestFigure != null) {
            ref.addAttribute(new Attribute("target", "#fig_" + bestFigure));
        }
        nodes.add(ref);
        if (spaceEnd)
            nodes.add(new Text(" "));
        return nodes;
    }

    public List<Node> markReferencesTableTEI(String text, List<LayoutToken> refTokens,
                                             List<Table> tables,
                                             boolean generateCoordinates) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        List<Node> nodes = new ArrayList<>();

        String textLow = text.toLowerCase();
        String bestTable = null;
        if (tables != null) {
            for (Table table : tables) {
                /*if ((table.getId() != null) &&
                        (table.getId().length() > 0) &&
                        (textLow.contains(table.getId().toLowerCase()))) {
                    bestTable = table.getId();
                    break;
                }*/
                if ((table.getLabel() != null) && (table.getLabel().length() > 0)) {
                    String label = TextUtilities.cleanField(table.getLabel(), false);
                    if ((label.length() > 0) &&
                        (textLow.contains(label.toLowerCase()))) {
                        bestTable = table.getId();
                        break;
                    }
                }
            }
        }

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" "))
            spaceEnd = true;
        text = text.trim();

        String coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = LayoutTokensUtil.getCoordsString(refTokens);
        }

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("type", "table"));

        if (coords != null) {
            ref.addAttribute(new Attribute("coords", coords));
        }
        ref.appendChild(text);
        if (bestTable != null) {
            ref.addAttribute(new Attribute("target", "#tab_" + bestTable));
        }
        nodes.add(ref);
        if (spaceEnd)
            nodes.add(new Text(" "));
        return nodes;
    }

    private static Pattern patternNumber = Pattern.compile("\\d+");

    public List<Node> markReferencesEquationTEI(String text,
                                                List<LayoutToken> refTokens,
                                                List<Equation> equations,
                                                boolean generateCoordinates) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        text = TextUtilities.cleanField(text, false);
        String textNumber = null;
        Matcher m = patternNumber.matcher(text);
        if (m.find()) {
            textNumber = m.group();
        }

        List<Node> nodes = new ArrayList<>();

        String textLow = text.toLowerCase();
        String bestFormula = null;
        if (equations != null) {
            for (Equation equation : equations) {
                if ((equation.getLabel() != null) && (equation.getLabel().length() > 0)) {
                    String label = TextUtilities.cleanField(equation.getLabel(), false);
                    Matcher m2 = patternNumber.matcher(label);
                    String labelNumber = null;
                    if (m2.find()) {
                        labelNumber = m2.group();
                    }
                    //if ((label.length() > 0) &&
                    //        (textLow.contains(label.toLowerCase()))) {
                    if ((labelNumber != null && textNumber != null && labelNumber.length() > 0 &&
                        labelNumber.equals(textNumber)) ||
                        ((label.length() > 0) && (textLow.equals(label.toLowerCase())))) {
                        bestFormula = equation.getId();
                        break;
                    }
                }
            }
        }

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" "))
            spaceEnd = true;
        text = text.trim();

        String coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = LayoutTokensUtil.getCoordsString(refTokens);
        }

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("type", "formula"));

        if (coords != null) {
            ref.addAttribute(new Attribute("coords", coords));
        }
        ref.appendChild(text);
        if (bestFormula != null) {
            ref.addAttribute(new Attribute("target", "#formula_" + bestFormula));
        }
        nodes.add(ref);
        if (spaceEnd)
            nodes.add(new Text(" "));
        return nodes;
    }

    private String normalizeText(String localText) {
        localText = localText.trim();
        localText = TextUtilities.dehyphenize(localText);
        localText = localText.replace("\n", " ");
        localText = localText.replace("  ", " ");

        return localText.trim();
    }

    /**
     * In case, the coordinates of structural elements are provided in the TEI
     * representation, we need the page sizes in order to scale the coordinates
     * appropriately. These size information are provided via the TEI facsimile
     * element, with a surface element for each page carrying the page size info.
     */
    public StringBuilder toTEIPages(StringBuilder buffer,
                                    Document doc,
                                    GrobidAnalysisConfig config) throws Exception {
        if (!config.isGenerateTeiCoordinates()) {
            // no cooredinates, nothing to do
            return buffer;
        }

        // page height and width
        List<Page> pages = doc.getPages();
        int pageNumber = 1;
        buffer.append("\t<facsimile>\n");
        for (Page page : pages) {
            buffer.append("\t\t<surface ");
            buffer.append("n=\"" + pageNumber + "\" ");
            buffer.append("ulx=\"0.0\" uly=\"0.0\" ");
            buffer.append("lrx=\"" + page.getWidth() + "\" lry=\"" + page.getHeight() + "\"");
            buffer.append("/>\n");
            pageNumber++;
        }
        buffer.append("\t</facsimile>\n");

        return buffer;
    }
}