
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
     * Parse a sequence of medics from a header, i.e. containing possibly
     * reference markers.
     *
     * @param medicSequence - the string corresponding to a raw sequence of names
     * @return the list of medics
     */
    public List<PersonMedical> processMedicsHeader(String medicSequence) throws Exception {
        List<PersonMedical> result = parsers.getMedicParser().processingHeader(medicSequence);
        return result;
    }

    /**
     * Apply a parsing model for the header of a PDF file based on CRF, using
     * first three pages of the PDF
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param result      bib result
     * @return the TEI representation of the extracted bibliographical
     * information
     */
    public String processHeaderMedicalReport(
        String inputFile,
        int consolidate,
        boolean includeRawAffiliations,
        HeaderMedicalItem result
    ) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .consolidateHeader(consolidate)
            .includeRawAffiliations(includeRawAffiliations)
            .build();
        return processHeaderMedicalReport(inputFile, config, result);
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
    public String processHeaderMedicalReport(String inputFile, int consolidate, HeaderMedicalItem resultHeader) {
        return processHeaderMedicalReport(inputFile, GrobidAnalysisConfig.defaultInstance(), resultHeader);
    }

    public String processHeaderMedicalReport(String inputFile, GrobidAnalysisConfig config, HeaderMedicalItem resultHeader) {
        // normally the medical items must not be null, but if it is the
        // case, we still continue
        // with a new instance, so that the resulting TEI string is still
        // delivered
        if (resultHeader == null) {
            resultHeader = new HeaderMedicalItem();
        }
        Pair<String, Document> resultTEI = parsers.getHeaderMedicalParser().processing(new File(inputFile), resultHeader, config);
        return resultTEI.getLeft();
    }

    @Override
    public synchronized void close() throws IOException {
        CrossrefClient.getInstance().close();
        parsers.close();
    }

}