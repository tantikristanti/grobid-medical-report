package org.grobid.trainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.GrobidModels;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.GrobidMedicalReportConfiguration;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEIFullMedicalTextSaxParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The class for training body part of medical reports.
 * This class is taken and adapted from the class FulltextTrainer class (@author Patrice Lopez)
 * <p>
 * Tanti, 2021
 */
public class FullMedicalTextTrainer extends AbstractTrainer {

    public FullMedicalTextTrainer() {
        super(GrobidModels.FULL_MEDICAL_TEXT);
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesFullText(corpusPath.getAbsolutePath() + "/tei",
            corpusPath + "/raw", outputFile, null, 1.0);
    }

    /**
     * Add the selected features to a full text example set
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
        return addFeaturesFullText(corpusDir.getAbsolutePath() + "/tei",
            corpusDir.getAbsolutePath() + "/raw",
            trainingOutputPath,
            evalOutputPath,
            splitRatio);
    }

    public int addFeaturesFullText(String sourceTEIPathLabel,
                                   String sourceRawPathLabel,
                                   final File trainingOutputPath,
                                   final File evalOutputPath,
                                   double splitRatio) {
        int totalExamples = 0;
        try {
            System.out.println("sourceTEIPathLabel: " + sourceTEIPathLabel);
            System.out.println("sourceRawPathLabel: " + sourceRawPathLabel);
            System.out.println("trainingOutputPath: " + trainingOutputPath);
            System.out.println("evalOutputPath: " + evalOutputPath);

            // we need first to generate the labeled files from the TEI annotated files
            File input = new File(sourceTEIPathLabel);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei.xml");
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

                TEIFullMedicalTextSaxParser parser2 = new TEIFullMedicalTextSaxParser();

                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser2);

                List<String> labeled = parser2.getLabeledResult();

                // we can now (try to) add the features
                // we open the featured file
                try {
                    File rawFile = new File(sourceRawPathLabel + File.separator +
                        name.replace(".tei.xml", ""));
                    if (!rawFile.exists()) {
                        LOGGER.error("The raw file does not exist: " + rawFile.getPath());
                        continue;
                    }

                    BufferedReader bis = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                            rawFile), "UTF8"));
                    int q = 0; // current position in the TEI labeled list
                    StringBuilder fulltext = new StringBuilder();

                    String line;
                    int l = 0;
                    String previousTag = null;
                    int nbInvalid = 0;
                    while ((line = bis.readLine()) != null) {
                        if (line.trim().length() == 0)
                            continue;
                        // we could apply here some more check on the wellformedness of the line
                        //fulltext.append(line);
                        l++;
                        int ii = line.indexOf(' ');
                        String token = null;
                        if (ii != -1) {
                            token = line.substring(0, ii);
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been generated by a recent version of grobid
                            token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                        }
                        //                    boolean found = false;
                        // we get the label in the labelled data file for the same token
                        for (int pp = q; pp < labeled.size(); pp++) {
                            String localLine = labeled.get(pp);
                            StringTokenizer st = new StringTokenizer(localLine, " ");
                            if (st.hasMoreTokens()) {
                                String localToken = st.nextToken();
                                // unicode normalisation of the token - it should not be necessary if the training data
                                // has been generated by a recent version of grobid
                                localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);

                                if (localToken.equals(token)) {
                                    String tag = st.nextToken();
                                    fulltext.append(line).append(" ").append(tag);
                                    previousTag = tag;
                                    q = pp + 1;
                                    nbInvalid = 0;
                                    //pp = q + 10;
                                    break;
                                }
                            }
                            if (pp - q > 5) {
                                LOGGER.warn(name + " / Fulltext trainer: TEI and raw file unsynchronized at raw line " + l + " : " + localLine);
                                nbInvalid++;
                                // let's reuse the latest tag
                                if (previousTag != null)
                                    fulltext.append(line).append(" ").append(previousTag);
                                break;
                            }
                        }
                        if (nbInvalid > 20) {
                            // too many consecutive synchronization issues
                            break;
                        }
                    }

                    bis.close();

                    // format with features for sequence tagging...
                    if (nbInvalid < 10) {
                        if ((writer2 == null) && (writer3 != null))
                            writer3.write(fulltext.toString() + "\n");
                        if ((writer2 != null) && (writer3 == null))
                            writer2.write(fulltext.toString() + "\n");
                        else {
                            if (Math.random() <= splitRatio)
                                writer2.write(fulltext.toString() + "\n");
                            else
                                writer3.write(fulltext.toString() + "\n");
                        }
                        totalExamples++;
                    } else {
                        LOGGER.error(name + " / too many synchronization issues, file not used in training data and to be fixed!");
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
            LOGGER.error("An exception occurred while running Grobid.", e);
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
        GrobidMedicalReportConfiguration grobidMedicalReportConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            grobidMedicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), GrobidMedicalReportConfiguration.class);
        } catch (Exception e) {
            System.err.println("The config file does not appear valid, see resources/config/grobid-medical-report.yaml");
        }
        try {
            String pGrobidHome = grobidMedicalReportConfiguration.getGrobidHome();

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);

            System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getInstance().getGrobidHome());
        } catch (final Exception exp) {
            System.err.println("grobid-medical-report initialisation failed: " + exp);
            exp.printStackTrace();
        }

        FullMedicalTextTrainer trainer = new FullMedicalTextTrainer();

        AbstractTrainer.runTraining(trainer);
        System.out.println(AbstractTrainer.runEvaluation(trainer));

        System.exit(0);
    }
}	