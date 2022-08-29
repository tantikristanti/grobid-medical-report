package org.grobid.core.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;


/**
 * grobid-medical-report environment definition as the parameters found in the grobid-medical-report.yaml configuration file.
 *
 */

public class GrobidMedicalReportConfiguration {
    private static Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportConfiguration.class);
    // path to the grobid-home (../grobid-home)
    public String grobidHome;

    // initializing all listed sequence labeling models
    public List<ModelParameters> models;

    public GrobidMedicalReportConfiguration getInstance() {
        return getInstance(null);
    }

    public static GrobidMedicalReportConfiguration getInstance(String projectRootPath) {
        GrobidMedicalReportConfiguration grobidMedicalReportConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            if (projectRootPath == null)
                grobidMedicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), GrobidMedicalReportConfiguration.class);
            else
                grobidMedicalReportConfiguration = mapper.readValue(new File(projectRootPath + "/resources/config/grobid-medical-report.yaml"), GrobidMedicalReportConfiguration.class);
        } catch(Exception e) {
            LOGGER.error("The config file does not appear valid, see resources/config/grobid-medical-report.yaml", e);
        }
        return grobidMedicalReportConfiguration;
    }


    public String getGrobidHome() {
        return grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public List<ModelParameters> getModels() {
        return models;
    }

    public void setModels(List<ModelParameters> models) {
        this.models = models;
    }



}
