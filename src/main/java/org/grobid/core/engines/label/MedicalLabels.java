package org.grobid.core.engines.label;

import org.grobid.core.GrobidMedicalReportModels;
import org.grobid.core.GrobidModels;

/**
 * Labels used in the medical-report-segmenter model
 * Tanti, 2020
 */
public class MedicalLabels extends TaggingLabels {

    MedicalLabels() {
        super();
    }

    // grobid-medical-report segmenter labels
    public final static String TITLE_PAGE_LABEL = "<titlePage>";
    public final static String HEADER_LABEL = "<header>";
    public final static String HEADNOTE_LABEL = "<headnote>";
    public final static String FOOTNOTE_LABEL = "<footnote>";
    public final static String MARGINNOTE_LABEL = "<marginnote>";
    public final static String LEFTNOTE_LABEL = "<leftnote>";
    public final static String RIGHTNOTE_LABEL = "<rightnote>";
    public final static String BODY_LABEL = "<body>";
    public final static String PAGE_NUMBER_LABEL = "<page>";
    public final static String ACKNOWLEDGEMENT_LABEL = "<acknowledgement>";
    public static final String ANNEX_LABEL = "<annex>";

    // grobid-medical-report specific labels
    public final static String ID_NUMBER_LABEL = "<idno>";
    public final static String DOCNUM_LABEL = "<docnum>";
    public static final String DOCTYPE_LABEL = "<doctype>";
    public final static String TITLE_LABEL = "<title>";
    public final static String DATE_LABEL = "<date>";
    public final static String DATELINE_LABEL = "<dateline>";
    public final static String TIME_LABEL = "<time>";
    public final static String NOTE_LABEL = "<note>";
    public final static String MEDIC_LABEL = "<medic>";
    public final static String PATIENT_LABEL = "<patient>";
    public final static String AFFILIATION_LABEL = "<affiliation>";
    public final static String ADDRESS_LABEL = "<address>";
    public static final String INSTITUTION_LABEL = "<institution>";
    public final static String ORG_LABEL = "<org>";
    public static final String ORG_NAME_LABEL = "<orgName>";
    public final static String EMAIL_LABEL = "<email>";
    public final static String PHONE_LABEL = "<phone>";
    public final static String FAX_LABEL = "<fax>";
    public final static String WEB_LABEL = "<web>";
    public static final String PLACE_NAME_LABEL = "<place>";
    public static final String SETTLEMENT_LABEL = "<settlement>"; // the name of a settlement such as a city, town, or village
    public static final String COUNTRY_LABEL = "<country>";
    public static final String REGION_LABEL = "<region>"; // the name of an administrative unit such as a state, province, or county, larger than a settlement, but smaller than a country

    // full medical text
    public static final String PARAGRAPH_LABEL = "<paragraph>";
    public static final String ITEM_LABEL = "<item>";
    public static final String SECTION_LABEL = "<section>";
    public static final String SUB_SECTION_LABEL = "<subsection>";
    public static final String FIGURE_MARKER_LABEL = "<figure_marker>";
    public static final String FIGURE_LABEL = "<figure>";
    public static final String TABLE_MARKER_LABEL = "<table_marker>";
    public static final String TABLE_LABEL = "<table>";

    // person (medics, patient)
    public static final String ROLE_LABEL = "<roleName>";
    public static final String PERSON_NAME_LABEL = "<persName>";
    public static final String PERSON_SEX_LABEL = "<sex>";
    public static final String PERSON_BIRTH_LABEL = "<birth>";
    public static final String PERSON_DEATH_LABEL = "<death>";
    
    // names
    public final static String FORENAME_LABEL = "<forename>";
    public final static String MIDDLENAME_LABEL = "<middlename>";
    public final static String SURNAME_LABEL = "<surname>";
    public final static String SUFFIX_LABEL = "<suffix>";

    /**
     * document header (<header>): front,
     * page header (<headnote>): note type headnote,
     * page footer (<footnote>): note type footnote,
     * left note (<leftnote>): note type left,
     * right note (<rightnote>): note type right,
     * document body (<body>): body,
     * page number (<page>): page,
     * acknowledgement (<acknowledgment>): acknowledgement,
     * other (<other>): other
     */

