package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Dateline;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorDateline;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.utilities.counters.CntManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DatelineParser extends AbstractParser {
    private static Logger LOGGER = LoggerFactory.getLogger(DatelineParser.class);
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();

    public DatelineParser() {
        super(GrobidModels.DATELINE);
    }

    public DatelineParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.DATELINE, cntManager);
        this.parsers = parsers;
    }

    public DatelineParser(EngineMedicalParsers parsers) {
        super(GrobidModels.CITATION);
        this.parsers = parsers;
    }

    public List<Dateline> process(String input) {
        List<String> datelineBlocks = new ArrayList<>();
        // force English language for the tokenization only
        List<LayoutToken> tokens = analyzer.tokenizeWithLayoutToken(input);
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }

        return processCommon(tokens);
    }

    protected List<Dateline> processCommon(List<LayoutToken> input) {
        if (CollectionUtils.isEmpty(input))
            return null;
        List<OffsetPosition> placeNamePositions = lexicon.tokenPositionsLocationNames(input);
        try {
            String features = FeaturesVectorDateline.addFeaturesDateline(input, null, placeNamePositions);
            String res = label(features);

            // extract results from the processed file
            return resultExtraction(res, input);
        } catch (Exception e) {
            throw new GrobidException("An exception on " + this.getClass().getName() + " occured while running Grobid.", e);
        }
    }

    public List<Dateline> resultExtraction(String result, List<LayoutToken> tokenizations) {
        List<Dateline> datelines = new ArrayList<>();
        Dateline dateline = new Dateline();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.DATELINE, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }
            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterText = LayoutTokensUtil.toText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.DATELINE_PLACE_NAME)) {
                if (isNotBlank(dateline.getPlaceName())) {
                    if (dateline.isNotNull()) {
                        datelines.add(dateline);
                        dateline = new Dateline();
                    }
                    dateline.setPlaceName(clusterText);
                }
            } else if (clusterLabel.equals(MedicalLabels.DATELINE_DATE)) {
                if (isNotBlank(dateline.getDate())) {
                    if (dateline.isNotNull()) {
                        datelines.add(dateline);
                        dateline = new Dateline();
                    }
                    dateline.setDate(clusterText);
                }

            } else if (clusterLabel.equals(MedicalLabels.DATELINE_TIME)) {
                if (isNotBlank(dateline.getTimeString())) {
                    if (dateline.isNotNull()) {
                        datelines.add(dateline);
                        dateline = new Dateline();
                    }
                    dateline.setTimeString(clusterText);
                }
            } else if (clusterLabel.equals(MedicalLabels.DATELINE_NOTE)) {
            if (isNotBlank(dateline.getNote())) {
                if (dateline.isNotNull()) {
                    datelines.add(dateline);
                    dateline = new Dateline();
                }
                dateline.setNote(clusterText);
            }
        }
        }

        if (dateline.isNotNull()) {
            datelines.add(dateline);
        }
        return datelines;
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

            List<OffsetPosition> placeNamePositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;
                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                placeNamePositions = lexicon.tokenPositionsLocationNames(tokenizations);

                String ress = FeaturesVectorDateline.addFeaturesDateline(tokenizations,
                    null, placeNamePositions);
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
                        buffer.append("\t<dateline>");
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

                    String output = writeField(s1, lastTag0, s2, "<place>", "<placeName>", addSpace, 0);

                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<date>", "<date>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<time>", "<time>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note>", addSpace, 0);
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
                    buffer.append("</dateline>\n");
                }
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
            } else if (lastTag0.equals("<place>")) {
                buffer.append("</placeName>");
            } else if (lastTag0.equals("<date>")) {
                buffer.append("</date>");
            } else if (lastTag0.equals("<time>")) {
                buffer.append("</time>");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>");
            } else {
                res = false;
            }

        }
        return res;
    }

}