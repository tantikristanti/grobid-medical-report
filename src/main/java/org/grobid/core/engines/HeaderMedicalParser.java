package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidMedicalReportModels;
import org.grobid.core.data.Date;
import org.grobid.core.data.HeaderMedicalItem;
import org.grobid.core.data.PersonMedical;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorHeaderMedical;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.counters.CntManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;

/**
 * A class for parsing header part of medical reports.
 * This class is adapted from the HeaderParser class of Grobid (@author Patrice Lopez)
 * <p>
 * Tanti, 2020
 */
public class HeaderMedicalParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMedicalParser.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();

    protected EngineMedicalParsers parsers;

    // default bins for relative position
    private static final int NBBINS_POSITION = 12;

    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;

    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;

    // projection scale for line length
    private static final int LINESCALE = 10;

    private Lexicon lexicon = Lexicon.getInstance();

    private File tmpPath = null;

    public HeaderMedicalParser(EngineMedicalParsers parsers) {
        super(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT);
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
        GrobidProperties.getInstance(new GrobidHomeFinder(Arrays.asList(MedicalReportProperties.get("grobid.home"))));
    }

    public HeaderMedicalParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, cntManager);
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
        // GrobidProperties.getInstance();
        GrobidProperties.getInstance(new GrobidHomeFinder(Arrays.asList(MedicalReportProperties.get("grobid.home"))));
    }

    /**
     * Processing with application of the medical-report segmentation model
     */
    public Pair<String, Document> processing(File input, HeaderMedicalItem resHeader, GrobidAnalysisConfig config) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(input, -1, -1, true, true, true);
            Document doc = parsers.getMedicalReportParser().processing(documentSource, config);

            String tei = processingHeaderSection(config, doc, resHeader, true);
            return new ImmutablePair<String, Document>(tei, doc);
        } finally {
            if (documentSource != null) {
                documentSource.close(true, true, true);
            }
        }
    }

    /**
     * Header processing after application of the medical-report segmentation model
     */
    public String processingHeaderSection(GrobidAnalysisConfig config, Document doc, HeaderMedicalItem resHeader, boolean serialize) {
        try {
            // retreive only the header (front) part
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(MedicalLabels.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            if (documentHeaderParts != null) {
                List<LayoutToken> tokenizationsHeader = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsHeader.add(tokenizations.get(i));
                    }
                }

                //String header = getSectionHeaderFeatured(doc, documentHeaderParts, true);
                Pair<String, List<LayoutToken>> featuredHeader = getSectionHeaderFeatured(doc, documentHeaderParts);
                String header = featuredHeader.getLeft();
                List<LayoutToken> headerTokenization = featuredHeader.getRight();
                String res = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    res = label(header);
                    resHeader = resultExtraction(res, headerTokenization, resHeader, doc);
                }

                // language identification
                StringBuilder contentSample = new StringBuilder();
                if (resHeader.getTitle() != null) {
                    contentSample.append(resHeader.getTitle());
                }

                if (contentSample.length() < 200) {
                    // we can exploit more textual content to ensure that the language identification will be
                    // correct
                    SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
                    if (documentBodyParts != null) {
                        StringBuilder contentBuffer = new StringBuilder();
                        for (DocumentPiece docPiece : documentBodyParts) {
                            DocumentPointer dp1 = docPiece.getLeft();
                            DocumentPointer dp2 = docPiece.getRight();

                            int tokens = dp1.getTokenDocPos();
                            int tokene = dp2.getTokenDocPos();
                            for (int i = tokens; i < tokene; i++) {
                                contentBuffer.append(tokenizations.get(i));
                                contentBuffer.append(" ");
                            }
                        }
                        contentSample.append(" ");
                        contentSample.append(contentBuffer.toString());
                    }
                }
                Language langu = languageUtilities.runLanguageId(contentSample.toString());
                if (langu != null) {
                    String lang = langu.getLang();
                    doc.setLanguage(lang);
                    resHeader.setLanguage(lang);
                } else {
                    resHeader.setLanguage("fr"); // by default it's French as medical reports are in French
                }

                if (resHeader != null) {

                    // document title
                    HeaderMedicalItem.cleanTitles(resHeader);
                    if (resHeader.getTitle() != null) {
                        String temp = TextUtilities.dehyphenize(resHeader.getTitle());
                        temp = temp.trim();
                        if (temp.length() > 1) {
                            if (temp.startsWith("1"))
                                temp = temp.substring(1, temp.length());
                            temp = temp.trim();
                        }
                        resHeader.setTitle(temp);
                    }

                    // medics
                    resHeader.setOriginalMedics(resHeader.getMedics());
                    resHeader.getMedicsTokens();

                    boolean fragmentedMedics = false;
                    boolean hasMarker = false;
                    List<Integer> medicsBlocks = new ArrayList<Integer>();
                    List<List<LayoutToken>> medicSegments = new ArrayList<>();
                    if (resHeader.getMedicsTokens() != null) {
                        // split the list of layout tokens when token "\t" is met
                        List<LayoutToken> currentSegment = new ArrayList<>();
                        for (LayoutToken theToken : resHeader.getMedicsTokens()) {
                            if (theToken.getText() != null && theToken.getText().equals("\t")) {
                                if (currentSegment.size() > 0)
                                    medicSegments.add(currentSegment);
                                currentSegment = new ArrayList<>();
                            } else
                                currentSegment.add(theToken);
                        }
                        // last segment
                        if (currentSegment.size() > 0)
                            medicSegments.add(currentSegment);

                        if (medicSegments.size() > 1) {
                            fragmentedMedics = true;
                        }
                        for (int k = 0; k < medicSegments.size(); k++) {
                            if (medicSegments.get(k).size() == 0)
                                continue;
                            List<PersonMedical> localMedics = parsers.getMedicParser()
                                .processingHeaderWithLayoutTokens(medicSegments.get(k), doc.getPDFAnnotations());
                            if (localMedics != null) {
                                for (PersonMedical pers : localMedics) {
                                    resHeader.addFullMedic(pers);
                                    if (pers.getMarkers() != null) {
                                        hasMarker = true;
                                    }
                                    medicsBlocks.add(k);
                                }
                            }
                        }
                    }

                    // remove invalid medics (no last name, noise, etc.)
                    resHeader.setFullMedics(PersonMedical.sanityCheck(resHeader.getFullMedics()));
                    // affiliation
                    resHeader.setFullAffiliations(
                        parsers.getAffiliationAddressParser().processReflow(res, tokenizations));
                    resHeader.attachEmails();
                    boolean attached = false;
                    if (fragmentedMedics && !hasMarker) {
                        if (resHeader.getFullAffiliations() != null) {
                            if (medicSegments != null) {
                                if (resHeader.getFullAffiliations().size() == medicSegments.size()) {
                                    int k = 0;
                                    List<PersonMedical> persons = resHeader.getFullMedics();
                                    for (PersonMedical pers : persons) {
                                        if (k < medicsBlocks.size()) {
                                            int indd = medicsBlocks.get(k);
                                            if (indd < resHeader.getFullAffiliations().size()) {
                                                pers.addAffiliation(resHeader.getFullAffiliations().get(indd));
                                            }
                                        }
                                        k++;
                                    }
                                    attached = true;
                                    resHeader.setFullAffiliations(null);
                                    resHeader.setAffiliation(null);
                                }
                            }
                        }
                    }
                    if (!attached) {
                        resHeader.attachAffiliations();
                    }

                    // remove duplicated medics
                    resHeader.setFullMedics(PersonMedical.deduplicate(resHeader.getFullMedics()));

                }

                resHeader = consolidateHeader(resHeader, config.getConsolidateHeader());

                // we don't need to serialize if we process the full text (it would be done 2 times)
                if (serialize) {
                    TEIFormatter teiFormatter = new TEIFormatter(doc, null);
                    StringBuilder tei = teiFormatter.toTEIHeader(resHeader, null, null, config);
                    tei.append("\t</text>\n");
                    tei.append("</TEI>\n");
                    return tei.toString();
                } else
                    return null;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return null;
    }


    /**
     * Return the header section with features to be processed by the sequence labelling model
     */
    public Pair<String, List<LayoutToken>> getSectionHeaderFeatured(Document doc,
                                                                    SortedSet<DocumentPiece> documentHeaderParts) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        StringBuilder header = new StringBuilder();
        String currentFont = null;
        int currentFontSize = -1;

        // vector for features
        FeaturesVectorHeaderMedical features;
        FeaturesVectorHeaderMedical previousFeatures = null;

        double lineStartX = Double.NaN;
        boolean indented = false;
        boolean centered = false;

        boolean endblock;
        //for (Integer blocknum : blockDocumentHeaders) {
        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        List<LayoutToken> headerTokenizations = new ArrayList<LayoutToken>();

        // find the largest, smallest and average size font on the header section
        double largestFontSize = 0.0;
        double smallestFontSize = 100000.0;
        double averageFontSize;
        double accumulatedFontSize = 0.0;
        int nbTokens = 0;
        for (DocumentPiece docPiece : documentHeaderParts) {
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

        // TBD: this would need to be made more efficient, by applying the regex only to a limited
        // part of the tokens
        /*List<LayoutToken> tokenizations = doc.getTokenizations();
        List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokenizations);
        List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokenizations);
        List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokenizations);*/

        for (DocumentPiece docPiece : documentHeaderParts) {
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

                /*for (int li = 0; li < lines.length; li++) {
                    String line = lines[li];

                    features.lineLength = featureFactory
                            .linearScaling(line.length(), maxLineLength, LINESCALE);

                    features.punctuationProfile = TextUtilities.punctuationProfile(line);
                }*/

                List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
                List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
                List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);

                /*for (OffsetPosition position : emailPositions) {
                    System.out.println(position.start + " " + position.end + " / " + tokens.get(position.start) + " ... " + tokens.get(position.end));
                }*/

                while (n < tokens.size()) {
                    if (blockIndex == dp2.getBlockPtr()) {
                        if (n > dp2.getTokenDocPos() - block.getStartToken()) {
                            break;
                        }
                    }

                    LayoutToken token = tokens.get(n);
                    headerTokenizations.add(token);

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
                        if (token != null && previousFeatures != null) {
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

                    features = new FeaturesVectorHeaderMedical();
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

                    /*if (spacingPreviousBlock != 0.0) {
                        features.spacingWithPreviousBlock = featureFactory
                            .linearScaling(spacingPreviousBlock-doc.getMinBlockSpacing(), doc.getMaxBlockSpacing()-doc.getMinBlockSpacing(), NBBINS_SPACE);
                    }*/

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density - doc.getMinCharacterDensity(), doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(), NBBINS_DENSITY);
//System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    if (previousFeatures != null)
                        header.append(previousFeatures.printVector());
                    previousFeatures = features;

                    n++;
                }

                if (previousFeatures != null) {
                    previousFeatures.blockStatus = "BLOCKEND";
                    previousFeatures.lineStatus = "LINEEND";
                    header.append(previousFeatures.printVector());
                    previousFeatures = null;
                }
            }


        }

        return Pair.of(header.toString(), headerTokenizations);
    }

    /**
     * Extract results from a labelled header.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @param medical       medical item
     * @return a medical item
     */
    public HeaderMedicalItem resultExtraction(String result, List<LayoutToken> tokenizations, HeaderMedicalItem medical, Document doc) {

        TaggingLabel lastClusterLabel = null;
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, result, tokenizations);

        String tokenLabel = null;
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }
            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.HEADER_DOCNUM)) {
                if (medical.getDocNum() != null && isDifferentandNotIncludedContent(medical.getDocNum(), clusterContent)) {
                    String currentDocNum = medical.getDocNum();
                    medical.setDocNum(clusterContent);
                    medical.checkIdentifier();
                    medical.setDocNum(currentDocNum);
                } else {
                    medical.setDocNum(clusterContent);
                    medical.checkIdentifier();
                }
            } else if (clusterLabel.equals(MedicalLabels.HEADER_DOCTYPE)) {
                if (medical.getDocumentType() != null) {
                    medical.setDocumentType(medical.getDocumentType() + clusterNonDehypenizedContent);
                } else
                    medical.setDocumentType(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_TITLE)) {
                if (medical.getTitle() == null) {
                    medical.setTitle(clusterContent);
                    List<LayoutToken> tokens = getLayoutTokens(cluster);
                    medical.addTitleTokens(tokens);
                }
            } else if (clusterLabel.equals(MedicalLabels.HEADER_DATE)) {
                if (medical.getDocumentDate() != null && medical.getDocumentDate().length() < clusterNonDehypenizedContent.length())
                    medical.setDocumentDate(clusterNonDehypenizedContent);
                else if (medical.getDocumentDate() == null)
                    medical.setDocumentDate(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_TIME)) {
                if (medical.getDocumentTime() != null && medical.getDocumentTime().length() < clusterNonDehypenizedContent.length())
                    medical.setDocumentTime(clusterNonDehypenizedContent);
                else if (medical.getDocumentDate() == null)
                    medical.setDocumentDate(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_DATELINE)) {
                if (medical.getDocumentDateLine() != null && medical.getDocumentDateLine().length() < clusterNonDehypenizedContent.length())
                    medical.setDocumentDateLine(clusterNonDehypenizedContent);
                else if (medical.getDocumentDateLine() == null)
                    medical.setDocumentDateLine(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_MEDIC)) {
                if (medical.getMedics() != null) {
                    medical.setMedics(medical.getMedics() + "\t" + clusterNonDehypenizedContent);
                    medical.addMedicsToken(new LayoutToken("\t", MedicalLabels.HEADER_MEDIC));

                    List<LayoutToken> tokens = cluster.concatTokens();
                    medical.addMedicsTokens(tokens);
                } else {
                    medical.setMedics(clusterNonDehypenizedContent);

                    List<LayoutToken> tokens = cluster.concatTokens();
                    medical.addMedicsTokens(tokens);
                }
            } else if (clusterLabel.equals(MedicalLabels.HEADER_PATIENT)) {
                if (medical.getPatients() != null) {
                    medical.setPatients(medical.getPatients() + "\t" + clusterNonDehypenizedContent);
                    medical.addPatientsToken(new LayoutToken("\t", MedicalLabels.HEADER_PATIENT));

                    List<LayoutToken> tokens = cluster.concatTokens();
                    medical.addPatientsTokens(tokens);
                } else {
                    medical.setPatients(clusterNonDehypenizedContent);

                    List<LayoutToken> tokens = cluster.concatTokens();
                    medical.addPatientsTokens(tokens);
                }
            } else if (clusterLabel.equals(MedicalLabels.HEADER_AFFILIATION)) {
                // affiliation **makers** should be marked SINGLECHAR LINESTART
                if (medical.getAffiliation() != null) {
                    medical.setAffiliation(medical.getAffiliation() + " ; " + clusterContent);
                } else
                    medical.setAffiliation(clusterContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_ADDRESS)) {
                if (medical.getAddress() != null) {
                    medical.setAddress(medical.getAddress() + " " + clusterContent);
                } else
                    medical.setAddress(clusterContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_ORG)) {
                if (medical.getOrg() != null) {
                    medical.setOrg(medical.getOrg() + " " + clusterContent);
                } else
                    medical.setOrg(clusterContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_EMAIL)) {
                if (medical.getEmail() != null) {
                    medical.setEmail(medical.getEmail() + "\t" + clusterNonDehypenizedContent);
                } else
                    medical.setEmail(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_PHONE)) {
                if (medical.getPhone() != null) {
                    medical.setPhone(medical.getPhone() + clusterNonDehypenizedContent);
                } else
                    medical.setPhone(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_FAX)) {
                if (medical.getFax() != null) {
                    medical.setWeb(medical.getFax() + clusterNonDehypenizedContent);
                } else
                    medical.setWeb(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_WEB)) {
                if (medical.getWeb() != null) {
                    medical.setWeb(medical.getWeb() + clusterNonDehypenizedContent);
                } else
                    medical.setWeb(clusterNonDehypenizedContent);
            }
        }
        return medical;
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
     * Extract results from a labelled header in the training format without any
     * string modification.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a result
     */
    public StringBuilder trainingExtraction(String result, List<LayoutToken> tokenizations) {
        // this is the main buffer for the whole header
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null;
        String s2 = null;
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
                    s2 = TextUtilities.HTMLEncode(s);
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

            if (newLine) {
                buffer.append("<lb/>");
            }

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

            output = writeField(buffer, s1, lastTag0, s2, "<docnum>", "<idno>", addSpace);
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<doctype>", "<note type=\"doctype\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<title>", "<docTitle>\n\t<titlePart>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<date>", "<date>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<time>", "<time>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<dateline>", "<dateline>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<medic>", "<person>\n\t<medic>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<patient>", "<person>\n\t<patient>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<affiliation>", "<byline>\n\t<affiliation>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<institution>", "<byline>\n\t<affiliation>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<address>", "<address>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<location>", "<address>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<email>", "<email>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<phone>", "<phone>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<fax>", "<fax>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<web>", "<ptr type=\"web\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<other>", "", addSpace);
            }

            lastTag = s1;

            if (!st.hasMoreTokens()) {
                if (lastTag != null) {
                    testClosingTag(buffer, "", currentTag0);
                }
            }
        }

        return buffer;
    }

    private void testClosingTag(StringBuilder buffer, String currentTag0, String lastTag0) {
        if (!currentTag0.equals(lastTag0)) {
            // we close the current tag
            if (lastTag0.equals("<docnum>")) {
                buffer.append("</idno>\n");
            } else if (lastTag0.equals("<doctype>")) {
                buffer.append("</note>\n");
            } else if (lastTag0.equals("<title>")) {
                buffer.append("</titlePart>\n\t</docTitle>\n");
            } else if (lastTag0.equals("<date>")) {
                buffer.append("</date>\n");
            } else if (lastTag0.equals("<time>")) {
                buffer.append("</time>\n");
            } else if (lastTag0.equals("<dateline>")) {
                buffer.append("</dateline>\n");
            } else if (lastTag0.equals("<medic>")) {
                buffer.append("</medic>\n\t</person>\n");
            } else if (lastTag0.equals("<patient>")) {
                buffer.append("</patient>\n\t</person>\n");
            } else if (lastTag0.equals("<affiliation>")) {
                buffer.append("</affiliation>\n\t</byline>\n");
            } else if (lastTag0.equals("<institution>")) {
                buffer.append("</affiliation>\n\t</byline>\n");
            } else if (lastTag0.equals("<address>")) {
                buffer.append("</address>\n");
            } else if (lastTag0.equals("<location>")) {
                buffer.append("</address>\n");
            } else if (lastTag0.equals("<email>")) {
                buffer.append("</email>\n");
            } else if (lastTag0.equals("<phone>")) {
                buffer.append("</phone>\n");
            } else if (lastTag0.equals("<fax>")) {
                buffer.append("</fax>\n");
            } else if (lastTag0.equals("<web>")) {
                buffer.append("</ptr>\n");
            }
        }
    }

    private boolean writeField(StringBuilder buffer, String s1, String lastTag0, String s2, String field, String outField, boolean addSpace) {
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (s1.equals(lastTag0) || (s1).equals("I-" + lastTag0)) {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else
                buffer.append("\n\t").append(outField).append(s2);
        }
        return result;
    }

    /**
     * Consolidate an existing list of recognized citations based on access to
     * external internet bibliographic databases.
     *
     * @param resHeader original biblio item
     * @return consolidated biblio item
     * <p>
     * I need to check this method
     */
    public HeaderMedicalItem consolidateHeader(HeaderMedicalItem resHeader, int consolidate) {
        if (consolidate == 0) {
            // there is no consolidation
            return resHeader;
        }
        return resHeader;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    /**
     * Process all the PDF in a given directory with a header medical process and
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

    public int createTrainingMedicalHeaderBatch(String inputDirectory,
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
                    //createTrainingFromPDF(file, outputDirectory, n);

                    // uncomment this command to create files containing features and blank training without any label
                    createBlankTrainingFromPDF(file, outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Process the specified pdf and format the result as training data for the header model.
     *
     * @param inputFile  input PDF file
     * @param pathOutput path to raw monograph featured sequence
     * @param pathOutput path to TEI
     * @param id         id
     */
    public Document createTrainingFromPDF(File inputFile,
                                          String pathOutput,
                                          int id) {
        if (tmpPath == null)
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        DocumentSource documentSource = null;
        Document doc = null;
        GrobidAnalysisConfig config = null;
        HeaderMedicalItem resultHeader = null;
        try {
            resultHeader = new HeaderMedicalItem();
            config = GrobidAnalysisConfig.defaultInstance();

            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for the header model, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.header.medical.tei.xml"));
            File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.header.medical"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            // segment first with medical report segmenter model
            doc = parsers.getMedicalReportParser().processing(documentSource, config);

            // retreive only the header (front) part
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(MedicalLabels.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            if (documentHeaderParts != null) {
                List<LayoutToken> tokenizationsHeader = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsHeader.add(tokenizations.get(i));
                    }
                }

                Pair<String, List<LayoutToken>> featuredHeader = getSectionHeaderFeatured(doc, documentHeaderParts);
                String header = featuredHeader.getLeft();
                List<LayoutToken> headerTokenization = featuredHeader.getRight();
                String rese = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    // we write the header untagged (but featurized)
                    Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(header + "\n");
                    writer.close();

                    rese = label(header);

                    // buffer for the header block
                    StringBuilder bufferHeader = trainingExtraction(rese, tokenizationsHeader);
                    //lang = LanguageUtilities.getInstance().runLanguageId(bufferHeader.toString());
                    if (lang != null) {
                        doc.setLanguage(lang.getLang());
                    }

                    // write the training TEI file for header which reflects the extract layout of the text as
                    // extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                        + pdfFileName.replace(".pdf", "")
                        + "\"/>\n\t</teiHeader>\n\t<text");

                    if (lang != null) {
                        writer.write(" xml:lang=\"" + lang.getLang() + "\"");
                    }
                    writer.write(">\n\t\t<front>\n");
                    writer.write(bufferHeader.toString());
                    writer.write("\n\t\t</front>\n\t</text>\n</tei>\n");
                    writer.close();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid training" +
                " data generation for header medical report.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Process the specified pdf and format the result as blank training data for the header model.
     *
     * @param inputFile  input PDF file
     * @param pathOutput path to raw monograph featured sequence
     * @param pathOutput path to TEI
     * @param id         id
     */

    public Document createBlankTrainingFromPDF(File inputFile,
                                               String pathOutput,
                                               int id) {
        if (tmpPath == null)
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        DocumentSource documentSource = null;
        Document doc = null;
        GrobidAnalysisConfig config = null;
        try {
            config = GrobidAnalysisConfig.defaultInstance();
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot create blank training dataset for the header model, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.header.medical.blank.tei.xml"));
            File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.header.medical"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            // segment first with medical report segmenter model
            doc = parsers.getMedicalReportParser().processing(documentSource, config);

            // retreive only the header part
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(MedicalLabels.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            if (documentHeaderParts != null) {
                List<LayoutToken> tokenizationsHeader = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsHeader.add(tokenizations.get(i));
                    }
                }

                Pair<String, List<LayoutToken>> featuredHeader = getSectionHeaderFeatured(doc, documentHeaderParts);
                String header = featuredHeader.getLeft();
                List<LayoutToken> headerTokenization = featuredHeader.getRight();
                if ((header != null) && (header.trim().length() > 0)) {
                    // we write the header untagged (but featurized)
                    Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(header + "\n");
                    writer.close();

                    // write the training TEI file for the header which reflects the extract layout of the text as
                    // extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                        + pdfFileName.replace(".pdf", "")
                        + "\"/>\n\t</teiHeader>\n\t<text");

                    if (lang != null) {
                        // by default, it's French language since the medical reports are in French
                        writer.write(" xml:lang=\"" + lang.getLang() + "\"");
                    }
                    writer.write(">\n\t\t<front>\n");
                    StringBuilder bufferHeader = new StringBuilder();

                    // just write the text without any label
                    for (LayoutToken token : tokenizationsHeader) {
                        bufferHeader.append(token.getText());
                    }

                    writer.write(bufferHeader.toString());
                    writer.write("\n\t\t</front>\n\t</text>\n</tei>\n");
                    writer.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occured while running grobid-medical blank training" +
                " data generation for the header-medical-report model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Processing Pdf files with the current models using new files located in a given directory.
     */

    public int processHighLevelBatch(String inputDirectory,
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
                    processingHighLevel(file, outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */

    public void processingHighLevel(File inputFile,
                                    String outputFile,
                                    int id) {
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }

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

            File outputTEIFile = new File(outputFile + File.separator + pdfFileName.replace(".pdf", ".header.high.level.medical.tei.xml"));
            Writer writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            // general segmentation
            doc = parsers.getMedicalReportParser().processing(documentSource, config);

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
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(MedicalLabels.HEADER);
            tokenizations = doc.getTokenizations();
            Pair<String, List<LayoutToken>> featuredHeader = null;

            if (documentParts != null) {
                List<LayoutToken> tokenizationsHeader = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsHeader.add(tokenizations.get(i));
                    }
                }

                featuredHeader = getSectionHeaderFeatured(doc, documentParts);
                String header = featuredHeader.getLeft();
                List<LayoutToken> headerTokenization = featuredHeader.getRight();
                String rese = null;

                if ((header != null) && (header.trim().length() > 0)) {
                    rese = label(header);

                    // buffer for the header block
                    StringBuilder bufferHeader = trainingExtraction(rese, headerTokenization);

                    // add the result
                    tei.append(bufferHeader);
                }

            }
            tei.append("\n\t</teiHeader>");

            // the body part
            if (lang != null) {
                tei.append("\n\n\t<text xml:lang=\"" + lang.getLang() + "\">\n\t\t");
            }

            tei.append("\n\t</text>");
            tei.append("\n</tei>\n");

            writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
            writer.write(tei.toString());
            writer.close();

        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid training" +
                " data generation for medical model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

    }


    /**
     * Processing Pdf files with the current models using new files located in a given directory.
     */

    public int processingHeaderUserBatch(String inputDirectory,
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
                    processingHeaderUser(file, outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */

    public void processingHeaderUser(File inputFile,
                                     String outputFile,
                                     int id) {
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }

        DocumentSource documentSource = null;
        Document doc = null;
        GrobidAnalysisConfig config = null;
        HeaderMedicalItem resHeader = null;

        try {
            config = GrobidAnalysisConfig.defaultInstance();

            resHeader = new HeaderMedicalItem();

            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot process the model, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File outputTEIFile = new File(outputFile + File.separator + pdfFileName.replace(".pdf", ".header.medical.tei.xml"));
            Writer writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);

            Pair<String, Document> resultTEI = processing(inputFile, resHeader, config);

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            StringBuilder tei = new StringBuilder();
            tei.append(resultTEI.getLeft());
            writer.write(tei.toString());
            writer.close();

        } catch (Exception e) {
            throw new GrobidException("An exception occured extracting header part of medical reports.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

    }

}
