package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.PersonName;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorPersonName;
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

/*A class for parsing person medical names
 *
 * Tanti, 2022
 * */

public class PersonNameParser extends AbstractParser {
	private static Logger LOGGER = LoggerFactory.getLogger(PersonNameParser.class);
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();

    public PersonNameParser() {
        super(GrobidModels.NAMES_PERSON_MEDICAL);
    }

    public PersonNameParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.NAMES_PERSON_MEDICAL, cntManager);
        this.parsers = parsers;
    }

    public PersonNameParser(EngineMedicalParsers parsers) {
        super(GrobidModels.NAMES_PERSON_MEDICAL);
        this.parsers = parsers;
    }

    /**
     * Processing of person's name
     */
    public PersonName process(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens);
    }

    public PersonName processingWithLayoutTokens(List<LayoutToken> inputs) {
        return processing(inputs);
    }

    /**
     * Common processing of patients (mostly for the header part, but it can be used in other parts, such as for the body part)
     *
     * @param tokens list of LayoutToken object to process
     * @return List of identified person name entities as POJO.
     */
    public PersonName processing(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        PersonName name = null;
        try {
            List<OffsetPosition> titlePositions = lexicon.tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = lexicon.tokenPositionsPersonSuffix(tokens);

            // get the features for the name
            String sequence = FeaturesVectorPersonName.addFeaturesName(tokens, null,
                titlePositions, suffixPositions);

            if (StringUtils.isEmpty(sequence))
                return null;
            // labelling the featured data
            String res = label(sequence);
            //System.out.println(res);

            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.NAMES_PERSON_MEDICAL, res, tokens);
            name = new PersonName();

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

                if (clusterLabel.equals(MedicalLabels.NAMES_TITLE)) {
                    if (name.getTitle() != null) {
                        name.setTitle(name.getTitle() + "\t" + clusterContent);
                    } else {
                        name.setTitle(clusterContent);
                    }
                    name.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.NAMES_FORENAME)) {
                    if (name.getFirstName() != null) {
                        name.setFirstName(name.getFirstName() + "\t" + clusterContent);
                    } else {
                        name.setFirstName(clusterContent);
                    }
                    name.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.NAMES_MIDDLENAME)) {
                    if (name.getMiddleName() != null) {
                        name.setMiddleName(name.getMiddleName() + "\t" + clusterContent);
                    } else {
                        name.setMiddleName(clusterContent);
                    }
                    name.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.NAMES_SURNAME)) {
                    if (name.getLastName() != null) {
                        name.setLastName(name.getLastName() + "\t" + clusterContent);
                    } else {
                        name.setLastName(clusterContent);
                    }
                    name.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.NAMES_SUFFIX)) {
                    if (name.getSuffix() != null) {
                        name.setSuffix(name.getSuffix() + "\t" + clusterContent);
                    } else {
                        name.setSuffix(clusterContent);
                    }
                    name.addLayoutTokens(cluster.concatTokens());
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return name;
    }

    /**
     * Extract results from a labeled sequence.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return list of patients
     */
    public List<PersonName> resultExtractionLayoutTokens(String result,
                                                      List<LayoutToken> tokenizations) {
        List<PersonName> listNames = new ArrayList<>();
        PersonName name = null;
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.NAMES_PERSON_MEDICAL, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.NAMES_TITLE)) {
                if (name.getTitle() != null) {
                    name.setTitle(name.getTitle() + "\t" + clusterContent);
                } else {
                    name.setTitle(clusterContent);
                }
                name.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.NAMES_FORENAME)) {
                if (name.getFirstName() != null) {
                    name.setFirstName(name.getFirstName() + "\t" + clusterContent);
                } else {
                    name.setFirstName(clusterContent);
                }
                name.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.NAMES_MIDDLENAME)) {
                if (name.getMiddleName() != null) {
                    name.setMiddleName(name.getMiddleName() + "\t" + clusterContent);
                } else {
                    name.setMiddleName(clusterContent);
                }
                name.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.NAMES_SURNAME)) {
                if (name.getLastName() != null) {
                    name.setLastName(name.getLastName() + "\t" + clusterContent);
                } else {
                    name.setLastName(clusterContent);
                }
                name.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.NAMES_SUFFIX)) {
                if (name.getSuffix() != null) {
                    name.setSuffix(name.getSuffix() + "\t" + clusterContent);
                } else {
                    name.setSuffix(clusterContent);
                }
                name.addLayoutTokens(cluster.concatTokens());
            }
            if (name != null) {
                listNames.add(name);
            }
        }
        return listNames;
    }

    /**
     * Extract results from a dateline string in the training format without any string modification.
     */
    public StringBuilder trainingExtraction(List<String> inputs) {
        StringBuilder buffer = new StringBuilder();
        try {
            if (inputs == null)
                return null;

            if (inputs.size() == 0)
                return null;

            List<OffsetPosition> titlePositions = null;
            List<OffsetPosition> suffixPositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;
                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                titlePositions = lexicon.tokenPositionsPersonTitle(tokenizations);
                suffixPositions = lexicon.tokenPositionsPersonSuffix(tokenizations);

                String ress = FeaturesVectorPersonName.addFeaturesName(tokenizations,
                    null, titlePositions, suffixPositions);
                String res = label(ress);

                String lastTag = null;
                String lastTag0 = null;
                String currentTag0 = null;
                boolean start = true;
                String s1 = null;
                String s2 = null;
                int p = 0;
                boolean addSpace;
                // extract results from the processed file
                StringTokenizer st = new StringTokenizer(res, "\n");
                while (st.hasMoreTokens()) {
                    addSpace = false;
                    String tok = st.nextToken().trim();
                    if (tok.length() == 0) {
                        // new dateline
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
                            s2 = TextUtilities.HTMLEncode(s); // the string

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
                            s1 = s; // the label
                        }
                        i++;
                    }
                    if (start && (s1 != null)) {
                        buffer.append("<name>");
                        start = false;
                    }
                    // lastTag, lastTag0 (without I-)
                    if (lastTag != null) {
                        if (lastTag.startsWith("I-")) {
                            lastTag0 = lastTag.substring(2, lastTag.length());
                        } else {
                            lastTag0 = lastTag;
                        }
                    }
                    // currentTag, currentTag (without I-)
                    if (s1 != null) {
                        if (s1.startsWith("I-")) {
                            currentTag0 = s1.substring(2, s1.length());
                        } else {
                            currentTag0 = s1;
                        }
                    }
                    // close tag
                    if ((lastTag0 != null) && (currentTag0 != null))
                        testClosingTag(buffer, currentTag0, lastTag0);

                    String output = writeField(s1, lastTag0, s2, "<forename>", "<forename>", addSpace, 0);

                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<middlename>", "<middlename>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<surname>", "<surname>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<title>", "<title>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<suffix>", "<suffix>", addSpace, 0);
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
                    buffer.append("</name>\n");
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return buffer;
    }

    /**
     * Extract results from a dateline string in the training format without any string modification.
     */
    public StringBuilder trainingExtractionAnonym(List<String> inputs, List<String>  dataOriginal, List<String> dataAnonymized) {
        StringBuilder buffer = new StringBuilder();
        try {
            if (inputs == null)
                return null;

            if (inputs.size() == 0)
                return null;

            List<OffsetPosition> titlePositions = null;
            List<OffsetPosition> suffixPositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;
                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                titlePositions = lexicon.tokenPositionsPersonTitle(tokenizations);
                suffixPositions = lexicon.tokenPositionsPersonSuffix(tokenizations);

                String ress = FeaturesVectorPersonName.addFeaturesName(tokenizations,
                    null, titlePositions, suffixPositions);
                String res = label(ress);

                String lastTag = null;
                String lastTag0 = null;
                String currentTag0 = null;
                boolean start = true;
                String s1 = null;
                String s2 = null;
                int p = 0;
                boolean addSpace;
                // extract results from the processed file
                StringTokenizer st = new StringTokenizer(res, "\n");
                while (st.hasMoreTokens()) {
                    addSpace = false;
                    String tok = st.nextToken().trim();
                    if (tok.length() == 0) {
                        // new dateline
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
                            // anonymize the data
                            String newText = s;
                            int idxFound =  dataOriginal.indexOf(s.trim());
                            if (idxFound >=0) {
                                newText = dataAnonymized.get(idxFound);
                            }

                            s2 = TextUtilities.HTMLEncode(newText); // the token

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).t();
                                // anonymize the data
                                idxFound =  dataOriginal.indexOf(tokOriginal.trim());
                                if (idxFound >=0) {
                                    tokOriginal = dataAnonymized.get(idxFound);
                                }

                                if (tokOriginal.equals(" ")
                                    || tokOriginal.equals("\u00A0")) {
                                    addSpace = true;
                                } else if (tokOriginal.equals(s)) {
                                    strop = true;
                                }
                                p++;
                            }
                        } else if (i == ll - 1) {
                            s1 = s; // the label
                        }
                        i++;
                    }
                    if (start && (s1 != null)) {
                        buffer.append("\t<name>");
                        start = false;
                    }
                    // lastTag, lastTag0 (without I-)
                    if (lastTag != null) {
                        if (lastTag.startsWith("I-")) {
                            lastTag0 = lastTag.substring(2, lastTag.length());
                        } else {
                            lastTag0 = lastTag;
                        }
                    }
                    // currentTag, currentTag (without I-)
                    if (s1 != null) {
                        if (s1.startsWith("I-")) {
                            currentTag0 = s1.substring(2, s1.length());
                        } else {
                            currentTag0 = s1;
                        }
                    }
                    // close tag
                    if ((lastTag0 != null) && (currentTag0 != null))
                        testClosingTag(buffer, currentTag0, lastTag0);

                    String output = writeField(s1, lastTag0, s2, "<forename>", "<forename>", addSpace, 0);

                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<middlename>", "<middlename>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<surname>", "<surname>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<title>", "<title>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<suffix>", "<suffix>", addSpace, 0);
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
                    buffer.append("</name>\n");
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(String s1,
                               String lastTag0,
                               String s2,
                               String field,
                               String outField,
                               boolean addSpace,
                               int nbIndent) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if ((s1.equals("<other>") || s1.equals("I-<other>"))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                for (int i = 0; i < nbIndent; i++) {
                    result += "\t";
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
                                   String lastTag0) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("");
            } else if (lastTag0.equals("<forename>")) {
                buffer.append("</forename>");
            } else if (lastTag0.equals("<middlename>")) {
                buffer.append("</middlename>");
            } else if (lastTag0.equals("<surname>")) {
                buffer.append("</surname>");
            } else if (lastTag0.equals("<title>")) {
                buffer.append("</title>");
            } else if (lastTag0.equals("<suffix>")) {
                buffer.append("</suffix>");
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