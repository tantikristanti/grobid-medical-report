package org.grobid.service.process;

import com.google.inject.Singleton;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.MedicalEntity;
import org.grobid.core.document.TEIFormatter;
import org.grobid.core.engines.EngineMedical;
import org.grobid.core.factory.GrobidMedicalPoolingFactory;
import org.grobid.core.features.FeaturesVectorMedicalNER;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.service.util.GrobidRestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Web services consuming String
 */
@Singleton
public class GrobidMedicalReportRestProcessString {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportRestProcessString.class);

    @Inject
    public GrobidMedicalReportRestProcessString() {

    }

    /**
     * Parse a raw dateline and return the corresponding normalized dateline.
     *
     * @param dateline raw dateline string
     * @return a response object containing the structured xml representation of the dateline
     */
    public Response processDateline(String dateline) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        EngineMedical engine = null;
        try {
            LOGGER.debug(">> Set the raw dateline for stateless service'...");

            engine = EngineMedical.getEngine(true);
            dateline = dateline.replaceAll("\\n", " ").replaceAll("\\t", " ");
            //Dateline result = engine.processDateline(dateline);
            String result = engine.processDatelineAsRaw(dateline);
            if (result != null) {
                //retVal = "<dateline>\n" + result.toTEI() + "</dateline>\n";
                retVal = "<dateline>\n\t" + result + "\n</dateline>\n";
            }

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Parse a raw medic and return the corresponding normalized medic.
     *
     * @param medic raw medic string
     * @return a response object containing the structured xml representation of the medic
     */
    public Response processMedic(String medic) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        EngineMedical engine = null;
        try {
            LOGGER.debug(">> Set the raw medic for stateless service'...");

            engine = EngineMedical.getEngine(true);
            medic = medic.replaceAll("\\n", " ").replaceAll("\\t", " ");
            //Medic result = engine.processMedic(medic);
            String result = engine.processMedicAsRaw(medic);
            if (result != null) {
                //retVal = "<medic>\n" + result.toTEI() + "</medic>\n";
                retVal = "<medic>\n\t" + result + "\n</medic>\n";
            }

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Parse a raw patient and return the corresponding normalized patient.
     *
     * @param patient raw patient string
     * @return a response object containing the structured xml representation of the patient
     */
    public Response processPatient(String patient) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        EngineMedical engine = null;
        try {
            LOGGER.debug(">> Set the raw patient for stateless service'...");

            engine = EngineMedical.getEngine(true);
            patient = patient.replaceAll("\\n", " ").replaceAll("\\t", " ");
            //Patient result = engine.processPatient(patient);
            String result = engine.processPatientAsRaw(patient);
            if (result != null) {
                //retVal = "<patient>\n" + result.toTEI() + "</patient>\n";
                retVal = "<patient>\n\t" + result + "\n</patient>\n";
            }

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Parse a raw text and return the corresponding medical terminologies (NER).
     *
     * @param text raw text
     * @return a response object containing the structured xml representation of the medical terminologies (NER)
     */
    public Response processNER(String text) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        EngineMedical engine = null;
        try {
            LOGGER.debug(">> Set the raw patient for stateless service'...");
            StringBuilder result = new StringBuilder();
            engine = EngineMedical.getEngine(true);
            result = engine.processMedicalNERAsRaw(text);

            if (result != null) {
                retVal = "<listEntity>\n\t" + result.toString() + "\n</listEntity>\n";
            }

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    public String methodLogIn() {
        return ">> " + GrobidMedicalReportRestProcessString.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public String methodLogOut() {
        return "<< " + GrobidMedicalReportRestProcessString.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }
}
