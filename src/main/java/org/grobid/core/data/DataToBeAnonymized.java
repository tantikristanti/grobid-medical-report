package org.grobid.core.data;

public class DataToBeAnonymized {
    String dataOriginal;

    public String getDataOriginal() {
        return dataOriginal;
    }

    public void setDataOriginal(String dataOriginal) {
        this.dataOriginal = dataOriginal;
    }

    public String getDataPseudo() {
        return dataPseudo;
    }

    public void setDataPseudo(String dataPseudo) {
        this.dataPseudo = dataPseudo;
    }

    String dataPseudo;

}
