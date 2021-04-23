package org.grobid.core.document;

import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class DocumentSourceMedical {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DocumentSourceMedical.class);
    public static void main(String[] args) throws IOException {
        String input = "/Volumes/LaCie/Tanti/PdfExampleMedical/NON_APHP/pdf-5.pdf";
        String output = "/Volumes/LaCie/Tanti/PdfExampleMedical/NON_APHP/pdf-5_byte.pdf";
        String outputByte = "/Volumes/LaCie/Tanti/PdfExampleMedical/NON_APHP/pdf-5_byte.bin";
        File fileInput = new File(input);
        File fileOutput = new File(output);

        FileInputStream fis = new FileInputStream(fileInput);
        FileOutputStream fos = new FileOutputStream(fileOutput);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] pdfInByte = Files.readAllBytes(Paths.get(input));
        byte[] bufferSize = new byte[1024];
        try {
            // InputStream from file
            for (int readNum; (readNum = fis.read(bufferSize)) != -1;) {
                //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                bos.write(bufferSize, 0, readNum);
                //System.out.println("read " + readNum + " bytes,");
            }

            // InputStream from byte
            InputStream inputStream = new ByteArrayInputStream(pdfInByte);
            for (int readNum; (readNum = inputStream.read(bufferSize)) != -1;) {
                //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                bos.write(bufferSize, 0, readNum);
                //System.out.println("read " + readNum + " bytes,");
            }

            int byteRead;

            while ((byteRead = inputStream.read()) != -1) {
                bos.write(bufferSize, 0, byteRead);
            }

        } catch (IOException ex) {
            logger.info("Processing: " + ex);
        }
        byte[] bytes = bos.toByteArray();

        // output binary pdf
        System.out.println(Arrays.toString(pdfInByte));
        Files.write(Paths.get(outputByte), pdfInByte);

        // write the bytes to an output file
        fos.write(bytes);
        fos.flush();
        fos.close();

        // text example
        /*String inputString = "date=September 16th, 2001";
        byte[] byteArrray = inputString.getBytes();
        System.out.println(Arrays.toString(byteArrray));
        //date=September 16th, 2001"
        //byte[] byteArrray = { 100, 97, 116, 101, 61, 83, 101, 112, 116, 101, 109, 98, 101, 114, 32, 49, 54, 116, 104, 44, 32, 50, 48, 48, 49};
        //September 16th, 2001"
        //byte[] byteArrray = { 83, 101, 112, 116, 101, 109, 98, 101, 114, 32, 49, 54, 116, 104, 44, 32, 50, 48, 48, 49 };
        String string = new String(byteArrray);
        System.out.println(string);*/
    }
}
