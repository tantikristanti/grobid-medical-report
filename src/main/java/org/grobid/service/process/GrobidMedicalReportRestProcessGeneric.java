package org.grobid.service.process;

import com.google.inject.Inject;
import org.grobid.core.data.SoftwareInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

public class GrobidMedicalReportRestProcessGeneric {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportRestProcessGeneric.class);

    @Inject
    public GrobidMedicalReportRestProcessGeneric() {
    }

    /**
     * Returns a string containing true, if the service is alive.
     *
     * @return returns a response object containing the string true if service
     * is alive.
     */
    public static Response isAlive() {
        Response response = null;
        try {
            LOGGER.debug("Called isAlive()...");

            String retVal = null;
            try {
                retVal = Boolean.valueOf(true).toString();
            } catch (Exception e) {
                LOGGER.error("grobid-medical-report service is not active. ", e);
                retVal = Boolean.valueOf(false).toString();
            }
            response = Response.status(Status.OK).entity(retVal).build();
        } catch (Exception e) {
            LOGGER.error("Exception occurred while check if the service is alive. " + e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }

    public static String getVersion() {
        return SoftwareInformation.getInstance().toJson();
    }

    /**
     * Returns the description of grobid-medical-report
     *
     * @return a response object containing a description
     */
    public Response getDescription() {
        Response response = null;

        LOGGER.debug("called getDescription_html()...");

        String htmlCode = "<h4>grobid-medical-report documentation</h4>" +
            "This service provides a RESTful interface for using the grobid-medical-system system. " +
            "grobid-medical-system  extracts data from French medical PDF files.";

        response = Response.status(Status.OK).entity(htmlCode)
            .type(MediaType.TEXT_HTML).build();

        return response;
    }

}
