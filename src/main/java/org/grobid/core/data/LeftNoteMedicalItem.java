package org.grobid.core.data;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.util.ClassicMedicEmailAssigner;
import org.grobid.core.data.util.EmailSanitizer;
import org.grobid.core.data.util.MedicEmailAssigner;
import org.grobid.core.document.Document;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Class for representing and exchanging items of left-note part of medical reports.
 * <p>
 * Tanti, 2020
 */
public class LeftNoteMedicalItem {
    protected static final Logger LOGGER = LoggerFactory.getLogger(LeftNoteMedicalItem.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    private MedicEmailAssigner medicEmailAssigner = new ClassicMedicEmailAssigner();
    private EmailSanitizer emailSanitizer = new EmailSanitizer();
    private String teiId;
    //TODO: keep in sync with teiId - now teiId is generated in many different places
    private Integer ordinal;
    private List<BoundingBox> coordinates = null;

    // map of labels (e.g. <title> or <abstract>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    private List<LayoutToken> medicsLayoutTokens = new ArrayList<>();

    @Override
    public String toString() {
        return "HeaderItem{" +
            ", languageUtilities=" + languageUtilities +
            ", language='" + language + '\'' +
            ", institution='" + institution + '\'' +
            ", website='" + website + '\'' +
            ", medics='" + medics + '\'' +
            ", location='" + location + '\'' +
            ", affiliation='" + affiliation + '\'' +
            ", address='" + address + '\'' +
            ", country='" + country + '\'' +
            ", town='" + town + '\'' +
            ", email='" + email + '\'' +
            ", phone='" + phone + '\'' +
            ", web='" + web + '\'' +
            ", locationDocument='" + location + '\'' +
            ", medicList=" + medicList +
            ", affiliationList=" + affiliationList +
            ", addressList=" + addressList +
            ", emailList=" + emailList +
            ", webList=" + webList +
            ", phoneList=" + phoneList +
            ", fullMedics=" + fullMedics +
            ", fullAffiliations=" + fullAffiliations +
            ", affiliationAddressBlock='" + affiliationAddressBlock + '\'' +
            ", beginPage=" + beginPage +
            ", endPage=" + endPage +
            ", year='" + year + '\'' +
            ", medicString='" + medicString + '\'' +
            ", path='" + path + '\'' +
            ", originalAffiliation='" + originalAffiliation + '\'' +
            '}';
    }

    private String language = null;
    private String website = null;
    private String address = null;
    private String placeName = null;
    private String country = null;
    private String town = null;
    private String email = null;
    private String phone = null;
    private String web = null;
    private String fax = null;
    private String medics = null;
    private String location = null;
    private String institution = null;
    private String affiliation = null;

    private String locationDocument = null;

    // abstract labeled featured sequence (to produce a structured abstract with, in particular, reference callout)
    private String labeledAbstract = null;

    // advanced grobid recognitions
    private List<String> medicList;
    private List<String> affiliationList;
    private List<String> addressList;
    private List<String> emailList;
    private List<String> webList;
    private List<String> phoneList;
    private List<String> faxList;

    private List<PersonMedical> fullMedics = null;
    private List<Affiliation> fullAffiliations = null;

    public String affiliationAddressBlock = null;

    // just for articles
    private int beginPage = -1;
    private int endPage = -1;
    private String year = null; // default is publication date on print media
    private String medicString = null;
    private String path = "";

    // for OCR post-corrections
    private String originalAffiliation = null;
    private String originalMedics = null;
    private String originalPatients = null;

    public LeftNoteMedicalItem() {
    }

    public String getLanguage() {
        return this.language;
    }

    public int getBeginPage() {
        return beginPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public String getYear() {
        return year;
    }

    public String getLabeledAbstract() {
        return labeledAbstract;
    }

    public String getEmail() {
        return email;
    }


    public String getMedics() {
        return medics;
    }

    public String getLocation() {
        return location;
    }


    public String getInstitution() {
        return institution;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getAddress() {
        return address;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getCountry() {
        return country;
    }

    public String getTown() {
        return town;
    }

    public String getPhone() {
        return phone;
    }

    public String getWeb() {
        return web;
    }

    public String getFax() {
        return fax;
    }

    public String getLocationDocument() {
        return locationDocument;
    }

    public String getMedicString() {
        return medicString;
    }

    public String getOriginalAffiliation() {
        return originalAffiliation;
    }

    public String getOriginalMedics() { return originalMedics;
    }

    public List<PersonMedical> getFullMedics() {
        return fullMedics;
    }

    public List<Affiliation> getFullAffiliations() {
        return fullAffiliations;
    }

    public void setLanguage(String theLanguage) {
        this.language = StringUtils.normalizeSpace(theLanguage);
    }

    public void setBeginPage(int p) {
        beginPage = p;
    }

    public void setEndPage(int p) {
        endPage = p;
    }

    public void setYear(String y) {
        year = StringUtils.normalizeSpace(y);
    }

    public void setLabeledAbstract(String labeledAbstract) {
        this.labeledAbstract = labeledAbstract;
    }

    public void setLocationDocument(String loc) {
        locationDocument = StringUtils.normalizeSpace(loc);
    }


    public void setMedicString(String s) {
        medicString = s;
    }

    public void setFullMedics(List<PersonMedical> fullMedics) {
        fullMedics = fullMedics;
    }

    public void setFullPatients(List<PersonMedical> fullPatients) {
        fullPatients = fullPatients;
    }

    public void setFullAffiliations(List<Affiliation> full) {
        fullAffiliations = full;
        // if no id is present in the affiliation objects, we add one
        int num = 0;
        if (fullAffiliations != null) {
            for (Affiliation affiliation : fullAffiliations) {
                if (affiliation.getKey() == null) {
                    affiliation.setKey("aff" + num);
                }
                num++;
            }
        }
    }

    public void setMedics(String aut) {
        medics = aut;
    }

    public LeftNoteMedicalItem addMedicsToken(LayoutToken lt) {
        medicsLayoutTokens.add(lt);
        return this;
    }

    public List<LayoutToken> getMedicsTokens() {
        return medicsLayoutTokens;
    }

    public void addMedic(String aut) {
        if (medics == null)
            medics = aut;
        else
            medics += " ; " + aut;

        if (medicList == null)
            medicList = new ArrayList<String>();
        if (!medicList.contains(aut))
            medicList.add(aut);
    }

    public void addFullMedic(PersonMedical meds) {
        if (fullMedics == null)
            fullMedics = new ArrayList<PersonMedical>();
        if (!fullMedics.contains(meds))
            fullMedics.add(meds);
    }

    public void setLocation(String loc) {
        location = StringUtils.normalizeSpace(loc);
    }

    public void setInstitution(String inst) {
        institution = StringUtils.normalizeSpace(inst);
    }

    public void setAffiliation(String a) {
        affiliation = a;
    }

    public void setAddress(String a) {
        address = a;
    }

    public void setPlaceName(String place) {
        placeName = place;
    }

    public void setCountry(String a) {
        country = a;
    }

    public void setTown(String a) {
        town = a;
    }

    public void setEmail(String e) {
        email = e;
    }

    public void setPhone(String p) {
        phone = p;
    }

    public void setWeb(String w) {
        web = StringUtils.normalizeSpace(w);
        web = web.replace(" ", "");
    }

    public void setFax(String fax) {
        fax = fax;
    }

    public void setOriginalAffiliation(String original) {
        originalAffiliation = original;
    }

    public void setOriginalMedics(String originalMedics) {
        originalMedics = originalMedics;
    }

    public void setOriginalPatients(String originalPatients) {
        originalPatients = originalMedics;
    }

    /**
     * General string cleaining for SQL strings. This method might depend on the chosen
     * relational database.
     */
    public static String cleanSQLString(String str) {
        if (str == null)
            return null;
        if (str.length() == 0)
            return null;
        String cleanedString = "";
        boolean special = false;
        for (int index = 0; (index < str.length()); index++) {
            char currentCharacter = str.charAt(index);
            if ((currentCharacter == '\'') || (currentCharacter == '%') || (currentCharacter == '_')) {
                special = true;
                cleanedString += '\\';
            }
            cleanedString += currentCharacter;
        }

        return cleanedString;
    }


    /**
     * Reinit all the values of the current bibliographical item
     */
    public void reset() {
        language = null;
        medics = null;
        year = null;
        institution = null;
        affiliation = null;
        address = null;
        email = null;
        phone = null;
        fax = null;
        web = null;
        beginPage = -1;
        endPage = -1;
        fullMedics = null;
        fullAffiliations = null;
    }

    public void buildLeftNoteSet(HeaderSet headerSet, String path0) {
        path = path0;
        try {
            // medics
            if (medics != null) {
                StringTokenizer st = new StringTokenizer(medics, ";");
                if (st.countTokens() > 0) {
                    while (st.hasMoreTokens()) {
                        String medic = st.nextToken();
                        if (medic != null)
                            medic = medic.trim();
                        //headerSet.addmedic(TextUtilities.HTMLEncode(medic));
                        headerSet.addMedics(medic);
                    }
                }
            }

            // institution as the document owner
            if (institution != null) {
                //headerSet.add(TextUtilities.HTMLEncode(publisher));
                headerSet.addInstitutions(institution);
            }
        } catch (Exception e) {
            throw new GrobidException("Cannot build a biblioSet, because of nested exception.", e);
        }
    }

    /**
     * Attach existing recognized emails to medics (default) or patients
     */
    public void attachEmails() {
        attachEmails(fullMedics);
    }

    public void attachEmails(List<PersonMedical> folks) {
        // do we have an email field recognized?
        if (email == null)
            return;
        // we check if we have several emails in the field
        email = email.trim();
        email = email.replace(" and ", "\t");
        ArrayList<String> emailles = new ArrayList<String>();
        StringTokenizer st0 = new StringTokenizer(email, "\t");
        while (st0.hasMoreTokens()) {
            emailles.add(st0.nextToken().trim());
        }

        List<String> sanitizedEmails = emailSanitizer.splitAndClean(emailles);

        if (sanitizedEmails != null) {
            medicEmailAssigner.assign(folks, sanitizedEmails);
        }
    }

    /**
     * Attach existing recognized emails to medics
     */
    public void attachMedicEmails() {
        attachEmails(fullMedics);
    }

    /**
     * Attach existing recognized affiliations to medics
     */
    public void attachAffiliations() {
        if (fullAffiliations == null) {
            return;
        }

        if (fullMedics == null) {
            return;
        }
        int nbAffiliations = fullAffiliations.size();
        int nbmedics = fullMedics.size();

        boolean hasMarker = false;

        // do we have markers in the affiliations?
        for (Affiliation aff : fullAffiliations) {
            if (aff.getMarker() != null) {
                hasMarker = true;
                break;
            }
        }

        if (nbAffiliations == 1) {
            // we distribute this affiliation to each medic
            Affiliation aff = fullAffiliations.get(0);
            for (PersonMedical pers : fullMedics) {
                pers.addAffiliation(aff);
            }
            aff.setFailAffiliation(false);
        } else if ((nbmedics == 1) && (nbAffiliations > 1)) {
            // we put all the affiliations to the single medic
            PersonMedical pers = fullMedics.get(0);
            for (Affiliation aff : fullAffiliations) {
                pers.addAffiliation(aff);
                aff.setFailAffiliation(false);
            }
        } else if (hasMarker) {
            // we get the marker for each affiliation and try to find the related medic in the
            // original medic field
            for (Affiliation aff : fullAffiliations) {
                if (aff.getMarker() != null) {
                    String marker = aff.getMarker();
                    int from = 0;
                    int ind = 0;
                    ArrayList<Integer> winners = new ArrayList<Integer>();
                    while (ind != -1) {
                        ind = originalMedics.indexOf(marker, from);
                        boolean bad = false;
                        if (ind != -1) {
                            // we check if we have a digit/letter (1) matching incorrectly
                            //  a double digit/letter (11), or a special non-digit (*) matching incorrectly
                            //  a double special non-digit (**)
                            if (marker.length() == 1) {
                                if (Character.isDigit(marker.charAt(0))) {
                                    if (ind - 1 > 0) {
                                        if (Character.isDigit(originalMedics.charAt(ind - 1))) {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 1 < originalMedics.length()) {
                                        if (Character.isDigit(originalMedics.charAt(ind + 1))) {
                                            bad = true;
                                        }
                                    }
                                } else if (Character.isLetter(marker.charAt(0))) {
                                    if (ind - 1 > 0) {
                                        if (Character.isLetter(originalMedics.charAt(ind - 1))) {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 1 < originalMedics.length()) {
                                        if (Character.isLetter(originalMedics.charAt(ind + 1))) {
                                            bad = true;
                                        }
                                    }
                                } else if (marker.charAt(0) == '*') {
                                    if (ind - 1 > 0) {
                                        if (originalMedics.charAt(ind - 1) == '*') {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 1 < originalMedics.length()) {
                                        if (originalMedics.charAt(ind + 1) == '*') {
                                            bad = true;
                                        }
                                    }
                                }
                            }
                            if (marker.length() == 2) {
                                // case with ** as marker
                                if ((marker.charAt(0) == '*') && (marker.charAt(1) == '*')) {
                                    if (ind - 2 > 0) {
                                        if ((originalMedics.charAt(ind - 1) == '*') &&
                                            (originalMedics.charAt(ind - 2) == '*')) {
                                            bad = true;
                                        }
                                    }
                                    if (ind + 2 < originalMedics.length()) {
                                        if ((originalMedics.charAt(ind + 1) == '*') &&
                                            (originalMedics.charAt(ind + 2) == '*')) {
                                            bad = true;
                                        }
                                    }
                                    if ((ind - 1 > 0) && (ind + 1 < originalMedics.length())) {
                                        if ((originalMedics.charAt(ind - 1) == '*') &&
                                            (originalMedics.charAt(ind + 1) == '*')) {
                                            bad = true;
                                        }
                                    }
                                }
                            }
                        }

                        if ((ind != -1) && !bad) {
                            // we find the associated medic name
                            String original = originalMedics.toLowerCase();
                            int p = 0;
                            int best = -1;
                            int ind2 = -1;
                            int bestDistance = 1000;
                            for (PersonMedical pers : fullMedics) {
                                if (!winners.contains(Integer.valueOf(p))) {
                                    String lastname = pers.getLastName();

                                    if (lastname != null) {
                                        lastname = lastname.toLowerCase();
                                        ind2 = original.indexOf(lastname, ind2 + 1);
                                        int dist = Math.abs(ind - (ind2 + lastname.length()));
                                        if (dist < bestDistance) {
                                            best = p;
                                            bestDistance = dist;
                                        }
                                    }
                                }
                                p++;
                            }

                            // and we associate this affiliation to this medic
                            if (best != -1) {
                                fullMedics.get(best).addAffiliation(aff);
                                aff.setFailAffiliation(false);
                                winners.add(Integer.valueOf(best));
                            }

                            from = ind + 1;
                        }
                        if (bad) {
                            from = ind + 1;
                            bad = false;
                        }
                    }
                }
            }
        } /*else if (nbmedics == nbAffiliations) {
            // risky heuristics, we distribute in this case one affiliation per medic
            // preserving medic
            // sometimes 2 affiliations belong both to 2 medics, for these case, the layout
            // positioning should be studied
            for (int p = 0; p < nbmedics; p++) {
                fullMedics.get(p).addAffiliation(fullAffiliations.get(p));
                System.out.println("attachment: " + p);
                System.out.println(fullMedics.get(p));
                fullAffiliations.get(p).setFailAffiliation(false);
            }
        }*/
    }

    /**
     * Create the TEI encoding for the medic+affiliation block for the current biblio object.
     */
    public String toTEIMedicBlock(int nbTag) {
        return toTEIMedicBlock(nbTag, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Create the TEI encoding for the medic+affiliation block for the current biblio object.
     */
    public String toTEIMedicBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        int nbmedics = 0;
        int nbAffiliations = 0;
        int nbAddresses = 0;

        boolean withCoordinates = false;
        if (config != null && config.getGenerateTeiCoordinates() != null) {
            withCoordinates = config.getGenerateTeiCoordinates().contains("persName");
        }

        // uncomment below when collaboration will be concretely added to headers
        /*
        if ( (collaboration != null) &&
            ( (fullMedics == null) || (fullMedics.size() == 0) ) ) {
            // collaboration plays at the same time the role of medic and affiliation
            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("<medic>").append("\n");
            TextUtilities.appendN(tei, '\t', nbTag+1);
            tei.append("<orgName type=\"collaboration\"");
            if (withCoordinates && (labeledTokens != null) ) {
                List<LayoutToken> collabTokens = labeledTokens.get("<collaboration>");
                if (withCoordinates && (collabTokens != null) && (!collabTokens.isEmpty())) {
                   tei.append(" coords=\"" + LayoutTokensUtil.getCoordsString(collabTokens) + "\"");
               }
            }
            tei.append(">").append(TextUtilities.HTMLEncode(collaboration)).append("</orgName>").append("\n");
            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("</medic>").append("\n");
            return tei.toString();
        }
        */

        List<PersonMedical> medics = fullMedics;

        Lexicon lexicon = Lexicon.getInstance();

        List<Affiliation> affs = fullAffiliations;
        if (affs == null)
            nbAffiliations = 0;
        else
            nbAffiliations = affs.size();

        if (medics == null)
            nbmedics = 0;
        else
            nbmedics = medics.size();
        boolean failAffiliation = true;

        //if (getmedics() != null) {
        if (medics != null) {
            failAffiliation = false;
            if (nbmedics > 0) {
                int autRank = 0;
                int contactAut = -1;
                //check if we have a single medic of contact
                for (PersonMedical medic : medics) {
                    if (medic.getEmail() != null) {
                        if (contactAut == -1)
                            contactAut = autRank;
                        else {
                            contactAut = -1;
                            break;
                        }
                    }
                    autRank++;
                }
                autRank = 0;
                for (PersonMedical medic : medics) {
                    if (medic.getLastName() != null) {
                        if (medic.getLastName().length() < 2)
                            continue;
                    }

                    if ((medic.getFirstName() == null) && (medic.getMiddleName() == null) &&
                        (medic.getLastName() == null)) {
                        continue;
                    }

                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("<medic");

                    if (autRank == contactAut) {
                        tei.append(" role=\"corresp\">\n");
                    } else
                        tei.append(">\n");

                    TextUtilities.appendN(tei, '\t', nbTag + 1);

                    String localString = medic.toTEI(withCoordinates);
                    localString = localString.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    tei.append(localString).append("\n");
                    if (medic.getEmail() != null) {
                        TextUtilities.appendN(tei, '\t', nbTag + 1);
                        tei.append("<email>" + TextUtilities.HTMLEncode(medic.getEmail()) + "</email>\n");
                    }

                    if (medic.getAffiliations() != null) {
                        for (Affiliation aff : medic.getAffiliations()) {
                            this.appendAffiliation(tei, nbTag + 1, aff, config, lexicon);
                        }
                    }

                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("</medic>\n");
                    autRank++;
                }
            }
        }

        // if the affiliations were not outputted with the medics, we add them here
        // (better than nothing!)
        if (affs != null) {
            for (Affiliation aff : affs) {
                if (aff.getFailAffiliation()) {
                    // dummy <medic> for TEI conformance
                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("<medic>\n");
                    this.appendAffiliation(tei, nbTag + 1, aff, config, lexicon);
                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("</medic>\n");
                }
            }
        } else if (affiliation != null) {
            StringTokenizer st2 = new StringTokenizer(affiliation, ";");
            int affiliationRank = 0;
            while (st2.hasMoreTokens()) {
                String aff = st2.nextToken();
                TextUtilities.appendN(tei, '\t', nbTag);
                tei.append("<medic>\n");
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<affiliation>\n");
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<orgName>" + TextUtilities.HTMLEncode(aff) + "</orgName>\n");
                if (nbAddresses == nbAffiliations) {
                    int addressRank = 0;
                    if (address != null) {
                        StringTokenizer st3 = new StringTokenizer(address, ";");
                        while (st3.hasMoreTokens()) {
                            String add = st3.nextToken();
                            if (addressRank == affiliationRank) {
                                TextUtilities.appendN(tei, '\t', nbTag + 2);
                                tei.append("<address><addrLine>" + TextUtilities.HTMLEncode(add)
                                    + "</addrLine></address>\n");
                                break;
                            }
                            addressRank++;
                        }
                    }
                }
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("</affiliation>\n");

                TextUtilities.appendN(tei, '\t', nbTag);
                tei.append("</medic>\n");

                affiliationRank++;
            }
        }
        return tei.toString();

    }

    private void appendAffiliation(
        StringBuffer tei,
        int nbTag,
        Affiliation aff,
        GrobidAnalysisConfig config,
        Lexicon lexicon
    ) {
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("<affiliation");
        if (aff.getKey() != null)
            tei.append(" key=\"").append(aff.getKey()).append("\"");
        tei.append(">\n");

        if (
            config.getIncludeRawAffiliations()
                && !StringUtils.isEmpty(aff.getRawAffiliationString())
        ) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            String encodedRawAffiliationString = TextUtilities.HTMLEncode(
                aff.getRawAffiliationString()
            );
            tei.append("<note type=\"raw_affiliation\">");
            LOGGER.debug("marker: {}", aff.getMarker());
            if (StringUtils.isNotEmpty(aff.getMarker())) {
                tei.append("<label>");
                tei.append(aff.getMarker());
                tei.append("</label> ");
            }
            tei.append(encodedRawAffiliationString);
            tei.append("</note>\n");
        }

        if (aff.getDepartments() != null) {
            if (aff.getDepartments().size() == 1) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<orgName type=\"department\">" +
                    TextUtilities.HTMLEncode(aff.getDepartments().get(0)) + "</orgName>\n");
            } else {
                int q = 1;
                for (String depa : aff.getDepartments()) {
                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    tei.append("<orgName type=\"department\" key=\"dep" + q + "\">" +
                        TextUtilities.HTMLEncode(depa) + "</orgName>\n");
                    q++;
                }
            }
        }

        if (aff.getLaboratories() != null) {
            if (aff.getLaboratories().size() == 1) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<orgName type=\"laboratory\">" +
                    TextUtilities.HTMLEncode(aff.getLaboratories().get(0)) + "</orgName>\n");
            } else {
                int q = 1;
                for (String labo : aff.getLaboratories()) {
                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    tei.append("<orgName type=\"laboratory\" key=\"lab" + q + "\">" +
                        TextUtilities.HTMLEncode(labo) + "</orgName>\n");
                    q++;
                }
            }
        }

        if (aff.getInstitutions() != null) {
            if (aff.getInstitutions().size() == 1) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<orgName type=\"institution\">" +
                    TextUtilities.HTMLEncode(aff.getInstitutions().get(0)) + "</orgName>\n");
            } else {
                int q = 1;
                for (String inst : aff.getInstitutions()) {
                    TextUtilities.appendN(tei, '\t', nbTag + 1);
                    tei.append("<orgName type=\"institution\" key=\"instit" + q + "\">" +
                        TextUtilities.HTMLEncode(inst) + "</orgName>\n");
                    q++;
                }
            }
        }

        if ((aff.getAddressString() != null) ||
            (aff.getAddrLine() != null) ||
            (aff.getPostBox() != null) ||
            (aff.getPostCode() != null) ||
            (aff.getSettlement() != null) ||
            (aff.getRegion() != null) ||
            (aff.getCountry() != null)) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);

            tei.append("<address>\n");
            if (aff.getAddressString() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<addrLine>" + TextUtilities.HTMLEncode(aff.getAddressString()) +
                    "</addrLine>\n");
            }
            if (aff.getAddrLine() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<addrLine>" + TextUtilities.HTMLEncode(aff.getAddrLine()) +
                    "</addrLine>\n");
            }
            if (aff.getPostBox() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<postBox>" + TextUtilities.HTMLEncode(aff.getPostBox()) +
                    "</postBox>\n");
            }
            if (aff.getPostCode() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<postCode>" + TextUtilities.HTMLEncode(aff.getPostCode()) +
                    "</postCode>\n");
            }
            if (aff.getSettlement() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<settlement>" + TextUtilities.HTMLEncode(aff.getSettlement()) +
                    "</settlement>\n");
            }
            if (aff.getRegion() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<region>" + TextUtilities.HTMLEncode(aff.getRegion()) +
                    "</region>\n");
            }
            if (aff.getCountry() != null) {
                String code = lexicon.getCountryCode(aff.getCountry());
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<country");
                if (code != null)
                    tei.append(" key=\"" + code + "\"");
                tei.append(">" + TextUtilities.HTMLEncode(aff.getCountry()) +
                    "</country>\n");
            }

            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("</address>\n");
        }

        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("</affiliation>\n");
    }

    private static volatile String possiblePreFixPageNumber = "[A-Ze]?";
    private static volatile String possiblePostFixPageNumber = "[A-Z]?";
    private static volatile Pattern page = Pattern.compile("(" + possiblePreFixPageNumber + "\\d+" + possiblePostFixPageNumber + ")");
    private static volatile Pattern pageDigits = Pattern.compile("\\d+");

    /**
     * Correct fields of the first medical item based on the second one and the reference string
     */
    public static void correct(LeftNoteMedicalItem med, LeftNoteMedicalItem medic) {
        //System.out.println("correct: \n" + bib.toTEI(0));
        //System.out.println("with: \n" + bibo.toTEI(0));
        if (medic.getMedics() != null)
            med.setMedics(medic.getMedics());
        if (medic.getBeginPage() != -1)
            med.setBeginPage(medic.getBeginPage());
        if (medic.getEndPage() != -1)
            med.setEndPage(medic.getEndPage());
        if (medic.getYear() != null)
            med.setYear(medic.getYear());
        if (medic.getLocation() != null)
            med.setLocation(medic.getLocation());
        // medics present in fullMedics list should be in the existing resources
        // at least the corresponding medic
        if (!CollectionUtils.isEmpty(medic.getFullMedics())) {
            if (CollectionUtils.isEmpty(med.getFullMedics()))
                med.setFullMedics(medic.getFullMedics());
            else if (medic.getFullMedics().size() == 1) {
                // we have the corresponding medic
                // check if the medic exists in the obtained list
                PersonMedical auto = (PersonMedical) medic.getFullMedics().get(0);
                List<PersonMedical> medics = med.getFullMedics();
                if (medics != null) {
                    for (PersonMedical aut : medics) {
                        if (StringUtils.isNotBlank(aut.getLastName()) && StringUtils.isNotBlank(auto.getLastName())) {
                            if (aut.getLastName().toLowerCase().equals(auto.getLastName().toLowerCase())) {
                                if (StringUtils.isBlank(aut.getFirstName()) ||
                                    (auto.getFirstName() != null &&
                                        aut.getFirstName().length() <= auto.getFirstName().length() &&
                                        auto.getFirstName().toLowerCase().startsWith(aut.getFirstName().toLowerCase()))) {
                                    aut.setFirstName(auto.getFirstName());
                                    aut.setCorresp(true);
                                    if (StringUtils.isNotBlank(auto.getEmail()))
                                        aut.setEmail(auto.getEmail());
                                    // should we also check the country ? affiliation?
                                    if (StringUtils.isNotBlank(auto.getMiddleName()) && (StringUtils.isBlank(aut.getMiddleName())))
                                        aut.setMiddleName(auto.getMiddleName());
                                }
                            }
                        }
                    }
                }
            } else if (medic.getFullMedics().size() > 1) {
                // we have the complete list of medics so we can take them from the second
                // biblio item and merge some possible extra from the first when a match is
                // reliable
                for (PersonMedical per : medic.getFullMedics()) {
                    // try to find the medic in the first item (we know it's not empty)
                    for (PersonMedical per2 : med.getFullMedics()) {


                        if (StringUtils.isNotBlank(per2.getLastName())) {
                            String aut2_lastname = per2.getLastName().toLowerCase();

                            if (StringUtils.isNotBlank(per.getLastName())) {
                                String aut_lastname = per.getLastName().toLowerCase();

                                if (aut_lastname.equals(aut2_lastname)) {
                                    // check also first name if present - at least for the initial
                                    if (StringUtils.isBlank(per2.getFirstName()) ||
                                        (StringUtils.isNotBlank(per2.getFirstName()) && StringUtils.isNotBlank(per.getFirstName()))) {
                                        // we have no first name or a match (full first name)

                                        if (StringUtils.isBlank(per2.getFirstName())
                                            ||
                                            per.getFirstName().equals(per2.getFirstName())
                                            ||
                                            (per.getFirstName().length() == 1 &&
                                                per.getFirstName().equals(per2.getFirstName().substring(0, 1)))
                                        ) {
                                            // we have a match (full or initial)
                                            if (StringUtils.isNotBlank(per2.getFirstName()) &&
                                                per2.getFirstName().length() > per.getFirstName().length())
                                                per.setFirstName(per2.getFirstName());
                                            if (StringUtils.isBlank(per.getMiddleName()))
                                                per.setMiddleName(per2.getMiddleName());
                                            if (StringUtils.isBlank(per.getTitle()))
                                                per.setTitle(per2.getTitle());
                                            if (StringUtils.isBlank(per.getSuffix()))
                                                per.setSuffix(per2.getSuffix());
                                            if (StringUtils.isBlank(per.getEmail()))
                                                per.setEmail(per2.getEmail());
                                            if (!CollectionUtils.isEmpty(per2.getAffiliations()))
                                                per.setAffiliations(per2.getAffiliations());
                                            if (!CollectionUtils.isEmpty(per2.getAffiliationBlocks()))
                                                per.setAffiliationBlocks(per2.getAffiliationBlocks());
                                            if (!CollectionUtils.isEmpty(per2.getAffiliationMarkers()))
                                                per.setAffiliationMarkers(per2.getAffiliationMarkers());
                                            if (!CollectionUtils.isEmpty(per2.getMarkers()))
                                                per.setMarkers(per2.getMarkers());
                                            if (!CollectionUtils.isEmpty(per2.getLayoutTokens()))
                                                per.setLayoutTokens(per2.getLayoutTokens());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                med.setFullMedics(medic.getFullMedics());
            }
        }
    }


    public String getTeiId() {
        return teiId;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void setCoordinates(List<BoundingBox> coordinates) {
        this.coordinates = coordinates;
    }

    public List<BoundingBox> getCoordinates() {
        return coordinates;
    }

    public Map<String, List<LayoutToken>> getLabeledTokens() {
        return labeledTokens;
    }

    public void setLabeledTokens(Map<String, List<LayoutToken>> labeledTokens) {
        this.labeledTokens = labeledTokens;
    }

    public List<LayoutToken> getLayoutTokens(TaggingLabel headerLabel) {
        if (labeledTokens == null) {
            LOGGER.debug("labeledTokens is null");
            return null;
        }
        if (headerLabel.getLabel() == null) {
            LOGGER.debug("headerLabel.getLabel() is null");
            return null;
        }
        return labeledTokens.get(headerLabel.getLabel());
    }

    public void setLayoutTokensForLabel(List<LayoutToken> tokens, TaggingLabel headerLabel) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();
        labeledTokens.put(headerLabel.getLabel(), tokens);
    }

    public void generalResultMapping(Document doc, String labeledResult, List<LayoutToken> tokenizations) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.HEADER, labeledResult, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            /*if (clusterLabel.equals(TaggingLabels.HEADER_INTRO)) {
                break;
            }*/
            List<LayoutToken> clusterTokens = cluster.concatTokens();
            List<LayoutToken> theList = labeledTokens.get(clusterLabel.getLabel());

            if (theList == null)
                theList = new ArrayList<>();
            for (LayoutToken token : clusterTokens)
                theList.add(token);
            labeledTokens.put(clusterLabel.getLabel(), theList);
        }
    }

    public void addMedicsTokens(List<LayoutToken> layoutTokens) {
        this.medicsLayoutTokens.addAll(layoutTokens);
    }
}
