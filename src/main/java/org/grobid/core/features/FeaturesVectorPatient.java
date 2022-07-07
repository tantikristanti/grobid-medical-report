package org.grobid.core.features;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class for features used for header parsing.
 *
 */
public class FeaturesVectorPatient {
    // default bins for relative position, set experimentally
    static private int nbBins = 12;

    public String string = null; // lexical feature
    public String label = null; // label if known
    public String blockStatus = null; // one of BLOCKSTART, BLOCKIN, BLOCKEND
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public boolean bold = false;
    public boolean italic = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean lastName = false;

    public boolean year = false;
    public boolean month = false;
    public boolean http = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public boolean containPunct = false;
    public int relativePosition = -1;

    // true if the token is part of a predefinied name (single or multi-token)
    public boolean isKnownTitle = false;
    public boolean isKnownSuffix = false;
    public boolean isKnownLocation = false;
    public boolean isKnownCountry = false;
    public boolean isKnownCity = false;

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
        StringBuilder res = new StringBuilder();

        // token string (1)
        res.append(string);

        // lowercase string (1)
        res.append(" ").append(string.toLowerCase());

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
        res.append(" ").append(lineStatus);

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" ").append(capitalisation);

        // digit information (1)
        res.append(" ").append(digit);

        // character information (1)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information (11)
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

        if (year)
            res.append(" 1");
        else
            res.append(" 0");

        if (month)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownTitle)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownSuffix)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownLocation)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownCountry)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownCity)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" ").append(punctType); // in case the token is a punctuation (NO otherwise)

        // relative position in the sequence (1)
        res.append(" ").append(relativePosition);

        // label - for training data (1)
        if (label != null)
            res.append(" ").append(label).append("\n");
        else
            res.append(" 0\n");

        return res.toString();
    }


    /**
     * Add features for patient parsing
     */
    static public String addFeaturesPatient(List<LayoutToken> tokens,
                                             List<String> labels,
                                             List<OffsetPosition> locationPositions,
                                             List<OffsetPosition> titlePositions,
                                             List<OffsetPosition> suffixPositions) throws Exception {
        if ((locationPositions == null) ||
            (titlePositions == null) ||
            (suffixPositions == null)) {
            throw new GrobidException("At least one list of gazetteer matches positions is null.");
        }

        FeatureFactory featureFactory = FeatureFactory.getInstance();

        StringBuilder patient = new StringBuilder();

        int currentLocationPositions = 0;
        int currentTitlePositions = 0;
        int currentSuffixPositions = 0;

        boolean isLocationToken;
        boolean isTitleToken;
        boolean isSuffixToken;
        boolean skipTest;

        String previousTag = null;
        String previousText = null;
        FeaturesVectorPatient features = null;
        int sentenceLenth = tokens.size(); // length of the current sentence
        for (int n=0; n < tokens.size(); n++) {
            LayoutToken token = tokens.get(n);
            String tag = null;
            if ( (labels != null) && (labels.size() > 0) && (n < labels.size()) )
                tag = labels.get(n);

            boolean outputLineStatus = false;
            isLocationToken = false;
            isTitleToken = false;
            isSuffixToken = false;
            skipTest = false;

            String text = token.getText();
            if (text.equals(" ")) {
                continue;
            }

            if (text.equals("\n")) {
                // should not be the case for citation model
                continue;
            }

            // remove blank spaces
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0 ) {
                continue;
            }

            // check the position of matched locations
            skipTest = false;
            if (locationPositions != null && (locationPositions.size() > 0)) {
                if (currentLocationPositions == locationPositions.size() - 1) {
                    if (locationPositions.get(currentLocationPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentLocationPositions; i < locationPositions.size(); i++) {
                        if ((locationPositions.get(i).start <= n) &&
                                (locationPositions.get(i).end >= n)) {
                            isLocationToken = true;
                            currentLocationPositions = i;
                            break;
                        } else if (locationPositions.get(i).start > n) {
                            isLocationToken = false;
                            currentLocationPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched titles
            skipTest = false;
            if ((titlePositions != null) && (titlePositions.size() > 0)) {
                if (currentTitlePositions == titlePositions.size() - 1) {
                    if (titlePositions.get(currentTitlePositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentTitlePositions; i < titlePositions.size(); i++) {
                        if ((titlePositions.get(i).start <= n) &&
                            (titlePositions.get(i).end >= n)) {
                            isTitleToken = true;
                            currentTitlePositions = i;
                            break;
                        } else if (titlePositions.get(i).start > n) {
                            isTitleToken = false;
                            currentTitlePositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched suffix
            skipTest = false;
            if (suffixPositions != null  && (suffixPositions.size() > 0)) {
                if (currentSuffixPositions == suffixPositions.size() - 1) {
                    if (suffixPositions.get(currentSuffixPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentSuffixPositions; i < suffixPositions.size(); i++) {
                        if ((suffixPositions.get(i).start <= n) &&
                                (suffixPositions.get(i).end >= n)) {
                            isSuffixToken = true;
                            currentSuffixPositions = i;
                            break;
                        } else if (suffixPositions.get(i).start > n) {
                            isSuffixToken = false;
                            currentSuffixPositions = i;
                            break;
                        }
                    }
                }
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorPatient();
            features.string = text;
            features.relativePosition = featureFactory.linearScaling(n, sentenceLenth, nbBins);

            if (n == 0) {
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
            } else if (tokens.size() == n+1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
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

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            if (featureFactory.test_common(text)) {
                features.commonName = true;
            }

            if (featureFactory.test_names(text)) {
                features.properName = true;
            }

            if (featureFactory.test_month(text)) {
                features.month = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            Matcher m2 = featureFactory.year.matcher(text);
            if (m2.find()) {
                features.year = true;
            }

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isLocationToken) {
                features.isKnownLocation = true;
            }

            if (featureFactory.test_country(text)) {
                features.isKnownCountry = true;
            }

            if (featureFactory.test_city(text)) {
                features.isKnownCity = true;
            }

            if (isTitleToken) {
                features.isKnownTitle = true;
            }

            if (isSuffixToken) {
                features.isKnownSuffix = true;
            }

            features.label = tag;

            patient.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }

        return patient.toString();
    }


    /**
     * Add features for patient parsing
     */
    static public String addFeaturesPatientAnonym(List<LayoutToken> tokens,
                                            List<String> labels,
                                            List<OffsetPosition> locationPositions,
                                            List<OffsetPosition> titlePositions,
                                            List<OffsetPosition> suffixPositions,
                                            List<String> dataOriginal,
                                            List<String> dataAnonymized) throws Exception {
        if ((locationPositions == null) ||
            (titlePositions == null) ||
            (suffixPositions == null)) {
            throw new GrobidException("At least one list of gazetteer matches positions is null.");
        }

        FeatureFactory featureFactory = FeatureFactory.getInstance();

        StringBuilder patient = new StringBuilder();

        int currentLocationPositions = 0;
        int currentTitlePositions = 0;
        int currentSuffixPositions = 0;

        boolean isLocationToken;
        boolean isTitleToken;
        boolean isSuffixToken;
        boolean skipTest;

        String previousTag = null;
        String previousText = null;
        FeaturesVectorPatient features = null;
        int sentenceLenth = tokens.size(); // length of the current sentence
        for (int n=0; n < tokens.size(); n++) {
            LayoutToken token = tokens.get(n);
            String tag = null;
            if ( (labels != null) && (labels.size() > 0) && (n < labels.size()) )
                tag = labels.get(n);

            boolean outputLineStatus = false;
            isLocationToken = false;
            isTitleToken = false;
            isSuffixToken = false;
            skipTest = false;

            String text = token.getText();
            if (text.equals(" ")) {
                continue;
            }

            if (text.equals("\n")) {
                // should not be the case for citation model
                continue;
            }

            // remove blank spaces
            for (int i=0; i<dataOriginal.size(); i++) {
                text = text.replace(dataOriginal.get(i), dataAnonymized.get(i));
            }
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0 ) {
                continue;
            }

            // check the position of matched locations
            skipTest = false;
            if (locationPositions != null && (locationPositions.size() > 0)) {
                if (currentLocationPositions == locationPositions.size() - 1) {
                    if (locationPositions.get(currentLocationPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentLocationPositions; i < locationPositions.size(); i++) {
                        if ((locationPositions.get(i).start <= n) &&
                            (locationPositions.get(i).end >= n)) {
                            isLocationToken = true;
                            currentLocationPositions = i;
                            break;
                        } else if (locationPositions.get(i).start > n) {
                            isLocationToken = false;
                            currentLocationPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched titles
            skipTest = false;
            if ((titlePositions != null) && (titlePositions.size() > 0)) {
                if (currentTitlePositions == titlePositions.size() - 1) {
                    if (titlePositions.get(currentTitlePositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentTitlePositions; i < titlePositions.size(); i++) {
                        if ((titlePositions.get(i).start <= n) &&
                            (titlePositions.get(i).end >= n)) {
                            isTitleToken = true;
                            currentTitlePositions = i;
                            break;
                        } else if (titlePositions.get(i).start > n) {
                            isTitleToken = false;
                            currentTitlePositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched suffix
            skipTest = false;
            if (suffixPositions != null  && (suffixPositions.size() > 0)) {
                if (currentSuffixPositions == suffixPositions.size() - 1) {
                    if (suffixPositions.get(currentSuffixPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentSuffixPositions; i < suffixPositions.size(); i++) {
                        if ((suffixPositions.get(i).start <= n) &&
                            (suffixPositions.get(i).end >= n)) {
                            isSuffixToken = true;
                            currentSuffixPositions = i;
                            break;
                        } else if (suffixPositions.get(i).start > n) {
                            isSuffixToken = false;
                            currentSuffixPositions = i;
                            break;
                        }
                    }
                }
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorPatient();
            features.string = text;
            features.relativePosition = featureFactory.linearScaling(n, sentenceLenth, nbBins);

            if (n == 0) {
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
            } else if (tokens.size() == n+1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
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

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            if (featureFactory.test_common(text)) {
                features.commonName = true;
            }

            if (featureFactory.test_names(text)) {
                features.properName = true;
            }

            if (featureFactory.test_month(text)) {
                features.month = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            Matcher m2 = featureFactory.year.matcher(text);
            if (m2.find()) {
                features.year = true;
            }

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isLocationToken) {
                features.isKnownLocation = true;
            }

            if (featureFactory.test_country(text)) {
                features.isKnownCountry = true;
            }

            if (featureFactory.test_city(text)) {
                features.isKnownCity = true;
            }

            if (isTitleToken) {
                features.isKnownTitle = true;
            }

            if (isSuffixToken) {
                features.isKnownSuffix = true;
            }

            features.label = tag;

            patient.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }

        return patient.toString();
    }

}