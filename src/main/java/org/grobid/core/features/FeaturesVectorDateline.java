package org.grobid.core.features;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class for adding features to dateline chunk.
 */
public class FeaturesVectorDateline {
    // default bins for relative position, set experimentally (as for the citation features)
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

    public boolean year = false;
    public boolean month = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public boolean containPunct = false;
    public int relativePosition = -1;

    // true if the token is part of a predefinied name (single or multi-token)
    public boolean isKnownLocation = false;
    public boolean isKnownCountry = false;
    public boolean isKnownCity = false;

    public String wordShape = null;

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
        StringBuilder res = new StringBuilder();

        // token string (1)
        res.append(string);

        // lowercase string (1)
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

        // lexical information (5)
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

        if (year)
            res.append(" 1");
        else
            res.append(" 0");

        if (month)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // word shape (1)
        res.append(" ").append(wordShape);

        // relative position in the sequence (1)
        res.append(" ").append(relativePosition);

        // label - for training data (1)
        if (label != null)
            res.append(" ").append(label).append("\n");
        else
            res.append(" 0\n");

        return res.toString();
    }

    static public String addFeaturesDateline(List<LayoutToken> tokens,
                                             List<String> labels,
                                             List<OffsetPosition> locationPositions) throws Exception {
        if (locationPositions == null) {
            throw new GrobidException("At least one list of gazetteer matches positions is null.");
        }
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        StringBuilder dateline = new StringBuilder();

        int currentLocationPositions = 0;
        boolean isLocationToken;
        boolean skipTest;

        String previousTag = null;
        String previousText = null;

        FeaturesVectorDateline features = null;

        int sentenceLenth = tokens.size(); // length of the current sentence
        for (int n=0; n < sentenceLenth; n++) {
            LayoutToken token = tokens.get(n);
            String tag = null;
            if ( (labels != null) && (labels.size() > 0) && (n < labels.size()) )
                tag = labels.get(n);

            boolean outputLineStatus = false;
            isLocationToken = false;
            skipTest = false;

            String text = token.getText();
            if (text.equals(" ") || text.equals("\n")) {
                continue;
            }

            // remove any spaces from the tet
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0 ) {
                continue;
            }

            // check the position of matches for locations
            skipTest = false;
            if (locationPositions != null) {
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
            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorDateline();
            features.string = text;
            features.relativePosition = featureFactory.linearScaling(n, sentenceLenth, nbBins);

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

            if (featureFactory.test_month(text)) {
                features.month = true;
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

            features.label = tag;

            dateline.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }
        return dateline.toString();
    }
}