package org.grobid.core.data;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.util.ClassicMedicEmailAssigner;
import org.grobid.core.data.util.EmailSanitizer;
import org.grobid.core.data.util.MedicEmailAssigner;
import org.grobid.core.document.Document;
import org.grobid.core.document.TEIFormatter;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.KeyGen;
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
            ", languageUtilities=" + languageUtilities +
            ", language='" + language + '\'' +
            ", title='" + title + '\'' +
            ", institution='" + institution + '\'' +
            ", subtitle='" + subtitle + '\'' +
            ", document_date='" + document_date + '\'' +
            ", normalized_publication_date=" + normalized_document_date +
            ", patients='" + patients + '\'' +
            ", owner_website='" + owner_website + '\'' +
            ", document_number='" + document_number + '\'' +
            ", month='" + month + '\'' +
            ", medics='" + medics + '\'' +
            ", firstMedicSurname='" + firstMedicSurname + '\'' +
            ", location='" + location + '\'' +
            ", pageRange='" + pageRange + '\'' +
            ", affiliation='" + affiliation + '\'' +
            ", address='" + address + '\'' +
            ", country='" + country + '\'' +
            ", town='" + town + '\'' +
            ", email='" + email + '\'' +
            ", docNumGeneral='" + docNumGeneral + '\'' +
            ", phone='" + phone + '\'' +
            ", web='" + web + '\'' +
            ", day='" + day + '\'' +
            ", locationDocument='" + location + '\'' +
            ", medicList=" + medicList +
            ", patientList=" + patientList +
            ", affiliationList=" + affiliationList +
            ", addressList=" + addressList +
            ", emailList=" + emailList +
            ", webList=" + webList +
            ", phoneList=" + phoneList +
            ", fullMedics=" + fullMedics +
            ", fullpatients=" + fullPatients +
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
    private String document_number = null; // print/default
    private String docNumGeneral = null;
    private String title = null;
    private String subtitle = null;
    private String owner_website = null;
    private String document_date = null;
    private String document_dateline = null;
    private Date normalized_document_date = null;
    private String address = null;
    private String country = null;
    private String town = null;
    private String email = null;
    private String phone = null;
    private String web = null;
    private String fax = null;
    private String day = null;
    private String month = null;
    private String medics = null;
    private String patients = null;
    private String firstMedicSurname = null;
    private String location = null;
    private String pageRange = null;
    private String institution = null;
    private String affiliation = null;

    private String locationDocument = null;

    // abstract labeled featured sequence (to produce a structured abstract with, in particular, reference callout)
    private String labeledAbstract = null;

    // advanced grobid recognitions
    private List<String> medicList;
    private List<String> patientList;
    private List<String> affiliationList;
    private List<String> addressList;
    private List<String> emailList;
    private List<String> webList;
    private List<String> phoneList;
    private List<String> faxList;

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

    public HeaderMedicalItem() {
    }

    public String getTitle() {
        return this.title;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getSubtitle() {
        if (subtitle != null)
            if (subtitle.length() != 0)
                if (!subtitle.equals("null"))
                    return this.subtitle;
        return null;
    }

    public String getDocumentDate() {
        return this.document_date;
    }

    public String getDocumentDateLine() {
        return this.document_dateline;
    }

    public Date getNormalizedDocumentDate() {
        return normalized_document_date;
    }

    public String getPatients() {
        return this.patients;
    }

    public String getOwnerWebsite() {
        return this.owner_website;
    }

    public String getDocNum() {
        return this.document_number;
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

    public String getLabeledAbstract() {
        return labeledAbstract;
    }

    public String getEmail() {
        return email;
    }

    public String getDocNumGeneral() {
        return docNumGeneral;
    }

    public String getMedics() {
        return medics;
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

    public String getInstitution() {
        return institution;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getAddress() {
        return address;
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

    public String getDay() {
        return day;
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

    public List<PersonMedical> getFullPatients() {
        return fullPatients;
    }

    public List<Affiliation> getFullAffiliations() {
        return fullAffiliations;
    }

    public void setTitle(String theTitle) {
        this.title = StringUtils.normalizeSpace(theTitle);
    }

    public void setLanguage(String theLanguage) {
        this.language = StringUtils.normalizeSpace(theLanguage);
    }

    public void setSubtitle(String theSubtitle) {
        this.subtitle = StringUtils.normalizeSpace(theSubtitle);
    }

    public void setDocumentDate(String theDate) {
        this.document_date = StringUtils.normalizeSpace(theDate);
    }

    public void setDocumentDateLine(String theDate) {
        this.document_dateline = StringUtils.normalizeSpace(theDate);
    }

    public void setNormalizedDocumentDate(Date theDate) {
        this.normalized_document_date = theDate;
    }

    public void setPatients(String thepatients) {
        this.patients = StringUtils.normalizeSpace(thepatients);
    }

    public void setPublisherWebsite(String theWebsite) {
        this.owner_website = StringUtils.normalizeSpace(theWebsite);
    }

    public void setDocNum(String docNum) {
        this.document_number = StringUtils.normalizeSpace(docNum);
    }

    public void setDocNumGeneral(String docNumGeneral) {
        this.docNumGeneral = StringUtils.normalizeSpace(docNumGeneral);
    }

    public void setMonth(String theMonth) {
        this.month = StringUtils.normalizeSpace(theMonth);
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

    public HeaderMedicalItem addMedicsToken(LayoutToken lt) {
        medicsLayoutTokens.add(lt);
        return this;
    }

    public HeaderMedicalItem addPatientsToken(LayoutToken lt) {
        patientsLayoutTokens.add(lt);
        return this;
    }

    public List<LayoutToken> getMedicsTokens() {
        return medicsLayoutTokens;
    }

    public List<LayoutToken> getPatientsTokens() {
        return patientsLayoutTokens;
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

    public void addFullPatient(PersonMedical pats) {
        if (fullPatients == null)
            fullPatients = new ArrayList<PersonMedical>();
        if (!fullPatients.contains(pats))
            fullPatients.add(pats);
    }

    public void addPatient(String pat) {
        if (patients == null)
            patients = pat;
        else
            patients += " ; " + pat;

        if (patientList == null)
            patientList = new ArrayList<String>();
        if (!patientList.contains(pat))
            patientList.add(pat);
    }

    public void setLocation(String loc) {
        location = StringUtils.normalizeSpace(loc);
    }

    public void setPageRange(String pages) {
        pageRange = StringUtils.normalizeSpace(pages);
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

    public void setDay(String d) {
        day = d;
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
     * Special string cleaining of ISBN and ISSN numbers.
     */
    public static String cleanISBNString(String str) {
        String cleanedString = "";
        for (int index = 0; (index < str.length()); index++) {
            char currentCharacter = str.charAt(index);
            if ((currentCharacter != '-') && (currentCharacter != ' ') && (currentCharacter != '\''))
                cleanedString += currentCharacter;
        }

        return StringUtils.normalizeSpace(cleanedString);
    }

    /**
     * Reinit all the values of the current bibliographical item
     */
    public void reset() {
        language = null;
        title = null;
        subtitle = null;
        document_number = null;
        docNumGeneral = null;
        owner_website = null;
        location = null;
        document_date = null;
        normalized_document_date = null;
        medics = null;
        patients = null;
        day = null;
        month = null;
        year = null;
        pageRange = null;
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

    public static void cleanTitles(HeaderMedicalItem bibl) {
        if (bibl.getTitle() != null) {
            String localTitle = TextUtilities.cleanField(bibl.getTitle(), false);
            if (localTitle.endsWith(" y")) {
                // some markers at the end of the title are extracted from the pdf as " y" at the end of the title
                // e.g. <title level="a" type="main">Computations in finite-dimensional Lie algebras y</title>
                localTitle = localTitle.substring(0, localTitle.length() - 2);
            }
            bibl.setTitle(localTitle);
        }
    }

    /**
     * Export to BibTeX format. Use "id" as BibTeX key.
     */
    public String toBibTeX() {
        return toBibTeX("id");
    }

    /**
     * Export to BibTeX format
     *
     * @param id the BibTeX ke to use.
     */
    public String toBibTeX(String id) {
        return toBibTeX(id, new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().includeRawCitations(false).build());
    }

    /**
     * Export to BibTeX format
     *
     * @param id the BibTeX ke to use
     */
    public String toBibTeX(String id, GrobidAnalysisConfig config) {
        String type = "misc";

        StringJoiner bibtex = new StringJoiner(",\n", "@" + type + "{" + id + ",\n", "\n}\n");

        try {

            // medic
            // fullMedics has to be used instead
            StringJoiner medics = new StringJoiner(" and ", "  medic = {", "}");
            if (fullMedics != null) {
                fullMedics.stream()
                    .filter(medic -> medic != null)
                    .forEachOrdered(medic -> {
                        String med = medic.getLastName();
                        if (medic.getFirstName() != null) {
                            med += ", ";
                            med += medic.getFirstName();
                        }
                        medics.add(med);
                    });
            } else if (this.medics != null) {
                StringTokenizer st = new StringTokenizer(this.medics, ";");
                while (st.hasMoreTokens()) {
                    String medic = st.nextToken();
                    if (medic != null) {
                        medics.add(medic.trim());
                    }
                }
            }
            bibtex.add(medics.toString());

            // title
            if (title != null) {
                bibtex.add("  title = {" + title + "}");
            }

            // patients
            if (patients != null) {
                String locpatients = patients.replace(" ; ", " and ");
                bibtex.add("  patient = {" + locpatients + "}");
            }
            // fullpatients has to be used instead

            // year
            if (document_date != null) {
                bibtex.add("  date = {" + document_date + "}");
            }

            // address
            if (location != null) {
                bibtex.add("  address = {" + location + "}");
            }

            // pages
            if (pageRange != null) {
                bibtex.add("  pages = {" + pageRange + "}");
            }

        } catch (Exception e) {
            LOGGER.error("Cannot export BibTex format, because of nested exception.", e);
            throw new GrobidException("Cannot export BibTex format, because of nested exception.", e);
        }
        return bibtex.toString();
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

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n - the index of the bibliographical record, the corresponding id will be b+n
     */
    public String toTEI(int n) {
        return toTEI(n, 0, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n - the index of the bibliographical record, the corresponding id will be b+n
     */
    public String toTEI(int n, GrobidAnalysisConfig config) {
        return toTEI(n, 0, config);
    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n      - the index of the bibliographical record, the corresponding id will be b+n
     * @param indent - the tabulation indentation for the output of the xml elements
     */
    public String toTEI(int n, int indent) {
        return toTEI(n, indent, GrobidAnalysisConfig.defaultInstance());
    }

    /**
     * Export the bibliographical item into a TEI BiblStruct string
     *
     * @param n      - the index of the bibliographical record, the corresponding id will be b+n
     * @param indent - the tabulation indentation for the output of the xml elements
     */
    public String toTEI(int n, int indent, GrobidAnalysisConfig config) {
        StringBuilder tei = new StringBuilder();
        boolean generateIDs = config.isGenerateTeiIds();
        try {
            // we just produce here xml strings
            for (int i = 0; i < indent; i++) {
                tei.append("\t");
            }
            tei.append("<biblStruct");
            boolean withCoords = (config.getGenerateTeiCoordinates() != null) && (config.getGenerateTeiCoordinates().contains("biblStruct"));
            tei.append(" ");
            if (withCoords)
                tei.append(TEIFormatter.getCoordsAttribute(coordinates, withCoords)).append(" ");
            if (!StringUtils.isEmpty(language)) {
                if (n == -1) {
                    tei.append("xml:lang=\"" + language + ">\n");
                } else {
                    teiId = "b" + n;
                    tei.append("xml:lang=\"" + language + "\" xml:id=\"" + teiId + "\">\n");
                }
                // TBD: we need to ensure that the language is normalized following xml lang attributes !
            } else {
                if (n == -1) {
                    tei.append(">\n");
                } else {
                    teiId = "b" + n;
                    tei.append("xml:id=\"" + teiId + "\">\n");
                }
            }

            boolean openAnalytic = false;
            if (title == null) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<monogr>\n");
            } else {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<analytic>\n");
                openAnalytic = true;
            }

            // title
            if (title != null) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<title");
                tei.append(" level=\"m\" type=\"main\"");

                if (generateIDs) {
                    String divID = KeyGen.getKey().substring(0, 7);
                    tei.append(" xml:id=\"_" + divID + "\"");
                }
                // here check the language ?
                tei.append(" xml:lang=\"").append(language)
                    .append("\">").append(TextUtilities.HTMLEncode(title)).append("</title>\n");
            }

            tei.append(toTEIMedicBlock(2, config));

            if (!StringUtils.isEmpty(docNumGeneral)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<idno>").append(TextUtilities.HTMLEncode(docNumGeneral)).append("</idno>\n");
            }

            if (!StringUtils.isEmpty(web)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<ptr target=\"").append(TextUtilities.HTMLEncode(web)).append("\" />\n");
            }

            if (openAnalytic) {
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("</analytic>\n");
                for (int i = 0; i < indent + 1; i++) {
                    tei.append("\t");
                }
                tei.append("<monogr>\n");
            }

            // a document, not a journal and not something in a book...
            if (patients != null) {
                //postProcessingpatients();

                StringTokenizer st = new StringTokenizer(patients, ";");
                if (st.countTokens() > 0) {
                    while (st.hasMoreTokens()) {
                        String patient = st.nextToken();
                        if (patient != null) {
                            patient = patient.trim();
                            for (int i = 0; i < indent + 2; i++) {
                                tei.append("\t");
                            }
                            tei.append("<patient>" + TextUtilities.HTMLEncode(patient) + "</patient>\n");
                        }
                    }
                } else {
                    if (patients != null) {
                        for (int i = 0; i < indent + 2; i++) {
                            tei.append("\t");
                        }
                        tei.append("<patient>" + TextUtilities.HTMLEncode(patients) + "</patient>\n");
                    }
                }
            }

            for (int i = 0; i < indent + 2; i++) {
                tei.append("\t");
            }
            if ((document_date != null) || (pageRange != null) || (location != null) || (institution != null)) {
                tei.append("<imprint>\n");
            } else {
                tei.append("<imprint/>\n");
            }
            // date
            if (normalized_document_date != null) {
                if ((normalized_document_date.getDay() != -1) |
                    (normalized_document_date.getMonth() != -1) |
                    (normalized_document_date.getYear() != -1)) {
                    int year = normalized_document_date.getYear();
                    int month = normalized_document_date.getMonth();
                    int day = normalized_document_date.getDay();

                    if (year != -1) {
                        String when = "";
                        if (year <= 9)
                            when += "000" + year;
                        else if (year <= 99)
                            when += "00" + year;
                        else if (year <= 999)
                            when += "0" + year;
                        else
                            when += year;
                        if (month != -1) {
                            if (month <= 9)
                                when += "-0" + month;
                            else
                                when += "-" + month;
                            if (day != -1) {
                                if (day <= 9)
                                    when += "-0" + day;
                                else
                                    when += "-" + day;
                            }
                        }
                        for (int i = 0; i < indent + 3; i++) {
                            tei.append("\t");
                        }
                        tei.append("<date type=\"issued\" when=\"");
                        tei.append(when + "\" />\n");
                    }
                } else if (this.getYear() != null) {
                    String when = "";
                    if (this.getYear().length() == 1)
                        when += "000" + this.getYear();
                    else if (this.getYear().length() == 2)
                        when += "00" + this.getYear();
                    else if (this.getYear().length() == 3)
                        when += "0" + this.getYear();
                    else if (this.getYear().length() == 4)
                        when += this.getYear();

                    if (this.getMonth() != null) {
                        if (this.getMonth().length() == 1)
                            when += "-0" + this.getMonth();
                        else
                            when += "-" + this.getMonth();
                        if (this.getDay() != null) {
                            if (this.getDay().length() == 1)
                                when += "-0" + this.getDay();
                            else
                                when += "-" + this.getDay();
                        }
                    }
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<date type=\"issued\" when=\"");
                    tei.append(when + "\" />\n");
                } else {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<date>" + TextUtilities.HTMLEncode(document_date) + "</date>\n");
                }
            } else if (document_date != null) {
                for (int i = 0; i < indent + 3; i++) {
                    tei.append("\t");
                }
                tei.append("<date>" + TextUtilities.HTMLEncode(document_date) + "</date>\n");
            }

            if (pageRange != null) {
                StringTokenizer st = new StringTokenizer(pageRange, "--");
                if (st.countTokens() == 2) {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<biblScope unit=\"page\" from=\"" +
                        TextUtilities.HTMLEncode(st.nextToken()) +
                        "\" to=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\" />\n");
                } else {
                    for (int i = 0; i < indent + 3; i++) {
                        tei.append("\t");
                    }
                    tei.append("<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange) + "</biblScope>\n");
                }
            }
            if (location != null) {
                for (int i = 0; i < indent + 3; i++) {
                    tei.append("\t");
                }
                tei.append("<pubPlace>" + TextUtilities.HTMLEncode(location) + "</pubPlace>\n");
            }

            if ((document_date != null) || (pageRange != null) || (location != null) || (institution != null)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("</imprint>\n");
            }

            if (!StringUtils.isEmpty(institution)) {
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("<respStmt>\n");
                for (int i = 0; i < indent + 3; i++) {
                    tei.append("\t");
                }
                tei.append("<orgName>" + TextUtilities.HTMLEncode(institution) + "</orgName>\n");
                for (int i = 0; i < indent + 2; i++) {
                    tei.append("\t");
                }
                tei.append("</respStmt>\n");
            }

            for (int i = 0; i < indent + 1; i++) {
                tei.append("\t");
            }
            tei.append("</monogr>\n");

            for (int i = 0; i < indent; i++) {
                tei.append("\t");
            }
            tei.append("</biblStruct>\n");
        } catch (Exception e) {
            throw new GrobidException("Cannot convert  bibliographical item into a TEI, " +
                "because of nested exception.", e);
        }

        return tei.toString();
    }

    public void buildHeaderSet(HeaderSet headerSet, String path0) {
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

            // patients
            if (patients != null) {
                //postProcessingpatients();

                StringTokenizer st = new StringTokenizer(patients, ";");
                if (st.countTokens() > 0) {
                    while (st.hasMoreTokens()) {
                        String patient = st.nextToken();
                        if (patient != null)
                            patient = patient.trim();
                        //headerSet.addPatients(TextUtilities.HTMLEncode(patient));
                        headerSet.addPatients(patient);
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
     * Export the bibliographical item into a TEI BiblStruct string with pointers and list sharing
     */
    public String toTEI2(HeaderSet bs) {
        String tei = "";
        try {
            // we just produce here xml strings, DOM XML objects should be used for JDK 1.4, J2E compliance thingy
            tei = "<biblStruct";
            if (language != null) {
                tei += " xml:lang=\"" + language + "\">\n";
                // TBD: the language should be normalized following xml lang attributes !
            } else {
                tei += " xml:lang=\"en\">\n";
            }

            tei += "\t<monogr>\n";

            // title
            if (title != null) {
                tei += "\t\t<title";
                tei += " level=\"m\"";
                tei += ">" + TextUtilities.HTMLEncode(title) + "</title>\n";
            } else {
                tei += "\t\t<title/>\n";
            }

            // medics
            if (medics != null) {
                StringTokenizer st = new StringTokenizer(medics, ";");
                if (st.countTokens() > 0) {
                    while (st.hasMoreTokens()) {
                        String medic = st.nextToken();
                        if (medic != null)
                            medic = medic.trim();
                        int ind = -1;
                        if (bs.getMedics() != null)
                            ind = bs.getMedics().indexOf(medic);
                        if (ind != -1) {
                            tei += "\t\t<contributor role=\"medic\">\n";
                            tei += "\t\t\t<ptr target=\"#medic" + ind + "\" />\n";
                            tei += "\t\t</contributor>\n";
                        } else {
                            tei += "\t\t<contributor role=\"medic\">" + TextUtilities.HTMLEncode(medic) +
                                "</contributor>\n";
                        }
                    }
                } else {
                    if (medics != null)
                        tei += "\t\t<medic>" + TextUtilities.HTMLEncode(medics) + "</medic>\n";
                }
            }

            if (patients != null) {
                //postProcessingpatients();

                StringTokenizer st = new StringTokenizer(patients, ";");
                if (st.countTokens() > 0) {
                    while (st.hasMoreTokens()) {
                        String patient = st.nextToken();
                        if (patient != null)
                            patient = patient.trim();
                        int ind = -1;
                        if (bs.getPatients() != null)
                            ind = bs.getPatients().indexOf(patient);
                        if (ind != -1) {
                            tei += "\t\t<contributor role=\"patient\">\n";
                            tei += "\t\t\t<ptr target=\"#patient" + ind + "\" />\n";
                            tei += "\t\t</contributor>\n";
                        } else {
                            tei += "\t\t<contributor role=\"patient\">" + TextUtilities.HTMLEncode(patient) +
                                "</contributor>\n";
                        }
                    }
                } else {
                    if (patients != null)
                        tei += "\t\t<patient>" + TextUtilities.HTMLEncode(patients) + "</patient>\n";
                }
            }

            // a document, not a journal and not something in a book...
            if ((document_date != null) || (pageRange != null) || (location != null)
                || (institution != null)) {
                tei += "\t\t<imprint>\n";
            }

            // date
            if (document_date != null) {
                tei += "\t\t\t<date>" + TextUtilities.HTMLEncode(document_date) + "</date>\n";
            }

            // page range
            if (pageRange != null) {
                StringTokenizer st = new StringTokenizer(pageRange, "--");
                if (st.countTokens() == 2) {
                    tei += "\t\t\t<biblScope unit=\"page\" from=\"" +
                        TextUtilities.HTMLEncode(st.nextToken()) +
                        "\" to=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\" />\n";
                } else {
                    tei += "\t\t\t<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange)
                        + "</biblScope>\n";
                }
            }
            if (location != null)
                tei += "\t\t\t<pubPlace>" + TextUtilities.HTMLEncode(location) + "</pubPlace>\n";

            if ((document_date != null) || (pageRange != null) || (location != null)
                || (institution != null)) {
                tei += "\t\t</imprint>\n";
            }

            tei += "\t</monogr>\n";

            tei += "</biblStruct>\n";
        } catch (Exception e) {
            throw new GrobidException("Cannot convert bibliographical item into a TEI, " +
                "because of nested exception.", e);
        }

        return tei;
    }

    public void setFirstMedicSurname(String firstMedicSurname) {
        this.firstMedicSurname = firstMedicSurname;
    }

    /**
     * Return the surname of the first medic.
     */
    public String getFirstMedicSurname() {
        if (this.firstMedicSurname != null) {
            return this.firstMedicSurname;
            //return TextUtilities.HTMLEncode(this.firstMedicSurname);
        }

        if (fullMedics != null) {
            if (fullMedics.size() > 0) {
                PersonMedical med = fullMedics.get(0);
                String sur = med.getLastName();
                if (sur != null) {
                    if (sur.length() > 0) {
                        this.firstMedicSurname = sur;
                        //return TextUtilities.HTMLEncode(sur);
                        return sur;
                    }
                }
            }
        }

        if (medics != null) {
            StringTokenizer st = new StringTokenizer(medics, ";");
            if (st.countTokens() > 0) {
                if (st.hasMoreTokens()) { // we take just the first medic
                    String medic = st.nextToken();
                    if (medic != null)
                        medic = medic.trim();
                    int ind = medic.lastIndexOf(" ");
                    if (ind != -1) {
                        this.firstMedicSurname = medic.substring(ind + 1);
                        //return TextUtilities.HTMLEncode(medic.substring(ind + 1));
                        return medic.substring(ind + 1);
                    } else {
                        this.firstMedicSurname = medic;
                        //return TextUtilities.HTMLEncode(medic);
                        return medic;
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
     * Attach existing recognized emails to medics
     */
    public void attachmedicEmails() {
        attachEmails(fullMedics);
    }

    /**
     * Attach existing recognized emails to patients
     */
    public void attachpatientEmails() {
        attachEmails(fullPatients);
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
        //System.out.println("correct: \n" + bib.toTEI(0));
        //System.out.println("with: \n" + bibo.toTEI(0));
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
        if (medic.getYear() != null)
            med.setYear(medic.getYear());
        if (medic.getNormalizedDocumentDate() != null)
            med.setNormalizedDocumentDate(medic.getNormalizedDocumentDate());
        if (medic.getMonth() != null)
            med.setMonth(medic.getMonth());
        if (medic.getDay() != null)
            med.setDay(medic.getDay());
        if (medic.getLocation() != null)
            med.setLocation(medic.getLocation());
        if (medic.getTitle() != null) {
            med.setTitle(medic.getTitle());
        }
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

    /**
     * Check is the biblio item can be considered as a minimally valid bibliographical reference.
     * A certain minimal number of core metadata have to be instanciated. Otherwise, the biblio
     * item can be considered as "garbage" extracted incorrectly.
     */
    public boolean rejectAsReference() {
        boolean titleSet = true;
        if ((title == null))
            titleSet = false;
        boolean medicSet = true;
        if (fullMedics == null)
            medicSet = false;
        // normally properties medics and medicList are null in the current Grobid version
        if (!titleSet && !medicSet)
            return true;
        else
            return false;
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
