package org.grobid.core.document;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.Date;
import org.grobid.core.data.*;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.FullMedicalTextParser;
import org.grobid.core.engines.citations.CalloutAnalyzer;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.*;
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

import static org.grobid.core.document.xml.XmlBuilderUtils.*;

/**
 * Class for generating a TEI representation of a document, taken and adapted from the TEIFormatter class (@author Patrice Lopez)
 * <p>
 * Tanti, 2021
 */

// the TEIFormatter cannot be extended because the variables are private
public class TEIFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TEIFormatter.class);

    private Document doc = null;
    private FullMedicalTextParser fullTextParser = null;
    public static final Set<TaggingLabel> MARKER_LABELS = Sets.newHashSet(
        MedicalLabels.FIGURE_MARKER,
        MedicalLabels.TABLE_MARKER);

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

    private static final String SCHEMA_XSD_LOCATION = "https://raw.githubusercontent.com/kermitt2/grobid/master/grobid-home/schemas/xsd/Grobid.xsd";
    private static final String SCHEMA_DTD_LOCATION = "https://raw.githubusercontent.com/kermitt2/grobid/master/grobid-home/schemas/dtd/Grobid.dtd";
    private static final String SCHEMA_RNG_LOCATION = "https://raw.githubusercontent.com/kermitt2/grobid/master/grobid-home/schemas/rng/Grobid.rng";

    public TEIFormatter(Document document, FullMedicalTextParser fullTextParser) {
        this.doc = document;
        this.fullTextParser = fullTextParser;
    }


    public static String toISOString(Date date) {
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
        }
        return when;
    }

    public StringBuilder toTEIHeaderLeftNote(HeaderMedicalItem headerItem,
                                             LeftNoteMedicalItem leftNoteMedicalItem,
                                             String defaultPublicationStatement,
                                             GrobidAnalysisConfig config) {
        return toTEIHeaderLeftNote(headerItem, leftNoteMedicalItem, SchemaDeclaration.XSD, defaultPublicationStatement, config);
    }

    public StringBuilder toTEIHeaderLeftNote(HeaderMedicalItem headerItem,
                                             LeftNoteMedicalItem leftNoteMedicalItem,
                                             SchemaDeclaration schemaDeclaration,
                                             String defaultPublicationStatement,
                                             GrobidAnalysisConfig config) {
        StringBuilder tei = new StringBuilder();
        tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (config.isWithXslStylesheet()) {
            tei.append("<?xml-stylesheet type=\"text/xsl\" href=\"../jsp/xmlverbatimwrapper.xsl\"?> \n");
        }
        if (schemaDeclaration == SchemaDeclaration.DTD) {
            tei.append("<!DOCTYPE TEI SYSTEM \"" + SCHEMA_DTD_LOCATION + "\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.XSD) {
            // XML schema
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" \n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "xsi:schemaLocation=\"http://www.tei-c.org/ns/1.0 " +
                SCHEMA_XSD_LOCATION +
                "\"\n xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
            //"\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
        } else if (schemaDeclaration == SchemaDeclaration.RNG) {
            // standard RelaxNG
            tei.append("<?xml-model href=\"" + SCHEMA_RNG_LOCATION +
                "\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\n");
        }

        // by default there is no schema association
        if (schemaDeclaration != SchemaDeclaration.XSD) {
            tei.append("<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\">\n");
        }

        // opening for the header and left-note parts
        if (doc.getLanguage() != null) {
            tei.append("\t<teiHeader xml:lang=\"" + doc.getLanguage() + "\">\n");
        } else {
            tei.append("\t<teiHeader>\n");
        }

        if (headerItem == null)  {
            // if the headerItem object is null, we simply create an empty one
            headerItem = new HeaderMedicalItem();
        } else if (leftNoteMedicalItem == null) {
            // if the leftNoteMedicalItem object is null, we simply create an empty one
            leftNoteMedicalItem = new LeftNoteMedicalItem();
        } else {
            tei.append("\t\t<fileDesc>\n\t\t\t<titleStmt>\n");

            // document title or document type
            if ((headerItem.getTitle() != null) || (headerItem.getDocumentType() != null)) {
                tei.append("\t\t\t\t<title type=\"main\">");
                if (headerItem.getTitle() != null) {
                    tei.append(TextUtilities.HTMLEncode(headerItem.getTitle()));
                }
                else if (headerItem.getDocumentType() != null) {
                    tei.append(TextUtilities.HTMLEncode(headerItem.getDocumentType()));
                }
                tei.append("</title>\n");
            }else {
                tei.append("\t\t\t\t<title xsi:nil=\"true\">\n");
            }

            // number of pages
            if (headerItem.getNbPages() > 0) {
                tei.append("\t\t\t\t<extent>\n");
                tei.append("\t\t\t\t\t<measure unit=\"pages\">" + headerItem.getNbPages() + "</measure>\n");
                tei.append("\t\t\t\t</extent>\n");
            }

            tei.append("\t\t\t</titleStmt>\n");

            tei.append("\t\t\t\t<availability status=\"unknown\">\n");
            tei.append("\t\t\t\t\t<p>Copyright : ");
            tei.append("©Assistance Publique – Hôpitaux de Paris (APHP)" + "</p>\n");
            tei.append("\t\t\t\t</availability>\n");

            // the format TEI : https://tei-c.org/release/doc/tei-p5-doc/en/html/HD.html#HD24
            if ((headerItem.getDocNum() != null) ||
                (headerItem.getLocation() != null) ||
                (headerItem.getDocumentDate() != null) ||
                (headerItem.getDocumentTime() != null) ||
                (headerItem.getDateline() != null) ||
                (headerItem.getAffiliation() != null) ||
                (headerItem.getAddress() != null) ||
                (headerItem.getPhone() != null) ||
                (headerItem.getFax() != null) ||
                (headerItem.getEmail() != null) ||
                (headerItem.getWeb() != null)) {

                tei.append("\t\t\t<publicationStmt>\n");

                // document number
                if (headerItem.getDocNum() != null) {
                    tei.append("\t\t\t\t<idno>" + TextUtilities.HTMLEncode(headerItem.getDocNum().replaceAll("\n", " ")) +
                        "</idno>\n");
                }

                // place name
                if (headerItem.getLocation() != null) {
                    tei.append("\t\t\t\t<pubPlace>" + TextUtilities.HTMLEncode(headerItem.getLocation().replaceAll("\n", "<lb/>")) +
                        "</pubPlace>\n");
                }

                // document date
                if (headerItem.getDocumentDate() != null && headerItem.getDocumentDate().length() > 0) {
                    tei.append("\t\t\t\t<date type=\"issued\" when=\"");
                    tei.append(headerItem.getDocumentDate()).append("\">");
                    tei.append(TextUtilities.HTMLEncode(headerItem.getDocumentDate()));
                    tei.append("</date>\n");
                } else if (headerItem.getListDatelines() != null) { // otherwise, call the dateline model
                    tei.append(headerItem.toTEIDatelineBlock(4, config));
                }

                // document publisher
                tei.append("\t\t\t\t<publisher>\n");
                if (headerItem.getAffiliation() != null) {
                    tei.append("\t\t\t\t\t<affiliation>" + TextUtilities.HTMLEncode(headerItem.getAffiliation()));
                    tei.append("</affiliation>\n");
                }
                // address if it exists
                if (headerItem.getAddress() != null) {
                    tei.append("\t\t\t\t\t<address>\n");
                    tei.append("\t\t\t\t\t\t<addrLine>" + TextUtilities.HTMLEncode(headerItem.getAddress()));
                    tei.append("</addrLine>\n");
                    tei.append("\t\t\t\t\t</address>\n");
                }
                // phone if it exists
                if (headerItem.getPhone() != null) {
                    tei.append("\t\t\t\t\t<phone>" + TextUtilities.HTMLEncode(headerItem.getPhone().replaceAll("\t", "; ").replaceAll("\n", " ")));
                    tei.append("</phone>\n");
                }
                // fax if it exists
                if (headerItem.getFax() != null) {
                    tei.append("\t\t\t\t\t<fax>" + TextUtilities.HTMLEncode(headerItem.getFax().replaceAll("\t", "; ").replaceAll("\n", " ")));
                    tei.append("</fax>\n");
                }
                // email if it exists
                if (headerItem.getEmail() != null) {
                    tei.append("\t\t\t\t\t<email>" + TextUtilities.HTMLEncode(headerItem.getEmail().replaceAll("\n", "")));
                    tei.append("</email>\n");
                }
                // web if it exists
                if (headerItem.getWeb() != null) {
                    tei.append("\t\t\t\t\t<ptr type=\"web\">" + TextUtilities.HTMLEncode(headerItem.getWeb().replaceAll("\n", "")));
                    tei.append("</ptr>\n");
                }
                tei.append("\t\t\t\t</publisher>\n");
                tei.append("\t\t\t</publicationStmt>\n");
            } else {
                tei.append("\t\t\t<publicationStmt>\n");
                tei.append("\t\t\t\t<publisher>").append("©Assistance Publique – Hôpitaux de Paris (APHP)").append("</publisher>\n");
                tei.append("\t\t\t\t<availability status=\"unknown\"><licence/></availability>\n");
                tei.append("\t\t\t</publicationStmt>\n");
            }
            tei.append("\t\t\t<sourceDesc>\n");

            // medics + information related to it
            if (headerItem.getListMedics() != null) {
                tei.append(headerItem.toTEIMedicBlock(4, config));
            }

            if (headerItem.getListPatients() != null) {
                // patients + information related to it
                tei.append(headerItem.toTEIPatientBlock(4, config));
            }

            // information concerning organization from the left-note part of the medical reports if they exist
            if (leftNoteMedicalItem.getRawLeftNote() != null && leftNoteMedicalItem.getRawLeftNote().length() > 0) {
                //tei.append(leftNoteMedicalItem.toTEILeftNoteBlock(4, config));
                tei.append("\t\t\t\t<listOrg>");
                tei.append("\t\t\t\t").append(leftNoteMedicalItem.getRawLeftNote().replaceAll("<lb/>", "").
                    replaceAll("<person>","<listPerson type=\"medics\">").
                    replaceAll("<medic>","\t<medic>").
                    replaceAll("</medic>","</medic>").
                    replaceAll("</person>","</listPerson>").
                    replaceAll("\n", "\n\t\t\t\t"));
                tei.append("</listOrg>\n");
            }

            tei.append("\t\t\t</sourceDesc>\n");
            tei.append("\t\t</fileDesc>\n");

            // encodingDesc gives info about the producer of the file
            tei.append("\t\t<encodingDesc>\n");
            tei.append("\t\t\t<appInfo>\n");

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            df.setTimeZone(tz);
            String dateISOString = df.format(new java.util.Date());

            tei.append("\t\t\t\t<application version=\"" + MedicalReportProperties.getVersion() +
                "\" ident=\"grobid-medical-report\" when=\"" + dateISOString + "\">\n");
            tei.append("\t\t\t\t\t<desc>grobid-medical-report is a GROBID (https://github.com/kermitt2/grobid) module for extracting and structuring medical reports into structured XML/TEI encoded documents.</desc>\n");
            tei.append("\t\t\t\t\t<ref target=\"https://github.com/tantikristanti/grobid-medical-report\"/>\n");
            tei.append("\t\t\t\t</application>\n");
            tei.append("\t\t\t</appInfo>\n");
            tei.append("\t\t</encodingDesc>\n");

            boolean textClassWritten = false;

            tei.append("\t\t<profileDesc>\n");

            if (textClassWritten)
                tei.append("\t\t\t</textClass>\n");

            tei.append("\t\t</profileDesc>\n");
        }
        tei.append("\t</teiHeader>\n");

        // output pages dimensions in the case coordinates will also be provided for some structures
        try {
            tei = toTEIPages(tei, doc, config);
        } catch (Exception e) {
            LOGGER.warn("Problem when serializing page size", e);
        }

        // opening for the body part
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
                                   HeaderMedicalItem headerItem,
                                   LayoutTokenization layoutTokenization,
                                   List<Figure> figures,
                                   List<Table> tables,
                                   List<CalloutAnalyzer.MarkerType> markerTypes,
                                   Document doc,
                                   GrobidAnalysisConfig config) throws Exception {
        if ((result == null) || (layoutTokenization == null) || (layoutTokenization.getTokenization() == null)) {
            buffer.append("\t\t<body/>\n");
            return buffer;
        }
        buffer.append("\t\t<body>\n");
        buffer = toTEITextPiece(buffer, result, headerItem, true,
            layoutTokenization, figures, tables, markerTypes, doc, config);

        // notes are still in the body
        //buffer = toTEINote(buffer, doc, markerTypes, config);

        buffer.append("</body>\n");

        return buffer;
    }

    private StringBuilder toTEINote(StringBuilder tei,
                                    Document doc,
                                    List<CalloutAnalyzer.MarkerType> markerTypes,
                                    GrobidAnalysisConfig config) throws Exception {
        // write the notes
        SortedSet<DocumentPiece> documentNoteParts = doc.getDocumentPart(MedicalLabels.FOOTNOTE);
        if (documentNoteParts != null) {
            tei = toTEINote("foot", documentNoteParts, tei, markerTypes, doc, config);
        }
        documentNoteParts = doc.getDocumentPart(MedicalLabels.HEADNOTE);
        if (documentNoteParts != null) {
            tei = toTEINote("head", documentNoteParts, tei, markerTypes, doc, config);
        }
        tei.append("\n\t\t\t");
        return tei;
    }

    private StringBuilder toTEINote(String noteType,
                                    SortedSet<DocumentPiece> documentNoteParts,
                                    StringBuilder tei,
                                    List<CalloutAnalyzer.MarkerType> markerTypes,
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
            new LayoutTokenization(tokenizationsAcknowledgement), null, null, null, doc, config);
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
                                    List<CalloutAnalyzer.MarkerType> markerTypes,
                                    Document doc,
                                    GrobidAnalysisConfig config) throws Exception {
        if ((result == null) || (tokenizations == null)) {
            return buffer;
        }

        buffer.append("\t\t\t<div type=\"annex\">\n");
        buffer = toTEITextPiece(buffer, result, headerItem, true,
            new LayoutTokenization(tokenizations), null, null, markerTypes, doc, config);
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
                                        List<CalloutAnalyzer.MarkerType> markerTypes,
                                        Document doc,
                                        GrobidAnalysisConfig config) throws Exception {
        TaggingLabel lastClusterLabel = null;
        int startPosition = buffer.length();

        //boolean figureBlock = false; // indicate that a figure or table sequence was met
        // used for reconnecting a paragraph that was cut by a figure/table

        List<LayoutToken> tokenizations = layoutTokenization.getTokenization();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULL_MEDICAL_TEXT, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        List<Element> divResults = new ArrayList<>();

        Element curDiv = teiElement("div");
        if (config.isGenerateTeiIds()) {
            String divID = KeyGen.getKey().substring(0, 7);
            addXmlId(curDiv, "_" + divID);
        }
        divResults.add(curDiv);
        Element curParagraph = null;
        List<LayoutToken> curParagraphTokens = null;
        Element curList = null;
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);
            if (clusterLabel.equals(MedicalLabels.TITLE)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                Element note = teiElement("title", clusterContent);
                curDiv.appendChild(note);
            } else if (clusterLabel.equals(MedicalLabels.SECTION)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                curDiv = teiElement("div");
                Element head = teiElement("head");
                head.addAttribute(new Attribute("level", "1"));
                // section numbers
                Pair<String, String> numb = getSectionNumber(clusterContent);
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

                if (config.isGenerateTeiCoordinates("head")) {
                    String coords = LayoutTokensUtil.getCoordsString(cluster.concatTokens());
                    if (coords != null) {
                        head.addAttribute(new Attribute("coords", coords));
                    }
                }

                curDiv.appendChild(head);
                divResults.add(curDiv);
            } else if (clusterLabel.equals(MedicalLabels.SUB_SECTION)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                curDiv = teiElement("div");
                Element head = teiElement("head");
                head.addAttribute(new Attribute("level", "2"));
                // section numbers
                Pair<String, String> numb = getSectionNumber(clusterContent);
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

                if (config.isGenerateTeiCoordinates("head")) {
                    String coords = LayoutTokensUtil.getCoordsString(cluster.concatTokens());
                    if (coords != null) {
                        head.addAttribute(new Attribute("coords", coords));
                    }
                }

                curDiv.appendChild(head);
                divResults.add(curDiv);
            } else if (clusterLabel.equals(MedicalLabels.ITEM)) {
                String clusterContent = LayoutTokensUtil.normalizeText(cluster.concatTokens());
                //curDiv.appendChild(teiElement("item", clusterContent));
                Element itemNode = teiElement("item", clusterContent);
                if (!MARKER_LABELS.contains(lastClusterLabel) && (lastClusterLabel != MedicalLabels.ITEM)) {
                    curList = teiElement("list");
                    curDiv.appendChild(curList);
                }
                if (curList != null) {
                    curList.appendChild(itemNode);
                }
            } else if (clusterLabel.equals(MedicalLabels.PARAGRAPH)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                if (isNewParagraph(lastClusterLabel, curParagraph)) {
                    if (curParagraph != null && config.isWithSentenceSegmentation()) {
                        segmentIntoSentences(curParagraph, curParagraphTokens, config, doc.getLanguage());
                    }
                    curParagraph = teiElement("p");
                    if (config.isGenerateTeiIds()) {
                        String divID = KeyGen.getKey().substring(0, 7);
                        addXmlId(curParagraph, "_" + divID);
                    }
                    curDiv.appendChild(curParagraph);
                    curParagraphTokens = new ArrayList<>();
                }
                curParagraph.appendChild(clusterContent);
                curParagraphTokens.addAll(cluster.concatTokens());
            } else if (MARKER_LABELS.contains(clusterLabel)) {
                List<LayoutToken> refTokens = cluster.concatTokens();
                refTokens = LayoutTokensUtil.dehyphenize(refTokens);
                String chunkRefString = LayoutTokensUtil.toText(refTokens);

                Element parent = curParagraph != null ? curParagraph : curDiv;
                parent.appendChild(new Text(" "));

                List<Node> refNodes;
                CalloutAnalyzer.MarkerType citationMarkerType = null;
                if (markerTypes != null && markerTypes.size() > 0) {
                    citationMarkerType = markerTypes.get(0);
                }
                if (clusterLabel.equals(MedicalLabels.FIGURE_MARKER)) {
                    refNodes = markReferencesFigureTEI(chunkRefString, refTokens, figures,
                        config.isGenerateTeiCoordinates("ref"));
                } else if (clusterLabel.equals(MedicalLabels.TABLE_MARKER)) {
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
                if (curParagraph != null)
                    curParagraphTokens.addAll(cluster.concatTokens());
            } else if (clusterLabel.equals(MedicalLabels.FIGURE) || clusterLabel.equals(MedicalLabels.TABLE)) {
                //figureBlock = true;
                if (curParagraph != null)
                    curParagraph.appendChild(new Text(" "));
            } if (clusterLabel.equals(MedicalLabels.PATIENT)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                Element note = teiElement("patient", clusterContent);
                curDiv.appendChild(note);
            } if (clusterLabel.equals(MedicalLabels.MEDIC)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                Element note = teiElement("medic", clusterContent);
                curDiv.appendChild(note);
            } else if (clusterLabel.equals(MedicalLabels.OTHER)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                Element note = teiElement("note", clusterContent);
                note.addAttribute(new Attribute("type", "other"));
                if (config.isGenerateTeiIds()) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    addXmlId(note, "_" + divID);
                }
                curDiv.appendChild(note);
            }

            lastClusterLabel = cluster.getTaggingLabel();
        }

        // in case we segment paragraph into sentences, we still need to do it for the last paragraph
        if (curParagraph != null && config.isWithSentenceSegmentation()) {
            segmentIntoSentences(curParagraph, curParagraphTokens, config, doc.getLanguage());
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
                String figSeg = figure.toTEI(config, doc, this, markerTypes);
                if (figSeg != null) {
                    buffer.append(figSeg).append("\n");
                }
            }
        }
        if (tables != null) {
            for (Table table : tables) {
                String tabSeg = table.toTEI(config, doc, this, markerTypes);
                if (tabSeg != null) {
                    buffer.append(tabSeg).append("\n");
                }
            }
        }

        buffer.append("\n\t\t");
        return buffer;
    }

    private boolean isNewParagraph(TaggingLabel lastClusterLabel, Element curParagraph) {
        return (!MARKER_LABELS.contains(lastClusterLabel) && lastClusterLabel != MedicalLabels.FIGURE
            && lastClusterLabel != MedicalLabels.TABLE) || curParagraph == null;
    }

    public void segmentIntoSentences(Element curParagraph, List<LayoutToken> curParagraphTokens, GrobidAnalysisConfig config, String lang) {
        // in order to avoid having a sentence boundary in the middle of a ref element
        // (which is frequent given the abbreviation in the reference expression, e.g. Fig.)
        // we only consider for sentence segmentation texts under <p> and skip the text under <ref>.
        if (curParagraph == null)
            return;

        // in xom, the following gives all the text under the element, for the whole subtree
        String text = curParagraph.getValue();

        // identify ref nodes, ref spans and ref positions
        Map<Integer, Node> mapRefNodes = new HashMap<>();
        List<Integer> refPositions = new ArrayList<>();
        List<OffsetPosition> forbiddenPositions = new ArrayList<>();
        int pos = 0;
        for (int i = 0; i < curParagraph.getChildCount(); i++) {
            Node theNode = curParagraph.getChild(i);
            if (theNode instanceof Text) {
                String chunk = theNode.getValue();
                pos += chunk.length();
            } else if (theNode instanceof Element) {
                // for readability in another conditional
                if (((Element) theNode).getLocalName().equals("ref")) {
                    // map character offset of the node
                    mapRefNodes.put(pos, theNode);
                    refPositions.add(pos);

                    String chunk = theNode.getValue();
                    forbiddenPositions.add(new OffsetPosition(pos, pos + chunk.length()));
                    pos += chunk.length();
                }
            }
        }

        List<OffsetPosition> theSentences =
            SentenceUtilities.getInstance().runSentenceDetection(text, forbiddenPositions, curParagraphTokens, new Language(lang));

        /*if (theSentences.size() == 0) {
            // this should normally not happen, but it happens (depending on sentence splitter, usually the text
            // is just a punctuation)
            // in this case we consider the current text as a unique sentence as fall back
            theSentences.add(new OffsetPosition(0, text.length()));
        }*/

        // segment the list of layout tokens according to the sentence segmentation if the coordinates are needed
        List<List<LayoutToken>> segmentedParagraphTokens = new ArrayList<>();
        List<LayoutToken> currentSentenceTokens = new ArrayList<>();
        pos = 0;

        if (config.isGenerateTeiCoordinates("s")) {

            int currentSentenceIndex = 0;
//System.out.println(text);
//System.out.println("theSentences.size(): " + theSentences.size());
            String sentenceChunk = text.substring(theSentences.get(currentSentenceIndex).start, theSentences.get(currentSentenceIndex).end);

            for (int i = 0; i < curParagraphTokens.size(); i++) {
                LayoutToken token = curParagraphTokens.get(i);
                if (token.getText() == null || token.getText().length() == 0)
                    continue;
                int newPos = sentenceChunk.indexOf(token.getText(), pos);
                if ((newPos != -1) || SentenceUtilities.toSkipToken(token.getText())) {
                    // just move on
                    currentSentenceTokens.add(token);
                    if (newPos != -1 && !SentenceUtilities.toSkipToken(token.getText()))
                        pos = newPos;
                } else {
                    if (currentSentenceTokens.size() > 0) {
                        segmentedParagraphTokens.add(currentSentenceTokens);
                        currentSentenceIndex++;
                        if (currentSentenceIndex >= theSentences.size())
                            break;
                        sentenceChunk = text.substring(theSentences.get(currentSentenceIndex).start, theSentences.get(currentSentenceIndex).end);
                    }
                    currentSentenceTokens = new ArrayList<>();
                    currentSentenceTokens.add(token);
                    pos = 0;
                }

                if (currentSentenceIndex >= theSentences.size())
                    break;
            }
            // last sentence
            if (currentSentenceTokens.size() > 0) {
                // check sentence index too ?
                segmentedParagraphTokens.add(currentSentenceTokens);
            }

/*if (segmentedParagraphTokens.size() != theSentences.size()) {
System.out.println("ERROR, segmentedParagraphTokens size:" + segmentedParagraphTokens.size() + " vs theSentences size: " + theSentences.size());
System.out.println(text);
System.out.println(theSentences.toString());
}*/

        }

        // update the xml paragraph element
        int currenChildIndex = 0;
        pos = 0;
        int posInSentence = 0;
        int refIndex = 0;
        for (int i = 0; i < theSentences.size(); i++) {
            pos = theSentences.get(i).start;
            posInSentence = 0;
            Element sentenceElement = teiElement("s");
            if (config.isGenerateTeiIds()) {
                String sID = KeyGen.getKey().substring(0, 7);
                addXmlId(sentenceElement, "_" + sID);
            }
            if (config.isGenerateTeiCoordinates("s")) {
                if (segmentedParagraphTokens.size() >= i + 1) {
                    currentSentenceTokens = segmentedParagraphTokens.get(i);
                    String coords = LayoutTokensUtil.getCoordsString(currentSentenceTokens);
                    if (coords != null) {
                        sentenceElement.addAttribute(new Attribute("coords", coords));
                    }
                }
            }

            int sentenceLength = theSentences.get(i).end - pos;
            // check if we have a ref between pos and pos+sentenceLength
            for (int j = refIndex; j < refPositions.size(); j++) {
                int refPos = refPositions.get(j).intValue();
                if (refPos < pos + posInSentence)
                    continue;

                if (refPos >= pos + posInSentence && refPos <= pos + sentenceLength) {
                    Node valueNode = mapRefNodes.get(refPos);
                    if (pos + posInSentence < refPos)
                        sentenceElement.appendChild(text.substring(pos + posInSentence, refPos));
                    valueNode.detach();
                    sentenceElement.appendChild(valueNode);
                    refIndex = j;
                    posInSentence = refPos + valueNode.getValue().length() - pos;
                }
                if (refPos > pos + sentenceLength) {
                    break;
                }
            }

            if (pos + posInSentence <= theSentences.get(i).end) {
                sentenceElement.appendChild(text.substring(pos + posInSentence, theSentences.get(i).end));
                curParagraph.appendChild(sentenceElement);
            }
        }

        for (int i = curParagraph.getChildCount() - 1; i >= 0; i--) {
            Node theNode = curParagraph.getChild(i);
            if (theNode instanceof Text) {
                curParagraph.removeChild(theNode);
            } else if (theNode instanceof Element) {
                // for readability in another conditional
                if (!((Element) theNode).getLocalName().equals("s")) {
                    curParagraph.removeChild(theNode);
                }
            }
        }

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

    private Pair<String, String> getSectionNumber(String text) {
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
            return new Pair<>(text, numb);
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

    /**
     * Mark using TEI annotations the identified references in the text body build with the machine learning model.
     */
    public List<Node> markReferencesTEILuceneBased(List<LayoutToken> refTokens,
                                                   ReferenceMarkerMatcher markerMatcher,
                                                   boolean generateCoordinates,
                                                   boolean keepUnsolvedCallout) throws EntityMatcherException {
        // safety tests
        if ((refTokens == null) || (refTokens.size() == 0))
            return null;
        String text = LayoutTokensUtil.toText(refTokens);
        if (text == null || text.trim().length() == 0 || text.endsWith("</ref>") || text.startsWith("<ref") || markerMatcher == null)
            return Collections.<Node>singletonList(new Text(text));

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" "))
            spaceEnd = true;
        //System.out.println("callout text: " + text);
        List<Node> nodes = new ArrayList<>();
        List<ReferenceMarkerMatcher.MatchResult> matchResults = markerMatcher.match(refTokens);
        if (matchResults != null) {
            for (ReferenceMarkerMatcher.MatchResult matchResult : matchResults) {
                // no need to HTMLEncode since XOM will take care about the correct escaping
                String markerText = LayoutTokensUtil.normalizeText(matchResult.getText());
                String coords = null;
                if (generateCoordinates && matchResult.getTokens() != null) {
                    coords = LayoutTokensUtil.getCoordsString(matchResult.getTokens());
                }

                Element ref = teiElement("ref");
                ref.addAttribute(new Attribute("type", "bibr"));

                if (coords != null) {
                    ref.addAttribute(new Attribute("coords", coords));
                }
                ref.appendChild(markerText);

                boolean solved = false;
                if (matchResult.getBibDataSet() != null) {
                    ref.addAttribute(new Attribute("target", "#b" + matchResult.getBibDataSet().getResBib().getOrdinal()));
                    solved = true;
                }
                if (solved || (!solved && keepUnsolvedCallout))
                    nodes.add(ref);
                else
                    nodes.add(textNode(matchResult.getText()));
            }
        }
        if (spaceEnd)
            nodes.add(new Text(" "));
        return nodes;
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