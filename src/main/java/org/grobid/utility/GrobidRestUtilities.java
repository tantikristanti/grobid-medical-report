package org.grobid.utility;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidRestUtilities {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GrobidRestUtilities.class);

    // type of PDF annotation for visualization purposes
    public enum Annotation {
        NER, BLOCK, FIGURE
    }

    /**
     * Check whether the result is null or empty.
     */
    public static boolean isResultNullOrEmpty(String result) {
        return StringUtils.isBlank(result);
    }

    public static Annotation getAnnotationFor(int type) {
        Annotation annotType = null;
        if (type == 0)
            annotType = Annotation.NER;
        else if (type == 1)
            annotType = Annotation.BLOCK;
        else if (type == 2)
            annotType = Annotation.FIGURE;

        return annotType;
    }

}
