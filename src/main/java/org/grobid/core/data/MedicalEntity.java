package org.grobid.core.data;

import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.NERLexicon;
import org.grobid.core.utilities.OffsetPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * Common representation of an unresolved entity mention for the NER components.
 *
 * @author Patrice Lopez
 */
public class MedicalEntity implements Comparable<MedicalEntity> {
    /* Biomedical NER base types are adapted from
     - grobid-ner (https://grobid-ner.readthedocs.io/en/latest/class-and-senses/)
     - The Quaero Corpus (https://quaerofrenchmed.limsi.fr/)
    * */
    public enum Medical_NER_Type {
        OTHER("OTHER"), // Entity not belonging to any classes
        PERSON("PERSON"), // grobid-ner : first, middle, last names and aliases of people and fictional characters
        LOCATION("LOCATION"), // grobid-ner : physical location, including planets and galaxies; The Quaero Corpus - GEOG : Geographic area
        ORGANISATION("ORGANISATION"), // grobid-ner : organized group of people, with some sort of legal entity and concrete membership
        ANIMAL("ANIMAL"), // grobid-ner : individual name of an animal
        MEASURE("MEASURE"), // grobid-ner : numerical amount, including an optional unit of measure
        LEGAL("LEGAL"), // grobid-ner : legal mentions such as article of law, convention, cases, treaty
        IDENTIFIER("IDENTIFIER"), // grobid-ner : systematized identifier such as phone number, fax, email address
        INSTALLATION("INSTALLATION"), // grobid-ner : structure built by humans
        SUBSTANCE("SUBSTANCE"), // Substances other than chemical and drugs
        PLANT("PLANT"), // grobid-ner : name of a plant
        PERIOD("PERIOD"), // grobid-ner : date, historical era or other time period, time expressions
        TITLE("TITLE"), // grobid-ner : personal or honorific title, for a person
        WEBSITE("WEBSITE"), // grobid-ner : website URL or name
        ANATOMY("ANATOMY"), // The Quaero Corpus - ANAT : Anatomical structure, body location or part, organ, body system
        CHEMICAL("CHEMICAL"), // The Quaero Corpus - CHEM : Chemical and drugs
        DEVICE("DEVICE"), // The Quaero Corpus - DEVI : Drug, medical, and research devices
        DISORDER("DISORDER"), // The Quaero Corpus - DISO : Sign, symptom, diseases, syndrom, and abnormality
        LIVING("LIVING"), // The Quaero Corpus - LIVB : Living beings other than human, animals, or plants (ex. Fungus, Bacterium, Virus)
        OBJECT("OBJECT"), // The Quaero Corpus - OBJC : Manufactured, physical objects
        PHENOMENA("PHENOMENA"), // The Quaero Corpus - PHEN : Laboratory or test results, human-caused phenomenon or process
        PHYSIOLOGY("PHYSIOLOGY"), // The Quaero Corpus - PHYS : Organism function, attribute, clinical attribute
        PROCEDURE("PROCEDURE"); // The Quaero Corpus - PROC : Diagnostic or laboratory procedures

        private String name;


        private Medical_NER_Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Orign of the entity definition
    public enum Origin {
        GROBID("grobid"),
        USER("user");

        private String name;

