package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing authority lists for headers.
 *
 * @author Patrice Lopez
 */
public class HeaderSet {
    private List<String> medics = null;
    private List<String> patients = null;
    private List<String> meetings = null;
    private List<String> publishers = null;
    private List<String> owners = null;
    private List<String> locations = null;

    private List<String> journals = null;
    private List<String> institutions = null;
    private List<String> affiliations = null; // ??
    private List<String> keywords = null;

    public HeaderSet() {
    }

    public List<String> getMedics() {
        return medics;
    }

    public List<String> getPatients() {
        return patients;
    }

    public List<String> getMeetings() {
        return meetings;
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void addMedic(String med) {
        if (medics == null)
            medics = new ArrayList<String>();
        if (med != null) {
            if (med.length() > 0) {
                if (medics.indexOf(med) == -1)
                    medics.add(med);
            }
        }
    }

    public void addOwners(String own) {
        if (owners == null)
            owners = new ArrayList<String>();
        if (own != null) {
            if (own.length() > 0) {
                if (owners.indexOf(own) == -1)
                    owners.add(own);
            }
        }
    }

    public void addPatients(String pat) {
        if (patients == null)
            patients = new ArrayList<String>();
        if (pat != null) {
            if (pat.length() > 0) {
                if (patients.indexOf(pat) == -1)
                    patients.add(pat);
            }
        }
    }

    public void addMeeting(String aut) {
        if (meetings == null)
            meetings = new ArrayList<String>();
        if (aut != null) {
            if (aut.length() > 0) {
                if (meetings.indexOf(aut) == -1)
                    meetings.add(aut);
            }
        }
    }

    /**
     * Export the bibliographical lists into TEI structures
     */
    public String toTEI() {
        String tei = "";

        // we just produce here xml strings, DOM XML objects should be used for JDK 1.4, J2E compliance thingy
        // medics
        if (medics != null) {
            tei += "<listPerson type=\"medic\">\n";

            int i = 0;
            for (String med : medics) {
                tei += "\t<person xml:id=\"medic" + i + "\">\n";
                tei += "\t\t<persName>";

                int ind = med.lastIndexOf(" ");
                if (ind != -1) {
                    tei += "\n\t\t\t<forename>" + med.substring(0, ind) + "</forename>\n";
                    tei += "\t\t\t<surname>" + med.substring(ind + 1) + "</surname>\n\t\t";
                } else
                    tei += med;

                tei += "</persName>\n";
                tei += "\t</person>\n";
                i++;
            }

            tei += "</listPerson>\n\n";
        }

        // patients
        if (patients != null) {
            tei += "<listPerson type=\"editor\">\n";

            int i = 0;
            for (String pat : patients) {
                tei += "\t<person xml:id=\"editor" + i + "\">\n";
                tei += "\t\t<persName>";

                int ind = pat.lastIndexOf(" ");
                if (ind != -1) {
                    tei += "\n\t\t\t<forename>" + pat.substring(0, ind) + "</forename>\n";
                    tei += "\t\t\t<surname>" + pat.substring(ind + 1) + "</surname>\n\t\t";
                } else
                    tei += pat;

                tei += "</persName>\n";
                tei += "\t</person>\n";
                i++;
            }

            tei += "</listPerson>\n\n";
        }

        // publishers
        if (publishers != null) {
            tei += "<listOrg type=\"publisher\">\n";

            int i = 0;
            for (String aut : publishers) {
                tei += "\t<org xml:id=\"publisher" + i + "\">\n";
                tei += "\t\t<orgName>";

                tei += aut;

                tei += "</orgName>\n";
                tei += "\t</org>\n";
                i++;
            }

            tei += "</listOrg>\n\n";
        }

        // meetings
        if (meetings != null) {
            tei += "<list type=\"meeting\">\n";

            int i = 0;
            for (String aut : meetings) {
                tei += "\t<meeting xml:id=\"meeting" + i + "\">";
                tei += aut;
                tei += "</meeting>\n";
                i++;
            }

            tei += "</list>\n\n";
        }

        return tei;
    }

}