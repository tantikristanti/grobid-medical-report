package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Medic;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorMedic;
import org.grobid.core.lang.Language;
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

    /**
     * Processing of medics in the header part
     */
    public List<Medic> processing(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens);
    }


    public List<Medic> processingWithLayoutTokens(List<LayoutToken> inputs) {
        return processing(inputs);
    }

    /**
     * Common processing of medics (mostly for the header part, but it can be used in other parts, such as for the body part)
     *
     * @param tokens list of LayoutToken object to process
     * @return List of identified Medic entities as POJO.
     */
    public List<Medic> processing(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        List<Medic> fullMedics = new ArrayList<>();
        Medic medic = null;
        try {
            List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
            List<OffsetPosition> titlePositions = lexicon.tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = lexicon.tokenPositionsPersonSuffix(tokens);
            List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
            List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);
            // get the features for the medic
            String sequence = FeaturesVectorMedic.addFeaturesMedic(tokens, null,
                locationPositions, titlePositions, suffixPositions, emailPositions, urlPositions);

            if (StringUtils.isEmpty(sequence))
                return null;
            // labelling the featured data
            String res = label(sequence);
            //System.out.println(res);

            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.MEDIC, res, tokens);
            medic = new Medic();

            List<TaggingTokenCluster> clusters = clusteror.cluster();
            for (TaggingTokenCluster cluster : clusters) {
                if (cluster == null) {
                    continue;
                }

                TaggingLabel clusterLabel = cluster.getTaggingLabel();
                Engine.getCntManager().i(clusterLabel);
                String clusterContent = StringUtils.normalizeSpace(LayoutTokensUtil.toText(cluster.concatTokens()));
                String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
                if (clusterContent.trim().length() == 0)
                    continue;

                if (clusterLabel.equals(MedicalLabels.MEDIC_ID)) {
                    if (medic.getIdno() != null) {
                        medic.setIdno(medic.getIdno() + "\t" + clusterContent);
                    } else {
                        medic.setIdno(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_ROLE)) {
                    if (medic.getRole() != null) {
                        medic.setRole(medic.getRole() + "\t" + clusterContent);
                    } else {
                        medic.setRole(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_NAME)) {
                    if (medic.getPersName() != null) {
                        medic.setPersName(medic.getPersName() + "\t" + clusterContent);
                    } else {
                        medic.setPersName(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_AFFILIATION)) {
                    if (medic.getAffiliation() != null) {
                        medic.setAffiliation(medic.getAffiliation() + "\t" + clusterContent);
                    } else {
                        medic.setAffiliation(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_ORGANISATION) || (clusterLabel.equals(MedicalLabels.MEDIC_SERVICE)) ||
                    (clusterLabel.equals(MedicalLabels.MEDIC_CENTER)) || (clusterLabel.equals(MedicalLabels.MEDIC_ADMINISTRATION))) {
                    if (medic.getOrganisation() != null) {
                        medic.setOrganisation(medic.getOrganisation() + "\t" + clusterContent);
                    } else {
                        medic.setOrganisation(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_INSTITUTION)) {
                    if (medic.getInstitution() != null) {
                        medic.setInstitution(medic.getInstitution() + "\t" + clusterContent);
                    } else {
                        medic.setInstitution(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_ADDRESS)) {
                    if (medic.getAddress() != null) {
                        medic.setAddress(medic.getAddress() + "\t" + clusterContent);
                    } else {
                        medic.setAddress(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_COUNTRY)) {
                    if (medic.getCountry() != null) {
                        medic.setCountry(medic.getCountry() + "\t" + clusterContent);
                    } else {
                        medic.setCountry(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_TOWN)) {
                    if (medic.getTown() != null) {
                        medic.setTown(medic.getTown() + "\t" + clusterContent);
                    } else {
                        medic.setTown(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_EMAIL)) {
                    if (medic.getEmail() != null) {
                        medic.setEmail(medic.getEmail() + "\t" + clusterContent);
                    } else {
                        medic.setEmail(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_PHONE)) {
                    if (medic.getPhone() != null) {
                        medic.setPhone(medic.getPhone() + "\t" + clusterContent);
                    } else {
                        medic.setPhone(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_FAX)) {
                    if (medic.getFax() != null) {
                        medic.setFax(medic.getFax() + "\t" + clusterContent);
                    } else {
                        medic.setFax(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_WEB)) {
                    if (medic.getWeb() != null) {
                        medic.setWeb(medic.getFax() + "\t" + clusterContent);
                    } else {
                        medic.setWeb(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.MEDIC_NOTE)) {
                    if (medic.getNote() != null) {
                        medic.setNote(medic.getNote() + "\t" + clusterContent);
                    } else {
                        medic.setNote(clusterContent);
                    }
                    medic.addLayoutTokens(cluster.concatTokens());
                }
                if (medic != null){
                    fullMedics.add(medic);
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return fullMedics;
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
            if (clusterLabel.equals(MedicalLabels.MEDIC_ID)) {
                if (medic.getIdno() != null) {
                    medic.setIdno(medic.getIdno() + "\t" + clusterContent);
                } else {
                    medic.setIdno(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_ROLE)) {
                if (medic.getRole() != null) {
                    medic.setRole(medic.getRole() + "\t" + clusterContent);
                } else {
                    medic.setRole(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_NAME)) {
                if (medic.getPersName() != null) {
                    medic.setPersName(medic.getPersName() + "\t" + clusterContent);
                } else {
                    medic.setPersName(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_AFFILIATION)) {
                if (medic.getAffiliation() != null) {
                    medic.setAffiliation(medic.getAffiliation() + "\t" + clusterContent);
                } else {
                    medic.setAffiliation(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_ORGANISATION) || (clusterLabel.equals(MedicalLabels.MEDIC_SERVICE)) ||
                (clusterLabel.equals(MedicalLabels.MEDIC_CENTER)) || (clusterLabel.equals(MedicalLabels.MEDIC_ADMINISTRATION))) {
                if (medic.getOrganisation() != null) {
                    medic.setOrganisation(medic.getOrganisation() + "\t" + clusterContent);
                } else {
                    medic.setOrganisation(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_INSTITUTION)) {
                if (medic.getInstitution() != null) {
                    medic.setInstitution(medic.getInstitution() + "\t" + clusterContent);
                } else {
                    medic.setInstitution(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_ADDRESS)) {
                if (medic.getAddress() != null) {
                    medic.setAddress(medic.getAddress() + "\t" + clusterContent);
                } else {
                    medic.setAddress(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_COUNTRY)) {
                if (medic.getCountry() != null) {
                    medic.setCountry(medic.getCountry() + "\t" + clusterContent);
                } else {
                    medic.setCountry(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_TOWN)) {
                if (medic.getTown() != null) {
                    medic.setTown(medic.getTown() + "\t" + clusterContent);
                } else {
                    medic.setTown(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_EMAIL)) {
                if (medic.getEmail() != null) {
                    medic.setEmail(medic.getEmail() + "\t" + clusterContent);
                } else {
                    medic.setEmail(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_PHONE)) {
                if (medic.getPhone() != null) {
                    medic.setPhone(medic.getPhone() + "\t" + clusterContent);
                } else {
                    medic.setPhone(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_FAX)) {
                if (medic.getFax() != null) {
                    medic.setFax(medic.getFax() + "\t" + clusterContent);
                } else {
                    medic.setFax(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_WEB)) {
                if (medic.getWeb() != null) {
                    medic.setWeb(medic.getFax() + "\t" + clusterContent);
                } else {
                    medic.setWeb(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.MEDIC_NOTE)) {
                if (medic.getNote() != null) {
                    medic.setNote(medic.getNote() + "\t" + clusterContent);
                } else {
                    medic.setNote(clusterContent);
                }
                medic.addLayoutTokens(cluster.concatTokens());
            }
            if (medic != null){
                medics.add(medic);
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


                    String output = writeField(s1, lastTag0, s2, "<idno>", "<idno>", addSpace, 0);
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<roleName>", "<roleName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<persname>", "<persName>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<affiliation>", "<affiliation>", addSpace, 0);
                    } 
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<orgname>", "<orgName>", addSpace, 0);
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
            } else if (lastTag0.equals("<idno>")) {
                buffer.append("</idno>");
            } else if (lastTag0.equals("<rolename>")) {
                buffer.append("</roleName>");
            } else if (lastTag0.equals("<persname>")) {
                buffer.append("</persName>");
            } else if (lastTag0.equals("<affiliation>")) {
                buffer.append("</affiliation>");
            } else if (lastTag0.equals("<orgname>")) {
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

    /**
     * In the context of field extraction, check if a newly extracted content is not redundant
     * with the already extracted content
     */
    private boolean isDifferentContent(String existingContent, String newContent) {
        if (existingContent == null) {
            return true;
        }
        if (newContent == null) {
            return false;
        }
        String newContentSimplified = newContent.toLowerCase();
        newContentSimplified = newContentSimplified.replace(" ", "").trim();
        String existinContentSimplified = existingContent.toLowerCase();
        existinContentSimplified = existinContentSimplified.replace(" ", "").trim();
        if (newContentSimplified.equals(existinContentSimplified))
            return false;
        else
            return true;
    }

    /**
     * In the context of field extraction, this variant of the previous method check if a newly
     * extracted content is not redundant globally and as any substring combination with the already
     * extracted content
     */
    private boolean isDifferentandNotIncludedContent(String existingContent, String newContent) {
        if (existingContent == null) {
            return true;
        }
        if (newContent == null) {
            return false;
        }
        String newContentSimplified = newContent.toLowerCase();
        newContentSimplified = newContentSimplified.replace(" ", "").trim();
        newContentSimplified = newContentSimplified.replace("-", "").trim();
        String existingContentSimplified = existingContent.toLowerCase();
        existingContentSimplified = existingContentSimplified.replace(" ", "").trim();
        existingContentSimplified = existingContentSimplified.replace("-", "").trim();
        if (newContentSimplified.equals(existingContentSimplified) ||
            existingContentSimplified.contains(newContentSimplified)
        )
            return false;
        else
            return true;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
