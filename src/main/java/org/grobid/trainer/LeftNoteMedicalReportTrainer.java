package org.grobid.trainer;

import org.grobid.core.GrobidMedicalReportModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.MedicalReportProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.evaluation.EvaluationUtilities;
import org.grobid.trainer.sax.TEILeftNoteMedicalSaxParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;


/**
 * A class to train and evaluate a model of left-note part of medical reports.
 * The goal is to recognize some information, for instance person (medical staffs, patients) names,
 * affiliation, addresss, phone, etc.
 *
 * @ Tanti, 2020
 */
public class LeftNoteMedicalReportTrainer extends AbstractTrainer {

    public LeftNoteMedicalReportTrainer() {
        super(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT);

        // adjusting CRF training parameters for this model (only with Wapiti)
        epsilon = 0.000001;
        window = 30;
        nbMaxIterations = 1500;
    }


    @Override
    public int createCRFPPData(File corpusPath, File trainingOutputPath) {
        return addFeaturesLeftNotes(corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            trainingOutputPath, null, 1.0);
    }

    /**
     * Add the selected features to a left-note example set
     *
     * @param corpusDir          a path where corpus files are located
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return the total number of used corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        return addFeaturesLeftNotes(corpusDir.getAbsolutePath() + "/tei",
            corpusDir.getAbsolutePath() + "/raw",
            trainingOutputPath,
            evalOutputPath,
            splitRatio);
    }

    /**
     * Add the selected features to the left-note model training
     *
     * @param sourceFile         source path
     * @param leftNotePath       leftNote path
     * @param trainingOutputPath output training file
     * @return number of corpus files
     */
    public int addFeaturesLeftNotes(String sourceFile,
                                    String leftNotePath,
                                    final File trainingOutputPath,
                                    final File evalOutputPath,
                                    double splitRatio) {
        System.out.println(sourceFile);
        System.out.println(leftNotePath);
        System.out.println(trainingOutputPath);
        System.out.println(evalOutputPath);

        System.out.println("TEI files: " + sourceFile);
        System.out.println("Left-note info files: " + leftNotePath);
        if (trainingOutputPath != null)
            System.out.println("outputPath for training data: " + trainingOutputPath);
        if (evalOutputPath != null)
            System.out.println("outputPath for evaluation data: " + evalOutputPath);

        int nbExamples = 0;
        try {
            File pathh = new File(sourceFile);
            // we process all tei files in the output directory
            File[] refFiles = pathh.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei") | name.endsWith(".tei.xml");
                }
            });

            if (refFiles == null)
                return 0;

            nbExamples = refFiles.length;
            System.out.println(nbExamples + " tei files");

            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, "UTF8");
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, "UTF8");
            }

            for (File teifile : refFiles) {
                String name = teifile.getName();
                //System.out.println(name);

                TEILeftNoteMedicalSaxParser parser2 = new TEILeftNoteMedicalSaxParser();
                parser2.setFileName(name);

                // get a factory
                SAXParserFactory spf = SAXParserFactory.newInstance();
                //get a new instance of parser
                SAXParser par = spf.newSAXParser();
                par.parse(teifile, parser2);

                ArrayList<String> labeled = parser2.getLabeledResult();

                //System.out.println(labeled);
                //System.out.println(parser2.getPDFName()+"._");

                File refDir2 = new File(leftNotePath);
                String leftNoteFile = null;
                File[] refFiles2 = refDir2.listFiles();
                for (File aRefFiles2 : refFiles2) {
                    String localFileName = aRefFiles2.getName();
                    if (localFileName.equals(parser2.getPDFName() + ".left.note.medical") ||
                        localFileName.equals(parser2.getPDFName() + ".training.left.note.medical")) {
                        leftNoteFile = localFileName;
                        break;
                    }
                    if ((localFileName.startsWith(parser2.getPDFName() + "._")) &&
                        (localFileName.endsWith(".left.note.medical") || localFileName.endsWith(".training.left.note.medical"))) {
                        leftNoteFile = localFileName;
                        break;
                    }
                }

                if (leftNoteFile == null)
                    continue;

                String pathLeftNote = leftNotePath + File.separator + leftNoteFile;
                int p = 0;
                BufferedReader bis = new BufferedReader(
                    new InputStreamReader(new FileInputStream(pathLeftNote), "UTF8"));

                StringBuilder leftNote = new StringBuilder();

                String line;
                while ((line = bis.readLine()) != null) {
                    leftNote.append(line);
                    int ii = line.indexOf(' ');
                    String token = null;
                    if (ii != -1) {
                        token = line.substring(0, ii);
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been gnerated by a recent version of grobid
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    }

                    // we get the label in the labelled data file for the same token
                    for (int pp = p; pp < labeled.size(); pp++) {
                        String localLine = labeled.get(pp);
                        StringTokenizer st = new StringTokenizer(localLine, " ");
                        if (st.hasMoreTokens()) {
                            String localToken = st.nextToken();
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been gnerated by a recent version of grobid
                            localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);

                            if (localToken.equals(token)) {
                                String tag = st.nextToken();
                                leftNote.append(" ").append(tag);
                                p = pp + 1;
                                pp = p + 10;
                            } /*else {
                                System.out.println("feature:"+token + " / tei:" + localToken);
                            }*/
                        }
                        if (pp - p > 5) {
                            break;
                        }
                    }
                    leftNote.append("\n");
                }
                bis.close();

                // post process for ensuring continous labelling
                StringBuilder leftNote2 = new StringBuilder();
                String leftNoteStr = leftNote.toString();
                StringTokenizer sto = new StringTokenizer(leftNoteStr, "\n");
                String lastLabel = null;
                String lastLastLabel = null;
                String previousLine = null;

                while (sto.hasMoreTokens()) {
                    String linee = sto.nextToken();
                    StringTokenizer sto2 = new StringTokenizer(linee, " ");
                    String label = null;
                    while (sto2.hasMoreTokens()) {
                        label = sto2.nextToken();
                    }
                    if (label != null) {
                        if (label.length() > 0) {
                            if (!((label.charAt(0) == '<') | (label.startsWith("I-<")))) {
                                label = null;
                            }
                        }
                    }

                    if (previousLine != null) {
                        if ((label != null) & (lastLabel == null) & (lastLastLabel != null)) {
                            if (label.equals(lastLastLabel)) {
                                lastLabel = label;
                                previousLine += " " + label;
                                leftNote2.append(previousLine);
                                leftNote2.append("\n");
                            } else {
                                //if (lastLabel == null)
                                //	previousLine += " <note>";
                                if (lastLabel != null) {
                                    leftNote2.append(previousLine);
                                    leftNote2.append("\n");
                                }
                            }
                        } else {
                            //if (lastLabel == null)
                            //	previousLine += " <note>";
                            if (lastLabel != null) {
                                leftNote2.append(previousLine);
                                leftNote2.append("\n");
                            }
                        }
                    }

//                    previousPreviousLine = previousLine;
                    previousLine = linee;

                    lastLastLabel = lastLabel;
                    lastLabel = label;
                }

                if (lastLabel != null) {
                    leftNote2.append(previousLine);
                    leftNote2.append("\n");
                }

                if ((writer2 == null) && (writer3 != null))
                    writer3.write(leftNote2.toString() + "\n");
                if ((writer2 != null) && (writer3 == null))
                    writer2.write(leftNote2.toString() + "\n");
                else {
                    if (Math.random() <= splitRatio)
                        writer2.write(leftNote2.toString() + "\n");
                    else
                        writer3.write(leftNote2.toString() + "\n");
                }
            }

            if (writer2 != null) {
                writer2.close();
                os2.close();
            }

            if (writer3 != null) {
                writer3.close();
                os3.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return nbExamples;
    }

    /**
     * Standard evaluation via the the usual Grobid evaluation framework.
     */
    public String evaluate() {
        File evalDataF = GrobidProperties.getInstance().getEvalCorpusPath(
            new File(new File("resources").getAbsolutePath()), model);

        File tmpEvalPath = getTempEvaluationDataPath();
        createCRFPPData(evalDataF, tmpEvalPath);

        return EvaluationUtilities.evaluateStandard(tmpEvalPath.getAbsolutePath(), getTagger()).toString();
    }

    public String splitTrainEvaluate(Double split, boolean random) {
        System.out.println("Paths :\n" + getCorpusPath() + "\n" + GrobidProperties.getModelPath(model).getAbsolutePath() + "\n" + getTempTrainingDataPath().getAbsolutePath() + "\n" + getTempEvaluationDataPath().getAbsolutePath() + " \nrand " + random);

        File trainDataPath = getTempTrainingDataPath();
        File evalDataPath = getTempEvaluationDataPath();

        final File dataPath = trainDataPath;
        createCRFPPData(getCorpusPath(), dataPath, evalDataPath, split);
        GenericTrainer trainer = TrainerFactory.getTrainer();

        if (epsilon != 0.0)
            trainer.setEpsilon(epsilon);
        if (window != 0)
            trainer.setWindow(window);
        if (nbMaxIterations != 0)
            trainer.setNbMaxIterations(nbMaxIterations);

        final File tempModelPath = new File(GrobidProperties.getModelPath(model).getAbsolutePath() + NEW_MODEL_EXT);
        final File oldModelPath = GrobidProperties.getModelPath(model);

        trainer.train(getTemplatePath(), dataPath, tempModelPath, GrobidProperties.getNBThreads(), model);

        // if we are here, that means that training succeeded
        renameModels(oldModelPath, tempModelPath);

        return EvaluationUtilities.evaluateStandard(evalDataPath.getAbsolutePath(), getTagger()).toString();
    }

    protected final File getCorpusPath() {
        return new File(MedicalReportProperties.get("grobid.full.medical.text.corpusPath"));
    }

    protected final File getTemplatePath() {
        return new File(MedicalReportProperties.get("grobid.full.medical.text.templatePath"));
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        /*GrobidProperties.getInstance();
        AbstractTrainer.runTraining(new LeftNoteMedicalReportTrainer());
        System.out.println(AbstractTrainer.runEvaluation(new LeftNoteMedicalReportTrainer()));
        System.exit(0);*/

        try {
            String pGrobidHome = MedicalReportProperties.get("grobid.home");

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);

            System.out.println("GROBID_HOME Path =" + GrobidProperties.get_GROBID_HOME_PATH());
        } catch (final Exception exp) {
            System.err.println("GROBID-medical-report initialisation failed: " + exp);
            exp.printStackTrace();
        }

        Trainer trainer = new LeftNoteMedicalReportTrainer();
        AbstractTrainer.runTraining(trainer);
        AbstractTrainer.runEvaluation(trainer);
    }
}