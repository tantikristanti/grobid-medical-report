package org.grobid.core.data;

/**
 * A class for representing and exchanging medics information.
 *
 */

public class Medic {
    private String role = null;
    private String persName = null;
    private String affiliation = null;
    private String organisation = null;
    private String institution = null;
    private String address = null;
    private String country = null;
    private String town = null;
    private String email = null;
    private String fax = null;
    private String phone = null;
    private String note = null;
    private String web = null;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
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
        return (role != null) ||
            (persName != null) ||
            (affiliation != null) ||
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
}
