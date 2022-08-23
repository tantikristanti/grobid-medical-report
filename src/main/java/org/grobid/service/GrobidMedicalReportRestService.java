package org.grobid.service;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.grobid.core.engines.EngineMedical;
import org.grobid.core.factory.AbstractEngineMedicalFactory;
import org.grobid.core.factory.GrobidMedicalPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidMedicalReportServiceConfiguration;
import org.grobid.service.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.util.NoSuchElementException;

/**
 * RESTful services for grobid-medical-report.
 */

@Timed
@Singleton
@Path(GrobidMedicalReportPaths.ROOT)
public class GrobidMedicalReportRestService implements GrobidMedicalReportPaths {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportRestService.class);

    private static final String NAMES = "names";
    private static final String DATE = "date";
    private static final String AFFILIATIONS = "affiliations";
    private static final String SHA1 = "sha1";
    private static final String XML = "xml";
    public static final String INPUT = "input";

    @Inject
    private GrobidMedicalReportRestProcessGeneric restProcessGeneric;

    @Inject
    private GrobidMedicalReportRestProcessString restProcessString;

    @Inject
    private GrobidMedicalReportRestProcessFiles restProcessFiles;

    @Inject
    public GrobidMedicalReportRestService(GrobidMedicalReportServiceConfiguration configuration) {
        GrobidProperties.setGrobidHome(new File(configuration.getGrobidHome()).getAbsolutePath());
        GrobidProperties.getInstance();
        GrobidProperties.setContextExecutionServer(true);
        LOGGER.info("Initiating servlet grobid-medical-report Rest Service");
        AbstractEngineMedicalFactory.init();
        EngineMedical engine = null;
        try {
            // this will init or not all the models in memory
            engine = EngineMedical.getEngine(configuration.getModelPreload());
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time.");
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs when initiating the grobid engine. ", exp);
        } finally {
            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.info("Initiating of servlet grobid-medical-report rest service finished.");
    }

    /**
        * @see org.grobid.service.process.GrobidMedicalReportRestProcessGeneric#isAlive()
     *
     */
    @Path(GrobidMedicalReportPaths.PATH_IS_ALIVE)
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response isAlive() {
        Response response = null;
        try {
            response = Response.status(Response.Status.OK)
                .entity(restProcessGeneric.isAlive())
                .type(MediaType.TEXT_HTML)
                .build();

        } catch (Exception e) {
            LOGGER.error("Exception occurred while check if the service is alive. " + e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;

    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessGeneric#getVersion()
     */
    @Path(GrobidMedicalReportPaths.PATH_GET_VERSION)
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response getVersion() {
        Response response = null;
        try {
            response = Response.status(Response.Status.OK)
                .entity(restProcessGeneric.getVersion())
                .type(MediaType.APPLICATION_JSON)
                .build();

        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessGeneric#getDescription()
     */
    @Path(GrobidMedicalReportPaths.PATH_GET_DESCRIPTION)
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response getDescription() {

        Response response = null;
        try {
            response = Response.status(Response.Status.OK)
                .entity(restProcessGeneric.getDescription())
                .type(MediaType.TEXT_HTML)
                .build();

        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }
}
