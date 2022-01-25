package org.grobid.trainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.MedicalReportConfiguration;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEIOrganizationSaxParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;


/**
 * A class to train and evaluate an organization model.
 * The goal is to recognize some information, for instance person (medical staffs, patients) names,
 * affiliation, addresss, phone, etc.
 *
 * @ Tanti, 2020
 */
public class OrganizationTrainer extends AbstractTrainer{

    public OrganizationTrainer() {
        super(GrobidModels.ORGANIZATION);
    }

    @Override
    public int createCRFPPData(File corpusPath, File trainingOutputPath) {
        return addFeaturesOrganization(corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            trainingOutputPath, null, 1.0);
    }

    /**
     * Add the selected features to a organization example set
     *
     * @param corpusDir
     *            a path where corpus files are located
     * @param trainingOutputPath
     *            path where to store the temporary training data
     * @param evalOutputPath
     *            path where to store the temporary evaluation data
     * @param splitRatio
     *            ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return the total number of used corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        return addFeaturesOrganization(corpusDir.getAbsolutePath() + "/tei",
            corpusDir.getAbsolutePath() + "/raw",
            trainingOutputPath,
            evalOutputPath,
            splitRatio);
    }

    /**
     * Add the selected features to the organization model training
     * @param sourceFile source path
     * @param organizationPath organization path
     * @param trainingOutputPath output training file
     * @return number of corpus files
     */
    public int addFeaturesOrganization(String sourceFile,
                                  String organizationPath,
                                  final File trainingOutputPath,
                                  final File evalOutputPath,
                                  double splitRatio) {
        System.out.println(sourceFile);
        System.out.println(organizationPath);
        System.out.println(trainingOutputPath);
        System.out.println(evalOutputPath);

        System.out.println("TEI files: " + sourceFile);
        System.out.println("Organization info files: " + organizationPath);
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

                TEIOrganizationSaxParser parser2 = new TEIOrganizationSaxParser();
                parser2.setFileName(name);

                // get a factory
                SAXParserFactory spf = SAXParserFactory.newInstance();
                //get a new instance of parser
                SAXParser par = spf.newSAXParser();
                par.parse(teifile, parser2);

                ArrayList<String> labeled = parser2.getLabeledResult();

                //System.out.println(labeled);
                //System.out.println(parser2.getPDFName()+"._");

                File refDir2 = new File(organizationPath);
                String organizationFile = null;
                File[] refFiles2 = refDir2.listFiles();
                for (File aRefFiles2 : refFiles2) {
                    String localFileName = aRefFiles2.getName();
                    if (localFileName.equals(parser2.getPDFName() + ".organization.medical") ||
                        localFileName.equals(parser2.getPDFName() + ".training.organization.medical")) {
                        organizationFile = localFileName;
                        break;
                    }
                    if ((localFileName.startsWith(parser2.getPDFName() + "._")) &&
                        (localFileName.endsWith(".organization.medical") || localFileName.endsWith(".training.organization.medical"))) {
                        organizationFile = localFileName;
                        break;
                    }
                }

                if (organizationFile == null)
                    continue;

                String pathOrganization = organizationPath + File.separator + organizationFile;
                int p = 0;
                BufferedReader bis = new BufferedReader(
                    new InputStreamReader(new FileInputStream(pathOrganization), "UTF8"));

                StringBuilder organization = new StringBuilder();

                String line;
                while ((line = bis.readLine()) != null) {
                    organization.append(line);
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

                                 /* anonymization of sensitive information (ex. person's name)
                                    With the reason that in the segmentation model,
                                    there is no named entity recognition since the purpose is for segmenting the document segmentation,
                                    so, to help identify the entity type of each token for anonymization purposes, we use grobid-ner.
                                     */
                                /*String[] splitLine  = line.split(" ");
                                // so we anonymize all tokens
                                splitLine[0] = "Anonym";
                                splitLine[1] = splitLine[0].toLowerCase();
                                line = String.join(" ", splitLine);
                                organization.append(line);*/

                                String tag = st.nextToken();
                                organization.append(" ").append(tag);
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
                    organization.append("\n");
                }
                bis.close();

                // post process for ensuring continous labelling
                StringBuilder organization2 = new StringBuilder();
                String organizationStr = organization.toString();
                StringTokenizer sto = new StringTokenizer(organizationStr, "\n");
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
                                organization2.append(previousLine);
                                organization2.append("\n");
                            } else {
                                //if (lastLabel == null)
                                //	previousLine += " <note>";
                                if (lastLabel != null) {
                                    organization2.append(previousLine);
                                    organization2.append("\n");
                                }
                            }
                        } else {
                            //if (lastLabel == null)
                            //	previousLine += " <note>";
                            if (lastLabel != null) {
                                organization2.append(previousLine);
                                organization2.append("\n");
                            }
                        }
                    }

//                    previousPreviousLine = previousLine;
                    previousLine = linee;

                    lastLastLabel = lastLabel;
                    lastLabel = label;
                }

                if (lastLabel != null) {
                    organization2.append(previousLine);
                    organization2.append("\n");
                }

                if ( (writer2 == null) && (writer3 != null) )
                    writer3.write(organization2.toString() + "\n");
                if ( (writer2 != null) && (writer3 == null) )
                    writer2.write(organization2.toString() + "\n");
                else {
                    if (Math.random() <= splitRatio)
                        writer2.write(organization2.toString() + "\n");
                    else
                        writer3.write(organization2.toString() + "\n");
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

        OrganizationTrainer trainer = new OrganizationTrainer();

        AbstractTrainer.runTraining(trainer);
        System.out.println(AbstractTrainer.runEvaluation(trainer));

        System.exit(0);
    }
}