package org.grobid.service.configuration;

import io.dropwizard.Configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class GrobidMedicalReportServiceConfiguration extends Configuration {
    @JsonProperty
    private String grobidHome;

    @JsonProperty
    private boolean modelPreload = false;

    @JsonProperty
    private String corsAllowedOrigins;

    @JsonProperty
    private String corsAllowedMethods;

    @JsonProperty
    private String corsAllowedHeaders;

    public String getGrobidHome() {
        return grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

    public boolean getModelPreload() {
        return modelPreload;
    }

    public void setModelPreload(boolean modelPreload) {
        this.modelPreload = modelPreload;
    }
}
