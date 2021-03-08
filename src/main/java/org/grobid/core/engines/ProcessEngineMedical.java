package org.grobid.core.engines;

import org.grobid.core.factory.GrobidMedicalFactory;
import org.grobid.core.main.batch.GrobidMainArgs;
import org.grobid.core.utilities.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Perform the batch processing for the different engine methods.
 *
 * @author Damien, Patrice
 */

public class ProcessEngineMedical extends ProcessEngine {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineMedical.class);

    /**
     * The engine.
     */
    private static EngineMedical engine;

    /**
     * @return the engine instance.
     */

    protected EngineMedical getEngine() {
        if (engine == null) {
            engine = GrobidMedicalFactory.getInstance().createEngine();
        }
        return engine;
    }

    /**
     * Close engine resources.
     */
    @Override
    public void close() throws IOException {
        if (engine != null) {
            engine.close();
        }
        System.exit(0);
    }

    /**
     * Process the headers using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processHeader(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        File[] files = pdfDirectory.listFiles();
        if (files == null) {
            LOGGER.warn("No files in directory: " + pdfDirectory);
        } else {
            processHeaderDirectory(files, pGbdArgs, pGbdArgs.getPath2Output());
        }
    }

    /**
     * Process the header recursively or not using pGbdArgs parameters.
     *
     * @param files    list of files to be processed
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    private void processHeaderDirectory(File[] files, final GrobidMainArgs pGbdArgs, String outputPath) {
        if (files != null) {
            boolean recurse = pGbdArgs.isRecursive();
            String result;
            for (final File currPdf : files) {
                try {
                    if (currPdf.getName().toLowerCase().endsWith(".pdf")) {
                        result = getEngine().processHeader(currPdf.getAbsolutePath(), 0, null);
                        File outputPathFile = new File(outputPath);
                        if (!outputPathFile.exists()) {
                            outputPathFile.mkdirs();
                        }
                        if (currPdf.getName().endsWith(".pdf")) {
                            IOUtilities.writeInFile(outputPath + File.separator
                                    + new File(currPdf.getAbsolutePath())
                                    .getName().replace(".pdf", ".tei.xml"), result.toString());
                        } else if (currPdf.getName().endsWith(".PDF")) {
                            IOUtilities.writeInFile(outputPath + File.separator
                                    + new File(currPdf.getAbsolutePath())
                                    .getName().replace(".PDF", ".tei.xml"), result.toString());
                        }
                    } else if (recurse && currPdf.isDirectory()) {
                        File[] newFiles = currPdf.listFiles();
                        if (newFiles != null) {
                            String newLevel = currPdf.getName();
                            processHeaderDirectory(newFiles, pGbdArgs, outputPath +
                                    File.separator + newLevel);
                        }
                    }
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the file " + currPdf.getAbsolutePath()
                            + ". Continuing the process for the other files", exp);
                }
            }
        }
    }


    /**
     * Generate training data for all models
     *
     * @param pGbdArgs The parameters.
     */
    public void createTraining(final GrobidMainArgs pGbdArgs) {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTraining(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model. 
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingBlank(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingBlank(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

}
