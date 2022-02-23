package org.grobid.trainer.sax;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import org.grobid.core.data.QuaeroDocument;
import org.grobid.core.data.QuaeroEntity;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*A main class to create resources (lexicon, training datasets) from @ The QUAERO French Medical Corpus https://quaerofrenchmed.limsi.fr/
The sources files are downloaded from the openly available sources https://quaerofrenchmed.limsi.fr/#download

Névéol A, Grouin C, Leixa J, Rosset S, Zweigenbaum P. The QUAERO French Medical Corpus: A Ressource for Medical Entity Recognition and Normalization. Fourth Workshop on Building and Evaluating Ressources for Health and Biomedical Text Processing - BioTxtM2014. 2014:24-30
*/


public class CreateMedicalDatasetsFromQuaeroCorpus {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateMedicalDatasetsFromQuaeroCorpus.class);

    public void createMedicalNerTraining(File input, File output) {
        try {
            if (!input.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + input);
            }

            if (!output.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + output);
            }

            // we process all pdf files in the directory
            File[] refFiles = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith("_bioc");
                }
            });

            if (refFiles == null)
                System.out.println(refFiles.length + " files to be processed.");

            // ClearParser components for sentence segmentation
            // slow down a bit at launch, but it is used only for generating more readable training
            String dictionaryFile = "data/clearNLP/dictionary-1.3.1.zip";
            LOGGER.info("Loading dictionary file for sentence segmentation: " + dictionaryFile);
            AbstractTokenizer tokenizer = EngineGetter.getTokenizer(Language.EN, new FileInputStream(dictionaryFile));
            LOGGER.info("End of loading dictionary file");

            for (final File file : refFiles) {
                try {
                    createTrainingBioC_Xml(file, output, tokenizer);
                } catch (final Exception exp) {
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
            }
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running Grobid batch.", exp);
        }
    }

    public void createTrainingBioC_Xml(File inputFile, File pathOutput, AbstractTokenizer tokenizer) {
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot create medical NER training datasets from the Quaero French Medical Corpus, because file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            SAXParserFactory spf = SAXParserFactory.newInstance();
            QuaeroCorpusSaxHandler handler = new QuaeroCorpusSaxHandler();

            try {
                SAXParser sp = spf.newSAXParser();
                sp.parse(inputFile, handler);

                List<QuaeroDocument> quaeroDocuments = handler.getDocuments();

                File outputTrainingFile = null;
                Writer writer = null;
                List<QuaeroEntity> quaeroEntities = new ArrayList<>();

                if (quaeroDocuments != null && quaeroDocuments.size() > 0) {

                    for (int i = 0; i < quaeroDocuments.size(); i++) {
                        if (quaeroDocuments.get(i).getEntities() != null && quaeroDocuments.get(i).getEntities().size() != 0) {
                            quaeroEntities = quaeroDocuments.get(i).getEntities();
                            // sort the entities by their offsets
                            quaeroEntities.sort(Comparator.comparing(QuaeroEntity::getOffset));

                            String theText = quaeroDocuments.get(i).getText();

                            // fix the broken offsets
                            fixBrokenOffsets(theText, quaeroEntities);

                            // determine whether nested entities or not
                            setNestedEntity(quaeroEntities);
                        } else {
                            continue;
                        }
                    }
                }

                outputTrainingFile = new File(pathOutput + File.separator + inputFile.getName() + ".training.french.medical.ner.tei.xml");
                writer = new OutputStreamWriter(new FileOutputStream(outputTrainingFile, false), StandardCharsets.UTF_8);
                StringBuilder sbAll = new StringBuilder(), sbHeader = new StringBuilder(), sbAfter = new StringBuilder(), sbContents = new StringBuilder();

                sbHeader.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                sbHeader.append("<corpus>\n");
                sbAll.append(sbHeader);
                if (quaeroDocuments != null && quaeroDocuments.size() > 0) {
                    for (int i = 0; i < quaeroDocuments.size(); i++) {
                        String docId = quaeroDocuments.get(i).getId().replace(" ", "_");
                        String theTextOriginal = quaeroDocuments.get(i).getText();
                        String theText = theTextOriginal;
                        //System.out.println("Document ID : " + docId + "; Text : " + theTextOriginal);

                        sbAll.append("\t<document name=\"" + docId + "\"" + " xml:lang=\"" + Language.FR + "\">\n");

                        if (quaeroDocuments.get(i).getEntities() != null && quaeroDocuments.get(i).getEntities().size() != 0) {
                            quaeroEntities = quaeroDocuments.get(i).getEntities();
                            int tagLengthSum = 0;
                            for (QuaeroEntity entity : quaeroEntities) {
                                StringBuilder sbContent = new StringBuilder();
                                // the mention and the class of the entity
                                String entityText = entity.getText().trim();
                                String entityType = entity.getType();
                                int entityStart = entity.getOffset();
                                int length = entity.getLength();
                                int entityEnd = entityStart + length;
                                boolean isNested = entity.isNestedEntity();
                                //String entityTextCut = theTextOriginal.substring(entityStart, entityEnd);
                                //System.out.println("entityTextOriginal : " + entityText + "; entityType : " + entityType + "; entityStart : " + entityStart + "; isNested : " + isNested);

                                // don't include the nested entities
                                if (isNested) {
                                    continue;
                                } else {
                                    sbContent.append("<ENAMEX type=\"" + entityType + "\">");
                                    sbContent.append(TextUtilities.HTMLEncode(entityText));
                                    sbContent.append("</ENAMEX>");
                                    String textBefore = "";
                                    if(entityStart + tagLengthSum >=0)
                                        textBefore = theText.substring(0, entityStart + tagLengthSum);

                                    String textAfter = theText.substring(entityEnd + tagLengthSum);
                                    theText = textBefore + sbContent.toString() + textAfter;
                                    tagLengthSum = tagLengthSum + sbContent.length() - length;
                                }
                            }
                            sbContents = new StringBuilder(theText);
                            //System.out.println("theText :" + theText);

                        } else {
                            // if there isn't any entity, just take the original text
                            sbContents = new StringBuilder(theTextOriginal);
                        }

                        if (sbContents.length() > 0) {
                            // convert to more readable format by lines
                            String[] paragraphs = sbContents.toString().split("\n");
                            for (int p = 0; p < paragraphs.length; p++) {
                                String splitText = paragraphs[p];
                                if (theText.trim().length() == 0)
                                    continue;

                                sbAll.append("\t\t<p" + " xml:id=\"p_" + +p + "\">" + splitText + "</p>\n");
                            }
                        }
                        sbAll.append("\t</document>\n");
                    }
                    sbAll.append("</corpus>\n");
                    System.out.println(sbAll);
                    writer.write(sbAll.toString());
                    writer.close();
                }
            } catch (SAXException | ParserConfigurationException | IOException ie) {
                ie.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occurred while creating " +
                "medical NER training data from the Quaero French Medical Corpus.", e);
        }
    }

    public void fixBrokenOffsets(String theText, List<QuaeroEntity> entities) {
        // check the offset error rates with the first entity found only
        int originalOffset = entities.get(0).getOffset();
        String entityText = entities.get(0).getText();
        int correctOffset = theText.indexOf(entityText);
        int errorOffsetRate = originalOffset - correctOffset;
        //System.out.println("originalOffset : " + originalOffset + "; correctOffset : " + correctOffset + "; errorOffsetRate : " + errorOffsetRate);

        for (QuaeroEntity entity : entities) {
            //System.out.println("originalOffset : " + entity.getOffset() + "; correctOffset : " + (entity.getOffset() - errorOffsetRate));
            entity.setOffset(entity.getOffset() - errorOffsetRate);
        }
    }

    public void setNestedEntity(List<QuaeroEntity> entities) {
        // find the longest entity as point to lookup
        int max = 0;
        for (QuaeroEntity entity : entities) {
            if ((entity.getText().split(" ").length) >= max) {
                max = entity.getText().split(" ").length;
            }
        }

        // lookup to the previous entities
        for (int i = 0; i < entities.size(); i++) {
            String currentEntity = entities.get(i).getText();
            int currentOffset = entities.get(i).getOffset();
            for (int j = 1; j <= max; j++) {
                if (i - j >= 0) {
                    String prevEntity = entities.get(i - j).getText();
                    int prevOffset = entities.get(i - j).getOffset();
                    int prevLength = entities.get(i - j).getLength();
                    if ((prevEntity != null) &&
                        (prevEntity.split(" ").length > 1)) {
                        if (currentEntity.split(" ").length == 1) {
                            if (Integer.compare(prevOffset, currentOffset) == 0) {
                                entities.get(i).setNestedEntity(true);
                            }
                        }

                        if (prevEntity.contains(currentEntity) &&
                            currentOffset >= prevOffset && currentOffset <= prevOffset + prevLength) {
                            entities.get(i).setNestedEntity(true);
                        }
                    }
                } else {
                    break;
                }
            }
        }

        // lookup to the next entities
        for (int i = 0; i < entities.size(); i++) {
            String currentEntity = entities.get(i).getText();
            int currentOffset = entities.get(i).getOffset();
            for (int j = 1; j <= max; j++) {
                if (i + j < entities.size() - 1) {
                    String nextEntity = entities.get(i + j).getText();
                    int nextOffset = entities.get(i + j).getOffset();
                    int nextLength = entities.get(i + j).getLength();
                    if ((nextEntity != null) &&
                        (nextEntity.split(" ").length > 1)) {
                        if (currentEntity.split(" ").length == 1) {
                            if (Integer.compare(currentOffset, nextOffset) == 0) {
                                entities.get(i).setNestedEntity(true);
                            }
                        }

                        if (nextEntity.contains(currentEntity) &&
                            (currentOffset >= nextOffset) && (currentOffset <= nextOffset + nextLength)) {
                            entities.get(i).setNestedEntity(true);
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    public boolean isDeterminant(String text) {
        List<String> determinant = new ArrayList<>();
        determinant.add("un");
        determinant.add("une");
        determinant.add("la");
        determinant.add("le");
        determinant.add("les");
        determinant.add("l\'");
        determinant.add("ce");
        determinant.add("cet");
        determinant.add("cette");
        determinant.add("ces");
        determinant.add("de");
        determinant.add("du");
        determinant.add("de la");
        determinant.add("des");
        determinant.add("au");
        determinant.add("aux");
        if (determinant.contains(text)) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        String inputDirectory = "resources/corpus/Quaero";
        String outputDirectory = "resources/corpus/Quaero/Results";
        CreateMedicalDatasetsFromQuaeroCorpus createMedicalDatasetsFromQuaeroCorpus = new CreateMedicalDatasetsFromQuaeroCorpus();
        createMedicalDatasetsFromQuaeroCorpus.createMedicalNerTraining(new File(inputDirectory), new File(outputDirectory));
    }

}
