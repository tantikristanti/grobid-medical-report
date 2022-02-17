package org.grobid.core.data;

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

/**
 * Class for representing and exchanging items of left-note part of medical reports.
 * <p>
 * Tanti, 2020
 */
public class LeftNoteMedicalItem {
    protected static final Logger LOGGER = LoggerFactory.getLogger(HeaderMedicalItem.class);

    private List<BoundingBox> coordinates = null;

    // map of labels (e.g. <affiliation> or <org>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    // information regarding the document
    private String language = null;
    private String idno = null;
    private String ghu = null;
    private String chu = null;
    private String dmu = null;
    private String pole = null;
    private String site = null;
    private String institution = null;
    private String university = null;
    private String hospital = null;
    private String center = null;
    private String service = null;
    private String department = null;
    private String unit = null;
    private String sub = null;
    private String org = null;
    private String address = null;
    private String country = null;
    private String settlement = null;
    private String phone = null;
    private String fax = null;
    private String email = null;
    private String web = null;
    private String medics = null;
    // left-note short note
    private String note = null;

    // list of medics, patients, datelines, affiliations for further process with related models
    private List<Organization> listOrganizations = null;
    private List<Medic> listMedics = null;
    private List<LayoutToken> affiliationLayoutTokens = new ArrayList<>();
    private List<LayoutToken> organizationLayoutTokens = new ArrayList<>();
    private List<LayoutToken> medicsLayoutTokens = new ArrayList<>();

    @Override
    public String toString() {
        return "LeftNoteItem{" +
            " language='" + language + '\'' +
            ", idno='" + idno + '\'' +
            ", ghu='" + ghu + '\'' +
            ", chu='" + chu + '\'' +
            ", dmu='" + dmu + '\'' +
            ", pole='" + pole + '\'' +
            ", hospital='" + hospital + '\'' +
            ", center='" + center + '\'' +
            ", service='" + service + '\'' +
            ", department='" + department + '\'' +
            ", sub='" + sub + '\'' +
            ", organization='" + org + '\'' +
            ", address='" + address + '\'' +
            ", country='" + country + '\'' +
            ", town='" + settlement + '\'' +
            ", phone='" + phone + '\'' +
            ", fax='" + fax + '\'' +
            ", web='" + web + '\'' +
            ", medics='" + medics + '\'' +
            ", note='" + note + '\'' +
            '}';
    }

    public LeftNoteMedicalItem() {}

    public LeftNoteMedicalItem addAffiliationToken(LayoutToken lt) {
        affiliationLayoutTokens.add(lt);
        return this;
    }

    public void addOrganization(Organization org) {
        if (listOrganizations == null)
            listOrganizations = new ArrayList<Organization>();
        if (!listOrganizations.contains(org))
            listOrganizations.add(org);
    }

    public LeftNoteMedicalItem addOrganizationToken(LayoutToken lt) {
        organizationLayoutTokens.add(lt);
        return this;
    }

    public void addMedic(Medic medic) {
        if (listMedics == null)
            listMedics = new ArrayList<Medic>();
        if (!listMedics.contains(medic))
            listMedics.add(medic);
    }

    public LeftNoteMedicalItem addMedicToken(LayoutToken lt) {
        medicsLayoutTokens.add(lt);
        return this;
    }

    /**
     * Reinit all the values of the current bibliographical item
     */
    public void reset() {
        coordinates = null;
        labeledTokens = null;
        language = null;
        idno = null;
        ghu = null;
        chu = null;
        dmu = null;
        pole = null;
        hospital = null;
        center = null;
        service = null;
        department = null;;
        org = null;
        address = null;
        country = null;
        settlement = null;
        phone = null;
        fax = null;
        email = null;
        web = null;
        medics = null;
        note = null;
        listOrganizations = null;
        listMedics = null;
        affiliationLayoutTokens = new ArrayList<>();
        organizationLayoutTokens = new ArrayList<>();
        medicsLayoutTokens = new ArrayList<>();
    }

