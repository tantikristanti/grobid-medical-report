package org.grobid.core.utilities;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.sax.FrMedicalNerCorpusSaxHandler;
import org.grobid.trainer.sax.FrenchCorpusSaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;

/* A class to merge XML training/testing files for other processes. Normally, we use the joined files to work, for instance, with DeLFT (https://github.com/kermitt2/delft/)
 * */

public class XMLMerger {
    private static final Logger logger = LoggerFactory.getLogger(FrenchCorpusSaxHandler.class);

    public static void main(String[] args) throws Exception {
        String dirPath = "../grobid-trainer/resources/dataset/fr-medical-ner/corpus/";
        File path = new File(dirPath);
        // we process all pdf files in the directory
        File[] refFiles = path.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                System.out.println(name);
                return name.endsWith(".xml");
            }
        });
        System.out.println(refFiles.length + " files to be processed.");

        FileOutputStream outputStream = new FileOutputStream(dirPath + "FrMedicalNerTrain.xml");
        XMLStreamWriter out = XMLOutputFactory.newInstance().createXMLStreamWriter(new OutputStreamWriter(outputStream));

        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

        FrMedicalNerCorpusSaxHandler handler = new FrMedicalNerCorpusSaxHandler(out);
        // we output everything before the root <corpus>
        out.writeStartDocument();
        out.writeCharacters("\r\n");
        // we start the root <corpus>
        out.writeStartElement("corpus");
        for (final File file : refFiles) {
            try {
                saxParser.parse(file, handler);
            } catch (final Exception exp) {
                logger.error("An error occurred while processing the following XML: " + file.getPath(), exp);
            }
        }
        // we output everything after </corpus>
        out.writeEndElement();
        out.close();
    }
}