    // Medical Report Segmenter Model
    public static final TaggingLabel TITLE_PAGE = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, TITLE_PAGE_LABEL);
    public static final TaggingLabel HEADER = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, HEADER_LABEL);
    public static final TaggingLabel HEADNOTE = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, HEADNOTE_LABEL);
    public static final TaggingLabel FOOTNOTE = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, FOOTNOTE_LABEL);
    public static final TaggingLabel MARGINNOTE = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, MARGINNOTE_LABEL);
    public static final TaggingLabel LEFTNOTE = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, LEFTNOTE_LABEL);
    public static final TaggingLabel RIGHTNOTE = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, RIGHTNOTE_LABEL);
    public static final TaggingLabel BODY = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, BODY_LABEL);
    public static final TaggingLabel PAGE_NUMBER = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, PAGE_NUMBER_LABEL);
    public static final TaggingLabel ACKNOWLEDGEMENT = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, ACKNOWLEDGEMENT_LABEL);
    public static final TaggingLabel ANNEX = new TaggingLabelImpl(GrobidModels.MEDICAL_REPORT_SEGMENTER, ANNEX_LABEL);

    // Header Medical Report Model
    public static final TaggingLabel HEADER_DOCNUM = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, DOCNUM_LABEL);
    public static final TaggingLabel HEADER_TITLE = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, TITLE_LABEL);
    public static final TaggingLabel HEADER_DATE = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, DATE_LABEL);
    public static final TaggingLabel HEADER_TIME = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, TIME_LABEL);
    public static final TaggingLabel HEADER_DATELINE = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, DATELINE_LABEL);
    public static final TaggingLabel HEADER_MEDIC = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, MEDIC_LABEL);
    public static final TaggingLabel HEADER_PATIENT = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, PATIENT_LABEL);
    public static final TaggingLabel HEADER_AFFILIATION = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, AFFILIATION_LABEL);
    public static final TaggingLabel HEADER_ADDRESS = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, ADDRESS_LABEL);
    public static final TaggingLabel HEADER_ORG = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, ORG_LABEL);
    public static final TaggingLabel HEADER_EMAIL = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, EMAIL_LABEL);
    public static final TaggingLabel HEADER_PHONE = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, PHONE_LABEL);
    public static final TaggingLabel HEADER_FAX = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, FAX_LABEL);
    public static final TaggingLabel HEADER_WEB = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, WEB_LABEL);
    public static final TaggingLabel HEADER_DOCTYPE = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, DOCTYPE_LABEL);
    public static final TaggingLabel HEADER_NOTE = new TaggingLabelImpl(GrobidModels.HEADER_MEDICAL_REPORT, NOTE_LABEL);

    // Left-Note Medical Report Model
    public static final TaggingLabel LEFT_NOTE_DOCNUM = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, DOCNUM_LABEL);
    public static final TaggingLabel LEFT_NOTE_MEDIC = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, MEDIC_LABEL);
    public static final TaggingLabel LEFT_NOTE_AFFILIATION = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, AFFILIATION_LABEL);
    public static final TaggingLabel LEFT_NOTE_ADDRESS = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, ADDRESS_LABEL);
    public static final TaggingLabel LEFT_NOTE_ORG = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, ORG_LABEL);
    public static final TaggingLabel LEFT_NOTE_EMAIL = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, EMAIL_LABEL);
    public static final TaggingLabel LEFT_NOTE_PHONE = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, PHONE_LABEL);
    public static final TaggingLabel LEFT_NOTE_FAX = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, FAX_LABEL);
    public static final TaggingLabel LEFT_NOTE_WEB = new TaggingLabelImpl(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, WEB_LABEL);

    // Full medical model
    public static final TaggingLabel TABLE_MARKER = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, TABLE_MARKER_LABEL);
    public static final TaggingLabel FIGURE_MARKER = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, FIGURE_MARKER_LABEL);
    public static final TaggingLabel PARAGRAPH = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, PARAGRAPH_LABEL);
    public static final TaggingLabel ITEM = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, ITEM_LABEL);
    public static final TaggingLabel TITLE = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, TITLE_LABEL);
    public static final TaggingLabel SECTION = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, SECTION_LABEL);
    public static final TaggingLabel SUB_SECTION = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, SUB_SECTION_LABEL);
    public static final TaggingLabel FIGURE = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, FIGURE_LABEL);
    public static final TaggingLabel TABLE = new TaggingLabelImpl(GrobidModels.FULL_MEDICAL_TEXT, TABLE_LABEL);

    // Medical personnel names
    public static final TaggingLabel NAMES_MEDIC_TITLE = new TaggingLabelImpl(GrobidModels.NAMES_MEDIC, TITLE_LABEL);
    public static final TaggingLabel NAMES_MEDIC_FORENAME = new TaggingLabelImpl(GrobidModels.NAMES_MEDIC, FORENAME_LABEL);
    public static final TaggingLabel NAMES_MEDIC_MIDDLENAME = new TaggingLabelImpl(GrobidModels.NAMES_MEDIC, MIDDLENAME_LABEL);
    public static final TaggingLabel NAMES_MEDIC_SURNAME = new TaggingLabelImpl(GrobidModels.NAMES_MEDIC, SURNAME_LABEL);
    public static final TaggingLabel NAMES_MEDIC_SUFFIX = new TaggingLabelImpl(GrobidModels.NAMES_MEDIC, SUFFIX_LABEL);

    // Dateline
    public static final TaggingLabel DATELINE_PLACE_NAME = new TaggingLabelImpl(GrobidModels.DATELINE, PLACE_NAME_LABEL);
    public static final TaggingLabel DATELINE_DATE = new TaggingLabelImpl(GrobidModels.DATELINE, DATE_LABEL);
    public static final TaggingLabel DATELINE_TIME = new TaggingLabelImpl(GrobidModels.DATELINE, TIME_LABEL);
    public static final TaggingLabel DATELINE_NOTE = new TaggingLabelImpl(GrobidModels.DATELINE, NOTE_LABEL);

    // Medics
    public static final TaggingLabel MEDIC_ROLE = new TaggingLabelImpl(GrobidModels.MEDIC, ROLE_LABEL);
    public static final TaggingLabel MEDIC_NAME = new TaggingLabelImpl(GrobidModels.MEDIC, PERSON_NAME_LABEL);
    public static final TaggingLabel MEDIC_AFFILIATION = new TaggingLabelImpl(GrobidModels.MEDIC, AFFILIATION_LABEL);
    public static final TaggingLabel MEDIC_ORGANISATION = new TaggingLabelImpl(GrobidModels.MEDIC, ORG_NAME_LABEL);
    public static final TaggingLabel MEDIC_INSTITUTION = new TaggingLabelImpl(GrobidModels.MEDIC, INSTITUTION_LABEL);
    public static final TaggingLabel MEDIC_ADDRESS = new TaggingLabelImpl(GrobidModels.MEDIC, ADDRESS_LABEL);
    public static final TaggingLabel MEDIC_COUNTRY = new TaggingLabelImpl(GrobidModels.MEDIC, COUNTRY_LABEL);
    public static final TaggingLabel MEDIC_TOWN = new TaggingLabelImpl(GrobidModels.MEDIC, SETTLEMENT_LABEL);
    public static final TaggingLabel MEDIC_EMAIL = new TaggingLabelImpl(GrobidModels.MEDIC, EMAIL_LABEL);
    public static final TaggingLabel MEDIC_PHONE = new TaggingLabelImpl(GrobidModels.MEDIC, PHONE_LABEL);
    public static final TaggingLabel MEDIC_FAX = new TaggingLabelImpl(GrobidModels.MEDIC, FAX_LABEL);
    public static final TaggingLabel MEDIC_WEB = new TaggingLabelImpl(GrobidModels.MEDIC, WEB_LABEL);
    public static final TaggingLabel MEDIC_NOTE = new TaggingLabelImpl(GrobidModels.MEDIC, NOTE_LABEL);

    // Patient
    public static final TaggingLabel PATIENT_ID = new TaggingLabelImpl(GrobidModels.PATIENT, ID_NUMBER_LABEL);
    public static final TaggingLabel PATIENT_NAME = new TaggingLabelImpl(GrobidModels.PATIENT, PERSON_NAME_LABEL);
    public static final TaggingLabel PATIENT_SEX = new TaggingLabelImpl(GrobidModels.PATIENT, PERSON_SEX_LABEL);
    public static final TaggingLabel PATIENT_DATE_BIRTH = new TaggingLabelImpl(GrobidModels.PATIENT, PERSON_BIRTH_LABEL);
    public static final TaggingLabel PATIENT_DATE_DEATH = new TaggingLabelImpl(GrobidModels.PATIENT, PERSON_DEATH_LABEL);
    public static final TaggingLabel PATIENT_ADDRESS = new TaggingLabelImpl(GrobidModels.PATIENT, ADDRESS_LABEL);
    public static final TaggingLabel PATIENT_COUNTRY = new TaggingLabelImpl(GrobidModels.PATIENT, COUNTRY_LABEL);
    public static final TaggingLabel PATIENT_TOWN = new TaggingLabelImpl(GrobidModels.PATIENT, SETTLEMENT_LABEL);
    public static final TaggingLabel PATIENT_PHONE = new TaggingLabelImpl(GrobidModels.PATIENT, PHONE_LABEL);
    public static final TaggingLabel PATIENT_NOTE = new TaggingLabelImpl(GrobidModels.PATIENT, NOTE_LABEL);

    // Names
    public static final TaggingLabel NAMES_PATIENT_TITLE = new TaggingLabelImpl(GrobidModels.NAMES_PATIENT, TITLE_LABEL);
    public static final TaggingLabel NAMES_PATIENT_FORENAME = new TaggingLabelImpl(GrobidModels.NAMES_PATIENT, FORENAME_LABEL);
    public static final TaggingLabel NAMES_PATIENT_MIDDLENAME = new TaggingLabelImpl(GrobidModels.NAMES_PATIENT, MIDDLENAME_LABEL);
    public static final TaggingLabel NAMES_PATIENT_SURNAME = new TaggingLabelImpl(GrobidModels.NAMES_PATIENT, SURNAME_LABEL);
    public static final TaggingLabel NAMES_PATIENT_SUFFIX = new TaggingLabelImpl(GrobidModels.NAMES_PATIENT, SUFFIX_LABEL);

    static {
        // medical-report-segmenter
        register(TITLE_PAGE);
        register(HEADER);
        register(HEADNOTE);
        register(FOOTNOTE);
        register(MARGINNOTE);
        register(LEFTNOTE);
        register(RIGHTNOTE);
        register(BODY);
        register(PAGE_NUMBER);
        register(ACKNOWLEDGEMENT);
        register(ANNEX);

        // header
        register(HEADER_DOCNUM);
        register(HEADER_TITLE);
        register(HEADER_DATE);
        register(HEADER_TIME);
        register(HEADER_DATELINE);
        register(HEADER_MEDIC);
        register(HEADER_PATIENT);
        register(HEADER_AFFILIATION);
        register(HEADER_ADDRESS);
        register(HEADER_ORG);
        register(HEADER_EMAIL);
        register(HEADER_PHONE);
        register(HEADER_FAX);
        register(HEADER_WEB);
        register(HEADER_DOCTYPE);

        // left-note
        register(LEFT_NOTE_DOCNUM);
        register(LEFT_NOTE_MEDIC);
        register(LEFT_NOTE_AFFILIATION);
        register(LEFT_NOTE_ADDRESS);
        register(LEFT_NOTE_ORG);
        register(LEFT_NOTE_EMAIL);
        register(LEFT_NOTE_PHONE);
        register(LEFT_NOTE_FAX);
        register(LEFT_NOTE_WEB);

        // full-medical-report
        register(TABLE_MARKER);
        register(FIGURE_MARKER);
        register(PARAGRAPH);
        register(ITEM);
        register(TITLE);
        register(SECTION);
        register(SUB_SECTION);
        register(FIGURE);
        register(TABLE);

        // dateline
        register(DATELINE_PLACE_NAME);
        register(DATELINE_DATE);
        register(DATELINE_TIME);
        register(DATELINE_NOTE);

        // medic
        register(MEDIC_ROLE);
        register(MEDIC_NAME);
        register(MEDIC_AFFILIATION);
        register(MEDIC_ORGANISATION);
        register(MEDIC_INSTITUTION);
        register(MEDIC_ADDRESS);
        register(MEDIC_COUNTRY);
        register(MEDIC_TOWN);
        register(MEDIC_EMAIL);
        register(MEDIC_PHONE);
        register(MEDIC_FAX);
        register(MEDIC_WEB);
        register(MEDIC_NOTE);

        // patient
        register(PATIENT_ID);
        register(PATIENT_SEX);
        register(PATIENT_NAME);
        register(PATIENT_DATE_BIRTH);
        register(PATIENT_DATE_DEATH);
        register(PATIENT_ADDRESS);
        register(PATIENT_COUNTRY);
        register(PATIENT_TOWN);
        register(PATIENT_PHONE);
        register(PATIENT_NOTE);
    }
}
