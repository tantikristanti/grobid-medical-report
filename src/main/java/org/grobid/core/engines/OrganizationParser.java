package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Organization;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorOrganization;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.counters.CntManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A class for parsing organizations found in medical reports.
 * Tanti, 2020
 */
public class OrganizationParser extends AbstractParser {
    private static Logger LOGGER = LoggerFactory.getLogger(OrganizationParser.class);
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();
    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();

    public OrganizationParser() {
        super(GrobidModels.ORGANIZATION);
    }

    public OrganizationParser(EngineMedicalParsers parsers) {
        super(GrobidModels.ORGANIZATION);
        this.parsers = parsers;
    }

    public OrganizationParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.ORGANIZATION, cntManager);
        this.parsers = parsers;
    }

    /**
     * Organization processing
     */
    public List<Organization> processing(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens);
    }

    public List<Organization> processingWithLayoutTokens(List<LayoutToken> inputs) {
        return processing(inputs);
    }

    /**
     * Common processing of medics (mostly for the header part, but it can be used in other parts, such as for the body part)
     *
     * @param tokens list of LayoutToken object to process
     * @return List of identified Medic entities as POJO.
     */
    public List<Organization> processing(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        List<Organization> fullOrganization = new ArrayList<>();
        Organization organization = null;
        try {
            List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
            List<OffsetPosition> titlePositions = lexicon.tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = lexicon.tokenPositionsPersonSuffix(tokens);
            List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
            List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);
            // get the features for the organization
            String sequence = FeaturesVectorOrganization.addFeaturesOrganization(tokens, null,
                locationPositions, titlePositions, suffixPositions, emailPositions, urlPositions);

            if (StringUtils.isEmpty(sequence))
                return null;
            // labelling the featured data
            String res = label(sequence);
            //System.out.println(res);

            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.ORGANIZATION, res, tokens);
            organization = new Organization();

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

                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_NAME)) {
                    if (organization.getOrgName() != null) {
                        organization.setOrgName(organization.getOrgName() + "\t" + clusterContent);
                    } else {
                        organization.setOrgName(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_ADDRESS)) {
                    if (organization.getAddress() != null) {
                        organization.setAddress(organization.getAddress() + "\t" + clusterContent);
                    } else {
                        organization.setAddress(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_COUNTRY)) {
                    if (organization.getCountry() != null) {
                        organization.setCountry(organization.getCountry() + "\t" + clusterContent);
                    } else {
                        organization.setCountry(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_TOWN)) {
                    if (organization.getTown() != null) {
                        organization.setTown(organization.getTown() + "\t" + clusterContent);
                    } else {
                        organization.setTown(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_EMAIL)) {
                    if (organization.getEmail() != null) {
                        organization.setEmail(organization.getEmail() + "\t" + clusterContent);
                    } else {
                        organization.setEmail(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_PHONE)) {
                    if (organization.getPhone() != null) {
                        organization.setPhone(organization.getPhone() + "\t" + clusterContent);
                    } else {
                        organization.setPhone(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_FAX)) {
                    if (organization.getFax() != null) {
                        organization.setFax(organization.getFax() + "\t" + clusterContent);
                    } else {
                        organization.setFax(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_WEB)) {
                    if (organization.getWeb() != null) {
                        organization.setWeb(organization.getWeb() + "\t" + clusterContent);
                    } else {
                        organization.setWeb(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ORGANIZATION_NOTE)) {
                    if (organization.getNote() != null) {
                        organization.setNote(organization.getNote() + "\t" + clusterContent);
                    } else {
                        organization.setNote(clusterContent);
                    }
                    organization.addLayoutTokens(cluster.concatTokens());
                }
                if (organization != null) {
                    fullOrganization.add(organization);
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return fullOrganization;
    }

    /**
     * Extract organization strings in the training format
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

                // add with features
                String featuresOrganization = FeaturesVectorOrganization.addFeaturesOrganization(tokenizations, null,
                    locationsPositions, titlePositions, suffixPositions, emailPositions, urlPositions);
                // labeling
                String res = label(featuresOrganization);

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
                        // new organization
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
                        //buffer.append("\t<org>");
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


                    String output = writeField(s1, lastTag0, s2, "<ghu>", "<orgName type=\"ghu\">", addSpace, 0);
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<chu>", "<orgName type=\"chu\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<dmu>", "<orgName type=\"dmu\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<pole>", "<orgName type=\"pole\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<site>", "<orgName type=\"site\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<institution>", "<orgName type=\"institution\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<university>", "<orgName type=\"university\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<hospital>", "<orgName type=\"hospital\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<center>", "<orgName type=\"center\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<service>", "<orgName type=\"service\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<department>", "<orgName type=\"department\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<unit>", "<orgName type=\"unit\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<organization>", "<orgName type=\"other\">", addSpace, 0);
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
                        output = writeField(s1, lastTag0, s2, "<note>", "<note type=\"organization\">", addSpace, 0);
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
                    //buffer.append("</org>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(String s1, String lastTag0, String s2,
                              String field, String outField, boolean addSpace, int nbIndent) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) { // if the current tag is the same as the previous tag
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                for (int i = 0; i < nbIndent; i++) {
                    result += "\t";
                }
                // if the current tag is different from the previous tag, we add a carriage return
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
            } else if (lastTag0.equals("<ghu>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<chu>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<dmu>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<pole>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<site>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<institution>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<university>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<hospital>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<center>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<service>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<department>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<unit>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<organization>")) {
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
