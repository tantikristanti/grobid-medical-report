package org.grobid.service;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import org.grobid.service.configuration.GrobidMedicalReportServiceConfiguration;
import org.grobid.service.exceptions.mapper.GrobidExceptionMapper;
import org.grobid.service.exceptions.mapper.GrobidExceptionsTranslationUtility;
import org.grobid.service.exceptions.mapper.GrobidServiceExceptionMapper;
import org.grobid.service.exceptions.mapper.WebApplicationExceptionMapper;
import org.grobid.service.process.*;
import org.grobid.service.resources.HealthResource;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class GrobidMedicalReportServiceModule extends DropwizardAwareModule<GrobidMedicalReportServiceConfiguration> {
    @Override
    public void configure(Binder binder) {
        // Generic modules
        binder.bind(HealthCheck.class);
        binder.bind(GrobidMedicalReportRestProcessGeneric.class);

        // web services
        binder.bind(GrobidMedicalReportRestService.class);

        // Core components (text and file processing)
        binder.bind(GrobidMedicalReportRestProcessString.class);
        binder.bind(GrobidMedicalReportRestProcessFiles.class);


    }
    @Provides
    protected ObjectMapper getObjectMapper() {
        return getEnvironment().getObjectMapper();
    }

    @Provides
    protected MetricRegistry provideMetricRegistry() {
        return getMetricRegistry();
    }

    //for unit tests
    protected MetricRegistry getMetricRegistry() {
        return getEnvironment().metrics();
    }

    @Provides
    Client provideClient() {
        return ClientBuilder.newClient();
    }
}