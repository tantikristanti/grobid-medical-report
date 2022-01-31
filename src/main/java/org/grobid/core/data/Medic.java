package org.grobid.core.data;

import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for representing and exchanging medics information.
 *
 */

public class Medic {
    private String idno = null;
    private String roleName = null;
    private String persName = null;
    private String affiliation = null;
    private String orgName = null;
    private String institution = null;
    private String address = null;
    private String country = null;
    private String town = null;
    private String email = null;
    private String fax = null;
    private String phone = null;
    private String note = null;
    private String web = null;

    private List<LayoutToken> layoutTokens = new ArrayList<>();

    public String getRole() {
        return roleName;
    }

    public void setRole(String role) {
        this.roleName = role;
    }

    public String getPersName() {
        return persName;
    }

    public void setPersName(String persName) {
        this.persName = persName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getOrganisation() {
        return orgName;
    }

    public void setOrganisation(String organisation) {
        this.orgName = organisation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNote() {
        return note;
    }

    public String getIdno() {
        return idno;
    }

    public void setIdno(String idno) {
        this.idno = idno;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public boolean isNotNull() {
        return (roleName != null) ||
            (persName != null) ||
            (affiliation != null) ||
            (orgName != null) ||
            (institution != null) ||
            (address != null) ||
            (country != null) ||
            (town != null) ||
            (email != null) ||
            (fax != null) ||
            (phone != null) ||
            (web != null) ||
            (note != null);
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> tokens) {
        this.layoutTokens = tokens;
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
