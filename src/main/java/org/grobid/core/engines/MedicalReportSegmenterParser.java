package org.grobid.core.engines;

import eugfc.imageio.plugins.PNMRegistry;
import org.apache.commons.io.FileUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.document.BasicStructureBuilder;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidExceptionStatus;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorMedical;
import org.grobid.core.layout.*;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.counters.CntManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.*;

// for image conversion we're using an ImageIO plugin for PPM format support
// see https://github.com/eug/imageio-pnm
// the jar for this plugin is located in the local repository

/**
 * This class realises a high level segmentation of medical reports into document header, page header,
 * page footer, left note, right note, document body, page, and acknowledgment.
 * <p>
 * <p>
 * Taken and adapted from the Segmentation class of Grobid (@author Patrice Lopez)
 * <p>
 * Tanti, 2020
 */
public class MedicalReportSegmenterParser extends AbstractParser {

	/*
        11 labels for this model:
            cover page (<cover>): titlePage (optionally under front),
            document header (<header>): front,
            page header (<headnote>): note type headnote,
            page footer (<footnote>): note type footnote,
            left note (<leftnote>): note type left,
            right note (<rightnote>): note type right,
            document body (<body>): body,
            page number (<page>): page,
            annexes <annex>,
            acknowledgement (<acknowledgment>): acknowledgement,
            other (<other>): other
	*/

    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalReportSegmenterParser.class);

    private EngineMedicalParsers parsers;

    // default bins for relative position
    private static final int NBBINS_POSITION = 12;

    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;

    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;

    // projection scale for line length
    private static final int LINESCALE = 10;

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    private FeatureFactory featureFactory = FeatureFactory.getInstance();

    public MedicalReportSegmenterParser() {
        super(GrobidModels.MEDICAL_REPORT_SEGMENTER);
    }

    public MedicalReportSegmenterParser(EngineMedicalParsers parsers) {
        super(GrobidModels.MEDICAL_REPORT_SEGMENTER);
        this.parsers = parsers;
    }

    public MedicalReportSegmenterParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.MEDICAL_REPORT_SEGMENTER, cntManager);
        this.parsers = parsers;
    }

    /**
     * Segment a PDF document into high level zones: document header, page footer,
     * page header, left note, right note, body, page number, and acknowledgment.
     *
     * @param documentSource document source
     * @return Document object with segmented medical information
     */
    public Document processing(DocumentSource documentSource, GrobidAnalysisConfig config) {
        try {
            Document doc = new Document(documentSource);
            if (config.getAnalyzer() != null)
                doc.setAnalyzer(config.getAnalyzer());
            doc.addTokenizedDocument(config);
            doc = prepareDocument(doc);

            // if assets is true, the images are still there under directory pathXML+"_data"
            // we copy them to the assetPath directory

            File assetFile = config.getPdfAssetPath();
            if (assetFile != null) {
                dealWithImages(documentSource, doc, assetFile, config);
            }
            return doc;
        } finally {
            {
                // remove the pdfalto tmp files, including the sub-directories
                DocumentSource.close(documentSource, true, true, true);
            }
        }
    }

    public Document processing(String text) {
        Document doc = Document.createFromText(text);
        return prepareDocument(doc);
    }

    public Document prepareDocument(Document doc) {

        List<LayoutToken> tokenizations = doc.getTokenizations();
        if (tokenizations.size() > GrobidProperties.getPdfTokensMax()) {
            throw new GrobidException("The document has " + tokenizations.size() + " tokens, but the limit is " + GrobidProperties.getPdfTokensMax(),
                GrobidExceptionStatus.TOO_MANY_TOKENS);
        }

        doc.produceStatistics();
        String content = getAllLinesFeatured(doc);
        if (isNotEmpty(trim(content))) {
            String labelledResult = label(content);
            // set the different sections of the Document object
            doc = BasicStructureBuilder.generalResultSegmentation(doc, labelledResult, tokenizations);
        }
        return doc;
    }

    private void dealWithImages(DocumentSource documentSource, Document doc, File assetFile, GrobidAnalysisConfig config) {
        if (assetFile != null) {
            // copy the files under the directory pathXML+"_data" (the asset files) into the path specified by assetPath

            if (!assetFile.exists()) {
                // we create it
                if (assetFile.mkdir()) {
                    LOGGER.debug("Directory created: " + assetFile.getPath());
                } else {
                    LOGGER.error("Failed to create directory: " + assetFile.getPath());
                }
            }
            PNMRegistry.registerAllServicesProviders();

            // filter all .jpg and .png files
            File directoryPath = new File(documentSource.getXmlFile().getAbsolutePath() + "_data");
            if (directoryPath.exists()) {
                File[] files = directoryPath.listFiles();
                if (files != null) {
                    int nbFiles = 0;
                    for (final File currFile : files) {
                        if (nbFiles > DocumentSource.PDFALTO_FILES_AMOUNT_LIMIT)
                            break;

                        String toLowerCaseName = currFile.getName().toLowerCase();
                        if (toLowerCaseName.endsWith(".png") || !config.isPreprocessImages()) {
                            try {
                                if (toLowerCaseName.endsWith(".svg")) {
                                    continue;
                                }
                                FileUtils.copyFileToDirectory(currFile, assetFile);
                                nbFiles++;
                            } catch (IOException e) {
                                LOGGER.error("Cannot copy file " + currFile.getAbsolutePath() + " to " + assetFile.getAbsolutePath(), e);
                            }
                        } else if (toLowerCaseName.endsWith(".jpg")
                            || toLowerCaseName.endsWith(".ppm")
                        ) {

                            String outputFilePath = "";
                            try {
                                final BufferedImage bi = ImageIO.read(currFile);

                                if (toLowerCaseName.endsWith(".jpg")) {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                        toLowerCaseName.replace(".jpg", ".png");
                                } else {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                        toLowerCaseName.replace(".ppm", ".png");
                                }
                                ImageIO.write(bi, "png", new File(outputFilePath));
                                nbFiles++;
                            } catch (IOException e) {
                                LOGGER.error("Cannot convert file " + currFile.getAbsolutePath() + " to " + outputFilePath, e);
                            }
                        }
                    }
                }
            }
            // update the path of the image description stored in Document
            if (config.isPreprocessImages()) {
                List<GraphicObject> images = doc.getImages();
                if (images != null) {
                    String subPath = assetFile.getPath();
                    int ind = subPath.lastIndexOf("/");
                    if (ind != -1)
                        subPath = subPath.substring(ind + 1, subPath.length());
                    for (GraphicObject image : images) {
                        String fileImage = image.getFilePath();
                        if (fileImage == null) {
                            continue;
                        }
                        fileImage = fileImage.replace(".ppm", ".png")
                            .replace(".jpg", ".png");
                        ind = fileImage.indexOf("/");
                        image.setFilePath(subPath + fileImage.substring(ind, fileImage.length()));
                    }
                }
            }
        }
    }

    /**
     * Addition of the features at line level for the complete document.
     * <p/>
     * This is an alternative to the token level, where the unit for labeling is the line - so allowing faster
     * processing and involving less features.
     * Lexical features becomes line prefix and suffix, the feature text unit is the first 10 characters of the
     * line without space.
     * The dictionary flags are at line level (i.e. the line contains a name mention, a place mention, a year, etc.)
     * Regarding layout features: font, size and style are the one associated to the first token of the line.
     */
    public String getAllLinesFeatured(Document doc) {

        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        //guaranteeing quality of service. Otherwise, there are some PDF that may contain 300k blocks and thousands of extracted "images" that ruins the performance
        if (blocks.size() > GrobidProperties.getPdfBlocksMax()) {
            throw new GrobidException("Post-processed document is too big, contains: " + blocks.size(), GrobidExceptionStatus.TOO_MANY_BLOCKS);
        }

        //boolean graphicVector = false;
        //boolean graphicBitmap = false;

        // list of textual patterns at the head and foot of pages which can be re-occur on several pages
        // (typically indicating a publisher foot or head notes)
        Map<String, Integer> patterns = new TreeMap<String, Integer>();
        Map<String, Boolean> firstTimePattern = new TreeMap<String, Boolean>();

        for (Page page : doc.getPages()) {
            // we just look at the two first and last blocks of the page
            if ((page.getBlocks() != null) && (page.getBlocks().size() > 0)) {
                for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        Block block = page.getBlocks().get(blockIndex);
                        String localText = block.getText();
                        if ((localText != null) && (localText.length() > 0)) {
                            String[] lines = localText.split("[\\n\\r]");
                            if (lines.length > 0) {
                                String line = lines[0];
                                String pattern = featureFactory.getPattern(line);
                                if (pattern.length() > 8) {
                                    Integer nb = patterns.get(pattern);
                                    if (nb == null) {
                                        patterns.put(pattern, Integer.valueOf(1));
                                        firstTimePattern.put(pattern, false);
                                    } else
                                        patterns.put(pattern, Integer.valueOf(nb + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        String featuresAsString = getFeatureVectorsAsString(doc, patterns, firstTimePattern);

        return featuresAsString;
    }

    private String getFeatureVectorsAsString(Document doc, Map<String, Integer> patterns,
                                             Map<String, Boolean> firstTimePattern) {
        StringBuilder fulltext = new StringBuilder();
        int documentLength = doc.getDocumentLenghtChar();

        String currentFont = null;
        int currentFontSize = -1;

        boolean newPage;
        boolean start = true;
        int mm = 0; // page position
        int nn = 0; // document position
        int pageLength = 0; // length of the current page
        double pageHeight = 0.0;

        // vector for features
        FeaturesVectorMedical features;
        FeaturesVectorMedical previousFeatures = null;

        for (Page page : doc.getPages()) {
            pageHeight = page.getHeight();
            newPage = true;
            double spacingPreviousBlock = 0.0; // discretized
            double lowestPos = 0.0;
            pageLength = page.getPageLengthChar();
            BoundingBox pageBoundingBox = page.getMainArea();
            mm = 0;
            //endPage = true;

            if ((page.getBlocks() == null) || (page.getBlocks().size() == 0))
                continue;

            for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                Block block = page.getBlocks().get(blockIndex);
                boolean graphicVector = false;
                boolean graphicBitmap = false;

                boolean lastPageBlock = false;
                boolean firstPageBlock = false;
                if (blockIndex == page.getBlocks().size() - 1) {
                    lastPageBlock = true;
                }

                if (blockIndex == 0) {
                    firstPageBlock = true;
                }

                // check if we have a graphical object connected to the current block
                List<GraphicObject> localImages = Document.getConnectedGraphics(block, doc);
                if (localImages != null) {
                    for (GraphicObject localImage : localImages) {
                        if (localImage.getType() == GraphicObjectType.BITMAP)
                            graphicBitmap = true;
                        if (localImage.getType() == GraphicObjectType.VECTOR || localImage.getType() == GraphicObjectType.VECTOR_BOX)
                            graphicVector = true;
                    }
                }

                if (lowestPos > block.getY()) {
                    // we have a vertical shift, which can be due to a change of column or other particular layout formatting
                    spacingPreviousBlock = doc.getMaxBlockSpacing() / 5.0; // default
                } else
                    spacingPreviousBlock = block.getY() - lowestPos;

                String localText = block.getText();
                if (localText == null)
                    continue;

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                    (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                    (!block.getText().contains("@IMAGE")))
                    density = (double) block.getText().length() / (block.getHeight() * block.getWidth());

                // is the current block in the main area of the page or not?
                boolean inPageMainArea = true;
                BoundingBox blockBoundingBox = BoundingBox.fromPointAndDimensions(page.getNumber(),
                    block.getX(), block.getY(), block.getWidth(), block.getHeight());
                if (pageBoundingBox == null || (!pageBoundingBox.contains(blockBoundingBox) && !pageBoundingBox.intersect(blockBoundingBox)))
                    inPageMainArea = false;

                String[] lines = localText.split("[\\n\\r]");
                // set the max length of the lines in the block, in number of characters
                int maxLineLength = 0;
                for (int p = 0; p < lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }
                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }
                for (int li = 0; li < lines.length; li++) {
                    String line = lines[li];

                    // for the layout information of the block, we take simply the first layout token
                    LayoutToken token = null;
                    if (tokens.size() > 0)
                        token = tokens.get(0);

                    double coordinateLineY = token.getY();

                    features = new FeaturesVectorMedical();
                    features.token = token;
                    features.line = line;

                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        String pattern = featureFactory.getPattern(line);
                        Integer nb = patterns.get(pattern);
                        if ((nb != null) && (nb > 1)) {
                            features.repetitivePattern = true;

                            Boolean firstTimeDone = firstTimePattern.get(pattern);
                            if ((firstTimeDone != null) && !firstTimeDone) {
                                features.firstRepetitivePattern = true;
                                firstTimePattern.put(pattern, true);
                            }
                        }
                    }

                    // we consider the first token of the line as usual lexical CRF token
                    // and the second token of the line as feature
                    StringTokenizer st2 = new StringTokenizer(line, " \t\f\u00A0");
                    // alternatively, use a Grobid analyser
                    String text = null;
                    String text2 = null;
                    if (st2.hasMoreTokens())
                        text = st2.nextToken();
                    if (st2.hasMoreTokens())
                        text2 = st2.nextToken();

                    if (text == null)
                        continue;

                    // final sanitisation and filtering
                    text = text.replaceAll("[ \n\r]", "");
                    text = text.trim();

                    if ((text.length() == 0) ||
                        (TextUtilities.filterLine(line))) {
                        continue;
                    }

                    features.string = text;
                    features.secondString = text2;

                    features.firstPageBlock = firstPageBlock;
                    features.lastPageBlock = lastPageBlock;
                    //features.lineLength = line.length() / LINESCALE;
                    features.lineLength = featureFactory
                        .linearScaling(line.length(), maxLineLength, LINESCALE);

                    features.punctuationProfile = TextUtilities.punctuationProfile(line);

                    if (graphicBitmap) {
                        features.bitmapAround = true;
                    }
                    if (graphicVector) {
                        features.vectorAround = true;
                    }

                    features.lineStatus = null;
                    features.punctType = null;

                    if ((li == 0) ||
                        ((previousFeatures != null) && previousFeatures.blockStatus.equals("BLOCKEND"))) {
                        features.blockStatus = "BLOCKSTART";
                    } else if (li == lines.length - 1) {
                        features.blockStatus = "BLOCKEND";
                        //endblock = true;
                    } else if (features.blockStatus == null) {
                        features.blockStatus = "BLOCKIN";
                    }

                    if (newPage) {
                        features.pageStatus = "PAGESTART";
                        newPage = false;
                        //endPage = false;
                        if (previousFeatures != null)
                            previousFeatures.pageStatus = "PAGEEND";
                    } else {
                        features.pageStatus = "PAGEIN";
                        newPage = false;
                        //endPage = false;
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

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    Matcher m3 = featureFactory.email.matcher(text);
                    if (m3.find()) {
                        features.email = true;
                    }

                    Matcher m4 = featureFactory.http.matcher(text);
                    if (m4.find()) {
                        features.http = true;
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

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    // HERE horizontal information
                    // CENTERED
                    // LEFTAJUSTED
                    // CENTERED

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    /*if (features.punctType == null)
                        features.punctType = "NOPUNCT";
*/
                    features.relativeDocumentPosition = featureFactory
                        .linearScaling(nn, documentLength, NBBINS_POSITION);
//System.out.println(nn + " " + documentLength + " " + NBBINS_POSITION + " " + features.relativeDocumentPosition);
                    features.relativePagePositionChar = featureFactory
                        .linearScaling(mm, pageLength, NBBINS_POSITION);
//System.out.println(mm + " " + pageLength + " " + NBBINS_POSITION + " " + features.relativePagePositionChar);
                    int pagePos = featureFactory
                        .linearScaling(coordinateLineY, pageHeight, NBBINS_POSITION);
//System.out.println(coordinateLineY + " " + pageHeight + " " + NBBINS_POSITION + " " + pagePos);
                    if (pagePos > NBBINS_POSITION)
                        pagePos = NBBINS_POSITION;
                    features.relativePagePosition = pagePos;
//System.out.println(coordinateLineY + "\t" + pageHeight);

                    if (spacingPreviousBlock != 0.0) {
                        features.spacingWithPreviousBlock = featureFactory
                            .linearScaling(spacingPreviousBlock - doc.getMinBlockSpacing(), doc.getMaxBlockSpacing() - doc.getMinBlockSpacing(), NBBINS_SPACE);
                    }

                    features.inMainArea = inPageMainArea;

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density - doc.getMinCharacterDensity(), doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(), NBBINS_DENSITY);
//System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    if (previousFeatures != null) {
                        String vector = previousFeatures.printVector();
                        fulltext.append(vector);
                    }
                    previousFeatures = features;
                }

//System.out.println((spacingPreviousBlock-doc.getMinBlockSpacing()) + " " + (doc.getMaxBlockSpacing()-doc.getMinBlockSpacing()) + " " + NBBINS_SPACE + " "
//    + featureFactory.linearScaling(spacingPreviousBlock-doc.getMinBlockSpacing(), doc.getMaxBlockSpacing()-doc.getMinBlockSpacing(), NBBINS_SPACE));

                // lowest position of the block
                lowestPos = block.getY() + block.getHeight();

                // update page-level and document-level positions
                if (tokens != null) {
                    mm += tokens.size();
                    nn += tokens.size();
                }
            }
        }
        if (previousFeatures != null)
            fulltext.append(previousFeatures.printVector());

        return fulltext.toString();
    }

    /**
     * Addition of the features at line level for the complete document.
     * <p/>
     * This is an alternative to the token level, where the unit for labeling is the line - so allowing faster
     * processing and involving less features.
     * Lexical features becomes line prefix and suffix, the feature text unit is the first 10 characters of the
     * line without space.
     * The dictionary flags are at line level (i.e. the line contains a name mention, a place mention, a year, etc.)
     * Regarding layout features: font, size and style are the one associated to the first token of the line.
     */
    public String getAllLinesFeaturedAnonym(Document doc, List<String> dataOriginal, List<String> dataAnonymized) {

        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        //guaranteeing quality of service. Otherwise, there are some PDF that may contain 300k blocks and thousands of extracted "images" that ruins the performance
        if (blocks.size() > GrobidProperties.getPdfBlocksMax()) {
            throw new GrobidException("Post-processed document is too big, contains: " + blocks.size(), GrobidExceptionStatus.TOO_MANY_BLOCKS);
        }

        //boolean graphicVector = false;
        //boolean graphicBitmap = false;

        // list of textual patterns at the head and foot of pages which can be re-occur on several pages
        // (typically indicating a publisher foot or head-notes)
        Map<String, Integer> patterns = new TreeMap<String, Integer>();
        Map<String, Boolean> firstTimePattern = new TreeMap<String, Boolean>();

        for (Page page : doc.getPages()) {
            // we just look at the two first and last blocks of the page
            if ((page.getBlocks() != null) && (page.getBlocks().size() > 0)) {
                for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        Block block = page.getBlocks().get(blockIndex);
                        String localText = block.getText();
                        if ((localText != null) && (localText.length() > 0)) {
                            String[] lines = localText.split("[\\n\\r]");
                            if (lines.length > 0) {
                                String line = lines[0];
                                String pattern = featureFactory.getPattern(line);
                                if (pattern.length() > 8) {
                                    Integer nb = patterns.get(pattern);
                                    if (nb == null) {
                                        patterns.put(pattern, Integer.valueOf(1));
                                        firstTimePattern.put(pattern, false);
                                    } else
                                        patterns.put(pattern, Integer.valueOf(nb + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        String featuresAsString = getFeatureVectorsAnonymAsString(doc, patterns, firstTimePattern, dataOriginal, dataAnonymized);

        return featuresAsString;
    }

    private String getFeatureVectorsAnonymAsString(Document doc, Map<String, Integer> patterns,
                                                   Map<String, Boolean> firstTimePattern,
                                                   List<String> dataOriginal, List<String> dataAnonymized) {
        StringBuilder fulltext = new StringBuilder();
        int documentLength = doc.getDocumentLenghtChar();

        String currentFont = null;
        int currentFontSize = -1;

        boolean newPage;
        boolean start = true;
        int mm = 0; // page position
        int nn = 0; // document position
        int pageLength = 0; // length of the current page
        double pageHeight = 0.0;

        // vector for features
        FeaturesVectorMedical features;
        FeaturesVectorMedical previousFeatures = null;

        for (Page page : doc.getPages()) {
            pageHeight = page.getHeight();
            newPage = true;
            double spacingPreviousBlock = 0.0; // discretized
            double lowestPos = 0.0;
            pageLength = page.getPageLengthChar();
            BoundingBox pageBoundingBox = page.getMainArea();
            mm = 0;
            //endPage = true;

            if ((page.getBlocks() == null) || (page.getBlocks().size() == 0))
                continue;

            for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                Block block = page.getBlocks().get(blockIndex);
                boolean graphicVector = false;
                boolean graphicBitmap = false;

                boolean lastPageBlock = false;
                boolean firstPageBlock = false;
                if (blockIndex == page.getBlocks().size() - 1) {
                    lastPageBlock = true;
                }

                if (blockIndex == 0) {
                    firstPageBlock = true;
                }

                // check if we have a graphical object connected to the current block
                List<GraphicObject> localImages = Document.getConnectedGraphics(block, doc);
                if (localImages != null) {
                    for (GraphicObject localImage : localImages) {
                        if (localImage.getType() == GraphicObjectType.BITMAP)
                            graphicBitmap = true;
                        if (localImage.getType() == GraphicObjectType.VECTOR || localImage.getType() == GraphicObjectType.VECTOR_BOX)
                            graphicVector = true;
                    }
                }

                if (lowestPos > block.getY()) {
                    // we have a vertical shift, which can be due to a change of column or other particular layout formatting
                    spacingPreviousBlock = doc.getMaxBlockSpacing() / 5.0; // default
                } else
                    spacingPreviousBlock = block.getY() - lowestPos;

                String localText = block.getText();
                if (localText == null)
                    continue;

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                    (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                    (!block.getText().contains("@IMAGE")))
                    density = (double) block.getText().length() / (block.getHeight() * block.getWidth());

                // is the current block in the main area of the page or not?
                boolean inPageMainArea = true;
                BoundingBox blockBoundingBox = BoundingBox.fromPointAndDimensions(page.getNumber(),
                    block.getX(), block.getY(), block.getWidth(), block.getHeight());
                if (pageBoundingBox == null || (!pageBoundingBox.contains(blockBoundingBox) && !pageBoundingBox.intersect(blockBoundingBox)))
                    inPageMainArea = false;

                String[] lines = localText.split("[\\n\\r]");
                // set the max length of the lines in the block, in number of characters
                int maxLineLength = 0;
                for (int p = 0; p < lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }
                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }
                for (int li = 0; li < lines.length; li++) {
                    String line = lines[li];

                    // for the layout information of the block, we take simply the first layout token
                    LayoutToken token = null;
                    if (tokens.size() > 0)
                        token = tokens.get(0);

                    double coordinateLineY = token.getY();

                    features = new FeaturesVectorMedical();
                    features.token = token; // tokenization information
                    features.line = line;

                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        String pattern = featureFactory.getPattern(line);
                        Integer nb = patterns.get(pattern);
                        if ((nb != null) && (nb > 1)) {
                            features.repetitivePattern = true;

                            Boolean firstTimeDone = firstTimePattern.get(pattern);
                            if ((firstTimeDone != null) && !firstTimeDone) {
                                features.firstRepetitivePattern = true;
                                firstTimePattern.put(pattern, true);
                            }
                        }
                    }

                    // we consider the first token of the line as usual lexical CRF token
                    // and the second token of the line as feature
                    StringTokenizer st2 = new StringTokenizer(line, " \t\f\u00A0");
                    // alternatively, use a Grobid analyser
                    String text = null;
                    String text2 = null;
                    if (st2.hasMoreTokens())
                        text = st2.nextToken();
                    if (st2.hasMoreTokens())
                        text2 = st2.nextToken();

                    if (text == null)
                        continue;

                    // final sanitization and filtering
                    text = text.replaceAll("[ \n\r]", ""); // the text is tokenized by blank spaces
                    text = text.trim();

                    if ((text.length() == 0) ||
                        (TextUtilities.filterLine(line))) {
                        continue;
                    }

                    // anonymize the data
                    int idx = dataOriginal.indexOf(text);
                    if (idx >= 0) {
                        text = dataAnonymized.get(idx).trim();
                    }

                    int idx2 = dataOriginal.indexOf(text2);
                    if (idx2 >= 0) {
                        text2 = dataAnonymized.get(idx2).trim();
                    }

                    features.string = text;
                    features.secondString = text2;

                    features.firstPageBlock = firstPageBlock;
                    features.lastPageBlock = lastPageBlock;
                    //features.lineLength = line.length() / LINESCALE;
                    features.lineLength = featureFactory
                        .linearScaling(line.length(), maxLineLength, LINESCALE);

                    features.punctuationProfile = TextUtilities.punctuationProfile(line);

                    if (graphicBitmap) {
                        features.bitmapAround = true;
                    }
                    if (graphicVector) {
                        features.vectorAround = true;
                    }

                    features.lineStatus = null;
                    features.punctType = null;

                    if ((li == 0) ||
                        ((previousFeatures != null) && previousFeatures.blockStatus.equals("BLOCKEND"))) {
                        features.blockStatus = "BLOCKSTART";
                    } else if (li == lines.length - 1) {
                        features.blockStatus = "BLOCKEND";
                        //endblock = true;
                    } else if (features.blockStatus == null) {
                        features.blockStatus = "BLOCKIN";
                    }

                    if (newPage) {
                        features.pageStatus = "PAGESTART";
                        newPage = false;
                        //endPage = false;
                        if (previousFeatures != null)
                            previousFeatures.pageStatus = "PAGEEND";
                    } else {
                        features.pageStatus = "PAGEIN";
                        newPage = false;
                        //endPage = false;
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

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    Matcher m3 = featureFactory.email.matcher(text);
                    if (m3.find()) {
                        features.email = true;
                    }

                    Matcher m4 = featureFactory.http.matcher(text);
                    if (m4.find()) {
                        features.http = true;
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

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    // HERE horizontal information
                    // CENTERED
                    // LEFTAJUSTED
                    // CENTERED

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    /*if (features.punctType == null)
                        features.punctType = "NOPUNCT";*/

                    features.relativeDocumentPosition = featureFactory
                        .linearScaling(nn, documentLength, NBBINS_POSITION);
//System.out.println(nn + " " + documentLength + " " + NBBINS_POSITION + " " + features.relativeDocumentPosition);
                    features.relativePagePositionChar = featureFactory
                        .linearScaling(mm, pageLength, NBBINS_POSITION);
//System.out.println(mm + " " + pageLength + " " + NBBINS_POSITION + " " + features.relativePagePositionChar);
                    int pagePos = featureFactory
                        .linearScaling(coordinateLineY, pageHeight, NBBINS_POSITION);
//System.out.println(coordinateLineY + " " + pageHeight + " " + NBBINS_POSITION + " " + pagePos);
                    if (pagePos > NBBINS_POSITION)
                        pagePos = NBBINS_POSITION;
                    features.relativePagePosition = pagePos;
//System.out.println(coordinateLineY + "\t" + pageHeight);

                    if (spacingPreviousBlock != 0.0) {
                        features.spacingWithPreviousBlock = featureFactory
                            .linearScaling(spacingPreviousBlock - doc.getMinBlockSpacing(), doc.getMaxBlockSpacing() - doc.getMinBlockSpacing(), NBBINS_SPACE);
                    }

                    features.inMainArea = inPageMainArea;

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density - doc.getMinCharacterDensity(), doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(), NBBINS_DENSITY);
//System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    if (previousFeatures != null) {
                        String vector = previousFeatures.printVector();
                        fulltext.append(vector);
                    }
                    previousFeatures = features;
                }

//System.out.println((spacingPreviousBlock-doc.getMinBlockSpacing()) + " " + (doc.getMaxBlockSpacing()-doc.getMinBlockSpacing()) + " " + NBBINS_SPACE + " "
//    + featureFactory.linearScaling(spacingPreviousBlock-doc.getMinBlockSpacing(), doc.getMaxBlockSpacing()-doc.getMinBlockSpacing(), NBBINS_SPACE));

                // lowest position of the block
                lowestPos = block.getY() + block.getHeight();

                // update page-level and document-level positions
                if (tokens != null) {
                    mm += tokens.size();
                    nn += tokens.size();
                }
            }
        }
        if (previousFeatures != null)
            fulltext.append(previousFeatures.printVector());

        return fulltext.toString();
    }

    /**
     * Generate training data with the current model using new files located in a given directory.
     * the generated training data can then be corrected manually to be used for updating the
     * medical-report-segmenter CRF model.
     */

    public int createTrainingMedicalSegmentationBatch(String inputDirectory,
                                                      String outputDirectory,
                                                      String mode,
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

            // collect the anonymized data
            File[] refFilesAnonym = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".txt");
                }
            });

            // put the anonymized data in a list
            List<String> dataAnonym = new ArrayList<>();
            for (final File file : refFilesAnonym) {
                try (Stream<String> lines = Files.lines(Paths.get(file.getName()))) {
                    dataAnonym = lines.collect(Collectors.toList());
                }
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
                    if (mode.equals("training")) {
                        // create blank training data for the segmentation model 
                        createBlankTrainingFromPDF(file.getAbsolutePath(), outputDirectory, n);
                    }
                    if (mode.equals("blank")) {
                        // create pre-annotated training data based on existing medical segmentation model
                        createTrainingMedicalSegmentation(file.getAbsolutePath(), outputDirectory, n);
                    }
                    if (mode.equals("anonym")) {
                        // create pre-annotated anonymized training data based on existing medical segmentation model
                        createTrainingMedicalSegmentationAnonym(file.getAbsolutePath(), outputDirectory, dataAnonym, n);
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
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */
    public void createTrainingMedicalSegmentation(String inputFile,
                                                  String outputFile,
                                                  int id) {
        DocumentSource documentSource = null;
        try {
            File file = new File(inputFile);

            documentSource = DocumentSource.fromPdf(file, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            String PDFFileName = file.getName();
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = getAllLinesFeatured(doc); // contain the tokens with the features
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // we write the full text untagged (but containing features)
            String outPathFulltext = outputFile + File.separator +
                PDFFileName.replace(".pdf", ".training.medical");
            Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFulltext), false), "UTF-8");
            writer.write(fulltext + "\n");
            writer.close();

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(TextUtilities.HTMLEncode(txtline.getText()));
            }
            String outPathRawtext = outputFile + File.separator +
                PDFFileName.replace(".pdf", ".training.medical.rawtxt");
            FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), "UTF-8");

            if (isNotBlank(fulltext)) {
                String rese = label(fulltext); // we put labels on the tokens with their corresponding features
                StringBuffer bufferFulltext = trainingExtraction(rese, tokenizations, doc);

                // write the TEI file to reflect the extact layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(new File(outputFile +
                    File.separator +
                    PDFFileName.replace(".pdf", ".training.medical.tei.xml")), false), "UTF-8");
                writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + PDFFileName +
                    "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"fr\">\n");

                writer.write(bufferFulltext.toString());
                writer.write("\n\t</text>\n</tei>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report training" +
                " data generation for medical model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Get the content of the pdf and produce a blank training data TEI file, i.e. a text only TEI file
     * without any tags. This is usefull to start from scratch the creation of training data at the same
     * level as the segmentation parser.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */
    public void createBlankTrainingFromPDF(String inputFile, String outputFile, int id) {
        DocumentSource documentSource = null;
        try {
            File file = new File(inputFile);

            documentSource = DocumentSource.fromPdf(file, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            String PDFFileName = file.getName();
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = getAllLinesFeatured(doc); // contain the tokens with the features
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // we write the full text untagged (but containing features)
            String outPathFulltext = outputFile + File.separator +
                PDFFileName.replace(".pdf", ".training.medical");
            Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFulltext), false), "UTF-8");
            writer.write(fulltext + "\n");
            writer.close();

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(TextUtilities.HTMLEncode(txtline.getText()));
            }

            fulltext = rawtxt.toString();
            if (isNotBlank(fulltext)) {
                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(new File(outputFile +
                    File.separator +
                    PDFFileName.replace(".pdf", ".training.medical.blank.tei.xml")), false), "UTF-8");
                writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + id +
                    "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"en\">\n");

                writer.write(fulltext);
                writer.write("\n\t</text>\n</tei>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical blank training" +
                " data generation for the medical-report-segmenter model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile  input file
     * @param outputFile path to fulltext
     * @param id         id
     */
    public void createTrainingMedicalSegmentationAnonym(String inputFile,
                                                        String outputFile,
                                                        List<String> dataAnonym,
                                                        int id) {

        // treat the anonymized data first
        List<String> dataOriginal = new ArrayList<>();
        List<String> dataAnonymized = new ArrayList<>();

        for (int i = 0; i < dataAnonym.size(); i++) {
            List<String> dataAnonymSplit = Arrays.asList(dataAnonym.get(i).split("\t"));
            if (dataAnonymSplit.get(0) != null) {
                dataOriginal.add(dataAnonymSplit.get(0));
            } else {
                dataOriginal.add("");
            }
            if (dataAnonymSplit.get(1) != null) {
                dataAnonymized.add(dataAnonymSplit.get(1));
            } else {
                dataAnonymized.add("");
            }
        }

        DocumentSource documentSource = null;
        try {
            File file = new File(inputFile);
            documentSource = DocumentSource.fromPdf(file, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            String PDFFileName = file.getName();
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = getAllLinesFeaturedAnonym(doc, dataOriginal, dataAnonymized); // contain the anonymized tokens with the features
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // we write the full text untagged (but containing features)
            String outPathFulltext = outputFile + File.separator +
                PDFFileName.replace(".pdf", ".training.medical");
            Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFulltext), false), "UTF-8");
            writer.write(fulltext + "\n");
            writer.close();

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(TextUtilities.HTMLEncode(txtline.getText()));
            }
            String outPathRawtext = outputFile + File.separator +
                PDFFileName.replace(".pdf", ".training.medical.rawtxt");
            FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), "UTF-8");

            if (isNotBlank(fulltext)) {
                String rese = label(fulltext); // we put labels on the tokens with their corresponding features
                StringBuffer bufferFulltext = trainingExtraction(rese, tokenizations, doc);

                // write the TEI file to reflect the extact layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(new File(outputFile +
                    File.separator +
                    PDFFileName.replace(".pdf", ".training.medical.tei.xml")), false), "UTF-8");
                writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + PDFFileName +
                    "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"fr\">\n");

                writer.write(bufferFulltext.toString());
                writer.write("\n\t</text>\n</tei>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report training" +
                " data generation for medical model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Extract results from a labelled full text in the training format without any string modification.
     *
     * @param result        result
     * @param tokenizations tokens
     * @return extraction
     */
    public StringBuffer trainingExtraction(String result,
                                           List<LayoutToken> tokenizations,
                                           Document doc) {
        // this is the main buffer for the whole full text
        StringBuffer buffer = new StringBuffer();
        try {
            List<Block> blocks = doc.getBlocks();
            int currentBlockIndex = 0;
            int indexLine = 0;

            StringTokenizer st = new StringTokenizer(result, "\n");
            String s1 = null; // current label/tag
            String s2 = null; // current lexical token
            String s3 = null; // current second lexical token
            String lastTag = null;

            // current token position
            int p = 0;
            boolean start = true;

            while (st.hasMoreTokens()) {
                boolean addSpace = false;
                String tok = st.nextToken().trim();
                String line = null; // current line

                if (tok.length() == 0) {
                    continue;
                }
                StringTokenizer stt = new StringTokenizer(tok, " \t");
                List<String> localFeatures = new ArrayList<String>();
                int i = 0;

                boolean newLine = true;
                int ll = stt.countTokens();
                while (stt.hasMoreTokens()) {
                    String s = stt.nextToken().trim();

                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // lexical token
                    } else if (i == 1) {
                        s3 = TextUtilities.HTMLEncode(s); // second lexical token
                    } else if (i == ll - 1) {
                        s1 = s; // current label
                    } else {
                        localFeatures.add(s); // we keep the feature values in case they appear useful
                    }
                    i++;
                }

                // as we process the document segmentation line by line, we don't use the usual
                // tokenization to rebuild the text flow, but we get each line again from the
                // text stored in the document blocks (similarly as when generating the features)
                line = null;
                while ((line == null) && (currentBlockIndex < blocks.size())) {
                    Block block = blocks.get(currentBlockIndex);
                    List<LayoutToken> tokens = block.getTokens();
                    if (tokens == null) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    String localText = block.getText();
                    if ((localText == null) || (localText.trim().length() == 0)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    String[] lines = localText.split("[\\n\\r]");
                    if ((lines.length == 0) || (indexLine >= lines.length)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    } else {
                        line = lines[indexLine];
                        indexLine++;
                        if (line.trim().length() == 0) {
                            line = null;
                            continue;
                        }

                        if (TextUtilities.filterLine(line)) {
                            line = null;
                            continue;
                        }
                    }
                }

                line = TextUtilities.HTMLEncode(line);

                if (newLine && !start) {
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

                //boolean closeParagraph = false;
                if (lastTag != null) {
                    //closeParagraph =
                    testClosingTag(buffer, currentTag0, lastTag0, s1);
                }

                boolean output;

                output = writeField(buffer, line, s1, lastTag0, s2, "<cover>", "<titlePage>", addSpace, 3);

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<header>", "<front>", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<headnote>", "<note place=\"headnote\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<footnote>", "<note place=\"footnote\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<leftnote>", "<note place=\"left\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<rightnote>", "<note place=\"right\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<page>", "<page>", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<body>", "<body>", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<annex>", "<div type=\"annex\">", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<acknowledgement>", "<div type=\"acknowledgement\">", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<other>", "", addSpace, 3);
                }

                lastTag = s1;

                if (!st.hasMoreTokens()) {
                    if (lastTag != null) {
                        testClosingTag(buffer, "", currentTag0, s1);
                    }
                }
                if (start) {
                    start = false;
                }
            }
            return buffer;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
    }

    /**
     * Extract results from a labelled full text in the training format without any string modification.
     *
     * @param result        result
     * @param tokenizations tokens
     * @return extraction
     */
    public StringBuffer trainingExtractionAnonym(String result,
                                                 List<LayoutToken> tokenizations,
                                                 Document doc,
                                                 List<String> dataOriginal,
                                                 List<String> dataAnonymized) {
        // this is the main buffer for the whole full text
        StringBuffer buffer = new StringBuffer();
        try {
            List<Block> blocks = doc.getBlocks();
            int currentBlockIndex = 0;
            int indexLine = 0;

            StringTokenizer st = new StringTokenizer(result, "\n");
            String s1 = null; // current label/tag
            String s2 = null; // current lexical token
            String s3 = null; // current second lexical token
            String lastTag = null;

            // current token position
            int p = 0;
            boolean start = true;

            while (st.hasMoreTokens()) {
                boolean addSpace = false;
                String tok = st.nextToken().trim();
                String line = null; // current line

                if (tok.length() == 0) {
                    continue;
                }
                StringTokenizer stt = new StringTokenizer(tok, " \t");
                List<String> localFeatures = new ArrayList<String>();
                int i = 0;

                boolean newLine = true;
                int ll = stt.countTokens();
                while (stt.hasMoreTokens()) {
                    String s = stt.nextToken().trim();
                    // anonymize the token
                    int idx = dataOriginal.indexOf(s);
                    if (idx >= 0) {
                        s = dataAnonymized.get(idx);
                    }

                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // lexical token
                    } else if (i == 1) {
                        s3 = TextUtilities.HTMLEncode(s); // second lexical token
                    } else if (i == ll - 1) {
                        s1 = s; // current label
                    } else {
                        localFeatures.add(s); // we keep the feature values in case they appear useful
                    }
                    i++;
                }

                // as we process the document segmentation line by line, we don't use the usual
                // tokenization to rebuild the text flow, but we get each line again from the
                // text stored in the document blocks (similarly as when generating the features)
                line = null;
                while ((line == null) && (currentBlockIndex < blocks.size())) {
                    Block block = blocks.get(currentBlockIndex);
                    List<LayoutToken> tokens = block.getTokens();
                    if (tokens == null) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    String localText = block.getText();
                    if ((localText == null) || (localText.trim().length() == 0)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    String[] lines = localText.split("[\\n\\r]");
                    if ((lines.length == 0) || (indexLine >= lines.length)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    } else {
                        line = lines[indexLine];
                        indexLine++;
                        if (line.trim().length() == 0) {
                            line = null;
                            continue;
                        }

                        if (TextUtilities.filterLine(line)) {
                            line = null;
                            continue;
                        }
                    }
                }
                line = TextUtilities.HTMLEncode(line);
                List<String> splitLine = analyzer.tokenize(line);
                for(int j = 0; j < splitLine.size(); j++) {
                    int idx = dataOriginal.indexOf(splitLine.get(j));
                    if (idx >= 0) {
                        splitLine.set(j, dataAnonymized.get(idx));
                    }
                }
                line = String.join("", splitLine);
                line = TextUtilities.HTMLEncode(line); // encode the text to its HTML equivalent

                if (newLine && !start) {
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

                //boolean closeParagraph = false;
                if (lastTag != null) {
                    testClosingTag(buffer, currentTag0, lastTag0, s1);
                }

                boolean output;

                output = writeField(buffer, line, s1, lastTag0, s2, "<cover>", "<titlePage>", addSpace, 3);

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<header>", "<front>", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<headnote>", "<note place=\"headnote\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<footnote>", "<note place=\"footnote\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<leftnote>", "<note place=\"left\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<rightnote>", "<note place=\"right\">",
                        addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<page>", "<page>", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<body>", "<body>", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<annex>", "<div type=\"annex\">", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<acknowledgement>", "<div type=\"acknowledgement\">", addSpace, 3);
                }

                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<other>", "", addSpace, 3);
                }

                lastTag = s1;

                if (!st.hasMoreTokens()) {
                    if (lastTag != null) {
                        testClosingTag(buffer, "", currentTag0, s1);
                    }
                }
                if (start) {
                    start = false;
                }
            }
            return buffer;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
    }

    /**
     * @param buffer
     * @param s1
     * @param lastTag0
     * @param s2
     * @param field
     * @param outField
     * @param addSpace
     * @param nbIndent
     * @return
     */
    private boolean writeField(StringBuffer buffer,
                               String line,
                               String s1,
                               String lastTag0,
                               String s2,
                               String field,
                               String outField,
                               boolean addSpace,
                               int nbIndent) {
        boolean result = false;
        // filter the output path
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            line = line.replace("@BULLET", "\u2022");
            // if previous and current tag are the same, we output the token
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                buffer.append(line);
            } else if (lastTag0 == null) {
                // if previous tagname is null, we output the opening xml tag
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(line);
            } else {
                // new opening tag, we output the opening xml tag
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(line);
            }
        }
        return result;
    }

    /**
     * @param buffer
     * @param currentTag0
     * @param lastTag0
     * @param currentTag
     * @return
     */
    private boolean testClosingTag(StringBuffer buffer,
                                   String currentTag0,
                                   String lastTag0,
                                   String currentTag) {
        boolean res = false;
        // reference_marker and citation_marker are two exceptions because they can be embedded

        if (!currentTag0.equals(lastTag0)) {

            res = false;
            // we close the current tag
            if (lastTag0.equals("<cover>")) {
                buffer.append("</titlePage>\n\n");
                res = true;
            } else if (lastTag0.equals("<header>")) {
                buffer.append("</front>\n\n");
                res = true;
            } else if (lastTag0.equals("<body>")) {
                buffer.append("</body>\n\n");
                res = true;
            } else if (lastTag0.equals("<headnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<footnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<leftnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<rightnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<page>")) {
                buffer.append("</page>\n\n");
                res = true;
            } else if (lastTag0.equals("<annex>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<acknowledgement>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<other>")) {
                buffer.append("\n\n");
            } else {
                res = false;
            }

        }
        return res;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}
