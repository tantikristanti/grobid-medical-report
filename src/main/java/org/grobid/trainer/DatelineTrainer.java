package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorDateline;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.trainer.sax.TEIDatelineSaxParser;
import org.grobid.utility.Utility;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;

/* A class for training the Dateline model.
 * */

public class DatelineTrainer extends AbstractTrainer {

    public DatelineTrainer() {
        super(GrobidModels.DATELINE);
    }

    /**
     * Add the selected features to a dateline example set
     *
     * @param corpusDir          a path where corpus files are located
     * @param trainingOutputPath path where to store the temporary training data
     * @return the total number of corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir, final File trainingOutputPath) {
        return createCRFPPData(corpusDir, trainingOutputPath, null, 1.0);
    }

    /**
     * Add the selected features to a dateline example set
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
        Lexicon lexicon = Lexicon.getInstance();
        try {
            System.out.println("sourcePathLabel: " + corpusDir);
            if (trainingOutputPath != null)
                System.out.println("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                System.out.println("outputPath for evaluation data: " + evalOutputPath);

            // we convert the tei files into the usual CRF label format
            // we process all tei files in the output directory
            final File[] refFiles = corpusDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            if (refFiles == null) {
                throw new IllegalStateException("Folder " + corpusDir.getAbsolutePath()
                    + " does not seem to contain training data. Please check");
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
            List<OffsetPosition> locationsPositions;

            int n = 0;
            for (; n < refFiles.length; n++) {
                final File teifile = refFiles[n];
                String name = teifile.getName();
                //System.out.println(name);

                final TEIDatelineSaxParser parser2 = new TEIDatelineSaxParser();

                // get a new instance of parser
                final SAXParser p = spf.newSAXParser();
                p.parse(teifile, parser2);

                final List<List<String>> allLabeled = parser2.getLabeledResult();
                final List<List<LayoutToken>> allTokens = parser2.getTokensResult();
                totalExamples += parser2.nbDatelines;

                // we can now add the features
                for (int i = 0; i < allTokens.size(); i++) {
                    // fix the offsets
                    int pos = 0;
                    for (LayoutToken token : allTokens.get(i)) {
                        token.setOffset(pos);
                        pos += token.getText().length();
                    }
                    locationsPositions = lexicon.tokenPositionsLocationNames(allTokens.get(i));

                    String headerDatelines = FeaturesVectorDateline.addFeaturesDateline(allTokens.get(i), allLabeled.get(i), locationsPositions);

                    if ((writer2 == null) && (writer3 != null))
                        writer3.write(headerDatelines + "\n \n");
                    if ((writer2 != null) && (writer3 == null))
                        writer2.write(headerDatelines + "\n \n");
                    else {
                        if (Math.random() <= splitRatio)
                            writer2.write(headerDatelines + "\n \n");
                        else
                            writer3.write(headerDatelines + "\n \n");
                    }
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
            throw new GrobidException("An exception occurred while running Grobid.", e);
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
        Utility utility = new Utility();
        utility.initGrobid(null);
        Trainer trainer = new DatelineTrainer();
        AbstractTrainer.runTraining(trainer);
        System.out.println(AbstractTrainer.runEvaluation(trainer));
        System.exit(0);
    }

}