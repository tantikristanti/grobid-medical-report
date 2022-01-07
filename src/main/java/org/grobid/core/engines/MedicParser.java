package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.Medic;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorDateline;
import org.grobid.core.features.FeaturesVectorMedic;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.counters.CntManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/*
* A class for parsing medics information
*
* Tanti, 2022
* 
* */

public class MedicParser extends AbstractParser {
    private static Logger LOGGER = LoggerFactory.getLogger(MedicParser.class);
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();

    public MedicParser() {
        super(GrobidModels.MEDIC);
    }

    public MedicParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.MEDIC, cntManager);
        this.parsers = parsers;
    }

    public MedicParser(EngineMedicalParsers parsers) {
        super(GrobidModels.MEDIC);
        this.parsers = parsers;
    }

    public List<Medic> process(String input) {
        // force English language for the tokenization only
        List<LayoutToken> tokens = analyzer.tokenizeWithLayoutToken(input);
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }

        return processCommon(tokens);
    }

    protected List<Medic> processCommon(List<LayoutToken> input) {
        if (CollectionUtils.isEmpty(input))
            return null;
        List<OffsetPosition> locationsPositions = lexicon.tokenPositionsLocationNames(input);
        List<OffsetPosition> titlePositions = lexicon.tokenPositionsPersonTitle(input);
        List<OffsetPosition> suffixPositions = lexicon.tokenPositionsPersonSuffix(input);
        List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(input);
        List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(input);

        try {
            String features = FeaturesVectorMedic.addFeaturesMedic(input, null, locationsPositions, titlePositions,
                suffixPositions, emailPositions, urlPositions);
            String res = label(features);

            // extract results from the processed file
            return resultExtractionLayoutTokens(res, input);
        } catch (Exception e) {
            throw new GrobidException("An exception on " + this.getClass().getName() + " occured while running Grobid.", e);
        }
    }
    
    /**
     * Extract results from a labeled sequence.
     *
     * @param result            result
     * @param tokenizations     list of tokens
     * @return list of medics
     */
    public List<Medic> resultExtractionLayoutTokens(String result,
                                       List<LayoutToken> tokenizations) {
        List<Medic> medics = new ArrayList<>();
        Medic medic = new Medic();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.MEDIC, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.MEDIC_ROLE)) {
                if (isNotBlank(medic.getRole())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setRole(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_NAME)) {
                if (isNotBlank(medic.getPersName())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setPersName(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_AFFILIATION)) {
                if (isNotBlank(medic.getAffiliation())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setAffiliation(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_ORGANISATION)) {
                if (isNotBlank(medic.getOrganisation())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setOrganisation(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_INSTITUTION)) {
                if (isNotBlank(medic.getInstitution())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setInstitution(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_ADDRESS)) {
                if (isNotBlank(medic.getAddress())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setAddress(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_COUNTRY)) {
                if (isNotBlank(medic.getCountry())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setCountry(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_TOWN)) {
                if (isNotBlank(medic.getTown())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setTown(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_EMAIL)) {
                if (isNotBlank(medic.getEmail())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setEmail(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_PHONE)) {
                if (isNotBlank(medic.getPhone())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setPhone(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_FAX)) {
                if (isNotBlank(medic.getFax())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setFax(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_WEB)) {
                if (isNotBlank(medic.getWeb())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setWeb(clusterContent);
                }
            } else if (clusterLabel.equals(MedicalLabels.MEDIC_NOTE)) {
                if (isNotBlank(medic.getNote())) {
                    if (medic.isNotNull()) {
                        medics.add(medic);
                        medic = new Medic();
                    }
                    medic.setNote(clusterContent);
                }
            }
        }
        return medics;
    }

    /**
     * Extract results from a list of medics strings in the training format
     * without any string modification.
     *
     * @param inputs list of input data
     * @return result
     */
    public StringBuilder trainingExtraction(List<String> inputs) {
        StringBuilder buffer = new StringBuilder();
        try {
            if (inputs == null)
                return null;

            if (inputs.size() == 0)
                return null;

            List<OffsetPosition> locationsPositions = null;
            List<OffsetPosition> titlePositions = null;
            List<OffsetPosition> suffixPositions = null;
            List<OffsetPosition> emailPositions = null;
            List<OffsetPosition> urlPositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;

                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                locationsPositions = lexicon.tokenPositionsLocationNames(tokenizations);
                titlePositions = lexicon.tokenPositionsPersonTitle(tokenizations);
                suffixPositions = lexicon.tokenPositionsPersonSuffix(tokenizations);
                emailPositions = lexicon.tokenPositionsEmailPattern(tokenizations);
                urlPositions = lexicon.tokenPositionsUrlPattern(tokenizations);

                String ress = FeaturesVectorMedic.addFeaturesMedic(tokenizations, null,
                        locationsPositions, titlePositions, suffixPositions, emailPositions, urlPositions);
                String res = label(ress);

                String lastTag = null;
                String lastTag0;
                String currentTag0 = null;
                boolean start = true;
                String s1 = null;
                String s2 = null;
                int p = 0;

                // extract results from the processed file
                StringTokenizer st = new StringTokenizer(res, "\n");
                while (st.hasMoreTokens()) {
                    boolean addSpace = false;
                    String tok = st.nextToken().trim();

                    if (tok.length() == 0) {
                        // new medic
                        start = true;
                        continue;
                    }
                    StringTokenizer stt = new StringTokenizer(tok, "\t");
                    int i = 0;

                    boolean newLine = false;
                    int ll = stt.countTokens();
                    while (stt.hasMoreTokens()) {
                        String s = stt.nextToken().trim();
                        if (i == 0) {
                            s2 = TextUtilities.HTMLEncode(s);
                            //s2 = s;

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).t();
                                if (tokOriginal.equals(" ")
                                        || tokOriginal.equals("\u00A0")) {
                                    addSpace = true;
                                } else if (tokOriginal.equals(s)) {
                                    strop = true;
                                }
                                p++;
                            }
                        } else if (i == ll - 1) {
                            s1 = s;
                        } 
                        i++;
                    }

                    if (start && (s1 != null)) {
                        buffer.append("\t<medic>");
                        start = false;
                    }

                    lastTag0 = null;
                    if (lastTag != null) {
                        if (lastTag.startsWith("I-")) {
                            lastTag0 = lastTag.substring(2, lastTag.length());
                        } else {
                            lastTag0 = lastTag;
                        }
                    }
                    if (s1 != null) {
                        if (s1.startsWith("I-")) {
                            currentTag0 = s1.substring(2, s1.length());
                        } else {
                            currentTag0 = s1;
                        }
                    }

                    if ((lastTag0 != null) && (currentTag0 != null))
                        testClosingTag(buffer, currentTag0, lastTag0);


                    String output = writeField(s1, lastTag0, s2, "<roleName>", "<roleName>", addSpace, 0);
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<persName>", "<persName>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<affiliation>", "<affiliation>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<orgName>", "<orgName>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<institution>", "<orgName>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<address>", "<address>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<country>", "<country>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<settlement>", "<settlement>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<email>", "<email>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<phone>", "<phone>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<fax>", "<fax>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<web>", "<ptr type=\"web\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note type=\"medic\">", addSpace, 0);
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
                    testClosingTag(buffer, currentTag0, lastTag0);
                    buffer.append("</medic>\n");
                }
            }
            
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(String s1, String lastTag0, String s2,
                              String field, String outField, boolean addSpace, int nbIndent) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";

                if (addSpace) {
                    result += " " + outField + s2;
                } else {
                    result += outField + s2;
                }
            }
        }
        return result;
    }

    private boolean testClosingTag(StringBuilder buffer, String currentTag0,
                                   String lastTag0) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("");
            } else if (lastTag0.equals("<roleName>")) {
                buffer.append("</roleName>");
            } else if (lastTag0.equals("<persName>")) {
                buffer.append("</persName>");
            } else if (lastTag0.equals("<affiliation>")) {
                buffer.append("</affiliation>");
            } else if (lastTag0.equals("<orgName>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<institution>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<address>")) {
                buffer.append("</address>");
            } else if (lastTag0.equals("<country>")) {
                buffer.append("</country>");
            } else if (lastTag0.equals("<settlement>")) {
                buffer.append("</settlement>");
            } else if (lastTag0.equals("<email>")) {
                buffer.append("</email>");
            } else if (lastTag0.equals("<phone>")) {
                buffer.append("</phone>");
            } else if (lastTag0.equals("<fax>")) {
                buffer.append("</fax>");
            } else if (lastTag0.equals("<web>")) {
                buffer.append("</ptr>");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>");
            } else {
                res = false;
            }

        }
        return res;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
