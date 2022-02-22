
package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.HeaderMedicalItem;
import org.grobid.core.data.LeftNoteMedicalItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;
import org.grobid.core.utilities.crossref.CrossrefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

/**
 * A class for managing the extraction of medical information from PDF documents or raw text.
 */
public class EngineMedical extends Engine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngineMedical.class);
    private final EngineMedicalParsers parsers = new EngineMedicalParsers();

    private static CntManager cntManager = CntManagerFactory.getCntManager();

    /**
     * Constructor for the grobid-medical-report engine instance.
     *
     * @param loadModels
     */
    public EngineMedical(boolean loadModels) {
        super(loadModels);
    }

    /**
     * Apply a parsing model for the header of a PDF file based on CRF, using
     * first three pages of the PDF
     *
     * @param inputFile         the path of the PDF file to be processed
     * @param resultHeader      result from the header parser
     * @param resultLeftNote    result from the left-note parser
     * @return the TEI representation of the extracted header and left-note items
     * information
     */
    public String processHeaderLeftNoteMedicalReport(
        String inputFile,
        HeaderMedicalItem resultHeader,
        LeftNoteMedicalItem resultLeftNote
    ) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .build();
        return processHeaderLeftNoteMedicalReport(inputFile, null, config, resultHeader, resultLeftNote);
    }

    /**
     * Apply a parsing model for the header of a PDF file based on CRF, using
     * dynamic range of pages as header
     *
     * @param inputFile         : the path of the PDF file to be processed
     * @param resultHeader      : header item result
     * @return the TEI representation of the extracted bibliographical
     * information
     */
    public String processHeaderLeftNoteMedicalReport(String inputFile, String md5Str, GrobidAnalysisConfig config,
                                                     HeaderMedicalItem resultHeader , LeftNoteMedicalItem resultLeftNote) {
        // normally the header or left note items must not be null, but if it is the case,
        // we still continue with a new instance, so that the resulting TEI string is still delivered
        if (resultHeader == null) {
            resultHeader = new HeaderMedicalItem();
        }
        if (resultLeftNote == null) {
            resultLeftNote = new LeftNoteMedicalItem();
        }
        Pair<String, Document> resultTEI = parsers.getHeaderMedicalParser().processing(new File(inputFile), md5Str, resultHeader, resultLeftNote, config);
        return resultTEI.getLeft(); // the left result is the TEI results, while the right one is the Document object result
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param inputFile    : the path of the PDF file to be processed
     * @param pathOutput      : the path where to put the CRF feature file and  the path where to put the annotated TEI representation (the
     *                     file to be annotated for "from scratch" training data)
     * @param id           : an optional ID to be used in the TEI file and the full text
     *                     file, -1 if not used
     */
    public void createTrainingBlank(File inputFile, String pathOutput, int id) {
        parsers.getFullMedicalTextParser().createBlankTrainingFromPDF(inputFile, pathOutput, id);
    }

    /**
     * Create training data for all models based on the application of
     * the current full text model on a new PDF
     *
     * @param inputFile    : the path of the PDF file to be processed
     * @param pathOutput   : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                      file to be corrected for gold-level training data)
     * @param id           : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTraining(File inputFile, String pathOutput,  int id) {
        System.out.println(inputFile.getPath());
        Document doc = parsers.getFullMedicalTextParser().createTraining(inputFile, pathOutput, id);
    }

    /**
     * Process all the PDF in a given directory with a segmentation process and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTraining(String directoryPath, String resultPath, int ind) {
        try {
            File path = new File(directoryPath);
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
            for (final File pdfFile : refFiles) {
                try {
                    createTraining(pdfFile, resultPath, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Process all the PDF in a given directory with a pdf extraction and
     * produce blank training data, i.e. TEI files with text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                        and default CRF feature files shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                        identify the training case. This is optional: no identifier
     *                        will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingBlank(String directoryPath, String resultPath, int ind) {
        try {
            File path = new File(directoryPath);
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
            for (final File pdfFile : refFiles) {
                try {
                    createTrainingBlank(pdfFile, resultPath, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     *
     * //TODO: remove invalid JavaDoc once refactoring is done and tested (left for easier reference)
     * Parse and convert the current article into TEI, this method performs the
     * whole parsing and conversion process. If onlyHeader is true, than only
     * the tei header data will be created.
     *
     * @param inputFile            - absolute path to the pdf to be processed
     * @param config               - Grobid config
     * @return the resulting structured document as a TEI string.
     */
    public String fullTextToTEI(File inputFile,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, null, config).getTei();
    }

    /**
     *
     * //TODO: remove invalid JavaDoc once refactoring is done and tested (left for easier reference)
     * Parse and convert the current article into TEI, this method performs the
     * whole parsing and conversion process. If onlyHeader is true, than only
     * the tei header data will be created.
     *
     * @param inputFile            - absolute path to the pdf to be processed
     * @param md5Str               - MD5 digest of the PDF file to be processed
     * @param config               - Grobid config
     * @return the resulting structured document as a TEI string.
     */
    public String fullTextToTEI(File inputFile,
                                String md5Str,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, md5Str, config).getTei();
    }

    public Document fullTextToTEIDoc(File inputFile,
                                     String md5Str,
                                     GrobidAnalysisConfig config) throws Exception {
        FullMedicalTextParser fullTextParser = parsers.getFullMedicalTextParser();
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToTEI on " + inputFile);
        long time = System.currentTimeMillis();
        resultDoc = fullTextParser.processing(inputFile, md5Str, config);
        LOGGER.debug("Ending processing fullTextToTEI on " + inputFile + ". Time to process: "
            + (System.currentTimeMillis() - time) + "ms");
        return resultDoc;
    }

    public Document fullTextToTEIDoc(File inputFile,
                                     GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, null, config);
    }

    public Document fullTextToTEIDoc(DocumentSource documentSource,
                                     GrobidAnalysisConfig config) throws Exception {
        FullTextParser fullTextParser = parsers.getFullTextParser();
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToTEI on " + documentSource);
        long time = System.currentTimeMillis();
        resultDoc = fullTextParser.processing(documentSource, config);
        LOGGER.debug("Ending processing fullTextToTEI on " + documentSource + ". Time to process: "
            + (System.currentTimeMillis() - time) + "ms");
        return resultDoc;
    }

    @Override
    public synchronized void close() throws IOException {
        CrossrefClient.getInstance().close();
        parsers.close();
    }

    public static void setCntManager(CntManager cntManager) {
        EngineMedical.cntManager = cntManager;
    }

    public static CntManager getCntManager() {
        return cntManager;
    }

    public EngineMedicalParsers getParsers() {
        return parsers;
    }
}