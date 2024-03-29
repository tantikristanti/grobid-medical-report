package org.grobid.trainer;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Training applications.
 */
public class MedicalReportTrainerRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(MedicalReportTrainerRunner.class);
    private static Utility utility = null;

    private static final List<String> models = Arrays.asList("medical-report-segmenter", "full-medical-text", "header-medical-report",
        "left-note-medical-report", "dateline", "fr-medical-ner", "medic", "patient", "organization", "address", "name-person-medical");
    private static final List<String> options = Arrays.asList("0 - train", "1 - evaluate", "2 - split, train and evaluate", "3 - n-fold evaluation");

    private enum RunType {
        TRAIN, EVAL, SPLIT, EVAL_N_FOLD;

        public static RunType getRunType(int i) {
            for (RunType t : values()) {
                if (t.ordinal() == i) {
                    return t;
                }
            }
            throw new IllegalStateException("Unsupported RunType with ordinal " + i);
        }
    }


    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalStateException(
                "Usage: {" + String.join(", ", options) + "} {" + String.join(", ", models) + "} -gH /path/to/Grobid/home}");
        }

        // the mode of train and or evaluation
        RunType mode = RunType.getRunType(Integer.parseInt(args[0]));
        if ((mode == RunType.SPLIT || mode == RunType.EVAL_N_FOLD) && (args.length < 4)) {
            throw new IllegalStateException(
                "Usage: {" + String.join(", ", options) + "} {" + String.join(", ", models) + "} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -n {[int, num folds for n-fold evaluation, optional]}");
        }

        // start grobid-home
        utility = new Utility();
        String path2GbdHome = null;
        Double split = 0.0;
        int numFolds = 0;
        String outputFilePath = null;
        GrobidHomeFinder grobidHomeFinder = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-gH")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing path to Grobid home. ");
                }
                path2GbdHome = args[i + 1];
                grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(path2GbdHome));
                grobidHomeFinder.findGrobidHomeOrFail();
            } else if (args[i].equals("-s")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing split ratio value. ");
                }
                try {
                    split = Double.parseDouble(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid split value: " + args[i + 1]);
                }

            } else if (args[i].equals("-n")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing number of folds value. ");
                }
                try {
                    numFolds = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid number of folds value: " + args[i + 1]);
                }

            } else if (args[i].equals("-o")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing output file. ");
                }
                outputFilePath = args[i + 1];

            }
        }

        utility.initGrobid(path2GbdHome);
        final String path2GbdProperties = path2GbdHome + File.separator + "config" + File.separator + "grobid.properties";

        // setting in grobid
        System.out.println("path2GbdHome=" + path2GbdHome + "   path2GbdProperties=" + path2GbdProperties);

        String model = args[1];
        AbstractTrainer trainer;

        if (model.equals("medical-report-segmenter")) {
            trainer = new MedicalReportSegmenterTrainer();
        } else if (model.equals("full-medical-text")) {
            trainer = new FullMedicalTextTrainer();
        } else if (model.equals("header-medical-report")) {
            trainer = new HeaderMedicalReportTrainer();
        } else if (model.equals("left-note-medical-report")) {
            trainer = new LeftNoteMedicalTrainer();
        } else if (model.equals("fr-medical-ner")) {
            trainer = new FrenchMedicalNERTrainer();
        } else if (model.equals("dateline")) {
            trainer = new DatelineTrainer();
        } else if (model.equals("medic")) {
            trainer = new MedicTrainer();
        } else if (model.equals("patient")) {
            trainer = new PatientTrainer();
        } else if (model.equals("organization")) {
            trainer = new OrganizationTrainer();
        } else if (model.equals("address")) {
            trainer = new AddressTrainer();
        } else if (model.equals("name-person-medical")) {
            trainer = new PersonNameMedicalTrainer();
        } else {
            throw new IllegalStateException("The model " + model + " is unknown.");
        }

        switch (mode) {
            case TRAIN:
                AbstractTrainer.runTraining(trainer);
                break;
            case EVAL:
                System.out.println(AbstractTrainer.runEvaluation(trainer));
                break;
            case SPLIT:
                System.out.println(AbstractTrainer.runSplitTrainingEvaluation(trainer, split));
                break;
            case EVAL_N_FOLD:
                if (numFolds == 0) {
                    throw new IllegalArgumentException("N should be > 0");
                }
                if (StringUtils.isNotEmpty(outputFilePath)) {
                    Path outputPath = Paths.get(outputFilePath);
                    if (Files.exists(outputPath)) {
                        System.err.println("Output file exists. ");
                    }
                } else {
                    String results = AbstractTrainer.runNFoldEvaluation(trainer, numFolds);
                    System.out.println(results);
                }
                break;
            default:
                throw new IllegalStateException("Invalid RunType: " + mode.name());
        }
        System.exit(0);
    }
}