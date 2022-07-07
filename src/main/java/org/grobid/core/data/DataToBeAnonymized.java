package org.grobid.core.data;

public class DataToBeAnonymized {
    // we keep the original information
    // the document number found in the header part
    private String idnoOriginal = null;
    // patient information
    private String securitySocialNumberOriginal = null;
    private String firstNamePatientOriginal = null;
    private String middleNamePatientOriginal = null;
    private String lastNamePatientOriginal = null;
    private String phonePatientOriginal = null;
    private String dateBirthPatientOriginal = null;
    private String addressPatientOriginal = null;
    private String emailPatientOriginal = null;
    // medic information
    private String firstNameMedicOriginal = null;
    private String middleNameMedicOriginal = null;
    private String lastNameMedicOriginal = null;
    private String addressMedicOriginal = null;

    // the replacements
    // the document number found in the header part
    private String idnoAnonym = null;

    // patient information
    private String securitySocialNumberAnonym = null;
    private String firstNamePatientAnonym = null;
    private String middleNamePatientAnonym = null;
    private String lastNamePatientAnonym = null;
    private String phonePatientAnonym = null;
    private String dateBirthPatientAnonym = null;
    private String addressPatientAnonym = null;
    private String emailPatientAnonym = null;
    // medic information
    private String firstNameMedicAnonym = null;
    private String middleNameMedicAnonym = null;
    private String lastNameMedicAnonym = null;
    private String addressMedicAnonym = null;


    public String getIdnoOriginal() {
        return idnoOriginal;
    }

    public void setIdnoOriginal(String idnoOriginal) {
        this.idnoOriginal = idnoOriginal;
    }


    public String getSecuritySocialNumberOriginal() {
        return securitySocialNumberOriginal;
    }

    public void setSecuritySocialNumberOriginal(String securitySocialNumberOriginal) {
        this.securitySocialNumberOriginal = securitySocialNumberOriginal;
    }

    public String getSecuritySocialNumberAnonym() {
        return securitySocialNumberAnonym;
    }

    public void setSecuritySocialNumberAnonym(String securitySocialNumberAnonym) {
        this.securitySocialNumberAnonym = securitySocialNumberAnonym;
    }

    public String getFirstNamePatientOriginal() {
        return firstNamePatientOriginal;
    }

    public void setFirstNamePatientOriginal(String firstNamePatientOriginal) {
        this.firstNamePatientOriginal = firstNamePatientOriginal;
    }

    public String getMiddleNamePatientOriginal() {
        return middleNamePatientOriginal;
    }

    public void setMiddleNamePatientOriginal(String middleNamePatientOriginal) {
        this.middleNamePatientOriginal = middleNamePatientOriginal;
    }

    public String getLastNamePatientOriginal() {
        return lastNamePatientOriginal;
    }

    public void setLastNamePatientOriginal(String lastNamePatientOriginal) {
        this.lastNamePatientOriginal = lastNamePatientOriginal;
    }


    public String getPhonePatientOriginal() {
        return phonePatientOriginal;
    }

    public void setPhonePatientOriginal(String phonePatientOriginal) {
        this.phonePatientOriginal = phonePatientOriginal;
    }

    public String getEmailPatientOriginal() {
        return emailPatientOriginal;
    }

    public void setEmailPatientOriginal(String emailPatientOriginal) {
        this.emailPatientOriginal = emailPatientOriginal;
    }

    public String getPhonePatientAnonym() {
        return phonePatientAnonym;
    }

    public void setPhonePatientAnonym(String phonePatientAnonym) {
        this.phonePatientAnonym = phonePatientAnonym;
    }

    public String getEmailPatientAnonym() {
        return emailPatientAnonym;
    }

    public void setEmailPatientAnonym(String emailPatientAnonym) {
        this.emailPatientAnonym = emailPatientAnonym;
    }

    public String getDateBirthPatientOriginal() {
        return dateBirthPatientOriginal;
    }

    public void setDateBirthPatientOriginal(String dateBirthPatientOriginal) {
        this.dateBirthPatientOriginal = dateBirthPatientOriginal;
    }

    public String getAddressPatientOriginal() {
        return addressPatientOriginal;
    }

    public void setAddressPatientOriginal(String addressPatientOriginal) {
        this.addressPatientOriginal = addressPatientOriginal;
    }

    public String getFirstNameMedicOriginal() {
        return firstNameMedicOriginal;
    }

    public void setFirstNameMedicOriginal(String firstNameMedicOriginal) {
        this.firstNameMedicOriginal = firstNameMedicOriginal;
    }

    public String getMiddleNameMedicOriginal() {
        return middleNameMedicOriginal;
    }

    public void setMiddleNameMedicOriginal(String middleNameMedicOriginal) {
        this.middleNameMedicOriginal = middleNameMedicOriginal;
    }

    public String getLastNameMedicOriginal() {
        return lastNameMedicOriginal;
    }

    public void setLastNameMedicOriginal(String lastNameMedicOriginal) {
        this.lastNameMedicOriginal = lastNameMedicOriginal;
    }

    public String getAddressMedicOriginal() {
        return addressMedicOriginal;
    }

    public void setAddressMedicOriginal(String addressMedicOriginal) {
        this.addressMedicOriginal = addressMedicOriginal;
    }

    public String getIdnoAnonym() {
        return idnoAnonym;
    }

    public void setIdnoAnonym(String idnoAnonym) {
        this.idnoAnonym = idnoAnonym;
    }

    public String getFirstNamePatientAnonym() {
        return firstNamePatientAnonym;
    }

    public void setFirstNamePatientAnonym(String firstNamePatientAnonym) {
        this.firstNamePatientAnonym = firstNamePatientAnonym;
    }

    public String getMiddleNamePatientAnonym() {
        return middleNamePatientAnonym;
    }

    public void setMiddleNamePatientAnonym(String middleNamePatientAnonym) {
        this.middleNamePatientAnonym = middleNamePatientAnonym;
    }

    public String getLastNamePatientAnonym() {
        return lastNamePatientAnonym;
    }

    public void setLastNamePatientAnonym(String lastNamePatientAnonym) {
        this.lastNamePatientAnonym = lastNamePatientAnonym;
    }

    public String getDateBirthPatientAnonym() {
        return dateBirthPatientAnonym;
    }

    public void setDateBirthPatientAnonym(String dateBirthPatientAnonym) {
        this.dateBirthPatientAnonym = dateBirthPatientAnonym;
    }

    public String getAddressPatientAnonym() {
        return addressPatientAnonym;
    }

    public void setAddressPatientAnonym(String addressPatientAnonym) {
        this.addressPatientAnonym = addressPatientAnonym;
    }

    public String getFirstNameMedicAnonym() {
        return firstNameMedicAnonym;
    }

    public void setFirstNameMedicAnonym(String firstNameMedicAnonym) {
        this.firstNameMedicAnonym = firstNameMedicAnonym;
    }

    public String getMiddleNameMedicAnonym() {
        return middleNameMedicAnonym;
    }

    public void setMiddleNameMedicAnonym(String middleNameMedicAnonym) {
        this.middleNameMedicAnonym = middleNameMedicAnonym;
    }

    public String getLastNameMedicAnonym() {
        return lastNameMedicAnonym;
    }

    public void setLastNameMedicAnonym(String lastNameMedicAnonym) {
        this.lastNameMedicAnonym = lastNameMedicAnonym;
    }

    public String getAddressMedicAnonym() {
        return addressMedicAnonym;
    }

    public void setAddressMedicAnonym(String addressMedicAnonym) {
        this.addressMedicAnonym = addressMedicAnonym;
    }
}
