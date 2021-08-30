package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

public class QuaeroDocument {

    private String id;
    private String text;
    private List<QuaeroEntity> entities = new ArrayList<>();

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getText() { return text; }

    public void setText(String text) {
        this.text = text;
    }

    public List<QuaeroEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<QuaeroEntity> entities) {
        this.entities = entities;
    }

    public void addEntity(QuaeroEntity entity) {
        entities.add(entity);
    }

}
