package org.grobid.core.features;

import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Class for features used for parsing sequence of address.
 * <p>
 * Tanti, 2022
 */
public class FeaturesVectorAddress {
    public String string = null; // lexical feature
    public String label = null; // label if known
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean lastName = false;
    public boolean locationName = false;
    public boolean countryName = false;
    public String punctType = null;
    public String wordShape = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)

    // true if the token is part of a predefinied name (single or multi-token)
    public boolean isLocationToken = false;
    public boolean isCityNameToken = false;

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
        StringBuffer res = new StringBuffer();

        // token string (1)
        res.append(string);

        // lowercase string
        res.append(" " + string.toLowerCase());

        // prefix (4)
        res.append(" " + TextUtilities.prefix(string, 1));
        res.append(" " + TextUtilities.prefix(string, 2));
        res.append(" " + TextUtilities.prefix(string, 3));
        res.append(" " + TextUtilities.prefix(string, 4));

        // suffix (4)
        res.append(" " + TextUtilities.suffix(string, 1));
        res.append(" " + TextUtilities.suffix(string, 2));
        res.append(" " + TextUtilities.suffix(string, 3));
        res.append(" " + TextUtilities.suffix(string, 4));

        // line information (1)
        res.append(" " + lineStatus);

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" " + capitalisation);

        // digit information (1)
        res.append(" " + digit);

        // character information (1)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information (3)
        if (properName)
            res.append(" 1");
        else
            res.append(" 0");

        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        if (firstName)
            res.append(" 1");
        else
            res.append(" 0");

        if (lastName)
            res.append(" 1");
        else
            res.append(" 0");

        if (locationName)
            res.append(" 1");
        else
            res.append(" 0");

        if (countryName)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        res.append(" ").append(wordShape);

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");
        else
            res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add features for the address model.
     */
    static public String addFeaturesAddress(List<LayoutToken> tokens, List<String> labels,
                                            List<OffsetPosition> locationPositions, List<OffsetPosition> cityNamePositions) throws Exception {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        StringBuffer name = new StringBuffer();
        boolean newline = true;
        String previousTag = null;
        String previousText = null;
        FeaturesVectorAddress features = null;
        LayoutToken token = null;

        int currentLocationPosition = 0;
        int currentCityNamePosition = 0;

        boolean isLocationToken;
        boolean isCityNameToken;
        boolean skipTest;

        for (int n = 0; n < tokens.size(); n++) {
            boolean outputLineStatus = false;
            isLocationToken = false;
            isCityNameToken = false;
            skipTest = false;

            token = tokens.get(n);

            String text = token.getText();
            if (text.equals(" ")) {
                continue;
            }

            newline = false;
            if (text.equals("\n")) {
                newline = true;
                continue;
            }

            // remove spaces
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0) {
                continue;
            }

            // check the position of matched title
            if ((locationPositions != null) && (locationPositions.size() > 0)) {
                if (currentLocationPosition == locationPositions.size() - 1) {
                    if (locationPositions.get(currentLocationPosition).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentLocationPosition; i < locationPositions.size(); i++) {
                        if ((locationPositions.get(i).start <= n) &&
                            (locationPositions.get(i).end >= n)) {
                            isLocationToken = true;
                            currentLocationPosition = i;
                            break;
                        } else if (locationPositions.get(i).start > n) {
                            isLocationToken = false;
                            currentLocationPosition = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched suffix
            skipTest = false;
            if (cityNamePositions != null) {
                if (currentCityNamePosition == cityNamePositions.size() - 1) {
                    if (cityNamePositions.get(currentCityNamePosition).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentCityNamePosition; i < cityNamePositions.size(); i++) {
                        if ((cityNamePositions.get(i).start <= n) &&
                            (cityNamePositions.get(i).end >= n)) {
                            isCityNameToken = true;
                            currentCityNamePosition = i;
                            break;
                        } else if (cityNamePositions.get(i).start > n) {
                            isCityNameToken = false;
                            currentCityNamePosition = i;
                            break;
                        }
                    }
                }
            }

            String tag = null;
            if (!CollectionUtils.isEmpty(labels) && (labels.size() > n)) {
                tag = labels.get(n);
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorAddress();
            features.string = text;

            if (newline) {
                features.lineStatus = "LINESTART";
                outputLineStatus = true;
            }

            Matcher m0 = featureFactory.isPunct.matcher(text);
            if (m0.find()) {
                features.punctType = "PUNCT";
            }

            if ((text.equals("(")) | (text.equals("["))) {
                features.punctType = "OPENBRACKET";
            } else if ((text.equals(")")) | (text.equals("]"))) {
                features.punctType = "ENDBRACKET";
            } else if (text.equals(".")) {
                features.punctType = "DOT";
            } else if (text.equals(",")) {
                features.punctType = "COMMA";
            } else if (text.equals("-")) {
                features.punctType = "HYPHEN";
            } else if (text.equals("\"") | text.equals("\'") | text.equals("`")) {
                features.punctType = "QUOTE";
            }

            if (n == 0) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINESTART";
                    outputLineStatus = true;
                }
            } else if (tokens.size() == n + 1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
                }
            } else {
                // look ahead...
                boolean endline = false;
                int i = 1;
                boolean endloop = false;
                while ((tokens.size() > n + i) & (!endloop)) {
                    String newLine = tokens.get(n + i).getText();
                    if (newLine != null) {
                        if (newLine.equals("\n")) {
                            endline = true;
                            if (!outputLineStatus) {
                                features.lineStatus = "LINEEND";
                                outputLineStatus = true;
                            }
                            endloop = true;
                        } else if (!newLine.equals(" ")) {
                            endloop = true;
                        }
                    }
                    i++;
                }
            }

            if (!outputLineStatus) {
                features.lineStatus = "LINEIN";
                outputLineStatus = true;
            }

            if (text.length() == 1) {
                features.singleChar = true;
            }

            if (Character.isUpperCase(text.charAt(0))) {
                features.capitalisation = "INITCAP";
            }

            if (featureFactory.test_all_capital(text)) {
                features.capitalisation = "ALLCAP";
            }

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            if (featureFactory.test_common(text)) {
                features.commonName = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isLocationToken) {
                features.isLocationToken = true;
            }

            if (isCityNameToken) {
                features.isCityNameToken = true;
            }

            features.label = tag;

            name.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }

        return name.toString();
    }

    /**
     * Add features for name parsing.
     */
    static public String addFeaturesAddressAnonym(List<LayoutToken> tokens, List<String> labels,
                                               List<OffsetPosition> locationPositions, List<OffsetPosition> cityNamePositions,
                                               List<String> dataOriginal, List<String> dataAnonymized) throws Exception {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        StringBuffer name = new StringBuffer();
        boolean newline = true;
        String previousTag = null;
        String previousText = null;
        FeaturesVectorAddress features = null;
        LayoutToken token = null;

        int currentLocationPosition = 0;
        int currentCityNamePosition = 0;

        boolean isLocationToken;
        boolean isCityNameToken;
        boolean skipTest;

        for (int n = 0; n < tokens.size(); n++) {
            boolean outputLineStatus = false;
            isLocationToken = false;
            isCityNameToken = false;
            skipTest = false;

            token = tokens.get(n);

            String text = token.getText();
            if (text.equals(" ")) {
                continue;
            }

            newline = false;
            if (text.equals("\n")) {
                newline = true;
                continue;
            }

            // anonymize the data
            int idxFound = dataOriginal.indexOf(text.trim());
            if (idxFound >= 0) {
                text = dataAnonymized.get(idxFound);
            }

            // remove blank spaces
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0) {
                continue;
            }

            // check the position of matched title
            if ((locationPositions != null) && (locationPositions.size() > 0)) {
                if (currentLocationPosition == locationPositions.size() - 1) {
                    if (locationPositions.get(currentLocationPosition).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentLocationPosition; i < locationPositions.size(); i++) {
                        if ((locationPositions.get(i).start <= n) &&
                            (locationPositions.get(i).end >= n)) {
                            isLocationToken = true;
                            currentLocationPosition = i;
                            break;
                        } else if (locationPositions.get(i).start > n) {
                            isLocationToken = false;
                            currentLocationPosition = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched suffix
            skipTest = false;
            if (cityNamePositions != null) {
                if (currentCityNamePosition == cityNamePositions.size() - 1) {
                    if (cityNamePositions.get(currentCityNamePosition).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentCityNamePosition; i < cityNamePositions.size(); i++) {
                        if ((cityNamePositions.get(i).start <= n) &&
                            (cityNamePositions.get(i).end >= n)) {
                            isCityNameToken = true;
                            currentCityNamePosition = i;
                            break;
                        } else if (cityNamePositions.get(i).start > n) {
                            isCityNameToken = false;
                            currentCityNamePosition = i;
                            break;
                        }
                    }
                }
            }
            String tag = null;
            if (!CollectionUtils.isEmpty(labels) && (labels.size() > n)) {
                tag = labels.get(n);
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorAddress();
            features.string = text;

            if (newline) {
                features.lineStatus = "LINESTART";
                outputLineStatus = true;
            }

            Matcher m0 = featureFactory.isPunct.matcher(text);
            if (m0.find()) {
                features.punctType = "PUNCT";
            }

            if ((text.equals("(")) | (text.equals("["))) {
                features.punctType = "OPENBRACKET";
            } else if ((text.equals(")")) | (text.equals("]"))) {
                features.punctType = "ENDBRACKET";
            } else if (text.equals(".")) {
                features.punctType = "DOT";
            } else if (text.equals(",")) {
                features.punctType = "COMMA";
            } else if (text.equals("-")) {
                features.punctType = "HYPHEN";
            } else if (text.equals("\"") | text.equals("\'") | text.equals("`")) {
                features.punctType = "QUOTE";
            }

            if (n == 0) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINESTART";
                    outputLineStatus = true;
                }
            } else if (tokens.size() == n + 1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
                }
            } else {
                // look ahead...
                boolean endline = false;
                int i = 1;
                boolean endloop = false;
                while ((tokens.size() > n + i) & (!endloop)) {
                    String newLine = tokens.get(n + i).getText();
                    if (newLine != null) {
                        if (newLine.equals("\n")) {
                            endline = true;
                            if (!outputLineStatus) {
                                features.lineStatus = "LINEEND";
                                outputLineStatus = true;
                            }
                            endloop = true;
                        } else if (!newLine.equals(" ")) {
                            endloop = true;
                        }
                    }
                    i++;
                }
            }

            if (!outputLineStatus) {
                features.lineStatus = "LINEIN";
                outputLineStatus = true;
            }

            if (text.length() == 1) {
                features.singleChar = true;
            }

            if (Character.isUpperCase(text.charAt(0))) {
                features.capitalisation = "INITCAP";
            }

            if (featureFactory.test_all_capital(text)) {
                features.capitalisation = "ALLCAP";
            }

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            if (featureFactory.test_common(text)) {
                features.commonName = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isLocationToken) {
                features.isLocationToken = true;
            }

            if (isCityNameToken) {
                features.isCityNameToken = true;
            }

            features.label = tag;

            name.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }

        return name.toString();
    }
}