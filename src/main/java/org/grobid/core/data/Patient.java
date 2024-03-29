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
    private String IDType = null;
    private String dateBirth = null;
    private String age = null;
    private String placeBirth = null;
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

    public String getIDType() {
        return IDType;
    }

    public void setIDType(String IDType) {
        this.IDType = IDType;
    }

    public String getDateBirth() {
        return dateBirth;
    }

    public void setDateBirth(String dateBirth) {
        this.dateBirth = dateBirth;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getPlaceBirth() {
        return placeBirth;
    }

    public void setPlaceBirth(String placeBirth) {
        this.placeBirth = placeBirth;
    }

    public String getDateDeath() {
        return dateDeath;
    }

    public void setDateDeath(String dateDeath) {
        this.dateDeath = dateDeath;
    }

    public String getPersName() {
        return persName;
    }

    public void setPersName(String persName) {
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

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }


    public boolean isNotNull() {
        return (ID != null) ||
            (IDType != null) ||
            (persName != null) ||
            (dateBirth != null) ||
            (placeBirth != null) ||
            (age != null) ||
            (dateDeath != null) ||
            (sex != null) ||
            (address != null) ||
            (country != null) ||
            (town != null) ||
            (email != null) ||
            (phone != null) ||
            (note != null);
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

    public String toTEI() {
        String thePatient = "";
        if (ID != null) {
            thePatient += "\t<idno>" + ID + "</idno>";
        }
        if (IDType != null) {
            thePatient += "\t<idType>" + IDType + "</idType>";
        }
        if (persName != null) {
            thePatient += "\t<persName>" + persName + "</persName>";
        }
        if (sex != null) {
            thePatient += "\t<sex>" + sex + "</sex>";
        }
        if (dateBirth != null) {
            thePatient += "\t<birthDate>" + dateBirth + "</birthDate>";
        }
        if (placeBirth != null) {
            thePatient += "\t<birthPlace>" + placeBirth + "</birthPlace>";
        }
        if (age != null) {
            thePatient += "\t<age>" + age + "</age>";
        }
        if (dateDeath != null) {
            thePatient += "\t<death>" + dateDeath + "</death>";
        }
        if (address != null) {
            thePatient += "\t<address>" + address + "</address>";
        }
        if (country != null) {
            thePatient += "\t<country>" + country + "</country>";
        }
        if (town != null) {
            thePatient += "\t<settlement>" + town + "</settlement>";
        }
        if (phone != null) {
            thePatient += "\t<phone>" + phone + "</phone>";
        }
        if (email != null) {
            thePatient += "\t<email>" + email + "</email>";
        }
        if (note != null) {
            thePatient += "\t<note type=\"patient\">" + note + "</note>";
        }

        return thePatient;
    }

}
