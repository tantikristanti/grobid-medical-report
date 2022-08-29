package org.grobid.core.data;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Class for representing and exchanging items of header part of medical reports.
 * <p>
 * Tanti, 2020
 */
public class HeaderMedicalItem {
    protected static final Logger LOGGER = LoggerFactory.getLogger(HeaderMedicalItem.class);

    private List<BoundingBox> coordinates = null;

    // map of labels (e.g. <title> or <affiliation>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    private List<LayoutToken> titleLayoutTokens = new ArrayList<>();
    private List<LayoutToken> medicsLayoutTokens = new ArrayList<>();
    private List<LayoutToken> patientsLayoutTokens = new ArrayList<>();
    private List<LayoutToken> datelinesLayoutTokens = new ArrayList<>();

    // information regarding the document
    private String appVersion = null;
    private String language = null;
    private int nbPages = -1;
    private String document_number = null;
    private String document_type = null;
    private String title = null;
    private String document_date = null;
    private Date normalized_document_date = null;
    private String time = null;
    private String dateline = null;
    private String location = null;

    // information regarding the publisher (organization/institution who issues the document)
    private String affiliation = null;
    private String address = null;
    private String email = null;
    private String phone = null;
    private String fax = null;
    private String web = null;
    private String org = null; // information from header or left-note

    // information regarding of person (medics, patients)
    private String medics = null;
    private String patients = null;

    // header short note
    private String note = null;

    // list of medics, patients, datelines, affiliations for further process with related models
    private List<Medic> listMedics = null;
    private List<Patient> listPatients = null;
    private List<Affiliation> fullAffiliations = null;
    private List<Dateline> listDatelines = null;

    @Override
    public String toString() {
        return "HeaderItem{" +
            " language='" + language + '\'' +
            ", number_pages=" + nbPages +
            ", publisher='" + affiliation + '\'' +
            ", address='" + address + '\'' +
            ", email_publisher='" + email + '\'' +
            ", phone_publisher='" + phone + '\'' +
            ", fax_publisher='" + fax + '\'' +
            ", web_publisher='" + web + '\'' +
            ", org='" + org + '\'' +
            ", document_number='" + document_number + '\'' +
            ", document_type='" + document_type + '\'' +
            ", title='" + title + '\'' +
            ", document_date='" + document_date + '\'' +
            ", document_time='" + time + '\'' +
            ", dateline'" + dateline + '\'' +
            ", location'" + location + '\'' +
            ", medics='" + medics + '\'' +
            ", patients='" + patients + '\'' +
            '}';
    }

    public HeaderMedicalItem() {
    }

