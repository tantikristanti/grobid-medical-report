package org.grobid.core.data;

import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for representing and exchanging patient information.
 *
 */

public class Patient {
    private String ID = null;
    private String dateBirth = null;
    private String dateDeath = null;
    private String persName = null;
    private String sex = null;
    private String address = null;
    private String country = null;
    private String town = null;
    private String email = null;
    private String phone = null;
    private String note = null;

    private List<LayoutToken> layoutTokens = new ArrayList<>();

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getDateDeath() {
        return dateDeath;
    }

    public void setDateDeath(String dateDeath) {
        this.dateDeath = dateDeath;
    }

    public void setPersName(String persName) {
        this.persName = persName;
    }

    public String getDateBirth() {
        return dateBirth;
    }

    public void setDateBirth(String dateBirth) {
        this.dateBirth = dateBirth;
    }

    public String getPersName() {
        return persName;
    }

    public void setFullName(String persName) {
        this.persName = persName;
    }


    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
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

    public boolean isNotNull() {
        return (ID != null) ||
            (dateBirth != null) ||
            (dateDeath != null) ||
            (persName != null) ||
            (sex != null) ||
            (address != null) ||
            (country != null) ||
            (town != null) ||
            (email != null) ||
            (phone != null) ||
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
