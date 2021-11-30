package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
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
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DatelineParser extends AbstractParser {
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();

    public DatelineParser() {
        super(GrobidModels.DATELINE);
    }

    DatelineParser(GrobidModel model) {
        super(model);
    }
    
    public List<Dateline> process(String input) {
        List<String> datelineBlocks = new ArrayList<>();
        // force English language for the tokenization only
        List<String> tokenizations = analyzer.tokenize(input, new Language("fr", 1.0));
        if (CollectionUtils.isEmpty(tokenizations)) {
            return null;
        }

        for(String tok : tokenizations) {
            if (!" ".equals(tok) && !"\n".equals(tok)) {
                tok = tok.replaceAll("[ \n]", "");
                datelineBlocks.add(tok + " <dateline>");
            }
        }
        
        return processCommon(datelineBlocks);
    }

    public List<Dateline> process(List<LayoutToken> input) {
        List<String> datelineBlocks = new ArrayList<>();
        for(LayoutToken tok : input) {
            if (!" ".equals(tok.getText()) && !"\n".equals(tok.getText())) {
                String normalizedText = tok.getText().replaceAll("[ \n]", "");
                datelineBlocks.add(normalizedText + " <dateline>");
            } 
        }

        return processCommon(datelineBlocks);
    }
    
    protected List<Dateline> processCommon(List<String> input) {
        if (CollectionUtils.isEmpty(input))
            return null;
        
        try {
            StringBuilder features = FeaturesVectorDateline.addFeaturesDateline(input);
            String res = label(features.toString());

            List<LayoutToken> tokenization = input.stream()
                .map(token -> new LayoutToken(token.split(" ")[0]))
                .collect(Collectors.toList());
            
            // extract results from the processed file
            return resultExtraction(res, tokenization);
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

                } else {
                    dateline.setPlaceName(clusterText);
                }
            } else if (clusterLabel.equals(MedicalLabels.DATELINE_DATE)) {
                if (isNotBlank(dateline.getDate())) {
                        if (dateline.isNotNull()) {
                            datelines.add(dateline);
                            dateline = new Dateline();
                        }
                        dateline.setDate(clusterText);
                } else {
                    dateline.setDate(clusterText);
                }
               
            } else if (clusterLabel.equals(MedicalLabels.DATELINE_TIME)) {
                if (isNotBlank(dateline.getTimeString())) {
                        if (dateline.isNotNull()) {
                            datelines.add(dateline);
                            dateline = new Dateline();
                        }
                        dateline.setTimeString(clusterText);
                } else {
                    dateline.setTimeString(clusterText);
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

            List<LayoutToken> tokenizations = null;
            List<String> datelineBlocks = new ArrayList<String>();
            for (String input : inputs) {
                if (input == null)
                    continue;

				tokenizations = analyzer.tokenizeWithLayoutToken(input, new Language("fr"));
				
				if (tokenizations.size() == 0)
                    return null;

				for(LayoutToken tok : tokenizations) {
                    if (tok.getText().equals("\n")) {
                        datelineBlocks.add("@newline");
                    } else if (!tok.getText().equals(" ")) {
                        datelineBlocks.add(tok.getText() + " <dateline>");
                    }
                }
                datelineBlocks.add("\n");
            }

            StringBuilder headerDateline = FeaturesVectorDateline.addFeaturesDateline(datelineBlocks);
            String res = label(headerDateline.toString());

            //System.out.print(res.toString());
            StringTokenizer st2 = new StringTokenizer(res, "\n");
            String lastTag = null;
            boolean tagClosed = false;
            int q = 0;
            boolean addSpace;
            boolean hasPlaceName = false;
            boolean hasDate = false;
            boolean hasTime = false;
            String lastTag0;
            String currentTag0;
            boolean start = true;
            while (st2.hasMoreTokens()) {
                String line = st2.nextToken();
                addSpace = false;
                if ((line.trim().length() == 0)) {
                    // new dateline
                    buffer.append("</dateline>\n");
                    hasPlaceName = false;
                    hasDate = false;
                    hasTime = false;
                    buffer.append("\t<dateline>");
                    continue;
                } else {
                    String theTok = tokenizations.get(q).getText();
                    while (theTok.equals(" ")) {
                        addSpace = true;
                        q++;
                        theTok = tokenizations.get(q).getText();
                    }
                    q++;
                }

                StringTokenizer st3 = new StringTokenizer(line, "\t");
                int ll = st3.countTokens();
                int i = 0;
                String s1 = null;
                String s2 = null;
                while (st3.hasMoreTokens()) {
                    String s = st3.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // string
                    }
					else if (i == ll - 1) {
                        s1 = s; // label
                    }
                    i++;
                }

                if (start && (s1 != null)) {
                    buffer.append("\t<dateline>");
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
                currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                tagClosed = lastTag0 != null && testClosingTag(buffer, currentTag0, lastTag0);

                String output = writeField(s1, lastTag0, s2, "<placeName>", "<placeName>", addSpace, 0);
                if (output != null) {
                    if (lastTag0 != null) {
                        if (hasPlaceName && !lastTag0.equals("<placeName>")) {
                            buffer.append("</dateline>\n");
                            hasDate = false;
                            hasTime = false;
                            buffer.append("\t<dateline>");
                        }
                    }
                    hasPlaceName = true;
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                } else {
                    output = writeField(s1, lastTag0, s2, "<date>", "<date>", false, 0);
                }

                if (output != null) {
                    if (lastTag0 != null) {
                        if (hasDate && !lastTag0.equals("<date>")) {
                            buffer.append("</dateline>\n");
                            hasPlaceName = false;
                            hasTime = false;
                            buffer.append("\t<dateline>");
                        }
                    }
                    buffer.append(output);
                    hasDate = true;
                    lastTag = s1;
                    continue;
                } else {
                    output = writeField(s1, lastTag0, s2, "<time>", "<time>", addSpace, 0);
                }

                if (output != null) {
                    if (lastTag0 != null) {
                        if (hasTime && !lastTag0.equals("<time>")) {
                            buffer.append("</dateline>\n");
                            hasPlaceName = false;
                            hasDate = false;
                            buffer.append("\t<dateline>");
                        }
                    }
                    buffer.append(output);
                    hasTime = true;
                    lastTag = s1;
                    continue;
                } else {
                    output = writeField(s1, lastTag0, s2, "<other>", "<other>", addSpace, 0);
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
        } catch (Exception e) {
//			e.printStackTrace();
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
            } else if (lastTag0.equals("<placeName>")) {
                buffer.append("</placeName>");
            } else if (lastTag0.equals("<date>")) {
                buffer.append("</date>");
            } else if (lastTag0.equals("<time>")) {
                buffer.append("</time>");
            } else {
                res = false;
            }

        }
        return res;
    }

}