package org.grobid.core.main.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.engines.EngineMedicalParsers;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.MedicalReportConfiguration;
import org.grobid.core.utilities.MedicalReportProperties;
import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;


/**
 * A main class of medical report batch processes.
 */
public class GrobidMedicalReportMain {
    private static Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportMain.class);

    private static final String COMMAND_CREATE_TRAINING_SEGMENTATION = "createTrainingSegmentation";
    private static final String COMMAND_CREATE_TRAINING_HEADER = "createTrainingHeader";
    private static final String COMMAND_CREATE_TRAINING_LEFT_NOTE = "createTrainingLeftNote";
    private static final String COMMAND_CREATE_TRAINING_FULL_MEDICAL_TEXT = "createTrainingFullMedicalText";
    private static final String COMMAND_PROCESS_HEADER_HIGH_LEVEL = "processHeader";
    private static final String COMMAND_PROCESS_HEADER = "processHeaderUser";
    private static final String COMMAND_PROCESS_LEFT_NOTE = "processLeftNote";
    private static final String COMMAND_PROCESS_FULL_TEXT = "processFulltextMedical";
    private static final String COMMAND_CREATE_MEDICAL_NER_TRAINING = "createMedicalNerTraining";

    private static List<String> availableCommands = Arrays.asList(
        COMMAND_CREATE_TRAINING_SEGMENTATION,
        COMMAND_CREATE_TRAINING_HEADER,
        COMMAND_CREATE_TRAINING_LEFT_NOTE,
        COMMAND_CREATE_TRAINING_FULL_MEDICAL_TEXT,
        COMMAND_PROCESS_HEADER_HIGH_LEVEL,
        COMMAND_PROCESS_HEADER,
        COMMAND_PROCESS_LEFT_NOTE,
        COMMAND_PROCESS_FULL_TEXT,
        COMMAND_CREATE_MEDICAL_NER_TRAINING
    );

    /**
     * Arguments of the batch.
     */
    private static GrobidMedicalReportMainArgs gbdArgs;


    /**
     * Build the path to grobid.properties from the path to grobid-home.
     *
     * @param pPath2GbdHome The path to Grobid home.
     * @return the path to grobid.properties.
     */
    protected final static String getPath2GbdProperties(final String pPath2GbdHome) {
        return pPath2GbdHome + File.separator + "config" + File.separator + "grobid.properties";
    }

    /**
     * Infer some parameters not given in arguments.
     */
    protected static void inferParamsNotSet() {
        String tmpFilePath;
        if (gbdArgs.getPath2grobidHome() == null) {
            MedicalReportConfiguration medicalReportConfiguration = null;
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                medicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), MedicalReportConfiguration.class);
            } catch (Exception e) {
                LOGGER.error("The config file does not appear valid, see resources/config/grobid-medical-report.yaml", e);
            }

            tmpFilePath = medicalReportConfiguration.getGrobidHome();

            if (tmpFilePath == null) {
                tmpFilePath = new File("grobid-home").getAbsolutePath();
                System.out.println("No path set for grobid-home. Using: " + tmpFilePath);
            }

            gbdArgs.setPath2grobidHome(tmpFilePath);
            gbdArgs.setPath2grobidProperty(new File("grobid.properties").getAbsolutePath());
        }
    }

    /**
     * Initialize the batch (using previous GrobidProperties configuration).
     */
    /*protected static void initProcess() {
        GrobidProperties.getInstance();
    }*/

    /*protected static void initProcess(String grobidHome) {
        try {
            final GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(grobidHome));
            grobidHomeFinder.findGrobidHomeOrFail();
            GrobidProperties.getInstance(grobidHomeFinder);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed: " + exp);
        }
    }*/

    /**
     * Init process with the provided grobid-home or  default value of the grobid home
     *
     * @param grobidHome
     */
    protected static void initProcess(String grobidHome) {
        MedicalReportConfiguration medicalReportConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            medicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), MedicalReportConfiguration.class);
        } catch (Exception e) {
            System.err.println("The config file does not appear valid, see resources/config/grobid-medical-report.yaml");
        }
        try {
            String pGrobidHome = medicalReportConfiguration.getGrobidHome();

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);

            System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getInstance().getGrobidHome());

            for (ModelParameters theModel : medicalReportConfiguration.getModels())
                GrobidProperties.getInstance().addModel(theModel);

            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("grobid-medical-report initialisation failed: " + exp);
            exp.printStackTrace();
        }
    }

    /**
     * @return String to display for help.
     */
    protected static String getHelp() {
        final StringBuffer help = new StringBuffer();
        help.append("HELP GROBID\n");
        help.append("-h: displays help\n");
        help.append("-gH: gives the path to grobid home directory.\n");
        help.append("-dIn: gives the path to the directory where the files to be processed are located, to be used only when the called method needs it.\n");
        help.append("-dOut: gives the path to the directory where the result files will be saved. The default output directory is the curent directory.\n");
        help.append("-r: recursive directory processing, default processing is not recursive.\n");
        help.append("-ignoreAssets: do not extract and save the PDF assets (bitmaps, vector graphics), by default the assets are extracted and saved.\n");
        help.append("-teiCoordinates: output a subset of the identified structures with coordinates in the original PDF, by default no coordinates are present.\n");
        help.append("-segmentSentences: add sentence segmentation level structures for paragraphs in the TEI XML result, by default no sentence segmentation is done.\n");
        help.append("-s: is the parameter used for process using string as input and not file.\n");
        help.append("-exe: gives the command to execute. The value should be one of these:\n");
        help.append("\t" + availableCommands + "\n");
        return help.toString();
    }

    /**
     * Process batch given the args.
     *
     * @param pArgs The arguments given to the batch.
     */
    protected static boolean processArgs(final String[] pArgs) {
        boolean result = true;
        if (pArgs.length == 0) {
            System.out.println(getHelp());
            result = false;
        } else {
            String currArg;
            for (int i = 0; i < pArgs.length; i++) {
                currArg = pArgs[i];
                if (currArg.equals("-h")) {
                    System.out.println(getHelp());
                    result = false;
                    break;
                }
                if (currArg.equals("-gH")) {
                    gbdArgs.setPath2grobidHome(pArgs[i + 1]);
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2grobidProperty(getPath2GbdProperties(pArgs[i + 1]));
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-dIn")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2Input(pArgs[i + 1]);
                        gbdArgs.setPdf(true);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-s")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setInput(pArgs[i + 1]);
                        gbdArgs.setPdf(false);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-dOut")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2Output(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-r")) {
                    gbdArgs.setRecursive(true);
                    continue;
                }
                if (currArg.equals("-ignoreAssets")) {
                    gbdArgs.setSaveAssets(false);
                    continue;
                }
                if (currArg.equals("-teiCoordinates")) {
                    gbdArgs.setTeiCoordinates(true);
                    continue;
                }
                if (currArg.equals("-segmentSentences")) {
                    gbdArgs.setSegmentSentences(true);
                    continue;
                }
                if (currArg.equals("-exe")) {
                    final String command = pArgs[i + 1];
                    if (availableCommands.contains(command)) {
                        gbdArgs.setProcessMethodName(command);
                        i++;
                        continue;
                    } else {
                        System.err.println("-exe value should be one value from this list: " + availableCommands);
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static void main(final String[] args) throws Exception {
        gbdArgs = new GrobidMedicalReportMainArgs();

        if (processArgs(args) && (gbdArgs.getProcessMethodName() != null)) {
            inferParamsNotSet();
            if (isNotEmpty(gbdArgs.getPath2grobidHome())) {
                initProcess(gbdArgs.getPath2grobidHome());
            } else {
                LOGGER.warn("Grobid home not provided, using default. ");
                initProcess(null);
            }

            int nb = 0;
            long time = System.currentTimeMillis();

            EngineMedicalParsers parsers = new EngineMedicalParsers();

            if (gbdArgs.getProcessMethodName().equals(COMMAND_CREATE_TRAINING_SEGMENTATION)) {
                nb = parsers.getMedicalReportSegmenterParser().createTrainingMedicalSegmentationBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_CREATE_TRAINING_HEADER)) {
                nb = parsers.getHeaderMedicalParser().createTrainingMedicalHeaderBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_CREATE_TRAINING_LEFT_NOTE)) {
                nb = parsers.getLeftNoteMedicalParser().createTrainingMedicalLeftNoteBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_CREATE_TRAINING_FULL_MEDICAL_TEXT)) {
                nb = parsers.getFullMedicalTextParser().createTrainingFullMedicalTextBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_PROCESS_HEADER_HIGH_LEVEL)) {
                nb = parsers.getHeaderMedicalParser().processHighLevelBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_PROCESS_HEADER)) {
                nb = parsers.getHeaderMedicalParser().processHeaderDirectory(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_PROCESS_LEFT_NOTE)) {
                nb = parsers.getLeftNoteMedicalParser().processLeftNoteDirectory(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_PROCESS_FULL_TEXT)) {
                nb = parsers.getFullMedicalTextParser().processFullTextDirectory(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(),
                    gbdArgs.isRecursive(), gbdArgs.getSaveAssets(), gbdArgs.getTeiCoordinates(), gbdArgs.getSegmentSentences(), -1);
            } else if (gbdArgs.getProcessMethodName().equals(COMMAND_CREATE_MEDICAL_NER_TRAINING)) {
                nb = parsers.getFrenchMedicalNERParser().createTrainingFrenchMedicalNerBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else {
                throw new RuntimeException("Command not yet implemented.");
            }
            LOGGER.info(nb + " files processed in " + (System.currentTimeMillis() - time) + " milliseconds");
            parsers.close();
        }
    }
}
