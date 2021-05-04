package org.grobid.core.data;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidMedicalReportModels;
import org.grobid.core.data.util.ClassicMedicEmailAssigner;
import org.grobid.core.data.util.EmailSanitizer;
import org.grobid.core.data.util.MedicEmailAssigner;
import org.grobid.core.document.Document;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for representing and exchanging items of header part of medical reports.
 * <p>
 * Tanti, 2020
 */
public class HeaderMedicalItem {
    protected static final Logger LOGGER = LoggerFactory.getLogger(HeaderMedicalItem.class);

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    private MedicEmailAssigner medicEmailAssigner = new ClassicMedicEmailAssigner();
    private EmailSanitizer emailSanitizer = new EmailSanitizer();
    private String teiId;
    //TODO: keep in sync with teiId - now teiId is generated in many different places
    private Integer ordinal;
    private List<BoundingBox> coordinates = null;

    // map of labels (e.g. <title> or <abstract>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    private List<LayoutToken> titleLayoutTokens = new ArrayList<>();
    private List<LayoutToken> medicsLayoutTokens = new ArrayList<>();
    private List<LayoutToken> patientsLayoutTokens = new ArrayList<>();

    @Override
    public String toString() {
        return "HeaderItem{" +
            "  languageUtilities=" + languageUtilities +
            ", language='" + language + '\'' +
            ", document_number='" + document_number + '\'' +
            ", document_type='" + document_type + '\'' +
            ", title='" + title + '\'' +
            ", document_date='" + document_date + '\'' +
            ", normalized_publication_date=" + normalized_document_date +
            ", document_time='" + document_time + '\'' +
            ", document_date_line='" + document_dateline + '\'' +
            ", fullMedics=" + fullMedics +
            ", medics='" + medics + '\'' +
            ", fullpatients=" + fullPatients +
            ", patients='" + patients + '\'' +
            ", affiliationList=" + affiliationList +
            ", affiliation='" + affiliation + '\'' +
            ", address='" + address + '\'' +
            ", fullAffiliations=" + fullAffiliations +
            ", affiliationAddressBlock='" + affiliationAddressBlock + '\'' +
            ", org='" + org + '\'' +
            ", email='" + email + '\'' +
            ", phone='" + phone + '\'' +
            ", fax='" + fax + '\'' +
            ", web='" + web + '\'' +
            ", pageRange='" + pageRange + '\'' +
            ", beginPage=" + beginPage +
            ", endPage=" + endPage +
            ", path='" + path + '\'' +
            '}';
    }

    private String language = null;
    private String document_number = null; // print/default
    private String docNumGeneral = null;
    private String title = null;
    private String document_date = null;
    private String document_dateline = null;
    private String document_time = null;
    private String document_type = null;
    private Date normalized_document_date = null;
    private String affiliation = null;
    private String address = null;
    private String org = null;
    private String email = null;
    private String phone = null;
    private String web = null;
    private String fax = null;
    private String day = null;
    private String month = null;
    private String medics = null;
    private String firstMedicSurname = null;
    private String patients = null;
    private String location = null;
    private String pageRange = null;
    private String institution = null;


    // advanced grobid recognitions
    private List<String> medicList;
    private List<String> patientList;
    private List<String> affiliationList;

    private List<PersonMedical> fullMedics = null;
    private List<PersonMedical> fullPatients = null;
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

    public HeaderMedicalItem() {}

    public String getLanguage() {
        return this.language;
    }

    public String getDocNum() { return this.document_number; }

    public String getDocumentType() { return this.document_type; }

    public String getTitle() { return this.title; }

    public String getDocumentDate() { return this.document_date; }

    public String getDay() {
        return day;
    }

    public Date getNormalizedDocumentDate() { return normalized_document_date; }

    public String getDocumentTime() { return this.document_time; }

    public String getDocumentDateLine() {
        return this.document_dateline;
    }

    public String getMedics() {
        return medics;
    }

