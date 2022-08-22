package org.grobid.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidMedicalReportConfiguration;
import org.grobid.core.utilities.GrobidProperties;

import java.io.File;
import java.util.Arrays;

public class Utility {
    /**
     * Init GROBID by loading the configuration and the native libraries in the grobid-home
     *
     * @param grobidHome
     */
    public void initGrobid(String grobidHome) {
        GrobidMedicalReportConfiguration grobidMedicalReportConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            grobidMedicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), GrobidMedicalReportConfiguration.class);
        } catch (Exception e) {
            System.err.println("The config file does not appear valid, see resources/config/grobid-medical-report.yaml");
        }
        try {
            String pGrobidHome = grobidMedicalReportConfiguration.getGrobidHome();
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);
            System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getInstance().getGrobidHome());

            for (GrobidConfig.ModelParameters theModel : grobidMedicalReportConfiguration.getModels()) {
                GrobidProperties.getInstance().addModel(theModel);
            }
            // load Grobid libraries
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("grobid-medical-report initialisation failed: " + exp);
            exp.printStackTrace();
        }
    }
}
