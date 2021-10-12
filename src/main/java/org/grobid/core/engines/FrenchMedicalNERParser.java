package org.grobid.core.engines;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Entity;
import org.grobid.core.data.MedicalEntity;
import org.grobid.core.data.Sentence;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentPointer;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeaturesVectorNER;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.lexicon.LexiconPositionsIndexes;
import org.grobid.core.lexicon.MedicalNERLexicon;
import org.grobid.core.lexicon.MedicalNERLexiconPositionsIndexes;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * NER for French medical reports
 * <p>
 * Tanti, 2021
 */


public class FrenchMedicalNERParser extends AbstractParser {

    private static Logger LOGGER = LoggerFactory.getLogger(FrenchMedicalNERParser.class);
    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    protected EngineMedicalParsers parsers;

    private MedicalNERLexicon medicalNERLexicon = MedicalNERLexicon.getInstance();
    private Lexicon lexicon = Lexicon.getInstance();

    private final GenericTagger frMedicalNer;

    private File tmpPath = null;

    public FrenchMedicalNERParser(EngineMedicalParsers parsers) {
        super(GrobidModels.FULL_MEDICAL_TEXT);
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
        GrobidProperties.getInstance(new GrobidHomeFinder(Arrays.asList(MedicalReportProperties.get("grobid.home"))));
        frMedicalNer = TaggerFactory.getTagger(GrobidModels.FR_MEDICAL_NER_QUAERO, GrobidCRFEngine.DELFT);
    }


    /**
     * Extract all occurrences of named entity from a simple piece of text.
     * The positions of the recognized entities are given as character offsets
     * (following Java specification of characters).
     */
    public List<MedicalEntity> extractNE(String text) {
        List<LayoutToken> tokens = null;
        try {
            // for the analyser is English to avoid any bad surprises
            //tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language(Language.EN, 1.0));
            tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language(Language.FR, 1.0));
        } catch(Exception e) {
            LOGGER.error("Tokenization failed", e);
        }

