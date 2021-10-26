package org.grobid.trainer;

import org.apache.commons.io.IOUtils;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Date;
import org.grobid.core.data.Entity;
import org.grobid.core.data.MedicalEntity;
import org.grobid.core.engines.EngineMedicalParsers;
import org.grobid.core.engines.FrenchMedicalNERParser;
import org.grobid.core.engines.NERParserCommon;
import org.grobid.core.engines.NERParsers;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;

import java.util.Arrays;
import java.util.List;

public class NERParser {
    public static void main(String[] args) {
        System.out.println("Test");
        String pGrobidHome = "../grobid-home";
        GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
        GrobidProperties.getInstance(grobidHomeFinder);

        System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getGrobidHomePath());

        EngineMedicalParsers engineMedicalParsers = new EngineMedicalParsers();
        String input = "Austria fought the enemies with Germany.";


        //List<Date> entities = engineMedicalParsers.getDateParser().process("1 October 2021");

        System.out.println("\n" + input);
        /*if (entities != null) {
            for (Entity entity : entities) {
                System.out.print(input.substring(entity.getOffsetStart(), entity.getOffsetEnd()) + "\t");
                System.out.println(entity.toString());
            }
        } else {
            System.out.println("No entity found.");
        }
        System.out.println("\n");*/


    }
}
