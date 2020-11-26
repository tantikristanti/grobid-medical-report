package org.grobid.core.data.util;

import org.grobid.core.data.PersonMedical;

import java.util.List;

/**
 * Tanti, 2020
 */
public interface MedicEmailAssigner {
    //embeds emails into medics
    //emails should be sanitized before
    public void assign(List<PersonMedical> authors, List<String> emails);
}
