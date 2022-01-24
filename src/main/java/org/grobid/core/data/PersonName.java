package org.grobid.core.data;

import org.apache.commons.lang3.StringUtils;

import nu.xom.Attribute;
import nu.xom.Element;

import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class for representing and exchanging person information, e.g. medics or patients.
 *
 *
 * Tanti, 2022
 *
 */
public class PersonName {
    private String rawName = null; // raw full name if relevant/available, e.g. name exactly as displayed
    private String title = null;
    private String firstName = null;
    private String middleName = null;
    private String lastName = null;
    private String suffix = null;

    private List<LayoutToken> layoutTokens = new ArrayList<>();

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String name) {
        rawName = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String tit) {
        if (tit != null) {
            while (tit.startsWith("(")) {
                tit = tit.substring(1,tit.length());
            }

            while (tit.endsWith(")")) {
                tit = tit.substring(0,tit.length()-1);
            }
        }

        title = tit;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String f) {
        firstName = f;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String f) {
        middleName = f;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String f) {
        lastName = f;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suf) {
        if (suf != null) {
            while (suf.startsWith("(")) {
                suf = suf.substring(1,suf.length());
            }

            while (suf.endsWith(")")) {
                suf = suf.substring(0,suf.length()-1);
            }
        }

        title = suf;
    }

    public boolean notNull() {
        if ((firstName == null) &&
            (middleName == null) &&
            (lastName == null) &&
            (title == null) &&
             (suffix == null)
        )
            return false;
        else
            return true;
    }

    /**
     * Create a new instance of Person object from current instance (shallow copy)
     */
    public PersonName clonePerson() {
        PersonName person = new PersonName();
        person.title = this.title;
        person.rawName = this.rawName;
        person.firstName = this.firstName ;
        person.middleName = this.middleName;
        person.lastName = this.lastName;
        person.suffix = this.suffix;

        return person;
    }

    public String toString() {
        String res = "";
        if (title != null)
            res += title + " ";
        if (firstName != null)
            res += firstName + " ";
        if (middleName != null)
            res += middleName + " ";
        if (lastName != null)
            res += lastName + " ";
        if (suffix != null)
            res += suffix;
        return res.trim();
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

    public String toTEI(boolean withCoordinates) {
        if ( (firstName == null) && (middleName == null) &&
            (lastName == null) ) {
            return null;
        }

        Element persElement = XmlBuilderUtils.teiElement("persName");

        if (withCoordinates && (getLayoutTokens() != null) && (!getLayoutTokens().isEmpty())) {
            XmlBuilderUtils.addCoords(persElement, LayoutTokensUtil.getCoordsString(getLayoutTokens()));
        }
        if (title != null) {
            persElement.appendChild(XmlBuilderUtils.teiElement("roleName", title));
        }
        if (firstName != null) {
            Element forename = XmlBuilderUtils.teiElement("forename", firstName);
            forename.addAttribute(new Attribute("type", "first"));
            persElement.appendChild(forename);
        }
        if (middleName != null) {
            Element mn = XmlBuilderUtils.teiElement("forename", middleName);
            mn.addAttribute(new Attribute("type", "middle"));
            persElement.appendChild(mn);
        }
        if (lastName != null) {
            persElement.appendChild(XmlBuilderUtils.teiElement("surname", lastName));
        }
        if (suffix != null) {
            persElement.appendChild(XmlBuilderUtils.teiElement("genName", suffix));
        }

        return XmlBuilderUtils.toXml(persElement);
    }

    /**
     * TEI serialization based on string builder, it allows to avoid namespaces and to better control
     * the formatting.
     */
    public String toTEI(boolean withCoordinates, int indent) {
        if ( (firstName == null) && (middleName == null) && (lastName == null) ) {
            return null;
        }

        StringBuilder tei = new StringBuilder();

        for (int i = 0; i < indent; i++) {
            tei.append("\t");
        }
        tei.append("<persName");
        if (withCoordinates && (getLayoutTokens() != null) && (!getLayoutTokens().isEmpty())) {
            tei.append(" ");
            tei.append(LayoutTokensUtil.getCoordsString(getLayoutTokens()));
        }
        tei.append(">\n");

        if (!StringUtils.isEmpty(title)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<roleName>"+TextUtilities.HTMLEncode(title)+"</roleName>\n");
        }

        if (!StringUtils.isEmpty(firstName)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<forename type=\"first\">"+TextUtilities.HTMLEncode(firstName)+"</forename>\n");
        }

        if (!StringUtils.isEmpty(middleName)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<forename type=\"middle\">"+TextUtilities.HTMLEncode(middleName)+"</forename>\n");
        }

        if (!StringUtils.isEmpty(lastName)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<surname>"+TextUtilities.HTMLEncode(lastName)+"</surname>\n");
        }

        if (!StringUtils.isEmpty(suffix)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<genName>"+TextUtilities.HTMLEncode(suffix)+"</genName>\n");
        }

        for (int i = 0; i < indent; i++) {
            tei.append("\t");
        }
        tei.append("</persName>");

        return tei.toString();
    }

    // list of character delimiters for capitalising names
    private static final String NAME_DELIMITERS = "-.,;:/_ ";

    /**
     * This normalisation takes care of uniform case for name components and for
     * transforming agglutinated initials (like "JM" in JM Smith)
     * which are put into the firstname into separate initials in first and middle names.
     *
     */
    public void normalizeName() {
        if (StringUtils.isEmpty(middleName) && !StringUtils.isEmpty(firstName) &&
            (firstName.length() == 2) && (TextUtilities.isAllUpperCase(firstName)) ) {
            middleName = firstName.substring(1,2);
            firstName = firstName.substring(0,1);
        }

        firstName = TextUtilities.capitalizeFully(firstName, NAME_DELIMITERS);
        middleName = TextUtilities.capitalizeFully(middleName, NAME_DELIMITERS);
        lastName = TextUtilities.capitalizeFully(lastName, NAME_DELIMITERS);
    }


    /**
     *  Return true if the person structure is a valid person name, in our case
     *  with at least a lastname or a raw name.
     */
    public boolean isValid() {
        if ( (lastName == null) && (rawName == null) )
            return false;
        else
            return true;
    }


    /**
     *  Deduplicate person names, optionally attached to affiliations, based
     *  on common forename/surname, taking into account abbreviated forms
     */
    public static List<PersonName> deduplicate(List<PersonName> persons) {
        if (persons == null)
            return null;
        if (persons.size() == 0)
            return persons;

        // we create a signature per person based on lastname and first name first letter
        Map<String,List<PersonName>> signatures = new TreeMap<String,List<PersonName>>();

        for(PersonName person : persons) {
            if (person.getLastName() == null || person.getLastName().trim().length() == 0) {
                // the minimal information to deduplicate is not available
                continue;
            }
            String signature = person.getLastName().toLowerCase();
            if (person.getFirstName() != null && person.getFirstName().trim().length() != 0) {
                signature += "_" + person.getFirstName().substring(0,1);
            }
            List<PersonName> localPersons = signatures.get(signature);
            if (localPersons == null) {
                localPersons = new ArrayList<PersonName>();
            }
            localPersons.add(person);
            signatures.put(signature, localPersons);
        }

        // match signature and check possible affiliation information
        for (Map.Entry<String,List<PersonName>> entry : signatures.entrySet()) {
            List<PersonName> localPersons = entry.getValue();
            if (localPersons.size() > 1) {
                // candidate for deduplication, check full forenames and middlenames to check if there is a clash
                List<PersonName> newLocalPersons = new ArrayList<PersonName>();
                for(int j=0; j < localPersons.size(); j++) {
                    PersonName localPerson =  localPersons.get(j);
                    String localFirstName = localPerson.getFirstName();
                    if (localFirstName != null) {
                        localFirstName = localFirstName.toLowerCase();
                        localFirstName = localFirstName.replaceAll("[\\-\\.]", "");
                    }
                    String localMiddleName = localPerson.getMiddleName();
                    if (localMiddleName != null) {
                        localMiddleName = localMiddleName.toLowerCase();
                        localMiddleName = localMiddleName.replaceAll("[\\-\\.]", "");
                    }
                    int nbClash = 0;
                    for(int k=0; k < localPersons.size(); k++) {
                        boolean clash = false;
                        if (k == j)
                            continue;
                        PersonName otherPerson = localPersons.get(k);
                        String otherFirstName = otherPerson.getFirstName();
                        if (otherFirstName != null) {
                            otherFirstName = otherFirstName.toLowerCase();
                            otherFirstName = otherFirstName.replaceAll("[\\-\\.]", "");
                        }
                        String otherMiddleName = otherPerson.getMiddleName();
                        if (otherMiddleName != null) {
                            otherMiddleName = otherMiddleName.toLowerCase();
                            otherMiddleName = otherMiddleName.replaceAll("[\\-\\.]", "");
                        }

                        // test first name clash
                        if (localFirstName != null && otherFirstName != null) {
                            if (localFirstName.length() == 1 && otherFirstName.length() == 1) {
                                if (!localFirstName.equals(otherFirstName)) {
                                    clash = true;
                                }
                            } else {
                                if (!localFirstName.equals(otherFirstName) &&
                                    !localFirstName.startsWith(otherFirstName) &&
                                    !otherFirstName.startsWith(localFirstName)
                                ) {
                                    clash = true;
                                }
                            }
                        }

                        // test middle name clash
                        if (!clash) {
                            if (localMiddleName != null && otherMiddleName != null) {
                                if (localMiddleName.length() == 1 && otherMiddleName.length() == 1) {
                                    if (!localMiddleName.equals(otherMiddleName)) {
                                        clash = true;
                                    }
                                } else {
                                    if (!localMiddleName.equals(otherMiddleName) &&
                                        !localMiddleName.startsWith(otherMiddleName) &&
                                        !otherMiddleName.startsWith(localMiddleName)
                                    ) {
                                        clash = true;
                                    }
                                }
                            }
                        }

                        if (clash) {
                            // increase the clash number for index j
                            nbClash++;
                        }
                    }

                    if (nbClash == 0) {
                        newLocalPersons.add(localPerson);
                    }
                }

                localPersons = newLocalPersons;

                if (localPersons.size() > 1) {
                    // if identified duplication, keep the most complete person form and the most complete
                    // affiliation information
                    PersonName localPerson =  localPersons.get(0);
                    String localFirstName = localPerson.getFirstName();
                    if (localFirstName != null)
                        localFirstName = localFirstName.toLowerCase();
                    String localMiddleName = localPerson.getMiddleName();
                    if (localMiddleName != null)
                        localMiddleName = localMiddleName.toLowerCase();
                    String localTitle = localPerson.getTitle();
                    if (localTitle != null)
                        localTitle = localTitle.toLowerCase();
                    String localSuffix = localPerson.getSuffix();
                    if (localSuffix != null)
                        localSuffix = localSuffix.toLowerCase();
                    for (int i=1; i<localPersons.size(); i++) {
                        PersonName otherPerson = localPersons.get(i);
                        // try to enrich first Person object
                        String otherFirstName = otherPerson.getFirstName();
                        if (otherFirstName != null)
                            otherFirstName = otherFirstName.toLowerCase();
                        String otherMiddleName = otherPerson.getMiddleName();
                        if (otherMiddleName != null)
                            otherMiddleName = otherMiddleName.toLowerCase();
                        String otherTitle = otherPerson.getTitle();
                        if (otherTitle != null)
                            otherTitle = otherTitle.toLowerCase();
                        String otherSuffix = otherPerson.getSuffix();
                        if (otherSuffix != null)
                            otherSuffix = otherSuffix.toLowerCase();

                        if ((localFirstName == null && otherFirstName != null) ||
                            (localFirstName != null && otherFirstName != null &&
                                otherFirstName.length() > localFirstName.length())) {
                            localPerson.setFirstName(otherPerson.getFirstName());
                            localFirstName = localPerson.getFirstName().toLowerCase();
                        }

                        if ((localMiddleName == null && otherMiddleName != null) ||
                            (localMiddleName != null && otherMiddleName != null &&
                                otherMiddleName.length() > localMiddleName.length())) {
                            localPerson.setMiddleName(otherPerson.getMiddleName());
                            localMiddleName = localPerson.getMiddleName().toLowerCase();
                        }

                        if ((localTitle == null && otherTitle != null) ||
                            (localTitle != null && otherTitle != null &&
                                otherTitle.length() > localTitle.length())) {
                            localPerson.setTitle(otherPerson.getTitle());
                            localTitle = localPerson.getTitle().toLowerCase();
                        }

                        if ((localSuffix == null && otherSuffix != null) ||
                            (localSuffix != null && otherSuffix != null &&
                                otherSuffix.length() > localSuffix.length())) {
                            localPerson.setSuffix(otherPerson.getSuffix());
                            localSuffix = localPerson.getSuffix().toLowerCase();
                        }

                        if (persons.contains(otherPerson))
                            persons.remove(otherPerson);
                    }
                }
            }
        }

        return persons;
    }

    /**
     *  Remove invalid/impossible person names (no last names, noise, etc.)
     */
    public static List<PersonName> sanityCheck(List<PersonName> persons) {
        if (persons == null)
            return null;
        if (persons.size() == 0)
            return persons;

        List<PersonName> result = new ArrayList<PersonName>();

        for(PersonName person : persons) {
            if (person.getLastName() == null || person.getLastName().trim().length() == 0)
                continue;
            else
                result.add(person);
        }

        return result;
    }

}