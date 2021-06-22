package org.grobid.core.utilities;

import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MedicalReportConfiguration {
    private static Logger LOGGER = LoggerFactory.getLogger(MedicalReportConfiguration.class);

    public String grobidHome;
    public String corpusMedicalReportSegmenterPath;
    public String templateMedicalReportSegmenterPath;
    public String evaluationMedicalReportSegmenterPath;

    public MedicalReportConfiguration getInstance() {
        return getInstance(null);
    }

    public static MedicalReportConfiguration getInstance(String projectRootPath) {

        MedicalReportConfiguration medicalReportConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            if (projectRootPath == null)
                medicalReportConfiguration = mapper.readValue(new File("resources/config/grobid-medical-report.yaml"), MedicalReportConfiguration.class);
            else
                medicalReportConfiguration = mapper.readValue(new File(projectRootPath + "/resources/config/grobid-medical-report.yaml"), MedicalReportConfiguration.class);
        } catch(Exception e) {
            LOGGER.error("The config file does not appear valid, see resources/config/grobid-astro.yaml", e);
        }
        return medicalReportConfiguration;
    }

    // sequence labeling models
    public List<ModelParameters> models;

    public String getGrobidHome() {
        return this.grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public String getCorpusMedicalReportSegmenterPath() {
        return this.corpusMedicalReportSegmenterPath;
    }

    public void setCorpusMedicalReportSegmenterPath(String corpusMedicalReportSegmenterPath) {
        this.corpusMedicalReportSegmenterPath = corpusMedicalReportSegmenterPath;
    }

    public String getTemplateMedicalReportSegmenterPath() {
        return this.templateMedicalReportSegmenterPath;
    }

    public void setTemplateMedicalReportSegmenterPath(String templateMedicalReportSegmenterPath) {
        this.templateMedicalReportSegmenterPath = templateMedicalReportSegmenterPath;
    }

    public String getEvaluationMedicalReportSegmenterPath() {
        return this.evaluationMedicalReportSegmenterPath;
    }

    public void setEvaluationMedicalReportSegmenterPath(String evaluationMedicalReportSegmenterPath) {
        this.evaluationMedicalReportSegmenterPath = evaluationMedicalReportSegmenterPath;
    }

    public List<ModelParameters> getModels() {
        return this.models;
    }

    public void getModels(List<ModelParameters> models) {
        this.models = models;
    }


}
