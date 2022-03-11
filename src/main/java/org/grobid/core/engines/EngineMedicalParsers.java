package org.grobid.core.engines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A list of parser for the grobid-medical-report sub-project
 */
public class EngineMedicalParsers extends EngineParsers {
    public static final Logger LOGGER = LoggerFactory.getLogger(EngineMedicalParsers.class);

    private MedicalReportSegmenterParser medicalReportSegmenterParser = null;
    private HeaderMedicalParser headerMedicalParser = null;
    private LeftNoteMedicalParser leftNoteMedicalParser = null;
    private FullMedicalTextParser fullTextParser = null;
    private FrenchMedicalNERParser frenchMedicalNERParser = null;
    private FrMedicalNERParser frMedicalNERParser = null;
    private AffiliationAddressParser affiliationAddressParser = null;
    private DatelineParser datelineParser = null;
    private DateParser dateParser = null;
    private OrganizationParser organizationParser = null;
    private MedicParser medicParser = null;
    private PatientParser patientParser = null;
    private PersonNameParser namePersonParser = null;
    private NEREnParser nerParser = null;
    private NERFrParser nerFrParser = null;

    public MedicalReportSegmenterParser getMedicalReportSegmenterParser() {
        if (medicalReportSegmenterParser == null) {
            synchronized (this) {
                if (medicalReportSegmenterParser == null) {
                    medicalReportSegmenterParser = new MedicalReportSegmenterParser();
                }
            }
        }
        return medicalReportSegmenterParser;
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

    public FullMedicalTextParser getFullMedicalTextParser() {
        if (fullTextParser == null) {
            synchronized (this) {
                if (fullTextParser == null) {
                    fullTextParser = new FullMedicalTextParser(this);
                }
            }
        }
        return fullTextParser;
    }

    public FrenchMedicalNERParser getFrenchMedicalNERParser() {
        if (frenchMedicalNERParser == null) {
            synchronized (this) {
                if (frenchMedicalNERParser == null) {
                    frenchMedicalNERParser = new FrenchMedicalNERParser(this);
                }
            }
        }
        return frenchMedicalNERParser;
    }

    public FrMedicalNERParser getFrMedicalNERParser() {
        if (frMedicalNERParser == null) {
            synchronized (this) {
                if (frMedicalNERParser == null) {
                    frMedicalNERParser = new FrMedicalNERParser(this);
                }
            }
        }
        return frMedicalNERParser;
    }

    public NEREnParser getNerParser() {
        if (nerParser == null) {
            synchronized (this) {
                if (nerParser == null) {
                    nerParser = new NEREnParser();
                }
            }
        }
        return  nerParser;
    }

    public NERFrParser getNerFrParser() {
        if (nerFrParser == null) {
            synchronized (this) {
                if (nerFrParser == null) {
                    nerFrParser = new NERFrParser();
                }
            }
        }
        return nerFrParser;
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

    public DatelineParser getDatelineParser() {
        if (datelineParser == null) {
            synchronized (this) {
                if (datelineParser == null) {
                    datelineParser = new DatelineParser();
                }
            }
        }
        return datelineParser;
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

    public OrganizationParser getOrganizationParser() {
        if (organizationParser == null) {
            synchronized (this) {
                if (organizationParser == null) {
                    organizationParser = new OrganizationParser();
                }
            }
        }
        return organizationParser;
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

    public PatientParser getPatientParser() {
        if (patientParser == null) {
            synchronized (this) {
                if (patientParser == null) {
                    patientParser = new PatientParser();
                }
            }
        }
        return patientParser;
    }

    public PersonNameParser getPersonNameParser() {
        if (namePersonParser == null) {
            synchronized (this) {
                if (namePersonParser == null) {
                    namePersonParser = new PersonNameParser();
                }
            }
        }
        return namePersonParser;
    }

    /**
     * Init all model, this will also load the model into memory
     */
    public void initAll() {
        medicalReportSegmenterParser = getMedicalReportSegmenterParser();
        headerMedicalParser = getHeaderMedicalParser();
        leftNoteMedicalParser = getLeftNoteMedicalParser();
        fullTextParser = getFullMedicalTextParser();
        frenchMedicalNERParser = getFrenchMedicalNERParser();
        affiliationAddressParser = getAffiliationAddressParser();
        datelineParser = getDatelineParser();
        dateParser = getDateParser();
        medicParser = getMedicParser();
        patientParser = getPatientParser();
        namePersonParser = getPersonNameParser();
        nerParser = getNerParser();
        nerFrParser = getNerFrParser();
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("==> Closing all resources...");

        if (medicalReportSegmenterParser != null) {
            medicalReportSegmenterParser.close();
            medicalReportSegmenterParser = null;
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

        if (fullTextParser != null) {
            fullTextParser.close();
            fullTextParser = null;
            LOGGER.debug("CLOSING fullMedicalTextParser");
        }

        if (frenchMedicalNERParser != null) {
            frenchMedicalNERParser.close();
            frenchMedicalNERParser = null;
            LOGGER.debug("CLOSING frenchMedicalNERParser");
        }

        if (affiliationAddressParser != null) {
            affiliationAddressParser.close();
            affiliationAddressParser = null;
            LOGGER.debug("CLOSING affiliationAddressParser");
        }

        if (datelineParser != null) {
            datelineParser.close();
            datelineParser = null;
            LOGGER.debug("CLOSING datelineParser");
        }

        if (dateParser != null) {
            dateParser.close();
            dateParser = null;
            LOGGER.debug("CLOSING dateParser");
        }

        if (medicParser != null) {
            medicParser.close();
            medicParser = null;
            LOGGER.debug("CLOSING medicParser");
        }

        if (patientParser != null) {
            patientParser.close();
            patientParser = null;
            LOGGER.debug("CLOSING patientParser");
        }

        if (nerParser != null) {
            nerParser.close();
            nerParser = null;
            LOGGER.debug("CLOSING nerParser");
        }

        if (nerFrParser != null) {
            nerFrParser.close();
            nerFrParser = null;
            LOGGER.debug("CLOSING nerFrParser");
        }

        LOGGER.debug("==> All resources closed");

    }
}
