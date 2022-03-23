package org.grobid.core.features;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Class for features used for NER in raw texts.
 *
 * Adapted from grobid-ner (@author Patrice Lopez)
 *
 * Tanti, 2021
 */
public class FeaturesVectorMedicalNER {
    public String string = null;     // lexical feature
    public String label = null;     // label if known

    public String capitalisation = null;// one of INITCAP, ALLCAPS, NOCAPS
    public String digit;                // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE

    // lexical features
    public boolean lastName = false;
    public boolean commonName = false;
    public boolean properName = false;
    public boolean firstName = false;
    public boolean year = false;
    public boolean month = false;
    public boolean cityName = false;
    public boolean countryName = false;

    public boolean anatomy = false;
    public boolean chemical = false;
    public boolean device = false;
    public boolean disorder = false;
    public boolean livingBeing = false;
    public boolean object = false;
    public boolean phenomena = false;
    public boolean physiology = false;
    public boolean procedure = false;

    public String shadowNumber = null; // Convert digits to “0”

    public String wordShape = null;
    // Convert upper-case letters to "X", lower- case letters to "x", digits to "d" and other to "c"
    // there is also a trimmed variant where sequence of similar character shapes are reduced to one
    // converted character shape
    public String wordShapeTrimmed = null;
    public boolean locationName = false;
    public boolean email = false;
    public boolean http = false;
    public boolean isLocationToken = false;
    public boolean isPersonTitleToken = false;
    public boolean isPersonSuffixToken = false;
    public boolean isOrganisationToken = false;
    public boolean isOrgFormToken = false;

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
        StringBuilder res = new StringBuilder();

        // token string (1)
        res.append(string);

        // lowercase string (1)
        res.append(" " + string.toLowerCase());

        //prefix (5)
        res.append(" " + TextUtilities.prefix(string, 1));
        res.append(" " + TextUtilities.prefix(string, 2));
        res.append(" " + TextUtilities.prefix(string, 3));
        res.append(" " + TextUtilities.prefix(string, 4));
        res.append(" " + TextUtilities.prefix(string, 5));

        //suffix (5)
        res.append(" " + TextUtilities.suffix(string, 1));
        res.append(" " + TextUtilities.suffix(string, 2));
        res.append(" " + TextUtilities.suffix(string, 3));
        res.append(" " + TextUtilities.suffix(string, 4));
        res.append(" " + TextUtilities.suffix(string, 5));

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

        // punctuation information (1)
        //res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // lexical information (11)
        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        if (properName)
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

        if (cityName)
            res.append(" 1");
        else
            res.append(" 0");

        if (countryName)
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

        if (locationName)
            res.append(" 1");
        else
            res.append(" 0");

        if (email)
            res.append(" 1");
        else
            res.append(" 0");

        if (http)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information of medical terminologies (8)
        if (anatomy)
            res.append(" 1");
        else
            res.append(" 0");

        if (chemical)
            res.append(" 1");
        else
            res.append(" 0");

        if (device)
            res.append(" 1");
        else
            res.append(" 0");

        if (disorder)
            res.append(" 1");
        else
            res.append(" 0");

        if (livingBeing)
            res.append(" 1");
        else
            res.append(" 0");

        if (phenomena)
            res.append(" 1");
        else
            res.append(" 0");

        if (physiology)
            res.append(" 1");
        else
            res.append(" 0");

        if (procedure)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical feature: belongs to a known location (1)
        if (isLocationToken)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical feature: belongs to a known person title (1)
        if (isPersonTitleToken)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical feature: belongs to a known person suffix (1)
        if (isPersonSuffixToken)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical feature: belongs to a known organisation (1)
        if (isOrganisationToken)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical feature: belongs to a known organisation form (1)
        if (isOrgFormToken)
            res.append(" 1");
        else
            res.append(" 0");

        // token length (1)
        //res.append(" " + string.length()); // /

        // shadow number (1)
        //res.append(" " + shadowNumber); // /

        // word shape (1)
        res.append(" " + wordShape);

