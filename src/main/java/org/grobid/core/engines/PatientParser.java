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
    public Patient processing(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens);
    }


    public Patient processingWithLayoutTokens(List<LayoutToken> inputs) {
        return processing(inputs);
    }

    /**
     * Common processing of patients (mostly for the header part, but it can be used in other parts, such as for the body part)
     *
     * @param tokens list of LayoutToken object to process
     * @return List of identified Patient entities as POJO.
     */
    public Patient processing(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        List<Patient> fullPatients = new ArrayList<>();
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

                if (clusterLabel.equals(MedicalLabels.PATIENT_ID)) {
                    if (patient.getID() != null) {
                        patient.setID(patient.getID() + "\t" + clusterContent);
                    } else {
                        patient.setID(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_NAME)) {
                    if (patient.getPersName() != null) {
                        patient.setPersName(patient.getPersName() + "\t" + clusterContent);
                    } else {
                        patient.setPersName(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_SEX)) {
                    if (patient.getSex() != null) {
                        patient.setSex(patient.getSex() + "\t" + clusterContent);
                    } else {
                        patient.setSex(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_BIRTH)) {
                    if (patient.getDateBirth() != null) {
                        patient.setSex(patient.getDateBirth() + "\t" + clusterContent);
                    } else {
                        patient.setDateBirth(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_DEATH)) {
                    if (patient.getDateDeath() != null) {
                        patient.setDateDeath(patient.getDateDeath() + "\t" + clusterContent);
                    } else {
                        patient.setDateDeath(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_ADDRESS)) {
                    if (patient.getAddress() != null) {
                        patient.setAddress(patient.getAddress() + "\t" + clusterContent);
                    } else {
                        patient.setAddress(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_COUNTRY)) {
                    if (patient.getCountry() != null) {
                        patient.setCountry(patient.getCountry() + "\t" + clusterContent);
                    } else {
                        patient.setCountry(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_TOWN)) {
                    if (patient.getTown() != null) {
                        patient.setTown(patient.getTown() + "\t" + clusterContent);
                    } else {
                        patient.setTown(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_PHONE)) {
                    if (patient.getPhone() != null) {
                        patient.setPhone(patient.getPhone() + "\t" + clusterContent);
                    } else {
                        patient.setPhone(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.PATIENT_NOTE)) {
                    if (patient.getNote() != null) {
                        patient.setNote(patient.getNote() + "\t" + clusterContent);
                    } else {
                        patient.setNote(clusterContent);
                    }
                    patient.addLayoutTokens(cluster.concatTokens());
                }
                /*if (patient != null) {
                    fullPatients.add(patient);
                }*/
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return patient;
    }

    /**
     * Extract results from a labeled sequence.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return list of patients
     */
    public List<Patient> resultExtractionLayoutTokens(String result,
                                                      List<LayoutToken> tokenizations) {
        List<Patient> patients = new ArrayList<>();
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
            if (clusterLabel.equals(MedicalLabels.PATIENT_ID)) {
                if (patient.getID() != null) {
                    patient.setID(patient.getID() + "\t" + clusterContent);
                } else {
                    patient.setID(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_NAME)) {
                if (patient.getPersName() != null) {
                    patient.setPersName(patient.getPersName() + "\t" + clusterContent);
                } else {
                    patient.setPersName(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_SEX)) {
                if (patient.getSex() != null) {
                    patient.setSex(patient.getSex() + "\t" + clusterContent);
                } else {
                    patient.setSex(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_BIRTH)) {
                if (patient.getDateBirth() != null) {
                    patient.setSex(patient.getDateBirth() + "\t" + clusterContent);
                } else {
                    patient.setDateBirth(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_DATE_DEATH)) {
                if (patient.getDateDeath() != null) {
                    patient.setDateDeath(patient.getDateDeath() + "\t" + clusterContent);
                } else {
                    patient.setDateDeath(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_ADDRESS)) {
                if (patient.getAddress() != null) {
                    patient.setAddress(patient.getAddress() + "\t" + clusterContent);
                } else {
                    patient.setAddress(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_COUNTRY)) {
                if (patient.getCountry() != null) {
                    patient.setCountry(patient.getCountry() + "\t" + clusterContent);
                } else {
                    patient.setCountry(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_TOWN)) {
                if (patient.getTown() != null) {
                    patient.setTown(patient.getTown() + "\t" + clusterContent);
                } else {
                    patient.setTown(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_PHONE)) {
                if (patient.getPhone() != null) {
                    patient.setPhone(patient.getPhone() + "\t" + clusterContent);
                } else {
                    patient.setPhone(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.PATIENT_NOTE)) {
                if (patient.getNote() != null) {
                    patient.setNote(patient.getNote() + "\t" + clusterContent);
                } else {
                    patient.setNote(clusterContent);
                }
                patient.addLayoutTokens(cluster.concatTokens());
            }
            if (patient != null) {
                patients.add(patient);
            }
        }
        return patients;
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
                        output = writeField(s1, lastTag0, s2, "<sex>", "<sex>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<persname>", "<persName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<birth>", "<birth>", addSpace, 0);
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
            throw new GrobidException("An exception occurred while running Grobid.", e);
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
                        if (i == 0) {
                            for (int j=0; j<dataOriginal.size(); j++) {
                                s = s.replace(dataOriginal.get(j), dataAnonymized.get(j));
                            }
                            s2 = TextUtilities.HTMLEncode(s);
                            //s2 = s;

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).t();
                                for (int j=0; j<dataOriginal.size(); j++) {
                                    tokOriginal = tokOriginal.replace(dataOriginal.get(j), dataAnonymized.get(j));
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
                        output = writeField(s1, lastTag0, s2, "<sex>", "<sex>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<persname>", "<persName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<birth>", "<birth>", addSpace, 0);
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
            } else if (lastTag0.equals("<persname>")) {
                buffer.append("</persName>");
            } else if (lastTag0.equals("<sex>")) {
                buffer.append("</sex>");
            } else if (lastTag0.equals("<birth>")) {
                buffer.append("</birth>");
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
