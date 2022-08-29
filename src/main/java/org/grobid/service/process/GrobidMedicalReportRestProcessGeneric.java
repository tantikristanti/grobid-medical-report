package org.grobid.service.process;

import com.google.inject.Inject;
import org.grobid.core.data.SoftwareInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static String isAlive() {
        String retVal = "";
        try {
            retVal = Boolean.valueOf(true).toString();
        } catch (Exception e) {
            LOGGER.error("grobid-medical-report service is not active. ", e);
            retVal = Boolean.valueOf(false).toString();
        }
        return retVal;
    }

    public static String getVersion() {
        return SoftwareInformation.getInstance().toJson();
    }

    /**
     * Returns the description of grobid-medical-report
     *
     * @return a response object containing a description
     */
    public String getDescription() {
        String htmlCode = "<h4>grobid-medical-report documentation</h4>" +
            "This service provides a RESTful interface for using the grobid-medical-system system. " +
            "grobid-medical-system  extracts data from French medical PDF files.";
        return htmlCode;
    }
}
