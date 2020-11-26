package org.grobid.core.data.util;

import org.grobid.core.data.PersonMedical;
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Tanti, 2020
 */
public class ClassicMedicEmailAssigner implements MedicEmailAssigner {

    @Override

    public void assign(List<PersonMedical> fullMedics, List<String> emails) {
        List<Integer> winners = new ArrayList<Integer>();

        // if 1 email and 1 medic
        if (fullMedics != null) {
            if ((emails.size() == 1) && (fullMedics.size() == 1)) {
                fullMedics.get(0).setEmail(emails.get(0));
            } else {
                // we asociate emails to the medics based on string proximity
                for (String mail : emails) {
                    int maxDist = 1000;
                    int best = -1;
                    int ind = mail.indexOf("@");
                    if (ind != -1) {
                        String nam = mail.substring(0, ind).toLowerCase();
                        int k = 0;
                        for (PersonMedical personMedical : fullMedics) {
                            Integer kk = k;
                            if (!winners.contains(kk)) {
                                List<String> emailVariants = TextUtilities.generateEmailVariants(personMedical.getFirstName(), personMedical.getLastName());

                                for (String variant : emailVariants) {
                                    variant = variant.toLowerCase();

                                    int dist = TextUtilities.getLevenshteinDistance(nam, variant);
                                    if (dist < maxDist) {
                                        best = k;
                                        maxDist = dist;
                                    }
                                }
                            }
                            k++;
                        }

                        // make sure that the best candidate found is not too far
                        if (best != -1 && maxDist < nam.length() / 2) {
                            PersonMedical winner = fullMedics.get(best);
                            winner.setEmail(mail);
                            winners.add(best);
                        }
                    }
                }

            }
        }
    }
}
