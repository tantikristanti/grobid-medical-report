package org.grobid.service.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.grobid.core.data.HeaderMedicalItem;
import org.grobid.core.data.LeftNoteMedicalItem;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.EngineMedical;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidMedicalPoolingFactory;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.KeyGen;
import org.grobid.service.exceptions.GrobidServiceException;
import org.grobid.service.util.GrobidRestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Web services consuming a file
 */
@Singleton
public class GrobidMedicalReportRestProcessFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidMedicalReportRestProcessFiles.class);

    @Inject
    public GrobidMedicalReportRestProcessFiles() {

    }

    /**
     * Upload PDF document as input and retrieve extracted header data in TEI/XML format as output
     *
     * @param inputStream the data of origin document
     * @return a response object which contains a TEI representation of the header part
     */
    public Response processHeaderDocument(final InputStream inputStream) {
        LOGGER.debug(methodLogIn());
        String retVal = null;
        Response response = null;
        File originFile = null;
        EngineMedical engine = null;
        try {
            engine = EngineMedical.getEngine(true);
            // if there is no engine in the pool, an Exception is thrown
            if (engine == null) {
                throw new GrobidServiceException("No grobid-medical-report engine available", Status.SERVICE_UNAVAILABLE);
            }
            // digest the MD5 input
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            }

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            HeaderMedicalItem result = new HeaderMedicalItem();

            // process the Header part
            retVal = engine.processHeader(originFile.getAbsolutePath(), md5Str, result);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Upload PDF document as input and retrieve extracted left-note data in TEI/XML format as output
     *
     * @param inputStream the data of origin document
     * @return a response object which contains a TEI representation of the left-note part
     */
    public Response processLeftNoteDocument(final InputStream inputStream) {
        LOGGER.debug(methodLogIn());
        String retVal = null;
        Response response = null;
        File originFile = null;
        EngineMedical engine = null;
        try {
            engine = EngineMedical.getEngine(true);
            // if there is no engine in the pool, an Exception is thrown
            if (engine == null) {
                throw new GrobidServiceException("No grobid-medical-report engine available", Status.SERVICE_UNAVAILABLE);
            }
            // digest the MD5 input
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            }

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            LeftNoteMedicalItem result = new LeftNoteMedicalItem();

            // process the left-note part
            retVal = engine.processLeftNote(originFile.getAbsolutePath(), md5Str, result);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Upload PDF document as input and retrieve extracted data of full medical document in TEI/XML format as output
     *
     * @param inputStream the data of origin document
     * @return a response object which contains a TEI representation of the full medical document
     */
    public Response processFullMedicalText(final InputStream inputStream,
                                            final int startPage,
                                            final int endPage,
                                            final boolean generateIDs,
                                            final boolean segmentSentences,
                                            final List<String> teiCoordinates) throws Exception {
        LOGGER.debug(methodLogIn());

        String retVal = null;
        Response response = null;
        File originFile = null;
        EngineMedical engine = null;
        try {
            engine = EngineMedical.getEngine(true);
            // if there is no engine in the pool, an Exception is thrown
            if (engine == null) {
                throw new GrobidServiceException("No grobid-medical-report engine available", Status.SERVICE_UNAVAILABLE);
            }
            // digest the MD5 input
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            }

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .generateTeiCoordinates(teiCoordinates)
                    .withSentenceSegmentation(segmentSentences)
                    .build();

            retVal = engine.fullTextToTEI(originFile, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Upload PDF document as input and retrieve extracted French NER data in TEI/XML format as output
     *
     * @param inputStream the data of origin document
     * @return a response object which contains a TEI representation of the left-note part
     */
    public Response processFrenchNER(final InputStream inputStream) {
        LOGGER.debug(methodLogIn());
        String retVal = null;
        Response response = null;
        File originFile = null;
        EngineMedical engine = null;
        try {
            engine = EngineMedical.getEngine(true);
            // if there is no engine in the pool, an Exception is thrown
            if (engine == null) {
                throw new GrobidServiceException("No grobid-medical-report engine available", Status.SERVICE_UNAVAILABLE);
            }
            // digest the MD5 input
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            }

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // process the medical NER
            retVal = engine.processFrenchNER(originFile.getAbsolutePath(), md5Str);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidMedicalPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document which shall be extracted into TEI + assets in a ZIP
     * archive.
     *
     * @param inputStream          the data of origin document
     * @param consolidateHeader    the consolidation option allows GROBID to exploit Crossref
     *                             for improving header information
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref
     *                             for improving citations information
     * @param startPage            give the starting page to consider in case of segmentation of the
     *                             PDF, -1 for the first page (default)
     * @param endPage              give the end page to consider in case of segmentation of the
     *                             PDF, -1 for the last page (default)
     * @param generateIDs          if true, generate random attribute id on the textual elements of
     *                             the resulting TEI
     * @param segmentSentences     if true, return results with segmented sentences
     * @return a response object mainly contain the TEI representation of the
     * full text
     */
    public Response processStatelessFulltextAssetDocument(final InputStream inputStream,
                                                          final int consolidateHeader,
                                                          final int consolidateCitations,
                                                          final boolean includeRawAffiliations,
                                                          final boolean includeRawCitations,
                                                          final int startPage,
                                                          final int endPage,
                                                          final boolean generateIDs,
                                                          final boolean segmentSentences,
                                                          final List<String> teiCoordinates) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        String assetPath = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            }

            // set the path for the asset files
            assetPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .consolidateHeader(consolidateHeader)
                    .consolidateCitations(consolidateCitations)
                    .includeRawAffiliations(includeRawAffiliations)
                    .includeRawCitations(includeRawCitations)
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .generateTeiCoordinates(teiCoordinates)
                    .pdfAssetPath(new File(assetPath))
                    .withSentenceSegmentation(segmentSentences)
                    .build();

            retVal = engine.fullTextToTEI(originFile, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {

                response = Response.status(Status.OK).type("application/zip").build();

                ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(ouputStream);
                out.putNextEntry(new ZipEntry("tei.xml"));
                out.write(retVal.getBytes(Charset.forName("UTF-8")));
                // put now the assets, i.e. all the files under the asset path
                File assetPathDir = new File(assetPath);
                if (assetPathDir.exists()) {
                    File[] files = assetPathDir.listFiles();
                    if (files != null) {
                        byte[] buffer = new byte[1024];
                        for (final File currFile : files) {
                            if (currFile.getName().toLowerCase().endsWith(".jpg")
                                || currFile.getName().toLowerCase().endsWith(".png")) {
                                try {
                                    ZipEntry ze = new ZipEntry(currFile.getName());
                                    out.putNextEntry(ze);
                                    FileInputStream in = new FileInputStream(currFile);
                                    int len;
                                    while ((len = in.read(buffer)) > 0) {
                                        out.write(buffer, 0, len);
                                    }
                                    in.close();
                                    out.closeEntry();
                                } catch (IOException e) {
                                    throw new GrobidServiceException("IO Exception when zipping", e, Status.INTERNAL_SERVER_ERROR);
                                }
                            }
                        }
                    }
                }
                out.finish();

                response = Response
                    .ok()
                    .type("application/zip")
                    .entity(ouputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"result.zip\"")
                    .build();
                out.close();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (assetPath != null) {
                IOUtilities.removeTempDirectory(assetPath);
            }

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    public String methodLogIn() {
        return ">> " + GrobidMedicalReportRestProcessFiles.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public String methodLogOut() {
        return "<< " + GrobidMedicalReportRestProcessFiles.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }
}