    /**
     * Create the TEI encoding for the left-note information
     */
    public String toTEILeftNoteBlock(int nbTag, GrobidAnalysisConfig config) {
        StringBuffer tei = new StringBuffer();
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("<listOrg>\n");
        if (ghu != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"ghu\">").append(TextUtilities.HTMLEncode(ghu)).append("</org>\n");
        }
        if (chu != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"chu\">").append(TextUtilities.HTMLEncode(chu)).append("</org>\n");
        }
        if (dmu != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"dmu\">").append(TextUtilities.HTMLEncode(dmu)).append("</org>\n");
        }
        if (pole != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"pole\">").append(TextUtilities.HTMLEncode(pole)).append("</org>\n");
        }
        if (site != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"site\">").append(TextUtilities.HTMLEncode(site)).append("</org>\n");
        }
        if (site != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"institution\">").append(TextUtilities.HTMLEncode(institution)).append("</org>\n");
        }
        if (university != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"university\">").append(TextUtilities.HTMLEncode(university)).append("</org>\n");
        }
        if (hospital != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"hospital\">").append(TextUtilities.HTMLEncode(hospital)).append("</org>\n");
        }
        if (center != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"center\">").append(TextUtilities.HTMLEncode(center)).append("</org>\n");
        }
        if (service != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"service\">").append(TextUtilities.HTMLEncode(service)).append("</org>\n");
        }
        if (department != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"department\">").append(TextUtilities.HTMLEncode(department)).append("</org>\n");
        }
        if (unit != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"unit\">").append(TextUtilities.HTMLEncode(unit)).append("</org>\n");
        }
        if (sub != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"sub\">").append(TextUtilities.HTMLEncode(sub)).append("</org>\n");
        }
        if (org != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<org>\n\t<orgName type=\"other\">").append(TextUtilities.HTMLEncode(org)).append("</org>\n");
        }
        if (address != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<address>").append(TextUtilities.HTMLEncode(address)).append("</address>\n");
        }
        if (country != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<country>").append(TextUtilities.HTMLEncode(country)).append("</country>\n");
        }
        if (settlement != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<settlement>").append(TextUtilities.HTMLEncode(settlement)).append("</settlement>\n");
        }
        if (phone != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<phone>").append(TextUtilities.HTMLEncode(phone)).append("</phone>\n");
        }
        if (fax != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<fax>").append(TextUtilities.HTMLEncode(fax)).append("</fax>\n");
        }
        if (email != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<email>").append(TextUtilities.HTMLEncode(email)).append("</email>\n");
        }
        if (web != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<ptr type=\"web\">").append(TextUtilities.HTMLEncode(web)).append("</ptr>\n");
        }
        if (note != null) {
            TextUtilities.appendN(tei, '\t', nbTag + 1);
            tei.append("<note type=\"short\">").append(TextUtilities.HTMLEncode(web)).append("</note>\n");
        }
        TextUtilities.appendN(tei, '\t', nbTag);
        tei.append("</listOrg>\n");
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
            if (medic.getRole() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<roleName>").append(TextUtilities.HTMLEncode(medic.getRole())).append("</roleName>\n");
            }
            if (medic.getPersName() != null) {
                TextUtilities.appendN(tei, '\t', nbTag + 2);
                tei.append("<persName>").append(TextUtilities.HTMLEncode(medic.getPersName())).append("</persName>\n");
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

    public List<LayoutToken> getLabeledTokens(TaggingLabel headerLabel) {
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

    public void generalResultMapping(String labeledResult, List<LayoutToken> tokenizations) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.LEFT_NOTE_MEDICAL_REPORT, labeledResult, tokenizations);
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

    public List<BoundingBox> getCoordinates() {
        return coordinates;
    }

    public Map<String, List<LayoutToken>> getLabeledTokens() {
        return labeledTokens;
    }

    public String getLanguage() {
        return language;
    }

    public String getAddress() {
        return address;
    }

    public String getCountry() {
        return country;
    }

    public String getSettlement() {
        return settlement;
    }

    public String getPhone() {
        return phone;
    }

    public String getFax() {
        return fax;
    }

    public String getEmail() {
        return email;
    }

    public String getWeb() {
        return web;
    }

    public String getMedics() {
        return medics;
    }

    public String getNote() {
        return note;
    }

    public String getIdno() {return idno;}

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public void setIdno(String idno) {this.idno = idno;}

    public List<Organization> getListOrganizations() {
        return listOrganizations;
    }

    public List<Medic> getListMedics() {
        return listMedics;
    }

    public List<LayoutToken> getAffiliationLayoutTokens() {
        return affiliationLayoutTokens;
    }

    public List<LayoutToken> getOrganizationLayoutTokens() {
        return organizationLayoutTokens;
    }

    public List<LayoutToken> getMedicsLayoutTokens() {
        return medicsLayoutTokens;
    }

    public String getCenter() {return center;}

    public void setCenter(String center) {this.center = center;}

    public String getService() {return service; }

    public void setService(String service) {this.service = service;}

    public String getDepartment() {
        return department;
    }

    public String getOrg() {
        return org;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getGhu() {
        return ghu;
    }

    public void setGhu(String ghu) {
        this.ghu = ghu;
    }

    public String getChu() {
        return chu;
    }

    public void setChu(String chu) {
        this.chu = chu;
    }

    public String getDmu() {
        return dmu;
    }

    public void setDmu(String dmu) {
        this.dmu = dmu;
    }

    public String getPole() {
        return pole;
    }

    public void setPole(String pole) {
        this.pole = pole;
    }


    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setLabeledTokens(List<LayoutToken> tokens, TaggingLabel label) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();
        labeledTokens.put(label.getLabel(), tokens);
    }

    public void setCoordinates(List<BoundingBox> coordinates) {
        this.coordinates = coordinates;
    }

    public void setLabeledTokens(Map<String, List<LayoutToken>> labeledTokens) {
        this.labeledTokens = labeledTokens;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setSettlement(String settlement) {
        this.settlement = settlement;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public void setMedics(String medics) {
        this.medics = medics;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setListOrganizations(List<Organization> listOrganizations) {
        this.listOrganizations = listOrganizations;
    }

    public void setListMedics(List<Medic> listMedics) {
        this.listMedics = listMedics;
    }

    public void setAffiliationLayoutTokens(List<LayoutToken> affiliationLayoutTokens) {
        this.affiliationLayoutTokens = affiliationLayoutTokens;
    }

    public void setOrganizationLayoutTokens(List<LayoutToken> organizationLayoutTokens) {
        this.organizationLayoutTokens = organizationLayoutTokens;
    }

    public void setMedicsLayoutTokens(List<LayoutToken> medicsLayoutTokens) {
        this.medicsLayoutTokens = medicsLayoutTokens;
    }
}