        // word shape trimmed (1)
        res.append(" " + wordShapeTrimmed);

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "");
        else
            res.append(" 0");

        return res.toString();
    }

    /**
     * Add feature for medic parsing.
     */
    static public String addFeaturesNER(List<LayoutToken> tokens, // tokens and layouts
                                          List<String> labels, // labels
                                          List<OffsetPosition> locationPositions,
                                          List<OffsetPosition> titlePositions,
                                          List<OffsetPosition> suffixPositions,
                                          List<OffsetPosition> emailPositions,
                                          List<OffsetPosition> urlPositions) throws Exception {

        if ((locationPositions == null) ||
            (titlePositions == null) ||
            (suffixPositions == null) ||
            (emailPositions == null) ||
            (urlPositions == null)) {
            throw new GrobidException("At least one list of gazetteer matches positions is null.");
        }

        FeatureFactory featureFactory = FeatureFactory.getInstance();
        FeatureFactoryMedical featureFactoryMedical = FeatureFactoryMedical.getInstance();

        StringBuilder featuresNer = new StringBuilder();

        int currentLocationPositions = 0;
        int currentTitlePositions = 0;
        int currentSuffixPositions = 0;
        int currentEmailPositions = 0;
        int currentUrlPositions = 0;

        boolean isLocationToken;
        boolean isTitleToken;
        boolean isSuffixToken;
        boolean isEmailToken;
        boolean isUrlToken;
        boolean skipTest;

        String previousTag = null;
        String previousText = null;
        FeaturesVectorMedicalNER features = null;
        int sentenceLength = tokens.size(); // length of the current sentence
        for (int n=0; n < tokens.size(); n++) {
            LayoutToken token = tokens.get(n);
            String tag = null;
            if ( (labels != null) && (labels.size() > 0) && (n < labels.size()) )
                tag = labels.get(n);

            boolean outputLineStatus = false;

            isLocationToken = false;
            isTitleToken = false;
            isSuffixToken = false;
            isEmailToken = false;
            isUrlToken = false;
            skipTest = false;

            String text = token.getText();

            if ((text == null) ||
                (text.length() == 0) ||
                text.equals(" ") ||
                text.equals("\n") ||
                text.equals("\r") ||
                text.equals("\t") ||
                text.equals("\u00A0")) {
                continue;
            }

            // remove blank spaces
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);

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
            if (titlePositions != null && (titlePositions.size() > 0)) {
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
            // check the position of matched suffixes
            skipTest = false;
            if (suffixPositions != null && (suffixPositions.size() > 0)) {
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
            // check the position of matched email
            skipTest = false;
            if (emailPositions != null && (emailPositions.size() > 0)) {
                if (currentEmailPositions == emailPositions.size() - 1) {
                    if (emailPositions.get(currentEmailPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentEmailPositions; i < emailPositions.size(); i++) {
                        if ((emailPositions.get(i).start <= n) &&
                            (emailPositions.get(i).end >= n)) {
                            isEmailToken = true;
                            currentEmailPositions = i;
                            break;
                        } else if (emailPositions.get(i).start > n) {
                            isEmailToken = false;
                            currentEmailPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matched url
            skipTest = false;
            if (urlPositions != null && (urlPositions.size() > 0)) {
                if (currentUrlPositions == urlPositions.size() - 1) {
                    if (urlPositions.get(currentUrlPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentUrlPositions; i < urlPositions.size(); i++) {
                        if ((urlPositions.get(i).start <= n) &&
                            (urlPositions.get(i).end >= n)) {
                            isUrlToken = true;
                            currentUrlPositions = i;
                            break;
                        } else if (urlPositions.get(i).start > n) {
                            isUrlToken = false;
                            currentUrlPositions = i;
                            break;
                        }
                    }
                }
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorMedicalNER();
            features.string = text;

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

            if (isTitleToken) {
                features.isPersonTitleToken = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            if (isSuffixToken) {
                features.isPersonSuffixToken = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isLocationToken || featureFactoryMedical.test_geography(text)) {
                features.locationName = true;
            }

            if (featureFactory.test_country(text)) {
                features.countryName = true;
            }

            if (featureFactory.test_city(text)) {
                features.cityName = true;
            }

            if (isEmailToken) {
                features.email = true;
            }

            if (isUrlToken) {
                features.http = true;
            }

            if (featureFactoryMedical.test_anatomies(text)) {
                features.anatomy = true;
            }

            if (featureFactoryMedical.test_drugs_chemicals(text)) {
                features.chemical = true;
            }

            if (featureFactoryMedical.test_devices(text) || featureFactoryMedical.test_objects(text) ) {
                features.device = true;
            }

            if (featureFactoryMedical.test_disorders(text)) {
                features.disorder = true;
            }

            if (featureFactoryMedical.test_living_beings(text)) {
                features.livingBeing = true;
            }

            if (featureFactoryMedical.test_phenomena(text)) {
                features.phenomena = true;
            }

            if (featureFactoryMedical.test_physiology(text)) {
                features.physiology = true;
            }

            if (featureFactoryMedical.test_procedures(text)) {
                features.procedure = true;
            }

            features.wordShape = TextUtilities.wordShape(text);

            features.wordShapeTrimmed = TextUtilities.wordShapeTrimmed(text);

            features.label = tag;

            featuresNer.append(features.printVector()+"\n");

            previousTag = tag;
            previousText = text;
        }

        return featuresNer.toString();
    }

}
	
	
