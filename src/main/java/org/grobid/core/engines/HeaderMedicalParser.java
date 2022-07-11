package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Date;
import org.grobid.core.data.*;
import org.grobid.core.document.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorHeaderMedical;
import org.grobid.core.features.FeaturesVectorMedic;
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
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.document.TEIFormatter.toISOString;

/**
 * A class for parsing header part of medical reports.
 * This class is adapted from the HeaderParser class of Grobid (@author Patrice Lopez)
 * <p>
 * Tanti, 2020
 */
public class HeaderMedicalParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMedicalParser.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();

    private EngineMedicalParsers parsers;

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
        super(GrobidModels.HEADER_MEDICAL_REPORT);
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
        GrobidProperties.getInstance(new GrobidHomeFinder(Arrays.asList(MedicalReportProperties.get("grobid.home"))));
    }

    public HeaderMedicalParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.HEADER_MEDICAL_REPORT, cntManager);
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
        GrobidProperties.getInstance(new GrobidHomeFinder(Arrays.asList(MedicalReportProperties.get("grobid.home"))));
    }


    public Pair<String, Document> processing(File input, String md5Str,
                                             HeaderMedicalItem resHeader,
                                             LeftNoteMedicalItem resLeftNote,
                                             GrobidAnalysisConfig config) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(input, config.getStartPage(), config.getEndPage());
            documentSource.setMD5(md5Str);
            // first, parse the document with the segmentation model
            Document doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            // then take only the header and left note parts for further process with this method
            String tei = processingHeaderLeftNoteSection(config, doc, resHeader, resLeftNote,  true);
            return new ImmutablePair<String, Document>(tei, doc);
        } finally {
            if (documentSource != null) {
                documentSource.close(true, true, true);
            }
        }
    }

    /**
     * Header and left-note processing after application of the medical-report segmentation model
     */
    public String processingHeaderLeftNoteSection(GrobidAnalysisConfig config, Document doc,
                                                  HeaderMedicalItem resHeader,
                                                  LeftNoteMedicalItem resLeftNote,
                                                  boolean serialize) {
        try {
            // retrieve only the header (front) part
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(MedicalLabels.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations(); // tokenizations for the entire document
            if (documentHeaderParts != null || resLeftNote != null) {
                if (documentHeaderParts != null) {
                    Pair<String, List<LayoutToken>> featuredHeader = getSectionHeaderFeatured(doc, documentHeaderParts);
                    String header = featuredHeader.getLeft(); // header data with features
                    List<LayoutToken> headerTokenization = featuredHeader.getRight(); // tokenizations of header data
                    String res = null;
                    if (StringUtils.isNotBlank(header)) {
                        res = label(header);
                        resHeader = resultExtraction(res, headerTokenization, resHeader);

                        // take the results of the header parsing and complete the header items with additional information (ex.,language, doctype, etc)
                        if (resHeader != null) {

                            // language identification
                            StringBuilder contentSample = new StringBuilder();
                            if (resHeader.getDocumentType() != null) {
                                contentSample.append(resHeader.getDocumentType());
                            }

                            if (contentSample.length() < 200) {
                                // we can exploit more textual content to ensure that the language identification will be
                                // correct
                                SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
                                if (documentBodyParts != null) {
                                    String stringSample = Document.getTokenizationParts(documentBodyParts, tokenizations)
                                        .stream().map(LayoutToken::toString)
                                        .collect(Collectors.joining(" "));

                                    contentSample.append(stringSample);
                                }
                            }
                            Language langu = languageUtilities.runLanguageId(contentSample.toString());
                            if (langu != null) {
                                String lang = langu.getLang();
                                doc.setLanguage(lang);
                                resHeader.setLanguage(lang);
                            } else {
                                resHeader.setLanguage("fr"); // by default, the language is French
                            }

                            // number of pages
                            resHeader.setNbPages(doc.getPages().size());

                            // we normalize the document date and set to ISO standard date
                            if (resHeader.getDocumentDate() != null) {
                                Optional<Date> normalisedDate = getNormalizedDate(resHeader.getDocumentDate());
                                if (normalisedDate.isPresent()) {
                                    resHeader.setNormalizedDocumentDate(normalisedDate.get());
                                }
                                // to ISO standard
                                resHeader.setDocumentDate(toISOString(resHeader.getNormalizedDocumentDate()));
                            } else if (resHeader.getDateline() != null) { // if the date doesn't exist, we use the information from the dateline
                                List<LayoutToken> datelineLayoutTokens = resHeader.getDatelinesTokens();
                                List<List<LayoutToken>> datelineSegments = new ArrayList<>();
                                if (isNotEmpty(datelineLayoutTokens)) {
                                    List<LayoutToken> currentSegment = new ArrayList<>();
                                    for (LayoutToken theToken : datelineLayoutTokens) {
                                        // split the list of layout tokens when token "\t" is met
                                        if (theToken.getText() != null && theToken.getText().equals("\t")) {
                                            if (currentSegment.size() > 0)
                                                datelineSegments.add(currentSegment);
                                            currentSegment = new ArrayList<>();
                                        } else
                                            currentSegment.add(theToken);
                                    }
                                    // last segment
                                    if (currentSegment.size() > 0)
                                        datelineSegments.add(currentSegment);

                                    for (int k = 0; k < datelineSegments.size(); k++) {
                                        if (datelineSegments.get(k).size() == 0)
                                            continue;

                                        // further datelines processing with the Dateline model
                                        List<Dateline> localDatelines = parsers.getDatelineParser()
                                            .processingWithLayoutTokens(datelineSegments.get(k));
                                        for (Dateline dateline : localDatelines) {
                                            // fill the header items
                                            if (dateline.getDoctype() != null) {
                                                resHeader.setDocumentType(dateline.getDoctype());
                                                // normalization of the date
                                                if (dateline.getDate() != null) {
                                                    Optional<Date> normalisedDate = getNormalizedDate(dateline.getDate());
                                                    if (normalisedDate.isPresent()) {
                                                        dateline.setDate(toISOString(normalisedDate.get()));
                                                    }
                                                }
                                            } else if (dateline.getPlaceName() != null) {
                                                if (resHeader.getLocation() == null) {
                                                    resHeader.setLocation(dateline.getPlaceName());
                                                    // normalization of the date
                                                    if (dateline.getDate() != null) {
                                                        Optional<Date> normalisedDate = getNormalizedDate(dateline.getDate());
                                                        if (normalisedDate.isPresent()) {
                                                            dateline.setDate(toISOString(normalisedDate.get()));
                                                        }
                                                    }
                                                }
                                            } else if (dateline.getDate() != null) {
                                                // normalization of the date
                                                Optional<Date> normalisedDate = getNormalizedDate(dateline.getDate());
                                                if (normalisedDate.isPresent()) {
                                                    dateline.setDate(toISOString(normalisedDate.get()));
                                                }
                                            }

                                            // add new dateline to the list of datelines of header item
                                            resHeader.addDateline(dateline);
                                        }
                                    }
                                }
                            }

                            // medics processing
                            if (resHeader.getMedics() != null) {
                                List<LayoutToken> medicLayoutTokens = resHeader.getMedicsTokens();
                                List<List<LayoutToken>> medicSegments = new ArrayList<>();
                                if (isNotEmpty(medicLayoutTokens)) {
                                    List<LayoutToken> currentSegment = new ArrayList<>();
                                    for (LayoutToken theToken : medicLayoutTokens) {
                                        // split the list of layout tokens when token "\t" is met
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

                                    for (int k = 0; k < medicSegments.size(); k++) {
                                        if (medicSegments.get(k).size() == 0)
                                            continue;
                                        // further medics processing with the Medic model
                                        List<Medic> localMedics = parsers.getMedicParser()
                                            .processingWithLayoutTokens(medicSegments.get(k));

                                        for (Medic medic : localMedics) {
                                            // TBD : remove duplicate names and information related to it
                                            //see how it works wit PersonMedical.deduplicate()
                                            // add new medic to the medics item
                                            resHeader.addMedic(medic);
                                        }
                                    }
                                }
                            }

                            // patients
                            if (resHeader.getPatients() != null) {
                                List<LayoutToken> patientLayoutTokens = resHeader.getPatientsTokens();
                                List<List<LayoutToken>> patientSegments = new ArrayList<>();
                                if (isNotEmpty(patientLayoutTokens)) {
                                    List<LayoutToken> currentSegment = new ArrayList<>();
                                    for (LayoutToken theToken : patientLayoutTokens) {
                                        // split the list of layout tokens when token "\t" is met
                                        if (theToken.getText() != null && theToken.getText().equals("\t")) {
                                            if (currentSegment.size() > 0)
                                                patientSegments.add(currentSegment);
                                            currentSegment = new ArrayList<>();
                                        } else
                                            currentSegment.add(theToken);
                                    }
                                    // last segment
                                    if (currentSegment.size() > 0)
                                        patientSegments.add(currentSegment);

                                    for (int k = 0; k < patientSegments.size(); k++) {
                                        if (patientSegments.get(k).size() == 0)
                                            continue;

                                        // further patient processing with the Patient model
                                        List<Patient> localPatients = parsers.getPatientParser()
                                            .processingWithLayoutTokens(patientSegments.get(k));

                                        for (Patient patient : localPatients) {
                                            // normalization of all birth and death dates of patients

                                            if (patient.getDateBirth() != null) {

                                                Optional<Date> normalisedDate = getNormalizedDate(patient.getDateBirth());
                                                if (normalisedDate.isPresent()) {
                                                    patient.setDateBirth(toISOString(normalisedDate.get())); // yyyy-mm-dd
                                                }

                                            }
                                            if (patient.getDateDeath() != null) {
                                                Optional<Date> normalisedDate = getNormalizedDate(patient.getDateDeath());
                                                if (normalisedDate.isPresent()) {
                                                    patient.setDateDeath(toISOString(normalisedDate.get()));
                                                }
                                            }
                                            // TBD : remove duplicate names and information related to it
                                            //see how it works wit PersonMedical.deduplicate()
                                            // add new medic to the medics item
                                            resHeader.addPatient(patient);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // the left-note information part of medical reports if they exist
                SortedSet<DocumentPiece> documentLeftNoteParts = doc.getDocumentPart(MedicalLabels.LEFTNOTE);

                if (documentLeftNoteParts != null) {
                    Pair<String, List<LayoutToken>> featuredLeftNote = parsers.getLeftNoteMedicalParser().getSectionLeftNoteFeatured(doc, documentLeftNoteParts);
                    String leftNote = featuredLeftNote.getLeft(); // left note data with features
                    List<LayoutToken> leftNoteTokenization = featuredLeftNote.getRight(); // tokenizations of left note data

                    String labeledLeftNote = null;
                    if ((leftNote != null) && (leftNote.trim().length() > 0)) {
                        labeledLeftNote = parsers.getLeftNoteMedicalParser().label(leftNote);
                        resLeftNote = parsers.getLeftNoteMedicalParser().resultExtraction(labeledLeftNote, leftNoteTokenization, resLeftNote);
                        resLeftNote.setRawLeftNote(parsers.getLeftNoteMedicalParser().trainingExtraction(labeledLeftNote, leftNoteTokenization).toString());

                        if (resLeftNote != null) {
                            // buffer for the medics block
                            StringBuilder bufferMedic = null;
                            // we need to rebuild the found string as it appears
                            String input = "";
                            int q = 0;
                            StringTokenizer st = new StringTokenizer(labeledLeftNote, "\n");
                            while (st.hasMoreTokens() && (q < leftNoteTokenization.size())) {
                                String line = st.nextToken();
                                String theTotalTok = leftNoteTokenization.get(q).getText();
                                String theTok = leftNoteTokenization.get(q).getText();
                                while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                                    q++;
                                    if ((q > 0) && (q < leftNoteTokenization.size())) {
                                        theTok = leftNoteTokenization.get(q).getText();
                                        theTotalTok += theTok;
                                    }
                                }
                                if (line.endsWith("<medic>")) {
                                    input += theTotalTok;
                                }
                                q++;
                            }

                            List<String> inputs = new ArrayList<String>();
                            if (input != null && input.trim().length() > 0) {
                                inputs.add(input.trim());
                                bufferMedic = parsers.getMedicParser().trainingExtraction(inputs); //if the models exists already

                                // force analyser with English, to avoid bad surprise
                                List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
                                List<String> tokenizationMedic = analyzer.tokenize(input);
                                List<String> medicBlocks = new ArrayList<String>();
                                if (tokenizationMedic.size() == 0)
                                    return null;
                                for (String tok : tokenizationMedic) {
                                    if (tok.equals("\n")) {
                                        medicBlocks.add("@newline");
                                    } else if (!tok.equals(" ")) {
                                        medicBlocks.add(tok + " <medic>");
                                    }
                                }

                                List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
                                List<OffsetPosition> titlePositions = lexicon.tokenPositionsPersonTitle(tokens);
                                List<OffsetPosition> suffixPositions = lexicon.tokenPositionsPersonSuffix(tokens);
                                List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
                                List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);
                                // we write the medic data with features
                                String featuredMedic = FeaturesVectorMedic.addFeaturesMedic(tokens, null,
                                    locationPositions, titlePositions, suffixPositions, emailPositions, urlPositions);
                            }

                            // medics processing
                            if (resLeftNote.getMedics() != null) {
                                List<LayoutToken> medicLayoutTokens = resLeftNote.getMedicsLayoutTokens();
                                List<List<LayoutToken>> medicSegments = new ArrayList<>();
                                if (isNotEmpty(medicLayoutTokens)) {
                                    List<LayoutToken> currentSegment = new ArrayList<>();
                                    for (LayoutToken theToken : medicLayoutTokens) {
                                        // split the list of layout tokens when token "\n" is met
                                        if (theToken.getText() != null && theToken.getText().equals("\n")) {
                                            if (currentSegment.size() > 0)
                                                medicSegments.add(currentSegment);
                                            currentSegment = new ArrayList<>();
                                        } else
                                            currentSegment.add(theToken);
                                    }
                                    // last segment
                                    if (currentSegment.size() > 0)
                                        medicSegments.add(currentSegment);

                                    for (int k = 0; k < medicSegments.size(); k++) {
                                        if (medicSegments.get(k).size() == 0)
                                            continue;
                                        // further medics processing with the Medic model
                                        List<Medic> localMedics = parsers.getMedicParser()
                                            .processingWithLayoutTokens(medicSegments.get(k));

                                        for (Medic medic : localMedics) {
                                            // add new medic
                                            resLeftNote.addMedic(medic);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (serialize) { // need to set the serialize into false for the full text processing for preventing the double process
                    TEIFormatter teiFormatter = new TEIFormatter(doc, null);
                    StringBuilder tei = teiFormatter.toTEIHeaderLeftNote(resHeader, resLeftNote, null, config);
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
     * Return the date, normalised using the DateParser
     */
    private Optional<Date> getNormalizedDate(String rawDate) {
        if (rawDate != null) {
            List<Date> dates = parsers.getDateParser().process(rawDate);
            if (isNotEmpty(dates)) {
                return Optional.of(dates.get(0));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
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
        // note: only  largest font size information is used currently
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

                    // not used
                    /*if (token.isSuperscript())
                        features.superscript = true;*/

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
     * Return the header section with features to be processed by the sequence labelling model
     */
    public Pair<String, List<LayoutToken>> getSectionHeaderFeaturedAnonym(Document doc,
                                                                    SortedSet<DocumentPiece> documentHeaderParts,
                                                                          List<String> dataOriginal,
                                                                          List<String> dataAnonymized) {
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
        // note: only  largest font size information is used currently
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

                    features = new FeaturesVectorHeaderMedical();

                    for (int i=0; i<dataOriginal.size(); i++) {
                         text = text.replace(dataOriginal.get(i), dataAnonymized.get(i));
                    }

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

                    // not used
                    /*if (token.isSuperscript())
                        features.superscript = true;*/

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
    public HeaderMedicalItem resultExtraction(String result, List<LayoutToken> tokenizations, HeaderMedicalItem medical) {

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.HEADER_MEDICAL_REPORT, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        medical.generalResultMapping(result, tokenizations);

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }
            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.HEADER_DOCNUM)) {
                if (medical.getDocNum() != null && isDifferentContent(medical.getDocNum(), clusterContent)) {
                    medical.setDocNum(clusterContent);
                } else {
                    medical.setDocNum(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.HEADER_DOCTYPE)) {
                if (medical.getDocumentType() != null && isDifferentContent(medical.getDocumentType(), clusterContent)) {
                    medical.setDocumentType(medical.getDocumentType() + " \n " + clusterContent);
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
                if (medical.getDocumentTime() != null)
                    medical.setDocumentTime(clusterNonDehypenizedContent);
                else if (medical.getDocumentDate() == null)
                    medical.setDocumentDate(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_DATELINE)) {
                if (medical.getDateline() != null) {
                    medical.setDateline(medical.getDateline() + "\t" + clusterNonDehypenizedContent);
                    medical.addDatelinesToken(new LayoutToken("\t", MedicalLabels.HEADER_DATELINE));

                    List<LayoutToken> tokens = cluster.concatTokens();
                    medical.addDatelinesTokens(tokens);
                } else {
                    medical.setDateline(clusterNonDehypenizedContent);

                    List<LayoutToken> tokens = cluster.concatTokens();
                    medical.addDatelinesTokens(tokens);
                }
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
                    medical.setFax(medical.getFax() + clusterNonDehypenizedContent);
                } else
                    medical.setFax(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_WEB)) {
                if (medical.getWeb() != null) {
                    medical.setWeb(medical.getWeb() + clusterNonDehypenizedContent);
                } else
                    medical.setWeb(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.HEADER_NOTE)) {
                if (medical.getNote() != null) {
                    medical.setNote(medical.getNote() + " " + clusterContent);
                } else
                    medical.setNote(clusterContent);
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
                output = writeField(buffer, s1, lastTag0, s2, "<org>", "<org>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<note>", "<note type=\"short\">", addSpace);
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

    /**
     * Extract results from a labelled header in the training format without any
     * string modification.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a result
     */
    public StringBuilder trainingExtractionAnonym(String result, List<LayoutToken> tokenizations,
                                                  List<String> dataOriginal, List<String> dataAnonymized) {
        // this is the main buffer for the whole header
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null; // the label
        String s2 = null; // the token
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
                    for (int j=0; j<dataOriginal.size(); j++) {
                        s = s.replace(dataOriginal.get(j), dataAnonymized.get(j));
                    }
                    s2 = TextUtilities.HTMLEncode(s);
                    //s2 = s;

                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).t();
                        for (int j=0; j<dataOriginal.size(); j++) {
                            tokOriginal = tokOriginal.replace(dataOriginal.get(j), dataAnonymized.get(j));
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
                output = writeField(buffer, s1, lastTag0, s2, "<org>", "<org>", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<note>", "<note type=\"short\">", addSpace);
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
            } else if (lastTag0.equals("<org>")) {
                buffer.append("</org>\n");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>\n");
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
                        // create blank training data for the header model
                        createBlankTrainingFromPDF(file, outputDirectory, n);
                    } else {
                        // create pre-annotated training data based on existing header model
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
            throw new GrobidException("An exception occurred while running Grobid batch.", exp);
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
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            // retreive only the header (front) part
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(MedicalLabels.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            if (documentHeaderParts != null) {

                Pair<String, List<LayoutToken>> featuredHeader = getSectionHeaderFeatured(doc, documentHeaderParts);
                String header = featuredHeader.getLeft();
                List<LayoutToken> headerTokenization = featuredHeader.getRight();
                String res = null;
                if (StringUtils.isNotBlank(header)) {
                    // we write the header untagged (but featurized)
                    Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(header + "\n");
                    writer.close();

                    res = label(header);

                    // buffer for the header block
                    StringBuilder bufferHeader = trainingExtraction(res, headerTokenization);
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
            throw new GrobidException("An exception occurred while running Grobid training" +
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
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            // retreive only the header part
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(MedicalLabels.HEADER);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            if (documentHeaderParts != null) {
                Pair<String, List<LayoutToken>> featuredHeader = getSectionHeaderFeatured(doc, documentHeaderParts);
                String header = featuredHeader.getLeft();
                List<LayoutToken> headerTokenization = featuredHeader.getRight();
                if ((header != null) && (header.trim().length() > 0)) {
                    // we write the header untagged (but featurized)
                    Writer writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(header + "\n");
                    writer.close();

                    // write the unlabeled training TEI file for the header
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

                    for (LayoutToken token : headerTokenization) {
                        bufferHeader.append(token.getText());
                    }

                    writer.write(bufferHeader.toString());
                    writer.write("\n\t\t</front>\n\t</text>\n</tei>\n");
                    writer.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while running grobid-medical blank training" +
                " data generation for the header-medical-report model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Processing Pdf files with the current models using new files located in a given directory.
     */
    public int processHeaderDirectory(String inputDirectory,
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
                    processingHeaderLeftNote(file, outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running Grobid batch.", exp);
        }
    }

    /**
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */
    public void processingHeaderLeftNote(File inputFile,
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
        LeftNoteMedicalItem resLeftNote = null;

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

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, config);

            String resultTEI = processingHeaderLeftNoteSection(config, doc, resHeader, resLeftNote, true);

            // TBD: language identifier here on content text sample
            Language lang = new Language("fr");

            StringBuilder tei = new StringBuilder();
            tei.append(resultTEI);
            writer.close();

        } catch (Exception e) {
            throw new GrobidException("An exception occurred extracting header part of medical reports.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }
}
