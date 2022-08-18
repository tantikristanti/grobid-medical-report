package org.grobid.core.utilities;

import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GrobidMedicalReportConfiguration {
    private static Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportConfiguration.class);
    private String grobidHome = "../grobid-home";
    private String tmp = "../grobid-home/tmp";
    // sequence labeling models
    public List<ModelParameters> models;

    public String getGrobidHome() {
        return grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public String getTmp() {
        return tmp;
    }

    public void setTmp(String tmp) {
        this.tmp = tmp;
    }


    public List<ModelParameters> getModels() {
        return models;
    }

    public void setModels(List<ModelParameters> models) {
        this.models = models;
    }
}
