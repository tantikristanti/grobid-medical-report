package org.grobid.core.engines;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.Entity;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentPointer;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * A class for process medical NER.
 * <p>
 * Tanti, 2021
 */
public class FrMedicalNERParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrMedicalNERParser.class);
    protected File tmpPath = null;
    protected EngineMedicalParsers parsers;
    protected NERParsers nerParsers;
    //private DeLFTTagger frMedicalNer;
    private GenericTagger frMedicalNer;

    public FrMedicalNERParser() {
        frMedicalNer = TaggerFactory.getTagger(GrobidModels.ENTITIES_NER);
        //frMedicalNer = TaggerFactory.getTagger(GrobidModels.FR_MEDICAL_NER_QUAERO, GrobidCRFEngine.DELFT);
    }

    public FrMedicalNERParser(EngineMedicalParsers parsers) {
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
        frMedicalNer = TaggerFactory.getTagger(GrobidModels.ENTITIES_NER);
        //frMedicalNer = new DeLFTTagger(GrobidModels.FR_MEDICAL_NER_QUAERO, "BidLSTM_CRF");
        //frMedicalNer = TaggerFactory.getTagger(GrobidModels.FR_MEDICAL_NER_QUAERO, GrobidCRFEngine.DELFT);
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

    public int processNERBatch(String inputDirectory,
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
                    // NER with grobid-ner
                    processNERWithGrobidNer(file, outputDirectory, n);

                    // NER with french-medical-ner
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
     * Process the specified pdf and format the result as blank training data for the header model.
     *
     * @param inputFile  input PDF file
     * @param pathOutput path to raw monograph featured sequence
     * @param pathOutput path to TEI
     * @param id         id
     */

    public Document processNERWithGrobidNer(File inputFile,
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

            // path for the results
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".fr.medical.ner.tei.xml"));

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

            // take only the body part
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

                // the text without any label
                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                if (id == -1) {
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader/>\n\t<text xml:lang=\"fr\">\n");
                } else {
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + id +
                        "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"fr\">\n");
                }
                //writer.write(bufferBody.toString());

                // calling the French Medical NER model (grobid-ner)
                nerParsers = new NERParsers();
                String stringBody = bufferBody.toString();
                List<Entity> entities = nerParsers.extractNE(stringBody);

                if (entities != null) {
                    int nerTextLength = 0;
                    for (Entity entity : entities) {
                        String nerTextStart = "<ENAMEX type=\"" + entity.getType().getName() + "\">";
                        String nerTextEnd = "</ENAMEX>";
                        int offsetStart = entity.getOffsetStart() + nerTextLength;
                        int offsetEnd = entity.getOffsetEnd() + nerTextLength;
                        nerTextLength = nerTextLength + nerTextStart.length() + nerTextEnd.length();
                        String combinedText = stringBody.substring(0, offsetStart) + nerTextStart +
                            entity.getRawName() + nerTextEnd + stringBody.substring(offsetEnd);
                        stringBody = combinedText;
                        /*writer.write(entity.getRawName() + "\t" + entity.getType().getName() + "\t" +
                            entity.getOffsetStart() + "\t" + entity.getOffsetEnd() + "\n");*/
                    }
                }
                // write the results by paragraph, that is, one paragraph contains one line
                String resultSplit[] = stringBody.split("\n");
                for (int p = 0; p < resultSplit.length; p++) {
                    writer.write("\t\t<p id=\"" + p + "\">" + resultSplit[p] + "</p>\n");
                }
                writer.write("\n\t</text>\n</tei>\n");
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

    public void close() throws IOException {
    }

}