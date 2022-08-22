package org.grobid.service;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.grobid.service.configuration.GrobidMedicalReportServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/*  An entry for web service application.
* */
public class GrobidMedicalReportServiceApplication extends Application<GrobidMedicalReportServiceConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportServiceApplication.class);
    private static final String[] DEFAULT_CONF_LOCATIONS = {"resources/config/grobid-medical-service.yaml"};
    private static final String RESOURCES = "/api";

    @Override
    public String getName() {
        return "grobid-medical-report-service";
    }

    @Override
    public void initialize(Bootstrap<GrobidMedicalReportServiceConfiguration> bootstrap) {
        GuiceBundle<GrobidMedicalReportServiceConfiguration> guiceBundle = GuiceBundle.defaultBuilder(GrobidMedicalReportServiceConfiguration.class)
            .modules(getGuiceModules())
            .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/web", "/", "index.html", "grobidMedicalReportAssets"));
    }

    private List<? extends Module> getGuiceModules() {
        return Lists.newArrayList(new GrobidMedicalReportServiceModule());
    }

    @Override
    public void run(GrobidMedicalReportServiceConfiguration configuration, Environment environment) {
        LOGGER.info("Service config={}", configuration);
        new DropwizardExports(environment.metrics()).register();
        ServletRegistration.Dynamic registration = environment.admin().addServlet("Prometheus", new MetricsServlet());
        registration.addMapping("/metrics/prometheus");
        environment.jersey().setUrlPattern(RESOURCES + "/*");

        String allowedOrigins = configuration.getCorsAllowedOrigins();
        String allowedMethods = configuration.getCorsAllowedMethods();
        String allowedHeaders = configuration.getCorsAllowedHeaders();

        // Enable CORS headers
        final FilterRegistration.Dynamic cors =
            environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, allowedMethods);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowedHeaders);

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, RESOURCES + "/*");
    }

    public static void main(String[] args) throws Exception {
        if (ArrayUtils.getLength(args) < 2) { // [0]-server, [1]-<path to config yaml file

            String foundConf = null;
            for (String p : DEFAULT_CONF_LOCATIONS) {
                File confLocation = new File(p).getAbsoluteFile();
                if (confLocation.exists()) {
                    foundConf = confLocation.getAbsolutePath();
                    LOGGER.info("Found conf path: {}", foundConf);
                    break;
                }
            }

            if (foundConf != null) {
                LOGGER.warn("Running with default arguments: \"server\" \"{}\"", foundConf);
                args = new String[]{"server", foundConf};
            } else {
                throw new RuntimeException("No explicit config provided and cannot find in one of the default locations: "
                    + Arrays.toString(DEFAULT_CONF_LOCATIONS));
            }
        }

        LOGGER.info("Configuration file: {}", new File(args[1]).getAbsolutePath());
        new GrobidMedicalReportServiceApplication().run(args);
    }

}