    public String getPatients() { return patients; }

    public String getAffiliation() { return affiliation; }

    public String getInstitution() { return institution; }

    public String getAddress() {
        return address;
    }

    public String getOrg() { return org; }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getFax() {
        return fax;
    }

    public String getWeb() {
        return web;
    }

    public String getMonth() {
        return this.month;
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

    public String getLocation() {
        return location;
    }

    public String getPageRange() {
        if (pageRange != null)
            return pageRange;
        else if ((beginPage != -1) && (endPage != -1))
            return "" + beginPage + "--" + endPage;
        else
            return null;
    }

    public List<PersonMedical> getFullMedics() {
        return fullMedics;
    }

    public List<PersonMedical> getFullPatients() { return fullPatients; }

    public List<Affiliation> getFullAffiliations() {
        return fullAffiliations;
    }

    public List<LayoutToken> getMedicsTokens() {
        return medicsLayoutTokens;
    }

    public List<LayoutToken> getPatientsTokens() {
        return patientsLayoutTokens;
    }


    public void setLanguage(String theLanguage) {
        this.language = StringUtils.normalizeSpace(theLanguage);
    }

    public void setDocNum(String idno) { this.document_number = StringUtils.normalizeSpace(idno); }

    public void setDocNumGeneral(String docNumGeneral) { docNumGeneral = StringUtils.normalizeSpace(docNumGeneral); }

    public void setDocumentType(String theDocumentType) { this.document_type = StringUtils.normalizeSpace(theDocumentType); }

    public void setTitle(String theTitle) {
        this.title = StringUtils.normalizeSpace(theTitle);
    }

    public void setDocumentDate(String theDate) { this.document_date = StringUtils.normalizeSpace(theDate); }

    public void setDocumentTime(String theTime) { this.document_time = StringUtils.normalizeSpace(theTime); }

    public void setDocumentDateLine(String theDateLine) { this.document_dateline = StringUtils.normalizeSpace(theDateLine); }

    public void setNormalizedDocumentDate(Date theDate) {
        this.normalized_document_date = theDate;
    }

    public void setPatients(String thepatients) { this.patients = StringUtils.normalizeSpace(thepatients); }

    public void setMedics(String themedics) {
        this.medics = StringUtils.normalizeSpace(themedics);
    }

    public void setBeginPage(int p) {
        beginPage = p;
    }

    public void setEndPage(int p) {
        endPage = p;
    }

    public void setFullMedics(List<PersonMedical> fMedics) { fullMedics = fMedics; }

    public void setFullPatients(List<PersonMedical> fPatients) { fullPatients = fPatients; }

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

    public HeaderMedicalItem addMedicsToken(LayoutToken lt) {
        medicsLayoutTokens.add(lt);
        return this;
    }

    public HeaderMedicalItem addPatientsToken(LayoutToken lt) {
        patientsLayoutTokens.add(lt);
        return this;
    }

    public void addFullMedic(PersonMedical meds) {
        if (fullMedics == null)
            fullMedics = new ArrayList<PersonMedical>();
        if (!fullMedics.contains(meds))
            fullMedics.add(meds);
    }

    public void addFullPatient(PersonMedical pats) {
        if (fullPatients == null)
            fullPatients = new ArrayList<PersonMedical>();
        if (!fullPatients.contains(pats))
            fullPatients.add(pats);
    }

    public void setPageRange(String pages) {
        pageRange = StringUtils.normalizeSpace(pages);
    }

    public void setAffiliation(String a) {
        affiliation = a;
    }

    public void setAddress(String a) {
        address = a;
    }

    public void setOrg(String or) { org = or; }

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

    public void setFax(String f) {
        fax = f;
    }

    public void setOriginalMedics(String medic) {
        originalMedics = medic;
    }

    public void setOriginalPatients(String patient) {
        originalPatients = patient;
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
        title = null;
        document_number = null;
        docNumGeneral = null;
        location = null;
        document_date = null;
        normalized_document_date = null;
        document_dateline = null;
        document_type = null;
        document_time = null;
        medics = null;
        patients = null;
        day = null;
        month = null;
        year = null;
        pageRange = null;
        institution = null;
        affiliation = null;
        address = null;
        org = null;
        email = null;
        phone = null;
        fax = null;
        web = null;
        beginPage = -1;
        endPage = -1;
        fullMedics = null;
        fullPatients = null;
        fullAffiliations = null;
        medicList = null;
        patientList = null;
        affiliationList = null;
        affiliationAddressBlock = null;
        medicString = null;
        originalAffiliation = null;
        originalMedics = null;
        originalPatients = null;
    }

    public static void cleanTitles(HeaderMedicalItem headerItem) {
        if (headerItem.getTitle() != null) {
            String localTitle = TextUtilities.cleanField(headerItem.getTitle(), false);
            if (localTitle.endsWith(" y")) {
                // some markers at the end of the title are extracted from the pdf as " y" at the end of the title
                // e.g. <title level="a" type="main">Computations in finite-dimensional Lie algebras y</title>
                localTitle = localTitle.substring(0, localTitle.length() - 2);
            }
            headerItem.setTitle(localTitle);
        }
    }

    /**
     * Check if the identifier pubnum is a document number. If yes, instanciate
     * the corresponding field and reset the generic pubnum field.
     */
    public void checkIdentifier() {
        // document number
        if (!StringUtils.isEmpty(docNumGeneral) && StringUtils.isEmpty(document_number)) {
            docNumGeneral = TextUtilities.cleanField(docNumGeneral, true);
            setDocNum(docNumGeneral);
            setDocNumGeneral(null);
        }
    }

    public void setFirstAuthorSurname(String firstMedicSurname) {
        this.firstMedicSurname = firstMedicSurname;
    }

    /**
     * Return the surname of the first author.
     */
    public String getFirstAuthorSurname() {
        if (this.firstMedicSurname != null) {
            return this.firstMedicSurname;
        }

        if (fullMedics != null) {
            if (fullMedics.size() > 0) {
                PersonMedical aut = fullMedics.get(0);
                String sur = aut.getLastName();
                if (sur != null) {
                    if (sur.length() > 0) {
                        this.firstMedicSurname = sur;
                        return sur;
                    }
                }
            }
        }

        if (medics != null) {
            StringTokenizer st = new StringTokenizer(medics, ";");
            if (st.countTokens() > 0) {
                if (st.hasMoreTokens()) { // we take just the first author
                    String author = st.nextToken();
                    if (author != null)
                        author = author.trim();
                    int ind = author.lastIndexOf(" ");
                    if (ind != -1) {
                        this.firstMedicSurname = author.substring(ind + 1);
                        //return TextUtilities.HTMLEncode(author.substring(ind + 1));
                        return author.substring(ind + 1);
                    } else {
                        this.firstMedicSurname = author;
                        //return TextUtilities.HTMLEncode(author);
                        return author;
                    }
                }
            }

        }
        return null;
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
     * Attach existing recognized emails to authors
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
     * Create the TEI encoding for the medic+affiliation block for the current header object.
     */
    public String toTEIMedicBlock(int nbTag) {
        return toTEIMedicBlock(nbTag, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Create the TEI encoding for the medic+affiliation block for the current header object.
     */
    public String toTEIMedicBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        if (medics != null || affiliation != null || address != null ||
            phone != null || fax != null || email != null || web != null ||
            org != null) {
            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("<listPerson type=\"medics\">\n");
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<medic>").append("\n");
            if(medics != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append(TextUtilities.HTMLEncode(medics) + "\n");
            }

            if(org != null){
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<org>").append("\n");
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append(TextUtilities.HTMLEncode(org) + "\n");
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("</org>\n");
            }


            TextUtilities.appendN(tei, '\t', nbTag + 2);
            tei.append("<affiliation>").append("\n");
            if(affiliation != null){
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append(TextUtilities.HTMLEncode(affiliation) + "\n");
            }
            if(address != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append("<address>").append("\n");
                TextUtilities.appendN(tei, '\t', nbTag + 4);
                tei.append("<addrLine>").append("\n");
                TextUtilities.appendN(tei, '\t', nbTag + 5);
                tei.append(TextUtilities.HTMLEncode(address) + "\n");
                TextUtilities.appendN(tei, '\t', nbTag + 4);
                tei.append("</addrLine>").append("\n");
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append("</address>").append("\n");
            }
            if(phone != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append("<phone>" + TextUtilities.HTMLEncode(phone.replaceAll("\\n", "")) + "</phone>\n");
            }
            if(fax != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append("<fax>" + TextUtilities.HTMLEncode(fax.replaceAll("\\n", "")) + "</fax>\n");
            }
            if(email != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append("<email>" + TextUtilities.HTMLEncode(email.replaceAll("\\n", "")) + "</email>\n");
            }
            if(web != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 3);
                tei.append("<web>" + TextUtilities.HTMLEncode(web.replaceAll("\\n", "")) + "</web>\n");
            }
            TextUtilities.appendN(tei, '\t', nbTag + 2);
            tei.append("</affiliation>\n");

            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("</medic>\n");

            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("</listPerson>\n");

        }
        // ===================original version===================
        /*StringBuffer tei = new StringBuffer();
        int nbmedics = 0;
        int nbAffiliations = 0;
        int nbAddresses = 0;

        boolean withCoordinates = false;
        if (config != null && config.getGenerateTeiCoordinates() != null) {
            withCoordinates = config.getGenerateTeiCoordinates().contains("persName");
        }

        // uncomment below when collaboration will be concretely added to headers
        *//*
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
        *//*

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
        */

        return tei.toString();
    }

    /**
     * Create the TEI encoding for the patient block for the current header object.
     */
    public String toTEIPatientBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        if (patients != null) {
            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("<listPerson type=\"patients\">\n");

            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<patient>").append("\n");

            if(patients != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append(TextUtilities.HTMLEncode(patients) + "\n");
            }

            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("</patient>\n");

            TextUtilities.appendN(tei, '\t', nbTag);
            tei.append("</listPerson>\n");

        }
        // ===================original version===================
        /*StringBuffer tei = new StringBuffer();
        int nbpatients = 0;
        int nbAffiliations = 0;
        int nbAddresses = 0;

        boolean withCoordinates = false;
        if (config != null && config.getGenerateTeiCoordinates() != null) {
            withCoordinates = config.getGenerateTeiCoordinates().contains("persName");
        }

        List<PersonMedical> patients = fullPatients;

        Lexicon lexicon = Lexicon.getInstance();

        if (patients == null)
            nbpatients = 0;
        else
            nbpatients = patients.size();

        if (patients != null) {
            if (nbpatients > 0) {
                int autRank = 0;
                int contactAut = -1;
                //check if we have a single patient of contact
                for (PersonMedical patient : patients) {
                    if (patient.getAddress() != null) {
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
                for (PersonMedical patient : patients) {
                    if (patient.getLastName() != null) {
                        if (patient.getLastName().length() < 2)
                            continue;
                    }

                    if ((patient.getFirstName() == null) && (patient.getMiddleName() == null) &&
                        (patient.getLastName() == null)) {
                        continue;
                    }

                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("<patient");

                    if (autRank == contactAut) {
                        tei.append(" role=\"corresp\">\n");
                    } else
                        tei.append(">\n");

                    TextUtilities.appendN(tei, '\t', nbTag + 1);

                    String localString = patient.toTEI(withCoordinates);
                    localString = localString.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    tei.append(localString).append("\n");
                    if (patient.getAddress() != null) {
                        TextUtilities.appendN(tei, '\t', nbTag + 1);
                        tei.append("<address><addrLine>" + TextUtilities.HTMLEncode(patient.getAddress()) + "</addrLine></address>\n");
                    }

                    TextUtilities.appendN(tei, '\t', nbTag);
                    tei.append("</patient>\n");
                    autRank++;
                }
            }
        }

        return tei.toString();*/

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
     * Try to normalize the page range, which can be expressed in abbreviated forms and with letter prefix.
     */
    public void postProcessPages() {
        if (pageRange != null) {
            Matcher matcher = page.matcher(pageRange);
            if (matcher.find()) {

                // below for the string form of the page numbers
                String firstPage = null;
                String lastPage = null;

                // alphaPrefix or alphaPostfix are for storing possible alphabetical prefix or postfix to page number,
                // e.g. "L" in Smith, G. P., Mazzotta, P., Okabe, N., et al. 2016, MNRAS, 456, L74
                // or "D" in  "Am J Cardiol. 1999, 83:143D-150D. 10.1016/S0002-9149(98)01016-9"
                String alphaPrefixStart = null;
                String alphaPrefixEnd = null;
                String alphaPostfixStart = null;
                String alphaPostfixEnd = null;

                // below for the integer form of the page numbers (part in case alphaPrefix is not null)
                int beginPage = -1;
                int endPage = -1;

                if (matcher.groupCount() > 0) {
                    firstPage = matcher.group(0);
                }

                if (firstPage != null) {
                    try {
                        beginPage = Integer.parseInt(firstPage);
                    } catch (Exception e) {
                        beginPage = -1;
                    }
                    if (beginPage != -1) {
                        pageRange = "" + beginPage;
                    } else {
                        pageRange = firstPage;

                        // try to get the numerical part of the page number, useful for later
                        Matcher matcher2 = pageDigits.matcher(firstPage);
                        if (matcher2.find()) {
                            try {
                                beginPage = Integer.parseInt(matcher2.group());
                                if (firstPage.length() > 0) {
                                    alphaPrefixStart = firstPage.substring(0, 1);
                                    // is it really alphabetical character?
                                    if (!Pattern.matches(possiblePreFixPageNumber, alphaPrefixStart)) {
                                        alphaPrefixStart = null;
                                        // look at postfix
                                        alphaPostfixStart = firstPage.substring(firstPage.length() - 1, firstPage.length());
                                        if (!Pattern.matches(possiblePostFixPageNumber, alphaPostfixStart)) {
                                            alphaPostfixStart = null;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                beginPage = -1;
                            }
                        }
                    }

                    if (matcher.find()) {
                        if (matcher.groupCount() > 0) {
                            lastPage = matcher.group(0);
                        }

                        if (lastPage != null) {
                            try {
                                endPage = Integer.parseInt(lastPage);
                            } catch (Exception e) {
                                endPage = -1;
                            }

                            if (endPage == -1) {
                                // try to get the numerical part of the page number, to be used for later
                                Matcher matcher2 = pageDigits.matcher(lastPage);
                                if (matcher2.find()) {
                                    try {
                                        endPage = Integer.parseInt(matcher2.group());
                                        if (lastPage.length() > 0) {
                                            alphaPrefixEnd = lastPage.substring(0, 1);
                                            // is it really alphabetical character?
                                            if (!Pattern.matches(possiblePreFixPageNumber, alphaPrefixEnd)) {
                                                alphaPrefixEnd = null;
                                                // look at postfix
                                                alphaPostfixEnd = lastPage.substring(lastPage.length() - 1, lastPage.length());
                                                if (!Pattern.matches(possiblePostFixPageNumber, alphaPostfixEnd)) {
                                                    alphaPostfixEnd = null;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        endPage = -1;
                                    }
                                }
                            }

                            if ((endPage != -1) && (endPage < beginPage)) {
                                // there are two possibilities:
                                // - the substitution, e.g. 433–8 -> 433--438, for example American Medical Association citation style
                                // - the addition, e.g. 433–8 -> 433--441
                                // unfortunately, it depends on the citation style

                                // we try to guess/refine the re-composition of pages

                                if (endPage >= 50) {
                                    // we assume no journal articles have more than 49 pages and is expressed as addition,
                                    // so it's a substitution
                                    int upperBound = firstPage.length() - lastPage.length();
                                    if (upperBound < firstPage.length() && upperBound > 0)
                                        lastPage = firstPage.substring(0, upperBound) + lastPage;
                                    pageRange += "--" + lastPage;
                                } else {
                                    if (endPage < 10) {
                                        // case 1 digit for endPage

                                        // last digit of begin page
                                        int lastDigitBeginPage = beginPage % 10;

                                        // if digit of lastPage lower than last digit of beginPage, it's an addition for sure
                                        if (endPage < lastDigitBeginPage)
                                            endPage = beginPage + endPage;
                                        else {
                                            // otherwise defaulting to substitution
                                            endPage = beginPage - lastDigitBeginPage + endPage;
                                        }
                                    } else if (endPage < 50) {
                                        // case 2 digit for endPage, we apply a similar heuristics
                                        int lastDigitBeginPage = beginPage % 100;
                                        if (endPage < lastDigitBeginPage)
                                            endPage = beginPage + endPage;
                                        else {
                                            // otherwise defaulting to substitution
                                            endPage = beginPage - lastDigitBeginPage + endPage;
                                        }
                                    }

                                    // we assume there is no article of more than 99 pages expressed in this abbreviated way
                                    // (which are for journal articles only, so short animals)

                                    if (alphaPrefixEnd != null)
                                        pageRange += "--" + alphaPrefixEnd + endPage;
                                    else if (alphaPostfixEnd != null)
                                        pageRange += "--" + endPage + alphaPostfixEnd;
                                    else
                                        pageRange += "--" + endPage;
                                }
                            } else if ((endPage != -1)) {
                                if (alphaPrefixEnd != null)
                                    pageRange += "--" + alphaPrefixEnd + endPage;
                                else if (alphaPostfixEnd != null)
                                    pageRange += "--" + endPage + alphaPostfixEnd;
                                else
                                    pageRange += "--" + lastPage;
                            } else {
                                pageRange += "--" + lastPage;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Correct fields of the first medical item based on the second one and the reference string
     */
    public static void correct(HeaderMedicalItem med, HeaderMedicalItem medic) {
        if (medic.getMedics() != null)
            med.setMedics(medic.getMedics());
        if (medic.getPatients() != null)
            med.setPatients(medic.getPatients());
        if (medic.getBeginPage() != -1)
            med.setBeginPage(medic.getBeginPage());
        if (medic.getEndPage() != -1)
            med.setEndPage(medic.getEndPage());
        if (medic.getPageRange() != null)
            med.setPageRange(medic.getPageRange());
        if (medic.getDocumentDate() != null)
            med.setDocumentDate(medic.getDocumentDate());
        if (medic.getDocumentTime() != null)
            med.setDocumentDate(medic.getDocumentDate());
        if (medic.getTitle() != null)
            med.setTitle(medic.getTitle());
        if (medic.getDocNum() != null)
            med.setDocNum(medic.getDocNum());

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

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidMedicalReportModels.HEADER_MEDICAL_REPORT, labeledResult, tokenizations);
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

    public void addTitleTokens(List<LayoutToken> layoutTokens) {
        this.titleLayoutTokens.addAll(layoutTokens);
    }

    public void addMedicsTokens(List<LayoutToken> layoutTokens) {
        this.medicsLayoutTokens.addAll(layoutTokens);
    }

    public void addPatientsTokens(List<LayoutToken> layoutTokens) {
        this.patientsLayoutTokens.addAll(layoutTokens);
    }
}
