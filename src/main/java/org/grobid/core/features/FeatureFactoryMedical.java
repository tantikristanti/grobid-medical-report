package org.grobid.core.features;

import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.lexicon.MedicalNERLexicon;
import org.grobid.core.utilities.OffsetPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * TO DO : This should be a simple extension class of Grobid FeatureFactory for managing and creating features or string sequence tagging problems.
 * However, the private access of the instance make it become impossible
 *
 * Tanti, 2022
 *
 */
public class FeatureFactoryMedical {

    private static FeatureFactoryMedical instance;

    public MedicalNERLexicon medicalNERLexicon = MedicalNERLexicon.getInstance();

    public static FeatureFactoryMedical getInstance() {
        if (instance == null) {
            synchronized (FeatureFactoryMedical.class) {
                if (instance == null) {
                    instance = new FeatureFactoryMedical();
                }
            }
        }
        return instance;
    }

    /**
     * Test if the current string is an anatomy name
     */
    public boolean test_anatomies(String tok) {
        return (medicalNERLexicon.inAnatomies(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a drug or chemical name
     */
    public boolean test_drugs_chemicals(String tok) {
        return (medicalNERLexicon.inChemicalDrugs(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a device name
     */
    public boolean test_devices(String tok) {
        return (medicalNERLexicon.inDevices(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a disorder name
     */
    public boolean test_disorders(String tok) {
        return (medicalNERLexicon.inDisorders(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a location name
     */
    public boolean test_geography(String tok) {
        return (medicalNERLexicon.inGeographicAreas(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a living being
     */
    public boolean test_living_beings(String tok) {
        return (medicalNERLexicon.inLivingBeings(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a hospital object
     */
    public boolean test_objects(String tok) {
        return (medicalNERLexicon.inObjects(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a phenomena
     */
    public boolean test_phenomena(String tok) {
        return (medicalNERLexicon.inPhenomena(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a physiology name
     */
    public boolean test_physiology(String tok) {
        return (medicalNERLexicon.inPhysiology(tok.toLowerCase()));
    }

    /**
     * Test if the current string is a procedure name
     */
    public boolean test_procedures(String tok) {
        return (medicalNERLexicon.inProcedures(tok.toLowerCase()));
    }
}