package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEIHeaderMedicalSaxParser;
import org.grobid.utility.Utility;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * A class to train and evaluate a model of header part of medical reports.
 * The goal is to recognize some information, for instance person (medical staffs, patients) names,
 * affiliation, addresss, phone, etc.
 *
 * @ Tanti, 2020
 */
public class HeaderMedicalReportTrainer extends AbstractTrainer {

    public HeaderMedicalReportTrainer() {
        super(GrobidModels.HEADER_MEDICAL_REPORT);
    }

    @Override
    public int createCRFPPData(File corpusPath, File trainingOutputPath) {
        return addFeaturesHeaders(corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            trainingOutputPath, null, 1.0);
    }

    /**
     * Add the selected features to a header example set
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
        return addFeaturesHeaders(corpusDir.getAbsolutePath() + "/tei",
            corpusDir.getAbsolutePath() + "/raw",
            trainingOutputPath,
            evalOutputPath,
            splitRatio);
    }

    /**
     * Add the selected features to the header model training
     *
     * @param sourceFile         source path
     * @param headerPath         header path
     * @param trainingOutputPath output training file
     * @return number of corpus files
     */
    public int addFeaturesHeaders(String sourceFile,
                                  String headerPath,
                                  final File trainingOutputPath,
                                  final File evalOutputPath,
                                  double splitRatio) {
        System.out.println(sourceFile);
        System.out.println(headerPath);
        System.out.println(trainingOutputPath);
        System.out.println(evalOutputPath);

        System.out.println("TEI files: " + sourceFile);
        System.out.println("header info files: " + headerPath);
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

                TEIHeaderMedicalSaxParser parser2 = new TEIHeaderMedicalSaxParser();
                parser2.setFileName(name);

                // get a factory
                SAXParserFactory spf = SAXParserFactory.newInstance();
                //get a new instance of parser
                SAXParser par = spf.newSAXParser();
                par.parse(teifile, parser2);

                ArrayList<String> labeled = parser2.getLabeledResult();

                File refDir2 = new File(headerPath);
                String headerFile = null;
                File[] refFiles2 = refDir2.listFiles();
                for (File aRefFiles2 : refFiles2) {
                    String localFileName = aRefFiles2.getName();
                    if (localFileName.equals(parser2.getPDFName() + ".header.medical") ||
                        localFileName.equals(parser2.getPDFName() + ".training.header.medical") ||
                        localFileName.equals(parser2.getPDFName() + ".anonym.training.header.medical")) {
                        headerFile = localFileName;
                        break;
                    }
                    if ((localFileName.startsWith(parser2.getPDFName() + "._")) &&
                        (localFileName.endsWith(".header.medical") ||
                            localFileName.endsWith(".training.header.medical") ||
                            localFileName.endsWith(".anonym.training.header.medical"))) {
                        headerFile = localFileName;
                        break;
                    }
                }

                if (headerFile == null)
                    continue;

                String pathHeader = headerPath + File.separator + headerFile;
                int p = 0;
                BufferedReader bis = new BufferedReader(
                    new InputStreamReader(new FileInputStream(pathHeader), "UTF8"));

                StringBuilder header = new StringBuilder();

                String line;
                while ((line = bis.readLine()) != null) {
                    header.append(line);
                    int ii = line.indexOf(' ');
                    String token = null;
                    if (ii != -1) {
                        token = line.substring(0, ii);
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been generated by a recent version of grobid
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    }

                    // we get the label in the labelled data file for the same token
                    for (int pp = p; pp < labeled.size(); pp++) {
                        String localLine = labeled.get(pp);
                        StringTokenizer st = new StringTokenizer(localLine, " ");
                        if (st.hasMoreTokens()) {
                            String localToken = st.nextToken();
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been generated by a recent version of grobid
                            localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);

                            if (localToken.equals(token)) { // if the token (localToken) in the file containing labeled data equals to the token in the file containing features

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
                                header.append(line);*/

                                String tag = st.nextToken();
                                header.append(" ").append(tag);
                                p = pp + 1;
                                pp = p + 10;
                            }
                        }
                        if (pp - p > 5) {
                            break;
                        }
                    }
                    header.append("\n");
                }
                bis.close();

                // post process for ensuring continuous labelling
                StringBuilder header2 = new StringBuilder();
                String headerStr = header.toString();
                StringTokenizer sto = new StringTokenizer(headerStr, "\n");
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
                                header2.append(previousLine);
                                header2.append("\n");
                            } else {
                                if (lastLabel != null) {
                                    header2.append(previousLine);
                                    header2.append("\n");
                                }
                            }
                        } else {
                            if (lastLabel != null) {
                                header2.append(previousLine);
                                header2.append("\n");
                            }
                        }
                    }
                    previousLine = linee;

                    lastLastLabel = lastLabel;
                    lastLabel = label;
                }

                if (lastLabel != null) {
                    header2.append(previousLine);
                    header2.append("\n");
                }

                if ((writer2 == null) && (writer3 != null))
                    writer3.write(header2.toString() + "\n");
                if ((writer2 != null) && (writer3 == null))
                    writer2.write(header2.toString() + "\n");
                else {
                    if (Math.random() <= splitRatio)
                        writer2.write(header2.toString() + "\n");
                    else
                        writer3.write(header2.toString() + "\n");
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
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
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
        Utility utility = new Utility();
        utility.initGrobid(null);
        Trainer trainer = new HeaderMedicalReportTrainer();
        AbstractTrainer.runTraining(trainer);
        System.out.println(AbstractTrainer.runEvaluation(trainer));
        System.exit(0);
    }
}