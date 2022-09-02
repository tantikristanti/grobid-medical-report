package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Address;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorAddress;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/*A class for parsing address
 *
 * Tanti, 2022
 * */

public class AddressParser extends AbstractParser {
    private static Logger LOGGER = LoggerFactory.getLogger(AddressParser.class);
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();

    public AddressParser() {
        super(GrobidModels.ADDRESS);
    }

    public AddressParser(EngineMedicalParsers parsers) {
        super(GrobidModels.ADDRESS);
        this.parsers = parsers;
    }

    public AddressParser(EngineMedicalParsers parsers, CntManager cntManager) {
        super(GrobidModels.ADDRESS, cntManager);
        this.parsers = parsers;
    }

    /**
     * Processing of address
     */
    public Address processing(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        input = UnicodeUtil.normaliseText(input);
        input = input.trim();

        input = TextUtilities.dehyphenize(input);

        // for language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens);
    }


    public Address processingWithLayoutTokens(List<LayoutToken> inputs) {
        return processing(inputs);
    }

    /**
     * Common processing of address
     *
     * @param tokens list of LayoutToken object to process
     * @return List of identified Address entities as POJO.
     */
    public Address processing(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        List<Address> fullAddress = new ArrayList<>();
        Address address = null;
        try {
            List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
            List<OffsetPosition> cityNamePositions = lexicon.tokenPositionsCityNames(tokens);

            // get the features for the address
            String sequence = FeaturesVectorAddress.addFeaturesAddress(tokens, null, locationPositions, cityNamePositions);

            if (StringUtils.isEmpty(sequence))
                return null;
            // labelling the featured data
            String res = label(sequence);
            //System.out.println(res);

            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.ADDRESS, res, tokens);
            address = new Address();

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

                if (clusterLabel.equals(MedicalLabels.ADDRESS_STREET_NUMBER)) {
                    if (address.getStreetNumber() != null) {
                        address.setStreetNumber(address.getStreetNumber() + "\t" + clusterContent);
                    } else {
                        address.setStreetNumber(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_STREET_NAME)) {
                    if (address.getStreetName() != null) {
                        address.setStreetName(address.getStreetName() + "\t" + clusterContent);
                    } else {
                        address.setStreetName(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_BUILDING_NUMBER)) {
                    if (address.getBuildingNumber() != null) {
                        address.setBuildingNumber(address.getBuildingNumber() + "\t" + clusterContent);
                    } else {
                        address.setBuildingNumber(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_BUILDING_NAME)) {
                    if (address.getBuildingName() != null) {
                        address.setBuildingName(address.getBuildingName() + "\t" + clusterContent);
                    } else {
                        address.setBuildingName(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_CITY)) {
                    if (address.getCity() != null) {
                        address.setCity(address.getCity() + "\t" + clusterContent);
                    } else {
                        address.setCity(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_POST_CODE)) {
                    if (address.getPostCode() != null) {
                        address.setPostCode(address.getPostCode() + "\t" + clusterContent);
                    } else {
                        address.setPostCode(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_PO_BOX)) {
                    if (address.getPoBox() != null) {
                        address.setPoBox(address.getPoBox() + "\t" + clusterContent);
                    } else {
                        address.setPoBox(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_COMMUNITY)) {
                    if (address.getCommunity() != null) {
                        address.setCommunity(address.getCommunity() + "\t" + clusterContent);
                    } else {
                        address.setCommunity(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_DISTRICT)) {
                    if (address.getDistrict() != null) {
                        address.setDistrict(address.getDistrict() + "\t" + clusterContent);
                    } else {
                        address.setDistrict(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_DEPARTMENT_NUMBER)) {
                    if (address.getDepartmentNumber() != null) {
                        address.setDepartmentNumber(address.getDepartmentNumber() + "\t" + clusterContent);
                    } else {
                        address.setDepartmentNumber(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_DEPARTMENT_NAME)) {
                    if (address.getDepartmentName() != null) {
                        address.setDepartmentName(address.getDepartmentName() + "\t" + clusterContent);
                    } else {
                        address.setDepartmentName(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_REGION)) {
                    if (address.getRegion() != null) {
                        address.setRegion(address.getRegion() + "\t" + clusterContent);
                    } else {
                        address.setRegion(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_COUNTRY)) {
                    if (address.getCountry() != null) {
                        address.setCountry(address.getCountry() + "\t" + clusterContent);
                    } else {
                        address.setCountry(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                if (clusterLabel.equals(MedicalLabels.ADDRESS_NOTE)) {
                    if (address.getNote() != null) {
                        address.setNote(address.getNote() + "\t" + clusterContent);
                    } else {
                        address.setNote(clusterContent);
                    }
                    address.addLayoutTokens(cluster.concatTokens());
                }
                /*if (address != null){
                    fullAddress.add(address);
                }*/
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
        return address;
    }

    /**
     * Extract results from a labeled sequence.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return list of address
     */
    public List<Address> resultExtractionLayoutTokens(String result,
                                                      List<LayoutToken> tokenizations) {
        List<Address> fullAddress = new ArrayList<>();
        Address address = new Address();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.ADDRESS, result, tokenizations);

        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            if (clusterLabel.equals(MedicalLabels.ADDRESS_STREET_NUMBER)) {
                if (address.getStreetNumber() != null) {
                    address.setStreetNumber(address.getStreetNumber() + "\t" + clusterContent);
                } else {
                    address.setStreetNumber(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_STREET_NAME)) {
                if (address.getStreetName() != null) {
                    address.setStreetName(address.getStreetName() + "\t" + clusterContent);
                } else {
                    address.setStreetName(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_BUILDING_NUMBER)) {
                if (address.getBuildingNumber() != null) {
                    address.setBuildingNumber(address.getBuildingNumber() + "\t" + clusterContent);
                } else {
                    address.setBuildingNumber(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_BUILDING_NAME)) {
                if (address.getBuildingName() != null) {
                    address.setBuildingName(address.getBuildingName() + "\t" + clusterContent);
                } else {
                    address.setBuildingName(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_CITY)) {
                if (address.getCity() != null) {
                    address.setCity(address.getCity() + "\t" + clusterContent);
                } else {
                    address.setCity(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_POST_CODE)) {
                if (address.getPostCode() != null) {
                    address.setPostCode(address.getPostCode() + "\t" + clusterContent);
                } else {
                    address.setPostCode(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_PO_BOX)) {
                if (address.getPoBox() != null) {
                    address.setPoBox(address.getPoBox() + "\t" + clusterContent);
                } else {
                    address.setPoBox(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_COMMUNITY)) {
                if (address.getCommunity() != null) {
                    address.setCommunity(address.getCommunity() + "\t" + clusterContent);
                } else {
                    address.setCommunity(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_DISTRICT)) {
                if (address.getDistrict() != null) {
                    address.setDistrict(address.getDistrict() + "\t" + clusterContent);
                } else {
                    address.setDistrict(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_DEPARTMENT_NUMBER)) {
                if (address.getDepartmentNumber() != null) {
                    address.setDepartmentNumber(address.getDepartmentNumber() + "\t" + clusterContent);
                } else {
                    address.setDepartmentNumber(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_DEPARTMENT_NAME)) {
                if (address.getDepartmentName() != null) {
                    address.setDepartmentName(address.getDepartmentName() + "\t" + clusterContent);
                } else {
                    address.setDepartmentName(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_REGION)) {
                if (address.getRegion() != null) {
                    address.setRegion(address.getRegion() + "\t" + clusterContent);
                } else {
                    address.setRegion(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_COUNTRY)) {
                if (address.getCountry() != null) {
                    address.setCountry(address.getCountry() + "\t" + clusterContent);
                } else {
                    address.setCountry(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (clusterLabel.equals(MedicalLabels.ADDRESS_NOTE)) {
                if (address.getNote() != null) {
                    address.setNote(address.getNote() + "\t" + clusterContent);
                } else {
                    address.setNote(clusterContent);
                }
                address.addLayoutTokens(cluster.concatTokens());
            }
            if (address != null) {
                fullAddress.add(address);
            }
        }
        return fullAddress;
    }

    /**
     * Extract results from a list of address strings in the training format without any string modification.
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

            List<OffsetPosition> locationPositions = null;
            List<OffsetPosition> cityNamePositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;

                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                locationPositions = lexicon.tokenPositionsLocationNames(tokenizations);
                cityNamePositions = lexicon.tokenPositionsCityNames(tokenizations);

                // get the features for the address
                String ress = FeaturesVectorAddress.addFeaturesAddress(tokenizations, null, locationPositions, cityNamePositions);
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
                        // new address
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
                        //buffer.append("\t<address>");
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


                    String output = writeField(s1, lastTag0, s2, "<streetnumber>", "<streetNumber>", addSpace, 0);
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<streetname>", "<streetName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<buildingnumber>", "<buildingNumber>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<buildingname>", "<buildingName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<city>", "<city>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<postcode>", "<postCode>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<pobox>", "<poBox>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<community>", "<community>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<district>", "<district>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<departmentnumber>", "<departmentNumber>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<departmentname>", "<departmentName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<region>", "<region>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<country>", "<country>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note type=\"address\">", addSpace, 0);
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
                    //buffer.append("</address>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
        }
        return buffer;
    }

    /**
     * Extract results from a list of address strings in the training format
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

            List<OffsetPosition> locationPositions = null;
            List<OffsetPosition> cityNamePositions = null;

            for (String input : inputs) {
                if (input == null)
                    continue;

                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                if (tokenizations.size() == 0)
                    return null;

                locationPositions = lexicon.tokenPositionsLocationNames(tokenizations);
                cityNamePositions = lexicon.tokenPositionsCityNames(tokenizations);

                // get the features for the address
                String ress = FeaturesVectorAddress.addFeaturesAddress(tokenizations, null, locationPositions, cityNamePositions);
                String res = label(ress);

                String lastTag = null;
                String lastTag0;
                String currentTag0 = null;
                boolean start = true;
                String s1 = null; // the label
                String s2 = null; // the token
                int p = 0;

                // extract results from the processed file
                StringTokenizer st = new StringTokenizer(res, "\n");
                while (st.hasMoreTokens()) {
                    boolean addSpace = false;
                    String tok = st.nextToken().trim();

                    if (tok.length() == 0) {
                        // new address
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
                            for (int j = 0; j < dataOriginal.size(); j++) {
                                s = s.replace(dataOriginal.get(j), dataAnonymized.get(j));
                            }
                            s2 = TextUtilities.HTMLEncode(s);
                            //s2 = s;

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).t();
                                for (int j = 0; j < dataOriginal.size(); j++) {
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
                        //buffer.append("\t<address>");
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


                    String output = writeField(s1, lastTag0, s2, "<streetnumber>", "<streetNumber>", addSpace, 0);
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<streetname>", "<streetName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<buildingnumber>", "<buildingNumber>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<buildingname>", "<buildingName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<city>", "<city>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<postcode>", "<postCode>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<pobox>", "<poBox>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<community>", "<community>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<district>", "<district>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<departmentnumber>", "<departmentNumber>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<departmentname>", "<departmentName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<region>", "<region>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<country>", "<country>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note type=\"address\">", addSpace, 0);
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
                    //buffer.append("</address>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running grobid-medical-report.", e);
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
            } else if (lastTag0.equals("<streetnumber>")) {
                buffer.append("</streetNumber>");
            } else if (lastTag0.equals("<streetname>")) {
                buffer.append("</streetName>");
            } else if (lastTag0.equals("<buildingnumber>")) {
                buffer.append("</buildingNumber>");
            } else if (lastTag0.equals("<buildingname>")) {
                buffer.append("</buildingName>");
            } else if (lastTag0.equals("<city>")) {
                buffer.append("</city>");
            } else if (lastTag0.equals("<postcode>")) {
                buffer.append("</postCode>");
            } else if (lastTag0.equals("<pobox>")) {
                buffer.append("</poBox>");
            } else if (lastTag0.equals("<community>")) {
                buffer.append("</community>");
            } else if (lastTag0.equals("<district>")) {
                buffer.append("</district>");
            } else if (lastTag0.equals("<departmentnumber>")) {
                buffer.append("</departmentNumber>");
            } else if (lastTag0.equals("<departmentname>")) {
                buffer.append("</departmentName>");
            } else if (lastTag0.equals("<region>")) {
                buffer.append("</region>");
            } else if (lastTag0.equals("<country>")) {
                buffer.append("</country>");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>");
            } else {
                res = false;
            }

        }
        return res;
    }

    /**
     * In the context of field extraction, check if a newly extracted content is not redundant with the already extracted content
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