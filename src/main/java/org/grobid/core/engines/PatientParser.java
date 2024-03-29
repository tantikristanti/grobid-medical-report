package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Patient;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorPatient;
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
 * A class for parsing patient information
 *
 * Tanti, 2022
 *
 * */

public class PatientParser extends AbstractParser {
    private static Logger LOGGER = LoggerFactory.getLogger(PatientParser.class);
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();

    public PatientParser() {
        super(GrobidModels.PATIENT);
    }

    public PatientParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.PATIENT, cntManager);
        this.parsers = parsers;
    }

    public PatientParser(EngineMedicalParsers parsers) {
        super(GrobidModels.PATIENT);
        this.parsers = parsers;
    }

    /**
     * Processing of patients in the header part
     */
    public Patient process(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return process(tokens);
    }


    public Patient processingWithLayoutTokens(List<LayoutToken> inputs) {
        return process(inputs);
    }

    /**
     * Common processing of patients (mostly for the header part, but it can be used in other parts, such as for the body part)
     *
     * @param tokens list of LayoutToken object to process
     * @return List of identified Patient entities as POJO.
     */
    public Patient process(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        Patient patient = null;
        try {
            List<OffsetPosition> locationsPositions = lexicon.tokenPositionsLocationNames(tokens);
            List<OffsetPosition> titlePositions = lexicon.tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = lexicon.tokenPositionsPersonSuffix(tokens);

            // get the features for the patient
            String sequence = FeaturesVectorPatient.addFeaturesPatient(tokens, null,
                locationsPositions, titlePositions, suffixPositions);

            if (StringUtils.isEmpty(sequence))
                return null;
            // labelling the featured data
            String res = label(sequence);
            //System.out.println(res);

            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.PATIENT, res, tokens);
            patient = new Patient();

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
                // we assume there is only one unique patient can be found in the header part of each document (but having different number ID)
                if (clusterLabel.equals(MedicalLabels.PATIENT_ID_TYPE)) {
                    if (patient.getIDType() != null) {
                        patient.setIDType(patient.getIDType() + "\t" + clusterContent);
                    } else {
                        patient.setIDType(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_ID)) {
                    if (patient.getID() != null) {
                        patient.setID(patient.getID() + "; " + clusterContent);
                    } else {
                        patient.setID(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_NAME)) {
                    if (patient.getPersName() == null) {
                        patient.setPersName(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_SEX)) {
                    if (patient.getSex() == null) {
                        patient.setSex(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_BIRTH)) {
                    if (patient.getDateBirth() == null) {
                        patient.setDateBirth(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_PLACE_BIRTH)) {
                    if (patient.getPlaceBirth() == null) {
                        patient.setPlaceBirth(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_AGE)) {
                    if (patient.getAge() == null) {
                        patient.setAge(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_DEATH)) {
                    if (patient.getDateDeath() == null) {
                        patient.setDateDeath(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_ADDRESS)) {
                    if (patient.getAddress() == null) {
                        patient.setAddress(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_COUNTRY)) {
                    if (patient.getCountry() == null) {
                        patient.setCountry(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_TOWN)) {
                    if (patient.getTown() != null) {
                        patient.setTown(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_PHONE)) {
                    if (patient.getPhone() != null &&
                        isDifferentContent(patient.getPhone(), clusterContent) &&
                        patient.getPhone().length() < clusterContent.length()) {
                        patient.setPhone(patient.getPhone() + "; " + clusterContent);
                    } else {
                        patient.setPhone(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_EMAIL)) {
                    if (patient.getEmail() != null &&
                        isDifferentContent(patient.getEmail(), clusterContent) &&
                        patient.getEmail().length() < clusterContent.length()) {
                        patient.setEmail(patient.getEmail() + "; " + clusterContent);
                    } else {
                        patient.setEmail(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_NOTE)) {
                    if (patient.getNote() != null) {
                        patient.setNote(patient.getNote() + " " + clusterContent);
                    } else {
                        patient.setNote(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
        return patient;
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
     * Extract results from a labeled sequence.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return list of patients
     */
    public Patient resultExtractionLayoutTokens(String result, List<LayoutToken> tokenizations) {
        Patient patient = new Patient();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.PATIENT, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.PATIENT_ID_TYPE)) {
                if (patient.getIDType() != null) {
                    patient.setIDType(patient.getIDType() + "\t" + clusterContent);
                } else {
                    patient.setIDType(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_ID)) {
                if (patient.getID() != null) {
                    patient.setID(patient.getID() + "; " + clusterContent);
                } else {
                    patient.setID(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_NAME)) {
                if (patient.getPersName() == null) {
                    patient.setPersName(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_SEX)) {
                if (patient.getSex() == null) {
                    patient.setSex(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_BIRTH)) {
                if (patient.getDateBirth() == null) {
                    patient.setDateBirth(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_PLACE_BIRTH)) {
                if (patient.getPlaceBirth() == null) {
                    patient.setPlaceBirth(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_AGE)) {
                if (patient.getAge() == null) {
                    patient.setAge(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_DEATH)) {
                if (patient.getDateDeath() == null) {
                    patient.setDateDeath(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_ADDRESS)) {
                if (patient.getAddress() == null) {
                    patient.setAddress(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_COUNTRY)) {
                if (patient.getCountry() == null) {
                    patient.setCountry(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_TOWN)) {
                if (patient.getTown() != null) {
                    patient.setTown(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_PHONE)) {
                if (patient.getPhone() != null &&
                    isDifferentContent(patient.getPhone(), clusterContent) &&
                    patient.getPhone().length() < clusterContent.length()) {
                    patient.setPhone(patient.getPhone() + "; " + clusterContent);
                } else {
                    patient.setPhone(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_EMAIL)) {
                if (patient.getEmail() != null &&
                    isDifferentContent(patient.getEmail(), clusterContent) &&
                    patient.getEmail().length() < clusterContent.length()) {
                    patient.setEmail(patient.getEmail() + "; " + clusterContent);
                } else {
                    patient.setEmail(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_NOTE)) {
                if (patient.getNote() != null) {
                    patient.setNote(patient.getNote() + " " + clusterContent);
                } else {
                    patient.setNote(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
        }
        return patient;
    }

    /**
     * Extract results from a list of patient strings in the training format
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

            for (String input : inputs) {
                if (input == null)
                    continue;

                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                locationsPositions = lexicon.tokenPositionsLocationNames(tokenizations);
                titlePositions = lexicon.tokenPositionsPersonTitle(tokenizations);
                suffixPositions = lexicon.tokenPositionsPersonSuffix(tokenizations);

                String ress = FeaturesVectorPatient.addFeaturesPatient(tokenizations, null,
                    locationsPositions, titlePositions, suffixPositions);
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
                        // new patient
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
                        //buffer.append("\t<patient>");
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
                        output = writeField(s1, lastTag0, s2, "<idtype>", "<idType>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<sex>", "<sex>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<persname>", "<persName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<birthdate>", "<birthDate>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<birthplace>", "<birthPlace>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<age>", "<age>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<death>", "<death>", addSpace, 0);
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
                        output = writeField(s1, lastTag0, s2, "<phone>", "<phone>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<email>", "<email>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note type=\"patient\">", addSpace, 0);
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
                    //buffer.append("</patient>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
        return buffer;
    }

    /**
     * Extract results from a list of patient strings in the training format
     * without any string modification.
     *
     * @param inputs list of input data
     * @return result
     */
    public StringBuilder trainingExtractionAnonym(List<String> inputs, List<String> dataOriginal, List<String> dataAnonymized) {
        StringBuilder buffer = new StringBuilder();
        try {
            if (inputs == null)
                return null;

            if (inputs.size() == 0)
                return null;

            List<OffsetPosition> locationsPositions = null;
            List<OffsetPosition> titlePositions = null;
            List<OffsetPosition> suffixPositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;

                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                locationsPositions = lexicon.tokenPositionsLocationNames(tokenizations);
                titlePositions = lexicon.tokenPositionsPersonTitle(tokenizations);
                suffixPositions = lexicon.tokenPositionsPersonSuffix(tokenizations);

                String ress = FeaturesVectorPatient.addFeaturesPatient(tokenizations, null,
                    locationsPositions, titlePositions, suffixPositions);
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
                        // new patient
                        start = true;
                        continue;
                    }
                    StringTokenizer stt = new StringTokenizer(tok, "\t");
                    int i = 0;

                    boolean newLine = false;
                    int ll = stt.countTokens();
                    while (stt.hasMoreTokens()) {
                        String s = stt.nextToken().trim();

                        // anonymize the token
                        int idx = dataOriginal.indexOf(s);
                        if (idx >= 0) {
                            s = dataAnonymized.get(idx);
                        }

                        if (i == 0) {
                            s2 = TextUtilities.HTMLEncode(s);
                            //s2 = s;

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).t();
                                if (dataOriginal.indexOf(tokOriginal) >= 0) {
                                    tokOriginal = dataAnonymized.get(idx);
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
                            s1 = s;
                        }
                        i++;
                    }

                    if (start && (s1 != null)) {
                        buffer.append("\t<patient>");
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
                        output = writeField(s1, lastTag0, s2, "<idtype>", "<idType>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<sex>", "<sex>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<persname>", "<persName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<birthdate>", "<birthDate>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<birthplace>", "<birthPlace>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<age>", "<age>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<death>", "<death>", addSpace, 0);
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
                        output = writeField(s1, lastTag0, s2, "<phone>", "<phone>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<email>", "<email>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note type=\"patient\">", addSpace, 0);
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
                    buffer.append("</patient>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
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
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
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
            } else if (lastTag0.equals("<idtype>")) {
                buffer.append("</idType>");
            } else if (lastTag0.equals("<persname>")) {
                buffer.append("</persName>");
            } else if (lastTag0.equals("<sex>")) {
                buffer.append("</sex>");
            } else if (lastTag0.equals("<birthdate>")) {
                buffer.append("</birthDate>");
            } else if (lastTag0.equals("<birthplace>")) {
                buffer.append("</birthPlace>");
            } else if (lastTag0.equals("<age>")) {
                buffer.append("</age>");
            } else if (lastTag0.equals("<death>")) {
                buffer.append("</death>");
            } else if (lastTag0.equals("<address>")) {
                buffer.append("</address>");
            } else if (lastTag0.equals("<country>")) {
                buffer.append("</country>");
            } else if (lastTag0.equals("<settlement>")) {
                buffer.append("</settlement>");
            } else if (lastTag0.equals("<phone>")) {
                buffer.append("</phone>");
            }  else if (lastTag0.equals("<email>")) {
                buffer.append("</email>");
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
