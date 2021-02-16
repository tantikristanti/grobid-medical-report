package org.grobid.core.engines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * A list of parser for the grobid-medical-report sub-project
 */
public class EngineMedicalParsers implements Closeable {
    public static final Logger LOGGER = LoggerFactory.getLogger(EngineMedicalParsers.class);

    private MedicalReportParser medicalReportParser = null;
    private HeaderMedicalParser headerMedicalParser = null;
    private LeftNoteMedicalParser leftNoteMedicalParser = null;
    private MedicParser medicParser = null;
    private AffiliationAddressParser affiliationAddressParser = null;
    private PersonMedicalParser personParser = null;
    private DateParser dateParser = null;
    //private BodyMedicalParser bodyMedicalParser = null;

    public MedicalReportParser getMedicalReportParser() {
        if (medicalReportParser == null) {
            synchronized (this) {
                if (medicalReportParser == null) {
                    medicalReportParser = new MedicalReportParser();
                }
            }
        }
        return medicalReportParser;
    }

    public HeaderMedicalParser getHeaderMedicalParser() {
        if (headerMedicalParser == null) {
            synchronized (this) {
                if (headerMedicalParser == null) {
                    headerMedicalParser = new HeaderMedicalParser(this);
                }
            }
        }
        return headerMedicalParser;
    }

    public LeftNoteMedicalParser getLeftNoteMedicalParser() {
        if (leftNoteMedicalParser == null) {
            synchronized (this) {
                if (leftNoteMedicalParser == null) {
                    leftNoteMedicalParser = new LeftNoteMedicalParser(this);
                }
            }
        }
        return leftNoteMedicalParser;
    }

    public MedicParser getMedicParser() {
        if (medicParser == null) {
            synchronized (this) {
                if (medicParser == null) {
                    medicParser = new MedicParser();
                }
            }
        }
        return medicParser;
    }

    public PersonMedicalParser getPersonParser() {
        if (personParser == null) {
            synchronized (this) {
                if (personParser == null) {
                    personParser = new PersonMedicalParser();
                }
            }
        }
        return personParser;
    }

    public AffiliationAddressParser getAffiliationAddressParser() {
        if (affiliationAddressParser == null) {
            synchronized (this) {
                if (affiliationAddressParser == null) {
                    affiliationAddressParser = new AffiliationAddressParser();
                }
            }
        }
        return affiliationAddressParser;
    }

    public DateParser getDateParser() {
        if (dateParser == null) {
            synchronized (this) {
                if (dateParser == null) {
                    dateParser = new DateParser();
                }
            }
        }
        return dateParser;
    }

    /*public BodyMedicalParser getBodyMedicaltParser() {
        if (bodyMedicalParser == null) {
            synchronized (this) {
                if (bodyMedicalParser == null) {
                    bodyMedicalParser = new BodyMedicalParser(this);
                }
            }
        }

        return bodyMedicalParser;
    }*/

    /**
     * Init all model, this will also load the model into memory
     */
    public void initAll() {
        medicalReportParser = getMedicalReportParser();
        affiliationAddressParser = getAffiliationAddressParser();
        // personParser = getPersonParser();
        headerMedicalParser = getHeaderMedicalParser();
        leftNoteMedicalParser = getLeftNoteMedicalParser();
        dateParser = getDateParser();
        // bodyMedicalParser = getBodyMedicaltParser();
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("==> Closing all resources...");

        if (medicalReportParser != null) {
            medicalReportParser.close();
            medicalReportParser = null;
            LOGGER.debug("CLOSING medicalReportSegmenterParser");
        }

        if (headerMedicalParser != null) {
            headerMedicalParser.close();
            headerMedicalParser = null;
            LOGGER.debug("CLOSING headerMedicalParser");
        }

        if (leftNoteMedicalParser != null) {
            leftNoteMedicalParser.close();
            leftNoteMedicalParser = null;
            LOGGER.debug("CLOSING leftNoteMedicalParser");
        }

        if (affiliationAddressParser != null) {
            affiliationAddressParser.close();
            affiliationAddressParser = null;
            LOGGER.debug("CLOSING affiliationAddressParser");
        }

        /*if (personParser != null) {
            personParser.close();
            personParser = null;
            LOGGER.debug("CLOSING personParser");
        }*/

        if (dateParser != null) {
            dateParser.close();
            dateParser = null;
            LOGGER.debug("CLOSING dateParser");
        }

        /*if (bodyMedicalParser != null) {
            bodyMedicalParser.close();
            bodyMedicalParser = null;
            LOGGER.debug("CLOSING bodyMedicalParser");
        }*/

        LOGGER.debug("==> All resources closed");

    }
}
