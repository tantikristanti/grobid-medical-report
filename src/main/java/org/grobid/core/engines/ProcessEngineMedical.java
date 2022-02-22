package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidMedicalFactory;
import org.grobid.core.main.batch.GrobidMainArgs;
import org.grobid.core.main.batch.GrobidMedicalReportMain;
import org.grobid.core.main.batch.GrobidMedicalReportMainArgs;
import org.grobid.core.utilities.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Perform the batch processing for the different engine methods.
 */

public class ProcessEngineMedical implements Closeable {

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
    public void processHeader(final GrobidMedicalReportMainArgs pGbdArgs) throws Exception {
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
    private void processHeaderDirectory(File[] files, final GrobidMedicalReportMainArgs pGbdArgs, String outputPath) {
        if (files != null) {
            boolean recurse = pGbdArgs.isRecursive();
            String result;
            for (final File currPdf : files) {
                try {
                    if (currPdf.getName().toLowerCase().endsWith(".pdf")) {
                        result = getEngine().processHeaderLeftNoteMedicalReport(currPdf.getAbsolutePath(), null, null);
                        File outputPathFile = new File(outputPath);
                        if (!outputPathFile.exists()) {
                            outputPathFile.mkdirs();
                        }
                        if (currPdf.getName().endsWith(".pdf")) {
                            IOUtilities.writeInFile(outputPath + File.separator
                                    + new File(currPdf.getAbsolutePath())
                                    .getName().replace(".pdf", ".header.medical.tei.xml"), result.toString());
                        } else if (currPdf.getName().endsWith(".PDF")) {
                            IOUtilities.writeInFile(outputPath + File.separator
                                    + new File(currPdf.getAbsolutePath())
                                    .getName().replace(".PDF", ".header.medical.tei.xml"), result.toString());
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
     * Process the full text using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processFullText(final GrobidMedicalReportMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        File[] files = pdfDirectory.listFiles();
        if (files == null) {
            LOGGER.warn("No files in directory: " + pdfDirectory);
        } else {
            List<String> elementCoordinates = null;
            if (pGbdArgs.getTeiCoordinates()) {
                elementCoordinates = Arrays.asList("org", "persName", "address");
            }
            processFullTextDirectory(files, pGbdArgs, pGbdArgs.getPath2Output(), pGbdArgs.getSaveAssets(),
                elementCoordinates, pGbdArgs.getSegmentSentences());
            System.out.println(Engine.getCntManager());
        }

    }

    /**
     * Process the full text recursively or not using pGbdArgs parameters.
     *
     * @param files    list of files to be processed
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    private void processFullTextDirectory(File[] files,
                                          final GrobidMainArgs pGbdArgs,
                                          String outputPath,
                                          boolean saveAssets,
                                          List<String> elementCoordinates,
                                          boolean segmentSentences) {
        if (files != null) {
            boolean recurse = pGbdArgs.isRecursive();
            String result;
            for (final File currPdf : files) {
                try {
                    if (currPdf.getName().toLowerCase().endsWith(".pdf")) {
                        System.out.println("Processing: " + currPdf.getPath());
                        GrobidAnalysisConfig config = null;
                        // path for saving assets
                        if (saveAssets) {
                            String baseName = currPdf.getName().replace(".pdf", "").replace(".PDF", "");
                            String assetPath = outputPath + File.separator + baseName + "_assets";
                            config = GrobidAnalysisConfig.builder()
                                .pdfAssetPath(new File(assetPath))
                                .generateTeiCoordinates(elementCoordinates)
                                .withSentenceSegmentation(segmentSentences)
                                .build();
                        } else
                            config = GrobidAnalysisConfig.builder()
                                .generateTeiCoordinates(elementCoordinates)
                                .withSentenceSegmentation(segmentSentences)
                                .build();
                        result = getEngine().fullTextToTEI(currPdf, config);
                        File outputPathFile = new File(outputPath);
                        if (!outputPathFile.exists()) {
                            outputPathFile.mkdir();
                        }
                        if (currPdf.getName().endsWith(".pdf")) {
                            IOUtilities.writeInFile(outputPath + File.separator
                                + new File(currPdf.getAbsolutePath())
                                .getName().replace(".pdf", ".medical.tei.xml"), result.toString());
                        } else if (currPdf.getName().endsWith(".PDF")) {
                            IOUtilities.writeInFile(outputPath + File.separator
                                + new File(currPdf.getAbsolutePath())
                                .getName().replace(".PDF", ".medical.tei.xml"), result.toString());
                        }
                    } else if (recurse && currPdf.isDirectory()) {
                        File[] newFiles = currPdf.listFiles();
                        if (newFiles != null) {
                            String newLevel = currPdf.getName();
                            processFullTextDirectory(newFiles, pGbdArgs, outputPath +
                                File.separator + newLevel, saveAssets, elementCoordinates, segmentSentences);
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
     * List the engine methods that can be called.
     *
     * @return List<String> containing the list of the methods.
     */
    public final static List<String> getUsableMethods() {
        final Class<?> pClass = new ProcessEngine().getClass();
        final List<String> availableMethods = new ArrayList<String>();
        for (final Method method : pClass.getMethods()) {
            if (isUsableMethod(method.getName())) {
                availableMethods.add(method.getName());
            }
        }
        return availableMethods;
    }

    /**
     * Check if the method is usable.
     *
     * @param pMethod method name.
     * @return if it is usable
     */
    protected final static boolean isUsableMethod(final String pMethod) {
        boolean isUsable = StringUtils.equals("wait", pMethod);
        isUsable |= StringUtils.equals("equals", pMethod);
        isUsable |= StringUtils.equals("toString", pMethod);
        isUsable |= StringUtils.equals("hashCode", pMethod);
        isUsable |= StringUtils.equals("getClass", pMethod);
        isUsable |= StringUtils.equals("notify", pMethod);
        isUsable |= StringUtils.equals("notifyAll", pMethod);
        isUsable |= StringUtils.equals("isUsableMethod", pMethod);
        isUsable |= StringUtils.equals("getUsableMethods", pMethod);
        isUsable |= StringUtils.equals("inferPdfInputPath", pMethod);
        isUsable |= StringUtils.equals("inferOutputPath", pMethod);
        isUsable |= StringUtils.equals("close", pMethod);
        return !isUsable;
    }

    /**
     * Infer the input path for pdfs if not given in arguments.
     *
     * @param pGbdArgs The GrobidMedicalReportMainArgs.
     */
    protected final static void inferPdfInputPath(final GrobidMedicalReportMainArgs pGbdArgs) {
        String tmpFilePath;
        if (pGbdArgs.getPath2Input() == null) {
            tmpFilePath = new File(".").getAbsolutePath();
            LOGGER.info("No path set for the input directory. Using: " + tmpFilePath);
            pGbdArgs.setPath2Input(tmpFilePath);
        }
    }

    /**
     * Infer the output path if not given in arguments.
     *
     * @param pGbdArgs The GrobidMedicalReportMainArgs.
     */
    protected final static void inferOutputPath(final GrobidMedicalReportMainArgs pGbdArgs) {
        String tmpFilePath;
        if (pGbdArgs.getPath2Output() == null) {
            tmpFilePath = new File(".").getAbsolutePath();
            LOGGER.info("No path set for the output directory. Using: " + tmpFilePath);
            pGbdArgs.setPath2Output(tmpFilePath);
        }
    }

}
