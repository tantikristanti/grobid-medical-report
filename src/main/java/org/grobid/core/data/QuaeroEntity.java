package org.grobid.core.data;

public class QuaeroEntity {
    private String type;
    private String text;
    private int offset;
    private boolean isNestedEntity;

    private int length;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isNestedEntity() {
        return isNestedEntity;
    }

    public void setNestedEntity(boolean nestedEntity) {
        isNestedEntity = nestedEntity;
    }

}
