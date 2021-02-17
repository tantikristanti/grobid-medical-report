package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidMedicalReportModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.PersonMedical;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorName;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.PDFAnnotation;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * A class to parse the medical personnel names.
 * Tanti, 2020
 */
public class MedicParser {
    private static Logger LOGGER = LoggerFactory.getLogger(MedicParser.class);
    private final GenericTagger namesMedicParser;

    public MedicParser() {
        namesMedicParser = TaggerFactory.getTagger(GrobidMedicalReportModels.NAME_MEDIC);
    }
    
    /**
     * Processing of medics names in header
     */
    public List<PersonMedical> processingHeader(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        input = input.trim().replaceAll("et\\.? al\\.?.*$", " ");

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens, null, true);
    }

    public List<PersonMedical> processingHeaderWithLayoutTokens(List<LayoutToken> inputs, List<PDFAnnotation> pdfAnnotations) {
        return processing(inputs, pdfAnnotations, true);
    }

    /**
     * Common processing of medics in header
     *
     * @param tokens list of LayoutToken object to process
     * @param head - if true use the model for header's name, otherwise the model for names in citation
     * @return List of identified Person entites as POJO.
     */
    public List<PersonMedical> processing(List<LayoutToken> tokens, List<PDFAnnotation> pdfAnnotations, boolean head) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        List<PersonMedical> fullMedics = null;
        try {
            List<OffsetPosition> titlePositions = Lexicon.getInstance().tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = Lexicon.getInstance().tokenPositionsPersonSuffix(tokens);

            String sequence = FeaturesVectorName.addFeaturesName(tokens, null,
                titlePositions, suffixPositions);
            if (StringUtils.isEmpty(sequence))
                return null;
            GenericTagger tagger = namesMedicParser ;
            String res = tagger.label(sequence);
//System.out.println(res);
            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidMedicalReportModels.NAME_MEDIC, res, tokens);
            PersonMedical medic = new PersonMedical();
            boolean newMarker = false;
            String currentMarker = null;
            List<TaggingTokenCluster> clusters = clusteror.cluster();
            for (TaggingTokenCluster cluster : clusters) {
                if (cluster == null) {
                    continue;
                }

                if(pdfAnnotations != null) {
                    for (LayoutToken medicsToken : cluster.concatTokens()) {
                        for (PDFAnnotation pdfAnnotation : pdfAnnotations) {
                            BoundingBox intersectBox = pdfAnnotation.getIntersectionBox(medicsToken);
                            if (intersectBox != null) {
                                BoundingBox medicsBox = BoundingBox.fromLayoutToken(medicsToken);
                                if (intersectBox.equals(medicsBox)) {

                                } else {
                                    double pixPerChar = medicsToken.getWidth() / medicsToken.getText().length();
                                    int charsCovered = (int) ((intersectBox.getWidth() / pixPerChar) + 0.5);
                                    if (pdfAnnotation.getDestination() != null && pdfAnnotation.getDestination().length() > 0) {
                                        Matcher orcidMatcher = TextUtilities.ORCIDPattern.matcher(pdfAnnotation.getDestination());
                                        if (orcidMatcher.find()) {
                                            // !! here we consider the annot is at the tail or end of the names
                                            String newToken = medicsToken.getText().substring(0, medicsToken.getText().length() - charsCovered);
                                            medicsToken.setText(newToken);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TaggingLabel clusterLabel = cluster.getTaggingLabel();
                Engine.getCntManager().i(clusterLabel);
                //String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
                String clusterContent = StringUtils.normalizeSpace(LayoutTokensUtil.toText(cluster.concatTokens()));
                if (clusterContent.trim().length() == 0)
                    continue;
                if (clusterLabel.equals(MedicalLabels.NAMES_MEDIC_MARKER)) {
                    // a marker introduces a new medic, and the marker could be attached to the previous (usual)
                    // or following medic (rare)
                    currentMarker = clusterContent;
                    newMarker = true;
                    boolean markerAssigned = false;
                    if (medic.notNull()) {
                        if (fullMedics == null) {
                            fullMedics = new ArrayList<>();
                        }
                        medic.addMarker(currentMarker);
                        markerAssigned = true;

                        if (!fullMedics.contains(medic)) {
                            fullMedics.add(medic);
                            medic = new PersonMedical();
                        }
                    }
                    if (!markerAssigned) {
                        medic.addMarker(currentMarker);
                    }
                } else if (clusterLabel.equals(MedicalLabels.NAMES_MEDIC_TITLE)) {
                    if (newMarker) {
                        medic.setTitle(clusterContent);
                        newMarker = false;
                    } else if (medic.getTitle() != null) {
                        if (medic.notNull()) {
                            if (fullMedics == null)
                                fullMedics = new ArrayList<>();
                            fullMedics.add(medic);
                        }
                        medic = new PersonMedical();
                        medic.setTitle(clusterContent);
                    } else {
                        medic.setTitle(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(MedicalLabels.NAMES_MEDIC_FORENAME)) {
                    if (newMarker) {
                        medic.setFirstName(clusterContent);
                        newMarker = false;
                    } else if (medic.getFirstName() != null) {
                        // new medic
                        if (medic.notNull()) {
                            if (fullMedics == null)
                                fullMedics = new ArrayList<>();
                            fullMedics.add(medic);
                        }
                        medic = new PersonMedical();
                        medic.setFirstName(clusterContent);
                    } else {
                        medic.setFirstName(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(MedicalLabels.NAMES_MEDIC_MIDDLENAME)) {
                    if (newMarker) {
                        medic.setMiddleName(clusterContent);
                        newMarker = false;
                    } else if (medic.getMiddleName() != null) {
                        medic.setMiddleName(medic.getMiddleName() + " " + clusterContent);
                    } else {
                        medic.setMiddleName(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(MedicalLabels.NAMES_MEDIC_SURNAME)) {
                    if (newMarker) {
                        medic.setLastName(clusterContent);
                        newMarker = false;
                    } else if (medic.getLastName() != null) {
                        // new medic
                        if (medic.notNull()) {
                            if (fullMedics == null)
                                fullMedics = new ArrayList<>();
                            fullMedics.add(medic);
                        }
                        medic = new PersonMedical();
                        medic.setLastName(clusterContent);
                    } else {
                        medic.setLastName(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(MedicalLabels.NAMES_MEDIC_SUFFIX)) {
                    if (medic.getSuffix() != null) {
                        medic.setSuffix(medic.getSuffix() + " " + clusterContent);
                    } else {
                        medic.setSuffix(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
            }

            // add last built medic
            if (medic.notNull()) {
                if (fullMedics == null) {
                    fullMedics = new ArrayList<PersonMedical>();
                }
                fullMedics.add(medic);
            }

            // some more person name normalisation
            if (fullMedics != null) {
                for(PersonMedical medics : fullMedics) {
                    medics.normalizeName();
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return fullMedics;
    }

    private boolean nameLabel(String label) {
        return label.endsWith("<surname>") || label.endsWith("<forename>") || label.endsWith("<middlename>");
    }

    /**
     * Extract results from a list of name strings in the training format without any string modification.
     *
     * @param input - the sequence of medic names to be processed as a string.
     * @param head - if true use the model for header's name, otherwise the model for names in citation
     * @return the pseudo-TEI training data
     */
    public StringBuilder trainingExtraction(String input,
                                            boolean head) {
        if (StringUtils.isEmpty(input))
            return null;
        // force analyser with English, to avoid bad surprise
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        StringBuilder buffer = new StringBuilder();
        try {
            if (CollectionUtils.isEmpty(tokens)) {
                return null;
            }

            List<OffsetPosition> titlePositions = Lexicon.getInstance().tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = Lexicon.getInstance().tokenPositionsPersonSuffix(tokens);

            String sequence = FeaturesVectorName.addFeaturesName(tokens, null, titlePositions, suffixPositions);
            if (StringUtils.isEmpty(sequence))
                return null;
            GenericTagger tagger = namesMedicParser;
            String res = tagger.label(sequence);

            // extract results from the processed file
            StringTokenizer st2 = new StringTokenizer(res, "\n");
            String lastTag = null;
            boolean start = true;
            boolean hasMarker = false;
            boolean hasSurname = false;
            boolean hasForename = false;
            boolean tagClosed;
            int q = 0;
            boolean addSpace;
            String lastTag0;
            String currentTag0;
            while (st2.hasMoreTokens()) {
                String line = st2.nextToken();
                addSpace = false;
                if ((line.trim().length() == 0)) {
                    // new medic
                    if (head)
                        buffer.append("/t<medic>\n");
                    continue;
                } else {
                    String theTok = tokens.get(q).getText();
                    while (theTok.equals(" ") || theTok.equals("\n")) {
                        addSpace = true;
                        q++;
                        theTok = tokens.get(q).getText();
                    }
                    q++;
                }

                StringTokenizer st3 = new StringTokenizer(line, "\t");
                int ll = st3.countTokens();
                int i = 0;
                String s1 = null;
                String s2 = null;
                boolean newLine = false;
                List<String> localFeatures = new ArrayList<String>();
                while (st3.hasMoreTokens()) {
                    String s = st3.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // string
                    } else if (i == ll - 2) {
                    } else if (i == ll - 1) {
                        s1 = s; // label
                    } else {
                        localFeatures.add(s);
                        if (s.equals("LINESTART") && !start) {
                            newLine = true;
                            start = false;
                        } else if (s.equals("LINESTART")) {
                            start = false;
                        }
                    }
                    i++;
                }

                lastTag0 = null;
                if (lastTag != null) {
                    if (lastTag.startsWith("I-")) {
                        lastTag0 = lastTag.substring(2, lastTag.length());
                    } else {
                        lastTag0 = lastTag;
                    }
                }
                currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                tagClosed = lastTag0 != null && testClosingTag(buffer, currentTag0, lastTag0, head);

                if (newLine) {
                    if (tagClosed) {
                        buffer.append("\t\t\t\t\t\t\t<lb/>\n");
                    } else {
                        buffer.append("<lb/>");
                    }

                }

                String output = writeField(s1, lastTag0, s2, "<marker>", "<marker>", addSpace, 8, head);
                if (output != null) {
                    if (hasMarker) {
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t</persName>\n");
                        }
                        hasForename = false;
                        hasSurname = false;
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t<persName>\n");
                        }
                        hasMarker = true;
                    }
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                } else {
                    output = writeField(s1, lastTag0, s2, "<other>", "<other>", addSpace, 8, head);
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<forename>", "<forename>", addSpace, 8, head);
                } else {
                    if (buffer.length() > 0) {
                        if (buffer.charAt(buffer.length() - 1) == '\n') {
                            buffer.deleteCharAt(buffer.length() - 1);
                        }
                    }
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<middlename>", "<middlename>", addSpace, 8, head);
                } else {
                    if (hasForename && !currentTag0.equals(lastTag0)) {
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t</persName>\n");
                        }
                        hasMarker = false;
                        hasSurname = false;
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t<persName>\n");
                        }
                    }
                    hasForename = true;
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<surname>", "<surname>", addSpace, 8, head);
                } else {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<title>", "<roleName>", addSpace, 8, head);
                } else {
                    if (hasSurname && !currentTag0.equals(lastTag0)) {
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t</persName>\n");
                        }
                        hasMarker = false;
                        hasForename = false;
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t<persName>\n");
                        }
                    }
                    hasSurname = true;
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<suffix>", "<suffix>", addSpace, 8, head);
                } else {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output != null) {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }

                lastTag = s1;
            }

            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
                currentTag0 = "";
                testClosingTag(buffer, currentTag0, lastTag0, head);
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(String s1,
                              String lastTag0,
                              String s2,
                              String field,
                              String outField,
                              boolean addSpace,
                              int nbIndent,
                              boolean head) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if ((s1.equals("<other>") || s1.equals("I-<other>"))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else if ((s1.equals(lastTag0) || s1.equals("I-" + lastTag0))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                if (head) {
                    for (int i = 0; i < nbIndent; i++) {
                        result += "\t";
                    }
                }
                if (addSpace)
                    result += " " + outField + s2;
                else
                    result += outField + s2;
            }
        }
        return result;
    }

    private boolean testClosingTag(StringBuilder buffer,
                                   String currentTag0,
                                   String lastTag0,
                                   boolean head) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                if (head)
                    buffer.append("\n");
            } else if (lastTag0.equals("<forename>")) {
                buffer.append("</forename>");
                if (head)
                    buffer.append("\n");
            } else if (lastTag0.equals("<middlename>")) {
                buffer.append("</middlename>");
                if (head)
                    buffer.append("\n");
            } else if (lastTag0.equals("<surname>")) {
                buffer.append("</surname>");
                if (head)
                    buffer.append("\n");
            } else if (lastTag0.equals("<title>")) {
                buffer.append("</roleName>");
                if (head)
                    buffer.append("\n");
            } else if (lastTag0.equals("<suffix>")) {
                buffer.append("</suffix>");
                if (head)
                    buffer.append("\n");
            } else if (lastTag0.equals("<marker>")) {
                buffer.append("</marker>");
                if (head)
                    buffer.append("\n");
            } else {
                res = false;
            }

        }
        return res;
    }

    public void close() throws IOException {
    }
}