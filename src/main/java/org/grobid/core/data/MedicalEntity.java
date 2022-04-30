package org.grobid.core.data;

import org.grobid.core.GrobidModels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.NERLexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.OffsetPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Representation of entity mentions.
 *
 */
public class MedicalEntity implements Comparable<MedicalEntity> {
    // map of labels (e.g. <medicament> or <symptom>) to LayoutToken
    private Map<String, List<LayoutToken>> labeledTokens;

    private String anatomy = null;
    private String date = null;
    private String device = null;
    private String dose = null;
    private String email = null;
    private String fax = null;
    private String living = null;
    private String location = null;
    private String measure = null;
    private String medicament = null;
    private String object = null;
    private String orgname = null;
    private String pathology = null;
    private String persname = null;
    private String phone = null;
    private String physiology = null;
    private String procedure = null;
    private String rolename = null;
    private String substance = null;
    private String symptom = null;
    private String unit = null;
    private String value = null;
    private String web = null;

    /* Biomedical NER base types are adapted from
     - grobid-ner (https://grobid-ner.readthedocs.io/en/latest/class-and-senses/)
     - The Quaero Corpus (https://quaerofrenchmed.limsi.fr/)
    * */
    public enum Medical_NER_Type {
        OTHER("OTHER"),
        ANATOMY("ANATOMY"),
        DATE("DATE"),
        DEVICE("DEVICE"),
        DOSE("DOSE"),
        EMAIL("EMAIL"),
        FAX("FAX"),
        LIVING("LIVING"),
        LOCATION("LOCATION"),
        MEASURE("MEASURE"),
        MEDICAMENT("MEDICAMENT"),
        OBJECT("OBJECT"),
        ORGNAME("ORGNAME"),
        PATHOLOGY("PATHOLOGY"),
        PERSNAME("PERSNAME"),
        PHONE("PHONE"),
        PHYSIOLOGY("PHYSIOLOGY"),
        PROCEDURE("PROCEDURE"),
        ROLENAME("ROLENAME"),
        SUBSTANCE("SUBSTANCE"),
        SYMPTOM("SYMPTOM"),
        UNIT("UNIT"),
        VALUE("VALUE"),
        WEB("WEB");

        private String name;


        private Medical_NER_Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // the origin of the entity definition
    public enum Origin {
        GROBID("grobid-medical-report"),
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
        } else if (stringType.toUpperCase().contains("ANATOMY")) {
            this.stringType = "ANATOMY";
        } else if (stringType.toUpperCase().contains("DATE")) {
            this.stringType = "DATE";
        } else if (stringType.toUpperCase().contains("DEVICE")) {
            this.stringType = "DEVICE";
        } else if (stringType.toUpperCase().contains("DOSE")) {
            this.stringType = "DOSE";
        } else if (stringType.toUpperCase().contains("EMAIL")) {
            this.stringType = "EMAIL";
        } else if (stringType.toUpperCase().contains("FAX")) {
            this.stringType = "FAX";
        } else if (stringType.toUpperCase().contains("LIVING")) {
            this.stringType = "LIVING";
        } else if (stringType.toUpperCase().contains("LOCATION")) {
            this.stringType = "LOCATION";
        } else if (stringType.toUpperCase().contains("MEASURE")) {
            this.stringType = "MEASURE";
        } else if (stringType.toUpperCase().contains("MEDICAMENT")) {
            this.stringType = "MEDICAMENT";
        } else if (stringType.toUpperCase().contains("OBJECT")) {
            this.stringType = "OBJECT";
        } else if (stringType.toUpperCase().contains("ORGNAME")) {
            this.stringType = "ORGNAME";
        } else if (stringType.toUpperCase().contains("PATHOLOGY")) {
            this.stringType = "PATHOLOGY";
        } else if (stringType.toUpperCase().contains("PERSNAME")) {
            this.stringType = "PERSNAME";
        } else if (stringType.toUpperCase().contains("PHONE")) {
            this.stringType = "PHONE";
        } else if (stringType.toUpperCase().contains("PHYSIOLOGY")) {
            this.stringType = "PHYSIOLOGY";
        } else if (stringType.toUpperCase().contains("PROCEDURE")) {
            this.stringType = "PROCEDURE";
        } else if (stringType.toUpperCase().contains("ROLENAME")) {
            this.stringType = "ROLENAME";
        } else if (stringType.toUpperCase().contains("SUBSTANCE")) {
            this.stringType = "SUBSTANCE";
        } else if (stringType.toUpperCase().contains("SYMPTOM")) {
            this.stringType = "SYMPTOM";
        } else if (stringType.toUpperCase().contains("UNIT")) {
            this.stringType = "UNIT";
        } else if (stringType.toUpperCase().contains("VALUE")) {
            this.stringType = "VALUE";
        } else if (stringType.toUpperCase().contains("WEB")) {
            this.stringType = "WEB";
        }
    }

    public String getAnatomy() {
        return anatomy;
    }

    public void setAnatomy(String anatomy) {
        this.anatomy = anatomy;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
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

    public String getLiving() {
        return living;
    }

    public void setLiving(String living) {
        this.living = living;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getMedicament() {
        return medicament;
    }

    public void setMedicament(String medicament) {
        this.medicament = medicament;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getOrgname() {
        return orgname;
    }

    public void setOrgname(String orgname) {
        this.orgname = orgname;
    }

    public String getPathology() {
        return pathology;
    }

    public void setPathology(String pathology) {
        this.pathology = pathology;
    }

    public String getPersname() {
        return persname;
    }

    public void setPersname(String persname) {
        this.persname = persname;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhysiology() {
        return physiology;
    }

    public void setPhysiology(String physiology) {
        this.physiology = physiology;
    }

    public String getSubstance() {
        return substance;
    }

    public void setSubstance(String substance) {
        this.substance = substance;
    }

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getProcedure() {
        return procedure;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
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

    public void generalResultMapping(String labeledResult, List<LayoutToken> tokenizations) {
        if (labeledTokens == null)
            labeledTokens = new TreeMap<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FR_MEDICAL_NER, labeledResult, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> clusterTokens = cluster.concatTokens();
            List<LayoutToken> theList = labeledTokens.get(clusterLabel.getLabel());

            theList = theList == null ? new ArrayList<>() : theList;
            theList.addAll(clusterTokens);
            labeledTokens.put(clusterLabel.getLabel(), theList);
        }
    }

}