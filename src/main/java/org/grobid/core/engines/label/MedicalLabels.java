package org.grobid.core.engines.label;

import org.grobid.core.GrobidMedicalReportModels;

/**
 * Labels used in the medical-report-segmenter model
 * Tanti, 2020
 */
public class MedicalLabels extends TaggingLabels {

    MedicalLabels() {
        super();
    }

    // grobid-medical-report segmenter labels
    public final static String HEADER_LABEL = "<header>";
    public final static String HEADNOTE_LABEL = "<headnote>";
    public final static String FOOTNOTE_LABEL = "<footnote>";
    public final static String LEFTNOTE_LABEL = "<leftnote>";
    public final static String RIGHTNOTE_LABEL = "<rightnote>";
    public final static String BODY_LABEL = "<body>";
    public final static String PAGE_NUMBER_LABEL = "<page>";
    public final static String ACKNOWLEDGEMENT_LABEL = "<acknowledgement>";
    public static final String ANNEX_LABEL = "<annex>";

    // grobid-medical-report specific labels
    public final static String DOCNUM_LABEL = "<idno>";
    public final static String TITLE_LABEL = "<title>";
    public final static String DATE_LABEL = "<date>";
    public final static String DATELINE_LABEL = "<dateline>";
    public final static String TIME_LABEL = "<time>";
    public final static String MEDIC_LABEL = "<medic>";
    public final static String ROLE_LABEL = "<location>";
    public final static String PATIENT_LABEL = "<patient>";
    public final static String INSTITUTION_LABEL = "<institution>";
    public final static String AFFILIATION_LABEL = "<affiliation>";
    public final static String ADDRESS_LABEL = "<address>";
    public final static String ORG_ABEL = "<org>";
    public final static String LOCATION_LABEL = "<location>";
    public final static String PLACE_NAME_LABEL = "<placeName>";
    public final static String EMAIL_LABEL = "<email>";
    public final static String PHONE_LABEL = "<phone>";
    public final static String FAX_LABEL = "<fax>";
    public final static String WEB_LABEL = "<web>";
    public final static String OTHER_LABEL = "<other>";
    public final static String MARKER_LABEL = "<marker>";

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
    public static final TaggingLabel HEADER = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, HEADER_LABEL);
    public static final TaggingLabel HEADNOTE = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, HEADNOTE_LABEL);
    public static final TaggingLabel FOOTNOTE = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, FOOTNOTE_LABEL);
    public static final TaggingLabel LEFTNOTE = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, LEFTNOTE_LABEL);
    public static final TaggingLabel RIGHTNOTE = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, RIGHTNOTE_LABEL);
    public static final TaggingLabel BODY = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, BODY_LABEL);
    public static final TaggingLabel PAGE_NUMBER = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, PAGE_NUMBER_LABEL);
    public static final TaggingLabel ACKNOWLEDGEMENT = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, ACKNOWLEDGEMENT_LABEL);
    public static final TaggingLabel ANNEX = new TaggingLabelImpl(GrobidMedicalReportModels.MEDICAL_REPORT_SEGMENTER, ANNEX_LABEL);

    // Header Medical Report Model
    public static final TaggingLabel HEADER_DOCNUM = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, DOCNUM_LABEL);
    public static final TaggingLabel HEADER_TITLE = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, TITLE_LABEL);
    public static final TaggingLabel HEADER_DATE = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, DATE_LABEL);
    public static final TaggingLabel HEADER_TIME = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, TIME_LABEL);
    public static final TaggingLabel HEADER_DATELINE = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, DATELINE_LABEL);
    public static final TaggingLabel HEADER_MEDIC = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, MEDIC_LABEL);
    public static final TaggingLabel HEADER_PATIENT = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, PATIENT_LABEL);
    public static final TaggingLabel HEADER_AFFILIATION = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, AFFILIATION_LABEL);
    public static final TaggingLabel HEADER_ADDRESS = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, ADDRESS_LABEL);
    public static final TaggingLabel HEADER_ORG = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, ORG_ABEL);
    public static final TaggingLabel HEADER_EMAIL = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, EMAIL_LABEL);
    public static final TaggingLabel HEADER_PHONE = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, PHONE_LABEL);
    public static final TaggingLabel HEADER_FAX = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, FAX_LABEL);
    public static final TaggingLabel HEADER_WEB = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, WEB_LABEL);
    public static final TaggingLabel HEADER_DOCTYPE = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, DOCTYPE_LABEL);
    public static final TaggingLabel HEADER_OTHER = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, OTHER_LABEL);

    // Left-Note Medical Report Model
    public static final TaggingLabel LEFT_NOTE_DOCNUM = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, DOCNUM_LABEL);
    public static final TaggingLabel LEFT_NOTE_MEDIC = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, MEDIC_LABEL);
    public static final TaggingLabel LEFT_NOTE_AFFILIATION = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, AFFILIATION_LABEL);
    public static final TaggingLabel LEFT_NOTE_ADDRESS = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, ADDRESS_LABEL);
    public static final TaggingLabel LEFT_NOTE_ORG = new TaggingLabelImpl(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, ORG_ABEL);
    public static final TaggingLabel LEFT_NOTE_EMAIL = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, EMAIL_LABEL);
    public static final TaggingLabel LEFT_NOTE_PHONE = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, PHONE_LABEL);
    public static final TaggingLabel LEFT_NOTE_FAX = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, FAX_LABEL);
    public static final TaggingLabel LEFT_NOTE_WEB = new TaggingLabelImpl(GrobidMedicalReportModels.LEFT_NOTE_MEDICAL_REPORT, WEB_LABEL);

    // Body part

    // Medical personnel names
    public static final TaggingLabel NAMES_MEDIC_MARKER = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_MEDIC, MARKER_LABEL);
    public static final TaggingLabel NAMES_MEDIC_TITLE = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_MEDIC, TITLE_LABEL);
    public static final TaggingLabel NAMES_MEDIC_FORENAME = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_MEDIC, FORENAME_LABEL);
    public static final TaggingLabel NAMES_MEDIC_MIDDLENAME = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_MEDIC, MIDDLENAME_LABEL);
    public static final TaggingLabel NAMES_MEDIC_SURNAME = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_MEDIC, SURNAME_LABEL);
    public static final TaggingLabel NAMES_MEDIC_SUFFIX = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_MEDIC, SUFFIX_LABEL);

    // Names
    public static final TaggingLabel NAMES_PATIENT_MARKER = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_PATIENT, MARKER_LABEL);
    public static final TaggingLabel NAMES_PATIENT_TITLE = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_PATIENT, TITLE_LABEL);
    public static final TaggingLabel NAMES_PATIENT_FORENAME = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_PATIENT, FORENAME_LABEL);
    public static final TaggingLabel NAMES_PATIENT_MIDDLENAME = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_PATIENT, MIDDLENAME_LABEL);
    public static final TaggingLabel NAMES_PATIENT_SURNAME = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_PATIENT, SURNAME_LABEL);
    public static final TaggingLabel NAMES_PATIENT_SUFFIX = new TaggingLabelImpl(GrobidMedicalReportModels.NAME_PATIENT, SUFFIX_LABEL);

    static {
        register(HEADER);
        register(HEADNOTE);
        register(FOOTNOTE);
        register(LEFTNOTE);
        register(RIGHTNOTE);
        register(BODY);
        register(PAGE_NUMBER);
        register(ACKNOWLEDGEMENT);
        register(ANNEX);
    }

}
