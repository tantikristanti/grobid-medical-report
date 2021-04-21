package org.grobid.core.document;

import javassist.bytecode.ByteArray;
import org.grobid.trainer.sax.TEIMedicalSaxParser;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class DocumentByte {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DocumentByte.class);
    public static void main(String[] args) throws IOException {
        /*Path pdfPath = Paths.get("/Volumes/LaCie/Tanti/PdfExampleMedical/pdf-5.pdf");
        byte[] pdf = Files.readAllBytes(pdfPath);*/

        // --------------------------

        /*File file = new File("/Volumes/LaCie/Tanti/PdfExampleMedical/pdf-5.pdf");

        FileInputStream fis = new FileInputStream(file);
        //System.out.println(file.exists() + "!!");
        //InputStream in = resource.openStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                bos.write(buf, 0, readNum); //no doubt here is 0
                //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                System.out.println("read " + readNum + " bytes,");
            }
        } catch (IOException ex) {
            logger.error(ex.toString());
        }
        byte[] bytes = bos.toByteArray();

        //below is the different part
        File someFile = new File("/Volumes/LaCie/Tanti/PdfExampleMedical/pdf-5-from-byte.pdf");
        FileOutputStream fos = new FileOutputStream(someFile);
        fos.write(bytes);
        fos.flush();
        fos.close();*/

        /*String fileName = "/Volumes/LaCie/Tanti/PdfExampleMedical/pdf-5.pdf";

        try (InputStream is = new FileInputStream(fileName)) {

            byte[] buffer = new byte[is.available()];
            is.read(buffer);

            int i = 0;

            for (byte b: buffer) {

                if (i % 10 == 0) {
                    System.out.println();
                }

                System.out.printf("%02x ", b);

                i++;
            }
        }

        System.out.println();*/



    }
}
