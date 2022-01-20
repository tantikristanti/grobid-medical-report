
package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.HeaderMedicalItem;
import org.grobid.core.data.LeftNoteMedicalItem;
import org.grobid.core.data.PersonMedical;
import org.grobid.core.document.Document;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.utilities.crossref.CrossrefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A class for managing the extraction of medical information from PDF documents or raw text.
 * This class extends the Engine class of Grobid (@author Patrice Lopez)
 */
public class EngineMedical extends Engine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngineMedical.class);

    private final EngineMedicalParsers parsers = new EngineMedicalParsers();

    // The list of accepted languages
    // the languages are encoded in ISO 3166
    // if null, all languages are accepted.
    private List<String> acceptedLanguages = null;

    /**
     * Constructor for the grobid-medical-report engine instance.
     *
     * @param loadModels
     */
    public EngineMedical(boolean loadModels) {
        super(loadModels);
    }

    /**
     * Apply a parsing model for the header of a PDF file based on CRF, using
     * first three pages of the PDF
     *
     * @param inputFile         the path of the PDF file to be processed
     * @param consolidate       the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                          information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                          metadata) or 2 (consolidate the citation and inject DOI only)
     * @param resultHeader      result from the header parser
     * @param resultLeftNote    result from the left-note parser
     * @return the TEI representation of the extracted header and left-note items
     * information
     */
    public String processHeaderLeftNoteMedicalReport(
        String inputFile,
        int consolidate,
        boolean includeRawAffiliations,
        HeaderMedicalItem resultHeader,
        LeftNoteMedicalItem resultLeftNote
    ) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .consolidateHeader(consolidate)
            .includeRawAffiliations(includeRawAffiliations)
            .build();
        return processHeaderLeftNoteMedicalReport(inputFile, config, resultHeader, resultLeftNote);
    }

    /**
     * Apply a parsing model for the header of a PDF file based on CRF, using
     * dynamic range of pages as header
     *
     * @param inputFile         : the path of the PDF file to be processed
     * @param resultHeader      : header item result
     * @return the TEI representation of the extracted bibliographical
     * information
     */
    public String processHeaderLeftNoteMedicalReport(String inputFile, int consolidate, HeaderMedicalItem resultHeader, LeftNoteMedicalItem resultLeftNote) {
        return processHeaderLeftNoteMedicalReport(inputFile, GrobidAnalysisConfig.defaultInstance(), resultHeader, resultLeftNote);
    }

    public String processHeaderLeftNoteMedicalReport(String inputFile, GrobidAnalysisConfig config, HeaderMedicalItem resultHeader , LeftNoteMedicalItem resultLeftNote) {
        // normally the medical items must not be null, but if it is the
        // case, we still continue
        // with a new instance, so that the resulting TEI string is still
        // delivered
        if (resultHeader == null) {
            resultHeader = new HeaderMedicalItem();
        }
        if (resultLeftNote == null) {
            resultLeftNote = new LeftNoteMedicalItem();
        }
        Pair<String, Document> resultTEI = parsers.getHeaderMedicalParser().processing(new File(inputFile), resultHeader, resultLeftNote, config);
        return resultTEI.getLeft();
    }

    @Override
    public synchronized void close() throws IOException {
        CrossrefClient.getInstance().close();
        parsers.close();
    }

}