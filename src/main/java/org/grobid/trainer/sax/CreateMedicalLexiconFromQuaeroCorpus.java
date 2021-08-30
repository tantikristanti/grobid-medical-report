package org.grobid.trainer.sax;

import org.grobid.core.data.QuaeroDocument;
import org.grobid.core.data.QuaeroEntity;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*A main class to create resources (lexicon, training datasets) from @ The QUAERO French Medical Corpus https://quaerofrenchmed.limsi.fr/
The sources files are downloaded from the openly available sources https://quaerofrenchmed.limsi.fr/#download

Névéol A, Grouin C, Leixa J, Rosset S, Zweigenbaum P. The QUAERO French Medical Corpus: A Ressource for Medical Entity Recognition and Normalization. Fourth Workshop on Building and Evaluating Ressources for Health and Biomedical Text Processing - BioTxtM2014. 2014:24-30
*/


public class CreateMedicalLexiconFromQuaeroCorpus {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateMedicalLexiconFromQuaeroCorpus.class);
    private List<QuaeroEntity> quaeroEntities = new ArrayList<>();

    public void createMedicalLexicon(String input, String output) {
        try {
            File path = new File(input);
            if (!path.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + input);
            }

            File pathOut = new File(output);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + output);
            }

            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith("_bioc");
                }
            });

            if (refFiles == null)
                System.out.println(refFiles.length + " files to be processed.");
            for (final File file : refFiles) {
                try {
                    createMedicalLexiconFromBioC(file, output);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + file.getPath() + ": " + exp);
                }
            }
            if (quaeroEntities != null ) {
                writeToFile(output);
            }
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    public void createMedicalLexiconFromBioC(File inputFile, String pathOutput) {
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot create medical lexicon from the Quaero French Medical Corpus, because file '" +
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
                    List<QuaeroEntity> quaeroEntities =  documents.get(i).getEntities();
                    for(QuaeroEntity entity : quaeroEntities){
                        QuaeroEntity quaeroEntity = new QuaeroEntity();
                        quaeroEntity.setType(entity.getType());
                        quaeroEntity.setText(entity.getText());
                        quaeroEntities.add(quaeroEntity);
                    }
                }
            } catch (SAXException | ParserConfigurationException | IOException ie) {
                ie.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occured while creating " +
                "medical lexicon from the Quaero French Medical Corpus.", e);
        }
    }

    public void writeToFile(String pathOutput) {
        try {
            // Group text by their NER types
            Map<String, List<QuaeroEntity>> byTypes = quaeroEntities.stream()
                .collect(Collectors.groupingBy(QuaeroEntity::getType));

            for (Map.Entry<String, List<QuaeroEntity>> entry : byTypes.entrySet()) {
                List<String> texts = new ArrayList<>();
                String key = entry.getKey(); // the key is the NER type
                //System.out.println("Type = " + key);

                File outputLexiconFile = new File(pathOutput + File.separator + key + ".txt");
                Writer writer = new OutputStreamWriter(new FileOutputStream(outputLexiconFile, false), StandardCharsets.UTF_8);

                List<QuaeroEntity> values = entry.getValue();
                for (QuaeroEntity value : values) {
                    texts.add(value.getText());
                }
                List<String> textDistinct = texts.stream().distinct().collect(Collectors.toList());
                //System.out.println("Mentions = " + textDistinct);

                for (String text : textDistinct) {
                    writer.write(text + "\n");
                }
                writer.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid training" +
                " data generation for header medical report.", e);
        }
    }

    public static void main(String[] args) {
        String inputDirectory = "resources/corpus/Quaero";
        String outputDirectory = "resources/lexicon";
        CreateMedicalLexiconFromQuaeroCorpus createMedicalLexiconFromQuaeroCorpus = new CreateMedicalLexiconFromQuaeroCorpus();
        createMedicalLexiconFromQuaeroCorpus.createMedicalLexicon(inputDirectory, outputDirectory);
    }

}
