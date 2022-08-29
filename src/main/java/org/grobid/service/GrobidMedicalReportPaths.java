package org.grobid.service;

/**
 * Interface contains path extensions for accessing the grobid-medical-report service.
 *
 */
public interface GrobidMedicalReportPaths {
    /**
     * root path
     * http://localhost:8090
     */
    String ROOT = "/";

    /**
     * path extension for is alive request.
     * http://localhost:8090/api/isalive
     */
    String PATH_IS_ALIVE = "isalive";

    /**
     * path extension for getting version
     * http://localhost:8090/api/version
     */
    String PATH_GET_VERSION = "version";

    /**
     * path extension for getting description
     * http://localhost:8090/api/grobidMedicalReport
     */
    String PATH_GET_DESCRIPTION = "grobidMedicalReport";

    /**
     * path extension for processing datelines.
     * $ curl -X POST -d "dateline=[TEXT]" localhost:8090/api/processDateline
     */
    String PATH_DATELINE = "processDateline";

    /**
     * path extension for processing medics.
     * $ curl -X POST -d "medic=[TEXT]" localhost:8090/api/processMedic
     */
    String PATH_MEDIC = "processMedic";

    /**
     * path extension for processing patients.
     * $ curl -X POST -d "patient=[TEXT]" localhost:8090/api/processPatient
     */
    String PATH_PATIENT = "processPatient";

    /**
     * path extension for processing French medical NER.
     * $ curl -X POST -d "ner=[TEXT]" localhost:8090/api/processNER
     */
    String PATH_MEDICAL_NER = "processNER";

    /**
	 * path extension for processing document headers.
	 */
	String PATH_HEADER = "processHeaderDocument";

	/**
	 * path extension for processing document left-notes.
	 */
	String PATH_LEFT_NOTE = "processLeftNoteDocument";

    /**
     * path extension for processing French Medical NER.
     */
    String PATH_FRENCH_MEDICAL_NER= "processFrenchMedicalNER";
	
	/**
	 * path extension for processing full text of documents.
	 */
	String PATH_FULL_MEDICAL_TEXT = "processFullMedicalText";

	/**
	 * path extension for processing full text of documents together with image extraction.
	 */
	String PATH_FULL_MEDICAL_TEXT_ASSET = "processFullMedicalTextAssetDocument";

	/**
	 * path extension for processing and annotating a PDF file.
	 */
	String PATH_PDF_ANNOTATION = "annotatePDF";
}
