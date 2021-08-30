package org.grobid.trainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.QuaeroDocument;
import org.grobid.core.data.QuaeroEntity;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorMedicalNER;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.MedicalReportConfiguration;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.trainer.sax.QuaeroCorpusSaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Create the French NER tagging model
 * <p>
 * Taken and adapted from grobid-ner (@author Patrice Lopez)
 * <p>
 * Tanti, 2021
 */
public class FrenchMedicalNERTrainer extends AbstractTrainer {

    private MedicalReportConfiguration medicalReportConfiguration = null;

    private static Logger LOGGER = LoggerFactory.getLogger(FrenchMedicalNERTrainer.class);

    protected Lexicon lexicon = Lexicon.getInstance();

    private String quaeroCorpusPath = null;

    private File tmpPath = null;

    public FrenchMedicalNERTrainer() {
        super(GrobidModels.FR_MEDICAL_NER);
    }

    public void setMedicalReportConfiguration(MedicalReportConfiguration config) {
        this.medicalReportConfiguration = config;
    }

    @Override
    /**
     * Add the selected features to the training data for the NER model
     */
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {
        return createCRFPPData(sourcePathLabel, outputPath, null, 1.0);
    }

    /**
     * Add the selected features to a NER example set
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
        int totalExamples = 0;
        try {
            System.out.println("sourcePathLabel: " + corpusDir);
            if (trainingOutputPath != null)
                System.out.println("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                System.out.println("outputPath for evaluation data: " + evalOutputPath);

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

            File corpusQuaeroDir = new File(quaeroCorpusPath);
            if (!corpusQuaeroDir.exists()) {
                LOGGER.warn("Directory does not exist: " + quaeroCorpusPath);
            }
            File[] files = corpusQuaeroDir.listFiles();
            if ((files == null) || (files.length == 0)) {
                LOGGER.warn("No files in directory: " + corpusQuaeroDir);
            }

            // process the core trainig set corresponding to LeMonde corpus first
            for (int i = 0; i < files.length; i++) {
                System.out.println(files[i].getName());
                if (files[i].getName().indexOf(".xml") != -1) {
                    totalExamples += processQuaero(files[i], writer2, writer3, splitRatio);
                }
            }

            if (writer2 != null) {
                writer2.close();
            }
            if (os2 != null) {
                os2.close();
            }

            if (writer3 != null) {
                writer3.close();
            }
            if (os3 != null) {
                os3.close();
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return totalExamples;
    }

    private int processQuaero(final File inputFile, Writer writerTraining, Writer writerEvaluation, double splitRatio) {
        int res = 0;
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot create the French NER medical lexicon from the Quaero French Medical Corpus, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            SAXParserFactory spf = SAXParserFactory.newInstance();
            QuaeroCorpusSaxHandler handler = new QuaeroCorpusSaxHandler();
            try {
                SAXParser sp = spf.newSAXParser();
                sp.parse(inputFile, handler);
                List<QuaeroDocument> documents = handler.getDocuments();

                for (int i = 0; i < documents.size(); i++) {
                    //System.out.println("Id : " + documents.get(i).getId() + "; Text : " + documents.get(i).getText());
                    List<QuaeroEntity> quaeroEntities = documents.get(i).getEntities();
                    for (QuaeroEntity entity : quaeroEntities) {
                        System.out.println("Type : " + entity.getType() + "; Text : " + entity.getText());
                    }
                }
            } catch (SAXException | ParserConfigurationException | IOException ie) {
                ie.printStackTrace();
            }

            /*Writer writer = new StringWriter();

            String output = writer.toString();
            String[] lines = output.split("\n");
            writer = null;

            // to store unit term positions
            List<OffsetPosition> locationPositions = null;
            List<OffsetPosition> personTitlePositions = null;
            List<OffsetPosition> organisationPositions = null;
            List<OffsetPosition> orgFormPositions = null;

            List<LayoutToken> tokens =new ArrayList<LayoutToken>();
            List<String> labels = new ArrayList<String>();
            String line = null;
            for(int i = 0; i<lines.length; i++) {
                line = lines[i].trim();
//System.out.println(line);
                // note that we work at sentence level
                if (line.startsWith("-DOCSTART-") || line.startsWith("-X-")) {
                    // the balance of data between training and evaluation is realised
                    // at document level
                    if ((writerTraining == null) && (writerEvaluation != null))
                        writer = writerEvaluation;
                    if ((writerTraining != null) && (writerEvaluation == null))
                        writer = writerTraining;
                    else {
                        if (Math.random() <= splitRatio)
                            writer = writerTraining;
                        else
                            writer = writerEvaluation;
                    }
                    continue;
                }

                // in the this line, we only keep what we are interested in for this model
                int ind = line.indexOf("\t");
                if (ind != -1) {
                    ind = line.indexOf("\t", ind + 1);
                    if (ind != -1)
                        line = line.substring(0, ind);
                }
                //System.out.println(line);

                if (line.trim().length() == 0 && tokens.size() > 0)  {

                    locationPositions = lexicon.tokenPositionsLocationNames(tokens);
                    personTitlePositions = lexicon.tokenPositionsPersonTitle(tokens);
                    organisationPositions = lexicon.tokenPositionsOrganisationNames(tokens);
                    orgFormPositions = lexicon.tokenPositionsOrgForm(tokens);
                    addFeaturesNER(line, labels, writer,
                        locationPositions, personTitlePositions, organisationPositions, orgFormPositions);
                    writer.write("\n");

                    tokens = new ArrayList<LayoutToken>();
                    labels = new ArrayList<String>();
                    res++;
                }
            }*/
        } catch (Exception ex) {
            throw new GrobidResourceException(
                "An exception occured when accessing/reading the Quaero French Medical Corpus files.", ex);

        } finally {
        }
        return res;
    }


    /**
     * Add the features for the NER model.
     */
    static public FeaturesVectorMedicalNER addFeaturesNER(String line,
                                                          boolean isLocationToken,
                                                          boolean isPersonTitleToken,
                                                          boolean isOrganisationToken,
                                                          boolean isOrgFormToken) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorMedicalNER featuresVectorMedicalNER = new FeaturesVectorMedicalNER();
        StringTokenizer st = new StringTokenizer(line, "\t ");
        if (st.hasMoreTokens()) {
            String word = st.nextToken();
            String label = null;
            if (st.hasMoreTokens())
                label = st.nextToken();

            featuresVectorMedicalNER.string = word;
            featuresVectorMedicalNER.label = label;

            if (word.length() == 1) {
                featuresVectorMedicalNER.singleChar = true;
            }

            if (featureFactory.test_all_capital(word))
                featuresVectorMedicalNER.capitalisation = "ALLCAPS";
            else if (featureFactory.test_first_capital(word))
                featuresVectorMedicalNER.capitalisation = "INITCAP";
            else
                featuresVectorMedicalNER.capitalisation = "NOCAPS";

            if (featureFactory.test_number(word))
                featuresVectorMedicalNER.digit = "ALLDIGIT";
            else if (featureFactory.test_digit(word))
                featuresVectorMedicalNER.digit = "CONTAINDIGIT";
            else
                featuresVectorMedicalNER.digit = "NODIGIT";

            Matcher m0 = featureFactory.isPunct.matcher(word);
            if (m0.find()) {
                featuresVectorMedicalNER.punctType = "PUNCT";
            }
            if ((word.equals("(")) | (word.equals("["))) {
                featuresVectorMedicalNER.punctType = "OPENBRACKET";
            } else if ((word.equals(")")) | (word.equals("]"))) {
                featuresVectorMedicalNER.punctType = "ENDBRACKET";
            } else if (word.equals(".")) {
                featuresVectorMedicalNER.punctType = "DOT";
            } else if (word.equals(",")) {
                featuresVectorMedicalNER.punctType = "COMMA";
            } else if (word.equals("-")) {
                featuresVectorMedicalNER.punctType = "HYPHEN";
            } else if (word.equals("\"") | word.equals("\'") | word.equals("`")) {
                featuresVectorMedicalNER.punctType = "QUOTE";
            }

            if (featuresVectorMedicalNER.capitalisation == null)
                featuresVectorMedicalNER.capitalisation = "NOCAPS";

            if (featuresVectorMedicalNER.digit == null)
                featuresVectorMedicalNER.digit = "NODIGIT";

            if (featuresVectorMedicalNER.punctType == null)
                featuresVectorMedicalNER.punctType = "NOPUNCT";

            Matcher m2 = featureFactory.year.matcher(word);
            if (m2.find()) {
                featuresVectorMedicalNER.year = true;
            }

            if (featureFactory.test_common(word)) {
                featuresVectorMedicalNER.commonName = true;
            }

            if (featureFactory.test_first_names(word)) {
                featuresVectorMedicalNER.firstName = true;
            }

            if (featureFactory.test_last_names(word)) {
                featuresVectorMedicalNER.lastName = true;
            }

            if (featureFactory.test_month(word)) {
                featuresVectorMedicalNER.month = true;
            }

            if (featureFactory.test_city(word)) {
                featuresVectorMedicalNER.cityName = true;
            }

            if (featureFactory.test_country(word)) {
                featuresVectorMedicalNER.countryName = true;
            }

            featuresVectorMedicalNER.isLocationToken = isLocationToken;

            featuresVectorMedicalNER.isPersonTitleToken = isPersonTitleToken;

            featuresVectorMedicalNER.isOrganisationToken = isOrganisationToken;

            featuresVectorMedicalNER.isOrgFormToken = isOrgFormToken;

            featuresVectorMedicalNER.shadowNumber = TextUtilities.shadowNumbers(word);

            featuresVectorMedicalNER.wordShape = TextUtilities.wordShape(word);

            featuresVectorMedicalNER.wordShapeTrimmed = TextUtilities.wordShapeTrimmed(word);

            // To be done : other lexicon information

        }

        return featuresVectorMedicalNER;
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
        } catch (Exception e) {
            System.err.println("The config file does not appear valid, see resources/config/grobid-medical-report.yaml");
        }
        try {
            String pGrobidHome = medicalReportConfiguration.getGrobidHome();

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);

            System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getInstance().getGrobidHome());
        } catch (final Exception exp) {
            System.err.println("grobid-medical-report initialisation failed: " + exp);
            exp.printStackTrace();
        }

        FrenchMedicalNERTrainer trainer = new FrenchMedicalNERTrainer();

        AbstractTrainer.runTraining(trainer);
        System.out.println(AbstractTrainer.runEvaluation(trainer));

        System.exit(0);
    }
}