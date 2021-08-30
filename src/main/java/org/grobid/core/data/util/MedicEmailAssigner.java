package org.grobid.core.data.util;

import org.grobid.core.data.PersonMedical;

import java.util.List;

public interface MedicEmailAssigner {
    //embeds emails into authors
    //emails should be sanitized before
    public void assign(List<PersonMedical> medics, List<String> emails);
}
