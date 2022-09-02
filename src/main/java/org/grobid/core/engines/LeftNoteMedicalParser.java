package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.LeftNoteMedicalItem;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorLeftNoteMedical;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * A class to extract the left-note part of medical reports.
 * <p>
 * Tanti, 2020
 */
public class LeftNoteMedicalParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeftNoteMedicalParser.class);
    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    private EngineMedicalParsers parsers;
    private Lexicon lexicon = Lexicon.getInstance();
    Utility utility = null;
    // default bins for relative position
    private static final int NBBINS_POSITION = 12;
    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;
    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;
    // projection scale for line length
    private static final int LINESCALE = 10;

    public LeftNoteMedicalParser() {
        super(GrobidModels.LEFT_NOTE_MEDICAL_REPORT);
    }

    public LeftNoteMedicalParser(EngineMedicalParsers parsers) {
        super(GrobidModels.LEFT_NOTE_MEDICAL_REPORT);
        this.parsers = parsers;
    }

    public LeftNoteMedicalParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, cntManager);
        this.parsers = parsers;
    }

    /**
     * Processing left-note after the application of the medical-report segmentation model
     */
    public Pair<String, Document> processingLeftNote(File input, String md5Str, LeftNoteMedicalItem resLeftNote, GrobidAnalysisConfig config) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(input, config.getStartPage(), config.getEndPage());
            documentSource.setMD5(md5Str);
            // first, parse the document with the segmentation model
            Document doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            // then take only the left note parts for further process with this method
            String tei = processingLeftNoteSection(config, doc, resLeftNote, true);
            return new ImmutablePair<String, Document>(tei, doc);
        } finally {
            if (documentSource != null) {
                documentSource.close(true, true, true);
            }
        }
    }

    /**
     * Left-Note processing after application of the medical-report segmentation model
     */
    public String processingLeftNoteSection(GrobidAnalysisConfig config, Document doc, LeftNoteMedicalItem resLeftNote, boolean serialize) {
        try {
            // retrieve only the left-note part
            SortedSet<DocumentPiece> documentLeftNoteParts = doc.getDocumentPart(MedicalLabels.LEFTNOTE);
            List<LayoutToken> tokenizations = doc.getTokenizations(); // tokens of the entire document

            if (documentLeftNoteParts != null) {
                Pair<String, List<LayoutToken>> featuredLeftNote = getSectionLeftNoteFeatured(doc, documentLeftNoteParts);
                String leftNote = featuredLeftNote.getLeft(); // data with features
                List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight(); // tokens
                String res = null;
                if (StringUtils.isNotBlank(leftNote)) {
                    // give labels
                    res = label(leftNote);

                    // save the labeled results in POJO
                    resLeftNote = resultExtraction(res, leftNoteTokenization, resLeftNote);

                    // set the labeled results without any change to the raw text
                    String strLeftNote = trainingExtraction(res, leftNoteTokenization).toString();
                    resLeftNote.setRawLeftNote(strLeftNote);

                    // take the results of the left-note parsing and complete the items with additional information
                    if (resLeftNote != null) {
                        // set the application version
                        resLeftNote.setAppVersion(GrobidMedicalReportProperties.getVersion());

                        // set the language
                        String lang = "fr"; // default, it's French
                        // otherwise, try the language identification from the body part text
                        StringBuilder textBody = new StringBuilder();
                        String contentSample = "";
                        SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
                        if (documentBodyParts != null) {
                            Pair<String, LayoutTokenization> featSeg = parsers.getFullMedicalTextParser().getBodyTextFeatured(doc, documentBodyParts);
                            if (featSeg != null) {
                                String bodytext = featSeg.getLeft(); // body data with features
                                List<LayoutToken> tokenizationsBody = featSeg.getRight().getTokenization(); // body tokens
                                for (LayoutToken token : tokenizationsBody) {
                                    textBody.append(token.getText());
                                }
                            }
                        }
                        // test only max 200 characters
                        if (textBody.toString().length() > 200) {
                            contentSample = textBody.toString().substring(0, 200);
                        } else {
                            contentSample = textBody.toString();
                        }
                        // define the new language if it exists
                        Language langu = languageUtilities.runLanguageId(contentSample);
                        if (langu != null) {
                            doc.setLanguage(lang);
                        }
                        doc.setLanguage(lang);
                        resLeftNote.setLanguage(lang);

                        // set number of pages
                        resLeftNote.setNbPages(doc.getPages().size());
                    }
                }
                if (serialize) { // need to set the `serialize` into false for the full text processing for preventing the double process
                    TEIFormatter teiFormatter = new TEIFormatter(doc, null);
                    StringBuilder tei = teiFormatter.toTEILeftNote(resLeftNote, null, config);
                    tei.append("</TEI>\n");
                    return tei.toString();
                } else
                    return null;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
        return null;
    }

    /**
     * Return the left-note section with features to be processed by the sequence labelling model
     */
    public Pair<String, List<LayoutToken>> getSectionLeftNoteFeatured(Document doc,
                                                                      SortedSet<DocumentPiece> documentLeftNoteParts) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        StringBuilder leftNote = new StringBuilder();
        String currentFont = null;
        int currentFontSize = -1;

        // vector for features
        FeaturesVectorLeftNoteMedical features;
        FeaturesVectorLeftNoteMedical previousFeatures = null;

        double lineStartX = Double.NaN;
        boolean indented = false;
        boolean centered = false;

        boolean endblock;
        //for (Integer blocknum : blockDocumentLeftNotes) {
        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        List<LayoutToken> leftNoteTokenizations = new ArrayList<LayoutToken>();

        // find the largest, smallest and average size font on the left-note section
        // note: only  largest font size information is used currently
        double largestFontSize = 0.0;
        double smallestFontSize = 100000.0;
        double averageFontSize;
        double accumulatedFontSize = 0.0;
        int nbTokens = 0;
        for (DocumentPiece docPiece : documentLeftNoteParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
                Block block = blocks.get(blockIndex);

                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }

                for (LayoutToken token : tokens) {
                    if (token.getFontSize() > largestFontSize) {
                        largestFontSize = token.getFontSize();
                    }

                    if (token.getFontSize() < smallestFontSize) {
                        smallestFontSize = token.getFontSize();
                    }

                    accumulatedFontSize += token.getFontSize();
                    nbTokens++;
                }
            }
        }
        averageFontSize = accumulatedFontSize / nbTokens;

        for (DocumentPiece docPiece : documentLeftNoteParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
                Block block = blocks.get(blockIndex);
                boolean newline = false;
                boolean previousNewline = true;
                endblock = false;
                double spacingPreviousBlock = 0.0; // discretized

                if (previousFeatures != null)
                    previousFeatures.blockStatus = "BLOCKEND";

                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }

                String localText = block.getText();
                if (localText == null)
                    continue;
                int startIndex = 0;
                int n = 0;
                if (blockIndex == dp1.getBlockPtr()) {
                    //n = block.getStartToken();
                    n = dp1.getTokenDocPos() - block.getStartToken();
                    startIndex = dp1.getTokenDocPos() - block.getStartToken();
                }

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                    (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                    (!block.getText().contains("@IMAGE")))
                    density = (double) block.getText().length() / (block.getHeight() * block.getWidth());

                String[] lines = localText.split("[\\n\\r]");
                // set the max length of the lines in the block, in number of characters
                int maxLineLength = 0;
                for (int p = 0; p < lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }

                List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
                List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
                List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);

                while (n < tokens.size()) {
                    if (blockIndex == dp2.getBlockPtr()) {
                        if (n > dp2.getTokenDocPos() - block.getStartToken()) {
                            break;
                        }
                    }

                    LayoutToken token = tokens.get(n);
                    leftNoteTokenizations.add(token);

                    String text = token.getText();
                    if (text == null) {
                        n++;
                        continue;
                    }

                    text = text.replace(" ", "");
                    if (text.length() == 0) {
                        n++;
                        continue;
                    }

                    if (text.equals("\n") || text.equals("\r")) {
                        previousNewline = true;
                        newline = false;
                        n++;
                        continue;
                    }

                    if (previousNewline) {
                        newline = true;
                        previousNewline = false;
                        if (previousFeatures != null) {
                            double previousLineStartX = lineStartX;
                            lineStartX = token.getX();
                            double characterWidth = token.width / token.getText().length();
                            if (!Double.isNaN(previousLineStartX)) {
                                // Indentation if line start is > 1 character width to the right of previous line start
                                if (lineStartX - previousLineStartX > characterWidth)
                                    indented = true;
                                    // Indentation ends if line start is > 1 character width to the left of previous line start
                                else if (previousLineStartX - lineStartX > characterWidth)
                                    indented = false;
                                // Otherwise indentation is unchanged
                            }
                        }
                    } else {
                        newline = false;
                    }
                    // centered ?

                    // final sanitisation and filtering for the token
                    text = text.replaceAll("[ \n]", "");
                    if (TextUtilities.filterLine(text)) {
                        n++;
                        continue;
                    }

                    features = new FeaturesVectorLeftNoteMedical();
                    features.token = token;
                    features.string = text;

                    if (newline)
                        features.lineStatus = "LINESTART";

                    Matcher m0 = featureFactory.isPunct.matcher(text);
                    if (m0.find()) {
                        features.punctType = "PUNCT";
                    }
                    if (text.equals("(") || text.equals("[")) {
                        features.punctType = "OPENBRACKET";

                    } else if (text.equals(")") || text.equals("]")) {
                        features.punctType = "ENDBRACKET";

                    } else if (text.equals(".")) {
                        features.punctType = "DOT";

                    } else if (text.equals(",")) {
                        features.punctType = "COMMA";

                    } else if (text.equals("-")) {
                        features.punctType = "HYPHEN";

                    } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
                        features.punctType = "QUOTE";
                    }

                    if (n == startIndex) {
                        // beginning of block
                        features.lineStatus = "LINESTART";
                        features.blockStatus = "BLOCKSTART";
                    } else if ((n == tokens.size() - 1) || (n + 1 > dp2.getTokenDocPos() - block.getStartToken())) {
                        // end of block
                        features.lineStatus = "LINEEND";
                        previousNewline = true;
                        features.blockStatus = "BLOCKEND";
                        endblock = true;
                    } else {
                        // look ahead to see if we are at the end of a line within the block
                        boolean endline = false;

                        int ii = 1;
                        boolean endloop = false;
                        while ((n + ii < tokens.size()) && (!endloop)) {
                            LayoutToken tok = tokens.get(n + ii);
                            if (tok != null) {
                                String toto = tok.getText();
                                if (toto != null) {
                                    if (toto.equals("\n") || text.equals("\r")) {
                                        endline = true;
                                        endloop = true;
                                    } else {
                                        if ((toto.trim().length() != 0)
                                            && (!text.equals("\u00A0"))
                                            && (!(toto.contains("@IMAGE")))
                                            && (!(toto.contains("@PAGE")))
                                            && (!text.contains(".pbm"))
                                            && (!text.contains(".ppm"))
                                            && (!text.contains(".png"))
                                            && (!text.contains(".svg"))
                                            && (!text.contains(".jpg"))) {
                                            endloop = true;
                                        }
                                    }
                                }
                            }

                            if (n + ii == tokens.size() - 1) {
                                endblock = true;
                                endline = true;
                            }

                            ii++;
                        }

                        if ((!endline) && !(newline)) {
                            features.lineStatus = "LINEIN";
                        } else if (!newline) {
                            features.lineStatus = "LINEEND";
                            previousNewline = true;
                        }

                        if ((!endblock) && (features.blockStatus == null))
                            features.blockStatus = "BLOCKIN";
                        else if (features.blockStatus == null)
                            features.blockStatus = "BLOCKEND";
                    }

                    if (indented) {
                        features.alignmentStatus = "LINEINDENT";
                    } else {
                        features.alignmentStatus = "ALIGNEDLEFT";
                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    // check token offsets for email and http address, or known location
                    if (locationPositions != null) {
                        for (OffsetPosition thePosition : locationPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.locationName = true;
                                break;
                            }
                        }
                    }
                    if (emailPositions != null) {
                        for (OffsetPosition thePosition : emailPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.email = true;
                                break;
                            }
                        }
                    }
                    if (urlPositions != null) {
                        for (OffsetPosition thePosition : urlPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.http = true;
                                break;
                            }
                        }
                    }

                    if (currentFont == null) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else if (!currentFont.equals(token.getFont())) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else
                        features.fontStatus = "SAMEFONT";

                    int newFontSize = (int) token.getFontSize();
                    if (currentFontSize == -1) {
                        currentFontSize = newFontSize;
                        features.fontSize = "HIGHERFONT";
                    } else if (currentFontSize == newFontSize) {
                        features.fontSize = "SAMEFONTSIZE";
                    } else if (currentFontSize < newFontSize) {
                        features.fontSize = "HIGHERFONT";
                        currentFontSize = newFontSize;
                    } else if (currentFontSize > newFontSize) {
                        features.fontSize = "LOWERFONT";
                        currentFontSize = newFontSize;
                    }

                    if (token.getFontSize() == largestFontSize)
                        features.largestFont = true;
                    if (token.getFontSize() == smallestFontSize)
                        features.smallestFont = true;
                    if (token.getFontSize() > averageFontSize)
                        features.largerThanAverageFont = true;

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    if (features.punctType == null)
                        features.punctType = "NOPUNCT";

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density - doc.getMinCharacterDensity(), doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(), NBBINS_DENSITY);
//System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    if (previousFeatures != null)
                        leftNote.append(previousFeatures.printVector());
                    previousFeatures = features;

                    n++;
                }

                if (previousFeatures != null) {
                    previousFeatures.blockStatus = "BLOCKEND";
                    previousFeatures.lineStatus = "LINEEND";
                    leftNote.append(previousFeatures.printVector());
                    previousFeatures = null;
                }
            }
        }

        return Pair.of(leftNote.toString(), leftNoteTokenizations);
    }

    /**
     * Return the left-note section with features to be processed by the sequence labelling model
     */
    public Pair<String, List<LayoutToken>> getSectionLeftNoteFeaturedAnonym(Document doc,
                                                                            SortedSet<DocumentPiece> documentLeftNoteParts,
                                                                            List<String> dataOriginal,
                                                                            List<String> dataAnonymized) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        StringBuilder leftNote = new StringBuilder();
        String currentFont = null;
        int currentFontSize = -1;

        // vector for features
        FeaturesVectorLeftNoteMedical features;
        FeaturesVectorLeftNoteMedical previousFeatures = null;

        double lineStartX = Double.NaN;
        boolean indented = false;
        boolean centered = false;

        boolean endblock;
        //for (Integer blocknum : blockDocumentLeftNotes) {
        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        List<LayoutToken> leftNoteTokenizations = new ArrayList<LayoutToken>();

        // find the largest, smallest and average size font on the left-note section
        // note: only  largest font size information is used currently
        double largestFontSize = 0.0;
        double smallestFontSize = 100000.0;
        double averageFontSize;
        double accumulatedFontSize = 0.0;
        int nbTokens = 0;
        for (DocumentPiece docPiece : documentLeftNoteParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
                Block block = blocks.get(blockIndex);

                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }

                for (LayoutToken token : tokens) {
                    if (token.getFontSize() > largestFontSize) {
                        largestFontSize = token.getFontSize();
                    }

                    if (token.getFontSize() < smallestFontSize) {
                        smallestFontSize = token.getFontSize();
                    }

                    accumulatedFontSize += token.getFontSize();
                    nbTokens++;
                }
            }
        }
        averageFontSize = accumulatedFontSize / nbTokens;

        for (DocumentPiece docPiece : documentLeftNoteParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
                Block block = blocks.get(blockIndex);
                boolean newline = false;
                boolean previousNewline = true;
                endblock = false;
                double spacingPreviousBlock = 0.0; // discretized

                if (previousFeatures != null)
                    previousFeatures.blockStatus = "BLOCKEND";

                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }

                String localText = block.getText();
                if (localText == null)
                    continue;
                int startIndex = 0;
                int n = 0;
                if (blockIndex == dp1.getBlockPtr()) {
                    //n = block.getStartToken();
                    n = dp1.getTokenDocPos() - block.getStartToken();
                    startIndex = dp1.getTokenDocPos() - block.getStartToken();
                }

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                    (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                    (!block.getText().contains("@IMAGE")))
                    density = (double) block.getText().length() / (block.getHeight() * block.getWidth());

                String[] lines = localText.split("[\\n\\r]");
                // set the max length of the lines in the block, in number of characters
                int maxLineLength = 0;
                for (int p = 0; p < lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }

                List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
                List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
                List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);

                while (n < tokens.size()) {
                    if (blockIndex == dp2.getBlockPtr()) {
                        if (n > dp2.getTokenDocPos() - block.getStartToken()) {
                            break;
                        }
                    }

                    LayoutToken token = tokens.get(n);
                    leftNoteTokenizations.add(token);

                    String text = token.getText();
                    if (text == null) {
                        n++;
                        continue;
                    }

                    text = text.replace(" ", "");
                    if (text.length() == 0) {
                        n++;
                        continue;
                    }

                    if (text.equals("\n") || text.equals("\r")) {
                        previousNewline = true;
                        newline = false;
                        n++;
                        continue;
                    }

                    if (previousNewline) {
                        newline = true;
                        previousNewline = false;
                        if (previousFeatures != null) {
                            double previousLineStartX = lineStartX;
                            lineStartX = token.getX();
                            double characterWidth = token.width / token.getText().length();
                            if (!Double.isNaN(previousLineStartX)) {
                                // Indentation if line start is > 1 character width to the right of previous line start
                                if (lineStartX - previousLineStartX > characterWidth)
                                    indented = true;
                                    // Indentation ends if line start is > 1 character width to the left of previous line start
                                else if (previousLineStartX - lineStartX > characterWidth)
                                    indented = false;
                                // Otherwise indentation is unchanged
                            }
                        }
                    } else {
                        newline = false;
                    }
                    // centered ?

                    // final sanitisation and filtering for the token
                    text = text.replaceAll("[ \n]", "");
                    if (TextUtilities.filterLine(text)) {
                        n++;
                        continue;
                    }

                    features = new FeaturesVectorLeftNoteMedical();
                    features.token = token;
                    features.string = text;

                    if (newline)
                        features.lineStatus = "LINESTART";

                    Matcher m0 = featureFactory.isPunct.matcher(text);
                    if (m0.find()) {
                        features.punctType = "PUNCT";
                    }
                    if (text.equals("(") || text.equals("[")) {
                        features.punctType = "OPENBRACKET";

                    } else if (text.equals(")") || text.equals("]")) {
                        features.punctType = "ENDBRACKET";

                    } else if (text.equals(".")) {
                        features.punctType = "DOT";

                    } else if (text.equals(",")) {
                        features.punctType = "COMMA";

                    } else if (text.equals("-")) {
                        features.punctType = "HYPHEN";

                    } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
                        features.punctType = "QUOTE";
                    }

                    if (n == startIndex) {
                        // beginning of block
                        features.lineStatus = "LINESTART";
                        features.blockStatus = "BLOCKSTART";
                    } else if ((n == tokens.size() - 1) || (n + 1 > dp2.getTokenDocPos() - block.getStartToken())) {
                        // end of block
                        features.lineStatus = "LINEEND";
                        previousNewline = true;
                        features.blockStatus = "BLOCKEND";
                        endblock = true;
                    } else {
                        // look ahead to see if we are at the end of a line within the block
                        boolean endline = false;

                        int ii = 1;
                        boolean endloop = false;
                        while ((n + ii < tokens.size()) && (!endloop)) {
                            LayoutToken tok = tokens.get(n + ii);
                            if (tok != null) {
                                String toto = tok.getText();
                                if (toto != null) {
                                    if (toto.equals("\n") || text.equals("\r")) {
                                        endline = true;
                                        endloop = true;
                                    } else {
                                        if ((toto.trim().length() != 0)
                                            && (!text.equals("\u00A0"))
                                            && (!(toto.contains("@IMAGE")))
                                            && (!(toto.contains("@PAGE")))
                                            && (!text.contains(".pbm"))
                                            && (!text.contains(".ppm"))
                                            && (!text.contains(".png"))
                                            && (!text.contains(".svg"))
                                            && (!text.contains(".jpg"))) {
                                            endloop = true;
                                        }
                                    }
                                }
                            }

                            if (n + ii == tokens.size() - 1) {
                                endblock = true;
                                endline = true;
                            }

                            ii++;
                        }

                        if ((!endline) && !(newline)) {
                            features.lineStatus = "LINEIN";
                        } else if (!newline) {
                            features.lineStatus = "LINEEND";
                            previousNewline = true;
                        }

                        if ((!endblock) && (features.blockStatus == null))
                            features.blockStatus = "BLOCKIN";
                        else if (features.blockStatus == null)
                            features.blockStatus = "BLOCKEND";
                    }

                    if (indented) {
                        features.alignmentStatus = "LINEINDENT";
                    } else {
                        features.alignmentStatus = "ALIGNEDLEFT";
                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    // check token offsets for email and http address, or known location
                    if (locationPositions != null) {
                        for (OffsetPosition thePosition : locationPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.locationName = true;
                                break;
                            }
                        }
                    }
                    if (emailPositions != null) {
                        for (OffsetPosition thePosition : emailPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.email = true;
                                break;
                            }
                        }
                    }
                    if (urlPositions != null) {
                        for (OffsetPosition thePosition : urlPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.http = true;
                                break;
                            }
                        }
                    }

                    if (currentFont == null) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else if (!currentFont.equals(token.getFont())) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else
                        features.fontStatus = "SAMEFONT";

                    int newFontSize = (int) token.getFontSize();
                    if (currentFontSize == -1) {
                        currentFontSize = newFontSize;
                        features.fontSize = "HIGHERFONT";
                    } else if (currentFontSize == newFontSize) {
                        features.fontSize = "SAMEFONTSIZE";
                    } else if (currentFontSize < newFontSize) {
                        features.fontSize = "HIGHERFONT";
                        currentFontSize = newFontSize;
                    } else if (currentFontSize > newFontSize) {
                        features.fontSize = "LOWERFONT";
                        currentFontSize = newFontSize;
                    }

                    if (token.getFontSize() == largestFontSize)
                        features.largestFont = true;
                    if (token.getFontSize() == smallestFontSize)
                        features.smallestFont = true;
                    if (token.getFontSize() > averageFontSize)
                        features.largerThanAverageFont = true;

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    if (features.punctType == null)
                        features.punctType = "NOPUNCT";

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density - doc.getMinCharacterDensity(), doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(), NBBINS_DENSITY);
//System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    if (previousFeatures != null)
                        leftNote.append(previousFeatures.printVector());
                    previousFeatures = features;

                    n++;
                }

                if (previousFeatures != null) {
                    previousFeatures.blockStatus = "BLOCKEND";
                    previousFeatures.lineStatus = "LINEEND";
                    leftNote.append(previousFeatures.printVector());
                    previousFeatures = null;
                }
            }
        }

        return Pair.of(leftNote.toString(), leftNoteTokenizations);
    }

    /**
     * Extract results from a labelled left-note.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @param leftNoteItem  left-note item
     * @return left-note item
     */
    public LeftNoteMedicalItem resultExtraction(String result, List<LayoutToken> tokenizations, LeftNoteMedicalItem leftNoteItem) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        leftNoteItem.generalResultMapping(result, tokenizations);
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }
            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_IDNO)) {
                if (leftNoteItem.getIdno() != null) {
                    leftNoteItem.setIdno(leftNoteItem.getIdno() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setIdno(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_ORGANIZATION)) {
                if (leftNoteItem.getOrg() != null) {
                    leftNoteItem.setOrg(leftNoteItem.getOrg() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setOrg(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_ADDRESS)) {
                if (leftNoteItem.getAddress() != null) {
                    leftNoteItem.setAddress(leftNoteItem.getAddress() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setAddress(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_COUNTRY)) {
                if (leftNoteItem.getCountry() != null) {
                    leftNoteItem.setCountry(leftNoteItem.getCountry() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setCountry(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_TOWN)) {
                if (leftNoteItem.getSettlement() != null) {
                    leftNoteItem.setSettlement(leftNoteItem.getSettlement() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setSettlement(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_PHONE)) {
                if (leftNoteItem.getPhone() != null) {
                    leftNoteItem.setPhone(leftNoteItem.getPhone() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setPhone(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_FAX)) {
                if (leftNoteItem.getFax() != null) {
                    leftNoteItem.setFax(leftNoteItem.getFax() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setFax(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_EMAIL)) {
                if (leftNoteItem.getEmail() != null) {
                    leftNoteItem.setEmail(leftNoteItem.getEmail() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setEmail(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_WEB)) {
                if (leftNoteItem.getWeb() != null) {
                    leftNoteItem.setWeb(leftNoteItem.getWeb() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setWeb(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_MEDIC)) {
                if (leftNoteItem.getMedics() != null) {
                    leftNoteItem.setMedics(leftNoteItem.getMedics() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setMedics(clusterNonDehypenizedContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.LEFT_NOTE_NOTE)) {
                if (leftNoteItem.getNote() != null) {
                    leftNoteItem.setNote(leftNoteItem.getNote() + "\n" + clusterNonDehypenizedContent);
                } else {
                    leftNoteItem.setNote(clusterNonDehypenizedContent);
                }
            }
        }
        return leftNoteItem;
    }

    /**
     * In the context of field extraction, check if a newly extracted content is not redundant
     * with the already extracted content
     */
    private boolean isDifferentContent(String existingContent, String newContent) {
        if (existingContent == null) {
            return true;
        }
        if (newContent == null) {
            return false;
        }
        String newContentSimplified = newContent.toLowerCase();
        newContentSimplified = newContentSimplified.replace(" ", "").trim();
        String existinContentSimplified = existingContent.toLowerCase();
        existinContentSimplified = existinContentSimplified.replace(" ", "").trim();
        if (newContentSimplified.equals(existinContentSimplified))
            return false;
        else
            return true;
    }

    /**
     * In the context of field extraction, this variant of the previous method check if a newly
     * extracted content is not redundant globally and as any substring combination with the already
     * extracted content
     */
    private boolean isDifferentandNotIncludedContent(String existingContent, String newContent) {
        if (existingContent == null) {
            return true;
        }
        if (newContent == null) {
            return false;
        }
        String newContentSimplified = newContent.toLowerCase();
        newContentSimplified = newContentSimplified.replace(" ", "").trim();
        newContentSimplified = newContentSimplified.replace("-", "").trim();
        String existingContentSimplified = existingContent.toLowerCase();
        existingContentSimplified = existingContentSimplified.replace(" ", "").trim();
        existingContentSimplified = existingContentSimplified.replace("-", "").trim();
        if (newContentSimplified.equals(existingContentSimplified) ||
            existingContentSimplified.indexOf(newContentSimplified) != -1
        )
            return false;
        else
            return true;
    }

    private List<LayoutToken> getLayoutTokens(TaggingTokenCluster cluster) {
        List<LayoutToken> tokens = new ArrayList<>();

        for (LabeledTokensContainer container : cluster.getLabeledTokensContainers()) {
            tokens.addAll(container.getLayoutTokens());
        }

        return tokens;
    }

    /**
     * Extract results from a labelled left-note in the training format without any
     * string modification.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a result
     */
    public StringBuilder trainingExtraction(String result, List<LayoutToken> tokenizations) {
        // this is the main buffer for the whole left-note
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null; // the label
        String s2 = null; // the text
        String lastTag = null;

        int p = 0;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, "\t");
            // List<String> localFeatures = new ArrayList<String>();
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
                    s2 = TextUtilities.HTMLEncode(s); // the text
                    //s2 = s;

                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).t();
                        if (tokOriginal.equals(" ")
                            || tokOriginal.equals("\u00A0")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                } else if (i == ll - 1) {
                    s1 = s;
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                    // localFeatures.add(s);
                }
                i++;
            }

            /*if (newLine) {
                buffer.append("<lb/>");
            }*/

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            String currentTag0 = null;
            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            if (lastTag != null) {
                testClosingTag(buffer, currentTag0, lastTag0);
            }

            boolean output;

            output = writeField(buffer, s1, lastTag0, s2, "<idno>", "<idno>", addSpace);
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<org>", "<org>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<address>", "<address>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<phone>", "<phone>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<fax>", "<fax>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<email>", "<email>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<web>", "<ptr type=\"web\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<medic>", "<medic>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<note>", "<note type=\"short\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<other>", "", addSpace);
            }

            lastTag = s1;

            if (!st.hasMoreTokens() && (lastTag != null)) {
                testClosingTag(buffer, "", currentTag0);
            }
        }
        return buffer;
    }

    /**
     * Extract results from a labelled left-note in the training format without any
     * string modification.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a result
     */
    public StringBuilder trainingExtractionAnonym(String result, List<LayoutToken> tokenizations,
                                                  List<String> dataOriginal,
                                                  List<String> dataAnonymized) {
        // this is the main buffer for the whole left-note
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null; // the label
        String s2 = null; // the text
        String lastTag = null;

        int p = 0;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, "\t");
            // List<String> localFeatures = new ArrayList<String>();
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();

                // anonymize the token
                int idx = dataOriginal.indexOf(s);
                if (idx >= 0) {
                    s = dataAnonymized.get(idx);
                }

                if (i == 0) {
                    s2 = TextUtilities.HTMLEncode(s);
                    //s2 = s;

                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).t();
                        if (dataOriginal.indexOf(tokOriginal) >= 0) {
                            tokOriginal = dataAnonymized.get(idx);
                        }
                        if (tokOriginal.equals(" ")
                            || tokOriginal.equals("\u00A0")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                } else if (i == ll - 1) {
                    s1 = s;
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                    // localFeatures.add(s);
                }
                i++;
            }

            /*if (newLine) {
                buffer.append("<lb/>");
            }*/

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            String currentTag0 = null;
            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            if (lastTag != null) {
                testClosingTag(buffer, currentTag0, lastTag0);
            }

            boolean output;

            output = writeField(buffer, s1, lastTag0, s2, "<idno>", "<idno>", addSpace);
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<org>", "<org>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<address>", "<address>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<phone>", "<phone>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<fax>", "<fax>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<email>", "<email>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<web>", "<ptr type=\"web\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<medic>", "<medic>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<note>", "<note type=\"short\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<other>", "", addSpace);
            }

            lastTag = s1;

            if (!st.hasMoreTokens() && lastTag != null) {
                testClosingTag(buffer, "", currentTag0);
            }
        }
        return buffer;
    }

    private boolean writeField(StringBuilder buffer, String s1, String lastTag0, String s2, String field, String outField, boolean addSpace) {
        // s1 :  the label, s2 : the text
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (s1.equals(lastTag0) || (s1).equals("I-" + lastTag0)) { // if the tag is the same as the last one, we continue to add the text
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else
                buffer.append("\n\t").append(outField).append(s2); // otherwise, we first add a carriage return before adding the text
        }
        return result;
    }

    private void testClosingTag(StringBuilder buffer, String currentTag0, String lastTag0) {
        if (!currentTag0.equals(lastTag0)) {
            // we close the current tag
            if (lastTag0.equals("<idno>")) {
                buffer.append("</idno>\n");
            } else if (lastTag0.equals("<org>")) {
                buffer.append("</org>\n");
            } else if (lastTag0.equals("<address>")) {
                buffer.append("</address>\n");
            } else if (lastTag0.equals("<phone>")) {
                buffer.append("</phone>\n");
            } else if (lastTag0.equals("<fax>")) {
                buffer.append("</fax>\n");
            } else if (lastTag0.equals("<email>")) {
                buffer.append("</email>\n");
            } else if (lastTag0.equals("<web>")) {
                buffer.append("</ptr>\n");
            } else if (lastTag0.equals("<medic>")) {
                // replace with the result of the medic parser
                String newData = moreExtraction(buffer, lastTag0);
                // empty the last buffer and fill with the new data
                buffer.setLength(0);
                buffer.append(newData);
                buffer.append("</medic>\n");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>\n");
            }
        }
    }

    private String moreExtraction(StringBuilder buffer, String lastTag0) {
        String theBufferString = buffer.toString();

        // take only the string of the last tag (e.g. the last <medic>)
        int idxLastMedic = theBufferString.lastIndexOf(lastTag0);
        String strBeforeInput = theBufferString.substring(0, idxLastMedic + lastTag0.length());
        String input = theBufferString.substring(idxLastMedic + lastTag0.length());

        StringBuilder newBuffer = new StringBuilder();
        List<String> inputToBeExtract = new ArrayList<>();
        if (input != null && lastTag0.equals("<medic>")) {
            inputToBeExtract.add(input);
            newBuffer.append(strBeforeInput);
            // call the medic parser
            String resMedic = parsers.getMedicParser().trainingExtraction(inputToBeExtract).toString();
            if (resMedic != null) {
                newBuffer.append(resMedic);
            }
        }

        return newBuffer.toString();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    /**
     * Process all the PDF in a given directory with a left-note medical process and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     *
     * @param inputDirectory  - the path to the directory containing PDF to be processed.
     * @param outputDirectory - the path to the directory where the results as XML files
     *                        and CRF feature files shall be written.
     * @param ind             - identifier integer to be included in the resulting files to
     *                        identify the training case. This is optional: no identifier
     *                        will be included if ind = -1
     * @return the number of processed files.
     */

    public int createTrainingMedicalLeftNoteBatch(String inputDirectory,
                                                  String outputDirectory,
                                                  boolean blank,
                                                  int ind) throws IOException {
        try {
            File path = new File(inputDirectory);
            if (!path.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + inputDirectory);
            }

            File pathOut = new File(outputDirectory);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + outputDirectory);
            }

            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File file : refFiles) {
                try {
                    if (blank) {
                        // create blank training data for the left-note (organization) model
                        createBlankTrainingFromPDF(file, outputDirectory, n);
                    } else {
                        // create pre-annotated training data based on existing left-note (organization) model
                        createTrainingFromPDF(file, outputDirectory, n);
                    }
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running grobid-medical-report batch.", exp);
        }
    }

    /**
     * Process the specified pdf and format the result as training data for the left-note model.
     *
     * @param inputFile  input PDF file
     * @param pathOutput path to raw monograph featured sequence
     * @param pathOutput path to TEI
     * @param id         id
     */
    public Document createTrainingFromPDF(File inputFile,
                                          String pathOutput,
                                          int id) {
        DocumentSource documentSource = null;
        Document doc = null;
        GrobidAnalysisConfig config = null;
        LeftNoteMedicalItem resultLeftNote = null;
        try {
            resultLeftNote = new LeftNoteMedicalItem();
            config = GrobidAnalysisConfig.defaultInstance();

            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for left-note model, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.left.note.medical.tei.xml"));
            File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.left.note.medical"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            // segment first with medical report segmenter model
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            // retreive only the left-note part
            SortedSet<DocumentPiece> documentLeftNoteParts = doc.getDocumentPart(MedicalLabels.LEFTNOTE);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            if (documentLeftNoteParts != null) {
                // these commands are be replace by `List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight();`
                Pair<String, List<LayoutToken>> featuredLeftNote = getSectionLeftNoteFeatured(doc, documentLeftNoteParts);
                String leftNote = featuredLeftNote.getLeft();
                List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight();
                String rese = null;
                if ((leftNote != null) && (leftNote.trim().length() > 0)) {
                    // we write left-note data with features
                    Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(leftNote + "\n");
                    writer.close();

                    // we tag with the left-note model
                    rese = label(leftNote);

                    // buffer for the left-note block
                    StringBuilder bufferLeftNote = trainingExtraction(rese, leftNoteTokenization);
                    lang = LanguageUtilities.getInstance().runLanguageId(bufferLeftNote.toString());
                    if (lang != null) {
                        doc.setLanguage(lang.getLang());
                        //writer.write(" xml:lang=\"" + lang.getLang() + "\"");
                    }

                    // write the training TEI file for left-note which reflects the extract layout of the text as
                    // extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                        + pdfFileName.replace(".pdf", "")
                        + "\"/>\n\t</teiHeader>\n\t<text");

                    writer.write(" xml:lang=\"fr\"");
                    writer.write(">\n\t\t<listOrg>\n");

                    writer.write(bufferLeftNote.toString());
                    writer.write("\n\t\t</listOrg>\n\t</text>\n</tei>\n");
                    writer.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running grobid-medical-report training" +
                " data generation for left-note medical report.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Process the specified pdf and format the result as blank training data for the left-note model.
     *
     * @param inputFile  input PDF file
     * @param pathOutput path to raw monograph featured sequence
     * @param pathOutput path to TEI
     * @param id         id
     */

    public Document createBlankTrainingFromPDF(File inputFile,
                                               String pathOutput,
                                               int id) {
        DocumentSource documentSource = null;
        Document doc = null;
        GrobidAnalysisConfig config = null;
        try {
            config = GrobidAnalysisConfig.defaultInstance();
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot create blank training dataset for the left-note model, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.left.note.medical.blank.tei.xml"));
            File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.left.note.medical"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            // segment first with medical report segmenter model
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            // retreive only the left-note part
            SortedSet<DocumentPiece> documentLeftNoteParts = doc.getDocumentPart(MedicalLabels.LEFTNOTE);
            List<LayoutToken> tokenizations = doc.getTokenizations(); // tokens of the segmentation  data

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            if (documentLeftNoteParts != null) {
                // these commands are be replace by `List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight();`
                /*List<LayoutToken> tokenizationsLeftNote = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentLeftNoteParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsLeftNote.add(tokenizations.get(i));
                    }
                }*/

                Pair<String, List<LayoutToken>> featuredLeftNote = getSectionLeftNoteFeatured(doc, documentLeftNoteParts);
                String leftNote = featuredLeftNote.getLeft(); // left note data with features
                List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight(); // tokens of the left note data
                if ((leftNote != null) && (leftNote.trim().length() > 0)) {
                    // we write left-note data with features
                    Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(leftNote + "\n");
                    writer.close();

                    // write the training TEI file for left-note which reflects the extract layout of the text as
                    // extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                        + pdfFileName.replace(".pdf", "")
                        + "\"/>\n\t</teiHeader>\n\t<text");

                    writer.write(" xml:lang=\"fr\"");
                    writer.write(">\n\t\t<listOrg>\n");

                    StringBuilder bufferLeftNote = new StringBuilder();
                    for (LayoutToken token : leftNoteTokenization) {
                        bufferLeftNote.append(token.getText());
                    }

                    writer.write(bufferLeftNote.toString());
                    writer.write("\n\t\t</listOrg>\n\t</text>\n</tei>\n");
                    writer.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running grobid-medical blank training" +
                " data generation for the left-note medical report.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Processing Pdf files with the current models using new files located in a given directory.
     */

    public int processLeftNoteDirectory(String inputDirectory,
                                        String outputDirectory,
                                        int ind) throws IOException {
        try {
            File path = new File(inputDirectory);
            if (!path.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + inputDirectory);
            }

            File pathOut = new File(outputDirectory);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + outputDirectory);
            }

            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File file : refFiles) {
                try {
                    processingLeftNote(file, outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running grobid-medical-report batch.", exp);
        }
    }

    /**
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */

    public void processingLeftNote(File inputFile,
                                   String outputFile,
                                   int id) {
        DocumentSource documentSource = null;
        Document doc = null;
        GrobidAnalysisConfig config = null;

        try {
            config = GrobidAnalysisConfig.defaultInstance();

            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot process the model, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File outputTEIFile = new File(outputFile + File.separator + pdfFileName.replace(".pdf", ".left.note.medical.tei.xml"));
            Writer writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            // general segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            StringBuilder tei = new StringBuilder();

            tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

            tei.append("<tei xml:space=\"preserve\">\n\t<teiHeader"
                + " xml:lang=\"" + lang.getLang()
                + "\">\n\t\t<fileDesc xml:id=\""
                + pdfFileName.replace(".pdf", "")
                + "\"/>\n\n\t");

            // header processing
            tokenizations = doc.getTokenizations();
            tei.append("\n\t</teiHeader>");

            // the body part
            if (lang != null) {
                tei.append("\n\n\t<text xml:lang=\"" + lang.getLang() + "\">\n\t\t");
            }

            tei.append("\n\t<note place=\"leftnote\">");
            // left-note processing
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(MedicalLabels.LEFTNOTE);
            Pair<String, List<LayoutToken>> featuredLeftNote = null;
            if (documentParts != null) {
                List<LayoutToken> tokenizationsLeftNote = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsLeftNote.add(tokenizations.get(i));
                    }
                }

                featuredLeftNote = getSectionLeftNoteFeatured(doc, documentParts);
                String leftNote = featuredLeftNote.getLeft();
                List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight();
                String rese = null;

                if ((leftNote != null) && (leftNote.trim().length() > 0)) {
                    rese = label(leftNote);

                    // buffer for the left-note block
                    StringBuilder bufferLeftNote = trainingExtraction(rese, tokenizationsLeftNote);

                    // add the result
                    tei.append(bufferLeftNote);
                }
            }

            tei.append("\n\t</note>");
            tei.append("\n\t</text>");
            tei.append("\n</tei>\n");

            writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
            writer.write(tei.toString());
            writer.close();

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report training" +
                " data generation for medical model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }
}