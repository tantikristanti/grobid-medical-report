package org.grobid.core.utilities;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class MedicalReportProperties {
    public static final Logger LOGGER = LoggerFactory.getLogger(GrobidProperties.class);

    private static String GROBID_MEDICAL_VERSION = null;
    static final String UNKNOWN_VERSION_STR = "unknown";
    private static final String GROBID_MEDICAL_VERSION_FILE = "/grobid-medical-version.txt";

	public static String get(String key) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream stream = classLoader.getResourceAsStream("grobid-medical-report.properties");
		
		java.util.Properties properties = new java.util.Properties();
		try {
			properties.load(stream);
		} catch (IOException e1) {
			return null;
		}
		
		return properties.getProperty(key);
	}

    /**
     * Returns the current version of grobid-medical-report
     *
     * @return grobid-medical-report version
     */
    public static String getVersion() {
        if (GROBID_MEDICAL_VERSION == null) {
            synchronized (GrobidProperties.class) {
                if (GROBID_MEDICAL_VERSION == null) {
                    String grobidMedicalVersion = UNKNOWN_VERSION_STR;
                    try (InputStream is = MedicalReportProperties.class.getResourceAsStream(GROBID_MEDICAL_VERSION_FILE)) {
                        grobidMedicalVersion = IOUtils.toString(is, "UTF-8");
                    } catch (IOException e) {
                        LOGGER.error("Cannot read Grobid version from resources", e);
                    }
                    GROBID_MEDICAL_VERSION = grobidMedicalVersion;
                }
            }
        }
        return GROBID_MEDICAL_VERSION;
    }
	
}
