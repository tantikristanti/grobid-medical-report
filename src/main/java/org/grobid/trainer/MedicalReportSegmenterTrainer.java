package org.grobid.trainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.MedicalReportConfiguration;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEIMedicalSaxParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A class to train and evaluate medical report segmenter model.
 *
 * Tanti, 2020
 */
public class MedicalReportSegmenterTrainer extends AbstractTrainer {

    private MedicalReportConfiguration medicalReportConfiguration = null;

    public MedicalReportSegmenterTrainer() {
        super(GrobidModels.MEDICAL_REPORT_SEGMENTER);
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesMedical(corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            outputFile, null, 1.0);
    }

    public void setMedicalReportConfiguration(MedicalReportConfiguration config) {
        this.medicalReportConfiguration = config;
    }

    /**
     * Add the selected features for the medical model
     *
     * @param corpusDir          path where corpus files are located
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
        return addFeaturesMedical(corpusDir.getAbsolutePath() + "/tei",
            corpusDir.getAbsolutePath() + "/raw",
            trainingOutputPath,
            evalOutputPath,
            splitRatio);
    }

    /**
     * Add the selected features for the medical model
     *
     * @param sourceTEIPathLabel path to corpus TEI files
     * @param sourceRawPathLabel path to corpus raw files
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return number of examples
     */
    public int addFeaturesMedical(String sourceTEIPathLabel,
                                  String sourceRawPathLabel,
                                  final File trainingOutputPath,
                                  final File evalOutputPath,
                                  double splitRatio) {
        int totalExamples = 0;
        try {
            System.out.println("TEI Path Label: " + sourceTEIPathLabel);
            System.out.println("Raw Path Label: " + sourceRawPathLabel);
            System.out.println("Training Output Path: " + trainingOutputPath);
            System.out.println("Eval Output Path: " + evalOutputPath);

            // we need first to generate the labeled files from the TEI annotated files
            File input = new File(sourceTEIPathLabel);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei.xml") || name.endsWith(".tei");
                }
            });

            if (refFiles == null) {
                return 0;
            }

            System.out.println(refFiles.length + " tei files");

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

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            for (File tf : refFiles) {
                String name = tf.getName();
                LOGGER.info("Processing: " + name);

                TEIMedicalSaxParser parser2 = new TEIMedicalSaxParser();

                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser2);

                List<String> labeled = parser2.getLabeledResult();

                // we can now add the features
                // we open the featured file
                try {
                    File theRawFile = new File(sourceRawPathLabel + File.separator + name.replace(".tei.xml", ""));
                    if (!theRawFile.exists()) {
                        LOGGER.error("The raw file does not exist: " + theRawFile.getPath());
                        continue;
                    }

                    int q = 0;
                    BufferedReader bis = new BufferedReader(
                        new InputStreamReader(new FileInputStream(theRawFile), "UTF8"));
                    StringBuilder medical = new StringBuilder();
                    String line = null;
                    int l = 0;
                    String previousTag = null;
                    int nbInvalid = 0;
                    while ((line = bis.readLine()) != null) {
                        l++;
                        int ii = line.indexOf(' ');
                        String token = null;
                        if (ii != -1) {
                            token = line.substring(0, ii);
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been gnerated by a recent version of grobid
                            token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                        }
                        // we get the label in the labelled data file for the same token
                        for (int pp = q; pp < labeled.size(); pp++) {
                            String localLine = labeled.get(pp);
                            StringTokenizer st = new StringTokenizer(localLine, " \t");
                            if (st.hasMoreTokens()) {
                                String localToken = st.nextToken();
                                // unicode normalisation of the token - it should not be necessary if the training data
                                // has been gnerated by a recent version of grobid
                                localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                                if (localToken.equals(token)) {
                                    String tag = st.nextToken();
                                    medical.append(line).append(" ").append(tag);
                                    previousTag = tag;
                                    q = pp + 1;
                                    nbInvalid = 0;
                                    //pp = q + 10;
                                    break;
                                }
                            }
                            if (pp - q > 5) {
                                //LOGGER.warn(name + " / Medical trainer: TEI and raw file unsynchronized at raw line " + l + " : " + localLine);
                                nbInvalid++;
                                // let's reuse the latest tag
                                if (previousTag != null)
                                    medical.append(line).append(" ").append(previousTag);
                                break;
                            }
                        }
                        if (nbInvalid > 20) {
                            // too many consecutive synchronization issues
                            break;
                        }
                    }
                    bis.close();
                    if (nbInvalid < 10) {
                        if ((writer2 == null) && (writer3 != null))
                            writer3.write(medical.toString() + "\n");
                        if ((writer2 != null) && (writer3 == null))
                            writer2.write(medical.toString() + "\n");
                        else {
                            if (Math.random() <= splitRatio)
                                writer2.write(medical.toString() + "\n");
                            else
                                writer3.write(medical.toString() + "\n");
                        }
                    } else {
                        LOGGER.warn(name + " / too many synchronization issues, file not used in training data and to be fixed!");
                    }
                } catch (Exception e) {
                    LOGGER.error("Fail to open or process raw file", e);
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
        return totalExamples;
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        MedicalReportConfiguration medicalReportConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            medicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), MedicalReportConfiguration.class);
        } catch(Exception e) {
            System.err.println("The config file does not appear valid, see resources/config/grobid-medical-report.yaml");
        }
        try {
            String pGrobidHome = medicalReportConfiguration.getGrobidHome();

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);

            System.out.println(">>>>>>>> GROBID_HOME="+GrobidProperties.getInstance().getGrobidHome());
        } catch (final Exception exp) {
            System.err.println("grobid-medical-report initialisation failed: " + exp);
            exp.printStackTrace();
        }

        MedicalReportSegmenterTrainer trainer = new MedicalReportSegmenterTrainer();

        AbstractTrainer.runTraining(trainer);
        System.out.println(AbstractTrainer.runEvaluation(trainer));

        System.exit(0);
    }
}