        private Origin(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    // name of the entity = entity type
    private String rawName = null;

    //entity type
    private String rawType;

    // entity-type in string format
    private String stringType = null;

    // normalised name of the entity
    private String normalisedName = null;

    // type of the entity (person, location, etc.)
    private Medical_NER_Type type = null;

    // subtypes of the entity when available - the first one is the main one, the others secondary subtypes
    private List<String> subTypes = null;

    // relative offset positions in context, if defined
    private OffsetPosition offsets = null;

    // probability of the entity in context, if defined
    private double prob = 1.0;

    // confidence score of the entity in context, if defined
    private double conf = 0.8;

    // all the sense information related to the entity
    private Sense sense = null;

    // optional bounding box in the source document
    private List<BoundingBox> boundingBoxes = null;

    //list of layout tokens corresponding to this entity
    private List<LayoutToken> layoutTokens = null;

    // orign of the entity definition
    private Origin origin = Origin.GROBID;

    // if the entity is an acronym; if true, the normalisedName will give the found expended form
    private boolean isAcronym = false;

    public MedicalEntity() {
        this.offsets = new OffsetPosition();
    }

    public MedicalEntity(String raw) {
        this.rawName = raw;
        this.offsets = new OffsetPosition();
    }

    public MedicalEntity(MedicalEntity ent) {
        rawName = ent.rawName;
        rawType = ent.rawType;
        stringType = ent.stringType;
        normalisedName = ent.normalisedName;
        type = ent.type;
        subTypes = ent.subTypes;
        offsets = ent.offsets;
        prob = ent.prob;
        conf = ent.conf;
        sense = ent.sense;
        origin = ent.origin;
        boundingBoxes = ent.boundingBoxes;
        isAcronym = ent.isAcronym;
        //startTokenPos = ent.startTokenPos;
        //endTokenPos = ent.startTokenPos;
    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String raw) {
        this.rawName = raw;
    }

    public String getNormalisedName() {
        return normalisedName;
    }

    public void setNormalisedName(String raw) {
        this.normalisedName = raw;
    }

    public Medical_NER_Type getType() {
        return type;
    }

    public void setType(Medical_NER_Type theType) {
        type = theType;
    }

    public String getRawType() {
        return rawType;
    }

    public void setRawType(String rawType) {
        this.rawType = rawType;
    }

    public String getStringType() {
        return stringType;
    }

    public void setStringType(String stringType) {
        if (stringType.toUpperCase().contains("OTHER")) {
            this.stringType = "OTHER";
        } else if (stringType.toUpperCase().contains("PERSON")) {
            this.stringType = "PERSON";
        } else if (stringType.toUpperCase().contains("LOCATION")) {
            this.stringType = "LOCATION";
        } else if (stringType.toUpperCase().contains("ORGANISATION")) {
            this.stringType = "ORGANISATION";
        } else if (stringType.toUpperCase().contains("ANIMAL")) {
            this.stringType = "ANIMAL";
        } else if (stringType.toUpperCase().contains("MEASURE")) {
            this.stringType = "MEASURE";
        } else if (stringType.toUpperCase().contains("LEGAL")) {
            this.stringType = "LEGAL";
        } else if (stringType.toUpperCase().contains("IDENTIFIER")) {
            this.stringType = "IDENTIFIER";
        } else if (stringType.toUpperCase().contains("INSTALLATION")) {
            this.stringType = "INSTALLATION";
        } else if (stringType.toUpperCase().contains("SUBSTANCE")) {
            this.stringType = "SUBSTANCE";
        } else if (stringType.toUpperCase().contains("DRUG")) {
            this.stringType = "DRUG";
        } else if (stringType.toUpperCase().contains("PLANT")) {
            this.stringType = "PLANT";
        } else if (stringType.toUpperCase().contains("PERIOD")) {
            this.stringType = "PERIOD";
        } else if (stringType.toUpperCase().contains("TITLE")) {
            this.stringType = "TITLE";
        } else if (stringType.toUpperCase().contains("WEBSITE")) {
            this.stringType = "WEBSITE";
        } else if (stringType.toUpperCase().contains("ANATOMY")) {
            this.stringType = "ANATOMY";
        } else if (stringType.toUpperCase().contains("DEVICE")) {
            this.stringType = "DEVICE";
        } else if (stringType.toUpperCase().contains("DISORDER")) {
            this.stringType = "DISORDER";
        } else if (stringType.toUpperCase().contains("LIVING")) {
            this.stringType = "LIVING";
        } else if (stringType.toUpperCase().contains("OBJECT")) {
            this.stringType = "OBJECT";
        } else if (stringType.toUpperCase().contains("PHENOMENA")) {
            this.stringType = "PHENOMENA";
        } else if (stringType.toUpperCase().contains("PHYSIOLOGY")) {
            this.stringType = "PHYSIOLOGY";
        } else if (stringType.toUpperCase().contains("PROCEDURE")) {
            this.stringType = "PROCEDURE";
        }
    }

    public void setTypeFromString(String theType) {
        type = Medical_NER_Type.valueOf(theType);
    }
    public List<String> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(List<String> theSubTypes) {
        subTypes = theSubTypes;
    }

    public void addSubType(String subType) {
        if (subTypes == null)
            subTypes = new ArrayList<String>();
        subTypes.add(subType);
    }

    public OffsetPosition getOffsets() {
        return offsets;
    }

    public void setOffsets(OffsetPosition offsets) {
        this.offsets = offsets;
    }

    public void setOffsetStart(int start) {
        offsets.start = start;
    }

    public int getOffsetStart() {
        return offsets.start;
    }

    public void setOffsetEnd(int end) {
        offsets.end = end;
    }

    public int getOffsetEnd() {
        return offsets.end;
    }

    public double getProb() {
        return this.prob;
    }

    public void setProb(double prob) {
        this.prob = prob;
    }

    public double getConf() {
        return this.conf;
    }

    public void setConf(double conf) {
        this.conf = conf;
    }

    public Sense getSense() {
        return sense;
    }

    public void setSense(Sense sense) {
        this.sense = sense;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void addBoundingBoxes(BoundingBox boundingBox) {
        if (this.boundingBoxes == null)
            this.boundingBoxes = new ArrayList<BoundingBox>();
        this.boundingBoxes.add(boundingBox);
    }

    public boolean getIsAcronym() {
        return this.isAcronym;
    }

    public void setIsAcronym(boolean acronym) {
        this.isAcronym = acronym;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    @Override
    public boolean equals(Object object) {
        boolean result = false;
        if ((object != null) && object instanceof Entity) {
            int start = ((Entity) object).getOffsetStart();
            int end = ((Entity) object).getOffsetEnd();
            if ((start != -1) && (end != -1)) {
                if ((start == offsets.start) && (end == offsets.end)) {
                    result = true;
                }
            } /*else {
				int startToken = ((Entity)object).getStartTokenPos();
				int endToken = ((Entity)object).getEndTokenPos();
				if ( (startToken != -1) && (endToken != -1) ) {
					if ( (startToken == startTokenPos) && (endToken == endTokenPos) ) {
						result = true;
					}
				}
			}*/
        }
        return result;
    }

    @Override
    public int compareTo(MedicalEntity theEntity) {
        int start = theEntity.getOffsetStart();
        int end = theEntity.getOffsetEnd();

        if (offsets.start != start)
            return offsets.start - start;
        else
            return offsets.end - end;

    }

    public String toJson() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{ ");
        buffer.append("\"rawName\" : \"" + rawName + "\"");
        if (normalisedName != null)
            buffer.append(", \"normalisedName\" : \"" + normalisedName + "\"");
        if (type != null)
            buffer.append(", \"type\" : \"" + type.getName() + "\"");

        if (subTypes != null) {
            buffer.append(", \"subtype\" : [ ");
            boolean begin = true;
            for (String subtype : subTypes) {
                if (begin) {
                    buffer.append("\"" + subtype + "\"");
                    begin = false;
                } else {
                    buffer.append(", \"" + subtype + "\"");
                }
            }
            buffer.append(" ] \"");
        }

        if ((offsets != null) && (offsets.start != -1) && (offsets.end != -1)) {
            buffer.append(", \"offsetStart\" : " + offsets.start);
            buffer.append(", \"offsetEnd\" : " + offsets.end);
        }

        // start and end token index not to be outputed

        if ((boundingBoxes != null) && (boundingBoxes.size() > 0)) {
            buffer.append(", \"pos\" : [");
            boolean start = true;
            for (BoundingBox box : boundingBoxes) {
                if (start) {
                    buffer.append("{").append(box.toJson()).append("}");
                    start = false;
                } else {
                    buffer.append(", {").append(box.toJson()).append("}");
                }
            }
            buffer.append("]");
        }

        buffer.append(", \"conf\" : \"" + conf + "\"");
        buffer.append(", \"prob\" : \"" + prob + "\"");

        if (sense != null) {
            buffer.append(", \"sense\" : { ");
            if (sense.getFineSense() != null) {
                buffer.append("\"fineSense\" : \"" + sense.getFineSense() + "\"");
                buffer.append(", \"conf\" : \"" + sense.getFineSenseConfidence() + "\"");
            }

            if (sense.getCoarseSense() != null) {
                if ((sense.getFineSense() == null) ||
                    ((sense.getFineSense() != null) && !sense.getCoarseSense().equals(sense.getFineSense()))) {
                    buffer.append(", \"coarseSense\" : \"" + sense.getCoarseSense() + "\"");
                }
            }
            buffer.append(" }");
        }

        buffer.append(" }");
        return buffer.toString();
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (rawName != null) {
            buffer.append(rawName + "\t");
        }
        if (normalisedName != null) {
            buffer.append(normalisedName + "\t");
        }
        if (type != null) {
            buffer.append(type + "\t");
        }
        if (subTypes != null) {
            for (String subType : subTypes)
                buffer.append(subType + "\t");
        }
        if (offsets != null) {
            buffer.append(offsets.toString() + "\t");
        }
        if (sense != null) {
            if (sense.getFineSense() != null) {
                buffer.append(sense.getFineSense() + "\t");
            }

            if (sense.getCoarseSense() != null) {
                if ((sense.getFineSense() == null) ||
                    ((sense.getFineSense() != null) && !sense.getCoarseSense().equals(sense.getFineSense()))) {
                    buffer.append(sense.getCoarseSense() + "\t");
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Export of entity annotation in TEI standoff format
     */
    public String toTEI(String id, int n) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<stf xml:id=\"" + "ner-" + n + "\" type=\"ne\" who=\"nerd\" when=\"\">");
        buffer.append("<ptr target=\"id," + offsets.start + "," + offsets.end + "\" />");
        if (type == Medical_NER_Type.LIVING) {
            buffer.append("<person>" + rawName + "</person>");
        }
        buffer.append("</stf>");
        return buffer.toString();
    }
}