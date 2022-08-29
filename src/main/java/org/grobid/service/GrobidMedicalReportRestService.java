package org.grobid.service;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.engines.EngineMedical;
import org.grobid.core.factory.AbstractEngineMedicalFactory;
import org.grobid.core.factory.GrobidMedicalPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidMedicalReportServiceConfiguration;
import org.grobid.service.process.GrobidMedicalReportRestProcessFiles;
import org.grobid.service.process.GrobidMedicalReportRestProcessGeneric;
import org.grobid.service.process.GrobidMedicalReportRestProcessString;
import org.grobid.service.util.GrobidRestUtils;
import org.grobid.utility.GrobidRestUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * RESTful services for grobid-medical-report.
 */

@Timed
@Singleton
@Path(GrobidMedicalReportPaths.ROOT)
public class GrobidMedicalReportRestService implements GrobidMedicalReportPaths {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportRestService.class);
    private static final String DATELINE = "dateline";
    private static final String MEDIC = "medic";
    private static final String PATIENT = "patient";

    private static final String NER = "ner";
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

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processDateline(String)
     */
    @Path(PATH_DATELINE)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processDateline_post(@FormParam(DATELINE) String dateline) {
        return restProcessString.processDateline(dateline);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processDateline(String)
     */
    @Path(PATH_DATELINE)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processDateline(@FormParam(DATELINE) String dateline) {
        return restProcessString.processDateline(dateline);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processMedic(String)
     */
    @Path(PATH_MEDIC)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processMedic_post(@FormParam(MEDIC) String medic) {
        return restProcessString.processMedic(medic);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processMedic(String)
     */
    @Path(PATH_MEDIC)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processMedic(@FormParam(MEDIC) String medic) {
        return restProcessString.processMedic(medic);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processPatient(String)
     */
    @Path(PATH_PATIENT)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processPatient_post(@FormParam(PATIENT) String patient) {
        return restProcessString.processPatient(patient);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processPatient(String)
     */
    @Path(PATH_PATIENT)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processPatient(@FormParam(PATIENT) String patient) {
        return restProcessString.processPatient(patient);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processNER(String)
     */
    @Path(PATH_MEDICAL_NER)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processNER_post(@FormParam(NER) String ner) {
        return restProcessString.processNER(ner);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessString#processNER(String)
     */
    @Path(PATH_MEDICAL_NER)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processNER(@FormParam(NER) String ner) {
        return restProcessString.processNER(ner);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processHeaderDocument(InputStream)
     */
    @Path(PATH_HEADER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processHeaderDocument_post(
        @FormDataParam(INPUT) InputStream inputStream) {
        return restProcessFiles.processHeaderDocument(inputStream);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processHeaderDocument(InputStream)
     */
    @Path(PATH_HEADER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processHeaderDocument(
        @FormDataParam(INPUT) InputStream inputStream) {
        return processHeaderDocument_post(inputStream);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processLeftNoteDocument(InputStream)
     */
    @Path(PATH_LEFT_NOTE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processLeftNoteDocument_post(
        @FormDataParam(INPUT) InputStream inputStream) {
        return restProcessFiles.processLeftNoteDocument(inputStream);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processLeftNoteDocument(InputStream)
     */
    @Path(PATH_LEFT_NOTE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processLeftNoteDocument(
        @FormDataParam(INPUT) InputStream inputStream) {
        return processLeftNoteDocument_post(inputStream);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processFullMedicalText(InputStream, int, int, boolean, boolean, List)
     */
    @Path(PATH_FULL_MEDICAL_TEXT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processFullMedicalText_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("-1") @FormDataParam("start") int startPage,
        @DefaultValue("-1") @FormDataParam("end") int endPage,
        @FormDataParam("generateIDs") String generateIDs,
        @FormDataParam("segmentSentences") String segmentSentences,
        @FormDataParam("teiCoordinates") List<FormDataBodyPart> coordinates) throws Exception {
        return processFullText(
            inputStream, startPage, endPage, generateIDs, segmentSentences, coordinates
        );
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processFullMedicalText(InputStream, int, int, boolean, boolean, List)
     */
    @Path(PATH_FULL_MEDICAL_TEXT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processFulltextDocument(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("-1") @FormDataParam("start") int startPage,
        @DefaultValue("-1") @FormDataParam("end") int endPage,
        @FormDataParam("generateIDs") String generateIDs,
        @FormDataParam("segmentSentences") String segmentSentences,
        @FormDataParam("teiCoordinates") List<FormDataBodyPart> coordinates) throws Exception {
        return processFullText(
            inputStream, startPage, endPage, generateIDs, segmentSentences, coordinates
        );
    }

    private Response processFullText(InputStream inputStream,
                                     int startPage,
                                     int endPage,
                                     String generateIDs,
                                     String segmentSentences,
                                     List<FormDataBodyPart> coordinates) throws Exception {
        boolean generate = generateID(generateIDs);
        boolean segment = generateID(segmentSentences);

        List<String> teiCoordinates = extractCoordinates(coordinates);

        return restProcessFiles.processFullMedicalText(
            inputStream, startPage, endPage, generate, segment, teiCoordinates
        );
    }

    private boolean generateID(String generateIDs) {
        boolean generate = false;
        if ((generateIDs != null) && (generateIDs.equals("1"))) {
            generate = true;
        }
        return generate;
    }

    private List<String> extractCoordinates(List<FormDataBodyPart> coordinates) {
        List<String> teiCoordinates = new ArrayList<>();
        if (coordinates != null) {
            for (FormDataBodyPart coordinate : coordinates) {
                String v = coordinate.getValueAs(String.class);
                teiCoordinates.add(v);
            }
        }
        return teiCoordinates;
    }


    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processFrenchNER(InputStream)
     */
    @Path(PATH_FRENCH_MEDICAL_NER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processFrenchMedicalNER_post(
        @FormDataParam(INPUT) InputStream inputStream) {
        return restProcessFiles.processFrenchNER(inputStream);
    }

    /**
     * @see org.grobid.service.process.GrobidMedicalReportRestProcessFiles#processFrenchNER(InputStream)
     */
    @Path(PATH_FRENCH_MEDICAL_NER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processFrenchMedicalNER(
        @FormDataParam(INPUT) InputStream inputStream) {
        return processFrenchMedicalNER_post(inputStream);
    }

    public GrobidMedicalReportRestProcessGeneric getRestProcessGeneric() {
        return restProcessGeneric;
    }

    public void setRestProcessGeneric(GrobidMedicalReportRestProcessGeneric restProcessGeneric) {
        this.restProcessGeneric = restProcessGeneric;
    }

    public GrobidMedicalReportRestProcessString getRestProcessString() {
        return restProcessString;
    }

    public void setRestProcessString(GrobidMedicalReportRestProcessString restProcessString) {
        this.restProcessString = restProcessString;
    }

    public GrobidMedicalReportRestProcessFiles getRestProcessFiles() {
        return restProcessFiles;
    }

    public void setRestProcessFiles(GrobidMedicalReportRestProcessFiles restProcessFiles) {
        this.restProcessFiles = restProcessFiles;
    }
}
