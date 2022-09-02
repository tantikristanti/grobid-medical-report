
package org.grobid.core.engines;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.*;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidMedicalPoolingFactory;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;
import org.grobid.core.utilities.crossref.CrossrefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public EngineMedicalParsers getParsers() {
        return parsers;
    }

    /**
     * @return a new engine from GrobidMedicalPoolingFactory if the execution is parallel. Otherwise, return the instance of the engine.
     */
    public static EngineMedical getEngine(boolean preload) {
        return GrobidMedicalPoolingFactory.getEngineFromPool(preload);
    }

    public static void setCntManager(CntManager cntManager) {
        EngineMedical.cntManager = cntManager;
    }

    public static CntManager getCntManager() {
        return cntManager;
    }

    /**
     * Parse a raw string containing datelines.
     *
     * @param dateline - the string containing raw datelines.
     * @return the list of all structured dateline objects recognized in the string.
     * @throws IOException
     */
    public Dateline processDateline(String dateline) throws Exception {
        Dateline result = parsers.getDatelineParser().process(dateline);
        return result;
    }

    /**
     * Parse a raw string containing datelines.
     *
     * @param dateline - the string containing raw dates.
     * @return the list of all structured dateline objects recognized in the string.
     * @throws IOException
     */
    public String processDatelineAsRaw(String dateline) throws Exception {
        List<String> inputs = new ArrayList<String>();
        inputs.add(dateline.trim());
        String result = parsers.getDatelineParser().trainingExtraction(inputs).toString();
        return result;
    }

    /**
     * Parse a raw string containing medics.
     *
     * @param medic - the string containing raw medics.
     * @return the list of all structured medic objects recognized in the string.
     * @throws IOException
     */
    public Medic processMedic(String medic) throws Exception {
        Medic result = parsers.getMedicParser().process(medic);
        return result;
    }

    /**
     * Parse a raw string containing medics.
     *
     * @param medic - the string containing raw medics.
     * @return the list of all structured medic objects recognized in the string.
     * @throws IOException
     */
    public String processMedicAsRaw(String medic) throws Exception {
        List<String> inputs = new ArrayList<String>();
        inputs.add(medic.trim());
        String result = parsers.getMedicParser().trainingExtraction(inputs).toString();
        return result;
    }

    /**
     * Parse a raw string containing patients.
     *
     * @param patient - the string containing raw patients.
     * @return the list of all structured patient objects recognized in the string.
     * @throws IOException
     */
    public Patient processPatient(String patient) throws Exception {
        Patient result = parsers.getPatientParser().process(patient);
        return result;
    }

    /**
     * Parse a raw string containing patients.
     *
     * @param patient - the string containing raw patients.
     * @return the list of all structured patient objects recognized in the string.
     * @throws IOException
     */
    public String processPatientAsRaw(String patient) throws Exception {
        List<String> inputs = new ArrayList<String>();
        inputs.add(patient.trim());
        String result = parsers.getPatientParser().trainingExtraction(inputs).toString();
        return result;
    }

    /**
     * Parse a raw string containing medical terminologies.
     *
     * @param text - the string containing raw text.
     * @return the list of all structured medical terminology objects recognized in the string.
     * @throws IOException
     */
    public List<MedicalEntity> processMedicalNER(String text) throws Exception {
        List<MedicalEntity> result = parsers.getFrenchMedicalNERParser().extractNER(text);
        return result;
    }

    /**
     * Parse a raw string containing medical terminologies.
     *
     * @param text - the string containing raw text.
     * @return the list of all structured medical terminologies recognized in the string.
     * @throws IOException
     */
    public StringBuilder processMedicalNERAsRaw(String text) throws Exception {
        StringBuilder result = parsers.getFrenchMedicalNERParser().extractNERToString(text);
        return result;
    }

    /**
     * Apply a CRF header parsing model on PDF file.
     * Parse only first three pages of the PDF file (normally, header part can be found in the first page of the document)
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param md5Str      MD5 digest of the processed file
     * @param result      POJO result
     * @return the TEI representation of the extracted header information
     */
    public String processHeader(String inputFile, String md5Str, HeaderMedicalItem result) {
        // use the Grobid config builder
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .build();
        return processHeader(inputFile, md5Str, config, result);
    }

    public String processHeader(String inputFile, String md5Str, GrobidAnalysisConfig config, HeaderMedicalItem result) {
        // if the result is null, continue with a new instance thus the resulting TEI is still delivered
        if (result == null) {
            result = new HeaderMedicalItem();
        }
        // call the header medical parser
        Pair<String, Document> resultTEI = parsers.getHeaderMedicalParser().processingHeader(new File(inputFile), md5Str, result, config);
        return resultTEI.getLeft();
    }

    /**
     * Apply a CRF left-note parsing model on PDF file.
     * Parse only first three pages of the PDF file (normally, structure organization information can be found in the first page of the document)
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param md5Str      MD5 digest of the processed file
     * @param result      POJO result
     * @return the TEI representation of the extracted left-note information
     */
    public String processLeftNote(String inputFile, String md5Str, LeftNoteMedicalItem result) {
        // use the Grobid config builder
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .build();
        return processLeftNote(inputFile, md5Str, config, result);
    }

    public String processLeftNote(String inputFile, String md5Str, GrobidAnalysisConfig config, LeftNoteMedicalItem result) {
        // if the result is null, continue with a new instance thus the resulting TEI is still delivered
        if (result == null) {
            result = new LeftNoteMedicalItem();
        }
        // call the left-note medical parser
        Pair<String, Document> resultTEI = parsers.getLeftNoteMedicalParser().processingLeftNote(new File(inputFile), md5Str, result, config);
        return resultTEI.getLeft();
    }

    /**
     * Apply a CRF header and left-note parsing models on PDF file.
     * Parse only first three pages of the PDF file (normally, header and left-note information can be found in the first page of the document)
     *
     * @param inputFile      the path of the PDF file to be processed
     * @param resultHeader   result from the header parser
     * @param resultLeftNote result from the left-note parser
     * @return the TEI representation of the extracted header and left-note items
     * information
     */
    public String processHeaderLeftNoteMedicalReport(
        String inputFile,
        HeaderMedicalItem resultHeader,
        LeftNoteMedicalItem resultLeftNote,
        StringBuilder strLeftNote
    ) {
        // use the Grobid config builder
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .build();
        return processHeaderLeftNoteMedicalReport(inputFile, null, config, resultHeader, resultLeftNote, strLeftNote);
    }

    public String processHeaderLeftNoteMedicalReport(String inputFile, String md5Str, GrobidAnalysisConfig config,
                                                     HeaderMedicalItem resultHeader, LeftNoteMedicalItem resultLeftNote,
                                                     StringBuilder strLeftNote) {
        // normally the header or left note items must not be null, but if it is the case,
        // we still continue with a new instance, so that the resulting TEI string is still delivered
        if (resultHeader == null) {
            resultHeader = new HeaderMedicalItem();
        }
        if (resultLeftNote == null) {
            resultLeftNote = new LeftNoteMedicalItem();
        }
        if (strLeftNote == null) {
            strLeftNote = new StringBuilder();
        }
        // call the header medical parser
        Pair<String, Document> resultTEI = parsers.getHeaderMedicalParser().processingHeaderLeftNote(new File(inputFile), md5Str, resultHeader,resultLeftNote, config);
        return resultTEI.getLeft(); // the left result is the TEI results, while the right one is the Document object result
    }

    /**
     * Apply a CRF French NER model on PDF file.
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param md5Str      MD5 digest of the processed file
     * @return the TEI representation of the extracted French NER information
     */
    public String processFrenchNER(String inputFile, String md5Str) {
        // use the Grobid config builder
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(-1)
            .endPage(-1)
            .build();
        return processFrenchNER(inputFile, md5Str, config);
    }

    public String processFrenchNER(String inputFile, String md5Str, GrobidAnalysisConfig config) {
        // call the French medical parser
        Pair<String, Document> resultTEI = parsers.getFrenchMedicalNERParser().processing(new File(inputFile), md5Str, config);
        return resultTEI.getLeft();
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the path where to put the annotated TEI representation (the
     *                   file to be annotated for "from scratch" training data)
     * @param id         : an optional ID to be used in the TEI file and the full text
     *                   file, -1 if not used
     */
    public void createTrainingBlank(File inputFile, String pathOutput, int id) {
        parsers.getFullMedicalTextParser().createBlankTrainingFromPDF(inputFile, pathOutput, id);
    }

    /**
     * Process all the PDF in a given directory with a pdf extraction and
     * produce blank training data, i.e. TEI files with text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      and default CRF feature files shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
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
                    createTrainingBlank(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Create training data for all models based on the application of
     * the current full text model on a new PDF
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                   file to be corrected for gold-level training data)
     * @param id         : an optional ID to be used in the TEI file, -1 if not used
     */
    public MedicalDocument generateText(File inputFile, String pathOutput, int id) {
        return parsers.getFullMedicalTextParser().generateText(inputFile, pathOutput, id);
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
    public int batchGenerateText(String directoryPath, String resultPath, int ind) {
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
            List<MedicalDocument> medicalDocuments = new ArrayList<>();
            MedicalDocument medicalDocument = new MedicalDocument();
            for (final File pdfFile : refFiles) {
                try {
                    medicalDocument = generateText(pdfFile, resultPath, ind + n);
                    if (medicalDocument != null) {
                        medicalDocuments.add(medicalDocument);
                    }
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
                }
                if (ind != -1)
                    n++;
            }
            System.out.println("medicalDocuments number: " + medicalDocuments.size());
            if (medicalDocuments != null && medicalDocuments.size() > 0) {
                File outputCsvFile = new File(resultPath + "/generatedRawTextMedicalCorpus.csv");
                File outputTextFile = new File(resultPath + "/generatedRawTextMedicalCorpus.txt");
                StringBuffer dataCsv = new StringBuffer(), dataTxt = new StringBuffer();
                dataCsv.append("Document_ID;Raw_Text\n");
                for (MedicalDocument medical : medicalDocuments) {
                    dataCsv.append(medical.getId() + ";" + medical.getText() + "\n");
                    dataTxt.append(medical.getText() + "\n");
                }
                FileUtils.writeStringToFile(outputCsvFile, dataCsv.toString(), StandardCharsets.UTF_8);
                FileUtils.writeStringToFile(outputTextFile, dataTxt.toString(), StandardCharsets.UTF_8);
            }
            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running grobid-medical-report batch.", exp);
        }
    }

    /**
     * Create training data for all models based on the application of
     * the current full text model on a new PDF
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                   file to be corrected for gold-level training data)
     * @param id         : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTraining(File inputFile, String pathOutput, int id) {
        parsers.getFullMedicalTextParser().createTraining(inputFile, pathOutput, id);
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
                    createTraining(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Create training data for all models based on the application of
     * the current full text model on a new PDF
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                   file to be corrected for gold-level training data)
     * @param id         : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTrainingAnonym(File inputFile, String pathOutput, int id) {
        parsers.getFullMedicalTextParser().createTrainingAnonym(inputFile, pathOutput, id);
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
    public int batchCreateTrainingAnonym(String directoryPath, String resultPath, int ind) {
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

            // collect all files contain anonymized data
            File[] refFilesAnonym = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".txt");
                }
            });

            if (refFilesAnonym == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File pdfFile : refFiles) {
                try {
                    createTrainingAnonym(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Create training data for all models based on the application of the current full text model on a new PDF.
     * <p>
     * Some data (document number, patient name and date of birth, doctor's name) will be anonymized.
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                   file to be corrected for gold-level training data)
     * @param id         : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createDataAnonymized(File inputFile, String pathOutput, int id) {
        parsers.getFullMedicalTextParser().createDataAnonymized(inputFile, pathOutput, id);
    }

    /**
     * Process all the PDF in a given directory with a segmentation process and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     * <p>
     * Some data (document number, patient name and date of birth, doctor's name) will be anonymized.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateDataAnonymized(String directoryPath, String resultPath, int ind) {
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
                    createDataAnonymized(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Generate blank training data for the French Medical NER model from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the path where to put the annotated TEI representation (the
     *                   file to be annotated for "from scratch" training data)
     * @param id         : an optional ID to be used in the TEI file and the full text
     *                   file, -1 if not used
     */
    public void createTrainingBlankFrenchMedicalNER(File inputFile, String pathOutput, int id) {
        parsers.getFrenchMedicalNERParser().createBlankTrainingFromPDF(inputFile, pathOutput, id);
    }

    /**
     * Process all the PDF in a given directory with a pdf extraction and
     * produce blank training data, i.e. TEI files with text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      and default CRF feature files shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingBlankFrenchMedicalNER(String directoryPath, String resultPath, int ind) {
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
                    createTrainingBlankFrenchMedicalNER(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Create training data for the French medical NER model based on the application of the current model on a new PDF
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                   file to be corrected for gold-level training data)
     * @param id         : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTrainingFrenchMedicalNER(File inputFile, String pathOutput, int id) {
        parsers.getFrenchMedicalNERParser().createTrainingFromPDF(inputFile, pathOutput, id);
    }

    /**
     * Create anonymized training data for the French medical NER model based on the application of the current model on a new PDF
     *
     * @param inputFile  : the path of the PDF file to be processed
     * @param pathOutput : the path where to put the CRF feature file and  the annotated TEI representation (the
     *                   file to be corrected for gold-level training data)
     * @param id         : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTrainingAnonymFrenchMedicalNER(File inputFile, String pathOutput, int id) {
        parsers.getFrenchMedicalNERParser().createTrainingAnonymFromPDF(inputFile, pathOutput, id);
    }

    /**
     * Process all the PDF in a given directory to produce training data for manual correction.
     * The goal of this method is to help to produce additional training data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingFrenchMedicalNER(String directoryPath, String resultPath, int ind) {
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
                    createTrainingFrenchMedicalNER(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * Process all the PDF in a given directory to produce anonymized training data for manual correction.
     * The goal of this method is to help to produce additional training data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingAnonymFrenchMedicalNER(String directoryPath, String resultPath, int ind) {
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
                    createTrainingAnonymFrenchMedicalNER(pdfFile, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + pdfFile.getPath(), exp);
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
     * //TODO: remove invalid JavaDoc once refactoring is done and tested (left for easier reference)
     * Parse and convert the current article into TEI, this method performs the
     * whole parsing and conversion process. If onlyHeader is true, than only
     * the tei header data will be created.
     *
     * @param inputFile - absolute path to the pdf to be processed
     * @param config    - Grobid config
     * @return the resulting structured document as a TEI string.
     */
    public String fullTextToTEI(File inputFile,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, null, config).getTei();
    }

    /**
     * Parse and convert the current medical document into TEI. This method performs the whole parsing and conversion process.
     *
     * @param inputFile - absolute path to the pdf to be processed
     * @param md5Str    - MD5 digest of the PDF file to be processed
     * @param config    - Grobid config
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
        FullMedicalTextParser fullTextParser = parsers.getFullMedicalTextParser();
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





}