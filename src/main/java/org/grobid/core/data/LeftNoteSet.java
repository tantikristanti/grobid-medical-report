package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing medic lists for left-note.
 *
 * Tanti, 2020
 */
public class LeftNoteSet {
    private List<String> institutions = null;
    private List<String> medics = null;
    private List<String> affiliations = null;
    private List<String> locations = null;

    public List<String> getMedics() {
        return medics;
    }

    public List<String> getLocations() {
        return locations;
    }

    public List<String> getAffiliations() { return affiliations; }

    public void addInstitutions(String institution) {
        if (institutions == null)
            institutions = new ArrayList<String>();
        if (institution != null) {
            if (institution.length() > 0) {
                if (institutions.indexOf(institution) == -1)
                    institutions.add(institution);
            }
        }
    }

    public void addMedics(String medic) {
        if (medics == null)
            medics = new ArrayList<String>();
        if (medic != null) {
            if (medic.length() > 0) {
                if (medics.indexOf(medic) == -1)
                    medics.add(medic);
            }
        }
    }

    public void addLocations(String location) {
        if (locations == null)
            locations = new ArrayList<String>();
        if (location != null) {
            if (location.length() > 0) {
                if (institutions.indexOf(location) == -1)
                    institutions.add(location);
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

        // institutions
        if (institutions != null) {
            tei += "<listOrg type=\"institution\">\n";

            int i = 0;
            for (String institution : institutions) {
                tei += "\t<org xml:id=\"institution" + i + "\">\n";
                tei += "\t\t<orgName>";

                tei += institution;

                tei += "</orgName>\n";
                tei += "\t</org>\n";
                i++;
            }

            tei += "</listOrg>\n\n";
        }

        // affiliations
        if (affiliations != null) {
            tei += "<listOrg type=\"affiliation\">\n";

            int i = 0;
            for (String affiliation : affiliations) {
                tei += "\t<org xml:id=\"affiliation" + i + "\">\n";
                tei += "\t\t<orgName>";

                tei += affiliation;

                tei += "</orgName>\n";
                tei += "\t</org>\n";
                i++;
            }

            tei += "</listOrg>\n\n";
        }

        return tei;
    }

}