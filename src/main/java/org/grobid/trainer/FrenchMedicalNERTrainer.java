package org.grobid.trainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorMedicalNER;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.lexicon.MedicalNERLexicon;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.*;
import org.grobid.trainer.sax.FrenchCorpusSaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.stylesheets.LinkStyle;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
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

    private static Logger LOGGER = LoggerFactory.getLogger(FrenchMedicalNERTrainer.class);
    protected MedicalNERLexicon medicalNERLexicon = MedicalNERLexicon.getInstance();
    protected Lexicon lexicon = Lexicon.getInstance();

    private String quaeroCorpusPath = null;

    private File tmpPath = null;

    public FrenchMedicalNERTrainer() {
        // if we train the French Quaero Corpus
        //super(GrobidModels.FR_MEDICAL_NER_QUAERO);

        super(GrobidModels.FR_MEDICAL_NER);
    }


    @Override
    /**
     * Add the selected features to the training data for the NER model
     */
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {
        return createCRFPPData(sourcePathLabel, outputPath, null, 1.0);
    }

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

            File corpusTEI = new File(corpusDir.getAbsolutePath().toString() + "/tei");
            if (!corpusTEI.exists()) {
                LOGGER.warn("Directory does not exist: " + corpusTEI.getAbsolutePath());
            }
            File[] files = corpusTEI.listFiles();
            if ((files == null) || (files.length == 0)) {
                LOGGER.warn("No TEI files in directory: " + corpusTEI);
            }

            // process the core trainig set corresponding to LeMonde corpus first
            for (int i = 0; i < files.length; i++) {
                System.out.println(files[i].getName());
                if (files[i].getName().indexOf(".xml") != -1)
                    totalExamples += process(files[i], writer2, writer3, splitRatio);
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
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return totalExamples;
    }

    private int process(final File inputFile, Writer writerTraining, Writer writerEvaluation, double splitRatio) {
        int res = 0;
        try {
            System.out.println("Path to French corpus training: " + inputFile.getPath());
            if (!inputFile.exists()) {
                throw new
                    GrobidException("Cannot start training, because corpus resource file is not correctly set : "
                    + inputFile.getPath());
            }

            String name = inputFile.getName();

            FrenchCorpusSaxHandler parser2 = new FrenchCorpusSaxHandler();
            parser2.setFileName(name);

            // get a factory
            SAXParserFactory spf = SAXParserFactory.newInstance();
            //get a new instance of parser
            SAXParser par = spf.newSAXParser();
            par.parse(inputFile, parser2);

            ArrayList<String> labeled = parser2.getLabeledResult();

            for (int i = 0; i < labeled.size(); i++) {
                String localLine = labeled.get(i);
                StringTokenizer st = new StringTokenizer(localLine, " ");

                // to store unit term positions
                List<OffsetPosition> locationPositions = null;
                List<OffsetPosition> personTitlePositions = null;
                List<OffsetPosition> organisationPositions = null;
                List<OffsetPosition> orgFormPositions = null;
                
                boolean isLocationToken = false;
                boolean isPersonTitleToken = false;
                boolean isOrganisationToken = false;
                boolean isOrgFormToken = false;
                if (st.hasMoreTokens()) {
                    String localToken = st.nextToken();
                    // unicode normalisation of the token - it should not be necessary if the training data
                    // has been gnerated by a recent version of grobid
                    localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                    String tag = st.nextToken();

                    // add features for the token
                    locationPositions = medicalNERLexicon.tokenPositionsGeographicNames(localToken);
                    if(locationPositions != null && locationPositions.size() > 0){
                        isLocationToken = true;
                    }

                    personTitlePositions = lexicon.tokenPositionsPersonTitle(localToken);
                    if(personTitlePositions != null && personTitlePositions.size() > 0){
                        isPersonTitleToken = true;
                    }

                    organisationPositions = lexicon.tokenPositionsOrganisationNames(localToken);
                    if(organisationPositions != null && organisationPositions.size() > 0){
                        isOrganisationToken = true;
                    }

                    orgFormPositions = lexicon.tokenPositionsOrgForm(localToken);
                    if(orgFormPositions != null && orgFormPositions.size() > 0){
                        isOrgFormToken = true;
                    }

                    // the "line" expected by the method FeaturesVectorNER.addFeaturesNER is the token
                    // followed by the label, separated by a space, and nothing else
                    FeaturesVectorMedicalNER featuresVector = FeaturesVectorMedicalNER.addFeaturesNER(localLine, isLocationToken, isPersonTitleToken, isOrganisationToken, isOrgFormToken);
                    writerTraining.write(featuresVector.printVector() + "\n");

                }
            }
            writerTraining.close();
        }
        catch (Exception ex) {
            throw new GrobidResourceException(
                "An exception occurred when accessing/reading the French corpus.", ex);

        }finally {
        }
        return res;
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