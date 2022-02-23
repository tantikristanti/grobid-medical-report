package org.grobid.core.lexicon;

import org.apache.commons.io.IOUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 * A class for managing the biomedical lexical resources for NER.
 *
 * Tanti, 2021
 */

public class MedicalNERLexicon{
    private static Logger LOGGER = LoggerFactory.getLogger(MedicalNERLexicon.class);

    private Set<String> anatomies = null;
    private Set<String> chemicalDrugs = null;
    private Set<String> devices = null;
    private Set<String> disorders = null;
    private Set<String> geographicAreas = null;
    private Set<String> livingBeings = null;
    private Set<String> objects = null;
    private Set<String> phenomena = null;
    private Set<String> physiology = null;
    private Set<String> procedures = null;

    private FastMatcher anatomyPattern = null;
    private FastMatcher chemicalDrugPattern = null;
    private FastMatcher devicePattern = null;
    private FastMatcher disorderPattern = null;
    private FastMatcher geographicAreaPattern = null;
    private FastMatcher livingBeingPattern = null;
    private FastMatcher objectPattern = null;
    private FastMatcher phenomenaPattern = null;
    private FastMatcher physiologyPattern = null;
    private FastMatcher procedurePattern = null;

    private static volatile MedicalNERLexicon instance;

    public static MedicalNERLexicon getInstance() {
        if (instance == null) {
            synchronized (Lexicon.class) {
                if (instance == null) {
                    getNewInstance();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of Medical NER Lexicon");
        GrobidProperties.getInstance();
        instance = new MedicalNERLexicon();
    }

    /**
     * Constructors
     */
    private MedicalNERLexicon() {
        initAnatomies();
        addAnatomies("resources/lexicon/ANAT.txt");

        initChemicalDrugs();
        addChemicalDrugs("resources/lexicon/CHEM.txt");

        initDevices();
        addDevices("resources/lexicon/DEVI.txt");

        initDisorders();
        addDisorders("resources/lexicon/DISO.txt");

        initGeographiAreas();
        addGeographiAreas("resources/lexicon/GEOG.txt");

        initLivingBeings();
        addLivingBeings("resources/lexicon/LIVB.txt");

        initObjects();
        addObjects("resources/lexicon/OBJC.txt");

        initPhenomena();
        addPhenomena("resources/lexicon/PHEN.txt");

        initPhysiology();
        addPhysiology("resources/lexicon/PHYS.txt");

        initProcedures();
        addProcedures("resources/lexicon/PROC.txt");
    }

    public void initAnatomies() {
        try {
            anatomyPattern = new FastMatcher(new File("resources/lexicon/ANAT.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for anatomy names.", e);
        }
    }

    public final void addAnatomies(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add anatomies to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add anatomies to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            anatomies = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!anatomies.contains(word)) {
                    anatomies.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initChemicalDrugs() {
        try {
            chemicalDrugPattern = new FastMatcher(new File("resources/lexicon/CHEM.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for chemical and drug names.", e);
        }
    }

    public final void addChemicalDrugs(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add chemical and drugs to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add chemical and drugs to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            chemicalDrugs = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!chemicalDrugs.contains(word)) {
                    chemicalDrugs.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initDevices() {
        try {
            devicePattern = new FastMatcher(new File("resources/lexicon/DEVI.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for device names.", e);
        }
    }

    public final void addDevices(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add devices to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add devices to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            devices = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!devices.contains(word)) {
                    devices.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initDisorders() {
        try {
            disorderPattern = new FastMatcher(new File("resources/lexicon/DISO.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for disorder names.", e);
        }
    }

    public final void addDisorders(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add disorders to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add disorders to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            disorders = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!disorders.contains(word)) {
                    disorders.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initGeographiAreas() {
        try {
            geographicAreaPattern = new FastMatcher(new File("resources/lexicon/GEOG.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for geograhic areas.", e);
        }
    }

    public final void addGeographiAreas(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add geographic areas to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add geographic areas to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            geographicAreas = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!geographicAreas.contains(word)) {
                    geographicAreas.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initLivingBeings() {
        try {
            livingBeingPattern = new FastMatcher(new File("resources/lexicon/LIVB.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for living beings.", e);
        }
    }

    public final void addLivingBeings(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add living beings to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add first living beings to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            livingBeings = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!livingBeings.contains(word)) {
                    livingBeings.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initObjects() {
        try {
            objectPattern = new FastMatcher(new File("resources/lexicon/OBJC.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for object names.", e);
        }
    }

    public final void addObjects(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add objects to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add objects to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            objects = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!objects.contains(word)) {
                    objects.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initPhenomena() {
        try {
            phenomenaPattern = new FastMatcher(new File("resources/lexicon/PHEN.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for phenomena.", e);
        }
    }

    public final void addPhenomena(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add phenomena to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add phenomena to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            phenomena = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!phenomena.contains(word)) {
                    phenomena.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initPhysiology() {
        try {
            physiologyPattern = new FastMatcher(new File("resources/lexicon/PHYS.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for physiology names.", e);
        }
    }

    public final void addPhysiology(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add physiology to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add physiology to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            physiology = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!physiology.contains(word)) {
                    physiology.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    public void initProcedures() {
        try {
            procedurePattern = new FastMatcher(new File("resources/lexicon/PROC.txt"));
        } catch (PatternSyntaxException e) {
            throw new GrobidResourceException("Error when compiling lexicon matcher for procedures.", e);
        }
    }

    public final void addProcedures(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot add procedures to dictionary, because file '" +
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot add procedures to dictionary, because cannot read file '" +
                file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        BufferedReader dis = null;
        try {
            ist = new FileInputStream(file);
            dis = new BufferedReader(new InputStreamReader(ist, "UTF8"));

            String word = null;
            procedures = new HashSet<>();
            while ((word = dis.readLine()) != null) {
                // read the line
                // the first token, separated by a tabulation, gives the word form
                word = word.toLowerCase();
                if (!procedures.contains(word)) {
                    procedures.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

    /**
     * Look-up in anatomy gazetteer
     */
    public boolean inAnatomies(String s) {
        return anatomies.contains(s);
    }

    /**
     * Look-up in chemical and drug gazetteer
     */
    public boolean inChemicalDrugs(String s) {
        return chemicalDrugs.contains(s);
    }

    /**
     * Look-up in devices gazetteer
     */
    public boolean inDevices(String s) {
        return devices.contains(s);
    }

    /**
     * Look-up in disorder gazetteer
     */
    public boolean inDisorders(String s) {
        return disorders.contains(s);
    }

    /**
     * Look-up in geographic areas gazetteer
     */
    public boolean inGeographicAreas(String s) {
        return geographicAreas.contains(s);
    }

    /**
     * Look-up in living being gazetteer
     */
    public boolean inLivingBeings(String s) {
        return livingBeings.contains(s);
    }

    /**
     * Look-up in objects gazetteer
     */
    public boolean inObjects(String s) {
        return objects.contains(s);
    }

    /**
     * Look-up in phenomena gazetteer
     */
    public boolean inPhenomena(String s) {
        return phenomena.contains(s);
    }

    /**
     * Look-up in physiology gazetteer
     */
    public boolean inPhysiology(String s) {
        return physiology.contains(s);
    }

    /**
     * Look-up in procedures gazetteer
     */
    public boolean inProcedures(String s) {
        return procedures.contains(s);
    }

    /**
     * Soft look-up in anatomy names gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsAnatomyNames(String s) {
        if (anatomyPattern == null) {
            initAnatomies();
        }
        List<OffsetPosition> results = anatomyPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsAnatomyNames(List<LayoutToken> s) {
        if (this.anatomyPattern == null) {
            this.initAnatomies();
        }

        List<OffsetPosition> results = this.anatomyPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in chemical and drug names gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsChemicalDrugsNames(String s) {
        if (chemicalDrugPattern == null) {
            initChemicalDrugs();
        }
        List<OffsetPosition> results = chemicalDrugPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsChemicalDrugsNames(List<LayoutToken> s) {
        if (this.chemicalDrugPattern == null) {
            this.initChemicalDrugs();
        }

        List<OffsetPosition> results = this.chemicalDrugPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in devices gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsDevicesNames(String s) {
        if (devicePattern == null) {
            initDevices();
        }
        List<OffsetPosition> results = devicePattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsDevicesNames(List<LayoutToken> s) {
        if (this.devicePattern == null) {
            this.initDevices();
        }

        List<OffsetPosition> results = this.devicePattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in disorder names gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsDisordersNames(String s) {
        if (disorderPattern == null) {
            initDisorders();
        }
        List<OffsetPosition> results = disorderPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsDisordersNames(List<LayoutToken> s) {
        if (this.disorderPattern == null) {
            this.initDisorders();
        }

        List<OffsetPosition> results = this.disorderPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in geographic areas gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsGeographicNames(String s) {
        if (geographicAreaPattern == null) {
            initGeographiAreas();
        }
        List<OffsetPosition> results = geographicAreaPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsGeographicNames(List<LayoutToken> s) {
        if (this.geographicAreaPattern == null) {
            this.initGeographiAreas();
        }

        List<OffsetPosition> results = this.geographicAreaPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in living beings gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsLivingBeings(String s) {
        if (livingBeingPattern == null) {
            initLivingBeings();
        }
        List<OffsetPosition> results = livingBeingPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsLivingBeings(List<LayoutToken> s) {
        if (this.livingBeingPattern == null) {
            this.initLivingBeings();
        }

        List<OffsetPosition> results = this.livingBeingPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in object names gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsObjectNames(String s) {
        if (objectPattern == null) {
            initObjects();
        }
        List<OffsetPosition> results = objectPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsObjectNames(List<LayoutToken> s) {
        if (this.objectPattern == null) {
            this.initObjects();
        }

        List<OffsetPosition> results = this.objectPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in anatomy name gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsPhenomena(String s) {
        if (phenomenaPattern == null) {
            initPhenomena();
        }
        List<OffsetPosition> results = phenomenaPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsPhenomena(List<LayoutToken> s) {
        if (this.phenomenaPattern == null) {
            this.initPhenomena();
        }

        List<OffsetPosition> results = this.phenomenaPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in physiology names gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsPhysiology(String s) {
        if (physiologyPattern == null) {
            initPhysiology();
        }
        List<OffsetPosition> results = physiologyPattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsPhysiology(List<LayoutToken> s) {
        if (this.physiologyPattern == null) {
            this.initPhysiology();
        }

        List<OffsetPosition> results = this.physiologyPattern.matchLayoutToken(s);
        return results;
    }

    /**
     * Soft look-up in proedures gazetteer with token positions
     */
    public List<OffsetPosition> tokenPositionsProcedures(String s) {
        if (procedurePattern == null) {
            initProcedures();
        }
        List<OffsetPosition> results = procedurePattern.matchCharacter(s);
        return results;
    }

    public List<OffsetPosition> tokenPositionsProcedures(List<LayoutToken> s) {
        if (this.procedurePattern == null) {
            this.initProcedures();
        }

        List<OffsetPosition> results = this.procedurePattern.matchLayoutToken(s);
        return results;
    }
}