        return extractNE(tokens);

    }

    /**
     * Extract all occurrences of named entities from a list of LayoutToken
     * coming from a document with fixed/preserved layout, e.g. PDF.
     * The positions of the recognized entities are given with coordinates in
     * the input document.
     */
    public List<MedicalEntity> extractNE(List<LayoutToken> tokens) {
        if (tokens == null)
            return null;

        MedicalNERLexiconPositionsIndexes positionsIndexes = new MedicalNERLexiconPositionsIndexes(medicalNERLexicon);
        positionsIndexes.computeIndexes(tokens);

        String res = toFeatureVectorLayout(tokens, positionsIndexes);
        String result = label(res);
        //List<Pair<String, String>> labeled = GenericTaggerUtils.getTokensAndLabels(result);

        //String text = LayoutTokensUtil.toText(tokens);
        List<MedicalEntity> entities = resultExtraction(GrobidModels.FR_MEDICAL_NER_QUAERO, result, tokens);

        return entities;
    }

    /**
     * Extract the named entities from a labelled sequence of LayoutToken.
     * This version use the new Clusteror class.
     */
    public List<MedicalEntity> resultExtraction(GrobidModels model, String result, List<LayoutToken> tokenizations) {

        // convert to usual Grobid label scheme to use TaggingTokenClusteror
        result = result.replace("\tB-", "\tI-");

        List<MedicalEntity> entities = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(model, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        MedicalEntity currentEntity = null;
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            if (clusterLabel.getLabel().equals("O"))
                continue;

            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
            currentEntity = new MedicalEntity();
            currentEntity.setRawName(clusterContent);
            currentEntity.setTypeFromString(GenericTaggerUtils.getPlainIOBLabel(clusterLabel.getLabel()));
            currentEntity.setBoundingBoxes(BoundingBoxCalculator.calculate(cluster.concatTokens()));
            currentEntity.setOffsets(calculateOffsets(cluster));
            currentEntity.setLayoutTokens(cluster.concatTokens());
            entities.add(currentEntity);
        }

        return entities;
    }

    private OffsetPosition calculateOffsets(TaggingTokenCluster cluster) {
        final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();
        if (CollectionUtils.isEmpty(labeledTokensContainers) || CollectionUtils.isEmpty(labeledTokensContainers.get(0).getLayoutTokens())) {
            return new OffsetPosition();
        }

        final LabeledTokensContainer labeledTokensContainer = labeledTokensContainers.get(0);
        final List<LayoutToken> layoutTokens = labeledTokensContainer.getLayoutTokens();

        int start = layoutTokens.get(0).getOffset();
        int end = start + LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens())).length();

        return new OffsetPosition(start, end);
    }

    public int extractFrenchMedicalNerBatch(String inputDirectory,
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
                    extractFrenchMedicalNer(file, outputDirectory, n);

                    // uncomment this command to create files containing features and blank training without any label
                    //createBlankTrainingFromPDF(file, outputDirectory, n);
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
     * Process the specified pdf and format the result as training data for all the models.
     *
     * @param inputFile  input file
     * @param pathOutput path for result
     * @param id         id
     */
    public Document extractFrenchMedicalNer(File inputFile,
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
        StringBuilder result = new StringBuilder();
        String lang = Language.FR; // by default, it's French
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for full-medical-text, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();
            Writer writer = null;

            // path for blank full-medical-text model
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner.tei.xml"));
            //File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            List<LayoutToken> tokenizations = doc.getTokenizations();

            // first, call the medical-report-segmenter model to have high level segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            // The BODY part after calling the segmentation model
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            if (documentBodyParts != null) {
                List<LayoutToken> tokenizationsBody = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentBodyParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsBody.add(tokenizations.get(i));
                    }
                }

                StringBuilder bufferBody = new StringBuilder();

                // just write the text without any label
                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                if (id == -1) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\"" + " xml:lang=\"" + lang + "\">\n");
                } else {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\"" + " xml:lang=\"" + lang + "\">\n");
                }
                // this is only for building the model-0
                //createTrainingFromTextGrobidNer(bufferBody.toString(), result, lang);
                writer.write(result + "\n");
                writer.write("\n\t\t</document>\n");
                writer.write("\t</subcorpus>\n</corpus>\n");
                writer.close();
                //}
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    // create training data for model-0 where the tagger comes from grobid-ner
    public StringBuilder createTrainingFromTextGrobidNer(String text, StringBuilder sb, String lang) throws IOException {
        if (isEmpty(text))
            return null;

        // let's segment in paragraphs, assuming we have one per paragraph per line
        String[] paragraphs = text.split("\n");
        for (int p = 0; p < paragraphs.length; p++) {

            String theText = paragraphs[p];
            if (theText.trim().length() == 0)
                continue;

            sb.append("\t\t\t<p " + "xml:id=\"P" + p + "\">\n");

            // we process NER at paragraph level (as it is trained at this level and because
            // inter sentence features/template are used by the CFR)
            //List<Entity> entities = parsers.getNerParser().extractNE(theText); // The English grobid-ner parser
            List<Entity> entities = parsers.getNerFrParser().extractNE(theText); // The French grobid-ner parser
            //int currentEntityIndex = 0;

            // ClearParser components for sentence segmentation
            // slow down a bit at launch, but it is used only for generating more readable training
            // let's segment in sentences with ClearNLP (to be updated to the newest NLP4J !)
            // the ClearNLP library is only available in English
            List<Sentence> sentences = sentenceSegmentation(theText, "en");
            int sentenceIndex = 0;
            for (int s = 0; s < sentences.size(); s++) {
                Sentence sentence = sentences.get(s);
                int sentenceStart = sentence.getOffsetStart();
                int sentenceEnd = sentence.getOffsetEnd();

                sb.append("\t\t\t\t<sentence xml:id=\"P" + p + "E" + sentenceIndex + "\">");

                if ((entities == null) || (entities.size() == 0)) {
                    // don't forget to encode the text for XML
                    sb.append(TextUtilities.HTMLEncode(theText.substring(sentenceStart, sentenceEnd)));
                } else {
                    int index = sentenceStart;
                    // smal adjustement to avoid sentence starting with a space
                    if (theText.charAt(index) == ' ')
                        index++;
                    for (Entity entity : entities) {
                        if (entity.getOffsetEnd() < sentenceStart)
                            continue;
                        if (entity.getOffsetStart() >= sentenceEnd)
                            break;

                        int entityStart = entity.getOffsetStart();
                        int entityEnd = entity.getOffsetEnd();

                        // don't forget to encode the text for XML
                        if (index < entityStart)
                            sb.append(TextUtilities.HTMLEncode(theText.substring(index, entityStart)));
                        sb.append("<ENAMEX type=\"" + entity.getType().getName() + "\">");
                        sb.append(TextUtilities.HTMLEncode(theText.substring(entityStart, entityEnd)));
                        sb.append("</ENAMEX>");

                        index = entityEnd;

                        while (index > sentenceEnd) {
                            // bad luck, the sentence segmentation or ner failed somehow and we have an
                            // entity across 2 sentences, so we merge on the fly these 2 sentences, which is
                            // easier than it looks ;)
                            s++;
                            if (s >= sentences.size())
                                break;
                            sentence = sentences.get(s);
                            sentenceStart = sentence.getOffsetStart();
                            sentenceEnd = sentence.getOffsetEnd();
                        }
                    }

                    if (index < sentenceEnd)
                        sb.append(TextUtilities.HTMLEncode(theText.substring(index, sentenceEnd)));
                    //else if (index > sentenceEnd)
                    //System.out.println(theText.length() + " / / " + theText + "/ / " + index + " / / " + sentenceEnd);
                }

                sb.append("</sentence>\n");
                sentenceIndex++;
            }
            sb.append("\t\t\t</p>\n");
        }
        return sb;
    }

    public static List<Sentence> sentenceSegmentation(String text, String language) throws FileNotFoundException {
        // this is only outputed for readability
        String dictionaryFile = "data/clearNLP/dictionary-1.3.1.zip";
        File tmpDir = new File("data/clearNLP/dictionary-1.3.1.zip");
        boolean exists = tmpDir.exists();
        AbstractTokenizer tokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile));
        AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
        // convert String into InputStream
        InputStream is = new ByteArrayInputStream(text.getBytes());
        // read it with BufferedReader
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<List<String>> sentences = segmenter.getSentences(br);
        List<Sentence> results = new ArrayList<Sentence>();

        if ((sentences == null) || (sentences.size() == 0)) {
            // there is some text but not in a state so that a sentence at least can be
            // identified by the sentence segmenter, so we parse it as a single sentence
            Sentence sentence = new Sentence();
            OffsetPosition pos = new OffsetPosition();
            pos.start = 0;
            pos.end = text.length();
            sentence.setOffsets(pos);
            results.add(sentence);
            return results;
        }

        // we need to realign with the original sentences, so we have to match it from the text
        // to be parsed based on the tokenization
        int offSetSentence = 0;
        //List<List<String>> trueSentences = new ArrayList<List<String>>();
        for (List<String> theSentence : sentences) {
            int next = offSetSentence;
            for (String token : theSentence) {
                next = text.indexOf(token, next);
                next = next + token.length();
            }

            Sentence sentence = new Sentence();
            OffsetPosition pos = new OffsetPosition();
            pos.start = offSetSentence;
            pos.end = next;
            sentence.setOffsets(pos);
            results.add(sentence);
            offSetSentence = next;
        }
        return results;
    }

    public int createTrainingFrenchMedicalNerBatch(String inputDirectory,
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
                    createTraining(file, outputDirectory, n);

                    // uncomment this command to create files containing features and blank training without any label
                    //createBlankTrainingFromPDF(file, outputDirectory, n);
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
     * Process the specified pdf and format the result as training data for all the models.
     *
     * @param inputFile  input file
     * @param pathOutput path for result
     * @param id         id
     */
    public Document createTraining(File inputFile,
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
        StringBuilder result = new StringBuilder();
        String lang = Language.FR; // by default, it's French
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for full-medical-text, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();
            Writer writer = null;

            // path for blank full-medical-text model
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner.tei.xml"));
            //File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            List<LayoutToken> tokenizations = doc.getTokenizations();

            // first, call the medical-report-segmenter model to have high level segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            // The BODY part after calling the segmentation model
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            if (documentBodyParts != null) {
                //Pair<String, LayoutTokenization> featSeg = getTextFeatured(doc, documentBodyParts);
                //if (featSeg != null) {
                //String bodytext = featSeg.getLeft();
                //List<LayoutToken> tokenizationsBody = featSeg.getRight().getTokenization();

                // we write the full text untagged
                    /*writer = new OutputStreamWriter(new FileOutputStream(outputRawFile, false), StandardCharsets.UTF_8);
                    writer.write(bodytext + "\n");
                    writer.close();*/

                List<LayoutToken> tokenizationsBody = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentBodyParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsBody.add(tokenizations.get(i));
                    }
                }

                StringBuilder bufferBody = new StringBuilder();

                // just write the text without any label
                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                if (id == -1) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\"" + " xml:lang=\"" + lang + "\">\n");
                } else {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\"" + " xml:lang=\"" + lang + "\">\n");
                }
                // this is only for building the model-0
                createTrainingFromTextGrobidNer(bufferBody.toString(), result, lang);
                writer.write(result + "\n");
                writer.write("\n\t\t</document>\n");
                writer.write("\t</subcorpus>\n</corpus>\n");
                writer.close();
                //}
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
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
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for full-medical-text, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();
            Writer writer = null;

            // path for blank full-medical-text model
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner.tei.xml"));
            //File outputRawFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = parsers.getMedicalReportSegmenterParser().getAllLinesFeatured(doc);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // first, call the medical-report-segmenter model to have high level segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            // The BODY part after calling the segmentation model
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            if (documentBodyParts != null) {
                List<LayoutToken> tokenizationsBody = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentBodyParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsBody.add(tokenizations.get(i));
                    }
                }

                StringBuilder bufferBody = new StringBuilder();

                // just write the text without any label
                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                if (id == -1) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\">\n");
                } else {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\">\n");
                }
                writer.write(bufferBody.toString());
                writer.write("\n\t\t</document>\n");
                writer.write("\t</subcorpus>\n</corpus>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Create datasets from The QUAERO French Medical Corpus.
     * Névéol A, Grouin C, Leixa J, Rosset S, Zweigenbaum P. The QUAERO French Medical Corpus: A Ressource for Medical Entity Recognition and Normalization. Fourth Workshop on Building and Evaluating Ressources for Health and Biomedical Text Processing - BioTxtM2014. 2014:24-30
     * https://quaerofrenchmed.limsi.fr
     *
     * @param inputDirectory    input directory containing The QUAERO French Medical Corpus
     * @param outputDirectory   path for results
     * @param ind               id
     */
    public int createDatasetsFromQuaeroBatch(String inputDirectory,
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
                    createTraining(file, outputDirectory, n);

                    // uncomment this command to create files containing features and blank training without any label
                    //createBlankTrainingFromPDF(file, outputDirectory, n);
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


    public static String toFeatureVectorLayout(List<LayoutToken> tokens, MedicalNERLexiconPositionsIndexes positionsIndexes) {
        StringBuffer ress = new StringBuffer();
        int posit = 0; // keep track of the position index in the list of positions

        for (LayoutToken token : tokens) {
            if ((token.getText() == null) ||
                (token.getText().length() == 0) ||
                token.getText().equals(" ") ||
                token.getText().equals("\t") ||
                token.getText().equals("\n") ||
                token.getText().equals("\r") ||
                token.getText().equals("\u00A0")) {
                continue;
            }

            // check if the token is a known NE
            // do we have a NE at position posit?
            boolean isLocationToken = LexiconPositionsIndexes
                .isTokenInLexicon(positionsIndexes.getLocalLocationPositions(), posit);
            boolean isPersonTitleToken = LexiconPositionsIndexes
                .isTokenInLexicon(positionsIndexes.getLocalPersonTitlePositions(), posit);
            boolean isOrganisationToken = LexiconPositionsIndexes
                .isTokenInLexicon(positionsIndexes.getLocalOrganisationPositions(), posit);
            boolean isOrgFormToken = LexiconPositionsIndexes
                .isTokenInLexicon(positionsIndexes.getLocalOrgFormPositions(), posit);

            ress.append(FeaturesVectorNER
                .addFeaturesNER(token.getText(),
                    isLocationToken, isPersonTitleToken, isOrganisationToken, isOrgFormToken)
                .printVector());

            ress.append("\n");
            posit++;
        }
        ress.append("\n");
        return ress.toString();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}