    public String getLanguage() {
        return this.language;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public int getNbPages() {
        return nbPages;
    }

    public String getDocNum() {
        return this.document_number;
    }

    public String getDocumentType() {
        return this.document_type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDocumentDate() {
        return this.document_date;
    }

    public Date getNormalizedDocumentDate() {
        return normalized_document_date;
    }

    public String getDocumentTime() {
        return this.time;
    }

    public String getDateline() {
        return this.dateline;
    }

    public String getMedics() {
        return medics;
    }

    public String getPatients() {
        return patients;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getAddress() {
        return address;
    }

    public String getOrg() {
        return org;
    }

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

    public String getLocation() {
        return this.location;
    }

    public String getNote() {
        return this.note;
    }

    public List<Medic> getListMedics() {
        return listMedics;
    }

    public List<Patient> getListPatients() {
        return listPatients;
    }

    public List<Dateline> getListDatelines() {
        return listDatelines;
    }

    public List<Affiliation> getFullAffiliations() {
        return fullAffiliations;
    }

    public List<LayoutToken> getMedicsTokens() {
        return medicsLayoutTokens;
    }

    public List<LayoutToken> getDatelinesTokens() {
        return datelinesLayoutTokens;
    }

    public List<LayoutToken> getPatientsTokens() {
        return patientsLayoutTokens;
    }

    public void setLanguage(String theLanguage) {
        this.language = StringUtils.normalizeSpace(theLanguage);
    }

    public void setNbPages(int nb) {
        this.nbPages = nb;
    }

    public void setDocNum(String idno) {
        this.document_number = StringUtils.normalizeSpace(idno);
    }

    public void setDocumentType(String theDocumentType) {
        this.document_type = StringUtils.normalizeSpace(theDocumentType);
    }

    public void setTitle(String theTitle) {
        this.title = StringUtils.normalizeSpace(theTitle);
    }

    public void setDocumentDate(String theDate) {
        this.document_date = StringUtils.normalizeSpace(theDate);
    }

    public void setDateline(String dateline) {
        this.dateline = StringUtils.normalizeSpace(dateline);
    }

    public void setLocation(String location) {
        this.location = StringUtils.normalizeSpace(location);
    }

    public void setNote(String note) {
        this.note = StringUtils.normalizeSpace(note);
    }

    public void setDocumentTime(String theTime) {
        this.time = StringUtils.normalizeSpace(theTime);
    }

    public void setNormalizedDocumentDate(Date theDate) {
        this.normalized_document_date = theDate;
    }

    public void setPatients(String thepatients) {
        this.patients = StringUtils.normalizeSpace(thepatients);
    }

    public void setMedics(String themedics) {
        this.medics = StringUtils.normalizeSpace(themedics);
    }

    public void setListMedics(List<Medic> fMedics) {
        this.listMedics = fMedics;
    }

    public void setListPatients(List<Patient> fPatients) {
        this.listPatients = fPatients;
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

    public HeaderMedicalItem addDatelinesToken(LayoutToken lt) {
        datelinesLayoutTokens.add(lt);
        return this;
    }

    public HeaderMedicalItem addMedicsToken(LayoutToken lt) {
        medicsLayoutTokens.add(lt);
        return this;
    }

    public HeaderMedicalItem addPatientsToken(LayoutToken lt) {
        patientsLayoutTokens.add(lt);
        return this;
    }

    public void addDateline(Dateline dateline) {
        if (listDatelines == null)
            listDatelines = new ArrayList<Dateline>();
        if (!listDatelines.contains(dateline))
            listDatelines.add(dateline);
    }

    public void addMedic(Medic medic) {
        if (listMedics == null)
            listMedics = new ArrayList<Medic>();
        if (!listMedics.contains(medic))
            listMedics.add(medic);
    }

    public void addPatient(Patient patient) {
        if (listPatients == null)
            listPatients = new ArrayList<Patient>();
        if (!listPatients.contains(patient))
            listPatients.add(patient);
    }

    public void setAffiliation(String a) {
        affiliation = a;
    }

    public void setAddress(String a) {
        address = a;
    }

    public void setOrg(String or) {
        org = or;
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

    public void setFax(String f) {
        fax = f;
    }


    /**
     * Reinit all the values of the current bibliographical item
     */
    public void reset() {
        language = null;
        nbPages = -1;
        document_number = null;
        document_type = null;
        title = null;
        document_date = null;
        normalized_document_date = null;
        time = null;
        dateline = null;
        location = null;
        affiliation = null;
        address = null;
        email = null;
        phone = null;
        fax = null;
        web = null;
        org = null;
        medics = null;
        patients = null;
        note = null;
    }

    /**
     * Create the TEI encoding for the dateline block for the current header object.
     */
    public String toTEIDatelineBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("<datelines>\n");
        for (Dateline dateline : listDatelines) {
            if (dateline.getDoctype() != null || dateline.getPlaceName() != null || dateline.getNote() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 1);
                tei.append("<dateline>").append("\n");
                if (dateline.getDoctype() != null) {
                    TextUtilities.appendN(tei, '\t', nbTag + 2);
                    tei.append("<note type=\"doctype\">").append(TextUtilities.HTMLEncode(dateline.getDoctype())).append("</note>");

                    if (dateline.getDate() != null) {
                        // the date has been in the ISO format using the Date model and parser
                        tei.append(" <date type=\"issued\" when=\"" + dateline.getDate() + "\">" +
                            TextUtilities.HTMLEncode(dateline.getDate()));
                        tei.append("</date>");
                    }
                    if (dateline.getTimeString() != null) {
                        tei.append(" <time>").append(TextUtilities.HTMLEncode(dateline.getTimeString())).append("</time>\n");
                    }
                } else if (dateline.getPlaceName() != null) {
                    TextUtilities.appendN(tei, '\t', nbTag + 2);
                    tei.append("<placeName>").append(TextUtilities.HTMLEncode(dateline.getPlaceName())).append("</placeName>");

                    if (dateline.getDate() != null) {
                        // the date has been in the ISO format using the Date model and parser
                        tei.append(" <date type=\"issued\" when=\"" + dateline.getDate() + "\">" +
                            TextUtilities.HTMLEncode(dateline.getDate()));
                        tei.append("</date>");
                    }
                    if (dateline.getTimeString() != null) {
                        tei.append(" <time>").append(TextUtilities.HTMLEncode(dateline.getTimeString())).append("</time>\n");
                    }
                } else if (dateline.getNote() != null) {
                    TextUtilities.appendN(tei, '\t', nbTag + 2);
                    tei.append("<note>").append(TextUtilities.HTMLEncode(dateline.getPlaceName())).append("</note>");
                    if (dateline.getDate() != null) {
                        // the date has been in the ISO format using the Date model and parser
                        tei.append(" <date type=\"issued\" when=\"").append(dateline.getDate() + "\">" +
                            TextUtilities.HTMLEncode(dateline.getDate()));
                        tei.append("</date>");
                    }
                    if (dateline.getTimeString() != null) {
                        tei.append(" <time>").append(TextUtilities.HTMLEncode(dateline.getTimeString())).append("</time>\n");
                    }
                }
            } else if (dateline.getDate() != null && dateline.getTimeString() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                // the date has been in the ISO format using the Date model and parser
                tei.append(" <date type=\"issued\" when=\"").append(dateline.getDate() + "\">" +
                    TextUtilities.HTMLEncode(dateline.getDate()));
                tei.append("</date>");
                tei.append(" <time>").append(TextUtilities.HTMLEncode(dateline.getTimeString())).append("</time>\n");
            } else if (dateline.getDate() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                // the date has been in the ISO format using the Date model and parser
                tei.append(" <date type=\"issued\" when=\"").append(dateline.getDate() + "\">" +
                    TextUtilities.HTMLEncode(dateline.getDate()));
                tei.append("</date>");
            }
            tei.append("\n");
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("</dateline>").append("\n");
        }
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("</datelines>\n");
        return tei.toString();
    }

    /**
     * Create the TEI encoding for the medics for the current header object.
     */
    public String toTEIMedicBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("<listPerson type=\"medics\">\n");
        for (Medic medic : listMedics) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<medic>").append("\n");
            if (medic.getIdno() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<idno>").append(TextUtilities.HTMLEncode(medic.getIdno())).append("</idno>\n");
            }
            if (medic.getPersName() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<persName>").append(TextUtilities.HTMLEncode(medic.getPersName())).append("</persName>\n");
            }
            if (medic.getRole() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<roleName>").append(TextUtilities.HTMLEncode(medic.getRole())).append("</roleName>\n");
            }
            if (medic.getAffiliation() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<affiliation>").append(TextUtilities.HTMLEncode(medic.getAffiliation())).append("</affiliation>\n");
            }
            if (medic.getOrganisation() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<orgName>").append(TextUtilities.HTMLEncode(medic.getOrganisation())).append("</orgName>\n");
            }
            if (medic.getInstitution() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<institution>").append(TextUtilities.HTMLEncode(medic.getInstitution())).append("</institution>\n");
            }
            if (medic.getAddress() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<address>").append(TextUtilities.HTMLEncode(medic.getAddress())).append("</address>\n");
            }
            if (medic.getCountry() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<country>").append(TextUtilities.HTMLEncode(medic.getCountry())).append("</country>\n");
            }
            if (medic.getTown() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<settlement>").append(TextUtilities.HTMLEncode(medic.getTown())).append("</settlement>\n");
            }
            if (medic.getEmail() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<email>").append(TextUtilities.HTMLEncode(medic.getEmail())).append("</email>\n");
            }
            if (medic.getPhone() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<phone>").append(TextUtilities.HTMLEncode(medic.getPhone())).append("</phone>\n");
            }
            if (medic.getFax() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<fax>").append(TextUtilities.HTMLEncode(medic.getFax())).append("</fax>\n");
            }
            if (medic.getWeb() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<ptr type=\"web\">").append(TextUtilities.HTMLEncode(medic.getWeb())).append("</ptr>\n");
            }
            if (medic.getNote() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<note type=\"medic\">").append(TextUtilities.HTMLEncode(medic.getNote())).append("</note>\n");
            }
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("</medic>\n");
        }
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("</listPerson>\n");
        return tei.toString();
    }

    /**
     * Create the TEI encoding for the patient block for the current header object.
     */
    public String toTEIPatientBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("<listPerson type=\"patients\">\n");
        TextUtilities.appendN(tei, '\t', nbTag + 1);
        tei.append("<patient>").append("\n");
        for (Patient patient : listPatients) {
            if (patient.getIDType() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<idType>").append(TextUtilities.HTMLEncode(patient.getIDType())).append("</idType>\n");
            }
            if (patient.getID() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<idno>").append(TextUtilities.HTMLEncode(patient.getID())).append("</idno>\n");
            }
            if (patient.getPersName() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<persName>").append(TextUtilities.HTMLEncode(patient.getPersName())).append("</persName>\n");
            }
            if (patient.getSex() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<sex type=\"" + TextUtilities.HTMLEncode(patient.getSex()) + "\">").append(TextUtilities.HTMLEncode(patient.getSex()));
                tei.append("</sex>\n");
            }
            if (patient.getDateBirth() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<birth when=\"" + TextUtilities.HTMLEncode(patient.getDateBirth()) + "\">").append(TextUtilities.HTMLEncode(patient.getDateBirth()));
                tei.append("</birth>\n");
            }
            if (patient.getAge() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<age>").append(TextUtilities.HTMLEncode(patient.getAge())).append("</age>\n");
            }
            if (patient.getDateDeath() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<death when=\"" + TextUtilities.HTMLEncode(patient.getDateDeath()) + "\">").append(TextUtilities.HTMLEncode(patient.getDateDeath()));
                tei.append("</death>\n");
            }
            if (patient.getAddress() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<address>").append(TextUtilities.HTMLEncode(patient.getAddress())).append("</address>\n");
            }
            if (patient.getCountry() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<country>").append(TextUtilities.HTMLEncode(patient.getCountry())).append("</country>\n");
            }
            if (patient.getTown() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<settlement>").append(TextUtilities.HTMLEncode(patient.getTown())).append("</settlement>\n");
            }
            if (patient.getPhone() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<phone>").append(TextUtilities.HTMLEncode(patient.getPhone())).append("</phone>\n");
            }
            if (patient.getNote() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<note type=\"patient\">").append(TextUtilities.HTMLEncode(patient.getNote())).append("</note>\n");
            }
            TextUtilities.appendN(tei, '\t', nbTag + 1);
        }
        tei.append("</patient>\n");
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("</listPerson>\n");
        return tei.toString();
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

    public void generalResultMapping(String labeledResult, List<LayoutToken> tokenizations) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.HEADER_MEDICAL_REPORT, labeledResult, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> clusterTokens = cluster.concatTokens();
            List<LayoutToken> theList = labeledTokens.get(clusterLabel.getLabel());

            theList = theList == null ? new ArrayList<>() : theList;
            theList.addAll(clusterTokens);
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

    public void addDatelinesTokens(List<LayoutToken> layoutTokens) {
        this.datelinesLayoutTokens.addAll(layoutTokens);
    }
}
