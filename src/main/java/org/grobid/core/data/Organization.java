package org.grobid.core.data;


import org.grobid.core.GrobidModels;
import org.grobid.core.engines.OrganizationParser;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Organization {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Organization.class);

    private String language = null;
    private String orgName = null;
    private String address = null;
    private String country = null;
    private String town = null;
    private String phone = null;
    private String fax = null;
    private String email = null;
    private String web = null;
    private String note = null;
    private String medic = null;
    private List<Medic> listMedics = new ArrayList<>();
    private Map<String, List<LayoutToken>> labeledTokens;
    private List<LayoutToken> layoutTokens = new ArrayList<>();
    private List<LayoutToken> medicsLayoutTokens = new ArrayList<>();
    // list of medics, patients, datelines, affiliations for further process with related models


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<LayoutToken> getMedicsTokens() {return medicsLayoutTokens;}

    public String getMedic() {
        return medic;
    }

    public void setMedic(String medic) {
        this.medic = medic;
    }

    public Map<String, List<LayoutToken>> getLabeledTokens() {
        return labeledTokens;
    }

    public void setLabeledTokens(Map<String, List<LayoutToken>> labeledTokens) {
        this.labeledTokens = labeledTokens;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }


    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public List<Medic> getMedics() {
        return listMedics;
    }

    public void setMedics(List<Medic> medics) {
        this.listMedics = medics;
    }


    public Organization addMedicsToken(LayoutToken lt) {
        medicsLayoutTokens.add(lt);
        return this;
    }

    public void addMedicsTokens(List<LayoutToken> layoutTokens) {this.medicsLayoutTokens.addAll(layoutTokens);}

    public void addMedic(Medic medic) {
        if (listMedics == null)
            listMedics = new ArrayList<Medic>();
        if (!listMedics.contains(medic))
            listMedics.add(medic);
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

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.ORGANIZATION, labeledResult, tokenizations);
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

    /**
     * TEI serialization via xom.
     */
    public void addLayoutTokens(List<LayoutToken> theTokens) {
        if (layoutTokens == null) {
            layoutTokens = new ArrayList<LayoutToken>();
        }
        layoutTokens.addAll(theTokens);
    }
}
